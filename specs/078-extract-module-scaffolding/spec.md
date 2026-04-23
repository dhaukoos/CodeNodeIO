# Feature Specification: Module Scaffolding Extraction

**Feature Branch**: `078-extract-module-scaffolding`
**Created**: 2026-04-22
**Status**: Draft
**Input**: User description: "Extract module directory creation and Gradle file generation from ModuleSaveService into a standalone ModuleScaffoldingGenerator component."

## Context

This is Step 2 of the Code Generation Migration Plan from feature 076. The dependency analysis identified module scaffolding as the root prerequisite — all other generators assume the directory structure exists. Currently, scaffolding logic (directory creation, Gradle file generation) is embedded within `ModuleSaveService.saveModule()` and `saveEntityModule()`. Extracting it into a standalone component makes this dependency explicit, testable, and reusable as a future CodeNode.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Extract ModuleScaffoldingGenerator (Priority: P1)

A developer creates a new `ModuleScaffoldingGenerator` class that encapsulates all module directory creation and Gradle file generation. Given a module name, output directory, and target platform list (e.g., Android, iOS, Desktop, Web), it creates the complete KMP module structure: the module directory, source set directories (commonMain, platform-specific main/test sets based on selected targets), the subdirectory structure (flow/, controller/, viewmodel/, userInterface/, nodes/), and the Gradle files (build.gradle.kts configured for the selected platforms, settings.gradle.kts). This component operates independently — it requires no FlowGraph, no IP types, no other generators.

**Why this priority**: The extraction must happen before refactoring the callers. The new class must exist and be tested before ModuleSaveService can delegate to it.

**Independent Test**: Call `ModuleScaffoldingGenerator.generate("TestModule", tempDir)`. Verify the returned directory exists and contains the expected structure: source set directories, subdirectory hierarchy, and Gradle files. No other files should be generated.

**Acceptance Scenarios**:

1. **Given** a module name "StopWatch", an output directory, and target platforms (Android, iOS, Desktop), **When** the scaffolding generator runs, **Then** a "StopWatch" directory is created containing `build.gradle.kts` configured for the selected platforms, `settings.gradle.kts`, and `src/commonMain/kotlin/io/codenode/stopwatch/` with subdirectories flow/, controller/, viewmodel/, userInterface/, nodes/
2. **Given** a module name and output directory where the module already exists, **When** the scaffolding generator runs, **Then** existing directories are preserved and Gradle files are not overwritten (write-once semantics)
3. **Given** the scaffolding generator, **When** called with a module name, directory, and target platforms (no FlowGraph), **Then** it succeeds — it has no dependency on FlowGraph or any other generator
4. **Given** a module targeting only Android and iOS, **When** the scaffolding generator runs, **Then** the `build.gradle.kts` configures only androidTarget and iOS targets, and platform-specific source set directories are created accordingly

---

### User Story 2 - Refactor ModuleSaveService to Use Scaffolding Generator (Priority: P2)

`ModuleSaveService.saveModule()` is refactored to call `ModuleScaffoldingGenerator.generate()` as its first step, then delegates to the individual content generators (flow, controller, viewmodel, UI, persistence). The same refactoring applies to `saveEntityModule()`. After refactoring, the behavior of both methods is identical to before — the only change is internal delegation.

**Why this priority**: The refactoring validates that the extracted component integrates correctly with the existing generation pipeline. All existing tests must continue to pass.

**Independent Test**: Run the full existing test suite (`./gradlew :flowGraph-generate:jvmTest`). All tests pass. Generate a module via the graph editor — identical output to before.

**Acceptance Scenarios**:

1. **Given** the refactored `saveModule()`, **When** generating a new module, **Then** the output is identical to the pre-refactoring output (same files, same content, same directory structure)
2. **Given** the refactored `saveEntityModule()`, **When** generating a repository module, **Then** the output is identical to the pre-refactoring output
3. **Given** the refactored code, **When** all existing tests run, **Then** 100% of tests pass without modification (behavior-preserving refactor)
4. **Given** the refactored `saveModule()`, **When** inspecting the code, **Then** directory creation and Gradle file generation are no longer inline — they are delegated to `ModuleScaffoldingGenerator`

---

### Edge Cases

- What happens when the output directory doesn't exist? The scaffolding generator creates it (same behavior as current ModuleSaveService).
- What happens when called with an empty module name? The generator returns an error or derives a safe default — consistent with current `deriveModuleName()` behavior.
- What happens when Gradle files already exist in the module directory? They are preserved (write-once) — same as current behavior where `writeFileIfNew` skips existing files.
- What happens when the module directory exists but is missing some subdirectories? The generator creates any missing subdirectories without affecting existing ones.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide a standalone `ModuleScaffoldingGenerator` component that creates a complete KMP module directory structure given a module name, output directory, and target platform list
- **FR-002**: The scaffolding generator MUST create the module directory, source set directories (commonMain/kotlin, jvmMain/kotlin, commonTest/kotlin), and subdirectory structure (flow/, controller/, viewmodel/, userInterface/, nodes/) with correct package paths
- **FR-003**: The scaffolding generator MUST generate `build.gradle.kts` and `settings.gradle.kts` with write-once semantics (skip if files already exist)
- **FR-004**: The scaffolding generator MUST NOT depend on FlowGraph, IP types, or any other generator — it operates solely on module name, output directory, and target platforms
- **FR-005**: `ModuleSaveService.saveModule()` MUST be refactored to delegate directory and Gradle creation to `ModuleScaffoldingGenerator` as its first step
- **FR-006**: `ModuleSaveService.saveEntityModule()` MUST be similarly refactored to use `ModuleScaffoldingGenerator`
- **FR-007**: The refactoring MUST be behavior-preserving — all existing tests pass without modification, and generated output is identical
- **FR-008**: The scaffolding generator MUST be independently testable with unit tests that verify directory creation and file generation without requiring a FlowGraph

### Key Entities

- **ModuleScaffoldingGenerator**: A standalone component responsible for creating the KMP module directory structure and Gradle files. Takes module name and output directory as inputs. Returns the module root directory.
- **ModuleScaffold**: The output of the scaffolding generator — a directory containing the complete KMP module structure ready for content generation by downstream generators.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The `ModuleScaffoldingGenerator` can create a complete module structure in isolation — verified by unit tests that pass without any FlowGraph input
- **SC-002**: 100% of existing `ModuleSaveService` tests pass after the refactoring — zero regressions
- **SC-003**: The refactored `saveModule()` and `saveEntityModule()` produce byte-identical output compared to the pre-refactoring versions for the same inputs
- **SC-004**: The scaffolding generator creates the correct subdirectory structure matching the folder hierarchy from feature 077 (flow/, controller/, viewmodel/, userInterface/, nodes/)

## Assumptions

- The scaffolding generator lives in the `flowGraph-generate` module alongside the existing generators
- The `build.gradle.kts` generation currently in `ModuleGenerator.generateBuildGradle()` is reused — the scaffolding generator calls the existing method, not duplicates it
- The `settings.gradle.kts` generation currently inline in `ModuleSaveService` is moved to the scaffolding generator
- Target platform information (Android, iOS, Desktop, Web) is a required input to the scaffolding generator — currently derived from the FlowGraph's `targetPlatforms`, but the scaffolding generator accepts it directly as a list
- This is a pure refactoring — no new user-facing functionality, no UI changes, no behavior changes
