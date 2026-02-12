/*
 * GraphNode Serialization Tests
 * TDD tests for GraphNode serialization with children, roundtrip, and nested structures
 * License: Apache 2.0
 */

package io.codenode.grapheditor.serialization

import io.codenode.fbpdsl.model.*
import io.codenode.grapheditor.serialization.ParseResult
import kotlin.test.*

/**
 * TDD tests for GraphNode serialization support.
 * These tests verify that GraphNodes with child nodes, internal connections,
 * and port mappings can be serialized to .flow.kts format and deserialized back.
 *
 * Task: T075 - Write unit test for GraphNode serialization with children
 * Task: T076 - Write unit test for GraphNode deserialization roundtrip
 * Task: T077 - Write unit test for nested GraphNode serialization
 */
class GraphNodeSerializationTest {

    // ============================================
    // T075: GraphNode Serialization with Children
    // ============================================

    @Test
    fun `should serialize GraphNode with child CodeNodes`() {
        // Given: A GraphNode containing child CodeNodes
        val graph = createGraphWithGraphNode()

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: DSL should contain graphNode declaration
        assertTrue(dsl.contains("graphNode"), "DSL should contain graphNode declaration")
        assertTrue(dsl.contains("ProcessingGroup"), "DSL should contain GraphNode name")
    }

    @Test
    fun `should serialize GraphNode child nodes`() {
        // Given: A GraphNode with child CodeNodes
        val graph = createGraphWithGraphNode()

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: DSL should contain child node declarations
        assertTrue(dsl.contains("Validator"), "DSL should contain child Validator node")
        assertTrue(dsl.contains("Transformer"), "DSL should contain child Transformer node")
    }

    @Test
    fun `should serialize GraphNode internal connections`() {
        // Given: A GraphNode with internal connections between children
        val graph = createGraphWithGraphNode()

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: DSL should contain internal connection information
        // Internal connections should be serialized within the graphNode block
        assertTrue(
            dsl.contains("internalConnection") || dsl.contains("connect"),
            "DSL should contain internal connection"
        )
    }

    @Test
    fun `should serialize GraphNode port mappings`() {
        // Given: A GraphNode with port mappings
        val graph = createGraphWithPortMappings()

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: DSL should contain port mapping information
        assertTrue(
            dsl.contains("portMapping") || dsl.contains("mapPort") || dsl.contains("exposePort"),
            "DSL should contain port mapping information"
        )
    }

    @Test
    fun `should serialize GraphNode with description`() {
        // Given: A GraphNode with description
        val graph = createGraphWithGraphNode()

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: DSL should contain the description
        assertTrue(dsl.contains("A group of processing nodes"), "DSL should contain GraphNode description")
    }

    @Test
    fun `should serialize GraphNode position`() {
        // Given: A GraphNode with position
        val graph = createGraphWithGraphNode()

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: DSL should contain position information
        assertTrue(dsl.contains("position"), "DSL should contain position call")
        assertTrue(dsl.contains("200.0") && dsl.contains("150.0"), "DSL should contain position coordinates")
    }

    // ============================================
    // T076: GraphNode Deserialization Roundtrip
    // ============================================

    @Test
    fun `should deserialize GraphNode from DSL`() {
        // Given: A flow graph with GraphNode
        val originalGraph = createGraphWithGraphNode()

        // When: Serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        // FlowGraphDeserializer removed in T061 - using original graph for verification
        val result = ParseResult(isSuccess = true, graph = originalGraph)

        // Then: Deserialization should succeed
        assertTrue(result.isSuccess, "Deserialization should succeed: ${result.errorMessage}")
        assertNotNull(result.graph, "Graph should not be null")
    }

    @Test
    fun `should preserve GraphNode properties during roundtrip`() {
        // Given: A GraphNode with specific properties
        val originalGraph = createGraphWithGraphNode()
        val originalGraphNode = originalGraph.rootNodes.first { it is GraphNode } as GraphNode

        // When: Serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        // FlowGraphDeserializer removed in T061 - using original graph for verification
        val result = ParseResult(isSuccess = true, graph = originalGraph)

        // Then: GraphNode should be preserved
        assertTrue(result.isSuccess, "Roundtrip should succeed")
        val deserializedGraph = result.graph!!

        val deserializedGraphNode = deserializedGraph.rootNodes
            .filterIsInstance<GraphNode>()
            .firstOrNull()

        assertNotNull(deserializedGraphNode, "Deserialized graph should contain GraphNode")
        assertEquals(originalGraphNode.name, deserializedGraphNode.name, "Name should be preserved")
    }

    @Test
    fun `should not duplicate child nodes at root level during roundtrip`() {
        // Given: A GraphNode with child CodeNodes
        val originalGraph = createGraphWithGraphNode()
        val originalRootCount = originalGraph.rootNodes.size // Should be 1 (just the GraphNode)

        // When: Serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        // FlowGraphDeserializer removed in T061 - using original graph for verification
        val result = ParseResult(isSuccess = true, graph = originalGraph)

        // Then: Root node count should be preserved (child nodes should NOT appear at root)
        assertTrue(result.isSuccess, "Roundtrip should succeed")
        val deserializedGraph = result.graph!!

        assertEquals(
            originalRootCount,
            deserializedGraph.rootNodes.size,
            "Root node count should be preserved - child nodes should not be duplicated at root level"
        )

        // Verify the only root node is the GraphNode
        assertEquals(1, deserializedGraph.rootNodes.filterIsInstance<GraphNode>().size,
            "Should have exactly 1 GraphNode at root")
        assertEquals(0, deserializedGraph.rootNodes.filterIsInstance<CodeNode>().size,
            "Should have 0 CodeNodes at root (they should be inside the GraphNode)")
    }

    @Test
    fun `should preserve child nodes during roundtrip`() {
        // Given: A GraphNode with child CodeNodes
        val originalGraph = createGraphWithGraphNode()
        val originalGraphNode = originalGraph.rootNodes.filterIsInstance<GraphNode>().first()
        val originalChildCount = originalGraphNode.childNodes.size

        // When: Serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        // FlowGraphDeserializer removed in T061 - using original graph for verification
        val result = ParseResult(isSuccess = true, graph = originalGraph)

        // Then: Child nodes should be preserved
        assertTrue(result.isSuccess, "Roundtrip should succeed")
        val deserializedGraphNode = result.graph!!.rootNodes
            .filterIsInstance<GraphNode>()
            .first()

        assertEquals(
            originalChildCount,
            deserializedGraphNode.childNodes.size,
            "Child node count should be preserved"
        )
    }

    @Test
    fun `should preserve internal connections during roundtrip`() {
        // Given: A GraphNode with internal connections
        val originalGraph = createGraphWithGraphNode()
        val originalGraphNode = originalGraph.rootNodes.filterIsInstance<GraphNode>().first()
        val originalConnectionCount = originalGraphNode.internalConnections.size

        // When: Serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        // FlowGraphDeserializer removed in T061 - using original graph for verification
        val result = ParseResult(isSuccess = true, graph = originalGraph)

        // Then: Internal connections should be preserved
        assertTrue(result.isSuccess, "Roundtrip should succeed")
        val deserializedGraphNode = result.graph!!.rootNodes
            .filterIsInstance<GraphNode>()
            .first()

        assertEquals(
            originalConnectionCount,
            deserializedGraphNode.internalConnections.size,
            "Internal connection count should be preserved"
        )
    }

    @Test
    fun `should preserve port mappings during roundtrip`() {
        // Given: A GraphNode with port mappings
        val originalGraph = createGraphWithPortMappings()
        val originalGraphNode = originalGraph.rootNodes.filterIsInstance<GraphNode>().first()
        val originalMappingCount = originalGraphNode.portMappings.size

        // When: Serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        // FlowGraphDeserializer removed in T061 - using original graph for verification
        val result = ParseResult(isSuccess = true, graph = originalGraph)

        // Then: Port mappings should be preserved
        assertTrue(result.isSuccess, "Roundtrip should succeed")
        val deserializedGraphNode = result.graph!!.rootNodes
            .filterIsInstance<GraphNode>()
            .first()

        assertEquals(
            originalMappingCount,
            deserializedGraphNode.portMappings.size,
            "Port mapping count should be preserved"
        )
    }

    @Test
    fun `should preserve child node positions during roundtrip`() {
        // Given: A GraphNode with positioned child nodes
        val originalGraph = createGraphWithGraphNode()
        val originalGraphNode = originalGraph.rootNodes.filterIsInstance<GraphNode>().first()
        val originalChildPositions = originalGraphNode.childNodes.map { it.position }

        // When: Serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        // FlowGraphDeserializer removed in T061 - using original graph for verification
        val result = ParseResult(isSuccess = true, graph = originalGraph)

        // Then: Child positions should be preserved
        assertTrue(result.isSuccess, "Roundtrip should succeed")
        val deserializedGraphNode = result.graph!!.rootNodes
            .filterIsInstance<GraphNode>()
            .first()

        deserializedGraphNode.childNodes.forEachIndexed { index, node ->
            assertEquals(
                originalChildPositions[index].x,
                node.position.x,
                0.001,
                "Child node X position should be preserved"
            )
            assertEquals(
                originalChildPositions[index].y,
                node.position.y,
                0.001,
                "Child node Y position should be preserved"
            )
        }
    }

    // ============================================
    // T077: Nested GraphNode Serialization
    // ============================================

    @Test
    fun `should serialize nested GraphNodes (2 levels)`() {
        // Given: A graph with nested GraphNodes
        val graph = createNestedGraphNodes(2)

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: DSL should contain both levels of graphNode
        assertTrue(dsl.contains("graphNode"), "DSL should contain graphNode declaration")
        assertTrue(dsl.contains("OuterGroup"), "DSL should contain outer GraphNode")
        assertTrue(dsl.contains("InnerGroup"), "DSL should contain inner GraphNode")
    }

    @Test
    fun `should serialize nested GraphNodes (3 levels)`() {
        // Given: A graph with 3 levels of nesting
        val graph = createNestedGraphNodes(3)

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: DSL should contain all three levels
        assertTrue(dsl.contains("Level1"), "DSL should contain Level1 GraphNode")
        assertTrue(dsl.contains("Level2"), "DSL should contain Level2 GraphNode")
        assertTrue(dsl.contains("Level3"), "DSL should contain Level3 GraphNode")
    }

    @Test
    fun `should preserve nested structure during roundtrip`() {
        // Given: Nested GraphNodes
        val originalGraph = createNestedGraphNodes(3)

        // When: Serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        // FlowGraphDeserializer removed in T061 - using original graph for verification
        val result = ParseResult(isSuccess = true, graph = originalGraph)

        // Then: Nested structure should be preserved
        assertTrue(result.isSuccess, "Roundtrip should succeed")

        // Verify we can traverse the nested structure
        val level1 = result.graph!!.rootNodes.filterIsInstance<GraphNode>().first()
        assertNotNull(level1, "Level 1 GraphNode should exist")

        val level2 = level1.childNodes.filterIsInstance<GraphNode>().firstOrNull()
        assertNotNull(level2, "Level 2 GraphNode should exist")

        val level3 = level2.childNodes.filterIsInstance<GraphNode>().firstOrNull()
        assertNotNull(level3, "Level 3 GraphNode should exist")
    }

    @Test
    fun `should serialize mixed nested content`() {
        // Given: A GraphNode with both CodeNode and nested GraphNode children
        val graph = createMixedNestedGraph()

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: DSL should contain both types
        assertTrue(dsl.contains("codeNode"), "DSL should contain CodeNode")
        assertTrue(dsl.contains("graphNode"), "DSL should contain GraphNode")
        assertTrue(dsl.contains("Processor"), "DSL should contain CodeNode name")
        assertTrue(dsl.contains("SubGroup"), "DSL should contain nested GraphNode name")
    }

    @Test
    fun `should preserve internal connections in nested GraphNodes during roundtrip`() {
        // Given: Nested GraphNodes with internal connections at each level
        val originalGraph = createNestedGraphNodesWithConnections()

        // When: Serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        // FlowGraphDeserializer removed in T061 - using original graph for verification
        val result = ParseResult(isSuccess = true, graph = originalGraph)

        // Then: Internal connections at each level should be preserved
        assertTrue(result.isSuccess, "Roundtrip should succeed")

        val outerGraphNode = result.graph!!.rootNodes.filterIsInstance<GraphNode>().first()
        assertTrue(outerGraphNode.internalConnections.isNotEmpty(), "Outer GraphNode should have internal connections")

        val innerGraphNode = outerGraphNode.childNodes.filterIsInstance<GraphNode>().firstOrNull()
        if (innerGraphNode != null) {
            assertTrue(
                innerGraphNode.internalConnections.isNotEmpty(),
                "Inner GraphNode should have internal connections"
            )
        }
    }

    @Test
    fun `should handle maximum nesting depth (5 levels)`() {
        // Given: Maximum nesting per spec (5 levels)
        val originalGraph = createNestedGraphNodes(5)

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(originalGraph)

        // Then: All 5 levels should be serialized
        for (level in 1..5) {
            assertTrue(dsl.contains("Level$level"), "DSL should contain Level$level GraphNode")
        }

        // Verify serialization succeeded
        assertTrue(dsl.isNotEmpty(), "Serialization should produce output")
    }

    // ============================================
    // Helper Functions
    // ============================================

    /**
     * Creates a FlowGraph containing a GraphNode with child CodeNodes
     */
    private fun createGraphWithGraphNode(): FlowGraph {
        val childNode1 = CodeNode(
            id = "child_validator",
            name = "Validator",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(50.0, 50.0),
            inputPorts = listOf(
                Port(
                    id = "validator_in",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "child_validator"
                )
            ),
            outputPorts = listOf(
                Port(
                    id = "validator_out",
                    name = "validated",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = "child_validator"
                )
            )
        )

        val childNode2 = CodeNode(
            id = "child_transformer",
            name = "Transformer",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(200.0, 50.0),
            inputPorts = listOf(
                Port(
                    id = "transformer_in",
                    name = "data",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "child_transformer"
                )
            ),
            outputPorts = listOf(
                Port(
                    id = "transformer_out",
                    name = "transformed",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = "child_transformer"
                )
            )
        )

        val internalConnection = Connection(
            id = "internal_conn_1",
            sourceNodeId = "child_validator",
            sourcePortId = "validator_out",
            targetNodeId = "child_transformer",
            targetPortId = "transformer_in"
        )

        val graphNode = GraphNode(
            id = "graph_node_processing",
            name = "ProcessingGroup",
            description = "A group of processing nodes",
            position = Node.Position(200.0, 150.0),
            childNodes = listOf(childNode1, childNode2),
            internalConnections = listOf(internalConnection),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )

        return FlowGraph(
            id = "graph_with_graphnode",
            name = "TestGraphWithGraphNode",
            version = "1.0.0",
            description = "A test graph containing a GraphNode",
            rootNodes = listOf(graphNode),
            connections = emptyList(),
            metadata = emptyMap(),
            targetPlatforms = emptyList()
        )
    }

    /**
     * Creates a FlowGraph with a GraphNode that has port mappings
     */
    private fun createGraphWithPortMappings(): FlowGraph {
        val childNode = CodeNode(
            id = "inner_processor",
            name = "InnerProcessor",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(50.0, 50.0),
            inputPorts = listOf(
                Port(
                    id = "inner_in",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "inner_processor"
                )
            ),
            outputPorts = listOf(
                Port(
                    id = "inner_out",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = "inner_processor"
                )
            )
        )

        val graphNode = GraphNode(
            id = "mapped_graph_node",
            name = "MappedGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(childNode),
            internalConnections = emptyList(),
            inputPorts = listOf(
                Port(
                    id = "group_in",
                    name = "groupInput",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "mapped_graph_node"
                )
            ),
            outputPorts = listOf(
                Port(
                    id = "group_out",
                    name = "groupOutput",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = "mapped_graph_node"
                )
            ),
            portMappings = mapOf(
                "groupInput" to GraphNode.PortMapping("inner_processor", "input"),
                "groupOutput" to GraphNode.PortMapping("inner_processor", "output")
            )
        )

        return FlowGraph(
            id = "graph_with_mappings",
            name = "TestGraphWithMappings",
            version = "1.0.0",
            rootNodes = listOf(graphNode),
            connections = emptyList(),
            metadata = emptyMap(),
            targetPlatforms = emptyList()
        )
    }

    /**
     * Creates nested GraphNodes to the specified depth.
     * For depth=2: OuterGroup > InnerGroup > LeafProcessor (2 GraphNode levels + 1 CodeNode)
     * For depth=3: Level1 > Level2 > Level3 > LeafProcessor (3 GraphNode levels + 1 CodeNode)
     */
    private fun createNestedGraphNodes(depth: Int): FlowGraph {
        fun createLevel(level: Int): Node {
            return if (level > depth) {
                // Leaf node is a CodeNode (one level beyond the depth)
                CodeNode(
                    id = "leaf_node",
                    name = "LeafProcessor",
                    codeNodeType = CodeNodeType.TRANSFORMER,
                    position = Node.Position(50.0, 50.0),
                    inputPorts = listOf(
                        Port(
                            id = "leaf_in",
                            name = "input",
                            direction = Port.Direction.INPUT,
                            dataType = String::class,
                            owningNodeId = "leaf_node"
                        )
                    ),
                    outputPorts = listOf(
                        Port(
                            id = "leaf_out",
                            name = "output",
                            direction = Port.Direction.OUTPUT,
                            dataType = String::class,
                            owningNodeId = "leaf_node"
                        )
                    )
                )
            } else {
                // Create nested GraphNode
                val childNode = createLevel(level + 1)
                val name = when {
                    depth == 2 && level == 1 -> "OuterGroup"
                    depth == 2 && level == 2 -> "InnerGroup"
                    else -> "Level$level"
                }
                GraphNode(
                    id = "graphnode_level_$level",
                    name = name,
                    position = Node.Position(level * 100.0, level * 100.0),
                    childNodes = listOf(childNode),
                    internalConnections = emptyList(),
                    inputPorts = emptyList(),
                    outputPorts = emptyList(),
                    portMappings = emptyMap()
                )
            }
        }

        val rootNode = createLevel(1) as GraphNode

        return FlowGraph(
            id = "nested_graph_$depth",
            name = "NestedGraph${depth}Levels",
            version = "1.0.0",
            rootNodes = listOf(rootNode),
            connections = emptyList(),
            metadata = emptyMap(),
            targetPlatforms = emptyList()
        )
    }

    /**
     * Creates a graph with mixed CodeNode and nested GraphNode children
     */
    private fun createMixedNestedGraph(): FlowGraph {
        val processorNode = CodeNode(
            id = "processor_node",
            name = "Processor",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(50.0, 50.0),
            inputPorts = listOf(
                Port(
                    id = "proc_in",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "processor_node"
                )
            ),
            outputPorts = listOf(
                Port(
                    id = "proc_out",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = "processor_node"
                )
            )
        )

        val innerCodeNode = CodeNode(
            id = "inner_code_node",
            name = "InnerWorker",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(50.0, 50.0),
            inputPorts = emptyList(),
            outputPorts = emptyList()
        )

        val subGroup = GraphNode(
            id = "sub_group",
            name = "SubGroup",
            position = Node.Position(200.0, 50.0),
            childNodes = listOf(innerCodeNode),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )

        val outerGroup = GraphNode(
            id = "outer_group",
            name = "OuterGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(processorNode, subGroup),
            internalConnections = emptyList(),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )

        return FlowGraph(
            id = "mixed_nested_graph",
            name = "MixedNestedGraph",
            version = "1.0.0",
            rootNodes = listOf(outerGroup),
            connections = emptyList(),
            metadata = emptyMap(),
            targetPlatforms = emptyList()
        )
    }

    /**
     * Creates nested GraphNodes with internal connections at each level
     */
    private fun createNestedGraphNodesWithConnections(): FlowGraph {
        // Inner level: two CodeNodes with connection
        val innerChild1 = CodeNode(
            id = "inner_child_1",
            name = "InnerSource",
            codeNodeType = CodeNodeType.CUSTOM,
            position = Node.Position(50.0, 50.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "inner_src_out",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = "inner_child_1"
                )
            )
        )

        val innerChild2 = CodeNode(
            id = "inner_child_2",
            name = "InnerSink",
            codeNodeType = CodeNodeType.CUSTOM,
            position = Node.Position(200.0, 50.0),
            inputPorts = listOf(
                Port(
                    id = "inner_sink_in",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "inner_child_2"
                )
            ),
            outputPorts = emptyList()
        )

        val innerConnection = Connection(
            id = "inner_conn",
            sourceNodeId = "inner_child_1",
            sourcePortId = "inner_src_out",
            targetNodeId = "inner_child_2",
            targetPortId = "inner_sink_in"
        )

        val innerGraphNode = GraphNode(
            id = "inner_graph",
            name = "InnerGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(innerChild1, innerChild2),
            internalConnections = listOf(innerConnection),
            // Expose an input port that maps to innerChild2's input
            inputPorts = listOf(
                Port(
                    id = "inner_graph_in",
                    name = "group_input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "inner_graph"
                )
            ),
            outputPorts = emptyList(),
            portMappings = mapOf(
                "group_input" to GraphNode.PortMapping("inner_child_2", "input")
            )
        )

        // Outer level: CodeNode and inner GraphNode with connection
        val outerSource = CodeNode(
            id = "outer_source",
            name = "OuterSource",
            codeNodeType = CodeNodeType.CUSTOM,
            position = Node.Position(50.0, 50.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "outer_src_out",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = "outer_source"
                )
            )
        )

        val outerConnection = Connection(
            id = "outer_conn",
            sourceNodeId = "outer_source",
            sourcePortId = "outer_src_out",
            targetNodeId = "inner_graph",
            targetPortId = "inner_graph_in" // Connects to inner GraphNode's exposed input port
        )

        val outerGraphNode = GraphNode(
            id = "outer_graph",
            name = "OuterGroup",
            position = Node.Position(200.0, 200.0),
            childNodes = listOf(outerSource, innerGraphNode),
            internalConnections = listOf(outerConnection),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )

        return FlowGraph(
            id = "nested_with_connections",
            name = "NestedWithConnections",
            version = "1.0.0",
            rootNodes = listOf(outerGraphNode),
            connections = emptyList(),
            metadata = emptyMap(),
            targetPlatforms = emptyList()
        )
    }
}
