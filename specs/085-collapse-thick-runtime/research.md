# Phase 0 Research: Universal-Runtime Gap Audit

**Date**: 2026-04-27
**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md)

## Purpose

This research consolidates the reverse-engineered runtime contract that the per-module thick stack (`{Module}Controller.kt`, `{Module}ControllerAdapter.kt`, `{Module}Flow.kt`) provides today, and identifies the precise delta between that contract and what `DynamicPipelineController` (in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/`) already provides. The goal is to confirm that the universal runtime can absorb the thick stack with minimal additions, and to enumerate every behavior that requires either an `fbpDsl` enhancement, a generated companion, or an explicit out-of-scope declaration.

All claims below are grounded in inspected source files. Line numbers are accurate as of the branch starting point.

---

## Decision 1: `DynamicPipelineController` is sufficient for the universal runtime, with one small `getStatus()` addition

**Rationale**: A complete walk of the public/protected members of every per-module Controller against `DynamicPipelineController` shows full overlap on lifecycle and observation, with one missing capability:

| Per-module Controller member | `DynamicPipelineController` equivalent | Verdict |
|---|---|---|
| `start(): FlowGraph` | `start()` (line 58-105) | Already supported |
| `stop(): FlowGraph` | `stop()` (line 107-111) | Already supported |
| `pause(): FlowGraph` | `pause()` (line 113-117) | Already supported |
| `resume(): FlowGraph` | `resume()` (line 119-123) | Already supported |
| `reset(): FlowGraph` | `reset()` (line 125-129; calls `onReset` callback at line 127) | Already supported |
| `executionState: StateFlow<ExecutionState>` | `executionState` (line 52) | Already supported |
| `setAttenuationDelay(ms: Long?)` | `setAttenuationDelay()` (line 131-134) | Already supported |
| `setEmissionObserver(...)` | `setEmissionObserver()` (line 136-139) | Already supported |
| `setValueObserver(...)` | `setValueObserver()` (line 141-144) | Already supported |
| `getStatus(): FlowExecutionStatus` (StopWatch line 126; **called at `KMPMobileApp/.../StopWatchIntegrationTest.kt:375`**) | **MISSING** | **GAP — must be added** |
| `setNodeState(nodeId, state): FlowGraph` (StopWatch line 130-134) | **MISSING** | Not used by KMPMobileApp; defer |
| `setNodeConfig(nodeId, config): FlowGraph` (StopWatch line 136-139) | **MISSING** | Not used by KMPMobileApp; defer |
| `bindToLifecycle(lifecycle: Lifecycle)` (StopWatch line 142-166) | **MISSING (Android-specific)** | Not used by KMPMobileApp (verified by grep); explicitly OUT OF SCOPE |
| `currentFlowGraph: FlowGraph` getter (StopWatch line 203-204) | Implicit (returned from `start()`/`stop()`/etc.) | Already supported |
| Per-port `val xxx: StateFlow<T>` (StopWatch line 61-64; Addresses line 61-65) | Not present on `DynamicPipelineController` | Provided by generated `{Module}Runtime.kt` adapter; reads directly from `{Module}State.{x}Flow` |

**The single required `fbpDsl` enhancement**: add `fun getStatus(): FlowExecutionStatus` to `ModuleController` (the interface in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ModuleController.kt`) and to `DynamicPipelineController` (its implementation). The implementation wraps the internal `RootControlNode` instance — `DynamicPipelineController` already constructs one implicitly via `DynamicPipeline` during `start()`. The plan identifies the necessary one-line change in `DynamicPipeline` to retain a reference and expose `getStatus()` upward.

**Two capabilities deferred**: `setNodeState`/`setNodeConfig` are not exercised by KMPMobileApp or any of its tests (verified by grep). They remain available on `RootControlNode` directly should a future feature need them. Adding them to `DynamicPipelineController` is straightforward (~10 lines) but not justified by current consumer usage.

**One capability declared out of scope**: `bindToLifecycle(Lifecycle)`. Android-specific (`androidx.lifecycle.Lifecycle`). KMPMobileApp does not call it (verified by grep across `KMPMobileApp/src/`). Lifecycle binding can be re-introduced by the application layer (UI calling `pause()`/`resume()` from its own lifecycle observers) without requiring it on the universal runtime.

**Alternatives considered**:
- *Add `getStatus`/`setNodeState`/`setNodeConfig` together for completeness.* Rejected — only `getStatus` has a real consumer today; the others would be dead code subject to bit-rot.
- *Move `bindToLifecycle` into a generated Android-specific extension.* Rejected — adds platform-conditional generation complexity for a behavior that no consumer currently uses.

**Source references**:
- `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineController.kt:36-152`
- `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ModuleController.kt:19-40`
- `CodeNodeIO-DemoProject/StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/controller/StopWatchController.kt:40-205` (reference shape of the thick Controller)
- `CodeNodeIO-DemoProject/KMPMobileApp/src/androidUnitTest/kotlin/io/codenode/mobileapp/StopWatchIntegrationTest.kt:369-378` (consumer's `getStatus()` call — the trigger for Decision 1)

---

## Decision 2: `{Module}Flow.kt` runtime files are entirely subsumed by `DynamicPipelineBuilder` — no replacement needed

**Rationale**: A line-by-line audit of `StopWatchFlow.kt` and `AddressesFlow.kt` shows four kinds of work, each fully covered by `DynamicPipelineBuilder` / `DynamicPipeline` (in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineBuilder.kt`):

| Work performed by `{Module}Flow.kt` | Where currently | Equivalent in dynamic pipeline |
|---|---|---|
| Node-runtime instantiation (`TimerEmitterCodeNode.createRuntime()` etc.) | `StopWatchFlow.kt:40-44` | `DynamicPipelineBuilder.build()` iterates the FlowGraph nodes and calls `def.createRuntime(node.name)` for each. Identical logic, applied generically. |
| Channel wiring (`timeIncrementer.inputChannel1 = timerEmitter.outputChannel1` etc.) | `StopWatchFlow.kt:75-80` | `DynamicPipeline.wireConnections()` uses index-based port matching + channel assignment. Handles multi-output and Source runtimes. Identical logic, with validation. |
| Topological start/stop (`start(scope)` launches in dependency order) | `StopWatchFlow.kt:49-54` | `DynamicPipeline.start()` handles topological order. Already subsumed. |
| State-object reset (`StopWatchState.reset()` invocation chain) | `StopWatchFlow.kt:reset()` | `DynamicPipelineController.reset()` (line 125-129) invokes the optional `onReset` callback (line 39) — the generated `{Module}Runtime.kt` factory passes `{Module}State::reset` here. Already subsumed. |

**No replacement file is needed for `{Module}Flow.kt`.** It can be deleted outright after each module is regenerated.

**Alternatives considered**:
- *Keep `{Module}Flow.kt` as a thin facade that delegates to `DynamicPipelineBuilder`.* Rejected — adds a per-module file with zero distinguishing content. The collapse goal is fewer files, not the same file count with different bodies.

**Source references**:
- `CodeNodeIO-DemoProject/StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/flow/StopWatchFlow.kt:1-82`
- `CodeNodeIO-DemoProject/Addresses/src/commonMain/kotlin/io/codenode/addresses/flow/AddressesFlow.kt:1-83`
- `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineBuilder.kt`

---

## Decision 3: `{Module}ControllerAdapter.kt` is pure delegation — replaced by an `object` expression inside `{Module}Runtime.kt`

**Rationale**: `AddressesControllerAdapter.kt` (52 lines) and `StopWatchControllerAdapter.kt` (49 lines) are 100% delegation: each property forwards to the wrapped `Controller`'s same-named property; each method forwards to the same-named method. Example, lines 24-50 of `AddressesControllerAdapter.kt`:

```kotlin
override val save: StateFlow<Any?> get() = controller.save
override val update: StateFlow<Any?> get() = controller.update
// ... (5 more properties)
override fun start(): FlowGraph = controller.start()
override fun stop(): FlowGraph = controller.stop()
// ... (3 more methods)
```

This is exactly what an anonymous `object` expression returning `{Module}ControllerInterface` accomplishes — no separate file required. The new `{Module}Runtime.kt`'s factory function instantiates a `DynamicPipelineController` and wraps it in such an `object` expression that delegates control methods to the controller and reads typed state flows from `{Module}State.{x}Flow` directly.

**Alternatives considered**:
- *Use Kotlin's class-level interface delegation (`class Foo : ControllerInterface, ModuleController by inner`).* Rejected — works for the `ModuleController` half but not for the typed state-flow getters which must come from `{Module}State`. Mixed strategy adds confusion; a single anonymous `object` is clearer.
- *Keep a separate generated `Adapter.kt`.* Rejected — adds a file solely to host code that fits inside the factory body (~20 lines).

**Source references**:
- `CodeNodeIO-DemoProject/Addresses/src/commonMain/kotlin/io/codenode/addresses/controller/AddressesControllerAdapter.kt:1-52`
- `CodeNodeIO-DemoProject/StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/controller/StopWatchControllerAdapter.kt:1-49`

---

## Decision 4: `{Module}ControllerInterface` extends `io.codenode.fbpdsl.runtime.ModuleController`; the GraphEditor reflection proxy continues to work without changes

**Rationale**: Today's per-module interface (e.g., `StopWatchControllerInterface`) declares its own `executionState`, `start/stop/pause/resume/reset`, but does NOT inherit from `ModuleController`. Production consumers therefore cannot reach `setAttenuationDelay`/`setEmissionObserver`/`setValueObserver`/`getStatus` through the typed interface — they have to cast to the concrete Controller class. After collapse, the concrete class disappears, so the only typed surface is the interface. The interface MUST expose the full `ModuleController` surface for production consumers to remain functional.

The cleanest change is to make `{Module}ControllerInterface : ModuleController`. This adds (by inheritance) `setAttenuationDelay`, `setEmissionObserver`, `setValueObserver`, `nodeDefinitionLookup` (existing), and (after Decision 1) `getStatus()`.

**The GraphEditor's reflection proxy** (`flowGraph-execute/src/jvmMain/.../ModuleSessionFactory.kt:108-150`) is unaffected. Audit:

- The proxy intercepts every method call by `method.name`.
- For names matching `start`/`stop`/`pause`/`resume`/`reset` → delegates to `DynamicPipelineController` ✓
- For `getExecutionState` → returns `controller.executionState` ✓
- For any other `getXxx` → resolves via reflection to `{Module}State.{x}Flow` field ✓
- For `setXxx` (the newly inherited setters from `ModuleController`) → falls into the `else null` branch (line 140). For methods returning `Unit` this is harmless — Kotlin's `Unit` ABI is `void`, so the proxy's null return is silently coerced. **This means calling `setAttenuationDelay(...)` through the proxy is a no-op**, but `ModuleSessionFactory` itself never calls these methods through the proxy — it calls them on the `DynamicPipelineController` instance directly (search `ModuleSessionFactory.kt` for `controller.setAttenuationDelay` etc.). So the no-op behavior is unreachable in practice.
- For `getStatus` (newly added) → also falls into the `else` branch. The current proxy resolution attempts `getXxx` field lookup on the State object: `propName = "status"`, looks for `statusFlow` field, fails, returns null. This means **`getStatus()` called through the proxy returns null** — which would NPE if anyone called it through the proxy in Runtime Preview. KMPMobileApp doesn't use Runtime Preview's proxy path, so this is theoretical for now. **Risk-mitigation step**: the plan adds an explicit case in the proxy handler for `getStatus` returning `controller.getStatus()`. This is a one-line addition to `ModuleSessionFactory.createControllerProxy`.

**Alternatives considered**:
- *Leave `{Module}ControllerInterface` independent of `ModuleController`; have production consumers cast to `DynamicPipelineController` to reach setters.* Rejected — leaks an implementation type into the consumer surface; defeats the typed-interface abstraction.
- *Add the setter signatures verbatim to `{Module}ControllerInterface` (not via inheritance).* Rejected — duplicative; if `ModuleController` evolves, every per-module interface drifts.

**Source references**:
- `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ModuleController.kt:19-40` (the surface that interfaces will inherit)
- `flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/ModuleSessionFactory.kt:108-150` (proxy handler — unchanged behaviorally except the optional one-line `getStatus` case)
- `CodeNodeIO-DemoProject/StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/controller/StopWatchControllerInterface.kt:21-39` (current shape — gains `: ModuleController` clause)

---

## Decision 5: One new generated per-module file `{Module}Runtime.kt` containing both the node registry and the factory function

**Rationale**: Production-app deployment has no filesystem scanning; `DynamicPipelineController` requires a `lookup: NodeDefinitionLookup` (a `(String) -> CodeNodeDefinition?` function). The cleanest way to satisfy this per module is a small generated file that hard-codes the lookup table.

The same file also hosts the factory function that:
1. Constructs a `DynamicPipelineController` with `flowGraphProvider`, `lookup`, and `onReset = {Module}State::reset`.
2. Wraps the controller in an anonymous `object` expression implementing `{Module}ControllerInterface`, delegating control to the controller and reading state flows from `{Module}State.{x}Flow`.

Reference template (StopWatch):

```kotlin
/*
 * StopWatchRuntime — Universal-runtime factory for the StopWatch module
 * Generated by CodeNodeIO ModuleRuntimeGenerator
 * License: Apache 2.0
 */

package io.codenode.stopwatch

import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.DynamicPipelineController
import io.codenode.stopwatch.controller.StopWatchControllerInterface
import io.codenode.stopwatch.nodes.DisplayReceiverCodeNode
import io.codenode.stopwatch.nodes.TimeIncrementerCodeNode
import io.codenode.stopwatch.nodes.TimerEmitterCodeNode
import io.codenode.stopwatch.viewmodel.StopWatchState

object StopWatchNodeRegistry {
    fun lookup(name: String): CodeNodeDefinition? = when (name) {
        "TimerEmitter"     -> TimerEmitterCodeNode
        "TimeIncrementer"  -> TimeIncrementerCodeNode
        "DisplayReceiver"  -> DisplayReceiverCodeNode
        else               -> null
    }
}

fun createStopWatchRuntime(flowGraph: FlowGraph): StopWatchControllerInterface {
    val controller = DynamicPipelineController(
        flowGraphProvider = { flowGraph },
        lookup = StopWatchNodeRegistry::lookup,
        onReset = StopWatchState::reset
    )
    return object : StopWatchControllerInterface, ModuleController by controller {
        override val elapsedSeconds = StopWatchState.elapsedSecondsFlow
        override val elapsedMinutes = StopWatchState.elapsedMinutesFlow
        override val seconds = StopWatchState.secondsFlow
        override val minutes = StopWatchState.minutesFlow
    }
}
```

Total size: ~30-35 lines. The `ModuleController by controller` interface delegation handles every inherited member (including the new `getStatus()`). Only the typed state-flow getters require explicit `override`s (the proxy-style runtime cannot use Kotlin delegation for these because they come from `{Module}State`, not from `controller`).

**Alternatives considered**:
- *Two files: one for the registry, one for the factory.* Rejected — splitting ~30 lines across two files for purely organizational reasons; both halves are tightly coupled (the factory references the registry).
- *Make the factory a `class` (e.g., `StopWatch(flowGraph)`).* Rejected — `StopWatch` collides with the existing top-level Composable function name in `userInterface/StopWatch.kt`. The top-level factory function `createStopWatchRuntime` avoids the collision.
- *Place the file inside `controller/` subpackage alongside the interface.* Rejected — the file isn't a controller; it's a module-level entry point. Top-level placement (`io.codenode.stopwatch`) signals "use this from outside the module."

**Source references**:
- `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineController.kt:36-49` (constructor signature for the factory)
- `CodeNodeIO-DemoProject/WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/viewmodel/WeatherForecastViewModel.kt` (precedent for reading `State.{x}Flow` directly)

---

## Decision 6: WeatherForecast already follows the "no thick stack" pattern in Runtime Preview; this feature gives it the missing piece (Runtime.kt) for production deployability

**Rationale**: WeatherForecast (the newest module) does NOT have `WeatherForecastController.kt`, `WeatherForecastControllerAdapter.kt`, or a `WeatherForecastFlow.kt` runtime file. It DOES have a `WeatherForecast.flow.kt` (the user-authored DSL), `WeatherForecastControllerInterface.kt`, `WeatherForecastViewModel.kt`, `WeatherForecastState.kt`, nodes, and a `WeatherForecastPreviewProvider.kt`. It works in Runtime Preview today via `ModuleSessionFactory`'s dynamic-pipeline + reflection-proxy path.

What WeatherForecast lacks for **production deployability** is exactly the new `{Module}Runtime.kt` file — without a way to construct `DynamicPipelineController` outside the GraphEditor (where there's no filesystem scanning), production code cannot use WeatherForecast as a library.

So the collapse adds `WeatherForecastRuntime.kt` to WeatherForecast (no deletion of thick files needed there) and modifies its `WeatherForecastControllerInterface` to extend `ModuleController`. This is the smallest possible per-module change in the rollout and serves as a forward-looking validation: the new shape is what every module looks like after the collapse.

**Alternatives considered**:
- *Skip WeatherForecast (no thick files to remove).* Rejected — leaves WeatherForecast in an inconsistent state vs the other four modules. Spec FR-008 explicitly requires regenerating all five.

**Source references**:
- `CodeNodeIO-DemoProject/WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/` (full module structure)
- `CodeNodeIO-DemoProject/WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/controller/WeatherForecastControllerInterface.kt:21-34` (current minimal shape — gains state-flow getters via regeneration AND `: ModuleController` clause)

---

## Decision 7: KMPMobileApp's instantiation sites change mechanically; test logic is preserved

**Rationale**: KMPMobileApp consumes the per-module Controllers in two places:

1. `KMPMobileApp/src/commonMain/.../mobileapp/App.kt:56-72` — production runtime instantiation:
   ```kotlin
   val stopWatchController = remember {
       StopWatchController(stopWatchFlowGraph).also { it.setAttenuationDelay(1000) }
   }
   val stopWatchViewModel = remember { StopWatchViewModel(StopWatchControllerAdapter(stopWatchController)) }
   ```
   becomes:
   ```kotlin
   val stopWatchController = remember {
       createStopWatchRuntime(stopWatchFlowGraph).also { it.setAttenuationDelay(1000) }
   }
   val stopWatchViewModel = remember { StopWatchViewModel(stopWatchController) }
   ```

2. `KMPMobileApp/src/androidUnitTest/.../StopWatchIntegrationTest.kt:121-203` — six test methods instantiate `StopWatchController(flowGraph)` directly. Each becomes `createStopWatchRuntime(flowGraph)`. The test assertions (executionState transitions, getStatus return shape at line 375, etc.) are unchanged — all still hold against the universal runtime.

The `import` lines change from `io.codenode.stopwatch.controller.StopWatchController` and `io.codenode.stopwatch.controller.StopWatchControllerAdapter` to `io.codenode.stopwatch.createStopWatchRuntime` (and similar for UserProfiles).

The `setAttenuationDelay(1000)` call, previously available because the concrete `Controller` implemented `ModuleController`, is now available via `{Module}ControllerInterface` (which inherits from `ModuleController` per Decision 4). No cast required.

**Alternatives considered**:
- *Provide a deprecation shim (`StopWatchController(graph)` stays as a top-level function alias).* Rejected — spec FR-015 explicitly accepts the import-surface break since the only consumer is in-tree. Shims add maintenance cost without consumer benefit.
- *Migrate KMPMobileApp in a separate change set.* Rejected — spec FR-014 requires atomic landing to avoid a broken main-branch state.

**Source references**:
- `CodeNodeIO-DemoProject/KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/App.kt:23-72` (call sites + import block)
- `CodeNodeIO-DemoProject/KMPMobileApp/src/androidUnitTest/kotlin/io/codenode/mobileapp/StopWatchIntegrationTest.kt:121-378` (test instantiation sites + getStatus assertion)

---

## Decision 8: Generator deletion is a clean cut, not a soft deprecation

**Rationale**: The three generators (`RuntimeControllerGenerator` ~367 lines, `RuntimeControllerAdapterGenerator` ~114 lines, `RuntimeFlowGenerator` ~423 lines = ~900 lines total) are all in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/`. They are invoked by module orchestration (likely `ModuleGenerator.kt` ~50KB; will be confirmed in tasks). Their unit tests are co-located in `commonTest/`.

After the collapse, no live code path invokes them. Per spec FR-011, they are deleted outright (along with their unit tests) rather than left as `@Deprecated`. The orchestrator (`ModuleGenerator.kt`) is updated in the same change set to invoke the new `ModuleRuntimeGenerator` and to skip the deleted three. This satisfies FR-012.

The `RuntimeControllerInterfaceGenerator` (~111 lines) is **kept** but modified: emit `interface X : io.codenode.fbpdsl.runtime.ModuleController { ... }` instead of `interface X { ... }`, and add the import. This is the minimal change required by Decision 4.

**Alternatives considered**:
- *Mark the three generators `@Deprecated(level = WARNING)` for one release before deletion.* Rejected — there are no out-of-tree consumers of generator classes (all generation is invoked by `flowGraph-generate` itself); deprecation provides no migration window for anyone.
- *Keep test fixtures of the deleted generators as historical references.* Rejected unless explicitly justified — leaves dead code in `commonTest/` that future contributors must classify.

**Source references**:
- `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeControllerGenerator.kt` (367 lines)
- `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeControllerAdapterGenerator.kt` (114 lines)
- `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeFlowGenerator.kt` (423 lines)
- `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeControllerInterfaceGenerator.kt` (111 lines — kept, modified)
- `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/ModuleGenerator.kt` (~1500 lines; orchestrator, modified to swap generators)

---

## Decision 9: Migration safety — generator must refuse to delete hand-edited files at target paths

**Rationale**: Each module's regeneration deletes three generator-target files (`Controller.kt`, `Adapter.kt`, `Flow.kt`). If a developer has manually edited those files (against project convention, but possible), the deletion would discard their work without warning.

The new `ModuleRuntimeGenerator` (and the orchestrator that schedules deletions) MUST inspect each target file's first ~5 lines for the standard `Generated by CodeNodeIO` marker comment. If the marker is absent (or has been removed by hand), refuse to delete and emit a structured warning naming the file. This satisfies FR-013.

For the five reference modules in the DemoProject, none of the target files have been hand-edited (verified by reading the headers — all carry the `Generated by` marker). The safety check is for future protection, not historical concern.

**Alternatives considered**:
- *Trust that no one hand-edits generated files; delete unconditionally.* Rejected — silent destruction of user work violates Constitution principle V (Observability & Debugging — "User-facing errors MUST be actionable").
- *Require the user to delete the files manually before re-running generation.* Rejected — defeats the goal of one-shot regeneration.

---

## Summary: Closing-the-Gap Checklist

| Item | Today | Target | Owner |
|---|---|---|---|
| `getStatus(): FlowExecutionStatus` on universal runtime | Missing on `DynamicPipelineController` and `ModuleController` | Added to both; implementation wraps `RootControlNode.getStatus()` | `fbpDsl` (modify `DynamicPipelineController.kt`, `DynamicPipeline.kt`, `ModuleController.kt`) |
| `setNodeState`/`setNodeConfig` on universal runtime | Missing | Deferred (no consumer) | — |
| `bindToLifecycle(Lifecycle)` | Per-module-Controller method, Android-specific | Out of scope; not used by KMPMobileApp | — |
| `{Module}ControllerInterface` extends `ModuleController` | Independent | Added superinterface clause | `RuntimeControllerInterfaceGenerator` (modify) |
| GraphEditor proxy handles `getStatus` | Returns null (NPE risk) | Adds explicit `getStatus` case returning `controller.getStatus()` | `ModuleSessionFactory.createControllerProxy` (one-line modify) |
| `{Module}Controller.kt` | Generated, ~200 lines/module | Deleted | `RuntimeControllerGenerator` (delete) |
| `{Module}ControllerAdapter.kt` | Generated, ~50 lines/module | Deleted; replaced by `object` expression inside `{Module}Runtime.kt` | `RuntimeControllerAdapterGenerator` (delete) |
| `{Module}Flow.kt` (runtime) | Generated, ~80-140 lines/module | Deleted; `DynamicPipelineBuilder` subsumes | `RuntimeFlowGenerator` (delete) |
| `{Module}Runtime.kt` (registry + factory) | Doesn't exist | Generated, ~30-35 lines/module | `ModuleRuntimeGenerator` (NEW) |
| KMPMobileApp imports + instantiations | `StopWatchController(graph)` + `StopWatchControllerAdapter(controller)` | `createStopWatchRuntime(graph)` (no Adapter wrap) | `App.kt` + `StopWatchIntegrationTest.kt` (manual edit, atomic with regeneration) |
| Five reference modules regenerated | Five modules with thick stack (or no Runtime.kt for WeatherForecast) | Five modules with `{Module}Runtime.kt` and no thick files | `ModuleGenerator` orchestrator (re-run on each module) |
| Generator unit tests for the deleted three | Live | Deleted | `commonTest/` (delete with the generators) |
| Hand-edit-safety check | Doesn't exist | Generator refuses to delete target files lacking the `Generated by` marker | `ModuleRuntimeGenerator` orchestration |

**No NEEDS CLARIFICATION items remain.** All technical-context fields in the plan are populated from inspected source. The design defaults documented in spec.md's Assumptions (keep `{Module}ControllerInterface`; break KMPMobileApp's import surface; atomic landing) are honored throughout.
