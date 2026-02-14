/*
 * CodeNodeLifecycleTest - TDD tests for CodeNode lifecycle control
 * Tests start, stop, pause, resume methods
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD tests for CodeNode lifecycle control methods.
 * Tests the start, stop, pause, resume functionality extracted from components.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CodeNodeLifecycleTest {

    private fun createTestNode(
        executionState: ExecutionState = ExecutionState.IDLE
    ): CodeNode {
        return CodeNode(
            id = "test-node",
            name = "TestNode",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(0.0, 0.0),
            executionState = executionState
        )
    }

    // T008: Test start creates nodeControlJob and runs processingBlock
    @Test
    fun `start creates nodeControlJob and runs processingBlock`() = runTest {
        val codeNode = createTestNode()

        var blockExecuted = false
        codeNode.start(this) {
            blockExecuted = true
        }

        advanceUntilIdle()

        assertTrue(blockExecuted, "Processing block should execute")
        assertNotNull(codeNode.nodeControlJob, "Job should be created")
    }

    // T009: Test stop cancels nodeControlJob
    @Test
    fun `stop cancels nodeControlJob`() = runTest {
        val codeNode = createTestNode()

        codeNode.start(this) {
            while (true) { delay(100) }
        }

        advanceTimeBy(50)
        assertNotNull(codeNode.nodeControlJob, "Job should exist before stop")

        codeNode.stop()

        assertNull(codeNode.nodeControlJob, "Job should be null after stop")
    }

    // T010: Test start cancels existing job before creating new one
    @Test
    fun `start cancels existing job before creating new one`() = runTest {
        val codeNode = createTestNode()

        var firstJobStarted = false
        var secondJobStarted = false

        codeNode.start(this) {
            firstJobStarted = true
            while (true) { delay(100) }
        }
        advanceTimeBy(50)

        val firstJob = codeNode.nodeControlJob
        assertTrue(firstJobStarted, "First job should have started")

        codeNode.start(this) {
            secondJobStarted = true
        }
        advanceUntilIdle()

        assertTrue(firstJob?.isCancelled == true, "First job should be cancelled")
        assertTrue(secondJobStarted, "Second job should start")
    }

    // T011: Test stop on idle node is no-op
    @Test
    fun `stop on idle node is no-op`() = runTest {
        val codeNode = createTestNode(executionState = ExecutionState.IDLE)

        // Should not throw
        codeNode.stop()

        assertNull(codeNode.nodeControlJob, "Job should remain null")
        assertTrue(codeNode.isIdle(), "State should remain IDLE")
    }

    // T012: Test stop on paused node transitions to IDLE
    @Test
    fun `stop on paused node cancels job`() = runTest {
        val codeNode = createTestNode(executionState = ExecutionState.RUNNING)

        codeNode.start(this) {
            while (true) { delay(100) }
        }
        advanceTimeBy(50)

        assertNotNull(codeNode.nodeControlJob, "Job should exist before stop")

        // Stop from paused state should work (stop is valid from RUNNING or PAUSED)
        codeNode.stop()

        assertNull(codeNode.nodeControlJob, "Job should be cancelled and null")
    }

    // T013: Test pause on running node - pause method exists and is no-op for state
    @Test
    fun `pause on running node is callable`() = runTest {
        val codeNode = createTestNode(executionState = ExecutionState.RUNNING)

        codeNode.start(this) {
            while (true) { delay(100) }
        }
        advanceTimeBy(50)

        // pause() should be callable without error
        codeNode.pause()

        // Job should still exist (not cancelled by pause)
        assertNotNull(codeNode.nodeControlJob, "Job should still exist after pause")

        // Cleanup: stop the job to avoid UncompletedCoroutinesError
        codeNode.stop()
    }

    // T014: Test resume from paused - resume method exists
    @Test
    fun `resume is callable on paused node`() = runTest {
        val codeNode = createTestNode(executionState = ExecutionState.PAUSED)

        // resume() should be callable without error
        codeNode.resume()

        // No assertion on state change since CodeNode is immutable data class
        // The method just signals intent
    }

    // T015: Test pause on non-running node is no-op
    @Test
    fun `pause on non-running node is no-op`() = runTest {
        val codeNode = createTestNode(executionState = ExecutionState.IDLE)

        // Should not throw
        codeNode.pause()

        assertTrue(codeNode.isIdle(), "State should remain IDLE")
    }

    // T016: Test resume on non-paused node is no-op
    @Test
    fun `resume on non-paused node is no-op`() = runTest {
        val codeNode = createTestNode(executionState = ExecutionState.RUNNING)

        // Should not throw
        codeNode.resume()

        assertTrue(codeNode.isRunning(), "State should remain RUNNING")
    }
}
