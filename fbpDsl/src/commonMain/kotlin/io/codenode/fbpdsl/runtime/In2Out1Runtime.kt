/*
 * In2Out1Runtime - Runtime for nodes with 2 inputs and 1 output
 * Manages synchronous receive from two input channels
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.ExecutionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Specialized NodeRuntime for nodes with 2 inputs and 1 output.
 *
 * Extends NodeRuntime with multi-input behavior:
 * - Waits for values on both input channels (synchronous receive)
 * - Invokes process function with both values
 * - Sends result to output channel
 * - Runs continuously until stopped
 * - Handles channel closure gracefully
 *
 * @param A Type of first input
 * @param B Type of second input
 * @param R Type of output
 * @param codeNode The underlying CodeNode model
 * @param process The processing function that combines both inputs
 */
class In2Out1Runtime<A : Any, B : Any, R : Any>(
    codeNode: CodeNode,
    private val process: In2Out1ProcessBlock<A, B, R>
) : NodeRuntime<A>(codeNode) {

    /**
     * Second input channel for receiving data.
     */
    var inputChannel2: ReceiveChannel<B>? = null

    /**
     * Output channel for sending processed results.
     * Separate from base class since output type R differs from input type A.
     */
    var processorOutputChannel: SendChannel<R>? = null

    /**
     * Starts the processor's continuous processing loop.
     *
     * The processingBlock parameter is ignored - the processor uses
     * its own process function configured at construction time.
     *
     * @param scope CoroutineScope to launch the processor job in
     * @param processingBlock Ignored - processor uses its own function
     */
    override fun start(scope: CoroutineScope, processingBlock: suspend () -> Unit) {
        // Cancel existing job if running
        nodeControlJob?.cancel()

        // Transition to RUNNING state
        executionState = ExecutionState.RUNNING

        // Launch the processor job
        nodeControlJob = scope.launch {
            try {
                // Get channels - return early if not set
                val inChannel1 = inputChannel ?: return@launch
                val inChannel2 = inputChannel2 ?: return@launch
                val outChannel = processorOutputChannel ?: return@launch

                // Continuous processing loop
                while (executionState != ExecutionState.IDLE) {
                    // Check pause state
                    while (executionState == ExecutionState.PAUSED) {
                        delay(10)
                    }

                    // Exit if stopped during pause
                    if (executionState == ExecutionState.IDLE) break

                    // Synchronous receive from both channels
                    val value1 = inChannel1.receive()
                    val value2 = inChannel2.receive()

                    // Process and send result
                    val result = process(value1, value2)
                    outChannel.send(result)
                }
            } catch (e: ClosedReceiveChannelException) {
                // Input channel closed - graceful shutdown
            } catch (e: ClosedSendChannelException) {
                // Output channel closed - graceful shutdown
            } finally {
                // Transition to IDLE and close output
                executionState = ExecutionState.IDLE
                processorOutputChannel?.close()
            }
        }
    }
}
