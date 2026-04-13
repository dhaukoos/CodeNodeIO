/*
 * GraphEditorCanvas - Canvas section with FlowGraphCanvas and connection context menu
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.fbpdsl.model.InformationPacketType
import io.codenode.flowgraphexecute.ConnectionAnimation
import io.codenode.flowgraphtypes.registry.IPTypeRegistry
import io.codenode.grapheditor.state.AddConnectionCommand
import io.codenode.grapheditor.state.GraphState
import io.codenode.grapheditor.state.MoveNodeCommand
import io.codenode.grapheditor.state.UndoRedoManager
import io.codenode.flowgraphinspect.viewmodel.IPPaletteViewModel

/**
 * Renders the FlowGraphCanvas and connection context menu overlay.
 *
 * Encapsulates all canvas callbacks: node selection, connection creation,
 * node movement, rectangular selection, GraphNode navigation, and connection
 * context menu handling.
 */
@Composable
fun GraphEditorCanvasSection(
    graphState: GraphState,
    undoRedoManager: UndoRedoManager,
    connectionColors: Map<String, androidx.compose.ui.graphics.Color>,
    boundaryConnectionColors: Map<String, androidx.compose.ui.graphics.Color>,
    ipTypeRegistry: IPTypeRegistry,
    ipPaletteViewModel: IPPaletteViewModel,
    focusRequester: FocusRequester,
    activeAnimations: List<ConnectionAnimation>,
    onStatusMessage: (String) -> Unit,
    onSelectedIPTypeCleared: () -> Unit,
) {
    FlowGraphCanvas(
        flowGraph = graphState.flowGraph,
        selectedNodeId = graphState.selectedNodeId,
        selectedConnectionIds = graphState.selectedConnectionIds,
        multiSelectedNodeIds = graphState.selectionState.selectedNodeIds,
        connectionColors = connectionColors,
        boundaryConnectionColors = boundaryConnectionColors,
        scale = graphState.scale,
        panOffset = graphState.panOffset,
        onScaleChanged = { newScale ->
            graphState.updateScale(newScale)
        },
        onPanOffsetChanged = { newOffset ->
            graphState.updatePanOffset(newOffset)
        },
        onNodeSelected = { nodeId ->
            // Clear multi-selection when doing single selection
            graphState.clearSelection()
            graphState.selectNode(nodeId)
            graphState.hideConnectionContextMenu()
            // Clear IP type selection when selecting a node
            if (nodeId != null) {
                onSelectedIPTypeCleared()
                ipPaletteViewModel.clearSelection()
            }
            onStatusMessage(if (nodeId != null) "Selected node" else "")
            // Restore focus so keyboard events (Delete) work
            focusRequester.requestFocus()
        },
        onConnectionSelected = { connectionId ->
            if (connectionId != null) {
                graphState.selectConnection(connectionId)
                // Clear IP type selection when selecting a connection
                onSelectedIPTypeCleared()
                ipPaletteViewModel.clearSelection()
                onStatusMessage("Selected connection")
            } else if (graphState.selectedConnectionIds.isNotEmpty()) {
                graphState.clearSelection()
                onStatusMessage("")
            }
            // Restore focus so keyboard events (Delete) work
            focusRequester.requestFocus()
        },
        onElementShiftClicked = { element ->
            // Toggle element in selection (unified for nodes and connections)
            graphState.toggleElementInSelection(element)
            val count = graphState.selectionState.totalSelectionCount
            onStatusMessage("$count item${if (count != 1) "s" else ""} selected")
            focusRequester.requestFocus()
        },
        onEmptyCanvasClicked = {
            // Clear all selections when clicking empty canvas
            graphState.clearSelection()
            onStatusMessage("")
            focusRequester.requestFocus()
        },
        onConnectionRightClick = { connectionId, position ->
            graphState.showConnectionContextMenu(connectionId, position)
        },
        onNodeMoved = { nodeId, newX, newY ->
            // Get old position before moving
            val node = graphState.flowGraph.findNode(nodeId)
            val oldPosition = if (node != null) {
                Offset(node.position.x.toFloat(), node.position.y.toFloat())
            } else {
                Offset.Zero
            }

            // Create and execute move command
            val command = MoveNodeCommand(
                nodeId,
                oldPosition,
                Offset(newX.toFloat(), newY.toFloat())
            )
            undoRedoManager.execute(command, graphState)
            onStatusMessage("Moved node")
        },
        onConnectionCreated = { connection: Connection ->
            // Create and execute add connection command
            val command = AddConnectionCommand(connection)
            try {
                undoRedoManager.execute(command, graphState)
                onStatusMessage("Created connection")
            } catch (e: Exception) {
                onStatusMessage(graphState.errorMessage ?: "Failed to create connection")
            }
        },
        // Rectangular selection callbacks
        selectionBoxBounds = graphState.selectionState.selectionBoxBounds,
        onRectangularSelectionStart = { position ->
            graphState.startRectangularSelection(position)
        },
        onRectangularSelectionUpdate = { position ->
            graphState.updateRectangularSelection(position)
        },
        onRectangularSelectionFinish = {
            val beforeCount = graphState.selectionState.nodeSelectionCount
            graphState.finishRectangularSelection()
            val afterCount = graphState.selectionState.nodeSelectionCount
            val newlySelected = afterCount - beforeCount
            if (newlySelected > 0) {
                onStatusMessage("Selected $newlySelected node${if (newlySelected > 1) "s" else ""}")
            }
        },
        // GraphNode navigation
        onGraphNodeExpandClicked = { graphNodeId ->
            if (graphState.navigateIntoGraphNode(graphNodeId)) {
                val nodeName = graphState.getCurrentGraphNodeName() ?: graphNodeId
                onStatusMessage("Navigated into: $nodeName")
            }
        },
        // Track canvas size for auto-centering
        onCanvasSizeChanged = { size ->
            graphState.updateCanvasSize(size)
        },
        displayNodes = graphState.getNodesInCurrentContext(),
        displayConnections = graphState.getConnectionsInCurrentContext(),
        currentGraphNode = graphState.getCurrentGraphNode(),
        activeAnimations = activeAnimations
    )

    // Connection Context Menu (rendered as overlay)
    graphState.connectionContextMenu?.let { menuState ->
        ConnectionContextMenu(
            connectionId = menuState.connectionId,
            position = menuState.position,
            ipTypes = ipTypeRegistry.getAllTypes(),
            currentTypeId = menuState.currentTypeId,
            onTypeSelected = { connId, typeId ->
                graphState.updateConnectionIPType(connId, typeId, ipTypeRegistry)
                onStatusMessage("Changed connection IP type")
            },
            onDismiss = {
                graphState.hideConnectionContextMenu()
            }
        )
    }
}
