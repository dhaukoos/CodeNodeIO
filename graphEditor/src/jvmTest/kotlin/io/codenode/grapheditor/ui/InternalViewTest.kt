/*
 * InternalView Tests
 * TDD tests for internal GraphNode view rendering with boundary
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port
import io.codenode.grapheditor.state.GraphState
import kotlin.test.*

/**
 * TDD tests for internal GraphNode view rendering.
 * These tests verify that when navigated into a GraphNode:
 * - Child nodes are displayed
 * - Internal connections are displayed
 * - A boundary indicator shows the GraphNode's ports
 * - Navigation breadcrumb is visible
 *
 * Task: T058 [US5] Write UI test for internal view rendering with boundary
 */
class InternalViewTest {

    // ============================================
    // T058: Tests for internal view rendering
    // ============================================

    @Test
    fun `internal view should display child nodes of GraphNode`() {
        // Given: A GraphNode with multiple children
        val child1 = createTestCodeNode("child1", "Child1", 50.0, 50.0)
        val child2 = createTestCodeNode("child2", "Child2", 200.0, 50.0)
        val child3 = createTestCodeNode("child3", "Child3", 125.0, 150.0)
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(child1, child2, child3),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)

        val graphState = GraphState(graph)

        // When: Navigating into the GraphNode
        graphState.navigateIntoGraphNode("graphNode1")

        // Then: getNodesInCurrentContext should return the child nodes
        val nodesInView = graphState.getNodesInCurrentContext()
        assertEquals(3, nodesInView.size, "Should show all 3 child nodes")
        assertTrue(nodesInView.any { it.id == "child1" }, "Should include child1")
        assertTrue(nodesInView.any { it.id == "child2" }, "Should include child2")
        assertTrue(nodesInView.any { it.id == "child3" }, "Should include child3")
    }

    @Test
    fun `internal view should display internal connections`() {
        // Given: A GraphNode with internal connections between children
        val child1 = createTestCodeNode("child1", "Child1", 50.0, 50.0)
        val child2 = createTestCodeNode("child2", "Child2", 200.0, 50.0)
        val internalConn = Connection(
            id = "internal_conn1",
            sourceNodeId = "child1",
            sourcePortId = "child1_out",
            targetNodeId = "child2",
            targetPortId = "child2_in"
        )
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(child1, child2),
            internalConnections = listOf(internalConn),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)

        val graphState = GraphState(graph)

        // When: Navigating into the GraphNode
        graphState.navigateIntoGraphNode("graphNode1")

        // Then: getConnectionsInCurrentContext should return internal connections
        val connectionsInView = graphState.getConnectionsInCurrentContext()
        assertEquals(1, connectionsInView.size, "Should show internal connection")
        assertEquals("internal_conn1", connectionsInView.first().id)
    }

    @Test
    fun `internal view should not display root-level nodes`() {
        // Given: A graph with both root nodes and a GraphNode with children
        val rootNode = createTestCodeNode("rootNode", "RootNode", 500.0, 100.0)
        val child1 = createTestCodeNode("child1", "Child1", 50.0, 50.0)
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(child1),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(rootNode)
            .addNode(graphNode)

        val graphState = GraphState(graph)

        // When: Navigating into the GraphNode
        graphState.navigateIntoGraphNode("graphNode1")

        // Then: Only child nodes should be visible, not root-level nodes
        val nodesInView = graphState.getNodesInCurrentContext()
        assertFalse(nodesInView.any { it.id == "rootNode" }, "Root node should not be visible")
        assertFalse(nodesInView.any { it.id == "graphNode1" }, "The GraphNode itself should not be visible")
        assertTrue(nodesInView.any { it.id == "child1" }, "Child node should be visible")
    }

    @Test
    fun `internal view should not display root-level connections`() {
        // Given: A graph with root-level connections and a GraphNode with internal connections
        val rootNode = createTestCodeNode("rootNode", "RootNode", 500.0, 100.0)
        val child1 = createTestCodeNode("child1", "Child1", 50.0, 50.0)
        val internalConn = Connection(
            id = "internal_conn1",
            sourceNodeId = "child1",
            sourcePortId = "child1_out",
            targetNodeId = "child1", // Self-loop for simplicity
            targetPortId = "child1_in"
        )
        val inputPort = Port<String>(
            id = "gn_in",
            name = "group_input",
            direction = Port.Direction.INPUT,
            dataType = String::class,
            owningNodeId = "graphNode1"
        )
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(child1),
            internalConnections = listOf(internalConn),
            inputPorts = listOf(inputPort),
            outputPorts = emptyList(),
            portMappings = mapOf("group_input" to GraphNode.PortMapping("child1", "child1_in"))
        )
        val rootConnection = Connection(
            id = "root_conn1",
            sourceNodeId = "rootNode",
            sourcePortId = "rootNode_out",
            targetNodeId = "graphNode1",
            targetPortId = "group_input"
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(rootNode)
            .addNode(graphNode)
            .addConnection(rootConnection)

        val graphState = GraphState(graph)

        // When: Navigating into the GraphNode
        graphState.navigateIntoGraphNode("graphNode1")

        // Then: Only internal connections should be visible
        val connectionsInView = graphState.getConnectionsInCurrentContext()
        assertFalse(connectionsInView.any { it.id == "root_conn1" }, "Root connection should not be visible")
        assertTrue(connectionsInView.any { it.id == "internal_conn1" }, "Internal connection should be visible")
    }

    @Test
    fun `navigation context should track GraphNode name for breadcrumb`() {
        // Given: A GraphNode with a meaningful name
        val child1 = createTestCodeNode("child1", "Child1", 50.0, 50.0)
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "Data Processing Pipeline",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(child1),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)

        val graphState = GraphState(graph)

        // When: Navigating into the GraphNode
        graphState.navigateIntoGraphNode("graphNode1")

        // Then: getCurrentGraphNodeName should return the GraphNode's name for breadcrumb display
        val currentName = graphState.getCurrentGraphNodeName()
        assertEquals("Data Processing Pipeline", currentName, "Should return GraphNode name for breadcrumb")
    }

    @Test
    fun `internal view should indicate GraphNode boundary with input ports`() {
        // Given: A GraphNode with input ports
        val child1 = createTestCodeNode("child1", "Child1", 50.0, 50.0)
        val inputPort = Port<String>(
            id = "gn_in1",
            name = "input1",
            direction = Port.Direction.INPUT,
            dataType = String::class,
            owningNodeId = "graphNode1"
        )
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(child1),
            internalConnections = emptyList(),
            inputPorts = listOf(inputPort),
            outputPorts = emptyList(),
            portMappings = mapOf("input1" to GraphNode.PortMapping("child1", "child1_in"))
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)

        val graphState = GraphState(graph)

        // When: Inside the GraphNode
        graphState.navigateIntoGraphNode("graphNode1")

        // Then: The GraphNode's input ports should be accessible for boundary rendering
        val currentGraphNodeId = graphState.navigationContext.currentGraphNodeId
        val currentGraphNode = graph.findNode(currentGraphNodeId!!) as GraphNode
        assertEquals(1, currentGraphNode.inputPorts.size, "GraphNode should have input port for boundary")
        assertEquals("input1", currentGraphNode.inputPorts.first().name)
    }

    @Test
    fun `internal view should indicate GraphNode boundary with output ports`() {
        // Given: A GraphNode with output ports
        val child1 = createTestCodeNode("child1", "Child1", 50.0, 50.0)
        val outputPort = Port<String>(
            id = "gn_out1",
            name = "output1",
            direction = Port.Direction.OUTPUT,
            dataType = String::class,
            owningNodeId = "graphNode1"
        )
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(child1),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = listOf(outputPort),
            portMappings = mapOf("output1" to GraphNode.PortMapping("child1", "child1_out"))
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)

        val graphState = GraphState(graph)

        // When: Inside the GraphNode
        graphState.navigateIntoGraphNode("graphNode1")

        // Then: The GraphNode's output ports should be accessible for boundary rendering
        val currentGraphNodeId = graphState.navigationContext.currentGraphNodeId
        val currentGraphNode = graph.findNode(currentGraphNodeId!!) as GraphNode
        assertEquals(1, currentGraphNode.outputPorts.size, "GraphNode should have output port for boundary")
        assertEquals("output1", currentGraphNode.outputPorts.first().name)
    }

    @Test
    fun `nested navigation should correctly show deep child nodes`() {
        // Given: Nested GraphNodes (GraphNode containing another GraphNode)
        val deepChild = createTestCodeNode("deepChild", "DeepChild", 25.0, 25.0)
        val innerGraphNode = GraphNode(
            id = "innerGraphNode",
            name = "InnerGroup",
            position = Node.Position(50.0, 50.0),
            childNodes = listOf(deepChild),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val outerGraphNode = GraphNode(
            id = "outerGraphNode",
            name = "OuterGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(innerGraphNode),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(outerGraphNode)

        val graphState = GraphState(graph)

        // When: Navigating two levels deep
        graphState.navigateIntoGraphNode("outerGraphNode")
        graphState.navigateIntoGraphNode("innerGraphNode")

        // Then: Should show deepest child nodes
        val nodesInView = graphState.getNodesInCurrentContext()
        assertEquals(1, nodesInView.size, "Should show only deep child")
        assertEquals("deepChild", nodesInView.first().id)
        assertEquals(2, graphState.navigationContext.depth, "Should be 2 levels deep")
    }

    @Test
    fun `internal view at root should show root nodes not GraphNode children`() {
        // Given: A graph with a GraphNode at root level
        val child1 = createTestCodeNode("child1", "Child1", 50.0, 50.0)
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(child1),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)

        val graphState = GraphState(graph)

        // When: At root level (not navigated into anything)
        // Then: Should show the GraphNode, not its children
        val nodesInView = graphState.getNodesInCurrentContext()
        assertTrue(graphState.navigationContext.isAtRoot, "Should be at root")
        assertEquals(1, nodesInView.size, "Should show 1 root node")
        assertEquals("graphNode1", nodesInView.first().id, "Should show the GraphNode")
    }

    @Test
    fun `selection should be cleared when navigating into GraphNode`() {
        // Given: A GraphNode is selected
        val child1 = createTestCodeNode("child1", "Child1", 50.0, 50.0)
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(child1),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("graphNode1")
        assertTrue(graphState.selectionState.selectedNodeIds.isNotEmpty(), "GraphNode should be selected")

        // When: Navigating into the GraphNode
        graphState.navigateIntoGraphNode("graphNode1")

        // Then: Selection should be cleared
        assertTrue(graphState.selectionState.selectedNodeIds.isEmpty(), "Selection should be cleared after navigation")
    }

    @Test
    fun `internal view should support selecting child nodes`() {
        // Given: Navigated into a GraphNode with children
        val child1 = createTestCodeNode("child1", "Child1", 50.0, 50.0)
        val child2 = createTestCodeNode("child2", "Child2", 200.0, 50.0)
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(child1, child2),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)

        val graphState = GraphState(graph)
        graphState.navigateIntoGraphNode("graphNode1")

        // When: Selecting a child node inside the GraphNode
        graphState.toggleNodeInSelection("child1")

        // Then: Child should be selected
        assertTrue(graphState.selectionState.containsNode("child1"), "Child1 should be selectable")
    }

    @Test
    fun `internal view should support selecting internal connections`() {
        // Given: Navigated into a GraphNode with internal connections
        val child1 = createTestCodeNode("child1", "Child1", 50.0, 50.0)
        val child2 = createTestCodeNode("child2", "Child2", 200.0, 50.0)
        val internalConn = Connection(
            id = "internal_conn1",
            sourceNodeId = "child1",
            sourcePortId = "child1_out",
            targetNodeId = "child2",
            targetPortId = "child2_in"
        )
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(child1, child2),
            internalConnections = listOf(internalConn),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)

        val graphState = GraphState(graph)
        graphState.navigateIntoGraphNode("graphNode1")

        // When: Selecting an internal connection
        graphState.selectConnection("internal_conn1")

        // Then: Connection should be selected
        assertTrue(graphState.selectionState.containsConnection("internal_conn1"), "Internal connection should be selectable")
    }

    // ============================================
    // T067: Zoom-Out Button Tests
    // Tests for zoom-out/back navigation in internal view
    // ============================================

    @Test
    fun `zoom-out should be available when inside a GraphNode`() {
        // Given: Inside a GraphNode
        val child1 = createTestCodeNode("child1", "Child1", 50.0, 50.0)
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(child1),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)

        val graphState = GraphState(graph)
        graphState.navigateIntoGraphNode("graphNode1")

        // Then: canNavigateOut should be true (zoom-out available)
        assertTrue(graphState.navigationContext.canNavigateOut, "Zoom-out should be available inside GraphNode")
    }

    @Test
    fun `zoom-out should not be available at root level`() {
        // Given: At root level
        val child1 = createTestCodeNode("child1", "Child1", 50.0, 50.0)
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(child1),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)

        val graphState = GraphState(graph)

        // Then: canNavigateOut should be false at root
        assertFalse(graphState.navigationContext.canNavigateOut, "Zoom-out should NOT be available at root")
    }

    @Test
    fun `clicking zoom-out should return to parent GraphNode`() {
        // Given: Nested 2 levels deep (inside innerGraphNode)
        val deepChild = createTestCodeNode("deepChild", "DeepChild", 25.0, 25.0)
        val innerGraphNode = GraphNode(
            id = "innerGraphNode",
            name = "InnerGroup",
            position = Node.Position(50.0, 50.0),
            childNodes = listOf(deepChild),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val outerGraphNode = GraphNode(
            id = "outerGraphNode",
            name = "OuterGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(innerGraphNode),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(outerGraphNode)

        val graphState = GraphState(graph)
        graphState.navigateIntoGraphNode("outerGraphNode")
        graphState.navigateIntoGraphNode("innerGraphNode")
        assertEquals(2, graphState.navigationContext.depth)

        // When: Simulating zoom-out button click
        graphState.navigateOut()

        // Then: Should be inside outerGraphNode
        assertEquals(1, graphState.navigationContext.depth)
        assertEquals("outerGraphNode", graphState.navigationContext.currentGraphNodeId)

        // And: Should now see innerGraphNode as the only child
        val nodesInView = graphState.getNodesInCurrentContext()
        assertEquals(1, nodesInView.size)
        assertEquals("innerGraphNode", nodesInView.first().id)
    }

    @Test
    fun `clicking zoom-out from depth 1 should return to root`() {
        // Given: Inside a GraphNode at depth 1
        val child1 = createTestCodeNode("child1", "Child1", 50.0, 50.0)
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(child1),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)

        val graphState = GraphState(graph)
        graphState.navigateIntoGraphNode("graphNode1")
        assertEquals(1, graphState.navigationContext.depth)

        // When: Clicking zoom-out
        graphState.navigateOut()

        // Then: Should be at root and see the GraphNode
        assertTrue(graphState.navigationContext.isAtRoot)
        val nodesInView = graphState.getNodesInCurrentContext()
        assertEquals(1, nodesInView.size)
        assertEquals("graphNode1", nodesInView.first().id)
    }

    @Test
    fun `zoom-out should preserve parent view state`() {
        // Given: Navigated into a GraphNode
        val child1 = createTestCodeNode("child1", "Child1", 50.0, 50.0)
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(child1),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val rootNode = createTestCodeNode("rootNode", "RootNode", 300.0, 100.0)
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)
            .addNode(rootNode)

        val graphState = GraphState(graph)

        // Verify root has 2 nodes
        assertEquals(2, graphState.getNodesInCurrentContext().size)

        // Navigate in and then out
        graphState.navigateIntoGraphNode("graphNode1")
        assertEquals(1, graphState.getNodesInCurrentContext().size)  // Only child1

        graphState.navigateOut()

        // Then: Should see both root nodes again
        val nodesInView = graphState.getNodesInCurrentContext()
        assertEquals(2, nodesInView.size, "Should see both root nodes after zoom-out")
        assertTrue(nodesInView.any { it.id == "graphNode1" })
        assertTrue(nodesInView.any { it.id == "rootNode" })
    }

    @Test
    fun `zoom-out should update current GraphNode name for display`() {
        // Given: 2 levels deep
        val deepChild = createTestCodeNode("deepChild", "DeepChild", 25.0, 25.0)
        val innerGraphNode = GraphNode(
            id = "innerGraphNode",
            name = "Inner Processing",
            position = Node.Position(50.0, 50.0),
            childNodes = listOf(deepChild),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val outerGraphNode = GraphNode(
            id = "outerGraphNode",
            name = "Outer Pipeline",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(innerGraphNode),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(outerGraphNode)

        val graphState = GraphState(graph)
        graphState.navigateIntoGraphNode("outerGraphNode")
        graphState.navigateIntoGraphNode("innerGraphNode")
        assertEquals("Inner Processing", graphState.getCurrentGraphNodeName())

        // When: Zooming out
        graphState.navigateOut()

        // Then: Current name should be "Outer Pipeline"
        assertEquals("Outer Pipeline", graphState.getCurrentGraphNodeName())

        // When: Zooming out again to root
        graphState.navigateOut()

        // Then: Current name should be null
        assertNull(graphState.getCurrentGraphNodeName())
    }

    // ============================================
    // Helper Functions
    // ============================================

    private fun createTestCodeNode(
        id: String,
        name: String,
        x: Double,
        y: Double
    ): CodeNode {
        return CodeNode(
            id = id,
            name = name,
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(x, y),
            inputPorts = listOf(
                Port(
                    id = "${id}_in",
                    name = "${id}_in",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = id
                )
            ),
            outputPorts = listOf(
                Port(
                    id = "${id}_out",
                    name = "${id}_out",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = id
                )
            )
        )
    }
}
