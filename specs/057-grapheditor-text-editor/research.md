# Research: GraphEditor Text Editor

**Feature**: 057-grapheditor-text-editor
**Date**: 2026-03-22

---

## Decision 1: Code Editor Library Selection

**Decision**: Use Compose's built-in `BasicTextField` with `VisualTransformation` and extend the existing `SyntaxHighlighter` — no external library dependency.

**Rationale**:
- All three candidate libraries are outdated and unmaintained:
  - **n34t0/compose-code-editor**: Last release Oct 2021, built against Kotlin 1.5.30/Compose alpha. Binary incompatible with our Kotlin 2.1.21/Compose 1.7.3. Depends on IntelliJ Platform (heavyweight). Effectively abandoned.
  - **Qawaz/compose-code-editor** (formerly wakaztahir): Last release Feb 2023, built against Kotlin 1.8.0/Compose 1.3.0. Distributed via GitHub Packages (requires PAT). Significant version gap.
  - **SnipMeDev/KodeView**: Most recent (Nov 2024, v0.9.0), Apache 2.0, on Maven Central, Kotlin 2.0.20. Best third-party option but editable mode (`CodeEditText`) is immature. No built-in line numbers.
- The project already has a working `SyntaxHighlighter` that produces `AnnotatedString` output — exactly what `VisualTransformation` needs for `BasicTextField`.
- Zero new dependencies = no maintenance risk, guaranteed Kotlin/Compose compatibility, full control over behavior.
- `BasicTextField` provides native undo/redo (Ctrl+Z/Ctrl+Y), copy/paste, and selection out of the box on Compose Desktop.

**Alternatives Considered**:
- KodeView as fallback if we later need multi-language highlighting beyond Kotlin/DSL.

---

## Decision 2: Syntax Highlighting for Kotlin Source Files

**Decision**: Extend the existing `SyntaxHighlighter` with Kotlin language keywords, keeping the same IntelliJ-dark-inspired color scheme.

**Rationale**:
- Current `SyntaxHighlighter` handles `.flow.kts` DSL keywords (flowGraph, codeNode, input, output, connect, version). It already supports string literals, comments (single-line and multi-line), and number literals.
- CodeNode source files are Kotlin `.kt` files. Adding Kotlin keywords (fun, val, var, class, object, override, return, if, when, etc.) to the keyword set is straightforward.
- Reusing the same color scheme (orange keywords, green strings, blue numbers, gray comments) provides visual consistency across the Textual view for both flowGraph DSL and CodeNode source files.

**Alternatives Considered**:
- Using KodeView's `Highlights` engine for Kotlin highlighting — rejected to avoid external dependency.

---

## Decision 3: Pencil Icon Placement in Properties Panel Header

**Decision**: Add the pencil (edit) icon to the right side of the `PropertiesPanelHeader` row, after the existing status indicators (dirty dot, error icon).

**Rationale**:
- Current header layout: left side has "Properties" title + node name/type, right side has status indicators.
- Adding the pencil icon to the right side follows the existing pattern of action/status elements on the right.
- The icon is contextual — only shown when the selected node has an editable source file (CodeNode, not built-in node).
- Uses Material Icons `Icons.Default.Edit` (pencil) consistent with Material Design conventions.

---

## Decision 4: File Dropdown Placement in Central Panel

**Decision**: Add the file selector dropdown to the left side of the central panel header, before the View Toggle buttons (Visual/Textual/Split).

**Rationale**:
- The current central panel header contains only the `ViewToggle` (Visual/Textual/Split buttons).
- Placing the dropdown on the left establishes a logical left-to-right flow: "What file?" → "How to view it?"
- The dropdown defaults to the flowGraph file and lists CodeNode source files when available.
- This matches the user's specification: "add to its left a dropdown selection."

---

## Decision 5: File I/O for CodeNode Source Files

**Decision**: Use `File.readText()` for loading and `File.writeText()` for saving, following existing patterns in `FlowKtParser` and `ModuleSaveService`.

**Rationale**:
- The project already uses direct file I/O for reading `.flow.kt` files (`file.readText()`) and writing generated modules.
- CodeNode source file paths are determinable from `NodeDefinitionRegistry`'s `NodeTemplateMeta` records and `PlacementLevel`:
  - MODULE: `{projectRoot}/{ModuleName}/src/commonMain/kotlin/io/codenode/{module}/nodes/{NodeName}CodeNode.kt`
  - PROJECT: `{projectRoot}/nodes/src/commonMain/kotlin/io/codenode/nodes/{NodeName}CodeNode.kt`
  - UNIVERSAL: `~/.codenode/nodes/{NodeName}CodeNode.kt`
- No version control integration needed per spec assumptions.

---

## Decision 6: Unsaved Changes Handling

**Decision**: Track dirty state in the editor. On navigation away (switching nodes, switching to Visual view, closing editor), prompt the user with a save/discard dialog if there are unsaved changes.

**Rationale**:
- Auto-save could silently introduce broken code. An explicit save action is safer for source code editing.
- The Properties Panel already has a dirty-state pattern (orange dot indicator + Save/Reset buttons) that can be reused as a UX pattern.
- A simple `AlertDialog` with "Save" / "Discard" / "Cancel" options is consistent with standard code editor behavior.

---

## Decision 7: Line Number Implementation

**Decision**: Implement line numbers as a side gutter — a `Column` of line-number `Text` elements synchronized with the editor's scroll state.

**Rationale**:
- `BasicTextField` doesn't natively support line numbers.
- A side gutter approach (fixed-width column to the left of the text area) is the standard pattern for code editors.
- Scroll synchronization between the gutter and the text field ensures line numbers stay aligned during scrolling.
- Line numbers use a subdued color (e.g., the existing comment gray 0xFF808080) and monospace font to maintain alignment.
