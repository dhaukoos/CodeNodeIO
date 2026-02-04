/*
 * GraphNodeRenderer - Visual Renderer for GraphNode (Container Nodes)
 * Renders GraphNodes with distinct visual style to differentiate from CodeNodes
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.fbpdsl.model.Port
import io.codenode.grapheditor.rendering.PortShape
import io.codenode.grapheditor.rendering.renderPort

/**
 * Renders a GraphNode with distinct visual styling.
 *
 * GraphNodes are rendered differently from CodeNodes to indicate they are container nodes:
 * - Rounded rectangle with double border (outer darker, inner lighter)
 * - Gradient fill from light blue to white
 * - "Expand" icon in top-right corner for zoom-in
 * - Badge showing number of child nodes
 */
object GraphNodeRenderer {

    // Colors for GraphNode rendering
    private val outerBorderColor = Color(0xFF1565C0)  // Darker blue
    private val innerBorderColor = Color(0xFF64B5F6)  // Lighter blue
    private val gradientStart = Color(0xFFE3F2FD)     // Light blue
    private val gradientEnd = Color.White
    private val selectedOuterBorder = Color(0xFF0D47A1)  // Even darker blue for selection
    private val selectedGlow = Color(0xFF2196F3).copy(alpha = 0.3f)
    private val headerColor = Color(0xFFBBDEFB)       // Header background
    private val badgeBackground = Color(0xFF1976D2)   // Badge background
    private val badgeText = Color.White
    private val expandIconColor = Color(0xFF616161)

    /**
     * Draws a GraphNode on the canvas.
     *
     * @param graphNode The GraphNode to draw
     * @param position Top-left position on canvas (in screen coordinates)
     * @param scale Current zoom scale
     * @param isSelected Whether the node is currently selected
     * @param isDragging Whether the node is being dragged
     * @param isMultiSelected Whether the node is part of a multi-selection
     */
    fun DrawScope.drawGraphNode(
        graphNode: GraphNode,
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
        val cornerRadius = 8f * scale

        // Calculate node height based on number of ports
        val maxPorts = maxOf(graphNode.inputPorts.size, graphNode.outputPorts.size).coerceAtLeast(1)
        val nodeHeight = headerHeight + (maxPorts * portSpacing) + 20f * scale

        // Draw selection glow for multi-selected nodes
        if (isMultiSelected) {
            drawRoundRect(
                color = selectedGlow,
                topLeft = Offset(position.x - 6f * scale, position.y - 6f * scale),
                size = Size(nodeWidth + 12f * scale, nodeHeight + 12f * scale),
                cornerRadius = CornerRadius(cornerRadius + 4f * scale)
            )
        }

        // Draw outer border (darker)
        val outerBorder = if (isSelected || isMultiSelected) selectedOuterBorder else outerBorderColor
        drawRoundRect(
            color = outerBorder,
            topLeft = Offset(position.x - 2f * scale, position.y - 2f * scale),
            size = Size(nodeWidth + 4f * scale, nodeHeight + 4f * scale),
            cornerRadius = CornerRadius(cornerRadius + 2f * scale)
        )

        // Draw gradient background
        val backgroundBrush = if (isDragging) {
            Brush.verticalGradient(
                colors = listOf(Color(0xFFBBDEFB), Color(0xFFE3F2FD)),
                startY = position.y,
                endY = position.y + nodeHeight
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(gradientStart, gradientEnd),
                startY = position.y,
                endY = position.y + nodeHeight
            )
        }
        drawRoundRect(
            brush = backgroundBrush,
            topLeft = position,
            size = Size(nodeWidth, nodeHeight),
            cornerRadius = CornerRadius(cornerRadius)
        )

        // Draw inner border (lighter)
        drawRoundRect(
            color = innerBorderColor,
            topLeft = position,
            size = Size(nodeWidth, nodeHeight),
            cornerRadius = CornerRadius(cornerRadius),
            style = Stroke(width = 2f * scale)
        )

        // Draw header background (use clip path or just draw rect since top is rounded from background)
        drawRect(
            color = headerColor,
            topLeft = Offset(position.x, position.y + cornerRadius),
            size = Size(nodeWidth, headerHeight - cornerRadius)
        )
        // Draw top rounded portion of header
        drawRoundRect(
            color = headerColor,
            topLeft = position,
            size = Size(nodeWidth, cornerRadius * 2),
            cornerRadius = CornerRadius(cornerRadius)
        )

        // Draw header separator line
        drawLine(
            color = innerBorderColor,
            start = Offset(position.x, position.y + headerHeight),
            end = Offset(position.x + nodeWidth, position.y + headerHeight),
            strokeWidth = 1f * scale
        )

        // Draw child count badge
        if (graphNode.childNodes.isNotEmpty()) {
            val badgeSize = 20f * scale
            val badgeX = position.x + nodeWidth - badgeSize - 8f * scale
            val badgeY = position.y + 5f * scale

            // Badge background
            drawCircle(
                color = badgeBackground,
                radius = badgeSize / 2f,
                center = Offset(badgeX + badgeSize / 2f, badgeY + badgeSize / 2f)
            )

            // We can't easily draw text in DrawScope, so we draw a simple indicator
            // The actual number will be shown via Compose Text overlay if needed
        }

        // Draw expand icon (simple arrow indicator)
        val iconSize = 12f * scale
        val iconX = position.x + nodeWidth - iconSize - 24f * scale
        val iconY = position.y + (headerHeight - iconSize) / 2f

        // Draw expand arrow (pointing right)
        val arrowPath = Path().apply {
            moveTo(iconX, iconY)
            lineTo(iconX + iconSize, iconY + iconSize / 2f)
            lineTo(iconX, iconY + iconSize)
        }
        drawPath(
            path = arrowPath,
            color = expandIconColor,
            style = Stroke(width = 2f * scale)
        )

        // Draw input ports on the left (as squares for PassThruPorts)
        graphNode.inputPorts.forEachIndexed { index, port ->
            val portY = position.y + headerHeight + 20f * scale + (index * portSpacing)
            val portX = position.x

            // Draw port as SQUARE (PassThruPort style for GraphNode boundary)
            renderPort(
                position = Offset(portX, portY),
                direction = Port.Direction.INPUT,
                shape = PortShape.SQUARE,
                isHovered = false,
                isConnected = true,  // Assume connected for now
                scale = scale
            )

            // Draw port connector line
            drawLine(
                color = Color(0xFF757575),
                start = Offset(portX + portRadius + 2f * scale, portY),
                end = Offset(portX + 30f * scale, portY),
                strokeWidth = 1f * scale
            )
        }

        // Draw output ports on the right (as squares for PassThruPorts)
        graphNode.outputPorts.forEachIndexed { index, port ->
            val portY = position.y + headerHeight + 20f * scale + (index * portSpacing)
            val portX = position.x + nodeWidth

            // Draw port as SQUARE (PassThruPort style for GraphNode boundary)
            renderPort(
                position = Offset(portX, portY),
                direction = Port.Direction.OUTPUT,
                shape = PortShape.SQUARE,
                isHovered = false,
                isConnected = true,  // Assume connected for now
                scale = scale
            )

            // Draw port connector line
            drawLine(
                color = Color(0xFF757575),
                start = Offset(portX - portRadius - 2f * scale, portY),
                end = Offset(portX - 30f * scale, portY),
                strokeWidth = 1f * scale
            )
        }

        // Draw container icon to indicate this is a GraphNode
        // Simple stacked rectangles icon
        val iconPadding = 8f * scale
        val smallRectSize = 8f * scale
        val containerIconX = position.x + iconPadding
        val containerIconY = position.y + (headerHeight - smallRectSize * 1.5f) / 2f

        // Back rectangle
        drawRect(
            color = Color(0xFF90CAF9),
            topLeft = Offset(containerIconX + 3f * scale, containerIconY),
            size = Size(smallRectSize, smallRectSize)
        )
        drawRect(
            color = innerBorderColor,
            topLeft = Offset(containerIconX + 3f * scale, containerIconY),
            size = Size(smallRectSize, smallRectSize),
            style = Stroke(width = 1f * scale)
        )

        // Front rectangle
        drawRect(
            color = Color(0xFFE3F2FD),
            topLeft = Offset(containerIconX, containerIconY + 4f * scale),
            size = Size(smallRectSize, smallRectSize)
        )
        drawRect(
            color = innerBorderColor,
            topLeft = Offset(containerIconX, containerIconY + 4f * scale),
            size = Size(smallRectSize, smallRectSize),
            style = Stroke(width = 1f * scale)
        )
    }
}
