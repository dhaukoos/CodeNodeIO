# Quickstart: GraphEditor Refactoring Plan

**Feature**: 064-grapheditor-refactor-plan
**Date**: 2026-04-02

## Prerequisites

- CodeNodeIO graphEditor builds and all existing tests pass (`./gradlew :graphEditor:jvmTest`)
- Access to graphEditor source tree (77 files in `graphEditor/src/jvmMain/kotlin/`)
- Familiarity with the existing test patterns in `graphEditor/src/jvmTest/kotlin/`

---

## Scenario 1: Verify Audit Completeness (US1 - P1)

### Steps

1. Open `graphEditor/ARCHITECTURE.md`
2. Count the total number of file entries in the audit table
3. Run `find graphEditor/src/jvmMain/kotlin -name "*.kt" | wc -l` to count actual source files
4. Compare the two counts

### Expected Result

- The audit table contains exactly 77 entries (one per `.kt` file)
- Every file has exactly one bucket assignment: `compose`, `persist`, `execute`, `generate`, `inspect`, or `root` (composition root)
- No files appear in multiple buckets
- No files from the source tree are missing from the audit

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

1. Run `./gradlew :graphEditor:jvmTest`
2. Check that all four characterization test classes pass:
   - `GraphDataOpsCharacterizationTest`
   - `RuntimeExecutionCharacterizationTest`
   - `ViewModelCharacterizationTest`
   - `SerializationRoundTripCharacterizationTest`

### Expected Result

- All characterization tests pass alongside existing tests
- No existing tests are broken by the addition of characterization tests
- Test output shows characterization tests running in the `characterization/` package

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

1. Open `graphEditor/MIGRATION.md`
2. For each of the five target modules (flowGraph-compose, flowGraph-persist, flowGraph-execute, flowGraph-generate, flowGraph-inspect):
   - Verify it lists which files move from the audit
   - Verify it defines a public API as Kotlin interfaces
   - Verify it specifies the extraction order step number
3. Cross-reference: every file in the audit should appear in exactly one module's file list (or in "stays in graphEditor")

### Expected Result

- All 77 audited files have a target module assignment
- Each module has at least one Kotlin interface definition
- The extraction order is: persist (1st) → inspect (2nd) → execute (3rd) → generate (4th) → compose (5th)
- Each extraction step specifies: files to move, interfaces to create, call sites to change, and which characterization tests must pass

---

## Scenario 6: Verify Extraction Order Has No Circular Dependencies (US3 - P2)

### Steps

1. In `graphEditor/MIGRATION.md`, review the extraction order
2. For each step N, check that the module being extracted does not depend on any module scheduled for step N+1 or later
3. Verify that each step explicitly states: "After this extraction, all characterization tests pass"

### Expected Result

- Step 1 (persist): No dependencies on inspect, execute, generate, or compose
- Step 2 (inspect): May depend on persist (already extracted). No dependencies on execute, generate, or compose
- Step 3 (execute): May depend on persist and inspect (already extracted). No dependencies on generate or compose
- Step 4 (generate): May depend on persist and inspect (already extracted). No dependencies on compose
- Step 5 (compose): May depend on all previously extracted modules
- No circular dependencies exist

---

## Scenario 7: Verify Meta-FlowGraph Loads in GraphEditor (US4 - P3)

### Steps

1. Launch graphEditor with DemoProject
2. Open `graphEditor/architecture.flow.kts` via File menu or by loading it programmatically
3. Inspect the canvas

### Expected Result

- The FlowGraph loads without errors
- Six nodes are visible on the canvas: flowGraph-compose, flowGraph-persist, flowGraph-execute, flowGraph-generate, flowGraph-inspect, graphEditor (composition root)
- Connections between nodes represent data flow: e.g., compose→persist (FlowGraph output), inspect→compose (NodeDescriptors to palette)
- The visual layout makes the architecture understandable at a glance

---

## Scenario 8: Verify Meta-FlowGraph Matches Migration Map (US4 - P3)

### Steps

1. Compare the connections in `graphEditor/architecture.flow.kts` with the module dependencies documented in `graphEditor/MIGRATION.md`
2. For each connection in the FlowGraph, verify a corresponding dependency exists in the migration map
3. For each dependency in the migration map, verify a corresponding connection exists in the FlowGraph

### Expected Result

- One-to-one correspondence between FlowGraph connections and migration map dependencies
- No phantom connections (connections in FlowGraph without migration map backing)
- No undocumented dependencies (migration map entries without FlowGraph connections)
