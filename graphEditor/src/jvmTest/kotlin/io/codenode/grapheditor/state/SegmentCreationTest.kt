/*
 * SegmentCreationTest - Integration Tests for Automatic Segment Creation
 * Tests that grouping/ungrouping automatically creates/merges connection segments
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Integration tests for automatic segment creation during group/ungroup operations.
 *
 * User Story 4: Automatic Segment Creation When Grouping
 * - Grouping automatically splits connections into segments at boundaries
 * - Ungrouping merges segments back to direct connections
 */
class SegmentCreationTest {

    /**
     * Helper to create a test CodeNode with typed ports.
     */
    private fun createTestCodeNode(
        id: String,
        name: String,
        x: Double,
        y: Double,
        hasInputPort: Boolean = true,
        hasOutputPort: Boolean = true
    ): CodeNode {
        val inputPorts = if (hasInputPort) {
            listOf(
                Port(
                    id = "${id}_input",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = id
                )
            )
        } else emptyList()

        val outputPorts = if (hasOutputPort) {
            listOf(
                Port(
                    id = "${id}_output",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = id
                )
            )
        } else emptyList()

        return CodeNode(
            id = id,
            name = name,
            description = "Test node",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(x, y),
            inputPorts = inputPorts,
            outputPorts = outputPorts
        )
    }

    // ==================== T042: Incoming Connection Segmentation ====================

    @Test
    fun `grouping nodes creates segments for incoming external connections`() {
        // Given: A flow A -> B -> C where we will group B and C
        val nodeA = createTestCodeNode("nodeA", "A", 0.0, 0.0, hasInputPort = false)
        val nodeB = createTestCodeNode("nodeB", "B", 100.0, 0.0)
        val nodeC = createTestCodeNode("nodeC", "C", 200.0, 0.0, hasOutputPort = false)

        val graph = flowGraph(name = "test", version = "1.0.0") {}
            .addNode(nodeA)
            .addNode(nodeB)
            .addNode(nodeC)
            .let { g ->
                g.copy(connections = listOf(
                    Connection(
                        id = "conn1",
                        sourceNodeId = "nodeA",
                        sourcePortId = "nodeA_output",
                        targetNodeId = "nodeB",
                        targetPortId = "nodeB_input"
                    ),
                    Connection(
                        id = "conn2",
                        sourceNodeId = "nodeB",
                        sourcePortId = "nodeB_output",
                        targetNodeId = "nodeC",
                        targetPortId = "nodeC_input"
                    )
                ))
            }

        // Create GraphState and select nodes B and C
        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("nodeB")
        graphState.toggleNodeInSelection("nodeC")

        assertTrue(graphState.canGroupSelection(), "Should be able to group 2 nodes")

        // When: Group the selected nodes
        val groupNode = graphState.groupSelectedNodes()

        // Then: A GraphNode should be created
        assertNotNull(groupNode, "GraphNode should be created")

        // And: The connection from A to the group should exist
        val connectionsToGroup = graphState.flowGraph.connections.filter {
            it.targetNodeId == groupNode.id
        }
        assertEquals(1, connectionsToGroup.size, "Should have 1 incoming connection to the group")

        // And: The connection should have segments (at least 1)
        val incomingConnection = connectionsToGroup.first()
        assertTrue(incomingConnection.segments.isNotEmpty(), "Connection should have segments")
    }

    @Test
    fun `incoming connection to grouped nodes has proper segment structure`() {
        // Given: A flow where external node connects to a node that will be grouped
        val external = createTestCodeNode("external", "External", 0.0, 0.0, hasInputPort = false)
        val internal1 = createTestCodeNode("internal1", "Internal1", 100.0, 0.0)
        val internal2 = createTestCodeNode("internal2", "Internal2", 200.0, 0.0, hasOutputPort = false)

        val graph = flowGraph(name = "test", version = "1.0.0") {}
            .addNode(external)
            .addNode(internal1)
            .addNode(internal2)
            .let { g ->
                g.copy(connections = listOf(
                    Connection(
                        id = "conn1",
                        sourceNodeId = "external",
                        sourcePortId = "external_output",
                        targetNodeId = "internal1",
                        targetPortId = "internal1_input"
                    ),
                    Connection(
                        id = "conn2",
                        sourceNodeId = "internal1",
                        sourcePortId = "internal1_output",
                        targetNodeId = "internal2",
                        targetPortId = "internal2_input"
                    )
                ))
            }

        val graphState = GraphState(graph)

        // Select Internal1 and Internal2 for grouping
        graphState.toggleNodeInSelection("internal1")
        graphState.toggleNodeInSelection("internal2")

        // Group the nodes
        val groupNode = graphState.groupSelectedNodes()
        assertNotNull(groupNode)

        // Verify the connection from External to GroupNode exists
        val connectionsFromExternal = graphState.flowGraph.connections.filter {
            it.sourceNodeId == "external" && it.targetNodeId == groupNode.id
        }

        assertEquals(1, connectionsFromExternal.size, "Should have connection from External to GroupNode")
    }

    @Test
    fun `grouping single node is not allowed`() {
        // Given: A flow A -> B -> C where we try to group just B
        val nodeA = createTestCodeNode("nodeA", "A", 0.0, 0.0, hasInputPort = false)
        val nodeB = createTestCodeNode("nodeB", "B", 100.0, 0.0)
        val nodeC = createTestCodeNode("nodeC", "C", 200.0, 0.0, hasOutputPort = false)

        val graph = flowGraph(name = "test", version = "1.0.0") {}
            .addNode(nodeA)
            .addNode(nodeB)
            .addNode(nodeC)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("nodeB")

        // Single node cannot be grouped
        assertFalse(graphState.canGroupSelection())
    }

    // ==================== T043: Outgoing Connection Segmentation ====================

    @Test
    fun `grouping nodes creates segments for outgoing external connections`() {
        // Given: A flow A -> B -> C where we will group A and B
        val nodeA = createTestCodeNode("nodeA", "A", 0.0, 0.0, hasInputPort = false)
        val nodeB = createTestCodeNode("nodeB", "B", 100.0, 0.0)
        val nodeC = createTestCodeNode("nodeC", "C", 200.0, 0.0, hasOutputPort = false)

        val graph = flowGraph(name = "test", version = "1.0.0") {}
            .addNode(nodeA)
            .addNode(nodeB)
            .addNode(nodeC)
            .let { g ->
                g.copy(connections = listOf(
                    Connection(
                        id = "conn1",
                        sourceNodeId = "nodeA",
                        sourcePortId = "nodeA_output",
                        targetNodeId = "nodeB",
                        targetPortId = "nodeB_input"
                    ),
                    Connection(
                        id = "conn2",
                        sourceNodeId = "nodeB",
                        sourcePortId = "nodeB_output",
                        targetNodeId = "nodeC",
                        targetPortId = "nodeC_input"
                    )
                ))
            }

        val graphState = GraphState(graph)

        // Select nodes A and B for grouping
        graphState.toggleNodeInSelection("nodeA")
        graphState.toggleNodeInSelection("nodeB")

        assertTrue(graphState.canGroupSelection())

        // When: Group the selected nodes
        val groupNode = graphState.groupSelectedNodes()

        // Then: The connection from the group to C should exist
        assertNotNull(groupNode)
        val connectionsFromGroup = graphState.flowGraph.connections.filter {
            it.sourceNodeId == groupNode.id
        }
        assertEquals(1, connectionsFromGroup.size, "Should have 1 outgoing connection from the group")

        // And: The connection should have segments
        val outgoingConnection = connectionsFromGroup.first()
        assertTrue(outgoingConnection.segments.isNotEmpty(), "Connection should have segments")
    }

    @Test
    fun `outgoing connection from grouped nodes targets correct external node`() {
        // Given: A flow where grouped node outputs to external node
        val internal1 = createTestCodeNode("internal1", "Internal1", 0.0, 0.0, hasInputPort = false)
        val internal2 = createTestCodeNode("internal2", "Internal2", 100.0, 0.0)
        val external = createTestCodeNode("external", "External", 200.0, 0.0, hasOutputPort = false)

        val graph = flowGraph(name = "test", version = "1.0.0") {}
            .addNode(internal1)
            .addNode(internal2)
            .addNode(external)
            .let { g ->
                g.copy(connections = listOf(
                    Connection(
                        id = "conn1",
                        sourceNodeId = "internal1",
                        sourcePortId = "internal1_output",
                        targetNodeId = "internal2",
                        targetPortId = "internal2_input"
                    ),
                    Connection(
                        id = "conn2",
                        sourceNodeId = "internal2",
                        sourcePortId = "internal2_output",
                        targetNodeId = "external",
                        targetPortId = "external_input"
                    )
                ))
            }

        val graphState = GraphState(graph)

        // Select Internal1 and Internal2 for grouping
        graphState.toggleNodeInSelection("internal1")
        graphState.toggleNodeInSelection("internal2")

        // Group the nodes
        val groupNode = graphState.groupSelectedNodes()
        assertNotNull(groupNode)

        // Verify the connection from GroupNode to External exists
        val connectionsToExternal = graphState.flowGraph.connections.filter {
            it.sourceNodeId == groupNode.id && it.targetNodeId == "external"
        }

        assertEquals(1, connectionsToExternal.size, "Should have connection from GroupNode to External")
    }

    @Test
    fun `grouping creates both input and output PassThruPorts when needed`() {
        // Given: A flow A -> B -> B2 -> C where B and B2 will be grouped
        val nodeA = createTestCodeNode("nodeA", "A", 0.0, 0.0, hasInputPort = false)
        val nodeB = createTestCodeNode("nodeB", "B", 100.0, 0.0)
        val nodeB2 = createTestCodeNode("nodeB2", "B2", 200.0, 0.0)
        val nodeC = createTestCodeNode("nodeC", "C", 300.0, 0.0, hasOutputPort = false)

        val graph = flowGraph(name = "test", version = "1.0.0") {}
            .addNode(nodeA)
            .addNode(nodeB)
            .addNode(nodeB2)
            .addNode(nodeC)
            .let { g ->
                g.copy(connections = listOf(
                    Connection(
                        id = "conn1",
                        sourceNodeId = "nodeA",
                        sourcePortId = "nodeA_output",
                        targetNodeId = "nodeB",
                        targetPortId = "nodeB_input"
                    ),
                    Connection(
                        id = "conn2",
                        sourceNodeId = "nodeB",
                        sourcePortId = "nodeB_output",
                        targetNodeId = "nodeB2",
                        targetPortId = "nodeB2_input"
                    ),
                    Connection(
                        id = "conn3",
                        sourceNodeId = "nodeB2",
                        sourcePortId = "nodeB2_output",
                        targetNodeId = "nodeC",
                        targetPortId = "nodeC_input"
                    )
                ))
            }

        val graphState = GraphState(graph)

        // Select B and B2 for grouping (middle of the flow)
        graphState.toggleNodeInSelection("nodeB")
        graphState.toggleNodeInSelection("nodeB2")

        // Group the nodes
        val groupNode = graphState.groupSelectedNodes()
        assertNotNull(groupNode)

        // Verify GraphNode has both input and output ports
        assertTrue(groupNode.inputPorts.isNotEmpty(), "GroupNode should have input ports")
        assertTrue(groupNode.outputPorts.isNotEmpty(), "GroupNode should have output ports")
    }

    // ==================== T044: Segment Merging on Ungroup ====================

    @Test
    fun `ungrouping restores child nodes to root level`() {
        // Given: A flow A -> B -> C where B and C are grouped
        val nodeA = createTestCodeNode("nodeA", "A", 0.0, 0.0, hasInputPort = false)
        val nodeB = createTestCodeNode("nodeB", "B", 100.0, 0.0)
        val nodeC = createTestCodeNode("nodeC", "C", 200.0, 0.0, hasOutputPort = false)

        val graph = flowGraph(name = "test", version = "1.0.0") {}
            .addNode(nodeA)
            .addNode(nodeB)
            .addNode(nodeC)
            .let { g ->
                g.copy(connections = listOf(
                    Connection(
                        id = "conn1",
                        sourceNodeId = "nodeA",
                        sourcePortId = "nodeA_output",
                        targetNodeId = "nodeB",
                        targetPortId = "nodeB_input"
                    ),
                    Connection(
                        id = "conn2",
                        sourceNodeId = "nodeB",
                        sourcePortId = "nodeB_output",
                        targetNodeId = "nodeC",
                        targetPortId = "nodeC_input"
                    )
                ))
            }

        val graphState = GraphState(graph)

        // Select and group B and C
        graphState.toggleNodeInSelection("nodeB")
        graphState.toggleNodeInSelection("nodeC")

        val groupNode = graphState.groupSelectedNodes()
        assertNotNull(groupNode)

        // Verify we have a GroupNode (2 nodes: A and GroupNode)
        assertEquals(2, graphState.flowGraph.rootNodes.size, "Should have A and GroupNode at root")

        // When: Ungroup the GraphNode
        val ungroupSuccess = graphState.ungroupGraphNode(groupNode.id)
        assertTrue(ungroupSuccess, "Ungroup should succeed")

        // Then: Original nodes should be restored to root level
        assertEquals(3, graphState.flowGraph.rootNodes.size, "Should have A, B, C at root after ungroup")

        // Verify that B and C are back at root level
        val restoredNodeB = graphState.flowGraph.rootNodes.find { it.name == "B" }
        val restoredNodeC = graphState.flowGraph.rootNodes.find { it.name == "C" }
        assertNotNull(restoredNodeB, "Node B should be restored to root level")
        assertNotNull(restoredNodeC, "Node C should be restored to root level")
    }

    @Test
    fun `ungrouping restores internal connections to root level`() {
        // Given: A grouped flow with internal connections
        val nodeA = createTestCodeNode("nodeA", "A", 0.0, 0.0, hasInputPort = false)
        val nodeB = createTestCodeNode("nodeB", "B", 100.0, 0.0)
        val nodeC = createTestCodeNode("nodeC", "C", 200.0, 0.0, hasOutputPort = false)

        val graph = flowGraph(name = "test", version = "1.0.0") {}
            .addNode(nodeA)
            .addNode(nodeB)
            .addNode(nodeC)
            .let { g ->
                g.copy(connections = listOf(
                    Connection(
                        id = "conn1",
                        sourceNodeId = "nodeA",
                        sourcePortId = "nodeA_output",
                        targetNodeId = "nodeB",
                        targetPortId = "nodeB_input"
                    ),
                    Connection(
                        id = "conn2",
                        sourceNodeId = "nodeB",
                        sourcePortId = "nodeB_output",
                        targetNodeId = "nodeC",
                        targetPortId = "nodeC_input"
                    )
                ))
            }

        val graphState = GraphState(graph)

        // Group B and C
        graphState.toggleNodeInSelection("nodeB")
        graphState.toggleNodeInSelection("nodeC")

        val groupNode = graphState.groupSelectedNodes()
        assertNotNull(groupNode)

        // Verify internal connection exists in the group
        assertEquals(1, groupNode.internalConnections.size, "Group should have 1 internal connection")
        val internalConn = groupNode.internalConnections.first()
        assertEquals("nodeB", internalConn.sourceNodeId)
        assertEquals("nodeC", internalConn.targetNodeId)

        // Ungroup
        graphState.ungroupGraphNode(groupNode.id)

        // Verify internal connection (B->C) is now at root level
        val restoredBtoC = graphState.flowGraph.connections.find { conn ->
            conn.sourceNodeId == "nodeB" && conn.targetNodeId == "nodeC"
        }
        assertNotNull(restoredBtoC, "Internal connection B->C should be restored to root level")
    }

    @Test
    fun `ungrouping a non-existent node returns false`() {
        val graph = flowGraph(name = "test", version = "1.0.0") {}
        val graphState = GraphState(graph)

        val result = graphState.ungroupGraphNode("non-existent-id")
        assertEquals(false, result)
    }

    @Test
    fun `ungrouping a CodeNode returns false`() {
        val nodeA = createTestCodeNode("nodeA", "A", 0.0, 0.0)
        val graph = flowGraph(name = "test", version = "1.0.0") {}
            .addNode(nodeA)

        val graphState = GraphState(graph)

        val result = graphState.ungroupGraphNode("nodeA")
        assertEquals(false, result)
    }

    // ==================== Helper Tests ====================

    @Test
    fun `canGroupSelection returns false for single node`() {
        val nodeA = createTestCodeNode("nodeA", "A", 0.0, 0.0)
        val graph = flowGraph(name = "test", version = "1.0.0") {}
            .addNode(nodeA)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("nodeA")

        assertEquals(false, graphState.canGroupSelection())
    }

    @Test
    fun `canGroupSelection returns true for multiple nodes`() {
        val nodeA = createTestCodeNode("nodeA", "A", 0.0, 0.0)
        val nodeB = createTestCodeNode("nodeB", "B", 100.0, 0.0)
        val graph = flowGraph(name = "test", version = "1.0.0") {}
            .addNode(nodeA)
            .addNode(nodeB)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("nodeA")
        graphState.toggleNodeInSelection("nodeB")

        assertEquals(true, graphState.canGroupSelection())
    }

    @Test
    fun `connection segments property is not empty after grouping`() {
        // Given: A simple flow A -> B
        val nodeA = createTestCodeNode("nodeA", "A", 0.0, 0.0, hasInputPort = false)
        val nodeB = createTestCodeNode("nodeB", "B", 100.0, 0.0, hasOutputPort = false)

        val graph = flowGraph(name = "test", version = "1.0.0") {}
            .addNode(nodeA)
            .addNode(nodeB)
            .let { g ->
                g.copy(connections = listOf(
                    Connection(
                        id = "conn1",
                        sourceNodeId = "nodeA",
                        sourcePortId = "nodeA_output",
                        targetNodeId = "nodeB",
                        targetPortId = "nodeB_input"
                    )
                ))
            }

        // Verify direct connection has segments
        val conn = graph.connections.first()
        assertTrue(conn.segments.isNotEmpty(), "Connection should have at least one segment")
        assertEquals(1, conn.segments.size, "Direct connection should have exactly 1 segment")
    }

    @Test
    fun `connection segment chain is valid for direct connection`() {
        // Given: A direct connection
        val connection = Connection(
            id = "conn1",
            sourceNodeId = "nodeA",
            sourcePortId = "portA",
            targetNodeId = "nodeB",
            targetPortId = "portB"
        )

        // When: Validating segment chain
        val validation = connection.validateSegmentChain()

        // Then: Should be valid
        assertTrue(validation.success, "Segment chain should be valid: ${validation.errors}")
    }
}
