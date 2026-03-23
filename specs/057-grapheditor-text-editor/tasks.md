# Tasks: GraphEditor Text Editor

**Input**: Design documents from `/specs/057-grapheditor-text-editor/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Not explicitly requested — no test tasks included.

**Organization**: Tasks grouped by user story. US1 delivers the core code editor with pencil-icon trigger. US2 adds the file selector dropdown. US3 ensures read-only distinction for flowGraph files.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: No new Gradle modules or external dependencies needed. This feature modifies and adds files within the existing `graphEditor` module only.

*(No tasks)*

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Extend the SyntaxHighlighter and create the CodeEditorViewModel — shared infrastructure required by all user stories.

- [ ] T001 Extend `SyntaxHighlightingTheme.keywords` with Kotlin language keywords (`override`, `return`, `if`, `else`, `when`, `for`, `while`, `import`, `package`, `private`, `internal`, `suspend`, `inline`, `reified`, `companion`, `data`, `sealed`, `enum`, `abstract`, `open`, `const`, `lateinit`, `by`, `lazy`, `true`, `false`, `null`, `is`, `as`, `in`, `this`, `super`, `try`, `catch`, `finally`, `throw`, `object`, `var`) in `graphEditor/src/jvmMain/kotlin/ui/SyntaxHighlighter.kt`
- [ ] T002 Create `CodeEditorViewModel` with file load/save, dirty tracking, and editor state management (currentFile, originalContent, editedContent, isDirty, isReadOnly, cursorPosition) in `graphEditor/src/jvmMain/kotlin/viewmodel/CodeEditorViewModel.kt`

**Checkpoint**: SyntaxHighlighter highlights Kotlin keywords. ViewModel can load/save files and track dirty state.

---

## Phase 3: User Story 1 — Edit CodeNode Source Files via Pencil Icon (Priority: P1)

**Goal**: Users can select a CodeNode, click a pencil icon in the Properties Panel header, and edit the node's source file in a code editor with syntax highlighting and line numbers.

**Independent Test**: Open a flowGraph with CodeNodes, select one, click pencil icon, verify editable code editor appears with syntax highlighting, line numbers, and save capability.

### Implementation

- [ ] T003 [US1] Create `CodeEditor` composable with `BasicTextField`, `VisualTransformation` for syntax highlighting, line number gutter, and save button in `graphEditor/src/jvmMain/kotlin/ui/CodeEditor.kt`
- [ ] T004 [US1] Add pencil icon (`Icons.Default.Edit`) to `PropertiesPanelHeader` right side (after status indicators), visible only when selected node has an editable source file, with `onEditClick` callback in `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`
- [ ] T005 [US1] Update `GraphViewContainer` and `GraphEditorWithToggle` in `ViewToggle.kt` to accept an optional `codeEditorContent` composable parameter — when provided and view is TEXTUAL, render the code editor instead of the read-only `TextualView` in `graphEditor/src/jvmMain/kotlin/ui/ViewToggle.kt`
- [ ] T006 [US1] Wire pencil icon click in `Main.kt` to: switch view mode to TEXTUAL, resolve selected CodeNode's source file path, load file into `CodeEditorViewModel`, and pass `CodeEditor` composable to `GraphEditorWithToggle` in `graphEditor/src/jvmMain/kotlin/Main.kt`
- [ ] T007 [US1] Add unsaved-changes dialog (Save/Discard/Cancel) that triggers when navigating away from a dirty editor (switching nodes, switching to Visual view, or closing editor) in `graphEditor/src/jvmMain/kotlin/ui/CodeEditor.kt`

**Checkpoint**: Pencil icon appears for CodeNodes. Clicking it opens editable code editor with syntax highlighting, line numbers, save, and unsaved-changes protection.

---

## Phase 4: User Story 2 — File Selector Dropdown in Central Panel Header (Priority: P2)

**Goal**: Users can navigate between the flowGraph file and CodeNode source files via a dropdown in the central panel header.

**Independent Test**: Open a flowGraph with multiple CodeNodes, verify dropdown shows all files, selecting a CodeNode entry selects the node on canvas and shows its file in the editor.

### Implementation

- [ ] T008 [US2] Create `FileSelector` dropdown composable that lists the flowGraph file (default) plus all CodeNode source files, with `onFileSelected` callback returning the selected `FileEntry` in `graphEditor/src/jvmMain/kotlin/ui/FileSelector.kt`
- [ ] T009 [US2] Build file entry list from the current flowGraph's nodes — iterate nodes, filter for CodeNodes with resolvable source files, create `FileEntry` objects with display name, file path, and associated node ID in `graphEditor/src/jvmMain/kotlin/viewmodel/CodeEditorViewModel.kt`
- [ ] T010 [US2] Integrate `FileSelector` into the central panel header row in `ViewToggle.kt` — place dropdown to the left of the View Toggle buttons, so the header reads: `[FileSelector dropdown] | View: [Visual] [Textual] [Split]` in `graphEditor/src/jvmMain/kotlin/ui/ViewToggle.kt`
- [ ] T011 [US2] Wire dropdown selection in `Main.kt` to: select the corresponding node on the canvas (if CodeNode selected), load the file into the editor, and switch to Textual view if a CodeNode is selected in `graphEditor/src/jvmMain/kotlin/Main.kt`

**Checkpoint**: Dropdown shows flowGraph + CodeNode files. Selecting a CodeNode entry selects it on canvas and opens its file in the editor.

---

## Phase 5: User Story 3 — Read-Only FlowGraph Textual View (Priority: P3)

**Goal**: FlowGraph DSL files remain read-only in Textual view with a visual distinction from editable CodeNode files.

**Independent Test**: Switch to Textual view with flowGraph selected — verify text is not editable, "Read Only" indicator visible. Switch to a CodeNode file — verify editing is enabled and indicator changes.

### Implementation

- [ ] T012 [US3] Add read-only visual indicator (e.g., "Read Only" label or distinct background shade) to `TextualView` when displaying flowGraph DSL, and ensure `BasicTextField` is disabled/not-editable for flowGraph files in `graphEditor/src/jvmMain/kotlin/ui/TextualView.kt`
- [ ] T013 [US3] Ensure `CodeEditor` composable displays an editable indicator (e.g., no "Read Only" label, standard editor background) to visually distinguish from read-only mode in `graphEditor/src/jvmMain/kotlin/ui/CodeEditor.kt`
- [ ] T014 [US3] Handle missing source file case — when a CodeNode's file path doesn't exist on disk, display "Source file not found: {path}" error message in the editor area instead of the code editor in `graphEditor/src/jvmMain/kotlin/ui/CodeEditor.kt`

**Checkpoint**: FlowGraph DSL is visually read-only. CodeNode files are visually editable. Missing files show error.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T015 Build the project to verify all changes compile via `./gradlew :graphEditor:build`
- [ ] T016 Run graphEditor and manually verify all 6 quickstart scenarios from `specs/057-grapheditor-text-editor/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No tasks needed
- **Foundational (Phase 2)**: No dependencies — can start immediately
- **US1 (Phase 3)**: Depends on Phase 2 (T001, T002)
- **US2 (Phase 4)**: Depends on Phase 3 (uses CodeEditor and ViewModel from US1)
- **US3 (Phase 5)**: Depends on Phase 3 (builds on CodeEditor composable)
- **Polish (Phase 6)**: Depends on all prior phases

### Within Phases

- **Phase 2**: T001 and T002 are independent and can run in parallel [P]
- **Phase 3**: T003 and T004 can run in parallel (different files). T005 depends on T003. T006 depends on T003-T005. T007 depends on T003.
- **Phase 4**: T008 is independent. T009 depends on T002. T010 depends on T008. T011 depends on T008-T010.
- **Phase 5**: T012, T013, T014 can run in parallel (different aspects of different files)

### Parallel Opportunities

```text
# Phase 2 — both foundational tasks in parallel:
T001 (SyntaxHighlighter) + T002 (CodeEditorViewModel)

# Phase 3 — initial composables in parallel:
T003 (CodeEditor) + T004 (PropertiesPanel pencil icon)

# Phase 5 — all three tasks in parallel:
T012 (TextualView read-only) + T013 (CodeEditor editable indicator) + T014 (missing file handling)
```

---

## Implementation Strategy

### MVP First (Phase 2 + Phase 3)

1. Extend SyntaxHighlighter + create ViewModel (T001, T002)
2. Build CodeEditor composable + pencil icon (T003, T004)
3. Integrate into ViewToggle + Main.kt (T005, T006, T007)
4. **STOP and VALIDATE**: Can edit CodeNode files via pencil icon

### Incremental Delivery

1. Phase 2: Foundation → syntax highlighting + state management ready
2. Phase 3: US1 → core editing via pencil icon works
3. Phase 4: US2 → file dropdown navigation works
4. Phase 5: US3 → read-only distinction + error handling
5. Phase 6: Full build + manual verification

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- T003 (CodeEditor) is the most complex task — BasicTextField + VisualTransformation + line number gutter + scroll synchronization
- T006 and T011 (Main.kt wiring) require understanding the existing `GraphEditorWithToggle` integration point
- The existing `TextualView` remains for read-only flowGraph DSL display; `CodeEditor` is a new composable for editable files
- Commit after each phase completion
