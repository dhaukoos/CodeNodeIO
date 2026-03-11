# Tasks: Remove Repository Module

**Input**: Design documents from `/specs/048-remove-entity-module/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Not explicitly requested — test tasks omitted.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup

**Purpose**: No new project setup needed — this feature modifies existing files only.

_(No tasks — all infrastructure already exists from feature 047.)_

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core removal logic that user story UI tasks depend on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T001 Implement `removeEntityModule()` method in `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt` — accepts entityName, moduleName, moduleDir, persistenceDir, projectDir, customNodeRepository, sourceIPTypeId; returns summary string
- [x] T002 Implement custom node removal logic within `removeEntityModule()` — filter `customNodeRepository.getAll()` by `sourceIPTypeId`, call `remove(id)` for each match, count removed
- [x] T003 Implement module directory deletion within `removeEntityModule()` — call `moduleDir.deleteRecursively()` if exists, track success
- [x] T004 Implement persistence file removal within `removeEntityModule()` — delete `{entityName}Entity.kt`, `{entityName}Dao.kt`, `{entityName}Repository.kt` from persistenceDir, skip missing files
- [x] T005 Implement AppDatabase regeneration within `removeEntityModule()` — reuse existing scan pattern from `regenerateAppDatabase()` to scan remaining `*Entity.kt` files and regenerate `AppDatabase.kt`
- [x] T006 Implement Gradle entry removal within `removeEntityModule()` — remove `include(":moduleName")` from `settings.gradle.kts` and `implementation(project(":moduleName"))` from `graphEditor/build.gradle.kts` by reading lines, filtering, writing back

**Checkpoint**: Removal service is complete and can be called from UI code.

---

## Phase 3: User Story 1 — Remove an existing entity module (Priority: P1) 🎯 MVP

**Goal**: Replace the disabled "Module exists" label with a "Remove Repository Module" button that removes all generated artifacts when clicked.

**Independent Test**: Create a module for an IP Type via "Create Repository Module", then click "Remove Repository Module" and verify all 7 artifact categories are cleaned up and the "Create Repository Module" button reappears.

### Implementation for User Story 1

- [x] T007 [US1] Update `IPTypePropertiesPanel` in `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt` — add `onRemoveRepositoryModule` callback parameter, replace disabled "Module exists" button with enabled "Remove Repository Module" button when `moduleExists` is true
- [x] T008 [US1] Add `onRemoveRepositoryModule` lambda in `graphEditor/src/jvmMain/kotlin/Main.kt` — wire the removal button to call `moduleSaveService.removeEntityModule()` with correct parameters derived from selected IP Type
- [x] T009 [US1] Reload `customNodes` state after removal in `graphEditor/src/jvmMain/kotlin/Main.kt` — set `customNodes = customNodeRepository.getAll()` after removal completes so the UI reflects the updated state and "Create Repository Module" button reappears

**Checkpoint**: User Story 1 is fully functional — modules can be created and removed via UI buttons.

---

## Phase 4: User Story 2 — Confirmation dialog before removal (Priority: P2)

**Goal**: Show a confirmation dialog when "Remove Repository Module" is clicked, preventing accidental deletion.

**Independent Test**: Click "Remove Repository Module", verify dialog appears, click Cancel and confirm no artifacts were removed. Click again, confirm, and verify artifacts are removed.

### Implementation for User Story 2

- [x] T010 [US2] Add `showRemoveConfirmDialog` state variable and `entityToRemove` tracking state in `graphEditor/src/jvmMain/kotlin/Main.kt`
- [x] T011 [US2] Implement `AlertDialog` composable in `graphEditor/src/jvmMain/kotlin/Main.kt` — title "Remove Module", message includes entity name, "Remove" confirm button triggers removal, "Cancel" button dismisses dialog
- [x] T012 [US2] Update the `onRemoveRepositoryModule` lambda in `graphEditor/src/jvmMain/kotlin/Main.kt` — instead of calling removal directly, set `showRemoveConfirmDialog = true` and store the selected IP Type info; move actual removal logic into the dialog's confirm action

**Checkpoint**: User Stories 1 AND 2 work together — removal requires explicit confirmation.

---

## Phase 5: User Story 3 — Status feedback after removal (Priority: P3)

**Goal**: Display a status message summarizing what was removed after the operation completes.

**Independent Test**: Remove a module and verify the status bar message reflects the outcome (e.g., "Removed GeoLocations module: 3 nodes, module directory, 3 persistence files, AppDatabase updated, 2 gradle entries").

### Implementation for User Story 3

- [x] T013 [US3] Update `removeEntityModule()` return value in `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt` — build a detailed summary string tracking counts for each artifact category (nodes removed, directory deleted, persistence files removed, AppDatabase regenerated, gradle entries removed)
- [x] T014 [US3] Display removal result in status bar in `graphEditor/src/jvmMain/kotlin/Main.kt` — set `statusMessage` state with the summary string returned from `removeEntityModule()` after the confirmation dialog's confirm action completes

**Checkpoint**: All three user stories are complete — removal with confirmation and status feedback.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Edge case handling and graceful degradation.

- [x] T015 Verify graceful handling of partial cleanup in `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt` — ensure each removal step is wrapped in try/catch so missing artifacts don't prevent remaining cleanup
- [ ] T016 Run quickstart.md validation — create a test module, remove it, verify all artifacts cleaned up and create button reappears

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 2)**: No dependencies — can start immediately
- **User Story 1 (Phase 3)**: Depends on Phase 2 (removal service must exist)
- **User Story 2 (Phase 4)**: Depends on Phase 3 (button must exist to add dialog)
- **User Story 3 (Phase 5)**: Depends on Phase 2 (removal service return value)
- **Polish (Phase 6)**: Depends on all user stories complete

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Foundational — no dependencies on other stories
- **User Story 2 (P2)**: Depends on User Story 1 (wraps the removal button with dialog)
- **User Story 3 (P3)**: Can start after Foundational but integrates with US1/US2 for status display

### Within Each User Story

- Service logic before UI wiring
- UI button before dialog wrapping
- Core implementation before status feedback

### Parallel Opportunities

- T002, T003, T004, T005, T006 are logically sequential within `removeEntityModule()` but could be developed as independent helper methods in parallel
- T013 (status string) can be developed in parallel with T010-T012 (dialog) since they modify different aspects
- T007 (PropertiesPanel) can be developed in parallel with T008 (Main.kt wiring)

---

## Parallel Example: Foundational Phase

```bash
# T002-T006 can be developed as independent private methods within ModuleSaveService:
Task: "Implement removeCustomNodes() helper"
Task: "Implement deleteModuleDirectory() helper"
Task: "Implement removePersistenceFiles() helper"
Task: "Implement regenerateAppDatabaseAfterRemoval() helper"
Task: "Implement removeGradleEntries() helper"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational (removal service)
2. Complete Phase 3: User Story 1 (button + wiring)
3. **STOP and VALIDATE**: Create module → Remove module → Verify cleanup
4. Functional removal without confirmation dialog

### Incremental Delivery

1. Foundational → Removal service ready
2. Add User Story 1 → Test removal → Functional MVP
3. Add User Story 2 → Test confirmation dialog → Safe removal
4. Add User Story 3 → Test status feedback → Complete feature
5. Each story adds UX value without breaking previous stories

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- All removal steps must be tolerant of missing artifacts (graceful degradation)
- Commit after each phase completion
- Stop at any checkpoint to validate independently
