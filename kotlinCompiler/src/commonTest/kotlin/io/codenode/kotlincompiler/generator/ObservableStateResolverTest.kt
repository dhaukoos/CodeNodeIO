/*
 * ObservableStateResolver Test
 * Tests for extracting observable state properties from sink node input ports
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * Tests for ObservableStateResolver - extracts observable state properties
 * from sink node input ports for MutableStateFlow generation.
 */
class ObservableStateResolverTest {

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

    private val resolver = ObservableStateResolver()

    // ========== Test: Single sink with 2 Int input ports ==========

    @Test
    fun `single sink with 2 Int input ports returns 2 properties`() {
        val sink = createTestCodeNode(
            id = "display",
            name = "DisplayReceiver",
            type = CodeNodeType.SINK,
            inputPorts = listOf(
                inputPort("d_sec", "seconds", Int::class, "display"),
                inputPort("d_min", "minutes", Int::class, "display")
            )
        )
        val flowGraph = createFlowGraph(listOf(sink))

        val properties = resolver.getObservableStateProperties(flowGraph)

        assertEquals(2, properties.size)

        val seconds = properties[0]
        assertEquals("seconds", seconds.name)
        assertEquals("Int", seconds.typeName)
        assertEquals("DisplayReceiver", seconds.sourceNodeName)
        assertEquals("seconds", seconds.sourcePortName)
        assertEquals("0", seconds.defaultValue)

        val minutes = properties[1]
        assertEquals("minutes", minutes.name)
        assertEquals("Int", minutes.typeName)
        assertEquals("DisplayReceiver", minutes.sourceNodeName)
        assertEquals("minutes", minutes.sourcePortName)
        assertEquals("0", minutes.defaultValue)
    }

    // ========== Test: No sink nodes ==========

    @Test
    fun `no sink nodes returns empty list`() {
        val generator = createTestCodeNode(
            id = "gen",
            name = "DataGenerator",
            type = CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("gen_out", "value", Int::class, "gen"))
        )
        val transformer = createTestCodeNode(
            id = "trans",
            name = "DataTransformer",
            type = CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", Int::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "output", String::class, "trans"))
        )
        val flowGraph = createFlowGraph(listOf(generator, transformer))

        val properties = resolver.getObservableStateProperties(flowGraph)

        assertTrue(properties.isEmpty())
    }

    // ========== Test: Two sinks with duplicate port names ==========

    @Test
    fun `two sinks with duplicate port names are disambiguated with node name prefix`() {
        val sink1 = createTestCodeNode(
            id = "sink1",
            name = "FirstReceiver",
            type = CodeNodeType.SINK,
            inputPorts = listOf(
                inputPort("s1_val", "value", Int::class, "sink1")
            )
        )
        val sink2 = createTestCodeNode(
            id = "sink2",
            name = "SecondReceiver",
            type = CodeNodeType.SINK,
            inputPorts = listOf(
                inputPort("s2_val", "value", String::class, "sink2")
            )
        )
        val flowGraph = createFlowGraph(listOf(sink1, sink2))

        val properties = resolver.getObservableStateProperties(flowGraph)

        assertEquals(2, properties.size)
        // Both port names are "value", so they should be disambiguated
        assertEquals("firstReceiverValue", properties[0].name)
        assertEquals("Int", properties[0].typeName)
        assertEquals("secondReceiverValue", properties[1].name)
        assertEquals("String", properties[1].typeName)
    }

    // ========== Test: Port with unmapped type ==========

    @Test
    fun `port with unmapped type defaults to Any`() {
        val sink = createTestCodeNode(
            id = "sink",
            name = "CustomSink",
            type = CodeNodeType.SINK,
            inputPorts = listOf(
                inputPort("s_data", "data", Map::class, "sink")
            )
        )
        val flowGraph = createFlowGraph(listOf(sink))

        val properties = resolver.getObservableStateProperties(flowGraph)

        assertEquals(1, properties.size)
        assertEquals("data", properties[0].name)
        assertEquals("Map", properties[0].typeName)
        assertEquals("null", properties[0].defaultValue)
    }

    // ========== Test: Sink with 1 input port ==========

    @Test
    fun `sink with 1 input port returns 1 property`() {
        val sink = createTestCodeNode(
            id = "logger",
            name = "Logger",
            type = CodeNodeType.SINK,
            inputPorts = listOf(
                inputPort("log_msg", "message", String::class, "logger")
            )
        )
        val flowGraph = createFlowGraph(listOf(sink))

        val properties = resolver.getObservableStateProperties(flowGraph)

        assertEquals(1, properties.size)
        assertEquals("message", properties[0].name)
        assertEquals("String", properties[0].typeName)
        assertEquals("Logger", properties[0].sourceNodeName)
        assertEquals("message", properties[0].sourcePortName)
        assertEquals("\"\"", properties[0].defaultValue)
    }

    // ========== Test: Default values for various types ==========

    @Test
    fun `default value for Int is 0`() {
        val sink = createTestCodeNode(
            id = "s", name = "S", type = CodeNodeType.SINK,
            inputPorts = listOf(inputPort("p", "count", Int::class, "s"))
        )
        val props = resolver.getObservableStateProperties(createFlowGraph(listOf(sink)))
        assertEquals("0", props[0].defaultValue)
    }

    @Test
    fun `default value for Long is 0L`() {
        val sink = createTestCodeNode(
            id = "s", name = "S", type = CodeNodeType.SINK,
            inputPorts = listOf(inputPort("p", "count", Long::class, "s"))
        )
        val props = resolver.getObservableStateProperties(createFlowGraph(listOf(sink)))
        assertEquals("0L", props[0].defaultValue)
    }

    @Test
    fun `default value for Double is 0_0`() {
        val sink = createTestCodeNode(
            id = "s", name = "S", type = CodeNodeType.SINK,
            inputPorts = listOf(inputPort("p", "value", Double::class, "s"))
        )
        val props = resolver.getObservableStateProperties(createFlowGraph(listOf(sink)))
        assertEquals("0.0", props[0].defaultValue)
    }

    @Test
    fun `default value for Float is 0_0f`() {
        val sink = createTestCodeNode(
            id = "s", name = "S", type = CodeNodeType.SINK,
            inputPorts = listOf(inputPort("p", "value", Float::class, "s"))
        )
        val props = resolver.getObservableStateProperties(createFlowGraph(listOf(sink)))
        assertEquals("0.0f", props[0].defaultValue)
    }

    @Test
    fun `default value for Boolean is false`() {
        val sink = createTestCodeNode(
            id = "s", name = "S", type = CodeNodeType.SINK,
            inputPorts = listOf(inputPort("p", "flag", Boolean::class, "s"))
        )
        val props = resolver.getObservableStateProperties(createFlowGraph(listOf(sink)))
        assertEquals("false", props[0].defaultValue)
    }

    // ========== Test: Non-sink nodes are ignored ==========

    @Test
    fun `generator with input ports is not treated as sink`() {
        // A node with both input and output ports is NOT a sink
        val processor = createTestCodeNode(
            id = "proc",
            name = "Processor",
            type = CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("in", "input", Int::class, "proc")),
            outputPorts = listOf(outputPort("out", "output", Int::class, "proc"))
        )
        val flowGraph = createFlowGraph(listOf(processor))

        val properties = resolver.getObservableStateProperties(flowGraph)

        assertTrue(properties.isEmpty())
    }

    // ========== Test: Mixed sink and non-sink nodes ==========

    @Test
    fun `only sink nodes contribute observable properties`() {
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
        val flowGraph = createFlowGraph(listOf(generator, sink))

        val properties = resolver.getObservableStateProperties(flowGraph)

        assertEquals(2, properties.size)
        assertEquals("seconds", properties[0].name)
        assertEquals("minutes", properties[1].name)
    }
}
