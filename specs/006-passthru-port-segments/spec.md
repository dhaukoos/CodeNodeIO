# Feature Specification: GraphNode Port and Connection Details

**Feature Branch**: `006-passthru-port-segments`
**Created**: 2026-02-04
**Status**: Draft
**Input**: User description: "GraphNode Port and Connection Details - Create PassThruPort and ConnectionSegment elements for boundary connections"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - PassThruPort Creation on GraphNode Boundary (Priority: P1)

A developer groups multiple CodeNodes into a GraphNode. Connections that cross the GraphNode boundary (connecting internal nodes to external nodes) are automatically handled by creating PassThruPorts at the boundary. These PassThruPorts act as intermediaries, referencing both the upstream and downstream ports they connect.

**Why this priority**: PassThruPorts are the foundational element that enables connections to cross GraphNode boundaries. Without them, the connection segmentation feature cannot function.

**Independent Test**: Group 3 nodes where one has an external incoming connection and another has an external outgoing connection. Verify that PassThruPorts are created on the GraphNode boundary with correct upstream/downstream references.

**Acceptance Scenarios**:

1. **Given** a connection from ExternalNode.output to InternalNode.input, **When** InternalNode is grouped into a GraphNode, **Then** an INPUT PassThruPort is created on the GraphNode with upstreamPort=ExternalNode.output and downstreamPort=InternalNode.input
2. **Given** a connection from InternalNode.output to ExternalNode.input, **When** InternalNode is grouped into a GraphNode, **Then** an OUTPUT PassThruPort is created on the GraphNode with upstreamPort=InternalNode.output and downstreamPort=ExternalNode.input
3. **Given** PassThruPort references upstreamPort with dataType=String and downstreamPort with dataType=String, **When** validating the PassThruPort, **Then** validation succeeds because all types match
4. **Given** PassThruPort references upstreamPort with dataType=String and downstreamPort with dataType=Integer, **When** attempting to create the PassThruPort, **Then** creation fails with type mismatch error

---

### User Story 2 - ConnectionSegment Representation (Priority: P2)

A developer views a Connection in the graph editor. The Connection displays as one or more ConnectionSegments rendered as bezier curves. Direct connections between two CodeNode Ports have a single segment. Connections that pass through a GraphNode boundary have multiple segments - one for each view context (exterior/interior).

**Why this priority**: ConnectionSegments enable the visual representation of connections that span GraphNode boundaries, making the hierarchical structure understandable to users.

**Independent Test**: Create a connection between two CodeNodes, verify it has one segment. Then group one node into a GraphNode, verify the original connection now has two segments.

**Acceptance Scenarios**:

1. **Given** a Connection between CodeNode A.output and CodeNode B.input, **When** viewing the connection, **Then** it contains exactly one ConnectionSegment
2. **Given** a Connection passing through a single GraphNode boundary (ExternalNode to PassThruPort to InternalNode), **When** viewing the connection, **Then** it contains exactly two ConnectionSegments
3. **Given** a Connection passing through two nested GraphNode boundaries, **When** viewing the connection, **Then** it contains exactly three ConnectionSegments
4. **Given** ConnectionSegment objects, **When** rendered in the graph editor, **Then** each segment displays as a bezier curve connecting its source to target

---

### User Story 3 - Visual Distinction of PassThruPorts (Priority: P3)

A developer viewing a GraphNode in the graph editor can visually distinguish PassThruPorts from regular Ports. Regular Ports on CodeNodes appear as small circles. PassThruPorts on GraphNode boundaries appear as small squares.

**Why this priority**: Visual distinction helps users understand the flow of data through hierarchical graphs and identify boundary crossing points.

**Independent Test**: Render a GraphNode with PassThruPorts and a CodeNode with regular Ports side by side, verify the port shapes are visually distinct.

**Acceptance Scenarios**:

1. **Given** a CodeNode with INPUT and OUTPUT Ports, **When** rendered in the graph editor, **Then** ports appear as small circles
2. **Given** a GraphNode with PassThruPorts on its boundary, **When** rendered in the graph editor, **Then** PassThruPorts appear as small squares
3. **Given** the interior view of a GraphNode, **When** PassThruPorts are visible on the boundary, **Then** they appear as squares on the boundary edge

---

### User Story 4 - Automatic Segment Creation When Grouping (Priority: P4)

A developer groups nodes into a GraphNode. Connections that previously connected directly between nodes are automatically split into segments at the GraphNode boundary. The exterior segment connects to the exterior view, the interior segment connects within the internal view.

**Why this priority**: Automatic segment creation ensures data flow continuity when refactoring graphs through grouping operations.

**Independent Test**: Create a flow A->B->C with connections. Group B into a GraphNode. Verify both connections now have two segments each, with PassThruPorts joining them.

**Acceptance Scenarios**:

1. **Given** Connection from ExternalNode to InternalNode, **When** InternalNode is grouped into GraphNode, **Then** Connection is split into exterior segment (ExternalNode to PassThruPort) and interior segment (PassThruPort to InternalNode)
2. **Given** Connection from InternalNode to ExternalNode, **When** InternalNode is grouped into GraphNode, **Then** Connection is split into interior segment (InternalNode to PassThruPort) and exterior segment (PassThruPort to ExternalNode)
3. **Given** the exterior view of a FlowGraph containing a GraphNode, **When** viewing connections to the GraphNode, **Then** only exterior segments are visible, connecting to PassThruPort squares on the boundary
4. **Given** the interior view of a GraphNode, **When** viewing connections from PassThruPorts, **Then** only interior segments are visible, connecting PassThruPort squares to internal nodes

---

### User Story 5 - Segment Visibility by Navigation Context (Priority: P5)

A developer navigating between root level and GraphNode internal views sees only the ConnectionSegments relevant to their current view context. Exterior segments appear at the parent level; interior segments appear inside the GraphNode.

**Why this priority**: Context-appropriate visibility prevents visual clutter and helps users focus on the relevant portion of the graph.

**Independent Test**: Navigate into a GraphNode, verify only interior segments are shown. Navigate out, verify only exterior segments are shown.

**Acceptance Scenarios**:

1. **Given** a Connection with exterior and interior segments, **When** viewing the root FlowGraph level, **Then** only the exterior segment is rendered
2. **Given** a Connection with exterior and interior segments, **When** navigating into the GraphNode, **Then** only the interior segment is rendered
3. **Given** nested GraphNodes with a connection spanning multiple boundaries, **When** navigating through each level, **Then** only segments belonging to that level are visible

---

### Edge Cases

- What happens when a PassThruPort's upstream or downstream port is deleted? The PassThruPort and its associated segments should be removed, and the connection marked invalid.
- What happens when ungrouping a GraphNode? PassThruPorts are removed, segments are merged back into single-segment connections where possible.
- What happens with self-loop connections within a GraphNode? They remain as single-segment internal connections, never crossing the boundary.
- What happens when a connection passes through multiple nested GraphNode boundaries? Each boundary crossing adds a segment pair, creating a chain of PassThruPorts.
- What happens when dataType compatibility is lost during refactoring? The PassThruPort creation fails and the user is notified of the type mismatch.

## Requirements *(mandatory)*

### Functional Requirements

#### PassThruPort Model

- **FR-001**: System MUST define PassThruPort as a subtype of Port with additional upstreamPort and downstreamPort references
- **FR-002**: System MUST validate that PassThruPort.dataType matches both upstreamPort.dataType and downstreamPort.dataType
- **FR-003**: System MUST validate that PassThruPort.direction matches downstreamPort.direction
- **FR-004**: System MUST ensure PassThruPort can only be owned by a GraphNode (not CodeNode)
- **FR-005**: System MUST store references to upstream and downstream ports as port identifiers (not full objects) to maintain loose coupling

#### ConnectionSegment Model

- **FR-006**: System MUST define ConnectionSegment as a data structure containing source reference, target reference, and parent connection reference
- **FR-007**: System MUST add a segments property to Connection as an ordered list of ConnectionSegments
- **FR-008**: System MUST ensure segments list has at least one element for any valid Connection
- **FR-009**: System MUST ensure segments are ordered from source to target (upstream to downstream)
- **FR-010**: System MUST ensure adjacent segments share a common endpoint (PassThruPort)

#### Automatic Segment Generation

- **FR-011**: System MUST automatically create PassThruPorts when grouping nodes causes connections to cross the GraphNode boundary
- **FR-012**: System MUST automatically split connections into segments when PassThruPorts are created
- **FR-013**: System MUST preserve the original Connection identity (same ID) when splitting into segments
- **FR-014**: System MUST automatically merge segments back into single-segment connections when ungrouping removes PassThruPorts

#### Visual Rendering

- **FR-015**: System MUST render regular Ports as small circles in the graph editor
- **FR-016**: System MUST render PassThruPorts as small squares in the graph editor
- **FR-017**: System MUST render ConnectionSegments as bezier curves
- **FR-018**: System MUST display only segments relevant to the current navigation context
- **FR-019**: System MUST display PassThruPorts on the GraphNode boundary in both exterior and interior views

#### Validation

- **FR-020**: System MUST validate that PassThruPort types match connected ports before creation
- **FR-021**: System MUST validate that all segments in a Connection form a continuous path
- **FR-022**: System MUST reject PassThruPort creation when type compatibility cannot be established

### Key Entities

- **PassThruPort**: A specialized Port that exists on GraphNode boundaries, containing references to both the upstream port (data source direction) and downstream port (data sink direction). Inherits from Port, adding upstreamPortId, upstreamNodeId, downstreamPortId, and downstreamNodeId properties.

- **ConnectionSegment**: A portion of a Connection representing the visual path between two endpoints. Contains sourceNodeId, sourcePortId, targetNodeId, targetPortId, and parentConnectionId. Rendered as a bezier curve in the UI.

- **Connection (extended)**: The existing Connection entity, extended with a segments list containing one or more ConnectionSegments that represent the complete path from source to target.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Developers can group nodes with external connections, and PassThruPorts are automatically created on the GraphNode boundary within 500ms
- **SC-002**: Connections crossing a single GraphNode boundary display as two distinct visual segments in the appropriate view contexts
- **SC-003**: Users can visually distinguish PassThruPorts (squares) from regular Ports (circles) at normal zoom levels
- **SC-004**: Ungrouping a GraphNode restores original single-segment connections within 500ms
- **SC-005**: Navigation between view contexts shows only relevant segments with no visual artifacts or leftover segments from other contexts
- **SC-006**: Type validation prevents creation of PassThruPorts with mismatched data types, with clear error messaging to the user
- **SC-007**: Nested GraphNodes with connections spanning 3+ levels correctly display segment chains with PassThruPorts at each boundary

## Assumptions

- PassThruPorts are not directly editable by users; they are automatically created and removed based on grouping operations
- The bezier curve rendering for ConnectionSegments will use the existing connection rendering infrastructure with minor modifications
- Port type compatibility follows the existing rules defined in the Port class
- ConnectionSegments share the same visual styling (color, line style) as their parent Connection
- The dataType of a PassThruPort is derived from the connected ports and cannot be independently specified
