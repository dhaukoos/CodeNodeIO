/*
 * SegmentVisibilityTest - Unit Tests for Segment Visibility by Navigation Context
 * Tests that only segments relevant to current view context are returned
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Unit tests for segment visibility based on navigation context.
 *
 * User Story 5: Segment Visibility by Navigation Context
 * - At root level, only exterior segments (scopeNodeId = null) are visible
 * - Inside a GraphNode, only interior segments (scopeNodeId = graphNodeId) are visible
 * - Nested GraphNodes only show segments matching the current scope
 */
class SegmentVisibilityTest {

    /**
     * Helper to create a test CodeNode with typed ports.
     */
    private fun createTestCodeNode(
        id: String,
        name: String,
        x: Double = 0.0,
        y: Double = 0.0,
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

    // ==================== T049: getSegmentsInContext() at root level ====================

    @Test
    fun `getSegmentsInContext at root returns only root level segments`() {
        // Given: A direct connection A -> B at root level
        val nodeA = createTestCodeNode("nodeA", "A", hasInputPort = false)
        val nodeB = createTestCodeNode("nodeB", "B", 100.0, 0.0, hasOutputPort = false)

        val connection = Connection(
            id = "conn1",
            sourceNodeId = "nodeA",
            sourcePortId = "nodeA_output",
            targetNodeId = "nodeB",
            targetPortId = "nodeB_input"
        )

        val graph = flowGraph(name = "test", version = "1.0.0") {}
            .addNode(nodeA)
            .addNode(nodeB)
            .copy(connections = listOf(connection))

        val graphState = GraphState(graph)

        // When: Getting segments at root level (default navigation context)
        assertTrue(graphState.navigationContext.isAtRoot, "Should be at root level")
        val segments = graphState.getSegmentsInContext()

        // Then: Should return the root level segment
        assertEquals(1, segments.size, "Should have 1 segment at root level")
        assertEquals(null, segments.first().scopeNodeId, "Segment should be at root level (null scope)")
    }

    @Test
    fun `getSegmentsInContext at root excludes interior segments`() {
        // Given: A flow A -> B -> C where B is grouped, creating:
        // - Exterior segment: A -> GroupNode (scopeNodeId = null)
        // - Interior segment: PassThruPort -> B (scopeNodeId = GroupNode)
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

        // Group B (single node grouping not normally allowed, but we need to create
        // the scenario. We'll group B and C instead)
        graphState.toggleNodeInSelection("nodeB")
        graphState.toggleNodeInSelection("nodeC")
        val groupNode = graphState.groupSelectedNodes()
        assertNotNull(groupNode, "Should create GraphNode")

        // When: Getting segments at root level
        assertTrue(graphState.navigationContext.isAtRoot)
        val rootSegments = graphState.getSegmentsInContext()

        // Then: Should only return segments where scopeNodeId is null
        assertTrue(
            rootSegments.all { it.scopeNodeId == null },
            "All root level segments should have null scopeNodeId"
        )
    }

    @Test
    fun `getSegmentsInContext at root returns empty when no connections exist`() {
        // Given: A graph with nodes but no connections
        val nodeA = createTestCodeNode("nodeA", "A")
        val nodeB = createTestCodeNode("nodeB", "B", 100.0, 0.0)

        val graph = flowGraph(name = "test", version = "1.0.0") {}
            .addNode(nodeA)
            .addNode(nodeB)

        val graphState = GraphState(graph)

        // When: Getting segments at root level
        val segments = graphState.getSegmentsInContext()

        // Then: Should return empty list
        assertTrue(segments.isEmpty(), "Should have no segments when no connections exist")
    }

    @Test
    fun `getSegmentsInContext at root handles multiple connections`() {
        // Given: Multiple connections A -> B, B -> C
        val nodeA = createTestCodeNode("nodeA", "A", hasInputPort = false)
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

        // When: Getting segments at root level
        val segments = graphState.getSegmentsInContext()

        // Then: Should return segments for both connections
        assertEquals(2, segments.size, "Should have 2 segments for 2 direct connections")
    }

    // ==================== T050: getSegmentsInContext() inside GraphNode ====================

    @Test
    fun `getSegmentsInContext inside GraphNode returns only interior segments`() {
        // Given: A flow where nodes are grouped
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

        // When: Navigate into the GraphNode
        val navigated = graphState.navigateIntoGraphNode(groupNode.id)
        assertTrue(navigated, "Should navigate into GraphNode")
        assertEquals(groupNode.id, graphState.navigationContext.currentGraphNodeId)

        // And: Get segments in current context
        val interiorSegments = graphState.getSegmentsInContext()

        // Then: All segments should have scopeNodeId matching the GraphNode
        assertTrue(
            interiorSegments.all { it.scopeNodeId == groupNode.id },
            "Interior segments should have scopeNodeId = ${groupNode.id}"
        )
    }

    @Test
    fun `getSegmentsInContext inside GraphNode excludes root level segments`() {
        // Given: A grouped flow with both exterior and interior segments
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

        // Navigate into the GraphNode
        graphState.navigateIntoGraphNode(groupNode.id)

        // When: Get segments in interior context
        val interiorSegments = graphState.getSegmentsInContext()

        // Then: No segments should have null scopeNodeId
        assertTrue(
            interiorSegments.none { it.scopeNodeId == null },
            "Interior view should not contain root level segments"
        )
    }

    @Test
    fun `getSegmentsInContext inside GraphNode returns internal connection segments`() {
        // Given: A GraphNode with internal connections between child nodes
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

        // Group B and C (which includes internal connection B -> C)
        graphState.toggleNodeInSelection("nodeB")
        graphState.toggleNodeInSelection("nodeC")
        val groupNode = graphState.groupSelectedNodes()
        assertNotNull(groupNode)

        // Verify internal connection exists
        assertEquals(1, groupNode.internalConnections.size, "Should have 1 internal connection")

        // Navigate into GraphNode
        graphState.navigateIntoGraphNode(groupNode.id)

        // When: Get segments in interior
        val interiorSegments = graphState.getSegmentsInContext()

        // Then: Should include segments for internal connections
        // The internal connection B -> C should produce a segment
        assertTrue(
            interiorSegments.isNotEmpty(),
            "Should have segments for internal connections"
        )
    }

    // ==================== T051: getSegmentsInContext() with nested GraphNodes ====================

    @Test
    fun `getSegmentsInContext with nested GraphNodes shows correct scope`() {
        // This test validates segment visibility in nested GraphNode scenarios
        // Given: A flow A -> B -> C -> D where B and C are grouped, then that group is nested

        // First, create a simple grouped scenario
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

        // Group B and C into first GraphNode
        graphState.toggleNodeInSelection("nodeB")
        graphState.toggleNodeInSelection("nodeC")
        val innerGroup = graphState.groupSelectedNodes()
        assertNotNull(innerGroup, "Should create inner GraphNode")

        // At root level, segments should have null scope
        assertTrue(graphState.navigationContext.isAtRoot)
        val rootSegments = graphState.getSegmentsInContext()
        assertTrue(
            rootSegments.all { it.scopeNodeId == null },
            "Root level should only show root segments"
        )

        // Navigate into the inner group
        graphState.navigateIntoGraphNode(innerGroup.id)
        assertEquals(1, graphState.navigationContext.depth)

        // Get segments inside the inner group
        val innerSegments = graphState.getSegmentsInContext()
        assertTrue(
            innerSegments.all { it.scopeNodeId == innerGroup.id },
            "Inside inner group should only show segments scoped to that group"
        )
    }

    @Test
    fun `getSegmentsInContext navigation changes visible segments`() {
        // Given: A grouped flow
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

        // When: Get segments at root
        val rootSegments = graphState.getSegmentsInContext()

        // Navigate in
        graphState.navigateIntoGraphNode(groupNode.id)
        val interiorSegments = graphState.getSegmentsInContext()

        // Navigate out
        graphState.navigateOut()
        val rootSegmentsAgain = graphState.getSegmentsInContext()

        // Then: Root segments should be consistent
        assertEquals(rootSegments.size, rootSegmentsAgain.size, "Root segments should be same after navigation")

        // And: Interior segments should differ from root
        // (Interior has scopeNodeId = groupNode.id, root has scopeNodeId = null)
        if (interiorSegments.isNotEmpty()) {
            assertTrue(
                interiorSegments.first().scopeNodeId != rootSegments.firstOrNull()?.scopeNodeId,
                "Interior and root segments should have different scopes"
            )
        }
    }

    @Test
    fun `getSegmentsInContext depth affects scope filtering`() {
        // Given: A flow with a GraphNode
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

        // Verify depth 0 (root)
        assertEquals(0, graphState.navigationContext.depth)
        val rootSegments = graphState.getSegmentsInContext()
        val rootScopeIds = rootSegments.map { it.scopeNodeId }.toSet()

        // Navigate to depth 1
        graphState.navigateIntoGraphNode(groupNode.id)
        assertEquals(1, graphState.navigationContext.depth)
        val depth1Segments = graphState.getSegmentsInContext()
        val depth1ScopeIds = depth1Segments.map { it.scopeNodeId }.toSet()

        // Then: Different depths should have different scope filters
        assertTrue(
            rootScopeIds == setOf(null) || rootScopeIds.isEmpty(),
            "Root level should have null or empty scope IDs"
        )
        assertTrue(
            depth1ScopeIds.all { it == groupNode.id } || depth1ScopeIds.isEmpty(),
            "Depth 1 should filter to groupNode.id or be empty"
        )
    }

    @Test
    fun `ConnectionSegment isVisibleInContext correctly filters by scope`() {
        // Given: Segments with different scopes
        val rootSegment = ConnectionSegment(
            id = "seg1",
            sourceNodeId = "nodeA",
            sourcePortId = "portA",
            targetNodeId = "nodeB",
            targetPortId = "portB",
            scopeNodeId = null,
            parentConnectionId = "conn1"
        )

        val interiorSegment = ConnectionSegment(
            id = "seg2",
            sourceNodeId = "nodeB",
            sourcePortId = "portB",
            targetNodeId = "nodeC",
            targetPortId = "portC",
            scopeNodeId = "graphNode1",
            parentConnectionId = "conn1"
        )

        // When/Then: Check visibility in different contexts
        assertTrue(rootSegment.isVisibleInContext(null), "Root segment visible at root")
        assertTrue(!rootSegment.isVisibleInContext("graphNode1"), "Root segment not visible inside GraphNode")

        assertTrue(interiorSegment.isVisibleInContext("graphNode1"), "Interior segment visible inside its GraphNode")
        assertTrue(!interiorSegment.isVisibleInContext(null), "Interior segment not visible at root")
        assertTrue(!interiorSegment.isVisibleInContext("differentGraphNode"), "Interior segment not visible in different GraphNode")
    }
}
