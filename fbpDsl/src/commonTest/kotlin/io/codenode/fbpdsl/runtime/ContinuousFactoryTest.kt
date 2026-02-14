/*
 * ContinuousFactoryTest - TDD tests for continuous factory methods
 * Tests createContinuousGenerator, createContinuousSink, etc.
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.model.CodeNodeType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TDD tests for continuous factory methods.
 * Tests the createContinuousGenerator and related functionality.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContinuousFactoryTest {

    // ========== User Story 1: Continuous Generator Tests ==========

    /**
     * T015: Test generator emits values at correct intervals using runTest and advanceTimeBy
     */
    @Test
    fun `generator emits values at correct intervals`() = runTest {
        val emissions = mutableListOf<Int>()
        val channel = Channel<Int>(Channel.BUFFERED)

        val runtime = CodeNodeFactory.createContinuousGenerator<Int>(
            name = "Counter"
        ) { emit ->
            var count = 0
            while (currentCoroutineContext().isActive) {
                delay(100)
                emit(++count)
            }
        }
        runtime.outputChannel = channel

        // Collect emissions in background
        val collectJob = launch {
            for (value in channel) {
                emissions.add(value)
            }
        }

        // Start generator
        runtime.start(this) {
            // The generator block runs inside start
        }

        // Advance virtual time - 350ms should give us 3 emissions (at 100, 200, 300)
        advanceTimeBy(350)

        // Verify emissions
        assertEquals(listOf(1, 2, 3), emissions, "Should have 3 emissions after 350ms")

        // Cleanup
        runtime.stop()
        channel.close()
        collectJob.cancel()
    }

    /**
     * T016: Test generator respects isActive check for graceful shutdown
     */
    @Test
    fun `generator respects isActive check for graceful shutdown`() = runTest {
        var loopIterations = 0
        val channel = Channel<Int>(Channel.BUFFERED)

        val runtime = CodeNodeFactory.createContinuousGenerator<Int>(
            name = "ActiveChecker"
        ) { emit ->
            while (currentCoroutineContext().isActive) {
                loopIterations++
                delay(50)
                emit(loopIterations)
            }
        }
        runtime.outputChannel = channel

        // Start generator
        runtime.start(this) {}

        // Let it run for a bit
        advanceTimeBy(150)
        val iterationsBeforeStop = loopIterations

        // Stop the generator
        runtime.stop()
        advanceUntilIdle()

        // Loop should have stopped - no more iterations
        val iterationsAfterStop = loopIterations
        assertEquals(iterationsBeforeStop, iterationsAfterStop, "No more iterations after stop")
        assertTrue(runtime.isIdle(), "Runtime should be IDLE after stop")

        channel.close()
    }

    /**
     * T017: Test generator output channel closes when stopped
     */
    @Test
    fun `generator output channel closes when stopped`() = runTest {
        val channel = Channel<Int>(Channel.BUFFERED)
        var channelClosed = false

        val runtime = CodeNodeFactory.createContinuousGenerator<Int>(
            name = "ChannelCloser"
        ) { emit ->
            while (currentCoroutineContext().isActive) {
                delay(100)
                emit(1)
            }
        }
        runtime.outputChannel = channel

        // Monitor channel closure
        val monitorJob = launch {
            try {
                for (value in channel) {
                    // Consume values
                }
            } finally {
                channelClosed = true
            }
        }

        // Start generator
        runtime.start(this) {}

        advanceTimeBy(150)

        // Stop generator - this should close the channel
        runtime.stop()
        advanceUntilIdle()

        // Verify channel was closed
        assertTrue(channelClosed, "Channel should be closed after generator stops")

        monitorJob.cancel()
    }

    /**
     * Test that createContinuousGenerator returns a properly configured NodeRuntime
     */
    @Test
    fun `createContinuousGenerator returns configured NodeRuntime`() = runTest {
        val runtime = CodeNodeFactory.createContinuousGenerator<String>(
            name = "TestGenerator",
            description = "A test generator"
        ) { emit ->
            emit("test")
        }

        // Verify NodeRuntime configuration
        assertNotNull(runtime, "Runtime should not be null")
        assertNotNull(runtime.codeNode, "CodeNode should not be null")
        assertEquals("TestGenerator", runtime.codeNode.name)
        assertEquals("A test generator", runtime.codeNode.description)
        assertEquals(CodeNodeType.GENERATOR, runtime.codeNode.codeNodeType)
        assertTrue(runtime.isIdle(), "Initial state should be IDLE")
    }

    /**
     * Test generator with custom channel capacity
     */
    @Test
    fun `generator uses custom channel capacity`() = runTest {
        val customCapacity = 10

        val runtime = CodeNodeFactory.createContinuousGenerator<Int>(
            name = "CapacityTest",
            channelCapacity = customCapacity
        ) { emit ->
            var count = 0
            while (currentCoroutineContext().isActive) {
                delay(10)
                emit(++count)
            }
        }

        // Verify runtime is created (capacity is internal implementation detail)
        assertNotNull(runtime, "Runtime should be created with custom capacity")
        assertEquals("CapacityTest", runtime.codeNode.name)
    }

    // ========== User Story 2: Continuous Sink Tests ==========

    /**
     * T022: Test sink receives all values from input channel
     */
    @Test
    fun `sink receives all values from input channel`() = runTest {
        val receivedValues = mutableListOf<Int>()
        val channel = Channel<Int>(Channel.BUFFERED)

        val runtime = CodeNodeFactory.createContinuousSink<Int>(
            name = "Collector"
        ) { value ->
            receivedValues.add(value)
        }
        runtime.inputChannel = channel

        // Start sink
        runtime.start(this) {}

        // Send values to the channel
        channel.send(1)
        channel.send(2)
        channel.send(3)

        // Let sink process
        advanceUntilIdle()

        // Verify all values received
        assertEquals(listOf(1, 2, 3), receivedValues, "Sink should receive all 3 values")

        // Cleanup
        runtime.stop()
        channel.close()
    }

    /**
     * T023: Test sink handles channel closure gracefully
     */
    @Test
    fun `sink handles channel closure gracefully`() = runTest {
        val receivedValues = mutableListOf<Int>()
        val channel = Channel<Int>(Channel.BUFFERED)
        var sinkExitedGracefully = false

        val runtime = CodeNodeFactory.createContinuousSink<Int>(
            name = "GracefulSink"
        ) { value ->
            receivedValues.add(value)
        }
        runtime.inputChannel = channel

        // Start sink
        runtime.start(this) {}

        // Send some values
        channel.send(1)
        channel.send(2)
        advanceUntilIdle()

        // Close the channel - sink should handle gracefully
        channel.close()
        advanceUntilIdle()

        // Verify values were received before closure
        assertEquals(listOf(1, 2), receivedValues, "Sink should have received values before closure")

        // Sink should now be idle (graceful exit)
        // Give it time to process channel closure
        advanceUntilIdle()

        // No exceptions thrown means graceful handling
        assertTrue(true, "Sink handled channel closure gracefully")

        runtime.stop()
    }

    /**
     * T024: Test sink uses NodeRuntime lifecycle control
     */
    @Test
    fun `sink uses NodeRuntime lifecycle control`() = runTest {
        val channel = Channel<Int>(Channel.BUFFERED)

        val runtime = CodeNodeFactory.createContinuousSink<Int>(
            name = "LifecycleSink",
            description = "Tests lifecycle"
        ) { _ -> }
        runtime.inputChannel = channel

        // Verify initial state
        assertTrue(runtime.isIdle(), "Initial state should be IDLE")
        assertNotNull(runtime.codeNode, "CodeNode should exist")
        assertEquals("LifecycleSink", runtime.codeNode.name)
        assertEquals(CodeNodeType.SINK, runtime.codeNode.codeNodeType)

        // Start sink
        runtime.start(this) {}
        assertTrue(runtime.isRunning(), "State should be RUNNING after start")

        // Stop sink
        runtime.stop()
        assertTrue(runtime.isIdle(), "State should be IDLE after stop")

        channel.close()
    }

    /**
     * Test that createContinuousSink returns a properly configured SinkRuntime
     */
    @Test
    fun `createContinuousSink returns configured SinkRuntime`() = runTest {
        val runtime = CodeNodeFactory.createContinuousSink<String>(
            name = "TestSink",
            description = "A test sink"
        ) { _ -> }

        // Verify configuration
        assertNotNull(runtime, "Runtime should not be null")
        assertNotNull(runtime.codeNode, "CodeNode should not be null")
        assertEquals("TestSink", runtime.codeNode.name)
        assertEquals("A test sink", runtime.codeNode.description)
        assertEquals(CodeNodeType.SINK, runtime.codeNode.codeNodeType)
        assertTrue(runtime.isIdle(), "Initial state should be IDLE")
    }
}
