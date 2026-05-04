/*
 * UIFBPSourceCodeNodeGeneratorTest — feature 087 (T005 split from UIFBPGeneratorTest).
 *
 * Pre-feature-087 baseline: assertions exercise today's createContinuousSource /
 * createSourceOut2 emission shape. T010 RED replaces these with the
 * `object + withSources(vararg)` Design B shape per
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

class UIFBPSourceCodeNodeGeneratorTest {

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
}
