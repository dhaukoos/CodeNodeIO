# Feature Specification: Wire Code Generator Panel

**Feature Branch**: `081-wire-codegen-panel`
**Created**: 2026-04-23
**Status**: Draft
**Input**: User description: "Connect the Code Generator panel's file-tree selections and Generate button to the code generation flow graphs, replacing the toolbar buttons."

## Context

This is Step 5 of the Code Generation Migration Plan from feature 076. The Code Generator panel (feature 076) is currently a functional UI prototype — path selection, input selection, and file-tree checkboxes all work, but the "Generate" button is disabled. The CodeGenerationRunner (feature 080) can execute generation flow graphs with selective inclusion/exclusion. This feature connects them: pressing "Generate" in the panel triggers the runner, writes the results to disk, and reports the outcome. The old toolbar buttons ("Generate Module", "Generate UI-FBP") are removed.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Wire Generate Button to Runner (Priority: P1)

A developer opens the Code Generator panel, selects a generation path (Generate Module, Repository, or UI-FBP), provides the primary input, optionally deselects files in the file tree, and clicks "Generate". The system executes the CodeGenerationRunner with the selected configuration, writes the generated file contents to the appropriate locations in the module directory, and reports the results via a status message (e.g., "Generated 7 files for StopWatch"). If any generators fail, the error is reported alongside the successes.

**Why this priority**: This is the core deliverable — making the Generate button functional completes the end-to-end pipeline from UI selection to file output.

**Independent Test**: Select "Generate Module" path in the Code Generator panel. Click "Generate". Verify files are written to the correct module directory. Verify the status message reports the correct file count.

**Acceptance Scenarios**:

1. **Given** the user selects "Generate Module" path with the current flow graph, **When** they click "Generate" with all files selected, **Then** 7 files are written to the module directory and a success status message is displayed
2. **Given** the user selects "Repository" path and picks an IP Type, **When** they click "Generate", **Then** the repository module files are generated matching the current "Create Repository Module" behavior
3. **Given** the user selects "UI-FBP" path and picks a UI file, **When** they click "Generate", **Then** the UI-FBP interface files are generated matching the current "Generate UI-FBP" behavior
4. **Given** the user deselects "Controller.kt" in the file tree, **When** they click "Generate", **Then** the Controller file is not generated but all other selected files are
5. **Given** a generation error occurs for one generator, **When** the user clicks "Generate", **Then** the status message reports both the successful files and the error

---

### User Story 2 - Remove Old Toolbar Buttons (Priority: P2)

The "Generate Module" and "Generate UI-FBP" toolbar buttons are removed from the top toolbar. All code generation is now exclusively accessed through the Code Generator panel on the left side. The "Save" button remains — it continues to write only the .flow.kt file.

**Why this priority**: Removing the old buttons after the panel is wired prevents confusion from having two ways to trigger generation. This is a cleanup step that depends on US1 working correctly.

**Independent Test**: Launch the graph editor. Verify "Generate Module" and "Generate UI-FBP" buttons are absent from the toolbar. Verify "Save" button still works. Verify the Code Generator panel is the only way to trigger code generation.

**Acceptance Scenarios**:

1. **Given** the graph editor toolbar, **When** inspected, **Then** "Generate Module" and "Generate UI-FBP" buttons are not present
2. **Given** the "Save" button, **When** clicked, **Then** it continues to write only the .flow.kt file (unchanged behavior)
3. **Given** the Code Generator panel, **When** the user generates files, **Then** the output is identical to what the old toolbar buttons would have produced for the same input

---

### User Story 3 - Deprecate ModuleSaveService Direct Calls (Priority: P3)

Direct calls to `ModuleSaveService.saveModule()` and `saveEntityModule()` from the graph editor UI are replaced by calls through the CodeGenerationRunner. The ModuleSaveService methods are marked as deprecated but not removed — they may still be needed for backward compatibility or testing. The new generation path is: Code Generator panel → CodeGenerationRunner → individual generators → file writing.

**Why this priority**: This is the final architectural cleanup — ensuring the new pipeline is the canonical path for all generation. ModuleSaveService becomes a legacy adapter.

**Independent Test**: Search the graph editor codebase for direct calls to `saveModule()` or `saveEntityModule()`. Verify they are routed through the CodeGenerationRunner pipeline.

**Acceptance Scenarios**:

1. **Given** the graph editor codebase, **When** searching for `moduleSaveService.saveModule()` calls, **Then** they are no longer invoked directly from UI code — generation goes through CodeGenerationRunner
2. **Given** `ModuleSaveService.saveModule()`, **When** inspected, **Then** it is marked as deprecated with a message directing to CodeGenerationRunner
3. **Given** the new generation pipeline, **When** generating a module, **Then** the output files are written by a file-writing layer that receives content from the runner (not by ModuleSaveService)

---

### Edge Cases

- What happens when the user clicks "Generate" without selecting a primary input (e.g., no IP Type for Repository path)? The button remains disabled or a validation message indicates the input is required.
- What happens when the output directory doesn't exist? The scaffolding generator (feature 078) creates it as part of the generation process.
- What happens when generated files already exist? They are overwritten (same behavior as current generation — always-overwrite for generated files, write-once for Gradle files and UI stubs).
- What happens when the user is on Free tier? The Code Generator panel is not visible (feature 074 tier gating), so the Generate button is inaccessible.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The "Generate" button in the Code Generator panel MUST be functional — clicking it triggers code generation via the CodeGenerationRunner
- **FR-002**: The generation MUST use the selected generation path, primary input, and file-tree checkbox selections to configure the runner
- **FR-003**: Generated file contents from the runner MUST be written to the correct locations in the module directory structure (following the folder hierarchy from feature 077)
- **FR-004**: The system MUST report generation results via a status message including the number of files generated and any errors
- **FR-005**: The "Generate Module" and "Generate UI-FBP" toolbar buttons MUST be removed from the top toolbar
- **FR-006**: The "Save" button MUST continue to work unchanged — writing only the .flow.kt file
- **FR-007**: Direct calls to `ModuleSaveService.saveModule()` and `saveEntityModule()` from UI code MUST be replaced by CodeGenerationRunner-based generation
- **FR-008**: `ModuleSaveService.saveModule()` and `saveEntityModule()` MUST be marked as deprecated
- **FR-009**: The Code Generator panel's output MUST be identical to the old toolbar buttons' output for the same input — backward compatibility verified by comparison
- **FR-010**: File-tree deselections MUST result in the corresponding files not being generated (selective generation)

### Key Entities

- **GenerateAction**: The end-to-end flow triggered by clicking "Generate" — reads panel state, builds GenerationConfig + SelectionFilter, runs CodeGenerationRunner, writes files, reports results.
- **FileWriter**: The component that takes GenerationResult content and writes files to the module directory, using ModuleScaffoldingGenerator for directory/Gradle creation and direct file writes for generated content.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Clicking "Generate" in the Code Generator panel produces the correct files in under 5 seconds for any generation path
- **SC-002**: The old "Generate Module" and "Generate UI-FBP" toolbar buttons are completely removed — zero references in toolbar code
- **SC-003**: Generated output from the panel matches the output from the old toolbar buttons for the same input — verified by file comparison
- **SC-004**: Selective generation works — deselecting N files results in exactly N fewer files written to disk
- **SC-005**: The status message accurately reports the number of files generated and any errors

## Assumptions

- The Code Generator panel (feature 076), CodeGenerationRunner (feature 080), and ModuleScaffoldingGenerator (feature 078) are all merged and available
- File writing uses ModuleScaffoldingGenerator for directory structure creation, then writes individual generated file contents to their correct paths
- The "Save" button and `saveFlowKtOnly()` are unaffected by this feature
- ModuleSaveService is not deleted — it is deprecated. It may still be used by tests or future migration steps.
- The Code Generator panel remains Pro-tier gated (feature 074)
