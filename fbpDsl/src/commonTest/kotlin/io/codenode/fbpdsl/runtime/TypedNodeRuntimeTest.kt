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
}
