/*
 * TimerEmitterComponent - UseCase for TimerEmitter CodeNode
 * Implements the timer tick logic for the StopWatch virtual circuit
 * License: Apache 2.0
 */

package io.codenode.stopwatch.usecases

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.runtime.ProcessResult2
import io.codenode.fbpdsl.runtime.RuntimeRegistry
import io.codenode.fbpdsl.runtime.SourceOut2Block
import io.codenode.fbpdsl.runtime.SourceOut2Runtime
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
 * This component uses CodeNodeFactory.createSourceOut2 to create a
 * SourceOut2Runtime with a generate block that handles the timer increment logic.
 * The runtime manages the loop lifecycle, pause/resume, and channel distribution.
 *
 * Features:
 * - Emits elapsedSeconds on outputChannel1 every speedAttenuation milliseconds
 * - Emits elapsedMinutes on outputChannel2 every speedAttenuation milliseconds
 * - Rolls seconds to 0 and increments minutes at 60
 * - Pause/resume handled by the runtime's emit hook
 * - Exposes StateFlows for UI observation
 *
 * Type: SOURCE (0 inputs, 2 outputs: Int for seconds, Int for minutes)
 *
 * @param speedAttenuation Tick interval in milliseconds (default: 1000ms = 1 second)
 * @param initialSeconds Initial seconds value (for testing)
 * @param initialMinutes Initial minutes value (for testing)
 */
class TimerEmitterComponent(
    private val speedAttenuation: Long = 1000L,
    initialSeconds: Int = 0,
    initialMinutes: Int = 0
) {

    // Observable state flows for elapsed time
    private val _elapsedSeconds = MutableStateFlow(initialSeconds)
    val elapsedSecondsFlow: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _elapsedMinutes = MutableStateFlow(initialMinutes)
    val elapsedMinutesFlow: StateFlow<Int> = _elapsedMinutes.asStateFlow()

    /**
     * SourceOut2Runtime created via factory method.
     * The runtime manages the loop, pause/resume hooks, and channel distribution.
     * outputChannel1: seconds (Int), outputChannel2: minutes (Int)
     */
    private val generatorRuntime: SourceOut2Runtime<Int, Int> = CodeNodeFactory.createSourceOut2(
        name = "TimerEmitter",
        description = "Emits elapsed time at regular intervals on two typed channels",
        generate = { emit ->
            while (currentCoroutineContext().isActive) {
                delay(speedAttenuation)

                var newSeconds = _elapsedSeconds.value + 1
                var newMinutes = _elapsedMinutes.value

                if (newSeconds >= 60) {
                    newSeconds = 0
                    newMinutes += 1
                }

                _elapsedSeconds.value = newSeconds
                _elapsedMinutes.value = newMinutes

                emit(ProcessResult2.both(newSeconds, newMinutes))
            }
        }
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
