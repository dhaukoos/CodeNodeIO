# Tasks: View GraphNode Properties

**Input**: Design documents from `/specs/033-graphnode-properties/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Tests**: Not requested in feature specification. No test tasks included.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Generalize state management to support both CodeNode and GraphNode before any UI work begins

- [x] T001 Generalize `updateNodeName()` to handle both CodeNode and GraphNode via `when` block in `graphEditor/src/jvmMain/kotlin/state/GraphState.kt`
- [x] T002 Extract `SharedNodeProperties` composable (name TextField + port sections) from `PropertiesContent` into a new private composable in `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`
- [x] T003 Refactor `PropertiesContent` to call `SharedNodeProperties` as its uppermost element, keeping CodeNode-specific sections (generic type config, configuration properties) below, in `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`
- [x] T004 Compile and verify no regressions: `./gradlew :graphEditor:compileKotlinJvm`

**Checkpoint**: Foundation ready — `SharedNodeProperties` is extracted, CodeNode panel behavior unchanged, `updateNodeName` works for both node types

---

## Phase 2: User Story 1 — View GraphNode Name and Ports (Priority: P1)

**Goal**: When a GraphNode is selected, the Properties Panel displays its editable name and input/output ports using the shared composable

**Independent Test**: Create a GraphNode (group 2+ nodes), select it, verify Properties Panel shows name field and port sections. Edit name and verify it updates.

### Implementation for User Story 1

- [x] T005 [US1] Add `GraphNodePropertiesPanel` composable in `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt` — header "GraphNode Properties", calls `SharedNodeProperties` for name and ports
- [x] T006 [US1] Add `selectedGraphNode: GraphNode?` parameter to `CompactPropertiesPanelWithViewModel` and route to `GraphNodePropertiesPanel` when a GraphNode is selected (priority: IP type > connection > GraphNode > CodeNode > empty) in `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`
- [x] T007 [US1] Derive `selectedGraphNode: GraphNode?` alongside existing `selectedNode: CodeNode?` in `graphEditor/src/jvmMain/kotlin/Main.kt` and pass it to `CompactPropertiesPanelWithViewModel`, wiring name/port change callbacks to `graphState.updateNodeName()` / `graphState.updatePortName()` / `graphState.updatePortType()`
- [x] T008 [US1] Compile and verify: `./gradlew :graphEditor:compileKotlinJvm`

**Checkpoint**: Selecting a GraphNode shows its name (editable) and ports in the Properties Panel. Selecting a CodeNode, connection, or IP type still works as before.

---

## Phase 3: User Story 2 — View Child Nodes List (Priority: P2)

**Goal**: Below the ports section, display a read-only list of child node names in the GraphNode Properties Panel

**Independent Test**: Select a GraphNode containing multiple child nodes, verify child node names appear below ports. Select a GraphNode with no child nodes, verify empty state message.

### Implementation for User Story 2

- [x] T009 [US2] Add "Child Nodes" section to `GraphNodePropertiesPanel` in `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt` — read-only list of `graphNode.childNodes` names with section header, and empty state message when no child nodes exist
- [x] T010 [US2] Compile and verify: `./gradlew :graphEditor:compileKotlinJvm`

**Checkpoint**: GraphNode Properties Panel shows name, ports, and child node names. All selection types (CodeNode, GraphNode, connection, IP type) display correctly.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: No dependencies — can start immediately. BLOCKS all user stories.
- **User Story 1 (Phase 2)**: Depends on Phase 1 completion (needs `SharedNodeProperties` and generalized `updateNodeName`)
- **User Story 2 (Phase 3)**: Depends on Phase 2 completion (adds child nodes section to `GraphNodePropertiesPanel` created in US1)

### Within Each Phase

- T001 is independent (different file from T002/T003)
- T002 must complete before T003 (extract before refactor)
- T005 must complete before T006 (create composable before routing to it)
- T006 must complete before T007 (panel parameter before Main.kt wiring)

### Parallel Opportunities

- T001 and T002 can run in parallel (different files: GraphState.kt vs PropertiesPanel.kt)

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Foundational (T001–T004)
2. Complete Phase 2: User Story 1 (T005–T008)
3. **STOP and VALIDATE**: Select a GraphNode — verify name and ports display correctly
4. Verify CodeNode, connection, and IP type panels still work

### Full Delivery

1. Complete Phase 1 + Phase 2 → MVP validated
2. Complete Phase 3: User Story 2 (T009–T010) → Child nodes list added
3. Final validation: all selection types work correctly

---

## Notes

- All changes are in 3 existing files — no new files created
- `SharedNodeProperties` is private to `PropertiesPanel.kt` to maintain access to private `PortEditorRow`
- `GraphNodePropertiesPanel` uses callback pattern (like `ConnectionPropertiesPanel`), not the `PropertiesPanelViewModel`
- Compile verification after each phase catches type errors early
