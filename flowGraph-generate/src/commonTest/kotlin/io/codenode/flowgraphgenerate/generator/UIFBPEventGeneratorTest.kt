/*
 * UIFBPEventGeneratorTest — feature 087 / FR-002.
 *
 * Pins the new sealed-interface emission: one case per spec.sourceOutput,
 * Update<PortName>(value) for valued ports, data object <PortName> for
 * Unit-typed ports.
 *
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.generator

import io.codenode.flowgraphgenerate.parser.PortInfo
import io.codenode.flowgraphgenerate.parser.UIFBPSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UIFBPEventGeneratorTest {

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

    // Case 1: one case per sourceOutput
    @Test
    fun `generate emits sealed interface with one case per sourceOutput`() {
        val output = UIFBPEventGenerator().generate(demoSpec)
        assertTrue(output.contains("sealed interface DemoUIEvent"))
        assertTrue(output.contains("data class UpdateNumA(val value: Double)"))
        assertTrue(output.contains("data class UpdateNumB(val value: Double)"))
    }

    // Case 2: Update{PortName} for valued ports
    @Test
    fun `generate uses Update{PortName} for valued ports`() {
        val spec = demoSpec.copy(sourceOutputs = listOf(PortInfo("clickCount", "Int")))
        val output = UIFBPEventGenerator().generate(spec)
        assertTrue(output.contains("data class UpdateClickCount(val value: Int)"),
            "valued source ports → data class Update<PortName>(val value: T)")
    }

    // Case 3: data object {PortName} for Unit-typed ports
    @Test
    fun `generate uses data object {PortName} for Unit-typed ports`() {
        val spec = demoSpec.copy(sourceOutputs = listOf(
            PortInfo("toggleLike", "Unit"),
            PortInfo("goBack", "Unit")
        ), ipTypeImports = emptyList())
        val output = UIFBPEventGenerator().generate(spec)
        assertTrue(output.contains("data object ToggleLike : DemoUIEvent"))
        assertTrue(output.contains("data object GoBack : DemoUIEvent"))
        assertFalse(output.contains("UpdateToggleLike"),
            "Unit-typed ports MUST NOT use the Update prefix")
    }

    // Case 4: empty sourceOutputs → empty sealed interface (no body)
    @Test
    fun `generate on zero sourceOutputs emits an empty sealed interface`() {
        val spec = demoSpec.copy(sourceOutputs = emptyList(), ipTypeImports = emptyList())
        val output = UIFBPEventGenerator().generate(spec)
        assertTrue(output.contains("sealed interface DemoUIEvent"),
            "FR-008: empty-events case still emits the sealed interface")
        // No nested cases — the body is empty (or just braces).
        assertFalse(output.contains("data class"))
        assertFalse(output.contains("data object"))
    }

    // Case 5: package + class name
    @Test
    fun `generate uses correct package and flowGraphPrefix-derived class name`() {
        val divergent = demoSpec.copy(flowGraphPrefix = "AltPrefix")
        val output = UIFBPEventGenerator().generate(divergent)
        assertTrue(output.contains("package io.codenode.demo.viewmodel"))
        assertTrue(output.contains("sealed interface AltPrefixEvent"))
    }

    // Case 6: IP-type imports only when at least one valued port references one
    @Test
    fun `generate emits IP-type imports only when at least one valued port references one`() {
        // No source ports referencing the IP type → no import
        val output1 = UIFBPEventGenerator().generate(demoSpec)
        assertFalse(output1.contains("import io.codenode.demo.iptypes.CalculationResults"),
            "demoSpec sourceOutputs are Doubles only; no IP-type import needed")

        // Add a source port referencing the IP type → import emitted
        val spec = demoSpec.copy(sourceOutputs = listOf(
            PortInfo("payload", "CalculationResults", isNullable = true)
        ))
        val output2 = UIFBPEventGenerator().generate(spec)
        assertTrue(output2.contains("import io.codenode.demo.iptypes.CalculationResults"),
            "when a valued source port references an IP type, the import is emitted")
    }

    // Case 7: determinism (SC-005)
    @Test
    fun `generate output is byte-identical across two consecutive calls`() {
        val a = UIFBPEventGenerator().generate(demoSpec)
        val b = UIFBPEventGenerator().generate(demoSpec)
        assertEquals(a, b)
    }

    // Case 8: nullable source-port types correctly handled
    @Test
    fun `generate handles nullable source-output port types correctly`() {
        val spec = demoSpec.copy(sourceOutputs = listOf(
            PortInfo("payload", "CalculationResults", isNullable = true)
        ))
        val output = UIFBPEventGenerator().generate(spec)
        assertTrue(output.contains("data class UpdatePayload(val value: CalculationResults?)"),
            "nullable source-port types preserve the `?` in the Event case's value type")
    }
}
