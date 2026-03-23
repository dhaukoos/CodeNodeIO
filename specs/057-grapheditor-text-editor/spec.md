# Feature Specification: GraphEditor Text Editor

**Feature Branch**: `057-grapheditor-text-editor`
**Created**: 2026-03-22
**Status**: Draft
**Input**: User description: "Add TextEditor support to graphEditor — enable editing CodeNode text files generated via the Node Generator directly within the graphEditor UI."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Edit CodeNode Source Files in GraphEditor (Priority: P1)

As a user who has generated a CodeNode via the Node Generator, I want to edit that node's source file directly within the graphEditor so that I can customize the node's processing logic without leaving the application.

When a node is selected in the flowGraph canvas and the user clicks the edit button (pencil icon) in the Properties Panel header, the central panel switches to Textual view mode and displays the selected node's source file in an editable code editor. The user can modify the code, and the editor provides a developer-friendly editing experience with syntax highlighting, line numbers, and standard text editing capabilities.

**Why this priority**: This is the core value of the feature — enabling in-app code editing eliminates context switching to an external IDE for simple node customizations.

**Independent Test**: Select a CodeNode on the canvas, click the pencil icon in the Properties Panel header, verify the central panel switches to Textual view and shows the node's source file in an editable code editor with syntax highlighting.

**Acceptance Scenarios**:

1. **Given** a flowGraph with at least one CodeNode is open, **When** the user selects a node and clicks the pencil icon in the Properties Panel header, **Then** the central panel switches to Textual view mode and displays the selected node's source file in an editable code editor.
2. **Given** the code editor is displaying a CodeNode file, **When** the user modifies the code, **Then** the changes are reflected in the editor and can be saved to the file.
3. **Given** the code editor is displaying a CodeNode file, **When** the user makes edits, **Then** the editor provides syntax highlighting, line numbers, and standard text editing features (copy, paste, undo, redo, find).
4. **Given** the code editor has unsaved changes, **When** the user attempts to navigate away (switch nodes, close editor, or switch to Visual view), **Then** the system either auto-saves or prompts the user to save/discard changes.

---

### User Story 2 - Navigate to Node Files via Central Panel Dropdown (Priority: P2)

As a user working in the graphEditor, I want a dropdown selector in the central panel header that lets me choose between the flowGraph file and individual node source files, so that I can quickly navigate to any file in the current flowGraph context.

The dropdown appears on the left side of the central flowGraph panel header. The current flowGraph file is the default selection. Other options are the individual CodeNode files present in the flowGraph. Selecting a node from this dropdown selects that node in the canvas and, when Textual view mode is active, displays the corresponding source file in the code editor.

**Why this priority**: This provides an alternative navigation pathway that complements the pencil-icon approach, making file discovery more intuitive especially for flowGraphs with many nodes.

**Independent Test**: Open a flowGraph with multiple CodeNodes, click the dropdown in the central panel header, verify all node files are listed, select one, and verify the node becomes selected and its file appears in the editor when in Textual view.

**Acceptance Scenarios**:

1. **Given** a flowGraph is open with multiple CodeNodes, **When** the user clicks the dropdown in the central panel header, **Then** the dropdown lists the current flowGraph file as the default plus all CodeNode source files in the flowGraph.
2. **Given** the dropdown is open, **When** the user selects a CodeNode entry, **Then** that node becomes selected in the canvas and, if in Textual view mode, the code editor displays that node's source file.
3. **Given** the dropdown is showing a CodeNode file and the view is Textual, **When** the user switches back to the flowGraph file in the dropdown, **Then** the Textual view shows the read-only flowGraph DSL (existing behavior).

---

### User Story 3 - Read-Only FlowGraph Textual View (Priority: P3)

As a user viewing the flowGraph in Textual mode, I want the flowGraph DSL to remain read-only so that I don't accidentally modify the serialized graph structure, which is intended to be edited only via the visual canvas.

The existing Textual view for flowGraph files continues to display as read-only. When the dropdown selector has the flowGraph file selected and the view is Textual, the editor area shows the DSL text without editing capabilities — matching current behavior. The read-only state is visually distinguishable from the editable state (e.g., different background shade or a read-only indicator).

**Why this priority**: This is primarily about preserving existing behavior and adding a visual distinction. The current Textual view is already read-only, so this story mostly ensures the new editable mode doesn't break that contract.

**Independent Test**: Open a flowGraph, switch to Textual view with the flowGraph file selected in the dropdown, verify the DSL text is displayed but cannot be edited. Then switch to a CodeNode file and verify editing is enabled.

**Acceptance Scenarios**:

1. **Given** the flowGraph file is selected in the dropdown and the view is Textual, **When** the user attempts to type or edit, **Then** no changes are made to the displayed text.
2. **Given** the flowGraph file is displayed in Textual view, **When** the user observes the editor area, **Then** there is a visual indicator that the content is read-only (e.g., subtle background difference or a "Read Only" label).
3. **Given** the user switches from a read-only flowGraph view to an editable CodeNode view, **When** the transition occurs, **Then** the editor state changes from read-only to editable and the visual distinction updates accordingly.

---

### Edge Cases

- What happens when a CodeNode's source file doesn't exist on disk (e.g., deleted externally)? The editor displays an error message indicating the file is missing.
- What happens when multiple users or processes modify the same file externally while it's open in the editor? The editor does not auto-reload; the user sees whatever was loaded at open time and can overwrite on save.
- What happens when the user selects a node whose source file cannot be discovered on disk? The pencil icon is hidden, and the node does not appear in the dropdown's file list.
- What happens when the flowGraph has no CodeNodes? The dropdown only shows the flowGraph file entry. The pencil icon in the Properties Panel header is not shown.
- What happens when a file save fails (e.g., permissions issue)? The system displays an error notification and the unsaved changes remain in the editor.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display an edit button (pencil icon) in the Properties Panel header when a CodeNode with an editable source file is selected.
- **FR-002**: Clicking the edit button MUST switch the central panel to Textual view mode and display the selected CodeNode's source file in an editable code editor.
- **FR-003**: The code editor MUST provide syntax highlighting appropriate for the source file language.
- **FR-004**: The code editor MUST display line numbers alongside the code content.
- **FR-005**: The code editor MUST support standard text editing operations: typing, selection, copy, paste, cut, undo, and redo.
- **FR-006**: The system MUST provide a mechanism to save edited file content back to disk.
- **FR-007**: The central panel header MUST include a dropdown selector on its left side that lists the flowGraph file and all CodeNode source files in the current flowGraph.
- **FR-008**: Selecting a CodeNode from the dropdown MUST select that node in the canvas.
- **FR-009**: When in Textual view, selecting a CodeNode from the dropdown MUST display that node's source file in the code editor.
- **FR-010**: When the flowGraph file is selected in the dropdown and Textual view is active, the content MUST be displayed as read-only (matching current behavior).
- **FR-011**: The read-only state MUST be visually distinguishable from the editable state.
- **FR-012**: The edit button MUST NOT appear for nodes whose `.kt` source file cannot be discovered on disk. A node is editable if its source file is discoverable from any scanned directory (template nodes, module source directories, or project source directories).
- **FR-013**: The system MUST handle missing source files gracefully by displaying an appropriate error message in the editor area.

### Key Entities

- **CodeNode Source File**: A source file on disk associated with a CodeNode in the flowGraph. Key attributes: file path (from `CodeNodeDefinition.sourceFilePath` for compiled nodes, or from template metadata for uncompiled nodes), file content, read/write status, associated node name.
- **Editor State**: Tracks the current file being edited, whether content is modified (dirty), and the read-only/editable mode.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can open and edit a CodeNode source file within the graphEditor in under 3 seconds (click pencil icon to editor ready).
- **SC-002**: Users can navigate to any node's source file via the dropdown selector in 2 clicks or fewer.
- **SC-003**: 100% of CodeNodes with discoverable `.kt` source files on disk are accessible for editing through the graphEditor.
- **SC-004**: FlowGraph DSL files remain uneditable in Textual view — zero accidental modifications possible.
- **SC-005**: All standard text editing operations (type, select, copy, paste, undo, redo) work correctly in the code editor.

## Clarifications

### Session 2026-03-22

- Q: Should compiled module nodes (e.g., StopWatch's TimeIncrementer) also become editable by resolving their source file paths from known module directories? → A: Yes — make all nodes with discoverable `.kt` source files editable by scanning module source directories too.
- Q: How should the system discover source file paths for compiled (module-level) nodes? → A: Add a `sourceFilePath` property to `CodeNodeDefinition` so compiled nodes self-declare their source location at registration time.

## Assumptions

- The code editor library will be a native Compose library (e.g., compose-code-editor or similar) that integrates with the existing Compose Desktop UI without requiring external process launches.
- CodeNode source files are Kotlin (.kt) files; syntax highlighting targets Kotlin syntax.
- Any CodeNode whose `.kt` source file can be discovered on disk is editable — this includes template nodes (Universal level), module source files, and project source files. Nodes are non-editable only when no source file can be located on disk.
- File save is a direct overwrite to the source file path — no version control integration is included in this feature.
- The pencil icon uses Material Icons (consistent with existing UI conventions in the graphEditor).
