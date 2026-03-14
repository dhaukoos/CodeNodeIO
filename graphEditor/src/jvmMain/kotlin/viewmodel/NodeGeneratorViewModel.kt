/*
 * NodeGeneratorViewModel - ViewModel for the Node Generator Panel
 * Encapsulates state and business logic for creating custom node types
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import io.codenode.grapheditor.repository.CustomNodeDefinition
import io.codenode.grapheditor.repository.CustomNodeRepository
import io.codenode.grapheditor.state.NodeDefinitionRegistry
import io.codenode.fbpdsl.runtime.NodeCategory
import java.io.File

/**
 * Placement level for generated node files.
 */
enum class PlacementLevel(val displayName: String) {
    MODULE("Module"),
    PROJECT("Project"),
    UNIVERSAL("Universal")
}

/**
 * State data class for the Node Generator Panel.
 * Contains all UI state including form fields and dropdown expansion states.
 */
data class NodeGeneratorPanelState(
    val name: String = "",
    val inputCount: Int = 1,
    val outputCount: Int = 1,
    val isExpanded: Boolean = false,
    val inputDropdownExpanded: Boolean = false,
    val outputDropdownExpanded: Boolean = false,
    val anyInput: Boolean = false,
    val category: NodeCategory = NodeCategory.TRANSFORMER,
    val placementLevel: PlacementLevel = PlacementLevel.PROJECT,
    val categoryDropdownExpanded: Boolean = false,
    val levelDropdownExpanded: Boolean = false,
    val generationError: String? = null,
    val generationSuccess: String? = null
) : BaseState {
    /**
     * Computed property: form is valid when name is non-blank AND
     * at least one port exists (not both 0/0).
     */
    val isValid: Boolean
        get() = name.isNotBlank() && !(inputCount == 0 && outputCount == 0)

    /**
     * Computed property: whether the "Any Input" toggle should be visible.
     * Only meaningful with 2 or more inputs.
     */
    val showAnyInputToggle: Boolean
        get() = inputCount >= 2

    /**
     * Computed property: genericType string following the pattern "inXoutY"
     * or "inXanyoutY" when anyInput is enabled.
     */
    val genericType: String
        get() {
            val anyPrefix = if (anyInput) "any" else ""
            return "in${inputCount}${anyPrefix}out${outputCount}"
        }
}

/**
 * ViewModel for the Node Generator Panel.
 * Manages state and business logic for creating custom node types.
 *
 * Supports two creation modes:
 * 1. Legacy: Creates a CustomNodeDefinition (persisted to JSON repository)
 * 2. CodeNode: Generates a self-contained {NodeName}CodeNode.kt file
 *
 * @param customNodeRepository Repository for persisting custom node definitions
 * @param registry Optional registry for name conflict detection and CodeNode generation
 * @param projectRoot Optional project root directory for file path resolution
 */
class NodeGeneratorViewModel(
    private val customNodeRepository: CustomNodeRepository,
    var registry: NodeDefinitionRegistry? = null,
    var projectRoot: File? = null
) : ViewModel() {

    private val _state = MutableStateFlow(NodeGeneratorPanelState())
    val state: StateFlow<NodeGeneratorPanelState> = _state.asStateFlow()

    fun setName(name: String) {
        _state.update { it.copy(name = name, generationError = null, generationSuccess = null) }
    }

    fun setInputCount(count: Int) {
        val coerced = count.coerceIn(0, 3)
        _state.update {
            it.copy(
                inputCount = coerced,
                anyInput = if (coerced < 2) false else it.anyInput
            )
        }
    }

    fun setOutputCount(count: Int) {
        _state.update { it.copy(outputCount = count.coerceIn(0, 3)) }
    }

    fun toggleExpanded() {
        _state.update { it.copy(isExpanded = !it.isExpanded) }
    }

    fun setInputDropdownExpanded(expanded: Boolean) {
        _state.update { it.copy(inputDropdownExpanded = expanded) }
    }

    fun setOutputDropdownExpanded(expanded: Boolean) {
        _state.update { it.copy(outputDropdownExpanded = expanded) }
    }

    fun setAnyInput(anyInput: Boolean) {
        _state.update { it.copy(anyInput = anyInput) }
    }

    fun setCategory(category: NodeCategory) {
        _state.update { it.copy(category = category) }
    }

    fun setPlacementLevel(level: PlacementLevel) {
        _state.update { it.copy(placementLevel = level) }
    }

    fun setCategoryDropdownExpanded(expanded: Boolean) {
        _state.update { it.copy(categoryDropdownExpanded = expanded) }
    }

    fun setLevelDropdownExpanded(expanded: Boolean) {
        _state.update { it.copy(levelDropdownExpanded = expanded) }
    }

    /**
     * Creates a new custom node definition (legacy mode).
     * Adds the node to the repository and resets the form.
     *
     * @return The created CustomNodeDefinition, or null if state was invalid
     */
    fun createNode(): CustomNodeDefinition? {
        val currentState = _state.value
        if (!currentState.isValid) return null

        val node = CustomNodeDefinition.create(
            name = currentState.name.trim(),
            inputCount = currentState.inputCount,
            outputCount = currentState.outputCount,
            anyInput = currentState.anyInput
        )
        customNodeRepository.add(node)
        reset()
        return node
    }

    /**
     * Generates a self-contained CodeNode file at the selected placement level.
     * Checks for name conflicts before generating.
     *
     * @return The generated File, or null if generation failed (check state.generationError)
     */
    fun generateCodeNode(): File? {
        val currentState = _state.value
        if (!currentState.isValid) return null

        val nodeName = currentState.name.trim()

        // T014: Name conflict detection
        val reg = registry
        if (reg != null && reg.nameExists(nodeName)) {
            _state.update { it.copy(generationError = "A node named \"$nodeName\" already exists") }
            return null
        }

        // T013: Resolve file path for the selected level
        val outputFile = resolveOutputPath(nodeName, currentState.placementLevel)
        if (outputFile == null) {
            _state.update { it.copy(generationError = "Cannot resolve output path for ${currentState.placementLevel.displayName} level") }
            return null
        }

        if (outputFile.exists()) {
            _state.update { it.copy(generationError = "File already exists: ${outputFile.name}") }
            return null
        }

        // T012: Generate the file content
        val content = generateCodeNodeContent(
            nodeName = nodeName,
            category = currentState.category,
            inputCount = currentState.inputCount,
            outputCount = currentState.outputCount,
            packageName = resolvePackageName(currentState.placementLevel)
        )

        // Write the file
        try {
            outputFile.parentFile?.mkdirs()
            outputFile.writeText(content)
        } catch (e: Exception) {
            _state.update { it.copy(generationError = "Failed to write file: ${e.message}") }
            return null
        }

        _state.update { it.copy(
            generationSuccess = "Generated ${outputFile.name}",
            generationError = null
        ) }
        reset()
        return outputFile
    }

    /**
     * Resolves the output file path for a CodeNode at the given placement level.
     */
    fun resolveOutputPath(nodeName: String, level: PlacementLevel): File? {
        val fileName = "${nodeName}CodeNode.kt"
        return when (level) {
            PlacementLevel.MODULE -> {
                // Module level: place in the currently-loaded module's nodes directory
                // For now, defaults to EdgeArtFilter; future: driven by active module context
                val root = projectRoot ?: return null
                root.resolve("EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/nodes/$fileName")
            }
            PlacementLevel.PROJECT -> {
                val root = projectRoot ?: return null
                root.resolve("nodes/src/commonMain/kotlin/io/codenode/nodes/$fileName")
            }
            PlacementLevel.UNIVERSAL -> {
                val home = System.getProperty("user.home")
                File(home, ".codenode/nodes/$fileName")
            }
        }
    }

    /**
     * Resolves the package name for the given placement level.
     */
    private fun resolvePackageName(level: PlacementLevel): String {
        return when (level) {
            PlacementLevel.MODULE -> "io.codenode.nodes"
            PlacementLevel.PROJECT -> "io.codenode.nodes"
            PlacementLevel.UNIVERSAL -> "io.codenode.nodes"
        }
    }

    /**
     * Generates the Kotlin source code for a self-contained CodeNode file.
     */
    fun generateCodeNodeContent(
        nodeName: String,
        category: NodeCategory,
        inputCount: Int,
        outputCount: Int,
        packageName: String
    ): String {
        val inputPortsList = (1..inputCount).joinToString(", ") {
            "PortSpec(\"input$it\", Any::class)"
        }
        val outputPortsList = (1..outputCount).joinToString(", ") {
            "PortSpec(\"output$it\", Any::class)"
        }

        val inputPortsExpr = if (inputCount == 0) "emptyList()" else "listOf($inputPortsList)"
        val outputPortsExpr = if (outputCount == 0) "emptyList()" else "listOf($outputPortsList)"

        val (runtimeFactory, blockType, blockImpl) = generateRuntimeBlock(
            category, inputCount, outputCount
        )

        return buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import io.codenode.fbpdsl.model.CodeNodeFactory")
            appendLine("import io.codenode.fbpdsl.runtime.*")
            appendLine("import kotlin.reflect.KClass")
            appendLine()
            appendLine("object ${nodeName}CodeNode : CodeNodeDefinition {")
            appendLine("    override val name = \"$nodeName\"")
            appendLine("    override val category = NodeCategory.${category.name}")
            appendLine("    override val description = \"${category.name.lowercase().replaceFirstChar { it.uppercase() }} node with $inputCount input(s) and $outputCount output(s)\"")
            appendLine("    override val inputPorts = $inputPortsExpr")
            appendLine("    override val outputPorts = $outputPortsExpr")
            appendLine()
            appendLine("    // TODO: Replace with your processing logic")
            appendLine("    private val processBlock: $blockType = $blockImpl")
            appendLine()
            appendLine("    override fun createRuntime(name: String): NodeRuntime {")
            appendLine("        return $runtimeFactory")
            appendLine("    }")
            appendLine("}")
            appendLine()
        }
    }

    /**
     * Generates the runtime factory call, block type alias, and default block implementation
     * based on the node category and port counts.
     *
     * @return Triple of (factoryCall, blockTypeAlias, defaultBlockImpl)
     */
    private fun generateRuntimeBlock(
        category: NodeCategory,
        inputCount: Int,
        outputCount: Int
    ): Triple<String, String, String> {
        return when {
            // Source: 0 inputs, 1 output
            inputCount == 0 && outputCount == 1 -> Triple(
                "CodeNodeFactory.createContinuousSource<Any>(\n            name = name,\n            generate = processBlock\n        )",
                "ContinuousSourceBlock<Any>",
                "{ emit -> emit(Unit) }"
            )
            // Source: 0 inputs, 2 outputs
            inputCount == 0 && outputCount == 2 -> Triple(
                "CodeNodeFactory.createContinuousSourceOut2<Any, Any>(\n            name = name,\n            generate = processBlock\n        )",
                "SourceOut2Block<Any, Any>",
                "{ emit1, emit2 -> emit1(Unit); emit2(Unit) }"
            )
            // Source: 0 inputs, 3 outputs
            inputCount == 0 && outputCount == 3 -> Triple(
                "CodeNodeFactory.createContinuousSourceOut3<Any, Any, Any>(\n            name = name,\n            generate = processBlock\n        )",
                "SourceOut3Block<Any, Any, Any>",
                "{ emit1, emit2, emit3 -> emit1(Unit); emit2(Unit); emit3(Unit) }"
            )
            // Transformer: 1 input, 1 output
            inputCount == 1 && outputCount == 1 -> Triple(
                "CodeNodeFactory.createContinuousTransformer<Any, Any>(\n            name = name,\n            transform = processBlock\n        )",
                "ContinuousTransformBlock<Any, Any>",
                "{ input -> input }"
            )
            // Sink: 1 input, 0 outputs
            inputCount == 1 && outputCount == 0 -> Triple(
                "CodeNodeFactory.createContinuousSink<Any>(\n            name = name,\n            consume = processBlock\n        )",
                "ContinuousSinkBlock<Any>",
                "{ input -> /* consume */ }"
            )
            // Sink: 2 inputs, 0 outputs
            inputCount == 2 && outputCount == 0 -> Triple(
                "CodeNodeFactory.createContinuousIn2Sink<Any, Any>(\n            name = name,\n            consume = processBlock\n        )",
                "ContinuousIn2SinkBlock<Any, Any>",
                "{ input1, input2 -> /* consume */ }"
            )
            // Sink: 3 inputs, 0 outputs
            inputCount == 3 && outputCount == 0 -> Triple(
                "CodeNodeFactory.createContinuousIn3Sink<Any, Any, Any>(\n            name = name,\n            consume = processBlock\n        )",
                "ContinuousIn3SinkBlock<Any, Any, Any>",
                "{ input1, input2, input3 -> /* consume */ }"
            )
            // Processor: 2 inputs, 1 output
            inputCount == 2 && outputCount == 1 -> Triple(
                "CodeNodeFactory.createIn2Out1Processor<Any, Any, Any>(\n            name = name,\n            process = processBlock\n        )",
                "In2Out1ProcessBlock<Any, Any, Any>",
                "{ input1, input2 -> input1 }"
            )
            // Processor: 3 inputs, 1 output
            inputCount == 3 && outputCount == 1 -> Triple(
                "CodeNodeFactory.createIn3Out1Processor<Any, Any, Any, Any>(\n            name = name,\n            process = processBlock\n        )",
                "In3Out1ProcessBlock<Any, Any, Any, Any>",
                "{ input1, input2, input3 -> input1 }"
            )
            // Generic fallback for other combinations
            else -> Triple(
                "CodeNodeFactory.createContinuousTransformer<Any, Any>(\n            name = name,\n            transform = processBlock\n        )",
                "ContinuousTransformBlock<Any, Any>",
                "{ input -> input }"
            )
        }
    }

    /**
     * Resets the form to its default state.
     * Preserves the expanded state of the panel.
     */
    fun reset() {
        _state.update {
            NodeGeneratorPanelState(isExpanded = it.isExpanded)
        }
    }
}
