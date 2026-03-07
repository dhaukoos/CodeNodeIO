/*
 * SourceRuntime - Specialized runtime for continuous source nodes
 * Manages the source loop with emit function
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.ExecutionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Specialized NodeRuntime for continuous source nodes.
 *
 * Extends NodeRuntime with source-specific behavior:
 * - Creates output channel with specified capacity
 * - Runs the source block with an emit function
 * - Closes output channel when source stops
 *
 * @param T Type of values emitted by the source
 * @param codeNode The underlying CodeNode model
 * @param channelCapacity Buffer capacity for output channel
 * @param generate The source block that receives an emit function
 */
class SourceRuntime<T : Any>(
    codeNode: CodeNode,
    private val channelCapacity: Int = Channel.BUFFERED,
    private val generate: ContinuousSourceBlock<T>
) : NodeRuntime(codeNode) {

    /**
     * Output channel for emitting source data.
     */
    var outputChannel: SendChannel<T>? = null

    init {
        // Create output channel with specified capacity
        outputChannel = Channel(channelCapacity)
    }

    /**
     * Starts the source's continuous emission loop.
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

        // Recreate output channel (previous one may have been closed on stop)
        outputChannel = Channel(channelCapacity)

        // Transition to RUNNING state
        executionState = ExecutionState.RUNNING

        // Register with registry for centralized lifecycle control
        registry?.register(this)

        // Launch the source job
        nodeControlJob = scope.launch {
            try {
                // Create emit function that sends to output channel
                // Includes pause check before each emit
                val emit: suspend (T) -> Unit = { value ->
                    // Pause hook - wait while paused
                    while (executionState == ExecutionState.PAUSED) {
                        delay(10)
                    }
                    // Only send if still running (not stopped during pause)
                    if (executionState == ExecutionState.RUNNING) {
                        outputChannel?.send(value)
                        onEmit?.invoke(codeNode.name, 0)
                    }
                }

                // Run the source block with emit function
                generate(emit)
            } catch (e: ClosedSendChannelException) {
                // Channel closed - graceful shutdown
            } finally {
                // Close output channel when loop exits
                outputChannel?.close()
            }
        }
    }
}
