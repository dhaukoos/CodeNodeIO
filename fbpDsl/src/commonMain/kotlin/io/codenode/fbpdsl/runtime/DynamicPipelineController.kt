/*
 * DynamicPipelineController - ModuleController for dynamically-built pipelines
 * Manages lifecycle, attenuation, and observers for dynamic pipelines
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowExecutionStatus
import io.codenode.fbpdsl.model.FlowGraph
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Implements ModuleController for dynamically-built pipelines.
 *
 * On each start(), reads the current FlowGraph, validates it, builds a fresh
 * DynamicPipeline, and runs it. Supports all lifecycle operations:
 * start/stop/pause/resume/attenuation/observers.
 *
 * @param flowGraphProvider Returns the current canvas FlowGraph on each call.
 *        Called on every start() to pick up canvas changes.
 * @param lookup Function to resolve node names to CodeNodeDefinitions
 * @param onReset Optional callback invoked during reset() to clear module-specific
 *        state (e.g., StopWatchState.reset()). The dynamic pipeline is generic and
 *        does not know about module state objects, so modules that need reset behavior
 *        must provide this callback.
 */
class DynamicPipelineController(
    private val flowGraphProvider: () -> FlowGraph,
    private val lookup: NodeDefinitionLookup,
    private val onReset: (() -> Unit)? = null
) : ModuleController {

    private val registry = RuntimeRegistry()
    private var pipeline: DynamicPipeline? = null
    private var flowScope: CoroutineScope? = null
    private var currentFlowGraph: FlowGraph = flowGraphProvider()

    /**
     * The active per-pipeline [CoroutineScope] when the controller is running, or
     * `null` before [start] / after [stop] / [cleanupPipeline].
     *
     * Added by feature 087 (Decision 8 — MVI for UI-FBP). Generated `{Name}Runtime`
     * factories launch source-port emissions on this scope so they share the
     * pipeline's exception handler + supervisor lifetime. Read-only by design;
     * callers MUST NOT cancel it directly (use [stop] instead).
     */
    val coroutineScope: CoroutineScope?
        get() = flowScope

    private var attenuationDelayMs: Long? = null
    private var emissionObserver: ((String, Int) -> Unit)? = null
    private var valueObserver: ((String, Int, Any?) -> Unit)? = null

    private val _executionState = MutableStateFlow(ExecutionState.IDLE)
    override val executionState: StateFlow<ExecutionState> = _executionState.asStateFlow()

    /** Validation error message from the last failed start(), or null if no error. */
    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()

    override fun start(): FlowGraph {
        _validationError.value = null

        // Clean up any previously running pipeline before rebuilding
        if (pipeline != null) {
            cleanupPipeline()
        }

        // Re-read the current canvas FlowGraph (picks up any canvas changes since last run)
        currentFlowGraph = flowGraphProvider()

        // Validate before building
        val result = DynamicPipelineBuilder.validate(currentFlowGraph, lookup)
        if (!result.isValid) {
            val errorMsg = result.errors.joinToString("\n") { it.message }
            _validationError.value = errorMsg
            _executionState.value = ExecutionState.IDLE
            return currentFlowGraph
        }

        // Build and start
        val newPipeline = DynamicPipelineBuilder.build(currentFlowGraph, lookup)
        pipeline = newPipeline

        // Set registry on all runtimes
        newPipeline.setRegistry(registry)

        // Apply stored attenuation and observers
        newPipeline.setAttenuationDelay(attenuationDelayMs)
        applyEmissionObserver(newPipeline)
        applyValueObserver(newPipeline)

        // Create scope with exception handler to surface runtime errors in the UI
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            _validationError.value = "Pipeline runtime error: ${throwable.message}"
            _executionState.value = ExecutionState.IDLE
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)
        flowScope = scope

        _executionState.value = ExecutionState.RUNNING

        scope.launch {
            newPipeline.start(scope)
        }

        return currentFlowGraph
    }

    override fun stop(): FlowGraph {
        cleanupPipeline()
        _executionState.value = ExecutionState.IDLE
        return currentFlowGraph
    }

    override fun pause(): FlowGraph {
        registry.pauseAll()
        _executionState.value = ExecutionState.PAUSED
        return currentFlowGraph
    }

    override fun resume(): FlowGraph {
        registry.resumeAll()
        _executionState.value = ExecutionState.RUNNING
        return currentFlowGraph
    }

    override fun reset(): FlowGraph {
        stop()
        onReset?.invoke()
        return currentFlowGraph
    }

    override fun setAttenuationDelay(ms: Long?) {
        attenuationDelayMs = ms
        pipeline?.setAttenuationDelay(ms)
    }

    override fun setEmissionObserver(observer: ((String, Int) -> Unit)?) {
        emissionObserver = observer
        pipeline?.let { applyEmissionObserver(it) }
    }

    override fun setValueObserver(observer: ((String, Int, Any?) -> Unit)?) {
        valueObserver = observer
        pipeline?.let { applyValueObserver(it) }
    }

    /**
     * Returns a snapshot derived from the currently-tracked FlowGraph.
     * The `overallState` is taken from the controller's own [executionState]
     * tracking (since per-node states in the FlowGraph are not mutated by
     * the dynamic-pipeline path); all count fields come from the FlowGraph.
     */
    override fun getStatus(): FlowExecutionStatus {
        val base = FlowExecutionStatus.fromFlowGraph(currentFlowGraph)
        return base.copy(overallState = _executionState.value)
    }

    private fun applyEmissionObserver(pipeline: DynamicPipeline) {
        pipeline.setEmissionObserver(emissionObserver)
    }

    private fun applyValueObserver(pipeline: DynamicPipeline) {
        pipeline.setValueObserver(valueObserver)
    }

    private fun cleanupPipeline() {
        pipeline?.stop()
        pipeline = null
        registry.clear()
        flowScope?.cancel()
        flowScope = null
    }
}
