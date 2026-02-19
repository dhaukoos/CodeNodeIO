/*
 * FilterRuntime - Specialized runtime for continuous filter nodes
 * Manages the filter loop that passes or drops values based on predicate
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.ExecutionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Specialized NodeRuntime for continuous filter nodes.
 *
 * Extends NodeRuntime with filter-specific behavior:
 * - Iterates over input channel receiving values
 * - Applies predicate to each value
 * - Sends values that pass predicate to output channel
 * - Drops values that fail predicate
 * - Handles channel closure gracefully
 *
 * @param T Type of values being filtered
 * @param codeNode The underlying CodeNode model
 * @param predicate Returns true to pass value, false to drop
 */
class FilterRuntime<T : Any>(
    codeNode: CodeNode,
    private val predicate: ContinuousFilterPredicate<T>
) : NodeRuntime(codeNode) {

    /**
     * Input channel for receiving data.
     */
    var inputChannel: ReceiveChannel<T>? = null

    /**
     * Output channel for emitting filtered data.
     */
    var outputChannel: SendChannel<T>? = null

    /**
     * Starts the filter's continuous processing loop.
     *
     * The processingBlock parameter is ignored - the filter uses
     * its own predicate configured at construction time.
     *
     * @param scope CoroutineScope to launch the filter job in
     * @param processingBlock Ignored - filter uses its own predicate
     */
    override fun start(scope: CoroutineScope, processingBlock: suspend () -> Unit) {
        // Cancel existing job if running
        nodeControlJob?.cancel()

        // Transition to RUNNING state
        executionState = ExecutionState.RUNNING

        // Register with registry for centralized lifecycle control
        registry?.register(this)

        // Launch the filter job
        nodeControlJob = scope.launch {
            try {
                // Get channels - return early if not set
                val inChannel = inputChannel ?: return@launch
                val outChannel = outputChannel ?: return@launch

                // Filter loop with pause support
                while (executionState != ExecutionState.IDLE) {
                    // Pause hook - wait while paused
                    while (executionState == ExecutionState.PAUSED) {
                        delay(10)
                    }
                    // Exit if stopped during pause
                    if (executionState == ExecutionState.IDLE) break

                    // Receive next value (suspends until available or channel closed)
                    val value = inChannel.receiveCatching().getOrNull() ?: break

                    // Apply predicate - only send if true
                    if (predicate(value)) {
                        outChannel.send(value)
                    }
                    // Otherwise drop the value
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
