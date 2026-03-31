# Feature Specification: Group Persistence Files by Entity

**Feature Branch**: `062-group-persistence-files`
**Created**: 2026-03-31
**Status**: Draft
**Input**: User description: "When a new repository module is created, in the persistence module, group the three generated files ({entity}Dao, {entity}Entity, {entity}Repository) in a directory named {entity}. Also, make this change for the existing Repository module entity types, UserProfile and Address."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - New Modules Generate Grouped Files (Priority: P1)

When a developer creates a new repository module via the graphEditor, the three persistence files (`{Entity}Entity`, `{Entity}Dao`, `{Entity}Repository`) are placed inside a subdirectory named after the entity within the shared persistence module's source tree, rather than at the flat package root.

**Why this priority**: This governs all future modules and is the primary change requested. Getting new module generation correct prevents future cleanup work.

**Independent Test**: Create a new repository module (e.g., "Product") via the graphEditor and verify that `ProductEntity.kt`, `ProductDao.kt`, and `ProductRepository.kt` all reside under a `Product/` subdirectory within the persistence source tree — then compile to confirm zero errors.

**Acceptance Scenarios**:

1. **Given** no existing module for "Product", **When** a developer creates a "Product" repository module, **Then** `ProductEntity.kt`, `ProductDao.kt`, and `ProductRepository.kt` are generated inside a `Product/` subdirectory within the persistence source tree.
2. **Given** a newly generated module with grouped files, **When** the project is compiled, **Then** all persistence classes resolve correctly with zero errors.
3. **Given** a newly generated module, **When** the runtime pipeline is launched, **Then** the repository node functions correctly end-to-end.

---

### User Story 2 - Existing Modules Migrated to Grouped Layout (Priority: P2)

The existing persistence files for `UserProfile` and `Address` entity types are reorganized into their own subdirectories (`UserProfile/` and `Address/`) within the persistence module source tree, matching the new layout convention.

**Why this priority**: Consistency across all modules is important for maintainability. Existing modules should match newly generated ones.

**Independent Test**: Inspect the persistence module source tree and confirm `UserProfileEntity.kt`, `UserProfileDao.kt`, `UserProfileRepository.kt` are under `UserProfile/`, and the Address equivalents are under `Address/`. Compile all modules and run runtime previews to confirm zero regressions.

**Acceptance Scenarios**:

1. **Given** existing UserProfile persistence files at the flat root level, **When** migration is applied, **Then** all three files reside under a `UserProfile/` subdirectory and the project compiles without errors.
2. **Given** existing Address persistence files at the flat root level, **When** migration is applied, **Then** all three files reside under an `Address/` subdirectory and the project compiles without errors.
3. **Given** migrated persistence files, **When** the UserProfiles or Addresses runtime pipeline is run, **Then** all CRUD operations work correctly with no regressions.

---

### Edge Cases

- What happens when module removal is triggered — the entire `{Entity}/` subdirectory must be deleted, not just individual files.
- What if a persistence subdirectory for an entity already partially exists when generation runs (conflict avoidance)?
- Imports across the codebase that reference persistence classes must all be updated to the new package path.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: When generating persistence files for a new repository module, the system MUST place `{Entity}Entity.kt`, `{Entity}Dao.kt`, and `{Entity}Repository.kt` inside a subdirectory named `{Entity}` within the persistence module source tree.
- **FR-002**: The package declaration inside each generated persistence file MUST reflect the subdirectory (e.g., `io.codenode.persistence.{entity}`).
- **FR-003**: The existing `UserProfileEntity.kt`, `UserProfileDao.kt`, and `UserProfileRepository.kt` files MUST be moved into a `UserProfile/` subdirectory with updated package declarations.
- **FR-004**: The existing `AddressEntity.kt`, `AddressDao.kt`, and `AddressRepository.kt` files MUST be moved into an `Address/` subdirectory with updated package declarations.
- **FR-005**: All import statements across the codebase that reference the moved persistence classes MUST be updated to reflect the new package paths.
- **FR-006**: When a repository module is removed, the system MUST delete the entire `{Entity}/` subdirectory from persistence rather than individual files.
- **FR-007**: After all changes, all existing and newly generated modules MUST compile and run without errors.

### Key Entities

- **Persistence Module**: The shared module containing all entity, DAO, and repository source files for all repository-backed modules.
- **Entity Subdirectory**: A folder named after the entity (PascalCase) that groups that entity's three persistence files.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A newly created repository module produces its three persistence files inside an `{Entity}/` subdirectory — verifiable by inspecting the generated file tree immediately after creation.
- **SC-002**: All existing modules (UserProfiles, Addresses) compile with zero errors after migration — verifiable by running a full project build.
- **SC-003**: Runtime pipelines for all existing modules function correctly after migration — verifiable by running the runtime preview for each and performing CRUD operations.
- **SC-004**: Module removal deletes the full `{Entity}/` subdirectory with no orphaned files remaining in the persistence module.

## Assumptions

- Only `UserProfile` and `Address` need migration; other previously deleted entity types are out of scope.
- The subdirectory name matches the entity name exactly in PascalCase (e.g., `UserProfile/`, `Address/`).
- Package names within generated files use a lowercase/consistent form following existing project conventions (e.g., `io.codenode.persistence.userprofile`).
- `AppDatabase.kt` entity registrations do not require structural changes — only import paths may need updating.
