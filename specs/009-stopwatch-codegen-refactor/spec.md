# Feature Specification: StopWatch Code Generation Refactor

**Feature Branch**: `009-stopwatch-codegen-refactor`
**Created**: 2026-02-11
**Status**: Draft
**Input**: User description: "Refactor StopWatch virtual circuit - The createStopWatchFlowGraph() function in the App.kt file in commonMain source code for KMPMobileApp should be defined as part of the generated code StopWatchFlow.kt. The properties palette needs to provide inputs for all required properties of a CodeNode. The processingLogic property should be a required input when attempting to compile. The UI will allow file name selection for the ProcessingLogic class/object. The compile operation should check that all required inputs are defined, and if not, generate a message indicating which requirements are still unmet."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Configure ProcessingLogic Reference in Properties Panel (Priority: P1)

A flow graph designer opens the StopWatch.flow.kts file in the graphEditor, selects a CodeNode (e.g., TimerEmitter), and needs to specify which Kotlin file provides the ProcessingLogic implementation for that node. The properties panel displays a "Processing Logic" field with a file browser button. The designer clicks the button, navigates to the demos/stopwatch/ directory, and selects TimerEmitterComponent.kt. The field shows the selected file reference, and this association is persisted when saving the flow graph.

**Why this priority**: This is the core enabler for code generation. Without the ability to specify ProcessingLogic references in the UI, the compiler cannot generate valid executable code that links nodes to their implementations.

**Independent Test**: Can be fully tested by opening a flow graph, selecting a node, and verifying the file selection dialog appears and the reference is stored. Delivers the foundational capability for associating nodes with their implementations.

**Acceptance Scenarios**:

1. **Given** a CodeNode is selected in the canvas, **When** the user views the properties panel, **Then** a "Processing Logic" field appears with a file browser button.
2. **Given** the "Processing Logic" file browser is clicked, **When** the user selects a Kotlin file, **Then** the file path is stored in the node's configuration and displayed in the properties panel.
3. **Given** a CodeNode has a ProcessingLogic file reference configured, **When** the flow graph is saved and reopened, **Then** the file reference is preserved and displayed correctly.

---

### User Story 2 - Compile Validation for Required Properties (Priority: P1)

A flow graph designer has created a virtual circuit with multiple CodeNodes but has not yet specified ProcessingLogic files for all nodes. When they attempt to compile the circuit (via Build/Compile menu or toolbar), the system validates that all required properties are defined. If any CodeNode lacks a ProcessingLogic reference, compilation halts and displays a clear error message listing each node with missing required properties, such as "TimerEmitter: Missing required property 'processingLogic'".

**Why this priority**: Equal priority with User Story 1 because validation prevents generating invalid code. Users need immediate feedback on what's missing before compilation proceeds.

**Independent Test**: Can be tested by creating a flow graph with nodes missing required properties and triggering compile. Delivers protection against generating incomplete/broken code.

**Acceptance Scenarios**:

1. **Given** a flow graph with all CodeNodes having ProcessingLogic configured, **When** compile is triggered, **Then** compilation proceeds without validation errors related to required properties.
2. **Given** a flow graph where one or more CodeNodes lack ProcessingLogic references, **When** compile is triggered, **Then** compilation halts and displays a message listing each node and its missing required properties.
3. **Given** validation errors are displayed, **When** the user reviews the error list, **Then** they can identify which specific nodes need configuration and what properties are missing.

---

### User Story 3 - Generated FlowGraph Factory Function (Priority: P2)

After successfully compiling a virtual circuit (all required properties configured), the code generator produces a StopWatchFlow.kt file that includes a `createStopWatchFlowGraph()` factory function. This function creates the FlowGraph programmatically with all nodes, ports, connections, and ProcessingLogic references. The KMPMobileApp's App.kt can then import and call this generated function instead of defining the FlowGraph manually.

**Why this priority**: This is the payoff of P1 features. Once validation passes, code generation produces the artifact that replaces manual FlowGraph construction.

**Independent Test**: Can be tested by compiling a fully-configured flow graph and verifying the generated code contains a proper factory function that creates an equivalent FlowGraph.

**Acceptance Scenarios**:

1. **Given** a validated flow graph with all required properties, **When** code generation completes, **Then** the generated file includes a `createStopWatchFlowGraph()` function that returns a FlowGraph instance.
2. **Given** the generated factory function exists, **When** it is called, **Then** it returns a FlowGraph with all nodes, ports, connections, and ProcessingLogic instances matching the original flow graph definition.
3. **Given** the generated code includes the factory function, **When** KMPMobileApp's App.kt replaces its manual `createStopWatchFlowGraph()` with an import from the generated code, **Then** the application compiles and runs identically.

---

### User Story 4 - Migrate KMPMobileApp to Use Generated Code (Priority: P3)

A developer updates KMPMobileApp to depend on the generated StopWatch module code instead of manually defining the FlowGraph in App.kt. They remove the hand-written `createStopWatchFlowGraph()` function from App.kt and replace it with an import of the generated function from `io.codenode.generated.stopwatch`. The application continues to function identically, demonstrating that the generated code is a drop-in replacement.

**Why this priority**: This is the integration step that validates the end-to-end workflow. It depends on all previous stories being complete.

**Independent Test**: Can be tested by modifying KMPMobileApp to use generated imports and verifying the app compiles and the stopwatch UI functions correctly.

**Acceptance Scenarios**:

1. **Given** the generated StopWatchFlow.kt contains the factory function, **When** KMPMobileApp's App.kt imports and uses the generated function, **Then** the application compiles successfully.
2. **Given** the application uses the generated FlowGraph, **When** the stopwatch is started, **Then** it displays elapsed time identically to when using the manual definition.

---

### Edge Cases

- What happens when the selected ProcessingLogic file is moved or deleted after configuration?
  - Display a validation warning indicating the file cannot be found, and prevent compilation until resolved.
- What happens when a user saves a flow graph with incomplete required properties?
  - Allow saving (to preserve work in progress) but display a warning indicator in the UI that compilation is not possible.
- What happens when multiple nodes reference the same ProcessingLogic file?
  - Allow this configuration; some implementations may be reusable across nodes.
- What happens when the ProcessingLogic file exists but doesn't contain a valid ProcessingLogic class/object?
  - Report a specific validation error during compile: "TimerEmitter: File 'X.kt' does not contain a valid ProcessingLogic implementation".

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a "Processing Logic" property editor in the properties panel for CodeNode selection.
- **FR-002**: System MUST display a file browser dialog when the Processing Logic field's browse button is clicked.
- **FR-003**: System MUST filter the file browser to show Kotlin files (*.kt) by default.
- **FR-004**: System MUST store the ProcessingLogic file reference in the CodeNode's configuration map with key "processingLogicFile".
- **FR-005**: System MUST persist ProcessingLogic file references when saving the flow graph to .flow.kts format.
- **FR-006**: System MUST restore ProcessingLogic file references when loading a .flow.kts file.
- **FR-007**: System MUST validate that all CodeNodes have ProcessingLogic configured before compilation proceeds.
- **FR-008**: System MUST display a clear error message listing all nodes with missing required properties when validation fails.
- **FR-009**: System MUST generate a `createXXXFlowGraph()` factory function in the generated Flow file (where XXX is the flow graph name in PascalCase).
- **FR-010**: Generated factory function MUST create CodeNode instances with ProcessingLogic references instantiated from the configured files.
- **FR-011**: Generated factory function MUST create all Connection instances matching the flow graph definition.
- **FR-012**: System MUST allow saving a flow graph even when required properties are incomplete (work-in-progress state).
- **FR-013**: System MUST indicate visually in the UI when a flow graph has validation errors that prevent compilation.

### Key Entities

- **ProcessingLogicReference**: Configuration data associating a CodeNode with its ProcessingLogic implementation file. Stored as a file path relative to the project root.
- **CompilationValidationResult**: Contains list of validation errors, each identifying a node ID, node name, and list of missing required properties.
- **GeneratedFlowFactory**: The code artifact containing the factory function that creates a FlowGraph programmatically.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can configure ProcessingLogic references for all CodeNodes in a flow graph using the properties panel within the graphEditor session.
- **SC-002**: Compilation of a flow graph with missing required properties displays specific error messages identifying 100% of the nodes with issues.
- **SC-003**: Users can identify and resolve all validation errors without needing external documentation or assistance.
- **SC-004**: Generated factory function produces a FlowGraph that passes equality comparison with the original manually-created FlowGraph (same nodes, ports, connections, and behavior).
- **SC-005**: KMPMobileApp successfully compiles and runs using only generated code for FlowGraph creation (no manual createStopWatchFlowGraph() in App.kt).

## Assumptions

- The ProcessingLogic implementations (e.g., TimerEmitterComponent.kt, DisplayReceiverComponent.kt) already exist in the demos/stopwatch/ directory.
- The .flow.kts format can accommodate storing additional configuration properties like "processingLogicFile".
- File paths are stored relative to the project root to maintain portability across development environments.
- The existing kotlinCompiler module's FlowGenerator and ComponentGenerator can be extended to support the new factory function generation.
- The graphEditor's PropertiesPanel already supports different editor types (text field, number field, dropdown) and can be extended with a file browser editor type.

## Out of Scope

- Automatic discovery or suggestion of ProcessingLogic implementations (users must manually select files).
- Type checking that the ProcessingLogic's input/output signature matches the CodeNode's ports (this could be a future enhancement).
- Support for ProcessingLogic defined inline in the .flow.kts file (only file references are supported).
- Refactoring other demos or applications beyond KMPMobileApp.
