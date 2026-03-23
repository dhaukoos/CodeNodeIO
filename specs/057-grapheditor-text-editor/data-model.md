# Data Model: GraphEditor Text Editor

**Feature**: 057-grapheditor-text-editor
**Date**: 2026-03-22

---

## Entities

### EditorState

Tracks the state of the code editor within the graphEditor. This is in-memory UI state, not persisted.

| Field | Type | Description |
|-------|------|-------------|
| currentFile | File? | The file currently loaded in the editor (null = no file open) |
| originalContent | String | Content as loaded from disk (for dirty detection) |
| editedContent | String | Current content in the editor (may differ from originalContent) |
| isDirty | Boolean | Whether editedContent differs from originalContent |
| isReadOnly | Boolean | True for flowGraph DSL files, false for CodeNode source files |
| cursorPosition | Int | Current cursor position in the text |

### FileEntry

Represents a selectable file in the central panel dropdown.

| Field | Type | Description |
|-------|------|-------------|
| displayName | String | Label shown in dropdown (e.g., "MyGraph.flow.kt", "FilterNode") |
| filePath | File | Absolute path to the file on disk |
| isFlowGraph | Boolean | True if this is the flowGraph DSL file (read-only) |
| associatedNodeId | String? | The node ID in the flowGraph this file belongs to (null for flowGraph file) |

---

## Relationships

- **EditorState** → **FileEntry**: The editor displays content for exactly one FileEntry at a time.
- **FileEntry** → **FlowGraph node**: Each CodeNode FileEntry maps to a node in the current flowGraph via `associatedNodeId`. Selecting a FileEntry in the dropdown also selects the corresponding node on the canvas.
- **FlowGraph** → **FileEntry list**: A flowGraph produces one FileEntry for itself (the .flow.kt file) plus one FileEntry per CodeNode that has an on-disk source file.

---

## State Transitions

### Editor Mode Transitions

```
No File Selected
    │
    ├─ [Select flowGraph in dropdown] → ReadOnly Mode (flowGraph DSL)
    │                                      │
    │                                      ├─ [Select CodeNode in dropdown] → Editable Mode
    │                                      └─ [Switch to Visual view] → No File Displayed
    │
    ├─ [Click pencil icon on CodeNode] → Editable Mode (CodeNode source)
    │                                      │
    │                                      ├─ [Edit text] → Dirty Editable Mode
    │                                      │                  │
    │                                      │                  ├─ [Save] → Clean Editable Mode
    │                                      │                  ├─ [Navigate away] → Prompt Save/Discard
    │                                      │                  └─ [Discard] → Clean Editable Mode
    │                                      │
    │                                      ├─ [Select different node] → (check dirty → prompt) → Editable Mode (new file)
    │                                      └─ [Select flowGraph in dropdown] → (check dirty → prompt) → ReadOnly Mode
    │
    └─ [Switch to Visual view] → Canvas displayed, editor state preserved
```
