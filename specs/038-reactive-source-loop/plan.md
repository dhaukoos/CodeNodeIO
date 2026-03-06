# Implementation Plan: Reactive Feedback Loop for Source Nodes

**Branch**: `038-reactive-source-loop` | **Date**: 2026-03-05 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/038-reactive-source-loop/spec.md`

## Summary

Source nodes currently use `awaitCancellation()` and never emit, leaving the pipeline inert. This feature adds a reactive feedback loop: the controller primes source output channels on start, processor runtimes apply an attenuation delay between receive and process, and source nodes reactively observe their StateFlow properties and re-emit when state changes. The result is a self-perpetuating pipeline (e.g., StopWatch ticking at 1-second intervals) that runs until paused or stopped.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (Kotlin Multiplatform)
**Primary Dependencies**: kotlinx-coroutines 1.8.0 (channels, StateFlow, combine, delay), kotlinx-serialization 1.6.0
**Storage**: N/A (in-memory models, generated source code files)
**Testing**: kotlin.test + kotlinx-coroutines-test (runTest, advanceTimeBy, runCurrent)
**Target Platform**: KMP (Android, iOS, Desktop)
**Project Type**: Multiplatform library (fbpDsl) + code generator (kotlinCompiler) + application module (StopWatch)
**Performance Goals**: 1-second feedback cycle rate for StopWatch; sub-10ms overhead for delay mechanism
**Constraints**: attenuationDelayMs must default to null for backward compatibility; existing tests must pass unchanged
**Scale/Scope**: ~16 runtime files + 2 generator files + 1 application file + associated tests

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| Licensing & IP | PASS | No new dependencies; all existing deps are Apache 2.0 / MIT |
| I. Code Quality | PASS | Changes follow existing patterns; 2-line delay insertion is uniform across runtimes |
| II. Test-Driven Development | PASS | Tests added for attenuationDelay behavior; generator tests updated for new patterns |
| III. UX Consistency | N/A | No user-facing UI changes |
| IV. Performance | PASS | attenuationDelayMs defaults to null (zero overhead when unused); existing behavior preserved |
| V. Observability | N/A | Internal framework change; no logging/metrics changes needed |

**Quality Gates**: All automated tests must pass. No new dependencies introduced.

## Project Structure

### Documentation (this feature)

```text
specs/038-reactive-source-loop/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/
├── TransformerRuntime.kt          # Add attenuationDelay (Phase 1)
├── FilterRuntime.kt               # Add attenuationDelay (Phase 1)
├── In1Out2Runtime.kt              # Add attenuationDelay (Phase 1)
├── In1Out3Runtime.kt              # Add attenuationDelay (Phase 1)
├── In2Out1Runtime.kt              # Add attenuationDelay (Phase 1)
├── In2Out2Runtime.kt              # Add attenuationDelay (Phase 1)
├── In2Out3Runtime.kt              # Add attenuationDelay (Phase 1)
├── In3Out1Runtime.kt              # Add attenuationDelay (Phase 1)
├── In3Out2Runtime.kt              # Add attenuationDelay (Phase 1)
├── In3Out3Runtime.kt              # Add attenuationDelay (Phase 1)
├── In2AnyOut1Runtime.kt           # Add attenuationDelay in select branches (Phase 1)
├── In2AnyOut2Runtime.kt           # Add attenuationDelay in select branches (Phase 1)
├── In2AnyOut3Runtime.kt           # Add attenuationDelay in select branches (Phase 1)
├── In3AnyOut1Runtime.kt           # Add attenuationDelay in select branches (Phase 1)
├── In3AnyOut2Runtime.kt           # Add attenuationDelay in select branches (Phase 1)
└── In3AnyOut3Runtime.kt           # Add attenuationDelay in select branches (Phase 1)

fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/
└── TypedNodeRuntimeTest.kt        # Add attenuationDelay tests (Phase 1)

kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/
├── RuntimeFlowGenerator.kt        # Reactive source generate blocks (Phase 2)
└── RuntimeControllerGenerator.kt  # Priming + attenuation propagation (Phase 3)

kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/
├── RuntimeFlowGeneratorTest.kt    # Update source block assertions (Phase 2)
└── RuntimeControllerGeneratorTest.kt # Update attenuation + priming tests (Phase 3)

graphEditor/src/jvmMain/kotlin/save/
└── ModuleSaveService.kt           # Pass viewModelPackage to controller generator (Phase 3)

StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/processingLogic/
└── TimeIncrementerProcessLogic.kt # Use input values instead of ignoring them (Phase 4)
```

**Structure Decision**: Existing KMP multi-module project structure. Changes span three modules: `fbpDsl` (runtime), `kotlinCompiler` (generators), and `StopWatch` (application). No new modules or directories needed.

## Implementation Phases

### Phase 1: Attenuation Delay in Processor Runtimes (US1 - FR-001, FR-002)

Add a 2-line delay check in all processor/transformer/filter runtimes, **after receive, before process**:

```kotlin
val delayMs = attenuationDelayMs
if (delayMs != null && delayMs > 0) delay(delayMs)
```

**Standard runtimes** (10 files): Insert between final `receive()` and `process()` / `transform()` / `predicate()` call.

**Any-input runtimes** (6 files): Insert inside each `select { onReceive { } }` branch, after updating `lastValue`, before `process()`.

**Test**: Add `In2Out2Runtime respects attenuationDelayMs before processing` test using virtual time (advanceTimeBy).

**Verification**: `./gradlew :fbpDsl:jvmTest`

### Phase 2: Reactive Source Generate Blocks (US2 - FR-003, FR-004, FR-007)

Update `RuntimeFlowGenerator.generateRuntimeInstances()` to replace `awaitCancellation()` with reactive StateFlow observation for source nodes.

**Pattern by output count**:
- 1-output: `${flowName}State.${prop.name}Flow.drop(1).collect { emit(it) }`
- 2-output: `combine(state.prop1Flow, state.prop2Flow) { a, b -> ProcessResult2.both(a, b) }.drop(1).collect { emit(it) }`
- 3-output: Same with `combine` of 3 flows + `ProcessResult3`

**Key**: `drop(1)` skips the initial combine emission; the controller primes instead.

**New method**: `generateReactiveSourceBlock(node, observableProps, flowName)` using `ObservableStateResolver` property matching via `sourceNodeName`.

**Imports to add**: `kotlinx.coroutines.flow.combine`, `kotlinx.coroutines.flow.drop`, `io.codenode.fbpdsl.runtime.ProcessResult2`/`ProcessResult3` (conditional on source output count).

**Tests**: Update 3 existing tests that assert `awaitCancellation()` to assert `combine`/`drop(1)`/`collect` patterns.

**Verification**: `./gradlew :kotlinCompiler:jvmTest`

### Phase 3: Controller Priming + Attenuation Propagation (US3 - FR-005, FR-006, FR-008, FR-011, FR-012)

Update `RuntimeControllerGenerator`:

1. **Add `viewModelPackage` parameter** to `generate()` method. Update call site in `ModuleSaveService.kt:487` to pass `basePackage`.

2. **Add State import** in `generateImports()` when source nodes with observable state exist.

3. **Priming in start()**: After `flow.start(scope)`, generate send calls for each source node's output ports:
   ```kotlin
   flow.timerEmitter.outputChannel1?.send(StopWatchState._elapsedSeconds.value)
   flow.timerEmitter.outputChannel2?.send(StopWatchState._elapsedMinutes.value)
   ```

4. **setAttenuationDelay propagates to ALL nodes**: Change `generateSetAttenuationDelayMethod` to iterate all `codeNodes` instead of just generators.

**Tests**: Update attenuation test assertions, add priming tests, add State import test.

**Verification**: `./gradlew :kotlinCompiler:jvmTest`

### Phase 4: StopWatch Processing Logic (US4 - FR-009, FR-010)

Update `TimeIncrementerProcessLogic.kt` to use input parameters:

```kotlin
val timeIncrementerTick: In2Out2TickBlock<Int, Int, Int, Int> = { elapsedSeconds, elapsedMinutes ->
    var newSeconds = elapsedSeconds + 1
    var newMinutes = elapsedMinutes
    if (newSeconds >= 60) { newSeconds = 0; newMinutes += 1 }
    StopWatchState._elapsedSeconds.value = newSeconds
    StopWatchState._elapsedMinutes.value = newMinutes
    ProcessResult2.both(newSeconds, newMinutes)
}
```

### Phase 5: Regenerate + Integration Test

1. Regenerate StopWatch via graphEditor "Save + Regen Stubs" (or manually)
2. `./gradlew :fbpDsl:jvmTest` — runtime tests
3. `./gradlew :kotlinCompiler:jvmTest` — generator tests
4. `./gradlew :KMPMobileApp:assembleDebug` — compilation check
5. Manual test: StopWatch Start → ticks at 1/sec, Pause → stops, Resume → continues, Reset → zeros
