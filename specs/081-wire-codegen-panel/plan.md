# Implementation Plan: Wire Code Generator Panel

**Branch**: `081-wire-codegen-panel` | **Date**: 2026-04-23 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/081-wire-codegen-panel/spec.md`

## Summary

Connect the Code Generator panel's Generate button to the CodeGenerationRunner, write generated content to disk, remove old toolbar buttons, and deprecate ModuleSaveService direct calls. This completes the migration from hardcoded generation to configurable flow-graph-based generation.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3
**Primary Dependencies**: graphEditor (CodeGeneratorPanel, TopToolbar, GraphEditorApp), flowGraph-generate (CodeGenerationRunner, ModuleScaffoldingGenerator, SelectionFilter)
**Testing**: `./gradlew :graphEditor:compileKotlinJvm :graphEditor:jvmTest`
**Constraints**: Backward compatibility — output must match old toolbar buttons. Save button unchanged.
**Scale/Scope**: ~6 modified files, 1 new file writer utility

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Consolidates generation to single panel. Removes duplication. |
| II. Test-Driven Development | PASS | Backward compatibility verified by output comparison. |
| III. User Experience Consistency | PASS | Generation moves from toolbar to dedicated panel — more discoverable. |
| IV. Performance Requirements | PASS | Under 5 seconds per spec SC-001. |
| V. Observability & Debugging | PASS | Status message reports file count and errors. |
| Licensing & IP | PASS | No new dependencies. |

## Project Structure

```text
graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/
├── ui/
│   ├── CodeGeneratorPanel.kt           # MODIFIED — enable Generate button, wire to runner
│   ├── TopToolbar.kt                   # MODIFIED — remove Generate Module + Generate UI-FBP buttons
│   ├── GraphEditorApp.kt               # MODIFIED — remove generate state vars, add runner wiring
│   └── GraphEditorDialogs.kt           # MODIFIED — remove Generate/UI-FBP dialog handlers
└── viewmodel/
    └── CodeGeneratorViewModel.kt        # MODIFIED — add generate action method

flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/
├── save/
│   └── ModuleSaveService.kt            # MODIFIED — add @Deprecated to saveModule/saveEntityModule
└── runner/
    └── GenerationFileWriter.kt         # NEW — writes GenerationResult content to disk
```

## Generate Action Design

### CodeGeneratorViewModel.generate()

New method that orchestrates the full generation:

```
suspend fun generate(outputDir: File): GenerationResult {
    1. Build GenerationConfig from panel state (flowGraphName, packages)
    2. Build SelectionFilter from file tree state
    3. Call ModuleScaffoldingGenerator.generate() for directory/Gradle setup
    4. Call CodeGenerationRunner.execute() with path, config, filter
    5. Call GenerationFileWriter.write() to write content to disk
    6. Return result
}
```

### GenerationFileWriter

New utility that maps GenerationResult entries to file paths and writes them:

```kotlin
class GenerationFileWriter {
    fun write(
        result: GenerationResult,
        moduleDir: File,
        basePackage: String
    ): List<String>  // returns list of files written
}
```

Maps generatorId → subdirectory + filename using the same logic as GenerationFileTreeBuilder.

### Generate Button Enable Conditions

```kotlin
enabled = when (state.selectedPath) {
    GENERATE_MODULE -> true
    REPOSITORY -> state.selectedIPTypeId != null
    UI_FBP -> state.selectedUIFilePath != null
}
```

## Toolbar Removal Design

Remove from `TopToolbar.kt`:
- `onGenerate: () -> Unit` parameter and "Generate Module" button
- `onGenerateUIFBP: () -> Unit` parameter and "Generate UI-FBP" button

Remove from `GraphEditorApp.kt`:
- `showGenerateDialog` and `showGenerateUIFBPDialog` state variables
- `onGenerate` and `onGenerateUIFBP` callback wiring

Remove from `GraphEditorDialogs.kt`:
- `showGenerateDialog` / `onShowGenerateDialogChanged` parameters
- `showGenerateUIFBPDialog` / `onShowGenerateUIFBPDialogChanged` parameters
- Both LaunchedEffect handlers for Generate and Generate UI-FBP

## Complexity Tracking

No constitution violations. No complexity justification needed.
