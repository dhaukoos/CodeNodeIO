# Feature Specification: GraphEditor Save Functionality

**Feature Branch**: `029-grapheditor-save`
**Created**: 2026-02-25
**Status**: Draft
**Input**: User description: "GraphEditor Save functionality — When Saving a new flowgraph name, the graphEditor asks for a path to save the module to. When Saving a flowgraph that has previously been saved (with the same name), then update the existing module. Specifically, overwrite the .flow.kt file and the generated files to capture the graph updates. Do not update any existing stub files, but add new ones as needed by the changes, and delete any stubs whose nodes have been removed."

## User Scenarios & Testing

### User Story 1 - Save New FlowGraph to Disk (Priority: P1)

A user creates or opens a FlowGraph in the graphEditor and clicks Save for the first time. The system prompts the user to choose a directory, then creates the full module structure with all generated files at that location.

**Why this priority**: This is the foundational save operation. Without it, no work can be persisted.

**Independent Test**: Create a new FlowGraph with 2 nodes and 1 connection. Click Save. Select a directory. Verify the module directory is created with .flow.kt, generated runtime files, processing logic stubs, and state properties stubs.

**Acceptance Scenarios**:

1. **Given** a FlowGraph that has never been saved, **When** the user clicks Save, **Then** the system shows a directory chooser dialog.
2. **Given** the user selects a valid directory, **When** the save completes, **Then** the module directory is created containing the .flow.kt DSL file, 5 generated runtime files, processing logic stubs for each node, and state properties stubs for nodes with ports.
3. **Given** the user cancels the directory chooser, **When** the dialog closes, **Then** no files are created and the FlowGraph remains unsaved.

---

### User Story 2 - Re-Save Updated FlowGraph (Priority: P1)

A user modifies a previously saved FlowGraph (same name) and clicks Save again. The system automatically saves to the same location without prompting for a directory. The .flow.kt and generated runtime files are overwritten to reflect changes. Existing stub files (processing logic, state properties) are preserved. New stubs are created for any newly added nodes.

**Why this priority**: This is the core iterative workflow. Users will modify and re-save frequently during development.

**Independent Test**: Save a FlowGraph with 2 nodes. Add a third node. Save again. Verify the .flow.kt and runtime files reflect all 3 nodes. Verify the original 2 stubs are unchanged. Verify a new stub was created for the third node.

**Acceptance Scenarios**:

1. **Given** a FlowGraph that has been saved before (same name), **When** the user clicks Save, **Then** the system saves directly to the previously used directory without showing a directory chooser.
2. **Given** a re-save, **When** the save completes, **Then** the .flow.kt file at the module root is overwritten with the current FlowGraph state.
3. **Given** a re-save, **When** the save completes, **Then** all 5 generated runtime files are overwritten with updated content.
4. **Given** a re-save where the user has edited a processing logic stub, **When** the save completes, **Then** the user's modifications to existing stubs are preserved.
5. **Given** a re-save where a new node was added, **When** the save completes, **Then** new processing logic and state properties stubs are created for the added node.

---

### User Story 3 - Delete Stubs for Removed Nodes (Priority: P1)

When a user removes a node from the FlowGraph and saves, the system deletes the orphaned processing logic and state properties stub files for that node.

**Why this priority**: Keeping orphaned files creates confusion and potential compilation errors. Clean deletion maintains a consistent module.

**Independent Test**: Save a FlowGraph with 3 nodes. Remove one node. Save again. Verify the removed node's stub files are deleted. Verify the remaining nodes' stubs are unchanged.

**Acceptance Scenarios**:

1. **Given** a previously saved FlowGraph where a node has been removed, **When** the user saves, **Then** the processing logic stub for the removed node is deleted from disk.
2. **Given** a previously saved FlowGraph where a node has been removed, **When** the user saves, **Then** the state properties stub for the removed node is deleted from disk.
3. **Given** a previously saved FlowGraph where a node has been removed, **When** the save completes, **Then** the deleted files are reported in the save result.
4. **Given** a FlowGraph where no nodes were removed, **When** the user saves, **Then** no stub files are deleted.

---

### User Story 4 - Save FlowGraph Under a New Name (Priority: P2)

When a user changes the FlowGraph name (e.g., renames "StopWatch3" to "StopWatch4") and clicks Save, the system treats this as a new FlowGraph and prompts for a directory. The original module at the old location is left untouched.

**Why this priority**: Supports the "Save As" workflow for creating variants. Less common than the core save/re-save loop.

**Independent Test**: Save a FlowGraph named "Alpha". Rename it to "Beta". Save again. Verify a new directory prompt appears and a new "Beta" module is created while "Alpha" remains unchanged.

**Acceptance Scenarios**:

1. **Given** a FlowGraph that was saved as "Alpha" and then renamed to "Beta", **When** the user clicks Save, **Then** the system shows a directory chooser (since "Beta" has never been saved).
2. **Given** the user selects a directory for "Beta", **When** the save completes, **Then** a new "Beta" module is created at the selected location.
3. **Given** the "Beta" save completes, **When** the user inspects the original "Alpha" location, **Then** the "Alpha" module is untouched.

---

### Edge Cases

- What happens when the previously saved directory no longer exists? The system detects this and re-prompts for a directory, treating it as a new save.
- What happens when Save is clicked with an empty FlowGraph (no nodes)? The system saves the module structure and .flow.kt but generates no stubs or state properties files. Runtime files are still generated for the empty flow.
- What happens when the user lacks write permissions to the saved directory? The system displays an error message and does not corrupt any existing files.
- What happens when a node is renamed (same ID, different name)? The old stub file name no longer matches. The system creates a new stub for the new name and deletes the orphaned stub for the old name.

## Requirements

### Functional Requirements

- **FR-001**: The system MUST prompt for a save directory when a FlowGraph is being saved for the first time (no prior save location for that name).
- **FR-002**: The system MUST remember the save location for each FlowGraph name during the current session.
- **FR-003**: The system MUST skip the directory prompt and save directly when re-saving a FlowGraph with the same name as a previous save.
- **FR-004**: On every save, the system MUST overwrite the .flow.kt DSL file at the module root with the current FlowGraph state.
- **FR-005**: On every save, the system MUST overwrite all generated runtime files (Flow, Controller, ControllerInterface, ControllerAdapter, ViewModel) with freshly generated content.
- **FR-006**: On every save, the system MUST NOT overwrite existing processing logic stub files or state properties stub files.
- **FR-007**: On every save, the system MUST create new processing logic and state properties stubs for any nodes that do not yet have corresponding stub files.
- **FR-008**: On every save, the system MUST delete processing logic and state properties stub files for nodes that no longer exist in the FlowGraph.
- **FR-009**: When the FlowGraph name changes, the system MUST treat the save as a new FlowGraph (prompt for directory), leaving the old module untouched.
- **FR-010**: If a previously saved directory no longer exists, the system MUST re-prompt for a directory.
- **FR-011**: The save result MUST report which files were created, overwritten, preserved, and deleted.

### Key Entities

- **FlowGraph**: The in-memory graph model being edited. Identified by name.
- **Module Directory**: The on-disk directory structure containing all generated and user-edited files for a FlowGraph.
- **Save Location Registry**: A session-scoped mapping from FlowGraph name to the directory where it was last saved.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Users can save a new FlowGraph to disk in 3 or fewer interactions (click Save, choose directory, confirm).
- **SC-002**: Users can re-save a modified FlowGraph in a single interaction (click Save) with no directory prompt.
- **SC-003**: 100% of generated runtime files reflect the current FlowGraph state after every save.
- **SC-004**: 100% of user-edited stub files are preserved across re-saves with no data loss.
- **SC-005**: 100% of orphaned stub files (from removed nodes) are deleted on save, leaving no stale files.
- **SC-006**: All existing tests continue to pass after implementation.

## Assumptions

- Node names within a FlowGraph are unique (enforced by the FlowGraph model).
- The save location mapping is session-scoped; it does not persist across application restarts. If persistence is needed later, it can be added as a follow-up feature.
- The current separate Compile button becomes redundant since Save now includes compilation. The Compile button may be removed or repurposed in a future feature.
- The module directory structure (gradle files, source directories) is created on first save and reused on subsequent saves.
- Gradle files (build.gradle.kts, settings.gradle.kts) are overwritten on every save to stay consistent with the FlowGraph's target platforms.
