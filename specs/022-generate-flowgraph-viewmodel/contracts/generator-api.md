# Generator API Contract: ViewModel Layer

**Feature**: 022-generate-flowgraph-viewmodel
**Date**: 2026-02-19

## ModuleGenerator API Extensions

### `generateControllerInterfaceClass(flowGraph: FlowGraph, packageName: String): String`

Generates a Kotlin interface defining observable state and lifecycle control contract for a flow.

**Input**:
- `flowGraph`: FlowGraph with SINK CodeNodes whose input ports define observable state
- `packageName`: Target package (e.g., `io.codenode.stopwatch.generated`)

**Algorithm**:
1. Collect sink nodes: `flowGraph.getAllCodeNodes().filter { it.codeNodeType == CodeNodeType.SINK }`
2. Collect input ports from all sink nodes → `List<SinkPortProperty>`
3. If multiple sink nodes contribute, prefix property names with node name (camelCase)
4. Generate interface with StateFlow properties + lifecycle methods

**Output**: Kotlin source string containing:
- Package declaration
- Required imports (StateFlow, ExecutionState, FlowGraph)
- Interface declaration `{Name}ControllerInterface`
- One `val {portName}: StateFlow<{portDataType}>` per sink input port
- `val executionState: StateFlow<ExecutionState>` (always included)
- 5 lifecycle methods: `fun start(): FlowGraph`, `fun stop(): FlowGraph`, `fun reset(): FlowGraph`, `fun pause(): FlowGraph`, `fun resume(): FlowGraph`

**Example output** (StopWatch - single sink with ports `seconds: Int`, `minutes: Int`):
```kotlin
package io.codenode.stopwatch.generated

import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowGraph
import kotlinx.coroutines.flow.StateFlow

interface StopWatchControllerInterface {
    val seconds: StateFlow<Int>
    val minutes: StateFlow<Int>
    val executionState: StateFlow<ExecutionState>
    fun start(): FlowGraph
    fun stop(): FlowGraph
    fun reset(): FlowGraph
    fun pause(): FlowGraph
    fun resume(): FlowGraph
}
```

---

### `generateControllerAdapterClass(flowGraph: FlowGraph, packageName: String): String`

Generates a Kotlin class implementing ControllerInterface by delegating to the generated Controller.

**Input**: Same as above.

**Output**: Kotlin source string containing:
- Package declaration
- Required imports
- Class `{Name}ControllerAdapter(private val controller: {Name}Controller) : {Name}ControllerInterface`
- `override val` for each StateFlow property delegating to `controller.{name}`
- `override fun` for each lifecycle method delegating to `controller.{name}()`

**Example output** (StopWatch):
```kotlin
package io.codenode.stopwatch.generated

import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowGraph
import kotlinx.coroutines.flow.StateFlow

class StopWatchControllerAdapter(
    private val controller: StopWatchController
) : StopWatchControllerInterface {
    override val seconds: StateFlow<Int>
        get() = controller.seconds
    override val minutes: StateFlow<Int>
        get() = controller.minutes
    override val executionState: StateFlow<ExecutionState>
        get() = controller.executionState
    override fun start(): FlowGraph = controller.start()
    override fun stop(): FlowGraph = controller.stop()
    override fun reset(): FlowGraph = controller.reset()
    override fun pause(): FlowGraph = controller.pause()
    override fun resume(): FlowGraph = controller.resume()
}
```

---

### `generateViewModelClass(flowGraph: FlowGraph, packageName: String): String`

Generates a Kotlin ViewModel class bridging FlowGraph domain logic with Compose UI.

**Input**: Same as above.

**Output**: Kotlin source string containing:
- Package declaration
- Required imports (ViewModel, StateFlow, ExecutionState, FlowGraph, ControllerInterface)
- Class `{Name}ViewModel(private val controller: {Name}ControllerInterface) : ViewModel()`
- `val` for each StateFlow property assigned from `controller.{name}`
- `fun` for each lifecycle method delegating to `controller.{name}()`

**Example output** (StopWatch):
```kotlin
package io.codenode.stopwatch.generated

import androidx.lifecycle.ViewModel
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowGraph
import kotlinx.coroutines.flow.StateFlow

class StopWatchViewModel(
    private val controller: StopWatchControllerInterface
) : ViewModel() {
    val seconds: StateFlow<Int> = controller.seconds
    val minutes: StateFlow<Int> = controller.minutes
    val executionState: StateFlow<ExecutionState> = controller.executionState
    fun start(): FlowGraph = controller.start()
    fun stop(): FlowGraph = controller.stop()
    fun reset(): FlowGraph = controller.reset()
    fun pause(): FlowGraph = controller.pause()
    fun resume(): FlowGraph = controller.resume()
}
```

---

## Helper: Sink Port Property Collection

### `collectSinkPortProperties(flowGraph: FlowGraph): List<SinkPortProperty>`

Internal helper that collects all observable state entries from sink node input ports.

**Algorithm**:
1. Filter `flowGraph.getAllCodeNodes()` to `codeNodeType == CodeNodeType.SINK`
2. For each sink node, collect `inputPorts`
3. For each port: create `SinkPortProperty(port.name, port.dataType.simpleName, node.name, node.name.camelCase(), port.id)`
4. If multiple sink nodes contribute, prefix each `propertyName` with `sinkNodeCamelCase`
5. Return collected entries

**Data class**:
```kotlin
data class SinkPortProperty(
    val propertyName: String,
    val kotlinType: String,
    val sinkNodeName: String,
    val sinkNodeCamelCase: String,
    val portId: String
)
```

---

## Updated `generateControllerClass()` Changes

The existing `generateControllerClass()` method needs two updates:

1. **StateFlow sources**: Change from generator component references to sink component references:
   ```kotlin
   // Before (hardcoded):
   val elapsedSeconds: StateFlow<Int> = flow.timerEmitter.elapsedSecondsFlow

   // After (derived from sink ports):
   val seconds: StateFlow<Int> = flow.displayReceiver.secondsFlow
   ```

2. **Registry wiring**: Generalize to iterate all nodes:
   ```kotlin
   // Before (hardcoded):
   flow.timerEmitter.registry = registry
   flow.displayReceiver.registry = registry

   // After (iterate all nodes):
   allCodeNodes.forEach { node ->
       flow.${node.name.camelCase()}.registry = registry
   }
   ```

---

## Updated `generateModule()` Method

```kotlin
fun generateModule(flowGraph, moduleName, packageName): GeneratedModule {
    // ... existing files (build.gradle.kts, settings.gradle.kts, Flow, Controller) ...

    // Add ControllerInterface
    files.add(GeneratedFile(
        name = "${flowGraph.name.pascalCase()}ControllerInterface.kt",
        path = "src/commonMain/kotlin/$flowPackagePath",
        content = generateControllerInterfaceClass(flowGraph, packageName)
    ))

    // Add ControllerAdapter
    files.add(GeneratedFile(
        name = "${flowGraph.name.pascalCase()}ControllerAdapter.kt",
        path = "src/commonMain/kotlin/$flowPackagePath",
        content = generateControllerAdapterClass(flowGraph, packageName)
    ))

    // Add ViewModel
    files.add(GeneratedFile(
        name = "${flowGraph.name.pascalCase()}ViewModel.kt",
        path = "src/commonMain/kotlin/$flowPackagePath",
        content = generateViewModelClass(flowGraph, packageName)
    ))

    return GeneratedModule(...)
}
```

---

## Updated `RegenerateStopWatch.kt`

**Port type updates** (from `Any::class` to `Int::class`):
```kotlin
Port(name = "seconds", dataType = Int::class, ...)   // was Any::class
Port(name = "minutes", dataType = Int::class, ...)   // was Any::class
Port(name = "elapsedSeconds", dataType = Int::class, ...)  // was Any::class
Port(name = "elapsedMinutes", dataType = Int::class, ...)  // was Any::class
```

**New file generation** (adds 3 files alongside Controller + Flow):
```kotlin
// Generate ControllerInterface
val interfaceContent = generator.generateControllerInterfaceClass(flowGraph, packageName)
File(stopWatchDir, "StopWatchControllerInterface.kt").writeText(interfaceContent)

// Generate ControllerAdapter
val adapterContent = generator.generateControllerAdapterClass(flowGraph, packageName)
File(stopWatchDir, "StopWatchControllerAdapter.kt").writeText(adapterContent)

// Generate ViewModel
val viewModelContent = generator.generateViewModelClass(flowGraph, packageName)
File(stopWatchDir, "StopWatchViewModel.kt").writeText(viewModelContent)
```
