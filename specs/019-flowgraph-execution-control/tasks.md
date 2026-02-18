# Tasks: Unified FlowGraph Execution Control

**Input**: Design documents from `/specs/019-flowgraph-execution-control/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, quickstart.md

**Tests**: Unit tests included as they are essential for verifying framework-level functionality (RuntimeRegistry, pause hooks).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: Branch creation and project verification

- [ ] T001 Verify project compiles with `./gradlew build`
- [ ] T002 [P] Review existing RootControlNode implementation in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/RootControlNode.kt`
- [ ] T003 [P] Review existing NodeRuntime pause/resume methods in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/NodeRuntime.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T004 Create RuntimeRegistry class in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/RuntimeRegistry.kt` with register, unregister, pauseAll, resumeAll, stopAll methods
- [ ] T005 Add RuntimeRegistry unit tests in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/RuntimeRegistryTest.kt`
- [ ] T006 Verify RuntimeRegistry thread-safety with concurrent register/unregister test

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 3 - Runtime Registration (Priority: P1) 🎯 MVP

**Goal**: Bridge between model state and runtime execution via RuntimeRegistry integration

**Independent Test**: Start a flow, verify runtimes register, call pauseAll(), confirm all registered runtimes receive pause() calls

**Why First**: This is the foundation that US1 and US2 depend on - RootControlNode needs RuntimeRegistry to propagate state to runtimes

### Implementation for User Story 3

- [ ] T007 [US3] Add registry property to RootControlNode.createFor() factory method in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/RootControlNode.kt`
- [ ] T008 [US3] Modify RootControlNode.pauseAll() to call registry?.pauseAll() in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/RootControlNode.kt`
- [ ] T009 [US3] Add resumeAll() method to RootControlNode that updates model state and calls registry?.resumeAll() in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/RootControlNode.kt`
- [ ] T010 [US3] Modify RootControlNode.stopAll() to call registry?.stopAll() in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/RootControlNode.kt`
- [ ] T011 [US3] Add registry property to NodeRuntime base class in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/NodeRuntime.kt`
- [ ] T012 [US3] Modify NodeRuntime.start() to register with registry in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/NodeRuntime.kt`
- [ ] T013 [US3] Modify NodeRuntime.stop() to unregister from registry in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/NodeRuntime.kt`
- [ ] T014 [US3] Add integration tests for runtime registration flow in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/RuntimeRegistrationTest.kt`

**Checkpoint**: RuntimeRegistry integration complete - runtimes register/unregister on lifecycle events

---

## Phase 4: User Story 4 - Pause-Aware Processing Loops (Priority: P2)

**Goal**: All runtime processing loops honor PAUSED state and wait for resume

**Independent Test**: Start a generator, pause it, verify no new emissions occur, resume, confirm emissions continue

### Implementation for User Story 4

- [ ] T015 [P] [US4] Add pause hook to GeneratorRuntime processing loop in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/GeneratorRuntime.kt`
- [ ] T016 [P] [US4] Add pause hook to SinkRuntime processing loop in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/SinkRuntime.kt`
- [ ] T017 [P] [US4] Verify pause hook exists in TransformerRuntime in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/TransformerRuntime.kt`
- [ ] T018 [P] [US4] Verify pause hook exists in FilterRuntime in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/FilterRuntime.kt`
- [ ] T019 [P] [US4] Add pause hook to Out2GeneratorRuntime in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/Out2GeneratorRuntime.kt`
- [ ] T020 [P] [US4] Add pause hook to Out3GeneratorRuntime in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/Out3GeneratorRuntime.kt`
- [ ] T021 [P] [US4] Add pause hook to In2SinkRuntime in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In2SinkRuntime.kt`
- [ ] T022 [P] [US4] Add pause hook to In3SinkRuntime in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In3SinkRuntime.kt`
- [ ] T023 [P] [US4] Verify pause hook in In2Out1Runtime in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In2Out1Runtime.kt`
- [ ] T024 [P] [US4] Verify pause hook in In3Out1Runtime in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In3Out1Runtime.kt`
- [ ] T025 [P] [US4] Verify/add pause hook in In1Out2Runtime in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In1Out2Runtime.kt`
- [ ] T026 [P] [US4] Verify/add pause hook in In1Out3Runtime in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In1Out3Runtime.kt`
- [ ] T027 [P] [US4] Verify/add pause hook in In2Out2Runtime in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In2Out2Runtime.kt`
- [ ] T028 [P] [US4] Verify/add pause hook in In2Out3Runtime in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In2Out3Runtime.kt`
- [ ] T029 [P] [US4] Verify/add pause hook in In3Out2Runtime in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In3Out2Runtime.kt`
- [ ] T030 [P] [US4] Verify/add pause hook in In3Out3Runtime in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In3Out3Runtime.kt`
- [ ] T031 [US4] Add pause/resume behavior tests in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/PauseResumeTest.kt`

**Checkpoint**: All runtime classes honor PAUSED state - processing loops wait when paused

---

## Phase 5: User Story 2 - Centralized Execution Control (Priority: P1)

**Goal**: UI controls route through RootControlNode for centralized state management

**Independent Test**: Verify button presses trigger RootControlNode methods and state propagates to all nodes

### Implementation for User Story 2

- [ ] T032 [US2] Add pause() method to StopWatchControllerInterface in `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/viewmodel/StopWatchControllerInterface.kt`
- [ ] T033 [US2] Add resume() method to StopWatchControllerInterface in `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/viewmodel/StopWatchControllerInterface.kt`
- [ ] T034 [US2] Update StopWatchController to own RuntimeRegistry in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchController.kt`
- [ ] T035 [US2] Update StopWatchController.start() to use RootControlNode.startAll() in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchController.kt`
- [ ] T036 [US2] Implement StopWatchController.pause() using RootControlNode.pauseAll() in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchController.kt`
- [ ] T037 [US2] Implement StopWatchController.resume() using RootControlNode.resumeAll() in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchController.kt`
- [ ] T038 [US2] Update StopWatchController.stop() to use RootControlNode.stopAll() in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchController.kt`
- [ ] T039 [US2] Update StopWatchControllerAdapter to implement pause/resume in `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/viewmodel/StopWatchControllerAdapter.kt`
- [ ] T040 [US2] Add pause() method to StopWatchViewModel in `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/viewmodel/StopWatchViewModel.kt`
- [ ] T041 [US2] Add resume() method to StopWatchViewModel in `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/viewmodel/StopWatchViewModel.kt`
- [ ] T042 [US2] Add unit tests for ViewModel pause/resume in `KMPMobileApp/src/commonTest/kotlin/io/codenode/mobileapp/viewmodel/StopWatchViewModelTest.kt`

**Checkpoint**: All execution control routes through RootControlNode

---

## Phase 6: User Story 1 - Pause/Resume UI Control (Priority: P1)

**Goal**: Pause/Resume button in StopWatch UI that freezes and resumes timer display

**Independent Test**: Start timer, press Pause, observe frozen display, press Resume, confirm timer continues from paused value

### Implementation for User Story 1

- [ ] T043 [US1] Add Pause button to StopWatch composable (visible when RUNNING) in `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatch.kt`
- [ ] T044 [US1] Add Resume button to StopWatch composable (visible when PAUSED) in `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatch.kt`
- [ ] T045 [US1] Wire Pause button onClick to viewModel.pause() in `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatch.kt`
- [ ] T046 [US1] Wire Resume button onClick to viewModel.resume() in `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatch.kt`
- [ ] T047 [US1] Update Start/Stop button enabled state based on executionState in `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatch.kt`
- [ ] T048 [US1] Update Reset button enabled state (disabled when RUNNING) in `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatch.kt`
- [ ] T049 [US1] Verify pause freezes analog clock hands display
- [ ] T050 [US1] Verify resume continues analog clock from paused position

**Checkpoint**: UI pause/resume fully functional

---

## Phase 7: User Story 5 - Execution State Inheritance (Priority: P2)

**Goal**: CodeNodes inherit execution state from RootControlNode by default unless independentControl=true

**Independent Test**: Create flow with multiple nodes, change root state, verify all child nodes (without independentControl) reflect new state

### Implementation for User Story 5

- [ ] T051 [US5] Verify CodeNode has independentControl property in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt`
- [ ] T052 [US5] Update RuntimeRegistry.pauseAll() to respect independentControl flag in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/RuntimeRegistry.kt`
- [ ] T053 [US5] Update RuntimeRegistry.resumeAll() to respect independentControl flag in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/RuntimeRegistry.kt`
- [ ] T054 [US5] Add tests for independentControl=true behavior in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/IndependentControlTest.kt`
- [ ] T055 [US5] Add tests for nested GraphNode state propagation in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/IndependentControlTest.kt`

**Checkpoint**: State inheritance respects independentControl flag

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Integration verification and final validation

- [ ] T056 Run all fbpDsl tests with `./gradlew :fbpDsl:jvmTest`
- [ ] T057 Run StopWatch tests with `./gradlew :StopWatch:jvmTest`
- [ ] T058 Run KMPMobileApp tests with `./gradlew :KMPMobileApp:testDebugUnitTest`
- [ ] T059 Build Android app with `./gradlew :KMPMobileApp:assembleDebug`
- [ ] T060 Manual verification: Start → Pause → verify frozen → Resume → verify continues
- [ ] T061 Manual verification: Start → Pause → Reset → verify returns to 00:00
- [ ] T062 Manual verification: Stop/Start cycle works correctly (addresses original bug)
- [ ] T063 Update quickstart.md validation checklist in `specs/019-flowgraph-execution-control/quickstart.md`
- [ ] T064 Code review for thread-safety in RuntimeRegistry usage

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 3 (Phase 3)**: Depends on Foundational - RuntimeRegistry must exist first
- **User Story 4 (Phase 4)**: Can start after Phase 3 - needs RuntimeRegistry for tests
- **User Story 2 (Phase 5)**: Depends on US3 and US4 - needs registry and pause hooks
- **User Story 1 (Phase 6)**: Depends on US2 - needs ViewModel pause/resume methods
- **User Story 5 (Phase 7)**: Can run parallel with US1/US2 after US3
- **Polish (Phase 8)**: Depends on all user stories complete

### User Story Dependencies

```
Foundational (RuntimeRegistry)
       |
       v
    US3 (Runtime Registration) ──────┐
       |                              |
       v                              v
    US4 (Pause Hooks)             US5 (State Inheritance)
       |
       v
    US2 (Centralized Control)
       |
       v
    US1 (UI Controls)
```

### Parallel Opportunities

**Within Phase 4 (US4)**: All pause hook tasks (T015-T030) can run in parallel - different files, no dependencies

**After Phase 3**: US4 and US5 can start in parallel

---

## Implementation Strategy

### MVP First (Core Functionality)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (RuntimeRegistry)
3. Complete Phase 3: US3 (Runtime Registration)
4. Complete Phase 4: US4 (Pause Hooks)
5. Complete Phase 5: US2 (Centralized Control)
6. Complete Phase 6: US1 (UI Controls)
7. **STOP and VALIDATE**: Test full pause/resume flow
8. Deploy/demo if ready

### Optional Enhancement

9. Complete Phase 7: US5 (State Inheritance with independentControl)
10. Complete Phase 8: Polish

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Pause hook pattern: `while (executionState == ExecutionState.PAUSED) { delay(10) }`
- After pause loop: check `if (executionState == ExecutionState.IDLE) break`
- RuntimeRegistry uses ConcurrentHashMap for thread-safety
- Commit after each task or logical group
