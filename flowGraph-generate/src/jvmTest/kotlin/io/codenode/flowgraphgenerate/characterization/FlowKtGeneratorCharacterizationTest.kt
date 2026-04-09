/*
 * FlowKtGenerator Characterization Test
 * Pins current .flow.kt generation behavior for FlowGraphs with various configurations
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.characterization

import io.codenode.fbpdsl.model.*
import io.codenode.flowgraphgenerate.generator.FlowKtGenerator
import kotlin.test.*

/**
 * Characterization tests for FlowKtGenerator.
 *
 * These tests capture the current .flow.kt output format, not correctness.
 * They serve as a safety net during vertical-slice extraction to flowGraph-generate.
 */
class FlowKtGeneratorCharacterizationTest {

    private val generator = FlowKtGenerator()

    // ========== Test Fixtures ==========

    private fun createTestNode(
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
        nodes: List<Node> = emptyList(),
        connections: List<Connection> = emptyList()
    ): FlowGraph {
        return FlowGraph(
            id = "flow_${name.lowercase()}",
            name = name,
            version = "1.0.0",
            description = "Test flow",
            rootNodes = nodes,
            connections = connections,
            targetPlatforms = listOf(
                FlowGraph.TargetPlatform.KMP_ANDROID,
                FlowGraph.TargetPlatform.KMP_IOS,
                FlowGraph.TargetPlatform.KMP_DESKTOP
            )
        )
    }

    // ========== Basic Structure Tests ==========

    @Test
    fun `generates non-empty output for simple flow`() {
        val node = createTestNode("n1", "Processor")
        val fg = createTestFlowGraph("Simple", listOf(node))

        val result = generator.generateFlowKt(fg, "io.codenode.test")

        assertTrue(result.isNotBlank(), "Should produce non-blank output")
    }

    @Test
    fun `includes package declaration at start`() {
        val node = createTestNode("n1", "Processor")
        val fg = createTestFlowGraph("PkgTest", listOf(node))

        val result = generator.generateFlowKt(fg, "io.codenode.mymodule")

        assertTrue(result.contains("package io.codenode.mymodule"),
            "Should include exact package declaration")

        val lines = result.lines().filter { it.isNotBlank() && !it.trim().startsWith("/*") && !it.trim().startsWith("*") }
        val firstNonComment = lines.firstOrNull { !it.trim().startsWith("//") }
        assertTrue(firstNonComment?.trim()?.startsWith("package ") == true,
            "Package should be first non-comment line")
    }

    @Test
    fun `includes import statements`() {
        val node = createTestNode("n1", "Processor")
        val fg = createTestFlowGraph("ImportTest", listOf(node))

        val result = generator.generateFlowKt(fg, "io.codenode.test")

        assertTrue(result.contains("import "), "Should contain import statements")
    }

    @Test
    fun `includes flowGraph DSL block`() {
        val node = createTestNode("n1", "Processor")
        val fg = createTestFlowGraph("DslTest", listOf(node))

        val result = generator.generateFlowKt(fg, "io.codenode.test")

        assertTrue(result.contains("flowGraph(") || result.contains("val ") && result.contains("Graph"),
            "Should contain flowGraph DSL block")
    }

    // ========== Node Generation Tests ==========

    @Test
    fun `generates codeNode block with node name`() {
        val node = createTestNode("timer", "TimerEmitter", CodeNodeType.SOURCE)
        val fg = createTestFlowGraph("NodeName", listOf(node))

        val result = generator.generateFlowKt(fg, "io.codenode.test")

        assertTrue(result.contains("TimerEmitter") || result.contains("timerEmitter"),
            "Should include node name")
    }

    @Test
    fun `generates codeNode block with node type`() {
        val node = createTestNode("src", "Generator", CodeNodeType.SOURCE)
        val fg = createTestFlowGraph("TypeTest", listOf(node))

        val result = generator.generateFlowKt(fg, "io.codenode.test")

        assertTrue(result.contains("nodeType = \"SOURCE\""),
            "Should include quoted node type")
    }

    @Test
    fun `generates codeNode block with position`() {
        val node = createTestNode("pos", "PositionNode").copy(
            position = Node.Position(150.0, 250.0)
        )
        val fg = createTestFlowGraph("PosTest", listOf(node))

        val result = generator.generateFlowKt(fg, "io.codenode.test")

        assertTrue(result.contains("150") && result.contains("250"),
            "Should include position coordinates")
    }

    @Test
    fun `generates input port declarations`() {
        val node = createTestNode(
            id = "proc",
            name = "Processor",
            inputPorts = listOf(
                Port(id = "p_data", name = "data", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "proc")
            )
        )
        val fg = createTestFlowGraph("InputPort", listOf(node))

        val result = generator.generateFlowKt(fg, "io.codenode.test")

        assertTrue(result.contains("data"), "Should include input port name 'data'")
    }

    @Test
    fun `generates output port declarations`() {
        val node = createTestNode(
            id = "gen",
            name = "Generator",
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(id = "p_result", name = "result", direction = Port.Direction.OUTPUT, dataType = String::class, owningNodeId = "gen")
            )
        )
        val fg = createTestFlowGraph("OutputPort", listOf(node))

        val result = generator.generateFlowKt(fg, "io.codenode.test")

        assertTrue(result.contains("result"), "Should include output port name 'result'")
    }

    @Test
    fun `generates multiple nodes`() {
        val node1 = createTestNode("n1", "FirstNode")
        val node2 = createTestNode("n2", "SecondNode")
        val fg = createTestFlowGraph("MultiNode", listOf(node1, node2))

        val result = generator.generateFlowKt(fg, "io.codenode.test")

        assertTrue(result.contains("FirstNode") || result.contains("firstNode"),
            "Should include first node")
        assertTrue(result.contains("SecondNode") || result.contains("secondNode"),
            "Should include second node")
    }

    // ========== Connection Generation Tests ==========

    @Test
    fun `generates connection statements`() {
        val source = createTestNode("src", "Source", CodeNodeType.SOURCE,
            inputPorts = emptyList(),
            outputPorts = listOf(Port(id = "src_out", name = "output", direction = Port.Direction.OUTPUT, dataType = String::class, owningNodeId = "src")))
        val sink = createTestNode("dst", "Destination", CodeNodeType.SINK,
            inputPorts = listOf(Port(id = "dst_in", name = "input", direction = Port.Direction.INPUT, dataType = String::class, owningNodeId = "dst")),
            outputPorts = emptyList())
        val conn = Connection("c1", "src", "src_out", "dst", "dst_in")
        val fg = createTestFlowGraph("ConnTest", listOf(source, sink), listOf(conn))

        val result = generator.generateFlowKt(fg, "io.codenode.test")

        assertTrue(result.contains("connect") || result.contains("Connection") || result.contains("->"),
            "Should include connection statement")
    }

    @Test
    fun `generates multiple connections`() {
        val emitter = createTestNode("em", "Emitter", CodeNodeType.SOURCE,
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(id = "em_out1", name = "valueA", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "em"),
                Port(id = "em_out2", name = "valueB", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "em")
            ))
        val receiver = createTestNode("rx", "Receiver", CodeNodeType.SINK,
            inputPorts = listOf(
                Port(id = "rx_in1", name = "inputA", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "rx"),
                Port(id = "rx_in2", name = "inputB", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "rx")
            ),
            outputPorts = emptyList())
        val connections = listOf(
            Connection("c1", "em", "em_out1", "rx", "rx_in1"),
            Connection("c2", "em", "em_out2", "rx", "rx_in2")
        )
        val fg = createTestFlowGraph("MultiConn", listOf(emitter, receiver), connections)

        val result = generator.generateFlowKt(fg, "io.codenode.test")

        val connectCount = result.windowed(7).count { it == "connect" }
        val arrowCount = result.windowed(2).count { it == "->" }
        assertTrue(connectCount >= 2 || arrowCount >= 2,
            "Should have at least 2 connection statements")
    }

    @Test
    fun `connection references both source and target nodes`() {
        val timer = createTestNode("timer", "TimerEmitter", CodeNodeType.SOURCE,
            inputPorts = emptyList(),
            outputPorts = listOf(Port(id = "timer_sec", name = "elapsedSeconds", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "timer")))
        val display = createTestNode("display", "DisplayReceiver", CodeNodeType.SINK,
            inputPorts = listOf(Port(id = "disp_sec", name = "seconds", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "display")),
            outputPorts = emptyList())
        val conn = Connection("c_sec", "timer", "timer_sec", "display", "disp_sec")
        val fg = createTestFlowGraph("RefTest", listOf(timer, display), listOf(conn))

        val result = generator.generateFlowKt(fg, "io.codenode.test")

        val hasSourceRef = result.contains("timer") || result.contains("Timer") || result.contains("elapsedSeconds")
        val hasTargetRef = result.contains("display") || result.contains("Display") || result.contains("seconds")
        assertTrue(hasSourceRef && hasTargetRef, "Connection should reference both source and target")
    }

    // ========== IP Type Override Tests ==========

    @Test
    fun `generates with custom IP type names`() {
        val node = createTestNode("n1", "TypedNode",
            inputPorts = listOf(Port(id = "p_in", name = "input", direction = Port.Direction.INPUT, dataType = String::class, owningNodeId = "n1")),
            outputPorts = listOf(Port(id = "p_out", name = "output", direction = Port.Direction.OUTPUT, dataType = String::class, owningNodeId = "n1"))
        )
        val fg = createTestFlowGraph("IpTypeTest", listOf(node))

        val ipTypeNames = mapOf("port_type_id" to "CustomDataType")
        val ipTypeImports = listOf("io.codenode.custom.CustomDataType")

        val result = generator.generateFlowKt(fg, "io.codenode.test",
            ipTypeNames = ipTypeNames, ipTypeImports = ipTypeImports)

        // Pin: generator accepts IP type overrides without error
        assertTrue(result.isNotBlank(), "Should produce output with IP type overrides")
        assertTrue(result.contains("package io.codenode.test"), "Should still have correct package")
    }

    // ========== Configuration Filtering Tests ==========

    @Test
    fun `filters out internal configuration keys`() {
        val node = createTestNode(
            id = "cfg",
            name = "ConfigNode",
            configuration = mapOf(
                "_useCaseClass" to "io.codenode.generated.SomeComponent",
                "visible_prop" to "value"
            )
        )
        val fg = createTestFlowGraph("ConfigFilter", listOf(node))

        val result = generator.generateFlowKt(fg, "io.codenode.test")

        assertFalse(result.contains("_useCaseClass"),
            "Should filter out _useCaseClass internal config")
        assertFalse(result.contains("processingLogic"),
            "Should not contain processingLogic references")
    }

    // ========== Edge Cases ==========

    @Test
    fun `handles empty FlowGraph with no nodes`() {
        val fg = createTestFlowGraph("Empty", emptyList())

        val result = generator.generateFlowKt(fg, "io.codenode.test")

        assertTrue(result.isNotBlank(), "Should produce output even for empty graph")
        assertTrue(result.contains("package io.codenode.test"), "Should include package declaration")
    }

    @Test
    fun `handles node with no ports`() {
        val node = createTestNode("nop", "NoPortNode",
            inputPorts = emptyList(), outputPorts = emptyList())
        val fg = createTestFlowGraph("NoPort", listOf(node))

        val result = generator.generateFlowKt(fg, "io.codenode.test")

        assertTrue(result.contains("NoPortNode") || result.contains("noPortNode"),
            "Should include node even without ports")
    }

    @Test
    fun `handles three-node chain with connections`() {
        val source = createTestNode("s", "Source", CodeNodeType.SOURCE,
            inputPorts = emptyList(),
            outputPorts = listOf(Port(id = "s_out", name = "output", direction = Port.Direction.OUTPUT, dataType = String::class, owningNodeId = "s")))
        val transformer = createTestNode("t", "Transform", CodeNodeType.TRANSFORMER,
            inputPorts = listOf(Port(id = "t_in", name = "input", direction = Port.Direction.INPUT, dataType = String::class, owningNodeId = "t")),
            outputPorts = listOf(Port(id = "t_out", name = "output", direction = Port.Direction.OUTPUT, dataType = String::class, owningNodeId = "t")))
        val sink = createTestNode("k", "Sink", CodeNodeType.SINK,
            inputPorts = listOf(Port(id = "k_in", name = "input", direction = Port.Direction.INPUT, dataType = String::class, owningNodeId = "k")),
            outputPorts = emptyList())
        val connections = listOf(
            Connection("c1", "s", "s_out", "t", "t_in"),
            Connection("c2", "t", "t_out", "k", "k_in")
        )
        val fg = createTestFlowGraph("Chain", listOf(source, transformer, sink), connections)

        val result = generator.generateFlowKt(fg, "io.codenode.test")

        assertTrue(result.contains("Source") || result.contains("source"), "Should include source node")
        assertTrue(result.contains("Transform") || result.contains("transform"), "Should include transformer")
        assertTrue(result.contains("Sink") || result.contains("sink"), "Should include sink node")
    }
}
