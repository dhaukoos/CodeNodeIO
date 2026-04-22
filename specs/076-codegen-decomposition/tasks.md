# Tasks: Unified Configurable Code Generation

**Input**: Design documents from `/specs/076-codegen-decomposition/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, quickstart.md

**Tests**: Unit tests included for the GenerationFileTree model and CodeGeneratorViewModel.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Create the GenerationFileTree data model used by both the dependency analysis (US1) and the Code Generator panel (US2).

**⚠️ CRITICAL**: The file tree model is shared infrastructure for US1 (documents reference it) and US2 (panel renders it).

- [X] T001 Create `GenerationPath` enum (GENERATE_MODULE, REPOSITORY, UI_FBP), `GenerationFileTree`, `FolderNode`, `FileNode`, and `TriState` data model in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/model/GenerationFileTree.kt`
- [X] T002 Add `toggleFolder(folderName)` and `toggleFile(folderName, fileName)` methods to `GenerationFileTree` that correctly update folder TriState (ALL/NONE/PARTIAL) when children change in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/model/GenerationFileTree.kt`
- [X] T003 Add unit tests for GenerationFileTree: folder toggle selects/deselects all children, individual file toggle updates parent to PARTIAL, empty tree handling in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/model/GenerationFileTreeTest.kt`
- [X] T004 Compile and run tests: `./gradlew :flowGraph-generate:jvmTest`

**Checkpoint**: GenerationFileTree model is ready. US1 and US2 can proceed.

---

## Phase 2: User Story 1 — Code Generation Dependency Analysis (Priority: P1) 🎯 MVP

**Goal**: Produce a dependency analysis document cataloguing all 22+ generators, their inputs/outputs/dependencies, a proposed folder hierarchy, and identification of module scaffolding as the root prerequisite.

**Independent Test**: Open `dependency-analysis.md` and verify every generator class from flowGraph-generate is listed. Verify the dependency graph is acyclic. Verify the proposed folder hierarchy maps every file from the Addresses module.

### Implementation

- [X] T005 [US1] Catalogue all generator classes in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/` — list each class name, file path, inputs, outputs (generated files), and dependencies in `specs/076-codegen-decomposition/dependency-analysis.md`
- [X] T006 [US1] Catalogue the orchestrator classes (`ModuleSaveService.saveModule()`, `saveEntityModule()`, `UIFBPInterfaceGenerator.generateAll()`) with their generator call sequences in `specs/076-codegen-decomposition/dependency-analysis.md`
- [X] T007 [US1] Draw a dependency graph (Mermaid or ASCII) showing directed edges from prerequisite generators to dependent generators, with Module Scaffolding as the root node in `specs/076-codegen-decomposition/dependency-analysis.md`
- [X] T008 [US1] Identify module scaffolding as a distinct prerequisite component (Module name from top bar → directory structure + gradle files) and document its extraction boundary in `specs/076-codegen-decomposition/dependency-analysis.md`
- [X] T009 [US1] Propose refined folder hierarchy: map every file from the existing Addresses module into the new structure (flow/, controller/, viewmodel/, nodes/, userInterface/, persistence/) in `specs/076-codegen-decomposition/dependency-analysis.md`
- [X] T010 [US1] Identify which generators could become independent CodeNodes vs GraphNodes in a future code-generation flow graph in `specs/076-codegen-decomposition/dependency-analysis.md`
- [X] T011 [US1] Create file tree builders for each generation path — static methods that produce `GenerationFileTree` for Generate Module, Repository, and UI-FBP paths given appropriate inputs in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/model/GenerationFileTreeBuilder.kt`

**Checkpoint**: Dependency analysis document is complete. Every generator is catalogued, dependencies mapped, folder hierarchy proposed.

---

## Phase 3: User Story 2 — Code Generator UI Panel Prototype (Priority: P2)

**Goal**: New "Code Generator" panel on the left side with path selection, input-specific dropdowns, file-tree checkboxes, and relocated repository buttons. Feature-gated under Pro tier.

**Independent Test**: Launch graph editor on Pro tier. Code Generator panel visible. Select each path — file tree populates correctly. Folder checkboxes toggle children. Repository buttons relocated from Properties Panel.

### Implementation

- [ ] T012 [P] [US2] Create `CodeGeneratorViewModel` with `MutableStateFlow<CodeGeneratorPanelState>`, path selection, IP Type selection, UI file selection, and file tree population methods in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/viewmodel/CodeGeneratorViewModel.kt`
- [ ] T013 [P] [US2] Create `CodeGeneratorPanel` composable following NodeGeneratorPanel pattern — CollapsiblePanel LEFT, 220.dp width, path dropdown, input-specific section (FlowGraph name label / IP Type dropdown / file chooser), scrollable file-tree with tri-state checkboxes, action buttons in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/CodeGeneratorPanel.kt`
- [ ] T014 [US2] Add `isCodeGeneratorExpanded` state and `CodeGeneratorViewModel` instantiation in `GraphEditorApp.kt`, pass featureGate for Pro tier gating in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorApp.kt`
- [ ] T015 [US2] Add CodeGeneratorPanel to the left column in `GraphEditorLayout.kt` alongside NodeGeneratorPanel and IPGeneratorPanel, passing featureGate to conditionally show/hide based on Pro tier in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorLayout.kt`
- [ ] T016 [US2] Remove "Create Repository Module" and "Remove Repository Module" buttons from `PropertiesPanel.kt` (IPTypePropertiesPanel section) in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/PropertiesPanel.kt`
- [ ] T017 [US2] Add "Create Repository Module" and "Remove Repository Module" buttons to `CodeGeneratorPanel.kt`, shown only when path = REPOSITORY and an IP Type is selected, wired through existing callbacks in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/CodeGeneratorPanel.kt`
- [ ] T018 [US2] Wire file tree population in `CodeGeneratorViewModel` — when path or input changes, call the appropriate `GenerationFileTreeBuilder` method to populate the file tree in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/viewmodel/CodeGeneratorViewModel.kt`
- [ ] T019 [US2] Compile and run tests: `./gradlew :graphEditor:compileKotlinJvm :graphEditor:jvmTest`

**Checkpoint**: Code Generator panel is fully interactive. Path selection populates file tree. Checkboxes toggle correctly. Repository buttons relocated.

---

## Phase 4: User Story 3 — Migration Plan Document (Priority: P3)

**Goal**: Produce a migration plan document describing the sequence of future features to evolve from monolithic to configurable code generation.

**Independent Test**: Open `migration-plan.md` and verify at least 3 future feature steps. Each step has scope, prerequisites, and deliverables.

### Implementation

- [ ] T020 [US3] Write migration plan step 1: Folder Hierarchy Migration — describe moving generated files to the new subdirectory structure in `specs/076-codegen-decomposition/migration-plan.md`
- [ ] T021 [US3] Write migration plan step 2: Module Scaffolding Extraction — describe extracting directory/gradle creation as a standalone component in `specs/076-codegen-decomposition/migration-plan.md`
- [ ] T022 [US3] Write migration plan step 3: Generator CodeNode Wrappers — describe wrapping individual generators as CodeNodes with typed ports in `specs/076-codegen-decomposition/migration-plan.md`
- [ ] T023 [US3] Write migration plan step 4: Code Generation FlowGraph — describe creating a flow graph that orchestrates code generation using the wrapped CodeNodes in `specs/076-codegen-decomposition/migration-plan.md`
- [ ] T024 [US3] Write migration plan step 5: Wire Code Generator Panel — describe connecting the panel file-tree selections to the generation flow graph in `specs/076-codegen-decomposition/migration-plan.md`
- [ ] T025 [US3] Add prerequisites and dependency arrows between migration steps, ensuring each step is independently spec-able and implementable in `specs/076-codegen-decomposition/migration-plan.md`

**Checkpoint**: Migration plan document is complete with 5+ ordered steps, each with clear scope, prerequisites, and deliverables.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Full verification across all quickstart scenarios

- [ ] T026 Run full test suite: `./gradlew :flowGraph-generate:jvmTest :graphEditor:jvmTest`
- [ ] T027 Run quickstart.md verification scenarios VS1–VS11
- [ ] T028 Verify dependency analysis covers all 22+ generators with no gaps
- [ ] T029 Verify Code Generator panel is hidden on Free tier, visible on Pro/Sim tier

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: No dependencies — can start immediately. BLOCKS US1 and US2 (file tree model needed).
- **User Story 1 (Phase 2)**: Depends on Foundational — needs GenerationFileTree model for tree builders
- **User Story 2 (Phase 3)**: Depends on Foundational + US1 — needs file tree model and tree builders
- **User Story 3 (Phase 4)**: Depends on US1 — migration plan references the dependency analysis
- **Polish (Phase 5)**: Depends on all user stories being complete

### Within Each Phase

- US1: Documentation tasks (T005–T010) can proceed in parallel, then tree builders (T011)
- US2: ViewModel (T012) and Panel (T013) in parallel, then wiring (T014–T018) sequentially
- US3: Documentation tasks (T020–T025) are sequential (each step builds on prior)

### Parallel Opportunities

```text
# Foundational:
T001, T002 (same file but sequential — model then methods)
T003 (tests after model)

# US1 documentation tasks:
T005, T006, T007, T008, T009, T010 (all writing to same doc but independent sections)

# US2 initial implementation:
T012, T013 (ViewModel and Panel in parallel — different files)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Foundational (T001–T004) — file tree model
2. Complete Phase 2: User Story 1 (T005–T011) — dependency analysis + tree builders
3. **STOP and VALIDATE**: Document covers all generators, dependency graph is correct, folder hierarchy maps all files
4. This is independently valuable — the analysis document guides all future decomposition work

### Incremental Delivery

1. Foundational → GenerationFileTree model ready
2. User Story 1 → Dependency analysis document (MVP!)
3. User Story 2 → Code Generator panel prototype
4. User Story 3 → Migration plan document
5. Polish → Full verification pass

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US1 is primarily documentation (dependency-analysis.md) plus one code artifact (GenerationFileTreeBuilder)
- US2 is the UI prototype — functional panel with no generation wiring
- US3 is purely documentation (migration-plan.md)
- The "Generate" button in the Code Generator panel is deliberately disabled in this feature
- Repository buttons move from PropertiesPanel to CodeGeneratorPanel — existing callbacks are reused
- Commit after each phase completion
