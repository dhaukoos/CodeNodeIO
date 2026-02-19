# Tasks: Refactor Base NodeRuntime Class

**Input**: Design documents from `/specs/021-refactor-noderuntime-base/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Existing tests serve as regression suite. Test updates are grouped with their corresponding runtime changes.

**Organization**: Tasks grouped by user story. US1 is foundational and MUST complete before US2-US4. US2 MUST complete before US3 (establishes pattern). US4 depends on US2+US3.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **fbpDsl module**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/`
- **fbpDsl tests**: `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/`
- **StopWatch module**: `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/`
- **StopWatch tests**: `StopWatch/src/commonTest/kotlin/io/codenode/stopwatch/`

---

## Phase 1: Setup

**Purpose**: Verify current state and establish baseline

- [x] T001 Run all existing tests to establish passing baseline via `./gradlew :fbpDsl:jvmTest :StopWatch:jvmTest`

---

## Phase 2: User Story 1 - Remove Generic Type and Channels from Base Runtime (Priority: P1) - Foundational

**Goal**: Strip `NodeRuntime` of its generic type parameter `<T: Any>` and its `inputChannel`/`outputChannel` properties, making it a pure lifecycle manager.

**Independent Test**: Base NodeRuntime compiles without generic parameter, lifecycle methods work correctly.

**⚠️ CRITICAL**: US2, US3, and US4 all depend on this phase completing first. After this phase, the codebase will NOT compile until subclasses are updated.

### Implementation for User Story 1

- [x] T002 [US1] Remove generic type parameter `<T: Any>` from class signature in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/NodeRuntime.kt`
- [x] T003 [US1] Remove `inputChannel: ReceiveChannel<T>?` property from `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/NodeRuntime.kt`
- [x] T004 [US1] Remove `outputChannel: SendChannel<T>?` property from `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/NodeRuntime.kt`
- [x] T005 [US1] Remove `outputChannel?.close()` from the `finally` block in `start()` method in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/NodeRuntime.kt`
- [x] T006 [US1] Remove unused imports (`ReceiveChannel`, `SendChannel`) from `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/NodeRuntime.kt`

**Checkpoint**: NodeRuntime is now a non-generic lifecycle manager. Compilation is expected to fail until subclasses are updated.

---

## Phase 3: User Story 2 - Update Single-Type Runtime Subclasses (Priority: P2)

**Goal**: Update GeneratorRuntime, SinkRuntime, TransformerRuntime, and FilterRuntime to inherit from non-generic NodeRuntime and define their own channel properties.

**Independent Test**: Each updated subclass compiles, passes its existing tests with property name updates.

### Implementation for User Story 2

- [x] T007 [P] [US2] Update GeneratorRuntime: change `NodeRuntime<T>(codeNode)` to `NodeRuntime(codeNode)`, define own `outputChannel: SendChannel<T>?` property in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/GeneratorRuntime.kt`
- [x] T008 [P] [US2] Update SinkRuntime: change `NodeRuntime<T>(codeNode)` to `NodeRuntime(codeNode)`, add own `inputChannel: ReceiveChannel<T>?` property (same name, single-input) in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/SinkRuntime.kt`
- [x] T009 [P] [US2] Update TransformerRuntime: change `NodeRuntime<TIn>(codeNode)` to `NodeRuntime(codeNode)`, add own `inputChannel: ReceiveChannel<TIn>?` property (same name, single-input), rename `transformerOutputChannel` to `outputChannel` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/TransformerRuntime.kt`
- [x] T010 [P] [US2] Update FilterRuntime: change `NodeRuntime<T>(codeNode)` to `NodeRuntime(codeNode)`, add own `inputChannel: ReceiveChannel<T>?` and `outputChannel: SendChannel<T>?` properties (same names, single-input/output) in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/FilterRuntime.kt`

**Checkpoint**: All 4 single-type runtime subclasses compile with updated inheritance and own channel properties.

---

## Phase 4: User Story 3 - Update Multi-Port Runtime Subclasses (Priority: P3)

**Goal**: Update all 12 multi-port runtime subclasses to inherit from non-generic NodeRuntime. Multi-input classes add own `inputChannel1` property; single-input classes add own `inputChannel` property. Single-output processors rename prefixed output channels to `outputChannel`.

**Independent Test**: Each updated subclass compiles, and existing typed node runtime tests pass with property name updates.

### Implementation for User Story 3

- [x] T011 [P] [US3] Update Out2GeneratorRuntime: change `NodeRuntime<U>(codeNode)` to `NodeRuntime(codeNode)` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/Out2GeneratorRuntime.kt`
- [x] T012 [P] [US3] Update Out3GeneratorRuntime: change `NodeRuntime<U>(codeNode)` to `NodeRuntime(codeNode)` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/Out3GeneratorRuntime.kt`
- [x] T013 [P] [US3] Update In2SinkRuntime: change `NodeRuntime<A>(codeNode)` to `NodeRuntime(codeNode)`, add `inputChannel1: ReceiveChannel<A>?` property, update internal `inputChannel` references to `inputChannel1` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In2SinkRuntime.kt`
- [x] T014 [P] [US3] Update In3SinkRuntime: change `NodeRuntime<A>(codeNode)` to `NodeRuntime(codeNode)`, add `inputChannel1: ReceiveChannel<A>?` property, update internal `inputChannel` references to `inputChannel1` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In3SinkRuntime.kt`
- [x] T015 [P] [US3] Update In1Out2Runtime: change `NodeRuntime<A>(codeNode)` to `NodeRuntime(codeNode)`, add own `inputChannel: ReceiveChannel<A>?` property (same name, single-input) in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In1Out2Runtime.kt`
- [x] T016 [P] [US3] Update In1Out3Runtime: change `NodeRuntime<A>(codeNode)` to `NodeRuntime(codeNode)`, add own `inputChannel: ReceiveChannel<A>?` property (same name, single-input) in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In1Out3Runtime.kt`
- [x] T017 [P] [US3] Update In2Out1Runtime: change `NodeRuntime<A>(codeNode)` to `NodeRuntime(codeNode)`, add `inputChannel1: ReceiveChannel<A>?` property, update internal `inputChannel` references to `inputChannel1`, rename `processorOutputChannel` to `outputChannel` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In2Out1Runtime.kt`
- [x] T018 [P] [US3] Update In2Out2Runtime: change `NodeRuntime<A>(codeNode)` to `NodeRuntime(codeNode)`, add `inputChannel1: ReceiveChannel<A>?` property, update internal `inputChannel` references to `inputChannel1` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In2Out2Runtime.kt`
- [x] T019 [P] [US3] Update In2Out3Runtime: change `NodeRuntime<A>(codeNode)` to `NodeRuntime(codeNode)`, add `inputChannel1: ReceiveChannel<A>?` property, update internal `inputChannel` references to `inputChannel1` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In2Out3Runtime.kt`
- [x] T020 [P] [US3] Update In3Out1Runtime: change `NodeRuntime<A>(codeNode)` to `NodeRuntime(codeNode)`, add `inputChannel1: ReceiveChannel<A>?` property, update internal `inputChannel` references to `inputChannel1`, rename `processorOutputChannel` to `outputChannel` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In3Out1Runtime.kt`
- [x] T021 [P] [US3] Update In3Out2Runtime: change `NodeRuntime<A>(codeNode)` to `NodeRuntime(codeNode)`, add `inputChannel1: ReceiveChannel<A>?` property, update internal `inputChannel` references to `inputChannel1` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In3Out2Runtime.kt`
- [x] T022 [P] [US3] Update In3Out3Runtime: change `NodeRuntime<A>(codeNode)` to `NodeRuntime(codeNode)`, add `inputChannel1: ReceiveChannel<A>?` property, update internal `inputChannel` references to `inputChannel1` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In3Out3Runtime.kt`

**Checkpoint**: All 16 runtime subclasses compile with non-generic NodeRuntime inheritance and own channel properties.

---

## Phase 5: User Story 4 - Update External References (Priority: P4)

**Goal**: Update RuntimeRegistry, CodeNodeFactory, test files, and StopWatch components to match new API.

**Independent Test**: Full test suite across all modules passes with zero failures.

### Implementation for User Story 4

- [x] T023 [P] [US4] Update RuntimeRegistry: replace all `NodeRuntime<*>` with `NodeRuntime` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/RuntimeRegistry.kt`
- [x] T024 [P] [US4] Update NodeRuntimeTest: remove channel-related tests, update `NodeRuntime<*>` references to `NodeRuntime` in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/NodeRuntimeTest.kt`
- [x] T025 [P] [US4] Update ContinuousFactoryTest: rename `.inputChannel =` to `.inputChannel1 =` for multi-input runtimes only (single-input keeps `.inputChannel`), rename `.transformerOutputChannel` to `.outputChannel` in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/ContinuousFactoryTest.kt`
- [x] T026 [P] [US4] Update TypedNodeRuntimeTest: rename `.inputChannel =` to `.inputChannel1 =` for multi-input runtimes only, rename `.processorOutputChannel` to `.outputChannel` in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/TypedNodeRuntimeTest.kt`
- [x] T027 [P] [US4] Update PauseResumeTest: rename `.inputChannel =` to `.inputChannel1 =` for multi-input runtimes only, rename `.transformerOutputChannel` to `.outputChannel` in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/PauseResumeTest.kt`
- [x] T028 [P] [US4] Update IndependentControlTest: rename `.inputChannel =` to `.inputChannel1 =` for multi-input runtimes only in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/IndependentControlTest.kt`
- [x] T029 [P] [US4] Update RuntimeRegistryTest: replace all `NodeRuntime<*>` with `NodeRuntime` in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/RuntimeRegistryTest.kt`
- [x] T030 [P] [US4] Update RuntimeRegistrationTest: replace all `NodeRuntime<*>` with `NodeRuntime` in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/RuntimeRegistrationTest.kt`
- [x] T031 [P] [US4] Update DisplayReceiverComponent: no input rename needed (single-input keeps `inputChannel`), verify delegation still works in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/DisplayReceiverComponent.kt`
- [x] T032 [P] [US4] Update StopWatchFlow: no input rename needed (single-input keeps `.inputChannel`), verify wiring still works in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchFlow.kt`
- [x] T033 [P] [US4] Update ChannelIntegrationTest: no input rename needed (single-input keeps `.inputChannel`), verify assertions still work in `StopWatch/src/commonTest/kotlin/io/codenode/stopwatch/ChannelIntegrationTest.kt`
- [x] T034 [US4] Verify fbpDsl tests pass via `./gradlew :fbpDsl:jvmTest`
- [x] T035 [US4] Verify StopWatch tests pass via `./gradlew :StopWatch:jvmTest`

**Checkpoint**: Full codebase compiles and all tests pass. Refactoring is complete.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and documentation

- [ ] T036 Run full test suite across all modules via `./gradlew :fbpDsl:jvmTest :StopWatch:jvmTest`
- [ ] T037 Verify SC-001: NodeRuntime has zero generic type parameters
- [ ] T038 Verify SC-002: NodeRuntime has zero channel properties
- [ ] T039 Verify SC-005: Channel naming follows convention (single-input: `inputChannel`, multi-input: `inputChannel1`; single-output: `outputChannel`, multi-output: `outputChannel1`)
- [ ] T040 Update quickstart.md with final code examples reflecting actual implementation in `specs/021-refactor-noderuntime-base/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - establishes baseline
- **US1 (Phase 2)**: Depends on Setup - BLOCKS US2, US3, US4
- **US2 (Phase 3)**: Depends on US1 - establishes pattern for US3
- **US3 (Phase 4)**: Depends on US1 - can run in parallel with US2 (different files)
- **US4 (Phase 5)**: Depends on US2 + US3 (needs all runtime classes updated before tests can pass)
- **Polish (Phase 6)**: Depends on all user stories complete

### User Story Dependencies

- **User Story 1 (P1)**: Foundational - must complete first (modifies base class)
- **User Story 2 (P2)**: Depends on US1 - fixes 4 single-type runtimes
- **User Story 3 (P3)**: Depends on US1 - fixes 12 multi-port runtimes (can parallel with US2)
- **User Story 4 (P4)**: Depends on US2 + US3 - fixes external references and tests

### Within Each User Story

- US1: Sequential (all changes to same file)
- US2: All [P] tasks can run in parallel (different files)
- US3: All [P] tasks can run in parallel (different files)
- US4: All [P] tasks can run in parallel (different files), then sequential verification

### Parallel Opportunities

- T007-T010: All US2 runtime updates can run in parallel (different files)
- T011-T022: All US3 runtime updates can run in parallel (different files)
- T023-T033: All US4 reference updates can run in parallel (different files)
- US2 and US3 can run in parallel with each other (different files, same dependency on US1)

---

## Implementation Strategy

### MVP First (US1 + US2 + US3 + US4)

This is an atomic refactoring - all phases must complete for the code to compile and tests to pass. There is no meaningful partial delivery point.

1. Complete Phase 1: Setup (baseline)
2. Complete Phase 2: US1 - Modify base NodeRuntime
3. Complete Phase 3+4: US2 + US3 in parallel - Fix all 16 subclasses
4. Complete Phase 5: US4 - Fix all external references and tests
5. Complete Phase 6: Polish and validation
6. **VALIDATE**: All tests pass, all success criteria met

### Incremental Delivery

Due to the atomic nature of this refactoring (base class change breaks all subclasses), the practical approach is:

1. Setup → Baseline established
2. US1 (base class) → Breaks compilation
3. US2 + US3 (all subclasses) → Restores compilation in runtime module
4. US4 (external references + tests) → Restores full compilation and tests
5. Polish → Final validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- This is an atomic refactoring - the codebase will not compile between US1 and US4 completion
- T002-T006 are logically one change to NodeRuntime.kt but broken into separate tasks for clarity
- Commit after each phase checkpoint
- The refactoring preserves all observable behavior - only property names and type signatures change
