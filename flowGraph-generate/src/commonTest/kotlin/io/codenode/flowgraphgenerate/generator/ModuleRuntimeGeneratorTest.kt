/*
 * ModuleRuntimeGeneratorTest - Tests for the universal-runtime per-module factory generator
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for `ModuleRuntimeGenerator` (feature 085: universal-runtime collapse).
 *
 * Pins the contract for the single new per-module file `{Module}Runtime.kt`
 * which replaces the eliminated thick-stack trio (Controller.kt + Adapter.kt
 * + Flow.kt runtime).
 */
class ModuleRuntimeGeneratorTest {

    // ========== Test fixtures ==========

    private fun outputPort(id: String, name: String, dataType: kotlin.reflect.KClass<*>, owningNodeId: String) =
        Port(
            id = id,
            name = name,
            direction = Port.Direction.OUTPUT,
            dataType = dataType,
            owningNodeId = owningNodeId
        )

    private fun inputPort(id: String, name: String, dataType: kotlin.reflect.KClass<*>, owningNodeId: String) =
        Port(
            id = id,
            name = name,
            direction = Port.Direction.INPUT,
            dataType = dataType,
            owningNodeId = owningNodeId
        )

    private fun codeNode(
        id: String,
        name: String,
        type: CodeNodeType,
        codeNodeClassFqcn: String,
        inputPorts: List<Port<*>> = emptyList(),
        outputPorts: List<Port<*>> = emptyList()
    ) = CodeNode(
        id = id,
        name = name,
        codeNodeType = type,
        position = Node.Position(0.0, 0.0),
        inputPorts = inputPorts,
        outputPorts = outputPorts,
        configuration = mapOf("_codeNodeClass" to codeNodeClassFqcn)
    )

    /**
     * StopWatch-shaped fixture: 3 nodes (TimerEmitter source, TimeIncrementer
     * transformer, DisplayReceiver sink) with two typed sink-input ports.
     */
    private fun stopWatchFlow(): FlowGraph {
        val timer = codeNode(
            id = "timer", name = "TimerEmitter",
            type = CodeNodeType.SOURCE,
            codeNodeClassFqcn = "io.codenode.stopwatch.nodes.TimerEmitterCodeNode",
            outputPorts = listOf(
                outputPort("t_sec", "elapsedSeconds", Int::class, "timer"),
                outputPort("t_min", "elapsedMinutes", Int::class, "timer")
            )
        )
        val incrementer = codeNode(
            id = "inc", name = "TimeIncrementer",
            type = CodeNodeType.TRANSFORMER,
            codeNodeClassFqcn = "io.codenode.stopwatch.nodes.TimeIncrementerCodeNode",
            inputPorts = listOf(
                inputPort("i_sec", "secondsIn", Int::class, "inc"),
                inputPort("i_min", "minutesIn", Int::class, "inc")
            ),
            outputPorts = listOf(
                outputPort("i_sec_out", "secondsOut", Int::class, "inc"),
                outputPort("i_min_out", "minutesOut", Int::class, "inc")
            )
        )
        val display = codeNode(
            id = "display", name = "DisplayReceiver",
            type = CodeNodeType.SINK,
            codeNodeClassFqcn = "io.codenode.stopwatch.nodes.DisplayReceiverCodeNode",
            inputPorts = listOf(
                inputPort("d_sec", "seconds", Int::class, "display"),
                inputPort("d_min", "minutes", Int::class, "display")
            )
        )
        return FlowGraph(
            id = "test-flow",
            name = "StopWatch",
            version = "1.0.0",
            rootNodes = listOf(timer, incrementer, display),
            connections = emptyList()
        )
    }

    private val generator = ModuleRuntimeGenerator()
    private val basePackage = "io.codenode.stopwatch"
    private val controllerPackage = "io.codenode.stopwatch.controller"
    private val viewModelPackage = "io.codenode.stopwatch.viewmodel"

    // ========== T012: NodeRegistry emission ==========

    @Test
    fun `emits node registry object with one when arm per node`() {
        val source = generator.generate(stopWatchFlow(), basePackage, controllerPackage, viewModelPackage)

        assertTrue(source.contains("object StopWatchNodeRegistry"),
            "must declare StopWatchNodeRegistry object")
        assertTrue(source.contains("fun lookup(name: String): CodeNodeDefinition?"),
            "registry must expose lookup(name)")
        assertTrue(source.contains("\"TimerEmitter\"") &&
                   source.contains("TimerEmitterCodeNode"),
            "must map TimerEmitter → TimerEmitterCodeNode")
        assertTrue(source.contains("\"TimeIncrementer\"") &&
                   source.contains("TimeIncrementerCodeNode"),
            "must map TimeIncrementer → TimeIncrementerCodeNode")
        assertTrue(source.contains("\"DisplayReceiver\"") &&
                   source.contains("DisplayReceiverCodeNode"),
            "must map DisplayReceiver → DisplayReceiverCodeNode")
        assertTrue(source.contains("else"),
            "lookup must include an else branch returning null")
    }

    // ========== T013: Factory function signature + DynamicPipelineController wiring ==========

    @Test
    fun `emits factory function with controller-interface return type`() {
        val source = generator.generate(stopWatchFlow(), basePackage, controllerPackage, viewModelPackage)
        assertTrue(
            source.contains(
                "fun createStopWatchRuntime(flowGraph: FlowGraph): StopWatchControllerInterface"
            ),
            "factory function signature mismatch; expected: " +
                "fun createStopWatchRuntime(flowGraph: FlowGraph): StopWatchControllerInterface"
        )
    }

    @Test
    fun `factory body constructs DynamicPipelineController with provider lookup and onReset`() {
        val source = generator.generate(stopWatchFlow(), basePackage, controllerPackage, viewModelPackage)
        assertTrue(source.contains("DynamicPipelineController("),
            "must instantiate DynamicPipelineController")
        assertTrue(source.contains("flowGraphProvider = { flowGraph }"),
            "must pass flowGraphProvider closure capturing the flowGraph param")
        assertTrue(source.contains("StopWatchNodeRegistry::lookup"),
            "must pass StopWatchNodeRegistry::lookup as the lookup function")
        assertTrue(source.contains("StopWatchState::reset"),
            "must pass StopWatchState::reset as the onReset callback")
    }

    // ========== T014: Object expression with ModuleController delegation + typed flows ==========

    @Test
    fun `factory returns object expression delegating ModuleController to controller`() {
        val source = generator.generate(stopWatchFlow(), basePackage, controllerPackage, viewModelPackage)
        // Kotlin interface delegation — handles every inherited member
        assertTrue(
            source.contains("object : StopWatchControllerInterface, ModuleController by controller"),
            "factory must return an object expression with ModuleController interface delegation"
        )
    }

    @Test
    fun `factory emits override val for every observable boundary port`() {
        val source = generator.generate(stopWatchFlow(), basePackage, controllerPackage, viewModelPackage)
        // Every property declared by RuntimeControllerInterfaceGenerator (source outputs +
        // sink inputs from ObservableStateResolver) must be overridden, otherwise the
        // anonymous `object : ControllerInterface, ModuleController by controller` won't compile.
        assertTrue(source.contains("override val seconds = StopWatchState.secondsFlow"),
            "must override sink-input port `seconds`")
        assertTrue(source.contains("override val minutes = StopWatchState.minutesFlow"),
            "must override sink-input port `minutes`")
        assertTrue(source.contains("override val elapsedSeconds = StopWatchState.elapsedSecondsFlow"),
            "must override source-output port `elapsedSeconds` (also declared on the interface)")
        assertTrue(source.contains("override val elapsedMinutes = StopWatchState.elapsedMinutesFlow"),
            "must override source-output port `elapsedMinutes` (also declared on the interface)")
    }

    @Test
    fun `factory does not emit explicit overrides for inherited control members`() {
        val source = generator.generate(stopWatchFlow(), basePackage, controllerPackage, viewModelPackage)
        // start/stop/pause/resume/reset/executionState/getStatus/setAttenuationDelay/observers
        // come from ModuleController by controller delegation — no explicit overrides.
        assertFalse(source.contains("override fun start()"),
            "start() must come via ModuleController delegation, not explicit override")
        assertFalse(source.contains("override val executionState"),
            "executionState must come via ModuleController delegation, not explicit override")
    }

    // ========== T015: Generator marker comment + imports + package ==========

    @Test
    fun `emits generator marker comment in header`() {
        val source = generator.generate(stopWatchFlow(), basePackage, controllerPackage, viewModelPackage)
        assertTrue(source.contains("Generated by CodeNodeIO ModuleRuntimeGenerator"),
            "header must carry the standard generator marker comment")
    }

    @Test
    fun `emits package declaration at controller subpackage`() {
        val source = generator.generate(stopWatchFlow(), basePackage, controllerPackage, viewModelPackage)
        assertTrue(source.contains("package io.codenode.stopwatch.controller"),
            "file must live in the controller subpackage alongside StopWatchControllerInterface")
    }

    @Test
    fun `does not import own-package ControllerInterface`() {
        val source = generator.generate(stopWatchFlow(), basePackage, controllerPackage, viewModelPackage)
        assertFalse(
            source.contains("import io.codenode.stopwatch.controller.StopWatchControllerInterface"),
            "ControllerInterface lives in the same package — must not be self-imported"
        )
    }

    @Test
    fun `emits required imports`() {
        val source = generator.generate(stopWatchFlow(), basePackage, controllerPackage, viewModelPackage)
        assertTrue(source.contains("import io.codenode.fbpdsl.model.FlowGraph"))
        assertTrue(source.contains("import io.codenode.fbpdsl.runtime.CodeNodeDefinition"))
        assertTrue(source.contains("import io.codenode.fbpdsl.runtime.DynamicPipelineController"))
        assertTrue(source.contains("import io.codenode.fbpdsl.runtime.ModuleController"))
        assertTrue(source.contains("import io.codenode.stopwatch.viewmodel.StopWatchState"))
        // Per-node CodeNodeDefinition imports
        assertTrue(source.contains("import io.codenode.stopwatch.nodes.TimerEmitterCodeNode"))
        assertTrue(source.contains("import io.codenode.stopwatch.nodes.TimeIncrementerCodeNode"))
        assertTrue(source.contains("import io.codenode.stopwatch.nodes.DisplayReceiverCodeNode"))
    }

    // ========== Edge case: zero-node graph ==========

    @Test
    fun `degenerates gracefully for graph with zero nodes`() {
        val emptyGraph = FlowGraph(
            id = "empty", name = "Empty", version = "1.0.0",
            rootNodes = emptyList(), connections = emptyList()
        )
        val source = generator.generate(emptyGraph, "io.codenode.empty",
            "io.codenode.empty.controller", "io.codenode.empty.viewmodel")
        assertTrue(source.contains("object EmptyNodeRegistry"))
        assertTrue(source.contains("fun createEmptyRuntime"))
        assertTrue(source.contains("else"),
            "empty registry's lookup must still have an else branch")
    }
}
