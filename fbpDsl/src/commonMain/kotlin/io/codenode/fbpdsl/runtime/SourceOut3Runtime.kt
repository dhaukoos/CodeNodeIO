/*
 * SourceOut3Runtime - Runtime for source nodes with 3 outputs
 * Manages emission to three output channels via ProcessResult3
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.ExecutionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Specialized NodeRuntime for source nodes with 3 outputs.
 *
 * Extends NodeRuntime with multi-output source behavior:
 * - Creates three output channels with specified capacity
 * - Runs the generate block with an emit function that receives ProcessResult3
 * - Distributes non-null values to appropriate output channels
 * - Closes output channels when source stops
 *
 * @param U Type of first output
 * @param V Type of second output
 * @param W Type of third output
 * @param codeNode The underlying CodeNode model
 * @param channelCapacity Buffer capacity for output channels
 * @param generate The generate block that receives an emit function
 */
class SourceOut3Runtime<U : Any, V : Any, W : Any>(
    codeNode: CodeNode,
    private val channelCapacity: Int = Channel.BUFFERED,
    private val generate: SourceOut3Block<U, V, W>
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

    /**
     * Third output channel for sending/receiving data.
     */
    var outputChannel3: Channel<W>? = null
        private set

    init {
        // Create output channels with specified capacity
        outputChannel1 = Channel(channelCapacity)
        outputChannel2 = Channel(channelCapacity)
        outputChannel3 = Channel(channelCapacity)
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
        outputChannel3 = Channel(channelCapacity)

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
                val out3 = outputChannel3 ?: return@launch

                // Create emit function that distributes to output channels
                // Includes pause check before each emit
                val emit: suspend (ProcessResult3<U, V, W>) -> Unit = { result ->
                    // Pause hook - wait while paused
                    while (executionState == ExecutionState.PAUSED) {
                        delay(10)
                    }
                    // Only send if still running (not stopped during pause)
                    if (executionState == ExecutionState.RUNNING) {
                        // Send non-null values to respective channels
                        result.out1?.let {
                            out1.send(it)
                            onEmit?.invoke(codeNode.id, 0)
                        }
                        result.out2?.let {
                            out2.send(it)
                            onEmit?.invoke(codeNode.id, 1)
                        }
                        result.out3?.let {
                            out3.send(it)
                            onEmit?.invoke(codeNode.id, 2)
                        }
                    }
                }

                // Run the generate block with emit function
                generate(emit)
            } catch (e: ClosedSendChannelException) {
                // Channel closed - graceful shutdown
            } finally {
                // Close output channels when loop exits
                outputChannel1?.close()
                outputChannel2?.close()
                outputChannel3?.close()
            }
        }
    }
}
