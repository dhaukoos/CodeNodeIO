/*
 * CodeGeneratorPanel - UI panel for configurable code generation
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.fbpdsl.model.InformationPacketType
import io.codenode.flowgraphgenerate.model.FolderNode
import io.codenode.flowgraphgenerate.model.GenerationPath
import io.codenode.flowgraphgenerate.model.TriState
import io.codenode.grapheditor.viewmodel.CodeGeneratorPanelState
import io.codenode.grapheditor.viewmodel.CodeGeneratorViewModel

@Composable
fun CodeGeneratorPanel(
    viewModel: CodeGeneratorViewModel,
    ipTypes: List<InformationPacketType> = emptyList(),
    onGenerate: () -> Unit = {},
    onCreateRepositoryModule: ((String) -> Unit)? = null,
    onRemoveRepositoryModule: ((String) -> Unit)? = null,
    moduleExists: (String) -> Boolean = { false },
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .width(250.dp)
            .background(Color(0xFFF5F5F5))
            .border(1.dp, Color(0xFFE0E0E0))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Code Generator", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Divider()

            PathSelector(
                selectedPath = state.selectedPath,
                isExpanded = state.pathDropdownExpanded,
                onExpandedChange = { viewModel.setPathDropdownExpanded(it) },
                onPathSelected = { viewModel.selectPath(it) }
            )

            InputSelector(state = state, viewModel = viewModel, ipTypes = ipTypes)

            Divider()
            FileTreeView(
                folders = state.fileTree.folders,
                onFolderToggle = { viewModel.toggleFolder(it) },
                onFileToggle = { folder, file -> viewModel.toggleFile(folder, file) },
                modifier = Modifier.weight(1f)
            )

            if (state.selectedPath == GenerationPath.REPOSITORY && state.selectedIPTypeId != null) {
                Divider()
                RepositoryActions(
                    ipTypeId = state.selectedIPTypeId!!,
                    moduleExists = moduleExists,
                    onCreateRepositoryModule = onCreateRepositoryModule,
                    onRemoveRepositoryModule = onRemoveRepositoryModule
                )
            }

            Divider()
            val generateEnabled = when (state.selectedPath) {
                GenerationPath.GENERATE_MODULE -> true
                GenerationPath.REPOSITORY -> state.selectedIPTypeId != null
                GenerationPath.UI_FBP -> state.selectedUIFilePath != null
            }
            Button(
                onClick = onGenerate,
                enabled = generateEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate", fontSize = 12.sp)
            }
    }
}

@Composable
private fun PathSelector(
    selectedPath: GenerationPath,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPathSelected: (GenerationPath) -> Unit
) {
    val pathLabels = mapOf(
        GenerationPath.GENERATE_MODULE to "Generate Module",
        GenerationPath.REPOSITORY to "Repository",
        GenerationPath.UI_FBP to "UI-FBP"
    )

    Column {
        Text("Generation Path", fontSize = 11.sp, color = Color.Gray)
        Box {
            OutlinedButton(
                onClick = { onExpandedChange(true) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(pathLabels[selectedPath] ?: "", fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text("\u25BC", fontSize = 10.sp)
            }
            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                GenerationPath.entries.forEach { path ->
                    DropdownMenuItem(onClick = { onPathSelected(path) }) {
                        Text(pathLabels[path] ?: path.name, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun InputSelector(
    state: CodeGeneratorPanelState,
    viewModel: CodeGeneratorViewModel,
    ipTypes: List<InformationPacketType>
) {
    when (state.selectedPath) {
        GenerationPath.GENERATE_MODULE -> {
            Text("Module", fontSize = 11.sp, color = Color.Gray)
            Text(state.flowGraphName, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        GenerationPath.REPOSITORY -> {
            val customTypes = ipTypes.filter { it.id !in setOf("ip_any", "ip_int", "ip_double", "ip_boolean", "ip_string") }
            Column {
                Text("IP Type", fontSize = 11.sp, color = Color.Gray)
                Box {
                    OutlinedButton(
                        onClick = { viewModel.setIPTypeDropdownExpanded(true) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val selectedName = customTypes.firstOrNull { it.id == state.selectedIPTypeId }?.typeName ?: "Select..."
                        Text(selectedName, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text("\u25BC", fontSize = 10.sp)
                    }
                    DropdownMenu(
                        expanded = state.ipTypeDropdownExpanded,
                        onDismissRequest = { viewModel.setIPTypeDropdownExpanded(false) }
                    ) {
                        if (customTypes.isEmpty()) {
                            DropdownMenuItem(onClick = {}, enabled = false) {
                                Text("No custom IP Types available", fontSize = 11.sp, color = Color.Gray)
                            }
                        } else {
                            customTypes.forEach { ipType ->
                                DropdownMenuItem(onClick = {
                                    val entityName = ipType.typeName
                                    val pluralName = "${entityName}s"
                                    viewModel.selectIPType(ipType.id, entityName, pluralName)
                                }) {
                                    Text(ipType.typeName, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        GenerationPath.UI_FBP -> {
            Text("UI File", fontSize = 11.sp, color = Color.Gray)
            OutlinedButton(
                onClick = {
                    val result = showFileOpenDialog()
                    val file = result.file
                    if (file != null) {
                        val moduleName = file.nameWithoutExtension
                        viewModel.selectUIFile(file.absolutePath, file.name, moduleName)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(state.selectedUIFileName ?: "Select UI file...", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FileTreeView(
    folders: List<FolderNode>,
    onFolderToggle: (String) -> Unit,
    onFileToggle: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text("Files to Generate", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        if (folders.isEmpty()) {
            Text("Select a generation path to see available files.",
                fontSize = 11.sp, color = Color.Gray)
        } else {
            folders.forEach { folder ->
                FolderRow(
                    folder = folder,
                    onFolderToggle = { onFolderToggle(folder.name) },
                    onFileToggle = { fileName -> onFileToggle(folder.name, fileName) }
                )
            }
        }
    }
}

@Composable
private fun FolderRow(
    folder: FolderNode,
    onFolderToggle: () -> Unit,
    onFileToggle: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onFolderToggle() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            TriStateCheckbox(
                state = when (folder.selectionState) {
                    TriState.ALL -> ToggleableState.On
                    TriState.NONE -> ToggleableState.Off
                    TriState.PARTIAL -> ToggleableState.Indeterminate
                },
                onClick = { onFolderToggle() },
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "${folder.name}/",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1565C0)
            )
        }
        folder.files.forEach { file ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp)
                    .clickable { onFileToggle(file.name) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = file.isSelected,
                    onCheckedChange = { onFileToggle(file.name) },
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(file.name, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun RepositoryActions(
    ipTypeId: String,
    moduleExists: (String) -> Boolean,
    onCreateRepositoryModule: ((String) -> Unit)?,
    onRemoveRepositoryModule: ((String) -> Unit)?
) {
    val exists = moduleExists(ipTypeId)
    if (exists && onRemoveRepositoryModule != null) {
        Button(
            onClick = { onRemoveRepositoryModule(ipTypeId) },
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Remove Repository Module", fontSize = 12.sp, color = MaterialTheme.colors.onError)
        }
    } else if (!exists && onCreateRepositoryModule != null) {
        Button(
            onClick = { onCreateRepositoryModule(ipTypeId) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Repository Module", fontSize = 12.sp)
        }
    }
}
