# Feature Specification: StopWatch App and Module Refactoring

**Feature Branch**: `030-stopwatch-module-refactor`
**Created**: 2026-02-25
**Status**: Draft
**Input**: User description: "Rename StopWatch module as StopWatchOriginal, rename StopWatch3 module as StopWatch, add userInterface folder, move UI files from KMPMobileApp to StopWatch module, ensure KMPMobileApp operates as before."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Rename StopWatch Modules (Priority: P1)

As a developer, I want the legacy StopWatch module renamed to StopWatchOriginal and the newer StopWatch3 module renamed to StopWatch, so that the project naming reflects the current active module and preserves the original for reference.

**Why this priority**: Module renaming is the foundational step that all other changes depend on. Without correct names, the build system references and module dependencies cannot be updated.

**Independent Test**: After renaming, the project builds successfully with the new module names, and both modules compile independently.

**Acceptance Scenarios**:

1. **Given** the existing StopWatch module, **When** the refactoring is applied, **Then** the module is renamed to StopWatchOriginal (directory, settings, and build configuration).
2. **Given** the existing StopWatch3 module, **When** the refactoring is applied, **Then** the module is renamed to StopWatch (directory, settings, and build configuration).
3. **Given** the root project settings, **When** the module names change, **Then** the root settings file references both StopWatchOriginal and StopWatch modules correctly.

---

### User Story 2 - Move UI Files to StopWatch Module (Priority: P1)

As a developer, I want the StopWatch and StopWatchFace UI composable files moved from KMPMobileApp into a new userInterface folder within the newly renamed StopWatch module, so that the StopWatch module is self-contained with both its runtime logic and its user interface.

**Why this priority**: Moving the UI files completes the module's self-containment and is required before the KMPMobileApp can be updated to reference the new location.

**Independent Test**: The StopWatch and StopWatchFace composables exist in the StopWatch module's userInterface folder, compile successfully, and can render the stopwatch UI.

**Acceptance Scenarios**:

1. **Given** StopWatch.kt and StopWatchFace.kt in KMPMobileApp, **When** the refactoring is applied, **Then** both files reside in the StopWatch module under a userInterface folder.
2. **Given** the moved files, **When** the StopWatch module is compiled, **Then** the UI composables compile without errors alongside the existing generated runtime code.
3. **Given** the moved files, **When** the files are examined, **Then** they no longer exist in KMPMobileApp's source directory.

---

### User Story 3 - KMPMobileApp Continues to Function (Priority: P1)

As a developer, I want the KMPMobileApp to continue operating exactly as before after the refactoring, so that the mobile application's behavior and user experience are unchanged.

**Why this priority**: Preserving the app's behavior is the ultimate validation that the refactoring is correct. The app must work identically for end users.

**Independent Test**: Launch the KMPMobileApp after all changes and verify the stopwatch starts, pauses, resumes, resets, and displays elapsed time correctly on the analog clock face.

**Acceptance Scenarios**:

1. **Given** the refactored project, **When** the KMPMobileApp is built, **Then** the build completes without errors.
2. **Given** the refactored KMPMobileApp, **When** the app is launched, **Then** the stopwatch UI renders with the analog clock face and control buttons.
3. **Given** the running KMPMobileApp, **When** the user starts the stopwatch, **Then** the seconds and minutes hands move and the digital display updates, identical to pre-refactoring behavior.
4. **Given** the KMPMobileApp depends on the renamed StopWatch module, **When** imports and references are examined, **Then** all references point to the correct new module and package locations.

---

### Edge Cases

- What happens if files in the StopWatch module have the same name as files being moved from KMPMobileApp? The files being moved (StopWatch.kt, StopWatchFace.kt) should go into a distinct userInterface folder, avoiding any naming conflicts.
- What happens to the StopWatchPreview composable in KMPMobileApp's Android-specific source set? It references StopWatch module types and must be updated to reference the newly renamed module's types.
- What happens to existing tests (e.g., StopWatchIntegrationTest in KMPMobileApp)? They must be updated to reference the renamed module's types and continue to pass.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The existing StopWatch module directory MUST be renamed to StopWatchOriginal, including its settings name and all build references.
- **FR-002**: The existing StopWatch3 module directory MUST be renamed to StopWatch, including its settings name and all build references.
- **FR-003**: The root project settings MUST include both StopWatchOriginal and the newly renamed StopWatch modules.
- **FR-004**: A new userInterface folder MUST be created within the StopWatch module's source structure.
- **FR-005**: The StopWatch.kt and StopWatchFace.kt composable files MUST be moved from KMPMobileApp into the StopWatch module's userInterface folder.
- **FR-006**: The KMPMobileApp MUST depend on the newly renamed StopWatch module (formerly StopWatch3) instead of the old StopWatch module.
- **FR-007**: All import statements and module references in KMPMobileApp MUST be updated to reflect the new module names and package paths.
- **FR-008**: The KMPMobileApp MUST build and operate identically to its pre-refactoring behavior.
- **FR-009**: The StopWatchOriginal module MUST remain compilable as-is, preserving it as a reference.
- **FR-010**: The StopWatch module (formerly StopWatch3) MUST compile successfully with the added userInterface files.

### Key Entities

- **StopWatchOriginal Module**: The preserved legacy module (formerly StopWatch). Contains the original runtime implementation using custom component classes and manual channel wiring.
- **StopWatch Module**: The active module (formerly StopWatch3). Contains the newer generation pattern with ProcessLogic functions, StateProperties, and CodeNodeFactory. After refactoring, also contains the userInterface composables.
- **KMPMobileApp**: The mobile application that consumes the StopWatch module. Must be updated to reference the renamed module while preserving identical end-user behavior.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All project modules build successfully after refactoring (0 compilation errors across StopWatchOriginal, StopWatch, and KMPMobileApp).
- **SC-002**: All existing tests pass after refactoring (100% test pass rate).
- **SC-003**: The KMPMobileApp stopwatch functionality works identically to pre-refactoring (start, stop, pause, resume, reset operations all function correctly).
- **SC-004**: The StopWatch module contains both runtime code and UI composables in their respective folders (generated, processingLogic, stateProperties, userInterface).

## Assumptions

- The StopWatch3 module is the intended replacement for the legacy StopWatch module going forward.
- The StopWatchOriginal module does not need to remain in the active build path but should remain compilable for reference.
- The userInterface folder follows the same source set convention as the existing generated, processingLogic, and stateProperties folders within the StopWatch module.
- Package names within the moved files will be updated to reflect their new module location.
- The StopWatchPreview.kt in KMPMobileApp's Android source set will also need updating to reference the new module's types.

## Scope

### In Scope

- Renaming StopWatch module to StopWatchOriginal
- Renaming StopWatch3 module to StopWatch
- Creating userInterface folder in the StopWatch module
- Moving StopWatch.kt and StopWatchFace.kt from KMPMobileApp to StopWatch/userInterface
- Updating all imports, dependencies, and references in KMPMobileApp
- Updating root settings.gradle.kts
- Ensuring all modules compile and tests pass

### Out of Scope

- Changing the StopWatch runtime behavior or logic
- Adding new features to the StopWatch app
- Modifying the StopWatchOriginal module's internal code
- Changing the KMPMobileApp's visual appearance or user interaction patterns
