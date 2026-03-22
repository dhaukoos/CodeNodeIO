# Feature Specification: Table Header Row for Entity List Views

**Feature Branch**: `056-table-header-row`
**Created**: 2026-03-22
**Status**: Draft
**Input**: User description: "Column headers for table view generated — When creating a Repository Module from an IP Type, the code generator creates a UI listview of the items with columns for each property. As part of the {entity}s.kt file, create a HeaderRow composable that is always displayed at the top of the entity list, with the property names as the column entries. The {entity}Row.kt file will not have those labels preceding the values. Make this change for code generation going forward and refactor existing Repository Modules (Addresses, GeoLocations, UserProfiles)."

## User Scenarios & Testing

### User Story 1 - Header Row in Entity List Views (Priority: P1)

As a user viewing an entity list (e.g., Addresses, UserProfiles, GeoLocations), I see a fixed header row at the top of the list displaying the column names (property names) for the data shown below. Each data row displays only the values — without repeating the property labels inline — so the list reads like a proper table.

**Why this priority**: This is the core visual change. Without the header row, users lose context for what each value represents. This story delivers the table-like layout that makes entity lists scannable and professional.

**Independent Test**: Open any entity module's list view. Verify a header row with column names appears at the top. Verify data rows show values only (no "Street:", "City:", etc. labels inline). Verify the header stays visible while scrolling through many items.

**Acceptance Scenarios**:

1. **Given** the Addresses module is loaded with address entries, **When** the user views the entity list, **Then** a header row displays "Street", "City", "State", "Zip" as column labels above the data rows.
2. **Given** the UserProfiles module is loaded with profile entries, **When** the user views the entity list, **Then** a header row displays column names matching the entity's properties.
3. **Given** the GeoLocations module is loaded with location entries, **When** the user views the entity list, **Then** a header row displays column names matching the entity's properties.
4. **Given** any entity list with data rows, **When** the user reads a data row, **Then** each cell shows only the value (e.g., "123 Main St") without a preceding label (not "Street: 123 Main St").
5. **Given** any entity list, **When** the header row and data rows are displayed, **Then** the column widths in the header align with the corresponding value columns in each data row.

---

### User Story 2 - Code Generator Produces Header Rows for New Modules (Priority: P2)

As a developer generating a new Repository Module from an IP Type, the code generator automatically produces the {entity}s.kt file with a HeaderRow composable and produces the {entity}Row.kt file with value-only cells (no inline labels). No manual editing is required to achieve the table-style layout.

**Why this priority**: Ensures all future modules follow the new pattern automatically. Without this, developers would need to manually refactor every newly generated module.

**Independent Test**: Generate a new Repository Module from an IP Type with 3+ properties. Open the generated {entity}s.kt file and confirm it contains a HeaderRow composable with the correct property names. Open the generated {entity}Row.kt file and confirm rows display values without inline labels.

**Acceptance Scenarios**:

1. **Given** an IP Type with properties "Name", "Email", "Phone", **When** the developer generates a new Repository Module, **Then** the generated {entity}s.kt file contains a HeaderRow composable listing "Name", "Email", "Phone" as column headers.
2. **Given** an IP Type with properties, **When** the developer generates a new Repository Module, **Then** the generated {entity}Row.kt file displays each property value without a preceding label.
3. **Given** a newly generated module, **When** the module is compiled and launched, **Then** the entity list displays correctly with header row and value-only data rows — no manual edits needed.

---

### User Story 3 - Existing Modules Refactored to New Pattern (Priority: P2)

The three existing Repository Modules — Addresses, GeoLocations, and UserProfiles — are updated to follow the same header row pattern, removing inline labels from data rows and adding a HeaderRow composable to the list container.

**Why this priority**: Equal to US2 because users interact with existing modules immediately. The visual inconsistency between old and new modules would be confusing.

**Independent Test**: Launch the graphEditor, run each existing module's runtime preview, and verify the entity list displays a header row with column names and data rows without inline labels.

**Acceptance Scenarios**:

1. **Given** the Addresses module, **When** the entity list is displayed, **Then** a header row shows "Street", "City", "State", "Zip" and each AddressRow shows only the values.
2. **Given** the GeoLocations module, **When** the entity list is displayed, **Then** a header row shows the property names and each GeoLocationRow shows only the values.
3. **Given** the UserProfiles module, **When** the entity list is displayed, **Then** a header row shows the property names and each UserProfileRow shows only the values.

---

### Edge Cases

- What happens when a property value is null or empty? Display an empty cell or a placeholder (e.g., "—").
- What happens when a property value is very long? Truncate with ellipsis to maintain column alignment.
- What happens when there are zero entities in the list? The header row still displays above the empty state message.
- What happens with a single-property entity? The header row displays one column name; the layout remains consistent.

## Requirements

### Functional Requirements

- **FR-001**: Each entity list view MUST display a header row above the data rows containing the property names as column labels.
- **FR-002**: The header row MUST remain visible at the top of the list at all times (it is not part of the scrollable content).
- **FR-003**: Data rows MUST display property values only, without preceding labels (e.g., show "123 Main St" not "Street: 123 Main St").
- **FR-004**: Column widths in the header row MUST align with the corresponding columns in the data rows.
- **FR-005**: The header row MUST be visually distinct from data rows (e.g., bold text, different background, or separator line).
- **FR-006**: The code generator MUST produce a HeaderRow composable in the generated {entity}s.kt file, using the IP Type's property names as column labels.
- **FR-007**: The code generator MUST produce {entity}Row.kt files that display values without inline labels.
- **FR-008**: Null or empty property values MUST display a placeholder (e.g., "—") to maintain column structure.
- **FR-009**: The existing Addresses, GeoLocations, and UserProfiles modules MUST be refactored to follow the header row pattern.

### Key Entities

- **HeaderRow**: A non-scrollable row displayed at the top of each entity list, containing one text cell per entity property. Derived from the entity's property names.
- **EntityRow**: A data row in the entity list. Each cell displays a single property value without a label. Column layout matches the HeaderRow.

## Success Criteria

### Measurable Outcomes

- **SC-001**: All three existing entity modules (Addresses, GeoLocations, UserProfiles) display a header row with correct column names when viewed in the runtime preview.
- **SC-002**: All three existing entity modules display data rows with values only (no inline labels) when viewed in the runtime preview.
- **SC-003**: A newly generated Repository Module from any IP Type with 2+ properties produces files that compile and display header row + value-only rows without manual editing.
- **SC-004**: Column alignment between header and data rows is visually consistent — headers sit directly above their corresponding values.
- **SC-005**: The header row remains visible when the user scrolls through a list of 20+ entities.

## Assumptions

- Property names from the IP Type definition are suitable as column header display text (e.g., "Street", "City" rather than internal identifiers).
- The existing column layout approach (weight-based or arrangement-based) is sufficient for alignment between header and data rows.
- The header row uses the same horizontal padding and spacing as data rows to ensure alignment.
- The empty state message continues to appear below the header row when no entities exist.
