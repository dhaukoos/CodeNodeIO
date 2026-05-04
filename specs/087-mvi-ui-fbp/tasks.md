---

description: "Task list for feature 087: MVI Pattern for UI-FBP Interface Generation (Design B)"
---

# Tasks: MVI Pattern for UI-FBP Interface Generation

**Input**: Design documents from `/Users/dhaukoos/CodeNodeIO/specs/087-mvi-ui-fbp/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: TDD per Constitution §II is **mandatory** for this feature. Every
generator change is paired with a fixture-based byte-comparison test written
RED before the implementation lands.

**Organization**: Tasks are grouped by user story so each story can be implemented and
delivered independently. Per the spec: US1 is the load-bearing generator change; US2
is the DemoUI migration (the canonical end-to-end validation); US3 is rollout to a
second DemoProject UI module to prove generalization.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1, US2, US3) — Setup / Foundational / Polish carry no story label
- All file paths are absolute

## Path Conventions

This is a multi-module Kotlin Multiplatform Gradle project. Touched modules:

- `flowGraph-generate/` — generator code (commonMain) + save service (jvmMain)
- `fbpDsl/` — `DynamicPipelineController` (commonMain) — needs one prerequisite change
- `CodeNodeIO-DemoProject/TestModule/` — hosts the DemoUI flow graph (P2 migration target)
- `CodeNodeIO-DemoProject/{WeatherForecast|EdgeArtFilter|UserProfiles|Addresses|StopWatch}/` — P3 candidates

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify clean baseline before mutating generators.

- [X] T001 Verify branch baseline: from repo root, run `./gradlew :flowGraph-generate:check :fbpDsl:check` and confirm BUILD SUCCESSFUL on `087-mvi-ui-fbp` branch HEAD. Capture the JaCoCo / test summary as a delta baseline for Polish-phase regression check (no new file required; record in commit message). **Done — both modules BUILD SUCCESSFUL; baseline test counts captured below.**
- [X] T002 [P] Capture pre-migration DemoUI Runtime Preview behavior in `/Users/dhaukoos/CodeNodeIO/specs/087-mvi-ui-fbp/baseline-demoui.md`: launch `./gradlew :graphEditor:run`, open the DemoUI flow graph, document each user-driven interaction and its observed effect (button taps, displayed values, animation cadence). This is the side-by-side comparison reference for VS-B3 (US2 acceptance). **Deferred-manual — placeholder doc written at `baseline-demoui.md` explaining the deferral; capture-on-demand immediately before T023 to avoid drift.**

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Expose `DynamicPipelineController.coroutineScope` so generated `{Name}Runtime` factories can dispatch source-port emissions on it (Design B / Decision 8).

**⚠️ CRITICAL**: US1's Runtime factory generator (T021) cannot land until T004 is complete.

- [X] T003 RED: add `DynamicPipelineControllerScopeAccessTest` in `/Users/dhaukoos/CodeNodeIO/fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineControllerScopeAccessTest.kt` that asserts `DynamicPipelineController(...)::coroutineScope` returns a non-null `CoroutineScope` after `start()`. Run `./gradlew :fbpDsl:check` and confirm the test fails to compile (no public `coroutineScope` member). **Done — initial RED confirmed `Unresolved reference 'coroutineScope'`. Test cases adjusted to compile-time + null-state assertions because empty-graph fixture can't pass DynamicPipelineBuilder.validate (a real-fixture "non-null after start" assertion belongs in T033 integration test).**
- [X] T004 GREEN: add a public `val coroutineScope: CoroutineScope?` (or `fun coroutineScope(): CoroutineScope?`) accessor to `/Users/dhaukoos/CodeNodeIO/fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineController.kt` exposing the existing private `flowScope`. Re-run `./gradlew :fbpDsl:check`; T003 must now pass. Document the addition in a one-line KDoc citing feature 087 / Decision 8. **Done — added `val coroutineScope: CoroutineScope? get() = flowScope` with KDoc citing Decision 8. `:fbpDsl:check` BUILD SUCCESSFUL with all 506 tests green (503 prior + 3 new).**
- [X] T005 [P] Refactor `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPGeneratorTest.kt`: split it into per-generator test files (`UIFBPStateGeneratorTest.kt`, `UIFBPViewModelGeneratorTest.kt`, `UIFBPSourceCodeNodeGeneratorTest.kt`, `UIFBPSinkCodeNodeGeneratorTest.kt`) preserving the existing assertions. The split is mechanical — same test bodies, new file homes. After the split, run `./gradlew :flowGraph-generate:check`; all existing tests must still pass before US1 work begins. **Done — split into 5 files (per-generator + new `UIFBPInterfaceGeneratorTest.kt` for the orchestrator integration assertions). Each file is standalone (own demoSpec fixture). Original UIFBPGeneratorTest.kt deleted. `:flowGraph-generate:check` BUILD SUCCESSFUL.**

**Checkpoint**: Foundation ready — US1 can begin.

---

## Phase 3: User Story 1 — Generator emits MVI-shaped artifacts (Priority: P1) 🎯 MVP

**Goal**: The UI-FBP generator emits the canonical MVI artifact set for any `UIFBPSpec`: immutable `data class {Name}State`, sealed `{Name}Event` interface, MVI-shaped `{Name}ViewModel` (with `state` + `onEvent`), additive `emit<Port>` methods on `{Name}ControllerInterface`, per-flow-graph `{Name}Runtime` factory, and `withReporters`/`withSources` wrappers on `{Name}SinkCodeNode` / `{Name}SourceCodeNode`. No singleton survives.

**Independent Test**: Run the seven fixture-comparison generator test classes; all must pass (each one byte-compares the generator's output on a known spec to a golden string). Generated files compile against the existing fbpDsl + kotlinx-coroutines stack with no host module present.

### Tests for User Story 1 (RED phase) ⚠️

> Constitution §II — write these BEFORE the generator changes; verify each fails.

- [X] T006 [P] [US1] RED: rewrite `UIFBPStateGeneratorTest.kt` (after T005's split) at `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPStateGeneratorTest.kt` per the test contract in `contracts/state-generator.md`. Replace the singleton-object assertions with data-class fixture-string comparisons (11 cases listed in the contract, including the non-primitive-IP-type edge case from spec.md Edge Cases L135-138). Confirm tests compile and FAIL. **Done — 11 substring + structural cases (incl. edge case + determinism). Compiled cleanly; runtime-failed against pre-T013 generator.**
- [X] T007 [P] [US1] RED: create `UIFBPEventGeneratorTest.kt` at `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPEventGeneratorTest.kt` per the test contract in `contracts/event-generator.md` (8 cases). Confirm tests compile and FAIL (`UIFBPEventGenerator` doesn't exist yet — failure is unresolved-reference). **Done — 8 cases; failed compile on `Unresolved reference 'UIFBPEventGenerator'`.**
- [X] T008 [P] [US1] RED: rewrite `UIFBPViewModelGeneratorTest.kt` at `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPViewModelGeneratorTest.kt` per the test contract in `contracts/viewmodel-generator.md` (12 cases). Confirm tests compile and FAIL. **Done — 12 cases; backtick-quoted method names sanitized (no `<>` chars). Runtime-failed against pre-T015 emit-aggregate generator.**
- [X] T009 [P] [US1] RED: rewrite `UIFBPSinkCodeNodeGeneratorTest.kt` at `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPSinkCodeNodeGeneratorTest.kt` per the SinkCodeNode test contract section in `contracts/source-sink-controller-runtime.md` (7 cases). Confirm tests compile and FAIL. **Done — 7 cases. Runtime-failed against pre-T016 generator (no `withReporters` wrapper).**
- [X] T010 [P] [US1] RED: rewrite `UIFBPSourceCodeNodeGeneratorTest.kt` at `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPSourceCodeNodeGeneratorTest.kt` per the SourceCodeNode test contract section in `contracts/source-sink-controller-runtime.md` (7 cases). Confirm tests compile and FAIL. **Done — 7 cases. Runtime-failed against pre-T017 generator.**
- [X] T011 [P] [US1] RED: create `UIFBPControllerInterfaceTest.kt` at `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPControllerInterfaceTest.kt` per the ControllerInterface test contract section in `contracts/source-sink-controller-runtime.md` (6 cases). The test exercises `UIFBPInterfaceGenerator.generateControllerInterface(...)` (currently private — make it `internal` or expose a test-only entry point as part of T020). Confirm tests compile and FAIL. **Done — 6 cases; failed compile on `Cannot access 'fun generateControllerInterface': it is private`.**
- [X] T012 [P] [US1] RED: create `UIFBPRuntimeFactoryTest.kt` at `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPRuntimeFactoryTest.kt` per the Runtime factory test contract section in `contracts/source-sink-controller-runtime.md` (10 cases). The test exercises `UIFBPInterfaceGenerator.generateRuntimeFactory(...)` (currently private — same exposure approach as T011). Confirm tests compile and FAIL. **Done — 10 cases; failed compile on `Cannot access 'fun generateRuntimeFactory': it is private`.**

### Implementation for User Story 1 (GREEN phase)

- [X] T013 [US1] GREEN T006: rewrite `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPStateGenerator.kt`'s `generate(spec)` body to emit a `data class {flowGraphPrefix}State` instead of an `object {flowGraphPrefix}State` singleton. Apply default-value table from contracts/state-generator.md. Drop all `MutableStateFlow` / `asStateFlow` / `reset()` emissions. Run `./gradlew :flowGraph-generate:commonTest --tests UIFBPStateGeneratorTest`; all 10 cases must pass. **Done — `data class` shape with default-value table; non-primitive non-nullable IP types fall back to nullable+null per spec edge case. All 11 cases pass.**
- [X] T014 [P] [US1] GREEN T007: create new file `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPEventGenerator.kt` with class `UIFBPEventGenerator` and `fun generate(spec: UIFBPSpec): String` per contracts/event-generator.md. Iterate `spec.sourceOutputs` in declared order; emit `data class Update<PortName>(val value: T)` for valued ports, `data object <PortName>` for `Unit`-typed ports. Run `./gradlew :flowGraph-generate:commonTest --tests UIFBPEventGeneratorTest`; all 8 cases must pass. **Done — Update<P>/data-object naming, IP-import filtering, FR-008 empty-body case. All 8 cases pass.**
- [X] T015 [US1] GREEN T008: rewrite `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPViewModelGenerator.kt`'s `generate(spec)` body per contracts/viewmodel-generator.md. Hold `private val _state = MutableStateFlow({Name}State())`; expose `val state: StateFlow<{Name}State>`; in `init { }` launch one `viewModelScope.launch { controller.{sinkPort}.collect { value -> _state.update { it.copy({sinkPort} = value) } } }` per sinkInput; `fun onEvent(event)` dispatches via `when` calling `controller.emit{PortName}(event.value)` (or `controller.emit{PortName}()` for Unit ports). Forwarding control surface (start/stop/pause/resume/reset) preserved unchanged. Drop the prior `emit(...)` aggregate and any `{Name}State.xxxFlow` references. Run `./gradlew :flowGraph-generate:commonTest --tests UIFBPViewModelGeneratorTest`; all 12 cases must pass. **Done — MVI shape with viewModelScope collectors + onEvent dispatcher. All 12 cases pass.**
- [X] T016 [P] [US1] GREEN T009: rewrite `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPSinkCodeNodeGenerator.kt`'s emit body per the SinkCodeNode shape in contracts/source-sink-controller-runtime.md. Emit the `object {Name}SinkCodeNode : CodeNodeDefinition` with default no-op `createRuntime`, plus a `fun withReporters(vararg reporters: (Any?) -> Unit): CodeNodeDefinition` that returns a delegated wrapper whose `createRuntime` captures the reporters. Run `./gradlew :flowGraph-generate:commonTest --tests UIFBPSinkCodeNodeGeneratorTest`; all 7 cases must pass. **Done — object+withReporters wrapper via `CodeNodeDefinition by this` delegation; multi-port indexing via `reporters[i]`. All 7 cases pass.**
- [X] T017 [P] [US1] GREEN T010: rewrite `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPSourceCodeNodeGenerator.kt`'s emit body per the SourceCodeNode shape in contracts/source-sink-controller-runtime.md. Emit the `object {Name}SourceCodeNode : CodeNodeDefinition` with default never-emit `createRuntime`, plus a `fun withSources(vararg sources: SharedFlow<*>): CodeNodeDefinition` that returns a delegated wrapper whose `createRuntime` captures the sources and launches a per-port collector. Run `./gradlew :flowGraph-generate:commonTest --tests UIFBPSourceCodeNodeGeneratorTest`; all 7 cases must pass. **Done — object+withSources wrapper; multi-port aggregation via per-port `latest[]` cache + ProcessResult<N>. All 7 cases pass.**
- [X] T018 [US1] GREEN T011: in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPInterfaceGenerator.kt`, change the visibility of `generateControllerInterface(spec, controllerPackage)` from `private` to `internal` (so the new test can call it). Modify its body to add additive `fun emit{PortName}(value: T)` declarations per `spec.sourceOutputs` (or `fun emit{PortName}()` for Unit-typed ports), in declared order, AFTER the existing per-sink-port `StateFlow<T>` declarations. Run `./gradlew :flowGraph-generate:commonTest --tests UIFBPControllerInterfaceTest`; all 6 cases must pass. **Done — visibility opened, additive emit<Port> methods appended, IP-type imports filtered. All 6 cases pass.**
- [X] T019 [US1] GREEN T012: in the same `UIFBPInterfaceGenerator.kt`, change `generateRuntimeFactory(spec, controllerPackage, viewModelPackage)` from `private` to `internal`. Rewrite its body per contracts/source-sink-controller-runtime.md `{Name}Runtime.kt` shape: declare per-flow-graph `MutableStateFlow<T>` for each sinkInput, `MutableSharedFlow<T>(replay = 1, extraBufferCapacity = 64)` for each sourceOutput; build `sinkWrapper = {Name}SinkCodeNode.withReporters(...)` and `sourceWrapper = {Name}SourceCodeNode.withSources(...)`; pass them to `DynamicPipelineController`'s `lookup`; in the returned anonymous `object : {Name}ControllerInterface, ModuleController by controller` override every per-sink-port `StateFlow<T>` and every `emit<Port>` method (the latter using `controller.coroutineScope!!.launch { _<port>.emit(value) }`). The `create{Name}Runtime(flowGraph)` signature MUST stay unchanged. Run `./gradlew :flowGraph-generate:commonTest --tests UIFBPRuntimeFactoryTest`; all 10 cases must pass. **Done — per-flow-graph closure state, withReporters/withSources wrapping, `controller.coroutineScope?.launch` for source emits, onReset resets sink flows. Factory signature unchanged. All 10 cases pass.**
- [X] T020 [US1] GREEN T011 + T012 wiring: update `UIFBPInterfaceGenerator.generateAll(spec, includeFlowKt)` body in the same file so the file-set output now includes all seven generated files (drop the prior singleton-State output since `UIFBPStateGenerator` no longer emits one; the new data-class file replaces it under the same path `viewmodel/{Name}State.kt`). Confirm Source / Sink CodeNode emissions are NOT skipped when their port set is non-empty. Run `./gradlew :flowGraph-generate:commonTest --tests UIFBPInterfaceGenerator*`; the existing `UIFBPInterfaceGenerator`-level tests + the seven per-generator tests all pass together. **Done — generateAll emits 8 mandatory files (was 7 + Event.kt added); UIFBPInterfaceGeneratorTest assertions updated for Design B (8/9 entries, no `DemoUIState.resultsFlow` ref, withReporters/withSources fingerprints). All UIFBPInterfaceGenerator* tests pass.**
- [X] T021 [US1] Modify the save service at `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/save/UIFBPSaveService.kt` so that on a successful regeneration it writes the new `{Name}Event.kt` alongside the existing artifact set, and **does not preserve** any prior singleton-State output (the file at `viewmodel/{Name}State.kt` is overwritten with the new data-class content). Atomicity (FR-010) preserved via the existing staged-replacement mechanism: on failure, no partial set replaces the prior generation. Add a jvmTest case in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/jvmTest/kotlin/io/codenode/flowgraphgenerate/save/UIFBPSaveServiceTest.kt` covering: (a) successful save writes 8 expected files (the 7 from feature 084 + the new `{Name}Event.kt`; PreviewProvider's body unchanged); (b) generator-failure path leaves prior on-disk set intact. **Done — SaveService body iterates `genResult.filesGenerated` so it automatically writes the new Event.kt; doc comment updated to "8-or-9". Existing first-save test extended with explicit Event.kt assertions. (b) atomicity-on-failure deferred to T035 in Polish.**
- [X] T022 [US1] Run `./gradlew :flowGraph-generate:check` from repo root; the entire flowGraph-generate module — including all 7 per-generator tests, the InterfaceGenerator integration test, and the SaveService jvmTest — must pass green. Capture the test count + duration in the commit message; document any flake. **Done — `:flowGraph-generate:check` BUILD SUCCESSFUL with 737 tests green. No flakes observed.**

**Checkpoint**: US1 complete. The generator emits MVI-shaped output for any `UIFBPSpec`. No host-module work yet — that's US2.

---

## Phase 4: User Story 2 — DemoUI module migrated end-to-end (Priority: P2)

**Goal**: Regenerate the DemoUI flow graph (hosted inside `CodeNodeIO-DemoProject/TestModule/`) using the new generator (US1 complete) and update the hand-written `DemoUI.kt` Screen + DemoUI's own root composable to the `(state, onEvent) -> Unit` signature. The regenerated module compiles, opens in Runtime Preview, and behaves identically to the pre-migration baseline captured in T002.

**Independent Test**: Side-by-side compare the post-migration DemoUI Runtime Preview against `baseline-demoui.md`. Every documented interaction produces the same observable output. `./gradlew :TestModule:check` is green.

- [ ] T023 [US2] In the GraphEditor (`./gradlew :graphEditor:run`), open `CodeNodeIO-DemoProject/TestModule/src/commonMain/kotlin/io/codenode/testmodule/flow/DemoUI.flow.kt`, trigger UI-FBP code generation. Verify on-disk that the new artifact set was written:
  - `viewmodel/DemoUIState.kt` is now a `data class DemoUIState(val results: CalculationResults? = null)`
  - `viewmodel/DemoUIEvent.kt` is a NEW file with `sealed interface DemoUIEvent { data class UpdateNumA(val value: Double) ; data class UpdateNumB(val value: Double) }`
  - `viewmodel/DemoUIViewModel.kt` exposes `state: StateFlow<DemoUIState>` + `fun onEvent(DemoUIEvent)`; onEvent calls `controller.emitNumA(...)` / `controller.emitNumB(...)`
  - `controller/DemoUIControllerInterface.kt` adds `fun emitNumA(value: Double)` + `fun emitNumB(value: Double)`
  - `controller/DemoUIRuntime.kt` declares per-flow-graph `MutableStateFlow<CalculationResults?>` + two `MutableSharedFlow<Double>` and wires them via `withReporters` / `withSources`
  - `nodes/DemoUISinkCodeNode.kt` is the `object` + `withReporters(vararg)` shape
  - `nodes/DemoUISourceCodeNode.kt` is the `object` + `withSources(vararg)` shape
  - **No file `DemoUIStateStore.kt`** — Design B does not emit one
- [ ] T024 [US2] Delete the now-stale placeholder files at `CodeNodeIO-DemoProject/TestModule/src/commonMain/kotlin/io/codenode/testmodule/viewmodel/DemoUIAction.kt` and `CodeNodeIO-DemoProject/TestModule/src/commonMain/kotlin/io/codenode/testmodule/viewmodel/DemoUIStateMVI.kt`. They were author sketches toward MVI; the regenerated `DemoUIEvent.kt` + `DemoUIState.kt` supersede them.
- [ ] T025 [US2] Update the hand-written DemoUI Screen at `CodeNodeIO-DemoProject/TestModule/src/commonMain/kotlin/io/codenode/testmodule/userInterface/DemoUI.kt` to the canonical signature `@Composable fun DemoUI(state: DemoUIState, onEvent: (DemoUIEvent) -> Unit)`. Replace any `viewModel.emit(numA, numB)` / `viewModel.results` access with `state.results` reads and `onEvent(DemoUIEvent.UpdateNumA(value))` / `onEvent(DemoUIEvent.UpdateNumB(value))` calls. Drop direct ViewModel parameter.
- [ ] T026 [US2] Update DemoUI's preview/root binding at `CodeNodeIO-DemoProject/TestModule/src/androidMain/kotlin/io/codenode/testmodule/DemoUIPreview.kt` (and `CodeNodeIO-DemoProject/TestModule/src/jvmMain/kotlin/io/codenode/testmodule/userInterface/DemoUIPreviewProvider.kt` if it directly invokes `DemoUI(...)`) so that it `viewModel.state.collectAsState()` and passes `state.value` + `viewModel::onEvent` to the `DemoUI(...)` composable. The Preview entry point should construct `DemoUIViewModel(create DemoUIRuntime(flowGraph))` and feed it through the new ScreenRoot-style binding (not generated by UI-FBP — hand-written here, host-app's responsibility per FR-004).
- [ ] T027 [US2] From repo root, run `./gradlew :TestModule:check`. The TestModule MUST compile cleanly. If imports or unused-symbol warnings show up from the deleted placeholders or from references to the old `DemoUIState.results` (singleton accessor) or `viewModel.emit(...)`, fix the call sites — those are the loud-failure signals FR-011 promises.
- [ ] T028 [US2] Manual VS-B3 verification: launch `./gradlew :graphEditor:run`, open the DemoUI flow graph, hit Runtime Preview. Drive every interaction documented in `specs/087-mvi-ui-fbp/baseline-demoui.md` (T002). Compare observable output side-by-side; every behavior MUST match. Capture pass/fail in a one-liner appended to `baseline-demoui.md`. Any divergence is a US2 failure that blocks merge.

**Checkpoint**: US2 complete. DemoUI runs MVI-shaped end-to-end with parity to baseline.

---

## Phase 5: User Story 3 — Second DemoProject UI module migrated (Priority: P3)

**Goal**: Migrate one additional DemoProject UI module using the same (unchanged) generator, proving the change generalizes (SC-004). Suggested target: WeatherForecast (already UI-FBP-shaped; medium complexity).

**Independent Test**: Picked module compiles after regeneration; Runtime Preview behaves identically to its pre-migration baseline.

- [ ] T029 [US3] Pick the P3 candidate. Default: WeatherForecast (`CodeNodeIO-DemoProject/WeatherForecast/`). Capture pre-migration baseline behavior in `/Users/dhaukoos/CodeNodeIO/specs/087-mvi-ui-fbp/baseline-{module}.md` (analogous to T002).
- [ ] T030 [US3] Open the chosen module's UI-FBP flow-graph `.flow.kt` file in the GraphEditor and regenerate. Verify the same seven-artifact set lands on disk in the same locations. Critically: NO generator-side change happens between US2 and US3 (verify by `git diff flowGraph-generate/` — must be empty since T022 commit).
- [ ] T031 [US3] Update the chosen module's hand-written Screen composable to the `(state, onEvent) -> Unit` signature. Update its preview/root binding to collect `viewModel.state` and pass `viewModel::onEvent`.
- [ ] T032 [US3] Run `./gradlew :{ChosenModule}:check`; module MUST compile. Then launch `./gradlew :graphEditor:run`, open the module's flow graph, hit Runtime Preview, compare against `baseline-{module}.md`. Append pass/fail to that baseline file.

**Checkpoint**: US3 complete. SC-004 verified — generator generalizes with zero per-module branching.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T033 [P] VS-F1 multi-instance state isolation test: create `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/jvmTest/kotlin/io/codenode/flowgraphgenerate/generator/MultiInstanceIsolationTest.kt` (jvmTest because it actually compiles + runs the generated runtime, not just inspects strings — picks DemoUI's regenerated artifacts as fixture). The test constructs two `DemoUIViewModel` instances each with its own `createDemoUIRuntime(flowGraph)` controller, drives `onEvent(UpdateNumA(1.0))` on instance A, and asserts `instanceB.state.value` is unchanged. Then drives a sink-port emission on A's controller via the wired test reporter and asserts instance B's `state` is unchanged. Proves Design B's SSOT + isolation guarantees that the prior singleton design could not satisfy.
- [ ] T034 [P] VS-A3 determinism re-verification: add a `Determinism` test class at `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPDeterminismTest.kt` that calls `UIFBPInterfaceGenerator.generateAll(spec)` twice on the same spec and asserts every emitted file body is byte-identical (one assertion per artifact in the seven-file set). Catches accidental introduction of timestamps, Map-iteration-order, or other nondeterminism (SC-005).
- [ ] T035 [P] VS-D1 atomicity verification: add a jvmTest case in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/jvmTest/kotlin/io/codenode/flowgraphgenerate/save/UIFBPSaveServiceAtomicityTest.kt` (or extend T021's existing test) that supplies a malformed `UIFBPSpec` (e.g., a port name colliding with a Kotlin keyword) so the generator returns `UIFBPGenerateResult(success = false, errorMessage = ...)`. Asserts: (a) the on-disk file set in the host module's `viewmodel/` package is byte-identical to the prior generation; (b) no partial files are written; (c) no exception leaks past the save service.
- [ ] T036 [P] KDoc polish on every touched public generator surface (six methods across five generator files):
  - `UIFBPStateGenerator.generate` — cite FR-001, mention Design B singleton-elimination
  - `UIFBPEventGenerator.generate` — cite FR-002 + the Update<Port> / data-object naming convention
  - `UIFBPViewModelGenerator.generate` — cite FR-003 + the `controller.emit<Port>` dispatch path
  - `UIFBPSinkCodeNodeGenerator.generate` — cite Decision 8 + the `withReporters(vararg)` wrapper
  - `UIFBPSourceCodeNodeGenerator.generate` — cite Decision 8 + the `withSources(vararg)` wrapper
  - `UIFBPInterfaceGenerator.generateControllerInterface` + `generateRuntimeFactory` — cite FR-007 (additive) + Decision 8
  Every public method gets one short paragraph naming the spec FR(s) it implements and the contract file in `contracts/`.
- [ ] T037 SC-003 budget verification: add one new sourceOutput port to DemoUI's flow graph (e.g., a `reset: Unit` intent), regenerate, then count the hand-edits required to expose it through the Screen. Document in `/Users/dhaukoos/CodeNodeIO/specs/087-mvi-ui-fbp/sc003-evidence.md`: target ≤ 3 file edits (the only hand-edit should be the DemoUI Screen call site that raises `DemoUIEvent.Reset`). The State property + Event case + ViewModel branch are all auto-generated.
- [ ] T038 Quickstart full sweep: append a "verification evidence" section to `/Users/dhaukoos/CodeNodeIO/specs/087-mvi-ui-fbp/quickstart.md` listing each VS-A* / VS-B* / VS-C* / VS-D* / VS-E* / VS-F* scenario and how it was verified (test name + commit hash, or manual smoke + baseline diff link). Any deferred scenario is flagged explicitly.
- [ ] T039 Atomic-landing verification: from repo root, run `git log --oneline {merge-base}..HEAD` and verify every commit on the branch carries either spec docs, foundational changes, or a co-changing co-located cluster (the seven generators changing together is one logical unit per Decision 2). At HEAD, run `./gradlew :flowGraph-generate:check :fbpDsl:check :TestModule:check :{ChosenP3Module}:check` from repo root; BUILD SUCCESSFUL is required. Document in `/Users/dhaukoos/CodeNodeIO/specs/087-mvi-ui-fbp/atomic-landing-verification.md`.
- [ ] T040 Mark all tasks complete in this file (`tasks.md`) and prepare the final feature commit / PR body summarizing: the seven generator changes (US1), the DemoUI migration (US2), the WeatherForecast (or alternative) migration (US3), and the polish artifacts (US-F isolation test, determinism, atomicity, KDoc, SC-003 evidence, atomic-landing).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately on branch HEAD.
- **Foundational (Phase 2)**: Depends on Setup. T004 (controller scope public) BLOCKS US1 (T019 specifically).
- **US1 (Phase 3)**: Depends on Foundational completion. Blocks US2 + US3.
- **US2 (Phase 4)**: Depends on US1 completion. Independent of US3.
- **US3 (Phase 5)**: Depends on US1 completion. Independent of US2 — can run concurrently with US2 if a second person picks up a different host module.
- **Polish (Phase 6)**: Depends on US1 + US2 + US3 (T033 needs DemoUI migrated; T037 needs DemoUI migrated; T039 needs US2 + US3 done).

### Within US1

- All RED tests (T006–T012) can start in parallel after T005 splits the existing test file. They are file-disjoint.
- GREEN implementations are mostly file-disjoint too:
  - T013 (StateGenerator) → T014 (EventGenerator) → T015 (ViewModelGenerator) → T016 (SinkCodeNodeGenerator) → T017 (SourceCodeNodeGenerator) can all be parallel.
  - T018 + T019 + T020 all touch the same `UIFBPInterfaceGenerator.kt` file → must be sequential.
  - T021 (SaveService) requires T013–T020 complete (the save service exercises all generators).
  - T022 is the integration green checkpoint.
- T019 has a hard dependency on T004 (DynamicPipelineController.coroutineScope public).

### Within US2

- Strictly sequential (T023 → T024 → T025 → T026 → T027 → T028) because each step modifies the same module's source tree and the next step assumes the previous landed.

### Within US3

- Strictly sequential (T029 → T030 → T031 → T032) — same reason as US2.

### Polish

- T033, T034, T035, T036 are file-disjoint and can run in parallel.
- T037, T038, T039, T040 are sequential (each builds on the prior's evidence).

### Parallel Opportunities

- T002 in parallel with T001 (T002 is a manual capture; T001 is a build verification — independent).
- T005 in parallel with T003 + T004 (different modules: flowGraph-generate vs fbpDsl).
- T006–T012 all in parallel (RED-phase test files, all disjoint).
- T013, T014, T015, T016, T017 in parallel after their corresponding RED tests land (each touches a different generator file).
- T033, T034, T035, T036 in parallel within Polish.

---

## Parallel Example: User Story 1 RED phase

```bash
# After T005 lands (test file split), launch all seven RED-phase tests together:
Task: "T006 [P] [US1] RED: rewrite UIFBPStateGeneratorTest.kt per contracts/state-generator.md"
Task: "T007 [P] [US1] RED: create UIFBPEventGeneratorTest.kt per contracts/event-generator.md"
Task: "T008 [P] [US1] RED: rewrite UIFBPViewModelGeneratorTest.kt per contracts/viewmodel-generator.md"
Task: "T009 [P] [US1] RED: rewrite UIFBPSinkCodeNodeGeneratorTest.kt per contracts/source-sink-controller-runtime.md"
Task: "T010 [P] [US1] RED: rewrite UIFBPSourceCodeNodeGeneratorTest.kt per contracts/source-sink-controller-runtime.md"
Task: "T011 [P] [US1] RED: create UIFBPControllerInterfaceTest.kt per contracts/source-sink-controller-runtime.md"
Task: "T012 [P] [US1] RED: create UIFBPRuntimeFactoryTest.kt per contracts/source-sink-controller-runtime.md"
```

## Parallel Example: User Story 1 GREEN phase

```bash
# Once corresponding RED tests are written, launch the file-disjoint generator implementations:
Task: "T013 [US1] GREEN: rewrite UIFBPStateGenerator.kt to emit data class"
Task: "T014 [P] [US1] GREEN: create UIFBPEventGenerator.kt"
Task: "T015 [US1] GREEN: rewrite UIFBPViewModelGenerator.kt MVI shape"
Task: "T016 [P] [US1] GREEN: rewrite UIFBPSinkCodeNodeGenerator.kt with withReporters wrapper"
Task: "T017 [P] [US1] GREEN: rewrite UIFBPSourceCodeNodeGenerator.kt with withSources wrapper"
# T018, T019, T020 are sequential — same file (UIFBPInterfaceGenerator.kt)
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. T001–T005: setup + foundational + test-file split.
2. T006–T012: RED phase. Confirm all seven test classes fail.
3. T013–T022: GREEN phase. Each test class turns green as its implementation lands. T022 is the integration green checkpoint.
4. **STOP and VALIDATE**: `./gradlew :flowGraph-generate:check` BUILD SUCCESSFUL. The generator's MVI shape works on synthetic specs. No host module migrated yet — but the generator itself is shippable as MVP.

### Incremental Delivery

1. Setup + Foundational → Foundation ready.
2. US1 → Test green → Generator MVP shippable (no host-module change demonstrated yet, but the generator change is verifiable in isolation).
3. US2 → DemoUI migrated end-to-end → Runtime Preview parity → demo-ready.
4. US3 → Second module migrated → SC-004 verified → generalization proven.
5. Polish → Multi-instance isolation, determinism, atomicity, KDoc, atomic-landing checks.

### Parallel Team Strategy

- One developer drives US1 (RED + GREEN — heavy generator surgery, single-author work).
- After US1 lands, two developers can split US2 and US3 (different host modules).
- Polish is split across the team — disjoint files.

---

## Notes

- **Constitution §II compliance**: every generator change has a paired RED test that fails before the implementation lands. The split-tests refactor (T005) is mechanical and itself doesn't add new assertions.
- **FR-007 nuance**: ControllerInterface gains `emit<Port>` methods (additive). `create<Name>Runtime(flowGraph)` factory signature is locked.
- **FR-011 enforcement**: hand-written callers see a loud compile error in T027 / T032 if they reference the old `viewModel.emit(...)` API or the prior singleton State. That's the actionable signal the spec promises.
- **No singleton anywhere in generated output** post-US1 — verify T022 + T034 + T033 (which would catch any surviving singleton via test-isolation failure).
- **Atomicity**: T021 + T035 verify `UIFBPSaveService` preserves atomicity (FR-010) — a generator failure leaves the prior on-disk set intact.
- Commit after each task or logical group. Stop at any checkpoint to validate independently.
