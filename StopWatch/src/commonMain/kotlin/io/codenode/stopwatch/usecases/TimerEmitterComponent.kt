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
import io.codenode.fbpdsl.runtime.Out2GeneratorRuntime
import io.codenode.fbpdsl.runtime.Out2TickBlock
import io.codenode.fbpdsl.runtime.ProcessResult2
import io.codenode.fbpdsl.runtime.RuntimeRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * TimerEmitter UseCase - Generator node that emits elapsed time at regular intervals.
 *
 * This component uses CodeNodeFactory.createTimedOut2Generator to create an
 * Out2GeneratorRuntime with a tick function that handles the timer increment logic.
 * The runtime manages the timed loop lifecycle, pause/resume, and channel distribution.
 *
 * Features:
 * - Emits elapsedSeconds on outputChannel1 every speedAttenuation milliseconds
 * - Emits elapsedMinutes on outputChannel2 every speedAttenuation milliseconds
 * - Rolls seconds to 0 and increments minutes at 60
 * - Pause/resume handled by the runtime's emit hook
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

    // Observable state flows for elapsed time
    private val _elapsedSeconds = MutableStateFlow(initialSeconds)
    val elapsedSecondsFlow: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _elapsedMinutes = MutableStateFlow(initialMinutes)
    val elapsedMinutesFlow: StateFlow<Int> = _elapsedMinutes.asStateFlow()

    /**
     * Tick function - pure business logic for each timer tick.
     * Increments seconds with rollover at 60, updates StateFlows,
     * and returns the result for channel distribution.
     */
    private val tick: Out2TickBlock<Int, Int> = {
        var newSeconds = _elapsedSeconds.value + 1
        var newMinutes = _elapsedMinutes.value

        if (newSeconds >= 60) {
            newSeconds = 0
            newMinutes += 1
        }

        _elapsedSeconds.value = newSeconds
        _elapsedMinutes.value = newMinutes

        ProcessResult2.both(newSeconds, newMinutes)
    }

    /**
     * Out2GeneratorRuntime created via timed factory method.
     * The runtime manages the timed loop, pause/resume hooks, and channel distribution.
     * outputChannel1: seconds (Int), outputChannel2: minutes (Int)
     */
    private val generatorRuntime: Out2GeneratorRuntime<Int, Int> = CodeNodeFactory.createTimedOut2Generator(
        name = "TimerEmitter",
        tickIntervalMs = speedAttenuation,
        description = "Emits elapsed time at regular intervals on two typed channels",
        tick = tick
    )

    val codeNode: CodeNode
        get() = generatorRuntime.codeNode

    val outputChannel1: Channel<Int>?
        get() = generatorRuntime.outputChannel1

    val outputChannel2: Channel<Int>?
        get() = generatorRuntime.outputChannel2

    var executionState: ExecutionState
        get() = generatorRuntime.executionState
        set(value) {
            generatorRuntime.executionState = value
        }

    var registry: RuntimeRegistry?
        get() = generatorRuntime.registry
        set(value) {
            generatorRuntime.registry = value
        }

    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        return mapOf(
            "elapsedSeconds" to InformationPacketFactory.create(_elapsedSeconds.value),
            "elapsedMinutes" to InformationPacketFactory.create(_elapsedMinutes.value)
        )
    }

    suspend fun start(scope: CoroutineScope) {
        generatorRuntime.start(scope) {}
    }

    fun stop() {
        generatorRuntime.stop()
    }

    fun reset() {
        stop()
        _elapsedSeconds.value = 0
        _elapsedMinutes.value = 0
    }
}
