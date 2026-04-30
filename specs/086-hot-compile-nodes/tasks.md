---
description: "Task list for feature 086-hot-compile-nodes"
---

# Tasks: Single-Session Generate → Execute (Hot-Compile Nodes)

**Input**: Design documents from `/specs/086-hot-compile-nodes/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/ ✓, quickstart.md ✓

**Tests**: TDD is mandatory per the project Constitution (`.specify/memory/constitution.md` §II). Every implementation task in this list is preceded by a Red test that MUST be written first and MUST fail before implementation begins.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

This feature touches three existing modules — no new Gradle module is introduced.

- **`flowGraph-inspect/`**: home of `InProcessCompiler`, `ClassloaderScope`, `NodeDefinitionRegistry` v2, `SessionCompileCache`, `ClasspathSnapshot` and the foundational `commonMain` types (per `plan.md` "Source Code" tree).
- **`graphEditor/`**: home of `RecompileSession`, `RecompileFeedbackPublisher`, `PipelineQuiescer`, `RecompileViewModel`, `RecompileButton`, and the `NodeGeneratorViewModel` hook.
- **`flowGraph-execute/`**: receives a one-line registry-aware lookup change in `ModuleSessionFactory`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, dependency wiring, license-gate verification.

- [X] T001 Add `implementation(kotlin("compiler-embeddable"))` to `flowGraph-inspect/build.gradle.kts`'s `jvmMain` source set; mirror the version pin already used by `flowGraph-generate/build.gradle.kts:42`.
- [X] T002 Create the new package directory `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/compile/` (and the parallel `commonMain` and `jvmTest` paths) so subsequent tasks can drop new files into a known location.
- [X] T003 Verify license-audit cleanliness (Constitution licensing gate). Run completed: `./gradlew :flowGraph-inspect:dependencies --configuration jvmRuntimeClasspath` resolved 75 distinct coordinates; GPL/LGPL/AGPL grep returned 0 matches. The borderline `org.jetbrains.intellij.deps:trove4j` is JetBrains' Apache 2.0 fork (not the original LGPL Trove4j). Audit output saved to `specs/086-hot-compile-nodes/license-audit-T003.txt` for reproducibility.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Cross-cutting types, compile-and-load infrastructure, and registry resolution-precedence change. EVERY user story consumes these — they MUST land first.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Tests for Foundational (TDD — Red phase)

> Constitution §II mandates that each test below is written FIRST and verified FAILING before its implementation task is started.

- [X] T004 [P] Test `CompileSource` data-class shape in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/commonTest/kotlin/io/codenode/flowgraphinspect/compile/CompileSourceTest.kt` (constructibility; `tier` and `hostModuleName` consistency rules — UNIVERSAL tier permits null host module; MODULE/PROJECT tiers require non-null).
- [X] T005 [P] Test `CompileUnit` sealed-class shape in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/commonTest/kotlin/io/codenode/flowgraphinspect/compile/CompileUnitTest.kt` (`SingleFile.sources == [source]`; `Module.sources` non-empty invariant; `description` formats are user-readable).
- [X] T006 [P] Test `CompileDiagnostic.formatForConsole()` in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/commonTest/kotlin/io/codenode/flowgraphinspect/compile/CompileDiagnosticTest.kt` (file:line prefix when `filePath` and `line>0`; bare message when `filePath==null`; preserves multi-line messages).
- [X] T007 [P] Test `CompileResult` invariants in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/commonTest/kotlin/io/codenode/flowgraphinspect/compile/CompileResultTest.kt` (`Failure.diagnostics.any { severity == ERROR }` is true; `Success.loadedDefinitionsByName` is non-empty for both unit shapes).
- [X] T008 [P] Test `RecompileResult` derived properties in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/commonTest/kotlin/io/codenode/flowgraphinspect/compile/RecompileResultTest.kt` (`success` derives correctly from `compileResult` variant; `unit` accessor returns `compileResult.unit`).
- [X] T009 [P] Test `ClasspathSnapshot` in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/jvmTest/kotlin/io/codenode/flowgraphinspect/compile/ClasspathSnapshotTest.kt` (reads a fixture `grapheditor-runtime-classpath.txt` from a tmpdir; preserves entry order; falls back to `System.getProperty("java.class.path")` when the file is missing; surfaces a warning on fallback).
- [X] T010 [P] Test `SessionCompileCache` in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/jvmTest/kotlin/io/codenode/flowgraphinspect/compile/SessionCompileCacheTest.kt` (`allocate(unit)` returns a fresh empty subdirectory; consecutive calls for the same unit return DIFFERENT subdirectories; `deleteAll()` removes the root tree).
- [X] T011 [P] Test `ChildFirstURLClassLoader` in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/jvmTest/kotlin/io/codenode/flowgraphinspect/compile/ChildFirstURLClassLoaderTest.kt` (a class in an `ownedPackages` prefix loads from the local URLs even when the same FQCN is on the parent classpath; classes outside the owned set delegate to parent).
- [X] T012 [P] Test `ClassloaderScope` in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/jvmTest/kotlin/io/codenode/flowgraphinspect/compile/ClassloaderScopeTest.kt` (`loadDefinition(fqcn)` returns a `CodeNodeDefinition` instance for a fixture `.class` directory; returns null for missing FQCNs; `close()` is idempotent).
- [X] T013 [P] Test `InProcessCompiler` in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/jvmTest/kotlin/io/codenode/flowgraphinspect/compile/InProcessCompilerTest.kt`. Cover every case from `contracts/in-process-compiler.md` "Test contract" table: `single-valid-source`, `single-broken-source`, `module-with-cross-reference`, `module-where-one-file-fails`, `module-empty-classpath`, `compile-twice-same-unit-different-content`, `large-source-warmup-cost`.
- [X] T014 [P] Test `NodeDefinitionRegistry` v2 modifications in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/jvmTest/kotlin/io/codenode/flowgraphinspect/registry/NodeDefinitionRegistryV2Test.kt`. Cover every case from `contracts/node-definition-registry-v2.md` "Test contract" table: `getByName-falls-back-to-launchtime-when-no-session-install`, `getByName-prefers-session-install-over-launchtime`, `revertSessionDefinition-falls-back-to-launchtime`, `installSessionDefinition-twice-replaces-prior-strong-reference` (uses `WeakReference` + `System.gc()` to verify scope is collectible), `getByName-returns-null-for-unknown-name`, `getByName-of-template-only-node-returns-null`.
- [X] T015 [P] Test `RecompileFeedbackPublisher` in `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/compile/RecompileFeedbackPublisherTest.kt` (one `CompileDiagnostic` → exactly one `ErrorConsoleEntry`; the entry's `source` field is `"Compile"`; the entry's `message` carries `formatForConsole()` output; success-only recompiles produce no entries).
- [X] T016 [P] Test `PipelineQuiescer` stub behavior in `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/compile/PipelineQuiescerTest.kt` (`stopAll()` returns 0 when no pipelines registered; returns 1 after a `register(controller)` call; the controller's `stop()` is invoked).
- [X] T017 Test `RecompileSession` core flow in `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/compile/RecompileSessionTest.kt`. Cover the cases from `contracts/recompile-session.md` that don't require a real Node Generator wiring: `serial-mutex-blocks-concurrent-recompile`, `failure-leaves-prior-install-intact`, `running-pipeline-is-stopped-before-compile`, `feedback-published-on-every-attempt`. (Generator-wiring cases land in US1 tests.)
- [X] T017a [P] **(added by /speckit.analyze remediation H1)** Test FR-006 "no auto-recompile on save/edit". In `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/compile/RecompileNoAutoTriggerTest.kt`, simulate a sequence of file-save events through the existing `CodeEditorViewModel.save()` path against a fixture module; assert the recorded count of `RecompileSession.recompile(Module(...))` invocations is exactly 0. Use a fake `RecompileSession` that records calls. The intent is a regression guard against a future PR silently wiring auto-recompile into a save handler.
- [X] T017b [P] **(added by /speckit.analyze remediation H2)** Test FR-011 "no cross-module side effects" in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/jvmTest/kotlin/io/codenode/flowgraphinspect/registry/CrossModuleIsolationTest.kt`. Install session definitions for two distinct fixture modules A and B in `NodeDefinitionRegistry`; recompile module A (replacing A's session install with a new `ClassloaderScope`); assert (a) module B's session install is untouched (same `ClassloaderScope` reference, same definition instance identity), and (b) `getByName` of every node in B continues to return its session-install instance.
- [X] T017c [P] **(added by /speckit.analyze remediation H3)** Test FR-016 "compile error in unit X doesn't break work on unit Y". In `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/compile/CrossUnitFailureIsolationTest.kt`: install session definitions for fixture modules A and B successfully; recompile module A with a fixture source containing a syntax error; assert (a) the recompile returns `RecompileResult.success == false`, (b) module A's prior session install is intact (FR-013 — verified via `getByName`), (c) module B's session install is intact (FR-011 — same scope reference), (d) a Runtime Preview against a fixture flow graph that uses only module B's nodes can still build (`DynamicPipelineBuilder.canBuildDynamic` returns true).

### Implementation for Foundational

- [X] T018 [P] Implement `CompileSource` in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/commonMain/kotlin/io/codenode/flowgraphinspect/compile/CompileSource.kt`. T004 passes. (Landed alongside T004 — data class impls are trivial and inseparable from the test that pins them.)
- [X] T019 [P] Implement `CompileUnit` (sealed class with `SingleFile` and `Module` variants) in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/commonMain/kotlin/io/codenode/flowgraphinspect/compile/CompileUnit.kt`. T005 passes. (Landed alongside T005.)
- [X] T020 [P] Implement `CompileDiagnostic` (with `Severity` enum and `formatForConsole()`) in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/commonMain/kotlin/io/codenode/flowgraphinspect/compile/CompileDiagnostic.kt`. T006 passes. (Landed alongside T006.)
- [X] T021 Implement `CompileResult` (sealed class with `Success` / `Failure` variants) in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/commonMain/kotlin/io/codenode/flowgraphinspect/compile/CompileResult.kt`. Depends on T018, T019, T020. T007 passes. (Landed alongside T007.)
- [X] T022 Implement `RecompileResult` in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/commonMain/kotlin/io/codenode/flowgraphinspect/compile/RecompileResult.kt`. Depends on T021. T008 passes. (Landed alongside T008.)
- [X] T023 [P] Implement `ClasspathSnapshot` in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/compile/ClasspathSnapshot.kt`. Reads `${projectRoot}/build/grapheditor-runtime-classpath.txt`; falls back to `System.getProperty("java.class.path")` (newline-split) with a one-time stderr warning. T009 passes.
- [X] T024 [P] Implement `SessionCompileCache` in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/compile/SessionCompileCache.kt`. Root dir is `~/.codenode/cache/sessions/{uuid}/`; per-unit subdirs are `units/{slug}-{counter}/` to ensure uniqueness across consecutive `allocate(unit)` calls. T010 passes.
- [X] T025 Implement `ChildFirstURLClassLoader` in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/compile/ChildFirstURLClassLoader.kt`. Override `loadClass(name, resolve)` to try local URLs FIRST when the FQCN's package matches any entry in `ownedPackages`, else delegate to super. T011 passes.
- [X] T026 Implement `ClassloaderScope` in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/compile/ClassloaderScope.kt`. Wraps a `ChildFirstURLClassLoader`; `loadDefinition(fqcn)` reflectively reads the Kotlin singleton's `INSTANCE` field. Depends on T025. T012 passes.
- [X] T027 Implement `InProcessCompiler.compile(unit)` in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/compile/InProcessCompiler.kt`. Builds `K2JVMCompilerArguments`, invokes `K2JVMCompiler().exec(...)`, captures `MessageCollector` callbacks into `CompileDiagnostic`s, walks the output dir to populate `loadedDefinitionsByName`. Depends on T021, T023, T024. T013 passes.
- [X] T028 Modify `NodeDefinitionRegistry` per `contracts/node-definition-registry-v2.md`: add `installSessionDefinition`, `revertSessionDefinition`, change `getByName` precedence (session install → launch-time → null). Edit `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/registry/NodeDefinitionRegistry.kt`. T014 passes.
- [X] T029 Implement `PipelineQuiescer` (stub returning 0 + `register(controller)` API) in `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/compile/PipelineQuiescer.kt`. The full RuntimePreviewPanel-aware implementation lands in US2 (T042); the foundational stub is sufficient for US1's per-file path which never has a running pipeline. T016 passes.
- [X] T030 Implement `RecompileFeedbackPublisher` in `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/compile/RecompileFeedbackPublisher.kt`. Maps each `CompileDiagnostic` to one `ErrorConsoleEntry` with `source = "Compile"` and `message = diagnostic.formatForConsole()`. Skips on Success-with-no-warnings. T015 passes.
- [X] T031 Implement `RecompileSession` core in `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/compile/RecompileSession.kt` per `contracts/recompile-session.md` "Behavior" sections. Includes both `recompile(unit)` and `recompileGenerated(...)` entry points; both delegate to the same internal coroutine after wrapping the source in the appropriate `CompileUnit`. T017 passes.
- [X] T032 Modify `ModuleSessionFactory` to consult `NodeDefinitionRegistry`'s session installs. The factory's lookup-injection seam at `ModuleSessionFactory.kt:76` (`{ name -> reg.getByName(name) }`) already routes through `getByName`, so T028's resolution-precedence change propagates transparently — no source change required. Regression test added at `/Users/dhaukoos/CodeNodeIO/flowGraph-execute/src/jvmTest/kotlin/io/codenode/flowgraphexecute/ModuleSessionFactoryRegistryV2Test.kt` proving the precedence and validating the seam.

**Checkpoint**: Foundational phase complete. The compile-and-load + registry pipeline can resolve in-session definitions; user story phases can begin.

---

## Phase 3: User Story 1 - Generate a Node and See It on the Palette Immediately (Priority: P1) 🎯 MVP

**Goal**: Per-file automatic compile fires after the Node Generator writes a new CodeNode source file (FR-001 / FR-003). The new node appears on the Node Palette with a usable placeholder definition before the user does anything else.

**Independent Test**: Open the GraphEditor against a project. Use the Node Generator to create a new Module-tier CodeNode. Without quitting or running any external Gradle task, the new node appears on the Node Palette under its declared name within 3 seconds (SC-001), can be dragged onto the canvas, and exposes its declared input/output ports.

### Tests for User Story 1 (TDD — Red phase)

- [X] T033 [P] [US1] Test the `NodeGeneratorViewModel`-to-`RecompileSession` hook in `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/jvmTest/kotlin/io/codenode/flowgraphgenerate/viewmodel/NodeGeneratorAutoCompileTest.kt`: after `NodeGeneratorViewModel.generateCodeNode()` writes a fixture source file to a tmpdir, the `NodeAutoCompileHook` is invoked exactly once with the right `(file, tier, hostModule)` triple. (The viewmodel lives in `flowGraph-generate`, not `graphEditor` as the contract originally suggested — placement adjusted because graphEditor depends on flowGraph-generate, not the reverse. The hook is a SAM interface; graphEditor wires the real RecompileSession-backed implementation. Recording-fake tests verify the wiring; integration test in T034 covers the real RecompileSession path.)
- [X] T034 [US1] End-to-end integration test in `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/compile/GenerateAndPaletteIntegrationTest.kt`: real `NodeGeneratorViewModel` + real `RecompileSession` + real `NodeDefinitionRegistry` against a temp module dir; assert (a) the generated source file exists; (b) the recompile completes within 30s; (c) `NodeDefinitionRegistry.getByName(generatedName)` returns a non-null `CodeNodeDefinition`; (d) `createRuntime(name)` returns a non-null `NodeRuntime`; (e) `registry.version` ticks on session install (drives palette refresh — T036).

### Implementation for User Story 1

- [X] T035 [US1] Wire `NodeGeneratorViewModel.generateCodeNode()` to invoke a new `NodeAutoCompileHook` after the source file is written. Introduced `NodeAutoCompileHook` (SAM interface in flowGraph-generate jvmMain) so the viewmodel doesn't depend on graphEditor; the GraphEditor's DI passes a real implementation that delegates to `RecompileSession.recompileGenerated(...)` via a background `CoroutineScope`. Edited `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/viewmodel/NodeGeneratorViewModel.kt` (added `autoCompileHook` ctor param; replaced the post-write status message; defends against synchronous hook throws), `/Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/viewmodel/NodeAutoCompileHook.kt` (new), and `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorApp.kt` (constructs `RecompileSession` + bridges the hook). T033 passes.
- [X] T036 [US1] Add a registry-change broadcast so the Node Palette UI refreshes after `installSessionDefinition`. Added `version: StateFlow<Long>` to `NodeDefinitionRegistry`; bumped on every `discoverAll`, `register`, `registerTemplate`, `scanDirectory` (when changed), `installSessionDefinition`, and `revertSessionDefinition` (when actually removed). Wired `GraphEditorApp` to collect `registry.version.drop(1)` and bump `editorState.registryVersion` (the existing recompose key) on every tick; the palette already listens via `remember(registryVersion)`. T034 passes.
- [ ] T037 [US1] Manually execute Quickstart `VS-A1`, `VS-A2`, and `VS-A3` against `CodeNodeIO-DemoProject/TestModule/`. Confirm the new node appears on the palette within 3 seconds (SC-001), is drag-droppable, exposes its ports, and that connecting it + saving the flow graph round-trips the chosen IP types into `.flow.kt`.

**Checkpoint**: US1 complete. Generate-and-see-on-palette works end-to-end; the foundational chain is exercised by a real Node Generator invocation.

---

## Phase 4: User Story 2 - Module Recompile + Runtime Preview (Priority: P2)

**Goal**: A user-invoked per-module recompile produces an atomic compile unit; a successful recompile installs every definition into `NodeDefinitionRegistry`; the next Runtime Preview run uses the recompiled behavior (FR-004..FR-007).

**Independent Test**: Open a flow graph whose CodeNodes have been generated (US1) and edited in this session. Click the "Recompile module" UI control (built in US3 — but for US2 verification we invoke `RecompileSession.recompile(...)` directly via test). After a successful recompile, click Start in Runtime Preview; the pipeline executes the user's edited business logic.

### Tests for User Story 2 (TDD — Red phase)

- [ ] T038 [P] [US2] Test `PipelineQuiescer` integration with `RuntimePreviewPanel` state in `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/compile/PipelineQuiescerIntegrationTest.kt`: register a real `DynamicPipelineController`; invoke `stopAll()`; assert the controller transitioned to `IDLE` and the `pipelinesQuiesced` count is 1.
- [ ] T039 [US2] Test `RecompileSession.recompile(Module)` in `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/compile/RecompileSessionModuleTest.kt`. Cover the contract cases from `contracts/recompile-session.md` "Test contract" that the foundational T017 deferred: `module-recompile-supersedes-prior-install`, `serial-mutex-blocks-concurrent-recompile` (Module variant), and an intra-module-cross-reference scenario where module A has two files and the second imports a class from the first — assert both classes appear in `loadedDefinitionsByName` and that the cross-reference resolves at registry-install time.
- [ ] T040 [P] [US2] Test the module-source-discovery utility in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/jvmTest/kotlin/io/codenode/flowgraphinspect/compile/ModuleSourceDiscoveryTest.kt`: walks a fixture module directory tree (`src/commonMain/kotlin/.../nodes/` and `src/jvmMain/kotlin/.../nodes/`) for `.kt` files; returns a `CompileUnit.Module` whose `sources` list matches the fixture's files; tier is correctly inferred from the module's location.

### Implementation for User Story 2

- [ ] T041 [US2] Implement `ModuleSourceDiscovery` in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/compile/ModuleSourceDiscovery.kt`. `forModule(moduleDir, moduleName, tier)` returns a `CompileUnit.Module` covering every `.kt` file under that module's CodeNode source directories (`nodes/` subtrees in commonMain + jvmMain). For Universal tier, the synthetic "module" is `~/.codenode/nodes/`. T040 passes.
- [ ] T042 [US2] Replace the `PipelineQuiescer` stub from T029 with the real RuntimePreviewPanel-aware implementation. Track active `DynamicPipelineController` instances at the GraphEditor session level (a registry the panel writes to on Start, removes from on Stop); `stopAll()` calls `.stop()` on each tracked controller. Edit `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/compile/PipelineQuiescer.kt` and the relevant `RuntimePreviewPanel` (or its viewmodel). T038 passes.
- [ ] T043 [US2] Verify and harden `RecompileSession.recompile(Module)`. The foundational `RecompileSession` (T031) handles the `recompile(unit)` API uniformly across `SingleFile` and `Module`; this task validates the Module path end-to-end with the now-real `PipelineQuiescer` and `ModuleSourceDiscovery`. Edit `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/compile/RecompileSession.kt` only if T039's tests reveal gaps in the foundational implementation. T039 passes.
- [ ] T044 [US2] Manually execute Quickstart `VS-A4`, `VS-A5`, `VS-A6`, and `VS-A7` against TestModule. Confirm: edited node sources are picked up by per-module recompile; intra-module cross-references resolve; running pipeline is stopped automatically (FR-014) with a count surfaced in the recompile feedback; Runtime Preview against the saved flow graph executes the new logic within the SC-002 budget (10s).

**Checkpoint**: US2 complete. Generate → wire → edit → recompile module → Runtime Preview is fully functional within one GraphEditor session, no Gradle invocation.

---

## Phase 5: User Story 3 - Recompile UI Surface and Diagnostics (Priority: P3)

**Goal**: A discoverable, low-friction toolbar control triggers per-module recompilation; feedback is structured (which module, which sources succeeded, which failed), surfaces in the existing `ErrorConsolePanel`, and reaches Module / Project / Universal tiers uniformly (FR-008..FR-011).

**Independent Test**: After editing a node source, locate and invoke the "Recompile module: TestModule" action in the GraphEditor UI. The action's outcome — success or specific compilation errors — is visibly surfaced (status line for success summary; `ErrorConsolePanel` for compile errors). Recompile reach for a Project-tier or Universal-tier node uses the same UI affordance.

### Tests for User Story 3 (TDD — Red phase)

- [ ] T045 [P] [US3] Test `RecompileViewModel` state-machine in `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/viewmodel/RecompileViewModelTest.kt`: idle → compiling → success/failure transitions; the `targetUnit` state property is non-null while compiling and reverts to null after; concurrent invocations queue (no race).
- [ ] T046 [P] [US3] Test `RecompileButton` rendering and click behavior in `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/ui/RecompileButtonTest.kt`: the button label includes the target module name (`"Recompile module: TestModule"`); clicking invokes the viewmodel's `recompile(...)` exactly once; while compiling, the button shows a busy state and is disabled.
- [ ] T047 [P] [US3] Test tier-aware target resolution in `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/compile/RecompileTargetResolverTest.kt`: a Module-tier source resolves to its host module's directory; a Project-tier source resolves to the project's `:nodes` shared module directory; a Universal-tier source resolves to `~/.codenode/nodes/` as a synthetic compile unit. The resolver returns a `CompileUnit.Module` ready for `RecompileSession`.

### Implementation for User Story 3

- [ ] T048 [P] [US3] Implement `RecompileViewModel` in `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/viewmodel/RecompileViewModel.kt`. Owns the `idle/compiling/lastResult` state machine; exposes `recompile(target: RecompileTarget)`; subscribes to `RecompileFeedbackPublisher` for diagnostic delivery. T045 passes.
- [ ] T049 [P] [US3] Implement `RecompileButton` (Compose composable) in `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/RecompileButton.kt` with the target-module label and busy state per spec FR-008. Place in the main toolbar (`TopToolbar` or equivalent). T046 passes.
- [ ] T050 [P] [US3] Add a "Recompile module" action to `CodeEditorPanel`'s action area in `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/CodeEditorPanel.kt`. Action target derives from the currently-open file's host module. Both the toolbar button (T049) and this Code Editor action route through the same `RecompileViewModel` to ensure consistent feedback.
- [ ] T051 [US3] Implement `RecompileTargetResolver` in `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/compile/RecompileTargetResolver.kt`. Exposes a `resolveTarget(node: CodeNode | sourceFile: File): CompileUnit.Module` that uses tier metadata to route Module-tier nodes to their host module; Project-tier nodes to `:nodes`; Universal-tier nodes to `~/.codenode/nodes/`. Reused by both T049 and T050. T047 passes.
- [ ] T052 [US3] Manually execute Quickstart `VS-B1`, `VS-B2`, and `VS-B3` to verify the recompile UI works uniformly across all three placement tiers and that compile errors surface with file/line information sufficient to locate the offending source without external tools (SC-003 / SC-006).

**Checkpoint**: US3 complete. The recompile UI surface is discoverable, tier-aware, and produces actionable diagnostics for every failure mode.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Performance benchmarks, soak tests, license re-verification, KDoc, and an end-to-end quickstart sweep validating the whole feature on a real project.

- [ ] T053 [P] Memory soak test in `/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/compile/RecompileSoakTest.kt` (SC-004). Recompile the same fixture module 50 times sequentially; after each, run `System.gc()` + `Thread.sleep(100)`; assert the count of live `ClassloaderScope` instances (tracked via `WeakReference`) is at most 2 (current + one transitionally-pinned by GC lag). Caps GraphEditor heap growth at one module's-worth of class definitions over the run.
- [ ] T054 [P] Performance benchmark in `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/jvmTest/kotlin/io/codenode/flowgraphinspect/compile/InProcessCompilerBenchmark.kt`. Asserts post-warmup p90 ≤ 1.0s for `SingleFile` and p90 ≤ 5.0s for `Module` of 10 files (SC-001 / SC-002). Uses `kotlinx-benchmark` or a hand-rolled p90 calculator.
- [ ] T055 [P] First-invocation warmup verification (still part of the benchmark file from T054 or a sibling test): record duration of the very first compile in a fresh JVM; assert it is ≤ 6.0s. Establishes the warmup-cost expectation users see at GraphEditor startup.
- [ ] T056 [P] Re-run license audit. From repo root, `./gradlew :flowGraph-inspect:dependencies --configuration jvmRuntimeClasspath > /tmp/086-final-deps.txt` and `./gradlew :graphEditor:dependencies --configuration jvmRuntimeClasspath > /tmp/086-grapheditor-deps.txt`. Grep both for GPL/LGPL/AGPL artifacts. Document the cleanliness in a working note in the spec dir.
- [ ] T057 [P] KDoc updates on public surfaces: `InProcessCompiler` (file at `/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/compile/InProcessCompiler.kt`), `RecompileSession` (`/Users/dhaukoos/CodeNodeIO/graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/compile/RecompileSession.kt`), `NodeDefinitionRegistry` (`/Users/dhaukoos/CodeNodeIO/flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/registry/NodeDefinitionRegistry.kt`). Each gets a class-level KDoc summarizing its contract per its respective `contracts/*.md` file plus a one-line pointer to that contract.
- [ ] T058 Full quickstart sweep: execute every leg of `quickstart.md` end-to-end on `CodeNodeIO-DemoProject/TestModule/` — `VS-A1` through `VS-A8`, `VS-B1` through `VS-B3`, `VS-C1`, `VS-C2`. Record outcomes in a working note for the merge commit message. Any failed leg blocks the merge.
- [ ] T059 Atomic-landing verification (Constitution discipline applied to feature 086): from repo root, `git log --oneline {merge-base}..HEAD` confirms that compiler/classloader infra (T018–T031), registry-v2 (T028), `ModuleSessionFactory` change (T032), Node Generator hook (T035), and UI surface (T048–T051) all land on one branch boundary. Reject the merge if any surface is split across branches.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1, T001–T003)**: No dependencies — can start immediately.
- **Foundational (Phase 2, T004–T032)**: Depends on Setup completion. BLOCKS all user stories.
- **User Stories (Phase 3+)**: All depend on Foundational phase completion. US1, US2, US3 can be worked in parallel by different developers after foundational lands, though US1 is the natural MVP-first path.
- **Polish (Phase 6)**: Depends on all desired user stories being complete.

### User Story Dependencies

- **US1 (P1)**: Depends only on Foundational. The Node Generator hook (T035) is the only US-specific implementation; the rest of the chain (compile → install → palette refresh) lives in foundational.
- **US2 (P2)**: Depends only on Foundational. `RecompileSession.recompile(Module)` was implemented in foundational T031; US2 adds the real `PipelineQuiescer` (T042), the module-source-discovery utility (T041), and integration tests (T039) that the foundational stub didn't cover.
- **US3 (P3)**: Depends on Foundational. May visually integrate with US1 (palette refresh on success) and US2 (toolbar button triggers per-module compile), but is independently testable via the viewmodel + button tests.

### Within Each User Story

- Tests MUST be written FIRST (TDD per Constitution §II) and verified failing before implementation begins.
- Foundational types before infrastructure before integration.
- Within Foundational: the commonMain types (T018–T022) before the jvmMain consumers (T023–T032).
- Manual quickstart legs (T037, T044, T052) run last in their respective stories — they validate the full vertical slice.

### Parallel Opportunities

- All 14 Foundational tests (T004–T017) can run in parallel — different files.
- Foundational implementations: T018–T020 are parallel, then T021/T022 sequential (T021 depends on the data types). T023–T024 parallel. T025 then T026. T027 depends on T021 + T023 + T024. T028 parallel with T029–T031. T032 sequential after T028.
- US1/US2/US3 can be worked in parallel by different developers after foundational lands. Each story's tests can run in parallel within the story.
- All Polish tasks marked [P] (T053–T057) can run in parallel.

---

## Parallel Example: Foundational Tests (T004–T017)

```bash
# All 14 foundational tests run in parallel — different files, no inter-dependencies:
Task: "Test CompileSource in CompileSourceTest.kt"
Task: "Test CompileUnit in CompileUnitTest.kt"
Task: "Test CompileDiagnostic in CompileDiagnosticTest.kt"
Task: "Test CompileResult in CompileResultTest.kt"
Task: "Test RecompileResult in RecompileResultTest.kt"
Task: "Test ClasspathSnapshot in ClasspathSnapshotTest.kt"
Task: "Test SessionCompileCache in SessionCompileCacheTest.kt"
Task: "Test ChildFirstURLClassLoader in ChildFirstURLClassLoaderTest.kt"
Task: "Test ClassloaderScope in ClassloaderScopeTest.kt"
Task: "Test InProcessCompiler in InProcessCompilerTest.kt"
Task: "Test NodeDefinitionRegistry v2 in NodeDefinitionRegistryV2Test.kt"
Task: "Test RecompileFeedbackPublisher in RecompileFeedbackPublisherTest.kt"
Task: "Test PipelineQuiescer (stub) in PipelineQuiescerTest.kt"
Task: "Test RecompileSession in RecompileSessionTest.kt"
```

## Parallel Example: User Story 3 Tests (T045–T047)

```bash
# All US3 tests in parallel — different files:
Task: "Test RecompileViewModel state machine in RecompileViewModelTest.kt"
Task: "Test RecompileButton rendering + click in RecompileButtonTest.kt"
Task: "Test RecompileTargetResolver tier reach in RecompileTargetResolverTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 (Setup, T001–T003).
2. Complete Phase 2 (Foundational, T004–T032) — this is the bulk of the work.
3. Complete Phase 3 (US1, T033–T037).
4. **STOP and VALIDATE**: VS-A1, VS-A2, VS-A3 manual sweep against TestModule.
5. The MVP demonstrates: generate a node → see it on palette in <3s → wire it → save the flow graph. The "execute it via Runtime Preview" half waits for US2.

### Incremental Delivery

1. Setup + Foundational → infrastructure ready. (Long phase; ~30 tasks.)
2. US1 → MVP demo: generate → palette appearance → wire → save.
3. US2 → full canonical workflow: generate → wire → edit → recompile module → Runtime Preview.
4. US3 → polished UI: discoverable toolbar button + tier reach + diagnostic UX.
5. Polish → SC verification (memory, perf, licensing) + final quickstart sweep.

### Parallel Team Strategy

With multiple developers:

1. The team completes Setup + Foundational together (one developer per type cluster: commonMain types, classloader/compiler, registry, session/feedback).
2. Once Foundational is done:
   - Developer A: US1 (Node Generator hook + palette refresh).
   - Developer B: US2 (module-source discovery + PipelineQuiescer + integration tests).
   - Developer C: US3 (UI components + tier resolver).
3. Stories complete and integrate independently; Polish phase runs last with the whole team.

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks.
- [Story] label maps tasks to user stories for traceability.
- TDD per Constitution §II: every test task above is a Red-phase task — its corresponding implementation task is the Green phase. Refactor as needed in subsequent commits.
- Manual quickstart legs (T037, T044, T052, T058) run on a real project (`CodeNodeIO-DemoProject`) and are recorded in working notes for the merge commit.
- Constitution licensing gate: T003 (start) and T056 (re-verify) bracket every other task with a clean dependency-tree audit.
- Atomic-landing verification (T059) keeps this feature on one branch boundary, mirroring the discipline used by features 084 and 085.
