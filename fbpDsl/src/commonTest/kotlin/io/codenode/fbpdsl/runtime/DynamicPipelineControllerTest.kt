/*
 * DynamicPipelineControllerTest - Tests for the universal-runtime controller
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowExecutionStatus
import io.codenode.fbpdsl.model.FlowGraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests added by feature 085 (Collapse the entity-module thick runtime onto
 * DynamicPipelineController). The `getStatus()` method is the only behavioral
 * gap between today's per-module Controllers and DynamicPipelineController;
 * these tests pin its contract before the production-app path depends on it
 * (KMPMobileApp's StopWatchIntegrationTest:369-378).
 */
class DynamicPipelineControllerTest {

    private fun emptyController(): DynamicPipelineController {
        val empty = flowGraph("test", version = "1.0.0") {}
        return DynamicPipelineController(
            flowGraphProvider = { empty },
            lookup = { null }
        )
    }

    @Test
    fun getStatus_returns_idle_before_start() {
        val controller = emptyController()
        val status: FlowExecutionStatus = controller.getStatus()
        assertNotNull(status, "getStatus() must never return null")
        assertEquals(
            ExecutionState.IDLE,
            status.overallState,
            "before start(), overallState must be IDLE"
        )
    }

    @Test
    fun getStatus_returns_FlowExecutionStatus_with_consistent_counts() {
        val controller = emptyController()
        val status = controller.getStatus()
        // Empty graph: totalNodes = 0; all per-state counts = 0.
        assertEquals(0, status.totalNodes)
        assertEquals(0, status.idleCount)
        assertEquals(0, status.runningCount)
        assertEquals(0, status.pausedCount)
        assertEquals(0, status.errorCount)
    }

    @Test
    fun getStatus_overallState_reflects_paused_after_pause() {
        val controller = emptyController()
        controller.pause()
        assertEquals(
            ExecutionState.PAUSED,
            controller.getStatus().overallState,
            "after pause(), overallState must reflect PAUSED"
        )
    }

    @Test
    fun getStatus_overallState_reflects_running_after_resume() {
        val controller = emptyController()
        controller.pause()
        controller.resume()
        assertEquals(
            ExecutionState.RUNNING,
            controller.getStatus().overallState,
            "after pause→resume, overallState must reflect RUNNING"
        )
    }

    @Test
    fun getStatus_returns_idle_after_stop() {
        val controller = emptyController()
        controller.pause()
        controller.stop()
        assertEquals(
            ExecutionState.IDLE,
            controller.getStatus().overallState,
            "after stop(), overallState must revert to IDLE"
        )
    }

    @Test
    fun getStatus_overallState_tracks_executionState_property() {
        // The two surfaces (StateFlow + getStatus) must agree.
        val controller = emptyController()
        assertEquals(controller.executionState.value, controller.getStatus().overallState)
        controller.pause()
        assertEquals(controller.executionState.value, controller.getStatus().overallState)
        controller.resume()
        assertEquals(controller.executionState.value, controller.getStatus().overallState)
        controller.stop()
        assertEquals(controller.executionState.value, controller.getStatus().overallState)
    }

    @Test
    fun getStatus_is_idempotent() {
        // Successive calls without state changes must return equivalent results.
        val controller = emptyController()
        val first = controller.getStatus()
        val second = controller.getStatus()
        assertEquals(first.overallState, second.overallState)
        assertEquals(first.totalNodes, second.totalNodes)
    }
}
