/*
 * ConnectionRenderer - Rendering Logic for Flow Graph Connections
 * Handles visual representation of connections between nodes with bezier curves and port indicators
 * License: Apache 2.0
 */

package io.codenode.grapheditor.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.ConnectionSegment
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.Port
import kotlin.math.abs

/**
 * Renders a connection between two nodes using a bezier curve
 *
 * @param connection The connection to render
 * @param sourceNode Source node
 * @param targetNode Target node
 * @param sourcePosition Canvas position of source node
 * @param targetPosition Canvas position of target node
 * @param scale Current canvas zoom scale
 * @param isSelected Whether the connection is selected
 * @param isHovered Whether the connection is being hovered over
 * @param isValid Whether the connection is valid (type-compatible)
 */
fun DrawScope.renderConnection(
    connection: Connection,
    sourceNode: Node,
    targetNode: Node,
    sourcePosition: Offset,
    targetPosition: Offset,
    scale: Float = 1f,
    isSelected: Boolean = false,
    isHovered: Boolean = false,
    isValid: Boolean = true
) {
    // Find the specific ports
    val sourcePort = findPort(sourceNode, connection.sourcePortId, Port.Direction.OUTPUT)
    val targetPort = findPort(targetNode, connection.targetPortId, Port.Direction.INPUT)

    if (sourcePort == null || targetPort == null) {
        // Draw invalid connection indicator
        renderInvalidConnection(sourcePosition, targetPosition, scale)
        return
    }

    // Calculate port positions
    val sourcePortPosition = calculatePortPosition(
        sourceNode,
        sourcePort,
        sourcePosition,
        scale
    )

    val targetPortPosition = calculatePortPosition(
        targetNode,
        targetPort,
        targetPosition,
        scale
    )

    // Determine connection styling
    val connectionColor = getConnectionColor(
        isValid = isValid,
        isSelected = isSelected,
        isHovered = isHovered
    )

    val strokeWidth = when {
        isSelected -> 3f * scale
        isHovered -> 2.5f * scale
        else -> 2f * scale
    }

    // Draw the bezier curve
    val path = createBezierPath(sourcePortPosition, targetPortPosition)

    drawPath(
        path = path,
        color = connectionColor,
        style = Stroke(
            width = strokeWidth,
            pathEffect = if (!isValid) PathEffect.dashPathEffect(floatArrayOf(10f * scale, 5f * scale)) else null
        )
    )

    // Draw arrowhead at target
    if (isValid) {
        drawArrowhead(
            position = targetPortPosition,
            direction = targetPortPosition - sourcePortPosition,
            color = connectionColor,
            scale = scale
        )
    }

    // Draw port connection indicators
    drawPortIndicator(sourcePortPosition, connectionColor, scale)
    drawPortIndicator(targetPortPosition, connectionColor, scale)
}

/**
 * Renders a connection segment between two nodes using a bezier curve.
 * Segments are portions of a full connection, visible within a specific scope context.
 *
 * @param segment The connection segment to render
 * @param sourceNode Source node of this segment
 * @param targetNode Target node of this segment
 * @param sourcePosition Canvas position of source node
 * @param targetPosition Canvas position of target node
 * @param scale Current canvas zoom scale
 * @param isSelected Whether the parent connection is selected
 * @param isHovered Whether the segment is being hovered over
 * @param color Optional color override for the segment
 */
fun DrawScope.renderSegment(
    segment: ConnectionSegment,
    sourceNode: Node,
    targetNode: Node,
    sourcePosition: Offset,
    targetPosition: Offset,
    scale: Float = 1f,
    isSelected: Boolean = false,
    isHovered: Boolean = false,
    color: Color? = null
) {
    // Find the specific ports
    val sourcePort = findPort(sourceNode, segment.sourcePortId, Port.Direction.OUTPUT)
        ?: findPort(sourceNode, segment.sourcePortId, Port.Direction.INPUT)
    val targetPort = findPort(targetNode, segment.targetPortId, Port.Direction.INPUT)
        ?: findPort(targetNode, segment.targetPortId, Port.Direction.OUTPUT)

    if (sourcePort == null || targetPort == null) {
        // Draw invalid segment indicator
        renderInvalidConnection(sourcePosition, targetPosition, scale)
        return
    }

    // Calculate port positions
    val sourcePortPosition = calculatePortPosition(
        sourceNode,
        sourcePort,
        sourcePosition,
        scale
    )

    val targetPortPosition = calculatePortPosition(
        targetNode,
        targetPort,
        targetPosition,
        scale
    )

    // Determine segment styling
    val segmentColor = color ?: getConnectionColor(
        isValid = true,
        isSelected = isSelected,
        isHovered = isHovered
    )

    val strokeWidth = when {
        isSelected -> 3f * scale
        isHovered -> 2.5f * scale
        else -> 2f * scale
    }

    // Draw the bezier curve
    val path = createBezierPath(sourcePortPosition, targetPortPosition)

    drawPath(
        path = path,
        color = segmentColor,
        style = Stroke(width = strokeWidth)
    )

    // Draw arrowhead at target
    drawArrowhead(
        position = targetPortPosition,
        direction = targetPortPosition - sourcePortPosition,
        color = segmentColor,
        scale = scale
    )

    // Draw port connection indicators
    drawPortIndicator(sourcePortPosition, segmentColor, scale)
    drawPortIndicator(targetPortPosition, segmentColor, scale)
}

/**
 * Creates a bezier curve path between two points
 */
private fun createBezierPath(
    start: Offset,
    end: Offset
): Path {
    val path = Path()
    path.moveTo(start.x, start.y)

    // Calculate control points for a smooth curve
    val dx = abs(end.x - start.x)
    val controlOffset = dx * 0.5f

    val control1 = Offset(start.x + controlOffset, start.y)
    val control2 = Offset(end.x - controlOffset, end.y)

    path.cubicTo(
        control1.x, control1.y,
        control2.x, control2.y,
        end.x, end.y
    )

    return path
}

/**
 * Draws an arrowhead at the target end of a connection
 */
private fun DrawScope.drawArrowhead(
    position: Offset,
    direction: Offset,
    color: Color,
    scale: Float
) {
    val arrowSize = 8f * scale

    // Normalize direction
    val length = kotlin.math.sqrt(direction.x * direction.x + direction.y * direction.y)
    if (length == 0f) return

    val normalizedDir = Offset(direction.x / length, direction.y / length)

    // Calculate arrowhead points
    val perpendicular = Offset(-normalizedDir.y, normalizedDir.x)

    val tip = position
    val left = position - normalizedDir * arrowSize + perpendicular * (arrowSize / 2)
    val right = position - normalizedDir * arrowSize - perpendicular * (arrowSize / 2)

    val arrowPath = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(left.x, left.y)
        lineTo(right.x, right.y)
        close()
    }

    drawPath(
        path = arrowPath,
        color = color
    )
}

/**
 * Draws a small indicator circle at port connection points
 */
private fun DrawScope.drawPortIndicator(
    position: Offset,
    color: Color,
    scale: Float
) {
    drawCircle(
        color = color,
        radius = 3f * scale,
        center = position
    )
}

/**
 * Renders an invalid connection with a dashed line
 */
private fun DrawScope.renderInvalidConnection(
    start: Offset,
    end: Offset,
    scale: Float
) {
    drawLine(
        color = Color(0xFFF44336),  // Red for invalid
        start = start,
        end = end,
        strokeWidth = 2f * scale,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f * scale, 4f * scale))
    )
}

/**
 * Calculates the position of a port on the canvas
 */
private fun calculatePortPosition(
    node: Node,
    port: Port<*>,
    nodePosition: Offset,
    scale: Float
): Offset {
    val nodeWidth = if (node is CodeNode) 150f * scale else 120f * scale
    val nodeHeight = if (node is CodeNode) 80f * scale else 60f * scale

    return when (port.direction) {
        Port.Direction.INPUT -> {
            // Input ports are on the left side
            val portCount = node.inputPorts.size
            val portIndex = node.inputPorts.indexOf(port)
            val portY = if (portCount > 1) {
                val spacing = (nodeHeight - 20f * scale) / (portCount + 1)
                spacing * (portIndex + 1)
            } else {
                nodeHeight / 2
            }
            Offset(nodePosition.x, nodePosition.y + portY)
        }
        Port.Direction.OUTPUT -> {
            // Output ports are on the right side
            val portCount = node.outputPorts.size
            val portIndex = node.outputPorts.indexOf(port)
            val portY = if (portCount > 1) {
                val spacing = (nodeHeight - 20f * scale) / (portCount + 1)
                spacing * (portIndex + 1)
            } else {
                nodeHeight / 2
            }
            Offset(nodePosition.x + nodeWidth, nodePosition.y + portY)
        }
    }
}

/**
 * Finds a port on a node by ID and direction
 */
private fun findPort(
    node: Node,
    portId: String,
    expectedDirection: Port.Direction
): Port<*>? {
    val ports = when (expectedDirection) {
        Port.Direction.INPUT -> node.inputPorts
        Port.Direction.OUTPUT -> node.outputPorts
    }
    return ports.find { it.id == portId }
}

/**
 * Determines the color for a connection based on its state
 */
private fun getConnectionColor(
    isValid: Boolean,
    isSelected: Boolean,
    isHovered: Boolean
): Color {
    return when {
        !isValid -> Color(0xFFF44336)  // Red for invalid connections
        isSelected -> Color(0xFF2196F3)  // Blue for selected
        isHovered -> Color(0xFF64B5F6)  // Light blue for hovered
        else -> Color(0xFF9E9E9E)  // Gray for normal
    }
}

/**
 * Checks if a point is near a connection line (for hit testing)
 *
 * @param connection The connection
 * @param sourceNode Source node
 * @param targetNode Target node
 * @param sourcePosition Canvas position of source node
 * @param targetPosition Canvas position of target node
 * @param point Point to test
 * @param scale Current zoom scale
 * @param threshold Distance threshold for hit detection
 * @return True if the point is near the connection
 */
fun isPointNearConnection(
    connection: Connection,
    sourceNode: Node,
    targetNode: Node,
    sourcePosition: Offset,
    targetPosition: Offset,
    point: Offset,
    scale: Float = 1f,
    threshold: Float = 10f
): Boolean {
    val sourcePort = findPort(sourceNode, connection.sourcePortId, Port.Direction.OUTPUT) ?: return false
    val targetPort = findPort(targetNode, connection.targetPortId, Port.Direction.INPUT) ?: return false

    val sourcePortPos = calculatePortPosition(sourceNode, sourcePort, sourcePosition, scale)
    val targetPortPos = calculatePortPosition(targetNode, targetPort, targetPosition, scale)

    // Simple distance check to line segment
    // For production, should use bezier curve distance calculation
    return distanceToLineSegment(point, sourcePortPos, targetPortPos) <= threshold * scale
}

/**
 * Calculates the distance from a point to a line segment
 */
private fun distanceToLineSegment(point: Offset, lineStart: Offset, lineEnd: Offset): Float {
    val dx = lineEnd.x - lineStart.x
    val dy = lineEnd.y - lineStart.y
    val lengthSquared = dx * dx + dy * dy

    if (lengthSquared == 0f) {
        // Line segment is a point
        val pdx = point.x - lineStart.x
        val pdy = point.y - lineStart.y
        return kotlin.math.sqrt(pdx * pdx + pdy * pdy)
    }

    // Calculate projection of point onto line
    val t = ((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy) / lengthSquared
    val clampedT = t.coerceIn(0f, 1f)

    val projectionX = lineStart.x + clampedT * dx
    val projectionY = lineStart.y + clampedT * dy

    val pdx = point.x - projectionX
    val pdy = point.y - projectionY

    return kotlin.math.sqrt(pdx * pdx + pdy * pdy)
}
