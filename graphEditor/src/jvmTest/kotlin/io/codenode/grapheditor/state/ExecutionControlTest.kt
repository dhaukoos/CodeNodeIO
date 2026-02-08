/*
 * ExecutionControl Tests
 * TDD tests for GraphState execution control operations
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.ControlConfig
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port
import kotlin.test.*

/**
 * TDD tests for GraphState execution control operations.
 * Tests T065-T069 for Phase 8: graphEditor Integration.
 */
class ExecutionControlTest {

    // ============================================
    // Helper Functions
    // ============================================

    private fun createTestCodeNode(
        id: String,
        name: String,
        x: Double = 0.0,
        y: Double = 0.0,
        executionState: ExecutionState = ExecutionState.IDLE,
        controlConfig: ControlConfig = ControlConfig()
    ): CodeNode {
        return CodeNode(
            id = id,
            name = name,
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(x, y),
            inputPorts = listOf(
                Port(
                    id = "${id}_input",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = id
                )
            ),
            outputPorts = listOf(
                Port(
                    id = "${id}_output",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = id
                )
            ),
            executionState = executionState,
            controlConfig = controlConfig
        )
    }

    private fun createTestGraphNode(
        id: String,
        name: String,
        children: List<Node>,
        x: Double = 0.0,
        y: Double = 0.0,
        executionState: ExecutionState = ExecutionState.IDLE,
        controlConfig: ControlConfig = ControlConfig()
    ): GraphNode {
        return GraphNode(
            id = id,
            name = name,
            position = Node.Position(x, y),
            childNodes = children.map { child ->
                when (child) {
                    is CodeNode -> child.copy(parentNodeId = id)
                    is GraphNode -> child.copy(parentNodeId = id)
                }
            },
            inputPorts = listOf(
                Port(
                    id = "${id}_input",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = id
                )
            ),
            outputPorts = listOf(
                Port(
                    id = "${id}_output",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = id
                )
            ),
            portMappings = mapOf(
                "input" to GraphNode.PortMapping(children.first().id, "input"),
                "output" to GraphNode.PortMapping(children.last().id, "output")
            ),
            executionState = executionState,
            controlConfig = controlConfig
        )
    }

    // ============================================
    // T065: Tests for setNodeExecutionState()
    // ============================================

    @Test
    fun `setNodeExecutionState should change node state to RUNNING`() {
        // Given: A graph with a CodeNode in IDLE state
        val node = createTestCodeNode("node1", "Processor")
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node)
        val graphState = GraphState(graph)

        // When: Setting execution state to RUNNING
        val success = graphState.setNodeExecutionState("node1", ExecutionState.RUNNING)

        // Then: Node should be in RUNNING state
        assertTrue(success, "setNodeExecutionState should return true")
        val updatedNode = graphState.flowGraph.findNode("node1")
        assertEquals(ExecutionState.RUNNING, updatedNode?.executionState,
            "Node should be in RUNNING state")
    }

    @Test
    fun `setNodeExecutionState should change node state to PAUSED`() {
        // Given: A graph with a CodeNode in RUNNING state
        val node = createTestCodeNode("node1", "Processor", executionState = ExecutionState.RUNNING)
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node)
        val graphState = GraphState(graph)

        // When: Setting execution state to PAUSED
        val success = graphState.setNodeExecutionState("node1", ExecutionState.PAUSED)

        // Then: Node should be in PAUSED state
        assertTrue(success, "setNodeExecutionState should return true")
        val updatedNode = graphState.flowGraph.findNode("node1")
        assertEquals(ExecutionState.PAUSED, updatedNode?.executionState,
            "Node should be in PAUSED state")
    }

    @Test
    fun `setNodeExecutionState should return false for non-existent node`() {
        // Given: A graph without the target node
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
        val graphState = GraphState(graph)

        // When: Setting execution state for non-existent node
        val success = graphState.setNodeExecutionState("nonexistent", ExecutionState.RUNNING)

        // Then: Should return false
        assertFalse(success, "setNodeExecutionState should return false for non-existent node")
    }

    @Test
    fun `setNodeExecutionState should propagate state to GraphNode children`() {
        // Given: A GraphNode with child nodes
        val child1 = createTestCodeNode("child1", "Child1")
        val child2 = createTestCodeNode("child2", "Child2")
        val graphNode = createTestGraphNode("graphNode1", "Container", listOf(child1, child2))
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)
        val graphState = GraphState(graph)

        // When: Setting execution state on the GraphNode
        val success = graphState.setNodeExecutionState("graphNode1", ExecutionState.RUNNING)

        // Then: All children should also be RUNNING
        assertTrue(success, "setNodeExecutionState should return true")
        val updatedGraphNode = graphState.flowGraph.findNode("graphNode1") as GraphNode
        assertEquals(ExecutionState.RUNNING, updatedGraphNode.executionState)
        updatedGraphNode.childNodes.forEach { child ->
            assertEquals(ExecutionState.RUNNING, child.executionState,
                "Child ${child.id} should be RUNNING")
        }
    }

    @Test
    fun `setNodeExecutionState should mark graph as dirty`() {
        // Given: A graph with a CodeNode
        val node = createTestCodeNode("node1", "Processor")
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node)
        val graphState = GraphState(graph)
        assertFalse(graphState.isDirty, "Graph should not be dirty initially")

        // When: Setting execution state
        graphState.setNodeExecutionState("node1", ExecutionState.RUNNING)

        // Then: Graph should be marked dirty
        assertTrue(graphState.isDirty, "Graph should be marked dirty after state change")
    }

    // ============================================
    // T065: Tests for setNodeControlConfig()
    // ============================================

    @Test
    fun `setNodeControlConfig should update node config`() {
        // Given: A graph with a CodeNode with default config
        val node = createTestCodeNode("node1", "Processor")
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node)
        val graphState = GraphState(graph)

        // When: Setting new control config
        val newConfig = ControlConfig(
            pauseBufferSize = 200,
            speedAttenuation = 500L,
            independentControl = true
        )
        val success = graphState.setNodeControlConfig("node1", newConfig)

        // Then: Node should have updated config
        assertTrue(success, "setNodeControlConfig should return true")
        val updatedNode = graphState.flowGraph.findNode("node1")
        assertEquals(200, updatedNode?.controlConfig?.pauseBufferSize)
        assertEquals(500L, updatedNode?.controlConfig?.speedAttenuation)
        assertTrue(updatedNode?.controlConfig?.independentControl == true)
    }

    @Test
    fun `setNodeControlConfig should return false for non-existent node`() {
        // Given: A graph without the target node
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
        val graphState = GraphState(graph)

        // When: Setting config for non-existent node
        val newConfig = ControlConfig(pauseBufferSize = 200)
        val success = graphState.setNodeControlConfig("nonexistent", newConfig)

        // Then: Should return false
        assertFalse(success, "setNodeControlConfig should return false for non-existent node")
    }

    @Test
    fun `setNodeControlConfig should propagate to GraphNode children`() {
        // Given: A GraphNode with child nodes
        val child1 = createTestCodeNode("child1", "Child1")
        val child2 = createTestCodeNode("child2", "Child2")
        val graphNode = createTestGraphNode("graphNode1", "Container", listOf(child1, child2))
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)
        val graphState = GraphState(graph)

        // When: Setting control config on the GraphNode
        val newConfig = ControlConfig(speedAttenuation = 1000L)
        val success = graphState.setNodeControlConfig("graphNode1", newConfig)

        // Then: Config should propagate to children
        assertTrue(success, "setNodeControlConfig should return true")
        val updatedGraphNode = graphState.flowGraph.findNode("graphNode1") as GraphNode
        assertEquals(1000L, updatedGraphNode.controlConfig.speedAttenuation)
        updatedGraphNode.childNodes.forEach { child ->
            assertEquals(1000L, child.controlConfig.speedAttenuation,
                "Child ${child.id} should have propagated speedAttenuation")
        }
    }

    @Test
    fun `setNodeControlConfig should mark graph as dirty`() {
        // Given: A graph with a CodeNode
        val node = createTestCodeNode("node1", "Processor")
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node)
        val graphState = GraphState(graph)
        assertFalse(graphState.isDirty, "Graph should not be dirty initially")

        // When: Setting control config
        graphState.setNodeControlConfig("node1", ControlConfig(pauseBufferSize = 500))

        // Then: Graph should be marked dirty
        assertTrue(graphState.isDirty, "Graph should be marked dirty after config change")
    }

    // ============================================
    // T065: Tests for findNodeById()
    // ============================================

    @Test
    fun `findNodeById should find root node`() {
        // Given: A graph with a root CodeNode
        val node = createTestCodeNode("node1", "Processor")
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node)
        val graphState = GraphState(graph)

        // When: Finding by ID
        val foundNode = graphState.findNodeById("node1")

        // Then: Should return the node
        assertNotNull(foundNode, "Should find root node")
        assertEquals("node1", foundNode.id)
        assertEquals("Processor", foundNode.name)
    }

    @Test
    fun `findNodeById should find nested child node`() {
        // Given: A GraphNode with nested child
        val child = createTestCodeNode("child1", "NestedChild")
        val graphNode = createTestGraphNode("graphNode1", "Container", listOf(child))
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)
        val graphState = GraphState(graph)

        // When: Finding child by ID
        val foundNode = graphState.findNodeById("child1")

        // Then: Should find the nested child
        assertNotNull(foundNode, "Should find nested child node")
        assertEquals("child1", foundNode.id)
        assertEquals("NestedChild", foundNode.name)
    }

    @Test
    fun `findNodeById should find deeply nested node`() {
        // Given: A deeply nested structure (3 levels)
        val deepChild = createTestCodeNode("deepChild", "DeepChild")
        val midGraphNode = createTestGraphNode("midGraph", "MidLevel", listOf(deepChild))
        val topGraphNode = createTestGraphNode("topGraph", "TopLevel", listOf(midGraphNode))
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(topGraphNode)
        val graphState = GraphState(graph)

        // When: Finding the deeply nested node
        val foundNode = graphState.findNodeById("deepChild")

        // Then: Should find it
        assertNotNull(foundNode, "Should find deeply nested node")
        assertEquals("deepChild", foundNode.id)
    }

    @Test
    fun `findNodeById should return null for non-existent node`() {
        // Given: A graph without the target node
        val node = createTestCodeNode("node1", "Processor")
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(node)
        val graphState = GraphState(graph)

        // When: Finding non-existent node
        val foundNode = graphState.findNodeById("nonexistent")

        // Then: Should return null
        assertNull(foundNode, "Should return null for non-existent node")
    }

    // ============================================
    // Integration Tests
    // ============================================

    @Test
    fun `execution control should work with nested GraphNodes`() {
        // Given: Nested GraphNode structure
        val innerChild = createTestCodeNode("innerChild", "InnerProcessor")
        val innerGraphNode = createTestGraphNode("innerGraph", "InnerContainer", listOf(innerChild))
        val outerChild = createTestCodeNode("outerChild", "OuterProcessor")
        val outerGraphNode = createTestGraphNode("outerGraph", "OuterContainer",
            listOf(innerGraphNode, outerChild))
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(outerGraphNode)
        val graphState = GraphState(graph)

        // When: Setting state on outer GraphNode
        graphState.setNodeExecutionState("outerGraph", ExecutionState.RUNNING)

        // Then: All descendants should be RUNNING
        val outerNode = graphState.findNodeById("outerGraph") as GraphNode
        assertEquals(ExecutionState.RUNNING, outerNode.executionState)

        val innerNode = graphState.findNodeById("innerGraph") as GraphNode
        assertEquals(ExecutionState.RUNNING, innerNode.executionState)

        val innerChildNode = graphState.findNodeById("innerChild")
        assertEquals(ExecutionState.RUNNING, innerChildNode?.executionState)

        val outerChildNode = graphState.findNodeById("outerChild")
        assertEquals(ExecutionState.RUNNING, outerChildNode?.executionState)
    }

    @Test
    fun `independent control should block state propagation`() {
        // Given: A GraphNode with an independent child
        val independentChild = createTestCodeNode(
            "independent", "IndependentProcessor",
            controlConfig = ControlConfig(independentControl = true)
        )
        val normalChild = createTestCodeNode("normal", "NormalProcessor")
        val graphNode = createTestGraphNode("container", "Container",
            listOf(independentChild, normalChild))
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)
        val graphState = GraphState(graph)

        // When: Setting state on container
        graphState.setNodeExecutionState("container", ExecutionState.RUNNING)

        // Then: Independent child should remain IDLE
        val independentNode = graphState.findNodeById("independent")
        assertEquals(ExecutionState.IDLE, independentNode?.executionState,
            "Independent node should remain IDLE")

        val normalNode = graphState.findNodeById("normal")
        assertEquals(ExecutionState.RUNNING, normalNode?.executionState,
            "Normal node should be RUNNING")
    }
}
