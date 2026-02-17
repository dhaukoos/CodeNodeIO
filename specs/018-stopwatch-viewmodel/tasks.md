# Tasks: StopWatch ViewModel Pattern

**Input**: Design documents from `/specs/018-stopwatch-viewmodel/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Tests**: Tests are included as this feature's success criteria (SC-002) require ViewModel unit tests without Compose UI dependencies.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **KMPMobileApp module**: `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/`
- **ViewModel package**: `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/viewmodel/`
- **Test files**: `KMPMobileApp/src/commonTest/kotlin/io/codenode/mobileapp/viewmodel/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add ViewModel dependency and create package structure

- [x] T001 Add `lifecycle-viewmodel-compose:2.8.0` dependency to `KMPMobileApp/build.gradle.kts` commonMain.dependencies
- [x] T002 Add `kotlinx-coroutines-test:1.8.0` dependency to `KMPMobileApp/build.gradle.kts` commonTest.dependencies
- [x] T003 Create `viewmodel/` package directory at `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/viewmodel/`

**Checkpoint**: ✅ Foundation ready - ViewModel infrastructure in place

---

## Phase 2: User Story 1 - Clean Separation Between FlowGraph and UI (Priority: P1) 🎯 MVP

**Goal**: Create StopWatchViewModel that exposes StateFlow properties delegated from StopWatchController, enabling UI to observe state without direct controller access.

**Independent Test**: After implementation, ViewModel can be instantiated with a mock controller, and tests verify state exposure without Compose UI dependencies.

### Tests for User Story 1

- [x] T004 [P] [US1] Create `FakeStopWatchController` test double in `KMPMobileApp/src/commonTest/kotlin/io/codenode/mobileapp/viewmodel/FakeStopWatchController.kt`
- [x] T005 [P] [US1] Create `StopWatchViewModelTest.kt` with initial state test at `KMPMobileApp/src/commonTest/kotlin/io/codenode/mobileapp/viewmodel/StopWatchViewModelTest.kt`

### Implementation for User Story 1

- [x] T006 [US1] Create `StopWatchViewModel` class extending ViewModel with StateFlow delegation at `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/viewmodel/StopWatchViewModel.kt`
- [x] T007 [US1] Add `elapsedSeconds: StateFlow<Int>` property delegated from controller in `StopWatchViewModel`
- [x] T008 [US1] Add `elapsedMinutes: StateFlow<Int>` property delegated from controller in `StopWatchViewModel`
- [x] T009 [US1] Add `executionState: StateFlow<ExecutionState>` property delegated from controller in `StopWatchViewModel`
- [x] T010 [US1] Add test verifying `elapsedSeconds` is exposed correctly in `StopWatchViewModelTest.kt`
- [x] T011 [US1] Add test verifying `elapsedMinutes` is exposed correctly in `StopWatchViewModelTest.kt`
- [x] T012 [US1] Add test verifying `executionState` is exposed correctly in `StopWatchViewModelTest.kt`

**Checkpoint**: ✅ User Story 1 complete - ViewModel exposes state, tests pass without Compose

---

## Phase 3: User Story 2 - Consistent Action Handling (Priority: P2)

**Goal**: Add start(), stop(), and reset() action methods to ViewModel that delegate to controller.

**Independent Test**: Tests verify action methods call correct controller methods and state updates accordingly.

### Tests for User Story 2

- [x] T013 [P] [US2] Add test for `start()` delegates to controller in `StopWatchViewModelTest.kt`
- [x] T014 [P] [US2] Add test for `stop()` delegates to controller in `StopWatchViewModelTest.kt`
- [x] T015 [P] [US2] Add test for `reset()` delegates to controller in `StopWatchViewModelTest.kt`

### Implementation for User Story 2

- [x] T016 [US2] Implement `start()` method delegating to `controller.start()` in `StopWatchViewModel`
- [x] T017 [US2] Implement `stop()` method delegating to `controller.stop()` in `StopWatchViewModel`
- [x] T018 [US2] Implement `reset()` method delegating to `controller.reset()` in `StopWatchViewModel`

**Checkpoint**: ✅ User Story 2 complete - All actions delegate to controller correctly

---

## Phase 4: User Story 3 - Platform-Agnostic ViewModel (Priority: P3)

**Goal**: Refactor StopWatch composable to use ViewModel, verify multiplatform compatibility.

**Independent Test**: Stopwatch app runs on Android with ViewModel, tests pass on JVM.

### Implementation for User Story 3

- [x] T019 [US3] Update `StopWatch.kt` composable to accept `StopWatchViewModel` parameter at `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatch.kt`
- [x] T020 [US3] Replace direct controller state access with `viewModel.elapsedSeconds.collectAsState()` in `StopWatch.kt`
- [x] T021 [US3] Replace direct controller state access with `viewModel.elapsedMinutes.collectAsState()` in `StopWatch.kt`
- [x] T022 [US3] Replace direct controller state access with `viewModel.executionState.collectAsState()` in `StopWatch.kt`
- [x] T023 [US3] Replace controller action calls with ViewModel actions (start, stop, reset) in `StopWatch.kt`
- [x] T024 [US3] Update `App.kt` to create StopWatchViewModel and pass to StopWatch composable at `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/App.kt`
- [x] T025 [US3] Verify tests pass on JVM: `./gradlew :KMPMobileApp:test`
- [x] T026 [US3] Verify Android build succeeds: `./gradlew :KMPMobileApp:assembleDebug`

**Checkpoint**: ✅ User Story 3 complete - ViewModel works on all platforms

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and documentation

- [ ] T027 [P] Add test for rapid start/stop/reset sequence (edge case) in `StopWatchViewModelTest.kt`
- [ ] T028 [P] Add test for late subscription scenario (edge case) in `StopWatchViewModelTest.kt`
- [ ] T029 Verify all tests pass without Compose UI imports (SC-002)
- [ ] T030 Verify existing functionality works identically (SC-003) - manual testing
- [ ] T031 Run quickstart.md validation checklist at `specs/018-stopwatch-viewmodel/quickstart.md`

**Checkpoint**: ✅ All success criteria validated

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **User Story 1 (Phase 2)**: Depends on Setup completion
- **User Story 2 (Phase 3)**: Depends on User Story 1 (needs ViewModel class to add actions)
- **User Story 3 (Phase 4)**: Depends on User Story 2 (needs complete ViewModel)
- **Polish (Phase 5)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Creates ViewModel with state properties - MVP
- **User Story 2 (P2)**: Adds action methods to ViewModel from US1
- **User Story 3 (P3)**: Uses complete ViewModel to refactor UI

### Parallel Opportunities

- T004 and T005 can run in parallel (different test files)
- T007, T008, T009 could be combined into single T006 task (same file)
- T013, T014, T015 can run in parallel (independent test methods)
- T027 and T028 can run in parallel (independent tests)

---

## Parallel Example: User Story 1 Tests

```bash
# Launch all test setup tasks together:
Task: "Create FakeStopWatchController test double"
Task: "Create StopWatchViewModelTest.kt with initial state test"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (add dependencies)
2. Complete Phase 2: User Story 1 (ViewModel with state)
3. **STOP and VALIDATE**: Run tests, verify StateFlow delegation works
4. Demo/review if ready

### Incremental Delivery

1. Complete Setup → Dependencies in place
2. Add User Story 1 → ViewModel exposes state, tests pass
3. Add User Story 2 → ViewModel has actions, tests pass
4. Add User Story 3 → UI uses ViewModel, app runs on all platforms
5. Each story adds value without breaking previous stories

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- ViewModel is a thin facade - delegates directly to StopWatchController
- Tests use FakeStopWatchController to avoid FlowGraph dependency
- StopWatchFace composable unchanged - only StopWatch.kt refactored
