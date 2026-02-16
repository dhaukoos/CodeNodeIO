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

    // ========== User Story 4: ProcessResult for Multi-Output Nodes ==========

    @Test
    fun `In1Out2Runtime sends ProcessResult2 to two outputs`() = runTest {
        // Given: A processor that splits an integer into two outputs
        val processor = CodeNodeFactory.createIn1Out2Processor<Int, Int, String>(
            name = "Splitter"
        ) { value -> ProcessResult2(value * 2, "value=$value") }

        val input = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel = input
        val output1 = processor.outputChannel1!!
        val output2 = processor.outputChannel2!!

        // When: Start and send values
        processor.start(this) { }

        input.send(5)
        advanceUntilIdle()

        input.send(10)
        advanceUntilIdle()

        // Then: Both outputs should have values
        assertEquals(10, output1.receive())
        assertEquals("value=5", output2.receive())
        assertEquals(20, output1.receive())
        assertEquals("value=10", output2.receive())

        processor.stop()
    }

    @Test
    fun `In1Out2Runtime skips sending null values (selective output)`() = runTest {
        // Given: A processor that conditionally outputs to channels
        val processor = CodeNodeFactory.createIn1Out2Processor<Int, Int, String>(
            name = "SelectiveSplitter"
        ) { value ->
            if (value % 2 == 0) {
                ProcessResult2.first(value) // Only send to first output
            } else {
                ProcessResult2.second("odd=$value") // Only send to second output
            }
        }

        val input = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel = input
        val output1 = processor.outputChannel1!!
        val output2 = processor.outputChannel2!!

        // When: Start and send values
        processor.start(this) { }

        input.send(2) // Even - goes to output1
        input.send(3) // Odd - goes to output2
        input.send(4) // Even - goes to output1
        advanceUntilIdle()

        // Then: Values should be routed correctly
        assertEquals(2, output1.receive())
        assertEquals("odd=3", output2.receive())
        assertEquals(4, output1.receive())

        processor.stop()
    }

    @Test
    fun `In1Out3Runtime sends ProcessResult3 to three outputs`() = runTest {
        // Given: A processor that splits input to three outputs
        val processor = CodeNodeFactory.createIn1Out3Processor<Int, Int, String, Boolean>(
            name = "TripleSplitter"
        ) { value -> ProcessResult3(value, "v=$value", value > 0) }

        val input = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel = input
        val output1 = processor.outputChannel1!!
        val output2 = processor.outputChannel2!!
        val output3 = processor.outputChannel3!!

        // When: Start and send values
        processor.start(this) { }

        input.send(5)
        advanceUntilIdle()

        // Then: All three outputs should have values
        assertEquals(5, output1.receive())
        assertEquals("v=5", output2.receive())
        assertEquals(true, output3.receive())

        processor.stop()
    }

    @Test
    fun `In2Out2Runtime combines multi-input with multi-output`() = runTest {
        // Given: A processor with 2 inputs and 2 outputs
        val processor = CodeNodeFactory.createIn2Out2Processor<Int, Int, Int, String>(
            name = "DualProcessor"
        ) { a, b -> ProcessResult2(a + b, "sum=${a + b}") }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel = input1
        processor.inputChannel2 = input2
        val output1 = processor.outputChannel1!!
        val output2 = processor.outputChannel2!!

        // When: Start and send values
        processor.start(this) { }

        input1.send(3)
        input2.send(7)
        advanceUntilIdle()

        // Then: Both outputs should have processed values
        assertEquals(10, output1.receive())
        assertEquals("sum=10", output2.receive())

        processor.stop()
    }

    @Test
    fun `ProcessResult2 destructuring works correctly`() = runTest {
        // Given: A ProcessResult2 value
        val result = ProcessResult2(42, "hello")

        // When: Destructure it
        val (out1, out2) = result

        // Then: Values should match
        assertEquals(42, out1)
        assertEquals("hello", out2)
    }

    @Test
    fun `ProcessResult3 destructuring works correctly`() = runTest {
        // Given: A ProcessResult3 value
        val result = ProcessResult3(1, "two", true)

        // When: Destructure it
        val (out1, out2, out3) = result

        // Then: Values should match
        assertEquals(1, out1)
        assertEquals("two", out2)
        assertEquals(true, out3)
    }

    // ========== User Story 5: Named Node Objects ==========

    @Test
    fun `factory method name parameter is reflected in CodeNode name`() = runTest {
        // Given: Various processors with specific names
        val processor1 = CodeNodeFactory.createIn2Out1Processor<Int, Int, Int>(
            name = "MyAdder"
        ) { a, b -> a + b }

        val processor2 = CodeNodeFactory.createIn1Out2Processor<Int, Int, String>(
            name = "MySplitter"
        ) { v -> ProcessResult2(v, v.toString()) }

        val sink = CodeNodeFactory.createIn2Sink<Int, String>(
            name = "MyCollector"
        ) { _, _ -> }

        val generator = CodeNodeFactory.createOut2Generator<Int, String>(
            name = "MyGenerator"
        ) { emit -> emit(ProcessResult2(1, "one")) }

        // Then: CodeNode names should match
        assertEquals("MyAdder", processor1.codeNode.name)
        assertEquals("MySplitter", processor2.codeNode.name)
        assertEquals("MyCollector", sink.codeNode.name)
        assertEquals("MyGenerator", generator.codeNode.name)
    }

    @Test
    fun `multiple nodes have unique names for identification`() = runTest {
        // Given: Multiple processors with different names
        val adder1 = CodeNodeFactory.createIn2Out1Processor<Int, Int, Int>(
            name = "Adder1"
        ) { a, b -> a + b }

        val adder2 = CodeNodeFactory.createIn2Out1Processor<Int, Int, Int>(
            name = "Adder2"
        ) { a, b -> a + b }

        val multiplier = CodeNodeFactory.createIn2Out1Processor<Int, Int, Int>(
            name = "Multiplier"
        ) { a, b -> a * b }

        // Then: Each node should have its own unique name
        assertEquals("Adder1", adder1.codeNode.name)
        assertEquals("Adder2", adder2.codeNode.name)
        assertEquals("Multiplier", multiplier.codeNode.name)

        // And: Names should be distinct
        assertTrue(adder1.codeNode.name != adder2.codeNode.name)
        assertTrue(adder1.codeNode.name != multiplier.codeNode.name)
    }

    // ========== Polish: Edge Cases ==========

    @Test
    fun `typed factories prevent 0x0 configuration by design`() = runTest {
        // The typed factory methods inherently prevent 0-input AND 0-output configurations:
        // - Sinks: In2Sink, In3Sink have inputs, no outputs
        // - Generators: Out2Generator, Out3Generator have outputs, no inputs
        // - Processors: In2Out1, In1Out2, etc. have both inputs and outputs
        //
        // There is no createIn0Out0 factory method, so this test documents that
        // the type system enforces valid configurations.

        // Verify minimum configurations exist and work:
        val sink = CodeNodeFactory.createIn2Sink<Int, Int>(name = "MinSink") { _, _ -> }
        assertTrue(sink.codeNode.inputPorts.isNotEmpty())
        assertTrue(sink.codeNode.outputPorts.isEmpty())

        val generator = CodeNodeFactory.createOut2Generator<Int, Int>(name = "MinGen") { emit ->
            emit(ProcessResult2(1, 2))
        }
        assertTrue(generator.codeNode.inputPorts.isEmpty())
        assertTrue(generator.codeNode.outputPorts.isNotEmpty())

        val processor = CodeNodeFactory.createIn2Out1Processor<Int, Int, Int>(name = "MinProc") { a, b -> a + b }
        assertTrue(processor.codeNode.inputPorts.isNotEmpty())
        assertTrue(processor.codeNode.outputPorts.isNotEmpty())
    }
}
