/*
 * UIFBPSourceCodeNodeGeneratorTest — feature 087 / Decision 8.
 *
 * Pins the new shape: object {Name}SourceCodeNode with default never-emit
 * createRuntime + a withSources(vararg) wrapper that returns a delegated
 * CodeNodeDefinition whose createRuntime captures per-flow-graph
 * SharedFlow<*> sources and forwards each emission to the matching output port.
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

    // Case 1: object + default never-emit createRuntime
    @Test
    fun `generate emits object SourceCodeNode with default never-emitting createRuntime`() {
        val output = UIFBPSourceCodeNodeGenerator().generate(demoSpec)
        assertNotNull(output)
        assertTrue(output.contains("object DemoUISourceCodeNode : CodeNodeDefinition"))
        assertTrue(output.contains("override fun createRuntime(name: String): NodeRuntime"))
    }

    // Case 2: withSources(vararg SharedFlow) extension
    @Test
    fun `generate emits withSources vararg SharedFlow extension method`() {
        val output = UIFBPSourceCodeNodeGenerator().generate(demoSpec)!!
        assertTrue(output.contains("fun withSources(vararg sources: SharedFlow<*>): CodeNodeDefinition"),
            "Decision 8: per-flow-graph wrapper takes vararg sources (one per sourceOutput, declared order)")
        assertTrue(output.contains("import kotlinx.coroutines.flow.SharedFlow"))
    }

    // Case 3: wrapper delegates createRuntime that launches collectors per source
    @Test
    fun `withSources wrapper createRuntime launches collectors per source flow`() {
        val output = UIFBPSourceCodeNodeGenerator().generate(demoSpec)!!
        assertTrue(output.contains("object : CodeNodeDefinition by this"),
            "wrapper uses Kotlin interface delegation `by this` to preserve identity")
        // The wrapper runtime collects from each source flow and forwards to its output port.
        assertTrue(output.contains("sources[0]"),
            "wrapper indexes sources by sourceOutput position")
        assertTrue(output.contains("sources[1]"))
    }

    // Case 4: single-port source
    @Test
    fun `generate handles single-port source correctly`() {
        val singlePort = demoSpec.copy(sourceOutputs = listOf(PortInfo("value", "Int")))
        val output = UIFBPSourceCodeNodeGenerator().generate(singlePort)!!
        assertTrue(output.contains("PortSpec(\"value\", Int::class)"))
        assertTrue(output.contains("override val inputPorts = emptyList<PortSpec>()"))
    }

    // Case 5: multi-port source
    @Test
    fun `generate handles multi-port source correctly`() {
        val output = UIFBPSourceCodeNodeGenerator().generate(demoSpec)!!
        assertTrue(output.contains("PortSpec(\"numA\", Double::class)"))
        assertTrue(output.contains("PortSpec(\"numB\", Double::class)"))
    }

    // Case 6: determinism (SC-005)
    @Test
    fun `generate output is byte-identical across two consecutive calls`() {
        val a = UIFBPSourceCodeNodeGenerator().generate(demoSpec)
        val b = UIFBPSourceCodeNodeGenerator().generate(demoSpec)
        assertEquals(a, b)
    }

    // Case 7: NO singleton State / StateStore reference
    @Test
    fun `generate does NOT reference any singleton State or StateStore`() {
        val output = UIFBPSourceCodeNodeGenerator().generate(demoSpec)!!
        assertFalse(output.contains("DemoUIState._"),
            "Design B: SourceCodeNode reads from controller-supplied SharedFlows, not singleton flows")
        assertFalse(output.contains("StateStore"))
        assertFalse(output.contains("import io.codenode.demo.viewmodel.DemoUIState"),
            "Design B: SourceCodeNode no longer needs to import the (gone) singleton")
    }
}
