# Quickstart: Reactive Feedback Loop for Source Nodes

**Feature Branch**: `038-reactive-source-loop`
**Date**: 2026-03-05

## Overview

This feature adds a reactive feedback loop to flow graphs: processor runtimes support a configurable attenuation delay between receive and process, source nodes reactively observe StateFlow changes and re-emit, and the controller primes source output channels on start. The result is a self-perpetuating pipeline (e.g., StopWatch ticking at 1-second intervals) that runs until paused or stopped.

## Key Concepts

### Attenuation Delay
- A `Long?` property on processor runtimes (`attenuationDelayMs`)
- When non-null and > 0, inserts a `delay()` between receiving input and processing
- Defaults to `null` — zero overhead, full backward compatibility
- Set via the controller's `setAttenuationDelay(delayMs)` method

### Reactive Source Observation
- Source nodes observe their corresponding `MutableStateFlow` properties
- Uses `combine(flow1, flow2) { ... }.drop(1).collect { emit(it) }` pattern
- `drop(1)` skips the initial combine emission (controller primes instead)
- Re-emits automatically whenever observed state changes

### Controller Priming
- On `start()`, the controller sends initial state values to source output channels
- This "primes the pump" — triggers the first processing cycle
- After priming, the feedback loop sustains itself reactively

## Build & Test

```bash
# Run runtime tests (attenuationDelay behavior)
./gradlew :fbpDsl:jvmTest

# Run generator tests (reactive source blocks, priming, attenuation propagation)
./gradlew :kotlinCompiler:jvmTest

# Run all affected module tests
./gradlew :fbpDsl:jvmTest :kotlinCompiler:jvmTest :graphEditor:jvmTest

# Full build
./gradlew build
```

## Development Sequence

1. **Add attenuationDelayMs to processor runtimes** (16 runtime files in fbpDsl)
2. **Generate reactive source blocks** in `RuntimeFlowGenerator` (replace `awaitCancellation()`)
3. **Generate controller priming + propagation** in `RuntimeControllerGenerator`
4. **Update StopWatch processing logic** to use input values (not read state directly)
5. **Regenerate StopWatch module** and verify end-to-end feedback loop

## Integration Test Scenarios

### Scenario 1: Processor Delay
```kotlin
// Create processor with 1000ms delay
val processor = CodeNodeFactory.createIn2Out1Processor<Int, Int, Int>(
    name = "Test",
    process = { a, b -> a + b }
)
processor.attenuationDelayMs = 1000L

// Send input, verify output arrives after ~1000ms (virtual time)
inputChannel1.send(5)
inputChannel2.send(10)
advanceTimeBy(999)
// No output yet
advanceTimeBy(2)
runCurrent()
// Output: 15
```

### Scenario 2: Reactive Source Re-emission
```kotlin
// Source observes state flows
// When state changes externally:
state._value.value = 42
// Source automatically re-emits 42 to output channel
```

### Scenario 3: Controller Priming
```kotlin
// State starts at defaults (0)
controller.start(scope)
// Source output channels receive initial values (0) without any external trigger
```

### Scenario 4: End-to-End StopWatch
```kotlin
controller.setAttenuationDelay(1000L)
controller.start(scope)
// t=0: Controller primes source → emits (0, 0)
// t=0: Processor receives (0, 0), starts 1000ms delay
// t=1000: Processor processes → (1, 0), updates state
// t=1000: Source observes state change → re-emits (1, 0)
// t=1000: Processor receives (1, 0), starts 1000ms delay
// t=2000: Processor processes → (2, 0), updates state
// ... continues at 1-second intervals
```
