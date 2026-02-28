# Data Model: Refine the ViewModel Binding

**Feature**: 034-refine-viewmodel-binding
**Date**: 2026-02-28

## Existing Entities (No Changes)

### FlowGraph
The source-of-truth definition for a module. Already provides all data needed:
- `name: String` — module name (e.g., "StopWatch")
- `codeNodes: List<CodeNode>` — all nodes with ports
- `connections: List<Connection>` — channel wiring between ports

### CodeNode
Individual processing nodes. Port data drives code generation:
- `name: String` — node name (e.g., "TimerEmitter", "DisplayReceiver")
- `inputPorts: List<Port<*>>` — data inputs
- `outputPorts: List<Port<*>>` — data outputs
- `nodeType: String` — "GENERIC", used to determine factory method

### Port
Typed connection point on a node:
- `name: String` — becomes property name in generated code
- `dataType: KClass<*>` — Kotlin type, determines MutableStateFlow type parameter

### ObservableProperty (existing, from ObservableStateResolver)
Represents a single observable state property derived from a sink input port:
- `name: String` — camelCase property name (from port name, with disambiguation)
- `typeName: String` — Kotlin type name (e.g., "Int", "String")
- `sourceNodeName: String` — originating sink node
- `sourcePortName: String` — originating port
- `defaultValue: String` — default MutableStateFlow value (e.g., "0", "\"\"", "false")

## New Entity: Module State Object

### Purpose
Replaces per-node `{NodeName}StateProperties` singleton objects with a single `{ModuleName}State` object in the ViewModel file.

### Structure
```
{ModuleName}State (object)
├── Per observable property (from sink input ports):
│   ├── _propertyName: MutableStateFlow<T>  (internal, writable)
│   └── propertyNameFlow: StateFlow<T>      (public, read-only)
└── reset(): Unit  (resets all properties to defaults)
```

### Derivation Rules
- One property pair per sink node input port
- Property names follow ObservableStateResolver disambiguation:
  - Unique across sinks → port name as-is
  - Colliding across sinks → `{nodeName}{portName}`
- Type from `port.dataType.simpleName`
- Default value from type (Int→0, String→"", Boolean→false, Long→0L, Double→0.0, Float→0.0f)

### Example: StopWatch Module
Given DisplayReceiver sink with inputs `seconds: Int` and `minutes: Int`:
```
StopWatchState (object)
├── _seconds: MutableStateFlow<Int> = MutableStateFlow(0)
├── secondsFlow: StateFlow<Int>
├── _minutes: MutableStateFlow<Int> = MutableStateFlow(0)
├── minutesFlow: StateFlow<Int>
└── reset()
```

## Relationship Changes

### Before (Per-Node StateProperties)
```
stateProperties/
├── TimerEmitterStateProperties.kt    ← object with output port MutableStateFlows
└── DisplayReceiverStateProperties.kt ← object with input port MutableStateFlows

Flow imports → N StateProperties objects
Controller delegates → Flow
ViewModel delegates → Controller
```

### After (Consolidated Module State)
```
StopWatchViewModel.kt  (stub, base package)
├── StopWatchState object  ← all sink input port MutableStateFlows
└── StopWatchViewModel class

Flow imports → 1 {ModuleName}State object
Controller delegates → Flow
ViewModel delegates → Controller + direct access to same-file State
```

## Files Affected

### Deleted Entities
- Per-node `{NodeName}StateProperties` objects (no longer generated)
- `stateProperties/` directory (no longer created)

### Moved Entities
- `{ModuleName}ViewModel` class: from `generated/` → base package directory

### New Entities
- `{ModuleName}State` object: generated within ViewModel stub file
