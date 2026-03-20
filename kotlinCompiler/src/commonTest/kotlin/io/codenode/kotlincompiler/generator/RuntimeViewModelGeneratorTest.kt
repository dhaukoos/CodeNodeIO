/*
 * RuntimeViewModelGenerator Test
 * Tests for generating {Name}ViewModel.kt stub with Module State object from FlowGraph
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * Tests for RuntimeViewModelGenerator - generates {Name}ViewModel.kt stub
 * containing a marker-delineated {ModuleName}State object and a
 * {ModuleName}ViewModel class.
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
    private val basePackage = "io.codenode.testapp"
    private val generatedPackage = "io.codenode.testapp.generated"

    // ========== Package and Imports ==========

    @Test
    fun `generates package declaration with base package`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("package io.codenode.testapp"))
    }

    @Test
    fun `generates required imports`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("import androidx.lifecycle.ViewModel"))
        assertTrue(result.contains("import io.codenode.fbpdsl.model.ExecutionState"))
        assertTrue(result.contains("import io.codenode.fbpdsl.model.FlowGraph"))
        assertTrue(result.contains("import kotlinx.coroutines.flow.StateFlow"))
    }

    @Test
    fun `generates MutableStateFlow imports when observable state exists`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("import kotlinx.coroutines.flow.MutableStateFlow"))
        assertTrue(result.contains("import kotlinx.coroutines.flow.asStateFlow"))
    }

    @Test
    fun `generates ControllerInterface import from generated package`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("import io.codenode.testapp.generated.StopWatch2ControllerInterface"))
    }

    @Test
    fun `no boundary nodes omits MutableStateFlow imports`() {
        val transformer = createTestCodeNode(
            "trans", "DataTransformer", CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", Int::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "output", String::class, "trans"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(transformer))
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertFalse(result.contains("import kotlinx.coroutines.flow.MutableStateFlow"))
        assertFalse(result.contains("import kotlinx.coroutines.flow.asStateFlow"))
        assertTrue(result.contains("import kotlinx.coroutines.flow.StateFlow"))
    }

    @Test
    fun `source-only graph generates MutableStateFlow imports`() {
        val gen = createTestCodeNode(
            "gen", "ValueGenerator", CodeNodeType.SOURCE,
            outputPorts = listOf(outputPort("g_out", "value", Int::class, "gen"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(gen))
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("import kotlinx.coroutines.flow.MutableStateFlow"))
        assertTrue(result.contains("import kotlinx.coroutines.flow.asStateFlow"))
    }

    // ========== Module Properties Section (State Object) ==========

    @Test
    fun `generates MODULE PROPERTIES START marker`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("// ===== MODULE PROPERTIES START ====="))
    }

    @Test
    fun `generates MODULE PROPERTIES END marker`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("// ===== MODULE PROPERTIES END ====="))
    }

    @Test
    fun `generates State object with flow name`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("object StopWatch2State {"))
    }

    @Test
    fun `generates internal MutableStateFlow for sink input ports`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("internal val _seconds = MutableStateFlow(0)"))
        assertTrue(result.contains("internal val _minutes = MutableStateFlow(0)"))
    }

    @Test
    fun `generates public StateFlow accessors for sink input ports`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("val secondsFlow: StateFlow<Int> = _seconds.asStateFlow()"))
        assertTrue(result.contains("val minutesFlow: StateFlow<Int> = _minutes.asStateFlow()"))
    }

    @Test
    fun `generates reset method in State object`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("fun reset()"))
        assertTrue(result.contains("_seconds.value = 0"))
        assertTrue(result.contains("_minutes.value = 0"))
    }

    @Test
    fun `no boundary nodes generates empty State object`() {
        val transformer = createTestCodeNode(
            "trans", "DataTransformer", CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", Int::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "output", String::class, "trans"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(transformer))
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("object TestFlowState {"))
        assertFalse(result.contains("MutableStateFlow("))
    }

    @Test
    fun `source-only graph generates MutableStateFlow for output ports`() {
        val gen = createTestCodeNode(
            "gen", "ValueGenerator", CodeNodeType.SOURCE,
            outputPorts = listOf(outputPort("g_out", "value", Int::class, "gen"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(gen))
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("object TestFlowState {"))
        assertTrue(result.contains("internal val _value = MutableStateFlow(0)"))
        assertTrue(result.contains("val valueFlow: StateFlow<Int> = _value.asStateFlow()"))
    }

    // ========== ViewModel Class ==========

    @Test
    fun `generates ViewModel class extending ViewModel`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("class StopWatch2ViewModel("))
        assertTrue(result.contains(") : ViewModel()"))
    }

    @Test
    fun `generates ControllerInterface constructor param`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("private val controller: StopWatch2ControllerInterface"))
    }

    @Test
    fun `delegates observable state from State object`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("val seconds: StateFlow<Int> = StopWatch2State.secondsFlow"))
        assertTrue(result.contains("val minutes: StateFlow<Int> = StopWatch2State.minutesFlow"))
    }

    @Test
    fun `delegates executionState from controller`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("val executionState: StateFlow<ExecutionState> = controller.executionState"))
    }

    @Test
    fun `delegates control methods`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("fun start(): FlowGraph = controller.start()"))
        assertTrue(result.contains("fun stop(): FlowGraph = controller.stop()"))
        assertTrue(result.contains("fun reset(): FlowGraph = controller.reset()"))
        assertTrue(result.contains("fun pause(): FlowGraph = controller.pause()"))
        assertTrue(result.contains("fun resume(): FlowGraph = controller.resume()"))
    }

    @Test
    fun `no boundary nodes still delegates all control methods`() {
        val transformer = createTestCodeNode(
            "trans", "DataTransformer", CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", Int::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "output", String::class, "trans"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(transformer))
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("fun start(): FlowGraph = controller.start()"))
        assertTrue(result.contains("fun stop(): FlowGraph = controller.stop()"))
        assertTrue(result.contains("fun reset(): FlowGraph = controller.reset()"))
        assertTrue(result.contains("fun pause(): FlowGraph = controller.pause()"))
        assertTrue(result.contains("fun resume(): FlowGraph = controller.resume()"))
    }

    @Test
    fun `no boundary nodes omits observable state delegation in ViewModel`() {
        val transformer = createTestCodeNode(
            "trans", "DataTransformer", CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", Int::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "output", String::class, "trans"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(transformer))
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertFalse(result.contains("// Observable state from module properties"))
        assertTrue(result.contains("val executionState: StateFlow<ExecutionState> = controller.executionState"))
    }

    @Test
    fun `source-only graph delegates observable state in ViewModel`() {
        val gen = createTestCodeNode(
            "gen", "ValueGenerator", CodeNodeType.SOURCE,
            outputPorts = listOf(outputPort("g_out", "value", Int::class, "gen"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(gen))
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("val value: StateFlow<Int> = TestFlowState.valueFlow"))
    }

    @Test
    fun `StopWatch-like flow generates state for both source and sink ports`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        // Source output ports
        assertTrue(result.contains("val elapsedSeconds: StateFlow<Int> = StopWatch2State.elapsedSecondsFlow"))
        assertTrue(result.contains("val elapsedMinutes: StateFlow<Int> = StopWatch2State.elapsedMinutesFlow"))
        // Sink input ports
        assertTrue(result.contains("val seconds: StateFlow<Int> = StopWatch2State.secondsFlow"))
        assertTrue(result.contains("val minutes: StateFlow<Int> = StopWatch2State.minutesFlow"))
    }

    // ========== generateModulePropertiesSection (Selective Regeneration) ==========

    @Test
    fun `generateModulePropertiesSection returns section with markers`() {
        val flowGraph = createStopWatchLikeFlow()
        val section = generator.generateModulePropertiesSection(flowGraph)

        assertTrue(section.startsWith("// ===== MODULE PROPERTIES START ====="))
        assertTrue(section.contains("// ===== MODULE PROPERTIES END ====="))
    }

    @Test
    fun `generateModulePropertiesSection contains State object`() {
        val flowGraph = createStopWatchLikeFlow()
        val section = generator.generateModulePropertiesSection(flowGraph)

        assertTrue(section.contains("object StopWatch2State {"))
        assertTrue(section.contains("internal val _seconds = MutableStateFlow(0)"))
        assertTrue(section.contains("val secondsFlow: StateFlow<Int> = _seconds.asStateFlow()"))
        assertTrue(section.contains("internal val _minutes = MutableStateFlow(0)"))
        assertTrue(section.contains("val minutesFlow: StateFlow<Int> = _minutes.asStateFlow()"))
    }

    @Test
    fun `generateModulePropertiesSection contains reset method`() {
        val flowGraph = createStopWatchLikeFlow()
        val section = generator.generateModulePropertiesSection(flowGraph)

        assertTrue(section.contains("fun reset()"))
        assertTrue(section.contains("_seconds.value = 0"))
        assertTrue(section.contains("_minutes.value = 0"))
    }

    @Test
    fun `generateModulePropertiesSection with no boundary nodes generates empty State object`() {
        val transformer = createTestCodeNode(
            "trans", "DataTransformer", CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", Int::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "output", String::class, "trans"))
        )
        val flowGraph = createFlowGraph(nodes = listOf(transformer))
        val section = generator.generateModulePropertiesSection(flowGraph)

        assertTrue(section.contains("object TestFlowState {"))
        assertTrue(section.contains("}"))
        assertFalse(section.contains("MutableStateFlow("))
    }

    // ========== ViewModel Section Markers ==========

    @Test
    fun `generates ViewModel section comment block`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("// ViewModel"))
        assertTrue(result.contains("// Binding interface between composable UI and FlowGraph."))
        assertTrue(result.contains("// User-editable section below"))
    }

    // ========== Entity Module Detection Tests ==========

    @Test
    fun `entity module generates DAO constructor parameter`() {
        val flowGraph = createEntityModuleFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("geoLocationDao: GeoLocationDao"))
    }

    @Test
    fun `entity module generates addEntity method`() {
        val flowGraph = createEntityModuleFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("fun addEntity(geoLocation: GeoLocationEntity)"))
    }

    @Test
    fun `entity module generates updateEntity method`() {
        val flowGraph = createEntityModuleFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("fun updateEntity(geoLocation: GeoLocationEntity)"))
    }

    @Test
    fun `entity module generates removeEntity method`() {
        val flowGraph = createEntityModuleFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("fun removeEntity(geoLocation: GeoLocationEntity)"))
    }

    @Test
    fun `entity module generates repository observation in init block`() {
        val flowGraph = createEntityModuleFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("init {"))
        assertTrue(result.contains("GeoLocationRepository(geoLocationDao)"))
        assertTrue(result.contains("repo.observeAll().collect"))
    }

    @Test
    fun `entity module generates persistence imports`() {
        val flowGraph = createEntityModuleFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("import io.codenode.persistence.GeoLocationDao"))
        assertTrue(result.contains("import io.codenode.persistence.GeoLocationEntity"))
        assertTrue(result.contains("import io.codenode.persistence.GeoLocationRepository"))
    }

    @Test
    fun `entity module generates viewModelScope import`() {
        val flowGraph = createEntityModuleFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("import androidx.lifecycle.viewModelScope"))
        assertTrue(result.contains("import kotlinx.coroutines.launch"))
    }

    @Test
    fun `entity module generates profiles state in module properties`() {
        val flowGraph = createEntityModuleFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("internal val _geoLocations = MutableStateFlow<List<GeoLocationEntity>>(emptyList())"))
    }

    @Test
    fun `entity module generates geoLocations property in ViewModel`() {
        val flowGraph = createEntityModuleFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertTrue(result.contains("val geoLocations: StateFlow<List<GeoLocationEntity>>"))
    }

    @Test
    fun `non-entity flow does not generate CRUD methods`() {
        val flowGraph = createStopWatchLikeFlow()
        val result = generator.generate(flowGraph, basePackage, generatedPackage)

        assertFalse(result.contains("fun addEntity"))
        assertFalse(result.contains("fun updateEntity"))
        assertFalse(result.contains("fun removeEntity"))
    }

    // ========== Helper: Entity Module FlowGraph ==========

    private fun createEntityModuleFlow(): FlowGraph {
        val cudNode = CodeNode(
            id = "cud",
            name = "GeoLocationCUD",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(100.0, 400.0),
            outputPorts = listOf(
                outputPort("cud_save", "save", Any::class, "cud"),
                outputPort("cud_update", "update", Any::class, "cud"),
                outputPort("cud_remove", "remove", Any::class, "cud")
            ),
            configuration = mapOf(
                "_cudSource" to "true",
                "_sourceIPTypeId" to "ip_test",
                "_sourceIPTypeName" to "GeoLocation"
            )
        )
        val repoNode = CodeNode(
            id = "repo",
            name = "GeoLocationRepository",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(400.0, 400.0),
            inputPorts = listOf(
                inputPort("repo_save", "save", Any::class, "repo"),
                inputPort("repo_update", "update", Any::class, "repo"),
                inputPort("repo_remove", "remove", Any::class, "repo")
            ),
            outputPorts = listOf(
                outputPort("repo_result", "result", Any::class, "repo"),
                outputPort("repo_error", "error", Any::class, "repo")
            ),
            configuration = mapOf(
                "_repository" to "true",
                "_sourceIPTypeId" to "ip_test",
                "_sourceIPTypeName" to "GeoLocation"
            )
        )
        val displayNode = CodeNode(
            id = "display",
            name = "GeoLocationsDisplay",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(700.0, 400.0),
            inputPorts = listOf(
                inputPort("disp_result", "result", Any::class, "display"),
                inputPort("disp_error", "error", Any::class, "display")
            ),
            configuration = mapOf(
                "_display" to "true",
                "_sourceIPTypeId" to "ip_test",
                "_sourceIPTypeName" to "GeoLocation"
            )
        )
        return createFlowGraph(
            name = "GeoLocations",
            nodes = listOf(cudNode, repoNode, displayNode)
        )
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
