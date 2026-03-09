# Feature Specification: Refactor UserProfiles Module

**Feature Branch**: `045-refactor-userprofiles-module`
**Created**: 2026-03-08
**Status**: Draft
**Input**: User description: "Refactor UserProfile module to better define discrete components by extracting persistence into a shared module and extracting source/sink node definitions into distinct files."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Extract Persistence into a Shared Module (Priority: P1)

A developer maintaining the codebase recognizes that the persistence infrastructure (AppDatabase, BaseDao, DatabaseModule, platform-specific DatabaseBuilders) and the entity/DAO classes (UserProfileEntity, UserProfileDao, UserProfileRepository) currently reside in the UserProfiles module. These files are tightly coupled to the Room persistence layer and serve as the data access foundation that future entity modules will also need. The developer creates a new shared `persistence` Gradle module and moves all persistence-related files there with a new `io.codenode.persistence` package. After the move, all existing functionality continues to work — the UserProfiles module references persistence types from the shared module instead of owning them.

**Why this priority**: This is the foundational refactoring step. The current architecture creates an undesirable coupling — any future module needing database access would depend on UserProfiles. Extracting persistence into its own module properly positions it as shared infrastructure. Moving entity/DAO classes alongside the infrastructure avoids circular dependencies (AppDatabase references UserProfileEntity, and UserProfileDao extends BaseDao). All other refactoring depends on the application still building and running correctly after this extraction.

**Independent Test**: Build and run the application after creating the persistence module and moving the files. Open the UserProfiles module in Runtime Preview, perform CRUD operations (Add, Update, Remove a profile), and verify all operations succeed with data persisting correctly.

**Acceptance Scenarios**:

1. **Given** the persistence files exist in the UserProfiles module, **When** they are extracted into a new shared persistence module, **Then** the application builds successfully with no compilation errors
2. **Given** the persistence module has been created, **When** the UserProfiles module performs CRUD operations, **Then** all operations succeed identically to before the extraction
3. **Given** the persistence module has been created, **When** a developer inspects the UserProfiles module, **Then** it no longer contains any persistence-related files (AppDatabase, BaseDao, DatabaseModule, UserProfileEntity, UserProfileDao, UserProfileRepository, or platform-specific DatabaseBuilder files)
4. **Given** the persistence module has been created, **When** a developer inspects the module dependency graph, **Then** UserProfiles depends on persistence (not vice versa), and no circular dependencies exist

---

### User Story 2 - Extract Source and Sink Nodes into Distinct Files (Priority: P2)

A developer working on the UserProfiles module wants to understand and maintain the individual CodeNode components. Currently, the UserProfileCUD source node and UserProfilesDisplay sink node definitions are embedded within the generated UserProfilesFlow file. The developer extracts each node into its own distinct file, making each component independently identifiable, readable, and maintainable. After extraction, the module behaves identically — the flow graph executes the same way with the same nodes, just organized into separate files.

**Why this priority**: This componentization improves code organization and maintainability. It establishes a pattern for how source and sink nodes should be structured as discrete components, which is a prerequisite for future generalization of EntityRepository modules.

**Independent Test**: Build and run the application after extracting the nodes. Open the UserProfiles module in Runtime Preview, perform CRUD operations, and verify the flow graph behaves identically — same node names, same connections, same data flow.

**Acceptance Scenarios**:

1. **Given** UserProfileCUD logic exists within a larger file, **When** it is extracted into its own distinct file, **Then** the application builds successfully
2. **Given** UserProfilesDisplay logic exists within a larger file, **When** it is extracted into its own distinct file, **Then** the application builds successfully
3. **Given** both nodes have been extracted, **When** the UserProfiles module executes in Runtime Preview, **Then** all CRUD operations and display updates work identically to before the extraction
4. **Given** both nodes have been extracted, **When** a developer inspects the module structure, **Then** each node (UserProfileCUD, UserProfilesDisplay) has its own dedicated file

---

### Edge Cases

- What happens if other modules already reference the persistence files from the UserProfiles module? All import paths must be updated to reference the new `io.codenode.persistence` package.
- What happens if the Room KSP compiler runs in the wrong module? The KSP room-compiler processors must be configured in the persistence module where AppDatabase and entity annotations live.
- What happens if the extracted node files introduce circular dependencies? The extraction must maintain the existing dependency direction — nodes reference shared types, not the other way around.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A new shared `persistence` Gradle module MUST be created with JVM, Android, and iOS targets
- **FR-002**: All persistence infrastructure files (AppDatabase, BaseDao, DatabaseModule, platform-specific DatabaseBuilder files) MUST be moved from UserProfiles to the persistence module
- **FR-003**: All entity and DAO files (UserProfileEntity, UserProfileDao, UserProfileRepository) MUST be moved from UserProfiles to the persistence module
- **FR-004**: All files moved to the persistence module MUST have their package declarations updated to `io.codenode.persistence`
- **FR-005**: All import references to moved files MUST be updated across the codebase to reflect the new package
- **FR-006**: The UserProfileCUD source node MUST be extracted into its own distinct file within the UserProfiles module
- **FR-007**: The UserProfilesDisplay sink node MUST be extracted into its own distinct file within the UserProfiles module
- **FR-008**: All existing UserProfiles CRUD functionality MUST continue to work identically after the refactoring
- **FR-009**: The application MUST build and run without errors after all changes are complete
- **FR-010**: The dependency graph MUST have no circular dependencies — UserProfiles depends on persistence, not vice versa

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The application builds with zero compilation errors after all refactoring is complete
- **SC-002**: 100% of existing UserProfiles CRUD operations (create, read, update, delete) work identically in Runtime Preview after the refactoring
- **SC-003**: The UserProfiles module contains zero persistence files — all persistence types reside in the shared persistence module
- **SC-004**: Each of the two CodeNodes (UserProfileCUD, UserProfilesDisplay) exists in its own distinct file, with zero node definitions remaining embedded in other files
- **SC-005**: The dependency graph is acyclic — persistence module has no project dependencies, UserProfiles depends on persistence and fbpDsl

## Assumptions

- A new Gradle module (`persistence`) is the appropriate level of abstraction for shared persistence infrastructure.
- Moving entity and DAO classes (UserProfileEntity, UserProfileDao, UserProfileRepository) alongside infrastructure avoids circular dependencies, since AppDatabase references entity classes.
- "Distinct files" means each CodeNode (UserProfileCUD, UserProfilesDisplay) gets its own dedicated source file, separate from the flow graph wiring and controller files.
- This refactoring is a pure structural change — no new features, no behavior changes, no API changes.
- The persistence module needs JVM, Android, and iOS targets to match the platforms where database access is required.
- The existing processing logic files for nodes remain in their current locations; only the node creation/definition code is being extracted.

## Scope

### In Scope

- Creating a new `persistence` Gradle module with JVM, Android, and iOS targets
- Moving all persistence-related files from UserProfiles to the persistence module
- Updating package declarations from `io.codenode.userprofiles.persistence` to `io.codenode.persistence`
- Extracting UserProfileCUD and UserProfilesDisplay into distinct files
- Updating all import paths across the codebase
- Moving Room, KSP, and SQLite build dependencies to the persistence module

### Out of Scope

- Generalizing the EntityRepository module creation pattern (future feature)
- Modifying any node behavior or processing logic
- Creating new nodes or removing existing nodes
- Adding new entity types to the persistence module (future feature)

## Dependencies

- Feature 036 (EntityRepository node for UserProfile IP Type) — established the UserProfileRepository node
- Feature 039 (EntityRepository module for UserProfile) — established the current module structure with UserProfileCUD and UserProfilesDisplay
