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
    fun `InterfaceGenerator produces the post-085 universal set with both source and sink ports`() {
        // 7 mandatory entries (with bootstrap .flow.kt off):
        //   State, ViewModel, SourceCodeNode, SinkCodeNode, ControllerInterface, Runtime, PreviewProvider
        val result = UIFBPInterfaceGenerator().generateAll(demoSpec)
        assertTrue(result.success)
        assertEquals(7, result.filesGenerated.size)
    }

    @Test
    fun `InterfaceGenerator produces 8 entries when includeFlowKt is true`() {
        val result = UIFBPInterfaceGenerator().generateAll(demoSpec, includeFlowKt = true)
        assertTrue(result.success)
        assertEquals(8, result.filesGenerated.size)
        assertTrue(result.filesGenerated.any { it.relativePath.endsWith("DemoUI.flow.kt") })
    }

    @Test
    fun `InterfaceGenerator emits all post-085 file paths with flow-graph prefix`() {
        val result = UIFBPInterfaceGenerator().generateAll(demoSpec, includeFlowKt = true)
        val paths = result.filesGenerated.map { it.relativePath }
        // Post-085 placement (per data-model.md §2 file table)
        assertTrue(paths.any { it == "src/commonMain/kotlin/io/codenode/demo/viewmodel/DemoUIState.kt" })
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
        assertTrue(runtimeFile.content.contains("override val results = DemoUIState.resultsFlow"),
            "anonymous object MUST override sink-input flows pointing at State")
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
        // ControllerInterface still emitted with sink-input flows
        val ctrlFile = result.filesGenerated.first { it.relativePath.endsWith("ControllerInterface.kt") }
        assertTrue(ctrlFile.content.contains("interface DemoUIControllerInterface : ModuleController"))
        assertTrue(ctrlFile.content.contains("val results: StateFlow<CalculationResults?>"))
        // ViewModel emit method with no parameters
        val vmFile = result.filesGenerated.first { it.relativePath.endsWith("ViewModel.kt") }
        assertTrue(!vmFile.content.contains("fun emit("),
            "ViewModel MUST omit emit(...) when there are no source outputs")
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
    fun `InterfaceGenerator omits Source when no source outputs (mandatory entries drop to 6)`() {
        // 7 - 1 (skipped Source) = 6 mandatory entries
        val noSourceSpec = demoSpec.copy(sourceOutputs = emptyList())
        val result = UIFBPInterfaceGenerator().generateAll(noSourceSpec)
        assertEquals(6, result.filesGenerated.size)
        assertTrue(result.filesGenerated.none { it.relativePath.contains("SourceCodeNode") })
    }

    // T019 (feature 084): End-to-end integration test (post-085 shape)
    @Test
    fun `end-to-end generated files contain expected structure for DemoUI`() {
        val result = UIFBPInterfaceGenerator().generateAll(demoSpec)
        assertTrue(result.success)

        val stateFile = result.filesGenerated.first { it.relativePath.endsWith("DemoUIState.kt") }
        assertTrue(stateFile.content.contains("object DemoUIState"))
        assertTrue(stateFile.content.contains("_numA"))
        assertTrue(stateFile.content.contains("_numB"))
        assertTrue(stateFile.content.contains("_results"))
        assertTrue(stateFile.content.contains("fun reset()"))

        val vmFile = result.filesGenerated.first { it.relativePath.endsWith("DemoUIViewModel.kt") }
        assertTrue(vmFile.content.contains("class DemoUIViewModel("))
        assertTrue(vmFile.content.contains("controller: DemoUIControllerInterface"))
        assertTrue(vmFile.content.contains("fun emit(numA: Double, numB: Double)"))
        assertTrue(vmFile.content.contains("val results: StateFlow<CalculationResults?>"))

        val sourceFile = result.filesGenerated.first { it.relativePath.endsWith("DemoUISourceCodeNode.kt") }
        assertTrue(sourceFile.content.contains("object DemoUISourceCodeNode : CodeNodeDefinition"))
        assertTrue(sourceFile.content.contains("CodeNodeType.SOURCE"))
        assertTrue(sourceFile.content.contains("PortSpec(\"numA\", Double::class)"))
        assertTrue(sourceFile.content.contains("PortSpec(\"numB\", Double::class)"))

        val sinkFile = result.filesGenerated.first { it.relativePath.endsWith("DemoUISinkCodeNode.kt") }
        assertTrue(sinkFile.content.contains("object DemoUISinkCodeNode : CodeNodeDefinition"))
        assertTrue(sinkFile.content.contains("CodeNodeType.SINK"))
        assertTrue(sinkFile.content.contains("PortSpec(\"results\", CalculationResults::class)"))
    }
}
