# Tasks: Reactive Feedback Loop for Source Nodes

**Input**: Design documents from `/specs/038-reactive-source-loop/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: No project initialization needed — existing multi-module KMP project. The `attenuationDelayMs` property already exists on `NodeRuntime` base class.

*(No setup tasks — all infrastructure already exists)*

---

## Phase 2: User Story 1 — Processor Delay Controls Cycle Rate (Priority: P1)

**Goal**: Add the 2-line attenuation delay check in all processor/transformer/filter runtimes, after receive and before process. The `attenuationDelayMs` property already exists on `NodeRuntime`; this phase wires it into the processing loops.

**Independent Test**: Create a processor with `attenuationDelayMs = 1000L`, send input, verify output arrives only after the configured delay using virtual time. `./gradlew :fbpDsl:jvmTest` passes.

### Implementation for User Story 1

- [X] T001 [P] [US1] Add attenuation delay to `TransformerRuntime.kt`: insert `val delayMs = attenuationDelayMs; if (delayMs != null && delayMs > 0) delay(delayMs)` between `receive()` and `transform()` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/TransformerRuntime.kt`
- [X] T002 [P] [US1] Add attenuation delay to `FilterRuntime.kt`: insert delay check between `receive()` and `predicate()` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/FilterRuntime.kt`
- [X] T003 [P] [US1] Add attenuation delay to standard multi-input/output processor runtimes (6 files): insert delay check after all `receive()` calls and before `process()` call in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/` files: `In1Out2Runtime.kt`, `In1Out3Runtime.kt`, `In2Out1Runtime.kt`, `In2Out2Runtime.kt`, `In2Out3Runtime.kt`, `In3Out1Runtime.kt`
- [X] T004 [P] [US1] Add attenuation delay to remaining standard multi-input/output processor runtimes (4 files): `In3Out2Runtime.kt`, `In3Out3Runtime.kt` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/`. Note: `In2Out2Runtime.kt` and `In2Out3Runtime.kt` covered in T003.
- [X] T005 [P] [US1] Add attenuation delay to any-input processor runtimes (6 files): insert delay check inside each `select { onReceive { } }` branch, after `lastValue` update and before `process()` call in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/` files: `In2AnyOut1Runtime.kt`, `In2AnyOut2Runtime.kt`, `In2AnyOut3Runtime.kt`, `In3AnyOut1Runtime.kt`, `In3AnyOut2Runtime.kt`, `In3AnyOut3Runtime.kt`
- [X] T006 [US1] Add attenuation delay test to `TypedNodeRuntimeTest.kt`: test `In2Out2Runtime respects attenuationDelayMs before processing` using virtual time (`advanceTimeBy`), verify no output before delay and output after delay in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/TypedNodeRuntimeTest.kt`
- [X] T007 [US1] Compile and run runtime tests: `./gradlew :fbpDsl:jvmTest`

**Checkpoint**: All processor runtimes respect `attenuationDelayMs`. Existing tests pass unchanged (null default preserves behavior). New test validates delay behavior.

---

## Phase 3: User Story 2 — Source Nodes Reactively Re-emit on State Changes (Priority: P2)

**Goal**: Update `RuntimeFlowGenerator` to replace `awaitCancellation()` in source node generate blocks with reactive StateFlow observation using `combine` + `drop(1)` + `collect`.

**Independent Test**: Generated source code for source nodes contains `combine`/`drop(1)`/`collect` patterns instead of `awaitCancellation()`. `./gradlew :kotlinCompiler:jvmTest` passes.

### Implementation for User Story 2

- [X] T008 [US2] Add `generateReactiveSourceBlock()` method to `RuntimeFlowGenerator.kt`: generate source observation pattern based on output port count (1-output: single flow `drop(1).collect`, 2-output: `combine` of 2 flows + `ProcessResult2`, 3-output: `combine` of 3 flows + `ProcessResult3`). Use `ObservableStateResolver` to match source node output ports to state properties. Replace `awaitCancellation()` call with the reactive block in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeFlowGenerator.kt`
- [X] T009 [US2] Add required imports for reactive source blocks in `RuntimeFlowGenerator.generateImports()`: add `kotlinx.coroutines.flow.combine`, `kotlinx.coroutines.flow.drop`, and conditional `ProcessResult2`/`ProcessResult3` imports when source nodes exist in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeFlowGenerator.kt`
- [X] T010 [US2] Update existing `RuntimeFlowGeneratorTest.kt` source block assertions: change tests that assert `awaitCancellation()` to assert the `combine`/`drop(1)`/`collect` pattern for 1-output, 2-output, and 3-output source nodes in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/RuntimeFlowGeneratorTest.kt`
- [X] T011 [US2] Compile and run generator tests: `./gradlew :kotlinCompiler:jvmTest`

**Checkpoint**: Source nodes generate reactive observation blocks. Existing generator tests updated and passing.

---

## Phase 4: User Story 3 — Controller Primes Source Nodes on Start (Priority: P3)

**Goal**: Update `RuntimeControllerGenerator` to: (1) accept state package parameter, (2) add State import, (3) generate priming logic in `start()`, (4) propagate attenuation delay to ALL nodes (not just sources).

**Independent Test**: Generated controller code contains priming `send()` calls in `start()`, State import, and `setAttenuationDelay` sets delay on all nodes. `./gradlew :kotlinCompiler:jvmTest` passes.

### Implementation for User Story 3

- [ ] T012 [US3] Add `viewModelPackage` parameter to `RuntimeControllerGenerator.generate()` method signature. Generate State import (`import ${viewModelPackage}.${flowName}State`) when source nodes with observable state exist in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeControllerGenerator.kt`
- [ ] T013 [US3] Generate priming logic in controller `start()` method: after `flow.start(scope)`, generate `scope.launch { }` block with `send()` calls for each source node's output ports using corresponding state property values (e.g., `flow.timerEmitter.outputChannel1?.send(StopWatchState._elapsedSeconds.value)`) in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeControllerGenerator.kt`
- [ ] T014 [US3] Update `generateSetAttenuationDelayMethod()` to iterate ALL `codeNodes` (not just source/generator nodes) so delay propagates to processor, transformer, and filter runtimes in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeControllerGenerator.kt`
- [ ] T015 [US3] Update `ModuleSaveService.kt` to pass `basePackage` (or viewModel package) as the `viewModelPackage` parameter when calling `runtimeControllerGenerator.generate()` in `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt`
- [ ] T016 [US3] Update `RuntimeControllerGeneratorTest.kt`: update attenuation delay test to assert delay is set on ALL nodes (not just sources), add priming test asserting `send()` calls in `start()`, add State import test in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/RuntimeControllerGeneratorTest.kt`
- [ ] T017 [US3] Compile and run tests: `./gradlew :kotlinCompiler:jvmTest :graphEditor:jvmTest`

**Checkpoint**: Controller primes source channels on start, imports State, and propagates delay to all nodes. Tests pass.

---

## Phase 5: User Story 4 — End-to-End Feedback Loop (Priority: P4)

**Goal**: Update StopWatch processing logic to use input parameter values (not read directly from global state), completing the feedback loop pattern. Regenerate StopWatch and verify end-to-end behavior.

**Independent Test**: StopWatch start → seconds increment at 1-second intervals, pause stops, resume continues, reset zeros.

### Implementation for User Story 4

- [ ] T018 [US4] Update `TimeIncrementerProcessLogic.kt` to use input parameters: change `{ _, _ ->` to `{ elapsedSeconds, elapsedMinutes ->`, replace `StopWatchState._elapsedSeconds.value + 1` with `elapsedSeconds + 1`, replace `StopWatchState._elapsedMinutes.value` with `elapsedMinutes`. Keep state write-back (`StopWatchState._elapsedSeconds.value = newSeconds`) to trigger reactive source re-emission in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/processingLogic/TimeIncrementerProcessLogic.kt`
- [ ] T019 [US4] Regenerate StopWatch module: run the graphEditor "Save + Regen Stubs" or manually regenerate `StopWatchFlow.kt` and `StopWatchController.kt` to include reactive source blocks and controller priming logic
- [ ] T020 [US4] Compile and run all tests: `./gradlew :fbpDsl:jvmTest :kotlinCompiler:jvmTest :graphEditor:jvmTest`

**Checkpoint**: StopWatch processing logic uses input values. Regenerated flow code includes reactive source blocks and controller priming.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Full build verification and manual integration testing.

- [ ] T021 Full build verification: `./gradlew build` — all modules compile and all tests pass

**Checkpoint**: Entire project builds. Reactive feedback loop is fully implemented.

---

## Dependencies & Execution Order

### Phase Dependencies

- **US1 (Phase 2)**: No dependencies — start immediately. Independent runtime-level change.
- **US2 (Phase 3)**: No dependencies on US1 — code generator change (different module). Can run in parallel with US1.
- **US3 (Phase 4)**: No dependencies on US1/US2 — controller generator change. Can run in parallel. However, test assertions may reference patterns from US2.
- **US4 (Phase 5)**: Depends on US1, US2, US3 being complete — end-to-end integration requires all pieces.
- **Polish (Phase 6)**: Depends on all user stories being complete.

### User Story Dependencies

```
US1 (Processor delay in runtimes) ─────────┐
US2 (Reactive source generate blocks) ──────┼── US4 (End-to-end feedback loop) ── Polish
US3 (Controller priming + propagation) ────┘
```

### Within Each User Story

- Runtime changes before test changes
- Implementation before tests
- Tests before compile checkpoint

### Parallel Opportunities

- **Within US1**: T001, T002, T003, T004, T005 can run in parallel (different files)
- **Across stories**: US1, US2, US3 can all run in parallel (different modules/files)
- **Within US3**: T012, T013, T014 modify the same file sequentially; T015 is independent

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: User Story 1 (T001–T007)
2. **STOP and VALIDATE**: Processor runtimes respect attenuation delay, all tests pass
3. This alone delivers rate-limiting capability

### Incremental Delivery

1. US1 (T001–T007) → Processor delay working
2. US2 (T008–T011) → Source nodes reactively observe state
3. US3 (T012–T017) → Controller primes and propagates delay
4. US4 (T018–T020) → StopWatch end-to-end feedback loop
5. Polish (T021) → Full build verified

Each increment leaves the project in a compilable, testable state.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- `attenuationDelayMs` property already exists on `NodeRuntime` base class — no need to add it
- The delay insertion is a uniform 2-line pattern: `val delayMs = attenuationDelayMs; if (delayMs != null && delayMs > 0) delay(delayMs)`
- For any-input runtimes, the delay goes inside each `select { onReceive { } }` branch
- `drop(1)` is critical: without it, the initial `combine` emission duplicates the controller's prime
- Specs/ documentation files are historical records and should NOT be updated
