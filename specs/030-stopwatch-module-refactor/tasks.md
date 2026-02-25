# Tasks: StopWatch Module Refactoring

**Input**: Design documents from `/specs/030-stopwatch-module-refactor/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, quickstart.md

**Tests**: No test tasks — this is a refactoring with existing tests that must continue to pass.

**Organization**: Tasks are grouped by user story. US1 (rename modules) must complete before US2 (move UI files), and US3 (KMPMobileApp validation) depends on both.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: Prepare the workspace for module renaming

- [ ] T001 Ensure StopWatch3/ is tracked in git (`git add StopWatch3/`)

**Checkpoint**: StopWatch3 files are tracked and ready for rename operations

---

## Phase 2: User Story 1 - Rename StopWatch Modules (Priority: P1) 🎯 MVP

**Goal**: Rename StopWatch → StopWatchOriginal, rename StopWatch3 → StopWatch with internal package/class renames from `stopwatch3` to `stopwatch`

**Independent Test**: `./gradlew :StopWatchOriginal:compileKotlinJvm` and `./gradlew :StopWatch:compileKotlinJvm` both succeed

### Phase 2a: Rename StopWatch → StopWatchOriginal

- [ ] T002 [US1] Rename directory `StopWatch/` → `StopWatchOriginal/`
- [ ] T003 [US1] Update `StopWatchOriginal/settings.gradle.kts`: rootProject.name = "StopWatchOriginal"
- [ ] T004 [US1] Update root `settings.gradle.kts`: change `include(":StopWatch")` → `include(":StopWatchOriginal")`
- [ ] T005 [US1] Verify: `./gradlew :StopWatchOriginal:compileKotlinJvm` succeeds

### Phase 2b: Rename StopWatch3 → StopWatch (directory and config)

- [ ] T006 [US1] Rename directory `StopWatch3/` → `StopWatch/`
- [ ] T007 [US1] Update `StopWatch/settings.gradle.kts`: rootProject.name = "StopWatch"
- [ ] T008 [US1] Add `include(":StopWatch")` to root `settings.gradle.kts`

### Phase 2c: Rename source directories (stopwatch3 → stopwatch)

- [ ] T009 [US1] Rename `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch3/` → `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/` (and any other source sets: androidMain, iosMain, jvmMain)

### Phase 2d: Rename generated files and update all packages/classes

- [ ] T010 [P] [US1] Rename generated files: StopWatch3Flow.kt → StopWatchFlow.kt, StopWatch3Controller.kt → StopWatchController.kt, StopWatch3ControllerInterface.kt → StopWatchControllerInterface.kt, StopWatch3ControllerAdapter.kt → StopWatchControllerAdapter.kt, StopWatch3ViewModel.kt → StopWatchViewModel.kt in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/`
- [ ] T011 [P] [US1] Rename flow DSL file: `StopWatch/StopWatch3.flow.kt` → `StopWatch/StopWatch.flow.kt`
- [ ] T012 [US1] Update all package declarations in all source files: `io.codenode.stopwatch3.*` → `io.codenode.stopwatch.*` (generated/, processingLogic/, stateProperties/, and flow DSL)
- [ ] T013 [US1] Update all class/interface names: `StopWatch3Flow` → `StopWatchFlow`, `StopWatch3Controller` → `StopWatchController`, `StopWatch3ControllerInterface` → `StopWatchControllerInterface`, `StopWatch3ControllerAdapter` → `StopWatchControllerAdapter`, `StopWatch3ViewModel` → `StopWatchViewModel` in all source files
- [ ] T014 [US1] Update all internal imports and cross-references: `io.codenode.stopwatch3.*` → `io.codenode.stopwatch.*`, string literals `"StopWatch3Controller"` → `"StopWatchController"`, `"StopWatch3"` → `"StopWatch"`, variable `stopWatch3FlowGraph` → `stopWatchFlowGraph`
- [ ] T015 [US1] Update `StopWatch/build.gradle.kts`: baseName = "StopWatch", namespace = "io.codenode.generated.StopWatch"
- [ ] T016 [US1] Verify: `./gradlew :StopWatch:compileKotlinJvm` succeeds

**Checkpoint**: Both StopWatchOriginal and StopWatch modules compile independently. No references to `stopwatch3` remain in StopWatch module.

---

## Phase 3: User Story 2 - Move UI Files to StopWatch Module (Priority: P1)

**Goal**: Create userInterface folder in StopWatch module, move StopWatch.kt and StopWatchFace.kt from KMPMobileApp, add Compose dependencies

**Independent Test**: `./gradlew :StopWatch:compileKotlinJvm` succeeds with the UI composables

### Implementation for User Story 2

- [ ] T017 [US2] Create directory `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/userInterface/`
- [ ] T018 [US2] Move `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatch.kt` → `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/userInterface/StopWatch.kt`
- [ ] T019 [US2] Move `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatchFace.kt` → `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/userInterface/StopWatchFace.kt`
- [ ] T020 [US2] Update package declarations in moved files: `package io.codenode.mobileapp` → `package io.codenode.stopwatch.userInterface`
- [ ] T021 [US2] Add Compose Multiplatform dependencies to `StopWatch/build.gradle.kts` (compose plugin, runtime, foundation, material3, ui)
- [ ] T022 [US2] Verify: `./gradlew :StopWatch:compileKotlinJvm` succeeds with userInterface files

**Checkpoint**: StopWatch module contains both runtime code and UI composables, compiles successfully

---

## Phase 4: User Story 3 - KMPMobileApp Continues to Function (Priority: P1)

**Goal**: Update KMPMobileApp to reference moved UI files and verify all functionality works identically

**Independent Test**: `./gradlew :KMPMobileApp:compileKotlinAndroid` succeeds and all tests pass

### Implementation for User Story 3

- [ ] T023 [US3] Update `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/App.kt`: add import `io.codenode.stopwatch.userInterface.StopWatch`
- [ ] T024 [US3] Update `KMPMobileApp/src/androidMain/kotlin/io/codenode/mobileapp/StopWatchPreview.kt`: add import for StopWatch composable from userInterface if needed
- [ ] T025 [US3] Verify: `./gradlew :KMPMobileApp:compileKotlinAndroid` succeeds
- [ ] T026 [US3] Verify: all existing tests pass (`./gradlew :KMPMobileApp:testDebugUnitTest`)

**Checkpoint**: KMPMobileApp builds and operates identically to pre-refactoring behavior

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Full validation across all modules

- [ ] T027 Verify no references to `stopwatch3` remain in StopWatch module (grep for `stopwatch3` and `StopWatch3`)
- [ ] T028 Run quickstart.md verification checklist (all 10 items)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **User Story 1 (Phase 2)**: Depends on Setup — BLOCKS User Stories 2 and 3
- **User Story 2 (Phase 3)**: Depends on User Story 1 completion (needs renamed StopWatch module)
- **User Story 3 (Phase 4)**: Depends on User Story 2 completion (needs UI files moved)
- **Polish (Phase 5)**: Depends on all user stories being complete

### Within Each User Story

- Phase 2a (rename StopWatch → StopWatchOriginal) must complete before Phase 2b
- Phase 2b (rename StopWatch3 → StopWatch) must complete before Phase 2c
- Phase 2c (rename source dirs) must complete before Phase 2d
- T012-T014 are sequential (packages before classes before cross-references)
- T017 must complete before T018-T019 (create dir before moving files)

### Parallel Opportunities

- T010 and T011 can run in parallel (renaming different files)
- T018 and T019 can run in parallel (moving different files) but both depend on T017

---

## Implementation Strategy

### Sequential Delivery (Required for This Feature)

This refactoring has strict sequential dependencies — each phase builds on the previous:

1. Complete Phase 1: Setup (git track StopWatch3)
2. Complete Phase 2: Rename modules (StopWatch → StopWatchOriginal, StopWatch3 → StopWatch)
3. Complete Phase 3: Move UI files to StopWatch/userInterface
4. Complete Phase 4: Update KMPMobileApp references
5. Complete Phase 5: Full validation

Each phase has a compile checkpoint to verify correctness before proceeding.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- This is a pure refactoring — no new code logic, only renames and moves
- Compile checkpoints after each phase catch errors early
- All 3 user stories are P1 but have strict sequential dependencies
- Total: 28 tasks across 5 phases
