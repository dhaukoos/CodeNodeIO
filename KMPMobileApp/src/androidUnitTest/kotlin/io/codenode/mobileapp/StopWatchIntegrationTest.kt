/*
 * StopWatchIntegrationTest - TDD Tests for StopWatch Composable Integration
 * User Story 3: StopWatch Composable uses generated StopWatchController
 * License: Apache 2.0
 */

package io.codenode.mobileapp

import io.codenode.fbpdsl.model.*
import io.codenode.stopwatch.generated.StopWatchController
import org.junit.Assert.*
import org.junit.Test

/**
 * TDD tests for integrating StopWatch composable with StopWatchController.
 *
 * These tests verify:
 * - T033: Start button calls controller.start() and transitions to RUNNING
 * - T034: Stop button calls controller.stop() and transitions to IDLE
 * - T035: Reset button calls controller.reset() and resets elapsedSeconds/elapsedMinutes to 0
 * - T036: isRunning derived from executionState == RUNNING
 */
class StopWatchIntegrationTest {

    // ========== Test Fixtures ==========

    /**
     * Creates a StopWatch FlowGraph for testing
     */
    private fun createStopWatchFlowGraph(): FlowGraph {
        val timerEmitterId = "timer-emitter"
        val displayReceiverId = "display-receiver"

        val timerEmitter = CodeNode(
            id = timerEmitterId,
            name = "TimerEmitter",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "${timerEmitterId}_elapsedSeconds",
                    name = "elapsedSeconds",
                    direction = Port.Direction.OUTPUT,
                    dataType = Int::class,
                    owningNodeId = timerEmitterId
                ),
                Port(
                    id = "${timerEmitterId}_elapsedMinutes",
                    name = "elapsedMinutes",
                    direction = Port.Direction.OUTPUT,
                    dataType = Int::class,
                    owningNodeId = timerEmitterId
                )
            ),
            controlConfig = ControlConfig(speedAttenuation = 1000L)
        )

        val displayReceiver = CodeNode(
            id = displayReceiverId,
            name = "DisplayReceiver",
            codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 100.0),
            inputPorts = listOf(
                Port(
                    id = "${displayReceiverId}_seconds",
                    name = "seconds",
                    direction = Port.Direction.INPUT,
                    dataType = Int::class,
                    owningNodeId = displayReceiverId
                ),
                Port(
                    id = "${displayReceiverId}_minutes",
                    name = "minutes",
                    direction = Port.Direction.INPUT,
                    dataType = Int::class,
                    owningNodeId = displayReceiverId
                )
            ),
            outputPorts = emptyList()
        )

        val connections = listOf(
            Connection(
                id = "conn_seconds",
                sourceNodeId = timerEmitterId,
                sourcePortId = "${timerEmitterId}_elapsedSeconds",
                targetNodeId = displayReceiverId,
                targetPortId = "${displayReceiverId}_seconds",
                channelCapacity = 1
            ),
            Connection(
                id = "conn_minutes",
                sourceNodeId = timerEmitterId,
                sourcePortId = "${timerEmitterId}_elapsedMinutes",
                targetNodeId = displayReceiverId,
                targetPortId = "${displayReceiverId}_minutes",
                channelCapacity = 1
            )
        )

        return FlowGraph(
            id = "stopwatch-flow",
            name = "StopWatch",
            version = "1.0.0",
            description = "Virtual circuit demo for stopwatch functionality",
            rootNodes = listOf(timerEmitter, displayReceiver),
            connections = connections,
            targetPlatforms = listOf(
                FlowGraph.TargetPlatform.KMP_ANDROID,
                FlowGraph.TargetPlatform.KMP_IOS,
                FlowGraph.TargetPlatform.KMP_DESKTOP
            )
        )
    }

    // ========== T033: Start Button Tests ==========

    @Test
    fun t033_controller_start_transitions_executionState_to_RUNNING() {
        // Given: A StopWatchController in IDLE state
        val flowGraph = createStopWatchFlowGraph()
        val controller = StopWatchController(flowGraph)

        // Verify initial state is IDLE
        assertEquals("Initial state should be IDLE",
            ExecutionState.IDLE, controller.executionState.value)

        // When: Calling start()
        controller.start()

        // Then: State should transition to RUNNING
        assertEquals("After start(), state should be RUNNING",
            ExecutionState.RUNNING, controller.executionState.value)
    }

    @Test
    fun t033_start_returns_updated_FlowGraph() {
        // Given: A StopWatchController
        val flowGraph = createStopWatchFlowGraph()
        val controller = StopWatchController(flowGraph)

        // When: Calling start()
        val updatedGraph = controller.start()

        // Then: Should return a FlowGraph
        assertNotNull("start() should return updated FlowGraph", updatedGraph)
        assertEquals("FlowGraph name should be preserved", "StopWatch", updatedGraph.name)
    }

    @Test
    fun t033_multiple_start_calls_maintain_RUNNING_state() {
        // Given: A StopWatchController
        val flowGraph = createStopWatchFlowGraph()
        val controller = StopWatchController(flowGraph)

        // When: Calling start() multiple times
        controller.start()
        controller.start()

        // Then: State should still be RUNNING
        assertEquals("Multiple start() calls should maintain RUNNING state",
            ExecutionState.RUNNING, controller.executionState.value)
    }

    // ========== T034: Stop Button Tests ==========

    @Test
    fun t034_controller_stop_transitions_executionState_to_IDLE() {
        // Given: A StopWatchController in RUNNING state
        val flowGraph = createStopWatchFlowGraph()
        val controller = StopWatchController(flowGraph)
        controller.start()
        assertEquals(ExecutionState.RUNNING, controller.executionState.value)

        // When: Calling stop()
        controller.stop()

        // Then: State should transition to IDLE
        assertEquals("After stop(), state should be IDLE",
            ExecutionState.IDLE, controller.executionState.value)
    }

    @Test
    fun t034_stop_from_PAUSED_state_transitions_to_IDLE() {
        // Given: A StopWatchController in PAUSED state
        val flowGraph = createStopWatchFlowGraph()
        val controller = StopWatchController(flowGraph)
        controller.start()
        controller.pause()
        assertEquals(ExecutionState.PAUSED, controller.executionState.value)

        // When: Calling stop()
        controller.stop()

        // Then: State should transition to IDLE
        assertEquals("After stop() from PAUSED, state should be IDLE",
            ExecutionState.IDLE, controller.executionState.value)
    }

    @Test
    fun t034_stop_returns_updated_FlowGraph() {
        // Given: A StopWatchController in RUNNING state
        val flowGraph = createStopWatchFlowGraph()
        val controller = StopWatchController(flowGraph)
        controller.start()

        // When: Calling stop()
        val updatedGraph = controller.stop()

        // Then: Should return a FlowGraph
        assertNotNull("stop() should return updated FlowGraph", updatedGraph)
    }

    // ========== T035: Reset Button Tests ==========

    @Test
    fun t035_controller_reset_transitions_executionState_to_IDLE() {
        // Given: A StopWatchController in RUNNING state
        val flowGraph = createStopWatchFlowGraph()
        val controller = StopWatchController(flowGraph)
        controller.start()

        // When: Calling reset()
        controller.reset()

        // Then: State should be IDLE
        assertEquals("After reset(), state should be IDLE",
            ExecutionState.IDLE, controller.executionState.value)
    }

    @Test
    fun t035_reset_resets_elapsedSeconds_to_0() {
        // Given: A StopWatchController
        val flowGraph = createStopWatchFlowGraph()
        val controller = StopWatchController(flowGraph)

        // When: Calling reset()
        controller.reset()

        // Then: elapsedSeconds should be 0
        assertEquals("After reset(), elapsedSeconds should be 0",
            0, controller.elapsedSeconds.value)
    }

    @Test
    fun t035_reset_resets_elapsedMinutes_to_0() {
        // Given: A StopWatchController
        val flowGraph = createStopWatchFlowGraph()
        val controller = StopWatchController(flowGraph)

        // When: Calling reset()
        controller.reset()

        // Then: elapsedMinutes should be 0
        assertEquals("After reset(), elapsedMinutes should be 0",
            0, controller.elapsedMinutes.value)
    }

    @Test
    fun t035_reset_from_PAUSED_state_clears_time_and_transitions_to_IDLE() {
        // Given: A StopWatchController in PAUSED state
        val flowGraph = createStopWatchFlowGraph()
        val controller = StopWatchController(flowGraph)
        controller.start()
        controller.pause()

        // When: Calling reset()
        controller.reset()

        // Then: State should be IDLE and time should be reset
        assertEquals("After reset(), state should be IDLE",
            ExecutionState.IDLE, controller.executionState.value)
        assertEquals("After reset(), elapsedSeconds should be 0",
            0, controller.elapsedSeconds.value)
        assertEquals("After reset(), elapsedMinutes should be 0",
            0, controller.elapsedMinutes.value)
    }

    // ========== T036: isRunning Derivation Tests ==========

    @Test
    fun t036_isRunning_is_true_when_executionState_is_RUNNING() {
        // Given: A StopWatchController
        val flowGraph = createStopWatchFlowGraph()
        val controller = StopWatchController(flowGraph)

        // When: Starting the controller
        controller.start()

        // Then: isRunning should be derived as true
        val isRunning = controller.executionState.value == ExecutionState.RUNNING
        assertTrue("isRunning should be true when executionState is RUNNING", isRunning)
    }

    @Test
    fun t036_isRunning_is_false_when_executionState_is_IDLE() {
        // Given: A StopWatchController in IDLE state
        val flowGraph = createStopWatchFlowGraph()
        val controller = StopWatchController(flowGraph)

        // Then: isRunning should be derived as false
        val isRunning = controller.executionState.value == ExecutionState.RUNNING
        assertFalse("isRunning should be false when executionState is IDLE", isRunning)
    }

    @Test
    fun t036_isRunning_is_false_when_executionState_is_PAUSED() {
        // Given: A StopWatchController in PAUSED state
        val flowGraph = createStopWatchFlowGraph()
        val controller = StopWatchController(flowGraph)
        controller.start()
        controller.pause()

        // Then: isRunning should be derived as false
        val isRunning = controller.executionState.value == ExecutionState.RUNNING
        assertFalse("isRunning should be false when executionState is PAUSED", isRunning)
    }

    @Test
    fun t036_isRunning_transitions_correctly_through_state_changes() {
        // Given: A StopWatchController
        val flowGraph = createStopWatchFlowGraph()
        val controller = StopWatchController(flowGraph)

        // Initially IDLE -> isRunning = false
        assertFalse("Initially isRunning should be false",
            controller.executionState.value == ExecutionState.RUNNING)

        // Start -> isRunning = true
        controller.start()
        assertTrue("After start(), isRunning should be true",
            controller.executionState.value == ExecutionState.RUNNING)

        // Pause -> isRunning = false
        controller.pause()
        assertFalse("After pause(), isRunning should be false",
            controller.executionState.value == ExecutionState.RUNNING)

        // Start again -> isRunning = true
        controller.start()
        assertTrue("After start() from PAUSED, isRunning should be true",
            controller.executionState.value == ExecutionState.RUNNING)

        // Stop -> isRunning = false
        controller.stop()
        assertFalse("After stop(), isRunning should be false",
            controller.executionState.value == ExecutionState.RUNNING)
    }

    // ========== Additional Controller State Tests ==========

    @Test
    fun controller_pause_transitions_executionState_to_PAUSED() {
        // Given: A StopWatchController in RUNNING state
        val flowGraph = createStopWatchFlowGraph()
        val controller = StopWatchController(flowGraph)
        controller.start()

        // When: Calling pause()
        controller.pause()

        // Then: State should be PAUSED
        assertEquals("After pause(), state should be PAUSED",
            ExecutionState.PAUSED, controller.executionState.value)
    }

    @Test
    fun controller_getStatus_returns_FlowExecutionStatus() {
        // Given: A StopWatchController
        val flowGraph = createStopWatchFlowGraph()
        val controller = StopWatchController(flowGraph)

        // When: Calling getStatus()
        val status = controller.getStatus()

        // Then: Should return a valid status
        assertNotNull("getStatus() should return FlowExecutionStatus", status)
    }

    @Test
    fun controller_exposes_StateFlow_properties() {
        // Given: A StopWatchController
        val flowGraph = createStopWatchFlowGraph()
        val controller = StopWatchController(flowGraph)

        // Then: All StateFlow properties should be accessible
        assertNotNull("elapsedSeconds StateFlow should be accessible", controller.elapsedSeconds)
        assertNotNull("elapsedMinutes StateFlow should be accessible", controller.elapsedMinutes)
        assertNotNull("executionState StateFlow should be accessible", controller.executionState)

        // And: Initial values should be correct
        assertEquals("Initial elapsedSeconds should be 0", 0, controller.elapsedSeconds.value)
        assertEquals("Initial elapsedMinutes should be 0", 0, controller.elapsedMinutes.value)
        assertEquals("Initial executionState should be IDLE",
            ExecutionState.IDLE, controller.executionState.value)
    }
}
