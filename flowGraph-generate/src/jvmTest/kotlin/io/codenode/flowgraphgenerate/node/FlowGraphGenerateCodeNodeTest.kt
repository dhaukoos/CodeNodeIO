/*
 * FlowGraphGenerateCodeNodeTest - TDD tests for FlowGraphGenerateCodeNode
 * Tests port signatures, runtime creation, and basic data flow
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.node

import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.In3AnyOut1Runtime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FlowGraphGenerateCodeNodeTest {

    // --- Port Signature Tests ---

    @Test
    fun `name is FlowGraphGenerate`() {
        assertEquals("FlowGraphGenerate", FlowGraphGenerateCodeNode.name)
    }

    @Test
    fun `category is TRANSFORMER`() {
        assertEquals(CodeNodeType.TRANSFORMER, FlowGraphGenerateCodeNode.category)
    }

    @Test
    fun `anyInput is true`() {
        assertTrue(FlowGraphGenerateCodeNode.anyInput)
    }

    @Test
    fun `has 3 input ports`() {
        assertEquals(3, FlowGraphGenerateCodeNode.inputPorts.size)
    }

    @Test
    fun `input port 1 is generationContext of type String`() {
        val port = FlowGraphGenerateCodeNode.inputPorts[0]
        assertEquals("generationContext", port.name)
        assertEquals(String::class, port.dataType)
    }

    @Test
    fun `input port 2 is nodeDescriptors of type String`() {
        val port = FlowGraphGenerateCodeNode.inputPorts[1]
        assertEquals("nodeDescriptors", port.name)
        assertEquals(String::class, port.dataType)
    }

    @Test
    fun `input port 3 is ipTypeMetadata of type String`() {
        val port = FlowGraphGenerateCodeNode.inputPorts[2]
        assertEquals("ipTypeMetadata", port.name)
        assertEquals(String::class, port.dataType)
    }

    @Test
    fun `has 1 output port`() {
        assertEquals(1, FlowGraphGenerateCodeNode.outputPorts.size)
    }

    @Test
    fun `output port is generatedOutput of type String`() {
        val port = FlowGraphGenerateCodeNode.outputPorts[0]
        assertEquals("generatedOutput", port.name)
        assertEquals(String::class, port.dataType)
    }

    // --- Runtime Creation Tests ---

    @Test
    fun `createRuntime returns In3AnyOut1Runtime`() {
        val runtime = FlowGraphGenerateCodeNode.createRuntime("test")
        assertIs<In3AnyOut1Runtime<String, String, String, String>>(runtime)
    }

    // --- Data Flow Tests ---

    private data class WiredRuntime(
        val runtime: In3AnyOut1Runtime<String, String, String, String>,
        val generationContext: Channel<String>,
        val nodeDescriptors: Channel<String>,
        val ipTypeMetadata: Channel<String>,
        val generatedOutput: Channel<String>
    )

    private fun createWiredRuntime(name: String = "test"): WiredRuntime {
        @Suppress("UNCHECKED_CAST")
        val runtime = FlowGraphGenerateCodeNode.createRuntime(name) as In3AnyOut1Runtime<String, String, String, String>

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
    fun `generationContext input triggers generatedOutput`() = runTest {
        val wired = createWiredRuntime("test-context-input")
        wired.runtime.start(this) {}

        wired.generationContext.send("""{"flowGraphModel":"{}","serializedOutput":"{}"}""")
        val result = wired.generatedOutput.receive()
        assertNotNull(result)
        assertTrue(result.isNotEmpty())

        wired.runtime.stop()
    }

    @Test
    fun `nodeDescriptors input triggers generatedOutput`() = runTest {
        val wired = createWiredRuntime("test-descriptors-input")
        wired.runtime.start(this) {}

        wired.nodeDescriptors.send("""[{"name":"TimeEmitter","category":"SOURCE"}]""")
        val result = wired.generatedOutput.receive()
        assertNotNull(result)

        wired.runtime.stop()
    }

    @Test
    fun `ipTypeMetadata input triggers generatedOutput`() = runTest {
        val wired = createWiredRuntime("test-metadata-input")
        wired.runtime.start(this) {}

        wired.ipTypeMetadata.send("""[{"id":"ip_time","typeName":"Time"}]""")
        val result = wired.generatedOutput.receive()
        assertNotNull(result)

        wired.runtime.stop()
    }

    @Test
    fun `output contains all three input values after receiving all inputs`() = runTest {
        val wired = createWiredRuntime("test-all-inputs")
        wired.runtime.start(this) {}

        val contextJson = """{"flowGraphModel":"model","serializedOutput":"serial"}"""
        val descriptorsJson = """[{"name":"Node1"}]"""
        val metadataJson = """[{"id":"ip_any"}]"""

        wired.generationContext.send(contextJson)
        wired.generatedOutput.receive() // consume first output

        wired.nodeDescriptors.send(descriptorsJson)
        wired.generatedOutput.receive() // consume second output

        wired.ipTypeMetadata.send(metadataJson)
        val result = wired.generatedOutput.receive()
        assertNotNull(result)
        assertTrue(result.contains("generationContext"), "Output should contain generationContext key")
        assertTrue(result.contains("nodeDescriptors"), "Output should contain nodeDescriptors key")
        assertTrue(result.contains("ipTypeMetadata"), "Output should contain ipTypeMetadata key")

        wired.runtime.stop()
    }

    @Test
    fun `empty generationContext still produces output`() = runTest {
        val wired = createWiredRuntime("test-empty")
        wired.runtime.start(this) {}

        wired.generationContext.send("")
        val result = wired.generatedOutput.receive()
        assertNotNull(result)

        wired.runtime.stop()
    }
}
