/*
 * UIFBPSinkCodeNodeGeneratorTest — feature 087 / Decision 8.
 *
 * Pins the new shape: object {Name}SinkCodeNode with default no-op
 * createRuntime + a withReporters(vararg) wrapper that returns a delegated
 * CodeNodeDefinition whose createRuntime captures per-flow-graph reporters.
 *
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.generator

import io.codenode.flowgraphgenerate.parser.PortInfo
import io.codenode.flowgraphgenerate.parser.UIFBPSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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

    // Case 1: object + default no-op createRuntime
    @Test
    fun `generate emits object SinkCodeNode with default no-op createRuntime`() {
        val output = UIFBPSinkCodeNodeGenerator().generate(demoSpec)
        assertNotNull(output)
        assertTrue(output.contains("object DemoUISinkCodeNode : CodeNodeDefinition"))
        assertTrue(output.contains("override fun createRuntime(name: String): NodeRuntime"))
        // Default runtime emits a no-op reporter — useful for palette drops outside a UI-FBP context.
    }

    // Case 2: withReporters(vararg) extension
    @Test
    fun `generate emits withReporters vararg extension method`() {
        val output = UIFBPSinkCodeNodeGenerator().generate(demoSpec)!!
        assertTrue(output.contains("fun withReporters(vararg reporters: (Any?) -> Unit): CodeNodeDefinition"),
            "Decision 8: per-flow-graph wrapper takes vararg reporters (one per sinkInput, declared order)")
    }

    // Case 3: wrapper delegates createRuntime with reporters captured
    @Test
    fun `withReporters wrapper createRuntime delegates to internal sink runtime with reporters`() {
        val output = UIFBPSinkCodeNodeGenerator().generate(demoSpec)!!
        // The wrapper's body should construct a delegated CodeNodeDefinition that captures reporters
        // and overrides createRuntime to invoke the helper with the captured callbacks.
        assertTrue(output.contains("object : CodeNodeDefinition by this"),
            "wrapper uses Kotlin interface delegation `by this` to preserve identity")
        assertTrue(output.contains("override fun createRuntime(name: String): NodeRuntime"),
            "the wrapper's createRuntime override captures the per-flow-graph reporters")
    }

    // Case 4: single-port sink
    @Test
    fun `generate handles single-port sink correctly`() {
        val output = UIFBPSinkCodeNodeGenerator().generate(demoSpec)!!
        assertTrue(output.contains("PortSpec(\"results\", CalculationResults::class)"))
        assertTrue(output.contains("override val outputPorts = emptyList<PortSpec>()"))
    }

    // Case 5: multi-port sink
    @Test
    fun `generate handles multi-port sink correctly with multiple reporters in declared order`() {
        val twoInput = demoSpec.copy(sinkInputs = listOf(
            PortInfo("result", "String"),
            PortInfo("error", "String")
        ), ipTypeImports = emptyList())
        val output = UIFBPSinkCodeNodeGenerator().generate(twoInput)!!
        assertTrue(output.contains("PortSpec(\"result\", String::class)"))
        assertTrue(output.contains("PortSpec(\"error\", String::class)"))
        // The internal helper threads multiple reporters into the runtime.
        assertTrue(output.contains("reporters[0]"),
            "multi-port sinks index reporters by sinkInput position")
        assertTrue(output.contains("reporters[1]"))
    }

    // Case 6: determinism (SC-005)
    @Test
    fun `generate output is byte-identical across two consecutive calls`() {
        val a = UIFBPSinkCodeNodeGenerator().generate(demoSpec)
        val b = UIFBPSinkCodeNodeGenerator().generate(demoSpec)
        assertEquals(a, b)
    }

    // Case 7: NO singleton State / StateStore reference
    @Test
    fun `generate does NOT reference any singleton State or StateStore`() {
        val output = UIFBPSinkCodeNodeGenerator().generate(demoSpec)!!
        assertFalse(output.contains("DemoUIState._"),
            "Design B: SinkCodeNode delivers via reporter callback, not singleton write")
        assertFalse(output.contains("StateStore"))
        assertFalse(output.contains("import io.codenode.demo.viewmodel.DemoUIState"),
            "Design B: SinkCodeNode no longer needs to import the (gone) singleton")
    }
}
