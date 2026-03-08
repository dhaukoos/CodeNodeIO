/*
 * RuntimeSession - Orchestrates runtime preview execution
 * Manages module lifecycle, attenuation propagation, and execution state
 * License: Apache 2.0
 */

package io.codenode.circuitsimulator

import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.runtime.ModuleController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
 * - Data flow animation (dot animation along connections)
 *
 * @param controller The module's controller implementing ModuleController
 * @param viewModel The module's ViewModel, stored opaquely for preview providers to cast
 * @param flowGraph The module's FlowGraph for connection lookups (animation)
 */
class RuntimeSession(
    private val controller: ModuleController,
    val viewModel: Any,
    private val flowGraph: FlowGraph? = null
) {

    private val _executionState = MutableStateFlow(ExecutionState.IDLE)
    /** Current execution state of the runtime session */
    val executionState: StateFlow<ExecutionState> = _executionState.asStateFlow()

    private val _attenuationDelayMs = MutableStateFlow(0L)
    /** Current attenuation delay in milliseconds (0 = max speed, up to 2000ms) */
    val attenuationDelayMs: StateFlow<Long> = _attenuationDelayMs.asStateFlow()

    /** Animation controller managing dot animations along connections */
    val animationController = DataFlowAnimationController()

    /** Debugger for capturing per-connection transit snapshots */
    val debugger = DataFlowDebugger()

    private val _animateDataFlow = MutableStateFlow(false)
    /** Whether data flow animation is enabled */
    val animateDataFlow: StateFlow<Boolean> = _animateDataFlow.asStateFlow()

    /** Minimum attenuation (ms) required to enable animation */
    val animationAttenuationThreshold: Long = 500L

    /** Tracks whether the controller was already running before RuntimeSession.start() */
    private var preStarted = false

    private var animationScope: CoroutineScope? = null

    /**
     * Enables or disables data flow animation.
     * Animation can only be enabled when attenuation >= [animationAttenuationThreshold].
     * When disabled, clears the emission observer on the controller.
     *
     * @param enabled true to enable animation, false to disable
     */
    fun setAnimateDataFlow(enabled: Boolean) {
        if (enabled && _attenuationDelayMs.value < animationAttenuationThreshold) {
            return
        }

        _animateDataFlow.value = enabled

        if (enabled && flowGraph != null) {
            val observer = animationController.createEmissionObserver(
                flowGraph = flowGraph,
                attenuationMs = { _attenuationDelayMs.value }
            )
            controller.setEmissionObserver(observer)

            // Wire value observer for debug snapshots
            val valueObserver = debugger.createValueObserver(flowGraph)
            controller.setValueObserver(valueObserver)

            // Start frame loop if execution is already running
            if (_executionState.value == ExecutionState.RUNNING || _executionState.value == ExecutionState.PAUSED) {
                animationScope?.cancel()
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
                animationScope = scope
                animationController.startFrameLoop(scope)
            }
        } else {
            controller.setEmissionObserver(null)
            controller.setValueObserver(null)
            animationController.clear()
            animationController.stopFrameLoop()
            debugger.clear()
            animationScope?.cancel()
            animationScope = null
        }
    }

    /**
     * Starts the flow graph execution.
     * Only valid when state is IDLE.
     *
     * If the controller is already running (e.g., event-driven modules like UserProfiles
     * that are pre-started in the factory), skips the redundant controller.start() call
     * and just wires up animation and syncs execution state.
     */
    fun start() {
        if (_executionState.value != ExecutionState.IDLE) return

        val alreadyRunning = controller.executionState.value == ExecutionState.RUNNING
        preStarted = alreadyRunning

        controller.setAttenuationDelay(_attenuationDelayMs.value)

        // Wire emission observer before start so runtimes have it when they begin
        if (_animateDataFlow.value && flowGraph != null) {
            val observer = animationController.createEmissionObserver(
                flowGraph = flowGraph,
                attenuationMs = { _attenuationDelayMs.value }
            )
            controller.setEmissionObserver(observer)

            // Wire value observer for debug snapshots
            val valueObserver = debugger.createValueObserver(flowGraph)
            controller.setValueObserver(valueObserver)

            animationScope?.cancel()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            animationScope = scope
            animationController.startFrameLoop(scope)
        }

        if (!alreadyRunning) {
            controller.start()
        }
        _executionState.value = ExecutionState.RUNNING
    }

    /**
     * Stops the flow graph execution and resets state.
     * Valid when state is RUNNING or PAUSED.
     *
     * If the controller was already running before RuntimeSession.start() was called
     * (pre-started modules), only clears animation state without resetting the controller,
     * preserving the module's running state and data.
     */
    fun stop() {
        if (_executionState.value == ExecutionState.IDLE) return

        animationController.clear()
        animationController.stopFrameLoop()
        controller.setEmissionObserver(null)
        controller.setValueObserver(null)
        debugger.clear()
        animationScope?.cancel()
        animationScope = null

        if (!preStarted) {
            controller.reset()
        }
        _executionState.value = ExecutionState.IDLE
    }

    /**
     * Pauses the flow graph execution.
     * Only valid when state is RUNNING.
     */
    fun pause() {
        if (_executionState.value != ExecutionState.RUNNING) return
        controller.pause()
        animationController.pause()
        _executionState.value = ExecutionState.PAUSED
    }

    /**
     * Resumes the flow graph execution from paused state.
     * Only valid when state is PAUSED.
     */
    fun resume() {
        if (_executionState.value != ExecutionState.PAUSED) return
        controller.resume()
        animationController.resume()
        _executionState.value = ExecutionState.RUNNING
    }

    /**
     * Sets the attenuation delay for the generator runtime.
     * Clamped to [0, 2000] range.
     * Takes effect on the next tick cycle.
     * Auto-disables animation if new value < threshold.
     *
     * @param ms Delay in milliseconds (0 = no delay/max speed)
     */
    fun setAttenuation(ms: Long) {
        val clamped = ms.coerceIn(0L, 2000L)
        _attenuationDelayMs.value = clamped
        controller.setAttenuationDelay(clamped)

        if (clamped < animationAttenuationThreshold && _animateDataFlow.value) {
            setAnimateDataFlow(false)
        }
    }
}
