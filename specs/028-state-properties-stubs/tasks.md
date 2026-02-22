# Tasks: State Properties Stubs

**Input**: Design documents from `/specs/028-state-properties-stubs/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- **kotlinCompiler generators**: `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/`
- **kotlinCompiler tests**: `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/`
- **graphEditor service**: `graphEditor/src/jvmMain/kotlin/save/`
- **graphEditor tests**: `graphEditor/src/jvmTest/kotlin/save/`

---

## Phase 1: Setup

**Purpose**: No setup tasks needed — all infrastructure already exists. The kotlinCompiler and graphEditor modules are established with existing generator patterns.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Create the new StatePropertiesGenerator class that all user stories depend on.

- [X] T001 Create StatePropertiesGenerator with `shouldGenerate()` and `getStatePropertiesFileName()` methods in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/StatePropertiesGenerator.kt`
- [X] T002 Add `getStatePropertiesObjectName()` method to StatePropertiesGenerator in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/StatePropertiesGenerator.kt`
- [X] T003 Add `generateStateProperties(codeNode, packageName)` method to StatePropertiesGenerator that generates the full Kotlin object source with MutableStateFlow/StateFlow pairs for all ports, `internal` visibility for mutable properties, public StateFlow accessors, type-appropriate defaults per R5, and a `reset()` method in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/StatePropertiesGenerator.kt`
- [X] T004 Create StatePropertiesGeneratorTest with tests for: `shouldGenerate()` returns true for nodes with ports and false for portless nodes; `getStatePropertiesFileName()` returns correct filename; `getStatePropertiesObjectName()` returns correct object name; `generateStateProperties()` generates correct output for a 2-output generator node, a 2-input sink node, a transformer node, and a node with non-primitive types (TODO default) in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/StatePropertiesGeneratorTest.kt`

**Checkpoint**: StatePropertiesGenerator is complete and tested — user story implementation can now begin

---

## Phase 3: User Story 1 - State Property Stubs Generated per Node (Priority: P1) MVP

**Goal**: When compiling a FlowGraph, generate a `stateProperties/{NodeName}StateProperties.kt` file for each node that has ports.

**Independent Test**: Compile a FlowGraph with a 2-output generator node, verify a state properties file is generated with matching MutableStateFlow properties.

### Implementation for User Story 1

- [X] T005 [US1] Add `STATE_PROPERTIES_SUBPACKAGE = "stateProperties"` constant and create `generateStatePropertiesFiles()` method in ModuleSaveService that generates state property files (with don't-overwrite semantics matching `generateProcessingLogicStubs()`) in `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt`
- [X] T006 [US1] Add `stateProperties` directory creation in `compileModule()` by calling `createDirectoryStructure()` for the stateProperties package, and call `generateStatePropertiesFiles()` from `compileModule()` in `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt`
- [X] T007 [US1] Add integration tests for state properties file generation in ModuleSaveServiceTest: verify stateProperties directory is created, verify correct files are generated for a StopWatch-like FlowGraph (TimerEmitterStateProperties.kt, DisplayReceiverStateProperties.kt), verify no file is generated for portless nodes in `graphEditor/src/jvmTest/kotlin/save/ModuleSaveServiceTest.kt`

**Checkpoint**: Compiling a FlowGraph generates state properties files in stateProperties/ directory

---

## Phase 4: User Story 2 - Processing Logic Stubs Import State Properties (Priority: P1)

**Goal**: Processing logic stub files import their corresponding state properties object so tick functions can reference `_portName.value`.

**Independent Test**: Generate stubs for a 2-output generator node, verify the processing logic file imports the state properties object.

### Implementation for User Story 2

- [X] T008 [US2] Update `generateStub()` in ProcessingLogicStubGenerator to accept a `statePropertiesPackage` parameter and add an import statement for the corresponding `{NodeName}StateProperties` object in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGenerator.kt`
- [X] T009 [US2] Update `generateProcessingLogicStubs()` in ModuleSaveService to pass the statePropertiesPackage parameter when calling `stubGenerator.generateStub()` in `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt`
- [X] T010 [US2] Update ProcessingLogicStubGeneratorTest to verify generated stubs contain import statement for the corresponding state properties object for generator, sink, and transformer nodes in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGeneratorTest.kt`

**Checkpoint**: Processing logic stubs import their state properties object

---

## Phase 5: User Story 4 - Runtime Flow Imports State Properties (Priority: P1)

**Goal**: The generated Flow class delegates observable state from state properties objects instead of owning MutableStateFlow directly.

**Independent Test**: Verify the generated Flow class imports state property objects and delegates StateFlow properties from them.

### Implementation for User Story 4

- [X] T011 [US4] Update `generate()` in RuntimeFlowGenerator to accept a `statePropertiesPackage` parameter and add import statements for all state properties objects in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeFlowGenerator.kt`
- [X] T012 [US4] Update `generateObservableState()` in RuntimeFlowGenerator to delegate `StateFlow` properties from state properties objects (e.g., `val secondsFlow: StateFlow<Int> = DisplayReceiverStateProperties.secondsFlow`) instead of creating local MutableStateFlow in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeFlowGenerator.kt`
- [X] T013 [US4] Update `generateSinkConsumeBlock()` in RuntimeFlowGenerator to reference state properties objects for `_portName.value` updates (e.g., `DisplayReceiverStateProperties._seconds.value = seconds`) in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeFlowGenerator.kt`
- [X] T014 [US4] Update `generateResetMethod()` in RuntimeFlowGenerator to call `reset()` on each state properties object for all nodes (not just sink nodes) instead of directly resetting MutableStateFlow values in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeFlowGenerator.kt`
- [X] T015 [US4] Update `generateRuntimeFiles()` in ModuleSaveService to pass `statePropertiesPackage` when calling `runtimeFlowGenerator.generate()` in `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt`
- [X] T016 [US4] Update RuntimeFlowGeneratorTest to verify: state properties imports are generated, observable state is delegated from state properties objects, sink consume block references state properties objects, reset method calls state properties reset() for all nodes in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/RuntimeFlowGeneratorTest.kt`

**Checkpoint**: Flow class delegates all observable state from state properties objects

---

## Phase 6: User Story 3 - State Properties Preserved on Re-compile (Priority: P2)

**Goal**: Existing state property files are not overwritten on re-compile, preserving developer modifications.

**Independent Test**: Compile, modify a state properties file, re-compile, verify modifications are preserved.

### Implementation for User Story 3

- [X] T017 [US3] Add orphan detection for stateProperties directory: extend `detectOrphanedComponents()` or add `detectOrphanedStateProperties()` to scan `stateProperties/` for `*StateProperties.kt` files not matching current FlowGraph nodes, and call it from `compileModule()` in `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt`
- [X] T018 [US3] Add integration tests for re-compile preservation: verify existing state properties files are not overwritten on re-compile, verify new nodes get fresh state properties files, verify orphaned state properties files produce warnings in `graphEditor/src/jvmTest/kotlin/save/ModuleSaveServiceTest.kt`

**Checkpoint**: State properties files survive re-compilation; orphans are warned

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Verify end-to-end correctness and update any dependent generators

- [X] T019 Check if RuntimeControllerGenerator, RuntimeControllerInterfaceGenerator, RuntimeControllerAdapterGenerator, or RuntimeViewModelGenerator need updates for state properties delegation (they may reference Flow's StateFlow properties which changed from owned to delegated) in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/Runtime*Generator.kt`
- [X] T020 Run full test suite: `./gradlew :kotlinCompiler:allTests` and `./gradlew :graphEditor:jvmTest` to verify all existing tests still pass
- [X] T021 Run quickstart.md validation: compile a StopWatch4-like FlowGraph and verify all 7 checklist items from quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 2)**: No dependencies — can start immediately (T001-T004)
- **User Story 1 (Phase 3)**: Depends on Phase 2 (StatePropertiesGenerator) — T005-T007
- **User Story 2 (Phase 4)**: Depends on Phase 2 (StatePropertiesGenerator) — T008-T010
- **User Story 4 (Phase 5)**: Depends on Phase 2 (StatePropertiesGenerator) — T011-T016
- **User Story 3 (Phase 6)**: Depends on US1 (files must exist to test preservation) — T017-T018
- **Polish (Phase 7)**: Depends on all user stories — T019-T021

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Foundational only — generates state property files
- **User Story 2 (P1)**: Depends on Foundational only — modifies stub generator (different file from US1)
- **User Story 4 (P1)**: Depends on Foundational only — modifies flow generator (different file from US1/US2)
- **User Story 3 (P2)**: Depends on US1 — tests re-compile of files that US1 generates

### Within Each User Story

- Implementation tasks before test updates (tests validate the implementation)
- ModuleSaveService integration after generator changes (wires generator into compile pipeline)

### Parallel Opportunities

- **Phase 2**: T001-T003 are sequential (same file), T004 follows
- **Phases 3, 4, 5**: US1, US2, US4 can run in parallel after Phase 2 (they modify different files: ModuleSaveService, ProcessingLogicStubGenerator, RuntimeFlowGenerator respectively)
- **Phase 6**: US3 must follow US1

---

## Parallel Example: User Stories 1, 2, and 4

```bash
# After Phase 2 (Foundational) completes, launch US1, US2, US4 in parallel:

# US1: State property file generation (ModuleSaveService + tests)
Task: T005 "Add generateStatePropertiesFiles() to ModuleSaveService"
Task: T006 "Wire into compileModule()"
Task: T007 "Integration tests"

# US2: Processing logic stub imports (ProcessingLogicStubGenerator + tests)
Task: T008 "Update generateStub() with statePropertiesPackage parameter"
Task: T009 "Update ModuleSaveService to pass statePropertiesPackage"
Task: T010 "Update ProcessingLogicStubGeneratorTest"

# US4: Flow class delegation (RuntimeFlowGenerator + tests)
Task: T011 "Add statePropertiesPackage parameter and imports"
Task: T012 "Delegate observable state"
Task: T013 "Update sink consume block"
Task: T014 "Update reset method"
Task: T015 "Wire statePropertiesPackage in ModuleSaveService"
Task: T016 "Update RuntimeFlowGeneratorTest"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational (StatePropertiesGenerator)
2. Complete Phase 3: User Story 1 (file generation via ModuleSaveService)
3. **STOP and VALIDATE**: Compile a FlowGraph and verify stateProperties/ files exist
4. Proceed to US2 + US4 for full integration

### Incremental Delivery

1. Phase 2 (Foundational) → StatePropertiesGenerator class ready
2. US1 → State property files generated on compile (MVP!)
3. US2 → Processing logic stubs import state properties
4. US4 → Flow class delegates from state properties
5. US3 → Re-compile preservation + orphan detection
6. Polish → Full validation against quickstart.md

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US1, US2, US4 are all P1 priority and can proceed in parallel after foundational phase
- US3 (P2) depends on US1 completion for re-compile testing
- The key design decision from research.md R1: use `internal` (not `private`) visibility for MutableStateFlow properties to enable cross-file access from processing logic stubs
