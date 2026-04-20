/*
 * GraphEditorDialogs - File operation and confirmation dialogs
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.codenode.fbpdsl.model.InformationPacketType
import io.codenode.flowgraphgenerate.generator.EntityModuleSpec
import io.codenode.flowgraphgenerate.generator.UIFBPInterfaceGenerator
import io.codenode.flowgraphgenerate.parser.UIComposableParser
import io.codenode.flowgraphgenerate.save.ModuleSaveService
import io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry
import io.codenode.flowgraphinspect.viewmodel.CodeEditorViewModel
import io.codenode.flowgraphpersist.serialization.FlowKtParser
import io.codenode.flowgraphtypes.registry.IPTypeRegistry
import io.codenode.grapheditor.state.GraphState
import io.codenode.grapheditor.util.findModuleRoot
import io.codenode.grapheditor.util.resolveConnectionIPTypes
import java.io.File

/**
 * Renders all dialog composables and LaunchedEffects for the graph editor.
 *
 * Includes: file open, save module, FlowGraph properties, remove repository module
 * confirmation, and unsaved changes dialogs.
 */
@Composable
fun GraphEditorDialogs(
    showOpenDialog: Boolean,
    onShowOpenDialogChanged: (Boolean) -> Unit,
    showModuleSaveDialog: Boolean,
    onShowModuleSaveDialogChanged: (Boolean) -> Unit,
    showGenerateDialog: Boolean = false,
    onShowGenerateDialogChanged: (Boolean) -> Unit = {},
    showGenerateUIFBPDialog: Boolean = false,
    onShowGenerateUIFBPDialogChanged: (Boolean) -> Unit = {},
    showFlowGraphPropertiesDialog: Boolean,
    onShowFlowGraphPropertiesDialogChanged: (Boolean) -> Unit,
    showRemoveConfirmDialog: Boolean,
    onShowRemoveConfirmDialogChanged: (Boolean) -> Unit,
    removeTargetIPType: InformationPacketType?,
    onRemoveTargetIPTypeChanged: (InformationPacketType?) -> Unit,
    showUnsavedChangesDialog: Boolean,
    onShowUnsavedChangesDialogChanged: (Boolean) -> Unit,
    graphState: GraphState,
    ipTypeRegistry: IPTypeRegistry,
    registry: NodeDefinitionRegistry,
    moduleSaveService: ModuleSaveService,
    saveLocationRegistry: MutableMap<String, File>,
    projectRoot: File,
    codeEditorViewModel: CodeEditorViewModel,
    pendingEditorAction: (() -> Unit)?,
    onPendingEditorActionChanged: ((() -> Unit)?) -> Unit,
    onModuleRootDirChanged: (File?) -> Unit,
    onStatusMessage: (String) -> Unit,
    onIpTypesVersionIncrement: () -> Unit,
) {
    // File open dialog
    if (showOpenDialog) {
        LaunchedEffect(Unit) {
            val openResult = showFileOpenDialog()
            val file = openResult.file
            if (file != null) {
                try {
                    // T062: Only support .flow.kt files (removed .flow.kts support)
                    val parser = FlowKtParser()
                    parser.setTypeResolver { typeName ->
                        ipTypeRegistry.getByTypeName(typeName)?.payloadType
                    }
                    val parseResult = parser.parseFlowKt(file.readText())
                    val loadedGraph = parseResult.graph
                    if (parseResult.isSuccess && loadedGraph != null) {
                        // Auto-resolve connection IP types from source port data types
                        // Pass portTypeNameHints for typealias IP types that can't be resolved via reflection
                        println("[DEBUG-OPEN] portTypeNameHints: ${parseResult.portTypeNameHints.size} entries")
                        parseResult.portTypeNameHints.forEach { (k, v) -> println("[DEBUG-OPEN]   hint: $k -> $v") }
                        println("[DEBUG-OPEN] ipTypeRegistry has ${ipTypeRegistry.getAllTypes().size} types:")
                        ipTypeRegistry.getAllTypes().forEach { t -> println("[DEBUG-OPEN]   reg: ${t.typeName} (id=${t.id})") }
                        val resolvedGraph = resolveConnectionIPTypes(loadedGraph, ipTypeRegistry, parseResult.portTypeNameHints)
                        println("[DEBUG-OPEN] Resolved ${resolvedGraph.connections.size} connections:")
                        resolvedGraph.connections.forEach { c ->
                            println("[DEBUG-OPEN]   ${c.sourcePortId} -> ${c.targetPortId} ipTypeId=${c.ipTypeId}")
                        }
                        graphState.setGraph(resolvedGraph, markDirty = false)
                        onModuleRootDirChanged(findModuleRoot(file.parentFile))
                        // Register save location so re-save skips the directory prompt
                        findModuleRoot(file.parentFile)?.parentFile?.let { parentDir ->
                            saveLocationRegistry[loadedGraph.name] = parentDir
                        }
                        onStatusMessage("Opened ${file.name}")
                    } else {
                        onStatusMessage("Error opening: ${parseResult.errorMessage}")
                    }
                } catch (e: Exception) {
                    onStatusMessage("Error opening: ${e.message}")
                }
            } else if (openResult.error != null) {
                onStatusMessage(openResult.error)
            }
            onShowOpenDialogChanged(false)
        }
    }

    // Save handler: writes only the .flow.kt file (no code generation)
    if (showModuleSaveDialog) {
        LaunchedEffect(Unit) {
            val flowGraphName = graphState.flowGraph.name
            val savedDir = saveLocationRegistry[flowGraphName]

            // Determine output directory: use registry or prompt user
            val outputDir = if (savedDir != null && savedDir.exists()) {
                savedDir
            } else {
                if (savedDir != null) {
                    saveLocationRegistry.remove(flowGraphName)
                }
                showDirectoryChooser("Save Flow Graph To")
            }

            if (outputDir != null) {
                val ipTypeNamesMap = buildMap {
                    for (ipType in ipTypeRegistry.getAllTypes()) {
                        put(ipType.id, ipType.typeName)
                    }
                }
                val result = moduleSaveService.saveFlowKtOnly(
                    flowGraph = graphState.flowGraph,
                    outputDir = outputDir,
                    ipTypeNames = ipTypeNamesMap,
                    codeNodeClassLookup = { nodeName ->
                        registry.getByName(nodeName)?.let {
                            it::class.qualifiedName
                        }
                    }
                )
                if (result.success) {
                    saveLocationRegistry[flowGraphName] = outputDir
                    onModuleRootDirChanged(result.moduleDir)
                    onStatusMessage("Saved ${graphState.flowGraph.name}.flow.kt")
                } else {
                    onStatusMessage("Save error: ${result.errorMessage}")
                }
            }
            onShowModuleSaveDialogChanged(false)
        }
    }

    // Generate Module handler: full module code generation
    if (showGenerateDialog) {
        LaunchedEffect(Unit) {
            val flowGraphName = graphState.flowGraph.name
            val savedDir = saveLocationRegistry[flowGraphName]

            val outputDir = if (savedDir != null && savedDir.exists()) {
                savedDir
            } else {
                if (savedDir != null) {
                    saveLocationRegistry.remove(flowGraphName)
                }
                showDirectoryChooser("Generate Module To")
            }

            if (outputDir != null) {
                val ipTypePropertiesMap = buildMap {
                    for (ipTypeId in ipTypeRegistry.getEntityModuleIPTypeIds()) {
                        val props = ipTypeRegistry.getCustomTypeProperties(ipTypeId)
                        if (props != null) {
                            put(ipTypeId, props.map { prop ->
                                io.codenode.flowgraphgenerate.generator.EntityProperty(
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
                            })
                        }
                    }
                }
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
                    onModuleRootDirChanged(result.moduleDir)
                    val created = result.filesCreated.size
                    val overwritten = result.filesOverwritten.size
                    val deleted = result.filesDeleted.size
                    onStatusMessage("Generated ${result.moduleDir?.name}: $created created, $overwritten overwritten, $deleted deleted")
                } else {
                    onStatusMessage("Generate error: ${result.errorMessage}")
                }
            }
            onShowGenerateDialogChanged(false)
        }
    }

    // Generate UI-FBP handler: parse UI file → generate ViewModel, State, Source, Sink
    if (showGenerateUIFBPDialog) {
        LaunchedEffect(Unit) {
            val fileResult = showFileOpenDialog()
            val file = fileResult.file
            if (file != null) {
                try {
                    val parser = UIComposableParser()
                    val parseResult = parser.parse(file.readText())
                    val spec = parseResult.spec
                    if (parseResult.isSuccess && spec != null) {
                        val generator = UIFBPInterfaceGenerator()
                        val genResult = generator.generateAll(spec)
                        if (genResult.success) {
                            val moduleDir = file.parentFile?.parentFile?.parentFile?.parentFile?.parentFile?.parentFile
                            if (moduleDir != null) {
                                for (genFile in genResult.filesGenerated) {
                                    val targetFile = File(moduleDir, genFile.relativePath)
                                    targetFile.parentFile?.mkdirs()
                                    targetFile.writeText(genFile.content)
                                }
                                onStatusMessage("Generated UI-FBP: ${genResult.filesGenerated.size} files for ${spec.moduleName}")
                            } else {
                                onStatusMessage("Could not determine module root directory")
                            }
                        } else {
                            onStatusMessage("Generation error: ${genResult.errorMessage}")
                        }
                    } else {
                        onStatusMessage("Parse error: ${parseResult.errorMessage}")
                    }
                } catch (e: Exception) {
                    onStatusMessage("Error: ${e.message}")
                }
            }
            onShowGenerateUIFBPDialogChanged(false)
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
            onDismiss = { onShowFlowGraphPropertiesDialogChanged(false) }
        )
    }

    // Remove Repository Module confirmation dialog
    if (showRemoveConfirmDialog && removeTargetIPType != null) {
        val ipType = removeTargetIPType
        val moduleName = EntityModuleSpec.fromIPType(
            ipTypeName = ipType.typeName,
            sourceIPTypeId = ipType.id,
            properties = emptyList()
        ).pluralName
        AlertDialog(
            onDismissRequest = {
                onShowRemoveConfirmDialogChanged(false)
                onRemoveTargetIPTypeChanged(null)
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
                        onShowRemoveConfirmDialogChanged(false)
                        onRemoveTargetIPTypeChanged(null)

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
                        onIpTypesVersionIncrement()
                        onStatusMessage(result)
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
                        onShowRemoveConfirmDialogChanged(false)
                        onRemoveTargetIPTypeChanged(null)
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
                onShowUnsavedChangesDialogChanged(false)
                pendingEditorAction?.invoke()
                onPendingEditorActionChanged(null)
            },
            onDiscard = {
                codeEditorViewModel.discardChanges()
                onShowUnsavedChangesDialogChanged(false)
                pendingEditorAction?.invoke()
                onPendingEditorActionChanged(null)
            },
            onCancel = {
                onShowUnsavedChangesDialogChanged(false)
                onPendingEditorActionChanged(null)
            }
        )
    }
}
