# Data Model: Reactive Feedback Loop for Source Nodes

**Feature Branch**: `038-reactive-source-loop`
**Date**: 2026-03-05

## Entity Changes

This feature is a behavioral change to existing runtime classes and code generators. No new entities, database tables, or serialization schemas are introduced.

### Key Entities (Behavioral, Not Structural)

**Source Node** — No structural change. Behavioral change: the generate block now observes `StateFlow` properties via `combine` + `drop(1)` + `collect` instead of using `awaitCancellation()`. Still has 0 inputs, 1-3 outputs.

**Processor Node** (Transformer, Filter, InXOutY, InXAnyOutY) — Adds one nullable property:
```kotlin
// Added to NodeRuntime base class (or each processor subclass)
var attenuationDelayMs: Long? = null
```
When non-null and > 0, a `delay(attenuationDelayMs)` call is inserted between receive and process in the runtime loop. Defaults to `null` (no delay, preserving backward compatibility).

**Sink Node** — No change. Sinks are terminal and do not participate in the feedback loop delay.

**Observable State** — No structural change. Existing `MutableStateFlow` properties in the generated `{FlowName}State` object are observed by source nodes and updated by processor/sink nodes. No new properties added.

**Controller** — Behavioral change only:
1. `start()` method now primes source node output channels with initial state values after starting the flow.
2. `setAttenuationDelay()` propagates to ALL runtime nodes (not just generators/sources).

### Runtime Property Addition

The only new property added to the codebase:

| Property | Type | Default | Location |
|----------|------|---------|----------|
| `attenuationDelayMs` | `Long?` | `null` | `NodeRuntime` (base class) or each processor runtime |

This property is runtime-only — it is NOT serialized, NOT persisted, and NOT part of the `CodeNode` model. It is set programmatically by the controller's `setAttenuationDelay()` method.

## Serialization Impact

None. No changes to:
- `.flow.kts` DSL files
- `CodeNode` model serialization
- `FlowGraph` serialization
- Room database entities
- JSON persistence

## Generated Code Pattern Changes

### Source Node Generate Block

**Before (037):**
```kotlin
val timerEmitter = CodeNodeFactory.createContinuousSource<Int>(
    name = "TimerEmitter",
    generate = { emit ->
        awaitCancellation()
    }
)
```

**After (038):**
```kotlin
val timerEmitter = CodeNodeFactory.createSourceOut2<Int, Int>(
    name = "TimerEmitter",
    generate = { emit ->
        combine(
            StopWatchState._elapsedSeconds,
            StopWatchState._elapsedMinutes
        ) { elapsedSeconds, elapsedMinutes ->
            ProcessResult2.both(elapsedSeconds, elapsedMinutes)
        }.drop(1).collect { result ->
            emit(result)
        }
    }
)
```

### Controller start() Method

**Before (037):**
```kotlin
fun start(scope: CoroutineScope) {
    flow.start(scope)
}
```

**After (038):**
```kotlin
fun start(scope: CoroutineScope) {
    flow.start(scope)
    scope.launch {
        flow.timerEmitter.outputChannel1?.send(StopWatchState._elapsedSeconds.value)
        flow.timerEmitter.outputChannel2?.send(StopWatchState._elapsedMinutes.value)
    }
}
```

### Controller setAttenuationDelay()

**Before (037):**
```kotlin
fun setAttenuationDelay(delayMs: Long) {
    flow.timerEmitter.attenuationDelayMs = delayMs
}
```

**After (038):**
```kotlin
fun setAttenuationDelay(delayMs: Long) {
    flow.timerEmitter.attenuationDelayMs = delayMs
    flow.timeIncrementer.attenuationDelayMs = delayMs
    flow.displayReceiver.attenuationDelayMs = delayMs
}
```
