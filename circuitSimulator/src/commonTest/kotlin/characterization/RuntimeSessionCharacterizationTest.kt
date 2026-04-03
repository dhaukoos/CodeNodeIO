/*
 * RuntimeSession Characterization Test
 * Pins current RuntimeSession lifecycle and observer behavior
 * License: Apache 2.0
 */

package characterization

import io.codenode.circuitsimulator.DataFlowAnimationController
import io.codenode.circuitsimulator.DataFlowDebugger
import io.codenode.circuitsimulator.RuntimeSession
import io.codenode.fbpdsl.model.*
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.ModuleController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.*

/**
 * Characterization tests for RuntimeSession, DataFlowAnimationController, and DataFlowDebugger.
 *
 * These tests capture the current lifecycle and observer behavior, not correctness.
 * They serve as a safety net during vertical-slice extraction to flowGraph-execute.
 */
class RuntimeSessionCharacterizationTest {

    // ========== Fake ModuleController ==========

    /**
     * Minimal fake ModuleController for testing RuntimeSession lifecycle.
     * Tracks method calls and manages execution state transitions.
     */
    private class FakeModuleController : ModuleController {
        private val _executionState = MutableStateFlow(ExecutionState.IDLE)
        override val executionState: StateFlow<ExecutionState> = _executionState.asStateFlow()

        var startCalled = false
        var pauseCalled = false
        var resumeCalled = false
        var resetCalled = false
        var lastAttenuationDelay: Long? = null
        var lastEmissionObserver: ((String, Int) -> Unit)? = null
        var lastValueObserver: ((String, Int, Any?) -> Unit)? = null

        /** When true, start() transitions to RUNNING. When false, stays IDLE (simulates validation failure). */
        var startSucceeds = true

        /** Dummy FlowGraph returned by lifecycle methods */
        private val dummyFlowGraph = FlowGraph(
            id = "dummy", name = "Dummy", version = "1.0.0",
            rootNodes = emptyList(), connections = emptyList()
        )

        override fun start(): FlowGraph {
            startCalled = true
            if (startSucceeds) {
                _executionState.value = ExecutionState.RUNNING
            }
            return dummyFlowGraph
        }

        override fun stop(): FlowGraph {
            _executionState.value = ExecutionState.IDLE
            return dummyFlowGraph
        }

        override fun pause(): FlowGraph {
            pauseCalled = true
            _executionState.value = ExecutionState.PAUSED
            return dummyFlowGraph
        }

        override fun resume(): FlowGraph {
            resumeCalled = true
            _executionState.value = ExecutionState.RUNNING
            return dummyFlowGraph
        }

        override fun reset(): FlowGraph {
            resetCalled = true
            _executionState.value = ExecutionState.IDLE
            return dummyFlowGraph
        }

        override fun setAttenuationDelay(ms: Long?) {
            lastAttenuationDelay = ms
        }

        override fun setEmissionObserver(observer: ((String, Int) -> Unit)?) {
            lastEmissionObserver = observer
        }

        override fun setValueObserver(observer: ((String, Int, Any?) -> Unit)?) {
            lastValueObserver = observer
        }
    }

    // ========== Test Fixtures ==========

    private fun createTestFlowGraph(): FlowGraph {
        val sourceId = "source_1"
        val sinkId = "sink_1"
        return FlowGraph(
            id = "test_flow",
            name = "TestFlow",
            version = "1.0.0",
            rootNodes = listOf(
                CodeNode(
                    id = sourceId,
                    name = "Source",
                    codeNodeType = CodeNodeType.SOURCE,
                    position = Node.Position(0.0, 0.0),
                    outputPorts = listOf(
                        Port(id = "p_out", name = "output", direction = Port.Direction.OUTPUT, dataType = String::class, owningNodeId = sourceId)
                    )
                ),
                CodeNode(
                    id = sinkId,
                    name = "Sink",
                    codeNodeType = CodeNodeType.SINK,
                    position = Node.Position(200.0, 0.0),
                    inputPorts = listOf(
                        Port(id = "p_in", name = "input", direction = Port.Direction.INPUT, dataType = String::class, owningNodeId = sinkId)
                    )
                )
            ),
            connections = listOf(
                Connection("c1", sourceId, "p_out", sinkId, "p_in")
            )
        )
    }

    private fun createSession(
        controller: FakeModuleController = FakeModuleController(),
        flowGraph: FlowGraph? = createTestFlowGraph()
    ): Pair<RuntimeSession, FakeModuleController> {
        val session = RuntimeSession(
            controller = controller,
            viewModel = Any(),
            flowGraph = flowGraph
        )
        return session to controller
    }

    // ========== Initial State Tests ==========

    @Test
    fun `initial execution state is IDLE`() {
        val (session, _) = createSession()

        assertEquals(ExecutionState.IDLE, session.executionState.value,
            "Initial execution state should be IDLE")
    }

    @Test
    fun `initial attenuation delay is zero`() {
        val (session, _) = createSession()

        assertEquals(0L, session.attenuationDelayMs.value,
            "Initial attenuation should be 0 (max speed)")
    }

    @Test
    fun `initial animate data flow is false`() {
        val (session, _) = createSession()

        assertFalse(session.animateDataFlow.value,
            "Animation should be disabled initially")
    }

    @Test
    fun `animation attenuation threshold is 200ms`() {
        val (session, _) = createSession()

        assertEquals(200L, session.animationAttenuationThreshold,
            "Animation threshold should be 200ms")
    }

    // ========== Lifecycle: Start ==========

    @Test
    fun `start transitions from IDLE to RUNNING`() {
        val (session, _) = createSession()

        session.start()

        assertEquals(ExecutionState.RUNNING, session.executionState.value,
            "Should transition to RUNNING after start")
    }

    @Test
    fun `start calls controller start`() {
        val (session, controller) = createSession()

        session.start()

        assertTrue(controller.startCalled, "Should call controller.start()")
    }

    @Test
    fun `start propagates attenuation to controller`() {
        val (session, controller) = createSession()
        session.setAttenuation(500)

        session.start()

        assertEquals(500L, controller.lastAttenuationDelay,
            "Should propagate attenuation to controller on start")
    }

    @Test
    fun `start is no-op when already RUNNING`() {
        val (session, controller) = createSession()
        session.start()
        controller.startCalled = false  // reset

        session.start()  // second start

        assertFalse(controller.startCalled,
            "Second start should be no-op when already RUNNING")
    }

    @Test
    fun `start does not transition to RUNNING if controller fails to start`() {
        val controller = FakeModuleController()
        controller.startSucceeds = false
        val (session, _) = createSession(controller)

        session.start()

        assertEquals(ExecutionState.IDLE, session.executionState.value,
            "Should remain IDLE if controller start fails (validation error)")
    }

    // ========== Lifecycle: Stop ==========

    @Test
    fun `stop transitions from RUNNING to IDLE`() {
        val (session, _) = createSession()
        session.start()

        session.stop()

        assertEquals(ExecutionState.IDLE, session.executionState.value,
            "Should transition to IDLE after stop")
    }

    @Test
    fun `stop calls controller reset`() {
        val (session, controller) = createSession()
        session.start()

        session.stop()

        assertTrue(controller.resetCalled,
            "Should call controller.reset() on stop")
    }

    @Test
    fun `stop is no-op when already IDLE`() {
        val (session, controller) = createSession()

        session.stop()

        assertFalse(controller.resetCalled,
            "Stop should be no-op when already IDLE")
    }

    @Test
    fun `stop from PAUSED transitions to IDLE`() {
        val (session, _) = createSession()
        session.start()
        session.pause()

        session.stop()

        assertEquals(ExecutionState.IDLE, session.executionState.value,
            "Should transition from PAUSED to IDLE on stop")
    }

    // ========== Lifecycle: Pause/Resume ==========

    @Test
    fun `pause transitions from RUNNING to PAUSED`() {
        val (session, _) = createSession()
        session.start()

        session.pause()

        assertEquals(ExecutionState.PAUSED, session.executionState.value,
            "Should transition to PAUSED")
    }

    @Test
    fun `pause calls controller pause`() {
        val (session, controller) = createSession()
        session.start()

        session.pause()

        assertTrue(controller.pauseCalled, "Should call controller.pause()")
    }

    @Test
    fun `pause is no-op when IDLE`() {
        val (session, controller) = createSession()

        session.pause()

        assertFalse(controller.pauseCalled,
            "Pause should be no-op when IDLE")
        assertEquals(ExecutionState.IDLE, session.executionState.value)
    }

    @Test
    fun `resume transitions from PAUSED to RUNNING`() {
        val (session, _) = createSession()
        session.start()
        session.pause()

        session.resume()

        assertEquals(ExecutionState.RUNNING, session.executionState.value,
            "Should transition back to RUNNING")
    }

    @Test
    fun `resume calls controller resume`() {
        val (session, controller) = createSession()
        session.start()
        session.pause()

        session.resume()

        assertTrue(controller.resumeCalled, "Should call controller.resume()")
    }

    @Test
    fun `resume is no-op when IDLE`() {
        val (session, controller) = createSession()

        session.resume()

        assertFalse(controller.resumeCalled,
            "Resume should be no-op when IDLE")
    }

    @Test
    fun `resume is no-op when RUNNING`() {
        val (session, controller) = createSession()
        session.start()

        session.resume()

        assertFalse(controller.resumeCalled,
            "Resume should be no-op when already RUNNING")
    }

    // ========== Full Lifecycle Cycle ==========

    @Test
    fun `full lifecycle IDLE to RUNNING to PAUSED to RUNNING to IDLE`() {
        val (session, _) = createSession()

        assertEquals(ExecutionState.IDLE, session.executionState.value)
        session.start()
        assertEquals(ExecutionState.RUNNING, session.executionState.value)
        session.pause()
        assertEquals(ExecutionState.PAUSED, session.executionState.value)
        session.resume()
        assertEquals(ExecutionState.RUNNING, session.executionState.value)
        session.stop()
        assertEquals(ExecutionState.IDLE, session.executionState.value)
    }

    @Test
    fun `can restart after stop`() {
        val (session, _) = createSession()

        session.start()
        session.stop()
        session.start()

        assertEquals(ExecutionState.RUNNING, session.executionState.value,
            "Should be able to restart after stop")
    }

    // ========== Attenuation ==========

    @Test
    fun `setAttenuation updates attenuation state`() {
        val (session, _) = createSession()

        session.setAttenuation(500)

        assertEquals(500L, session.attenuationDelayMs.value)
    }

    @Test
    fun `setAttenuation clamps to 0-2000 range`() {
        val (session, _) = createSession()

        session.setAttenuation(-100)
        assertEquals(0L, session.attenuationDelayMs.value, "Should clamp negative to 0")

        session.setAttenuation(5000)
        assertEquals(2000L, session.attenuationDelayMs.value, "Should clamp above 2000 to 2000")
    }

    @Test
    fun `setAttenuation propagates to controller`() {
        val (session, controller) = createSession()

        session.setAttenuation(750)

        assertEquals(750L, controller.lastAttenuationDelay)
    }

    @Test
    fun `setAttenuation below threshold auto-disables animation`() {
        val (session, _) = createSession()
        session.setAttenuation(500)
        session.setAnimateDataFlow(true)
        assertTrue(session.animateDataFlow.value, "Animation should be enabled")

        session.setAttenuation(100)  // Below 200ms threshold

        assertFalse(session.animateDataFlow.value,
            "Animation should be auto-disabled when attenuation drops below threshold")
    }

    // ========== Animation ==========

    @Test
    fun `setAnimateDataFlow requires attenuation above threshold`() {
        val (session, _) = createSession()
        // Attenuation is 0 (default) — below 200ms threshold

        session.setAnimateDataFlow(true)

        assertFalse(session.animateDataFlow.value,
            "Animation should not enable when attenuation < threshold")
    }

    @Test
    fun `setAnimateDataFlow enables when attenuation is sufficient`() {
        val (session, _) = createSession()
        session.setAttenuation(500)

        session.setAnimateDataFlow(true)

        assertTrue(session.animateDataFlow.value,
            "Animation should enable when attenuation >= threshold")
    }

    @Test
    fun `setAnimateDataFlow wires emission observer on controller`() {
        val (session, controller) = createSession()
        session.setAttenuation(500)

        session.setAnimateDataFlow(true)

        assertNotNull(controller.lastEmissionObserver,
            "Should wire emission observer when animation enabled")
    }

    @Test
    fun `setAnimateDataFlow wires value observer on controller`() {
        val (session, controller) = createSession()
        session.setAttenuation(500)

        session.setAnimateDataFlow(true)

        assertNotNull(controller.lastValueObserver,
            "Should wire value observer when animation enabled")
    }

    @Test
    fun `disabling animation clears observers on controller`() {
        val (session, controller) = createSession()
        session.setAttenuation(500)
        session.setAnimateDataFlow(true)

        session.setAnimateDataFlow(false)

        assertNull(controller.lastEmissionObserver,
            "Should clear emission observer when animation disabled")
        assertNull(controller.lastValueObserver,
            "Should clear value observer when animation disabled")
    }

    // ========== Pre-Started Controller ==========

    @Test
    fun `pre-started controller skips controller start call`() {
        val controller = FakeModuleController()
        // Simulate pre-started: controller already RUNNING before session.start()
        controller.startSucceeds = true
        controller.start()  // Controller is now RUNNING
        controller.startCalled = false  // Reset tracking

        val session = RuntimeSession(
            controller = controller,
            viewModel = Any(),
            flowGraph = createTestFlowGraph()
        )

        session.start()

        assertFalse(controller.startCalled,
            "Should not call controller.start() for pre-started controller")
        assertEquals(ExecutionState.RUNNING, session.executionState.value,
            "Session should still transition to RUNNING")
    }

    @Test
    fun `pre-started controller stop does not call controller reset`() {
        val controller = FakeModuleController()
        controller.start()  // Pre-start
        controller.startCalled = false

        val session = RuntimeSession(
            controller = controller,
            viewModel = Any(),
            flowGraph = createTestFlowGraph()
        )
        session.start()

        session.stop()

        assertFalse(controller.resetCalled,
            "Should not reset pre-started controller on stop")
        assertEquals(ExecutionState.IDLE, session.executionState.value,
            "Session should still transition to IDLE")
    }

    // ========== DataFlowAnimationController ==========

    @Test
    fun `animationController initial active animations is empty`() {
        val (session, _) = createSession()

        assertTrue(session.animationController.activeAnimations.value.isEmpty(),
            "Initial animations should be empty")
    }

    @Test
    fun `animationController clear resets animations`() {
        val (session, _) = createSession()

        session.animationController.clear()

        assertTrue(session.animationController.activeAnimations.value.isEmpty(),
            "Clear should result in empty animations")
    }

    // ========== DataFlowDebugger ==========

    @Test
    fun `debugger initial state has no snapshots`() {
        val (session, _) = createSession()

        assertNull(session.debugger.getSnapshotValue("any_connection"),
            "Should have no snapshots initially")
    }

    @Test
    fun `debugger clear resets all snapshots`() {
        val (session, _) = createSession()

        session.debugger.clear()

        assertNull(session.debugger.getSnapshotValue("c1"),
            "Clear should reset all snapshots")
    }

    // ========== FlowGraph Provider ==========

    @Test
    fun `session with flowGraphProvider uses provider for observer wiring`() {
        val controller = FakeModuleController()
        val fg = createTestFlowGraph()
        var providerCalled = false

        val session = RuntimeSession(
            controller = controller,
            viewModel = Any(),
            flowGraphProvider = {
                providerCalled = true
                fg
            }
        )
        session.setAttenuation(500)

        session.setAnimateDataFlow(true)

        assertTrue(providerCalled,
            "Should call flowGraphProvider when wiring observers")
    }

    // ========== Validation Error ==========

    @Test
    fun `validationError is null for non-DynamicPipelineController`() {
        val (session, _) = createSession()

        assertNull(session.validationError.value,
            "Validation error should be null for standard ModuleController")
    }
}
