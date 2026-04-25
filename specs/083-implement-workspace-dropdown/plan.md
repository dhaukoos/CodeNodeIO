# Implementation Plan: Implement Module as Workspace via Dropdown

**Branch**: `083-implement-workspace-dropdown` | **Date**: 2026-04-25 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/083-implement-workspace-dropdown/spec.md`

## Summary

Replace the toolbar gear icon + label with a module dropdown for workspace switching. Rename FlowGraph Properties to Module Properties with Create Module functionality. Redesign New/Open/Save to operate on flowGraphs within the workspace module. Persist workspace across restarts.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3
**Primary Dependencies**: graphEditor (TopToolbar, GraphEditorApp, GraphEditorDialogs, FlowGraphPropertiesDialog), flowGraph-generate (ModuleScaffoldingGenerator), fbpDsl (FlowGraph model)
**Storage**: `~/.codenode/config.properties` for workspace persistence + MRU list
**Testing**: `./gradlew :graphEditor:compileKotlinJvm :graphEditor:jvmTest`
**Constraints**: Backward compatibility — existing .flow.kt files openable via "Open from...". Demo project modules must work.
**Scale/Scope**: ~10 modified files, 1 new ViewModel, ~2 new composables

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Clean workspace model replaces scattered state. Single-responsibility ViewModel. |
| II. Test-Driven Development | PASS | Workspace state testable. End-to-end verification via quickstart scenarios. |
| III. User Experience Consistency | PASS | Dropdown follows existing toolbar patterns. Module Properties follows dialog conventions. |
| IV. Performance Requirements | N/A | UI changes, no runtime impact. |
| V. Observability & Debugging | PASS | Workspace state observable. Title bar shows context. |
| Licensing & IP | PASS | No new dependencies. |

## Project Structure

```text
graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/
├── viewmodel/
│   └── WorkspaceViewModel.kt               # NEW — workspace state, MRU, persistence
├── ui/
│   ├── TopToolbar.kt                        # MODIFIED — replace gear icon with module dropdown
│   ├── ModuleDropdown.kt                    # NEW — dropdown composable with MRU + actions
│   ├── ModulePropertiesDialog.kt            # NEW — renamed from FlowGraphPropertiesDialog, redesigned
│   ├── NewFlowGraphDialog.kt                # NEW — simple name-only dialog
│   ├── GraphEditorApp.kt                    # MODIFIED — wire workspace state, title bar, New/Open/Save
│   ├── GraphEditorDialogs.kt                # MODIFIED — replace FlowGraph Properties with Module Properties
│   └── GraphEditorLayout.kt                 # MODIFIED — pass workspace context
└── Main.kt                                  # MODIFIED — dynamic window title
```

## WorkspaceViewModel Design

```kotlin
class WorkspaceViewModel {
    val currentModuleDir: StateFlow<File?>
    val currentModuleName: StateFlow<String>
    val activeFlowGraphName: StateFlow<String?>
    val mruModules: StateFlow<List<File>>
    val isDirty: StateFlow<Boolean>

    fun openModule(moduleDir: File)
    fun createModule(name: String, dir: File, platforms: List<TargetPlatform>)
    fun switchModule(moduleDir: File)
    fun setActiveFlowGraph(name: String, file: File?)
    fun markDirty()
    fun markClean()
    fun persistState()
    fun restoreState()
}
```

## Module Dropdown Design

Replaces gear icon + label in TopToolbar:
```
[StopWatch ▼]  →  DropdownMenu {
    ✓ StopWatch          // current (checkmark)
      WeatherForecast    // MRU
      UserProfiles       // MRU
    ───────────────
      Open Module...     // directory chooser
      Create New Module... // Module Properties create mode
    ───────────────
      Module Settings... // Module Properties edit mode
}
```

## New/Open/Save Redesign

### New
- Dialog: single `OutlinedTextField` for flowGraph name
- Validation: non-empty, no duplicate in module's flow/, valid filename
- On confirm: `FlowGraph(name = enteredName, ...)` + set as active + mark dirty

### Open  
- Scoped: starts in `{moduleDir}/src/commonMain/kotlin/{packagePath}/flow/`
- Filter: `*.flow.kt`
- On select: load graph, set as active, update title bar
- Fallback: "Open from..." browses full filesystem, infers module

### Save
- Deterministic: `{moduleDir}/src/commonMain/kotlin/{packagePath}/flow/{name}.flow.kt`
- No prompt. Disabled without module.
- Calls `saveFlowKtOnly()` with workspace-derived output directory

## Migration from Current Behavior

| Component | Current | New |
|-----------|---------|-----|
| Toolbar left side | Gear icon + `flowGraphName` label | Module dropdown `[ModuleName ▼]` |
| FlowGraph Properties | Dialog with name + platforms | Module Properties with Create Module |
| `saveLocationRegistry` | Maps graph name → directory | Replaced by `WorkspaceViewModel.currentModuleDir` |
| `showModuleSaveDialog` | Directory chooser on first save | Deterministic save, no chooser |
| New button | Creates `FlowGraph("New Graph")` inline | Dialog prompts for name |
| Open button | Full filesystem file chooser | Scoped to module's flow/ directory |
| Title bar | Static "CodeNodeIO" | Dynamic "CodeNodeIO — {FlowGraphName}" |

## Complexity Tracking

No constitution violations. No complexity justification needed.
