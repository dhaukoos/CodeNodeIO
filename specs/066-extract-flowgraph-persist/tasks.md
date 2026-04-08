# Tasks: flowGraph-persist Module Extraction

**Input**: Design documents from `/specs/066-extract-flowgraph-persist/`
**Prerequisites**: plan.md, spec.md, research.md, quickstart.md

**Tests**: TDD tests for the CodeNode are required (FR-006, Constitution Principle II).

**Organization**: Tasks follow the Strangler Fig pattern: setup → copy → TDD tests → implementation → migration → removal → architecture wiring → verification.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Module Skeleton)

**Purpose**: Create flowGraph-persist module structure and Gradle configuration

- [X] T001 Add `include("flowGraph-persist")` to `settings.gradle.kts` after `:flowGraph-types`
- [X] T002 Create `flowGraph-persist/build.gradle.kts` following the flowGraph-types pattern: KMP module with JVM + iOS targets, depends on `:fbpDsl` and `:flowGraph-types`, kotlinx-serialization plugin, JUnit 5 for tests
- [X] T003 Create source directory structure: `flowGraph-persist/src/commonMain/kotlin/io/codenode/flowgraphpersist/model/`, `flowGraph-persist/src/jvmMain/kotlin/io/codenode/flowgraphpersist/serialization/`, `flowGraph-persist/src/jvmMain/kotlin/io/codenode/flowgraphpersist/state/`, `flowGraph-persist/src/jvmMain/kotlin/io/codenode/flowgraphpersist/node/`, `flowGraph-persist/src/jvmTest/kotlin/io/codenode/flowgraphpersist/node/`
- [X] T004 Run `./gradlew :flowGraph-persist:compileKotlinJvm` to verify empty module compiles

**Checkpoint**: Empty module compiles. Ready for file extraction.

---

## Phase 2: File Extraction (US1 — Strangler Fig Copy)

**Purpose**: Copy 6 files from graphEditor to flowGraph-persist with updated packages

**Goal**: All 6 files compile in the new module with updated package declarations

**Independent Test**: `./gradlew :flowGraph-persist:compileKotlinJvm` succeeds

- [X] T005 [P] [US1] Copy `graphEditor/src/jvmMain/kotlin/model/GraphNodeTemplateMeta.kt` to `flowGraph-persist/src/commonMain/kotlin/io/codenode/flowgraphpersist/model/GraphNodeTemplateMeta.kt` — update package to `io.codenode.flowgraphpersist.model`
- [X] T006 [P] [US1] Copy `graphEditor/src/jvmMain/kotlin/serialization/FlowGraphSerializer.kt` to `flowGraph-persist/src/jvmMain/kotlin/io/codenode/flowgraphpersist/serialization/FlowGraphSerializer.kt` — update package to `io.codenode.flowgraphpersist.serialization`, update internal imports
- [X] T007 [P] [US1] Copy `graphEditor/src/jvmMain/kotlin/serialization/FlowKtParser.kt` to `flowGraph-persist/src/jvmMain/kotlin/io/codenode/flowgraphpersist/serialization/FlowKtParser.kt` — update package to `io.codenode.flowgraphpersist.serialization`
- [X] T008 [P] [US1] Copy `graphEditor/src/jvmMain/kotlin/serialization/GraphNodeTemplateSerializer.kt` to `flowGraph-persist/src/jvmMain/kotlin/io/codenode/flowgraphpersist/serialization/GraphNodeTemplateSerializer.kt` — update package to `io.codenode.flowgraphpersist.serialization`, update internal imports to reference new model and serialization packages
- [X] T009 [P] [US1] Copy `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphNodeTemplateRegistry.kt` to `flowGraph-persist/src/jvmMain/kotlin/io/codenode/flowgraphpersist/state/GraphNodeTemplateRegistry.kt` — update package to `io.codenode.flowgraphpersist.state`, update internal imports
- [X] T010 [P] [US1] Copy `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphNodeTemplateInstantiator.kt` to `flowGraph-persist/src/jvmMain/kotlin/io/codenode/flowgraphpersist/state/GraphNodeTemplateInstantiator.kt` — update package to `io.codenode.flowgraphpersist.state`, update internal imports
- [X] T011 [US1] Run `./gradlew :flowGraph-persist:compileKotlinJvm` to verify all 6 copied files compile in the new module

**Checkpoint**: 6 files copied and compiling. Both modules coexist (Strangler Fig).

---

## Phase 3: TDD Tests for CodeNode (US2 — Write Tests First)

**Purpose**: Write TDD tests for FlowGraphPersistCodeNode before implementation

**Goal**: Tests exist, compile, and fail (no implementation yet)

**Independent Test**: `./gradlew :flowGraph-persist:jvmTest` — tests compile but fail

- [X] T012 [US2] Create `flowGraph-persist/src/jvmTest/kotlin/io/codenode/flowgraphpersist/node/FlowGraphPersistCodeNodeTest.kt` with TDD tests covering: port signatures (2 inputs, 3 outputs, all String), anyInput mode, createRuntime returns In2AnyOut3Runtime, toNodeTypeDefinition returns correct metadata
- [X] T013 [US2] Add TDD tests for data flow: serialize command on flowGraphModel produces output on serializedOutput, deserialize command produces output on loadedFlowGraph, template commands produce output on graphNodeTemplates
- [X] T014 [US2] Add TDD tests for boundary conditions: empty/null input is no-op, malformed JSON command returns error on appropriate output, ipTypeMetadata is cached and used during serialization

**Checkpoint**: TDD tests written and failing. Ready for CodeNode implementation.

---

## Phase 4: CodeNode Implementation (US2 — Wrap as CodeNode)

**Purpose**: Implement FlowGraphPersistCodeNode as In2AnyOut3Runtime wrapper

**Goal**: CodeNode wraps FlowGraphSerializer, FlowKtParser, GraphNodeTemplateSerializer, GraphNodeTemplateRegistry behind 2 input ports and 3 output ports

**Independent Test**: `./gradlew :flowGraph-persist:jvmTest` — all CodeNode tests pass

- [X] T015 [US2] Create `flowGraph-persist/src/jvmMain/kotlin/io/codenode/flowgraphpersist/node/FlowGraphPersistCodeNode.kt` implementing `CodeNodeDefinition` with `In2AnyOut3Runtime<String, String, String, String, String>` and `anyInput = true`
- [X] T016 [US2] Implement processing logic: flowGraphModel input deserializes JSON commands (serialize, deserialize, saveTemplate, loadTemplate, deleteTemplate, listTemplates); ipTypeMetadata input caches type data for serialization; outputs emit on appropriate ports based on command type
- [X] T017 [US2] Run `./gradlew :flowGraph-persist:jvmTest` to verify all CodeNode tests pass

**Checkpoint**: CodeNode implemented. All TDD tests pass. Module builds independently.

---

## Phase 5: Call Site Migration (US3 — Update Consumers)

**Purpose**: Migrate graphEditor call sites from direct class imports to flowGraph-persist module packages

**Goal**: All consumers use `io.codenode.flowgraphpersist.*` imports

**Independent Test**: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` — all tests pass

- [X] T018 [US3] Add dependency on `:flowGraph-persist` in `graphEditor/build.gradle.kts`
- [X] T019 [US3] Update all graphEditor source files that import `io.codenode.grapheditor.serialization.FlowGraphSerializer` to use `io.codenode.flowgraphpersist.serialization.FlowGraphSerializer`
- [X] T020 [US3] Update all graphEditor source files that import `io.codenode.grapheditor.serialization.FlowKtParser` to use `io.codenode.flowgraphpersist.serialization.FlowKtParser`
- [X] T021 [US3] Update all graphEditor source files that import `io.codenode.grapheditor.serialization.GraphNodeTemplateSerializer` to use `io.codenode.flowgraphpersist.serialization.GraphNodeTemplateSerializer`
- [X] T022 [US3] Update all graphEditor source files that import `io.codenode.grapheditor.model.GraphNodeTemplateMeta` to use `io.codenode.flowgraphpersist.model.GraphNodeTemplateMeta`
- [X] T023 [US3] Update all graphEditor source files that import `io.codenode.grapheditor.state.GraphNodeTemplateRegistry` or `io.codenode.grapheditor.state.GraphNodeTemplateInstantiator` to use `io.codenode.flowgraphpersist.state.*`
- [X] T024 [US3] Update all graphEditor test files that reference the migrated classes to use new `io.codenode.flowgraphpersist.*` imports
- [X] T025 [US3] Run `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` to verify no regressions after call site migration

**Checkpoint**: All call sites migrated. Old files are dead code. All tests pass.

---

## Phase 6: Remove Originals (US4 — Complete Extraction)

**Purpose**: Remove the 6 original files from graphEditor (Strangler Fig completion)

**Goal**: No copies of the 6 extracted files remain in graphEditor

**Independent Test**: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` — all tests pass

- [ ] T026 [P] [US4] Remove `graphEditor/src/jvmMain/kotlin/serialization/FlowGraphSerializer.kt`
- [ ] T027 [P] [US4] Remove `graphEditor/src/jvmMain/kotlin/serialization/FlowKtParser.kt`
- [ ] T028 [P] [US4] Remove `graphEditor/src/jvmMain/kotlin/serialization/GraphNodeTemplateSerializer.kt`
- [ ] T029 [P] [US4] Remove `graphEditor/src/jvmMain/kotlin/model/GraphNodeTemplateMeta.kt`
- [ ] T030 [P] [US4] Remove `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphNodeTemplateRegistry.kt`
- [ ] T031 [P] [US4] Remove `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphNodeTemplateInstantiator.kt`
- [ ] T032 [US4] Fix any remaining same-package reference errors by adding explicit imports to `io.codenode.flowgraphpersist.*` in test or source files that relied on same-package access
- [ ] T033 [US4] Run `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` to verify no regressions after original file removal

**Checkpoint**: Original files removed. graphEditor depends on flowGraph-persist for all serialization/template functionality. All tests pass.

---

## Phase 7: Architecture FlowGraph Wiring (US5 — Wire into architecture.flow.kt)

**Purpose**: Populate flowGraph-persist GraphNode with child CodeNode and port mappings

**Goal**: architecture.flow.kt reflects the live FlowGraphPersist CodeNode with 2 inputs, 3 outputs

**Independent Test**: `./gradlew :graphEditor:jvmTest --tests "characterization.ArchitectureFlowKtsTest"` — all tests pass

- [ ] T034 [US5] Add `FlowGraphPersist` child codeNode inside the `flowGraph-persist` graphNode in `graphEditor/architecture.flow.kt` with 2 inputs (flowGraphModel, ipTypeMetadata) and 3 outputs (serializedOutput, loadedFlowGraph, graphNodeTemplates), nodeType = "TRANSFORMER"
- [ ] T035 [US5] Add portMapping declarations wiring all 5 exposed ports to the child codeNode ports in `graphEditor/architecture.flow.kt`
- [ ] T036 [US5] Run `./gradlew :graphEditor:jvmTest --tests "characterization.ArchitectureFlowKtsTest"` to verify all architecture tests pass

**Checkpoint**: architecture.flow.kt updated. flowGraph-persist node contains FlowGraphPersist CodeNode with port mappings. All architecture tests pass.

---

## Phase 8: Verification & Cross-Cutting Concerns (US6)

**Purpose**: Verify dependency direction, no cycles, Strangler Fig compliance, full test suite

- [ ] T037 [US6] Verify no dependency on `:graphEditor` in `flowGraph-persist/build.gradle.kts` — dependency direction is graphEditor → flowGraph-persist → flowGraph-types → fbpDsl
- [ ] T038 [US6] Verify no circular dependency exists by running `./gradlew :flowGraph-persist:dependencies` and confirming only fbpDsl and flowGraph-types appear
- [ ] T039 [US6] Verify Strangler Fig sequence in git history: module creation → file copy → TDD tests → CodeNode implementation → call site migration → original removal → architecture wiring
- [ ] T040 Run full test suite: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest :flowGraph-types:jvmTest :flowGraph-persist:jvmTest` to verify zero regressions
- [ ] T041 Run quickstart.md validation scenarios (Scenarios 1-10)

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

- **Phase 2**: All 6 file copy tasks (T005-T010) can run in parallel
- **Phase 6**: All 6 file removal tasks (T026-T031) can run in parallel
- **Phase 7 + Phase 5/6**: Architecture wiring can run independently from call site migration

---

## Implementation Strategy

### MVP First (Phase 1-2 Only)

1. Complete Phase 1: Module skeleton
2. Complete Phase 2: Copy 6 files
3. **STOP and VALIDATE**: Module compiles independently

### Incremental Delivery

1. Phase 1-2: Module exists with files → compiles
2. Phase 3-4: CodeNode wraps module → TDD tests pass
3. Phase 5: Call sites migrated → all tests pass
4. Phase 6: Originals removed → clean extraction
5. Phase 7: Architecture wired → visible in FlowGraph
6. Phase 8: Full verification → feature complete
