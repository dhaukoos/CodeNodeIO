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

    // ==========================================================================
    // User Story 2: Selective Subgraph Control Override (independentControl)
    // ==========================================================================

    // ========== T024: independentControl Flag Tests ==========

    @Test
    fun `T024 - node with independentControl=true is skipped during propagation`() {
        // Given: A GraphNode with one independent child and one normal child
        val independentChild = createCodeNode(
            "independent",
            controlConfig = ControlConfig(independentControl = true)
        )
        val normalChild = createCodeNode("normal")
        val parent = createGraphNode("parent", listOf(independentChild, normalChild))

        // When: Parent state is changed to RUNNING
        val runningParent = parent.withExecutionState(ExecutionState.RUNNING, propagate = true)

        // Then: Only the normal child should be updated
        assertEquals(ExecutionState.RUNNING, runningParent.executionState)

        val updatedIndependent = runningParent.childNodes.find { it.id == "independent" }
        val updatedNormal = runningParent.childNodes.find { it.id == "normal" }

        assertEquals(ExecutionState.IDLE, updatedIndependent?.executionState,
            "Independent node should remain IDLE")
        assertEquals(ExecutionState.RUNNING, updatedNormal?.executionState,
            "Normal node should be RUNNING")
    }

    @Test
    fun `T024 - independent GraphNode is skipped during parent propagation`() {
        // Given: Parent with an independent GraphNode child
        val grandchild = createCodeNode("grandchild")
        val independentGroup = createGraphNode(
            "independent-group",
            listOf(grandchild),
            controlConfig = ControlConfig(independentControl = true)
        )
        val parent = createGraphNode("parent", listOf(independentGroup))

        // When: Parent state is changed to RUNNING
        val runningParent = parent.withExecutionState(ExecutionState.RUNNING, propagate = true)

        // Then: Independent group and its children should remain IDLE
        assertEquals(ExecutionState.RUNNING, runningParent.executionState)

        val updatedGroup = runningParent.childNodes.first() as GraphNode
        assertEquals(ExecutionState.IDLE, updatedGroup.executionState,
            "Independent group should remain IDLE")
        assertEquals(ExecutionState.IDLE, updatedGroup.childNodes.first().executionState,
            "Grandchild of independent group should remain IDLE")
    }

    @Test
    fun `T024 - multiple independent nodes are all skipped`() {
        // Given: Parent with multiple independent children
        val independent1 = createCodeNode("ind-1", controlConfig = ControlConfig(independentControl = true))
        val independent2 = createCodeNode("ind-2", controlConfig = ControlConfig(independentControl = true))
        val normal = createCodeNode("normal")
        val parent = createGraphNode("parent", listOf(independent1, independent2, normal))

        // When: Parent state is changed
        val runningParent = parent.withExecutionState(ExecutionState.RUNNING, propagate = true)

        // Then: All independent nodes should be skipped
        val ind1 = runningParent.childNodes.find { it.id == "ind-1" }
        val ind2 = runningParent.childNodes.find { it.id == "ind-2" }
        val norm = runningParent.childNodes.find { it.id == "normal" }

        assertEquals(ExecutionState.IDLE, ind1?.executionState)
        assertEquals(ExecutionState.IDLE, ind2?.executionState)
        assertEquals(ExecutionState.RUNNING, norm?.executionState)
    }

    // ========== T025: Direct Control of Independent Node Tests ==========

    @Test
    fun `T025 - independent node can be controlled directly`() {
        // Given: An independent CodeNode
        val independentNode = createCodeNode(
            "independent",
            controlConfig = ControlConfig(independentControl = true)
        )

        // When: Directly changing its state
        val runningNode = independentNode.withExecutionState(ExecutionState.RUNNING)

        // Then: State should change
        assertEquals(ExecutionState.RUNNING, runningNode.executionState)
    }

    @Test
    fun `T025 - independent GraphNode can be controlled directly`() {
        // Given: An independent GraphNode with children
        val child1 = createCodeNode("child-1")
        val child2 = createCodeNode("child-2")
        val independentGroup = createGraphNode(
            "independent-group",
            listOf(child1, child2),
            controlConfig = ControlConfig(independentControl = true)
        )

        // When: Directly controlling the independent group
        val runningGroup = independentGroup.withExecutionState(ExecutionState.RUNNING, propagate = true)

        // Then: The group and its children should all be RUNNING
        assertEquals(ExecutionState.RUNNING, runningGroup.executionState)
        assertTrue(runningGroup.childNodes.all { it.executionState == ExecutionState.RUNNING },
            "Children of directly controlled independent group should be RUNNING")
    }

    @Test
    fun `T025 - independent node retains state when parent changes multiple times`() {
        // Given: Parent with an independent child in RUNNING state
        val independentChild = createCodeNode(
            "independent",
            executionState = ExecutionState.RUNNING,
            controlConfig = ControlConfig(independentControl = true)
        )
        val parent = createGraphNode("parent", listOf(independentChild))

        // When: Parent goes through multiple state changes
        val pausedParent = parent.withExecutionState(ExecutionState.PAUSED, propagate = true)
        val stoppedParent = pausedParent.withExecutionState(ExecutionState.IDLE, propagate = true)
        val errorParent = stoppedParent.withExecutionState(ExecutionState.ERROR, propagate = true)

        // Then: Independent child should retain RUNNING state through all changes
        val finalIndependent = errorParent.childNodes.first()
        assertEquals(ExecutionState.RUNNING, finalIndependent.executionState,
            "Independent node should retain its state through all parent changes")
    }

    // ========== T026: Independent Node Propagating to Its Own Children ==========

    @Test
    fun `T026 - independent node propagates to its own children when controlled directly`() {
        // Given: Independent GraphNode with children
        val grandchild1 = createCodeNode("gc-1")
        val grandchild2 = createCodeNode("gc-2")
        val independentGroup = createGraphNode(
            "independent",
            listOf(grandchild1, grandchild2),
            controlConfig = ControlConfig(independentControl = true)
        )

        // When: Independent group is controlled directly
        val runningGroup = independentGroup.withExecutionState(ExecutionState.RUNNING, propagate = true)

        // Then: Its children should receive the state
        assertEquals(ExecutionState.RUNNING, runningGroup.executionState)
        assertTrue(runningGroup.childNodes.all { it.executionState == ExecutionState.RUNNING },
            "Children of independent group should be updated when group is controlled directly")
    }

    @Test
    fun `T026 - independent group's nested hierarchy propagates correctly`() {
        // Given: Independent group with nested structure
        val leaf = createCodeNode("leaf")
        val nestedGroup = createGraphNode("nested", listOf(leaf))
        val independentGroup = createGraphNode(
            "independent",
            listOf(nestedGroup),
            controlConfig = ControlConfig(independentControl = true)
        )

        // When: Independent group is controlled directly
        val pausedGroup = independentGroup.withExecutionState(ExecutionState.PAUSED, propagate = true)

        // Then: All descendants should be paused
        assertEquals(ExecutionState.PAUSED, pausedGroup.executionState)
        val updatedNested = pausedGroup.childNodes.first() as GraphNode
        assertEquals(ExecutionState.PAUSED, updatedNested.executionState)
        assertEquals(ExecutionState.PAUSED, updatedNested.childNodes.first().executionState)
    }

    @Test
    fun `T026 - independent node respects independentControl within its own children`() {
        // Given: Independent group with an independent grandchild
        val independentGrandchild = createCodeNode(
            "ind-grandchild",
            controlConfig = ControlConfig(independentControl = true)
        )
        val normalGrandchild = createCodeNode("normal-grandchild")
        val independentGroup = createGraphNode(
            "independent",
            listOf(independentGrandchild, normalGrandchild),
            controlConfig = ControlConfig(independentControl = true)
        )

        // When: Independent group is controlled directly
        val runningGroup = independentGroup.withExecutionState(ExecutionState.RUNNING, propagate = true)

        // Then: The independent grandchild should still be skipped
        val indGc = runningGroup.childNodes.find { it.id == "ind-grandchild" }
        val normGc = runningGroup.childNodes.find { it.id == "normal-grandchild" }

        assertEquals(ExecutionState.IDLE, indGc?.executionState,
            "Independent grandchild should remain IDLE even when parent group is controlled directly")
        assertEquals(ExecutionState.RUNNING, normGc?.executionState,
            "Normal grandchild should be RUNNING")
    }

    // ========== T027: Nested Independent Boundaries Tests ==========

    @Test
    fun `T027 - nothing below independent boundary affected by parent changes`() {
        // Given: Hierarchy with independent boundary in the middle
        //   root -> independent-group -> grandchild
        val grandchild = createCodeNode("grandchild")
        val independentGroup = createGraphNode(
            "independent",
            listOf(grandchild),
            controlConfig = ControlConfig(independentControl = true)
        )
        val root = createGraphNode("root", listOf(independentGroup))

        // When: Root is set to RUNNING
        val runningRoot = root.withExecutionState(ExecutionState.RUNNING, propagate = true)

        // Then: Everything below the independent boundary should be unchanged
        assertEquals(ExecutionState.RUNNING, runningRoot.executionState)

        val updatedIndGroup = runningRoot.childNodes.first() as GraphNode
        assertEquals(ExecutionState.IDLE, updatedIndGroup.executionState,
            "Independent group should remain IDLE")
        assertEquals(ExecutionState.IDLE, updatedIndGroup.childNodes.first().executionState,
            "Grandchild below independent boundary should remain IDLE")
    }

    @Test
    fun `T027 - deeply nested independent boundary blocks propagation`() {
        // Given: Deep hierarchy with independent boundary at level 2
        //   root -> level1 -> independent-level2 -> level3 -> leaf
        val leaf = createCodeNode("leaf")
        val level3 = createGraphNode("level3", listOf(leaf))
        val independentLevel2 = createGraphNode(
            "level2",
            listOf(level3),
            controlConfig = ControlConfig(independentControl = true)
        )
        val level1 = createGraphNode("level1", listOf(independentLevel2))
        val root = createGraphNode("root", listOf(level1))

        // When: Root is set to RUNNING
        val runningRoot = root.withExecutionState(ExecutionState.RUNNING, propagate = true)

        // Then: Level1 should be RUNNING, but level2 and below should be IDLE
        val updatedL1 = runningRoot.childNodes.first() as GraphNode
        assertEquals(ExecutionState.RUNNING, updatedL1.executionState, "Level1 should be RUNNING")

        val updatedL2 = updatedL1.childNodes.first() as GraphNode
        assertEquals(ExecutionState.IDLE, updatedL2.executionState,
            "Independent Level2 should remain IDLE")

        val updatedL3 = updatedL2.childNodes.first() as GraphNode
        assertEquals(ExecutionState.IDLE, updatedL3.executionState,
            "Level3 below boundary should remain IDLE")

        assertEquals(ExecutionState.IDLE, updatedL3.childNodes.first().executionState,
            "Leaf below boundary should remain IDLE")
    }

    @Test
    fun `T027 - sibling nodes are affected even when one sibling is independent`() {
        // Given: Parent with independent and non-independent siblings
        val independentSibling = createCodeNode(
            "independent",
            controlConfig = ControlConfig(independentControl = true)
        )
        val normalSibling1 = createCodeNode("normal-1")
        val normalSibling2 = createCodeNode("normal-2")
        val parent = createGraphNode("parent",
            listOf(independentSibling, normalSibling1, normalSibling2))

        // When: Parent is set to RUNNING
        val runningParent = parent.withExecutionState(ExecutionState.RUNNING, propagate = true)

        // Then: Normal siblings should be affected, independent should not
        val ind = runningParent.childNodes.find { it.id == "independent" }
        val norm1 = runningParent.childNodes.find { it.id == "normal-1" }
        val norm2 = runningParent.childNodes.find { it.id == "normal-2" }

        assertEquals(ExecutionState.IDLE, ind?.executionState)
        assertEquals(ExecutionState.RUNNING, norm1?.executionState)
        assertEquals(ExecutionState.RUNNING, norm2?.executionState)
    }

    @Test
    fun `T027 - multiple independent boundaries at different levels`() {
        // Given: Hierarchy with independent nodes at multiple levels
        val deepIndependent = createCodeNode(
            "deep-ind",
            controlConfig = ControlConfig(independentControl = true)
        )
        val normalLeaf = createCodeNode("normal-leaf")
        val level2 = createGraphNode("level2", listOf(deepIndependent, normalLeaf))
        val independentLevel1 = createGraphNode(
            "ind-level1",
            listOf(level2),
            controlConfig = ControlConfig(independentControl = true)
        )
        val normalLevel1 = createCodeNode("normal-level1")
        val root = createGraphNode("root", listOf(independentLevel1, normalLevel1))

        // When: Root is set to RUNNING
        val runningRoot = root.withExecutionState(ExecutionState.RUNNING, propagate = true)

        // Then: Only normalLevel1 at root level should be RUNNING
        val indL1 = runningRoot.childNodes.find { it.id == "ind-level1" } as GraphNode
        val normL1 = runningRoot.childNodes.find { it.id == "normal-level1" }

        assertEquals(ExecutionState.IDLE, indL1.executionState,
            "Independent level1 should remain IDLE")
        assertEquals(ExecutionState.RUNNING, normL1?.executionState,
            "Normal level1 should be RUNNING")

        // And: Everything under the independent boundary should be IDLE
        val l2 = indL1.childNodes.first() as GraphNode
        assertEquals(ExecutionState.IDLE, l2.executionState)
        assertTrue(l2.childNodes.all { it.executionState == ExecutionState.IDLE },
            "All nodes under independent boundary should remain IDLE")
    }
}
