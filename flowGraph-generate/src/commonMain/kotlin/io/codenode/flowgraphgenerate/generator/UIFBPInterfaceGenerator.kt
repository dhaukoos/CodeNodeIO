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
    private val eventGenerator = UIFBPEventGenerator()
    private val viewModelGenerator = UIFBPViewModelGenerator()
    private val sourceGenerator = UIFBPSourceCodeNodeGenerator()
    private val sinkGenerator = UIFBPSinkCodeNodeGenerator()
    private val previewProviderGenerator = PreviewProviderGenerator()

    /**
     * Emits the Design B universal artifact set for a UI-FBP module.
     *
     * Mandatory output set (8 files when both source and sink ports are present):
     *   1. `viewmodel/{FlowGraph}State.kt` — public immutable data class (FR-001)
     *   2. `viewmodel/{FlowGraph}Event.kt` — public sealed interface (FR-002, NEW)
     *   3. `viewmodel/{FlowGraph}ViewModel.kt` — MVI shape (FR-003)
     *   4. `nodes/{FlowGraph}SourceCodeNode.kt` — object + withSources wrapper (Decision 8)
     *   5. `nodes/{FlowGraph}SinkCodeNode.kt` — object + withReporters wrapper (Decision 8)
     *   6. `controller/{FlowGraph}ControllerInterface.kt` — additive emit<Port> (FR-007)
     *   7. `controller/{FlowGraph}Runtime.kt` — per-flow-graph factory (Design B)
     *   8. `jvmMain/userInterface/{FlowGraph}PreviewProvider.kt` — body unchanged from feature 084
     *
     * Source / Sink CodeNodes are skipped when their port set is empty (degenerate-spec
     * edge cases preserved from feature 084).
     *
     * @param includeFlowKt When true, additionally emits a bootstrap `flow/{FlowGraph}.flow.kt`.
     *                      Default false; the merge case is the [UIFBPSaveService]'s job.
     */
    fun generateAll(spec: UIFBPSpec, includeFlowKt: Boolean = false): UIFBPGenerateResult {
        return try {
            val files = mutableListOf<UIFBPGeneratedFile>()
            val basePath = spec.packageName.replace(".", "/")
            val flowGraphPrefix = spec.flowGraphPrefix
            val controllerPackage = "${spec.packageName}.controller"
            val viewModelPackage = "${spec.packageName}.viewmodel"

            // 1. State (viewmodel/{FlowGraph}State.kt) — data class (Design B)
            files.add(UIFBPGeneratedFile(
                relativePath = "src/commonMain/kotlin/$basePath/viewmodel/${flowGraphPrefix}State.kt",
                content = stateGenerator.generate(spec)
            ))

            // 2. Event (viewmodel/{FlowGraph}Event.kt) — sealed interface (NEW, FR-002)
            files.add(UIFBPGeneratedFile(
                relativePath = "src/commonMain/kotlin/$basePath/viewmodel/${flowGraphPrefix}Event.kt",
                content = eventGenerator.generate(spec)
            ))

            // 3. ViewModel (viewmodel/{FlowGraph}ViewModel.kt) — MVI shape
            files.add(UIFBPGeneratedFile(
                relativePath = "src/commonMain/kotlin/$basePath/viewmodel/${flowGraphPrefix}ViewModel.kt",
                content = viewModelGenerator.generate(spec)
            ))

            // 4. Source CodeNode — skipped when empty
            val sourceContent = sourceGenerator.generate(spec)
            if (sourceContent != null) {
                files.add(UIFBPGeneratedFile(
                    relativePath = "src/commonMain/kotlin/$basePath/nodes/${flowGraphPrefix}SourceCodeNode.kt",
                    content = sourceContent
                ))
            }

            // 5. Sink CodeNode — skipped when empty
            val sinkContent = sinkGenerator.generate(spec)
            if (sinkContent != null) {
                files.add(UIFBPGeneratedFile(
                    relativePath = "src/commonMain/kotlin/$basePath/nodes/${flowGraphPrefix}SinkCodeNode.kt",
                    content = sinkContent
                ))
            }

            // 6. ControllerInterface — preserves existing surface + additive emit<Port> (FR-007 additive)
            files.add(UIFBPGeneratedFile(
                relativePath = "src/commonMain/kotlin/$basePath/controller/${flowGraphPrefix}ControllerInterface.kt",
                content = generateControllerInterface(spec, controllerPackage)
            ))

            // 7. Runtime factory — per-flow-graph closure state (Design B)
            files.add(UIFBPGeneratedFile(
                relativePath = "src/commonMain/kotlin/$basePath/controller/${flowGraphPrefix}Runtime.kt",
                content = generateRuntimeFactory(spec, controllerPackage, viewModelPackage)
            ))

            // 8. PreviewProvider — body unchanged from feature 084
            files.add(UIFBPGeneratedFile(
                relativePath = "src/jvmMain/kotlin/$basePath/userInterface/${flowGraphPrefix}PreviewProvider.kt",
                content = previewProviderGenerator.generate(
                    flowGraph = syntheticFlowGraph(spec),
                    basePackage = spec.packageName,
                    viewModelPackage = viewModelPackage,
                    composableName = spec.composableName
                )
            ))

            // Optional: bootstrap .flow.kt
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
     * Inline emission of `{FlowGraph}ControllerInterface.kt` (FR-007 additive).
     *
     * Preserves every member the feature-084 baseline emitted (per-sink-port
     * `StateFlow<T>` plus the inherited `ModuleController` surface), and adds
     * one `fun emit{PortName}(value: T)` per source-output port (or
     * `fun emit{PortName}()` for `Unit`-typed ports) per Decision 2 / Design B.
     *
     * Visibility is `internal` so the per-method test
     * (`UIFBPControllerInterfaceTest`) can call this entry point directly.
     */
    internal fun generateControllerInterface(spec: UIFBPSpec, controllerPackage: String): String {
        val sb = StringBuilder()
        val interfaceName = "${spec.flowGraphPrefix}ControllerInterface"

        sb.appendLine("/*")
        sb.appendLine(" * $interfaceName")
        sb.appendLine(" * Generated by CodeNodeIO UIFBPInterfaceGenerator (feature 087 / Design B additive)")
        sb.appendLine(" * License: Apache 2.0")
        sb.appendLine(" */")
        sb.appendLine()
        sb.appendLine("package $controllerPackage")
        sb.appendLine()
        sb.appendLine("import io.codenode.fbpdsl.runtime.ModuleController")
        sb.appendLine("import kotlinx.coroutines.flow.StateFlow")
        // IP-type imports — only when at least one sinkInput or VALUED sourceOutput needs them.
        val needed = (spec.sinkInputs.map { it.typeName } +
            spec.sourceOutputs.filter { it.typeName != "Unit" }.map { it.typeName }).toSet()
        for (imp in spec.ipTypeImports.filter { needed.contains(it.substringAfterLast('.')) }) {
            sb.appendLine("import $imp")
        }
        sb.appendLine()
        sb.appendLine("/**")
        sb.appendLine(" * Typed control surface for ${spec.flowGraphPrefix}.")
        sb.appendLine(" *")
        sb.appendLine(" * Inherits start/stop/pause/resume/reset/getStatus/setAttenuationDelay/")
        sb.appendLine(" * setEmissionObserver/setValueObserver and executionState from [ModuleController].")
        sb.appendLine(" * Adds typed StateFlow members for each sink-input port the UI observes")
        sb.appendLine(" * AND additive emit<Port>(value) methods for each source-output port the UI raises")
        sb.appendLine(" * (Design B / FR-007 additive).")
        sb.appendLine(" */")
        sb.appendLine("interface $interfaceName : ModuleController {")
        for (port in spec.sinkInputs) {
            val typeStr = if (port.isNullable) "${port.typeName}?" else port.typeName
            sb.appendLine("    val ${port.name}: StateFlow<$typeStr>")
        }
        for (port in spec.sourceOutputs) {
            val caseName = port.name.replaceFirstChar { it.uppercase() }
            if (port.typeName == "Unit") {
                sb.appendLine("    fun emit$caseName()")
            } else {
                val typeStr = if (port.isNullable) "${port.typeName}?" else port.typeName
                sb.appendLine("    fun emit$caseName(value: $typeStr)")
            }
        }
        sb.appendLine("}")

        return sb.toString()
    }

    /**
     * Inline emission of `{FlowGraph}Runtime.kt` — feature 087 / Design B.
     *
     * The factory `create{FlowGraph}Runtime(flowGraph): {FlowGraph}ControllerInterface`
     * is the per-flow-graph state owner: it constructs `MutableStateFlow<T>` per
     * sinkInput + `MutableSharedFlow<T>` per sourceOutput, wires them through
     * `withReporters(...)` / `withSources(...)` wrappers on the per-module
     * Source/Sink CodeNodes, and returns an anonymous object that overrides
     * every per-sink-port `StateFlow` and per-source-port `emit<Port>` method.
     *
     * Source emissions dispatch on `controller.coroutineScope` (added by T004
     * / fbpDsl) so they share the pipeline's exception handler + supervisor
     * lifetime. The factory signature is unchanged from feature 084, so
     * `ModuleSessionFactory` and Runtime Preview keep working without changes.
     *
     * Visibility is `internal` so the per-method test
     * (`UIFBPRuntimeFactoryTest`) can call this entry point directly.
     */
    internal fun generateRuntimeFactory(
        spec: UIFBPSpec,
        controllerPackage: String,
        viewModelPackage: String
    ): String {
        val sb = StringBuilder()
        val name = spec.flowGraphPrefix
        val interfaceName = "${name}ControllerInterface"
        val sourceObj = "${name}SourceCodeNode"
        val sinkObj = "${name}SinkCodeNode"

        sb.appendLine("/*")
        sb.appendLine(" * ${name}Runtime — per-flow-graph factory for the $name UI-FBP module.")
        sb.appendLine(" * Generated by CodeNodeIO UIFBPInterfaceGenerator (feature 087 / Design B)")
        sb.appendLine(" * License: Apache 2.0")
        sb.appendLine(" */")
        sb.appendLine()
        sb.appendLine("package $controllerPackage")
        sb.appendLine()
        sb.appendLine("import io.codenode.fbpdsl.model.FlowGraph")
        sb.appendLine("import io.codenode.fbpdsl.runtime.CodeNodeDefinition")
        sb.appendLine("import io.codenode.fbpdsl.runtime.DynamicPipelineController")
        sb.appendLine("import io.codenode.fbpdsl.runtime.ModuleController")
        sb.appendLine("import kotlinx.coroutines.flow.MutableSharedFlow")
        sb.appendLine("import kotlinx.coroutines.flow.MutableStateFlow")
        sb.appendLine("import kotlinx.coroutines.flow.StateFlow")
        sb.appendLine("import kotlinx.coroutines.flow.asStateFlow")
        sb.appendLine("import kotlinx.coroutines.launch")
        if (spec.sourceOutputs.isNotEmpty()) {
            sb.appendLine("import ${spec.packageName}.nodes.$sourceObj")
        }
        if (spec.sinkInputs.isNotEmpty()) {
            sb.appendLine("import ${spec.packageName}.nodes.$sinkObj")
        }
        // IP-type imports — needed for the typed MutableStateFlow / emit method bodies.
        val needed = (spec.sinkInputs.map { it.typeName } +
            spec.sourceOutputs.filter { it.typeName != "Unit" }.map { it.typeName }).toSet()
        for (imp in spec.ipTypeImports.filter { needed.contains(it.substringAfterLast('.')) }) {
            sb.appendLine("import $imp")
        }

        sb.appendLine()
        sb.appendLine("/**")
        sb.appendLine(" * Constructs a $interfaceName backed by [DynamicPipelineController].")
        sb.appendLine(" * Per-flow-graph state lives in this function's closure — multiple")
        sb.appendLine(" * concurrent calls produce fully isolated controllers.")
        sb.appendLine(" */")
        sb.appendLine("fun create${name}Runtime(flowGraph: FlowGraph): $interfaceName {")

        // Per-flow-graph MutableStateFlow per sinkInput.
        for (port in spec.sinkInputs) {
            val (typeStr, defaultExpr) = sinkShape(port)
            sb.appendLine("    val _${port.name} = MutableStateFlow<$typeStr>($defaultExpr)")
        }

        // Per-flow-graph MutableSharedFlow per sourceOutput.
        for (port in spec.sourceOutputs) {
            val typeStr = if (port.typeName == "Unit") "Unit"
                else if (port.isNullable) "${port.typeName}?"
                else port.typeName
            sb.appendLine("    val _${port.name} = MutableSharedFlow<$typeStr>(replay = 1, extraBufferCapacity = 64)")
        }

        // Build sinkWrapper / sourceWrapper.
        if (spec.sinkInputs.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("    val sinkWrapper = $sinkObj.withReporters(")
            for ((i, port) in spec.sinkInputs.withIndex()) {
                val (typeStr, _) = sinkShape(port)
                val comma = if (i < spec.sinkInputs.size - 1) "," else ""
                sb.appendLine("        { value -> _${port.name}.value = value as $typeStr }$comma")
            }
            sb.appendLine("    )")
        }
        if (spec.sourceOutputs.isNotEmpty()) {
            val sourceFlows = spec.sourceOutputs.joinToString(", ") { "_${it.name}" }
            sb.appendLine("    val sourceWrapper = $sourceObj.withSources($sourceFlows)")
        }

        // DynamicPipelineController — onReset resets every per-sink-port flow.
        sb.appendLine()
        sb.appendLine("    val controller = DynamicPipelineController(")
        sb.appendLine("        flowGraphProvider = { flowGraph },")
        sb.appendLine("        lookup = { nodeName -> when (nodeName) {")
        if (spec.sourceOutputs.isNotEmpty()) {
            sb.appendLine("            \"${name}Source\" -> sourceWrapper")
        }
        if (spec.sinkInputs.isNotEmpty()) {
            sb.appendLine("            \"${name}Sink\" -> sinkWrapper")
        }
        sb.appendLine("            else -> null")
        sb.appendLine("        } },")
        sb.appendLine("        onReset = {")
        for (port in spec.sinkInputs) {
            val (_, defaultExpr) = sinkShape(port)
            sb.appendLine("            _${port.name}.value = $defaultExpr")
        }
        sb.appendLine("        }")
        sb.appendLine("    )")

        // Returned anonymous object — sinkPort StateFlows + per-source emit methods.
        sb.appendLine()
        sb.appendLine("    return object : $interfaceName, ModuleController by controller {")
        for (port in spec.sinkInputs) {
            val (typeStr, _) = sinkShape(port)
            sb.appendLine("        override val ${port.name}: StateFlow<$typeStr> = _${port.name}.asStateFlow()")
        }
        for (port in spec.sourceOutputs) {
            val caseName = port.name.replaceFirstChar { it.uppercase() }
            if (port.typeName == "Unit") {
                sb.appendLine("        override fun emit$caseName() {")
                sb.appendLine("            controller.coroutineScope?.launch { _${port.name}.emit(Unit) }")
                sb.appendLine("        }")
            } else {
                val typeStr = if (port.isNullable) "${port.typeName}?" else port.typeName
                sb.appendLine("        override fun emit$caseName(value: $typeStr) {")
                sb.appendLine("            controller.coroutineScope?.launch { _${port.name}.emit(value) }")
                sb.appendLine("        }")
            }
        }
        sb.appendLine("    }")
        sb.appendLine("}")

        return sb.toString()
    }

    /** Returns `(propertyType, defaultExpression)` for a sink-input port (mirrors UIFBPStateGenerator). */
    private fun sinkShape(port: PortInfo): Pair<String, String> {
        if (port.isNullable) return "${port.typeName}?" to "null"
        return when (port.typeName) {
            "Int" -> "Int" to "0"
            "Long" -> "Long" to "0L"
            "Double" -> "Double" to "0.0"
            "Float" -> "Float" to "0.0f"
            "Boolean" -> "Boolean" to "false"
            "String" -> "String" to "\"\""
            "Unit" -> "Unit" to "Unit"
            else -> "${port.typeName}?" to "null"
        }
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
