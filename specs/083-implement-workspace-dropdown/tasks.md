# Tasks: Implement Module as Workspace via Dropdown

**Input**: Design documents from `/specs/083-implement-workspace-dropdown/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, quickstart.md
**Design reference**: `specs/082-module-flowgraph-ux-design/ux-design.md` Section 2.4 (Variation D)

**Tests**: Unit tests for WorkspaceViewModel state management.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Create the WorkspaceViewModel that manages module context, active flowGraph, and MRU list. All UI changes depend on this.

- [ ] T001 Create `WorkspaceViewModel` with `currentModuleDir`, `currentModuleName`, `activeFlowGraphName`, `mruModules`, `isDirty` as StateFlow properties, plus `openModule()`, `createModule()`, `switchModule()`, `setActiveFlowGraph()`, `markDirty()`, `markClean()` methods in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/viewmodel/WorkspaceViewModel.kt`
- [ ] T002 Add workspace persistence: `persistState()` writes `workspace.current` and `workspace.mru` to `~/.codenode/config.properties`; `restoreState()` reads them on startup in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/viewmodel/WorkspaceViewModel.kt`
- [ ] T003 Add unit tests for WorkspaceViewModel: openModule sets state, switchModule updates MRU, persistState/restoreState round-trip, markDirty/markClean in `graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/viewmodel/WorkspaceViewModelTest.kt`
- [ ] T004 Compile and run: `./gradlew :graphEditor:jvmTest`

**Checkpoint**: WorkspaceViewModel manages workspace state with persistence. All user stories can use it.

---

## Phase 2: User Story 1 — Module Dropdown in Toolbar (Priority: P1) 🎯 MVP

**Goal**: Replace gear icon + label with module dropdown. Title bar shows flowGraph name.

**Independent Test**: Launch editor. Dropdown visible. Select module. Title bar updates.

### Implementation

- [ ] T005 [P] [US1] Create `ModuleDropdown` composable with `OutlinedButton` showing module name + `▼`, `DropdownMenu` with current module (checkmarked), MRU list, dividers, "Open Module...", "Create New Module...", "Module Settings..." in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/ModuleDropdown.kt`
- [ ] T006 [US1] Replace the gear icon + `flowGraphName` label in `TopToolbar.kt` with `ModuleDropdown`, passing `WorkspaceViewModel` state and callbacks in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/TopToolbar.kt`
- [ ] T007 [US1] Instantiate `WorkspaceViewModel` in `GraphEditorApp.kt`, call `restoreState()` on startup, pass to `TopToolbar` and `GraphEditorContent` in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorApp.kt`
- [ ] T008 [US1] Update `Main.kt` window title to dynamically show `"CodeNodeIO — ${activeFlowGraphName ?: ""}"` from `WorkspaceViewModel` in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/Main.kt`
- [ ] T009 [US1] Wire "Open Module..." dropdown action to show directory chooser filtered for directories containing `build.gradle.kts`, then call `workspaceViewModel.openModule()` in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorApp.kt`
- [ ] T010 [US1] Wire module switching: when MRU module selected, prompt for unsaved changes if `isDirty`, then call `workspaceViewModel.switchModule()` in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorApp.kt`
- [ ] T011 [US1] Compile and run: `./gradlew :graphEditor:compileKotlinJvm :graphEditor:jvmTest`

**Checkpoint**: Module dropdown visible in toolbar. Title bar shows flowGraph name. Module switching works.

---

## Phase 3: User Story 2 — Redesign Module Properties Dialog (Priority: P2)

**Goal**: Rename FlowGraph Properties to Module Properties. Add Create Module functionality with validation.

**Independent Test**: Open Module Properties via dropdown. Enter name + platforms. Create module.

### Implementation

- [ ] T012 [US2] Create `ModulePropertiesDialog` composable with: name field (empty default, 3+ char validation), platform checkboxes (1+ required), Create Module button (enabled when valid), edit mode (read-only name + path display) in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/ModulePropertiesDialog.kt`
- [ ] T013 [US2] Wire "Create Module" button to show directory chooser, then call `ModuleScaffoldingGenerator.generate()` and `workspaceViewModel.createModule()` in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/ModulePropertiesDialog.kt`
- [ ] T014 [US2] Wire "Create New Module..." and "Module Settings..." dropdown actions to show `ModulePropertiesDialog` in create mode and edit mode respectively in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorApp.kt`
- [ ] T015 [US2] Remove the old `FlowGraphPropertiesDialog` and its `showFlowGraphPropertiesDialog` state/callbacks from `GraphEditorApp.kt` and `GraphEditorDialogs.kt` in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/`
- [ ] T016 [US2] Compile and run: `./gradlew :graphEditor:compileKotlinJvm :graphEditor:jvmTest`

**Checkpoint**: Module Properties dialog works. Create Module produces scaffolding. Old FlowGraph Properties removed.

---

## Phase 4: User Story 3 — Redesign New/Open/Save (Priority: P3)

**Goal**: New shows name-only dialog. Open scoped to module. Save deterministic.

**Independent Test**: New → enter name → blank canvas. Save → no prompt. Open → scoped to flow/.

### Implementation

- [ ] T017 [P] [US3] Create `NewFlowGraphDialog` composable with single name field, validation (non-empty, no duplicate in module's flow/, valid filename), and confirm/cancel buttons in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/NewFlowGraphDialog.kt`
- [ ] T018 [US3] Wire "New" toolbar button to show `NewFlowGraphDialog`, on confirm create a blank `FlowGraph(name = enteredName)`, set as active via `workspaceViewModel.setActiveFlowGraph()`, update title bar in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorApp.kt`
- [ ] T019 [US3] Redesign "Open" to scope the file chooser to `{workspaceModuleDir}/src/commonMain/kotlin/{packagePath}/flow/` filtered to `.flow.kt` files. Add "Open from..." fallback via secondary action. When opening from different module, infer module and call `switchModule()` in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorDialogs.kt`
- [ ] T020 [US3] Redesign "Save" to write deterministically to `{workspaceModuleDir}/src/commonMain/kotlin/{packagePath}/flow/{flowGraphName}.flow.kt` with no directory chooser. Disable Save when no module loaded. Remove `saveLocationRegistry` usage in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorDialogs.kt`
- [ ] T021 [US3] Disable New, Open (scoped), and Save buttons when `workspaceViewModel.currentModuleDir` is null in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/TopToolbar.kt`
- [ ] T022 [US3] Compile and run: `./gradlew :graphEditor:compileKotlinJvm :graphEditor:jvmTest`

**Checkpoint**: New/Open/Save work within workspace context. Save is deterministic. Disabled without module.

---

## Phase 5: User Story 4 — Workspace Persistence and Edge Cases (Priority: P4)

**Goal**: Last workspace persists. Unsaved changes prompt. Edge cases handled.

**Independent Test**: Close and reopen app — same workspace. Modify + switch → prompt. Duplicate name → error.

### Implementation

- [ ] T023 [US4] Call `workspaceViewModel.persistState()` on module changes (open, create, switch) and on app close in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorApp.kt`
- [ ] T024 [US4] Add unsaved-changes prompt when switching modules: if `isDirty`, show dialog with Save / Don't Save / Cancel before proceeding in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorApp.kt`
- [ ] T025 [US4] Add duplicate flowGraph name validation in `NewFlowGraphDialog`: check `{moduleDir}/flow/` for existing `.flow.kt` with same name, show inline error in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/NewFlowGraphDialog.kt`
- [ ] T026 [US4] Handle deleted module directory: on Save or Open attempt, show error and open Module Properties for resolution in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorDialogs.kt`
- [ ] T027 [US4] Remove stale MRU entries: when a module directory no longer exists, silently remove it from the MRU list on next dropdown render in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/ModuleDropdown.kt`
- [ ] T028 [US4] Compile and run: `./gradlew :graphEditor:compileKotlinJvm :graphEditor:jvmTest`

**Checkpoint**: Workspace persists. Edge cases handled gracefully.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Full verification and backward compatibility

- [ ] T029 Run full test suite: `./gradlew :flowGraph-generate:jvmTest :graphEditor:jvmTest`
- [ ] T030 Run quickstart.md verification scenarios VS1–VS10
- [ ] T031 Verify all demo project modules (StopWatch, UserProfiles, Addresses, EdgeArtFilter, WeatherForecast) work with the new workspace model
- [ ] T032 Verify "Open from..." can still open .flow.kt files from non-standard locations (backward compatibility)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: No dependencies. Creates WorkspaceViewModel. BLOCKS all user stories.
- **User Story 1 (Phase 2)**: Depends on Foundational. Creates dropdown and title bar.
- **User Story 2 (Phase 3)**: Depends on US1 (dropdown triggers Module Properties).
- **User Story 3 (Phase 4)**: Depends on US1 + US2 (workspace context + module creation must work).
- **User Story 4 (Phase 5)**: Depends on US3 (persistence and edge cases polish the core).
- **Polish (Phase 6)**: Depends on all user stories.

### Parallel Opportunities

```text
# US1: ModuleDropdown composable can be created in parallel with toolbar wiring:
T005 (ModuleDropdown.kt) in parallel with T008 (Main.kt title bar)

# US3: NewFlowGraphDialog can be created in parallel with Open/Save redesign:
T017 (NewFlowGraphDialog.kt) in parallel
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Foundational (T001–T004) — WorkspaceViewModel
2. Complete Phase 2: User Story 1 (T005–T011) — Module dropdown + title bar
3. **STOP and VALIDATE**: Dropdown visible, module switching works, title bar shows flowGraph name
4. Old Module Properties still works as fallback during validation

### Incremental Delivery

1. Foundational → WorkspaceViewModel with persistence
2. User Story 1 → Module dropdown in toolbar + title bar (MVP!)
3. User Story 2 → Module Properties dialog redesign
4. User Story 3 → New/Open/Save redesign
5. User Story 4 → Persistence + edge cases
6. Polish → Full verification + backward compatibility

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story
- The `saveLocationRegistry` is replaced by `WorkspaceViewModel.currentModuleDir` — removed in US3
- The old `FlowGraphPropertiesDialog` is replaced by `ModulePropertiesDialog` — removed in US2
- The "Save" button remains in the toolbar — only its behavior changes (deterministic, no prompt)
- Commit after each phase
