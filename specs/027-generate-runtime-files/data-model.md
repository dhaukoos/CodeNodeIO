# Data Model: Generate Runtime Files from FlowGraph Compilation

**Feature**: 027-generate-runtime-files
**Date**: 2026-02-20

## Entities

This feature does not introduce new persistent entities. It generates source code files from the existing FlowGraph data model. The key data model relationships leveraged during generation are documented below.

### Input: FlowGraph (existing)

The FlowGraph is the primary input to all generators. Key traversal paths:

```
FlowGraph
├── name: String                    → Used as prefix for all generated class names
├── version: String                 → Included in KDoc
├── description: String?            → Included in KDoc
├── getAllCodeNodes(): List<CodeNode>
│   └── CodeNode
│       ├── name: String            → Component instance name (camelCase), class name (PascalCase)
│       ├── codeNodeType: CodeNodeType
│       ├── inputPorts: List<Port<*>>
│       │   └── Port
│       │       ├── name: String    → Channel property name, observable state property name
│       │       ├── dataType: KClass<*>  → StateFlow generic type
│       │       └── direction: Direction (INPUT/OUTPUT)
│       └── outputPorts: List<Port<*>>
└── connections: List<Connection>
    └── Connection
        ├── sourceNodeId: String    → Resolves to source CodeNode
        ├── sourcePortId: String    → Resolves to source Port (output)
        ├── targetNodeId: String    → Resolves to target CodeNode
        └── targetPortId: String    → Resolves to target Port (input)
```

### Output: Generated File Set

For a FlowGraph named "StopWatch2", the following files are generated:

| File | Package | Class Name | Overwrite Policy |
|------|---------|------------|------------------|
| `StopWatch2Flow.kt` | `{base}.generated` | `StopWatch2Flow` | Always overwrite |
| `StopWatch2Controller.kt` | `{base}.generated` | `StopWatch2Controller` | Always overwrite |
| `StopWatch2ControllerInterface.kt` | `{base}.generated` | `StopWatch2ControllerInterface` | Always overwrite |
| `StopWatch2ControllerAdapter.kt` | `{base}.generated` | `StopWatch2ControllerAdapter` | Always overwrite |
| `StopWatch2ViewModel.kt` | `{base}.generated` | `StopWatch2ViewModel` | Always overwrite |

### Observable State Derivation

The Controller, Interface, Adapter, and ViewModel expose observable state derived from sink nodes:

```
FlowGraph
└── getAllCodeNodes().filter { it.codeNodeType == SINK || (inputPorts.isNotEmpty() && outputPorts.isEmpty()) }
    └── inputPorts
        └── For each port:
            Property name: port.name.camelCase()
            Property type: StateFlow<port.dataType>
```

**Disambiguation rule**: If two sink nodes have ports with the same name, prefix with `{nodeName}{PortName}` in camelCase (e.g., `displayReceiverSeconds`).

### Runtime Instance Creation Derivation

Each CodeNode maps to a direct runtime instance in the Flow class (no Component wrapper):

```
CodeNode
├── name → variable name (camelCase), tick import reference
├── inputPorts.size + outputPorts.size → CodeNodeFactory method selection
├── port types → generic type parameters on runtime
└── Generated code:
    val {varName} = CodeNodeFactory.create{Method}<Types>(
        name = "{NodeName}",
        tick/consume/transform = {nodeName}Tick  // from usecases.logicmethods
    )
```

For sink nodes, the consume block wraps the user's tick to also update MutableStateFlows:
```
consume = { a, b ->
    _portAFlow.value = a
    _portBFlow.value = b
    {nodeName}Tick(a, b)
}
```

### Connection → Channel Wiring Derivation

Each connection maps to a channel assignment between runtime instances:

```
Connection
├── sourceNode = findNode(sourceNodeId)
├── sourcePortIndex = sourceNode.outputPorts.indexOf(sourcePort)
├── targetNode = findNode(targetNodeId)
├── targetPortIndex = targetNode.inputPorts.indexOf(targetPort)
└── Generated code:
    {targetRuntime}.inputChannel{N} = {sourceRuntime}.outputChannel{N}
```

Channel property name resolution (runtime class properties):
- 1 output → `outputChannel`
- 2+ outputs: index 0 → `outputChannel1`, index 1 → `outputChannel2`, index 2 → `outputChannel3`
- 1 input → `inputChannel`
- 2+ inputs: index 0 → `inputChannel1`, index 1 → `inputChannel2`, index 2 → `inputChannel3`
