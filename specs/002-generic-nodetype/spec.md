# Feature Specification: Generic NodeType Definition

**Feature Branch**: `002-generic-nodetype`
**Created**: 2026-01-28
**Status**: Draft
**Input**: User description: "Add Generic NodeCategory to NodeTypeDefinition in fbpDsl module with configurable inputs/outputs, supporting 0-5 inputs and 0-5 outputs, with customizable properties including name, icon, port names, and UseCase class reference"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create Generic Node from Palette (Priority: P1)

As a developer using the visual flow graph editor, I want to drag a generic node type from the palette onto the canvas so that I can create flexible processing nodes with a specific number of inputs and outputs without being constrained to predefined specialized node types.

**Why this priority**: This is the core functionality - without the ability to add generic nodes to the canvas, no other features matter. Generic nodes provide the flexibility needed for custom processing logic that doesn't fit existing categories.

**Independent Test**: Can be fully tested by opening the graph editor, locating the Generic category in the node palette, dragging a generic node (e.g., "in2out1") onto the canvas, and verifying the node appears with the correct number of input and output ports.

**Acceptance Scenarios**:

1. **Given** the graph editor is open with an empty canvas, **When** I expand the "Generic" category in the node palette, **Then** I see a list of generic node types with names indicating their input/output configuration (e.g., "in0out1", "in1out1", "in2out3").

2. **Given** I have selected a generic node type "in2out1" from the palette, **When** I drag it onto the canvas, **Then** a new node is created with exactly 2 input ports and 1 output port.

3. **Given** a generic node exists on the canvas, **When** I inspect the node, **Then** I see default port names (e.g., "input1", "input2" for inputs and "output1" for outputs).

---

### User Story 2 - Configure Generic Node Properties (Priority: P2)

As a developer, I want to customize a generic node's properties including its display name, icon, port names, and associated processing logic so that the node clearly represents its purpose in my flow graph.

**Why this priority**: Customization enables meaningful use of generic nodes. Without configuration, generic nodes would be indistinguishable and difficult to understand in complex graphs.

**Independent Test**: Can be tested by selecting a generic node on the canvas, opening the properties panel, modifying the node name and port names, and verifying the changes are reflected in both the canvas display and the serialized graph.

**Acceptance Scenarios**:

1. **Given** I have a generic "in1out1" node selected on the canvas, **When** I open the properties panel, **Then** I see editable fields for: node name, icon/image resource, input port name, output port name, and UseCase class reference.

2. **Given** I am editing a generic node's properties, **When** I change the node name from "in1out1" to "ValidateEmail", **Then** the node's display name on the canvas updates to "ValidateEmail".

3. **Given** I am editing a generic node with 2 inputs, **When** I rename "input1" to "emailAddress" and "input2" to "validationRules", **Then** the port labels on the canvas update to show the new names.

4. **Given** I have configured a UseCase class reference for a generic node, **When** I save and reload the flow graph, **Then** the UseCase reference is preserved.

---

### User Story 3 - Generate Code from Generic Nodes (Priority: P3)

As a developer, I want to generate code from flow graphs containing generic nodes so that my custom processing logic is included in the generated application.

**Why this priority**: Code generation is the ultimate purpose of flow graphs. Generic nodes must participate in code generation to be useful for building real applications.

**Independent Test**: Can be tested by creating a flow graph with generic nodes that have UseCase references, triggering KMP code generation, and verifying the generated code includes appropriate component classes that reference the specified UseCases.

**Acceptance Scenarios**:

1. **Given** a flow graph contains a generic node with a UseCase class reference, **When** I generate KMP code, **Then** the generated code includes a component that delegates to the specified UseCase class.

2. **Given** a flow graph contains a generic node without a UseCase class reference, **When** I generate code, **Then** the generated code includes a placeholder component with TODO comments indicating where processing logic should be implemented.

3. **Given** a generic node has custom port names configured, **When** code is generated, **Then** the generated component uses the custom port names for its channel/flow parameters.

---

### User Story 4 - Serialize and Deserialize Generic Nodes (Priority: P2)

As a developer, I want generic nodes to be properly saved and loaded in .flow.kts files so that my flow graph configurations persist across sessions.

**Why this priority**: Persistence is essential for practical use. Without proper serialization, all configuration would be lost when closing the editor.

**Independent Test**: Can be tested by creating a flow graph with configured generic nodes, saving to a .flow.kts file, closing and reopening the file, and verifying all generic node configurations are restored correctly.

**Acceptance Scenarios**:

1. **Given** a flow graph contains generic nodes with custom configurations, **When** I save the graph, **Then** the .flow.kts file contains DSL syntax representing the generic nodes with all their properties.

2. **Given** a .flow.kts file contains generic node definitions, **When** I open the file in the graph editor, **Then** the generic nodes are recreated with their correct input/output counts, names, and configurations.

---

### Edge Cases

- What happens when a user tries to create a generic node with 0 inputs AND 0 outputs? (Should be allowed - represents a generator/source node or a termination point)
- What happens when a user specifies more than 5 inputs or outputs? (Should be rejected with a validation error)
- How does the system handle a UseCase reference to a class that doesn't exist? (Should show a warning but allow the configuration - the class may be created later)
- What happens when a generic node's port count doesn't match the connected edges after loading? (Should display validation errors on affected connections)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST add a "GENERIC" category to NodeCategory enum for organizing generic node types in the palette
- **FR-002**: System MUST provide a factory function `createGenericNodeType(numInputs: Int, numOutputs: Int)` that creates NodeTypeDefinition instances with the specified port counts
- **FR-003**: Factory function MUST validate that numInputs is between 0 and 5 (inclusive)
- **FR-004**: Factory function MUST validate that numOutputs is between 0 and 5 (inclusive)
- **FR-005**: Generic node types MUST have default names following the pattern "in[M]out[N]" (e.g., "in1out0", "in3out2")
- **FR-006**: Generic node types MUST generate default input port names as "input1", "input2", etc.
- **FR-007**: Generic node types MUST generate default output port names as "output1", "output2", etc.
- **FR-008**: Generic NodeTypeDefinition MUST support an optional icon/image resource property
- **FR-009**: Generic NodeTypeDefinition MUST support custom display names that override the default "in[M]out[N]" pattern
- **FR-010**: Generic NodeTypeDefinition MUST support custom port names that override the defaults
- **FR-011**: Generic NodeTypeDefinition MUST support a UseCase class reference property for specifying processing logic
- **FR-012**: NodePalette component MUST display the Generic category with available generic node types
- **FR-013**: Code generators MUST support generating code from generic nodes using the UseCase reference when provided
- **FR-014**: Serialization MUST persist all generic node properties including custom names, icon references, and UseCase references
- **FR-015**: Deserialization MUST restore generic nodes with all their configured properties

### Key Entities

- **GenericNodeTypeDefinition**: Extension or configuration of NodeTypeDefinition specifically for generic nodes, containing: base node type definition, custom display name, icon/image resource reference, custom port name mappings, UseCase class reference
- **NodeCategory.GENERIC**: New enumeration value representing the generic node category
- **UseCase Reference**: A string or class reference property that links a generic node to its processing logic implementation

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All 36 possible generic node type combinations (0-5 inputs × 0-5 outputs) can be created via the factory function
- **SC-002**: Generic nodes appear in the node palette within 100ms of palette initialization
- **SC-003**: Users can configure a generic node's properties (name, ports, UseCase) in under 30 seconds
- **SC-004**: Flow graphs containing generic nodes save and load with 100% fidelity of configuration
- **SC-005**: Code generation for flow graphs with generic nodes completes successfully when UseCase references are valid
- **SC-006**: 100% of existing NodeTypeDefinition integration points support the new GENERIC category without modification to calling code

## Assumptions

- Generic nodes use the same data type system as existing nodes (Any/generic type for maximum flexibility)
- The UseCase class reference is a fully-qualified class name string that will be validated at code generation time, not at configuration time
- Icons/images for generic nodes are optional and the system provides a default generic node icon
- The 0-5 input/output limit is based on practical visual layout constraints in the graph editor
- All existing node type infrastructure (validation, rendering, connection handling) applies to generic nodes
