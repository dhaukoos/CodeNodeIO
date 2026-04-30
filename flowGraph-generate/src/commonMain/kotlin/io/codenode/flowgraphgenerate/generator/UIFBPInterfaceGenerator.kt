/*
 * UIFBPInterfaceGenerator — post-085 orchestrator for the UI-FBP code-generation path.
 *
 * Per feature 085's universal-runtime collapse, every module exposes the same surface:
 *   controller/{FlowGraph}ControllerInterface.kt extending ModuleController
 *   controller/{FlowGraph}Runtime.kt with create{FlowGraph}Runtime(flowGraph): {FlowGraph}ControllerInterface
 *   jvmMain/userInterface/{FlowGraph}PreviewProvider.kt registering with PreviewRegistry
 *
 * UI-FBP modules ride this same shape. This class emits the 7-or-8 entry artifact set
 * documented in specs/084-ui-fbp-runtime-preview/data-model.md §2 (Source/Sink CodeNodes
 * are skipped when their port set is empty per T015 (e)/(f)).
 *
 * Three-identifier model (post-082/083 + Decision 2):
 *   flowGraphPrefix — drives generated-file prefixes + PreviewRegistry key
 *   composableName — the user-authored function the PreviewProvider invokes
 *   packageName    — drives on-disk path translation
 *
 * The ControllerInterface and Runtime factory are emitted inline (rather than reusing
 * feature 085's RuntimeControllerInterfaceGenerator / ModuleRuntimeGenerator) so user
 * IP-type names from the typed UIFBPSpec survive into the emitted source — the universal
 * generators erase types to Any via KClass<*>.simpleName.
 *
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.generator

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.model.Node
import io.codenode.flowgraphgenerate.parser.PortInfo
import io.codenode.flowgraphgenerate.parser.UIFBPSpec

data class UIFBPGenerateResult(
    val success: Boolean,
    val filesGenerated: List<UIFBPGeneratedFile> = emptyList(),
    val errorMessage: String? = null
)

data class UIFBPGeneratedFile(
    val relativePath: String,
    val content: String
)

class UIFBPInterfaceGenerator {

    private val stateGenerator = UIFBPStateGenerator()
    private val viewModelGenerator = UIFBPViewModelGenerator()
    private val sourceGenerator = UIFBPSourceCodeNodeGenerator()
    private val sinkGenerator = UIFBPSinkCodeNodeGenerator()
    private val previewProviderGenerator = PreviewProviderGenerator()

    /**
     * Emits the post-085 universal artifact set for a UI-FBP module.
     *
     * @param spec The UI-FBP-derived spec (post-clarification: typed identifiers separate
     *             flow-graph prefix from user-authored composable name).
     * @param includeFlowKt When true, additionally emits a bootstrap `flow/{FlowGraph}.flow.kt`.
     *                      Default false; the merge case is the [UIFBPSaveService]'s job.
     * @return A [UIFBPGenerateResult] with 7 mandatory entries (or 8 with bootstrap), modulo
     *         optional Source/Sink CodeNodes which are skipped when their port set is empty.
     */
    fun generateAll(spec: UIFBPSpec, includeFlowKt: Boolean = false): UIFBPGenerateResult {
        return try {
            val files = mutableListOf<UIFBPGeneratedFile>()
            val basePath = spec.packageName.replace(".", "/")
            val flowGraphPrefix = spec.flowGraphPrefix
            val controllerPackage = "${spec.packageName}.controller"
            val viewModelPackage = "${spec.packageName}.viewmodel"

            // 1. State (viewmodel/{FlowGraph}State.kt)
            files.add(UIFBPGeneratedFile(
                relativePath = "src/commonMain/kotlin/$basePath/viewmodel/${flowGraphPrefix}State.kt",
                content = stateGenerator.generate(spec)
            ))

            // 2. ViewModel (viewmodel/{FlowGraph}ViewModel.kt)
            files.add(UIFBPGeneratedFile(
                relativePath = "src/commonMain/kotlin/$basePath/viewmodel/${flowGraphPrefix}ViewModel.kt",
                content = viewModelGenerator.generate(spec)
            ))

            // 3. Source CodeNode — skipped when empty (degenerate spec edge case T015 (e))
            val sourceContent = sourceGenerator.generate(spec)
            if (sourceContent != null) {
                files.add(UIFBPGeneratedFile(
                    relativePath = "src/commonMain/kotlin/$basePath/nodes/${flowGraphPrefix}SourceCodeNode.kt",
                    content = sourceContent
                ))
            }

            // 4. Sink CodeNode — skipped when empty (degenerate spec edge case T015 (f))
            val sinkContent = sinkGenerator.generate(spec)
            if (sinkContent != null) {
                files.add(UIFBPGeneratedFile(
                    relativePath = "src/commonMain/kotlin/$basePath/nodes/${flowGraphPrefix}SinkCodeNode.kt",
                    content = sinkContent
                ))
            }

            // 5. ControllerInterface (controller/{FlowGraph}ControllerInterface.kt) — extends ModuleController
            files.add(UIFBPGeneratedFile(
                relativePath = "src/commonMain/kotlin/$basePath/controller/${flowGraphPrefix}ControllerInterface.kt",
                content = generateControllerInterface(spec, controllerPackage)
            ))

            // 6. Runtime factory (controller/{FlowGraph}Runtime.kt) — replaces the trio (Flow/Controller/Adapter)
            files.add(UIFBPGeneratedFile(
                relativePath = "src/commonMain/kotlin/$basePath/controller/${flowGraphPrefix}Runtime.kt",
                content = generateRuntimeFactory(spec, controllerPackage, viewModelPackage)
            ))

            // 7. PreviewProvider (jvmMain/userInterface/{FlowGraph}PreviewProvider.kt)
            files.add(UIFBPGeneratedFile(
                relativePath = "src/jvmMain/kotlin/$basePath/userInterface/${flowGraphPrefix}PreviewProvider.kt",
                content = previewProviderGenerator.generate(
                    flowGraph = syntheticFlowGraph(spec),
                    basePackage = spec.packageName,
                    viewModelPackage = viewModelPackage,
                    composableName = spec.composableName
                )
            ))

            // 8. Bootstrap .flow.kt (optional)
            if (includeFlowKt) {
                files.add(UIFBPGeneratedFile(
                    relativePath = "src/commonMain/kotlin/$basePath/flow/${flowGraphPrefix}.flow.kt",
                    content = generateBootstrapFlowKt(spec)
                ))
            }

            UIFBPGenerateResult(success = true, filesGenerated = files)
        } catch (e: Exception) {
            UIFBPGenerateResult(success = false, errorMessage = "Generation failed: ${e.message}")
        }
    }

    /**
     * Builds a minimal synthetic FlowGraph from a UIFBPSpec — used to feed the post-085
     * generators that take a FlowGraph (PreviewProviderGenerator). Source/Sink ports are
     * untyped (Any::class) — UI-FBP-specific typed-ness flows through the typed UIFBPSpec
     * directly to the inline-emitted ControllerInterface and Runtime factory below.
     */
    private fun syntheticFlowGraph(spec: UIFBPSpec): FlowGraph {
        val nodes = mutableListOf<CodeNode>()
        val zeroPosition = Node.Position(0.0, 0.0)
        if (spec.sourceOutputs.isNotEmpty()) {
            nodes.add(CodeNode(
                id = "${spec.flowGraphPrefix}_source",
                name = "${spec.flowGraphPrefix}Source",
                codeNodeType = CodeNodeType.SOURCE,
                position = zeroPosition
            ))
        }
        if (spec.sinkInputs.isNotEmpty()) {
            nodes.add(CodeNode(
                id = "${spec.flowGraphPrefix}_sink",
                name = "${spec.flowGraphPrefix}Sink",
                codeNodeType = CodeNodeType.SINK,
                position = zeroPosition
            ))
        }
        return FlowGraph(
            id = spec.flowGraphPrefix.lowercase(),
            name = spec.flowGraphPrefix,
            version = "1.0.0",
            rootNodes = nodes,
            connections = emptyList()
        )
    }

    /**
     * Inline emission of `{FlowGraph}ControllerInterface.kt`. Produces the same shape
     * as feature 085's `RuntimeControllerInterfaceGenerator` (interface extends
     * ModuleController, one `val y: StateFlow<T>` per observable port), but driven from
     * the typed [UIFBPSpec] so user IP-type type names survive into the interface
     * (where `RuntimeControllerInterfaceGenerator` would erase them to Any via
     * `KClass<*>.simpleName`).
     */
    private fun generateControllerInterface(spec: UIFBPSpec, controllerPackage: String): String {
        val sb = StringBuilder()
        val interfaceName = "${spec.flowGraphPrefix}ControllerInterface"

        sb.appendLine("/*")
        sb.appendLine(" * $interfaceName")
        sb.appendLine(" * Generated by CodeNodeIO UIFBPInterfaceGenerator")
        sb.appendLine(" * License: Apache 2.0")
        sb.appendLine(" */")
        sb.appendLine()
        sb.appendLine("package $controllerPackage")
        sb.appendLine()
        sb.appendLine("import io.codenode.fbpdsl.runtime.ModuleController")
        sb.appendLine("import kotlinx.coroutines.flow.StateFlow")
        for (imp in spec.ipTypeImports) {
            sb.appendLine("import $imp")
        }
        sb.appendLine()
        sb.appendLine("/**")
        sb.appendLine(" * Typed control surface for ${spec.flowGraphPrefix}.")
        sb.appendLine(" *")
        sb.appendLine(" * Inherits start/stop/pause/resume/reset/getStatus/setAttenuationDelay/")
        sb.appendLine(" * setEmissionObserver/setValueObserver and executionState from [ModuleController].")
        sb.appendLine(" * Adds typed StateFlow members for each sink-input port the UI observes.")
        sb.appendLine(" */")
        sb.appendLine("interface $interfaceName : ModuleController {")
        for (port in spec.sinkInputs) {
            val typeStr = if (port.isNullable) "${port.typeName}?" else port.typeName
            sb.appendLine("    val ${port.name}: StateFlow<$typeStr>")
        }
        sb.appendLine("}")

        return sb.toString()
    }

    /**
     * Inline emission of `{FlowGraph}Runtime.kt` — the post-085 factory function
     * `create{FlowGraph}Runtime(flowGraph): {FlowGraph}ControllerInterface` returning an
     * anonymous `object : {FlowGraph}ControllerInterface, ModuleController by controller`
     * over a [DynamicPipelineController]. Mirrors `ModuleRuntimeGenerator`'s output shape.
     *
     * The lookup body resolves the synthetic Source/Sink CodeNode names UI-FBP emits.
     */
    private fun generateRuntimeFactory(
        spec: UIFBPSpec,
        controllerPackage: String,
        viewModelPackage: String
    ): String {
        val sb = StringBuilder()
        val name = spec.flowGraphPrefix
        val interfaceName = "${name}ControllerInterface"
        val stateName = "${name}State"
        val sourceObj = "${name}SourceCodeNode"
        val sinkObj = "${name}SinkCodeNode"

        sb.appendLine("/*")
        sb.appendLine(" * ${name}Runtime — Universal-runtime factory for the $name UI-FBP module")
        sb.appendLine(" * Generated by CodeNodeIO UIFBPInterfaceGenerator")
        sb.appendLine(" * License: Apache 2.0")
        sb.appendLine(" */")
        sb.appendLine()
        sb.appendLine("package $controllerPackage")
        sb.appendLine()
        sb.appendLine("import io.codenode.fbpdsl.model.FlowGraph")
        sb.appendLine("import io.codenode.fbpdsl.runtime.CodeNodeDefinition")
        sb.appendLine("import io.codenode.fbpdsl.runtime.DynamicPipelineController")
        sb.appendLine("import io.codenode.fbpdsl.runtime.ModuleController")
        sb.appendLine("import $viewModelPackage.$stateName")
        if (spec.sourceOutputs.isNotEmpty()) {
            sb.appendLine("import ${spec.packageName}.nodes.$sourceObj")
        }
        if (spec.sinkInputs.isNotEmpty()) {
            sb.appendLine("import ${spec.packageName}.nodes.$sinkObj")
        }
        sb.appendLine()
        sb.appendLine("/**")
        sb.appendLine(" * Module-local lookup for resolving node names to compiled CodeNodeDefinitions.")
        sb.appendLine(" * Used by [DynamicPipelineController] when running this module without the GraphEditor.")
        sb.appendLine(" */")
        sb.appendLine("object ${name}NodeRegistry {")
        sb.appendLine("    fun lookup(nodeName: String): CodeNodeDefinition? = when (nodeName) {")
        if (spec.sourceOutputs.isNotEmpty()) {
            sb.appendLine("        \"${name}Source\" -> $sourceObj")
        }
        if (spec.sinkInputs.isNotEmpty()) {
            sb.appendLine("        \"${name}Sink\" -> $sinkObj")
        }
        sb.appendLine("        else -> null")
        sb.appendLine("    }")
        sb.appendLine("}")
        sb.appendLine()
        sb.appendLine("/**")
        sb.appendLine(" * Constructs a $interfaceName backed by [DynamicPipelineController].")
        sb.appendLine(" * Production-app consumers call this directly. The returned object delegates")
        sb.appendLine(" * every [ModuleController] member to the underlying controller and reads typed")
        sb.appendLine(" * state flows directly from $stateName.")
        sb.appendLine(" */")
        sb.appendLine("fun create${name}Runtime(flowGraph: FlowGraph): $interfaceName {")
        sb.appendLine("    val controller = DynamicPipelineController(")
        sb.appendLine("        flowGraphProvider = { flowGraph },")
        sb.appendLine("        lookup = ${name}NodeRegistry::lookup,")
        sb.appendLine("        onReset = $stateName::reset")
        sb.appendLine("    )")
        sb.appendLine("    return object : $interfaceName, ModuleController by controller {")
        for (port in spec.sinkInputs) {
            sb.appendLine("        override val ${port.name} = $stateName.${port.name}Flow")
        }
        sb.appendLine("    }")
        sb.appendLine("}")

        return sb.toString()
    }

    /**
     * Bootstrap `.flow.kt` emission. Unchanged from pre-082/083 except that the file is
     * now placed in `flow/` (matches feature-085 conventions) and the prefix is taken
     * from `flowGraphPrefix`.
     */
    private fun generateBootstrapFlowKt(spec: UIFBPSpec): String {
        val sb = StringBuilder()
        val name = spec.flowGraphPrefix
        val graphVarName = name.replaceFirstChar { it.lowercase() } + "FlowGraph"

        sb.appendLine("package ${spec.packageName}.flow")
        sb.appendLine()
        sb.appendLine("import io.codenode.fbpdsl.dsl.*")
        sb.appendLine("import io.codenode.fbpdsl.model.*")
        sb.appendLine()
        sb.appendLine("val $graphVarName = flowGraph(\"$name\", version = \"1.0.0\") {")

        if (spec.sourceOutputs.isNotEmpty()) {
            val sourceName = "${name}Source"
            sb.appendLine("    val source = codeNode(\"$sourceName\", nodeType = \"SOURCE\") {")
            sb.appendLine("        position(100.0, 300.0)")
            for (port in spec.sourceOutputs) {
                sb.appendLine("        output(\"${port.name}\", Any::class)")
            }
            sb.appendLine("    }")
        }

        if (spec.sinkInputs.isNotEmpty()) {
            sb.appendLine()
            val sinkName = "${name}Sink"
            sb.appendLine("    val sink = codeNode(\"$sinkName\", nodeType = \"SINK\") {")
            sb.appendLine("        position(600.0, 300.0)")
            for (port in spec.sinkInputs) {
                sb.appendLine("        input(\"${port.name}\", Any::class)")
            }
            sb.appendLine("    }")
        }

        sb.appendLine("}")

        return sb.toString()
    }

    @Suppress("unused") // kept for source compatibility; callers pass PortInfo directly
    private fun PortInfo.kotlinType(): String =
        if (isNullable) "$typeName?" else typeName
}
