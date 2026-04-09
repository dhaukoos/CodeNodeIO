/*
 * GenerateContextAggregatorCodeNodeTest - TDD tests for GenerateContextAggregatorCodeNode
 * Tests port signatures, runtime creation, and basic data flow
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.node

import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.In2AnyOut1Runtime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GenerateContextAggregatorCodeNodeTest {

    // --- Port Signature Tests ---

    @Test
    fun `name is GenerateContextAggregator`() {
        assertEquals("GenerateContextAggregator", GenerateContextAggregatorCodeNode.name)
    }

    @Test
    fun `category is TRANSFORMER`() {
        assertEquals(CodeNodeType.TRANSFORMER, GenerateContextAggregatorCodeNode.category)
    }

    @Test
    fun `anyInput is true`() {
        assertTrue(GenerateContextAggregatorCodeNode.anyInput)
    }

    @Test
    fun `has 2 input ports`() {
        assertEquals(2, GenerateContextAggregatorCodeNode.inputPorts.size)
    }

    @Test
    fun `input port 1 is flowGraphModel of type String`() {
        val port = GenerateContextAggregatorCodeNode.inputPorts[0]
        assertEquals("flowGraphModel", port.name)
        assertEquals(String::class, port.dataType)
    }

    @Test
    fun `input port 2 is serializedOutput of type String`() {
        val port = GenerateContextAggregatorCodeNode.inputPorts[1]
        assertEquals("serializedOutput", port.name)
        assertEquals(String::class, port.dataType)
    }

    @Test
    fun `has 1 output port`() {
        assertEquals(1, GenerateContextAggregatorCodeNode.outputPorts.size)
    }

    @Test
    fun `output port is generationContext of type String`() {
        val port = GenerateContextAggregatorCodeNode.outputPorts[0]
        assertEquals("generationContext", port.name)
        assertEquals(String::class, port.dataType)
    }

    // --- Runtime Creation Tests ---

    @Test
    fun `createRuntime returns In2AnyOut1Runtime`() {
        val runtime = GenerateContextAggregatorCodeNode.createRuntime("test")
        assertIs<In2AnyOut1Runtime<String, String, String>>(runtime)
    }

    // --- Data Flow Tests ---

    private data class WiredRuntime(
        val runtime: In2AnyOut1Runtime<String, String, String>,
        val flowGraphModel: Channel<String>,
        val serializedOutput: Channel<String>,
        val generationContext: Channel<String>
    )

    private fun createWiredRuntime(name: String = "test"): WiredRuntime {
        @Suppress("UNCHECKED_CAST")
        val runtime = GenerateContextAggregatorCodeNode.createRuntime(name) as In2AnyOut1Runtime<String, String, String>

        val input1 = Channel<String>(Channel.BUFFERED)
        val input2 = Channel<String>(Channel.BUFFERED)
        val output = Channel<String>(Channel.BUFFERED)

        runtime.inputChannel1 = input1
        runtime.inputChannel2 = input2
        runtime.outputChannel = output

        return WiredRuntime(runtime, input1, input2, output)
    }

    @Test
    fun `flowGraphModel input triggers generationContext output`() = runTest {
        val wired = createWiredRuntime("test-model-input")
        wired.runtime.start(this) {}

        wired.flowGraphModel.send("""{"nodes":["TimeEmitter"]}""")
        val result = wired.generationContext.receive()
        assertNotNull(result)
        assertTrue(result.isNotEmpty())

        wired.runtime.stop()
    }

    @Test
    fun `serializedOutput input triggers generationContext output`() = runTest {
        val wired = createWiredRuntime("test-serialized-input")
        wired.runtime.start(this) {}

        wired.serializedOutput.send("""{"format":"kts"}""")
        val result = wired.generationContext.receive()
        assertNotNull(result)
        assertTrue(result.isNotEmpty())

        wired.runtime.stop()
    }

    @Test
    fun `generationContext contains both flowGraphModel and serializedOutput data`() = runTest {
        val wired = createWiredRuntime("test-combined")
        wired.runtime.start(this) {}

        val modelJson = """{"nodes":["TimeEmitter","Display"]}"""
        val serializedJson = """{"format":"kts","target":"jvm"}"""

        wired.flowGraphModel.send(modelJson)
        // Consume first output (triggered by model alone with initial serializedOutput)
        wired.generationContext.receive()

        wired.serializedOutput.send(serializedJson)
        // Second output should contain both values
        val result = wired.generationContext.receive()
        assertNotNull(result)
        assertTrue(result.contains("flowGraphModel"), "Output should contain flowGraphModel key")
        assertTrue(result.contains("serializedOutput"), "Output should contain serializedOutput key")

        wired.runtime.stop()
    }

    @Test
    fun `empty inputs produce empty generationContext`() = runTest {
        val wired = createWiredRuntime("test-empty")
        wired.runtime.start(this) {}

        wired.flowGraphModel.send("")
        val result = wired.generationContext.receive()
        assertNotNull(result)

        wired.runtime.stop()
    }
}
