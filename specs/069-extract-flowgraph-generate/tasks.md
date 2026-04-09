# Tasks: Extract flowGraph-generate Module

**Input**: Design documents from `/specs/069-extract-flowgraph-generate/`
**Prerequisites**: plan.md (required), spec.md (required), research.md

**Tests**: TDD tests required per spec FR-013 and constitution principle II.

**Organization**: Tasks follow the established 8-phase Strangler Fig pattern from features 065-068, organized by user story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Module Initialization)

**Purpose**: Create flowGraph-generate KMP module structure and build configuration

- [X] T001 Create flowGraph-generate module directory structure: `flowGraph-generate/src/{commonMain,commonTest,jvmMain,jvmTest}/kotlin/io/codenode/flowgraphgenerate/`
- [X] T002 Create `flowGraph-generate/build.gradle.kts` with KMP configuration — dependencies: `:fbpDsl`, `:flowGraph-types`, `:flowGraph-persist`, `:flowGraph-inspect`, kotlinPoet, coroutines.core, serialization.json, kotlin-compiler-embeddable (jvmMain only), kotlin-test + junit5 (test)
- [X] T003 Update `settings.gradle.kts` — add `include(":flowGraph-generate")` (keep `:kotlinCompiler` for now during Strangler Fig)

**Checkpoint**: Empty module compiles — `./gradlew :flowGraph-generate:compileKotlinJvm`

---

## Phase 2: Copy Files (Strangler Fig — Duplicate)

**Purpose**: Copy all source and test files to the new module, update package declarations. Originals remain in place.

### kotlinCompiler commonMain (30 files)

- [X] T004 [P] Copy 27 generator files from `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/` to `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/` — update package to `io.codenode.flowgraphgenerate.generator` and all internal `io.codenode.kotlincompiler` imports
- [X] T005 [P] Copy 8 template files from `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/templates/` to `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/templates/` — update package and imports
- [X] T006 [P] Copy `LicenseValidator.kt` from `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/validator/` to `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/validator/` — update package and imports

### kotlinCompiler commonTest (23 files)

- [X] T007 [P] Copy 18 generator test files from `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/` to `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/` — update package and imports
- [X] T008 [P] Copy 3 contract test files from `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/contract/` to `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/contract/` — update package and imports
- [X] T009 [P] Copy `PropertyCodeGenTest.kt` from `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/integration/` to `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/integration/` — update package and imports
- [X] T010 [P] Copy `LicenseValidationTest.kt` from `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/validator/` to `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/validator/` — update package and imports

### kotlinCompiler jvmTest (2 characterization tests)

- [X] T011 [P] Copy `CodeGenerationCharacterizationTest.kt` and `FlowKtGeneratorCharacterizationTest.kt` from `kotlinCompiler/src/jvmTest/kotlin/characterization/` to `flowGraph-generate/src/jvmTest/kotlin/io/codenode/flowgraphgenerate/characterization/` — update package and imports

### graphEditor generate-bucket files (6 files → jvmMain)

- [X] T012 [P] Copy `IPGeneratorViewModel.kt` and `NodeGeneratorViewModel.kt` from `graphEditor/src/jvmMain/kotlin/viewmodel/` to `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/viewmodel/` — update package and imports
- [X] T013 [P] Copy `CompilationService.kt`, `CompilationValidator.kt`, `RequiredPropertyValidator.kt` from `graphEditor/src/jvmMain/kotlin/compilation/` to `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/compilation/` — update package and imports
- [X] T014 [P] Copy `ModuleSaveService.kt` from `graphEditor/src/jvmMain/kotlin/save/` to `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/save/` — update package and imports

### Move DemoProject tool scripts

- [X] T015 Move `RegenerateStopWatch.kt` from `kotlinCompiler/src/jvmMain/kotlin/io/codenode/kotlincompiler/tools/` to `CodeNodeIO-DemoProject/tools/src/jvmMain/kotlin/io/codenode/demoproject/tools/` — updated package to `io.codenode.demoproject.tools`, imports to `io.codenode.flowgraphgenerate`
- [X] T016 Move `GenerateGeoLocationModule.kt` from `kotlinCompiler/src/jvmMain/kotlin/io/codenode/kotlincompiler/` to `CodeNodeIO-DemoProject/tools/src/jvmMain/kotlin/io/codenode/demoproject/tools/` — updated package and imports similarly

**Checkpoint**: New module compiles alongside originals — `./gradlew :flowGraph-generate:compileKotlinJvm :flowGraph-generate:jvmTest`

---

## Phase 3: User Story 2 — TDD Tests for CodeNodes (Priority: P2)

**Goal**: Write failing tests for both CodeNodeDefinitions before implementation

**Independent Test**: Tests compile but fail until CodeNode implementations exist

### TDD Tests

- [X] T017 [P] [US2] Write TDD tests for GenerateContextAggregatorCodeNode in `flowGraph-generate/src/jvmTest/kotlin/io/codenode/flowgraphgenerate/node/GenerateContextAggregatorCodeNodeTest.kt` — test port signatures (2 inputs: flowGraphModel, serializedOutput; 1 output: generationContext), anyInput=true, category=TRANSFORMER, runtime creation, basic data flow
- [X] T018 [P] [US2] Write TDD tests for FlowGraphGenerateCodeNode in `flowGraph-generate/src/jvmTest/kotlin/io/codenode/flowgraphgenerate/node/FlowGraphGenerateCodeNodeTest.kt` — test port signatures (3 inputs: generationContext, nodeDescriptors, ipTypeMetadata; 1 output: generatedOutput), anyInput=true, category=TRANSFORMER, runtime creation, basic data flow

**Checkpoint**: Tests compile but fail — `./gradlew :flowGraph-generate:jvmTest` shows expected failures

---

## Phase 4: User Story 2 — CodeNode Implementation (Priority: P2)

**Goal**: Implement both CodeNodeDefinitions using existing runtime types

- [X] T019 [P] [US2] Implement GenerateContextAggregatorCodeNode in `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/node/GenerateContextAggregatorCodeNode.kt` — object implementing CodeNodeDefinition, uses CodeNodeFactory.createIn2AnyOut1Processor, combines flowGraphModel + serializedOutput into generationContext JSON string
- [X] T020 [P] [US2] Implement FlowGraphGenerateCodeNode in `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/node/FlowGraphGenerateCodeNode.kt` — object implementing CodeNodeDefinition, uses CodeNodeFactory.createIn3AnyOut1Processor, takes generationContext + nodeDescriptors + ipTypeMetadata → produces generatedOutput

**Checkpoint**: All TDD tests pass — `./gradlew :flowGraph-generate:jvmTest`

---

## Phase 5: User Story 1 — Migrate Consumers (Priority: P1)

**Goal**: Switch all consumers to import from `io.codenode.flowgraphgenerate` instead of `io.codenode.kotlincompiler` or graphEditor generate packages

### Build dependency updates

- [ ] T021 [US1] Update `graphEditor/build.gradle.kts` — replace `project(":kotlinCompiler")` with `project(":flowGraph-generate")`
- [ ] T022 [US1] Update `idePlugin/build.gradle.kts` — replace `project(":kotlinCompiler")` with `project(":flowGraph-generate")`

### graphEditor import migrations

- [ ] T023 [US1] Search all graphEditor source files for imports of `io.codenode.kotlincompiler` — update each to `io.codenode.flowgraphgenerate`. Key files: Main.kt and any files importing generator/template/validator classes
- [ ] T024 [US1] Update `graphEditor/src/jvmMain/kotlin/ui/IPGeneratorPanel.kt` — change ViewModel import from graphEditor's viewmodel package to `io.codenode.flowgraphgenerate.viewmodel.IPGeneratorViewModel`
- [ ] T025 [US1] Update `graphEditor/src/jvmMain/kotlin/ui/NodeGeneratorPanel.kt` — change ViewModel import from graphEditor's viewmodel package to `io.codenode.flowgraphgenerate.viewmodel.NodeGeneratorViewModel`
- [ ] T026 [US1] Search graphEditor for imports of `compilation.CompilationService`, `compilation.CompilationValidator`, `compilation.RequiredPropertyValidator`, `save.ModuleSaveService` — update to `io.codenode.flowgraphgenerate.compilation.*` and `io.codenode.flowgraphgenerate.save.*`

### idePlugin import migrations

- [ ] T027 [US1] Search all idePlugin source files for imports of `io.codenode.kotlincompiler` — update each to `io.codenode.flowgraphgenerate`

### Other module migrations

- [ ] T028 [US1] Search entire codebase (excluding kotlinCompiler and flowGraph-generate) for any remaining references to `io.codenode.kotlincompiler` — update all found references

**Checkpoint**: Both graphEditor and idePlugin compile with new imports — `./gradlew :graphEditor:compileKotlinJvm :idePlugin:compileKotlin`

---

## Phase 6: User Story 1 — Remove Originals (Priority: P1)

**Goal**: Delete original files and the kotlinCompiler module

- [ ] T029 [US1] Remove the 6 original graphEditor generate-bucket files: `graphEditor/src/jvmMain/kotlin/viewmodel/IPGeneratorViewModel.kt`, `graphEditor/src/jvmMain/kotlin/viewmodel/NodeGeneratorViewModel.kt`, `graphEditor/src/jvmMain/kotlin/compilation/CompilationService.kt`, `graphEditor/src/jvmMain/kotlin/compilation/CompilationValidator.kt`, `graphEditor/src/jvmMain/kotlin/compilation/RequiredPropertyValidator.kt`, `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt`
- [ ] T030 [US1] Delete the entire `kotlinCompiler/` directory
- [ ] T031 [US1] Update `settings.gradle.kts` — remove `include(":kotlinCompiler")`
- [ ] T032 [US1] Verify zero references to `io.codenode.kotlincompiler` remain outside `flowGraph-generate` — search entire codebase

**Checkpoint**: Full project compiles and all tests pass — `./gradlew check`

---

## Phase 7: User Story 3 — Architecture Wiring (Priority: P3)

**Goal**: Update architecture.flow.kt with two-node sub-graph for flowGraph-generate

- [ ] T033 [US3] Update the generate graphNode in `graphEditor/architecture.flow.kt` — add child codeNode "GenerateContextAggregator" (TRANSFORMER) with inputs flowGraphModel, serializedOutput and output generationContext
- [ ] T034 [US3] Add child codeNode "FlowGraphGenerate" (TRANSFORMER) with inputs generationContext, nodeDescriptors, ipTypeMetadata and output generatedOutput to the generate graphNode in `graphEditor/architecture.flow.kt`
- [ ] T035 [US3] Add port mappings to generate graphNode in `graphEditor/architecture.flow.kt` — map 4 exposed inputs to correct child codeNode ports, map exposed output to FlowGraphGenerate's generatedOutput
- [ ] T036 [US3] Add internal connection in generate graphNode: GenerateContextAggregator output "generationContext" → FlowGraphGenerate input "generationContext"
- [ ] T037 [US3] Update generate graphNode description to reflect actual file count and two-node sub-graph structure

**Checkpoint**: architecture.flow.kt parses successfully with all 20 external connections intact

---

## Phase 8: Verification & Polish

**Purpose**: Final validation across the entire project

- [ ] T038 Run full project compilation: `./gradlew compileKotlinJvm` (all modules)
- [ ] T039 Run full test suite: `./gradlew check` — all existing tests must pass
- [ ] T040 Run characterization tests specifically: `./gradlew :flowGraph-generate:jvmTest --tests "io.codenode.flowgraphgenerate.characterization.*"`
- [ ] T041 Run architecture validation: `./gradlew :graphEditor:jvmTest --tests "*ArchitectureFlowKtsTest*"` (if exists)
- [ ] T042 Verify SC-004: grep entire codebase for `io.codenode.kotlincompiler` — must return zero results outside flowGraph-generate
- [ ] T043 Verify SC-005: confirm `kotlinCompiler/` directory does not exist
- [ ] T044 Verify SC-008: confirm IPGeneratorPanel.kt and NodeGeneratorPanel.kt in graphEditor compile with flowGraph-generate imports

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Copy)**: Depends on Phase 1 — all copy tasks [P] can run in parallel
- **Phase 3 (TDD Tests)**: Depends on Phase 2 — both test tasks [P] can run in parallel
- **Phase 4 (CodeNode Impl)**: Depends on Phase 3 — both impl tasks [P] can run in parallel
- **Phase 5 (Migrate)**: Depends on Phase 2 (files exist in new location) — migration tasks are sequential (shared files)
- **Phase 6 (Remove)**: Depends on Phase 5 (consumers migrated) — sequential
- **Phase 7 (Architecture)**: Depends on Phase 4 (CodeNodes exist) and Phase 6 (module clean) — sequential
- **Phase 8 (Verify)**: Depends on all prior phases

### User Story Dependencies

- **US1 (Extract)**: Phases 1-2, 5-6 — core extraction and migration
- **US2 (CodeNodes)**: Phases 3-4 — TDD tests and implementation (can overlap with Phase 2 copy work)
- **US3 (Architecture)**: Phase 7 — depends on US1 and US2 completion

### Parallel Opportunities

- **Phase 2**: All copy tasks (T004-T016) can run in parallel — different source/target directories
- **Phase 3**: Both TDD test tasks (T017-T018) can run in parallel — different files
- **Phase 4**: Both CodeNode implementations (T019-T020) can run in parallel — different files
- **Phase 5**: T021-T022 (build files) can run in parallel; T023-T028 are sequential per module

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Copy files
3. Complete Phase 5: Migrate consumers
4. Complete Phase 6: Remove originals
5. **VALIDATE**: `./gradlew check` — all tests pass, kotlinCompiler gone

### Full Delivery

1. MVP (above) + Phase 3-4 (CodeNodes with TDD) + Phase 7 (Architecture wiring)
2. Phase 8: Final verification

---

## Notes

- The Strangler Fig pattern means originals stay in place until Phase 6. Both old and new modules coexist during Phases 2-5.
- T015-T016 (DemoProject tool scripts) may need a separate commit/PR in the CodeNodeIO-DemoProject repo.
- The 6 graphEditor files go to jvmMain (not commonMain) because they use JVM ViewModel and filesystem APIs.
- Total files moving: 65 kotlinCompiler + 6 graphEditor = 71 files. The 2 tool scripts move to DemoProject (not extracted).
