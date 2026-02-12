# Tasks: Module Save Architecture

**Feature Branch**: `010-module-save-architecture`
**Spec**: [spec.md](./spec.md)
**Approach**: TDD - Write tests first (marked [P]), verify they fail, then implement

---

## Phase 1: User Story 1 - Save Creates Module Structure (Priority: P1)

**Goal**: When user saves a FlowGraph, create complete KMP module directory structure

**Independent Test**: Save a FlowGraph and verify module directory with build.gradle.kts and source directories is created

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T001 [P] [US1] Create ModuleSaveServiceTest.kt with test for creating module directory on save in graphEditor/src/jvmTest/kotlin/save/ModuleSaveServiceTest.kt
- [X] T002 [P] [US1] Add test for generating build.gradle.kts with KMP configuration in graphEditor/src/jvmTest/kotlin/save/ModuleSaveServiceTest.kt
- [X] T003 [P] [US1] Add test for creating src/commonMain/kotlin/{package} directory structure in graphEditor/src/jvmTest/kotlin/save/ModuleSaveServiceTest.kt
- [X] T004 [P] [US1] Add test for generating settings.gradle.kts in graphEditor/src/jvmTest/kotlin/save/ModuleSaveServiceTest.kt
- [X] T005 [P] [US1] Add test for deriving module name from FlowGraph name in graphEditor/src/jvmTest/kotlin/save/ModuleSaveServiceTest.kt

### Implementation for User Story 1

- [X] T006 [US1] Create ModuleSaveService class with saveModule(flowGraph, outputDir) method in graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt
- [X] T007 [US1] Implement module directory creation with proper structure in graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt
- [X] T008 [US1] Implement build.gradle.kts generation (reuse/adapt from ModuleGenerator) in graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt
- [X] T009 [US1] Implement settings.gradle.kts generation in graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt
- [X] T010 [US1] Integrate ModuleSaveService into graphEditor Save action in graphEditor/src/jvmMain/kotlin/Main.kt

**Checkpoint**: User Story 1 complete - Save creates module directory structure

---

## Phase 2: User Story 2 - FlowGraph as Compiled Kotlin (.flow.kt) (Priority: P1)

**Goal**: Save FlowGraph as compiled Kotlin source file instead of script

**Independent Test**: Save FlowGraph, verify .flow.kt is valid Kotlin that compiles, and graphEditor can reopen it

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T011 [P] [US2] Create FlowKtGeneratorTest.kt with test for generating valid Kotlin syntax in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/FlowKtGeneratorTest.kt
- [X] T012 [P] [US2] Add test for generating package declaration in .flow.kt in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/FlowKtGeneratorTest.kt
- [X] T013 [P] [US2] Add test for generating codeNode DSL blocks with all properties in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/FlowKtGeneratorTest.kt
- [X] T014 [P] [US2] Add test for generating connection DSL statements in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/FlowKtGeneratorTest.kt
- [X] T015 [P] [US2] Add test for generating processingLogic<T>() references in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/FlowKtGeneratorTest.kt
- [X] T016 [P] [US2] Create FlowKtParserTest.kt with test for parsing .flow.kt back to FlowGraph in graphEditor/src/jvmTest/kotlin/serialization/FlowKtParserTest.kt
- [X] T017 [P] [US2] Add test for round-trip: FlowGraph → .flow.kt → FlowGraph equality in graphEditor/src/jvmTest/kotlin/serialization/FlowKtParserTest.kt

### Implementation for User Story 2

- [X] T018 [US2] Create FlowKtGenerator class with generateFlowKt(flowGraph, packageName) method in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowKtGenerator.kt
- [X] T019 [US2] Implement flowGraph DSL block generation with name, version, description in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowKtGenerator.kt
- [X] T020 [US2] Implement codeNode DSL block generation with position, ports, config in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowKtGenerator.kt
- [X] T021 [US2] Implement connection DSL statement generation in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowKtGenerator.kt
- [X] T022 [US2] Implement processingLogic<T>() reference generation in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowKtGenerator.kt
- [X] T023 [US2] Create FlowKtParser class with parseFlowKt(content) method in graphEditor/src/jvmMain/kotlin/serialization/FlowKtParser.kt
- [X] T024 [US2] Implement Kotlin source parsing to extract FlowGraph structure in graphEditor/src/jvmMain/kotlin/serialization/FlowKtParser.kt
- [X] T025 [US2] Integrate FlowKtGenerator into ModuleSaveService in graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt
- [X] T026 [US2] Update graphEditor Open to use FlowKtParser for .flow.kt files in graphEditor/src/jvmMain/kotlin/Main.kt

**Checkpoint**: User Story 2 complete - FlowGraph saved as compiled Kotlin

---

## Phase 3: User Story 3 - ProcessingLogic Stub Generation (Priority: P2)

**Goal**: Generate ProcessingLogic stub files for each CodeNode when saving module

**Independent Test**: Save FlowGraph with multiple nodes, verify stub files created with correct interface implementation

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T027 [P] [US3] Create ProcessingLogicStubGeneratorTest.kt with test for generating stub class name from node name in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGeneratorTest.kt
- [X] T028 [P] [US3] Add test for stub implementing ProcessingLogic interface in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGeneratorTest.kt
- [X] T029 [P] [US3] Add test for stub invoke() method with correct input parameter types in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGeneratorTest.kt
- [X] T030 [P] [US3] Add test for stub KDoc describing node type (GENERATOR/TRANSFORMER/SINK) in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGeneratorTest.kt
- [X] T031 [P] [US3] Add test for stub listing input/output ports in KDoc in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGeneratorTest.kt

### Implementation for User Story 3

- [X] T032 [US3] Create ProcessingLogicStubGenerator class with generateStub(codeNode, packageName) method in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGenerator.kt
- [X] T033 [US3] Implement class declaration with ProcessingLogic interface in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGenerator.kt
- [X] T034 [US3] Implement invoke() method stub with NotImplementedError in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGenerator.kt
- [X] T035 [US3] Implement KDoc generation with node type and port descriptions in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGenerator.kt
- [X] T036 [US3] Integrate ProcessingLogicStubGenerator into ModuleSaveService in graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt
- [X] T037 [US3] Generate one stub file per CodeNode during save in graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt

**Checkpoint**: User Story 3 complete - ProcessingLogic stubs generated on save

---

## Phase 4: User Story 4 - Compile Generates Factory Function (Priority: P2)

**Goal**: Compile generates factory function that instantiates FlowGraph with ProcessingLogic

**Independent Test**: After implementing stubs, compile and verify factory function creates valid FlowGraph

### Tests for User Story 4

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T038 [P] [US4] Add test for factory function instantiating ProcessingLogic from module in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGeneratorTest.kt
- [X] T039 [P] [US4] Add test for factory function using class references from .flow.kt in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGeneratorTest.kt
- [X] T040 [P] [US4] Add test for compile validation: all ProcessingLogic classes exist in kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGeneratorTest.kt

### Implementation for User Story 4

- [X] T041 [US4] Update FlowGraphFactoryGenerator to read processingLogic references from .flow.kt in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGenerator.kt
- [X] T042 [US4] Generate ProcessingLogic instantiation using class names from same package in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGenerator.kt
- [X] T043 [US4] Update compile validation to check ProcessingLogic classes exist in module in graphEditor/src/jvmMain/kotlin/compilation/CompilationValidator.kt
- [X] T044 [US4] Generate {GraphName}Factory.kt with createXXXFlowGraph() function in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ModuleGenerator.kt

**Checkpoint**: User Story 4 complete - Compile generates working factory function

---

## Phase 5: User Story 5 - Incremental Save (Priority: P3)

**Goal**: Re-saving existing module preserves user implementations, only adds new stubs

**Independent Test**: Modify existing FlowGraph, save, verify existing ProcessingLogic files unchanged

### Tests for User Story 5

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T045 [P] [US5] Add test for preserving existing ProcessingLogic files on re-save in graphEditor/src/jvmTest/kotlin/save/ModuleSaveServiceTest.kt
- [X] T046 [P] [US5] Add test for generating stubs only for NEW nodes in graphEditor/src/jvmTest/kotlin/save/ModuleSaveServiceTest.kt
- [X] T047 [P] [US5] Add test for warning when node removed (orphaned ProcessingLogic) in graphEditor/src/jvmTest/kotlin/save/ModuleSaveServiceTest.kt
- [X] T048 [P] [US5] Add test for updating .flow.kt while preserving structure in graphEditor/src/jvmTest/kotlin/save/ModuleSaveServiceTest.kt

### Implementation for User Story 5

- [X] T049 [US5] Implement detection of existing module at save location in graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt
- [X] T050 [US5] Implement diff logic: identify new, existing, and removed nodes in graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt
- [X] T051 [US5] Skip stub generation for nodes with existing ProcessingLogic files in graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt
- [X] T052 [US5] Generate warning for orphaned ProcessingLogic files in graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt
- [X] T053 [US5] Update .flow.kt without regenerating entire file in graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt

**Checkpoint**: User Story 5 complete - Incremental save preserves user work

---

## Phase 6: Migration & Integration

**Goal**: Migrate StopWatch demo to new architecture and verify end-to-end

### Migration Tasks

- [ ] T054 [MIG] Create StopWatch module using new save architecture in StopWatch/
- [ ] T055 [MIG] Convert demos/stopwatch/StopWatch.flow.kts to StopWatch/src/.../StopWatch.flow.kt
- [ ] T056 [MIG] Move TimerEmitterComponent.kt to StopWatch module source directory
- [ ] T057 [MIG] Move DisplayReceiverComponent.kt to StopWatch module source directory
- [ ] T058 [MIG] Run compile to generate factory function in StopWatch module
- [ ] T059 [MIG] Update KMPMobileApp to import createStopWatchFlowGraph from generated module
- [ ] T060 [MIG] Verify KMPMobileApp compiles and runs with stopwatch functioning

### Cleanup Tasks

- [ ] T061 [CLN] Remove FlowGraphDeserializer (no longer needed for .flow.kts parsing) in graphEditor/src/jvmMain/kotlin/serialization/
- [ ] T062 [CLN] Remove .flow.kts file support from graphEditor Open action
- [ ] T063 [CLN] Update graphEditor Save dialog to default to module save (not single file)
- [ ] T064 [CLN] Remove demos/stopwatch/ directory (migrated to StopWatch module)

**Checkpoint**: Migration complete - StopWatch uses new architecture

---

## Phase 7: Polish & Documentation

**Goal**: Cleanup, documentation, and final validation

- [ ] T065 [DOC] Add KDoc to ModuleSaveService in graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt
- [ ] T066 [DOC] Add KDoc to FlowKtGenerator in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowKtGenerator.kt
- [ ] T067 [DOC] Add KDoc to FlowKtParser in graphEditor/src/jvmMain/kotlin/serialization/FlowKtParser.kt
- [ ] T068 [DOC] Add KDoc to ProcessingLogicStubGenerator in kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGenerator.kt
- [ ] T069 [VAL] Run ./gradlew :graphEditor:test to verify all graphEditor tests pass
- [ ] T070 [VAL] Run ./gradlew :kotlinCompiler:test to verify all kotlinCompiler tests pass
- [ ] T071 [VAL] Run ./gradlew build to verify full project compilation
- [ ] T072 [VAL] Manual test: Create new FlowGraph, save, implement stubs, compile, run

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Module Structure) ─┬─► Phase 3 (Stub Generation) ─► Phase 4 (Factory) ─► Phase 5 (Incremental)
                            │
Phase 2 (.flow.kt Format) ──┘
                                                                                          │
                                                                                          ▼
                                                                               Phase 6 (Migration)
                                                                                          │
                                                                                          ▼
                                                                               Phase 7 (Polish)
```

### Task Dependencies Within Phases

- T006-T010 depend on T001-T005 tests existing
- T018-T026 depend on T011-T017 tests existing
- T032-T037 depend on T027-T031 tests existing
- T041-T044 depend on T038-T040 tests existing
- T049-T053 depend on T045-T048 tests existing
- T054-T060 depend on Phases 1-4 complete
- T061-T064 depend on T054-T060 migration verified

---

## Notes

### Key Architectural Decisions

1. **ModuleSaveService** is the orchestrator for save operations, coordinating:
   - Directory structure creation
   - .flow.kt generation (via FlowKtGenerator)
   - Stub generation (via ProcessingLogicStubGenerator)

2. **FlowKtGenerator** produces compiled Kotlin, not script - enables IDE tooling

3. **FlowKtParser** reads .flow.kt files by parsing Kotlin source - replaces FlowGraphDeserializer

4. **ProcessingLogicStubGenerator** creates minimal valid implementations users can fill in

5. **Incremental save** (Phase 5) is important for real-world usage but can be deferred

### Files Created by This Spec

**New files:**
- `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt`
- `graphEditor/src/jvmMain/kotlin/serialization/FlowKtParser.kt`
- `graphEditor/src/jvmTest/kotlin/save/ModuleSaveServiceTest.kt`
- `graphEditor/src/jvmTest/kotlin/serialization/FlowKtParserTest.kt`
- `kotlinCompiler/src/commonMain/kotlin/.../FlowKtGenerator.kt`
- `kotlinCompiler/src/commonMain/kotlin/.../ProcessingLogicStubGenerator.kt`
- `kotlinCompiler/src/commonTest/kotlin/.../FlowKtGeneratorTest.kt`
- `kotlinCompiler/src/commonTest/kotlin/.../ProcessingLogicStubGeneratorTest.kt`

**Modified files:**
- `graphEditor/src/jvmMain/kotlin/Main.kt` (Save/Open integration)
- `kotlinCompiler/src/commonMain/kotlin/.../FlowGraphFactoryGenerator.kt`
- `kotlinCompiler/src/commonMain/kotlin/.../ModuleGenerator.kt`
- `graphEditor/src/jvmMain/kotlin/compilation/CompilationValidator.kt`

**Removed files:**
- `graphEditor/src/jvmMain/kotlin/serialization/FlowGraphDeserializer.kt` (if exists)
- `demos/stopwatch/` directory (after migration)
