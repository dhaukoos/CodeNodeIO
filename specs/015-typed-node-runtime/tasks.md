# Tasks: Typed NodeRuntime Stubs

**Input**: Design documents from `/specs/015-typed-node-runtime/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: This feature follows TDD approach with virtual time testing (runTest, advanceTimeBy) per constitution requirements.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **fbpDsl**: Core library (`fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/`)
- **Tests**: (`fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/`)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create ProcessResult types that all multi-output runtimes depend on

- [ ] T001 Create ProcessResult.kt file at fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ProcessResult.kt
- [ ] T002 [P] Implement ProcessResult2<U, V> data class with nullable fields, companion object (of, first, second, both), and destructuring in ProcessResult.kt
- [ ] T003 [P] Implement ProcessResult3<U, V, W> data class with nullable fields, companion object (of, all), and destructuring in ProcessResult.kt

---

## Phase 2: Foundational (Type Aliases)

**Purpose**: Add type aliases for process block signatures that MUST be complete before runtime implementations

**⚠️ CRITICAL**: No runtime class can be implemented until these type aliases exist

- [ ] T004 Add In2Out1ProcessBlock<A, B, R> type alias in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ContinuousTypes.kt
- [ ] T005 [P] Add In3Out1ProcessBlock<A, B, C, R> type alias in ContinuousTypes.kt
- [ ] T006 [P] Add In1Out2ProcessBlock<A, U, V> type alias returning ProcessResult2 in ContinuousTypes.kt
- [ ] T007 [P] Add In1Out3ProcessBlock<A, U, V, W> type alias returning ProcessResult3 in ContinuousTypes.kt
- [ ] T008 [P] Add In2Out2ProcessBlock<A, B, U, V> type alias in ContinuousTypes.kt
- [ ] T009 [P] Add In2Out3ProcessBlock<A, B, U, V, W> type alias in ContinuousTypes.kt
- [ ] T010 [P] Add In3Out2ProcessBlock<A, B, C, U, V> type alias in ContinuousTypes.kt
- [ ] T011 [P] Add In3Out3ProcessBlock<A, B, C, U, V, W> type alias in ContinuousTypes.kt
- [ ] T012 [P] Add In2SinkBlock<A, B> and In3SinkBlock<A, B, C> type aliases in ContinuousTypes.kt
- [ ] T013 [P] Add Out2GeneratorBlock<U, V> and Out3GeneratorBlock<U, V, W> type aliases in ContinuousTypes.kt

**Checkpoint**: ProcessResult types and all type aliases ready - runtime implementation can begin

---

## Phase 3: User Story 1 - Create Typed Processor Node (Priority: P1) 🎯 MVP

**Goal**: Factory methods generate typed runtimes with matching channel configuration for multi-input/output nodes

**Independent Test**: Create In2Out1 processor, wire channels, send data on both inputs, verify output

### Tests for User Story 1

- [ ] T014 [P] [US1] Create TypedNodeRuntimeTest.kt at fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/TypedNodeRuntimeTest.kt
- [ ] T015 [P] [US1] Write test: In2Out1Runtime receives from both inputs and produces output in TypedNodeRuntimeTest.kt
- [ ] T016 [P] [US1] Write test: In2Out1Runtime processes multiple tuples continuously in TypedNodeRuntimeTest.kt
- [ ] T017 [P] [US1] Write test: In3Out1Runtime receives from three inputs and produces output in TypedNodeRuntimeTest.kt

### Implementation for User Story 1

- [ ] T018 [US1] Create In2Out1Runtime<A, B, R> class in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In2Out1Runtime.kt with inputChannel2, synchronous receive loop, continuous processing
- [ ] T019 [P] [US1] Create In3Out1Runtime<A, B, C, R> class in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In3Out1Runtime.kt with inputChannel2, inputChannel3, synchronous receive
- [ ] T020 [US1] Add createIn2Out1Processor<A, B, R> factory method to CodeNodeFactory in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt
- [ ] T021 [US1] Add createIn3Out1Processor<A, B, C, R> factory method to CodeNodeFactory
- [ ] T022 [US1] Verify all US1 tests pass: ./gradlew test --tests "*TypedNodeRuntimeTest*"

**Checkpoint**: Multi-input single-output processors work with continuous mode

---

## Phase 4: User Story 2 - Lifecycle Control of Typed Nodes (Priority: P1)

**Goal**: All typed node runtimes provide start(), stop(), pause(), resume() methods

**Independent Test**: Start In2Out1 node, verify processing, pause it, verify stopped, resume, stop

### Tests for User Story 2

- [ ] T023 [P] [US2] Write test: In2Out1Runtime.stop() cancels processing and closes output channel in TypedNodeRuntimeTest.kt
- [ ] T024 [P] [US2] Write test: In2Out1Runtime.pause() suspends processing until resume() in TypedNodeRuntimeTest.kt
- [ ] T025 [P] [US2] Write test: runtime handles ClosedReceiveChannelException gracefully in TypedNodeRuntimeTest.kt
- [ ] T026 [P] [US2] Write test: runtime handles ClosedSendChannelException gracefully in TypedNodeRuntimeTest.kt

### Implementation for User Story 2

- [ ] T027 [US2] Add pause state check in In2Out1Runtime processing loop (while PAUSED, delay and recheck)
- [ ] T028 [US2] Add pause state check in In3Out1Runtime processing loop
- [ ] T029 [US2] Add ClosedReceiveChannelException handling with graceful shutdown in In2Out1Runtime
- [ ] T030 [US2] Add ClosedReceiveChannelException handling in In3Out1Runtime
- [ ] T031 [US2] Verify all US2 tests pass: ./gradlew test --tests "*TypedNodeRuntimeTest*"

**Checkpoint**: Lifecycle control works on multi-input processors

---

## Phase 5: User Story 3 - Generator and Sink Node Variants (Priority: P2)

**Goal**: Multi-input sinks (0 outputs) and multi-output generators (0 inputs) using factory pattern

**Independent Test**: Create Out2Generator, connect to In2Sink, verify end-to-end data flow

### Tests for User Story 3

- [ ] T032 [P] [US3] Write test: In2SinkRuntime consumes from two inputs continuously in TypedNodeRuntimeTest.kt
- [ ] T033 [P] [US3] Write test: In3SinkRuntime consumes from three inputs continuously in TypedNodeRuntimeTest.kt
- [ ] T034 [P] [US3] Write test: Out2GeneratorRuntime emits ProcessResult2 to two output channels in TypedNodeRuntimeTest.kt
- [ ] T035 [P] [US3] Write test: Out3GeneratorRuntime emits ProcessResult3 to three output channels in TypedNodeRuntimeTest.kt

### Implementation for User Story 3

- [ ] T036 [US3] Create In2SinkRuntime<A, B> class in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In2SinkRuntime.kt
- [ ] T037 [P] [US3] Create In3SinkRuntime<A, B, C> class in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In3SinkRuntime.kt
- [ ] T038 [P] [US3] Create Out2GeneratorRuntime<U, V> class in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/Out2GeneratorRuntime.kt
- [ ] T039 [P] [US3] Create Out3GeneratorRuntime<U, V, W> class in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/Out3GeneratorRuntime.kt
- [ ] T040 [US3] Add createIn2Sink<A, B> and createIn3Sink<A, B, C> factory methods to CodeNodeFactory
- [ ] T041 [US3] Add createOut2Generator<U, V> and createOut3Generator<U, V, W> factory methods to CodeNodeFactory
- [ ] T042 [US3] Verify all US3 tests pass: ./gradlew test --tests "*TypedNodeRuntimeTest*"

**Checkpoint**: Multi-input sinks and multi-output generators work

---

## Phase 6: User Story 4 - ProcessResult for Multi-Output Nodes (Priority: P2)

**Goal**: Multi-output processors use ProcessResult return type with nullable fields for selective sending

**Independent Test**: Create In1Out3 processor, return ProcessResult with some nulls, verify only non-null values sent

### Tests for User Story 4

- [ ] T043 [P] [US4] Write test: In1Out2Runtime sends ProcessResult2 to two outputs in TypedNodeRuntimeTest.kt
- [ ] T044 [P] [US4] Write test: In1Out2Runtime skips sending null values (selective output) in TypedNodeRuntimeTest.kt
- [ ] T045 [P] [US4] Write test: In1Out3Runtime sends ProcessResult3 to three outputs in TypedNodeRuntimeTest.kt
- [ ] T046 [P] [US4] Write test: In2Out2Runtime combines multi-input with multi-output in TypedNodeRuntimeTest.kt
- [ ] T047 [P] [US4] Write test: ProcessResult2 destructuring works correctly in TypedNodeRuntimeTest.kt
- [ ] T048 [P] [US4] Write test: ProcessResult3 destructuring works correctly in TypedNodeRuntimeTest.kt

### Implementation for User Story 4

- [ ] T049 [US4] Create In1Out2Runtime<A, U, V> class in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In1Out2Runtime.kt with outputChannel1, outputChannel2, selective send
- [ ] T050 [P] [US4] Create In1Out3Runtime<A, U, V, W> class in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In1Out3Runtime.kt
- [ ] T051 [P] [US4] Create In2Out2Runtime<A, B, U, V> class in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In2Out2Runtime.kt
- [ ] T052 [P] [US4] Create In2Out3Runtime<A, B, U, V, W> class in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In2Out3Runtime.kt
- [ ] T053 [P] [US4] Create In3Out2Runtime<A, B, C, U, V> class in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In3Out2Runtime.kt
- [ ] T054 [P] [US4] Create In3Out3Runtime<A, B, C, U, V, W> class in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In3Out3Runtime.kt
- [ ] T055 [US4] Add createIn1Out2Processor<A, U, V> and createIn1Out3Processor<A, U, V, W> factory methods to CodeNodeFactory
- [ ] T056 [US4] Add createIn2Out2Processor and createIn2Out3Processor factory methods to CodeNodeFactory
- [ ] T057 [US4] Add createIn3Out2Processor and createIn3Out3Processor factory methods to CodeNodeFactory
- [ ] T058 [US4] Verify all US4 tests pass: ./gradlew test --tests "*TypedNodeRuntimeTest*"

**Checkpoint**: All multi-output configurations work with ProcessResult selective sending

---

## Phase 7: User Story 5 - Named Node Objects (Priority: P3)

**Goal**: All factory methods accept name parameter, and CodeNode reflects the name for debugging

**Independent Test**: Create node with name="TestProcessor", verify name appears in CodeNode

### Tests for User Story 5

- [ ] T059 [P] [US5] Write test: factory method name parameter is reflected in CodeNode.name in TypedNodeRuntimeTest.kt
- [ ] T060 [P] [US5] Write test: multiple nodes have unique names for identification in TypedNodeRuntimeTest.kt

### Implementation for User Story 5

- [ ] T061 [US5] Verify all factory methods accept name parameter (already implemented in factory signatures)
- [ ] T062 [US5] Verify CodeNode.name is set correctly in all factory methods
- [ ] T063 [US5] Verify all US5 tests pass: ./gradlew test --tests "*TypedNodeRuntimeTest*"

**Checkpoint**: Named nodes work for debugging and identification

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Edge case validation, integration testing, documentation

- [ ] T064 [P] Write test: factory rejects 0-input AND 0-output configuration in TypedNodeRuntimeTest.kt
- [ ] T065 [P] Add factory validation to reject 0×0 configuration with clear error message
- [ ] T066 Run all tests to verify no regressions: ./gradlew test
- [ ] T067 Update MEMORY.md with lessons learned from this feature
- [ ] T068 Verify quickstart.md examples work as documented

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup - BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Foundational phase completion
  - US1 (Typed Processor) can start immediately after Foundational
  - US2 (Lifecycle) depends on US1 runtime classes existing
  - US3 (Generator/Sink) can start in parallel with US1
  - US4 (ProcessResult) depends on ProcessResult types from Setup
  - US5 (Named Nodes) can start after any factory methods exist
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

```
Phase 1 (Setup: ProcessResult)
    │
    ▼
Phase 2 (Foundational: Type Aliases) ─────────────────────────┐
    │                                                          │
    ├───────────────────┬───────────────────┐                 │
    ▼                   ▼                   ▼                 │
US1 (Processor)    US3 (Gen/Sink)     US5 (Named) ←──────────┤
    │                   │                   │                 │
    ▼                   │                   │                 │
US2 (Lifecycle)         │                   │                 │
    │                   │                   │                 │
    └─────────┬─────────┘                   │                 │
              ▼                             │                 │
          US4 (Multi-Output)                │                 │
              │                             │                 │
              └─────────────────────────────┘                 │
                            │                                 │
                            ▼                                 │
                      Phase 8 (Polish)                        │
```

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Runtime classes before factory methods
- Factory methods before integration tests
- Story complete before moving to next priority

### Parallel Opportunities

- T002, T003 (ProcessResult types) can run in parallel
- T004-T013 (type aliases) can run in parallel after ProcessResult
- T014-T017 (US1 tests) can be written in parallel
- T018, T019 (runtime classes) - T019 can run parallel after T018 pattern established
- T036-T039 (US3 runtimes) can run in parallel
- T049-T054 (US4 runtimes) can run in parallel

---

## Parallel Example: Phase 2 (Type Aliases)

```bash
# After ProcessResult types exist, launch all type aliases in parallel:
Task T004: "Add In2Out1ProcessBlock type alias"
Task T005: "Add In3Out1ProcessBlock type alias"
Task T006: "Add In1Out2ProcessBlock type alias"
Task T007: "Add In1Out3ProcessBlock type alias"
Task T008: "Add In2Out2ProcessBlock type alias"
Task T009: "Add In2Out3ProcessBlock type alias"
Task T010: "Add In3Out2ProcessBlock type alias"
Task T011: "Add In3Out3ProcessBlock type alias"
Task T012: "Add sink block type aliases"
Task T013: "Add generator block type aliases"
```

---

## Parallel Example: User Story 4 (Multi-Output Runtimes)

```bash
# All runtime classes can be created in parallel (different files):
Task T049: "Create In1Out2Runtime"
Task T050: "Create In1Out3Runtime"
Task T051: "Create In2Out2Runtime"
Task T052: "Create In2Out3Runtime"
Task T053: "Create In3Out2Runtime"
Task T054: "Create In3Out3Runtime"
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2 Only)

1. Complete Phase 1: Setup (ProcessResult types)
2. Complete Phase 2: Foundational (type aliases)
3. Complete Phase 3: User Story 1 (In2Out1, In3Out1 processors)
4. Complete Phase 4: User Story 2 (lifecycle control)
5. **STOP and VALIDATE**: Test multi-input processors work with continuous mode
6. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → ProcessResult and type aliases ready
2. Add US1 (Processors) → Test independently → Multi-input processors work
3. Add US2 (Lifecycle) → Test independently → Pause/resume/graceful shutdown
4. Add US3 (Gen/Sink) → Test independently → Multi-input sinks, multi-output generators
5. Add US4 (Multi-Output) → Test independently → Full 15 configurations supported
6. Add US5 (Named Nodes) → Test independently → Debugging/identification

### Single Developer Strategy

Execute in priority order:
1. Phase 1 → Phase 2 → Phase 3 (US1) → Phase 4 (US2) → Phase 5 (US3) → Phase 6 (US4) → Phase 7 (US5) → Phase 8

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing (TDD per constitution)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Runtime tests use virtual time (runTest, advanceTimeBy) for deterministic testing
- All runtimes operate in continuous mode per clarification session
