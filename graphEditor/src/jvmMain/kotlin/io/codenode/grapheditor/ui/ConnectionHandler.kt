/*
 * ConnectionHandler - Connection Creation Logic for Flow Graph Editor
 * Handles interactive connection creation between node ports (click output → click input)
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.Port
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.grapheditor.state.GraphState
import io.codenode.grapheditor.state.PortLocation
import io.codenode.grapheditor.rendering.getNodeBounds

/**
 * State holder for connection creation workflow
 * Manages the two-step process: click source port → click target port
 */
class ConnectionCreationState {
    /**
     * The source port being connected from (null if no connection in progress)
     */
    var sourcePort by mutableStateOf<PortInfo?>(null)
        private set

    /**
     * Current mouse position during connection creation (for visual feedback)
     */
    var currentMousePosition by mutableStateOf(Offset.Zero)
        private set

    /**
     * Whether a connection creation is in progress
     */
    val isCreatingConnection: Boolean
        get() = sourcePort != null

    /**
     * Starts connection creation from a source port
     *
     * @param nodeId ID of the node containing the port
     * @param portId ID of the port
     * @param portPosition Position of the port on the canvas
     * @param direction Direction of the port (should be OUTPUT for source)
     */
    fun startConnection(
        nodeId: String,
        portId: String,
        portPosition: Offset,
        direction: Port.Direction
    ) {
        sourcePort = PortInfo(nodeId, portId, portPosition, direction)
        currentMousePosition = portPosition
    }

    /**
     * Updates the current mouse position (for drawing the temporary connection line)
     *
     * @param position Current mouse position in canvas coordinates
     */
    fun updateMousePosition(position: Offset) {
        currentMousePosition = position
    }

    /**
     * Completes the connection to a target port
     *
     * @param targetNodeId ID of the target node
     * @param targetPortId ID of the target port
     * @param targetDirection Direction of the target port (should be INPUT)
     * @return The source PortInfo if successful, null if invalid
     */
    fun completeConnection(
        targetNodeId: String,
        targetPortId: String,
        targetDirection: Port.Direction
    ): PortInfo? {
        val source = sourcePort ?: return null

        // Validate that source is OUTPUT and target is INPUT
        if (source.direction != Port.Direction.OUTPUT) {
            return null
        }
        if (targetDirection != Port.Direction.INPUT) {
            return null
        }

        // Prevent self-connections (same node)
        if (source.nodeId == targetNodeId) {
            return null
        }

        val result = source
        cancelConnection()
        return result
    }

    /**
     * Cancels the current connection creation
     */
    fun cancelConnection() {
        sourcePort = null
        currentMousePosition = Offset.Zero
    }
}

/**
 * Information about a port for connection creation
 *
 * @property nodeId ID of the node containing the port
 * @property portId ID of the port
 * @property position Position of the port on the canvas
 * @property direction Direction of the port (INPUT or OUTPUT)
 */
data class PortInfo(
    val nodeId: String,
    val portId: String,
    val position: Offset,
    val direction: Port.Direction
)

/**
 * Creates a remembered ConnectionCreationState instance
 *
 * @return A remembered ConnectionCreationState
 */
@Composable
fun rememberConnectionCreationState(): ConnectionCreationState {
    return remember { ConnectionCreationState() }
}

/**
 * Modifier that enables connection creation on the canvas
 *
 * @param connectionState The connection creation state
 * @param graphState The graph state to update
 * @param onConnectionCreated Callback when a connection is successfully created
 * @param onConnectionFailed Callback when a connection creation fails with error message
 */
fun Modifier.connectionCreationHandler(
    connectionState: ConnectionCreationState,
    graphState: GraphState,
    onConnectionCreated: (Connection) -> Unit = {},
    onConnectionFailed: (String) -> Unit = {}
): Modifier = this.pointerInput(Unit) {
    detectTapGestures { offset ->
        // Track mouse movement if connection is in progress
        if (connectionState.isCreatingConnection) {
            connectionState.updateMousePosition(offset)
        }
    }
}

/**
 * Modifier that makes a port interactive for connection creation
 *
 * @param node The node containing the port
 * @param port The port to make interactive
 * @param portPosition Position of the port on the canvas
 * @param connectionState The connection creation state
 * @param graphState The graph state to update
 * @param onConnectionCreated Callback when a connection is created
 * @param onConnectionFailed Callback when connection creation fails
 */
fun Modifier.portConnectionHandler(
    node: Node,
    port: Port<*>,
    portPosition: Offset,
    connectionState: ConnectionCreationState,
    graphState: GraphState,
    onConnectionCreated: (Connection) -> Unit = {},
    onConnectionFailed: (String) -> Unit = {}
): Modifier = this.pointerInput(node.id, port.id) {
    detectTapGestures { _ ->
        if (connectionState.isCreatingConnection) {
            // Second click - complete the connection
            val sourceInfo = connectionState.completeConnection(
                targetNodeId = node.id,
                targetPortId = port.id,
                targetDirection = port.direction
            )

            if (sourceInfo != null) {
                // Create the connection
                val connection = Connection(
                    id = "conn_${System.currentTimeMillis()}_${(0..9999).random()}",
                    sourceNodeId = sourceInfo.nodeId,
                    sourcePortId = sourceInfo.portId,
                    targetNodeId = node.id,
                    targetPortId = port.id
                )

                // Validate and add to graph
                val success = graphState.addConnection(connection)
                if (success) {
                    onConnectionCreated(connection)
                } else {
                    onConnectionFailed(graphState.errorMessage ?: "Failed to create connection")
                }
            } else {
                connectionState.cancelConnection()
                onConnectionFailed("Invalid connection: Check port directions and nodes")
            }
        } else {
            // First click - start connection from this port
            if (port.direction == Port.Direction.OUTPUT) {
                connectionState.startConnection(
                    nodeId = node.id,
                    portId = port.id,
                    portPosition = portPosition,
                    direction = port.direction
                )
            } else {
                onConnectionFailed("Connections must start from an output port")
            }
        }
    }
}

/**
 * Calculates the position of a port on the canvas
 *
 * @param node The node containing the port
 * @param port The port
 * @param nodePosition Position of the node on the canvas
 * @param scale Current canvas zoom scale
 * @return Position of the port on the canvas
 */
fun calculatePortPosition(
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
 * Finds a port at the given position on a node
 *
 * @param node The node to search
 * @param position Position to check (in canvas coordinates)
 * @param nodePosition Position of the node on the canvas
 * @param scale Current canvas zoom scale
 * @param threshold Distance threshold for hit detection (in pixels)
 * @return The port if found within threshold, null otherwise
 */
fun findPortAtPosition(
    node: Node,
    position: Offset,
    nodePosition: Offset,
    scale: Float,
    threshold: Float = 10f
): Port<*>? {
    val portRadius = 6f * scale
    val hitRadius = portRadius + threshold

    // Check input ports
    node.inputPorts.forEachIndexed { index, port ->
        val portPos = calculatePortPosition(node, port, nodePosition, scale)
        val distance = (position - portPos).getDistance()
        if (distance <= hitRadius) {
            return port
        }
    }

    // Check output ports
    node.outputPorts.forEachIndexed { index, port ->
        val portPos = calculatePortPosition(node, port, nodePosition, scale)
        val distance = (position - portPos).getDistance()
        if (distance <= hitRadius) {
            return port
        }
    }

    return null
}

/**
 * Helper to calculate distance between two points
 */
private fun Offset.getDistance(): Float {
    return kotlin.math.sqrt(x * x + y * y)
}

/**
 * Validates whether a connection can be created between two ports
 *
 * @param sourceNode Source node
 * @param sourcePort Source port
 * @param targetNode Target node
 * @param targetPort Target port
 * @return ValidationResult with success flag and error messages
 */
fun validateConnection(
    sourceNode: Node,
    sourcePort: Port<*>,
    targetNode: Node,
    targetPort: Port<*>
): ConnectionValidationResult {
    val errors = mutableListOf<String>()

    // Check port directions
    if (sourcePort.direction != Port.Direction.OUTPUT) {
        errors.add("Source port must be an OUTPUT port")
    }
    if (targetPort.direction != Port.Direction.INPUT) {
        errors.add("Target port must be an INPUT port")
    }

    // Check for self-connection
    if (sourceNode.id == targetNode.id) {
        errors.add("Cannot connect a node to itself")
    }

    // Check data type compatibility (basic check)
    if (sourcePort.dataType != targetPort.dataType) {
        errors.add("Port data types do not match: ${sourcePort.dataType.simpleName} → ${targetPort.dataType.simpleName}")
    }

    return ConnectionValidationResult(
        isValid = errors.isEmpty(),
        errors = errors
    )
}

/**
 * Result of connection validation
 *
 * @property isValid Whether the connection is valid
 * @property errors List of validation error messages
 */
data class ConnectionValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

/**
 * Checks if a connection already exists between two ports
 *
 * @param graphState The graph state to check
 * @param sourceNodeId ID of the source node
 * @param sourcePortId ID of the source port
 * @param targetNodeId ID of the target node
 * @param targetPortId ID of the target port
 * @return true if a connection already exists
 */
fun connectionExists(
    graphState: GraphState,
    sourceNodeId: String,
    sourcePortId: String,
    targetNodeId: String,
    targetPortId: String
): Boolean {
    return graphState.flowGraph.connections.any { conn ->
        conn.sourceNodeId == sourceNodeId &&
        conn.sourcePortId == sourcePortId &&
        conn.targetNodeId == targetNodeId &&
        conn.targetPortId == targetPortId
    }
}

/**
 * Gets all connections connected to a specific port
 *
 * @param graphState The graph state to search
 * @param nodeId ID of the node
 * @param portId ID of the port
 * @return List of connections involving this port
 */
fun getConnectionsForPort(
    graphState: GraphState,
    nodeId: String,
    portId: String
): List<Connection> {
    return graphState.flowGraph.connections.filter { conn ->
        (conn.sourceNodeId == nodeId && conn.sourcePortId == portId) ||
        (conn.targetNodeId == nodeId && conn.targetPortId == portId)
    }
}

/**
 * Checks if a port can accept more connections based on its cardinality
 *
 * @param graphState The graph state
 * @param nodeId ID of the node
 * @param portId ID of the port
 * @param maxConnections Maximum allowed connections (default: unlimited)
 * @return true if the port can accept more connections
 */
fun canPortAcceptConnection(
    graphState: GraphState,
    nodeId: String,
    portId: String,
    maxConnections: Int = Int.MAX_VALUE
): Boolean {
    val existingConnections = getConnectionsForPort(graphState, nodeId, portId)
    return existingConnections.size < maxConnections
}
