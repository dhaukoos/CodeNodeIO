/*
 * Toolbar Group/Ungroup Buttons Tests
 * TDD tests for the Group/Ungroup toolbar button behavior
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
 * TDD tests for Toolbar Group/Ungroup button behavior.
 * Tests T051 for User Story 4 (Ungroup GraphNode via Toolbar).
 *
 * These tests verify the complete ungroup workflow through the toolbar,
 * ensuring the Ungroup button correctly triggers ungroupGraphNode when
 * a single GraphNode is selected.
 */
class ToolbarGroupButtonsTest {

    // ============================================
    // T085: Verify Group button disabled when single node selected
    // ============================================

    @Test
    fun `Group button should be disabled when single node is selected`() {
        // Given: A graph with a CodeNode selected
        val codeNode = createTestCodeNode("node1", "Node1", 100.0, 100.0)
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(codeNode)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("node1")

        // Then: canGroupSelection should return false (only 1 node selected)
        assertFalse(graphState.canGroupSelection(), "Group should be disabled for single node selection")
    }

    @Test
    fun `Group button should be disabled when no nodes are selected`() {
        // Given: A graph with nodes but nothing selected
        val codeNode = createTestCodeNode("node1", "Node1", 100.0, 100.0)
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(codeNode)

        val graphState = GraphState(graph)

        // Then: canGroupSelection should return false (nothing selected)
        assertFalse(graphState.canGroupSelection(), "Group should be disabled when nothing selected")
    }

    @Test
    fun `Group button should be enabled when two or more nodes are selected`() {
        // Given: A graph with two nodes selected
        val node1 = createTestCodeNode("node1", "Node1", 100.0, 100.0)
        val node2 = createTestCodeNode("node2", "Node2", 200.0, 100.0)
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)
            .addNode(node2)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("node1")
        graphState.toggleNodeInSelection("node2")

        // Then: canGroupSelection should return true
        assertTrue(graphState.canGroupSelection(), "Group should be enabled when 2+ nodes selected")
    }

    // ============================================
    // T086: Verify Ungroup button disabled when CodeNode selected
    // (Already covered by existing test: `Ungroup button should be disabled when CodeNode is selected`)
    // ============================================

    // ============================================
    // T051: Tests for Ungroup toolbar button behavior
    // ============================================

    @Test
    fun `Ungroup button should be enabled when single GraphNode is selected`() {
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

        // When: Selecting the GraphNode
        graphState.toggleNodeInSelection("graphNode1")

        // Then: canUngroupSelection should return true
        assertTrue(graphState.canUngroupSelection(), "Ungroup should be enabled for single GraphNode")
    }

    @Test
    fun `Ungroup button should be disabled when no nodes are selected`() {
        // Given: A graph with a GraphNode but nothing selected
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

        // Then: canUngroupSelection should return false (nothing selected)
        assertFalse(graphState.canUngroupSelection(), "Ungroup should be disabled when nothing selected")
    }

    @Test
    fun `Ungroup button should be disabled when CodeNode is selected`() {
        // Given: A graph with a CodeNode selected
        val codeNode = createTestCodeNode("node1", "Node1", 100.0, 100.0)
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(codeNode)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("node1")

        // Then: canUngroupSelection should return false (not a GraphNode)
        assertFalse(graphState.canUngroupSelection(), "Ungroup should be disabled for CodeNode")
    }

    @Test
    fun `Ungroup button should be disabled when multiple nodes are selected`() {
        // Given: A graph with two GraphNodes, both selected
        val child1 = createTestCodeNode("child1", "Child1", 0.0, 0.0)
        val child2 = createTestCodeNode("child2", "Child2", 0.0, 0.0)
        val graphNode1 = GraphNode(
            id = "graphNode1",
            name = "Group1",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(child1),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graphNode2 = GraphNode(
            id = "graphNode2",
            name = "Group2",
            position = Node.Position(300.0, 100.0),
            childNodes = listOf(child2),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode1)
            .addNode(graphNode2)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("graphNode1")
        graphState.toggleNodeInSelection("graphNode2")

        // Then: canUngroupSelection should return false (multiple selection)
        assertFalse(graphState.canUngroupSelection(), "Ungroup should be disabled for multiple selection")
    }

    @Test
    fun `clicking Ungroup button should restore child nodes to canvas`() {
        // Given: A GraphNode with children is selected
        val child1 = createTestCodeNode("child1", "Child1", 0.0, 0.0)
        val child2 = createTestCodeNode("child2", "Child2", 100.0, 0.0)
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
        graphState.toggleNodeInSelection("graphNode1")
        assertTrue(graphState.canUngroupSelection())

        // When: Simulating Ungroup button click (calls ungroupGraphNode)
        val selectedId = graphState.selectionState.selectedNodeIds.firstOrNull()
        assertNotNull(selectedId)
        val success = graphState.ungroupGraphNode(selectedId)

        // Then: Children should be restored
        assertTrue(success, "Ungroup operation should succeed")
        val rootNodeIds = graphState.flowGraph.rootNodes.map { it.id }
        assertTrue(rootNodeIds.contains("child1"))
        assertTrue(rootNodeIds.contains("child2"))
        assertFalse(rootNodeIds.contains("graphNode1"))
    }

    @Test
    fun `Ungroup operation should restore connections between restored nodes`() {
        // Given: A GraphNode with internal connections
        val child1 = createTestCodeNode("child1", "Child1", 0.0, 0.0)
        val child2 = createTestCodeNode("child2", "Child2", 100.0, 0.0)
        val internalConn = Connection(
            id = "conn1",
            sourceNodeId = "child1",
            sourcePortId = "child1_out",
            targetNodeId = "child2",
            targetPortId = "child2_in"
        )
        val graphNode = GraphNode(
            id = "graphNode1",
            name = "TestGroup",
            position = Node.Position(150.0, 100.0),
            childNodes = listOf(child1, child2),
            internalConnections = listOf(internalConn),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("graphNode1")

        // When: Ungrouping
        graphState.ungroupGraphNode("graphNode1")

        // Then: Internal connection should be in root graph connections
        val conn = graphState.flowGraph.connections.find { it.id == "conn1" }
        assertNotNull(conn, "Internal connection should be restored to root graph")
    }

    @Test
    fun `Ungroup should redirect external connections to restored child nodes`() {
        // Given: External node connected to GraphNode with port mapping
        val externalNode = createTestCodeNode("external", "External", 0.0, 100.0)
        val child1 = createTestCodeNode("child1", "Child1", 0.0, 0.0)

        val inputPort = Port<String>(
            id = "gn_in",
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
            inputPorts = listOf(inputPort),
            outputPorts = emptyList(),
            portMappings = mapOf(
                "in_child1_in" to GraphNode.PortMapping("child1", "child1_in")
            )
        )

        // Note: Connection uses port ID ("gn_in"), not port name ("in_child1_in")
        val extConn = Connection(
            id = "ext_conn",
            sourceNodeId = "external",
            sourcePortId = "external_out",
            targetNodeId = "graphNode1",
            targetPortId = "gn_in"
        )

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(externalNode)
            .addNode(graphNode)
            .addConnection(extConn)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("graphNode1")

        // When: Ungrouping
        graphState.ungroupGraphNode("graphNode1")

        // Then: External connection should be redirected to child node
        val conn = graphState.flowGraph.connections.find { it.id == "ext_conn" }
        assertNotNull(conn)
        assertEquals("external", conn.sourceNodeId)
        assertEquals("child1", conn.targetNodeId, "Connection should be redirected to child node")
        assertEquals("child1_in", conn.targetPortId, "Connection should target original child port")
    }

    @Test
    fun `Group then Ungroup should restore original node structure`() {
        // Given: Two nodes with a connection between them
        val node1 = createTestCodeNode("node1", "Node1", 100.0, 100.0)
        val node2 = createTestCodeNode("node2", "Node2", 200.0, 100.0)
        val connection = Connection(
            id = "conn1",
            sourceNodeId = "node1",
            sourcePortId = "node1_out",
            targetNodeId = "node2",
            targetPortId = "node2_in"
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)
            .addNode(node2)
            .addConnection(connection)

        val graphState = GraphState(graph)

        // Step 1: Select both nodes
        graphState.toggleNodeInSelection("node1")
        graphState.toggleNodeInSelection("node2")
        assertTrue(graphState.canGroupSelection())

        // Step 2: Group them
        val graphNode = graphState.groupSelectedNodes()
        assertNotNull(graphNode)
        assertEquals(1, graphState.flowGraph.rootNodes.size, "Should have one GraphNode")

        // Step 3: Ungroup
        assertTrue(graphState.canUngroupSelection(), "Should be able to ungroup the newly created GraphNode")
        val success = graphState.ungroupGraphNode(graphNode.id)
        assertTrue(success)

        // Then: Original structure should be restored
        assertEquals(2, graphState.flowGraph.rootNodes.size, "Should have two nodes again")
        val restoredConn = graphState.flowGraph.connections.find { it.id == "conn1" }
        assertNotNull(restoredConn, "Original connection should be restored")
        assertEquals("node1", restoredConn.sourceNodeId)
        assertEquals("node2", restoredConn.targetNodeId)
    }

    @Test
    fun `Ungroup should work correctly when navigated at root level`() {
        // Given: A GraphNode at root level
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

        // Verify we're at root
        assertTrue(graphState.navigationContext.isAtRoot)

        // Select and ungroup
        graphState.toggleNodeInSelection("graphNode1")
        graphState.ungroupGraphNode("graphNode1")

        // Should work at root level
        assertTrue(graphState.flowGraph.rootNodes.any { it.id == "child1" })
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
