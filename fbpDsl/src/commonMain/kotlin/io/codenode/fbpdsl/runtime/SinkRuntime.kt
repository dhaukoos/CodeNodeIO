/*
 * SinkRuntime - Specialized runtime for continuous sink nodes
 * Manages the sink loop that consumes from input channel
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.ExecutionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch

/**
 * Specialized NodeRuntime for continuous sink nodes.
 *
 * Extends NodeRuntime with sink-specific behavior:
 * - Iterates over input channel consuming values
 * - Handles channel closure gracefully (ClosedReceiveChannelException)
 * - Runs the consume block for each received value
 *
 * @param T Type of values consumed by the sink
 * @param codeNode The underlying CodeNode model
 * @param consume The sink block that processes each received value
 */
class SinkRuntime<T : Any>(
    codeNode: CodeNode,
    private val consume: ContinuousSinkBlock<T>
) : NodeRuntime<T>(codeNode) {

    /**
     * Starts the sink's continuous consumption loop.
     *
     * The processingBlock parameter is ignored - the sink uses
     * its own consume block configured at construction time.
     *
     * @param scope CoroutineScope to launch the sink job in
     * @param processingBlock Ignored - sink uses its own block
     */
    override fun start(scope: CoroutineScope, processingBlock: suspend () -> Unit) {
        // Cancel existing job if running
        nodeControlJob?.cancel()

        // Transition to RUNNING state
        executionState = ExecutionState.RUNNING

        // Launch the sink job
        nodeControlJob = scope.launch {
            try {
                // Get input channel - return early if not set
                val channel = inputChannel ?: return@launch

                // Iterate over channel using for-loop (handles closure gracefully)
                for (value in channel) {
                    consume(value)
                }
                // For-loop exits normally when channel is closed
            } catch (e: ClosedReceiveChannelException) {
                // Channel closed unexpectedly - graceful shutdown
            }
        }
    }
}
