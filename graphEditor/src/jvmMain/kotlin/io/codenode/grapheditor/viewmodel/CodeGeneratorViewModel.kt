/*
 * CodeGeneratorViewModel - State management for the Code Generator panel
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import io.codenode.flowgraphgenerate.model.GenerationFileTree
import io.codenode.flowgraphgenerate.model.GenerationFileTreeBuilder
import io.codenode.flowgraphgenerate.model.GenerationPath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CodeGeneratorPanelState(
    val isExpanded: Boolean = false,
    val selectedPath: GenerationPath = GenerationPath.GENERATE_MODULE,
    val pathDropdownExpanded: Boolean = false,
    val selectedIPTypeId: String? = null,
    val ipTypeDropdownExpanded: Boolean = false,
    val selectedUIFilePath: String? = null,
    val selectedUIFileName: String? = null,
    val fileTree: GenerationFileTree = GenerationFileTree.empty(),
    val flowGraphName: String = "New Graph"
)

class CodeGeneratorViewModel {

    private val _state = MutableStateFlow(CodeGeneratorPanelState())
    val state: StateFlow<CodeGeneratorPanelState> = _state.asStateFlow()

    fun toggleExpanded() {
        _state.value = _state.value.copy(isExpanded = !_state.value.isExpanded)
    }

    fun setPathDropdownExpanded(expanded: Boolean) {
        _state.value = _state.value.copy(pathDropdownExpanded = expanded)
    }

    fun selectPath(path: GenerationPath) {
        _state.value = _state.value.copy(
            selectedPath = path,
            pathDropdownExpanded = false,
            selectedIPTypeId = null,
            selectedUIFilePath = null,
            selectedUIFileName = null,
            fileTree = GenerationFileTree.empty()
        )
        rebuildFileTree()
    }

    fun setIPTypeDropdownExpanded(expanded: Boolean) {
        _state.value = _state.value.copy(ipTypeDropdownExpanded = expanded)
    }

    fun selectIPType(ipTypeId: String, entityName: String, pluralName: String) {
        _state.value = _state.value.copy(
            selectedIPTypeId = ipTypeId,
            ipTypeDropdownExpanded = false,
            fileTree = GenerationFileTreeBuilder.buildForRepository(entityName, pluralName)
        )
    }

    fun selectUIFile(filePath: String, fileName: String, moduleName: String) {
        _state.value = _state.value.copy(
            selectedUIFilePath = filePath,
            selectedUIFileName = fileName,
            fileTree = GenerationFileTreeBuilder.buildForUIFBP(moduleName)
        )
    }

    fun updateFlowGraphName(name: String) {
        val needsRebuild = _state.value.flowGraphName != name || _state.value.fileTree.folders.isEmpty()
        _state.value = _state.value.copy(flowGraphName = name)
        if (needsRebuild && _state.value.selectedPath == GenerationPath.GENERATE_MODULE) {
            rebuildFileTree()
        }
    }

    fun toggleFolder(folderName: String) {
        _state.value = _state.value.copy(
            fileTree = _state.value.fileTree.toggleFolder(folderName)
        )
    }

    fun toggleFile(folderName: String, fileName: String) {
        _state.value = _state.value.copy(
            fileTree = _state.value.fileTree.toggleFile(folderName, fileName)
        )
    }

    private fun rebuildFileTree() {
        val s = _state.value
        val tree = when (s.selectedPath) {
            GenerationPath.GENERATE_MODULE -> GenerationFileTreeBuilder.buildForGenerateModule(s.flowGraphName)
            GenerationPath.REPOSITORY -> GenerationFileTree.empty()
            GenerationPath.UI_FBP -> GenerationFileTree.empty()
        }
        _state.value = s.copy(fileTree = tree)
    }
}
