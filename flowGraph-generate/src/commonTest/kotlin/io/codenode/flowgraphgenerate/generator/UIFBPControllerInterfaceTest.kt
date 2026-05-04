/*
 * UIFBPControllerInterfaceTest — feature 087 / FR-007 (additive).
 *
 * Pins the additive emit<SourcePort>(value) methods on the generated
 * {Name}ControllerInterface. Existing per-sink-port StateFlow members and the
 * inherited ModuleController surface are preserved unchanged.
 *
 * Tests exercise the now-internal generateControllerInterface entry point on
 * UIFBPInterfaceGenerator (visibility opened by T018 GREEN as part of the
 * Design B rewrite).
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

class UIFBPControllerInterfaceTest {

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

    private fun generate(spec: UIFBPSpec): String =
        UIFBPInterfaceGenerator().generateControllerInterface(spec, "${spec.packageName}.controller")

    // Case 1: existing per-sink-port StateFlow members preserved (FR-007)
    @Test
    fun `interface preserves per-sink-port StateFlow members from feature 084 baseline`() {
        val output = generate(demoSpec)
        assertTrue(output.contains("interface DemoUIControllerInterface : ModuleController"))
        assertTrue(output.contains("val results: StateFlow<CalculationResults?>"),
            "FR-007: per-sink-port StateFlow<T> members preserved unchanged")
        assertTrue(output.contains("import io.codenode.fbpdsl.runtime.ModuleController"))
        assertTrue(output.contains("import kotlinx.coroutines.flow.StateFlow"))
    }

    // Case 2: additive emit<Port>(value: T) per sourceOutput
    @Test
    fun `interface adds one emit Port value method per sourceOutput`() {
        val output = generate(demoSpec)
        assertTrue(output.contains("fun emitNumA(value: Double)"),
            "FR-007 additive: one emit method per source-output port")
        assertTrue(output.contains("fun emitNumB(value: Double)"))
    }

    // Case 3: additive emit<Port>() (no-arg) for Unit-typed source ports
    @Test
    fun `interface adds emit Port no-arg for Unit-typed source ports`() {
        val spec = demoSpec.copy(sourceOutputs = listOf(
            PortInfo("toggleLike", "Unit"),
            PortInfo("goBack", "Unit")
        ), ipTypeImports = emptyList())
        val output = generate(spec)
        assertTrue(output.contains("fun emitToggleLike()"))
        assertTrue(output.contains("fun emitGoBack()"))
    }

    // Case 4: empty sourceOutputs → no emit methods
    @Test
    fun `interface emits no emit methods when sourceOutputs is empty`() {
        val spec = demoSpec.copy(sourceOutputs = emptyList())
        val output = generate(spec)
        assertTrue(output.contains("interface DemoUIControllerInterface : ModuleController"))
        assertFalse(output.contains("fun emit"),
            "no source ports → no emit methods")
    }

    // Case 5: package + imports
    @Test
    fun `interface declared in correct package with required imports`() {
        val output = generate(demoSpec)
        assertTrue(output.contains("package io.codenode.demo.controller"))
        // IP type import only when a sink-input or source-output references one.
        assertTrue(output.contains("import io.codenode.demo.iptypes.CalculationResults"),
            "demoSpec's results sink references the IP type → import expected")
    }

    // Case 6: determinism (SC-005)
    @Test
    fun `interface output is byte-identical across two consecutive calls`() {
        val a = generate(demoSpec)
        val b = generate(demoSpec)
        assertEquals(a, b)
    }
}
