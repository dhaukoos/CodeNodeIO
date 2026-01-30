# Feature Specification: InformationPacket Palette Support

**Feature Branch**: `003-ip-palette-support`
**Created**: 2026-01-30
**Status**: Draft
**Input**: User description: "Add InformationPacket support to graphEditor with IP Palette, connection selection, and type management"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View and Browse InformationPacket Types (Priority: P1)

A developer opens the graphEditor and wants to see what InformationPacket types are available for use in their flow graph. They access the IP Palette panel which displays a scrollable list of available IP types. Each type is shown with its name and a small colored indicator square. The developer can use the search field to filter the list and find specific types quickly.

**Why this priority**: This is the foundational capability that enables all other IP-related features. Without being able to view available IP types, users cannot make informed decisions about connection typing.

**Independent Test**: Can be fully tested by opening the graphEditor, locating the IP Palette, verifying all 5 default types (Any, Int, Double, Boolean, String) are visible with their color indicators, and using the search field to filter results.

**Acceptance Scenarios**:

1. **Given** the graphEditor is open, **When** the user views the IP Palette, **Then** they see a scrollable list of all defined InformationPacket types with their names and color indicators
2. **Given** the IP Palette is visible, **When** the user types in the search field, **Then** the list filters to show only types whose names contain the search text
3. **Given** the IP Palette contains multiple types, **When** the user selects a type from the list, **Then** the Textual view displays the defining code for that IP type

---

### User Story 2 - Select and Inspect Connections (Priority: P2)

A developer working in the Visual view of the graphEditor wants to inspect a connection between two nodes. They click on a connection line to select it. The connection becomes visually highlighted, and the Properties Panel updates to show the connection's IP type properties, including the type name and its associated color displayed as a colored rectangle next to an editable RGB value.

**Why this priority**: Connection selection is essential for users to understand and modify the data types flowing through their graph. This builds on the IP type awareness from Story 1.

**Independent Test**: Can be fully tested by creating a graph with at least one connection, clicking on the connection in the Visual view, verifying it becomes selected, and confirming the Properties Panel shows the connection's IP type with a color swatch and RGB value.

**Acceptance Scenarios**:

1. **Given** a graph with connections exists in the Visual view, **When** the user clicks on a connection line, **Then** the connection becomes visually selected (highlighted)
2. **Given** a connection is selected, **When** the Properties Panel updates, **Then** it displays the connection's current IP type name, the associated color as a small rounded rectangle, and an editable RGB text field
3. **Given** a connection is selected, **When** the user clicks elsewhere on the canvas, **Then** the connection is deselected
4. **Given** a connection is selected, **When** the user edits the RGB value in the Properties Panel, **Then** the color indicator updates to reflect the new color

---

### User Story 3 - Change Connection IP Type via Context Menu (Priority: P3)

A developer wants to change the IP type of an existing connection. They right-click on a connection in the Visual view, which opens a context menu displaying a dropdown list of all available IP types. Selecting a type from the menu updates the connection to use that IP type, and the connection's visual appearance updates to reflect the new type's color.

**Why this priority**: Modifying connection types is the primary way users will configure data flow in their graphs. This depends on both viewing available types (Story 1) and selecting connections (Story 2).

**Independent Test**: Can be fully tested by right-clicking on a connection, verifying the context menu appears with all available IP types listed, selecting a different type, and confirming the connection updates to the new type.

**Acceptance Scenarios**:

1. **Given** a connection exists in the Visual view, **When** the user right-clicks on the connection, **Then** a context menu appears with a dropdown list of all defined IP types
2. **Given** the context menu is open, **When** the user selects an IP type from the list, **Then** the connection's IP type is updated to the selected type
3. **Given** a connection's IP type has been changed, **When** viewing the connection, **Then** it displays visual indicators reflecting the new type's color
4. **Given** the context menu is open, **When** the user clicks outside the menu, **Then** the menu closes without making changes

---

### Edge Cases

- What happens when searching with no matching results? The IP Palette displays an empty state with a "No matching types" message.
- How does the system handle invalid RGB values? Invalid RGB input (non-numeric, out of range 0-255) is rejected with inline validation feedback, and the previous valid value is preserved.
- What happens when right-clicking on empty canvas area? No context menu appears; the right-click is ignored for connection purposes.
- How does the system handle clicking near but not on a connection? Connections have a reasonable click tolerance (a few pixels) to make selection easier.

## Requirements *(mandatory)*

### Functional Requirements

**InformationPacket Definition**

- **FR-001**: System MUST support an optional color property for InformationPacket definitions
- **FR-002**: System MUST use black as the default color for InformationPackets
- **FR-003**: System MUST associate the default "Any" payload type with the black color
- **FR-004**: System MUST provide default InformationPacket definitions for: Any, Int, Double, Boolean, String

**IP Palette Component**

- **FR-005**: System MUST display an IP Palette panel in the graphEditor containing all defined InformationPacket types
- **FR-006**: System MUST display each IP type in the palette with its type name and a small rounded square showing its associated color
- **FR-007**: System MUST provide a search field in the IP Palette to filter types by name
- **FR-008**: System MUST make the IP Palette list scrollable when types exceed visible area
- **FR-009**: System MUST display the selected IP type's defining code in the Textual view when a type is selected in the palette

**Connection Selection**

- **FR-010**: System MUST allow users to select connections by clicking on them in the Visual view
- **FR-011**: System MUST provide visual feedback when a connection is selected (highlighting)
- **FR-012**: System MUST display selected connection's IP type properties in the Properties Panel
- **FR-013**: System MUST display the IP type's color as a small rounded rectangle in the Properties Panel
- **FR-014**: System MUST provide an editable RGB text field next to the color indicator
- **FR-015**: System MUST validate RGB input values (0-255 range for each component)

**Connection Type Context Menu**

- **FR-016**: System MUST display a context menu when right-clicking on a connection
- **FR-017**: System MUST show a dropdown list of all defined IP types in the context menu
- **FR-018**: System MUST update the connection's IP type when a type is selected from the context menu
- **FR-019**: System MUST close the context menu when clicking outside of it or pressing Escape

### Key Entities

- **InformationPacket**: Represents a typed data container with a payload type and optional color. Key attributes: typeName (identifier), payloadType (the data class type), color (RGB value, defaults to black)
- **Connection**: Represents a link between node ports in a flow graph. Has an associated IP type that defines what data flows through it. Key attributes: sourceNodeId, sourcePortId, targetNodeId, targetPortId, ipType
- **IPPalette**: UI component displaying available InformationPacket types with search/filter capability

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can view all available IP types within 1 second of opening the graphEditor
- **SC-002**: Users can find a specific IP type using search in under 3 seconds
- **SC-003**: Users can select a connection and view its properties with a single click
- **SC-004**: Users can change a connection's IP type in under 5 seconds using the context menu
- **SC-005**: 100% of default IP types (Any, Int, Double, Boolean, String) are visible in the palette on first load
- **SC-006**: Color indicators accurately display the RGB value associated with each IP type

## Assumptions

- The graphEditor already supports a Textual view that can display code definitions
- The Properties Panel infrastructure exists and can be extended for connection properties
- The Visual view supports mouse interaction events (click, right-click) on connections
- RGB color format (three values 0-255) is sufficient for color representation
- The five default Kotlin types (Any, Int, Double, Boolean, String) provide adequate initial coverage for most use cases
