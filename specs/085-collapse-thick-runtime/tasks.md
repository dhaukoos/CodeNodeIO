---
description: "Task list for feature 085: Collapse the entity-module thick runtime onto DynamicPipelineController"
---

# Tasks: Collapse the Entity-Module Thick Runtime onto DynamicPipelineController

**Input**: Design documents from `/Users/dhaukoos/CodeNodeIO/specs/085-collapse-thick-runtime/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/universal-runtime.md, quickstart.md

**Tests**: Plan calls for tests-first per Constitution principle II (TDD). Test tasks ARE included.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4, US5)
- All file paths are absolute

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: No new dependencies or project structure needed. Confirm pre-flight build is green so the diff baseline is clean.

- [ ] T001 Confirm clean baseline: from repo root run `./gradlew :fbpDsl:jvmTest :flowGraph-generate:jvmTest :flowGraph-execute:jvmTest` and confirm all pass on `085-collapse-thick-runtime` before any edits
- [ ] T002 Confirm DemoProject baseline: from `/Users/dhaukoos/CodeNodeIO-DemoProject` run `./gradlew :StopWatch:compileKotlinJvm :Addresses:compileKotlinJvm :UserProfiles:compileKotlinJvm :EdgeArtFilter:compileKotlinJvm :WeatherForecast:compileKotlinJvm :KMPMobileApp:assembleDebug` and confirm all pass

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The `getStatus()` addition to `fbpDsl` AND the `ModuleSessionFactory` proxy update unblock every user story. Done first, written test-first per principle II.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T003 Write failing unit test `getStatus_returns_idle_before_start` in `/Users/dhaukoos/CodeNodeIO/fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineControllerTest.kt` (create file if absent) asserting `controller.getStatus()` returns `FlowExecutionStatus(overallState = ExecutionState.IDLE, ...)` before any `start()` call
- [ ] T004 [P] Write failing unit test `getStatus_reflects_running_after_start` in same `DynamicPipelineControllerTest.kt` asserting overall state RUNNING + non-empty per-node map after `start()` against a small valid FlowGraph + lookup
- [ ] T005 [P] Write failing unit test `getStatus_reflects_paused_after_pause` and `getStatus_returns_idle_after_stop` in same `DynamicPipelineControllerTest.kt`
- [ ] T006 Add `fun getStatus(): FlowExecutionStatus` to the `ModuleController` interface in `/Users/dhaukoos/CodeNodeIO/fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ModuleController.kt`
- [ ] T007 In `/Users/dhaukoos/CodeNodeIO/fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipeline.kt` retain the internal `RootControlNode` instance as a class field (it's currently constructed locally during start) and expose `fun getStatus(): FlowExecutionStatus = rootControlNode.getStatus()`
- [ ] T008 In `/Users/dhaukoos/CodeNodeIO/fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineController.kt` implement `override fun getStatus(): FlowExecutionStatus = pipeline?.getStatus() ?: FlowExecutionStatus(overallState = ExecutionState.IDLE, perNode = emptyMap())` (verify the `FlowExecutionStatus` empty-state constructor signature matches; adjust accordingly)
- [ ] T009 Run `./gradlew :fbpDsl:jvmTest --tests '*DynamicPipelineControllerTest*'` and confirm T003–T005 now pass
- [ ] T010 Add explicit `"getStatus" -> controller.getStatus()` case to the `when` branch in `createControllerProxy` at `/Users/dhaukoos/CodeNodeIO/flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/ModuleSessionFactory.kt:108-150`
- [ ] T011 Run `./gradlew :fbpDsl:jvmTest :flowGraph-execute:jvmTest` and confirm green; foundational changes are complete

**Checkpoint**: Foundation ready — universal runtime now satisfies the full `ModuleController` surface; user stories can proceed.

---

## Phase 3: User Story 1 — Generated modules contain only module-specific files (Priority: P1) 🎯 MVP

**Goal**: Module generation no longer emits `{Module}Controller.kt`, `{Module}ControllerAdapter.kt`, or `{Module}Flow.kt`; one new generator emits `{Module}Runtime.kt`. All five reference modules regenerate cleanly.

**Independent Test**: For each of the five reference modules, regenerate and confirm: (a) the eliminated trio is absent, (b) `{Module}Runtime.kt` is present at the module package root with the structure prescribed by `data-model.md` §4, (c) the module compiles cleanly under JVM target.

### Tests for User Story 1 ⚠️

- [ ] T012 [P] [US1] Write failing unit test `emits_node_registry_with_one_entry_per_node` in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/ModuleRuntimeGeneratorTest.kt` (create file) asserting that `ModuleRuntimeGenerator().generate(spec)` for a 3-node spec contains `object {Module}NodeRegistry` and a `when` branch with one arm per node mapping name → CodeNodeDefinition FQCN
- [ ] T013 [P] [US1] Write failing unit test `emits_factory_function_with_correct_signature` in same `ModuleRuntimeGeneratorTest.kt` asserting the emitted source contains `fun create{Module}Runtime(flowGraph: FlowGraph): {Module}ControllerInterface` and the body constructs `DynamicPipelineController` with `flowGraphProvider`, `lookup`, `onReset` arguments
- [ ] T014 [P] [US1] Write failing unit test `emits_object_expression_with_module_controller_delegation_and_typed_state_flows` in same test file asserting the factory's return value is an `object : {Module}ControllerInterface, ModuleController by controller` expression with one `override val xxx = {Module}State.xxxFlow` line per `TypedStateFlow` in the spec
- [ ] T015 [P] [US1] Write failing unit test `emits_generator_marker_comment` in same test file asserting the emitted file's first ~5 lines contain `Generated by CodeNodeIO ModuleRuntimeGenerator`
- [ ] T016 [P] [US1] Write failing unit test `emits_module_controller_superinterface_clause` in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeControllerInterfaceGeneratorTest.kt` (modify existing file) asserting the regenerated interface declaration is `interface {Module}ControllerInterface : ModuleController { ... }` with the import `io.codenode.fbpdsl.runtime.ModuleController` present, and that the body declares only the typed `val xxx: StateFlow<T>` getters (no inherited members redeclared)

### Implementation for User Story 1

- [ ] T017 [P] [US1] Create `ModuleRuntimeSpec`, `NodeRegistryEntry`, `TypedStateFlow` data classes in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/ModuleRuntimeSpec.kt` per `data-model.md` §2
- [ ] T018 [US1] Create `ModuleRuntimeGenerator` class in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/ModuleRuntimeGenerator.kt` with `fun generate(spec: ModuleRuntimeSpec): String` emitting the template in `data-model.md` §4 / `research.md` Decision 5; depends on T017
- [ ] T019 [US1] Modify `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeControllerInterfaceGenerator.kt` to (a) emit `import io.codenode.fbpdsl.runtime.ModuleController`, (b) emit `interface X : ModuleController { ... }` superinterface clause, (c) drop redeclarations of `executionState`, `start`, `stop`, `pause`, `resume`, `reset` from the body (keep only typed `val xxx: StateFlow<T>` getters)
- [ ] T020 [US1] Run `./gradlew :flowGraph-generate:jvmTest --tests '*ModuleRuntimeGeneratorTest*' --tests '*RuntimeControllerInterfaceGeneratorTest*'` and confirm T012–T016 now pass
- [ ] T021 [US1] Modify `ModuleGenerator` orchestrator at `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/ModuleGenerator.kt` to (a) build a `ModuleRuntimeSpec` from the existing per-module spec, (b) invoke `ModuleRuntimeGenerator` once per module emitting `{moduleRoot}/src/commonMain/kotlin/{packagePath}/{Module}Runtime.kt`, (c) remove all call sites that invoke `RuntimeControllerGenerator`, `RuntimeControllerAdapterGenerator`, `RuntimeFlowGenerator`, (d) schedule deletion of `controller/{Module}Controller.kt`, `controller/{Module}ControllerAdapter.kt`, `flow/{Module}Flow.kt` for every module being regenerated
- [ ] T022 [US1] Implement the deletion safety check in the orchestrator: before deleting any scheduled file, read its first ~5 lines and confirm the `Generated by CodeNodeIO` marker is present; if absent, record a `SKIPPED_CONFLICT` `FileChange` and do NOT delete (per FR-013)
- [ ] T023 [US1] Run `./gradlew :flowGraph-generate:jvmTest` and confirm green
- [ ] T024 [P] [US1] Regenerate `StopWatch` module: trigger module regeneration via `ModuleGenerator` against `/Users/dhaukoos/CodeNodeIO-DemoProject/StopWatch`; confirm the structured summary lists `StopWatchRuntime.kt` CREATED, `StopWatchControllerInterface.kt` UPDATED, the trio DELETED; then run `./gradlew :StopWatch:compileKotlinJvm` from `/Users/dhaukoos/CodeNodeIO-DemoProject` and confirm clean compile
- [ ] T025 [P] [US1] Regenerate `Addresses` module similarly; verify the regenerated `AddressesRuntime.kt` references the `AddressDao` injection path correctly (note: Addresses' `ViewModel` takes a `(ControllerInterface, AddressDao)` constructor — confirm the regenerated `Runtime.kt` still works with this two-arg ViewModel via the consumer's existing `Persistence.dao` resolution); compile via `./gradlew :Addresses:compileKotlinJvm`
- [ ] T026 [P] [US1] Regenerate `UserProfiles` module similarly; compile via `./gradlew :UserProfiles:compileKotlinJvm`
- [ ] T027 [P] [US1] Regenerate `EdgeArtFilter` module similarly; compile via `./gradlew :EdgeArtFilter:compileKotlinJvm`
- [ ] T028 [P] [US1] Regenerate `WeatherForecast` module similarly (note: WeatherForecast had no thick stack to delete; only changes are `WeatherForecastRuntime.kt` CREATED and `WeatherForecastControllerInterface.kt` UPDATED to add `: ModuleController` plus the typed state-flow getters); compile via `./gradlew :WeatherForecast:compileKotlinJvm`
- [ ] T029 [US1] Verify SC-001 line-count drop: `find /Users/dhaukoos/CodeNodeIO-DemoProject/{StopWatch,Addresses,UserProfiles,EdgeArtFilter,WeatherForecast}/src/commonMain -type f \( -name "*Controller.kt" -o -name "*ControllerAdapter.kt" -o -name "*Flow.kt" \) | grep -v ControllerInterface | grep -v "\.flow\.kt"` returns empty output

**Checkpoint**: Generated-module shape now matches the universal-runtime contract; all five reference modules compile. KMPMobileApp will NOT yet compile against them — that's US2.

---

## Phase 4: User Story 2 — KMPMobileApp works end-to-end against the collapsed modules (Priority: P1)

**Goal**: KMPMobileApp's call sites and integration tests are migrated to the new instantiation pattern; the app builds, the tests pass, and primary user journeys (StopWatch tab + UserProfiles tab) work end-to-end.

**Independent Test**: Build KMPMobileApp; run its existing integration test suite; manually exercise both tabs on an Android emulator/device and confirm parity with pre-collapse behavior.

**Critical**: Per FR-014 and the Foundational checkpoint, US2 must land in the same change set as US1 — the main branch is never observed in a state where modules are collapsed but KMPMobileApp still references the eliminated classes.

### Implementation for User Story 2

- [ ] T030 [US2] Update `/Users/dhaukoos/CodeNodeIO-DemoProject/KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/App.kt`: change imports at lines 23-30 from `io.codenode.stopwatch.controller.StopWatchController` / `StopWatchControllerAdapter` and `io.codenode.userprofiles.controller.UserProfilesController` / `UserProfilesControllerAdapter` to `io.codenode.stopwatch.createStopWatchRuntime` and `io.codenode.userprofiles.createUserProfilesRuntime`
- [ ] T031 [US2] In same `App.kt` lines 56-62, replace `StopWatchController(stopWatchFlowGraph)` with `createStopWatchRuntime(stopWatchFlowGraph)` and replace `StopWatchViewModel(StopWatchControllerAdapter(stopWatchController))` with `StopWatchViewModel(stopWatchController)` (the typed `StopWatchControllerInterface` returned by the factory satisfies the ViewModel constructor directly)
- [ ] T032 [US2] In same `App.kt` lines 64-72, apply the equivalent change for UserProfiles: `createUserProfilesRuntime(userProfilesFlowGraph)` and `UserProfilesViewModel(userProfilesController, UserProfilesPersistence.dao)`
- [ ] T033 [US2] Update `/Users/dhaukoos/CodeNodeIO-DemoProject/KMPMobileApp/src/androidMain/kotlin/io/codenode/mobileapp/StopWatchPreview.kt` (lines 9, 16): change import + `StopWatchController(stopWatchFlowGraph)` to `createStopWatchRuntime(stopWatchFlowGraph)`
- [ ] T034 [US2] Update `/Users/dhaukoos/CodeNodeIO-DemoProject/KMPMobileApp/src/androidUnitTest/kotlin/io/codenode/mobileapp/StopWatchIntegrationTest.kt`: change the import at line 10 to `io.codenode.stopwatch.createStopWatchRuntime`; replace every `StopWatchController(flowGraph)` instantiation (lines ~123, 141, 155, 172, 188, and any others) with `createStopWatchRuntime(flowGraph)`; the `controller.getStatus()` call at line 375 continues to work because `StopWatchControllerInterface` now inherits `getStatus()` from `ModuleController`
- [ ] T035 [US2] Build KMPMobileApp Android target: from `/Users/dhaukoos/CodeNodeIO-DemoProject` run `./gradlew :KMPMobileApp:assembleDebug` and confirm clean build
- [ ] T036 [US2] Run KMPMobileApp's integration tests: `./gradlew :KMPMobileApp:testDebugUnitTest --tests '*StopWatchIntegrationTest*'` and confirm every test passes (specifically the `controller_getStatus_returns_FlowExecutionStatus` test at line 369-378)
- [ ] T037 [US2] Manual end-to-end check (Android emulator or device): launch KMPMobileApp, exercise StopWatch tab (start/pause/resume/stop/reset) and UserProfiles tab (add/edit/delete a profile); confirm behavior matches pre-collapse (per quickstart.md VS-D4)
- [ ] T038 [US2] Verify import-surface migration is complete: `grep -rn "StopWatchController(\|UserProfilesController(\|StopWatchControllerAdapter\|UserProfilesControllerAdapter" /Users/dhaukoos/CodeNodeIO-DemoProject/KMPMobileApp/src` returns empty output

**Checkpoint**: Production-app path validated. The collapse is functionally complete: modules are minimal AND deployable. Remaining stories are confidence/regression checks and cleanup.

---

## Phase 5: User Story 3 — Runtime Preview behavior unchanged (Priority: P1)

**Goal**: Confirm the GraphEditor's Runtime Preview behaves identically pre- and post-collapse for every reference module. This is a regression-prevention checkpoint, gated on US1 (modules must be regeneratable first).

**Independent Test**: For each of the five reference modules, open Runtime Preview in the GraphEditor and walk through Start/Stop/Pause/Resume/Reset, attenuation, observers, and a representative data-flow scenario; confirm zero behavioral regression.

### Tests for User Story 3 ⚠️

- [ ] T039 [P] [US3] Write integration test `runtime_preview_session_creates_for_StopWatch` in `/Users/dhaukoos/CodeNodeIO/flowGraph-execute/src/jvmTest/kotlin/io/codenode/flowgraphexecute/ModuleSessionFactoryRegressionTest.kt` (create file) asserting `ModuleSessionFactory.createSession("StopWatch", ...)` returns a non-null `RuntimeSession` whose ViewModel is castable to `io.codenode.stopwatch.viewmodel.StopWatchViewModel` (exercise via classpath fixture)
- [ ] T040 [P] [US3] Add equivalent assertions in the same `ModuleSessionFactoryRegressionTest.kt` for the other four modules (Addresses, UserProfiles, EdgeArtFilter, WeatherForecast)
- [ ] T041 [P] [US3] Add an additional assertion in the same file: invoking `getStatus()` on the proxy-implemented `{Module}ControllerInterface` returns a non-null `FlowExecutionStatus` (validates the `ModuleSessionFactory.createControllerProxy` `"getStatus"` case added in T010)

### Implementation / Verification for User Story 3

- [ ] T042 [US3] Run `./gradlew :flowGraph-execute:jvmTest --tests '*ModuleSessionFactoryRegressionTest*'` and confirm T039–T041 pass
- [ ] T043 [US3] Manual quickstart VS-C1: launch the GraphEditor, open StopWatch in Runtime Preview, exercise Start/Pause/Resume/Stop/Reset, confirm behavior matches pre-collapse
- [ ] T044 [P] [US3] Manual quickstart VS-C1 for Addresses (run alongside T043 if multiple GraphEditor instances are used)
- [ ] T045 [P] [US3] Manual quickstart VS-C1 for UserProfiles
- [ ] T046 [P] [US3] Manual quickstart VS-C1 for EdgeArtFilter
- [ ] T047 [P] [US3] Manual quickstart VS-C1 for WeatherForecast
- [ ] T048 [US3] Manual quickstart VS-C2 (regression checks): for at least StopWatch and Addresses, verify dataflow animation, attenuation slider, and per-port data preview behave identically to pre-collapse

**Checkpoint**: Runtime Preview parity confirmed. SC-005 met.

---

## Phase 6: User Story 4 — Generator output for new modules is materially simpler (Priority: P2)

**Goal**: Confirm a fresh module created via the existing module-creation flow emits at least three fewer files than today, and the emitted output is small enough to read in a single sitting.

**Independent Test**: Create a new module via the GraphEditor's Create New Module flow; count the generated files; confirm the count is at least three lower than equivalent pre-collapse generation.

### Implementation / Verification for User Story 4

- [ ] T049 [US4] Create a new fresh entity-style module via the GraphEditor's "Create New Module..." flow in a scratch location; record the list of files emitted by the generator (use the structured summary from T021)
- [ ] T050 [US4] Verify the emitted set contains `{Module}Runtime.kt`, `{Module}ControllerInterface.kt`, `{Module}State.kt`, `{Module}ViewModel.kt`, nodes/, `.flow.kt`, and `{Module}PreviewProvider.kt`, but does NOT contain `{Module}Controller.kt`, `{Module}ControllerAdapter.kt`, or `{Module}Flow.kt` runtime
- [ ] T051 [US4] Verify SC-006 quantitatively: the file count is at least 3 lower than the pre-collapse equivalent for the same module type
- [ ] T052 [US4] Read every emitted file end-to-end and confirm each is either trivially small (≤ ~30 lines of declarative wiring, e.g., `{Module}Runtime.kt`) or genuinely module-specific (no copy-pasta-able boilerplate); record outcome

**Checkpoint**: New-module developer experience confirmed simpler. SC-006 met.

---

## Phase 7: User Story 5 — Deprecated generator surface is removed cleanly (Priority: P3)

**Goal**: The three deprecated generators and their unit tests are removed from the codebase; no live invocation path references them; orchestration uses only the new generator + the kept interface generator.

**Independent Test**: Grep the repo for the three deprecated generator class names; confirm absence (or only clearly-archival references). Run the full test suite and confirm no test depends on the removed generators.

### Implementation for User Story 5

- [ ] T053 [P] [US5] Delete `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeControllerGenerator.kt`
- [ ] T054 [P] [US5] Delete `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeControllerAdapterGenerator.kt`
- [ ] T055 [P] [US5] Delete `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeFlowGenerator.kt`
- [ ] T056 [P] [US5] Delete the corresponding unit test files: `RuntimeControllerGeneratorTest.kt`, `RuntimeControllerAdapterGeneratorTest.kt`, `RuntimeFlowGeneratorTest.kt` from `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/`
- [ ] T057 [US5] Verify absence: `find /Users/dhaukoos/CodeNodeIO/flowGraph-generate/src -name 'RuntimeControllerGenerator.kt' -o -name 'RuntimeControllerAdapterGenerator.kt' -o -name 'RuntimeFlowGenerator.kt' -o -name 'RuntimeControllerGeneratorTest.kt' -o -name 'RuntimeControllerAdapterGeneratorTest.kt' -o -name 'RuntimeFlowGeneratorTest.kt'` returns empty output
- [ ] T058 [US5] Verify no live references remain: `grep -rn "RuntimeControllerGenerator\|RuntimeControllerAdapterGenerator\|RuntimeFlowGenerator" /Users/dhaukoos/CodeNodeIO/flowGraph-generate/src --include='*.kt'` returns empty (or only clearly-archival CHANGELOG comments)
- [ ] T059 [US5] Verify the orchestrator doesn't invoke the deleted generators: `grep -n "ModuleRuntimeGenerator\|RuntimeControllerInterfaceGenerator\|RuntimeControllerGenerator\|RuntimeControllerAdapterGenerator\|RuntimeFlowGenerator" /Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/ModuleGenerator.kt` shows references ONLY to `ModuleRuntimeGenerator` and `RuntimeControllerInterfaceGenerator`
- [ ] T060 [US5] Run full project test suite from `/Users/dhaukoos/CodeNodeIO`: `./gradlew test` (or the equivalent multi-module test target); confirm no test failure attributable to the removal

**Checkpoint**: Generator codebase cleanup complete. SC-007 met.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Hand-edit safety verification, full quickstart pass, documentation consumer-facing reference per SC-008, and the unblocking handoff to feature 084.

- [ ] T061 [P] Quickstart VS-F (FR-013 hand-edit safety synthetic test): per `/Users/dhaukoos/CodeNodeIO/specs/085-collapse-thick-runtime/quickstart.md` VS-F, fabricate an unmarked file at a generator-target path, re-run regeneration, confirm the orchestrator refuses to delete it and emits a structured `SKIPPED_CONFLICT` warning naming the file; restore the workspace afterward
- [ ] T062 [P] Quickstart VS-E (deprecated-generator absence): per `/Users/dhaukoos/CodeNodeIO/specs/085-collapse-thick-runtime/quickstart.md` VS-E1, VS-E2, VS-E3, run the grep/find checks and confirm all return the expected empty/restricted output
- [ ] T063 [P] Quickstart VS-A: re-run `./gradlew :fbpDsl:jvmTest --tests '*DynamicPipelineControllerTest*'` and inspect the new `getStatus`-related signatures via `grep -n "fun getStatus" /Users/dhaukoos/CodeNodeIO/fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/{ModuleController,DynamicPipelineController,DynamicPipeline}.kt`
- [ ] T064 [P] Author the consumer-facing reference for the new instantiation pattern (per SC-008) in `/Users/dhaukoos/CodeNodeIO/specs/085-collapse-thick-runtime/quickstart.md` (extend the existing VS-D section with a "Production-app integration template" subsection showing a complete `main()` consuming any of the five reference modules); a new developer should be able to write a working production-app instantiation in under 30 minutes from this template alone
- [ ] T065 Full quickstart sweep: walk through every VS in `/Users/dhaukoos/CodeNodeIO/specs/085-collapse-thick-runtime/quickstart.md` (VS-A through VS-F) end-to-end as a single coordinated pass; record any deviations from expected outcomes
- [ ] T066 Verify spec SC-001–SC-009 are all met: line-count drop (SC-001/002), clean compile (SC-003), KMPMobileApp parity (SC-004), Runtime Preview parity (SC-005), file-count drop for new modules (SC-006), generator removal (SC-007), consumer-facing template (SC-008), and feature-084 unblock readiness (SC-009)
- [ ] T067 Update feature 084's spec status: in `/Users/dhaukoos/CodeNodeIO/specs/084-ui-fbp-runtime-preview/spec.md`, change the `Status: On Hold pending feature 085` line and the HOLD NOTICE block to reflect that feature 085 has shipped and 084 is now ready for `/speckit.clarify` + `/speckit.plan` re-runs against the universal runtime; cite the relevant new contracts (`{Module}Runtime.kt` factory, `ModuleController` interface inheritance) so the resumption has a clear starting point

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — confirms baseline
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories. Adds `getStatus()` to fbpDsl + the `ModuleSessionFactory` proxy update
- **User Story 1 (Phase 3)**: Depends on Foundational. Generator changes + reference-module regeneration
- **User Story 2 (Phase 4)**: Depends on User Story 1 (cannot migrate KMPMobileApp's imports until the modules have been regenerated to expose the new factory functions). Per FR-014 these MUST land atomically with US1 in the same change set
- **User Story 3 (Phase 5)**: Depends on User Story 1 (cannot validate Runtime Preview behavior on the new shape until modules are regenerated). Independent of US2 — Runtime Preview doesn't touch KMPMobileApp
- **User Story 4 (Phase 6)**: Depends on User Story 1. Independent of US2/US3
- **User Story 5 (Phase 7)**: Depends on User Story 1 (the orchestrator must already not invoke the deprecated generators before they can be safely deleted)
- **Polish (Phase 8)**: Depends on US1–US5 complete

### Within Each User Story

- Tests written first (where applicable) and confirmed to fail before implementation begins
- Generator changes (US1) must be complete before module regeneration tasks run
- Module regeneration is parallelizable across modules (T024–T028 all marked [P])
- KMPMobileApp updates (US2) sequence: imports → instantiations → build → tests → manual check → grep verification
- Generator deletions (US5) are parallelizable (T053–T056)

### Parallel Opportunities

- T004, T005 — additional `getStatus` test cases (different test methods, same file body added together)
- T012–T016 — five generator-output unit-test cases (within two test files, written together)
- T024–T028 — five module regenerations (different module directories)
- T039–T041 — three regression-test assertions in the same file (written together)
- T044–T047 — four manual Runtime Preview verifications (different module sessions)
- T053–T056 — four file deletions (independent files)
- T061, T062, T063, T064 — Polish-phase verification tasks (independent files)

---

## Parallel Example: Phase 3 module regenerations

Once T021 (orchestrator update) and T023 (generator-tests green) are complete, the five module regeneration tasks can run in parallel:

```bash
# Five independent regenerations, one per module
Task: "T024 Regenerate StopWatch module and compile"
Task: "T025 Regenerate Addresses module and compile"
Task: "T026 Regenerate UserProfiles module and compile"
Task: "T027 Regenerate EdgeArtFilter module and compile"
Task: "T028 Regenerate WeatherForecast module and compile"
```

---

## Implementation Strategy

### MVP First (US1 + US2 atomic)

Per FR-014, US1 (module regeneration) and US2 (KMPMobileApp migration) MUST land atomically in the same change set. Treat them as a single MVP:

1. Complete Phase 1 (Setup baseline)
2. Complete Phase 2 (Foundational — `getStatus()` + proxy update)
3. Complete Phase 3 (US1 — generators + module regeneration)
4. Complete Phase 4 (US2 — KMPMobileApp migration)
5. **STOP and VALIDATE**: Phase 5 (US3) Runtime Preview parity check confirms no regression
6. The MVP is now shippable: modules are collapsed AND production app works

### Incremental Delivery After MVP

7. Phase 6 (US4) — confirm new-module DX improvement
8. Phase 7 (US5) — delete the deprecated generators
9. Phase 8 (Polish) — quickstart sweep + author consumer-facing reference + unblock feature 084

### Parallel Team Strategy

With multiple developers, after Phase 2:

- Developer A: Phase 3 (US1) generator work + reference-module regenerations
- Developer B (after A finishes T021–T023): Phase 4 (US2) KMPMobileApp migration
- Developer C: Phase 5 (US3) Runtime Preview regression test + manual verifications
- Developer A or D (after Phase 3 finishes): Phase 7 (US5) generator deletion

US1 and US2 must merge together (per FR-014); the rest can ship as independent PRs in priority order.

---

## Notes

- [P] tasks = different files or independent units of work; no dependencies on incomplete tasks
- [Story] label maps each task to its priority story for traceability
- US1 and US2 share the FR-014 atomicity constraint — do not merge US1 alone to main
- US3 (Runtime Preview parity) is the regression checkpoint — run it before declaring US1 done
- Verify foundational tests fail before implementing T006–T008 (per Constitution principle II)
- Commit after each task or logical group; the `Generated by` marker comments make per-file diffs self-documenting
- Stop at any checkpoint to validate independently
- Avoid cross-story dependencies that break independent testability (only US1→US2 atomicity is enforced)
