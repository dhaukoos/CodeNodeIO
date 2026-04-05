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
- [X] T003 [P] Create placeholder `MIGRATION.md` at repo root with section headers (Module Boundaries, Public APIs, Extraction Order, Step-by-Step Instructions)
- [X] T004 [P] Create characterization test directory at `kotlinCompiler/src/jvmTest/kotlin/characterization/`
- [X] T005 [P] Create placeholder `kotlinCompiler/ARCHITECTURE.md` with section headers matching graphEditor/ARCHITECTURE.md
- [X] T006 [P] Create placeholder `circuitSimulator/ARCHITECTURE.md` with section headers matching graphEditor/ARCHITECTURE.md
- [X] T007 [P] Create characterization test directory at `circuitSimulator/src/commonTest/kotlin/characterization/`

**Checkpoint**: Directory structure and placeholder files exist for all three modules

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish the bucket definitions and import-analysis methodology that all user stories depend on

**Warning**: No user story work can begin until this phase is complete

- [X] T008 Define the seven responsibility bucket categories in `graphEditor/ARCHITECTURE.md`: types, compose, persist, execute, generate, inspect, root (composition root) — include a 2-3 sentence description of each bucket's scope based on research.md R1
- [X] T009 Document the multi-signal assignment methodology in `graphEditor/ARCHITECTURE.md`: primary type operated on, import analysis, @Composable annotation, cross-reference density — based on research.md R2

**Checkpoint**: Bucket definitions and assignment methodology documented — audit can now begin

---

## Phase 3: User Story 1 — Audit and Catalog All Module Files (Priority: P1) MVP

**Goal**: Catalog every `.kt` file across graphEditor, kotlinCompiler, and circuitSimulator into exactly one responsibility bucket with cross-bucket dependency mapping

**Independent Test**: Total file count in audit matches actual file count (~120 files). Every file has exactly one bucket. All cross-bucket dependencies listed with source, target, type, and boundary.

### graphEditor Audit

- [X] T010 [US1] Audit all 25 files in `graphEditor/src/jvmMain/kotlin/ui/` — for each file, read imports and primary types, assign to one bucket, note cross-bucket dependencies. Record results in `graphEditor/ARCHITECTURE.md` audit table.
- [X] T011 [US1] Audit all 11 files in `graphEditor/src/jvmMain/kotlin/viewmodel/` — read imports and primary types, assign to one bucket, note cross-bucket dependencies. Record in `graphEditor/ARCHITECTURE.md`.
- [X] T012 [US1] Audit all 8 files in `graphEditor/src/jvmMain/kotlin/state/` — read imports and primary types, assign to one bucket, note cross-bucket dependencies. Record in `graphEditor/ARCHITECTURE.md`.
- [X] T013 [P] [US1] Audit all 6 files in `graphEditor/src/jvmMain/kotlin/model/` — read imports and primary types, assign to one bucket, note cross-bucket dependencies. Record in `graphEditor/ARCHITECTURE.md`.
- [X] T014 [P] [US1] Audit all 3 files in `graphEditor/src/jvmMain/kotlin/serialization/` — read imports and primary types, assign to one bucket, note cross-bucket dependencies. Record in `graphEditor/ARCHITECTURE.md`.
- [X] T015 [P] [US1] Audit all 3 files in `graphEditor/src/jvmMain/kotlin/compilation/` — read imports and primary types, assign to one bucket, note cross-bucket dependencies. Record in `graphEditor/ARCHITECTURE.md`.
- [X] T016 [P] [US1] Audit all 3 files in `graphEditor/src/jvmMain/kotlin/rendering/` — read imports and primary types, assign to one bucket, note cross-bucket dependencies. Record in `graphEditor/ARCHITECTURE.md`.
- [X] T017 [P] [US1] Audit all 2 files in `graphEditor/src/jvmMain/kotlin/repository/` and 1 file in `graphEditor/src/jvmMain/kotlin/save/` — read imports, assign buckets, note dependencies. Record in `graphEditor/ARCHITECTURE.md`.
- [X] T018 [US1] Audit all 9 files in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/` — read imports and primary types, assign to one bucket, note cross-bucket dependencies. Record in `graphEditor/ARCHITECTURE.md`.
- [X] T019 [US1] Audit all 5 files in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/` and `Main.kt` — read imports and primary types, assign to one bucket, note cross-bucket dependencies. Record in `graphEditor/ARCHITECTURE.md`.
- [X] T020 [US1] Build the seam matrix in `graphEditor/ARCHITECTURE.md` — consolidate all cross-bucket dependencies into a single table with columns: Source File, Target File, Dependency Type, Source Bucket, Target Bucket, Boundary. Count seams per boundary.
- [X] T021 [US1] Validate audit completeness in `graphEditor/ARCHITECTURE.md` — verify total file count equals 77, every file appears exactly once, no files duplicated or missing. Add summary section with file counts per bucket.

### kotlinCompiler Audit

- [X] T022 [US1] Audit all generator files in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/` (27 files) — for each file, read imports and primary types, assign to one bucket (primarily `generate`), note cross-bucket dependencies. Record results in `kotlinCompiler/ARCHITECTURE.md` audit table.
- [X] T023 [P] [US1] Audit all template files in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/templates/` (8 files) — read imports, assign buckets, note dependencies. Record in `kotlinCompiler/ARCHITECTURE.md`.
- [X] T024 [P] [US1] Audit validator and JVM-specific files in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/validator/` and `kotlinCompiler/src/jvmMain/kotlin/` (3 files) — read imports, assign buckets, note dependencies. Record in `kotlinCompiler/ARCHITECTURE.md`.
- [X] T025 [US1] Build seam matrix for `kotlinCompiler/ARCHITECTURE.md` — consolidate cross-bucket dependencies, noting cross-module seams (kotlinCompiler→graphEditor, kotlinCompiler→fbpDsl). Count seams per boundary.
- [X] T026 [US1] Validate kotlinCompiler audit completeness — verify total file count matches actual count (38 files), every file appears exactly once.

### circuitSimulator Audit

- [X] T027 [US1] Audit all 5 files in `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/` — for each file, read imports and primary types, assign to one bucket (primarily `execute`), note cross-bucket dependencies. Record results in `circuitSimulator/ARCHITECTURE.md` audit table.
- [X] T028 [US1] Build seam matrix for `circuitSimulator/ARCHITECTURE.md` — consolidate cross-bucket dependencies, noting cross-module seams (circuitSimulator→fbpDsl, circuitSimulator→graphEditor). Count seams per boundary.
- [X] T029 [US1] Validate circuitSimulator audit completeness — verify total file count equals 5, every file appears exactly once.

### Cross-Module Validation

- [X] T030 [US1] Build consolidated cross-module seam summary — document all seams that cross module boundaries (graphEditor↔kotlinCompiler, graphEditor↔circuitSimulator) in a summary table. These become the primary extraction interfaces.
- [X] T031 [US1] Validate total audit completeness across all three modules — verify combined file count matches actual count (120 files), every file appears exactly once across the three ARCHITECTURE.md files.

**Checkpoint**: All three ARCHITECTURE.md files contain complete audits; cross-module seams documented

---

## Phase 4: User Story 2 — Write Characterization Tests (Priority: P1)

**Goal**: Write characterization tests that pin current behavior at every identified seam across all three modules

**Independent Test**: Run `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest` — all characterization tests pass. Temporarily break a seam and verify at least one test fails.

### graphEditor Characterization Tests

- [X] T032 [US2] Write `GraphDataOpsCharacterizationTest.kt` in `graphEditor/src/jvmTest/kotlin/characterization/` — tests that construct FlowGraphs via GraphState, perform operations (addNode, connectPorts, validateConnection, cycle detection, create GraphNode with child nodes and port mappings), and assert on results. Pin current behavior for all graph data seams. Must run without Compose.
- [X] T033 [US2] Write `SerializationRoundTripCharacterizationTest.kt` in `graphEditor/src/jvmTest/kotlin/characterization/` — tests that serialize FlowGraphs to .flow.kt via FlowGraphSerializer, deserialize via FlowKtParser, and assert the round-trip produces equivalent graphs. Cover CodeNode, GraphNode, nested GraphNodes, all port types.
- [X] T034 [US2] Write `RuntimeExecutionCharacterizationTest.kt` in `graphEditor/src/jvmTest/kotlin/characterization/` — tests that create a FlowGraph, wire and execute it, and assert on state transitions (IDLE→RUNNING→STOPPED). Use kotlinx-coroutines-test with virtual time where applicable.
- [X] T035 [US2] Write `ViewModelCharacterizationTest.kt` in `graphEditor/src/jvmTest/kotlin/characterization/` — tests that create ViewModels, perform graph mutations through them, and assert on exposed state. Must run without Compose.
- [X] T036 [US2] Run `./gradlew :graphEditor:jvmTest` and verify all characterization tests pass alongside existing tests — no test regressions

### kotlinCompiler Characterization Tests

- [X] T037 [P] [US2] Write `CodeGenerationCharacterizationTest.kt` in `kotlinCompiler/src/jvmTest/kotlin/characterization/` — tests that create known FlowGraphs, run them through generators, and assert on generated Kotlin source output. Pin current code generation behavior.
- [X] T038 [P] [US2] Write `FlowKtGeneratorCharacterizationTest.kt` in `kotlinCompiler/src/jvmTest/kotlin/characterization/` — tests that create FlowGraphs and run them through FlowKtGenerator, asserting on generated .flow.kt content.
- [X] T039 [US2] Run `./gradlew :kotlinCompiler:jvmTest` and verify all characterization tests pass alongside existing tests — no test regressions

### circuitSimulator Characterization Tests

- [X] T040 [P] [US2] Write `RuntimeSessionCharacterizationTest.kt` in `circuitSimulator/src/commonTest/kotlin/characterization/` — tests that create RuntimeSession instances, exercise start/stop/pause/resume lifecycle, and assert on execution state transitions.
- [X] T041 [US2] Verify circuitSimulator characterization tests compile and pass

### Full Verification

- [X] T042 [US2] Run `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` and verify ALL characterization tests across all modules pass — no regressions anywhere

**Checkpoint**: All characterization test classes pass across all modules. Safety net is in place for future extraction.

---

## Phase 5: User Story 3 — Define Module Boundaries and Extraction Order (Priority: P2)

**Goal**: Create the migration map assigning every audited file across all three modules to a target module, defining public APIs, and specifying safe extraction order

**Independent Test**: Every file in all three audits has a target module assignment. Extraction order has no circular dependencies. Each module has interface definitions.

### Implementation for User Story 3

- [X] T043 [US3] Define module boundaries in `MIGRATION.md` (repo root) — for each of the six target modules (flowGraph-types, flowGraph-compose, flowGraph-persist, flowGraph-execute, flowGraph-generate, flowGraph-inspect), list which files from all three module audits move to that module and which files stay in graphEditor (composition root with source/sink split). Every file must appear in exactly one module assignment. File counts: types:9, inspect:13, persist:8, compose:10, execute:7, generate:46, root:27 = 120.
- [X] T044 [US3] Define public APIs in `MIGRATION.md` — for each target module, specify the Kotlin interfaces that represent the functions currently called across module boundaries. Include interface name, method signatures, and which current call sites would use them.
- [X] T045 [US3] Define extraction order in `MIGRATION.md` — document the six extraction steps (types→persist→inspect→execute→generate→compose per research.md R5). For each step specify: which files move, which interfaces are created, which call sites change to delegation, and which characterization tests must pass.
- [X] T046 [US3] Validate extraction order in `MIGRATION.md` — verify no step N depends on a module scheduled for step N+1 or later. Document the dependency justification for each step. Add a dependency diagram showing the extraction sequence.
- [X] T047 [US3] Cross-validate migration map against all three audits in `MIGRATION.md` — verify every file across graphEditor/ARCHITECTURE.md, kotlinCompiler/ARCHITECTURE.md, and circuitSimulator/ARCHITECTURE.md appears in exactly one module assignment. Verify seam boundaries align with module interfaces.

**Checkpoint**: MIGRATION.md is complete with file assignments from all three modules, interfaces, and validated extraction order

---

## Phase 6: User Story 4 — Create Architecture FlowGraph as Executable Blueprint (Priority: P2)

**Goal**: Create `architecture.flow.kt` with eight GraphNode containers, 19 connections forming a validated DAG, serving as both target blueprint and Phase B scaffold

**Independent Test**: Open `architecture.flow.kt` in graphEditor — it loads without errors, shows all eight nodes with 19 connections. Run ArchitectureFlowKtsTest — all 10 structural invariant tests pass.

### Implementation for User Story 4

- [X] T048 [US4] Create `graphEditor/architecture.flow.kt` — write a FlowGraph in the .flow.kt DSL format with eight GraphNode containers (flowGraph-types, flowGraph-compose, flowGraph-persist, flowGraph-execute, flowGraph-generate, flowGraph-inspect, graphEditor-source, graphEditor-sink). Define typed input/output ports per module matching the public APIs from MIGRATION.md. Connect ports to show inter-module data flow (19 connections). Use data-oriented port naming (nodeDescriptors, ipTypeMetadata, flowGraphModel, etc.).
- [X] T049 [US4] Verify `graphEditor/architecture.flow.kt` loads in the graphEditor — launch graphEditor, open the file, confirm all eight nodes render on canvas with 19 connections visible. Fix any parsing or rendering issues.
- [X] T050 [US4] Cross-validate `graphEditor/architecture.flow.kt` against `MIGRATION.md` — verify one-to-one correspondence between FlowGraph connections and migration map dependencies. Document the mapping in a comment block at the top of the .flow.kt file.
- [X] T051 [US4] Write `ArchitectureFlowKtsTest.kt` in `graphEditor/src/jvmTest/kotlin/characterization/` — 10 tests validating structural invariants: parses successfully, correct graph name, all 8 nodes, exactly 19 connections, types and inspect as hub sources, graphEditor-source has only command outputs, graphEditor-sink has only state inputs, all workflow modules receive flowGraphModel from source, no cycles (DFS), target platforms specified.
- [X] T052 [US4] Run `./gradlew :graphEditor:jvmTest --tests "characterization.ArchitectureFlowKtsTest"` and verify all 10 structural invariant tests pass

**Checkpoint**: Architecture FlowGraph loads in graphEditor and represents the target architecture as a validated DAG with 8 nodes and 19 connections

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final validation across all deliverables

- [X] T053 Run full validation per `specs/064-vertical-slice-refactor/quickstart.md` — execute all 9 scenarios and verify expected results
- [X] T054 Run `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest` — confirm all characterization tests and existing tests pass together
- [X] T055 Review all three ARCHITECTURE.md files and MIGRATION.md for readability — verify a developer unfamiliar with the codebase can understand the file assignments, seam analysis, module boundaries, and extraction order (SC-008)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup — defines bucket methodology before audit
- **US1 Audit (Phase 3)**: Depends on Foundational — needs bucket definitions to classify files
- **US2 Characterization Tests (Phase 4)**: Depends on Foundational — needs bucket definitions to identify seams
- **US3 Migration Map (Phase 5)**: Depends on US1 — needs all three module audits complete to assign files to target modules
- **US4 Architecture FlowGraph (Phase 6)**: Depends on US3 — needs module boundaries and APIs to create accurate connections
- **Polish (Phase 7)**: Depends on all user stories complete

### User Story Dependencies

- **US1 (Audit)**: Can start after Foundational (Phase 2)
- **US2 (Tests)**: Can start after Foundational (Phase 2) — can run in parallel with US1
- **US3 (Migration Map)**: Depends on US1 completion — requires ALL audits complete
- **US4 (Architecture FlowGraph)**: Depends on US3 completion — requires module boundaries and APIs

### Within Each User Story

- US1: graphEditor audit first; kotlinCompiler (T022-T026) and circuitSimulator (T027-T029) can run in parallel; cross-module validation (T030-T031) follows both
- US2: graphEditor tests first; kotlinCompiler tests (T037-T039) and circuitSimulator tests (T040-T041) can run in parallel; full verification (T042) follows all
- US3: Boundaries (T043) first, then APIs (T044) and order (T045) can partially parallelize, then validation (T046-T047)
- US4: Flow file creation (T048) first, then verification (T049), cross-validation (T050), tests (T051), and test run (T052) sequentially

### Parallel Opportunities

- T004, T005, T006, T007 can run in parallel (different files, setup tasks)
- T022, T023, T024 can partially parallelize (different subdirectories in kotlinCompiler)
- T027 can run in parallel with T022-T024 (different module)
- T037, T038, T040 can run in parallel (different test files in different modules)

---

## Implementation Strategy

### Current State

All tasks through Phase 6 (US4) are complete. The remaining work is Phase 7 polish tasks (T053-T055).

### Incremental Delivery

1. ~~Setup + Foundational → Methodology ready~~ (DONE)
2. ~~US1 Audit → All ~120 files cataloged~~ (DONE)
3. ~~US2 Characterization Tests → Full safety net~~ (DONE)
4. ~~US3 Migration Map → Module boundaries, APIs, extraction order~~ (DONE)
5. ~~US4 Architecture FlowGraph → Executable blueprint with validated DAG~~ (DONE)
6. Polish (T053-T055) → Final validation → **Phase A complete**

### What Comes Next (Phase B — separate features)

After Phase A completes, seven Phase B features execute the extraction:
1. flowGraph-types (9 files) — IP type lifecycle
2. flowGraph-persist (8 files) — save/load workflow
3. flowGraph-inspect (13 files) — node discovery and examination
4. flowGraph-execute (7 files) — runtime pipeline
5. flowGraph-generate (46 files) — code generation
6. flowGraph-compose (10 files) — interactive graph building
7. graphEditor shell (27 files) — source/sink composition root

Each Phase B feature extracts code, wraps as a coarse-grained CodeNode, and populates the corresponding empty GraphNode container in `architecture.flow.kt`.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- This feature (Phase A) produces documentation and test files, not production code
- Characterization tests must pass on the UNMODIFIED codebase — they pin existing behavior
- Each module's ARCHITECTURE.md sits in its own module root for proximity to the code it describes
- MIGRATION.md sits at repo root because it spans all three source modules
- architecture.flow.kt uses GraphNode containers with typed ports — Phase B populates each with a real CodeNode
- No Gradle modules are created in this feature — that is Phase B work
- Six-module partition (adding flowGraph-types) eliminates cyclic dependencies found in the original 5-module plan
- graphEditor composition root modeled as source/sink split reflecting FBP bidirectional data flow
