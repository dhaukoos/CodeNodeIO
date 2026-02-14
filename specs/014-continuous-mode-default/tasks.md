# Tasks: Continuous Mode as Default

**Input**: Design documents from `/specs/014-continuous-mode-default/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Test tasks included - this feature uses TDD approach with virtual time testing (runTest, advanceTimeBy).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **fbpDsl**: Core library (`fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/`)
- **StopWatch**: Example module (`StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/`)
- **Tests**: (`fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/`)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create new runtime package and type aliases

- [ ] T001 Create runtime package directory at fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/
- [ ] T002 [P] Create ContinuousTypes.kt with type aliases in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ContinuousTypes.kt

---

## Phase 2: Foundational (Refactor Lifecycle from CodeNode to NodeRuntime)

**Purpose**: Move feature 013's lifecycle additions from CodeNode to NodeRuntime - MUST be complete before user stories

**⚠️ CRITICAL**: This phase relocates runtime concerns out of the serializable CodeNode model

### Tests for Foundational Phase

- [ ] T003 Rename CodeNodeLifecycleTest.kt to NodeRuntimeTest.kt at fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/NodeRuntimeTest.kt
- [ ] T004 Update test imports and class references in NodeRuntimeTest.kt to test NodeRuntime instead of CodeNode

### Implementation for Foundational Phase

- [ ] T005 Create NodeRuntime.kt class with lifecycle methods in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/NodeRuntime.kt
- [ ] T006 Remove @Transient nodeControlJob property from CodeNode in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt
- [ ] T007 Remove start(scope, processingBlock) method from CodeNode in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt
- [ ] T008 Remove stop() method from CodeNode in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt
- [ ] T009 Remove pause() and resume() methods from CodeNode in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt
- [ ] T010 Update TimerEmitterComponent to use NodeRuntime in StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/TimerEmitterComponent.kt
- [ ] T011 Update DisplayReceiverComponent to use NodeRuntime in StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/DisplayReceiverComponent.kt
- [ ] T012 Update ChannelIntegrationTest to use NodeRuntime in StopWatch/src/commonTest/kotlin/io/codenode/stopwatch/usecases/ChannelIntegrationTest.kt
- [ ] T013 Run all tests to verify lifecycle relocation works: ./gradlew test

**Checkpoint**: NodeRuntime owns all lifecycle control, CodeNode is pure serializable model

---

## Phase 3: User Story 1 - Continuous Generator Nodes (Priority: P1) 🎯 MVP

**Goal**: Create generator nodes that emit continuously using the factory

**Independent Test**: Create a timer generator that emits every 100ms, verify 5 emissions after 500ms virtual time

### Tests for User Story 1

- [ ] T014 [P] [US1] Create ContinuousFactoryTest.kt test file at fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/ContinuousFactoryTest.kt
- [ ] T015 [US1] Write test: generator emits values at correct intervals using runTest and advanceTimeBy in ContinuousFactoryTest.kt
- [ ] T016 [US1] Write test: generator respects isActive check for graceful shutdown in ContinuousFactoryTest.kt
- [ ] T017 [US1] Write test: generator output channel closes when stopped in ContinuousFactoryTest.kt

### Implementation for User Story 1

- [ ] T018 [US1] Add createContinuousGenerator<T> method to CodeNodeFactory in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt
- [ ] T019 [US1] Implement generator loop with isActive check and emit function in createContinuousGenerator
- [ ] T020 [US1] Add channelCapacity parameter to createContinuousGenerator with default Channel.BUFFERED
- [ ] T021 [US1] Verify all US1 tests pass: ./gradlew test --tests "*ContinuousFactoryTest*"

**Checkpoint**: Continuous generators work with virtual time testing

---

## Phase 4: User Story 2 - Continuous Sink Nodes (Priority: P1)

**Goal**: Create sink nodes that collect from channels continuously

**Independent Test**: Create a sink that receives from a channel, send 3 values, verify all 3 received

### Tests for User Story 2

- [ ] T022 [P] [US2] Write test: sink receives all values from input channel in ContinuousFactoryTest.kt
- [ ] T023 [P] [US2] Write test: sink handles channel closure gracefully in ContinuousFactoryTest.kt
- [ ] T024 [US2] Write test: sink uses NodeRuntime lifecycle control in ContinuousFactoryTest.kt

### Implementation for User Story 2

- [ ] T025 [US2] Add createContinuousSink<T> method to CodeNodeFactory in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt
- [ ] T026 [US2] Implement sink loop that iterates over input channel with ClosedReceiveChannelException handling
- [ ] T027 [US2] Verify all US2 tests pass: ./gradlew test --tests "*ContinuousFactoryTest*"

**Checkpoint**: Continuous sinks work, handle channel closure gracefully

---

## Phase 5: User Story 4 - Channel-Based Communication (Priority: P1)

**Goal**: Nodes communicate via typed channels with backpressure

**Independent Test**: Wire generator to sink via buffered channel, verify data flows through

### Tests for User Story 4

- [ ] T028 [P] [US4] Write test: channel wiring between generator and sink works in ContinuousFactoryTest.kt
- [ ] T029 [P] [US4] Write test: backpressure prevents memory exhaustion (buffered channel fills) in ContinuousFactoryTest.kt
- [ ] T030 [US4] Write test: channel closure propagates through flow graph in ContinuousFactoryTest.kt

### Implementation for User Story 4

- [ ] T031 [US4] Ensure NodeRuntime inputChannel/outputChannel properties support wiring in NodeRuntime.kt
- [ ] T032 [US4] Add channel creation helper to factory methods (internal use)
- [ ] T033 [US4] Verify all US4 tests pass: ./gradlew test --tests "*ContinuousFactoryTest*"

**Checkpoint**: Typed channels work with backpressure, generator→sink flows work

---

## Phase 6: User Story 3 - Continuous Transformer Nodes (Priority: P2)

**Goal**: Create transformer nodes that process streams continuously

**Independent Test**: Create transformer that doubles input values, wire generator→transformer→sink, verify output

### Tests for User Story 3

- [ ] T034 [P] [US3] Write test: transformer receives input and emits transformed output in ContinuousFactoryTest.kt
- [ ] T035 [P] [US3] Write test: transformer respects pause/resume for flow control in ContinuousFactoryTest.kt
- [ ] T036 [US3] Write test: transformer chain (generator→transformer→sink) works in ContinuousFactoryTest.kt

### Implementation for User Story 3

- [ ] T037 [US3] Add createContinuousTransformer<TIn, TOut> method to CodeNodeFactory in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt
- [ ] T038 [US3] Implement transformer loop: receive→transform→send with proper channel handling
- [ ] T039 [US3] Add createContinuousFilter<T> method to CodeNodeFactory for predicate-based filtering
- [ ] T040 [US3] Verify all US3 tests pass: ./gradlew test --tests "*ContinuousFactoryTest*"

**Checkpoint**: Transformers and filters work in continuous pipelines

---

## Phase 7: User Story 5 - Backward Compatibility (Priority: P2)

**Goal**: Single-invocation mode remains available for existing code

**Independent Test**: Existing ProcessingLogic implementations still work with deprecated factory methods

### Tests for User Story 5

- [ ] T041 [P] [US5] Write test: existing createGenerator method still works in ContinuousFactoryTest.kt
- [ ] T042 [P] [US5] Write test: existing createSink method still works in ContinuousFactoryTest.kt
- [ ] T043 [US5] Write test: ProcessingLogic implementations work unchanged

### Implementation for User Story 5

- [ ] T044 [US5] Add @Deprecated annotation to createGenerator with migration message in CodeNodeFactory.kt
- [ ] T045 [US5] Add @Deprecated annotation to createSink with migration message in CodeNodeFactory.kt
- [ ] T046 [US5] Verify existing tests still pass: ./gradlew test
- [ ] T047 [US5] Document migration path in quickstart.md

**Checkpoint**: Backward compatibility verified, deprecation warnings in place

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Integration validation and documentation

- [ ] T048 [P] Replace TimerEmitterComponent implementation with factory-created generator (optional refactor)
- [ ] T049 [P] Replace DisplayReceiverComponent implementation with factory-created sink (optional refactor)
- [ ] T050 Run ChannelIntegrationTest with factory-created nodes: ./gradlew :StopWatch:test
- [ ] T051 Run all tests to verify no regressions: ./gradlew test
- [ ] T052 Update MEMORY.md with lessons learned from this feature
- [ ] T053 Verify quickstart.md examples work as documented

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup - BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Foundational phase completion
  - US1 (Generators) can start immediately after Foundational
  - US2 (Sinks) can start immediately after Foundational (parallel with US1)
  - US4 (Channels) depends on US1 + US2 completion (needs both to test wiring)
  - US3 (Transformers) depends on US4 completion (needs channel infrastructure)
  - US5 (Backward Compat) can start after Foundational (parallel with US1/US2)
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

```
Phase 1 (Setup)
    │
    ▼
Phase 2 (Foundational) ──────────────────────────────────────┐
    │                                                         │
    ├───────────────────┬───────────────────┐                │
    ▼                   ▼                   ▼                │
US1 (Generators)    US2 (Sinks)        US5 (Backward)        │
    │                   │                   │                │
    └─────────┬─────────┘                   │                │
              ▼                             │                │
          US4 (Channels)                    │                │
              │                             │                │
              ▼                             │                │
          US3 (Transformers)                │                │
              │                             │                │
              └─────────────────────────────┘                │
                            │                                │
                            ▼                                │
                      Phase 8 (Polish)                       │
```

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Factory methods before loop implementations
- Core implementation before integration tests
- Story complete before moving to next priority

### Parallel Opportunities

- T002 (ContinuousTypes.kt) can run in parallel with T001 directory creation
- T003, T004 (test file rename) can run in parallel with T005 (NodeRuntime creation)
- T014-T017 (US1 tests) can be written in parallel
- T022-T024 (US2 tests) can be written in parallel
- US1 and US2 can be implemented in parallel after Foundational
- US5 (backward compatibility) can run in parallel with US1/US2

---

## Parallel Example: Foundational Phase

```bash
# After T001 (directory creation), launch in parallel:
Task T002: "Create ContinuousTypes.kt"
Task T003: "Rename CodeNodeLifecycleTest.kt"
Task T005: "Create NodeRuntime.kt"

# After T005 (NodeRuntime created), launch T006-T009 sequentially (same file):
Task T006: "Remove nodeControlJob from CodeNode"
Task T007: "Remove start() from CodeNode"
Task T008: "Remove stop() from CodeNode"
Task T009: "Remove pause()/resume() from CodeNode"

# T010-T012 can run in parallel (different files):
Task T010: "Update TimerEmitterComponent"
Task T011: "Update DisplayReceiverComponent"
Task T012: "Update ChannelIntegrationTest"
```

---

## Parallel Example: User Stories 1 & 2

```bash
# After Foundational complete, launch US1 and US2 in parallel:

# US1 Tests (parallel):
Task T014: "Create ContinuousFactoryTest.kt"
Task T015: "Test generator emits at intervals"
Task T016: "Test generator respects isActive"
Task T017: "Test generator channel closes"

# US2 Tests (parallel, same file but different tests):
Task T022: "Test sink receives values"
Task T023: "Test sink handles closure"
Task T024: "Test sink lifecycle"

# Implementation follows (US1 and US2 can be parallel since different methods):
Task T018: "Add createContinuousGenerator"
Task T025: "Add createContinuousSink"
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2 Only)

1. Complete Phase 1: Setup (create runtime package)
2. Complete Phase 2: Foundational (relocate lifecycle to NodeRuntime)
3. Complete Phase 3: User Story 1 (Continuous Generators)
4. Complete Phase 4: User Story 2 (Continuous Sinks)
5. **STOP and VALIDATE**: Test generator→sink wiring works
6. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → NodeRuntime ready, CodeNode clean
2. Add US1 (Generators) → Test independently → Factory creates working generators
3. Add US2 (Sinks) → Test independently → Factory creates working sinks
4. Add US4 (Channels) → Test wiring → Generator-to-sink flows work
5. Add US3 (Transformers) → Test independently → Full pipeline support
6. Add US5 (Backward Compat) → Verify existing code works
7. Polish → Integration validation

### Single Developer Strategy

Execute in priority order:
1. Phase 1 → Phase 2 → Phase 3 (US1) → Phase 4 (US2) → Phase 5 (US4) → Phase 6 (US3) → Phase 7 (US5) → Phase 8

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- NodeRuntime tests use virtual time (runTest, advanceTimeBy) for deterministic testing
- Channel.BUFFERED (64 elements) is the default buffer size
