/*
 * StopWatchControllerInterface
 * Interface for StopWatch controller implementations
 * License: Apache 2.0
 */

package io.codenode.stopwatch.generated

import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowGraph
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface defining the contract for StopWatch controllers.
 *
 * This interface enables dependency injection and testing by allowing
 * both the real StopWatchController and test fakes to be used with
 * StopWatchViewModel.
 */
interface StopWatchControllerInterface {

    /**
     * Current elapsed seconds (0-59) as observable StateFlow.
     */
    val elapsedSeconds: StateFlow<Int>

    /**
     * Current elapsed minutes as observable StateFlow.
     */
    val elapsedMinutes: StateFlow<Int>

    /**
     * Current execution state (IDLE, RUNNING, PAUSED) as observable StateFlow.
     */
    val executionState: StateFlow<ExecutionState>

    /**
     * Starts the stopwatch.
     * @return Updated FlowGraph
     */
    fun start(): FlowGraph

    /**
     * Stops the stopwatch.
     * @return Updated FlowGraph
     */
    fun stop(): FlowGraph

    /**
     * Resets the stopwatch to initial state.
     * @return Updated FlowGraph
     */
    fun reset(): FlowGraph

    /**
     * Pauses the stopwatch.
     * Transitions to PAUSED state and halts timer updates.
     * @return Updated FlowGraph
     */
    fun pause(): FlowGraph

    /**
     * Resumes the stopwatch from paused state.
     * Transitions back to RUNNING state and continues timer updates.
     * @return Updated FlowGraph
     */
    fun resume(): FlowGraph
}
