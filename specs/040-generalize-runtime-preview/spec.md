# Feature Specification: Generalize Runtime Preview

**Feature Branch**: `040-generalize-runtime-preview`
**Created**: 2026-03-06
**Status**: Draft
**Input**: User description: "The Runtime Preview is currently hardcoded to only render StopWatch composables. There's no UserProfilesPreviewProvider and no way to dynamically render module-specific previews. The RuntimeSession also appears to be StopWatch-specific — its viewModel property exposes StopWatch state (seconds, minutes, executionState). Now let's generalize this to support whatever module is loaded."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Module-Agnostic Runtime Session (Priority: P1)

As a developer using the graphEditor, when I load any module (StopWatch, UserProfiles, or future modules), the Runtime Preview panel should automatically initialize a runtime session appropriate for that module. The runtime session should no longer be hardcoded to StopWatch — it should dynamically create the correct controller, adapter, and state bindings based on the loaded module.

**Why this priority**: Without a module-agnostic runtime session, no other preview functionality can work for non-StopWatch modules. This is the foundational change that unblocks all other user stories.

**Independent Test**: Load the StopWatch module in the graphEditor — runtime preview continues to function exactly as before (start, stop, pause, resume, attenuation). Load the UserProfiles module — runtime session initializes without errors and execution controls (start/stop/pause/resume) function correctly.

**Acceptance Scenarios**:

1. **Given** the graphEditor with StopWatch module loaded, **When** the user opens the Runtime Preview panel, **Then** the runtime session initializes with StopWatch-specific state and all execution controls work as before.
2. **Given** the graphEditor with UserProfiles module loaded, **When** the user opens the Runtime Preview panel, **Then** the runtime session initializes with UserProfiles-specific state and execution controls (start, stop, pause, resume, attenuation) work correctly.
3. **Given** the graphEditor with any module loaded, **When** the user switches to a different module, **Then** the previous runtime session is cleanly stopped and a new session is initialized for the newly loaded module.

---

### User Story 2 - Dynamic Preview Rendering (Priority: P1)

As a developer using the graphEditor, when I select a composable from the preview dropdown, the Runtime Preview panel should render it using the currently loaded module's state — not just StopWatch composables. The dropdown already discovers composable files from the module's `userInterface/` directory; now selecting any of them should render a live preview driven by the module's runtime state.

**Why this priority**: This is the core visual payoff — users need to see their module's UI rendered live. Without this, the preview panel shows "Preview not available" for every non-StopWatch module.

**Independent Test**: Load UserProfiles module, select "UserProfiles" from the composable dropdown, verify a live preview of the UserProfiles screen renders showing current module state (e.g., empty profile list, Add/Update/Remove buttons).

**Acceptance Scenarios**:

1. **Given** the StopWatch module loaded and "StopWatch" selected in the dropdown, **When** the runtime is started, **Then** the StopWatch clock face renders and updates in real time.
2. **Given** the UserProfiles module loaded and "UserProfiles" selected in the dropdown, **When** the runtime is started, **Then** the UserProfiles screen renders showing the current profile list and action buttons.
3. **Given** any module loaded, **When** the user selects a composable from the dropdown, **Then** the preview area renders that composable using the module's live runtime state — no "Preview not available" message appears.

---

### User Story 3 - Backward Compatibility for StopWatch (Priority: P2)

As a developer who currently relies on the StopWatch runtime preview, the generalization must not break any existing StopWatch preview functionality. The StopWatch preview (clock face, digital display, screen with controls) must continue to work exactly as it does today after the refactoring.

**Why this priority**: Ensuring no regression in existing functionality is essential, but it is inherently addressed by User Story 1. This story exists to explicitly validate backward compatibility.

**Independent Test**: Load StopWatch module, start runtime, verify clock ticks, pause/resume works, attenuation slider changes tick speed, both "StopWatch" and "StopWatchScreen" composable previews render correctly.

**Acceptance Scenarios**:

1. **Given** the StopWatch module loaded, **When** the user starts the runtime and selects "StopWatch" preview, **Then** the clock face renders and ticks at the configured attenuation rate — identical behavior to the current implementation.
2. **Given** the StopWatch module loaded, **When** the user selects "StopWatchScreen" preview, **Then** the full screen preview (clock + controls) renders correctly.

---

### Edge Cases

- What happens when a module has no composables in its `userInterface/` directory? The dropdown should show "No composables found" and the preview area should display "No preview available" — same as current behavior.
- What happens when the runtime session fails to initialize for a module (e.g., missing controller or flow)? The preview panel should display a clear error message and execution controls should remain disabled.
- What happens when the user loads a module, starts the runtime, then switches to a different module? The previous runtime must be stopped before the new one initializes to prevent resource leaks.
- What happens when a module's composable depends on state that hasn't been populated yet (e.g., empty profile list)? The composable should render its default/empty state gracefully.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The runtime session MUST be able to initialize with any module's controller and flow, not just StopWatch.
- **FR-002**: The runtime session MUST expose module-specific state (ViewModel) in a way that preview composables can consume it, regardless of the specific module loaded.
- **FR-003**: The Runtime Preview panel MUST dynamically render any composable discovered from the loaded module's `userInterface/` directory — not just hardcoded StopWatch composables.
- **FR-004**: Each module MUST be able to provide its own preview rendering logic that maps its ViewModel state to its composable functions.
- **FR-005**: Execution controls (start, stop, pause, resume, attenuation) MUST work uniformly across all modules.
- **FR-006**: The system MUST cleanly stop and release resources from the current runtime session before initializing a new one when the module changes.
- **FR-007**: StopWatch preview behavior MUST remain identical after the generalization — no regressions in existing functionality.
- **FR-008**: When a module has no preview provider or no composables, the panel MUST display an appropriate informational message rather than crashing.

### Key Entities

- **Runtime Session**: The orchestrator that manages flow execution lifecycle (start/stop/pause/resume), attenuation, and ViewModel binding for a loaded module. Currently StopWatch-specific; must become module-agnostic.
- **Preview Provider**: A module-specific component that knows how to render that module's composables given the module's ViewModel state. Each module supplies its own preview provider.
- **Module Controller**: The generated controller for a module's FlowGraph (e.g., StopWatchController, UserProfilesController). The runtime session must work with any module's controller.
- **Module ViewModel**: The ViewModel exposing observable state for a module (e.g., StopWatchViewModel, UserProfilesViewModel). Preview providers consume the ViewModel to render live previews.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of existing StopWatch preview functionality works identically after the refactoring — no regressions.
- **SC-002**: The UserProfiles module preview renders correctly in the graphEditor Runtime Preview panel, demonstrating that at least one non-StopWatch module works end-to-end.
- **SC-003**: Adding preview support for a new module requires only creating a module-specific preview provider — no changes to the RuntimeSession, RuntimePreviewPanel, or core preview infrastructure.
- **SC-004**: Switching between modules in the graphEditor cleanly transitions the runtime session within 1 second, with no resource leaks or stale state from the previous module.

## Assumptions

- Each module already has a generated Controller, ControllerAdapter, ControllerInterface, and ViewModel (from the existing code generation pipeline).
- The composable discovery mechanism (`discoverComposables()`) already works correctly for all modules and does not need changes.
- Module preview providers will be created in the graphEditor project (not in the modules themselves), since the graphEditor is a desktop-only tool and modules target multiple platforms.
- The attenuation control behavior is uniform across all modules — no module needs custom attenuation logic.

## Scope

### In Scope

- Making RuntimeSession module-agnostic (parameterized by module)
- Creating a preview provider for UserProfiles (proving the pattern works)
- Refactoring RuntimePreviewPanel to dynamically dispatch to the correct preview provider
- Maintaining full backward compatibility with StopWatch previews

### Out of Scope

- Auto-generating preview providers from module metadata (future enhancement)
- Adding new preview panel features (e.g., interactive controls within the preview)
- Modifying the composable discovery mechanism
- Supporting modules that are not yet built or generated
