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
    moduleFlowDir: File?,
    currentModuleDir: File?,
    projectRoot: File,
    codeEditorViewModel: CodeEditorViewModel,
    pendingEditorAction: (() -> Unit)?,
    onPendingEditorActionChanged: ((() -> Unit)?) -> Unit,
    onModuleRootDirChanged: (File?) -> Unit,
    onStatusMessage: (String) -> Unit,
    onRegistryVersionIncrement: () -> Unit = {},
    onIpTypesVersionIncrement: () -> Unit,
) {
    // File open dialog — scoped to module's flow/ directory when available
    if (showOpenDialog) {
        LaunchedEffect(Unit) {
            val openResult = showFileOpenDialog(initialDir = moduleFlowDir)
            val file = openResult.file
            if (file != null) {
                try {
                    val parser = FlowKtParser()
                    parser.setTypeResolver { typeName ->
                        ipTypeRegistry.getByTypeName(typeName)?.payloadType
                    }
                    val parseResult = parser.parseFlowKt(file.readText())
                    val loadedGraph = parseResult.graph
                    if (parseResult.isSuccess && loadedGraph != null) {
                        val resolvedGraph = resolveConnectionIPTypes(loadedGraph, ipTypeRegistry, parseResult.portTypeNameHints)
                        graphState.setGraph(resolvedGraph, markDirty = false)
                        onModuleRootDirChanged(findModuleRoot(file.parentFile))
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

    // Save handler: deterministic write to workspace module's flow/ directory
    if (showModuleSaveDialog) {
        LaunchedEffect(Unit) {
            if (currentModuleDir == null) {
                onStatusMessage("No module loaded — open or create a module first")
            } else {
                val ipTypeNamesMap = buildMap {
                    for (ipType in ipTypeRegistry.getAllTypes()) {
                        put(ipType.id, ipType.typeName)
                    }
                }
                val result = moduleSaveService.saveFlowKtOnly(
                    flowGraph = graphState.flowGraph,
                    outputDir = currentModuleDir.parentFile ?: currentModuleDir,
                    moduleName = currentModuleDir.name,
                    ipTypeNames = ipTypeNamesMap,
                    codeNodeClassLookup = { nodeName ->
                        registry.getByName(nodeName)?.let {
                            it::class.qualifiedName
                        }
                    }
                )
                if (result.success) {
                    onModuleRootDirChanged(result.moduleDir)
                    graphState.markAsSaved()
                    onStatusMessage("Saved ${graphState.flowGraph.name}.flow.kt")
                } else {
                    onStatusMessage("Save error: ${result.errorMessage}")
                }
            }
            onShowModuleSaveDialogChanged(false)
        }
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

private fun ensureFbpDslDependency(buildFile: File) {
    if (!buildFile.exists()) return
    val content = buildFile.readText()
    if (content.contains("io.codenode:fbpDsl") || content.contains("\":fbpDsl\"")) return

    val marker = "commonMain.dependencies {"
    val insertionPoint = content.indexOf(marker)
    if (insertionPoint == -1) return

    val afterMarker = insertionPoint + marker.length
    val updated = content.substring(0, afterMarker) +
        "\n            implementation(\"io.codenode:fbpDsl\")" +
        content.substring(afterMarker)
    buildFile.writeText(updated)
}
