/*
 * StopWatchViewModel
 * ViewModel bridging FlowGraph domain logic with Compose UI
 * License: Apache 2.0
 */

package io.codenode.mobileapp.viewmodel

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
     * Current elapsed seconds (0-59).
     * Delegated from controller's StateFlow.
     * Updates when the timer ticks.
     */
    val elapsedSeconds: StateFlow<Int> = controller.elapsedSeconds

    /**
     * Current elapsed minutes.
     * Delegated from controller's StateFlow.
     * Updates when seconds roll over to a new minute.
     */
    val elapsedMinutes: StateFlow<Int> = controller.elapsedMinutes

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
}
