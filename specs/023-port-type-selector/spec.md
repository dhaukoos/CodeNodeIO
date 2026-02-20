# Feature Specification: Port Type Selector

**Feature Branch**: `023-port-type-selector`
**Created**: 2026-02-20
**Status**: Draft
**Input**: User description: "graphEditor Port Type UI input - On the Properties panel for Generic Nodes on the graphEditor, where the Ports are listed, to the right of each name Textfield input, add a dropdown selection from the list of IP Types. This selection will specify IP type for the port. Changes to the port type should propagate to any already attached connections and to their port on the other end. Subsequent changes to the connection type will also propagate to any attached ports. In general, the last change to a port or connection type will propagate to any attached elements, where the last chosen type is selected."

## User Scenarios & Testing

### User Story 1 - Select IP Type for a Port (Priority: P1)

As a graph designer, I want to select a data type for each port on a Generic Node from a dropdown of available IP Types, so that the port's data type is explicitly defined rather than defaulting to "Any".

When I select a Generic Node and open its Properties panel, I see each port listed with its name text field. To the right of each port name, a dropdown displays the currently assigned IP type (defaulting to "Any"). I can open the dropdown to see all available IP types and select one. The port's data type updates immediately to reflect my choice.

**Why this priority**: This is the core interaction - without the dropdown, no type selection can happen. It delivers standalone value by letting users assign concrete types to ports, which is a prerequisite for type-safe code generation.

**Independent Test**: Can be tested by selecting a Generic Node, opening the Properties panel, and verifying the IP type dropdown appears next to each port name and that selecting a type updates the port's data type.

**Acceptance Scenarios**:

1. **Given** a Generic Node is selected and the Properties panel is visible, **When** I look at the port list, **Then** each port row shows a name text field and an IP type dropdown to its right.
2. **Given** a port with no previously assigned type, **When** I view the dropdown, **Then** it displays "Any" as the default selection.
3. **Given** the IP type dropdown is open, **When** I select "Int" from the list, **Then** the port's data type updates to Int and the dropdown displays "Int".
4. **Given** a port with a previously assigned type of "String", **When** I reopen the Properties panel, **Then** the dropdown shows "String" as the current selection (the type persists).

---

### User Story 2 - Port Type Change Propagates to Connections and Remote Ports (Priority: P2)

As a graph designer, I want my port type changes to automatically propagate to any connections attached to that port and to the port on the other end of each connection, so that the entire data path stays consistent without manual updates.

When I change a port's IP type, every connection attached to that port updates its type to match. Additionally, the port at the other end of each affected connection also updates to the same type. This ensures the full data path (source port -> connection -> target port) shares a consistent type.

**Why this priority**: Without propagation, users would need to manually update every connection and remote port, which is error-prone and tedious. This story makes the type selector practical for real graph editing workflows.

**Independent Test**: Can be tested by creating two connected nodes, changing the type on one port, and verifying the connection and the remote port both reflect the new type.

**Acceptance Scenarios**:

1. **Given** a port connected to another node via a connection, **When** I change the port's IP type from "Any" to "Int", **Then** the connection's type updates to "Int" and the port on the other end of the connection also updates to "Int".
2. **Given** a port connected to multiple nodes via separate connections, **When** I change the port's IP type, **Then** all attached connections and all their remote ports update to the new type.
3. **Given** a port with no connections, **When** I change the port's IP type, **Then** only the port itself updates (no errors occur).

---

### User Story 3 - Connection Type Change Propagates to Attached Ports (Priority: P3)

As a graph designer, I want connection type changes to propagate to the ports on both ends, so that the "last change wins" rule applies bidirectionally - whether I change a type on a port or on a connection, the entire data path updates consistently.

When a connection's IP type changes (through any mechanism), both the source port and the target port update to match the connection's new type. Combined with US2, this creates a "last change wins" propagation model: the most recent type assignment on any element in a data path (port or connection) propagates to all other elements in that path.

**Why this priority**: This completes the bidirectional propagation model. While US2 handles the most common case (changing a port updates its connections), this story ensures consistency when a connection type changes independently.

**Independent Test**: Can be tested by changing a connection's IP type and verifying both attached ports update to match.

**Acceptance Scenarios**:

1. **Given** a connection between two ports, **When** the connection's IP type changes to "String", **Then** the source port and the target port both update their data type to "String".
2. **Given** a chain of propagation (port A changed, propagates to connection, propagates to port B), **When** the user then changes port B's type to "Double", **Then** the connection and port A update to "Double" (last change wins).

---

### Edge Cases

- What happens when a port type change would propagate to a port on a node that is not a Generic Node (e.g., a node with fixed port types)? Propagation should still update the port's data type regardless of node type, since all ports have a data type field.
- What happens when propagation creates a cycle (port A -> connection -> port B, and port B has another connection back to port A)? The propagation should apply the type change to all directly connected elements in a single pass without re-triggering propagation recursively.
- What happens when the user changes a port name and type simultaneously? Each change should be handled independently; the type change triggers propagation regardless of other property edits.
- What happens when a connection is deleted after a type was propagated? The ports retain their last-assigned types; deleting a connection does not reset port types.

## Requirements

### Functional Requirements

- **FR-001**: The Properties panel for Generic Nodes MUST display an IP type dropdown selector to the right of each port's name text field, for both input and output ports.
- **FR-002**: The IP type dropdown MUST list all available IP types from the system's type registry (currently: Any, Int, Double, Boolean, String).
- **FR-003**: The IP type dropdown MUST show the port's current data type as the selected value, defaulting to "Any" for ports without an explicit type assignment.
- **FR-004**: When the user selects a new IP type from the dropdown, the port's data type MUST update immediately.
- **FR-005**: When a port's data type changes, the system MUST update the IP type on every connection attached to that port.
- **FR-006**: When a port's data type changes, the system MUST update the data type on the port at the other end of every attached connection.
- **FR-007**: When a connection's IP type changes, the system MUST update the data type on both the source port and the target port of that connection.
- **FR-008**: Type propagation MUST be non-recursive: a single type change propagates once to all directly connected elements without triggering further cascading propagation.
- **FR-009**: The port type selection MUST persist when the graph is saved and reloaded.
- **FR-010**: The IP type dropdown MUST display the type name and use the type's associated color as a visual indicator.

### Key Entities

- **Port**: A typed entry/exit point on a node. Has a name, direction (input/output), and a data type. The data type is selected from the IP type registry.
- **Connection**: A link between an output port on one node and an input port on another. Carries an IP type identifier that reflects the data flowing through it.
- **IP Type (InformationPacketType)**: A registered data type with a unique identifier, display name, color, and underlying type reference. The system provides default types (Any, Int, Double, Boolean, String) and supports custom types.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Users can assign an IP type to any port on a Generic Node in under 3 seconds (open dropdown, select type).
- **SC-002**: 100% of type changes on a port propagate correctly to all attached connections and remote ports in a single action.
- **SC-003**: 100% of type changes on a connection propagate correctly to both attached ports in a single action.
- **SC-004**: Port type selections persist across save/load cycles with zero data loss.
- **SC-005**: The IP type dropdown is visually consistent with existing property editors in the Properties panel (follows the same layout patterns and styling).

## Assumptions

- The IP type registry is already populated with default types (Any, Int, Double, Boolean, String) and is accessible from the Properties panel.
- Connections already have an IP type identifier field that can be set programmatically.
- The Properties panel already supports dropdown editors for other property types, providing an established pattern to follow.
- Port data types are stored on the Port model and can be updated without affecting other port properties (name, direction, etc.).
- Custom IP types registered by the user will automatically appear in the dropdown alongside the defaults.
- All node types (not just Generic Nodes) have ports with data types, but the dropdown selector is only added to the Properties panel for Generic Nodes as specified.
