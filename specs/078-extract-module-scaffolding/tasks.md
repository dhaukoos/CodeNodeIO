# Tasks: Module Scaffolding Extraction

**Input**: Design documents from `/specs/078-extract-module-scaffolding/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, quickstart.md

**Tests**: Unit tests included for the new ModuleScaffoldingGenerator (spec FR-008 requires isolated testability).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Phase 1: User Story 1 — Extract ModuleScaffoldingGenerator (Priority: P1) 🎯 MVP

**Goal**: Create a standalone `ModuleScaffoldingGenerator` that creates KMP module directory structure and Gradle files given a module name, output directory, and target platforms. No FlowGraph dependency.

**Independent Test**: Call `ModuleScaffoldingGenerator.generate("TestModule", tempDir, listOf(KMP_ANDROID, KMP_IOS))`. Verify directory structure, Gradle files, and all subdirectories (flow/, controller/, viewmodel/, userInterface/, nodes/, iptypes/).

### Tests

- [ ] T001 [P] [US1] Add test: `generate()` creates module directory with correct name in `flowGraph-generate/src/jvmTest/kotlin/io/codenode/flowgraphgenerate/save/ModuleScaffoldingGeneratorTest.kt`
- [ ] T002 [P] [US1] Add test: `generate()` creates commonMain, jvmMain, commonTest source set directories with correct package paths in `flowGraph-generate/src/jvmTest/kotlin/io/codenode/flowgraphgenerate/save/ModuleScaffoldingGeneratorTest.kt`
- [ ] T003 [P] [US1] Add test: `generate()` creates all subdirectories (flow/, controller/, viewmodel/, userInterface/, nodes/, iptypes/) in `flowGraph-generate/src/jvmTest/kotlin/io/codenode/flowgraphgenerate/save/ModuleScaffoldingGeneratorTest.kt`
- [ ] T004 [P] [US1] Add test: `generate()` creates platform-specific source sets (androidMain, iosMain) based on target platforms in `flowGraph-generate/src/jvmTest/kotlin/io/codenode/flowgraphgenerate/save/ModuleScaffoldingGeneratorTest.kt`
- [ ] T005 [P] [US1] Add test: `generate()` writes build.gradle.kts and settings.gradle.kts with write-once semantics (skip if exist) in `flowGraph-generate/src/jvmTest/kotlin/io/codenode/flowgraphgenerate/save/ModuleScaffoldingGeneratorTest.kt`
- [ ] T006 [P] [US1] Add test: `generate()` returns ScaffoldingResult with correct basePackage, subpackage paths, and filesCreated list in `flowGraph-generate/src/jvmTest/kotlin/io/codenode/flowgraphgenerate/save/ModuleScaffoldingGeneratorTest.kt`

### Implementation

- [ ] T007 [US1] Create `ScaffoldingResult` data class with moduleDir, basePackage, flowPackage, controllerPackage, viewModelPackage, userInterfacePackage, and filesCreated in `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/save/ModuleScaffoldingGenerator.kt`
- [ ] T008 [US1] Create `ModuleScaffoldingGenerator` class with `generate(moduleName, outputDir, targetPlatforms, packagePrefix, isEntityModule)` that creates module directory, all source set directories (commonMain, jvmMain, commonTest, platform-specific), all subdirectories (flow/, controller/, viewmodel/, userInterface/, nodes/, iptypes/), and writes Gradle files in `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/save/ModuleScaffoldingGenerator.kt`
- [ ] T009 [US1] Move `generateSettingsGradle()` from `ModuleSaveService` to `ModuleScaffoldingGenerator` in `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/save/ModuleScaffoldingGenerator.kt`
- [ ] T010 [US1] Run scaffolding tests: `./gradlew :flowGraph-generate:jvmTest --tests "*ModuleScaffoldingGenerator*"`

**Checkpoint**: ModuleScaffoldingGenerator creates complete module structure in isolation. All unit tests pass.

---

## Phase 2: User Story 2 — Refactor ModuleSaveService (Priority: P2)

**Goal**: Refactor `saveModule()` and `saveEntityModule()` to delegate scaffolding to the new generator. All existing tests pass — behavior-preserving.

**Independent Test**: Run `./gradlew :flowGraph-generate:jvmTest`. 100% of existing ModuleSaveServiceTest tests pass.

### Implementation

- [ ] T011 [US2] Add `ModuleScaffoldingGenerator` as a field in `ModuleSaveService` in `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/save/ModuleSaveService.kt`
- [ ] T012 [US2] Refactor `saveModule()` to call `scaffoldingGenerator.generate()` first, then use the returned ScaffoldingResult for package paths and module directory — remove inline directory creation and Gradle file writing in `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/save/ModuleSaveService.kt`
- [ ] T013 [US2] Refactor `saveEntityModule()` to call `scaffoldingGenerator.generate()` similarly — remove inline directory creation and Gradle file writing in `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/save/ModuleSaveService.kt`
- [ ] T014 [US2] Remove `createDirectoryStructure()` and `generateSettingsGradle()` private methods from `ModuleSaveService` (now in scaffolding generator) in `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/save/ModuleSaveService.kt`
- [ ] T015 [US2] Run full existing test suite: `./gradlew :flowGraph-generate:jvmTest` — verify 100% pass, zero regressions

**Checkpoint**: ModuleSaveService delegates scaffolding. All existing tests pass. Behavior identical.

---

## Phase 3: Polish & Cross-Cutting Concerns

**Purpose**: Full verification

- [ ] T016 Run full test suite: `./gradlew :flowGraph-generate:jvmTest :graphEditor:jvmTest`
- [ ] T017 Run quickstart.md verification scenarios VS1–VS5

---

## Dependencies & Execution Order

### Phase Dependencies

- **User Story 1 (Phase 1)**: No dependencies — creates new class. BLOCKS US2.
- **User Story 2 (Phase 2)**: Depends on US1 — refactoring needs the new class.
- **Polish (Phase 3)**: Depends on both user stories.

### Within Each Phase

- US1: Tests (T001–T006) in parallel, then implementation (T007–T009) sequential, then verify (T010)
- US2: Sequential — T011 → T012 → T013 → T014 → T015

### Parallel Opportunities

```text
# US1 tests (all in same test file but independent):
T001, T002, T003, T004, T005, T006
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: User Story 1 (T001–T010)
2. **STOP and VALIDATE**: Scaffolding generator works in isolation, all tests pass
3. This is independently valuable — the component exists and is tested

### Incremental Delivery

1. User Story 1 → ModuleScaffoldingGenerator exists and tested (MVP!)
2. User Story 2 → ModuleSaveService refactored to delegate
3. Polish → Full verification

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- This is a pure refactoring — no new user-facing functionality
- `generateSettingsGradle()` moves FROM ModuleSaveService TO ModuleScaffoldingGenerator
- `createDirectoryStructure()` moves FROM ModuleSaveService TO ModuleScaffoldingGenerator
- `ModuleGenerator.generateBuildGradle()` stays where it is — scaffolding generator calls it
- Commit after each phase completion
