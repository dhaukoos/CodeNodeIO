/*
 * EdgeCaseTest - Edge Case Tests for PassThruPort and ConnectionSegment
 * Tests unusual scenarios and boundary conditions for robust behavior
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Edge case tests for PassThruPort and ConnectionSegment feature.
 *
 * T068: Verify edge case: PassThruPort upstream/downstream port deletion
 * T069: Verify edge case: self-loop connections stay single-segment
 */
class EdgeCaseTest {

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

    // ==================== T068: PassThruPort upstream/downstream port deletion ====================

    @Test
    fun `removing external node that connects to PassThruPort removes the connection`() {
        // Given: A flow A -> [Group containing B] where A is external
        val nodeA = createTestCodeNode("nodeA", "A", 0.0, 0.0, hasInputPort = false)
        val nodeB = createTestCodeNode("nodeB", "B", 100.0, 0.0, hasOutputPort = false)
        val nodeC = createTestCodeNode("nodeC", "C", 200.0, 0.0)

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
                    )
                ))
            }

        val graphState = GraphState(graph)

        // Group B and C
        graphState.toggleNodeInSelection("nodeB")
        graphState.toggleNodeInSelection("nodeC")
        val groupNode = graphState.groupSelectedNodes()
        assertNotNull(groupNode)

        // Verify connection from A to Group exists
        val connectionsToGroup = graphState.flowGraph.connections.filter {
            it.targetNodeId == groupNode.id
        }
        assertEquals(1, connectionsToGroup.size)

        // When: Remove external node A
        graphState.removeNode("nodeA")

        // Then: The connection to the Group should also be removed
        val remainingConnections = graphState.flowGraph.connections
        assertEquals(0, remainingConnections.size, "Connection should be removed when external node is deleted")
    }

    @Test
    fun `removing node connected to PassThruPort downstream removes the connection`() {
        // Given: A flow [Group containing A] -> B where B is external
        val nodeA = createTestCodeNode("nodeA", "A", 0.0, 0.0, hasInputPort = false)
        val nodeB = createTestCodeNode("nodeB", "B", 100.0, 0.0, hasOutputPort = false)
        val nodeC = createTestCodeNode("nodeC", "C", 50.0, 50.0)

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
                    )
                ))
            }

        val graphState = GraphState(graph)

        // Group A and C (nodeA has outgoing connection to nodeB)
        graphState.toggleNodeInSelection("nodeA")
        graphState.toggleNodeInSelection("nodeC")
        val groupNode = graphState.groupSelectedNodes()
        assertNotNull(groupNode)

        // Verify connection from Group to B exists
        val connectionsFromGroup = graphState.flowGraph.connections.filter {
            it.sourceNodeId == groupNode.id
        }
        assertEquals(1, connectionsFromGroup.size)

        // When: Remove external node B (the downstream node)
        graphState.removeNode("nodeB")

        // Then: The connection from the Group should also be removed
        val remainingConnections = graphState.flowGraph.connections
        assertEquals(0, remainingConnections.size, "Connection should be removed when downstream node is deleted")
    }

    @Test
    fun `ungrouping after external port deletion preserves valid state`() {
        // Given: A flow A -> [Group containing B, C] -> D
        val nodeA = createTestCodeNode("nodeA", "A", 0.0, 0.0, hasInputPort = false)
        val nodeB = createTestCodeNode("nodeB", "B", 100.0, 0.0)
        val nodeC = createTestCodeNode("nodeC", "C", 200.0, 0.0)
        val nodeD = createTestCodeNode("nodeD", "D", 300.0, 0.0, hasOutputPort = false)

        val graph = flowGraph(name = "test", version = "1.0.0") {}
            .addNode(nodeA)
            .addNode(nodeB)
            .addNode(nodeC)
            .addNode(nodeD)
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
                    ),
                    Connection(
                        id = "conn3",
                        sourceNodeId = "nodeC",
                        sourcePortId = "nodeC_output",
                        targetNodeId = "nodeD",
                        targetPortId = "nodeD_input"
                    )
                ))
            }

        val graphState = GraphState(graph)

        // Group B and C
        graphState.toggleNodeInSelection("nodeB")
        graphState.toggleNodeInSelection("nodeC")
        val groupNode = graphState.groupSelectedNodes()
        assertNotNull(groupNode)

        // Remove external node A
        graphState.removeNode("nodeA")

        // When: Ungroup the GraphNode
        val ungroupResult = graphState.ungroupGraphNode(groupNode.id)

        // Then: Ungroup should succeed and state should be valid
        assertTrue(ungroupResult, "Ungroup should succeed even after external node deletion")

        // Verify B and C are restored to root level
        val restoredB = graphState.flowGraph.rootNodes.find { it.name == "B" }
        val restoredC = graphState.flowGraph.rootNodes.find { it.name == "C" }
        assertNotNull(restoredB, "Node B should be restored")
        assertNotNull(restoredC, "Node C should be restored")

        // Verify internal connection B->C still exists
        val internalConnection = graphState.flowGraph.connections.find { conn ->
            conn.sourceNodeId == restoredB.id && conn.targetNodeId == restoredC.id
        }
        assertNotNull(internalConnection, "Internal connection B->C should be preserved")
    }

    // ==================== T069: Self-loop connections stay single-segment ====================

    @Test
    fun `self-loop connection has single segment`() {
        // Given: A connection from a node to itself
        val connection = Connection(
            id = "self_loop",
            sourceNodeId = "nodeA",
            sourcePortId = "nodeA_output",
            targetNodeId = "nodeA",
            targetPortId = "nodeA_input"
        )

        // When: Get segments
        val segments = connection.segments

        // Then: Should have exactly 1 segment
        assertEquals(1, segments.size, "Self-loop should have exactly 1 segment")
        assertEquals("nodeA", segments[0].sourceNodeId)
        assertEquals("nodeA", segments[0].targetNodeId)
    }

    @Test
    fun `self-loop connection is detected correctly`() {
        // Given: A self-loop connection
        val selfLoop = Connection(
            id = "loop1",
            sourceNodeId = "nodeA",
            sourcePortId = "portOut",
            targetNodeId = "nodeA",
            targetPortId = "portIn"
        )

        // Then: isSelfLoop should return true
        assertTrue(selfLoop.isSelfLoop(), "Connection should be detected as self-loop")
    }

    @Test
    fun `self-loop on node inside GraphNode still has single segment`() {
        // Given: A node with a self-loop that gets grouped
        val nodeA = createTestCodeNode("nodeA", "A", 0.0, 0.0)
        val nodeB = createTestCodeNode("nodeB", "B", 100.0, 0.0)

        // Add both input and output port to nodeA for self-loop
        val nodeAWithPorts = nodeA.copy(
            inputPorts = listOf(
                Port(
                    id = "nodeA_input",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "nodeA"
                )
            ),
            outputPorts = listOf(
                Port(
                    id = "nodeA_output",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = "nodeA"
                )
            )
        )

        val graph = flowGraph(name = "test", version = "1.0.0") {}
            .addNode(nodeAWithPorts)
            .addNode(nodeB)
            .let { g ->
                g.copy(connections = listOf(
                    // Self-loop on nodeA
                    Connection(
                        id = "selfloop",
                        sourceNodeId = "nodeA",
                        sourcePortId = "nodeA_output",
                        targetNodeId = "nodeA",
                        targetPortId = "nodeA_input"
                    )
                ))
            }

        val graphState = GraphState(graph)

        // Group A and B
        graphState.toggleNodeInSelection("nodeA")
        graphState.toggleNodeInSelection("nodeB")
        val groupNode = graphState.groupSelectedNodes()
        assertNotNull(groupNode)

        // Self-loop should now be an internal connection of the GraphNode
        val internalSelfLoop = groupNode.internalConnections.find { it.id == "selfloop" }
        assertNotNull(internalSelfLoop, "Self-loop should be in internal connections")

        // Self-loop should still have single segment
        assertEquals(1, internalSelfLoop.segments.size, "Self-loop should still have 1 segment inside GraphNode")
        assertTrue(internalSelfLoop.isSelfLoop(), "Connection should still be detected as self-loop")
    }

    @Test
    fun `segment chain validation succeeds for self-loop`() {
        // Given: A self-loop connection
        val selfLoop = Connection(
            id = "loop1",
            sourceNodeId = "nodeA",
            sourcePortId = "portOut",
            targetNodeId = "nodeA",
            targetPortId = "portIn"
        )

        // When: Validate segment chain
        val validation = selfLoop.validateSegmentChain()

        // Then: Should be valid
        assertTrue(validation.success, "Self-loop segment chain should be valid: ${validation.errors}")
    }

    // ==================== Additional Edge Cases ====================

    @Test
    fun `connection segment visibility correct for internal self-loop`() {
        // Given: A GraphNode containing a node with a self-loop
        val nodeA = createTestCodeNode("nodeA", "A", 0.0, 0.0)
        val nodeB = createTestCodeNode("nodeB", "B", 100.0, 0.0)

        val nodeAWithPorts = nodeA.copy(
            inputPorts = listOf(
                Port(
                    id = "nodeA_input",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "nodeA"
                )
            ),
            outputPorts = listOf(
                Port(
                    id = "nodeA_output",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = "nodeA"
                )
            )
        )

        val graph = flowGraph(name = "test", version = "1.0.0") {}
            .addNode(nodeAWithPorts)
            .addNode(nodeB)
            .let { g ->
                g.copy(connections = listOf(
                    Connection(
                        id = "selfloop",
                        sourceNodeId = "nodeA",
                        sourcePortId = "nodeA_output",
                        targetNodeId = "nodeA",
                        targetPortId = "nodeA_input"
                    )
                ))
            }

        val graphState = GraphState(graph)

        // Group A and B
        graphState.toggleNodeInSelection("nodeA")
        graphState.toggleNodeInSelection("nodeB")
        val groupNode = graphState.groupSelectedNodes()
        assertNotNull(groupNode)

        // Navigate into the GraphNode
        graphState.navigateIntoGraphNode(groupNode.id)

        // Get segments in current context (inside the GraphNode)
        val segmentsInContext = graphState.getSegmentsInContext()

        // The self-loop segment should be visible when viewing inside the GraphNode
        val selfLoopSegment = segmentsInContext.find { seg ->
            seg.sourceNodeId == "nodeA" && seg.targetNodeId == "nodeA"
        }
        assertNotNull(selfLoopSegment, "Self-loop segment should be visible inside the GraphNode")
    }

    @Test
    fun `empty selection cannot be grouped`() {
        val graph = flowGraph(name = "test", version = "1.0.0") {}
        val graphState = GraphState(graph)

        // No selection
        val canGroup = graphState.canGroupSelection()

        assertEquals(false, canGroup, "Empty selection should not be groupable")
    }

    @Test
    fun `single node selection cannot be grouped`() {
        val nodeA = createTestCodeNode("nodeA", "A", 0.0, 0.0)
        val graph = flowGraph(name = "test", version = "1.0.0") {}
            .addNode(nodeA)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("nodeA")

        val canGroup = graphState.canGroupSelection()

        assertEquals(false, canGroup, "Single node selection should not be groupable")
    }
}
