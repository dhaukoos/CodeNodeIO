/*
 * TransformerRuntime - Specialized runtime for continuous transformer nodes
 * Manages the transformer loop that receives, transforms, and sends values
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.ExecutionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

/**
 * Specialized NodeRuntime for continuous transformer nodes.
 *
 * Extends NodeRuntime with transformer-specific behavior:
 * - Iterates over input channel receiving values
 * - Applies transform function to each value
 * - Sends transformed values to output channel
 * - Handles channel closure gracefully
 * - Respects pause/resume for flow control
 *
 * @param TIn Type of input values
 * @param TOut Type of output values
 * @param codeNode The underlying CodeNode model
 * @param transform The transform function applied to each input value
 */
class TransformerRuntime<TIn : Any, TOut : Any>(
    codeNode: CodeNode,
    private val transform: ContinuousTransformBlock<TIn, TOut>
) : NodeRuntime(codeNode) {

    /**
     * Input channel for receiving data.
     */
    var inputChannel: ReceiveChannel<TIn>? = null

    /**
     * Output channel for emitting transformed data.
     */
    var outputChannel: SendChannel<TOut>? = null

    /**
     * Starts the transformer's continuous processing loop.
     *
     * The processingBlock parameter is ignored - the transformer uses
     * its own transform block configured at construction time.
     *
     * @param scope CoroutineScope to launch the transformer job in
     * @param processingBlock Ignored - transformer uses its own block
     */
    override fun start(scope: CoroutineScope, processingBlock: suspend () -> Unit) {
        // Cancel existing job if running
        nodeControlJob?.cancel()

        // Transition to RUNNING state
        executionState = ExecutionState.RUNNING

        // Register with registry for centralized lifecycle control
        registry?.register(this)

        // Launch the transformer job
        nodeControlJob = scope.launch {
            try {
                // Get input channel - return early if not set
                val inChannel = inputChannel ?: return@launch
                val outChannel = outputChannel ?: return@launch

                // Transform loop with pause support
                while (executionState != ExecutionState.IDLE) {
                    // Check pause state
                    while (executionState == ExecutionState.PAUSED) {
                        kotlinx.coroutines.delay(10)
                    }
                    // Exit if stopped during pause
                    if (executionState == ExecutionState.IDLE) break

                    // Receive next value (suspends until available or channel closed)
                    val value = inChannel.receiveCatching().getOrNull() ?: break

                    // Transform and send
                    val transformed = transform(value)
                    outChannel.send(transformed)
                }
            } catch (e: ClosedReceiveChannelException) {
                // Input channel closed - graceful shutdown
            } finally {
                // Close output channel when loop exits
                outputChannel?.close()
            }
        }
    }
}
