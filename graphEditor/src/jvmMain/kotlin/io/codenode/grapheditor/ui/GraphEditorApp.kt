/*
 * GraphEditorApp - Main composable for the GraphEditor application
 * Orchestrates all sub-composables: state init, toolbar, content, status bar, dialogs
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.InformationPacketType
import io.codenode.flowgraphcompose.viewmodel.CanvasInteractionViewModel
import io.codenode.flowgraphcompose.viewmodel.PropertiesPanelViewModel
import io.codenode.flowgraphexecute.ModuleSessionFactory
import io.codenode.flowgraphgenerate.compilation.CompilationService
import io.codenode.flowgraphgenerate.save.ModuleSaveService
import io.codenode.flowgraphgenerate.save.ModuleScaffoldingGenerator
import io.codenode.flowgraphgenerate.viewmodel.IPGeneratorViewModel
import io.codenode.flowgraphgenerate.viewmodel.NodeGeneratorViewModel
import io.codenode.flowgraphinspect.discovery.DynamicPreviewDiscovery
import io.codenode.flowgraphinspect.viewmodel.CodeEditorViewModel
import io.codenode.flowgraphinspect.viewmodel.IPPaletteViewModel
import io.codenode.flowgraphinspect.viewmodel.NodePaletteViewModel
import io.codenode.flowgraphtypes.discovery.IPTypeDiscovery
import io.codenode.flowgraphtypes.generator.IPTypeFileGenerator
import io.codenode.fbpdsl.model.FeatureGate
import io.codenode.fbpdsl.subscription.LocalFeatureGate
import io.codenode.flowgraphtypes.repository.FileIPTypeRepository
import io.codenode.flowgraphtypes.repository.IPTypeMigration
import io.codenode.grapheditor.state.GroupNodesCommand
import io.codenode.grapheditor.state.UngroupNodeCommand
import io.codenode.grapheditor.viewmodel.EditorDialog
import io.codenode.grapheditor.viewmodel.GraphEditorViewModel
import io.codenode.grapheditor.viewmodel.LocalSharedState
import io.codenode.grapheditor.viewmodel.SharedStateProvider
import io.codenode.grapheditor.viewmodel.WorkspaceViewModel
import kotlinx.coroutines.flow.drop
import java.io.File

/**
 * Main composable for the GraphEditor application.
 * Integrates all components: canvas, palette, properties, controls, and error display.
 *
 * Acts as a high-level orchestrator composing:
 * - [rememberGraphEditorState] for core state initialization
 * - [SharedStateProvider] for ViewModel-based state sharing
 * - [TopToolbar] for editor toolbar actions
 * - [GraphEditorContent] for the main panel layout
 * - [StatusBar] for status information
 * - [GraphEditorDialogs] for file/save/properties dialogs
 */
@Composable
fun GraphEditorApp(
    onTitleChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Core state initialization (graphState, registries, project root, etc.)
    val editorState = rememberGraphEditorState()
    val graphState = editorState.graphState
    val undoRedoManager = editorState.undoRedoManager
    val propertyChangeTracker = editorState.propertyChangeTracker
    val registry = editorState.registry
    val graphNodeTemplateRegistry = editorState.graphNodeTemplateRegistry
    val ipTypeRegistry = editorState.ipTypeRegistry
    val projectRoot = editorState.projectRoot
    var moduleRootDir by editorState.moduleRootDir
    var statusMessage by editorState.statusMessage
    var registryVersion by editorState.registryVersion
    var ipTypesVersion by editorState.ipTypesVersion

    // WorkspaceViewModel for module context
    val workspaceViewModel = remember { WorkspaceViewModel() }
    LaunchedEffect(Unit) {
        workspaceViewModel.restoreState()
        workspaceViewModel.currentModuleDir.value?.let { moduleRootDir = it }
    }
    val currentModuleName by workspaceViewModel.currentModuleName.collectAsState()
    val mruModules by workspaceViewModel.mruModules.collectAsState()
    val activeFlowGraphName by workspaceViewModel.activeFlowGraphName.collectAsState()

    // Panel collapse/expand state
    var isNodePanelExpanded by remember { mutableStateOf(true) }
    var isIPPanelExpanded by remember { mutableStateOf(true) }
    var isPropertiesPanelExpanded by remember { mutableStateOf(true) }

    // Runtime preview session and panel state
    var isRuntimePanelExpanded by remember { mutableStateOf(false) }
    var isCodeGeneratorPanelExpanded by remember { mutableStateOf(false) }

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
        // Sync workspace when moduleRootDir changes externally (e.g., from opening a .flow.kt)
        moduleRootDir?.let { dir ->
            if (dir.absolutePath != workspaceViewModel.currentModuleDir.value?.absolutePath) {
                workspaceViewModel.openModule(dir)
            }
        }
    }

    // NodePaletteViewModel for the Node Palette
    val nodePaletteViewModel = remember { NodePaletteViewModel() }

    // CodeGeneratorViewModel for the Code Generator panel
    val codeGeneratorViewModel = remember { io.codenode.grapheditor.viewmodel.CodeGeneratorViewModel() }

    // CodeEditorViewModel for the code editor
    val codeEditorViewModel = remember { CodeEditorViewModel() }
    var editorViewMode by remember { mutableStateOf(ViewMode.VISUAL) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var pendingEditorAction by remember { mutableStateOf<(() -> Unit)?>(null) }
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
    // Resolve the tool's own source root (CodeNodeIO) for INTERNAL tier IP type discovery.
    // This may differ from projectRoot when CODENODE_PROJECT_DIR points to a user project.
    val toolRoot = remember {
        var dir = java.io.File(System.getProperty("user.dir"))
        while (dir.parentFile != null && !java.io.File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile
        }
        // Only use as toolRoot if the iptypes module exists here
        val iptypesModule = java.io.File(dir, "iptypes/src/commonMain/kotlin")
        if (iptypesModule.isDirectory) dir else null
    }
    val discovery = remember { IPTypeDiscovery(projectRoot, modulePaths, toolRoot) }
    val ipTypeFileGenerator = remember { IPTypeFileGenerator(projectRoot) }
    // Discover IP types from filesystem on startup
    LaunchedEffect(Unit) {
        // One-time migration from legacy JSON to filesystem (skips Module-level types)
        val migration = IPTypeMigration(ipTypeFileGenerator, discovery)
        migration.migrateIfNeeded()

        val discovered = discovery.discoverAll()
        println("[DEBUG-STARTUP] projectRoot: ${projectRoot.absolutePath}")
        println("[DEBUG-STARTUP] toolRoot: ${toolRoot?.absolutePath ?: "null"}")
        println("[DEBUG-STARTUP] discovered ${discovered.size} IP types:")
        discovered.forEach { meta -> println("[DEBUG-STARTUP]   ${meta.typeId}: ${meta.typeName} (tier=${meta.tier}, file=${meta.filePath})") }
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
            val pluralName = io.codenode.flowgraphgenerate.generator.pluralize(ipType.typeName)
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
    var showNewFlowGraphDialog by remember { mutableStateOf(false) }
    var showModulePropertiesDialog by remember { mutableStateOf(false) }
    var moduleDialogMode by remember { mutableStateOf(ModuleDialogMode.CREATE) }
    var showRemoveConfirmDialog by remember { mutableStateOf(false) }
    var pendingSwitchModuleDir by remember { mutableStateOf<File?>(null) }
    var showModuleSwitchConfirmDialog by remember { mutableStateOf(false) }
    var removeTargetIPType by remember { mutableStateOf<InformationPacketType?>(null) }

    // Create/recreate RuntimeSession synchronously when module changes
    // (must be synchronous so runtimeSession is always in sync with moduleRootDir
    // during the same recomposition — avoids ClassCastException on module switch)
    // Pass the editor's FlowGraph so animation connection IDs match the Canvas.
    val runtimeSession = remember(moduleRootDir, graphState.flowGraph) {
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
    val featureGate: FeatureGate = remember { LocalFeatureGate() }

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
        val name = graphState.flowGraph.name
        if (name != "New Graph") {
            workspaceViewModel.setActiveFlowGraph(name)
        } else {
            workspaceViewModel.setActiveFlowGraph("")
        }
    }

    LaunchedEffect(activeFlowGraphName) {
        val title = if (!activeFlowGraphName.isNullOrEmpty()) {
            "CodeNodeIO \u2014 $activeFlowGraphName"
        } else {
            "CodeNodeIO"
        }
        onTitleChanged(title)
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
                currentModuleName = currentModuleName,
                mruModules = mruModules,
                onSwitchModule = { moduleDir ->
                    if (graphState.isDirty) {
                        pendingSwitchModuleDir = moduleDir
                        showModuleSwitchConfirmDialog = true
                    } else {
                        workspaceViewModel.switchModule(moduleDir)
                        moduleRootDir = moduleDir
                        statusMessage = "Switched to module: ${moduleDir.name}"
                    }
                },
                onOpenModule = {
                    val chooser = javax.swing.JFileChooser().apply {
                        fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                        dialogTitle = "Open Module Directory"
                    }
                    if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                        val selectedDir = chooser.selectedFile
                        if (java.io.File(selectedDir, "build.gradle.kts").exists()) {
                            workspaceViewModel.openModule(selectedDir)
                            moduleRootDir = selectedDir
                            statusMessage = "Opened module: ${selectedDir.name}"
                        } else {
                            statusMessage = "Not a valid module directory (no build.gradle.kts)"
                        }
                    }
                },
                onCreateModule = {
                    moduleDialogMode = ModuleDialogMode.CREATE
                    showModulePropertiesDialog = true
                },
                onModuleSettings = {
                    moduleDialogMode = ModuleDialogMode.EDIT
                    showModulePropertiesDialog = true
                },
                hasModule = workspaceViewModel.currentModuleDir.value != null,
                onNew = { showNewFlowGraphDialog = true },
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

            GraphEditorContent(
                graphState = graphState,
                undoRedoManager = undoRedoManager,
                registry = registry,
                graphNodeTemplateRegistry = graphNodeTemplateRegistry,
                ipTypeRegistry = ipTypeRegistry,
                projectRoot = projectRoot,
                moduleRootDir = moduleRootDir,
                nodeGeneratorViewModel = nodeGeneratorViewModel,
                nodePaletteViewModel = nodePaletteViewModel,
                ipPaletteViewModel = ipPaletteViewModel,
                ipGeneratorViewModel = ipGeneratorViewModel,
                codeEditorViewModel = codeEditorViewModel,
                propertiesPanelViewModel = propertiesPanelViewModel,
                canvasInteractionViewModel = canvasInteractionViewModel,
                moduleSaveService = moduleSaveService,
                runtimeSession = runtimeSession,
                runtimeExecutionState = runtimeExecutionState,
                animateDataFlow = animateDataFlow,
                activeAnimations = activeAnimations,
                nodeTypes = nodeTypes,
                ipTypes = ipTypes,
                selectedIPType = selectedIPType,
                onSelectedIPTypeChanged = { selectedIPType = it },
                editorViewMode = editorViewMode,
                onEditorViewModeChanged = { editorViewMode = it },
                isInsideGraphNode = isInsideGraphNode,
                currentGraphNodeName = currentGraphNodeName,
                onStatusMessage = { statusMessage = it },
                onRegistryVersionIncrement = { registryVersion++ },
                onIpTypesVersionIncrement = { ipTypesVersion++ },
                onModuleRootDirChanged = { moduleRootDir = it },
                showUnsavedChangesDialog = showUnsavedChangesDialog,
                onShowUnsavedChangesDialogChanged = { showUnsavedChangesDialog = it },
                pendingEditorAction = pendingEditorAction,
                onPendingEditorActionChanged = { pendingEditorAction = it },
                showRemoveConfirmDialog = showRemoveConfirmDialog,
                onShowRemoveConfirmDialogChanged = { showRemoveConfirmDialog = it },
                removeTargetIPType = removeTargetIPType,
                onRemoveTargetIPTypeChanged = { removeTargetIPType = it },
                isNodePanelExpanded = isNodePanelExpanded,
                onNodePanelExpandedChanged = { isNodePanelExpanded = it },
                isIPPanelExpanded = isIPPanelExpanded,
                onIPPanelExpandedChanged = { isIPPanelExpanded = it },
                isPropertiesPanelExpanded = isPropertiesPanelExpanded,
                onPropertiesPanelExpandedChanged = { isPropertiesPanelExpanded = it },
                isRuntimePanelExpanded = isRuntimePanelExpanded,
                onRuntimePanelExpandedChanged = { isRuntimePanelExpanded = it },
                featureGate = featureGate,
                codeGeneratorViewModel = codeGeneratorViewModel,
                isCodeGeneratorPanelExpanded = isCodeGeneratorPanelExpanded,
                onCodeGeneratorPanelExpandedChanged = { isCodeGeneratorPanelExpanded = it },
            )

            Divider()

            // Status bar
            StatusBar(
                message = statusMessage,
                nodeCount = graphState.flowGraph.rootNodes.size,
                connectionCount = graphState.flowGraph.connections.size,
                selectionCount = graphState.selectionState.totalSelectionCount
            )
        }

        GraphEditorDialogs(
            showOpenDialog = showOpenDialog,
            onShowOpenDialogChanged = { showOpenDialog = it },
            showModuleSaveDialog = showModuleSaveDialog,
            onShowModuleSaveDialogChanged = { showModuleSaveDialog = it },
            showRemoveConfirmDialog = showRemoveConfirmDialog,
            onShowRemoveConfirmDialogChanged = { showRemoveConfirmDialog = it },
            removeTargetIPType = removeTargetIPType,
            onRemoveTargetIPTypeChanged = { removeTargetIPType = it },
            showUnsavedChangesDialog = showUnsavedChangesDialog,
            onShowUnsavedChangesDialogChanged = { showUnsavedChangesDialog = it },
            graphState = graphState,
            ipTypeRegistry = ipTypeRegistry,
            registry = registry,
            moduleSaveService = moduleSaveService,
            moduleFlowDir = workspaceViewModel.currentModuleDir.value?.let { resolveFlowDir(it) },
            currentModuleDir = workspaceViewModel.currentModuleDir.value,
            projectRoot = projectRoot,
            codeEditorViewModel = codeEditorViewModel,
            pendingEditorAction = pendingEditorAction,
            onPendingEditorActionChanged = { pendingEditorAction = it },
            onModuleRootDirChanged = { moduleRootDir = it },
            onStatusMessage = { statusMessage = it },
            onRegistryVersionIncrement = { registryVersion++ },
            onIpTypesVersionIncrement = { ipTypesVersion++ },
        )

        if (showNewFlowGraphDialog) {
            val moduleDir = workspaceViewModel.currentModuleDir.value
            val flowDir = moduleDir?.let { resolveFlowDir(it) }
            NewFlowGraphDialog(
                moduleFlowDir = flowDir,
                onConfirm = { name ->
                    showNewFlowGraphDialog = false
                    val newGraph = flowGraph(
                        name = name,
                        version = "1.0.0"
                    ) {}
                    graphState.setGraph(newGraph, markDirty = true)
                    graphState.navigateToRoot()
                    workspaceViewModel.setActiveFlowGraph(name)
                    statusMessage = "Created new flowGraph: $name"
                },
                onDismiss = { showNewFlowGraphDialog = false }
            )
        }

        if (showModulePropertiesDialog) {
            ModulePropertiesDialog(
                mode = moduleDialogMode,
                existingName = currentModuleName,
                existingPath = workspaceViewModel.currentModuleDir.value?.absolutePath ?: "",
                existingPlatforms = graphState.flowGraph.targetPlatforms.toSet(),
                onCreateModule = { name, platforms ->
                    showModulePropertiesDialog = false
                    val chooser = javax.swing.JFileChooser().apply {
                        fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                        dialogTitle = "Select Parent Directory for Module"
                    }
                    if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                        try {
                            val scaffolding = ModuleScaffoldingGenerator()
                            val result = scaffolding.generate(
                                moduleName = name,
                                outputDir = chooser.selectedFile,
                                targetPlatforms = platforms
                            )
                            workspaceViewModel.createModule(result.moduleDir)
                            moduleRootDir = result.moduleDir
                            statusMessage = "Created module: $name (${result.filesCreated.size} files)"
                        } catch (e: Exception) {
                            statusMessage = "Error creating module: ${e.message}"
                        }
                    }
                },
                onDismiss = { showModulePropertiesDialog = false }
            )
        }

        if (showModuleSwitchConfirmDialog && pendingSwitchModuleDir != null) {
            AlertDialog(
                onDismissRequest = {
                    showModuleSwitchConfirmDialog = false
                    pendingSwitchModuleDir = null
                },
                title = { Text("Unsaved Changes") },
                text = { Text("You have unsaved changes. Save before switching modules?") },
                confirmButton = {
                    Row {
                        TextButton(onClick = {
                            showModuleSwitchConfirmDialog = false
                            pendingSwitchModuleDir = null
                        }) {
                            Text("Cancel")
                        }
                        TextButton(onClick = {
                            showModuleSwitchConfirmDialog = false
                            pendingSwitchModuleDir?.let { moduleDir ->
                                workspaceViewModel.switchModule(moduleDir)
                                moduleRootDir = moduleDir
                                statusMessage = "Switched to module: ${moduleDir.name}"
                            }
                            pendingSwitchModuleDir = null
                        }) {
                            Text("Don't Save")
                        }
                        TextButton(onClick = {
                            showModuleSwitchConfirmDialog = false
                            showModuleSaveDialog = true
                            pendingSwitchModuleDir?.let { moduleDir ->
                                workspaceViewModel.switchModule(moduleDir)
                                moduleRootDir = moduleDir
                                statusMessage = "Saved and switched to module: ${moduleDir.name}"
                            }
                            pendingSwitchModuleDir = null
                        }) {
                            Text("Save")
                        }
                    }
                },
                dismissButton = {}
            )
        }
        }
    }
}

private fun resolveFlowDir(moduleDir: File): File? {
    val srcDir = File(moduleDir, "src/commonMain/kotlin")
    if (!srcDir.isDirectory) return null
    return srcDir.walkTopDown()
        .filter { it.isDirectory && it.name == "flow" }
        .firstOrNull()
}
