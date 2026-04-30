# Implementation Plan: Single-Session Generate → Execute (Hot-Compile Nodes)

**Branch**: `086-hot-compile-nodes` | **Date**: 2026-04-29 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/086-hot-compile-nodes/spec.md`

## Summary

Eliminate the rebuild-+-relaunch loop that today gates every "create a CodeNode and try it" exercise. Achieved by hot-compiling CodeNode source files in-process via the embedded Kotlin compiler (`kotlin-compiler-embeddable`) and loading the resulting bytecode through a session-scoped classloader. Two compile granularities, deliberately split along the user's canonical workflow:

1. **Per-file automatic compile on Node Generation** — fires automatically when the Node Generator (only) writes a new CodeNode source file. (UI-FBP code generation and Entity Module generation rely on 2. Per-module manual compile.) Output: a placeholder `CodeNodeDefinition` registered on the Node Palette so the user can drag it onto the canvas, choose port types graphically, and save the resulting `.flow.kt` — all without a Gradle invocation. Cost: ~1s for a single file.
2. **Per-module manual compile** — fires when the user clicks an explicit "Recompile module" control (toolbar / Code Editor area) before launching Runtime Preview. Output: every CodeNode source in the host module is compiled atomically; the resulting classloader supersedes the launch-time JAR for that module's nodes. Intra-module cross-references resolve correctly. Cost: ~3-5s for typical modules.

A registry-resolution-precedence change (in-session classloader → launch-time classpath → not found) ensures Runtime Preview always picks up the freshest bytecode.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (existing project standard).
**Primary Dependencies**:
- `kotlin-compiler-embeddable` 2.1.21 (Apache 2.0, JetBrains) — programmatic Kotlin compiler. Already a transitive dep of `flowGraph-generate` (used by the existing module-compilation test path), so no new top-level dependency is introduced for the GraphEditor classpath.
- Existing: `flowGraph-inspect.NodeDefinitionRegistry`, `flowGraph-execute.DynamicPipelineBuilder`, `graphEditor.ErrorConsolePanel` (feature 084), `graphEditor.NodeGeneratorViewModel`, `graphEditor.CodeEditorViewModel`.
**Storage**: Filesystem (compiled `.class` files written to a session-scoped temp directory under `~/.codenode/cache/sessions/{session-id}/`; the directory is best-effort cleaned on graceful shutdown). No database, no persistent state across sessions — by design (FR-018).
**Testing**: kotlin-test (existing). Compilation tests use real `kotlin-compiler-embeddable` against fixture sources in `jvmTest`. Classloader scoping verified with isolation fixtures.
**Target Platform**: JVM (Compose Desktop GraphEditor only). The hot-compile mechanism is JVM-only because `kotlin-compiler-embeddable` itself is JVM-only. KMP `commonMain` is untouched.
**Project Type**: Single application — the GraphEditor; with shared library code in existing `flowGraph-*` modules.
**Performance Goals**:
- Per-file compile: ≤ 1.0s p90 on a developer-class workstation (drives SC-001's 3-second palette-appearance budget).
- Per-module compile (≤ 10 source files): ≤ 5.0s p90 (drives SC-002's 10-second recompile-to-Preview-Start budget).
- Memory growth across 50 sequential recompiles of one module: ≤ size of one module's compiled class set (drives SC-004).
**Constraints**:
- No GraphEditor crash on compile failure (FR-013).
- Compile errors must surface via the existing `ErrorConsolePanel` (feature 084).
- No automatic recompile on save / on edit (FR-006). Per-module compile is exclusively user-invoked.
- Per-file auto-compile applies ONLY to source files emitted by the Node Generator (FR-001 / FR-012). UI-FBP code generation and Entity Module generation rely on the per-module manual recompile (US2). User-edited sources also go through per-module recompile.
- Apache 2.0 / permissive license compliance — verified via Phase 0 research for every new transitive.
**Scale/Scope**:
- Typical project: 5–15 modules; module size 1–15 CodeNode files; recompile cadence dozens per active authoring session.
- Concurrent compiles: at most one per moment (subsequent invocations enqueue or short-circuit; design choice in Phase 0).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Licensing & IP (KMP/Apache 2.0 standard)

- ✅ **Primary dependency `kotlin-compiler-embeddable`**: Apache 2.0 (JetBrains-aligned). No GPL/LGPL infection risk.
- ✅ **Header convention**: every new `.kt` and `.kts` file authored under this feature begins with the project's standard Apache 2.0 header above the package declaration.
- ⚠️ **Transitive audit**: kotlin-compiler-embeddable pulls in a non-trivial transitive set (jansi, JNA, etc.). Phase 0 research item: confirm none of the transitives are GPL/LGPL/AGPL.
- ✅ **No KMP common/native infection**: all hot-compile code lives in `jvmMain` source sets. `commonMain` source sets remain pure (no embedded-compiler linkage).
- ✅ **No copyleft-by-copying risk**: the project synthesizes its own classloader and compile-driver logic; no verbatim copies from GPL/AGPL projects are introduced.

### Core Principles

- **Code Quality First** (I): every new module/class single-responsibility. `InProcessCompiler` does only invocation + diagnostic capture. `RecompileSession` does only classloader + registry coordination. UI controls (`RecompileButton`) do only trigger + status. Functions kept under 50 lines per `Maintainability` standard.
- **Test-Driven Development** (II): TDD is mandatory. Phase 1 produces contracts BEFORE implementation tasks; Phase 2 tasks (generated by `/speckit.tasks`) will follow Red → Green → Refactor. Test types planned: unit (`InProcessCompiler` against fixture sources), integration (`RecompileSession` end-to-end against a real temp module), contract (`NodeDefinitionRegistry` resolution-precedence behavior).
- **User Experience Consistency** (III): the recompile UI follows existing GraphEditor patterns — toolbar button + ErrorConsolePanel feedback (the same surface introduced in feature 084 for runtime errors). Response time: <100ms for the click; the longer-running compile shows a status indicator. Error messages are actionable (file/line, copyable).
- **Performance Requirements** (IV): SCs encode hard p90 budgets. Phase 1 includes a benchmark contract: a compile latency benchmark suite runs against a fixture module to detect regressions. SC-004 explicitly bounds memory growth.
- **Observability & Debugging** (V): structured `RecompileResult` (per-source success/failure + diagnostics) flows to ErrorConsolePanel. Compile diagnostics include file path + line number sufficient for direct navigation. No secrets in compile diagnostics.

### Quality Gates

- All hot-compile sources go through the standard CI test path. No `@Ignore` / skipped tests in the hot-compile suite.
- License compliance enforced by Phase 0 transitive audit.
- Performance benchmarks run as part of `:flowGraph-inspect:check` and `:graphEditor:check`.

**Gate result**: PASS (one Phase 0 research item to confirm transitive licensing). No constitutional violations require justification.

## Project Structure

### Documentation (this feature)

```text
specs/086-hot-compile-nodes/
├── plan.md              # This file
├── spec.md              # /speckit.specify output
├── research.md          # Phase 0 output (this command)
├── data-model.md        # Phase 1 output (this command)
├── quickstart.md        # Phase 1 output (this command)
├── contracts/           # Phase 1 output (this command)
│   ├── in-process-compiler.md
│   ├── recompile-session.md
│   └── node-definition-registry-v2.md
└── tasks.md             # /speckit.tasks output (NOT this command)
```

### Source Code (repository root)

This feature touches three existing modules and introduces no new Gradle module. The compiler-and-classloader plumbing lives next to the registry it feeds (`flowGraph-inspect/jvmMain`); the UI integration lives in the GraphEditor.

```text
flowGraph-inspect/
├── src/jvmMain/kotlin/io/codenode/flowgraphinspect/
│   ├── compile/
│   │   ├── InProcessCompiler.kt          # NEW — invokes kotlin-compiler-embeddable
│   │   ├── CompileResult.kt              # NEW — structured outcome (success/diagnostics)
│   │   ├── ClassloaderScope.kt           # NEW — per-recompile-unit classloader
│   │   └── SessionCompileCache.kt        # NEW — temp dir lifecycle + per-unit output
│   └── registry/
│       └── NodeDefinitionRegistry.kt     # MODIFIED — resolution precedence (FR-017)
└── src/jvmTest/kotlin/io/codenode/flowgraphinspect/
    └── compile/
        ├── InProcessCompilerTest.kt      # NEW — fixture sources → success/failure
        ├── ClassloaderScopeTest.kt       # NEW — isolation + GC eviction
        └── NodeDefinitionRegistryV2Test.kt  # NEW — precedence behavior

graphEditor/
├── src/jvmMain/kotlin/io/codenode/grapheditor/
│   ├── compile/
│   │   ├── RecompileSession.kt           # NEW — coordinates per-file + per-module + registry update
│   │   ├── RecompileFeedbackPublisher.kt # NEW — RecompileResult → ErrorConsolePanel + status line
│   │   └── PipelineQuiescer.kt           # NEW — stops running pipelines on recompile (FR-014)
│   ├── ui/
│   │   ├── RecompileButton.kt            # NEW — toolbar control with target-module label
│   │   └── CodeEditorPanel.kt            # MODIFIED — per-module recompile action in editor toolbar
│   └── viewmodel/
│       ├── NodeGeneratorViewModel.kt     # MODIFIED — fire per-file auto-compile after generate()
│       └── RecompileViewModel.kt         # NEW — owns the recompile UI state machine
└── src/jvmTest/kotlin/io/codenode/grapheditor/
    └── compile/
        ├── RecompileSessionIntegrationTest.kt  # NEW — generate-and-use scenario end-to-end
        └── PipelineQuiescerTest.kt             # NEW — running-pipeline conflict handling

flowGraph-execute/
└── src/jvmMain/kotlin/io/codenode/flowgraphexecute/
    └── ModuleSessionFactory.kt           # MODIFIED — consult NodeDefinitionRegistry's session classloaders for createRuntime()
```

**Structure Decision**: Place compiler + classloader machinery in `flowGraph-inspect/jvmMain` (close to the registry it feeds). Place UI + ViewModels + workflow coordination in `graphEditor/jvmMain`. `flowGraph-execute` gets a one-line registry-aware lookup change in `ModuleSessionFactory`. No new Gradle module — keeps the dependency tree shallow and avoids a churn-heavy refactor.

## Complexity Tracking

> No Constitution Check violations. The following items are intrinsic complexity, not principle violations — recorded for transparency, not as exceptions.

| Item | Why Inherent | What Mitigates It |
|---|---|---|
| Embedded Kotlin compiler is heavy (~30 MB transitively, slow first invocation) | The headline workflow requires real Kotlin compilation; there's no lighter alternative that preserves correctness | One-time JVM-warmup cost amortized across the session; per-file cost stays under 1s after warmup; benchmarks validate. |
| Custom classloader semantics introduce a class-identity rule (`Class.forName("X")` from the launch-time loader returns a different `Class` than from the in-session loader) | Preserving this distinction is what enables hot-reload. Collapsing it would defeat the feature. | Single rule everywhere: pipeline-build resolution always goes through `NodeDefinitionRegistry`, never through `Class.forName` directly. Registry encapsulates the precedence chain (FR-017). |
| Memory growth across recompiles must be bounded but classloaders can be GC-pinned by stray references | The JVM's classloader-GC model is non-obvious; one stray reference and old classloaders pin forever | `RecompileSession` holds the only strong reference to a generation's classloader; replacing the registry entry releases the reference; SC-004 benchmark catches regressions. |

## Phase 0: Research (executed below — output committed as `research.md`)

Research items dispatched and consolidated in `research.md`:

1. `kotlin-compiler-embeddable` programmatic-invocation API surface and license-aligned usage pattern.
2. Transitive licensing audit for the embeddable compiler dependency tree (Constitution licensing gate).
3. Custom classloader strategy: parent-first vs child-first; URLClassLoader sufficiency; class-identity preservation across the in-session/launch-time boundary.
4. Classpath snapshot mechanism: how to discover the running JVM's classpath JARs at runtime to feed back to `kotlinc` (already partly solved by `writeRuntimeClasspath` in DemoProject; reuse the same file).
5. Diagnostic-message extraction format from `kotlin-compiler-embeddable` so file/line info reaches `ErrorConsolePanel` losslessly.
6. Memory-eviction policy: explicit replacement vs WeakReference; how to verify GC eligibility in tests.
7. Concurrency policy: serialize compile invocations; running-pipeline-conflict handling default.

## Phase 1: Design & Contracts (executed below)

Phase 1 generates `data-model.md`, three contract documents under `contracts/`, `quickstart.md`, and updates the agent-context file. See those artifacts for entity shapes, contract surfaces, and the vertical-slice walkthrough.

## Constitution Check (re-evaluated post-Phase 1)

After completing the data model, contracts, and quickstart, re-check every principle:

- ✅ **Licensing**: Phase 0 Decision 2 confirmed `kotlin-compiler-embeddable` Apache 2.0 with permissive transitives. Phase 1 introduces no additional dependencies. The `verify transitives` step at implementation time remains a hard gate.
- ✅ **Code Quality** (I): every entity in `data-model.md` is single-responsibility. `InProcessCompiler` does compile, `ClassloaderScope` does class loading, `NodeDefinitionRegistry` does resolution, `RecompileSession` does coordination. No method specified exceeds 50 lines (smallest verified by contract test counts).
- ✅ **TDD** (II): each contract document defines its test contract (named test cases) BEFORE implementation. `/speckit.tasks` will produce test-first task ordering per the standard pattern.
- ✅ **UX Consistency** (III): the recompile UI reuses the existing toolbar pattern + `ErrorConsolePanel` from feature 084. No new UX patterns are introduced.
- ✅ **Performance** (IV): hard p90 budgets in plan + contracts (1.0s/5.0s for compile, 1.5s/6.0s for end-to-end). Benchmark contracts in `in-process-compiler.md` and `recompile-session.md`.
- ✅ **Observability** (V): every recompile produces a structured `RecompileResult` with timing, diagnostics, pipeline-quiesce count. All flow to `ErrorConsolePanel` (existing observable surface).

**Post-design gate result**: PASS. No new violations introduced by Phase 1 design. Ready to proceed to `/speckit.tasks`.

### Verified during `/speckit.analyze`

Code-read of `flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/ModuleSessionFactory.kt:76` confirmed: `createSession` injects `lookup = { name -> reg.getByName(name) }` directly into the `DynamicPipelineController` it constructs. The GraphEditor's Runtime Preview path therefore consults `NodeDefinitionRegistry` for every node, never the entity-module-generated `{Module}NodeRegistry::lookup`. FR-017's resolution-precedence change (T028) propagates to Runtime Preview through this single seam — T032 reduces to a regression test confirming the seam is preserved post-modification, not a behavioral change. The entity-module's `{Module}NodeRegistry::lookup` is exercised only by the production-app integration path (feature 085 VS-D5), which is out of scope for feature 086 per FR-018.

## Stop point

This `/speckit.plan` invocation ends here. Generated artifacts:

- `/Users/dhaukoos/CodeNodeIO/specs/086-hot-compile-nodes/plan.md` (this file)
- `/Users/dhaukoos/CodeNodeIO/specs/086-hot-compile-nodes/research.md`
- `/Users/dhaukoos/CodeNodeIO/specs/086-hot-compile-nodes/data-model.md`
- `/Users/dhaukoos/CodeNodeIO/specs/086-hot-compile-nodes/contracts/in-process-compiler.md`
- `/Users/dhaukoos/CodeNodeIO/specs/086-hot-compile-nodes/contracts/recompile-session.md`
- `/Users/dhaukoos/CodeNodeIO/specs/086-hot-compile-nodes/contracts/node-definition-registry-v2.md`
- `/Users/dhaukoos/CodeNodeIO/specs/086-hot-compile-nodes/quickstart.md`
- Agent-context file `/Users/dhaukoos/CodeNodeIO/CLAUDE.md` (updated by `update-agent-context.sh`).

**Next phase**: `/speckit.tasks` to produce the implementation task list.
