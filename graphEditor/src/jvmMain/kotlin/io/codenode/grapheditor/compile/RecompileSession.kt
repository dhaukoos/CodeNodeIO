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
 * Concurrency: serializes compile invocations via an internal mutex (Decision 7).
 * Failure isolation: on [CompileResult.Failure][io.codenode.flowgraphinspect.compile.CompileResult.Failure]
 * the registry is NOT touched (FR-013).
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
     * Tracks installed scopes so [shutdown] can close them. The registry holds the
     * canonical strong reference for resolution; this mirror is for cleanup.
     */
    private val ownedScopes: MutableList<ClassloaderScope> = mutableListOf()

    @Volatile
    private var shutdownInvoked: Boolean = false

    /**
     * Compile [unit]. On success, install every produced [io.codenode.fbpdsl.runtime.CodeNodeDefinition]
     * into [registry]. On failure, leave the registry untouched (FR-013).
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
                    registry.installSessionDefinition(scope, nodeName, def)
                    installedAny = true
                }
            }
            if (installedAny) {
                ownedScopes += scope
            } else {
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
     * [io.codenode.flowgraphgenerate.viewmodel.NodeGeneratorViewModel].
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

    /** Idempotent shutdown — closes session classloaders and deletes the cache. */
    fun shutdown() {
        if (shutdownInvoked) return
        shutdownInvoked = true
        for (s in ownedScopes) {
            try {
                s.close()
            } catch (_: Exception) {
                // best-effort
            }
        }
        ownedScopes.clear()
        try {
            sessionCacheDir.deleteRecursively()
        } catch (_: Exception) {
            // best-effort
        }
    }
}
