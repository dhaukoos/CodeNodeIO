/*
 * Out2GeneratorRuntime - Runtime for generator nodes with 2 outputs
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
 * Specialized NodeRuntime for generator nodes with 2 outputs.
 *
 * Extends NodeRuntime with multi-output generator behavior:
 * - Creates two output channels with specified capacity
 * - Runs the generator block with an emit function that receives ProcessResult2
 * - Distributes non-null values to appropriate output channels
 * - Closes output channels when generator stops
 *
 * @param U Type of first output
 * @param V Type of second output
 * @param codeNode The underlying CodeNode model
 * @param channelCapacity Buffer capacity for output channels
 * @param generate The generator block that receives an emit function (existing mode)
 * @param tickIntervalMs Milliseconds between tick invocations (timed tick mode)
 * @param tickBlock Function called once per interval, returns values to emit (timed tick mode)
 */
class Out2GeneratorRuntime<U : Any, V : Any>(
    codeNode: CodeNode,
    private val channelCapacity: Int = Channel.BUFFERED,
    private val generate: Out2GeneratorBlock<U, V>? = null
) : NodeRuntime<U>(codeNode) {

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
     * Starts the generator's emission loop.
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

        // Recreate output channels (previous ones may have been closed on stop)
        outputChannel1 = Channel(channelCapacity)
        outputChannel2 = Channel(channelCapacity)

        // Transition to RUNNING state
        executionState = ExecutionState.RUNNING

        // Register with registry for centralized lifecycle control
        registry?.register(this)

        // Launch the generator job
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
                        result.out1?.let { out1.send(it) }
                        result.out2?.let { out2.send(it) }
                    }
                }

                // Run the generator block
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
