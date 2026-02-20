# Tasks: Generate FlowGraph ViewModel

**Input**: Design documents from `/specs/022-generate-flowgraph-viewmodel/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/generator-api.md, quickstart.md

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add build dependency required for ViewModel in StopWatch module

- [x] T001 Add `implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")` to commonMain dependencies in StopWatch/build.gradle.kts

---

## Phase 2: User Story 1 - Move ViewModel to StopWatch Module (Priority: P1)

**Goal**: Move ViewModel, ControllerInterface, and ControllerAdapter from KMPMobileApp into StopWatch module. Tests pass with current property names (elapsedSeconds, elapsedMinutes).

**Independent Test**: `./gradlew :StopWatch:compileKotlinJvm :KMPMobileApp:compileKotlinDesktop` compiles with zero errors. Moved test files execute in StopWatch test source set.

### Implementation for User Story 1

- [x] T002 [P] [US1] Move StopWatchControllerInterface.kt from KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/viewmodel/ to StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchControllerInterface.kt — update package to `io.codenode.stopwatch.generated`
- [x] T003 [P] [US1] Move StopWatchControllerAdapter.kt from KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/viewmodel/ to StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchControllerAdapter.kt — update package to `io.codenode.stopwatch.generated`, update import for StopWatchController
- [x] T004 [P] [US1] Move StopWatchViewModel.kt from KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/viewmodel/ to StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchViewModel.kt — update package to `io.codenode.stopwatch.generated`
- [x] T005 [P] [US1] Move FakeStopWatchController.kt from KMPMobileApp/src/commonTest/kotlin/io/codenode/mobileapp/viewmodel/ to StopWatch/src/commonTest/kotlin/io/codenode/stopwatch/viewmodel/FakeStopWatchController.kt — update package to `io.codenode.stopwatch.viewmodel`, update import for StopWatchControllerInterface to `io.codenode.stopwatch.generated`
- [x] T006 [P] [US1] Move StopWatchViewModelTest.kt from KMPMobileApp/src/commonTest/kotlin/io/codenode/mobileapp/viewmodel/ to StopWatch/src/commonTest/kotlin/io/codenode/stopwatch/viewmodel/StopWatchViewModelTest.kt — update package to `io.codenode.stopwatch.viewmodel`, update imports for generated classes to `io.codenode.stopwatch.generated`
- [x] T007 [P] [US1] Update imports in KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/App.kt — change `io.codenode.mobileapp.viewmodel.StopWatchControllerAdapter` to `io.codenode.stopwatch.generated.StopWatchControllerAdapter` and `io.codenode.mobileapp.viewmodel.StopWatchViewModel` to `io.codenode.stopwatch.generated.StopWatchViewModel`
- [x] T008 [P] [US1] Update imports in KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatch.kt — change `io.codenode.mobileapp.viewmodel.StopWatchViewModel` to `io.codenode.stopwatch.generated.StopWatchViewModel`
- [x] T009 [US1] Delete old viewmodel directories: KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/viewmodel/ and KMPMobileApp/src/commonTest/kotlin/io/codenode/mobileapp/viewmodel/
- [x] T010 [US1] Verify build and tests pass after move: `./gradlew :StopWatch:jvmTest :KMPMobileApp:compileKotlinIosX64`

**Checkpoint**: StopWatch module is self-contained with ViewModel layer. KMPMobileApp references classes via StopWatch dependency. All 13 existing ViewModel tests pass in StopWatch module.

---

## Phase 3: User Story 2 - Generate ControllerInterface from FlowGraph (Priority: P2)

**Goal**: (1) Rename hand-written property names to match port-derived names (elapsedSeconds → seconds, elapsedMinutes → minutes). (2) Implement `generateControllerInterfaceClass()` in ModuleGenerator that produces output matching the updated hand-written interface.

**Independent Test**: Generator output compiles and matches the updated hand-written ControllerInterface. All tests pass with port-derived property names.

### Prerequisite Renames for User Story 2

- [x] T011 [P] [US2] Update port dataType from `Any::class` to `Int::class` for all DisplayReceiver and TimerEmitter ports in kotlinCompiler/src/jvmMain/kotlin/io/codenode/kotlincompiler/tools/RegenerateStopWatch.kt
- [x] T012 [P] [US2] Update port dataType from `Any::class` to `Int::class` for all DisplayReceiver and TimerEmitter ports in StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/StopWatch.flow.kt
- [x] T013 [US2] Rename StateFlow properties in StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/DisplayReceiverComponent.kt — `displayedSecondsFlow` → `secondsFlow`, `displayedMinutesFlow` → `minutesFlow`, `_displayedSeconds` → `_seconds`, `_displayedMinutes` → `_minutes`. Also update any tests in StopWatch/src/commonTest/ that directly reference these component properties.
- [x] T014 [US2] Update StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchController.kt — rename properties `elapsedSeconds` → `seconds`, `elapsedMinutes` → `minutes`; change wiring from `flow.timerEmitter.elapsedSecondsFlow` → `flow.displayReceiver.secondsFlow` and `flow.timerEmitter.elapsedMinutesFlow` → `flow.displayReceiver.minutesFlow`
- [x] T015 [P] [US2] Rename `elapsedSeconds` → `seconds` and `elapsedMinutes` → `minutes` in StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchControllerInterface.kt
- [x] T016 [P] [US2] Rename `elapsedSeconds` → `seconds` and `elapsedMinutes` → `minutes` in StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchControllerAdapter.kt
- [x] T017 [P] [US2] Rename `elapsedSeconds` → `seconds` and `elapsedMinutes` → `minutes` in StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchViewModel.kt
- [x] T018 [P] [US2] Rename `elapsedSeconds` → `seconds` and `elapsedMinutes` → `minutes` (MutableStateFlow fields, property declarations, helper methods) in StopWatch/src/commonTest/kotlin/io/codenode/stopwatch/viewmodel/FakeStopWatchController.kt
- [x] T019 [P] [US2] Rename all `elapsedSeconds` → `seconds` and `elapsedMinutes` → `minutes` references in test assertions in StopWatch/src/commonTest/kotlin/io/codenode/stopwatch/viewmodel/StopWatchViewModelTest.kt
- [x] T020 [P] [US2] Update property names `viewModel.elapsedSeconds` → `viewModel.seconds` and `viewModel.elapsedMinutes` → `viewModel.minutes` in KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatch.kt
- [x] T021 [US2] Verify build and tests pass after all renames: `./gradlew :StopWatch:jvmTest :KMPMobileApp:compileKotlinIosX64`

### Generator Implementation for User Story 2

- [ ] T022 [US2] Add `SinkPortProperty` data class (propertyName, kotlinType, sinkNodeName, sinkNodeCamelCase, portId) and `collectSinkPortProperties(flowGraph)` helper method to kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ModuleGenerator.kt — see contracts/generator-api.md for algorithm
- [ ] T023 [US2] Implement `generateControllerInterfaceClass(flowGraph, packageName)` method in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ModuleGenerator.kt — generates interface with StateFlow properties from sink ports + executionState + 5 lifecycle methods. See contracts/generator-api.md for exact output format.
- [ ] T024 [US2] Update `generateControllerClass()` in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ModuleGenerator.kt — derive StateFlow property declarations from sink port properties (instead of hardcoded) using pattern `val {portName}: StateFlow<{type}> = flow.{sinkNodeCamelCase}.{portName}Flow`; generalize registry wiring to iterate all nodes
- [ ] T025 [US2] Create kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/ViewModelGeneratorTest.kt with tests: (1) single-sink ControllerInterface matches expected output, (2) multi-sink prefixed property names, (3) zero-sink produces interface with only executionState and lifecycle methods, (4) collectSinkPortProperties returns correct entries
- [ ] T026 [US2] Verify generator tests pass: `./gradlew :kotlinCompiler:jvmTest --tests "*ViewModelGeneratorTest*"`

**Checkpoint**: All property renames complete. Generator produces ControllerInterface matching the updated hand-written version. All 13 ViewModel tests + generator tests pass.

---

## Phase 4: User Story 3 - Generate ControllerAdapter from FlowGraph (Priority: P3)

**Goal**: Implement `generateControllerAdapterClass()` that produces a ControllerAdapter implementing the ControllerInterface by delegating to Controller.

**Independent Test**: Generated Adapter compiles against generated Interface and Controller. All delegation methods verified in tests.

### Implementation for User Story 3

- [ ] T027 [US3] Implement `generateControllerAdapterClass(flowGraph, packageName)` method in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ModuleGenerator.kt — generates class implementing ControllerInterface, delegates StateFlow properties and lifecycle methods to Controller. See contracts/generator-api.md for exact output format.
- [ ] T028 [US3] Add ControllerAdapter generation tests to kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/ViewModelGeneratorTest.kt — (1) single-sink adapter matches expected output, (2) multi-sink adapter with prefixed properties
- [ ] T029 [US3] Verify generator tests pass: `./gradlew :kotlinCompiler:jvmTest --tests "*ViewModelGeneratorTest*"`

**Checkpoint**: ControllerAdapter generation verified. Output matches hand-written adapter pattern.

---

## Phase 5: User Story 4 - Generate ViewModel from FlowGraph (Priority: P4)

**Goal**: Implement `generateViewModelClass()` that produces a ViewModel extending platform ViewModel base class, accepting ControllerInterface as constructor dependency.

**Independent Test**: Generated ViewModel compiles and passes existing StopWatchViewModelTest.

### Implementation for User Story 4

- [ ] T030 [US4] Implement `generateViewModelClass(flowGraph, packageName)` method in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ModuleGenerator.kt — generates class extending ViewModel(), assigns StateFlow properties from controller, delegates lifecycle methods. See contracts/generator-api.md for exact output format.
- [ ] T031 [US4] Add ViewModel generation tests to kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/ViewModelGeneratorTest.kt — (1) single-sink ViewModel matches expected output, (2) multi-sink ViewModel with prefixed properties
- [ ] T032 [US4] Verify generator tests pass: `./gradlew :kotlinCompiler:jvmTest --tests "*ViewModelGeneratorTest*"`

**Checkpoint**: All three generator methods implemented and tested. Each produces output matching the hand-written StopWatch equivalents.

---

## Phase 6: User Story 5 - Identify Undefined Inputs (Priority: P5)

**Goal**: Document all metadata gaps between the current FlowGraph schema and what the generator needs, with recommended conventions for each gap.

**Independent Test**: Document lists all gaps with recommended conventions. No gaps left undocumented.

### Implementation for User Story 5

- [ ] T033 [US5] Create specs/022-generate-flowgraph-viewmodel/undefined-inputs.md documenting: (1) port data types default to Any::class, (2) component StateFlow naming convention {portName}Flow is not enforced, (3) reset behavior is hardcoded, (4) no port filtering mechanism for excluding internal ports, (5) StateFlow initial values not derivable from FlowGraph. Include recommended convention or DSL extension for each gap.

**Checkpoint**: Analysis document complete. All metadata gaps identified with mitigations.

---

## Phase 7: Polish & Integration

**Purpose**: Wire generators into module generation pipeline and verify end-to-end

- [ ] T034 Update `generateModule()` method in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ModuleGenerator.kt to include ControllerInterface, ControllerAdapter, and ViewModel in the generated file list
- [ ] T035 Update kotlinCompiler/src/jvmMain/kotlin/io/codenode/kotlincompiler/tools/RegenerateStopWatch.kt to generate ControllerInterface, ControllerAdapter, and ViewModel files alongside Controller and Flow
- [ ] T036 Run full build and test verification: `./gradlew build`
- [ ] T037 Validate quickstart.md scenarios match actual implementation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - start immediately
- **US1 (Phase 2)**: Depends on Setup (T001) - moves files with current property names
- **US2 (Phase 3)**: Depends on US1 completion - renames properties then implements generator
- **US3 (Phase 4)**: Depends on US2 (T022 collectSinkPortProperties helper)
- **US4 (Phase 5)**: Depends on US2 (T022 collectSinkPortProperties helper)
- **US5 (Phase 6)**: Independent of US2-US4, but best done after observing generation gaps
- **Polish (Phase 7)**: Depends on US2, US3, US4 completion

### User Story Dependencies

- **US1 (P1)**: Can start after Setup - no dependencies on other stories
- **US2 (P2)**: Depends on US1 (files must be moved before renaming)
- **US3 (P3)**: Depends on US2 (T022 helper + T023 interface method exist)
- **US4 (P4)**: Depends on US2 (T022 helper exists). Can run in parallel with US3.
- **US5 (P5)**: No code dependencies. Can be written any time after US2 analysis.

### Within Each User Story

- Prerequisite renames before generator implementation (US2)
- Implementation before tests
- Build verification checkpoint at end of each story

### Parallel Opportunities

- **US1**: T002-T008 can all run in parallel (different files)
- **US2 renames**: T011-T012 parallel; T015-T020 parallel (after T013-T014)
- **US3 + US4**: Can run in parallel after US2 completes (both only need the shared helper)
- **US2 generator**: T022 → T023 + T024 sequential (helper first, then methods that use it)

---

## Parallel Example: User Story 1

```bash
# Launch all file moves in parallel (different source and destination files):
Task: "Move StopWatchControllerInterface.kt to StopWatch/generated/"
Task: "Move StopWatchControllerAdapter.kt to StopWatch/generated/"
Task: "Move StopWatchViewModel.kt to StopWatch/generated/"
Task: "Move FakeStopWatchController.kt to StopWatch/viewmodel/"
Task: "Move StopWatchViewModelTest.kt to StopWatch/viewmodel/"
Task: "Update App.kt imports"
Task: "Update StopWatch.kt imports"
# Then sequentially:
Task: "Delete old viewmodel directories"
Task: "Verify build"
```

## Parallel Example: User Story 2 (Renames)

```bash
# Launch port type updates in parallel:
Task: "Update RegenerateStopWatch.kt port types"
Task: "Update StopWatch.flow.kt port types"
# Then component + controller (sequential):
Task: "Rename DisplayReceiverComponent StateFlows"
Task: "Update StopWatchController wiring"
# Then all ViewModel layer renames in parallel:
Task: "Rename ControllerInterface properties"
Task: "Rename ControllerAdapter properties"
Task: "Rename ViewModel properties"
Task: "Rename FakeStopWatchController properties"
Task: "Rename StopWatchViewModelTest assertions"
Task: "Update KMPMobileApp StopWatch.kt property names"
# Then verify:
Task: "Verify build and tests"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001)
2. Complete Phase 2: US1 Move (T002-T010)
3. **STOP and VALIDATE**: StopWatch module self-contained, KMPMobileApp builds, all tests pass
4. This proves the module structure works before any generator changes

### Incremental Delivery

1. Setup + US1 → Module restructure validated (MVP)
2. US2 Renames → Port-derived property names across all files
3. US2 Generator → ControllerInterface generation verified
4. US3 → ControllerAdapter generation verified
5. US4 → ViewModel generation verified
6. US5 → Metadata gap analysis documented
7. Polish → Full pipeline integration

### Sequential Execution (Single Developer)

1. T001 → T010 (Setup + US1): Move files, verify build
2. T011 → T026 (US2): Rename properties, implement ControllerInterface generator
3. T027 → T029 (US3): Implement ControllerAdapter generator
4. T030 → T032 (US4): Implement ViewModel generator
5. T033 (US5): Write analysis document
6. T034 → T037 (Polish): Integration + verification

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US1 moves files with CURRENT names (elapsedSeconds). US2 renames to port-derived names (seconds).
- All generator methods use the existing `buildString` pattern in ModuleGenerator
- See contracts/generator-api.md for exact expected output of each generator method
- Property rename affects 7+ files — do all renames before build verification
- Commit after each checkpoint to enable rollback
