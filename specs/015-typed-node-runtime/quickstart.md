# Quickstart: Typed NodeRuntime Stubs

**Feature**: 015-typed-node-runtime
**Date**: 2026-02-15

## Continuous Mode Processing

All typed node runtimes operate in **continuous mode** by default, consistent with feature 014. Nodes continuously process input tuples in a loop until `stop()` is called. Use `runTest` with virtual time (`advanceTimeBy`, `advanceUntilIdle`) for deterministic testing.

## Basic Usage Examples

### Example 1: Two-Input Processor (In2Out1)

Create a node that continuously combines two inputs into a single output.

```kotlin
import io.codenode.fbpdsl.model.CodeNodeFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest

data class FullName(val full: String)
data class FirstName(val name: String)
data class LastName(val name: String)

@OptIn(ExperimentalCoroutinesApi::class)
fun `two-input processor continuously combines names`() = runTest {
    // Create processor that combines first and last name
    val combiner = CodeNodeFactory.createIn2Out1Processor<FirstName, LastName, FullName>(
        name = "NameCombiner"
    ) { first, last ->
        FullName("${first.name} ${last.name}")
    }

    // Create channels
    val firstNameChannel = Channel<FirstName>(Channel.BUFFERED)
    val lastNameChannel = Channel<LastName>(Channel.BUFFERED)
    val outputChannel = Channel<FullName>(Channel.BUFFERED)

    // Wire channels
    combiner.inputChannel = firstNameChannel
    combiner.inputChannel2 = lastNameChannel
    combiner.processorOutputChannel = outputChannel

    // Start processor - runs continuously until stopped
    combiner.start(this) { }

    // Send multiple tuples - node processes each continuously
    firstNameChannel.send(FirstName("John"))
    lastNameChannel.send(LastName("Doe"))
    advanceUntilIdle()

    firstNameChannel.send(FirstName("Jane"))
    lastNameChannel.send(LastName("Smith"))
    advanceUntilIdle()

    // Receive results - both processed by same running node
    assertEquals("John Doe", outputChannel.receive().full)
    assertEquals("Jane Smith", outputChannel.receive().full)

    combiner.stop()
}
```

### Example 2: Multi-Output Processor (In1Out2)

Create a node that continuously splits input into two typed outputs.

```kotlin
import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.ProcessResult2
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest

data class Person(val name: String, val age: Int)
data class Name(val value: String)
data class Age(val value: Int)

@OptIn(ExperimentalCoroutinesApi::class)
fun `multi-output processor continuously extracts fields`() = runTest {
    // Create processor that extracts name and age
    val extractor = CodeNodeFactory.createIn1Out2Processor<Person, Name, Age>(
        name = "PersonExtractor"
    ) { person ->
        ProcessResult2.both(Name(person.name), Age(person.age))
    }

    // Create input channel
    val inputChannel = Channel<Person>(Channel.BUFFERED)

    // Wire input channel (output channels are created by the runtime)
    extractor.inputChannel = inputChannel

    // Get output channels from runtime
    val nameChannel = extractor.outputChannel1!!
    val ageChannel = extractor.outputChannel2!!

    // Start processor - runs continuously
    extractor.start(this) { }

    // Stream multiple values through the same running node
    inputChannel.send(Person("Alice", 30))
    inputChannel.send(Person("Bob", 25))
    advanceUntilIdle()

    // Both processed continuously
    assertEquals("Alice", nameChannel.receive().value)
    assertEquals(30, ageChannel.receive().value)
    assertEquals("Bob", nameChannel.receive().value)
    assertEquals(25, ageChannel.receive().value)

    extractor.stop()
}
```

### Example 3: Multi-Output with Selective Sending

Continuously route values to different outputs based on conditions.

```kotlin
import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.ProcessResult2
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest

data class Number(val value: Int)
data class EvenNumber(val value: Int)
data class OddNumber(val value: Int)

@OptIn(ExperimentalCoroutinesApi::class)
fun `router continuously routes to different outputs`() = runTest {
    // Router that sends to different outputs based on parity
    val router = CodeNodeFactory.createIn1Out2Processor<Number, EvenNumber, OddNumber>(
        name = "EvenOddRouter"
    ) { number ->
        if (number.value % 2 == 0) {
            ProcessResult2.first(EvenNumber(number.value))  // Only first output
        } else {
            ProcessResult2.second(OddNumber(number.value))  // Only second output
        }
    }

    val inputChannel = Channel<Number>(Channel.BUFFERED)

    router.inputChannel = inputChannel

    // Get output channels from runtime
    val evenChannel = router.outputChannel1!!
    val oddChannel = router.outputChannel2!!

    // Start - runs continuously
    router.start(this) { }

    // Stream values - each routed to appropriate output
    inputChannel.send(Number(2))
    inputChannel.send(Number(3))
    inputChannel.send(Number(4))
    advanceUntilIdle()

    // Selective outputs: 2 and 4 to even, 3 to odd
    assertEquals(2, evenChannel.receive().value)
    assertEquals(3, oddChannel.receive().value)
    assertEquals(4, evenChannel.receive().value)

    router.stop()
}
```

### Example 4: Two-Input Sink (In2Sink)

Create a sink that continuously consumes from two input channels.

```kotlin
import io.codenode.fbpdsl.model.CodeNodeFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest

data class Key(val id: String)
data class Value(val data: String)

@OptIn(ExperimentalCoroutinesApi::class)
fun `two-input sink continuously consumes key-value pairs`() = runTest {
    val results = mutableListOf<Pair<String, String>>()

    // Create sink that receives key-value pairs continuously
    val kvSink = CodeNodeFactory.createIn2Sink<Key, Value>(
        name = "KeyValueSink"
    ) { key, value ->
        results.add(key.id to value.data)
    }

    val keyChannel = Channel<Key>(Channel.BUFFERED)
    val valueChannel = Channel<Value>(Channel.BUFFERED)

    kvSink.inputChannel = keyChannel
    kvSink.inputChannel2 = valueChannel

    // Start sink - runs continuously
    kvSink.start(this) { }

    // Send multiple tuples - all consumed by same running sink
    keyChannel.send(Key("user-1"))
    valueChannel.send(Value("John Doe"))

    keyChannel.send(Key("user-2"))
    valueChannel.send(Value("Jane Smith"))
    advanceUntilIdle()

    // Verify continuous processing
    assertEquals(2, results.size)
    assertEquals("user-1" to "John Doe", results[0])
    assertEquals("user-2" to "Jane Smith", results[1])

    kvSink.stop()
}
```

### Example 5: Two-Output Generator (Out2Generator)

Create a generator that continuously emits to two output channels.

```kotlin
import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.ProcessResult2
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest

data class Timestamp(val tick: Int)
data class TickCount(val count: Int)

@OptIn(ExperimentalCoroutinesApi::class)
fun `two-output generator continuously emits to both channels`() = runTest {
    // Generator that continuously emits both timestamp and tick count
    val ticker = CodeNodeFactory.createOut2Generator<Timestamp, TickCount>(
        name = "DualTicker"
    ) { emit ->
        var count = 0
        while (currentCoroutineContext().isActive) {
            count++
            emit(ProcessResult2.both(
                Timestamp(count),
                TickCount(count)
            ))
            delay(100)  // Virtual time - instant in tests
        }
    }

    // Get output channels from runtime (created internally)
    val timestampChannel = ticker.outputChannel1!!
    val tickChannel = ticker.outputChannel2!!

    // Start generator - runs continuously
    ticker.start(this) { }

    // Advance virtual time to trigger emissions
    advanceTimeBy(350)
    advanceUntilIdle()

    // Verify continuous emissions (3 ticks at 100ms intervals)
    assertEquals(1, timestampChannel.receive().tick)
    assertEquals(1, tickChannel.receive().count)
    assertEquals(2, timestampChannel.receive().tick)
    assertEquals(2, tickChannel.receive().count)
    assertEquals(3, timestampChannel.receive().tick)
    assertEquals(3, tickChannel.receive().count)

    ticker.stop()
}
```

### Example 6: Full Pipeline with Continuous Flow

Connect generator → transformer → sink in a continuous pipeline.

```kotlin
import io.codenode.fbpdsl.model.CodeNodeFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
fun `full pipeline processes data continuously`() = runTest {
    val received = mutableListOf<Int>()

    // Generator: emits 1, 2, 3... continuously
    val generator = CodeNodeFactory.createContinuousGenerator<Int>(
        name = "Counter"
    ) { emit ->
        var n = 0
        while (currentCoroutineContext().isActive) {
            emit(++n)
            delay(100)
        }
    }

    // Transformer: doubles each value continuously
    val doubler = CodeNodeFactory.createIn2Out1Processor<Int, Int, Int>(
        name = "Doubler"
    ) { a, b -> a * b }  // Using In2Out1 to multiply by constant

    // Sink: collects values continuously
    val collector = CodeNodeFactory.createContinuousSink<Int>(
        name = "Collector"
    ) { value ->
        received.add(value)
    }

    // Wire the pipeline
    val genToDoubler = generator.outputChannel!!
    val constantChannel = Channel<Int>(Channel.BUFFERED)
    val doublerToSink = Channel<Int>(Channel.BUFFERED)

    doubler.inputChannel = genToDoubler
    doubler.inputChannel2 = constantChannel
    doubler.processorOutputChannel = doublerToSink
    collector.inputChannel = doublerToSink

    // Start all nodes - all run continuously
    collector.start(this) { }
    doubler.start(this) { }
    generator.start(this) { }

    // Feed constant multiplier for each value
    repeat(3) { constantChannel.send(2) }

    advanceTimeBy(350)
    advanceUntilIdle()

    // Verify continuous processing: 1*2, 2*2, 3*2
    assertEquals(listOf(2, 4, 6), received)

    // Stop all nodes
    generator.stop()
    doubler.stop()
    collector.stop()
}
```

---

## Testing Patterns

### Virtual Time Testing

All tests use `runTest` with virtual time for deterministic behavior:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class TypedNodeRuntimeTest {

    @Test
    fun `continuous processor handles multiple tuples`() = runTest {
        val combiner = CodeNodeFactory.createIn2Out1Processor<Int, Int, Int>(
            name = "Adder"
        ) { a, b -> a + b }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val output = Channel<Int>(Channel.BUFFERED)

        combiner.inputChannel = input1
        combiner.inputChannel2 = input2
        combiner.processorOutputChannel = output

        combiner.start(this) { }

        // Process multiple tuples continuously
        repeat(3) { i ->
            input1.send(i)
            input2.send(10)
        }
        advanceUntilIdle()

        // All processed by same running node
        assertEquals(10, output.receive())  // 0 + 10
        assertEquals(11, output.receive())  // 1 + 10
        assertEquals(12, output.receive())  // 2 + 10

        combiner.stop()
    }

    @Test
    fun `processor handles channel closure gracefully`() = runTest {
        val combiner = CodeNodeFactory.createIn2Out1Processor<Int, Int, Int>(
            name = "Adder"
        ) { a, b -> a + b }

        val input1 = Channel<Int>(Channel.BUFFERED)
        val input2 = Channel<Int>(Channel.BUFFERED)
        val output = Channel<Int>(Channel.BUFFERED)

        combiner.inputChannel = input1
        combiner.inputChannel2 = input2
        combiner.processorOutputChannel = output

        combiner.start(this) { }

        // Close one input channel
        input1.close()
        advanceUntilIdle()

        // Output should be closed, node should be idle
        assertTrue(output.isClosedForReceive)
        assertTrue(combiner.isIdle())
    }

    @Test
    fun `pause and resume control continuous processing`() = runTest {
        val processed = mutableListOf<Int>()

        val sink = CodeNodeFactory.createContinuousSink<Int>(
            name = "Collector"
        ) { value -> processed.add(value) }

        val input = Channel<Int>(Channel.BUFFERED)
        sink.inputChannel = input

        sink.start(this) { }

        // Process while running
        input.send(1)
        advanceUntilIdle()
        assertEquals(listOf(1), processed)

        // Pause - node stops processing
        sink.pause()
        input.send(2)
        advanceUntilIdle()
        assertEquals(listOf(1), processed)  // Still 1, not processing

        // Resume - continues processing
        sink.resume()
        advanceUntilIdle()
        assertEquals(listOf(1, 2), processed)  // Now includes 2

        sink.stop()
    }
}
```

---

## Verification Checklist

- [ ] `createIn2Out1Processor` creates runtime with 2 inputs, 1 output
- [ ] `createIn1Out2Processor` creates runtime with 1 input, 2 outputs
- [ ] `createIn2Sink` creates sink with 2 inputs
- [ ] `createOut2Generator` creates generator with 2 outputs
- [ ] ProcessResult2 supports destructuring: `val (a, b) = result`
- [ ] ProcessResult3 supports destructuring: `val (a, b, c) = result`
- [ ] Selective output works: null values are not sent
- [ ] Channel closure triggers graceful shutdown
- [ ] Lifecycle control (start/stop/pause/resume) works on all runtimes
- [ ] Factory rejects 0-input AND 0-output configuration
- [ ] All 15 valid configurations have factory methods
- [ ] **Continuous mode**: All runtimes process in a loop until stopped
- [ ] **Virtual time**: Tests use runTest with advanceTimeBy/advanceUntilIdle
