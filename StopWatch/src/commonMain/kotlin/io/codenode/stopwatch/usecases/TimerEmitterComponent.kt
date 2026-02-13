/*
 * TimerEmitterComponent - UseCase for TimerEmitter CodeNode
 * Implements the timer tick logic for the StopWatch virtual circuit
 * License: Apache 2.0
 */

package io.codenode.stopwatch.usecases

import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.InformationPacket
import io.codenode.fbpdsl.model.InformationPacketFactory
import io.codenode.fbpdsl.model.ProcessingLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
 * This component implements the timer logic from the original StopWatch LaunchedEffect:
 * - Emits elapsedSeconds every speedAttenuation milliseconds
 * - Rolls seconds to 0 and increments minutes at 60
 * - Only emits when executionState == RUNNING
 *
 * Type: GENERATOR (0 inputs, 2 outputs: elapsedSeconds, elapsedMinutes)
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

    // Output SharedFlow for emitting to downstream nodes (legacy)
    val output = MutableSharedFlow<TimerOutput>(replay = 1)

    // Output channel for FBP point-to-point semantics (assigned by flow wiring)
    var outputChannel: SendChannel<Any>? = null

    // Observable state flows for elapsed time
    private val _elapsedSeconds = MutableStateFlow(initialSeconds)
    val elapsedSecondsFlow: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _elapsedMinutes = MutableStateFlow(initialMinutes)
    val elapsedMinutesFlow: StateFlow<Int> = _elapsedMinutes.asStateFlow()

    // Execution state - controls whether timer is running
    var executionState: ExecutionState = ExecutionState.IDLE

    // Job tracking for cancellation
    private var timerJob: Job? = null

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
     *
     * @param scope CoroutineScope to run the timer in
     */
    suspend fun start(scope: CoroutineScope) {
        // Cancel any existing timer job
        timerJob?.cancel()

        // Launch coroutine tick loop
        timerJob = scope.launch {
            // Check executionState in while loop condition
            while (isActive && executionState == ExecutionState.RUNNING) {
                // delay for speedAttenuation interval
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

                // Update state flows
                _elapsedSeconds.value = newSeconds
                _elapsedMinutes.value = newMinutes

                // Emit to output for downstream connections
                val timerOutput = TimerOutput(newSeconds, newMinutes)
                output.emit(timerOutput)

                // Also send to channel if wired
                try {
                    outputChannel?.send(timerOutput)
                } catch (e: ClosedSendChannelException) {
                    // Channel closed - graceful shutdown
                }
            }
        }
    }

    /**
     * Stops the timer.
     */
    fun stop() {
        executionState = ExecutionState.IDLE
        timerJob?.cancel()
        timerJob = null
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
