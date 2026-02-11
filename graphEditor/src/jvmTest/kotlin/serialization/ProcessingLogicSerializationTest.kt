/*
 * ProcessingLogic Serialization Test
 * Tests for serializing and deserializing processingLogicFile configuration
 * License: Apache 2.0
 */

package io.codenode.grapheditor.serialization

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port
import java.io.File
import kotlin.test.*

/**
 * Tests for processingLogicFile configuration serialization.
 *
 * Verifies that:
 * - T020: processingLogicFile is serialized via config() call
 * - T021: Round-trip save/reload preserves processingLogicFile configuration
 */
class ProcessingLogicSerializationTest {

    // ============================================
    // T020: Serialization Output Tests
    // ============================================

    @Test
    fun `config call should include _useCaseClass when present`() {
        // Given: A CodeNode with _useCaseClass configuration (the actual key used in Properties Panel)
        val node = CodeNode(
            id = "test_node",
            name = "TestNode",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            configuration = mapOf("_useCaseClass" to "demos/stopwatch/TimerEmitterComponent.kt")
        )
        val graph = createGraphWithNodes(listOf(node))

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: DSL should contain config call for _useCaseClass
        assertTrue(
            dsl.contains("config(\"_useCaseClass\""),
            "DSL should contain config call for _useCaseClass"
        )
        assertTrue(
            dsl.contains("demos/stopwatch/TimerEmitterComponent.kt"),
            "DSL should contain the file path"
        )
    }

    @Test
    fun `config call should include processingLogicFile when present`() {
        // Given: A CodeNode with processingLogicFile configuration
        val node = createNodeWithProcessingLogic(
            processingLogicFile = "demos/stopwatch/TimerEmitterComponent.kt"
        )
        val graph = createGraphWithNodes(listOf(node))

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: DSL should contain config call for processingLogicFile
        assertTrue(
            dsl.contains("config(\"processingLogicFile\""),
            "DSL should contain config call for processingLogicFile"
        )
        assertTrue(
            dsl.contains("demos/stopwatch/TimerEmitterComponent.kt"),
            "DSL should contain the file path"
        )
    }

    @Test
    fun `config call should serialize relative path with forward slashes`() {
        // Given: A CodeNode with relative path using forward slashes
        val relativePath = "src/components/MyComponent.kt"
        val node = createNodeWithProcessingLogic(processingLogicFile = relativePath)
        val graph = createGraphWithNodes(listOf(node))

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: DSL should preserve forward slashes
        assertTrue(dsl.contains(relativePath), "Path should be preserved with forward slashes")
        assertFalse(dsl.contains("\\\\"), "Path should not contain escaped backslashes")
    }

    @Test
    fun `config call should handle multiple configuration properties`() {
        // Given: A CodeNode with processingLogicFile and other config properties
        val node = CodeNode(
            id = "test_node",
            name = "TestNode",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            configuration = mapOf(
                "processingLogicFile" to "demos/Component.kt",
                "speedAttenuation" to "1000",
                "outputFormat" to "json"
            )
        )
        val graph = createGraphWithNodes(listOf(node))

        // When: Serializing to DSL
        val dsl = FlowGraphSerializer.serialize(graph)

        // Then: All configuration properties should be serialized
        assertTrue(dsl.contains("config(\"processingLogicFile\""), "Should have processingLogicFile")
        assertTrue(dsl.contains("config(\"speedAttenuation\""), "Should have speedAttenuation")
        assertTrue(dsl.contains("config(\"outputFormat\""), "Should have outputFormat")
    }

    // ============================================
    // T021: Round-Trip Serialization Tests
    // ============================================

    @Test
    fun `round-trip should preserve processingLogicFile configuration`() {
        // Given: A CodeNode with processingLogicFile configuration
        val originalPath = "demos/stopwatch/TimerEmitterComponent.kt"
        val node = createNodeWithProcessingLogic(processingLogicFile = originalPath)
        val originalGraph = createGraphWithNodes(listOf(node))

        // When: Serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        val result = FlowGraphDeserializer.deserialize(dsl)

        // Then: Deserialization should succeed
        assertTrue(result.isSuccess, "Deserialization should succeed: ${result.errorMessage}")
        assertNotNull(result.graph, "Deserialized graph should not be null")

        // And: processingLogicFile should be preserved
        val deserializedNode = result.graph!!.rootNodes.firstOrNull() as? CodeNode
        assertNotNull(deserializedNode, "Should have deserialized node")
        assertEquals(
            originalPath,
            deserializedNode.configuration["processingLogicFile"],
            "processingLogicFile should be preserved"
        )
    }

    @Test
    fun `file round-trip should preserve processingLogicFile configuration`() {
        // Given: A graph with processingLogicFile configuration
        val originalPath = "demos/stopwatch/DisplayReceiverComponent.kt"
        val node = createNodeWithProcessingLogic(processingLogicFile = originalPath)
        val originalGraph = createGraphWithNodes(listOf(node))

        val tempFile = File.createTempFile("test_flow_", ".flow.kts")
        tempFile.deleteOnExit()

        try {
            // When: Saving to file and reloading
            FlowGraphSerializer.serializeToFile(originalGraph, tempFile)
            val result = FlowGraphDeserializer.deserializeFromFile(tempFile)

            // Then: processingLogicFile should be preserved
            assertTrue(result.isSuccess, "File round-trip should succeed")
            val reloadedNode = result.graph!!.rootNodes.firstOrNull() as? CodeNode
            assertNotNull(reloadedNode, "Should have reloaded node")
            assertEquals(
                originalPath,
                reloadedNode.configuration["processingLogicFile"],
                "processingLogicFile should be preserved after file round-trip"
            )
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `round-trip should preserve multiple nodes with processingLogicFile`() {
        // Given: Multiple CodeNodes with different processingLogicFile values
        val timerEmitter = createNodeWithProcessingLogic(
            id = "timer_emitter",
            name = "TimerEmitter",
            processingLogicFile = "demos/stopwatch/TimerEmitterComponent.kt"
        )
        val displayReceiver = createNodeWithProcessingLogic(
            id = "display_receiver",
            name = "DisplayReceiver",
            processingLogicFile = "demos/stopwatch/DisplayReceiverComponent.kt"
        )
        val originalGraph = createGraphWithNodes(listOf(timerEmitter, displayReceiver))

        // When: Serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        val result = FlowGraphDeserializer.deserialize(dsl)

        // Then: Both processingLogicFile values should be preserved
        assertTrue(result.isSuccess, "Deserialization should succeed")
        assertEquals(2, result.graph!!.rootNodes.size, "Should have 2 nodes")

        val nodeConfigs = result.graph!!.rootNodes
            .filterIsInstance<CodeNode>()
            .associate { it.name to it.configuration["processingLogicFile"] }

        assertEquals(
            "demos/stopwatch/TimerEmitterComponent.kt",
            nodeConfigs["TimerEmitter"],
            "TimerEmitter processingLogicFile should be preserved"
        )
        assertEquals(
            "demos/stopwatch/DisplayReceiverComponent.kt",
            nodeConfigs["DisplayReceiver"],
            "DisplayReceiver processingLogicFile should be preserved"
        )
    }

    @Test
    fun `round-trip should preserve empty processingLogicFile`() {
        // Given: A CodeNode with empty processingLogicFile
        val node = createNodeWithProcessingLogic(processingLogicFile = "")
        val originalGraph = createGraphWithNodes(listOf(node))

        // When: Serializing and deserializing
        val dsl = FlowGraphSerializer.serialize(originalGraph)
        val result = FlowGraphDeserializer.deserialize(dsl)

        // Then: Empty value should be preserved (or key may be absent)
        assertTrue(result.isSuccess, "Deserialization should succeed")
        val deserializedNode = result.graph!!.rootNodes.firstOrNull() as? CodeNode
        assertNotNull(deserializedNode, "Should have deserialized node")

        // Empty config values may or may not be preserved depending on implementation
        val configValue = deserializedNode.configuration["processingLogicFile"]
        assertTrue(
            configValue == "" || configValue == null,
            "Empty processingLogicFile should result in empty or null value"
        )
    }

    // ============================================
    // Helper Functions
    // ============================================

    private fun createNodeWithProcessingLogic(
        id: String = "test_node",
        name: String = "TestNode",
        processingLogicFile: String
    ): CodeNode {
        return CodeNode(
            id = id,
            name = name,
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "${id}_output",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = Any::class,
                    owningNodeId = id
                )
            ),
            configuration = mapOf("processingLogicFile" to processingLogicFile)
        )
    }

    private fun createGraphWithNodes(nodes: List<CodeNode>): FlowGraph {
        return FlowGraph(
            id = "test_graph",
            name = "TestGraph",
            version = "1.0.0",
            description = "Test graph for processingLogicFile serialization",
            rootNodes = nodes,
            connections = emptyList(),
            metadata = emptyMap(),
            targetPlatforms = emptyList()
        )
    }
}
