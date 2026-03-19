# Feature Specification: Persist CodeNode Metadata Through Save Pipeline

**Feature Branch**: `054-persist-codenode-metadata`
**Created**: 2026-03-19
**Status**: Draft
**Input**: User description: "Persist CodeNodeDefinition metadata through the save pipeline so that regenerated Flow files use CodeNodeDefinition.createRuntime() instead of falling back to legacy CodeNodeFactory patterns."

## Clarifications

### Session 2026-03-19

- Q: Should this feature preserve backward compatibility for legacy custom nodes without CodeNodeDefinitions, or can we assume all nodes have CodeNodeDefinitions? → A: Drop legacy support AND clean up the legacy CustomNodeDefinition infrastructure (custom-nodes.json, FileCustomNodeRepository, legacy palette discovery path) as part of this feature. All 18 production nodes across 5 modules already have CodeNodeDefinitions; the only legacy-only entries are 2 test artifacts (MyIn2AnyOut1, CUDoperations) which can be removed.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Saved Modules Regenerate CodeNode-Driven Flows (Priority: P1)

A module developer saves a module in the graphEditor whose nodes originate from CodeNodeDefinition objects. When the save completes, the regenerated Flow file uses the self-contained CodeNode runtime creation pattern instead of the legacy processingLogic/factory pattern. On subsequent saves (re-saves), the Flow file continues to use the CodeNode pattern — the metadata is not lost.

**Why this priority**: This is the core value. Without it, every save overwrites the CodeNode-driven Flow files with legacy code, undoing the architectural improvement from feature 053.

**Independent Test**: Save a module that contains nodes backed by CodeNodeDefinitions (e.g., StopWatch). Open the regenerated Flow file and confirm it contains CodeNode imports and createRuntime() calls instead of processingLogic imports and CodeNodeFactory calls.

**Acceptance Scenarios**:

1. **Given** a module with nodes placed from CodeNodeDefinitions, **When** the user saves the module, **Then** the regenerated Flow file imports the CodeNodeDefinition objects and creates runtimes via createRuntime() with appropriate type casts.
2. **Given** a previously saved module, **When** the user re-opens and re-saves it without changes, **Then** the regenerated Flow file still uses the CodeNode pattern — no fallback to legacy generation.
3. **Given** a module with nodes placed from CodeNodeDefinitions, **When** the user saves the module, **Then** the serialized module definition file preserves the CodeNode class identity so it can be recovered on reload.

---

### User Story 2 - Drag-and-Drop Nodes Retain CodeNode Identity (Priority: P2)

When a user drags a node from the palette onto the canvas, and that node originates from a registered CodeNodeDefinition, the node retains its CodeNode identity in the graph. This identity persists through subsequent editing operations (move, connect, disconnect) and is available when the module is saved.

**Why this priority**: This closes the entry-point gap. If nodes lose their CodeNode identity on creation, the save pipeline has nothing to persist.

**Independent Test**: Drag a CodeNodeDefinition-backed node onto the canvas, inspect the node's configuration in the graph model, and confirm the CodeNode class identity is present.

**Acceptance Scenarios**:

1. **Given** a CodeNodeDefinition is registered and visible in the palette, **When** the user clicks to place the node on the canvas, **Then** the resulting graph node contains the CodeNode class identity in its configuration.
2. **Given** a CodeNodeDefinition-backed node on the canvas, **When** the user drags it from the palette (drag-and-drop path), **Then** the resulting graph node also contains the CodeNode class identity.

---

### User Story 3 - Remove Legacy CustomNodeDefinition Infrastructure (Priority: P3)

The legacy CustomNodeDefinition system — including the JSON repository file, FileCustomNodeRepository, legacy palette discovery path, and the legacy createNode() method in NodeGeneratorViewModel — is removed. All node creation and discovery flows exclusively through CodeNodeDefinitions. The 2 remaining test artifacts (MyIn2AnyOut1, CUDoperations) in the legacy JSON are discarded.

**Why this priority**: With all production nodes covered by CodeNodeDefinitions, the legacy infrastructure is dead weight. Removing it eliminates a parallel code path and simplifies the node lifecycle.

**Independent Test**: Verify that the graphEditor launches without errors, the palette shows only CodeNodeDefinition-backed nodes, and no references to CustomNodeDefinition or FileCustomNodeRepository remain in the active code paths.

**Acceptance Scenarios**:

1. **Given** the legacy JSON repository file and FileCustomNodeRepository are removed, **When** the graphEditor launches, **Then** the palette populates exclusively from registered CodeNodeDefinitions without errors.
2. **Given** the legacy createNode() method is removed from NodeGeneratorViewModel, **When** the user generates a new node via the Node Generator, **Then** a CodeNodeDefinition .kt file is produced (existing generateCodeNode() path).
3. **Given** all legacy infrastructure is removed, **When** the project compiles, **Then** zero references to CustomNodeDefinition or FileCustomNodeRepository remain in active source code.

---

### Edge Cases

- What happens when the user edits a node's ports or properties after placement? The CodeNode class identity should be retained as long as the node itself is not replaced.
- What happens when a node is copied and pasted? The copy should retain the CodeNode class identity from the original.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: When a CodeNodeDefinition-backed node is placed on the canvas (via click or drag-and-drop), the resulting graph node MUST contain the fully-qualified CodeNodeDefinition class name in its configuration.
- **FR-002**: The module definition file format MUST preserve CodeNodeDefinition class names across save and reload cycles.
- **FR-003**: The runtime flow generator MUST use the CodeNode-driven generation path (createRuntime() with type cast) for all nodes, since all nodes are backed by CodeNodeDefinitions.
- **FR-004**: The legacy CustomNodeDefinition class, FileCustomNodeRepository, and the legacy JSON repository file MUST be removed from the active codebase.
- **FR-005**: The legacy palette discovery path in NodeDefinitionRegistry (loading from FileCustomNodeRepository) MUST be removed; the palette MUST populate exclusively from registered CodeNodeDefinitions.
- **FR-006**: Copy-paste operations on CodeNode-backed nodes MUST preserve the CodeNode class identity in the pasted copy.
- **FR-007**: The existing 5 modules (StopWatch, UserProfiles, GeoLocations, Addresses, EdgeArtFilter) MUST produce CodeNode-driven Flow files on their next save.
- **FR-008**: The legacy createNode() method in NodeGeneratorViewModel MUST be removed; only the CodeNodeDefinition generation path (generateCodeNode()) should remain.

### Key Entities

- **CodeNodeDefinition class identity**: The fully-qualified class name (e.g., `io.codenode.stopwatch.nodes.TimerEmitterCodeNode`) that links a graph node to its self-contained CodeNodeDefinition object.
- **Node configuration**: A string key-value map on each graph node that carries metadata through the pipeline — from canvas placement through serialization to code generation.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of nodes placed from registered CodeNodeDefinitions retain their class identity through save, reload, and re-save cycles.
- **SC-002**: Regenerated Flow files for all modules use createRuntime() calls — zero legacy CodeNodeFactory calls in generated code.
- **SC-003**: All existing compilation checks pass after the change (graphEditor compiles, kotlinCompiler tests pass, fbpDsl tests have no new failures beyond pre-existing ones).
- **SC-004**: All 5 modules (StopWatch, UserProfiles, GeoLocations, Addresses, EdgeArtFilter) produce CodeNode-driven Flow files when saved from the graphEditor.
- **SC-005**: Zero references to CustomNodeDefinition or FileCustomNodeRepository remain in the active source code after cleanup.

## Assumptions

- CodeNodeDefinition objects are Kotlin `object` singletons with stable fully-qualified class names that do not change between sessions.
- The existing `_codeNodeClass` configuration key convention (used by RuntimeFlowGenerator) is the agreed-upon mechanism for carrying CodeNode class identity.
- The save pipeline always has access to the same set of registered CodeNodeDefinitions that were available when nodes were placed on the canvas.
- All production nodes are covered by CodeNodeDefinitions; no active module depends on legacy CustomNodeDefinition-only nodes.

## Out of Scope

- Migrating the KMPMobileApp or its controllers to use the dynamic pipeline — that is a separate feature.
- Adding new CodeNodeDefinition objects — this feature only ensures existing ones flow through the save pipeline.
- Changing the graphEditor's runtime preview behavior — it already uses DynamicPipelineController and is unaffected.
