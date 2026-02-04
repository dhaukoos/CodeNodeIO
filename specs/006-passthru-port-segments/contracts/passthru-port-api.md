# Contract: PassThruPort API

**Feature**: 006-passthru-port-segments
**Date**: 2026-02-04

## PassThruPort Factory API

### createPassThruPort

Creates a PassThruPort for a boundary-crossing connection.

```kotlin
object PassThruPortFactory {
    /**
     * Creates a PassThruPort for a connection crossing a GraphNode boundary.
     *
     * @param graphNodeId The GraphNode that will own this port
     * @param upstreamNodeId Node on the upstream (source) side
     * @param upstreamPortId Port on the upstream side
     * @param downstreamNodeId Node on the downstream (sink) side
     * @param downstreamPortId Port on the downstream side
     * @param direction Direction of the port (INPUT or OUTPUT)
     * @param dataType The data type for the port
     * @return Result containing PassThruPort or validation errors
     */
    fun <T : Any> create(
        graphNodeId: String,
        upstreamNodeId: String,
        upstreamPortId: String,
        downstreamNodeId: String,
        downstreamPortId: String,
        direction: Port.Direction,
        dataType: KClass<T>
    ): Result<PassThruPort<T>>
}
```

**Preconditions**:
- All node and port IDs must reference existing entities
- `dataType` must be compatible with both upstream and downstream ports
- `direction` must match downstream port direction

**Postconditions**:
- Returns Success with valid PassThruPort
- Or Returns Failure with descriptive error message

**Error Cases**:
| Error | Message |
|-------|---------|
| Type mismatch (upstream) | "Upstream port {id} has incompatible type: expected {expected}, got {actual}" |
| Type mismatch (downstream) | "Downstream port {id} has incompatible type: expected {expected}, got {actual}" |
| Direction mismatch | "Direction {given} does not match downstream port direction {expected}" |
| Node not found | "Node {id} not found in graph" |
| Port not found | "Port {id} not found on node {nodeId}" |

---

## PassThruPort Validation API

### validate

Validates a PassThruPort against the current graph state.

```kotlin
fun PassThruPort<*>.validate(
    graph: FlowGraph
): ValidationResult
```

**Returns**: ValidationResult with success=true or list of errors

---

## Integration Points

### GraphNodeFactory Integration

```kotlin
object GraphNodeFactory {
    /**
     * Creates a GraphNode from selected nodes, including PassThruPorts
     * for boundary-crossing connections.
     *
     * @param selectedNodeIds IDs of nodes to group
     * @param allNodes All nodes in the current context
     * @param allConnections All connections in the current context
     * @param graphNodeName Name for the new GraphNode
     * @return The created GraphNode with PassThruPorts, or null if invalid
     */
    fun createFromSelection(
        selectedNodeIds: Set<String>,
        allNodes: List<Node>,
        allConnections: List<Connection>,
        graphNodeName: String
    ): GraphNode?
}
```

**Behavior Change**:
- Now creates PassThruPorts for each boundary-crossing connection
- PassThruPorts are added to GraphNode's inputPorts/outputPorts
- Returns null if any PassThruPort creation fails (type mismatch)

---

## Test Requirements

### Unit Tests

1. **PassThruPort creation with valid types** - Should succeed
2. **PassThruPort creation with mismatched upstream type** - Should fail
3. **PassThruPort creation with mismatched downstream type** - Should fail
4. **PassThruPort creation with wrong direction** - Should fail
5. **PassThruPort validation with missing upstream node** - Should fail
6. **PassThruPort validation with missing downstream node** - Should fail
7. **PassThruPort delegates to underlying Port** - Properties accessible

### Integration Tests

1. **Group nodes creates PassThruPorts** - Verify correct ports created
2. **Ungroup removes PassThruPorts** - Verify ports removed
3. **Nested grouping creates chained PassThruPorts** - Multiple levels work
