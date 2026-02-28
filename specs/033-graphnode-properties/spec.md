# Feature Specification: View GraphNode Properties

**Feature Branch**: `033-graphnode-properties`
**Created**: 2026-02-27
**Status**: Draft
**Input**: User description: "When a GraphNode is selected in the graphEditor, its properties should be displayed in the Properties panel on the right side. Like for CodeNodes, at the top is a TextField for the name property, and below that the Input and Output Ports. Below that, unique to GraphNodes, is a list of the names of the child Nodes."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View GraphNode Name and Ports (Priority: P1)

When a user selects a GraphNode on the canvas, the Properties Panel on the right displays the GraphNode's name in an editable text field at the top, followed by its input and output ports — matching the existing layout used for CodeNodes.

**Why this priority**: This is the core value of the feature. Without seeing and editing the GraphNode's name and ports, the Properties Panel is empty/useless when a GraphNode is selected.

**Independent Test**: Can be fully tested by creating a GraphNode (grouping two or more nodes), selecting it, and verifying the Properties Panel shows the name field and port sections.

**Acceptance Scenarios**:

1. **Given** a flow graph with a GraphNode that has input and output ports, **When** the user clicks on the GraphNode, **Then** the Properties Panel displays an editable name text field at the top with the GraphNode's current name.
2. **Given** a selected GraphNode with input ports, **When** the Properties Panel is visible, **Then** it lists each input port with its name and type, matching the existing CodeNode port display format.
3. **Given** a selected GraphNode with output ports, **When** the Properties Panel is visible, **Then** it lists each output port with its name and type, matching the existing CodeNode port display format.
4. **Given** a selected GraphNode, **When** the user edits the name text field, **Then** the GraphNode's name updates in the flow graph.

---

### User Story 2 - View Child Nodes List (Priority: P2)

Below the ports section, the Properties Panel displays a read-only list of the names of the GraphNode's child nodes, giving the user a quick overview of what's inside the group without having to navigate into it.

**Why this priority**: This is the GraphNode-specific value — seeing child node names at a glance. It builds on the P1 foundation but delivers unique insight specific to GraphNodes.

**Independent Test**: Can be tested by creating a GraphNode containing multiple child nodes, selecting it, and verifying the child node names appear in the Properties Panel below the ports.

**Acceptance Scenarios**:

1. **Given** a selected GraphNode containing three child nodes named "Generator", "Transform", and "Sink", **When** the Properties Panel is visible, **Then** a "Child Nodes" section appears below the ports listing all three names.
2. **Given** a selected GraphNode with no child nodes (edge case — freshly created empty group), **When** the Properties Panel is visible, **Then** the "Child Nodes" section shows an appropriate empty state message.

---

### Edge Cases

- What happens when a GraphNode has no ports? The ports sections should be omitted, and the child nodes section appears directly below the name.
- What happens when a GraphNode has many child nodes? The list should be scrollable within the existing panel scroll area.
- What happens when the user switches selection from a CodeNode to a GraphNode? The Properties Panel should update to show the GraphNode layout (with child nodes section) instead of the CodeNode layout (with configuration properties).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Properties Panel MUST display an editable name text field when a GraphNode is selected, pre-populated with the GraphNode's current name.
- **FR-002**: The Properties Panel MUST display input and output port sections for a selected GraphNode, using the same format as CodeNode port display.
- **FR-003**: The Properties Panel MUST display a "Child Nodes" section below the ports, listing the name of each child node contained within the selected GraphNode.
- **FR-004**: The child nodes list MUST be read-only (display only, no editing or interaction).
- **FR-005**: Editing the GraphNode name in the text field MUST update the GraphNode's name in the flow graph.
- **FR-006**: The Properties Panel MUST correctly transition between CodeNode, GraphNode, connection, and IP type display modes when the user changes their selection.

### Key Entities

- **GraphNode**: A composite node that contains child nodes and internal connections. Key attributes: name, input ports, output ports, child nodes list.
- **Child Node**: Any node (CodeNode or nested GraphNode) contained within a GraphNode. Displayed by name in the Properties Panel.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can see the name, ports, and child node names of a selected GraphNode immediately upon selection.
- **SC-002**: Users can edit the GraphNode name directly in the Properties Panel and see the change reflected in the graph.
- **SC-003**: Users can identify all child nodes within a GraphNode without navigating into it, by reading the child nodes list in the Properties Panel.
- **SC-004**: Switching selection between CodeNodes, GraphNodes, connections, and IP types always displays the correct properties view with no stale or mixed content.
