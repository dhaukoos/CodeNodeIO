# Implementation Plan: Refactor TimerEmitterComponent

**Branch**: `020-refactor-timer-emitter` | **Date**: 2026-02-18 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/020-refactor-timer-emitter/spec.md`

## Summary

Refactor TimerEmitterComponent to cleanly separate common CodeNode runtime lifecycle concerns from unique processing logic. The approach adds a "timed tick" mode to `Out2GeneratorRuntime` where the runtime manages the execution loop (while-loop, delay, pause/resume hooks, state checks) and the component provides only a tick function that returns what to emit. This eliminates ~60% of boilerplate from component code and prevents lifecycle bugs (like the pause/resume and reset/restart bugs recently fixed).

## Technical Context

**Language/Version**: Kotlin 2.1.21 (Kotlin Multiplatform)
**Primary Dependencies**: kotlinx-coroutines 1.8.0, Compose Multiplatform 1.7.3, lifecycle-viewmodel-compose 2.8.0
**Storage**: N/A (in-memory FlowGraph state)
**Testing**: kotlin.test with kotlinx-coroutines-test (runTest, advanceTimeBy, advanceUntilIdle)
**Target Platform**: Desktop (JVM), iOS (native), Android
**Project Type**: Multi-module KMP (fbpDsl library + StopWatch app + KMPMobileApp)
**Performance Goals**: Timer ticks at configurable intervals (default 1000ms), no visible latency on pause/resume
**Constraints**: KMP-compatible (no JVM-only constructs like `synchronized`)
**Scale/Scope**: 5 files modified, 1 new type alias, 1 new factory method

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
| --------- | ------ | ----- |
| I. Code Quality First | PASS | Refactoring improves readability and maintainability by separating concerns. Functions stay under 50 lines. |
| II. Test-Driven Development | PASS | Existing tests serve as regression suite. New runtime capability will have tests written first. |
| III. User Experience Consistency | PASS | No user-facing changes. StopWatch behavior is identical before and after. |
| IV. Performance Requirements | PASS | No performance impact. Tick timing is unchanged. |
| V. Observability & Debugging | PASS | No changes to logging or error handling patterns. |
| Licensing & IP | PASS | No new dependencies. All code is Apache 2.0. |

**Post-Design Re-check**: All gates still PASS. No new patterns, classes, or dependencies that would violate constitution principles.

## Project Structure

### Documentation (this feature)

```text
specs/020-refactor-timer-emitter/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 research output
├── data-model.md        # Phase 1 data model
├── quickstart.md        # Phase 1 quickstart guide
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/
├── runtime/
│   ├── ContinuousTypes.kt          # Add Out2TickBlock type alias
│   └── Out2GeneratorRuntime.kt     # Add timed tick mode to start()
└── model/
    └── CodeNodeFactory.kt          # Add createTimedOut2Generator factory method

fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/
└── TimedGeneratorTest.kt           # New: tests for timed tick mode

StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/
├── usecases/
│   ├── TimerEmitterComponent.kt    # Refactor: replace generator block with tick function
│   └── DisplayReceiverComponent.kt # Clean up: remove commented-out code
└── generated/
    └── StopWatchFlow.kt            # No changes expected

StopWatch/src/commonTest/kotlin/io/codenode/stopwatch/
├── TimerEmitterComponentTest.kt    # Update: adjust for new tick API
└── ChannelIntegrationTest.kt       # Update: adjust setup if needed
```

**Structure Decision**: Existing multi-module KMP structure (fbpDsl library + StopWatch app). Runtime changes go in fbpDsl, component changes go in StopWatch. No new modules or directories needed.

## Design Decisions

### D1: Tick Block vs Generator Block

The existing `Out2GeneratorBlock` signature is:
```kotlin
typealias Out2GeneratorBlock<U, V> = suspend (emit: suspend (ProcessResult2<U, V>) -> Unit) -> Unit
```

The user must implement the entire execution loop including while-loop, delay, pause checks, and emit calls. The new `Out2TickBlock` signature is:
```kotlin
typealias Out2TickBlock<U, V> = suspend () -> ProcessResult2<U, V>
```

The user provides only the "what happens each tick" logic. The runtime manages the loop.

### D2: Backward Compatibility

The existing `Out2GeneratorBlock` API remains unchanged. `createOut2Generator` still works as before. The new `createTimedOut2Generator` is an additional factory method. No breaking changes.

### D3: Timed Loop Ownership

The timed loop (while + delay + pause hooks) moves from the component's generator block into `Out2GeneratorRuntime.start()`. This is implemented by having the runtime construct an internal generator block that wraps the user's tick function in the standard loop pattern, then delegate to the existing `generate(emit)` mechanism.

### D4: Channel Recreation on Restart

The recent bug fix (commit 584a897) already added channel recreation in `Out2GeneratorRuntime.start()`. This fix is preserved and benefits the timed tick mode automatically.
