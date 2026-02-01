# Tasks: InformationPacket Palette Support

**Input**: Design documents from `/specs/003-ip-palette-support/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Tests are REQUIRED per project constitution (TDD mandatory). All test tasks must be completed before implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Multi-module KMP project**: `fbpDsl/`, `graphEditor/` at repository root
- Paths assume multi-module structure per plan.md

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure for IP Palette feature

- [x] T001 Verify existing project structure supports new model files in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/
- [x] T002 Verify existing project structure supports new UI files in graphEditor/src/jvmMain/kotlin/ui/
- [x] T003 Verify existing project structure supports new state files in graphEditor/src/jvmMain/kotlin/state/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core model infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 [P] Create IPColor data class with RGB validation in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/IPColor.kt
- [x] T005 [P] Create IPColorTest with validation tests in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/IPColorTest.kt
- [x] T006 [P] Create InformationPacketType data class in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/InformationPacketType.kt
- [x] T007 [P] Create InformationPacketTypeTest in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/InformationPacketTypeTest.kt
- [x] T008 Modify Connection data class to add optional ipTypeId property in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Connection.kt
- [x] T009 Create IPTypeRegistry class with default types in graphEditor/src/jvmMain/kotlin/state/IPTypeRegistry.kt
- [x] T010 Create IPTypeRegistryTest in graphEditor/src/jvmTest/kotlin/state/IPTypeRegistryTest.kt

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - View and Browse InformationPacket Types (Priority: P1) 🎯 MVP

**Goal**: Enable developers to view available InformationPacket types in a searchable palette with color indicators

**Independent Test**: Open the graphEditor, locate the IP Palette, verify all 5 default types (Any, Int, Double, Boolean, String) are visible with their color indicators, and use the search field to filter results.

### Tests for User Story 1 (TDD - Write These FIRST) ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T011 [P] [US1] UI test for IPPalette component in graphEditor/src/jvmTest/kotlin/ui/IPPaletteTest.kt

### Implementation for User Story 1

- [x] T012 [P] [US1] Implement IPPalette Composable with search field and type list in graphEditor/src/jvmMain/kotlin/ui/IPPalette.kt
- [x] T013 [US1] Integrate IPPalette into Main.kt layout alongside NodePalette in graphEditor/src/jvmMain/kotlin/Main.kt
- [x] T014 [US1] Add IP type selection callback to display type code in TextualView in graphEditor/src/jvmMain/kotlin/ui/TextualView.kt

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Select and Inspect Connections (Priority: P2)

**Goal**: Enable developers to select connections by clicking on them and view their IP type properties in the Properties Panel

**Independent Test**: Create a graph with at least one connection, click on the connection in the Visual view, verify it becomes selected, and confirm the Properties Panel shows the connection's IP type with a color swatch and RGB value.

### Tests for User Story 2 (TDD - Write These FIRST) ⚠️

- [x] T015 [P] [US2] UI test for connection selection in graphEditor/src/jvmTest/kotlin/ui/ConnectionSelectionTest.kt
- [x] T016 [P] [US2] UI test for ColorEditor component in graphEditor/src/jvmTest/kotlin/ui/ColorEditorTest.kt

### Implementation for User Story 2

- [x] T017 [P] [US2] Create ColorEditor Composable with color swatch and RGB input in graphEditor/src/jvmMain/kotlin/ui/ColorEditor.kt
- [x] T018 [US2] Add selectedConnectionId state and selectConnection method to GraphState in graphEditor/src/jvmMain/kotlin/state/GraphState.kt
- [x] T019 [US2] Implement findConnectionAtPosition function with Bezier hit detection in graphEditor/src/jvmMain/kotlin/ui/FlowGraphCanvas.kt
- [x] T020 [US2] Add connection click handling to FlowGraphCanvas (left-click to select) in graphEditor/src/jvmMain/kotlin/ui/FlowGraphCanvas.kt
- [x] T021 [US2] Add visual highlighting for selected connections in graphEditor/src/jvmMain/kotlin/ui/FlowGraphCanvas.kt
- [x] T022 [US2] Add connection properties section to PropertiesPanel (IP type name, color swatch, RGB editor) in graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt
- [x] T023 [US2] Wire up connection selection and deletion in graphEditor/src/jvmMain/kotlin/Main.kt

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Change Connection IP Type via Context Menu (Priority: P3)

**Goal**: Enable developers to change a connection's IP type by right-clicking on it and selecting from a dropdown menu

**Independent Test**: Right-click on a connection, verify the context menu appears with all available IP types listed, select a different type, and confirm the connection updates to the new type.

### Tests for User Story 3 (TDD - Write These FIRST) ⚠️

- [ ] T024 [P] [US3] UI test for ConnectionContextMenu component in graphEditor/src/jvmTest/kotlin/ui/ConnectionContextMenuTest.kt

### Implementation for User Story 3

- [ ] T025 [P] [US3] Create ConnectionContextMenu Composable with IP type dropdown in graphEditor/src/jvmMain/kotlin/ui/ConnectionContextMenu.kt
- [ ] T026 [US3] Add connectionContextMenu state and methods to GraphState in graphEditor/src/jvmMain/kotlin/state/GraphState.kt
- [ ] T027 [US3] Add right-click handling to FlowGraphCanvas for connections in graphEditor/src/jvmMain/kotlin/ui/FlowGraphCanvas.kt
- [ ] T028 [US3] Implement updateConnectionIPType method in GraphState in graphEditor/src/jvmMain/kotlin/state/GraphState.kt
- [ ] T029 [US3] Render ConnectionContextMenu in Main.kt when context menu state is set in graphEditor/src/jvmMain/kotlin/Main.kt
- [ ] T030 [US3] Update connection visual color based on IP type in graphEditor/src/jvmMain/kotlin/rendering/ConnectionRenderer.kt

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T031 [P] Add keyboard support for context menu dismissal (Escape key) in graphEditor/src/jvmMain/kotlin/ui/ConnectionContextMenu.kt
- [ ] T032 [P] Ensure proper z-ordering (nodes on top of connections) in FlowGraphCanvas in graphEditor/src/jvmMain/kotlin/ui/FlowGraphCanvas.kt
- [ ] T033 [P] Add empty state message "No matching types" to IPPalette search in graphEditor/src/jvmMain/kotlin/ui/IPPalette.kt
- [ ] T034 Update FlowGraphSerializer to serialize ipTypeId in connections in graphEditor/src/jvmMain/kotlin/serialization/FlowGraphSerializer.kt
- [ ] T035 Update FlowGraphDeserializer to deserialize ipTypeId in connections in graphEditor/src/jvmMain/kotlin/serialization/FlowGraphDeserializer.kt
- [ ] T036 Run quickstart.md validation scenarios to verify feature completeness

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Independent of US1 (different UI components)
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Integrates with US2 connection selection but should be independently testable

### Within Each User Story

- Tests (TDD) MUST be written and FAIL before implementation
- Models before state
- State before UI components
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Foundational tasks marked [P] can run in parallel (T004-T007, T009-T010)
- All tests for a user story marked [P] can run in parallel
- Once Foundational phase completes:
  - US1 and US2 can start in parallel (different components)
  - US3 should ideally follow US2 (uses connection selection)
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together (TDD - write these first):
Task: "UI test for IPPalette component in graphEditor/src/jvmTest/kotlin/ui/IPPaletteTest.kt"

# After tests fail, implement components:
Task: "Implement IPPalette Composable with search field and type list in graphEditor/src/jvmMain/kotlin/ui/IPPalette.kt"
```

---

## Parallel Example: Foundational Phase

```bash
# Launch all parallel foundational tasks together:
Task: "Create IPColor data class with RGB validation"
Task: "Create IPColorTest with validation tests"
Task: "Create InformationPacketType data class"
Task: "Create InformationPacketTypeTest"
Task: "Create IPTypeRegistry class with default types"
Task: "Create IPTypeRegistryTest"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (IP Palette)
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

**Deliverable**: Working IP Palette panel with all 5 default types visible, searchable, and clickable to view code.

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo (Connection selection)
4. Add User Story 3 → Test independently → Deploy/Demo (Context menu)
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (IP Palette)
   - Developer B: User Story 2 (Connection Selection)
3. After US2 complete:
   - Developer A or B: User Story 3 (Context Menu)
4. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- **TDD MANDATORY**: Verify tests fail before implementing (constitution requirement)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- **Backward Compatibility**: Connection.ipTypeId is nullable - existing graphs load without modification

---

## Total Task Count: 36 tasks

### Tasks per Phase:
- **Setup**: 3 tasks
- **Foundational**: 7 tasks (BLOCKING)
- **US1 (IP Palette)**: 4 tasks (1 test + 3 implementation)
- **US2 (Connection Selection)**: 9 tasks (2 tests + 7 implementation)
- **US3 (Context Menu)**: 7 tasks (1 test + 6 implementation)
- **Polish**: 6 tasks

### Parallel Opportunities:
- **Foundational phase**: 6 tasks can run in parallel (T004-T007, T009-T010)
- **US1 tests**: 1 test task
- **US2 tests**: 2 test tasks can run in parallel
- **US3 tests**: 1 test task
- **After Foundational**: US1 and US2 can start in parallel (14 tasks across 2 stories)

### Suggested MVP Scope:
**Phases 1-3 only (Setup + Foundational + US1)** = 14 tasks

**Deliverable**: Working IP Palette panel with search, color indicators, and code display for all 5 default InformationPacket types.
