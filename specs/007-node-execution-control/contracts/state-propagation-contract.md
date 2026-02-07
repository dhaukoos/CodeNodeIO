# Contract: State Propagation Behavior

**Feature**: 007-node-execution-control
**Date**: 2026-02-07
**Type**: Behavioral Contract

## Overview

This contract defines the hierarchical state propagation behavior for execution state and control configuration changes.

## Propagation Rules

### Rule 1: Default Propagation

**When**: A GraphNode's `executionState` or `controlConfig` is changed with `propagate = true` (default).

**Then**:
1. The GraphNode itself is updated with the new value
2. Each direct child is evaluated:
   - If `child.controlConfig.independentControl == false` → child is updated
   - If `child.controlConfig.independentControl == true` → child is unchanged
3. For children that are GraphNodes, step 2 is applied recursively to their children

**Example**:
```
GraphNode A (IDLE → RUNNING)
├── CodeNode B (independentControl = false) → Updated to RUNNING
├── CodeNode C (independentControl = true)  → Remains IDLE
└── GraphNode D (independentControl = false) → Updated to RUNNING
    ├── CodeNode E (independentControl = false) → Updated to RUNNING
    └── CodeNode F (independentControl = true)  → Remains IDLE
```

---

### Rule 2: Independent Control Boundary

**When**: A node has `controlConfig.independentControl == true`.

**Then**:
1. The node does NOT receive state changes from its parent
2. The node CAN be controlled directly (via `setNodeState` or direct `withExecutionState`)
3. When the node is controlled directly, it DOES propagate to its descendants (following Rule 1)

**Rationale**: Independent control creates a "boundary" - the node is isolated from above but still controls below.

---

### Rule 3: No Propagation Mode

**When**: `withExecutionState(state, propagate = false)` is called.

**Then**:
1. Only the target node is updated
2. No children are affected, regardless of `independentControl` flags

**Use Case**: Debugging scenarios where only a specific node's state should change.

---

### Rule 4: Config Propagation Independence

**When**: `controlConfig` is changed on a parent.

**Then**:
1. The `independentControl` flag itself is NEVER propagated (it's a local setting)
2. Other config properties (`pauseBufferSize`, `speedAttenuation`, `autoResumeOnError`) ARE propagated
3. Children with `independentControl == true` retain their entire `controlConfig`

**Example**:
```kotlin
// Parent changes speedAttenuation from 0 to 500ms
parent.withControlConfig(ControlConfig(speedAttenuation = 500L))

// Child A (independentControl = false) → speedAttenuation = 500L
// Child B (independentControl = true)  → speedAttenuation unchanged
```

---

### Rule 5: Synchronization on Independent Control Disable

**When**: A node's `independentControl` is changed from `true` to `false`.

**Then**:
1. The node SHOULD synchronize to its parent's current state
2. This is NOT automatic - requires explicit sync operation
3. RootControlNode provides `syncIndependentNodes()` for batch synchronization

**Signature**:
```kotlin
fun GraphNode.syncToParentState(parentState: ExecutionState): GraphNode
fun RootControlNode.syncIndependentNodes(): FlowGraph
```

---

## Edge Case Behaviors

### Edge Case 1: Parent State Change During Error

**Scenario**: Parent attempts to set RUNNING, but child is in ERROR state.

**Behavior**:
- ERROR state is cleared
- Child transitions to RUNNING
- This enables error recovery via parent control

**Rationale**: Allowing parent to override ERROR enables "try again" workflows.

---

### Edge Case 2: Nested Independent Control

**Scenario**:
```
GraphNode A (independentControl = false)
└── GraphNode B (independentControl = true)
    └── CodeNode C (independentControl = false)
```
A changes state.

**Behavior**:
- A is updated
- B is NOT updated (has independentControl)
- C is NOT updated (parent B was not updated, so no propagation from B)

**Key Insight**: Independent control creates a hard boundary. Nothing below the boundary is affected by changes above.

---

### Edge Case 3: RootControlNode and Independent Nodes

**Scenario**: `rootControlNode.startAll()` called, but some nodes have `independentControl = true`.

**Behavior**:
- Root-level nodes are updated (regardless of their `independentControl` - root nodes have no parent)
- Descendants follow normal propagation rules
- `getStatus()` reports accurate counts including independent nodes' actual states

**Note**: Root-level nodes' `independentControl` flag only matters if they're later added as children of another node.

---

## Invariants

1. **Immutability**: All propagation operations return new instances; original nodes are never mutated.
2. **Consistency**: After any propagation, `node.executionState` equals the state that was set (for affected nodes).
3. **No Cycles**: Propagation always flows downward in the tree; no upward or lateral propagation.
4. **Determinism**: Given the same input, propagation produces identical results every time.

## Performance Bounds

| Scenario | Time Complexity | Space Complexity |
|----------|----------------|------------------|
| Propagation to N nodes | O(N) | O(N) new node instances |
| Skip independent subtree (M nodes) | O(N-M) | O(N-M) new instances |
| Deep tree (depth D, nodes N) | O(N) | O(D) call stack |
