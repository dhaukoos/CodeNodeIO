/*
 * MultiSelection Test
 * UI tests for multi-selection functionality (Shift-click)
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.grapheditor.state.GraphState
import io.codenode.grapheditor.state.SelectionState
import kotlin.test.*

/**
 * TDD tests for multi-selection functionality.
 * Tests the Shift-click selection behavior and selection rendering.
 */
class MultiSelectionTest {

    // ============================================
    // T013: Shift-click Node Selection Tests
    // ============================================

    @Test
    fun `toggleNodeInSelection should add node to empty selection`() {
        // Given: A graph with nodes and empty selection
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            codeNode("Node1") { output("out", String::class) }
            codeNode("Node2") { input("in", String::class) }
        }
        val graphState = GraphState(graph)

        // When: Toggle first node into selection
        val node1Id = graph.rootNodes[0].id
        graphState.toggleNodeInSelection(node1Id)

        // Then: Node should be in selection
        assertTrue(graphState.selectionState.selectedNodeIds.contains(node1Id))
        assertEquals(1, graphState.selectionState.nodeSelectionCount)
    }

    @Test
    fun `toggleNodeInSelection should add multiple nodes to selection`() {
        // Given: A graph with multiple nodes
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            codeNode("Node1") { output("out", String::class) }
            codeNode("Node2") { input("in", String::class) }
            codeNode("Node3") { input("in", String::class) }
        }
        val graphState = GraphState(graph)

        // When: Toggle multiple nodes
        val node1Id = graph.rootNodes[0].id
        val node2Id = graph.rootNodes[1].id
        val node3Id = graph.rootNodes[2].id

        graphState.toggleNodeInSelection(node1Id)
        graphState.toggleNodeInSelection(node2Id)
        graphState.toggleNodeInSelection(node3Id)

        // Then: All nodes should be in selection
        assertEquals(3, graphState.selectionState.nodeSelectionCount)
        assertTrue(graphState.selectionState.selectedNodeIds.containsAll(setOf(node1Id, node2Id, node3Id)))
    }

    @Test
    fun `toggleNodeInSelection should remove node from selection when already selected`() {
        // Given: A graph with a node already in selection
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            codeNode("Node1") { output("out", String::class) }
            codeNode("Node2") { input("in", String::class) }
        }
        val graphState = GraphState(graph)

        val node1Id = graph.rootNodes[0].id
        graphState.toggleNodeInSelection(node1Id)
        assertEquals(1, graphState.selectionState.nodeSelectionCount)

        // When: Toggle same node again
        graphState.toggleNodeInSelection(node1Id)

        // Then: Node should be removed from selection
        assertFalse(graphState.selectionState.selectedNodeIds.contains(node1Id))
        assertEquals(0, graphState.selectionState.nodeSelectionCount)
    }

    @Test
    fun `toggleNodeInSelection should preserve other selected nodes when toggling`() {
        // Given: A graph with multiple selected nodes
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            codeNode("Node1") { output("out", String::class) }
            codeNode("Node2") { input("in", String::class) }
            codeNode("Node3") { input("in", String::class) }
        }
        val graphState = GraphState(graph)

        val node1Id = graph.rootNodes[0].id
        val node2Id = graph.rootNodes[1].id
        val node3Id = graph.rootNodes[2].id

        graphState.toggleNodeInSelection(node1Id)
        graphState.toggleNodeInSelection(node2Id)
        graphState.toggleNodeInSelection(node3Id)

        // When: Remove node2 from selection
        graphState.toggleNodeInSelection(node2Id)

        // Then: node1 and node3 should still be selected
        assertEquals(2, graphState.selectionState.nodeSelectionCount)
        assertTrue(graphState.selectionState.selectedNodeIds.contains(node1Id))
        assertFalse(graphState.selectionState.selectedNodeIds.contains(node2Id))
        assertTrue(graphState.selectionState.selectedNodeIds.contains(node3Id))
    }

    // ============================================
    // T014: Selection Highlight Rendering Tests
    // ============================================

    @Test
    fun `isNodeSelected should return true for selected nodes`() {
        // Given: A graph with selected nodes
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            codeNode("Node1") { output("out", String::class) }
            codeNode("Node2") { input("in", String::class) }
        }
        val graphState = GraphState(graph)

        val node1Id = graph.rootNodes[0].id
        graphState.toggleNodeInSelection(node1Id)

        // Then: isNodeSelected should return true for selected node
        assertTrue(graphState.isNodeSelected(node1Id))
    }

    @Test
    fun `isNodeSelected should return false for non-selected nodes`() {
        // Given: A graph with some nodes selected
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            codeNode("Node1") { output("out", String::class) }
            codeNode("Node2") { input("in", String::class) }
        }
        val graphState = GraphState(graph)

        val node1Id = graph.rootNodes[0].id
        val node2Id = graph.rootNodes[1].id
        graphState.toggleNodeInSelection(node1Id)

        // Then: isNodeSelected should return false for non-selected node
        assertFalse(graphState.isNodeSelected(node2Id))
    }

    @Test
    fun `selectionState hasSelection should be true when nodes are selected`() {
        // Given: A graph with selected nodes
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            codeNode("Node1") { output("out", String::class) }
        }
        val graphState = GraphState(graph)

        val node1Id = graph.rootNodes[0].id
        graphState.toggleNodeInSelection(node1Id)

        // Then: hasSelection should be true
        assertTrue(graphState.selectionState.hasSelection)
    }

    @Test
    fun `selectionState canGroup should be true when 2 or more nodes selected`() {
        // Given: A graph with 2 nodes selected
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            codeNode("Node1") { output("out", String::class) }
            codeNode("Node2") { input("in", String::class) }
        }
        val graphState = GraphState(graph)

        graphState.toggleNodeInSelection(graph.rootNodes[0].id)
        graphState.toggleNodeInSelection(graph.rootNodes[1].id)

        // Then: canGroup should be true
        assertTrue(graphState.selectionState.canGroup)
    }

    @Test
    fun `selectionState canGroup should be false with less than 2 nodes selected`() {
        // Given: A graph with 1 node selected
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            codeNode("Node1") { output("out", String::class) }
            codeNode("Node2") { input("in", String::class) }
        }
        val graphState = GraphState(graph)

        graphState.toggleNodeInSelection(graph.rootNodes[0].id)

        // Then: canGroup should be false
        assertFalse(graphState.selectionState.canGroup)
    }

    // ============================================
    // T015: Click-to-Clear Selection Tests
    // ============================================

    @Test
    fun `clearSelection should remove all nodes from selection`() {
        // Given: A graph with multiple selected nodes
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            codeNode("Node1") { output("out", String::class) }
            codeNode("Node2") { input("in", String::class) }
            codeNode("Node3") { input("in", String::class) }
        }
        val graphState = GraphState(graph)

        graphState.toggleNodeInSelection(graph.rootNodes[0].id)
        graphState.toggleNodeInSelection(graph.rootNodes[1].id)
        graphState.toggleNodeInSelection(graph.rootNodes[2].id)
        assertEquals(3, graphState.selectionState.nodeSelectionCount)

        // When: Clear selection
        graphState.clearSelection()

        // Then: Selection should be empty
        assertEquals(0, graphState.selectionState.nodeSelectionCount)
        assertFalse(graphState.selectionState.hasSelection)
    }

    @Test
    fun `clearSelection should also clear connection selection`() {
        // Given: A graph with selected nodes and connections
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            val n1 = codeNode("Node1") { output("out", String::class) }
            val n2 = codeNode("Node2") { input("in", String::class) }
            addConnection(n1.output("out") connect n2.input("in"))
        }
        val graphState = GraphState(graph)

        graphState.toggleNodeInSelection(graph.rootNodes[0].id)
        graphState.toggleConnectionInSelection(graph.connections[0].id)

        // When: Clear selection
        graphState.clearSelection()

        // Then: Both node and connection selection should be cleared
        assertEquals(0, graphState.selectionState.nodeSelectionCount)
        assertEquals(0, graphState.selectionState.connectionSelectionCount)
        assertFalse(graphState.selectionState.hasSelection)
    }

    @Test
    fun `clearSelection on empty selection should be no-op`() {
        // Given: A graph with empty selection
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            codeNode("Node1") { output("out", String::class) }
        }
        val graphState = GraphState(graph)

        // When: Clear empty selection
        graphState.clearSelection()

        // Then: Should not throw and selection should still be empty
        assertEquals(0, graphState.selectionState.nodeSelectionCount)
        assertFalse(graphState.selectionState.hasSelection)
    }

    // ============================================
    // Connection Selection Tests (for connection highlighting)
    // ============================================

    @Test
    fun `toggleConnectionInSelection should add connection to selection`() {
        // Given: A graph with a connection
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            val n1 = codeNode("Node1") { output("out", String::class) }
            val n2 = codeNode("Node2") { input("in", String::class) }
            addConnection(n1.output("out") connect n2.input("in"))
        }
        val graphState = GraphState(graph)

        val connId = graph.connections[0].id

        // When: Toggle connection
        graphState.toggleConnectionInSelection(connId)

        // Then: Connection should be in selection
        assertTrue(graphState.selectionState.selectedConnectionIds.contains(connId))
        assertEquals(1, graphState.selectionState.connectionSelectionCount)
    }

    @Test
    fun `toggleConnectionInSelection should remove connection when already selected`() {
        // Given: A graph with a selected connection
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            val n1 = codeNode("Node1") { output("out", String::class) }
            val n2 = codeNode("Node2") { input("in", String::class) }
            addConnection(n1.output("out") connect n2.input("in"))
        }
        val graphState = GraphState(graph)

        val connId = graph.connections[0].id
        graphState.toggleConnectionInSelection(connId)

        // When: Toggle again
        graphState.toggleConnectionInSelection(connId)

        // Then: Connection should be removed
        assertFalse(graphState.selectionState.selectedConnectionIds.contains(connId))
        assertEquals(0, graphState.selectionState.connectionSelectionCount)
    }

    @Test
    fun `selectConnectionsBetweenNodes should auto-select internal connections`() {
        // Given: A graph with 3 connected nodes
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            val n1 = codeNode("Node1") { output("out", String::class) }
            val n2 = codeNode("Node2") {
                input("in", String::class)
                output("out", String::class)
            }
            val n3 = codeNode("Node3") { input("in", String::class) }
            addConnection(n1.output("out") connect n2.input("in"))
            addConnection(n2.output("out") connect n3.input("in"))
        }
        val graphState = GraphState(graph)

        val node1Id = graph.rootNodes[0].id
        val node2Id = graph.rootNodes[1].id

        // When: Select internal connections between node1 and node2
        graphState.selectConnectionsBetweenNodes(setOf(node1Id, node2Id))

        // Then: Connection between node1 and node2 should be selected
        // Connection to node3 should not be selected
        assertEquals(1, graphState.selectionState.connectionSelectionCount)

        val conn1to2 = graph.connections.find {
            it.sourceNodeId == node1Id && it.targetNodeId == node2Id
        }
        assertNotNull(conn1to2)
        assertTrue(graphState.selectionState.selectedConnectionIds.contains(conn1to2.id))
    }

    // ============================================
    // T088: Performance Test - Select 50+ nodes in <100ms
    // ============================================

    @Test
    fun `selecting 50 nodes should complete in under 100ms`() {
        // Given: A graph with 50+ nodes
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            repeat(50) { i ->
                codeNode("Node$i") {
                    input("in", String::class)
                    output("out", String::class)
                }
            }
        }
        val graphState = GraphState(graph)

        // When: Select all nodes and measure time
        val startTime = System.currentTimeMillis()
        graph.rootNodes.forEach { node ->
            graphState.toggleNodeInSelection(node.id)
        }
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Then: Selection should complete in under 100ms
        assertEquals(50, graphState.selectionState.nodeSelectionCount)
        assertTrue(
            duration < 100,
            "Selecting 50 nodes took ${duration}ms, should be under 100ms"
        )
    }

    @Test
    fun `adding nodes to selection should scale linearly`() {
        // Given: A graph with 100 nodes
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            repeat(100) { i ->
                codeNode("Node$i") {
                    input("in", String::class)
                    output("out", String::class)
                }
            }
        }
        val graphState = GraphState(graph)

        // When: Select all nodes
        val startTime = System.currentTimeMillis()
        val nodeIds = graph.rootNodes.map { it.id }.toSet()
        graphState.addNodesToSelection(nodeIds)
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Then: Selection should complete in under 100ms (allowing for overhead)
        assertEquals(100, graphState.selectionState.nodeSelectionCount)
        assertTrue(
            duration < 100,
            "Adding 100 nodes to selection took ${duration}ms, should be under 100ms"
        )
    }
}
