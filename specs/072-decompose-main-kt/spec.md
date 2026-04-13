# Feature Specification: Decompose graphEditor Main.kt

**Feature Branch**: `072-decompose-main-kt`
**Created**: 2026-04-13
**Status**: Draft
**Input**: User description: "Decompose graphEditor's Main.kt — the file is almost 2000 lines long and does not follow clean code guidelines. Break it up into logical pieces. Composable functions should be their own files in the /ui folder. The GraphEditorApp composable is nearly 1500 lines and should be broken into logical subfunctions. The refactored Main.kt should simply contain the main() function. Use TDD to confirm refactoring preserves original functionality."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Extract Top-Level Composables and Utilities to Separate Files (Priority: P1)

A developer working on the graph editor needs to locate and modify UI components without navigating a 2000-line monolithic file. Each top-level composable function (TopToolbar, StatusBar, GraphEditorAppPreview) is extracted to its own file in the /ui folder. Utility functions (file dialogs, module root resolution, connection IP type resolution) are extracted to appropriate utility files.

**Why this priority**: This is the simplest, lowest-risk extraction — moving already self-contained functions to their own files with no logic changes required. It establishes the pattern for subsequent decomposition.

**Independent Test**: After extraction, the application compiles and all existing tests pass. Each extracted file contains exactly the same function signature and body as before.

**Acceptance Scenarios**:

1. **Given** `TopToolbar` composable exists in Main.kt, **When** extraction is complete, **Then** `TopToolbar` resides in its own file in the /ui folder with identical signature and behavior
2. **Given** `StatusBar` composable exists in Main.kt, **When** extraction is complete, **Then** `StatusBar` resides in its own file in the /ui folder with identical signature and behavior
3. **Given** utility functions (`showFileOpenDialog`, `showDirectoryChooser`, `resolveFlowKtFromModule`, `findModuleRoot`, `resolveConnectionIPTypes`) exist in Main.kt, **When** extraction is complete, **Then** each utility resides in an appropriate file outside Main.kt
4. **Given** all extractions are complete, **When** the full test suite runs, **Then** all existing tests pass with zero failures

---

### User Story 2 - Decompose GraphEditorApp into Logical Subfunctions (Priority: P2)

A developer needs to understand and modify specific aspects of the graph editor (e.g., keyboard handling, file operations, panel layout) without reading through a 1500-line composable. The `GraphEditorApp` function is decomposed into well-named subfunctions that each handle one logical concern.

**Why this priority**: This is the high-impact refactoring that addresses the core problem — the massive GraphEditorApp function. It depends on US1 being complete so the file is already smaller and the pattern is established.

**Independent Test**: After decomposition, the application compiles, all existing tests pass, and the editor opens, loads files, and responds to user interactions identically to before.

**Acceptance Scenarios**:

1. **Given** `GraphEditorApp` contains initialization logic (state creation, registry setup), **When** decomposition is complete, **Then** initialization is grouped into a clearly named subfunction or section
2. **Given** `GraphEditorApp` contains keyboard shortcut handling, **When** decomposition is complete, **Then** keyboard handling is extracted to its own composable or handler function
3. **Given** `GraphEditorApp` contains file open/save dialog orchestration, **When** decomposition is complete, **Then** file operations are extracted to their own composable or handler
4. **Given** `GraphEditorApp` contains the main layout (panels, canvas, toolbar, status bar), **When** decomposition is complete, **Then** the top-level layout is readable as a high-level composition of named subfunctions
5. **Given** all decomposition is complete, **When** the full test suite runs, **Then** all existing tests pass with zero failures

---

### User Story 3 - Reduce Main.kt to Entry Point Only (Priority: P3)

A developer opening Main.kt expects to see only the application entry point. After all extractions and decomposition, Main.kt contains only the `main()` function and its direct dependencies.

**Why this priority**: This is the final cleanup step that delivers the stated goal. It depends on US1 and US2 being complete.

**Independent Test**: Main.kt contains fewer than 50 lines. The application launches correctly from the `main()` function.

**Acceptance Scenarios**:

1. **Given** US1 and US2 are complete, **When** Main.kt is reviewed, **Then** it contains only the `main()` function and necessary imports
2. **Given** the refactored Main.kt, **When** the application is launched, **Then** the graph editor opens and functions identically to before the refactoring
3. **Given** the refactored Main.kt, **When** line count is measured, **Then** it is fewer than 50 lines

---

### Edge Cases

- What happens if extracted composables reference `remember` state defined in the parent scope? State must be passed as parameters or lifted appropriately.
- What happens if private utility functions are referenced from multiple new files? Visibility must be changed to `internal` or the function must be placed in a shared utility location.
- What happens if the extraction changes the order of composable recomposition? The layout structure must be preserved exactly.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The refactoring MUST NOT change any user-visible behavior of the graph editor
- **FR-002**: Each extracted composable MUST reside in its own file within the appropriate package directory
- **FR-003**: The `GraphEditorApp` composable MUST be decomposed into subfunctions where each handles a single logical concern
- **FR-004**: The final Main.kt MUST contain only the `main()` function and minimal boilerplate
- **FR-005**: All existing tests MUST continue to pass after each extraction step
- **FR-006**: Extracted functions MUST maintain the same public/internal API signatures — callers outside the module must not need changes
- **FR-007**: The refactoring MUST be verified using TDD — compilation and test suite passing confirms behavioral equivalence at each step

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Main.kt is reduced from ~1920 lines to fewer than 50 lines
- **SC-002**: No single source file in the graphEditor module exceeds 500 lines after decomposition
- **SC-003**: 100% of existing tests pass after refactoring with zero modifications to test assertions
- **SC-004**: The graph editor application launches and functions identically to the pre-refactoring state
- **SC-005**: Each new file contains exactly one primary composable or one cohesive group of related utility functions

### Assumptions

- The existing test suite provides sufficient coverage to detect behavioral regressions from the refactoring
- Composable functions can be called across files within the same module without restriction
- The `GraphEditorApp` composable's internal state can be passed to subfunctions via parameters without changing behavior
- No external modules depend on the internal structure of Main.kt (only the `main()` entry point and `GraphEditorApp` composable are public API)
