/*
 * UIFBPStateGeneratorTest — feature 087 (T005 split from UIFBPGeneratorTest).
 *
 * Pre-feature-087 baseline: assertions exercise today's singleton-object output
 * shape. T006 RED replaces these with data-class fixture-string comparisons
 * per contracts/state-generator.md. The split is mechanical — same assertions,
 * new file home — so the existing test count stays green at this point.
 *
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.generator

import io.codenode.flowgraphgenerate.parser.PortInfo
import io.codenode.flowgraphgenerate.parser.UIFBPSpec
import kotlin.test.Test
import kotlin.test.assertTrue

class UIFBPStateGeneratorTest {

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
}
