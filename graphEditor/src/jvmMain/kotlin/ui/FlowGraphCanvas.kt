/*
 * FlowGraphCanvas - Main Canvas for Visual Flow Graph Editing
 * Provides pan, zoom, and node rendering capabilities
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.Port

/**
 * Main canvas component for rendering and interacting with flow graphs
 *
 * @param flowGraph The flow graph to display
 * @param selectedNodeId ID of the currently selected node (if any)
 * @param scale Current zoom scale (1.0 = 100%)
 * @param panOffset Current pan offset for scrolling the view
 * @param onScaleChanged Callback when zoom scale changes from user interaction
 * @param onPanOffsetChanged Callback when pan offset changes from user interaction
 * @param onNodeSelected Callback when a node is selected
 * @param onNodeMoved Callback when a node is dragged to a new position
 * @param onConnectionCreated Callback when a new connection is created
 * @param modifier Modifier for the canvas
 */
@Composable
fun FlowGraphCanvas(
    flowGraph: FlowGraph,
    selectedNodeId: String? = null,
    scale: Float = 1f,
    panOffset: Offset = Offset.Zero,
    onScaleChanged: (Float) -> Unit = {},
    onPanOffsetChanged: (Offset) -> Unit = {},
    onNodeSelected: (String?) -> Unit = {},
    onNodeMoved: (nodeId: String, newX: Double, newY: Double) -> Unit = { _, _, _ -> },
    onConnectionCreated: (Connection) -> Unit = {},
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
                                draggingNode = null
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (creatingConnection) {
                            // Update connection end position
                            connectionEndPosition += dragAmount
                        } else if (draggingNode == null) {
                            // Pan the canvas - notify parent of new offset
                            onPanOffsetChanged(currentPanOffset.value + dragAmount)
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
                        } else if (draggingNode != null) {
                            val node = currentFlowGraph.value.findNode(draggingNode!!)
                            if (node != null) {
                                val newX = node.position.x + dragOffset.x / currentScale.value
                                val newY = node.position.y + dragOffset.y / currentScale.value
                                onNodeMoved(draggingNode!!, newX, newY)
                            }
                            onNodeSelected(draggingNode)
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
                    }
                )
            }
            .pointerInput(Unit) {
                // Handle taps for quick selection - use Unit key so it never recreates
                detectTapGestures(
                    onTap = { offset ->
                        // Read current values from the stable references
                        val tappedNode = findNodeAtPosition(
                            currentFlowGraph.value,
                            offset,
                            currentPanOffset.value,
                            currentScale.value
                        )
                        onNodeSelected(tappedNode?.id)
                    }
                )
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
                    dragOffset = dragOffset
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
                val isSelected = node.id == selectedNodeId
                val isDragging = node.id == draggingNode

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
                    isSelected = isSelected,
                    isDragging = isDragging
                )
            }
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
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConnection(
    connection: Connection,
    nodes: List<Node>,
    offset: Offset,
    scale: Float,
    draggingNodeId: String? = null,
    dragOffset: Offset = Offset.Zero
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

    // Draw Bezier curve
    drawBezierConnection(sourcePos, targetPos, scale)
}

/**
 * Draws a node on the canvas with ports
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNode(
    node: Node,
    position: Offset,
    scale: Float,
    isSelected: Boolean,
    isDragging: Boolean
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
        isSelected -> Color(0xFF64B5F6)
        else -> Color.White
    }

    val borderColor = when {
        isSelected -> Color(0xFF2196F3)
        else -> Color(0xFF9E9E9E)
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
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f * scale)
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
