/*
 * FlowGraphTypesCodeNodeTest - TDD tests for the FlowGraphTypes CodeNode
 * Tests written BEFORE implementation per US4
 * License: Apache 2.0
 */

package io.codenode.flowgraphtypes.node

import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Port
import io.codenode.fbpdsl.runtime.In3AnyOut1Runtime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * TDD tests for FlowGraphTypesCodeNode.
 *
 * Verifies the CodeNode contract:
 * - 3 input ports (filesystemPaths, classpathEntries, ipTypeCommands)
 * - 1 output port (ipTypeMetadata)
 * - anyInput mode (fires on ANY input change)
 * - Data flows correctly through channels
 * - Mutation commands produce updated output
 * - Boundary conditions handled gracefully
 */
class FlowGraphTypesCodeNodeTest {

    // --- T020: Port signature tests ---

    @Test
    fun `node has correct name`() {
        assertEquals("FlowGraphTypes", FlowGraphTypesCodeNode.name)
    }

    @Test
    fun `node is a transformer category`() {
        assertEquals(CodeNodeType.TRANSFORMER, FlowGraphTypesCodeNode.category)
    }

    @Test
    fun `node has exactly 3 input ports`() {
        assertEquals(3, FlowGraphTypesCodeNode.inputPorts.size)
    }

    @Test
    fun `node has exactly 1 output port`() {
        assertEquals(1, FlowGraphTypesCodeNode.outputPorts.size)
    }

    @Test
    fun `input ports have correct names`() {
        val portNames = FlowGraphTypesCodeNode.inputPorts.map { it.name }
        assertEquals(listOf("filesystemPaths", "classpathEntries", "ipTypeCommands"), portNames)
    }

    @Test
    fun `output port has correct name`() {
        assertEquals("ipTypeMetadata", FlowGraphTypesCodeNode.outputPorts.first().name)
    }

    @Test
    fun `all ports use String data type`() {
        FlowGraphTypesCodeNode.inputPorts.forEach { port ->
            assertEquals(String::class, port.dataType, "Input port '${port.name}' should use String type")
        }
        FlowGraphTypesCodeNode.outputPorts.forEach { port ->
            assertEquals(String::class, port.dataType, "Output port '${port.name}' should use String type")
        }
    }

    // --- T021: anyInput mode test ---

    @Test
    fun `node uses anyInput mode`() {
        assertTrue(FlowGraphTypesCodeNode.anyInput, "FlowGraphTypesCodeNode must use anyInput mode")
    }

    @Test
    fun `createRuntime returns In3AnyOut1Runtime`() {
        val runtime = FlowGraphTypesCodeNode.createRuntime("test-node")
        assertIs<In3AnyOut1Runtime<*, *, *, *>>(runtime)
    }

    // --- T022: Data flow through channels ---

    @Test
    fun `providing filesystem paths produces metadata output`() = runTest {
        val runtime = FlowGraphTypesCodeNode.createRuntime("test-flow") as In3AnyOut1Runtime<String, String, String, String>

        val inputChannel1 = Channel<String>(Channel.BUFFERED)
        val inputChannel2 = Channel<String>(Channel.BUFFERED)
        val inputChannel3 = Channel<String>(Channel.BUFFERED)
        val outputChannel = Channel<String>(Channel.BUFFERED)

        runtime.inputChannel1 = inputChannel1
        runtime.inputChannel2 = inputChannel2
        runtime.inputChannel3 = inputChannel3
        runtime.outputChannel = outputChannel

        runtime.start(this) {}

        // Send filesystem paths — should trigger metadata output
        inputChannel1.send("/tmp/test-project")
        val result = outputChannel.receive()
        assertTrue(result.isNotEmpty(), "Output should contain metadata (non-empty string)")

        runtime.stop()
    }

    @Test
    fun `providing classpath entries produces metadata output`() = runTest {
        val runtime = FlowGraphTypesCodeNode.createRuntime("test-classpath") as In3AnyOut1Runtime<String, String, String, String>

        val inputChannel1 = Channel<String>(Channel.BUFFERED)
        val inputChannel2 = Channel<String>(Channel.BUFFERED)
        val inputChannel3 = Channel<String>(Channel.BUFFERED)
        val outputChannel = Channel<String>(Channel.BUFFERED)

        runtime.inputChannel1 = inputChannel1
        runtime.inputChannel2 = inputChannel2
        runtime.inputChannel3 = inputChannel3
        runtime.outputChannel = outputChannel

        runtime.start(this) {}

        // Send classpath entries — should trigger metadata output
        inputChannel2.send("/tmp/classes")
        val result = outputChannel.receive()
        assertTrue(result.isNotEmpty(), "Output should contain metadata (non-empty string)")

        runtime.stop()
    }

    // --- T023: Mutation commands ---

    @Test
    fun `mutation command through ipTypeCommands produces updated output`() = runTest {
        val runtime = FlowGraphTypesCodeNode.createRuntime("test-commands") as In3AnyOut1Runtime<String, String, String, String>

        val inputChannel1 = Channel<String>(Channel.BUFFERED)
        val inputChannel2 = Channel<String>(Channel.BUFFERED)
        val inputChannel3 = Channel<String>(Channel.BUFFERED)
        val outputChannel = Channel<String>(Channel.BUFFERED)

        runtime.inputChannel1 = inputChannel1
        runtime.inputChannel2 = inputChannel2
        runtime.inputChannel3 = inputChannel3
        runtime.outputChannel = outputChannel

        runtime.start(this) {}

        // Send a register command
        inputChannel3.send("""{"command":"register","typeName":"TestType","typeId":"ip_testtype"}""")
        val result = outputChannel.receive()
        assertTrue(result.contains("TestType"), "Output should contain the registered type name")

        runtime.stop()
    }

    @Test
    fun `unregister command removes type from metadata`() = runTest {
        val runtime = FlowGraphTypesCodeNode.createRuntime("test-unregister") as In3AnyOut1Runtime<String, String, String, String>

        val inputChannel1 = Channel<String>(Channel.BUFFERED)
        val inputChannel2 = Channel<String>(Channel.BUFFERED)
        val inputChannel3 = Channel<String>(Channel.BUFFERED)
        val outputChannel = Channel<String>(Channel.BUFFERED)

        runtime.inputChannel1 = inputChannel1
        runtime.inputChannel2 = inputChannel2
        runtime.inputChannel3 = inputChannel3
        runtime.outputChannel = outputChannel

        runtime.start(this) {}

        // Register a type first
        inputChannel3.send("""{"command":"register","typeName":"ToRemove","typeId":"ip_toremove"}""")
        val afterRegister = outputChannel.receive()
        assertTrue(afterRegister.contains("ToRemove"), "Type should be registered")

        // Unregister it
        inputChannel3.send("""{"command":"unregister","typeId":"ip_toremove"}""")
        val afterUnregister = outputChannel.receive()
        assertTrue(!afterUnregister.contains("ip_toremove"), "Type should be removed from metadata")

        runtime.stop()
    }

    // --- T024: Boundary conditions ---

    @Test
    fun `empty filesystem paths handled gracefully`() = runTest {
        val runtime = FlowGraphTypesCodeNode.createRuntime("test-empty-paths") as In3AnyOut1Runtime<String, String, String, String>

        val inputChannel1 = Channel<String>(Channel.BUFFERED)
        val inputChannel2 = Channel<String>(Channel.BUFFERED)
        val inputChannel3 = Channel<String>(Channel.BUFFERED)
        val outputChannel = Channel<String>(Channel.BUFFERED)

        runtime.inputChannel1 = inputChannel1
        runtime.inputChannel2 = inputChannel2
        runtime.inputChannel3 = inputChannel3
        runtime.outputChannel = outputChannel

        runtime.start(this) {}

        // Send empty string — should still produce valid output without crashing
        inputChannel1.send("")
        val result = outputChannel.receive()
        assertTrue(result.isNotEmpty(), "Should produce valid metadata even with empty paths")

        runtime.stop()
    }

    @Test
    fun `malformed command handled gracefully`() = runTest {
        val runtime = FlowGraphTypesCodeNode.createRuntime("test-malformed") as In3AnyOut1Runtime<String, String, String, String>

        val inputChannel1 = Channel<String>(Channel.BUFFERED)
        val inputChannel2 = Channel<String>(Channel.BUFFERED)
        val inputChannel3 = Channel<String>(Channel.BUFFERED)
        val outputChannel = Channel<String>(Channel.BUFFERED)

        runtime.inputChannel1 = inputChannel1
        runtime.inputChannel2 = inputChannel2
        runtime.inputChannel3 = inputChannel3
        runtime.outputChannel = outputChannel

        runtime.start(this) {}

        // Send malformed JSON command — should not crash, should still produce output
        inputChannel3.send("not valid json {{{")
        val result = outputChannel.receive()
        assertTrue(result.isNotEmpty(), "Should produce valid metadata even with malformed command")

        runtime.stop()
    }

    @Test
    fun `toNodeTypeDefinition produces correct palette entry`() {
        val ntd = FlowGraphTypesCodeNode.toNodeTypeDefinition()
        assertEquals("FlowGraphTypes", ntd.name)
        assertEquals(CodeNodeType.TRANSFORMER, ntd.category)

        val inputTemplates = ntd.portTemplates.filter { it.direction == Port.Direction.INPUT }
        val outputTemplates = ntd.portTemplates.filter { it.direction == Port.Direction.OUTPUT }
        assertEquals(3, inputTemplates.size)
        assertEquals(1, outputTemplates.size)
    }
}
