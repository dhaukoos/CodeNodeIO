/*
 * GraphNodeFactory Tests
 * TDD tests for creating GraphNodes from selected nodes
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.factory

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * TDD tests for GraphNodeFactory.
 * Tests the creation of GraphNodes from selected nodes with auto-generated port mappings.
 */
class GraphNodeFactoryTest {

    // ============================================
    // T034: Unit test for GraphNodeFactory.createFromSelection()
    // ============================================

    @Test
    fun `createFromSelection should create GraphNode containing selected nodes`() {
        // Given: Multiple nodes that will be grouped
        val node1 = createTestCodeNode("node1", "Transformer1", 100.0, 100.0)
        val node2 = createTestCodeNode("node2", "Transformer2", 200.0, 100.0)
        val allNodes = listOf(node1, node2)
        val selectedNodeIds = setOf("node1", "node2")
        val connections = emptyList<Connection>()

        // When: Creating a GraphNode from selection
        val result = GraphNodeFactory.createFromSelection(
            selectedNodeIds = selectedNodeIds,
            allNodes = allNodes,
            allConnections = connections,
            graphNodeName = "GroupedNodes"
        )

        // Then: GraphNode should be created with the selected nodes as children
        assertNotNull(result)
        assertEquals("GroupedNodes", result.name)
        assertEquals(2, result.childNodes.size)
        assertTrue(result.childNodes.any { it.id == "node1" })
        assertTrue(result.childNodes.any { it.id == "node2" })
    }

    @Test
    fun `createFromSelection should fail if fewer than 2 nodes selected`() {
        // Given: Only one node selected
        val node1 = createTestCodeNode("node1", "Transformer1", 100.0, 100.0)
        val allNodes = listOf(node1)
        val selectedNodeIds = setOf("node1")
        val connections = emptyList<Connection>()

        // When: Attempting to create a GraphNode
        val result = GraphNodeFactory.createFromSelection(
            selectedNodeIds = selectedNodeIds,
            allNodes = allNodes,
            allConnections = connections,
            graphNodeName = "SingleNode"
        )

        // Then: Should return null (cannot group single node)
        assertNull(result)
    }

    @Test
    fun `createFromSelection should set child nodes parentNodeId to new GraphNode`() {
        // Given: Multiple nodes
        val node1 = createTestCodeNode("node1", "Transformer1", 100.0, 100.0)
        val node2 = createTestCodeNode("node2", "Transformer2", 200.0, 100.0)
        val allNodes = listOf(node1, node2)
        val selectedNodeIds = setOf("node1", "node2")
        val connections = emptyList<Connection>()

        // When: Creating a GraphNode
        val result = GraphNodeFactory.createFromSelection(
            selectedNodeIds = selectedNodeIds,
            allNodes = allNodes,
            allConnections = connections,
            graphNodeName = "GroupedNodes"
        )

        // Then: All child nodes should have parentNodeId set to the GraphNode's ID
        assertNotNull(result)
        result.childNodes.forEach { child ->
            assertEquals(result.id, child.parentNodeId)
        }
    }

    @Test
    fun `createFromSelection should preserve internal connections between selected nodes`() {
        // Given: Two nodes with a connection between them
        val node1 = createTestCodeNode("node1", "Source", 100.0, 100.0)
        val node2 = createTestCodeNode("node2", "Target", 200.0, 100.0)
        val allNodes = listOf(node1, node2)
        val selectedNodeIds = setOf("node1", "node2")
        val internalConnection = Connection(
            id = "conn1",
            sourceNodeId = "node1",
            sourcePortId = "node1_out",
            targetNodeId = "node2",
            targetPortId = "node2_in"
        )
        val connections = listOf(internalConnection)

        // When: Creating a GraphNode
        val result = GraphNodeFactory.createFromSelection(
            selectedNodeIds = selectedNodeIds,
            allNodes = allNodes,
            allConnections = connections,
            graphNodeName = "GroupedNodes"
        )

        // Then: Internal connection should be preserved in GraphNode
        assertNotNull(result)
        assertEquals(1, result.internalConnections.size)
        assertEquals("conn1", result.internalConnections[0].id)
    }

    @Test
    fun `createFromSelection should position GraphNode at centroid of selected nodes`() {
        // Given: Nodes at various positions
        val node1 = createTestCodeNode("node1", "Node1", 100.0, 100.0)
        val node2 = createTestCodeNode("node2", "Node2", 200.0, 100.0)
        val node3 = createTestCodeNode("node3", "Node3", 150.0, 200.0)
        val allNodes = listOf(node1, node2, node3)
        val selectedNodeIds = setOf("node1", "node2", "node3")
        val connections = emptyList<Connection>()

        // When: Creating a GraphNode
        val result = GraphNodeFactory.createFromSelection(
            selectedNodeIds = selectedNodeIds,
            allNodes = allNodes,
            allConnections = connections,
            graphNodeName = "GroupedNodes"
        )

        // Then: GraphNode position should be at centroid
        assertNotNull(result)
        // Centroid: x = (100 + 200 + 150) / 3 = 150, y = (100 + 100 + 200) / 3 = 133.33
        assertEquals(150.0, result.position.x, 0.1)
        assertEquals(133.33, result.position.y, 0.1)
    }

    @Test
    fun `createFromSelection should generate unique ID for GraphNode`() {
        // Given: Multiple nodes
        val node1 = createTestCodeNode("node1", "Transformer1", 100.0, 100.0)
        val node2 = createTestCodeNode("node2", "Transformer2", 200.0, 100.0)
        val allNodes = listOf(node1, node2)
        val selectedNodeIds = setOf("node1", "node2")
        val connections = emptyList<Connection>()

        // When: Creating two GraphNodes
        val result1 = GraphNodeFactory.createFromSelection(
            selectedNodeIds = selectedNodeIds,
            allNodes = allNodes,
            allConnections = connections,
            graphNodeName = "Group1"
        )
        val result2 = GraphNodeFactory.createFromSelection(
            selectedNodeIds = selectedNodeIds,
            allNodes = allNodes,
            allConnections = connections,
            graphNodeName = "Group2"
        )

        // Then: IDs should be different
        assertNotNull(result1)
        assertNotNull(result2)
        assertNotEquals(result1.id, result2.id)
    }

    // ============================================
    // T035: Unit test for port mapping generation from external connections
    // ============================================

    @Test
    fun `createFromSelection should generate input port for external incoming connection`() {
        // Given: An external node connected to a selected node's input
        val externalNode = createTestCodeNode("external", "External", 0.0, 100.0)
        val node1 = createTestCodeNode("node1", "Internal1", 100.0, 100.0)
        val node2 = createTestCodeNode("node2", "Internal2", 200.0, 100.0)
        val allNodes = listOf(externalNode, node1, node2)
        val selectedNodeIds = setOf("node1", "node2")

        val externalToInternal = Connection(
            id = "ext_conn",
            sourceNodeId = "external",
            sourcePortId = "external_out",
            targetNodeId = "node1",
            targetPortId = "node1_in"
        )
        val connections = listOf(externalToInternal)

        // When: Creating a GraphNode
        val result = GraphNodeFactory.createFromSelection(
            selectedNodeIds = selectedNodeIds,
            allNodes = allNodes,
            allConnections = connections,
            graphNodeName = "GroupedNodes"
        )

        // Then: GraphNode should have an input port with mapping to node1's input
        assertNotNull(result)
        assertEquals(1, result.inputPorts.size)
        val inputPort = result.inputPorts[0]
        assertEquals(Port.Direction.INPUT, inputPort.direction)

        // Port mapping should reference the internal node's port
        val mapping = result.portMappings[inputPort.name]
        assertNotNull(mapping)
        assertEquals("node1", mapping.childNodeId)
        assertEquals("node1_in", mapping.childPortName)
    }

    @Test
    fun `createFromSelection should generate output port for external outgoing connection`() {
        // Given: A selected node connected to an external node
        val node1 = createTestCodeNode("node1", "Internal1", 100.0, 100.0)
        val node2 = createTestCodeNode("node2", "Internal2", 200.0, 100.0)
        val externalNode = createTestCodeNode("external", "External", 300.0, 100.0)
        val allNodes = listOf(node1, node2, externalNode)
        val selectedNodeIds = setOf("node1", "node2")

        val internalToExternal = Connection(
            id = "ext_conn",
            sourceNodeId = "node2",
            sourcePortId = "node2_out",
            targetNodeId = "external",
            targetPortId = "external_in"
        )
        val connections = listOf(internalToExternal)

        // When: Creating a GraphNode
        val result = GraphNodeFactory.createFromSelection(
            selectedNodeIds = selectedNodeIds,
            allNodes = allNodes,
            allConnections = connections,
            graphNodeName = "GroupedNodes"
        )

        // Then: GraphNode should have an output port with mapping to node2's output
        assertNotNull(result)
        assertEquals(1, result.outputPorts.size)
        val outputPort = result.outputPorts[0]
        assertEquals(Port.Direction.OUTPUT, outputPort.direction)

        // Port mapping should reference the internal node's port
        val mapping = result.portMappings[outputPort.name]
        assertNotNull(mapping)
        assertEquals("node2", mapping.childNodeId)
        assertEquals("node2_out", mapping.childPortName)
    }

    @Test
    fun `createFromSelection should generate both input and output ports for bidirectional external connections`() {
        // Given: External connections in both directions
        val externalIn = createTestCodeNode("external_in", "Source", 0.0, 100.0)
        val node1 = createTestCodeNode("node1", "Internal1", 100.0, 100.0)
        val node2 = createTestCodeNode("node2", "Internal2", 200.0, 100.0)
        val externalOut = createTestCodeNode("external_out", "Sink", 300.0, 100.0)
        val allNodes = listOf(externalIn, node1, node2, externalOut)
        val selectedNodeIds = setOf("node1", "node2")

        val incomingConn = Connection(
            id = "incoming",
            sourceNodeId = "external_in",
            sourcePortId = "external_in_out",
            targetNodeId = "node1",
            targetPortId = "node1_in"
        )
        val internalConn = Connection(
            id = "internal",
            sourceNodeId = "node1",
            sourcePortId = "node1_out",
            targetNodeId = "node2",
            targetPortId = "node2_in"
        )
        val outgoingConn = Connection(
            id = "outgoing",
            sourceNodeId = "node2",
            sourcePortId = "node2_out",
            targetNodeId = "external_out",
            targetPortId = "external_out_in"
        )
        val connections = listOf(incomingConn, internalConn, outgoingConn)

        // When: Creating a GraphNode
        val result = GraphNodeFactory.createFromSelection(
            selectedNodeIds = selectedNodeIds,
            allNodes = allNodes,
            allConnections = connections,
            graphNodeName = "GroupedNodes"
        )

        // Then: GraphNode should have both input and output ports
        assertNotNull(result)
        assertEquals(1, result.inputPorts.size)
        assertEquals(1, result.outputPorts.size)

        // Internal connection should be preserved
        assertEquals(1, result.internalConnections.size)
    }

    @Test
    fun `createFromSelection should not include internal connections as port mappings`() {
        // Given: Two selected nodes with internal connection only
        val node1 = createTestCodeNode("node1", "Source", 100.0, 100.0)
        val node2 = createTestCodeNode("node2", "Target", 200.0, 100.0)
        val allNodes = listOf(node1, node2)
        val selectedNodeIds = setOf("node1", "node2")

        val internalConn = Connection(
            id = "internal",
            sourceNodeId = "node1",
            sourcePortId = "node1_out",
            targetNodeId = "node2",
            targetPortId = "node2_in"
        )
        val connections = listOf(internalConn)

        // When: Creating a GraphNode
        val result = GraphNodeFactory.createFromSelection(
            selectedNodeIds = selectedNodeIds,
            allNodes = allNodes,
            allConnections = connections,
            graphNodeName = "GroupedNodes"
        )

        // Then: No external ports should be generated (connection is internal)
        assertNotNull(result)
        assertEquals(0, result.inputPorts.size)
        assertEquals(0, result.outputPorts.size)
        assertEquals(0, result.portMappings.size)

        // But internal connection should be preserved
        assertEquals(1, result.internalConnections.size)
    }

    @Test
    fun `createFromSelection should handle multiple external connections to same internal port`() {
        // Given: Two external nodes connecting to the same internal port
        val external1 = createTestCodeNode("ext1", "External1", 0.0, 50.0)
        val external2 = createTestCodeNode("ext2", "External2", 0.0, 150.0)
        val node1 = createTestCodeNode("node1", "Internal", 100.0, 100.0)
        val node2 = createTestCodeNode("node2", "Internal2", 200.0, 100.0)
        val allNodes = listOf(external1, external2, node1, node2)
        val selectedNodeIds = setOf("node1", "node2")

        // Note: In practice, a port typically can't have multiple incoming connections
        // but for robustness, test that we handle it gracefully
        val conn1 = Connection(
            id = "conn1",
            sourceNodeId = "ext1",
            sourcePortId = "ext1_out",
            targetNodeId = "node1",
            targetPortId = "node1_in"
        )
        val conn2 = Connection(
            id = "conn2",
            sourceNodeId = "ext2",
            sourcePortId = "ext2_out",
            targetNodeId = "node1",
            targetPortId = "node1_in"  // Same target port
        )
        val connections = listOf(conn1, conn2)

        // When: Creating a GraphNode
        val result = GraphNodeFactory.createFromSelection(
            selectedNodeIds = selectedNodeIds,
            allNodes = allNodes,
            allConnections = connections,
            graphNodeName = "GroupedNodes"
        )

        // Then: Should create port mappings (implementation may deduplicate)
        assertNotNull(result)
        assertTrue(result.inputPorts.isNotEmpty())
    }

    @Test
    fun `generatePortMappings should return empty list when no external connections`() {
        // Given: Selected nodes with only internal connections
        val selectedNodeIds = setOf("node1", "node2")
        val internalConn = Connection(
            id = "internal",
            sourceNodeId = "node1",
            sourcePortId = "out",
            targetNodeId = "node2",
            targetPortId = "in"
        )
        val connections = listOf(internalConn)

        // When: Generating port mappings
        val result = GraphNodeFactory.generatePortMappings(
            selectedNodeIds = selectedNodeIds,
            allConnections = connections
        )

        // Then: Should return empty list
        assertTrue(result.isEmpty())
    }

    @Test
    fun `generatePortMappings should identify external connections correctly`() {
        // Given: Mix of internal and external connections
        val selectedNodeIds = setOf("node1", "node2")
        val internalConn = Connection(
            id = "internal",
            sourceNodeId = "node1",
            sourcePortId = "out",
            targetNodeId = "node2",
            targetPortId = "in"
        )
        val externalIncoming = Connection(
            id = "external_in",
            sourceNodeId = "external",
            sourcePortId = "out",
            targetNodeId = "node1",
            targetPortId = "in"
        )
        val externalOutgoing = Connection(
            id = "external_out",
            sourceNodeId = "node2",
            sourcePortId = "out",
            targetNodeId = "external",
            targetPortId = "in"
        )
        val connections = listOf(internalConn, externalIncoming, externalOutgoing)

        // When: Generating port mappings
        val result = GraphNodeFactory.generatePortMappings(
            selectedNodeIds = selectedNodeIds,
            allConnections = connections
        )

        // Then: Should identify 2 external connections
        assertEquals(2, result.size)
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
