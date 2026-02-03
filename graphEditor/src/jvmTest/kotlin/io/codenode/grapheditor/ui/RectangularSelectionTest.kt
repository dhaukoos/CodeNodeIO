/*
 * RectangularSelection Test
 * UI tests for rectangular selection functionality (Shift-drag)
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.ui.geometry.Offset
import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port
import io.codenode.grapheditor.state.GraphState
import io.codenode.grapheditor.state.SelectionState
import kotlin.test.*

/**
 * TDD tests for rectangular selection functionality.
 * Tests the Shift-drag selection behavior and selection box rendering.
 */
class RectangularSelectionTest {

    /**
     * Helper to create a CodeNode at a specific position for testing.
     */
    private fun createNodeAtPosition(id: String, name: String, x: Double, y: Double): CodeNode {
        return CodeNode(
            id = id,
            name = name,
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(x, y),
            inputPorts = listOf(
                Port(id = "${id}_in", name = "in", direction = Port.Direction.INPUT, dataType = String::class, owningNodeId = id)
            ),
            outputPorts = listOf(
                Port(id = "${id}_out", name = "out", direction = Port.Direction.OUTPUT, dataType = String::class, owningNodeId = id)
            )
        )
    }

    // ============================================
    // T024: Rectangular Selection Initiation Tests
    // ============================================

    @Test
    fun `startRectangularSelection should set selection box start position`() {
        // Given: A graph with nodes
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            codeNode("Node1") { output("out", String::class) }
            codeNode("Node2") { input("in", String::class) }
        }
        val graphState = GraphState(graph)

        // When: Start rectangular selection at a position
        val startPosition = Offset(100f, 100f)
        graphState.startRectangularSelection(startPosition)

        // Then: Selection box start should be set
        assertEquals(startPosition, graphState.selectionState.selectionBoxStart)
        assertTrue(graphState.selectionState.isRectangularSelectionActive)
    }

    @Test
    fun `startRectangularSelection should initialize selection box end to start`() {
        // Given: A graph
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            codeNode("Node1") { output("out", String::class) }
        }
        val graphState = GraphState(graph)

        // When: Start rectangular selection
        val startPosition = Offset(50f, 75f)
        graphState.startRectangularSelection(startPosition)

        // Then: Selection box end should also be at start position
        assertEquals(startPosition, graphState.selectionState.selectionBoxEnd)
    }

    @Test
    fun `startRectangularSelection should not clear existing selection`() {
        // Given: A graph with a node already selected
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            codeNode("Node1") { output("out", String::class) }
            codeNode("Node2") { input("in", String::class) }
        }
        val graphState = GraphState(graph)
        val node1Id = graph.rootNodes[0].id
        graphState.toggleNodeInSelection(node1Id)
        assertEquals(1, graphState.selectionState.nodeSelectionCount)

        // When: Start rectangular selection
        graphState.startRectangularSelection(Offset(200f, 200f))

        // Then: Existing selection should be preserved
        assertTrue(graphState.selectionState.containsNode(node1Id))
    }

    // ============================================
    // T025: Selection Box Rendering During Drag Tests
    // ============================================

    @Test
    fun `updateRectangularSelection should update selection box end position`() {
        // Given: A graph with active rectangular selection
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            codeNode("Node1") { output("out", String::class) }
        }
        val graphState = GraphState(graph)
        graphState.startRectangularSelection(Offset(100f, 100f))

        // When: Update selection to new position
        val newPosition = Offset(300f, 250f)
        graphState.updateRectangularSelection(newPosition)

        // Then: Selection box end should be updated
        assertEquals(newPosition, graphState.selectionState.selectionBoxEnd)
        assertEquals(Offset(100f, 100f), graphState.selectionState.selectionBoxStart)
    }

    @Test
    fun `updateRectangularSelection should handle reverse drag direction`() {
        // Given: A graph with rectangular selection started at a position
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            codeNode("Node1") { output("out", String::class) }
        }
        val graphState = GraphState(graph)
        graphState.startRectangularSelection(Offset(300f, 300f))

        // When: Drag in reverse direction (toward top-left)
        graphState.updateRectangularSelection(Offset(100f, 100f))

        // Then: Selection box bounds should still be valid (normalized)
        val bounds = graphState.selectionState.selectionBoxBounds
        assertNotNull(bounds)
        assertEquals(100f, bounds.left)
        assertEquals(100f, bounds.top)
        assertEquals(300f, bounds.right)
        assertEquals(300f, bounds.bottom)
    }

    @Test
    fun `selectionBoxBounds should return proper rectangle during selection`() {
        // Given: A graph with active rectangular selection
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            codeNode("Node1") { output("out", String::class) }
        }
        val graphState = GraphState(graph)
        graphState.startRectangularSelection(Offset(50f, 75f))
        graphState.updateRectangularSelection(Offset(200f, 175f))

        // Then: Selection box bounds should be correct
        val bounds = graphState.selectionState.selectionBoxBounds
        assertNotNull(bounds)
        assertEquals(50f, bounds.left)
        assertEquals(75f, bounds.top)
        assertEquals(200f, bounds.right)
        assertEquals(175f, bounds.bottom)
    }

    // ============================================
    // T026: Nodes Enclosed Are Selected on Release Tests
    // ============================================

    @Test
    fun `finishRectangularSelection should select nodes whose centers are inside box`() {
        // Given: A graph with nodes at known positions
        // Node center = position + (width/2, height/2) where width=180, height~75 at scale 1.0
        // So node at (50,50) has center at approximately (140, 87)
        val node1 = createNodeAtPosition("node1", "Node1", 50.0, 50.0)  // Center ~(140, 87) - inside box
        val node2 = createNodeAtPosition("node2", "Node2", 500.0, 500.0)  // Center ~(590, 537) - outside box

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)
            .addNode(node2)

        val graphState = GraphState(graph)

        // When: Draw selection box that contains node1's center but not node2's
        // Box from (0,0) to (200,150) should contain center (140, 87)
        graphState.startRectangularSelection(Offset(0f, 0f))
        graphState.updateRectangularSelection(Offset(200f, 150f))
        graphState.finishRectangularSelection()

        // Then: Only Node1 should be selected
        assertTrue(graphState.selectionState.containsNode("node1"))
        assertFalse(graphState.selectionState.containsNode("node2"))
    }

    @Test
    fun `finishRectangularSelection should add to existing selection`() {
        // Given: A graph with one node already selected
        val node1 = createNodeAtPosition("node1", "Node1", 50.0, 50.0)
        val node2 = createNodeAtPosition("node2", "Node2", 250.0, 50.0)
        val node3 = createNodeAtPosition("node3", "Node3", 600.0, 600.0)

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)
            .addNode(node2)
            .addNode(node3)

        val graphState = GraphState(graph)
        graphState.toggleNodeInSelection("node1")

        // When: Draw selection box around node2 only
        // Node2 at (250, 50) has center ~(340, 87)
        graphState.startRectangularSelection(Offset(300f, 50f))
        graphState.updateRectangularSelection(Offset(400f, 150f))
        graphState.finishRectangularSelection()

        // Then: Both node1 and node2 should be selected
        assertTrue(graphState.selectionState.containsNode("node1"))
        assertTrue(graphState.selectionState.containsNode("node2"))
        assertFalse(graphState.selectionState.containsNode("node3"))
    }

    @Test
    fun `finishRectangularSelection should clear selection box state`() {
        // Given: A graph with active rectangular selection
        val node1 = createNodeAtPosition("node1", "Node1", 50.0, 50.0)

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)

        val graphState = GraphState(graph)
        graphState.startRectangularSelection(Offset(100f, 100f))
        graphState.updateRectangularSelection(Offset(300f, 300f))

        // When: Finish selection
        graphState.finishRectangularSelection()

        // Then: Selection box should be cleared
        assertFalse(graphState.selectionState.isRectangularSelectionActive)
        assertNull(graphState.selectionState.selectionBoxStart)
        assertNull(graphState.selectionState.selectionBoxEnd)
    }

    @Test
    fun `finishRectangularSelection with no nodes inside should not modify selection`() {
        // Given: A graph with nodes outside the selection area
        val node1 = createNodeAtPosition("node1", "Node1", 500.0, 500.0)

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)

        val graphState = GraphState(graph)

        // When: Draw selection box in empty area (far from node center)
        graphState.startRectangularSelection(Offset(10f, 10f))
        graphState.updateRectangularSelection(Offset(50f, 50f))
        graphState.finishRectangularSelection()

        // Then: No nodes should be selected
        assertEquals(0, graphState.selectionState.nodeSelectionCount)
    }

    @Test
    fun `cancelRectangularSelection should clear selection box without selecting`() {
        // Given: A graph with active rectangular selection over a node
        val node1 = createNodeAtPosition("node1", "Node1", 50.0, 50.0)

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)

        val graphState = GraphState(graph)
        graphState.startRectangularSelection(Offset(0f, 0f))
        graphState.updateRectangularSelection(Offset(300f, 300f))

        // When: Cancel selection
        graphState.cancelRectangularSelection()

        // Then: Selection box should be cleared and no nodes selected
        assertFalse(graphState.selectionState.isRectangularSelectionActive)
        assertNull(graphState.selectionState.selectionBoxStart)
        assertEquals(0, graphState.selectionState.nodeSelectionCount)
    }

    @Test
    fun `finishRectangularSelection should use node center for hit detection`() {
        // Given: A graph with a node positioned so its top-left is at edge
        // Node at (200, 200) with width 180, height ~75 at scale 1.0
        // Center would be at approximately (290, 237)
        val node1 = createNodeAtPosition("node1", "Node1", 200.0, 200.0)

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)

        val graphState = GraphState(graph)

        // When: Draw selection box that covers the node's top-left corner but not center
        // Box from (0,0) to (250,220) doesn't contain center (290, 237)
        graphState.startRectangularSelection(Offset(0f, 0f))
        graphState.updateRectangularSelection(Offset(250f, 220f))
        graphState.finishRectangularSelection()

        // Then: Node should NOT be selected because center is outside the box
        assertFalse(graphState.selectionState.containsNode("node1"))
    }

    @Test
    fun `multiple nodes can be selected with single rectangular selection`() {
        // Given: A graph with multiple clustered nodes
        val node1 = createNodeAtPosition("node1", "Node1", 50.0, 50.0)   // Center ~(140, 87)
        val node2 = createNodeAtPosition("node2", "Node2", 50.0, 150.0)  // Center ~(140, 187)
        val node3 = createNodeAtPosition("node3", "Node3", 50.0, 250.0)  // Center ~(140, 287)
        val node4 = createNodeAtPosition("node4", "Node4", 600.0, 600.0) // Center ~(690, 637) - outside

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)
            .addNode(node2)
            .addNode(node3)
            .addNode(node4)

        val graphState = GraphState(graph)

        // When: Draw selection box around first 3 nodes' centers
        // Box from (0,0) to (200,350) contains centers at y=87, 187, 287
        graphState.startRectangularSelection(Offset(0f, 0f))
        graphState.updateRectangularSelection(Offset(200f, 350f))
        graphState.finishRectangularSelection()

        // Then: First 3 nodes should be selected
        assertEquals(3, graphState.selectionState.nodeSelectionCount)
        assertTrue(graphState.selectionState.containsNode("node1"))
        assertTrue(graphState.selectionState.containsNode("node2"))
        assertTrue(graphState.selectionState.containsNode("node3"))
        assertFalse(graphState.selectionState.containsNode("node4"))
    }

    @Test
    fun `connections between selected nodes should also be selected`() {
        // Given: A graph with connected nodes
        val node1 = createNodeAtPosition("node1", "Node1", 50.0, 50.0)   // Center ~(140, 87) - inside
        val node2 = createNodeAtPosition("node2", "Node2", 50.0, 150.0)  // Center ~(140, 187) - inside
        val node3 = createNodeAtPosition("node3", "Node3", 600.0, 600.0) // Center ~(690, 637) - outside

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)
            .addNode(node2)
            .addNode(node3)
            .addConnection(
                io.codenode.fbpdsl.model.Connection(
                    id = "conn1",
                    sourceNodeId = "node1",
                    sourcePortId = "node1_out",
                    targetNodeId = "node2",
                    targetPortId = "node2_in"
                )
            )
            .addConnection(
                io.codenode.fbpdsl.model.Connection(
                    id = "conn2",
                    sourceNodeId = "node2",
                    sourcePortId = "node2_out",
                    targetNodeId = "node3",
                    targetPortId = "node3_in"
                )
            )

        val graphState = GraphState(graph)

        // When: Draw selection box around node1 and node2 only
        graphState.startRectangularSelection(Offset(0f, 0f))
        graphState.updateRectangularSelection(Offset(200f, 250f))
        graphState.finishRectangularSelection()

        // Then: node1, node2, and conn1 should be selected (both endpoints inside)
        // conn2 should NOT be selected (node3 is outside)
        assertEquals(2, graphState.selectionState.nodeSelectionCount)
        assertTrue(graphState.selectionState.containsNode("node1"))
        assertTrue(graphState.selectionState.containsNode("node2"))
        assertFalse(graphState.selectionState.containsNode("node3"))

        assertEquals(1, graphState.selectionState.connectionSelectionCount)
        assertTrue(graphState.selectionState.containsConnection("conn1"))
        assertFalse(graphState.selectionState.containsConnection("conn2"))
    }

    @Test
    fun `connections with one endpoint outside box should not be selected`() {
        // Given: A graph with a connection spanning inside and outside the box
        val node1 = createNodeAtPosition("node1", "Node1", 50.0, 50.0)   // inside
        val node2 = createNodeAtPosition("node2", "Node2", 600.0, 600.0) // outside

        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node1)
            .addNode(node2)
            .addConnection(
                io.codenode.fbpdsl.model.Connection(
                    id = "conn1",
                    sourceNodeId = "node1",
                    sourcePortId = "node1_out",
                    targetNodeId = "node2",
                    targetPortId = "node2_in"
                )
            )

        val graphState = GraphState(graph)

        // When: Draw selection box around node1 only
        graphState.startRectangularSelection(Offset(0f, 0f))
        graphState.updateRectangularSelection(Offset(200f, 150f))
        graphState.finishRectangularSelection()

        // Then: Only node1 should be selected, not the connection
        assertEquals(1, graphState.selectionState.nodeSelectionCount)
        assertTrue(graphState.selectionState.containsNode("node1"))
        assertEquals(0, graphState.selectionState.connectionSelectionCount)
    }
}
