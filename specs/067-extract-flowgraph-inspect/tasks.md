# Tasks: flowGraph-inspect Module Extraction

**Input**: Design documents from `/specs/067-extract-flowgraph-inspect/`
**Prerequisites**: plan.md, spec.md, research.md, quickstart.md

**Tests**: TDD tests for the CodeNode are required (FR-007, Constitution Principle II).

**Organization**: Tasks follow the Strangler Fig pattern: setup → copy → TDD tests → implementation → migration → removal → architecture wiring → verification.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Module Skeleton)

**Purpose**: Create flowGraph-inspect module structure and Gradle configuration

- [X] T001 Add `include("flowGraph-inspect")` to `settings.gradle.kts` after `:flowGraph-persist`
- [X] T002 Create `flowGraph-inspect/build.gradle.kts` following the flowGraph-persist pattern: KMP module with JVM + iOS targets, depends on `:fbpDsl`, `:flowGraph-types`, and `:flowGraph-persist`, kotlinx-coroutines, kotlinx-serialization, androidx.lifecycle-viewmodel plugin, JUnit 5 for tests
- [X] T003 Create source directory structure: `flowGraph-inspect/src/commonMain/kotlin/io/codenode/flowgraphinspect/`, `flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/registry/`, `flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/viewmodel/`, `flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/discovery/`, `flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/node/`, `flowGraph-inspect/src/jvmTest/kotlin/io/codenode/flowgraphinspect/node/`
- [X] T004 Run `./gradlew :flowGraph-inspect:compileKotlinJvm` to verify empty module compiles

**Checkpoint**: Empty module compiles. Ready for file extraction.

---

## Phase 2: File Extraction (US1 — Strangler Fig Copy)

**Purpose**: Copy 7 files from graphEditor to flowGraph-inspect with updated packages

**Goal**: All 7 files compile in the new module with updated package declarations. State classes drop the `: BaseState` marker (R4).

**Independent Test**: `./gradlew :flowGraph-inspect:compileKotlinJvm` succeeds

- [X] T005 [P] [US1] Copy `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/NodeDefinitionRegistry.kt` to `flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/registry/NodeDefinitionRegistry.kt` — update package to `io.codenode.flowgraphinspect.registry`
- [X] T006 [P] [US1] Copy `graphEditor/src/jvmMain/kotlin/viewmodel/CodeEditorViewModel.kt` to `flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/viewmodel/CodeEditorViewModel.kt` — update package to `io.codenode.flowgraphinspect.viewmodel`, update NodeDefinitionRegistry import to `io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry`, remove `: BaseState` from CodeEditorState
- [X] T007 [P] [US1] Copy `graphEditor/src/jvmMain/kotlin/viewmodel/IPPaletteViewModel.kt` to `flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/viewmodel/IPPaletteViewModel.kt` — update package to `io.codenode.flowgraphinspect.viewmodel`, remove `: BaseState` from IPPaletteState
- [X] T008 [P] [US1] Copy `graphEditor/src/jvmMain/kotlin/viewmodel/GraphNodePaletteViewModel.kt` to `flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/viewmodel/GraphNodePaletteViewModel.kt` — update package to `io.codenode.flowgraphinspect.viewmodel`, remove `: BaseState` from GraphNodePaletteState
- [X] T009 [P] [US1] Copy `graphEditor/src/jvmMain/kotlin/viewmodel/NodePaletteViewModel.kt` to `flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/viewmodel/NodePaletteViewModel.kt` — update package to `io.codenode.flowgraphinspect.viewmodel`, remove `: BaseState` from NodePaletteState
- [X] T010 [P] [US1] Copy `graphEditor/src/jvmMain/kotlin/ui/ComposableDiscovery.kt` to `flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/discovery/ComposableDiscovery.kt` — update package to `io.codenode.flowgraphinspect.discovery`
- [X] T011 [P] [US1] Copy `graphEditor/src/jvmMain/kotlin/ui/DynamicPreviewDiscovery.kt` to `flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/discovery/DynamicPreviewDiscovery.kt` — update package to `io.codenode.flowgraphinspect.discovery`
- [X] T012 [US1] Run `./gradlew :flowGraph-inspect:compileKotlinJvm` to verify all 7 copied files compile in the new module
- [X] T013 [US1] Verify Compose UI files remain unchanged in graphEditor: `CodeEditor.kt`, `ColorEditor.kt`, `IPPalette.kt`, `NodePalette.kt`, `SyntaxHighlighter.kt` all still exist in `graphEditor/src/jvmMain/kotlin/ui/`

**Checkpoint**: 7 files copied and compiling. Both modules coexist (Strangler Fig).

---

## Phase 3: TDD Tests for CodeNode (US2 — Write Tests First)

**Purpose**: Write TDD tests for FlowGraphInspectCodeNode before implementation

**Goal**: Tests exist, compile, and fail (no implementation yet)

**Independent Test**: `./gradlew :flowGraph-inspect:jvmTest` — tests compile but fail

- [X] T014 [US2] Create `flowGraph-inspect/src/jvmTest/kotlin/io/codenode/flowgraphinspect/node/FlowGraphInspectCodeNodeTest.kt` with TDD tests covering: port signatures (2 inputs, 1 output, all String), anyInput mode, createRuntime returns In2AnyOut1Runtime, toNodeTypeDefinition returns correct metadata
- [X] T015 [US2] Add TDD tests for data flow: filesystemPaths input triggers node discovery and emits nodeDescriptors, classpathEntries input triggers compiled node discovery and emits nodeDescriptors, either input independently triggers processing using cached value from other input
- [X] T016 [US2] Add TDD tests for boundary conditions: empty input is no-op, directory that doesn't exist returns empty list, no CodeNode definitions found returns empty nodeDescriptors

**Checkpoint**: TDD tests written and failing. Ready for CodeNode implementation.

---

## Phase 4: CodeNode Implementation (US2 — Wrap as CodeNode)

**Purpose**: Implement FlowGraphInspectCodeNode as In2AnyOut1Runtime wrapper

**Goal**: CodeNode wraps NodeDefinitionRegistry, ComposableDiscovery, DynamicPreviewDiscovery behind 2 input ports and 1 output port

**Independent Test**: `./gradlew :flowGraph-inspect:jvmTest` — all CodeNode tests pass

- [X] T017 [US2] Create `flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/node/FlowGraphInspectCodeNode.kt` implementing `CodeNodeDefinition` with `In2AnyOut1Runtime<String, String, String>` and `anyInput = true`
- [X] T018 [US2] Implement processing logic: filesystemPaths input scans directories for .kt source files containing CodeNode definitions; classpathEntries input uses ServiceLoader to discover compiled CodeNodes; combined results emitted as JSON nodeDescriptors on the output port; either input uses cached value from the other
- [X] T019 [US2] Run `./gradlew :flowGraph-inspect:jvmTest` to verify all CodeNode tests pass

**Checkpoint**: CodeNode implemented. All TDD tests pass. Module builds independently.

---

## Phase 5: Call Site Migration (US3 — Update Consumers)

**Purpose**: Migrate graphEditor call sites from direct class imports to flowGraph-inspect module packages

**Goal**: All consumers use `io.codenode.flowgraphinspect.*` imports

**Independent Test**: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` — all tests pass

- [X] T020 [US3] Add dependency on `:flowGraph-inspect` in `graphEditor/build.gradle.kts`
- [X] T021 [US3] Update `graphEditor/src/jvmMain/kotlin/Main.kt` — change imports of NodeDefinitionRegistry, CodeEditorViewModel, IPPaletteViewModel, NodePaletteViewModel, DynamicPreviewDiscovery to `io.codenode.flowgraphinspect.*` packages
- [X] T022 [P] [US3] Update `graphEditor/src/jvmMain/kotlin/ui/CodeEditor.kt` — change import of CodeEditorViewModel to `io.codenode.flowgraphinspect.viewmodel.CodeEditorViewModel`
- [X] T023 [P] [US3] Update `graphEditor/src/jvmMain/kotlin/ui/IPPalette.kt` — change import of IPPaletteViewModel to `io.codenode.flowgraphinspect.viewmodel.IPPaletteViewModel`
- [X] T024 [P] [US3] Update `graphEditor/src/jvmMain/kotlin/ui/NodePalette.kt` — change import of NodePaletteViewModel to `io.codenode.flowgraphinspect.viewmodel.NodePaletteViewModel`
- [X] T025 [P] [US3] Update `graphEditor/src/jvmMain/kotlin/ui/RuntimePreviewPanel.kt` — change import of `discoverComposables` to `io.codenode.flowgraphinspect.discovery.discoverComposables`
- [X] T026 [P] [US3] Update `graphEditor/src/jvmMain/kotlin/viewmodel/NodeGeneratorViewModel.kt` — change imports of NodeDefinitionRegistry and NodeTemplateMeta to `io.codenode.flowgraphinspect.registry.*`
- [X] T027 [P] [US3] Update `graphEditor/src/jvmMain/kotlin/ui/ModuleSessionFactory.kt` — change import of NodeDefinitionRegistry to `io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry`
- [X] T028 [US3] Update all graphEditor test files that reference the migrated classes to use new `io.codenode.flowgraphinspect.*` imports — specifically `characterization/ViewModelCharacterizationTest.kt` and any test files importing NodeDefinitionRegistry, palette ViewModels, or discovery utilities
- [X] T029 [US3] Run `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` to verify no regressions after call site migration

**Checkpoint**: All call sites migrated. Old files are dead code. All tests pass.

---

## Phase 6: Remove Originals (US4 — Complete Extraction)

**Purpose**: Remove the 7 original files from graphEditor (Strangler Fig completion)

**Goal**: No copies of the 7 extracted files remain in graphEditor

**Independent Test**: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` — all tests pass

- [X] T030 [P] [US4] Remove `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/NodeDefinitionRegistry.kt`
- [X] T031 [P] [US4] Remove `graphEditor/src/jvmMain/kotlin/viewmodel/CodeEditorViewModel.kt`
- [X] T032 [P] [US4] Remove `graphEditor/src/jvmMain/kotlin/viewmodel/IPPaletteViewModel.kt`
- [X] T033 [P] [US4] Remove `graphEditor/src/jvmMain/kotlin/viewmodel/GraphNodePaletteViewModel.kt`
- [X] T034 [P] [US4] Remove `graphEditor/src/jvmMain/kotlin/viewmodel/NodePaletteViewModel.kt`
- [X] T035 [P] [US4] Remove `graphEditor/src/jvmMain/kotlin/ui/ComposableDiscovery.kt`
- [X] T036 [P] [US4] Remove `graphEditor/src/jvmMain/kotlin/ui/DynamicPreviewDiscovery.kt`
- [X] T037 [US4] Fix any remaining same-package reference errors by adding explicit imports to `io.codenode.flowgraphinspect.*` in test or source files that relied on same-package access
- [X] T038 [US4] Migrate test files: move `graphEditor/src/jvmTest/kotlin/viewmodel/IPPaletteViewModelTest.kt` and `graphEditor/src/jvmTest/kotlin/viewmodel/NodePaletteViewModelTest.kt` to `flowGraph-inspect/src/jvmTest/kotlin/io/codenode/flowgraphinspect/viewmodel/` with updated packages and imports
- [X] T039 [US4] Run `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest :flowGraph-inspect:jvmTest` to verify no regressions after original file removal

**Checkpoint**: Original files removed. graphEditor depends on flowGraph-inspect for all node discovery/palette state functionality. All tests pass.

---

## Phase 7: Architecture FlowGraph Wiring (US5 — Wire into architecture.flow.kt)

**Purpose**: Populate flowGraph-inspect GraphNode with child CodeNode and port mappings

**Goal**: architecture.flow.kt reflects the live FlowGraphInspect CodeNode with 2 inputs, 1 output

**Independent Test**: `./gradlew :graphEditor:jvmTest --tests "characterization.ArchitectureFlowKtsTest"` — all tests pass

- [ ] T040 [US5] Update description of the `flowGraph-inspect` graphNode in `graphEditor/architecture.flow.kt` to reflect corrected 7-file scope
- [ ] T041 [US5] Add `FlowGraphInspect` child codeNode inside the `flowGraph-inspect` graphNode in `graphEditor/architecture.flow.kt` with 2 inputs (filesystemPaths, classpathEntries) and 1 output (nodeDescriptors), nodeType = "TRANSFORMER"
- [ ] T042 [US5] Add portMapping declarations wiring all 3 exposed ports to the child codeNode ports in `graphEditor/architecture.flow.kt`
- [ ] T043 [US5] Run `./gradlew :graphEditor:jvmTest --tests "characterization.ArchitectureFlowKtsTest"` to verify all architecture tests pass

**Checkpoint**: architecture.flow.kt updated. flowGraph-inspect node contains FlowGraphInspect CodeNode with port mappings. All architecture tests pass.

---

## Phase 8: Verification & Cross-Cutting Concerns (US6)

**Purpose**: Verify dependency direction, no cycles, Strangler Fig compliance, full test suite

- [ ] T044 [US6] Verify no dependency on `:graphEditor` in `flowGraph-inspect/build.gradle.kts` — dependency direction is graphEditor → flowGraph-inspect → {flowGraph-types, flowGraph-persist} → fbpDsl
- [ ] T045 [US6] Verify no circular dependency exists by running `./gradlew :flowGraph-inspect:dependencies` and confirming only fbpDsl, flowGraph-types, and flowGraph-persist appear
- [ ] T046 [US6] Verify Strangler Fig sequence in git history: module creation → file copy → TDD tests → CodeNode implementation → call site migration → original removal → architecture wiring
- [ ] T047 Run full test suite: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest :flowGraph-types:jvmTest :flowGraph-persist:jvmTest :flowGraph-inspect:jvmTest` to verify zero regressions
- [ ] T048 Run quickstart.md validation scenarios (Scenarios 1-14)

**Checkpoint**: All verification complete. No cycles. Strangler Fig pattern followed. Zero regressions.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — starts immediately
- **Phase 2 (Extract Files)**: Depends on Phase 1 completion (module must exist)
- **Phase 3 (TDD Tests)**: Depends on Phase 2 completion (files must be in module for CodeNode to reference)
- **Phase 4 (CodeNode Implementation)**: Depends on Phase 3 completion (TDD — tests must exist and fail first)
- **Phase 5 (Call Site Migration)**: Depends on Phase 4 completion (CodeNode must exist for call sites to consume)
- **Phase 6 (Remove Originals)**: Depends on Phase 5 completion (all consumers must use new module)
- **Phase 7 (Architecture Wiring)**: Depends on Phase 4 completion (CodeNode must exist); can run in parallel with Phases 5-6 if architecture tests are independent
- **Phase 8 (Verification)**: Depends on all previous phases

### User Story Dependencies

- **US1 (P1 — Extract Files)**: Foundation — no dependencies on other stories
- **US2 (P2 — CodeNode)**: Depends on US1 (needs extracted files to wrap)
- **US3 (P3 — Migrate Call Sites)**: Depends on US2 (CodeNode must exist)
- **US4 (P4 — Remove Originals)**: Depends on US3 (all consumers redirected)
- **US5 (P5 — Architecture Wiring)**: Depends on US2 (CodeNode must exist)
- **US6 (P6 — Verification)**: Depends on all stories

### Parallel Opportunities

- **Phase 2**: All 7 file copy tasks (T005-T011) can run in parallel
- **Phase 5**: Call site update tasks (T022-T027) can run in parallel (different files)
- **Phase 6**: All 7 file removal tasks (T030-T036) can run in parallel
- **Phase 7 + Phase 5/6**: Architecture wiring can run independently from call site migration

---

## Implementation Strategy

### MVP First (Phase 1-2 Only)

1. Complete Phase 1: Module skeleton
2. Complete Phase 2: Copy 7 files
3. **STOP and VALIDATE**: Module compiles independently

### Incremental Delivery

1. Phase 1-2: Module exists with files → compiles
2. Phase 3-4: CodeNode wraps module → TDD tests pass
3. Phase 5: Call sites migrated → all tests pass
4. Phase 6: Originals removed → clean extraction
5. Phase 7: Architecture wired → visible in FlowGraph
6. Phase 8: Full verification → feature complete
