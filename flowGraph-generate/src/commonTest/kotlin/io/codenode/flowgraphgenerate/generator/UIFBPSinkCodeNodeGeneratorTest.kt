/*
 * UIFBPSinkCodeNodeGeneratorTest — feature 087 (T005 split from UIFBPGeneratorTest).
 *
 * Pre-feature-087 baseline: assertions exercise today's createContinuousSink /
 * createSinkIn2 emission shape. T009 RED replaces these with the
 * `object + withReporters(vararg)` Design B shape per
 * contracts/source-sink-controller-runtime.md. The split is mechanical.
 *
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.generator

import io.codenode.flowgraphgenerate.parser.PortInfo
import io.codenode.flowgraphgenerate.parser.UIFBPSpec
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UIFBPSinkCodeNodeGeneratorTest {

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
}
