# Tasks: Decompose graphEditor Main.kt

**Input**: Design documents from `/specs/072-decompose-main-kt/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, quickstart.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story. TDD verification (compile + test pass) is required after each extraction step.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: Branch verification and establish green baseline

- [X] T001 Verify on branch `072-decompose-main-kt` and run `./gradlew :graphEditor:jvmTest` to establish green baseline
- [X] T002 Record baseline line count of `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/Main.kt` (expect ~1923 lines)

---

## Phase 2: User Story 1 - Extract Top-Level Composables and Utilities (Priority: P1) 🎯 MVP

**Goal**: Move self-contained top-level functions out of Main.kt into their own files, reducing its size by ~600 lines with zero logic changes.

**Independent Test**: `./gradlew :graphEditor:compileKotlinJvm` succeeds and `./gradlew :graphEditor:jvmTest` passes after each extraction.

### Extract Self-Contained Composables

- [X] T003 [US1] Extract `TopToolbar` composable (lines ~1530–1676) to `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/TopToolbar.kt` — move function with its imports, change visibility to `internal` if needed, verify compile + tests pass
- [X] T004 [US1] Extract `StatusBar` composable (lines ~1682–1735) to `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/StatusBar.kt` — move function with its imports, verify compile + tests pass
- [X] T005 [US1] Run `./gradlew :graphEditor:jvmTest` to confirm no regressions after composable extractions

### Extract Utility Functions

- [X] T006 [P] [US1] Create `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/FileDialogUtils.kt` — move `showFileOpenDialog()`, `FileOpenResult` data class, and `showDirectoryChooser()` from Main.kt with their imports
- [X] T007 [P] [US1] Create `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/util/ModuleRootResolver.kt` — move `findModuleRoot()` and `resolveFlowKtFromModule()` from Main.kt
- [X] T008 [P] [US1] Create `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/util/ConnectionIPTypeResolver.kt` — move `resolveConnectionIPTypes()` from Main.kt
- [X] T009 [US1] Update imports in Main.kt to reference the new file locations for all extracted utilities, verify compile + tests pass
- [X] T010 [US1] Run `./gradlew :graphEditor:jvmTest` to confirm no regressions after utility extractions

**Checkpoint**: Main.kt is reduced by ~600 lines. All extracted functions are in their own files. All tests pass.

---

## Phase 3: User Story 2 - Decompose GraphEditorApp into Logical Subfunctions (Priority: P2)

**Goal**: Break the ~1300-line `GraphEditorApp` composable into well-named sub-composables, each handling one logical concern.

**Independent Test**: `./gradlew :graphEditor:compileKotlinJvm` succeeds and `./gradlew :graphEditor:jvmTest` passes after each decomposition step.

### Extract State Initialization

- [X] T011 [US2] Create `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorInitialization.kt` — define `GraphEditorState` class bundling all state variables (graphState, undoRedoManager, propertyChangeTracker, registries, ViewModels, panel states, moduleRootDir, runtimeSession, etc.) and a `@Composable fun rememberGraphEditorState(): GraphEditorState` factory that contains the initialization logic from GraphEditorApp lines ~115–468
- [X] T012 [US2] Update `GraphEditorApp` in Main.kt to call `rememberGraphEditorState()` and destructure/use the returned state holder, removing the inline initialization code. Verify compile + tests pass

### Extract Connection Color Mapper

- [X] T013 [US2] Create `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/ConnectionColorMapper.kt` — extract connection color computation (Main.kt lines ~713–847) as `@Composable fun rememberConnectionColors(flowGraph, ipTypeRegistry, navigationContext, ...): ConnectionColorState` returning a data class with the color maps
- [X] T014 [US2] Update `GraphEditorApp` to call `rememberConnectionColors()` instead of inline computation. Verify compile + tests pass

### Extract Keyboard Handler

- [X] T015 [US2] Create `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorKeyboardHandler.kt` — extract keyboard shortcut handling (Main.kt lines ~570–601) as a function `fun handleGraphEditorKeyEvent(keyEvent, graphState, undoRedoManager, ...): Boolean`
- [X] T016 [US2] Update `GraphEditorApp` to delegate `onKeyEvent` to the extracted handler. Verify compile + tests pass

### Extract Canvas Section

- [X] T017 [US2] Create `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorCanvas.kt` — extract the canvas section (Main.kt lines ~849–1045) as `@Composable fun GraphEditorCanvasSection(...)` containing the `FlowGraphCanvas` call and all its callbacks (onNodeSelected, onConnectionCreated, onNodeMoved, navigation, etc.)
- [X] T018 [US2] Update `GraphEditorApp` to call `GraphEditorCanvasSection()` in place of the inline canvas block. Verify compile + tests pass

### Extract File Operation Dialogs

- [X] T019 [US2] Create `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorDialogs.kt` — extract all dialog LaunchedEffects and dialog composables (Main.kt lines ~1306–1522) as `@Composable fun GraphEditorDialogs(...)` containing: file open, save module, FlowGraph properties, remove repository module confirmation, and unsaved changes dialogs
- [X] T020 [US2] Update `GraphEditorApp` to call `GraphEditorDialogs()` in place of the inline dialog blocks. Verify compile + tests pass

### Extract Main Layout

- [X] T021 [US2] Create `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorLayout.kt` — extract the main panel layout (Main.kt lines ~604–1293) as `@Composable fun GraphEditorContent(...)` containing the Row with node panel, IP panel, canvas section, properties panel, runtime panel, and overlay controls
- [X] T022 [US2] Update `GraphEditorApp` to call `GraphEditorContent()` in place of the inline layout. Verify compile + tests pass

### Verification

- [X] T023 [US2] Run `./gradlew :graphEditor:jvmTest` — full test suite must pass with zero failures
- [X] T024 [US2] Verify `GraphEditorApp` is now a high-level orchestrator that composes: `rememberGraphEditorState()`, `SharedStateProvider`, `TopToolbar`, `GraphEditorContent`, `StatusBar`, `GraphEditorDialogs` — Main.kt reduced from 1923 to 599 lines (69% reduction), GraphEditorApp is ~500 lines (ViewModel init + LaunchedEffects + orchestration)

**Checkpoint**: GraphEditorApp is decomposed into ~6 sub-composables. Each file handles one concern. All tests pass.

---

## Phase 4: User Story 3 - Reduce Main.kt to Entry Point Only (Priority: P3)

**Goal**: Move `GraphEditorApp` out of Main.kt so it contains only the `main()` function.

**Independent Test**: `wc -l Main.kt` shows fewer than 50 lines. Application launches correctly.

- [X] T025 [US3] Move `GraphEditorApp` composable and `GraphEditorAppPreview` to `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorApp.kt` — Main.kt retains only `main()` function, Koin init, and window setup
- [X] T026 [US3] Update imports in Main.kt to reference `GraphEditorApp` from its new location. Verify compile + tests pass
- [X] T027 [US3] Verify Main.kt is under 50 lines with `wc -l`
- [X] T028 [US3] Run `./gradlew :graphEditor:jvmTest` — full test suite must pass

**Checkpoint**: Main.kt contains only `main()`. All graph editor UI logic lives in `ui/` and `util/` packages.

---

## Phase 5: Polish & Verification

**Purpose**: Full validation and file size checks

- [X] T029 Run full test suite `./gradlew :graphEditor:jvmTest` to verify no regressions
- [X] T030 Verify no single file exceeds 500 lines: check all new files with `wc -l`
- [X] T031 Run quickstart.md verification scenarios (VS1–VS6)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — establishes baseline
- **US1 (Phase 2)**: Depends on Setup — extract self-contained functions first
- **US2 (Phase 3)**: Depends on US1 — decompose the now-smaller GraphEditorApp
- **US3 (Phase 4)**: Depends on US2 — move the now-small GraphEditorApp to its own file
- **Polish (Phase 5)**: Depends on US3 completion

### User Story Dependencies

- **US1 → US2 → US3**: Sequential dependency chain (each reduces Main.kt further)
- US1 must complete before US2 because US2 decomposes the function that US1 has already shrunk
- US3 is the final move after US2 has made GraphEditorApp small enough to be a standalone file

### Within Each User Story

- Extract to new file → Update caller imports → Verify compile + tests
- Each extraction is atomic — rollback is trivial (revert the file move)

### Parallel Opportunities

- T006, T007, T008 can run in parallel (different new files, no dependencies)
- Within US2, each sub-composable extraction is sequential (each modifies GraphEditorApp)

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (baseline)
2. Complete Phase 2: US1 (extract self-contained functions)
3. **STOP and VALIDATE**: ~600 lines removed from Main.kt, all tests green
4. Proceed to Phase 3 for GraphEditorApp decomposition

### Incremental Delivery

1. Setup → Baseline green
2. US1 → TopToolbar, StatusBar, utilities extracted → Validate
3. US2 → GraphEditorApp decomposed into sub-composables → Validate
4. US3 → Main.kt reduced to entry point → Validate
5. Polish → Full verification

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- TDD verification at each step: compile succeeds + all existing tests pass
- This is a pure structural refactoring — NO logic changes, NO new features
- Each extraction step should be a single commit for easy rollback
- Visibility changes: `private` functions extracted to other files become `internal`
- State sharing: Use parameter passing for sub-composables, state holder class for initialization
