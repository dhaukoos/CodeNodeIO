# Tasks: Node ExecutionState and ControlConfig

**Input**: Design documents from `/specs/007-node-execution-control/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/ ✓

**Tests**: TDD is mandatory per project constitution. Tests are written first and must FAIL before implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **fbpDsl module**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/` and `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/`
- **graphEditor module**: `graphEditor/src/jvmMain/kotlin/` and `graphEditor/src/jvmTest/kotlin/`
- **kotlinCompiler module**: `kotlinCompiler/src/jvmMain/kotlin/io/codenode/kotlincompiler/`

---

## Phase 1: Setup and Extraction

**Purpose**: Extract ExecutionState and ControlConfig to separate files; prepare for Node base class modifications

- [X] T001 Create ExecutionState enum file at `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/ExecutionState.kt` with IDLE, RUNNING, PAUSED, ERROR values (extracted from CodeNode)
- [X] T002 Create ControlConfig data class file at `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/ControlConfig.kt` with pauseBufferSize, speedAttenuation, autoResumeOnError, and new independentControl flag
- [X] T003 Update CodeNode imports in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt` to use extracted ExecutionState and ControlConfig (remove internal definitions)
- [X] T004 Verify all existing tests pass after extraction by running `./gradlew :fbpDsl:jvmTest`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Add abstract properties to Node sealed class - MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T005 Write test for Node abstract properties in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/NodeExecutionStateTest.kt` - verify CodeNode and GraphNode both have executionState and controlConfig
- [X] T006 Add abstract `executionState: ExecutionState` property to Node sealed class in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Node.kt`
- [X] T007 Add abstract `controlConfig: ControlConfig` property to Node sealed class in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Node.kt`
- [X] T008 Add abstract `withExecutionState(ExecutionState, Boolean): Node` method to Node in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Node.kt`
- [X] T009 Add abstract `withControlConfig(ControlConfig, Boolean): Node` method to Node in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Node.kt`
- [X] T010 Add `executionState` and `controlConfig` properties to GraphNode in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/GraphNode.kt` with default values
- [X] T011 Implement `withExecutionState` in GraphNode (no propagation yet, just copy) in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/GraphNode.kt`
- [X] T012 Implement `withControlConfig` in GraphNode (no propagation yet, just copy) in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/GraphNode.kt`
- [X] T013 Implement `withExecutionState` in CodeNode (simple copy) in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt`
- [X] T014 Implement `withControlConfig` in CodeNode (simple copy) in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt`
- [X] T015 Verify all existing tests still pass and T005 tests pass by running `./gradlew :fbpDsl:jvmTest`

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Hierarchical Execution Control (Priority: P1) 🎯 MVP

**Goal**: Enable GraphNode to propagate execution state changes to all descendants

**Independent Test**: Create a GraphNode containing multiple CodeNodes, change the parent's execution state, and verify all children reflect the new state

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T016 [P] [US1] Write test for basic state propagation in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/StatePropagationTest.kt` - parent IDLE→RUNNING propagates to all children
- [X] T017 [P] [US1] Write test for nested propagation in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/StatePropagationTest.kt` - state propagates through nested GraphNodes to all descendants
- [X] T018 [P] [US1] Write test for ERROR propagation in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/StatePropagationTest.kt` - parent ERROR transitions all children to ERROR
- [X] T019 [P] [US1] Write test for propagate=false mode in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/StatePropagationTest.kt` - only target node changes

### Implementation for User Story 1

- [X] T020 [US1] Implement `propagateStateToChildren(ExecutionState)` helper in GraphNode at `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/GraphNode.kt`
- [X] T021 [US1] Update `withExecutionState` in GraphNode to call `propagateStateToChildren` when propagate=true at `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/GraphNode.kt`
- [X] T022 [US1] Ensure recursive propagation works for nested GraphNodes (GraphNode children call their own withExecutionState)
- [X] T023 [US1] Run US1 tests and verify all pass: `./gradlew :fbpDsl:jvmTest --tests "io.codenode.fbpdsl.model.StatePropagationTest"`

**Checkpoint**: User Story 1 (MVP) complete - hierarchical state propagation works

---

## Phase 4: User Story 2 - Selective Subgraph Control Override (Priority: P2)

**Goal**: Enable independent control mode where nodes are exempt from parent state propagation

**Independent Test**: Set a subgraph to independentControl=true, change parent state, verify subgraph retains its own state

### Tests for User Story 2 ⚠️

- [X] T024 [P] [US2] Write test for independentControl flag in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/StatePropagationTest.kt` - node with independentControl=true is skipped during propagation
- [X] T025 [P] [US2] Write test for direct control of independent node in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/StatePropagationTest.kt` - independent node can be controlled directly
- [X] T026 [P] [US2] Write test for independent node propagating to its own children in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/StatePropagationTest.kt`
- [X] T027 [P] [US2] Write test for nested independent boundaries in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/StatePropagationTest.kt` - nothing below independent boundary affected by parent changes

### Implementation for User Story 2

- [X] T028 [US2] Update `propagateStateToChildren` to check `child.controlConfig.independentControl` and skip if true at `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/GraphNode.kt`
- [X] T029 [US2] Implement `propagateConfigToChildren(ControlConfig)` helper in GraphNode (respecting independentControl) at `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/GraphNode.kt`
- [X] T030 [US2] Update `withControlConfig` in GraphNode to call `propagateConfigToChildren` when propagate=true at `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/GraphNode.kt`
- [X] T031 [US2] Run US2 tests and verify all pass: `./gradlew :fbpDsl:jvmTest --tests "io.codenode.fbpdsl.model.StatePropagationTest"`

**Checkpoint**: User Story 2 complete - independent control isolation works

---

## Phase 5: User Story 3 - RootControlNode Master Control (Priority: P3)

**Goal**: Create RootControlNode class as master controller for entire FlowGraph

**Independent Test**: Create a RootControlNode for a flowGraph, use startAll/pauseAll/stopAll, verify all nodes respond correctly

### Tests for User Story 3 ⚠️

- [X] T032 [P] [US3] Write test for RootControlNode.createFor() factory in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/RootControlNodeTest.kt`
- [X] T033 [P] [US3] Write test for startAll() in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/RootControlNodeTest.kt` - all nodes transition to RUNNING
- [X] T034 [P] [US3] Write test for pauseAll() in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/RootControlNodeTest.kt` - all nodes transition to PAUSED
- [X] T035 [P] [US3] Write test for stopAll() in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/RootControlNodeTest.kt` - all nodes transition to IDLE
- [X] T036 [P] [US3] Write test for getStatus() in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/RootControlNodeTest.kt` - returns accurate FlowExecutionStatus
- [X] T037 [P] [US3] Write test for setNodeState() in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/RootControlNodeTest.kt` - targets specific node by ID

### Implementation for User Story 3

- [X] T038 [US3] Create FlowExecutionStatus data class at `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/FlowExecutionStatus.kt` with totalNodes, idleCount, runningCount, pausedCount, errorCount, overallState
- [X] T039 [US3] Create RootControlNode class at `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/RootControlNode.kt` with id, flowGraph, name, createdAt properties
- [X] T040 [US3] Implement `createFor()` companion factory method in RootControlNode at `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/RootControlNode.kt`
- [X] T041 [US3] Implement `startAll()` method in RootControlNode - sets all root nodes to RUNNING at `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/RootControlNode.kt`
- [X] T042 [US3] Implement `pauseAll()` method in RootControlNode - sets all root nodes to PAUSED at `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/RootControlNode.kt`
- [X] T043 [US3] Implement `stopAll()` method in RootControlNode - sets all root nodes to IDLE at `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/RootControlNode.kt`
- [X] T044 [US3] Implement `getStatus()` method in RootControlNode - traverses graph and counts states at `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/RootControlNode.kt`
- [X] T045 [US3] Implement `setNodeState()` method in RootControlNode - finds node by ID and sets state at `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/RootControlNode.kt`
- [X] T046 [US3] Implement `setNodeConfig()` method in RootControlNode - finds node by ID and sets config at `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/RootControlNode.kt`
- [X] T047 [US3] Run US3 tests and verify all pass: `./gradlew :fbpDsl:jvmTest --tests "io.codenode.fbpdsl.model.RootControlNodeTest"`

**Checkpoint**: User Story 3 complete - RootControlNode provides unified flow control

---

## Phase 6: User Story 4 - Speed Attenuation Propagation (Priority: P4)

**Goal**: ControlConfig changes (including speedAttenuation) propagate to children following same rules as executionState

**Independent Test**: Set speedAttenuation on a GraphNode, verify all child nodes inherit the attenuation value

### Tests for User Story 4 ⚠️

- [ ] T048 [P] [US4] Write test for speedAttenuation propagation in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/StatePropagationTest.kt` - parent speedAttenuation=500 propagates to children
- [ ] T049 [P] [US4] Write test for child override in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/StatePropagationTest.kt` - child's own speedAttenuation takes precedence in its subtree
- [ ] T050 [P] [US4] Write test for independentControl respecting config in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/StatePropagationTest.kt` - independent nodes retain their own ControlConfig

### Implementation for User Story 4

- [ ] T051 [US4] Ensure `propagateConfigToChildren` correctly propagates speedAttenuation, pauseBufferSize, autoResumeOnError (but not independentControl) at `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/GraphNode.kt`
- [ ] T052 [US4] Add `getEffectiveControlConfig()` method to Node (abstract) and implement in CodeNode and GraphNode at respective files
- [ ] T053 [US4] Run US4 tests and verify all pass: `./gradlew :fbpDsl:jvmTest --tests "io.codenode.fbpdsl.model.StatePropagationTest"`

**Checkpoint**: User Story 4 complete - controlConfig propagation works correctly

---

## Phase 7: User Story 5 - KMP Module Generation (Priority: P5)

**Goal**: Generate KMP module from flowGraph with developer-specified module name

**Independent Test**: Compile a simple flowGraph to a named KMP module, verify module structure is created

### Tests for User Story 5 ⚠️

- [ ] T054 [P] [US5] Write test for module structure generation in `kotlinCompiler/src/jvmTest/kotlin/io/codenode/kotlincompiler/ModuleGeneratorTest.kt` - verify directory structure created
- [ ] T055 [P] [US5] Write test for build.gradle.kts generation in `kotlinCompiler/src/jvmTest/kotlin/io/codenode/kotlincompiler/ModuleGeneratorTest.kt` - verify KMP gradle config
- [ ] T056 [P] [US5] Write test for FlowGraph class generation in `kotlinCompiler/src/jvmTest/kotlin/io/codenode/kotlincompiler/ModuleGeneratorTest.kt` - verify flow instantiation code
- [ ] T057 [P] [US5] Write test for RootControlNode wrapper generation in `kotlinCompiler/src/jvmTest/kotlin/io/codenode/kotlincompiler/ModuleGeneratorTest.kt`

### Implementation for User Story 5

- [ ] T058 [US5] Create ModuleGenerator class at `kotlinCompiler/src/jvmMain/kotlin/io/codenode/kotlincompiler/ModuleGenerator.kt`
- [ ] T059 [US5] Implement `generateModuleStructure(flowGraph, moduleName, outputPath)` method to create directory structure
- [ ] T060 [US5] Implement `generateBuildGradle(moduleName)` to create KMP build.gradle.kts content
- [ ] T061 [US5] Implement `generateFlowGraphClass(flowGraph, packageName)` to create flow instantiation code
- [ ] T062 [US5] Implement `generateControllerClass(flowGraph, packageName)` to create RootControlNode wrapper
- [ ] T063 [US5] Integrate module generation with graphEditor compilation workflow at `graphEditor/src/jvmMain/kotlin/`
- [ ] T064 [US5] Run US5 tests and verify all pass: `./gradlew :kotlinCompiler:jvmTest --tests "io.codenode.kotlincompiler.ModuleGeneratorTest"`

**Checkpoint**: User Story 5 complete - KMP module generation works

---

## Phase 8: graphEditor Integration

**Purpose**: Integrate execution control features into graphEditor UI state

- [ ] T065 [P] Write test for GraphState execution control operations in `graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/state/ExecutionControlTest.kt`
- [ ] T066 Add `setNodeExecutionState(nodeId, state)` method to GraphState at `graphEditor/src/jvmMain/kotlin/state/GraphState.kt`
- [ ] T067 Add `setNodeControlConfig(nodeId, config)` method to GraphState at `graphEditor/src/jvmMain/kotlin/state/GraphState.kt`
- [ ] T068 Add helper methods `findNodeById(nodeId)` to traverse FlowGraph and locate nodes at `graphEditor/src/jvmMain/kotlin/state/GraphState.kt`
- [ ] T069 Run graphEditor tests: `./gradlew :graphEditor:jvmTest --tests "io.codenode.grapheditor.state.ExecutionControlTest"`

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T070 [P] Write performance test for state propagation (<100ms for 100 nodes) in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/StatePropagationTest.kt`
- [ ] T071 [P] Write performance test for RootControlNode getStatus (<10ms) in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/RootControlNodeTest.kt`
- [ ] T072 Add validation for circular parent references in GraphNode at `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/GraphNode.kt`
- [ ] T073 Run quickstart.md validation scenarios manually to verify all 5 scenarios work
- [ ] T074 [P] Verify Apache 2.0 license headers on all new files
- [ ] T075 Run full test suite: `./gradlew test`
- [ ] T076 Update serialization tests to verify backward compatibility with existing .flow.kts files

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3 → P4 → P5)
- **graphEditor Integration (Phase 8)**: Depends on US1-US3 completion (needs core model)
- **Polish (Phase 9)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Builds on US1 propagation mechanism
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Uses propagation from US1/US2
- **User Story 4 (P4)**: Can start after US2 (needs config propagation) - Extends controlConfig handling
- **User Story 5 (P5)**: Can start after US3 (needs RootControlNode) - Depends on RootControlNode being defined

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Entity definitions before methods
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- **Phase 1**: T001 and T002 can run in parallel (different files)
- **Phase 2**: T005-T009 tests can be written in parallel; T010-T014 implementations can be parallel after abstracts defined
- **Phase 3**: T016-T019 tests can run in parallel
- **Phase 4**: T024-T027 tests can run in parallel
- **Phase 5**: T032-T037 tests can run in parallel; T038-T039 can be parallel
- **Phase 6**: T048-T050 tests can run in parallel
- **Phase 7**: T054-T057 tests can run in parallel

---

## Parallel Example: User Story 3

```bash
# Launch all tests for User Story 3 together:
Task: T032 "Write test for RootControlNode.createFor() factory"
Task: T033 "Write test for startAll()"
Task: T034 "Write test for pauseAll()"
Task: T035 "Write test for stopAll()"
Task: T036 "Write test for getStatus()"
Task: T037 "Write test for setNodeState()"

# Launch parallel model creation:
Task: T038 "Create FlowExecutionStatus data class"
Task: T039 "Create RootControlNode class structure"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T004)
2. Complete Phase 2: Foundational (T005-T015)
3. Complete Phase 3: User Story 1 (T016-T023)
4. **STOP and VALIDATE**: Test hierarchical state propagation independently
5. Deploy/demo if ready - basic execution control is functional

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Independent control isolation works
4. Add User Story 3 → RootControlNode master control works
5. Add User Story 4 → ControlConfig propagation works
6. Add User Story 5 → KMP module generation works
7. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (P1) - core propagation
   - Developer B: User Story 3 (P3) - can start RootControlNode structure early
3. After US1 complete:
   - Developer A: User Story 2 (P2) - independent control
   - Developer B: Continue US3 implementation
4. US4 and US5 can proceed after their dependencies complete

---

## Summary

| Phase | Story | Task Count | Parallel Tasks |
|-------|-------|------------|----------------|
| Phase 1: Setup | - | 4 | 2 |
| Phase 2: Foundational | - | 11 | 5 |
| Phase 3: User Story 1 | US1 | 8 | 4 (tests) |
| Phase 4: User Story 2 | US2 | 8 | 4 (tests) |
| Phase 5: User Story 3 | US3 | 16 | 6 (tests) |
| Phase 6: User Story 4 | US4 | 6 | 3 (tests) |
| Phase 7: User Story 5 | US5 | 11 | 4 (tests) |
| Phase 8: graphEditor | - | 5 | 1 |
| Phase 9: Polish | - | 7 | 3 |
| **TOTAL** | | **76** | **32** |

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- TDD mandatory per project constitution
