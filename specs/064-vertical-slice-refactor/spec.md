# Feature Specification: Vertical Slice Refactor

**Feature Branch**: `064-vertical-slice-refactor`
**Created**: 2026-04-02
**Updated**: 2026-04-02 — Expanded scope to include kotlinCompiler and circuitSimulator modules
**Status**: Draft
**Input**: User description: "Vertical Slice Refactor — Define the migration map and characterization test suite for decomposing the graphEditor, kotlinCompiler, and circuitSimulator modules into vertical-slice modules. Deliverables: (1) audit all three modules and catalog every file by responsibility bucket, (2) write characterization tests that pin current behavior, (3) define module boundaries and extraction order, (4) create a meta-FlowGraph mapping the target architecture."

## User Scenarios & Testing

### User Story 1 - Audit and Catalog All Module Files (Priority: P1)

A developer preparing to refactor the application needs a complete inventory of what the graphEditor, kotlinCompiler, and circuitSimulator modules contain and how each file relates to user-facing workflows. They run the audit, which catalogs every file across all three modules into vertical-slice responsibility buckets (Compose, Persist, Execute, Generate, Inspect) plus a "Composition Root" bucket for files that remain in graphEditor. The audit identifies cross-bucket dependencies (seams) that must be addressed during extraction.

**Why this priority**: Without a complete understanding of what the three modules contain and how their parts relate, any extraction attempt risks breaking the application. The audit is the foundation for all subsequent work.

**Independent Test**: Run the audit on graphEditor, kotlinCompiler, and circuitSimulator source trees. Verify every `.kt` file across all three modules appears in exactly one responsibility bucket. Verify cross-bucket dependencies are listed with source file, target file, and dependency type (function call, type reference, import).

**Acceptance Scenarios**:

1. **Given** the three modules exist with their current source files, **When** the audit is executed, **Then** every `.kt` file in `graphEditor/src/jvmMain/kotlin/`, `kotlinCompiler/src/commonMain/kotlin/`, `kotlinCompiler/src/jvmMain/kotlin/`, and `circuitSimulator/src/commonMain/kotlin/` is assigned to exactly one responsibility bucket.
2. **Given** the audit is complete, **When** a developer reviews the output, **Then** each bucket entry includes the file path, primary responsibility, and a list of cross-bucket dependencies (other buckets this file directly references).
3. **Given** the audit identifies cross-bucket dependencies, **When** the seams are reviewed, **Then** each seam entry specifies: source file, target file, dependency type (function call, type reference, inheritance), and which extraction boundary it crosses.
4. **Given** the audit output exists, **When** the total file count is compared against the actual file count across all three modules, **Then** they match exactly — no files are missing or duplicated.

---

### User Story 2 - Write Characterization Tests Pinning Current Behavior (Priority: P1)

A developer needs confidence that extracting code from graphEditor, kotlinCompiler, and circuitSimulator will not change observable behavior. They write characterization tests that capture what the code does right now — not what it should do. These tests cover graph data operations (add node, connect ports, validate, serialize), execution/runtime (create graph, run, assert state transitions), ViewModel integration (verify ViewModel exposes correct state after graph mutations), code generation (verify generators produce expected output), and runtime session management (animation, debugging). All characterization tests pass on the current codebase before any extraction begins.

**Why this priority**: Characterization tests are the safety net for the entire refactoring. Without them, there is no way to verify that extractions preserve behavior. This is equally critical as the audit.

**Independent Test**: Run `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest` and verify all characterization tests pass. Intentionally break a seam (e.g., change a serialization format) and verify at least one characterization test fails — confirming the tests actually pin behavior.

**Acceptance Scenarios**:

1. **Given** the audit has identified graph data operation seams, **When** characterization tests are written for those seams, **Then** the tests construct FlowGraphs through the graphEditor's current API, perform operations (add node, connect ports, validate, serialize), and assert on the results without launching Compose.
2. **Given** the audit has identified runtime/execution seams, **When** characterization tests are written for those seams, **Then** the tests create a graph, run it, and assert on output state transitions using the existing fbpDsl test infrastructure.
3. **Given** the audit has identified ViewModel integration points, **When** characterization tests are written for those points, **Then** the tests verify that ViewModels expose the correct state after graph mutations, testing the ViewModel layer directly without rendering composables.
4. **Given** the audit has identified code generation seams in kotlinCompiler, **When** characterization tests are written for those seams, **Then** the tests verify that generators produce expected output for known inputs (FlowGraph → generated Kotlin source).
5. **Given** the audit has identified runtime session seams in circuitSimulator, **When** characterization tests are written for those seams, **Then** the tests verify that RuntimeSession, DataFlowAnimationController, and DataFlowDebugger produce expected state transitions and observer callbacks.
6. **Given** all characterization tests are written, **When** tests are run on the unmodified codebase, **Then** all characterization tests pass.
7. **Given** all characterization tests pass, **When** a known seam is intentionally modified (e.g., a serialization output format), **Then** at least one characterization test fails — confirming the tests detect behavioral changes.

---

### User Story 3 - Define Module Boundaries and Extraction Order (Priority: P2)

A developer needs a concrete plan for which code moves where and in what order. Using the audit results across all three modules, they define five vertical-slice modules (flowGraph-compose, flowGraph-persist, flowGraph-execute, flowGraph-generate, flowGraph-inspect), assign each audited file to its target module, specify the public API (Kotlin interfaces) for each module, and determine the extraction order based on dependency analysis. Files from kotlinCompiler map primarily to flowGraph-generate; files from circuitSimulator map primarily to flowGraph-execute. The extraction order ensures that each step leaves the application fully functional.

**Why this priority**: The migration map turns the audit data into an actionable plan. Without defined boundaries and ordering, extraction attempts would be ad hoc and risk creating circular dependencies or broken intermediate states.

**Independent Test**: Review the migration map document. Verify every audited file has a target module assignment. Verify the extraction order has no circular dependency violations (module N does not depend on unextracted module N+1). Verify each module's public API is defined as Kotlin interfaces.

**Acceptance Scenarios**:

1. **Given** the audit is complete for all three modules, **When** module boundaries are defined, **Then** every file in the audit is assigned to exactly one target module: flowGraph-compose, flowGraph-persist, flowGraph-execute, flowGraph-generate, flowGraph-inspect, or graphEditor (composition root).
2. **Given** module boundaries are defined, **When** the public API for each module is specified, **Then** each module has a set of Kotlin interface definitions that represent the functions the current modules call internally for that module's responsibility.
3. **Given** module APIs are defined, **When** the extraction order is determined, **Then** the order ensures that extracting module N does not require module N+1 to already be extracted — no forward dependencies in the extraction sequence.
4. **Given** the extraction order is defined, **When** each step is reviewed, **Then** it specifies: which files move, which interfaces are created, which graphEditor call sites change to delegation, and which characterization tests must still pass.

---

### User Story 4 - Create Meta-FlowGraph of Target Architecture (Priority: P3)

A developer wants to visualize the target architecture using the project's own paradigm. They create a FlowGraph (openable in the graphEditor itself) where each vertical-slice module is represented as a node, the shared fbpDsl vocabulary is a central hub, and connections show the data flow between modules (e.g., flowGraph-compose outputs a FlowGraph that flowGraph-persist accepts as input). This meta-FlowGraph serves as living documentation that dogfoods the FBP paradigm.

**Why this priority**: While valuable for communication and validation, this is a documentation artifact. The refactoring can proceed without it, making it lower priority than the audit, tests, and migration map.

**Independent Test**: Open the meta-FlowGraph `.flow.kts` file in the graphEditor. Verify all five vertical-slice modules appear as nodes. Verify connections between modules match the data flow described in the migration map. Verify the FlowGraph loads without errors.

**Acceptance Scenarios**:

1. **Given** the module boundaries are defined, **When** the meta-FlowGraph is created, **Then** it contains one node per vertical-slice module (flowGraph-compose, flowGraph-persist, flowGraph-execute, flowGraph-generate, flowGraph-inspect) plus a node for the graphEditor composition root.
2. **Given** the meta-FlowGraph exists, **When** connections are reviewed, **Then** each connection represents a real data flow: e.g., flowGraph-compose outputs FlowGraph instances that flowGraph-persist accepts, flowGraph-inspect outputs NodeDescriptors that flowGraph-compose's palette accepts.
3. **Given** the meta-FlowGraph is complete, **When** it is opened in the graphEditor, **Then** it loads and renders without errors, visually showing the target architecture as a flow-based program.
4. **Given** the meta-FlowGraph exists, **When** the shared vocabulary (fbpDsl types) is reviewed, **Then** the FlowGraph shows fbpDsl as the central type vocabulary that all modules reference but none own.

---

### Edge Cases

- What happens when a file belongs to multiple responsibility buckets? It is assigned to its primary bucket, with cross-references noted. During extraction, the file may need to be split.
- What happens when a characterization test depends on Compose runtime (e.g., composable functions)? The test should target the ViewModel layer, not the composable. If no ViewModel exists for that behavior, the test documents the gap and the extraction plan includes creating one.
- What happens when the extraction order reveals a circular dependency between two target modules? The dependency is documented as a "shared concern" that must be resolved before extraction — either by extracting the shared code to fbpDsl or by creating a small shared utility module.
- What happens when a file has no clear bucket assignment? It is temporarily assigned to "Composition Root" (stays in graphEditor) and flagged for review during the extraction of adjacent modules.
- What happens when a kotlinCompiler file serves multiple vertical slices (e.g., a utility used by both generate and persist)? It is assigned to the slice with highest affinity and flagged; the extraction plan may move it to a shared utility or fbpDsl.
- What happens when characterization tests reveal undocumented behavior that appears to be a bug? The test captures the current behavior as-is. A separate issue is filed for the potential bug. The refactoring does not fix bugs — it preserves behavior.

## Requirements

### Functional Requirements

- **FR-001**: The audit MUST catalog every `.kt` file in `graphEditor/src/jvmMain/kotlin/`, `kotlinCompiler/src/commonMain/kotlin/`, `kotlinCompiler/src/jvmMain/kotlin/`, and `circuitSimulator/src/commonMain/kotlin/` with its responsibility bucket assignment.
- **FR-002**: The audit MUST identify all cross-bucket dependencies with source file, target file, and dependency type — including cross-module dependencies.
- **FR-003**: Characterization tests MUST cover graph data operations: node creation, port connection, port type validation, cycle detection, and serialization round-trips.
- **FR-004**: Characterization tests MUST cover runtime execution: graph creation, execution start/stop, state transition assertions, and runtime session management (animation controller, debugger).
- **FR-005**: Characterization tests MUST cover ViewModel integration: verifying ViewModel state after graph mutations without rendering composables.
- **FR-005a**: Characterization tests MUST cover code generation: verifying kotlinCompiler generators produce expected output for known FlowGraph inputs.
- **FR-006**: All characterization tests MUST pass on the unmodified codebase.
- **FR-007**: The migration map MUST assign every audited file to exactly one target module.
- **FR-008**: The migration map MUST define the public API for each target module as Kotlin interfaces.
- **FR-009**: The migration map MUST specify an extraction order where each step leaves the application fully functional.
- **FR-010**: The migration map MUST define five vertical-slice modules: flowGraph-compose, flowGraph-persist, flowGraph-execute, flowGraph-generate, flowGraph-inspect.
- **FR-011**: Each extraction step in the migration map MUST specify: files to move, interfaces to create, call sites to change, and characterization tests that must pass.
- **FR-012**: The meta-FlowGraph MUST be a valid `.flow.kts` file loadable by the graphEditor.
- **FR-013**: The meta-FlowGraph MUST represent each target module as a node with connections showing inter-module data flow.

### Key Entities

- **Responsibility Bucket**: A classification category for source files across graphEditor, kotlinCompiler, and circuitSimulator, corresponding to a target vertical-slice module or the composition root.
- **Seam**: A cross-bucket dependency between two files — the point where code in one bucket directly references code in another. Seams become module boundaries during extraction.
- **Characterization Test**: A test that captures current observable behavior without asserting correctness. Used as a regression safety net during extraction.
- **Migration Map**: A document specifying target module assignments, public APIs, extraction order, and step-by-step instructions for each extraction phase.
- **Meta-FlowGraph**: A `.flow.kts` file representing the target architecture as a flow-based program, using the project's own FBP paradigm for documentation.
- **Vertical Slice Module**: A Gradle module organized around a user workflow (compose, persist, execute, generate, inspect) rather than a technical layer.

## Success Criteria

### Measurable Outcomes

- **SC-001**: 100% of `.kt` source files across graphEditor, kotlinCompiler, and circuitSimulator are cataloged in the audit with a bucket assignment — zero files missing.
- **SC-002**: All cross-bucket and cross-module dependencies are documented with sufficient detail (source, target, type) to plan interface boundaries.
- **SC-003**: Characterization tests achieve coverage of all identified seams — every seam has at least one test that would fail if the seam's behavior changed.
- **SC-004**: All characterization tests pass on the current unmodified codebase via `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest`.
- **SC-005**: The migration map assigns every audited file to a target module with no unassigned files remaining.
- **SC-006**: The extraction order in the migration map can be validated by dependency analysis — no step requires a module that hasn't been extracted yet.
- **SC-007**: The meta-FlowGraph loads in the graphEditor without errors and visually represents all five target modules and their connections.
- **SC-008**: A developer unfamiliar with the codebase can read the migration map and understand which files move where, in what order, and why.

## Assumptions

- The existing graphEditor, kotlinCompiler, and circuitSimulator codebases are stable enough to write characterization tests against — no major in-flight refactors that would invalidate the tests.
- The five vertical-slice modules (flowGraph-compose, flowGraph-persist, flowGraph-execute, flowGraph-generate, flowGraph-inspect) are the correct decomposition. If the audit reveals a better partitioning, the module list may be adjusted.
- The fbpDsl module already contains the shared domain types (FlowGraph, Node, Port, Connection, InformationPacket) and does not need modification for this feature.
- Characterization tests can be written for the ViewModel layer without requiring a Compose test framework — standard kotlin.test and coroutines-test are sufficient.
- The Strangler Fig pattern (copy, delegate, delete) is the extraction strategy. The migration map documents this approach but does not execute it — execution is a separate feature.
- graphEditor will become the composition root: Compose UI, ViewModels, and DI wiring stay here; all business logic moves to vertical-slice modules.
- kotlinCompiler files map primarily to flowGraph-generate; circuitSimulator files map primarily to flowGraph-execute. Some files may need splitting if they serve multiple slices.
- The existing kotlinCompiler and circuitSimulator Gradle modules will be dissolved — their code absorbed into the vertical-slice modules.
- This feature produces planning artifacts (audit document, tests, migration map, meta-FlowGraph) — it does not perform the actual module extraction.
