/*
 * TimerEmitterComponent - UseCase for TimerEmitter CodeNode
 * Implements the timer tick logic for the StopWatch virtual circuit
 * License: Apache 2.0
 */

package io.codenode.stopwatch.usecases

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.InformationPacket
import io.codenode.fbpdsl.model.InformationPacketFactory
import io.codenode.fbpdsl.model.ProcessingLogic
import io.codenode.fbpdsl.runtime.GeneratorRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive

/**
 * Data class for timer output values
 */
data class TimerOutput(
    val elapsedSeconds: Int,
    val elapsedMinutes: Int
)

/**
 * TimerEmitter UseCase - Generator node that emits elapsed time at regular intervals.
 *
 * This component uses CodeNodeFactory.createContinuousGenerator to create a
 * GeneratorRuntime that manages the timer loop lifecycle.
 *
 * Features:
 * - Emits elapsedSeconds every speedAttenuation milliseconds
 * - Rolls seconds to 0 and increments minutes at 60
 * - Only emits when executionState == RUNNING
 * - Exposes StateFlows for UI observation
 *
 * Type: GENERATOR (0 inputs, 1 output: TimerOutput)
 *
 * @param speedAttenuation Tick interval in milliseconds (default: 1000ms = 1 second)
 * @param initialSeconds Initial seconds value (for testing)
 * @param initialMinutes Initial minutes value (for testing)
 */
class TimerEmitterComponent(
    private val speedAttenuation: Long = 1000L,
    initialSeconds: Int = 0,
    initialMinutes: Int = 0
) : ProcessingLogic {

    // Observable state flows for elapsed time - declared first for closure capture
    private val _elapsedSeconds = MutableStateFlow(initialSeconds)
    val elapsedSecondsFlow: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _elapsedMinutes = MutableStateFlow(initialMinutes)
    val elapsedMinutesFlow: StateFlow<Int> = _elapsedMinutes.asStateFlow()

    /**
     * GeneratorRuntime created via factory method.
     * Manages the timer loop lifecycle with proper channel handling.
     */
    private val generatorRuntime: GeneratorRuntime<TimerOutput> = CodeNodeFactory.createContinuousGenerator(
        name = "TimerEmitter",
        description = "Emits elapsed time at regular intervals"
    ) { emit ->
        // Continuous timer loop - runs until stopped
        while (currentCoroutineContext().isActive && executionState == ExecutionState.RUNNING) {
            // Delay for tick interval
            delay(speedAttenuation)

            // Check state again after delay (may have changed during delay)
            if (executionState != ExecutionState.RUNNING) break

            // Increment seconds with rollover logic
            var newSeconds = _elapsedSeconds.value + 1
            var newMinutes = _elapsedMinutes.value

            if (newSeconds >= 60) {
                newSeconds = 0
                newMinutes += 1
            }

            // Update state flows for UI observation
            _elapsedSeconds.value = newSeconds
            _elapsedMinutes.value = newMinutes

            // Emit to output channel for downstream nodes
            emit(TimerOutput(newSeconds, newMinutes))
        }
    }

    /**
     * CodeNode reference - delegates to generatorRuntime.
     */
    val codeNode: CodeNode
        get() = generatorRuntime.codeNode

    /**
     * Output channel for FBP point-to-point semantics with backpressure.
     * The GeneratorRuntime creates its own buffered channel.
     * Can be overwritten for external wiring if needed.
     */
    var outputChannel
        get() = generatorRuntime.outputChannel
        set(value) {
            generatorRuntime.outputChannel = value
        }

    /**
     * Execution state - delegated to GeneratorRuntime.
     */
    var executionState: ExecutionState
        get() = generatorRuntime.executionState
        set(value) {
            generatorRuntime.executionState = value
        }

    /**
     * ProcessingLogic implementation - generates timer output.
     * For continuous operation, use start() instead.
     */
    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        // Single invocation returns current state
        return mapOf(
            "elapsedSeconds" to InformationPacketFactory.create(_elapsedSeconds.value),
            "elapsedMinutes" to InformationPacketFactory.create(_elapsedMinutes.value)
        )
    }

    /**
     * Starts the continuous timer tick loop.
     * Delegates to GeneratorRuntime.start().
     *
     * @param scope CoroutineScope to run the timer in
     */
    suspend fun start(scope: CoroutineScope) {
        generatorRuntime.start(scope) {
            // Processing block is ignored - generator uses its own block
        }
    }

    /**
     * Stops the timer.
     * Delegates to GeneratorRuntime.stop().
     */
    fun stop() {
        generatorRuntime.stop()
    }

    /**
     * Resets the timer to zero.
     */
    fun reset() {
        stop()
        _elapsedSeconds.value = 0
        _elapsedMinutes.value = 0
    }
}
