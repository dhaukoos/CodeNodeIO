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
        moduleName = "DemoUI",
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

    // T009: State generator tests
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

    // T008: ViewModel generator tests
    @Test
    fun `ViewModelGenerator produces ViewModel extending ViewModel`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertTrue(output.contains("class DemoUIViewModel : ViewModel()"))
        assertTrue(output.contains("import androidx.lifecycle.ViewModel"))
    }

    @Test
    fun `ViewModelGenerator exposes StateFlow properties for Sink inputs`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertTrue(output.contains("val results: StateFlow<CalculationResults?>"))
        assertTrue(output.contains("DemoUIState.resultsFlow"))
    }

    @Test
    fun `ViewModelGenerator generates emit method for Source outputs`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertTrue(output.contains("fun emit(numA: Double, numB: Double)"))
        assertTrue(output.contains("DemoUIState._numA.value = numA"))
        assertTrue(output.contains("DemoUIState._numB.value = numB"))
    }

    @Test
    fun `ViewModelGenerator includes reset method`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertTrue(output.contains("fun reset()"))
        assertTrue(output.contains("DemoUIState.reset()"))
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

    // Orchestrator tests
    @Test
    fun `InterfaceGenerator produces all four files`() {
        val result = UIFBPInterfaceGenerator().generateAll(demoSpec)
        assertTrue(result.success)
        assertEquals(4, result.filesGenerated.size)
    }

    @Test
    fun `InterfaceGenerator uses correct file paths`() {
        val result = UIFBPInterfaceGenerator().generateAll(demoSpec)
        val paths = result.filesGenerated.map { it.relativePath }
        assertTrue(paths.any { it.endsWith("DemoUIState.kt") })
        assertTrue(paths.any { it.endsWith("DemoUIViewModel.kt") })
        assertTrue(paths.any { it.endsWith("DemoUISourceCodeNode.kt") })
        assertTrue(paths.any { it.endsWith("DemoUISinkCodeNode.kt") })
    }

    @Test
    fun `InterfaceGenerator omits Source when no source outputs`() {
        val noSourceSpec = demoSpec.copy(sourceOutputs = emptyList())
        val result = UIFBPInterfaceGenerator().generateAll(noSourceSpec)
        assertEquals(3, result.filesGenerated.size)
        assertTrue(result.filesGenerated.none { it.relativePath.contains("SourceCodeNode") })
    }

    // T019: End-to-end integration test
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
        assertTrue(vmFile.content.contains("class DemoUIViewModel : ViewModel()"))
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
