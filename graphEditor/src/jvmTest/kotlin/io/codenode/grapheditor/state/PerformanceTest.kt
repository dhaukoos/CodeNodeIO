/*
 * PerformanceTest - Performance Tests for PassThruPort and ConnectionSegment
 * Verifies that critical operations meet performance targets
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.*
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.measureTime

/**
 * Performance tests for PassThruPort and ConnectionSegment feature.
 *
 * T071: Performance test: PassThruPort creation < 500ms
 * T072: Performance test: segment visibility switching < 100ms
 */
class PerformanceTest {

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

    // ==================== T071: PassThruPort creation < 500ms ====================

    @Test
    fun `PassThruPort creation completes within 500ms for moderate graph`() {
        // Given: A moderate-sized graph with 20 nodes and connections
        val nodes = (0 until 20).map { i ->
            createTestCodeNode(
                id = "node$i",
                name = "Node$i",
                x = (i % 5) * 100.0,
                y = (i / 5) * 100.0,
                hasInputPort = i > 0,  // First node has no input
                hasOutputPort = i < 19  // Last node has no output
            )
        }

        // Create connections between adjacent nodes
        val connections = (0 until 19).map { i ->
            Connection(
                id = "conn$i",
                sourceNodeId = "node$i",
                sourcePortId = "node${i}_output",
                targetNodeId = "node${i + 1}",
                targetPortId = "node${i + 1}_input"
            )
        }

        var graph = flowGraph(name = "test", version = "1.0.0") {}
        nodes.forEach { node -> graph = graph.addNode(node) }
        graph = graph.copy(connections = connections)

        val graphState = GraphState(graph)

        // Select 10 nodes for grouping (half the graph)
        (5 until 15).forEach { i ->
            graphState.toggleNodeInSelection("node$i")
        }

        // When: Measure time to create PassThruPorts via grouping
        val duration = measureTime {
            val groupNode = graphState.groupSelectedNodes()
            assertNotNull(groupNode, "Group should be created")
        }

        // Then: Should complete within 500ms
        val durationMs = duration.inWholeMilliseconds
        assertTrue(
            durationMs < 500,
            "PassThruPort creation should complete within 500ms, took ${durationMs}ms"
        )
        println("PassThruPort creation for 10 nodes: ${durationMs}ms")
    }

    @Test
    fun `PassThruPort creation completes within 500ms for complex graph with multiple boundary crossings`() {
        // Given: A graph with many cross-connections that will create multiple PassThruPorts
        val nodeA1 = createTestCodeNode("a1", "A1", 0.0, 0.0, hasInputPort = false)
        val nodeA2 = createTestCodeNode("a2", "A2", 0.0, 100.0, hasInputPort = false)
        val nodeA3 = createTestCodeNode("a3", "A3", 0.0, 200.0, hasInputPort = false)

        // Middle nodes to be grouped
        val middleNodes = (0 until 10).map { i ->
            createTestCodeNode(
                id = "m$i",
                name = "M$i",
                x = 100.0 + (i % 2) * 50,
                y = i * 50.0
            )
        }

        val nodeB1 = createTestCodeNode("b1", "B1", 250.0, 0.0, hasOutputPort = false)
        val nodeB2 = createTestCodeNode("b2", "B2", 250.0, 100.0, hasOutputPort = false)
        val nodeB3 = createTestCodeNode("b3", "B3", 250.0, 200.0, hasOutputPort = false)

        // Create many connections
        val connections = mutableListOf<Connection>()

        // A nodes to middle nodes
        connections.add(Connection("c1", "a1", "a1_output", "m0", "m0_input"))
        connections.add(Connection("c2", "a2", "a2_output", "m3", "m3_input"))
        connections.add(Connection("c3", "a3", "a3_output", "m6", "m6_input"))

        // Internal connections between middle nodes
        for (i in 0 until 9) {
            connections.add(Connection("im$i", "m$i", "m${i}_output", "m${i+1}", "m${i+1}_input"))
        }

        // Middle nodes to B nodes
        connections.add(Connection("c4", "m2", "m2_output", "b1", "b1_input"))
        connections.add(Connection("c5", "m5", "m5_output", "b2", "b2_input"))
        connections.add(Connection("c6", "m9", "m9_output", "b3", "b3_input"))

        var graph = flowGraph(name = "test", version = "1.0.0") {}
        graph = graph.addNode(nodeA1).addNode(nodeA2).addNode(nodeA3)
        middleNodes.forEach { graph = graph.addNode(it) }
        graph = graph.addNode(nodeB1).addNode(nodeB2).addNode(nodeB3)
        graph = graph.copy(connections = connections)

        val graphState = GraphState(graph)

        // Select all middle nodes
        middleNodes.forEach { graphState.toggleNodeInSelection(it.id) }

        // When: Measure time to create PassThruPorts
        val duration = measureTime {
            val groupNode = graphState.groupSelectedNodes()
            assertNotNull(groupNode, "Group should be created with multiple PassThruPorts")
        }

        // Then: Should complete within 500ms
        val durationMs = duration.inWholeMilliseconds
        assertTrue(
            durationMs < 500,
            "PassThruPort creation with multiple boundaries should complete within 500ms, took ${durationMs}ms"
        )
        println("PassThruPort creation with multiple boundaries: ${durationMs}ms")
    }

    // ==================== T072: Segment visibility switching < 100ms ====================

    @Test
    fun `segment visibility switching completes within 100ms`() {
        // Given: A graph with a GraphNode containing many internal connections
        val externalNode = createTestCodeNode("ext", "External", 0.0, 0.0, hasInputPort = false)

        val internalNodes = (0 until 10).map { i ->
            createTestCodeNode(
                id = "int$i",
                name = "Internal$i",
                x = 100.0 + (i % 2) * 50,
                y = i * 50.0
            )
        }

        val externalTarget = createTestCodeNode("target", "Target", 300.0, 0.0, hasOutputPort = false)

        // Internal connections
        val internalConnections = (0 until 9).map { i ->
            Connection("ic$i", "int$i", "int${i}_output", "int${i+1}", "int${i+1}_input")
        }

        // External connections
        val allConnections = internalConnections + listOf(
            Connection("ext_conn", "ext", "ext_output", "int0", "int0_input"),
            Connection("out_conn", "int9", "int9_output", "target", "target_input")
        )

        var graph = flowGraph(name = "test", version = "1.0.0") {}
        graph = graph.addNode(externalNode)
        internalNodes.forEach { graph = graph.addNode(it) }
        graph = graph.addNode(externalTarget)
        graph = graph.copy(connections = allConnections)

        val graphState = GraphState(graph)

        // Group internal nodes
        internalNodes.forEach { graphState.toggleNodeInSelection(it.id) }
        val groupNode = graphState.groupSelectedNodes()
        assertNotNull(groupNode)

        // Measure navigation into the GraphNode (segment visibility switching)
        val navigateInDuration = measureTime {
            graphState.navigateIntoGraphNode(groupNode.id)
            // Force segment computation
            val segments = graphState.getSegmentsInContext()
            assertTrue(segments.isNotEmpty(), "Should have segments in internal view")
        }

        // Then: Navigation and segment visibility should be fast
        val navigateInMs = navigateInDuration.inWholeMilliseconds
        assertTrue(
            navigateInMs < 100,
            "Navigate into GraphNode should complete within 100ms, took ${navigateInMs}ms"
        )
        println("Navigate into GraphNode: ${navigateInMs}ms")

        // Measure navigation out of the GraphNode
        val navigateOutDuration = measureTime {
            graphState.navigateOut()
            // Force segment recomputation
            val segments = graphState.getSegmentsInContext()
            assertTrue(segments.isNotEmpty(), "Should have segments in external view")
        }

        val navigateOutMs = navigateOutDuration.inWholeMilliseconds
        assertTrue(
            navigateOutMs < 100,
            "Navigate out of GraphNode should complete within 100ms, took ${navigateOutMs}ms"
        )
        println("Navigate out of GraphNode: ${navigateOutMs}ms")
    }

    @Test
    fun `getSegmentsInContext is fast for nested GraphNodes`() {
        // Given: A nested graph structure (GraphNode containing another GraphNode)
        val node1 = createTestCodeNode("n1", "N1", 0.0, 0.0, hasInputPort = false)
        val node2 = createTestCodeNode("n2", "N2", 100.0, 0.0)
        val node3 = createTestCodeNode("n3", "N3", 200.0, 0.0)
        val node4 = createTestCodeNode("n4", "N4", 300.0, 0.0, hasOutputPort = false)

        val connections = listOf(
            Connection("c1", "n1", "n1_output", "n2", "n2_input"),
            Connection("c2", "n2", "n2_output", "n3", "n3_input"),
            Connection("c3", "n3", "n3_output", "n4", "n4_input")
        )

        var graph = flowGraph(name = "test", version = "1.0.0") {}
        graph = graph.addNode(node1).addNode(node2).addNode(node3).addNode(node4)
        graph = graph.copy(connections = connections)

        val graphState = GraphState(graph)

        // Create first level GraphNode (group n2 and n3)
        graphState.toggleNodeInSelection("n2")
        graphState.toggleNodeInSelection("n3")
        val outerGroup = graphState.groupSelectedNodes()
        assertNotNull(outerGroup)

        // Navigate into the outer group
        graphState.navigateIntoGraphNode(outerGroup.id)

        // Measure repeated segment visibility queries
        var totalMs = 0L
        val iterations = 100

        repeat(iterations) {
            val duration = measureTime {
                graphState.getSegmentsInContext()
            }
            totalMs += duration.inWholeMilliseconds
        }

        val averageMs = totalMs / iterations
        assertTrue(
            averageMs < 10,  // Should be well under 100ms per call
            "getSegmentsInContext average should be under 10ms, got ${averageMs}ms"
        )
        println("getSegmentsInContext average over $iterations calls: ${averageMs}ms")
    }

    @Test
    fun `connection segment computation is fast`() {
        // Given: A connection that may cross boundaries
        val connection = Connection(
            id = "test_conn",
            sourceNodeId = "source",
            sourcePortId = "out",
            targetNodeId = "target",
            targetPortId = "in"
        )

        // Measure segment computation time
        var totalMs = 0L
        val iterations = 1000

        repeat(iterations) {
            connection.invalidateSegments()
            val duration = measureTime {
                val segments = connection.segments
                assertTrue(segments.isNotEmpty())
            }
            totalMs += duration.inWholeMilliseconds
        }

        val averageMs = totalMs.toDouble() / iterations
        assertTrue(
            averageMs < 1,  // Should be sub-millisecond
            "Segment computation should be sub-millisecond, got ${averageMs}ms average"
        )
        println("Segment computation average over $iterations calls: ${averageMs}ms")
    }
}
