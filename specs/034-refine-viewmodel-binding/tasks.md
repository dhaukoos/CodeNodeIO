# Tasks: Refine the ViewModel Binding

**Input**: Design documents from `/specs/034-refine-viewmodel-binding/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Tests**: Not explicitly requested. Existing test files are updated/deleted as part of the refactoring (not new tests).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: User Story 1 — Rename StopWatch to StopWatchV2 (Priority: P1)

**Goal**: Preserve the current working StopWatch module as StopWatchV2, serving as a baseline for comparison during the code generation refactoring.

**Independent Test**: Build and run KMPMobileApp with StopWatchV2 — verify the stopwatch displays, starts/stops/pauses/resets correctly.

### Implementation for User Story 1

- [ ] T001 [US1] Copy `StopWatch/` directory to `StopWatchV2/` at the project root
- [ ] T002 [US1] Update all package declarations from `io.codenode.stopwatch` to `io.codenode.stopwatchv2` in all source files under `StopWatchV2/src/`
- [ ] T003 [US1] Rename all `StopWatch`-prefixed class names, object names, and val names to `StopWatchV2` prefix in all source files under `StopWatchV2/src/` (e.g., `StopWatchFlow` → `StopWatchV2Flow`, `stopWatchFlowGraph` → `stopWatchV2FlowGraph`, `StopWatchState` or `StopWatchViewModel` → `StopWatchV2*`)
- [ ] T004 [US1] Update `StopWatchV2/build.gradle.kts` — change module name references if any, and verify Gradle plugin/dependency declarations are correct for the renamed module
- [ ] T005 [P] [US1] Update `settings.gradle.kts` (root) — replace `include(":StopWatch")` with `include(":StopWatchV2")` (keep any existing `:StopWatchOriginal` entry)
- [ ] T006 [P] [US1] Update `KMPMobileApp/build.gradle.kts` — change dependency from `project(":StopWatch")` to `project(":StopWatchV2")`
- [ ] T007 [US1] Update `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/App.kt` — change all imports from `io.codenode.stopwatch.*` to `io.codenode.stopwatchv2.*` and update class references (`StopWatchController` → `StopWatchV2Controller`, `StopWatchViewModel` → `StopWatchV2ViewModel`, `StopWatchControllerAdapter` → `StopWatchV2ControllerAdapter`, `stopWatchFlowGraph` → `stopWatchV2FlowGraph`, `StopWatchScreen` → `StopWatchV2Screen`)
- [ ] T008 [US1] Compile and verify: `./gradlew :StopWatchV2:compileKotlinJvm :KMPMobileApp:compileKotlinJvm`

**Checkpoint**: StopWatchV2 compiles and KMPMobileApp uses it successfully. Baseline preserved.

---

## Phase 2: User Story 2 — Refactor Code Generation for ViewModel as Binding Interface (Priority: P2)

**Goal**: Refactor generators to produce a ViewModel stub with marker-delineated `{ModuleName}State` object in the base package, eliminate StateProperties generation, and remove all stateProperties-related code and tests.

**Independent Test**: Run `./gradlew :kotlinCompiler:allTests :graphEditor:jvmTest` — all generator and save service tests pass with the new patterns.

### Generator Changes

- [ ] T009 [P] [US2] Refactor `RuntimeViewModelGenerator.kt` in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/` — generate ViewModel stub with: (1) marker-delineated `{ModuleName}State` object containing MutableStateFlow/StateFlow pairs derived from sink input ports via ObservableStateResolver, with `// ===== MODULE PROPERTIES START =====` and `// ===== MODULE PROPERTIES END =====` markers and `reset()` method, (2) `{ModuleName}ViewModel` class extending `ViewModel()` with state delegation from `{ModuleName}State` and control method delegation from `{ModuleName}ControllerInterface`. Add `generateModulePropertiesSection()` method for selective regeneration support. Change output target from `generated/` to base package.
- [ ] T010 [P] [US2] Refactor `RuntimeFlowGenerator.kt` in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/` — replace `statePropertiesPackage: String?` parameter with `viewModelPackage: String`, import `{ModuleName}State` from viewModelPackage, delegate StateFlow properties as `val {prop}Flow = {ModuleName}State.{prop}Flow`, update sink consume blocks to `{ModuleName}State._{prop}.value = value`, and update `reset()` to call `{ModuleName}State.reset()` instead of per-node `{NodeName}StateProperties.reset()`
- [ ] T011 [P] [US2] Refactor `ProcessingLogicStubGenerator.kt` in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/` — remove `statePropertiesPackage` parameter from `generateStub()` and `generateStubWithPreservedBody()` methods, remove all StateProperties import generation from generated stubs
- [ ] T012 [P] [US2] Delete `StatePropertiesGenerator.kt` from `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/`

### Orchestrator Changes

- [ ] T013 [US2] Refactor `ModuleSaveService.kt` in `graphEditor/src/jvmMain/kotlin/save/` — remove stateProperties directory creation (`statePropertiesDir`), remove StateProperties file generation loop (`generateStatePropertiesFiles`), remove stateProperties orphan cleanup (`deleteOrphanedStateProperties`), remove `statePropertiesGenerator` field. Move ViewModel generation from `generated/` directory to base package directory. Implement selective regeneration: if ViewModel file exists, read it, extract everything outside `MODULE PROPERTIES START/END` markers, regenerate the Module Properties section with current port definitions, reassemble the file. If markers are missing in existing file, treat as fresh generation. Pass `viewModelPackage` instead of `statePropertiesPackage` to `RuntimeFlowGenerator` and remove `statePropertiesPackage` from `ProcessingLogicStubGenerator` calls.

### Test Changes

- [ ] T014 [P] [US2] Delete `StatePropertiesGeneratorTest.kt` from `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/`
- [ ] T015 [P] [US2] Update `RuntimeViewModelGeneratorTest.kt` in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/` — test new stub structure: verify `{ModuleName}State` object generation with MutableStateFlow/StateFlow pairs from sink input ports, verify `MODULE PROPERTIES START/END` marker comments, verify `{ModuleName}ViewModel` class with state delegation from `{ModuleName}State` and control methods from `{ModuleName}ControllerInterface`, verify `reset()` method in state object, verify `generateModulePropertiesSection()` for selective regeneration
- [ ] T016 [P] [US2] Update `RuntimeFlowGeneratorTest.kt` in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/` — replace all `statePropertiesPackage` test cases with `viewModelPackage` equivalents, verify `{ModuleName}State` import generation, verify StateFlow delegation from `{ModuleName}State.{prop}Flow`, verify consume block updates via `{ModuleName}State._{prop}.value`, verify `reset()` calls `{ModuleName}State.reset()`, remove all per-node StateProperties delegation test assertions
- [ ] T017 [P] [US2] Update `ProcessingLogicStubGeneratorTest.kt` in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/` — remove all test cases that verify StateProperties import generation in stubs, remove `statePropertiesPackage` parameter from test method calls
- [ ] T018 [US2] Update `ModuleSaveServiceTest.kt` in `graphEditor/src/jvmTest/kotlin/save/` — remove StateProperties file creation assertions, remove StateProperties preservation/orphan deletion assertions, update file count expectations (no stateProperties files in totals), add tests for ViewModel selective regeneration (Module Properties section updated on re-save, user code outside markers preserved, missing markers trigger fresh generation), verify ViewModel is generated in base package directory (not `generated/`)
- [ ] T019 [US2] Compile and run all tests: `./gradlew :kotlinCompiler:allTests :graphEditor:jvmTest`

**Checkpoint**: All generators produce new patterns. StatePropertiesGenerator and its test are deleted. All existing tests updated and passing.

---

## Phase 3: User Story 3 — Create New StopWatch Module & Validate Equivalence (Priority: P3)

**Goal**: Produce a new StopWatch module using the refactored code generators and verify the KMPMobileApp functions identically to the StopWatchV2 baseline.

**Independent Test**: Build and run KMPMobileApp with the new StopWatch module — verify the stopwatch displays, starts/stops/pauses/resets identically to StopWatchV2.

### Implementation for User Story 3

- [ ] T020 [US3] Create `StopWatch/` module directory with `build.gradle.kts` (copy from `StopWatchV2/build.gradle.kts`, update module name references back to StopWatch) and base source directory at `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/`
- [ ] T021 [US3] Copy `.flow.kt` from `StopWatchV2/src/commonMain/kotlin/io/codenode/stopwatchv2/StopWatchV2.flow.kt` to `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/StopWatch.flow.kt` — update package declaration to `io.codenode.stopwatch`, rename val from `stopWatchV2FlowGraph` to `stopWatchFlowGraph`, rename flowGraph name from `"StopWatchV2"` to `"StopWatch"`, rename node references back to original names
- [ ] T022 [P] [US3] Create `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/` directory and generate the 4 runtime files using the refactored generators: `StopWatchFlow.kt` (with `StopWatchState` imports from base package), `StopWatchController.kt`, `StopWatchControllerInterface.kt`, `StopWatchControllerAdapter.kt`
- [ ] T023 [P] [US3] Create ViewModel stub `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/StopWatchViewModel.kt` using the refactored RuntimeViewModelGenerator — must contain `StopWatchState` object with `_seconds`/`secondsFlow` and `_minutes`/`minutesFlow` MutableStateFlow/StateFlow pairs between `MODULE PROPERTIES START/END` markers, plus `StopWatchViewModel` class with state delegation and control methods
- [ ] T024 [P] [US3] Create processingLogic stubs in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/processingLogic/` — `TimerEmitterProcessLogic.kt` with local `var currentSeconds`/`var currentMinutes` state and increment logic returning `ProcessResult2.both(newSeconds, newMinutes)`, and `DisplayReceiverProcessLogic.kt` as a passthrough sink tick (receives seconds and minutes, no state updates needed since Flow handles it)
- [ ] T025 [P] [US3] Copy userInterface files from `StopWatchV2/src/commonMain/kotlin/io/codenode/stopwatchv2/userInterface/` to `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/userInterface/` — update package declarations from `io.codenode.stopwatchv2` to `io.codenode.stopwatch`, rename class/val references from `StopWatchV2*` to `StopWatch*`
- [ ] T026 [US3] Update `settings.gradle.kts` (root) — add `include(":StopWatch")` alongside existing `include(":StopWatchV2")` so both modules coexist
- [ ] T027 [US3] Update `KMPMobileApp/build.gradle.kts` — change dependency from `project(":StopWatchV2")` to `project(":StopWatch")` and update `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/App.kt` — change imports from `io.codenode.stopwatchv2.*` to `io.codenode.stopwatch.*` and class references from `StopWatchV2*` to `StopWatch*`
- [ ] T028 [US3] Compile and verify: `./gradlew :StopWatch:compileKotlinJvm :KMPMobileApp:compileKotlinJvm`
- [ ] T029 [US3] Verify file structure: confirm no `stateProperties/` folder exists in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/`, confirm `StopWatchViewModel.kt` is in the base package directory (not `generated/`), confirm `generated/` contains only the 4 runtime files (Flow, Controller, ControllerInterface, ControllerAdapter)

**Checkpoint**: New StopWatch module compiles and KMPMobileApp functions equivalently to StopWatchV2 baseline. No stateProperties folder exists. ViewModel is a stub in the base package.

---

## Dependencies & Execution Order

### Phase Dependencies

- **User Story 1 (Phase 1)**: No dependencies — can start immediately
- **User Story 2 (Phase 2)**: No dependencies on US1 — can start immediately (modifies generators, not StopWatch module). Can run in parallel with US1.
- **User Story 3 (Phase 3)**: Depends on BOTH US1 (StopWatchV2 exists as baseline) and US2 (generators produce new patterns). Must complete after both.

### Within Each Phase

**US1**: T001 → T002 → T003 → T004 → (T005 ∥ T006) → T007 → T008
**US2**: (T009 ∥ T010 ∥ T011 ∥ T012) → T013 → (T014 ∥ T015 ∥ T016 ∥ T017) → T018 → T019
**US3**: T020 → T021 → (T022 ∥ T023 ∥ T024 ∥ T025) → T026 → T027 → T028 → T029

### Parallel Opportunities

- US1 and US2 can run in parallel (completely independent: different files, different modules)
- Within US2: T009/T010/T011/T012 (generator changes + delete) can all run in parallel
- Within US2: T014/T015/T016/T017 (test updates) can all run in parallel after generator changes
- Within US3: T022/T023/T024/T025 (runtime files, ViewModel, processing logic, UI) can all run in parallel

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: US1 — Rename StopWatch to StopWatchV2 (T001–T008)
2. **STOP and VALIDATE**: Build KMPMobileApp with StopWatchV2, verify identical behavior
3. Baseline is preserved

### Incremental Delivery

1. US1 (T001–T008): Rename → Baseline preserved
2. US2 (T009–T019): Refactor generators → StateProperties eliminated, tests pass
3. US3 (T020–T029): New StopWatch module → Equivalence validated

### Parallel Team Strategy

With two developers:
1. Developer A: US1 (T001–T008) — rename StopWatch
2. Developer B: US2 (T009–T019) — refactor generators
3. Both done → either developer: US3 (T020–T029) — validate

---

## Notes

- US1 and US2 are completely independent and can run in parallel
- US3 MUST wait for both US1 and US2 to complete
- All tasks within a phase marked [P] can run in parallel (different files, no dependencies)
- StatePropertiesGenerator.kt and StatePropertiesGeneratorTest.kt are DELETED (T012, T014)
- The ViewModel selective regeneration (marker-based) is the key new capability — tested in T015, T018
- Processing logic stubs in the new StopWatch (T024) must use local state instead of StateProperties references
