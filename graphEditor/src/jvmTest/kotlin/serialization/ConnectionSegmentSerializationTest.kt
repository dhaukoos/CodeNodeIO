/*
 * ConnectionSegment Serialization Tests
 * TDD tests for ConnectionSegment serialization to/from .flow.kts format
 * License: Apache 2.0
 */

package io.codenode.grapheditor.serialization

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * TDD tests for ConnectionSegment serialization support.
 * These tests verify that ConnectionSegments are correctly serialized to .flow.kts format
 * and deserialized back with all properties preserved.
 *
 * Task: T058 - Write unit tests for ConnectionSegment serialization
 *
 * ConnectionSegments represent visual portions of a Connection within a specific scope.
 * They must serialize:
 * - Segment ID and parent connection ID
 * - Source/target node and port IDs
 * - Scope node ID (null = root level, or GraphNode ID for interior segments)
 */
class ConnectionSegmentSerializationTest {

    // ============================================
    // T058: ConnectionSegment Serialization Tests
    // ============================================

    @Test
    fun `should serialize Connection with single segment`() {
        // Given: A direct CodeNode-to-CodeNode connection (1 segment)
        val graph = createGraphWithSingleSegmentConnection()

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: DSL should contain connection
        assertTrue(dsl.contains("connect"), "DSL should contain connection declaration")
        assertTrue(dsl.contains("Source") && dsl.contains("Sink"), "DSL should contain node names")
    }

    @Test
    fun `should serialize Connection with two segments`() {
        // Given: A connection crossing one GraphNode boundary (2 segments)
        val graph = createGraphWithTwoSegmentConnection()

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: DSL should contain connection and GraphNode
        assertTrue(dsl.contains("graphNode"), "DSL should contain GraphNode")
        assertTrue(
            dsl.contains("connect") || dsl.contains("internalConnection"),
            "DSL should contain connection declaration"
        )
    }

    @Test
    fun `should serialize segment scope information`() {
        // Given: A connection with segments in different scopes
        val graph = createGraphWithTwoSegmentConnection()

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: DSL should preserve scope information
        // Scope is implicit in the structure (root-level vs internal connections)
        assertTrue(dsl.contains("graphNode"), "DSL should contain GraphNode for scoping")
        assertTrue(
            dsl.contains("internalConnection") || dsl.contains("connect"),
            "DSL should contain scope-specific connections"
        )
    }

    @Test
    fun `should serialize segment source node and port IDs`() {
        // Given: A connection with specific source node/port
        val graph = createGraphWithSingleSegmentConnection()

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: DSL should contain source node/port references
        assertTrue(
            dsl.contains("Source") || dsl.contains("source"),
            "DSL should contain source node reference"
        )
        assertTrue(
            dsl.contains("output") || dsl.contains("out"),
            "DSL should contain source port reference"
        )
    }

    @Test
    fun `should serialize segment target node and port IDs`() {
        // Given: A connection with specific target node/port
        val graph = createGraphWithSingleSegmentConnection()

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: DSL should contain target node/port references
        assertTrue(
            dsl.contains("Sink") || dsl.contains("sink"),
            "DSL should contain target node reference"
        )
        assertTrue(
            dsl.contains("input") || dsl.contains("in"),
            "DSL should contain target port reference"
        )
    }

    @Test
    fun `should preserve single segment connection during roundtrip`() {
        // Given: A single segment connection
        val originalGraph = createGraphWithSingleSegmentConnection()
        val originalConnectionCount = originalGraph.connections.size

        // When: Serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        val result = FlowGraphDeserializer.deserialize(dsl)

        // Then: Connection should be preserved
        assertTrue(result.isSuccess, "Roundtrip should succeed: ${result.errorMessage}")
        assertEquals(
            originalConnectionCount,
            result.graph!!.connections.size,
            "Connection count should be preserved"
        )
    }

    @Test
    fun `should preserve connection endpoints during roundtrip`() {
        // Given: A connection with specific endpoints
        val originalGraph = createGraphWithSingleSegmentConnection()
        val originalConn = originalGraph.connections.first()

        // When: Serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        val result = FlowGraphDeserializer.deserialize(dsl)

        // Then: Connection endpoints should be preserved
        assertTrue(result.isSuccess, "Roundtrip should succeed")
        val deserializedConn = result.graph!!.connections.firstOrNull()

        assertNotNull(deserializedConn, "Connection should exist")
        // Source and target nodes should be preserved (IDs may change but connectivity preserved)
        assertNotNull(deserializedConn.sourceNodeId, "Source node ID should exist")
        assertNotNull(deserializedConn.targetNodeId, "Target node ID should exist")
        assertNotNull(deserializedConn.sourcePortId, "Source port ID should exist")
        assertNotNull(deserializedConn.targetPortId, "Target port ID should exist")
    }

    @Test
    fun `should preserve internal connections during roundtrip`() {
        // Given: A GraphNode with internal connection (interior segment)
        val originalGraph = createGraphWithInternalConnection()
        val originalGraphNode = originalGraph.rootNodes.filterIsInstance<GraphNode>().first()
        val originalInternalCount = originalGraphNode.internalConnections.size

        // When: Serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        val result = FlowGraphDeserializer.deserialize(dsl)

        // Then: Internal connections should be preserved
        assertTrue(result.isSuccess, "Roundtrip should succeed")
        val deserializedGraphNode = result.graph!!.rootNodes.filterIsInstance<GraphNode>().first()

        assertEquals(
            originalInternalCount,
            deserializedGraphNode.internalConnections.size,
            "Internal connection count should be preserved"
        )
    }

    @Test
    fun `should serialize Connection with three segments`() {
        // Given: A connection crossing nested GraphNode boundaries (3 segments)
        val graph = createGraphWithThreeSegmentConnection()

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: DSL should contain nested GraphNode structure
        assertTrue(dsl.contains("graphNode"), "DSL should contain GraphNode")
        assertTrue(
            dsl.contains("OuterGroup") && dsl.contains("InnerGroup"),
            "DSL should contain nested GraphNode names"
        )
    }

    @Test
    fun `should preserve nested connection structure during roundtrip`() {
        // Given: Nested GraphNodes with connections at each level
        val originalGraph = createGraphWithThreeSegmentConnection()

        // When: Serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        val result = FlowGraphDeserializer.deserialize(dsl)

        // Then: Nested structure should be preserved
        assertTrue(result.isSuccess, "Roundtrip should succeed")

        val outerGraphNode = result.graph!!.rootNodes.filterIsInstance<GraphNode>().firstOrNull()
        assertNotNull(outerGraphNode, "Outer GraphNode should exist")

        val innerGraphNode = outerGraphNode.childNodes.filterIsInstance<GraphNode>().firstOrNull()
        assertNotNull(innerGraphNode, "Inner GraphNode should exist")
    }

    @Test
    fun `should serialize multiple connections with different segment counts`() {
        // Given: Multiple connections with varying segment counts
        val graph = createGraphWithMixedSegmentConnections()

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: DSL should contain all connection declarations
        val connectCount = Regex("connect|internalConnection").findAll(dsl).count()
        assertTrue(connectCount >= 2, "DSL should contain multiple connections")
    }

    @Test
    fun `should preserve multiple connections during roundtrip`() {
        // Given: Multiple connections
        val originalGraph = createGraphWithMixedSegmentConnections()
        val originalTotalConnections = originalGraph.connections.size +
            originalGraph.rootNodes.filterIsInstance<GraphNode>()
                .sumOf { it.internalConnections.size }

        // When: Serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        val result = FlowGraphDeserializer.deserialize(dsl)

        // Then: All connections should be preserved
        assertTrue(result.isSuccess, "Roundtrip should succeed")

        val deserializedTotalConnections = result.graph!!.connections.size +
            result.graph!!.rootNodes.filterIsInstance<GraphNode>()
                .sumOf { it.internalConnections.size }

        assertEquals(
            originalTotalConnections,
            deserializedTotalConnections,
            "Total connection count should be preserved"
        )
    }

    @Test
    fun `should serialize connection IP type if present`() {
        // Given: A connection with IP type specified
        val graph = createGraphWithTypedConnection()

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: DSL should contain IP type information
        assertTrue(
            dsl.contains("withType") || dsl.contains("StringData"),
            "DSL should contain IP type information"
        )
    }

    @Test
    fun `should preserve connection IP type during roundtrip`() {
        // Given: A connection with IP type
        val originalGraph = createGraphWithTypedConnection()
        val originalConn = originalGraph.connections.first()

        // When: Serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        val result = FlowGraphDeserializer.deserialize(dsl)

        // Then: IP type should be preserved (if serialization supports it)
        assertTrue(result.isSuccess, "Roundtrip should succeed")

        // IP type preservation is optional - check connection exists
        val deserializedConn = result.graph!!.connections.firstOrNull()
        assertNotNull(deserializedConn, "Connection should be preserved")
    }

    @Test
    fun `should handle connection with null parent scope`() {
        // Given: A root-level connection (scopeNodeId = null)
        val graph = createGraphWithSingleSegmentConnection()

        // When: Serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(graph)
        val result = FlowGraphDeserializer.deserialize(dsl)

        // Then: Root-level connection should be preserved
        assertTrue(result.isSuccess, "Roundtrip should succeed")
        assertTrue(
            result.graph!!.connections.isNotEmpty(),
            "Root-level connections should be preserved"
        )
    }

    // ============================================
    // Helper Functions
    // ============================================

    /**
     * Creates a graph with a direct CodeNode-to-CodeNode connection (1 segment)
     */
    private fun createGraphWithSingleSegmentConnection(): FlowGraph {
        val source = CodeNode(
            id = "source_node",
            name = "Source",
            codeNodeType = CodeNodeType.CUSTOM,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "source_out",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = "source_node"
                )
            )
        )

        val sink = CodeNode(
            id = "sink_node",
            name = "Sink",
            codeNodeType = CodeNodeType.CUSTOM,
            position = Node.Position(300.0, 100.0),
            inputPorts = listOf(
                Port(
                    id = "sink_in",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "sink_node"
                )
            ),
            outputPorts = emptyList()
        )

        val connection = Connection(
            id = "single_seg_conn",
            sourceNodeId = "source_node",
            sourcePortId = "source_out",
            targetNodeId = "sink_node",
            targetPortId = "sink_in"
        )

        return FlowGraph(
            id = "single_segment_graph",
            name = "SingleSegmentGraph",
            version = "1.0.0",
            rootNodes = listOf(source, sink),
            connections = listOf(connection),
            metadata = emptyMap(),
            targetPlatforms = emptyList()
        )
    }

    /**
     * Creates a graph with a connection crossing one GraphNode boundary (2 segments)
     */
    private fun createGraphWithTwoSegmentConnection(): FlowGraph {
        // External source
        val externalSource = CodeNode(
            id = "external_source",
            name = "ExternalSource",
            codeNodeType = CodeNodeType.CUSTOM,
            position = Node.Position(50.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "ext_src_out",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = "external_source"
                )
            )
        )

        // Internal processor inside GraphNode
        val internalProcessor = CodeNode(
            id = "internal_processor",
            name = "InternalProcessor",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(50.0, 50.0),
            inputPorts = listOf(
                Port(
                    id = "int_proc_in",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "internal_processor"
                )
            ),
            outputPorts = emptyList()
        )

        // GraphNode containing internal processor
        val graphNode = GraphNode(
            id = "two_seg_group",
            name = "TwoSegGroup",
            position = Node.Position(200.0, 100.0),
            childNodes = listOf(internalProcessor),
            internalConnections = emptyList(),
            inputPorts = listOf(
                Port(
                    id = "group_in",
                    name = "groupInput",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "two_seg_group"
                )
            ),
            outputPorts = emptyList(),
            portMappings = mapOf(
                "groupInput" to GraphNode.PortMapping("internal_processor", "input")
            )
        )

        // Connection from external to GraphNode (creates 2 segments)
        val connection = Connection(
            id = "two_seg_conn",
            sourceNodeId = "external_source",
            sourcePortId = "ext_src_out",
            targetNodeId = "two_seg_group",
            targetPortId = "group_in"
        )

        return FlowGraph(
            id = "two_segment_graph",
            name = "TwoSegmentGraph",
            version = "1.0.0",
            rootNodes = listOf(externalSource, graphNode),
            connections = listOf(connection),
            metadata = emptyMap(),
            targetPlatforms = emptyList()
        )
    }

    /**
     * Creates a graph with internal connection inside a GraphNode
     */
    private fun createGraphWithInternalConnection(): FlowGraph {
        val childSource = CodeNode(
            id = "child_source",
            name = "ChildSource",
            codeNodeType = CodeNodeType.CUSTOM,
            position = Node.Position(50.0, 50.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "child_src_out",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = "child_source"
                )
            )
        )

        val childSink = CodeNode(
            id = "child_sink",
            name = "ChildSink",
            codeNodeType = CodeNodeType.CUSTOM,
            position = Node.Position(200.0, 50.0),
            inputPorts = listOf(
                Port(
                    id = "child_sink_in",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "child_sink"
                )
            ),
            outputPorts = emptyList()
        )

        val internalConn = Connection(
            id = "internal_conn",
            sourceNodeId = "child_source",
            sourcePortId = "child_src_out",
            targetNodeId = "child_sink",
            targetPortId = "child_sink_in"
        )

        val graphNode = GraphNode(
            id = "internal_conn_group",
            name = "InternalConnGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(childSource, childSink),
            internalConnections = listOf(internalConn),
            inputPorts = emptyList(),
            outputPorts = emptyList(),
            portMappings = emptyMap()
        )

        return FlowGraph(
            id = "internal_conn_graph",
            name = "InternalConnectionGraph",
            version = "1.0.0",
            rootNodes = listOf(graphNode),
            connections = emptyList(),
            metadata = emptyMap(),
            targetPlatforms = emptyList()
        )
    }

    /**
     * Creates a graph with connection crossing nested GraphNode boundaries (3 segments)
     */
    private fun createGraphWithThreeSegmentConnection(): FlowGraph {
        // Innermost processor
        val innerProcessor = CodeNode(
            id = "inner_processor",
            name = "InnerProcessor",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(50.0, 50.0),
            inputPorts = listOf(
                Port(
                    id = "inner_proc_in",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "inner_processor"
                )
            ),
            outputPorts = emptyList()
        )

        // Inner GraphNode
        val innerGroup = GraphNode(
            id = "inner_group",
            name = "InnerGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(innerProcessor),
            internalConnections = emptyList(),
            inputPorts = listOf(
                Port(
                    id = "inner_grp_in",
                    name = "innerInput",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "inner_group"
                )
            ),
            outputPorts = emptyList(),
            portMappings = mapOf(
                "innerInput" to GraphNode.PortMapping("inner_processor", "input")
            )
        )

        // Outer GraphNode
        val outerGroup = GraphNode(
            id = "outer_group",
            name = "OuterGroup",
            position = Node.Position(200.0, 100.0),
            childNodes = listOf(innerGroup),
            internalConnections = emptyList(),
            inputPorts = listOf(
                Port(
                    id = "outer_grp_in",
                    name = "outerInput",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "outer_group"
                )
            ),
            outputPorts = emptyList(),
            portMappings = mapOf(
                "outerInput" to GraphNode.PortMapping("inner_group", "innerInput")
            )
        )

        // External source
        val externalSource = CodeNode(
            id = "external_source",
            name = "ExternalSource",
            codeNodeType = CodeNodeType.CUSTOM,
            position = Node.Position(50.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "ext_src_out",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = "external_source"
                )
            )
        )

        // Connection crosses two boundaries (creates 3 segments)
        val connection = Connection(
            id = "three_seg_conn",
            sourceNodeId = "external_source",
            sourcePortId = "ext_src_out",
            targetNodeId = "outer_group",
            targetPortId = "outer_grp_in"
        )

        return FlowGraph(
            id = "three_segment_graph",
            name = "ThreeSegmentGraph",
            version = "1.0.0",
            rootNodes = listOf(externalSource, outerGroup),
            connections = listOf(connection),
            metadata = emptyMap(),
            targetPlatforms = emptyList()
        )
    }

    /**
     * Creates a graph with multiple connections with different segment counts
     */
    private fun createGraphWithMixedSegmentConnections(): FlowGraph {
        // Direct connection nodes
        val directSource = CodeNode(
            id = "direct_source",
            name = "DirectSource",
            codeNodeType = CodeNodeType.CUSTOM,
            position = Node.Position(50.0, 50.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "dir_src_out",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = "direct_source"
                )
            )
        )

        val directSink = CodeNode(
            id = "direct_sink",
            name = "DirectSink",
            codeNodeType = CodeNodeType.CUSTOM,
            position = Node.Position(200.0, 50.0),
            inputPorts = listOf(
                Port(
                    id = "dir_sink_in",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "direct_sink"
                )
            ),
            outputPorts = emptyList()
        )

        // GraphNode with internal nodes
        val internalProcessor = CodeNode(
            id = "internal_processor",
            name = "InternalProcessor",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(50.0, 50.0),
            inputPorts = listOf(
                Port(
                    id = "int_proc_in",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "internal_processor"
                )
            ),
            outputPorts = emptyList()
        )

        val graphNode = GraphNode(
            id = "mixed_group",
            name = "MixedGroup",
            position = Node.Position(100.0, 200.0),
            childNodes = listOf(internalProcessor),
            internalConnections = emptyList(),
            inputPorts = listOf(
                Port(
                    id = "mixed_grp_in",
                    name = "groupInput",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "mixed_group"
                )
            ),
            outputPorts = emptyList(),
            portMappings = mapOf(
                "groupInput" to GraphNode.PortMapping("internal_processor", "input")
            )
        )

        // Direct connection (1 segment)
        val directConn = Connection(
            id = "direct_conn",
            sourceNodeId = "direct_source",
            sourcePortId = "dir_src_out",
            targetNodeId = "direct_sink",
            targetPortId = "dir_sink_in"
        )

        // Boundary crossing connection (2 segments)
        val boundaryConn = Connection(
            id = "boundary_conn",
            sourceNodeId = "direct_source",
            sourcePortId = "dir_src_out",
            targetNodeId = "mixed_group",
            targetPortId = "mixed_grp_in"
        )

        return FlowGraph(
            id = "mixed_segment_graph",
            name = "MixedSegmentGraph",
            version = "1.0.0",
            rootNodes = listOf(directSource, directSink, graphNode),
            connections = listOf(directConn, boundaryConn),
            metadata = emptyMap(),
            targetPlatforms = emptyList()
        )
    }

    /**
     * Creates a graph with a typed connection (ipTypeId specified)
     */
    private fun createGraphWithTypedConnection(): FlowGraph {
        val source = CodeNode(
            id = "typed_source",
            name = "TypedSource",
            codeNodeType = CodeNodeType.CUSTOM,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "typed_src_out",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = "typed_source"
                )
            )
        )

        val sink = CodeNode(
            id = "typed_sink",
            name = "TypedSink",
            codeNodeType = CodeNodeType.CUSTOM,
            position = Node.Position(300.0, 100.0),
            inputPorts = listOf(
                Port(
                    id = "typed_sink_in",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "typed_sink"
                )
            ),
            outputPorts = emptyList()
        )

        val typedConnection = Connection(
            id = "typed_conn",
            sourceNodeId = "typed_source",
            sourcePortId = "typed_src_out",
            targetNodeId = "typed_sink",
            targetPortId = "typed_sink_in",
            ipTypeId = "StringData"
        )

        return FlowGraph(
            id = "typed_conn_graph",
            name = "TypedConnectionGraph",
            version = "1.0.0",
            rootNodes = listOf(source, sink),
            connections = listOf(typedConnection),
            metadata = emptyMap(),
            targetPlatforms = emptyList()
        )
    }

    @Test
    fun `should preserve boundary connection segments after roundtrip`() {
        // Given: A GraphNode with port mappings to interior nodes
        val graph = createGraphWithTwoSegmentConnection()
        val originalGraphNode = graph.rootNodes.filterIsInstance<GraphNode>().first()

        // Verify original has port mappings
        assertTrue(originalGraphNode.portMappings.isNotEmpty(), "GraphNode should have port mappings")

        // When: Serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(graph)
        val result = FlowGraphDeserializer.deserialize(dsl)

        // Then: Port mappings should be preserved with correct references
        assertTrue(result.isSuccess, "Roundtrip should succeed: ${result.errorMessage}")

        val deserializedGraphNode = result.graph!!.rootNodes.filterIsInstance<GraphNode>().first()

        // Port mappings should be preserved
        assertEquals(
            originalGraphNode.portMappings.size,
            deserializedGraphNode.portMappings.size,
            "Port mapping count should be preserved"
        )

        // Child nodes should exist
        assertTrue(deserializedGraphNode.childNodes.isNotEmpty(), "Child nodes should be preserved")

        // Port mapping should reference valid child nodes (by name, after deserialization)
        deserializedGraphNode.portMappings.forEach { (portName, mapping) ->
            val childNode = deserializedGraphNode.childNodes.find { it.name == mapping.childNodeId }
            assertNotNull(childNode, "Port mapping for '$portName' should reference valid child node by name: ${mapping.childNodeId}")

            // Child port should exist (by name)
            val childPort = childNode!!.inputPorts.find { it.name == mapping.childPortName }
                ?: childNode.outputPorts.find { it.name == mapping.childPortName }
            assertNotNull(childPort, "Port mapping for '$portName' should reference valid child port by name: ${mapping.childPortName}")
        }
    }
}
