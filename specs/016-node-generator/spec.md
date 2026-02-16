# Feature Specification: Node Generator UI Tool

**Feature Branch**: `016-node-generator`
**Created**: 2026-02-15
**Status**: Draft
**Input**: User description: "Visual graph editor integration of Typed NodeRuntime Stubs - Create a new UI tool for the graphEditor called Node Generator with inputs for Name, Number of Inputs (0-3), and Number of Outputs (0-3), plus Create and Cancel buttons. The Node Generator will integrate the new Typed NodeRuntime Stubs functionality into the generated code. The GenericType will follow the existing naming convention (e.g., in1out1). Once the Create button is pressed, the generated Node will be added to the Generic section of the Node Palette. Place the new UI tool above the Node Palette."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create Custom Node Type (Priority: P1)

A user wants to create a custom node type with a specific number of inputs and outputs to use in their flow graph. They open the Node Generator panel, enter a name for their node (e.g., "DataMerger"), select 2 inputs and 1 output, and click Create. The new node type immediately appears in the Generic section of the Node Palette, ready to be dragged onto the canvas.

**Why this priority**: This is the core functionality of the feature. Without the ability to create custom node types, the entire feature has no value.

**Independent Test**: Can be fully tested by creating a node type with any valid configuration and verifying it appears in the Node Palette's Generic section.

**Acceptance Scenarios**:

1. **Given** the Node Generator panel is visible, **When** the user enters "DataMerger" as the name, selects 2 inputs and 1 output, and clicks Create, **Then** a new node type named "DataMerger" with genericType "in2out1" appears in the Generic section of the Node Palette.

2. **Given** the Node Generator panel is visible, **When** the user creates a node with 0 inputs and 2 outputs, **Then** the node appears with genericType "in0out2" in the Node Palette.

3. **Given** the Node Generator panel is visible, **When** the user creates a node with 3 inputs and 3 outputs, **Then** the node appears with genericType "in3out3" in the Node Palette.

---

### User Story 2 - Cancel Node Creation (Priority: P2)

A user starts entering information in the Node Generator but decides they don't want to create a node after all. They click the Cancel button, and the form is reset without adding any node to the palette.

**Why this priority**: Essential for good UX - users need a way to abort an action they've started without committing changes.

**Independent Test**: Can be tested by entering values into the form, clicking Cancel, and verifying no node is added and the form is cleared.

**Acceptance Scenarios**:

1. **Given** the user has entered "TestNode" as the name and selected inputs/outputs, **When** they click Cancel, **Then** no node is added to the Node Palette and the form fields are reset to their default state.

2. **Given** the Node Generator form has default values, **When** the user clicks Cancel, **Then** the form remains in its default state with no side effects.

---

### User Story 3 - Use Created Node in Flow Graph (Priority: P2)

After creating a custom node type, the user drags it from the Node Palette onto the canvas to use it in their flow graph. The node appears with the correct number of input and output ports as defined during creation.

**Why this priority**: Validates the end-to-end integration - the created node must actually work in the flow graph.

**Independent Test**: Can be tested by creating a node type, dragging it to the canvas, and verifying the correct port configuration.

**Acceptance Scenarios**:

1. **Given** a custom node "DataMerger" with 2 inputs and 1 output exists in the Node Palette, **When** the user drags it onto the canvas, **Then** a node instance appears with 2 input ports and 1 output port.

2. **Given** a custom node is placed on the canvas, **When** the user views its properties, **Then** the properties panel shows the correct genericType configuration.

---

### User Story 4 - Persist Custom Nodes Across Sessions (Priority: P2)

A user creates several custom node types during a session. When they close and reopen the application, all their custom node types are still available in the Node Palette's Generic section, ready to use.

**Why this priority**: Persistence ensures users don't lose their work and can build up a library of reusable custom nodes over time.

**Independent Test**: Can be tested by creating a custom node, closing the application, reopening it, and verifying the node is still present in the palette.

**Acceptance Scenarios**:

1. **Given** a user has created custom nodes "NodeA" and "NodeB", **When** the application is closed and reopened, **Then** both "NodeA" and "NodeB" appear in the Generic section of the Node Palette.

2. **Given** the CustomNodeRepository file exists with saved nodes, **When** the application starts, **Then** all saved custom node types are loaded and displayed in the Node Palette.

3. **Given** a user creates a new custom node, **When** the node is added to the palette, **Then** the CustomNodeRepository is updated and persisted to file.

---

### Edge Cases

- What happens when the user enters an empty name? The Create button should be disabled or show a validation error.
- What happens when a node with the same name already exists? The system should allow it (node IDs are unique, names don't need to be unique in the palette).
- What happens when the user enters 0 inputs and 0 outputs? This should NOT be allowed - the Create button should be disabled as a node with no ports has no utility in a flow graph.
- What happens when the CustomNodeRepository file is missing or corrupted on startup? The system should start with an empty repository and allow users to create new nodes normally.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display a Node Generator panel above the Node Palette in the same column.
- **FR-002**: The Node Generator MUST provide a text input field for the node name.
- **FR-003**: The Node Generator MUST provide a selector for Number of Inputs with valid values 0, 1, 2, or 3.
- **FR-004**: The Node Generator MUST provide a selector for Number of Outputs with valid values 0, 1, 2, or 3.
- **FR-005**: The Node Generator MUST display a Create button that adds the node to the palette.
- **FR-006**: The Node Generator MUST display a Cancel button that resets the form without adding a node.
- **FR-007**: The Create button MUST be disabled when the node name is empty or contains only whitespace.
- **FR-007a**: The Create button MUST be disabled when both Number of Inputs and Number of Outputs are set to 0.
- **FR-008**: When Create is clicked, the system MUST generate a genericType string following the pattern "inXoutY" where X is the number of inputs and Y is the number of outputs.
- **FR-009**: The created node MUST appear in the Generic section of the Node Palette immediately after creation.
- **FR-010**: The created node MUST be draggable from the palette to the canvas.
- **FR-011**: When placed on the canvas, the node MUST have the correct number of input and output ports as specified during creation.
- **FR-012**: The form MUST reset to default values after successful node creation.
- **FR-013**: The system MUST persist custom node types to a CustomNodeRepository.
- **FR-014**: The CustomNodeRepository MUST be serializable to a file for persistence across sessions.
- **FR-015**: On application startup, the system MUST load previously saved custom node types from the CustomNodeRepository and display them in the Node Palette.

### Key Entities

- **NodeTypeDefinition**: Represents a custom node type with name, genericType (e.g., "in2out1"), input port count, and output port count.
- **Node Generator Form State**: Contains the current values for name, input count, and output count being entered by the user.
- **CustomNodeRepository**: A collection of user-created custom node types that can be serialized to/from a file for persistence across sessions.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can create a new custom node type in under 10 seconds (enter name, select inputs/outputs, click Create).
- **SC-002**: 100% of valid input/output combinations (0-3 inputs × 0-3 outputs, excluding 0/0 = 15 combinations) produce correctly configured nodes.
- **SC-003**: Created nodes are immediately usable - can be dragged to canvas and connected to other nodes without refresh or restart.
- **SC-004**: The Node Generator panel is visible and accessible without scrolling when the Node Palette is visible.
- **SC-005**: Custom node types persist across application restarts with 100% data integrity (no loss of created nodes).

## Assumptions

- The Node Palette already supports a "Generic" section where custom nodes can be added dynamically.
- The existing NodeTypeDefinition and node creation infrastructure can be extended to support dynamically created node types.
- Input/output selectors will use dropdown menus or similar standard controls (specific control type is an implementation detail).
- Default values for the form: empty name, 1 input, 1 output.
- The CustomNodeRepository file will be stored in a standard application data location.

## Scope Boundaries

**In Scope**:
- Node Generator UI panel with name input and input/output selectors
- Create and Cancel buttons with appropriate behavior
- Adding created nodes to the Generic section of the Node Palette
- Integration with existing drag-and-drop functionality
- Persisting custom node types across sessions via CustomNodeRepository
- Loading saved custom node types on application startup

**Out of Scope**:
- Editing or deleting previously created custom node types
- Custom port naming during node type creation (ports will use default names like "input1", "input2", etc.)
- Custom node icons or visual styling
