/*
 * GraphEditor Main Entry Point
 * Compose Desktop visual graph editor for Flow-Based Programming
 * License: Apache 2.0
 */

package io.codenode.grapheditor

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.grapheditor.state.GraphState
import io.codenode.grapheditor.state.SelectableElement
import io.codenode.grapheditor.state.rememberUndoRedoManager
import io.codenode.grapheditor.state.AddNodeCommand
import io.codenode.grapheditor.state.MoveNodeCommand
import io.codenode.grapheditor.state.AddConnectionCommand
import io.codenode.grapheditor.state.RemoveNodeCommand
import io.codenode.grapheditor.state.GroupNodesCommand
import io.codenode.grapheditor.state.UngroupNodeCommand
import io.codenode.grapheditor.ui.CompactCanvasControls
import io.codenode.grapheditor.ui.ConnectionErrorDisplay
import io.codenode.grapheditor.ui.FlowGraphCanvas
import io.codenode.grapheditor.ui.NodePalette
import io.codenode.grapheditor.ui.GraphEditorWithToggle
import io.codenode.grapheditor.ui.ViewMode
import io.codenode.grapheditor.ui.CompactPropertiesPanel
import io.codenode.grapheditor.ui.PropertiesPanelState
import io.codenode.grapheditor.state.rememberPropertyChangeTracker
import io.codenode.grapheditor.serialization.FlowKtParser
import io.codenode.grapheditor.save.ModuleSaveService
import io.codenode.fbpdsl.model.NodeTypeDefinition
import io.codenode.fbpdsl.model.PortTemplate
import io.codenode.fbpdsl.model.Port
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.factory.getCommonGenericNodeTypes
import io.codenode.fbpdsl.model.InformationPacketType
import io.codenode.grapheditor.state.IPTypeRegistry
import io.codenode.grapheditor.ui.IPPalette
import io.codenode.grapheditor.ui.ConnectionContextMenu
import io.codenode.grapheditor.ui.NavigationBreadcrumbBar
import io.codenode.grapheditor.ui.NavigationZoomOutButton
import io.codenode.grapheditor.compilation.CompilationService
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Creates sample node types for the palette
 * Includes both specialized node types and common generic node types
 */
fun createSampleNodeTypes(): List<NodeTypeDefinition> {
    // Add common generic node types (in0out1, in1out0, in1out1, in1out2, in2out1)
    val genericTypes = getCommonGenericNodeTypes()

    val specializedTypes = listOf(
        NodeTypeDefinition(
            id = "nodeType_generator",
            name = "Data Generator",
            category = NodeTypeDefinition.NodeCategory.SERVICE,
            description = "Generates or loads data into the flow",
            portTemplates = listOf(
                PortTemplate(
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    description = "Data output stream"
                )
            ),
            defaultConfiguration = mapOf(
                "intervalMs" to "1000",
                "maxItems" to "100",
                "autoStart" to "true"
            ),
            configurationSchema = """
                {
                    "type": "object",
                    "properties": {
                        "intervalMs": {"type": "integer", "minimum": 100, "maximum": 60000},
                        "maxItems": {"type": "integer", "minimum": 1, "maximum": 10000},
                        "autoStart": {"type": "boolean"}
                    }
                }
            """.trimIndent()
        ),
        NodeTypeDefinition(
            id = "nodeType_transformer",
            name = "Transform",
            category = NodeTypeDefinition.NodeCategory.TRANSFORMER,
            description = "Transforms data from one format to another",
            portTemplates = listOf(
                PortTemplate(
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    description = "Data input"
                ),
                PortTemplate(
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    description = "Transformed data output"
                )
            ),
            defaultConfiguration = mapOf(
                "outputFormat" to "json",
                "preserveOrder" to "true"
            ),
            configurationSchema = """
                {
                    "type": "object",
                    "properties": {
                        "outputFormat": {"type": "string", "enum": ["json", "xml", "csv"]},
                        "preserveOrder": {"type": "boolean"}
                    }
                }
            """.trimIndent()
        ),
        NodeTypeDefinition(
            id = "nodeType_filter",
            name = "Filter",
            category = NodeTypeDefinition.NodeCategory.TRANSFORMER,
            description = "Filters data based on conditions",
            portTemplates = listOf(
                PortTemplate(
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = Any::class,
                    description = "Input data"
                ),
                PortTemplate(
                    name = "passed",
                    direction = Port.Direction.OUTPUT,
                    dataType = Any::class,
                    description = "Data that passed filter"
                ),
                PortTemplate(
                    name = "rejected",
                    direction = Port.Direction.OUTPUT,
                    dataType = Any::class,
                    description = "Data that failed filter"
                )
            ),
            defaultConfiguration = mapOf(
                "filterField" to "status",
                "filterValue" to "active",
                "caseSensitive" to "false"
            ),
            configurationSchema = """
                {
                    "type": "object",
                    "properties": {
                        "filterField": {"type": "string"},
                        "filterValue": {"type": "string"},
                        "caseSensitive": {"type": "boolean"}
                    },
                    "required": ["filterField", "filterValue"]
                }
            """.trimIndent()
        ),
        NodeTypeDefinition(
            id = "nodeType_api",
            name = "API Call",
            category = NodeTypeDefinition.NodeCategory.API_ENDPOINT,
            description = "Makes HTTP API requests",
            portTemplates = listOf(
                PortTemplate(
                    name = "request",
                    direction = Port.Direction.INPUT,
                    dataType = Any::class,
                    description = "Request data"
                ),
                PortTemplate(
                    name = "response",
                    direction = Port.Direction.OUTPUT,
                    dataType = Any::class,
                    description = "Response data"
                )
            ),
            defaultConfiguration = mapOf(
                "url" to "https://api.example.com",
                "method" to "GET",
                "timeout" to "30"
            ),
            configurationSchema = """
                {
                    "type": "object",
                    "properties": {
                        "url": {"type": "string"},
                        "method": {"type": "string", "enum": ["GET", "POST", "PUT", "DELETE"]},
                        "timeout": {"type": "integer", "minimum": 1, "maximum": 120}
                    },
                    "required": ["url"]
                }
            """.trimIndent()
        ),
        NodeTypeDefinition(
            id = "nodeType_database",
            name = "Database Query",
            category = NodeTypeDefinition.NodeCategory.DATABASE,
            description = "Executes database queries",
            portTemplates = listOf(
                PortTemplate(
                    name = "query",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    description = "SQL query"
                ),
                PortTemplate(
                    name = "results",
                    direction = Port.Direction.OUTPUT,
                    dataType = Any::class,
                    description = "Query results"
                )
            ),
            defaultConfiguration = mapOf(
                "connectionString" to "jdbc:postgresql://localhost:5432/db",
                "maxResults" to "100",
                "timeout" to "30"
            ),
            configurationSchema = """
                {
                    "type": "object",
                    "properties": {
                        "connectionString": {"type": "string"},
                        "maxResults": {"type": "integer", "minimum": 1, "maximum": 10000},
                        "timeout": {"type": "integer", "minimum": 1, "maximum": 300}
                    },
                    "required": ["connectionString"]
                }
            """.trimIndent()
        )
    )

    // Return specialized types first, then generic types
    return specializedTypes + genericTypes
}

/**
 * Main composable for the GraphEditor application
 * Integrates all components: canvas, palette, properties, controls, and error display
 */
@Composable
fun GraphEditorApp(modifier: Modifier = Modifier) {
    // Initialize state with empty graph
    val initialGraph = remember {
        flowGraph(name = "New Graph", version = "1.0.0") {
            // Empty graph
        }
    }
    val graphState = remember { GraphState(initialGraph) }
    val undoRedoManager = rememberUndoRedoManager()
    val propertyChangeTracker = rememberPropertyChangeTracker(undoRedoManager, graphState)
    val nodeTypes = remember { createSampleNodeTypes() }
    val ipTypeRegistry = remember { IPTypeRegistry.withDefaults() }
    val ipTypes = remember { ipTypeRegistry.getAllTypes() }
    var selectedIPType by remember { mutableStateOf<InformationPacketType?>(null) }
    var showOpenDialog by remember { mutableStateOf(false) }
    var showCompileDialog by remember { mutableStateOf(false) }
    var showModuleSaveDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Ready - Create a new graph or open an existing one") }
    val compilationService = remember { CompilationService() }
    val moduleSaveService = remember { ModuleSaveService() }

    // Derive button states from selection - these update automatically when selection changes
    val selectionState = graphState.selectionState  // Read selection state to ensure reactivity
    val canGroup = selectionState.selectedNodeIds.size >= 2
    val canUngroup = selectionState.selectedNodeIds.size == 1 &&
        graphState.flowGraph.findNode(selectionState.selectedNodeIds.firstOrNull() ?: "") is io.codenode.fbpdsl.model.GraphNode

    // Navigation state for GraphNode drill-down
    val navigationContext = graphState.navigationContext
    val isInsideGraphNode = !navigationContext.isAtRoot
    val currentGraphNodeName = graphState.getCurrentGraphNodeName()

    MaterialTheme {
        Column(modifier = modifier.fillMaxSize()) {
            // Top toolbar
            TopToolbar(
                undoRedoManager = undoRedoManager,
                canGroup = canGroup,
                canUngroup = canUngroup,
                isInsideGraphNode = isInsideGraphNode,
                currentGraphNodeName = currentGraphNodeName,
                onNew = {
                    val newGraph = flowGraph(
                        name = "New Graph",
                        version = "1.0.0"
                    ) {}
                    graphState.setGraph(newGraph, markDirty = false)
                    graphState.navigateToRoot()
                    statusMessage = "New graph created"
                },
                onOpen = { showOpenDialog = true },
                onSave = { showModuleSaveDialog = true },
                onUndo = {
                    if (undoRedoManager.undo(graphState)) {
                        statusMessage = "Undo: ${undoRedoManager.getRedoDescription() ?: "action"}"
                    }
                },
                onRedo = {
                    if (undoRedoManager.redo(graphState)) {
                        statusMessage = "Redo: ${undoRedoManager.getUndoDescription() ?: "action"}"
                    }
                },
                onGroup = {
                    val selectedIds = graphState.selectionState.selectedNodeIds.toSet()
                    if (selectedIds.size >= 2) {
                        val command = GroupNodesCommand(selectedIds)
                        undoRedoManager.execute(command, graphState)
                        statusMessage = "Created group from ${selectedIds.size} nodes"
                    }
                },
                onUngroup = {
                    val selectedId = graphState.selectionState.selectedNodeIds.firstOrNull()
                    if (selectedId != null) {
                        val command = UngroupNodeCommand(selectedId)
                        undoRedoManager.execute(command, graphState)
                        val childCount = graphState.selectionState.selectedNodeIds.size
                        statusMessage = "Ungrouped into $childCount node${if (childCount != 1) "s" else ""}"
                    }
                },
                onNavigateBack = {
                    if (graphState.navigateOut()) {
                        statusMessage = "Navigated back to parent"
                    }
                },
                onCompile = {
                    showCompileDialog = true
                }
            )

            Divider()

            // Navigation breadcrumb bar (only visible when inside a GraphNode)
            NavigationBreadcrumbBar(
                navigationContext = graphState.navigationContext,
                graphNodeNames = graphState.getGraphNodeNamesInPath(),
                onNavigateToRoot = {
                    graphState.navigateToRoot()
                    statusMessage = "Navigated to root"
                },
                onNavigateToLevel = { depth ->
                    graphState.navigateToDepth(depth)
                    val name = graphState.getCurrentGraphNodeName() ?: "parent"
                    statusMessage = "Navigated to $name"
                }
            )

            // Focus requester for keyboard handling
            val focusRequester = remember { FocusRequester() }

            // Request focus when composition completes
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            // Main content area with keyboard handling
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            when (keyEvent.key) {
                                Key.Delete, Key.Backspace -> {
                                    // Delete selected node or connection
                                    val nodeId = graphState.selectedNodeId
                                    val connectionIds = graphState.selectedConnectionIds

                                    if (nodeId != null) {
                                        val command = RemoveNodeCommand(nodeId)
                                        undoRedoManager.execute(command, graphState)
                                        graphState.selectNode(null)
                                        statusMessage = "Deleted node"
                                        true
                                    } else if (connectionIds.isNotEmpty()) {
                                        // Delete selected connections
                                        connectionIds.forEach { connectionId ->
                                            graphState.removeConnection(connectionId)
                                        }
                                        graphState.clearSelection()
                                        statusMessage = "Deleted connection"
                                        true
                                    } else {
                                        false
                                    }
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
            ) {
                // Layout: NodePalette on left, Canvas on right
                Row(modifier = Modifier.fillMaxSize()) {
                    // Node Palette
                    NodePalette(
                        nodeTypes = nodeTypes,
                        onNodeSelected = { nodeType ->
                            // Clear IP type selection when working with nodes
                            selectedIPType = null
                            // Create a new node from the selected type
                            // Offset each new node so they don't stack on top of each other
                            val nodeId = "node_${System.currentTimeMillis()}"
                            val nodeCount = graphState.flowGraph.rootNodes.size
                            val xOffset = 300.0 + (nodeCount % 3) * 150.0  // 3 nodes per row
                            val yOffset = 200.0 + (nodeCount / 3) * 100.0  // New row every 3 nodes
                            val newNode = CodeNode(
                                id = nodeId,
                                name = nodeType.name,
                                codeNodeType = CodeNodeType.CUSTOM,
                                description = nodeType.description,
                                position = io.codenode.fbpdsl.model.Node.Position(xOffset, yOffset),
                                inputPorts = nodeType.getInputPortTemplates().map { template ->
                                    Port(
                                        id = "port_${System.currentTimeMillis()}_${template.name}",
                                        name = template.name,
                                        direction = template.direction,
                                        dataType = template.dataType,
                                        owningNodeId = nodeId
                                    )
                                },
                                outputPorts = nodeType.getOutputPortTemplates().map { template ->
                                    Port(
                                        id = "port_${System.currentTimeMillis()}_${template.name}",
                                        name = template.name,
                                        direction = template.direction,
                                        dataType = template.dataType,
                                        owningNodeId = nodeId
                                    )
                                },
                                configuration = nodeType.defaultConfiguration
                            )
                            // Use undo/redo manager to execute the command
                            val command = AddNodeCommand(newNode, Offset(xOffset.toFloat(), yOffset.toFloat()))
                            undoRedoManager.execute(command, graphState)
                            statusMessage = "Added ${nodeType.name} node"
                        }
                    )

                    // IP Palette
                    IPPalette(
                        ipTypes = ipTypes,
                        selectedTypeId = selectedIPType?.id,
                        onTypeSelected = { ipType ->
                            selectedIPType = ipType
                            statusMessage = "Selected IP type: ${ipType.typeName}"
                        }
                    )

                    // Compute connection colors based on IP types
                    val connectionColors: Map<String, Color> = remember(graphState.flowGraph.connections, ipTypeRegistry) {
                        val colorMap = mutableMapOf<String, Color>()
                        graphState.flowGraph.connections.forEach { connection ->
                            connection.ipTypeId?.let { typeId ->
                                ipTypeRegistry.getById(typeId)?.let { ipType ->
                                    val ipColor = ipType.color
                                    colorMap[connection.id] = Color(
                                        red = ipColor.red / 255f,
                                        green = ipColor.green / 255f,
                                        blue = ipColor.blue / 255f
                                    )
                                }
                            }
                        }
                        colorMap
                    }

                    // Compute boundary port colors for interior view of GraphNodes
                    // Maps boundary port ID to color from the parent-level connection's IP type
                    val currentGraphNode = graphState.getCurrentGraphNode()
                    val boundaryConnectionColors: Map<String, Color> = remember(
                        graphState.flowGraph.connections,
                        ipTypeRegistry,
                        currentGraphNode?.id
                    ) {
                        if (currentGraphNode == null) {
                            emptyMap()
                        } else {
                            val colorMap = mutableMapOf<String, Color>()
                            graphState.flowGraph.connections.forEach { connection ->
                                // Check if this connection targets the current GraphNode (input boundary)
                                if (connection.targetNodeId == currentGraphNode.id) {
                                    connection.ipTypeId?.let { typeId ->
                                        ipTypeRegistry.getById(typeId)?.let { ipType ->
                                            val ipColor = ipType.color
                                            colorMap[connection.targetPortId] = Color(
                                                red = ipColor.red / 255f,
                                                green = ipColor.green / 255f,
                                                blue = ipColor.blue / 255f
                                            )
                                        }
                                    }
                                }
                                // Check if this connection sources from the current GraphNode (output boundary)
                                if (connection.sourceNodeId == currentGraphNode.id) {
                                    connection.ipTypeId?.let { typeId ->
                                        ipTypeRegistry.getById(typeId)?.let { ipType ->
                                            val ipColor = ipType.color
                                            colorMap[connection.sourcePortId] = Color(
                                                red = ipColor.red / 255f,
                                                green = ipColor.green / 255f,
                                                blue = ipColor.blue / 255f
                                            )
                                        }
                                    }
                                }
                            }
                            colorMap
                        }
                    }

                    // Main Canvas with View Toggle (Visual/Textual/Split)
                    GraphEditorWithToggle(
                        flowGraph = graphState.flowGraph,
                        initialMode = ViewMode.VISUAL,
                        overrideText = selectedIPType?.toCode(),
                        overrideTitle = selectedIPType?.let { "IP Type: ${it.typeName}" },
                        onVisualViewContent = {
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
                                    statusMessage = if (nodeId != null) "Selected node" else ""
                                },
                                onConnectionSelected = { connectionId ->
                                    if (connectionId != null) {
                                        graphState.selectConnection(connectionId)
                                        statusMessage = "Selected connection"
                                    } else if (graphState.selectedConnectionIds.isNotEmpty()) {
                                        graphState.clearSelection()
                                        statusMessage = ""
                                    }
                                },
                                onElementShiftClicked = { element ->
                                    // Toggle element in selection (unified for nodes and connections)
                                    graphState.toggleElementInSelection(element)
                                    val count = graphState.selectionState.totalSelectionCount
                                    statusMessage = "$count item${if (count != 1) "s" else ""} selected"
                                },
                                onEmptyCanvasClicked = {
                                    // Clear all selections when clicking empty canvas
                                    graphState.clearSelection()
                                    statusMessage = ""
                                },
                                onConnectionRightClick = { connectionId, position ->
                                    graphState.showConnectionContextMenu(connectionId, position)
                                },
                                onNodeMoved = { nodeId, newX, newY ->
                                    // Get old position before moving
                                    val node = graphState.flowGraph.findNode(nodeId)
                                    val oldPosition = if (node is CodeNode) {
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
                                    statusMessage = "Moved node"
                                },
                                onConnectionCreated = { connection ->
                                    // Create and execute add connection command
                                    val command = AddConnectionCommand(connection)
                                    try {
                                        undoRedoManager.execute(command, graphState)
                                        statusMessage = "Created connection"
                                    } catch (e: Exception) {
                                        statusMessage = graphState.errorMessage ?: "Failed to create connection"
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
                                        statusMessage = "Selected $newlySelected node${if (newlySelected > 1) "s" else ""}"
                                    }
                                },
                                // GraphNode navigation
                                onGraphNodeExpandClicked = { graphNodeId ->
                                    if (graphState.navigateIntoGraphNode(graphNodeId)) {
                                        val nodeName = graphState.getCurrentGraphNodeName() ?: graphNodeId
                                        statusMessage = "Navigated into: $nodeName"
                                    }
                                },
                                // Track canvas size for auto-centering
                                onCanvasSizeChanged = { size ->
                                    graphState.updateCanvasSize(size)
                                },
                                displayNodes = graphState.getNodesInCurrentContext(),
                                displayConnections = graphState.getConnectionsInCurrentContext(),
                                currentGraphNode = graphState.getCurrentGraphNode()
                            )

                            // Connection Context Menu (rendered as overlay)
                            graphState.connectionContextMenu?.let { menuState ->
                                ConnectionContextMenu(
                                    connectionId = menuState.connectionId,
                                    position = menuState.position,
                                    ipTypes = ipTypeRegistry.getAllTypes(),
                                    currentTypeId = menuState.currentTypeId,
                                    onTypeSelected = { connId, typeId ->
                                        graphState.updateConnectionIPType(connId, typeId)
                                        statusMessage = "Changed connection IP type"
                                    },
                                    onDismiss = {
                                        graphState.hideConnectionContextMenu()
                                    }
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Properties Panel (right side) - shows when exactly one element is selected
                    // When multiple elements are selected, panel should be empty
                    val hasSingleSelection = graphState.selectionState.totalSelectionCount == 1

                    val selectedNode = if (hasSingleSelection) {
                        graphState.selectedNodeId?.let { nodeId ->
                            graphState.flowGraph.findNode(nodeId) as? CodeNode
                        }
                    } else null

                    val selectedConnection = if (hasSingleSelection) {
                        graphState.selectedConnectionIds.firstOrNull()?.let { connectionId ->
                            graphState.getConnectionsInCurrentContext().find { it.id == connectionId }
                        }
                    } else null

                    CompactPropertiesPanel(
                        selectedNode = selectedNode,
                        selectedConnection = selectedConnection,
                        flowGraph = graphState.flowGraph,
                        propertyDefinitions = selectedNode?.let { node ->
                            // Derive property definitions from node type or use defaults
                            nodeTypes.find { it.name == node.name }?.let { nodeType ->
                                PropertiesPanelState.derivePropertyDefinitions(nodeType)
                            } ?: emptyList()
                        } ?: emptyList(),
                        onNodeNameChanged = { newName ->
                            selectedNode?.let { node ->
                                graphState.updateNodeName(node.id, newName)
                                statusMessage = "Renamed node to: $newName"
                            }
                        },
                        onPropertyChanged = { key, value ->
                            selectedNode?.let { node ->
                                val oldValue = node.configuration[key] ?: ""
                                propertyChangeTracker.trackChange(node.id, key, oldValue, value)
                                statusMessage = "Updated property: $key"
                            }
                        },
                        onPortNameChanged = { portId, newName ->
                            selectedNode?.let { node ->
                                graphState.updatePortName(node.id, portId, newName)
                                statusMessage = "Renamed port to: $newName"
                            }
                        }
                    )
                }

                // Canvas controls overlay (bottom right)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    CompactCanvasControls(
                        graphState = graphState,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Navigation zoom-out button overlay (bottom left) - only visible when inside a GraphNode
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomStart
                ) {
                    NavigationZoomOutButton(
                        enabled = isInsideGraphNode,
                        currentGraphNodeName = currentGraphNodeName,
                        onClick = {
                            if (graphState.navigateOut()) {
                                statusMessage = "Navigated back to parent"
                            }
                        },
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Error display overlay (top center)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    ConnectionErrorDisplay(
                        graphState = graphState,
                        modifier = Modifier.padding(16.dp).widthIn(max = 500.dp)
                    )
                }
            }

            Divider()

            // Status bar
            StatusBar(
                message = statusMessage,
                nodeCount = graphState.flowGraph.rootNodes.size,
                connectionCount = graphState.flowGraph.connections.size,
                selectionCount = graphState.selectionState.totalSelectionCount
            )
        }

        // File dialogs
        if (showOpenDialog) {
            LaunchedEffect(Unit) {
                val file = showFileOpenDialog()
                if (file != null) {
                    try {
                        // T062: Only support .flow.kt files (removed .flow.kts support)
                        val parser = FlowKtParser()
                        val parseResult = parser.parseFlowKt(file.readText())
                        if (parseResult.isSuccess && parseResult.graph != null) {
                            graphState.setGraph(parseResult.graph, markDirty = false)
                            statusMessage = "Opened ${file.name}"
                        } else {
                            statusMessage = "Error opening: ${parseResult.errorMessage}"
                        }
                    } catch (e: Exception) {
                        statusMessage = "Error opening: ${e.message}"
                    }
                }
                showOpenDialog = false
            }
        }

        if (showCompileDialog) {
            LaunchedEffect(Unit) {
                val outputDir = showDirectoryChooser()
                if (outputDir != null) {
                    val result = compilationService.compileToModule(
                        flowGraph = graphState.flowGraph,
                        outputDir = outputDir
                    )
                    if (result.success) {
                        statusMessage = "Compiled ${result.fileCount} files to ${result.outputPath}"
                    } else {
                        statusMessage = "Compile error: ${result.errorMessage}"
                    }
                }
                showCompileDialog = false
            }
        }

        // T010: Module Save dialog handler
        if (showModuleSaveDialog) {
            LaunchedEffect(Unit) {
                val outputDir = showDirectoryChooser("Save Module To")
                if (outputDir != null) {
                    val result = moduleSaveService.saveModule(
                        flowGraph = graphState.flowGraph,
                        outputDir = outputDir
                    )
                    if (result.success) {
                        statusMessage = "Module saved to ${result.moduleDir?.name}"
                    } else {
                        statusMessage = "Save error: ${result.errorMessage}"
                    }
                }
                showModuleSaveDialog = false
            }
        }
    }
}

/**
 * Top toolbar with file operations, undo/redo, group/ungroup, and navigation
 */
@Composable
fun TopToolbar(
    undoRedoManager: io.codenode.grapheditor.state.UndoRedoManager,
    canGroup: Boolean = false,
    canUngroup: Boolean = false,
    isInsideGraphNode: Boolean = false,
    currentGraphNodeName: String? = null,
    onNew: () -> Unit,
    onOpen: () -> Unit,
    onSave: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onGroup: () -> Unit = {},
    onUngroup: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onCompile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(56.dp),
        color = Color(0xFF2196F3),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Back button (only visible when inside a GraphNode)
            if (isInsideGraphNode) {
                TextButton(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text("\u2190 Back")  // Left arrow
                }

                Divider(
                    modifier = Modifier.width(1.dp).height(32.dp),
                    color = Color.White.copy(alpha = 0.3f)
                )
            }

            // Title with optional breadcrumb
            Text(
                text = if (isInsideGraphNode && currentGraphNodeName != null) {
                    "Inside: $currentGraphNodeName"
                } else {
                    "CodeNodeIO Graph Editor"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.weight(1f))

            // File operations
            TextButton(
                onClick = onNew,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("New")
            }

            TextButton(
                onClick = onOpen,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Open")
            }

            TextButton(
                onClick = onSave,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Save")
            }

            Divider(
                modifier = Modifier.width(1.dp).height(32.dp),
                color = Color.White.copy(alpha = 0.3f)
            )

            // Undo/Redo
            TextButton(
                onClick = onUndo,
                enabled = undoRedoManager.canUndo,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Undo")
            }

            TextButton(
                onClick = onRedo,
                enabled = undoRedoManager.canRedo,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Redo")
            }

            Divider(
                modifier = Modifier.width(1.dp).height(32.dp),
                color = Color.White.copy(alpha = 0.3f)
            )

            // Group/Ungroup
            TextButton(
                onClick = onGroup,
                enabled = canGroup,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Group")
            }

            TextButton(
                onClick = onUngroup,
                enabled = canUngroup,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Ungroup")
            }

            Divider(
                modifier = Modifier.width(1.dp).height(32.dp),
                color = Color.White.copy(alpha = 0.3f)
            )

            // Compile
            TextButton(
                onClick = onCompile,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Compile")
            }
        }
    }
}

/**
 * Status bar showing current state
 */
@Composable
fun StatusBar(
    message: String,
    nodeCount: Int,
    connectionCount: Int,
    selectionCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(28.dp),
        color = Color(0xFFF5F5F5),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                fontSize = 12.sp,
                color = Color(0xFF424242)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Selection count badge (only show when 2+ items selected)
            if (selectionCount >= 2) {
                Surface(
                    color = Color(0xFF2196F3),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "$selectionCount selected",
                        fontSize = 11.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "Nodes: $nodeCount",
                fontSize = 12.sp,
                color = Color(0xFF757575)
            )

            Text(
                text = "Connections: $connectionCount",
                fontSize = 12.sp,
                color = Color(0xFF757575)
            )
        }
    }
}

/**
 * Show file open dialog for .flow.kt files
 */
fun showFileOpenDialog(): File? {
    val fileChooser = JFileChooser().apply {
        dialogTitle = "Open Flow Graph"
        // T062: Only accept .flow.kt (compiled) files
        fileFilter = FileNameExtensionFilter("Flow Graph Files (*.flow.kt)", "kt")
    }

    return if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        fileChooser.selectedFile
    } else {
        null
    }
}

/**
 * Show directory chooser for module compilation output
 *
 * @param title Dialog title (default: "Select Output Directory for KMP Module")
 */
fun showDirectoryChooser(title: String = "Select Output Directory for KMP Module"): File? {
    val fileChooser = JFileChooser().apply {
        dialogTitle = title
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isAcceptAllFileFilterUsed = false
    }

    return if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        fileChooser.selectedFile
    } else {
        null
    }
}

/**
 * Preview of the GraphEditor app
 */
@Preview
@Composable
fun GraphEditorAppPreview() {
    MaterialTheme {
        GraphEditorApp()
    }
}

/**
 * Main entry point for the standalone Graph Editor application
 */
fun main() = application {
    val windowState = rememberWindowState(
        width = 1400.dp,
        height = 900.dp
    )

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "CodeNodeIO Graph Editor - Visual Flow-Based Programming"
    ) {
        GraphEditorApp()
    }
}
