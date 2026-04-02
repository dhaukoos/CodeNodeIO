/*
 * RuntimeExecutionCharacterizationTest - Characterization tests for execution state management
 * Pins current behavior of execution state transitions, control config propagation,
 * and the seam between graphEditor and fbpDsl runtime.
 * License: Apache 2.0
 */

package io.codenode.grapheditor.characterization

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.ControlConfig
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port
import io.codenode.grapheditor.state.GraphState
import kotlin.test.*

/**
 * Characterization tests that pin the current behavior of execution state management
 * in GraphState. These tests cover the seam between the graphEditor state layer and
 * the fbpDsl runtime model (ExecutionState, ControlConfig).
 */
class RuntimeExecutionCharacterizationTest {

    // ============================================
    // Test Fixtures
    // ============================================

    private fun createCodeNode(
        id: String,
        name: String,
        type: CodeNodeType = CodeNodeType.TRANSFORMER,
        executionState: ExecutionState = ExecutionState.IDLE,
        controlConfig: ControlConfig = ControlConfig()
    ): CodeNode {
        return CodeNode(
            id = id,
            name = name,
            codeNodeType = type,
            position = Node.Position(0.0, 0.0),
            inputPorts = listOf(
                Port(id = "${id}_in", name = "input", direction = Port.Direction.INPUT,
                    dataType = String::class, owningNodeId = id)
            ),
            outputPorts = listOf(
                Port(id = "${id}_out", name = "output", direction = Port.Direction.OUTPUT,
                    dataType = String::class, owningNodeId = id)
            ),
            executionState = executionState,
            controlConfig = controlConfig
        )
    }

    // ============================================
    // Execution State Transitions
    // ============================================

    @Test
    fun `new nodes start in IDLE state`() {
        val node = createCodeNode("n1", "Test")
        assertEquals(ExecutionState.IDLE, node.executionState)
    }

    @Test
    fun `setNodeExecutionState transitions IDLE to RUNNING`() {
        val node = createCodeNode("n1", "Test")
        val graph = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(node)
        val graphState = GraphState(graph)

        val success = graphState.setNodeExecutionState("n1", ExecutionState.RUNNING)

        assertTrue(success)
        val updated = graphState.flowGraph.findNode("n1") as CodeNode
        assertEquals(ExecutionState.RUNNING, updated.executionState)
    }

    @Test
    fun `setNodeExecutionState transitions RUNNING to PAUSED`() {
        val node = createCodeNode("n1", "Test", executionState = ExecutionState.RUNNING)
        val graph = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(node)
        val graphState = GraphState(graph)

        val success = graphState.setNodeExecutionState("n1", ExecutionState.PAUSED)

        assertTrue(success)
        val updated = graphState.flowGraph.findNode("n1") as CodeNode
        assertEquals(ExecutionState.PAUSED, updated.executionState)
    }

    @Test
    fun `setNodeExecutionState returns false for non-existent node`() {
        val graph = flowGraph(name = "Test", version = "1.0.0") {}
        val graphState = GraphState(graph)

        val success = graphState.setNodeExecutionState("nonexistent", ExecutionState.RUNNING)

        assertFalse(success)
    }

    // ============================================
    // State Propagation in GraphNodes
    // ============================================

    @Test
    fun `setNodeExecutionState on GraphNode propagates to children`() {
        val child1 = createCodeNode("c1", "Child1")
        val child2 = createCodeNode("c2", "Child2")
        val graphNode = GraphNode(
            id = "gn1",
            name = "Group",
            position = Node.Position(0.0, 0.0),
            childNodes = listOf(child1, child2),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(graphNode)
        val graphState = GraphState(graph)

        graphState.setNodeExecutionState("gn1", ExecutionState.RUNNING)

        val updatedGN = graphState.flowGraph.findNode("gn1") as GraphNode
        assertEquals(ExecutionState.RUNNING, updatedGN.executionState)
        // Children should also be RUNNING due to propagation
        updatedGN.childNodes.forEach { child ->
            assertEquals(ExecutionState.RUNNING, child.executionState,
                "Child ${child.name} should propagate to RUNNING")
        }
    }

    // ============================================
    // Control Config
    // ============================================

    @Test
    fun `setNodeControlConfig updates the config`() {
        val node = createCodeNode("n1", "Test")
        val graph = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(node)
        val graphState = GraphState(graph)

        val newConfig = ControlConfig(independentControl = true)
        val success = graphState.setNodeControlConfig("n1", newConfig)

        assertTrue(success)
        val updated = graphState.flowGraph.findNode("n1") as CodeNode
        assertTrue(updated.controlConfig.independentControl)
    }

    @Test
    fun `setNodeControlConfig returns false for non-existent node`() {
        val graph = flowGraph(name = "Test", version = "1.0.0") {}
        val graphState = GraphState(graph)

        val success = graphState.setNodeControlConfig("nonexistent", ControlConfig())

        assertFalse(success)
    }

    // ============================================
    // ExecutionState on FlowGraph model
    // ============================================

    @Test
    fun `ExecutionState enum has expected values`() {
        // Pin the enum values that the system depends on
        val values = ExecutionState.entries
        assertTrue(values.contains(ExecutionState.IDLE))
        assertTrue(values.contains(ExecutionState.RUNNING))
        assertTrue(values.contains(ExecutionState.ERROR))
        assertTrue(values.contains(ExecutionState.PAUSED))
    }

    @Test
    fun `multiple nodes can have different execution states`() {
        val n1 = createCodeNode("n1", "Running", executionState = ExecutionState.RUNNING)
        val n2 = createCodeNode("n2", "Idle")
        val graph = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(n1).addNode(n2)
        val graphState = GraphState(graph)

        val node1 = graphState.flowGraph.findNode("n1") as CodeNode
        val node2 = graphState.flowGraph.findNode("n2") as CodeNode
        assertEquals(ExecutionState.RUNNING, node1.executionState)
        assertEquals(ExecutionState.IDLE, node2.executionState)
    }
}
