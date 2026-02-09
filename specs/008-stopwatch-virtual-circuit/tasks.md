# Tasks: StopWatch Virtual Circuit Demo

**Input**: Design documents from `/specs/008-stopwatch-virtual-circuit/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, quickstart.md

**Tests**: TDD approach - tests are included per constitution requirement (Test-Driven Development mandatory).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- **KMPMobileApp**: `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/`
- **StopWatch module**: `StopWatch/src/commonMain/kotlin/io/codenode/generated/stopwatch/`
- **Demo assets**: `demos/stopwatch/`
- **Tests**: `*/src/commonTest/kotlin/` or `*/src/jvmTest/kotlin/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and demo asset directories

- [ ] T001 Create demos/stopwatch/ directory for FlowGraph storage
- [ ] T002 [P] Verify existing graphEditor compiles with `./gradlew :graphEditor:build`
- [ ] T003 [P] Verify existing kotlinCompiler compiles with `./gradlew :kotlinCompiler:build`
- [ ] T004 [P] Verify KMPMobileApp compiles with `./gradlew :KMPMobileApp:build`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T005 Verify ModuleGenerator can generate StopWatchController wrapper in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ModuleGenerator.kt
- [ ] T006 [P] Verify RootControlNode.startAll(), stopAll(), pauseAll() work correctly in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/RootControlNode.kt
- [ ] T007 [P] Verify Port type compatibility checking supports Int type in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Port.kt
- [ ] T008 [P] Verify FlowGraph serialization/deserialization for .flow.kts files in fbpDsl

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - StopWatch FlowGraph Creation (Priority: P1) 🎯 MVP

**Goal**: Developer can create a FlowGraph with TimerEmitter and DisplayReceiver nodes in graphEditor

**Independent Test**: Create FlowGraph in graphEditor, save as .flow.kts, reload and verify structure

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T009 [P] [US1] TDD test: FlowGraph with name "StopWatch" can be created in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/StopWatchFlowGraphTest.kt
- [ ] T010 [P] [US1] TDD test: CodeNode with 0 inputs and 2 Int outputs validates successfully in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/StopWatchFlowGraphTest.kt
- [ ] T011 [P] [US1] TDD test: CodeNode with 2 Int inputs and 0 outputs validates successfully in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/StopWatchFlowGraphTest.kt
- [ ] T012 [P] [US1] TDD test: Connections between Int ports validate successfully in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/StopWatchFlowGraphTest.kt
- [ ] T013 [P] [US1] TDD test: FlowGraph serializes to .flow.kts and deserializes correctly in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/serialization/StopWatchSerializationTest.kt

### Implementation for User Story 1

- [ ] T014 [US1] Create StopWatch.flow.kts FlowGraph definition file in demos/stopwatch/StopWatch.flow.kts per data-model.md
- [ ] T015 [US1] Define TimerEmitter CodeNode with outputPorts [elapsedSeconds: Int, elapsedMinutes: Int] in demos/stopwatch/StopWatch.flow.kts
- [ ] T016 [US1] Define DisplayReceiver CodeNode with inputPorts [seconds: Int, minutes: Int] in demos/stopwatch/StopWatch.flow.kts
- [ ] T017 [US1] Add connections: TimerEmitter.elapsedSeconds → DisplayReceiver.seconds, TimerEmitter.elapsedMinutes → DisplayReceiver.minutes in demos/stopwatch/StopWatch.flow.kts
- [ ] T018 [US1] Set controlConfig.speedAttenuation = 1000L on TimerEmitter in demos/stopwatch/StopWatch.flow.kts
- [ ] T019 [US1] Verify FlowGraph loads in graphEditor by running `./gradlew :graphEditor:run` and opening the file

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - KMP Module Generation from FlowGraph (Priority: P1)

**Goal**: FlowGraph compiles into a valid KMP module with Controller and FlowGraph classes

**Independent Test**: Use Compile button to generate module, verify directory structure and compilation

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T020 [P] [US2] TDD test: ModuleGenerator produces StopWatchFlowGraph.kt file in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/StopWatchModuleGeneratorTest.kt
- [ ] T021 [P] [US2] TDD test: ModuleGenerator produces StopWatchController.kt with start(), stop(), pause(), getStatus() methods in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/StopWatchModuleGeneratorTest.kt
- [ ] T022 [P] [US2] TDD test: StopWatchController.bindToLifecycle() pauses on ON_PAUSE and resumes on ON_RESUME in StopWatch/src/commonTest/kotlin/io/codenode/generated/stopwatch/LifecycleBindingTest.kt
- [ ] T023 [P] [US2] TDD test: Generated build.gradle.kts has correct KMP configuration (Kotlin 2.1.21, Compose 1.7.3) in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/StopWatchModuleGeneratorTest.kt
- [ ] T024 [P] [US2] TDD test: Generated module compiles with `./gradlew :StopWatch:build` in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/StopWatchModuleGeneratorTest.kt

### Implementation for User Story 2

- [ ] T025 [US2] Generate StopWatch/ module directory from demos/stopwatch/StopWatch.flow.kts using graphEditor Compile button
- [ ] T026 [US2] Add lifecycle-runtime-compose:2.9.6 dependency to generated StopWatch/build.gradle.kts
- [ ] T027 [US2] Verify generated StopWatchFlowGraph.kt instantiates TimerEmitter and DisplayReceiver nodes in StopWatch/src/commonMain/kotlin/io/codenode/generated/stopwatch/StopWatchFlowGraph.kt
- [ ] T028 [US2] Verify generated StopWatchController.kt wraps RootControlNode with start(), stop(), pause(), reset(), getStatus() in StopWatch/src/commonMain/kotlin/io/codenode/generated/stopwatch/StopWatchController.kt
- [ ] T029 [US2] Add StateFlow properties for elapsedSeconds, elapsedMinutes, executionState to StopWatchController in StopWatch/src/commonMain/kotlin/io/codenode/generated/stopwatch/StopWatchController.kt
- [ ] T030 [US2] Implement bindToLifecycle(lifecycle: Lifecycle) with wasRunningBeforePause tracking in StopWatch/src/commonMain/kotlin/io/codenode/generated/stopwatch/StopWatchController.kt
- [ ] T031 [US2] Add include(":StopWatch") to settings.gradle.kts at repository root
- [ ] T032 [US2] Verify generated module compiles with `./gradlew :StopWatch:build`

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - StopWatch Composable Integration (Priority: P1)

**Goal**: Refactored StopWatch composable uses generated module, replacing LaunchedEffect with RootControlNode

**Independent Test**: Run KMPMobileApp on Android/iOS, verify start/stop/reset controls timer correctly

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T033 [P] [US3] TDD test: Start button calls controller.start() and transitions to RUNNING in KMPMobileApp/src/commonTest/kotlin/io/codenode/mobileapp/StopWatchIntegrationTest.kt
- [ ] T034 [P] [US3] TDD test: Stop button calls controller.stop() and transitions to IDLE in KMPMobileApp/src/commonTest/kotlin/io/codenode/mobileapp/StopWatchIntegrationTest.kt
- [ ] T035 [P] [US3] TDD test: Reset button calls controller.reset() and resets elapsedSeconds/elapsedMinutes to 0 in KMPMobileApp/src/commonTest/kotlin/io/codenode/mobileapp/StopWatchIntegrationTest.kt
- [ ] T036 [P] [US3] TDD test: isRunning derived from executionState == RUNNING in KMPMobileApp/src/commonTest/kotlin/io/codenode/mobileapp/StopWatchIntegrationTest.kt

### Implementation for User Story 3

- [ ] T037 [US3] Add implementation(project(":StopWatch")) to KMPMobileApp/build.gradle.kts commonMain dependencies
- [ ] T038 [US3] Extract StopWatchFace composable to separate file KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatchFace.kt
- [ ] T039 [US3] Move secondsToRad() helper function to StopWatchFace.kt in KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatchFace.kt
- [ ] T040 [US3] Refactor StopWatch.kt to import StopWatchController from generated module in KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatch.kt
- [ ] T041 [US3] Replace isRunning mutableStateOf with executionState from controller in KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatch.kt
- [ ] T042 [US3] Replace elapsedSeconds/elapsedMinutes mutableStateOf with StateFlow.collectAsState() from controller in KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatch.kt
- [ ] T043 [US3] Replace LaunchedEffect timer logic with controller.start()/stop() calls in KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatch.kt
- [ ] T044 [US3] Implement reset button to call controller.reset() in KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatch.kt
- [ ] T045 [US3] (Optional) Add LaunchedEffect to call controller.bindToLifecycle(LocalLifecycleOwner.current.lifecycle) in KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatch.kt
- [ ] T046 [US3] Verify refactored KMPMobileApp compiles with `./gradlew :KMPMobileApp:build`
- [ ] T047 [US3] Manual test: Run on Android emulator, verify Start/Stop/Reset work correctly

**Checkpoint**: At this point, User Stories 1, 2, AND 3 should all work independently

---

## Phase 6: User Story 4 - UseCase Mapping (Priority: P2)

**Goal**: TimerEmitter and DisplayReceiver UseCases correctly map to original composable logic

**Independent Test**: Verify generated UseCase stubs have correct signatures matching original functions

### Tests for User Story 4

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T048 [P] [US4] TDD test: TimerEmitter emits incrementing elapsedSeconds every speedAttenuation ms in StopWatch/src/commonTest/kotlin/io/codenode/generated/stopwatch/TimerEmitterComponentTest.kt
- [ ] T049 [P] [US4] TDD test: TimerEmitter rolls elapsedSeconds to 0 and increments elapsedMinutes at 60 in StopWatch/src/commonTest/kotlin/io/codenode/generated/stopwatch/TimerEmitterComponentTest.kt
- [ ] T050 [P] [US4] TDD test: TimerEmitter stops emitting when executionState != RUNNING in StopWatch/src/commonTest/kotlin/io/codenode/generated/stopwatch/TimerEmitterComponentTest.kt
- [ ] T051 [P] [US4] TDD test: DisplayReceiver updates state when receiving seconds/minutes inputs in StopWatch/src/commonTest/kotlin/io/codenode/generated/stopwatch/DisplayReceiverComponentTest.kt

### Implementation for User Story 4

- [ ] T052 [US4] Implement TimerEmitter.process() with coroutine tick loop in StopWatch/src/commonMain/kotlin/io/codenode/generated/stopwatch/TimerEmitterComponent.kt
- [ ] T053 [US4] Add seconds/minutes state variables with rollover logic (seconds >= 60) in StopWatch/src/commonMain/kotlin/io/codenode/generated/stopwatch/TimerEmitterComponent.kt
- [ ] T054 [US4] Add delay(controlConfig.speedAttenuation) for tick interval in StopWatch/src/commonMain/kotlin/io/codenode/generated/stopwatch/TimerEmitterComponent.kt
- [ ] T055 [US4] Add executionState check in while loop condition in StopWatch/src/commonMain/kotlin/io/codenode/generated/stopwatch/TimerEmitterComponent.kt
- [ ] T056 [US4] Implement DisplayReceiver.process() to update StateFlow values in StopWatch/src/commonMain/kotlin/io/codenode/generated/stopwatch/DisplayReceiverComponent.kt
- [ ] T057 [US4] Wire TimerEmitter outputs to DisplayReceiver inputs via Channel in StopWatch/src/commonMain/kotlin/io/codenode/generated/stopwatch/StopWatchFlowGraph.kt
- [ ] T058 [US4] Verify all StopWatch module tests pass with `./gradlew :StopWatch:test`

**Checkpoint**: All user stories should now be independently functional

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T059 [P] Add KDoc documentation to StopWatchController public methods (including bindToLifecycle) in StopWatch/src/commonMain/kotlin/io/codenode/generated/stopwatch/StopWatchController.kt
- [ ] T060 [P] Add KDoc documentation to StopWatchFlowGraph in StopWatch/src/commonMain/kotlin/io/codenode/generated/stopwatch/StopWatchFlowGraph.kt
- [ ] T061 [P] Verify timer accuracy is within ±100ms tolerance (SC-006) with performance test in StopWatch/src/commonTest/kotlin/io/codenode/generated/stopwatch/TimerAccuracyTest.kt
- [ ] T062 [P] Verify lifecycle binding pauses timer when app backgrounded on Android in manual test
- [ ] T063 Run full test suite with `./gradlew test`
- [ ] T064 Run quickstart.md validation steps manually
- [ ] T065 Manual test on iOS simulator (if Xcode available)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-6)**: All depend on Foundational phase completion
  - US1, US2, US3 are all P1 priority and should be done sequentially
  - US4 (P2) can be done after US1-US3 are complete
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - Creates FlowGraph definition
- **User Story 2 (P1)**: Depends on US1 completion - Needs FlowGraph to generate module
- **User Story 3 (P1)**: Depends on US2 completion - Needs generated module to integrate
- **User Story 4 (P2)**: Can start after US2 - Implements UseCase logic (parallel with US3 if desired)

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Models/entities before services
- Services before integration
- Core implementation before wiring
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- All tests within a user story marked [P] can run in parallel
- US4 implementation can run in parallel with US3 (after US2 completes)
- All Polish tasks marked [P] can run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "TDD test: FlowGraph with name StopWatch can be created" [T009]
Task: "TDD test: CodeNode with 0 inputs and 2 Int outputs validates" [T010]
Task: "TDD test: CodeNode with 2 Int inputs and 0 outputs validates" [T011]
Task: "TDD test: Connections between Int ports validate" [T012]
Task: "TDD test: FlowGraph serializes to .flow.kts and deserializes" [T013]

# Then sequential implementation:
Task: "Create StopWatch.flow.kts FlowGraph definition file" [T014]
Task: "Define TimerEmitter CodeNode" [T015]
# ... etc
```

---

## Parallel Example: User Story 4

```bash
# Launch all tests for User Story 4 together:
Task: "TDD test: TimerEmitter emits incrementing elapsedSeconds" [T045]
Task: "TDD test: TimerEmitter rolls elapsedSeconds at 60" [T046]
Task: "TDD test: TimerEmitter stops when executionState != RUNNING" [T047]
Task: "TDD test: DisplayReceiver updates state on input" [T048]

# Then sequential implementation following test results
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2 + 3)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (FlowGraph creation)
4. Complete Phase 4: User Story 2 (Module generation)
5. Complete Phase 5: User Story 3 (Composable integration)
6. **STOP and VALIDATE**: Test full integration on Android
7. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test FlowGraph creation → Validate DSL
3. Add User Story 2 → Test module generation → Validate compilation
4. Add User Story 3 → Test integration → Deploy/Demo (MVP!)
5. Add User Story 4 → Test UseCase logic → Full functionality
6. Each story adds value without breaking previous stories

### Suggested MVP Scope

**MVP = User Stories 1 + 2 + 3** (all P1 priority)

This delivers:
- Visual FlowGraph creation
- KMP module generation
- Working stopwatch with virtual circuit architecture

User Story 4 (P2) adds polished UseCase implementations but MVP works with generated stubs + composable state management.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
