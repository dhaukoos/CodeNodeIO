# Tasks: StopWatch Code Generation Refactor

**Input**: Design documents from `/specs/009-stopwatch-codegen-refactor/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Per constitution (TDD required), tests are included for new functionality.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

Based on plan.md structure:
- **graphEditor**: `graphEditor/src/jvmMain/kotlin/` and `graphEditor/src/jvmTest/kotlin/`
- **kotlinCompiler**: `kotlinCompiler/src/commonMain/kotlin/` and `kotlinCompiler/src/commonTest/kotlin/`
- **StopWatch module**: `StopWatch/src/commonMain/kotlin/`
- **KMPMobileApp**: `KMPMobileApp/src/commonMain/kotlin/`
- **demos**: `demos/stopwatch/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Extend existing enums and prepare property infrastructure

- [X] T001 Add `FILE_PATH` value to PropertyType enum in graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt
- [X] T002 Add `FILE_BROWSER` value to EditorType enum in graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt
- [X] T003 Add editorType mapping for `FILE_PATH -> FILE_BROWSER` in PropertyDefinition.editorType in graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Create validation data classes used by multiple user stories

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 Create PropertyValidationError data class in graphEditor/src/jvmMain/kotlin/compilation/CompilationValidator.kt
- [X] T005 [P] Create NodeValidationError data class with message property in graphEditor/src/jvmMain/kotlin/compilation/CompilationValidator.kt
- [X] T006 [P] Create CompilationValidationResult data class with success, nodeErrors, isValid, errorSummary in graphEditor/src/jvmMain/kotlin/compilation/CompilationValidator.kt
- [X] T007 Create RequiredPropertySpec data class with key, displayName, validator in graphEditor/src/jvmMain/kotlin/compilation/CompilationValidator.kt
- [X] T008 Define PROCESSING_LOGIC_SPEC constant for processingLogicFile validation in graphEditor/src/jvmMain/kotlin/compilation/CompilationValidator.kt

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Configure ProcessingLogic Reference in Properties Panel (Priority: P1) 🎯 MVP

**Goal**: Add file browser editor to PropertiesPanel for selecting ProcessingLogic implementation files

**Independent Test**: Open flow graph, select a CodeNode, verify "Processing Logic" field appears with browse button, select a file, verify path stored and persisted

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T009 [P] [US1] Create FileBrowserEditorTest.kt with test for rendering text field and browse button in graphEditor/src/jvmTest/kotlin/ui/FileBrowserEditorTest.kt
- [X] T010 [P] [US1] Add test for FileBrowserEditor displaying existing path value in graphEditor/src/jvmTest/kotlin/ui/FileBrowserEditorTest.kt
- [X] T011 [P] [US1] Add test for FileBrowserEditor error state display in graphEditor/src/jvmTest/kotlin/ui/FileBrowserEditorTest.kt
- [X] T012 [P] [US1] Add test for FileBrowserEditor onValueChange callback in graphEditor/src/jvmTest/kotlin/ui/FileBrowserEditorTest.kt

### Implementation for User Story 1

- [X] T013 [US1] Create FileBrowserEditor composable with text field, browse button, and error display in graphEditor/src/jvmMain/kotlin/ui/PropertyEditors.kt
- [X] T014 [US1] Implement showProcessingLogicFileDialog() function using JFileChooser in graphEditor/src/jvmMain/kotlin/ui/PropertyEditors.kt
- [X] T015 [US1] Add file filter for Kotlin files (*.kt) to JFileChooser in graphEditor/src/jvmMain/kotlin/ui/PropertyEditors.kt
- [X] T016 [US1] Implement relative path conversion from projectRoot in FileBrowserEditor in graphEditor/src/jvmMain/kotlin/ui/PropertyEditors.kt
- [X] T017 [US1] Add FILE_BROWSER case to PropertyEditorRow when clause in graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt
- [X] T018 [US1] Create PROCESSING_LOGIC_PROPERTY PropertyDefinition constant for CodeNodes in graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt
- [X] T019 [US1] Add processingLogicFile to codeNodePropertyDefinitions list in PropertiesPanel in graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt
- [X] T020 [US1] Verify processingLogicFile serialization in FlowGraphSerializer config() call (existing support) in graphEditor/src/jvmMain/kotlin/serialization/FlowGraphSerializer.kt
- [X] T021 [US1] Test save and reload of flow graph with processingLogicFile configuration

**Checkpoint**: User Story 1 complete - ProcessingLogic files can be selected and saved

---

## Phase 4: User Story 2 - Compile Validation for Required Properties (Priority: P1)

**Goal**: Validate all required properties before code generation proceeds

**Independent Test**: Create flow graph with missing processingLogicFile, trigger compile, verify error dialog shows specific nodes with missing properties

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T022 [P] [US2] Create CompilationValidatorTest.kt with test for valid graph passing validation in graphEditor/src/jvmTest/kotlin/compilation/CompilationValidatorTest.kt
- [ ] T023 [P] [US2] Add test for graph with missing processingLogicFile failing validation in graphEditor/src/jvmTest/kotlin/compilation/CompilationValidatorTest.kt
- [ ] T024 [P] [US2] Add test for multiple nodes with missing properties all reported in graphEditor/src/jvmTest/kotlin/compilation/CompilationValidatorTest.kt
- [ ] T025 [P] [US2] Add test for file existence validation (file not found error) in graphEditor/src/jvmTest/kotlin/compilation/CompilationValidatorTest.kt
- [ ] T026 [P] [US2] Add test for .kt file extension validation in graphEditor/src/jvmTest/kotlin/compilation/CompilationValidatorTest.kt

### Implementation for User Story 2

- [ ] T027 [US2] Create CompilationValidator class with validate(flowGraph, projectRoot) method in graphEditor/src/jvmMain/kotlin/compilation/CompilationValidator.kt
- [ ] T028 [US2] Implement iteration over all CodeNodes to check required properties in CompilationValidator in graphEditor/src/jvmMain/kotlin/compilation/CompilationValidator.kt
- [ ] T029 [US2] Implement processingLogicFile validation: not blank, ends with .kt, file exists in graphEditor/src/jvmMain/kotlin/compilation/CompilationValidator.kt
- [ ] T030 [US2] Aggregate NodeValidationError list into CompilationValidationResult in graphEditor/src/jvmMain/kotlin/compilation/CompilationValidator.kt
- [ ] T031 [US2] Create CompilationValidationDialog composable for displaying errors in graphEditor/src/jvmMain/kotlin/ui/CompilationValidationDialog.kt
- [ ] T032 [US2] Integrate CompilationValidator call before compile in CompilationService in graphEditor/src/jvmMain/kotlin/compilation/CompilationService.kt
- [ ] T033 [US2] Add validation error dialog state and display in Main.kt compile handler in graphEditor/src/jvmMain/kotlin/Main.kt
- [ ] T034 [US2] Test full compile flow with validation dialog appearing on missing properties

**Checkpoint**: User Story 2 complete - Compile validates required properties and shows errors

---

## Phase 5: User Story 3 - Generated FlowGraph Factory Function (Priority: P2)

**Goal**: Generate createXXXFlowGraph() factory function that creates FlowGraph with ProcessingLogic instances

**Independent Test**: Compile fully-configured flow graph, verify generated StopWatchFlow.kt contains createStopWatchFlowGraph() function that returns valid FlowGraph

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T035 [P] [US3] Create FlowGraphFactoryGeneratorTest.kt with test for generating factory function signature in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGeneratorTest.kt
- [ ] T036 [P] [US3] Add test for generated function creating CodeNodes with correct properties in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGeneratorTest.kt
- [ ] T037 [P] [US3] Add test for generated function including ProcessingLogic instantiation in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGeneratorTest.kt
- [ ] T038 [P] [US3] Add test for generated function creating all Port definitions in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGeneratorTest.kt
- [ ] T039 [P] [US3] Add test for generated function creating all Connection definitions in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGeneratorTest.kt

### Implementation for User Story 3

- [ ] T040 [US3] Create FlowGraphFactoryGenerator class in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGenerator.kt
- [ ] T041 [US3] Implement generateFactoryFunction(flowGraph, packageName) returning FunSpec in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGenerator.kt
- [ ] T042 [US3] Generate function name as create{GraphName}FlowGraph using KotlinPoet in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGenerator.kt
- [ ] T043 [US3] Generate CodeNode creation code with all properties (id, name, codeNodeType, position) in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGenerator.kt
- [ ] T044 [US3] Generate Port list creation for inputPorts and outputPorts in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGenerator.kt
- [ ] T045 [US3] Generate ProcessingLogic instantiation from processingLogicFile class name in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGenerator.kt
- [ ] T046 [US3] Implement resolveProcessingLogicClassName(filePath) helper function in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGenerator.kt
- [ ] T047 [US3] Generate Connection list creation with all connection properties in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGenerator.kt
- [ ] T048 [US3] Generate FlowGraph return statement with rootNodes, connections, targetPlatforms in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGenerator.kt
- [ ] T049 [US3] Generate import statements for ProcessingLogic classes in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGenerator.kt
- [ ] T050 [US3] Integrate FlowGraphFactoryGenerator into ModuleGenerator.generateFlowGraphClass() in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ModuleGenerator.kt
- [ ] T051 [US3] Add factory function to generated FileSpec in ModuleGenerator in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ModuleGenerator.kt

**Checkpoint**: User Story 3 complete - Code generator produces factory function

---

## Phase 6: User Story 4 - Migrate KMPMobileApp to Use Generated Code (Priority: P3)

**Goal**: Update StopWatch demo and KMPMobileApp to use generated factory function instead of manual FlowGraph construction

**Independent Test**: KMPMobileApp compiles and runs with stopwatch UI functioning identically using generated code

### Implementation for User Story 4

- [ ] T052 [US4] Update StopWatch.flow.kts to add processingLogicFile config for TimerEmitter node in demos/stopwatch/StopWatch.flow.kts
- [ ] T053 [US4] Update StopWatch.flow.kts to add processingLogicFile config for DisplayReceiver node in demos/stopwatch/StopWatch.flow.kts
- [ ] T054 [US4] Regenerate StopWatchFlow.kt by running compile from graphEditor or gradle task
- [ ] T055 [US4] Verify generated createStopWatchFlowGraph() function in StopWatch/src/commonMain/kotlin/io/codenode/generated/stopwatch/StopWatchFlow.kt
- [ ] T056 [US4] Remove manual createStopWatchFlowGraph() function from KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/App.kt
- [ ] T057 [US4] Add import for createStopWatchFlowGraph from io.codenode.generated.stopwatch in KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/App.kt
- [ ] T058 [US4] Update MainContent() to call imported createStopWatchFlowGraph() in KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/App.kt
- [ ] T059 [US4] Run ./gradlew :KMPMobileApp:build to verify compilation
- [ ] T060 [US4] Run KMPMobileApp and verify stopwatch UI displays elapsed time correctly

**Checkpoint**: User Story 4 complete - KMPMobileApp uses generated code

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Cleanup, documentation, and final validation

- [ ] T061 [P] Add KDoc documentation to FileBrowserEditor in graphEditor/src/jvmMain/kotlin/ui/PropertyEditors.kt
- [ ] T062 [P] Add KDoc documentation to CompilationValidator in graphEditor/src/jvmMain/kotlin/compilation/CompilationValidator.kt
- [ ] T063 [P] Add KDoc documentation to FlowGraphFactoryGenerator in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGenerator.kt
- [ ] T064 [P] Run ./gradlew :graphEditor:test to verify all graphEditor tests pass
- [ ] T065 [P] Run ./gradlew :kotlinCompiler:test to verify all kotlinCompiler tests pass
- [ ] T066 Run ./gradlew build to verify full project compilation
- [ ] T067 Validate quickstart.md scenarios work end-to-end

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup (T001-T003) - BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational (T004-T008)
- **User Story 2 (Phase 4)**: Depends on Foundational (T004-T008), can run parallel with US1
- **User Story 3 (Phase 5)**: Depends on User Story 2 (validation must work before generation)
- **User Story 4 (Phase 6)**: Depends on User Story 3 (need generated code to migrate)
- **Polish (Phase 7)**: Depends on all user stories complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational - No dependencies on US1 (parallel possible)
- **User Story 3 (P2)**: Depends on US2 (validation must pass before generation)
- **User Story 4 (P3)**: Depends on US3 (need factory function before migration)

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Data classes before services
- Core functionality before UI integration
- Story complete before moving to dependent story

### Parallel Opportunities

**Phase 1 (Setup)**:
- T001, T002, T003 are sequential (same file)

**Phase 2 (Foundational)**:
- T005, T006 can run in parallel (same file, different classes)

**Phase 3 (US1)**:
- T009, T010, T011, T012 tests can run in parallel
- T013-T019 are mostly sequential (building on each other)

**Phase 4 (US2)**:
- T022, T023, T024, T025, T026 tests can run in parallel
- T027-T030 sequential within CompilationValidator
- T031 parallel (different file)

**Phase 5 (US3)**:
- T035, T036, T037, T038, T039 tests can run in parallel
- T040-T049 sequential within FlowGraphFactoryGenerator
- T050, T051 sequential with generator

**Phase 6 (US4)**:
- T052, T053 can run in parallel
- T054-T060 mostly sequential

**Phase 7 (Polish)**:
- T061, T062, T063 can run in parallel (different files)
- T064, T065 can run in parallel (different modules)

---

## Parallel Example: User Story 1 Tests

```bash
# Launch all tests for User Story 1 together:
Task: "T009 Create FileBrowserEditorTest.kt with test for rendering"
Task: "T010 Add test for FileBrowserEditor displaying existing path"
Task: "T011 Add test for FileBrowserEditor error state display"
Task: "T012 Add test for FileBrowserEditor onValueChange callback"
```

## Parallel Example: User Story 2 Tests

```bash
# Launch all tests for User Story 2 together:
Task: "T022 Create CompilationValidatorTest.kt with test for valid graph"
Task: "T023 Add test for graph with missing processingLogicFile failing"
Task: "T024 Add test for multiple nodes with missing properties"
Task: "T025 Add test for file existence validation"
Task: "T026 Add test for .kt file extension validation"
```

---

## Implementation Strategy

### MVP First (User Stories 1 + 2)

1. Complete Phase 1: Setup (T001-T003)
2. Complete Phase 2: Foundational (T004-T008)
3. Complete Phase 3: User Story 1 (T009-T021)
4. Complete Phase 4: User Story 2 (T022-T034)
5. **STOP and VALIDATE**: Test file selection and compile validation independently
6. Deploy/demo if ready

### Full Feature Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test file browser independently
3. Add User Story 2 → Test validation independently
4. Add User Story 3 → Test code generation independently
5. Add User Story 4 → Test migration end-to-end
6. Polish → Documentation and final validation

### Parallel Team Strategy

With two developers:
1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (file browser)
   - Developer B: User Story 2 (validation)
3. After US1+US2 complete:
   - Developer A: User Story 3 (code generation)
   - Developer B: Write additional tests, docs
4. After US3 complete:
   - Developer A: User Story 4 (migration)
   - Developer B: Polish phase

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable (except US3→US4 dependency)
- Verify tests fail before implementing (TDD per constitution)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
