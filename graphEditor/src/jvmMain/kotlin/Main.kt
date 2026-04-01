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
import io.codenode.grapheditor.state.NodeDefinitionRegistry
import io.codenode.grapheditor.state.GraphNodeTemplateRegistry
import io.codenode.grapheditor.state.LevelCompatibilityChecker
import io.codenode.grapheditor.state.NodePromoter
import io.codenode.grapheditor.viewmodel.SharedStateProvider
import io.codenode.grapheditor.viewmodel.LocalSharedState
import io.codenode.grapheditor.viewmodel.NodeGeneratorViewModel
import io.codenode.grapheditor.viewmodel.NodePaletteViewModel
import io.codenode.grapheditor.viewmodel.IPPaletteViewModel
import io.codenode.grapheditor.viewmodel.PropertiesPanelViewModel
import io.codenode.grapheditor.viewmodel.CanvasInteractionViewModel
import io.codenode.grapheditor.viewmodel.CodeEditorViewModel
import io.codenode.grapheditor.viewmodel.FileEntry
import io.codenode.grapheditor.viewmodel.GraphEditorViewModel
import io.codenode.grapheditor.viewmodel.EditorDialog
import androidx.compose.runtime.CompositionLocalProvider
import io.codenode.grapheditor.state.MoveNodeCommand
import io.codenode.grapheditor.state.AddConnectionCommand
import io.codenode.grapheditor.state.RemoveNodeCommand
import io.codenode.grapheditor.state.GroupNodesCommand
import io.codenode.grapheditor.state.UngroupNodeCommand
import io.codenode.grapheditor.ui.CompactCanvasControls
import io.codenode.grapheditor.ui.ConnectionErrorDisplay
import io.codenode.grapheditor.ui.FlowGraphCanvas
import io.codenode.grapheditor.ui.FlowGraphCanvasWithViewModel
import io.codenode.grapheditor.ui.NodePalette
import io.codenode.grapheditor.ui.NodeGeneratorPanel
import io.codenode.grapheditor.ui.GraphEditorWithToggle
import io.codenode.grapheditor.ui.ViewMode
import io.codenode.grapheditor.ui.CollapsiblePanel
import io.codenode.grapheditor.ui.CodeEditor
import io.codenode.grapheditor.ui.CompactPropertiesPanelWithViewModel
import io.codenode.grapheditor.ui.UnsavedChangesDialog
import io.codenode.grapheditor.ui.PanelSide
import io.codenode.grapheditor.ui.DynamicPreviewDiscovery
import io.codenode.grapheditor.ui.ModuleSessionFactory
import io.codenode.grapheditor.ui.RuntimePreviewPanel
import io.codenode.circuitsimulator.ConnectionAnimation
import io.codenode.circuitsimulator.RuntimeSession
import io.codenode.grapheditor.ui.PropertiesPanelState
import io.codenode.grapheditor.repository.FileIPTypeRepository
import io.codenode.grapheditor.repository.IPTypeMigration
import io.codenode.grapheditor.viewmodel.IPGeneratorViewModel
import io.codenode.grapheditor.ui.IPGeneratorPanel
import io.codenode.grapheditor.state.rememberPropertyChangeTracker
import io.codenode.grapheditor.serialization.FlowKtParser
import io.codenode.grapheditor.save.ModuleSaveService
import io.codenode.kotlincompiler.generator.EntityModuleSpec
import io.codenode.kotlincompiler.generator.EntityProperty
import io.codenode.fbpdsl.model.NodeTypeDefinition
import io.codenode.fbpdsl.model.PortTemplate
import io.codenode.fbpdsl.model.Port
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.fbpdsl.model.InformationPacketType
import io.codenode.grapheditor.state.IPTypeDiscovery
import io.codenode.grapheditor.state.IPTypeFileGenerator
import io.codenode.grapheditor.state.IPTypeRegistry
import io.codenode.grapheditor.ui.IPPalette
import io.codenode.grapheditor.ui.ConnectionContextMenu
import io.codenode.grapheditor.ui.NavigationBreadcrumbBar
import io.codenode.grapheditor.ui.NavigationZoomOutButton
import org.koin.core.context.startKoin
import org.koin.dsl.module
import io.codenode.grapheditor.ui.FlowGraphPropertiesDialog
import io.codenode.fbpdsl.model.FlowGraph.TargetPlatform
import io.codenode.grapheditor.compilation.CompilationService
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import io.codenode.fbpdsl.model.ExecutionState
import kotlinx.coroutines.flow.drop
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

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

    // Panel collapse/expand state
    var isNodePanelExpanded by remember { mutableStateOf(true) }
    var isIPPanelExpanded by remember { mutableStateOf(true) }
    var isPropertiesPanelExpanded by remember { mutableStateOf(true) }

    // Runtime preview session and panel state
    var isRuntimePanelExpanded by remember { mutableStateOf(false) }
    var moduleRootDir by remember { mutableStateOf<File?>(null) }

    // Resolve project root: explicit property from --project/CODENODE_PROJECT_DIR, or walk up to settings.gradle.kts
    val projectRoot = remember {
        val explicit = System.getProperty("codenode.project.dir")
        if (explicit != null) {
            File(explicit)
        } else {
            var dir = File(System.getProperty("user.dir"))
            while (dir.parentFile != null && !File(dir, "settings.gradle.kts").exists()) {
                dir = dir.parentFile
            }
            dir
        }
    }

    // T015: Central registry for discovering self-contained node definitions
    val registry = remember { NodeDefinitionRegistry() }
    // Track registry version to trigger recomposition when nodes are discovered
    var registryVersion by remember { mutableStateOf(0) }

    // 063: Central registry for saved GraphNode templates
    val graphNodeTemplateRegistry = remember { GraphNodeTemplateRegistry() }

    // NodeGeneratorViewModel for the Node Generator Panel
    val nodeGeneratorViewModel = remember {
        NodeGeneratorViewModel(
            registry = registry,
            projectRoot = projectRoot
        )
    }

    // Keep Node Generator's moduleLoaded state in sync with moduleRootDir
    LaunchedEffect(moduleRootDir) {
        nodeGeneratorViewModel.setModuleLoaded(moduleRootDir != null, moduleRootDir?.absolutePath)
    }

    // NodePaletteViewModel for the Node Palette
    val nodePaletteViewModel = remember { NodePaletteViewModel() }

    // CodeEditorViewModel for the code editor
    val codeEditorViewModel = remember { CodeEditorViewModel() }
    var editorViewMode by remember { mutableStateOf(ViewMode.VISUAL) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var pendingEditorAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var selectedFileEntry by remember { mutableStateOf<FileEntry?>(null) }

    // Discover all node definitions on startup (runtime discovery, no compile-time module dependencies)
    LaunchedEffect(Unit) {
        // Discover compiled and template nodes from all sources
        registry.discoverAll()
        // Dynamically scan all module node and userInterface directories in the project
        // Scan both commonMain and jvmMain source sets
        val sourceSetDirs = listOf("src/commonMain/kotlin", "src/jvmMain/kotlin")
        projectRoot.listFiles { file -> file.isDirectory }?.forEach { moduleDir ->
            for (sourceSet in sourceSetDirs) {
                val srcDir = moduleDir.resolve(sourceSet)
                if (srcDir.isDirectory) {
                    srcDir.walkTopDown()
                        .filter { it.isDirectory && it.name == "nodes" }
                        .forEach { registry.scanDirectory(it) }
                    // Discover and invoke PreviewProvider objects via reflection
                    srcDir.walkTopDown()
                        .filter { it.isDirectory && it.name == "userInterface" }
                        .forEach { uiDir ->
                            DynamicPreviewDiscovery.discoverAndRegister(uiDir)
                        }
                }
            }
        }
        // Make registry available for runtime node resolution
        ModuleSessionFactory.registry = registry
        // Initialize persistence layer early so DAOs are available for pipeline execution
        ModuleSessionFactory.initializePersistence()
        registryVersion++
    }

    // Palette shows only filesystem-discovered CodeNodes (compiled + templates)
    val nodeTypes = remember(registryVersion) {
        registry.getAllForPalette()
    }
    val ipTypeRegistry = remember { IPTypeRegistry.withDefaults() }
    val ipTypeRepository = remember { FileIPTypeRepository() }
    val modulePaths = remember {
        // Dynamically discover module directories (any subdirectory with src/commonMain/kotlin)
        // Exclude shared directories (iptypes/, nodes/) which are not modules
        val sharedDirs = setOf("iptypes", "nodes", "persistence", "build", ".gradle", ".git")
        projectRoot.listFiles { file ->
            file.isDirectory &&
            file.name !in sharedDirs &&
            java.io.File(file, "src/commonMain/kotlin").isDirectory
        }?.toList() ?: emptyList()
    }
    val discovery = remember { IPTypeDiscovery(projectRoot, modulePaths) }
    val ipTypeFileGenerator = remember { IPTypeFileGenerator(projectRoot) }
    // Discover IP types from filesystem on startup
    var ipTypesVersion by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        // One-time migration from legacy JSON to filesystem (skips Module-level types)
        val migration = IPTypeMigration(ipTypeFileGenerator, discovery)
        migration.migrateIfNeeded()

        val discovered = discovery.discoverAll()
        ipTypeRegistry.registerFromFilesystem(discovered) { meta ->
            discovery.resolveKClass(meta)
        }

        // Also load any legacy custom types from JSON repository (for migration period)
        ipTypeRepository.load()
        ipTypeRepository.getAllDefinitions().forEach { definition ->
            if (!ipTypeRegistry.contains(definition.id)) {
                ipTypeRegistry.registerCustomType(definition)
            }
        }

        // Auto-detect which IP types have existing entity modules in the project
        // by checking for module directories matching pluralized type names
        ipTypeRegistry.getAllTypes().forEach { ipType ->
            val pluralName = io.codenode.kotlincompiler.generator.pluralize(ipType.typeName)
            val moduleDir = java.io.File(projectRoot, pluralName)
            if (moduleDir.isDirectory && java.io.File(moduleDir, "build.gradle.kts").exists()) {
                ipTypeRegistry.setEntityModule(ipType.id, true)
            }
        }

        // Discover saved GraphNode templates from all three tiers
        graphNodeTemplateRegistry.discoverAll(projectRoot, modulePaths)

        ipTypesVersion++
    }
    val ipTypes = remember(ipTypesVersion, moduleRootDir) {
        ipTypeRegistry.getVisibleTypes(activeModuleName = moduleRootDir?.name)
    }
    val ipGeneratorViewModel = remember(ipTypeRegistry, ipTypeRepository) {
        IPGeneratorViewModel(ipTypeRegistry, ipTypeRepository, ipTypeFileGenerator, discovery)
    }
    // Keep IP Generator's moduleLoaded state in sync with moduleRootDir
    LaunchedEffect(moduleRootDir) {
        ipGeneratorViewModel.setModuleLoaded(moduleRootDir != null, moduleRootDir?.absolutePath)
    }
    var selectedIPType by remember { mutableStateOf<InformationPacketType?>(null) }
    var showOpenDialog by remember { mutableStateOf(false) }
    var showModuleSaveDialog by remember { mutableStateOf(false) }
    var showFlowGraphPropertiesDialog by remember { mutableStateOf(false) }
    val saveLocationRegistry = remember { mutableMapOf<String, File>() }
    var statusMessage by remember { mutableStateOf("Ready - Create a new graph or open an existing one") }
    var showRemoveConfirmDialog by remember { mutableStateOf(false) }
    var removeTargetIPType by remember { mutableStateOf<InformationPacketType?>(null) }

    // Create/recreate RuntimeSession synchronously when module changes
    // (must be synchronous so runtimeSession is always in sync with moduleRootDir
    // during the same recomposition — avoids ClassCastException on module switch)
    // Pass the editor's FlowGraph so animation connection IDs match the Canvas.
    val runtimeSession = remember(moduleRootDir) {
        moduleRootDir?.name?.let {
            ModuleSessionFactory.createSession(
                moduleName = it,
                editorFlowGraph = graphState.flowGraph,
                flowGraphProvider = { graphState.flowGraph }
            )
        }
    }

    // Collect animation and debug state from the session
    val animateDataFlow = runtimeSession?.animateDataFlow?.collectAsState()?.value ?: false
    val activeAnimations = runtimeSession?.animationController?.activeAnimations?.collectAsState()?.value ?: emptyList()
    val runtimeExecutionState = runtimeSession?.executionState?.collectAsState()?.value ?: ExecutionState.IDLE

    // Stop previous session when it's replaced or removed
    DisposableEffect(runtimeSession) {
        onDispose {
            runtimeSession?.let {
                if (it.executionState.value != ExecutionState.IDLE) {
                    it.stop()
                }
            }
        }
    }

    // Auto-stop runtime when graph is edited while running (FR-007)
    LaunchedEffect(runtimeSession) {
        snapshotFlow { graphState.flowGraph }
            .drop(1) // Skip the initial value
            .collect {
                if (runtimeSession?.executionState?.value != ExecutionState.IDLE) {
                    runtimeSession?.stop()
                    statusMessage = "Execution stopped: graph was modified"
                }
            }
    }

    // IPPaletteViewModel for the IP Palette
    val ipPaletteViewModel = remember {
        IPPaletteViewModel(
            ipTypeRegistry = ipTypeRegistry,
            ipTypeRepository = ipTypeRepository,
            onCustomTypesChanged = {
                ipTypesVersion++
                ipGeneratorViewModel.refreshExistingTypeNames()
            },
            onTypeSelected = { ipType ->
                selectedIPType = ipType
                if (ipType != null) {
                    statusMessage = "Selected IP type: ${ipType.typeName}"
                }
            }
        )
    }

    // Update IPPaletteViewModel with deletable type IDs when custom IP types change
    LaunchedEffect(ipTypesVersion) {
        ipPaletteViewModel.updateDeletableTypeIds(ipTypeRegistry.getCustomTypeIds())
    }

    val compilationService = remember { CompilationService() }
    val moduleSaveService = remember { ModuleSaveService() }

    // PropertiesPanelViewModel for the Properties Panel
    val propertiesPanelViewModel = remember {
        PropertiesPanelViewModel(
            onNodeNameChanged = { newName ->
                graphState.selectedNodeId?.let { nodeId ->
                    graphState.updateNodeName(nodeId, newName)
                    statusMessage = "Renamed node to: $newName"
                }
            },
            onPropertyChanged = { key, value ->
                graphState.selectedNodeId?.let { nodeId ->
                    val node = graphState.flowGraph.findNode(nodeId)
                    val oldValue = node?.configuration?.get(key) ?: ""
                    propertyChangeTracker.trackChange(nodeId, key, oldValue, value)
                    statusMessage = "Updated property: $key"
                }
            },
            onPortNameChanged = { portId, newName ->
                graphState.selectedNodeId?.let { nodeId ->
                    graphState.updatePortName(nodeId, portId, newName)
                    statusMessage = "Renamed port to: $newName"
                }
            },
            onPortTypeChanged = { portId, typeName ->
                graphState.selectedNodeId?.let { nodeId ->
                    graphState.updatePortType(nodeId, portId, typeName, ipTypeRegistry)
                    statusMessage = "Changed port type to: $typeName"
                }
            },
            onConnectionIPTypeChanged = { connectionId, ipTypeId ->
                graphState.updateConnectionIPType(connectionId, ipTypeId, ipTypeRegistry)
                statusMessage = "Changed connection IP type"
            }
        )
    }

    // CanvasInteractionViewModel for canvas interactions
    val canvasInteractionViewModel = remember {
        CanvasInteractionViewModel(
            onNodeMoved = { nodeId, newX, newY ->
                graphState.updateNodePosition(nodeId, newX, newY)
                statusMessage = "Moved node"
            },
            onConnectionCreated = { connection ->
                graphState.addConnection(connection)
                statusMessage = "Created connection"
            }
        )
    }

    // GraphEditorViewModel for orchestration-level state
    val graphEditorViewModel = remember {
        GraphEditorViewModel(
            onCreateNewGraph = {
                val newGraph = flowGraph(
                    name = "New Graph",
                    version = "1.0.0"
                ) {}
                graphState.setGraph(newGraph, markDirty = false)
                graphState.navigateToRoot()
            },
            onOpenGraph = { /* Dialog will handle this */ },
            onSaveGraph = { /* Dialog will handle this */ },
            onUndo = {
                undoRedoManager.undo(graphState)
            },
            onRedo = {
                undoRedoManager.redo(graphState)
            },
            onGroupSelectedNodes = {
                val selectedIds = graphState.selectionState.selectedNodeIds.toSet()
                if (selectedIds.size >= 2) {
                    val command = GroupNodesCommand(selectedIds)
                    undoRedoManager.execute(command, graphState)
                }
            },
            onUngroupSelectedNode = {
                val selectedId = graphState.selectionState.selectedNodeIds.firstOrNull()
                if (selectedId != null) {
                    val command = UngroupNodeCommand(selectedId)
                    undoRedoManager.execute(command, graphState)
                }
            },
            onNavigateBack = {
                graphState.navigateOut()
            }
        )
    }

    // Derive button states from selection - these update automatically when selection changes
    val selectionState = graphState.selectionState  // Read selection state to ensure reactivity
    val canGroup = selectionState.selectedNodeIds.size >= 2
    val canUngroup = selectionState.selectedNodeIds.size == 1 &&
        graphState.flowGraph.findNode(selectionState.selectedNodeIds.firstOrNull() ?: "") is io.codenode.fbpdsl.model.GraphNode

    // Navigation state for GraphNode drill-down
    val navigationContext = graphState.navigationContext
    val isInsideGraphNode = !navigationContext.isAtRoot
    val currentGraphNodeName = graphState.getCurrentGraphNodeName()

    // Update GraphEditorViewModel state based on derived values
    LaunchedEffect(canGroup, canUngroup) {
        graphEditorViewModel.updateGroupingState(canGroup, canUngroup)
    }

    LaunchedEffect(isInsideGraphNode, currentGraphNodeName) {
        graphEditorViewModel.updateNavigationState(isInsideGraphNode, currentGraphNodeName)
    }

    LaunchedEffect(undoRedoManager.canUndo, undoRedoManager.canRedo) {
        graphEditorViewModel.updateUndoRedoState(
            canUndo = undoRedoManager.canUndo,
            canRedo = undoRedoManager.canRedo,
            undoDescription = undoRedoManager.getUndoDescription(),
            redoDescription = undoRedoManager.getRedoDescription()
        )
    }

    LaunchedEffect(graphState.flowGraph.name) {
        graphEditorViewModel.updateFlowGraphName(graphState.flowGraph.name)
    }

    // Create SharedStateProvider for ViewModel pattern
    val sharedState = remember(graphState, undoRedoManager, propertyChangeTracker, ipTypeRegistry) {
        SharedStateProvider(
            graphState = graphState,
            undoRedoManager = undoRedoManager,
            propertyChangeTracker = propertyChangeTracker,
            ipTypeRegistry = ipTypeRegistry
        )
    }

    CompositionLocalProvider(LocalSharedState provides sharedState) {
        MaterialTheme {
        Column(modifier = modifier.fillMaxSize()) {
            // Top toolbar
            TopToolbar(
                undoRedoManager = undoRedoManager,
                canGroup = canGroup,
                canUngroup = canUngroup,
                isInsideGraphNode = isInsideGraphNode,
                currentGraphNodeName = currentGraphNodeName,
                flowGraphName = graphState.flowGraph.name,
                onShowProperties = { showFlowGraphPropertiesDialog = true },
                onNew = {
                    val newGraph = flowGraph(
                        name = "New Graph",
                        version = "1.0.0"
                    ) {}
                    graphState.setGraph(newGraph, markDirty = false)
                    graphState.navigateToRoot()
                    moduleRootDir = null
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
                // Layout: NodeGeneratorPanel + NodePalette on left, Canvas on right
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left column: Node Generator Panel above Node Palette
                    CollapsiblePanel(
                        isExpanded = isNodePanelExpanded,
                        onToggle = { isNodePanelExpanded = !isNodePanelExpanded },
                        side = PanelSide.LEFT,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                    Column(modifier = Modifier.fillMaxHeight()) {
                        // Node Generator Panel
                        NodeGeneratorPanel(
                            viewModel = nodeGeneratorViewModel,
                            onCodeNodeGenerated = {
                                // Refresh palette to include the newly generated node
                                registryVersion++
                            }
                        )

                        // Node Palette
                        NodePalette(
                            viewModel = nodePaletteViewModel,
                            nodeTypes = nodeTypes,
                            graphNodeTemplates = graphNodeTemplateRegistry.getAll(),
                            onGraphNodeTemplateSelected = { template ->
                                io.codenode.grapheditor.state.GraphNodeTemplateInstantiator.instantiate(
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
                                    statusMessage = "Added GraphNode '${template.name}' from palette"
                                } ?: run {
                                    statusMessage = "Failed to load GraphNode template '${template.name}'"
                                }
                            },
                            onNodeSelected = { nodeType ->
                            // Clear IP type selection when working with nodes
                            selectedIPType = null
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
                            statusMessage = "Added ${nodeType.name} node"
                        }
                        )
                    }
                    }

                    // IP Generator + IP Palette
                    CollapsiblePanel(
                        isExpanded = isIPPanelExpanded,
                        onToggle = { isIPPanelExpanded = !isIPPanelExpanded },
                        side = PanelSide.LEFT,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                    Column {
                        IPGeneratorPanel(
                            viewModel = ipGeneratorViewModel,
                            onTypeCreated = { definition ->
                                ipTypesVersion++
                                statusMessage = "Created IP type: ${definition.typeName}"
                            },
                            ipTypes = ipTypes
                        )
                        IPPalette(
                            viewModel = ipPaletteViewModel,
                            ipTypes = ipTypes
                        )
                    }
                    }

                    // Compute connection colors based on IP types
                    // Include both top-level connections and current GraphNode's internal connections
                    val currentGraphNode = graphState.getCurrentGraphNode()
                    val connectionColors: Map<String, Color> = remember(
                        graphState.flowGraph.connections,
                        ipTypeRegistry,
                        currentGraphNode?.id,
                        currentGraphNode?.internalConnections
                    ) {
                        val colorMap = mutableMapOf<String, Color>()
                        // Top-level connections
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
                        // Internal connections of current GraphNode (for interior view)
                        currentGraphNode?.internalConnections?.forEach { connection ->
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
                    // Also derives colors from internal connections via port mappings (for instantiated templates)
                    val boundaryConnectionColors: Map<String, Color> = remember(
                        graphState.flowGraph.connections,
                        ipTypeRegistry,
                        currentGraphNode?.id,
                        currentGraphNode?.internalConnections
                    ) {
                        if (currentGraphNode == null) {
                            emptyMap()
                        } else {
                            val colorMap = mutableMapOf<String, Color>()
                            // 1. Colors from parent-level connections (external wiring)
                            graphState.flowGraph.connections.forEach { connection ->
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
                            // 2. Derive boundary port colors from internal connections via port mappings
                            // This covers cases where the GraphNode isn't externally connected (e.g., instantiated templates)
                            val childPortToIpType = mutableMapOf<String, String>()
                            currentGraphNode.internalConnections.forEach { conn ->
                                conn.ipTypeId?.let { typeId ->
                                    childPortToIpType[conn.sourcePortId] = typeId
                                    childPortToIpType[conn.targetPortId] = typeId
                                }
                            }
                            for (port in currentGraphNode.inputPorts + currentGraphNode.outputPorts) {
                                if (colorMap.containsKey(port.id)) continue
                                val mapping = currentGraphNode.portMappings[port.name] ?: currentGraphNode.portMappings[port.id]
                                if (mapping != null) {
                                    // Find the child port ID that matches this mapping
                                    val childNode = currentGraphNode.childNodes.find { it.id == mapping.childNodeId }
                                    if (childNode != null) {
                                        val childPort = (childNode.inputPorts + childNode.outputPorts).find { it.name == mapping.childPortName }
                                        if (childPort != null) {
                                            childPortToIpType[childPort.id]?.let { typeId ->
                                                ipTypeRegistry.getById(typeId)?.let { ipType ->
                                                    val ipColor = ipType.color
                                                    colorMap[port.id] = Color(
                                                        red = ipColor.red / 255f,
                                                        green = ipColor.green / 255f,
                                                        blue = ipColor.blue / 255f
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                // Fallback: trace through port mapping to child port's actual type
                                if (!colorMap.containsKey(port.id)) {
                                    val mapping = currentGraphNode.portMappings[port.name] ?: currentGraphNode.portMappings[port.id]
                                    if (mapping != null) {
                                        val childNode = currentGraphNode.childNodes.find { it.id == mapping.childNodeId }
                                        val childPort = childNode?.let { cn ->
                                            (cn.inputPorts + cn.outputPorts).find { it.name == mapping.childPortName }
                                        }
                                        val typeName = childPort?.dataType?.simpleName
                                        if (typeName != null && typeName != "Any") {
                                            ipTypeRegistry.getByTypeName(typeName)?.let { ipType ->
                                                val ipColor = ipType.color
                                                colorMap[port.id] = Color(
                                                    red = ipColor.red / 255f,
                                                    green = ipColor.green / 255f,
                                                    blue = ipColor.blue / 255f
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            colorMap
                        }
                    }

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
                                    onSaveStatusMessage = { msg -> statusMessage = msg }
                                )
                            }
                        } else null,
                        onViewModeChanged = { newMode ->
                            if (newMode == ViewMode.VISUAL && codeEditorState.isDirty) {
                                pendingEditorAction = {
                                    codeEditorViewModel.clear()
                                    editorViewMode = ViewMode.VISUAL
                                }
                                showUnsavedChangesDialog = true
                            } else {
                                editorViewMode = newMode
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
                                editorViewMode = ViewMode.TEXTUAL
                            } else {
                                // CodeNode file: select node on canvas, load file, switch to textual
                                entry.associatedNodeId?.let { nodeId ->
                                    graphState.clearSelection()
                                    graphState.selectNode(nodeId)
                                }
                                codeEditorViewModel.loadFile(entry.filePath)
                                editorViewMode = ViewMode.TEXTUAL
                            }
                        },
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
                                    // Clear IP type selection when selecting a node
                                    if (nodeId != null) {
                                        selectedIPType = null
                                        ipPaletteViewModel.clearSelection()
                                    }
                                    statusMessage = if (nodeId != null) "Selected node" else ""
                                    // Restore focus so keyboard events (Delete) work
                                    focusRequester.requestFocus()
                                },
                                onConnectionSelected = { connectionId ->
                                    if (connectionId != null) {
                                        graphState.selectConnection(connectionId)
                                        // Clear IP type selection when selecting a connection
                                        selectedIPType = null
                                        ipPaletteViewModel.clearSelection()
                                        statusMessage = "Selected connection"
                                    } else if (graphState.selectedConnectionIds.isNotEmpty()) {
                                        graphState.clearSelection()
                                        statusMessage = ""
                                    }
                                    // Restore focus so keyboard events (Delete) work
                                    focusRequester.requestFocus()
                                },
                                onElementShiftClicked = { element ->
                                    // Toggle element in selection (unified for nodes and connections)
                                    graphState.toggleElementInSelection(element)
                                    val count = graphState.selectionState.totalSelectionCount
                                    statusMessage = "$count item${if (count != 1) "s" else ""} selected"
                                    focusRequester.requestFocus()
                                },
                                onEmptyCanvasClicked = {
                                    // Clear all selections when clicking empty canvas
                                    graphState.clearSelection()
                                    statusMessage = ""
                                    focusRequester.requestFocus()
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
                        onToggle = { isPropertiesPanelExpanded = !isPropertiesPanelExpanded },
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
                                statusMessage = "Renamed node to: $newName"
                            }
                        },
                        onGraphNodePortNameChanged = { portId, newName ->
                            graphState.selectedNodeId?.let { nodeId ->
                                graphState.updatePortName(nodeId, portId, newName)
                                statusMessage = "Renamed port to: $newName"
                            }
                        },
                        onGraphNodePortTypeChanged = { portId, typeName ->
                            graphState.selectedNodeId?.let { nodeId ->
                                graphState.updatePortType(nodeId, portId, typeName, ipTypeRegistry)
                                statusMessage = "Changed port type to: $typeName"
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
                                        ipTypesVersion++
                                        val created = result.filesCreated.size
                                        val overwritten = result.filesOverwritten.size
                                        statusMessage = "Created ${spec.pluralName} module: $created created, $overwritten overwritten"
                                    } else {
                                        statusMessage = "Module creation error: ${result.errorMessage}"
                                    }
                                }
                            } else null
                        },
                        onRemoveRepositoryModule = selectedIPType?.let { ipType ->
                            {
                                removeTargetIPType = ipType
                                showRemoveConfirmDialog = true
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
                            { level: io.codenode.grapheditor.model.PlacementLevel ->
                                graphNodeTemplateRegistry.saveGraphNode(
                                    gn, level, moduleRootDir?.absolutePath
                                )
                                statusMessage = "Saved '${gn.name}' to palette at ${level.displayName} level"
                            }
                        },
                        onRemoveGraphNodeFromPalette = selectedGraphNode?.let { gn ->
                            graphNodeTemplateRegistry.getByName(gn.name)?.let { meta ->
                                { level: io.codenode.grapheditor.model.PlacementLevel ->
                                    graphNodeTemplateRegistry.removeTemplate(gn.name, level)
                                    statusMessage = "Removed '${gn.name}' from palette"
                                }
                            }
                        },
                        checkPromotionCandidates = selectedGraphNode?.let { gn ->
                            { level: io.codenode.grapheditor.model.PlacementLevel ->
                                LevelCompatibilityChecker.checkCompatibility(gn, level, registry)
                            }
                        },
                        onPromoteAndSave = selectedGraphNode?.let { gn ->
                            { candidates: List<io.codenode.grapheditor.state.PromotionCandidate>, level: io.codenode.grapheditor.model.PlacementLevel ->
                                NodePromoter.promoteNodes(
                                    candidates, level,
                                    activeModulePath = moduleRootDir?.absolutePath,
                                    projectRoot = projectRoot
                                )
                                graphNodeTemplateRegistry.saveGraphNode(
                                    gn, level, moduleRootDir?.absolutePath
                                )
                                statusMessage = "Promoted ${candidates.size} node(s) and saved '${gn.name}' to palette at ${level.displayName} level"
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
                                    editorViewMode = ViewMode.TEXTUAL
                                    statusMessage = "Editing: ${file.name}"
                                }
                            }
                        }
                    )
                    }

                    // Runtime Preview Panel (right side, after properties)
                    RuntimePreviewPanel(
                        runtimeSession = runtimeSession,
                        isExpanded = isRuntimePanelExpanded,
                        onToggle = { isRuntimePanelExpanded = !isRuntimePanelExpanded },
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
                val openResult = showFileOpenDialog()
                val file = openResult.file
                if (file != null) {
                    try {
                        // T062: Only support .flow.kt files (removed .flow.kts support)
                        val parser = FlowKtParser()
                        val parseResult = parser.parseFlowKt(file.readText())
                        if (parseResult.isSuccess && parseResult.graph != null) {
                            graphState.setGraph(parseResult.graph, markDirty = false)
                            moduleRootDir = findModuleRoot(file.parentFile)
                            // Register save location so re-save skips the directory prompt
                            moduleRootDir?.parentFile?.let { parentDir ->
                                saveLocationRegistry[parseResult.graph.name] = parentDir
                            }
                            statusMessage = "Opened ${file.name}"
                        } else {
                            statusMessage = "Error opening: ${parseResult.errorMessage}"
                        }
                    } catch (e: Exception) {
                        statusMessage = "Error opening: ${e.message}"
                    }
                } else if (openResult.error != null) {
                    statusMessage = openResult.error
                }
                showOpenDialog = false
            }
        }

        // Unified Save handler: registry lookup → directory chooser if needed → saveModule()
        if (showModuleSaveDialog) {
            LaunchedEffect(Unit) {
                val flowGraphName = graphState.flowGraph.name
                val savedDir = saveLocationRegistry[flowGraphName]

                // Determine output directory: use registry or prompt user
                val outputDir = if (savedDir != null && savedDir.exists()) {
                    // Re-save to known location (no directory prompt)
                    savedDir
                } else {
                    // Remove stale entry if directory no longer exists
                    if (savedDir != null) {
                        saveLocationRegistry.remove(flowGraphName)
                    }
                    // First save or directory gone: prompt for directory
                    showDirectoryChooser("Save Module To")
                }

                if (outputDir != null) {
                    // Build IP type properties map for repository code generation
                    val ipTypePropertiesMap = buildMap {
                        for (ipTypeId in ipTypeRegistry.getEntityModuleIPTypeIds()) {
                            val props = ipTypeRegistry.getCustomTypeProperties(ipTypeId)
                            if (props != null) {
                                put(ipTypeId, props.map { prop ->
                                    io.codenode.kotlincompiler.generator.EntityProperty(
                                        name = prop.name,
                                        kotlinType = when (prop.typeId) {
                                            "ip_int" -> "Int"
                                            "ip_double" -> "Double"
                                            "ip_boolean" -> "Boolean"
                                            "ip_string" -> "String"
                                            else -> "String" // ip_any and custom types → String
                                        },
                                        isRequired = prop.isRequired
                                    )
                                })
                            }
                        }
                    }
                    // Build IP type id→name map for .flow.kt port type resolution
                    val ipTypeNamesMap = buildMap {
                        for (ipType in ipTypeRegistry.getAllTypes()) {
                            put(ipType.id, ipType.typeName)
                        }
                    }
                    val result = moduleSaveService.saveModule(
                        flowGraph = graphState.flowGraph,
                        outputDir = outputDir,
                        ipTypeProperties = ipTypePropertiesMap,
                        ipTypeNames = ipTypeNamesMap,
                        codeNodeClassLookup = { nodeName ->
                            registry.getByName(nodeName)?.let {
                                it::class.qualifiedName
                            }
                        }
                    )
                    if (result.success) {
                        saveLocationRegistry[flowGraphName] = outputDir
                        moduleRootDir = result.moduleDir
                        val created = result.filesCreated.size
                        val overwritten = result.filesOverwritten.size
                        val deleted = result.filesDeleted.size
                        statusMessage = "Saved to ${result.moduleDir?.name}: $created created, $overwritten overwritten, $deleted deleted"
                    } else {
                        statusMessage = "Save error: ${result.errorMessage}"
                    }
                }
                showModuleSaveDialog = false
            }
        }

        // FlowGraph Properties dialog
        if (showFlowGraphPropertiesDialog) {
            FlowGraphPropertiesDialog(
                name = graphState.flowGraph.name,
                targetPlatforms = graphState.flowGraph.targetPlatforms.toSet(),
                onNameChanged = { newName ->
                    if (newName.isNotBlank()) {
                        graphState.setGraph(graphState.flowGraph.copy(name = newName), markDirty = true)
                    }
                },
                onTargetPlatformToggled = { platform ->
                    val current = graphState.flowGraph.targetPlatforms.toMutableList()
                    if (platform in current) current.remove(platform) else current.add(platform)
                    graphState.setGraph(graphState.flowGraph.withTargetPlatforms(current), markDirty = true)
                },
                onDismiss = { showFlowGraphPropertiesDialog = false }
            )
        }

        // Remove Repository Module confirmation dialog
        if (showRemoveConfirmDialog && removeTargetIPType != null) {
            val ipType = removeTargetIPType!!
            val moduleName = EntityModuleSpec.fromIPType(
                ipTypeName = ipType.typeName,
                sourceIPTypeId = ipType.id,
                properties = emptyList()
            ).pluralName
            AlertDialog(
                onDismissRequest = {
                    showRemoveConfirmDialog = false
                    removeTargetIPType = null
                },
                title = { Text("Remove Module") },
                text = {
                    Text(
                        "Are you sure you want to remove the $moduleName module? " +
                        "This will delete the module directory, persistence files, and Gradle entries."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRemoveConfirmDialog = false
                            removeTargetIPType = null

                            // Use the configured project directory
                            val projectDir = projectRoot
                            val entityName = ipType.typeName
                            val moduleDir = File(projectDir, moduleName)
                            val persistenceDir = File(
                                projectDir,
                                "persistence/src/commonMain/kotlin/io/codenode/persistence"
                            )

                            val result = moduleSaveService.removeEntityModule(
                                entityName = entityName,
                                moduleName = moduleName,
                                moduleDir = moduleDir,
                                persistenceDir = persistenceDir,
                                projectDir = projectDir,
                                sourceIPTypeId = ipType.id
                            )

                            ipTypeRegistry.setEntityModule(ipType.id, false)
                            ipTypesVersion++
                            statusMessage = result
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.error
                        )
                    ) {
                        Text("Remove", color = MaterialTheme.colors.onError)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showRemoveConfirmDialog = false
                            removeTargetIPType = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Unsaved changes dialog for code editor
        if (showUnsavedChangesDialog) {
            UnsavedChangesDialog(
                fileName = codeEditorViewModel.state.value.currentFile?.name ?: "file",
                onSave = {
                    codeEditorViewModel.save()
                    showUnsavedChangesDialog = false
                    pendingEditorAction?.invoke()
                    pendingEditorAction = null
                },
                onDiscard = {
                    codeEditorViewModel.discardChanges()
                    showUnsavedChangesDialog = false
                    pendingEditorAction?.invoke()
                    pendingEditorAction = null
                },
                onCancel = {
                    showUnsavedChangesDialog = false
                    pendingEditorAction = null
                }
            )
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
    flowGraphName: String = "New Graph",
    onShowProperties: () -> Unit = {},
    onNew: () -> Unit,
    onOpen: () -> Unit,
    onSave: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onGroup: () -> Unit = {},
    onUngroup: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
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

            // Show graph name and properties button when at root level
            if (!isInsideGraphNode) {
                Text(
                    text = " - ",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 18.sp
                )
                Text(
                    text = flowGraphName,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    fontSize = 18.sp
                )
                IconButton(onClick = onShowProperties) {
                    Text(
                        text = "\u2699",  // Gear icon
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }

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
 * Result of a file open dialog operation.
 */
data class FileOpenResult(val file: File? = null, val error: String? = null)

/**
 * Show file open dialog for .flow.kt files or module directories.
 *
 * Accepts both files and directories. When a directory (module folder) is selected,
 * resolves the .flow.kt file at the conventional path:
 *   {moduleDir}/src/commonMain/kotlin/io/codenode/{modulename}/{ModuleName}.flow.kt
 */
fun showFileOpenDialog(): FileOpenResult {
    // Start the file chooser at the project directory
    val startDir = System.getProperty("codenode.project.dir")?.let { File(it) }
        ?: File(System.getProperty("user.dir"))
    val fileChooser = JFileChooser(startDir).apply {
        dialogTitle = "Open Flow Graph or Module Folder"
        fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
        fileFilter = FileNameExtensionFilter("Flow Graph Files (*.flow.kt)", "kt")
        isAcceptAllFileFilterUsed = true
    }

    if (fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return FileOpenResult()

    val selected = fileChooser.selectedFile
    if (selected.isFile) return FileOpenResult(file = selected)

    // Directory selected — find .flow.kt at the conventional module path
    val flowFile = resolveFlowKtFromModule(selected)
    return if (flowFile != null) {
        FileOpenResult(file = flowFile)
    } else {
        FileOpenResult(error = "No .flow.kt found in ${selected.name}/src/commonMain/kotlin/io/codenode/...")
    }
}

/**
 * Resolves the .flow.kt file from a module directory.
 *
 * Looks at: {moduleDir}/src/commonMain/kotlin/io/codenode/{modulename}/{ModuleName}.flow.kt
 * The module name is derived from the directory name (lowercased for the package path,
 * original case for the filename).
 */
private fun resolveFlowKtFromModule(moduleDir: File): File? {
    val moduleName = moduleDir.name
    val packageName = moduleName.lowercase()
    val flowFile = moduleDir.resolve("src/commonMain/kotlin/io/codenode/$packageName/$moduleName.flow.kt")
    return if (flowFile.exists()) flowFile else null
}

/**
 * Show directory chooser for module compilation output
 *
 * @param title Dialog title (default: "Select Output Directory for KMP Module")
 */
fun showDirectoryChooser(title: String = "Select Output Directory for KMP Module"): File? {
    val startDir = System.getProperty("codenode.project.dir")?.let { File(it) }
        ?: File(System.getProperty("user.dir"))
    val fileChooser = JFileChooser(startDir).apply {
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
 * Walks up from a starting directory to find the module root (directory containing build.gradle.kts).
 * Stops after 10 levels to avoid traversing too far up.
 *
 * @param startDir The directory to start searching from
 * @return The module root directory, or null if not found
 */
private fun findModuleRoot(startDir: File?): File? {
    var dir = startDir
    var depth = 0
    while (dir != null && depth < 10) {
        if (File(dir, "build.gradle.kts").exists()) return dir
        dir = dir.parentFile
        depth++
    }
    return null
}


/**
 * Main entry point for the standalone Graph Editor application
 */
fun main(args: Array<String> = emptyArray()) {
    // Resolve project directory from (in priority order):
    // 1. --project <path> command-line argument
    // 2. CODENODE_PROJECT_DIR environment variable
    // 3. Current working directory (legacy behavior)
    val projectDirArg = args.indexOf("--project").let { idx ->
        if (idx >= 0 && idx + 1 < args.size) args[idx + 1] else null
    }
    val projectDirEnv = System.getenv("CODENODE_PROJECT_DIR")
    val resolvedProjectDir = (projectDirArg ?: projectDirEnv)?.let { File(it).absoluteFile }

    // Store the resolved project directory for downstream use.
    // Do NOT set user.dir — it breaks JFileChooser directory navigation.
    if (resolvedProjectDir != null) {
        System.setProperty("codenode.project.dir", resolvedProjectDir.absolutePath)
    }

    // Koin DI is initialized with an empty module set.
    // Project modules register their Koin modules at runtime when loaded.
    startKoin {
        modules(module { })
    }

    application {
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
}
