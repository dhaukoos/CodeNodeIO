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
 * and delegates observable state to {ModuleName}State from the viewModel package.
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
    private val viewModelPackage = "io.codenode.testapp"

    // ========== Test 1: StopWatch-like flow ==========

    @Test
    fun `StopWatch-like flow generates tick imports only for nodes with both inputs and outputs`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertFalse(result.contains("import io.codenode.testapp.usecases.timerEmitterTick"),
            "Source nodes should not have tick stub imports")
        assertFalse(result.contains("import io.codenode.testapp.usecases.displayReceiverTick"),
            "Sink nodes should not have tick stub imports")
    }

    @Test
    fun `StopWatch-like flow generates package declaration`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("package io.codenode.testapp.generated"))
    }

    @Test
    fun `StopWatch-like flow generates class declaration`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("class StopWatch2Flow"))
    }

    @Test
    fun `StopWatch-like flow imports State object from viewModel package`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("import io.codenode.testapp.StopWatch2State"),
            "Should import StopWatch2State from viewModelPackage")
    }

    @Test
    fun `StopWatch-like flow does not import MutableStateFlow or asStateFlow`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertFalse(result.contains("import kotlinx.coroutines.flow.MutableStateFlow"),
            "Should not import MutableStateFlow — owned by State object")
        assertFalse(result.contains("import kotlinx.coroutines.flow.asStateFlow"),
            "Should not import asStateFlow — owned by State object")
    }

    @Test
    fun `StopWatch-like flow imports StateFlow for type declaration`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("import kotlinx.coroutines.flow.StateFlow"))
    }

    @Test
    fun `StopWatch-like flow imports combine and drop for reactive source`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("import kotlinx.coroutines.flow.combine"),
            "Should import combine for multi-output source observation")
        assertTrue(result.contains("import kotlinx.coroutines.flow.drop"),
            "Should import drop for skipping initial emission")
        assertTrue(result.contains("import io.codenode.fbpdsl.runtime.ProcessResult2"),
            "Should import ProcessResult2 for 2-output source")
    }

    @Test
    fun `StopWatch-like flow delegates observable state from State object`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("val secondsFlow: StateFlow<Int> = StopWatch2State.secondsFlow"),
            "Should delegate secondsFlow from StopWatch2State")
        assertTrue(result.contains("val minutesFlow: StateFlow<Int> = StopWatch2State.minutesFlow"),
            "Should delegate minutesFlow from StopWatch2State")
    }

    @Test
    fun `StopWatch-like flow does not create local MutableStateFlow`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertFalse(result.contains("private val _seconds = MutableStateFlow"),
            "Should not create local MutableStateFlow")
        assertFalse(result.contains("asStateFlow()"),
            "Should not use asStateFlow() — State object owns them")
    }

    @Test
    fun `StopWatch-like flow generates source runtime instance with reactive observation`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("internal val timerEmitter = CodeNodeFactory.createSourceOut2<Int, Int>("))
        assertTrue(result.contains("name = \"TimerEmitter\""))
        assertTrue(result.contains("generate = { emit ->"),
            "Source nodes should use generate with emit parameter")
        assertTrue(result.contains("combine("),
            "2-output source should use combine")
        assertTrue(result.contains("StopWatch2State._elapsedSeconds"),
            "Should observe elapsedSeconds state flow")
        assertTrue(result.contains("StopWatch2State._elapsedMinutes"),
            "Should observe elapsedMinutes state flow")
        assertTrue(result.contains(".drop(1).collect"),
            "Should drop initial emission")
        assertTrue(result.contains("ProcessResult2.both(elapsedSeconds, elapsedMinutes)"),
            "Should create ProcessResult2 from combined values")
        assertFalse(result.contains("tickIntervalMs"),
            "Source nodes should not have tickIntervalMs")
        assertFalse(result.contains("tick = timerEmitterTick"),
            "Source nodes should not reference tick stubs")
    }

    @Test
    fun `source node ignores tickIntervalMs config and uses reactive observation`() {
        val timerEmitter = CodeNode(
            id = "timer",
            name = "TimerEmitter",
            codeNodeType = CodeNodeType.SOURCE,
            position = Node.Position(100.0, 200.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                outputPort("timer_out", "value", Int::class, "timer")
            ),
            configuration = mapOf("tickIntervalMs" to "500L")
        )
        val flowGraph = createFlowGraph(name = "ConfigTest", nodes = listOf(timerEmitter))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertFalse(result.contains("tickIntervalMs"),
            "Source nodes should not use tickIntervalMs even if configured")
        assertTrue(result.contains("generate = { emit ->"),
            "Source node should use generate with emit parameter")
        assertTrue(result.contains("ConfigTestState._value.drop(1).collect"),
            "1-output source should observe state flow with drop(1)")
    }

    @Test
    fun `StopWatch-like flow sink consume block updates State object`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("internal val displayReceiver = CodeNodeFactory.createSinkIn2<Int, Int>("))
        assertTrue(result.contains("name = \"DisplayReceiver\""))
        assertTrue(result.contains("consume = { seconds, minutes ->"))
        assertTrue(result.contains("StopWatch2State._seconds.value = seconds"),
            "Consume block should update via StopWatch2State._seconds")
        assertTrue(result.contains("StopWatch2State._minutes.value = minutes"),
            "Consume block should update via StopWatch2State._minutes")
    }

    @Test
    fun `StopWatch-like flow sink consume block does not call tick function`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertFalse(result.contains("displayReceiverTick(seconds, minutes)"),
            "Sink consume block should not call tick function — only updates observable state")
    }

    @Test
    fun `StopWatch-like flow generates wireConnections with numbered channels`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("private fun wireConnections()"))
        assertTrue(result.contains("displayReceiver.inputChannel1 = timerEmitter.outputChannel1"))
        assertTrue(result.contains("displayReceiver.inputChannel2 = timerEmitter.outputChannel2"))
    }

    @Test
    fun `StopWatch-like flow generates start method`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("suspend fun start(scope: CoroutineScope)"))
        assertTrue(result.contains("timerEmitter.start(scope) {}"))
        assertTrue(result.contains("wireConnections()"))
        assertTrue(result.contains("displayReceiver.start(scope) {}"))
    }

    @Test
    fun `StopWatch-like flow generates stop method`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("fun stop()"))
        assertTrue(result.contains("timerEmitter.stop()"))
        assertTrue(result.contains("displayReceiver.stop()"))
    }

    @Test
    fun `StopWatch-like flow reset calls State object reset`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("fun reset()"))
        assertTrue(result.contains("StopWatch2State.reset()"),
            "Reset should call StopWatch2State.reset()")
    }

    @Test
    fun `StopWatch-like flow reset does not directly reset MutableStateFlow values`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertFalse(result.contains("_seconds.value = 0"),
            "Reset should not directly reset MutableStateFlow")
        assertFalse(result.contains("_minutes.value = 0"),
            "Reset should not directly reset MutableStateFlow")
    }

    @Test
    fun `StopWatch-like flow generates CodeNodeFactory import`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("import io.codenode.fbpdsl.model.CodeNodeFactory"))
    }

    @Test
    fun `StopWatch-like flow generates CoroutineScope import`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("import kotlinx.coroutines.CoroutineScope"))
    }

    // ========== Test 2: No connections ==========

    @Test
    fun `no connections generates empty wireConnections body`() {
        val gen = createTestCodeNode(
            "gen", "Generator", CodeNodeType.SOURCE,
            outputPorts = listOf(outputPort("g_out", "value", Int::class, "gen"))
        )
        val sink = createTestCodeNode(
            "sink", "Receiver", CodeNodeType.SINK,
            inputPorts = listOf(inputPort("s_in", "value", Int::class, "sink"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(gen, sink))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("private fun wireConnections()"))
    }

    // ========== Test 3: No boundary nodes ==========

    @Test
    fun `no boundary nodes does not import State object`() {
        val transformer = createTestCodeNode(
            "trans", "Transformer", CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", Int::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "output", String::class, "trans"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(transformer))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertFalse(result.contains("TestFlowState"),
            "Should not reference State object when no boundary nodes exist")
    }

    @Test
    fun `no boundary nodes generates empty reset body`() {
        val transformer = createTestCodeNode(
            "trans", "Transformer", CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", Int::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "output", String::class, "trans"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(transformer))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("fun reset()"))
        assertFalse(result.contains("State.reset()"))
    }

    @Test
    fun `source-only graph imports State object and generates state delegation`() {
        val gen = createTestCodeNode(
            "gen", "ValueGenerator", CodeNodeType.SOURCE,
            outputPorts = listOf(outputPort("g_out", "value", Int::class, "gen"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(gen))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("import io.codenode.testapp.TestFlowState"),
            "Should import State object when source has output ports")
        assertTrue(result.contains("val valueFlow: StateFlow<Int> = TestFlowState.valueFlow"),
            "Should delegate valueFlow from TestFlowState")
    }

    @Test
    fun `StopWatch-like flow delegates observable state for both source and sink ports`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        // Source output ports
        assertTrue(result.contains("val elapsedSecondsFlow: StateFlow<Int> = StopWatch2State.elapsedSecondsFlow"),
            "Should delegate elapsedSecondsFlow from source output port")
        assertTrue(result.contains("val elapsedMinutesFlow: StateFlow<Int> = StopWatch2State.elapsedMinutesFlow"),
            "Should delegate elapsedMinutesFlow from source output port")
        // Sink input ports
        assertTrue(result.contains("val secondsFlow: StateFlow<Int> = StopWatch2State.secondsFlow"),
            "Should delegate secondsFlow from sink input port")
        assertTrue(result.contains("val minutesFlow: StateFlow<Int> = StopWatch2State.minutesFlow"),
            "Should delegate minutesFlow from sink input port")
    }

    // ========== Test 4: Single-output generator + single-input sink ==========

    @Test
    fun `single output generator uses outputChannel`() {
        val gen = createTestCodeNode(
            "gen", "ValueGenerator", CodeNodeType.SOURCE,
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
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("valueSink.inputChannel = valueGenerator.outputChannel"))
    }

    @Test
    fun `single input sink uses inputChannel`() {
        val gen = createTestCodeNode(
            "gen", "ValueGenerator", CodeNodeType.SOURCE,
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
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("valueSink.inputChannel = valueGenerator.outputChannel"))
        assertFalse(result.contains("inputChannel1"))
    }

    @Test
    fun `single output source uses createContinuousSource with reactive observation`() {
        val gen = createTestCodeNode(
            "gen", "ValueGenerator", CodeNodeType.SOURCE,
            outputPorts = listOf(outputPort("g_out", "value", Int::class, "gen"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(gen))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("CodeNodeFactory.createContinuousSource<Int>("))
        assertTrue(result.contains("generate = { emit ->"),
            "Source node should use generate with emit parameter")
        assertTrue(result.contains("TestFlowState._value.drop(1).collect { value ->"),
            "1-output source should observe state flow with drop(1)")
        assertTrue(result.contains("emit(value)"),
            "Should emit value on state change")
        assertFalse(result.contains("tick = valueGeneratorTick"),
            "Source node should not reference tick stub")
    }

    @Test
    fun `single input sink delegates observable state from State object`() {
        val sink = createTestCodeNode(
            "sink", "Logger", CodeNodeType.SINK,
            inputPorts = listOf(inputPort("s_in", "message", String::class, "sink"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(sink))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("val messageFlow: StateFlow<String> = TestFlowState.messageFlow"),
            "Should delegate messageFlow from TestFlowState")
    }

    @Test
    fun `single input sink consume block updates State object`() {
        val sink = createTestCodeNode(
            "sink", "Logger", CodeNodeType.SINK,
            inputPorts = listOf(inputPort("s_in", "message", String::class, "sink"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(sink))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("CodeNodeFactory.createContinuousSink<String>("))
        assertTrue(result.contains("consume = { message ->"))
        assertTrue(result.contains("TestFlowState._message.value = message"),
            "Consume block should reference TestFlowState._message")
        assertFalse(result.contains("loggerTick(message)"),
            "Sink consume block should not call tick function — only updates observable state")
    }

    @Test
    fun `single input sink reset calls State object reset`() {
        val sink = createTestCodeNode(
            "sink", "Logger", CodeNodeType.SINK,
            inputPorts = listOf(inputPort("s_in", "message", String::class, "sink"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(sink))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("TestFlowState.reset()"),
            "Reset should call TestFlowState.reset()")
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
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

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
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("CodeNodeFactory.createContinuousFilter<Int>("))
        assertTrue(result.contains("filter = evenFilterTick"))
    }

    // ========== Test: KDoc header ==========

    @Test
    fun `generates KDoc with flow name and version`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("Flow orchestrator for: StopWatch2"))
        assertTrue(result.contains("Version: 1.0.0"))
    }

    // ========== Test: Any-input node ==========

    @Test
    fun `any-input node uses createIn2AnyOut1Processor factory method`() {
        val anyProcessor = CodeNode(
            id = "proc",
            name = "AnyAdder",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(100.0, 200.0),
            inputPorts = listOf(
                inputPort("p_a", "first", Int::class, "proc"),
                inputPort("p_b", "second", Int::class, "proc")
            ),
            outputPorts = listOf(outputPort("p_r", "result", Int::class, "proc")),
            configuration = mapOf("_genericType" to "in2anyout1")
        )
        val flowGraph = createFlowGraph(nodes = listOf(anyProcessor))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("CodeNodeFactory.createIn2AnyOut1Processor<Int, Int, Int>("),
            "Should use any-input factory method")
        assertTrue(result.contains("process = anyAdderTick"),
            "Should pass process parameter")
    }

    @Test
    fun `any-input node generates initialValue parameters`() {
        val anyProcessor = CodeNode(
            id = "proc",
            name = "AnyAdder",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(100.0, 200.0),
            inputPorts = listOf(
                inputPort("p_a", "first", Int::class, "proc"),
                inputPort("p_b", "second", String::class, "proc")
            ),
            outputPorts = listOf(outputPort("p_r", "result", Int::class, "proc")),
            configuration = mapOf("_genericType" to "in2anyout1")
        )
        val flowGraph = createFlowGraph(nodes = listOf(anyProcessor))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("initialValue1 = 0,"),
            "Should generate initialValue1 for Int type")
        assertTrue(result.contains("initialValue2 = \"\","),
            "Should generate initialValue2 for String type")
    }

    @Test
    fun `any-input sink uses createSinkIn2Any factory method`() {
        val anySink = CodeNode(
            id = "sink",
            name = "AnySink",
            codeNodeType = CodeNodeType.SINK,
            position = Node.Position(100.0, 200.0),
            inputPorts = listOf(
                inputPort("s_a", "data", Int::class, "sink"),
                inputPort("s_b", "label", String::class, "sink")
            ),
            outputPorts = emptyList(),
            configuration = mapOf("_genericType" to "in2anyout0")
        )
        val flowGraph = createFlowGraph(nodes = listOf(anySink))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("CodeNodeFactory.createSinkIn2Any<Int, String>("),
            "Should use any-input sink factory method")
        assertTrue(result.contains("initialValue1 = 0,"),
            "Should generate initialValue1 for Int type")
        assertTrue(result.contains("initialValue2 = \"\","),
            "Should generate initialValue2 for String type")
    }

    @Test
    fun `non-any node with genericType config uses standard factory method`() {
        val processor = CodeNode(
            id = "proc",
            name = "Adder",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(100.0, 200.0),
            inputPorts = listOf(
                inputPort("p_a", "first", Int::class, "proc"),
                inputPort("p_b", "second", Int::class, "proc")
            ),
            outputPorts = listOf(outputPort("p_r", "result", Int::class, "proc")),
            configuration = mapOf("_genericType" to "in2out1")
        )
        val flowGraph = createFlowGraph(nodes = listOf(processor))
        val result = generator.generate(flowGraph, generatedPackage, usecasesPackage, viewModelPackage)

        assertTrue(result.contains("CodeNodeFactory.createIn2Out1Processor<Int, Int, Int>("),
            "Should use standard factory method for non-any node")
        assertFalse(result.contains("initialValue"),
            "Should not generate initialValue for standard nodes")
    }

    // ========== Helper: StopWatch-like FlowGraph ==========

    private fun createStopWatchLikeFlow(): FlowGraph {
        val timerEmitter = createTestCodeNode(
            id = "timer",
            name = "TimerEmitter",
            type = CodeNodeType.SOURCE,
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
