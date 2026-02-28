# Feature Specification: Refine the ViewModel Binding

**Feature Branch**: `034-refine-viewmodel-binding`
**Created**: 2026-02-28
**Status**: Draft
**Input**: User description: "Refine the ViewModel binding — refactor the code-generated ViewModel class from a wrapper around the flowGraph controller to a binding interface between composable UI views and flowGraph nodes. Move ViewModel out of generated/ to become a stub file. Eliminate per-node stateProperties files. Consolidate observable state into the ViewModel."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Rename Existing StopWatch to StopWatchV2 (Priority: P1)

As a developer, I want to preserve the current working StopWatch module by renaming it to StopWatchV2, so that I have a reference implementation to compare against while refactoring the code generation approach.

**Why this priority**: This is the safety net. Without a preserved copy of the working prototype, there is no baseline to validate the refactored output against. Must complete first.

**Independent Test**: Rename the StopWatch module to StopWatchV2, update all internal references (package names, class names, imports), and verify the KMPMobileApp continues to function identically using StopWatchV2.

**Acceptance Scenarios**:

1. **Given** the existing StopWatch module, **When** it is renamed to StopWatchV2 (module directory, package declarations, class names, import references in KMPMobileApp), **Then** the project compiles successfully and StopWatchV2 behaves identically to the original StopWatch.
2. **Given** the renamed StopWatchV2 module, **When** the KMPMobileApp is launched, **Then** the stopwatch UI displays, starts/stops/pauses/resets correctly with the same behavior as before the rename.
3. **Given** the renamed StopWatchV2 module, **When** searching the codebase for references to the old "StopWatch" name (excluding the new StopWatch module created in US2), **Then** no stale references to the original StopWatch package or class names remain.

---

### User Story 2 - Refactor Code Generation for ViewModel as Binding Interface (Priority: P2)

As a developer, I want the code generator to produce a ViewModel that acts as the binding interface between composable UI views and the flowGraph's nodes, so that generator nodes bind user interaction methods to input channels and sink nodes bind reactive display properties to output channels — all in one cohesive ViewModel stub file.

**Why this priority**: This is the core refactoring. It changes the code generation output to produce the new ViewModel structure, eliminating the stateProperties folder and consolidating observable state into the ViewModel itself.

**Independent Test**: Using the same .flow.kt definition file, generate a new StopWatch module with the refactored code generation. Verify the generated ViewModel file is a stub (outside generated/), contains module properties derived from sink node input ports, and provides user interaction methods derived from generator node output ports. Verify no stateProperties folder is generated.

**Acceptance Scenarios**:

1. **Given** the StopWatch .flow.kt definition, **When** code generation runs with the refactored generators, **Then** the {moduleName}ViewModel file is created one level above the generated/ folder (in the base package directory) as a stub file intended for user modification.
2. **Given** the refactored code generation, **When** generating the StopWatch module, **Then** the ViewModel file contains a delineated "Module Properties" section with observable state properties derived from sink node input ports, using the port names as property names.
3. **Given** the refactored code generation, **When** generating the StopWatch module, **Then** no stateProperties/ folder or per-node StateProperties files are created.
4. **Given** the refactored code generation, **When** generating the StopWatch module, **Then** the ViewModel exposes user interaction methods (start, stop, pause, resume, reset) that bind to the flowGraph's control interface, and the generated runtime files (Flow, Controller) reference the ViewModel's properties instead of separate StateProperties objects.
5. **Given** the ViewModel stub file already exists with user-written code outside the Module Properties markers, **When** code generation runs again, **Then** the Module Properties section between markers is regenerated with current port definitions while all user code outside the markers is preserved.
6. **Given** the refactored code generation, **When** reviewing the codebase, **Then** the StatePropertiesGenerator class, its test file, and all stateProperties generation/orchestration logic in ModuleSaveService and related tests have been removed.

---

### User Story 3 - Validate KMPMobileApp Equivalence (Priority: P3)

As a developer, I want to verify that the KMPMobileApp has equivalent functionality when using the new refactored StopWatch module, so that I can confirm the refactoring produces correct, working code.

**Why this priority**: This is the validation step. It confirms the refactored output is functionally equivalent to the original, completing the proof that the new code generation approach works.

**Independent Test**: Update KMPMobileApp to use the new StopWatch module (with the refactored ViewModel binding), build and run the app, and verify the stopwatch UI displays and operates identically to the StopWatchV2 baseline.

**Acceptance Scenarios**:

1. **Given** the new StopWatch module generated with the refactored code generation, **When** the KMPMobileApp is updated to import from the new StopWatch module, **Then** the project compiles successfully.
2. **Given** the KMPMobileApp using the new StopWatch module, **When** the app is launched, **Then** the stopwatch displays seconds and minutes, and Start/Stop/Pause/Resume/Reset controls function identically to the StopWatchV2 baseline.
3. **Given** both StopWatchV2 (original approach) and StopWatch (refactored approach) modules exist, **When** comparing their runtime behavior, **Then** both produce identical user-facing results from the same .flow.kt definition.

---

### Edge Cases

- What happens when a sink node has zero input ports? The ViewModel should have no observable properties for that node.
- What happens when a generator node has zero output ports? The ViewModel should have no user-driven data methods for that node (only control methods like start/stop remain).
- What happens when the ViewModel stub file is manually edited and code generation runs again? User code outside the Module Properties markers is preserved. The Module Properties section (between markers) is regenerated to reflect current port definitions.
- What happens when a node is removed from the .flow.kt? The Module Properties section is regenerated without that node's properties. User code referencing removed properties will produce compile errors, alerting the developer.
- What happens when port types change between regenerations? The Module Properties section is regenerated with updated types. User code referencing old types will produce compile errors.
- What happens when the user removes the Module Properties marker comments? The system treats the file as if markers are missing and generates a fresh Module Properties section (appended or replacing the full file with markers restored).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST rename the existing StopWatch module to StopWatchV2, including all package declarations, class names, file names, and import references across the codebase.
- **FR-002**: The system MUST generate the {moduleName}ViewModel file in the base package directory (one level above generated/) rather than inside the generated/ folder.
- **FR-003**: The {moduleName}ViewModel file MUST use selective regeneration — the Module Properties section (delineated by marker comments) is regenerated on each save to reflect current port definitions, while all user-written code outside the markers is preserved.
- **FR-004**: The ViewModel MUST contain a marker-delineated "Module Properties" section with observable state properties (MutableStateFlow/StateFlow pairs) derived from sink node input ports, using the port names as property names. The markers MUST clearly indicate that the section is auto-generated and will be overwritten.
- **FR-005**: The ViewModel MUST act as the binding interface between composable UI views and the flowGraph, with generator nodes binding user interaction methods to input channels and sink nodes binding reactive composable display to output channels.
- **FR-006**: The code generator MUST NOT produce a stateProperties/ folder or per-node {nodeName}StateProperties files.
- **FR-007**: The generated runtime files (Flow, Controller) MUST reference the ViewModel's consolidated properties instead of separate StateProperties objects for observable state management.
- **FR-011**: The StatePropertiesGenerator class and its dedicated test file MUST be removed from the codebase, since stateProperties files are no longer generated.
- **FR-012**: All references to StatePropertiesGenerator and stateProperties generation logic MUST be removed from the ModuleSaveService and its tests, as well as from any other generator or test files that reference the eliminated stateProperties pattern.
- **FR-008**: The port type for observable properties MUST be derived from the IP types available in the graphEditor, enabling type selection from the IP type palette.
- **FR-009**: The KMPMobileApp MUST compile and function equivalently using the refactored StopWatch module as it did with the original (now StopWatchV2) module.
- **FR-010**: The system MUST support both StopWatchV2 (preserved original) and StopWatch (refactored) modules coexisting in the project for validation purposes.

### Key Entities

- **ViewModel Stub**: The {moduleName}ViewModel file that lives outside generated/, contains module properties derived from node ports, and is intended for user modification after initial generation.
- **Module Properties Section**: A delineated region within the ViewModel stub that holds observable state properties (StateFlow pairs) derived from sink node input ports.
- **Observable State Property**: A MutableStateFlow/StateFlow pair in the ViewModel, named after a sink node's input port, representing data flowing from the flowGraph to the UI.

## Assumptions

- The .flow.kt file format and FlowGraph DSL remain unchanged — only the code generation output changes.
- The existing processingLogic stub preservation principle (extract user code, regenerate boilerplate) informs the ViewModel selective regeneration approach, but uses marker comments instead of lambda body extraction.
- The Controller, ControllerInterface, and ControllerAdapter generated files continue to exist in the generated/ folder; only the ViewModel moves out.
- Port names from sink nodes are sufficiently descriptive to serve as ViewModel property names without transformation (beyond standard camelCase conventions).
- The refactored ViewModel will still extend `androidx.lifecycle.ViewModel` for lifecycle awareness.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The StopWatchV2 module compiles and runs with identical behavior to the original StopWatch module — all control operations (start, stop, pause, resume, reset) and display updates (seconds, minutes) work correctly.
- **SC-002**: The new StopWatch module, generated with the refactored code generation, compiles successfully with the ViewModel as a stub file outside of generated/.
- **SC-003**: No stateProperties/ folder or per-node StateProperties files exist in the newly generated StopWatch module.
- **SC-004**: The KMPMobileApp functions equivalently with both the StopWatchV2 (original) and StopWatch (refactored) modules — same user experience, same control behavior, same display output.
- **SC-005**: On a second code generation run, user-written code outside the Module Properties markers in the ViewModel stub is preserved, while the Module Properties section is correctly regenerated with current port definitions.
- **SC-006**: The StatePropertiesGenerator class and its test file no longer exist in the codebase, and no remaining code references them — all stateProperties generation logic and tests are fully removed.
