# Tasks: Refactor TimerEmitterComponent

**Input**: Design documents from `/specs/020-refactor-timer-emitter/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Included per constitution (TDD mandate). Tests written first, verified failing before implementation.

**Organization**: Tasks grouped by user story. US2 (runtime infrastructure) is foundational and must complete before US1 (component refactoring).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **fbpDsl module**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/`
- **fbpDsl tests**: `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/`
- **StopWatch module**: `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/`
- **StopWatch tests**: `StopWatch/src/commonTest/kotlin/io/codenode/stopwatch/`

---

## Phase 1: Setup

**Purpose**: Verify current state and establish baseline

- [X] T001 Run all existing StopWatch JVM tests to establish passing baseline via `./gradlew :StopWatch:jvmTest :fbpDsl:jvmTest`
- [X] T002 Snapshot current TimerEmitterComponent line count and verify current behavior (start, pause, resume, reset) in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/TimerEmitterComponent.kt`

---

## Phase 2: User Story 2 - Runtime Handles Timed Generation Loop (Priority: P2) - Foundational

**Goal**: Add timed tick mode to Out2GeneratorRuntime so components can supply a simple tick function instead of implementing the entire execution loop.

**Independent Test**: Create a timed generator with a tick function and verify it emits at the specified interval with correct pause/resume/stop behavior.

**⚠️ CRITICAL**: US1 (component refactoring) depends on this phase completing first.

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T003 [P] [US2] Write test: timed Out2Generator emits tick results at configured interval in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/TimedGeneratorTest.kt`
- [X] T004 [P] [US2] Write test: timed Out2Generator pauses tick loop when execution state is PAUSED in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/TimedGeneratorTest.kt`
- [X] T005 [P] [US2] Write test: timed Out2Generator resumes ticking after resume in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/TimedGeneratorTest.kt`
- [X] T006 [P] [US2] Write test: timed Out2Generator stops cleanly and closes channels on stop in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/TimedGeneratorTest.kt`
- [X] T007 [P] [US2] Write test: timed Out2Generator with zero interval emits without delay in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/TimedGeneratorTest.kt`
- [X] T008 [P] [US2] Write test: timed Out2Generator distributes null-filtered ProcessResult2 to selective channels in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/TimedGeneratorTest.kt`

### Implementation for User Story 2

- [X] T009 [US2] Add `Out2TickBlock<U, V>` type alias in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ContinuousTypes.kt`
- [X] T010 [US2] Add `tickIntervalMs` and `tickBlock` constructor parameters to Out2GeneratorRuntime in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/Out2GeneratorRuntime.kt` — **Approach changed**: timed tick loop is in the factory method instead of the constructor/start(), keeping Out2GeneratorRuntime unchanged
- [X] T011 [US2] Implement timed tick loop in Out2GeneratorRuntime.start() that calls tickBlock at tickIntervalMs with pause/resume/stop hooks in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/Out2GeneratorRuntime.kt` — **Approach changed**: timed loop is constructed in `createTimedOut2Generator` factory, wrapped as a generate block that leverages existing pause/resume/stop hooks in start()
- [X] T012 [US2] Add `createTimedOut2Generator` factory method in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt`
- [X] T013 [US2] Verify all T003-T008 tests pass and existing Out2GeneratorRuntime tests still pass via `./gradlew :fbpDsl:jvmTest`

**Checkpoint**: Timed Out2Generator runtime is functional. New factory method available. All existing tests still pass.

---

## Phase 3: User Story 1 - Simplified Component Authoring (Priority: P1) 🎯 MVP

**Goal**: Refactor TimerEmitterComponent to use the timed tick mode, removing all lifecycle boilerplate and leaving only the incrementer business logic and StateFlow declarations.

**Independent Test**: StopWatch app starts, pauses, resumes, and resets correctly with the refactored component. Component code contains no ExecutionState references in business logic.

### Tests for User Story 1

> **NOTE: Existing tests serve as regression suite. Update setup to match new API.**

- [X] T014 [US1] Update TimerEmitterComponentTest to use refactored component API (tick-based, no manual executionState setup before start) in `StopWatch/src/commonTest/kotlin/io/codenode/stopwatch/usecases/TimerEmitterComponentTest.kt`
- [X] T015 [US1] Update ChannelIntegrationTest if any wiring changes are needed for refactored component in `StopWatch/src/commonTest/kotlin/io/codenode/stopwatch/ChannelIntegrationTest.kt`

### Implementation for User Story 1

- [X] T016 [US1] Refactor TimerEmitterComponent: replace generator block with Out2TickBlock tick function containing only incrementer + StateFlow updates in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/TimerEmitterComponent.kt`
- [X] T017 [US1] Refactor TimerEmitterComponent: replace createOut2Generator with createTimedOut2Generator factory call in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/TimerEmitterComponent.kt`
- [X] T018 [US1] Refactor TimerEmitterComponent: remove unused imports (ExecutionState, currentCoroutineContext, isActive, delay) in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/TimerEmitterComponent.kt`
- [X] T019 [US1] Verify all StopWatch tests pass via `./gradlew :StopWatch:jvmTest`
- [X] T020 [US1] Verify refactored TimerEmitterComponent has fewer than 20 lines of business logic (SC-001) in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/TimerEmitterComponent.kt` — **Result: 14 lines**

**Checkpoint**: TimerEmitterComponent is refactored. All StopWatch tests pass. Component contains only business logic.

---

## Phase 4: User Story 3 - DisplayReceiverComponent Cleanup (Priority: P3)

**Goal**: Clean up DisplayReceiverComponent to follow the same separation pattern, removing commented-out code and ensuring consistent delegation.

**Independent Test**: StopWatch display updates correctly. Component code contains only consumer function and StateFlow declarations.

### Implementation for User Story 3

- [X] T021 [US3] Remove commented-out code blocks (lines 58-71, 84-88) from DisplayReceiverComponent in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/DisplayReceiverComponent.kt` — Also removed unused `speedAttenuation` param and unused imports (`currentCoroutineContext`, `delay`, `isActive`)
- [X] T022 [US3] Verify DisplayReceiverComponent contains only consumer function, StateFlow declarations, and thin delegation in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/DisplayReceiverComponent.kt` — **Result: 6 lines of business logic**
- [X] T023 [US3] Verify all StopWatch tests still pass after cleanup via `./gradlew :StopWatch:jvmTest`

**Checkpoint**: Both components follow the clean separation pattern. All tests pass.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Final validation, documentation, and cross-module verification

- [X] T024 Run full test suite across all modules via `./gradlew :fbpDsl:jvmTest :StopWatch:jvmTest`
- [X] T025 Verify StopWatch app behavior (start, pause, resume, reset) matches pre-refactoring behavior (SC-003)
- [X] T026 Update quickstart.md with final code examples reflecting actual implementation in `specs/020-refactor-timer-emitter/quickstart.md`
- [X] T027 Verify SC-001: TimerEmitterComponent business logic is under 20 lines — **14 lines**
- [X] T028 Verify SC-004: A new timed generator can be created with only a tick function and interval (demonstrated by TimedGeneratorTest)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - establishes baseline
- **US2 Foundational (Phase 2)**: Depends on Setup - BLOCKS US1
- **US1 MVP (Phase 3)**: Depends on US2 completion (needs timed generator runtime)
- **US3 Cleanup (Phase 4)**: No dependency on US1/US2 (independent cleanup)
- **Polish (Phase 5)**: Depends on all user stories complete

### User Story Dependencies

- **User Story 2 (P2)**: Foundational - must complete first (provides runtime infrastructure)
- **User Story 1 (P1)**: Depends on US2 - uses the timed generator runtime to refactor the component
- **User Story 3 (P3)**: Independent - can run in parallel with US1 after US2 completes

### Within Each User Story

- Tests written and verified failing before implementation
- Type aliases before runtime modifications
- Runtime modifications before factory methods
- Factory methods before component refactoring
- All tests pass before checkpoint

### Parallel Opportunities

- T003-T008: All US2 tests can be written in parallel (same file, different test functions)
- T009, T010: Type alias and constructor change can be done in parallel (different files)
- T014, T015: US1 test updates can be done in parallel (different files)
- T021-T022: US3 cleanup tasks can run in parallel with US1 tasks (different modules)

---

## Parallel Example: User Story 2

```bash
# Write all US2 tests in parallel (same file, different functions):
Task: "T003 - Timed Out2Generator emits at interval"
Task: "T004 - Timed Out2Generator pauses"
Task: "T005 - Timed Out2Generator resumes"
Task: "T006 - Timed Out2Generator stops cleanly"
Task: "T007 - Zero interval emits without delay"
Task: "T008 - Selective channel distribution"

# Then implement in sequence:
Task: "T009 - Add type alias"
Task: "T010 - Add constructor parameters"
Task: "T011 - Implement timed loop"
Task: "T012 - Add factory method"
Task: "T013 - Verify all tests pass"
```

---

## Implementation Strategy

### MVP First (US2 + US1)

1. Complete Phase 1: Setup (baseline)
2. Complete Phase 2: US2 - Timed generator runtime (foundational)
3. Complete Phase 3: US1 - Refactor TimerEmitterComponent (MVP value)
4. **STOP and VALIDATE**: All tests pass, StopWatch works identically
5. Proceed to US3 and Polish if satisfied

### Incremental Delivery

1. Setup → Baseline established
2. US2 (runtime) → New timed tick capability available, all existing tests pass
3. US1 (component) → TimerEmitterComponent simplified, StopWatch works
4. US3 (cleanup) → DisplayReceiverComponent cleaned up
5. Polish → Final validation and documentation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US2 is prioritized as foundational despite being P2 in spec because US1 depends on it
- Commit after each phase checkpoint
- The refactoring preserves all observable behavior - only internal structure changes
