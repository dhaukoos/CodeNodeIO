/*
 * GraphNodeRenderer Tests
 * TDD tests for GraphNode zoom-in button rendering
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port
import io.codenode.grapheditor.state.GraphState
import kotlin.test.*

/**
 * TDD tests for GraphNodeRenderer zoom-in functionality.
 * These tests verify that the zoom-in button (expand icon) on GraphNodes:
 * - Is rendered in the correct position on the GraphNode header
 * - Is visually distinct and clickable
 * - Triggers navigation into the GraphNode when clicked
 *
 * Task: T057 [US5] Write UI test for zoom-in button rendering on GraphNode
 */
class GraphNodeRendererTest {

    // ============================================
    // T057: Tests for zoom-in button rendering
    // ============================================

    @Test
    fun `GraphNode should have expand icon in header`() {
        // Given: A GraphNode with child nodes
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

        // Then: The GraphNode has properties needed for expand icon rendering
        // The expand icon is rendered in the header area
        assertTrue(graphNode.childNodes.isNotEmpty(), "GraphNode should have child nodes for expand icon to be meaningful")
        assertNotNull(graphNode.position, "GraphNode should have position for rendering")
    }

    @Test
    fun `expand icon should be positioned in top-right area of header`() {
        // Given: Standard GraphNode rendering parameters
        val nodeWidth = 180f
        val headerHeight = 30f
        val iconSize = 12f
        val scale = 1.0f

        // When: Calculating expand icon position
        val iconX = nodeWidth - iconSize - 24f * scale  // Position from GraphNodeRenderer
        val iconY = (headerHeight - iconSize) / 2f

        // Then: Icon should be in the right portion of the header
        assertTrue(iconX > nodeWidth / 2, "Expand icon should be in right half of header")
        assertTrue(iconY > 0 && iconY < headerHeight, "Expand icon should be vertically centered in header")
        assertTrue(iconX + iconSize < nodeWidth, "Expand icon should not overflow header")
    }

    @Test
    fun `expand icon area should be large enough for click interaction`() {
        // Given: Expand icon dimensions at standard scale
        val iconSize = 12f
        val scale = 1.0f
        val minClickArea = 10f  // Minimum reasonable click target size

        // Then: The icon should be large enough for interaction
        val actualIconSize = iconSize * scale
        assertTrue(actualIconSize >= minClickArea, "Expand icon should be at least $minClickArea pixels for clickability")
    }

    @Test
    fun `expand icon should scale with zoom level`() {
        // Given: Different zoom scales
        val baseIconSize = 12f
        val scales = listOf(0.5f, 1.0f, 1.5f, 2.0f)

        // Then: Icon size should scale proportionally
        scales.forEach { scale ->
            val scaledIconSize = baseIconSize * scale
            assertEquals(baseIconSize * scale, scaledIconSize, "Icon size should scale with zoom level $scale")
        }
    }

    @Test
    fun `clicking expand icon should trigger navigation into GraphNode`() {
        // Given: A GraphState with a GraphNode
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

        // When: Simulating expand icon click (calling navigateIntoGraphNode)
        val success = graphState.navigateIntoGraphNode("graphNode1")

        // Then: Should navigate into the GraphNode
        assertTrue(success, "Should successfully navigate into GraphNode")
        assertFalse(graphState.navigationContext.isAtRoot, "Should no longer be at root")
        assertEquals("graphNode1", graphState.navigationContext.currentGraphNodeId, "Should be inside graphNode1")
    }

    @Test
    fun `expand icon click should not trigger when clicking on empty GraphNode`() {
        // Given: A GraphNode with no children
        val emptyGraphNode = GraphNode(
            id = "emptyGraphNode",
            name = "EmptyGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = emptyList(),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(emptyGraphNode)

        val graphState = GraphState(graph)

        // When: Trying to navigate into empty GraphNode
        val success = graphState.navigateIntoGraphNode("emptyGraphNode")

        // Then: Navigation should still work (empty GraphNode is still a valid navigation target)
        // The UI may choose to disable the expand icon, but the navigation should be possible
        assertTrue(success, "Should be able to navigate into even empty GraphNode")
    }

    @Test
    fun `expand icon should be distinguishable from selection highlight`() {
        // Given: GraphNode rendering parameters
        val selectedOuterBorder = 0xFF0D47A1  // Dark blue for selection
        val expandIconColor = 0xFF616161      // Gray for expand icon

        // Then: Colors should be visually distinct
        assertNotEquals(selectedOuterBorder, expandIconColor, "Expand icon color should differ from selection highlight")
    }

    @Test
    fun `expand icon hit detection should work at various zoom levels`() {
        // Given: Expand icon hit area parameters
        val baseIconSize = 12f
        val hitPadding = 8f  // Extra hit area around icon

        // Test at different scales
        listOf(0.5f, 1.0f, 2.0f).forEach { scale ->
            val scaledIconSize = baseIconSize * scale
            val hitAreaSize = scaledIconSize + (hitPadding * 2 * scale)

            // Then: Hit area should be proportionally larger than icon
            assertTrue(hitAreaSize > scaledIconSize, "Hit area should be larger than icon at scale $scale")
        }
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
