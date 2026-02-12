/*
 * TimerEmitterComponentTest - TDD tests for TimerEmitter UseCase
 * Tests T048-T050: Timer emission behavior
 * License: Apache 2.0
 */

package io.codenode.stopwatch.usecases

import io.codenode.fbpdsl.model.ExecutionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD tests for TimerEmitter component.
 * These tests define the expected behavior BEFORE implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerEmitterComponentTest {

    /**
     * T048: TimerEmitter emits incrementing elapsedSeconds every speedAttenuation ms.
     *
     * Given: A TimerEmitter with speedAttenuation = 100ms and RUNNING state
     * When: 300ms of time passes
     * Then: elapsedSeconds should have incremented 3 times (0 -> 1 -> 2 -> 3)
     */
    @Test
    fun timerEmitter_emits_incrementing_elapsedSeconds_at_speedAttenuation_interval() = runTest {
        // Given
        val speedAttenuation = 100L
        val timerEmitter = TimerEmitterComponent(speedAttenuation = speedAttenuation)

        // Collect emitted seconds (drop initial StateFlow value of 0)
        val emittedSeconds = mutableListOf<Int>()
        val collectJob = launch {
            timerEmitter.elapsedSecondsFlow.drop(1).take(3).collect { seconds ->
                emittedSeconds.add(seconds)
            }
        }

        // When - start the timer
        timerEmitter.executionState = ExecutionState.RUNNING
        val startJob = launch {
            timerEmitter.start(this)
        }

        // Advance virtual time
        advanceTimeBy(350) // Allow for 3+ ticks

        // Then
        assertTrue(emittedSeconds.size >= 3, "Expected at least 3 emissions, got ${emittedSeconds.size}")
        assertEquals(1, emittedSeconds[0], "First tick emission should be 1")
        assertEquals(2, emittedSeconds[1], "Second tick emission should be 2")
        assertEquals(3, emittedSeconds[2], "Third tick emission should be 3")

        // Cleanup
        timerEmitter.stop()
        collectJob.cancel()
        startJob.cancel()
    }

    /**
     * T049: TimerEmitter rolls elapsedSeconds to 0 and increments elapsedMinutes at 60.
     *
     * Given: A TimerEmitter that has counted to 59 seconds
     * When: One more second tick occurs
     * Then: elapsedSeconds should reset to 0 and elapsedMinutes should increment to 1
     */
    @Test
    fun timerEmitter_rolls_seconds_to_zero_and_increments_minutes_at_60() = runTest {
        // Given
        val speedAttenuation = 10L // Fast tick for testing
        val timerEmitter = TimerEmitterComponent(
            speedAttenuation = speedAttenuation,
            initialSeconds = 59,
            initialMinutes = 0
        )

        // Track the seconds/minutes values after rollover
        var capturedSeconds = -1
        var capturedMinutes = -1

        val collectJob = launch {
            // We expect seconds to go 59 -> 0, and minutes to go 0 -> 1
            timerEmitter.elapsedSecondsFlow.collect { seconds ->
                if (seconds == 0 && capturedSeconds == -1) {
                    capturedSeconds = seconds
                    capturedMinutes = timerEmitter.elapsedMinutesFlow.first()
                }
            }
        }

        // When
        timerEmitter.executionState = ExecutionState.RUNNING
        val startJob = launch {
            timerEmitter.start(this)
        }

        // Advance time for at least 2 ticks (59 -> 0)
        advanceTimeBy(25)

        // Then
        assertEquals(0, capturedSeconds, "Seconds should roll over to 0")
        assertEquals(1, capturedMinutes, "Minutes should increment to 1")

        // Cleanup
        timerEmitter.stop()
        collectJob.cancel()
        startJob.cancel()
    }

    /**
     * T050: TimerEmitter stops emitting when executionState != RUNNING.
     *
     * Given: A running TimerEmitter
     * When: executionState changes to PAUSED or IDLE
     * Then: No more emissions should occur
     */
    @Test
    fun timerEmitter_stops_emitting_when_execution_state_is_not_running() = runTest {
        // Given
        val speedAttenuation = 50L
        val timerEmitter = TimerEmitterComponent(speedAttenuation = speedAttenuation)

        val emittedSeconds = mutableListOf<Int>()
        val collectJob = launch {
            timerEmitter.elapsedSecondsFlow.collect { seconds ->
                emittedSeconds.add(seconds)
            }
        }

        // Start the timer
        timerEmitter.executionState = ExecutionState.RUNNING
        val startJob = launch {
            timerEmitter.start(this)
        }

        // Let it tick a couple times
        advanceTimeBy(120) // ~2 ticks

        val countBeforeStop = emittedSeconds.size
        assertTrue(countBeforeStop >= 2, "Should have at least 2 emissions before stop")

        // When - change state to PAUSED
        timerEmitter.executionState = ExecutionState.PAUSED

        // Advance more time
        advanceTimeBy(200)

        // Then - no more emissions after state change
        val countAfterStop = emittedSeconds.size
        assertEquals(countBeforeStop, countAfterStop,
            "No new emissions should occur after PAUSED state. Before: $countBeforeStop, After: $countAfterStop")

        // Cleanup
        timerEmitter.stop()
        collectJob.cancel()
        startJob.cancel()
    }

    /**
     * Additional test: Verify initial state is correct.
     */
    @Test
    fun timerEmitter_has_zero_initial_values() = runTest {
        val timerEmitter = TimerEmitterComponent()

        assertEquals(0, timerEmitter.elapsedSecondsFlow.first(), "Initial seconds should be 0")
        assertEquals(0, timerEmitter.elapsedMinutesFlow.first(), "Initial minutes should be 0")
    }
}
