/*
 * RuntimeSession - Orchestrates runtime preview execution
 * Manages module lifecycle, attenuation propagation, and execution state
 * License: Apache 2.0
 */

package io.codenode.circuitsimulator

import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.runtime.ModuleController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Orchestrates runtime preview execution within the graphEditor.
 *
 * Manages:
 * - Module controller lifecycle (start/stop/pause/resume)
 * - Module-specific ViewModel for UI binding (opaque to RuntimeSession)
 * - Attenuation delay propagation to runtime nodes
 * - Execution state transitions (IDLE, RUNNING, PAUSED)
 *
 * @param controller The module's controller implementing ModuleController
 * @param viewModel The module's ViewModel, stored opaquely for preview providers to cast
 */
class RuntimeSession(
    private val controller: ModuleController,
    val viewModel: Any
) {

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
