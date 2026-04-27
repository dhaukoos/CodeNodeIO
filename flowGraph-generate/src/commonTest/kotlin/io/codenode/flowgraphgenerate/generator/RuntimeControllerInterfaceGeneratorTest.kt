/*
 * RuntimeControllerInterfaceGenerator Test
 * Tests for generating {Name}ControllerInterface.kt from FlowGraph
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * Tests for RuntimeControllerInterfaceGenerator - generates {Name}ControllerInterface.kt
 * declaring the same control methods and StateFlow properties as the Controller.
 */
class RuntimeControllerInterfaceGeneratorTest {

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

    private val generator = RuntimeControllerInterfaceGenerator()
    private val generatedPackage = "io.codenode.testapp.generated"

    // ========== Test 1: StopWatch-like flow ==========

    @Test
    fun `StopWatch-like flow generates interface declaration`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("interface StopWatch2ControllerInterface"))
    }

    @Test
    fun `StopWatch-like flow generates package declaration`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("package io.codenode.testapp.generated"))
    }

    @Test
    fun `StopWatch-like flow generates observable state properties`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("val seconds: StateFlow<Int>"))
        assertTrue(result.contains("val minutes: StateFlow<Int>"))
    }

    // executionState + control methods (start/stop/pause/resume/reset) are no longer
    // declared on the interface body — they're inherited from ModuleController.
    // See `interface body does not redeclare members inherited from ModuleController`
    // and `interface declaration extends ModuleController` below.

    @Test
    fun `StopWatch-like flow generates required imports`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains("import io.codenode.fbpdsl.model.FlowGraph"))
        assertTrue(result.contains("import kotlinx.coroutines.flow.StateFlow"))
        // After feature 085: ExecutionState is no longer imported here because
        // executionState is inherited from ModuleController.
        assertTrue(result.contains("import io.codenode.fbpdsl.runtime.ModuleController"))
    }

    // ========== Feature 085: ModuleController inheritance ==========

    @Test
    fun `interface declaration extends ModuleController`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(
            result.contains("interface StopWatch2ControllerInterface : ModuleController"),
            "interface must extend ModuleController so consumers reach " +
                "setAttenuationDelay/observers/getStatus through the typed surface"
        )
    }

    @Test
    fun `interface body does not redeclare members inherited from ModuleController`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, generatedPackage)

        // The typed sink-input flows (seconds/minutes) stay; everything else
        // comes from ModuleController by inheritance.
        assertFalse(result.contains("fun start(): FlowGraph"),
            "start() must be inherited from ModuleController, not redeclared")
        assertFalse(result.contains("fun stop(): FlowGraph"),
            "stop() must be inherited")
        assertFalse(result.contains("fun pause(): FlowGraph"),
            "pause() must be inherited")
        assertFalse(result.contains("fun resume(): FlowGraph"),
            "resume() must be inherited")
        assertFalse(result.contains("fun reset(): FlowGraph"),
            "reset() must be inherited")
        assertFalse(result.contains("val executionState: StateFlow<ExecutionState>"),
            "executionState must be inherited, not redeclared")
    }

    // ========== Test 2: No boundary nodes → only executionState property ==========

    @Test
    fun `no boundary nodes still extends ModuleController`() {
        val transformer = createTestCodeNode(
            "trans", "DataTransformer", CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", Int::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "output", String::class, "trans"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(transformer))
        val result = generator.generate(flowGraph, generatedPackage)

        assertTrue(result.contains(": ModuleController"),
            "even with no boundary nodes, interface must extend ModuleController")
        assertFalse(result.contains("val input: StateFlow"))
    }

    @Test
    fun `no sink nodes still inherits all control methods via ModuleController`() {
        val gen = createTestCodeNode(
            "gen", "ValueGenerator", CodeNodeType.SOURCE,
            outputPorts = listOf(outputPort("g_out", "value", Int::class, "gen"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(gen))
        val result = generator.generate(flowGraph, generatedPackage)

        // Control methods come from ModuleController, not redeclared here.
        assertTrue(result.contains(": ModuleController"))
        assertFalse(result.contains("fun start(): FlowGraph"))
        assertFalse(result.contains("fun stop(): FlowGraph"))
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
