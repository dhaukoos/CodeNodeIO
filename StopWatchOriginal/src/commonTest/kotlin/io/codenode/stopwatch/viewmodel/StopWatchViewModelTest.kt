/*
 * StopWatchViewModelTest
 * Unit tests for StopWatchViewModel
 * License: Apache 2.0
 */

package io.codenode.stopwatch.viewmodel

import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.stopwatch.generated.StopWatchViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

        assertEquals(0, viewModel.seconds.first())
        assertEquals(0, viewModel.minutes.first())
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
        assertEquals(0, viewModel.seconds.first())

        // Update controller state
        controller.setSeconds(30)
        assertEquals(30, viewModel.seconds.first())

        // Update to max seconds before rollover
        controller.setSeconds(59)
        assertEquals(59, viewModel.seconds.first())
    }

    @Test
    fun `elapsedMinutes is exposed correctly from controller`() = runTest {
        val controller = FakeStopWatchController()
        val viewModel = StopWatchViewModel(controller)

        // Initial value
        assertEquals(0, viewModel.minutes.first())

        // Update controller state
        controller.setMinutes(5)
        assertEquals(5, viewModel.minutes.first())

        // Update to higher value
        controller.setMinutes(120)
        assertEquals(120, viewModel.minutes.first())
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
        controller.setSeconds(58)
        assertEquals(58, viewModel.seconds.first())

        controller.setSeconds(59)
        assertEquals(59, viewModel.seconds.first())

        // Minute rollover
        controller.setSeconds(0)
        controller.setMinutes(1)
        assertEquals(0, viewModel.seconds.first())
        assertEquals(1, viewModel.minutes.first())
    }

    // ========================================
    // Action Delegation Tests (T013, T014, T015)
    // ========================================

    @Test
    fun `start delegates to controller`() = runTest {
        val controller = FakeStopWatchController()
        val viewModel = StopWatchViewModel(controller)

        // Verify not called initially
        assertEquals(false, controller.startCalled)

        // Call start through ViewModel
        viewModel.start()

        // Verify delegation
        assertTrue(controller.startCalled)
        assertEquals(ExecutionState.RUNNING, viewModel.executionState.first())
    }

    @Test
    fun `stop delegates to controller`() = runTest {
        val controller = FakeStopWatchController()
        val viewModel = StopWatchViewModel(controller)

        // Start first
        viewModel.start()
        assertEquals(ExecutionState.RUNNING, viewModel.executionState.first())

        // Verify stop not called yet
        assertEquals(false, controller.stopCalled)

        // Call stop through ViewModel
        viewModel.stop()

        // Verify delegation
        assertTrue(controller.stopCalled)
        assertEquals(ExecutionState.IDLE, viewModel.executionState.first())
    }

    @Test
    fun `reset delegates to controller`() = runTest {
        val controller = FakeStopWatchController()
        val viewModel = StopWatchViewModel(controller)

        // Set some elapsed time
        controller.setSeconds(30)
        controller.setMinutes(5)
        assertEquals(30, viewModel.seconds.first())
        assertEquals(5, viewModel.minutes.first())

        // Verify reset not called yet
        assertEquals(false, controller.resetCalled)

        // Call reset through ViewModel
        viewModel.reset()

        // Verify delegation
        assertTrue(controller.resetCalled)
        assertEquals(0, viewModel.seconds.first())
        assertEquals(0, viewModel.minutes.first())
        assertEquals(ExecutionState.IDLE, viewModel.executionState.first())
    }

    // ========================================
    // Pause/Resume Action Tests (T042)
    // ========================================

    @Test
    fun `pause delegates to controller`() = runTest {
        val controller = FakeStopWatchController()
        val viewModel = StopWatchViewModel(controller)

        // Start first
        viewModel.start()
        assertEquals(ExecutionState.RUNNING, viewModel.executionState.first())

        // Verify pause not called yet
        assertEquals(false, controller.pauseCalled)

        // Call pause through ViewModel
        viewModel.pause()

        // Verify delegation
        assertTrue(controller.pauseCalled)
        assertEquals(ExecutionState.PAUSED, viewModel.executionState.first())
    }

    @Test
    fun `resume delegates to controller`() = runTest {
        val controller = FakeStopWatchController()
        val viewModel = StopWatchViewModel(controller)

        // Start and pause first
        viewModel.start()
        viewModel.pause()
        assertEquals(ExecutionState.PAUSED, viewModel.executionState.first())

        // Verify resume not called yet
        assertEquals(false, controller.resumeCalled)

        // Call resume through ViewModel
        viewModel.resume()

        // Verify delegation
        assertTrue(controller.resumeCalled)
        assertEquals(ExecutionState.RUNNING, viewModel.executionState.first())
    }

    @Test
    fun `pause preserves elapsed time`() = runTest {
        val controller = FakeStopWatchController()
        val viewModel = StopWatchViewModel(controller)

        // Set elapsed time
        controller.setSeconds(45)
        controller.setMinutes(3)
        viewModel.start()

        // Verify initial state
        assertEquals(45, viewModel.seconds.first())
        assertEquals(3, viewModel.minutes.first())

        // Pause should not affect elapsed time
        viewModel.pause()
        assertEquals(45, viewModel.seconds.first())
        assertEquals(3, viewModel.minutes.first())
        assertEquals(ExecutionState.PAUSED, viewModel.executionState.first())
    }

    @Test
    fun `resume continues from paused state`() = runTest {
        val controller = FakeStopWatchController()
        val viewModel = StopWatchViewModel(controller)

        // Set elapsed time, start, and pause
        controller.setSeconds(30)
        controller.setMinutes(2)
        viewModel.start()
        viewModel.pause()

        // Verify paused
        assertEquals(ExecutionState.PAUSED, viewModel.executionState.first())
        assertEquals(30, viewModel.seconds.first())
        assertEquals(2, viewModel.minutes.first())

        // Resume should restore running state without affecting time
        viewModel.resume()
        assertEquals(ExecutionState.RUNNING, viewModel.executionState.first())
        assertEquals(30, viewModel.seconds.first())
        assertEquals(2, viewModel.minutes.first())
    }

    // ========================================
    // Edge Case Tests (T027, T028)
    // ========================================

    @Test
    fun `rapid start stop reset sequence works correctly`() = runTest {
        val controller = FakeStopWatchController()
        val viewModel = StopWatchViewModel(controller)

        // Rapid sequence: start -> stop -> start -> reset -> start
        viewModel.start()
        assertEquals(ExecutionState.RUNNING, viewModel.executionState.first())
        assertTrue(controller.startCalled)

        viewModel.stop()
        assertEquals(ExecutionState.IDLE, viewModel.executionState.first())
        assertTrue(controller.stopCalled)

        // Reset flags for next sequence
        controller.resetFlags()

        viewModel.start()
        assertEquals(ExecutionState.RUNNING, viewModel.executionState.first())
        assertTrue(controller.startCalled)

        // Simulate some elapsed time
        controller.setSeconds(10)
        controller.setMinutes(1)

        viewModel.reset()
        assertEquals(ExecutionState.IDLE, viewModel.executionState.first())
        assertEquals(0, viewModel.seconds.first())
        assertEquals(0, viewModel.minutes.first())
        assertTrue(controller.resetCalled)

        // Start again after reset
        controller.resetFlags()
        viewModel.start()
        assertEquals(ExecutionState.RUNNING, viewModel.executionState.first())
        assertTrue(controller.startCalled)
    }

    @Test
    fun `late subscription receives current state`() = runTest {
        val controller = FakeStopWatchController()

        // Set state before creating ViewModel (simulating late subscription)
        controller.setSeconds(45)
        controller.setMinutes(3)
        controller.setExecutionState(ExecutionState.RUNNING)

        // Create ViewModel after state is already set (late subscription)
        val viewModel = StopWatchViewModel(controller)

        // Verify late subscriber gets current state immediately
        assertEquals(45, viewModel.seconds.first())
        assertEquals(3, viewModel.minutes.first())
        assertEquals(ExecutionState.RUNNING, viewModel.executionState.first())
    }
}
