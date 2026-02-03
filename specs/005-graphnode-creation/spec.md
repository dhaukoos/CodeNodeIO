# Feature Specification: GraphNode Creation Support

**Feature Branch**: `005-graphnode-creation`
**Created**: 2026-02-02
**Status**: Draft
**Input**: User description: "Add GraphNode creation support to graphEditor - Allow the user to select a group of elements (nodes and connections), both by holding the shift key and selecting a series of elements, and also by holding the shift key, clicking on the grid, and drawing out a rectangular bounding box (illustrated with dotted lines while dragging). Add a group/ungroup menu option when right-clicking on a group of selected nodes. When the group menu option (group action) is selected, create a graphNode to encapsulate the selected nodes. The input and output ports of the graphNode should reflect the ports of selected nodes that have connections leading outside of the selected group of nodes. When a graphNode is created, it becomes represented on the graphEditor by its own individual node (with a distinct UI design to differentiate it from CodeNodes). Add a small button to the UI design of a graphNode that zooms in, and then shows just the contents of the graphNode in the graphEditor. This view shows the outer boundary of the graphNode with its own input and output ports on the boundary, as well as the internal connected nodes of the graphNode. This internal graphNode view has its own small button to jump back (zoom out) to the encapsulating single graphNode, in the context of the parent graphNode or flowGraph."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Multi-Select Nodes with Shift-Click (Priority: P1)

A developer is working in the graphEditor Visual view and wants to select multiple nodes to perform a bulk operation. They hold the Shift key and click on individual nodes one at a time. Each clicked node is added to the current selection, visually indicated by a selection highlight. The developer can also Shift-click on an already-selected node to deselect it. When multiple nodes are selected, all their internal connections (connections between selected nodes) are also visually indicated as part of the selection.

**Why this priority**: Multi-selection is the foundational capability that enables all grouping operations. Without the ability to select multiple nodes, users cannot create GraphNodes. This is the essential first step.

**Independent Test**: Can be fully tested by opening a graph with 5+ nodes, Shift-clicking on 3 nodes in sequence, verifying each becomes selected (highlighted), and confirming the selection persists until explicitly cleared.

**Acceptance Scenarios**:

1. **Given** the graphEditor Visual view is open with multiple nodes, **When** the user holds Shift and clicks on a node, **Then** that node is added to the current selection with a visible highlight
2. **Given** multiple nodes are selected, **When** the user Shift-clicks on an already-selected node, **Then** that node is removed from the selection
3. **Given** multiple nodes are selected, **When** the user clicks on an empty area of the canvas (without Shift), **Then** all nodes are deselected
4. **Given** multiple nodes are selected, **When** the user views the canvas, **Then** connections between selected nodes are visually distinguished from connections to unselected nodes

---

### User Story 2 - Rectangular Selection with Shift-Drag (Priority: P2)

A developer wants to quickly select a cluster of nodes in a specific region. They hold the Shift key, click on an empty area of the canvas, and drag to draw a rectangular selection box. While dragging, a dotted-line rectangle visually shows the selection area. When they release the mouse, all nodes whose centers fall within the rectangle are added to the current selection.

**Why this priority**: Rectangular selection significantly speeds up selection of node clusters compared to individual Shift-clicking. This is a common UX pattern that users expect from visual editors.

**Independent Test**: Can be fully tested by opening a graph with clustered nodes, Shift-dragging a rectangle around a subset, and verifying all enclosed nodes become selected while nodes outside remain unselected.

**Acceptance Scenarios**:

1. **Given** the graphEditor Visual view is open, **When** the user holds Shift and clicks on empty canvas space, **Then** a rectangular selection operation begins
2. **Given** a rectangular selection is in progress, **When** the user drags the mouse, **Then** a dotted-line rectangle is drawn from the start point to the current mouse position
3. **Given** a rectangular selection is in progress, **When** the user releases the mouse button, **Then** all nodes with centers inside the rectangle are added to the selection
4. **Given** nodes are already selected, **When** the user performs a rectangular selection with Shift held, **Then** newly enclosed nodes are added to the existing selection (additive selection)

---

### User Story 3 - Group Nodes into GraphNode via Context Menu (Priority: P3)

A developer has selected multiple nodes that represent a logical unit of functionality and wants to encapsulate them into a reusable GraphNode. They right-click on the selection and choose "Group" from the context menu. The system creates a new GraphNode containing all selected nodes. The GraphNode's input ports are automatically created for any input ports on the internal nodes that had connections from outside the selection. Similarly, output ports are created for any output ports that had connections going outside the selection. The selected nodes are replaced on the canvas by a single GraphNode with a distinct visual appearance.

**Why this priority**: This is the core feature - creating GraphNodes from selections. It depends on multi-selection (US1/US2) being complete.

**Independent Test**: Can be fully tested by selecting 3 connected nodes where one has an external input and one has an external output, right-clicking, selecting "Group", and verifying a single GraphNode appears with the appropriate input and output ports.

**Acceptance Scenarios**:

1. **Given** multiple nodes are selected, **When** the user right-clicks on the selection, **Then** a context menu appears with a "Group" option
2. **Given** the context menu is open with nodes selected, **When** the user clicks "Group", **Then** a new GraphNode is created containing all selected nodes
3. **Given** selected nodes have input ports connected from unselected nodes, **When** a GraphNode is created, **Then** the GraphNode has input ports corresponding to those external connections
4. **Given** selected nodes have output ports connected to unselected nodes, **When** a GraphNode is created, **Then** the GraphNode has output ports corresponding to those external connections
5. **Given** a GraphNode is created, **When** viewing the canvas, **Then** the GraphNode has a visually distinct appearance from CodeNodes (different border, color, or icon)
6. **Given** connections existed from external nodes to the selected nodes, **When** a GraphNode is created, **Then** those connections are redirected to the GraphNode's corresponding ports

---

### User Story 4 - Ungroup GraphNode via Context Menu (Priority: P4)

A developer wants to break apart an existing GraphNode back into its constituent nodes. They right-click on a GraphNode and choose "Ungroup" from the context menu. The GraphNode is removed and replaced by its internal nodes, which are positioned on the canvas. External connections that were attached to the GraphNode's ports are reconnected to the original internal node ports.

**Why this priority**: Ungrouping enables users to modify or reorganize their graph structure. It's the inverse of grouping and essential for iterative design.

**Independent Test**: Can be fully tested by right-clicking on an existing GraphNode, selecting "Ungroup", and verifying the internal nodes appear on the canvas with their original connections restored.

**Acceptance Scenarios**:

1. **Given** a GraphNode is selected, **When** the user right-clicks on it, **Then** a context menu appears with an "Ungroup" option
2. **Given** the context menu is open on a GraphNode, **When** the user clicks "Ungroup", **Then** the GraphNode is removed from the canvas
3. **Given** a GraphNode is ungrouped, **When** viewing the canvas, **Then** all internal nodes are now visible on the canvas
4. **Given** a GraphNode had external connections, **When** it is ungrouped, **Then** those connections are restored to the original internal node ports
5. **Given** a GraphNode is ungrouped, **When** viewing the canvas, **Then** the internal nodes are positioned reasonably (not all stacked on top of each other)

---

### User Story 5 - Zoom Into GraphNode (Priority: P5)

A developer wants to view and edit the internal structure of a GraphNode. They click a small "zoom in" button on the GraphNode's visual representation. The canvas view transitions to show only the contents of that GraphNode: its internal nodes and their connections. The view also displays the GraphNode's boundary with its input and output ports positioned along the edges, showing how data flows into and out of the GraphNode.

**Why this priority**: Hierarchical navigation enables users to manage complexity in large graphs. This depends on GraphNodes existing (US3).

**Independent Test**: Can be fully tested by clicking the zoom-in button on a GraphNode and verifying the view changes to show internal nodes, internal connections, and boundary ports.

**Acceptance Scenarios**:

1. **Given** a GraphNode is visible on the canvas, **When** the user views the GraphNode, **Then** a small "zoom in" button is visible on the GraphNode
2. **Given** a GraphNode is visible, **When** the user clicks the zoom-in button, **Then** the canvas view transitions to show the GraphNode's internal contents
3. **Given** the user is viewing a GraphNode's internal view, **When** viewing the canvas, **Then** the GraphNode's boundary is displayed with input ports on the left edge and output ports on the right edge
4. **Given** the user is viewing a GraphNode's internal view, **When** viewing the canvas, **Then** all internal nodes and their connections are visible
5. **Given** the user is viewing a GraphNode's internal view, **When** viewing the canvas, **Then** a visual indicator shows the current navigation context (e.g., breadcrumb or title)

---

### User Story 6 - Zoom Out from GraphNode View (Priority: P6)

A developer is viewing the internal contents of a GraphNode and wants to return to the parent context (either the top-level FlowGraph or a parent GraphNode if nested). They click a "zoom out" button visible in the internal view. The canvas transitions back to show the parent context with the GraphNode displayed as a single node.

**Why this priority**: This completes the hierarchical navigation feature, allowing users to move up and down the graph hierarchy.

**Independent Test**: Can be fully tested by zooming into a GraphNode, then clicking the zoom-out button, and verifying the view returns to the parent context with the GraphNode visible.

**Acceptance Scenarios**:

1. **Given** the user is viewing a GraphNode's internal view, **When** viewing the UI, **Then** a "zoom out" button is visible
2. **Given** the user is viewing a GraphNode's internal view, **When** the user clicks the zoom-out button, **Then** the canvas transitions to the parent context
3. **Given** a nested GraphNode exists (GraphNode inside GraphNode), **When** the user zooms out from the innermost GraphNode, **Then** the view shows the containing GraphNode's internal view
4. **Given** the user is at the top-level FlowGraph, **When** viewing the UI, **Then** no zoom-out button is displayed (or it is disabled)

---

### Edge Cases

- What happens when the user tries to group a single node? The "Group" option is disabled or hidden - grouping requires at least 2 nodes.
- What happens when all selected nodes have only internal connections (no external connections)? A GraphNode is created with zero input ports and zero output ports.
- What happens when grouping creates a GraphNode with many ports (e.g., 10+ inputs)? Ports are arranged to fit within the GraphNode visual, potentially with scrolling or compact display.
- What happens when ungrouping a GraphNode that is nested inside another GraphNode? The internal nodes become siblings within the parent GraphNode (they don't escape to the top level).
- What happens when the user tries to ungroup a CodeNode? The "Ungroup" option is not available for CodeNodes - only GraphNodes can be ungrouped.
- What happens when connections between selected nodes form a cycle? The cycle is preserved within the GraphNode; no special handling needed.
- How does rectangular selection handle nodes that are partially inside the selection rectangle? Selection is based on node center point - if center is inside, node is selected.

## Requirements *(mandatory)*

### Functional Requirements

**Multi-Selection**

- **FR-001**: System MUST support Shift-click to add/remove individual nodes from the current selection
- **FR-002**: System MUST support Shift-drag to perform rectangular selection starting from empty canvas space
- **FR-003**: System MUST display a dotted-line rectangle during rectangular selection drag operations
- **FR-004**: System MUST visually distinguish selected nodes from unselected nodes (selection highlight)
- **FR-005**: System MUST visually distinguish connections between selected nodes from connections to unselected nodes
- **FR-006**: System MUST clear selection when clicking on empty canvas space without Shift held
- **FR-007**: System MUST support selecting both CodeNodes and GraphNodes using the same selection mechanisms

**Grouping/Ungrouping**

- **FR-008**: System MUST display a context menu with "Group" option when right-clicking on a multi-node selection
- **FR-009**: System MUST create a GraphNode containing all selected nodes when "Group" is selected
- **FR-010**: System MUST automatically generate input ports on the GraphNode for each input port on internal nodes that has external connections
- **FR-011**: System MUST automatically generate output ports on the GraphNode for each output port on internal nodes that has external connections
- **FR-012**: System MUST redirect external connections to the appropriate GraphNode ports after grouping
- **FR-013**: System MUST display a context menu with "Ungroup" option when right-clicking on a GraphNode
- **FR-014**: System MUST restore internal nodes to the canvas when "Ungroup" is selected
- **FR-015**: System MUST restore external connections to original internal node ports when ungrouping
- **FR-016**: System MUST position ungrouped nodes reasonably on the canvas (not overlapping)

**GraphNode Visual Design**

- **FR-017**: System MUST render GraphNodes with a visually distinct appearance from CodeNodes
- **FR-018**: System MUST display a "zoom in" button on GraphNode visual representation
- **FR-019**: System MUST display input ports on the left side of the GraphNode and output ports on the right side

**Hierarchical Navigation**

- **FR-020**: System MUST transition to internal GraphNode view when zoom-in button is clicked
- **FR-021**: System MUST display GraphNode boundary with ports in the internal view
- **FR-022**: System MUST position input ports along the left boundary edge and output ports along the right boundary edge in internal view
- **FR-023**: System MUST display a "zoom out" button in the internal GraphNode view
- **FR-024**: System MUST transition to parent context when zoom-out button is clicked
- **FR-025**: System MUST support nested GraphNodes (GraphNodes containing GraphNodes)
- **FR-026**: System MUST display navigation context indicator (breadcrumb or title) showing current location in hierarchy

**Persistence**

- **FR-027**: System MUST serialize GraphNode hierarchy to .flow.kts files
- **FR-028**: System MUST deserialize GraphNode hierarchy from .flow.kts files
- **FR-029**: System MUST preserve internal node positions within GraphNodes during serialization

### Key Entities

- **GraphNode**: A composite node that encapsulates other nodes (CodeNodes or nested GraphNodes). Contains: internal nodes, internal connections, auto-generated interface ports, position and size on canvas, visual state (collapsed/expanded)
- **Selection**: The set of currently selected nodes. Managed as state in the graphEditor. Supports add, remove, clear, and bulk operations.
- **SelectionBox**: A temporary visual element during rectangular selection. Contains: start position, current position, calculated bounds.
- **NavigationContext**: Represents the current view location in the graph hierarchy. Can be: top-level FlowGraph, or a specific GraphNode (with path to root).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can select 10 nodes using rectangular selection in under 2 seconds
- **SC-002**: Users can group a selection of nodes into a GraphNode in under 3 seconds (right-click + menu selection)
- **SC-003**: GraphNode creation correctly identifies 100% of external connections and generates appropriate ports
- **SC-004**: Users can navigate into a GraphNode and back out in under 2 seconds total
- **SC-005**: Nested GraphNodes up to 5 levels deep function correctly with proper navigation
- **SC-006**: GraphNode visual appearance is immediately distinguishable from CodeNodes (verified by user testing)
- **SC-007**: Grouped/ungrouped graphs serialize and deserialize with 100% fidelity

## Assumptions

- The existing GraphNode model class in fbpDsl already supports containing child nodes and connections
- The existing serialization infrastructure can be extended to handle hierarchical GraphNode structures
- The context menu infrastructure from the IP Palette Support feature (003) can be reused
- GraphNode ports inherit the same data types as the internal node ports they represent
- Node positions are stored relative to their container (FlowGraph or parent GraphNode)
- The zoom in/out operations are instantaneous transitions (no animation required for MVP)
- Selection state is transient and not persisted to files
