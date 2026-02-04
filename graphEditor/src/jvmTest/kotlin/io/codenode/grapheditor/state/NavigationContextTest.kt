/*
 * NavigationContext Test
 * Unit tests for NavigationContext data class
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port
import kotlin.test.*

/**
 * TDD tests for NavigationContext.
 * These tests verify that NavigationContext correctly:
 * - Manages navigation path as a stack
 * - Provides pushInto/popOut navigation operations
 * - Calculates computed properties for current context
 * - Supports hierarchical navigation up to 5 levels deep
 */
class NavigationContextTest {

    // ============================================
    // Initial State Tests
    // ============================================

    @Test
    fun `empty NavigationContext should be at root`() {
        val context = NavigationContext()
        assertTrue(context.isAtRoot)
    }

    @Test
    fun `empty NavigationContext should have empty path`() {
        val context = NavigationContext()
        assertTrue(context.path.isEmpty())
    }

    @Test
    fun `empty NavigationContext should have null currentGraphNodeId`() {
        val context = NavigationContext()
        assertNull(context.currentGraphNodeId)
    }

    @Test
    fun `empty NavigationContext should have depth 0`() {
        val context = NavigationContext()
        assertEquals(0, context.depth)
    }

    @Test
    fun `empty NavigationContext should have null parentGraphNodeId`() {
        val context = NavigationContext()
        assertNull(context.parentGraphNodeId)
    }

    @Test
    fun `empty NavigationContext should not allow navigation out`() {
        val context = NavigationContext()
        assertFalse(context.canNavigateOut)
    }

    // ============================================
    // pushInto Tests
    // ============================================

    @Test
    fun `pushInto should add graphNodeId to path`() {
        val context = NavigationContext()
        val newContext = context.pushInto("graphNode1")

        assertEquals(listOf("graphNode1"), newContext.path)
    }

    @Test
    fun `pushInto should update currentGraphNodeId`() {
        val context = NavigationContext()
        val newContext = context.pushInto("graphNode1")

        assertEquals("graphNode1", newContext.currentGraphNodeId)
    }

    @Test
    fun `pushInto should no longer be at root`() {
        val context = NavigationContext()
        val newContext = context.pushInto("graphNode1")

        assertFalse(newContext.isAtRoot)
    }

    @Test
    fun `pushInto should allow navigation out`() {
        val context = NavigationContext()
        val newContext = context.pushInto("graphNode1")

        assertTrue(newContext.canNavigateOut)
    }

    @Test
    fun `pushInto should increment depth`() {
        val context = NavigationContext()
        val newContext = context.pushInto("graphNode1")

        assertEquals(1, newContext.depth)
    }

    @Test
    fun `pushInto multiple times should build path`() {
        val context = NavigationContext()
            .pushInto("graphNode1")
            .pushInto("graphNode2")
            .pushInto("graphNode3")

        assertEquals(listOf("graphNode1", "graphNode2", "graphNode3"), context.path)
        assertEquals(3, context.depth)
        assertEquals("graphNode3", context.currentGraphNodeId)
    }

    @Test
    fun `pushInto should support 5 levels of nesting`() {
        val context = NavigationContext()
            .pushInto("level1")
            .pushInto("level2")
            .pushInto("level3")
            .pushInto("level4")
            .pushInto("level5")

        assertEquals(5, context.depth)
        assertEquals("level5", context.currentGraphNodeId)
        assertEquals("level4", context.parentGraphNodeId)
    }

    // ============================================
    // popOut Tests
    // ============================================

    @Test
    fun `popOut should remove last graphNodeId from path`() {
        val context = NavigationContext(path = listOf("graphNode1", "graphNode2"))
        val newContext = context.popOut()

        assertEquals(listOf("graphNode1"), newContext.path)
    }

    @Test
    fun `popOut should update currentGraphNodeId to parent`() {
        val context = NavigationContext(path = listOf("graphNode1", "graphNode2"))
        val newContext = context.popOut()

        assertEquals("graphNode1", newContext.currentGraphNodeId)
    }

    @Test
    fun `popOut to root should set currentGraphNodeId to null`() {
        val context = NavigationContext(path = listOf("graphNode1"))
        val newContext = context.popOut()

        assertNull(newContext.currentGraphNodeId)
        assertTrue(newContext.isAtRoot)
    }

    @Test
    fun `popOut should decrement depth`() {
        val context = NavigationContext(path = listOf("graphNode1", "graphNode2"))
        val newContext = context.popOut()

        assertEquals(1, newContext.depth)
    }

    @Test
    fun `popOut at root should remain at root`() {
        val context = NavigationContext()
        val newContext = context.popOut()

        assertTrue(newContext.isAtRoot)
        assertEquals(0, newContext.depth)
    }

    // ============================================
    // reset Tests
    // ============================================

    @Test
    fun `reset should return to root`() {
        val context = NavigationContext(path = listOf("graphNode1", "graphNode2", "graphNode3"))
        val newContext = context.reset()

        assertTrue(newContext.isAtRoot)
        assertTrue(newContext.path.isEmpty())
        assertEquals(0, newContext.depth)
    }

    @Test
    fun `reset on empty context should remain at root`() {
        val context = NavigationContext()
        val newContext = context.reset()

        assertTrue(newContext.isAtRoot)
    }

    // ============================================
    // parentGraphNodeId Tests
    // ============================================

    @Test
    fun `parentGraphNodeId should be null at depth 1`() {
        val context = NavigationContext(path = listOf("graphNode1"))
        assertNull(context.parentGraphNodeId)
    }

    @Test
    fun `parentGraphNodeId should return second-to-last at depth 2`() {
        val context = NavigationContext(path = listOf("graphNode1", "graphNode2"))
        assertEquals("graphNode1", context.parentGraphNodeId)
    }

    @Test
    fun `parentGraphNodeId should return second-to-last at deeper levels`() {
        val context = NavigationContext(path = listOf("level1", "level2", "level3", "level4"))
        assertEquals("level3", context.parentGraphNodeId)
    }

    // ============================================
    // contains Tests
    // ============================================

    @Test
    fun `contains should return true for graphNodeId in path`() {
        val context = NavigationContext(path = listOf("graphNode1", "graphNode2", "graphNode3"))
        assertTrue(context.contains("graphNode2"))
    }

    @Test
    fun `contains should return false for graphNodeId not in path`() {
        val context = NavigationContext(path = listOf("graphNode1", "graphNode2"))
        assertFalse(context.contains("graphNode3"))
    }

    @Test
    fun `contains should return false for empty path`() {
        val context = NavigationContext()
        assertFalse(context.contains("graphNode1"))
    }

    // ============================================
    // Immutability Tests
    // ============================================

    @Test
    fun `pushInto should not modify original context`() {
        val original = NavigationContext()
        val newContext = original.pushInto("graphNode1")

        assertTrue(original.isAtRoot)
        assertFalse(newContext.isAtRoot)
    }

    @Test
    fun `popOut should not modify original context`() {
        val original = NavigationContext(path = listOf("graphNode1", "graphNode2"))
        val newContext = original.popOut()

        assertEquals(2, original.depth)
        assertEquals(1, newContext.depth)
    }

    // ============================================
    // Edge Cases
    // ============================================

    @Test
    fun `path should preserve order`() {
        val context = NavigationContext()
            .pushInto("first")
            .pushInto("second")
            .pushInto("third")

        assertEquals("first", context.path[0])
        assertEquals("second", context.path[1])
        assertEquals("third", context.path[2])
    }

    // ============================================
    // T056: GraphState.navigateInto() Tests
    // These tests verify that navigateIntoGraphNode() correctly
    // updates NavigationContext via pushInto()
    // ============================================

    @Test
    fun `navigateIntoGraphNode should update NavigationContext path`() {
        // Given: A graph with a GraphNode
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
        assertTrue(graphState.navigationContext.isAtRoot, "Should start at root")

        // When: Navigating into the GraphNode
        val success = graphState.navigateIntoGraphNode("graphNode1")

        // Then: NavigationContext should be updated
        assertTrue(success, "Navigation should succeed")
        assertEquals(listOf("graphNode1"), graphState.navigationContext.path)
        assertEquals("graphNode1", graphState.navigationContext.currentGraphNodeId)
    }

    @Test
    fun `navigateIntoGraphNode should fail for non-existent node`() {
        // Given: An empty graph
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
        val graphState = GraphState(graph)

        // When: Trying to navigate into non-existent node
        val success = graphState.navigateIntoGraphNode("nonExistent")

        // Then: Navigation should fail, context unchanged
        assertFalse(success, "Navigation should fail for non-existent node")
        assertTrue(graphState.navigationContext.isAtRoot, "Should remain at root")
    }

    @Test
    fun `navigateIntoGraphNode should fail for CodeNode`() {
        // Given: A graph with only a CodeNode (not a GraphNode)
        val codeNode = createTestCodeNode("codeNode1", "CodeNode1", 100.0, 100.0)
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(codeNode)

        val graphState = GraphState(graph)

        // When: Trying to navigate into a CodeNode
        val success = graphState.navigateIntoGraphNode("codeNode1")

        // Then: Navigation should fail
        assertFalse(success, "Navigation should fail for CodeNode")
        assertTrue(graphState.navigationContext.isAtRoot, "Should remain at root")
    }

    @Test
    fun `navigateIntoGraphNode should support nested navigation`() {
        // Given: Nested GraphNodes
        val deepChild = createTestCodeNode("deepChild", "DeepChild", 25.0, 25.0)
        val innerGraphNode = GraphNode(
            id = "innerGraphNode",
            name = "InnerGroup",
            position = Node.Position(50.0, 50.0),
            childNodes = listOf(deepChild),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val outerGraphNode = GraphNode(
            id = "outerGraphNode",
            name = "OuterGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(innerGraphNode),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(outerGraphNode)

        val graphState = GraphState(graph)

        // When: Navigating two levels deep
        graphState.navigateIntoGraphNode("outerGraphNode")
        graphState.navigateIntoGraphNode("innerGraphNode")

        // Then: Path should have both nodes
        assertEquals(listOf("outerGraphNode", "innerGraphNode"), graphState.navigationContext.path)
        assertEquals(2, graphState.navigationContext.depth)
        assertEquals("innerGraphNode", graphState.navigationContext.currentGraphNodeId)
        assertEquals("outerGraphNode", graphState.navigationContext.parentGraphNodeId)
    }

    @Test
    fun `navigateIntoGraphNode should clear selection`() {
        // Given: A GraphNode that is selected
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
        graphState.toggleNodeInSelection("graphNode1")
        assertTrue(graphState.selectionState.selectedNodeIds.isNotEmpty())

        // When: Navigating into the GraphNode
        graphState.navigateIntoGraphNode("graphNode1")

        // Then: Selection should be cleared
        assertTrue(graphState.selectionState.selectedNodeIds.isEmpty(), "Selection should be cleared")
    }

    @Test
    fun `navigateIntoGraphNode should update canNavigateOut`() {
        // Given: A graph with a GraphNode
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
        assertFalse(graphState.navigationContext.canNavigateOut, "Should not be able to navigate out at root")

        // When: Navigating into the GraphNode
        graphState.navigateIntoGraphNode("graphNode1")

        // Then: Should now be able to navigate out
        assertTrue(graphState.navigationContext.canNavigateOut, "Should be able to navigate out after navigating in")
    }

    @Test
    fun `navigateIntoGraphNode followed by navigateOut should return to root`() {
        // Given: Navigated into a GraphNode
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
        graphState.navigateIntoGraphNode("graphNode1")
        assertFalse(graphState.navigationContext.isAtRoot)

        // When: Navigating out
        val success = graphState.navigateOut()

        // Then: Should be back at root
        assertTrue(success, "Navigate out should succeed")
        assertTrue(graphState.navigationContext.isAtRoot, "Should be at root")
    }

    @Test
    fun `navigateIntoGraphNode should update isAtRoot to false`() {
        // Given: A graph with a GraphNode at root
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
        assertTrue(graphState.navigationContext.isAtRoot)

        // When: Navigating into the GraphNode
        graphState.navigateIntoGraphNode("graphNode1")

        // Then: isAtRoot should be false
        assertFalse(graphState.navigationContext.isAtRoot)
    }

    @Test
    fun `navigateIntoGraphNode should support 5 levels deep`() {
        // Given: 5 nested GraphNodes
        fun createNestedGraphNodes(level: Int): GraphNode {
            return if (level == 5) {
                val leaf = createTestCodeNode("leaf", "Leaf", 0.0, 0.0)
                GraphNode(
                    id = "level$level",
                    name = "Level$level",
                    position = Node.Position(0.0, 0.0),
                    childNodes = listOf(leaf),
                    internalConnections = emptyList(),
                    inputPorts = emptyList(),
                    outputPorts = emptyList(),
                    portMappings = emptyMap()
                )
            } else {
                GraphNode(
                    id = "level$level",
                    name = "Level$level",
                    position = Node.Position(0.0, 0.0),
                    childNodes = listOf(createNestedGraphNodes(level + 1)),
                    internalConnections = emptyList(),
                    inputPorts = emptyList(),
                    outputPorts = emptyList(),
                    portMappings = emptyMap()
                )
            }
        }

        val rootGraphNode = createNestedGraphNodes(1)
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(rootGraphNode)

        val graphState = GraphState(graph)

        // When: Navigating 5 levels deep
        graphState.navigateIntoGraphNode("level1")
        graphState.navigateIntoGraphNode("level2")
        graphState.navigateIntoGraphNode("level3")
        graphState.navigateIntoGraphNode("level4")
        graphState.navigateIntoGraphNode("level5")

        // Then: Should be at depth 5
        assertEquals(5, graphState.navigationContext.depth)
        assertEquals("level5", graphState.navigationContext.currentGraphNodeId)
        assertEquals(listOf("level1", "level2", "level3", "level4", "level5"), graphState.navigationContext.path)
    }

    // ============================================
    // navigateToDepth Tests (Breadcrumb Navigation)
    // ============================================

    @Test
    fun `navigateToDepth should navigate to root when depth is 0`() {
        // Given: Navigated 3 levels deep
        val graphState = createNestedGraphStateAtDepth(3)
        assertEquals(3, graphState.navigationContext.depth)

        // When: Navigating to depth 0
        val success = graphState.navigateToDepth(0)

        // Then: Should be at root
        assertTrue(success)
        assertTrue(graphState.navigationContext.isAtRoot)
        assertEquals(0, graphState.navigationContext.depth)
    }

    @Test
    fun `navigateToDepth should navigate to intermediate level`() {
        // Given: Navigated 3 levels deep
        val graphState = createNestedGraphStateAtDepth(3)
        assertEquals(3, graphState.navigationContext.depth)

        // When: Navigating to depth 1
        val success = graphState.navigateToDepth(1)

        // Then: Should be at depth 1
        assertTrue(success)
        assertEquals(1, graphState.navigationContext.depth)
        assertEquals("level1", graphState.navigationContext.currentGraphNodeId)
    }

    @Test
    fun `navigateToDepth should fail for current or higher depth`() {
        // Given: At depth 2
        val graphState = createNestedGraphStateAtDepth(2)

        // When: Trying to navigate to same depth or higher
        val successSame = graphState.navigateToDepth(2)
        val successHigher = graphState.navigateToDepth(3)

        // Then: Both should fail
        assertFalse(successSame)
        assertFalse(successHigher)
        assertEquals(2, graphState.navigationContext.depth) // Unchanged
    }

    @Test
    fun `navigateToDepth should fail for negative depth`() {
        // Given: At depth 2
        val graphState = createNestedGraphStateAtDepth(2)

        // When: Trying to navigate to negative depth
        val success = graphState.navigateToDepth(-1)

        // Then: Should fail
        assertFalse(success)
        assertEquals(2, graphState.navigationContext.depth) // Unchanged
    }

    @Test
    fun `navigateToDepth should clear selection`() {
        // Given: At depth 2 with a selection
        val graphState = createNestedGraphStateAtDepth(2)
        graphState.toggleNodeInSelection("someNode")

        // When: Navigating to depth 0
        graphState.navigateToDepth(0)

        // Then: Selection should be cleared
        assertTrue(graphState.selectionState.selectedNodeIds.isEmpty())
    }

    // ============================================
    // T066: GraphState.navigateOut() Tests
    // Tests for zoom-out / navigate back functionality
    // ============================================

    @Test
    fun `navigateOut should return false when at root`() {
        // Given: At root level
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
        val graphState = GraphState(graph)
        assertTrue(graphState.navigationContext.isAtRoot)

        // When: Trying to navigate out
        val success = graphState.navigateOut()

        // Then: Should fail and remain at root
        assertFalse(success, "Should return false when already at root")
        assertTrue(graphState.navigationContext.isAtRoot)
    }

    @Test
    fun `navigateOut should update NavigationContext path`() {
        // Given: Navigated 2 levels deep
        val graphState = createNestedGraphStateAtDepth(2)
        assertEquals(2, graphState.navigationContext.depth)
        assertEquals(listOf("level1", "level2"), graphState.navigationContext.path)

        // When: Navigating out once
        val success = graphState.navigateOut()

        // Then: Should be at depth 1
        assertTrue(success)
        assertEquals(1, graphState.navigationContext.depth)
        assertEquals(listOf("level1"), graphState.navigationContext.path)
        assertEquals("level1", graphState.navigationContext.currentGraphNodeId)
    }

    @Test
    fun `navigateOut should clear selection`() {
        // Given: Inside a GraphNode with a selection
        val graphState = createNestedGraphStateAtDepth(1)
        graphState.toggleNodeInSelection("leaf")
        assertTrue(graphState.selectionState.selectedNodeIds.isNotEmpty())

        // When: Navigating out
        graphState.navigateOut()

        // Then: Selection should be cleared
        assertTrue(graphState.selectionState.selectedNodeIds.isEmpty())
    }

    @Test
    fun `navigateOut from depth 1 should return to root`() {
        // Given: At depth 1
        val graphState = createNestedGraphStateAtDepth(1)
        assertEquals(1, graphState.navigationContext.depth)
        assertFalse(graphState.navigationContext.isAtRoot)

        // When: Navigating out
        val success = graphState.navigateOut()

        // Then: Should be at root
        assertTrue(success)
        assertTrue(graphState.navigationContext.isAtRoot)
        assertEquals(0, graphState.navigationContext.depth)
        assertNull(graphState.navigationContext.currentGraphNodeId)
    }

    @Test
    fun `navigateOut should update canNavigateOut correctly`() {
        // Given: At depth 2
        val graphState = createNestedGraphStateAtDepth(2)
        assertTrue(graphState.navigationContext.canNavigateOut)

        // When: Navigating out to depth 1
        graphState.navigateOut()

        // Then: Should still be able to navigate out
        assertTrue(graphState.navigationContext.canNavigateOut)

        // When: Navigating out to root
        graphState.navigateOut()

        // Then: Should NOT be able to navigate out
        assertFalse(graphState.navigationContext.canNavigateOut)
    }

    @Test
    fun `navigateOut multiple times should reach root`() {
        // Given: At depth 3
        val graphState = createNestedGraphStateAtDepth(3)
        assertEquals(3, graphState.navigationContext.depth)

        // When: Navigating out 3 times
        assertTrue(graphState.navigateOut())
        assertEquals(2, graphState.navigationContext.depth)

        assertTrue(graphState.navigateOut())
        assertEquals(1, graphState.navigationContext.depth)

        assertTrue(graphState.navigateOut())
        assertEquals(0, graphState.navigationContext.depth)

        // Then: Should be at root and further navigation should fail
        assertTrue(graphState.navigationContext.isAtRoot)
        assertFalse(graphState.navigateOut())
    }

    @Test
    fun `navigateOut should update parentGraphNodeId correctly`() {
        // Given: At depth 3
        val graphState = createNestedGraphStateAtDepth(3)
        assertEquals("level2", graphState.navigationContext.parentGraphNodeId)

        // When: Navigating out to depth 2
        graphState.navigateOut()

        // Then: Parent should be level1
        assertEquals("level1", graphState.navigationContext.parentGraphNodeId)

        // When: Navigating out to depth 1
        graphState.navigateOut()

        // Then: Parent should be null (at root)
        assertNull(graphState.navigationContext.parentGraphNodeId)
    }

    // ============================================
    // getGraphNodeNamesInPath Tests
    // ============================================

    @Test
    fun `getGraphNodeNamesInPath should return empty map at root`() {
        // Given: At root
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
        val graphState = GraphState(graph)

        // When/Then
        assertTrue(graphState.getGraphNodeNamesInPath().isEmpty())
    }

    @Test
    fun `getGraphNodeNamesInPath should return name for single level`() {
        // Given: Inside one GraphNode
        val child = createTestCodeNode("child", "Child", 0.0, 0.0)
        val graphNode = GraphNode(
            id = "group1",
            name = "MyGroup",
            position = Node.Position(0.0, 0.0),
            childNodes = listOf(child),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)

        val graphState = GraphState(graph)
        graphState.navigateIntoGraphNode("group1")

        // When/Then
        val names = graphState.getGraphNodeNamesInPath()
        assertEquals(1, names.size)
        assertEquals("MyGroup", names["group1"])
    }

    @Test
    fun `getGraphNodeNamesInPath should return names for multiple levels`() {
        // Given: 3 levels deep
        val graphState = createNestedGraphStateAtDepth(3)

        // When/Then
        val names = graphState.getGraphNodeNamesInPath()
        assertEquals(3, names.size)
        assertEquals("Level1", names["level1"])
        assertEquals("Level2", names["level2"])
        assertEquals("Level3", names["level3"])
    }

    // ============================================
    // getCurrentGraphNode Tests
    // ============================================

    @Test
    fun `getCurrentGraphNode should return null at root`() {
        // Given: At root
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
        val graphState = GraphState(graph)

        // When/Then
        assertNull(graphState.getCurrentGraphNode())
    }

    @Test
    fun `getCurrentGraphNode should return the GraphNode when inside one`() {
        // Given: Inside a GraphNode
        val child = createTestCodeNode("child", "Child", 0.0, 0.0)
        val graphNode = GraphNode(
            id = "group1",
            name = "MyGroup",
            position = Node.Position(0.0, 0.0),
            childNodes = listOf(child),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(graphNode)

        val graphState = GraphState(graph)
        graphState.navigateIntoGraphNode("group1")

        // When/Then
        val currentNode = graphState.getCurrentGraphNode()
        assertNotNull(currentNode)
        assertEquals("group1", currentNode.id)
        assertEquals("MyGroup", currentNode.name)
    }

    @Test
    fun `getCurrentGraphNode should return innermost GraphNode when nested`() {
        // Given: 2 levels deep
        val graphState = createNestedGraphStateAtDepth(2)

        // When/Then
        val currentNode = graphState.getCurrentGraphNode()
        assertNotNull(currentNode)
        assertEquals("level2", currentNode.id)
    }

    // ============================================
    // Helper Functions
    // ============================================

    /**
     * Creates a GraphState with nested GraphNodes and navigates to the specified depth.
     */
    private fun createNestedGraphStateAtDepth(depth: Int): GraphState {
        fun createNestedGraphNodes(level: Int, maxLevel: Int): GraphNode {
            return if (level == maxLevel) {
                val leaf = createTestCodeNode("leaf", "Leaf", 0.0, 0.0)
                GraphNode(
                    id = "level$level",
                    name = "Level$level",
                    position = Node.Position(0.0, 0.0),
                    childNodes = listOf(leaf),
                    internalConnections = emptyList(),
                    inputPorts = emptyList(),
                    outputPorts = emptyList(),
                    portMappings = emptyMap()
                )
            } else {
                GraphNode(
                    id = "level$level",
                    name = "Level$level",
                    position = Node.Position(0.0, 0.0),
                    childNodes = listOf(createNestedGraphNodes(level + 1, maxLevel)),
                    internalConnections = emptyList(),
                    inputPorts = emptyList(),
                    outputPorts = emptyList(),
                    portMappings = emptyMap()
                )
            }
        }

        val rootGraphNode = createNestedGraphNodes(1, depth)
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(rootGraphNode)

        val graphState = GraphState(graph)

        // Navigate to the target depth
        for (level in 1..depth) {
            graphState.navigateIntoGraphNode("level$level")
        }

        return graphState
    }

    // ============================================
    // Helper Functions for T056 Tests
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
