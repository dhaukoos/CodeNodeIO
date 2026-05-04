/*
 * UIFBPRuntimeFactoryTest — feature 087 / Decision 8.
 *
 * Pins the per-flow-graph Runtime factory shape: declares MutableStateFlow
 * per sinkInput, MutableSharedFlow per sourceOutput, wires sinkWrapper /
 * sourceWrapper via withReporters / withSources, and returns an anonymous
 * object overriding the per-sink-port StateFlows + emit<Port> methods.
 *
 * Tests exercise the now-internal generateRuntimeFactory entry point on
 * UIFBPInterfaceGenerator (visibility opened by T019 GREEN as part of the
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

class UIFBPRuntimeFactoryTest {

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
        UIFBPInterfaceGenerator().generateRuntimeFactory(
            spec,
            "${spec.packageName}.controller",
            "${spec.packageName}.viewmodel"
        )

    // Case 1: factory signature locked
    @Test
    fun `factory signature is create Name Runtime flowGraph FlowGraph ControllerInterface`() {
        val output = generate(demoSpec)
        assertTrue(output.contains("fun createDemoUIRuntime(flowGraph: FlowGraph): DemoUIControllerInterface"),
            "FR-007: factory signature MUST stay unchanged so ModuleSessionFactory keeps working")
    }

    // Case 2: per-sinkInput MutableStateFlow declarations
    @Test
    fun `factory body declares one MutableStateFlow per sinkInput`() {
        val output = generate(demoSpec)
        assertTrue(output.contains("val _results = MutableStateFlow<CalculationResults?>(null)"),
            "Decision 8: per-flow-graph state lives in the factory closure, not a singleton")
        assertTrue(output.contains("import kotlinx.coroutines.flow.MutableStateFlow"))
    }

    // Case 3: per-sourceOutput MutableSharedFlow declarations
    @Test
    fun `factory body declares one MutableSharedFlow per sourceOutput`() {
        val output = generate(demoSpec)
        assertTrue(output.contains("val _numA = MutableSharedFlow<Double>(replay = 1, extraBufferCapacity = 64)"))
        assertTrue(output.contains("val _numB = MutableSharedFlow<Double>(replay = 1, extraBufferCapacity = 64)"))
        assertTrue(output.contains("import kotlinx.coroutines.flow.MutableSharedFlow"))
    }

    // Case 4: sinkWrapper via withReporters with one reporter per sinkInput
    @Test
    fun `factory body wires sinkWrapper via withReporters with one reporter per sinkInput`() {
        val output = generate(demoSpec)
        assertTrue(output.contains("DemoUISinkCodeNode.withReporters("),
            "Design B: SinkCodeNode is wired via withReporters wrapper")
        assertTrue(output.contains("_results.value = value as CalculationResults?"),
            "the reporter writes the IP into the per-flow-graph MutableStateFlow")
    }

    // Case 5: sourceWrapper via withSources with one flow per sourceOutput
    @Test
    fun `factory body wires sourceWrapper via withSources with one flow per sourceOutput`() {
        val output = generate(demoSpec)
        assertTrue(output.contains("DemoUISourceCodeNode.withSources(_numA, _numB)"),
            "Design B: SourceCodeNode is wired via withSources wrapper with the per-flow-graph SharedFlows")
    }

    // Case 6: returned object overrides per-sink-port StateFlow + per-source-port emit
    @Test
    fun `factory returned object overrides one StateFlow per sinkInput plus one emit per sourceOutput`() {
        val output = generate(demoSpec)
        assertTrue(output.contains("override val results: StateFlow<CalculationResults?> = _results.asStateFlow()"))
        assertTrue(output.contains("override fun emitNumA(value: Double)"))
        assertTrue(output.contains("override fun emitNumB(value: Double)"))
    }

    // Case 7: emit dispatches via controller.coroutineScope
    @Test
    fun `factory body uses controller coroutineScope launch for source emits`() {
        val output = generate(demoSpec)
        assertTrue(output.contains("controller.coroutineScope?.launch"),
            "source-emit dispatchers MUST tolerate the no-scope window (controller not yet started or stopped)")
        assertTrue(output.contains("_numA.emit(value)"))
        assertTrue(output.contains("_numB.emit(value)"))
    }

    // Case 8: onReset resets every sink MutableStateFlow to its declared default
    @Test
    fun `factory body onReset resets every sink MutableStateFlow to declared default`() {
        val output = generate(demoSpec)
        assertTrue(output.contains("onReset = {"),
            "the controller's onReset hook is wired up by the factory")
        assertTrue(output.contains("_results.value = null"),
            "nullable IP type → reset to null")
    }

    // Case 9: NO singleton State / StateStore reference
    @Test
    fun `factory body does NOT reference any singleton State or StateStore`() {
        val output = generate(demoSpec)
        assertFalse(output.contains("DemoUIState._"),
            "Design B: no singleton-State writes anywhere in the factory")
        assertFalse(output.contains("DemoUIState.resultsFlow"),
            "Design B: per-sink-port flows are factory-local, not singleton-derived")
        assertFalse(output.contains("StateStore"))
    }

    // Case 10: determinism (SC-005)
    @Test
    fun `factory output is byte-identical across two consecutive calls`() {
        val a = generate(demoSpec)
        val b = generate(demoSpec)
        assertEquals(a, b)
    }
}
