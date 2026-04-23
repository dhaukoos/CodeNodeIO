# Research: Wire Code Generator Panel

**Feature**: 081-wire-codegen-panel
**Date**: 2026-04-23

## R1: Generate Action Flow

**Decision**: When the user clicks "Generate", the panel:
1. Reads the selected `GenerationPath` and builds a `GenerationConfig` from the panel state (flow graph name, packages)
2. Builds a `SelectionFilter` from the file tree state via `SelectionFilter.fromFileTree()`
3. Calls `ModuleScaffoldingGenerator.generate()` to ensure the directory structure exists
4. Calls `CodeGenerationRunner.execute()` with the path, config, and filter
5. Writes each entry in `GenerationResult.generatedFiles` to the appropriate file path in the module directory
6. Reports results via status message

**Rationale**: This flow reuses all components from features 078 (scaffolding), 079 (wrappers), and 080 (runner). The only new code is the wiring in the panel and the file-writing step.

## R2: File Writing Strategy

**Decision**: After the runner produces `GenerationResult`, a file writer maps each generator ID to its target file path (using the folder hierarchy from feature 077) and writes the content. The path mapping uses the same logic as `GenerationFileTreeBuilder` — each `FileNode.name` is the filename, and the `FolderNode.name` is the subdirectory.

**File path construction**: `moduleDir/src/commonMain/kotlin/{packagePath}/{folder}/{fileName}`

**Write semantics**:
- Generated content files: always overwrite (same as current behavior)
- Gradle files and UI stubs: handled by ModuleScaffoldingGenerator (write-once)

## R3: Toolbar Button Removal

**Decision**: Remove `onGenerate` and `onGenerateUIFBP` callbacks from `TopToolbar`, `GraphEditorApp`, and `GraphEditorDialogs`. Remove the corresponding `showGenerateDialog` and `showGenerateUIFBPDialog` state variables and LaunchedEffect handlers. The "Save" button and `onSave` callback remain unchanged.

**Files affected**:
- `TopToolbar.kt` — remove "Generate Module" and "Generate UI-FBP" buttons
- `GraphEditorApp.kt` — remove state variables and callback wiring
- `GraphEditorDialogs.kt` — remove Generate and Generate UI-FBP LaunchedEffect handlers

## R4: ModuleSaveService Deprecation

**Decision**: Add `@Deprecated` annotations to `saveModule()` and `saveEntityModule()` with replacement messages pointing to `CodeGenerationRunner.execute()`. Do not delete the methods — they may be used by existing tests or future migration.

## R5: Panel Generate Button Enable Conditions

**Decision**: The "Generate" button is enabled when:
- For GENERATE_MODULE: always (flow graph name is always available)
- For REPOSITORY: an IP Type is selected (`selectedIPTypeId != null`)
- For UI_FBP: a UI file is selected (`selectedUIFilePath != null`)

Currently the button is always disabled (`enabled = false`). Change to conditional based on path + input readiness.
