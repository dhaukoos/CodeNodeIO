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
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.dp
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Connection

/**
 * Main canvas component for rendering and interacting with flow graphs
 *
 * @param flowGraph The flow graph to display
 * @param selectedNodeId ID of the currently selected node (if any)
 * @param onNodeSelected Callback when a node is selected
 * @param onNodeMoved Callback when a node is dragged to a new position
 * @param onConnectionCreated Callback when a new connection is created
 * @param modifier Modifier for the canvas
 */
@Composable
fun FlowGraphCanvas(
    flowGraph: FlowGraph,
    selectedNodeId: String? = null,
    onNodeSelected: (String?) -> Unit = {},
    onNodeMoved: (nodeId: String, newX: Double, newY: Double) -> Unit = { _, _, _ -> },
    onConnectionCreated: (Connection) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Canvas state
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableStateOf(1f) }
    var draggingNode by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // Create a stable reference to current state values that gesture detectors can read
    val currentFlowGraph = remember { mutableStateOf(flowGraph) }
    val currentPanOffset = remember { mutableStateOf(Offset.Zero) }
    val currentScale = remember { mutableStateOf(1f) }

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
                        // Read current values from the stable references
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
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (draggingNode == null) {
                            panOffset += dragAmount
                        } else {
                            dragOffset += dragAmount
                        }
                    },
                    onDragEnd = {
                        if (draggingNode != null) {
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
                    scale = transformedScale
                )
            }

            // Draw nodes
            flowGraph.rootNodes.forEach { node ->
                val isSelected = node.id == selectedNodeId
                val isDragging = node.id == draggingNode

                val nodeOffset = if (isDragging) {
                    Offset(
                        node.position.x.toFloat() + dragOffset.x,
                        node.position.y.toFloat() + dragOffset.y
                    )
                } else {
                    Offset(node.position.x.toFloat(), node.position.y.toFloat())
                }

                drawNode(
                    node = node,
                    position = nodeOffset + transformedOffset,
                    scale = transformedScale,
                    isSelected = isSelected,
                    isDragging = isDragging
                )
            }
        }
    }
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
 * Draws a connection line between two nodes
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConnection(
    connection: Connection,
    nodes: List<Node>,
    offset: Offset,
    scale: Float
) {
    val sourceNode = nodes.find { it.id == connection.sourceNodeId } ?: return
    val targetNode = nodes.find { it.id == connection.targetNodeId } ?: return

    val sourcePos = Offset(
        sourceNode.position.x.toFloat() + offset.x,
        sourceNode.position.y.toFloat() + offset.y
    )

    val targetPos = Offset(
        targetNode.position.x.toFloat() + offset.x,
        targetNode.position.y.toFloat() + offset.y
    )

    // Draw simple line for now (will be enhanced by ConnectionRenderer)
    drawLine(
        color = Color(0xFF2196F3),
        start = sourcePos,
        end = targetPos,
        strokeWidth = 2f * scale
    )
}

/**
 * Draws a node on the canvas
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNode(
    node: Node,
    position: Offset,
    scale: Float,
    isSelected: Boolean,
    isDragging: Boolean
) {
    val nodeWidth = 120f * scale
    val nodeHeight = 60f * scale

    val nodeColor = when {
        isDragging -> Color(0xFFBBDEFB)
        isSelected -> Color(0xFF64B5F6)
        else -> Color.White
    }

    val borderColor = when {
        isSelected -> Color(0xFF2196F3)
        else -> Color(0xFF9E9E9E)
    }

    // Draw node rectangle (placeholder - will be enhanced by NodeRenderer)
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
    val nodeWidth = 120f * scale
    val nodeHeight = 60f * scale

    return flowGraph.rootNodes.findLast { node ->
        val nodePos = Offset(
            node.position.x.toFloat() + panOffset.x,
            node.position.y.toFloat() + panOffset.y
        )

        tapPosition.x >= nodePos.x &&
        tapPosition.x <= nodePos.x + nodeWidth &&
        tapPosition.y >= nodePos.y &&
        tapPosition.y <= nodePos.y + nodeHeight
    }
}
