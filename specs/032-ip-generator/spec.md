# Feature Specification: IP Generator Interface

**Feature Branch**: `032-ip-generator`
**Created**: 2026-02-27
**Status**: Draft
**Input**: User description: "Create an IP Generator interface. In the graphEditor, create an IP Generator right above the IP Types panel (analogous to the Node Generator above the Node Palette). Its UI has a TextField input for its name, and below that a '+' button for adding properties to the data type. Each property is reflected as a row in the UI similar to the current properties in the Properties panel, with a TextField for a name and a dropDown selector for the type (from the existing list of types). One additional UI element in the row is a '-' button to allow removal. At the bottom of the UI pane are Cancel and Create buttons (like the Node Generator). Clicking Create generates a new IP Type that gets added to the list; while clicking Cancel returns the UI to its original state."

## User Scenarios & Testing

### User Story 1 - Create a Custom IP Type (Priority: P1)

A graph designer wants to define a new Information Packet type to represent a domain-specific data structure in their flow graph. They open the IP Generator panel, enter a name (e.g., "UserProfile"), add properties like "name" (String, required), "age" (Int, optional), and "active" (Boolean, required), and click Create. The new type immediately appears in the IP Types palette and is available for assignment to connections. Required properties are non-nullable with no default; optional properties are nullable with a default of null.

**Why this priority**: This is the core capability of the feature. Without the ability to create custom IP types, no other functionality is possible.

**Independent Test**: Can be fully tested by opening the IP Generator, entering a name and at least one property, clicking Create, and verifying the new type appears in the IP Types palette list.

**Acceptance Scenarios**:

1. **Given** the IP Generator panel is visible, **When** the user enters a valid name, adds at least one property with a name and type, and clicks Create, **Then** a new IP Type appears in the IP Types palette with the entered name.
2. **Given** the IP Generator panel is visible, **When** the user enters a name but adds no properties and clicks Create, **Then** the type is created as a simple marker type with no properties.
3. **Given** the user has entered a name and added properties, **When** the user clicks Cancel, **Then** all entered data is cleared and the panel returns to its initial empty state.

---

### User Story 2 - Manage Properties During Creation (Priority: P2)

A graph designer is building a complex IP type and needs to add, modify, and remove properties before finalizing. They use the "+" button to add property rows, edit names and types in each row, and use the "-" button to remove properties they no longer need.

**Why this priority**: Property management is essential for creating useful composite types, but the core create flow (US1) must work first.

**Independent Test**: Can be tested by adding multiple property rows, editing their names and types, removing some, and verifying the final list matches expectations before clicking Create.

**Acceptance Scenarios**:

1. **Given** the IP Generator panel is visible, **When** the user clicks the "+" button, **Then** a new property row appears with a name text field, a type dropdown selector, and a required toggle defaulting to checked (required).
2. **Given** a property row exists, **When** the user clicks the "-" button on that row, **Then** the property row is removed from the list.
3. **Given** multiple property rows exist, **When** the user changes a property name, type, or required toggle, **Then** the change is reflected immediately in the row.
4. **Given** zero property rows exist, **When** the user looks at the "+" button, **Then** the "+" button remains available to add the first property.
5. **Given** a property row has required unchecked, **When** the type is created, **Then** that property is generated as nullable and optional with a default value of null (e.g., `val age: Int? = null`).

---

### User Story 3 - Validation Feedback (Priority: P3)

A graph designer needs clear feedback when their IP type definition is invalid. The system prevents creation of types with empty names or duplicate names, and visually indicates validation state.

**Why this priority**: Validation prevents user errors and data corruption, but basic creation must work first.

**Independent Test**: Can be tested by attempting to create a type with an empty name, a duplicate name, or a property with an empty name, and verifying appropriate feedback is displayed.

**Acceptance Scenarios**:

1. **Given** the IP Generator panel is visible, **When** the name field is empty, **Then** the Create button is disabled.
2. **Given** the IP Generator panel is visible, **When** the user enters a name that already exists in the IP Types registry, **Then** the Create button is disabled and a visual indicator shows the name is taken.
3. **Given** properties have been added, **When** any property has an empty name, **Then** the Create button is disabled.
4. **Given** properties have been added, **When** two properties share the same name, **Then** the Create button is disabled and a visual indicator shows the duplicate.

---

### Edge Cases

- What happens when the user creates a type with the same name as a built-in type (e.g., "String")? The system prevents this by treating built-in and existing type names as reserved.
- What happens when the user adds a property and selects a custom IP type (previously created) as its type? This is allowed, enabling composition of custom types.
- What happens when the panel is collapsed while partially filled? The entered data is preserved until Cancel is explicitly clicked.
- What happens when the user creates a type with only a name and no properties? This is allowed, resulting in a simple marker/tag type.

## Requirements

### Functional Requirements

- **FR-001**: The system MUST display an IP Generator panel positioned directly above the IP Types palette, following the same layout pattern as the Node Generator above the Node Palette.
- **FR-002**: The IP Generator panel MUST be collapsible/expandable via a header toggle, consistent with the Node Generator pattern.
- **FR-003**: The IP Generator MUST provide a text field for entering the new IP type's name.
- **FR-004**: The IP Generator MUST provide a "+" button that adds a new property row to the type definition.
- **FR-005**: Each property row MUST contain a text field for the property name, a dropdown selector for the property type, a boolean toggle for required/optional, and a "-" button to remove the row.
- **FR-005a**: The required toggle MUST default to checked (required) when a new property row is added.
- **FR-005b**: When the required toggle is unchecked (optional), the generated property MUST be nullable with a default value of null.
- **FR-006**: The property type dropdown MUST list all currently registered IP types (both built-in and user-created).
- **FR-007**: The IP Generator MUST provide a Cancel button that clears all entered data and returns the panel to its initial state.
- **FR-008**: The IP Generator MUST provide a Create button that generates a new IP Type from the entered name and properties.
- **FR-009**: Upon clicking Create, the new IP Type MUST be immediately added to the IP Types registry and appear in the IP Types palette.
- **FR-010**: The Create button MUST be disabled when the name field is empty.
- **FR-011**: The Create button MUST be disabled when the entered name matches an existing IP type name (case-insensitive).
- **FR-012**: The Create button MUST be disabled when any property row has an empty name.
- **FR-013**: The Create button MUST be disabled when two or more properties share the same name.

### Key Entities

- **Custom IP Type**: A user-defined Information Packet type with a name and zero or more named, typed properties. Represents a composite data structure for use in flow graph connections.
- **IP Property**: A named field within a custom IP type, consisting of a property name, a reference to an existing IP type that defines the field's data type, and a required/optional flag. Required properties are non-nullable; optional properties are nullable with a default of null.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Users can create a new custom IP type with a name and properties in under 30 seconds.
- **SC-002**: Newly created IP types appear in the IP Types palette within 1 second of clicking Create.
- **SC-003**: 100% of validation errors (empty name, duplicate name, empty property name, duplicate property name) are caught before the Create button can be clicked.
- **SC-004**: Users can add and remove properties without any data loss in other fields during the same creation session.

## Assumptions

- The IP Generator follows the same visual design language as the existing Node Generator (collapsible panel, Cancel/Create buttons, consistent spacing and typography).
- A color is automatically assigned to newly created IP types (e.g., a default or randomly generated color), since the user description does not specify color selection.
- Custom IP types created via the IP Generator are persisted to JSON using kotlinx.serialization, following the existing custom node persistence pattern. Types are loaded on startup and saved after each creation.
- The property type dropdown uses the IP type's display name for selection.
- Creating an IP type with zero properties is valid (results in a simple marker/tag type).
