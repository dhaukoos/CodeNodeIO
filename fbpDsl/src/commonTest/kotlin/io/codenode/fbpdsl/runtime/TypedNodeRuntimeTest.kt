/*
 * TypedNodeRuntimeTest - Tests for multi-input/multi-output node runtimes
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.model.ExecutionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TypedNodeRuntimeTest {

    // ========== User Story 1: Create Typed Processor Node ==========

    @Test
    fun `In2Out1Runtime receives from both inputs and produces output`() = runTest {
        // Given: A processor that adds two integers
        val processor = CodeNodeFactory.createIn2Out1Processor<Int, Int, Int>(
            name = "Adder"
        ) { a, b -> a + b }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val output = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel = input1
        processor.inputChannel2 = input2
        processor.processorOutputChannel = output

        // When: Start and send values
        processor.start(this) { }

        input1.send(5)
        input2.send(3)
        advanceUntilIdle()

        // Then: Output should be sum
        assertEquals(8, output.receive())

        processor.stop()
    }

    @Test
    fun `In2Out1Runtime processes multiple tuples continuously`() = runTest {
        // Given: A processor that concatenates strings
        val processor = CodeNodeFactory.createIn2Out1Processor<String, String, String>(
            name = "Concatenator"
        ) { a, b -> "$a$b" }

        val input1 = Channel<String>(Channel.BUFFERED)
        val input2 = Channel<String>(Channel.BUFFERED)
        val output = Channel<String>(Channel.BUFFERED)

        processor.inputChannel = input1
        processor.inputChannel2 = input2
        processor.processorOutputChannel = output

        // When: Start and send multiple tuples
        processor.start(this) { }

        input1.send("Hello")
        input2.send("World")
        advanceUntilIdle()

        input1.send("Foo")
        input2.send("Bar")
        advanceUntilIdle()

        input1.send("Test")
        input2.send("123")
        advanceUntilIdle()

        // Then: All tuples should be processed
        assertEquals("HelloWorld", output.receive())
        assertEquals("FooBar", output.receive())
        assertEquals("Test123", output.receive())

        processor.stop()
    }

    @Test
    fun `In3Out1Runtime receives from three inputs and produces output`() = runTest {
        // Given: A processor that combines three values
        val processor = CodeNodeFactory.createIn3Out1Processor<Int, Int, Int, Int>(
            name = "TripleAdder"
        ) { a, b, c -> a + b + c }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val input3 = Channel<Int>(Channel.BUFFERED)
        val output = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel = input1
        processor.inputChannel2 = input2
        processor.inputChannel3 = input3
        processor.processorOutputChannel = output

        // When: Start and send values
        processor.start(this) { }

        input1.send(10)
        input2.send(20)
        input3.send(30)
        advanceUntilIdle()

        // Then: Output should be sum of all three
        assertEquals(60, output.receive())

        processor.stop()
    }

    // ========== User Story 2: Lifecycle Control ==========

    @Test
    fun `In2Out1Runtime stop cancels processing and closes output channel`() = runTest {
        // Given: A running processor
        val processor = CodeNodeFactory.createIn2Out1Processor<Int, Int, Int>(
            name = "Adder"
        ) { a, b -> a + b }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val output = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel = input1
        processor.inputChannel2 = input2
        processor.processorOutputChannel = output

        processor.start(this) { }
        advanceUntilIdle()

        // When: Stop the processor
        processor.stop()
        advanceUntilIdle()

        // Then: State should be IDLE and output channel should be closed
        assertEquals(ExecutionState.IDLE, processor.executionState)
        assertTrue(output.isClosedForReceive)
    }

    @Test
    fun `In2Out1Runtime pause suspends processing until resume`() = runTest {
        // Given: A running processor
        val processor = CodeNodeFactory.createIn2Out1Processor<Int, Int, Int>(
            name = "Adder"
        ) { a, b -> a + b }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val output = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel = input1
        processor.inputChannel2 = input2
        processor.processorOutputChannel = output

        processor.start(this) { }

        // Process first value pair
        input1.send(1)
        input2.send(2)
        advanceUntilIdle()
        assertEquals(3, output.receive())

        // When: Pause and verify state
        processor.pause()
        assertTrue(processor.isPaused())
        assertEquals(ExecutionState.PAUSED, processor.executionState)

        // When: Resume and verify state
        processor.resume()
        assertTrue(processor.isRunning())
        assertEquals(ExecutionState.RUNNING, processor.executionState)

        // Then: Processing continues after resume
        input1.send(10)
        input2.send(20)
        advanceUntilIdle()
        assertEquals(30, output.receive())

        processor.stop()
    }

    @Test
    fun `In2Out1Runtime handles ClosedReceiveChannelException gracefully`() = runTest {
        // Given: A running processor
        val processor = CodeNodeFactory.createIn2Out1Processor<Int, Int, Int>(
            name = "Adder"
        ) { a, b -> a + b }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val output = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel = input1
        processor.inputChannel2 = input2
        processor.processorOutputChannel = output

        processor.start(this) { }
        advanceUntilIdle()

        // When: Close input channel
        input1.close()
        advanceUntilIdle()

        // Then: Processor should gracefully stop (state becomes IDLE)
        assertEquals(ExecutionState.IDLE, processor.executionState)
    }

    @Test
    fun `In2Out1Runtime handles ClosedSendChannelException gracefully`() = runTest {
        // Given: A running processor
        val processor = CodeNodeFactory.createIn2Out1Processor<Int, Int, Int>(
            name = "Adder"
        ) { a, b -> a + b }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val output = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel = input1
        processor.inputChannel2 = input2
        processor.processorOutputChannel = output

        processor.start(this) { }

        // Send values and close output before they can be processed
        input1.send(1)
        input2.send(2)
        output.close()
        advanceUntilIdle()

        // Then: Processor should gracefully stop (state becomes IDLE)
        assertEquals(ExecutionState.IDLE, processor.executionState)
    }

    @Test
    fun `In3Out1Runtime stop cancels processing and closes output channel`() = runTest {
        // Given: A running processor
        val processor = CodeNodeFactory.createIn3Out1Processor<Int, Int, Int, Int>(
            name = "TripleAdder"
        ) { a, b, c -> a + b + c }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val input3 = Channel<Int>(Channel.BUFFERED)
        val output = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel = input1
        processor.inputChannel2 = input2
        processor.inputChannel3 = input3
        processor.processorOutputChannel = output

        processor.start(this) { }
        advanceUntilIdle()

        // When: Stop the processor
        processor.stop()
        advanceUntilIdle()

        // Then: State should be IDLE and output channel should be closed
        assertEquals(ExecutionState.IDLE, processor.executionState)
        assertTrue(output.isClosedForReceive)
    }

    @Test
    fun `In3Out1Runtime pause suspends processing until resume`() = runTest {
        // Given: A running processor
        val processor = CodeNodeFactory.createIn3Out1Processor<Int, Int, Int, Int>(
            name = "TripleAdder"
        ) { a, b, c -> a + b + c }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val input3 = Channel<Int>(Channel.BUFFERED)
        val output = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel = input1
        processor.inputChannel2 = input2
        processor.inputChannel3 = input3
        processor.processorOutputChannel = output

        processor.start(this) { }

        // Process first value tuple
        input1.send(1)
        input2.send(2)
        input3.send(3)
        advanceUntilIdle()
        assertEquals(6, output.receive())

        // When: Pause and verify state
        processor.pause()
        assertTrue(processor.isPaused())
        assertEquals(ExecutionState.PAUSED, processor.executionState)

        // When: Resume and verify state
        processor.resume()
        assertTrue(processor.isRunning())
        assertEquals(ExecutionState.RUNNING, processor.executionState)

        // Then: Processing continues after resume
        input1.send(10)
        input2.send(20)
        input3.send(30)
        advanceUntilIdle()
        assertEquals(60, output.receive())

        processor.stop()
    }

    // ========== User Story 3: Generator and Sink Node Variants ==========

    @Test
    fun `In2SinkRuntime consumes from two inputs continuously`() = runTest {
        // Given: A sink that collects pairs
        val collected = mutableListOf<Pair<Int, String>>()
        val sink = CodeNodeFactory.createIn2Sink<Int, String>(
            name = "PairCollector"
        ) { a, b -> collected.add(Pair(a, b)) }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<String>(Channel.BUFFERED)

        sink.inputChannel = input1
        sink.inputChannel2 = input2

        // When: Start and send multiple pairs
        sink.start(this) { }

        input1.send(1)
        input2.send("one")
        advanceUntilIdle()

        input1.send(2)
        input2.send("two")
        advanceUntilIdle()

        input1.send(3)
        input2.send("three")
        advanceUntilIdle()

        // Then: All pairs should be collected
        assertEquals(3, collected.size)
        assertEquals(Pair(1, "one"), collected[0])
        assertEquals(Pair(2, "two"), collected[1])
        assertEquals(Pair(3, "three"), collected[2])

        sink.stop()
    }

    @Test
    fun `In3SinkRuntime consumes from three inputs continuously`() = runTest {
        // Given: A sink that collects triples
        val collected = mutableListOf<Triple<Int, Int, Int>>()
        val sink = CodeNodeFactory.createIn3Sink<Int, Int, Int>(
            name = "TripleCollector"
        ) { a, b, c -> collected.add(Triple(a, b, c)) }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val input3 = Channel<Int>(Channel.BUFFERED)

        sink.inputChannel = input1
        sink.inputChannel2 = input2
        sink.inputChannel3 = input3

        // When: Start and send multiple triples
        sink.start(this) { }

        input1.send(1)
        input2.send(2)
        input3.send(3)
        advanceUntilIdle()

        input1.send(10)
        input2.send(20)
        input3.send(30)
        advanceUntilIdle()

        // Then: All triples should be collected
        assertEquals(2, collected.size)
        assertEquals(Triple(1, 2, 3), collected[0])
        assertEquals(Triple(10, 20, 30), collected[1])

        sink.stop()
    }

    @Test
    fun `Out2GeneratorRuntime emits ProcessResult2 to two output channels`() = runTest {
        // Given: A generator that emits pairs
        var count = 0
        val generator = CodeNodeFactory.createOut2Generator<Int, String>(
            name = "PairGenerator"
        ) { emit ->
            emit(ProcessResult2(1, "one"))
            emit(ProcessResult2(2, "two"))
            emit(ProcessResult2(3, "three"))
        }

        // When: Start the generator
        generator.start(this) { }
        advanceUntilIdle()

        // Then: Both output channels should have values
        val output1 = generator.outputChannel1!!
        val output2 = generator.outputChannel2!!

        assertEquals(1, output1.receive())
        assertEquals("one", output2.receive())
        assertEquals(2, output1.receive())
        assertEquals("two", output2.receive())
        assertEquals(3, output1.receive())
        assertEquals("three", output2.receive())

        generator.stop()
    }

    @Test
    fun `Out3GeneratorRuntime emits ProcessResult3 to three output channels`() = runTest {
        // Given: A generator that emits triples
        val generator = CodeNodeFactory.createOut3Generator<Int, String, Boolean>(
            name = "TripleGenerator"
        ) { emit ->
            emit(ProcessResult3(1, "a", true))
            emit(ProcessResult3(2, "b", false))
        }

        // When: Start the generator
        generator.start(this) { }
        advanceUntilIdle()

        // Then: All three output channels should have values
        val output1 = generator.outputChannel1!!
        val output2 = generator.outputChannel2!!
        val output3 = generator.outputChannel3!!

        assertEquals(1, output1.receive())
        assertEquals("a", output2.receive())
        assertEquals(true, output3.receive())
        assertEquals(2, output1.receive())
        assertEquals("b", output2.receive())
        assertEquals(false, output3.receive())

        generator.stop()
    }
}
