/*
 * FlowGraphCanvas - Main Canvas for Visual Flow Graph Editing
 * Provides pan, zoom, and node rendering capabilities
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Rect
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.Port
import io.codenode.grapheditor.state.SelectableElement

/**
 * Main canvas component for rendering and interacting with flow graphs
 *
 * @param flowGraph The flow graph to display
 * @param selectedNodeId ID of the currently selected node (if any)
 * @param selectedConnectionIds Set of selected connection IDs (for multi-select support)
 * @param multiSelectedNodeIds Set of node IDs in multi-selection (Shift-click selection)
 * @param connectionColors Map of connection IDs to their display colors (for IP type coloring)
 * @param scale Current zoom scale (1.0 = 100%)
 * @param panOffset Current pan offset for scrolling the view
 * @param onScaleChanged Callback when zoom scale changes from user interaction
 * @param onPanOffsetChanged Callback when pan offset changes from user interaction
 * @param onNodeSelected Callback when a node is selected
 * @param onConnectionSelected Callback when a connection is selected
 * @param onElementShiftClicked Callback when any element is Shift-clicked (for multi-selection toggle)
 * @param onEmptyCanvasClicked Callback when empty canvas is clicked (for clearing selection)
 * @param onConnectionRightClick Callback when a connection is right-clicked: (connectionId, screenPosition)
 * @param onNodeMoved Callback when a node is dragged to a new position
 * @param onConnectionCreated Callback when a new connection is created
 * @param selectionBoxBounds Current selection box bounds for rendering (null if not active)
 * @param onRectangularSelectionStart Callback when Shift+drag starts on empty canvas
 * @param onRectangularSelectionUpdate Callback during Shift+drag to update selection box
 * @param onRectangularSelectionFinish Callback when Shift+drag ends
 * @param modifier Modifier for the canvas
 */
@Composable
fun FlowGraphCanvas(
    flowGraph: FlowGraph,
    selectedNodeId: String? = null,
    selectedConnectionIds: Set<String> = emptySet(),
    multiSelectedNodeIds: Set<String> = emptySet(),
    connectionColors: Map<String, Color> = emptyMap(),
    scale: Float = 1f,
    panOffset: Offset = Offset.Zero,
    onScaleChanged: (Float) -> Unit = {},
    onPanOffsetChanged: (Offset) -> Unit = {},
    onNodeSelected: (String?) -> Unit = {},
    onConnectionSelected: (String?) -> Unit = {},
    onElementShiftClicked: (SelectableElement) -> Unit = {},
    onEmptyCanvasClicked: () -> Unit = {},
    onConnectionRightClick: (String, Offset) -> Unit = { _, _ -> },
    onNodeMoved: (nodeId: String, newX: Double, newY: Double) -> Unit = { _, _, _ -> },
    onConnectionCreated: (Connection) -> Unit = {},
    selectionBoxBounds: Rect? = null,
    onRectangularSelectionStart: (Offset) -> Unit = {},
    onRectangularSelectionUpdate: (Offset) -> Unit = {},
    onRectangularSelectionFinish: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Drag state (local only)
    var draggingNode by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // Connection creation state
    var creatingConnection by remember { mutableStateOf(false) }
    var connectionSourceNode by remember { mutableStateOf<String?>(null) }
    var connectionSourcePort by remember { mutableStateOf<String?>(null) }
    var connectionEndPosition by remember { mutableStateOf(Offset.Zero) }

    // Track Shift state at pointer down for use in drag handler
    var shiftPressedAtPointerDown by remember { mutableStateOf(false) }

    // Rectangular selection state
    var isRectangularSelectionActive by remember { mutableStateOf(false) }

    // Create a stable reference to current state values that gesture detectors can read
    val currentFlowGraph = remember { mutableStateOf(flowGraph) }
    val currentPanOffset = remember { mutableStateOf(panOffset) }
    val currentScale = remember { mutableStateOf(scale) }

    // Keep the references updated
    currentFlowGraph.value = flowGraph
    currentPanOffset.value = panOffset
    currentScale.value = scale

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .pointerInput(Unit) {
                // Handle right-click for connection context menu
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press) {
                            val isRightClick = event.buttons.isSecondaryPressed
                            if (isRightClick) {
                                val position = event.changes.first().position

                                // Check if right-clicking on a connection
                                val connectionUnderPointer = findConnectionAtPosition(
                                    currentFlowGraph.value,
                                    position,
                                    currentPanOffset.value,
                                    currentScale.value
                                )
                                if (connectionUnderPointer != null) {
                                    onConnectionRightClick(connectionUnderPointer, position)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                // Handle immediate pointer down for connection selection
                // This fires before detectDragGestures which waits for movement
                // Skip if Shift is pressed - let the tap handler deal with multi-selection
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) {
                            val position = event.changes.first().position
                            val isShiftPressed = event.keyboardModifiers.isShiftPressed

                            // Track Shift state for the drag handler
                            shiftPressedAtPointerDown = isShiftPressed

                            // Skip immediate selection if Shift is pressed
                            if (isShiftPressed) continue

                            // Check if clicking on a connection (but not on a node or port)
                            val portInfo = findPortAtPosition(
                                currentFlowGraph.value,
                                position,
                                currentPanOffset.value,
                                currentScale.value
                            )
                            val nodeUnderPointer = findNodeAtPosition(
                                currentFlowGraph.value,
                                position,
                                currentPanOffset.value,
                                currentScale.value
                            )

                            if (portInfo == null && nodeUnderPointer == null) {
                                val connectionUnderPointer = findConnectionAtPosition(
                                    currentFlowGraph.value,
                                    position,
                                    currentPanOffset.value,
                                    currentScale.value
                                )
                                if (connectionUnderPointer != null) {
                                    // Select connection immediately (normal click only)
                                    onConnectionSelected(connectionUnderPointer)
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                // Handle dragging - use Unit key so it never recreates
                detectDragGestures(
                    onDragStart = { offset ->
                        // Check if we're starting a connection from an output port
                        val portInfo = findPortAtPosition(
                            currentFlowGraph.value,
                            offset,
                            currentPanOffset.value,
                            currentScale.value
                        )

                        if (portInfo != null && portInfo.direction == Port.Direction.OUTPUT) {
                            // Starting a connection from an output port
                            creatingConnection = true
                            connectionSourceNode = portInfo.nodeId
                            connectionSourcePort = portInfo.portId
                            connectionEndPosition = offset
                        } else {
                            // Check if clicking on a node for dragging
                            val nodeUnderPointer = findNodeAtPosition(
                                currentFlowGraph.value,
                                offset,
                                currentPanOffset.value,
                                currentScale.value
                            )
                            if (nodeUnderPointer != null) {
                                draggingNode = nodeUnderPointer.id
                                dragOffset = Offset.Zero
                            } else {
                                // Check if clicking on a connection
                                val connectionUnderPointer = findConnectionAtPosition(
                                    currentFlowGraph.value,
                                    offset,
                                    currentPanOffset.value,
                                    currentScale.value
                                )
                                if (connectionUnderPointer != null) {
                                    // Only do single-selection if Shift was NOT pressed
                                    // Shift-click will be handled by the tap handler for multi-selection
                                    if (!shiftPressedAtPointerDown) {
                                        onConnectionSelected(connectionUnderPointer)
                                    }
                                    draggingNode = "__connection__"  // Prevent panning
                                } else if (shiftPressedAtPointerDown) {
                                    // Shift+drag on empty canvas: start rectangular selection
                                    isRectangularSelectionActive = true
                                    onRectangularSelectionStart(offset)
                                    draggingNode = "__rectangular_selection__"
                                } else {
                                    draggingNode = null
                                }
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (creatingConnection) {
                            // Update connection end position
                            connectionEndPosition += dragAmount
                        } else if (draggingNode == "__rectangular_selection__") {
                            // Update rectangular selection box
                            onRectangularSelectionUpdate(change.position)
                        } else if (draggingNode == null) {
                            // Pan the canvas - notify parent of new offset
                            onPanOffsetChanged(currentPanOffset.value + dragAmount)
                        } else if (draggingNode == "__connection__") {
                            // On a connection - don't pan or drag, just consume the event
                            // Selection will be handled by detectTapGestures
                        } else {
                            // Drag the node
                            dragOffset += dragAmount
                        }
                    },
                    onDragEnd = {
                        if (creatingConnection) {
                            // Check if we ended on an input port
                            val targetPortInfo = findPortAtPosition(
                                currentFlowGraph.value,
                                connectionEndPosition,
                                currentPanOffset.value,
                                currentScale.value
                            )

                            if (targetPortInfo != null &&
                                targetPortInfo.direction == Port.Direction.INPUT &&
                                targetPortInfo.nodeId != connectionSourceNode) {
                                // Create the connection
                                val connection = Connection(
                                    id = "conn_${System.currentTimeMillis()}",
                                    sourceNodeId = connectionSourceNode!!,
                                    sourcePortId = connectionSourcePort!!,
                                    targetNodeId = targetPortInfo.nodeId,
                                    targetPortId = targetPortInfo.portId
                                )
                                onConnectionCreated(connection)
                            }

                            // Reset connection creation state
                            creatingConnection = false
                            connectionSourceNode = null
                            connectionSourcePort = null
                        } else if (draggingNode == "__rectangular_selection__") {
                            // Finish rectangular selection and select enclosed nodes
                            isRectangularSelectionActive = false
                            onRectangularSelectionFinish()
                        } else if (draggingNode == "__connection__") {
                            // Connection was already selected in onDragStart, nothing to do
                        } else if (draggingNode != null) {
                            val node = currentFlowGraph.value.findNode(draggingNode!!)
                            if (node != null) {
                                val newX = node.position.x + dragOffset.x / currentScale.value
                                val newY = node.position.y + dragOffset.y / currentScale.value
                                onNodeMoved(draggingNode!!, newX, newY)
                            }
                            // Only do single-selection if Shift was NOT pressed
                            // Shift-click selection is handled by the tap handler
                            if (!shiftPressedAtPointerDown) {
                                onNodeSelected(draggingNode)
                                onConnectionSelected(null)  // Clear connection selection
                            }
                        }
                        draggingNode = null
                        dragOffset = Offset.Zero
                    },
                    onDragCancel = {
                        draggingNode = null
                        dragOffset = Offset.Zero
                        creatingConnection = false
                        connectionSourceNode = null
                        connectionSourcePort = null
                        isRectangularSelectionActive = false
                    }
                )
            }
            .pointerInput(Unit) {
                // Handle taps for quick selection with Shift key detection
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Release) {
                            val change = event.changes.firstOrNull() ?: continue

                            // Check if this was a tap (no significant movement)
                            if (change.previousPressed && !change.pressed) {
                                val offset = change.position
                                val isShiftPressed = event.keyboardModifiers.isShiftPressed

                                // Priority: Node > Connection > Empty
                                val tappedNode = findNodeAtPosition(
                                    currentFlowGraph.value,
                                    offset,
                                    currentPanOffset.value,
                                    currentScale.value
                                )

                                if (tappedNode != null) {
                                    if (isShiftPressed) {
                                        // Shift-click: toggle element in selection
                                        onElementShiftClicked(SelectableElement.Node(tappedNode.id))
                                    } else {
                                        // Normal click: single selection
                                        onNodeSelected(tappedNode.id)
                                        onConnectionSelected(null)
                                    }
                                } else {
                                    val tappedConnection = findConnectionAtPosition(
                                        currentFlowGraph.value,
                                        offset,
                                        currentPanOffset.value,
                                        currentScale.value
                                    )

                                    if (tappedConnection != null) {
                                        if (isShiftPressed) {
                                            // Shift-click: toggle element in selection
                                            onElementShiftClicked(SelectableElement.Connection(tappedConnection))
                                        }
                                        // Normal click: connection was already selected on pointer down
                                    } else {
                                        // Empty canvas tap
                                        if (!isShiftPressed) {
                                            // Without Shift: clear all selections
                                            onEmptyCanvasClicked()
                                            onNodeSelected(null)
                                            onConnectionSelected(null)
                                        }
                                        // With Shift on empty canvas: do nothing (preserve selection)
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // Draw grid background
            drawGrid(panOffset, scale)

            // Apply pan and scale transformations
            val transformedOffset = panOffset
            val transformedScale = scale

            // Draw connections first (behind nodes)
            flowGraph.connections.forEach { connection ->
                drawConnection(
                    connection = connection,
                    nodes = flowGraph.rootNodes,
                    offset = transformedOffset,
                    scale = transformedScale,
                    draggingNodeId = draggingNode,
                    dragOffset = dragOffset,
                    isSelected = connection.id in selectedConnectionIds,
                    ipTypeColor = connectionColors[connection.id]
                )
            }

            // Draw pending connection line if creating a connection
            if (creatingConnection && connectionSourceNode != null && connectionSourcePort != null) {
                val sourceNode = flowGraph.findNode(connectionSourceNode!!)
                if (sourceNode != null) {
                    val sourcePort = sourceNode.outputPorts.find { it.id == connectionSourcePort }
                    if (sourcePort != null) {
                        val sourcePortIndex = sourceNode.outputPorts.indexOf(sourcePort)
                        val portSpacing = 25f * scale
                        val nodeWidth = 180f * scale
                        val headerHeight = 30f * scale

                        // Convert graph coordinates to screen coordinates
                        val sourceNodePos = Offset(
                            sourceNode.position.x.toFloat() * scale + panOffset.x,
                            sourceNode.position.y.toFloat() * scale + panOffset.y
                        )

                        val sourcePortPos = Offset(
                            sourceNodePos.x + nodeWidth,
                            sourceNodePos.y + headerHeight + 20f * scale + (sourcePortIndex * portSpacing)
                        )

                        // Draw dashed Bezier curve from source port to current mouse position
                        drawBezierConnection(
                            start = sourcePortPos,
                            end = connectionEndPosition,
                            scale = scale,
                            color = Color(0xFF2196F3),
                            strokeWidth = 3f,
                            isDashed = true
                        )

                        // Draw circle at the end position
                        drawCircle(
                            color = Color(0xFF2196F3),
                            radius = 5f * scale,
                            center = connectionEndPosition
                        )
                    }
                }
            }

            // Draw nodes
            flowGraph.rootNodes.forEach { node ->
                // Node is selected if it's the single selection OR in multi-selection
                val isSelected = node.id == selectedNodeId || node.id in multiSelectedNodeIds
                val isDragging = node.id == draggingNode
                val isMultiSelected = node.id in multiSelectedNodeIds

                // Convert graph coordinates to screen coordinates: screenPos = graphPos * scale + panOffset
                val screenPosition = Offset(
                    node.position.x.toFloat() * transformedScale + transformedOffset.x,
                    node.position.y.toFloat() * transformedScale + transformedOffset.y
                )

                // Add drag offset (already in screen pixels) if dragging
                val finalPosition = if (isDragging) {
                    screenPosition + dragOffset
                } else {
                    screenPosition
                }

                drawNode(
                    node = node,
                    position = finalPosition,
                    scale = transformedScale,
                    isMultiSelected = isMultiSelected,
                    isSelected = isSelected,
                    isDragging = isDragging
                )
            }
        }

        // Draw selection box overlay if active
        if (selectionBoxBounds != null) {
            SelectionBox(bounds = selectionBoxBounds)
        }
    }
}

/**
 * Draws a Bezier curve connection between two points
 * Creates a smooth S-curve suitable for flow graph connections
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBezierConnection(
    start: Offset,
    end: Offset,
    scale: Float,
    color: Color = Color(0xFF2196F3),
    strokeWidth: Float = 3f,
    isDashed: Boolean = false
) {
    val path = Path().apply {
        moveTo(start.x, start.y)

        // Calculate control points for horizontal Bezier curve
        // The distance between start and end determines how far out the control points extend
        val dx = end.x - start.x
        val controlPointOffset = kotlin.math.abs(dx) * 0.5f

        // Control point 1: extends to the right from start point
        val cp1 = Offset(start.x + controlPointOffset, start.y)

        // Control point 2: extends to the left from end point
        val cp2 = Offset(end.x - controlPointOffset, end.y)

        // Draw cubic Bezier curve
        cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, end.x, end.y)
    }

    val pathEffect = if (isDashed) {
        androidx.compose.ui.graphics.PathEffect.dashPathEffect(
            floatArrayOf(10f * scale, 5f * scale)
        )
    } else {
        null
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth * scale, pathEffect = pathEffect)
    )
}

/**
 * Draws a grid pattern on the canvas background
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(
    offset: Offset,
    scale: Float
) {
    val gridSize = 20f * scale
    val gridColor = Color(0xFFE0E0E0)

    // Draw vertical lines
    var x = offset.x % gridSize
    while (x < size.width) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        x += gridSize
    }

    // Draw horizontal lines
    var y = offset.y % gridSize
    while (y < size.height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += gridSize
    }
}

/**
 * Draws a connection line between two nodes as a Bezier curve
 *
 * @param connection The connection to draw
 * @param nodes List of all nodes for position lookup
 * @param offset Canvas pan offset
 * @param scale Current zoom scale
 * @param draggingNodeId ID of node being dragged (if any)
 * @param dragOffset Current drag offset
 * @param isSelected Whether the connection is selected
 * @param ipTypeColor Optional color based on IP type
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConnection(
    connection: Connection,
    nodes: List<Node>,
    offset: Offset,
    scale: Float,
    draggingNodeId: String? = null,
    dragOffset: Offset = Offset.Zero,
    isSelected: Boolean = false,
    ipTypeColor: Color? = null
) {
    val sourceNode = nodes.find { it.id == connection.sourceNodeId } ?: return
    val targetNode = nodes.find { it.id == connection.targetNodeId } ?: return

    // Find the actual source and target ports
    val sourcePort = sourceNode.outputPorts.find { it.id == connection.sourcePortId } ?: return
    val targetPort = targetNode.inputPorts.find { it.id == connection.targetPortId } ?: return

    // Calculate port positions
    val portSpacing = 25f * scale
    val nodeWidth = 180f * scale
    val headerHeight = 30f * scale

    // Convert graph coordinates to screen coordinates: screenPos = graphPos * scale + offset
    // Apply drag offset (in screen pixels) if this node is being dragged
    val sourceNodePos = Offset(
        sourceNode.position.x.toFloat() * scale + offset.x,
        sourceNode.position.y.toFloat() * scale + offset.y
    ) + if (sourceNode.id == draggingNodeId) dragOffset else Offset.Zero

    val targetNodePos = Offset(
        targetNode.position.x.toFloat() * scale + offset.x,
        targetNode.position.y.toFloat() * scale + offset.y
    ) + if (targetNode.id == draggingNodeId) dragOffset else Offset.Zero

    val sourcePortIndex = sourceNode.outputPorts.indexOf(sourcePort)
    val targetPortIndex = targetNode.inputPorts.indexOf(targetPort)

    // Calculate exact port positions
    val sourcePos = Offset(
        sourceNodePos.x + nodeWidth,  // Right side of source node
        sourceNodePos.y + headerHeight + 20f * scale + (sourcePortIndex * portSpacing)
    )

    val targetPos = Offset(
        targetNodePos.x,  // Left side of target node
        targetNodePos.y + headerHeight + 20f * scale + (targetPortIndex * portSpacing)
    )

    // Use IP type color if provided, otherwise default to dark gray (for "Any" type)
    // Selected connections use blue highlighting
    val baseColor = ipTypeColor ?: Color(0xFF424242)  // IP type color or dark gray for default/Any
    val selectedColor = Color(0xFF2196F3)  // Blue for selection

    val strokeColor = if (isSelected) selectedColor else baseColor
    val strokeWidth = if (isSelected) 4f else 3f

    // Draw selection glow first (underneath) for selected connections
    if (isSelected) {
        drawBezierConnection(
            start = sourcePos,
            end = targetPos,
            scale = scale,
            color = selectedColor.copy(alpha = 0.3f),
            strokeWidth = 8f
        )
    }

    // Draw main connection line
    drawBezierConnection(sourcePos, targetPos, scale, strokeColor, strokeWidth)
}

/**
 * Draws a node on the canvas with ports
 *
 * @param node The node to draw
 * @param position Canvas position (top-left corner)
 * @param scale Current zoom scale
 * @param isSelected Whether the node is selected (single or multi)
 * @param isDragging Whether the node is being dragged
 * @param isMultiSelected Whether the node is part of a multi-selection
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNode(
    node: Node,
    position: Offset,
    scale: Float,
    isSelected: Boolean,
    isDragging: Boolean,
    isMultiSelected: Boolean = false
) {
    val portSpacing = 25f * scale
    val portRadius = 6f * scale
    val nodeWidth = 180f * scale
    val headerHeight = 30f * scale

    // Calculate node height based on number of ports
    val maxPorts = maxOf(node.inputPorts.size, node.outputPorts.size)
    val nodeHeight = headerHeight + (maxPorts.coerceAtLeast(1) * portSpacing) + 20f * scale

    val nodeColor = when {
        isDragging -> Color(0xFFBBDEFB)
        isMultiSelected -> Color(0xFFE3F2FD)  // Light blue for multi-selection
        isSelected -> Color(0xFF64B5F6)
        else -> Color.White
    }

    val borderColor = when {
        isMultiSelected -> Color(0xFF1976D2)  // Darker blue for multi-selection
        isSelected -> Color(0xFF2196F3)
        else -> Color(0xFF9E9E9E)
    }

    val borderWidth = if (isMultiSelected) 3f * scale else 2f * scale

    // Draw selection glow for multi-selected nodes
    if (isMultiSelected) {
        drawRect(
            color = Color(0xFF2196F3).copy(alpha = 0.2f),
            topLeft = Offset(position.x - 4f * scale, position.y - 4f * scale),
            size = androidx.compose.ui.geometry.Size(nodeWidth + 8f * scale, nodeHeight + 8f * scale)
        )
    }

    // Draw main node body
    drawRect(
        color = nodeColor,
        topLeft = position,
        size = androidx.compose.ui.geometry.Size(nodeWidth, nodeHeight)
    )

    // Draw border
    drawRect(
        color = borderColor,
        topLeft = position,
        size = androidx.compose.ui.geometry.Size(nodeWidth, nodeHeight),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = borderWidth)
    )

    // Draw header background
    drawRect(
        color = Color(0xFFE3F2FD),
        topLeft = position,
        size = androidx.compose.ui.geometry.Size(nodeWidth, headerHeight)
    )

    // Draw header separator line
    drawLine(
        color = borderColor,
        start = Offset(position.x, position.y + headerHeight),
        end = Offset(position.x + nodeWidth, position.y + headerHeight),
        strokeWidth = 1f * scale
    )

    // Draw node name in header (simplified - just drawing a rectangle placeholder for text)
    // In a real implementation, we'd use TextMeasurer or Compose UI Text

    // Draw input ports on the left
    node.inputPorts.forEachIndexed { index, port ->
        val portY = position.y + headerHeight + 20f * scale + (index * portSpacing)
        val portX = position.x

        // Draw port circle
        drawCircle(
            color = Color(0xFF4CAF50),
            radius = portRadius,
            center = Offset(portX, portY)
        )

        // Draw port circle border
        drawCircle(
            color = Color(0xFF2E7D32),
            radius = portRadius,
            center = Offset(portX, portY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f * scale)
        )

        // Draw port name indicator (small line extending into the node)
        drawLine(
            color = Color(0xFF757575),
            start = Offset(portX + portRadius + 2f * scale, portY),
            end = Offset(portX + 30f * scale, portY),
            strokeWidth = 1f * scale
        )
    }

    // Draw output ports on the right
    node.outputPorts.forEachIndexed { index, port ->
        val portY = position.y + headerHeight + 20f * scale + (index * portSpacing)
        val portX = position.x + nodeWidth

        // Draw port circle
        drawCircle(
            color = Color(0xFF2196F3),
            radius = portRadius,
            center = Offset(portX, portY)
        )

        // Draw port circle border
        drawCircle(
            color = Color(0xFF1565C0),
            radius = portRadius,
            center = Offset(portX, portY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f * scale)
        )

        // Draw port name indicator (small line extending into the node)
        drawLine(
            color = Color(0xFF757575),
            start = Offset(portX - portRadius - 2f * scale, portY),
            end = Offset(portX - 30f * scale, portY),
            strokeWidth = 1f * scale
        )
    }
}

/**
 * Finds a port at the given canvas position
 */
private fun findPortAtPosition(
    flowGraph: FlowGraph,
    tapPosition: Offset,
    panOffset: Offset,
    scale: Float
): PortInfo? {
    val portRadius = 6f * scale
    val clickRadius = 12f * scale // Larger hit area for easier clicking

    flowGraph.rootNodes.forEach { node ->
        val portSpacing = 25f * scale
        val nodeWidth = 180f * scale
        val headerHeight = 30f * scale
        // Convert graph coordinates to screen coordinates: screenPos = graphPos * scale + panOffset
        val nodePos = Offset(
            node.position.x.toFloat() * scale + panOffset.x,
            node.position.y.toFloat() * scale + panOffset.y
        )

        // Check input ports (left side)
        node.inputPorts.forEachIndexed { index, port ->
            val portY = nodePos.y + headerHeight + 20f * scale + (index * portSpacing)
            val portX = nodePos.x
            val portCenter = Offset(portX, portY)

            if ((tapPosition - portCenter).getDistance() <= clickRadius) {
                return PortInfo(node.id, port.id, portCenter, Port.Direction.INPUT)
            }
        }

        // Check output ports (right side)
        node.outputPorts.forEachIndexed { index, port ->
            val portY = nodePos.y + headerHeight + 20f * scale + (index * portSpacing)
            val portX = nodePos.x + nodeWidth
            val portCenter = Offset(portX, portY)

            if ((tapPosition - portCenter).getDistance() <= clickRadius) {
                return PortInfo(node.id, port.id, portCenter, Port.Direction.OUTPUT)
            }
        }
    }

    return null
}

/**
 * Finds a node at the given canvas position
 */
private fun findNodeAtPosition(
    flowGraph: FlowGraph,
    tapPosition: Offset,
    panOffset: Offset,
    scale: Float
): Node? {
    return flowGraph.rootNodes.findLast { node ->
        // Calculate node dimensions (same as drawNode)
        val portSpacing = 25f * scale
        val nodeWidth = 180f * scale
        val headerHeight = 30f * scale
        val maxPorts = maxOf(node.inputPorts.size, node.outputPorts.size)
        val nodeHeight = headerHeight + (maxPorts.coerceAtLeast(1) * portSpacing) + 20f * scale

        // Convert graph coordinates to screen coordinates: screenPos = graphPos * scale + panOffset
        val nodePos = Offset(
            node.position.x.toFloat() * scale + panOffset.x,
            node.position.y.toFloat() * scale + panOffset.y
        )

        tapPosition.x >= nodePos.x &&
        tapPosition.x <= nodePos.x + nodeWidth &&
        tapPosition.y >= nodePos.y &&
        tapPosition.y <= nodePos.y + nodeHeight
    }
}

/**
 * Computes a point on a cubic Bezier curve at parameter t
 */
private fun cubicBezier(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val u = 1 - t
    return Offset(
        u*u*u*p0.x + 3*u*u*t*p1.x + 3*u*t*t*p2.x + t*t*t*p3.x,
        u*u*u*p0.y + 3*u*u*t*p1.y + 3*u*t*t*p2.y + t*t*t*p3.y
    )
}

/**
 * Samples a cubic Bezier curve at evenly spaced t values
 */
private fun sampleBezierCurve(start: Offset, end: Offset, samples: Int = 20): List<Offset> {
    val controlPointOffset = kotlin.math.abs(end.x - start.x) * 0.5f
    val cp1 = Offset(start.x + controlPointOffset, start.y)
    val cp2 = Offset(end.x - controlPointOffset, end.y)

    return (0 until samples).map { i ->
        val t = i.toFloat() / (samples - 1)
        cubicBezier(start, cp1, cp2, end, t)
    }
}

/**
 * Finds a connection at the given screen position
 * Returns connection ID or null, uses 8-pixel tolerance
 */
private fun findConnectionAtPosition(
    flowGraph: FlowGraph,
    position: Offset,
    panOffset: Offset,
    scale: Float,
    tolerance: Float = 8f
): String? {
    val portSpacing = 25f * scale
    val nodeWidth = 180f * scale
    val headerHeight = 30f * scale

    for (connection in flowGraph.connections) {
        val sourceNode = flowGraph.findNode(connection.sourceNodeId) ?: continue
        val targetNode = flowGraph.findNode(connection.targetNodeId) ?: continue

        val sourcePort = sourceNode.outputPorts.find { it.id == connection.sourcePortId } ?: continue
        val targetPort = targetNode.inputPorts.find { it.id == connection.targetPortId } ?: continue

        // Calculate screen positions (same logic as drawConnection)
        val sourceNodePos = Offset(
            sourceNode.position.x.toFloat() * scale + panOffset.x,
            sourceNode.position.y.toFloat() * scale + panOffset.y
        )
        val targetNodePos = Offset(
            targetNode.position.x.toFloat() * scale + panOffset.x,
            targetNode.position.y.toFloat() * scale + panOffset.y
        )

        val sourcePortIndex = sourceNode.outputPorts.indexOf(sourcePort)
        val targetPortIndex = targetNode.inputPorts.indexOf(targetPort)

        val sourcePos = Offset(
            sourceNodePos.x + nodeWidth,
            sourceNodePos.y + headerHeight + 20f * scale + (sourcePortIndex * portSpacing)
        )
        val targetPos = Offset(
            targetNodePos.x,
            targetNodePos.y + headerHeight + 20f * scale + (targetPortIndex * portSpacing)
        )

        // Sample the Bezier curve and check distance to click point
        val samples = sampleBezierCurve(sourcePos, targetPos)
        val minDistance = samples.minOfOrNull { sample ->
            (position - sample).getDistance()
        } ?: Float.MAX_VALUE

        if (minDistance <= tolerance * scale) {
            return connection.id
        }
    }

    return null
}
