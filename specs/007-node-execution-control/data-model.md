# Data Model: Node ExecutionState and ControlConfig

**Feature**: 007-node-execution-control
**Date**: 2026-02-07

## Entity Overview

This feature modifies existing entities (Node, CodeNode, GraphNode) and introduces new entities (RootControlNode, FlowExecutionStatus).

## Entities

### ExecutionState (Enum)

**Description**: Represents the execution lifecycle state of any Node.

**Location**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/ExecutionState.kt` (extracted from CodeNode)

| Value | Description |
|-------|-------------|
| IDLE | Node is not currently processing; ready to start |
| RUNNING | Node is actively processing InformationPackets |
| PAUSED | Node execution is paused; buffering incoming packets |
| ERROR | Node encountered an error and stopped execution |

**State Transitions**:
```
IDLE → RUNNING (start)
RUNNING → PAUSED (pause)
RUNNING → ERROR (error occurred)
PAUSED → RUNNING (resume)
PAUSED → IDLE (stop)
ERROR → IDLE (reset)
ERROR → RUNNING (force start / error recovery)
```

---

### ControlConfig (Data Class)

**Description**: Configuration for execution control operations. Extended with `independentControl` flag.

**Location**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/ControlConfig.kt` (extracted from CodeNode)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| pauseBufferSize | Int | 100 | Maximum packets to buffer when paused |
| speedAttenuation | Long | 0 | Delay in milliseconds between processing cycles |
| autoResumeOnError | Boolean | false | Whether to automatically resume after error state |
| independentControl | Boolean | false | **NEW**: Exempts node from parent state propagation |

**Validation Rules**:
- `pauseBufferSize` must be > 0
- `speedAttenuation` must be >= 0

**Behavior**:
- When `independentControl = true`, the node:
  - Retains its own `executionState` when parent changes
  - Retains its own `controlConfig` settings when parent changes
  - Must be controlled directly (not via parent propagation)
- When `independentControl = false` (default), the node:
  - Inherits parent's `executionState` on parent state change
  - Inherits parent's `controlConfig` on parent config change (except `independentControl` itself)

---

### Node (Sealed Class - Modified)

**Description**: Base entity for all FBP graph nodes. Extended with execution control properties.

**Location**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Node.kt`

**New Abstract Properties**:

| Property | Type | Description |
|----------|------|-------------|
| executionState | ExecutionState | Current execution lifecycle state |
| controlConfig | ControlConfig | Execution control configuration |

**New Methods** (abstract):

| Method | Signature | Description |
|--------|-----------|-------------|
| withExecutionState | `(ExecutionState, Boolean) -> Node` | Create copy with new state, optionally propagate |
| withControlConfig | `(ControlConfig, Boolean) -> Node` | Create copy with new config, optionally propagate |
| getEffectiveControlConfig | `() -> ControlConfig` | Get merged config (own + inherited) |

---

### CodeNode (Data Class - Modified)

**Description**: Terminal processing node. Properties promoted to Node base class.

**Location**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt`

**Changes**:
- `executionState` property now satisfies abstract property from Node (no code change needed)
- `controlConfig` property now satisfies abstract property from Node (no code change needed)
- Internal `ExecutionState` enum moved to separate file
- Internal `ControlConfig` class moved to separate file

**Backward Compatibility**: All existing CodeNode instantiation patterns continue to work unchanged.

---

### GraphNode (Data Class - Modified)

**Description**: Hierarchical container node. Extended with execution control properties and propagation methods.

**Location**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/GraphNode.kt`

**New Properties**:

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| executionState | ExecutionState | IDLE | Current execution lifecycle state |
| controlConfig | ControlConfig | ControlConfig() | Execution control configuration |

**New Methods**:

| Method | Signature | Description |
|--------|-----------|-------------|
| withExecutionState | `(ExecutionState, Boolean) -> GraphNode` | Create copy with new state, propagate to children if enabled |
| withControlConfig | `(ControlConfig, Boolean) -> GraphNode` | Create copy with new config, propagate to children if enabled |
| propagateStateToChildren | `(ExecutionState) -> List<Node>` | Create updated child list with propagated state |
| propagateConfigToChildren | `(ControlConfig) -> List<Node>` | Create updated child list with propagated config |
| getEffectiveControlConfig | `() -> ControlConfig` | Get own config (GraphNode is authoritative for its subtree) |

**Propagation Behavior**:
1. When `withExecutionState(state, propagate=true)` is called:
   - GraphNode's executionState is updated
   - For each child where `child.controlConfig.independentControl == false`:
     - If child is CodeNode: create copy with new state
     - If child is GraphNode: recursively call `withExecutionState(state, propagate=true)`
   - Children with `independentControl == true` are unchanged
2. Same pattern applies to `withControlConfig`

---

### RootControlNode (New Class)

**Description**: Master controller for an entire FlowGraph. Provides unified execution control operations.

**Location**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/RootControlNode.kt`

| Property | Type | Description |
|----------|------|-------------|
| id | String | Unique identifier for this controller |
| flowGraph | FlowGraph | The flow graph being controlled |
| name | String | Human-readable name |
| createdAt | Long | Timestamp of creation (epoch ms) |

**Methods**:

| Method | Signature | Description |
|--------|-----------|-------------|
| startAll | `() -> FlowGraph` | Set all root nodes to RUNNING, returns updated graph |
| pauseAll | `() -> FlowGraph` | Set all root nodes to PAUSED, returns updated graph |
| stopAll | `() -> FlowGraph` | Set all root nodes to IDLE, returns updated graph |
| getStatus | `() -> FlowExecutionStatus` | Get aggregated status across all nodes |
| setNodeState | `(String, ExecutionState) -> FlowGraph` | Set specific node state by ID |
| setNodeConfig | `(String, ControlConfig) -> FlowGraph` | Set specific node config by ID |

**Factory Method**:
```kotlin
companion object {
    fun createFor(flowGraph: FlowGraph, name: String = "Controller"): RootControlNode
}
```

---

### FlowExecutionStatus (Data Class - New)

**Description**: Aggregated execution status across a FlowGraph.

**Location**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/FlowExecutionStatus.kt`

| Property | Type | Description |
|----------|------|-------------|
| totalNodes | Int | Total number of nodes in the graph |
| idleCount | Int | Count of nodes in IDLE state |
| runningCount | Int | Count of nodes in RUNNING state |
| pausedCount | Int | Count of nodes in PAUSED state |
| errorCount | Int | Count of nodes in ERROR state |
| independentControlCount | Int | Count of nodes with independentControl enabled |
| overallState | ExecutionState | Derived: RUNNING if any running, ERROR if any error, etc. |

**Derivation Rules for overallState**:
1. If any node is in ERROR → ERROR
2. Else if any node is RUNNING → RUNNING
3. Else if any node is PAUSED → PAUSED
4. Else → IDLE

---

## Relationships

```
FlowGraph
    │
    ├── rootNodes: List<Node>
    │       │
    │       ├── CodeNode (has executionState, controlConfig)
    │       │
    │       └── GraphNode (has executionState, controlConfig)
    │               │
    │               └── childNodes: List<Node>
    │                       │
    │                       ├── CodeNode
    │                       └── GraphNode (recursive)
    │
    └── RootControlNode ────> controls ────> FlowGraph
```

## Serialization Notes

- ExecutionState and ControlConfig are `@Serializable`
- New GraphNode properties have defaults, ensuring backward compatibility with older .flow.kts files
- RootControlNode is NOT serialized with the FlowGraph (it's a runtime controller)
- FlowExecutionStatus is computed on demand, not persisted

## Migration Path

1. **Existing CodeNode usage**: No changes required. Existing properties satisfy new abstract contract.
2. **Existing GraphNode usage**: No changes required. New properties have defaults.
3. **Existing .flow.kts files**: Deserialize successfully; new properties get default values.
4. **Tests**: Existing tests continue to pass; new tests added for execution control.
