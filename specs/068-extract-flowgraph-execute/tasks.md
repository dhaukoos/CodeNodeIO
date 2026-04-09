# Tasks: flowGraph-execute Module Extraction

**Input**: Design documents from `/specs/068-extract-flowgraph-execute/`
**Prerequisites**: plan.md, spec.md, research.md, quickstart.md

**Tests**: TDD tests for the CodeNode are required (FR-008, Constitution Principle II).

**Organization**: Tasks follow the Strangler Fig pattern: setup → copy → TDD tests → implementation → migration → removal → architecture wiring → verification.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Module Skeleton)

**Purpose**: Create flowGraph-execute module structure and Gradle configuration

- [X] T001 Add `include("flowGraph-execute")` to `settings.gradle.kts` after `:flowGraph-inspect`
- [X] T002 Create `flowGraph-execute/build.gradle.kts` following the flowGraph-inspect pattern: KMP module with JVM + iOS targets, depends on `:fbpDsl` and `:flowGraph-inspect`, kotlinx-coroutines, kotlinx-serialization, JUnit 5 for tests. Do NOT include Compose dependencies (R4 — unused by source files).
- [X] T003 Create source directory structure: `flowGraph-execute/src/commonMain/kotlin/io/codenode/flowgraphexecute/`, `flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/`, `flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/node/`, `flowGraph-execute/src/jvmTest/kotlin/io/codenode/flowgraphexecute/node/`, `flowGraph-execute/src/jvmTest/kotlin/io/codenode/flowgraphexecute/characterization/`
- [X] T004 Run `./gradlew :flowGraph-execute:compileKotlinJvm` to verify empty module compiles

**Checkpoint**: Empty module compiles. Ready for file extraction.

---

## Phase 2: File Extraction (US1 — Strangler Fig Copy)

**Purpose**: Copy 6 files from circuitSimulator and graphEditor to flowGraph-execute with updated packages

**Goal**: All 6 files compile in the new module. 5 circuitSimulator files go in commonMain (R3 — no JVM-specific APIs). ModuleSessionFactory goes in jvmMain (uses java.lang.reflect).

**Independent Test**: `./gradlew :flowGraph-execute:compileKotlinJvm` succeeds

- [X] T005 [P] [US1] Copy `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/RuntimeSession.kt` to `flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/RuntimeSession.kt` — update package to `io.codenode.flowgraphexecute`, update all internal `io.codenode.circuitsimulator` imports
- [X] T006 [P] [US1] Copy `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/DataFlowAnimationController.kt` to `flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/DataFlowAnimationController.kt` — update package to `io.codenode.flowgraphexecute`
- [X] T007 [P] [US1] Copy `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/DataFlowDebugger.kt` to `flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/DataFlowDebugger.kt` — update package to `io.codenode.flowgraphexecute`
- [X] T008 [P] [US1] Copy `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/ConnectionAnimation.kt` to `flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/ConnectionAnimation.kt` — update package to `io.codenode.flowgraphexecute`
- [X] T009 [P] [US1] Copy `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/CircuitSimulator.kt` to `flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/CircuitSimulator.kt` — update package to `io.codenode.flowgraphexecute`
- [X] T010 [P] [US1] Copy `graphEditor/src/jvmMain/kotlin/ui/ModuleSessionFactory.kt` to `flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/ModuleSessionFactory.kt` — update package to `io.codenode.flowgraphexecute`, update `io.codenode.circuitsimulator.RuntimeSession` import to `io.codenode.flowgraphexecute.RuntimeSession`, keep `io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry` import unchanged
- [X] T011 [US1] Run `./gradlew :flowGraph-execute:compileKotlinJvm` to verify all 6 copied files compile in the new module
- [X] T012 [US1] Verify Strangler Fig coexistence: run `./gradlew :graphEditor:compileKotlinJvm :circuitSimulator:compileKotlinJvm :flowGraph-execute:compileKotlinJvm` — all three modules compile independently

**Checkpoint**: 6 files copied and compiling. All modules coexist (Strangler Fig).

---

## Phase 3: TDD Tests for CodeNode (US2 — Write Tests First)

**Purpose**: Write TDD tests for FlowGraphExecuteCodeNode before implementation

**Goal**: Tests exist, compile, and fail (no implementation yet)

**Independent Test**: `./gradlew :flowGraph-execute:jvmTest` — tests compile but fail

- [X] T013 [US2] Create `flowGraph-execute/src/jvmTest/kotlin/io/codenode/flowgraphexecute/node/FlowGraphExecuteCodeNodeTest.kt` with TDD tests covering: port signatures (2 inputs: flowGraphModel, nodeDescriptors; 3 outputs: executionState, animations, debugSnapshots; all String type), anyInput mode, category = TRANSFORMER, createRuntime returns In2AnyOut3Runtime
- [X] T014 [US2] Add TDD tests for data flow: flowGraphModel input triggers execution state emission on executionState port, ProcessResult3 selective output (not every input produces all 3 outputs), empty/invalid flowGraphModel returns ProcessResult3(null, null, null)
- [X] T015 [US2] Add TDD tests for boundary conditions: empty input is no-op, nodeDescriptors arriving alone caches value for later use, toNodeTypeDefinition returns correct metadata

**Checkpoint**: TDD tests written and failing. Ready for CodeNode implementation.

---

## Phase 4: CodeNode Implementation (US2 — Wrap as CodeNode)

**Purpose**: Implement FlowGraphExecuteCodeNode as In2AnyOut3Runtime wrapper

**Goal**: CodeNode wraps RuntimeSession, DataFlowAnimationController, DataFlowDebugger behind 2 input ports and 3 output ports using ProcessResult3

**Independent Test**: `./gradlew :flowGraph-execute:jvmTest` — all CodeNode tests pass

- [X] T016 [US2] Create `flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/node/FlowGraphExecuteCodeNode.kt` implementing `CodeNodeDefinition` with `In2AnyOut3Runtime<String, String, String, String, String>` and `anyInput = true`, using `ProcessResult3` for selective output
- [X] T017 [US2] Implement processing logic: flowGraphModel input configures the runtime pipeline to execute; nodeDescriptors input caches node definitions for pipeline building; executionState output emits lifecycle state changes (IDLE, RUNNING, PAUSED, ERROR); animations output emits active ConnectionAnimation data when animation enabled; debugSnapshots output emits per-connection value captures
- [X] T018 [US2] Run `./gradlew :flowGraph-execute:jvmTest` to verify all CodeNode tests pass

**Checkpoint**: CodeNode implemented. All TDD tests pass. Module builds independently.

---

## Phase 5: Call Site Migration (US3 — Update Consumers)

**Purpose**: Migrate graphEditor call sites from circuitSimulator imports to flowGraph-execute packages

**Goal**: All consumers use `io.codenode.flowgraphexecute.*` imports

**Independent Test**: `./gradlew :graphEditor:jvmTest :circuitSimulator:jvmTest` — all tests pass

- [ ] T019 [US3] Update `graphEditor/build.gradle.kts` — add `implementation(project(":flowGraph-execute"))` and remove `implementation(project(":circuitSimulator"))` dependency
- [ ] T020 [US3] Update `graphEditor/src/jvmMain/kotlin/Main.kt` — change imports of `io.codenode.circuitsimulator.ConnectionAnimation` and `io.codenode.circuitsimulator.RuntimeSession` to `io.codenode.flowgraphexecute.ConnectionAnimation` and `io.codenode.flowgraphexecute.RuntimeSession`; change import of ModuleSessionFactory to `io.codenode.flowgraphexecute.ModuleSessionFactory`
- [ ] T021 [P] [US3] Update `graphEditor/src/jvmMain/kotlin/ui/FlowGraphCanvas.kt` — change import of `io.codenode.circuitsimulator.ConnectionAnimation` to `io.codenode.flowgraphexecute.ConnectionAnimation`
- [ ] T022 [P] [US3] Update `graphEditor/src/jvmMain/kotlin/ui/RuntimePreviewPanel.kt` — change import of `io.codenode.circuitsimulator.RuntimeSession` to `io.codenode.flowgraphexecute.RuntimeSession`
- [ ] T023 [US3] Update `idePlugin/build.gradle.kts` — replace `implementation(project(":circuitSimulator"))` with `implementation(project(":flowGraph-execute"))`
- [ ] T024 [US3] Search all graphEditor source files for any remaining `io.codenode.circuitsimulator` imports and update them to `io.codenode.flowgraphexecute`; also search for any remaining `io.codenode.grapheditor.ui.ModuleSessionFactory` imports
- [ ] T025 [US3] Run `./gradlew :graphEditor:compileKotlinJvm :idePlugin:compileKotlinJvm` to verify all call site migrations compile
- [ ] T026 [US3] Run `./gradlew :graphEditor:jvmTest` to verify no regressions after call site migration

**Checkpoint**: All call sites migrated. Old files are dead code. All tests pass.

---

## Phase 6: Remove Originals and Absorb circuitSimulator (US4 — Complete Extraction)

**Purpose**: Remove the 6 original files and absorb circuitSimulator into flowGraph-execute

**Goal**: circuitSimulator source files deleted. ModuleSessionFactory deleted from graphEditor. Test migrated.

**Independent Test**: `./gradlew :graphEditor:jvmTest :flowGraph-execute:jvmTest` — all tests pass

- [ ] T027 [P] [US4] Remove `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/RuntimeSession.kt`
- [ ] T028 [P] [US4] Remove `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/DataFlowAnimationController.kt`
- [ ] T029 [P] [US4] Remove `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/DataFlowDebugger.kt`
- [ ] T030 [P] [US4] Remove `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/ConnectionAnimation.kt`
- [ ] T031 [P] [US4] Remove `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/CircuitSimulator.kt`
- [ ] T032 [P] [US4] Remove `graphEditor/src/jvmMain/kotlin/ui/ModuleSessionFactory.kt`
- [ ] T033 [US4] Migrate test: copy `circuitSimulator/src/commonTest/kotlin/characterization/RuntimeSessionCharacterizationTest.kt` to `flowGraph-execute/src/jvmTest/kotlin/io/codenode/flowgraphexecute/characterization/RuntimeSessionCharacterizationTest.kt` — update package to `io.codenode.flowgraphexecute.characterization`, update all `io.codenode.circuitsimulator` imports to `io.codenode.flowgraphexecute`
- [ ] T034 [US4] Remove the original `circuitSimulator/src/commonTest/kotlin/characterization/RuntimeSessionCharacterizationTest.kt`
- [ ] T035 [US4] Fix any remaining same-package reference errors by adding explicit imports to `io.codenode.flowgraphexecute.*` in source or test files that relied on same-package access
- [ ] T036 [US4] Run `./gradlew :graphEditor:jvmTest :flowGraph-execute:jvmTest :flowGraph-inspect:jvmTest` to verify no regressions after original file removal

**Checkpoint**: Original files removed. circuitSimulator is absorbed. graphEditor depends on flowGraph-execute. All tests pass.

---

## Phase 7: Architecture FlowGraph Wiring (US5 — Wire into architecture.flow.kt)

**Purpose**: Populate flowGraph-execute GraphNode with child CodeNode and port mappings

**Goal**: architecture.flow.kt reflects the live FlowGraphExecute CodeNode with 2 inputs, 3 outputs

**Independent Test**: `./gradlew :graphEditor:jvmTest --tests "io.codenode.grapheditor.characterization.ArchitectureFlowKtsTest"` — all tests pass

- [ ] T037 [US5] Update description of the `flowGraph-execute` graphNode in `graphEditor/architecture.flow.kt` to reflect the actual file composition (5 circuitSimulator files + ModuleSessionFactory)
- [ ] T038 [US5] Add `FlowGraphExecute` child codeNode inside the `flowGraph-execute` graphNode in `graphEditor/architecture.flow.kt` with 2 inputs (flowGraphModel, nodeDescriptors) and 3 outputs (executionState, animations, debugSnapshots), nodeType = "TRANSFORMER"
- [ ] T039 [US5] Add portMapping declarations wiring all 5 exposed ports to the child codeNode ports in `graphEditor/architecture.flow.kt`
- [ ] T040 [US5] Run `./gradlew :graphEditor:jvmTest --tests "io.codenode.grapheditor.characterization.ArchitectureFlowKtsTest"` to verify all architecture tests pass

**Checkpoint**: architecture.flow.kt updated. flowGraph-execute node contains FlowGraphExecute CodeNode with port mappings. All architecture tests pass.

---

## Phase 8: Verification & Cross-Cutting Concerns (US6)

**Purpose**: Verify dependency direction, no cycles, Strangler Fig compliance, full test suite

- [ ] T041 [US6] Verify no dependency on `:graphEditor` or `:circuitSimulator` in `flowGraph-execute/build.gradle.kts` — dependency direction is graphEditor → flowGraph-execute → flowGraph-inspect → fbpDsl
- [ ] T042 [US6] Verify no circular dependency exists by running `./gradlew :flowGraph-execute:dependencies` and confirming only fbpDsl and flowGraph-inspect appear
- [ ] T043 [US6] Verify Strangler Fig sequence in git history: module creation → file copy → TDD tests → CodeNode implementation → call site migration → original removal → architecture wiring
- [ ] T044 [US6] Verify RuntimePreviewPanel.kt remains unchanged in `graphEditor/src/jvmMain/kotlin/ui/RuntimePreviewPanel.kt`
- [ ] T045 [US6] Verify idePlugin compiles: `./gradlew :idePlugin:compileKotlinJvm`
- [ ] T046 Run full test suite: `./gradlew :graphEditor:jvmTest :flowGraph-types:jvmTest :flowGraph-persist:jvmTest :flowGraph-inspect:jvmTest :flowGraph-execute:jvmTest` to verify zero regressions
- [ ] T047 Run quickstart.md validation scenarios (Scenarios 1-15)

**Checkpoint**: All verification complete. No cycles. Strangler Fig pattern followed. Zero regressions.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — starts immediately
- **Phase 2 (Extract Files)**: Depends on Phase 1 completion (module must exist)
- **Phase 3 (TDD Tests)**: Depends on Phase 2 completion (files must be in module for CodeNode to reference)
- **Phase 4 (CodeNode Implementation)**: Depends on Phase 3 completion (TDD — tests must exist and fail first)
- **Phase 5 (Call Site Migration)**: Depends on Phase 4 completion (module must compile with CodeNode for graphEditor to depend on it)
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
- **Phase 5**: Call site update tasks (T021-T022) can run in parallel (different files)
- **Phase 6**: All 6 file removal tasks (T027-T032) can run in parallel
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
4. Phase 6: Originals removed → circuitSimulator absorbed
5. Phase 7: Architecture wired → visible in FlowGraph
6. Phase 8: Full verification → feature complete
