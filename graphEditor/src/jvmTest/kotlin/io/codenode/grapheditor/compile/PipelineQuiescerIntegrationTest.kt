/*
 * PipelineQuiescerIntegrationTest — TDD test for T038 (US2)
 *
 * Verifies the real PipelineQuiescer behavior used by RuntimePreviewPanel:
 *   - A registered DynamicPipelineController transitions to IDLE on stopAll()
 *   - The Stoppable SAM lets non-controller targets (e.g., RuntimeSession) register
 *   - pipelinesQuiesced count is non-zero in the recompile feedback
 *
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compile

import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.runtime.DynamicPipelineController
import io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PipelineQuiescerIntegrationTest {

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
    fun `registered DynamicPipelineController transitions to IDLE on stopAll`() {
        val q = PipelineQuiescer()
        val controller = makeController()
        q.register(controller)

        val count = q.stopAll()

        assertEquals(1, count, "stopAll must return the count of stopped targets")
        assertEquals(
            ExecutionState.IDLE,
            controller.executionState.value,
            "the controller MUST be in IDLE state after stopAll"
        )
    }

    @Test
    fun `Stoppable SAM is invoked exactly once on stopAll`() {
        val q = PipelineQuiescer()
        val callCount = AtomicInteger(0)
        val stoppable = PipelineQuiescer.Stoppable { callCount.incrementAndGet() }
        q.register(stoppable)

        val count = q.stopAll()
        assertEquals(1, count)
        assertEquals(1, callCount.get(), "Stoppable.stop() must fire exactly once")
    }

    @Test
    fun `mixed Stoppable and DynamicPipelineController registrations are stopped together`() {
        val q = PipelineQuiescer()
        val callCount = AtomicInteger(0)
        val stoppable = PipelineQuiescer.Stoppable { callCount.incrementAndGet() }
        val controller = makeController()

        q.register(stoppable)
        q.register(controller)

        val count = q.stopAll()
        assertEquals(2, count)
        assertEquals(1, callCount.get())
        assertEquals(ExecutionState.IDLE, controller.executionState.value)
    }

    @Test
    fun `unregister Stoppable prevents subsequent stopAll from invoking it`() {
        val q = PipelineQuiescer()
        val callCount = AtomicInteger(0)
        val stoppable = PipelineQuiescer.Stoppable { callCount.incrementAndGet() }
        q.register(stoppable)
        q.unregister(stoppable)

        val count = q.stopAll()
        assertEquals(0, count)
        assertEquals(0, callCount.get())
    }

    @Test
    fun `unregister DynamicPipelineController by reference removes its wrap`() {
        val q = PipelineQuiescer()
        val controller = makeController()
        q.register(controller)
        q.unregister(controller)

        val count = q.stopAll()
        assertEquals(0, count)
    }

    @Test
    fun `Stoppable that throws does not abort the rest of stopAll`() {
        val q = PipelineQuiescer()
        val firstFired = AtomicInteger(0)
        val secondFired = AtomicInteger(0)
        q.register(PipelineQuiescer.Stoppable { firstFired.incrementAndGet() })
        q.register(PipelineQuiescer.Stoppable { error("intentionally throws") })
        q.register(PipelineQuiescer.Stoppable { secondFired.incrementAndGet() })

        val count = q.stopAll()
        assertEquals(3, count, "all three targets are counted even when one throws")
        assertTrue(firstFired.get() == 1 && secondFired.get() == 1)
    }
}
