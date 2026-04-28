/*
 * CodeGeneratorViewModel - State management for the Code Generator panel
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.flowgraphgenerate.model.GenerationFileTree
import io.codenode.flowgraphgenerate.model.GenerationFileTreeBuilder
import io.codenode.flowgraphgenerate.model.GenerationPath
import io.codenode.flowgraphgenerate.nodes.GenerationConfig
import io.codenode.flowgraphgenerate.runner.CodeGenerationRunner
import io.codenode.flowgraphgenerate.runner.GenerationFileWriter
import io.codenode.flowgraphgenerate.runner.GenerationResult
import io.codenode.flowgraphgenerate.runner.SelectionFilter
import io.codenode.flowgraphgenerate.save.ModuleScaffoldingGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class CodeGeneratorPanelState(
    val isExpanded: Boolean = false,
    val selectedPath: GenerationPath = GenerationPath.GENERATE_MODULE,
    val pathDropdownExpanded: Boolean = false,
    val selectedIPTypeId: String? = null,
    val ipTypeDropdownExpanded: Boolean = false,
    val selectedUIFilePath: String? = null,
    val selectedUIFileName: String? = null,
    val selectedFlowGraphFilePath: String? = null,
    val selectedFlowGraphFileName: String? = null,
    val selectedFlowGraph: FlowGraph? = null,
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
            selectedFlowGraphFilePath = null,
            selectedFlowGraphFileName = null,
            selectedFlowGraph = null,
            fileTree = GenerationFileTree.empty()
        )
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

    /**
     * Records a user-selected `.flow.kt` file as the source of truth for the
     * GENERATE_MODULE path's generation. The parsed FlowGraph drives every
     * generator (its `name` becomes the module name, its nodes populate the
     * registry, etc.) and the panel's "Files to Generate" tree is rebuilt
     * from the FlowGraph's name.
     */
    fun selectFlowGraphFile(filePath: String, fileName: String, flowGraph: FlowGraph) {
        val moduleName = flowGraph.name.ifBlank { fileName.removeSuffix(".flow.kt").removeSuffix(".kt") }
        _state.value = _state.value.copy(
            selectedFlowGraphFilePath = filePath,
            selectedFlowGraphFileName = fileName,
            selectedFlowGraph = flowGraph,
            flowGraphName = moduleName,
            fileTree = GenerationFileTreeBuilder.buildForGenerateModule(moduleName)
        )
    }

    /**
     * Updates the displayed flowGraph name. Kept as a pure setter so that
     * incidental updates from the canvas (e.g., on every recomposition) don't
     * silently rebuild the GENERATE_MODULE tree — which now requires explicit
     * file selection via [selectFlowGraphFile].
     */
    fun updateFlowGraphName(name: String) {
        if (_state.value.flowGraphName != name) {
            _state.value = _state.value.copy(flowGraphName = name)
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

    private val runner = CodeGenerationRunner()
    private val scaffoldingGenerator = ModuleScaffoldingGenerator()
    private val fileWriter = GenerationFileWriter()

    suspend fun generate(
        outputDir: File,
        flowGraph: FlowGraph,
        targetPlatforms: List<FlowGraph.TargetPlatform> = emptyList()
    ): GenerationResult {
        val s = _state.value
        // GENERATE_MODULE path now requires the user to pick a `.flow.kt` file
        // explicitly; that parsed graph drives generation. Other paths still use
        // the canvas's flowGraph passed in by the caller.
        val effectiveFlowGraph = if (s.selectedPath == GenerationPath.GENERATE_MODULE) {
            s.selectedFlowGraph ?: return GenerationResult(
                generatedFiles = emptyMap(),
                errors = mapOf("CodeGeneratorViewModel" to "No FlowGraph file selected")
            )
        } else {
            flowGraph
        }
        val moduleName = effectiveFlowGraph.name.ifBlank { s.flowGraphName }

        val scaffold = scaffoldingGenerator.generate(
            moduleName = moduleName,
            outputDir = outputDir,
            targetPlatforms = targetPlatforms
        )

        val config = GenerationConfig(
            flowGraph = effectiveFlowGraph,
            basePackage = scaffold.basePackage,
            flowPackage = scaffold.flowPackage,
            controllerPackage = scaffold.controllerPackage,
            viewModelPackage = scaffold.viewModelPackage,
            userInterfacePackage = scaffold.userInterfacePackage,
            moduleName = moduleName
        )

        val filter = SelectionFilter.fromFileTree(s.fileTree)
        val result = runner.execute(s.selectedPath, config, filter)

        fileWriter.write(result, scaffold.moduleDir, scaffold.basePackage, moduleName)

        return result
    }
}
