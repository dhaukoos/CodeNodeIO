/*
 * FlowGraphComposeCodeNodeTest - TDD tests for FlowGraphComposeCodeNode
 * Tests port signatures, runtime creation, and basic data flow
 * License: Apache 2.0
 */

package io.codenode.flowgraphcompose.nodes

import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.In3AnyOut1Runtime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FlowGraphComposeCodeNodeTest {

    // --- Port Signature Tests ---

    @Test
    fun `name is FlowGraphCompose`() {
        assertEquals("FlowGraphCompose", FlowGraphComposeCodeNode.name)
    }

    @Test
    fun `category is TRANSFORMER`() {
        assertEquals(CodeNodeType.TRANSFORMER, FlowGraphComposeCodeNode.category)
    }

    @Test
    fun `anyInput is true`() {
        assertTrue(FlowGraphComposeCodeNode.anyInput)
    }

    @Test
    fun `has 3 input ports`() {
        assertEquals(3, FlowGraphComposeCodeNode.inputPorts.size)
    }

    @Test
    fun `input port 1 is flowGraphModel of type String`() {
        val port = FlowGraphComposeCodeNode.inputPorts[0]
        assertEquals("flowGraphModel", port.name)
        assertEquals(String::class, port.dataType)
    }

    @Test
    fun `input port 2 is nodeDescriptors of type String`() {
        val port = FlowGraphComposeCodeNode.inputPorts[1]
        assertEquals("nodeDescriptors", port.name)
        assertEquals(String::class, port.dataType)
    }

    @Test
    fun `input port 3 is ipTypeMetadata of type String`() {
        val port = FlowGraphComposeCodeNode.inputPorts[2]
        assertEquals("ipTypeMetadata", port.name)
        assertEquals(String::class, port.dataType)
    }

    @Test
    fun `has 1 output port`() {
        assertEquals(1, FlowGraphComposeCodeNode.outputPorts.size)
    }

    @Test
    fun `output port is graphState of type String`() {
        val port = FlowGraphComposeCodeNode.outputPorts[0]
        assertEquals("graphState", port.name)
        assertEquals(String::class, port.dataType)
    }

    // --- Runtime Creation Tests ---

    @Test
    fun `createRuntime returns In3AnyOut1Runtime`() {
        val runtime = FlowGraphComposeCodeNode.createRuntime("test")
        assertIs<In3AnyOut1Runtime<String, String, String, String>>(runtime)
    }

    // --- Data Flow Tests ---

    private data class WiredRuntime(
        val runtime: In3AnyOut1Runtime<String, String, String, String>,
        val flowGraphModel: Channel<String>,
        val nodeDescriptors: Channel<String>,
        val ipTypeMetadata: Channel<String>,
        val graphState: Channel<String>
    )

    private fun createWiredRuntime(name: String = "test"): WiredRuntime {
        @Suppress("UNCHECKED_CAST")
        val runtime = FlowGraphComposeCodeNode.createRuntime(name) as In3AnyOut1Runtime<String, String, String, String>

        val input1 = Channel<String>(Channel.BUFFERED)
        val input2 = Channel<String>(Channel.BUFFERED)
        val input3 = Channel<String>(Channel.BUFFERED)
        val output = Channel<String>(Channel.BUFFERED)

        runtime.inputChannel1 = input1
        runtime.inputChannel2 = input2
        runtime.inputChannel3 = input3
        runtime.outputChannel = output

        return WiredRuntime(runtime, input1, input2, input3, output)
    }

    @Test
    fun `flowGraphModel input triggers graphState output`() = runTest {
        val wired = createWiredRuntime("test-model-input")
        wired.runtime.start(this) {}

        wired.flowGraphModel.send("""{"nodes":["TimeEmitter","Display"]}""")
        val result = wired.graphState.receive()
        assertNotNull(result)
        assertTrue(result.isNotEmpty())

        wired.runtime.stop()
    }

    @Test
    fun `nodeDescriptors input triggers graphState output`() = runTest {
        val wired = createWiredRuntime("test-descriptors-input")
        wired.runtime.start(this) {}

        wired.nodeDescriptors.send("""[{"name":"TimeEmitter","category":"SOURCE"}]""")
        val result = wired.graphState.receive()
        assertNotNull(result)

        wired.runtime.stop()
    }

    @Test
    fun `ipTypeMetadata input triggers graphState output`() = runTest {
        val wired = createWiredRuntime("test-metadata-input")
        wired.runtime.start(this) {}

        wired.ipTypeMetadata.send("""[{"id":"ip_time","typeName":"Time"}]""")
        val result = wired.graphState.receive()
        assertNotNull(result)

        wired.runtime.stop()
    }

    @Test
    fun `output contains all three input values after receiving all inputs`() = runTest {
        val wired = createWiredRuntime("test-all-inputs")
        wired.runtime.start(this) {}

        val modelJson = """{"nodes":["TimeEmitter","Display"]}"""
        val descriptorsJson = """[{"name":"Node1"}]"""
        val metadataJson = """[{"id":"ip_any"}]"""

        wired.flowGraphModel.send(modelJson)
        wired.graphState.receive() // consume first output

        wired.nodeDescriptors.send(descriptorsJson)
        wired.graphState.receive() // consume second output

        wired.ipTypeMetadata.send(metadataJson)
        val result = wired.graphState.receive()
        assertNotNull(result)
        assertTrue(result.contains("flowGraphModel"), "Output should contain flowGraphModel key")
        assertTrue(result.contains("nodeDescriptors"), "Output should contain nodeDescriptors key")
        assertTrue(result.contains("ipTypeMetadata"), "Output should contain ipTypeMetadata key")

        wired.runtime.stop()
    }

    @Test
    fun `empty flowGraphModel still produces output`() = runTest {
        val wired = createWiredRuntime("test-empty")
        wired.runtime.start(this) {}

        wired.flowGraphModel.send("")
        val result = wired.graphState.receive()
        assertNotNull(result)

        wired.runtime.stop()
    }
}
