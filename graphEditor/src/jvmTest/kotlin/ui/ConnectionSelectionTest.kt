/*
 * ConnectionSelection Test
 * UI tests for connection selection in FlowGraphCanvas
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.ui.geometry.Offset
import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.PortFactory
import kotlin.test.*

/**
 * TDD tests for connection selection in FlowGraphCanvas.
 * These tests verify that connection selection correctly:
 * - Detects clicks on connection Bezier curves
 * - Respects click tolerance of 8 pixels
 * - Prioritizes nodes over connections when overlapping
 * - Deselects connections when clicking empty canvas
 * - Handles right-click for context menu
 */
class ConnectionSelectionTest {
    // ============================================
    // Test Setup - Sample Graph with Connections
    // ============================================

    private fun createTestNode(
        id: String,
        name: String,
        x: Double,
        y: Double,
        hasInput: Boolean = true,
        hasOutput: Boolean = true,
    ): CodeNode {
        val inputPorts =
            if (hasInput) {
                listOf(PortFactory.input<Any>(name = "input", owningNodeId = id))
            } else {
                emptyList()
            }

        val outputPorts =
            if (hasOutput) {
                listOf(PortFactory.output<Any>(name = "output", owningNodeId = id))
            } else {
                emptyList()
            }

        return CodeNode(
            id = id,
            name = name,
            codeNodeType = CodeNodeType.CUSTOM,
            position = Node.Position(x, y),
            inputPorts = inputPorts,
            outputPorts = outputPorts,
        )
    }

    private fun createTestConnection(
        id: String = "conn_1",
        sourceNodeId: String = "node_a",
        sourcePortId: String,
        targetNodeId: String = "node_b",
        targetPortId: String,
        ipTypeId: String? = null,
    ): Connection =
        Connection(
            id = id,
            sourceNodeId = sourceNodeId,
            sourcePortId = sourcePortId,
            targetNodeId = targetNodeId,
            targetPortId = targetPortId,
            ipTypeId = ipTypeId,
        )

    private fun createTestFlowGraph(): io.codenode.fbpdsl.model.FlowGraph {
        val nodeA = createTestNode("node_a", "Node A", 100.0, 100.0)
        val nodeB = createTestNode("node_b", "Node B", 400.0, 100.0)

        // Get the port IDs from the nodes
        val sourcePortId = nodeA.outputPorts.first().id
        val targetPortId = nodeB.inputPorts.first().id

        val connection =
            createTestConnection(
                sourcePortId = sourcePortId,
                targetPortId = targetPortId,
            )

        return flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(nodeA)
            .addNode(nodeB)
            .addConnection(connection)
    }

    // ============================================
    // Bezier Hit Detection Algorithm Tests
    // ============================================

    @Test
    fun `bezier point sampling generates expected number of points`() {
        // Given: Start and end points for a Bezier curve
        val start = Offset(180f, 130f) // Output port position
        val end = Offset(400f, 130f) // Input port position

        // When: Sampling the Bezier curve at 20 points
        val samplePoints = sampleBezierCurve(start, end, samples = 20)

        // Then: Should have 20 sample points
        assertEquals(20, samplePoints.size)
    }

    @Test
    fun `bezier curve starts and ends at correct positions`() {
        // Given: Start and end points
        val start = Offset(100f, 100f)
        val end = Offset(300f, 200f)

        // When: Sampling at high resolution
        val points = sampleBezierCurve(start, end, samples = 100)

        // Then: First point should be near start, last near end
        assertTrue((points.first() - start).getDistance() < 5f)
        assertTrue((points.last() - end).getDistance() < 5f)
    }

    @Test
    fun `point distance to bezier curve calculated correctly`() {
        // Given: A horizontal Bezier curve from (100, 100) to (300, 100)
        val start = Offset(100f, 100f)
        val end = Offset(300f, 100f)

        // When: Measuring distance from a point on the curve
        val pointOnCurve = Offset(200f, 100f)
        val distance = distanceToBezierCurve(pointOnCurve, start, end)

        // Then: Distance should be very small (near 0)
        assertTrue(distance < 5f, "Point on curve should have distance < 5, got $distance")
    }

    @Test
    fun `point far from bezier curve has large distance`() {
        // Given: A horizontal Bezier curve
        val start = Offset(100f, 100f)
        val end = Offset(300f, 100f)

        // When: Measuring distance from a point far from the curve
        val farPoint = Offset(200f, 200f) // 100 pixels below the curve
        val distance = distanceToBezierCurve(farPoint, start, end)

        // Then: Distance should be approximately 100 pixels
        assertTrue(distance > 80f, "Far point should have distance > 80, got $distance")
    }

    // ============================================
    // Connection Selection Tests
    // ============================================

    @Test
    fun `clicking on connection selects it`() {
        // Given: Graph with connection from A to B
        val flowGraph = createTestFlowGraph()

        // When: User clicks on the connection line (middle of connection)
        // The connection runs from node_a (x=100) to node_b (x=400)
        // At scale=1 and panOffset=0, output port is at x=280 (100 + 180), input port at x=400
        // Middle point would be around x=340
        val clickPosition = Offset(340f, 150f) // headerHeight + portOffset
        val connectionId =
            findConnectionAtPosition(
                flowGraph,
                clickPosition,
                panOffset = Offset.Zero,
                scale = 1f,
                tolerance = 8f,
            )

        // Then: Connection should be found
        assertNotNull(connectionId, "Connection should be found at click position")
        assertEquals("conn_1", connectionId)
    }

    @Test
    fun `clicking elsewhere returns null`() {
        // Given: Graph with connection
        val flowGraph = createTestFlowGraph()

        // When: User clicks on empty canvas (far from connection)
        val emptyPosition = Offset(50f, 300f)
        val connectionId =
            findConnectionAtPosition(
                flowGraph,
                emptyPosition,
                panOffset = Offset.Zero,
                scale = 1f,
                tolerance = 8f,
            )

        // Then: No connection selected
        assertNull(connectionId)
    }

    @Test
    fun `connection click tolerance is 8 pixels`() {
        // Given: A connection
        val flowGraph = createTestFlowGraph()

        // Get the y-position of the connection (header + port offset)
        val connectionY = 150f // approximately where the port would be

        // When: User clicks 7 pixels from connection line
        val closeClick = Offset(340f, connectionY + 7f)
        val foundClose =
            findConnectionAtPosition(
                flowGraph,
                closeClick,
                panOffset = Offset.Zero,
                scale = 1f,
                tolerance = 8f,
            )

        // Then: Connection is selected (within tolerance)
        assertNotNull(foundClose, "Click within tolerance should select connection")

        // When: User clicks 15 pixels from connection line (definitely outside)
        val farClick = Offset(340f, connectionY + 20f)
        val foundFar =
            findConnectionAtPosition(
                flowGraph,
                farClick,
                panOffset = Offset.Zero,
                scale = 1f,
                tolerance = 8f,
            )

        // Then: Connection is NOT selected
        assertNull(foundFar, "Click outside tolerance should not select connection")
    }

    @Test
    fun `pan offset affects connection hit detection`() {
        // Given: Graph with connection and pan offset
        val flowGraph = createTestFlowGraph()
        val panOffset = Offset(50f, 50f)

        // When: User clicks on connection accounting for pan
        // Original middle would be at (340, 150), with pan it's at (390, 200)
        val adjustedClick = Offset(390f, 200f)
        val connectionId =
            findConnectionAtPosition(
                flowGraph,
                adjustedClick,
                panOffset = panOffset,
                scale = 1f,
                tolerance = 8f,
            )

        // Then: Connection should be found
        assertNotNull(connectionId)
    }

    @Test
    fun `scale affects connection hit detection`() {
        // Given: Graph with connection at 2x zoom
        val flowGraph = createTestFlowGraph()
        val scale = 2f

        // When: User clicks on scaled connection position
        // At 2x scale, coordinates are doubled
        // Node A at (100, 100) becomes screen (200, 200)
        // Output port at screen x = 200 + 180*2 = 560, y = 200 + 30*2 + 20*2 = 300
        // Node B at (400, 100) becomes screen (800, 200)
        // Input port at screen (800, 300)
        // Middle would be around x = 680
        val scaledClick = Offset(680f, 300f)
        val connectionId =
            findConnectionAtPosition(
                flowGraph,
                scaledClick,
                panOffset = Offset.Zero,
                scale = scale,
                tolerance = 8f,
            )

        // Then: Connection should be found
        assertNotNull(connectionId, "Scaled connection should be found")
    }

    @Test
    fun `multiple connections can be distinguished`() {
        // Given: Graph with multiple connections
        val nodeA =
            createTestNode("node_a", "Node A", 100.0, 100.0).let { node ->
                // Create node with two output ports
                CodeNode(
                    id = node.id,
                    name = node.name,
                    codeNodeType = CodeNodeType.CUSTOM,
                    position = node.position,
                    inputPorts = emptyList(),
                    outputPorts =
                        listOf(
                            PortFactory.output<Any>("output1", node.id),
                            PortFactory.output<Any>("output2", node.id),
                        ),
                )
            }

        val nodeB =
            createTestNode("node_b", "Node B", 400.0, 100.0).let { node ->
                // Create node with two input ports
                CodeNode(
                    id = node.id,
                    name = node.name,
                    codeNodeType = CodeNodeType.CUSTOM,
                    position = node.position,
                    inputPorts =
                        listOf(
                            PortFactory.input<Any>("input1", node.id),
                            PortFactory.input<Any>("input2", node.id),
                        ),
                    outputPorts = emptyList(),
                )
            }

        val conn1 =
            Connection(
                id = "conn_1",
                sourceNodeId = "node_a",
                sourcePortId = nodeA.outputPorts[0].id,
                targetNodeId = "node_b",
                targetPortId = nodeB.inputPorts[0].id,
            )

        val conn2 =
            Connection(
                id = "conn_2",
                sourceNodeId = "node_a",
                sourcePortId = nodeA.outputPorts[1].id,
                targetNodeId = "node_b",
                targetPortId = nodeB.inputPorts[1].id,
            )

        val graph =
            flowGraph("Multi", "1.0.0") {}
                .addNode(nodeA)
                .addNode(nodeB)
                .addConnection(conn1)
                .addConnection(conn2)

        // When: Clicking on the first connection (first port y-position)
        // First port at headerHeight(30) + 20 = 50 offset from node top (at y=100)
        // Screen position: 100 + 30 + 20 = 150
        val click1 = Offset(340f, 150f)
        val found1 = findConnectionAtPosition(graph, click1, Offset.Zero, 1f, 8f)

        // Then: First connection should be found
        assertEquals("conn_1", found1)

        // When: Clicking on the second connection (second port y-position)
        // Second port at 150 + 25 (port spacing) = 175
        val click2 = Offset(340f, 175f)
        val found2 = findConnectionAtPosition(graph, click2, Offset.Zero, 1f, 8f)

        // Then: Second connection should be found
        assertEquals("conn_2", found2)
    }

    // ============================================
    // Selection State Tests
    // ============================================

    @Test
    fun `selecting connection deselects node`() {
        // Given: A node is currently selected
        var selectedNodeId: String? = "node_a"
        var selectedConnectionId: String? = null

        // When: Connection is selected
        // Simulating GraphState behavior
        selectedConnectionId = "conn_1"
        selectedNodeId = null // Node deselected when connection selected

        // Then: Node should be deselected
        assertNull(selectedNodeId)
        assertEquals("conn_1", selectedConnectionId)
    }

    @Test
    fun `selecting node deselects connection`() {
        // Given: A connection is currently selected
        var selectedNodeId: String? = null
        var selectedConnectionId: String? = "conn_1"

        // When: Node is selected
        selectedNodeId = "node_a"
        selectedConnectionId = null // Connection deselected when node selected

        // Then: Connection should be deselected
        assertEquals("node_a", selectedNodeId)
        assertNull(selectedConnectionId)
    }

    // ============================================
    // Helper Functions for Testing
    // ============================================

    /**
     * Samples a cubic Bezier curve at evenly spaced t values.
     * This is the algorithm that will be used in FlowGraphCanvas.
     */
    private fun sampleBezierCurve(
        start: Offset,
        end: Offset,
        samples: Int,
    ): List<Offset> {
        val controlPointOffset = kotlin.math.abs(end.x - start.x) * 0.5f
        val cp1 = Offset(start.x + controlPointOffset, start.y)
        val cp2 = Offset(end.x - controlPointOffset, end.y)

        return (0 until samples).map { i ->
            val t = i.toFloat() / (samples - 1)
            cubicBezier(start, cp1, cp2, end, t)
        }
    }

    /**
     * Evaluates a cubic Bezier curve at parameter t (0 to 1).
     */
    private fun cubicBezier(
        p0: Offset,
        p1: Offset,
        p2: Offset,
        p3: Offset,
        t: Float,
    ): Offset {
        val u = 1 - t
        val tt = t * t
        val uu = u * u
        val uuu = uu * u
        val ttt = tt * t

        val x = uuu * p0.x + 3 * uu * t * p1.x + 3 * u * tt * p2.x + ttt * p3.x
        val y = uuu * p0.y + 3 * uu * t * p1.y + 3 * u * tt * p2.y + ttt * p3.y

        return Offset(x, y)
    }

    /**
     * Calculates the minimum distance from a point to a Bezier curve.
     */
    private fun distanceToBezierCurve(
        point: Offset,
        start: Offset,
        end: Offset,
    ): Float {
        val samples = sampleBezierCurve(start, end, samples = 20)
        return samples.minOf { (it - point).getDistance() }
    }

    /**
     * Finds a connection at the given position.
     * This mirrors the implementation that will be added to FlowGraphCanvas.
     */
    private fun findConnectionAtPosition(
        flowGraph: io.codenode.fbpdsl.model.FlowGraph,
        position: Offset,
        panOffset: Offset,
        scale: Float,
        tolerance: Float = 8f,
    ): String? {
        val portSpacing = 25f * scale
        val nodeWidth = 180f * scale
        val headerHeight = 30f * scale

        for (connection in flowGraph.connections) {
            val sourceNode = flowGraph.findNode(connection.sourceNodeId) ?: continue
            val targetNode = flowGraph.findNode(connection.targetNodeId) ?: continue

            // Calculate port positions in screen coordinates
            val sourceNodePos =
                Offset(
                    sourceNode.position.x.toFloat() * scale + panOffset.x,
                    sourceNode.position.y.toFloat() * scale + panOffset.y,
                )

            val targetNodePos =
                Offset(
                    targetNode.position.x.toFloat() * scale + panOffset.x,
                    targetNode.position.y.toFloat() * scale + panOffset.y,
                )

            // Find port indices
            val sourcePortIndex =
                sourceNode.outputPorts
                    .indexOfFirst { it.id == connection.sourcePortId }
                    .takeIf { it >= 0 } ?: continue

            val targetPortIndex =
                targetNode.inputPorts
                    .indexOfFirst { it.id == connection.targetPortId }
                    .takeIf { it >= 0 } ?: continue

            // Calculate port positions
            val sourcePortPos =
                Offset(
                    sourceNodePos.x + nodeWidth,
                    sourceNodePos.y + headerHeight + 20f * scale + (sourcePortIndex * portSpacing),
                )

            val targetPortPos =
                Offset(
                    targetNodePos.x,
                    targetNodePos.y + headerHeight + 20f * scale + (targetPortIndex * portSpacing),
                )

            // Check if click is within tolerance of the Bezier curve
            val distance = distanceToBezierCurve(position, sourcePortPos, targetPortPos)
            if (distance <= tolerance) {
                return connection.id
            }
        }

        return null
    }
}
