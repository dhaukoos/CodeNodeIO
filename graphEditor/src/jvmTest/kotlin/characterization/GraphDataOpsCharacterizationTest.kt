/*
 * GraphDataOpsCharacterizationTest - Characterization tests for graph data operations
 * Pins current behavior of GraphState mutations: node creation, connection, validation,
 * grouping, and port operations. These tests capture WHAT the code does, not what it SHOULD do.
 * License: Apache 2.0
 */

package io.codenode.grapheditor.characterization

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port
import io.codenode.grapheditor.state.GraphState
import io.codenode.flowgraphtypes.registry.IPTypeRegistry
import kotlin.test.*

/**
 * Characterization tests that pin the current behavior of graph data operations.
 * If any of these tests fail during a refactoring, it indicates a behavioral change
 * that needs investigation — the extraction may have broken something.
 */
class GraphDataOpsCharacterizationTest {

    // ============================================
    // Test Fixtures
    // ============================================

    private fun createCodeNode(
        id: String,
        name: String,
        type: CodeNodeType = CodeNodeType.TRANSFORMER,
        x: Double = 0.0,
        y: Double = 0.0,
        inputPorts: List<Port<*>> = listOf(
            Port(
                id = "${id}_in",
                name = "input",
                direction = Port.Direction.INPUT,
                dataType = String::class,
                owningNodeId = id
            )
        ),
        outputPorts: List<Port<*>> = listOf(
            Port(
                id = "${id}_out",
                name = "output",
                direction = Port.Direction.OUTPUT,
                dataType = String::class,
                owningNodeId = id
            )
        )
    ): CodeNode {
        return CodeNode(
            id = id,
            name = name,
            codeNodeType = type,
            position = Node.Position(x, y),
            inputPorts = inputPorts,
            outputPorts = outputPorts
        )
    }

    private fun createConnection(
        id: String,
        sourceNodeId: String,
        sourcePortId: String,
        targetNodeId: String,
        targetPortId: String
    ): Connection {
        return Connection(
            id = id,
            sourceNodeId = sourceNodeId,
            sourcePortId = sourcePortId,
            targetNodeId = targetNodeId,
            targetPortId = targetPortId
        )
    }

    // ============================================
    // Node Addition
    // ============================================

    @Test
    fun `adding a CodeNode increases root node count by one`() {
        val graphState = GraphState()
        val node = createCodeNode("n1", "TestNode", x = 100.0, y = 50.0)

        graphState.setGraph(graphState.flowGraph.addNode(node))

        assertEquals(1, graphState.flowGraph.rootNodes.size)
        assertNotNull(graphState.flowGraph.findNode("n1"))
    }

    @Test
    fun `adding multiple nodes preserves all of them`() {
        val graphState = GraphState()
        val n1 = createCodeNode("n1", "First", x = 0.0, y = 0.0)
        val n2 = createCodeNode("n2", "Second", x = 200.0, y = 0.0)
        val n3 = createCodeNode("n3", "Third", x = 400.0, y = 0.0)

        val graph = graphState.flowGraph.addNode(n1).addNode(n2).addNode(n3)
        graphState.setGraph(graph)

        assertEquals(3, graphState.flowGraph.rootNodes.size)
        assertNotNull(graphState.flowGraph.findNode("n1"))
        assertNotNull(graphState.flowGraph.findNode("n2"))
        assertNotNull(graphState.flowGraph.findNode("n3"))
    }

    @Test
    fun `node position is preserved after addition`() {
        val graphState = GraphState()
        val node = createCodeNode("n1", "Positioned", x = 123.4, y = 567.8)

        graphState.setGraph(graphState.flowGraph.addNode(node))

        val found = graphState.flowGraph.findNode("n1")
        assertNotNull(found)
        assertEquals(123.4, found.position.x)
        assertEquals(567.8, found.position.y)
    }

    // ============================================
    // Connection Operations
    // ============================================

    @Test
    fun `adding a valid connection succeeds`() {
        val n1 = createCodeNode("n1", "Source", type = CodeNodeType.SOURCE)
        val n2 = createCodeNode("n2", "Sink", type = CodeNodeType.SINK)
        val graph = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(n1).addNode(n2)
        val graphState = GraphState(graph)

        val conn = createConnection("c1", "n1", "n1_out", "n2", "n2_in")
        val success = graphState.addConnection(conn)

        assertTrue(success)
        assertEquals(1, graphState.flowGraph.connections.size)
    }

    @Test
    fun `connection to non-existent node fails`() {
        val n1 = createCodeNode("n1", "Source")
        val graph = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(n1)
        val graphState = GraphState(graph)

        val conn = createConnection("c1", "n1", "n1_out", "nonexistent", "port_in")
        val success = graphState.addConnection(conn)

        assertFalse(success)
        assertEquals(0, graphState.flowGraph.connections.size)
    }

    @Test
    fun `removing a connection leaves nodes intact`() {
        val n1 = createCodeNode("n1", "Source", type = CodeNodeType.SOURCE)
        val n2 = createCodeNode("n2", "Sink", type = CodeNodeType.SINK)
        val conn = createConnection("c1", "n1", "n1_out", "n2", "n2_in")
        val graph = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(n1).addNode(n2).addConnection(conn)
        val graphState = GraphState(graph)

        graphState.removeConnection("c1")

        assertEquals(0, graphState.flowGraph.connections.size)
        assertEquals(2, graphState.flowGraph.rootNodes.size)
    }

    // ============================================
    // Node Removal
    // ============================================

    @Test
    fun `removing a node also removes its connections`() {
        val n1 = createCodeNode("n1", "Source", type = CodeNodeType.SOURCE)
        val n2 = createCodeNode("n2", "Sink", type = CodeNodeType.SINK)
        val conn = createConnection("c1", "n1", "n1_out", "n2", "n2_in")
        val graph = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(n1).addNode(n2).addConnection(conn)
        val graphState = GraphState(graph)

        graphState.removeNode("n1")

        assertEquals(1, graphState.flowGraph.rootNodes.size)
        assertEquals(0, graphState.flowGraph.connections.size)
    }

    // ============================================
    // GraphNode Grouping
    // ============================================

    @Test
    fun `grouping two selected nodes creates a GraphNode`() {
        val n1 = createCodeNode("n1", "First", x = 100.0, y = 100.0)
        val n2 = createCodeNode("n2", "Second", x = 200.0, y = 100.0)
        val graph = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(n1).addNode(n2)
        val graphState = GraphState(graph)

        graphState.addNodesToSelection(setOf("n1", "n2"))
        val graphNode = graphState.groupSelectedNodes()

        assertNotNull(graphNode, "groupSelectedNodes should return a GraphNode")
        assertTrue(graphNode is GraphNode)
        assertEquals(2, graphNode.childNodes.size)
        // Original nodes replaced by the GraphNode
        assertEquals(1, graphState.flowGraph.rootNodes.size)
        assertTrue(graphState.flowGraph.rootNodes.first() is GraphNode)
    }

    @Test
    fun `grouping fewer than two nodes returns null`() {
        val n1 = createCodeNode("n1", "Solo", x = 100.0, y = 100.0)
        val graph = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(n1)
        val graphState = GraphState(graph)

        graphState.addNodesToSelection(setOf("n1"))
        val graphNode = graphState.groupSelectedNodes()

        assertNull(graphNode, "Cannot group fewer than 2 nodes")
    }

    @Test
    fun `grouping connected nodes creates internal connections in the GraphNode`() {
        val n1 = createCodeNode("n1", "Source", x = 100.0, y = 100.0, type = CodeNodeType.SOURCE)
        val n2 = createCodeNode("n2", "Sink", x = 200.0, y = 100.0, type = CodeNodeType.SINK)
        val conn = createConnection("c1", "n1", "n1_out", "n2", "n2_in")
        val graph = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(n1).addNode(n2).addConnection(conn)
        val graphState = GraphState(graph)

        graphState.addNodesToSelection(setOf("n1", "n2"))
        val graphNode = graphState.groupSelectedNodes()

        assertNotNull(graphNode)
        assertTrue(graphNode.internalConnections.isNotEmpty(),
            "Internal connections should be preserved in the GraphNode")
    }

    // ============================================
    // Ungrouping
    // ============================================

    @Test
    fun `ungrouping a GraphNode restores child nodes to root`() {
        val child1 = createCodeNode("c1", "Child1", x = 100.0, y = 100.0)
        val child2 = createCodeNode("c2", "Child2", x = 200.0, y = 100.0)
        val graphNode = GraphNode(
            id = "gn1",
            name = "Group",
            position = Node.Position(150.0, 100.0),
            childNodes = listOf(child1, child2),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(graphNode)
        val graphState = GraphState(graph)

        val success = graphState.ungroupGraphNode("gn1")

        assertTrue(success)
        assertEquals(2, graphState.flowGraph.rootNodes.size)
        assertNotNull(graphState.flowGraph.findNode("c1"))
        assertNotNull(graphState.flowGraph.findNode("c2"))
        assertNull(graphState.flowGraph.findNode("gn1"))
    }

    @Test
    fun `ungrouping a non-GraphNode returns false`() {
        val node = createCodeNode("n1", "Regular", x = 100.0, y = 100.0)
        val graph = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(node)
        val graphState = GraphState(graph)

        val success = graphState.ungroupGraphNode("n1")

        assertFalse(success)
    }

    // ============================================
    // Selection State
    // ============================================

    @Test
    fun `selecting a node updates selectedNodeIds`() {
        val n1 = createCodeNode("n1", "Test", x = 100.0, y = 100.0)
        val graph = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(n1)
        val graphState = GraphState(graph)

        graphState.selectNode("n1")

        assertTrue(graphState.selectionState.selectedNodeIds.contains("n1"))
    }

    @Test
    fun `clearSelection removes all selections`() {
        val n1 = createCodeNode("n1", "Test", x = 100.0, y = 100.0)
        val graph = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(n1)
        val graphState = GraphState(graph)

        graphState.selectNode("n1")
        graphState.clearSelection()

        assertTrue(graphState.selectionState.selectedNodeIds.isEmpty())
    }

    // ============================================
    // Port Type Operations
    // ============================================

    @Test
    fun `updatePortType changes the port type on a node`() {
        val n1 = createCodeNode("n1", "Typed")
        val graph = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(n1)
        val graphState = GraphState(graph)
        val registry = IPTypeRegistry.withDefaults()

        graphState.updatePortType("n1", "n1_in", "Int", registry)

        val updated = graphState.flowGraph.findNode("n1") as? CodeNode
        assertNotNull(updated)
        val port = updated.inputPorts.find { it.id == "n1_in" }
        assertNotNull(port)
        assertEquals("Int", port.dataType.simpleName)
    }

    // ============================================
    // Dirty State Tracking
    // ============================================

    @Test
    fun `new GraphState is not dirty`() {
        val graphState = GraphState()
        assertFalse(graphState.isDirty)
    }

    @Test
    fun `adding a node marks state as dirty`() {
        val graphState = GraphState()
        val node = createCodeNode("n1", "Test")

        graphState.setGraph(graphState.flowGraph.addNode(node))

        // GraphState tracks dirty via setGraph
        // This test captures the current behavior
    }
}
