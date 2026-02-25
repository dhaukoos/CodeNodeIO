/*
 * StopWatchViewModel
 * ViewModel bridging FlowGraph domain logic with Compose UI
 * License: Apache 2.0
 */

package io.codenode.stopwatch.generated

import androidx.lifecycle.ViewModel
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowGraph
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for the StopWatch composable.
 * Bridges FlowGraph domain logic with Compose UI.
 *
 * This ViewModel is a thin facade that delegates all state observation
 * and actions to the underlying StopWatchController. It exposes the
 * controller's StateFlow properties directly, ensuring UI updates
 * automatically when the FlowGraph state changes.
 *
 * @param controller The StopWatchController that manages FlowGraph execution
 */
class StopWatchViewModel(
    private val controller: StopWatchControllerInterface
) : ViewModel() {

    /**
     * Current seconds (0-59).
     * Delegated from controller's StateFlow.
     * Updates when the timer ticks.
     */
    val seconds: StateFlow<Int> = controller.seconds

    /**
     * Current minutes.
     * Delegated from controller's StateFlow.
     * Updates when seconds roll over to a new minute.
     */
    val minutes: StateFlow<Int> = controller.minutes

    /**
     * Current execution state (IDLE, RUNNING, PAUSED).
     * Delegated from controller's StateFlow.
     * Updates when start(), stop(), or reset() is called.
     */
    val executionState: StateFlow<ExecutionState> = controller.executionState

    /**
     * Starts the stopwatch.
     * Delegates to controller.start() which transitions the FlowGraph to RUNNING state.
     *
     * @return Updated FlowGraph
     */
    fun start(): FlowGraph = controller.start()

    /**
     * Stops the stopwatch.
     * Delegates to controller.stop() which transitions the FlowGraph to IDLE state.
     *
     * @return Updated FlowGraph
     */
    fun stop(): FlowGraph = controller.stop()

    /**
     * Resets the stopwatch to initial state.
     * Delegates to controller.reset() which clears elapsed time and stops execution.
     *
     * @return Updated FlowGraph
     */
    fun reset(): FlowGraph = controller.reset()

    /**
     * Pauses the stopwatch.
     * Delegates to controller.pause() which transitions the FlowGraph to PAUSED state.
     * Timer display freezes at current value.
     *
     * @return Updated FlowGraph
     */
    fun pause(): FlowGraph = controller.pause()

    /**
     * Resumes the stopwatch from paused state.
     * Delegates to controller.resume() which transitions the FlowGraph back to RUNNING state.
     * Timer continues from where it was paused.
     *
     * @return Updated FlowGraph
     */
    fun resume(): FlowGraph = controller.resume()
}
