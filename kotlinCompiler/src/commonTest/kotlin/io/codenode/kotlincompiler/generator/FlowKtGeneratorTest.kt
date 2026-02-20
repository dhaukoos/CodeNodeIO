/*
 * FlowKtGenerator Test
 * TDD tests for generating .flow.kt compiled Kotlin files from FlowGraph
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * TDD tests for FlowKtGenerator - generates .flow.kt compiled Kotlin files.
 *
 * These tests are written FIRST and should FAIL until FlowKtGenerator is implemented.
 *
 * T011: Test for generating valid Kotlin syntax
 * T012: Test for generating package declaration
 * T013: Test for generating codeNode DSL blocks with all properties
 * T014: Test for generating connection DSL statements
 */
class FlowKtGeneratorTest {

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
        nodes: List<Node> = listOf(createTestCodeNode("node1", "Processor")),
        connections: List<Connection> = emptyList()
    ): FlowGraph {
        return FlowGraph(
            id = "flow_${name.lowercase()}",
            name = name,
            version = "1.0.0",
            description = "Test flow for .flow.kt generation",
            rootNodes = nodes,
            connections = connections,
            targetPlatforms = listOf(
                FlowGraph.TargetPlatform.KMP_ANDROID,
                FlowGraph.TargetPlatform.KMP_IOS,
                FlowGraph.TargetPlatform.KMP_DESKTOP
            )
        )
    }

    // ========== T011: Valid Kotlin Syntax ==========

    @Test
    fun `T011 - generateFlowKt produces non-empty output`() {
        // Given
        val flowGraph = createTestFlowGraph("SimpleFlow")
        val generator = FlowKtGenerator()

        // When
        val result = generator.generateFlowKt(flowGraph, "io.codenode.generated.simpleflow")

        // Then
        assertTrue(result.isNotBlank(), "Generated .flow.kt should not be blank")
    }

    @Test
    fun `T011 - generateFlowKt produces valid Kotlin structure`() {
        // Given
        val flowGraph = createTestFlowGraph("ValidFlow")
        val generator = FlowKtGenerator()

        // When
        val result = generator.generateFlowKt(flowGraph, "io.codenode.generated.validflow")

        // Then
        // Should have basic Kotlin file structure
        assertTrue(result.contains("package "), "Should contain package declaration")
        assertTrue(result.contains("import "), "Should contain imports")
    }

    @Test
    fun `T011 - generateFlowKt includes flowGraph DSL call`() {
        // Given
        val flowGraph = createTestFlowGraph("DslFlow")
        val generator = FlowKtGenerator()

        // When
        val result = generator.generateFlowKt(flowGraph, "io.codenode.generated.dslflow")

        // Then
        assertTrue(result.contains("flowGraph(") || result.contains("val ") && result.contains("Graph"),
            "Should contain flowGraph DSL or graph variable declaration")
    }

    // ========== T012: Package Declaration ==========

    @Test
    fun `T012 - generateFlowKt includes correct package declaration`() {
        // Given
        val flowGraph = createTestFlowGraph("PackageTest")
        val packageName = "io.codenode.generated.packagetest"
        val generator = FlowKtGenerator()

        // When
        val result = generator.generateFlowKt(flowGraph, packageName)

        // Then
        assertTrue(result.contains("package $packageName"),
            "Should contain exact package declaration")
    }

    @Test
    fun `T012 - package declaration is at the start of file`() {
        // Given
        val flowGraph = createTestFlowGraph("PackagePosition")
        val packageName = "io.codenode.test"
        val generator = FlowKtGenerator()

        // When
        val result = generator.generateFlowKt(flowGraph, packageName)

        // Then
        val lines = result.lines().filter { it.isNotBlank() && !it.trim().startsWith("/*") && !it.trim().startsWith("*") }
        val firstNonCommentLine = lines.firstOrNull { !it.trim().startsWith("//") }
        assertTrue(firstNonCommentLine?.trim()?.startsWith("package ") == true,
            "Package declaration should be first non-comment line")
    }

    // ========== T013: CodeNode DSL Blocks ==========

    @Test
    fun `T013 - generateFlowKt includes codeNode DSL block`() {
        // Given
        val node = createTestCodeNode("timer", "TimerEmitter", CodeNodeType.GENERATOR)
        val flowGraph = createTestFlowGraph("NodeTest", listOf(node))
        val generator = FlowKtGenerator()

        // When
        val result = generator.generateFlowKt(flowGraph, "io.codenode.generated.nodetest")

        // Then
        assertTrue(result.contains("codeNode(") || result.contains("CodeNode("),
            "Should contain codeNode DSL block or CodeNode declaration")
    }

    @Test
    fun `T013 - codeNode includes node name`() {
        // Given
        val node = createTestCodeNode("proc", "DataProcessor")
        val flowGraph = createTestFlowGraph("NameTest", listOf(node))
        val generator = FlowKtGenerator()

        // When
        val result = generator.generateFlowKt(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(result.contains("DataProcessor") || result.contains("dataProcessor"),
            "Should include node name 'DataProcessor'")
    }

    @Test
    fun `T013 - codeNode includes position`() {
        // Given
        val node = createTestCodeNode("pos", "PositionNode").copy(
            position = Node.Position(150.0, 250.0)
        )
        val flowGraph = createTestFlowGraph("PosTest", listOf(node))
        val generator = FlowKtGenerator()

        // When
        val result = generator.generateFlowKt(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(result.contains("150") && result.contains("250"),
            "Should include position coordinates")
    }

    @Test
    fun `T013 - codeNode includes input ports`() {
        // Given
        val node = createTestCodeNode(
            id = "input-test",
            name = "InputNode",
            inputPorts = listOf(
                Port(
                    id = "port_data",
                    name = "data",
                    direction = Port.Direction.INPUT,
                    dataType = Int::class,
                    owningNodeId = "input-test"
                )
            )
        )
        val flowGraph = createTestFlowGraph("InputPortTest", listOf(node))
        val generator = FlowKtGenerator()

        // When
        val result = generator.generateFlowKt(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(result.contains("input") || result.contains("Input"),
            "Should reference input port")
        assertTrue(result.contains("data"),
            "Should include port name 'data'")
    }

    @Test
    fun `T013 - codeNode includes output ports`() {
        // Given
        val node = createTestCodeNode(
            id = "output-test",
            name = "OutputNode",
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "port_result",
                    name = "result",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = "output-test"
                )
            )
        )
        val flowGraph = createTestFlowGraph("OutputPortTest", listOf(node))
        val generator = FlowKtGenerator()

        // When
        val result = generator.generateFlowKt(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(result.contains("output") || result.contains("Output"),
            "Should reference output port")
        assertTrue(result.contains("result"),
            "Should include port name 'result'")
    }

    @Test
    fun `T013 - codeNode includes node type`() {
        // Given
        val node = createTestCodeNode("gen", "Generator", CodeNodeType.GENERATOR)
        val flowGraph = createTestFlowGraph("TypeTest", listOf(node))
        val generator = FlowKtGenerator()

        // When
        val result = generator.generateFlowKt(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(result.contains("GENERATOR") || result.contains("Generator") || result.contains("nodeType"),
            "Should include node type reference")
    }

    @Test
    fun `T013 - generates multiple codeNodes for multiple nodes`() {
        // Given
        val node1 = createTestCodeNode("n1", "FirstNode")
        val node2 = createTestCodeNode("n2", "SecondNode")
        val flowGraph = createTestFlowGraph("MultiNode", listOf(node1, node2))
        val generator = FlowKtGenerator()

        // When
        val result = generator.generateFlowKt(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(result.contains("FirstNode") || result.contains("firstNode"),
            "Should include first node")
        assertTrue(result.contains("SecondNode") || result.contains("secondNode"),
            "Should include second node")
    }

    // ========== T014: Connection DSL Statements ==========

    @Test
    fun `T014 - generateFlowKt includes connection statements`() {
        // Given
        val node1 = createTestCodeNode("src", "Source", outputPorts = listOf(
            Port(id = "src_out", name = "output", direction = Port.Direction.OUTPUT, dataType = String::class, owningNodeId = "src")
        ))
        val node2 = createTestCodeNode("dest", "Destination", inputPorts = listOf(
            Port(id = "dest_in", name = "input", direction = Port.Direction.INPUT, dataType = String::class, owningNodeId = "dest")
        ))
        val connection = Connection(
            id = "conn1",
            sourceNodeId = "src",
            sourcePortId = "src_out",
            targetNodeId = "dest",
            targetPortId = "dest_in"
        )
        val flowGraph = createTestFlowGraph("ConnTest", listOf(node1, node2), listOf(connection))
        val generator = FlowKtGenerator()

        // When
        val result = generator.generateFlowKt(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(result.contains("connect") || result.contains("Connection") || result.contains("->"),
            "Should include connection statement or arrow operator")
    }

    @Test
    fun `T014 - connection references source and target nodes`() {
        // Given
        val node1 = createTestCodeNode("timer", "TimerEmitter", outputPorts = listOf(
            Port(id = "timer_seconds", name = "elapsedSeconds", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "timer")
        ))
        val node2 = createTestCodeNode("display", "DisplayReceiver", inputPorts = listOf(
            Port(id = "display_sec", name = "seconds", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "display")
        ))
        val connection = Connection(
            id = "conn_sec",
            sourceNodeId = "timer",
            sourcePortId = "timer_seconds",
            targetNodeId = "display",
            targetPortId = "display_sec"
        )
        val flowGraph = createTestFlowGraph("RefTest", listOf(node1, node2), listOf(connection))
        val generator = FlowKtGenerator()

        // When
        val result = generator.generateFlowKt(flowGraph, "io.codenode.generated")

        // Then
        // Should reference both nodes in some form
        val hasSourceRef = result.contains("timer") || result.contains("Timer") || result.contains("elapsedSeconds")
        val hasTargetRef = result.contains("display") || result.contains("Display") || result.contains("seconds")
        assertTrue(hasSourceRef && hasTargetRef,
            "Connection should reference both source and target")
    }

    @Test
    fun `T014 - generates multiple connections`() {
        // Given
        val node1 = createTestCodeNode("emitter", "Emitter", outputPorts = listOf(
            Port(id = "out1", name = "valueA", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "emitter"),
            Port(id = "out2", name = "valueB", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "emitter")
        ))
        val node2 = createTestCodeNode("receiver", "Receiver", inputPorts = listOf(
            Port(id = "in1", name = "inputA", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "receiver"),
            Port(id = "in2", name = "inputB", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "receiver")
        ))
        val connections = listOf(
            Connection("c1", "emitter", "out1", "receiver", "in1"),
            Connection("c2", "emitter", "out2", "receiver", "in2")
        )
        val flowGraph = createTestFlowGraph("MultiConn", listOf(node1, node2), connections)
        val generator = FlowKtGenerator()

        // When
        val result = generator.generateFlowKt(flowGraph, "io.codenode.generated")

        // Then
        // Count occurrences of connection-related patterns
        val connectCount = result.windowed(7).count { it == "connect" || it == "Connect" }
        val connectionCount = result.windowed(10).count { it == "Connection" }
        val arrowCount = result.count { it == '→' } + result.windowed(2).count { it == "->" }

        assertTrue(connectCount >= 2 || connectionCount >= 2 || arrowCount >= 2,
            "Should have at least 2 connection statements")
    }

    // ========== No ProcessingLogic References ==========

    @Test
    fun `generated output does not contain processingLogic references`() {
        // Given - node with _useCaseClass config (legacy, should be ignored)
        val node = createTestCodeNode(
            id = "timer",
            name = "TimerEmitter",
            configuration = mapOf(
                "_useCaseClass" to "io.codenode.generated.stopwatch.TimerEmitterComponent"
            )
        )
        val flowGraph = createTestFlowGraph("NoLogicTest", listOf(node))
        val generator = FlowKtGenerator()

        // When
        val result = generator.generateFlowKt(flowGraph, "io.codenode.generated")

        // Then - processingLogic references should NOT appear in generated output
        assertFalse(result.contains("processingLogic"),
            "Should not contain processingLogic DSL call")
        assertFalse(result.contains("ProcessingLogic"),
            "Should not reference ProcessingLogic interface")
        // _useCaseClass is an internal config and should be filtered out
        assertFalse(result.contains("_useCaseClass"),
            "Should not include _useCaseClass in output")
    }

    @Test
    fun `nodes generate correctly without processingLogic`() {
        // Given
        val node = createTestCodeNode(
            id = "proc",
            name = "DataProcessor",
            configuration = emptyMap()
        )
        val flowGraph = createTestFlowGraph("CleanTest", listOf(node))
        val generator = FlowKtGenerator()

        // When
        val result = generator.generateFlowKt(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(result.contains("DataProcessor") || result.contains("dataProcessor"),
            "Should still generate node without processingLogic")
        assertFalse(result.contains("processingLogic"),
            "Should not contain any processingLogic references")
    }
}
