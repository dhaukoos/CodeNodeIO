# Tasks: GraphNode Creation Support

**Input**: Design documents from `/specs/005-graphnode-creation/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Tests are REQUIRED per project constitution (TDD mandatory). All test tasks must be completed before implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4, US5, US6)
- Include exact file paths in descriptions

## Path Conventions

- **Multi-module KMP project**: `fbpDsl/`, `graphEditor/` at repository root
- Paths assume multi-module structure per plan.md

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure for GraphNode creation feature

- [X] T001 Verify existing GraphNode model has required properties (childNodes, internalConnections, portMappings) in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/GraphNode.kt
- [X] T002 [P] Create factory package directory at fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/
- [X] T003 [P] Create state test directory at graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Tests for Foundation (TDD - Write FIRST, must FAIL) ⚠️

- [X] T004 [P] Write unit tests for SelectionState data class (nodes and connections) in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/SelectionStateTest.kt
- [X] T005 [P] Write unit tests for NavigationContext data class in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/NavigationContextTest.kt

### Implementation for Foundation

- [X] T006 [P] Create SelectionState data class with selectedNodeIds, selectedConnectionIds, selectionBox properties in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/SelectionState.kt
- [X] T007 [P] Create NavigationContext data class with path, pushInto, popOut methods in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/NavigationContext.kt
- [X] T008 Add selectionState property to GraphState in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphState.kt
- [X] T009 Add navigationContext property to GraphState in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphState.kt
- [X] T010 Add getNodesInCurrentContext() method to GraphState in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphState.kt
- [X] T011 Add getConnectionsInCurrentContext() method to GraphState in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphState.kt
- [X] T012 Verify T004-T005 tests pass after implementation

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Multi-Select Nodes with Shift-Click (Priority: P1) 🎯 MVP

**Goal**: Enable developers to select multiple nodes by Shift-clicking, with visual feedback

**Independent Test**: Open graph with 5+ nodes, Shift-click on 3 nodes, verify all become selected with highlights, click empty canvas to clear

### Tests for User Story 1 (TDD - Write FIRST, must FAIL) ⚠️

- [X] T013 [P] [US1] Write UI test for Shift-click node selection in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/ui/MultiSelectionTest.kt
- [X] T014 [P] [US1] Write UI test for selection highlight rendering in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/ui/MultiSelectionTest.kt
- [X] T015 [P] [US1] Write UI test for click-to-clear selection in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/ui/MultiSelectionTest.kt

### Implementation for User Story 1

- [X] T016 [US1] Add toggleNodeInSelection(nodeId: String) method to GraphState in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphState.kt
- [X] T017 [US1] Add clearSelection() method to GraphState in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphState.kt
- [X] T018 [US1] Modify FlowGraphCanvas to detect Shift key modifier in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/FlowGraphCanvas.kt
- [X] T019 [US1] Modify FlowGraphCanvas onClick to call toggleNodeInSelection when Shift held in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/FlowGraphCanvas.kt
- [X] T020 [US1] Modify FlowGraphCanvas onClick to call clearSelection when clicking empty canvas without Shift in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/FlowGraphCanvas.kt
- [X] T021 [US1] Modify NodeRenderer to render selection highlight for nodes in selectionState.selectedNodeIds in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/rendering/NodeRenderer.kt
- [X] T022 [US1] Modify ConnectionRenderer to highlight connections between selected nodes in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/rendering/ConnectionRenderer.kt
- [X] T023 [US1] Verify T013-T015 tests pass after implementation

**Checkpoint**: User Story 1 complete - multi-select via Shift-click works independently

---

## Phase 4: User Story 2 - Rectangular Selection with Shift-Drag (Priority: P2)

**Goal**: Enable developers to select multiple nodes by Shift-dragging a rectangle on the canvas

**Independent Test**: Shift-drag rectangle around cluster of nodes, verify all enclosed nodes become selected, dotted rectangle visible during drag

### Tests for User Story 2 (TDD - Write FIRST, must FAIL) ⚠️

- [X] T024 [P] [US2] Write UI test for rectangular selection initiation in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/ui/RectangularSelectionTest.kt
- [X] T025 [P] [US2] Write UI test for selection box rendering during drag in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/ui/RectangularSelectionTest.kt
- [X] T026 [P] [US2] Write UI test for nodes enclosed in box are selected on release in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/ui/RectangularSelectionTest.kt

### Implementation for User Story 2

- [X] T027 [P] [US2] Create SelectionBox Composable for dotted rectangle rendering in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/SelectionBox.kt
- [X] T028 [US2] Add startRectangularSelection(startPosition: Offset) method to GraphState in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphState.kt
- [X] T029 [US2] Add updateRectangularSelection(currentPosition: Offset) method to GraphState in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphState.kt
- [X] T030 [US2] Add finishRectangularSelection() method to GraphState with node center detection in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphState.kt
- [X] T031 [US2] Modify FlowGraphCanvas to detect Shift+drag on empty canvas in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/FlowGraphCanvas.kt
- [X] T032 [US2] Integrate SelectionBox rendering into FlowGraphCanvas when selection active in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/FlowGraphCanvas.kt
- [X] T033 [US2] Verify T024-T026 tests pass after implementation

**Checkpoint**: User Stories 1 AND 2 complete - both Shift-click and rectangular selection work

---

## Phase 5: User Story 3 - Group Nodes into GraphNode via Context Menu (Priority: P3)

**Goal**: Enable developers to group selected nodes into a GraphNode with auto-generated ports

**Independent Test**: Select 3 connected nodes with external connections, right-click, select "Group", verify single GraphNode appears with correct ports

### Tests for User Story 3 (TDD - Write FIRST, must FAIL) ⚠️

- [X] T034 [P] [US3] Write unit test for GraphNodeFactory.createFromSelection() in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/factory/GraphNodeFactoryTest.kt
- [X] T035 [P] [US3] Write unit test for port mapping generation from external connections in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/factory/GraphNodeFactoryTest.kt
- [X] T036 [P] [US3] Write UI test for GroupContextMenu rendering in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/ui/GroupContextMenuTest.kt
- [X] T037 [P] [US3] Write integration test for grouping operation in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/ui/GroupContextMenuTest.kt

### Implementation for User Story 3

- [X] T038 [P] [US3] Create GraphNodeFactory with createFromSelection() method in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/GraphNodeFactory.kt
- [X] T039 [US3] Implement generatePortMappings() for detecting external connections in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/GraphNodeFactory.kt
- [X] T040 [P] [US3] Create GroupContextMenuState data class in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GroupContextMenuState.kt
- [X] T041 [P] [US3] Create GroupContextMenu Composable with "Group" option in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GroupContextMenu.kt
- [X] T042 [US3] Add showGroupContextMenu() method to GraphState in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphState.kt
- [X] T043 [US3] Add groupSelectedNodes() method to GraphState using GraphNodeFactory in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphState.kt
- [X] T044 [US3] Modify FlowGraphCanvas to show GroupContextMenu on right-click with selection in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/FlowGraphCanvas.kt
- [X] T045 [P] [US3] Create GraphNodeRenderer for distinct GraphNode visual in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphNodeRenderer.kt
- [X] T046 [US3] Integrate GraphNodeRenderer into NodeRenderer for GraphNode type in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/FlowGraphCanvas.kt
- [X] T047 [US3] Verify T034-T037 tests pass after implementation

**Checkpoint**: User Story 3 complete - nodes can be grouped into GraphNodes

---

## Phase 5A: Toolbar Group/Ungroup Buttons (Priority: P3.5) - UI Revision

**Goal**: Replace context menu with toolbar buttons for Group/Ungroup actions

**Rationale**: Right-clicking on selected elements causes deselection due to event handling issues. Moving to toolbar buttons provides more reliable UX and follows common editor patterns.

**Plan Reference**: See `specs/005-graphnode-creation/toolbar-group-buttons-plan.md`

### Implementation for Toolbar Transition

- [X] T048A [US3] Add canGroupSelection() helper to GraphState - returns true when 2+ nodes selected in graphEditor/src/jvmMain/kotlin/state/GraphState.kt
- [X] T048B [US3] Add canUngroupSelection() helper to GraphState - returns true when single GraphNode selected in graphEditor/src/jvmMain/kotlin/state/GraphState.kt
- [X] T048C [US3] Add Group button to TopToolbar (enabled when canGroupSelection) in graphEditor/src/jvmMain/kotlin/Main.kt
- [X] T048D [US3/US4] Add Ungroup button to TopToolbar (enabled when canUngroupSelection) in graphEditor/src/jvmMain/kotlin/Main.kt
- [X] T048E [US3] Remove GroupContextMenu composable call from Main.kt UI tree in graphEditor/src/jvmMain/kotlin/Main.kt
- [X] T048F [US3] Remove onGroupRightClick parameter and handler from FlowGraphCanvas in graphEditor/src/jvmMain/kotlin/ui/FlowGraphCanvas.kt
- [X] T048G [US3] Remove groupContextMenu state, showGroupContextMenu(), hideGroupContextMenu() from GraphState in graphEditor/src/jvmMain/kotlin/state/GraphState.kt
- [X] T048H [US3] Update GroupContextMenuTest to test toolbar button behavior in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/ui/GroupContextMenuTest.kt

**Checkpoint**: Group/Ungroup now accessible via toolbar buttons instead of context menu

---

## Phase 6: User Story 4 - Ungroup GraphNode via Toolbar (Priority: P4)

**Goal**: Enable developers to ungroup a GraphNode back into its constituent nodes

**Independent Test**: Select GraphNode, click "Ungroup" in toolbar, verify internal nodes appear on canvas with connections restored

### Tests for User Story 4 (TDD - Write FIRST, must FAIL) ⚠️

- [X] T049 [P] [US4] Write unit test for ungroupGraphNode() restoring child nodes in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/GraphStateTest.kt
- [X] T050 [P] [US4] Write unit test for connection restoration after ungroup in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/GraphStateTest.kt
- [X] T051 [P] [US4] Write UI test for Ungroup toolbar button behavior in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/ui/ToolbarGroupButtonsTest.kt

### Implementation for User Story 4

- [X] T052 [US4] Add ungroupGraphNode(graphNodeId: String) method to GraphState in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphState.kt
- [X] T053 [US4] Implement node positioning algorithm for ungrouped nodes (avoid overlap) in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphState.kt
- [X] T054 [US4] Implement connection restoration from port mappings in ungroupGraphNode in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphState.kt
- [X] T055 [US4] Verify T049-T051 tests pass after implementation

**Checkpoint**: User Stories 3 AND 4 complete - full group/ungroup cycle works via toolbar

---

## Phase 7: User Story 5 - Zoom Into GraphNode (Priority: P5)

**Goal**: Enable developers to navigate into a GraphNode to view/edit its internal structure

**Independent Test**: Click zoom-in button on GraphNode, verify view shows internal nodes, boundary with ports, and breadcrumb

### Tests for User Story 5 (TDD - Write FIRST, must FAIL) ⚠️

- [X] T056 [P] [US5] Write unit test for navigateInto() updating NavigationContext in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/NavigationContextTest.kt
- [X] T057 [P] [US5] Write UI test for zoom-in button rendering on GraphNode in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/ui/GraphNodeRendererTest.kt
- [X] T058 [P] [US5] Write UI test for internal view rendering with boundary in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/ui/InternalViewTest.kt

### Implementation for User Story 5

- [X] T059 [US5] Add navigateInto(graphNodeId: String) method to GraphState in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphState.kt
- [X] T060 [US5] Add zoom-in button to GraphNodeRenderer in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphNodeRenderer.kt
- [X] T061 [US5] Create NavigationBreadcrumb Composable in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/NavigationBreadcrumb.kt
- [X] T062 [US5] Modify FlowGraphCanvas to render based on NavigationContext (internal vs root view) in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/FlowGraphCanvas.kt
- [X] T063 [US5] Implement boundary rendering with ports in internal view in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/FlowGraphCanvas.kt
- [X] T064 [US5] Integrate NavigationBreadcrumb into Main.kt layout in graphEditor/src/jvmMain/kotlin/Main.kt
- [X] T065 [US5] Verify T056-T058 tests pass after implementation

**Checkpoint**: User Story 5 complete - can navigate into GraphNodes

---

## Phase 8: User Story 6 - Zoom Out from GraphNode View (Priority: P6)

**Goal**: Enable developers to navigate back up to parent context from internal GraphNode view

**Independent Test**: While in internal view, click zoom-out button, verify return to parent context with GraphNode visible

### Tests for User Story 6 (TDD - Write FIRST, must FAIL) ⚠️

- [X] T066 [P] [US6] Write unit test for navigateOut() updating NavigationContext in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/NavigationContextTest.kt
- [X] T067 [P] [US6] Write UI test for zoom-out button in internal view in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/ui/InternalViewTest.kt
- [X] T068 [P] [US6] Write integration test for nested navigation (3+ levels) in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/ui/NavigationTest.kt

### Implementation for User Story 6

- [X] T069 [US6] Add navigateOut() method to GraphState in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphState.kt
- [X] T070 [US6] Add NavigationZoomOutButton Composable in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/NavigationZoomOutButton.kt
- [X] T071 [US6] Integrate zoom-out button into FlowGraphCanvas internal view in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/FlowGraphCanvas.kt
- [X] T072 [US6] Add breadcrumb click navigation in NavigationBreadcrumb in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/NavigationBreadcrumb.kt
- [X] T073 [US6] Disable zoom-out button when at root level in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/NavigationZoomOutButton.kt
- [X] T074 [US6] Verify T066-T068 tests pass after implementation

**Checkpoint**: All navigation features complete - full hierarchical traversal works

---

## Phase 9: Serialization & Persistence

**Purpose**: Complete GraphNode serialization support for .flow.kts files

### Tests for Serialization (TDD - Write FIRST, must FAIL) ⚠️

- [ ] T075 [P] Write unit test for GraphNode serialization with children in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/serialization/GraphNodeSerializationTest.kt
- [ ] T076 [P] Write unit test for GraphNode deserialization roundtrip in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/serialization/GraphNodeSerializationTest.kt
- [ ] T077 [P] Write unit test for nested GraphNode serialization in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/serialization/GraphNodeSerializationTest.kt

### Implementation for Serialization

- [ ] T078 Complete serializeGraphNode() with recursive child serialization in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/serialization/FlowGraphSerializer.kt
- [ ] T079 Add internalConnections serialization to serializeGraphNode() in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/serialization/FlowGraphSerializer.kt
- [ ] T080 Add portMappings serialization to serializeGraphNode() in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/serialization/FlowGraphSerializer.kt
- [ ] T081 Complete deserializeGraphNode() with recursive child parsing in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/serialization/FlowGraphDeserializer.kt
- [ ] T082 Verify T075-T077 tests pass after implementation

**Checkpoint**: Serialization complete - GraphNodes persist and reload correctly

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T083 [P] Add selection count badge to status bar in graphEditor/src/jvmMain/kotlin/Main.kt
- [ ] T084 [P] Add undo/redo support for group/ungroup operations in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/UndoRedoManager.kt
- [ ] T085 Verify edge case: Group button disabled when single node selected in graphEditor/src/jvmMain/kotlin/Main.kt
- [ ] T086 Verify edge case: Ungroup button disabled when CodeNode selected in graphEditor/src/jvmMain/kotlin/Main.kt
- [ ] T087 Run quickstart.md validation scenarios (all 7 scenarios pass)
- [ ] T088 Performance test: select 50+ nodes in <100ms
- [ ] T089 Final code review for Apache 2.0 header compliance on all new files

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-8)**: All depend on Foundational phase completion
  - US1 and US2 can run in parallel (different features)
  - US3 depends on US1 or US2 (needs selection)
  - **Phase 5A (Toolbar Transition)**: Depends on US3 completion - switches from context menu to toolbar
  - US4 depends on Phase 5A (needs toolbar buttons in place)
  - US5 depends on US3 (needs GraphNodes to navigate into)
  - US6 depends on US5 (needs internal view to navigate out from)
- **Serialization (Phase 9)**: Can run after US3 (needs GraphNodes), parallel with US4-US6
- **Polish (Phase 10)**: Depends on all user stories being complete

### User Story Dependencies

```
Setup (Phase 1)
    │
    ▼
Foundational (Phase 2)
    │
    ├──────────────────┐
    ▼                  ▼
US1 (P1)            US2 (P2)
    │                  │
    └──────┬───────────┘
           ▼
        US3 (P3) ◄───── Serialization (Phase 9)
           │
           ▼
    Phase 5A (Toolbar)
           │
    ┌──────┼──────┐
    ▼             ▼
US4 (P4)       US5 (P5)
                  │
                  ▼
               US6 (P6)
                  │
                  ▼
            Polish (Phase 10)
```

### Within Each User Story

- Tests (TDD) MUST be written and FAIL before implementation
- State/models before UI components
- Core logic before integration
- Story complete before moving to dependent stories

### Parallel Opportunities

- **Phase 1**: T002 and T003 can run in parallel
- **Phase 2**: T004, T005 can run in parallel; T006, T007 can run in parallel
- **Phase 3 (US1)**: T013, T014, T015 can all run in parallel
- **Phase 4 (US2)**: T024, T025, T026 can all run in parallel; T027 parallel with state tasks
- **Phase 5 (US3)**: T034-T037 can all run in parallel; T038, T040, T041, T045 can run in parallel
- **Phase 6 (US4)**: T048, T049, T050 can all run in parallel
- **Phase 7 (US5)**: T056, T057, T058 can all run in parallel
- **Phase 8 (US6)**: T066, T067, T068 can all run in parallel
- **Phase 9**: T075, T076, T077 can all run in parallel
- **Phase 10**: T083, T084, T085 can all run in parallel

---

## Parallel Example: User Story 3

```bash
# Launch all tests for User Story 3 together (TDD - write these first):
Task: T034 "Unit test for GraphNodeFactory.createFromSelection()"
Task: T035 "Unit test for port mapping generation"
Task: T036 "UI test for GroupContextMenu rendering"
Task: T037 "Integration test for grouping operation"

# After tests fail, launch parallel implementation tasks:
Task: T038 "Create GraphNodeFactory"
Task: T040 "Create GroupContextMenuState"
Task: T041 "Create GroupContextMenu Composable"
Task: T045 "Create GraphNodeRenderer"

# Then sequential tasks:
Task: T039 "Implement generatePortMappings()"
Task: T042 "Add showGroupContextMenu() to GraphState"
Task: T043 "Add groupSelectedNodes() to GraphState"
Task: T044 "Integrate context menu into FlowGraphCanvas"
Task: T046 "Integrate GraphNodeRenderer into NodeRenderer"
Task: T047 "Verify all tests pass"
```

---

## Implementation Strategy

### MVP First (User Stories 1-3 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (Multi-select)
4. Complete Phase 4: User Story 2 (Rectangular selection)
5. Complete Phase 5: User Story 3 (Group into GraphNode)
6. **STOP and VALIDATE**: Test grouping workflow end-to-end
7. Deploy/demo if ready

**MVP Deliverable**: Developers can select nodes and group them into GraphNodes with auto-generated ports.

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → **Selection MVP!**
3. Add User Story 2 → Test independently → **Enhanced selection**
4. Add User Story 3 → Test independently → **Grouping MVP!**
5. Add User Story 4 → Test independently → **Full group/ungroup cycle**
6. Add User Story 5 + 6 → Test independently → **Hierarchical navigation**
7. Add Serialization → **Persistence complete**
8. Polish → **Production ready**

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 + User Story 2 (selection features)
   - Developer B: User Story 3 (grouping) + Serialization
3. After US3 complete:
   - Developer A: User Story 4 (ungrouping)
   - Developer B: User Story 5 + 6 (navigation)
4. Polish as team

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- **TDD MANDATORY**: Verify tests fail before implementing (constitution requirement)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence

---

## Total Task Count: 96 tasks

### Tasks per Phase:
- **Setup**: 3 tasks
- **Foundational**: 9 tasks (BLOCKING)
- **US1 (Multi-select)**: 11 tasks (3 tests + 8 implementation)
- **US2 (Rectangular)**: 10 tasks (3 tests + 7 implementation)
- **US3 (Group)**: 14 tasks (4 tests + 10 implementation)
- **Phase 5A (Toolbar Transition)**: 8 tasks (UI revision)
- **US4 (Ungroup)**: 7 tasks (3 tests + 4 implementation)
- **US5 (Zoom In)**: 10 tasks (3 tests + 7 implementation)
- **US6 (Zoom Out)**: 9 tasks (3 tests + 6 implementation)
- **Serialization**: 8 tasks (3 tests + 5 implementation)
- **Polish**: 7 tasks

### Parallel Opportunities:
- **Setup phase**: 2 tasks can run in parallel
- **Foundational phase**: 4 tasks can run in parallel
- **US1-US2**: Can run in parallel after Foundational
- **Phase 5A**: Sequential after US3, before US4
- **Serialization**: Can run in parallel with Phase 5A, US4-US6
- **Polish**: 3+ tasks can run in parallel

### Suggested MVP Scope:
**Phases 1-5A only (Setup + Foundational + US1 + US2 + US3 + Toolbar Transition)** = 55 tasks

**Deliverable**: Working multi-selection (Shift-click and rectangular) with grouping into GraphNodes via toolbar buttons. Users can create hierarchical graph structures.
