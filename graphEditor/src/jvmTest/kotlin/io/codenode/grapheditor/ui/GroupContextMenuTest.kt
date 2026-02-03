/*
 * Group/Ungroup Tests
 * TDD tests for the Group/Ungroup toolbar button functionality
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.ui.geometry.Offset
import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.grapheditor.state.GraphState
import kotlin.test.*

/**
 * TDD tests for Group/Ungroup toolbar functionality.
 * Tests the canGroupSelection/canUngroupSelection helpers and groupSelectedNodes method.
 */
class GroupContextMenuTest {

    // ============================================
    // Tests for canGroupSelection()
    // ============================================

    @Test
    fun `canGroupSelection returns true when 2 or more nodes selected`() {
        // Given: 2 nodes selected
        val node1 = createTestCodeNode("node1", "Node1", 100.0, 100.0)
        val node2 = createTestCodeNode("node2", "Node2", 200.0, 100.0)
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)
            .addNode(node2)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("node1")
        graphState.toggleNodeInSelection("node2")

        // Then: canGroupSelection should return true
        assertTrue(graphState.canGroupSelection())
    }

    @Test
    fun `canGroupSelection returns false when fewer than 2 nodes selected`() {
        // Given: Only 1 node selected
        val node1 = createTestCodeNode("node1", "Node1", 100.0, 100.0)
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("node1")

        // Then: canGroupSelection should return false
        assertFalse(graphState.canGroupSelection())
    }

    @Test
    fun `canGroupSelection returns false when no nodes selected`() {
        // Given: No nodes selected
        val node1 = createTestCodeNode("node1", "Node1", 100.0, 100.0)
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)

        val graphState = GraphState(graph)

        // Then: canGroupSelection should return false
        assertFalse(graphState.canGroupSelection())
    }

    // ============================================
    // Tests for canUngroupSelection()
    // ============================================

    @Test
    fun `canUngroupSelection returns true when single GraphNode selected`() {
        // Given: A single GraphNode is selected
        val childNode = createTestCodeNode("child1", "Child1", 0.0, 0.0)
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(childNode),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("graphNode1")

        // Then: canUngroupSelection should return true
        assertTrue(graphState.canUngroupSelection())
    }

    @Test
    fun `canUngroupSelection returns false when multiple nodes selected including GraphNode`() {
        // Given: Multiple nodes selected including a GraphNode
        val childNode = createTestCodeNode("child1", "Child1", 0.0, 0.0)
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(childNode),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val codeNode = createTestCodeNode("node2", "Node2", 200.0, 100.0)
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)
            .addNode(codeNode)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("graphNode1")
        graphState.toggleNodeInSelection("node2")

        // Then: canUngroupSelection should return false (multiple selection)
        assertFalse(graphState.canUngroupSelection())
    }

    @Test
    fun `canUngroupSelection returns false when CodeNode selected`() {
        // Given: A CodeNode (not GraphNode) selected
        val node1 = createTestCodeNode("node1", "Node1", 100.0, 100.0)
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("node1")

        // Then: canUngroupSelection should return false
        assertFalse(graphState.canUngroupSelection())
    }

    @Test
    fun `canUngroupSelection returns false when no nodes selected`() {
        // Given: No nodes selected
        val node1 = createTestCodeNode("node1", "Node1", 100.0, 100.0)
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)

        val graphState = GraphState(graph)

        // Then: canUngroupSelection should return false
        assertFalse(graphState.canUngroupSelection())
    }

    // ============================================
    // Tests for groupSelectedNodes()
    // ============================================

    @Test
    fun `groupSelectedNodes should create GraphNode from selected nodes`() {
        // Given: A graph with multiple nodes selected
        val node1 = createTestCodeNode("node1", "Node1", 100.0, 100.0)
        val node2 = createTestCodeNode("node2", "Node2", 200.0, 100.0)
        val node3 = createTestCodeNode("node3", "Node3", 300.0, 100.0)  // Not selected
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)
            .addNode(node2)
            .addNode(node3)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("node1")
        graphState.toggleNodeInSelection("node2")

        // When: Grouping selected nodes
        val createdGraphNode = graphState.groupSelectedNodes()

        // Then: GraphNode should be created
        assertNotNull(createdGraphNode)
        assertEquals(2, createdGraphNode.childNodes.size)

        // Original nodes should be removed from root
        val rootNodeIds = graphState.flowGraph.rootNodes.map { it.id }
        assertFalse(rootNodeIds.contains("node1"))
        assertFalse(rootNodeIds.contains("node2"))
        assertTrue(rootNodeIds.contains("node3"))  // Unselected node still there
        assertTrue(rootNodeIds.contains(createdGraphNode.id))  // New GraphNode added
    }

    @Test
    fun `groupSelectedNodes should preserve internal connections`() {
        // Given: Selected nodes with connection between them
        val node1 = createTestCodeNode("node1", "Node1", 100.0, 100.0)
        val node2 = createTestCodeNode("node2", "Node2", 200.0, 100.0)
        val internalConnection = Connection(
            id = "conn1",
            sourceNodeId = "node1",
            sourcePortId = "node1_out",
            targetNodeId = "node2",
            targetPortId = "node2_in"
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)
            .addNode(node2)
            .addConnection(internalConnection)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("node1")
        graphState.toggleNodeInSelection("node2")

        // When: Grouping selected nodes
        val createdGraphNode = graphState.groupSelectedNodes()

        // Then: Internal connection should be preserved in GraphNode
        assertNotNull(createdGraphNode)
        assertEquals(1, createdGraphNode.internalConnections.size)
        assertEquals("conn1", createdGraphNode.internalConnections[0].id)

        // Connection should be removed from root graph
        assertFalse(graphState.flowGraph.connections.any { it.id == "conn1" })
    }

    @Test
    fun `groupSelectedNodes should generate port mappings for external connections`() {
        // Given: External node connected to selected nodes
        val externalNode = createTestCodeNode("external", "External", 0.0, 100.0)
        val node1 = createTestCodeNode("node1", "Node1", 100.0, 100.0)
        val node2 = createTestCodeNode("node2", "Node2", 200.0, 100.0)
        val externalConnection = Connection(
            id = "ext_conn",
            sourceNodeId = "external",
            sourcePortId = "external_out",
            targetNodeId = "node1",
            targetPortId = "node1_in"
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(externalNode)
            .addNode(node1)
            .addNode(node2)
            .addConnection(externalConnection)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("node1")
        graphState.toggleNodeInSelection("node2")

        // When: Grouping selected nodes
        val createdGraphNode = graphState.groupSelectedNodes()

        // Then: GraphNode should have input port with mapping
        assertNotNull(createdGraphNode)
        assertTrue(createdGraphNode.inputPorts.isNotEmpty())
        assertTrue(createdGraphNode.portMappings.isNotEmpty())

        // External connection should be redirected to GraphNode
        val updatedConnection = graphState.flowGraph.connections.find { it.id == "ext_conn" }
        assertNotNull(updatedConnection)
        assertEquals(createdGraphNode.id, updatedConnection.targetNodeId)
    }

    @Test
    fun `groupSelectedNodes should fail silently when fewer than 2 nodes selected`() {
        // Given: Only one node selected
        val node1 = createTestCodeNode("node1", "Node1", 100.0, 100.0)
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("node1")

        // When: Attempting to group
        val result = graphState.groupSelectedNodes()

        // Then: Should return null
        assertNull(result)
        // Graph should be unchanged
        assertEquals(1, graphState.flowGraph.rootNodes.size)
    }

    @Test
    fun `groupSelectedNodes should clear selection after grouping`() {
        // Given: Multiple nodes selected
        val node1 = createTestCodeNode("node1", "Node1", 100.0, 100.0)
        val node2 = createTestCodeNode("node2", "Node2", 200.0, 100.0)
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)
            .addNode(node2)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("node1")
        graphState.toggleNodeInSelection("node2")
        assertEquals(2, graphState.selectionState.nodeSelectionCount)

        // When: Grouping selected nodes
        val createdGraphNode = graphState.groupSelectedNodes()

        // Then: Selection should be cleared and new GraphNode selected
        assertNotNull(createdGraphNode)
        assertEquals(1, graphState.selectionState.nodeSelectionCount)
        assertTrue(graphState.selectionState.containsNode(createdGraphNode.id))
    }

    @Test
    fun `groupSelectedNodes should handle connections in both directions`() {
        // Given: External nodes connected to and from selected nodes
        val sourceExternal = createTestCodeNode("source", "Source", 0.0, 100.0)
        val node1 = createTestCodeNode("node1", "Node1", 100.0, 100.0)
        val node2 = createTestCodeNode("node2", "Node2", 200.0, 100.0)
        val sinkExternal = createTestCodeNode("sink", "Sink", 300.0, 100.0)

        val incomingConn = Connection(
            id = "incoming",
            sourceNodeId = "source",
            sourcePortId = "source_out",
            targetNodeId = "node1",
            targetPortId = "node1_in"
        )
        val outgoingConn = Connection(
            id = "outgoing",
            sourceNodeId = "node2",
            sourcePortId = "node2_out",
            targetNodeId = "sink",
            targetPortId = "sink_in"
        )
        val internalConn = Connection(
            id = "internal",
            sourceNodeId = "node1",
            sourcePortId = "node1_out",
            targetNodeId = "node2",
            targetPortId = "node2_in"
        )

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(sourceExternal)
            .addNode(node1)
            .addNode(node2)
            .addNode(sinkExternal)
            .addConnection(incomingConn)
            .addConnection(internalConn)
            .addConnection(outgoingConn)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("node1")
        graphState.toggleNodeInSelection("node2")

        // When: Grouping selected nodes
        val createdGraphNode = graphState.groupSelectedNodes()

        // Then: GraphNode should have both input and output ports
        assertNotNull(createdGraphNode)
        assertEquals(1, createdGraphNode.inputPorts.size)
        assertEquals(1, createdGraphNode.outputPorts.size)
        assertEquals(1, createdGraphNode.internalConnections.size)
    }

    // ============================================
    // Integration test: full group then ungroup flow
    // ============================================

    @Test
    fun `canUngroupSelection returns true immediately after groupSelectedNodes`() {
        // Given: Two nodes selected
        val node1 = createTestCodeNode("node1", "Node1", 100.0, 100.0)
        val node2 = createTestCodeNode("node2", "Node2", 200.0, 100.0)
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)
            .addNode(node2)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("node1")
        graphState.toggleNodeInSelection("node2")

        // Verify canGroup is true, canUngroup is false before grouping
        assertTrue(graphState.canGroupSelection(), "canGroupSelection should be true before grouping")
        assertFalse(graphState.canUngroupSelection(), "canUngroupSelection should be false before grouping")

        // When: Grouping the selected nodes
        val createdGraphNode = graphState.groupSelectedNodes()

        // Then: canUngroup should be true immediately after grouping
        assertNotNull(createdGraphNode, "groupSelectedNodes should return a GraphNode")
        assertEquals(1, graphState.selectionState.selectedNodeIds.size, "Exactly one node should be selected")
        assertTrue(graphState.selectionState.selectedNodeIds.contains(createdGraphNode.id),
            "The created GraphNode should be selected")

        // Verify the GraphNode is in the flow graph
        val foundNode = graphState.flowGraph.findNode(createdGraphNode.id)
        assertNotNull(foundNode, "GraphNode should be findable in the flow graph")
        assertTrue(foundNode is GraphNode, "Found node should be a GraphNode instance")

        // The key assertion: canUngroupSelection should return true
        assertTrue(graphState.canUngroupSelection(),
            "canUngroupSelection should be true immediately after grouping")

        // Also verify canGroup is false (only one node selected)
        assertFalse(graphState.canGroupSelection(),
            "canGroupSelection should be false after grouping (single node selected)")
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
