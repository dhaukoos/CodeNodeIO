# Tasks: Generate Runtime Files from FlowGraph Compilation

**Input**: Design documents from `/specs/027-generate-runtime-files/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Tests**: Unit tests included per constitution TDD requirement.

**Organization**: Tasks are grouped by user story. US1 (Flow) is foundational — US2-US5 build on its shared utilities. US3-US5 can run in parallel after US2.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **kotlinCompiler generators**: `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/`
- **kotlinCompiler tests**: `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/`
- **graphEditor save**: `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/save/`
- **graphEditor save tests**: `graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/save/`

---

## Phase 1: Setup

**Purpose**: Shared utility classes used across all 5 generators

- [ ] T001 Create `ObservableStateResolver` utility class in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ObservableStateResolver.kt` — extracts observable state properties from sink node input ports. Implements: `getObservableStateProperties(flowGraph): List<ObservableProperty>` with `ObservableProperty(name, typeName, sourceNodeName, sourcePortName, defaultValue)`. Handles port name disambiguation when multiple sinks have same port names (prefix with nodeName). Maps `dataType.simpleName` to Kotlin type; defaults to "Any". Default values: 0 for Int, 0L for Long, 0.0 for Double, false for Boolean, "" for String, null for Any.
- [ ] T002 Create `ConnectionWiringResolver` utility class in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ConnectionWiringResolver.kt` — resolves FlowGraph connections to channel property assignments. Implements: `getWiringStatements(flowGraph): List<WiringStatement>` with `WiringStatement(targetVarName, targetChannelProp, sourceVarName, sourceChannelProp)`. Channel naming: 1 input→`inputChannel`, 2+ inputs→`inputChannel1`/`inputChannel2`/`inputChannel3`; 1 output→`outputChannel`, 2+ outputs→`outputChannel1`/`outputChannel2`/`outputChannel3`.
- [ ] T003 Create `RuntimeTypeResolver` utility class in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeTypeResolver.kt` — maps `(inputPortCount, outputPortCount)` to CodeNodeFactory method names and runtime type names. Implements: `getFactoryMethodName(node): String` (e.g., "createTimedOut2Generator", "createIn2Sink"), `getRuntimeTypeName(node): String` (e.g., "Out2GeneratorRuntime<Int, Int>"), `getTickParamName(node): String` (e.g., "tick", "consume", "transform"). Uses port count mapping from research.md R4.

---

## Phase 2: Foundational (Unit Tests for Utilities)

**Purpose**: Verify shared utilities before using them in generators

**⚠️ CRITICAL**: All generators depend on these utilities being correct

- [ ] T004 [P] Write unit tests for `ObservableStateResolver` in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/ObservableStateResolverTest.kt` — test cases: (1) single sink with 2 Int input ports → 2 properties; (2) no sink nodes → empty list; (3) two sinks with duplicate port names → disambiguated with node name prefix; (4) port with unmapped type → defaults to "Any"; (5) sink with 1 input port → 1 property.
- [ ] T005 [P] Write unit tests for `ConnectionWiringResolver` in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/ConnectionWiringResolverTest.kt` — test cases: (1) 2 connections between generator and sink → 2 wiring statements; (2) no connections → empty list; (3) single connection with single-output generator → uses `outputChannel`; (4) single connection with single-input sink → uses `inputChannel`.
- [ ] T006 [P] Write unit tests for `RuntimeTypeResolver` in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/RuntimeTypeResolverTest.kt` — test cases: (1) 0-in/2-out → "createTimedOut2Generator", tick param "tick"; (2) 2-in/0-out → "createIn2Sink", tick param "consume"; (3) 1-in/1-out → "createContinuousTransformer", tick param "transform"; (4) 0-in/1-out → "createContinuousGenerator", tick param "tick"; (5) 1-in/0-out → "createContinuousSink", tick param "consume".

**Checkpoint**: Utilities pass all tests. Generator implementation can begin.

---

## Phase 3: User Story 1 - Generate Flow Orchestrator (Priority: P1) 🎯 MVP

**Goal**: Generate `{FlowName}Flow.kt` that directly creates runtime instances via CodeNodeFactory, wires connections, and owns MutableStateFlow properties for sink ports.

**Independent Test**: Compile any FlowGraph and verify the generated Flow file contains runtime instances, tick imports, connection wiring, MutableStateFlow properties, and start/stop/reset methods.

### Tests for User Story 1

- [ ] T007 [P] [US1] Write unit tests for `RuntimeFlowGenerator` in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/RuntimeFlowGeneratorTest.kt` — test cases: (1) StopWatch-like flow (1 Out2Generator + 1 In2Sink + 2 connections) → verify tick imports, runtime instances, MutableStateFlow properties for sink ports, wireConnections(), start/stop/reset; (2) no connections → wireConnections() has no wiring; (3) no sink nodes → no MutableStateFlow properties; (4) single-output generator + single-input sink + 1 connection → uses `outputChannel`/`inputChannel`.

### Implementation for User Story 1

- [ ] T008 [US1] Implement `RuntimeFlowGenerator` in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeFlowGenerator.kt` — `fun generate(flowGraph: FlowGraph, generatedPackage: String, usecasesPackage: String): String`. Uses ObservableStateResolver, ConnectionWiringResolver, RuntimeTypeResolver. Generates: package declaration, imports (CodeNodeFactory, tick vals from usecases.logicmethods, CoroutineScope, MutableStateFlow, StateFlow, RuntimeRegistry, ExecutionState), class with MutableStateFlow properties for sink ports, runtime instance properties via CodeNodeFactory.create*(), start(scope)/stop()/reset()/wireConnections() methods. Sink consume blocks wrap user tick + update StateFlows. Reference: `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchFlow.kt`.
- [ ] T009 [US1] Verify RuntimeFlowGenerator tests pass — run `./gradlew :kotlinCompiler:allTests` and fix any failures.

**Checkpoint**: `RuntimeFlowGenerator` produces correct Flow.kt content for any FlowGraph.

---

## Phase 4: User Story 2 - Generate Controller with Observable State (Priority: P2)

**Goal**: Generate `{FlowName}Controller.kt` that wraps the Flow with execution control and exposes observable state as StateFlow properties.

**Independent Test**: Verify generated Controller contains Flow instance, start/stop/pause/resume/reset methods delegating to RootControlNode/RuntimeRegistry, executionState StateFlow, and one StateFlow per sink input port reading from flow.*Flow properties.

### Tests for User Story 2

- [ ] T010 [P] [US2] Write unit tests for `RuntimeControllerGenerator` in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/RuntimeControllerGeneratorTest.kt` — test cases: (1) StopWatch-like flow → verify Flow instance, start/stop/pause/resume/reset methods, executionState StateFlow, seconds/minutes StateFlow properties delegated from flow; (2) no sink nodes → only executionState property; (3) verify bindToLifecycle() method present.

### Implementation for User Story 2

- [ ] T011 [US2] Implement `RuntimeControllerGenerator` in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeControllerGenerator.kt` — `fun generate(flowGraph: FlowGraph, generatedPackage: String, usecasesPackage: String): String`. Generates: class with FlowGraph constructor param, RuntimeRegistry, RootControlNode, {Name}Flow instance, CoroutineScope, wasRunningBeforePause flag, StateFlow properties (executionState + one per observable state delegated from flow.*Flow), start/stop/pause/resume/reset/getStatus/setNodeState/setNodeConfig/bindToLifecycle methods. Reference: `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchController.kt`.
- [ ] T012 [US2] Verify RuntimeControllerGenerator tests pass — run `./gradlew :kotlinCompiler:allTests` and fix any failures.

**Checkpoint**: `RuntimeControllerGenerator` produces correct Controller.kt content.

---

## Phase 5: User Story 3 - Generate Controller Interface (Priority: P3)

**Goal**: Generate `{FlowName}ControllerInterface.kt` declaring the same control methods and StateFlow properties as the Controller.

**Independent Test**: Verify generated interface declares all StateFlow properties and control methods matching the Controller.

### Tests for User Story 3

- [ ] T013 [P] [US3] Write unit tests for `RuntimeControllerInterfaceGenerator` in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/RuntimeControllerInterfaceGeneratorTest.kt` — test cases: (1) StopWatch-like flow → verify interface with seconds/minutes/executionState StateFlow properties and start/stop/reset/pause/resume methods returning FlowGraph; (2) no sink nodes → only executionState property.

### Implementation for User Story 3

- [ ] T014 [US3] Implement `RuntimeControllerInterfaceGenerator` in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeControllerInterfaceGenerator.kt` — `fun generate(flowGraph: FlowGraph, generatedPackage: String): String`. Generates: interface with StateFlow property declarations (executionState + observable state), and control method declarations (start, stop, reset, pause, resume returning FlowGraph). Reference: `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchControllerInterface.kt`.
- [ ] T015 [US3] Verify RuntimeControllerInterfaceGenerator tests pass — run `./gradlew :kotlinCompiler:allTests` and fix any failures.

**Checkpoint**: `RuntimeControllerInterfaceGenerator` produces correct Interface.kt content.

---

## Phase 6: User Story 4 - Generate Controller Adapter (Priority: P4)

**Goal**: Generate `{FlowName}ControllerAdapter.kt` wrapping the Controller and implementing the Interface.

**Independent Test**: Verify generated adapter takes Controller as constructor param, implements Interface, and delegates all properties and methods.

### Tests for User Story 4

- [ ] T016 [P] [US4] Write unit tests for `RuntimeControllerAdapterGenerator` in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/RuntimeControllerAdapterGeneratorTest.kt` — test cases: (1) StopWatch-like flow → verify adapter implements interface, takes controller constructor param, delegates seconds/minutes/executionState and all control methods; (2) no sink nodes → only executionState delegation.

### Implementation for User Story 4

- [ ] T017 [US4] Implement `RuntimeControllerAdapterGenerator` in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeControllerAdapterGenerator.kt` — `fun generate(flowGraph: FlowGraph, generatedPackage: String): String`. Generates: class with Controller constructor param, implements ControllerInterface, overrides all StateFlow properties and control methods by delegating to controller. Reference: `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchControllerAdapter.kt`.
- [ ] T018 [US4] Verify RuntimeControllerAdapterGenerator tests pass — run `./gradlew :kotlinCompiler:allTests` and fix any failures.

**Checkpoint**: `RuntimeControllerAdapterGenerator` produces correct Adapter.kt content.

---

## Phase 7: User Story 5 - Generate ViewModel (Priority: P5)

**Goal**: Generate `{FlowName}ViewModel.kt` extending ViewModel, delegating to Controller Interface.

**Independent Test**: Verify generated ViewModel extends androidx.lifecycle.ViewModel, takes ControllerInterface constructor param, and delegates all properties and methods.

### Tests for User Story 5

- [ ] T019 [P] [US5] Write unit tests for `RuntimeViewModelGenerator` in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/RuntimeViewModelGeneratorTest.kt` — test cases: (1) StopWatch-like flow → verify ViewModel extends ViewModel, takes ControllerInterface param, delegates seconds/minutes/executionState and all control methods; (2) no sink nodes → only executionState delegation.

### Implementation for User Story 5

- [ ] T020 [US5] Implement `RuntimeViewModelGenerator` in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeViewModelGenerator.kt` — `fun generate(flowGraph: FlowGraph, generatedPackage: String): String`. Generates: class extending ViewModel with ControllerInterface constructor param, delegates all StateFlow properties and control methods. Reference: `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchViewModel.kt`.
- [ ] T021 [US5] Verify RuntimeViewModelGenerator tests pass — run `./gradlew :kotlinCompiler:allTests` and fix any failures.

**Checkpoint**: All 5 generators produce correct output individually.

---

## Phase 8: Integration & Polish

**Purpose**: Wire generators into ModuleSaveService and verify end-to-end

- [ ] T022 Integrate all 5 generators into `ModuleSaveService` in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/save/ModuleSaveService.kt` — add private fields for each generator, add `generateRuntimeFiles()` private method called after `.flow.kt` generation, write all 5 files to `generated/` package directory (always overwrite), add filenames to `filesCreated` list.
- [ ] T023 Write integration tests in `graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/save/ModuleSaveServiceTest.kt` — add test cases: (1) saveModule creates all 5 runtime files in generated/ directory; (2) re-save overwrites existing runtime files; (3) filesCreated list includes all 5 runtime file paths.
- [ ] T024 Run full build and verify — `./gradlew :kotlinCompiler:allTests :graphEditor:compileKotlinJvm :graphEditor:jvmTest` — fix any compilation or test failures across both modules.

**Checkpoint**: End-to-end: compiling a FlowGraph from graphEditor produces all 5 runtime files.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — T001, T002, T003 can run in parallel
- **Phase 2 (Foundational Tests)**: Depends on Phase 1 — T004, T005, T006 can run in parallel
- **Phase 3 (US1 Flow)**: Depends on Phase 2
- **Phase 4 (US2 Controller)**: Depends on Phase 3 (uses ObservableStateResolver from US1 setup)
- **Phase 5 (US3 Interface)**: Depends on Phase 2 only (uses ObservableStateResolver but not Flow/Controller generators)
- **Phase 6 (US4 Adapter)**: Depends on Phase 2 only
- **Phase 7 (US5 ViewModel)**: Depends on Phase 2 only
- **Phase 8 (Integration)**: Depends on ALL user stories (Phases 3-7)

### User Story Dependencies

- **US1 (Flow)**: Depends on Setup + Foundational
- **US2 (Controller)**: Depends on US1 (generates code referencing {Name}Flow class)
- **US3 (Interface)**: Can start after Foundational — independent of US1/US2
- **US4 (Adapter)**: Can start after Foundational — independent of US1/US2 (just references class names)
- **US5 (ViewModel)**: Can start after Foundational — independent of US1/US2

### Parallel Opportunities

After Phase 2:
- US3 (Interface), US4 (Adapter), US5 (ViewModel) can run in parallel
- US1 (Flow) can run in parallel with US3/US4/US5
- US2 (Controller) must wait for US1

---

## Parallel Example: After Phase 2

```bash
# These can all run in parallel after Phase 2:
Task: T007-T009 (US1 Flow generator)
Task: T013-T015 (US3 Interface generator)
Task: T016-T018 (US4 Adapter generator)
Task: T019-T021 (US5 ViewModel generator)

# Then sequentially:
Task: T010-T012 (US2 Controller generator - depends on US1)

# Finally:
Task: T022-T024 (Integration)
```

---

## Implementation Strategy

### MVP First (US1 Flow Only)

1. Complete Phase 1: Setup (T001-T003)
2. Complete Phase 2: Foundational Tests (T004-T006)
3. Complete Phase 3: US1 Flow (T007-T009)
4. **STOP and VALIDATE**: Flow generator produces correct output
5. Can integrate just the Flow generator into ModuleSaveService for early feedback

### Incremental Delivery

1. Setup + Foundational → Utilities ready
2. US1 (Flow) → Core runtime wiring works
3. US2 (Controller) → Execution control works
4. US3-US5 (Interface, Adapter, ViewModel) → MVVM stack complete
5. Integration → Full end-to-end pipeline

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story
- All generators follow `FlowKtGenerator` pattern: `fun generate(flowGraph, generatedPackage, usecasesPackage): String`
- Use StopWatch reference files as the canonical example for generated output
- Generated files always go to `{base}.generated` package and are always overwritten
- User code lives only in `{base}.usecases.logicmethods/` tick val stubs
