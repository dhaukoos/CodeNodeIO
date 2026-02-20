/*
 * FlowGraphFactoryGenerator Test
 * Tests for generating factory functions that create NodeRuntime instances with tick stubs
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * Tests for FlowGraphFactoryGenerator - generates factory functions using tick stubs.
 *
 * Tests verify that generated factory code:
 * - Imports tick functions from logicmethods package
 * - Calls the correct CodeNodeFactory.createTimed* factory methods
 * - Passes tick function references as the tick parameter
 * - No longer references ProcessingLogic or _useCaseClass
 */
class FlowGraphFactoryGeneratorTest {

    // ========== Test Fixtures ==========

    private fun createTestCodeNode(
        id: String,
        name: String,
        type: CodeNodeType = CodeNodeType.TRANSFORMER,
        inputPorts: List<Port<*>> = listOf(
            Port(
                id = "${id}_input",
                name = "input",
                direction = Port.Direction.INPUT,
                dataType = String::class,
                owningNodeId = id
            )
        ),
        outputPorts: List<Port<*>> = listOf(
            Port(
                id = "${id}_output",
                name = "output",
                direction = Port.Direction.OUTPUT,
                dataType = String::class,
                owningNodeId = id
            )
        ),
        configuration: Map<String, String> = emptyMap()
    ): CodeNode {
        return CodeNode(
            id = id,
            name = name,
            codeNodeType = type,
            position = Node.Position(100.0, 200.0),
            inputPorts = inputPorts,
            outputPorts = outputPorts,
            configuration = configuration
        )
    }

    private fun createTestFlowGraph(
        name: String = "TestFlow",
        nodes: List<Node> = listOf(
            createTestCodeNode(
                "timer", "TimerEmitter", CodeNodeType.GENERATOR,
                inputPorts = emptyList(),
                outputPorts = listOf(
                    Port(
                        id = "timer_sec", name = "elapsedSeconds",
                        direction = Port.Direction.OUTPUT, dataType = Int::class,
                        owningNodeId = "timer"
                    ),
                    Port(
                        id = "timer_min", name = "elapsedMinutes",
                        direction = Port.Direction.OUTPUT, dataType = Int::class,
                        owningNodeId = "timer"
                    )
                )
            ),
            createTestCodeNode(
                "display", "DisplayReceiver", CodeNodeType.SINK,
                inputPorts = listOf(
                    Port(
                        id = "display_sec", name = "seconds",
                        direction = Port.Direction.INPUT, dataType = Int::class,
                        owningNodeId = "display"
                    ),
                    Port(
                        id = "display_min", name = "minutes",
                        direction = Port.Direction.INPUT, dataType = Int::class,
                        owningNodeId = "display"
                    )
                ),
                outputPorts = emptyList()
            )
        ),
        connections: List<Connection> = listOf(
            Connection(
                id = "conn_1",
                sourceNodeId = "timer",
                sourcePortId = "timer_sec",
                targetNodeId = "display",
                targetPortId = "display_sec"
            )
        )
    ): FlowGraph {
        return FlowGraph(
            id = "flow_${name.lowercase()}",
            name = name,
            version = "1.0.0",
            description = "Test flow for factory generation",
            rootNodes = nodes,
            connections = connections
        )
    }

    // ========== Factory Function Structure ==========

    @Test
    fun `generateFactory produces non-empty output`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        val result = generator.generateFactory(flowGraph, "io.codenode.generated.stopwatch")

        assertTrue(result.isNotBlank(), "Generated factory should not be blank")
    }

    @Test
    fun `generateFactory creates factory function with correct name`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        val result = generator.generateFactory(flowGraph, "io.codenode.generated.stopwatch")

        assertTrue(result.contains("fun createStopWatchFlowGraph"),
            "Should create factory function with graph name")
    }

    @Test
    fun `generateFactory includes package declaration`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val packageName = "io.codenode.generated.stopwatch"
        val generator = FlowGraphFactoryGenerator()

        val result = generator.generateFactory(flowGraph, packageName)

        assertTrue(result.contains("package $packageName"),
            "Should include correct package declaration")
    }

    @Test
    fun `generateFactory includes CodeNodeFactory import`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        val result = generator.generateFactory(flowGraph, "io.codenode.generated.stopwatch")

        assertTrue(result.contains("import io.codenode.fbpdsl.model.CodeNodeFactory"),
            "Should import CodeNodeFactory")
    }

    @Test
    fun `factory function has KDoc documentation`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        val result = generator.generateFactory(flowGraph, "io.codenode.generated.stopwatch")

        assertTrue(result.contains("/**") && result.contains("*/"),
            "Should include KDoc documentation")
        assertTrue(result.contains("@generated"),
            "Should include @generated tag")
    }

    // ========== Tick Function Imports ==========

    @Test
    fun `factory imports tick functions from logicmethods`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        val result = generator.generateFactory(flowGraph, "io.codenode.generated.stopwatch")

        assertTrue(result.contains("import io.codenode.generated.stopwatch.logicmethods.timerEmitterTick"),
            "Should import timerEmitterTick from logicmethods")
        assertTrue(result.contains("import io.codenode.generated.stopwatch.logicmethods.displayReceiverTick"),
            "Should import displayReceiverTick from logicmethods")
    }

    @Test
    fun `factory uses separate usecasesPackage for tick imports`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        val result = generator.generateFactory(flowGraph, "io.codenode.generated.stopwatch", "io.codenode.stopwatch")

        assertTrue(result.contains("import io.codenode.stopwatch.logicmethods.timerEmitterTick"),
            "Should import from usecasesPackage.logicmethods")
    }

    // ========== Factory Method Calls ==========

    @Test
    fun `factory calls createTimedOut2Generator for 0-in-2-out node`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        val result = generator.generateFactory(flowGraph, "io.codenode.generated.stopwatch")

        assertTrue(result.contains("CodeNodeFactory.createTimedOut2Generator<Int, Int>"),
            "Should call createTimedOut2Generator for 0-in-2-out generator")
    }

    @Test
    fun `factory calls createTimedIn2Sink for 2-in-0-out node`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        val result = generator.generateFactory(flowGraph, "io.codenode.generated.stopwatch")

        assertTrue(result.contains("CodeNodeFactory.createTimedIn2Sink<Int, Int>"),
            "Should call createTimedIn2Sink for 2-in-0-out sink")
    }

    @Test
    fun `factory passes tick function reference`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        val result = generator.generateFactory(flowGraph, "io.codenode.generated.stopwatch")

        assertTrue(result.contains("tick = timerEmitterTick"),
            "Should pass timerEmitterTick as tick parameter")
        assertTrue(result.contains("tick = displayReceiverTick"),
            "Should pass displayReceiverTick as tick parameter")
    }

    @Test
    fun `factory includes node name parameter`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        val result = generator.generateFactory(flowGraph, "io.codenode.generated.stopwatch")

        assertTrue(result.contains("name = \"TimerEmitter\""),
            "Should include node name for TimerEmitter")
        assertTrue(result.contains("name = \"DisplayReceiver\""),
            "Should include node name for DisplayReceiver")
    }

    @Test
    fun `factory includes tickIntervalMs parameter`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        val result = generator.generateFactory(flowGraph, "io.codenode.generated.stopwatch")

        assertTrue(result.contains("tickIntervalMs = "),
            "Should include tickIntervalMs parameter")
    }

    @Test
    fun `factory uses tickIntervalMs from node configuration`() {
        val node = createTestCodeNode(
            "gen", "FastGenerator", CodeNodeType.GENERATOR,
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "gen_out", name = "value",
                    direction = Port.Direction.OUTPUT, dataType = Int::class,
                    owningNodeId = "gen"
                )
            ),
            configuration = mapOf("tickIntervalMs" to "500")
        )
        val flowGraph = FlowGraph(
            id = "flow_1", name = "Fast", version = "1.0.0",
            rootNodes = listOf(node)
        )
        val generator = FlowGraphFactoryGenerator()

        val result = generator.generateFactory(flowGraph, "io.codenode.generated")

        assertTrue(result.contains("tickIntervalMs = 500"),
            "Should use tickIntervalMs from node configuration")
    }

    // ========== No ProcessingLogic References ==========

    @Test
    fun `factory does not reference ProcessingLogic`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        val result = generator.generateFactory(flowGraph, "io.codenode.generated.stopwatch")

        assertFalse(result.contains("ProcessingLogic"),
            "Should not reference ProcessingLogic")
        assertFalse(result.contains("processingLogic"),
            "Should not contain processingLogic property")
    }

    @Test
    fun `factory does not reference _useCaseClass`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        val result = generator.generateFactory(flowGraph, "io.codenode.generated.stopwatch")

        assertFalse(result.contains("_useCaseClass"),
            "Should not reference _useCaseClass config key")
    }

    @Test
    fun `factory does not instantiate Component classes`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        val result = generator.generateFactory(flowGraph, "io.codenode.generated.stopwatch")

        assertFalse(result.contains("Component()"),
            "Should not instantiate Component classes")
    }

    // ========== Factory Method Selection ==========

    @Test
    fun `getFactoryMethodName returns correct method for each node type`() {
        val generator = FlowGraphFactoryGenerator()

        // Generator (0 in, 1 out)
        val gen1 = createTestCodeNode("g", "Gen",
            inputPorts = emptyList(),
            outputPorts = listOf(Port(
                id = "o", name = "out",
                direction = Port.Direction.OUTPUT, dataType = Int::class,
                owningNodeId = "g"
            )))
        assertEquals("createTimedGenerator", generator.getFactoryMethodName(gen1))

        // Sink (1 in, 0 out)
        val sink1 = createTestCodeNode("s", "Sink",
            inputPorts = listOf(Port(
                id = "i", name = "in",
                direction = Port.Direction.INPUT, dataType = Int::class,
                owningNodeId = "s"
            )),
            outputPorts = emptyList())
        assertEquals("createTimedSink", generator.getFactoryMethodName(sink1))

        // Filter (1 in, 1 out, same type)
        val filter = createTestCodeNode("f", "Filter",
            inputPorts = listOf(Port(
                id = "i", name = "in",
                direction = Port.Direction.INPUT, dataType = Int::class,
                owningNodeId = "f"
            )),
            outputPorts = listOf(Port(
                id = "o", name = "out",
                direction = Port.Direction.OUTPUT, dataType = Int::class,
                owningNodeId = "f"
            )))
        assertEquals("createTimedFilter", generator.getFactoryMethodName(filter))

        // Transformer (1 in, 1 out, different types)
        val trans = createTestCodeNode("t", "Trans",
            inputPorts = listOf(Port(
                id = "i", name = "in",
                direction = Port.Direction.INPUT, dataType = String::class,
                owningNodeId = "t"
            )),
            outputPorts = listOf(Port(
                id = "o", name = "out",
                direction = Port.Direction.OUTPUT, dataType = Int::class,
                owningNodeId = "t"
            )))
        assertEquals("createTimedTransformer", generator.getFactoryMethodName(trans))
    }

    // ========== Validation ==========

    @Test
    fun `getRequiredComponents returns tick stub file names`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        val required = generator.getRequiredComponents(flowGraph)

        assertTrue(required.contains("TimerEmitterProcessLogic"),
            "Should list TimerEmitterProcessLogic as required")
        assertTrue(required.contains("DisplayReceiverProcessLogic"),
            "Should list DisplayReceiverProcessLogic as required")
    }

    @Test
    fun `validateComponents returns success when all stubs exist`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()
        val existingFiles = setOf(
            "TimerEmitterProcessLogic.kt",
            "DisplayReceiverProcessLogic.kt"
        )

        val result = generator.validateComponents(flowGraph, existingFiles)

        assertTrue(result.isValid, "Validation should pass when all stubs exist")
        assertTrue(result.missingComponents.isEmpty(), "No missing stubs expected")
    }

    @Test
    fun `validateComponents returns failure with missing stubs`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()
        val existingFiles = setOf(
            "TimerEmitterProcessLogic.kt"
            // DisplayReceiverProcessLogic.kt is missing
        )

        val result = generator.validateComponents(flowGraph, existingFiles)

        assertFalse(result.isValid, "Validation should fail when stubs are missing")
        assertTrue(result.missingComponents.contains("DisplayReceiverProcessLogic"),
            "Should report DisplayReceiverProcessLogic as missing")
    }

    // ========== Additional Tests ==========

    @Test
    fun `factory file name follows convention`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        val fileName = generator.getFactoryFileName(flowGraph)

        assertEquals("StopWatchFactory.kt", fileName,
            "Factory file should be named {GraphName}Factory.kt")
    }

    @Test
    fun `factory handles multiple nodes`() {
        val nodes = listOf(
            createTestCodeNode("gen", "Generator", CodeNodeType.GENERATOR,
                inputPorts = emptyList(),
                outputPorts = listOf(Port(
                    id = "g_out", name = "value",
                    direction = Port.Direction.OUTPUT, dataType = Int::class,
                    owningNodeId = "gen"
                ))),
            createTestCodeNode("proc", "Processor",
                inputPorts = listOf(Port(
                    id = "p_in", name = "input",
                    direction = Port.Direction.INPUT, dataType = Int::class,
                    owningNodeId = "proc"
                )),
                outputPorts = listOf(Port(
                    id = "p_out", name = "output",
                    direction = Port.Direction.OUTPUT, dataType = String::class,
                    owningNodeId = "proc"
                ))),
            createTestCodeNode("sink", "Receiver", CodeNodeType.SINK,
                inputPorts = listOf(Port(
                    id = "s_in", name = "data",
                    direction = Port.Direction.INPUT, dataType = String::class,
                    owningNodeId = "sink"
                )),
                outputPorts = emptyList())
        )
        val flowGraph = FlowGraph(
            id = "flow_1", name = "Pipeline", version = "1.0.0", rootNodes = nodes
        )
        val generator = FlowGraphFactoryGenerator()

        val result = generator.generateFactory(flowGraph, "io.codenode.generated.pipeline")

        assertTrue(result.contains("createTimedGenerator<Int>"), "Should create generator")
        assertTrue(result.contains("createTimedTransformer<Int, String>"), "Should create transformer")
        assertTrue(result.contains("createTimedSink<String>"), "Should create sink")
        assertTrue(result.contains("tick = generatorTick"), "Should reference generatorTick")
        assertTrue(result.contains("tick = processorTick"), "Should reference processorTick")
        assertTrue(result.contains("tick = receiverTick"), "Should reference receiverTick")
    }
}
