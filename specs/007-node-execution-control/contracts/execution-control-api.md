# Contract: Execution Control API

**Feature**: 007-node-execution-control
**Date**: 2026-02-07
**Type**: Internal API

## Overview

This contract defines the internal API for execution state management operations on nodes and flows.

## Node Execution Control Operations

### Set Execution State

**Operation**: Change the execution state of a node, optionally propagating to descendants.

**Signature**:
```kotlin
fun Node.withExecutionState(
    newState: ExecutionState,
    propagate: Boolean = true
): Node
```

**Preconditions**:
- Node must be valid (passes `validate()`)

**Postconditions**:
- Returned node has `executionState == newState`
- If `propagate == true` and node is GraphNode:
  - All children without `independentControl` have updated state
  - Children with `independentControl` are unchanged
- If `propagate == false`:
  - Only this node's state changes

**Error Conditions**: None (state transitions are always valid)

---

### Set Control Config

**Operation**: Change the control configuration of a node, optionally propagating to descendants.

**Signature**:
```kotlin
fun Node.withControlConfig(
    newConfig: ControlConfig,
    propagate: Boolean = true
): Node
```

**Preconditions**:
- `newConfig` must be valid (passes internal validation)

**Postconditions**:
- Returned node has `controlConfig == newConfig`
- Propagation follows same rules as `withExecutionState`

**Error Conditions**:
- `IllegalArgumentException` if `newConfig.pauseBufferSize <= 0`
- `IllegalArgumentException` if `newConfig.speedAttenuation < 0`

---

## RootControlNode Operations

### Start All Nodes

**Operation**: Transition all nodes in the attached FlowGraph to RUNNING state.

**Signature**:
```kotlin
fun RootControlNode.startAll(): FlowGraph
```

**Preconditions**:
- FlowGraph must be valid

**Postconditions**:
- All root nodes have `executionState == RUNNING`
- All descendants (except `independentControl`) have `executionState == RUNNING`
- Nodes with `independentControl` retain their original state

---

### Pause All Nodes

**Operation**: Transition all nodes in the attached FlowGraph to PAUSED state.

**Signature**:
```kotlin
fun RootControlNode.pauseAll(): FlowGraph
```

**Preconditions**:
- FlowGraph must be valid

**Postconditions**:
- All root nodes have `executionState == PAUSED`
- All descendants (except `independentControl`) have `executionState == PAUSED`
- Nodes with `independentControl` retain their original state

---

### Stop All Nodes

**Operation**: Transition all nodes in the attached FlowGraph to IDLE state.

**Signature**:
```kotlin
fun RootControlNode.stopAll(): FlowGraph
```

**Preconditions**:
- FlowGraph must be valid

**Postconditions**:
- All root nodes have `executionState == IDLE`
- All descendants (except `independentControl`) have `executionState == IDLE`
- Nodes with `independentControl` retain their original state

---

### Get Flow Status

**Operation**: Compute aggregated execution status across all nodes.

**Signature**:
```kotlin
fun RootControlNode.getStatus(): FlowExecutionStatus
```

**Preconditions**: None

**Postconditions**:
- Returns `FlowExecutionStatus` with accurate counts
- `totalNodes` equals total count of all nodes (root + descendants)
- `overallState` follows derivation rules in data-model.md

---

### Set Specific Node State

**Operation**: Set execution state for a specific node by ID.

**Signature**:
```kotlin
fun RootControlNode.setNodeState(
    nodeId: String,
    newState: ExecutionState
): FlowGraph
```

**Preconditions**:
- Node with `nodeId` must exist in the FlowGraph

**Postconditions**:
- Target node has `executionState == newState`
- If target is GraphNode, state propagates to descendants (respecting `independentControl`)

**Error Conditions**:
- `NoSuchElementException` if `nodeId` not found

---

## GraphState Integration (graphEditor)

### Execute State Change

**Operation**: Apply execution state change through GraphState for UI integration.

**Signature**:
```kotlin
fun GraphState.setNodeExecutionState(
    nodeId: String,
    newState: ExecutionState
): Unit
```

**Preconditions**:
- Node with `nodeId` must exist in current FlowGraph

**Postconditions**:
- `flowGraph` is updated with new node state
- Propagation occurs for GraphNode targets
- `isDirty` is set to true
- UI recomposes to reflect new state

---

## Performance Contract

| Operation | Max Latency (p99) | Notes |
|-----------|-------------------|-------|
| withExecutionState (single node) | < 1ms | No tree traversal |
| withExecutionState (100 node tree) | < 10ms | Full propagation |
| withExecutionState (500 node tree) | < 50ms | Full propagation |
| startAll / pauseAll / stopAll | < 100ms | Regardless of graph size |
| getStatus | < 10ms | Tree traversal for counting |
