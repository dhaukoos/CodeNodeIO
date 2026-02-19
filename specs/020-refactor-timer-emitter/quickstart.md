# Quickstart: Refactor TimerEmitterComponent

**Feature**: 020-refactor-timer-emitter
**Date**: 2026-02-18

## Overview

This refactoring separates common CodeNode runtime lifecycle concerns from unique component business logic. The key change is adding a "timed tick" mode to `Out2GeneratorRuntime` so that components like TimerEmitterComponent don't need to implement their own execution loops with pause/resume/stop handling.

## Before & After

### Before: TimerEmitterComponent (pre-refactoring)

The component implemented its own execution loop with manual pause/resume/stop handling:

```kotlin
class TimerEmitterComponent(
    private val speedAttenuation: Long = 1000L,
    initialSeconds: Int = 0,
    initialMinutes: Int = 0
) : ProcessingLogic {

    private val _elapsedSeconds = MutableStateFlow(initialSeconds)
    val elapsedSecondsFlow: StateFlow<Int> = _elapsedSeconds.asStateFlow()
    private val _elapsedMinutes = MutableStateFlow(initialMinutes)
    val elapsedMinutesFlow: StateFlow<Int> = _elapsedMinutes.asStateFlow()

    // Business logic mixed with lifecycle management (~27 lines)
    val generator: Out2GeneratorBlock<Int, Int> = { emit ->
        while (currentCoroutineContext().isActive && executionState != ExecutionState.IDLE) {
            while (executionState == ExecutionState.PAUSED) { delay(10) }
            if (executionState == ExecutionState.IDLE) break
            if (speedAttenuation > 0) {
                delay(speedAttenuation)
                if (executionState == ExecutionState.IDLE) break
                if (executionState == ExecutionState.PAUSED) continue
            }
            val result = incrementer(_elapsedSeconds.value, _elapsedMinutes.value)
            _elapsedSeconds.value = result.out1!!
            _elapsedMinutes.value = result.out2!!
            emit(result)
        }
    }

    private val generatorRuntime = CodeNodeFactory.createOut2Generator(
        name = "TimerEmitter",
        generate = generator
    )

    // ~70 lines of property delegation, lifecycle forwarding...
}
```

### After: TimerEmitterComponent (refactored)

The component provides only its unique business logic (14 lines). The runtime handles the execution loop:

```kotlin
class TimerEmitterComponent(
    private val speedAttenuation: Long = 1000L,
    initialSeconds: Int = 0,
    initialMinutes: Int = 0
) : ProcessingLogic {

    private val _elapsedSeconds = MutableStateFlow(initialSeconds)
    val elapsedSecondsFlow: StateFlow<Int> = _elapsedSeconds.asStateFlow()
    private val _elapsedMinutes = MutableStateFlow(initialMinutes)
    val elapsedMinutesFlow: StateFlow<Int> = _elapsedMinutes.asStateFlow()

    // Pure business logic - no lifecycle concerns
    private val tick: Out2TickBlock<Int, Int> = {
        var newSeconds = _elapsedSeconds.value + 1
        var newMinutes = _elapsedMinutes.value
        if (newSeconds >= 60) {
            newSeconds = 0
            newMinutes += 1
        }
        _elapsedSeconds.value = newSeconds
        _elapsedMinutes.value = newMinutes
        ProcessResult2.both(newSeconds, newMinutes)
    }

    // Runtime handles loop, pause, resume, channels, registry
    private val generatorRuntime = CodeNodeFactory.createTimedOut2Generator<Int, Int>(
        name = "TimerEmitter",
        tickIntervalMs = speedAttenuation,
        description = "Emits elapsed time at regular intervals",
        tick = tick
    )

    // Thin delegation layer
    val codeNode: CodeNode get() = generatorRuntime.codeNode
    val outputChannel1 get() = generatorRuntime.outputChannel1
    val outputChannel2 get() = generatorRuntime.outputChannel2
    var executionState: ExecutionState
        get() = generatorRuntime.executionState
        set(value) { generatorRuntime.executionState = value }
    var registry: RuntimeRegistry?
        get() = generatorRuntime.registry
        set(value) { generatorRuntime.registry = value }
    suspend fun start(scope: CoroutineScope) = generatorRuntime.start(scope) {}
    fun stop() = generatorRuntime.stop()
    fun reset() { stop(); _elapsedSeconds.value = 0; _elapsedMinutes.value = 0 }
}
```

## New Runtime API

### Out2TickBlock Type Alias

```kotlin
// In ContinuousTypes.kt
typealias Out2TickBlock<U, V> = suspend () -> ProcessResult2<U, V>
```

### Factory Method

```kotlin
// In CodeNodeFactory.kt
inline fun <reified U : Any, reified V : Any> createTimedOut2Generator(
    name: String,
    tickIntervalMs: Long,
    channelCapacity: Int = Channel.BUFFERED,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline tick: Out2TickBlock<U, V>
): Out2GeneratorRuntime<U, V>
```

### Runtime Behavior

The factory wraps the tick function in a timed generate block and delegates to `createOut2Generator`:

```kotlin
// Inside createTimedOut2Generator factory
val timedGenerate: Out2GeneratorBlock<U, V> = { emit ->
    while (currentCoroutineContext().isActive) {
        delay(tickIntervalMs)
        emit(tick())
    }
}
return createOut2Generator(name, generate = timedGenerate, ...)
```

The runtime's `start()` method provides the emit lambda with built-in pause checking:

```kotlin
// Inside Out2GeneratorRuntime.start()
val emit: suspend (ProcessResult2<U, V>) -> Unit = { result ->
    while (executionState == ExecutionState.PAUSED) { delay(10) }
    if (executionState == ExecutionState.RUNNING) {
        result.out1?.let { out1.send(it) }
        result.out2?.let { out2.send(it) }
    }
}
gen(emit)  // Runs the timed generate block
```

### Virtual Time Testing Note

Due to a KMP limitation, `delay()` in lambdas compiled from `commonMain` does not correctly interact with `StandardTestDispatcher`'s virtual time. For testing timed generators with virtual time:

```kotlin
// Use createOut2Generator with a test-defined loop instead of createTimedOut2Generator
val generator = CodeNodeFactory.createOut2Generator<Int, Int>(
    name = "TestTicker",
    generate = { emit ->  // Lambda at test call site - delay works with virtual time
        while (currentCoroutineContext().isActive) {
            delay(100)
            counter++
            emit(ProcessResult2.both(counter, counter * 10))
        }
    }
)
```

## Files Changed

| File | Change Type | Description |
| ---- | ----------- | ----------- |
| `fbpDsl/.../runtime/ContinuousTypes.kt` | Add | New `Out2TickBlock` type alias |
| `fbpDsl/.../runtime/Out2GeneratorRuntime.kt` | Modify | Make `generate` nullable for factory delegation |
| `fbpDsl/.../model/CodeNodeFactory.kt` | Add | New `createTimedOut2Generator` factory method |
| `fbpDsl/.../runtime/TimedGeneratorTest.kt` | Add | 7 tests for timed generator behavior |
| `StopWatch/.../usecases/TimerEmitterComponent.kt` | Refactor | Replace generator block with tick function (14 lines business logic) |
| `StopWatch/.../usecases/DisplayReceiverComponent.kt` | Clean up | Remove commented-out code, unused param/imports (6 lines business logic) |
| `StopWatch/.../usecases/TimerEmitterComponentTest.kt` | Update | Remove manual executionState setup, adjust pause assertion |
| `StopWatch/.../ChannelIntegrationTest.kt` | Update | Remove manual executionState setup |
