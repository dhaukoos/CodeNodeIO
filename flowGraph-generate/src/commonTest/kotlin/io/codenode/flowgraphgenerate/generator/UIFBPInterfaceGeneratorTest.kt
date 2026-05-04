/*
 * UIFBPInterfaceGeneratorTest — feature 087 (T005 split from UIFBPGeneratorTest).
 *
 * Pre-feature-087 baseline: integration-level assertions over the orchestrator
 * (UIFBPInterfaceGenerator.generateAll). T020 GREEN updates these for the
 * Design B 8-mandatory-file output (the existing 7 + the new {Name}Event.kt;
 * PreviewProvider's body is unchanged from feature 084).
 *
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.generator

import io.codenode.flowgraphgenerate.parser.PortInfo
import io.codenode.flowgraphgenerate.parser.UIFBPSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UIFBPInterfaceGeneratorTest {

    private val demoSpec = UIFBPSpec(
        flowGraphPrefix = "DemoUI",
        composableName = "DemoUI",
        viewModelTypeName = "DemoUIViewModel",
        packageName = "io.codenode.demo",
        sourceOutputs = listOf(
            PortInfo("numA", "Double"),
            PortInfo("numB", "Double")
        ),
        sinkInputs = listOf(
            PortInfo("results", "CalculationResults", isNullable = true)
        ),
        ipTypeImports = listOf("io.codenode.demo.iptypes.CalculationResults")
    )

    @Test
    fun `InterfaceGenerator produces the Design B universal set with both source and sink ports`() {
        // 8 mandatory entries (with bootstrap .flow.kt off):
        //   State, Event (NEW), ViewModel, SourceCodeNode, SinkCodeNode, ControllerInterface, Runtime, PreviewProvider
        val result = UIFBPInterfaceGenerator().generateAll(demoSpec)
        assertTrue(result.success)
        assertEquals(8, result.filesGenerated.size)
    }

    @Test
    fun `InterfaceGenerator produces 9 entries when includeFlowKt is true`() {
        val result = UIFBPInterfaceGenerator().generateAll(demoSpec, includeFlowKt = true)
        assertTrue(result.success)
        assertEquals(9, result.filesGenerated.size)
        assertTrue(result.filesGenerated.any { it.relativePath.endsWith("DemoUI.flow.kt") })
    }

    @Test
    fun `InterfaceGenerator emits all Design B file paths with flow-graph prefix`() {
        val result = UIFBPInterfaceGenerator().generateAll(demoSpec, includeFlowKt = true)
        val paths = result.filesGenerated.map { it.relativePath }
        assertTrue(paths.any { it == "src/commonMain/kotlin/io/codenode/demo/viewmodel/DemoUIState.kt" })
        assertTrue(paths.any { it == "src/commonMain/kotlin/io/codenode/demo/viewmodel/DemoUIEvent.kt" },
            "Design B: new {Name}Event.kt is part of the mandatory output")
        assertTrue(paths.any { it == "src/commonMain/kotlin/io/codenode/demo/viewmodel/DemoUIViewModel.kt" })
        assertTrue(paths.any { it == "src/commonMain/kotlin/io/codenode/demo/nodes/DemoUISourceCodeNode.kt" })
        assertTrue(paths.any { it == "src/commonMain/kotlin/io/codenode/demo/nodes/DemoUISinkCodeNode.kt" })
        assertTrue(paths.any { it == "src/commonMain/kotlin/io/codenode/demo/controller/DemoUIControllerInterface.kt" })
        assertTrue(paths.any { it == "src/commonMain/kotlin/io/codenode/demo/controller/DemoUIRuntime.kt" })
        assertTrue(paths.any { it == "src/jvmMain/kotlin/io/codenode/demo/userInterface/DemoUIPreviewProvider.kt" })
        assertTrue(paths.any { it == "src/commonMain/kotlin/io/codenode/demo/flow/DemoUI.flow.kt" })
    }

    @Test
    fun `ControllerInterface entry extends ModuleController and declares typed sink-input flows`() {
        val result = UIFBPInterfaceGenerator().generateAll(demoSpec)
        val ctrlFile = result.filesGenerated.first { it.relativePath.endsWith("DemoUIControllerInterface.kt") }
        // Fingerprint: post-085 universal-runtime contract
        assertTrue(ctrlFile.content.contains("interface DemoUIControllerInterface : ModuleController"),
            "interface MUST extend ModuleController so the inherited control surface is available")
        assertTrue(ctrlFile.content.contains("import io.codenode.fbpdsl.runtime.ModuleController"))
        assertTrue(ctrlFile.content.contains("val results: StateFlow<CalculationResults?>"),
            "sink-input flows are typed members of the interface")
        assertTrue(ctrlFile.content.contains("package io.codenode.demo.controller"))
    }

    @Test
    fun `Runtime entry contains DynamicPipelineController and createDemoUIRuntime factory`() {
        val result = UIFBPInterfaceGenerator().generateAll(demoSpec)
        val runtimeFile = result.filesGenerated.first { it.relativePath.endsWith("DemoUIRuntime.kt") }
        // Fingerprints: factory function + DynamicPipelineController + ModuleController-by-controller delegation
        assertTrue(runtimeFile.content.contains("fun createDemoUIRuntime(flowGraph: FlowGraph): DemoUIControllerInterface"),
            "factory signature MUST be createDemoUIRuntime(flowGraph): {FlowGraph}ControllerInterface")
        assertTrue(runtimeFile.content.contains("DynamicPipelineController("),
            "factory body MUST construct a DynamicPipelineController")
        assertTrue(runtimeFile.content.contains("ModuleController by controller"),
            "anonymous object MUST delegate ModuleController to the underlying controller")
        // Design B: per-flow-graph state lives in the factory closure; sink flows
        // are constructor-local MutableStateFlows asStateFlow()'d in the override.
        assertTrue(runtimeFile.content.contains("override val results: StateFlow<CalculationResults?> = _results.asStateFlow()"),
            "anonymous object MUST override sink-input flows from per-flow-graph MutableStateFlows")
        assertTrue(runtimeFile.content.contains("package io.codenode.demo.controller"))
    }

    @Test
    fun `PreviewProvider entry uses flow-graph prefix as registry key and composable name as function call`() {
        // Decoupled-name fixture: divergent flowGraphPrefix vs composableName.
        val divergent = demoSpec.copy(
            flowGraphPrefix = "AltPrefix",
            composableName = "DemoUI",
            viewModelTypeName = "AltPrefixViewModel"
        )
        val result = UIFBPInterfaceGenerator().generateAll(divergent)
        val previewFile = result.filesGenerated.first { it.relativePath.endsWith("AltPrefixPreviewProvider.kt") }
        assertTrue(previewFile.relativePath.startsWith("src/jvmMain/kotlin/"),
            "PreviewProvider MUST live in jvmMain (preview-api is desktop-only)")
        assertTrue(previewFile.content.contains("PreviewRegistry.register(\"AltPrefix\")"),
            "registry key MUST equal flowGraphPrefix (matches RuntimePreviewPanel lookup)")
        assertTrue(previewFile.content.contains("DemoUI(viewModel = vm, modifier = modifier)"),
            "lambda body MUST invoke the user-authored Composable function name")
    }

    // T015 (e): zero source-output ports degenerate case
    @Test
    fun `degenerate spec with zero source outputs still produces a structurally-complete output`() {
        val noSourceSpec = demoSpec.copy(sourceOutputs = emptyList())
        val result = UIFBPInterfaceGenerator().generateAll(noSourceSpec)
        assertTrue(result.success)

        // Source CodeNode is skipped
        assertTrue(result.filesGenerated.none { it.relativePath.contains("SourceCodeNode") })
        // ControllerInterface still emitted with sink-input flows + no emit methods (Design B)
        val ctrlFile = result.filesGenerated.first { it.relativePath.endsWith("ControllerInterface.kt") }
        assertTrue(ctrlFile.content.contains("interface DemoUIControllerInterface : ModuleController"))
        assertTrue(ctrlFile.content.contains("val results: StateFlow<CalculationResults?>"))
        assertTrue(!ctrlFile.content.contains("fun emit"),
            "Design B: no source outputs → no emit methods on ControllerInterface")
        // ViewModel onEvent dispatcher exists but the when block is empty (FR-008)
        val vmFile = result.filesGenerated.first { it.relativePath.endsWith("ViewModel.kt") }
        assertTrue(!vmFile.content.contains("fun emit("),
            "Design B: prior emit(...) aggregate is gone")
        assertTrue(vmFile.content.contains("fun onEvent(event: DemoUIEvent)"),
            "Design B: onEvent dispatcher always emitted (FR-008)")
        // Event sealed interface still emitted (empty body)
        val eventFile = result.filesGenerated.first { it.relativePath.endsWith("DemoUIEvent.kt") }
        assertTrue(eventFile.content.contains("sealed interface DemoUIEvent"))
    }

    // T015 (f): zero sink-input ports degenerate case
    @Test
    fun `degenerate spec with zero sink inputs still produces a structurally-complete output`() {
        val noSinkSpec = demoSpec.copy(sinkInputs = emptyList())
        val result = UIFBPInterfaceGenerator().generateAll(noSinkSpec)
        assertTrue(result.success)

        // Sink CodeNode is skipped
        assertTrue(result.filesGenerated.none { it.relativePath.contains("SinkCodeNode") })
        // ControllerInterface still emitted, degenerate to inherited-only ModuleController surface
        val ctrlFile = result.filesGenerated.first { it.relativePath.endsWith("ControllerInterface.kt") }
        assertTrue(ctrlFile.content.contains("interface DemoUIControllerInterface : ModuleController"))
        // No `val y: StateFlow<...>` members beyond the inherited surface
        assertTrue(!ctrlFile.content.contains("val results"),
            "with zero sink inputs the interface MUST NOT declare sink-input flow members")
    }

    @Test
    fun `InterfaceGenerator omits Source when no source outputs (mandatory entries drop to 7)`() {
        // 8 - 1 (skipped Source) = 7 mandatory entries
        val noSourceSpec = demoSpec.copy(sourceOutputs = emptyList())
        val result = UIFBPInterfaceGenerator().generateAll(noSourceSpec)
        assertEquals(7, result.filesGenerated.size)
        assertTrue(result.filesGenerated.none { it.relativePath.contains("SourceCodeNode") })
    }

    // End-to-end integration test (Design B shape)
    @Test
    fun `end-to-end generated files contain expected Design B structure for DemoUI`() {
        val result = UIFBPInterfaceGenerator().generateAll(demoSpec)
        assertTrue(result.success)

        val stateFile = result.filesGenerated.first { it.relativePath.endsWith("DemoUIState.kt") }
        assertTrue(stateFile.content.contains("data class DemoUIState("),
            "Design B: State is an immutable data class")
        assertTrue(stateFile.content.contains("val results: CalculationResults? = null"))

        val eventFile = result.filesGenerated.first { it.relativePath.endsWith("DemoUIEvent.kt") }
        assertTrue(eventFile.content.contains("sealed interface DemoUIEvent"))
        assertTrue(eventFile.content.contains("data class UpdateNumA(val value: Double)"))
        assertTrue(eventFile.content.contains("data class UpdateNumB(val value: Double)"))

        val vmFile = result.filesGenerated.first { it.relativePath.endsWith("DemoUIViewModel.kt") }
        assertTrue(vmFile.content.contains("class DemoUIViewModel("))
        assertTrue(vmFile.content.contains("controller: DemoUIControllerInterface"))
        assertTrue(vmFile.content.contains("val state: StateFlow<DemoUIState>"))
        assertTrue(vmFile.content.contains("fun onEvent(event: DemoUIEvent)"))
        assertTrue(vmFile.content.contains("controller.emitNumA(event.value)"))

        val ctrlFile = result.filesGenerated.first { it.relativePath.endsWith("DemoUIControllerInterface.kt") }
        assertTrue(ctrlFile.content.contains("fun emitNumA(value: Double)"))
        assertTrue(ctrlFile.content.contains("fun emitNumB(value: Double)"))

        val runtimeFile = result.filesGenerated.first { it.relativePath.endsWith("DemoUIRuntime.kt") }
        assertTrue(runtimeFile.content.contains("DemoUISinkCodeNode.withReporters("))
        assertTrue(runtimeFile.content.contains("DemoUISourceCodeNode.withSources(_numA, _numB)"))

        val sourceFile = result.filesGenerated.first { it.relativePath.endsWith("DemoUISourceCodeNode.kt") }
        assertTrue(sourceFile.content.contains("object DemoUISourceCodeNode : CodeNodeDefinition"))
        assertTrue(sourceFile.content.contains("fun withSources(vararg sources: SharedFlow<*>)"))
        assertTrue(sourceFile.content.contains("PortSpec(\"numA\", Double::class)"))

        val sinkFile = result.filesGenerated.first { it.relativePath.endsWith("DemoUISinkCodeNode.kt") }
        assertTrue(sinkFile.content.contains("object DemoUISinkCodeNode : CodeNodeDefinition"))
        assertTrue(sinkFile.content.contains("fun withReporters(vararg reporters: (Any?) -> Unit)"))
        assertTrue(sinkFile.content.contains("PortSpec(\"results\", CalculationResults::class)"))
    }
}
