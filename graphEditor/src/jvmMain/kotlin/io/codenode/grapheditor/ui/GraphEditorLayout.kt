/*
 * GraphEditorLayout - Main content layout with panels, canvas, and overlays
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import kotlinx.coroutines.launch
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.fbpdsl.model.InformationPacketType
import io.codenode.fbpdsl.model.NodeTypeDefinition
import io.codenode.fbpdsl.model.Port
import io.codenode.flowgraphexecute.ConnectionAnimation
import io.codenode.flowgraphexecute.RuntimeSession
import io.codenode.flowgraphgenerate.generator.EntityModuleSpec
import io.codenode.flowgraphgenerate.generator.EntityProperty
import io.codenode.flowgraphgenerate.save.ModuleSaveService
import io.codenode.flowgraphgenerate.viewmodel.IPGeneratorViewModel
import io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry
import io.codenode.flowgraphinspect.viewmodel.CodeEditorViewModel
import io.codenode.flowgraphinspect.viewmodel.FileEntry
import io.codenode.flowgraphinspect.viewmodel.IPPaletteViewModel
import io.codenode.flowgraphinspect.viewmodel.NodePaletteViewModel
import io.codenode.flowgraphcompose.viewmodel.CanvasInteractionViewModel
import io.codenode.flowgraphcompose.viewmodel.PropertiesPanelViewModel
import io.codenode.flowgraphgenerate.viewmodel.NodeGeneratorViewModel
import io.codenode.flowgraphpersist.state.GraphNodeTemplateRegistry
import io.codenode.flowgraphtypes.registry.IPTypeRegistry
import io.codenode.grapheditor.state.AddNodeCommand
import io.codenode.grapheditor.state.GraphState
import io.codenode.grapheditor.state.LevelCompatibilityChecker
import io.codenode.grapheditor.state.NodePromoter
import io.codenode.grapheditor.state.UndoRedoManager
import io.codenode.grapheditor.util.findModuleRoot
import io.codenode.grapheditor.util.resolveConnectionIPTypes

/**
 * Main content area of the graph editor, containing:
 * - Navigation breadcrumb bar
 * - Node generator and palette panels (left)
 * - IP generator and palette panels (left)
 * - Canvas with view toggle (center)
 * - Properties panel (right)
 * - Runtime preview panel (right)
 * - Canvas controls, navigation, and error overlays
 */
@Composable
fun ColumnScope.GraphEditorContent(
    graphState: GraphState,
    undoRedoManager: UndoRedoManager,
    registry: NodeDefinitionRegistry,
    graphNodeTemplateRegistry: GraphNodeTemplateRegistry,
    ipTypeRegistry: IPTypeRegistry,
    projectRoot: java.io.File,
    moduleRootDir: java.io.File?,
    nodeGeneratorViewModel: NodeGeneratorViewModel,
    nodePaletteViewModel: NodePaletteViewModel,
    ipPaletteViewModel: IPPaletteViewModel,
    ipGeneratorViewModel: IPGeneratorViewModel,
    codeEditorViewModel: CodeEditorViewModel,
    propertiesPanelViewModel: PropertiesPanelViewModel,
    canvasInteractionViewModel: CanvasInteractionViewModel,
    moduleSaveService: ModuleSaveService,
    runtimeSession: RuntimeSession?,
    runtimeExecutionState: ExecutionState?,
    animateDataFlow: Boolean,
    activeAnimations: List<ConnectionAnimation>,
    nodeTypes: List<NodeTypeDefinition>,
    ipTypes: List<InformationPacketType>,
    selectedIPType: InformationPacketType?,
    onSelectedIPTypeChanged: (InformationPacketType?) -> Unit,
    editorViewMode: ViewMode,
    onEditorViewModeChanged: (ViewMode) -> Unit,
    isInsideGraphNode: Boolean,
    currentGraphNodeName: String?,
    onStatusMessage: (String) -> Unit,
    onRegistryVersionIncrement: () -> Unit,
    onIpTypesVersionIncrement: () -> Unit,
    onModuleRootDirChanged: (java.io.File?) -> Unit,
    showUnsavedChangesDialog: Boolean,
    onShowUnsavedChangesDialogChanged: (Boolean) -> Unit,
    pendingEditorAction: (() -> Unit)?,
    onPendingEditorActionChanged: ((() -> Unit)?) -> Unit,
    showRemoveConfirmDialog: Boolean,
    onShowRemoveConfirmDialogChanged: (Boolean) -> Unit,
    removeTargetIPType: InformationPacketType?,
    onRemoveTargetIPTypeChanged: (InformationPacketType?) -> Unit,
    // Panel collapse/expand state
    isNodePanelExpanded: Boolean,
    onNodePanelExpandedChanged: (Boolean) -> Unit,
    isIPPanelExpanded: Boolean,
    onIPPanelExpandedChanged: (Boolean) -> Unit,
    isPropertiesPanelExpanded: Boolean,
    onPropertiesPanelExpandedChanged: (Boolean) -> Unit,
    isRuntimePanelExpanded: Boolean,
    onRuntimePanelExpandedChanged: (Boolean) -> Unit,
    featureGate: io.codenode.fbpdsl.model.FeatureGate? = null,
    codeGeneratorViewModel: io.codenode.grapheditor.viewmodel.CodeGeneratorViewModel? = null,
    isCodeGeneratorPanelExpanded: Boolean = false,
    onCodeGeneratorPanelExpandedChanged: (Boolean) -> Unit = {},
) {
    // Navigation breadcrumb bar (only visible when inside a GraphNode)
    NavigationBreadcrumbBar(
        navigationContext = graphState.navigationContext,
        graphNodeNames = graphState.getGraphNodeNamesInPath(),
        onNavigateToRoot = {
            graphState.navigateToRoot()
            onStatusMessage("Navigated to root")
        },
        onNavigateToLevel = { depth ->
            graphState.navigateToDepth(depth)
            val name = graphState.getCurrentGraphNodeName() ?: "parent"
            onStatusMessage("Navigated to $name")
        }
    )

    // Focus requester for keyboard handling
    val focusRequester = remember { FocusRequester() }

    // Request focus when composition completes
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // File selector state for code editor
    var selectedFileEntry by remember { mutableStateOf<FileEntry?>(null) }

    // Main content area with keyboard handling
    Box(
        modifier = Modifier
            .fillMaxSize()
            .weight(1f)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                handleGraphEditorKeyEvent(
                    keyEvent = keyEvent,
                    graphState = graphState,
                    undoRedoManager = undoRedoManager,
                    onStatusMessage = onStatusMessage,
                )
            }
    ) {
        // Layout: NodeGeneratorPanel + NodePalette on left, Canvas on right
        Row(modifier = Modifier.fillMaxSize()) {
            // Left column: Node Generator Panel above Node Palette
            CollapsiblePanel(
                isExpanded = isNodePanelExpanded,
                onToggle = { onNodePanelExpandedChanged(!isNodePanelExpanded) },
                side = PanelSide.LEFT,
                modifier = Modifier.fillMaxHeight()
            ) {
            Column(modifier = Modifier.fillMaxHeight()) {
                // Node Generator Panel
                NodeGeneratorPanel(
                    viewModel = nodeGeneratorViewModel,
                    onCodeNodeGenerated = {
                        // Refresh palette to include the newly generated node
                        onRegistryVersionIncrement()
                    }
                )

                // Node Palette
                val filteredNodeTypes = if (featureGate?.canUseRepositoryNodes() == false) {
                    nodeTypes.filter { it.category != io.codenode.fbpdsl.model.CodeNodeType.DATABASE }
                } else {
                    nodeTypes
                }
                NodePalette(
                    viewModel = nodePaletteViewModel,
                    nodeTypes = filteredNodeTypes,
                    graphNodeTemplates = graphNodeTemplateRegistry.getAll(),
                    onGraphNodeTemplateSelected = { template ->
                        io.codenode.flowgraphpersist.state.GraphNodeTemplateInstantiator.instantiate(
                            template,
                            graphNodeTemplateRegistry,
                            ipTypeRegistry
                        )?.let { graphNode ->
                            val nodeCount = graphState.flowGraph.rootNodes.size
                            val xOffset = 300.0 + (nodeCount % 3) * 150.0
                            val yOffset = 200.0 + (nodeCount / 3) * 100.0
                            val positioned = graphNode.copy(
                                position = io.codenode.fbpdsl.model.Node.Position(xOffset, yOffset)
                            )
                            graphState.addNode(positioned, Offset(xOffset.toFloat(), yOffset.toFloat()))
                            onStatusMessage("Added GraphNode '${template.name}' from palette")
                        } ?: run {
                            onStatusMessage("Failed to load GraphNode template '${template.name}'")
                        }
                    },
                    onNodeSelected = { nodeType ->
                    // Clear IP type selection when working with nodes
                    onSelectedIPTypeChanged(null)
                    ipPaletteViewModel.clearSelection()
                    // Create a new node from the selected type
                    // Offset each new node so they don't stack on top of each other
                    val nodeId = "node_${System.currentTimeMillis()}"
                    val nodeCount = graphState.flowGraph.rootNodes.size
                    val xOffset = 300.0 + (nodeCount % 3) * 150.0  // 3 nodes per row
                    val yOffset = 200.0 + (nodeCount / 3) * 100.0  // New row every 3 nodes
                    val newNode = CodeNode(
                        id = nodeId,
                        name = nodeType.name,
                        codeNodeType = nodeType.category,
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
                    onStatusMessage("Added ${nodeType.name} node")
                }
                )
            }
            }

            // IP Generator + IP Palette
            CollapsiblePanel(
                isExpanded = isIPPanelExpanded,
                onToggle = { onIPPanelExpandedChanged(!isIPPanelExpanded) },
                side = PanelSide.LEFT,
                modifier = Modifier.fillMaxHeight()
            ) {
            Column {
                IPGeneratorPanel(
                    viewModel = ipGeneratorViewModel,
                    onTypeCreated = { definition ->
                        onIpTypesVersionIncrement()
                        onStatusMessage("Created IP type: ${definition.typeName}")
                    },
                    ipTypes = ipTypes
                )
                IPPalette(
                    viewModel = ipPaletteViewModel,
                    ipTypes = ipTypes
                )
            }
            }

            // Code Generator panel (Pro tier only)
            if (codeGeneratorViewModel != null && featureGate?.canGenerate() != false) {
                codeGeneratorViewModel.updateFlowGraphName(graphState.flowGraph.name)
                CollapsiblePanel(
                    isExpanded = isCodeGeneratorPanelExpanded,
                    onToggle = { onCodeGeneratorPanelExpandedChanged(!isCodeGeneratorPanelExpanded) },
                    side = PanelSide.LEFT,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    CodeGeneratorPanel(
                        viewModel = codeGeneratorViewModel,
                        ipTypes = ipTypes,
                        onLoadFlowGraphFile = { file ->
                            try {
                                val parser = io.codenode.flowgraphpersist.serialization.FlowKtParser()
                                parser.setTypeResolver { typeName ->
                                    ipTypeRegistry.getByTypeName(typeName)?.payloadType
                                }
                                val parseResult = parser.parseFlowKt(file.readText())
                                val loadedGraph = parseResult.graph
                                if (parseResult.isSuccess && loadedGraph != null) {
                                    val resolvedGraph = resolveConnectionIPTypes(
                                        loadedGraph,
                                        ipTypeRegistry,
                                        parseResult.portTypeNameHints
                                    )
                                    codeGeneratorViewModel.selectFlowGraphFile(
                                        filePath = file.absolutePath,
                                        fileName = file.name,
                                        flowGraph = resolvedGraph
                                    )
                                    onStatusMessage("Loaded ${file.name} for code generation")
                                } else {
                                    onStatusMessage("Error loading: ${parseResult.errorMessage}")
                                }
                            } catch (e: Exception) {
                                onStatusMessage("Error loading: ${e.message}")
                            }
                        },
                        onGenerate = {
                            kotlinx.coroutines.MainScope().launch {
                                val panelState = codeGeneratorViewModel.state.value
                                if (panelState.selectedPath == io.codenode.flowgraphgenerate.model.GenerationPath.UI_FBP) {
                                    // UI-FBP routes through UIFBPSaveService directly (per FR-014/FR-015
                                    // explicit-pair input). The host module is the directory containing
                                    // the user-selected .flow.kt — derived from its path.
                                    val flowGraphFilePath = panelState.selectedFlowGraphFilePath
                                    if (flowGraphFilePath == null) {
                                        onStatusMessage("UI-FBP: select a .flow.kt file before generating")
                                        return@launch
                                    }
                                    val moduleRoot = findModuleRoot(java.io.File(flowGraphFilePath).parentFile)
                                    if (moduleRoot == null) {
                                        onStatusMessage("UI-FBP: unable to locate module root from .flow.kt path")
                                        return@launch
                                    }
                                    val saveResult = codeGeneratorViewModel.generateUIFBP(moduleRoot)
                                    val msg = if (saveResult.success) {
                                        val created = saveResult.files.count { it.kind == io.codenode.flowgraphgenerate.save.FileChangeKind.CREATED }
                                        val updated = saveResult.files.count { it.kind == io.codenode.flowgraphgenerate.save.FileChangeKind.UPDATED }
                                        val unchanged = saveResult.files.count { it.kind == io.codenode.flowgraphgenerate.save.FileChangeKind.UNCHANGED }
                                        val skipped = saveResult.files.count { it.kind == io.codenode.flowgraphgenerate.save.FileChangeKind.SKIPPED_CONFLICT }
                                        "UI-FBP: $created created, $updated updated, $unchanged unchanged" +
                                            (if (skipped > 0) ", $skipped SKIPPED (hand-edited)" else "")
                                    } else {
                                        "UI-FBP failed: ${saveResult.errorMessage ?: "unknown error"}"
                                    }
                                    onStatusMessage(msg)
                                } else {
                                    // GENERATE_MODULE / REPOSITORY paths: existing CodeGenerationRunner flow.
                                    val dir = showDirectoryChooser("Generate Module To")
                                    if (dir != null) {
                                        val result = codeGeneratorViewModel.generate(
                                            outputDir = dir,
                                            flowGraph = graphState.flowGraph,
                                            targetPlatforms = graphState.flowGraph.targetPlatforms
                                        )
                                        val msg = if (result.isSuccess) {
                                            "Generated ${result.totalGenerated} files for ${graphState.flowGraph.name}"
                                        } else {
                                            "Generated ${result.totalGenerated} files, ${result.totalErrors} errors"
                                        }
                                        onStatusMessage(msg)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }

            // Compute connection colors based on IP types
            val currentGraphNode = graphState.getCurrentGraphNode()
            val colorState = rememberConnectionColors(
                connections = graphState.flowGraph.connections,
                ipTypeRegistry = ipTypeRegistry,
                currentGraphNode = currentGraphNode,
            )
            val connectionColors = colorState.connectionColors
            val boundaryConnectionColors = colorState.boundaryConnectionColors

            // Main Canvas with View Toggle (Visual/Textual/Split)
            val codeEditorState by codeEditorViewModel.state.collectAsState()

            // Build file entries for file selector dropdown
            val fileEntries = remember(graphState.flowGraph) {
                CodeEditorViewModel.buildFileEntries(
                    flowGraph = graphState.flowGraph,
                    registry = registry
                )
            }

            GraphEditorWithToggle(
                flowGraph = graphState.flowGraph,
                initialMode = editorViewMode,
                overrideText = selectedIPType?.toCode(),
                overrideTitle = selectedIPType?.let { "IP Type: ${it.typeName}" },
                codeEditorContent = if (codeEditorState.currentFile != null) {
                    {
                        CodeEditor(
                            viewModel = codeEditorViewModel,
                            onSaveStatusMessage = { msg -> onStatusMessage(msg) }
                        )
                    }
                } else null,
                onViewModeChanged = { newMode ->
                    if (newMode == ViewMode.VISUAL && codeEditorState.isDirty) {
                        onPendingEditorActionChanged {
                            codeEditorViewModel.clear()
                            onEditorViewModeChanged(ViewMode.VISUAL)
                        }
                        onShowUnsavedChangesDialogChanged(true)
                    } else {
                        onEditorViewModeChanged(newMode)
                        if (newMode == ViewMode.VISUAL) {
                            codeEditorViewModel.clear()
                        }
                    }
                },
                fileEntries = fileEntries,
                selectedFileEntry = selectedFileEntry,
                onFileSelected = { entry ->
                    selectedFileEntry = entry
                    if (entry.isFlowGraph) {
                        // FlowGraph file: clear code editor, switch to textual (read-only DSL)
                        codeEditorViewModel.clear()
                        onEditorViewModeChanged(ViewMode.TEXTUAL)
                    } else {
                        // CodeNode file: select node on canvas, load file, switch to textual
                        entry.associatedNodeId?.let { nodeId ->
                            graphState.clearSelection()
                            graphState.selectNode(nodeId)
                        }
                        codeEditorViewModel.loadFile(entry.filePath)
                        onEditorViewModeChanged(ViewMode.TEXTUAL)
                    }
                },
                onVisualViewContent = {
                    GraphEditorCanvasSection(
                        graphState = graphState,
                        undoRedoManager = undoRedoManager,
                        connectionColors = connectionColors,
                        boundaryConnectionColors = boundaryConnectionColors,
                        ipTypeRegistry = ipTypeRegistry,
                        ipPaletteViewModel = ipPaletteViewModel,
                        focusRequester = focusRequester,
                        activeAnimations = activeAnimations,
                        onStatusMessage = onStatusMessage,
                        onSelectedIPTypeCleared = {
                            onSelectedIPTypeChanged(null)
                            ipPaletteViewModel.clearSelection()
                        },
                    )
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

            val selectedGraphNode = if (hasSingleSelection) {
                graphState.selectedNodeId?.let { nodeId ->
                    graphState.flowGraph.findNode(nodeId) as? GraphNode
                }
            } else null

            val selectedConnection = if (hasSingleSelection) {
                graphState.selectedConnectionIds.firstOrNull()?.let { connectionId ->
                    graphState.getConnectionsInCurrentContext().find { it.id == connectionId }
                }
            } else null

            CollapsiblePanel(
                isExpanded = isPropertiesPanelExpanded,
                onToggle = { onPropertiesPanelExpandedChanged(!isPropertiesPanelExpanded) },
                side = PanelSide.RIGHT,
                modifier = Modifier.fillMaxHeight()
            ) {
            CompactPropertiesPanelWithViewModel(
                viewModel = propertiesPanelViewModel,
                selectedNode = selectedNode,
                selectedConnection = selectedConnection,
                selectedIPType = selectedIPType,
                selectedGraphNode = selectedGraphNode,
                flowGraph = graphState.flowGraph,
                propertyDefinitions = selectedNode?.let { node ->
                    // Derive property definitions from node type or use defaults
                    // First try to match by node name, then by _genericType for generic nodes
                    val matchingNodeType = nodeTypes.find { it.name == node.name }
                        ?: node.configuration["_genericType"]?.let { genericType ->
                            nodeTypes.find { it.name == genericType }
                        }
                    matchingNodeType?.let { nodeType ->
                        PropertiesPanelState.derivePropertyDefinitions(nodeType)
                    } ?: emptyList()
                } ?: emptyList(),
                ipTypeRegistry = ipTypeRegistry,
                onGraphNodeNameChanged = { newName ->
                    graphState.selectedNodeId?.let { nodeId ->
                        graphState.updateNodeName(nodeId, newName)
                        onStatusMessage("Renamed node to: $newName")
                    }
                },
                onGraphNodePortNameChanged = { portId, newName ->
                    graphState.selectedNodeId?.let { nodeId ->
                        graphState.updatePortName(nodeId, portId, newName)
                        onStatusMessage("Renamed port to: $newName")
                    }
                },
                onGraphNodePortTypeChanged = { portId, typeName ->
                    graphState.selectedNodeId?.let { nodeId ->
                        graphState.updatePortType(nodeId, portId, typeName, ipTypeRegistry)
                        onStatusMessage("Changed port type to: $typeName")
                    }
                },
                onCreateRepositoryModule = selectedIPType?.let { ipType ->
                    val props = ipTypeRegistry.getCustomTypeProperties(ipType.id)
                    if (props != null && props.isNotEmpty()) {
                        {
                            // Build EntityModuleSpec and save the entity module
                            val entityProps = props.map { prop ->
                                EntityProperty(
                                    name = prop.name,
                                    kotlinType = when (prop.typeId) {
                                        "ip_int" -> "Int"
                                        "ip_double" -> "Double"
                                        "ip_boolean" -> "Boolean"
                                        "ip_string" -> "String"
                                        else -> "String"
                                    },
                                    isRequired = prop.isRequired
                                )
                            }
                            val spec = EntityModuleSpec.fromIPType(
                                ipTypeName = ipType.typeName,
                                sourceIPTypeId = ipType.id,
                                properties = entityProps
                            )

                            // Save to project root directory
                            val outputDir = projectRoot
                            val persistenceDir = java.io.File(
                                outputDir,
                                "persistence/src/commonMain/kotlin/io/codenode/persistence"
                            )
                            val result = moduleSaveService.saveEntityModule(
                                spec = spec,
                                moduleOutputDir = outputDir,
                                persistenceDir = persistenceDir
                            )
                            if (result.success) {
                                ipTypeRegistry.setEntityModule(ipType.id, true)
                                onIpTypesVersionIncrement()
                                val created = result.filesCreated.size
                                val overwritten = result.filesOverwritten.size
                                onStatusMessage("Created ${spec.pluralName} module: $created created, $overwritten overwritten")
                            } else {
                                onStatusMessage("Module creation error: ${result.errorMessage}")
                            }
                        }
                    } else null
                },
                onRemoveRepositoryModule = selectedIPType?.let { ipType ->
                    {
                        onRemoveTargetIPTypeChanged(ipType)
                        onShowRemoveConfirmDialogChanged(true)
                    }
                },
                moduleExists = selectedIPType?.let { ipType ->
                    ipTypeRegistry.hasEntityModule(ipType.id)
                } ?: false,
                moduleLoaded = moduleRootDir != null,
                graphNodeIsSaved = selectedGraphNode?.let { gn ->
                    graphNodeTemplateRegistry.nameExists(gn.name)
                } ?: false,
                graphNodeSavedTier = selectedGraphNode?.let { gn ->
                    graphNodeTemplateRegistry.getByName(gn.name)?.tier
                },
                onSaveGraphNodeToPalette = selectedGraphNode?.let { gn ->
                    { level: io.codenode.fbpdsl.model.PlacementLevel ->
                        graphNodeTemplateRegistry.saveGraphNode(
                            gn, level, moduleRootDir?.absolutePath
                        )
                        onStatusMessage("Saved '${gn.name}' to palette at ${level.displayName} level")
                    }
                },
                onRemoveGraphNodeFromPalette = selectedGraphNode?.let { gn ->
                    graphNodeTemplateRegistry.getByName(gn.name)?.let { meta ->
                        { level: io.codenode.fbpdsl.model.PlacementLevel ->
                            graphNodeTemplateRegistry.removeTemplate(gn.name, level)
                            onStatusMessage("Removed '${gn.name}' from palette")
                        }
                    }
                },
                checkPromotionCandidates = selectedGraphNode?.let { gn ->
                    { level: io.codenode.fbpdsl.model.PlacementLevel ->
                        LevelCompatibilityChecker.checkCompatibility(gn, level, registry)
                    }
                },
                onPromoteAndSave = selectedGraphNode?.let { gn ->
                    { candidates: List<io.codenode.grapheditor.state.PromotionCandidate>, level: io.codenode.fbpdsl.model.PlacementLevel ->
                        NodePromoter.promoteNodes(
                            candidates, level,
                            activeModulePath = moduleRootDir?.absolutePath,
                            projectRoot = projectRoot
                        )
                        graphNodeTemplateRegistry.saveGraphNode(
                            gn, level, moduleRootDir?.absolutePath
                        )
                        onStatusMessage("Promoted ${candidates.size} node(s) and saved '${gn.name}' to palette at ${level.displayName} level")
                    }
                },
                checkDuplicateName = { name, level ->
                    graphNodeTemplateRegistry.getByName(name)?.let { existing ->
                        existing.tier == level
                    } ?: false
                },
                debugger = runtimeSession?.debugger,
                isPaused = runtimeExecutionState == ExecutionState.PAUSED,
                isAnimateDataFlow = animateDataFlow,
                showEditButton = selectedNode != null && registry.getSourceFilePath(selectedNode.name) != null,
                onEditClick = selectedNode?.let { node ->
                    registry.getSourceFilePath(node.name)?.let { filePath ->
                        {
                            val file = java.io.File(filePath)
                            codeEditorViewModel.loadFile(file, readOnly = false)
                            onEditorViewModeChanged(ViewMode.TEXTUAL)
                            onStatusMessage("Editing: ${file.name}")
                        }
                    }
                },
                onEditChildNode = { childNodeName ->
                    registry.getSourceFilePath(childNodeName)?.let { filePath ->
                        val file = java.io.File(filePath)
                        codeEditorViewModel.loadFile(file, readOnly = false)
                        onEditorViewModeChanged(ViewMode.TEXTUAL)
                        onStatusMessage("Editing: ${file.name}")
                    }
                },
                childNodeHasSource = { childNodeName ->
                    registry.getSourceFilePath(childNodeName) != null
                }
            )
            }

            // Runtime Preview Panel (right side, after properties)
            RuntimePreviewPanel(
                runtimeSession = runtimeSession,
                isExpanded = isRuntimePanelExpanded,
                onToggle = { onRuntimePanelExpandedChanged(!isRuntimePanelExpanded) },
                featureGate = featureGate,
                moduleRootDir = moduleRootDir,
                flowGraphName = graphState.flowGraph.name,
                animateDataFlow = animateDataFlow,
                onAnimateDataFlowChanged = { runtimeSession?.setAnimateDataFlow(it) },
                modifier = Modifier.fillMaxHeight()
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
                        onStatusMessage("Navigated back to parent")
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
}
