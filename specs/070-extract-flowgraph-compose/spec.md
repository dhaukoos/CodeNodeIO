# Feature Specification: Extract flowGraph-compose Module

**Feature Branch**: `070-extract-flowgraph-compose`
**Created**: 2026-04-09
**Status**: Draft
**Input**: User description: "flowGraph-compose module extraction — Step 6 of Phase B vertical-slice decomposition"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Extract Graph Composition Files into flowGraph-compose Module (Priority: P1)

As a developer working on the Phase B vertical-slice decomposition, I need to extract the 4 non-UI graph composition files from graphEditor into a new `flowGraph-compose` KMP module so that graph mutation, interaction, and undo/redo concerns are isolated behind an FBP-native boundary expressed as CodeNode ports.

The extraction follows the Strangler Fig pattern: copy files into the new module first, switch all consumers to import from the new location, then remove the originals. From graphEditor, 4 files move: 2 viewmodel files (GraphEditorViewModel.kt, CanvasInteractionViewModel.kt) and 2 state files from the io/.../state/ package. The 6 Compose UI files (GraphState.kt, PropertyChangeTracker.kt, UndoRedoManager.kt, ConnectionContextMenu.kt, ConnectionHandler.kt, DragAndDropHandler.kt) remain in graphEditor since they have Compose dependencies or are the core data model used directly by all UI components.

This is the final workflow module extraction (Step 6). All five prior extractions (types, persist, inspect, execute, generate) are complete and stable, so compose extraction creates interfaces consumed only by the root composition shell.

**Why this priority**: This is the core extraction — without it, the module boundary doesn't exist. All other stories depend on this.

**Independent Test**: Can be verified by running the new module's compilation and confirming all existing characterization tests (GraphDataOpsCharacterizationTest, ViewModelCharacterizationTest) pass from the new module location.

**Acceptance Scenarios**:

1. **Given** graphEditor with 4 non-UI graph composition files, **When** the extraction is complete, **Then** a new `flowGraph-compose` KMP module exists containing all source and test files under the `io.codenode.flowgraphcompose` package.
2. **Given** consumers importing graph composition classes from graphEditor packages, **When** imports are migrated, **Then** all consumers reference `io.codenode.flowgraphcompose` and compile successfully.
3. **Given** the original 4 files in graphEditor, **When** originals are removed, **Then** the files no longer exist in graphEditor and no remaining references to old packages exist.
4. **Given** the 6 Compose UI files in graphEditor (GraphState, PropertyChangeTracker, UndoRedoManager, ConnectionContextMenu, ConnectionHandler, DragAndDropHandler), **When** extraction is complete, **Then** these files remain in graphEditor and import from `io.codenode.flowgraphcompose` for any ViewModel or state dependencies that moved.

---

### User Story 2 - Wrap flowGraph-compose as a CodeNode (Priority: P2)

As a developer, I need the flowGraph-compose module boundary expressed as a single CodeNode with typed input/output ports, so that the module participates in the FBP data flow graph.

Per architecture.flow.kt, the compose graphNode has 3 inputs and 1 output, which fits within a single In3AnyOut1 runtime:

- **FlowGraphCompose** (In3AnyOut1): Takes `flowGraphModel` + `nodeDescriptors` + `ipTypeMetadata` and produces `graphState`. The flowGraphModel carries mutation commands (add node, remove node, connect, disconnect), nodeDescriptors provides palette context, and ipTypeMetadata provides type information for port connection validation.

The CodeNode uses anyInput mode so each input independently triggers processing with cached values from the other inputs.

Unit tests for the CodeNode are written first (TDD) to define the port contract before implementation.

**Why this priority**: The CodeNode is the FBP-native module boundary. It must exist before wiring into the architecture graph.

**Independent Test**: Can be verified by running unit tests that confirm port signatures, runtime creation, anyInput mode, and basic data flow through the CodeNode.

**Acceptance Scenarios**:

1. **Given** the FlowGraphCompose CodeNode, **When** inspected, **Then** it declares 3 input ports (flowGraphModel, nodeDescriptors, ipTypeMetadata) and 1 output port (graphState), all typed as String, with anyInput=true.
2. **Given** the FlowGraphCompose CodeNode runtime, **When** data arrives on the flowGraphModel input, **Then** it processes the mutation command using cached nodeDescriptors and ipTypeMetadata and produces updated graphState on the output port.
3. **Given** the FlowGraphCompose CodeNode runtime, **When** nodeDescriptors or ipTypeMetadata arrives independently, **Then** it caches the value and produces updated graphState reflecting the new context.
4. **Given** the FlowGraphCompose CodeNode definition, **When** compiled and deployed, **Then** it is discoverable via META-INF/codenode/node-definitions and declares its sourceFilePath for edit button support.

---

### User Story 3 - Wire flowGraph-compose CodeNode into architecture.flow.kt (Priority: P3)

As a developer, I need the flowGraph-compose graphNode in architecture.flow.kt updated with a child codeNode definition, port mappings, and verified external connections so that the architecture graph accurately reflects the real module wiring.

**Why this priority**: This completes the integration into the architecture flow graph. It depends on US1 (module exists) and US2 (CodeNode exists).

**Independent Test**: Can be verified by parsing architecture.flow.kt and confirming the compose graphNode contains a child codeNode with correct port mappings, and all 20 external connections remain valid.

**Acceptance Scenarios**:

1. **Given** the compose graphNode in architecture.flow.kt, **When** updated, **Then** it contains one child codeNode: "FlowGraphCompose" (TRANSFORMER, 3 inputs, 1 output).
2. **Given** the child codeNode, **When** port mappings are defined, **Then** the 3 exposed input ports map to FlowGraphCompose's inputs (flowGraphModel, nodeDescriptors, ipTypeMetadata) and the exposed output port maps to FlowGraphCompose's graphState.
3. **Given** the updated architecture.flow.kt, **When** parsed, **Then** it produces a valid FlowGraph with all 20 external connections intact and the compose node has the expected single-node structure.
4. **Given** all six workflow modules now have child codeNode definitions, **When** architecture.flow.kt is reviewed, **Then** every graphNode (types, inspect, persist, compose, execute, generate) contains at least one child codeNode with complete port mappings.

---

### Edge Cases

- What happens when the 4 moved files depend on Compose UI classes still in graphEditor? The dependency direction must be inverted: graphEditor depends on flowGraph-compose, not vice versa.
- What happens when GraphState.kt (staying in graphEditor) is referenced by the moved ViewModel files? The ViewModel must import GraphState from graphEditor, or the interface must be abstracted so flowGraph-compose doesn't depend on graphEditor directly.
- What happens when the moved files reference UndoRedoManager or PropertyChangeTracker (which stay in graphEditor)? These dependencies must be handled through interfaces or the files must be structured so compose depends on abstractions, not concrete Compose-dependent classes.
- What happens when characterization tests reference both moved and non-moved files? Tests for the moved logic relocate to the new module; UI-coupled tests remain in graphEditor.
- What happens when this is the last extraction and the compose graphNode's "10 files" description no longer matches? The description and file count must be updated to reflect the actual post-extraction state.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST create a new `flowGraph-compose` KMP module with the standard multi-platform structure (commonMain, commonTest, jvmMain, jvmTest).
- **FR-002**: System MUST copy 4 graphEditor files (2 viewmodel files, 2 state files) into the new module with package renamed from graphEditor packages to `io.codenode.flowgraphcompose`.
- **FR-003**: System MUST NOT move GraphState.kt, PropertyChangeTracker.kt, UndoRedoManager.kt, ConnectionContextMenu.kt, ConnectionHandler.kt, or DragAndDropHandler.kt — these Compose-dependent or core-data-model files remain in graphEditor.
- **FR-004**: System MUST implement a FlowGraphComposeCodeNode CodeNodeDefinition with 3 input ports (flowGraphModel, nodeDescriptors, ipTypeMetadata) and 1 output port (graphState), all typed as String, using anyInput mode.
- **FR-005**: System MUST declare sourceFilePath on FlowGraphComposeCodeNode via resolveSourceFilePath for edit button discoverability.
- **FR-006**: System MUST create a META-INF/codenode/node-definitions registry file listing the FlowGraphComposeCodeNode fully qualified class name.
- **FR-007**: System MUST migrate all consumers of the 4 moved files to import from `io.codenode.flowgraphcompose`.
- **FR-008**: System MUST remove the 4 original files from graphEditor after consumers are migrated.
- **FR-009**: System MUST update architecture.flow.kt to add a child codeNode and port mappings to the compose graphNode.
- **FR-010**: System MUST update settings.gradle.kts to include the `:flowGraph-compose` module.
- **FR-011**: System MUST ensure all existing characterization tests (GraphDataOpsCharacterizationTest, ViewModelCharacterizationTest) pass from the new module location.
- **FR-012**: System MUST include TDD unit tests for the FlowGraphComposeCodeNode covering port signatures, runtime creation, anyInput mode, and data flow.
- **FR-013**: System MUST follow the Strangler Fig pattern: copy first, migrate consumers, then remove originals — each as separate verifiable steps.
- **FR-014**: System MUST ensure the dependency graph remains a DAG — flowGraph-compose MUST NOT depend on graphEditor, and no circular dependencies are introduced.

### Key Entities

- **FlowGraphComposeCodeNode**: CodeNodeDefinition (In3AnyOut1) that takes flowGraphModel commands, nodeDescriptors, and ipTypeMetadata to produce graphState reflecting the current graph composition.
- **flowGraph-compose module**: KMP module containing the 4 non-UI graph composition files extracted from graphEditor.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The `flowGraph-compose` module compiles successfully on all target platforms.
- **SC-002**: All existing characterization tests pass from the new module location without modification to test logic.
- **SC-003**: All existing tests across the entire project pass after extraction is complete.
- **SC-004**: Zero references remain to the old graphEditor package paths for the 4 moved files in any source file outside `flowGraph-compose`.
- **SC-005**: The 4 original files no longer exist in graphEditor.
- **SC-006**: architecture.flow.kt parses successfully and contains the compose graphNode with a child codeNode (FlowGraphCompose), and correct port mappings for all 3 inputs and 1 output.
- **SC-007**: Unit tests for FlowGraphComposeCodeNode verify port signatures, anyInput mode, and data flow.
- **SC-008**: graphEditor Compose UI files compile and reference moved classes from `io.codenode.flowgraphcompose`.

## Assumptions

- The 4 files being extracted do not have circular dependencies back to graphEditor Compose UI code. If they do, the dependency direction must be inverted (UI depends on compose, not vice versa) or abstractions must be introduced.
- In3AnyOut1Runtime exists in fbpDsl and is sufficient for the single-node FlowGraphCompose CodeNode.
- The build.gradle.kts for the new module will depend on :fbpDsl and possibly :flowGraph-types or :flowGraph-inspect as needed by the extracted code.
- GraphState.kt stays in graphEditor because it is the core data model referenced by all Compose UI components. If the moved ViewModels need GraphState, the dependency flows as graphEditor → flowGraph-compose (for ViewModels) and flowGraph-compose → graphEditor would be a cycle. This may require abstracting GraphState behind an interface in flowGraph-compose, with graphEditor providing the concrete implementation.
