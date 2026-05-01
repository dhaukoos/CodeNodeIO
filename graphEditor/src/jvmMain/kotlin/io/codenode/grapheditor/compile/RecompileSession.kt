/*
 * RecompileSession - session-scoped coordinator for in-process compiles
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compile

import io.codenode.fbpdsl.model.PlacementLevel
import io.codenode.flowgraphinspect.compile.ClassloaderScope
import io.codenode.flowgraphinspect.compile.CompileResult
import io.codenode.flowgraphinspect.compile.CompileSource
import io.codenode.flowgraphinspect.compile.CompileUnit
import io.codenode.flowgraphinspect.compile.InProcessCompiler
import io.codenode.flowgraphinspect.compile.RecompileResult
import io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coordinates per-file (FR-001) and per-module (FR-004) recompiles. One instance per
 * GraphEditor session; created at startup, disposed at shutdown.
 *
 * Behavior contract:
 * - **Concurrency** — serializes every compile invocation via an internal mutex
 *   (Decision 7); concurrent callers are queued, never run in parallel.
 * - **Failure isolation** — on [CompileResult.Failure][io.codenode.flowgraphinspect.compile.CompileResult.Failure]
 *   the registry is left untouched (FR-013); the prior install (or launch-time
 *   classpath entry) remains canonical.
 * - **Pipeline quiescence** — every recompile first calls [PipelineQuiescer.stopAll]
 *   so the running execution graph cannot observe a partially-installed scope
 *   (FR-014).
 * - **Memory bound** — when a new install supersedes the last node name owned
 *   by an earlier scope, that scope is closed and dropped mid-session (SC-004 /
 *   Decision 6). The session's resident footprint stays bounded by the size of
 *   the currently-installed definition set, not the cumulative recompile count.
 *
 * Lifetime: the instance is created in `GraphEditorApp.kt` at startup and
 * [shutdown] is invoked exactly once at app teardown.
 */
class RecompileSession(
    private val compiler: InProcessCompiler,
    private val registry: NodeDefinitionRegistry,
    private val pipelineQuiescer: PipelineQuiescer,
    private val publisher: RecompileFeedbackPublisher,
    private val sessionCacheDir: File
) {
    /**
     * Mutex serializing every compile invocation in this session per Decision 7. Without
     * serialization, concurrent compiles would race on cache directories AND on
     * registry session installs.
     */
    private val mutex: Mutex = Mutex()

    /**
     * Tracks live scopes by the set of node names they currently own. Two roles:
     *   1. [shutdown] closes every entry.
     *   2. When a recompile supersedes a scope's last owned node name, the entry is
     *      removed and the scope is closed mid-session — required for SC-004
     *      ("old versions are eligible for collection — not pinned forever").
     *
     * Without per-supersession eviction, every recompile would add a new scope and
     * the prior generation's classloader+classes would be retained until shutdown,
     * making session-resident memory grow linearly with recompile count.
     */
    private val ownedScopes: MutableMap<ClassloaderScope, MutableSet<String>> = mutableMapOf()

    /** Reverse index: which scope currently owns each node name? */
    private val scopeByNodeName: MutableMap<String, ClassloaderScope> = mutableMapOf()

    @Volatile
    private var shutdownInvoked: Boolean = false

    /**
     * Compile [unit] and (on success) atomically install every produced
     * [io.codenode.fbpdsl.runtime.CodeNodeDefinition] into [registry].
     *
     * Pre-conditions:
     *  - The session must not be shut down. A post-shutdown call returns a
     *    synthetic failure result with a single ERROR diagnostic; no compile is
     *    invoked, no registry mutation occurs.
     *
     * Side effects:
     *  - Pipelines registered with [pipelineQuiescer] are stopped before the
     *    compile begins (FR-014). The quiesced count is reported back via
     *    [RecompileResult.pipelinesQuiesced] for the UI message.
     *  - On [CompileResult.Success]: every loaded definition is installed via
     *    [io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry.installSessionDefinition].
     *    For each installed name, the prior owning scope is decremented; once
     *    its owned-name set empties, the scope is closed and our strong reference
     *    is dropped (SC-004).
     *  - On [CompileResult.Failure]: registry is untouched (FR-013).
     *  - In all cases: the result is published via [publisher] so the Error
     *    Console / status bar can render diagnostics + duration.
     *
     * Thread-safety: serialized by [mutex]. Concurrent callers queue.
     */
    suspend fun recompile(unit: CompileUnit): RecompileResult = mutex.withLock {
        if (shutdownInvoked) {
            // Build a synthetic RecompileResult expressing "session is closed" — no
            // compiler invocation, no registry mutation.
            val synthetic = CompileResult.Failure(
                unit = unit,
                diagnostics = listOf(
                    io.codenode.flowgraphinspect.compile.CompileDiagnostic(
                        severity = io.codenode.flowgraphinspect.compile.CompileDiagnostic.Severity.ERROR,
                        filePath = null, line = 0, column = 0,
                        message = "RecompileSession is shut down; ignoring recompile request."
                    )
                )
            )
            val r = RecompileResult(synthetic, durationMs = 0)
            publisher.publish(r)
            return@withLock r
        }

        // FR-014: stop any running pipeline first; record the count.
        val quiescedCount = pipelineQuiescer.stopAll()

        val started = System.nanoTime()
        val compileResult = compiler.compile(unit)
        val elapsedMs = (System.nanoTime() - started) / 1_000_000

        // On Success: build a ClassloaderScope and install every loaded definition.
        // On Failure: do nothing to the registry (FR-013) — prior version stays canonical.
        if (compileResult is CompileResult.Success) {
            val outputDir = File(compileResult.classOutputDir)
            val scope = ClassloaderScope(
                unit = unit,
                classOutputDir = outputDir,
                parent = this::class.java.classLoader
            )
            var installedAny = false
            for ((nodeName, fqcn) in compileResult.loadedDefinitionsByName) {
                val def = scope.loadDefinition(fqcn)
                if (def != null) {
                    // Evict the prior scope that owned this name (if any). Once its
                    // owned-name set empties, close it and drop our strong reference
                    // so it becomes GC-eligible (SC-004).
                    scopeByNodeName[nodeName]?.let { priorScope ->
                        if (priorScope !== scope) {
                            val priorNames = ownedScopes[priorScope]
                            priorNames?.remove(nodeName)
                            if (priorNames.isNullOrEmpty()) {
                                ownedScopes.remove(priorScope)
                                try {
                                    priorScope.close()
                                } catch (_: Exception) {
                                    // best-effort
                                }
                            }
                        }
                    }
                    registry.installSessionDefinition(scope, nodeName, def)
                    scopeByNodeName[nodeName] = scope
                    ownedScopes.getOrPut(scope) { mutableSetOf() }.add(nodeName)
                    installedAny = true
                }
            }
            if (!installedAny) {
                scope.close()
            }
        }

        val result = RecompileResult(
            compileResult = compileResult,
            durationMs = elapsedMs,
            pipelinesQuiesced = quiescedCount
        )
        publisher.publish(result)
        return@withLock result
    }

    /**
     * Convenience for the FR-001 auto-fire path used by
     * [io.codenode.flowgraphgenerate.viewmodel.NodeGeneratorViewModel] when the user
     * confirms node generation. Wraps [file] in a [CompileUnit.SingleFile] tagged
     * with the active [tier] and (optionally) [hostModule], then delegates to
     * [recompile].
     */
    suspend fun recompileGenerated(
        file: File,
        tier: PlacementLevel,
        hostModule: String?
    ): RecompileResult = recompile(
        CompileUnit.SingleFile(
            CompileSource(
                absolutePath = file.absolutePath,
                tier = tier,
                hostModuleName = hostModule
            )
        )
    )

    /**
     * Closes every still-live session classloader scope and deletes the on-disk
     * compile cache. Idempotent — safe to call from a finally block. Any further
     * [recompile] call after [shutdown] returns a synthetic failure result.
     */
    fun shutdown() {
        if (shutdownInvoked) return
        shutdownInvoked = true
        for (s in ownedScopes.keys) {
            try {
                s.close()
            } catch (_: Exception) {
                // best-effort
            }
        }
        ownedScopes.clear()
        scopeByNodeName.clear()
        try {
            sessionCacheDir.deleteRecursively()
        } catch (_: Exception) {
            // best-effort
        }
    }
}
