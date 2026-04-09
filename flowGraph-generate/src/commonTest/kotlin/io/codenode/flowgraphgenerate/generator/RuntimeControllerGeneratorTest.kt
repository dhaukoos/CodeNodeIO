/*
 * RuntimeControllerGenerator Test
 * Tests for generating {Name}Controller.kt from FlowGraph
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * Tests for RuntimeControllerGenerator - generates {Name}Controller.kt that
 * wraps the Flow with execution control and exposes observable state as
 * StateFlow properties.
 */
class RuntimeControllerGeneratorTest {

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

    private val generator = RuntimeControllerGenerator()
    private val generatedPackage = "io.codenode.testapp.generated"
    private val viewModelPackage = "io.codenode.testapp"

    // ========== Test 1: StopWatch-like flow ==========

    @Test
    fun `StopWatch-like flow generates class with FlowGraph param`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("class StopWatch2Controller("))
        assertTrue(result.contains("private var flowGraph: FlowGraph"))
    }

    @Test
    fun `StopWatch-like flow generates package declaration`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("package io.codenode.testapp.generated"))
    }

    @Test
    fun `StopWatch-like flow generates RuntimeRegistry`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("private val registry = RuntimeRegistry()"))
    }

    @Test
    fun `StopWatch-like flow generates RootControlNode`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("RootControlNode.createFor("))
        assertTrue(result.contains("name = \"StopWatch2Controller\""))
        assertTrue(result.contains("registry = registry"))
    }

    @Test
    fun `StopWatch-like flow generates Flow instance`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("private val flow = StopWatch2Flow()"))
    }

    @Test
    fun `StopWatch-like flow generates CoroutineScope and wasRunningBeforePause`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("private var flowScope: CoroutineScope? = null"))
        assertTrue(result.contains("private var wasRunningBeforePause: Boolean = false"))
    }

    @Test
    fun `StopWatch-like flow generates observable state delegated from flow`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("val seconds: StateFlow<Int> = flow.secondsFlow"))
        assertTrue(result.contains("val minutes: StateFlow<Int> = flow.minutesFlow"))
    }

    @Test
    fun `StopWatch-like flow generates executionState StateFlow`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("private val _executionState = MutableStateFlow(ExecutionState.IDLE)"))
        assertTrue(result.contains("val executionState: StateFlow<ExecutionState> = _executionState.asStateFlow()"))
    }

    @Test
    fun `StopWatch-like flow generates start method with registry wiring`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("fun start(): FlowGraph"))
        assertTrue(result.contains("flowGraph = controller.startAll()"))
        assertTrue(result.contains("_executionState.value = ExecutionState.RUNNING"))
        assertTrue(result.contains("flow.timerEmitter.registry = registry"))
        assertTrue(result.contains("flow.displayReceiver.registry = registry"))
        assertTrue(result.contains("flow.timerEmitter.executionState = ExecutionState.RUNNING"))
        assertTrue(result.contains("flow.start(scope)"))
    }

    @Test
    fun `StopWatch-like flow start does not set sink executionState to RUNNING`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertFalse(result.contains("flow.displayReceiver.executionState = ExecutionState.RUNNING"))
    }

    @Test
    fun `StopWatch-like flow generates pause method`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("fun pause(): FlowGraph"))
        assertTrue(result.contains("flowGraph = controller.pauseAll()"))
        assertTrue(result.contains("_executionState.value = ExecutionState.PAUSED"))
    }

    @Test
    fun `StopWatch-like flow generates resume method`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("fun resume(): FlowGraph"))
        assertTrue(result.contains("flowGraph = controller.resumeAll()"))
    }

    @Test
    fun `StopWatch-like flow generates stop method`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("fun stop(): FlowGraph"))
        assertTrue(result.contains("flowGraph = controller.stopAll()"))
        assertTrue(result.contains("_executionState.value = ExecutionState.IDLE"))
        assertTrue(result.contains("flow.stop()"))
        assertTrue(result.contains("flowScope?.cancel()"))
    }

    @Test
    fun `StopWatch-like flow generates reset method`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("fun reset(): FlowGraph"))
        assertTrue(result.contains("wasRunningBeforePause = false"))
        assertFalse(result.contains("flow.timerEmitter.reset()"),
            "Should not call reset() on runtime instances (NodeRuntime has no reset method)")
        assertFalse(result.contains("flow.displayReceiver.reset()"),
            "Should not call reset() on runtime instances (NodeRuntime has no reset method)")
        assertTrue(result.contains("stop()"))
        assertTrue(result.contains("flow.reset()"))
        assertTrue(result.contains("return flowGraph"))
    }

    @Test
    fun `StopWatch-like flow generates getStatus method`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("fun getStatus(): FlowExecutionStatus"))
        assertTrue(result.contains("controller.getStatus()"))
    }

    @Test
    fun `StopWatch-like flow generates setNodeState method`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("fun setNodeState(nodeId: String, state: ExecutionState): FlowGraph"))
        assertTrue(result.contains("controller.setNodeState(nodeId, state)"))
    }

    @Test
    fun `StopWatch-like flow generates setNodeConfig method`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("fun setNodeConfig(nodeId: String, config: ControlConfig): FlowGraph"))
        assertTrue(result.contains("controller.setNodeConfig(nodeId, config)"))
    }

    @Test
    fun `StopWatch-like flow generates setAttenuationDelay method for all nodes`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("fun setAttenuationDelay(ms: Long?)"))
        assertTrue(result.contains("flow.timerEmitter.attenuationDelayMs = ms"),
            "Should set attenuationDelayMs on source nodes")
        assertTrue(result.contains("flow.displayReceiver.attenuationDelayMs = ms"),
            "Should set attenuationDelayMs on all nodes including sinks")
    }

    @Test
    fun `StopWatch-like flow with processor sets delay on all nodes`() {
        val flowGraph = createStopWatchWithProcessorFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("flow.timerEmitter.attenuationDelayMs = ms"),
            "Should set attenuationDelayMs on source node")
        assertTrue(result.contains("flow.timeIncrementer.attenuationDelayMs = ms"),
            "Should set attenuationDelayMs on processor node")
        assertTrue(result.contains("flow.displayReceiver.attenuationDelayMs = ms"),
            "Should set attenuationDelayMs on sink node")
    }

    @Test
    fun `StopWatch-like flow generates currentFlowGraph getter`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("val currentFlowGraph: FlowGraph"))
        assertTrue(result.contains("get() = flowGraph"))
    }

    @Test
    fun `StopWatch-like flow generates framework imports`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("import io.codenode.fbpdsl.model.RootControlNode"))
        assertTrue(result.contains("import io.codenode.fbpdsl.model.FlowGraph"))
        assertTrue(result.contains("import io.codenode.fbpdsl.model.ExecutionState"))
        assertTrue(result.contains("import io.codenode.fbpdsl.model.ControlConfig"))
        assertTrue(result.contains("import io.codenode.fbpdsl.model.FlowExecutionStatus"))
        assertTrue(result.contains("import io.codenode.fbpdsl.runtime.RuntimeRegistry"))
        assertTrue(result.contains("import kotlinx.coroutines.CoroutineScope"))
        assertTrue(result.contains("import kotlinx.coroutines.flow.MutableStateFlow"))
        assertTrue(result.contains("import kotlinx.coroutines.flow.StateFlow"))
    }

    // ========== Test 2: No boundary nodes → only executionState property ==========

    @Test
    fun `no boundary nodes generates only executionState property`() {
        val transformer = createTestCodeNode(
            "trans", "DataTransformer", CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", Int::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "output", String::class, "trans"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(transformer))
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("val executionState: StateFlow<ExecutionState>"))
        // No observable state from boundary ports
        assertFalse(result.contains("= flow.") && result.contains("Flow"))
    }

    @Test
    fun `no boundary nodes does not delegate observable state from flow`() {
        val transformer = createTestCodeNode(
            "trans", "DataTransformer", CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", Int::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "output", String::class, "trans"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(transformer))
        val result = generator.generate(flowGraph, generatedPackage)

        // Should not have any "val xxx: StateFlow<...> = flow.xxxFlow" lines
        // but SHOULD have executionState
        assertTrue(result.contains("val executionState: StateFlow<ExecutionState>"))
        assertFalse(result.contains("flow.inputFlow"))
    }

    @Test
    fun `no sink nodes still generates all control methods`() {
        val gen = createTestCodeNode(
            "gen", "ValueGenerator", CodeNodeType.SOURCE,
            outputPorts = listOf(outputPort("g_out", "value", Int::class, "gen"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(gen))
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("fun start(): FlowGraph"))
        assertTrue(result.contains("fun stop(): FlowGraph"))
        assertTrue(result.contains("fun pause(): FlowGraph"))
        assertTrue(result.contains("fun resume(): FlowGraph"))
        assertTrue(result.contains("fun reset(): FlowGraph"))
    }

    // ========== Test 3: bindToLifecycle ==========

    @Test
    fun `generates bindToLifecycle with lifecycle observer`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("fun bindToLifecycle(lifecycle: Lifecycle)"))
        assertTrue(result.contains("lifecycle.addObserver"))
        assertTrue(result.contains("LifecycleEventObserver"))
        assertTrue(result.contains("Lifecycle.Event.ON_START"))
        assertTrue(result.contains("Lifecycle.Event.ON_STOP"))
        assertTrue(result.contains("Lifecycle.Event.ON_DESTROY"))
        assertTrue(result.contains("wasRunningBeforePause"))
    }

    @Test
    fun `generates lifecycle imports`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("import androidx.lifecycle.Lifecycle"))
        assertTrue(result.contains("import androidx.lifecycle.LifecycleEventObserver"))
        assertTrue(result.contains("import androidx.lifecycle.LifecycleOwner"))
    }

    // ========== Test: Controller priming ==========

    @Test
    fun `start primes source output channels with initial state values`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, viewModelPackage)

        assertTrue(result.contains("flow.timerEmitter.outputChannel1?.send(StopWatch2State._elapsedSeconds.value)"),
            "Should prime outputChannel1 with elapsedSeconds state value")
        assertTrue(result.contains("flow.timerEmitter.outputChannel2?.send(StopWatch2State._elapsedMinutes.value)"),
            "Should prime outputChannel2 with elapsedMinutes state value")
    }

    @Test
    fun `start primes single-output source with outputChannel`() {
        val gen = createTestCodeNode(
            "gen", "ValueGenerator", CodeNodeType.SOURCE,
            outputPorts = listOf(outputPort("g_out", "value", Int::class, "gen"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(gen))
        val result = generator.generate(flowGraph, generatedPackage, viewModelPackage)

        assertTrue(result.contains("flow.valueGenerator.outputChannel?.send(TestFlowState._value.value)"),
            "Should prime outputChannel with value state")
    }

    @Test
    fun `start does not prime when viewModelPackage is null`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertFalse(result.contains("State._"),
            "Should not generate priming when viewModelPackage is null")
    }

    // ========== Test: State import ==========

    @Test
    fun `imports State when viewModelPackage provided and sources exist`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage, viewModelPackage)

        assertTrue(result.contains("import io.codenode.testapp.StopWatch2State"),
            "Should import State object from viewModelPackage")
    }

    @Test
    fun `does not import State when viewModelPackage is null`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertFalse(result.contains("import io.codenode.testapp.StopWatch2State"),
            "Should not import State when viewModelPackage is null")
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

    private fun createStopWatchWithProcessorFlow(): FlowGraph {
        val timerEmitter = createTestCodeNode(
            id = "timer",
            name = "TimerEmitter",
            type = CodeNodeType.SOURCE,
            outputPorts = listOf(
                outputPort("timer_sec", "elapsedSeconds", Int::class, "timer"),
                outputPort("timer_min", "elapsedMinutes", Int::class, "timer")
            )
        )
        val timeIncrementer = createTestCodeNode(
            id = "proc",
            name = "TimeIncrementer",
            type = CodeNodeType.TRANSFORMER,
            inputPorts = listOf(
                inputPort("proc_sec_in", "seconds", Int::class, "proc"),
                inputPort("proc_min_in", "minutes", Int::class, "proc")
            ),
            outputPorts = listOf(
                outputPort("proc_sec_out", "seconds", Int::class, "proc"),
                outputPort("proc_min_out", "minutes", Int::class, "proc")
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
        return createFlowGraph(
            name = "StopWatch2",
            nodes = listOf(timerEmitter, timeIncrementer, displayReceiver)
        )
    }
}
