# Research: StopWatch Virtual Circuit Demo

**Feature**: 008-stopwatch-virtual-circuit
**Date**: 2026-02-08

## Overview

This document consolidates research findings for implementing the StopWatch virtual circuit demo. All NEEDS CLARIFICATION items from technical context have been resolved.

---

## Research Topics

### 1. FlowGraph Execution Model

**Question**: How should the FBP nodes communicate and execute in the context of a Compose-based UI?

**Decision**: Use coroutine-based execution with channels for node communication.

**Findings**:
- The existing fbpDsl module uses `kotlinx.coroutines.Channel` for inter-node communication
- CodeNode.ProcessingLogic interface defines suspend functions for async processing
- RootControlNode provides unified execution control with startAll/stopAll/pauseAll
- Compose's LaunchedEffect and collectAsState work seamlessly with coroutines

**Code Pattern**:
```kotlin
// TimerEmitter emits to channels
val outputChannel = Channel<Int>(Channel.BUFFERED)
launch {
    while (executionState == ExecutionState.RUNNING) {
        delay(controlConfig.speedAttenuation)
        outputChannel.send(elapsedSeconds)
    }
}

// DisplayReceiver collects from channels
launch {
    outputChannel.consumeEach { seconds ->
        // Update UI state
    }
}
```

**Rationale**: Coroutines are already the execution backbone of the project. Using channels maintains consistency with existing patterns and enables natural integration with Compose's reactive model.

---

### 2. Timer Implementation Strategy

**Question**: How should the timer tick mechanism work within the FBP execution model?

**Decision**: TimerEmitter uses a coroutine-based tick loop controlled by RootControlNode state.

**Findings**:
- Original StopWatch uses `LaunchedEffect(isRunning)` with `delay(1000)`
- controlConfig.speedAttenuation maps directly to delay duration
- ExecutionState.RUNNING controls whether the loop emits ticks
- No separate isRunning variable needed—state comes from RootControlNode

**Mapping**:
| Original | Virtual Circuit |
|----------|-----------------|
| `isRunning` boolean | `executionState == ExecutionState.RUNNING` |
| `delay(1000)` | `delay(controlConfig.speedAttenuation)` where speedAttenuation=1000 |
| `elapsedSeconds += 1` | Emit to elapsedSeconds output port |
| `elapsedMinutes += 1` | Emit to elapsedMinutes output port |

**Rollover Logic**:
```kotlin
if (elapsedSeconds >= 60) {
    elapsedSeconds = 0
    elapsedMinutes += 1
}
```
This logic remains in TimerEmitter UseCase, emitting both values after each tick.

---

### 3. Module Integration Approach

**Question**: How should the generated StopWatch module integrate with KMPMobileApp?

**Decision**: Include as a local project dependency via settings.gradle.kts.

**Findings**:
- KMPMobileApp already includes fbpDsl (androidMain only) as project dependency
- settings.gradle.kts pattern: `include(":moduleName")`
- build.gradle.kts pattern: `implementation(project(":moduleName"))`
- Generated module must have matching Kotlin/Compose versions

**Integration Steps**:
1. Generate StopWatch module to `StopWatch/` directory
2. Add to root settings.gradle.kts: `include(":StopWatch")`
3. Add to KMPMobileApp/build.gradle.kts commonMain: `implementation(project(":StopWatch"))`
4. Import in StopWatch.kt: `import io.codenode.generated.stopwatch.*`

**Gradle Configuration Alignment**:
- Kotlin version: Must match root project (2.1.21)
- Compose version: Must match KMPMobileApp (1.7.3)
- Android SDK: compileSdk 34, minSdk 24

---

### 4. StopWatchFace Extraction

**Question**: How should the private StopWatchFace composable be made accessible?

**Decision**: Extract to separate file with internal visibility.

**Findings**:
- Currently defined as `private fun StopWatchFace(...)` inside StopWatch.kt
- Needed by DisplayReceiver UseCase in generated module
- Options: public (too exposed), internal (module-visible), or composable lambda parameter

**Implementation**:
1. Create new file: `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatchFace.kt`
2. Move StopWatchFace composable and helper functions (secondsToRad, textMeasurer)
3. Mark as `internal fun StopWatchFace(...)`
4. DisplayReceiver receives seconds/minutes and invokes StopWatchFace internally

**Note**: Since DisplayReceiver is in a separate module, we may need to use public visibility or pass a composable lambda. Final decision during implementation.

---

### 5. State Mapping Pattern

**Question**: How should ExecutionState map to the original isRunning boolean?

**Decision**: Simple boolean derivation: `isRunning = executionState == ExecutionState.RUNNING`

**State Mapping Table**:
| ExecutionState | isRunning | StopWatch Behavior |
|----------------|-----------|-------------------|
| IDLE | false | Timer stopped, can start |
| RUNNING | true | Timer ticking, can stop |
| PAUSED | false | Timer paused (treat as stopped for demo) |
| ERROR | false | Error occurred (treat as stopped for demo) |

**Reset Behavior**:
- Reset button calls `controller.stop()` AND resets elapsed values
- elapsedSeconds and elapsedMinutes managed as state in the composable
- These are "accumulated state" separate from execution state

---

### 6. Port Type Configuration

**Question**: What data types should the ports use for elapsedSeconds and elapsedMinutes?

**Decision**: Use Int type for both ports.

**Findings**:
- Port class supports generic types via `dataType: KClass<T>`
- Int is sufficient for seconds (0-59) and minutes (0-MAX_INT)
- Type compatibility checking via `Port.isCompatibleWith()`

**Port Definitions**:
```kotlin
// TimerEmitter outputs
OutputPort(id = "elapsedSeconds", name = "Elapsed Seconds", dataType = Int::class)
OutputPort(id = "elapsedMinutes", name = "Elapsed Minutes", dataType = Int::class)

// DisplayReceiver inputs
InputPort(id = "seconds", name = "Seconds", dataType = Int::class)
InputPort(id = "minutes", name = "Minutes", dataType = Int::class)
```

---

### 7. ExecutionState vs Android Lifecycle

**Question**: Should we replace ExecutionState with Android/KMP Lifecycle (androidx.lifecycle)?

**Decision**: Keep ExecutionState in fbpDsl; add optional lifecycle binding utility in StopWatchController.

**Analysis**:

| Aspect | ExecutionState (fbpDsl) | Lifecycle (androidx) |
|--------|------------------------|---------------------|
| **States** | IDLE, RUNNING, PAUSED, ERROR | INITIALIZED, CREATED, STARTED, RESUMED, DESTROYED |
| **Purpose** | FBP node processing control | UI component visibility/activity |
| **Control** | Explicit (you call start/stop) | System-driven (OS triggers events) |
| **Error handling** | ERROR state built-in | No error state |

**Reasons to Keep ExecutionState**:
1. **ERROR state has no Lifecycle equivalent** - FBP nodes need to signal processing errors
2. **Semantic mismatch** - "RUNNING" means "actively processing data packets"; "RESUMED" means "UI is in foreground"
3. **Control direction is inverted** - In FBP, *you* decide when nodes process; in Lifecycle, the *system* decides
4. **fbpDsl should remain UI-agnostic** - Enables headless/CLI apps, server-side, non-Compose targets
5. **ControlConfig integration** - speedAttenuation, pauseBufferSize, independentControl are FBP-specific

**Solution**: Add optional `bindToLifecycle()` utility in StopWatchController:

```kotlin
// In StopWatchController.kt (generated)
private var wasRunningBeforePause = false

fun bindToLifecycle(lifecycle: Lifecycle) {
    lifecycle.addObserver(LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_PAUSE -> {
                wasRunningBeforePause = isRunning
                if (isRunning) pause()
            }
            Lifecycle.Event.ON_RESUME -> {
                if (wasRunningBeforePause) start()
            }
            Lifecycle.Event.ON_DESTROY -> stop()
            else -> {}
        }
    })
}
```

**Usage in Composable**:
```kotlin
val lifecycleOwner = LocalLifecycleOwner.current
LaunchedEffect(controller, lifecycleOwner) {
    controller.bindToLifecycle(lifecycleOwner.lifecycle)
}
```

**Rationale**: This gives users the choice to opt-in to lifecycle binding without forcing the coupling. The fbpDsl module stays pure and portable while generated controllers can integrate with platform lifecycles when desired.

**Dependency**: Requires `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.9.6` in generated module.

---

## Best Practices Applied

### Kotlin Multiplatform
- Use expect/actual for platform-specific implementations
- Prefer commonMain for shared logic
- Test on both Android and iOS early

### Compose Desktop/Mobile
- Compose state hoisting for reusability
- Use rememberCoroutineScope for side effects
- Avoid platform-specific APIs in commonMain

### FBP Design
- Single responsibility per node
- Typed connections prevent runtime errors
- RootControlNode provides unified control interface

---

## Unresolved Questions

None. All technical decisions have been made and documented.

---

## References

- [fbpDsl Models](../../fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/)
- [ModuleGenerator](../../kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ModuleGenerator.kt)
- [Existing StopWatch](../../KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatch.kt)
- [RootControlNode](../../fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/RootControlNode.kt)
