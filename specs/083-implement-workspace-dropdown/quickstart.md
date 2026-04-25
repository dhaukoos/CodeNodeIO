# Quickstart Verification: Implement Module as Workspace via Dropdown

**Feature**: 083-implement-workspace-dropdown
**Date**: 2026-04-25

## Verification Scenarios

### VS1: Module Dropdown Visible
**Steps**: Launch graph editor. Inspect toolbar.
**Expected**: Module dropdown visible (not gear icon). Shows "[No Module ▼]" if no workspace.

### VS2: Create New Module via Dropdown
**Steps**: Click dropdown → "Create New Module..." → enter name, select platforms → "Create Module" → select directory.
**Expected**: Module scaffolding created. Dropdown shows new module name.

### VS3: Title Bar Shows FlowGraph Name
**Steps**: Open a .flow.kt file. Inspect window title bar.
**Expected**: "CodeNodeIO — {FlowGraphName}".

### VS4: New FlowGraph — Name Only
**Steps**: With module loaded, click "New" → enter name → confirm.
**Expected**: Single-field dialog. Blank canvas. Title bar updates.

### VS5: Save — No Directory Prompt
**Steps**: Modify a flowGraph. Click "Save".
**Expected**: File written to `{module}/flow/{name}.flow.kt`. No directory chooser appears.

### VS6: Open — Scoped to Module
**Steps**: Click "Open".
**Expected**: File chooser opens in current module's flow/ directory, showing only .flow.kt files.

### VS7: Switch Module via Dropdown
**Steps**: Click dropdown → select a different module from MRU list.
**Expected**: Workspace switches. Dropdown shows new module. Canvas updates.

### VS8: Unsaved Changes Prompt
**Steps**: Modify flowGraph. Switch modules via dropdown.
**Expected**: Prompt: "Save before switching?" with Save / Don't Save / Cancel.

### VS9: Workspace Persists Across Restarts
**Steps**: Open a module. Close app. Reopen.
**Expected**: Same module loaded as workspace.

### VS10: New/Save Disabled Without Module
**Steps**: Launch with no workspace (first launch or cleared config).
**Expected**: New and Save buttons disabled. Open available for "Open from..." only.
