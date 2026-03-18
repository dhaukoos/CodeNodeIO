# Feature Specification: CodeNode-Driven Flow Runtime

**Feature Branch**: `053-codenode-flow-runtime`
**Created**: 2026-03-17
**Status**: Draft
**Input**: User description: "Restructure generated Flow files to resolve runtime behavior from self-contained CodeNodeDefinition objects instead of processingLogic tick functions. The generated *Flow.kt files currently import and wire processingLogic functions directly. Instead, they should use CodeNodeDefinition.createRuntime() to obtain node runtimes, matching what DynamicPipelineBuilder already does. This eliminates the processingLogic/ directories as a dependency, making CodeNodes the single source of truth for node behavior. The .flow.kt files should also have their unused processingLogic imports removed. Scope: StopWatch, UserProfiles, GeoLocations, Addresses modules and the RuntimeFlowGenerator in kotlinCompiler."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generated Flow Uses CodeNode Runtimes (Priority: P1)

When a module's generated Flow file runs (whether in the graphEditor fallback path or the KMPMobileApp), it obtains each node's runtime behavior from the node's self-contained CodeNodeDefinition rather than from a separate processingLogic tick function. This makes CodeNodes the single source of truth for how nodes behave — editing a CodeNode's logic automatically takes effect everywhere the node is used, without needing to keep a parallel processingLogic file in sync.

**Why this priority**: This is the core deliverable. Without it, two separate copies of each node's behavior exist (CodeNode closures and processingLogic functions), creating a maintenance burden and a drift risk where they diverge silently.

**Independent Test**: Run the KMPMobileApp's StopWatch module. Start the timer, verify it ticks, pause, resume, stop/reset. All behavior should be identical to before the change — but the generated StopWatchFlow no longer imports anything from the processingLogic package.

**Acceptance Scenarios**:

1. **Given** the StopWatch module's generated Flow file, **When** the Flow is started, **Then** each node runtime is created via its CodeNodeDefinition and the timer ticks correctly without any processingLogic import.
2. **Given** any of the 4 migrated modules (StopWatch, UserProfiles, GeoLocations, Addresses), **When** the generated Flow file is inspected, **Then** it contains zero references to the module's processingLogic package.
3. **Given** the generated Flow file creates node runtimes from CodeNodeDefinitions, **When** a CodeNode's internal logic is modified, **Then** the change takes effect in both the dynamic pipeline and the generated Flow without any other file edits.

---

### User Story 2 - Code Generator Produces CodeNode-Based Flows (Priority: P2)

When a developer saves a new module from the graphEditor, the RuntimeFlowGenerator produces a Flow file that resolves node runtimes from CodeNodeDefinitions instead of generating processingLogic import references. New modules created going forward follow the CodeNode-driven pattern from the start.

**Why this priority**: Without updating the generator, any newly created module would revert to the old processingLogic pattern, re-introducing the dual-source problem for future modules.

**Independent Test**: In the graphEditor, create a test FlowGraph with nodes that have registered CodeNodeDefinitions, trigger module save, and inspect the generated Flow file. It should reference CodeNodeDefinitions, not processingLogic functions.

**Acceptance Scenarios**:

1. **Given** a FlowGraph with nodes that have registered CodeNodeDefinitions, **When** the RuntimeFlowGenerator produces a Flow file, **Then** the generated code resolves runtimes via CodeNodeDefinition lookup rather than importing processingLogic tick functions.
2. **Given** a FlowGraph with a mix of nodes (some with CodeNodeDefinitions, some without), **When** the RuntimeFlowGenerator produces a Flow file, **Then** nodes with CodeNodeDefinitions use the CodeNode-driven approach, and nodes without fall back to the existing processingLogic pattern.

---

### User Story 3 - Dead ProcessingLogic Files and Imports Removed (Priority: P3)

The processingLogic directories and unused processingLogic imports in .flow.kt files are cleaned up for the 4 migrated modules, eliminating dead code that could confuse developers or cause misleading search results.

**Why this priority**: Cleanup naturally follows once the generated Flows no longer depend on processingLogic. Leaving dead files creates confusion about which code is authoritative.

**Independent Test**: Search the 4 migrated module directories for any remaining processingLogic references or files. None should exist. The project compiles and all tests pass.

**Acceptance Scenarios**:

1. **Given** the 4 migrated modules (StopWatch, UserProfiles, GeoLocations, Addresses), **When** the processingLogic directories are removed, **Then** the project compiles successfully with no unresolved references.
2. **Given** the .flow.kt files for the 4 migrated modules, **When** inspected, **Then** they contain no `import ...processingLogic.*` statements.
3. **Given** any module that still lacks CodeNodeDefinitions (e.g., EdgeArtFilter if applicable), **When** that module is compiled, **Then** its processingLogic files remain intact and functional.

---

### Edge Cases

- What happens when a module has some nodes with CodeNodeDefinitions and some without? The generated Flow should handle mixed resolution — CodeNode-driven for known nodes, processingLogic for legacy nodes.
- What happens when the KMPMobileApp runs on iOS? The CodeNodeDefinition objects must be accessible from commonMain, not JVM-only code.
- What happens when a generated Flow file is regenerated after this change? It should produce the new CodeNode-driven format, not revert to processingLogic imports.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Generated Flow files MUST obtain node runtimes from CodeNodeDefinition objects instead of importing processingLogic tick functions, for any node that has a registered CodeNodeDefinition.
- **FR-002**: The RuntimeFlowGenerator MUST be updated to produce Flow files that resolve runtimes via CodeNodeDefinition lookup when CodeNodeDefinitions are available for the node.
- **FR-003**: The RuntimeFlowGenerator MUST fall back to the existing processingLogic pattern for nodes that do not have a CodeNodeDefinition, ensuring backward compatibility for un-migrated modules.
- **FR-004**: The .flow.kt files for StopWatch, UserProfiles, GeoLocations, and Addresses MUST have their unused `import ...processingLogic.*` statements removed.
- **FR-005**: The processingLogic directories for StopWatch, UserProfiles, GeoLocations, and Addresses MUST be deleted, as their contents are now embedded in CodeNode closures.
- **FR-006**: All CodeNodeDefinition objects MUST remain in commonMain source sets so they are accessible from both desktop (graphEditor) and mobile (KMPMobileApp) targets.
- **FR-007**: The generated Flow file's runtime behavior MUST be identical to the current behavior — no observable changes to timing, state updates, or data flow.
- **FR-008**: The KMPMobileApp MUST continue to compile and run correctly using the restructured generated Flow files.

### Key Entities

- **CodeNodeDefinition**: Self-contained object that defines a node's identity (name, category, ports) and creates its runtime via `createRuntime()`. The single source of truth for node behavior.
- **Generated Flow**: A module's orchestrator class that creates node runtimes, wires channel connections, and manages start/stop lifecycle. Currently resolves behavior from processingLogic; will resolve from CodeNodeDefinitions.
- **ProcessingLogic**: Legacy per-node tick/process functions in separate files. Being eliminated as a dependency in favor of CodeNode-embedded logic.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Zero processingLogic imports exist in the 4 migrated modules' generated Flow files and .flow.kt files.
- **SC-002**: Zero processingLogic source files remain in the 4 migrated modules' directories.
- **SC-003**: The KMPMobileApp compiles and runs identically on all targets (Android, iOS) with no behavioral changes.
- **SC-004**: The RuntimeFlowGenerator produces CodeNode-driven Flow files for any module whose nodes all have CodeNodeDefinitions.
- **SC-005**: The existing test suite passes with no regressions (same pass/fail as before this change).
- **SC-006**: Modifying a CodeNode's internal logic takes effect in both the dynamic pipeline path and the generated Flow path without editing any other file.

## Assumptions

- All 4 migrated modules (StopWatch, UserProfiles, GeoLocations, Addresses) already have complete CodeNodeDefinition coverage — every node in each module has a corresponding CodeNode object.
- The CodeNodeDefinition objects are in commonMain source sets and are accessible from KMP targets (Android, iOS, JVM).
- EdgeArtFilter's processingLogic files (if any) are out of scope and remain untouched unless EdgeArtFilter also has full CodeNodeDefinition coverage.
- The generated Controller and Adapter files are not affected by this change — only the generated Flow files are restructured.

## Scope

### In Scope

- Restructuring the 4 generated Flow files (StopWatchFlow, UserProfilesFlow, GeoLocationsFlow, AddressesFlow) to use CodeNodeDefinition-based runtime creation
- Updating the RuntimeFlowGenerator to produce CodeNode-driven Flow files
- Removing processingLogic directories and imports from the 4 migrated modules
- Verifying KMPMobileApp compilation and runtime behavior

### Out of Scope

- Migrating KMPMobileApp away from generated Controllers (separate future feature)
- Modifying the generated Controller or Adapter files
- Changes to EdgeArtFilter or any module without full CodeNodeDefinition coverage
- Changes to the DynamicPipelineController or DynamicPipelineBuilder
