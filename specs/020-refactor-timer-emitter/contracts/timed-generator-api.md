# Internal API Contract: Timed Generator

**Feature**: 020-refactor-timer-emitter
**Date**: 2026-02-18

## New Type Alias

```kotlin
// ContinuousTypes.kt
typealias Out2TickBlock<U, V> = suspend () -> ProcessResult2<U, V>
```

## New Factory Method

```kotlin
// CodeNodeFactory.kt
inline fun <reified U : Any, reified V : Any> createTimedOut2Generator(
    name: String,
    tickIntervalMs: Long,
    channelCapacity: Int = Channel.BUFFERED,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline tick: Out2TickBlock<U, V>
): Out2GeneratorRuntime<U, V>
```

### Parameters

| Parameter | Type | Default | Description |
| --------- | ---- | ------- | ----------- |
| name | String | required | Display name for the CodeNode |
| tickIntervalMs | Long | required | Milliseconds between tick invocations |
| channelCapacity | Int | BUFFERED (64) | Output channel buffer size |
| position | Node.Position | ORIGIN | Visual position in graph editor |
| description | String? | null | Optional description |
| tick | Out2TickBlock<U, V> | required | Function called once per interval, returns values to emit |

### Behavior Contract

1. The runtime calls `tick()` once every `tickIntervalMs` milliseconds
2. The delay occurs BEFORE the first tick (same as current TimerEmitterComponent behavior)
3. When paused, the runtime suspends the loop (no ticks occur during pause)
4. When resumed, ticking resumes at the configured interval
5. When stopped, the loop exits and channels are closed
6. The tick function has no access to execution state - it is pure business logic
7. If `tick()` throws an exception, the runtime catches it and the loop exits (same as ClosedSendChannelException handling)

## Modified Runtime: Out2GeneratorRuntime

### Constructor Change

```kotlin
class Out2GeneratorRuntime<U : Any, V : Any>(
    codeNode: CodeNode,
    private val channelCapacity: Int = Channel.BUFFERED,
    private val generate: Out2GeneratorBlock<U, V>? = null,     // existing, now optional
    private val tickIntervalMs: Long = 0,                        // new
    private val tickBlock: Out2TickBlock<U, V>? = null           // new
) : NodeRuntime<U>(codeNode)
```

### Invariant

Exactly one of `generate` or `tickBlock` must be non-null. If both are null or both are non-null, behavior is undefined (factory methods enforce this).
