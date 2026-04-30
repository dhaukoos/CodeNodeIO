/*
 * UIFBPGeneratorTest - Tests for all four UI-FBP generators
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.generator

import io.codenode.flowgraphgenerate.parser.PortInfo
import io.codenode.flowgraphgenerate.parser.UIFBPSpec
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UIFBPGeneratorTest {

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

    // T009: State generator tests — post-082/083 emits to viewmodel/ subpackage with flow-graph prefix
    @Test
    fun `StateGenerator emits to viewmodel subpackage`() {
        val output = UIFBPStateGenerator().generate(demoSpec)
        assertTrue(output.contains("package io.codenode.demo.viewmodel"),
            "post-085: State lives at {basePackage}.viewmodel.{FlowGraph}State (matches " +
                "ModuleSessionFactory's preferred FQCN lookup order)")
    }

    @Test
    fun `StateGenerator uses flowGraphPrefix not moduleName for class name`() {
        val divergentSpec = demoSpec.copy(flowGraphPrefix = "AltPrefix")
        val output = UIFBPStateGenerator().generate(divergentSpec)
        assertTrue(output.contains("object AltPrefixState"),
            "the State class name MUST come from flowGraphPrefix (not from composableName " +
                "which equals 'DemoUI' in this fixture)")
    }

    @Test
    fun `StateGenerator produces State object with MutableStateFlow pairs`() {
        val output = UIFBPStateGenerator().generate(demoSpec)
        assertTrue(output.contains("object DemoUIState"))
        assertTrue(output.contains("internal val _numA = MutableStateFlow"))
        assertTrue(output.contains("val numAFlow: StateFlow<Double>"))
        assertTrue(output.contains("internal val _numB = MutableStateFlow"))
        assertTrue(output.contains("val numBFlow: StateFlow<Double>"))
        assertTrue(output.contains("internal val _results = MutableStateFlow"))
        assertTrue(output.contains("val resultsFlow: StateFlow<CalculationResults?>"))
    }

    @Test
    fun `StateGenerator includes reset function`() {
        val output = UIFBPStateGenerator().generate(demoSpec)
        assertTrue(output.contains("fun reset()"))
        assertTrue(output.contains("_numA.value = 0.0"))
        assertTrue(output.contains("_results.value = null"))
    }

    @Test
    fun `StateGenerator includes IP type imports`() {
        val output = UIFBPStateGenerator().generate(demoSpec)
        assertTrue(output.contains("import io.codenode.demo.iptypes.CalculationResults"))
    }

    // T007: ViewModel generator tests — post-085 ViewModel takes ControllerInterface
    @Test
    fun `ViewModelGenerator emits to viewmodel subpackage with flowGraphPrefix-derived name`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertTrue(output.contains("package io.codenode.demo.viewmodel"),
            "ViewModel lives at {basePackage}.viewmodel matching ModuleSessionFactory's lookup")
    }

    @Test
    fun `ViewModelGenerator constructor takes typed ControllerInterface and extends ViewModel`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        // The generated ViewModel must accept the typed ControllerInterface so
        // ModuleSessionFactory.tryCreateViewModel's reflection match succeeds.
        assertTrue(
            output.contains("class DemoUIViewModel(") &&
                output.contains("private val controller: DemoUIControllerInterface") &&
                output.contains(") : ViewModel()"),
            "post-085: ViewModel constructor MUST be (private val controller: " +
                "DemoUIControllerInterface) and the class MUST extend ViewModel"
        )
        assertTrue(output.contains("import androidx.lifecycle.ViewModel"))
        assertTrue(output.contains("import io.codenode.demo.controller.DemoUIControllerInterface"),
            "the ControllerInterface lives in {basePackage}.controller")
    }

    @Test
    fun `ViewModelGenerator exposes StateFlow properties read directly from State`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertTrue(output.contains("val results: StateFlow<CalculationResults?>"))
        assertTrue(output.contains("DemoUIState.resultsFlow"),
            "flows are read directly from {FlowGraph}State (matches WeatherForecast/Addresses precedent)")
    }

    @Test
    fun `ViewModelGenerator exposes executionState from the controller`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertTrue(output.contains("val executionState"),
            "post-085 ViewModel re-exposes executionState from the inherited ModuleController surface")
        assertTrue(output.contains("controller.executionState"),
            "executionState comes from the ControllerInterface (inherits from ModuleController)")
    }

    @Test
    fun `ViewModelGenerator generates emit method writing to State mutable fields`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertTrue(output.contains("fun emit(numA: Double, numB: Double)"))
        assertTrue(output.contains("DemoUIState._numA.value = numA"))
        assertTrue(output.contains("DemoUIState._numB.value = numB"))
    }

    @Test
    fun `ViewModelGenerator emits forwarding control methods delegating to controller`() {
        // Per data-model.md §5 + tasks T007/T008: the UI calls viewModel.start() etc. directly.
        // These methods are NOT inherited from androidx.lifecycle.ViewModel; the generator must
        // emit them as one-line delegations.
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertTrue(output.contains("fun start(): FlowGraph = controller.start()"),
            "must forward start() to controller.start()")
        assertTrue(output.contains("fun stop(): FlowGraph = controller.stop()"),
            "must forward stop() to controller.stop()")
        assertTrue(output.contains("fun pause(): FlowGraph = controller.pause()"),
            "must forward pause() to controller.pause()")
        assertTrue(output.contains("fun resume(): FlowGraph = controller.resume()"),
            "must forward resume() to controller.resume()")
        assertTrue(output.contains("fun reset(): FlowGraph = controller.reset()"),
            "must forward reset() to controller.reset()")
    }

    // T010: Source CodeNode generator tests
    @Test
    fun `SourceGenerator produces Source CodeNode with correct category`() {
        val output = UIFBPSourceCodeNodeGenerator().generate(demoSpec)
        assertNotNull(output)
        assertTrue(output.contains("object DemoUISourceCodeNode : CodeNodeDefinition"))
        assertTrue(output.contains("override val category = CodeNodeType.SOURCE"))
    }

    @Test
    fun `SourceGenerator produces correct output ports`() {
        val output = UIFBPSourceCodeNodeGenerator().generate(demoSpec)!!
        assertTrue(output.contains("PortSpec(\"numA\", Double::class)"))
        assertTrue(output.contains("PortSpec(\"numB\", Double::class)"))
        assertTrue(output.contains("override val inputPorts = emptyList<PortSpec>()"))
    }

    @Test
    fun `SourceGenerator uses createSourceOut2 for two outputs`() {
        val output = UIFBPSourceCodeNodeGenerator().generate(demoSpec)!!
        assertTrue(output.contains("createSourceOut2<Double, Double>"))
    }

    @Test
    fun `SourceGenerator returns null when no source outputs`() {
        val noSourceSpec = demoSpec.copy(sourceOutputs = emptyList())
        val output = UIFBPSourceCodeNodeGenerator().generate(noSourceSpec)
        assertNull(output)
    }

    @Test
    fun `SourceGenerator uses createContinuousSource for single output`() {
        val singleSpec = demoSpec.copy(sourceOutputs = listOf(PortInfo("value", "Int")))
        val output = UIFBPSourceCodeNodeGenerator().generate(singleSpec)!!
        assertTrue(output.contains("createContinuousSource<Int>"))
    }

    // T011: Sink CodeNode generator tests
    @Test
    fun `SinkGenerator produces Sink CodeNode with correct category`() {
        val output = UIFBPSinkCodeNodeGenerator().generate(demoSpec)
        assertNotNull(output)
        assertTrue(output.contains("object DemoUISinkCodeNode : CodeNodeDefinition"))
        assertTrue(output.contains("override val category = CodeNodeType.SINK"))
    }

    @Test
    fun `SinkGenerator produces correct input ports`() {
        val output = UIFBPSinkCodeNodeGenerator().generate(demoSpec)!!
        assertTrue(output.contains("PortSpec(\"results\", CalculationResults::class)"))
        assertTrue(output.contains("override val outputPorts = emptyList<PortSpec>()"))
    }

    @Test
    fun `SinkGenerator uses createContinuousSink for single input`() {
        val output = UIFBPSinkCodeNodeGenerator().generate(demoSpec)!!
        assertTrue(output.contains("createContinuousSink<CalculationResults>"))
    }

    @Test
    fun `SinkGenerator returns null when no sink inputs`() {
        val noSinkSpec = demoSpec.copy(sinkInputs = emptyList())
        val output = UIFBPSinkCodeNodeGenerator().generate(noSinkSpec)
        assertNull(output)
    }

    @Test
    fun `SinkGenerator uses createSinkIn2 for two inputs`() {
        val twoInputSpec = demoSpec.copy(sinkInputs = listOf(
            PortInfo("result", "String"),
            PortInfo("error", "String")
        ))
        val output = UIFBPSinkCodeNodeGenerator().generate(twoInputSpec)!!
        assertTrue(output.contains("createSinkIn2<String, String>"))
    }

    @Test
    fun `SinkGenerator includes IP type import for custom types`() {
        val output = UIFBPSinkCodeNodeGenerator().generate(demoSpec)!!
        assertTrue(output.contains("import io.codenode.demo.iptypes.CalculationResults"))
    }

    // ========== T015: Orchestrator (post-085 universal set) ==========

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

    // ========== Backward-compat orchestrator paths ==========

    @Test
    fun `InterfaceGenerator omits Source when no source outputs (mandatory entries drop to 6)`() {
        // 7 - 1 (skipped Source) = 6 mandatory entries
        val noSourceSpec = demoSpec.copy(sourceOutputs = emptyList())
        val result = UIFBPInterfaceGenerator().generateAll(noSourceSpec)
        assertEquals(6, result.filesGenerated.size)
        assertTrue(result.filesGenerated.none { it.relativePath.contains("SourceCodeNode") })
    }

    // T019: End-to-end integration test (post-085 shape)
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
