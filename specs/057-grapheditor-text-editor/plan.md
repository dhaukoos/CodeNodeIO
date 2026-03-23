# Implementation Plan: GraphEditor Text Editor

**Branch**: `057-grapheditor-text-editor` | **Date**: 2026-03-22 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/057-grapheditor-text-editor/spec.md`

## Summary

Add an integrated code editor to the graphEditor that enables editing CodeNode source files directly within the application. Uses Compose's built-in `BasicTextField` with `VisualTransformation` and extends the existing `SyntaxHighlighter` with Kotlin keywords — no external library dependencies. Adds a pencil icon to the Properties Panel header for launching the editor, and a file selector dropdown to the central panel header for navigating between flowGraph DSL and CodeNode source files. FlowGraph DSL remains read-only; CodeNode files are fully editable with syntax highlighting, line numbers, and unsaved-changes protection.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3
**Primary Dependencies**: Compose Material3 (UI components), kotlinx-coroutines 1.8.0, lifecycle-viewmodel-compose 2.8.0
**Storage**: Filesystem (read/write CodeNode `.kt` source files via `File.readText()` / `File.writeText()`)
**Testing**: Manual testing via graphEditor runtime (no automated UI tests for this feature)
**Target Platform**: JVM Desktop (macOS, Linux, Windows)
**Project Type**: KMP multi-module desktop application
**Performance Goals**: Editor loads and is interactive within 3 seconds; syntax highlighting re-renders on each keystroke without perceptible lag for files under 1000 lines
**Constraints**: No external code editor library dependencies; extend existing `SyntaxHighlighter`
**Scale/Scope**: Typical CodeNode source files are 30-150 lines of Kotlin code

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Single-responsibility composables, clear naming, type-safe state |
| II. Test-Driven Development | EXCEPTION | UI-only feature; manual testing via graphEditor runtime preview. No automated UI test framework in project. |
| III. User Experience Consistency | PASS | Uses existing Material3 components, follows current panel/header patterns, consistent color scheme |
| IV. Performance Requirements | PASS | Files are small (30-150 lines); `BasicTextField` + `VisualTransformation` is efficient for this scale |
| V. Observability & Debugging | N/A | Desktop UI feature, no production service metrics needed |
| Licensing | PASS | No new dependencies; all changes use existing Apache 2.0 / MIT licensed stack |

**Post-Design Re-check**: All gates still pass. No new dependencies introduced.

## Project Structure

### Documentation (this feature)

```text
specs/057-grapheditor-text-editor/
├── plan.md              # This file
├── research.md          # Code editor library evaluation + design decisions
├── data-model.md        # EditorState and FileEntry models
├── quickstart.md        # Integration test scenarios
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
graphEditor/src/jvmMain/kotlin/
├── Main.kt                          # Central panel header dropdown integration
├── ui/
│   ├── CodeEditor.kt                # NEW: Editable code editor composable (BasicTextField + line numbers)
│   ├── FileSelector.kt              # NEW: Dropdown file selector composable
│   ├── PropertiesPanel.kt           # MODIFY: Add pencil icon to header
│   ├── SyntaxHighlighter.kt         # MODIFY: Add Kotlin keywords
│   ├── TextualView.kt               # MODIFY: Route to CodeEditor for editable files
│   └── ViewToggle.kt                # MODIFY: Support file-aware view switching
└── viewmodel/
    └── CodeEditorViewModel.kt       # NEW: Editor state management (load, save, dirty tracking)
```

**Structure Decision**: All changes are within the existing `graphEditor` module. Two new files (`CodeEditor.kt`, `FileSelector.kt`) for new composables, one new ViewModel (`CodeEditorViewModel.kt`), and modifications to four existing files. No new Gradle modules or dependencies.

## Design Decisions

### 1. Code Editor Approach: BasicTextField + VisualTransformation

Use Compose's `BasicTextField` with a custom `VisualTransformation` that applies syntax highlighting via the existing `SyntaxHighlighter`. This provides:
- Editable text with full keyboard support (type, select, copy, paste, undo, redo)
- Syntax highlighting re-applied on each text change
- Consistent look with the existing read-only `TextualView` (same colors, font, background)

### 2. Line Numbers via Side Gutter

A fixed-width `Column` to the left of the `BasicTextField`, synchronized with the editor's vertical scroll state. Line numbers use subdued color (`0xFF808080`) and monospace font. Width auto-adjusts based on total line count (e.g., 3 digits for files with 100+ lines).

### 3. SyntaxHighlighter Extension

Add Kotlin keywords to the existing keyword set: `fun`, `val`, `var`, `class`, `object`, `override`, `return`, `if`, `else`, `when`, `for`, `while`, `import`, `package`, `private`, `internal`, `suspend`, `inline`, `reified`, `companion`, `data`, `sealed`, `enum`, `abstract`, `open`, `const`, `lateinit`, `by`, `lazy`, `true`, `false`, `null`, `is`, `as`, `in`, `this`, `super`, `try`, `catch`, `finally`, `throw`.

The highlighter already handles strings, comments, and numbers — these work for Kotlin without changes.

### 4. Editor ↔ View Mode Integration

- When the pencil icon is clicked or a CodeNode is selected in the dropdown, the view mode switches to `TEXTUAL` and the `TextualView` renders the `CodeEditor` composable instead of the read-only `Text`.
- The `TextualView` receives a new parameter to distinguish between read-only (flowGraph DSL) and editable (CodeNode source) modes.
- In `SPLIT` mode, the right pane shows the code editor if a CodeNode file is selected, or read-only DSL if the flowGraph file is selected.

### 5. File Discovery

CodeNode source file paths are resolved from the flowGraph's node metadata:
- Each `CodeNode` in the flowGraph has a `name` and `nodeType`.
- The `NodeDefinitionRegistry` (or `NodeTemplateMeta`) maps node names to file paths based on `PlacementLevel` (MODULE, PROJECT, UNIVERSAL).
- The dropdown is populated by iterating the flowGraph's nodes, filtering for those with resolvable source files.

### 6. Save Mechanism

- A "Save" button appears in the editor toolbar (or code editor header) when content is dirty.
- Keyboard shortcut Ctrl+S / Cmd+S saves the current file.
- Save writes content via `File.writeText()` and resets the dirty flag.
- A save failure shows an error in the status bar.

## Complexity Tracking

No constitution violations requiring justification. All changes use existing patterns and dependencies.
