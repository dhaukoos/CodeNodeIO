/*
 * PassThruPort Creation Integration Tests
 * TDD tests for grouping nodes creating PassThruPorts for boundary-crossing connections
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * TDD integration tests for PassThruPort creation during grouping operations.
 * Tests that grouping nodes with external connections creates appropriate PassThruPorts.
 */
class PassThruPortCreationTest {

    // ============================================
    // T016: Integration test for grouping creating PassThruPorts
    // ============================================

    /**
     * Helper to create a test CodeNode with typed ports.
     */
    private fun createTestCodeNode(
        id: String,
        name: String,
        x: Double,
        y: Double,
        inputPortName: String? = "input",
        outputPortName: String? = "output"
    ): CodeNode {
        val inputPorts = if (inputPortName != null) {
            listOf(
                Port(
                    id = "${id}_input",
                    name = inputPortName,
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = id
                )
            )
        } else emptyList()

        val outputPorts = if (outputPortName != null) {
            listOf(
                Port(
                    id = "${id}_output",
                    name = outputPortName,
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

    @Test
    fun `grouping nodes with incoming external connection creates INPUT PassThruPort`() {
        // Given: External -> NodeA -> NodeB (group NodeA and NodeB)
        val external = createTestCodeNode("external", "External", 0.0, 0.0, inputPortName = null)
        val nodeA = createTestCodeNode("nodeA", "NodeA", 100.0, 0.0)
        val nodeB = createTestCodeNode("nodeB", "NodeB", 200.0, 0.0)

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(external)
            .addNode(nodeA)
            .addNode(nodeB)
            .let { g ->
                g.copy(connections = listOf(
                    Connection(
                        id = "conn1",
                        sourceNodeId = "external",
                        sourcePortId = "external_output",
                        targetNodeId = "nodeA",
                        targetPortId = "nodeA_input"
                    ),
                    Connection(
                        id = "conn2",
                        sourceNodeId = "nodeA",
                        sourcePortId = "nodeA_output",
                        targetNodeId = "nodeB",
                        targetPortId = "nodeB_input"
                    )
                ))
            }

        val graphState = GraphState(graph)

        // When: Grouping nodeA and nodeB
        graphState.toggleNodeInSelection("nodeA")
        graphState.toggleNodeInSelection("nodeB")
        val groupedNode = graphState.groupSelectedNodes()

        // Then: GraphNode should have an INPUT PassThruPort for the incoming connection
        assertNotNull(groupedNode, "GraphNode should be created")
        assertTrue(groupedNode.inputPorts.isNotEmpty(), "GraphNode should have input ports")

        // The INPUT port should be a PassThruPort (or at least have the right properties)
        val inputPort = groupedNode.inputPorts.first()
        assertEquals(Port.Direction.INPUT, inputPort.direction)
    }

    @Test
    fun `grouping nodes with outgoing external connection creates OUTPUT PassThruPort`() {
        // Given: NodeA -> NodeB -> External (group NodeA and NodeB)
        val nodeA = createTestCodeNode("nodeA", "NodeA", 0.0, 0.0, inputPortName = null)
        val nodeB = createTestCodeNode("nodeB", "NodeB", 100.0, 0.0)
        val external = createTestCodeNode("external", "External", 200.0, 0.0, outputPortName = null)

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(nodeA)
            .addNode(nodeB)
            .addNode(external)
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
                        targetNodeId = "external",
                        targetPortId = "external_input"
                    )
                ))
            }

        val graphState = GraphState(graph)

        // When: Grouping nodeA and nodeB
        graphState.toggleNodeInSelection("nodeA")
        graphState.toggleNodeInSelection("nodeB")
        val groupedNode = graphState.groupSelectedNodes()

        // Then: GraphNode should have an OUTPUT PassThruPort for the outgoing connection
        assertNotNull(groupedNode, "GraphNode should be created")
        assertTrue(groupedNode.outputPorts.isNotEmpty(), "GraphNode should have output ports")

        // The OUTPUT port should be a PassThruPort (or at least have the right properties)
        val outputPort = groupedNode.outputPorts.first()
        assertEquals(Port.Direction.OUTPUT, outputPort.direction)
    }

    @Test
    fun `grouping nodes with both incoming and outgoing creates INPUT and OUTPUT PassThruPorts`() {
        // Given: External1 -> NodeA -> NodeB -> External2 (group NodeA and NodeB)
        val external1 = createTestCodeNode("external1", "External1", 0.0, 0.0, inputPortName = null)
        val nodeA = createTestCodeNode("nodeA", "NodeA", 100.0, 0.0)
        val nodeB = createTestCodeNode("nodeB", "NodeB", 200.0, 0.0)
        val external2 = createTestCodeNode("external2", "External2", 300.0, 0.0, outputPortName = null)

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(external1)
            .addNode(nodeA)
            .addNode(nodeB)
            .addNode(external2)
            .let { g ->
                g.copy(connections = listOf(
                    Connection(
                        id = "conn1",
                        sourceNodeId = "external1",
                        sourcePortId = "external1_output",
                        targetNodeId = "nodeA",
                        targetPortId = "nodeA_input"
                    ),
                    Connection(
                        id = "conn2",
                        sourceNodeId = "nodeA",
                        sourcePortId = "nodeA_output",
                        targetNodeId = "nodeB",
                        targetPortId = "nodeB_input"
                    ),
                    Connection(
                        id = "conn3",
                        sourceNodeId = "nodeB",
                        sourcePortId = "nodeB_output",
                        targetNodeId = "external2",
                        targetPortId = "external2_input"
                    )
                ))
            }

        val graphState = GraphState(graph)

        // When: Grouping nodeA and nodeB
        graphState.toggleNodeInSelection("nodeA")
        graphState.toggleNodeInSelection("nodeB")
        val groupedNode = graphState.groupSelectedNodes()

        // Then: GraphNode should have both INPUT and OUTPUT PassThruPorts
        assertNotNull(groupedNode, "GraphNode should be created")
        assertTrue(groupedNode.inputPorts.isNotEmpty(), "GraphNode should have input ports")
        assertTrue(groupedNode.outputPorts.isNotEmpty(), "GraphNode should have output ports")
        assertEquals(1, groupedNode.inputPorts.size, "Should have 1 input PassThruPort")
        assertEquals(1, groupedNode.outputPorts.size, "Should have 1 output PassThruPort")
    }

    @Test
    fun `grouping nodes with no external connections creates no PassThruPorts`() {
        // Given: NodeA -> NodeB (both will be grouped, no external connections)
        val nodeA = createTestCodeNode("nodeA", "NodeA", 0.0, 0.0, inputPortName = null)
        val nodeB = createTestCodeNode("nodeB", "NodeB", 100.0, 0.0, outputPortName = null)

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
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

        val graphState = GraphState(graph)

        // When: Grouping nodeA and nodeB
        graphState.toggleNodeInSelection("nodeA")
        graphState.toggleNodeInSelection("nodeB")
        val groupedNode = graphState.groupSelectedNodes()

        // Then: GraphNode should have no PassThruPorts (only internal connection)
        assertNotNull(groupedNode, "GraphNode should be created")
        assertTrue(groupedNode.inputPorts.isEmpty(), "GraphNode should have no input ports")
        assertTrue(groupedNode.outputPorts.isEmpty(), "GraphNode should have no output ports")
    }

    @Test
    fun `grouping nodes with multiple incoming connections creates multiple INPUT PassThruPorts`() {
        // Given: External1 -> NodeA, External2 -> NodeA, NodeA -> NodeB (group NodeA and NodeB)
        val external1 = createTestCodeNode("external1", "External1", 0.0, 0.0, inputPortName = null)
        val external2 = createTestCodeNode("external2", "External2", 0.0, 100.0, inputPortName = null)
        val nodeA = CodeNode(
            id = "nodeA",
            name = "NodeA",
            description = "Test node with two inputs",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(100.0, 50.0),
            inputPorts = listOf(
                Port(
                    id = "nodeA_input1",
                    name = "input1",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "nodeA"
                ),
                Port(
                    id = "nodeA_input2",
                    name = "input2",
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
        val nodeB = createTestCodeNode("nodeB", "NodeB", 200.0, 50.0, outputPortName = null)

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(external1)
            .addNode(external2)
            .addNode(nodeA)
            .addNode(nodeB)
            .let { g ->
                g.copy(connections = listOf(
                    Connection(
                        id = "conn1",
                        sourceNodeId = "external1",
                        sourcePortId = "external1_output",
                        targetNodeId = "nodeA",
                        targetPortId = "nodeA_input1"
                    ),
                    Connection(
                        id = "conn2",
                        sourceNodeId = "external2",
                        sourcePortId = "external2_output",
                        targetNodeId = "nodeA",
                        targetPortId = "nodeA_input2"
                    ),
                    Connection(
                        id = "conn3",
                        sourceNodeId = "nodeA",
                        sourcePortId = "nodeA_output",
                        targetNodeId = "nodeB",
                        targetPortId = "nodeB_input"
                    )
                ))
            }

        val graphState = GraphState(graph)

        // When: Grouping nodeA and nodeB
        graphState.toggleNodeInSelection("nodeA")
        graphState.toggleNodeInSelection("nodeB")
        val groupedNode = graphState.groupSelectedNodes()

        // Then: GraphNode should have 2 INPUT PassThruPorts
        assertNotNull(groupedNode, "GraphNode should be created")
        assertEquals(2, groupedNode.inputPorts.size, "GraphNode should have 2 input PassThruPorts")
    }

    @Test
    fun `PassThruPort should reference correct upstream and downstream nodes`() {
        // Given: External -> NodeA -> NodeB (group NodeA and NodeB)
        val external = createTestCodeNode("external", "External", 0.0, 0.0, inputPortName = null)
        val nodeA = createTestCodeNode("nodeA", "NodeA", 100.0, 0.0)
        val nodeB = createTestCodeNode("nodeB", "NodeB", 200.0, 0.0)

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(external)
            .addNode(nodeA)
            .addNode(nodeB)
            .let { g ->
                g.copy(connections = listOf(
                    Connection(
                        id = "conn1",
                        sourceNodeId = "external",
                        sourcePortId = "external_output",
                        targetNodeId = "nodeA",
                        targetPortId = "nodeA_input"
                    ),
                    Connection(
                        id = "conn2",
                        sourceNodeId = "nodeA",
                        sourcePortId = "nodeA_output",
                        targetNodeId = "nodeB",
                        targetPortId = "nodeB_input"
                    )
                ))
            }

        val graphState = GraphState(graph)

        // When: Grouping nodeA and nodeB
        graphState.toggleNodeInSelection("nodeA")
        graphState.toggleNodeInSelection("nodeB")
        val groupedNode = graphState.groupSelectedNodes()

        // Then: The INPUT PassThruPort should reference:
        // - upstream: external node (external)
        // - downstream: internal node (nodeA)
        assertNotNull(groupedNode)

        // Check port mappings to verify correct references
        val inputMapping = groupedNode.portMappings.values.find {
            it.childNodeId == "nodeA"
        }
        assertNotNull(inputMapping, "Should have port mapping for nodeA")
    }

    @Test
    fun `connections should be updated to reference GraphNode PassThruPorts after grouping`() {
        // Given: External -> NodeA -> NodeB (group NodeA and NodeB)
        val external = createTestCodeNode("external", "External", 0.0, 0.0, inputPortName = null)
        val nodeA = createTestCodeNode("nodeA", "NodeA", 100.0, 0.0)
        val nodeB = createTestCodeNode("nodeB", "NodeB", 200.0, 0.0)

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(external)
            .addNode(nodeA)
            .addNode(nodeB)
            .let { g ->
                g.copy(connections = listOf(
                    Connection(
                        id = "conn1",
                        sourceNodeId = "external",
                        sourcePortId = "external_output",
                        targetNodeId = "nodeA",
                        targetPortId = "nodeA_input"
                    ),
                    Connection(
                        id = "conn2",
                        sourceNodeId = "nodeA",
                        sourcePortId = "nodeA_output",
                        targetNodeId = "nodeB",
                        targetPortId = "nodeB_input"
                    )
                ))
            }

        val graphState = GraphState(graph)

        // When: Grouping nodeA and nodeB
        graphState.toggleNodeInSelection("nodeA")
        graphState.toggleNodeInSelection("nodeB")
        val groupedNode = graphState.groupSelectedNodes()

        // Then: The external connection should now target the GraphNode
        assertNotNull(groupedNode)
        val updatedConnections = graphState.flowGraph.connections
        val externalConnection = updatedConnections.find { it.sourceNodeId == "external" }
        assertNotNull(externalConnection, "External connection should still exist")
        assertEquals(groupedNode.id, externalConnection.targetNodeId,
            "External connection should now target the GraphNode")
    }
}
