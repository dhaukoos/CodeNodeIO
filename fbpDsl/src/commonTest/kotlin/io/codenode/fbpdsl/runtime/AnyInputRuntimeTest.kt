/*
 * AnyInputRuntimeTest - Tests for any-input runtime variants
 * Verifies select-based concurrent channel listening behavior
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
class AnyInputRuntimeTest {

    // ========== In2AnyOut1Runtime ==========

    @Test
    fun `In2AnyOut1Runtime fires when only input1 receives data`() = runTest {
        val processor = CodeNodeFactory.createIn2AnyOut1Processor<Int, Int, Int>(
            name = "AnyAdder",
            initialValue1 = 0,
            initialValue2 = 0
        ) { a, b -> a + b }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val output = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel1 = input1
        processor.inputChannel2 = input2
        processor.outputChannel = output

        processor.start(this) { }

        // Send only to input1 — should fire with initialValue2 (0)
        input1.send(5)
        advanceUntilIdle()

        assertEquals(5, output.receive()) // 5 + 0

        processor.stop()
    }

    @Test
    fun `In2AnyOut1Runtime fires when only input2 receives data`() = runTest {
        val processor = CodeNodeFactory.createIn2AnyOut1Processor<Int, Int, Int>(
            name = "AnyAdder",
            initialValue1 = 0,
            initialValue2 = 0
        ) { a, b -> a + b }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val output = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel1 = input1
        processor.inputChannel2 = input2
        processor.outputChannel = output

        processor.start(this) { }

        // Send only to input2 — should fire with initialValue1 (0)
        input2.send(7)
        advanceUntilIdle()

        assertEquals(7, output.receive()) // 0 + 7

        processor.stop()
    }

    @Test
    fun `In2AnyOut1Runtime caches last values across triggers`() = runTest {
        val processor = CodeNodeFactory.createIn2AnyOut1Processor<Int, Int, Int>(
            name = "AnyAdder",
            initialValue1 = 0,
            initialValue2 = 0
        ) { a, b -> a + b }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val output = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel1 = input1
        processor.inputChannel2 = input2
        processor.outputChannel = output

        processor.start(this) { }

        // First: send to input1 (caches 10)
        input1.send(10)
        advanceUntilIdle()
        assertEquals(10, output.receive()) // 10 + 0

        // Second: send to input2 (uses cached 10 for input1)
        input2.send(3)
        advanceUntilIdle()
        assertEquals(13, output.receive()) // 10 + 3

        // Third: send to input1 again (uses cached 3 for input2)
        input1.send(20)
        advanceUntilIdle()
        assertEquals(23, output.receive()) // 20 + 3

        processor.stop()
    }

    @Test
    fun `In2AnyOut1Runtime pause suspends processing until resume`() = runTest {
        val processor = CodeNodeFactory.createIn2AnyOut1Processor<Int, Int, Int>(
            name = "AnyAdder",
            initialValue1 = 0,
            initialValue2 = 0
        ) { a, b -> a + b }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val output = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel1 = input1
        processor.inputChannel2 = input2
        processor.outputChannel = output

        processor.start(this) { }

        input1.send(5)
        advanceUntilIdle()
        assertEquals(5, output.receive())

        // Pause
        processor.pause()
        assertEquals(ExecutionState.PAUSED, processor.executionState)

        // Resume
        processor.resume()
        assertEquals(ExecutionState.RUNNING, processor.executionState)

        input2.send(8)
        advanceUntilIdle()
        assertEquals(13, output.receive()) // cached 5 + 8

        processor.stop()
    }

    @Test
    fun `In2AnyOut1Runtime stop transitions to IDLE`() = runTest {
        val processor = CodeNodeFactory.createIn2AnyOut1Processor<Int, Int, Int>(
            name = "AnyAdder",
            initialValue1 = 0,
            initialValue2 = 0
        ) { a, b -> a + b }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val output = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel1 = input1
        processor.inputChannel2 = input2
        processor.outputChannel = output

        processor.start(this) { }
        advanceUntilIdle()

        processor.stop()
        advanceUntilIdle()

        assertEquals(ExecutionState.IDLE, processor.executionState)
    }

    @Test
    fun `In2AnyOut1Runtime resetCachedValues clears to initial defaults`() = runTest {
        val processor = CodeNodeFactory.createIn2AnyOut1Processor<Int, Int, Int>(
            name = "AnyAdder",
            initialValue1 = 0,
            initialValue2 = 0
        ) { a, b -> a + b }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val output = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel1 = input1
        processor.inputChannel2 = input2
        processor.outputChannel = output

        processor.start(this) { }

        // Cache a value
        input1.send(100)
        advanceUntilIdle()
        assertEquals(100, output.receive()) // 100 + 0

        processor.stop()
        advanceUntilIdle()

        // Reset cached values
        processor.resetCachedValues()

        // Restart with fresh channels
        val newInput1 = Channel<Int>(Channel.BUFFERED)
        val newInput2 = Channel<Int>(Channel.BUFFERED)
        val newOutput = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel1 = newInput1
        processor.inputChannel2 = newInput2
        processor.outputChannel = newOutput

        processor.start(this) { }

        // After reset, cached values should be back to initial (0, 0)
        newInput2.send(5)
        advanceUntilIdle()
        assertEquals(5, newOutput.receive()) // 0 + 5 (not 100 + 5)

        processor.stop()
    }

    // ========== In2AnySinkRuntime ==========

    @Test
    fun `In2AnySinkRuntime fires when any input receives data`() = runTest {
        val received = mutableListOf<Pair<Int, String>>()

        val sink = CodeNodeFactory.createIn2AnySink<Int, String>(
            name = "AnySink",
            initialValue1 = 0,
            initialValue2 = ""
        ) { a, b -> received.add(a to b) }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<String>(Channel.BUFFERED)

        sink.inputChannel1 = input1
        sink.inputChannel2 = input2

        sink.start(this) { }

        // Fire with just input1
        input1.send(42)
        advanceUntilIdle()
        assertEquals(1, received.size)
        assertEquals(42 to "", received[0]) // 42, initial ""

        // Fire with just input2 (cached 42 for input1)
        input2.send("hello")
        advanceUntilIdle()
        assertEquals(2, received.size)
        assertEquals(42 to "hello", received[1])

        sink.stop()
    }

    // ========== In2AnyOut2Runtime ==========

    @Test
    fun `In2AnyOut2Runtime fires on any input with selective output`() = runTest {
        val processor = CodeNodeFactory.createIn2AnyOut2Processor<Int, Int, Int, String>(
            name = "AnyDualOut",
            initialValue1 = 0,
            initialValue2 = 0
        ) { a, b -> ProcessResult2(a + b, "sum=${a + b}") }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val output1 = Channel<Int>(Channel.BUFFERED)
        val output2 = Channel<String>(Channel.BUFFERED)

        processor.inputChannel1 = input1
        processor.inputChannel2 = input2
        processor.outputChannel1 = output1
        processor.outputChannel2 = output2

        processor.start(this) { }

        input1.send(10)
        advanceUntilIdle()

        assertEquals(10, output1.receive())
        assertEquals("sum=10", output2.receive())

        processor.stop()
    }

    // ========== In2AnyOut3Runtime ==========

    @Test
    fun `In2AnyOut3Runtime fires on any input with 3 outputs`() = runTest {
        val processor = CodeNodeFactory.createIn2AnyOut3Processor<Int, Int, Int, String, Boolean>(
            name = "AnyTripleOut",
            initialValue1 = 0,
            initialValue2 = 0
        ) { a, b ->
            val sum = a + b
            ProcessResult3(sum, "sum=$sum", sum > 10)
        }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val output1 = Channel<Int>(Channel.BUFFERED)
        val output2 = Channel<String>(Channel.BUFFERED)
        val output3 = Channel<Boolean>(Channel.BUFFERED)

        processor.inputChannel1 = input1
        processor.inputChannel2 = input2
        processor.outputChannel1 = output1
        processor.outputChannel2 = output2
        processor.outputChannel3 = output3

        processor.start(this) { }

        input2.send(15)
        advanceUntilIdle()

        assertEquals(15, output1.receive())
        assertEquals("sum=15", output2.receive())
        assertEquals(true, output3.receive())

        processor.stop()
    }

    // ========== In3AnyOut1Runtime ==========

    @Test
    fun `In3AnyOut1Runtime fires when any of 3 inputs receives data`() = runTest {
        val processor = CodeNodeFactory.createIn3AnyOut1Processor<Int, Int, Int, Int>(
            name = "AnyTripleAdder",
            initialValue1 = 0,
            initialValue2 = 0,
            initialValue3 = 0
        ) { a, b, c -> a + b + c }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val input3 = Channel<Int>(Channel.BUFFERED)
        val output = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel1 = input1
        processor.inputChannel2 = input2
        processor.inputChannel3 = input3
        processor.outputChannel = output

        processor.start(this) { }

        // Fire with just input1
        input1.send(10)
        advanceUntilIdle()
        assertEquals(10, output.receive()) // 10 + 0 + 0

        // Fire with just input3 (cached: 10, 0)
        input3.send(5)
        advanceUntilIdle()
        assertEquals(15, output.receive()) // 10 + 0 + 5

        // Fire with just input2 (cached: 10, 5)
        input2.send(3)
        advanceUntilIdle()
        assertEquals(18, output.receive()) // 10 + 3 + 5

        processor.stop()
    }

    @Test
    fun `In3AnyOut1Runtime caches and resets correctly`() = runTest {
        val processor = CodeNodeFactory.createIn3AnyOut1Processor<Int, Int, Int, Int>(
            name = "AnyTripleAdder",
            initialValue1 = 0,
            initialValue2 = 0,
            initialValue3 = 0
        ) { a, b, c -> a + b + c }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val input3 = Channel<Int>(Channel.BUFFERED)
        val output = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel1 = input1
        processor.inputChannel2 = input2
        processor.inputChannel3 = input3
        processor.outputChannel = output

        processor.start(this) { }

        // Cache values
        input1.send(10)
        advanceUntilIdle()
        output.receive()

        input2.send(20)
        advanceUntilIdle()
        output.receive()

        processor.stop()
        advanceUntilIdle()

        // Reset
        processor.resetCachedValues()

        val newInput1 = Channel<Int>(Channel.BUFFERED)
        val newInput2 = Channel<Int>(Channel.BUFFERED)
        val newInput3 = Channel<Int>(Channel.BUFFERED)
        val newOutput = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel1 = newInput1
        processor.inputChannel2 = newInput2
        processor.inputChannel3 = newInput3
        processor.outputChannel = newOutput

        processor.start(this) { }

        // After reset, all should be back to 0
        newInput3.send(7)
        advanceUntilIdle()
        assertEquals(7, newOutput.receive()) // 0 + 0 + 7

        processor.stop()
    }

    // ========== In3AnySinkRuntime ==========

    @Test
    fun `In3AnySinkRuntime fires when any of 3 inputs receives data`() = runTest {
        val received = mutableListOf<Triple<Int, Int, Int>>()

        val sink = CodeNodeFactory.createIn3AnySink<Int, Int, Int>(
            name = "AnyTripleSink",
            initialValue1 = 0,
            initialValue2 = 0,
            initialValue3 = 0
        ) { a, b, c -> received.add(Triple(a, b, c)) }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val input3 = Channel<Int>(Channel.BUFFERED)

        sink.inputChannel1 = input1
        sink.inputChannel2 = input2
        sink.inputChannel3 = input3

        sink.start(this) { }

        input1.send(1)
        advanceUntilIdle()
        assertEquals(Triple(1, 0, 0), received.last())

        input2.send(2)
        advanceUntilIdle()
        assertEquals(Triple(1, 2, 0), received.last())

        input3.send(3)
        advanceUntilIdle()
        assertEquals(Triple(1, 2, 3), received.last())

        sink.stop()
    }

    // ========== In3AnyOut2Runtime ==========

    @Test
    fun `In3AnyOut2Runtime fires on any input with 2 outputs`() = runTest {
        val processor = CodeNodeFactory.createIn3AnyOut2Processor<Int, Int, Int, Int, String>(
            name = "Any3to2",
            initialValue1 = 0,
            initialValue2 = 0,
            initialValue3 = 0
        ) { a, b, c ->
            val sum = a + b + c
            ProcessResult2(sum, "total=$sum")
        }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val input3 = Channel<Int>(Channel.BUFFERED)
        val output1 = Channel<Int>(Channel.BUFFERED)
        val output2 = Channel<String>(Channel.BUFFERED)

        processor.inputChannel1 = input1
        processor.inputChannel2 = input2
        processor.inputChannel3 = input3
        processor.outputChannel1 = output1
        processor.outputChannel2 = output2

        processor.start(this) { }

        input2.send(10)
        advanceUntilIdle()

        assertEquals(10, output1.receive())
        assertEquals("total=10", output2.receive())

        processor.stop()
    }

    // ========== In3AnyOut3Runtime ==========

    @Test
    fun `In3AnyOut3Runtime fires on any input with 3 outputs`() = runTest {
        val processor = CodeNodeFactory.createIn3AnyOut3Processor<Int, Int, Int, Int, String, Boolean>(
            name = "Any3to3",
            initialValue1 = 0,
            initialValue2 = 0,
            initialValue3 = 0
        ) { a, b, c ->
            val sum = a + b + c
            ProcessResult3(sum, "sum=$sum", sum > 5)
        }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val input3 = Channel<Int>(Channel.BUFFERED)
        val output1 = Channel<Int>(Channel.BUFFERED)
        val output2 = Channel<String>(Channel.BUFFERED)
        val output3 = Channel<Boolean>(Channel.BUFFERED)

        processor.inputChannel1 = input1
        processor.inputChannel2 = input2
        processor.inputChannel3 = input3
        processor.outputChannel1 = output1
        processor.outputChannel2 = output2
        processor.outputChannel3 = output3

        processor.start(this) { }

        input3.send(8)
        advanceUntilIdle()

        assertEquals(8, output1.receive())
        assertEquals("sum=8", output2.receive())
        assertEquals(true, output3.receive())

        processor.stop()
    }

    // ========== Channel Closure ==========

    @Test
    fun `In2AnyOut1Runtime handles ClosedReceiveChannelException gracefully`() = runTest {
        val processor = CodeNodeFactory.createIn2AnyOut1Processor<Int, Int, Int>(
            name = "AnyAdder",
            initialValue1 = 0,
            initialValue2 = 0
        ) { a, b -> a + b }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val output = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel1 = input1
        processor.inputChannel2 = input2
        processor.outputChannel = output

        processor.start(this) { }
        advanceUntilIdle()

        // Close both input channels
        input1.close()
        input2.close()
        advanceUntilIdle()

        assertEquals(ExecutionState.IDLE, processor.executionState)
    }

    @Test
    fun `In2AnyOut1Runtime handles ClosedSendChannelException gracefully`() = runTest {
        val processor = CodeNodeFactory.createIn2AnyOut1Processor<Int, Int, Int>(
            name = "AnyAdder",
            initialValue1 = 0,
            initialValue2 = 0
        ) { a, b -> a + b }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val output = Channel<Int>(Channel.BUFFERED)

        processor.inputChannel1 = input1
        processor.inputChannel2 = input2
        processor.outputChannel = output

        processor.start(this) { }

        // Close output, then try to trigger
        input1.send(1)
        output.close()
        advanceUntilIdle()

        assertEquals(ExecutionState.IDLE, processor.executionState)
    }
}
