/*
 * UIFBPViewModelGeneratorTest — feature 087 / Design B.
 *
 * Pins the MVI ViewModel shape: state: StateFlow<{Name}State> + fun onEvent(Event).
 * onEvent dispatches through controller.emit<Port>(value) — NO singleton-State writes.
 * Constructor signature unchanged so ModuleSessionFactory's reflection match still works.
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

class UIFBPViewModelGeneratorTest {

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

    // Case 1: constructor signature
    @Test
    fun `generate emits class with constructor (controller ControllerInterface)`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertTrue(output.contains("class DemoUIViewModel("))
        assertTrue(output.contains("private val controller: DemoUIControllerInterface"))
        assertTrue(output.contains(") : ViewModel()"))
        assertTrue(output.contains("import io.codenode.demo.controller.DemoUIControllerInterface"))
    }

    // Case 2: private MutableStateFlow + public state
    @Test
    fun `generate exposes private MutableStateFlow plus public StateFlow named state`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertTrue(output.contains("private val _state = MutableStateFlow(DemoUIState())"))
        assertTrue(output.contains("val state: StateFlow<DemoUIState> = _state.asStateFlow()"))
    }

    // Case 3: per-sinkInput collector folding into _state
    @Test
    fun `generate launches one viewModelScope collector per sinkInput, calling _state update copy port value`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertTrue(output.contains("init {"),
            "FR-003: ViewModel's init block launches per-sink-port collectors")
        assertTrue(output.contains("viewModelScope.launch"),
            "collectors run inside viewModelScope")
        assertTrue(output.contains("controller.results.collect"),
            "each collector reads from controller.<sinkPort>")
        assertTrue(output.contains("_state.update { it.copy(results = value) }"),
            "each emission folds into _state via copy(...)")
        assertTrue(output.contains("import androidx.lifecycle.viewModelScope"))
        assertTrue(output.contains("import kotlinx.coroutines.flow.update"))
        assertTrue(output.contains("import kotlinx.coroutines.launch"))
    }

    // Case 4: empty sinkInputs → init block omitted
    @Test
    fun `generate omits init block when sinkInputs is empty`() {
        val spec = demoSpec.copy(sinkInputs = emptyList(), ipTypeImports = emptyList())
        val output = UIFBPViewModelGenerator().generate(spec)
        assertFalse(output.contains("init {"),
            "FR-009: empty-sinkInputs case omits the init block; _state stays at default")
        assertTrue(output.contains("private val _state = MutableStateFlow(DemoUIState())"),
            "the State data class default constructor still fires")
    }

    // Case 5: onEvent dispatcher with one branch per sourceOutput
    @Test
    fun `generate emits onEvent dispatcher with one when branch per sourceOutput`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertTrue(output.contains("fun onEvent(event: DemoUIEvent)"))
        assertTrue(output.contains("when (event)"))
        assertTrue(output.contains("is DemoUIEvent.UpdateNumA"))
        assertTrue(output.contains("is DemoUIEvent.UpdateNumB"))
    }

    // Case 6: Update branches call controller.emit<Port>(value)
    @Test
    fun `generate uses Update branches calling controller emit-PortName-event-value`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertTrue(output.contains("is DemoUIEvent.UpdateNumA -> controller.emitNumA(event.value)"))
        assertTrue(output.contains("is DemoUIEvent.UpdateNumB -> controller.emitNumB(event.value)"))
    }

    // Case 7: data-object branches call controller.emit<Port>()
    @Test
    fun `generate uses data-object branches calling controller emit-PortName-noargs`() {
        val spec = demoSpec.copy(sourceOutputs = listOf(
            PortInfo("toggleLike", "Unit"),
            PortInfo("goBack", "Unit")
        ), ipTypeImports = emptyList())
        val output = UIFBPViewModelGenerator().generate(spec)
        assertTrue(output.contains("DemoUIEvent.ToggleLike -> controller.emitToggleLike()"))
        assertTrue(output.contains("DemoUIEvent.GoBack -> controller.emitGoBack()"))
    }

    // Case 8: empty sourceOutputs → empty when block
    @Test
    fun `generate emits empty when block when sourceOutputs is empty`() {
        val spec = demoSpec.copy(sourceOutputs = emptyList())
        val output = UIFBPViewModelGenerator().generate(spec)
        assertTrue(output.contains("fun onEvent(event: DemoUIEvent)"))
        assertTrue(output.contains("when (event)"))
        // No branches between when (event) { … }
        assertFalse(output.contains("is DemoUIEvent."),
            "empty sealed interface → no when branches")
    }

    // Case 9: forwarding control surface preserved
    @Test
    fun `generate forwards start stop pause resume reset to controller`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertTrue(output.contains("fun start(): FlowGraph = controller.start()"))
        assertTrue(output.contains("fun stop(): FlowGraph = controller.stop()"))
        assertTrue(output.contains("fun pause(): FlowGraph = controller.pause()"))
        assertTrue(output.contains("fun resume(): FlowGraph = controller.resume()"))
        assertTrue(output.contains("fun reset(): FlowGraph = controller.reset()"))
    }

    // Case 10: NO prior emit(...) aggregate (FR-011)
    @Test
    fun `generate does NOT emit prior emit aggregate`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertFalse(output.contains("fun emit(numA: Double, numB: Double)"),
            "FR-011: the prior emit(...) aggregate API is removed wholesale")
    }

    // Case 11: NO singleton State / StateStore reference (Design B)
    @Test
    fun `generate does NOT reference any singleton State or StateStore (Design B)`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertFalse(output.contains("DemoUIState._"),
            "Design B: ViewModel never writes to a singleton's mutable backing")
        assertFalse(output.contains("DemoUIState.resultsFlow"),
            "Design B: ViewModel reads sink flows from the controller, not from a singleton")
        assertFalse(output.contains("StateStore"),
            "Design B: no StateStore singleton at all")
    }

    // Case 12: determinism (SC-005)
    @Test
    fun `generate output is byte-identical across two consecutive calls`() {
        val a = UIFBPViewModelGenerator().generate(demoSpec)
        val b = UIFBPViewModelGenerator().generate(demoSpec)
        assertEquals(a, b)
    }
}
