# Tasks: Code Generation Flow Graphs

**Input**: Design documents from `/specs/080-codegen-flow-graphs/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, quickstart.md

**Tests**: Unit tests included for the runner and selection filter.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Create the GenerationResult and SelectionFilter data models used by all user stories.

- [X] T001 Create `GenerationResult` data class in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/runner/GenerationResult.kt`
- [X] T002 Create `SelectionFilter` data class with `fromFileTree()` factory in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/runner/SelectionFilter.kt`
- [X] T003 Add 6 unit tests for SelectionFilter in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/runner/SelectionFilterTest.kt`
- [X] T004 Compile and run: `./gradlew :flowGraph-generate:jvmTest` — all pass

**Checkpoint**: Data models ready. Flow graphs and runner can proceed.

---

## Phase 2: User Story 1 — Create Generation Flow Graphs (Priority: P1) 🎯 MVP

**Goal**: Create 3 generation flow graph files as compositions of Generator CodeNodes, loadable in the graph editor.

**Independent Test**: Open each flow graph in the graph editor. Verify correct nodes and connections.

### Implementation

- [X] T005 [P] [US1] Create `GenerateModule.flow.kt` — 7 generators fan-out from ConfigSource to ResultCollector in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/flow/GenerateModule.flow.kt`
- [X] T006 [P] [US1] Create `GenerateRepository.flow.kt` — 11 generators (7 module + 4 entity) fan-out in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/flow/GenerateRepository.flow.kt`
- [X] T007 [P] [US1] Create `GenerateUIFBP.flow.kt` — 9 generators (5 shared + 4 UI-FBP) fan-out in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/flow/GenerateUIFBP.flow.kt`
- [X] T008 [US1] Compile: `./gradlew :flowGraph-generate:compileKotlinJvm` — BUILD SUCCESSFUL

**Checkpoint**: All 3 flow graphs compile and are loadable in the graph editor.

---

## Phase 3: User Story 2 — CodeGenerationRunner (Priority: P2)

**Goal**: Create a runner that executes generation flow graphs via the FBP runtime (DynamicPipelineController), producing GenerationResult with parallel fan-out execution.

**Independent Test**: Execute runner with GenerateModule flow graph and test FlowGraph. Verify 7 non-empty content entries.

### Implementation

- [ ] T009 [US2] Create `CodeGenerationRunner` class with `execute(generationPath, config, selectionFilter)` that loads the generation flow graph, removes excluded nodes, builds a DynamicPipelineController, emits config through Source, collects outputs in Sink, and returns GenerationResult in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/runner/CodeGenerationRunner.kt`
- [ ] T010 [US2] Create Source CodeNode `GenerationConfigSourceNode` that emits a GenerationConfig into the pipeline in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/nodes/GenerationConfigSourceNode.kt`
- [ ] T011 [US2] Create Sink CodeNode `GenerationResultCollectorNode` that receives generated file contents and accumulates them into a GenerationResult in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/nodes/GenerationResultCollectorNode.kt`
- [ ] T012 [US2] Add unit test: runner with GenerateModule path produces 7 non-empty content entries in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/runner/CodeGenerationRunnerTest.kt`
- [ ] T013 [US2] Add unit test: runner output matches ModuleSaveService output for the same FlowGraph input in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/runner/CodeGenerationRunnerTest.kt`
- [ ] T014 [US2] Compile and run: `./gradlew :flowGraph-generate:jvmTest`

**Checkpoint**: Runner executes generation flow graphs via FBP runtime. Output matches ModuleSaveService.

---

## Phase 4: User Story 3 — File Tree Selection Mapping (Priority: P3)

**Goal**: File-tree checkbox deselections exclude corresponding Generator CodeNodes from execution.

**Independent Test**: Deselect "Controller.kt". Execute. Verify Controller output absent.

### Implementation

- [ ] T015 [US3] Wire `SelectionFilter.fromFileTree()` into `CodeGenerationRunner.execute()` — excluded generators are removed from the flow graph before building the pipeline in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/runner/CodeGenerationRunner.kt`
- [ ] T016 [US3] Add unit test: excluding "RuntimeControllerGenerator" via SelectionFilter produces 6 entries with Controller absent and listed in skipped in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/runner/CodeGenerationRunnerTest.kt`
- [ ] T017 [US3] Add unit test: excluding all generators produces empty GenerationResult with all listed in skipped in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/runner/CodeGenerationRunnerTest.kt`
- [ ] T018 [US3] Compile and run: `./gradlew :flowGraph-generate:jvmTest`

**Checkpoint**: Selective execution works. Checkbox deselections map to generator exclusion.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Full verification

- [ ] T019 Run full test suite: `./gradlew :flowGraph-generate:jvmTest :graphEditor:jvmTest`
- [ ] T020 Run quickstart.md verification scenarios VS1–VS6
- [ ] T021 Verify existing generators and ModuleSaveService are unchanged

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: No dependencies. BLOCKS US2 and US3.
- **User Story 1 (Phase 2)**: Depends on Foundational (data models). Can start in parallel with data model creation.
- **User Story 2 (Phase 3)**: Depends on US1 (flow graphs) + Foundational (data models).
- **User Story 3 (Phase 4)**: Depends on US2 (runner must exist to add filtering).
- **Polish (Phase 5)**: Depends on all user stories.

### Parallel Opportunities

```text
# US1 — 3 flow graphs (different files):
T005, T006, T007

# Foundational data models (different files):
T001, T002
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Foundational (T001–T004)
2. Complete Phase 2: User Story 1 (T005–T008) — flow graphs loadable in editor
3. **STOP and VALIDATE**: Open each flow graph, verify visual layout

### Incremental Delivery

1. Foundational → Data models ready
2. User Story 1 → 3 flow graphs (MVP — visual representation)
3. User Story 2 → Runner executes via FBP runtime with parallel fan-out
4. User Story 3 → Selective execution via file tree mapping
5. Polish → Full verification

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story
- The runner uses DynamicPipelineController (FBP runtime) — NOT a custom synchronous runner
- Fan-out topology parallelizes independent generators automatically
- Existing generators and ModuleSaveService are unchanged
- Commit after each phase
