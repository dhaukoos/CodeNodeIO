# Implementation Plan: Add Runtime Preview Support to UI-FBP Code Generation

**Branch**: `084-ui-fbp-runtime-preview` | **Date**: 2026-04-28 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/Users/dhaukoos/CodeNodeIO/specs/084-ui-fbp-runtime-preview/spec.md` (post `/speckit.clarify` Session 2026-04-28)

## Summary

Make the existing UI-FBP code-generation path produce a module that loads in the GraphEditor's Runtime Preview AND deploys to a production app, by riding the universal-runtime collapse delivered in feature 085. Today's UI-FBP path emits four files (State, ViewModel, Source CodeNode, Sink CodeNode) plus an optional bootstrap `.flow.kt`; the generated module cannot be loaded by Runtime Preview and is not deployable without the GraphEditor. After this feature, UI-FBP additionally emits exactly three artifacts — a `controller/{FlowGraph}ControllerInterface.kt` extending `io.codenode.fbpdsl.runtime.ModuleController`, a `controller/{FlowGraph}Runtime.kt` factory `create{FlowGraph}Runtime(flowGraph): {FlowGraph}ControllerInterface`, and a `userInterface/{FlowGraph}PreviewProvider.kt` (jvmMain) — and adjusts the existing State/ViewModel pair to fit the universal pattern (ViewModel constructor takes `({FlowGraph}ControllerInterface)`, State lives in `viewmodel/`). The `{FlowGraph}` prefix is derived from the user-selected `.flow.kt` file's prefix name (post-082/083 naming model), independent of the host module's name.

Implementation strategy: feature 085's `RuntimeControllerInterfaceGenerator`, `ModuleRuntimeGenerator`, and `PreviewProviderGenerator` are already wired into `GenerationPath.UI_FBP` in `CodeGenerationRunner`. The work is therefore **mostly integration** — make UI-FBP's input model (`UIFBPSpec` from the parser) reach those generators with the right field semantics: flow-graph-derived prefix, parser-extracted Composable function name (a third identifier the inherited `PreviewProviderGenerator` does not yet model). Two seams require code changes: (1) `PreviewProviderGenerator` must accept a separate `composableName` parameter so the registered `PreviewRegistry` key (the flow graph prefix) can differ from the Composable function call (the user's actual function name); (2) `UIFBPViewModelGenerator` must change its emitted constructor to `({FlowGraph}ControllerInterface)` and read flows directly from `{FlowGraph}State`, matching the entity-module ViewModel shape. A new `UIFBPSaveService` (jvmMain) handles `.flow.kt` parse-and-merge, the conflict / hand-edit safety surface (FR-016), the legacy `saved/` cleanup (FR-010), and the explicit-pair input validation (FR-014/FR-015). Build wiring is fully delegated to feature 085's `ModuleGenerator` scaffolding (FR-009 retired); UI-FBP detects an unscaffolded host and refuses with an actionable error.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (Kotlin Multiplatform). Generators live in `flowGraph-generate/commonMain` (pure string emitters, KMP-safe); the orchestrating `UIFBPSaveService` lives in `flowGraph-generate/jvmMain` (filesystem I/O + `.flow.kt` parse-and-merge). Generated source spans the host module's `commonMain` (KMP) and `jvmMain` (the `PreviewProvider`).
**Primary Dependencies**:
- **Generators reused as-is**: `RuntimeControllerInterfaceGenerator` (emits `interface X : ModuleController` with one `val y: StateFlow<T>` per observable port), `ModuleRuntimeGenerator` (emits the `controller/{FlowGraph}Runtime.kt` factory). Both inhale a `FlowGraph` + package set; UI-FBP feeds them by translating `UIFBPSpec` into a `FlowGraph` model (Source + Sink CodeNodes wired by an empty middle, identical to today's bootstrap `.flow.kt` shape).
- **Generators modified by this feature**: `PreviewProviderGenerator` (extend signature to accept `composableName: String` independently of the flow graph prefix), `UIFBPViewModelGenerator` (constructor change to `({FlowGraph}ControllerInterface)`, flows read from `{FlowGraph}State`, control methods delegate through controller), `UIFBPStateGenerator` (path migration from base package to `viewmodel/`).
- **Generators unchanged**: `UIFBPSourceCodeNodeGenerator`, `UIFBPSinkCodeNodeGenerator`.
- **Parser**: `UIComposableParser` + `UIFBPSpec` — `UIFBPSpec` may need a `composableName` field separate from `moduleName` (today they're conflated; see research Decision 2).
- **Runtime side (Runtime Preview path)**: `flowGraph-execute/ModuleSessionFactory` (reflection proxy + ViewModel reflection lookup; unchanged by this feature — already updated by 085 for `getStatus`), `flowGraph-inspect/DynamicPreviewDiscovery` (scans for `*PreviewProvider.kt`; unchanged).
- **Runtime side (production-app path)**: `fbpDsl/runtime/DynamicPipelineController`, `fbpDsl/runtime/ModuleController`. Consumer template documented in feature 085's `quickstart.md` VS-D5.
- **Generated module deps**: `org.jetbrains.compose.*` 1.7.3, `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0`, `kotlinx-coroutines-core:1.8.0`, `io.codenode:fbpDsl`, `io.codenode:preview-api` (jvmMain only — already added by feature 085's `ModuleGenerator` scaffolding).

**Storage**: N/A. Generator emits source files to the host module's directory tree; no new persistence introduced.
**Testing**:
- Unit tests for new/modified generators alongside the existing `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBP*GeneratorTest.kt` files. Each generator test asserts the emitted text contains the expected interface/factory shape, package declaration, imports, and conformance with the contract pinned in `contracts/controller-interface.md`.
- Integration tests in `flowGraph-generate/src/jvmTest/.../save/UIFBPSaveServiceTest.kt` (new) — temp-dir fixtures exercising first-save, re-save (idempotency), `.flow.kt` parse-and-merge with port-shape changes, hand-edit safety, legacy `saved/` cleanup, and the unscaffolded-host refusal path.
- A regression test in `flowGraph-execute/src/jvmTest/.../UIFBPModuleSessionFactoryTest.kt` (new, optional) — drives a generator-produced fixture through `ModuleSessionFactory.createSession` to prove the Runtime-Preview reflection proxy contract holds end-to-end.
- Manual end-to-end verification through the GraphEditor — covered by `quickstart.md`'s VS-A (TestModule migration), VS-B (green-field), VS-C (deployable parity via 085's VS-D5 template).

**Target Platform**: GraphEditor on JVM Desktop (host of code generation + Runtime Preview). Generated modules remain KMP-targeted (jvm + android + iOS), with the new `PreviewProvider` and `preview-api` dependency confined to `jvmMain`.
**Project Type**: KMP library modules in a multi-module Gradle composite build (CodeNodeIO + sibling DemoProject). No web/mobile front-end relevant here.
**Performance Goals** (from spec):
- SC-001: Module loads in Runtime Preview within 30 s of generation. Dominated by Gradle compile time, not generator throughput.
- SC-003: UI input → graph → UI output round-trip within 1 s.
- Generator wall-time should remain under 1 s for a single UI file (existing behavior).

**Constraints**:
- KMP-first: generated `commonMain` files must not import `jvmMain`-only types. Only the `PreviewProvider` lives in `jvmMain`.
- Generators in `flowGraph-generate/commonMain` are pure string emitters with no I/O; the `UIFBPSaveService` is the single jvmMain orchestrator that touches the filesystem and parses/serializes `.flow.kt`.
- The generator MUST be idempotent: re-running with unchanged inputs produces zero file mutations and zero `.flow.kt` content changes (FR-011).
- The `{FlowGraph}ControllerInterface` MUST extend `io.codenode.fbpdsl.runtime.ModuleController` so the Kotlin interface delegation `ModuleController by controller` in the Runtime factory's anonymous object resolves cleanly (post-085 contract).
- The `PreviewProvider` MUST register under the **flow graph prefix** (matches what `RuntimePreviewPanel` looks up via `flowGraphName`) and MUST invoke the user's actual Composable function name in its body (parser-extracted; possibly distinct from the flow graph prefix).
- UI-FBP MUST refuse to emit when the host module lacks the `jvm()` target or the `preview-api` dependency, directing the user to the scaffolding migration documented in `quickstart.md` (FR-009 in its retired-and-replaced form).
- The user's input is an **explicit `{flow graph, UI file}` pair**. UI-FBP MUST NOT scan the module to discover them implicitly (FR-014/FR-015 post-clarification).

**Scale/Scope**: Single feature with bounded scope — extend one existing generator pipeline; emit one new file (`PreviewProvider`), refactor two existing files (`State`, `ViewModel`), and route through the post-085 universal `ControllerInterface` + `Runtime` generators. Touch one host `build.gradle.kts` shape only to detect (not edit). No cross-module API changes outside `flowGraph-generate`. Net new code: ~150–250 lines in `flowGraph-generate` (one new save service + signature changes on three generators); test code ~400–600 lines. ~3–4 files touched in DemoProject's TestModule for the one-time legacy migration.

## Constitution Check

*Initial gate evaluated against `.specify/memory/constitution.md` v1.0.0.*

### Licensing & IP

- **Static linking rule**: All proposed dependencies (`io.codenode:fbpDsl`, `io.codenode:preview-api`, `org.jetbrains.compose.*`, `org.jetbrains.androidx.lifecycle.*`, `kotlinx-coroutines`) are Apache 2.0. No GPL/LGPL surface. The post-085 universal-runtime collapse already exercised this dependency set across all 5 reference modules.
- **Header management**: All generated files (and any new generator source) MUST begin with the project's standard `/* * <Name> * License: Apache 2.0 */` header above the `package` declaration. Existing UI-FBP generators already follow this; the seam-modified generators preserve the convention.
- **Transitives**: No new external dependencies are introduced — all referenced libraries are already on the project classpath, validated by feature 085's full project test suite. **PASS**

### I. Code Quality First

- **Single responsibility**: `UIFBPSaveService` is the sole new orchestrator; each generator change preserves a single-method `generate(...)` contract (signature additions only). The `PreviewProviderGenerator` change adds one parameter; the `UIFBPViewModelGenerator` change rewrites its emit body but keeps the input contract.
- **Type safety**: All generator inputs are typed (`UIFBPSpec`, `FlowGraph`, package strings); no reflection in the generator path itself.
- **Documentation**: Generated files carry both the standard license header AND a `Generated by CodeNodeIO {Generator}` marker comment (the post-085 convention `GenerationFileWriter.carriesGeneratorMarker` checks for). New `UIFBPSaveService` and new tests carry the standard header.
- **Security**: No external input boundary; generator input is local source files validated by `UIComposableParser`. **PASS**

### II. Test-Driven Development

- Each generator change gets a unit test written first asserting the emitted text contains the new shape (extending interface, package change, ViewModel constructor signature, PreviewProvider's separate composable-name handling).
- A round-trip regeneration test verifies idempotency on unchanged input (FR-011).
- The new `UIFBPSaveService` gets jvmTest integration coverage for: first-save, re-save idempotency, `.flow.kt` merge with port adds/removes, hand-edit safety (`SKIPPED_CONFLICT`), legacy `saved/` cleanup, unscaffolded-host refusal.
- An optional `UIFBPModuleSessionFactoryTest` proves the reflection-proxy contract holds end-to-end against generator output.
- Coverage targets: 100% line on new/modified generators (string templates); >80% on `UIFBPSaveService`. **PASS**

### III. User Experience Consistency

- The user-facing surface is the GraphEditor's Runtime Preview behavior — currently fails silently for UI-FBP modules. After this feature, the same panel renders without changes to its layout, controls, or interaction patterns. Narrowing inconsistent behavior, not introducing new patterns.
- Error handling: re-generation produces a structured `UIFBPSaveResult` summary (created/updated/unchanged/conflict/deleted entries + `.flow.kt` merge report) per FR-013. No silent destruction.
- Explicit-pair input: the GraphEditor's existing file-selector pattern (already used by feature 085's Generate Module path) is reused — no new UX vocabulary. **PASS**

### IV. Performance Requirements

- Generator execution is bounded text manipulation (~milliseconds). No new hot paths.
- Runtime side reuses `DynamicPipelineController` (already in production use across all 5 reference modules post-085). No new performance characteristics. **PASS**

### V. Observability & Debugging

- Every emitted file carries a `Generated by CodeNodeIO {Generator}` marker comment so any future debugger can trace the artifact back to its generator (and so the post-085 hand-edit safety check in `GenerationFileWriter` continues to work for UI-FBP outputs).
- `UIFBPSaveService` emits a structured `UIFBPSaveResult` (per-file `FileChange` + `FlowKtMergeReport`) usable by both interactive UI and CI/automation contexts.
- No logging of secrets; all generator content is from local source files. **PASS**

**Gate result: PASS — no constitutional violations to justify.**

## Project Structure

### Documentation (this feature)

```text
specs/084-ui-fbp-runtime-preview/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 — gap analysis, post-085 reuse decisions
├── data-model.md        # Phase 1 — generator I/O shape, save-service contract
├── quickstart.md        # Phase 1 — manual verification (VS-A migration, VS-B green-field, VS-C deployable)
├── contracts/
│   └── controller-interface.md  # Post-085 ControllerInterface + Runtime factory contract
├── checklists/
│   └── requirements.md  # Pre-existing
├── CROSS-CHECK-085.md   # 2026-04-28 cross-check artifact (delta vs feature 085)
├── spec.md              # Feature spec (post-clarification)
└── tasks.md             # Phase 2 — created by /speckit.tasks (not by /speckit.plan)
```

### Source Code (repository root)

This is a multi-module Gradle composite build (CodeNodeIO) with a sibling DemoProject. The change touches one module in `CodeNodeIO/` and updates one example module in `CodeNodeIO-DemoProject/` (one-time TestModule migration).

```text
CodeNodeIO/
└── flowGraph-generate/
    └── src/
        ├── commonMain/kotlin/io/codenode/flowgraphgenerate/
        │   ├── parser/
        │   │   └── UIFBPSpec.kt                              # MODIFY: add `composableName: String` field separate from `moduleName`/flowGraphPrefix
        │   ├── generator/
        │   │   ├── UIFBPStateGenerator.kt                    # MODIFY: emit to viewmodel/ subpackage; prefix from flow-graph (not module)
        │   │   ├── UIFBPViewModelGenerator.kt                # MODIFY: constructor takes ({FlowGraph}ControllerInterface); flows from State; control methods delegate through controller; prefix from flow-graph
        │   │   ├── UIFBPSourceCodeNodeGenerator.kt           # MODIFY (minimal): use flow-graph prefix for emitted class name and package layout
        │   │   ├── UIFBPSinkCodeNodeGenerator.kt             # MODIFY (minimal): use flow-graph prefix for emitted class name and package layout
        │   │   ├── UIFBPInterfaceGenerator.kt                # MODIFY: orchestrator now feeds RuntimeControllerInterfaceGenerator + ModuleRuntimeGenerator + PreviewProviderGenerator (the post-085 universal generators) using a UIFBPSpec→FlowGraph translation
        │   │   ├── PreviewProviderGenerator.kt               # MODIFY (085-owned): add `composableName: String` parameter so registry key (flow-graph prefix) can differ from Composable function call (user-authored name)
        │   │   ├── RuntimeControllerInterfaceGenerator.kt    # REUSE AS-IS (085): emits interface X : ModuleController
        │   │   └── ModuleRuntimeGenerator.kt                 # REUSE AS-IS (085): emits controller/{FlowGraph}Runtime.kt factory
        │   └── runner/
        │       └── CodeGenerationRunner.kt                   # NO CHANGE EXPECTED: GenerationPath.UI_FBP already includes the post-085 generator set
        ├── jvmMain/kotlin/io/codenode/flowgraphgenerate/save/
        │   └── UIFBPSaveService.kt                           # NEW: orchestrate filesystem write-out, .flow.kt parse-and-merge, hand-edit safety, legacy saved/ cleanup, unscaffolded-host refusal, structured UIFBPSaveResult
        ├── commonTest/kotlin/io/codenode/flowgraphgenerate/generator/
        │   ├── UIFBPGeneratorTest.kt                         # MODIFY: extend with new file outputs + revised VM constructor + flow-graph-prefix verification
        │   ├── UIFBPViewModelGeneratorTest.kt                # MODIFY (or NEW): assert constructor signature + flow-graph prefix + flows-from-State pattern
        │   ├── UIFBPStateGeneratorTest.kt                    # MODIFY: assert viewmodel/ subpackage emission + flow-graph prefix
        │   ├── PreviewProviderGeneratorTest.kt               # MODIFY (085-owned): add cases proving registry key (flow-graph) and composable invocation (composable-name) decouple correctly
        │   └── UIFBPInterfaceGeneratorTest.kt                # MODIFY: end-to-end orchestrator test against the post-085 generator set; verify all 7 emitted files (State, ViewModel, Source, Sink, ControllerInterface, Runtime, PreviewProvider)
        └── jvmTest/kotlin/io/codenode/flowgraphgenerate/save/
            └── UIFBPSaveServiceTest.kt                       # NEW: temp-dir fixtures for first-save, re-save idempotency, .flow.kt port-shape merge, hand-edit safety (SKIPPED_CONFLICT), legacy saved/ cleanup, unscaffolded-host refusal

CodeNodeIO/flowGraph-execute/
└── src/jvmTest/kotlin/io/codenode/flowgraphexecute/
    └── UIFBPModuleSessionFactoryTest.kt                       # NEW (optional): drive a generator-produced fixture through ModuleSessionFactory.createSession, assert proxy + ViewModel cast succeed, getStatus() returns non-null

CodeNodeIO-DemoProject/
└── TestModule/
    ├── build.gradle.kts                                       # NO CHANGE if scaffolded; otherwise one-time migration documented in quickstart.md (082/083 era)
    └── src/
        ├── commonMain/kotlin/io/codenode/demo/
        │   ├── viewmodel/
        │   │   ├── DemoUIState.kt                             # KEEP (canonical post-migration location); regenerated against the post-085 contract
        │   │   └── DemoUIViewModel.kt                         # OVERWRITTEN by re-generation: constructor takes (DemoUIControllerInterface)
        │   ├── controller/
        │   │   ├── DemoUIControllerInterface.kt               # NEW (generated, extends ModuleController)
        │   │   └── DemoUIRuntime.kt                           # NEW (generated, factory createDemoUIRuntime)
        │   ├── flow/
        │   │   └── DemoUI.flow.kt                             # KEEP (user surface) — merged on re-save
        │   ├── userInterface/
        │   │   └── DemoUI.kt                                  # MODIFY: import path corrected to viewmodel/ (one-time migration), uncomment live emit
        │   ├── nodes/                                         # OVERWRITTEN by re-generation
        │   ├── iptypes/                                       # UNCHANGED
        │   └── saved/                                         # DELETE: legacy duplicate package (FR-010)
        └── jvmMain/kotlin/io/codenode/demo/userInterface/
            └── DemoUIPreviewProvider.kt                       # NEW (generated; registers under "DemoUI" key; calls DemoUI() composable by name)
```

**Structure Decision**: Extend the existing `flowGraph-generate` UI-FBP pipeline. UI-FBP becomes a **thin orchestrator** over feature 085's universal generator set: it produces a `FlowGraph` model from `UIFBPSpec` (Source + Sink CodeNodes) and feeds that plus the parser-extracted Composable function name into `RuntimeControllerInterfaceGenerator` (reused as-is), `ModuleRuntimeGenerator` (reused as-is), and `PreviewProviderGenerator` (extended to accept a separate `composableName`). The new `UIFBPSaveService` in `jvmMain` composes those with `.flow.kt` parse-and-merge, hand-edit safety, legacy cleanup, and unscaffolded-host detection. No new modules, no cross-module dependency changes, no changes to `fbpDsl`, `flowGraph-execute`, `flowGraph-inspect`, or `preview-api`. Net code change is concentrated in `flowGraph-generate`; the DemoProject's TestModule absorbs a one-time legacy `saved/` cleanup + import-path fix (manual, documented in `quickstart.md`).

## Complexity Tracking

> No constitutional violations to justify. No new top-level modules, no new dependency surface, no new cross-cutting patterns introduced. Net code added is smaller than the pre-085 plan because feature 085 already absorbed the largest pieces (`ControllerInterface`, `Runtime`, `PreviewProvider` generators) and retired the four obsolete entity-module generator reuses (`RuntimeControllerGenerator`, `RuntimeControllerAdapterGenerator`, `RuntimeFlowGenerator`, plus the no-longer-needed `UIFBPSpecAdapter` translation layer that was going to feed them).
