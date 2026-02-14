# Quickstart: Continuous Mode as Default

## Overview

This guide shows how to use the new continuous factory methods to create nodes that run in loops, processing data through channels.

## Step 1: Create a Continuous Generator

**File**: Example usage

### Before (Custom Component)

```kotlin
// Had to create custom component class
class TimerEmitterComponent(
    private val speedAttenuation: Long = 1000L
) : ProcessingLogic {
    var codeNode: CodeNode? = CodeNode(...)
    var outputChannel: SendChannel<TimerOutput>? = null

    suspend fun start(scope: CoroutineScope) {
        val node = codeNode ?: return
        node.start(scope) {
            while (currentCoroutineContext().isActive && executionState == ExecutionState.RUNNING) {
                delay(speedAttenuation)
                // ... tick logic ...
                outputChannel?.send(timerOutput)
            }
        }
    }

    fun stop() {
        codeNode?.stop()
    }
}
```

### After (Factory Method)

```kotlin
// Create with factory - no custom class needed!
val timerRuntime = CodeNodeFactory.createContinuousGenerator<TimerOutput>(
    name = "TimerEmitter",
    description = "Emits elapsed time every second"
) { emit ->
    var seconds = 0
    var minutes = 0

    while (isActive) {
        delay(1000)

        seconds++
        if (seconds >= 60) {
            seconds = 0
            minutes++
        }

        emit(TimerOutput(seconds, minutes))
    }
}

// Start the generator
timerRuntime.start(scope) {
    // Loop is managed internally
}

// Stop when done
timerRuntime.stop()
```

## Step 2: Create a Continuous Sink

### Before (Custom Component)

```kotlin
class DisplayReceiverComponent : ProcessingLogic {
    var codeNode: CodeNode? = CodeNode(...)
    var inputChannel: ReceiveChannel<TimerOutput>? = null

    private val _displayedSeconds = MutableStateFlow(0)
    val displayedSecondsFlow: StateFlow<Int> = _displayedSeconds.asStateFlow()

    suspend fun start(scope: CoroutineScope) {
        val node = codeNode ?: return
        val channel = inputChannel ?: return

        node.start(scope) {
            for (timerOutput in channel) {
                _displayedSeconds.value = timerOutput.elapsedSeconds
            }
        }
    }
}
```

### After (Factory Method)

```kotlin
// State can be captured from outer scope
val displayedSeconds = MutableStateFlow(0)
val displayedMinutes = MutableStateFlow(0)

val displayRuntime = CodeNodeFactory.createContinuousSink<TimerOutput>(
    name = "DisplayReceiver",
    description = "Updates display with timer values"
) { timerOutput ->
    displayedSeconds.value = timerOutput.elapsedSeconds
    displayedMinutes.value = timerOutput.elapsedMinutes
}

// Wire up the channel
displayRuntime.inputChannel = timerRuntime.outputChannel

// Start both
timerRuntime.start(scope) { /* generator loop */ }
displayRuntime.start(scope) { /* sink loop */ }
```

## Step 3: Create a Continuous Transformer

```kotlin
// Transform TimerOutput to formatted string
val formatterRuntime = CodeNodeFactory.createContinuousTransformer<TimerOutput, String>(
    name = "TimeFormatter",
    description = "Formats time as MM:SS"
) { timerOutput ->
    String.format("%02d:%02d", timerOutput.elapsedMinutes, timerOutput.elapsedSeconds)
}
```

## Step 4: Create a Continuous Filter

```kotlin
// Only pass through values where seconds is a multiple of 10
val decimatorRuntime = CodeNodeFactory.createContinuousFilter<TimerOutput>(
    name = "TenSecondFilter",
    description = "Passes values every 10 seconds"
) { timerOutput ->
    timerOutput.elapsedSeconds % 10 == 0
}
```

## Step 5: Wire Nodes Together

```kotlin
// Create a channel to connect generator to sink
val channel = Channel<TimerOutput>(Channel.BUFFERED)

// Wire up
timerRuntime.outputChannel = channel
displayRuntime.inputChannel = channel

// Start in order (sink first to be ready)
displayRuntime.start(scope) { /* sink runs */ }
timerRuntime.start(scope) { /* generator runs */ }

// Later, stop in reverse order
timerRuntime.stop()
displayRuntime.stop()
channel.close()
```

## Step 6: Complete StopWatch Example

```kotlin
suspend fun runStopWatch(scope: CoroutineScope) {
    // Create state holders
    val displayedSeconds = MutableStateFlow(0)
    val displayedMinutes = MutableStateFlow(0)

    // Create channel
    val timerChannel = Channel<TimerOutput>(Channel.BUFFERED)

    // Create generator
    val timer = CodeNodeFactory.createContinuousGenerator<TimerOutput>(
        name = "Timer"
    ) { emit ->
        var seconds = 0
        var minutes = 0

        while (isActive) {
            delay(1000)
            seconds++
            if (seconds >= 60) {
                seconds = 0
                minutes++
            }
            emit(TimerOutput(seconds, minutes))
        }
    }
    timer.outputChannel = timerChannel

    // Create sink
    val display = CodeNodeFactory.createContinuousSink<TimerOutput>(
        name = "Display"
    ) { output ->
        displayedSeconds.value = output.elapsedSeconds
        displayedMinutes.value = output.elapsedMinutes
    }
    display.inputChannel = timerChannel

    // Start (sink first)
    display.codeNode = display.codeNode?.withExecutionState(ExecutionState.RUNNING)
    display.start(scope) { /* managed internally */ }

    timer.codeNode = timer.codeNode?.withExecutionState(ExecutionState.RUNNING)
    timer.start(scope) { /* managed internally */ }

    // Observe state
    displayedSeconds.collect { seconds ->
        println("Elapsed: $seconds seconds")
    }
}
```

## Unit Testing with Virtual Time

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ContinuousGeneratorTest {

    @Test
    fun `generator emits values at correct intervals`() = runTest {
        val emissions = mutableListOf<Int>()
        val channel = Channel<Int>(Channel.BUFFERED)

        val generator = CodeNodeFactory.createContinuousGenerator<Int>(
            name = "Counter"
        ) { emit ->
            var count = 0
            while (isActive) {
                delay(100)
                emit(++count)
            }
        }
        generator.outputChannel = channel

        // Collect emissions
        val collectJob = launch {
            channel.consumeEach { emissions.add(it) }
        }

        // Start generator
        generator.start(this) { /* loop runs */ }

        // Advance virtual time
        advanceTimeBy(350)

        // Verify emissions
        assertEquals(listOf(1, 2, 3), emissions)

        // Cleanup
        generator.stop()
        channel.close()
        collectJob.cancel()
    }
}
```

## Migration Guide

### From Single-Invocation to Continuous

**Before** (single invocation):
```kotlin
val node = CodeNodeFactory.createGenerator<MyOutput>(
    name = "MyGen",
    generate = { MyOutput(value) }  // Called once
)
```

**After** (continuous):
```kotlin
val wiring = CodeNodeFactory.createContinuousGenerator<MyOutput>(
    name = "MyGen"
) { emit ->
    while (isActive) {
        delay(interval)
        emit(MyOutput(value))  // Called repeatedly
    }
}
```

### Key Differences

| Aspect | Single-Invocation | Continuous |
|--------|-------------------|------------|
| Return type | `CodeNode` | `NodeRuntime<T>` |
| Execution | One call per trigger | Loop until stopped |
| Communication | `InformationPacket` | `Channel<T>` |
| Lifecycle | Manual | Managed via `start()`/`stop()` |
| Testing | Call `process()` | Use `runTest` + `advanceTimeBy` |

## Verification Checklist

- [ ] `createContinuousGenerator<T>` creates NodeRuntime with output channel
- [ ] `createContinuousSink<T>` creates NodeRuntime with input channel
- [ ] `createContinuousTransformer<TIn, TOut>` creates NodeRuntime with both channels
- [ ] `createContinuousFilter<T>` creates NodeRuntime with both channels
- [ ] Generator loop respects `isActive` check
- [ ] Sink handles `ClosedReceiveChannelException` gracefully
- [ ] Channels close when node stops
- [ ] Virtual time testing works with `runTest`
- [ ] Existing single-invocation methods still work (deprecated)
- [ ] All existing tests pass

## Common Issues

### Issue: Generator doesn't emit

**Cause**: Missing `isActive` check causing immediate exit.

**Solution**: Use `while (isActive)` pattern:
```kotlin
{ emit ->
    while (isActive) {  // Not: while (true)
        delay(interval)
        emit(value)
    }
}
```

### Issue: Sink misses values

**Cause**: Sink started after generator, channel buffer overflows.

**Solution**: Start sink before generator:
```kotlin
sink.start(scope) { ... }     // First
generator.start(scope) { ... } // Second
```

### Issue: Memory leak / orphaned coroutines

**Cause**: Not calling `stop()` on nodes.

**Solution**: Use structured concurrency or try-finally:
```kotlin
try {
    generator.start(scope) { ... }
    // ...
} finally {
    generator.stop()
    channel.close()
}
```

### Issue: Channel not closing on stop

**Cause**: Output channel not closed in finally block.

**Solution**: Factory methods handle this internally, but for custom code:
```kotlin
try {
    while (isActive) {
        // ...
    }
} finally {
    outputChannel?.close()
}
```
