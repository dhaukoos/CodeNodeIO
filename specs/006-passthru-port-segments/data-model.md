# Data Model: PassThruPort and ConnectionSegment

**Feature**: 006-passthru-port-segments
**Date**: 2026-02-04

## Entity Definitions

### PassThruPort

A specialized port type used exclusively on GraphNode boundaries to bridge connections between interior and exterior scopes.

```
PassThruPort<T>
├── port: Port<T>              # Underlying port (id, name, direction, dataType, owningNodeId)
├── upstreamNodeId: String     # Node ID on upstream (source) side
├── upstreamPortId: String     # Port ID on upstream side
├── downstreamNodeId: String   # Node ID on downstream (sink) side
├── downstreamPortId: String   # Port ID on downstream side
└── Derived properties:
    ├── id: String             # From port.id
    ├── name: String           # From port.name
    ├── direction: Direction   # From port.direction (matches downstream direction)
    ├── dataType: KClass<T>    # From port.dataType (must match both endpoints)
    └── owningNodeId: String   # From port.owningNodeId (the GraphNode ID)
```

**Constraints**:
- `dataType` MUST match `upstreamPort.dataType` AND `downstreamPort.dataType`
- `direction` MUST match `downstreamPort.direction`
- `owningNodeId` MUST be a GraphNode ID (not CodeNode)
- For INPUT PassThruPort: upstream is external, downstream is internal
- For OUTPUT PassThruPort: upstream is internal, downstream is external

**State Transitions**:
- Created: When grouping creates boundary-crossing connection
- Deleted: When ungrouping removes the GraphNode boundary

---

### ConnectionSegment

A visual portion of a Connection representing the path between two endpoints within a single scope context.

```
ConnectionSegment
├── id: String                  # Unique identifier
├── sourceNodeId: String        # Starting node
├── sourcePortId: String        # Starting port
├── targetNodeId: String        # Ending node
├── targetPortId: String        # Ending port
├── scopeNodeId: String?        # Visibility scope (null = root, else = inside GraphNode)
└── parentConnectionId: String  # Reference to parent Connection
```

**Constraints**:
- Adjacent segments in a Connection MUST share a common PassThruPort endpoint
- Segments are ordered from overall source to overall target
- `scopeNodeId` determines in which view context the segment is visible

**Segment Count Rules**:
- CodeNode → CodeNode: 1 segment (direct)
- CodeNode → PassThruPort → CodeNode: 2 segments (1 exterior, 1 interior)
- CodeNode → PassThruPort → PassThruPort → CodeNode: 3 segments (nested GraphNode)

---

### Connection (Extended)

The existing Connection entity with added segments property.

```
Connection
├── id: String
├── sourceNodeId: String
├── sourcePortId: String
├── targetNodeId: String
├── targetPortId: String
├── channelCapacity: Int
├── parentScopeId: String?
├── ipTypeId: String?
└── segments: List<ConnectionSegment>  # NEW - ordered list of visual segments
```

**Constraints**:
- `segments` MUST have at least 1 element
- `segments[0].sourceNodeId` MUST equal `sourceNodeId`
- `segments[last].targetNodeId` MUST equal `targetNodeId`
- For i in 0..<segments.size-1: `segments[i].targetNodeId` MUST equal `segments[i+1].sourceNodeId`

---

### GraphNode (Modified)

GraphNode ports may now include PassThruPorts.

```
GraphNode
├── ... existing properties ...
├── inputPorts: List<Port<*>>   # May contain PassThruPort instances
├── outputPorts: List<Port<*>>  # May contain PassThruPort instances
└── portMappings: Map<String, PortMapping>  # Unchanged
```

**New Validation Rule**:
- When a port is a PassThruPort, its `downstreamNodeId` MUST reference a child node

---

## Entity Relationships

```
┌─────────────────────────────────────────────────────────────────────┐
│                           FlowGraph                                  │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────────┐ │
│  │  CodeNode   │    │  GraphNode  │    │      Connection         │ │
│  │             │    │             │    │  ┌──────────────────┐   │ │
│  │ ○ Port      │───▶│ □ PassThru  │◀───│  │ ConnectionSegment│   │ │
│  │ ○ Port      │    │   Port      │    │  │ ConnectionSegment│   │ │
│  └─────────────┘    │             │    │  └──────────────────┘   │ │
│                     │ ┌─────────┐ │    └─────────────────────────┘ │
│                     │ │ChildNode│ │                                 │
│                     │ └─────────┘ │                                 │
│                     └─────────────┘                                 │
└─────────────────────────────────────────────────────────────────────┘

Legend:
  ○ = Regular Port (circle in UI)
  □ = PassThruPort (square in UI)
  ──▶ = References
```

---

## Validation Rules

### PassThruPort Validation

| Rule | Error Message |
|------|---------------|
| dataType matches upstream | "PassThruPort type mismatch: upstream port has type {x}, expected {y}" |
| dataType matches downstream | "PassThruPort type mismatch: downstream port has type {x}, expected {y}" |
| direction matches downstream | "PassThruPort direction must match downstream: got {x}, expected {y}" |
| owningNode is GraphNode | "PassThruPort must be owned by GraphNode, not {nodeType}" |
| upstream exists | "PassThruPort references non-existent upstream port {id}" |
| downstream exists | "PassThruPort references non-existent downstream port {id}" |

### ConnectionSegment Validation

| Rule | Error Message |
|------|---------------|
| sourceNode exists | "Segment references non-existent source node {id}" |
| targetNode exists | "Segment references non-existent target node {id}" |
| sourcePort exists | "Segment references non-existent source port {id}" |
| targetPort exists | "Segment references non-existent target port {id}" |
| parentConnection exists | "Segment references non-existent connection {id}" |

### Connection.segments Validation

| Rule | Error Message |
|------|---------------|
| segments not empty | "Connection must have at least one segment" |
| first segment matches source | "First segment source must match connection source" |
| last segment matches target | "Last segment target must match connection target" |
| adjacent segments connect | "Segment {i} target must match segment {i+1} source" |

---

## Serialization Format

### .flow.kts DSL Extensions

```kotlin
// PassThruPort in GraphNode definition
val myGroup = graphNode("MyGroup") {
    passThruInput("in_validated", String::class) {
        upstream(externalNode, "output")
        downstream(internalValidator, "input")
    }
    passThruOutput("out_result", String::class) {
        upstream(internalProcessor, "output")
        downstream(externalConsumer, "input")
    }
    // ... child nodes and internal connections ...
}

// Connection with segments (explicit form, typically auto-generated)
connection(nodeA, "output", nodeB, "input") {
    segment(nodeA, "output", groupNode, "in_validated", scope = null)
    segment(groupNode, "in_validated", validator, "input", scope = "groupNode")
}
```

### Backward Compatibility

Files without PassThruPort/segment definitions are automatically upgraded:
1. Existing Port definitions remain valid
2. Missing segments implies single-segment connection
3. PortMappings without PassThruPorts create implicit PassThruPorts on load
