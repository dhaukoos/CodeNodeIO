/*
 * ConnectionWiringResolver Test
 * Tests for resolving FlowGraph connections to channel property assignments
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * Tests for ConnectionWiringResolver - resolves FlowGraph connections to
 * channel wiring statements for runtime instance channel assignments.
 */
class ConnectionWiringResolverTest {

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
        nodes: List<CodeNode>,
        connections: List<Connection> = emptyList()
    ): FlowGraph {
        return FlowGraph(
            id = "test-flow",
            name = "TestFlow",
            version = "1.0.0",
            rootNodes = nodes,
            connections = connections
        )
    }

    private val resolver = ConnectionWiringResolver()

    // ========== Test: 2 connections between Out2Generator and In2Sink ==========

    @Test
    fun `2 connections between generator and sink produces 2 wiring statements`() {
        val generator = createTestCodeNode(
            id = "gen",
            name = "TimerEmitter",
            type = CodeNodeType.GENERATOR,
            outputPorts = listOf(
                outputPort("gen_sec", "seconds", Int::class, "gen"),
                outputPort("gen_min", "minutes", Int::class, "gen")
            )
        )
        val sink = createTestCodeNode(
            id = "display",
            name = "DisplayReceiver",
            type = CodeNodeType.SINK,
            inputPorts = listOf(
                inputPort("d_sec", "seconds", Int::class, "display"),
                inputPort("d_min", "minutes", Int::class, "display")
            )
        )
        val connections = listOf(
            Connection(
                id = "conn1",
                sourceNodeId = "gen",
                sourcePortId = "gen_sec",
                targetNodeId = "display",
                targetPortId = "d_sec"
            ),
            Connection(
                id = "conn2",
                sourceNodeId = "gen",
                sourcePortId = "gen_min",
                targetNodeId = "display",
                targetPortId = "d_min"
            )
        )
        val flowGraph = createFlowGraph(listOf(generator, sink), connections)

        val statements = resolver.getWiringStatements(flowGraph)

        assertEquals(2, statements.size)

        // First connection: gen outputChannel1 → display inputChannel1
        val stmt1 = statements[0]
        assertEquals("displayReceiver", stmt1.targetVarName)
        assertEquals("inputChannel1", stmt1.targetChannelProp)
        assertEquals("timerEmitter", stmt1.sourceVarName)
        assertEquals("outputChannel1", stmt1.sourceChannelProp)

        // Second connection: gen outputChannel2 → display inputChannel2
        val stmt2 = statements[1]
        assertEquals("displayReceiver", stmt2.targetVarName)
        assertEquals("inputChannel2", stmt2.targetChannelProp)
        assertEquals("timerEmitter", stmt2.sourceVarName)
        assertEquals("outputChannel2", stmt2.sourceChannelProp)
    }

    // ========== Test: No connections ==========

    @Test
    fun `no connections returns empty list`() {
        val generator = createTestCodeNode(
            id = "gen",
            name = "Generator",
            type = CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("gen_out", "value", Int::class, "gen"))
        )
        val flowGraph = createFlowGraph(listOf(generator))

        val statements = resolver.getWiringStatements(flowGraph)

        assertTrue(statements.isEmpty())
    }

    // ========== Test: Single output generator uses outputChannel ==========

    @Test
    fun `single output generator uses outputChannel`() {
        val generator = createTestCodeNode(
            id = "gen",
            name = "ValueGenerator",
            type = CodeNodeType.GENERATOR,
            outputPorts = listOf(
                outputPort("gen_out", "value", Int::class, "gen")
            )
        )
        val sink = createTestCodeNode(
            id = "sink",
            name = "ValueSink",
            type = CodeNodeType.SINK,
            inputPorts = listOf(
                inputPort("sink_in", "value", Int::class, "sink")
            )
        )
        val connections = listOf(
            Connection(
                id = "conn1",
                sourceNodeId = "gen",
                sourcePortId = "gen_out",
                targetNodeId = "sink",
                targetPortId = "sink_in"
            )
        )
        val flowGraph = createFlowGraph(listOf(generator, sink), connections)

        val statements = resolver.getWiringStatements(flowGraph)

        assertEquals(1, statements.size)
        assertEquals("outputChannel", statements[0].sourceChannelProp)
    }

    // ========== Test: Single input sink uses inputChannel ==========

    @Test
    fun `single input sink uses inputChannel`() {
        val generator = createTestCodeNode(
            id = "gen",
            name = "ValueGenerator",
            type = CodeNodeType.GENERATOR,
            outputPorts = listOf(
                outputPort("gen_out", "value", Int::class, "gen")
            )
        )
        val sink = createTestCodeNode(
            id = "sink",
            name = "ValueSink",
            type = CodeNodeType.SINK,
            inputPorts = listOf(
                inputPort("sink_in", "value", Int::class, "sink")
            )
        )
        val connections = listOf(
            Connection(
                id = "conn1",
                sourceNodeId = "gen",
                sourcePortId = "gen_out",
                targetNodeId = "sink",
                targetPortId = "sink_in"
            )
        )
        val flowGraph = createFlowGraph(listOf(generator, sink), connections)

        val statements = resolver.getWiringStatements(flowGraph)

        assertEquals(1, statements.size)
        assertEquals("inputChannel", statements[0].targetChannelProp)
    }

    // ========== Test: 3-output generator uses numbered channels ==========

    @Test
    fun `3 output generator uses outputChannel1 outputChannel2 outputChannel3`() {
        val generator = createTestCodeNode(
            id = "gen",
            name = "TriGenerator",
            type = CodeNodeType.GENERATOR,
            outputPorts = listOf(
                outputPort("gen_o1", "first", Int::class, "gen"),
                outputPort("gen_o2", "second", String::class, "gen"),
                outputPort("gen_o3", "third", Boolean::class, "gen")
            )
        )
        val sink1 = createTestCodeNode(
            id = "s1",
            name = "Sink1",
            type = CodeNodeType.SINK,
            inputPorts = listOf(inputPort("s1_in", "value", Int::class, "s1"))
        )
        val sink2 = createTestCodeNode(
            id = "s2",
            name = "Sink2",
            type = CodeNodeType.SINK,
            inputPorts = listOf(inputPort("s2_in", "value", String::class, "s2"))
        )
        val sink3 = createTestCodeNode(
            id = "s3",
            name = "Sink3",
            type = CodeNodeType.SINK,
            inputPorts = listOf(inputPort("s3_in", "value", Boolean::class, "s3"))
        )
        val connections = listOf(
            Connection("c1", "gen", "gen_o1", "s1", "s1_in"),
            Connection("c2", "gen", "gen_o2", "s2", "s2_in"),
            Connection("c3", "gen", "gen_o3", "s3", "s3_in")
        )
        val flowGraph = createFlowGraph(listOf(generator, sink1, sink2, sink3), connections)

        val statements = resolver.getWiringStatements(flowGraph)

        assertEquals(3, statements.size)
        assertEquals("outputChannel1", statements[0].sourceChannelProp)
        assertEquals("outputChannel2", statements[1].sourceChannelProp)
        assertEquals("outputChannel3", statements[2].sourceChannelProp)
        // All sinks have 1 input, so they use inputChannel (not numbered)
        assertEquals("inputChannel", statements[0].targetChannelProp)
        assertEquals("inputChannel", statements[1].targetChannelProp)
        assertEquals("inputChannel", statements[2].targetChannelProp)
    }

    // ========== Test: 3-input sink uses numbered channels ==========

    @Test
    fun `3 input sink uses inputChannel1 inputChannel2 inputChannel3`() {
        val gen1 = createTestCodeNode(
            id = "g1", name = "Gen1", type = CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("g1_out", "value", Int::class, "g1"))
        )
        val gen2 = createTestCodeNode(
            id = "g2", name = "Gen2", type = CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("g2_out", "value", String::class, "g2"))
        )
        val gen3 = createTestCodeNode(
            id = "g3", name = "Gen3", type = CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("g3_out", "value", Boolean::class, "g3"))
        )
        val sink = createTestCodeNode(
            id = "sink", name = "TriSink", type = CodeNodeType.SINK,
            inputPorts = listOf(
                inputPort("s_i1", "first", Int::class, "sink"),
                inputPort("s_i2", "second", String::class, "sink"),
                inputPort("s_i3", "third", Boolean::class, "sink")
            )
        )
        val connections = listOf(
            Connection("c1", "g1", "g1_out", "sink", "s_i1"),
            Connection("c2", "g2", "g2_out", "sink", "s_i2"),
            Connection("c3", "g3", "g3_out", "sink", "s_i3")
        )
        val flowGraph = createFlowGraph(listOf(gen1, gen2, gen3, sink), connections)

        val statements = resolver.getWiringStatements(flowGraph)

        assertEquals(3, statements.size)
        assertEquals("inputChannel1", statements[0].targetChannelProp)
        assertEquals("inputChannel2", statements[1].targetChannelProp)
        assertEquals("inputChannel3", statements[2].targetChannelProp)
    }

    // ========== Test: Variable names are camelCase ==========

    @Test
    fun `variable names are camelCase of node names`() {
        val generator = createTestCodeNode(
            id = "gen",
            name = "TimerEmitter",
            type = CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("gen_out", "value", Int::class, "gen"))
        )
        val sink = createTestCodeNode(
            id = "sink",
            name = "DisplayReceiver",
            type = CodeNodeType.SINK,
            inputPorts = listOf(inputPort("sink_in", "value", Int::class, "sink"))
        )
        val connections = listOf(
            Connection("c1", "gen", "gen_out", "sink", "sink_in")
        )
        val flowGraph = createFlowGraph(listOf(generator, sink), connections)

        val statements = resolver.getWiringStatements(flowGraph)

        assertEquals("timerEmitter", statements[0].sourceVarName)
        assertEquals("displayReceiver", statements[0].targetVarName)
    }
}
