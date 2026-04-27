# Implementation Plan: Collapse the Entity-Module Thick Runtime onto DynamicPipelineController

**Branch**: `085-collapse-thick-runtime` | **Date**: 2026-04-27 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/Users/dhaukoos/CodeNodeIO/specs/085-collapse-thick-runtime/spec.md`

## Summary

`DynamicPipelineController` (in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineController.kt`) and `DynamicPipelineBuilder` together already subsume every runtime behavior that today's per-module `{Module}Controller.kt` + `{Module}Flow.kt` pair provides — node-runtime instantiation, channel wiring, lifecycle orchestration, attenuation, emission/value observers, and reset. The per-module `{Module}ControllerAdapter.kt` is pure delegation. Three small gaps remain: (1) `DynamicPipelineController` does not expose `getStatus()`, and `KMPMobileApp/src/androidUnitTest/.../StopWatchIntegrationTest.kt:369-378` does call it on a per-module Controller; (2) per-module `{Module}ControllerInterface` does not extend `io.codenode.fbpdsl.runtime.ModuleController`, so production consumers cannot reach `setAttenuationDelay`/observers through the typed interface; (3) production deployment has no filesystem scanning, so it needs a per-module way to resolve node names → `CodeNodeDefinition` objects (today implicit via the imports inside `{Module}Flow.kt`).

This plan closes those three gaps and replaces the eliminated trio (`Controller.kt` + `ControllerAdapter.kt` + `Flow.kt`) with **one new small per-module file**: `{Module}Runtime.kt`. That file contains an `object {Module}NodeRegistry { fun lookup(...) }` plus a top-level `create{Module}Runtime(flowGraph): {Module}ControllerInterface` factory whose body wires `DynamicPipelineController` to the registry and delegates the typed `StateFlow` getters to the existing module `State` object. Three generators (`RuntimeControllerGenerator`, `RuntimeControllerAdapterGenerator`, `RuntimeFlowGenerator`) are removed; one new generator (`ModuleRuntimeGenerator`) takes their place. `{Module}ControllerInterface` is changed to extend `ModuleController` (interface-shape change is benign because the GraphEditor's reflection proxy still satisfies it for the methods it cares about). All five reference modules are regenerated to the new shape, and KMPMobileApp's instantiation sites are updated atomically with that regeneration so the main branch never observes a broken intermediate state.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP — Kotlin Multiplatform). Generators run on JVM; generated source spans `commonMain` (KMP) only for the new file (the eliminated trio's `flow/` placement is gone).

**Primary Dependencies**:
- Universal runtime (target sufficient surface): `fbpDsl/src/commonMain/.../runtime/{DynamicPipelineController, DynamicPipelineBuilder, DynamicPipeline, ModuleController, RuntimeRegistry, NodeDefinitionLookup}.kt`. Today's `RootControlNode` (in `fbpDsl/.../model/`) provides per-node state inspection via `FlowExecutionStatus` — needed for `getStatus()` parity.
- Generator side: `flowGraph-generate/src/commonMain/.../generator/{RuntimeControllerInterfaceGenerator}.kt` (kept, modified to add `: ModuleController` superinterface clause); the three generators slated for removal (`RuntimeControllerGenerator`, `RuntimeControllerAdapterGenerator`, `RuntimeFlowGenerator`); a new `ModuleRuntimeGenerator` (this feature) emits `{Module}Runtime.kt`. Module orchestration (whatever wires generators today — `ModuleGenerator.kt`) updated to call the new generator and skip the removed three.
- Runtime Preview side (no changes expected): `flowGraph-execute/src/jvmMain/.../ModuleSessionFactory.kt` continues to build `DynamicPipelineController` and the reflection proxy. The interface-shape change (extending `ModuleController`) does not require proxy updates because the proxy returns null for unhandled `setXxx` calls and `ModuleSessionFactory` invokes attenuation/observer methods on the dynamic controller directly, not via the proxy.
- Production-app consumer (one site): `CodeNodeIO-DemoProject/KMPMobileApp/src/commonMain/.../mobileapp/App.kt:56-72` (StopWatch + UserProfiles instantiation) and `KMPMobileApp/src/androidUnitTest/.../mobileapp/StopWatchIntegrationTest.kt:121-203` (multiple StopWatchController instantiations + a `controller.getStatus()` call at line 375).

**Storage**: N/A. Generator emits source files to module directory trees; no new persistence.

**Testing**:
- Unit tests for the new `ModuleRuntimeGenerator` in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/ModuleRuntimeGeneratorTest.kt`.
- Unit tests for `DynamicPipelineController.getStatus()` (new method) in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineControllerTest.kt`.
- Updated `KMPMobileApp/src/androidUnitTest/.../StopWatchIntegrationTest.kt` — same test logic, only constructor calls switched from `StopWatchController(graph)` to `createStopWatchRuntime(graph)`.
- Integration test in `flowGraph-execute/src/jvmTest/kotlin/io/codenode/flowgraphexecute/ModuleSessionFactoryRegressionTest.kt` exercising each of the five regenerated modules through Runtime Preview's session-creation path to assert no regressions in proxy behavior.
- Manual quickstart verification covering the GraphEditor Runtime Preview behavior for all five reference modules (no behavioral change expected vs. pre-collapse).

**Target Platform**: KMP modules continue to target jvm + android + iOS (per existing module configurations). The new `{Module}Runtime.kt` lives in `commonMain` and uses no platform-specific APIs.

**Project Type**: Multi-module Gradle composite build (CodeNodeIO + DemoProject). No new top-level modules; this feature only modifies existing `fbpDsl` (one method add, one interface clause add), modifies `flowGraph-generate` (remove three generators, add one, modify one), modifies five DemoProject modules (regeneration), and modifies one DemoProject app (KMPMobileApp instantiation sites).

**Performance Goals**: Runtime behavior identical to today (per spec SC-005). Generator wall-time per module remains under 1 second (existing target). No new hot paths.

**Constraints**:
- KMP-first: every change in `fbpDsl` and every generator output must be `commonMain`-clean. Lifecycle binding (today's `bindToLifecycle(Lifecycle)` on the per-module Controller) is Android-specific and is OUT OF SCOPE for this feature — KMPMobileApp does not call `bindToLifecycle` (verified by grep), so no production regression.
- The `{Module}ControllerInterface` FQCN (`io.codenode.{modulename}.controller.{Module}ControllerInterface`) MUST be preserved (FR-007). Internal shape may change provided the GraphEditor's reflection proxy continues to work.
- KMPMobileApp's source updates MUST land in the same change set as the module regeneration (FR-014). This means a single PR / commit boundary, not split.
- The collapse MUST be reversible at the per-module level during the rollout — each module's regeneration is independent, so partial migration is possible mid-feature for testing, but the final state requires all five modules and KMPMobileApp updated.

**Scale/Scope**: Bounded. One method added to `fbpDsl` (`getStatus()` on `DynamicPipelineController` + `ModuleController`). One generator added, three removed in `flowGraph-generate`. Five DemoProject modules regenerated (deletes ~1,400 lines of generated boilerplate; adds ~150 lines of new generated boilerplate — ~30 lines per module × 5). KMPMobileApp's `App.kt` (~10 line diff) and `StopWatchIntegrationTest.kt` (~20 line diff) updated. No cross-module API changes outside the deliberate Controller/Adapter/Flow elimination.

## Constitution Check

*Initial gate evaluated against `.specify/memory/constitution.md` v1.0.0.*

### Licensing & IP

- **Static linking rule**: No new dependencies introduced. All affected code (`fbpDsl`, `flowGraph-generate`, generated module sources, KMPMobileApp) stays Apache 2.0. **PASS**.
- **Header management**: All new generator output must carry the standard project header above the `package` declaration. The new `ModuleRuntimeGenerator` will replicate the existing UI-FBP/entity-module generator pattern (`/* * <Name> * License: Apache 2.0 */`). **PASS**.
- **Transitives**: No new external dependencies. **PASS**.

### I. Code Quality First

- Single responsibility: the new `ModuleRuntimeGenerator` has one method; the new `getStatus()` on `DynamicPipelineController` exposes existing internal state. Per-module generated `{Module}Runtime.kt` is small, declarative, and module-specific.
- Type safety: pure Kotlin throughout; no reflection in generators; production-app code calls strongly-typed factory functions returning a typed `{Module}ControllerInterface`.
- Documentation: generated files carry the standard `Generated by CodeNodeIO ModuleRuntimeGenerator` marker comment.
- Security: no input boundaries crossed; generator inputs are local module specifications. **PASS**.

### II. Test-Driven Development

- New `getStatus()` method gets a unit test in `fbpDsl` written first asserting the FlowExecutionStatus shape and that it reflects current pipeline state.
- New `ModuleRuntimeGenerator` gets a unit test asserting the emitted file contents and shape (registry entries, factory function signature, interface implementation).
- KMPMobileApp's `StopWatchIntegrationTest` migration: tests stay; only instantiation lines change. The pre-existing `controller_getStatus_returns_FlowExecutionStatus` (line 369) is the proof-of-need for `getStatus()` parity.
- A new regression test in `flowGraph-execute/jvmTest` exercises Runtime Preview's session-creation path against each of the five regenerated modules.
- Coverage: 100% line coverage on the new generator (string template); >80% on the new `getStatus()` method (it's a thin wrapper over existing `RootControlNode.getStatus()`). **PASS**.

### III. User Experience Consistency

- The user-facing surface (GraphEditor Runtime Preview behavior) is invariant per spec US3 / SC-005. KMPMobileApp's runtime UX is invariant per US2 / SC-004.
- Error handling: if `ModuleRuntimeGenerator` is asked to delete a hand-edited file at a generator-target path, it MUST refuse with a clear, actionable warning per FR-013. **PASS**.

### IV. Performance Requirements

- No new hot paths. The dynamic pipeline path is what Runtime Preview already uses today; production now uses the same path. No measured perf delta expected. **PASS**.

### V. Observability & Debugging

- Generated files retain the `Generated by` marker comment so any future debugger can trace artifacts back to the new generator.
- Re-generation MUST emit a structured summary listing created/updated/unchanged/deleted files, including any `SKIPPED_CONFLICT` outcomes per FR-013. (This mirrors the structured-summary pattern already established in feature 084's planning.)
- No logging of secrets; all generated content from local sources. **PASS**.

**Gate result: PASS — no violations to justify.**

## Project Structure

### Documentation (this feature)

```text
specs/085-collapse-thick-runtime/
├── plan.md              # This file
├── research.md          # Phase 0 — gap audit and design decisions
├── data-model.md        # Phase 1 — generator inputs/outputs, file-emission table, runtime contract
├── quickstart.md        # Phase 1 — manual verification path covering all 5 modules + KMPMobileApp
├── contracts/
│   └── universal-runtime.md  # The {Module}ControllerInterface ↔ DynamicPipelineController contract
└── tasks.md             # Phase 2 — created by /speckit.tasks
```

### Source Code (repository root)

This is a multi-module Gradle composite build. The change touches three modules in `CodeNodeIO/`, regenerates five modules in the sibling `CodeNodeIO-DemoProject/`, and updates one app in DemoProject.

```text
CodeNodeIO/
├── fbpDsl/
│   ├── src/commonMain/kotlin/io/codenode/fbpdsl/runtime/
│   │   ├── DynamicPipelineController.kt           # MODIFY: add getStatus(): FlowExecutionStatus
│   │   ├── DynamicPipeline.kt                     # MODIFY: expose getStatus() (wraps RootControlNode)
│   │   └── ModuleController.kt                    # MODIFY: add fun getStatus(): FlowExecutionStatus to interface
│   └── src/commonTest/kotlin/io/codenode/fbpdsl/runtime/
│       └── DynamicPipelineControllerTest.kt       # MODIFY/NEW: add getStatus() test
│
├── flowGraph-generate/
│   └── src/
│       ├── commonMain/kotlin/io/codenode/flowgraphgenerate/generator/
│       │   ├── ModuleRuntimeGenerator.kt          # NEW: emits {Module}Runtime.kt
│       │   ├── RuntimeControllerInterfaceGenerator.kt # MODIFY: emit "interface X : ModuleController { ... }" superinterface clause
│       │   ├── RuntimeControllerGenerator.kt      # DELETE
│       │   ├── RuntimeControllerAdapterGenerator.kt # DELETE
│       │   ├── RuntimeFlowGenerator.kt            # DELETE
│       │   └── ModuleGenerator.kt                 # MODIFY: invoke ModuleRuntimeGenerator; stop invoking the three deleted generators
│       └── commonTest/kotlin/io/codenode/flowgraphgenerate/generator/
│           ├── ModuleRuntimeGeneratorTest.kt      # NEW
│           ├── RuntimeControllerInterfaceGeneratorTest.kt # MODIFY: assert ": ModuleController" appears
│           ├── RuntimeControllerGeneratorTest.kt  # DELETE
│           ├── RuntimeControllerAdapterGeneratorTest.kt # DELETE
│           └── RuntimeFlowGeneratorTest.kt        # DELETE
│
└── flowGraph-execute/
    └── src/jvmTest/kotlin/io/codenode/flowgraphexecute/
        └── ModuleSessionFactoryRegressionTest.kt  # NEW: per-module Runtime Preview regression coverage post-collapse

CodeNodeIO-DemoProject/
├── StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/
│   ├── StopWatchRuntime.kt                        # NEW (generated)
│   ├── controller/StopWatchControllerInterface.kt # MODIFY (regenerated): now extends ModuleController
│   ├── controller/StopWatchController.kt          # DELETE
│   ├── controller/StopWatchControllerAdapter.kt   # DELETE
│   └── flow/StopWatchFlow.kt                      # DELETE (StopWatch.flow.kt — the user-authored DSL — kept)
│
├── Addresses/src/commonMain/kotlin/io/codenode/addresses/
│   ├── AddressesRuntime.kt                        # NEW (generated)
│   ├── controller/AddressesControllerInterface.kt # MODIFY (regenerated): now extends ModuleController
│   ├── controller/AddressesController.kt          # DELETE
│   ├── controller/AddressesControllerAdapter.kt   # DELETE
│   └── flow/AddressesFlow.kt                      # DELETE
│
├── UserProfiles/                                  # SAME pattern as Addresses
│   ├── (UserProfilesRuntime.kt NEW; Controller/Adapter/Flow.kt DELETED; ControllerInterface MODIFIED)
│
├── EdgeArtFilter/                                 # SAME pattern as StopWatch (no persistence)
│   ├── (EdgeArtFilterRuntime.kt NEW; Controller/Adapter/Flow.kt DELETED; ControllerInterface MODIFIED)
│
├── WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/
│   ├── WeatherForecastRuntime.kt                  # NEW (generated) — WeatherForecast had no thick stack to remove; gains production-deployability
│   └── controller/WeatherForecastControllerInterface.kt # MODIFY (regenerated): now extends ModuleController
│
└── KMPMobileApp/
    └── src/
        ├── commonMain/kotlin/io/codenode/mobileapp/
        │   └── App.kt                             # MODIFY: replace StopWatchController(...)/UserProfilesController(...) call sites with createStopWatchRuntime(...)/createUserProfilesRuntime(...); drop ControllerAdapter wrapping
        └── androidUnitTest/kotlin/io/codenode/mobileapp/
            └── StopWatchIntegrationTest.kt        # MODIFY: replace constructor calls (~6 sites); test logic unchanged; getStatus() call at line 375 now goes via the {Module}ControllerInterface (which inherits getStatus from ModuleController)
```

**Structure Decision**: The collapse is implemented in three coordinated edits: (1) a small `fbpDsl` enhancement (`getStatus()` on `ModuleController` + `DynamicPipelineController`); (2) a generator swap in `flowGraph-generate` (delete 3 generators + their tests, add 1 new generator + its test, modify the interface generator + its orchestrator); (3) a coordinated DemoProject rewrite (regenerate 5 modules, update 1 consumer app + its tests). The new `{Module}Runtime.kt` file is the single per-module generated artifact replacing the eliminated trio — its structure is fully prescribed by `data-model.md`. No new modules, no cross-cutting architectural patterns, no changes to `flowGraph-execute`, `flowGraph-inspect`, `preview-api`, or any other module's runtime path.

## Complexity Tracking

> No constitutional violations to justify. The interface-shape change (`{Module}ControllerInterface : ModuleController`) is documented in `contracts/universal-runtime.md` along with proof that the GraphEditor reflection proxy continues to satisfy it.
