# Implementation Plan: MVI Pattern for UI-FBP Interface Generation

**Branch**: `087-mvi-ui-fbp` | **Date**: 2026-05-03 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/087-mvi-ui-fbp/spec.md`

## Summary

Replace the current UI-FBP generator output (a singleton `{Name}State` object
with `MutableStateFlow` fields plus a `{Name}ViewModel` exposing `emit(...)`)
with the canonical MVI shape: an immutable `data class {Name}State`, a sealed
`{Name}Event` hierarchy, and a `{Name}ViewModel` that exposes
`state: StateFlow<{Name}State>` and `fun onEvent(event: {Name}Event)`. Hand-written
`{Name}Screen` composables in host modules then take `(state, onEvent) -> Unit`.
The generator itself stops emitting any Screen / ScreenRoot composable — those
remain entirely in the host module's hand-written code.

**Per the 2026-05-03 clarification (Design B):** the singleton State object
is **eliminated entirely** — no surviving `StateStore` or any other shared
mutable. Per-flow-graph state moves to the `DynamicPipelineController`
instance (instance-per-flow-graph). The `ControllerInterface` gains additive
`emit<SourcePort>(...)` methods; the Source and Sink CodeNodes gain
`withSources(...)` / `withReporters(...)` wrappers that the `{Name}Runtime`
factory uses to inject per-flow-graph flows at runtime construction time.
The State data class becomes the genuine SSOT for the UI; multiple
flow-graph instances are state-isolated by construction.

The `create{Name}Runtime(flowGraph)` factory **signature** is unchanged so
`ModuleSessionFactory` and the Runtime Preview panel keep working without
modification. DemoUI is the canonical migration target (P2); other
DemoProject UI modules follow as P3 with no generator-side changes.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP — Kotlin Multiplatform). Generators
live in `flowGraph-generate/commonMain` (pure string builders, KMP-safe);
the orchestrating `UIFBPSaveService` lives in `flowGraph-generate/jvmMain`
(filesystem I/O + `.flow.kt` parse-and-merge). Generated source lands under
the host module's `commonMain` (`viewmodel/`, `controller/`, `nodes/`).
**Primary Dependencies**: existing only — kotlinx-coroutines 1.8.0 (StateFlow,
viewModelScope), Compose Multiplatform 1.7.3 (downstream UI consumers, NOT a
generator dependency), lifecycle-viewmodel-compose 2.8.0 (ViewModel base
class). No new third-party additions.
**Storage**: N/A. The generator writes `.kt` files to the host module's
directory tree; no new persistence is introduced.
**Testing**: kotlin.test in `commonTest` for the pure generators
(byte-comparison fixtures), `jvmTest` for the `UIFBPSaveService` integration
path. `kotlinx-coroutines-test` (`runTest`) for ViewModel coroutine scope.
**Target Platform**: KMP — common code paths land in `commonMain`. The
filesystem-touching save service is `jvmMain`-only; per the project's KMP-first
memory, the JVM Desktop is the preview/debug surface, not a permitted home
for production logic.
**Project Type**: Existing multi-module Gradle project (single repo). Touches
`flowGraph-generate/`, plus the host modules in `CodeNodeIO-DemoProject/`
(DemoUI for P2; StopWatch / KMPMobileApp / WeatherForecast for P3).
**Performance Goals**: Single-module regeneration ≤ 500ms wall-clock (matches
existing baseline). Deterministic byte-identical output for unchanged specs
(SC-005); verified by `git diff --exit-code`.
**Constraints**: No new module dependencies. No backwards-compatibility shim
for the prior `emit(...)` ViewModel API or singleton State (FR-011, Design B).
`create{Name}Runtime(...)` factory signature and PreviewProvider unchanged
(FR-007). `ControllerInterface` gains additive `emit<SourcePort>(...)` methods
(FR-007 softened). `DynamicPipelineController.coroutineScope` must be
publicly accessible (verify or add a public accessor — flagged as a
prerequisite task).
**Scale/Scope**: 8 mandatory generated files per UI-FBP module under Design B —
the 7 from feature 084 (`{Name}State.kt`, `{Name}ViewModel.kt`,
`{Name}ControllerInterface.kt`, `{Name}Runtime.kt`, `{Name}SourceCodeNode.kt`,
`{Name}SinkCodeNode.kt`, `{Name}PreviewProvider.kt`) + the new
`{Name}Event.kt`. Of those 8, **7 change shape under Design B** (every file
except `{Name}PreviewProvider.kt`, whose body is unchanged from feature 084).
One canonical migration (DemoUI, P2) plus ≥1 follow-up migration (P3); 4
additional candidates available for staged rollout outside this feature.
Estimated total work: 1 multi-generator refactor + 1–2 demo-module
migrations in-scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| **Licensing & IP** (no GPL/LGPL) | ✓ Pass | No new dependencies introduced. All existing deps (kotlinx-coroutines, lifecycle-viewmodel-compose, Compose Multiplatform) are Apache 2.0. |
| **I. Code Quality First** | ✓ Pass | All public generator methods get KDoc citing the spec's FR they implement. Generated code follows the same header/license-comment convention as existing UIFBP* generators. |
| **II. Test-Driven Development** | ✓ Pass | Plan front-loads RED test phase (Phase 3, US1) — fixture-based byte-comparison tests for each modified generator (`UIFBPStateGeneratorTest`, `UIFBPEventGeneratorTest`, `UIFBPViewModelGeneratorTest`, `UIFBPSinkCodeNodeGeneratorTest`, `UIFBPSourceCodeNodeGeneratorTest`, `UIFBPControllerInterfaceTest`, `UIFBPRuntimeFactoryTest`) written BEFORE the generator changes. DemoUI migration (P2) lands behind a manual smoke test of the runtime preview. |
| **III. UX Consistency** | ✓ Pass with note | The generator change is intentionally breaking for hand-written UI callers (FR-011). Per the spec's edge-case handling, callers see a loud compile error citing the new signature — actionable per Principle III ("Tell user what went wrong AND what to do next"). The migration is one-time per host module. |
| **IV. Performance Requirements** | ✓ Pass | Generator wall-clock budget unchanged from feature 084 baseline. Determinism (SC-005) is verifiable. No runtime perf impact (the generated code paths are equivalent to today's — the State data class snapshot vs singleton object difference is single-allocation-per-update; negligible for UI-rate updates). |
| **V. Observability & Debugging** | ✓ Pass | Generator failures return structured `UIFBPGenerateResult(success=false, errorMessage=...)` (existing pattern). Atomicity (FR-010) ensures no partial replacement on failure — the prior generation remains the on-disk truth. |
| **KMP-first** (per project memory) | ✓ Pass | All generators stay in `commonMain`; only `UIFBPSaveService` (filesystem I/O) is `jvmMain`-only. No Android- or iOS-specific touches. |

**Gate result**: All gates pass. No Complexity Tracking entries needed.

## Project Structure

### Documentation (this feature)

```text
specs/087-mvi-ui-fbp/
├── plan.md              # This file (/speckit.plan output)
├── spec.md              # Feature specification (clarified 2026-05-03 → Design B)
├── research.md          # Phase 0 output (Decision 2 revised; Decision 8 added)
├── data-model.md        # Phase 1 output (Design B)
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (generator contracts)
│   ├── state-generator.md
│   ├── event-generator.md
│   ├── viewmodel-generator.md
│   └── source-sink-controller-runtime.md   # Design B — covers Source/Sink/Controller/Runtime co-changes
├── checklists/
│   └── requirements.md  # Spec quality gate (already written)
└── tasks.md             # Phase 2 output (NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
flowGraph-generate/
├── src/commonMain/kotlin/io/codenode/flowgraphgenerate/
│   ├── parser/
│   │   └── UIFBPSpec.kt                       # unchanged — sourceOutputs/sinkInputs already carry what we need
│   └── generator/
│       ├── UIFBPStateGenerator.kt             # MODIFIED — emit data class instead of singleton object
│       ├── UIFBPEventGenerator.kt             # NEW — emit sealed interface {Name}Event
│       ├── UIFBPViewModelGenerator.kt         # MODIFIED — expose state + onEvent; onEvent calls controller.emit<Port>
│       ├── UIFBPSourceCodeNodeGenerator.kt    # MODIFIED — emit object with default + withSources(vararg) wrapper
│       ├── UIFBPSinkCodeNodeGenerator.kt      # MODIFIED — emit object with default + withReporters(vararg) wrapper
│       └── UIFBPInterfaceGenerator.kt         # MODIFIED — ControllerInterface gains additive emit<Port> methods;
│                                              #   Runtime factory wires per-flow-graph MutableStateFlow / MutableSharedFlow,
│                                              #   builds sinkWrapper / sourceWrapper, returns object overriding everything
├── src/jvmMain/kotlin/io/codenode/flowgraphgenerate/
│   └── save/
│       └── UIFBPSaveService.kt                # MODIFIED — drop the singleton-State output file from prior generations;
│                                              #   write the new {Name}Event.kt file alongside the existing set
└── src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/
    ├── UIFBPStateGeneratorTest.kt             # MODIFIED — fixtures swap singleton-object for data-class shape
    ├── UIFBPEventGeneratorTest.kt             # NEW — fixture-based byte-comparison tests
    ├── UIFBPViewModelGeneratorTest.kt         # MODIFIED — fixtures use state/onEvent + controller.emit<Port>
    ├── UIFBPSourceCodeNodeGeneratorTest.kt    # MODIFIED (or NEW if absent) — fixture for object + withSources wrapper
    ├── UIFBPSinkCodeNodeGeneratorTest.kt      # MODIFIED (or NEW if absent) — fixture for object + withReporters wrapper
    ├── UIFBPControllerInterfaceTest.kt        # NEW — fixture for additive emit methods
    └── UIFBPRuntimeFactoryTest.kt             # NEW — fixture for per-flow-graph state + wrapper wiring

fbpDsl/  (or wherever DynamicPipelineController lives)
└── src/commonMain/kotlin/io/codenode/fbpdsl/runtime/
    └── DynamicPipelineController.kt          # PREREQUISITE: confirm coroutineScope is publicly accessible
                                              #   (or add a public accessor) — required for source-emit dispatch.

CodeNodeIO-DemoProject/
├── TestModule/  (P2 — hosts the DemoUI flow graph)
│   └── src/commonMain/kotlin/io/codenode/testmodule/
│       ├── viewmodel/                # REGENERATED — DemoUIState (data class), DemoUIEvent (sealed), DemoUIViewModel
│       │                             # The prior DemoUIState.kt singleton is REPLACED, not retained.
│       │                             # DemoUIAction.kt and DemoUIStateMVI.kt placeholder files DELETED.
│       ├── controller/               # REGENERATED (additive: emit<Port> methods on interface; per-flow-graph state in factory)
│       ├── nodes/                    # REGENERATED (Source + Sink: object + withSources / withReporters wrappers)
│       └── userInterface/            # HAND-EDITED — DemoUI.kt updated to (state, onEvent) signature;
│                                     # DemoUI's own root composable updated to collect viewModel.state + pass viewModel::onEvent
└── (WeatherForecast/, EdgeArtFilter/, UserProfiles/, Addresses/, StopWatch/ — P3 follow-on migrations)
```

**Structure Decision**: Existing multi-module Gradle layout. Generator
changes stay inside `flowGraph-generate/`; the only filesystem-touching code
remains `jvmMain` (per the project's KMP-first memory). Host-module
regeneration writes to existing directory conventions established by feature
084 — no new directories introduced. **Under Design B, the change set spans
seven generator output files per UI-FBP module** (State, Event, ViewModel,
ControllerInterface, Runtime, SourceCodeNode, SinkCodeNode); only the
PreviewProvider stays untouched. One pre-implementation prerequisite: confirm
or expose `DynamicPipelineController.coroutineScope` so the generated source
emits can dispatch on it.

## Complexity Tracking

> No Constitution Check violations. Section omitted.
