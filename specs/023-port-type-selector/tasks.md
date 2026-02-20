# Tasks: Port Type Selector

**Input**: Design documents from `/specs/023-port-type-selector/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/ui-interactions.md, quickstart.md

**Tests**: No tests requested - implementation tasks only.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **graphEditor module**: `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/`
- **State**: `graphEditor/.../state/GraphState.kt`
- **UI**: `graphEditor/.../ui/PropertiesPanel.kt`
- **ViewModel**: `graphEditor/.../viewmodel/PropertiesPanelViewModel.kt`
- **Main**: `graphEditor/.../Main.kt`

---

## Phase 1: Setup

**Purpose**: No project initialization needed - all infrastructure exists (IPTypeRegistry, DropdownEditor, GraphState patterns, serialization). Feature modifies existing files only.

No setup tasks required.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: No foundational tasks needed. Per research.md (R4), all required infrastructure is ready:
- `Port.dataType: KClass<T>` already stores and serializes type
- `Connection.ipTypeId: String?` already stores and serializes IP type ID
- `IPTypeRegistry` has 5 default types with `getByTypeName()` and `getAllTypes()`
- `DropdownEditor` composable is reusable
- `FlowGraphSerializer` already handles both `dataType` and `ipTypeId`

**Checkpoint**: Foundation ready - all user stories can proceed.

---

## Phase 3: User Story 1 - Select IP Type for a Port (Priority: P1) MVP

**Goal**: Add an IP type dropdown selector to each port row in the Properties panel for Generic Nodes. Selecting a type updates the port's dataType.

**Independent Test**: Select a Generic Node, open Properties panel, verify each port has an IP type dropdown next to its name field. Select "Int" from the dropdown and verify the port's data type updates. Save, reload, and verify the selection persists.

### Implementation for User Story 1

- [x] T001 [P] [US1] Add `updatePortType(nodeId, portId, typeName)` method to `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphState.kt` - Resolve ipType from IPTypeRegistry.getByTypeName(), copy port with new dataType, copy node with updated port lists, replace in flowGraph, set isDirty. Follow the existing `updatePortName()` pattern (lines ~349-368). No propagation logic yet.

- [x] T002 [P] [US1] Add `updatePortType(portId, typeName)` method and `onPortTypeChanged: ((String, String) -> Unit)?` callback to `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/viewmodel/PropertiesPanelViewModel.kt` - Follow the existing `updatePortName` / `onPortNameChanged` pattern. The method delegates to the injected callback.

- [x] T003 [US1] Add IP type dropdown to port rows in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/PropertiesPanel.kt` - In the port row composable (around lines 487-550), add a dropdown to the right of each port name TextField. Use the existing DropdownEditor pattern from PropertyEditors.kt (lines 189-247). Populate options from `IPTypeRegistry.getAllTypes()`. Display each option with a color swatch (small circle using IPColor) and typeName. Resolve the current selection from `port.dataType.simpleName`. On selection, call `viewModel.updatePortType(portId, typeName)`.

- [x] T004 [US1] Wire `onPortTypeChanged` callback in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/Main.kt` - Set `propertiesPanelViewModel.onPortTypeChanged` to call `graphState.updatePortType(nodeId, portId, typeName)`. Follow the existing `onPortNameChanged` wiring pattern. The nodeId must be resolved from the currently selected node.

- [x] T005 [US1] Build verification for US1 - Run `./gradlew :graphEditor:compileKotlinJvm` to verify compilation. If build fails, fix issues. Verify no regressions in existing graphEditor tests with `./gradlew :graphEditor:test`.

**Checkpoint**: At this point, the IP type dropdown should appear on port rows and selecting a type should update the port. Save/load should preserve selections (existing serialization handles this).

---

## Phase 4: User Story 2 - Port Type Propagation to Connections and Remote Ports (Priority: P2)

**Goal**: When a port's IP type changes, automatically propagate the change to all attached connections (update ipTypeId) and to the port on the other end of each connection (update dataType).

**Independent Test**: Create two connected nodes, change a port type from "Any" to "Int", verify the connection's IP type updates to "ip_int" and the remote port's data type updates to Int. Test with a port connected to multiple nodes to verify all connections and remote ports update.

### Implementation for User Story 2

- [ ] T006 [US2] Extend `updatePortType()` in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphState.kt` with propagation logic - After updating the port (from T001), find all connections where `sourcePortId == portId` or `targetPortId == portId`. For each connection: (a) copy connection with `ipTypeId = ipType.id`, (b) find the remote port (the other end), (c) copy remote port with `dataType = ipType.payloadType`, (d) copy remote node with updated port lists. Apply all updates to flowGraph in a single pass (non-recursive per D2 in plan.md). Follow the contract in `contracts/ui-interactions.md` Interaction 2.

- [ ] T007 [US2] Verify US2 propagation - Run `./gradlew :graphEditor:compileKotlinJvm` and `./gradlew :graphEditor:test`. Manually verify: open graphEditor, create two connected Generic Nodes, change a port type, confirm connection color changes and remote port dropdown updates.

**Checkpoint**: Port type changes now propagate through the full data path (source port -> connection -> target port).

---

## Phase 5: User Story 3 - Connection Type Change Propagates to Attached Ports (Priority: P3)

**Goal**: When a connection's IP type changes, propagate the change to both the source port and target port, completing the bidirectional "last change wins" model.

**Independent Test**: Change a connection's IP type and verify both the source and target ports update their data type to match.

### Implementation for User Story 3

- [ ] T008 [US3] Extend `updateConnectionIPType()` in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/GraphState.kt` to propagate to both attached ports - After updating the connection's ipTypeId (existing logic at lines ~1255-1265), resolve the ipType from IPTypeRegistry.getById(). Find source port (via connection.sourceNodeId + sourcePortId) and target port (via targetNodeId + targetPortId). Copy both ports with `dataType = ipType.payloadType`, update their owning nodes. Follow the contract in `contracts/ui-interactions.md` Interaction 3.

- [ ] T009 [US3] Verify US3 propagation - Run `./gradlew :graphEditor:compileKotlinJvm` and `./gradlew :graphEditor:test`. Manually verify: change a connection type and confirm both port dropdowns reflect the new type.

**Checkpoint**: Bidirectional propagation complete. Any type change (port or connection) propagates to all connected elements.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation across all user stories.

- [ ] T010 Run quickstart.md validation scenarios - Verify all 3 workflows from quickstart.md: (1) Setting port types on a new node, (2) Changing types on an existing graph with propagation, (3) Persistence round-trip (save, close, reopen, verify). Document results.

- [ ] T011 Full build verification and edge case review - Run `./gradlew :graphEditor:compileKotlinJvm` and `./gradlew :graphEditor:test`. Review edge cases from spec.md: non-Generic nodes, cyclic propagation (single pass), simultaneous name+type changes, deleted connections retaining port types.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No tasks needed
- **Foundational (Phase 2)**: No tasks needed - all infrastructure exists
- **US1 (Phase 3)**: Can start immediately
- **US2 (Phase 4)**: Depends on T001 (extends `updatePortType()` from US1)
- **US3 (Phase 5)**: Can start after Phase 2 (extends existing `updateConnectionIPType()`)
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: No dependencies - can start immediately
  - T001 and T002 are parallel [P] (different files: GraphState.kt vs PropertiesPanelViewModel.kt)
  - T003 depends on T002 (calls ViewModel method)
  - T004 depends on T001 and T002 (wires callback between them)
  - T005 depends on T001-T004
- **User Story 2 (P2)**: Depends on T001 from US1 (extends the same method)
- **User Story 3 (P3)**: Independent of other stories (extends a different existing method), but logically follows US2

### Within Each User Story

- State logic before UI (GraphState before PropertiesPanel)
- ViewModel before UI (PropertiesPanel calls ViewModel methods)
- Wiring (Main.kt) after both state and ViewModel are ready
- Build verification last

### Parallel Opportunities

```bash
# Phase 3 - US1 parallel tasks (different files):
T001: GraphState.kt - Add updatePortType()
T002: PropertiesPanelViewModel.kt - Add updatePortType() + callback

# Phase 4 + Phase 5 could theoretically overlap:
T006: Extends GraphState.updatePortType() (US2)
T008: Extends GraphState.updateConnectionIPType() (US3)
# But T008 is a different method, so T006 and T008 could run in parallel
# if T001 is already complete
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 3: User Story 1 (T001-T005)
2. **STOP and VALIDATE**: Dropdown appears, type selection works, persists on save/load
3. This alone provides value - users can assign types to ports

### Incremental Delivery

1. US1 (T001-T005) → Dropdown works, port types assignable → MVP
2. US2 (T006-T007) → Port type changes propagate to connections and remote ports
3. US3 (T008-T009) → Connection type changes propagate to ports → Full bidirectional model
4. Polish (T010-T011) → Validated against all quickstart scenarios and edge cases

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Total: 11 tasks across 4 files
- No new files created - all changes extend existing graphEditor module code
- No schema/model changes needed - Port.dataType and Connection.ipTypeId already exist
- Serialization is already handled by existing FlowGraphSerializer
