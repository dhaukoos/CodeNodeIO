/*
 * RecompileSession - session-scoped coordinator for in-process compiles
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compile

import io.codenode.fbpdsl.model.PlacementLevel
import io.codenode.flowgraphinspect.compile.CompileUnit
import io.codenode.flowgraphinspect.compile.InProcessCompiler
import io.codenode.flowgraphinspect.compile.RecompileResult
import io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry
import java.io.File

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
     * Compile [unit]. On success, install every produced [CodeNodeDefinition] into
     * [registry]. On failure, leave the registry untouched (FR-013).
     *
     * Suspends until the compile completes and the registry is updated; returns a
     * structured [RecompileResult] with timing, diagnostics, and pipeline-quiesce count.
     */
    suspend fun recompile(unit: CompileUnit): RecompileResult {
        throw NotImplementedError("T031 will implement RecompileSession.recompile")
    }

    /** Convenience for the FR-001 auto-fire path used by [io.codenode.grapheditor.viewmodel.NodeGeneratorViewModel]. */
    suspend fun recompileGenerated(
        file: File,
        tier: PlacementLevel,
        hostModule: String?
    ): RecompileResult {
        throw NotImplementedError("T031 will implement RecompileSession.recompileGenerated")
    }

    /** Idempotent shutdown — closes session classloaders and deletes the cache. */
    fun shutdown() {
        throw NotImplementedError("T031 will implement RecompileSession.shutdown")
    }
}
