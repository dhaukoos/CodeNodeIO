/*
 * DisplayReceiverComponentTest - TDD tests for DisplayReceiver UseCase
 * Task T051: DisplayReceiver updates state when receiving seconds/minutes inputs
 * License: Apache 2.0
 */

package io.codenode.stopwatch.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD tests for DisplayReceiver component.
 * These tests define the expected behavior BEFORE implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DisplayReceiverComponentTest {

    /**
     * T051: DisplayReceiver updates state when receiving seconds/minutes inputs.
     *
     * Given: A DisplayReceiver component
     * When: seconds and minutes values are sent to its input
     * Then: The component's displayedSeconds and displayedMinutes StateFlows should update
     */
    @Test
    fun displayReceiver_updates_state_when_receiving_seconds_minutes_inputs() = runTest {
        // Given
        val displayReceiver = DisplayReceiverComponent()

        val startJob = launch {
            displayReceiver.start(this)
        }

        // When - send seconds value
        displayReceiver.receiveSeconds(42)
        advanceUntilIdle()

        // Then
        assertEquals(42, displayReceiver.secondsFlow.first(),
            "displayedSeconds should update to 42")

        // When - send minutes value
        displayReceiver.receiveMinutes(7)
        advanceUntilIdle()

        // Then
        assertEquals(7, displayReceiver.minutesFlow.first(),
            "displayedMinutes should update to 7")

        // Cleanup
        displayReceiver.stop()
        startJob.cancel()
    }

    /**
     * Test that DisplayReceiver has zero initial values.
     */
    @Test
    fun displayReceiver_has_zero_initial_values() = runTest {
        val displayReceiver = DisplayReceiverComponent()

        assertEquals(0, displayReceiver.secondsFlow.first(),
            "Initial displayedSeconds should be 0")
        assertEquals(0, displayReceiver.minutesFlow.first(),
            "Initial displayedMinutes should be 0")
    }

    /**
     * Test that DisplayReceiver updates continuously as new values arrive.
     */
    @Test
    fun displayReceiver_updates_continuously_as_values_arrive() = runTest {
        // Given
        val displayReceiver = DisplayReceiverComponent()

        val startJob = launch {
            displayReceiver.start(this)
        }

        // When - send a sequence of values
        for (i in 1..5) {
            displayReceiver.receiveSeconds(i)
            displayReceiver.receiveMinutes(i * 10)
            advanceUntilIdle()

            // Then - verify each update
            assertEquals(i, displayReceiver.secondsFlow.first(),
                "displayedSeconds should update to $i")
            assertEquals(i * 10, displayReceiver.minutesFlow.first(),
                "displayedMinutes should update to ${i * 10}")
        }

        // Cleanup
        displayReceiver.stop()
        startJob.cancel()
    }
}
