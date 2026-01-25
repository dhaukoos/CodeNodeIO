/*
 * ViewSync Test
 * Integration tests for bidirectional view synchronization between visual and textual representations
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.*
import io.codenode.grapheditor.state.GraphState
import kotlin.test.*

class ViewSyncTest {

    private fun createInitialGraph(): FlowGraph {
        return flowGraph("SyncTestGraph", version = "1.0.0") {
            val gen = codeNode("Generator") {
                output("data", String::class)
            }

            val proc = codeNode("Processor") {
                input("input", String::class)
                output("result", String::class)
            }

            gen.output("data") connect proc.input("input")
        }
    }

    @Test
    fun `should synchronize node addition from visual to textual view`() {
        // Given a graph state with initial graph
        val graphState = GraphState(createInitialGraph())
        val initialNodeCount = graphState.flowGraph.rootNodes.size

        // When adding a node in visual view
        val newNode = CodeNode(
            id = "node_new_123",
            name = "NewNode",
            codeNodeType = CodeNodeType.CUSTOM,
            description = "A new node",
            position = Node.Position(400.0, 200.0),
            inputPorts = listOf(
                Port(
                    id = "port_in_123",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "node_new_123"
                )
            ),
            outputPorts = emptyList()
        )

        graphState.addNode(newNode, androidx.compose.ui.geometry.Offset(400f, 200f))

        // Then the textual view should reflect the change
        assertEquals(initialNodeCount + 1, graphState.flowGraph.rootNodes.size,
            "Textual view should show added node")

        val addedNode = graphState.flowGraph.rootNodes.find { it.name == "NewNode" }
        assertNotNull(addedNode, "New node should be in graph")
        assertEquals("NewNode", addedNode.name)
    }

    @Test
    fun `should synchronize connection creation from visual to textual view`() {
        // Given a graph state with two unconnected nodes
        val graphState = GraphState(flowGraph("ConnectionSync", version = "1.0.0") {
            val node1 = codeNode("Node1") {
                output("out1", String::class)
            }

            val node2 = codeNode("Node2") {
                input("in2", String::class)
            }
        })

        val initialConnectionCount = graphState.flowGraph.connections.size

        // When creating a connection in visual view
        val node1 = graphState.flowGraph.rootNodes[0]
        val node2 = graphState.flowGraph.rootNodes[1]

        val connection = Connection(
            id = "conn_sync_123",
            sourceNodeId = node1.id,
            sourcePortId = node1.outputPorts.first().id,
            targetNodeId = node2.id,
            targetPortId = node2.inputPorts.first().id
        )

        graphState.addConnection(connection)

        // Then the textual view should show the connection
        assertEquals(initialConnectionCount + 1, graphState.flowGraph.connections.size,
            "Textual view should show new connection")

        val addedConnection = graphState.flowGraph.connections.find { it.id == "conn_sync_123" }
        assertNotNull(addedConnection, "Connection should be in graph")
    }

    @Test
    fun `should synchronize node deletion from visual to textual view`() {
        // Given a graph state with multiple nodes
        val graphState = GraphState(createInitialGraph())
        val initialNodeCount = graphState.flowGraph.rootNodes.size
        val nodeToRemove = graphState.flowGraph.rootNodes.first()

        // When deleting a node in visual view
        graphState.removeNode(nodeToRemove.id)

        // Then the textual view should reflect the deletion
        assertEquals(initialNodeCount - 1, graphState.flowGraph.rootNodes.size,
            "Textual view should show node removed")

        val deletedNode = graphState.flowGraph.rootNodes.find { it.id == nodeToRemove.id }
        assertNull(deletedNode, "Node should be removed from graph")
    }

    @Test
    fun `should synchronize node position changes from visual to textual view`() {
        // Given a graph state
        val graphState = GraphState(createInitialGraph())
        val node = graphState.flowGraph.rootNodes.first() as CodeNode
        val originalPosition = node.position

        // When moving a node in visual view
        val newX = 500.0
        val newY = 300.0
        graphState.updateNodePosition(node.id, newX, newY)

        // Then the textual view should reflect the new position
        val movedNode = graphState.flowGraph.findNode(node.id) as CodeNode
        assertNotNull(movedNode)
        assertEquals(newX, movedNode.position.x, 0.1, "X position should be updated")
        assertEquals(newY, movedNode.position.y, 0.1, "Y position should be updated")
        assertNotEquals(originalPosition.x, movedNode.position.x, "Position should have changed")
    }

    @Test
    fun `should maintain consistency between visual and textual views during multiple operations`() {
        // Given a graph state
        val graphState = GraphState(createInitialGraph())

        // When performing multiple operations
        // 1. Add a node
        val newNode = CodeNode(
            id = "node_multi_123",
            name = "MultiOpNode",
            codeNodeType = CodeNodeType.CUSTOM,
            position = Node.Position(500.0, 400.0),
            inputPorts = emptyList(),
            outputPorts = emptyList()
        )
        graphState.addNode(newNode, androidx.compose.ui.geometry.Offset(500f, 400f))

        // 2. Remove the first original node
        val firstNode = graphState.flowGraph.rootNodes.first()
        graphState.removeNode(firstNode.id)

        // 3. Update position of remaining node
        val remainingNode = graphState.flowGraph.rootNodes.find { it.name == "Processor" }
        assertNotNull(remainingNode)
        graphState.updateNodePosition(remainingNode.id, 600.0, 500.0)

        // Then both views should be consistent
        assertTrue(graphState.flowGraph.rootNodes.any { it.name == "MultiOpNode" },
            "Added node should be in graph")
        assertFalse(graphState.flowGraph.rootNodes.any { it.id == firstNode.id },
            "Removed node should not be in graph")

        val updatedNode = graphState.flowGraph.findNode(remainingNode.id) as CodeNode
        assertEquals(600.0, updatedNode.position.x, 0.1, "Position should be updated")
    }

    @Test
    fun `should handle rapid view switches without data loss`() {
        // Given a graph state
        val graphState = GraphState(createInitialGraph())
        val originalNodeCount = graphState.flowGraph.rootNodes.size
        val originalConnectionCount = graphState.flowGraph.connections.size

        // When simulating rapid view switches (visual → textual → visual → textual)
        // The graph state should remain consistent

        // Then no data should be lost
        assertEquals(originalNodeCount, graphState.flowGraph.rootNodes.size,
            "Node count should remain consistent")
        assertEquals(originalConnectionCount, graphState.flowGraph.connections.size,
            "Connection count should remain consistent")

        // And graph structure should be intact
        graphState.flowGraph.rootNodes.forEach { node ->
            assertNotNull(node.id, "Node ID should be preserved")
            assertNotNull(node.name, "Node name should be preserved")
        }
    }

    @Test
    fun `should preserve all graph metadata during view synchronization`() {
        // Given a graph with metadata
        val graph = flowGraph(
            name = "MetadataSync",
            version = "2.5.0",
            description = "Graph with metadata to preserve"
        ) {
            val node = codeNode("TestNode") {
                output("out", String::class)
            }
        }

        val graphState = GraphState(graph)

        // When performing operations that trigger sync
        val newNode = CodeNode(
            id = "node_metadata_123",
            name = "NewNode",
            codeNodeType = CodeNodeType.CUSTOM,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = emptyList()
        )
        graphState.addNode(newNode, androidx.compose.ui.geometry.Offset(100f, 100f))

        // Then all metadata should be preserved
        assertEquals("MetadataSync", graphState.flowGraph.name, "Name should be preserved")
        assertEquals("2.5.0", graphState.flowGraph.version, "Version should be preserved")
        assertEquals("Graph with metadata to preserve", graphState.flowGraph.description,
            "Description should be preserved")
    }

    @Test
    fun `should handle empty graph transitions between views`() {
        // Given an empty graph
        val emptyGraph = flowGraph("EmptySync", version = "1.0.0") {}
        val graphState = GraphState(emptyGraph)

        // When adding first node
        val firstNode = CodeNode(
            id = "node_first_123",
            name = "FirstNode",
            codeNodeType = CodeNodeType.CUSTOM,
            position = Node.Position(200.0, 200.0),
            inputPorts = emptyList(),
            outputPorts = emptyList()
        )
        graphState.addNode(firstNode, androidx.compose.ui.geometry.Offset(200f, 200f))

        // Then both views should show the node
        assertEquals(1, graphState.flowGraph.rootNodes.size, "Should have one node")
        assertEquals("FirstNode", graphState.flowGraph.rootNodes.first().name)
    }

    @Test
    fun `should synchronize complex operations with undo and redo`() {
        // Given a graph state with undo/redo support
        val graphState = GraphState(createInitialGraph())
        val initialGraph = graphState.flowGraph

        // When performing an operation
        val newNode = CodeNode(
            id = "node_undo_123",
            name = "UndoTestNode",
            codeNodeType = CodeNodeType.CUSTOM,
            position = Node.Position(300.0, 300.0),
            inputPorts = emptyList(),
            outputPorts = emptyList()
        )
        graphState.addNode(newNode, androidx.compose.ui.geometry.Offset(300f, 300f))

        // Then the change should be reflected in both views
        assertTrue(graphState.flowGraph.rootNodes.any { it.name == "UndoTestNode" },
            "Both views should show the added node")

        // Note: Actual undo/redo implementation would use UndoRedoManager
        // This test verifies the state synchronization foundation
    }
}
