# Tasks: Node Control Extraction

**Input**: Design documents from `/specs/013-node-control-extraction/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **fbpDsl/**: Core FBP DSL model library (CodeNode lives here)
- **StopWatch/**: Example flow module with components (TimerEmitterComponent, DisplayReceiverComponent)

---

## Phase 1: Setup

**Purpose**: Verify existing infrastructure and prepare for extraction

- [x] T001 Review current CodeNode structure at fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt
- [x] T002 Review current TimerEmitterComponent at StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/TimerEmitterComponent.kt
- [x] T003 Review current DisplayReceiverComponent at StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/DisplayReceiverComponent.kt
- [x] T004 Run existing test suites to establish baseline: `./gradlew :fbpDsl:jvmTest :StopWatch:jvmTest`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core lifecycle infrastructure in CodeNode that MUST be complete before component refactoring

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T005 Add coroutine imports (Job, CoroutineScope, launch, isActive) to fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt
- [x] T006 Add `@Transient var nodeControlJob: Job? = null` property to CodeNode data class at fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt

**Checkpoint**: Foundation ready - CodeNode has nodeControlJob property

---

## Phase 3: User Story 1 - Unified Node Lifecycle Control (Priority: P1) 🎯 MVP

**Goal**: CodeNode provides standardized lifecycle control (start, stop, pause, resume) so all node components have consistent execution management

**Independent Test**: Create a CodeNode, call start(), verify RUNNING state with active job, call stop() and verify graceful shutdown to IDLE

### Tests for User Story 1

- [x] T007 [US1] Create CodeNodeLifecycleTest file at fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/CodeNodeLifecycleTest.kt with test imports and class structure
- [x] T008 [US1] Write test `start creates nodeControlJob and runs processingBlock` in CodeNodeLifecycleTest.kt
- [x] T009 [US1] Write test `stop cancels nodeControlJob` in CodeNodeLifecycleTest.kt
- [x] T010 [US1] Write test `start cancels existing job before creating new one` in CodeNodeLifecycleTest.kt
- [x] T011 [US1] Write test `stop on idle node is no-op` in CodeNodeLifecycleTest.kt
- [x] T012 [US1] Write test `stop on paused node transitions to IDLE` in CodeNodeLifecycleTest.kt
- [x] T013 [US1] Write test `pause on running node transitions to PAUSED` in CodeNodeLifecycleTest.kt
- [x] T014 [US1] Write test `resume from paused transitions back to RUNNING` in CodeNodeLifecycleTest.kt
- [x] T015 [US1] Write test `pause on non-running node is no-op` in CodeNodeLifecycleTest.kt
- [x] T016 [US1] Write test `resume on non-paused node is no-op` in CodeNodeLifecycleTest.kt

### Implementation for User Story 1

- [x] T017 [US1] Implement `suspend fun start(scope: CoroutineScope, processingBlock: suspend () -> Unit)` method in CodeNode.kt
- [x] T018 [US1] Implement `fun stop()` method in CodeNode.kt that cancels nodeControlJob and sets it to null
- [x] T019 [US1] Implement `fun pause()` method in CodeNode.kt (only valid when RUNNING)
- [x] T020 [US1] Implement `fun resume()` method in CodeNode.kt (only valid when PAUSED)
- [x] T021 [US1] Run CodeNodeLifecycleTest to verify all 9 tests pass: `./gradlew :fbpDsl:jvmTest --tests "*CodeNodeLifecycleTest*"`

**Checkpoint**: CodeNode has full lifecycle control (start, stop, pause, resume) - independently testable

---

## Phase 4: User Story 2 - TimerEmitterComponent Refactoring (Priority: P2)

**Goal**: TimerEmitterComponent delegates lifecycle management to CodeNode, retaining only timer-specific business logic

**Independent Test**: Run existing TimerEmitterComponentTest suite - all tests should pass after refactoring

### Implementation for User Story 2

- [ ] T022 [US2] Add `var codeNode: CodeNode? = null` property to TimerEmitterComponent at StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/TimerEmitterComponent.kt
- [ ] T023 [US2] Add convenience `executionState` getter that delegates to codeNode in TimerEmitterComponent.kt
- [ ] T024 [US2] Refactor `start(scope)` method to delegate job management to `codeNode.start()` while retaining timer tick logic in TimerEmitterComponent.kt
- [ ] T025 [US2] Refactor `stop()` method to delegate to `codeNode.stop()` in TimerEmitterComponent.kt
- [ ] T026 [US2] Remove `private var timerJob: Job?` property (now delegated to CodeNode) from TimerEmitterComponent.kt
- [ ] T027 [US2] Remove local `var executionState` property (now delegated to CodeNode) from TimerEmitterComponent.kt
- [ ] T028 [US2] Run TimerEmitterComponentTest to verify all existing tests pass: `./gradlew :StopWatch:jvmTest --tests "*TimerEmitterComponentTest*"`

**Checkpoint**: TimerEmitterComponent delegates lifecycle to CodeNode - existing tests pass

---

## Phase 5: User Story 3 - DisplayReceiverComponent Refactoring (Priority: P3)

**Goal**: DisplayReceiverComponent uses CodeNode's lifecycle management, validating the pattern works for sink components

**Independent Test**: Run existing DisplayReceiverComponentTest suite - all tests should pass after refactoring

### Implementation for User Story 3

- [ ] T029 [US3] Add `var codeNode: CodeNode? = null` property to DisplayReceiverComponent at StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/DisplayReceiverComponent.kt
- [ ] T030 [US3] Refactor `start(scope)` method to delegate job management to `codeNode.start()` while retaining channel iteration logic in DisplayReceiverComponent.kt
- [ ] T031 [US3] Refactor `stop()` method to delegate to `codeNode.stop()` in DisplayReceiverComponent.kt
- [ ] T032 [US3] Remove `private var collectionJob: Job?` property (now delegated to CodeNode) from DisplayReceiverComponent.kt
- [ ] T033 [US3] Run DisplayReceiverComponentTest to verify all existing tests pass: `./gradlew :StopWatch:jvmTest --tests "*DisplayReceiverComponentTest*"`

**Checkpoint**: DisplayReceiverComponent delegates lifecycle to CodeNode - existing tests pass

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and integration verification

- [ ] T034 [P] Run all fbpDsl tests: `./gradlew :fbpDsl:jvmTest`
- [ ] T035 [P] Run all StopWatch tests: `./gradlew :StopWatch:jvmTest`
- [ ] T036 Run ChannelIntegrationTest to verify end-to-end flow: `./gradlew :StopWatch:jvmTest --tests "*ChannelIntegrationTest*"`
- [ ] T037 Update quickstart.md verification checklist at specs/013-node-control-extraction/quickstart.md
- [ ] T038 Verify no duplicate job management code remains in components (code review)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - US1 (Lifecycle Control) must complete before US2 and US3 (components need CodeNode methods)
  - US2 (TimerEmitter) and US3 (DisplayReceiver) can run in parallel after US1
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - Core lifecycle infrastructure
- **User Story 2 (P2)**: Depends on US1 - Needs CodeNode.start(), stop(), pause(), resume() methods
- **User Story 3 (P3)**: Depends on US1 - Needs CodeNode.start(), stop() methods; can run parallel with US2

### Within Each User Story

- Tests written first (TDD approach from constitution)
- Tests must fail before implementation
- Implementation follows test definitions
- Run story-specific tests after implementation
- Story complete when all tests pass

### Parallel Opportunities

- T001, T002, T003 can run in parallel (review tasks, different files)
- T007-T016 tests can be written in one file sequentially
- T022-T027 and T029-T032 can run in parallel after US1 completes (different files)
- T034, T035 can run in parallel (different modules)

---

## Parallel Example: User Stories 2 & 3 (After US1 Completes)

```bash
# Launch component refactoring in parallel:
Task: "Refactor TimerEmitterComponent in StopWatch/.../TimerEmitterComponent.kt"
Task: "Refactor DisplayReceiverComponent in StopWatch/.../DisplayReceiverComponent.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (verification)
2. Complete Phase 2: Foundational (nodeControlJob property)
3. Complete Phase 3: User Story 1 (lifecycle methods + tests)
4. **STOP and VALIDATE**: Run CodeNodeLifecycleTest
5. Verify 9 tests pass before proceeding

### Incremental Delivery

1. Complete Setup + Foundational → Infrastructure ready
2. Add User Story 1 → Test independently → Core lifecycle works
3. Add User Story 2 → Test independently → TimerEmitter refactored
4. Add User Story 3 → Test independently → DisplayReceiver refactored
5. Each story validates the extraction pattern without breaking previous stories

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US2 and US3 depend on US1 (need CodeNode lifecycle methods)
- US2 and US3 can run in parallel once US1 is complete
- All existing tests (12+) must pass after refactoring - no test expectation changes
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
