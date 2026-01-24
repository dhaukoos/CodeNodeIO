/*
 * GraphEditor Main Entry Point
 * Compose Desktop visual graph editor for Flow-Based Programming
 * License: Apache 2.0
 */

package io.codenode.grapheditor

import androidx.compose.desktop.ui.tooling.preview.Preview
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
import io.codenode.grapheditor.state.rememberUndoRedoManager
import io.codenode.grapheditor.state.AddNodeCommand
import io.codenode.grapheditor.state.MoveNodeCommand
import io.codenode.grapheditor.state.AddConnectionCommand
import io.codenode.grapheditor.ui.CanvasControls
import io.codenode.grapheditor.ui.CompactCanvasControls
import io.codenode.grapheditor.ui.ConnectionErrorDisplay
import io.codenode.grapheditor.ui.FlowGraphCanvas
import io.codenode.grapheditor.ui.NodePalette
import io.codenode.grapheditor.serialization.FlowGraphSerializer
import io.codenode.grapheditor.serialization.FlowGraphDeserializer
import io.codenode.fbpdsl.model.NodeTypeDefinition
import io.codenode.fbpdsl.model.PortTemplate
import io.codenode.fbpdsl.model.Port
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Connection
import androidx.compose.ui.geometry.Offset
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Creates sample node types for the palette
 */
fun createSampleNodeTypes(): List<NodeTypeDefinition> {
    return listOf(
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
            )
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
            )
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
            )
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
            )
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
            )
        )
    )
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
    val nodeTypes = remember { createSampleNodeTypes() }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showOpenDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Ready - Create a new graph or open an existing one") }

    MaterialTheme {
        Column(modifier = modifier.fillMaxSize()) {
            // Top toolbar
            TopToolbar(
                undoRedoManager = undoRedoManager,
                onNew = {
                    val newGraph = flowGraph(
                        name = "New Graph",
                        version = "1.0.0"
                    ) {}
                    graphState.setGraph(newGraph, markDirty = false)
                    statusMessage = "New graph created"
                },
                onOpen = { showOpenDialog = true },
                onSave = { showSaveDialog = true },
                onUndo = {
                    if (undoRedoManager.undo(graphState)) {
                        statusMessage = "Undo: ${undoRedoManager.getRedoDescription() ?: "action"}"
                    }
                },
                onRedo = {
                    if (undoRedoManager.redo(graphState)) {
                        statusMessage = "Redo: ${undoRedoManager.getUndoDescription() ?: "action"}"
                    }
                }
            )

            Divider()

            // Main content area
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                // Layout: NodePalette on left, Canvas on right
                Row(modifier = Modifier.fillMaxSize()) {
                    // Node Palette
                    NodePalette(
                        nodeTypes = nodeTypes,
                        onNodeSelected = { nodeType ->
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
                                }
                            )
                            // Use undo/redo manager to execute the command
                            val command = AddNodeCommand(newNode, Offset(xOffset.toFloat(), yOffset.toFloat()))
                            undoRedoManager.execute(command, graphState)
                            statusMessage = "Added ${nodeType.name} node"
                        }
                    )

                    // Main Canvas
                    FlowGraphCanvas(
                        flowGraph = graphState.flowGraph,
                        selectedNodeId = graphState.selectedNodeId,
                        onNodeSelected = { nodeId ->
                            graphState.selectNode(nodeId)
                            statusMessage = if (nodeId != null) "Selected node" else "Deselected"
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
                        modifier = Modifier.weight(1f)
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
                connectionCount = graphState.flowGraph.connections.size
            )
        }

        // File dialogs
        if (showSaveDialog) {
            LaunchedEffect(Unit) {
                val file = showFileSaveDialog()
                if (file != null) {
                    try {
                        val content = FlowGraphSerializer.serialize(graphState.flowGraph)
                        file.writeText(content)
                        statusMessage = "Saved to ${file.name}"
                    } catch (e: Exception) {
                        statusMessage = "Error saving: ${e.message}"
                    }
                }
                showSaveDialog = false
            }
        }

        if (showOpenDialog) {
            LaunchedEffect(Unit) {
                val file = showFileOpenDialog()
                if (file != null) {
                    try {
                        val result = FlowGraphDeserializer.deserializeFromFile(file)
                        if (result.isSuccess && result.graph != null) {
                            graphState.setGraph(result.graph, markDirty = false)
                            statusMessage = "Opened ${file.name}"
                        } else {
                            statusMessage = "Error opening: ${result.errorMessage}"
                        }
                    } catch (e: Exception) {
                        statusMessage = "Error opening: ${e.message}"
                    }
                }
                showOpenDialog = false
            }
        }
    }
}

/**
 * Top toolbar with file operations and undo/redo
 */
@Composable
fun TopToolbar(
    undoRedoManager: io.codenode.grapheditor.state.UndoRedoManager,
    onNew: () -> Unit,
    onOpen: () -> Unit,
    onSave: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
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
            // Title
            Text(
                text = "CodeNodeIO Graph Editor",
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
 * Show file save dialog
 */
fun showFileSaveDialog(): File? {
    val fileChooser = JFileChooser().apply {
        dialogTitle = "Save Flow Graph"
        fileFilter = FileNameExtensionFilter("Flow Graph Files (*.flow.kts)", "kts")
        selectedFile = File("graph.flow.kts")
    }

    return if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        var file = fileChooser.selectedFile
        if (!file.name.endsWith(".flow.kts")) {
            file = File(file.parentFile, "${file.name}.flow.kts")
        }
        file
    } else {
        null
    }
}

/**
 * Show file open dialog
 */
fun showFileOpenDialog(): File? {
    val fileChooser = JFileChooser().apply {
        dialogTitle = "Open Flow Graph"
        fileFilter = FileNameExtensionFilter("Flow Graph Files (*.flow.kts)", "kts")
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
