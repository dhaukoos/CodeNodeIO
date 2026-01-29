/*
 * GenericNodeSerializationTest - Tests for Generic Node Serialization
 * Verifies generic nodes with metadata serialize and deserialize correctly
 * License: Apache 2.0
 */

package io.codenode.grapheditor.serialization

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port
import kotlin.test.*

/**
 * Tests for serialization and deserialization of generic nodes.
 * Verifies that generic node metadata (_genericType, _useCaseClass, custom port names)
 * is preserved through serialization round-trips.
 *
 * Test organization follows TDD task breakdown:
 * - T030: _genericType metadata serialization
 * - T031: _useCaseClass metadata serialization
 * - T032: Custom port names serialization
 * - T033: Round-trip deserialization
 */
class GenericNodeSerializationTest {

    // ========== Helper Functions ==========

    private fun createGenericNode(
        id: String = "node_generic_1",
        name: String = "in2out1",
        genericType: String? = "in2out1",
        useCaseClass: String? = null,
        inputNames: List<String> = listOf("input1", "input2"),
        outputNames: List<String> = listOf("output1")
    ): CodeNode {
        val configuration = mutableMapOf<String, String>()
        genericType?.let { configuration["_genericType"] = it }
        useCaseClass?.let { configuration["_useCaseClass"] = it }

        return CodeNode(
            id = id,
            name = name,
            codeNodeType = CodeNodeType.TRANSFORMER,
            description = "Generic processing node with ${inputNames.size} inputs and ${outputNames.size} outputs",
            position = Node.Position(200.0, 200.0),
            inputPorts = inputNames.mapIndexed { index, portName ->
                Port(
                    id = "${id}_input_$portName",
                    name = portName,
                    direction = Port.Direction.INPUT,
                    dataType = Any::class,
                    owningNodeId = id
                )
            },
            outputPorts = outputNames.mapIndexed { index, portName ->
                Port(
                    id = "${id}_output_$portName",
                    name = portName,
                    direction = Port.Direction.OUTPUT,
                    dataType = Any::class,
                    owningNodeId = id
                )
            },
            configuration = configuration
        )
    }

    private fun createGraphWithGenericNode(node: CodeNode): FlowGraph {
        return FlowGraph(
            id = "graph_test",
            name = "TestGraph",
            version = "1.0.0",
            description = "Test graph with generic node",
            rootNodes = listOf(node),
            connections = emptyList(),
            metadata = emptyMap(),
            targetPlatforms = emptyList()
        )
    }

    // ========== T030: _genericType Metadata Serialization Tests ==========

    @Test
    fun `serialization includes _genericType in config`() {
        // Given a generic node with _genericType metadata
        val node = createGenericNode(genericType = "in2out1")
        val graph = createGraphWithGenericNode(node)

        // When serializing
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then the DSL should contain the _genericType config
        assertTrue(dsl.contains("_genericType"), "DSL should contain _genericType key")
        assertTrue(dsl.contains("in2out1"), "DSL should contain _genericType value")
    }

    @Test
    fun `serialization includes _genericType for in0out1 generator`() {
        val node = createGenericNode(
            name = "in0out1",
            genericType = "in0out1",
            inputNames = emptyList(),
            outputNames = listOf("output1")
        )
        val graph = createGraphWithGenericNode(node)

        val dsl = FlowGraphSerializer.serialize(graph)

        assertTrue(dsl.contains("config(\"_genericType\", \"in0out1\")"),
            "DSL should contain _genericType config for in0out1")
    }

    @Test
    fun `serialization includes _genericType for in5out5 max ports`() {
        val node = createGenericNode(
            name = "in5out5",
            genericType = "in5out5",
            inputNames = listOf("input1", "input2", "input3", "input4", "input5"),
            outputNames = listOf("output1", "output2", "output3", "output4", "output5")
        )
        val graph = createGraphWithGenericNode(node)

        val dsl = FlowGraphSerializer.serialize(graph)

        assertTrue(dsl.contains("_genericType"), "DSL should contain _genericType")
        assertTrue(dsl.contains("in5out5"), "DSL should contain in5out5 value")
    }

    @Test
    fun `serialization omits _genericType when not present`() {
        val node = createGenericNode(genericType = null)
        val graph = createGraphWithGenericNode(node)

        val dsl = FlowGraphSerializer.serialize(graph)

        assertFalse(dsl.contains("_genericType"), "DSL should not contain _genericType when null")
    }

    // ========== T031: _useCaseClass Metadata Serialization Tests ==========

    @Test
    fun `serialization includes _useCaseClass in config`() {
        // Given a generic node with _useCaseClass metadata
        val node = createGenericNode(
            useCaseClass = "com.example.validators.EmailValidator"
        )
        val graph = createGraphWithGenericNode(node)

        // When serializing
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then the DSL should contain the _useCaseClass config
        assertTrue(dsl.contains("_useCaseClass"), "DSL should contain _useCaseClass key")
        assertTrue(dsl.contains("com.example.validators.EmailValidator"),
            "DSL should contain _useCaseClass value")
    }

    @Test
    fun `serialization includes both _genericType and _useCaseClass`() {
        val node = createGenericNode(
            genericType = "in1out1",
            useCaseClass = "com.example.MyUseCase",
            inputNames = listOf("input1"),
            outputNames = listOf("output1")
        )
        val graph = createGraphWithGenericNode(node)

        val dsl = FlowGraphSerializer.serialize(graph)

        assertTrue(dsl.contains("_genericType"), "DSL should contain _genericType")
        assertTrue(dsl.contains("_useCaseClass"), "DSL should contain _useCaseClass")
        assertTrue(dsl.contains("in1out1"), "DSL should contain generic type value")
        assertTrue(dsl.contains("com.example.MyUseCase"), "DSL should contain usecase class value")
    }

    @Test
    fun `serialization omits _useCaseClass when not present`() {
        val node = createGenericNode(useCaseClass = null)
        val graph = createGraphWithGenericNode(node)

        val dsl = FlowGraphSerializer.serialize(graph)

        assertFalse(dsl.contains("_useCaseClass"), "DSL should not contain _useCaseClass when null")
    }

    @Test
    fun `serialization escapes special characters in _useCaseClass`() {
        // Edge case: class name with unusual but valid characters
        val node = createGenericNode(
            useCaseClass = "com.example.My_UseCase\$Inner"
        )
        val graph = createGraphWithGenericNode(node)

        val dsl = FlowGraphSerializer.serialize(graph)

        assertTrue(dsl.contains("_useCaseClass"), "DSL should contain _useCaseClass")
        // Should be properly escaped
        assertTrue(dsl.contains("My_UseCase"), "DSL should contain class name")
    }

    // ========== T032: Custom Port Names Serialization Tests ==========

    @Test
    fun `serialization includes custom input port names`() {
        val node = createGenericNode(
            inputNames = listOf("email", "password"),
            outputNames = listOf("output1")
        )
        val graph = createGraphWithGenericNode(node)

        val dsl = FlowGraphSerializer.serialize(graph)

        assertTrue(dsl.contains("input(\"email\""), "DSL should contain custom input name 'email'")
        assertTrue(dsl.contains("input(\"password\""), "DSL should contain custom input name 'password'")
    }

    @Test
    fun `serialization includes custom output port names`() {
        val node = createGenericNode(
            inputNames = listOf("input1"),
            outputNames = listOf("success", "error")
        )
        val graph = createGraphWithGenericNode(node)

        val dsl = FlowGraphSerializer.serialize(graph)

        assertTrue(dsl.contains("output(\"success\""), "DSL should contain custom output name 'success'")
        assertTrue(dsl.contains("output(\"error\""), "DSL should contain custom output name 'error'")
    }

    @Test
    fun `serialization includes all custom port names for complex node`() {
        val node = createGenericNode(
            name = "DataRouter",
            genericType = "in3out2",
            inputNames = listOf("data", "config", "metadata"),
            outputNames = listOf("primary", "secondary")
        )
        val graph = createGraphWithGenericNode(node)

        val dsl = FlowGraphSerializer.serialize(graph)

        // Check all inputs
        assertTrue(dsl.contains("input(\"data\""), "DSL should contain input 'data'")
        assertTrue(dsl.contains("input(\"config\""), "DSL should contain input 'config'")
        assertTrue(dsl.contains("input(\"metadata\""), "DSL should contain input 'metadata'")

        // Check all outputs
        assertTrue(dsl.contains("output(\"primary\""), "DSL should contain output 'primary'")
        assertTrue(dsl.contains("output(\"secondary\""), "DSL should contain output 'secondary'")
    }

    @Test
    fun `serialization handles empty port names for in0out0`() {
        val node = createGenericNode(
            name = "in0out0",
            genericType = "in0out0",
            inputNames = emptyList(),
            outputNames = emptyList()
        )
        val graph = createGraphWithGenericNode(node)

        val dsl = FlowGraphSerializer.serialize(graph)

        // Should not have input/output declarations
        assertFalse(dsl.contains("input(\"input"), "DSL should not have input ports")
        assertFalse(dsl.contains("output(\"output"), "DSL should not have output ports")
    }

    // ========== T033: Round-trip Deserialization Tests ==========

    @Test
    fun `roundtrip preserves _genericType metadata`() {
        // Given a generic node with _genericType
        val originalNode = createGenericNode(genericType = "in2out1")
        val originalGraph = createGraphWithGenericNode(originalNode)

        // When serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        val result = FlowGraphDeserializer.deserialize(dsl)

        // Then deserialization should succeed
        assertTrue(result.isSuccess, "Round-trip should succeed")
        assertNotNull(result.graph, "Deserialized graph should not be null")

        // And the node should exist
        assertTrue(result.graph!!.rootNodes.isNotEmpty(), "Should have nodes")
    }

    @Test
    fun `roundtrip preserves graph name and version`() {
        val originalNode = createGenericNode()
        val originalGraph = createGraphWithGenericNode(originalNode)

        val dsl = FlowGraphSerializer.serialize(originalGraph)
        val result = FlowGraphDeserializer.deserialize(dsl)

        assertTrue(result.isSuccess, "Round-trip should succeed")
        assertEquals("TestGraph", result.graph!!.name, "Graph name should be preserved")
        assertEquals("1.0.0", result.graph!!.version, "Graph version should be preserved")
    }

    @Test
    fun `roundtrip preserves node count`() {
        val node1 = createGenericNode(
            id = "node_1",
            name = "Validator",
            genericType = "in1out2",
            inputNames = listOf("data"),
            outputNames = listOf("valid", "invalid")
        )
        val node2 = createGenericNode(
            id = "node_2",
            name = "Transformer",
            genericType = "in1out1",
            inputNames = listOf("input"),
            outputNames = listOf("output")
        )

        val graph = FlowGraph(
            id = "graph_test",
            name = "MultiNodeGraph",
            version = "1.0.0",
            description = "Graph with multiple generic nodes",
            rootNodes = listOf(node1, node2),
            connections = emptyList(),
            metadata = emptyMap(),
            targetPlatforms = emptyList()
        )

        val dsl = FlowGraphSerializer.serialize(graph)
        val result = FlowGraphDeserializer.deserialize(dsl)

        assertTrue(result.isSuccess, "Round-trip should succeed")
        assertEquals(2, result.graph!!.rootNodes.size, "Node count should be preserved")
    }

    @Test
    fun `roundtrip preserves node positions`() {
        val originalNode = createGenericNode()
        val originalGraph = createGraphWithGenericNode(originalNode)

        val dsl = FlowGraphSerializer.serialize(originalGraph)
        val result = FlowGraphDeserializer.deserialize(dsl)

        assertTrue(result.isSuccess, "Round-trip should succeed")
        val deserializedNode = result.graph!!.rootNodes.first()
        assertEquals(200.0, deserializedNode.position.x, 0.1, "X position should be preserved")
        assertEquals(200.0, deserializedNode.position.y, 0.1, "Y position should be preserved")
    }

    @Test
    fun `roundtrip preserves port count`() {
        val originalNode = createGenericNode(
            inputNames = listOf("a", "b", "c"),
            outputNames = listOf("x", "y")
        )
        val originalGraph = createGraphWithGenericNode(originalNode)

        val dsl = FlowGraphSerializer.serialize(originalGraph)
        val result = FlowGraphDeserializer.deserialize(dsl)

        assertTrue(result.isSuccess, "Round-trip should succeed")
        val deserializedNode = result.graph!!.rootNodes.first()
        assertEquals(3, deserializedNode.inputPorts.size, "Input port count should be preserved")
        assertEquals(2, deserializedNode.outputPorts.size, "Output port count should be preserved")
    }

    @Test
    fun `roundtrip works with full generic node configuration`() {
        // Create a fully configured generic node
        val originalNode = createGenericNode(
            id = "node_full",
            name = "EmailValidator",
            genericType = "in2out2",
            useCaseClass = "com.example.EmailValidatorUseCase",
            inputNames = listOf("email", "rules"),
            outputNames = listOf("valid", "invalid")
        )
        val originalGraph = FlowGraph(
            id = "graph_full",
            name = "ValidationGraph",
            version = "2.0.0",
            description = "Email validation workflow",
            rootNodes = listOf(originalNode),
            connections = emptyList(),
            metadata = emptyMap(),
            targetPlatforms = emptyList()
        )

        // Serialize
        val dsl = FlowGraphSerializer.serialize(originalGraph)

        // Verify serialization includes all metadata
        assertTrue(dsl.contains("_genericType"), "Serialized DSL should contain _genericType")
        assertTrue(dsl.contains("_useCaseClass"), "Serialized DSL should contain _useCaseClass")
        assertTrue(dsl.contains("email"), "Serialized DSL should contain custom port name")

        // Deserialize
        val result = FlowGraphDeserializer.deserialize(dsl)

        // Verify basic round-trip success
        assertTrue(result.isSuccess, "Full configuration round-trip should succeed")
        assertNotNull(result.graph, "Deserialized graph should not be null")
        assertEquals("ValidationGraph", result.graph!!.name, "Graph name should be preserved")
        assertEquals(1, result.graph!!.rootNodes.size, "Should have 1 node")
    }

    @Test
    fun `roundtrip preserves _genericType in node configuration`() {
        // Given a node with _genericType config
        val originalNode = createGenericNode(
            genericType = "in3out2",
            inputNames = listOf("a", "b", "c"),
            outputNames = listOf("x", "y")
        )
        val originalGraph = createGraphWithGenericNode(originalNode)

        // When serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        val result = FlowGraphDeserializer.deserialize(dsl)

        // Then configuration should be preserved
        assertTrue(result.isSuccess, "Round-trip should succeed")
        val deserializedNode = result.graph!!.rootNodes.first() as CodeNode
        assertEquals("in3out2", deserializedNode.configuration["_genericType"],
            "_genericType should be preserved through round-trip")
    }

    @Test
    fun `roundtrip preserves _useCaseClass in node configuration`() {
        // Given a node with _useCaseClass config
        val originalNode = createGenericNode(
            genericType = "in1out1",
            useCaseClass = "com.example.MyUseCase",
            inputNames = listOf("input"),
            outputNames = listOf("output")
        )
        val originalGraph = createGraphWithGenericNode(originalNode)

        // When serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        val result = FlowGraphDeserializer.deserialize(dsl)

        // Then configuration should be preserved
        assertTrue(result.isSuccess, "Round-trip should succeed")
        val deserializedNode = result.graph!!.rootNodes.first() as CodeNode
        assertEquals("com.example.MyUseCase", deserializedNode.configuration["_useCaseClass"],
            "_useCaseClass should be preserved through round-trip")
    }

    @Test
    fun `roundtrip preserves custom port names`() {
        // Given a node with custom port names
        val originalNode = createGenericNode(
            inputNames = listOf("email", "password"),
            outputNames = listOf("success", "error")
        )
        val originalGraph = createGraphWithGenericNode(originalNode)

        // When serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        val result = FlowGraphDeserializer.deserialize(dsl)

        // Then port names should be preserved
        assertTrue(result.isSuccess, "Round-trip should succeed")
        val deserializedNode = result.graph!!.rootNodes.first()
        val inputNames = deserializedNode.inputPorts.map { it.name }
        val outputNames = deserializedNode.outputPorts.map { it.name }

        assertTrue(inputNames.contains("email"), "Custom input port 'email' should be preserved")
        assertTrue(inputNames.contains("password"), "Custom input port 'password' should be preserved")
        assertTrue(outputNames.contains("success"), "Custom output port 'success' should be preserved")
        assertTrue(outputNames.contains("error"), "Custom output port 'error' should be preserved")
    }
}
