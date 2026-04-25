# Research: Implement Module as Workspace via Dropdown

**Feature**: 083-implement-workspace-dropdown
**Date**: 2026-04-25

## R1: Workspace State Model

**Decision**: Create a `WorkspaceState` that holds the current module context, active flowGraph, and MRU list. This replaces the existing `saveLocationRegistry` and scattered module-tracking state.

**State model**:
- `currentModuleDir: File?` — root directory of the active workspace module
- `currentModuleName: String` — module name (from directory name)
- `activeFlowGraphName: String?` — name of the currently open flowGraph
- `activeFlowGraphFile: File?` — file path of the current .flow.kt
- `mruModules: List<File>` — recently used module directories (max 5)
- `isDirty: Boolean` — whether the current flowGraph has unsaved changes

**Persistence**: `currentModuleDir` and `mruModules` persisted to `~/.codenode/config.properties`.

## R2: Module Dropdown Implementation

**Decision**: Replace the existing gear icon + `flowGraphName` label in `TopToolbar.kt` with an `OutlinedButton` + `DropdownMenu` following the same pattern as the Code Generator panel's path selector.

**Current toolbar code** (TopToolbar.kt ~line 65-80): Shows `flowGraphName` text and `onShowProperties` callback for the gear icon.

**New dropdown**: `OutlinedButton` showing current module name with `▼` indicator. `DropdownMenu` with:
- Current module (checkmarked)
- MRU modules (up to 5)
- Divider
- "Open Module..." → directory chooser filtered to dirs with `build.gradle.kts`
- "Create New Module..." → opens Module Properties in create mode
- Divider
- "Module Settings..." → opens Module Properties in edit mode

## R3: Title Bar Update

**Decision**: Update the Compose Desktop window title to include the active flowGraph name. Currently the title is set in `Main.kt` or the `Window` composable. Change to dynamic: `"CodeNodeIO — ${activeFlowGraphName ?: ""}"`.

**When no flowGraph is open**: Title shows "CodeNodeIO".
**When flowGraph is open**: Title shows "CodeNodeIO — MainFlow".

## R4: New FlowGraph Dialog

**Decision**: Replace the current inline `flowGraph("New Graph") {}` creation with a dialog that prompts for flowGraph name only.

**Validation**:
- Name must be non-empty
- Name must not match an existing .flow.kt in the module's flow/ directory
- Name must be valid for a filename (no special characters)

**On confirm**: Creates a new FlowGraph with the entered name and sets it as active. Does NOT write to disk until Save.

## R5: Scoped Open

**Decision**: The "Open" button opens a file chooser starting in the workspace module's `flow/` directory, filtered to `.flow.kt` files. An "Open from..." option (e.g., via Shift+Click or a menu item) opens a full filesystem browser.

**When opening from a different module**: Infer the new module by walking up from the file to find `build.gradle.kts`. Switch workspace to that module. Update dropdown and MRU list.

## R6: Deterministic Save

**Decision**: Save writes to `{workspaceModuleDir}/src/commonMain/kotlin/{packagePath}/flow/{flowGraphName}.flow.kt`. The path is fully determined by the workspace module and flowGraph name — no directory chooser needed.

**When no module is loaded**: Save is disabled (button grayed out with tooltip "Open or create a module first").

**The existing `saveFlowKtOnly()` remains** but is called with the workspace-derived path instead of using the save location registry.

## R7: MRU Persistence

**Decision**: The MRU module list is stored in `~/.codenode/config.properties` as a comma-separated list of absolute paths under the key `workspace.mru`. The current workspace is stored under `workspace.current`. Maximum 5 entries.
