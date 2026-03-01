# Tasks: Any-Input Trigger Mode for Node Generator

**Input**: Design documents from `/specs/035-any-input-trigger/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Tests**: Included per constitution TDD mandate. Runtime classes require comprehensive testing (8 variants, concurrent behavior). Code generators require verification of correct output.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: User Story 1 — Create Any-Input Node via Node Generator (Priority: P1)

**Goal**: Add an "Any Input" toggle to the Node Generator UI that creates custom node definitions with the any-input type identifier (`in2anyout1` etc.) and persists the flag.

**Independent Test**: Open the Node Generator, set 2+ inputs, toggle "Any Input" ON, verify type preview shows `in2anyout1`. Create the node and verify it appears in the palette with the correct type. Reduce inputs to 1 and verify the toggle hides and turns OFF.

### Implementation for User Story 1

- [X] T001 [P] [US1] Add `anyInput: Boolean = false` parameter to `createGenericNodeType()` function — when true, generate `_genericType` as `"in${numInputs}anyout${numOutputs}"` instead of `"in${numInputs}out${numOutputs}"` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactory.kt`
- [X] T002 [P] [US1] Add `anyInput: Boolean = false` field to `NodeGeneratorPanelState` data class, add computed `val showAnyInputToggle: Boolean get() = inputCount >= 2`, update `genericType` computed property to return `"in${inputCount}anyout${outputCount}"` when `anyInput` is true in `graphEditor/src/jvmMain/kotlin/viewmodel/NodeGeneratorViewModel.kt`
- [X] T003 [US1] Add `anyInput: Boolean = false` field to `CustomNodeDefinition` data class, update `create()` factory method to accept `anyInput` parameter and compute `genericType` conditionally (`"in${inputCount}anyout${outputCount}"` when true), update `toNodeTypeDefinition()` to pass `anyInput` to `createGenericNodeType()` in `graphEditor/src/jvmMain/kotlin/repository/CustomNodeDefinition.kt`
- [X] T004 [US1] Add `setAnyInput(anyInput: Boolean)` method to `NodeGeneratorViewModel`, update `setInputCount()` to auto-disable anyInput (set to false) when new count < 2, update `createNode()` to pass `state.anyInput` to `CustomNodeDefinition.create()`, update `reset()` to reset anyInput to false in `graphEditor/src/jvmMain/kotlin/viewmodel/NodeGeneratorViewModel.kt`
- [X] T005 [US1] Add "Any Input" `Switch` composable to `NodeGeneratorPanelContent` — add `onAnyInputChange: (Boolean) -> Unit` callback parameter, render Switch between the output dropdown and type preview, visible only when `state.showAnyInputToggle` is true, wire to `viewModel.setAnyInput(it)` in `NodeGeneratorPanel` in `graphEditor/src/jvmMain/kotlin/ui/NodeGeneratorPanel.kt`
- [X] T006 [US1] Compile and verify: `./gradlew :graphEditor:compileKotlinJvm :fbpDsl:compileKotlinJvm`

**Checkpoint**: Node Generator UI displays the "Any Input" toggle for 2+ inputs, type preview updates correctly, custom nodes persist with anyInput flag. Toggle auto-hides when inputs reduced below 2.

---

## Phase 2: User Story 2 — Any-Input Runtime Behavior (Priority: P2)

**Goal**: Create 8 new `In{A}AnyOut{B}Runtime` classes that fire their process block when ANY input delivers data (using Kotlin `select` expression), along with corresponding type aliases and factory methods.

**Independent Test**: Create an `In2AnyOut1Runtime` instance, send data to only input 1, verify process block fires with input 1's value and default (0) for input 2. Send data to input 2, verify process block fires with cached input 1 value and new input 2 value. Test pause/resume/stop/reset.

### Implementation for User Story 2

- [ ] T007 [US2] Add 16 new type aliases to `ContinuousTypes.kt` — 8 process block aliases (`In2AnyOut1ProcessBlock<A, B, R>`, `In2AnySinkBlock<A, B>`, `In2AnyOut2ProcessBlock<A, B, U, V>`, `In2AnyOut3ProcessBlock<A, B, U, V, W>`, `In3AnyOut1ProcessBlock<A, B, C, R>`, `In3AnySinkBlock<A, B, C>`, `In3AnyOut2ProcessBlock<A, B, C, U, V>`, `In3AnyOut3ProcessBlock<A, B, C, U, V, W>`) + 8 tick block aliases (`In2AnyOut1TickBlock`, `In2AnySinkTickBlock`, `In2AnyOut2TickBlock`, `In2AnyOut3TickBlock`, `In3AnyOut1TickBlock`, `In3AnySinkTickBlock`, `In3AnyOut2TickBlock`, `In3AnyOut3TickBlock`) in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ContinuousTypes.kt`
- [ ] T008 [P] [US2] Create `In2AnyOut1Runtime<A : Any, B : Any, R : Any>` — extends `NodeRuntime`, has `inputChannel1`, `inputChannel2`, `outputChannel`, `lastValue1: A`, `lastValue2: B` cached fields. `start()` uses `select` expression to fire process block on ANY input receive, passing cached values for non-triggered inputs. Include `reset()` to clear cached values to initial defaults. Handle pause/resume/stop, channel closure in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In2AnyOut1Runtime.kt`
- [ ] T009 [P] [US2] Create `In2AnySinkRuntime<A : Any, B : Any>` — same pattern as T008 but no output channel, uses `In2AnySinkBlock`, consumes values without producing output in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In2AnySinkRuntime.kt`
- [ ] T010 [P] [US2] Create `In2AnyOut2Runtime<A : Any, B : Any, U : Any, V : Any>` — 2 inputs, 2 outputs, uses `In2AnyOut2ProcessBlock` returning `ProcessResult2`, sends non-null results to respective output channels in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In2AnyOut2Runtime.kt`
- [ ] T011 [P] [US2] Create `In2AnyOut3Runtime<A : Any, B : Any, U : Any, V : Any, W : Any>` — 2 inputs, 3 outputs, uses `In2AnyOut3ProcessBlock` returning `ProcessResult3` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In2AnyOut3Runtime.kt`
- [ ] T012 [P] [US2] Create `In3AnyOut1Runtime<A : Any, B : Any, C : Any, R : Any>` — 3 inputs, 1 output, has `lastValue1`, `lastValue2`, `lastValue3` cached fields, 3-way `select` expression in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In3AnyOut1Runtime.kt`
- [ ] T013 [P] [US2] Create `In3AnySinkRuntime<A : Any, B : Any, C : Any>` — 3-input any-trigger sink, no output, uses `In3AnySinkBlock` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In3AnySinkRuntime.kt`
- [ ] T014 [P] [US2] Create `In3AnyOut2Runtime<A : Any, B : Any, C : Any, U : Any, V : Any>` — 3 inputs, 2 outputs in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In3AnyOut2Runtime.kt`
- [ ] T015 [P] [US2] Create `In3AnyOut3Runtime<A : Any, B : Any, C : Any, U : Any, V : Any, W : Any>` — 3 inputs, 3 outputs in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In3AnyOut3Runtime.kt`
- [ ] T016 [US2] Add 8 factory methods to `CodeNodeFactory` — `createIn2AnyOut1Processor<A, B, R>`, `createIn2AnySink<A, B>`, `createIn2AnyOut2Processor<A, B, U, V>`, `createIn2AnyOut3Processor<A, B, U, V, W>`, `createIn3AnyOut1Processor<A, B, C, R>`, `createIn3AnySink<A, B, C>`, `createIn3AnyOut2Processor<A, B, C, U, V>`, `createIn3AnyOut3Processor<A, B, C, U, V, W>` — following the pattern of existing factory methods but using any-input runtime classes in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt`
- [ ] T017 [US2] Create test class `AnyInputRuntimeTest` with tests for all 8 any-input runtime variants verifying: (1) process block fires when only one input receives data, (2) cached last values are provided for non-triggered inputs, (3) type-appropriate defaults used before first receive, (4) pause/resume lifecycle works, (5) stop/reset clears cached values in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/AnyInputRuntimeTest.kt`
- [ ] T018 [US2] Compile and run tests: `./gradlew :fbpDsl:allTests`

**Checkpoint**: All 8 any-input runtime variants compile, pass tests, and demonstrate correct select-based concurrent channel listening behavior.

---

## Phase 3: User Story 3 — Code Generation Support for Any-Input Nodes (Priority: P3)

**Goal**: Update the code generators (RuntimeTypeResolver, RuntimeFlowGenerator, ProcessingLogicStubGenerator) to recognize any-input nodes and produce correct class names, factory method calls, and tick type aliases in generated code.

**Independent Test**: Configure a FlowGraph with a node whose `_genericType` configuration contains "any" (e.g., `in2anyout1`), run the generators, and verify the output references `createIn2AnyOut1Processor`, `In2AnyOut1Runtime`, and `In2AnyOut1TickBlock`.

### Implementation for User Story 3

- [ ] T019 [US3] Add `anyInput: Boolean = false` parameter to `getFactoryMethodName()`, `getRuntimeTypeName()`, and `getTickParamName()` in `RuntimeTypeResolver` — when true, return any-input variant names (e.g., `createIn2AnyOut1Processor` instead of `createIn2Out1Processor`, `In2AnyOut1Runtime<A, B, R>` instead of `In2Out1Runtime<A, B, R>`) for all 8 multi-input combinations in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeTypeResolver.kt`
- [ ] T020 [P] [US3] Update `RuntimeFlowGenerator` to detect anyInput flag from each node's `_genericType` configuration value (check if it contains "any"), pass the derived boolean to `runtimeTypeResolver.getFactoryMethodName(node, anyInput)` and `runtimeTypeResolver.getRuntimeTypeName(node, anyInput)` calls in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeFlowGenerator.kt`
- [ ] T021 [P] [US3] Update `ProcessingLogicStubGenerator.getTickTypeAlias()` to accept `anyInput: Boolean = false` parameter — when true, return any-input tick type alias names (e.g., `In2AnyOut1TickBlock` instead of `In2Out1TickBlock`). Update `generateStub()` and `generateStubWithPreservedBody()` to detect anyInput from `codeNode.configuration["_genericType"]` and pass it through in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGenerator.kt`
- [ ] T022 [P] [US3] Update `RuntimeTypeResolverTest` — add test cases for all 8 any-input variant mappings verifying `getFactoryMethodName(node, anyInput=true)`, `getRuntimeTypeName(node, anyInput=true)`, and `getTickParamName(node, anyInput=true)` return correct any-input names in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/RuntimeTypeResolverTest.kt`
- [ ] T023 [P] [US3] Update `RuntimeFlowGeneratorTest` — add test case with a FlowGraph containing an any-input node (set `_genericType` to `in2anyout1` in node configuration), verify generated code contains `createIn2AnyOut1Processor` factory call and correct runtime class reference in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/RuntimeFlowGeneratorTest.kt`
- [ ] T024 [P] [US3] Update `ProcessingLogicStubGeneratorTest` — add test case verifying that a node with `_genericType` containing "any" generates a stub with `In2AnyOut1TickBlock` (or equivalent) tick type alias in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGeneratorTest.kt`
- [ ] T025 [US3] Compile and run all tests: `./gradlew :kotlinCompiler:allTests :graphEditor:jvmTest :fbpDsl:allTests`

**Checkpoint**: Code generators correctly produce any-input class names, factory methods, and tick type aliases. All generator tests pass.

---

## Dependencies & Execution Order

### Phase Dependencies

- **User Story 1 (Phase 1)**: No dependencies — can start immediately
- **User Story 2 (Phase 2)**: No dependencies on US1 — can start immediately (different modules/files). Can run in parallel with US1.
- **User Story 3 (Phase 3)**: Depends on US2 (needs runtime class names and type alias names for code generation). Must complete after US2.

### Within Each Phase

**US1**: (T001 ∥ T002) → T003 → T004 → T005 → T006
**US2**: T007 → (T008 ∥ T009 ∥ T010 ∥ T011 ∥ T012 ∥ T013 ∥ T014 ∥ T015) → T016 → T017 → T018
**US3**: T019 → (T020 ∥ T021) → (T022 ∥ T023 ∥ T024) → T025

### Parallel Opportunities

- US1 and US2 can run in parallel (completely independent: different files, different modules)
- Within US1: T001/T002 (GenericNodeTypeFactory + NodeGeneratorPanelState) can run in parallel
- Within US2: T008-T015 (all 8 runtime classes) can all run in parallel after T007
- Within US3: T020/T021 (flow gen + stub gen) can run in parallel after T019
- Within US3: T022/T023/T024 (test updates) can all run in parallel

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: US1 — Add toggle to Node Generator (T001–T006)
2. **STOP and VALIDATE**: Open Node Generator, verify toggle appears/hides, type preview updates, node persists
3. UI feature is complete and visible

### Incremental Delivery

1. US1 (T001–T006): UI toggle + persistence → Users can create any-input node definitions
2. US2 (T007–T018): Runtime classes + factory → Any-input nodes have runtime behavior
3. US3 (T019–T025): Code generators → Any-input nodes participate in full save-compile-run lifecycle

### Parallel Team Strategy

With two developers:
1. Developer A: US1 (T001–T006) — Node Generator UI
2. Developer B: US2 (T007–T018) — Runtime classes
3. Both done → either developer: US3 (T019–T025) — Code generators

---

## Notes

- US1 and US2 are completely independent and can run in parallel
- US3 MUST wait for US2 to complete (needs runtime class/type alias names)
- All tasks within a phase marked [P] can run in parallel (different files, no dependencies)
- Kotlin `select` expression from `kotlinx.coroutines.selects` is the core mechanism for any-input behavior
- Cached `lastValue` fields in runtime classes need type-appropriate initial defaults (0 for Int, "" for String, etc.) — these are type parameters, so initial values must be provided at construction time or use `lateinit`/nullable approaches
- The `_genericType` configuration field is the bridge between the UI/persistence layer and the code generators — presence of "any" in the string signals any-input mode
