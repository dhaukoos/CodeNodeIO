# Feature Specification: Refactor UserProfiles Module

**Feature Branch**: `045-refactor-userprofiles-module`
**Created**: 2026-03-08
**Status**: Draft
**Input**: User description: "Refactor UserProfile module to better define discrete components by relocating shared persistence files to the app module and extracting source/sink node definitions into distinct files."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Relocate Shared Persistence Files to App Module (Priority: P1)

A developer maintaining the codebase recognizes that the persistence infrastructure files (AppDatabase, BaseDao, DatabaseModule) currently reside in the UserProfiles module, but they are shared application-level concerns that other modules will also need. The developer moves these files from the UserProfiles module's persistence folder to the KMPMobileApp module in a new analogous persistence folder. After the move, all existing functionality continues to work — the UserProfiles module references the persistence layer from the app module instead of owning it.

**Why this priority**: This is the foundational refactoring step. The persistence files are application-level infrastructure, not UserProfiles-specific. Moving them first unblocks future modules from depending on UserProfiles just to access the database. All other refactoring depends on the application still building and running correctly after this move.

**Independent Test**: Build and run the application after moving the three persistence files. Open the UserProfiles module in Runtime Preview, perform CRUD operations (Add, Update, Remove a profile), and verify all operations succeed with data persisting correctly.

**Acceptance Scenarios**:

1. **Given** the persistence files exist in the UserProfiles module, **When** they are moved to the KMPMobileApp module in a new persistence folder, **Then** the application builds successfully with no compilation errors
2. **Given** the persistence files have been moved, **When** the UserProfiles module performs CRUD operations, **Then** all operations succeed identically to before the move
3. **Given** the persistence files have been moved, **When** a developer inspects the UserProfiles module, **Then** it no longer contains AppDatabase, BaseDao, or DatabaseModule files

---

### User Story 2 - Extract Source and Sink Nodes into Distinct Files (Priority: P2)

A developer working on the UserProfiles module wants to understand and maintain the individual CodeNode components. Currently, the UserProfileCUD source node and UserProfilesDisplay sink node definitions are embedded within larger generated files. The developer extracts each node into its own distinct file, making each component independently identifiable, readable, and maintainable. After extraction, the module behaves identically — the flow graph executes the same way with the same nodes, just organized into separate files.

**Why this priority**: This componentization improves code organization and maintainability. It establishes a pattern for how source and sink nodes should be structured as discrete components, which is a prerequisite for future generalization of EntityRepository modules. However, it depends on US1 being complete since the persistence references may need updating.

**Independent Test**: Build and run the application after extracting the nodes. Open the UserProfiles module in Runtime Preview, perform CRUD operations, and verify the flow graph behaves identically — same node names, same connections, same data flow.

**Acceptance Scenarios**:

1. **Given** UserProfileCUD logic exists within a larger file, **When** it is extracted into its own distinct file, **Then** the application builds successfully
2. **Given** UserProfilesDisplay logic exists within a larger file, **When** it is extracted into its own distinct file, **Then** the application builds successfully
3. **Given** both nodes have been extracted, **When** the UserProfiles module executes in Runtime Preview, **Then** all CRUD operations and display updates work identically to before the extraction
4. **Given** both nodes have been extracted, **When** a developer inspects the module structure, **Then** each node (UserProfileCUD, UserProfilesDisplay) has its own dedicated file

---

### Edge Cases

- What happens if other modules already reference the persistence files from the UserProfiles module? All import paths must be updated to reference the new location in the KMPMobileApp module.
- What happens if the build system has module-level dependencies that break after moving files? Module dependency declarations must be updated so UserProfiles depends on KMPMobileApp for persistence access.
- What happens if the extracted node files introduce circular dependencies? The extraction must maintain the existing dependency direction — nodes reference shared types, not the other way around.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The persistence infrastructure files (AppDatabase, BaseDao, DatabaseModule) MUST be relocated from the UserProfiles module to the KMPMobileApp module in a new persistence folder
- **FR-002**: All import references to the moved persistence files MUST be updated across the codebase to reflect the new location
- **FR-003**: The UserProfileCUD source node MUST be extracted into its own distinct file within the UserProfiles module
- **FR-004**: The UserProfilesDisplay sink node MUST be extracted into its own distinct file within the UserProfiles module
- **FR-005**: All existing UserProfiles CRUD functionality MUST continue to work identically after the refactoring
- **FR-006**: The application MUST build and run without errors after all changes are complete
- **FR-007**: Module dependency declarations MUST be updated so UserProfiles correctly depends on KMPMobileApp for persistence access

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The application builds with zero compilation errors after all refactoring is complete
- **SC-002**: 100% of existing UserProfiles CRUD operations (create, read, update, delete) work identically in Runtime Preview after the refactoring
- **SC-003**: The UserProfiles module contains zero persistence infrastructure files (AppDatabase, BaseDao, DatabaseModule) — they reside solely in the KMPMobileApp module
- **SC-004**: Each of the two CodeNodes (UserProfileCUD, UserProfilesDisplay) exists in its own distinct file, with zero node definitions remaining embedded in other files

## Assumptions

- The KMPMobileApp module exists and is an appropriate location for shared application-level persistence infrastructure.
- The persistence files being moved (AppDatabase, BaseDao, DatabaseModule) are not UserProfiles-specific — they serve as application-wide database infrastructure.
- "Distinct files" means each CodeNode (UserProfileCUD, UserProfilesDisplay) gets its own dedicated source file, separate from the flow graph wiring and controller files.
- This refactoring is a pure structural change — no new features, no behavior changes, no API changes.
- The existing processing logic files for nodes remain in their current locations; only the node creation/definition code is being extracted.

## Scope

### In Scope

- Moving AppDatabase, BaseDao, and DatabaseModule from UserProfiles to KMPMobileApp
- Extracting UserProfileCUD and UserProfilesDisplay into distinct files
- Updating all import paths and module dependencies
- Verifying existing functionality is preserved

### Out of Scope

- Generalizing the EntityRepository module creation pattern (future feature)
- Modifying any node behavior or processing logic
- Creating new nodes or removing existing nodes
- Changes to any module other than UserProfiles and KMPMobileApp (beyond import path updates)

## Dependencies

- Feature 036 (EntityRepository node for UserProfile IP Type) — established the UserProfileRepository node
- Feature 039 (EntityRepository module for UserProfile) — established the current module structure with UserProfileCUD and UserProfilesDisplay
