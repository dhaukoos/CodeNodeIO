# Tasks: Folder Hierarchy Migration

**Input**: Design documents from `/specs/077-folder-hierarchy-migration/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, quickstart.md

**Tests**: Compilation of demo modules serves as the test suite. No separate unit tests — generator changes are validated by end-to-end module compilation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Update ModuleSaveService constants and path construction — all generators depend on these.

**⚠️ CRITICAL**: Generator path changes depend on the new subpackage constants.

- [ ] T001 Replace `GENERATED_SUBPACKAGE = "generated"` with `FLOW_SUBPACKAGE = "flow"`, `CONTROLLER_SUBPACKAGE = "controller"`, and `VIEWMODEL_SUBPACKAGE = "viewmodel"` in `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/save/ModuleSaveService.kt`
- [ ] T002 Update `saveModule()` path construction to write .flow.kt and Flow.kt to `flow/` subpackage, Controller*.kt to `controller/` subpackage, and ViewModel.kt to `viewmodel/` subpackage in `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/save/ModuleSaveService.kt`
- [ ] T003 Update `saveFlowKtOnly()` to write .flow.kt to `flow/` subpackage instead of base package in `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/save/ModuleSaveService.kt`
- [ ] T004 Compile: `./gradlew :flowGraph-generate:compileKotlinJvm`

**Checkpoint**: ModuleSaveService writes to new paths. Individual generators can now be updated.

---

## Phase 2: User Story 1 — Update Code Generators to Write to New Paths (Priority: P1) 🎯 MVP

**Goal**: All generators produce files with correct package declarations and import statements for the new folder hierarchy. No files written to `generated/` or base package root.

**Independent Test**: Generate a new module via "Generate Module". Verify output has flow/, controller/, viewmodel/, userInterface/ subdirectories. Verify compilation.

### Implementation

- [ ] T005 [P] [US1] Update `RuntimeFlowGenerator.generate()` — change package declaration from `generatedPackage` to flow subpackage, update any internal imports in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeFlowGenerator.kt`
- [ ] T006 [P] [US1] Update `RuntimeControllerGenerator.generate()` — change package to controller subpackage, update import of Flow.kt to `{basePackage}.flow.{Name}Flow` in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeControllerGenerator.kt`
- [ ] T007 [P] [US1] Update `RuntimeControllerInterfaceGenerator.generate()` — change package to controller subpackage in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeControllerInterfaceGenerator.kt`
- [ ] T008 [P] [US1] Update `RuntimeControllerAdapterGenerator.generate()` — change package to controller subpackage, update import of ControllerInterface to `{basePackage}.controller` in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeControllerAdapterGenerator.kt`
- [ ] T009 [US1] Update `RuntimeViewModelGenerator.generate()` — change package from base to viewmodel subpackage, update imports of ControllerInterface and ControllerAdapter to `{basePackage}.controller` in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeViewModelGenerator.kt`
- [ ] T010 [US1] Update `UserInterfaceStubGenerator.generate()` — update import of ViewModel to `{basePackage}.viewmodel.{Name}ViewModel` in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UserInterfaceStubGenerator.kt`
- [ ] T011 [US1] Update `EntityModuleGenerator.generateModule()` — change output paths for converters (to persistence/), .flow.kt (to flow/), runtime files (to flow/ and controller/), ViewModel (to viewmodel/) in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/EntityModuleGenerator.kt`
- [ ] T012 [US1] Update `EntityConverterGenerator` — verify or update package declaration to persistence subpackage in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/EntityModuleGenerator.kt`
- [ ] T013 [US1] Update `EntityPersistenceGenerator` — verify package declaration uses persistence subpackage in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/EntityPersistenceGenerator.kt`
- [ ] T014 [US1] Compile and run existing tests: `./gradlew :flowGraph-generate:jvmTest`

**Checkpoint**: All generators produce files with correct new packages and imports. New modules use the new layout exclusively.

---

## Phase 3: User Story 2 — Migrate Existing Demo Project Modules (Priority: P2)

**Goal**: Move files in the 6 demo project modules from the flat layout to the new hierarchy. All modules compile after migration.

**Independent Test**: Run `./gradlew :StopWatch:compileKotlinJvm :UserProfiles:compileKotlinJvm :Addresses:compileKotlinJvm :EdgeArtFilter:compileKotlinJvm :WeatherForecast:compileKotlinJvm` in the Demo Project.

### Implementation

- [ ] T015 [US2] Migrate StopWatch module: create flow/, controller/, viewmodel/ directories; move .flow.kt and Flow.kt to flow/, Controller*.kt to controller/ (from generated/), ViewModel.kt to viewmodel/; update package declarations and imports; delete generated/ in `CodeNodeIO-DemoProject/StopWatch/`
- [ ] T016 [US2] Compile StopWatch: `cd CodeNodeIO-DemoProject && ./gradlew :StopWatch:compileKotlinJvm`
- [ ] T017 [US2] Migrate UserProfiles module: same pattern as StopWatch plus move Persistence.kt and Converters.kt to persistence/ if not already there; update all package/import references in `CodeNodeIO-DemoProject/UserProfiles/`
- [ ] T018 [US2] Compile UserProfiles: `cd CodeNodeIO-DemoProject && ./gradlew :UserProfiles:compileKotlinJvm`
- [ ] T019 [US2] Migrate Addresses module: same pattern as UserProfiles (has persistence files) in `CodeNodeIO-DemoProject/Addresses/`
- [ ] T020 [US2] Compile Addresses: `cd CodeNodeIO-DemoProject && ./gradlew :Addresses:compileKotlinJvm`
- [ ] T021 [US2] Migrate EdgeArtFilter module: same pattern as StopWatch (no persistence) in `CodeNodeIO-DemoProject/EdgeArtFilter/`
- [ ] T022 [US2] Compile EdgeArtFilter: `cd CodeNodeIO-DemoProject && ./gradlew :EdgeArtFilter:compileKotlinJvm`
- [ ] T023 [US2] Migrate WeatherForecast module: same pattern as StopWatch (no persistence) in `CodeNodeIO-DemoProject/WeatherForecast/`
- [ ] T024 [US2] Compile WeatherForecast: `cd CodeNodeIO-DemoProject && ./gradlew :WeatherForecast:compileKotlinJvm`
- [ ] T025 [US2] Migrate TestModule: move ViewModel.kt and State.kt to viewmodel/ if not already there; update imports in `CodeNodeIO-DemoProject/TestModule/`
- [ ] T026 [US2] Compile TestModule: `cd CodeNodeIO-DemoProject && ./gradlew :TestModule:compileDebugKotlinAndroid`

**Checkpoint**: All 6 demo modules compile with the new layout. No `generated/` directories remain.

---

## Phase 4: User Story 3 — Update Discovery and Scanning (Priority: P3)

**Goal**: ModuleSessionFactory resolves classes from new packages. Graph editor discovers nodes and previews from migrated modules.

**Independent Test**: Launch graph editor, open a migrated module's flow graph. Verify nodes appear in palette. Run Runtime Preview.

### Implementation

- [ ] T027 [US3] Update `ModuleSessionFactory` class loading to try `{modulePackage}.controller.{Name}ControllerInterface` first, falling back to `{modulePackage}.generated.{Name}ControllerInterface` for backward compatibility in `flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/ModuleSessionFactory.kt`
- [ ] T028 [US3] Update `ModuleSessionFactory` ViewModel resolution to try `{modulePackage}.viewmodel.{Name}ViewModel` first, falling back to `{modulePackage}.{Name}ViewModel` in `flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/ModuleSessionFactory.kt`
- [ ] T029 [US3] Verify `GraphEditorApp.kt` scanning for `nodes/` and `userInterface/` directories uses name-based matching that works with the new layout (no changes expected) in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorApp.kt`
- [ ] T030 [US3] Verify `DynamicPreviewDiscovery` continues to find preview providers in `userInterface/` (no changes expected) in `flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/discovery/DynamicPreviewDiscovery.kt`
- [ ] T031 [US3] Compile and run all tests: `./gradlew :flowGraph-execute:compileKotlinJvm :graphEditor:compileKotlinJvm :graphEditor:jvmTest`

**Checkpoint**: Runtime class resolution works with both old and new layouts. Graph editor discovers all nodes and previews.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Full verification across all quickstart scenarios

- [ ] T032 Run full test suite: `./gradlew :flowGraph-generate:jvmTest :graphEditor:jvmTest`
- [ ] T033 Run quickstart.md verification scenarios VS1–VS11
- [ ] T034 Verify no `generated/` directories remain in migrated demo modules: `find CodeNodeIO-DemoProject -name "generated" -type d`
- [ ] T035 Verify user-authored files are unchanged: compare UI composable files before and after migration

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: No dependencies — updates ModuleSaveService constants. BLOCKS US1.
- **User Story 1 (Phase 2)**: Depends on Foundational — generators need new constants. BLOCKS US2.
- **User Story 2 (Phase 3)**: Depends on US1 — migration validates generator output matches expected layout.
- **User Story 3 (Phase 4)**: Depends on US2 — scanning validation needs migrated modules.
- **Polish (Phase 5)**: Depends on all user stories.

### Within Each Phase

- US1: Generators T005–T008 can run in parallel (different files). T009–T013 sequential (ViewModel refs controller, entity generators depend on save service).
- US2: Each module migration is independent BUT sequential compile verification needed per module.
- US3: T027–T028 (ModuleSessionFactory) before T029–T030 (verification).

### Parallel Opportunities

```text
# US1 generator updates (4 runtime generators — different files):
T005 (RuntimeFlowGenerator)
T006 (RuntimeControllerGenerator)
T007 (RuntimeControllerInterfaceGenerator)
T008 (RuntimeControllerAdapterGenerator)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Foundational (T001–T004)
2. Complete Phase 2: User Story 1 (T005–T014)
3. **STOP and VALIDATE**: Generate a new module, verify new layout, compile
4. This is independently valuable — new modules use the correct hierarchy

### Incremental Delivery

1. Foundational → ModuleSaveService constants updated
2. User Story 1 → Generators write to new paths (MVP!)
3. User Story 2 → Demo modules migrated and compiling
4. User Story 3 → Runtime scanning works with new layout
5. Polish → Full verification pass

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Demo module migration (US2) is performed on the CodeNodeIO-DemoProject repository, not CodeNodeIO
- User-authored files (UI composables, custom CodeNodes) are NOT moved — only imports updated
- The `generated/` directory is replaced by `flow/` + `controller/` — no other subdirectories change
- ModuleSessionFactory dual-layout fallback ensures backward compatibility during transition
- Commit after each phase completion
