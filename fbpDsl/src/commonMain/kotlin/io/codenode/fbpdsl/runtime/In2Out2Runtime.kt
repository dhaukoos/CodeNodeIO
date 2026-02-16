/*
 * In2Out2Runtime - Runtime for nodes with 2 inputs and 2 outputs
 * Manages continuous processing with synchronous receive and selective output
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.ExecutionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Specialized NodeRuntime for nodes with 2 inputs and 2 outputs.
 *
 * Extends NodeRuntime with multi-input/multi-output behavior:
 * - Waits for values on both input channels (synchronous receive)
 * - Invokes process function that returns ProcessResult2
 * - Sends non-null values to respective output channels (selective output)
 * - Runs continuously until stopped
 * - Handles channel closure gracefully
 *
 * @param A Type of first input
 * @param B Type of second input
 * @param U Type of first output
 * @param V Type of second output
 * @param codeNode The underlying CodeNode model
 * @param channelCapacity Buffer capacity for output channels
 * @param process The processing function that combines inputs and produces ProcessResult2
 */
class In2Out2Runtime<A : Any, B : Any, U : Any, V : Any>(
    codeNode: CodeNode,
    private val channelCapacity: Int = Channel.BUFFERED,
    private val process: In2Out2ProcessBlock<A, B, U, V>
) : NodeRuntime<A>(codeNode) {

    /**
     * Second input channel for receiving data.
     */
    var inputChannel2: ReceiveChannel<B>? = null

    /**
     * First output channel for sending data.
     */
    var outputChannel1: Channel<U>? = null
        private set

    /**
     * Second output channel for sending data.
     */
    var outputChannel2: Channel<V>? = null
        private set

    init {
        // Create output channels with specified capacity
        outputChannel1 = Channel(channelCapacity)
        outputChannel2 = Channel(channelCapacity)
    }

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
                val out1 = outputChannel1 ?: return@launch
                val out2 = outputChannel2 ?: return@launch

                // Continuous processing loop
                while (executionState != ExecutionState.IDLE) {
                    // Check pause state
                    while (executionState == ExecutionState.PAUSED) {
                        delay(10)
                    }

                    // Exit if stopped during pause
                    if (executionState == ExecutionState.IDLE) break

                    // Synchronous receive from both input channels
                    val value1 = inChannel1.receive()
                    val value2 = inChannel2.receive()

                    // Process and get result
                    val result = process(value1, value2)

                    // Send non-null values to respective channels (selective output)
                    result.out1?.let { out1.send(it) }
                    result.out2?.let { out2.send(it) }
                }
            } catch (e: ClosedReceiveChannelException) {
                // Input channel closed - graceful shutdown
            } catch (e: ClosedSendChannelException) {
                // Output channel closed - graceful shutdown
            } finally {
                // Transition to IDLE and close outputs
                executionState = ExecutionState.IDLE
                outputChannel1?.close()
                outputChannel2?.close()
            }
        }
    }
}
