/*
 * NodeRuntimeTest - TDD tests for NodeRuntime lifecycle control
 * Tests start, stop, pause, resume methods
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.Node
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD tests for NodeRuntime lifecycle control methods.
 * Tests the start, stop, pause, resume functionality.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NodeRuntimeTest {

    private fun createTestRuntime(
        executionState: ExecutionState = ExecutionState.IDLE
    ): NodeRuntime<String> {
        val codeNode = CodeNode(
            id = "test-node",
            name = "TestNode",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(0.0, 0.0),
            executionState = executionState
        )
        return NodeRuntime<String>(codeNode).also {
            it.executionState = executionState
        }
    }

    // Test start creates nodeControlJob and runs processingBlock
    @Test
    fun `start creates nodeControlJob and runs processingBlock`() = runTest {
        val runtime = createTestRuntime()

        var blockExecuted = false
        runtime.start(this) {
            blockExecuted = true
        }

        advanceUntilIdle()

        assertTrue(blockExecuted, "Processing block should execute")
        assertNotNull(runtime.nodeControlJob, "Job should be created")
    }

    // Test stop cancels nodeControlJob
    @Test
    fun `stop cancels nodeControlJob`() = runTest {
        val runtime = createTestRuntime()

        runtime.start(this) {
            while (true) { delay(100) }
        }

        advanceTimeBy(50)
        assertNotNull(runtime.nodeControlJob, "Job should exist before stop")

        runtime.stop()

        assertNull(runtime.nodeControlJob, "Job should be null after stop")
    }

    // Test start cancels existing job before creating new one
    @Test
    fun `start cancels existing job before creating new one`() = runTest {
        val runtime = createTestRuntime()

        var firstJobStarted = false
        var secondJobStarted = false

        runtime.start(this) {
            firstJobStarted = true
            while (true) { delay(100) }
        }
        advanceTimeBy(50)

        val firstJob = runtime.nodeControlJob
        assertTrue(firstJobStarted, "First job should have started")

        runtime.start(this) {
            secondJobStarted = true
        }
        advanceUntilIdle()

        assertTrue(firstJob?.isCancelled == true, "First job should be cancelled")
        assertTrue(secondJobStarted, "Second job should start")
    }

    // Test stop on idle node is no-op
    @Test
    fun `stop on idle node is no-op`() = runTest {
        val runtime = createTestRuntime(executionState = ExecutionState.IDLE)

        // Should not throw
        runtime.stop()

        assertNull(runtime.nodeControlJob, "Job should remain null")
        assertTrue(runtime.isIdle(), "State should remain IDLE")
    }

    // Test stop on running node cancels job and sets state to IDLE
    @Test
    fun `stop on running node cancels job`() = runTest {
        val runtime = createTestRuntime()

        runtime.start(this) {
            while (true) { delay(100) }
        }
        advanceTimeBy(50)

        assertTrue(runtime.isRunning(), "State should be RUNNING after start")
        assertNotNull(runtime.nodeControlJob, "Job should exist before stop")

        runtime.stop()

        assertNull(runtime.nodeControlJob, "Job should be cancelled and null")
        assertTrue(runtime.isIdle(), "State should be IDLE after stop")
    }

    // Test pause on running node changes state to PAUSED
    @Test
    fun `pause on running node changes state to PAUSED`() = runTest {
        val runtime = createTestRuntime()

        runtime.start(this) {
            while (true) { delay(100) }
        }
        advanceTimeBy(50)

        assertTrue(runtime.isRunning(), "State should be RUNNING")

        runtime.pause()

        assertTrue(runtime.isPaused(), "State should be PAUSED after pause")
        // Job should still exist (not cancelled by pause)
        assertNotNull(runtime.nodeControlJob, "Job should still exist after pause")

        // Cleanup: stop the job to avoid UncompletedCoroutinesError
        runtime.stop()
    }

    // Test resume from paused changes state to RUNNING
    @Test
    fun `resume from paused changes state to RUNNING`() = runTest {
        val runtime = createTestRuntime()

        runtime.start(this) {
            while (true) { delay(100) }
        }
        advanceTimeBy(50)

        runtime.pause()
        assertTrue(runtime.isPaused(), "State should be PAUSED")

        runtime.resume()

        assertTrue(runtime.isRunning(), "State should be RUNNING after resume")

        // Cleanup
        runtime.stop()
    }

    // Test pause on non-running node is no-op
    @Test
    fun `pause on non-running node is no-op`() = runTest {
        val runtime = createTestRuntime(executionState = ExecutionState.IDLE)

        // Should not throw
        runtime.pause()

        assertTrue(runtime.isIdle(), "State should remain IDLE")
    }

    // Test resume on non-paused node is no-op
    @Test
    fun `resume on non-paused node is no-op`() = runTest {
        val runtime = createTestRuntime()

        runtime.start(this) {
            while (true) { delay(100) }
        }
        advanceTimeBy(50)

        assertTrue(runtime.isRunning(), "State should be RUNNING")

        // Should not throw or change state
        runtime.resume()

        assertTrue(runtime.isRunning(), "State should remain RUNNING")

        // Cleanup
        runtime.stop()
    }

    // Test start sets executionState to RUNNING
    @Test
    fun `start sets executionState to RUNNING`() = runTest {
        val runtime = createTestRuntime(executionState = ExecutionState.IDLE)

        assertTrue(runtime.isIdle(), "Initial state should be IDLE")

        runtime.start(this) {
            delay(1000)
        }
        advanceTimeBy(50)

        assertTrue(runtime.isRunning(), "State should be RUNNING after start")

        // Cleanup
        runtime.stop()
    }
}
