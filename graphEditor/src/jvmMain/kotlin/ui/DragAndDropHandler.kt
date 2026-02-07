/*
 * DragAndDropHandler - Drag and Drop Logic for Node Palette to Canvas
 * Handles dragging node types from the palette and dropping them onto the canvas
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import io.codenode.fbpdsl.model.NodeTypeDefinition
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.grapheditor.state.GraphState

/**
 * State holder for drag-and-drop operations
 * Tracks the node type being dragged and current drag position
 */
class DragAndDropState {
    /**
     * The node type currently being dragged (null if no drag in progress)
     */
    var draggingNodeType by mutableStateOf<NodeTypeDefinition?>(null)
        private set

    /**
     * Current drag position in screen coordinates (relative to drag start)
     */
    var dragPosition by mutableStateOf(Offset.Zero)
        private set

    /**
     * Initial drag start position in screen coordinates
     */
    var dragStartPosition by mutableStateOf(Offset.Zero)
        private set

    /**
     * Whether a drag operation is currently in progress
     */
    val isDragging: Boolean
        get() = draggingNodeType != null

    /**
     * Starts a drag operation with the specified node type
     *
     * @param nodeType The node type being dragged
     * @param startPosition Initial position where drag started
     */
    fun startDrag(nodeType: NodeTypeDefinition, startPosition: Offset) {
        draggingNodeType = nodeType
        dragStartPosition = startPosition
        dragPosition = startPosition
    }

    /**
     * Updates the drag position during a drag operation
     *
     * @param position New position in screen coordinates
     */
    fun updateDragPosition(position: Offset) {
        dragPosition = position
    }

    /**
     * Ends the drag operation and returns the dragged node type
     *
     * @return The node type that was being dragged, or null if no drag in progress
     */
    fun endDrag(): NodeTypeDefinition? {
        val nodeType = draggingNodeType
        draggingNodeType = null
        dragPosition = Offset.Zero
        dragStartPosition = Offset.Zero
        return nodeType
    }

    /**
     * Cancels the current drag operation without dropping
     */
    fun cancelDrag() {
        draggingNodeType = null
        dragPosition = Offset.Zero
        dragStartPosition = Offset.Zero
    }
}

/**
 * Creates a remembered DragAndDropState instance
 *
 * @return A remembered DragAndDropState
 */
@Composable
fun rememberDragAndDropState(): DragAndDropState {
    return remember { DragAndDropState() }
}

/**
 * Modifier that makes a composable draggable as a node type source
 *
 * @param nodeType The node type this composable represents
 * @param dragAndDropState The drag-and-drop state to update
 * @param onDragStart Callback when drag starts (optional)
 * @param onDragEnd Callback when drag ends (optional)
 */
fun Modifier.draggableNodeType(
    nodeType: NodeTypeDefinition,
    dragAndDropState: DragAndDropState,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {}
): Modifier = this.pointerInput(nodeType) {
    detectDragGesturesAfterLongPress(
        onDragStart = { offset ->
            dragAndDropState.startDrag(nodeType, offset)
            onDragStart()
        },
        onDrag = { change, _ ->
            dragAndDropState.updateDragPosition(change.position)
        },
        onDragEnd = {
            dragAndDropState.endDrag()
            onDragEnd()
        },
        onDragCancel = {
            dragAndDropState.cancelDrag()
            onDragEnd()
        }
    )
}

/**
 * Modifier that makes a canvas accept dropped node types
 *
 * @param dragAndDropState The drag-and-drop state to monitor
 * @param graphState The graph state to update when nodes are dropped
 * @param panOffset Current canvas pan offset (for coordinate transformation)
 * @param scale Current canvas zoom scale (for coordinate transformation)
 * @param onNodeDropped Callback when a node is successfully dropped (optional)
 */
fun Modifier.nodeDropTarget(
    dragAndDropState: DragAndDropState,
    graphState: GraphState,
    panOffset: Offset,
    scale: Float,
    onNodeDropped: (NodeTypeDefinition, Offset) -> Unit = { _, _ -> }
): Modifier = this.pointerInput(Unit) {
    detectTapGestures(
        onPress = { offset ->
            // Check if there's a drag in progress when pointer is released
            if (dragAndDropState.isDragging) {
                val nodeType = dragAndDropState.endDrag()
                if (nodeType != null) {
                    // Convert screen coordinates to canvas coordinates
                    val canvasPosition = screenToCanvas(offset, panOffset, scale)

                    // Create a new node instance from the node type
                    val newNode = createNodeFromType(nodeType, canvasPosition)

                    // Add the node to the graph
                    graphState.addNode(newNode, canvasPosition)

                    // Notify callback
                    onNodeDropped(nodeType, canvasPosition)
                }
            }
        }
    )
}

/**
 * Converts screen coordinates to canvas coordinates
 *
 * @param screenPosition Position in screen coordinates
 * @param panOffset Current canvas pan offset
 * @param scale Current canvas zoom scale
 * @return Position in canvas coordinates
 */
fun screenToCanvas(screenPosition: Offset, panOffset: Offset, scale: Float): Offset {
    return Offset(
        x = (screenPosition.x - panOffset.x) / scale,
        y = (screenPosition.y - panOffset.y) / scale
    )
}

/**
 * Converts canvas coordinates to screen coordinates
 *
 * @param canvasPosition Position in canvas coordinates
 * @param panOffset Current canvas pan offset
 * @param scale Current canvas zoom scale
 * @return Position in screen coordinates
 */
fun canvasToScreen(canvasPosition: Offset, panOffset: Offset, scale: Float): Offset {
    return Offset(
        x = canvasPosition.x * scale + panOffset.x,
        y = canvasPosition.y * scale + panOffset.y
    )
}

/**
 * Creates a new node instance from a node type definition
 *
 * @param nodeType The node type definition
 * @param position The initial position for the node
 * @return A new CodeNode instance
 */
fun createNodeFromType(nodeType: NodeTypeDefinition, position: Offset): CodeNode {
    // Generate unique ID
    val nodeId = "node_${System.currentTimeMillis()}_${(0..9999).random()}"

    // Map node category to CodeNodeType
    val codeNodeType = when (nodeType.category) {
        NodeTypeDefinition.NodeCategory.UI_COMPONENT -> CodeNodeType.GENERATOR // UI generates events/data
        NodeTypeDefinition.NodeCategory.SERVICE -> CodeNodeType.TRANSFORMER // Services transform data
        NodeTypeDefinition.NodeCategory.TRANSFORMER -> CodeNodeType.TRANSFORMER
        NodeTypeDefinition.NodeCategory.VALIDATOR -> CodeNodeType.VALIDATOR
        NodeTypeDefinition.NodeCategory.API_ENDPOINT -> CodeNodeType.API_ENDPOINT
        NodeTypeDefinition.NodeCategory.DATABASE -> CodeNodeType.DATABASE
        NodeTypeDefinition.NodeCategory.GENERIC -> CodeNodeType.TRANSFORMER // Generic nodes use flexible transformer type
    }

    // Create input ports from templates
    val inputPorts = nodeType.getInputPortTemplates().map { template ->
        Port<Any>(
            id = "${nodeId}_input_${template.name}",
            name = template.name,
            direction = Port.Direction.INPUT,
            dataType = template.dataType as kotlin.reflect.KClass<Any>,
            required = template.required,
            defaultValue = null,
            validationRules = emptyList(),
            owningNodeId = nodeId
        )
    }

    // Create output ports from templates
    val outputPorts = nodeType.getOutputPortTemplates().map { template ->
        Port<Any>(
            id = "${nodeId}_output_${template.name}",
            name = template.name,
            direction = Port.Direction.OUTPUT,
            dataType = template.dataType as kotlin.reflect.KClass<Any>,
            required = template.required,
            defaultValue = null,
            validationRules = emptyList(),
            owningNodeId = nodeId
        )
    }

    // Create the CodeNode
    return CodeNode(
        id = nodeId,
        name = nodeType.name,
        codeNodeType = codeNodeType,
        description = nodeType.description,
        position = Node.Position(position.x.toDouble(), position.y.toDouble()),
        inputPorts = inputPorts,
        outputPorts = outputPorts,
        configuration = emptyMap(),
        parentNodeId = null,
        executionState = ExecutionState.IDLE,
        coroutineHandle = null,
        processingLogic = null // No processing logic yet - will be added later
    )
}

/**
 * Helper extension to check if a position is within a rectangular area
 *
 * @param topLeft Top-left corner of the rectangle
 * @param size Size of the rectangle (width and height)
 * @return true if this position is inside the rectangle
 */
fun Offset.isInRect(topLeft: Offset, size: androidx.compose.ui.geometry.Size): Boolean {
    return x >= topLeft.x && x <= topLeft.x + size.width &&
           y >= topLeft.y && y <= topLeft.y + size.height
}

/**
 * Calculates the distance between two points
 *
 * @param other The other point
 * @return Distance in pixels
 */
fun Offset.distanceTo(other: Offset): Float {
    val dx = x - other.x
    val dy = y - other.y
    return kotlin.math.sqrt(dx * dx + dy * dy)
}
