# Tasks: GraphEditor Refactoring Plan

**Input**: Design documents from `/specs/064-grapheditor-refactor-plan/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, quickstart.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup

**Purpose**: Create directory structure and placeholder files for deliverables

- [X] T001 Create characterization test directory at `graphEditor/src/jvmTest/kotlin/characterization/`
- [X] T002 [P] Create placeholder `graphEditor/ARCHITECTURE.md` with section headers (File Audit, Responsibility Buckets, Seam Matrix)
- [X] T003 [P] Create placeholder `graphEditor/MIGRATION.md` with section headers (Module Boundaries, Public APIs, Extraction Order, Step-by-Step Instructions)

**Checkpoint**: Directory structure and placeholder files exist

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish the bucket definitions and import-analysis methodology that all user stories depend on

**Warning**: No user story work can begin until this phase is complete

- [X] T004 Define the six responsibility bucket categories in `graphEditor/ARCHITECTURE.md`: compose, persist, execute, generate, inspect, root (composition root) — include a 2-3 sentence description of each bucket's scope based on research.md R1
- [X] T005 Document the multi-signal assignment methodology in `graphEditor/ARCHITECTURE.md`: primary type operated on, import analysis, @Composable annotation, cross-reference density — based on research.md R2

**Checkpoint**: Bucket definitions and assignment methodology documented — audit can now begin

---

## Phase 3: User Story 1 — Audit and Catalog graphEditor Files (Priority: P1) MVP

**Goal**: Catalog every `.kt` file in `graphEditor/src/jvmMain/kotlin/` into exactly one responsibility bucket with cross-bucket dependency mapping

**Independent Test**: Total file count in audit matches actual file count (77 files). Every file has exactly one bucket. All cross-bucket dependencies listed with source, target, type, and boundary.

### Implementation for User Story 1

- [X] T006 [US1] Audit all 25 files in `graphEditor/src/jvmMain/kotlin/ui/` — for each file, read imports and primary types, assign to one bucket, note cross-bucket dependencies. Record results in `graphEditor/ARCHITECTURE.md` audit table.
- [X] T007 [US1] Audit all 11 files in `graphEditor/src/jvmMain/kotlin/viewmodel/` — read imports and primary types, assign to one bucket, note cross-bucket dependencies. Record in `graphEditor/ARCHITECTURE.md`.
- [X] T008 [US1] Audit all 8 files in `graphEditor/src/jvmMain/kotlin/state/` — read imports and primary types, assign to one bucket, note cross-bucket dependencies. Record in `graphEditor/ARCHITECTURE.md`.
- [X] T009 [P] [US1] Audit all 6 files in `graphEditor/src/jvmMain/kotlin/model/` — read imports and primary types, assign to one bucket, note cross-bucket dependencies. Record in `graphEditor/ARCHITECTURE.md`.
- [X] T010 [P] [US1] Audit all 3 files in `graphEditor/src/jvmMain/kotlin/serialization/` — read imports and primary types, assign to one bucket, note cross-bucket dependencies. Record in `graphEditor/ARCHITECTURE.md`.
- [X] T011 [P] [US1] Audit all 3 files in `graphEditor/src/jvmMain/kotlin/compilation/` — read imports and primary types, assign to one bucket, note cross-bucket dependencies. Record in `graphEditor/ARCHITECTURE.md`.
- [X] T012 [P] [US1] Audit all 3 files in `graphEditor/src/jvmMain/kotlin/rendering/` — read imports and primary types, assign to one bucket, note cross-bucket dependencies. Record in `graphEditor/ARCHITECTURE.md`.
- [X] T013 [P] [US1] Audit all 2 files in `graphEditor/src/jvmMain/kotlin/repository/` and 1 file in `graphEditor/src/jvmMain/kotlin/save/` — read imports, assign buckets, note dependencies. Record in `graphEditor/ARCHITECTURE.md`.
- [X] T014 [US1] Audit all 9 files in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/` — read imports and primary types, assign to one bucket, note cross-bucket dependencies. Record in `graphEditor/ARCHITECTURE.md`.
- [X] T015 [US1] Audit all 5 files in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/` and `Main.kt` — read imports and primary types, assign to one bucket, note cross-bucket dependencies. Record in `graphEditor/ARCHITECTURE.md`.
- [X] T016 [US1] Build the seam matrix in `graphEditor/ARCHITECTURE.md` — consolidate all cross-bucket dependencies into a single table with columns: Source File, Target File, Dependency Type (function call / type reference / inheritance / state sharing), Source Bucket, Target Bucket, Boundary (e.g., compose→persist). Count seams per boundary.
- [X] T017 [US1] Validate audit completeness in `graphEditor/ARCHITECTURE.md` — verify total file count equals 77, every file appears exactly once, no files duplicated or missing. Add a summary section with file counts per bucket.

**Checkpoint**: ARCHITECTURE.md contains complete file audit with all 77 files assigned to buckets and all seams documented

---

## Phase 4: User Story 2 — Write Characterization Tests (Priority: P1)

**Goal**: Write characterization tests that pin current behavior at every identified seam, organized into four test classes

**Independent Test**: Run `./gradlew :graphEditor:jvmTest` — all characterization tests pass. Temporarily break a seam and verify at least one test fails.

### Implementation for User Story 2

- [X] T018 [US2] Write `GraphDataOpsCharacterizationTest.kt` in `graphEditor/src/jvmTest/kotlin/characterization/` — tests that construct FlowGraphs via GraphState, perform operations (addNode, connectPorts, validateConnection, cycle detection, create GraphNode with child nodes and port mappings), and assert on results. Pin current behavior for all graph data seams identified in the audit. Must run without Compose.
- [X] T019 [US2] Write `SerializationRoundTripCharacterizationTest.kt` in `graphEditor/src/jvmTest/kotlin/characterization/` — tests that serialize FlowGraphs to .flow.kts via FlowGraphSerializer, deserialize via FlowKtParser, and assert the round-trip produces equivalent graphs. Cover CodeNode, GraphNode (with child nodes, internal connections, port mappings), nested GraphNodes, all port types (input, output, passThru). Pin current serialization format.
- [X] T020 [US2] Write `RuntimeExecutionCharacterizationTest.kt` in `graphEditor/src/jvmTest/kotlin/characterization/` — tests that create a FlowGraph, wire and execute it, and assert on state transitions (IDLE→RUNNING→STOPPED). Cover execution start/stop, channel wiring, and execution state propagation. Use kotlinx-coroutines-test with virtual time where applicable.
- [X] T021 [US2] Write `ViewModelCharacterizationTest.kt` in `graphEditor/src/jvmTest/kotlin/characterization/` — tests that create ViewModels (GraphEditorViewModel, NodePaletteViewModel, PropertiesPanelViewModel), perform graph mutations through them, and assert on exposed state. Verify palette state, properties panel state, and canvas interaction state. Must run without Compose.
- [X] T022 [US2] Run `./gradlew :graphEditor:jvmTest` and verify all characterization tests pass alongside existing tests — no test regressions

**Checkpoint**: All four characterization test classes pass. Existing tests unaffected. Safety net is in place for future extraction.

---

## Phase 5: User Story 3 — Define Module Boundaries and Extraction Order (Priority: P2)

**Goal**: Create the migration map assigning every audited file to a target module, defining public APIs, and specifying safe extraction order

**Independent Test**: Every file in the audit has a target module assignment. Extraction order has no circular dependencies. Each module has interface definitions.

### Implementation for User Story 3

- [ ] T023 [US3] Define module boundaries in `graphEditor/MIGRATION.md` — for each of the five target modules (flowGraph-compose, flowGraph-persist, flowGraph-execute, flowGraph-generate, flowGraph-inspect), list which files from the audit move to that module and which files stay in graphEditor (composition root). Every file must appear in exactly one module assignment.
- [ ] T024 [US3] Define public APIs in `graphEditor/MIGRATION.md` — for each target module, specify the Kotlin interfaces that represent the functions graphEditor currently calls internally for that module's responsibility. Include interface name, method signatures, and which current call sites would use them.
- [ ] T025 [US3] Define extraction order in `graphEditor/MIGRATION.md` — document the five extraction steps (persist→inspect→execute→generate→compose per research.md R5). For each step specify: which files move, which interfaces are created, which graphEditor call sites change to delegation, and which characterization tests must pass.
- [ ] T026 [US3] Validate extraction order in `graphEditor/MIGRATION.md` — verify no step N depends on a module scheduled for step N+1 or later. Document the dependency justification for each step. Add a dependency diagram showing the extraction sequence.
- [ ] T027 [US3] Cross-validate migration map against audit in `graphEditor/MIGRATION.md` — verify every file in ARCHITECTURE.md appears in exactly one module assignment in MIGRATION.md. Verify seam boundaries from the audit align with the module interfaces defined. Document any discrepancies resolved.

**Checkpoint**: MIGRATION.md is complete with file assignments, interfaces, and validated extraction order

---

## Phase 6: User Story 4 — Create Meta-FlowGraph (Priority: P3)

**Goal**: Create a .flow.kts file representing the target architecture as a FlowGraph viewable in the graphEditor

**Independent Test**: Open architecture.flow.kts in graphEditor — it loads without errors, shows all five module nodes with connections matching the migration map.

### Implementation for User Story 4

- [ ] T028 [US4] Create `graphEditor/architecture.flow.kts` — write a FlowGraph in the existing .flow.kts DSL format with six nodes (flowGraph-compose, flowGraph-persist, flowGraph-execute, flowGraph-generate, flowGraph-inspect, graphEditor composition root). Define input/output ports per module matching the public APIs from MIGRATION.md. Connect ports to show inter-module data flow.
- [ ] T029 [US4] Verify `graphEditor/architecture.flow.kts` loads in the graphEditor — launch graphEditor, open the file, confirm all six nodes render on canvas with connections visible. Fix any parsing or rendering issues.
- [ ] T030 [US4] Cross-validate `graphEditor/architecture.flow.kts` against `graphEditor/MIGRATION.md` — verify one-to-one correspondence between FlowGraph connections and migration map dependencies. Document the mapping in a comment block at the top of the .flow.kts file.

**Checkpoint**: Meta-FlowGraph loads in graphEditor and visually represents the target architecture

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final validation across all deliverables

- [ ] T031 Run full validation per `specs/064-grapheditor-refactor-plan/quickstart.md` — execute all 8 scenarios and verify expected results
- [ ] T032 Run `./gradlew :graphEditor:jvmTest` — confirm all characterization tests and existing tests pass together
- [ ] T033 Review ARCHITECTURE.md and MIGRATION.md for readability — verify a developer unfamiliar with the codebase can understand the file assignments, seam analysis, module boundaries, and extraction order (SC-008)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup — defines bucket methodology before audit
- **US1 Audit (Phase 3)**: Depends on Foundational — needs bucket definitions to classify files
- **US2 Characterization Tests (Phase 4)**: Depends on Foundational — needs bucket definitions to identify seams. Can start in parallel with US1 once foundational is complete, but seam coverage validation (T022) benefits from the audit being done
- **US3 Migration Map (Phase 5)**: Depends on US1 — needs the complete file audit to assign files to modules
- **US4 Meta-FlowGraph (Phase 6)**: Depends on US3 — needs module boundaries and APIs to create accurate connections
- **Polish (Phase 7)**: Depends on all user stories complete

### User Story Dependencies

- **US1 (Audit)**: Can start after Foundational (Phase 2) — no story dependencies
- **US2 (Tests)**: Can start after Foundational (Phase 2) — independent of US1 but benefits from audit seam data
- **US3 (Migration Map)**: Depends on US1 completion — requires the full file audit
- **US4 (Meta-FlowGraph)**: Depends on US3 completion — requires module boundaries and APIs

### Within Each User Story

- US1: Directory scans (T006-T015) can partially parallelize by subdirectory; seam matrix (T016) and validation (T017) must follow
- US2: All four test classes (T018-T021) can be written in parallel; verification (T022) must follow all
- US3: Boundaries (T023) first, then APIs (T024) and order (T025) can partially parallelize, then validation (T026-T027)
- US4: Flow file creation (T028) first, then verification (T029) and cross-validation (T030) sequentially

### Parallel Opportunities

- T002 and T003 can run in parallel (different files)
- T009, T010, T011, T012, T013 can run in parallel (different subdirectories, same output file but different sections)
- T018, T019, T020, T021 can run in parallel (different test files)

---

## Parallel Example: User Story 2

```bash
# Launch all four characterization test classes in parallel:
Task: "Write GraphDataOpsCharacterizationTest.kt in graphEditor/src/jvmTest/kotlin/characterization/"
Task: "Write SerializationRoundTripCharacterizationTest.kt in graphEditor/src/jvmTest/kotlin/characterization/"
Task: "Write RuntimeExecutionCharacterizationTest.kt in graphEditor/src/jvmTest/kotlin/characterization/"
Task: "Write ViewModelCharacterizationTest.kt in graphEditor/src/jvmTest/kotlin/characterization/"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (bucket definitions)
3. Complete Phase 3: US1 — Audit all 77 files
4. **STOP and VALIDATE**: Verify audit completeness (77 files, no gaps, seam matrix complete)
5. The audit alone delivers significant value — it maps the monolith and identifies extraction boundaries

### Incremental Delivery

1. Setup + Foundational → Methodology ready
2. US1 (Audit) → Complete file catalog with seam map → **First deliverable**
3. US2 (Tests) → Characterization tests pinning behavior → **Safety net in place**
4. US3 (Migration Map) → Module boundaries, APIs, extraction order → **Actionable roadmap**
5. US4 (Meta-FlowGraph) → Visual architecture → **Living documentation**
6. Each story adds value without breaking previous deliverables

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- This feature produces documentation and test files, not production code
- Characterization tests must pass on the UNMODIFIED codebase — they pin existing behavior
- The audit and migration map are Markdown files in graphEditor/ root for proximity to the code they describe
- No Gradle modules are created in this feature — that is future extraction work
