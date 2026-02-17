/*
 * StopWatchViewModelTest
 * Unit tests for StopWatchViewModel
 * License: Apache 2.0
 */

package io.codenode.mobileapp.viewmodel

import io.codenode.fbpdsl.model.ExecutionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for StopWatchViewModel.
 *
 * These tests verify that the ViewModel correctly delegates state observation
 * and actions to the underlying controller. Tests use FakeStopWatchController
 * to avoid FlowGraph dependencies and enable pure unit testing.
 */
class StopWatchViewModelTest {

    // ========================================
    // Initial State Tests (T005)
    // ========================================

    @Test
    fun `initial state is IDLE with zero elapsed time`() = runTest {
        val controller = FakeStopWatchController()
        val viewModel = StopWatchViewModel(controller)

        assertEquals(0, viewModel.elapsedSeconds.first())
        assertEquals(0, viewModel.elapsedMinutes.first())
        assertEquals(ExecutionState.IDLE, viewModel.executionState.first())
    }

    // ========================================
    // State Exposure Tests (T010, T011, T012)
    // ========================================

    @Test
    fun `elapsedSeconds is exposed correctly from controller`() = runTest {
        val controller = FakeStopWatchController()
        val viewModel = StopWatchViewModel(controller)

        // Initial value
        assertEquals(0, viewModel.elapsedSeconds.first())

        // Update controller state
        controller.setElapsedSeconds(30)
        assertEquals(30, viewModel.elapsedSeconds.first())

        // Update to max seconds before rollover
        controller.setElapsedSeconds(59)
        assertEquals(59, viewModel.elapsedSeconds.first())
    }

    @Test
    fun `elapsedMinutes is exposed correctly from controller`() = runTest {
        val controller = FakeStopWatchController()
        val viewModel = StopWatchViewModel(controller)

        // Initial value
        assertEquals(0, viewModel.elapsedMinutes.first())

        // Update controller state
        controller.setElapsedMinutes(5)
        assertEquals(5, viewModel.elapsedMinutes.first())

        // Update to higher value
        controller.setElapsedMinutes(120)
        assertEquals(120, viewModel.elapsedMinutes.first())
    }

    @Test
    fun `executionState is exposed correctly from controller`() = runTest {
        val controller = FakeStopWatchController()
        val viewModel = StopWatchViewModel(controller)

        // Initial state
        assertEquals(ExecutionState.IDLE, viewModel.executionState.first())

        // Update to RUNNING
        controller.setExecutionState(ExecutionState.RUNNING)
        assertEquals(ExecutionState.RUNNING, viewModel.executionState.first())

        // Update to PAUSED
        controller.setExecutionState(ExecutionState.PAUSED)
        assertEquals(ExecutionState.PAUSED, viewModel.executionState.first())

        // Update back to IDLE
        controller.setExecutionState(ExecutionState.IDLE)
        assertEquals(ExecutionState.IDLE, viewModel.executionState.first())
    }

    @Test
    fun `state updates are reflected immediately`() = runTest {
        val controller = FakeStopWatchController()
        val viewModel = StopWatchViewModel(controller)

        // Simulate timer counting: seconds increment, then minute rollover
        controller.setElapsedSeconds(58)
        assertEquals(58, viewModel.elapsedSeconds.first())

        controller.setElapsedSeconds(59)
        assertEquals(59, viewModel.elapsedSeconds.first())

        // Minute rollover
        controller.setElapsedSeconds(0)
        controller.setElapsedMinutes(1)
        assertEquals(0, viewModel.elapsedSeconds.first())
        assertEquals(1, viewModel.elapsedMinutes.first())
    }
}
