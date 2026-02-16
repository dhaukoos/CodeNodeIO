# Tasks: ViewModel Pattern Migration

**Input**: Design documents from `/specs/017-viewmodel-pattern/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Tests**: Tests are included as this feature enables testability as a core goal (User Story 2).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **graphEditor module**: `graphEditor/src/jvmMain/kotlin/`
- **Test files**: `graphEditor/src/jvmTest/kotlin/`
- **ViewModel package**: `graphEditor/src/jvmMain/kotlin/viewmodel/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add ViewModel dependency and create package structure

- [x] T001 Add `lifecycle-viewmodel-compose:2.10.0-alpha06` dependency to `graphEditor/build.gradle.kts`
- [x] T002 Create `viewmodel/` package directory at `graphEditor/src/jvmMain/kotlin/viewmodel/`
- [x] T003 [P] Create `viewmodel/` test package directory at `graphEditor/src/jvmTest/kotlin/viewmodel/`
- [x] T004 [P] Create `SharedStateProvider.kt` data class with GraphState, UndoRedoManager, PropertyChangeTracker, IPTypeRegistry, CustomNodeRepository fields at `graphEditor/src/jvmMain/kotlin/viewmodel/SharedStateProvider.kt`
- [x] T005 Create `LocalSharedState` CompositionLocal in `graphEditor/src/jvmMain/kotlin/viewmodel/SharedStateProvider.kt`

**Checkpoint**: ✅ Foundation ready - ViewModel infrastructure in place

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish base ViewModel pattern that all user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T006 Create `BaseState.kt` marker interface for ViewModel state data classes at `graphEditor/src/jvmMain/kotlin/viewmodel/BaseState.kt`
- [x] T007 Wrap existing state classes in `CompositionLocalProvider(LocalSharedState provides sharedState)` in Main.kt at `graphEditor/src/jvmMain/kotlin/Main.kt`
- [x] T008 Verify existing tests still pass after CompositionLocal integration

**Checkpoint**: ✅ Foundation complete - user story implementation can now begin

---

## Phase 3: User Story 1 - Consistent State Management (Priority: P1) 🎯 MVP

**Goal**: Each major UI component has a corresponding ViewModel that encapsulates all its state

**Independent Test**: After migration, every UI component's state and logic can be traced to a single ViewModel class, and no business logic remains in composable functions.

### Implementation for User Story 1

#### NodeGeneratorViewModel (Simplest - Start Here)

- [x] T009 [US1] Create `NodeGeneratorPanelState` data class with name, inputCount, outputCount, isExpanded, dropdownExpanded fields at `graphEditor/src/jvmMain/kotlin/viewmodel/NodeGeneratorViewModel.kt`
- [x] T010 [US1] Create `NodeGeneratorViewModel` class extending ViewModel with StateFlow<NodeGeneratorPanelState> at `graphEditor/src/jvmMain/kotlin/viewmodel/NodeGeneratorViewModel.kt`
- [x] T011 [US1] Implement setName(), setInputCount(), setOutputCount(), toggleExpanded(), createNode(), reset() actions in `NodeGeneratorViewModel`
- [x] T012 [US1] Refactor `NodeGeneratorPanel.kt` to use `NodeGeneratorViewModel` instead of local mutableStateOf calls at `graphEditor/src/jvmMain/kotlin/ui/NodeGeneratorPanel.kt`
- [x] T013 [US1] Remove business logic from `NodeGeneratorPanel.kt` composable - keep only UI rendering

#### NodePaletteViewModel

- [x] T014 [P] [US1] Create `NodePaletteState` data class with searchQuery, expandedCategories, deletableNodeNames fields at `graphEditor/src/jvmMain/kotlin/viewmodel/NodePaletteViewModel.kt`
- [x] T015 [US1] Create `NodePaletteViewModel` class with StateFlow<NodePaletteState> at `graphEditor/src/jvmMain/kotlin/viewmodel/NodePaletteViewModel.kt`
- [x] T016 [US1] Implement setSearchQuery(), toggleCategory(), deleteCustomNode() actions in `NodePaletteViewModel`
- [x] T017 [US1] Refactor `NodePalette.kt` to use `NodePaletteViewModel` at `graphEditor/src/jvmMain/kotlin/ui/NodePalette.kt`

#### IPPaletteViewModel

- [x] T018 [P] [US1] Create `IPPaletteState` data class with allTypes, selectedTypeId, expandedCategories fields at `graphEditor/src/jvmMain/kotlin/viewmodel/IPPaletteViewModel.kt`
- [x] T019 [US1] Create `IPPaletteViewModel` class with StateFlow<IPPaletteState> at `graphEditor/src/jvmMain/kotlin/viewmodel/IPPaletteViewModel.kt`
- [x] T020 [US1] Implement selectType(), clearSelection(), toggleCategory() actions in `IPPaletteViewModel`
- [x] T021 [US1] Refactor `IPPalette.kt` to use `IPPaletteViewModel` at `graphEditor/src/jvmMain/kotlin/ui/IPPalette.kt`

#### PropertiesPanelViewModel

- [x] T022 [P] [US1] Create `PropertiesPanelViewModelState` data class with selectedNode, selectedConnection, editingPropertyKey, pendingChanges, validationErrors fields at `graphEditor/src/jvmMain/kotlin/viewmodel/PropertiesPanelViewModel.kt`
- [x] T023 [US1] Create `PropertiesPanelViewModel` class with StateFlow at `graphEditor/src/jvmMain/kotlin/viewmodel/PropertiesPanelViewModel.kt`
- [x] T024 [US1] Implement startEditing(), updatePendingChange(), commitChanges(), cancelEditing(), updateNodeName(), updatePortName() actions
- [x] T025 [US1] Refactor `PropertiesPanel.kt` to use `PropertiesPanelViewModel` at `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`

#### CanvasInteractionViewModel

- [ ] T026 [P] [US1] Create `CanvasInteractionState` data class with draggingNodeId, pendingConnection, selectionBoxBounds, hoveredNodeId, hoveredPort, connectionContextMenu, interactionMode fields at `graphEditor/src/jvmMain/kotlin/viewmodel/CanvasInteractionViewModel.kt`
- [ ] T027 [US1] Create `CanvasInteractionViewModel` class with StateFlow at `graphEditor/src/jvmMain/kotlin/viewmodel/CanvasInteractionViewModel.kt`
- [ ] T028 [US1] Implement drag actions: startNodeDrag(), updateNodeDrag(), endNodeDrag()
- [ ] T029 [US1] Implement connection actions: startConnectionCreation(), updateConnectionEndpoint(), completeConnection(), cancelConnection()
- [ ] T030 [US1] Implement selection actions: startRectangularSelection(), updateRectangularSelection(), finishRectangularSelection()
- [ ] T031 [US1] Implement hover/menu actions: showConnectionContextMenu(), hideConnectionContextMenu(), setHoveredNode(), setHoveredPort()
- [ ] T032 [US1] Refactor `FlowGraphCanvas.kt` to use `CanvasInteractionViewModel` - move business state, keep transient gesture state at `graphEditor/src/jvmMain/kotlin/ui/FlowGraphCanvas.kt`

#### GraphEditorViewModel (Orchestration - Last)

- [ ] T033 [P] [US1] Create `GraphEditorState` data class with dialog flags, statusMessage, canGroup, canUngroup, isInsideGraphNode at `graphEditor/src/jvmMain/kotlin/viewmodel/GraphEditorViewModel.kt`
- [ ] T034 [US1] Create `GraphEditorViewModel` class with StateFlow at `graphEditor/src/jvmMain/kotlin/viewmodel/GraphEditorViewModel.kt`
- [ ] T035 [US1] Implement file actions: createNewGraph(), openGraph(), saveGraph()
- [ ] T036 [US1] Implement edit actions: undo(), redo(), groupSelectedNodes(), ungroupSelectedNode()
- [ ] T037 [US1] Implement navigation actions: navigateBack(), compile()
- [ ] T038 [US1] Implement dialog actions: showDialog(), hideDialog(), setStatusMessage()
- [ ] T039 [US1] Refactor `Main.kt` to use `GraphEditorViewModel` - reduce to layout and orchestration only at `graphEditor/src/jvmMain/kotlin/Main.kt`

**Checkpoint**: ✅ User Story 1 complete - all UI components have dedicated ViewModels

---

## Phase 4: User Story 2 - Improved Testability (Priority: P2)

**Goal**: Unit tests can be written for ViewModel classes that verify state changes, business logic, and side effects without any Compose UI dependencies

**Independent Test**: Every ViewModel can be instantiated and tested without Compose UI imports

### Tests for User Story 2

- [ ] T040 [P] [US2] Create `NodeGeneratorViewModelTest.kt` with state transition tests at `graphEditor/src/jvmTest/kotlin/viewmodel/NodeGeneratorViewModelTest.kt`
- [ ] T041 [P] [US2] Create `NodePaletteViewModelTest.kt` with search and category expansion tests at `graphEditor/src/jvmTest/kotlin/viewmodel/NodePaletteViewModelTest.kt`
- [ ] T042 [P] [US2] Create `IPPaletteViewModelTest.kt` with selection tests at `graphEditor/src/jvmTest/kotlin/viewmodel/IPPaletteViewModelTest.kt`
- [ ] T043 [P] [US2] Create `PropertiesPanelViewModelTest.kt` with editing and validation tests at `graphEditor/src/jvmTest/kotlin/viewmodel/PropertiesPanelViewModelTest.kt`
- [ ] T044 [P] [US2] Create `CanvasInteractionViewModelTest.kt` with drag, connection, and selection tests at `graphEditor/src/jvmTest/kotlin/viewmodel/CanvasInteractionViewModelTest.kt`
- [ ] T045 [P] [US2] Create `GraphEditorViewModelTest.kt` with file and edit action tests at `graphEditor/src/jvmTest/kotlin/viewmodel/GraphEditorViewModelTest.kt`

### Implementation for User Story 2

- [ ] T046 [US2] Verify all ViewModel tests pass without any `androidx.compose` imports in test files
- [ ] T047 [US2] Add test for undo/redo through GraphEditorViewModel at `graphEditor/src/jvmTest/kotlin/viewmodel/GraphEditorViewModelTest.kt`
- [ ] T048 [US2] Add test for node creation flow through NodeGeneratorViewModel at `graphEditor/src/jvmTest/kotlin/viewmodel/NodeGeneratorViewModelTest.kt`

**Checkpoint**: ✅ User Story 2 complete - all ViewModels testable without Compose

---

## Phase 5: User Story 3 - Modular Component Architecture (Priority: P3)

**Goal**: New UI components can be added with their own ViewModels without modifying existing ViewModel classes

**Independent Test**: A new UI component can be added with its own ViewModel without modifying existing ViewModel classes

### Implementation for User Story 3

- [ ] T049 [US3] Document ViewModel creation pattern in `graphEditor/README.md` or code comments
- [ ] T050 [US3] Verify ViewModels communicate through SharedStateProvider, not direct references
- [ ] T051 [US3] Add interface documentation for ViewModel state and action contracts in `graphEditor/src/jvmMain/kotlin/viewmodel/SharedStateProvider.kt`
- [ ] T052 [US3] Create example/template for adding a new ViewModel in quickstart.md at `specs/017-viewmodel-pattern/quickstart.md`

**Checkpoint**: ✅ User Story 3 complete - modular architecture documented and verified

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and cleanup

- [ ] T053 [P] Verify all existing automated tests pass (SC-002)
- [ ] T054 [P] Verify all existing editor features work identically (SC-003) - manual testing
- [ ] T055 Code review: confirm no business logic in composable functions (SC-005)
- [ ] T056 Verify all ViewModel classes follow `*ViewModel` naming convention (SC-006)
- [ ] T057 Verify 100% of scattered `remember { mutableStateOf() }` for business state moved to ViewModels (SC-001)
- [ ] T058 Run quickstart.md validation checklist at `specs/017-viewmodel-pattern/quickstart.md`

**Checkpoint**: ✅ All success criteria validated

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational - extracts all ViewModels
- **User Story 2 (Phase 4)**: Depends on User Story 1 - adds tests for ViewModels
- **User Story 3 (Phase 5)**: Depends on User Story 1 - documents modularity
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational - creates all ViewModels
- **User Story 2 (P2)**: Requires User Story 1 complete - tests the ViewModels
- **User Story 3 (P3)**: Requires User Story 1 complete - documents the patterns

### Within User Story 1 (ViewModel Extraction Order)

Per research.md Decision 3:
1. NodeGeneratorViewModel (simplest, isolated) - T009-T013
2. NodePaletteViewModel - T014-T017
3. IPPaletteViewModel - T018-T021
4. PropertiesPanelViewModel - T022-T025
5. CanvasInteractionViewModel (most complex) - T026-T032
6. GraphEditorViewModel (orchestration, last) - T033-T039

### Parallel Opportunities

- T003 and T004 can run in parallel (different directories)
- T014, T018, T022, T026, T033 can run in parallel (creating state data classes)
- T040-T045 can run in parallel (different test files)
- T053-T056 can run in parallel (independent validation tasks)

---

## Parallel Example: ViewModel State Classes

```bash
# Launch all state data class tasks together:
Task: "Create NodePaletteState data class"
Task: "Create IPPaletteState data class"
Task: "Create PropertiesPanelViewModelState data class"
Task: "Create CanvasInteractionState data class"
Task: "Create GraphEditorState data class"
```

## Parallel Example: ViewModel Tests

```bash
# Launch all ViewModel test tasks together:
Task: "Create NodeGeneratorViewModelTest.kt"
Task: "Create NodePaletteViewModelTest.kt"
Task: "Create IPPaletteViewModelTest.kt"
Task: "Create PropertiesPanelViewModelTest.kt"
Task: "Create CanvasInteractionViewModelTest.kt"
Task: "Create GraphEditorViewModelTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (dependency + packages)
2. Complete Phase 2: Foundational (CompositionLocal integration)
3. Complete Phase 3: User Story 1 (all 6 ViewModels)
4. **STOP and VALIDATE**: Test all features work identically
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Complete NodeGeneratorViewModel → Test independently → First ViewModel done
3. Continue with remaining ViewModels one at a time
4. Add User Story 2 (tests) → All ViewModels testable
5. Add User Story 3 (documentation) → Patterns documented
6. Each step adds value without breaking previous work

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- ViewModel extraction order per research.md: simplest → most complex
- Keep transient gesture state (pointerPosition, shiftPressed) in FlowGraphCanvas per Decision 4
- Use JetBrains lifecycle-viewmodel-compose per Decision 1
- Tests verify StateFlow state transitions without Compose imports
