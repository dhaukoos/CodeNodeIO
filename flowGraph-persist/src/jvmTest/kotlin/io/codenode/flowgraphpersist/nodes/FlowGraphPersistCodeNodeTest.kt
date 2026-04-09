/*
 * FlowGraphPersistCodeNodeTest - TDD tests for the FlowGraphPersist CodeNode
 * Tests written BEFORE implementation per FR-006
 * License: Apache 2.0
 */

package io.codenode.flowgraphpersist.nodes

import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Port
import io.codenode.fbpdsl.runtime.In2AnyOut3Runtime
import io.codenode.fbpdsl.runtime.ProcessResult3
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD tests for FlowGraphPersistCodeNode.
 *
 * Verifies the CodeNode contract:
 * - 2 input ports (flowGraphModel, ipTypeMetadata)
 * - 3 output ports (serializedOutput, loadedFlowGraph, graphNodeTemplates)
 * - anyInput mode (fires on ANY input change)
 * - Data flows correctly through channels
 * - Command processing produces output on correct ports
 * - Boundary conditions handled gracefully
 */
class FlowGraphPersistCodeNodeTest {

    // --- T012: Port signature tests ---

    @Test
    fun `node has correct name`() {
        assertEquals("FlowGraphPersist", FlowGraphPersistCodeNode.name)
    }

    @Test
    fun `node is a transformer category`() {
        assertEquals(CodeNodeType.TRANSFORMER, FlowGraphPersistCodeNode.category)
    }

    @Test
    fun `node has exactly 2 input ports`() {
        assertEquals(2, FlowGraphPersistCodeNode.inputPorts.size)
    }

    @Test
    fun `node has exactly 3 output ports`() {
        assertEquals(3, FlowGraphPersistCodeNode.outputPorts.size)
    }

    @Test
    fun `input ports have correct names`() {
        val portNames = FlowGraphPersistCodeNode.inputPorts.map { it.name }
        assertEquals(listOf("flowGraphModel", "ipTypeMetadata"), portNames)
    }

    @Test
    fun `output ports have correct names`() {
        val portNames = FlowGraphPersistCodeNode.outputPorts.map { it.name }
        assertEquals(listOf("serializedOutput", "loadedFlowGraph", "graphNodeTemplates"), portNames)
    }

    @Test
    fun `all ports use String data type`() {
        FlowGraphPersistCodeNode.inputPorts.forEach { port ->
            assertEquals(String::class, port.dataType, "Input port '${port.name}' should use String type")
        }
        FlowGraphPersistCodeNode.outputPorts.forEach { port ->
            assertEquals(String::class, port.dataType, "Output port '${port.name}' should use String type")
        }
    }

    @Test
    fun `node uses anyInput mode`() {
        assertTrue(FlowGraphPersistCodeNode.anyInput, "FlowGraphPersistCodeNode must use anyInput mode")
    }

    @Test
    fun `createRuntime returns In2AnyOut3Runtime`() {
        val runtime = FlowGraphPersistCodeNode.createRuntime("test-node")
        assertIs<In2AnyOut3Runtime<*, *, *, *, *>>(runtime)
    }

    @Test
    fun `toNodeTypeDefinition produces correct palette entry`() {
        val ntd = FlowGraphPersistCodeNode.toNodeTypeDefinition()
        assertEquals("FlowGraphPersist", ntd.name)
        assertEquals(CodeNodeType.TRANSFORMER, ntd.category)

        val inputTemplates = ntd.portTemplates.filter { it.direction == Port.Direction.INPUT }
        val outputTemplates = ntd.portTemplates.filter { it.direction == Port.Direction.OUTPUT }
        assertEquals(2, inputTemplates.size)
        assertEquals(3, outputTemplates.size)
    }

    // --- T013: Data flow tests ---

    @Test
    fun `serialize command on flowGraphModel produces output on serializedOutput`() = runTest {
        val runtime = FlowGraphPersistCodeNode.createRuntime("test-serialize") as In2AnyOut3Runtime<String, String, String, String, String>

        val inputChannel1 = Channel<String>(Channel.BUFFERED)
        val inputChannel2 = Channel<String>(Channel.BUFFERED)
        val outputChannel1 = Channel<String>(Channel.BUFFERED)
        val outputChannel2 = Channel<String>(Channel.BUFFERED)
        val outputChannel3 = Channel<String>(Channel.BUFFERED)

        runtime.inputChannel1 = inputChannel1
        runtime.inputChannel2 = inputChannel2
        runtime.outputChannel1 = outputChannel1
        runtime.outputChannel2 = outputChannel2
        runtime.outputChannel3 = outputChannel3

        runtime.start(this) {}

        // Send a serialize command
        inputChannel1.send("""{"action":"serialize","graphName":"TestGraph","graphVersion":"1.0.0","nodes":[],"connections":[]}""")
        val result = outputChannel1.receive()
        assertTrue(result.isNotEmpty(), "serializedOutput should contain .flow.kt text")
        assertTrue(result.contains("flowGraph"), "serializedOutput should contain flowGraph DSL")

        runtime.stop()
    }

    @Test
    fun `deserialize command produces output on loadedFlowGraph`() = runTest {
        val runtime = FlowGraphPersistCodeNode.createRuntime("test-deserialize") as In2AnyOut3Runtime<String, String, String, String, String>

        val inputChannel1 = Channel<String>(Channel.BUFFERED)
        val inputChannel2 = Channel<String>(Channel.BUFFERED)
        val outputChannel1 = Channel<String>(Channel.BUFFERED)
        val outputChannel2 = Channel<String>(Channel.BUFFERED)
        val outputChannel3 = Channel<String>(Channel.BUFFERED)

        runtime.inputChannel1 = inputChannel1
        runtime.inputChannel2 = inputChannel2
        runtime.outputChannel1 = outputChannel1
        runtime.outputChannel2 = outputChannel2
        runtime.outputChannel3 = outputChannel3

        runtime.start(this) {}

        // Send a deserialize command with valid .flow.kt content
        val flowKtContent = """
            import io.codenode.fbpdsl.dsl.*
            import io.codenode.fbpdsl.model.*

            val graph = flowGraph("TestGraph", version = "1.0.0") {
                val node1 = codeNode("Node1") {
                    position(0.0, 0.0)
                    input("in1", String::class)
                    output("out1", String::class)
                }
            }
        """.trimIndent()
        inputChannel1.send("""{"action":"deserialize","content":"${flowKtContent.replace("\"", "\\\"").replace("\n", "\\n")}"}""")
        val result = outputChannel2.receive()
        assertTrue(result.isNotEmpty(), "loadedFlowGraph should contain parsed graph data")
        assertTrue(result.contains("TestGraph"), "loadedFlowGraph should contain the graph name")

        runtime.stop()
    }

    @Test
    fun `listTemplates command produces output on graphNodeTemplates`() = runTest {
        val runtime = FlowGraphPersistCodeNode.createRuntime("test-templates") as In2AnyOut3Runtime<String, String, String, String, String>

        val inputChannel1 = Channel<String>(Channel.BUFFERED)
        val inputChannel2 = Channel<String>(Channel.BUFFERED)
        val outputChannel1 = Channel<String>(Channel.BUFFERED)
        val outputChannel2 = Channel<String>(Channel.BUFFERED)
        val outputChannel3 = Channel<String>(Channel.BUFFERED)

        runtime.inputChannel1 = inputChannel1
        runtime.inputChannel2 = inputChannel2
        runtime.outputChannel1 = outputChannel1
        runtime.outputChannel2 = outputChannel2
        runtime.outputChannel3 = outputChannel3

        runtime.start(this) {}

        // Send a listTemplates command
        inputChannel1.send("""{"action":"listTemplates"}""")
        val result = outputChannel3.receive()
        assertTrue(result.isNotEmpty(), "graphNodeTemplates should contain template listing")
        assertTrue(result.contains("templates"), "graphNodeTemplates should contain templates key")

        runtime.stop()
    }

    @Test
    fun `ipTypeMetadata input caches type data`() = runTest {
        val runtime = FlowGraphPersistCodeNode.createRuntime("test-cache") as In2AnyOut3Runtime<String, String, String, String, String>

        val inputChannel1 = Channel<String>(Channel.BUFFERED)
        val inputChannel2 = Channel<String>(Channel.BUFFERED)
        val outputChannel1 = Channel<String>(Channel.BUFFERED)
        val outputChannel2 = Channel<String>(Channel.BUFFERED)
        val outputChannel3 = Channel<String>(Channel.BUFFERED)

        runtime.inputChannel1 = inputChannel1
        runtime.inputChannel2 = inputChannel2
        runtime.outputChannel1 = outputChannel1
        runtime.outputChannel2 = outputChannel2
        runtime.outputChannel3 = outputChannel3

        runtime.start(this) {}

        // Send ipTypeMetadata — should be cached without crashing
        inputChannel2.send("""{"types":[{"id":"ip_string","typeName":"String"}]}""")

        // The anyInput mode should fire and produce some output (ack on one of the output channels)
        // We just verify it doesn't crash and processes the input
        val result = withTimeoutOrNull(2000) {
            // May produce output on any channel depending on implementation
            outputChannel1.tryReceive().getOrNull()
                ?: outputChannel2.tryReceive().getOrNull()
                ?: outputChannel3.tryReceive().getOrNull()
        }
        // ipTypeMetadata alone may or may not produce output — the key test is no crash

        runtime.stop()
    }

    // --- T014: Boundary condition tests ---

    @Test
    fun `empty flowGraphModel input is no-op`() = runTest {
        val runtime = FlowGraphPersistCodeNode.createRuntime("test-empty") as In2AnyOut3Runtime<String, String, String, String, String>

        val inputChannel1 = Channel<String>(Channel.BUFFERED)
        val inputChannel2 = Channel<String>(Channel.BUFFERED)
        val outputChannel1 = Channel<String>(Channel.BUFFERED)
        val outputChannel2 = Channel<String>(Channel.BUFFERED)
        val outputChannel3 = Channel<String>(Channel.BUFFERED)

        runtime.inputChannel1 = inputChannel1
        runtime.inputChannel2 = inputChannel2
        runtime.outputChannel1 = outputChannel1
        runtime.outputChannel2 = outputChannel2
        runtime.outputChannel3 = outputChannel3

        runtime.start(this) {}

        // Send empty string — should not produce output (no-op)
        inputChannel1.send("")
        val result = withTimeoutOrNull(500) { outputChannel1.receive() }
        assertNull(result, "Empty input should not produce output on serializedOutput")

        runtime.stop()
    }

    @Test
    fun `malformed JSON command does not crash`() = runTest {
        val runtime = FlowGraphPersistCodeNode.createRuntime("test-malformed") as In2AnyOut3Runtime<String, String, String, String, String>

        val inputChannel1 = Channel<String>(Channel.BUFFERED)
        val inputChannel2 = Channel<String>(Channel.BUFFERED)
        val outputChannel1 = Channel<String>(Channel.BUFFERED)
        val outputChannel2 = Channel<String>(Channel.BUFFERED)
        val outputChannel3 = Channel<String>(Channel.BUFFERED)

        runtime.inputChannel1 = inputChannel1
        runtime.inputChannel2 = inputChannel2
        runtime.outputChannel1 = outputChannel1
        runtime.outputChannel2 = outputChannel2
        runtime.outputChannel3 = outputChannel3

        runtime.start(this) {}

        // Send malformed JSON — should not crash, should produce no output (all null)
        inputChannel1.send("not valid json {{{")
        val result = withTimeoutOrNull(500) { outputChannel1.receive() }
        assertNull(result, "Malformed input should not produce output")

        runtime.stop()
    }
}
