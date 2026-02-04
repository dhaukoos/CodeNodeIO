/*
 * PortRenderer - Rendering Logic for Flow Graph Ports
 * Handles visual representation of ports with shape differentiation (Circle vs Square)
 * License: Apache 2.0
 */

package io.codenode.grapheditor.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import io.codenode.fbpdsl.model.PassThruPort
import io.codenode.fbpdsl.model.Port

/**
 * Enum representing the visual shape of a port.
 *
 * - CIRCLE: Standard port rendered as a circle (used for CodeNode ports)
 * - SQUARE: PassThruPort rendered as a square (used for GraphNode boundary ports)
 */
enum class PortShape {
    /** Standard port rendered as a circle (CodeNode ports) */
    CIRCLE,

    /** PassThruPort rendered as a square (GraphNode boundary ports) */
    SQUARE
}

/**
 * Port rendering dimensions
 */
object PortDimensions {
    /** Base size of a port in dp */
    const val BASE_SIZE = 8f

    /** Hover size multiplier */
    const val HOVER_MULTIPLIER = 1.5f

    /** Stroke width for port outlines */
    const val STROKE_WIDTH = 2f

    /** Corner radius for square ports */
    const val SQUARE_CORNER_RADIUS = 2f
}

/**
 * Port color scheme
 */
object PortColors {
    /** Unconnected port stroke color */
    val UNCONNECTED_STROKE = Color(0xFF757575)

    /** Connected port fill and stroke color */
    val CONNECTED = Color(0xFF2196F3)

    /** Hovered port fill color */
    val HOVERED = Color(0xFF64B5F6)

    /** Invalid port color */
    val INVALID = Color(0xFFF44336)

    /** Transparent fill for unconnected ports */
    val UNCONNECTED_FILL = Color.Transparent
}

/**
 * Extension function to determine the shape for rendering a port.
 *
 * @return PortShape.SQUARE if the port is a PassThruPort, PortShape.CIRCLE otherwise
 */
fun Port<*>.getPortShape(): PortShape {
    return PortShape.CIRCLE
}

/**
 * Extension function to determine the shape for rendering a PassThruPort.
 *
 * @return Always returns PortShape.SQUARE for PassThruPorts
 */
fun PassThruPort<*>.getPortShape(): PortShape {
    return PortShape.SQUARE
}

/**
 * Renders a port with the specified shape.
 *
 * @param position Center position of the port
 * @param direction INPUT (left side) or OUTPUT (right side)
 * @param shape CIRCLE for regular ports, SQUARE for PassThruPorts
 * @param isHovered Whether mouse is over the port
 * @param isConnected Whether port has a connection
 * @param scale Current canvas zoom level
 */
fun DrawScope.renderPort(
    position: Offset,
    direction: Port.Direction,
    shape: PortShape,
    isHovered: Boolean = false,
    isConnected: Boolean = false,
    scale: Float = 1f
) {
    val baseSize = PortDimensions.BASE_SIZE * scale
    val size = if (isHovered) baseSize * PortDimensions.HOVER_MULTIPLIER else baseSize
    val strokeWidth = PortDimensions.STROKE_WIDTH * scale

    val fillColor = when {
        isHovered -> PortColors.HOVERED
        isConnected -> PortColors.CONNECTED
        else -> PortColors.UNCONNECTED_FILL
    }

    val strokeColor = when {
        isHovered -> PortColors.CONNECTED
        isConnected -> PortColors.CONNECTED
        else -> PortColors.UNCONNECTED_STROKE
    }

    when (shape) {
        PortShape.CIRCLE -> {
            // Draw filled circle
            drawCircle(
                color = fillColor,
                radius = size / 2,
                center = position
            )
            // Draw stroke
            drawCircle(
                color = strokeColor,
                radius = size / 2,
                center = position,
                style = Stroke(width = strokeWidth)
            )
        }
        PortShape.SQUARE -> {
            val halfSize = size / 2
            val topLeft = Offset(position.x - halfSize, position.y - halfSize)

            // Draw filled square
            drawRect(
                color = fillColor,
                topLeft = topLeft,
                size = Size(size, size)
            )
            // Draw stroke
            drawRect(
                color = strokeColor,
                topLeft = topLeft,
                size = Size(size, size),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

/**
 * Renders PassThruPorts on GraphNode boundary in both exterior and interior views.
 *
 * @param inputPorts List of INPUT PassThruPorts to render on left edge
 * @param outputPorts List of OUTPUT PassThruPorts to render on right edge
 * @param boundaryLeft Left edge X coordinate of the boundary
 * @param boundaryRight Right edge X coordinate of the boundary
 * @param boundaryTop Top Y coordinate of the boundary
 * @param boundaryHeight Height of the boundary
 * @param scale Current canvas zoom level
 * @param hoveredPortId ID of currently hovered port (or null)
 * @param connectedPortIds Set of port IDs that have connections
 */
fun DrawScope.renderBoundaryPorts(
    inputPorts: List<PassThruPort<*>>,
    outputPorts: List<PassThruPort<*>>,
    boundaryLeft: Float,
    boundaryRight: Float,
    boundaryTop: Float,
    boundaryHeight: Float,
    scale: Float,
    hoveredPortId: String? = null,
    connectedPortIds: Set<String> = emptySet()
) {
    // Render INPUT PassThruPorts on left edge
    if (inputPorts.isNotEmpty()) {
        val spacing = boundaryHeight / (inputPorts.size + 1)
        inputPorts.forEachIndexed { index, port ->
            val y = boundaryTop + spacing * (index + 1)
            renderPort(
                position = Offset(boundaryLeft, y),
                direction = Port.Direction.INPUT,
                shape = PortShape.SQUARE,
                isHovered = port.id == hoveredPortId,
                isConnected = port.id in connectedPortIds,
                scale = scale
            )
        }
    }

    // Render OUTPUT PassThruPorts on right edge
    if (outputPorts.isNotEmpty()) {
        val spacing = boundaryHeight / (outputPorts.size + 1)
        outputPorts.forEachIndexed { index, port ->
            val y = boundaryTop + spacing * (index + 1)
            renderPort(
                position = Offset(boundaryRight, y),
                direction = Port.Direction.OUTPUT,
                shape = PortShape.SQUARE,
                isHovered = port.id == hoveredPortId,
                isConnected = port.id in connectedPortIds,
                scale = scale
            )
        }
    }
}
