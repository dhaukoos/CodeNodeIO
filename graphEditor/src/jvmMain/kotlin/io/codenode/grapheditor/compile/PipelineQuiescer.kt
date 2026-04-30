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
    /** Add a controller to the tracking set. Idempotent. */
    fun register(controller: DynamicPipelineController) {
        throw NotImplementedError("T029 will implement PipelineQuiescer.register")
    }

    /** Remove a controller from the tracking set. Idempotent (no-op when absent). */
    fun unregister(controller: DynamicPipelineController) {
        throw NotImplementedError("T029 will implement PipelineQuiescer.unregister")
    }

    /**
     * Stops every currently-registered controller (calls [DynamicPipelineController.stop]).
     * Returns the count of controllers that were stopped — surfaced in [RecompileResult.pipelinesQuiesced].
     */
    fun stopAll(): Int {
        throw NotImplementedError("T029 will implement PipelineQuiescer.stopAll")
    }
}
