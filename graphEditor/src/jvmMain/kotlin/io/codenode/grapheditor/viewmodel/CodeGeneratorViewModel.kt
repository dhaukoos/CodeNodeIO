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
import io.codenode.flowgraphgenerate.parser.UIComposableParser
import io.codenode.flowgraphgenerate.save.ModuleScaffoldingGenerator
import io.codenode.flowgraphgenerate.save.UIFBPSaveOptions
import io.codenode.flowgraphgenerate.save.UIFBPSaveResult
import io.codenode.flowgraphgenerate.save.UIFBPSaveService
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
        // Post-082/083: the file tree's prefix is the flow-graph prefix, not the UI file's
        // name. For the UI_FBP path we rebuild the tree only when the user picks the
        // .flow.kt selector via [selectFlowGraphFile]; the UI file selection just records
        // its path/name (used downstream by UIFBPSaveService to parse the source).
        _state.value = _state.value.copy(
            selectedUIFilePath = filePath,
            selectedUIFileName = fileName,
            fileTree = if (_state.value.selectedPath == GenerationPath.UI_FBP) {
                _state.value.fileTree // tree already built by selectFlowGraphFile
            } else {
                GenerationFileTreeBuilder.buildForUIFBP(moduleName)
            }
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
        val rebuiltTree = when (_state.value.selectedPath) {
            GenerationPath.UI_FBP -> GenerationFileTreeBuilder.buildForUIFBP(moduleName)
            else -> GenerationFileTreeBuilder.buildForGenerateModule(moduleName)
        }
        _state.value = _state.value.copy(
            selectedFlowGraphFilePath = filePath,
            selectedFlowGraphFileName = fileName,
            selectedFlowGraph = flowGraph,
            flowGraphName = moduleName,
            fileTree = rebuiltTree
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
    private val uifbpComposableParser = UIComposableParser()
    private val uifbpSaveService = UIFBPSaveService()

    /**
     * Runs UI-FBP code generation against the user-selected `{flow graph, UI file}` pair.
     *
     * Per FR-014/FR-015 (post-clarification): both inputs are explicit selections held in
     * panel state. This entry-point bypasses [CodeGenerationRunner] (which doesn't model
     * the UI-FBP-specific `UIFBPSpec` shape) and calls [UIFBPSaveService] directly.
     *
     * @param moduleRoot The host module's directory (must contain `build.gradle.kts`).
     * @return [UIFBPSaveResult] describing the per-file outcome and structured `.flow.kt`
     *         merge report. Caller (the GraphEditor's status line) reads `success`,
     *         `errorMessage`, and the per-file kinds.
     */
    fun generateUIFBP(moduleRoot: File): UIFBPSaveResult {
        val s = _state.value
        val flowGraphFilePath = s.selectedFlowGraphFilePath
        val uiFilePath = s.selectedUIFilePath
        if (flowGraphFilePath == null || uiFilePath == null) {
            return UIFBPSaveResult(
                success = false,
                errorMessage = "UI-FBP requires an explicit {flow graph, UI file} pair. " +
                    "Select both inputs via the file selectors before clicking Generate."
            )
        }
        val flowGraphFile = File(flowGraphFilePath)
        val uiFile = File(uiFilePath)
        if (!flowGraphFile.exists() || !uiFile.exists()) {
            return UIFBPSaveResult(
                success = false,
                errorMessage = "Selected file(s) no longer exist on disk: " +
                    listOfNotNull(
                        if (!flowGraphFile.exists()) flowGraphFile.absolutePath else null,
                        if (!uiFile.exists()) uiFile.absolutePath else null
                    ).joinToString(", ")
            )
        }
        val flowGraphPrefix = flowGraphFile.name.removeSuffix(".flow.kt").removeSuffix(".kt")
        val parseResult = uifbpComposableParser.parse(uiFile.readText(), flowGraphPrefix = flowGraphPrefix)
        if (!parseResult.isSuccess || parseResult.spec == null) {
            return UIFBPSaveResult(
                success = false,
                errorMessage = "Selected UI file is not a qualifying UI-FBP source: " +
                    (parseResult.errorMessage ?: "no Composable function with a viewModel parameter")
            )
        }
        return uifbpSaveService.save(
            spec = parseResult.spec!!,
            flowGraphFile = flowGraphFile,
            moduleRoot = moduleRoot,
            options = UIFBPSaveOptions()
        )
    }

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
