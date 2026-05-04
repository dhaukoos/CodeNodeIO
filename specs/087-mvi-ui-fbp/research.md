# Phase 0 Research: MVI Pattern for UI-FBP Interface Generation

## Context

The MVI shape is well-established in the Android/KMP world (Reaktive's MVI,
Decompose's Store, Orbit, Ballast). The spec already locked the high-level
shape — immutable State data class, sealed Event interface, ViewModel with
`state: StateFlow<S>` + `onEvent(E)`. The research below resolves the
generator-side design decisions that the spec deliberately stayed
implementation-agnostic about.

---

## Decision 1 — ViewModel scope: how does the ViewModel's `state` get updated?

**Question**: The current ViewModel reads `State.xxxFlow` directly because the
Sink CodeNode mutates `State._xxx.value` as a side-effect. With State now
immutable (`data class`), the ViewModel needs a different update path.

**Decision**: The generated ViewModel holds a `private val _state =
MutableStateFlow({Name}State())` and exposes `val state: StateFlow<{Name}State> =
_state.asStateFlow()`. In its `init { … }` block, the ViewModel launches one
collector per controller-exposed sink port (the `controller.{port}: StateFlow<T>`
members from feature 084's ControllerInterface) inside `viewModelScope`,
folding each emission into a new state snapshot via
`_state.update { it.copy({port} = value) }`.

**Rationale**:
- Preserves the existing `ControllerInterface` contract (FR-007). The
  controller still exposes per-port `StateFlow`s; the ViewModel becomes the
  fold point that produces a single composite snapshot.
- `viewModelScope` is the canonical lifecycle in `androidx.lifecycle`; it's
  already used elsewhere in the codebase (entity-module ViewModels) and is
  KMP-supported by `lifecycle-viewmodel-compose 2.8.0`.
- `MutableStateFlow.update { copy(...) }` is the idiomatic, race-free update
  primitive — readers always see consistent snapshots.
- One collector per port keeps the generated body trivial to read; the
  alternative (combine + map all ports into a single State) costs more in
  generated complexity for no observable behavior change.

**Alternatives considered**:
- *combine(...) over all ports → State*: produces fewer recompositions per
  burst but generates a tall, hard-to-read body (one combine arity per port
  count, capped at 5 in stdlib). Rejected on readability.
- *Lazy state derivation* (compute State on each read): requires re-snapshotting
  the controller's per-port flows; defeats the purpose of having a single
  StateFlow<State>. Rejected.

---

## Decision 2 — Event dispatch: how does `onEvent` route to source-port emissions?

**(Revised 2026-05-03 per /speckit.clarify Q1: Design B chosen.)**

**Question**: The current ViewModel exposes `fun emit(numA: Double, numB: Double)`
that writes to `{Name}State._numA.value` etc. (the singleton's mutable
backing). With public `{Name}State` now an immutable data class, the
generated `onEvent({Name}Event)` body must drive the FBP source-port
emission some other way.

**Decision (Design B)**: **Eliminate the singleton entirely.** Move all
per-port flows from the singleton into the `DynamicPipelineController`
instance (instance-per-flow-graph, NOT a singleton). The controller becomes
the single owner of cross-component state. Specifically:

- **Sink direction**: The `{Name}Runtime` factory creates per-sink-port
  `MutableStateFlow<T>` instances, captures them in a closure passed to
  `DynamicPipelineController` (or to the constructed object that wraps it),
  and exposes them as the `{Name}ControllerInterface`'s public
  `val <sinkPort>: StateFlow<T>` members (unchanged signature). The
  `{Name}SinkCodeNode` is given a "sink reporter" callback at runtime
  construction — see Decision 8.
- **Source direction**: The controller wraps per-source-port
  `MutableSharedFlow<T>` (replay = 1, default behavior of conflated
  channel). The `{Name}ControllerInterface` gains additive
  `fun emit<SourcePort>(value: T)` methods per source-output port. The
  `{Name}SourceCodeNode` collects from these flows (instead of from a
  singleton) and emits to its FBP output port.
- **ViewModel `onEvent` body**: dispatches `when (event)` over every Event
  case, calling `controller.emit<SourcePort>(event.value)` (or
  `controller.emit<SourcePort>()` for `Unit`-typed `data object` events).
- **No `{Name}StateStore`, no `{Name}State` singleton.** The State data
  class is the genuine SSOT for the UI; per-flow-graph controller state is
  the genuine SSOT for the runtime.

**Rationale**:
- **Real SSOT**: `_state` on the ViewModel is authoritative for the UI; the
  controller's `MutableStateFlow`s + `MutableSharedFlow`s are authoritative
  for the runtime. No two-truths drift.
- **Real unidirectional flow**: every state change on the UI side flows
  through the reducer (`_state.update { copy(...) }`); every source-port
  emission flows through `controller.emit<Port>(...)`. No shared mutable
  globals.
- **Real test isolation**: a fresh `DynamicPipelineController` instance =
  fresh state. Two tests constructing two `DemoUIViewModel`s do not share
  state. Multiple instances of the same flow graph in one JVM are
  independent.
- **FR-007 softened, not broken**: the `ControllerInterface` keeps every
  method it exposed before (`start / stop / pause / resume / reset /
  getStatus / executionState` + per-sink-port `StateFlow<T>`); it adds
  `emit<SourcePort>` methods. Existing consumers (Runtime Preview panel,
  `ModuleSessionFactory`) don't call emit methods so they're unaffected.

**Cost**:
- Larger blast radius: `UIFBPSourceCodeNodeGenerator`,
  `UIFBPSinkCodeNodeGenerator`, and the inline ControllerInterface +
  Runtime emission inside `UIFBPInterfaceGenerator` all change shape (in
  addition to the State / Event / ViewModel trio).
- New runtime affordance: the Sink CodeNode needs a way to receive a
  controller-injected callback at construction time — see Decision 8.

**Alternatives considered**:
- *Option A — keep singleton, rename `{Name}StateStore`, mark internal*:
  smaller blast radius but delivers MVI surface only, not MVI semantics
  (two sources of truth; module-scoped singleton breaks isolation;
  source-port path bypasses any reducer). Rejected at clarify time as it
  invites the same redesign discussion the next time someone reaches for
  true MVI.
- *Option C — hybrid: keep singleton for sink only; eliminate for source
  only*: half the blast radius of B, half the architectural payoff.
  Rejected — the sink-side dual-truth + isolation issues remain.
- *Re-use `viewModel.emit(...)` inside `onEvent`*: rejected per FR-011.

---

## Decision 3 — Empty Event / empty State edge cases

**Question**: How does the generator emit when the spec has zero
`sourceOutputs` (no events) or zero `sinkInputs` (no observable state)?

**Decision**:
- *Zero events*: emit `sealed interface {Name}Event` with no cases.
  ViewModel's `onEvent` body becomes a `when (event) {}` that the compiler
  proves exhaustive (no cases means no branches). The Screen's
  `(Event) -> Unit` parameter remains; callers cannot construct an Event so
  the callback is never invoked. This is the cheapest uniform shape (FR-008).
- *Zero state*: emit `data class {Name}State()` (zero-arg). The ViewModel's
  `_state` initializes to `{Name}State()` and never updates. The Screen's
  `state` parameter remains for symmetry (FR-009).

**Rationale**: Both cases preserve the canonical signature without callers
needing to special-case them. A homogenous shape is more important than
saving a handful of LOC on degenerate modules.

**Alternatives considered**:
- *Skip emitting Event.kt when empty*: makes the Screen's signature
  non-uniform across modules. Rejected.
- *Use `Nothing` as the Event type*: clever but obscure; readers would have
  to know that `(Nothing) -> Unit` is uncallable. Rejected on readability.

---

## Decision 4 — Determinism (SC-005)

**Question**: How do we guarantee byte-identical regeneration for an
unchanged spec?

**Decision**: Generators iterate over `spec.sinkInputs` and
`spec.sourceOutputs` in the order they appear in the spec (already a
deterministic `List`). No `Map`-based intermediates. No timestamps. No
`System.currentTimeMillis` in the file header (the existing generators
already follow this convention; preserved). The header license comment is a
fixed string; no per-run interpolation.

**Rationale**: Existing generators already meet this bar (verified by
inspection of `UIFBPStateGenerator.kt`). The new `UIFBPEventGenerator`
follows the same conventions.

**Alternatives considered**: None — determinism is non-negotiable per SC-005.

---

## Decision 5 — Test strategy (Constitution §II)

**Question**: Where do the new generator tests live? Fixture-based or
assertion-based?

**Decision**: Fixture-based byte-comparison in `commonTest`. Each generator
test case writes a known-good golden output to a string constant (or a
resource file) and asserts the generator's output equals it. Matches the
existing `UIFBPGeneratorTest` style.

**Rationale**:
- Fixture tests are the strongest signal for "the contract didn't drift" —
  any change breaks them loudly.
- Goldens are easy to regenerate locally (rerun the generator, paste output,
  rerun test).
- Existing `UIFBPGeneratorTest` already uses this pattern for the State and
  ViewModel generators; consistency.

**Alternatives considered**:
- *Pure structural assertions (`assertTrue(output.contains(...))`)*: brittle
  in a different way — easy to write tests that pass on broken output.
  Rejected.

---

## Decision 6 — DemoUI migration scope

**Question**: What exactly does "DemoUI" map to on disk?

**Investigation**: `find` over the demo project shows DemoUI is the
**flow-graph prefix** for a UI-FBP module that lives **inside the TestModule
package** (`io.codenode.testmodule`). The generated artifacts are at:

- `TestModule/src/commonMain/kotlin/io/codenode/testmodule/viewmodel/DemoUIState.kt`
- `TestModule/src/commonMain/kotlin/io/codenode/testmodule/viewmodel/DemoUIViewModel.kt`
- `TestModule/src/commonMain/kotlin/io/codenode/testmodule/nodes/DemoUI{Source,Sink}CodeNode.kt`
- `TestModule/src/commonMain/kotlin/io/codenode/testmodule/controller/DemoUI{ControllerInterface,Runtime}.kt`
- `TestModule/src/commonMain/kotlin/io/codenode/testmodule/userInterface/DemoUI.kt` (hand-written Screen)
- `TestModule/src/jvmMain/kotlin/io/codenode/testmodule/userInterface/DemoUIPreviewProvider.kt`

The user has already begun MVI-direction sketches in:
- `TestModule/.../viewmodel/DemoUIAction.kt` — placeholder sealed interface
  (the user typed "Action" rather than "Event"; we'll standardize on "Event"
  per the spec to match the user's larger MviScreen example)
- `TestModule/.../viewmodel/DemoUIStateMVI.kt` — placeholder data class

Both placeholders will be replaced by the regenerated `DemoUIState.kt` (data
class) and `DemoUIEvent.kt` (sealed interface). The transitional `*MVI.kt`
and `*Action.kt` files get deleted as part of P2 cleanup.

**Decision**: P2 migration target is the DemoUI flow graph inside
TestModule. P3 module candidates (per `find` results): WeatherForecast,
EdgeArtFilter, UserProfiles, Addresses, StopWatch.

---

## Decision 7 — Generator output for the new `DemoUIEvent.kt` file location

**Question**: Where in the host module's tree does the new `{Name}Event.kt`
live?

**Decision**: Same package as `{Name}State.kt` —
`{basePackage}.viewmodel.{Name}Event`. Mirrors the State location convention
established by feature 085, and ensures `ModuleSessionFactory`'s preferred
FQCN lookup path for ViewModel-adjacent types stays consistent.

**Rationale**: The Event is co-equal to the State (both are pure data types
the ViewModel exposes). Co-location simplifies imports (the ViewModel's
package is also `viewmodel`, so no extra import needed inside the generated
ViewModel body).

**Alternatives considered**:
- *Separate `{basePackage}.event` package*: cleaner namespace but breaks
  established convention; rejected for consistency.

---

## Decision 8 — Sink CodeNode: how does it deliver values to the controller?

**(Added 2026-05-03 alongside Decision 2's Design B revision.)**

**Question**: Today `{Name}SinkCodeNode` writes received IPs to
`{Name}State._<port>.value` (singleton). Under Design B the singleton is
gone. The Sink CodeNode is itself a Kotlin `object` (the
`CodeNodeDefinition` singleton — required by the FBP runtime's discovery
model), so it has no per-instance hook. How does it deliver values to the
correct per-flow-graph controller's `MutableStateFlow`?

**Decision**: The `{Name}SinkCodeNode` provides its NodeRuntime via
`createRuntime(name)` (existing API). The runtime returned is an
`In<N>SinkRuntime<...>` (or equivalent) whose `tick(...)` body writes the
received value into a **callback supplied by the controller**. The Runtime
factory `create{Name}Runtime(flowGraph)` constructs the controller and
**parameterizes the SinkCodeNode's runtime** with a "sink reporter":

```kotlin
fun create{Name}Runtime(flowGraph: FlowGraph): {Name}ControllerInterface {
    val _resultsFlow = MutableStateFlow<CalculationResults?>(null)
    // … one MutableStateFlow per sinkInput
    val _numAFlow = MutableSharedFlow<Double>(replay = 1, extraBufferCapacity = 64)
    // … one MutableSharedFlow per sourceOutput

    val controller = DynamicPipelineController(
        flowGraphProvider = { flowGraph },
        lookup = { name ->
            when (name) {
                "{Name}Sink" -> {Name}SinkCodeNode.withReporter { results ->
                    _resultsFlow.value = results
                }
                "{Name}Source" -> {Name}SourceCodeNode.withSource(_numAFlow, /* … */)
                else -> null
            }
        },
        onReset = {
            _resultsFlow.value = null
            // … reset every flow to its default
        }
    )
    return object : {Name}ControllerInterface, ModuleController by controller {
        override val results: StateFlow<CalculationResults?> = _resultsFlow.asStateFlow()
        override fun emitNumA(value: Double) {
            controller.scope.launch { _numAFlow.emit(value) }
        }
        // … one override per sink port + one emit per source port
    }
}
```

The `withReporter(...)` and `withSource(...)` extension methods on the
SinkCodeNode / SourceCodeNode definitions return a wrapper
`CodeNodeDefinition` that, when its `createRuntime(...)` is called, returns
a runtime closured over the supplied callback / source flow. This pattern
keeps `CodeNodeDefinition` itself an `object` singleton (required by FBP
discovery) while allowing per-flow-graph state injection at runtime
construction time.

**Rationale**:
- **Per-flow-graph isolation**: each `create{Name}Runtime(flowGraph)` call
  produces fresh `MutableStateFlow` / `MutableSharedFlow` instances closured
  inside fresh `withReporter` / `withSource` wrappers. Two simultaneous
  instances of the same flow graph share nothing.
- **Stays inside the existing CodeNodeDefinition discovery model**: the
  `object` singleton is preserved; only its `createRuntime(...)` body
  changes shape. `NodeDefinitionRegistry` continues to find the SinkCodeNode
  the same way.
- **Aligns with Decision 1**: ViewModel collects from `controller.<sinkPort>`
  (controller-owned StateFlows) — same path it would have collected from
  under the singleton design, just with a different backing source.
- **No reflection, no service locators**: the wiring is plain Kotlin
  closures. Easy to read and test.

**Alternatives considered**:
- *Coroutine-context-injected callback (Kotlin contextual receivers)*: too
  experimental; not stable in 2.1.21. Rejected.
- *Service-locator pattern (a global `CallbackRegistry` keyed by flow-graph
  ID)*: a singleton with extra steps; doesn't fix isolation. Rejected.
- *Make `CodeNodeDefinition` non-singleton*: violates the FBP discovery
  contract; would cascade into the rest of the codebase. Rejected.

**Naming**: the wrapper extension is `withReporters(vararg reporters: (Any?) -> Unit)`
on Sink CodeNode definitions (one reporter per sinkInput, in declared
order) and `withSources(vararg sources: SharedFlow<*>)` on Source CodeNode
definitions (one flow per sourceOutput, in declared order). Both are
emitted inline into the per-module `{Name}SinkCodeNode.kt` /
`{Name}SourceCodeNode.kt` rather than living as shared helpers — see
contracts/source-sink-controller-runtime.md for the canonical shape.

---

## Resolved unknowns summary

| Item | Resolution |
|------|------------|
| ViewModel state-update mechanism | per-port collectors fold into MutableStateFlow via `update { copy(...) }` (Decision 1) |
| Event → source-port routing | **Design B** — singleton eliminated; ViewModel calls `controller.emit<Port>(value)`; ControllerInterface gains additive emit methods (Decision 2, revised) |
| Empty events / empty state | uniform empty hierarchies; no signature special-casing (Decision 3) |
| Byte-identical regeneration | preserved existing determinism conventions (Decision 4) |
| Test format | fixture-based byte-comparison in commonTest (Decision 5) |
| DemoUI = which on-disk module | DemoUI flow graph inside TestModule (Decision 6) |
| New Event.kt location | `{basePackage}.viewmodel.{Name}Event` (Decision 7) |
| Sink CodeNode → controller delivery | controller-injected reporter callback via `withReporter(...)` wrapper at runtime construction; per-flow-graph `MutableStateFlow` instances captured in the Runtime factory closure (Decision 8) |

No NEEDS CLARIFICATION items remain. Ready for Phase 1.
