# Tasks: PassThruPort and ConnectionSegment

**Input**: Design documents from `/specs/006-passthru-port-segments/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Tests are REQUIRED per project constitution (TDD mandatory). All test tasks must be completed before implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4, US5)
- Include exact file paths in descriptions

## Path Conventions

- **Multi-module KMP project**: `fbpDsl/`, `graphEditor/` at repository root
- Paths assume multi-module structure per plan.md

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure for PassThruPort/ConnectionSegment feature

- [ ] T001 Verify existing Port model structure in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Port.kt
- [ ] T002 [P] Create model package directory for new entities at fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/
- [ ] T003 [P] Create test directory at fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core model classes that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Tests for Foundation (TDD - Write FIRST, must FAIL) ⚠️

- [ ] T004 [P] Write unit tests for PassThruPort data class (composition, property delegation) in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/PassThruPortTest.kt
- [ ] T005 [P] Write unit tests for ConnectionSegment data class in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/ConnectionSegmentTest.kt
- [ ] T006 [P] Write unit tests for Connection.segments property in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/ConnectionSegmentTest.kt

### Implementation for Foundation

- [ ] T007 [P] Create PassThruPort data class with port composition and upstream/downstream references in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/PassThruPort.kt
- [ ] T008 [P] Create ConnectionSegment data class with source/target/scope references in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/ConnectionSegment.kt
- [ ] T009 Add segments property to Connection with cached computation in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Connection.kt
- [ ] T010 Add invalidateSegments() method to Connection in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Connection.kt
- [ ] T011 Add PortShape enum (CIRCLE, SQUARE) in graphEditor/src/jvmMain/kotlin/rendering/PortRenderer.kt
- [ ] T012 Verify T004-T006 tests pass after implementation

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - PassThruPort Creation on GraphNode Boundary (Priority: P1) 🎯 MVP

**Goal**: Automatically create PassThruPorts when grouping nodes with boundary-crossing connections

**Independent Test**: Group 3 nodes where one has an external incoming connection and another has an external outgoing connection. Verify PassThruPorts are created with correct upstream/downstream references.

### Tests for User Story 1 (TDD - Write FIRST, must FAIL) ⚠️

- [ ] T013 [P] [US1] Write unit tests for PassThruPortFactory.create() in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/factory/PassThruPortFactoryTest.kt
- [ ] T014 [P] [US1] Write unit tests for PassThruPort type validation (matching dataTypes) in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/factory/PassThruPortFactoryTest.kt
- [ ] T015 [P] [US1] Write unit tests for PassThruPort direction validation in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/factory/PassThruPortFactoryTest.kt
- [ ] T016 [P] [US1] Write integration test for grouping creating PassThruPorts in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/PassThruPortCreationTest.kt

### Implementation for User Story 1

- [ ] T017 [US1] Create PassThruPortFactory object with create() method in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/PassThruPortFactory.kt
- [ ] T018 [US1] Implement type compatibility validation in PassThruPortFactory in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/PassThruPortFactory.kt
- [ ] T019 [US1] Implement direction matching validation in PassThruPortFactory in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/PassThruPortFactory.kt
- [ ] T020 [US1] Modify GraphNodeFactory.createFromSelection() to call PassThruPortFactory in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/GraphNodeFactory.kt
- [ ] T021 [US1] Update GraphNodeFactory to add PassThruPorts to inputPorts/outputPorts in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/GraphNodeFactory.kt
- [ ] T022 [US1] Add error handling for type mismatch failures in GraphNodeFactory in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/GraphNodeFactory.kt
- [ ] T023 [US1] Verify T013-T016 tests pass after implementation

**Checkpoint**: User Story 1 complete - PassThruPorts are automatically created when grouping

---

## Phase 4: User Story 2 - ConnectionSegment Representation (Priority: P2)

**Goal**: Connections display as one or more segments; boundary crossings have multiple segments

**Independent Test**: Create a connection between two CodeNodes (1 segment). Group one node, verify connection now has 2 segments.

### Tests for User Story 2 (TDD - Write FIRST, must FAIL) ⚠️

- [ ] T024 [P] [US2] Write unit tests for Connection.computeSegments() single segment case in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/ConnectionSegmentTest.kt
- [ ] T025 [P] [US2] Write unit tests for Connection.computeSegments() two segment case in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/ConnectionSegmentTest.kt
- [ ] T026 [P] [US2] Write unit tests for Connection.computeSegments() three segment case (nested) in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/ConnectionSegmentTest.kt
- [ ] T027 [P] [US2] Write unit tests for segment chain validation in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/ConnectionSegmentTest.kt

### Implementation for User Story 2

- [ ] T028 [US2] Implement Connection.computeSegments() for direct CodeNode-to-CodeNode in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Connection.kt
- [ ] T029 [US2] Implement Connection.computeSegments() for single boundary crossing in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Connection.kt
- [ ] T030 [US2] Implement Connection.computeSegments() for nested boundary crossings in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Connection.kt
- [ ] T031 [US2] Add validateSegmentChain() method to Connection in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Connection.kt
- [ ] T032 [US2] Verify T024-T027 tests pass after implementation

**Checkpoint**: User Story 2 complete - Connections correctly segment at boundaries

---

## Phase 5: User Story 3 - Visual Distinction of PassThruPorts (Priority: P3)

**Goal**: Regular Ports render as circles, PassThruPorts render as squares

**Independent Test**: Render GraphNode with PassThruPorts and CodeNode with regular Ports, verify shapes are distinct.

### Tests for User Story 3 (TDD - Write FIRST, must FAIL) ⚠️

- [ ] T033 [P] [US3] Write unit tests for Port.getPortShape() returning CIRCLE in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/rendering/PortRenderingTest.kt
- [ ] T034 [P] [US3] Write unit tests for PassThruPort.getPortShape() returning SQUARE in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/rendering/PortRenderingTest.kt
- [ ] T035 [P] [US3] Write rendering tests for circle vs square port shapes in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/rendering/PortRenderingTest.kt

### Implementation for User Story 3

- [ ] T036 [US3] Add getPortShape() extension function for Port in graphEditor/src/jvmMain/kotlin/rendering/PortRenderer.kt
- [ ] T037 [US3] Implement renderPort() with shape parameter in graphEditor/src/jvmMain/kotlin/rendering/PortRenderer.kt
- [ ] T038 [US3] Update NodeRenderer to call renderPort() with correct shape in graphEditor/src/jvmMain/kotlin/rendering/NodeRenderer.kt
- [ ] T039 [US3] Implement renderBoundaryPorts() for GraphNode boundary in graphEditor/src/jvmMain/kotlin/rendering/PortRenderer.kt
- [ ] T040 [US3] Integrate boundary port rendering into GraphNodeRenderer in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphNodeRenderer.kt
- [ ] T041 [US3] Verify T033-T035 tests pass after implementation

**Checkpoint**: User Story 3 complete - Port shapes visually distinguish PassThruPorts

---

## Phase 6: User Story 4 - Automatic Segment Creation When Grouping (Priority: P4)

**Goal**: Grouping automatically splits connections into segments at boundaries

**Independent Test**: Create flow A->B->C, group B, verify both connections have 2 segments with PassThruPorts joining them.

### Tests for User Story 4 (TDD - Write FIRST, must FAIL) ⚠️

- [ ] T042 [P] [US4] Write integration test for incoming connection segmentation in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/SegmentCreationTest.kt
- [ ] T043 [P] [US4] Write integration test for outgoing connection segmentation in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/SegmentCreationTest.kt
- [ ] T044 [P] [US4] Write integration test for segment merging on ungroup in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/SegmentCreationTest.kt

### Implementation for User Story 4

- [ ] T045 [US4] Update GraphState.groupSelectedNodes() to call invalidateSegments() in graphEditor/src/jvmMain/kotlin/state/GraphState.kt
- [ ] T046 [US4] Update GraphState.ungroupGraphNode() to merge segments in graphEditor/src/jvmMain/kotlin/state/GraphState.kt
- [ ] T047 [US4] Add helper method to detect boundary-crossing connections in graphEditor/src/jvmMain/kotlin/state/GraphState.kt
- [ ] T048 [US4] Verify T042-T044 tests pass after implementation

**Checkpoint**: User Story 4 complete - Segments auto-create/merge with group/ungroup

---

## Phase 7: User Story 5 - Segment Visibility by Navigation Context (Priority: P5)

**Goal**: Only segments relevant to current view context are rendered

**Independent Test**: Navigate into GraphNode, verify only interior segments shown. Navigate out, verify only exterior segments shown.

### Tests for User Story 5 (TDD - Write FIRST, must FAIL) ⚠️

- [ ] T049 [P] [US5] Write unit tests for getSegmentsInContext() at root level in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/SegmentVisibilityTest.kt
- [ ] T050 [P] [US5] Write unit tests for getSegmentsInContext() inside GraphNode in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/SegmentVisibilityTest.kt
- [ ] T051 [P] [US5] Write unit tests for getSegmentsInContext() with nested GraphNodes in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/SegmentVisibilityTest.kt

### Implementation for User Story 5

- [ ] T052 [US5] Add getSegmentsInContext() method to GraphState in graphEditor/src/jvmMain/kotlin/state/GraphState.kt
- [ ] T053 [US5] Update getConnectionsInCurrentContext() to return filtered segments in graphEditor/src/jvmMain/kotlin/state/GraphState.kt
- [ ] T054 [US5] Update ConnectionRenderer to render segments instead of full connections in graphEditor/src/jvmMain/kotlin/rendering/ConnectionRenderer.kt
- [ ] T055 [US5] Update FlowGraphCanvas to pass current scope to rendering in graphEditor/src/jvmMain/kotlin/ui/FlowGraphCanvas.kt
- [ ] T056 [US5] Verify T049-T051 tests pass after implementation

**Checkpoint**: User Story 5 complete - Segment visibility tied to navigation context

---

## Phase 8: Serialization & Persistence

**Purpose**: Complete PassThruPort and ConnectionSegment serialization support for .flow.kts files

### Tests for Serialization (TDD - Write FIRST, must FAIL) ⚠️

- [ ] T057 [P] Write unit tests for PassThruPort serialization in graphEditor/src/jvmTest/kotlin/serialization/PassThruPortSerializationTest.kt
- [ ] T058 [P] Write unit tests for ConnectionSegment serialization in graphEditor/src/jvmTest/kotlin/serialization/ConnectionSegmentSerializationTest.kt
- [ ] T059 [P] Write unit tests for backward compatibility (files without PassThruPorts) in graphEditor/src/jvmTest/kotlin/serialization/BackwardCompatibilityTest.kt

### Implementation for Serialization

- [ ] T060 Add PassThruPort serialization to FlowGraphSerializer in graphEditor/src/jvmMain/kotlin/serialization/FlowGraphSerializer.kt
- [ ] T061 Add ConnectionSegment serialization to FlowGraphSerializer in graphEditor/src/jvmMain/kotlin/serialization/FlowGraphSerializer.kt
- [ ] T062 Add PassThruPort deserialization to FlowGraphDeserializer in graphEditor/src/jvmMain/kotlin/serialization/FlowGraphDeserializer.kt
- [ ] T063 Add ConnectionSegment deserialization to FlowGraphDeserializer in graphEditor/src/jvmMain/kotlin/serialization/FlowGraphDeserializer.kt
- [ ] T064 Implement backward compatibility (upgrade old files on load) in graphEditor/src/jvmMain/kotlin/serialization/FlowGraphDeserializer.kt
- [ ] T065 Verify T057-T059 tests pass after implementation

**Checkpoint**: Serialization complete - PassThruPorts and segments persist correctly

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T066 [P] Add PassThruPort hover state styling in graphEditor/src/jvmMain/kotlin/rendering/PortRenderer.kt
- [ ] T067 [P] Add segment selection highlighting (all segments for selected connection) in graphEditor/src/jvmMain/kotlin/rendering/ConnectionRenderer.kt
- [ ] T068 Verify edge case: PassThruPort upstream/downstream port deletion in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/EdgeCaseTest.kt
- [ ] T069 Verify edge case: self-loop connections stay single-segment in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/EdgeCaseTest.kt
- [ ] T070 Run quickstart.md validation scenarios (all 7 scenarios pass)
- [ ] T071 Performance test: PassThruPort creation < 500ms
- [ ] T072 Performance test: segment visibility switching < 100ms
- [ ] T073 Final code review for Apache 2.0 header compliance on all new files

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Foundational phase completion
  - US1 must complete before US2 (segments need PassThruPorts to cross)
  - US2 must complete before US4 (auto-segmentation needs segment computation)
  - US2 must complete before US5 (visibility filtering needs segments)
  - US3 can run in parallel with US2, US4, US5 (rendering is independent)
- **Serialization (Phase 8)**: Can run after US1 and US2 complete
- **Polish (Phase 9)**: Depends on all user stories being complete

### User Story Dependencies

```
Setup (Phase 1)
    │
    ▼
Foundational (Phase 2)
    │
    ├──────────────────────┐
    ▼                      │
US1 (P1) PassThruPort      │
    │                      │
    ▼                      │
US2 (P2) Segments ─────────┤
    │                      │
    ├──────────────────────┤
    │                      ▼
    ├──────────────► US3 (P3) Visual Distinction
    │
    ├─────────▶ US4 (P4) Auto-Segment on Group
    │
    └─────────▶ US5 (P5) Segment Visibility
                   │
                   ▼
            Serialization (Phase 8)
                   │
                   ▼
             Polish (Phase 9)
```

### Within Each User Story

- Tests (TDD) MUST be written and FAIL before implementation
- Models before factories
- Factories before integration
- Core logic before UI rendering
- Story complete before moving to dependent stories

### Parallel Opportunities

- **Phase 1**: T002 and T003 can run in parallel
- **Phase 2**: T004, T005, T006 can all run in parallel; T007, T008 can run in parallel
- **Phase 3 (US1)**: T013, T014, T015, T016 can all run in parallel
- **Phase 4 (US2)**: T024, T025, T026, T027 can all run in parallel
- **Phase 5 (US3)**: T033, T034, T035 can all run in parallel
- **Phase 6 (US4)**: T042, T043, T044 can all run in parallel
- **Phase 7 (US5)**: T049, T050, T051 can all run in parallel
- **Phase 8**: T057, T058, T059 can all run in parallel
- **Phase 9**: T066, T067 can run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together (TDD - write these first):
Task: T013 "Unit test for PassThruPortFactory.create()"
Task: T014 "Unit test for PassThruPort type validation"
Task: T015 "Unit test for PassThruPort direction validation"
Task: T016 "Integration test for grouping creating PassThruPorts"

# After tests fail, launch sequential implementation tasks:
Task: T017 "Create PassThruPortFactory object"
Task: T018 "Implement type compatibility validation"
Task: T019 "Implement direction matching validation"
Task: T020 "Modify GraphNodeFactory to call PassThruPortFactory"
Task: T021 "Add PassThruPorts to inputPorts/outputPorts"
Task: T022 "Add error handling for type mismatch"
Task: T023 "Verify all tests pass"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (PassThruPort Creation)
4. **STOP and VALIDATE**: Test PassThruPort creation independently
5. Deploy/demo if ready

**MVP Deliverable**: Developers can group nodes and PassThruPorts are automatically created at boundaries with type validation.

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → **PassThruPort MVP!**
3. Add User Story 2 → Test independently → **Segment representation**
4. Add User Story 3 → Test independently → **Visual distinction (squares)**
5. Add User Story 4 → Test independently → **Auto-segment on group**
6. Add User Story 5 → Test independently → **Context-aware visibility**
7. Add Serialization → **Persistence complete**
8. Polish → **Production ready**

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 → User Story 2
   - Developer B: User Story 3 (can start after Foundational, independent of US1/US2)
3. After US2 complete:
   - Developer A: User Story 4 + User Story 5
   - Developer B: Serialization
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

## Total Task Count: 73 tasks

### Tasks per Phase:
- **Setup**: 3 tasks
- **Foundational**: 9 tasks (3 tests + 6 implementation)
- **US1 (PassThruPort Creation)**: 11 tasks (4 tests + 7 implementation)
- **US2 (Segments)**: 9 tasks (4 tests + 5 implementation)
- **US3 (Visual Distinction)**: 9 tasks (3 tests + 6 implementation)
- **US4 (Auto-Segment)**: 7 tasks (3 tests + 4 implementation)
- **US5 (Visibility)**: 8 tasks (3 tests + 5 implementation)
- **Serialization**: 9 tasks (3 tests + 6 implementation)
- **Polish**: 8 tasks

### Parallel Opportunities:
- **Foundational phase**: 5 tasks can run in parallel
- **US1-US5 test phases**: All test tasks can run in parallel within each phase
- **US3**: Can run in parallel with US2, US4, US5 after Foundational

### Suggested MVP Scope:
**Phases 1-3 only (Setup + Foundational + US1)** = 23 tasks

**Deliverable**: Working PassThruPort creation with type validation when grouping nodes with boundary-crossing connections.
