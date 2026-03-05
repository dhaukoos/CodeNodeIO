/*
 * RuntimeSession - Orchestrates runtime preview execution
 * Manages StopWatch lifecycle, attenuation propagation, and execution state
 * License: Apache 2.0
 */

package io.codenode.circuitsimulator

import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.stopwatch.generated.StopWatchController
import io.codenode.stopwatch.generated.StopWatchControllerAdapter
import io.codenode.stopwatch.StopWatchViewModel
import io.codenode.stopwatch.stopWatchFlowGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Orchestrates runtime preview execution within the graphEditor.
 *
 * Manages:
 * - StopWatchController creation and lifecycle
 * - StopWatchViewModel for UI binding
 * - Attenuation delay propagation to generator runtimes
 * - Execution state transitions (IDLE, RUNNING, PAUSED)
 */
class RuntimeSession {

    private val controller = StopWatchController(stopWatchFlowGraph)
    private val adapter = StopWatchControllerAdapter(controller)

    /** ViewModel for binding to the StopWatch UI composable */
    val viewModel = StopWatchViewModel(adapter)

    private val _executionState = MutableStateFlow(ExecutionState.IDLE)
    /** Current execution state of the runtime session */
    val executionState: StateFlow<ExecutionState> = _executionState.asStateFlow()

    private val _attenuationDelayMs = MutableStateFlow(0L)
    /** Current attenuation delay in milliseconds (0 = max speed, up to 2000ms) */
    val attenuationDelayMs: StateFlow<Long> = _attenuationDelayMs.asStateFlow()

    /**
     * Starts the flow graph execution.
     * Only valid when state is IDLE.
     */
    fun start() {
        if (_executionState.value != ExecutionState.IDLE) return
        controller.setAttenuationDelay(_attenuationDelayMs.value)
        controller.start()
        _executionState.value = ExecutionState.RUNNING
    }

    /**
     * Stops the flow graph execution and resets state.
     * Valid when state is RUNNING or PAUSED.
     */
    fun stop() {
        if (_executionState.value == ExecutionState.IDLE) return
        controller.reset()
        _executionState.value = ExecutionState.IDLE
    }

    /**
     * Pauses the flow graph execution.
     * Only valid when state is RUNNING.
     */
    fun pause() {
        if (_executionState.value != ExecutionState.RUNNING) return
        controller.pause()
        _executionState.value = ExecutionState.PAUSED
    }

    /**
     * Resumes the flow graph execution from paused state.
     * Only valid when state is PAUSED.
     */
    fun resume() {
        if (_executionState.value != ExecutionState.PAUSED) return
        controller.resume()
        _executionState.value = ExecutionState.RUNNING
    }

    /**
     * Sets the attenuation delay for the generator runtime.
     * Clamped to [0, 2000] range.
     * Takes effect on the next tick cycle.
     *
     * @param ms Delay in milliseconds (0 = no delay/max speed)
     */
    fun setAttenuation(ms: Long) {
        val clamped = ms.coerceIn(0L, 2000L)
        _attenuationDelayMs.value = clamped
        controller.setAttenuationDelay(clamped)
    }
}
