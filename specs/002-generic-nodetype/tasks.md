# Tasks: Generic NodeType Definition

**Input**: Design documents from `/specs/002-generic-nodetype/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: TDD approach as mandated by project constitution - tests written before implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

Based on plan.md, this is a multi-module Kotlin Multiplatform project:
- `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/` - Core DSL module
- `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/` - DSL tests
- `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/` - Visual editor
- `graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/` - Editor tests
- `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/` - Code generator
- `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/` - Generator tests
- `idePlugin/src/main/resources/` - IDE plugin resources

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Core enum extension and factory foundation

- [x] T001 Add GENERIC value to NodeCategory enum in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/NodeTypeDefinition.kt
- [x] T002 Create factory package directory at fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/
- [x] T003 [P] Create test package directory at fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/factory/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Add GENERIC case to category-to-CodeNodeType mapping in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/DragAndDropHandler.kt (line ~219-225)
- [x] T005 Create GenericNodeConfiguration data class in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/GenericNodeConfiguration.kt
- [x] T006 Write unit tests for GenericNodeConfiguration validation in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/factory/GenericNodeConfigurationTest.kt

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Create Generic Node from Palette (Priority: P1) 🎯 MVP

**Goal**: Enable developers to drag generic node types from the palette onto the canvas with correct port configurations

**Independent Test**: Open graph editor, expand "Generic" category in palette, drag "in2out1" onto canvas, verify 2 input ports and 1 output port appear

### Tests for User Story 1 (TDD - Write FIRST, must FAIL before implementation)

- [ ] T007 [P] [US1] Write unit tests for createGenericNodeType factory function in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactoryTest.kt - test valid ranges (0-5)
- [ ] T008 [P] [US1] Write unit tests for factory invalid inputs (negative, >5) in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactoryTest.kt
- [ ] T009 [P] [US1] Write unit tests for default naming pattern "in{M}out{N}" in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactoryTest.kt
- [ ] T010 [P] [US1] Write unit tests for port template generation (input1, input2, output1, etc.) in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactoryTest.kt
- [ ] T011 [P] [US1] Write unit tests for getAllGenericNodeTypes returning 36 combinations in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactoryTest.kt
- [ ] T012 [P] [US1] Write unit tests for getCommonGenericNodeTypes returning 5 common types in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactoryTest.kt

### Implementation for User Story 1

- [ ] T013 [US1] Implement createGenericNodeType factory function in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactory.kt
- [ ] T014 [US1] Implement getAllGenericNodeTypes with lazy caching in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactory.kt
- [ ] T015 [US1] Implement getCommonGenericNodeTypes (in0out1, in1out0, in1out1, in1out2, in2out1) in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactory.kt
- [ ] T016 [US1] Add generic node types to sample nodes in graphEditor/src/jvmMain/kotlin/Main.kt createSampleNodeTypes() function
- [ ] T017 [US1] Write integration test verifying generic nodes appear in palette in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/ui/GenericNodePaletteTest.kt
- [ ] T018 [US1] Verify all T007-T012 tests pass after implementation

**Checkpoint**: User Story 1 complete - generic nodes can be created from palette with correct ports

---

## Phase 4: User Story 2 - Configure Generic Node Properties (Priority: P2)

**Goal**: Enable developers to customize generic node display name, icon, port names, and UseCase reference

**Independent Test**: Select a generic node, open properties panel, change name to "ValidateEmail", rename ports, verify changes persist in UI

### Tests for User Story 2 (TDD - Write FIRST, must FAIL before implementation)

- [ ] T019 [P] [US2] Write unit tests for custom name override in factory in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactoryTest.kt
- [ ] T020 [P] [US2] Write unit tests for custom port names in factory in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactoryTest.kt
- [ ] T021 [P] [US2] Write unit tests for iconResource parameter in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactoryTest.kt
- [ ] T022 [P] [US2] Write unit tests for useCaseClassName parameter in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactoryTest.kt
- [ ] T023 [P] [US2] Write unit tests for port name count mismatch validation in fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactoryTest.kt

### Implementation for User Story 2

- [ ] T024 [US2] Add customName parameter support to createGenericNodeType in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactory.kt
- [ ] T025 [US2] Add inputNames/outputNames parameters with validation in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactory.kt
- [ ] T026 [US2] Add iconResource parameter to createGenericNodeType in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactory.kt
- [ ] T027 [US2] Add useCaseClassName parameter to createGenericNodeType in fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactory.kt
- [ ] T028 [US2] Create default generic node icon at idePlugin/src/main/resources/icons/generic-node.svg
- [ ] T029 [US2] Verify all T019-T023 tests pass after implementation

**Checkpoint**: User Story 2 complete - generic nodes can be configured with custom properties

---

## Phase 5: User Story 4 - Serialize and Deserialize Generic Nodes (Priority: P2)

**Goal**: Generic nodes persist to .flow.kts files and reload correctly with all configuration intact

**Independent Test**: Create graph with configured generic node, save to file, close, reopen, verify all settings preserved

**Note**: US4 (serialization) is implemented before US3 (code gen) because code gen depends on proper serialization of UseCase references

### Tests for User Story 4 (TDD - Write FIRST, must FAIL before implementation)

- [ ] T030 [P] [US4] Write serialization test for generic node with _genericType metadata in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/serialization/GenericNodeSerializationTest.kt
- [ ] T031 [P] [US4] Write serialization test for generic node with _useCaseClass metadata in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/serialization/GenericNodeSerializationTest.kt
- [ ] T032 [P] [US4] Write serialization test for generic node with custom port names in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/serialization/GenericNodeSerializationTest.kt
- [ ] T033 [P] [US4] Write roundtrip deserialization test in graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/serialization/GenericNodeSerializationTest.kt

### Implementation for User Story 4

- [ ] T034 [US4] Update serializeCodeNode in FlowGraphSerializer to include _genericType metadata when present in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/serialization/FlowGraphSerializer.kt
- [ ] T035 [US4] Update serializeCodeNode to include _useCaseClass metadata when present in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/serialization/FlowGraphSerializer.kt
- [ ] T036 [US4] Verify FlowGraphDeserializer handles generic node metadata correctly in graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/serialization/FlowGraphDeserializer.kt
- [ ] T037 [US4] Verify all T030-T033 tests pass after implementation

**Checkpoint**: User Story 4 complete - generic nodes serialize and deserialize correctly

---

## Phase 6: User Story 3 - Generate Code from Generic Nodes (Priority: P3)

**Goal**: Code generation produces components that delegate to UseCase classes or include TODO placeholders

**Independent Test**: Create graph with generic node having UseCase reference, generate KMP code, verify component delegates to UseCase

### Tests for User Story 3 (TDD - Write FIRST, must FAIL before implementation)

- [ ] T038 [P] [US3] Write contract test for generated component with UseCase delegation in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/GenericNodeGeneratorTest.kt
- [ ] T039 [P] [US3] Write contract test for generated placeholder component (no UseCase) in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/GenericNodeGeneratorTest.kt
- [ ] T040 [P] [US3] Write contract test for custom port names in generated component in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/GenericNodeGeneratorTest.kt
- [ ] T041 [P] [US3] Write contract test for supportsGenericNode detection in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/GenericNodeGeneratorTest.kt

### Implementation for User Story 3

- [ ] T042 [US3] Create GenericNodeGenerator class with supportsGenericNode method in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/GenericNodeGenerator.kt
- [ ] T043 [US3] Implement generateComponent for nodes with UseCase reference in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/GenericNodeGenerator.kt
- [ ] T044 [US3] Implement generatePlaceholderComponent for nodes without UseCase in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/GenericNodeGenerator.kt
- [ ] T045 [US3] Integrate GenericNodeGenerator with KotlinCodeGenerator in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/KotlinCodeGenerator.kt
- [ ] T046 [US3] Verify all T038-T041 tests pass after implementation

**Checkpoint**: User Story 3 complete - code generation supports generic nodes

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and documentation

- [ ] T047 [P] Run full test suite to verify all 4 user stories work together
- [ ] T048 [P] Verify quickstart.md scenarios work as documented
- [ ] T049 [P] Verify backward compatibility - existing graphs still load correctly
- [ ] T050 Update Main.kt to include 5 common generic nodes in default palette
- [ ] T051 Final code review for Apache 2.0 header compliance on all new files

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-6)**: All depend on Foundational phase completion
  - US1 must complete before US2 (US2 extends factory function from US1)
  - US4 (serialization) should complete before US3 (code gen uses serialized metadata)
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

```
Setup (Phase 1)
    │
    ▼
Foundational (Phase 2)
    │
    ├───────────────────────┐
    ▼                       │
User Story 1 (P1) ──────────┤
    │                       │
    ▼                       │
User Story 2 (P2) ──────────┤
    │                       │
    ▼                       │
User Story 4 (P2) ◄─────────┘
    │
    ▼
User Story 3 (P3)
    │
    ▼
Polish (Phase 7)
```

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Implementation follows test requirements
- All tests must PASS before story is considered complete
- Story complete before moving to next priority

### Parallel Opportunities

**Phase 1**: T002 and T003 can run in parallel

**Phase 2**: All tasks sequential (modifying related code)

**Phase 3 (US1)**:
- T007, T008, T009, T010, T011, T012 can ALL run in parallel (different test cases)
- T013, T014, T015 sequential (same file)

**Phase 4 (US2)**:
- T019, T020, T021, T022, T023 can ALL run in parallel (different test cases)
- T024, T025, T026, T027 sequential (same file)
- T028 can run in parallel with others

**Phase 5 (US4)**:
- T030, T031, T032, T033 can ALL run in parallel (different test cases)
- T034, T035, T036 sequential (same file)

**Phase 6 (US3)**:
- T038, T039, T040, T041 can ALL run in parallel (different test cases)
- T042, T043, T044 sequential (same file)

**Phase 7**:
- T047, T048, T049 can ALL run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together (TDD):
Task: T007 "Unit tests for createGenericNodeType factory - valid ranges"
Task: T008 "Unit tests for factory invalid inputs"
Task: T009 "Unit tests for default naming pattern"
Task: T010 "Unit tests for port template generation"
Task: T011 "Unit tests for getAllGenericNodeTypes"
Task: T012 "Unit tests for getCommonGenericNodeTypes"

# After tests exist and fail, implement sequentially:
Task: T013 "Implement createGenericNodeType factory function"
Task: T014 "Implement getAllGenericNodeTypes with lazy caching"
Task: T015 "Implement getCommonGenericNodeTypes"
Task: T016 "Add generic nodes to Main.kt sample types"
Task: T017 "Integration test for palette display"
Task: T018 "Verify all tests pass"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test generic nodes can be dragged from palette
5. Demo/verify if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → **MVP Complete!**
3. Add User Story 2 → Test independently → Configurable nodes
4. Add User Story 4 → Test independently → Persistent nodes
5. Add User Story 3 → Test independently → Code generation works
6. Polish → Production ready

### Single Developer Flow

Follow phases sequentially:
1. Setup (T001-T003)
2. Foundational (T004-T006)
3. US1 (T007-T018)
4. US2 (T019-T029)
5. US4 (T030-T037)
6. US3 (T038-T046)
7. Polish (T047-T051)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently testable after completion
- TDD enforced: verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- All new .kt files must include Apache 2.0 header
