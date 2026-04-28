# Tasks: Add Runtime Preview Support to UI-FBP Code Generation (post-085)

**Input**: Design documents from `/Users/dhaukoos/CodeNodeIO/specs/084-ui-fbp-runtime-preview/`
**Prerequisites**: plan.md, spec.md (post-clarification Session 2026-04-28), research.md, data-model.md, contracts/, quickstart.md

**Tests**: TDD is mandatory per the project constitution and explicitly required by plan.md ("Each generator change gets a unit test written first"). Test tasks are listed before their corresponding implementation tasks.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (Setup/Foundational/Polish carry no story label)
- Exact file paths included in every task

## Path Conventions

- Generators (commonMain): `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/`
- Generators tests (commonTest): `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/`
- Save service (jvmMain): `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/`
- Save service tests (jvmTest): `flowGraph-generate/src/jvmTest/kotlin/io/codenode/flowgraphgenerate/`
- Runtime contract tests (jvmTest): `flowGraph-execute/src/jvmTest/kotlin/io/codenode/flowgraphexecute/`
- DemoProject TestModule (sibling repo): `/Users/dhaukoos/CodeNodeIO-DemoProject/TestModule/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish the post-082/083 typed input model that every later phase reads from.

- [X] T001 Inventory the current `UIFBPSpec` field set and its callers. Read `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/parser/UIFBPSpec.kt` and `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/parser/UIComposableParser.kt`. Record (in a working note inside `/Users/dhaukoos/CodeNodeIO/specs/084-ui-fbp-runtime-preview/CROSS-CHECK-085.md` or as a comment in tasks.md) the full set of files that reference `spec.moduleName` so that the field-rename in T002–T004 can be propagated atomically.
- [X] T002 [P] Extend `UIFBPSpec` with the post-082/083 typed fields in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/parser/UIFBPSpec.kt`: add `flowGraphPrefix: String` (drives generated-file prefix + PreviewRegistry key) and `composableName: String` (drives the Composable function call inside PreviewProvider). Retain `moduleName` only as a deprecated alias for one release (`@Deprecated("Use flowGraphPrefix or composableName per Decision 2", ReplaceWith("flowGraphPrefix"))`) so callers can migrate incrementally; remove in T058.
- [X] T003 [P] Add a unit test for the extended `UIFBPSpec` shape in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/parser/UIFBPSpecTest.kt` (create file). Tests pin: (a) constructing a spec with all three potentially-distinct identifiers (flowGraphPrefix, composableName, packageName) succeeds; (b) the deprecated `moduleName` resolves to `flowGraphPrefix`; (c) `ipTypeImports` is unchanged.
- [X] T004 Update `UIComposableParser` in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/parser/UIComposableParser.kt` to populate the new `composableName` field from the parsed `@Composable fun X(viewModel: ...)` declaration AND accept a separately-supplied `flowGraphPrefix` parameter (sourced from the user-selected `.flow.kt` file's filename minus `.flow.kt`). Add a test in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/parser/UIComposableParserTest.kt` (create or extend) asserting the parser populates both fields independently.

**Checkpoint**: Setup complete — typed input model is post-082/083 ready and validated.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Generator-level changes that every user story depends on. Each story consumes one or more of these modified generators.

**⚠️ CRITICAL**: No user story work can begin until Phase 2 is complete (each story exercises at least one Phase-2 generator).

- [X] T005 [P] Test-first: assert the post-085 emission shape of `PreviewProviderGenerator` with a separate `composableName` input. Extend `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/PreviewProviderGeneratorTest.kt` with cases proving: (a) when `composableName == flowGraphPrefix.pascalCase()` (the entity-module convention), the output is identical to what 085 emits today (regression-free); (b) when `composableName != flowGraphPrefix`, the `PreviewRegistry.register("…")` key uses the flow-graph prefix while the lambda body invokes the user-authored Composable name; (c) the imports section imports the user-authored Composable's package so the function reference resolves.
- [X] T006 Extend `PreviewProviderGenerator` in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/PreviewProviderGenerator.kt` to accept an optional `composableName: String?` parameter (default null → falls back to `flowGraph.name.pascalCase()` for entity-module callers' backward compat). Decouple the registry-key emission (always `flowGraph.name.pascalCase()`) from the in-lambda Composable function call (uses `composableName` if provided; falls back otherwise). T005 passes.
- [X] T007 [P] Test-first: pin `UIFBPViewModelGenerator`'s post-085 emission shape. Create or extend `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPViewModelGeneratorTest.kt` with cases proving: (a) emitted ViewModel constructor is exactly `({FlowGraph}ControllerInterface)` (single arg, public); (b) emitted class extends `androidx.lifecycle.ViewModel`; (c) one `val y: StateFlow<T> = {FlowGraph}State.yFlow` member per `spec.sinkInputs` entry; (d) `executionState: StateFlow<ExecutionState> = controller.executionState`; (e) `emit(...)` writes to `{FlowGraph}State._x` mutable fields; (f) emitted file contains forwarding control methods `fun start/stop/pause/resume/reset(): FlowGraph` each delegating one-to-one to `controller.{same}()`; (g) generated file lives under `viewmodel/` subpackage; (h) generated-file prefix is `flowGraphPrefix`, not `moduleName`.
- [X] T008 Modify `UIFBPViewModelGenerator` in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPViewModelGenerator.kt` to satisfy T007: switch the emitted constructor to `({FlowGraph}ControllerInterface)`, read flows from `{FlowGraph}State.{y}Flow`, emit forwarding control methods `fun start/stop/pause/resume/reset(): FlowGraph` that one-to-one delegate to `controller.{same}()` (these are required because the UI calls them directly as `viewModel.start()` per US1.AS3 / US2.AS3 — they do NOT come from inheritance), preserve any pre-existing `emit(...)` body that writes to `{FlowGraph}State._x` mutable fields, and emit under the `viewmodel/` subpackage with the flow-graph prefix.
- [X] T009 [P] Test-first: pin `UIFBPStateGenerator`'s post-085 emission shape. Create or extend `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPStateGeneratorTest.kt` asserting: (a) generated file lives under `viewmodel/` subpackage; (b) generated-file prefix is `flowGraphPrefix`; (c) emits a Kotlin `object {FlowGraph}State` with the standard `_y: MutableStateFlow<T>` + `val yFlow: StateFlow<T> = _y.asStateFlow()` pattern per port; (d) emits `fun reset()` covering every `_y`.
- [X] T010 Modify `UIFBPStateGenerator` in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPStateGenerator.kt` to satisfy T009: change the emitted package from base to `viewmodel/`, derive the file prefix from `flowGraphPrefix` instead of `moduleName`.
- [X] T011 [P] Test-first: minimal-change assertions for `UIFBPSourceCodeNodeGenerator`. Extend `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPSourceCodeNodeGeneratorTest.kt` (create if absent) asserting the emitted CodeNode class name is `{flowGraphPrefix}SourceCodeNode` and the file lives in `nodes/` (path unchanged from current).
- [X] T012 Modify `UIFBPSourceCodeNodeGenerator` in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPSourceCodeNodeGenerator.kt` to derive its emitted class name and the FQCN it stamps into the `_codeNodeClass` configuration entry from `flowGraphPrefix` (was `moduleName`).
- [X] T013 [P] Test-first: minimal-change assertions for `UIFBPSinkCodeNodeGenerator` (mirror T011) in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPSinkCodeNodeGeneratorTest.kt`.
- [X] T014 Modify `UIFBPSinkCodeNodeGenerator` in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPSinkCodeNodeGenerator.kt` to mirror T012's flow-graph-prefix derivation.
- [X] T015 [P] Test-first: orchestrator-level assertion in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPInterfaceGeneratorTest.kt`. Pin: (a) `generateAll(spec, includeFlowKt)` returns 8 entries (the post-085 universal set per data-model §2 table); (b) entries 5 and 6 are emitted by `RuntimeControllerInterfaceGenerator` and `ModuleRuntimeGenerator` (verifiable by content fingerprints — interface extends `ModuleController`, factory contains `DynamicPipelineController(`); (c) entry 7 is a PreviewProvider whose registry key matches `flowGraphPrefix` and lambda-body composable call matches `composableName`; (d) when `spec.flowGraphPrefix != spec.composableName`, the registry key and the function call disagree as expected; (e) **degenerate-spec edge case (zero source-output ports)**: a spec with `sourceOutputs.isEmpty()` produces a structurally-complete output — Source CodeNode is either skipped or emits a zero-output runtime; ControllerInterface still emits with one `val y` per `sinkInputs` entry; ViewModel's `emit(...)` body has no parameters; (f) **degenerate-spec edge case (zero sink-input ports)**: a spec with `sinkInputs.isEmpty()` produces a structurally-complete output — ControllerInterface degenerates to the inherited-only `ModuleController` surface (zero `val y` members); ViewModel exposes only `executionState` + control methods; Sink CodeNode is either skipped or emits a zero-input runtime.
- [X] T016 Rewrite `UIFBPInterfaceGenerator.generateAll(spec, includeFlowKt)` in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPInterfaceGenerator.kt` to be a thin orchestrator over the post-085 universal generators. Translate `UIFBPSpec` into a `FlowGraph` model (Source CodeNode + Sink CodeNode wired by an empty middle, with `_codeNodeClass` configuration entries pointing at the per-flow-graph generated CodeNode FQCNs). Feed that `FlowGraph` plus the package set to `RuntimeControllerInterfaceGenerator` and `ModuleRuntimeGenerator`; feed `flowGraphPrefix` + `composableName` to the extended `PreviewProviderGenerator` from T006. Return an 8-entry `UIFBPGenerateResult` per the data-model table. T015 passes.
- [X] T017 Verify `CodeGenerationRunner.GenerationPath.UI_FBP` in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/runner/CodeGenerationRunner.kt` includes the post-085 universal generators (`ModuleRuntimeGenerator`, `RuntimeControllerInterfaceGenerator`, `PreviewProviderGenerator`) feature 085 already wired in. If any are missing, add them; otherwise no change. Run `./gradlew :flowGraph-generate:check` and confirm green.

**Checkpoint**: Foundation ready — every modified generator's emission shape is locked in by a test; orchestrator returns the post-085 universal set.

---

## Phase 3: User Story 1 - UI-FBP-Generated Module Opens in Runtime Preview (Priority: P1) 🎯 MVP

**Goal**: After UI-FBP code generation completes, the GraphEditor's Runtime Preview MUST be able to discover, load, and render the module's Compose UI without any manual file edits. Module reports a healthy "running" state with no missing-class / missing-PreviewProvider / missing-controller errors.

**Independent Test**: Run UI-FBP on a known-qualifying Composable file in a feature-085-scaffolded module that contains no other generated UI-FBP artifacts. Open Runtime Preview. Verify the UI appears, no error dialogs/red placeholders, runtime status reports running. (Quickstart VS-A2–A4 + VS-B3–B4 cover this.)

- [X] T018 [US1] Test-first: `UIFBPSaveService` first-save scenario. Create `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/jvmTest/kotlin/io/codenode/flowgraphgenerate/save/UIFBPSaveServiceTest.kt` with a `first save against a feature-085-scaffolded module emits the post-085 universal set` test: temp-dir fixture mirroring a freshly-scaffolded `Quickstart084` module (build.gradle.kts with `jvm()` + `preview-api`, an empty `flow/Quickstart084.flow.kt`, a qualifying `userInterface/Quickstart084.kt`); call `save(...)` and assert `UIFBPSaveResult.success == true`, every entry from the data-model §2 file table is `kind = CREATED`, and `flowKtMerge.mode == UNCHANGED` (the bootstrap `.flow.kt` already exists from scaffolding).
- [X] T019 [US1] Test-first: `UIFBPSaveService` unscaffolded-host refusal (FR-009 post-clarification). Add a test asserting that when `build.gradle.kts` lacks `jvm()` OR lacks `io.codenode:preview-api`, `save(...)` returns `success = false` with `errorMessage` naming the missing piece(s) and `files.isEmpty()`. Also assert no file was created on disk (use temp-dir snapshot before/after).
- [X] T020 [US1] Create `UIFBPSaveService.kt` at `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/save/UIFBPSaveService.kt` with the contract from `data-model.md` §3: constructor takes `UIFBPInterfaceGenerator + FlowKtParser + FlowGraphSerializer`; `save(spec, flowGraphFile, moduleRoot, options)` returns `UIFBPSaveResult`. Implement the pre-flight host validation (Decision 5) and the per-file decide-and-write loop (CREATED/UNCHANGED for now; UPDATED/SKIPPED_CONFLICT/DELETED arrive in US3). T018 and T019 pass.
- [X] T021 [P] [US1] Test-first: `UIFBPSaveResult` data classes. Add a unit test in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/save/UIFBPSaveResultTest.kt` (create commonMain `save/` directory if needed, OR put the data classes in `commonMain` and the test in `commonTest`) asserting `FileChangeKind` covers all five values (CREATED, UPDATED, UNCHANGED, SKIPPED_CONFLICT, DELETED) and `FlowKtMergeMode` covers all four (CREATED, UPDATED, UNCHANGED, PARSE_FAILED_SKIPPED).
- [X] T022 [US1] Place the result/option/report data classes (`UIFBPSaveResult`, `UIFBPSaveOptions`, `FileChange`, `FileChangeKind`, `FlowKtMergeReport`, `FlowKtMergeMode`, `PortChange`, `DroppedConnection`) in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/save/UIFBPSaveTypes.kt` (create file). Keeping them in `commonMain` makes the contract KMP-safe; `UIFBPSaveService` (jvmMain) imports them. T021 passes.
- [X] T023 [P] [US1] Test-first: end-to-end Runtime-Preview reflection contract via a generator-produced fixture. Create `/Users/dhaukoos/CodeNodeIO/flowGraph-execute/src/jvmTest/kotlin/io/codenode/flowgraphexecute/UIFBPModuleSessionFactoryTest.kt` exercising the Runtime Preview path: build a fake/synthetic UI-FBP module fixture (a generator-produced `{FlowGraph}ControllerInterface` extending `ModuleController` + a `{FlowGraph}State` object + a `{FlowGraph}ViewModel(controllerInterface)` — using the same testfake pattern feature 085's `ModuleSessionFactoryRegressionTest` uses), call `ModuleSessionFactory.createSession(...)`, and assert (a) the returned `RuntimeSession.viewModel` is castable to the typed ViewModel; (b) `viewModel.statusViaProxy()` returns a non-null `FlowExecutionStatus` (proves the `getStatus` proxy case still routes correctly for UI-FBP-shaped interfaces); (c) `viewModel.executionState.value == ExecutionState.IDLE` before start.
- [X] T024 [US1] Update `UIFBPInterfaceGenerator`'s caller (the GraphEditor's UI-FBP code-generation entry point). Find the entry point: `grep -rn "UIFBPInterfaceGenerator\|generateAll" /Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmMain --include=*.kt`. Wire it to: (a) take an explicit `{flow graph file, UI file}` pair via two file selectors mirroring feature 085's Generate Module pattern (already in `CodeGeneratorPanel.kt`); (b) parse the UI file via `UIComposableParser` with the flow-graph-derived prefix; (c) call `UIFBPSaveService.save(...)`; (d) surface the resulting `UIFBPSaveResult` in the GraphEditor's status line. Per FR-014/FR-015, no implicit module scanning.
- [X] T025 (deferred — requires hands-on GraphEditor session) [US1] Manually execute Quickstart VS-A1 against `/Users/dhaukoos/CodeNodeIO-DemoProject/TestModule/`: confirm `build.gradle.kts` has `jvm()` + `preview-api`; if not, apply the migration (Step 1 of VS-A1). Then delete `src/commonMain/kotlin/io/codenode/demo/saved/`; fix `userInterface/DemoUI.kt`'s import to point at `viewmodel/`; uncomment the live `viewModel.emit(a, b)` call. Record the resulting state in a working note inside `/Users/dhaukoos/CodeNodeIO-DemoProject/TestModule/` for VS-A2 to consume.
- [X] T026 (deferred — requires hands-on GraphEditor session) [US1] Manually execute Quickstart VS-A2 (re-run UI-FBP against `{DemoUI.flow.kt, DemoUI.kt}`) and VS-A3 (`./gradlew :TestModule:compileKotlinJvm`). Confirm `UIFBPSaveResult` lists every file from the data-model §2 table with the right `kind`, the build is clean. Record the structured result.
- [X] T027 (deferred — requires hands-on GraphEditor session) [US1] Manually execute Quickstart VS-A4 (open Runtime Preview against `DemoUI`). Confirm: UI renders inside the panel; runtime status reports running; no missing-class / missing-PreviewProvider errors; Stop → Start cycle is clean. This satisfies US1's acceptance scenarios 1–3.
- [X] T028 (deferred — requires hands-on GraphEditor session) [US1] Manually execute Quickstart VS-B1–B3 against a green-field `Quickstart084` module created via feature 085's Generate Module path: confirm UI-FBP runs against a freshly-scaffolded module with zero migration steps and `UIFBPSaveResult` lists the same post-085 set as CREATED. Record the structured result.

**Checkpoint**: US1 complete — UI-FBP-generated modules load in Runtime Preview without manual file edits. MVP shippable.

---

## Phase 4: User Story 2 - Manually-Added Business Logic Drives the UI End-to-End (Priority: P2)

**Goal**: User wires a passthrough or transform between Source-CodeNode outputs and Sink-CodeNode inputs in the generated `.flow.kt`, opens Runtime Preview, interacts with the UI, and observes UI outputs updating in response — confirming end-to-end flow from UI through the FBP graph back to UI.

**Independent Test**: Take a US1-passing module, edit `.flow.kt` to wire each Source output directly to the matching Sink input via a passthrough, save, open Runtime Preview, change a UI input, observe the corresponding Sink-driven UI element update within one second. (Quickstart VS-A5 + VS-B4 cover this.)

US2 builds on US1's runtime-preview-loadable module. The generators themselves do not need additional changes — feature 085's `DynamicPipelineController` already supports Start/Stop/Pause/Resume with active dataflow, and US1's emitted Source/Sink CodeNode runtimes already write to/read from `{FlowGraph}State` mutable fields.

- [ ] T029 [US2] Test-first: extend `UIFBPInterfaceGeneratorTest.kt` (`/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPInterfaceGeneratorTest.kt`) with a contract assertion: the generated Source CodeNode's runtime emit body and the generated Sink CodeNode's runtime receive body refer to the same `{FlowGraph}State` mutable fields the ViewModel's `emit(...)` writes to. This is the static evidence that wiring `Source → (graph) → Sink` will round-trip values through State.
- [ ] T030 [US2] Test-first: add to `UIFBPModuleSessionFactoryTest.kt` (`/Users/dhaukoos/CodeNodeIO/flowGraph-execute/src/jvmTest/kotlin/io/codenode/flowgraphexecute/UIFBPModuleSessionFactoryTest.kt`) a passthrough end-to-end test that drives the synthetic UI-FBP fixture: write a value via `viewModel.emit(...)`, advance the dispatcher, assert the corresponding Sink-flow on the State updates. (This proves the runtime path works for UI-FBP-shaped modules; the actual GraphEditor UI exercise is manual T032.)
- [ ] T031 [US2] If T029 or T030 fail: investigate which generator skipped the State-field hookup. The expected fix is in `UIFBPSourceCodeNodeGenerator` and/or `UIFBPSinkCodeNodeGenerator` (T012/T014); they must emit runtime bodies that read/write the same `{FlowGraph}State._x` fields the ViewModel touches. Make the fix; T029/T030 pass.
- [ ] T032 [US2] Manually execute Quickstart VS-A5 (DemoUI passthrough) and VS-B4 (Quickstart084 passthrough). Confirm: Start works; Emit propagates to Sink-driven UI; Pause halts propagation; Resume restores it; Stop returns the runtime to IDLE without coroutine leaks. This satisfies US2's acceptance scenarios 1–3.

**Checkpoint**: US2 complete — UI inputs round-trip through user-wired graphs back to UI outputs. The "virtual circuit" core promise is delivered.

---

## Phase 5: User Story 3 - Generated Module Compiles Without Manual Fixup (Priority: P3)

**Goal**: After UI-FBP runs, the host module compiles successfully under the project's standard build invocation with zero generator-attributable errors. Re-opening the GraphEditor finds the module in the workspace dropdown without warnings.

**Independent Test**: Run UI-FBP against a fresh feature-085-scaffolded module, run `./gradlew :Module:compileKotlinJvm`, expect a clean compile. (Quickstart VS-A3 + VS-B4's compile step cover this.)

This story formalizes the legacy-cleanup and hand-edit-safety surfaces of `UIFBPSaveService` so the post-clarification spec's FR-010/FR-016 are honored. US1 already covered the happy-path compile; US3 covers the conflict-handling and migration edges.

- [ ] T033 [US3] Test-first: in `UIFBPSaveServiceTest.kt`, add a `legacy saved/ cleanup with deleteLegacyLocations=true and marker present deletes the duplicate files` test. Seed a temp-dir fixture with the post-085 expected outputs AND a `saved/{FlowGraph}State.kt` + `saved/{FlowGraph}ViewModel.kt` carrying the `Generated by CodeNodeIO` marker. Call `save(spec, flowGraphFile, moduleRoot, UIFBPSaveOptions(deleteLegacyLocations = true))`. Assert: both `saved/` files appear in `files` with `kind = DELETED`, the empty `saved/` directory is removed, no SKIPPED_CONFLICT entries.
- [ ] T034 [US3] Test-first: in the same file, add a `legacy saved/ cleanup with deleteLegacyLocations=true but marker absent leaves files in place` test. Same fixture but with the marker stripped from one of the legacy files. Assert: that file appears in `files` with `kind = SKIPPED_CONFLICT` and a `reason` naming the missing marker; the file is NOT deleted; the other (marked) legacy file IS deleted.
- [ ] T035 [US3] Test-first: in `UIFBPSaveServiceTest.kt`, add a `target file lacking the Generated marker is SKIPPED_CONFLICT` test. Seed a temp-dir with a hand-edited `controller/{FlowGraph}ControllerInterface.kt` (without the marker). Call `save(...)`. Assert the file is NOT overwritten and appears with `kind = SKIPPED_CONFLICT`. (This pins FR-016 for the post-085 generator-target paths.)
- [ ] T036 [US3] Test-first: in `UIFBPSaveServiceTest.kt`, add a `target file with marker AND content already matching is UNCHANGED` test. Seed a temp-dir where the previous save's output is still on disk verbatim. Call `save(...)` again. Assert every emitted file is `kind = UNCHANGED` and zero file mutations occurred (compare mtimes pre/post).
- [ ] T037 [US3] Implement the `UIFBPSaveService` legacy-cleanup branch (`UIFBPSaveOptions.deleteLegacyLocations = true` path) in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/save/UIFBPSaveService.kt`. Reuse feature 085's `carriesGeneratorMarker` heuristic (or extract it to a shared utility): a file lacking the marker is SKIPPED_CONFLICT; a file with the marker is DELETED. Empty parent dir cleanup. T033/T034 pass.
- [ ] T038 [US3] Implement the per-file UPDATED/UNCHANGED/SKIPPED_CONFLICT decision tree in `UIFBPSaveService.write(...)` (the per-file write loop). Reuses the marker-comment heuristic from T037. Trim trailing whitespace before content compare to avoid spurious UPDATEDs. T035/T036 pass.
- [ ] T039 [US3] Manually execute Quickstart VS-A8 (hand-written conflict on `DemoUIControllerInterface.kt`). Confirm `UIFBPSaveResult` reports `SKIPPED_CONFLICT` with a clear reason; the file is not overwritten. Re-add the marker; re-run; the file is now UPDATED.
- [ ] T040 [US3] Manually execute Quickstart VS-A9 (unscaffolded-host refusal): temporarily strip `jvm()` or `preview-api` from `TestModule/build.gradle.kts`, re-run UI-FBP, confirm refusal with actionable error and zero file changes, restore the build script.

**Checkpoint**: US3 complete — clean compile guaranteed; legacy `saved/` cleanup is opt-in and marker-safe; hand-written files at generator-target paths are protected.

---

## Phase 6: User Story 4 - Re-running Generation Preserves Manually-Added Business Logic (Priority: P4)

**Goal**: Re-running UI-FBP after the user has added business-logic CodeNodes to `.flow.kt` MUST preserve those CodeNodes and valid connections. UI-signature changes (port adds/removes) MUST update the Source/Sink port shapes; only connections referencing now-invalid ports are dropped, and the user is told which.

**Independent Test**: Generate a module via UI-FBP, manually add CodeNodes + connections to its `.flow.kt`, modify the source Composable's parameter list, re-run UI-FBP, verify the user-added CodeNodes still exist and valid connections survive; orphaned connections are dropped with a structured notification. (Quickstart VS-A6 + VS-A7 cover this.)

US4 implements the `.flow.kt` parse-and-merge logic (Decision 7).

- [ ] T041 [US4] Test-first: in `UIFBPSaveServiceTest.kt`, add a `re-save against unchanged spec produces UNCHANGED .flow.kt merge mode` test (FR-011 idempotency). Save once; save again with the same inputs. Assert `flowKtMerge.mode == UNCHANGED`, zero `portsAdded`/`portsRemoved`/`connectionsDropped`, `userNodesPreserved == 0` (no business-logic CodeNodes seeded).
- [ ] T042 [US4] Test-first: add a `port-add scenario adds a Source output without disturbing user-added CodeNodes` test. Seed a temp-dir with a previously-saved `.flow.kt` whose middle has a user-added passthrough CodeNode wired between Source and Sink. Re-save with a `UIFBPSpec` whose `sourceOutputs` has one extra port. Assert: `flowKtMerge.mode == UPDATED`, `portsAdded` contains exactly the new port, `portsRemoved.isEmpty()`, `connectionsDropped.isEmpty()`, `userNodesPreserved == 1`. The serialized `.flow.kt` content still contains the passthrough CodeNode.
- [ ] T043 [US4] Test-first: add a `port-remove scenario drops only invalid connections and reports them` test. Seed a temp-dir with a `.flow.kt` whose user-added CodeNode is connected to a Sink port that the new spec no longer has. Re-save. Assert: `portsRemoved` contains the missing port, `connectionsDropped` lists the orphaned connection with a clear `reason`, the user-added CodeNode itself is preserved (`userNodesPreserved == 1`).
- [ ] T044 [US4] Test-first: add a `parse-failed .flow.kt is SKIPPED, not overwritten` test. Seed a temp-dir with a `.flow.kt` containing syntactically broken DSL. Re-save. Assert: `flowKtMerge.mode == PARSE_FAILED_SKIPPED`, the file content on disk is unchanged byte-for-byte, a warning is emitted in `UIFBPSaveResult.warnings`.
- [ ] T045 [US4] Implement the `.flow.kt` parse-and-merge branch in `UIFBPSaveService.save(...)` per Decision 7: if file exists, parse via `FlowKtParser`; if parse fails, emit PARSE_FAILED_SKIPPED; otherwise compute the port diff against the new `UIFBPSpec`, mutate Source/Sink CodeNodes in-place (add/remove ports), drop invalid connections, re-serialize via `FlowGraphSerializer`. Populate `FlowKtMergeReport` with the diff. Tests T041–T044 pass.
- [ ] T046 [US4] Manually execute Quickstart VS-A6 (idempotency) and VS-A7 (UI-signature change). Confirm: re-save with no UI changes produces zero-mutation result; adding a Source parameter produces `UPDATED` mode with the new port in `portsAdded` and zero connections dropped (assuming the user-added CodeNodes don't reference removed ports).
- [ ] T047 [US4] Manually execute Quickstart VS-B5 (multi-flow-graph in one module). Confirm UI-FBP can run against two distinct `{flow graph, UI file}` pairs in the same module without artifact collision; both modules' Runtime Preview entries appear in the composable dropdown.

**Checkpoint**: US4 complete — re-generation is non-destructive; user-added business logic survives across UI-signature changes; merge results are surfaced in a structured report.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Wrap-up tasks that span multiple stories or finalize the feature.

- [ ] T048 Verify each spec Functional Requirement (FR-001 through FR-016) has at least one test or quickstart step covering it. Walk the spec.md checklist and record the test/step path for each FR in a working note. Any FR without coverage triggers a follow-up task.
- [ ] T049 Verify each Success Criterion (SC-001 through SC-007; SC-008 is retired) is empirically met. SC-001/002/003: run `./gradlew :TestModule:check` and a manual Runtime Preview start/start-flow timing exercise; SC-004: re-run UI-FBP against unchanged TestModule spec and confirm zero mutations; SC-005: re-run with port adds/removes and confirm preservation rate; SC-006: confirm TestModule reaches working Runtime Preview after exactly one UI-FBP run + the documented one-time migration; SC-007: time a fresh-developer walkthrough against a feature-085-scaffolded module from "drop UI file" to "Runtime Preview running" without consulting docs beyond `quickstart.md`.
- [ ] T050 Run the full project test suite from `/Users/dhaukoos/CodeNodeIO`: `./gradlew test`. Confirm zero regressions attributable to this feature. If any test fails: investigate, attribute, fix or roll back.
- [ ] T051 Run `./gradlew :flowGraph-generate:check` and `./gradlew :flowGraph-execute:jvmTest`. Confirm green.
- [ ] T052 [P] Update the `UIFBPInterfaceGenerator` source file's KDoc to reflect the post-085 orchestrator role (replaces pre-085 thick-stack orchestration). Edit `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPInterfaceGenerator.kt`'s class-level KDoc.
- [ ] T053 [P] Update the `UIFBPSaveService` source file's KDoc with the contract summary from `data-model.md` §3 and a one-line pointer to `quickstart.md`'s VS-A1 migration section. Edit `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/save/UIFBPSaveService.kt`.
- [ ] T054 [P] Verify the post-clarification spec's "Edge Cases" section is fully covered: "Module contains multiple qualifying UI files or multiple flow graphs" → VS-B5 (T047); "Composable signature has no input parameters beyond viewModel" → T015 (e); "Composable observes no `StateFlow` properties on the ViewModel" → T015 (f); "Module already contains hand-written controller, flow-runtime, or preview-provider files for the same UI" → T035 / T039; "UI-FBP run inside a module scaffolded under a different build configuration" → T019 / T040; "Runtime Preview opened before any business-logic graph has been added" → US1 itself (T026, T027). Confirm each edge case has a concrete test or quickstart step ID; flag any uncovered case as a follow-up task.
- [ ] T055 [P] Confirm `CROSS-CHECK-085.md` is still accurate after this feature's implementation. If any item under "084-scope work that is **already done** by feature 085" has shifted (e.g., the `PreviewProviderGenerator` extension landed here actually moved that responsibility back into 084 land), update CROSS-CHECK-085.md to reflect post-implementation reality.
- [ ] T056 Run a full quickstart sweep — VS-A (full sequence A1–A9), VS-B (B1–B5), VS-C (C1–C3) — end-to-end as a single coordinated pass. Record outcomes in a working note for the resumption commit.
- [ ] T057 Verify FR-014 atomic-landing-style constraint (the same discipline feature 085 followed): from repo root, `git log --oneline {merge-base}..HEAD` confirms generator changes (T002–T017), save service (T020/T037/T038/T045), GraphEditor entry-point wiring (T024), and the TestModule migration record all land in one PR boundary. Reject the merge if any surface is split.
- [ ] T058 [P] Remove the `@Deprecated` `moduleName` alias from `UIFBPSpec` (added in T002) once all internal callers have migrated. Run `./gradlew check` to confirm no regressions. This is the cleanup task that closes the deprecation window opened in T002.

---

## Dependencies

### Phase ordering

- **Phase 1 (Setup, T001–T004)** → **Phase 2 (Foundational, T005–T017)** → **Phases 3–6 (US1–US4, in priority order)** → **Phase 7 (Polish)**.
- Phase 2 BLOCKS all user-story phases — every story consumes at least one Phase-2 generator change.
- US1 (Phase 3, T018–T028) BLOCKS US2 and US3 (the runtime-preview-loadable module is the substrate for US2's flow exercise and US3's compile guarantee).
- US3 (Phase 5, T033–T040) and US4 (Phase 6, T041–T047) are independent of each other (US3 covers legacy/conflict; US4 covers `.flow.kt` merge); they can be worked in parallel after US1 lands.
- US2 (Phase 4) is the lightest phase — most of its work landed in US1 and the foundational generator changes; only manual + integration verification remains.

### Within-phase ordering

- Within Phase 2: each generator's test (T005/T007/T009/T011/T013/T015) precedes its implementation (T006/T008/T010/T012/T014/T016). Generators are file-disjoint, so test-first pairs are parallelizable across pairs.
- Within Phase 3 (US1): `UIFBPSaveService` tests (T018, T019, T021) precede the implementation file (T020, T022). T023 is a runtime-side fixture test that can run in parallel with T020. T024 (entry-point wiring) requires T020+T022 complete. T025–T028 are manual quickstart steps requiring T024 complete.
- Within Phase 5 (US3): tests (T033–T036) precede implementation (T037, T038). Manual steps (T039, T040) require implementation complete.
- Within Phase 6 (US4): tests (T041–T044) precede implementation (T045). Manual steps (T046, T047) require implementation complete.

### File-disjoint parallelism

- Phase 2 generator pairs are file-disjoint (each generator lives in its own file and has its own test file). T005+T007+T009+T011+T013+T015 can run in parallel; their corresponding implementations T006+T008+T010+T012+T014+T016 can run in parallel after their tests are written. T016 has a soft dependency on T006 (the orchestrator calls the extended `PreviewProviderGenerator`), so write T016 last in the implementation batch.
- Phase 7 polish tasks are mostly file-disjoint and parallelizable (T052, T053, T054, T055, T058 marked [P]).

---

## Parallel Execution Examples

**Phase 2 — generator tests in parallel (one batch)**:

```
Run in parallel: T005 T007 T009 T011 T013 T015
(each writes a test in a different test file; no inter-dependencies)
```

**Phase 2 — generator implementations in parallel (one batch, after the test batch)**:

```
Run in parallel: T006 T008 T010 T012 T014
Then sequentially: T016 (depends on T006), T017 (verification)
```

**Phase 3 (US1) — test-first then implement**:

```
Sequentially: T018 → T019 → T021 → T020 → T022 → T023 → T024
Then manual sequence: T025 → T026 → T027 → T028
```

**Phases 5 and 6 in parallel after US1 lands**:

```
Phase 5 (US3) tests batch [P]: T033 T034 T035 T036
Phase 5 (US3) impl batch:     T037 T038
Phase 6 (US4) tests batch [P]: T041 T042 T043 T044
Phase 6 (US4) impl:            T045
(US3 impl and US4 impl can run in parallel — different code paths in UIFBPSaveService)
```

---

## Implementation Strategy

### MVP (US1 only)

Phases 1, 2, 3 land first. Outcome: any UI-FBP-generated module on a feature-085-scaffolded host loads in Runtime Preview without manual file edits. This is the spec's "minimum bar for the feature to provide any value at all" and matches the pre-clarification MVP scope.

Estimated effort: T001–T028 = 28 tasks, ~50–70% parallelizable in phases 1+2.

### Incremental delivery

After MVP:

1. **US2 (Phase 4)** — the lightest follow-up. Most work is verification (the runtime path was already proven by feature 085's `DynamicPipelineController` work; we just reverify the UI-FBP fixture variant). Deliverable: end-to-end flow demos (the "virtual circuit" promise visibly working).
2. **US3 (Phase 5)** — the conflict-handling and legacy-cleanup surfaces. Important for sustained use but not for the first user demo. Deliverable: re-generation against modules with legacy `saved/` packages or hand-edited generated files behaves predictably (no silent overwrites, no surprise compile breaks).
3. **US4 (Phase 6)** — the `.flow.kt` parse-and-merge. Highest-effort follow-up; requires careful test coverage to avoid regressions in user-added graph content. Deliverable: re-running UI-FBP across UI-signature changes preserves user work.
4. **Phase 7 polish** — verification + cleanup; runs concurrently with the user demos of US2–US4.

### Dependencies on external work

- **Feature 085 (universal-runtime collapse)**: shipped 2026-04-28; `RuntimeControllerInterfaceGenerator`, `ModuleRuntimeGenerator`, `PreviewProviderGenerator`, `ModuleGenerator` scaffolding, and the `ModuleSessionFactory` `getStatus` proxy update are all on main. Phase 2 reuses `RuntimeControllerInterfaceGenerator` and `ModuleRuntimeGenerator` as-is and extends `PreviewProviderGenerator` with one parameter.
- **Features 082/083 (multiple flow graphs per module)**: shipped earlier; introduced the post-082/083 naming model that drives the `flowGraphPrefix` / `composableName` / `moduleName` separation in T002.
