/*
 * FakeStopWatchController
 * Test double for StopWatchController
 * License: Apache 2.0
 */

package io.codenode.mobileapp.viewmodel

import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake implementation of StopWatchController for unit testing.
 *
 * Provides controllable StateFlow properties and tracks method calls
 * without requiring a real FlowGraph or coroutine execution.
 */
class FakeStopWatchController : StopWatchControllerInterface {

    private val _elapsedSeconds = MutableStateFlow(0)
    override val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _elapsedMinutes = MutableStateFlow(0)
    override val elapsedMinutes: StateFlow<Int> = _elapsedMinutes.asStateFlow()

    private val _executionState = MutableStateFlow(ExecutionState.IDLE)
    override val executionState: StateFlow<ExecutionState> = _executionState.asStateFlow()

    // Tracking flags for method calls
    var startCalled = false
        private set
    var stopCalled = false
        private set
    var resetCalled = false
        private set
    var pauseCalled = false
        private set
    var resumeCalled = false
        private set

    // Minimal FlowGraph for test returns
    private val emptyFlowGraph = FlowGraph(
        id = "test-flow",
        name = "TestFlow",
        version = "1.0.0"
    )

    /**
     * Simulates starting the stopwatch.
     * Sets startCalled flag and transitions to RUNNING state.
     */
    override fun start(): FlowGraph {
        startCalled = true
        _executionState.value = ExecutionState.RUNNING
        return emptyFlowGraph
    }

    /**
     * Simulates stopping the stopwatch.
     * Sets stopCalled flag and transitions to IDLE state.
     */
    override fun stop(): FlowGraph {
        stopCalled = true
        _executionState.value = ExecutionState.IDLE
        return emptyFlowGraph
    }

    /**
     * Simulates resetting the stopwatch.
     * Sets resetCalled flag, clears elapsed time, and transitions to IDLE state.
     */
    override fun reset(): FlowGraph {
        resetCalled = true
        _elapsedSeconds.value = 0
        _elapsedMinutes.value = 0
        _executionState.value = ExecutionState.IDLE
        return emptyFlowGraph
    }

    /**
     * Simulates pausing the stopwatch.
     * Sets pauseCalled flag and transitions to PAUSED state.
     */
    override fun pause(): FlowGraph {
        pauseCalled = true
        _executionState.value = ExecutionState.PAUSED
        return emptyFlowGraph
    }

    /**
     * Simulates resuming the stopwatch from paused state.
     * Sets resumeCalled flag and transitions to RUNNING state.
     */
    override fun resume(): FlowGraph {
        resumeCalled = true
        _executionState.value = ExecutionState.RUNNING
        return emptyFlowGraph
    }

    // Test helper methods for controlling state

    /**
     * Sets elapsed seconds for testing state observation.
     */
    fun setElapsedSeconds(seconds: Int) {
        _elapsedSeconds.value = seconds
    }

    /**
     * Sets elapsed minutes for testing state observation.
     */
    fun setElapsedMinutes(minutes: Int) {
        _elapsedMinutes.value = minutes
    }

    /**
     * Sets execution state for testing state observation.
     */
    fun setExecutionState(state: ExecutionState) {
        _executionState.value = state
    }

    /**
     * Resets all tracking flags for test isolation.
     */
    fun resetFlags() {
        startCalled = false
        stopCalled = false
        resetCalled = false
        pauseCalled = false
        resumeCalled = false
    }
}
