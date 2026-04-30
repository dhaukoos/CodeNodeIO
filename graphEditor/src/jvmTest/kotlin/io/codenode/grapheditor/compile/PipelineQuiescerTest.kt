/*
 * PipelineQuiescerTest - TDD Red tests for foundational PipelineQuiescer behavior
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compile

import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.runtime.DynamicPipelineController
import io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PipelineQuiescerTest {

    /** Empty graph — sufficient for the controller's lifecycle methods. */
    private fun emptyGraph() = FlowGraph(
        id = "test_empty",
        name = "TestEmpty",
        version = "1.0.0",
        rootNodes = emptyList(),
        connections = emptyList()
    )

    private fun makeController(): DynamicPipelineController {
        val registry = NodeDefinitionRegistry()
        return DynamicPipelineController(
            flowGraphProvider = { emptyGraph() },
            lookup = { name -> registry.getByName(name) },
            onReset = null
        )
    }

    @Test
    fun `stopAll returns 0 when no controllers registered`() {
        val q = PipelineQuiescer()
        assertEquals(0, q.stopAll())
    }

    @Test
    fun `stopAll returns count of registered controllers`() {
        val q = PipelineQuiescer()
        q.register(makeController())
        q.register(makeController())
        assertEquals(2, q.stopAll())
    }

    @Test
    fun `register is idempotent`() {
        val q = PipelineQuiescer()
        val controller = makeController()
        q.register(controller)
        q.register(controller)
        assertEquals(1, q.stopAll(), "registering the same controller twice must not double-count")
    }

    @Test
    fun `unregister removes a previously registered controller`() {
        val q = PipelineQuiescer()
        val controller = makeController()
        q.register(controller)
        q.unregister(controller)
        assertEquals(0, q.stopAll())
    }

    @Test
    fun `unregister of unknown controller is a no-op`() {
        val q = PipelineQuiescer()
        q.unregister(makeController()) // must not throw
        assertEquals(0, q.stopAll())
    }

    @Test
    fun `stopAll invokes stop on each registered controller`() {
        val q = PipelineQuiescer()
        val a = makeController()
        val b = makeController()
        q.register(a)
        q.register(b)
        q.stopAll()
        // After stopAll the controllers' executionState is IDLE — verifies stop() ran.
        assertTrue(
            a.executionState.value == io.codenode.fbpdsl.model.ExecutionState.IDLE &&
                b.executionState.value == io.codenode.fbpdsl.model.ExecutionState.IDLE,
            "stopAll must call stop() on every registered controller"
        )
    }
}
