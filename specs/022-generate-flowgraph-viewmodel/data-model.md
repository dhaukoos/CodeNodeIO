# Data Model: Generate FlowGraph ViewModel

**Feature**: 022-generate-flowgraph-viewmodel
**Date**: 2026-02-19

## Entities

### SinkPortProperty

Derived from sink node input ports in the FlowGraph. Each instance represents one observable state property to generate in the ControllerInterface/ViewModel.

| Field | Type | Description |
|-------|------|-------------|
| propertyName | String | Port name, used as the ControllerInterface property name (e.g., `seconds`) |
| kotlinType | String | Type name derived from `Port.dataType.simpleName` (e.g., `Int`) |
| sinkNodeName | String | Name of the sink CodeNode owning this port (e.g., `DisplayReceiver`) |
| sinkNodeCamelCase | String | camelCase of sink node name for code references (e.g., `displayReceiver`) |
| portId | String | Port ID from the FlowGraph for traceability |

**Derivation**: For each `CodeNode` where `codeNodeType == SINK`, for each port in `inputPorts`:
- `propertyName = port.name`
- `kotlinType = port.dataType.simpleName ?: "Any"`
- `sinkNodeName = codeNode.name`

**Multi-sink prefixing**: When the FlowGraph has multiple sink nodes with input ports, each `propertyName` is prefixed with `sinkNodeCamelCase` to avoid naming collisions (e.g., `displayReceiverSeconds`).

### Generated File Set

The complete set of files generated for the ViewModel layer per FlowGraph.

| File | Class Name | Dependencies |
|------|------------|--------------|
| `{Name}ControllerInterface.kt` | `{Name}ControllerInterface` | fbpDsl (ExecutionState, FlowGraph), kotlinx.coroutines.flow (StateFlow) |
| `{Name}ControllerAdapter.kt` | `{Name}ControllerAdapter` | ControllerInterface, Controller |
| `{Name}ViewModel.kt` | `{Name}ViewModel` | ControllerInterface, androidx.lifecycle.ViewModel |

Where `{Name}` is `flowGraph.name.pascalCase()` (e.g., `StopWatch`).

## Relationships

```text
FlowGraph
  └── CodeNode[] (rootNodes)
        └── codeNodeType == SINK
              └── inputPorts[] → SinkPortProperty[]

ModuleGenerator
  ├── generateControllerClass()              → {Name}Controller.kt     (existing)
  ├── generateFlowGraphClass()               → {Name}Flow.kt           (existing)
  ├── generateControllerInterfaceClass()     → {Name}ControllerInterface.kt (NEW)
  ├── generateControllerAdapterClass()       → {Name}ControllerAdapter.kt   (NEW)
  └── generateViewModelClass()               → {Name}ViewModel.kt           (NEW)
      └── collectSinkPortProperties()        → List<SinkPortProperty>  (shared helper)

ViewModel → ControllerInterface (constructor dependency)
ControllerAdapter → ControllerInterface (implements)
ControllerAdapter → Controller (wraps via delegation)
Controller → Flow (owns instance, reads sink component StateFlows)
Flow → Components (instantiates, wires channels)
```

## State Transitions

No new state machines. The generated ViewModel delegates all state management to the existing Controller/RuntimeRegistry system.

The `executionState: StateFlow<ExecutionState>` follows the existing state machine:
```
IDLE → RUNNING (start)
RUNNING → PAUSED (pause)
PAUSED → RUNNING (resume)
RUNNING → IDLE (stop)
PAUSED → IDLE (stop)
Any → IDLE (reset)
```

## Port Data Type Mapping

The generator maps `Port.dataType` (KClass) to Kotlin type names for generated code:

| Port.dataType | Generated type | StateFlow declaration |
|---------------|---------------|----------------------|
| `Int::class` | `Int` | `val seconds: StateFlow<Int>` |
| `String::class` | `String` | `val label: StateFlow<String>` |
| `Boolean::class` | `Boolean` | `val active: StateFlow<Boolean>` |
| `Double::class` | `Double` | `val value: StateFlow<Double>` |
| `Any::class` | `Any` | `val data: StateFlow<Any>` (fallback) |

The `simpleName` property of `KClass` provides the type name. Custom types use their simple class name.

## Component StateFlow Naming Convention

Sink components must expose StateFlow properties following the `{portName}Flow` convention:

| Port name | Component property | Backing field |
|-----------|-------------------|---------------|
| `seconds` | `val secondsFlow: StateFlow<Int>` | `private val _seconds = MutableStateFlow(0)` |
| `minutes` | `val minutesFlow: StateFlow<Int>` | `private val _minutes = MutableStateFlow(0)` |

The generator constructs Controller references as: `flow.{sinkNodeCamelCase}.{portName}Flow`
