# Contract: Source/Sink CodeNode + ControllerInterface + Runtime generators (Design B)

**Implements**: FR-003, FR-007 (additive), FR-011, FR-012, SC-001, SC-005,
plus Decision 2 (singleton elimination) and Decision 8 (controller-injected
sink/source wiring).

This contract covers four interrelated generator changes that must land
together. They cannot be split — partial application produces a non-compiling
host module.

## Files affected

| Generator | Output file | Status |
|-----------|-------------|--------|
| `UIFBPSinkCodeNodeGenerator` | `nodes/{Name}SinkCodeNode.kt` | MODIFIED (shape change) |
| `UIFBPSourceCodeNodeGenerator` | `nodes/{Name}SourceCodeNode.kt` | MODIFIED (shape change) |
| `UIFBPInterfaceGenerator.generateControllerInterface(...)` | `controller/{Name}ControllerInterface.kt` | MODIFIED (additive) |
| `UIFBPInterfaceGenerator.generateRuntimeFactory(...)` | `controller/{Name}Runtime.kt` | MODIFIED (shape change) |

## `{Name}SinkCodeNode.kt` shape

```kotlin
/* license header */
package {basePackage}.nodes

import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.fbpdsl.model.CodeNodeType
/* IP-type imports */

object {Name}SinkCodeNode : CodeNodeDefinition {
    override val name = "{Name}Sink"
    override val category = CodeNodeType.SINK
    override val description = ""
    override val inputPorts = listOf(
        PortSpec("{port[0].name}", {port[0].kotlinType}::class),
        // … one PortSpec per spec.sinkInputs
    )
    override val outputPorts = emptyList<PortSpec>()

    /** Default runtime — no-op reporter. Used outside of UI-FBP runtime context. */
    override fun createRuntime(name: String): NodeRuntime =
        createSinkRuntime(name) { /* no-op */ }

    /**
     * Returns a per-flow-graph CodeNodeDefinition wrapper that delivers
     * received IPs to [reporter]. Used by {Name}Runtime to route values into
     * the controller's per-port MutableStateFlow.
     */
    fun withReporter(reporter: (Any?) -> Unit): CodeNodeDefinition =
        object : CodeNodeDefinition by this {
            override fun createRuntime(name: String): NodeRuntime =
                createSinkRuntime(name, reporter)
        }

    private fun createSinkRuntime(name: String, reporter: (Any?) -> Unit): NodeRuntime {
        // builds an In<N>SinkRuntime whose tick body calls reporter(value)
        // for each input port, in declared order
    }
}
```

### Key behaviors

- `object {Name}SinkCodeNode` keeps its singleton-`object` identity required
  by `NodeDefinitionRegistry`.
- The default `createRuntime(name)` returns a runtime with a no-op reporter,
  so the SinkCodeNode is harmless when dragged onto a graph in the
  GraphEditor outside a UI-FBP runtime.
- `withReporter(reporter)` returns a delegated `CodeNodeDefinition` (via
  Kotlin interface delegation `by this`) whose only override is
  `createRuntime`. The delegation preserves identity for the FBP discovery
  model while injecting the per-flow-graph callback.
- Single-port sinks call `reporter(value)`; multi-port sinks call
  `reporter` once per port emission, but in practice UI-FBP sink graphs
  are typically single-port — see Decision 8 for the multi-port handling
  cut (separate reporter per port).

### Multi-port sinks

When `spec.sinkInputs.size > 1`, `withReporter` takes a list of reporters
(one per port, in declared order):

```kotlin
fun withReporters(vararg reporters: (Any?) -> Unit): CodeNodeDefinition = …
```

The Runtime factory passes one reporter per sinkInput; each routes to its
matching `MutableStateFlow`.

## `{Name}SourceCodeNode.kt` shape

```kotlin
/* license header */
package {basePackage}.nodes

import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.fbpdsl.model.CodeNodeType
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
/* IP-type imports */

object {Name}SourceCodeNode : CodeNodeDefinition {
    override val name = "{Name}Source"
    override val category = CodeNodeType.SOURCE
    override val description = ""
    override val inputPorts = emptyList<PortSpec>()
    override val outputPorts = listOf(
        PortSpec("{port[0].name}", {port[0].kotlinType}::class),
        // … one PortSpec per spec.sourceOutputs
    )

    /** Default runtime — never-emitting source. Used outside UI-FBP runtime context. */
    override fun createRuntime(name: String): NodeRuntime =
        createSourceRuntime(name, /* never-emit flows */)

    /**
     * Returns a per-flow-graph CodeNodeDefinition wrapper that emits values
     * from [sources] (one SharedFlow per output port, in declared order)
     * onto its FBP output channels.
     */
    fun withSources(vararg sources: SharedFlow<*>): CodeNodeDefinition =
        object : CodeNodeDefinition by this {
            override fun createRuntime(name: String): NodeRuntime =
                createSourceRuntime(name, *sources)
        }

    private fun createSourceRuntime(name: String, vararg sources: SharedFlow<*>): NodeRuntime {
        // builds an Out<N>SourceRuntime that launches one collector per source flow
        // and forwards each emission to the matching output channel.
    }
}
```

### Key behaviors

- Same singleton-`object` discipline as the SinkCodeNode.
- Default `createRuntime(name)` produces a never-emitting runtime — safe
  for palette drops outside a UI-FBP runtime context.
- `withSources(...)` returns a delegated `CodeNodeDefinition` that captures
  the per-flow-graph source flows.
- Single-port sources call `withSources(singleFlow)`; multi-port sources
  pass one flow per output port, in declared order.

## `{Name}ControllerInterface.kt` shape (additive)

```kotlin
/* license header */
package {basePackage}.controller

import io.codenode.fbpdsl.runtime.ModuleController
import kotlinx.coroutines.flow.StateFlow
/* IP-type imports */

interface {Name}ControllerInterface : ModuleController {
    /* unchanged: per-sink-port StateFlow members */
    val {sinkPort}: StateFlow<{kotlinType}>
    // … one per spec.sinkInputs

    /* NEW (Design B): per-source-port emit method */
    fun emit{SourcePortName}(value: {kotlinType})
    // … or, for Unit-typed source ports:
    fun emit{SourcePortName}()
    // … one per spec.sourceOutputs
}
```

### Naming

- Sink-side `val <sinkPort>`: unchanged from today (port name lowercased
  per the existing `UIFBPSpec` convention).
- Source-side `fun emit<SourcePort>`: `emit` + PortName PascalCased (first
  letter of port name uppercased; no underscore-splitting). Mirrors the
  Event case naming `Update<PortName>` for symmetry.

## `{Name}Runtime.kt` shape

```kotlin
/* license header */
package {basePackage}.controller

import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.DynamicPipelineController
import io.codenode.fbpdsl.runtime.ModuleController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import {basePackage}.nodes.{Name}SinkCodeNode  /* if sinkInputs not empty */
import {basePackage}.nodes.{Name}SourceCodeNode /* if sourceOutputs not empty */
/* IP-type imports */

object {Name}NodeRegistry {
    fun lookup(nodeName: String, /* injected wrappers */): CodeNodeDefinition? = when (nodeName) {
        "{Name}Source" -> /* sourceWrapper */
        "{Name}Sink"   -> /* sinkWrapper */
        else -> null
    }
}

fun create{Name}Runtime(flowGraph: FlowGraph): {Name}ControllerInterface {
    // Per-flow-graph state — fresh per call.
    val _<sinkPort> = MutableStateFlow<{T}>({default})
    // … one MutableStateFlow per sinkInput

    val _<sourcePort> = MutableSharedFlow<{T}>(replay = 1, extraBufferCapacity = 64)
    // … one MutableSharedFlow per sourceOutput

    val sinkWrapper = {Name}SinkCodeNode.withReporters(
        { value -> _<sinkPort[0]>.value = value as {T0} },
        { value -> _<sinkPort[1]>.value = value as {T1} },
        // …
    )
    val sourceWrapper = {Name}SourceCodeNode.withSources(_<sourcePort[0]>, _<sourcePort[1]>, /* … */)

    val controller = DynamicPipelineController(
        flowGraphProvider = { flowGraph },
        lookup = { name -> when (name) {
            "{Name}Source" -> sourceWrapper
            "{Name}Sink"   -> sinkWrapper
            else -> null
        }},
        onReset = {
            _<sinkPort>.value = {default}
            // … reset every sink flow
        }
    )

    return object : {Name}ControllerInterface, ModuleController by controller {
        override val <sinkPort>: StateFlow<{T}> = _<sinkPort>.asStateFlow()
        // … one override per sinkInput

        override fun emit<SourcePort>(value: {T}) {
            controller.coroutineScope.launch { _<sourcePort>.emit(value) }
        }
        // … one override per sourceOutput
    }
}
```

### Key behaviors

- Factory signature `create{Name}Runtime(flowGraph: FlowGraph): {Name}ControllerInterface`
  is **unchanged** — `ModuleSessionFactory` and `RuntimePreviewPanel`
  continue to call it the same way.
- All per-flow-graph state lives in the closure; multiple calls produce
  fully isolated controllers.
- Source emits use `controller.coroutineScope.launch { … }` so the caller
  (the ViewModel's `onEvent`) doesn't block on the emit.
- The `sinkWrapper` / `sourceWrapper` `CodeNodeDefinition`s are passed to
  `DynamicPipelineController.lookup` so the FBP runtime resolves the
  per-flow-graph wired versions, not the bare `{Name}*CodeNode` singletons.
- The local `{Name}NodeRegistry.lookup` helper is preserved for production-app
  consumers (calling `create{Name}Runtime` outside the GraphEditor); but in
  the generated body it's no longer the source of truth — the inline
  `lookup` lambda above is. (The helper can be removed if no consumer
  references it; phase 2 will identify.)
- `controller.coroutineScope` must be exposed on `DynamicPipelineController`
  (today it's accessible internally; we'll need to verify it's exposed or
  add a public accessor — flagged as a tasks.md prerequisite).

### Edge cases

- **Empty `sourceOutputs`**: omit the source wrapper entirely; the runtime
  doesn't emit any source flows; ControllerInterface gains no `emit*`
  methods.
- **Empty `sinkInputs`**: omit the sink wrapper entirely; the
  ControllerInterface has no per-sink-port StateFlows; ViewModel's `init`
  block is empty.

## Test contract (RED phase)

Tests live in
`flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/`.

### `UIFBPSinkCodeNodeGeneratorTest.kt` — required cases

1. `generate emits object {Name}SinkCodeNode with default no-op createRuntime`
2. `generate emits withReporters(vararg) extension method`
3. `withReporters wrapper's createRuntime delegates to generated sink runtime with reporters captured`
4. `generate handles single-port sink correctly`
5. `generate handles multi-port sink correctly (multiple reporters in declared order)`
6. `generate output is byte-identical across two calls`
7. `generate does NOT reference any State or StateStore singleton`

### `UIFBPSourceCodeNodeGeneratorTest.kt` — required cases

1. `generate emits object {Name}SourceCodeNode with default never-emitting createRuntime`
2. `generate emits withSources(vararg SharedFlow<*>) extension method`
3. `withSources wrapper's createRuntime launches collectors per source flow`
4. `generate handles single-port source correctly`
5. `generate handles multi-port source correctly`
6. `generate output is byte-identical across two calls`
7. `generate does NOT reference any State or StateStore singleton`

### `UIFBPControllerInterfaceTest.kt` — required cases

1. `interface preserves all per-sink-port StateFlow<T> members from feature 084 baseline`
2. `interface adds one emit{PortName}(value: T) method per sourceOutput`
3. `interface adds emit{PortName}() (no-arg) for Unit-typed source ports`
4. `interface emits no emit methods when sourceOutputs is empty`
5. `interface compiles against ModuleController parent`
6. `output is byte-identical across two calls`

### `UIFBPRuntimeFactoryTest.kt` — required cases

1. `factory signature is create{Name}Runtime(flowGraph: FlowGraph): {Name}ControllerInterface (unchanged)`
2. `factory body declares one MutableStateFlow per sinkInput`
3. `factory body declares one MutableSharedFlow per sourceOutput`
4. `factory body wires sinkWrapper via withReporters with one reporter per sinkInput`
5. `factory body wires sourceWrapper via withSources with one flow per sourceOutput`
6. `factory body's returned anonymous object overrides one StateFlow per sinkInput + one emit per sourceOutput`
7. `factory body uses controller.coroutineScope.launch for source emits`
8. `factory body's onReset resets every sink MutableStateFlow to its declared default`
9. `factory body does NOT reference any singleton State or StateStore`
10. `output is byte-identical across two calls`

Each test uses fixture-string comparison.
