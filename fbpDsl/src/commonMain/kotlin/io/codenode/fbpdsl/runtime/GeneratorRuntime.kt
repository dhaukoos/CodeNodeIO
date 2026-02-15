/*
 * GeneratorRuntime - Specialized runtime for continuous generator nodes
 * Manages the generator loop with emit function
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.ExecutionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.launch

/**
 * Specialized NodeRuntime for continuous generator nodes.
 *
 * Extends NodeRuntime with generator-specific behavior:
 * - Creates output channel with specified capacity
 * - Runs the generator block with an emit function
 * - Closes output channel when generator stops
 *
 * @param T Type of values emitted by the generator
 * @param codeNode The underlying CodeNode model
 * @param channelCapacity Buffer capacity for output channel
 * @param generate The generator block that receives an emit function
 */
class GeneratorRuntime<T : Any>(
    codeNode: CodeNode,
    private val channelCapacity: Int = Channel.BUFFERED,
    private val generate: ContinuousGeneratorBlock<T>
) : NodeRuntime<T>(codeNode) {

    init {
        // Create output channel with specified capacity
        outputChannel = Channel(channelCapacity)
    }

    /**
     * Starts the generator's continuous emission loop.
     *
     * The processingBlock parameter is ignored - the generator uses
     * its own generate block configured at construction time.
     *
     * @param scope CoroutineScope to launch the generator job in
     * @param processingBlock Ignored - generator uses its own block
     */
    override fun start(scope: CoroutineScope, processingBlock: suspend () -> Unit) {
        // Cancel existing job if running
        nodeControlJob?.cancel()

        // Transition to RUNNING state
        executionState = ExecutionState.RUNNING

        // Launch the generator job
        nodeControlJob = scope.launch {
            try {
                // Create emit function that sends to output channel
                val emit: suspend (T) -> Unit = { value ->
                    outputChannel?.send(value)
                }

                // Run the generator block with emit function
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
