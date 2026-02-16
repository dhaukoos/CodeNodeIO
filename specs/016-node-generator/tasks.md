# Tasks: Node Generator UI Tool

**Input**: Design documents from `/specs/016-node-generator/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/internal-api.md, quickstart.md

**Tests**: Tests are included as this is a UI feature with validation logic that benefits from TDD.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **graphEditor module**: `graphEditor/src/jvmMain/kotlin/`
- **Test files**: `graphEditor/src/jvmTest/kotlin/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and repository structure

- [x] T001 Create repository directory structure at `graphEditor/src/jvmMain/kotlin/repository/`
- [x] T002 Verify kotlinx-serialization dependency in `graphEditor/build.gradle.kts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**Critical**: These components are shared by all user stories

- [x] T003 Create `NodeGeneratorState` data class in `graphEditor/src/jvmMain/kotlin/state/NodeGeneratorState.kt` with name, inputCount, outputCount fields, computed `isValid` and `genericType` properties, and helper methods `reset()`, `withName()`, `withInputCount()`, `withOutputCount()`
- [x] T004 [P] Create `CustomNodeDefinition` serializable data class in `graphEditor/src/jvmMain/kotlin/repository/CustomNodeDefinition.kt` with id, name, inputCount, outputCount, genericType, createdAt fields, `create()` factory method, and `toNodeTypeDefinition()` converter
- [x] T005 Create `CustomNodeRepository` interface in `graphEditor/src/jvmMain/kotlin/repository/CustomNodeRepository.kt` with `getAll()`, `add()`, `load()`, `save()` methods
- [x] T006 Create `FileCustomNodeRepository` implementation in `graphEditor/src/jvmMain/kotlin/repository/FileCustomNodeRepository.kt` with JSON serialization to `~/.codenode/custom-nodes.json`

**Checkpoint**: ✅ Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Create Custom Node Type (Priority: P1) MVP

**Goal**: Users can create custom node types with configurable inputs/outputs and see them in the Node Palette

**Independent Test**: Create a node with name "TestNode", 2 inputs, 1 output. Verify node appears in Generic section of Node Palette with genericType "in2out1".

### Tests for User Story 1

- [x] T007 [P] [US1] Create validation tests in `graphEditor/src/jvmTest/kotlin/state/NodeGeneratorStateTest.kt` testing: name validation (blank, whitespace), input/output range coercion, isValid computed property for all 16 combinations including 0/0 rejection
- [x] T008 [P] [US1] Create CustomNodeDefinition tests in `graphEditor/src/jvmTest/kotlin/repository/CustomNodeDefinitionTest.kt` testing: create() factory method generates unique IDs, toNodeTypeDefinition() returns correct NodeTypeDefinition

### Implementation for User Story 1

- [x] T009 [US1] Create `NodeGeneratorPanel` composable in `graphEditor/src/jvmMain/kotlin/ui/NodeGeneratorPanel.kt` with: header "Node Generator", OutlinedTextField for name, dropdowns for inputCount (0-3) and outputCount (0-3), Create and Cancel buttons
- [x] T010 [US1] Implement Create button disabled logic when `!state.isValid` (name blank OR both inputs and outputs are 0) in `graphEditor/src/jvmMain/kotlin/ui/NodeGeneratorPanel.kt`
- [x] T011 [US1] Wire NodeGeneratorPanel to Main.kt: add state `var nodeGeneratorState by remember { mutableStateOf(NodeGeneratorState()) }`, place panel above NodePalette in left column
- [x] T012 [US1] Implement onCreateNode callback: create CustomNodeDefinition via factory, add to customNodes state, trigger palette refresh in `graphEditor/src/jvmMain/kotlin/Main.kt`
- [x] T013 [US1] Update NodePalette integration: combine `createSampleNodeTypes()` with `customNodes.map { it.toNodeTypeDefinition() }` for allNodeTypes in `graphEditor/src/jvmMain/kotlin/Main.kt`

**Checkpoint**: ✅ User Story 1 complete - users can create custom nodes and see them in palette

---

## Phase 4: User Story 2 - Cancel Node Creation (Priority: P2)

**Goal**: Users can abort node creation and reset the form

**Independent Test**: Enter values in form, click Cancel, verify form resets and no node is added.

### Implementation for User Story 2

- [x] T014 [US2] Implement Cancel button onClick in `NodeGeneratorPanel.kt` to call `onStateChange(state.reset())` to reset form to defaults
- [x] T015 [US2] Verify form reset after Create by calling `onStateChange(state.reset())` after successful node creation in `NodeGeneratorPanel.kt`

**Checkpoint**: ✅ User Story 2 complete - Cancel functionality works

---

## Phase 5: User Story 3 - Use Created Node in Flow Graph (Priority: P2)

**Goal**: Created nodes can be dragged to canvas with correct port configuration

**Independent Test**: Create "DataMerger" with 2 inputs, 1 output. Drag to canvas. Verify node shows 2 input ports and 1 output port.

### Implementation for User Story 3

- [x] T016 [US3] Verify GenericNodeTypeFactory.createGenericNodeType() integration in `CustomNodeDefinition.toNodeTypeDefinition()` produces correct port counts in `graphEditor/src/jvmMain/kotlin/repository/CustomNodeDefinition.kt`
- [ ] T017 [US3] Test drag-and-drop from palette to canvas for custom nodes - verify node instance has correct input/output ports (manual verification or integration test)

**Checkpoint**: User Story 3 in progress - T017 requires manual verification

---

## Phase 6: User Story 4 - Persist Custom Nodes Across Sessions (Priority: P2)

**Goal**: Custom nodes survive application restart

**Independent Test**: Create node, close app, reopen app, verify node still in palette.

### Tests for User Story 4

- [ ] T018 [P] [US4] Create persistence tests in `graphEditor/src/jvmTest/kotlin/repository/CustomNodeRepositoryTest.kt` testing: save/load roundtrip, missing file handling (returns empty list), corrupted JSON handling (returns empty list, logs warning)

### Implementation for User Story 4

- [x] T019 [US4] Initialize FileCustomNodeRepository in Main.kt with `remember { FileCustomNodeRepository() }`
- [x] T020 [US4] Call `customNodeRepository.load()` on startup via `LaunchedEffect(Unit)` in `graphEditor/src/jvmMain/kotlin/Main.kt`
- [x] T021 [US4] Wire onCreateNode to call `customNodeRepository.add(node)` for persistence in `graphEditor/src/jvmMain/kotlin/Main.kt`
- [x] T022 [US4] Implement directory creation in FileCustomNodeRepository.save() with `file.parentFile?.mkdirs()` in `graphEditor/src/jvmMain/kotlin/repository/FileCustomNodeRepository.kt`

**Checkpoint**: User Story 4 implementation complete - T018 (persistence tests) remaining

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and edge case handling

- [ ] T023 [P] Verify all 15 valid input/output combinations (0-3 x 0-3, excluding 0/0) create correct nodes
- [ ] T024 [P] Test edge cases: empty name disabled, whitespace-only name disabled, 0/0 combination disabled
- [ ] T025 Run quickstart.md validation checklist: form validation, Create/Cancel behavior, palette update, persistence, error handling
- [ ] T026 Code review: verify immutability of data classes, proper error handling, no mutable leaks

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-6)**: All depend on Foundational phase completion
  - US1 (P1): Can start immediately after Foundational
  - US2 (P2): Depends on US1 (Cancel uses same form as Create)
  - US3 (P2): Can run parallel with US2 after US1
  - US4 (P2): Can run parallel with US2/US3 after US1
- **Polish (Phase 7)**: Depends on all user stories being complete

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Models/data classes before services/repositories
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- T003 and T004 can run in parallel (different files)
- T007 and T008 can run in parallel (different test files)
- T018 can run in parallel with US3 implementation
- T023, T024, and T026 can run in parallel (different validation types)

---

## Parallel Example: Foundational Phase

```bash
# Launch these together:
Task: "Create NodeGeneratorState data class in graphEditor/src/jvmMain/kotlin/state/NodeGeneratorState.kt"
Task: "Create CustomNodeDefinition serializable data class in graphEditor/src/jvmMain/kotlin/repository/CustomNodeDefinition.kt"
```

## Parallel Example: User Story 1 Tests

```bash
# Launch these together:
Task: "Create validation tests in graphEditor/src/jvmTest/kotlin/state/NodeGeneratorStateTest.kt"
Task: "Create CustomNodeDefinition tests in graphEditor/src/jvmTest/kotlin/repository/CustomNodeDefinitionTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test US1 independently - can create nodes and see in palette
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational -> Foundation ready
2. Add User Story 1 -> Test independently -> Demo (MVP!)
3. Add User Story 2 -> Test independently (Cancel works)
4. Add User Story 3 -> Test independently (Nodes work on canvas)
5. Add User Story 4 -> Test independently (Persistence works)
6. Each story adds value without breaking previous stories

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently

---

## Progress Summary

**Commit fcd30bc** (2026-02-16): T001-T022 implemented (except T017, T018)

| Phase | Status | Tasks |
|-------|--------|-------|
| Phase 1: Setup | ✅ Complete | T001-T002 |
| Phase 2: Foundational | ✅ Complete | T003-T006 |
| Phase 3: US1 (MVP) | ✅ Complete | T007-T013 |
| Phase 4: US2 (Cancel) | ✅ Complete | T014-T015 |
| Phase 5: US3 (Canvas) | 🔄 In Progress | T016 ✅, T017 pending |
| Phase 6: US4 (Persist) | 🔄 In Progress | T019-T022 ✅, T018 pending |
| Phase 7: Polish | ⏳ Not Started | T023-T026 |

**UI Enhancements implemented:**
- Collapsible panel header with expand/collapse arrow (▶/▼)
- Horizontal layout for Inputs/Outputs dropdowns (label left, dropdown right)
- Panel starts collapsed by default
