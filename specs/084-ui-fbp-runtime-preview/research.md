# Phase 0 Research: UI-FBP Runtime Preview Gap Analysis

**Date**: 2026-04-26 (drafted) / 2026-04-28 (cross-checked against 085)
**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Cross-check**: [CROSS-CHECK-085.md](./CROSS-CHECK-085.md)

> **POST-085 INTEGRATION NOTE (2026-04-28)**: Feature 085 (universal-runtime collapse) shipped 2026-04-28. Decision 1 below is **fully retired** — the thick stack it specified (`{Module}Controller.kt` + `{Module}ControllerAdapter.kt` + `{Module}Flow.kt` runtime) no longer exists; the three generators that produced it (`RuntimeControllerGenerator`, `RuntimeControllerAdapterGenerator`, `RuntimeFlowGenerator`) are deleted. UI-FBP rides the universal runtime collapse just like every other module. Decisions 2 and 4 are **partially valid** — the ControllerInterface and ViewModel shapes survive, but the interface now extends `ModuleController` (control surface inherited; UI-FBP's per-port flow members become `override val`). Decision 3 (`PreviewProvider` in `jvmMain`) is **already implemented** by the new `PreviewProviderGenerator`. Decisions 5–8 survive structurally. The `UIFBPSpecAdapter` and likely the `UIFBPControllerInterfaceGenerator` are unnecessary because their target generators are gone or have absorbed the UI-FBP shape. See [CROSS-CHECK-085.md](./CROSS-CHECK-085.md) for the line-by-line delta.

## Purpose

This research consolidates the reverse-engineered runtime contract that any UI-FBP-generated module must satisfy in order for the GraphEditor's Runtime Preview panel to load, render, and execute it. It also documents the precise delta between today's UI-FBP generator output (as observed in `CodeNodeIO-DemoProject/TestModule` with `DemoUI.kt`) and that contract.

All claims below are grounded in source files referenced by absolute path; line numbers are accurate as of the branch starting point.

---

## Decision 1 (RETIRED 2026-04-28 by feature 085): ~~Generate the full thick stack (ControllerInterface + Controller + ControllerAdapter + Flow runtime + PreviewProvider), matching today's entity-module pattern, so the module is both Runtime-Preview-loadable AND deployable to a production app~~

> **Status**: This decision is fully retired. Feature 085 (universal-runtime collapse) eliminated the thick stack across all modules. UI-FBP modules now emit `controller/{Module}ControllerInterface.kt` (extending `ModuleController`) + `controller/{Module}Runtime.kt` (factory `create{Module}Runtime(flowGraph)` returning an anonymous `object : {Module}ControllerInterface, ModuleController by controller`). The deployable contract is satisfied by the Runtime factory; production-app consumers wire it as documented in `specs/085-collapse-thick-runtime/quickstart.md` VS-D5. The `RuntimeControllerGenerator`, `RuntimeControllerAdapterGenerator`, `RuntimeFlowGenerator` referenced below are **deleted**. The "background — what an earlier draft got wrong" subsection in this decision was correct on its own terms at the time of writing, but the deployment-path argument is now moot because the deployment path also goes through the universal runtime.

**Original decision content (historical, retained for context):**

**Background — what an earlier draft of this research got wrong**: An earlier version of this decision claimed that `Controller.kt`, `ControllerAdapter.kt`, and `Flow.kt` runtime files were no longer needed because `ModuleSessionFactory.createControllerProxy` (`flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/ModuleSessionFactory.kt:108-150`) builds a `java.lang.reflect.Proxy` over `DynamicPipelineController` for every module. That observation is correct **for the GraphEditor's Runtime Preview path only**, but it confused two distinct runtime paths:

1. **GraphEditor Runtime Preview path** (in-process, reflection-based): `ModuleSessionFactory` constructs a `DynamicPipelineController` and a proxy implementing the module's `ControllerInterface`. Per-module `Controller.kt` / `Adapter.kt` / `Flow.kt` are NOT used here — verified by inspection of `ModuleSessionFactory.kt:51-101`.
2. **Production-app deployment path** (a built Android/iOS/Desktop app importing the module as a library): The app instantiates the module's per-module `Controller` directly (e.g., `AddressesController(flowGraph)`), which constructs its hand-coded `AddressesFlow()` runtime, wires nodes, and bridges to the app's `ViewModel`. There is no `ModuleSessionFactory`, no reflection, no dynamic proxy. The thick files ARE used here.

WeatherForecast (the newest module in the DemoProject) omits the thick files and works in Runtime Preview, but it is correspondingly **not deployable** to a production app without the GraphEditor — that is a known scope limitation of WeatherForecast, not a project-wide deprecation of the thick stack.

**Decision (per Q1 in spec Clarifications)**: UI-FBP MUST emit the thick stack so its modules can serve **both** scenarios. The artifact set MUST be structurally identical to what today's entity-module generators (`RuntimeControllerGenerator`, `RuntimeControllerAdapterGenerator`, `RuntimeFlowGenerator`) emit for Addresses / UserProfiles / EdgeArtFilter — same package layout, same class shapes, same constructor signatures. Reference shape: `CodeNodeIO-DemoProject/Addresses/src/commonMain/kotlin/io/codenode/addresses/controller/AddressesController.kt` and sibling `flow/AddressesFlow.kt`.

**Future direction (per Q2 in spec Clarifications)**: A separate follow-up feature is expected to collapse the thick stack onto a universal `DynamicPipelineController`-based runtime in fbpDsl, eliminating most per-module boilerplate (`Controller.kt` shrinks to ~10 lines wrapping `DynamicPipelineController`; `ControllerAdapter.kt` and `Flow.kt` runtime disappear entirely; per-module `NodeRegistry` becomes a tiny generated map). UI-FBP modules will participate in that collapse alongside entity modules. Feature 084 stays focused on bringing UI-FBP up to parity with the current entity-module pattern; it does not pioneer the universal-runtime collapse.

**Alternatives considered**:
- *Thin / preview-only* (the original Decision 1). Rejected per Q1 — leaves UI-FBP modules un-deployable.
- *Pioneer the universal-runtime collapse in 084 itself* (only for UI-FBP; entity modules unchanged). Rejected per Q2 — risks designing the universal shape around UI-FBP-only constraints; better to collapse all modules together in a dedicated feature.
- *Collapse globally inside 084.* Rejected per Q2 — blast radius too large; would expand 084 to a project-wide refactor.

**Source references**:
- `flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/ModuleSessionFactory.kt:51-150` (Runtime Preview path)
- `CodeNodeIO-DemoProject/Addresses/src/commonMain/kotlin/io/codenode/addresses/controller/AddressesController.kt` (thick deployable Controller — reference shape)
- `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeControllerGenerator.kt` (existing generator UI-FBP will reuse / call into)
- `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeControllerAdapterGenerator.kt`
- `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeFlowGenerator.kt`
- `CodeNodeIO-DemoProject/WeatherForecast/` (counter-example: thin-only, not deployable without GraphEditor)

---

## Decision 2: `{Module}ControllerInterface.kt` follows the WeatherForecast / entity-module shape — minimal control surface, with state-flow getters for each port to satisfy the GraphEditor proxy

**Rationale**: `ModuleSessionFactory.createViewModel` at line 87-88 attempts to load the controller interface from exactly two FQCNs (in order):

1. `io.codenode.{moduleName.lowercase()}.controller.{ModuleName}ControllerInterface` — **new layout** (target)
2. `io.codenode.{moduleName.lowercase()}.generated.{ModuleName}ControllerInterface` — legacy fallback (do not emit here)

The proxy handler (lines 114-142) intercepts each call by `method.name`:
- `start`/`stop`/`pause`/`resume`/`reset` → invoke `DynamicPipelineController.{same}()`, return current `FlowGraph`.
- `getExecutionState` → return `controller.executionState`.
- Any other `getXxx` → reflectively read `{Module}State.xxxFlow` field.

**However**, in production-app deployment the consumer is the per-module `{Module}Controller` class (not the proxy), and its constructor wires its own `{Module}Flow()` runtime. The interface methods in that path are implemented by `{Module}Controller` directly, returning `FlowGraph` from its `controller.startAll()` etc.

Generated interface (template — matches `AddressesControllerInterface` / `WeatherForecastControllerInterface` shape):

```kotlin
package io.codenode.{modulename}.controller

import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowGraph
import kotlinx.coroutines.flow.StateFlow
// + IP type imports per spec.ipTypeImports

interface {Module}ControllerInterface {
    // For each port y exposed by {Module}State as yyyFlow:
    val {y}: StateFlow<{T}>
    // ...
    val executionState: StateFlow<ExecutionState>
    fun start(): FlowGraph
    fun stop(): FlowGraph
    fun pause(): FlowGraph
    fun resume(): FlowGraph
    fun reset(): FlowGraph
}
```

The set of `val xxx: StateFlow<T>` properties is derived from `UIFBPSpec.sinkInputs` (display outputs the UI observes); source outputs are not exposed on the interface because the UI does not observe them as flows in the entity-module pattern. **All flow names emitted in the interface MUST exist as `xxxFlow` properties on `{Module}State`** — already true for current UI-FBP-generated state objects (verified in `TestModule/src/commonMain/kotlin/io/codenode/demo/viewmodel/DemoUIState.kt`). This satisfies both the GraphEditor proxy's reflection lookup AND the production-app `{Module}Controller`'s direct field implementation (where the controller exposes the per-port `StateFlow`s as members backed by its `{Module}Flow` runtime).

**Alternatives considered**:
- *Minimal interface (executionState + control methods only, like WeatherForecast).* Rejected — works for Runtime Preview where the ViewModel reads State directly, but for production-app deployment we want the controller surface to expose the typed flows so consuming apps can bind to the interface (not to a concrete State object).
- *Mirror source outputs as readable flows on the interface.* Rejected — entity modules don't do this; the UI emits via `viewModel.emit(...)` but doesn't observe source values back through the controller. Keeping the interface minimal-but-complete (sink ports only) matches existing precedent.
- *Place interface in the legacy `generated/` subpackage.* Rejected — fallback supported but accumulates legacy debt.

**Source references**:
- `flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/ModuleSessionFactory.kt:84-142` (Runtime Preview FQCN + proxy)
- `CodeNodeIO-DemoProject/Addresses/src/commonMain/kotlin/io/codenode/addresses/controller/AddressesControllerInterface.kt` (reference shape — entity module)
- `CodeNodeIO-DemoProject/WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/controller/WeatherForecastControllerInterface.kt` (reference shape — minimal variant)

---

## Decision 3: Generate `{Module}PreviewProvider.kt` in `src/jvmMain/kotlin/{pkg}/userInterface/`

**Rationale**: `DynamicPreviewDiscovery.discoverAndRegister` (`flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/discovery/DynamicPreviewDiscovery.kt:30-54`) walks a directory non-recursively, picks files whose name ends with `PreviewProvider.kt`, parses the `package` declaration and the `object Xxxx PreviewProvider` declaration with a regex, then reflectively loads the class, fetches the `INSTANCE` field, and invokes the `register()` method.

`GraphEditorApp.kt:130-146` walks each module looking at both `src/commonMain/kotlin` and `src/jvmMain/kotlin`, finds any directory named `userInterface`, and runs the discovery on it. So a `PreviewProvider.kt` in either source set is reachable; we choose `jvmMain` to mirror StopWatch's convention and to localize the `preview-api` dependency to the JVM target (it is needed only for desktop preview).

Generated provider (template):

```kotlin
/*
 * {Module}PreviewProvider — Registers {Module} preview composable for the runtime panel
 * License: Apache 2.0
 */

package io.codenode.{modulename}.userInterface

import androidx.compose.ui.Modifier
import io.codenode.previewapi.PreviewRegistry
import io.codenode.{modulename}.viewmodel.{Module}ViewModel

object {Module}PreviewProvider {
    fun register() {
        PreviewRegistry.register("{Module}") { viewModel, modifier ->
            val vm = viewModel as {Module}ViewModel
            {Module}(viewModel = vm, modifier = modifier)
        }
    }
}
```

The lookup key is the **Composable function name**, not the module name. In `RuntimePreviewPanel.kt:108-116`, `composables` is discovered by scanning the userInterface dir for `@Composable fun X(viewModel: ...)` declarations, and the panel defaults to the entry whose name equals `flowGraphName`. UI-FBP today derives `flowGraphName` from `spec.moduleName` (which, for current usage, equals the Composable function name). The PreviewProvider registers under that same name. **Edge alignment requirement**: if a future change makes the Composable function name diverge from the module name, this contract breaks; the generator MUST register under the *Composable function name*, which is captured today as `spec.moduleName` and validated at parse time.

**Alternatives considered**:
- *Place PreviewProvider in `commonMain`.* Possible (PreviewRegistry is in `preview-api/commonMain`). Rejected — StopWatch precedent puts it in `jvmMain`, and keeping the `preview-api` dependency JVM-only avoids dragging a Compose-Desktop-flavored API into Android/iOS targets.
- *Register multiple Composables per module (à la StopWatch's `StopWatch` + `StopWatchScreen`).* Out of scope — UI-FBP's input is exactly one qualifying Composable. Multi-UI support is gated by FR-017.

**Source references**:
- `flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/discovery/DynamicPreviewDiscovery.kt:30-54`
- `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorApp.kt:130-146`
- `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/RuntimePreviewPanel.kt:108-116, 435-450`
- `preview-api/src/commonMain/kotlin/io/codenode/previewapi/PreviewRegistry.kt`

---

## Decision 4: Modify the generated `ViewModel` to take `({Module}ControllerInterface)` and follow the entity-module ViewModel shape — flows from State, control methods through controller

**Rationale**: `ModuleSessionFactory.tryCreateViewModel` (lines 157-188) attempts ViewModel construction in this order:

1. Single-arg constructor whose first parameter accepts the `ControllerInterface`.
2. Two-arg constructor `(ControllerInterface, Dao)` — used by entity modules with Koin-resolved DAOs.

If neither matches, `createSession` falls back to returning `RuntimeSession(controller, Any(), ...)` (line 69), and the cast `viewModel as {Module}ViewModel` inside the PreviewProvider throws `ClassCastException` at runtime. So the generated ViewModel **must** declare a public single-arg constructor that accepts the ControllerInterface.

The current generated ViewModel (`TestModule/src/commonMain/kotlin/io/codenode/demo/viewmodel/DemoUIViewModel.kt`) has a no-arg constructor, exposes flows directly from `{Module}State`, and provides an `emit(...)` that mutates `{Module}State` fields. Redesign (matches `WeatherForecastViewModel` / `AddressesViewModel` shape):

```kotlin
package io.codenode.{modulename}.viewmodel

import androidx.lifecycle.ViewModel
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.{modulename}.controller.{Module}ControllerInterface
import io.codenode.{modulename}.{Module}State    // imports State from same module
import kotlinx.coroutines.flow.StateFlow
// + IP type imports

class {Module}ViewModel(
    private val controller: {Module}ControllerInterface
) : ViewModel() {
    // Observable state from module properties — read directly from State
    val {y}: StateFlow<{T}> = {Module}State.{y}Flow
    // ... (one per sinkInput)

    // Execution state from controller
    val executionState: StateFlow<ExecutionState> = controller.executionState

    // Source emit method — writes to State mutable fields
    fun emit({sourceOutputs as parameters}) {
        {Module}State._{a}.value = {a}
        // ... etc.
    }

    // Control methods delegate to controller
    fun start(): FlowGraph = controller.start()
    fun stop(): FlowGraph = controller.stop()
    fun pause(): FlowGraph = controller.pause()
    fun resume(): FlowGraph = controller.resume()
    fun reset(): FlowGraph = controller.reset()
}
```

This matches both reference shapes: `WeatherForecastViewModel` (flows read from `WeatherForecastState.xxxFlow` directly, control methods through controller) and `AddressesViewModel` (same pattern with an additional DAO injected for repository observation). The pattern works with both runtime paths:
- **Runtime Preview**: the GraphEditor's proxy implements the interface; `controller.start()` etc. delegate to `DynamicPipelineController`. The flows the ViewModel reads from `{Module}State` are mutated by Source/Sink CodeNodes running inside the dynamic pipeline.
- **Production app**: the per-module `{Module}Controller` implements the interface directly; `controller.start()` constructs the hand-coded `{Module}Flow()` runtime. The same `{Module}State` fields are mutated by the same Source/Sink CodeNodes (which use the same factory methods regardless of which controller drives them).

The `emit(...)` method continues to write directly to `{Module}State._x` mutable fields. The Source CodeNode's runtime body observes those fields and emits into the FBP graph.

**Alternatives considered**:
- *Keep the no-arg constructor.* Rejected — fails ModuleSessionFactory's reflection lookup; PreviewProvider cast throws.
- *Read flows through the controller (`val y = controller.y`) instead of directly from State.* Rejected — diverges from WeatherForecast/Addresses precedent and adds an unnecessary indirection layer (the controller's flow accessors ultimately resolve to State fields anyway).
- *Use a property setter for the controller.* Rejected — diverges from the entity-module convention.

**Source references**:
- `flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/ModuleSessionFactory.kt:157-188` (tryCreateViewModel)
- `CodeNodeIO-DemoProject/WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/viewmodel/WeatherForecastViewModel.kt` (reference shape — minimal)
- `CodeNodeIO-DemoProject/Addresses/src/commonMain/kotlin/io/codenode/addresses/viewmodel/AddressesViewModel.kt` (reference shape — with DAO)
- `CodeNodeIO-DemoProject/TestModule/src/commonMain/kotlin/io/codenode/demo/viewmodel/DemoUIViewModel.kt` (current UI-FBP shape — to be revised)

---

## Decision 5: Ensure the module's `build.gradle.kts` has a `jvm()` target and a `jvmMain` `preview-api` dependency

**Rationale**: The existing `TestModule/build.gradle.kts` declares `androidTarget()` only — no `jvm()` target. Without `jvm()`, the module's compiled classes are not on the GraphEditor's JVM classpath, so `Class.forName(fqcn)` in `ModuleSessionFactory` and `DynamicPreviewDiscovery` will return `ClassNotFoundException` and silently skip. The PreviewProvider also depends on `io.codenode:preview-api`, which is not declared in `TestModule` today.

Required edits to `build.gradle.kts`:

1. Add `jvm { compilations.all { kotlinOptions.jvmTarget = "17" } }` block.
2. Add `val jvmMain by getting { dependencies { implementation("io.codenode:preview-api") } }` source-set block.
3. Apply the default hierarchy template if not already present (so iOS source sets work alongside jvm).

The `UIFBPSaveService` (new) is responsible for applying these edits idempotently — a presence check for `jvm {` and for the `preview-api` dependency, with insertions only if missing. Robustness expectation: if the module's `build.gradle.kts` is not in a recognized format (e.g., heavily customized), the service MUST emit a warning naming the missing pieces and skip the auto-edit, leaving the user to apply them by hand.

**Alternatives considered**:
- *Have the user wire the build script manually.* Rejected — violates SC-007 ("no documentation needed about additional generated files or build wiring").
- *Have the project-level scaffold pre-add `jvm()` to every new module.* Out of scope per spec assumption ("module scaffolding handled separately"). However, an aligned change in module scaffolding to default to `jvm()` would make this gradle touch-up a no-op for new modules — a follow-up worth flagging.

**Source references**:
- `CodeNodeIO-DemoProject/TestModule/build.gradle.kts` (current shape)
- `CodeNodeIO-DemoProject/StopWatch/build.gradle.kts:17-79` (target shape we're aligning to)

---

## Decision 6: Remove the legacy `saved/` package; UI-FBP regenerates only into `viewmodel/`

**Rationale**: `TestModule/src/commonMain/kotlin/io/codenode/demo/saved/` contains a parallel pair of `DemoUIState.kt` and `DemoUIViewModel.kt` that conflict with the same files in `viewmodel/`. The `userInterface/DemoUI.kt` currently imports from `saved/` (`import io.codenode.demo.saved.DemoUIViewModel`), and this is the legacy import path. The runtime side (`ModuleSessionFactory:84-98`) prefers `viewmodel/` first.

The current `UIFBPInterfaceGenerator.generateAll` (line 31 of the existing source) writes State and ViewModel to `src/commonMain/kotlin/$basePath/${spec.moduleName}State.kt` and `${spec.viewModelTypeName}.kt` — i.e., the **base package**, not `saved/` and not `viewmodel/`. So the `saved/` files in TestModule are stale output from an even older generator iteration. Today's UI-FBP would emit to base package; we are changing it to emit to `viewmodel/` to match the new layout the runtime prefers.

Plan:

- Generator emits all State/ViewModel files to `commonMain/kotlin/$basePath/viewmodel/`.
- `UIFBPSaveService` MAY (with user opt-in via flag, default off) delete known-stale-locations: a `saved/` directory containing files matching the old naming convention, AND any `${moduleName}State.kt` / `${moduleName}ViewModel.kt` directly in the base package. Default-off because deleting user files without consent violates the "behavior under conflict" requirement (FR-016).
- For the `TestModule` migration, `quickstart.md` documents a one-line manual `rm -rf src/commonMain/kotlin/io/codenode/demo/saved/` step and updates `DemoUI.kt` to import from `viewmodel/`. After that one-time migration, re-running UI-FBP keeps the module clean.

**Alternatives considered**:
- *Always delete legacy locations.* Rejected — risks deleting user-edited files.
- *Leave duplicates in place.* Rejected — fails SC-002 (clean compile) due to duplicate symbol declarations and breaks `ModuleSessionFactory`'s state lookup (it picks one and the other's mutations go to a phantom State).

---

## Decision 7: Re-generation preserves user-added content of `.flow.kt` by merging at the FlowGraph DSL level, not by rewriting the file

**Rationale**: FR-011/FR-012 require that re-running UI-FBP not destroy user-added CodeNodes and connections. The simplest implementation is:

1. If `.flow.kt` does not exist, emit the bootstrap (existing behavior of `generateBootstrapFlowKt`).
2. If `.flow.kt` exists, parse it via `flowGraph-persist/FlowKtParser` to obtain a `FlowGraph` model, compute the new Source/Sink CodeNode port sets from `UIFBPSpec`, then update the existing CodeNodes in-place: add new ports, remove ports the user no longer has in the UI, and drop only those connections that reference removed ports. Re-serialize via `flowGraph-persist/FlowGraphSerializer`.
3. Surface a structured summary listing: new ports added, ports removed, connections dropped (with from/to identifiers), and a flag indicating whether user-added CodeNodes were preserved (always true if step 2 succeeds).

If parsing fails (the user manually edited `.flow.kt` into a non-parseable shape), the generator MUST NOT overwrite — it MUST emit an error and direct the user to fix or remove the file. This satisfies FR-016.

**Alternatives considered**:
- *Always overwrite with bootstrap.* Rejected — destroys user work; fails FR-011/012.
- *Append-only (never modify existing CodeNodes).* Rejected — won't propagate UI signature changes; user would need to manually edit Source/Sink ports.
- *Diff at the text level.* Rejected — fragile; `.flow.kt` is structured DSL and round-trips cleanly through the existing parser/serializer.

**Source references**:
- `flowGraph-persist/src/commonMain/kotlin/io/codenode/flowgraphpersist/FlowKtParser.kt`
- `flowGraph-persist/src/commonMain/kotlin/io/codenode/flowgraphpersist/FlowGraphSerializer.kt`
- `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPInterfaceGenerator.kt:75-110` (current bootstrap)

---

## Decision 8: All new generators live in `commonMain` of `flowGraph-generate`; the filesystem-touching `UIFBPSaveService` lives in `jvmMain`

**Rationale**: The existing UI-FBP generators are all in `commonMain` (verified by inspecting `UIFBPInterfaceGenerator.kt`). They produce `String` content from `UIFBPSpec` input and have no I/O. Maintaining this discipline keeps the generators pure and unit-testable in `commonTest` without filesystem fixtures.

The merge logic for `.flow.kt` (Decision 7) and the `build.gradle.kts` touch-up (Decision 5) are I/O- and project-aware and live in a new `UIFBPSaveService` in `jvmMain`. This service composes the pure generators, calls the `flowGraph-persist` parser/serializer (also commonMain but invoked from jvm), writes outputs, and emits the structured summary. Tests for `UIFBPSaveService` are integration tests in `jvmTest` against tmpdir fixtures.

**Alternatives considered**:
- *Put everything in `jvmMain`.* Rejected — breaks KMP-first principle (constitution + memory note `feedback_kmp_first.md`).
- *Put I/O in `commonMain` via `okio`.* Rejected — adds a dependency for marginal benefit; the only consumer is the desktop GraphEditor.

---

## Summary: Closing-the-Gap Checklist

| Artifact | Today (UI-FBP) | Target | Owner |
|---|---|---|---|
| `{Module}State.kt` | Emitted to base package; canonical copy in `viewmodel/` exists in TestModule | Emit to `viewmodel/` | `UIFBPStateGenerator` (path change in orchestrator) |
| `{Module}ViewModel.kt` | No-arg constructor; flows from State directly | `(ControllerInterface)` constructor; flows from State; control methods through controller (matches WeatherForecast/Addresses shape) | `UIFBPViewModelGenerator` (signature change) |
| `{Module}SourceCodeNode.kt` | Emitted to `nodes/` | Unchanged | `UIFBPSourceCodeNodeGenerator` |
| `{Module}SinkCodeNode.kt` | Emitted to `nodes/` | Unchanged | `UIFBPSinkCodeNodeGenerator` |
| `{Module}ControllerInterface.kt` | **Not generated** | Emit to `controller/` | **NEW** `UIFBPControllerInterfaceGenerator` (or reuse `RuntimeControllerInterfaceGenerator` if its inputs align) |
| `{Module}Controller.kt` | **Not generated** | Emit to `controller/` (thick deployable) | **REUSE** `RuntimeControllerGenerator` (entity-module path) |
| `{Module}ControllerAdapter.kt` | **Not generated** | Emit to `controller/` (thick deployable) | **REUSE** `RuntimeControllerAdapterGenerator` |
| `{Module}Flow.kt` (runtime) | **Not generated** | Emit to `flow/` (thick deployable) | **REUSE** `RuntimeFlowGenerator` |
| `{Module}PreviewProvider.kt` | **Not generated** | Emit to `jvmMain/.../userInterface/` | **NEW** `UIFBPPreviewProviderGenerator` |
| `.flow.kt` | Optional bootstrap | Parse-and-merge if present | `UIFBPSaveService` |
| `build.gradle.kts` | Untouched | Idempotent edit: add `jvm()` + `preview-api` | `UIFBPSaveService` |
| Legacy `saved/` package | Present in TestModule | One-time delete (manual or opt-in flag) | `quickstart.md` |

**Reuse strategy**: `RuntimeControllerGenerator`, `RuntimeControllerAdapterGenerator`, `RuntimeFlowGenerator`, and `RuntimeControllerInterfaceGenerator` are existing commonMain generators in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/`. Each takes its own input model (entity-module-flavored). The plan's adapter step is to either (a) construct equivalent input models from `UIFBPSpec` and call them directly, or (b) introduce a thin `UIFBPSpec → {RuntimeControllerSpec, RuntimeFlowSpec, ...}` mapping layer in `UIFBPInterfaceGenerator`. Option (a) is preferred to avoid reimplementing the thick-stack templates; the adapter mapping is a small, well-contained translation.

**Future direction**: The follow-up feature noted in spec Clarifications Q2 is expected to deprecate the four `Runtime*Generator` classes above by introducing a universal `DynamicPipelineController`-based runtime in fbpDsl. UI-FBP modules will then drop their thick generated files in lockstep with entity modules. Until that feature lands, UI-FBP rides the same generator surface as entity modules.

**No NEEDS CLARIFICATION items remain.** All technical-context fields in the plan are populated from inspected source. Spec Clarifications Q1 (thin vs thick: thick) and Q2 (universal-runtime collapse: deferred to a follow-up) have been recorded in spec.md.
