# Quickstart: Refactor TimerEmitterComponent

**Feature**: 020-refactor-timer-emitter
**Date**: 2026-02-18

## Overview

This refactoring separates common CodeNode runtime lifecycle concerns from unique component business logic. The key change is adding a "timed tick" mode to `Out2GeneratorRuntime` so that components like TimerEmitterComponent don't need to implement their own execution loops with pause/resume/stop handling.

## Before & After

### Before: TimerEmitterComponent (current)

The component implements its own execution loop with manual pause/resume/stop handling:

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

    // Business logic mixed with lifecycle management
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

    // ~70 lines of property delegation, lifecycle forwarding...
    var executionState: ExecutionState
        get() = generatorRuntime.executionState
        set(value) { generatorRuntime.executionState = value }
    // ... etc
}
```

### After: TimerEmitterComponent (refactored)

The component provides only its unique business logic. The runtime handles the execution loop:

```kotlin
class TimerEmitterComponent(
    speedAttenuation: Long = 1000L,
    initialSeconds: Int = 0,
    initialMinutes: Int = 0
) : ProcessingLogic {

    private val _elapsedSeconds = MutableStateFlow(initialSeconds)
    val elapsedSecondsFlow: StateFlow<Int> = _elapsedSeconds.asStateFlow()
    private val _elapsedMinutes = MutableStateFlow(initialMinutes)
    val elapsedMinutesFlow: StateFlow<Int> = _elapsedMinutes.asStateFlow()

    // Pure business logic - no lifecycle concerns
    fun incrementer(oldSeconds: Int, oldMinutes: Int): ProcessResult2<Int, Int> {
        var newSeconds = oldSeconds + 1
        var newMinutes = oldMinutes
        if (newSeconds >= 60) {
            newSeconds = 0
            newMinutes += 1
        }
        return ProcessResult2.both(newSeconds, newMinutes)
    }

    // Tick function - called once per interval by the runtime
    private val tick: Out2TickBlock<Int, Int> = {
        val result = incrementer(_elapsedSeconds.value, _elapsedMinutes.value)
        _elapsedSeconds.value = result.out1!!
        _elapsedMinutes.value = result.out2!!
        result
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

### Runtime Behavior (Internal to Out2GeneratorRuntime)

When started in timed tick mode, the runtime runs:

```kotlin
// Internal loop in Out2GeneratorRuntime.start()
while (currentCoroutineContext().isActive && executionState != ExecutionState.IDLE) {
    while (executionState == ExecutionState.PAUSED) { delay(10) }
    if (executionState == ExecutionState.IDLE) break
    delay(tickIntervalMs)
    if (executionState == ExecutionState.IDLE) break
    if (executionState == ExecutionState.PAUSED) continue
    val result = tickBlock()
    result.out1?.let { out1.send(it) }
    result.out2?.let { out2.send(it) }
}
```

## Files Changed

| File | Change Type | Description |
| ---- | ----------- | ----------- |
| `fbpDsl/.../runtime/ContinuousTypes.kt` | Add | New `Out2TickBlock` type alias |
| `fbpDsl/.../runtime/Out2GeneratorRuntime.kt` | Modify | Add timed tick mode to `start()` |
| `fbpDsl/.../model/CodeNodeFactory.kt` | Add | New `createTimedOut2Generator` factory method |
| `StopWatch/.../usecases/TimerEmitterComponent.kt` | Refactor | Replace generator block with tick function |
| `StopWatch/.../usecases/DisplayReceiverComponent.kt` | Clean up | Remove commented-out code |
| `StopWatch/...Test/...` | Update | Adjust test setup for new API |
