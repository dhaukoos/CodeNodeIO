# Quickstart: Node ExecutionState and ControlConfig

**Feature**: 007-node-execution-control
**Date**: 2026-02-07

## Developer Setup

### Prerequisites

1. CodeNodeIO repository with features 001-006 complete
2. Kotlin 2.1.21 and JDK 17+
3. IntelliJ IDEA or Android Studio with Kotlin plugin

### Running the graphEditor

```bash
# From repository root
./gradlew :graphEditor:run
```

## Feature Walkthrough

### Scenario 1: Basic Execution State Control

**Goal**: Set a GraphNode to RUNNING and verify all children are updated.

**Steps**:
1. Create a flow with a GraphNode containing multiple CodeNodes
2. Navigate into the GraphNode to see children
3. Use execution controls to set parent to RUNNING
4. Verify all children show RUNNING state

**Code Example**:
```kotlin
val graphNode = GraphNode(
    id = "group1",
    name = "ProcessingGroup",
    position = Node.Position(100.0, 100.0),
    childNodes = listOf(nodeA, nodeB, nodeC),
    // Children start in IDLE
)

// Set to RUNNING - all children will update
val runningGroup = graphNode.withExecutionState(ExecutionState.RUNNING)

// Verify
assert(runningGroup.executionState == ExecutionState.RUNNING)
assert(runningGroup.childNodes.all {
    (it as? CodeNode)?.executionState == ExecutionState.RUNNING
})
```

---

### Scenario 2: Independent Control for Debugging

**Goal**: Isolate a subgraph to control it independently from parent.

**Steps**:
1. Create a nested GraphNode hierarchy
2. Set the inner GraphNode to have `independentControl = true`
3. Change the parent's state and verify inner group is unchanged
4. Control the inner group directly

**Code Example**:
```kotlin
// Create inner group with independent control
val innerGroup = GraphNode(
    id = "inner",
    name = "DebugGroup",
    position = Node.Position(200.0, 200.0),
    childNodes = listOf(debugNode1, debugNode2),
    controlConfig = ControlConfig(independentControl = true)  // Enable isolation
)

val outerGroup = GraphNode(
    id = "outer",
    name = "MainGroup",
    position = Node.Position(0.0, 0.0),
    childNodes = listOf(innerGroup, otherNode),
    executionState = ExecutionState.IDLE
)

// Pause outer group
val pausedOuter = outerGroup.withExecutionState(ExecutionState.PAUSED)

// Inner group (independent) is unchanged
val unchangedInner = pausedOuter.childNodes.find { it.id == "inner" } as GraphNode
assert(unchangedInner.executionState == ExecutionState.IDLE)  // Still IDLE!

// Other node (not independent) is paused
val pausedOther = pausedOuter.childNodes.find { it.id != "inner" }
assert((pausedOther as CodeNode).executionState == ExecutionState.PAUSED)

// Control inner group directly
val runningInner = unchangedInner.withExecutionState(ExecutionState.RUNNING)
```

---

### Scenario 3: RootControlNode for Entire Flow

**Goal**: Control an entire flowGraph from a single master controller.

**Steps**:
1. Create a flowGraph with multiple root-level nodes
2. Attach a RootControlNode
3. Use startAll/pauseAll/stopAll for unified control
4. Query status for aggregated state view

**Code Example**:
```kotlin
// Create flow with multiple root nodes
val flowGraph = flowGraph(name = "MyFlow", version = "1.0.0") {}
    .addNode(processingNode1)
    .addNode(processingNode2)
    .addNode(graphNodeWithChildren)

// Create controller
val controller = RootControlNode.createFor(flowGraph, name = "MainController")

// Start everything
val runningFlow = controller.startAll()

// Check status
val status = controller.getStatus()
println("Total nodes: ${status.totalNodes}")
println("Running: ${status.runningCount}")
println("Overall: ${status.overallState}")

// Pause for debugging
val pausedFlow = controller.pauseAll()

// Stop when done
val stoppedFlow = controller.stopAll()
```

---

### Scenario 4: Speed Attenuation for Debugging

**Goal**: Slow down a subgraph to observe data flow.

**Steps**:
1. Select a GraphNode to slow down
2. Set speedAttenuation in controlConfig
3. Verify child nodes inherit the attenuation

**Code Example**:
```kotlin
// Slow down group to 500ms between processing cycles
val slowConfig = ControlConfig(speedAttenuation = 500L)
val slowGroup = graphNode.withControlConfig(slowConfig)

// All children now have 500ms delay
slowGroup.childNodes.forEach { child ->
    if (!child.controlConfig.independentControl) {
        assert(child.controlConfig.speedAttenuation == 500L)
    }
}
```

---

### Scenario 5: Error Recovery

**Goal**: Recover a subgraph from ERROR state.

**Steps**:
1. Simulate an error in a node
2. Observe error propagation (or containment)
3. Recover by setting parent to RUNNING

**Code Example**:
```kotlin
// Node enters error state
val erroredNode = codeNode.withExecutionState(ExecutionState.ERROR)

// Place in group
val groupWithError = graphNode.copy(
    childNodes = listOf(erroredNode, otherNode)
)

// Recover by setting parent to RUNNING
val recoveredGroup = groupWithError.withExecutionState(ExecutionState.RUNNING)

// Error is cleared, node is now running
assert((recoveredGroup.childNodes[0] as CodeNode).executionState == ExecutionState.RUNNING)
```

---

## Key Code Locations

| Component | File Path |
|-----------|-----------|
| ExecutionState enum | `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/ExecutionState.kt` |
| ControlConfig class | `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/ControlConfig.kt` |
| Node base class | `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Node.kt` |
| GraphNode (propagation) | `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/GraphNode.kt` |
| RootControlNode | `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/RootControlNode.kt` |
| FlowExecutionStatus | `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/FlowExecutionStatus.kt` |

## Testing Commands

```bash
# Run Node execution state tests
./gradlew :fbpDsl:jvmTest --tests "io.codenode.fbpdsl.model.NodeExecutionStateTest"

# Run state propagation tests
./gradlew :fbpDsl:jvmTest --tests "io.codenode.fbpdsl.model.StatePropagationTest"

# Run RootControlNode tests
./gradlew :fbpDsl:jvmTest --tests "io.codenode.fbpdsl.model.RootControlNodeTest"

# Run all fbpDsl tests
./gradlew :fbpDsl:jvmTest

# Run graphEditor integration tests
./gradlew :graphEditor:jvmTest --tests "io.codenode.grapheditor.state.ExecutionControlTest"
```

## Common Issues

### Issue: State not propagating to children
**Solution**: Check if children have `independentControl = true`. Use `getStatus()` to see which nodes have independent control enabled.

### Issue: Independent node not responding to control
**Solution**: Independent nodes must be controlled directly. Use `setNodeState(nodeId, state)` on RootControlNode or call `withExecutionState` directly on the node.

### Issue: Unexpected state after deserialization
**Solution**: Execution state is persisted in .flow.kts files. Old files deserialize with default IDLE state. If you need to persist a running state, save after starting.

### Issue: Performance slow on large graphs
**Solution**: Check for excessive nesting depth (> 10 levels) or very large flat hierarchies (> 500 nodes). Consider restructuring the graph or using independent control to limit propagation scope.
