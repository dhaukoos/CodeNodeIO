/*
 * StatePropagationTest - Tests for hierarchical state propagation
 * Verifies that execution state changes propagate correctly through node hierarchy
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for hierarchical execution state propagation.
 *
 * User Story 1: Hierarchical Execution Control (Priority: P1 - MVP)
 * Goal: Enable GraphNode to propagate execution state changes to all descendants
 *
 * These tests verify:
 * 1. Basic state propagation from parent to children
 * 2. Nested propagation through multiple GraphNode levels
 * 3. ERROR state propagation
 * 4. propagate=false mode for isolated changes
 */
class StatePropagationTest {

    // ========== Helper Functions ==========

    /**
     * Creates a test CodeNode with the given ID and parent
     */
    private fun createCodeNode(
        id: String,
        parentId: String? = null,
        executionState: ExecutionState = ExecutionState.IDLE,
        controlConfig: ControlConfig = ControlConfig()
    ): CodeNode {
        return CodeNode(
            id = id,
            name = "CodeNode $id",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(0.0, 0.0),
            inputPorts = listOf(PortFactory.input<String>("input", id)),
            outputPorts = listOf(PortFactory.output<String>("output", id)),
            parentNodeId = parentId,
            executionState = executionState,
            controlConfig = controlConfig
        )
    }

    /**
     * Creates a test GraphNode with the given children
     */
    private fun createGraphNode(
        id: String,
        children: List<Node>,
        parentId: String? = null,
        executionState: ExecutionState = ExecutionState.IDLE,
        controlConfig: ControlConfig = ControlConfig()
    ): GraphNode {
        // Update children to have correct parent ID
        val childrenWithParent = children.map { child ->
            child.withParent(id)
        }

        return GraphNode(
            id = id,
            name = "GraphNode $id",
            position = Node.Position(0.0, 0.0),
            inputPorts = listOf(PortFactory.input<String>("input", id)),
            outputPorts = listOf(PortFactory.output<String>("output", id)),
            parentNodeId = parentId,
            childNodes = childrenWithParent,
            portMappings = mapOf(
                "input" to GraphNode.PortMapping(childrenWithParent.first().id, "input"),
                "output" to GraphNode.PortMapping(childrenWithParent.last().id, "output")
            ),
            executionState = executionState,
            controlConfig = controlConfig
        )
    }

    // ========== T016: Basic State Propagation Tests ==========

    @Test
    fun `T016 - parent IDLE to RUNNING propagates to all children`() {
        // Given: A GraphNode with multiple CodeNode children, all in IDLE state
        val child1 = createCodeNode("child-1")
        val child2 = createCodeNode("child-2")
        val child3 = createCodeNode("child-3")
        val parent = createGraphNode("parent", listOf(child1, child2, child3))

        // Verify initial state
        assertEquals(ExecutionState.IDLE, parent.executionState)
        assertTrue(parent.childNodes.all { it.executionState == ExecutionState.IDLE })

        // When: Parent's execution state is changed to RUNNING with propagation
        val runningParent = parent.withExecutionState(ExecutionState.RUNNING, propagate = true)

        // Then: Parent and all children should be RUNNING
        assertEquals(ExecutionState.RUNNING, runningParent.executionState)
        assertTrue(runningParent.childNodes.all { it.executionState == ExecutionState.RUNNING },
            "All children should be RUNNING after parent state change")
    }

    @Test
    fun `T016 - parent RUNNING to PAUSED propagates to all children`() {
        // Given: A GraphNode with children, all in RUNNING state
        val child1 = createCodeNode("child-1", executionState = ExecutionState.RUNNING)
        val child2 = createCodeNode("child-2", executionState = ExecutionState.RUNNING)
        val parent = createGraphNode("parent", listOf(child1, child2), executionState = ExecutionState.RUNNING)

        // When: Parent is paused
        val pausedParent = parent.withExecutionState(ExecutionState.PAUSED, propagate = true)

        // Then: All children should be paused
        assertEquals(ExecutionState.PAUSED, pausedParent.executionState)
        assertTrue(pausedParent.childNodes.all { it.executionState == ExecutionState.PAUSED },
            "All children should be PAUSED after parent state change")
    }

    @Test
    fun `T016 - parent state change to IDLE propagates to all children`() {
        // Given: A GraphNode with children in various states
        val child1 = createCodeNode("child-1", executionState = ExecutionState.RUNNING)
        val child2 = createCodeNode("child-2", executionState = ExecutionState.PAUSED)
        val parent = createGraphNode("parent", listOf(child1, child2), executionState = ExecutionState.RUNNING)

        // When: Parent is stopped (IDLE)
        val stoppedParent = parent.withExecutionState(ExecutionState.IDLE, propagate = true)

        // Then: All children should be IDLE
        assertEquals(ExecutionState.IDLE, stoppedParent.executionState)
        assertTrue(stoppedParent.childNodes.all { it.executionState == ExecutionState.IDLE },
            "All children should be IDLE after parent state change")
    }

    // ========== T017: Nested Propagation Tests ==========

    @Test
    fun `T017 - state propagates through nested GraphNodes to all descendants`() {
        // Given: A nested hierarchy: parent -> innerGroup -> grandchild
        val grandchild1 = createCodeNode("grandchild-1")
        val grandchild2 = createCodeNode("grandchild-2")
        val innerGroup = createGraphNode("inner-group", listOf(grandchild1, grandchild2))
        val siblingNode = createCodeNode("sibling")
        val parent = createGraphNode("parent", listOf(innerGroup, siblingNode))

        // Verify initial state
        assertEquals(ExecutionState.IDLE, parent.executionState)

        // When: Parent is set to RUNNING
        val runningParent = parent.withExecutionState(ExecutionState.RUNNING, propagate = true)

        // Then: All descendants should be RUNNING
        assertEquals(ExecutionState.RUNNING, runningParent.executionState)

        // Check direct children
        val updatedInnerGroup = runningParent.childNodes.find { it.id == "inner-group" } as GraphNode
        val updatedSibling = runningParent.childNodes.find { it.id == "sibling" }

        assertEquals(ExecutionState.RUNNING, updatedInnerGroup.executionState,
            "Inner GraphNode should be RUNNING")
        assertEquals(ExecutionState.RUNNING, updatedSibling?.executionState,
            "Sibling CodeNode should be RUNNING")

        // Check grandchildren
        assertTrue(updatedInnerGroup.childNodes.all { it.executionState == ExecutionState.RUNNING },
            "All grandchildren should be RUNNING")
    }

    @Test
    fun `T017 - state propagates through three levels of nesting`() {
        // Given: Three levels deep: root -> level1 -> level2 -> leafNodes
        val leaf1 = createCodeNode("leaf-1")
        val leaf2 = createCodeNode("leaf-2")
        val level2 = createGraphNode("level-2", listOf(leaf1, leaf2))
        val level1Child = createCodeNode("level1-child")
        val level1 = createGraphNode("level-1", listOf(level2, level1Child))
        val rootChild = createCodeNode("root-child")
        val root = createGraphNode("root", listOf(level1, rootChild))

        // When: Root is set to PAUSED
        val pausedRoot = root.withExecutionState(ExecutionState.PAUSED, propagate = true)

        // Then: All nodes at all levels should be PAUSED
        assertEquals(ExecutionState.PAUSED, pausedRoot.executionState)

        // Verify level 1
        val updatedLevel1 = pausedRoot.childNodes.find { it.id == "level-1" } as GraphNode
        assertEquals(ExecutionState.PAUSED, updatedLevel1.executionState)

        // Verify level 2
        val updatedLevel2 = updatedLevel1.childNodes.find { it.id == "level-2" } as GraphNode
        assertEquals(ExecutionState.PAUSED, updatedLevel2.executionState)

        // Verify leaf nodes
        assertTrue(updatedLevel2.childNodes.all { it.executionState == ExecutionState.PAUSED },
            "All leaf nodes should be PAUSED")
    }

    @Test
    fun `T017 - mixed hierarchy propagates correctly`() {
        // Given: GraphNode with mix of CodeNodes and nested GraphNodes
        val innerChild1 = createCodeNode("inner-1")
        val innerChild2 = createCodeNode("inner-2")
        val innerGroup = createGraphNode("inner-group", listOf(innerChild1, innerChild2))
        val directChild1 = createCodeNode("direct-1")
        val directChild2 = createCodeNode("direct-2")
        val parent = createGraphNode("parent", listOf(directChild1, innerGroup, directChild2))

        // When: Parent is set to RUNNING
        val runningParent = parent.withExecutionState(ExecutionState.RUNNING, propagate = true)

        // Then: All nodes should be RUNNING
        assertEquals(ExecutionState.RUNNING, runningParent.executionState)
        assertTrue(runningParent.childNodes.all { it.executionState == ExecutionState.RUNNING },
            "All direct children should be RUNNING")

        val updatedInnerGroup = runningParent.childNodes.find { it.id == "inner-group" } as GraphNode
        assertTrue(updatedInnerGroup.childNodes.all { it.executionState == ExecutionState.RUNNING },
            "All inner group children should be RUNNING")
    }

    // ========== T018: ERROR State Propagation Tests ==========

    @Test
    fun `T018 - parent ERROR transitions all children to ERROR`() {
        // Given: A GraphNode with children in RUNNING state
        val child1 = createCodeNode("child-1", executionState = ExecutionState.RUNNING)
        val child2 = createCodeNode("child-2", executionState = ExecutionState.RUNNING)
        val parent = createGraphNode("parent", listOf(child1, child2), executionState = ExecutionState.RUNNING)

        // When: Parent encounters an error
        val errorParent = parent.withExecutionState(ExecutionState.ERROR, propagate = true)

        // Then: All children should be in ERROR state
        assertEquals(ExecutionState.ERROR, errorParent.executionState)
        assertTrue(errorParent.childNodes.all { it.executionState == ExecutionState.ERROR },
            "All children should be ERROR after parent error")
    }

    @Test
    fun `T018 - ERROR propagates through nested hierarchy`() {
        // Given: Nested hierarchy in RUNNING state
        val grandchild = createCodeNode("grandchild", executionState = ExecutionState.RUNNING)
        val innerGroup = createGraphNode("inner", listOf(grandchild), executionState = ExecutionState.RUNNING)
        val parent = createGraphNode("parent", listOf(innerGroup), executionState = ExecutionState.RUNNING)

        // When: Parent encounters an error
        val errorParent = parent.withExecutionState(ExecutionState.ERROR, propagate = true)

        // Then: All descendants should be in ERROR state
        assertEquals(ExecutionState.ERROR, errorParent.executionState)

        val updatedInner = errorParent.childNodes.first() as GraphNode
        assertEquals(ExecutionState.ERROR, updatedInner.executionState)
        assertEquals(ExecutionState.ERROR, updatedInner.childNodes.first().executionState)
    }

    @Test
    fun `T018 - recovery from ERROR by setting parent to RUNNING`() {
        // Given: A GraphNode hierarchy in ERROR state
        val child1 = createCodeNode("child-1", executionState = ExecutionState.ERROR)
        val child2 = createCodeNode("child-2", executionState = ExecutionState.ERROR)
        val parent = createGraphNode("parent", listOf(child1, child2), executionState = ExecutionState.ERROR)

        // When: Parent is recovered by setting to RUNNING
        val recoveredParent = parent.withExecutionState(ExecutionState.RUNNING, propagate = true)

        // Then: All children should be recovered to RUNNING
        assertEquals(ExecutionState.RUNNING, recoveredParent.executionState)
        assertTrue(recoveredParent.childNodes.all { it.executionState == ExecutionState.RUNNING },
            "All children should be recovered to RUNNING")
    }

    // ========== T019: propagate=false Mode Tests ==========

    @Test
    fun `T019 - propagate=false only changes target node`() {
        // Given: A GraphNode with children in IDLE state
        val child1 = createCodeNode("child-1")
        val child2 = createCodeNode("child-2")
        val parent = createGraphNode("parent", listOf(child1, child2))

        // When: Parent is changed to RUNNING without propagation
        val runningParent = parent.withExecutionState(ExecutionState.RUNNING, propagate = false)

        // Then: Only parent should be RUNNING, children remain IDLE
        assertEquals(ExecutionState.RUNNING, runningParent.executionState)
        assertTrue(runningParent.childNodes.all { it.executionState == ExecutionState.IDLE },
            "Children should remain IDLE when propagate=false")
    }

    @Test
    fun `T019 - propagate=false works with nested hierarchy`() {
        // Given: Nested hierarchy all in IDLE
        val grandchild = createCodeNode("grandchild")
        val innerGroup = createGraphNode("inner", listOf(grandchild))
        val parent = createGraphNode("parent", listOf(innerGroup))

        // When: Parent is changed to RUNNING without propagation
        val runningParent = parent.withExecutionState(ExecutionState.RUNNING, propagate = false)

        // Then: Only parent should be RUNNING
        assertEquals(ExecutionState.RUNNING, runningParent.executionState)

        val unchangedInner = runningParent.childNodes.first() as GraphNode
        assertEquals(ExecutionState.IDLE, unchangedInner.executionState,
            "Inner group should remain IDLE")
        assertEquals(ExecutionState.IDLE, unchangedInner.childNodes.first().executionState,
            "Grandchild should remain IDLE")
    }

    @Test
    fun `T019 - default propagate parameter is true`() {
        // Given: A GraphNode with children
        val child = createCodeNode("child")
        val parent = createGraphNode("parent", listOf(child))

        // When: Using withExecutionState without specifying propagate (should default to true)
        val runningParent = parent.withExecutionState(ExecutionState.RUNNING)

        // Then: Both parent and child should be RUNNING (default propagation)
        assertEquals(ExecutionState.RUNNING, runningParent.executionState)
        assertEquals(ExecutionState.RUNNING, runningParent.childNodes.first().executionState,
            "Child should be RUNNING when using default propagate=true")
    }

    @Test
    fun `T019 - propagate=false preserves existing child states`() {
        // Given: GraphNode with children in mixed states
        val child1 = createCodeNode("child-1", executionState = ExecutionState.RUNNING)
        val child2 = createCodeNode("child-2", executionState = ExecutionState.PAUSED)
        val child3 = createCodeNode("child-3", executionState = ExecutionState.ERROR)
        val parent = createGraphNode("parent", listOf(child1, child2, child3), executionState = ExecutionState.RUNNING)

        // When: Parent is changed to IDLE without propagation
        val stoppedParent = parent.withExecutionState(ExecutionState.IDLE, propagate = false)

        // Then: Children should retain their original states
        assertEquals(ExecutionState.IDLE, stoppedParent.executionState)

        val states = stoppedParent.childNodes.map { it.executionState }
        assertEquals(ExecutionState.RUNNING, states[0], "Child 1 should remain RUNNING")
        assertEquals(ExecutionState.PAUSED, states[1], "Child 2 should remain PAUSED")
        assertEquals(ExecutionState.ERROR, states[2], "Child 3 should remain ERROR")
    }

    // ========== Immutability Tests ==========

    @Test
    fun `propagation creates new node instances without modifying originals`() {
        // Given: Original hierarchy
        val child = createCodeNode("child")
        val parent = createGraphNode("parent", listOf(child))
        val originalChildState = parent.childNodes.first().executionState

        // When: Parent state is changed with propagation
        val runningParent = parent.withExecutionState(ExecutionState.RUNNING, propagate = true)

        // Then: Original parent and child should be unchanged
        assertEquals(ExecutionState.IDLE, parent.executionState, "Original parent should be unchanged")
        assertEquals(originalChildState, parent.childNodes.first().executionState,
            "Original child should be unchanged")

        // And: New parent and child should have new state
        assertEquals(ExecutionState.RUNNING, runningParent.executionState)
        assertEquals(ExecutionState.RUNNING, runningParent.childNodes.first().executionState)
    }
}
