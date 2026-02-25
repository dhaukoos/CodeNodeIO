# Tasks: GraphEditor Save Functionality

**Input**: Design documents from `/specs/029-grapheditor-save/`
**Prerequisites**: plan.md, spec.md, research.md, quickstart.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- **graphEditor service**: `graphEditor/src/jvmMain/kotlin/save/`
- **graphEditor UI**: `graphEditor/src/jvmMain/kotlin/Main.kt`
- **graphEditor tests**: `graphEditor/src/jvmTest/kotlin/save/`

---

## Phase 1: Setup

**Purpose**: No setup tasks needed — all infrastructure already exists. The graphEditor module has established save/compile patterns.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Enhance ModuleSaveResult and create the unified saveModule() that all user stories depend on.

- [ ] T001 Add `filesOverwritten: List<String>` and `filesDeleted: List<String>` fields to `ModuleSaveResult` data class in `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt`
- [ ] T002 Create unified `saveModule()` that merges the current `saveModule()` (module dir, gradle files, .flow.kt) and `compileModule()` (runtime files, stubs, state properties) into a single method. The unified method must: create module directory and structure, write gradle files, write .flow.kt at module root, generate 5 runtime files (always overwrite), generate processing logic stubs (don't overwrite existing), generate state properties stubs (don't overwrite existing), delete orphaned processing logic stubs, delete orphaned state properties stubs. Track files in `filesCreated`, `filesOverwritten`, and `filesDeleted` lists in `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt`
- [ ] T003 Update existing tests in `ModuleSaveServiceTest.kt` to use the unified `saveModule()` instead of separate `saveModule()`/`compileModule()` calls. Verify a single `saveModule()` call creates the full module: .flow.kt, gradle files, 5 runtime files, stubs, state properties. Verify `filesOverwritten` and `filesDeleted` fields are populated correctly in `graphEditor/src/jvmTest/kotlin/save/ModuleSaveServiceTest.kt`

**Checkpoint**: Unified saveModule() is complete and tested — UI integration can now begin

---

## Phase 3: User Story 1 - Save New FlowGraph to Disk (Priority: P1) MVP

**Goal**: When saving a FlowGraph for the first time, prompt for a directory and create the full module.

**Independent Test**: Create a FlowGraph, call saveModule() with a temp directory, verify full module structure is created with all file categories.

### Implementation for User Story 1

- [ ] T004 [US1] Replace `lastSaveOutputDir: File?` state variable with `saveLocationRegistry` (`MutableMap<String, File>`) that maps FlowGraph names to their save directories in `graphEditor/src/jvmMain/kotlin/Main.kt`
- [ ] T005 [US1] Rewrite the Save button `LaunchedEffect` handler: look up `saveLocationRegistry[flowGraph.name]` — if not found, show directory chooser, call unified `saveModule()`, store result directory in registry, display result status message. Handle cancel (user closes directory chooser → no changes) in `graphEditor/src/jvmMain/kotlin/Main.kt`
- [ ] T006 [US1] Add integration test: first save with unified `saveModule()` creates full module (module dir, .flow.kt, gradle files, 5 runtime files, processing logic stubs, state properties stubs) and `filesCreated` includes all files in `graphEditor/src/jvmTest/kotlin/save/ModuleSaveServiceTest.kt`

**Checkpoint**: First-time save creates complete module from single Save action

---

## Phase 4: User Story 2 - Re-Save Updated FlowGraph (Priority: P1)

**Goal**: When re-saving a FlowGraph with the same name, skip the directory prompt and update the existing module.

**Independent Test**: Save a FlowGraph, modify it (add a node), save again, verify .flow.kt and runtime files updated, existing stubs preserved, new stubs created.

### Implementation for User Story 2

- [ ] T007 [US2] Extend the Save handler in Main.kt: if `saveLocationRegistry[flowGraph.name]` exists AND the directory still exists on disk, call `saveModule()` directly (no directory chooser). If directory no longer exists, remove from registry and fall through to first-save flow (FR-010) in `graphEditor/src/jvmMain/kotlin/Main.kt`
- [ ] T008 [US2] Add integration tests for re-save: verify .flow.kt is overwritten with updated content, all 5 runtime files are overwritten, existing processing logic stubs are NOT overwritten, existing state properties stubs are NOT overwritten, new stubs are created for added nodes, `filesOverwritten` includes .flow.kt and runtime files, `filesCreated` includes only new stub files in `graphEditor/src/jvmTest/kotlin/save/ModuleSaveServiceTest.kt`

**Checkpoint**: Re-save updates generated files, preserves user stubs, creates new stubs

---

## Phase 5: User Story 3 - Delete Stubs for Removed Nodes (Priority: P1)

**Goal**: On save, delete orphaned stub files for nodes that no longer exist in the FlowGraph.

**Independent Test**: Save a FlowGraph with 3 nodes, remove one node, save again, verify orphaned stubs deleted and remaining stubs preserved.

### Implementation for User Story 3

- [ ] T009 [US3] Add integration tests for orphan deletion: save a FlowGraph with 3 nodes (generator + sink + transformer), remove the transformer, re-save with unified `saveModule()`, verify the transformer's processing logic stub is deleted, verify the transformer's state properties stub is deleted, verify the generator and sink stubs are preserved, verify `filesDeleted` contains the deleted file paths in `graphEditor/src/jvmTest/kotlin/save/ModuleSaveServiceTest.kt`

**Checkpoint**: Orphaned stubs are deleted on save, remaining stubs preserved

---

## Phase 6: User Story 4 - Save FlowGraph Under a New Name (Priority: P2)

**Goal**: When the FlowGraph name changes, treat as a new save (prompt for directory).

**Independent Test**: Save a FlowGraph named "Alpha", rename to "Beta", save again, verify directory prompt appears and new module created.

### Implementation for User Story 4

- [ ] T010 [US4] Add integration tests for name change: save as "Alpha" (stored in registry), rename FlowGraph to "Beta", verify registry lookup for "Beta" returns null (triggering directory prompt), save "Beta" to a new location, verify "Alpha" module is untouched, verify "Beta" module is a complete new module in `graphEditor/src/jvmTest/kotlin/save/ModuleSaveServiceTest.kt`

**Checkpoint**: Name changes trigger new save flow, old module preserved

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Clean up redundant UI elements and validate end-to-end

- [ ] T011 Remove Compile button, `showCompileDialog` state variable, `onCompile` callback, and compile `LaunchedEffect` block from `graphEditor/src/jvmMain/kotlin/Main.kt`
- [ ] T012 Deprecate or remove standalone `compileModule()` method from `ModuleSaveService.kt` (keep if needed for backward compatibility with tests, otherwise remove) in `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt`
- [ ] T013 Run full test suite: `./gradlew :graphEditor:jvmTest` and `./gradlew :kotlinCompiler:allTests` to verify all tests pass
- [ ] T014 Run quickstart.md validation: verify all 11 checklist items from quickstart.md against a StopWatch3-like FlowGraph lifecycle (first save, re-save with added node, re-save with removed node, save under new name)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 2)**: No dependencies — can start immediately (T001-T003)
- **User Story 1 (Phase 3)**: Depends on Phase 2 (unified saveModule) — T004-T006
- **User Story 2 (Phase 4)**: Depends on US1 (save handler + registry exist) — T007-T008
- **User Story 3 (Phase 5)**: Depends on Phase 2 (orphan deletion in unified saveModule) — T009
- **User Story 4 (Phase 6)**: Depends on US1 (registry-based lookup) — T010
- **Polish (Phase 7)**: Depends on all user stories — T011-T014

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Foundational only — implements save handler + registry
- **User Story 2 (P1)**: Depends on US1 — extends save handler with re-save path
- **User Story 3 (P1)**: Depends on Foundational only — orphan deletion already in unified saveModule, just needs tests
- **User Story 4 (P2)**: Depends on US1 — name change detection via registry lookup

### Within Each Phase

- Service-level changes (ModuleSaveService) before UI changes (Main.kt)
- Implementation before tests within the same phase

### Parallel Opportunities

- **Phase 2**: T001 before T002 (same file), T003 follows
- **US3 and US4**: T009 and T010 can run in parallel after US1 completes (different test scenarios, no overlapping code)

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational (unified saveModule)
2. Complete Phase 3: User Story 1 (save handler + registry)
3. **STOP and VALIDATE**: Save a FlowGraph and verify full module is created
4. Proceed to US2 + US3 for iterative save workflow

### Incremental Delivery

1. Phase 2 (Foundational) → Unified saveModule() ready
2. US1 → First-time save works (MVP!)
3. US2 → Re-save without directory prompt
4. US3 → Orphaned stubs deleted
5. US4 → Name change detection
6. Polish → Compile button removed, full validation

---

## Notes

- The unified saveModule() replaces both the old saveModule() and compileModule() — this is the core refactoring
- Orphan deletion (US3) is built into the unified method from T002, so US3 tasks are primarily tests
- The save location registry is session-scoped (in-memory Map) — no persistence needed
- Existing ModuleSaveServiceTest tests will need updating since they currently call saveModule() and compileModule() separately
- The Compile button removal (T011) should happen last to avoid breaking the UI during development
