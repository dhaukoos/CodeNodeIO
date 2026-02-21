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

### Connection → Channel Wiring Derivation

Each connection maps to a channel assignment in the Flow class:

```
Connection
├── sourceNode = findNode(sourceNodeId)
├── sourcePortIndex = sourceNode.outputPorts.indexOf(sourcePort)
├── targetNode = findNode(targetNodeId)
├── targetPortIndex = targetNode.inputPorts.indexOf(targetPort)
└── Generated code:
    {targetVarName}.{inputChannelProp} = {sourceVarName}.{outputChannelProp}
```

Channel property name resolution:
- Output port at index 0 with multiple outputs → `outputChannel1`
- Output port at index 1 → `outputChannel2`
- Output port at index 2 → `outputChannel3`
- Input port at index 0 → `inputChannel`
- Input port at index 1 → `inputChannel2`
- Input port at index 2 → `inputChannel3`
