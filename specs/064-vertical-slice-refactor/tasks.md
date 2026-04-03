# Tasks: Vertical Slice Refactor

**Input**: Design documents from `/specs/064-vertical-slice-refactor/`
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
- [X] T034 [P] Create characterization test directory at `kotlinCompiler/src/jvmTest/kotlin/characterization/`
- [X] T035 [P] Create placeholder `kotlinCompiler/ARCHITECTURE.md` with section headers matching graphEditor/ARCHITECTURE.md
- [X] T036 [P] Create placeholder `circuitSimulator/ARCHITECTURE.md` with section headers matching graphEditor/ARCHITECTURE.md
- [X] T037 [P] Create characterization test directory at `circuitSimulator/src/commonTest/kotlin/characterization/`
- [X] T038 Move `graphEditor/MIGRATION.md` to repo root `MIGRATION.md` — migration map spans all three modules

**Checkpoint**: Directory structure and placeholder files exist for all three modules

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish the bucket definitions and import-analysis methodology that all user stories depend on

**Warning**: No user story work can begin until this phase is complete

- [X] T004 Define the six responsibility bucket categories in `graphEditor/ARCHITECTURE.md`: compose, persist, execute, generate, inspect, root (composition root) — include a 2-3 sentence description of each bucket's scope based on research.md R1
- [X] T005 Document the multi-signal assignment methodology in `graphEditor/ARCHITECTURE.md`: primary type operated on, import analysis, @Composable annotation, cross-reference density — based on research.md R2

**Checkpoint**: Bucket definitions and assignment methodology documented — audit can now begin

---

## Phase 3: User Story 1 — Audit and Catalog All Module Files (Priority: P1) MVP

**Goal**: Catalog every `.kt` file across graphEditor, kotlinCompiler, and circuitSimulator into exactly one responsibility bucket with cross-bucket dependency mapping

**Independent Test**: Total file count in audit matches actual file count (~123 files). Every file has exactly one bucket. All cross-bucket dependencies listed with source, target, type, and boundary.

### graphEditor Audit (Complete)

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

### kotlinCompiler Audit (New)

- [X] T039 [US1] Audit all generator files in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/` (27 files) — for each file, read imports and primary types, assign to one bucket (all `generate`), note cross-bucket dependencies. Record results in `kotlinCompiler/ARCHITECTURE.md` audit table.
- [X] T040 [P] [US1] Audit all template files in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/templates/` (8 files) — read imports, assign buckets, note dependencies. Record in `kotlinCompiler/ARCHITECTURE.md`.
- [X] T041 [P] [US1] Audit validator and JVM-specific files in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/validator/` and `kotlinCompiler/src/jvmMain/kotlin/` (3 files) — read imports, assign buckets, note dependencies. Record in `kotlinCompiler/ARCHITECTURE.md`.
- [X] T042 [US1] Build seam matrix for `kotlinCompiler/ARCHITECTURE.md` — consolidate cross-bucket dependencies, noting cross-module seams (kotlinCompiler→graphEditor, kotlinCompiler→fbpDsl). Count seams per boundary.
- [X] T043 [US1] Validate kotlinCompiler audit completeness — verify total file count matches actual count (38 files), every file appears exactly once.

### circuitSimulator Audit (New)

- [X] T044 [US1] Audit all 5 files in `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/` — for each file, read imports and primary types, assign to one bucket (all `execute`), note cross-bucket dependencies. Record results in `circuitSimulator/ARCHITECTURE.md` audit table.
- [X] T045 [US1] Build seam matrix for `circuitSimulator/ARCHITECTURE.md` — consolidate cross-bucket dependencies, noting cross-module seams (circuitSimulator→fbpDsl, circuitSimulator→graphEditor). Count seams per boundary.
- [X] T046 [US1] Validate circuitSimulator audit completeness — verify total file count equals 5, every file appears exactly once.

### Cross-Module Validation

- [X] T047 [US1] Build consolidated cross-module seam summary — document all seams that cross module boundaries (graphEditor↔kotlinCompiler, graphEditor↔circuitSimulator) in a summary table. These become the primary extraction interfaces.
- [X] T048 [US1] Validate total audit completeness across all three modules — verify combined file count matches actual count (120 files), every file appears exactly once across the three ARCHITECTURE.md files.

**Checkpoint**: All three ARCHITECTURE.md files contain complete audits; cross-module seams documented

---

## Phase 4: User Story 2 — Write Characterization Tests (Priority: P1)

**Goal**: Write characterization tests that pin current behavior at every identified seam across all three modules

**Independent Test**: Run `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest` — all characterization tests pass. Temporarily break a seam and verify at least one test fails.

### graphEditor Characterization Tests (Complete)

- [X] T018 [US2] Write `GraphDataOpsCharacterizationTest.kt` in `graphEditor/src/jvmTest/kotlin/characterization/` — tests that construct FlowGraphs via GraphState, perform operations (addNode, connectPorts, validateConnection, cycle detection, create GraphNode with child nodes and port mappings), and assert on results. Pin current behavior for all graph data seams identified in the audit. Must run without Compose.
- [X] T019 [US2] Write `SerializationRoundTripCharacterizationTest.kt` in `graphEditor/src/jvmTest/kotlin/characterization/` — tests that serialize FlowGraphs to .flow.kts via FlowGraphSerializer, deserialize via FlowKtParser, and assert the round-trip produces equivalent graphs. Cover CodeNode, GraphNode (with child nodes, internal connections, port mappings), nested GraphNodes, all port types (input, output, passThru). Pin current serialization format.
- [X] T020 [US2] Write `RuntimeExecutionCharacterizationTest.kt` in `graphEditor/src/jvmTest/kotlin/characterization/` — tests that create a FlowGraph, wire and execute it, and assert on state transitions (IDLE→RUNNING→STOPPED). Cover execution start/stop, channel wiring, and execution state propagation. Use kotlinx-coroutines-test with virtual time where applicable.
- [X] T021 [US2] Write `ViewModelCharacterizationTest.kt` in `graphEditor/src/jvmTest/kotlin/characterization/` — tests that create ViewModels (GraphEditorViewModel, NodePaletteViewModel, PropertiesPanelViewModel), perform graph mutations through them, and assert on exposed state. Verify palette state, properties panel state, and canvas interaction state. Must run without Compose.
- [X] T022 [US2] Run `./gradlew :graphEditor:jvmTest` and verify all characterization tests pass alongside existing tests — no test regressions

### kotlinCompiler Characterization Tests (New)

- [X] T049 [P] [US2] Write `CodeGenerationCharacterizationTest.kt` in `kotlinCompiler/src/jvmTest/kotlin/characterization/` — tests that create known FlowGraphs, run them through KotlinCodeGenerator/ComponentGenerator, and assert on generated Kotlin source output. Pin current code generation behavior for component generation, flow generation, and module generation.
- [X] T050 [P] [US2] Write `FlowKtGeneratorCharacterizationTest.kt` in `kotlinCompiler/src/jvmTest/kotlin/characterization/` — tests that create FlowGraphs and run them through FlowKtGenerator, asserting on the generated .flow.kt content. Cover nodes with various port configurations, connections, and IP type overrides.
- [X] T051 [US2] Run `./gradlew :kotlinCompiler:jvmTest` and verify all characterization tests pass alongside existing tests — no test regressions

### circuitSimulator Characterization Tests (New)

- [X] T052 [P] [US2] Write `RuntimeSessionCharacterizationTest.kt` in `circuitSimulator/src/commonTest/kotlin/characterization/` — tests that create RuntimeSession instances with mock ModuleControllers, exercise start/stop/pause/resume lifecycle, and assert on execution state transitions. Pin animation controller and debugger observer behavior.
- [X] T053 [US2] Verify circuitSimulator characterization tests compile and pass — run appropriate Gradle test task

### Full Verification

- [X] T054 [US2] Run `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` and verify ALL characterization tests across all modules pass alongside existing tests — no regressions anywhere

**Checkpoint**: All characterization test classes pass across all modules. Existing tests unaffected. Safety net is in place for future extraction.

---

## Phase 5: User Story 3 — Define Module Boundaries and Extraction Order (Priority: P2)

**Goal**: Create the migration map assigning every audited file across all three modules to a target module, defining public APIs, and specifying safe extraction order

**Independent Test**: Every file in all three audits has a target module assignment. Extraction order has no circular dependencies. Each module has interface definitions.

### Implementation for User Story 3

- [ ] T023 [US3] Define module boundaries in `MIGRATION.md` (repo root) — for each of the five target modules (flowGraph-compose, flowGraph-persist, flowGraph-execute, flowGraph-generate, flowGraph-inspect), list which files from all three module audits move to that module and which files stay in graphEditor (composition root). Every file must appear in exactly one module assignment. Note which target modules absorb kotlinCompiler and circuitSimulator files.
- [ ] T024 [US3] Define public APIs in `MIGRATION.md` — for each target module, specify the Kotlin interfaces that represent the functions currently called across module boundaries. Include interface name, method signatures, and which current call sites would use them.
- [ ] T025 [US3] Define extraction order in `MIGRATION.md` — document the five extraction steps (persist→inspect→execute→generate→compose per research.md R5). For each step specify: which files move (from which source module), which interfaces are created, which call sites change to delegation, and which characterization tests must pass.
- [ ] T026 [US3] Validate extraction order in `MIGRATION.md` — verify no step N depends on a module scheduled for step N+1 or later. Document the dependency justification for each step. Add a dependency diagram showing the extraction sequence.
- [ ] T027 [US3] Cross-validate migration map against all three audits in `MIGRATION.md` — verify every file across graphEditor/ARCHITECTURE.md, kotlinCompiler/ARCHITECTURE.md, and circuitSimulator/ARCHITECTURE.md appears in exactly one module assignment. Verify seam boundaries align with module interfaces. Document any discrepancies resolved.

**Checkpoint**: MIGRATION.md is complete with file assignments from all three modules, interfaces, and validated extraction order

---

## Phase 6: User Story 4 — Create Meta-FlowGraph (Priority: P3)

**Goal**: Create a .flow.kts file representing the target architecture as a FlowGraph viewable in the graphEditor

**Independent Test**: Open architecture.flow.kts in graphEditor — it loads without errors, shows all five module nodes with connections matching the migration map.

### Implementation for User Story 4

- [ ] T028 [US4] Create `graphEditor/architecture.flow.kts` — write a FlowGraph in the existing .flow.kts DSL format with six nodes (flowGraph-compose, flowGraph-persist, flowGraph-execute, flowGraph-generate, flowGraph-inspect, graphEditor composition root). Define input/output ports per module matching the public APIs from MIGRATION.md. Connect ports to show inter-module data flow.
- [ ] T029 [US4] Verify `graphEditor/architecture.flow.kts` loads in the graphEditor — launch graphEditor, open the file, confirm all six nodes render on canvas with connections visible. Fix any parsing or rendering issues.
- [ ] T030 [US4] Cross-validate `graphEditor/architecture.flow.kts` against `MIGRATION.md` — verify one-to-one correspondence between FlowGraph connections and migration map dependencies. Document the mapping in a comment block at the top of the .flow.kts file.

**Checkpoint**: Meta-FlowGraph loads in graphEditor and visually represents the target architecture

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final validation across all deliverables

- [ ] T031 Run full validation per `specs/064-vertical-slice-refactor/quickstart.md` — execute all 8 scenarios and verify expected results
- [ ] T032 Run `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest` — confirm all characterization tests and existing tests pass together
- [ ] T033 Review all three ARCHITECTURE.md files and MIGRATION.md for readability — verify a developer unfamiliar with the codebase can understand the file assignments, seam analysis, module boundaries, and extraction order (SC-008)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup — defines bucket methodology before audit
- **US1 Audit (Phase 3)**: Depends on Foundational — needs bucket definitions to classify files. graphEditor audit is complete; kotlinCompiler and circuitSimulator audits are new work.
- **US2 Characterization Tests (Phase 4)**: Depends on Foundational — needs bucket definitions to identify seams. graphEditor tests complete; kotlinCompiler and circuitSimulator tests are new work.
- **US3 Migration Map (Phase 5)**: Depends on US1 — needs all three module audits complete to assign files to target modules
- **US4 Meta-FlowGraph (Phase 6)**: Depends on US3 — needs module boundaries and APIs to create accurate connections
- **Polish (Phase 7)**: Depends on all user stories complete

### User Story Dependencies

- **US1 (Audit)**: graphEditor complete. kotlinCompiler and circuitSimulator can start immediately (bucket definitions already established).
- **US2 (Tests)**: graphEditor complete. kotlinCompiler and circuitSimulator tests can start in parallel with their respective audits.
- **US3 (Migration Map)**: Depends on ALL audits complete (graphEditor + kotlinCompiler + circuitSimulator)
- **US4 (Meta-FlowGraph)**: Depends on US3 completion — requires module boundaries and APIs

### Within Each User Story

- US1: graphEditor done; kotlinCompiler audit (T039-T043) and circuitSimulator audit (T044-T046) can run in parallel; cross-module validation (T047-T048) follows both
- US2: graphEditor done; kotlinCompiler tests (T049-T051) and circuitSimulator tests (T052-T053) can run in parallel; full verification (T054) follows all
- US3: Boundaries (T023) first, then APIs (T024) and order (T025) can partially parallelize, then validation (T026-T027)
- US4: Flow file creation (T028) first, then verification (T029) and cross-validation (T030) sequentially

### Parallel Opportunities

- T034, T035, T036, T037, T038 can run in parallel (different files, setup tasks)
- T039, T040, T041 can partially parallelize (different subdirectories in kotlinCompiler)
- T044 can run in parallel with T039-T041 (different module)
- T049, T050, T052 can run in parallel (different test files in different modules)

---

## Implementation Strategy

### Resuming from Current State

The graphEditor audit (T006-T017) and graphEditor characterization tests (T018-T022) are complete. The next work is:

1. Complete Phase 1 new setup tasks (T034-T038) — create directories and placeholders for kotlinCompiler and circuitSimulator
2. Complete US1 kotlinCompiler audit (T039-T043) and circuitSimulator audit (T044-T046) in parallel
3. Cross-module validation (T047-T048)
4. Complete US2 kotlinCompiler tests (T049-T051) and circuitSimulator tests (T052-T053) — can partially overlap with audits
5. Full test verification (T054)
6. Proceed to US3 (Migration Map) with all three audits and test suites complete

### Incremental Delivery

1. ~~Setup + Foundational → Methodology ready~~ (DONE)
2. ~~US1 graphEditor Audit → 77 files cataloged~~ (DONE)
3. ~~US2 graphEditor Tests → Safety net for graphEditor~~ (DONE)
4. US1 kotlinCompiler + circuitSimulator Audit → All ~123 files cataloged → **Complete audit**
5. US2 kotlinCompiler + circuitSimulator Tests → Full safety net → **Complete test coverage**
6. US3 (Migration Map) → Module boundaries, APIs, extraction order → **Actionable roadmap**
7. US4 (Meta-FlowGraph) → Visual architecture → **Living documentation**

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- This feature produces documentation and test files, not production code
- Characterization tests must pass on the UNMODIFIED codebase — they pin existing behavior
- Each module's ARCHITECTURE.md sits in its own module root for proximity to the code it describes
- MIGRATION.md sits at repo root because it spans all three source modules
- No Gradle modules are created in this feature — that is future extraction work
- kotlinCompiler (41 main files) maps primarily to flowGraph-generate; circuitSimulator (5 files) maps primarily to flowGraph-execute
