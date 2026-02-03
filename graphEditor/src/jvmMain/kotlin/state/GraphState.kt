/*
 * GraphState - UI State Management for Flow Graph Editor
 * Manages graph data, selection, viewport, and history for undo/redo
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.fbpdsl.model.NodeTypeDefinition
import io.codenode.grapheditor.ui.ConnectionContextMenuState

/**
 * State holder for the visual flow graph editor.
 * Manages all mutable state for the canvas, including graph data, selection, viewport, and history.
 *
 * This is a Compose-compatible state holder that provides reactive updates to the UI.
 * All mutations create new immutable copies, enabling proper Compose recomposition and undo/redo.
 *
 * @property initialGraph The initial flow graph to display
 */
class GraphState(initialGraph: FlowGraph = flowGraph(
    name = "Untitled",
    version = "1.0.0"
) {}) {

    /**
     * The current flow graph being edited
     */
    var flowGraph by mutableStateOf(initialGraph)
        private set

    /**
     * Unified selection state for nodes and connections.
     * Single-selection is just a selection with one element.
     * Multi-selection is a selection with multiple elements.
     */
    var selectionState by mutableStateOf(SelectionState())
        private set

    /**
     * Currently selected node ID (null if no node selection or multiple nodes selected).
     * This is a convenience property - the source of truth is selectionState.
     */
    val selectedNodeId: String?
        get() = if (selectionState.nodeSelectionCount == 1) {
            selectionState.selectedNodeIds.firstOrNull()
        } else {
            null
        }

    /**
     * Set of currently selected connection IDs.
     * This is a convenience property - the source of truth is selectionState.
     */
    val selectedConnectionIds: Set<String>
        get() = selectionState.selectedConnectionIds

    /**
     * Canvas pan offset (for scrolling/panning the view)
     */
    var panOffset by mutableStateOf(Offset.Zero)
        private set

    /**
     * Canvas zoom scale (1.0 = 100%, 0.5 = 50%, 2.0 = 200%)
     */
    var scale by mutableStateOf(1f)
        private set

    /**
     * Whether the graph has unsaved changes
     */
    var isDirty by mutableStateOf(false)
        private set

    /**
     * Error message to display (null if no error)
     */
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /**
     * Node currently being dragged (null if no drag in progress)
     */
    var draggingNodeId by mutableStateOf<String?>(null)
        private set

    /**
     * Offset accumulated during current drag operation
     */
    var dragOffset by mutableStateOf(Offset.Zero)
        private set

    /**
     * Connection being created (stores source node and port IDs)
     */
    var pendingConnection by mutableStateOf<PendingConnection?>(null)
        private set

    /**
     * Node being hovered over (for visual feedback)
     */
    var hoveredNodeId by mutableStateOf<String?>(null)
        private set

    /**
     * Port being hovered over (for connection creation)
     */
    var hoveredPort by mutableStateOf<PortLocation?>(null)
        private set

    /**
     * Context menu state for connection right-click menu
     */
    var connectionContextMenu by mutableStateOf<ConnectionContextMenuState?>(null)
        private set

    /**
     * Navigation context for hierarchical GraphNode traversal.
     * Tracks the path from root to current view level.
     */
    var navigationContext by mutableStateOf(NavigationContext())
        private set

    // ============================================================================
    // Graph Mutation Operations
    // ============================================================================

    /**
     * Replaces the entire flow graph with a new one
     *
     * @param newGraph The new flow graph
     * @param markDirty Whether to mark the graph as modified
     */
    fun setGraph(newGraph: FlowGraph, markDirty: Boolean = true) {
        flowGraph = newGraph
        if (markDirty) {
            isDirty = true
        }
    }

    /**
     * Clears the graph and resets to an empty state
     */
    fun clearGraph() {
        flowGraph = flowGraph(
            name = "Untitled",
            version = "1.0.0"
        ) {}
        selectionState = SelectionState()
        panOffset = Offset.Zero
        scale = 1f
        isDirty = false
        errorMessage = null
        draggingNodeId = null
        dragOffset = Offset.Zero
        pendingConnection = null
        hoveredNodeId = null
        hoveredPort = null
        connectionContextMenu = null
    }

    /**
     * Adds a new node to the graph at the specified position
     *
     * @param node The node to add
     * @param position Canvas position (in canvas coordinates)
     */
    fun addNode(node: Node, position: Offset) {
        // Create a copy of the node with the specified position
        val positionedNode = when (node) {
            is CodeNode -> node.copy(position = Node.Position(position.x.toDouble(), position.y.toDouble()))
            else -> node
        }

        flowGraph = flowGraph.addNode(positionedNode)
        isDirty = true
    }

    /**
     * Removes a node from the graph (also removes connected edges)
     *
     * @param nodeId The ID of the node to remove
     */
    fun removeNode(nodeId: String) {
        // Remove the node
        flowGraph = flowGraph.removeNode(nodeId)

        // Remove all connections involving this node
        val connectionsToRemove = flowGraph.getConnectionsForNode(nodeId)
        connectionsToRemove.forEach { connection ->
            flowGraph = flowGraph.removeConnection(connection.id)
        }

        // Remove from selection if the node was selected
        if (selectionState.containsNode(nodeId)) {
            selectionState = selectionState.copy(
                selectedElements = selectionState.selectedElements - SelectableElement.Node(nodeId)
            )
        }

        isDirty = true
    }

    /**
     * Updates the position of a node
     *
     * @param nodeId The ID of the node to move
     * @param newX New X coordinate (in canvas coordinates)
     * @param newY New Y coordinate (in canvas coordinates)
     */
    fun updateNodePosition(nodeId: String, newX: Double, newY: Double) {
        val node = flowGraph.findNode(nodeId) ?: return

        val updatedNode = when (node) {
            is CodeNode -> node.copy(position = Node.Position(newX, newY))
            else -> node
        }

        // Replace the node in the graph
        flowGraph = flowGraph.removeNode(nodeId).addNode(updatedNode)
        isDirty = true
    }

    /**
     * Adds a connection between two nodes
     *
     * @param connection The connection to add
     * @return true if connection was added successfully, false if invalid
     */
    fun addConnection(connection: Connection): Boolean {
        // Validate the connection
        val validation = connection.validate()
        if (!validation.success) {
            errorMessage = "Invalid connection: ${validation.errors.joinToString(", ")}"
            return false
        }

        // Check if source and target nodes exist
        val sourceNode = flowGraph.findNode(connection.sourceNodeId)
        val targetNode = flowGraph.findNode(connection.targetNodeId)

        if (sourceNode == null || targetNode == null) {
            errorMessage = "Cannot create connection: source or target node not found"
            return false
        }

        // Add the connection
        flowGraph = flowGraph.addConnection(connection)
        isDirty = true
        errorMessage = null
        return true
    }

    /**
     * Removes a connection from the graph
     *
     * @param connectionId The ID of the connection to remove
     */
    fun removeConnection(connectionId: String) {
        flowGraph = flowGraph.removeConnection(connectionId)
        // Remove from selection if the connection was selected
        if (selectionState.containsConnection(connectionId)) {
            selectionState = selectionState.copy(
                selectedElements = selectionState.selectedElements - SelectableElement.Connection(connectionId)
            )
        }
        isDirty = true
    }

    /**
     * Updates a property value on a node's configuration
     *
     * @param nodeId The ID of the node to update
     * @param propertyKey The configuration key to update
     * @param value The new value
     */
    fun updateNodeProperty(nodeId: String, propertyKey: String, value: String) {
        val node = flowGraph.findNode(nodeId) as? CodeNode ?: return

        val updatedConfig = node.configuration + (propertyKey to value)
        val updatedNode = node.copy(configuration = updatedConfig)

        // Replace the node in the graph
        flowGraph = flowGraph.removeNode(nodeId).addNode(updatedNode)
        isDirty = true
    }

    /**
     * Updates a node's display name.
     *
     * @param nodeId The ID of the node to update
     * @param newName The new display name for the node
     */
    fun updateNodeName(nodeId: String, newName: String) {
        val node = flowGraph.findNode(nodeId) as? CodeNode ?: return

        val updatedNode = node.copy(name = newName)

        // Replace the node in the graph
        flowGraph = flowGraph.removeNode(nodeId).addNode(updatedNode)
        isDirty = true
    }

    /**
     * Updates a port's name on a node.
     *
     * @param nodeId The ID of the node containing the port
     * @param portId The ID of the port to rename
     * @param newName The new name for the port
     */
    fun updatePortName(nodeId: String, portId: String, newName: String) {
        val node = flowGraph.findNode(nodeId) as? CodeNode ?: return

        // Find and update the port in input or output ports
        val updatedInputPorts = node.inputPorts.map { port ->
            if (port.id == portId) port.copy(name = newName) else port
        }
        val updatedOutputPorts = node.outputPorts.map { port ->
            if (port.id == portId) port.copy(name = newName) else port
        }

        val updatedNode = node.copy(
            inputPorts = updatedInputPorts,
            outputPorts = updatedOutputPorts
        )

        // Replace the node in the graph
        flowGraph = flowGraph.removeNode(nodeId).addNode(updatedNode)
        isDirty = true
    }

    // ============================================================================
    // Selection Operations
    // ============================================================================

    /**
     * Selects a single node (clears previous selection).
     * This is equivalent to setting selectionState to contain only this node.
     *
     * @param nodeId The ID of the node to select (null to clear selection)
     */
    fun selectNode(nodeId: String?) {
        selectionState = if (nodeId != null) {
            SelectionState(selectedElements = setOf(SelectableElement.Node(nodeId)))
        } else {
            SelectionState()
        }
    }

    /**
     * Adds a node to the current selection (multi-select)
     *
     * @param nodeId The ID of the node to add to selection
     */
    fun addNodeToSelection(nodeId: String) {
        selectionState = selectionState.copy(
            selectedElements = selectionState.selectedElements + SelectableElement.Node(nodeId)
        )
    }

    /**
     * Selects a single connection (clears previous selection).
     * This is equivalent to setting selectionState to contain only this connection.
     *
     * @param connectionId The ID of the connection to select
     */
    fun selectConnection(connectionId: String) {
        selectionState = SelectionState(selectedElements = setOf(SelectableElement.Connection(connectionId)))
    }

    /**
     * Adds a connection to the current selection (multi-select)
     *
     * @param connectionId The ID of the connection to add
     */
    fun addConnectionToSelection(connectionId: String) {
        selectionState = selectionState.copy(
            selectedElements = selectionState.selectedElements + SelectableElement.Connection(connectionId)
        )
    }

    /**
     * Clears all selections
     */
    fun clearSelection() {
        selectionState = SelectionState()
    }

    // ============================================================================
    // Toggle Selection Operations (for Shift-Click)
    // ============================================================================

    /**
     * Toggles a node in the selection.
     * If the node is not selected, it's added to the selection.
     * If the node is already selected, it's removed from the selection.
     *
     * @param nodeId The ID of the node to toggle
     */
    fun toggleNodeInSelection(nodeId: String) {
        selectionState = selectionState.toggleNode(nodeId)
    }

    /**
     * Toggles a connection in the selection.
     * If the connection is not selected, it's added to the selection.
     * If the connection is already selected, it's removed from the selection.
     *
     * @param connectionId The ID of the connection to toggle
     */
    fun toggleConnectionInSelection(connectionId: String) {
        selectionState = selectionState.toggleConnection(connectionId)
    }

    /**
     * Toggles any selectable element in the selection.
     *
     * @param element The element to toggle
     */
    fun toggleElementInSelection(element: SelectableElement) {
        selectionState = selectionState.toggle(element)
    }

    /**
     * Selects all connections that are between the given nodes.
     * A connection is "between" the nodes if both its source and target are in the node set.
     *
     * @param nodeIds The set of node IDs to find internal connections for
     */
    fun selectConnectionsBetweenNodes(nodeIds: Set<String>) {
        val internalConnectionElements = flowGraph.connections.filter { conn ->
            conn.sourceNodeId in nodeIds && conn.targetNodeId in nodeIds
        }.map { SelectableElement.Connection(it.id) }.toSet()

        selectionState = selectionState.copy(
            selectedElements = selectionState.selectedElements
                .filterIsInstance<SelectableElement.Node>()
                .toSet() + internalConnectionElements
        )
    }

    /**
     * Adds multiple nodes to the selection.
     *
     * @param nodeIds The set of node IDs to add
     */
    fun addNodesToSelection(nodeIds: Set<String>) {
        val nodeElements = nodeIds.map { SelectableElement.Node(it) }.toSet()
        selectionState = selectionState.copy(
            selectedElements = selectionState.selectedElements + nodeElements
        )
    }

    // ============================================================================
    // Viewport Operations
    // ============================================================================

    /**
     * Updates the canvas pan offset
     *
     * @param offset New pan offset
     */
    fun updatePanOffset(offset: Offset) {
        panOffset = offset
    }

    /**
     * Adjusts the canvas pan offset by a delta
     *
     * @param delta Amount to pan by
     */
    fun pan(delta: Offset) {
        panOffset += delta
    }

    /**
     * Updates the canvas zoom scale
     *
     * @param newScale New zoom scale (clamped to 0.1 - 5.0)
     */
    fun updateScale(newScale: Float) {
        scale = newScale.coerceIn(0.1f, 5.0f)
    }

    /**
     * Adjusts the canvas zoom scale by a factor
     *
     * @param factor Zoom factor (e.g., 1.1 to zoom in 10%, 0.9 to zoom out 10%)
     * @param focalPoint Point to zoom towards (in screen coordinates)
     */
    fun zoom(factor: Float, focalPoint: Offset? = null) {
        val oldScale = scale
        val newScale = (scale * factor).coerceIn(0.1f, 5.0f)

        if (focalPoint != null && newScale != oldScale) {
            // Adjust pan offset to zoom towards focal point
            val scaleDelta = newScale / oldScale
            val offsetX = focalPoint.x - (focalPoint.x - panOffset.x) * scaleDelta
            val offsetY = focalPoint.y - (focalPoint.y - panOffset.y) * scaleDelta
            panOffset = Offset(offsetX, offsetY)
        }

        scale = newScale
    }

    /**
     * Resets the viewport to default (centered at origin, 100% zoom)
     */
    fun resetViewport() {
        panOffset = Offset.Zero
        scale = 1f
    }

    // ============================================================================
    // Drag Operations
    // ============================================================================

    /**
     * Starts dragging a node
     *
     * @param nodeId The ID of the node being dragged
     */
    fun startDraggingNode(nodeId: String) {
        draggingNodeId = nodeId
        dragOffset = Offset.Zero
    }

    /**
     * Updates the drag offset during a drag operation
     *
     * @param delta Amount to drag by (in canvas coordinates)
     */
    fun updateDragOffset(delta: Offset) {
        dragOffset += delta
    }

    /**
     * Finishes dragging a node and commits the position change
     */
    fun finishDraggingNode() {
        val nodeId = draggingNodeId ?: return
        val node = flowGraph.findNode(nodeId) ?: return

        // Calculate final position
        val currentPos = when (node) {
            is CodeNode -> node.position
            else -> return
        }

        val newX = currentPos.x + dragOffset.x / scale
        val newY = currentPos.y + dragOffset.y / scale

        // Update node position
        updateNodePosition(nodeId, newX, newY)

        // Reset drag state
        draggingNodeId = null
        dragOffset = Offset.Zero
    }

    /**
     * Cancels the current drag operation without committing changes
     */
    fun cancelDrag() {
        draggingNodeId = null
        dragOffset = Offset.Zero
    }

    // ============================================================================
    // Connection Creation
    // ============================================================================

    /**
     * Starts creating a connection from a source port
     *
     * @param sourceNodeId ID of the source node
     * @param sourcePortId ID of the source port
     */
    fun startConnection(sourceNodeId: String, sourcePortId: String) {
        pendingConnection = PendingConnection(sourceNodeId, sourcePortId)
        errorMessage = null
    }

    /**
     * Finishes creating a connection to a target port
     *
     * @param targetNodeId ID of the target node
     * @param targetPortId ID of the target port
     * @return true if connection was created successfully
     */
    fun finishConnection(targetNodeId: String, targetPortId: String): Boolean {
        val pending = pendingConnection ?: return false

        // Create the connection
        val connection = Connection(
            id = "conn_${System.currentTimeMillis()}",
            sourceNodeId = pending.sourceNodeId,
            sourcePortId = pending.sourcePortId,
            targetNodeId = targetNodeId,
            targetPortId = targetPortId
        )

        val success = addConnection(connection)
        pendingConnection = null
        return success
    }

    /**
     * Cancels the pending connection creation
     */
    fun cancelConnection() {
        pendingConnection = null
        errorMessage = null
    }

    // ============================================================================
    // Hover State
    // ============================================================================

    /**
     * Sets the node being hovered over
     *
     * @param nodeId The ID of the hovered node (null to clear)
     */
    fun setHoveredNode(nodeId: String?) {
        hoveredNodeId = nodeId
    }

    /**
     * Updates the port being hovered over
     *
     * @param port The location of the hovered port (null to clear)
     */
    fun updateHoveredPort(port: PortLocation?) {
        hoveredPort = port
    }

    // ============================================================================
    // Context Menu Operations
    // ============================================================================

    /**
     * Shows the connection context menu at the specified position
     *
     * @param connectionId The ID of the connection to show the menu for
     * @param position Screen position where the menu should appear
     */
    fun showConnectionContextMenu(connectionId: String, position: Offset) {
        val connection = flowGraph.connections.find { it.id == connectionId }
        connectionContextMenu = ConnectionContextMenuState(
            connectionId = connectionId,
            position = position,
            currentTypeId = connection?.ipTypeId
        )
    }

    /**
     * Hides the connection context menu
     */
    fun hideConnectionContextMenu() {
        connectionContextMenu = null
    }

    /**
     * Updates the IP type of a connection
     *
     * @param connectionId The ID of the connection to update
     * @param ipTypeId The new IP type ID to assign
     */
    fun updateConnectionIPType(connectionId: String, ipTypeId: String) {
        val connection = flowGraph.connections.find { it.id == connectionId } ?: return

        val updatedConnection = connection.copy(ipTypeId = ipTypeId)

        // Remove old connection and add updated one
        flowGraph = flowGraph.removeConnection(connectionId)
        flowGraph = flowGraph.addConnection(updatedConnection)

        isDirty = true
    }

    // ============================================================================
    // Error Handling
    // ============================================================================

    /**
     * Sets an error message to display
     *
     * @param message The error message (null to clear)
     */
    fun setError(message: String?) {
        errorMessage = message
    }

    /**
     * Clears the current error message
     */
    fun clearError() {
        errorMessage = null
    }

    // ============================================================================
    // Dirty State
    // ============================================================================

    /**
     * Marks the graph as saved (clears dirty flag)
     */
    fun markAsSaved() {
        isDirty = false
    }

    /**
     * Marks the graph as modified (sets dirty flag)
     */
    fun markAsDirty() {
        isDirty = true
    }

    // ============================================================================
    // Query Operations
    // ============================================================================

    /**
     * Checks if a node is currently selected
     *
     * @param nodeId The ID of the node to check
     * @return true if the node is selected
     */
    fun isNodeSelected(nodeId: String): Boolean {
        return selectionState.containsNode(nodeId)
    }

    /**
     * Checks if a connection is currently selected
     *
     * @param connectionId The ID of the connection to check
     * @return true if the connection is selected
     */
    fun isConnectionSelected(connectionId: String): Boolean {
        return selectionState.containsConnection(connectionId)
    }

    /**
     * Checks if a node is currently being dragged
     *
     * @param nodeId The ID of the node to check
     * @return true if the node is being dragged
     */
    fun isNodeDragging(nodeId: String): Boolean {
        return draggingNodeId == nodeId
    }

    /**
     * Checks if a node is currently hovered
     *
     * @param nodeId The ID of the node to check
     * @return true if the node is hovered
     */
    fun isNodeHovered(nodeId: String): Boolean {
        return hoveredNodeId == nodeId
    }

    /**
     * Gets the current position of a node (including drag offset if dragging)
     *
     * @param nodeId The ID of the node
     * @return The current canvas position, or null if node not found
     */
    fun getNodePosition(nodeId: String): Offset? {
        val node = flowGraph.findNode(nodeId) as? CodeNode ?: return null

        val baseOffset = Offset(node.position.x.toFloat(), node.position.y.toFloat())

        return if (draggingNodeId == nodeId) {
            baseOffset + dragOffset / scale
        } else {
            baseOffset
        }
    }

    // ============================================================================
    // Navigation Context Operations
    // ============================================================================

    /**
     * Gets the nodes visible in the current navigation context.
     *
     * - If at root: returns flowGraph.nodes (root-level nodes)
     * - If navigated into a GraphNode: returns that GraphNode's childNodes
     *
     * @return List of nodes in the current view context
     */
    fun getNodesInCurrentContext(): List<Node> {
        val currentGraphNodeId = navigationContext.currentGraphNodeId

        return if (currentGraphNodeId == null) {
            // At root level - return top-level nodes
            flowGraph.rootNodes
        } else {
            // Inside a GraphNode - return its children
            val graphNode = flowGraph.findNode(currentGraphNodeId) as? GraphNode
            graphNode?.childNodes ?: emptyList()
        }
    }

    /**
     * Gets the connections visible in the current navigation context.
     *
     * - If at root: returns flowGraph.connections (root-level connections)
     * - If navigated into a GraphNode: returns that GraphNode's internalConnections
     *
     * @return List of connections in the current view context
     */
    fun getConnectionsInCurrentContext(): List<Connection> {
        val currentGraphNodeId = navigationContext.currentGraphNodeId

        return if (currentGraphNodeId == null) {
            // At root level - return top-level connections
            flowGraph.connections
        } else {
            // Inside a GraphNode - return its internal connections
            val graphNode = flowGraph.findNode(currentGraphNodeId) as? GraphNode
            graphNode?.internalConnections ?: emptyList()
        }
    }
}

/**
 * Represents a connection being created (before target is selected)
 *
 * @property sourceNodeId ID of the source node
 * @property sourcePortId ID of the source port
 */
data class PendingConnection(
    val sourceNodeId: String,
    val sourcePortId: String
)

/**
 * Represents the location of a port (for hover detection)
 *
 * @property nodeId ID of the node containing the port
 * @property portId ID of the port
 * @property isInput Whether this is an input port (false = output port)
 */
data class PortLocation(
    val nodeId: String,
    val portId: String,
    val isInput: Boolean
)

/**
 * Creates a GraphState instance as a remembered Compose state
 *
 * @param initialGraph The initial flow graph to display
 * @return A remembered GraphState instance
 */
@Composable
fun rememberGraphState(initialGraph: FlowGraph): GraphState {
    return remember(initialGraph) {
        GraphState(initialGraph)
    }
}
