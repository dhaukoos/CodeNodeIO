/*
 * NodeExecutionStateTest - Tests for Node execution state properties
 * Verifies that all Node subclasses have executionState and controlConfig
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for Node execution state and control configuration properties.
 *
 * These tests verify that:
 * 1. All Node subclasses (CodeNode, GraphNode) have executionState property
 * 2. All Node subclasses have controlConfig property
 * 3. Default values are correct
 * 4. withExecutionState and withControlConfig methods work correctly
 */
class NodeExecutionStateTest {

    // Helper to create a minimal CodeNode for testing
    private fun createTestCodeNode(
        id: String = "test-code-node",
        executionState: ExecutionState = ExecutionState.IDLE,
        controlConfig: ControlConfig = ControlConfig()
    ): CodeNode {
        return CodeNode(
            id = id,
            name = "Test Code Node",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(0.0, 0.0),
            inputPorts = listOf(
                PortFactory.input<String>("input", id)
            ),
            outputPorts = listOf(
                PortFactory.output<String>("output", id)
            ),
            executionState = executionState,
            controlConfig = controlConfig
        )
    }

    // Helper to create a minimal GraphNode for testing
    private fun createTestGraphNode(
        id: String = "test-graph-node",
        executionState: ExecutionState = ExecutionState.IDLE,
        controlConfig: ControlConfig = ControlConfig(),
        childNodes: List<Node> = emptyList()
    ): GraphNode {
        val children = childNodes.ifEmpty {
            listOf(createTestCodeNode(id = "child-1").withParent(id))
        }
        return GraphNode(
            id = id,
            name = "Test Graph Node",
            position = Node.Position(0.0, 0.0),
            inputPorts = listOf(
                PortFactory.input<String>("input", id)
            ),
            outputPorts = listOf(
                PortFactory.output<String>("output", id)
            ),
            childNodes = children,
            portMappings = mapOf(
                "input" to GraphNode.PortMapping("child-1", "input"),
                "output" to GraphNode.PortMapping("child-1", "output")
            ),
            executionState = executionState,
            controlConfig = controlConfig
        )
    }

    // ========== CodeNode Tests ==========

    @Test
    fun `CodeNode has executionState property with default IDLE`() {
        val codeNode = createTestCodeNode()
        assertEquals(ExecutionState.IDLE, codeNode.executionState)
    }

    @Test
    fun `CodeNode has controlConfig property with defaults`() {
        val codeNode = createTestCodeNode()
        assertNotNull(codeNode.controlConfig)
        assertEquals(100, codeNode.controlConfig.pauseBufferSize)
        assertEquals(0L, codeNode.controlConfig.speedAttenuation)
        assertEquals(false, codeNode.controlConfig.autoResumeOnError)
        assertEquals(false, codeNode.controlConfig.independentControl)
    }

    @Test
    fun `CodeNode withExecutionState creates copy with new state`() {
        val codeNode = createTestCodeNode(executionState = ExecutionState.IDLE)
        val runningNode = codeNode.withExecutionState(ExecutionState.RUNNING)

        assertEquals(ExecutionState.IDLE, codeNode.executionState) // Original unchanged
        assertEquals(ExecutionState.RUNNING, runningNode.executionState)
        assertEquals(codeNode.id, runningNode.id) // Same ID
    }

    @Test
    fun `CodeNode withControlConfig creates copy with new config`() {
        val codeNode = createTestCodeNode()
        val newConfig = ControlConfig(pauseBufferSize = 200, speedAttenuation = 50L)
        val updatedNode = codeNode.withControlConfig(newConfig)

        assertEquals(100, codeNode.controlConfig.pauseBufferSize) // Original unchanged
        assertEquals(200, updatedNode.controlConfig.pauseBufferSize)
        assertEquals(50L, updatedNode.controlConfig.speedAttenuation)
    }

    // ========== GraphNode Tests ==========

    @Test
    fun `GraphNode has executionState property with default IDLE`() {
        val graphNode = createTestGraphNode()
        assertEquals(ExecutionState.IDLE, graphNode.executionState)
    }

    @Test
    fun `GraphNode has controlConfig property with defaults`() {
        val graphNode = createTestGraphNode()
        assertNotNull(graphNode.controlConfig)
        assertEquals(100, graphNode.controlConfig.pauseBufferSize)
        assertEquals(0L, graphNode.controlConfig.speedAttenuation)
        assertEquals(false, graphNode.controlConfig.autoResumeOnError)
        assertEquals(false, graphNode.controlConfig.independentControl)
    }

    @Test
    fun `GraphNode withExecutionState creates copy with new state`() {
        val graphNode = createTestGraphNode(executionState = ExecutionState.IDLE)
        val runningNode = graphNode.withExecutionState(ExecutionState.RUNNING)

        assertEquals(ExecutionState.IDLE, graphNode.executionState) // Original unchanged
        assertEquals(ExecutionState.RUNNING, runningNode.executionState)
        assertEquals(graphNode.id, runningNode.id) // Same ID
    }

    @Test
    fun `GraphNode withControlConfig creates copy with new config`() {
        val graphNode = createTestGraphNode()
        val newConfig = ControlConfig(pauseBufferSize = 200, speedAttenuation = 50L)
        val updatedNode = graphNode.withControlConfig(newConfig)

        assertEquals(100, graphNode.controlConfig.pauseBufferSize) // Original unchanged
        assertEquals(200, updatedNode.controlConfig.pauseBufferSize)
        assertEquals(50L, updatedNode.controlConfig.speedAttenuation)
    }

    // ========== Polymorphic Access Tests ==========

    @Test
    fun `Node reference can access executionState polymorphically`() {
        val codeNode: Node = createTestCodeNode(executionState = ExecutionState.RUNNING)
        val graphNode: Node = createTestGraphNode(executionState = ExecutionState.PAUSED)

        assertEquals(ExecutionState.RUNNING, codeNode.executionState)
        assertEquals(ExecutionState.PAUSED, graphNode.executionState)
    }

    @Test
    fun `Node reference can access controlConfig polymorphically`() {
        val config = ControlConfig(speedAttenuation = 100L)
        val codeNode: Node = createTestCodeNode(controlConfig = config)
        val graphNode: Node = createTestGraphNode(controlConfig = config)

        assertEquals(100L, codeNode.controlConfig.speedAttenuation)
        assertEquals(100L, graphNode.controlConfig.speedAttenuation)
    }

    @Test
    fun `Node withExecutionState returns correct subtype`() {
        val codeNode: Node = createTestCodeNode()
        val graphNode: Node = createTestGraphNode()

        val updatedCodeNode = codeNode.withExecutionState(ExecutionState.RUNNING)
        val updatedGraphNode = graphNode.withExecutionState(ExecutionState.RUNNING)

        assertTrue(updatedCodeNode is CodeNode)
        assertTrue(updatedGraphNode is GraphNode)
        assertEquals(ExecutionState.RUNNING, updatedCodeNode.executionState)
        assertEquals(ExecutionState.RUNNING, updatedGraphNode.executionState)
    }

    @Test
    fun `Node withControlConfig returns correct subtype`() {
        val codeNode: Node = createTestCodeNode()
        val graphNode: Node = createTestGraphNode()

        val newConfig = ControlConfig(independentControl = true)
        val updatedCodeNode = codeNode.withControlConfig(newConfig)
        val updatedGraphNode = graphNode.withControlConfig(newConfig)

        assertTrue(updatedCodeNode is CodeNode)
        assertTrue(updatedGraphNode is GraphNode)
        assertTrue(updatedCodeNode.controlConfig.independentControl)
        assertTrue(updatedGraphNode.controlConfig.independentControl)
    }

    // ========== All ExecutionState Values Tests ==========

    @Test
    fun `CodeNode can have all ExecutionState values`() {
        ExecutionState.entries.forEach { state ->
            val node = createTestCodeNode(executionState = state)
            assertEquals(state, node.executionState)
        }
    }

    @Test
    fun `GraphNode can have all ExecutionState values`() {
        ExecutionState.entries.forEach { state ->
            val node = createTestGraphNode(executionState = state)
            assertEquals(state, node.executionState)
        }
    }

    // ========== ControlConfig with independentControl Tests ==========

    @Test
    fun `ControlConfig independentControl defaults to false`() {
        val config = ControlConfig()
        assertEquals(false, config.independentControl)
    }

    @Test
    fun `ControlConfig can set independentControl to true`() {
        val config = ControlConfig(independentControl = true)
        assertEquals(true, config.independentControl)
    }

    @Test
    fun `CodeNode can have independentControl enabled`() {
        val config = ControlConfig(independentControl = true)
        val node = createTestCodeNode(controlConfig = config)
        assertTrue(node.controlConfig.independentControl)
    }

    @Test
    fun `GraphNode can have independentControl enabled`() {
        val config = ControlConfig(independentControl = true)
        val node = createTestGraphNode(controlConfig = config)
        assertTrue(node.controlConfig.independentControl)
    }
}
