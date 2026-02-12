# Feature Specification: StopWatch Virtual Circuit Refactor

**Feature Branch**: `011-stopwatch-refactor`
**Created**: 2026-02-12
**Status**: Draft
**Input**: User description: "Refactor StopWatch virtual circuit to eliminate redundant createStopWatchFlowGraph() function in App.kt by using the StopWatch module directly. Add required property validation in graphEditor properties panel and compile-time requirement checking."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Compile Validation for Required Properties (Priority: P1)

A graph designer working in the graphEditor attempts to compile a flow graph that has nodes with missing required configuration properties. The system validates all required inputs before compilation and provides clear feedback about which requirements are unmet.

**Why this priority**: This is foundational - without compile-time validation, users cannot know what properties need to be filled in before the code generation will work correctly. This prevents broken code generation and guides users to complete their configurations.

**Independent Test**: Can be tested by creating a node in the graphEditor with missing required properties (like `_useCaseClass`) and clicking Compile. The system should display a validation message listing the unmet requirements.

**Acceptance Scenarios**:

1. **Given** a flow graph with a CodeNode missing required property `_useCaseClass`, **When** the user clicks Compile, **Then** the system displays a message: "Compilation blocked: Node 'TimerEmitter' missing required property: _useCaseClass"
2. **Given** a flow graph with multiple nodes missing required properties, **When** the user clicks Compile, **Then** the system lists all nodes and their missing required properties
3. **Given** a flow graph with all required properties filled in, **When** the user clicks Compile, **Then** compilation proceeds normally

---

### User Story 2 - Properties Panel Shows Required Properties (Priority: P2)

A graph designer selects a CodeNode in the graphEditor and the properties panel displays all required configuration properties for that node type, clearly indicating which are mandatory. The designer can input values for these properties directly in the panel.

**Why this priority**: After users understand what's required (P1), they need a way to actually fill in those requirements. The properties panel is the primary interface for this configuration.

**Independent Test**: Can be tested by selecting a GENERIC node type and verifying the properties panel shows the `_useCaseClass` field with a required indicator. Input a value and verify it persists.

**Acceptance Scenarios**:

1. **Given** a GENERIC CodeNode is selected, **When** the user views the properties panel, **Then** the `_useCaseClass` property appears with a required indicator (*)
2. **Given** a GENERIC CodeNode with `speedAttenuation` property, **When** the user views the properties panel, **Then** the `speedAttenuation` property appears as an editable field
3. **Given** the user enters a value in a required property field, **When** the user clicks away or saves, **Then** the value is persisted to the node's configuration

---

### User Story 3 - Remove Redundant FlowGraph Creation (Priority: P3)

The mobile app (KMPMobileApp) currently has a `createStopWatchFlowGraph()` function that duplicates the `stopWatchFlowGraph` defined in StopWatch.flow.kt. The app should reference the StopWatch module's exported flow graph definition directly instead of maintaining a redundant copy.

**Why this priority**: This is the refactoring goal, but it depends on P1 and P2 being in place. Once the graphEditor can properly handle required properties and validate them, the StopWatch module becomes the single source of truth.

**Independent Test**: Can be tested by removing `createStopWatchFlowGraph()` from App.kt and importing `stopWatchFlowGraph` from the StopWatch module. The mobile app should continue to function identically.

**Acceptance Scenarios**:

1. **Given** the KMPMobileApp imports `stopWatchFlowGraph` from StopWatch module, **When** the app starts, **Then** the stopwatch UI renders correctly
2. **Given** the stopwatch is running using the module's flow graph, **When** time elapses, **Then** the display updates with elapsed seconds and minutes
3. **Given** the `createStopWatchFlowGraph()` function is removed, **When** the project compiles, **Then** there are no compilation errors

---

### Edge Cases

- What happens when a node has no required properties defined? The properties panel shows all available properties without required indicators.
- How does the system handle a node type that doesn't have a configuration schema? The system falls back to treating all properties as optional strings.
- What happens if a user clears a required property value that was previously set? The compile validation catches this and reports the missing requirement.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST identify required configuration properties for each CodeNode type
- **FR-002**: System MUST display required property indicators (*) in the properties panel for mandatory fields
- **FR-003**: System MUST validate all required properties are defined before compilation
- **FR-004**: System MUST display clear error messages listing each node and its missing required properties when validation fails
- **FR-005**: System MUST allow users to input values for all configuration properties in the properties panel
- **FR-006**: System MUST persist property values to the node's configuration when edited
- **FR-007**: KMPMobileApp MUST reference the StopWatch module's `stopWatchFlowGraph` instead of duplicating the definition
- **FR-008**: The `_useCaseClass` property MUST be required for GENERIC node types to enable code generation

### Key Entities

- **CodeNode**: A processing unit in the flow graph with a configuration map containing key-value pairs. Some configurations are required for code generation (e.g., `_useCaseClass` for GENERIC nodes).
- **PropertyDefinition**: Metadata about a configuration property including name, type, required status, and validation constraints.
- **CompilationResult**: The outcome of a compile operation, including success status, generated files, or validation error messages.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can see required property indicators for all mandatory configuration fields in the properties panel
- **SC-002**: Compilation attempts with missing required properties are blocked and display clear error messages within 2 seconds
- **SC-003**: 100% of nodes with missing required properties are reported in the validation error message
- **SC-004**: The KMPMobileApp successfully runs using the imported `stopWatchFlowGraph` from the StopWatch module
- **SC-005**: Zero redundant FlowGraph definitions exist in the codebase after refactoring

## Assumptions

- The `_useCaseClass` property is the primary required property for GENERIC node types
- The existing `PropertiesPanel` and `CompilationService` classes will be extended rather than replaced
- The StopWatch module's `stopWatchFlowGraph` variable is already properly exported and accessible
- Property definitions for built-in node types will be derived from their JSON configuration schemas
