# Feature Specification: Remove Repository Module

**Feature Branch**: `048-remove-entity-module`
**Created**: 2026-03-11
**Status**: Draft
**Input**: User description: "Add a Remove Repository Module feature to the entity module generator (feature 047). When a module already exists for an IP Type, instead of showing the disabled Module exists message, show a Remove Repository Module button. Clicking it should remove all generated artifacts: custom node definitions, module directory, persistence files, AppDatabase entries, and Gradle file entries."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Remove an existing entity module (Priority: P1)

A user has previously created an entity module for an IP Type (e.g., "Address") using the "Create Repository Module" button. They now want to remove it entirely — perhaps because it was created with incorrect properties, or they want to start fresh. Instead of seeing a disabled "Module exists" label, they see a "Remove Repository Module" button. Clicking it removes all generated artifacts and returns the IP Type to a state where it can have a new module created.

**Why this priority**: This is the core and only feature — enabling module removal to support iterative development and testing workflows.

**Independent Test**: Can be tested by creating a module for an IP Type, then clicking "Remove Repository Module" and verifying all artifacts are cleaned up and the "Create Repository Module" button reappears.

**Acceptance Scenarios**:

1. **Given** an IP Type with an existing entity module, **When** the user selects that IP Type in the Properties Panel, **Then** a "Remove Repository Module" button is displayed instead of the disabled "Module exists" label.
2. **Given** the user clicks "Remove Repository Module", **When** the operation completes, **Then** the three custom node definitions (CUD, Repository, Display) are removed from the custom node repository.
3. **Given** the user clicks "Remove Repository Module", **When** the operation completes, **Then** the generated module directory (e.g., `Addresses/`) is deleted from the project.
4. **Given** the user clicks "Remove Repository Module", **When** the operation completes, **Then** the persistence files (Entity, Dao, Repository) are removed from the persistence module.
5. **Given** the user clicks "Remove Repository Module", **When** the operation completes, **Then** `AppDatabase.kt` is regenerated without the removed entity while preserving all other entities.
6. **Given** the user clicks "Remove Repository Module", **When** the operation completes, **Then** the `include(":ModuleName")` entry is removed from `settings.gradle.kts`.
7. **Given** the user clicks "Remove Repository Module", **When** the operation completes, **Then** the `implementation(project(":ModuleName"))` entry is removed from `graphEditor/build.gradle.kts`.
8. **Given** the removal operation completes successfully, **When** the user views the IP Type in the Properties Panel, **Then** the "Create Repository Module" button is available again.

---

### User Story 2 - Confirmation dialog before removal (Priority: P2)

The user clicks "Remove Repository Module" and a confirmation dialog appears asking if they are sure they want to remove the module. This prevents accidental deletion of a module and all its artifacts. The user can confirm to proceed or cancel to abort.

**Why this priority**: A destructive operation that deletes files and modifies build configuration requires explicit confirmation to prevent accidental data loss.

**Independent Test**: Can be tested by clicking "Remove Repository Module", verifying the dialog appears, then clicking cancel and confirming no artifacts were removed.

**Acceptance Scenarios**:

1. **Given** the user clicks "Remove Repository Module", **When** the button is clicked, **Then** a confirmation dialog appears asking "Are you sure you want to remove the [EntityName] module? This will delete the module directory, persistence files, and Gradle entries."
2. **Given** the confirmation dialog is displayed, **When** the user clicks "Confirm", **Then** the removal operation proceeds.
3. **Given** the confirmation dialog is displayed, **When** the user clicks "Cancel", **Then** no artifacts are removed and the dialog closes.

---

### User Story 3 - Status feedback after removal (Priority: P3)

After confirming the removal, the user sees a status message indicating the operation succeeded, including what was removed. If any part of the removal fails (e.g., module directory doesn't exist), the operation should still clean up what it can and report results.

**Why this priority**: Feedback and graceful error handling improve confidence in the operation but are secondary to the confirmation dialog and core removal logic.

**Independent Test**: Can be tested by removing a module and verifying the status bar message reflects the outcome.

**Acceptance Scenarios**:

1. **Given** the user confirms removal, **When** the operation completes successfully, **Then** a status message displays summarizing what was removed (e.g., "Removed Addresses module: 3 nodes, module directory, 3 persistence files, gradle entries").
2. **Given** a module directory was already manually deleted, **When** the user confirms removal, **Then** the remaining artifacts (custom nodes, persistence files, gradle entries) are still cleaned up without error.

---

### Edge Cases

- What happens when only some of the three custom node definitions exist (e.g., one was manually deleted)? The removal should still proceed, removing whatever exists.
- What happens when the persistence files don't exist on disk? The removal should skip missing files without error.
- What happens when the Gradle files don't contain the expected entries? The removal should skip the Gradle updates without error.
- What happens when the module directory doesn't exist? The removal should skip directory deletion without error.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display a "Remove Repository Module" button in the IP Type Properties Panel when all three entity module nodes (CUD, Repository, Display) exist for the selected IP Type.
- **FR-002**: System MUST display a confirmation dialog when the remove button is clicked, requiring the user to confirm before any artifacts are deleted.
- **FR-003**: System MUST remove the three custom node definitions (CUD, Repository, Display) from the custom node repository when the remove button is clicked.
- **FR-004**: System MUST delete the generated module directory (named after the plural entity name) from the project root when the remove button is clicked.
- **FR-005**: System MUST remove the entity's persistence files (Entity, Dao, Repository `.kt` files) from the persistence module source directory.
- **FR-006**: System MUST regenerate `AppDatabase.kt` to exclude the removed entity while preserving all other entities.
- **FR-007**: System MUST remove the `include(":ModuleName")` entry from `settings.gradle.kts`.
- **FR-008**: System MUST remove the `implementation(project(":ModuleName"))` entry from `graphEditor/build.gradle.kts`.
- **FR-009**: System MUST update the UI state after removal so the custom nodes list is refreshed and the "Create Repository Module" button becomes available again.
- **FR-010**: System MUST handle partial cleanup gracefully — if any artifact is already missing, the operation should continue removing remaining artifacts without failing.
- **FR-011**: System MUST display a status message after removal indicating the result.

### Key Entities

- **CustomNodeDefinition**: The three node definitions (CUD source, Repository processor, Display sink) stored in the custom node repository, identified by `sourceIPTypeId`.
- **Entity Module Directory**: The generated Gradle module directory containing flow graph, runtime, processing logic, and UI files.
- **Persistence Files**: The Entity, Dao, and Repository Kotlin files in the shared persistence module.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can remove a previously created entity module with a single button click.
- **SC-002**: After removal, the "Create Repository Module" button is immediately available for the same IP Type.
- **SC-003**: All seven artifact categories (3 custom nodes, module directory, 3 persistence files, AppDatabase, settings.gradle.kts, build.gradle.kts) are cleaned up by the removal operation.
- **SC-004**: The removal operation completes successfully even when some artifacts are already missing.

## Assumptions

- The project root directory can be determined from the location of `settings.gradle.kts` (found by walking up from the running application's working directory).
- The persistence module is always at `persistence/src/commonMain/kotlin/io/codenode/persistence` relative to the project root.
- The module directory name matches the plural entity name (e.g., "Addresses" for the "Address" IP Type).
- The entity name for persistence files can be derived from the IP Type name (e.g., "Address" -> "AddressEntity.kt", "AddressDao.kt", "AddressRepository.kt").
