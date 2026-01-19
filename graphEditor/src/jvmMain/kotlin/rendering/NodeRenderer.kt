/*
 * NodeRenderer - Rendering Logic for Flow Graph Nodes
 * Handles visual representation of nodes with ports, labels, and state indicators
 * License: Apache 2.0
 */

package io.codenode.grapheditor.rendering

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port

/**
 * Renders a node on the canvas with visual styling based on node type and state
 *
 * @param node The node to render
 * @param position Canvas position (top-left corner)
 * @param scale Current canvas zoom scale
 * @param isSelected Whether the node is currently selected
 * @param isHovered Whether the node is being hovered over
 * @param isDragging Whether the node is being dragged
 * @param textMeasurer Text measurer for rendering labels
 */
fun DrawScope.renderNode(
    node: Node,
    position: Offset,
    scale: Float = 1f,
    isSelected: Boolean = false,
    isHovered: Boolean = false,
    isDragging: Boolean = false,
    textMeasurer: TextMeasurer? = null
) {
    when (node) {
        is CodeNode -> renderCodeNode(
            node = node,
            position = position,
            scale = scale,
            isSelected = isSelected,
            isHovered = isHovered,
            isDragging = isDragging,
            textMeasurer = textMeasurer
        )
        else -> renderGenericNode(
            node = node,
            position = position,
            scale = scale,
            isSelected = isSelected,
            isDragging = isDragging
        )
    }
}

/**
 * Renders a CodeNode with type-specific styling
 */
private fun DrawScope.renderCodeNode(
    node: CodeNode,
    position: Offset,
    scale: Float,
    isSelected: Boolean,
    isHovered: Boolean,
    isDragging: Boolean,
    textMeasurer: TextMeasurer?
) {
    val nodeWidth = 150f * scale
    val nodeHeight = 80f * scale
    val cornerRadius = 8f * scale

    // Determine colors based on node type and state
    val (fillColor, borderColor) = getNodeColors(
        nodeType = node.codeNodeType,
        isSelected = isSelected,
        isHovered = isHovered,
        isDragging = isDragging
    )

    // Draw shadow if selected or dragging
    if (isSelected || isDragging) {
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.2f),
            topLeft = position + Offset(2f * scale, 2f * scale),
            size = Size(nodeWidth, nodeHeight),
            cornerRadius = CornerRadius(cornerRadius)
        )
    }

    // Draw node body
    drawRoundRect(
        color = fillColor,
        topLeft = position,
        size = Size(nodeWidth, nodeHeight),
        cornerRadius = CornerRadius(cornerRadius)
    )

    // Draw border
    val borderWidth = if (isSelected) 3f * scale else 2f * scale
    drawRoundRect(
        color = borderColor,
        topLeft = position,
        size = Size(nodeWidth, nodeHeight),
        cornerRadius = CornerRadius(cornerRadius),
        style = Stroke(width = borderWidth)
    )

    // Draw node type indicator (colored bar at top)
    val typeBarHeight = 6f * scale
    drawRoundRect(
        color = getNodeTypeColor(node.codeNodeType),
        topLeft = position,
        size = Size(nodeWidth, typeBarHeight),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius, 0f, 0f)
    )

    // Draw node name
    textMeasurer?.let { measurer ->
        val textStyle = TextStyle(
            fontSize = (12 * scale).sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF212121)
        )

        val textLayoutResult = measurer.measure(
            text = node.name,
            style = textStyle
        )

        val textX = position.x + (nodeWidth - textLayoutResult.size.width) / 2
        val textY = position.y + typeBarHeight + 8f * scale

        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(textX, textY)
        )
    }

    // Draw ports
    renderPorts(
        node = node,
        position = position,
        nodeWidth = nodeWidth,
        nodeHeight = nodeHeight,
        scale = scale
    )

    // Draw configuration indicator if node has configuration
    if (node.configuration.isNotEmpty()) {
        drawCircle(
            color = Color(0xFFFF9800),
            radius = 4f * scale,
            center = Offset(position.x + nodeWidth - 10f * scale, position.y + 10f * scale)
        )
    }
}

/**
 * Renders ports on a node
 */
private fun DrawScope.renderPorts(
    node: CodeNode,
    position: Offset,
    nodeWidth: Float,
    nodeHeight: Float,
    scale: Float
) {
    val portRadius = 6f * scale
    val portSpacing = if (node.inputPorts.size > 1) {
        (nodeHeight - 20f * scale) / (node.inputPorts.size + 1)
    } else {
        nodeHeight / 2
    }

    // Draw input ports (left side)
    node.inputPorts.forEachIndexed { index, port ->
        val portY = position.y + portSpacing * (index + 1)
        val portCenter = Offset(position.x, portY)

        drawCircle(
            color = getPortColor(port),
            radius = portRadius,
            center = portCenter
        )

        drawCircle(
            color = Color(0xFF424242),
            radius = portRadius,
            center = portCenter,
            style = Stroke(width = 1.5f * scale)
        )
    }

    // Draw output ports (right side)
    val outputPortSpacing = if (node.outputPorts.size > 1) {
        (nodeHeight - 20f * scale) / (node.outputPorts.size + 1)
    } else {
        nodeHeight / 2
    }

    node.outputPorts.forEachIndexed { index, port ->
        val portY = position.y + outputPortSpacing * (index + 1)
        val portCenter = Offset(position.x + nodeWidth, portY)

        drawCircle(
            color = getPortColor(port),
            radius = portRadius,
            center = portCenter
        )

        drawCircle(
            color = Color(0xFF424242),
            radius = portRadius,
            center = portCenter,
            style = Stroke(width = 1.5f * scale)
        )
    }
}

/**
 * Renders a generic node (fallback for non-CodeNode types)
 */
private fun DrawScope.renderGenericNode(
    node: Node,
    position: Offset,
    scale: Float,
    isSelected: Boolean,
    isDragging: Boolean
) {
    val nodeWidth = 120f * scale
    val nodeHeight = 60f * scale

    val fillColor = when {
        isDragging -> Color(0xFFBBDEFB)
        isSelected -> Color(0xFF64B5F6)
        else -> Color.White
    }

    val borderColor = if (isSelected) Color(0xFF2196F3) else Color(0xFF9E9E9E)

    drawRoundRect(
        color = fillColor,
        topLeft = position,
        size = Size(nodeWidth, nodeHeight),
        cornerRadius = CornerRadius(4f * scale)
    )

    drawRoundRect(
        color = borderColor,
        topLeft = position,
        size = Size(nodeWidth, nodeHeight),
        cornerRadius = CornerRadius(4f * scale),
        style = Stroke(width = 2f * scale)
    )
}

/**
 * Gets fill and border colors based on node state
 */
private fun getNodeColors(
    nodeType: CodeNodeType,
    isSelected: Boolean,
    isHovered: Boolean,
    isDragging: Boolean
): Pair<Color, Color> {
    val baseFillColor = when {
        isDragging -> Color(0xFFE3F2FD)
        isHovered -> Color(0xFFF5F5F5)
        else -> Color.White
    }

    val borderColor = when {
        isSelected -> Color(0xFF2196F3)
        isHovered -> Color(0xFF64B5F6)
        else -> Color(0xFFBDBDBD)
    }

    return baseFillColor to borderColor
}

/**
 * Gets color representing the node type
 */
private fun getNodeTypeColor(nodeType: CodeNodeType): Color {
    return when (nodeType) {
        CodeNodeType.GENERATOR -> Color(0xFF4CAF50)  // Green - produces data
        CodeNodeType.TRANSFORMER -> Color(0xFF2196F3)  // Blue - transforms data
        CodeNodeType.FILTER -> Color(0xFFFFC107)  // Amber - filters data
        CodeNodeType.VALIDATOR -> Color(0xFFFF9800)  // Orange - validates data
        CodeNodeType.SPLITTER -> Color(0xFF9C27B0)  // Purple - splits data
        CodeNodeType.MERGER -> Color(0xFF00BCD4)  // Cyan - merges data
        CodeNodeType.SINK -> Color(0xFFF44336)  // Red - consumes data
    }
}

/**
 * Gets color for a port based on its properties
 */
private fun getPortColor(port: Port<*>): Color {
    return when (port.direction) {
        Port.Direction.INPUT -> Color(0xFF4CAF50)  // Green for inputs
        Port.Direction.OUTPUT -> Color(0xFF2196F3)  // Blue for outputs
    }
}

/**
 * Calculates the bounds of a node for hit testing
 *
 * @param node The node
 * @param position Canvas position
 * @param scale Current zoom scale
 * @return Rectangle bounds (x, y, width, height)
 */
fun getNodeBounds(
    node: Node,
    position: Offset,
    scale: Float = 1f
): NodeBounds {
    val width = when (node) {
        is CodeNode -> 150f * scale
        else -> 120f * scale
    }

    val height = when (node) {
        is CodeNode -> 80f * scale
        else -> 60f * scale
    }

    return NodeBounds(
        x = position.x,
        y = position.y,
        width = width,
        height = height
    )
}

/**
 * Data class representing node bounds
 */
data class NodeBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    fun contains(point: Offset): Boolean {
        return point.x >= x && point.x <= x + width &&
               point.y >= y && point.y <= y + height
    }
}
