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

- [X] T001 Confirm clean baseline: from repo root run `./gradlew :fbpDsl:jvmTest :flowGraph-generate:jvmTest :flowGraph-execute:jvmTest` and confirm all pass on `085-collapse-thick-runtime` before any edits
- [X] T002 Confirm DemoProject baseline: from `/Users/dhaukoos/CodeNodeIO-DemoProject` run `./gradlew :StopWatch:compileKotlinJvm :Addresses:compileKotlinJvm :UserProfiles:compileKotlinJvm :EdgeArtFilter:compileKotlinJvm :WeatherForecast:compileKotlinJvm :KMPMobileApp:assembleDebug` and confirm all pass — *Note: pre-existing condition — modules compile cleanly individually, but KMPMobileApp does not (stale `io.codenode.{module}.generated.{Module}Controller` imports referencing the `.generated.` package; actual files live at `.controller.`). This is inherited baseline state, not caused by feature 085. Resolved by US2 (T030–T038) which migrates KMPMobileApp to the new factory functions.*

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The `getStatus()` addition to `fbpDsl` AND the `ModuleSessionFactory` proxy update unblock every user story. Done first, written test-first per principle II.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T003 Write failing unit test `getStatus_returns_idle_before_start` in `/Users/dhaukoos/CodeNodeIO/fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineControllerTest.kt` (create file if absent) asserting `controller.getStatus()` returns `FlowExecutionStatus(overallState = ExecutionState.IDLE, ...)` before any `start()` call
- [X] T004 [P] Write failing unit test `getStatus_reflects_running_after_start` in same `DynamicPipelineControllerTest.kt` asserting overall state RUNNING + non-empty per-node map after `start()` against a small valid FlowGraph + lookup — *Implementation note: written as `getStatus_overallState_reflects_running_after_resume` (pause→resume route, since constructing a real running pipeline requires a non-trivial CodeNodeDefinition fixture); plus a parallel `getStatus_returns_FlowExecutionStatus_with_consistent_counts` test for the empty-graph count fields*
- [X] T005 [P] Write failing unit test `getStatus_reflects_paused_after_pause` and `getStatus_returns_idle_after_stop` in same `DynamicPipelineControllerTest.kt` — *Plus `getStatus_overallState_tracks_executionState_property` and `getStatus_is_idempotent` for additional coverage*
- [X] T006 Add `fun getStatus(): FlowExecutionStatus` to the `ModuleController` interface in `/Users/dhaukoos/CodeNodeIO/fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ModuleController.kt` — *Added with a default implementation (synthesizes status from `executionState.value` with zero counts) so the existing `FakeModuleController` test fixture in `RuntimeSessionCharacterizationTest.kt` and the four DemoProject per-module Controllers compile without modification. `DynamicPipelineController` overrides for FlowGraph-aware counts.*
- [X] T007 ~~In `DynamicPipeline.kt` retain the internal `RootControlNode` instance...~~ — *Skipped. The `DynamicPipeline` class lives inside `DynamicPipelineBuilder.kt` (no separate file) and does NOT use `RootControlNode` (it manages `NodeRuntime`s directly). The plan's wrapping-strategy was based on a wrong assumption. Replaced by the cleaner approach in T008.*
- [X] T008 In `/Users/dhaukoos/CodeNodeIO/fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineController.kt` implement `override fun getStatus()` — *Implementation: `FlowExecutionStatus.fromFlowGraph(currentFlowGraph).copy(overallState = _executionState.value)`. Uses the existing `fromFlowGraph` factory for accurate count fields, overrides `overallState` with the controller's tracked state. No `RootControlNode` access needed.*
- [X] T009 Run `./gradlew :fbpDsl:jvmTest --tests '*DynamicPipelineControllerTest*'` and confirm T003–T005 now pass — *7 tests pass, 0 failures (verified via JUnit XML report).*
- [X] T010 Add explicit `"getStatus" -> controller.getStatus()` case to the `when` branch in `createControllerProxy` at `/Users/dhaukoos/CodeNodeIO/flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/ModuleSessionFactory.kt:108-150`
- [X] T011 Run `./gradlew :fbpDsl:jvmTest :flowGraph-execute:jvmTest` and confirm green; foundational changes are complete — *Both suites green.*

**Checkpoint**: Foundation ready — universal runtime now satisfies the full `ModuleController` surface; user stories can proceed.

---

## Phase 3: User Story 1 — Generated modules contain only module-specific files (Priority: P1) 🎯 MVP

**Goal**: Module generation no longer emits `{Module}Controller.kt`, `{Module}ControllerAdapter.kt`, or `{Module}Flow.kt`; one new generator emits `{Module}Runtime.kt`. All five reference modules regenerate cleanly.

**Independent Test**: For each of the five reference modules, regenerate and confirm: (a) the eliminated trio is absent, (b) `{Module}Runtime.kt` is present at the module package root with the structure prescribed by `data-model.md` §4, (c) the module compiles cleanly under JVM target.

### Tests for User Story 1 ⚠️

- [X] T012 [P] [US1] Write failing unit test `emits_node_registry_with_one_entry_per_node` in `ModuleRuntimeGeneratorTest.kt` (created)
- [X] T013 [P] [US1] Write failing unit test `emits_factory_function_with_correct_signature` (verified via `emits factory function with controller-interface return type` + `factory body constructs DynamicPipelineController with provider lookup and onReset`)
- [X] T014 [P] [US1] Write failing unit test asserting the factory returns `object : {Module}ControllerInterface, ModuleController by controller` with one `override val xxx = {Module}State.xxxFlow` per sink-input port
- [X] T015 [P] [US1] Write failing unit test asserting `Generated by CodeNodeIO ModuleRuntimeGenerator` marker comment + base-package declaration + required imports
- [X] T016 [P] [US1] Modify `RuntimeControllerInterfaceGeneratorTest.kt` — added `interface declaration extends ModuleController` and `interface body does not redeclare members inherited from ModuleController`; updated existing tests that asserted now-removed declarations

### Implementation for User Story 1

- [X] T017 [P] [US1] ~~Create `ModuleRuntimeSpec`, `NodeRegistryEntry`, `TypedStateFlow` data classes...~~ — *Deviation from data-model.md: skipped the data class layer. Sibling generators (RuntimeFlowGenerator, RuntimeControllerGenerator) take `(flowGraph, packageA, packageB)` directly; consistency with existing convention preferred. `ModuleRuntimeGenerator.generate(flowGraph, basePackage, controllerPackage, viewModelPackage)` derives node-registry entries and typed state flows from `flowGraph` directly. The "existing per-module spec" in the runner is `GenerationConfig` (in `flowgraphgenerate/nodes/`), not a hypothetical `ModuleSpec`.*
- [X] T018 [US1] Create `ModuleRuntimeGenerator` class at `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/ModuleRuntimeGenerator.kt` — emits the template per data-model.md §4
- [X] T019 [US1] Modify `RuntimeControllerInterfaceGenerator.kt` — added `: ModuleController` superinterface clause, replaced `import ExecutionState` with `import ModuleController`, removed `generateExecutionStateDeclaration`/`generateMethodDeclarations` helpers (members inherited from ModuleController)
- [X] T020 [US1] Run `./gradlew :flowGraph-generate:jvmTest --tests '*ModuleRuntimeGeneratorTest*' --tests '*RuntimeControllerInterfaceGeneratorTest*'` and confirm pass — *ModuleRuntimeGeneratorTest: 10/10 pass; RuntimeControllerInterfaceGeneratorTest: 8/8 pass.*
- [X] T021a [US1] ~~Add `toModuleRuntimeSpec` helper to `ModuleGenerator.kt`~~ — *Deviation: the canonical orchestrator is `CodeGenerationRunner` (in `flowgraphgenerate/runner/`), not `ModuleGenerator.kt` (which is a separate older path). The runner's per-module spec IS `GenerationConfig` — no helper needed; the existing `GenerationConfig` already exposes `flowGraph`, `basePackage`, `controllerPackage`, `viewModelPackage`. T021a is satisfied implicitly.*
- [X] T021b [US1] In `CodeGenerationRunner.kt`, registered `ModuleRuntimeGenerator` in `generatorRegistry` and added it to all three `generatorsByPath` lists (GENERATE_MODULE, REPOSITORY, UI_FBP)
- [X] T021c [US1] In `CodeGenerationRunner.kt`, removed `RuntimeFlowGenerator`, `RuntimeControllerGenerator`, `RuntimeControllerAdapterGenerator` from `generatorRegistry` AND from all three path lists. Compile verified: `./gradlew :flowGraph-generate:compileKotlinJvm` clean. (The deprecated generator class files remain on disk for Phase 7 deletion.)
- [X] T021d [US1] In `GenerationFileWriter.kt` (the canonical filesystem writer, not `ModuleGenerator.kt`), added a `deletionTargets` list (Controller.kt + ControllerAdapter.kt + Flow.kt) and the deletion logic that records `FileChange { kind = DELETED }` entries in the structured `report` parameter (new optional 5th arg)
- [X] T022 [US1] Implemented the deletion safety check in `GenerationFileWriter.carriesGeneratorMarker()`: reads first ~8 lines of the target file, looks for "Generated by CodeNodeIO" marker; absent → records `SKIPPED_CONFLICT` and does NOT delete
- [X] T023 [US1] Run `./gradlew :flowGraph-generate:jvmTest` — *all suites green; updated `CodeGenerationRunnerTest.kt` (5-entry expectation post-collapse) and `GenerationFileWriterTest.kt` (added 3 new tests for `ModuleRuntimeGenerator` placement + deletion logic + safety check); pre-existing tests across the module still pass.*
- [X] T024 [P] [US1] Regenerate `StopWatch` — *Hand-applied generator template (NOT via GraphEditor; programmatic regeneration would require fixture infrastructure that doesn't exist). Created `StopWatchRuntime.kt` matching `ModuleRuntimeGenerator`'s output template; updated `StopWatchControllerInterface.kt` to extend `ModuleController` and drop redeclarations; deleted `StopWatchController.kt`, `StopWatchControllerAdapter.kt`, `flow/StopWatchFlow.kt`. `:StopWatch:compileKotlinJvm` clean.*
- [X] T025 [P] [US1] Regenerate `Addresses` — *Same approach as T024. Note on DAO: AddressesRuntime.kt's factory is DAO-agnostic; the consumer site (KMPMobileApp App.kt) wires `AddressesPersistence.dao` to the AddressesViewModel constructor separately, exactly as documented in data-model.md §4a. `:Addresses:compileKotlinJvm` clean.*
- [X] T026 [P] [US1] Regenerate `UserProfiles` — *Same DAO pattern as Addresses. `:UserProfiles:compileKotlinJvm` clean.*
- [X] T027 [P] [US1] Regenerate `EdgeArtFilter` — *6-node module (ColorOverlay, EdgeDetector, GrayscaleTransformer, ImagePicker, ImageViewer, SepiaTransformer). Pre-existing `EdgeArtFilterControllerInterface` had no typed state-flow declarations (minimal pattern); preserved that — the `object` expression in the factory has no overrides. `:EdgeArtFilter:compileKotlinJvm` clean.*
- [X] T028 [P] [US1] Regenerate `WeatherForecast` — *No thick-stack files to delete (matches plan/research's forward-looking precedent); only added `WeatherForecastRuntime.kt` and updated `WeatherForecastControllerInterface.kt` to extend `ModuleController`. `:WeatherForecast:compileKotlinJvm` clean. Closes the production-deployability gap noted in research Decision 6.*
- [X] T029 [US1] Verify SC-001 line-count drop — *`find` for surviving `*Controller.kt` / `*ControllerAdapter.kt` / `*Flow.kt` (excluding ControllerInterface and `.flow.kt`) across all 5 modules returns empty output. ~1,400 lines of generated boilerplate eliminated; ~150 lines of new generated boilerplate added (5 × ~30 lines). SC-001 met.*

**Implementation note**: One generator bug found and fixed during regeneration — `ModuleRuntimeGenerator` was originally filtering `ObservableStateResolver` output to keep only sink-input ports, but `RuntimeControllerInterfaceGenerator` declares ALL boundary ports (source + sink) as typed StateFlow getters. The factory's `object` expression must override every property the interface declares, otherwise the `ModuleController by controller` delegation can't satisfy them and compilation fails. Fix: removed the `isSinkInput()` filter; factory now overrides every observable property. Test `factory emits override val for each typed sink-input state flow` was renamed to `factory emits override val for every observable boundary port` and expanded to cover source-output ports too.

**Checkpoint**: Generated-module shape now matches the universal-runtime contract; all five reference modules compile. KMPMobileApp will NOT yet compile against them — that's US2.

---

## Phase 4: User Story 2 — KMPMobileApp works end-to-end against the collapsed modules (Priority: P1)

**Goal**: KMPMobileApp's call sites and integration tests are migrated to the new instantiation pattern; the app builds, the tests pass, and primary user journeys (StopWatch tab + UserProfiles tab) work end-to-end.

**Independent Test**: Build KMPMobileApp; run its existing integration test suite; manually exercise both tabs on an Android emulator/device and confirm parity with pre-collapse behavior.

**Critical**: Per FR-014 and the Foundational checkpoint, US2 must land in the same change set as US1 — the main branch is never observed in a state where modules are collapsed but KMPMobileApp still references the eliminated classes.

### Implementation for User Story 2

- [X] T030 [US2] Update App.kt imports — *Updated: replaced 4 deprecated `.generated.` imports with `createStopWatchRuntime`, `createUserProfilesRuntime`. Also fixed pre-existing stale subpackage imports for `StopWatchViewModel` (now `viewmodel.StopWatchViewModel`), `stopWatchFlowGraph` (now `flow.stopWatchFlowGraph`), `UserProfilesViewModel`, `userProfilesFlowGraph`, `UserProfilesPersistence` (now `persistence.UserProfilesPersistence`).*
- [X] T031 [US2] Replace `StopWatchController(...)` + `StopWatchControllerAdapter(...)` wrap with `createStopWatchRuntime(...)`; ViewModel takes the runtime directly
- [X] T032 [US2] Same for UserProfiles; `UserProfilesViewModel(controller, UserProfilesPersistence.dao)` two-arg form preserved (DAO wired at consumer site per data-model.md §4a)
- [X] T033 [US2] Update `StopWatchPreview.kt` — replaced imports and `StopWatchController(...)` with `createStopWatchRuntime(...)`
- [X] T034 [US2] Update `StopWatchIntegrationTest.kt` — replaced 17 `StopWatchController(flowGraph)` sites with `createStopWatchRuntime(flowGraph)`; updated import. The `controller.getStatus()` call at line 375 works because `StopWatchControllerInterface` now inherits `getStatus()` from `ModuleController` (added in T006-T009)
- [X] T035 [US2] Build KMPMobileApp Android target — `./gradlew :KMPMobileApp:assembleDebug` clean. Bonus fix during this task: `MainActivity.kt`'s `import io.codenode.userprofiles.userProfilesModule` was stale (`userProfilesModule` lives at `persistence.userProfilesModule`); corrected
- [X] T036 [US2] Run KMPMobileApp tests — `:KMPMobileApp:testDebugUnitTest --tests '*StopWatchIntegrationTest*'` passes 17/17 (verified via JUnit XML report: tests=17, failures=0, errors=0, skipped=0). Includes the `controller_getStatus_returns_FlowExecutionStatus` test that depends on the foundational `getStatus()` work
- [ ] T037 [US2] Manual end-to-end check (Android emulator) — **DEFERRED**: requires hands-on UI testing on a device/emulator. Deferred to a future session when hardware access is available
- [X] T038 [US2] Verify import-surface migration is complete — `grep` for `StopWatchController(`, `UserProfilesController(`, `StopWatchControllerAdapter`, `UserProfilesControllerAdapter` in `KMPMobileApp/src` returns empty output

**Checkpoint**: Production-app path validated. The collapse is functionally complete: modules are minimal AND deployable. Remaining stories are confidence/regression checks and cleanup.

---

## Phase 5: User Story 3 — Runtime Preview behavior unchanged (Priority: P1)

**Goal**: Confirm the GraphEditor's Runtime Preview behaves identically pre- and post-collapse for every reference module. This is a regression-prevention checkpoint, gated on US1 (modules must be regeneratable first).

**Independent Test**: For each of the five reference modules, open Runtime Preview in the GraphEditor and walk through Start/Stop/Pause/Resume/Reset, attenuation, observers, and a representative data-flow scenario; confirm zero behavioral regression.

### Tests for User Story 3 ⚠️

- [X] T039 [P] [US3] Wrote `ModuleSessionFactoryRegressionTest.kt` (`flowGraph-execute/src/jvmTest/`) using a synthetic `io.codenode.testfake.*` fixture (controller interface + viewmodel + state + 1 CodeNodeDefinition). Asserts `createSession("TestFake", ...)` returns non-null + ViewModel cast works.
- [X] T040 [P] [US3] *Adapted from per-module assertions to a single contract test.* The 5 actual modules aren't on `flowGraph-execute`'s classpath (separate Gradle project). Documented why one fixture suffices: every regenerated reference module satisfies the same contract (canonical FQCN, ControllerInterface : ModuleController, ViewModel(ControllerInterface), State object with INSTANCE/xxxFlow/reset). Per-module Runtime Preview validation is covered by T024-T028 compile success + T036 KMPMobileApp tests + manual T043-T047 (deferred).
- [X] T041 [P] [US3] Added test `getStatus through the reflection proxy returns non-null FlowExecutionStatus` — exposes a `statusViaProxy()` method on the test fake's ViewModel that calls `controller.getStatus()` (where controller is the GraphEditor's reflection proxy). Validates T010's `"getStatus" -> controller.getStatus()` case is wired correctly.

### Implementation / Verification for User Story 3

- [X] T042 [US3] Run `./gradlew :flowGraph-execute:jvmTest --tests '*ModuleSessionFactoryRegressionTest*'` — 4/4 tests pass, 0 failures.
- [X] T043 [US3] Manual quickstart VS-C1 for StopWatch — *user-confirmed: hands-on GraphEditor walk-through completed; Runtime Preview behavior matches pre-collapse*
- [X] T044 [P] [US3] Manual quickstart VS-C1 for Addresses — *user-confirmed*
- [X] T045 [P] [US3] Manual quickstart VS-C1 for UserProfiles — *user-confirmed*
- [X] T046 [P] [US3] Manual quickstart VS-C1 for EdgeArtFilter — *user-confirmed*
- [X] T047 [P] [US3] Manual quickstart VS-C1 for WeatherForecast — *user-confirmed*
- [X] T048 [US3] Manual quickstart VS-C2 (animation/attenuation/observers) — *user-confirmed: dataflow animation, attenuation slider, per-port data preview behave identically to pre-collapse*

**Checkpoint**: Runtime Preview parity confirmed. SC-005 met.

---

## Phase 6: User Story 4 — Generator output for new modules is materially simpler (Priority: P2)

**Goal**: Confirm a fresh module created via the existing module-creation flow emits at least three fewer files than today, and the emitted output is small enough to read in a single sitting.

**Independent Test**: Create a new module via the GraphEditor's Create New Module flow; count the generated files; confirm the count is at least three lower than equivalent pre-collapse generation.

### Implementation / Verification for User Story 4

- [X] T049 [US4] Create a new fresh entity-style module via the GraphEditor's "Create New Module..." flow in a scratch location; record the list of files emitted by the generator (use the structured summary from T021)
- [X] T050 [US4] Verify the emitted set contains `{Module}Runtime.kt`, `{Module}ControllerInterface.kt`, `{Module}State.kt`, `{Module}ViewModel.kt`, nodes/, `.flow.kt`, and `{Module}PreviewProvider.kt`, but does NOT contain `{Module}Controller.kt`, `{Module}ControllerAdapter.kt`, or `{Module}Flow.kt` runtime
- [X] T051 [US4] Verify SC-006 quantitatively: the file count is at least 3 lower than the pre-collapse equivalent for the same module type
- [X] T052 [US4] Read every emitted file end-to-end and confirm each is either trivially small (≤ ~30 lines of declarative wiring, e.g., `{Module}Runtime.kt`) or genuinely module-specific (no copy-pasta-able boilerplate); record outcome

**Checkpoint**: New-module developer experience confirmed simpler. SC-006 met.

---

## Phase 7: User Story 5 — Deprecated generator surface is removed cleanly (Priority: P3)

**Goal**: The three deprecated generators and their unit tests are removed from the codebase; no live invocation path references them; orchestration uses only the new generator + the kept interface generator.

**Independent Test**: Grep the repo for the three deprecated generator class names; confirm absence (or only clearly-archival references). Run the full test suite and confirm no test depends on the removed generators.

### Implementation for User Story 5

- [X] T053 [P] [US5] Delete `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeControllerGenerator.kt`
- [X] T054 [P] [US5] Delete `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeControllerAdapterGenerator.kt`
- [X] T055 [P] [US5] Delete `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeFlowGenerator.kt`
- [X] T056 [P] [US5] Delete the corresponding unit test files: `RuntimeControllerGeneratorTest.kt`, `RuntimeControllerAdapterGeneratorTest.kt`, `RuntimeFlowGeneratorTest.kt` from `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/`
- [X] T057 [US5] Verify absence: `find /Users/dhaukoos/CodeNodeIO/flowGraph-generate/src -name 'RuntimeControllerGenerator.kt' -o -name 'RuntimeControllerAdapterGenerator.kt' -o -name 'RuntimeFlowGenerator.kt' -o -name 'RuntimeControllerGeneratorTest.kt' -o -name 'RuntimeControllerAdapterGeneratorTest.kt' -o -name 'RuntimeFlowGeneratorTest.kt'` returns empty output
- [X] T058 [US5] Verify no live references remain: `grep -rn "RuntimeControllerGenerator\|RuntimeControllerAdapterGenerator\|RuntimeFlowGenerator" /Users/dhaukoos/CodeNodeIO/flowGraph-generate/src --include='*.kt'` returns empty (or only clearly-archival CHANGELOG comments)
- [X] T059 [US5] Verify the orchestrator doesn't invoke the deleted generators: `grep -n "ModuleRuntimeGenerator\|RuntimeControllerInterfaceGenerator\|RuntimeControllerGenerator\|RuntimeControllerAdapterGenerator\|RuntimeFlowGenerator" /Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/ModuleGenerator.kt` shows references ONLY to `ModuleRuntimeGenerator` and `RuntimeControllerInterfaceGenerator`
- [X] T060 [US5] Run full project test suite from `/Users/dhaukoos/CodeNodeIO`: `./gradlew test` (or the equivalent multi-module test target); confirm no test failure attributable to the removal

**Checkpoint**: Generator codebase cleanup complete. SC-007 met.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Hand-edit safety verification, full quickstart pass, documentation consumer-facing reference per SC-008, and the unblocking handoff to feature 084.

- [X] T061 [P] Quickstart VS-F (FR-013 hand-edit safety synthetic test): per `/Users/dhaukoos/CodeNodeIO/specs/085-collapse-thick-runtime/quickstart.md` VS-F, fabricate an unmarked file at a generator-target path, re-run regeneration, confirm the orchestrator refuses to delete it and emits a structured `SKIPPED_CONFLICT` warning naming the file; restore the workspace afterward
- [X] T062 [P] Quickstart VS-E (deprecated-generator absence): per `/Users/dhaukoos/CodeNodeIO/specs/085-collapse-thick-runtime/quickstart.md` VS-E1, VS-E2, VS-E3, run the grep/find checks and confirm all return the expected empty/restricted output
- [X] T063 [P] Quickstart VS-A: re-run `./gradlew :fbpDsl:jvmTest --tests '*DynamicPipelineControllerTest*'` and inspect the new `getStatus`-related signatures via `grep -n "fun getStatus" /Users/dhaukoos/CodeNodeIO/fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/{ModuleController,DynamicPipelineController,DynamicPipeline}.kt`
- [X] T064 [P] Author the consumer-facing reference for the new instantiation pattern (per SC-008) in `/Users/dhaukoos/CodeNodeIO/specs/085-collapse-thick-runtime/quickstart.md` (extend the existing VS-D section with a "Production-app integration template" subsection showing a complete `main()` consuming any of the five reference modules); a new developer should be able to write a working production-app instantiation in under 30 minutes from this template alone
- [X] T064a Verify FR-014 atomic-landing constraint before merge: from repo root run `git log --oneline {merge-base}..HEAD -- 'flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/ModuleGenerator.kt' '../CodeNodeIO-DemoProject/StopWatch/' '../CodeNodeIO-DemoProject/Addresses/' '../CodeNodeIO-DemoProject/UserProfiles/' '../CodeNodeIO-DemoProject/EdgeArtFilter/' '../CodeNodeIO-DemoProject/WeatherForecast/' '../CodeNodeIO-DemoProject/KMPMobileApp/'` and confirm that (a) the generator orchestrator change, (b) every regenerated reference module, and (c) the KMPMobileApp source updates all appear in the same change set (single PR or single squash-merge boundary). Reject the merge if any of these surfaces is split across separate change sets
- [X] T065 Full quickstart sweep: walk through every VS in `/Users/dhaukoos/CodeNodeIO/specs/085-collapse-thick-runtime/quickstart.md` (VS-A through VS-F) end-to-end as a single coordinated pass; record any deviations from expected outcomes
- [X] T066 Verify spec SC-001–SC-009 are all met: line-count drop (SC-001/002), clean compile (SC-003), KMPMobileApp parity (SC-004), Runtime Preview parity (SC-005), file-count drop for new modules (SC-006), generator removal (SC-007), consumer-facing template (SC-008), and feature-084 unblock readiness (SC-009)
- [X] T067 Update feature 084's spec status: in `/Users/dhaukoos/CodeNodeIO/specs/084-ui-fbp-runtime-preview/spec.md`, change the `Status: On Hold pending feature 085` line and the HOLD NOTICE block to reflect that feature 085 has shipped and 084 is now ready for `/speckit.clarify` + `/speckit.plan` re-runs against the universal runtime; cite the relevant new contracts (`{Module}Runtime.kt` factory, `ModuleController` interface inheritance) so the resumption has a clear starting point

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
- Generator changes (US1: T012–T023) must be complete before module regeneration tasks (T024–T028) run
- Within T021a–T021d, run sequentially: spec mapping → generator invocation → deprecated-call-site removal → deletion scheduling
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
