/*
 * In2SinkRuntime - Runtime for sink nodes with 2 inputs
 * Manages synchronous receive from two input channels
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.ExecutionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Specialized NodeRuntime for sink nodes with 2 inputs.
 *
 * Extends NodeRuntime with multi-input sink behavior:
 * - Waits for values on both input channels (synchronous receive)
 * - Invokes consume function with both values
 * - No output channels
 * - Runs continuously until stopped
 * - Handles channel closure gracefully
 *
 * @param A Type of first input
 * @param B Type of second input
 * @param codeNode The underlying CodeNode model
 * @param consume The sink function that processes both inputs
 */
class In2SinkRuntime<A : Any, B : Any>(
    codeNode: CodeNode,
    private val consume: In2SinkBlock<A, B>
) : NodeRuntime<A>(codeNode) {

    /**
     * Second input channel for receiving data.
     */
    var inputChannel2: ReceiveChannel<B>? = null

    /**
     * Starts the sink's continuous consumption loop.
     *
     * The processingBlock parameter is ignored - the sink uses
     * its own consume function configured at construction time.
     *
     * @param scope CoroutineScope to launch the sink job in
     * @param processingBlock Ignored - sink uses its own function
     */
    override fun start(scope: CoroutineScope, processingBlock: suspend () -> Unit) {
        // Cancel existing job if running
        nodeControlJob?.cancel()

        // Transition to RUNNING state
        executionState = ExecutionState.RUNNING

        // Launch the sink job
        nodeControlJob = scope.launch {
            try {
                // Get channels - return early if not set
                val inChannel1 = inputChannel ?: return@launch
                val inChannel2 = inputChannel2 ?: return@launch

                // Continuous consumption loop
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

                    // Consume values
                    consume(value1, value2)
                }
            } catch (e: ClosedReceiveChannelException) {
                // Input channel closed - graceful shutdown
            } finally {
                // Transition to IDLE
                executionState = ExecutionState.IDLE
            }
        }
    }
}
