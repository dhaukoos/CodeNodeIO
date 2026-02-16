/*
 * CanvasInteractionViewModel - ViewModel for Canvas Interactions
 * Encapsulates state and business logic for canvas drag, connection, selection, and hover interactions
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.Port

/**
 * Represents the current interaction mode of the canvas.
 */
enum class InteractionMode {
    /** No interaction in progress */
    IDLE,
    /** Dragging a node */
    DRAGGING_NODE,
    /** Creating a connection by dragging from an output port */
    CREATING_CONNECTION,
    /** Rectangular selection in progress (Shift+drag on empty canvas) */
    RECTANGULAR_SELECTION,
    /** Panning the canvas */
    PANNING
}

/**
 * Represents a pending connection being created.
 *
 * @param sourceNodeId The ID of the source node
 * @param sourcePortId The ID of the source output port
 * @param endPosition The current end position of the connection line (screen coordinates)
 */
data class PendingConnection(
    val sourceNodeId: String,
    val sourcePortId: String,
    val endPosition: Offset
)

/**
 * Represents information about a hovered port.
 *
 * @param nodeId The ID of the node containing the port
 * @param portId The ID of the port
 * @param direction The direction of the port (INPUT or OUTPUT)
 */
data class HoveredPortInfo(
    val nodeId: String,
    val portId: String,
    val direction: Port.Direction
)

/**
 * Represents information for a connection context menu.
 *
 * @param connectionId The ID of the connection
 * @param screenPosition The screen position where the menu should appear
 */
data class ConnectionContextMenuInfo(
    val connectionId: String,
    val screenPosition: Offset
)

/**
 * State data class for the Canvas Interaction ViewModel.
 * Contains all business state for canvas interactions.
 *
 * @param interactionMode The current interaction mode
 * @param draggingNodeId ID of the node being dragged (null if not dragging)
 * @param pendingConnection Information about the connection being created (null if not creating)
 * @param selectionBoxBounds Bounds of the rectangular selection box (null if not selecting)
 * @param hoveredNodeId ID of the node currently hovered (null if none)
 * @param hoveredPort Information about the currently hovered port (null if none)
 * @param connectionContextMenu Information for showing connection context menu (null if not showing)
 */
data class CanvasInteractionState(
    val interactionMode: InteractionMode = InteractionMode.IDLE,
    val draggingNodeId: String? = null,
    val pendingConnection: PendingConnection? = null,
    val selectionBoxBounds: Rect? = null,
    val hoveredNodeId: String? = null,
    val hoveredPort: HoveredPortInfo? = null,
    val connectionContextMenu: ConnectionContextMenuInfo? = null
) : BaseState {
    /** Whether a node drag is in progress */
    val isDraggingNode: Boolean
        get() = interactionMode == InteractionMode.DRAGGING_NODE && draggingNodeId != null

    /** Whether a connection is being created */
    val isCreatingConnection: Boolean
        get() = interactionMode == InteractionMode.CREATING_CONNECTION && pendingConnection != null

    /** Whether rectangular selection is active */
    val isRectangularSelectionActive: Boolean
        get() = interactionMode == InteractionMode.RECTANGULAR_SELECTION && selectionBoxBounds != null

    /** Whether any interaction is in progress */
    val hasActiveInteraction: Boolean
        get() = interactionMode != InteractionMode.IDLE
}

/**
 * ViewModel for canvas interactions.
 * Manages state and business logic for drag, connection, selection, and hover operations.
 *
 * This ViewModel encapsulates:
 * - Node dragging state and actions
 * - Connection creation state and actions
 * - Rectangular selection state and actions
 * - Hover state for nodes and ports
 * - Connection context menu state
 *
 * Transient gesture state (dragOffset, shiftPressedAtPointerDown) remains in FlowGraphCanvas
 * as per Decision 4 in research.md.
 *
 * @param onNodeMoved Callback when a node drag completes: (nodeId, newX, newY)
 * @param onConnectionCreated Callback when a new connection is created
 */
class CanvasInteractionViewModel(
    private val onNodeMoved: (String, Double, Double) -> Unit = { _, _, _ -> },
    private val onConnectionCreated: (Connection) -> Unit = { _ -> }
) : ViewModel() {

    private val _state = MutableStateFlow(CanvasInteractionState())
    val state: StateFlow<CanvasInteractionState> = _state.asStateFlow()

    // ========== Drag Actions ==========

    /**
     * Starts dragging a node.
     *
     * @param nodeId The ID of the node to drag
     */
    fun startNodeDrag(nodeId: String) {
        _state.update {
            it.copy(
                interactionMode = InteractionMode.DRAGGING_NODE,
                draggingNodeId = nodeId
            )
        }
    }

    /**
     * Updates the node drag state.
     * Note: The actual dragOffset is managed in FlowGraphCanvas as transient gesture state.
     * This method is available for any business state updates during drag.
     */
    fun updateNodeDrag() {
        // Currently no business state to update during drag
        // The dragOffset (screen pixels) is managed in FlowGraphCanvas
    }

    /**
     * Ends the node drag and commits the position change.
     *
     * @param newX The new X position in graph coordinates
     * @param newY The new Y position in graph coordinates
     */
    fun endNodeDrag(newX: Double, newY: Double) {
        val nodeId = _state.value.draggingNodeId
        if (nodeId != null) {
            onNodeMoved(nodeId, newX, newY)
        }
        _state.update {
            it.copy(
                interactionMode = InteractionMode.IDLE,
                draggingNodeId = null
            )
        }
    }

    /**
     * Cancels the current node drag without committing changes.
     */
    fun cancelNodeDrag() {
        _state.update {
            it.copy(
                interactionMode = InteractionMode.IDLE,
                draggingNodeId = null
            )
        }
    }

    // ========== Connection Actions ==========

    /**
     * Starts creating a new connection from an output port.
     *
     * @param sourceNodeId The ID of the source node
     * @param sourcePortId The ID of the source output port
     * @param startPosition The screen position where the connection starts
     */
    fun startConnectionCreation(sourceNodeId: String, sourcePortId: String, startPosition: Offset) {
        _state.update {
            it.copy(
                interactionMode = InteractionMode.CREATING_CONNECTION,
                pendingConnection = PendingConnection(
                    sourceNodeId = sourceNodeId,
                    sourcePortId = sourcePortId,
                    endPosition = startPosition
                )
            )
        }
    }

    /**
     * Updates the end position of the pending connection.
     *
     * @param newEndPosition The new end position in screen coordinates
     */
    fun updateConnectionEndpoint(newEndPosition: Offset) {
        _state.update { currentState ->
            currentState.pendingConnection?.let { pending ->
                currentState.copy(
                    pendingConnection = pending.copy(endPosition = newEndPosition)
                )
            } ?: currentState
        }
    }

    /**
     * Completes the connection to a target port.
     *
     * @param targetNodeId The ID of the target node
     * @param targetPortId The ID of the target input port
     */
    fun completeConnection(targetNodeId: String, targetPortId: String) {
        val pending = _state.value.pendingConnection
        if (pending != null && targetNodeId != pending.sourceNodeId) {
            val connection = Connection(
                id = "conn_${System.currentTimeMillis()}",
                sourceNodeId = pending.sourceNodeId,
                sourcePortId = pending.sourcePortId,
                targetNodeId = targetNodeId,
                targetPortId = targetPortId
            )
            onConnectionCreated(connection)
        }
        cancelConnection()
    }

    /**
     * Cancels the current connection creation.
     */
    fun cancelConnection() {
        _state.update {
            it.copy(
                interactionMode = InteractionMode.IDLE,
                pendingConnection = null
            )
        }
    }

    // ========== Selection Actions ==========

    /**
     * Starts rectangular selection (Shift+drag on empty canvas).
     *
     * @param startPosition The screen position where selection starts
     */
    fun startRectangularSelection(startPosition: Offset) {
        _state.update {
            it.copy(
                interactionMode = InteractionMode.RECTANGULAR_SELECTION,
                selectionBoxBounds = Rect(startPosition, startPosition)
            )
        }
    }

    /**
     * Updates the rectangular selection bounds.
     *
     * @param currentPosition The current position during drag
     */
    fun updateRectangularSelection(currentPosition: Offset) {
        _state.update { currentState ->
            currentState.selectionBoxBounds?.let { bounds ->
                // Keep the original start position (topLeft) and update to current position
                val startPosition = bounds.topLeft
                currentState.copy(
                    selectionBoxBounds = Rect(
                        left = minOf(startPosition.x, currentPosition.x),
                        top = minOf(startPosition.y, currentPosition.y),
                        right = maxOf(startPosition.x, currentPosition.x),
                        bottom = maxOf(startPosition.y, currentPosition.y)
                    )
                )
            } ?: currentState
        }
    }

    /**
     * Finishes the rectangular selection.
     * The actual selection of nodes is handled by the caller based on the final bounds.
     */
    fun finishRectangularSelection() {
        _state.update {
            it.copy(
                interactionMode = InteractionMode.IDLE,
                selectionBoxBounds = null
            )
        }
    }

    /**
     * Cancels the rectangular selection.
     */
    fun cancelRectangularSelection() {
        _state.update {
            it.copy(
                interactionMode = InteractionMode.IDLE,
                selectionBoxBounds = null
            )
        }
    }

    // ========== Hover/Menu Actions ==========

    /**
     * Shows the connection context menu.
     *
     * @param connectionId The ID of the connection
     * @param screenPosition The screen position for the menu
     */
    fun showConnectionContextMenu(connectionId: String, screenPosition: Offset) {
        _state.update {
            it.copy(
                connectionContextMenu = ConnectionContextMenuInfo(connectionId, screenPosition)
            )
        }
    }

    /**
     * Hides the connection context menu.
     */
    fun hideConnectionContextMenu() {
        _state.update {
            it.copy(connectionContextMenu = null)
        }
    }

    /**
     * Sets the currently hovered node.
     *
     * @param nodeId The ID of the hovered node, or null to clear
     */
    fun setHoveredNode(nodeId: String?) {
        _state.update {
            it.copy(hoveredNodeId = nodeId)
        }
    }

    /**
     * Sets the currently hovered port.
     *
     * @param portInfo Information about the hovered port, or null to clear
     */
    fun setHoveredPort(portInfo: HoveredPortInfo?) {
        _state.update {
            it.copy(hoveredPort = portInfo)
        }
    }

    /**
     * Clears all hover state.
     */
    fun clearHoverState() {
        _state.update {
            it.copy(
                hoveredNodeId = null,
                hoveredPort = null
            )
        }
    }

    // ========== General Actions ==========

    /**
     * Resets all interaction state to idle.
     */
    fun reset() {
        _state.update {
            CanvasInteractionState()
        }
    }

    /**
     * Starts canvas panning mode.
     */
    fun startPanning() {
        _state.update {
            it.copy(interactionMode = InteractionMode.PANNING)
        }
    }

    /**
     * Ends canvas panning mode.
     */
    fun endPanning() {
        _state.update {
            it.copy(interactionMode = InteractionMode.IDLE)
        }
    }
}
