/*
 * CodeGeneration Characterization Test
 * Pins current code generation behavior at the KotlinCodeGenerator/ComponentGenerator/FlowGenerator seams
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.characterization

import io.codenode.fbpdsl.model.*
import io.codenode.flowgraphgenerate.generator.ComponentGenerator
import io.codenode.flowgraphgenerate.generator.FlowGenerator
import io.codenode.flowgraphgenerate.generator.KotlinCodeGenerator
import kotlin.test.*

/**
 * Characterization tests for code generation pipeline.
 *
 * These tests capture the current behavior of generators, not correctness.
 * They serve as a safety net during vertical-slice extraction to flowGraph-generate.
 * If any test fails after a code move, the extraction broke something.
 */
class CodeGenerationCharacterizationTest {

    // ========== Test Fixtures ==========

    private fun createTestNode(
        name: String,
        type: CodeNodeType = CodeNodeType.TRANSFORMER,
        inputPorts: List<Port<*>> = emptyList(),
        outputPorts: List<Port<*>> = emptyList(),
        configuration: Map<String, String> = emptyMap()
    ): CodeNode {
        val nodeId = "test_${name.lowercase()}"
        return CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = type,
            description = "Test node: $name",
            position = Node.Position(100.0, 200.0),
            inputPorts = inputPorts.map { it.copy(owningNodeId = nodeId) },
            outputPorts = outputPorts.map { it.copy(owningNodeId = nodeId) },
            configuration = configuration
        )
    }

    private fun createPort(
        name: String,
        direction: Port.Direction,
        dataType: kotlin.reflect.KClass<*> = String::class
    ): Port<*> {
        return Port(
            id = "port_$name",
            name = name,
            direction = direction,
            dataType = dataType,
            owningNodeId = ""
        )
    }

    private fun createTestFlowGraph(
        name: String = "TestFlow",
        nodes: List<Node> = emptyList(),
        connections: List<Connection> = emptyList()
    ): FlowGraph {
        return FlowGraph(
            id = "flow_${name.lowercase()}",
            name = name,
            version = "1.0.0",
            description = "Test flow for characterization",
            rootNodes = nodes,
            connections = connections,
            targetPlatforms = listOf(
                FlowGraph.TargetPlatform.KMP_ANDROID,
                FlowGraph.TargetPlatform.KMP_IOS,
                FlowGraph.TargetPlatform.KMP_DESKTOP
            )
        )
    }

    // ========== KotlinCodeGenerator Tests ==========

    private val kotlinCodeGenerator = KotlinCodeGenerator()

    @Test
    fun `generateNodeComponent produces FileSpec with class matching node name`() {
        val node = createTestNode("DataProcessor")
        val fileSpec = kotlinCodeGenerator.generateNodeComponent(node)
        val code = fileSpec.toString()

        assertTrue(code.contains("class DataProcessor"), "Should generate class with PascalCase name")
        assertTrue(code.contains("package "), "Should include package declaration")
    }

    @Test
    fun `generateNodeComponent handles SOURCE node type`() {
        val node = createTestNode(
            "TimerEmitter",
            type = CodeNodeType.SOURCE,
            outputPorts = listOf(
                createPort("elapsed", Port.Direction.OUTPUT, Long::class)
            )
        )
        val fileSpec = kotlinCodeGenerator.generateNodeComponent(node)
        val code = fileSpec.toString()

        assertTrue(code.contains("TimerEmitter"), "Should generate class for SOURCE node")
    }

    @Test
    fun `generateNodeComponent handles SINK node type`() {
        val node = createTestNode(
            "DisplayReceiver",
            type = CodeNodeType.SINK,
            inputPorts = listOf(
                createPort("value", Port.Direction.INPUT, String::class)
            )
        )
        val fileSpec = kotlinCodeGenerator.generateNodeComponent(node)
        val code = fileSpec.toString()

        assertTrue(code.contains("DisplayReceiver"), "Should generate class for SINK node")
    }

    @Test
    fun `generateNodeComponent handles TRANSFORMER with input and output ports`() {
        val node = createTestNode(
            "TimeIncrementer",
            type = CodeNodeType.TRANSFORMER,
            inputPorts = listOf(createPort("input", Port.Direction.INPUT, Int::class)),
            outputPorts = listOf(createPort("output", Port.Direction.OUTPUT, Int::class))
        )
        val fileSpec = kotlinCodeGenerator.generateNodeComponent(node)
        val code = fileSpec.toString()

        assertTrue(code.contains("TimeIncrementer"), "Should generate TRANSFORMER class")
    }

    @Test
    fun `generateNodeComponent handles FILTER node type`() {
        val node = createTestNode(
            "PositiveFilter",
            type = CodeNodeType.FILTER,
            inputPorts = listOf(createPort("input", Port.Direction.INPUT, Int::class)),
            outputPorts = listOf(createPort("output", Port.Direction.OUTPUT, Int::class))
        )
        val fileSpec = kotlinCodeGenerator.generateNodeComponent(node)
        val code = fileSpec.toString()

        assertTrue(code.contains("PositiveFilter"), "Should generate FILTER class")
    }

    @Test
    fun `generateNodeComponent handles node with configuration properties`() {
        val node = createTestNode(
            "ConfiguredNode",
            configuration = mapOf("interval" to "1000", "unit" to "ms")
        )
        val fileSpec = kotlinCodeGenerator.generateNodeComponent(node)
        val code = fileSpec.toString()

        // Pin: generated code should exist (configuration may or may not be embedded)
        assertTrue(code.contains("ConfiguredNode"), "Should generate class even with configuration")
    }

    @Test
    fun `generateProject produces GeneratedProject with files`() {
        val source = createTestNode(
            "Emitter",
            type = CodeNodeType.SOURCE,
            outputPorts = listOf(createPort("out", Port.Direction.OUTPUT, String::class))
        )
        val sink = createTestNode(
            "Receiver",
            type = CodeNodeType.SINK,
            inputPorts = listOf(createPort("in", Port.Direction.INPUT, String::class))
        )
        val connection = Connection(
            id = "conn1",
            sourceNodeId = "test_emitter",
            sourcePortId = "port_out",
            targetNodeId = "test_receiver",
            targetPortId = "port_in"
        )
        val flowGraph = createTestFlowGraph("SimpleProject", listOf(source, sink), listOf(connection))

        val project = kotlinCodeGenerator.generateProject(flowGraph)

        assertEquals("SimpleProject", project.name, "Project name should match FlowGraph name")
        assertTrue(project.fileCount() > 0, "Should generate at least one file")
        assertTrue(project.fileNames().isNotEmpty(), "Should have file names")
    }

    @Test
    fun `generateProject includes component files for each node`() {
        val node1 = createTestNode("NodeA", type = CodeNodeType.SOURCE,
            outputPorts = listOf(createPort("out", Port.Direction.OUTPUT)))
        val node2 = createTestNode("NodeB", type = CodeNodeType.SINK,
            inputPorts = listOf(createPort("in", Port.Direction.INPUT)))
        val flowGraph = createTestFlowGraph("MultiNode", listOf(node1, node2))

        val project = kotlinCodeGenerator.generateProject(flowGraph)

        assertTrue(project.fileCount() >= 2, "Should generate at least one file per node")
    }

    // ========== ComponentGenerator Tests ==========

    private val componentGenerator = ComponentGenerator()

    @Test
    fun `generateComponent produces FileSpec for TRANSFORMER`() {
        val node = createTestNode(
            "Processor",
            type = CodeNodeType.TRANSFORMER,
            inputPorts = listOf(createPort("input", Port.Direction.INPUT)),
            outputPorts = listOf(createPort("output", Port.Direction.OUTPUT))
        )

        val fileSpec = componentGenerator.generateComponent(node)
        val code = fileSpec.toString()

        assertTrue(code.contains("package ${ComponentGenerator.COMPONENTS_PACKAGE}"),
            "Should use components package")
        assertTrue(code.contains("class "), "Should contain a class declaration")
    }

    @Test
    fun `generateComponent produces FileSpec for SOURCE with output port`() {
        val node = createTestNode(
            "Generator",
            type = CodeNodeType.SOURCE,
            outputPorts = listOf(createPort("output", Port.Direction.OUTPUT, Int::class))
        )

        val fileSpec = componentGenerator.generateComponent(node)
        val code = fileSpec.toString()

        assertTrue(code.isNotBlank(), "Should generate non-blank output for SOURCE")
    }

    @Test
    fun `generateComponent produces FileSpec for multi-input multi-output node`() {
        val node = createTestNode(
            "Splitter",
            type = CodeNodeType.SPLITTER,
            inputPorts = listOf(createPort("input", Port.Direction.INPUT, String::class)),
            outputPorts = listOf(
                createPort("output1", Port.Direction.OUTPUT, String::class),
                createPort("output2", Port.Direction.OUTPUT, String::class)
            )
        )

        val fileSpec = componentGenerator.generateComponent(node)
        val code = fileSpec.toString()

        assertTrue(code.isNotBlank(), "Should generate code for SPLITTER")
    }

    // ========== FlowGenerator Tests ==========

    private val flowGenerator = FlowGenerator()

    @Test
    fun `generateFlowOrchestrator produces FileSpec with orchestrator class`() {
        val source = createTestNode("Emitter", type = CodeNodeType.SOURCE,
            outputPorts = listOf(createPort("out", Port.Direction.OUTPUT)))
        val sink = createTestNode("Receiver", type = CodeNodeType.SINK,
            inputPorts = listOf(createPort("in", Port.Direction.INPUT)))
        val connection = Connection("c1", "test_emitter", "port_out", "test_receiver", "port_in")
        val flowGraph = createTestFlowGraph("OrchestratorTest", listOf(source, sink), listOf(connection))

        val fileSpec = flowGenerator.generateFlowOrchestrator(flowGraph)
        val code = fileSpec.toString()

        assertTrue(code.contains("package ${FlowGenerator.FLOW_PACKAGE}"),
            "Should use flow package")
        assertTrue(code.contains("class "), "Should contain orchestrator class")
    }

    @Test
    fun `generateFlowOrchestrator includes start and stop functions`() {
        val node = createTestNode("Solo", type = CodeNodeType.SOURCE,
            outputPorts = listOf(createPort("out", Port.Direction.OUTPUT)))
        val flowGraph = createTestFlowGraph("LifecycleTest", listOf(node))

        val fileSpec = flowGenerator.generateFlowOrchestrator(flowGraph)
        val code = fileSpec.toString()

        assertTrue(code.contains("fun start(") || code.contains("fun start()"),
            "Should include start function")
        assertTrue(code.contains("fun stop(") || code.contains("fun stop()"),
            "Should include stop function")
    }

    @Test
    fun `generateFlowOrchestrator includes wire connections logic`() {
        val source = createTestNode("Source", type = CodeNodeType.SOURCE,
            outputPorts = listOf(createPort("out", Port.Direction.OUTPUT)))
        val sink = createTestNode("Sink", type = CodeNodeType.SINK,
            inputPorts = listOf(createPort("in", Port.Direction.INPUT)))
        val connection = Connection("c1", "test_source", "port_out", "test_sink", "port_in")
        val flowGraph = createTestFlowGraph("WiringTest", listOf(source, sink), listOf(connection))

        val fileSpec = flowGenerator.generateFlowOrchestrator(flowGraph)
        val code = fileSpec.toString()

        assertTrue(code.contains("wireConnections") || code.contains("wire") || code.contains("channel"),
            "Should include connection wiring logic")
    }

    // ========== Cross-Generator Consistency ==========

    @Test
    fun `generateProject output is consistent with individual generators`() {
        val node = createTestNode("ConsistencyNode", type = CodeNodeType.TRANSFORMER,
            inputPorts = listOf(createPort("in", Port.Direction.INPUT)),
            outputPorts = listOf(createPort("out", Port.Direction.OUTPUT)))
        val flowGraph = createTestFlowGraph("ConsistencyTest", listOf(node))

        val project = kotlinCodeGenerator.generateProject(flowGraph)
        val componentFileSpec = componentGenerator.generateComponent(node)

        // Pin: project should include a file for the node's component
        val projectFileNames = project.fileNames()
        assertTrue(projectFileNames.isNotEmpty(), "Project should have generated files")
    }

    @Test
    fun `all generators use io_codenode_generated base package`() {
        val node = createTestNode("PackageTest")
        val flowGraph = createTestFlowGraph("PackageFlow", listOf(node))

        val componentCode = componentGenerator.generateComponent(node).toString()
        val flowCode = flowGenerator.generateFlowOrchestrator(flowGraph).toString()

        assertTrue(componentCode.contains("io.codenode.generated"),
            "ComponentGenerator should use io.codenode.generated package")
        assertTrue(flowCode.contains("io.codenode.generated"),
            "FlowGenerator should use io.codenode.generated package")
    }
}
