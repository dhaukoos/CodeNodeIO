# Quickstart: GraphEditor Text Editor

**Feature**: 057-grapheditor-text-editor
**Date**: 2026-03-22

---

## Scenario 1: Edit a CodeNode Source File via Pencil Icon

1. Launch graphEditor and open a flowGraph that contains CodeNodes (e.g., StopWatch)
2. Click on a CodeNode in the canvas to select it
3. In the Properties Panel header (right side), observe the pencil icon appears
4. Click the pencil icon
5. **Expected**: Central panel switches to Textual view. The selected node's source file loads in an editable code editor with syntax highlighting and line numbers.
6. Type some changes to the code
7. **Expected**: The editor shows modified content. A dirty indicator appears.
8. Save the file (via save mechanism)
9. **Expected**: File is written to disk. Dirty indicator clears.

## Scenario 2: Navigate to Node Files via Dropdown

1. Launch graphEditor and open a flowGraph with multiple CodeNodes
2. Observe the dropdown selector on the left side of the central panel header
3. **Expected**: Dropdown shows the flowGraph file name as the default selection
4. Click the dropdown
5. **Expected**: Dropdown lists the flowGraph file plus each CodeNode's source file
6. Select a CodeNode entry from the dropdown
7. **Expected**: The corresponding node becomes selected on the canvas. If in Textual view, the code editor displays that node's source file.

## Scenario 3: Read-Only FlowGraph DSL View

1. Launch graphEditor and open a flowGraph
2. Select the flowGraph file in the dropdown (or ensure it's already selected)
3. Switch to Textual view
4. **Expected**: The flowGraph DSL text is displayed with syntax highlighting
5. Attempt to type or edit the text
6. **Expected**: No modifications are possible. A visual indicator shows "Read Only" or uses a distinct background to signal read-only mode.

## Scenario 4: Unsaved Changes Warning

1. Open a CodeNode source file in the editor (via pencil icon or dropdown)
2. Make changes to the code
3. Select a different node from the dropdown (or click pencil icon for another node)
4. **Expected**: A dialog appears asking to Save, Discard, or Cancel
5. Click "Save" → changes saved, new file loads
6. (Repeat step 2-3) Click "Discard" → changes lost, new file loads
7. (Repeat step 2-3) Click "Cancel" → stays on current file with unsaved changes

## Scenario 5: Non-Editable Node (Built-in Node)

1. Open a flowGraph with a mix of CodeNodes and built-in nodes
2. Select a built-in node (one without a generated source file)
3. **Expected**: The pencil icon does NOT appear in the Properties Panel header
4. Check the dropdown
5. **Expected**: The built-in node does NOT appear in the file list

## Scenario 6: Missing Source File

1. Open a flowGraph with a CodeNode whose source file has been deleted from disk
2. Select that node and click the pencil icon (or select from dropdown)
3. **Expected**: The editor area shows an error message: "Source file not found: {path}"
