/*
 * FlowGraphInspectCodeNodeTest - TDD tests for the FlowGraphInspect CodeNode
 * Tests written BEFORE implementation per FR-007
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.node

import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Port
import io.codenode.fbpdsl.runtime.In2AnyOut1Runtime
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
 * TDD tests for FlowGraphInspectCodeNode.
 *
 * Verifies the CodeNode contract:
 * - 2 input ports (filesystemPaths, classpathEntries)
 * - 1 output port (nodeDescriptors)
 * - anyInput mode (fires on ANY input change)
 * - Data flows correctly through channels
 * - Boundary conditions handled gracefully
 */
class FlowGraphInspectCodeNodeTest {

    // --- T014: Port signature tests ---

    @Test
    fun `node has correct name`() {
        assertEquals("FlowGraphInspect", FlowGraphInspectCodeNode.name)
    }

    @Test
    fun `node is a transformer category`() {
        assertEquals(CodeNodeType.TRANSFORMER, FlowGraphInspectCodeNode.category)
    }

    @Test
    fun `node has exactly 2 input ports`() {
        assertEquals(2, FlowGraphInspectCodeNode.inputPorts.size)
    }

    @Test
    fun `node has exactly 1 output port`() {
        assertEquals(1, FlowGraphInspectCodeNode.outputPorts.size)
    }

    @Test
    fun `input ports have correct names`() {
        val portNames = FlowGraphInspectCodeNode.inputPorts.map { it.name }
        assertEquals(listOf("filesystemPaths", "classpathEntries"), portNames)
    }

    @Test
    fun `output port has correct name`() {
        val portNames = FlowGraphInspectCodeNode.outputPorts.map { it.name }
        assertEquals(listOf("nodeDescriptors"), portNames)
    }

    @Test
    fun `all ports use String data type`() {
        FlowGraphInspectCodeNode.inputPorts.forEach { port ->
            assertEquals(String::class, port.dataType, "Input port '${port.name}' should use String type")
        }
        FlowGraphInspectCodeNode.outputPorts.forEach { port ->
            assertEquals(String::class, port.dataType, "Output port '${port.name}' should use String type")
        }
    }

    @Test
    fun `node uses anyInput mode`() {
        assertTrue(FlowGraphInspectCodeNode.anyInput, "FlowGraphInspectCodeNode must use anyInput mode")
    }

    @Test
    fun `createRuntime returns In2AnyOut1Runtime`() {
        val runtime = FlowGraphInspectCodeNode.createRuntime("test-node")
        assertIs<In2AnyOut1Runtime<*, *, *>>(runtime)
    }

    @Test
    fun `toNodeTypeDefinition produces correct palette entry`() {
        val ntd = FlowGraphInspectCodeNode.toNodeTypeDefinition()
        assertEquals("FlowGraphInspect", ntd.name)
        assertEquals(CodeNodeType.TRANSFORMER, ntd.category)

        val inputTemplates = ntd.portTemplates.filter { it.direction == Port.Direction.INPUT }
        val outputTemplates = ntd.portTemplates.filter { it.direction == Port.Direction.OUTPUT }
        assertEquals(2, inputTemplates.size)
        assertEquals(1, outputTemplates.size)
    }

    // --- T015: Data flow tests ---

    @Test
    fun `filesystemPaths input triggers node discovery and emits nodeDescriptors`() = runTest {
        val runtime = FlowGraphInspectCodeNode.createRuntime("test-fs") as In2AnyOut1Runtime<String, String, String>

        val inputChannel1 = Channel<String>(Channel.BUFFERED)
        val inputChannel2 = Channel<String>(Channel.BUFFERED)
        val outputChannel = Channel<String>(Channel.BUFFERED)

        runtime.inputChannel1 = inputChannel1
        runtime.inputChannel2 = inputChannel2
        runtime.outputChannel = outputChannel

        runtime.start(this) {}

        // Send a filesystem path — should trigger discovery and emit JSON nodeDescriptors
        inputChannel1.send("/tmp/nonexistent-test-dir")
        val result = withTimeoutOrNull(2000) { outputChannel.receive() }
        assertNotNull(result, "filesystemPaths input should produce nodeDescriptors output")
        assertTrue(result.contains("nodes"), "nodeDescriptors should contain 'nodes' key")

        runtime.stop()
    }

    @Test
    fun `classpathEntries input triggers compiled node discovery and emits nodeDescriptors`() = runTest {
        val runtime = FlowGraphInspectCodeNode.createRuntime("test-cp") as In2AnyOut1Runtime<String, String, String>

        val inputChannel1 = Channel<String>(Channel.BUFFERED)
        val inputChannel2 = Channel<String>(Channel.BUFFERED)
        val outputChannel = Channel<String>(Channel.BUFFERED)

        runtime.inputChannel1 = inputChannel1
        runtime.inputChannel2 = inputChannel2
        runtime.outputChannel = outputChannel

        runtime.start(this) {}

        // Send classpath entries — should trigger compiled node discovery
        inputChannel2.send("io.codenode.flowgraphinspect")
        val result = withTimeoutOrNull(2000) { outputChannel.receive() }
        assertNotNull(result, "classpathEntries input should produce nodeDescriptors output")
        assertTrue(result.contains("nodes"), "nodeDescriptors should contain 'nodes' key")

        runtime.stop()
    }

    @Test
    fun `either input independently triggers processing using cached value from other input`() = runTest {
        val runtime = FlowGraphInspectCodeNode.createRuntime("test-cache") as In2AnyOut1Runtime<String, String, String>

        val inputChannel1 = Channel<String>(Channel.BUFFERED)
        val inputChannel2 = Channel<String>(Channel.BUFFERED)
        val outputChannel = Channel<String>(Channel.BUFFERED)

        runtime.inputChannel1 = inputChannel1
        runtime.inputChannel2 = inputChannel2
        runtime.outputChannel = outputChannel

        runtime.start(this) {}

        // Send first input — should trigger with cached (empty) second input
        inputChannel1.send("/tmp/test-path-1")
        val result1 = withTimeoutOrNull(2000) { outputChannel.receive() }
        assertNotNull(result1, "First input alone should trigger processing")

        // Send second input — should trigger with cached first input
        inputChannel2.send("io.codenode.test")
        val result2 = withTimeoutOrNull(2000) { outputChannel.receive() }
        assertNotNull(result2, "Second input alone should trigger processing with cached first input")

        runtime.stop()
    }

    // --- T016: Boundary condition tests ---

    @Test
    fun `empty filesystemPaths input emits empty node list`() = runTest {
        val runtime = FlowGraphInspectCodeNode.createRuntime("test-empty-fs") as In2AnyOut1Runtime<String, String, String>

        val inputChannel1 = Channel<String>(Channel.BUFFERED)
        val inputChannel2 = Channel<String>(Channel.BUFFERED)
        val outputChannel = Channel<String>(Channel.BUFFERED)

        runtime.inputChannel1 = inputChannel1
        runtime.inputChannel2 = inputChannel2
        runtime.outputChannel = outputChannel

        runtime.start(this) {}

        // Send empty string — should produce output with empty nodes list
        inputChannel1.send("")
        val result = withTimeoutOrNull(2000) { outputChannel.receive() }
        assertNotNull(result, "Empty input should still produce output")
        assertTrue(result.contains("\"nodes\":[]") || result.contains("\"nodes\": []"),
            "Empty input should produce empty nodes list, got: $result")

        runtime.stop()
    }

    @Test
    fun `directory that does not exist returns empty node list`() = runTest {
        val runtime = FlowGraphInspectCodeNode.createRuntime("test-missing-dir") as In2AnyOut1Runtime<String, String, String>

        val inputChannel1 = Channel<String>(Channel.BUFFERED)
        val inputChannel2 = Channel<String>(Channel.BUFFERED)
        val outputChannel = Channel<String>(Channel.BUFFERED)

        runtime.inputChannel1 = inputChannel1
        runtime.inputChannel2 = inputChannel2
        runtime.outputChannel = outputChannel

        runtime.start(this) {}

        // Send path to non-existent directory
        inputChannel1.send("/tmp/absolutely-does-not-exist-codenode-test-12345")
        val result = withTimeoutOrNull(2000) { outputChannel.receive() }
        assertNotNull(result, "Non-existent directory should still produce output")
        assertTrue(result.contains("\"nodes\":[]") || result.contains("\"nodes\": []"),
            "Non-existent directory should produce empty nodes list, got: $result")

        runtime.stop()
    }

    @Test
    fun `no CodeNode definitions found returns empty nodeDescriptors`() = runTest {
        val runtime = FlowGraphInspectCodeNode.createRuntime("test-no-nodes") as In2AnyOut1Runtime<String, String, String>

        val inputChannel1 = Channel<String>(Channel.BUFFERED)
        val inputChannel2 = Channel<String>(Channel.BUFFERED)
        val outputChannel = Channel<String>(Channel.BUFFERED)

        runtime.inputChannel1 = inputChannel1
        runtime.inputChannel2 = inputChannel2
        runtime.outputChannel = outputChannel

        runtime.start(this) {}

        // Create a temp directory with no .kt files
        val tempDir = java.io.File.createTempFile("codenode-test", "").also {
            it.delete()
            it.mkdirs()
        }
        try {
            inputChannel1.send(tempDir.absolutePath)
            val result = withTimeoutOrNull(2000) { outputChannel.receive() }
            assertNotNull(result, "Empty directory should still produce output")
            assertTrue(result.contains("\"nodes\":[]") || result.contains("\"nodes\": []"),
                "Empty directory should produce empty nodes list, got: $result")
        } finally {
            tempDir.deleteRecursively()
        }

        runtime.stop()
    }
}
