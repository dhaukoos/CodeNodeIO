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
            is GraphNode -> node.copy(position = Node.Position(newX, newY))
            else -> node
        }

        // Check if we're inside a GraphNode - if so, update the child node within that GraphNode
        val currentGraphNodeId = navigationContext.currentGraphNodeId
        if (currentGraphNodeId != null) {
            // We're inside a GraphNode - update the child node within the GraphNode
            val parentGraphNode = flowGraph.findNode(currentGraphNodeId) as? GraphNode
            if (parentGraphNode != null && parentGraphNode.childNodes.any { it.id == nodeId }) {
                // Update the child node within the GraphNode
                val updatedChildNodes = parentGraphNode.childNodes.map { childNode ->
                    if (childNode.id == nodeId) updatedNode else childNode
                }
                val updatedGraphNode = parentGraphNode.copy(childNodes = updatedChildNodes)
                // Replace the GraphNode at the root level
                flowGraph = flowGraph.removeNode(currentGraphNodeId).addNode(updatedGraphNode)
                isDirty = true
                return
            }
        }

        // Default: update at root level (for root-level nodes)
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

    fun addConnectionsToSelection(connectionIds: Set<String>) {
        val connectionElements = connectionIds.map { SelectableElement.Connection(it) }.toSet()
        selectionState = selectionState.copy(
            selectedElements = selectionState.selectedElements + connectionElements
        )
    }

    // ============================================================================
    // Rectangular Selection Operations
    // ============================================================================

    /**
     * Starts a rectangular selection at the given position.
     * This is called when the user begins Shift-dragging on empty canvas.
     *
     * @param startPosition The starting position of the selection box (in screen coordinates)
     */
    fun startRectangularSelection(startPosition: Offset) {
        selectionState = selectionState.copy(
            selectionBoxStart = startPosition,
            selectionBoxEnd = startPosition,
            isRectangularSelectionActive = true
        )
    }

    /**
     * Updates the rectangular selection to the current drag position.
     * This is called during Shift-drag to update the selection box.
     *
     * @param currentPosition The current drag position (in screen coordinates)
     */
    fun updateRectangularSelection(currentPosition: Offset) {
        if (selectionState.isRectangularSelectionActive) {
            selectionState = selectionState.copy(
                selectionBoxEnd = currentPosition
            )
        }
    }

    /**
     * Finishes the rectangular selection, selecting all nodes whose centers
     * are within the selection box.
     * This is called when the user releases the mouse after Shift-dragging.
     */
    fun finishRectangularSelection() {
        val bounds = selectionState.selectionBoxBounds
        if (bounds != null) {
            // Find all nodes whose centers are inside the selection box
            val nodesToSelect = getNodesInCurrentContext().filter { node ->
                val nodeCenter = calculateNodeCenter(node)
                bounds.contains(nodeCenter)
            }.map { it.id }.toSet()

            // Add selected nodes to existing selection
            addNodesToSelection(nodesToSelect)

            // Find all connections where both endpoints are inside the selection box
            val connectionsToSelect = getConnectionsInCurrentContext().filter { connection ->
                nodesToSelect.contains(connection.sourceNodeId) &&
                nodesToSelect.contains(connection.targetNodeId)
            }.map { it.id }.toSet()

            // Add selected connections to existing selection
            addConnectionsToSelection(connectionsToSelect)
        }

        // Clear selection box state
        selectionState = selectionState.copy(
            selectionBoxStart = null,
            selectionBoxEnd = null,
            isRectangularSelectionActive = false
        )
    }

    /**
     * Cancels the rectangular selection without selecting any nodes.
     * This is called when the user presses Escape or the selection is otherwise cancelled.
     */
    fun cancelRectangularSelection() {
        selectionState = selectionState.copy(
            selectionBoxStart = null,
            selectionBoxEnd = null,
            isRectangularSelectionActive = false
        )
    }

    /**
     * Calculates the center point of a node for hit detection.
     * Uses the node's position plus half its typical dimensions.
     *
     * @param node The node to calculate center for
     * @return The center point in screen coordinates
     */
    private fun calculateNodeCenter(node: Node): Offset {
        // Node dimensions from FlowGraphCanvas rendering
        val nodeWidth = 180f * scale
        val portSpacing = 25f * scale
        val headerHeight = 30f * scale
        val maxPorts = maxOf(
            (node as? CodeNode)?.inputPorts?.size ?: 1,
            (node as? CodeNode)?.outputPorts?.size ?: 1
        ).coerceAtLeast(1)
        val nodeHeight = headerHeight + (maxPorts * portSpacing) + 20f * scale

        // Calculate screen position: graphPos * scale + panOffset
        val screenX = node.position.x.toFloat() * scale + panOffset.x
        val screenY = node.position.y.toFloat() * scale + panOffset.y

        // Center is position + half dimensions
        return Offset(
            screenX + nodeWidth / 2f,
            screenY + nodeHeight / 2f
        )
    }

    // ============================================================================
    // Group/Ungroup Operations
    // ============================================================================

    /**
     * Returns true if the current selection can be grouped (2+ nodes selected).
     */
    fun canGroupSelection(): Boolean {
        return selectionState.selectedNodeIds.size >= 2
    }

    /**
     * Returns true if the current selection can be ungrouped (single GraphNode selected).
     */
    fun canUngroupSelection(): Boolean {
        val selectedIds = selectionState.selectedNodeIds
        if (selectedIds.size != 1) return false
        val node = flowGraph.findNode(selectedIds.first())
        return node is GraphNode
    }

    /**
     * Groups the currently selected nodes into a new GraphNode.
     *
     * This method:
     * 1. Uses GraphNodeFactory to create a GraphNode from selected nodes
     * 2. Removes the selected nodes from the flow graph
     * 3. Adds the new GraphNode to the flow graph
     * 4. Redirects external connections to the GraphNode's ports
     * 5. Clears selection and selects the new GraphNode
     *
     * @return The created GraphNode, or null if grouping was not possible
     */
    fun groupSelectedNodes(): GraphNode? {
        val selectedNodeIds = selectionState.selectedNodeIds
        if (selectedNodeIds.size < 2) {
            return null
        }

        // Create the GraphNode using the factory
        val graphNode = io.codenode.fbpdsl.factory.GraphNodeFactory.createFromSelection(
            selectedNodeIds = selectedNodeIds,
            allNodes = flowGraph.rootNodes,
            allConnections = flowGraph.connections,
            graphNodeName = "Group"
        ) ?: return null

        // Get the port mappings for connection redirection
        val portMappings = io.codenode.fbpdsl.factory.GraphNodeFactory.generatePortMappings(
            selectedNodeIds = selectedNodeIds,
            allConnections = flowGraph.connections
        )

        // Build a lookup from child port (nodeId_portId) to actual GraphNode port ID
        // This ensures we use the correct port IDs that were created on the GraphNode
        val childPortToGraphNodePort = mutableMapOf<String, String>()
        graphNode.portMappings.forEach { (portName, mapping) ->
            val childKey = "${mapping.childNodeId}_${mapping.childPortName}"
            // Find the actual port ID by matching the port name
            val port = (graphNode.inputPorts + graphNode.outputPorts).find { it.name == portName }
            if (port != null) {
                childPortToGraphNodePort[childKey] = port.id
            }
        }

        // Build a map of external connection ID to GraphNode port info for redirection
        val connectionRedirects = portMappings.associate { mapping ->
            val childKey = "${mapping.childNodeId}_${mapping.childPortId}"
            val actualPortId = childPortToGraphNodePort[childKey] ?: mapping.graphNodePortName
            mapping.externalConnectionId to Triple(
                mapping.graphNodePortDirection,
                graphNode.id,
                actualPortId
            )
        }

        // Remove selected nodes from the graph
        var updatedGraph = flowGraph
        selectedNodeIds.forEach { nodeId ->
            updatedGraph = updatedGraph.removeNode(nodeId)
        }

        // Remove internal connections (they're now inside the GraphNode)
        val internalConnectionIds = flowGraph.connections
            .filter { conn ->
                conn.sourceNodeId in selectedNodeIds && conn.targetNodeId in selectedNodeIds
            }
            .map { it.id }
        internalConnectionIds.forEach { connId ->
            updatedGraph = updatedGraph.removeConnection(connId)
        }

        // Redirect external connections to the GraphNode's ports
        connectionRedirects.forEach { (connId, redirectInfo) ->
            val (direction, graphNodeId, portName) = redirectInfo
            val existingConn = updatedGraph.connections.find { it.id == connId }
            if (existingConn != null) {
                val updatedConn = if (direction == io.codenode.fbpdsl.model.Port.Direction.INPUT) {
                    // Incoming connection: update target to GraphNode
                    existingConn.copy(
                        targetNodeId = graphNodeId,
                        targetPortId = portName
                    )
                } else {
                    // Outgoing connection: update source to GraphNode
                    existingConn.copy(
                        sourceNodeId = graphNodeId,
                        sourcePortId = portName
                    )
                }
                updatedGraph = updatedGraph.removeConnection(connId)
                updatedGraph = updatedGraph.addConnection(updatedConn)
            }
        }

        // Add the GraphNode to the graph
        updatedGraph = updatedGraph.addNode(graphNode)

        // Update the flow graph
        flowGraph = updatedGraph
        isDirty = true

        // Clear selection and select the new GraphNode
        clearSelection()
        toggleNodeInSelection(graphNode.id)

        return graphNode
    }

    /**
     * Ungroups a GraphNode, restoring its child nodes and connections to the parent graph.
     *
     * This method:
     * 1. Validates that the node is a GraphNode
     * 2. Restores child nodes to the root graph with adjusted positions
     * 3. Restores internal connections to the root graph
     * 4. Redirects external connections to the original child ports using port mappings
     * 5. Removes the GraphNode from the graph
     * 6. Selects the restored child nodes
     *
     * @param graphNodeId The ID of the GraphNode to ungroup
     * @return true if ungrouping was successful, false if the node doesn't exist or isn't a GraphNode
     */
    fun ungroupGraphNode(graphNodeId: String): Boolean {
        // Step 1: Validate that the node exists and is a GraphNode
        val node = flowGraph.findNode(graphNodeId)
        if (node !is GraphNode) {
            return false
        }

        val graphNode = node

        // Step 2: Prepare child nodes with cleared parentNodeId
        // Child positions are already absolute (preserved from when they were grouped),
        // so we just clear the parentNodeId without adjusting positions
        val restoredChildNodes = graphNode.childNodes.map { childNode ->
            when (childNode) {
                is CodeNode -> childNode.copy(parentNodeId = null)
                is GraphNode -> childNode.copy(parentNodeId = null)
                else -> childNode.withParent(null)
            }
        }

        // Step 3: Build updated graph - start by removing the GraphNode
        var updatedGraph = flowGraph.removeNode(graphNodeId)

        // Step 4: Add restored child nodes to the graph
        restoredChildNodes.forEach { childNode ->
            updatedGraph = updatedGraph.addNode(childNode)
        }

        // Step 5: Restore internal connections to root graph
        graphNode.internalConnections.forEach { internalConn ->
            updatedGraph = updatedGraph.addConnection(internalConn)
        }

        // Step 6: Redirect external connections using port mappings
        // Find all connections that reference the GraphNode and redirect them
        val connectionsToRedirect = flowGraph.connections.filter { conn ->
            conn.sourceNodeId == graphNodeId || conn.targetNodeId == graphNodeId
        }

        connectionsToRedirect.forEach { conn ->
            // Remove the old connection (it references the GraphNode)
            updatedGraph = updatedGraph.removeConnection(conn.id)

            // Determine the new connection based on port mappings
            val redirectedConn = if (conn.targetNodeId == graphNodeId) {
                // Incoming connection: GraphNode is target, redirect to child's input port
                val portMapping = graphNode.portMappings[conn.targetPortId]
                if (portMapping != null) {
                    conn.copy(
                        targetNodeId = portMapping.childNodeId,
                        targetPortId = portMapping.childPortName
                    )
                } else {
                    // No mapping found, skip this connection
                    null
                }
            } else if (conn.sourceNodeId == graphNodeId) {
                // Outgoing connection: GraphNode is source, redirect from child's output port
                val portMapping = graphNode.portMappings[conn.sourcePortId]
                if (portMapping != null) {
                    conn.copy(
                        sourceNodeId = portMapping.childNodeId,
                        sourcePortId = portMapping.childPortName
                    )
                } else {
                    // No mapping found, skip this connection
                    null
                }
            } else {
                null
            }

            // Add the redirected connection if we found a mapping
            if (redirectedConn != null) {
                updatedGraph = updatedGraph.addConnection(redirectedConn)
            }
        }

        // Step 7: Update the flow graph
        flowGraph = updatedGraph

        // Step 8: Clear selection and select restored child nodes
        clearSelection()
        restoredChildNodes.forEach { childNode ->
            toggleNodeInSelection(childNode.id)
        }

        // Step 9: Mark as dirty
        isDirty = true

        return true
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

    /**
     * Navigates into a GraphNode to view its internal structure.
     * Updates the navigation context to show the GraphNode's children.
     *
     * @param graphNodeId The ID of the GraphNode to navigate into
     * @return true if navigation was successful, false if the node doesn't exist or isn't a GraphNode
     */
    fun navigateIntoGraphNode(graphNodeId: String): Boolean {
        val node = flowGraph.findNode(graphNodeId)
        if (node !is GraphNode) {
            return false
        }
        navigationContext = navigationContext.pushInto(graphNodeId)
        clearSelection()
        return true
    }

    /**
     * Navigates out of the current GraphNode to its parent level.
     * If at root, this has no effect.
     *
     * @return true if navigation occurred, false if already at root
     */
    fun navigateOut(): Boolean {
        if (navigationContext.isAtRoot) {
            return false
        }
        navigationContext = navigationContext.popOut()
        clearSelection()
        return true
    }

    /**
     * Resets navigation to the root FlowGraph level.
     */
    fun navigateToRoot() {
        navigationContext = navigationContext.reset()
        clearSelection()
    }

    /**
     * Gets the name of the currently viewed GraphNode, or null if at root.
     */
    fun getCurrentGraphNodeName(): String? {
        val currentId = navigationContext.currentGraphNodeId ?: return null
        val node = flowGraph.findNode(currentId) as? GraphNode
        return node?.name
    }

    /**
     * Gets a map of GraphNode IDs to their names for breadcrumb display.
     * Includes all GraphNodes in the current navigation path.
     *
     * @return Map of GraphNode ID to display name
     */
    fun getGraphNodeNamesInPath(): Map<String, String> {
        return navigationContext.path.mapNotNull { nodeId ->
            val node = flowGraph.findNode(nodeId) as? GraphNode
            node?.let { nodeId to it.name }
        }.toMap()
    }

    /**
     * Navigates to a specific depth in the navigation path.
     * Depth 0 navigates to root, depth 1 navigates to inside the first GraphNode, etc.
     *
     * @param targetDepth The target depth to navigate to
     * @return true if navigation was performed, false if already at that depth
     */
    fun navigateToDepth(targetDepth: Int): Boolean {
        val currentDepth = navigationContext.depth
        if (targetDepth >= currentDepth) {
            return false  // Can't navigate forward with this method
        }
        if (targetDepth < 0) {
            return false  // Invalid depth
        }

        // Pop out until we reach the target depth
        while (navigationContext.depth > targetDepth) {
            navigationContext = navigationContext.popOut()
        }
        clearSelection()
        return true
    }

    /**
     * Gets the current GraphNode being viewed, or null if at root level.
     * Used for boundary rendering in the internal view.
     *
     * @return The current GraphNode or null if at root
     */
    fun getCurrentGraphNode(): GraphNode? {
        val currentId = navigationContext.currentGraphNodeId ?: return null
        return flowGraph.findNode(currentId) as? GraphNode
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
