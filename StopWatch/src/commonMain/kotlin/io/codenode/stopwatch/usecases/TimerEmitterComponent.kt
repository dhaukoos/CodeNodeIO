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
import io.codenode.fbpdsl.runtime.Out2GeneratorBlock
import io.codenode.fbpdsl.runtime.Out2GeneratorRuntime
import io.codenode.fbpdsl.runtime.ProcessResult2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive

/**
 * TimerEmitter UseCase - Generator node that emits elapsed time at regular intervals.
 *
 * This component uses CodeNodeFactory.createOut2Generator to create an
 * Out2GeneratorRuntime that manages the timer loop lifecycle with two typed output channels.
 *
 * Features:
 * - Emits elapsedSeconds on outputChannel1 every speedAttenuation milliseconds
 * - Emits elapsedMinutes on outputChannel2 every speedAttenuation milliseconds
 * - Rolls seconds to 0 and increments minutes at 60
 * - Only emits when executionState == RUNNING
 * - Exposes StateFlows for UI observation
 *
 * Type: GENERATOR (0 inputs, 2 outputs: Int for seconds, Int for minutes)
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

    fun incrementer(oldSeconds: Int, oldMinutes: Int ): ProcessResult2<Int, Int> {
        // Increment seconds with rollover logic
        var newSeconds = oldSeconds + 1
        var newMinutes = oldMinutes

        if (newSeconds >= 60) {
            newSeconds = 0
            newMinutes += 1
        }
        // Return both output channels for downstream nodes
        return ProcessResult2.both(newSeconds, newMinutes)
    }

    val generator: Out2GeneratorBlock<Int, Int>  = { emit ->
        // Continuous timer loop - runs until stopped
        while (currentCoroutineContext().isActive && executionState == ExecutionState.RUNNING) {
            // Delay for tick interval
            if (speedAttenuation > 0) {
                delay(speedAttenuation)
                // Check state again after delay (may have changed during delay)
                if (executionState != ExecutionState.RUNNING) break
            }

            val result = incrementer(_elapsedSeconds.value, _elapsedMinutes.value)

            // Update state flows for UI observation
            _elapsedSeconds.value = result.out1!!
            _elapsedMinutes.value = result.out2!!

            // Emit to both output channels for downstream nodes
            emit(result)
        }
    }

    /**
     * Out2GeneratorRuntime created via factory method.
     * Manages the timer loop lifecycle with proper dual-channel handling.
     * outputChannel1: seconds (Int), outputChannel2: minutes (Int)
     */
    private val generatorRuntime: Out2GeneratorRuntime<Int, Int> = CodeNodeFactory.createOut2Generator(
        name = "TimerEmitter",
        description = "Emits elapsed time at regular intervals on two typed channels",
        generate = generator
    )

    /**
     * CodeNode reference - delegates to generatorRuntime.
     */
    val codeNode: CodeNode
        get() = generatorRuntime.codeNode

    /**
     * First output channel (seconds) for FBP point-to-point semantics with backpressure.
     * The Out2GeneratorRuntime creates its own buffered channels internally.
     * Read-only - wiring is done by assigning to downstream node's inputChannel.
     */
    val outputChannel1: Channel<Int>?
        get() = generatorRuntime.outputChannel1

    /**
     * Second output channel (minutes) for FBP point-to-point semantics with backpressure.
     * The Out2GeneratorRuntime creates its own buffered channels internally.
     * Read-only - wiring is done by assigning to downstream node's inputChannel.
     */
    val outputChannel2: Channel<Int>?
        get() = generatorRuntime.outputChannel2

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
