/*
 * RootControlNodeTest - Tests for RootControlNode master controller
 * Verifies unified flow execution control operations
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Tests for RootControlNode - the master controller for FlowGraph execution.
 *
 * User Story 3: RootControlNode Master Control (Priority: P3)
 * Goal: Create RootControlNode class as master controller for entire FlowGraph
 *
 * These tests verify:
 * 1. Factory method createFor()
 * 2. startAll() - transitions all nodes to RUNNING
 * 3. pauseAll() - transitions all nodes to PAUSED
 * 4. stopAll() - transitions all nodes to IDLE
 * 5. getStatus() - returns accurate FlowExecutionStatus
 * 6. setNodeState() - targets specific node by ID
 */
class RootControlNodeTest {

    // ========== Helper Functions ==========

    /**
     * Creates a test CodeNode with the given ID
     */
    private fun createCodeNode(
        id: String,
        executionState: ExecutionState = ExecutionState.IDLE,
        controlConfig: ControlConfig = ControlConfig()
    ): CodeNode {
        return CodeNode(
            id = id,
            name = "CodeNode $id",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(0.0, 0.0),
            inputPorts = listOf(PortFactory.input<String>("input", id)),
            outputPorts = listOf(PortFactory.output<String>("output", id)),
            executionState = executionState,
            controlConfig = controlConfig
        )
    }

    /**
     * Creates a test GraphNode with children
     */
    private fun createGraphNode(
        id: String,
        children: List<Node>,
        executionState: ExecutionState = ExecutionState.IDLE,
        controlConfig: ControlConfig = ControlConfig()
    ): GraphNode {
        val childrenWithParent = children.map { it.withParent(id) }
        return GraphNode(
            id = id,
            name = "GraphNode $id",
            position = Node.Position(0.0, 0.0),
            inputPorts = listOf(PortFactory.input<String>("input", id)),
            outputPorts = listOf(PortFactory.output<String>("output", id)),
            childNodes = childrenWithParent,
            portMappings = mapOf(
                "input" to GraphNode.PortMapping(childrenWithParent.first().id, "input"),
                "output" to GraphNode.PortMapping(childrenWithParent.last().id, "output")
            ),
            executionState = executionState,
            controlConfig = controlConfig
        )
    }

    /**
     * Creates a test FlowGraph with the given root nodes
     */
    private fun createFlowGraph(
        rootNodes: List<Node>,
        name: String = "TestFlow"
    ): FlowGraph {
        return FlowGraph(
            id = "test-flow-${System.currentTimeMillis()}",
            name = name,
            version = "1.0.0",
            rootNodes = rootNodes
        )
    }

    // ========== T032: createFor() Factory Tests ==========

    @Test
    fun `T032 - createFor creates RootControlNode with correct flowGraph`() {
        // Given: A FlowGraph
        val node1 = createCodeNode("node-1")
        val node2 = createCodeNode("node-2")
        val flowGraph = createFlowGraph(listOf(node1, node2))

        // When: Creating a RootControlNode
        val controller = RootControlNode.createFor(flowGraph)

        // Then: Controller should be created with correct properties
        assertNotNull(controller)
        assertNotNull(controller.id)
        assertEquals(flowGraph, controller.flowGraph)
    }

    @Test
    fun `T032 - createFor uses provided name`() {
        // Given: A FlowGraph
        val flowGraph = createFlowGraph(listOf(createCodeNode("node-1")))

        // When: Creating with custom name
        val controller = RootControlNode.createFor(flowGraph, name = "MyController")

        // Then: Name should be set
        assertEquals("MyController", controller.name)
    }

    @Test
    fun `T032 - createFor uses default name when not provided`() {
        // Given: A FlowGraph
        val flowGraph = createFlowGraph(listOf(createCodeNode("node-1")))

        // When: Creating without name
        val controller = RootControlNode.createFor(flowGraph)

        // Then: Default name should be used
        assertEquals("Controller", controller.name)
    }

    @Test
    fun `T032 - createFor sets createdAt timestamp`() {
        // Given: A FlowGraph
        val flowGraph = createFlowGraph(listOf(createCodeNode("node-1")))
        val beforeCreate = System.currentTimeMillis()

        // When: Creating controller
        val controller = RootControlNode.createFor(flowGraph)
        val afterCreate = System.currentTimeMillis()

        // Then: createdAt should be within the time range
        assertTrue(controller.createdAt >= beforeCreate)
        assertTrue(controller.createdAt <= afterCreate)
    }

    // ========== T033: startAll() Tests ==========

    @Test
    fun `T033 - startAll transitions all root nodes to RUNNING`() {
        // Given: FlowGraph with multiple root nodes in IDLE
        val node1 = createCodeNode("node-1")
        val node2 = createCodeNode("node-2")
        val node3 = createCodeNode("node-3")
        val flowGraph = createFlowGraph(listOf(node1, node2, node3))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Calling startAll
        val runningGraph = controller.startAll()

        // Then: All root nodes should be RUNNING
        assertTrue(runningGraph.rootNodes.all { it.executionState == ExecutionState.RUNNING },
            "All root nodes should be RUNNING")
    }

    @Test
    fun `T033 - startAll propagates to all descendants`() {
        // Given: FlowGraph with nested hierarchy
        val grandchild = createCodeNode("grandchild")
        val child = createCodeNode("child")
        val graphNode = createGraphNode("parent", listOf(child, grandchild))
        val flowGraph = createFlowGraph(listOf(graphNode))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Calling startAll
        val runningGraph = controller.startAll()

        // Then: All descendants should be RUNNING
        val allNodes = runningGraph.getAllNodes()
        assertTrue(allNodes.all { it.executionState == ExecutionState.RUNNING },
            "All nodes including descendants should be RUNNING")
    }

    @Test
    fun `T033 - startAll respects independentControl on descendants`() {
        // Given: FlowGraph with independent child
        val independentChild = createCodeNode("independent",
            controlConfig = ControlConfig(independentControl = true))
        val normalChild = createCodeNode("normal")
        val graphNode = createGraphNode("parent", listOf(independentChild, normalChild))
        val flowGraph = createFlowGraph(listOf(graphNode))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Calling startAll
        val runningGraph = controller.startAll()

        // Then: Independent child should remain IDLE
        val parentNode = runningGraph.rootNodes.first() as GraphNode
        val indChild = parentNode.childNodes.find { it.id == "independent" }
        val normChild = parentNode.childNodes.find { it.id == "normal" }

        assertEquals(ExecutionState.RUNNING, parentNode.executionState)
        assertEquals(ExecutionState.IDLE, indChild?.executionState,
            "Independent child should remain IDLE")
        assertEquals(ExecutionState.RUNNING, normChild?.executionState)
    }

    @Test
    fun `T033 - startAll returns new FlowGraph instance`() {
        // Given: FlowGraph
        val flowGraph = createFlowGraph(listOf(createCodeNode("node-1")))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Calling startAll
        val runningGraph = controller.startAll()

        // Then: Should return new instance, original unchanged
        assertEquals(ExecutionState.IDLE, flowGraph.rootNodes.first().executionState)
        assertEquals(ExecutionState.RUNNING, runningGraph.rootNodes.first().executionState)
    }

    // ========== T034: pauseAll() Tests ==========

    @Test
    fun `T034 - pauseAll transitions all root nodes to PAUSED`() {
        // Given: FlowGraph with nodes in RUNNING state
        val node1 = createCodeNode("node-1", executionState = ExecutionState.RUNNING)
        val node2 = createCodeNode("node-2", executionState = ExecutionState.RUNNING)
        val flowGraph = createFlowGraph(listOf(node1, node2))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Calling pauseAll
        val pausedGraph = controller.pauseAll()

        // Then: All root nodes should be PAUSED
        assertTrue(pausedGraph.rootNodes.all { it.executionState == ExecutionState.PAUSED },
            "All root nodes should be PAUSED")
    }

    @Test
    fun `T034 - pauseAll propagates to all descendants`() {
        // Given: FlowGraph with nested hierarchy in RUNNING
        val child = createCodeNode("child", executionState = ExecutionState.RUNNING)
        val graphNode = createGraphNode("parent", listOf(child), executionState = ExecutionState.RUNNING)
        val flowGraph = createFlowGraph(listOf(graphNode))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Calling pauseAll
        val pausedGraph = controller.pauseAll()

        // Then: All nodes should be PAUSED
        val allNodes = pausedGraph.getAllNodes()
        assertTrue(allNodes.all { it.executionState == ExecutionState.PAUSED },
            "All nodes including descendants should be PAUSED")
    }

    @Test
    fun `T034 - pauseAll respects independentControl`() {
        // Given: FlowGraph with independent running child
        val independentChild = createCodeNode("independent",
            executionState = ExecutionState.RUNNING,
            controlConfig = ControlConfig(independentControl = true))
        val graphNode = createGraphNode("parent", listOf(independentChild),
            executionState = ExecutionState.RUNNING)
        val flowGraph = createFlowGraph(listOf(graphNode))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Calling pauseAll
        val pausedGraph = controller.pauseAll()

        // Then: Independent child should still be RUNNING
        val parentNode = pausedGraph.rootNodes.first() as GraphNode
        val indChild = parentNode.childNodes.first()
        assertEquals(ExecutionState.RUNNING, indChild.executionState,
            "Independent child should remain RUNNING")
    }

    // ========== T035: stopAll() Tests ==========

    @Test
    fun `T035 - stopAll transitions all root nodes to IDLE`() {
        // Given: FlowGraph with nodes in various states
        val node1 = createCodeNode("node-1", executionState = ExecutionState.RUNNING)
        val node2 = createCodeNode("node-2", executionState = ExecutionState.PAUSED)
        val node3 = createCodeNode("node-3", executionState = ExecutionState.ERROR)
        val flowGraph = createFlowGraph(listOf(node1, node2, node3))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Calling stopAll
        val stoppedGraph = controller.stopAll()

        // Then: All root nodes should be IDLE
        assertTrue(stoppedGraph.rootNodes.all { it.executionState == ExecutionState.IDLE },
            "All root nodes should be IDLE")
    }

    @Test
    fun `T035 - stopAll propagates to all descendants`() {
        // Given: FlowGraph with nested hierarchy in various states
        val child = createCodeNode("child", executionState = ExecutionState.RUNNING)
        val graphNode = createGraphNode("parent", listOf(child), executionState = ExecutionState.PAUSED)
        val flowGraph = createFlowGraph(listOf(graphNode))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Calling stopAll
        val stoppedGraph = controller.stopAll()

        // Then: All nodes should be IDLE
        val allNodes = stoppedGraph.getAllNodes()
        assertTrue(allNodes.all { it.executionState == ExecutionState.IDLE },
            "All nodes including descendants should be IDLE")
    }

    @Test
    fun `T035 - stopAll respects independentControl`() {
        // Given: FlowGraph with independent running child
        val independentChild = createCodeNode("independent",
            executionState = ExecutionState.RUNNING,
            controlConfig = ControlConfig(independentControl = true))
        val graphNode = createGraphNode("parent", listOf(independentChild),
            executionState = ExecutionState.RUNNING)
        val flowGraph = createFlowGraph(listOf(graphNode))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Calling stopAll
        val stoppedGraph = controller.stopAll()

        // Then: Independent child should still be RUNNING
        val parentNode = stoppedGraph.rootNodes.first() as GraphNode
        val indChild = parentNode.childNodes.first()
        assertEquals(ExecutionState.RUNNING, indChild.executionState,
            "Independent child should remain RUNNING")
    }

    // ========== T036: getStatus() Tests ==========

    @Test
    fun `T036 - getStatus returns accurate total node count`() {
        // Given: FlowGraph with multiple nodes
        val child1 = createCodeNode("child-1")
        val child2 = createCodeNode("child-2")
        val graphNode = createGraphNode("parent", listOf(child1, child2))
        val rootNode = createCodeNode("root")
        val flowGraph = createFlowGraph(listOf(graphNode, rootNode))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Getting status
        val status = controller.getStatus()

        // Then: Total should include all nodes (parent + 2 children + root = 4)
        assertEquals(4, status.totalNodes)
    }

    @Test
    fun `T036 - getStatus returns accurate state counts`() {
        // Given: FlowGraph with nodes in different states
        val runningNode = createCodeNode("running", executionState = ExecutionState.RUNNING)
        val pausedNode = createCodeNode("paused", executionState = ExecutionState.PAUSED)
        val errorNode = createCodeNode("error", executionState = ExecutionState.ERROR)
        val idleNode = createCodeNode("idle", executionState = ExecutionState.IDLE)
        val flowGraph = createFlowGraph(listOf(runningNode, pausedNode, errorNode, idleNode))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Getting status
        val status = controller.getStatus()

        // Then: Counts should be accurate
        assertEquals(4, status.totalNodes)
        assertEquals(1, status.idleCount)
        assertEquals(1, status.runningCount)
        assertEquals(1, status.pausedCount)
        assertEquals(1, status.errorCount)
    }

    @Test
    fun `T036 - getStatus counts independentControl nodes`() {
        // Given: FlowGraph with independent nodes
        val independent1 = createCodeNode("ind-1",
            controlConfig = ControlConfig(independentControl = true))
        val independent2 = createCodeNode("ind-2",
            controlConfig = ControlConfig(independentControl = true))
        val normal = createCodeNode("normal")
        val flowGraph = createFlowGraph(listOf(independent1, independent2, normal))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Getting status
        val status = controller.getStatus()

        // Then: Independent count should be 2
        assertEquals(2, status.independentControlCount)
    }

    @Test
    fun `T036 - getStatus calculates overallState correctly - ERROR priority`() {
        // Given: FlowGraph with one ERROR node
        val runningNode = createCodeNode("running", executionState = ExecutionState.RUNNING)
        val errorNode = createCodeNode("error", executionState = ExecutionState.ERROR)
        val flowGraph = createFlowGraph(listOf(runningNode, errorNode))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Getting status
        val status = controller.getStatus()

        // Then: Overall state should be ERROR (highest priority)
        assertEquals(ExecutionState.ERROR, status.overallState)
    }

    @Test
    fun `T036 - getStatus calculates overallState correctly - RUNNING priority`() {
        // Given: FlowGraph with RUNNING and IDLE nodes (no ERROR)
        val runningNode = createCodeNode("running", executionState = ExecutionState.RUNNING)
        val idleNode = createCodeNode("idle", executionState = ExecutionState.IDLE)
        val flowGraph = createFlowGraph(listOf(runningNode, idleNode))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Getting status
        val status = controller.getStatus()

        // Then: Overall state should be RUNNING
        assertEquals(ExecutionState.RUNNING, status.overallState)
    }

    @Test
    fun `T036 - getStatus calculates overallState correctly - PAUSED priority`() {
        // Given: FlowGraph with PAUSED and IDLE nodes (no ERROR or RUNNING)
        val pausedNode = createCodeNode("paused", executionState = ExecutionState.PAUSED)
        val idleNode = createCodeNode("idle", executionState = ExecutionState.IDLE)
        val flowGraph = createFlowGraph(listOf(pausedNode, idleNode))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Getting status
        val status = controller.getStatus()

        // Then: Overall state should be PAUSED
        assertEquals(ExecutionState.PAUSED, status.overallState)
    }

    @Test
    fun `T036 - getStatus calculates overallState correctly - all IDLE`() {
        // Given: FlowGraph with all IDLE nodes
        val idle1 = createCodeNode("idle-1")
        val idle2 = createCodeNode("idle-2")
        val flowGraph = createFlowGraph(listOf(idle1, idle2))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Getting status
        val status = controller.getStatus()

        // Then: Overall state should be IDLE
        assertEquals(ExecutionState.IDLE, status.overallState)
    }

    @Test
    fun `T036 - getStatus includes nested node states`() {
        // Given: FlowGraph with nested running node
        val runningChild = createCodeNode("running-child", executionState = ExecutionState.RUNNING)
        val graphNode = createGraphNode("parent", listOf(runningChild))
        val flowGraph = createFlowGraph(listOf(graphNode))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Getting status
        val status = controller.getStatus()

        // Then: Running child should be counted
        assertEquals(1, status.runningCount)
        assertEquals(ExecutionState.RUNNING, status.overallState)
    }

    // ========== T037: setNodeState() Tests ==========

    @Test
    fun `T037 - setNodeState changes specific node state`() {
        // Given: FlowGraph with multiple nodes
        val node1 = createCodeNode("node-1")
        val node2 = createCodeNode("node-2")
        val flowGraph = createFlowGraph(listOf(node1, node2))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Setting specific node to RUNNING
        val updatedGraph = controller.setNodeState("node-1", ExecutionState.RUNNING)

        // Then: Only node-1 should be RUNNING
        val updated1 = updatedGraph.findNode("node-1")
        val updated2 = updatedGraph.findNode("node-2")
        assertEquals(ExecutionState.RUNNING, updated1?.executionState)
        assertEquals(ExecutionState.IDLE, updated2?.executionState)
    }

    @Test
    fun `T037 - setNodeState on GraphNode propagates to children`() {
        // Given: FlowGraph with GraphNode
        val child1 = createCodeNode("child-1")
        val child2 = createCodeNode("child-2")
        val graphNode = createGraphNode("parent", listOf(child1, child2))
        val flowGraph = createFlowGraph(listOf(graphNode))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Setting GraphNode to RUNNING
        val updatedGraph = controller.setNodeState("parent", ExecutionState.RUNNING)

        // Then: GraphNode and children should be RUNNING
        val parent = updatedGraph.findNode("parent") as GraphNode
        assertEquals(ExecutionState.RUNNING, parent.executionState)
        assertTrue(parent.childNodes.all { it.executionState == ExecutionState.RUNNING })
    }

    @Test
    fun `T037 - setNodeState on nested node works`() {
        // Given: FlowGraph with nested hierarchy
        val child = createCodeNode("child")
        val graphNode = createGraphNode("parent", listOf(child))
        val flowGraph = createFlowGraph(listOf(graphNode))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Setting nested node to RUNNING
        val updatedGraph = controller.setNodeState("child", ExecutionState.RUNNING)

        // Then: Only the child should be RUNNING
        val parent = updatedGraph.findNode("parent") as GraphNode
        val updatedChild = parent.childNodes.first()
        assertEquals(ExecutionState.IDLE, parent.executionState)
        assertEquals(ExecutionState.RUNNING, updatedChild.executionState)
    }

    @Test
    fun `T037 - setNodeState throws for non-existent node`() {
        // Given: FlowGraph
        val flowGraph = createFlowGraph(listOf(createCodeNode("node-1")))
        val controller = RootControlNode.createFor(flowGraph)

        // When/Then: Setting non-existent node should throw
        assertFailsWith<NoSuchElementException> {
            controller.setNodeState("non-existent", ExecutionState.RUNNING)
        }
    }

    @Test
    fun `T037 - setNodeState on independent node works`() {
        // Given: FlowGraph with independent node
        val independentNode = createCodeNode("independent",
            controlConfig = ControlConfig(independentControl = true))
        val flowGraph = createFlowGraph(listOf(independentNode))
        val controller = RootControlNode.createFor(flowGraph)

        // When: Directly setting independent node's state
        val updatedGraph = controller.setNodeState("independent", ExecutionState.RUNNING)

        // Then: Independent node should be RUNNING
        val updated = updatedGraph.findNode("independent")
        assertEquals(ExecutionState.RUNNING, updated?.executionState)
    }
}
