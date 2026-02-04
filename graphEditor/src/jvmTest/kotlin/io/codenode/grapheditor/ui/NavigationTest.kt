/*
 * Navigation Integration Tests
 * TDD tests for nested navigation through GraphNode hierarchy (3+ levels)
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port
import io.codenode.grapheditor.state.GraphState
import kotlin.test.*

/**
 * TDD integration tests for nested GraphNode navigation.
 * These tests verify navigation through 3+ levels of nesting,
 * ensuring correct state management throughout the navigation journey.
 *
 * Task: T068 [US6] Write integration test for nested navigation (3+ levels)
 */
class NavigationTest {

    // ============================================
    // T068: Nested Navigation Integration Tests (3+ Levels)
    // ============================================

    @Test
    fun `should navigate 3 levels deep and back to root`() {
        // Given: 3 nested GraphNodes
        val graphState = createNestedGraphState(3)

        // When/Then: Navigate in 3 levels
        assertTrue(graphState.navigateIntoGraphNode("level1"))
        assertEquals(1, graphState.navigationContext.depth)
        assertEquals("level1", graphState.navigationContext.currentGraphNodeId)

        assertTrue(graphState.navigateIntoGraphNode("level2"))
        assertEquals(2, graphState.navigationContext.depth)
        assertEquals("level2", graphState.navigationContext.currentGraphNodeId)

        assertTrue(graphState.navigateIntoGraphNode("level3"))
        assertEquals(3, graphState.navigationContext.depth)
        assertEquals("level3", graphState.navigationContext.currentGraphNodeId)

        // Navigate back out
        assertTrue(graphState.navigateOut())
        assertEquals(2, graphState.navigationContext.depth)

        assertTrue(graphState.navigateOut())
        assertEquals(1, graphState.navigationContext.depth)

        assertTrue(graphState.navigateOut())
        assertTrue(graphState.navigationContext.isAtRoot)
    }

    @Test
    fun `should navigate 5 levels deep - maximum supported nesting`() {
        // Given: 5 nested GraphNodes (maximum per spec)
        val graphState = createNestedGraphState(5)

        // When: Navigate all 5 levels in
        for (level in 1..5) {
            assertTrue(
                graphState.navigateIntoGraphNode("level$level"),
                "Should navigate into level$level"
            )
            assertEquals(level, graphState.navigationContext.depth)
        }

        // Then: Should be at maximum depth
        assertEquals(5, graphState.navigationContext.depth)
        assertEquals(
            listOf("level1", "level2", "level3", "level4", "level5"),
            graphState.navigationContext.path
        )

        // Navigate all the way back out
        for (level in 5 downTo 1) {
            assertTrue(graphState.navigateOut())
            assertEquals(level - 1, graphState.navigationContext.depth)
        }

        assertTrue(graphState.navigationContext.isAtRoot)
    }

    @Test
    fun `should show correct nodes at each navigation level`() {
        // Given: 3 nested GraphNodes
        val graphState = createNestedGraphState(3)

        // At root: should see level1 GraphNode
        var nodes = graphState.getNodesInCurrentContext()
        assertEquals(1, nodes.size)
        assertEquals("level1", nodes.first().id)

        // Navigate into level1: should see level2 GraphNode
        graphState.navigateIntoGraphNode("level1")
        nodes = graphState.getNodesInCurrentContext()
        assertEquals(1, nodes.size)
        assertEquals("level2", nodes.first().id)

        // Navigate into level2: should see level3 GraphNode
        graphState.navigateIntoGraphNode("level2")
        nodes = graphState.getNodesInCurrentContext()
        assertEquals(1, nodes.size)
        assertEquals("level3", nodes.first().id)

        // Navigate into level3: should see leaf CodeNode
        graphState.navigateIntoGraphNode("level3")
        nodes = graphState.getNodesInCurrentContext()
        assertEquals(1, nodes.size)
        assertEquals("leaf", nodes.first().id)
    }

    @Test
    fun `should show correct connections at each level`() {
        // Given: 3 nested GraphNodes with internal connections
        val graphState = createNestedGraphStateWithConnections(3)

        // At each level, should see the internal connections for that level
        graphState.navigateIntoGraphNode("level1")
        var connections = graphState.getConnectionsInCurrentContext()
        assertEquals(1, connections.size)
        assertEquals("level1_conn", connections.first().id)

        graphState.navigateIntoGraphNode("level2")
        connections = graphState.getConnectionsInCurrentContext()
        assertEquals(1, connections.size)
        assertEquals("level2_conn", connections.first().id)

        graphState.navigateIntoGraphNode("level3")
        connections = graphState.getConnectionsInCurrentContext()
        assertEquals(1, connections.size)
        assertEquals("level3_conn", connections.first().id)
    }

    @Test
    fun `should clear selection when navigating between levels`() {
        // Given: 3 nested GraphNodes
        val graphState = createNestedGraphState(3)

        // Navigate into level1
        graphState.navigateIntoGraphNode("level1")

        // Select something
        graphState.toggleNodeInSelection("level2")
        assertTrue(graphState.selectionState.selectedNodeIds.isNotEmpty())

        // Navigate into level2
        graphState.navigateIntoGraphNode("level2")

        // Selection should be cleared
        assertTrue(graphState.selectionState.selectedNodeIds.isEmpty())

        // Select something at this level
        graphState.toggleNodeInSelection("level3")
        assertTrue(graphState.selectionState.selectedNodeIds.isNotEmpty())

        // Navigate out
        graphState.navigateOut()

        // Selection should be cleared again
        assertTrue(graphState.selectionState.selectedNodeIds.isEmpty())
    }

    @Test
    fun `should maintain correct path through complex navigation`() {
        // Given: 4 nested GraphNodes
        val graphState = createNestedGraphState(4)

        // Navigate to level 3
        graphState.navigateIntoGraphNode("level1")
        graphState.navigateIntoGraphNode("level2")
        graphState.navigateIntoGraphNode("level3")
        assertEquals(listOf("level1", "level2", "level3"), graphState.navigationContext.path)

        // Navigate back to level 1
        graphState.navigateOut()
        graphState.navigateOut()
        assertEquals(listOf("level1"), graphState.navigationContext.path)

        // Navigate deeper again
        graphState.navigateIntoGraphNode("level2")
        graphState.navigateIntoGraphNode("level3")
        graphState.navigateIntoGraphNode("level4")
        assertEquals(
            listOf("level1", "level2", "level3", "level4"),
            graphState.navigationContext.path
        )
    }

    @Test
    fun `should handle navigateToDepth correctly for deep navigation`() {
        // Given: 5 levels deep
        val graphState = createNestedGraphState(5)
        for (level in 1..5) {
            graphState.navigateIntoGraphNode("level$level")
        }
        assertEquals(5, graphState.navigationContext.depth)

        // When: Navigate directly to depth 2
        graphState.navigateToDepth(2)

        // Then: Should be at depth 2
        assertEquals(2, graphState.navigationContext.depth)
        assertEquals(listOf("level1", "level2"), graphState.navigationContext.path)
        assertEquals("level2", graphState.navigationContext.currentGraphNodeId)
    }

    @Test
    fun `should handle navigateToRoot from deep nesting`() {
        // Given: 4 levels deep
        val graphState = createNestedGraphState(4)
        for (level in 1..4) {
            graphState.navigateIntoGraphNode("level$level")
        }
        assertEquals(4, graphState.navigationContext.depth)

        // When: Navigate to root
        graphState.navigateToRoot()

        // Then: Should be at root
        assertTrue(graphState.navigationContext.isAtRoot)
        assertEquals(0, graphState.navigationContext.depth)
        assertTrue(graphState.navigationContext.path.isEmpty())
    }

    @Test
    fun `should track breadcrumb names through deep navigation`() {
        // Given: 4 nested GraphNodes with meaningful names
        val graphState = createNestedGraphStateWithNames(
            listOf("Data Input", "Processing", "Validation", "Output")
        )

        // Navigate 3 levels deep
        graphState.navigateIntoGraphNode("level1")
        graphState.navigateIntoGraphNode("level2")
        graphState.navigateIntoGraphNode("level3")

        // Then: Names should be available for breadcrumb
        val names = graphState.getGraphNodeNamesInPath()
        assertEquals(3, names.size)
        assertEquals("Data Input", names["level1"])
        assertEquals("Processing", names["level2"])
        assertEquals("Validation", names["level3"])
    }

    @Test
    fun `should handle getCurrentGraphNode at each level`() {
        // Given: 3 nested GraphNodes
        val graphState = createNestedGraphState(3)

        // At root: should be null
        assertNull(graphState.getCurrentGraphNode())

        // Navigate to level 1
        graphState.navigateIntoGraphNode("level1")
        var currentNode = graphState.getCurrentGraphNode()
        assertNotNull(currentNode)
        assertEquals("level1", currentNode.id)
        assertEquals("Level1", currentNode.name)

        // Navigate to level 2
        graphState.navigateIntoGraphNode("level2")
        currentNode = graphState.getCurrentGraphNode()
        assertNotNull(currentNode)
        assertEquals("level2", currentNode.id)

        // Navigate to level 3
        graphState.navigateIntoGraphNode("level3")
        currentNode = graphState.getCurrentGraphNode()
        assertNotNull(currentNode)
        assertEquals("level3", currentNode.id)
    }

    @Test
    fun `should correctly report canNavigateOut at each level`() {
        // Given: 3 nested GraphNodes
        val graphState = createNestedGraphState(3)

        // At root: cannot navigate out
        assertFalse(graphState.navigationContext.canNavigateOut)

        // At level 1: can navigate out
        graphState.navigateIntoGraphNode("level1")
        assertTrue(graphState.navigationContext.canNavigateOut)

        // At level 2: can navigate out
        graphState.navigateIntoGraphNode("level2")
        assertTrue(graphState.navigationContext.canNavigateOut)

        // At level 3: can navigate out
        graphState.navigateIntoGraphNode("level3")
        assertTrue(graphState.navigationContext.canNavigateOut)

        // Navigate back to root
        graphState.navigateOut()
        graphState.navigateOut()
        graphState.navigateOut()
        assertFalse(graphState.navigationContext.canNavigateOut)
    }

    @Test
    fun `should handle parentGraphNodeId correctly at each level`() {
        // Given: 4 nested GraphNodes
        val graphState = createNestedGraphState(4)

        // At root: no parent
        assertNull(graphState.navigationContext.parentGraphNodeId)

        // At level 1: no parent (root is parent but null in this case)
        graphState.navigateIntoGraphNode("level1")
        assertNull(graphState.navigationContext.parentGraphNodeId)

        // At level 2: parent is level1
        graphState.navigateIntoGraphNode("level2")
        assertEquals("level1", graphState.navigationContext.parentGraphNodeId)

        // At level 3: parent is level2
        graphState.navigateIntoGraphNode("level3")
        assertEquals("level2", graphState.navigationContext.parentGraphNodeId)

        // At level 4: parent is level3
        graphState.navigateIntoGraphNode("level4")
        assertEquals("level3", graphState.navigationContext.parentGraphNodeId)
    }

    @Test
    fun `full round-trip navigation should preserve graph state`() {
        // Given: A graph with 3 levels
        val graphState = createNestedGraphState(3)
        val initialRootNodes = graphState.flowGraph.rootNodes.map { it.id }.toSet()
        val initialConnections = graphState.flowGraph.connections.map { it.id }.toSet()

        // Navigate deep and back
        graphState.navigateIntoGraphNode("level1")
        graphState.navigateIntoGraphNode("level2")
        graphState.navigateIntoGraphNode("level3")
        graphState.navigateOut()
        graphState.navigateOut()
        graphState.navigateOut()

        // Then: Graph should be unchanged
        val finalRootNodes = graphState.flowGraph.rootNodes.map { it.id }.toSet()
        val finalConnections = graphState.flowGraph.connections.map { it.id }.toSet()

        assertEquals(initialRootNodes, finalRootNodes, "Root nodes should be unchanged")
        assertEquals(initialConnections, finalConnections, "Connections should be unchanged")
    }

    // ============================================
    // Helper Functions
    // ============================================

    /**
     * Creates a GraphState with nested GraphNodes at the specified depth.
     * Structure: level1 > level2 > ... > levelN > leaf (CodeNode)
     */
    private fun createNestedGraphState(depth: Int): GraphState {
        fun createNestedGraphNodes(level: Int): GraphNode {
            return if (level == depth) {
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

        return GraphState(graph)
    }

    /**
     * Creates a GraphState with nested GraphNodes, each having internal connections.
     */
    private fun createNestedGraphStateWithConnections(depth: Int): GraphState {
        fun createNestedGraphNodes(level: Int): GraphNode {
            val child1 = createTestCodeNode("child${level}_a", "ChildA", 0.0, 0.0)
            val child2 = if (level == depth) {
                createTestCodeNode("child${level}_b", "ChildB", 100.0, 0.0)
            } else {
                createNestedGraphNodes(level + 1)
            }

            val internalConn = Connection(
                id = "level${level}_conn",
                sourceNodeId = child1.id,
                sourcePortId = "${child1.id}_out",
                targetNodeId = child2.id,
                targetPortId = if (child2 is CodeNode) "${child2.id}_in" else "group_in"
            )

            return GraphNode(
                id = "level$level",
                name = "Level$level",
                position = Node.Position(0.0, 0.0),
                childNodes = listOf(child1, child2),
                internalConnections = listOf(internalConn),
                inputPorts = emptyList(),
                outputPorts = emptyList(),
                portMappings = emptyMap()
            )
        }

        val rootGraphNode = createNestedGraphNodes(1)
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {}
            .addNode(rootGraphNode)

        return GraphState(graph)
    }

    /**
     * Creates a GraphState with nested GraphNodes using custom names for each level.
     */
    private fun createNestedGraphStateWithNames(names: List<String>): GraphState {
        fun createNestedGraphNodes(level: Int): GraphNode {
            val name = names.getOrElse(level - 1) { "Level$level" }
            return if (level == names.size) {
                val leaf = createTestCodeNode("leaf", "Leaf", 0.0, 0.0)
                GraphNode(
                    id = "level$level",
                    name = name,
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
                    name = name,
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

        return GraphState(graph)
    }

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
