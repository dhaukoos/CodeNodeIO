/*
 * RuntimeFlowGenerator Test
 * Tests for generating {Name}Flow.kt from FlowGraph
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * Tests for RuntimeFlowGenerator - generates {Name}Flow.kt that creates
 * runtime instances directly via CodeNodeFactory, wires connections,
 * and owns MutableStateFlow properties for sink input ports.
 */
class RuntimeFlowGeneratorTest {

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

    private val generator = RuntimeFlowGenerator()
    private val generatedPackage = "io.codenode.testapp.generated"
    private val usecasesPackage = "io.codenode.testapp.usecases"

    // ========== Test 1: StopWatch-like flow ==========

    @Test
    fun `StopWatch-like flow generates tick imports`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertTrue(result.contains("import io.codenode.testapp.usecases.timerEmitterTick"))
        assertTrue(result.contains("import io.codenode.testapp.usecases.displayReceiverTick"))
    }

    @Test
    fun `StopWatch-like flow generates package declaration`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertTrue(result.contains("package io.codenode.testapp.generated"))
    }

    @Test
    fun `StopWatch-like flow generates class declaration`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertTrue(result.contains("class StopWatch2Flow"))
    }

    @Test
    fun `StopWatch-like flow generates MutableStateFlow properties for sink ports`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertTrue(result.contains("private val _seconds = MutableStateFlow(0)"))
        assertTrue(result.contains("val secondsFlow: StateFlow<Int> = _seconds.asStateFlow()"))
        assertTrue(result.contains("private val _minutes = MutableStateFlow(0)"))
        assertTrue(result.contains("val minutesFlow: StateFlow<Int> = _minutes.asStateFlow()"))
    }

    @Test
    fun `StopWatch-like flow generates generator runtime instance with timed factory`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertTrue(result.contains("internal val timerEmitter = CodeNodeFactory.createTimedOut2Generator<Int, Int>("))
        assertTrue(result.contains("name = \"TimerEmitter\""))
        assertTrue(result.contains("tickIntervalMs = 1000L"))
        assertTrue(result.contains("tick = timerEmitterTick"))
    }

    @Test
    fun `StopWatch-like flow generates sink runtime instance with wrapped consume`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertTrue(result.contains("internal val displayReceiver = CodeNodeFactory.createIn2Sink<Int, Int>("))
        assertTrue(result.contains("name = \"DisplayReceiver\""))
        assertTrue(result.contains("consume = { seconds, minutes ->"))
        assertTrue(result.contains("_seconds.value = seconds"))
        assertTrue(result.contains("_minutes.value = minutes"))
        assertTrue(result.contains("displayReceiverTick(seconds, minutes)"))
    }

    @Test
    fun `StopWatch-like flow generates wireConnections with numbered channels`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertTrue(result.contains("private fun wireConnections()"))
        assertTrue(result.contains("displayReceiver.inputChannel1 = timerEmitter.outputChannel1"))
        assertTrue(result.contains("displayReceiver.inputChannel2 = timerEmitter.outputChannel2"))
    }

    @Test
    fun `StopWatch-like flow generates start method`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertTrue(result.contains("suspend fun start(scope: CoroutineScope)"))
        assertTrue(result.contains("timerEmitter.start(scope)"))
        assertTrue(result.contains("wireConnections()"))
        assertTrue(result.contains("displayReceiver.start(scope)"))
    }

    @Test
    fun `StopWatch-like flow generates stop method`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertTrue(result.contains("fun stop()"))
        assertTrue(result.contains("timerEmitter.stop()"))
        assertTrue(result.contains("displayReceiver.stop()"))
    }

    @Test
    fun `StopWatch-like flow generates reset method that zeroes StateFlows`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertTrue(result.contains("fun reset()"))
        assertTrue(result.contains("_seconds.value = 0"))
        assertTrue(result.contains("_minutes.value = 0"))
    }

    @Test
    fun `StopWatch-like flow generates CodeNodeFactory import`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertTrue(result.contains("import io.codenode.fbpdsl.model.CodeNodeFactory"))
    }

    @Test
    fun `StopWatch-like flow generates coroutine imports`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertTrue(result.contains("import kotlinx.coroutines.CoroutineScope"))
        assertTrue(result.contains("import kotlinx.coroutines.flow.MutableStateFlow"))
        assertTrue(result.contains("import kotlinx.coroutines.flow.StateFlow"))
        assertTrue(result.contains("import kotlinx.coroutines.flow.asStateFlow"))
    }

    // ========== Test 2: No connections ==========

    @Test
    fun `no connections generates empty wireConnections body`() {
        val gen = createTestCodeNode(
            "gen", "Generator", CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("g_out", "value", Int::class, "gen"))
        )
        val sink = createTestCodeNode(
            "sink", "Receiver", CodeNodeType.SINK,
            inputPorts = listOf(inputPort("s_in", "value", Int::class, "sink"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(gen, sink))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        // wireConnections should exist but have no wiring statements
        assertTrue(result.contains("private fun wireConnections()"))
        // Should not contain any channel assignments
        assertFalse(result.contains("inputChannel") && result.contains(" = ") && result.contains("outputChannel"))
    }

    // ========== Test 3: No sink nodes ==========

    @Test
    fun `no sink nodes generates no MutableStateFlow properties`() {
        val gen = createTestCodeNode(
            "gen", "Generator", CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("g_out", "value", Int::class, "gen"))
        )
        val transformer = createTestCodeNode(
            "trans", "Transformer", CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", Int::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "output", String::class, "trans"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(gen, transformer))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertFalse(result.contains("MutableStateFlow"))
        assertFalse(result.contains("asStateFlow"))
    }

    @Test
    fun `no sink nodes generates empty reset body`() {
        val gen = createTestCodeNode(
            "gen", "Generator", CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("g_out", "value", Int::class, "gen"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(gen))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertTrue(result.contains("fun reset()"))
    }

    // ========== Test 4: Single-output generator + single-input sink ==========

    @Test
    fun `single output generator uses outputChannel`() {
        val gen = createTestCodeNode(
            "gen", "ValueGenerator", CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("g_out", "value", Int::class, "gen"))
        )
        val sink = createTestCodeNode(
            "sink", "ValueSink", CodeNodeType.SINK,
            inputPorts = listOf(inputPort("s_in", "data", Int::class, "sink"))
        )
        val connections = listOf(
            Connection("c1", "gen", "g_out", "sink", "s_in")
        )
        val flowGraph = createFlowGraph(nodes = listOf(gen, sink), connections = connections)
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertTrue(result.contains("valueSink.inputChannel = valueGenerator.outputChannel"))
    }

    @Test
    fun `single input sink uses inputChannel`() {
        val gen = createTestCodeNode(
            "gen", "ValueGenerator", CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("g_out", "value", Int::class, "gen"))
        )
        val sink = createTestCodeNode(
            "sink", "ValueSink", CodeNodeType.SINK,
            inputPorts = listOf(inputPort("s_in", "data", Int::class, "sink"))
        )
        val connections = listOf(
            Connection("c1", "gen", "g_out", "sink", "s_in")
        )
        val flowGraph = createFlowGraph(nodes = listOf(gen, sink), connections = connections)
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        // inputChannel (not inputChannel1) for single-input sink
        assertTrue(result.contains("valueSink.inputChannel = valueGenerator.outputChannel"))
        assertFalse(result.contains("inputChannel1"))
    }

    @Test
    fun `single output generator uses createTimedGenerator`() {
        val gen = createTestCodeNode(
            "gen", "ValueGenerator", CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("g_out", "value", Int::class, "gen"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(gen))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertTrue(result.contains("CodeNodeFactory.createTimedGenerator<Int>("))
        assertTrue(result.contains("tick = valueGeneratorTick"))
    }

    @Test
    fun `single input sink creates MutableStateFlow for port`() {
        val sink = createTestCodeNode(
            "sink", "Logger", CodeNodeType.SINK,
            inputPorts = listOf(inputPort("s_in", "message", String::class, "sink"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(sink))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertTrue(result.contains("private val _message = MutableStateFlow(\"\")"))
        assertTrue(result.contains("val messageFlow: StateFlow<String> = _message.asStateFlow()"))
    }

    @Test
    fun `single input sink uses createContinuousSink with wrapped consume`() {
        val sink = createTestCodeNode(
            "sink", "Logger", CodeNodeType.SINK,
            inputPorts = listOf(inputPort("s_in", "message", String::class, "sink"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(sink))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertTrue(result.contains("CodeNodeFactory.createContinuousSink<String>("))
        assertTrue(result.contains("consume = { message ->"))
        assertTrue(result.contains("_message.value = message"))
        assertTrue(result.contains("loggerTick(message)"))
    }

    // ========== Test: Transformer node passes tick directly ==========

    @Test
    fun `transformer node passes tick val directly`() {
        val transformer = createTestCodeNode(
            "trans", "DataTransformer", CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", String::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "output", Int::class, "trans"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(transformer))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertTrue(result.contains("CodeNodeFactory.createContinuousTransformer<String, Int>("))
        assertTrue(result.contains("transform = dataTransformerTick"))
    }

    // ========== Test: Filter node passes tick directly ==========

    @Test
    fun `filter node uses createContinuousFilter with filter param`() {
        val filter = createTestCodeNode(
            "filt", "EvenFilter", CodeNodeType.FILTER,
            inputPorts = listOf(inputPort("f_in", "value", Int::class, "filt")),
            outputPorts = listOf(outputPort("f_out", "value", Int::class, "filt"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(filter))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertTrue(result.contains("CodeNodeFactory.createContinuousFilter<Int>("))
        assertTrue(result.contains("filter = evenFilterTick"))
    }

    // ========== Test: KDoc header ==========

    @Test
    fun `generates KDoc with flow name and version`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage)

        assertTrue(result.contains("Flow orchestrator for: StopWatch2"))
        assertTrue(result.contains("Version: 1.0.0"))
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
