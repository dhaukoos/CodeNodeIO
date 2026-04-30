/*
 * PipelineQuiescer - stops in-flight Runtime Preview pipelines before a recompile (FR-014)
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compile

import io.codenode.fbpdsl.runtime.DynamicPipelineController

/**
 * Tracks active [DynamicPipelineController] instances at the GraphEditor session level.
 * Before any recompile that could replace a class definition, [stopAll] halts every
 * tracked pipeline so coroutines holding old classloader references are released.
 *
 * RuntimePreviewPanel (or its viewmodel) registers/unregisters controllers on
 * Start/Stop. [RecompileSession] calls [stopAll] before invoking the compiler.
 *
 * The foundational implementation (T029) supports the [register]/[unregister]/[stopAll]
 * API in isolation; T042 wires it to the actual RuntimePreviewPanel state.
 */
class PipelineQuiescer {
    /** Generic stoppable target — any pipeline/runtime that needs stopping pre-recompile. */
    fun interface Stoppable {
        fun stop()
    }

    private val stoppables = java.util.LinkedHashSet<Stoppable>()
    /** Identity map so we can unregister by the original DynamicPipelineController reference. */
    private val controllerWraps = mutableMapOf<DynamicPipelineController, Stoppable>()
    private val lock = Any()

    /** Register a generic Stoppable. Idempotent. */
    fun register(stoppable: Stoppable) {
        synchronized(lock) { stoppables.add(stoppable) }
    }

    /** Unregister a generic Stoppable. Idempotent (no-op when absent). */
    fun unregister(stoppable: Stoppable) {
        synchronized(lock) { stoppables.remove(stoppable) }
    }

    /** Add a controller to the tracking set. Idempotent. */
    fun register(controller: DynamicPipelineController) {
        synchronized(lock) {
            val wrap = controllerWraps.getOrPut(controller) {
                Stoppable { controller.stop() }
            }
            stoppables.add(wrap)
        }
    }

    /** Remove a controller from the tracking set. Idempotent (no-op when absent). */
    fun unregister(controller: DynamicPipelineController) {
        synchronized(lock) {
            controllerWraps.remove(controller)?.let { stoppables.remove(it) }
        }
    }

    /**
     * Stops every currently-registered Stoppable. Returns the count stopped — surfaced
     * in [io.codenode.flowgraphinspect.compile.RecompileResult.pipelinesQuiesced].
     */
    fun stopAll(): Int {
        val snapshot = synchronized(lock) { stoppables.toList() }
        for (s in snapshot) {
            try {
                s.stop()
            } catch (_: Exception) {
                // Best-effort: continue stopping the rest even if one throws.
            }
        }
        return snapshot.size
    }
}
