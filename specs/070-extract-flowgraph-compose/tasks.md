# Tasks: Extract flowGraph-compose Module

**Input**: Design documents from `/specs/070-extract-flowgraph-compose/`
**Prerequisites**: plan.md (required), spec.md (required), research.md

**Tests**: TDD tests required per spec FR-012 and constitution principle II.

**Organization**: Tasks follow the established 8-phase Strangler Fig pattern from features 065-069, organized by user story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Module Initialization)

**Purpose**: Create flowGraph-compose KMP module structure and build configuration

- [X] T001 Create flowGraph-compose module directory structure: `flowGraph-compose/src/{jvmMain,jvmTest}/kotlin/io/codenode/flowgraphcompose/`
- [X] T002 Create `flowGraph-compose/build.gradle.kts` with KMP configuration — dependencies: `:fbpDsl` (commonMain), compose-desktop + lifecycle-viewmodel-compose 2.8.0 + kotlinx-coroutines 1.8.0 (jvmMain), kotlin-test + junit5 (test). Follow the pattern from `flowGraph-inspect/build.gradle.kts` for compose-desktop dependency setup
- [X] T003 Update `settings.gradle.kts` — add `include(":flowGraph-compose")`

**Checkpoint**: Empty module compiles — `./gradlew :flowGraph-compose:compileKotlinJvm`

---

## Phase 2: Copy Files (Strangler Fig — Duplicate)

**Purpose**: Copy 4 source files to the new module, update package declarations. Originals remain in place.

### viewmodel files (2 files)

- [X] T004 [P] [US1] Copy `CanvasInteractionViewModel.kt` from `graphEditor/src/jvmMain/kotlin/viewmodel/` to `flowGraph-compose/src/jvmMain/kotlin/io/codenode/flowgraphcompose/viewmodel/` — update package to `io.codenode.flowgraphcompose.viewmodel`, update all internal `io.codenode.grapheditor` imports. If file references `GraphState` from graphEditor, refactor to depend on fbpDsl model types (FlowGraph, Node, Connection) or extract an interface
- [X] T005 [P] [US1] Copy `PropertiesPanelViewModel.kt` from `graphEditor/src/jvmMain/kotlin/viewmodel/` to `flowGraph-compose/src/jvmMain/kotlin/io/codenode/flowgraphcompose/viewmodel/` — update package and imports similarly

### state files (2 files)

- [X] T006 [P] [US1] Copy `NodeGeneratorState.kt` from `graphEditor/src/jvmMain/kotlin/state/` to `flowGraph-compose/src/jvmMain/kotlin/io/codenode/flowgraphcompose/state/` — update package to `io.codenode.flowgraphcompose.state`
- [X] T007 [P] [US1] Copy `ViewSynchronizer.kt` from `graphEditor/src/jvmMain/kotlin/state/` to `flowGraph-compose/src/jvmMain/kotlin/io/codenode/flowgraphcompose/state/` — update package and imports. If file references `GraphState` from graphEditor, refactor to depend on fbpDsl model types instead

**Checkpoint**: New module compiles alongside originals — `./gradlew :flowGraph-compose:compileKotlinJvm`

---

## Phase 3: User Story 2 — TDD Tests for CodeNode (Priority: P2)

**Goal**: Write failing test for FlowGraphComposeCodeNode before implementation

**Independent Test**: Test compiles but fails until CodeNode implementation exists

### TDD Tests

- [X] T008 [US2] Write TDD tests for FlowGraphComposeCodeNode in `flowGraph-compose/src/jvmTest/kotlin/io/codenode/flowgraphcompose/nodes/FlowGraphComposeCodeNodeTest.kt` — test port signatures (3 inputs: flowGraphModel, nodeDescriptors, ipTypeMetadata; 1 output: graphState), anyInput=true, category=TRANSFORMER, runtime creation, basic data flow (send flowGraphModel → receive graphState), caching behavior (send nodeDescriptors then flowGraphModel → both used)

**Checkpoint**: Test compiles but fails — `./gradlew :flowGraph-compose:jvmTest` shows expected failure

---

## Phase 4: User Story 2 — CodeNode Implementation (Priority: P2)

**Goal**: Implement FlowGraphComposeCodeNode using In3AnyOut1Runtime

- [X] T009 [US2] Implement FlowGraphComposeCodeNode in `flowGraph-compose/src/jvmMain/kotlin/io/codenode/flowgraphcompose/nodes/FlowGraphComposeCodeNode.kt` — Kotlin object implementing CodeNodeDefinition, uses CodeNodeFactory.createIn3AnyOut1Processor, anyInput=true, takes flowGraphModel + nodeDescriptors + ipTypeMetadata → produces graphState. Include `override val sourceFilePath: String? get() = resolveSourceFilePath(this::class.java)` and import from `io.codenode.fbpdsl.runtime.resolveSourceFilePath`
- [X] T010 [US2] Create META-INF registry file at `flowGraph-compose/src/jvmMain/resources/META-INF/codenode/node-definitions` — single line: `io.codenode.flowgraphcompose.nodes.FlowGraphComposeCodeNode`

**Checkpoint**: All TDD tests pass — `./gradlew :flowGraph-compose:jvmTest`

---

## Phase 5: User Story 1 — Migrate Consumers (Priority: P1)

**Goal**: Switch all graphEditor consumers to import from `io.codenode.flowgraphcompose` instead of graphEditor's own viewmodel/state packages for the 4 moved files

### Build dependency update

- [X] T011 [US1] Update `graphEditor/build.gradle.kts` — add `project(":flowGraph-compose")` dependency

### graphEditor import migrations

- [X] T012 [US1] Search all graphEditor source files for imports of `CanvasInteractionViewModel` from `io.codenode.grapheditor.viewmodel` — update each to `io.codenode.flowgraphcompose.viewmodel.CanvasInteractionViewModel`. Key files: Main.kt, SharedStateProvider.kt, FlowGraphCanvas.kt, any UI composable referencing canvas interactions
- [X] T013 [US1] Search all graphEditor source files for imports of `PropertiesPanelViewModel` from `io.codenode.grapheditor.viewmodel` — update each to `io.codenode.flowgraphcompose.viewmodel.PropertiesPanelViewModel`. Key files: Main.kt, SharedStateProvider.kt, PropertiesPanel.kt
- [X] T014 [US1] Search all graphEditor source files for imports of `NodeGeneratorState` from `io.codenode.grapheditor.state` — update each to `io.codenode.flowgraphcompose.state.NodeGeneratorState`. Key files: UI panels referencing node generator form state
- [X] T015 [US1] Search all graphEditor source files for imports of `ViewSynchronizer` from `io.codenode.grapheditor.state` — update each to `io.codenode.flowgraphcompose.state.ViewSynchronizer`. Key files: Main.kt, SharedStateProvider.kt

**Checkpoint**: graphEditor compiles with new imports — `./gradlew :graphEditor:compileKotlinJvm`

---

## Phase 6: User Story 1 — Remove Originals (Priority: P1)

**Goal**: Delete the 4 original files from graphEditor

- [X] T016 [US1] Remove `graphEditor/src/jvmMain/kotlin/viewmodel/CanvasInteractionViewModel.kt`
- [X] T017 [US1] Remove `graphEditor/src/jvmMain/kotlin/viewmodel/PropertiesPanelViewModel.kt`
- [X] T018 [US1] Remove `graphEditor/src/jvmMain/kotlin/state/NodeGeneratorState.kt`
- [X] T019 [US1] Remove `graphEditor/src/jvmMain/kotlin/state/ViewSynchronizer.kt`
- [X] T020 [US1] Verify zero references to old package paths for the 4 moved files remain — grep entire codebase for `io.codenode.grapheditor.viewmodel.CanvasInteractionViewModel`, `io.codenode.grapheditor.viewmodel.PropertiesPanelViewModel`, `io.codenode.grapheditor.state.NodeGeneratorState`, `io.codenode.grapheditor.state.ViewSynchronizer`

**Checkpoint**: Full project compiles and all tests pass — `./gradlew check`

---

## Phase 7: User Story 3 — Architecture Wiring (Priority: P3)

**Goal**: Update architecture.flow.kt with child codeNode for flowGraph-compose

- [ ] T021 [US3] Update the compose graphNode in `graphEditor/architecture.flow.kt` — add child codeNode "FlowGraphCompose" (TRANSFORMER) with inputs flowGraphModel, nodeDescriptors, ipTypeMetadata and output graphState
- [ ] T022 [US3] Add port mappings to compose graphNode in `graphEditor/architecture.flow.kt` — map 3 exposed inputs (flowGraphModel, nodeDescriptors, ipTypeMetadata) to FlowGraphCompose's input ports, map exposed output (graphState) to FlowGraphCompose's graphState output
- [ ] T023 [US3] Update compose graphNode description to reflect actual file count and single-node structure in `graphEditor/architecture.flow.kt`

**Checkpoint**: architecture.flow.kt parses successfully with all 20 external connections intact

---

## Phase 8: Verification & Polish

**Purpose**: Final validation across the entire project

- [ ] T024 Run full project compilation: `./gradlew compileKotlinJvm` (all modules)
- [ ] T025 Run full test suite: `./gradlew check` — all existing tests must pass
- [ ] T026 Run characterization tests specifically: `./gradlew :graphEditor:jvmTest --tests "*GraphDataOpsCharacterizationTest*"` and `./gradlew :graphEditor:jvmTest --tests "*ViewModelCharacterizationTest*"`
- [ ] T027 Verify SC-004: grep entire codebase for old graphEditor package paths of the 4 moved files — must return zero results outside flowGraph-compose
- [ ] T028 Verify SC-005: confirm the 4 original files do not exist in graphEditor
- [ ] T029 Verify SC-006: confirm architecture.flow.kt contains compose graphNode with child codeNode FlowGraphCompose and correct port mappings
- [ ] T030 Verify SC-008: confirm graphEditor Compose UI files compile with flowGraph-compose imports
- [ ] T031 Verify all six workflow graphNodes in architecture.flow.kt have child codeNode definitions (types, inspect, persist, compose, execute, generate) — completing the Phase B vertical-slice decomposition

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Copy)**: Depends on Phase 1 — all copy tasks [P] can run in parallel
- **Phase 3 (TDD Tests)**: Depends on Phase 1 (module exists) — can overlap with Phase 2
- **Phase 4 (CodeNode Impl)**: Depends on Phase 3
- **Phase 5 (Migrate)**: Depends on Phase 2 (files exist in new location)
- **Phase 6 (Remove)**: Depends on Phase 5 (consumers migrated)
- **Phase 7 (Architecture)**: Depends on Phase 4 (CodeNode exists) and Phase 6 (module clean)
- **Phase 8 (Verify)**: Depends on all prior phases

### User Story Dependencies

- **US1 (Extract)**: Phases 1-2, 5-6 — core extraction and migration
- **US2 (CodeNode)**: Phases 3-4 — TDD tests and implementation (can overlap with Phase 2)
- **US3 (Architecture)**: Phase 7 — depends on US1 and US2 completion

### Parallel Opportunities

- **Phase 2**: All copy tasks (T004-T007) can run in parallel — different source/target files
- **Phase 5**: T012-T015 are sequential per file (shared graphEditor files)
- **Phase 6**: T016-T019 can run in parallel — different files being deleted

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Copy files
3. Complete Phase 5: Migrate consumers
4. Complete Phase 6: Remove originals
5. **VALIDATE**: `./gradlew check` — all tests pass, originals removed

### Full Delivery

1. MVP (above) + Phase 3-4 (CodeNode with TDD) + Phase 7 (Architecture wiring)
2. Phase 8: Final verification

---

## Notes

- The Strangler Fig pattern means originals stay in place until Phase 6. Both old and new modules coexist during Phases 2-5.
- All 4 files go to jvmMain (not commonMain) because they depend on Compose Desktop types (Offset, Rect, @Composable).
- Critical risk: If moved files reference GraphState.kt (stays in graphEditor), they must be refactored during Phase 2 to use fbpDsl model types instead, avoiding circular dependency.
- This is the final workflow module extraction (Step 6). After Phase 7, all six graphNodes in architecture.flow.kt will have child codeNode definitions.
- Total files: 4 from graphEditor moving to flowGraph-compose, plus 1 new CodeNode + 1 test = 6 new files.
