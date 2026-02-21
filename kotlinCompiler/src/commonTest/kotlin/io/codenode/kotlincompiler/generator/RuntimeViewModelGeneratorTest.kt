/*
 * RuntimeViewModelGenerator Test
 * Tests for generating {Name}ViewModel.kt from FlowGraph
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * Tests for RuntimeViewModelGenerator - generates {Name}ViewModel.kt
 * extending ViewModel, delegating to Controller Interface.
 */
class RuntimeViewModelGeneratorTest {

    // ========== Test Fixtures ==========

    private fun createTestCodeNode(
        id: String,
        name: String,
        type: CodeNodeType = CodeNodeType.TRANSFORMER,
        inputPorts: List<Port<*>> = emptyList(),
        outputPorts: List<Port<*>> = emptyList()
    ): CodeNode {
        return CodeNode(
            id = id,
            name = name,
            codeNodeType = type,
            position = Node.Position(100.0, 200.0),
            inputPorts = inputPorts,
            outputPorts = outputPorts
        )
    }

    private fun inputPort(id: String, name: String, dataType: kotlin.reflect.KClass<*>, owningNodeId: String): Port<*> {
        return Port(
            id = id,
            name = name,
            direction = Port.Direction.INPUT,
            dataType = dataType,
            owningNodeId = owningNodeId
        )
    }

    private fun outputPort(id: String, name: String, dataType: kotlin.reflect.KClass<*>, owningNodeId: String): Port<*> {
        return Port(
            id = id,
            name = name,
            direction = Port.Direction.OUTPUT,
            dataType = dataType,
            owningNodeId = owningNodeId
        )
    }

    private fun createFlowGraph(
        name: String = "TestFlow",
        nodes: List<CodeNode>,
        connections: List<Connection> = emptyList()
    ): FlowGraph {
        return FlowGraph(
            id = "test-flow",
            name = name,
            version = "1.0.0",
            description = "Test flow description",
            rootNodes = nodes,
            connections = connections
        )
    }

    private val generator = RuntimeViewModelGenerator()
    private val generatedPackage = "io.codenode.testapp.generated"

    // ========== Test 1: StopWatch-like flow ==========

    @Test
    fun `StopWatch-like flow generates ViewModel extending ViewModel`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("class StopWatch2ViewModel("))
        assertTrue(result.contains(") : ViewModel()"))
    }

    @Test
    fun `StopWatch-like flow generates ControllerInterface constructor param`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("private val controller: StopWatch2ControllerInterface"))
    }

    @Test
    fun `StopWatch-like flow generates package declaration`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("package io.codenode.testapp.generated"))
    }

    @Test
    fun `StopWatch-like flow delegates observable state`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("val seconds: StateFlow<Int> = controller.seconds"))
        assertTrue(result.contains("val minutes: StateFlow<Int> = controller.minutes"))
    }

    @Test
    fun `StopWatch-like flow delegates executionState`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("val executionState: StateFlow<ExecutionState> = controller.executionState"))
    }

    @Test
    fun `StopWatch-like flow delegates start method`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("fun start(): FlowGraph = controller.start()"))
    }

    @Test
    fun `StopWatch-like flow delegates stop method`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("fun stop(): FlowGraph = controller.stop()"))
    }

    @Test
    fun `StopWatch-like flow delegates reset method`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("fun reset(): FlowGraph = controller.reset()"))
    }

    @Test
    fun `StopWatch-like flow delegates pause method`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("fun pause(): FlowGraph = controller.pause()"))
    }

    @Test
    fun `StopWatch-like flow delegates resume method`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("fun resume(): FlowGraph = controller.resume()"))
    }

    @Test
    fun `StopWatch-like flow generates required imports`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("import androidx.lifecycle.ViewModel"))
        assertTrue(result.contains("import io.codenode.fbpdsl.model.ExecutionState"))
        assertTrue(result.contains("import io.codenode.fbpdsl.model.FlowGraph"))
        assertTrue(result.contains("import kotlinx.coroutines.flow.StateFlow"))
    }

    // ========== Test 2: No sink nodes → only executionState delegation ==========

    @Test
    fun `no sink nodes delegates only executionState`() {
        val gen = createTestCodeNode(
            "gen", "ValueGenerator", CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("g_out", "value", Int::class, "gen"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(gen))
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("val executionState: StateFlow<ExecutionState> = controller.executionState"))
        assertFalse(result.contains("val value"))
    }

    @Test
    fun `no sink nodes still delegates all control methods`() {
        val gen = createTestCodeNode(
            "gen", "ValueGenerator", CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("g_out", "value", Int::class, "gen"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(gen))
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("fun start(): FlowGraph = controller.start()"))
        assertTrue(result.contains("fun stop(): FlowGraph = controller.stop()"))
        assertTrue(result.contains("fun reset(): FlowGraph = controller.reset()"))
        assertTrue(result.contains("fun pause(): FlowGraph = controller.pause()"))
        assertTrue(result.contains("fun resume(): FlowGraph = controller.resume()"))
    }

    // ========== Helper: StopWatch-like FlowGraph ==========

    private fun createStopWatchLikeFlow(): FlowGraph {
        val timerEmitter = createTestCodeNode(
            id = "timer",
            name = "TimerEmitter",
            type = CodeNodeType.GENERATOR,
            outputPorts = listOf(
                outputPort("timer_sec", "elapsedSeconds", Int::class, "timer"),
                outputPort("timer_min", "elapsedMinutes", Int::class, "timer")
            )
        )
        val displayReceiver = createTestCodeNode(
            id = "display",
            name = "DisplayReceiver",
            type = CodeNodeType.SINK,
            inputPorts = listOf(
                inputPort("display_sec", "seconds", Int::class, "display"),
                inputPort("display_min", "minutes", Int::class, "display")
            )
        )
        val connections = listOf(
            Connection("conn1", "timer", "timer_sec", "display", "display_sec"),
            Connection("conn2", "timer", "timer_min", "display", "display_min")
        )
        return createFlowGraph(
            name = "StopWatch2",
            nodes = listOf(timerEmitter, displayReceiver),
            connections = connections
        )
    }
}
