# Contract: Universal Runtime ↔ Generated Module

This contract defines the runtime surface every collapsed module exposes and the obligations the universal runtime (`DynamicPipelineController` + `ModuleController` interface) MUST satisfy. After this feature ships, every module participates in this contract — there is no per-module variation in runtime shape.

## Sources of truth

- **Universal runtime**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineController.kt`, `DynamicPipeline.kt`, `ModuleController.kt`, `DynamicPipelineBuilder.kt`.
- **Per-module factory** (generated): `{moduleRoot}/src/commonMain/kotlin/{packagePath}/{Module}Runtime.kt`.
- **Per-module typed interface** (generated): `{moduleRoot}/src/commonMain/kotlin/{packagePath}/controller/{Module}ControllerInterface.kt`.
- **Runtime Preview consumer** (unchanged): `flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/ModuleSessionFactory.kt`.

## The two consumer paths

A collapsed module is consumed in exactly two ways. Both paths satisfy the same contract.

### Path A — GraphEditor Runtime Preview (in-process, reflection-driven)

`ModuleSessionFactory.createSession` constructs a `DynamicPipelineController` directly (bypassing the per-module factory function — there's no need to run it inside the GraphEditor) and a `java.lang.reflect.Proxy` implementing `{Module}ControllerInterface`. The proxy intercepts:

- Control method calls (`start`/`stop`/`pause`/`resume`/`reset`) → delegates to the dynamic controller.
- `getExecutionState()` → returns `controller.executionState`.
- `getStatus()` → returns `controller.getStatus()` (NEW; one-line proxy addition required).
- Setter methods inherited from `ModuleController` (`setAttenuationDelay`, `setEmissionObserver`, `setValueObserver`) → fall through to the `else` branch returning null (Unit-coerced; harmless because Runtime Preview calls these on the dynamic controller directly, not via the proxy).
- Any other `getXxx` (the per-port typed state flows) → reflectively reads `{Module}State.{x}Flow` field.

### Path B — Production-app deployment (KMPMobileApp and similar)

The consumer calls the generated factory function:

```kotlin
val runtime: {Module}ControllerInterface = create{Module}Runtime(flowGraph)
```

The factory constructs a `DynamicPipelineController` and wraps it in an anonymous `object` expression that implements `{Module}ControllerInterface` via `ModuleController by controller` (Kotlin interface delegation, handles every inherited member) plus explicit `override val xxx = {Module}State.xxxFlow` for each typed state-flow getter. The consumer holds the `runtime` reference and calls any method on `{Module}ControllerInterface` (typed flows, control methods, setters, `getStatus`) directly — no proxy involved.

## Obligations on the universal runtime (`DynamicPipelineController` + `ModuleController`)

The universal runtime MUST provide every member declared on `ModuleController`:

| Member | Behavior |
|---|---|
| `val executionState: StateFlow<ExecutionState>` | Reflects current pipeline state (IDLE → RUNNING → PAUSED ↔ RUNNING → IDLE). |
| `fun start(): FlowGraph` | Validates the FlowGraph against the lookup; builds and starts the dynamic pipeline; returns the FlowGraph. If validation fails, sets `validationError`, returns the FlowGraph in IDLE state. |
| `fun stop(): FlowGraph` | Stops the pipeline; cancels coroutines; transitions to IDLE; returns the FlowGraph. |
| `fun pause(): FlowGraph` | Pauses every node runtime; transitions to PAUSED; returns the FlowGraph. |
| `fun resume(): FlowGraph` | Resumes every node runtime; transitions to RUNNING; returns the FlowGraph. |
| `fun reset(): FlowGraph` | Stops, then invokes the optional `onReset` callback (the generated factory passes `{Module}State::reset`); returns the FlowGraph. |
| `fun setAttenuationDelay(ms: Long?)` | Sets the per-emission delay on every running node runtime. Persists across pipeline rebuilds. |
| `fun setEmissionObserver(observer)` | Installs a callback invoked on every emission. Persists across pipeline rebuilds. |
| `fun setValueObserver(observer)` | Installs a callback invoked on every value sent. Persists across pipeline rebuilds. |
| `fun getStatus(): FlowExecutionStatus` | **NEW.** Returns a snapshot of per-node execution state for the current pipeline. Returns IDLE + empty per-node map when no pipeline is running. Implementation wraps `RootControlNode.getStatus()` from the internally-held pipeline. |
| `var nodeDefinitionLookup: ((String) -> CodeNodeDefinition?)?` | Optional override of the lookup; used by `ModuleSessionFactory` for Runtime Preview. Production-app consumers do not set this (the lookup is closed over by the factory). |

## Obligations on each generated `{Module}Runtime.kt`

Each module MUST emit a file that satisfies:

1. **Lives at module package root** — `{moduleRoot}/src/commonMain/kotlin/{packagePath}/{ModuleName}Runtime.kt`. NOT in any subpackage.
2. **Contains exactly one `object {ModuleName}NodeRegistry`** with a `fun lookup(name: String): CodeNodeDefinition?` whose `when` covers every node referenced by the module's `.flow.kt` graph.
3. **Contains exactly one top-level factory function** `create{ModuleName}Runtime(flowGraph: FlowGraph): {ModuleName}ControllerInterface`. Body must:
   - Construct a `DynamicPipelineController` with `flowGraphProvider = { flowGraph }`, `lookup = {ModuleName}NodeRegistry::lookup`, `onReset = {ModuleName}State::reset`.
   - Return an anonymous `object` expression implementing `{ModuleName}ControllerInterface`, using `ModuleController by controller` for inherited members and explicit `override val xxx = {ModuleName}State.xxxFlow` for each per-port state-flow getter declared on the interface.
4. **Carries the standard `Generated by CodeNodeIO ModuleRuntimeGenerator` marker comment** in its header (for safety-check identification per FR-013).
5. **Imports only KMP-clean symbols** — no platform-specific (Android/iOS/JVM) imports.

## Obligations on each generated `{Module}ControllerInterface`

After the `RuntimeControllerInterfaceGenerator` modification:

1. **Lives at canonical FQCN** `io.codenode.{modulename}.controller.{Module}ControllerInterface`. (Unchanged from today; required by `ModuleSessionFactory:84-101` lookup contract.)
2. **Extends `io.codenode.fbpdsl.runtime.ModuleController`** — gains every member of `ModuleController` by inheritance.
3. **Declares only the per-port `val xxx: StateFlow<T>` typed state-flow getters** in its body — no redeclaration of inherited control methods or `executionState`.
4. **Carries the standard `Generated by CodeNodeIO RuntimeControllerInterfaceGenerator` marker comment**.

## Obligations on each generated `{Module}State`

Unchanged by this feature, but the universal runtime contract relies on the State object satisfying these (already true in all five reference modules):

| Requirement | Why |
|---|---|
| Be a Kotlin `object` (single `INSTANCE` field) | The factory function references it as a static singleton. |
| Live at `io.codenode.{modulename}.viewmodel.{Module}State` (preferred) or `io.codenode.{modulename}.{Module}State` (fallback for legacy modules like WeatherForecast which today lives at the root) | The factory function's import resolves it; Runtime Preview also reads it via reflection. |
| Expose `{x}Flow: StateFlow<T>` for every typed state-flow getter declared on the interface | The factory's `override val xxx = {Module}State.xxxFlow` references these directly. |
| Expose internal `_{x}: MutableStateFlow<T>` companions | Source/Sink CodeNode runtimes write to these. (Internal implementation detail — not part of this contract per se, but required for the runtime to function.) |
| Expose a `fun reset()` method | The factory's `onReset` callback references it. |

## Failure modes

| Failure | Symptom — Path A (Runtime Preview) | Symptom — Path B (production app) |
|---|---|---|
| `{Module}ControllerInterface` missing or at wrong FQCN | `ModuleSessionFactory.createSession` returns a session with `Any()` ViewModel; PreviewProvider's cast throws `ClassCastException`. | Compile-time error (the factory's return type is unresolved). |
| `{Module}NodeRegistry::lookup` returns null for a node referenced in the FlowGraph | Runtime Preview shows a validation error from `DynamicPipelineBuilder.validate()` and refuses to start. | Same — `start()` sets `validationError` and returns the FlowGraph in IDLE. |
| `{Module}State.{x}Flow` field missing for a typed state-flow getter | Runtime Preview's reflection proxy returns null; the consumer's `collectAsState()` throws NPE. | Compile-time error in the factory's `override val xxx = ...` line. |
| `{Module}State` missing `reset()` method | The `onReset` callback fails at compile time in the factory; `reset()` is a no-op for the State. | Same. |
| `getStatus()` called via the proxy without the `ModuleSessionFactory` proxy update from this feature | Returns null; consumer crashes. (Hence the proxy update in research Decision 4.) | N/A — production path doesn't use the proxy. |

Generators MUST emit code that satisfies every requirement above; the test plan (`tasks.md`) MUST include reflection-based tests for the Runtime Preview contract AND compile-time tests (build the regenerated module under JVM target) for the production-app contract.

## Versioning

This contract is internal to the project. Future evolution paths to be aware of:

- Adding a new method to `ModuleController` is non-breaking for production consumers (existing factory `object` expressions inherit the new method via Kotlin delegation as long as they use `ModuleController by controller`).
- Adding a new method to `ModuleController` that the GraphEditor proxy must intercept requires a corresponding update to `ModuleSessionFactory.createControllerProxy` (see the `getStatus` precedent in Decision 4 of research).
- Removing a method from `ModuleController` is breaking for production consumers. Avoid; deprecate first.
- Renaming the canonical `{Module}ControllerInterface` FQCN is breaking for both paths (Runtime Preview proxy lookup AND production-app imports). Avoid.
