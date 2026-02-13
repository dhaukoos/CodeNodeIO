/*
 * Channel Capacity Test
 * Unit tests for channel capacity mapping in code generation
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * Tests for channel capacity mapping in ModuleGenerator.
 *
 * Verifies that Connection.channelCapacity is correctly mapped to
 * Kotlin Channel constructor arguments in generated code.
 */
class ChannelCapacityTest {

    // ========== Test Fixtures ==========

    private fun createTestCodeNode(
        id: String,
        name: String,
        type: CodeNodeType = CodeNodeType.TRANSFORMER
    ): CodeNode {
        return CodeNode(
            id = id,
            name = name,
            codeNodeType = type,
            position = Node.Position(0.0, 0.0),
            inputPorts = listOf(
                Port(
                    id = "${id}_input",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = id
                )
            ),
            outputPorts = listOf(
                Port(
                    id = "${id}_output",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = id
                )
            )
        )
    }

    private fun createFlowGraphWithConnection(
        channelCapacity: Int
    ): FlowGraph {
        val sourceNode = createTestCodeNode("source", "Source", CodeNodeType.GENERATOR)
        val targetNode = createTestCodeNode("target", "Target", CodeNodeType.SINK)
        val connection = Connection(
            id = "conn_1",
            sourceNodeId = "source",
            sourcePortId = "source_output",
            targetNodeId = "target",
            targetPortId = "target_input",
            channelCapacity = channelCapacity
        )

        return FlowGraph(
            id = "test_flow",
            name = "TestFlow",
            version = "1.0.0",
            rootNodes = listOf(sourceNode, targetNode),
            connections = listOf(connection)
        )
    }

    // ========== channelCapacityArg() Unit Tests ==========

    @Test
    fun `channelCapacityArg maps 0 to Channel RENDEZVOUS`() {
        val result = ModuleGenerator.channelCapacityArg(0)
        assertEquals("Channel.RENDEZVOUS", result)
    }

    @Test
    fun `channelCapacityArg maps -1 to Channel UNLIMITED`() {
        val result = ModuleGenerator.channelCapacityArg(-1)
        assertEquals("Channel.UNLIMITED", result)
    }

    @Test
    fun `channelCapacityArg maps positive integer to its string value`() {
        assertEquals("1", ModuleGenerator.channelCapacityArg(1))
        assertEquals("5", ModuleGenerator.channelCapacityArg(5))
        assertEquals("100", ModuleGenerator.channelCapacityArg(100))
    }

    // ========== Generated Code Tests ==========

    @Test
    fun `generated code uses Channel RENDEZVOUS for capacity 0`() {
        // Given
        val flowGraph = createFlowGraphWithConnection(channelCapacity = 0)
        val generator = ModuleGenerator()

        // When
        val generatedCode = generator.generateFlowGraphClass(flowGraph, "io.test")

        // Then
        assertTrue(
            generatedCode.contains("Channel<Any>(Channel.RENDEZVOUS)"),
            "Generated code should use Channel.RENDEZVOUS for capacity 0"
        )
    }

    @Test
    fun `generated code uses buffered Channel for positive capacity`() {
        // Given
        val flowGraph = createFlowGraphWithConnection(channelCapacity = 5)
        val generator = ModuleGenerator()

        // When
        val generatedCode = generator.generateFlowGraphClass(flowGraph, "io.test")

        // Then
        assertTrue(
            generatedCode.contains("Channel<Any>(5)"),
            "Generated code should use Channel(5) for capacity 5"
        )
    }

    @Test
    fun `generated code includes Channel imports`() {
        // Given
        val flowGraph = createFlowGraphWithConnection(channelCapacity = 1)
        val generator = ModuleGenerator()

        // When
        val generatedCode = generator.generateFlowGraphClass(flowGraph, "io.test")

        // Then
        assertTrue(
            generatedCode.contains("import kotlinx.coroutines.channels.Channel"),
            "Generated code should import Channel"
        )
        assertTrue(
            generatedCode.contains("import kotlinx.coroutines.channels.SendChannel"),
            "Generated code should import SendChannel"
        )
        assertTrue(
            generatedCode.contains("import kotlinx.coroutines.channels.ReceiveChannel"),
            "Generated code should import ReceiveChannel"
        )
    }

    @Test
    fun `generated code wires channels to components`() {
        // Given
        val flowGraph = createFlowGraphWithConnection(channelCapacity = 1)
        val generator = ModuleGenerator()

        // When
        val generatedCode = generator.generateFlowGraphClass(flowGraph, "io.test")

        // Then
        assertTrue(
            generatedCode.contains("source.outputChannel = channel_conn_1"),
            "Generated code should assign channel to source outputChannel"
        )
        assertTrue(
            generatedCode.contains("target.inputChannel = channel_conn_1"),
            "Generated code should assign channel to target inputChannel"
        )
    }

    @Test
    fun `generated stop method closes channels before stopping components`() {
        // Given
        val flowGraph = createFlowGraphWithConnection(channelCapacity = 1)
        val generator = ModuleGenerator()

        // When
        val generatedCode = generator.generateFlowGraphClass(flowGraph, "io.test")

        // Then
        assertTrue(
            generatedCode.contains("channel_conn_1.close()"),
            "Generated stop() should close channels"
        )

        // Verify channels are closed before components are stopped
        val closeIndex = generatedCode.indexOf("channel_conn_1.close()")
        val stopIndex = generatedCode.indexOf("source.stop()")
        assertTrue(
            closeIndex < stopIndex,
            "Channels should be closed before components are stopped"
        )
    }
}
