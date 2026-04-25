# Feature Specification: Implement Module as Workspace via Dropdown

**Feature Branch**: `083-implement-workspace-dropdown`
**Created**: 2026-04-25
**Status**: Draft
**Input**: Implement Variation D from feature 082 (ux-design.md) — Module as Workspace via Dropdown.

## Context

Feature 082 designed the UX for a many-to-one module/flowGraph relationship model with four UI variations. Variation D was selected: the module IS the workspace, the toolbar's gear icon + label becomes a module dropdown for quick workspace switching, the title bar shows the active flowGraph name, and New/Open/Save operate on flowGraphs within the workspace module.

**Design reference**: `specs/082-module-flowgraph-ux-design/ux-design.md` — Sections 1 (Module Properties dialog), 2.4 (Variation D), 3 (Relationship model), 4 (Migration notes with 11 implementation items).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Module Dropdown in Toolbar (Priority: P1)

The existing gear icon + flowGraph name label in the toolbar is replaced by a module dropdown. The dropdown shows the current workspace module name (e.g., "[StopWatch ▼]"). Clicking it reveals a menu with: the current module (checkmarked), recently used modules, "Open Module...", "Create New Module...", and "Module Settings...". Selecting a different module switches the workspace. The title bar updates to show "CodeNodeIO — {FlowGraphName}" for the active flowGraph.

**Why this priority**: The dropdown is the central UI element for the workspace model — all other changes depend on module context being accessible via this control.

**Independent Test**: Launch the graph editor. Verify the toolbar shows a module dropdown instead of the gear icon. Open a module's .flow.kt. Verify the dropdown shows the module name and the title bar shows the flowGraph name. Click the dropdown and verify the menu contents.

**Acceptance Scenarios**:

1. **Given** the toolbar, **When** inspected, **Then** the gear icon + label is replaced by a dropdown showing the current module name
2. **Given** no module loaded, **When** the dropdown is viewed, **Then** it shows "[No Module ▼]" with "Open Module..." and "Create New Module..." options
3. **Given** a module is loaded, **When** the dropdown is clicked, **Then** it shows the current module (checkmarked), MRU modules, "Open Module...", "Create New Module...", and "Module Settings..."
4. **Given** a flowGraph is open, **When** the title bar is inspected, **Then** it shows "CodeNodeIO — {FlowGraphName}"
5. **Given** a different module is selected from the dropdown, **When** it is clicked, **Then** the workspace switches to that module

---

### User Story 2 - Redesign Module Properties Dialog (Priority: P2)

The "FlowGraph Properties" dialog is renamed to "Module Properties". The name field starts empty (minimum 3 characters). Target platform checkboxes require at least 1 selected. A "Create Module" button (enabled when validation passes) opens a directory chooser and creates module scaffolding. In edit mode (module loaded), the name is read-only and the module path is displayed.

**Why this priority**: Module Properties is the creation point for new modules. It must work before the workspace model is meaningful.

**Independent Test**: Open Module Properties with no module. Enter "StopWatch", select Android + iOS. Click "Create Module". Select a directory. Verify scaffolding is created and the workspace switches to the new module.

**Acceptance Scenarios**:

1. **Given** Module Properties opens with no module, **When** inspected, **Then** the name field is empty, platforms are unchecked, and "Create Module" is disabled
2. **Given** name ≥ 3 chars and ≥ 1 platform selected, **When** "Create Module" is clicked, **Then** a directory chooser appears and module scaffolding is created
3. **Given** a module is loaded, **When** Module Properties opens, **Then** the name is read-only, platforms are pre-filled and editable, and the module path is displayed
4. **Given** Module Properties, **When** accessed via dropdown "Module Settings...", **Then** it opens in edit mode for the current workspace module

---

### User Story 3 - Redesign New/Open/Save for FlowGraphs (Priority: P3)

New, Open, and Save operate on flowGraphs within the current workspace module. "New" shows a simple name-only dialog and creates a blank flowGraph. "Open" is scoped to the workspace module's `flow/` directory (with "Open from..." fallback). "Save" writes deterministically to `{workspace}/flow/{flowGraphName}.flow.kt` — no directory prompt. All three are disabled when no module is loaded.

**Why this priority**: These are the core editing operations. They depend on the workspace model (US1) and module creation (US2) being in place.

**Independent Test**: With a module loaded, click "New" → enter "MainFlow" → blank canvas appears. Click "Save" → file written to module's flow/ directory without prompting. Click "Open" → shows only .flow.kt files from the module.

**Acceptance Scenarios**:

1. **Given** a workspace module is loaded, **When** "New" is clicked, **Then** a dialog prompts for flowGraph name only (no module selection)
2. **Given** a valid name is entered in New, **When** confirmed, **Then** a blank canvas appears and the title bar shows "CodeNodeIO — {name}"
3. **Given** a flowGraph is open, **When** "Save" is clicked, **Then** the file is written to `{workspace}/flow/{name}.flow.kt` with no directory prompt
4. **Given** no module is loaded, **When** New, Open, or Save is clicked, **Then** the button is disabled or a message indicates a module must be created/opened first
5. **Given** "Open" is clicked, **When** the file chooser appears, **Then** it is scoped to the current module's `flow/` directory
6. **Given** a user opens a .flow.kt from a different module via "Open from...", **When** the file loads, **Then** the workspace switches to that module

---

### User Story 4 - Workspace Persistence and Edge Cases (Priority: P4)

The last used workspace module is remembered across application restarts. Unsaved changes prompt when switching modules. Edge cases (no module, empty module, orphan flowGraph, duplicate names) are handled per the design document.

**Why this priority**: Polish and robustness. Core functionality works without this, but the experience degrades without persistence and edge case handling.

**Independent Test**: Open a module, close the app, reopen — same module is active. Modify a flowGraph, switch modules via dropdown — unsaved changes prompt appears.

**Acceptance Scenarios**:

1. **Given** a workspace module is active, **When** the application is closed and reopened, **Then** the same module is restored as the workspace
2. **Given** unsaved changes to the current flowGraph, **When** switching modules via dropdown, **Then** a prompt offers Save / Don't Save / Cancel
3. **Given** a duplicate flowGraph name is entered in New, **When** confirmed, **Then** an error message indicates the name already exists in this module
4. **Given** the module directory has been deleted externally, **When** Save is attempted, **Then** an error is shown and Module Properties opens for resolution

---

### Edge Cases

- What happens when no module is loaded at startup (first launch)? Module Properties opens automatically in create mode. The dropdown shows "[No Module ▼]".
- What happens when the MRU module list contains a module that no longer exists? It is removed from the list silently on next access attempt, with no error shown until the user explicitly tries to select it.
- What happens when a user tries to create a flowGraph with the same name as an existing one? The New dialog validates against existing .flow.kt files in the module's flow/ directory and shows an inline error.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The toolbar gear icon + label MUST be replaced by a module dropdown showing the current workspace module name
- **FR-002**: The dropdown MUST include: current module (checkmarked), MRU modules, "Open Module...", "Create New Module...", "Module Settings..."
- **FR-003**: The title bar MUST show "CodeNodeIO — {FlowGraphName}" for the active flowGraph, or "CodeNodeIO" when no flowGraph is open
- **FR-004**: "FlowGraph Properties" MUST be renamed to "Module Properties" throughout the UI
- **FR-005**: The Module Properties dialog MUST have an empty name field (min 3 chars), platform checkboxes (min 1), and "Create Module" button with validation gating
- **FR-006**: "New" MUST show a name-only dialog and create a blank flowGraph within the workspace module
- **FR-007**: "Open" MUST be scoped to the workspace module's `flow/` directory with an "Open from..." fallback for full filesystem browsing
- **FR-008**: "Save" MUST write deterministically to `{workspace}/flow/{flowGraphName}.flow.kt` with no directory prompt
- **FR-009**: New, Open, and Save MUST be disabled when no module is loaded
- **FR-010**: Switching modules via dropdown MUST prompt to save unsaved changes
- **FR-011**: The last used workspace MUST be persisted and restored on application restart
- **FR-012**: FlowGraph names MUST be validated for uniqueness within the module before creation
- **FR-013**: Existing .flow.kt files in non-standard locations MUST still be openable via "Open from..."

### Key Entities

- **Workspace**: The currently active module context. Determines where New creates, Open browses, Save writes, and Generate operates. Persisted across restarts.
- **Module Dropdown**: Toolbar control replacing the gear icon. Shows current workspace, MRU list, and module management actions.
- **MRU Module List**: Recently used modules maintained for quick switching. Persisted across restarts.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The toolbar shows a module dropdown — no gear icon + label visible
- **SC-002**: Save never prompts for a directory — 100% deterministic file writing
- **SC-003**: The New dialog has exactly 1 field (flowGraph name) — no module selection
- **SC-004**: Module context is always visible via the dropdown — users can identify their workspace at a glance
- **SC-005**: Workspace persists across restarts — reopening the app restores the previous module
- **SC-006**: All existing demo project modules (StopWatch, UserProfiles, Addresses, EdgeArtFilter, WeatherForecast) work correctly with the new workspace model

## Assumptions

- The workspace model replaces the save location registry — module root directory is the single source of truth
- `saveFlowKtOnly()` in ModuleSaveService is updated to write to the workspace module's `flow/` directory
- ModuleScaffoldingGenerator (feature 078) handles module creation
- The MRU module list is persisted to `~/.codenode/config.properties` alongside the subscription tier
- The "Open from..." fallback infers the module from the opened file's directory (walks up to find `build.gradle.kts`)
- Feature 082's ux-design.md Section 2.4 (Variation D) is the authoritative design reference
