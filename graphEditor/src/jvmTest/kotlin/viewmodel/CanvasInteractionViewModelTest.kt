/*
 * CanvasInteractionViewModelTest - Unit tests for CanvasInteractionViewModel
 * Verifies drag, connection, selection, and hover operations without Compose UI dependencies
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.Port
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CanvasInteractionViewModelTest {

    // ========== Initial State Tests ==========

    @Test
    fun `initial state is idle with no active interactions`() = runTest {
        val viewModel = CanvasInteractionViewModel()
        val state = viewModel.state.first()

        assertEquals(InteractionMode.IDLE, state.interactionMode)
        assertNull(state.draggingNodeId)
        assertNull(state.pendingConnection)
        assertNull(state.selectionBoxBounds)
        assertNull(state.hoveredNodeId)
        assertNull(state.hoveredPort)
        assertNull(state.connectionContextMenu)
        assertFalse(state.hasActiveInteraction)
    }

    // ========== Drag Tests ==========

    @Test
    fun `startNodeDrag sets dragging state`() = runTest {
        val viewModel = CanvasInteractionViewModel()

        viewModel.startNodeDrag("node123")

        val state = viewModel.state.first()
        assertEquals(InteractionMode.DRAGGING_NODE, state.interactionMode)
        assertEquals("node123", state.draggingNodeId)
        assertTrue(state.isDraggingNode)
        assertTrue(state.hasActiveInteraction)
    }

    @Test
    fun `endNodeDrag calls callback and resets state`() = runTest {
        var movedNodeId: String? = null
        var movedX: Double? = null
        var movedY: Double? = null

        val viewModel = CanvasInteractionViewModel(
            onNodeMoved = { nodeId, x, y ->
                movedNodeId = nodeId
                movedX = x
                movedY = y
            }
        )

        viewModel.startNodeDrag("node123")
        viewModel.endNodeDrag(100.0, 200.0)

        assertEquals("node123", movedNodeId)
        assertEquals(100.0, movedX)
        assertEquals(200.0, movedY)

        val state = viewModel.state.first()
        assertEquals(InteractionMode.IDLE, state.interactionMode)
        assertNull(state.draggingNodeId)
        assertFalse(state.isDraggingNode)
    }

    @Test
    fun `cancelNodeDrag resets state without callback`() = runTest {
        var callbackCalled = false

        val viewModel = CanvasInteractionViewModel(
            onNodeMoved = { _, _, _ -> callbackCalled = true }
        )

        viewModel.startNodeDrag("node123")
        viewModel.cancelNodeDrag()

        assertFalse(callbackCalled)

        val state = viewModel.state.first()
        assertEquals(InteractionMode.IDLE, state.interactionMode)
        assertNull(state.draggingNodeId)
    }

    // ========== Connection Tests ==========

    @Test
    fun `startConnectionCreation sets pending connection`() = runTest {
        val viewModel = CanvasInteractionViewModel()

        viewModel.startConnectionCreation("sourceNode", "sourcePort", Offset(50f, 100f))

        val state = viewModel.state.first()
        assertEquals(InteractionMode.CREATING_CONNECTION, state.interactionMode)
        assertTrue(state.isCreatingConnection)

        val pending = state.pendingConnection
        assertNotNull(pending)
        assertEquals("sourceNode", pending.sourceNodeId)
        assertEquals("sourcePort", pending.sourcePortId)
        assertEquals(Offset(50f, 100f), pending.endPosition)
    }

    @Test
    fun `updateConnectionEndpoint updates pending connection position`() = runTest {
        val viewModel = CanvasInteractionViewModel()

        viewModel.startConnectionCreation("node", "port", Offset(0f, 0f))
        viewModel.updateConnectionEndpoint(Offset(200f, 300f))

        val state = viewModel.state.first()
        assertEquals(Offset(200f, 300f), state.pendingConnection?.endPosition)
    }

    @Test
    fun `completeConnection calls callback and resets state`() = runTest {
        var createdConnection: Connection? = null

        val viewModel = CanvasInteractionViewModel(
            onConnectionCreated = { createdConnection = it }
        )

        viewModel.startConnectionCreation("sourceNode", "sourcePort", Offset(0f, 0f))
        viewModel.completeConnection("targetNode", "targetPort")

        assertNotNull(createdConnection)
        assertEquals("sourceNode", createdConnection!!.sourceNodeId)
        assertEquals("sourcePort", createdConnection!!.sourcePortId)
        assertEquals("targetNode", createdConnection!!.targetNodeId)
        assertEquals("targetPort", createdConnection!!.targetPortId)

        val state = viewModel.state.first()
        assertEquals(InteractionMode.IDLE, state.interactionMode)
        assertNull(state.pendingConnection)
    }

    @Test
    fun `completeConnection does not create connection to same node`() = runTest {
        var callbackCalled = false

        val viewModel = CanvasInteractionViewModel(
            onConnectionCreated = { callbackCalled = true }
        )

        viewModel.startConnectionCreation("sameNode", "port1", Offset(0f, 0f))
        viewModel.completeConnection("sameNode", "port2")

        assertFalse(callbackCalled)
    }

    @Test
    fun `cancelConnection resets state without callback`() = runTest {
        var callbackCalled = false

        val viewModel = CanvasInteractionViewModel(
            onConnectionCreated = { callbackCalled = true }
        )

        viewModel.startConnectionCreation("source", "port", Offset(0f, 0f))
        viewModel.cancelConnection()

        assertFalse(callbackCalled)

        val state = viewModel.state.first()
        assertEquals(InteractionMode.IDLE, state.interactionMode)
        assertNull(state.pendingConnection)
    }

    // ========== Selection Tests ==========

    @Test
    fun `startRectangularSelection creates initial selection box`() = runTest {
        val viewModel = CanvasInteractionViewModel()

        viewModel.startRectangularSelection(Offset(100f, 100f))

        val state = viewModel.state.first()
        assertEquals(InteractionMode.RECTANGULAR_SELECTION, state.interactionMode)
        assertTrue(state.isRectangularSelectionActive)
        assertNotNull(state.selectionBoxBounds)
        assertEquals(Offset(100f, 100f), state.selectionBoxBounds?.topLeft)
    }

    @Test
    fun `updateRectangularSelection expands selection box`() = runTest {
        val viewModel = CanvasInteractionViewModel()

        viewModel.startRectangularSelection(Offset(100f, 100f))
        viewModel.updateRectangularSelection(Offset(300f, 200f))

        val state = viewModel.state.first()
        val bounds = state.selectionBoxBounds
        assertNotNull(bounds)
        assertEquals(100f, bounds.left)
        assertEquals(100f, bounds.top)
        assertEquals(300f, bounds.right)
        assertEquals(200f, bounds.bottom)
    }

    @Test
    fun `updateRectangularSelection handles negative drag direction`() = runTest {
        val viewModel = CanvasInteractionViewModel()

        viewModel.startRectangularSelection(Offset(300f, 200f))
        viewModel.updateRectangularSelection(Offset(100f, 100f))

        val state = viewModel.state.first()
        val bounds = state.selectionBoxBounds
        assertNotNull(bounds)
        assertEquals(100f, bounds.left)
        assertEquals(100f, bounds.top)
        assertEquals(300f, bounds.right)
        assertEquals(200f, bounds.bottom)
    }

    @Test
    fun `finishRectangularSelection resets state`() = runTest {
        val viewModel = CanvasInteractionViewModel()

        viewModel.startRectangularSelection(Offset(0f, 0f))
        viewModel.updateRectangularSelection(Offset(100f, 100f))
        viewModel.finishRectangularSelection()

        val state = viewModel.state.first()
        assertEquals(InteractionMode.IDLE, state.interactionMode)
        assertNull(state.selectionBoxBounds)
        assertFalse(state.isRectangularSelectionActive)
    }

    @Test
    fun `cancelRectangularSelection resets state`() = runTest {
        val viewModel = CanvasInteractionViewModel()

        viewModel.startRectangularSelection(Offset(0f, 0f))
        viewModel.cancelRectangularSelection()

        val state = viewModel.state.first()
        assertEquals(InteractionMode.IDLE, state.interactionMode)
        assertNull(state.selectionBoxBounds)
    }

    // ========== Hover/Menu Tests ==========

    @Test
    fun `showConnectionContextMenu sets menu state`() = runTest {
        val viewModel = CanvasInteractionViewModel()

        viewModel.showConnectionContextMenu("conn123", Offset(150f, 250f))

        val state = viewModel.state.first()
        val menu = state.connectionContextMenu
        assertNotNull(menu)
        assertEquals("conn123", menu.connectionId)
        assertEquals(Offset(150f, 250f), menu.screenPosition)
    }

    @Test
    fun `hideConnectionContextMenu clears menu state`() = runTest {
        val viewModel = CanvasInteractionViewModel()

        viewModel.showConnectionContextMenu("conn", Offset(0f, 0f))
        viewModel.hideConnectionContextMenu()

        assertNull(viewModel.state.first().connectionContextMenu)
    }

    @Test
    fun `setHoveredNode updates hovered node`() = runTest {
        val viewModel = CanvasInteractionViewModel()

        viewModel.setHoveredNode("node123")
        assertEquals("node123", viewModel.state.first().hoveredNodeId)

        viewModel.setHoveredNode(null)
        assertNull(viewModel.state.first().hoveredNodeId)
    }

    @Test
    fun `setHoveredPort updates hovered port`() = runTest {
        val viewModel = CanvasInteractionViewModel()

        val portInfo = HoveredPortInfo("node1", "port1", Port.Direction.OUTPUT)
        viewModel.setHoveredPort(portInfo)

        val state = viewModel.state.first()
        assertEquals(portInfo, state.hoveredPort)

        viewModel.setHoveredPort(null)
        assertNull(viewModel.state.first().hoveredPort)
    }

    @Test
    fun `clearHoverState clears both node and port hover`() = runTest {
        val viewModel = CanvasInteractionViewModel()

        viewModel.setHoveredNode("node")
        viewModel.setHoveredPort(HoveredPortInfo("node", "port", Port.Direction.INPUT))

        viewModel.clearHoverState()

        val state = viewModel.state.first()
        assertNull(state.hoveredNodeId)
        assertNull(state.hoveredPort)
    }

    // ========== General Tests ==========

    @Test
    fun `reset clears all state`() = runTest {
        val viewModel = CanvasInteractionViewModel()

        // Set various state
        viewModel.startNodeDrag("node")
        viewModel.setHoveredNode("hovered")
        viewModel.showConnectionContextMenu("conn", Offset(0f, 0f))

        // Reset
        viewModel.reset()

        val state = viewModel.state.first()
        assertEquals(InteractionMode.IDLE, state.interactionMode)
        assertNull(state.draggingNodeId)
        assertNull(state.pendingConnection)
        assertNull(state.selectionBoxBounds)
        assertNull(state.hoveredNodeId)
        assertNull(state.hoveredPort)
        assertNull(state.connectionContextMenu)
    }

    @Test
    fun `startPanning sets panning mode`() = runTest {
        val viewModel = CanvasInteractionViewModel()

        viewModel.startPanning()

        assertEquals(InteractionMode.PANNING, viewModel.state.first().interactionMode)
    }

    @Test
    fun `endPanning returns to idle`() = runTest {
        val viewModel = CanvasInteractionViewModel()

        viewModel.startPanning()
        viewModel.endPanning()

        assertEquals(InteractionMode.IDLE, viewModel.state.first().interactionMode)
    }
}
