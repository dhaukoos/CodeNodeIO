/*
 * StopWatchControllerAdapter
 * Adapter wrapping StopWatchController to implement StopWatchControllerInterface
 * License: Apache 2.0
 */

package io.codenode.stopwatch.generated

import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowGraph
import kotlinx.coroutines.flow.StateFlow

/**
 * Adapter that wraps the generated StopWatchController to implement
 * StopWatchControllerInterface.
 *
 * This adapter enables the StopWatchViewModel to work with both the
 * real controller (via this adapter) and test fakes.
 *
 * @param controller The generated StopWatchController to wrap
 */
class StopWatchControllerAdapter(
    private val controller: StopWatchController
) : StopWatchControllerInterface {

    override val seconds: StateFlow<Int>
        get() = controller.seconds

    override val minutes: StateFlow<Int>
        get() = controller.minutes

    override val executionState: StateFlow<ExecutionState>
        get() = controller.executionState

    override fun start(): FlowGraph = controller.start()

    override fun stop(): FlowGraph = controller.stop()

    override fun reset(): FlowGraph = controller.reset()

    override fun pause(): FlowGraph = controller.pause()

    override fun resume(): FlowGraph = controller.resume()
}
