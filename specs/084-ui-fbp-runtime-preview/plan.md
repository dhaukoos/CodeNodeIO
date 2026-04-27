# Implementation Plan: Add Runtime Preview Support to UI-FBP Code Generation

**Branch**: `084-ui-fbp-runtime-preview` | **Date**: 2026-04-26 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/Users/dhaukoos/CodeNodeIO/specs/084-ui-fbp-runtime-preview/spec.md`

## Summary

UI-FBP code generation today emits four files (a `State` object, a `ViewModel`, a `Source CodeNode`, a `Sink CodeNode`) plus an optional bootstrap `.flow.kt` graph. The resulting module cannot be loaded by the GraphEditor's Runtime Preview panel AND cannot be deployed to a production app that imports the module without the GraphEditor in the loop. Per spec Clarifications Q1, this feature targets BOTH scenarios by emitting the full thick artifact set used today by entity modules (Addresses, UserProfiles, EdgeArtFilter): `ControllerInterface` + `Controller` + `ControllerAdapter` + `Flow` runtime in `commonMain`, plus a `PreviewProvider` in `jvmMain`, plus a constructor-shape change to the generated `ViewModel` so it accepts the `ControllerInterface`. Per spec Clarifications Q2, this feature does NOT pioneer the universal-runtime collapse onto `DynamicPipelineController`; that is a separate follow-up that will deprecate the thick stack across all modules at once.

Implementation strategy: extend `UIFBPInterfaceGenerator` to orchestrate two new UI-FBP-specific generators (`UIFBPControllerInterfaceGenerator` for the interface shape, `UIFBPPreviewProviderGenerator` for the JVM preview registration) AND to invoke the four existing entity-module generators (`RuntimeControllerInterfaceGenerator`, `RuntimeControllerGenerator`, `RuntimeControllerAdapterGenerator`, `RuntimeFlowGenerator`) by translating `UIFBPSpec` into their input models. The new `UIFBPSaveService` (in `jvmMain`) handles filesystem write-out, `.flow.kt` parse-and-merge, and the `build.gradle.kts` touch-up. The legacy `saved/` vs `viewmodel/` duplication observed in TestModule is cleaned up via a one-time migration documented in `quickstart.md`.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (Kotlin Multiplatform). Generators run on JVM; generated source spans `commonMain` (KMP) and `jvmMain`.
**Primary Dependencies**:
- Generator side: `flowGraph-generate` (existing `UIFBPInterfaceGenerator`, `UIFBPSpec`, `UIFBPParseResult`) PLUS the four existing entity-module generators that UI-FBP will now invoke: `RuntimeControllerInterfaceGenerator`, `RuntimeControllerGenerator`, `RuntimeControllerAdapterGenerator`, `RuntimeFlowGenerator`. A small adapter layer translates `UIFBPSpec` → the input models those generators expect.
- Runtime side (Runtime Preview path): `flowGraph-execute/ModuleSessionFactory`, `fbpDsl/DynamicPipelineController`, `fbpDsl/DynamicPipelineBuilder`, `flowGraph-inspect/DynamicPreviewDiscovery`, `preview-api/PreviewRegistry`.
- Runtime side (deployable path — production app): generated `{Module}Controller` constructs `{Module}Flow()` (hand-coded runtime), uses `RootControlNode`, `RuntimeRegistry`, `ModuleController` from `fbpDsl/runtime/`. No GraphEditor classes on the deployment path.
- Generated module deps: `org.jetbrains.compose.*` 1.7.3, `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0`, `kotlinx-coroutines-core:1.8.0`, `io.codenode:fbpDsl`, `io.codenode:preview-api` (jvmMain only).

**Storage**: N/A. Generator emits source files to the workspace module's directory tree; no new persistence introduced.
**Testing**:
- Unit tests for new generators in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/` alongside existing `UIFBPGeneratorTest.kt`.
- A regeneration round-trip test that takes a known input UI file, runs the generator, and asserts the file set matches a golden snapshot.
- Manual end-to-end verification through the GraphEditor against the cleaned-up `TestModule` (covered by `quickstart.md`).

**Target Platform**: GraphEditor on JVM Desktop (host). Generated modules remain KMP-targeted (jvm + android + iOS), with the new `PreviewProvider` and `preview-api` dependency confined to `jvmMain` so that Android/iOS targets are unaffected.
**Project Type**: KMP library modules in a multi-module Gradle composite build (CodeNodeIO + DemoProject). No web/mobile front-end relevant here.
**Performance Goals**: Per spec SC-001 / SC-003 — module load in Runtime Preview within 30 s of generation; UI input → graph → UI output round-trip within 1 s. These are dominated by Gradle compile time, not generator throughput; generator wall-time should remain under 1 s for a single UI file (existing behavior).
**Constraints**:
- KMP-first: generated files in `commonMain` must not import `jvmMain`-only types. Only `PreviewProvider` lives in `jvmMain`.
- The generator is invoked from `commonMain` of `flowGraph-generate` (it has no JVM-only dependencies today). Filesystem I/O when writing the generated set is the caller's responsibility.
- The generator MUST be idempotent and MUST NOT overwrite hand-written files at generation targets without an explicit conflict signal.
- The `ControllerInterface` contract MUST match what `ModuleSessionFactory.createControllerProxy` synthesizes by reflection (specifically: `getExecutionState()`, `start()`, `stop()`, `pause()`, `resume()`, `reset()` returning either `Unit` or `FlowGraph`, plus `get{Property}Flow()` getters that the proxy resolves against `{Module}State.{property}Flow` fields).

**Scale/Scope**: Single feature with bounded scope — extend one existing generator pipeline; emit ~2 new generated files per UI file plus modify ~2 existing generated files; touch one host build.gradle.kts shape. No cross-module API changes outside `flowGraph-generate`.

## Constitution Check

*Initial gate evaluated against `.specify/memory/constitution.md` v1.0.0.*

### Licensing & IP

- **Static linking rule**: All proposed dependencies (`io.codenode:fbpDsl`, `io.codenode:preview-api`, `org.jetbrains.compose.*`, `org.jetbrains.androidx.lifecycle.*`, `kotlinx-coroutines`) are Apache 2.0 — permitted under the constitution. No GPL/LGPL surface.
- **Header management**: All generated files MUST begin with the project's standard license header above the `package` declaration. The existing UI-FBP generators already follow this pattern (`/* * <Name> * License: Apache 2.0 */`). New generators MUST replicate.
- **Transitives**: No new external dependencies are introduced — all referenced libraries are already on the project classpath. **PASS**

### I. Code Quality First

- Single responsibility: each new generator (`UIFBPControllerInterfaceGenerator`, `UIFBPPreviewProviderGenerator`) is one class with one method (`generate(spec) -> String`), mirroring existing UI-FBP generators.
- Type safety: generators are pure-Kotlin string builders with typed `UIFBPSpec` input. No reflection in the generator path.
- Documentation: generated files carry the standard header + a `Generated by CodeNodeIO UIFBPInterfaceGenerator` comment block (matching today's output).
- Security: generator input comes from a project's local source files; no external input boundary. Filenames are derived from already-validated `UIFBPSpec.moduleName` / `packageName`. **PASS**

### II. Test-Driven Development

- Each new generator gets a unit test in `UIFBPGeneratorTest.kt` (or a new sibling test file) **written first** asserting the emitted text contains the expected interface shape, package, and reflection-discoverable structure.
- A round-trip regeneration test verifies idempotency on unchanged input.
- An integration-style test (in `flowGraph-execute/src/jvmTest`) MAY exercise `ModuleSessionFactory.createSession` against a generator-produced module fixture to prove the proxy contract holds — feasible because the runtime side is already JVM-loaded by tests.
- Coverage targets: 100% line coverage on new generators (they're string templates) and >80% on regeneration-merge logic. **PASS**

### III. User Experience Consistency

- The user-facing surface here is the GraphEditor's Runtime Preview behavior, which currently fails silently for UI-FBP modules ("No runtime available for this module" / "No preview available"). After this feature, the same panel renders the UI without changes to its layout, controls, or interaction patterns — a *narrowing* of inconsistent behavior, fully aligned with the principle.
- Error handling: when re-generation drops a graph connection because a port disappeared, the generator MUST surface a structured summary (already promised in FR-013) so the user sees an actionable diff rather than silent destruction. **PASS**

### IV. Performance Requirements

- Generator execution is bounded text manipulation (~milliseconds). No new hot paths.
- Runtime side reuses `DynamicPipelineController` already in production use for entity modules — no new performance characteristics introduced. **PASS**

### V. Observability & Debugging

- The generator's output already includes a "Generated by" comment so a future debugger can trace any emitted artifact back to this generator.
- Re-generation MUST produce a structured summary (created/updated/unchanged files, dropped connections) to satisfy FR-013 and the observability principle.
- No logging of secrets; all generated content is from local source files. **PASS**

**Gate result: PASS — no violations to justify.**

## Project Structure

### Documentation (this feature)

```text
specs/084-ui-fbp-runtime-preview/
├── plan.md              # This file
├── research.md          # Phase 0 — gap analysis, runtime-contract reverse-engineering
├── data-model.md        # Phase 1 — generated artifact set, schema of inputs/outputs
├── quickstart.md        # Phase 1 — manual verification path against TestModule
├── contracts/
│   └── controller-interface.md  # Reflection contract synthesized by ModuleSessionFactory
└── tasks.md             # Phase 2 — created by /speckit.tasks
```

### Source Code (repository root)

This is a multi-module Gradle composite build. The change touches one module in `CodeNodeIO/` and updates one example module in the sibling `CodeNodeIO-DemoProject/`.

```text
CodeNodeIO/
├── flowGraph-generate/
│   └── src/
│       ├── commonMain/kotlin/io/codenode/flowgraphgenerate/
│       │   ├── generator/
│       │   │   ├── UIFBPInterfaceGenerator.kt              # MODIFY: orchestrate new + reused generators
│       │   │   ├── UIFBPStateGenerator.kt                  # MODIFY: emit to viewmodel/ subpackage
│       │   │   ├── UIFBPViewModelGenerator.kt              # MODIFY: constructor takes ControllerInterface; flows from State
│       │   │   ├── UIFBPSourceCodeNodeGenerator.kt         # UNCHANGED
│       │   │   ├── UIFBPSinkCodeNodeGenerator.kt           # UNCHANGED
│       │   │   ├── UIFBPControllerInterfaceGenerator.kt    # NEW (UI-FBP-flavored interface shape)
│       │   │   ├── UIFBPPreviewProviderGenerator.kt        # NEW
│       │   │   ├── UIFBPSpecAdapter.kt                     # NEW: translates UIFBPSpec → entity-generator inputs
│       │   │   ├── RuntimeControllerInterfaceGenerator.kt  # REUSED (no changes); may be invoked instead of UIFBPControllerInterfaceGenerator if shapes align
│       │   │   ├── RuntimeControllerGenerator.kt           # REUSED (no changes); UI-FBP invokes via adapter
│       │   │   ├── RuntimeControllerAdapterGenerator.kt    # REUSED
│       │   │   └── RuntimeFlowGenerator.kt                 # REUSED
│       │   └── parser/
│       │       └── UIFBPSpec.kt                            # UNCHANGED (existing fields suffice for adapter)
│       ├── jvmMain/kotlin/io/codenode/flowgraphgenerate/save/
│       │   └── UIFBPSaveService.kt                         # NEW: filesystem writer + .flow.kt merge + build.gradle.kts touch-up
│       └── commonTest/kotlin/io/codenode/flowgraphgenerate/generator/
│           ├── UIFBPGeneratorTest.kt                       # MODIFY: extend with new file outputs + revised VM constructor
│           ├── UIFBPControllerInterfaceGeneratorTest.kt    # NEW
│           ├── UIFBPPreviewProviderGeneratorTest.kt        # NEW
│           ├── UIFBPSpecAdapterTest.kt                     # NEW: validates adapter outputs match what reused generators expect
│           ├── UIFBPThickStackIntegrationTest.kt           # NEW: end-to-end generation produces a structurally-complete module
│           └── UIFBPRegenerationTest.kt                    # NEW: idempotency + .flow.kt preservation
│
└── flowGraph-execute/
    └── src/jvmTest/kotlin/io/codenode/flowgraphexecute/
        └── UIFBPModuleSessionFactoryTest.kt                # NEW (optional integration test against generated fixture)

CodeNodeIO-DemoProject/
└── TestModule/
    ├── build.gradle.kts                                    # MODIFY: add jvm() target + jvmMain preview-api dependency
    └── src/
        ├── commonMain/kotlin/io/codenode/demo/
        │   ├── viewmodel/
        │   │   ├── DemoUIState.kt                          # KEEP (canonical location)
        │   │   └── DemoUIViewModel.kt                      # MODIFY: take ControllerInterface
        │   ├── controller/
        │   │   ├── DemoUIControllerInterface.kt            # NEW (generated — interface)
        │   │   ├── DemoUIController.kt                     # NEW (generated — thick deployable)
        │   │   └── DemoUIControllerAdapter.kt              # NEW (generated — thick deployable)
        │   ├── flow/
        │   │   ├── DemoUI.flow.kt                          # KEEP (user surface)
        │   │   └── DemoUIFlow.kt                           # NEW (generated — thick runtime)
        │   ├── userInterface/
        │   │   └── DemoUI.kt                               # MODIFY: import path corrected to viewmodel/, remove commented-out emit
        │   ├── nodes/                                      # UNCHANGED
        │   ├── iptypes/                                    # UNCHANGED
        │   └── saved/                                      # DELETE: legacy duplicate package
        └── jvmMain/kotlin/io/codenode/demo/userInterface/
            └── DemoUIPreviewProvider.kt                    # NEW (generated)
```

**Structure Decision**: Extend the existing `flowGraph-generate` module's UI-FBP generator pipeline. Two new commonMain generators (`UIFBPControllerInterfaceGenerator`, `UIFBPPreviewProviderGenerator`) are added alongside the existing four; a new `UIFBPSpecAdapter` translates `UIFBPSpec` into the input models expected by the existing four entity-module generators (`RuntimeControllerInterfaceGenerator`, `RuntimeControllerGenerator`, `RuntimeControllerAdapterGenerator`, `RuntimeFlowGenerator`), which UI-FBP now invokes. One existing generator (`UIFBPViewModelGenerator`) is modified; a new jvmMain `UIFBPSaveService` orchestrates filesystem write-out, merges with any existing `.flow.kt`, and applies the `build.gradle.kts` touch-up. No new modules, no cross-module dependency changes, no changes to `fbpDsl`, `flowGraph-execute`, `flowGraph-inspect`, or `preview-api`. The `TestModule` cleanup (deleting `saved/`) is a one-time migration documented in `quickstart.md`. Note: a future feature is expected to deprecate the four `Runtime*Generator` reuses by collapsing all modules onto a universal `DynamicPipelineController`-based runtime in fbpDsl; UI-FBP modules will then drop their thick generated files in lockstep with entity modules.

## Complexity Tracking

> No constitutional violations to justify. No new top-level modules, no new dependency surface, no new cross-cutting patterns introduced.
