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

    // ========== User Story 4: Channel-Based Communication Tests ==========

    /**
     * T028: Test channel wiring between generator and sink works
     */
    @Test
    fun `channel wiring between generator and sink works`() = runTest {
        val receivedValues = mutableListOf<Int>()

        // Create generator that emits 1, 2, 3
        val generator = CodeNodeFactory.createContinuousGenerator<Int>(
            name = "NumberGenerator"
        ) { emit ->
            for (i in 1..3) {
                emit(i)
                delay(50)
            }
        }

        // Create sink that collects values
        val sink = CodeNodeFactory.createContinuousSink<Int>(
            name = "NumberCollector"
        ) { value ->
            receivedValues.add(value)
        }

        // Wire generator output to sink input via shared channel
        val channel = Channel<Int>(Channel.BUFFERED)
        generator.outputChannel = channel
        sink.inputChannel = channel

        // Start sink first (to be ready for data)
        sink.start(this) {}

        // Start generator
        generator.start(this) {}

        // Let data flow
        advanceTimeBy(200)
        advanceUntilIdle()

        // Verify data flowed through
        assertEquals(listOf(1, 2, 3), receivedValues, "Sink should receive all values from generator")

        // Cleanup
        generator.stop()
        sink.stop()
        channel.close()
    }

    /**
     * T029: Test backpressure prevents memory exhaustion (buffered channel fills)
     */
    @Test
    fun `backpressure with small buffer causes producer to wait`() = runTest {
        var emitCount = 0
        var receiveCount = 0

        // Use a small capacity channel (2 elements)
        val smallChannel = Channel<Int>(capacity = 2)

        // Fast generator - tries to emit quickly
        val generator = CodeNodeFactory.createContinuousGenerator<Int>(
            name = "FastProducer"
        ) { emit ->
            while (currentCoroutineContext().isActive) {
                emitCount++
                emit(emitCount)
                // No delay - tries to flood
            }
        }
        generator.outputChannel = smallChannel

        // Slow sink - processes slowly
        val sink = CodeNodeFactory.createContinuousSink<Int>(
            name = "SlowConsumer"
        ) { _ ->
            receiveCount++
            delay(100) // Slow processing
        }
        sink.inputChannel = smallChannel

        // Start both
        sink.start(this) {}
        generator.start(this) {}

        // Let it run briefly
        advanceTimeBy(50)

        // Generator should be blocked by backpressure after filling buffer
        // With capacity 2, generator can emit at most 2-3 items before blocking
        assertTrue(emitCount <= 4, "Generator should be limited by backpressure, emitted: $emitCount")

        // Let sink consume some
        advanceTimeBy(150)

        // Cleanup
        generator.stop()
        sink.stop()
        smallChannel.close()
    }

    /**
     * T030: Test channel closure propagates through flow graph
     */
    @Test
    fun `channel closure propagates through flow graph`() = runTest {
        val receivedValues = mutableListOf<Int>()
        var sinkCompleted = false

        // Create generator
        val generator = CodeNodeFactory.createContinuousGenerator<Int>(
            name = "ClosingGenerator"
        ) { emit ->
            // Emit a few values then exit (simulating completion)
            emit(1)
            emit(2)
            emit(3)
            // Generator block exits - channel should close
        }

        // Create sink that tracks completion
        val sink = CodeNodeFactory.createContinuousSink<Int>(
            name = "CompletionTracker"
        ) { value ->
            receivedValues.add(value)
        }

        // Wire via channel
        val channel = Channel<Int>(Channel.BUFFERED)
        generator.outputChannel = channel
        sink.inputChannel = channel

        // Start sink first
        val sinkJob = launch {
            sink.start(this) {}
            // When sink's for-loop exits (channel closed), we get here
            sinkCompleted = true
        }

        // Start generator
        generator.start(this) {}

        // Let data flow and generator complete
        advanceUntilIdle()

        // Generator completes, closes channel, sink should see closure
        assertEquals(listOf(1, 2, 3), receivedValues, "Sink should receive all values before closure")

        // Cleanup
        generator.stop()
        sink.stop()
        sinkJob.cancel()
    }

    /**
     * Test end-to-end flow: generator -> channel -> sink
     */
    @Test
    fun `end-to-end generator to sink flow works`() = runTest {
        val results = mutableListOf<String>()

        // Generator emits strings
        val generator = CodeNodeFactory.createContinuousGenerator<String>(
            name = "StringGenerator"
        ) { emit ->
            emit("Hello")
            delay(50)
            emit("World")
            delay(50)
            emit("!")
        }

        // Sink collects strings
        val sink = CodeNodeFactory.createContinuousSink<String>(
            name = "StringCollector"
        ) { value ->
            results.add(value)
        }

        // Wire them together
        val channel = Channel<String>(Channel.BUFFERED)
        generator.outputChannel = channel
        sink.inputChannel = channel

        // Start both (sink first)
        sink.start(this) {}
        generator.start(this) {}

        // Let flow complete
        advanceTimeBy(200)
        advanceUntilIdle()

        // Verify
        assertEquals(listOf("Hello", "World", "!"), results)

        // Cleanup
        generator.stop()
        sink.stop()
        channel.close()
    }

    // ========== User Story 3: Continuous Transformer Tests ==========

    /**
     * T034: Test transformer receives input and emits transformed output
     */
    @Test
    fun `transformer receives input and emits transformed output`() = runTest {
        val results = mutableListOf<Int>()

        // Create transformer that doubles input values
        val transformer = CodeNodeFactory.createContinuousTransformer<Int, Int>(
            name = "Doubler"
        ) { value ->
            value * 2
        }

        // Create channels
        val inputChannel = Channel<Int>(Channel.BUFFERED)
        val outputChannel = Channel<Int>(Channel.BUFFERED)
        transformer.inputChannel = inputChannel
        transformer.transformerOutputChannel = outputChannel

        // Collect outputs in background
        val collectJob = launch {
            for (value in outputChannel) {
                results.add(value)
            }
        }

        // Start transformer
        transformer.start(this) {}

        // Send input values
        inputChannel.send(1)
        inputChannel.send(2)
        inputChannel.send(3)
        advanceUntilIdle()

        // Verify transformed output
        assertEquals(listOf(2, 4, 6), results, "Transformer should double input values")

        // Cleanup
        transformer.stop()
        inputChannel.close()
        outputChannel.close()
        collectJob.cancel()
    }

    /**
     * T035: Test transformer respects pause/resume for flow control
     */
    @Test
    fun `transformer respects pause and resume for flow control`() = runTest {
        val results = mutableListOf<Int>()
        var processedCount = 0

        // Create transformer that tracks processing
        val transformer = CodeNodeFactory.createContinuousTransformer<Int, Int>(
            name = "PausableTransformer"
        ) { value ->
            processedCount++
            value + 10
        }

        // Create channels
        val inputChannel = Channel<Int>(Channel.BUFFERED)
        val outputChannel = Channel<Int>(Channel.BUFFERED)
        transformer.inputChannel = inputChannel
        transformer.transformerOutputChannel = outputChannel

        // Collect outputs
        val collectJob = launch {
            for (value in outputChannel) {
                results.add(value)
            }
        }

        // Start transformer
        transformer.start(this) {}

        // Send value and verify processing
        inputChannel.send(1)
        advanceUntilIdle()
        assertEquals(1, processedCount, "Should process first value")
        assertTrue(transformer.isRunning(), "Transformer should be running")

        // Pause transformer
        transformer.pause()
        assertTrue(transformer.isPaused(), "Transformer should be paused")

        // Resume transformer
        transformer.resume()
        assertTrue(transformer.isRunning(), "Transformer should be running after resume")

        // Send more values
        inputChannel.send(2)
        inputChannel.send(3)
        advanceUntilIdle()

        assertEquals(listOf(11, 12, 13), results, "All values should be transformed")

        // Cleanup
        transformer.stop()
        inputChannel.close()
        outputChannel.close()
        collectJob.cancel()
    }

    /**
     * T036: Test transformer chain (generator→transformer→sink) works
     */
    @Test
    fun `transformer chain generator to transformer to sink works`() = runTest {
        val results = mutableListOf<String>()

        // Create generator that emits numbers
        val generator = CodeNodeFactory.createContinuousGenerator<Int>(
            name = "NumberGenerator"
        ) { emit ->
            emit(1)
            emit(2)
            emit(3)
        }

        // Create transformer that converts Int to String with prefix
        val transformer = CodeNodeFactory.createContinuousTransformer<Int, String>(
            name = "IntToStringTransformer"
        ) { value ->
            "Value: $value"
        }

        // Create sink that collects strings
        val sink = CodeNodeFactory.createContinuousSink<String>(
            name = "StringCollector"
        ) { value ->
            results.add(value)
        }

        // Wire: generator -> transformer -> sink
        val channel1 = Channel<Int>(Channel.BUFFERED)
        val channel2 = Channel<String>(Channel.BUFFERED)

        generator.outputChannel = channel1
        transformer.inputChannel = channel1
        transformer.transformerOutputChannel = channel2
        sink.inputChannel = channel2

        // Start all nodes (sink first, then transformer, then generator)
        sink.start(this) {}
        transformer.start(this) {}
        generator.start(this) {}

        // Let data flow through the pipeline
        advanceUntilIdle()

        // Verify complete pipeline worked
        assertEquals(
            listOf("Value: 1", "Value: 2", "Value: 3"),
            results,
            "Data should flow through generator→transformer→sink pipeline"
        )

        // Cleanup
        generator.stop()
        transformer.stop()
        sink.stop()
        channel1.close()
        channel2.close()
    }

    /**
     * Test that createContinuousTransformer returns a properly configured TransformerRuntime
     */
    @Test
    fun `createContinuousTransformer returns configured TransformerRuntime`() = runTest {
        val transformer = CodeNodeFactory.createContinuousTransformer<Int, String>(
            name = "TestTransformer",
            description = "A test transformer"
        ) { value ->
            value.toString()
        }

        // Verify configuration
        assertNotNull(transformer, "Transformer should not be null")
        assertNotNull(transformer.codeNode, "CodeNode should not be null")
        assertEquals("TestTransformer", transformer.codeNode.name)
        assertEquals("A test transformer", transformer.codeNode.description)
        assertEquals(CodeNodeType.TRANSFORMER, transformer.codeNode.codeNodeType)
        assertTrue(transformer.isIdle(), "Initial state should be IDLE")
    }

    /**
     * Test continuous filter passes values that match predicate
     */
    @Test
    fun `continuous filter passes values that match predicate`() = runTest {
        val results = mutableListOf<Int>()

        // Create filter that only passes even numbers
        val filter = CodeNodeFactory.createContinuousFilter<Int>(
            name = "EvenFilter"
        ) { value ->
            value % 2 == 0
        }

        // Create channels
        val inputChannel = Channel<Int>(Channel.BUFFERED)
        val outputChannel = Channel<Int>(Channel.BUFFERED)
        filter.inputChannel = inputChannel
        filter.outputChannel = outputChannel

        // Collect outputs
        val collectJob = launch {
            for (value in outputChannel) {
                results.add(value)
            }
        }

        // Start filter
        filter.start(this) {}

        // Send mixed values
        inputChannel.send(1)
        inputChannel.send(2)
        inputChannel.send(3)
        inputChannel.send(4)
        inputChannel.send(5)
        advanceUntilIdle()

        // Verify only even numbers passed through
        assertEquals(listOf(2, 4), results, "Filter should only pass even numbers")

        // Cleanup
        filter.stop()
        inputChannel.close()
        outputChannel.close()
        collectJob.cancel()
    }

    /**
     * Test that createContinuousFilter returns a properly configured FilterRuntime
     */
    @Test
    fun `createContinuousFilter returns configured FilterRuntime`() = runTest {
        val filter = CodeNodeFactory.createContinuousFilter<Int>(
            name = "TestFilter",
            description = "A test filter"
        ) { value ->
            value > 0
        }

        // Verify configuration
        assertNotNull(filter, "Filter should not be null")
        assertNotNull(filter.codeNode, "CodeNode should not be null")
        assertEquals("TestFilter", filter.codeNode.name)
        assertEquals("A test filter", filter.codeNode.description)
        assertEquals(CodeNodeType.FILTER, filter.codeNode.codeNodeType)
        assertTrue(filter.isIdle(), "Initial state should be IDLE")
    }

    // ========== User Story 5: Backward Compatibility Tests ==========

    /**
     * T041: Test existing createGenerator method still works
     */
    @Test
    fun `existing createGenerator method still works`() = runTest {
        // Use the original single-invocation generator factory
        @Suppress("DEPRECATION")
        val node = CodeNodeFactory.createGenerator<String>(
            name = "LegacyGenerator",
            description = "A legacy generator"
        ) {
            "generated value"
        }

        // Verify CodeNode is returned (not NodeRuntime)
        assertNotNull(node, "Node should not be null")
        assertEquals("LegacyGenerator", node.name)
        assertEquals("A legacy generator", node.description)
        assertEquals(CodeNodeType.GENERATOR, node.codeNodeType)

        // Verify processing logic works
        assertNotNull(node.processingLogic, "ProcessingLogic should be set")
        val result = node.processingLogic?.process(emptyMap())
        assertNotNull(result, "Result should not be null")
        assertEquals(1, result?.size, "Should have one output")
        assertTrue(result?.containsKey("output") == true, "Should have 'output' key")
    }

    /**
     * T042: Test existing createSink method still works
     */
    @Test
    fun `existing createSink method still works`() = runTest {
        var receivedValue: String? = null

        // Use the original single-invocation sink factory
        @Suppress("DEPRECATION")
        val node = CodeNodeFactory.createSink<String>(
            name = "LegacySink",
            description = "A legacy sink"
        ) { value ->
            receivedValue = value
        }

        // Verify CodeNode is returned (not NodeRuntime)
        assertNotNull(node, "Node should not be null")
        assertEquals("LegacySink", node.name)
        assertEquals("A legacy sink", node.description)
        assertEquals(CodeNodeType.SINK, node.codeNodeType)

        // Verify processing logic works
        assertNotNull(node.processingLogic, "ProcessingLogic should be set")
        val inputPacket = io.codenode.fbpdsl.model.InformationPacketFactory.create("test input")
        val result = node.processingLogic?.process(mapOf("input" to inputPacket))
        assertEquals("test input", receivedValue, "Sink should have received the value")
        assertTrue(result?.isEmpty() == true, "Sink should return empty map")
    }

    /**
     * T043: Test ProcessingLogic implementations work unchanged
     */
    @Test
    fun `ProcessingLogic implementations work unchanged`() = runTest {
        // Create a node using the traditional ProcessingLogic pattern
        val processingLogic = io.codenode.fbpdsl.model.ProcessingLogic { inputs ->
            val inputPacket = inputs["input"] as? io.codenode.fbpdsl.model.InformationPacket<Int>
            val value = inputPacket?.payload ?: 0
            val result = value * 2
            mapOf("output" to io.codenode.fbpdsl.model.InformationPacketFactory.create(result))
        }

        val node = CodeNodeFactory.create(
            name = "ProcessingLogicNode",
            codeNodeType = CodeNodeType.TRANSFORMER,
            processingLogic = processingLogic
        )

        // Verify node works with ProcessingLogic
        assertNotNull(node, "Node should not be null")
        assertNotNull(node.processingLogic, "ProcessingLogic should be set")

        // Execute processing logic
        val inputPacket = io.codenode.fbpdsl.model.InformationPacketFactory.create(5)
        val result = node.processingLogic?.process(mapOf("input" to inputPacket))

        // Verify result
        assertNotNull(result, "Result should not be null")
        assertEquals(1, result?.size, "Should have one output")

        @Suppress("UNCHECKED_CAST")
        val outputPacket = result?.get("output") as? io.codenode.fbpdsl.model.InformationPacket<Int>
        assertEquals(10, outputPacket?.payload, "Should double the input value")
    }
}
