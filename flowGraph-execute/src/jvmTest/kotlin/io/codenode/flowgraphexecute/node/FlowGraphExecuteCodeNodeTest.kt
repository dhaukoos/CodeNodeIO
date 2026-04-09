/*
 * FlowGraphExecuteCodeNodeTest - TDD tests for the FlowGraphExecute CodeNode
 * Tests written BEFORE implementation per FR-008
 * License: Apache 2.0
 */

package io.codenode.flowgraphexecute.node

import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Port
import io.codenode.fbpdsl.runtime.In2AnyOut3Runtime
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
 * TDD tests for FlowGraphExecuteCodeNode.
 *
 * Verifies the CodeNode contract:
 * - 2 input ports (flowGraphModel, nodeDescriptors)
 * - 3 output ports (executionState, animations, debugSnapshots)
 * - anyInput mode (fires on ANY input change)
 * - Data flows correctly through channels
 * - Command processing produces output on correct ports
 * - Boundary conditions handled gracefully
 */
class FlowGraphExecuteCodeNodeTest {

    // --- T013: Port signature tests ---

    @Test
    fun `node has correct name`() {
        assertEquals("FlowGraphExecute", FlowGraphExecuteCodeNode.name)
    }

    @Test
    fun `node is a transformer category`() {
        assertEquals(CodeNodeType.TRANSFORMER, FlowGraphExecuteCodeNode.category)
    }

    @Test
    fun `node has exactly 2 input ports`() {
        assertEquals(2, FlowGraphExecuteCodeNode.inputPorts.size)
    }

    @Test
    fun `node has exactly 3 output ports`() {
        assertEquals(3, FlowGraphExecuteCodeNode.outputPorts.size)
    }

    @Test
    fun `input ports have correct names`() {
        val portNames = FlowGraphExecuteCodeNode.inputPorts.map { it.name }
        assertEquals(listOf("flowGraphModel", "nodeDescriptors"), portNames)
    }

    @Test
    fun `output ports have correct names`() {
        val portNames = FlowGraphExecuteCodeNode.outputPorts.map { it.name }
        assertEquals(listOf("executionState", "animations", "debugSnapshots"), portNames)
    }

    @Test
    fun `all ports use String data type`() {
        FlowGraphExecuteCodeNode.inputPorts.forEach { port ->
            assertEquals(String::class, port.dataType, "Input port '${port.name}' should use String type")
        }
        FlowGraphExecuteCodeNode.outputPorts.forEach { port ->
            assertEquals(String::class, port.dataType, "Output port '${port.name}' should use String type")
        }
    }

    @Test
    fun `node uses anyInput mode`() {
        assertTrue(FlowGraphExecuteCodeNode.anyInput, "FlowGraphExecuteCodeNode must use anyInput mode")
    }

    @Test
    fun `createRuntime returns In2AnyOut3Runtime`() {
        val runtime = FlowGraphExecuteCodeNode.createRuntime("test-node")
        assertIs<In2AnyOut3Runtime<*, *, *, *, *>>(runtime)
    }

    @Test
    fun `toNodeTypeDefinition produces correct palette entry`() {
        val ntd = FlowGraphExecuteCodeNode.toNodeTypeDefinition()
        assertEquals("FlowGraphExecute", ntd.name)
        assertEquals(CodeNodeType.TRANSFORMER, ntd.category)

        val inputTemplates = ntd.portTemplates.filter { it.direction == Port.Direction.INPUT }
        val outputTemplates = ntd.portTemplates.filter { it.direction == Port.Direction.OUTPUT }
        assertEquals(2, inputTemplates.size)
        assertEquals(3, outputTemplates.size)
    }

    // --- T014: Data flow tests ---

    private fun createWiredRuntime(name: String): WiredRuntime {
        val runtime = FlowGraphExecuteCodeNode.createRuntime(name) as In2AnyOut3Runtime<String, String, String, String, String>

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

        return WiredRuntime(runtime, inputChannel1, inputChannel2, outputChannel1, outputChannel2, outputChannel3)
    }

    private data class WiredRuntime(
        val runtime: In2AnyOut3Runtime<String, String, String, String, String>,
        val flowGraphModel: Channel<String>,
        val nodeDescriptors: Channel<String>,
        val executionState: Channel<String>,
        val animations: Channel<String>,
        val debugSnapshots: Channel<String>
    )

    @Test
    fun `flowGraphModel input triggers executionState output`() = runTest {
        val wired = createWiredRuntime("test-state")
        wired.runtime.start(this) {}

        // Send a configure command with a flow graph model
        wired.flowGraphModel.send("""{"action":"configure","flowGraph":{"name":"TestGraph","nodes":[],"connections":[]}}""")
        val result = wired.executionState.receive()
        assertTrue(result.isNotEmpty(), "executionState should emit state")
        assertTrue(result.contains("IDLE"), "Initial configuration should report IDLE state")

        wired.runtime.stop()
    }

    @Test
    fun `selective output - not every input produces all 3 outputs`() = runTest {
        val wired = createWiredRuntime("test-selective")
        wired.runtime.start(this) {}

        // Send a configure command — should produce executionState but not animations or debugSnapshots
        wired.flowGraphModel.send("""{"action":"configure","flowGraph":{"name":"TestGraph","nodes":[],"connections":[]}}""")
        val stateResult = wired.executionState.receive()
        assertNotNull(stateResult, "executionState should have output")

        val animResult = withTimeoutOrNull(500) { wired.animations.receive() }
        assertNull(animResult, "animations should not produce output on configure")

        val debugResult = withTimeoutOrNull(500) { wired.debugSnapshots.receive() }
        assertNull(debugResult, "debugSnapshots should not produce output on configure")

        wired.runtime.stop()
    }

    @Test
    fun `empty flowGraphModel returns no output`() = runTest {
        val wired = createWiredRuntime("test-empty-model")
        wired.runtime.start(this) {}

        // Send empty string — should be no-op
        wired.flowGraphModel.send("")
        val result = withTimeoutOrNull(500) { wired.executionState.receive() }
        assertNull(result, "Empty flowGraphModel should not produce output")

        wired.runtime.stop()
    }

    @Test
    fun `invalid flowGraphModel returns no output`() = runTest {
        val wired = createWiredRuntime("test-invalid-model")
        wired.runtime.start(this) {}

        // Send malformed JSON — should not crash, should produce no output
        wired.flowGraphModel.send("not valid json {{{")
        val result = withTimeoutOrNull(500) { wired.executionState.receive() }
        assertNull(result, "Malformed input should not produce output")

        wired.runtime.stop()
    }

    // --- T015: Boundary condition tests ---

    @Test
    fun `nodeDescriptors arriving alone caches value for later use`() = runTest {
        val wired = createWiredRuntime("test-cache-descriptors")
        wired.runtime.start(this) {}

        // Send nodeDescriptors alone — should be cached without crashing
        wired.nodeDescriptors.send("""[{"name":"TestNode","category":"TRANSFORMER"}]""")

        // No output expected on any port (just caching)
        val stateResult = withTimeoutOrNull(500) { wired.executionState.receive() }
        assertNull(stateResult, "nodeDescriptors alone should not produce executionState output")

        wired.runtime.stop()
    }

    @Test
    fun `toNodeTypeDefinition returns correct metadata`() {
        val ntd = FlowGraphExecuteCodeNode.toNodeTypeDefinition()
        assertEquals("FlowGraphExecute", ntd.name)
        assertEquals(CodeNodeType.TRANSFORMER, ntd.category)
        assertNotNull(ntd.description, "Description should not be null")
        assertTrue(ntd.description.isNotEmpty(), "Description should not be empty")

        // Verify port templates have correct names
        val inputNames = ntd.portTemplates
            .filter { it.direction == Port.Direction.INPUT }
            .map { it.name }
        val outputNames = ntd.portTemplates
            .filter { it.direction == Port.Direction.OUTPUT }
            .map { it.name }

        assertTrue(inputNames.contains("flowGraphModel"), "Should have flowGraphModel input template")
        assertTrue(inputNames.contains("nodeDescriptors"), "Should have nodeDescriptors input template")
        assertTrue(outputNames.contains("executionState"), "Should have executionState output template")
        assertTrue(outputNames.contains("animations"), "Should have animations output template")
        assertTrue(outputNames.contains("debugSnapshots"), "Should have debugSnapshots output template")
    }

    @Test
    fun `status command emits current execution state`() = runTest {
        val wired = createWiredRuntime("test-status")
        wired.runtime.start(this) {}

        // Send a status query command
        wired.flowGraphModel.send("""{"action":"status"}""")
        val result = wired.executionState.receive()
        assertTrue(result.contains("IDLE"), "Status should report IDLE when no pipeline is running")

        wired.runtime.stop()
    }
}
