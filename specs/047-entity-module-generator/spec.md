# Feature Specification: Generalize Entity Repository Module Creation

**Feature Branch**: `047-entity-module-generator`
**Created**: 2026-03-09
**Status**: Draft
**Input**: User description: "Generalize creation of EntityRepository modules — create code-generation templates for all node types and UI files following the UserProfiles module pattern, and replace the 'Create Repository Node' button with 'Create Repository Module' to generate a complete working module for any entity type."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generate Complete Entity Module from IP Type (Priority: P1)

A developer creates a new IP Type (e.g., GeoLocation) using the IP Generator panel, defining its properties (latitude, longitude, label, etc.). After creation, they click "Create Repository Module" in the IP Type Properties panel. The system generates a fully functional module — including all three nodes ({Entity}CUD, {Entity}Repository, {Entity}sDisplay), the FlowGraph connecting them, UI composables for listing/adding/updating entities, the ViewModel with CRUD methods, and all processing logic stubs — following the same structure as the existing UserProfiles module.

**Why this priority**: This is the core value of the feature — enabling one-click generation of a complete, working entity module. Without this, developers must manually create each file and wire them together.

**Independent Test**: Can be fully tested by creating a GeoLocation IP Type, clicking "Create Repository Module", and verifying that the generated module directory contains all expected files with correct entity-specific naming and property mappings.

**Acceptance Scenarios**:

1. **Given** a custom IP Type "GeoLocation" with properties (latitude: Double, longitude: Double, label: String), **When** the developer clicks "Create Repository Module", **Then** the system generates a complete module directory named "GeoLocations" containing all required source files.
2. **Given** the generated GeoLocations module, **When** the developer inspects the files, **Then** each file uses "GeoLocation" as the entity name with correct property mappings throughout (entity fields, form fields, display columns).
3. **Given** the generated GeoLocations module, **When** the developer inspects the FlowGraph definition, **Then** it connects GeoLocationCUD (source) → GeoLocationRepository (processor) → GeoLocationsDisplay (sink) with properly typed ports.

---

### User Story 2 - Generalized Code-Generation Templates for Nodes (Priority: P2)

The code-generation system produces templates for all three node types in an entity module: the {Entity}CUD source node (emitting create/update/delete operations), the {Entity}Repository processor node (already exists and is generalized), and the {Entity}sDisplay sink node (receiving and displaying entity lists). Each template is parameterized by entity name and properties.

**Why this priority**: The node templates are the backbone of the generated module. The Repository node template already exists; the CUD and Display templates complete the set.

**Independent Test**: Can be tested by invoking each code generator with a test entity name and verifying the output matches the expected pattern (comparing structure against the UserProfiles equivalents).

**Acceptance Scenarios**:

1. **Given** an entity name "GeoLocation", **When** the CUD node template is generated, **Then** it produces a source node definition with three reactive source outputs (save, update, remove) matching the UserProfileCUD pattern.
2. **Given** an entity name "GeoLocation", **When** the Display node template is generated, **Then** it produces a sink node definition with two inputs (entities, error) and zero outputs, matching the UserProfilesDisplay pattern.
3. **Given** an entity name "GeoLocation", **When** the FlowGraph template is generated, **Then** it produces a valid .flow.kt file connecting all three nodes with correctly typed connections.

---

### User Story 3 - Generalized UI Composable Templates (Priority: P3)

The code-generation system produces three UI composable files for each entity module: {Entity}s.kt (main list view with add/update/remove buttons), AddUpdate{Entity}.kt (form dialog for creating or editing an entity), and {Entity}Row.kt (single row display in the list). Each template maps entity properties to form fields and display columns automatically.

**Why this priority**: The UI templates make the generated module immediately usable without manual UI coding. They depend on the node templates (P2) being in place.

**Independent Test**: Can be tested by generating UI files for GeoLocation and verifying each composable renders the correct form fields (latitude, longitude, label) and list columns.

**Acceptance Scenarios**:

1. **Given** a GeoLocation entity with properties (latitude: Double, longitude: Double, label: String, isActive: Boolean), **When** the AddUpdateGeoLocation.kt template is generated, **Then** it contains input fields for each property with appropriate input types (numeric for Double, text for String, checkbox for Boolean).
2. **Given** the generated GeoLocations.kt composable, **When** rendered, **Then** it displays a scrollable list of entities with add, update, and remove buttons matching the UserProfiles layout.
3. **Given** the generated GeoLocationRow.kt composable, **When** rendered for a single entity, **Then** it displays all entity properties in a single row with appropriate formatting.

---

### User Story 4 - ViewModel with CRUD Methods (Priority: P4)

The generated ViewModel includes entity-specific CRUD methods — add{Entity}(item), update{Entity}(item), remove{Entity}(item) — that trigger the corresponding reactive source flows in the FlowGraph. The ViewModel also exposes a reactive list of entities and execution state, following the UserProfilesViewModel pattern.

**Why this priority**: The ViewModel connects the UI to the FlowGraph. It depends on the node templates (P2) and UI templates (P3) being correct.

**Independent Test**: Can be tested by verifying the generated ViewModel source contains the correct method signatures and state flow declarations for the given entity type.

**Acceptance Scenarios**:

1. **Given** a generated GeoLocationsViewModel, **When** inspected, **Then** it contains methods addGeoLocation(item), updateGeoLocation(item), removeGeoLocation(item) that write to the corresponding state flows.
2. **Given** a generated GeoLocationsViewModel, **When** inspected, **Then** it contains a reactive entities list flow populated from the repository via the injected DAO.

---

### User Story 5 - Button Replacement in IP Type Properties Panel (Priority: P5)

The existing "Create Repository Node" button in the IP Type Properties panel is replaced with a "Create Repository Module" button. When pressed, it triggers the full module generation pipeline (nodes, FlowGraph, UI, ViewModel, processing logic) instead of creating only a single repository node.

**Why this priority**: This is a UI change that depends on all generation templates being ready.

**Independent Test**: Can be tested by opening the IP Type Properties panel for a custom IP type and verifying the button label is "Create Repository Module" and that pressing it triggers module generation.

**Acceptance Scenarios**:

1. **Given** the IP Type Properties panel showing a custom IP type with properties, **When** the developer views the panel, **Then** a button labeled "Create Repository Module" is displayed (not "Create Repository Node").
2. **Given** a custom IP type without an existing module, **When** the developer clicks "Create Repository Module", **Then** the system creates all three node definitions and generates the complete module directory.
3. **Given** a custom IP type that already has a generated module, **When** the developer views the panel, **Then** the button is disabled and shows "Module exists".

---

### Edge Cases

- What happens when the entity name is a single character (e.g., "X")? The system should still generate valid file and class names.
- What happens when entity properties include reserved words? The system should handle or reject them gracefully.
- What happens when the entity has no properties (only the auto-generated id)? The system should generate valid but minimal UI forms and display rows.
- What happens when the developer clicks "Create Repository Module" while another generation is in progress? The button should be disabled during generation.
- What happens when the generated module directory already exists on disk? The system should warn before overwriting.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST generate a {Entity}CUD source node definition with three reactive source outputs (save, update, remove) parameterized by the entity type.
- **FR-002**: System MUST generate a {Entity}sDisplay sink node definition with two inputs (entities, error) and zero outputs, parameterized by the entity type.
- **FR-003**: System MUST generate a {Entity}s.flow.kt FlowGraph file connecting {Entity}CUD → {Entity}Repository → {Entity}sDisplay with correctly typed ports.
- **FR-004**: System MUST generate an AddUpdate{Entity}.kt composable with form fields mapped to entity properties, using appropriate input types (text for String, numeric for numeric types, checkbox for Boolean).
- **FR-005**: System MUST generate an {Entity}Row.kt composable displaying all entity properties in a single list row.
- **FR-006**: System MUST generate an {Entity}s.kt composable with a scrollable entity list and add/update/remove action buttons.
- **FR-007**: System MUST generate a {Entity}sViewModel with add{Entity}(item), update{Entity}(item), and remove{Entity}(item) methods wired to the corresponding state flows.
- **FR-008**: System MUST generate a {Entity}sPersistence.kt file providing dependency injection wiring for the entity's DAO.
- **FR-009**: System MUST generate all processing logic stubs for the CUD and Display nodes.
- **FR-010**: The "Create Repository Node" button MUST be replaced with "Create Repository Module" in the IP Type Properties panel.
- **FR-011**: The "Create Repository Module" button MUST be disabled and show "Module exists" when a module has already been generated for the IP type.
- **FR-012**: System MUST add the entity's persistence files (Entity, DAO, Repository) to the shared persistence module, following the established architecture where all persistence components reside centrally.
- **FR-013**: Generated module MUST follow the same directory structure and file organization as the UserProfiles module.

### Key Entities

- **Entity Template**: A parameterized definition of an entity type, derived from an IP Type's properties. Contains entity name, property list (name, type, required flag), and generated naming conventions (plural form, camelCase/PascalCase variants).
- **Generated Module**: The complete output directory containing all source files for a single entity CRUD module — nodes, FlowGraph, ViewModel, UI composables, processing logic, and dependency injection wiring.
- **Node Definitions**: The three custom node definitions (CUD source, Repository processor, Display sink) registered in the custom node repository for use in the graph editor.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A developer can generate a complete, compilable entity module from a custom IP type in a single button click, taking no more than 5 seconds.
- **SC-002**: 100% of generated files follow the naming conventions and directory structure of the UserProfiles reference module.
- **SC-003**: The generated module compiles without errors when added to the project.
- **SC-004**: The generated UI is immediately functional — the developer can add, update, and remove entities without writing any additional code.
- **SC-005**: Validation with the GeoLocation IP type produces a working GeoLocations module that passes manual testing of all CRUD operations.

## Assumptions

- The UserProfiles module serves as the canonical template for generated modules. All generated code follows its patterns and conventions.
- Entity names provided via IP Types are valid identifiers (PascalCase, no special characters). The IP Generator already enforces this.
- The plural form of entity names is formed by appending "s" (e.g., GeoLocation → GeoLocations). Irregular plurals are not handled.
- The persistence module architecture from feature 046 is used: all persistence components (Entity, DAO, AppDatabase, Repository) reside in the shared persistence module, and feature modules access DAOs via dependency injection.
- Optional entity properties (isRequired=false) map to nullable types in the generated entity class and optional form fields in the UI.
- The auto-generated primary key (`id: Long`) is always included and is not shown in the UI form.

## Dependencies

- Feature 039 (UserProfiles module) — provides the reference implementation and module structure template.
- Feature 046 (Persistence dependency injection) — provides the dependency injection architecture for DAO access.
- Existing RepositoryCodeGenerator — provides the Entity, DAO, Repository, and database code generation (already generalized).
- Existing ModuleSaveService — provides module directory creation and file writing infrastructure.

## Out of Scope

- Generating test files for the entity module.
- Custom UI layouts or themes beyond the standard UserProfiles pattern.
- Handling entity relationships (foreign keys, one-to-many, many-to-many).
- Database migration generation when entity properties change.
- Automatically registering the generated module in settings.gradle.kts or adding it to the graph editor's module list.
