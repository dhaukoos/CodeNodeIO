/*
 * UIFBPViewModelGeneratorTest — feature 087 (T005 split from UIFBPGeneratorTest).
 *
 * Pre-feature-087 baseline: assertions exercise today's emit(...)-aggregate
 * ViewModel shape. T008 RED replaces these with state/onEvent fixture-string
 * comparisons per contracts/viewmodel-generator.md. The split is mechanical.
 *
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.generator

import io.codenode.flowgraphgenerate.parser.PortInfo
import io.codenode.flowgraphgenerate.parser.UIFBPSpec
import kotlin.test.Test
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

    @Test
    fun `ViewModelGenerator emits to viewmodel subpackage with flowGraphPrefix-derived name`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertTrue(output.contains("package io.codenode.demo.viewmodel"),
            "ViewModel lives at {basePackage}.viewmodel matching ModuleSessionFactory's lookup")
    }

    @Test
    fun `ViewModelGenerator constructor takes typed ControllerInterface and extends ViewModel`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        // The generated ViewModel must accept the typed ControllerInterface so
        // ModuleSessionFactory.tryCreateViewModel's reflection match succeeds.
        assertTrue(
            output.contains("class DemoUIViewModel(") &&
                output.contains("private val controller: DemoUIControllerInterface") &&
                output.contains(") : ViewModel()"),
            "post-085: ViewModel constructor MUST be (private val controller: " +
                "DemoUIControllerInterface) and the class MUST extend ViewModel"
        )
        assertTrue(output.contains("import androidx.lifecycle.ViewModel"))
        assertTrue(output.contains("import io.codenode.demo.controller.DemoUIControllerInterface"),
            "the ControllerInterface lives in {basePackage}.controller")
    }

    @Test
    fun `ViewModelGenerator exposes StateFlow properties read directly from State`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertTrue(output.contains("val results: StateFlow<CalculationResults?>"))
        assertTrue(output.contains("DemoUIState.resultsFlow"),
            "flows are read directly from {FlowGraph}State (matches WeatherForecast/Addresses precedent)")
    }

    @Test
    fun `ViewModelGenerator exposes executionState from the controller`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertTrue(output.contains("val executionState"),
            "post-085 ViewModel re-exposes executionState from the inherited ModuleController surface")
        assertTrue(output.contains("controller.executionState"),
            "executionState comes from the ControllerInterface (inherits from ModuleController)")
    }

    @Test
    fun `ViewModelGenerator generates emit method writing to State mutable fields`() {
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertTrue(output.contains("fun emit(numA: Double, numB: Double)"))
        assertTrue(output.contains("DemoUIState._numA.value = numA"))
        assertTrue(output.contains("DemoUIState._numB.value = numB"))
    }

    @Test
    fun `ViewModelGenerator emits forwarding control methods delegating to controller`() {
        // Per data-model.md §5 + tasks T007/T008: the UI calls viewModel.start() etc. directly.
        // These methods are NOT inherited from androidx.lifecycle.ViewModel; the generator must
        // emit them as one-line delegations.
        val output = UIFBPViewModelGenerator().generate(demoSpec)
        assertTrue(output.contains("fun start(): FlowGraph = controller.start()"),
            "must forward start() to controller.start()")
        assertTrue(output.contains("fun stop(): FlowGraph = controller.stop()"),
            "must forward stop() to controller.stop()")
        assertTrue(output.contains("fun pause(): FlowGraph = controller.pause()"),
            "must forward pause() to controller.pause()")
        assertTrue(output.contains("fun resume(): FlowGraph = controller.resume()"),
            "must forward resume() to controller.resume()")
        assertTrue(output.contains("fun reset(): FlowGraph = controller.reset()"),
            "must forward reset() to controller.reset()")
    }
}
