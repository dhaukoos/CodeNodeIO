/*
 * GraphState Tests
 * TDD tests for GraphState ungroupGraphNode functionality
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port
import kotlin.test.*

/**
 * TDD tests for GraphState ungroupGraphNode functionality.
 * Tests T049 and T050 for User Story 4 (Ungroup GraphNode via Toolbar).
 */
class GraphStateTest {

    // ============================================
    // T049: Tests for ungroupGraphNode() restoring child nodes
    // ============================================

    @Test
    fun `ungroupGraphNode should restore child nodes to parent graph`() {
        // Given: A graph with a GraphNode containing 2 child nodes
        val child1 = createTestCodeNode("child1", "Child1", 100.0, 100.0)
        val child2 = createTestCodeNode("child2", "Child2", 200.0, 100.0)
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(150.0, 100.0),
            childNodes = listOf(child1, child2),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)

        val graphState = GraphState(graph)

        // When: Ungrouping the GraphNode
        val success = graphState.ungroupGraphNode("graphNode1")

        // Then: Child nodes should be restored to root graph
        assertTrue(success, "ungroupGraphNode should return true")
        val rootNodeIds = graphState.flowGraph.rootNodes.map { it.id }
        assertTrue(rootNodeIds.contains("child1"), "child1 should be in root nodes")
        assertTrue(rootNodeIds.contains("child2"), "child2 should be in root nodes")
        assertFalse(rootNodeIds.contains("graphNode1"), "GraphNode should be removed from root")
    }

    @Test
    fun `ungroupGraphNode should preserve child node original positions`() {
        // Given: A GraphNode with child nodes at their original absolute positions
        // (When nodes are grouped, they keep their absolute positions - the GraphNode
        // is just placed at their centroid)
        val child1 = createTestCodeNode("child1", "Child1", 100.0, 100.0)
        val child2 = createTestCodeNode("child2", "Child2", 200.0, 150.0)
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(150.0, 125.0), // Centroid of children
            childNodes = listOf(child1, child2),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)

        val graphState = GraphState(graph)

        // When: Ungrouping the GraphNode
        graphState.ungroupGraphNode("graphNode1")

        // Then: Child nodes should keep their original absolute positions (no offset)
        val restoredChild1 = graphState.flowGraph.findNode("child1")
        val restoredChild2 = graphState.flowGraph.findNode("child2")

        assertNotNull(restoredChild1, "child1 should exist in graph")
        assertNotNull(restoredChild2, "child2 should exist in graph")

        // Child positions should be preserved exactly as they were
        assertEquals(100.0, restoredChild1.position.x, "child1 x position should be preserved")
        assertEquals(100.0, restoredChild1.position.y, "child1 y position should be preserved")
        assertEquals(200.0, restoredChild2.position.x, "child2 x position should be preserved")
        assertEquals(150.0, restoredChild2.position.y, "child2 y position should be preserved")
    }

    @Test
    fun `ungroupGraphNode should return false for non-existent node`() {
        // Given: A graph without the specified node
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}

        val graphState = GraphState(graph)

        // When: Trying to ungroup a non-existent node
        val success = graphState.ungroupGraphNode("nonExistent")

        // Then: Should return false
        assertFalse(success, "ungroupGraphNode should return false for non-existent node")
    }

    @Test
    fun `ungroupGraphNode should return false for CodeNode`() {
        // Given: A graph with a CodeNode (not a GraphNode)
        val codeNode = createTestCodeNode("node1", "Node1", 100.0, 100.0)
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(codeNode)

        val graphState = GraphState(graph)

        // When: Trying to ungroup a CodeNode
        val success = graphState.ungroupGraphNode("node1")

        // Then: Should return false (can only ungroup GraphNodes)
        assertFalse(success, "ungroupGraphNode should return false for CodeNode")
    }

    @Test
    fun `ungroupGraphNode should clear parentNodeId from restored children`() {
        // Given: A GraphNode with children that have parentNodeId set
        val child1 = CodeNode(
            id = "child1",
            name = "Child1",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(0.0, 0.0),
            parentNodeId = "graphNode1",
            inputPorts = emptyList(),
            outputPorts = emptyList()
        )
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

        // When: Ungrouping the GraphNode
        graphState.ungroupGraphNode("graphNode1")

        // Then: Restored child should have null parentNodeId
        val restoredChild = graphState.flowGraph.findNode("child1") as? CodeNode
        assertNotNull(restoredChild, "child1 should exist in graph")
        assertNull(restoredChild.parentNodeId, "parentNodeId should be cleared after ungroup")
    }

    // ============================================
    // T050: Tests for connection restoration after ungroup
    // ============================================

    @Test
    fun `ungroupGraphNode should restore internal connections`() {
        // Given: A GraphNode with internal connections between children
        val child1 = createTestCodeNode("child1", "Child1", 0.0, 0.0)
        val child2 = createTestCodeNode("child2", "Child2", 100.0, 0.0)
        val internalConnection = Connection(
            id = "internal_conn",
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
            internalConnections = listOf(internalConnection),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)

        val graphState = GraphState(graph)

        // When: Ungrouping the GraphNode
        graphState.ungroupGraphNode("graphNode1")

        // Then: Internal connection should be restored to root graph
        val restoredConnection = graphState.flowGraph.connections.find { it.id == "internal_conn" }
        assertNotNull(restoredConnection, "Internal connection should be restored")
        assertEquals("child1", restoredConnection.sourceNodeId)
        assertEquals("child2", restoredConnection.targetNodeId)
    }

    @Test
    fun `ungroupGraphNode should restore external connections using port mappings`() {
        // Given: An external node connected to a GraphNode, with port mappings
        val externalNode = createTestCodeNode("external", "External", 0.0, 100.0)
        val child1 = createTestCodeNode("child1", "Child1", 0.0, 0.0)

        // Create GraphNode with an input port mapped to child1's input
        val graphNodeInputPort = Port<String>(
            id = "gn_input",
            name = "in_child1_in",
            direction = Port.Direction.INPUT,
            dataType = String::class,
            owningNodeId = "graphNode1"
        )
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(200.0, 100.0),
            childNodes = listOf(child1),
            internalConnections = emptyList(),
            inputPorts = listOf(graphNodeInputPort),
            outputPorts = emptyList(),
            portMappings = mapOf(
                "in_child1_in" to GraphNode.PortMapping(
                    childNodeId = "child1",
                    childPortName = "child1_in"
                )
            )
        )

        // Connection from external to GraphNode's mapped port
        val externalConnection = Connection(
            id = "ext_conn",
            sourceNodeId = "external",
            sourcePortId = "external_out",
            targetNodeId = "graphNode1",
            targetPortId = "in_child1_in"
        )

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(externalNode)
            .addNode(graphNode)
            .addConnection(externalConnection)

        val graphState = GraphState(graph)

        // When: Ungrouping the GraphNode
        graphState.ungroupGraphNode("graphNode1")

        // Then: External connection should be redirected to the original child port
        val restoredConnection = graphState.flowGraph.connections.find { it.id == "ext_conn" }
        assertNotNull(restoredConnection, "External connection should still exist")
        assertEquals("external", restoredConnection.sourceNodeId, "Source should remain external node")
        assertEquals("child1", restoredConnection.targetNodeId, "Target should be redirected to child node")
        assertEquals("child1_in", restoredConnection.targetPortId, "Target port should be child's original port")
    }

    @Test
    fun `ungroupGraphNode should restore outgoing external connections using port mappings`() {
        // Given: A GraphNode connected to an external node via output port mapping
        val child1 = createTestCodeNode("child1", "Child1", 0.0, 0.0)
        val externalNode = createTestCodeNode("external", "External", 300.0, 100.0)

        // Create GraphNode with an output port mapped to child1's output
        val graphNodeOutputPort = Port<String>(
            id = "gn_output",
            name = "out_child1_out",
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
            outputPorts = listOf(graphNodeOutputPort),
            portMappings = mapOf(
                "out_child1_out" to GraphNode.PortMapping(
                    childNodeId = "child1",
                    childPortName = "child1_out"
                )
            )
        )

        // Connection from GraphNode's mapped port to external
        val externalConnection = Connection(
            id = "ext_conn",
            sourceNodeId = "graphNode1",
            sourcePortId = "out_child1_out",
            targetNodeId = "external",
            targetPortId = "external_in"
        )

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)
            .addNode(externalNode)
            .addConnection(externalConnection)

        val graphState = GraphState(graph)

        // When: Ungrouping the GraphNode
        graphState.ungroupGraphNode("graphNode1")

        // Then: External connection should be redirected from the original child port
        val restoredConnection = graphState.flowGraph.connections.find { it.id == "ext_conn" }
        assertNotNull(restoredConnection, "External connection should still exist")
        assertEquals("child1", restoredConnection.sourceNodeId, "Source should be redirected to child node")
        assertEquals("child1_out", restoredConnection.sourcePortId, "Source port should be child's original port")
        assertEquals("external", restoredConnection.targetNodeId, "Target should remain external node")
    }

    @Test
    fun `ungroupGraphNode should handle both incoming and outgoing external connections`() {
        // Given: A GraphNode with both incoming and outgoing external connections
        val sourceExternal = createTestCodeNode("source", "Source", 0.0, 100.0)
        val child1 = createTestCodeNode("child1", "Child1", 0.0, 0.0)
        val child2 = createTestCodeNode("child2", "Child2", 100.0, 0.0)
        val sinkExternal = createTestCodeNode("sink", "Sink", 400.0, 100.0)

        // GraphNode with input and output ports
        val inputPort = Port<String>(
            id = "gn_in",
            name = "in_child1_in",
            direction = Port.Direction.INPUT,
            dataType = String::class,
            owningNodeId = "graphNode1"
        )
        val outputPort = Port<String>(
            id = "gn_out",
            name = "out_child2_out",
            direction = Port.Direction.OUTPUT,
            dataType = String::class,
            owningNodeId = "graphNode1"
        )
        val internalConnection = Connection(
            id = "internal",
            sourceNodeId = "child1",
            sourcePortId = "child1_out",
            targetNodeId = "child2",
            targetPortId = "child2_in"
        )
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(200.0, 100.0),
            childNodes = listOf(child1, child2),
            internalConnections = listOf(internalConnection),
            inputPorts = listOf(inputPort),
            outputPorts = listOf(outputPort),
            portMappings = mapOf(
                "in_child1_in" to GraphNode.PortMapping("child1", "child1_in"),
                "out_child2_out" to GraphNode.PortMapping("child2", "child2_out")
            )
        )

        val incomingConn = Connection(
            id = "incoming",
            sourceNodeId = "source",
            sourcePortId = "source_out",
            targetNodeId = "graphNode1",
            targetPortId = "in_child1_in"
        )
        val outgoingConn = Connection(
            id = "outgoing",
            sourceNodeId = "graphNode1",
            sourcePortId = "out_child2_out",
            targetNodeId = "sink",
            targetPortId = "sink_in"
        )

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(sourceExternal)
            .addNode(graphNode)
            .addNode(sinkExternal)
            .addConnection(incomingConn)
            .addConnection(outgoingConn)

        val graphState = GraphState(graph)

        // When: Ungrouping the GraphNode
        graphState.ungroupGraphNode("graphNode1")

        // Then: All connections should be properly restored
        val connections = graphState.flowGraph.connections

        // Check incoming connection
        val restoredIncoming = connections.find { it.id == "incoming" }
        assertNotNull(restoredIncoming)
        assertEquals("source", restoredIncoming.sourceNodeId)
        assertEquals("child1", restoredIncoming.targetNodeId)
        assertEquals("child1_in", restoredIncoming.targetPortId)

        // Check outgoing connection
        val restoredOutgoing = connections.find { it.id == "outgoing" }
        assertNotNull(restoredOutgoing)
        assertEquals("child2", restoredOutgoing.sourceNodeId)
        assertEquals("child2_out", restoredOutgoing.sourcePortId)
        assertEquals("sink", restoredOutgoing.targetNodeId)

        // Check internal connection
        val restoredInternal = connections.find { it.id == "internal" }
        assertNotNull(restoredInternal)
        assertEquals("child1", restoredInternal.sourceNodeId)
        assertEquals("child2", restoredInternal.targetNodeId)
    }

    @Test
    fun `ungroupGraphNode should select restored child nodes after ungroup`() {
        // Given: A GraphNode with multiple children
        val child1 = createTestCodeNode("child1", "Child1", 0.0, 0.0)
        val child2 = createTestCodeNode("child2", "Child2", 100.0, 0.0)
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
        // Select the GraphNode first
        graphState.toggleNodeInSelection("graphNode1")

        // When: Ungrouping the GraphNode
        graphState.ungroupGraphNode("graphNode1")

        // Then: Restored children should be selected
        val selectedIds = graphState.selectionState.selectedNodeIds
        assertTrue(selectedIds.contains("child1"), "child1 should be selected after ungroup")
        assertTrue(selectedIds.contains("child2"), "child2 should be selected after ungroup")
        assertFalse(selectedIds.contains("graphNode1"), "GraphNode should not be in selection")
    }

    @Test
    fun `ungroupGraphNode should mark graph as dirty`() {
        // Given: A graph with a GraphNode
        val child1 = createTestCodeNode("child1", "Child1", 0.0, 0.0)
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
        graphState.markAsSaved() // Clear dirty flag

        // When: Ungrouping the GraphNode
        graphState.ungroupGraphNode("graphNode1")

        // Then: Graph should be marked as dirty
        assertTrue(graphState.isDirty, "Graph should be marked dirty after ungroup")
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
