/*
 * SourceOut2Runtime - Runtime for source nodes with 2 outputs
 * Manages emission to two output channels via ProcessResult2
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.ExecutionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Specialized NodeRuntime for source nodes with 2 outputs.
 *
 * Extends NodeRuntime with multi-output source behavior:
 * - Creates two output channels with specified capacity
 * - Runs the generate block with an emit function that receives ProcessResult2
 * - Distributes non-null values to appropriate output channels
 * - Closes output channels when source stops
 *
 * @param U Type of first output
 * @param V Type of second output
 * @param codeNode The underlying CodeNode model
 * @param channelCapacity Buffer capacity for output channels
 * @param generate The generate block that receives an emit function
 */
class SourceOut2Runtime<U : Any, V : Any>(
    codeNode: CodeNode,
    private val channelCapacity: Int = Channel.BUFFERED,
    private val generate: SourceOut2Block<U, V>? = null
) : NodeRuntime(codeNode) {

    /**
     * First output channel for sending/receiving data.
     */
    var outputChannel1: Channel<U>? = null
        private set

    /**
     * Second output channel for sending/receiving data.
     */
    var outputChannel2: Channel<V>? = null
        private set

    init {
        // Create output channels with specified capacity
        outputChannel1 = Channel(channelCapacity)
        outputChannel2 = Channel(channelCapacity)
    }

    /**
     * Starts the source's emission loop.
     *
     * The processingBlock parameter is ignored - the source uses
     * its own generate block configured at construction time.
     *
     * @param scope CoroutineScope to launch the source job in
     * @param processingBlock Ignored - source uses its own block
     */
    override fun start(scope: CoroutineScope, processingBlock: suspend () -> Unit) {
        // Cancel existing job if running
        nodeControlJob?.cancel()

        // Recreate output channels (previous ones may have been closed on stop)
        outputChannel1 = Channel(channelCapacity)
        outputChannel2 = Channel(channelCapacity)

        // Transition to RUNNING state
        executionState = ExecutionState.RUNNING

        // Register with registry for centralized lifecycle control
        registry?.register(this)

        // Launch the source job
        nodeControlJob = scope.launch {
            try {
                // Get output channels
                val out1 = outputChannel1 ?: return@launch
                val out2 = outputChannel2 ?: return@launch

                val gen = generate ?: return@launch

                // Create emit function that distributes to output channels
                // Includes pause check before each emit
                val emit: suspend (ProcessResult2<U, V>) -> Unit = { result ->
                    // Pause hook - wait while paused
                    while (executionState == ExecutionState.PAUSED) {
                        delay(10)
                    }
                    // Only send if still running (not stopped during pause)
                    if (executionState == ExecutionState.RUNNING) {
                        // Send non-null values to respective channels
                        result.out1?.let {
                            out1.send(it)
                            onEmit?.invoke(codeNode.name, 0)
                            onEmitValue?.invoke(codeNode.name, 0, it)
                        }
                        result.out2?.let {
                            out2.send(it)
                            onEmit?.invoke(codeNode.name, 1)
                            onEmitValue?.invoke(codeNode.name, 1, it)
                        }
                    }
                }

                // Run the generate block
                gen(emit)
            } catch (e: ClosedSendChannelException) {
                // Channel closed - graceful shutdown
            } finally {
                // Close output channels when loop exits
                outputChannel1?.close()
                outputChannel2?.close()
            }
        }
    }
}
