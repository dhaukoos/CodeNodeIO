/*
 * RuntimeRegistrationTest - Integration tests for runtime registration flow
 * Tests that NodeRuntime auto-registers/unregisters with RuntimeRegistry
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.ControlConfig
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.RootControlNode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RuntimeRegistrationTest {

    private fun createTestCodeNode(
        id: String,
        independentControl: Boolean = false
    ): CodeNode {
        return CodeNode(
            id = id,
            name = "Test Node $id",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position.ORIGIN,
            controlConfig = ControlConfig(independentControl = independentControl)
        )
    }

    private fun createTestRuntime(
        id: String,
        registry: RuntimeRegistry? = null,
        independentControl: Boolean = false
    ): NodeRuntime<String> {
        return NodeRuntime(createTestCodeNode(id, independentControl), registry)
    }

    // ========== Auto-Registration Tests ==========

    @Test
    fun start_registersRuntimeWithRegistry() = runTest {
        val registry = RuntimeRegistry()
        val runtime = createTestRuntime("node1", registry)

        assertEquals(0, registry.count)
        assertFalse(registry.isRegistered("node1"))

        // Registration happens before processing block runs
        runtime.start(this) { }
        advanceUntilIdle()

        // Should be registered after start
        assertEquals(1, registry.count)
        assertTrue(registry.isRegistered("node1"))

        runtime.stop()
    }

    @Test
    fun stop_unregistersRuntimeFromRegistry() = runTest {
        val registry = RuntimeRegistry()
        val runtime = createTestRuntime("node1", registry)

        runtime.start(this) { }
        advanceUntilIdle()

        assertTrue(registry.isRegistered("node1"))

        runtime.stop()

        // Should be unregistered after stop
        assertEquals(0, registry.count)
        assertFalse(registry.isRegistered("node1"))
    }

    @Test
    fun multipleRuntimes_allRegisterAndUnregister() = runTest {
        val registry = RuntimeRegistry()
        val runtime1 = createTestRuntime("node1", registry)
        val runtime2 = createTestRuntime("node2", registry)
        val runtime3 = createTestRuntime("node3", registry)

        // Start all with empty processing blocks - the job completes immediately
        // but registration happens before the block runs
        runtime1.start(this) { }
        runtime2.start(this) { }
        runtime3.start(this) { }
        advanceUntilIdle()

        // All should be registered
        assertEquals(3, registry.count)
        assertTrue(registry.isRegistered("node1"))
        assertTrue(registry.isRegistered("node2"))
        assertTrue(registry.isRegistered("node3"))

        // Stop in different order
        runtime2.stop()
        assertEquals(2, registry.count)
        assertFalse(registry.isRegistered("node2"))

        runtime1.stop()
        assertEquals(1, registry.count)

        runtime3.stop()
        assertEquals(0, registry.count)
    }

    @Test
    fun start_withoutRegistry_noError() = runTest {
        // Runtime without registry should work fine
        val runtime = createTestRuntime("node1", registry = null)

        runtime.start(this) { }
        advanceUntilIdle()

        // State should still be RUNNING (processing block completed but state persists)
        // Actually, the block completes immediately so we check it was RUNNING
        // For this test, we just verify no exceptions occur
        runtime.stop()
        assertTrue(runtime.isIdle())
    }

    // ========== RootControlNode Integration Tests ==========

    @Test
    fun rootControlNode_pauseAll_pausesRegisteredRuntimes() = runTest {
        val registry = RuntimeRegistry()
        val flowGraph = FlowGraph(
            id = "test-flow",
            name = "Test Flow",
            version = "1.0.0",
            rootNodes = listOf(createTestCodeNode("node1"), createTestCodeNode("node2"))
        )
        val controller = RootControlNode.createFor(flowGraph, "TestController", registry)

        val runtime1 = createTestRuntime("node1", registry)
        val runtime2 = createTestRuntime("node2", registry)

        // Start runtimes - empty processing blocks, state changes happen immediately
        runtime1.start(this) { }
        runtime2.start(this) { }
        advanceUntilIdle()

        assertTrue(runtime1.isRunning())
        assertTrue(runtime2.isRunning())

        // Pause through controller
        controller.pauseAll()

        // Runtimes should be paused
        assertTrue(runtime1.isPaused())
        assertTrue(runtime2.isPaused())

        // Cleanup
        runtime1.stop()
        runtime2.stop()
    }

    @Test
    fun rootControlNode_resumeAll_resumesRegisteredRuntimes() = runTest {
        val registry = RuntimeRegistry()
        val flowGraph = FlowGraph(
            id = "test-flow",
            name = "Test Flow",
            version = "1.0.0",
            rootNodes = listOf(createTestCodeNode("node1"))
        )
        val controller = RootControlNode.createFor(flowGraph, "TestController", registry)

        val runtime = createTestRuntime("node1", registry)

        runtime.start(this) { }
        advanceUntilIdle()

        // Pause first
        controller.pauseAll()
        assertTrue(runtime.isPaused())

        // Resume
        controller.resumeAll()
        assertTrue(runtime.isRunning())

        // Cleanup
        runtime.stop()
    }

    @Test
    fun rootControlNode_stopAll_stopsRegisteredRuntimes() = runTest {
        val registry = RuntimeRegistry()
        val flowGraph = FlowGraph(
            id = "test-flow",
            name = "Test Flow",
            version = "1.0.0",
            rootNodes = listOf(createTestCodeNode("node1"), createTestCodeNode("node2"))
        )
        val controller = RootControlNode.createFor(flowGraph, "TestController", registry)

        val runtime1 = createTestRuntime("node1", registry)
        val runtime2 = createTestRuntime("node2", registry)

        // Start runtimes with empty processing blocks
        runtime1.start(this) { }
        runtime2.start(this) { }
        advanceUntilIdle()

        assertEquals(2, registry.count)

        // Stop through controller
        controller.stopAll()

        // Runtimes should be stopped and unregistered
        assertTrue(runtime1.isIdle())
        assertTrue(runtime2.isIdle())
        assertEquals(0, registry.count)
    }

    @Test
    fun rootControlNode_withoutRegistry_stillUpdateModelState() {
        // Controller without registry should still update model state
        val flowGraph = FlowGraph(
            id = "test-flow",
            name = "Test Flow",
            version = "1.0.0",
            rootNodes = listOf(createTestCodeNode("node1"))
        )
        val controller = RootControlNode.createFor(flowGraph, "TestController", registry = null)

        // These should work without registry
        val pausedGraph = controller.pauseAll()
        assertEquals(ExecutionState.PAUSED, pausedGraph.rootNodes.first().executionState)

        // Create new controller with updated graph for resumeAll
        val pausedController = RootControlNode.createFor(pausedGraph, "TestController", registry = null)
        val resumedGraph = pausedController.resumeAll()
        assertEquals(ExecutionState.RUNNING, resumedGraph.rootNodes.first().executionState)
    }

    // ========== IndependentControl Tests ==========

    @Test
    fun pauseAll_skipsIndependentControlRuntimes() = runTest {
        val registry = RuntimeRegistry()
        val flowGraph = FlowGraph(
            id = "test-flow",
            name = "Test Flow",
            version = "1.0.0",
            rootNodes = listOf(
                createTestCodeNode("normal", independentControl = false),
                createTestCodeNode("independent", independentControl = true)
            )
        )
        val controller = RootControlNode.createFor(flowGraph, "TestController", registry)

        val normalRuntime = createTestRuntime("normal", registry, independentControl = false)
        val independentRuntime = createTestRuntime("independent", registry, independentControl = true)

        // Start both with empty processing blocks
        normalRuntime.start(this) { }
        independentRuntime.start(this) { }
        advanceUntilIdle()

        assertTrue(normalRuntime.isRunning())
        assertTrue(independentRuntime.isRunning())

        // Pause through controller
        controller.pauseAll()

        // Normal runtime should be paused
        assertTrue(normalRuntime.isPaused())
        // Independent runtime should still be running
        assertTrue(independentRuntime.isRunning())

        // Cleanup
        normalRuntime.stop()
        independentRuntime.stop()
    }

    @Test
    fun resumeAll_skipsIndependentControlRuntimes() = runTest {
        val registry = RuntimeRegistry()
        val flowGraph = FlowGraph(
            id = "test-flow",
            name = "Test Flow",
            version = "1.0.0",
            rootNodes = listOf(
                createTestCodeNode("normal", independentControl = false),
                createTestCodeNode("independent", independentControl = true)
            )
        )
        val controller = RootControlNode.createFor(flowGraph, "TestController", registry)

        val normalRuntime = createTestRuntime("normal", registry, independentControl = false)
        val independentRuntime = createTestRuntime("independent", registry, independentControl = true)

        // Start both with empty processing blocks
        normalRuntime.start(this) { }
        independentRuntime.start(this) { }
        advanceUntilIdle()

        // Manually pause both (simulating they were paused independently)
        normalRuntime.pause()
        independentRuntime.pause()

        assertTrue(normalRuntime.isPaused())
        assertTrue(independentRuntime.isPaused())

        // Resume through controller
        controller.resumeAll()

        // Normal runtime should be resumed
        assertTrue(normalRuntime.isRunning())
        // Independent runtime should still be paused (not affected by controller)
        assertTrue(independentRuntime.isPaused())

        // Cleanup
        normalRuntime.stop()
        independentRuntime.stop()
    }

    // ========== Restart Cycle Tests ==========

    @Test
    fun restartCycle_reregistersRuntime() = runTest {
        val registry = RuntimeRegistry()
        val runtime = createTestRuntime("node1", registry)

        // First start
        runtime.start(this) { }
        advanceUntilIdle()
        assertTrue(registry.isRegistered("node1"))

        // Stop
        runtime.stop()
        assertFalse(registry.isRegistered("node1"))

        // Restart
        runtime.start(this) { }
        advanceUntilIdle()
        assertTrue(registry.isRegistered("node1"))

        // Cleanup
        runtime.stop()
    }
}
