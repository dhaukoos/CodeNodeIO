# Tasks: Modify Source and Sink Nodes

**Input**: Design documents from `/specs/037-source-sink-refactor/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/naming-convention.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: No project initialization needed — existing multi-module KMP project.

*(No setup tasks — all infrastructure already exists)*

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Rename the CodeNodeType enum value from GENERATOR to SOURCE. This change causes compile errors everywhere GENERATOR is referenced, which drives the systematic updates in all subsequent phases.

**CRITICAL**: No user story work can begin until this is complete.

- [X] T001 Rename `GENERATOR` to `SOURCE` in `CodeNodeType` enum and update KDoc description in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt`

**Checkpoint**: Enum renamed — project will not compile until all references are updated in subsequent phases.

---

## Phase 3: User Story 1 — Rename Generator Terminology to Source (Priority: P1)

**Goal**: Replace all "generator" terminology with "source" for the base single-output source node: runtime class, type alias, factory method, UI labels, serialization, and all references across all three modules.

**Independent Test**: Search codebase for `GeneratorRuntime`, `ContinuousGeneratorBlock`, `createContinuousGenerator`, and `CodeNodeType.GENERATOR` — zero matches should remain. `./gradlew :fbpDsl:jvmTest :kotlinCompiler:jvmTest :graphEditor:jvmTest` passes.

### Implementation for User Story 1

- [X] T002 [US1] Rename `GeneratorRuntime.kt` → `SourceRuntime.kt`: create new file with class renamed to `SourceRuntime<T>`, update all internal references, delete old file in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/`
- [X] T003 [US1] Rename `ContinuousGeneratorBlock<T>` → `ContinuousSourceBlock<T>` type alias in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ContinuousTypes.kt`
- [X] T004 [US1] Rename `createContinuousGenerator<T>` → `createContinuousSource<T>` factory method and update all `CodeNodeType.GENERATOR` → `CodeNodeType.SOURCE` references in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt`
- [X] T005 [US1] Update `RuntimeTypeResolver.kt` to return `SourceRuntime` and `createContinuousSource` for single-output source nodes in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeTypeResolver.kt`
- [X] T006 [US1] Update `RuntimeFlowGenerator.kt` to use `SOURCE` terminology (variable names, comments, factory calls) in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeFlowGenerator.kt`
- [X] T007 [P] [US1] Update `FlowKtGenerator.kt` to serialize `"SOURCE"` instead of `"GENERATOR"` in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowKtGenerator.kt`
- [X] T008 [P] [US1] Update `FlowGraphSerializer.kt` to deserialize both `"GENERATOR"` (legacy) and `"SOURCE"` (new) to `CodeNodeType.SOURCE` in `graphEditor/src/jvmMain/kotlin/serialization/FlowGraphSerializer.kt`
- [X] T009 [P] [US1] Update `NodeRenderer.kt` to use `CodeNodeType.SOURCE` in color mapping and labels in `graphEditor/src/jvmMain/kotlin/rendering/NodeRenderer.kt`
- [X] T010 [P] [US1] Update `DragAndDropHandler.kt` and `Main.kt` to use `CodeNodeType.SOURCE` in `graphEditor/src/jvmMain/kotlin/ui/DragAndDropHandler.kt` and `graphEditor/src/jvmMain/kotlin/Main.kt`
- [X] T011 [P] [US1] Rename `GeneratorTemplate.kt` → `SourceTemplate.kt` and update class/references in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/templates/`
- [X] T012 [P] [US1] Update `ComponentGenerator.kt` to use SOURCE references in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ComponentGenerator.kt`
- [X] T013 [US1] Update all fbpDsl test files to use `SOURCE`/`SourceRuntime` naming: `ContinuousFactoryTest.kt`, `PauseResumeTest.kt`, `IndependentControlTest.kt`, `NodeRuntimeTest.kt`, `RuntimeRegistryTest.kt`, `RuntimeRegistrationTest.kt`, `StopWatchFlowGraphTest.kt`, `StopWatchSerializationTest.kt` in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/`
- [X] T014 [US1] Update all kotlinCompiler test files to use `SOURCE` naming: `RuntimeTypeResolverTest.kt`, `RuntimeFlowGeneratorTest.kt`, `ProcessingLogicStubGeneratorTest.kt`, `RuntimeViewModelGeneratorTest.kt`, `RuntimeControllerGeneratorTest.kt`, `RuntimeControllerInterfaceGeneratorTest.kt`, `RuntimeControllerAdapterGeneratorTest.kt`, `FlowKtGeneratorTest.kt`, `ConnectionWiringResolverTest.kt`, `ObservableStateResolverTest.kt`, `FlowGraphFactoryGeneratorTest.kt`, `ChannelCapacityTest.kt`, `ViewModelGeneratorTest.kt`, `StopWatchModuleGeneratorTest.kt`, `PropertyCodeGenTest.kt` in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/`
- [X] T015 [US1] Update graphEditor test files to use `SOURCE` naming: `ModuleSaveServiceTest.kt`, `RequiredPropertyValidatorTest.kt`, `CompilationValidatorTest.kt` in `graphEditor/src/jvmTest/kotlin/`
- [X] T016 [US1] Compile and run all tests: `./gradlew :fbpDsl:jvmTest :kotlinCompiler:jvmTest :graphEditor:jvmTest`

**Checkpoint**: All "Generator" references for the base single-output source node are renamed. Project compiles and tests pass.

---

## Phase 4: User Story 2 — Source Nodes Emit via ViewModel Binding (Priority: P1)

**Goal**: Remove the timed tick loop pattern from source nodes. Source nodes no longer have `createTimedGenerator`/`createTimedSource` factory methods, tick type aliases, or processing logic stubs. Data enters the flow graph via ViewModel binding to UI functions.

**Independent Test**: Generated source node runtime code in `RuntimeFlowGenerator` output contains no `tickIntervalMs`, no `tick =` parameter, and no while loop. `ProcessingLogicStubGenerator.shouldGenerateStub()` returns `false` for SOURCE nodes. All tests pass.

### Implementation for User Story 2

- [X] T017 [US2] Remove timed source factory methods (`createTimedGenerator`, `createTimedOut2Generator`, `createTimedOut3Generator`) from `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt`
- [X] T018 [US2] Remove source tick type aliases (`GeneratorTickBlock`, `Out2TickBlock`, `Out3TickBlock`) from `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ContinuousTypes.kt`
- [X] T019 [US2] Update `ProcessingLogicStubGenerator.shouldGenerateStub()` to return `false` for `CodeNodeType.SOURCE` nodes in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGenerator.kt`
- [X] T020 [US2] Update `RuntimeFlowGenerator.kt` to not use tick pattern for source nodes — no `tickIntervalMs`, no `tick = ...Tick` parameter, use `createContinuousSource` with ViewModel-driven generate block in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeFlowGenerator.kt`
- [X] T021 [US2] Update `RuntimeTypeResolver.kt` to remove timed source factory method mappings in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeTypeResolver.kt`
- [X] T022 [US2] Update `ProcessingLogicStubGenerator` tick type alias resolution to skip source node patterns (0 inputs) in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGenerator.kt`
- [X] T023 [US2] Update fbpDsl tests for timed source removal: `ContinuousFactoryTest.kt`, `TypedNodeRuntimeTest.kt` in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/`
- [X] T024 [US2] Update kotlinCompiler tests for source stub and tick removal: `ProcessingLogicStubGeneratorTest.kt`, `RuntimeFlowGeneratorTest.kt`, `RuntimeTypeResolverTest.kt`, `FlowGraphFactoryGeneratorTest.kt` in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/`
- [X] T025 [US2] Compile and run tests: `./gradlew :fbpDsl:jvmTest :kotlinCompiler:jvmTest`

**Checkpoint**: Source nodes are fully ViewModel-driven — no tick loops, no timed factories, no processing logic stubs. Tests pass.

---

## Phase 5: User Story 3 — Sink Nodes Have No Processing Logic Stub (Priority: P2)

**Goal**: Remove processing logic stub generation for sink nodes. Sink consume blocks in generated code only update observable state — no user tick function call. Timed sink factory methods and tick type aliases are removed. Orphan detection in ModuleSaveService cleans up existing sink stubs on re-save.

**Independent Test**: `ProcessingLogicStubGenerator.shouldGenerateStub()` returns `false` for SINK nodes. Generated sink consume blocks contain only state updates (no `...Tick(...)` call). Saving a module with sinks produces no stub files for sink nodes.

### Implementation for User Story 3

- [X] T026 [US3] Update `ProcessingLogicStubGenerator.shouldGenerateStub()` to return `false` for `CodeNodeType.SINK` nodes in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGenerator.kt`
- [X] T027 [US3] Remove tick call from sink consume blocks in `RuntimeFlowGenerator.kt` — keep only `{FlowName}State._prop.value = value` updates in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeFlowGenerator.kt`
- [X] T028 [US3] Remove timed sink factory methods (`createTimedSink`, `createTimedIn2Sink`, `createTimedIn3Sink`) from `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt`
- [X] T029 [US3] Remove sink tick type aliases (`SinkTickBlock`, `In2SinkTickBlock`, `In3SinkTickBlock`, `In2AnySinkTickBlock`, `In3AnySinkTickBlock`) from `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ContinuousTypes.kt`
- [X] T030 [US3] Update orphan detection in `ModuleSaveService.kt` to delete source and sink processing logic stub files on re-save in `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt`
- [X] T031 [US3] Update tests for sink stub removal: `ProcessingLogicStubGeneratorTest.kt`, `RuntimeFlowGeneratorTest.kt`, `ModuleSaveServiceTest.kt` in `kotlinCompiler/src/commonTest/` and `graphEditor/src/jvmTest/`
- [X] T032 [US3] Compile and run tests: `./gradlew :fbpDsl:jvmTest :kotlinCompiler:jvmTest :graphEditor:jvmTest`

**Checkpoint**: Sink nodes are pure state bridges — no stubs, no tick calls, no timed factories. Tests pass.

---

## Phase 6: User Story 4 — Rename Multi-Output Source Runtime Classes (Priority: P2)

**Goal**: Rename `Out2GeneratorRuntime` → `SourceOut2Runtime` and `Out3GeneratorRuntime` → `SourceOut3Runtime`, along with their factory methods and type aliases, following the `{Role}{Direction}{Count}` naming convention.

**Independent Test**: No references to `Out2GeneratorRuntime`, `Out3GeneratorRuntime`, `createOut2Generator`, `createOut3Generator`, `Out2GeneratorBlock`, or `Out3GeneratorBlock` remain. Tests pass.

### Implementation for User Story 4

- [X] T033 [P] [US4] Rename `Out2GeneratorRuntime.kt` → `SourceOut2Runtime.kt`: create new file with class renamed to `SourceOut2Runtime`, delete old file in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/`
- [X] T034 [P] [US4] Rename `Out3GeneratorRuntime.kt` → `SourceOut3Runtime.kt`: create new file with class renamed to `SourceOut3Runtime`, delete old file in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/`
- [X] T035 [US4] Rename multi-output source factory methods (`createOut2Generator` → `createSourceOut2`, `createOut3Generator` → `createSourceOut3`) in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt`
- [X] T036 [US4] Rename multi-output source type aliases (`Out2GeneratorBlock` → `SourceOut2Block`, `Out3GeneratorBlock` → `SourceOut3Block`) in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ContinuousTypes.kt`
- [X] T037 [US4] Update `RuntimeTypeResolver.kt` to return `SourceOut2Runtime`/`SourceOut3Runtime` and `createSourceOut2`/`createSourceOut3` for multi-output source nodes in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeTypeResolver.kt`
- [X] T038 [US4] Update `FlowGraphFactoryGenerator.kt` for multi-output source naming in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGenerator.kt`
- [X] T039 [US4] Update tests for multi-output source renaming: `TypedNodeRuntimeTest.kt` in `fbpDsl/src/commonTest/`, `RuntimeTypeResolverTest.kt`, `RuntimeFlowGeneratorTest.kt`, `FlowGraphFactoryGeneratorTest.kt` in `kotlinCompiler/src/commonTest/`
- [X] T040 [US4] Compile and run tests: `./gradlew :fbpDsl:jvmTest :kotlinCompiler:jvmTest`

**Checkpoint**: Multi-output source classes follow `SourceOut{N}Runtime` naming convention. Tests pass.

---

## Phase 7: User Story 5 — Rename Multi-Input Sink Runtime Classes (Priority: P2)

**Goal**: Rename `In2SinkRuntime` → `SinkIn2Runtime`, `In3SinkRuntime` → `SinkIn3Runtime`, `In2AnySinkRuntime` → `SinkIn2AnyRuntime`, `In3AnySinkRuntime` → `SinkIn3AnyRuntime`, along with their factory methods and type aliases.

**Independent Test**: No references to `In2SinkRuntime`, `In3SinkRuntime`, `In2AnySinkRuntime`, `In3AnySinkRuntime`, `createIn2Sink`, `createIn3Sink`, `createIn2AnySink`, `createIn3AnySink`, `In2SinkBlock`, `In3SinkBlock` remain. Tests pass.

### Implementation for User Story 5

- [X] T041 [P] [US5] Rename `In2SinkRuntime.kt` → `SinkIn2Runtime.kt`: create new file with class renamed to `SinkIn2Runtime`, delete old file in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/`
- [X] T042 [P] [US5] Rename `In3SinkRuntime.kt` → `SinkIn3Runtime.kt`: create new file with class renamed to `SinkIn3Runtime`, delete old file in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/`
- [X] T043 [P] [US5] Rename `In2AnySinkRuntime.kt` → `SinkIn2AnyRuntime.kt`: create new file with class renamed to `SinkIn2AnyRuntime`, delete old file in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/`
- [X] T044 [P] [US5] Rename `In3AnySinkRuntime.kt` → `SinkIn3AnyRuntime.kt`: create new file with class renamed to `SinkIn3AnyRuntime`, delete old file in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/`
- [X] T045 [US5] Rename multi-input sink factory methods (`createIn2Sink` → `createSinkIn2`, `createIn3Sink` → `createSinkIn3`, `createIn2AnySink` → `createSinkIn2Any`, `createIn3AnySink` → `createSinkIn3Any`) in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt`
- [X] T046 [US5] Rename multi-input sink type aliases (`In2SinkBlock` → `SinkIn2Block`, `In3SinkBlock` → `SinkIn3Block`, `In2AnySinkBlock` → `SinkIn2AnyBlock`, `In3AnySinkBlock` → `SinkIn3AnyBlock`) in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ContinuousTypes.kt`
- [X] T047 [US5] Update `RuntimeTypeResolver.kt` to return `SinkIn2Runtime`/`SinkIn3Runtime`/`SinkIn2AnyRuntime`/`SinkIn3AnyRuntime` and `createSinkIn2`/`createSinkIn3`/`createSinkIn2Any`/`createSinkIn3Any` in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeTypeResolver.kt`
- [X] T048 [US5] Update `FlowGraphFactoryGenerator.kt` for multi-input sink naming in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGenerator.kt`
- [X] T049 [US5] Update tests for multi-input sink renaming: `TypedNodeRuntimeTest.kt`, `AnyInputRuntimeTest.kt`, `PauseResumeTest.kt` in `fbpDsl/src/commonTest/`, `RuntimeTypeResolverTest.kt`, `RuntimeFlowGeneratorTest.kt`, `FlowGraphFactoryGeneratorTest.kt` in `kotlinCompiler/src/commonTest/`
- [X] T050 [US5] Compile and run tests: `./gradlew :fbpDsl:jvmTest :kotlinCompiler:jvmTest`

**Checkpoint**: Multi-input sink classes follow `SinkIn{N}Runtime` naming convention. Tests pass.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Update existing generated modules and perform final verification across the entire project.

- [X] T051 [P] Update StopWatch module: rename `GeneratorTickBlock`/`SinkTickBlock` references in processing logic stubs in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/processingLogic/`
- [X] T052 [P] Update StopWatchV2 module: rename `GeneratorTickBlock`/`SinkTickBlock` references in processing logic stubs in `StopWatchV2/src/commonMain/kotlin/io/codenode/stopwatchv2/processingLogic/`
- [X] T053 [P] Update UserProfiles module: rename tick block and factory references in processing logic stubs in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/processingLogic/`
- [X] T054 [P] Update RepositoryPattern module: rename tick block and factory references in processing logic stubs in `RepositoryPattern/src/commonMain/kotlin/io/codenode/repositorypattern/processingLogic/`
- [X] T055 [P] Update StopWatchOriginal components: rename `GeneratorRuntime`/`SinkRuntime` references in `StopWatchOriginal/src/commonMain/kotlin/io/codenode/stopwatch/usecases/`
- [X] T056 [P] Update KMPMobileApp integration test: rename `CodeNodeType.GENERATOR` references in `KMPMobileApp/src/androidUnitTest/kotlin/io/codenode/mobileapp/StopWatchIntegrationTest.kt`
- [X] T057 [P] Update RegenerateStopWatch tool: rename references in `kotlinCompiler/src/jvmMain/kotlin/io/codenode/kotlincompiler/tools/RegenerateStopWatch.kt`
- [X] T058 Full build verification: `./gradlew build`
- [X] T059 Zero-reference check: search active source code for `GeneratorRuntime`, `GENERATOR`, `createTimedGenerator`, `Out2GeneratorRuntime`, `In2SinkRuntime`, `createIn2Sink` — confirm zero matches outside specs/ documentation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 2)**: No dependencies — start immediately. BLOCKS all user stories.
- **US1 (Phase 3)**: Depends on Foundational (T001). Must complete before US2 and US4.
- **US2 (Phase 4)**: Depends on US1 (uses renamed SOURCE names). Can run in parallel with US3.
- **US3 (Phase 5)**: Depends on Foundational (T001). Can run in parallel with US2.
- **US4 (Phase 6)**: Depends on US1 (base source rename must be done first). Can run in parallel with US5.
- **US5 (Phase 7)**: Depends on Foundational (T001). Can run in parallel with US4.
- **Polish (Phase 8)**: Depends on all user stories being complete.

### User Story Dependencies

```
T001 (Foundational)
  ├── US1 (P1: Rename Generator → Source)
  │     ├── US2 (P1: Remove tick loops for sources)
  │     └── US4 (P2: Rename multi-output sources)
  ├── US3 (P2: Remove sink stubs) — independent of US1
  └── US5 (P2: Rename multi-input sinks) — independent of US1
```

### Within Each User Story

- Runtime class file renames before factory/type alias updates
- Core module (fbpDsl) before code generator module (kotlinCompiler)
- Code generators before graph editor
- Implementation before tests
- Tests before compile checkpoint

### Parallel Opportunities

- **Within US1**: T007, T008, T009, T010, T011, T012 can run in parallel (different files)
- **Across stories**: US2 and US3 can run in parallel; US4 and US5 can run in parallel
- **Within US4**: T033 and T034 can run in parallel (different files)
- **Within US5**: T041, T042, T043, T044 can run in parallel (different files)
- **Polish**: T051–T057 can all run in parallel (different modules)

---

## Parallel Example: User Story 5

```bash
# Launch all runtime class file renames in parallel:
Task: "Rename In2SinkRuntime.kt → SinkIn2Runtime.kt"
Task: "Rename In3SinkRuntime.kt → SinkIn3Runtime.kt"
Task: "Rename In2AnySinkRuntime.kt → SinkIn2AnyRuntime.kt"
Task: "Rename In3AnySinkRuntime.kt → SinkIn3AnyRuntime.kt"

# Then sequentially: factory methods → type aliases → code generators → tests → compile
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational (T001)
2. Complete Phase 3: User Story 1 (T002–T016)
3. **STOP and VALIDATE**: All "Generator" base references renamed to "Source", project compiles, tests pass
4. This alone delivers the core terminology change

### Incremental Delivery

1. T001 → Foundation ready
2. US1 (T002–T016) → Base rename complete, project functional
3. US2 (T017–T025) → Source nodes are ViewModel-driven, no tick loops
4. US3 (T026–T032) → Sink nodes are pure state bridges, no stubs
5. US4 (T033–T040) → Multi-output sources use consistent naming
6. US5 (T041–T050) → Multi-input sinks use consistent naming
7. Polish (T051–T059) → Existing modules updated, full build verified

Each increment leaves the project in a compilable, testable state.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- File renames (T002, T033, T034, T041–T044) involve creating new file + deleting old file (not git mv, since class names change too)
- Backward compatibility for `.flow.kt` deserialization (T008) must handle both "GENERATOR" and "SOURCE"
- Specs/ documentation files are historical records and should NOT be updated
- Existing generated module stubs (T051–T057) reference old type aliases; update imports and type references
