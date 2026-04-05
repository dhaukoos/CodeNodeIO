# Quickstart: Vertical Slice Refactor

**Feature**: 064-vertical-slice-refactor
**Date**: 2026-04-04

## Prerequisites

- CodeNodeIO builds and all existing tests pass (`./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest`)
- Access to all three source trees: graphEditor (77 files), kotlinCompiler (41 main files), circuitSimulator (5 files)
- Familiarity with the existing test patterns in `graphEditor/src/jvmTest/kotlin/` and `kotlinCompiler/src/jvmTest/kotlin/`

---

## Scenario 1: Verify Audit Completeness (US1 - P1)

### Steps

1. Open `graphEditor/ARCHITECTURE.md`, `kotlinCompiler/ARCHITECTURE.md`, and `circuitSimulator/ARCHITECTURE.md`
2. Count the total number of file entries across all three audit documents
3. Count actual source files across all three modules
4. Compare the two counts

### Expected Result

- The audit documents contain ~120 entries total (77 graphEditor + 38 kotlinCompiler + 5 circuitSimulator)
- Every file has exactly one bucket assignment: `types`, `compose`, `persist`, `execute`, `generate`, `inspect`, or `root` (composition root)
- No files appear in multiple buckets
- No files from any source tree are missing from the audits

---

## Scenario 2: Verify Seam Documentation (US1 - P1)

### Steps

1. In `graphEditor/ARCHITECTURE.md`, locate the seam/dependency matrix
2. Pick a file known to cross boundaries (e.g., `GraphEditorViewModel.kt` — likely touches compose, persist, and execute)
3. Verify the seam entries for that file list specific target files, dependency types, and boundary labels

### Expected Result

- Every cross-bucket dependency is documented with: source file, target file, dependency type (function call, type reference, inheritance, state sharing), and boundary label (e.g., "compose→persist")
- The seam list for `GraphEditorViewModel.kt` includes references to serialization (persist), execution state (execute), and node registry (inspect) — confirming its role as a composition-root ViewModel that will need to delegate to multiple slices

---

## Scenario 3: Verify Characterization Tests Pass (US2 - P1)

### Steps

1. Run `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest`
2. Check that all characterization test classes pass:
   - graphEditor: `GraphDataOpsCharacterizationTest`, `RuntimeExecutionCharacterizationTest`, `ViewModelCharacterizationTest`, `SerializationRoundTripCharacterizationTest`, `ArchitectureFlowKtsTest`
   - kotlinCompiler: `CodeGenerationCharacterizationTest`, `FlowKtGeneratorCharacterizationTest`
   - circuitSimulator: `RuntimeSessionCharacterizationTest`

### Expected Result

- All characterization tests pass alongside existing tests
- No existing tests are broken by the addition of characterization tests
- Test output shows characterization tests running in the `characterization/` packages across modules

---

## Scenario 4: Verify Characterization Tests Detect Changes (US2 - P1)

### Steps

1. Temporarily modify a known seam — for example, change the serialization output format in `FlowGraphSerializer.kt` (e.g., rename a DSL keyword)
2. Run `./gradlew :graphEditor:jvmTest`
3. Revert the change

### Expected Result

- At least one characterization test in `SerializationRoundTripCharacterizationTest` fails
- The failure message clearly indicates which round-trip assertion broke
- After reverting, all tests pass again

---

## Scenario 5: Verify Migration Map Completeness (US3 - P2)

### Steps

1. Open `MIGRATION.md` (repo root)
2. For each of the six target modules (flowGraph-types, flowGraph-compose, flowGraph-persist, flowGraph-execute, flowGraph-generate, flowGraph-inspect):
   - Verify it lists which files move from each source module's audit
   - Verify it defines a public API as Kotlin interfaces
   - Verify it specifies the extraction order step number
3. Verify the graphEditor composition root section describes the source/sink split (27 files)
4. Cross-reference: every file in all three audits should appear in exactly one target module's file list (or in "stays in graphEditor")

### Expected Result

- All ~120 audited files have a target module assignment
- Each module has at least one Kotlin interface definition
- The extraction order is: types (1st) → persist (2nd) → inspect (3rd) → execute (4th) → generate (5th) → compose (6th)
- Each extraction step specifies: files to move, interfaces to create, call sites to change, and which characterization tests must pass
- File counts match: types:9, inspect:13, persist:8, compose:10, execute:7, generate:46, root:27 = 120

---

## Scenario 6: Verify Extraction Order Has No Circular Dependencies (US3 - P2)

### Steps

1. In `MIGRATION.md`, review the extraction order
2. For each step N, check that the module being extracted does not depend on any module scheduled for step N+1 or later
3. Verify that each step explicitly states: "After this extraction, all characterization tests pass"

### Expected Result

- Step 1 (types): No dependencies on other workflow modules
- Step 2 (persist): Depends on types (already extracted). No dependencies on inspect, execute, generate, or compose
- Step 3 (inspect): Depends on types (already extracted). No dependencies on persist, execute, generate, or compose
- Step 4 (execute): May depend on types and inspect (already extracted). No dependencies on generate or compose
- Step 5 (generate): May depend on types, persist, and inspect (already extracted). No dependencies on compose
- Step 6 (compose): May depend on all previously extracted modules
- No circular dependencies exist

---

## Scenario 7: Verify Architecture FlowGraph Loads in GraphEditor (US4 - P2)

### Steps

1. Launch graphEditor with DemoProject
2. Open `graphEditor/architecture.flow.kt` via File menu
3. Inspect the canvas

### Expected Result

- The FlowGraph loads without errors
- Eight nodes are visible on the canvas: flowGraph-types, flowGraph-compose, flowGraph-persist, flowGraph-execute, flowGraph-generate, flowGraph-inspect, graphEditor-source, graphEditor-sink
- 19 connections between nodes represent data flow
- graphEditor-source has only outbound connections (4 command flows)
- graphEditor-sink has only inbound connections (8 state flows)
- types and inspect are hub sources (4 outbound connections each)
- The visual layout makes the architecture understandable at a glance

---

## Scenario 8: Verify Architecture FlowGraph Structural Invariants (US4 - P2)

### Steps

1. Run `./gradlew :graphEditor:jvmTest --tests "characterization.ArchitectureFlowKtsTest"`
2. Review test results

### Expected Result

- All 10 tests pass:
  - `architecture flow kt parses successfully`
  - `architecture flow kt has correct graph name`
  - `architecture flow kt contains all eight nodes`
  - `architecture flow kt has exactly 19 connections`
  - `types and inspect are the two hub source nodes`
  - `graphEditor-source has only command outputs`
  - `graphEditor-sink has only state inputs`
  - `all workflow modules receive flowGraphModel from source`
  - `no cycles exist in the connection graph`
  - `all target platforms are specified`

---

## Scenario 9: Verify Architecture FlowGraph Matches Migration Map (US4 - P2)

### Steps

1. Compare the connections in `graphEditor/architecture.flow.kt` with the module dependencies documented in `MIGRATION.md`
2. For each connection in the FlowGraph, verify a corresponding dependency exists in the migration map
3. For each dependency in the migration map, verify a corresponding connection exists in the FlowGraph

### Expected Result

- One-to-one correspondence between FlowGraph connections and migration map dependencies
- No phantom connections (connections in FlowGraph without migration map backing)
- No undocumented dependencies (migration map entries without FlowGraph connections)
- Port names in the FlowGraph match the data-oriented naming used in MIGRATION.md
