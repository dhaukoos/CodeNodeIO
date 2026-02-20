/*
 * ViewModelGeneratorTest
 * Tests for ViewModel layer generation from FlowGraph
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * Tests for ModuleGenerator ViewModel layer generation methods:
 * - collectSinkPortProperties
 * - generateControllerInterfaceClass
 */
class ViewModelGeneratorTest {

    // ========== Test Fixtures ==========

    /**
     * Creates a StopWatch-like FlowGraph with a single sink (DisplayReceiver)
     * having two Int input ports: seconds and minutes.
     */
    private fun createSingleSinkFlowGraph(): FlowGraph {
        val generatorId = "node_generator"
        val sinkId = "node_sink"

        val generator = CodeNode(
            id = generatorId,
            name = "TimerEmitter",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "${generatorId}_elapsedSeconds",
                    name = "elapsedSeconds",
                    direction = Port.Direction.OUTPUT,
                    dataType = Int::class,
                    owningNodeId = generatorId
                ),
                Port(
                    id = "${generatorId}_elapsedMinutes",
                    name = "elapsedMinutes",
                    direction = Port.Direction.OUTPUT,
                    dataType = Int::class,
                    owningNodeId = generatorId
                )
            )
        )

        val sink = CodeNode(
            id = sinkId,
            name = "DisplayReceiver",
            codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 100.0),
            inputPorts = listOf(
                Port(
                    id = "${sinkId}_seconds",
                    name = "seconds",
                    direction = Port.Direction.INPUT,
                    dataType = Int::class,
                    owningNodeId = sinkId
                ),
                Port(
                    id = "${sinkId}_minutes",
                    name = "minutes",
                    direction = Port.Direction.INPUT,
                    dataType = Int::class,
                    owningNodeId = sinkId
                )
            ),
            outputPorts = emptyList()
        )

        return FlowGraph(
            id = "graph_stopwatch",
            name = "StopWatch",
            version = "1.0.0",
            rootNodes = listOf(generator, sink),
            connections = listOf(
                Connection(
                    id = "conn_seconds",
                    sourceNodeId = generatorId,
                    sourcePortId = "${generatorId}_elapsedSeconds",
                    targetNodeId = sinkId,
                    targetPortId = "${sinkId}_seconds"
                ),
                Connection(
                    id = "conn_minutes",
                    sourceNodeId = generatorId,
                    sourcePortId = "${generatorId}_elapsedMinutes",
                    targetNodeId = sinkId,
                    targetPortId = "${sinkId}_minutes"
                )
            )
        )
    }

    /**
     * Creates a FlowGraph with two sinks to test multi-sink prefixing.
     */
    private fun createMultiSinkFlowGraph(): FlowGraph {
        val generatorId = "node_generator"
        val sink1Id = "node_display"
        val sink2Id = "node_alert"

        val generator = CodeNode(
            id = generatorId,
            name = "DataSource",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "${generatorId}_value",
                    name = "value",
                    direction = Port.Direction.OUTPUT,
                    dataType = Int::class,
                    owningNodeId = generatorId
                )
            )
        )

        val displaySink = CodeNode(
            id = sink1Id,
            name = "DisplayReceiver",
            codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 50.0),
            inputPorts = listOf(
                Port(
                    id = "${sink1Id}_seconds",
                    name = "seconds",
                    direction = Port.Direction.INPUT,
                    dataType = Int::class,
                    owningNodeId = sink1Id
                )
            ),
            outputPorts = emptyList()
        )

        val alertSink = CodeNode(
            id = sink2Id,
            name = "AlertReceiver",
            codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 200.0),
            inputPorts = listOf(
                Port(
                    id = "${sink2Id}_level",
                    name = "level",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = sink2Id
                )
            ),
            outputPorts = emptyList()
        )

        return FlowGraph(
            id = "graph_multi",
            name = "MultiSink",
            version = "1.0.0",
            rootNodes = listOf(generator, displaySink, alertSink),
            connections = emptyList()
        )
    }

    /**
     * Creates a FlowGraph with no sinks (generator-only).
     */
    private fun createZeroSinkFlowGraph(): FlowGraph {
        val generatorId = "node_generator"

        val generator = CodeNode(
            id = generatorId,
            name = "DataSource",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "${generatorId}_output",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = Int::class,
                    owningNodeId = generatorId
                )
            )
        )

        return FlowGraph(
            id = "graph_nosink",
            name = "NoSink",
            version = "1.0.0",
            rootNodes = listOf(generator),
            connections = emptyList()
        )
    }

    // ========== collectSinkPortProperties Tests ==========

    @Test
    fun collectSinkPortProperties_single_sink_returns_unprefixed_port_names() {
        val generator = ModuleGenerator()
        val flowGraph = createSingleSinkFlowGraph()

        val properties = generator.collectSinkPortProperties(flowGraph)

        assertEquals(2, properties.size)
        assertEquals("seconds", properties[0].propertyName)
        assertEquals("Int", properties[0].kotlinType)
        assertEquals("DisplayReceiver", properties[0].sinkNodeName)
        assertEquals("displayReceiver", properties[0].sinkNodeCamelCase)

        assertEquals("minutes", properties[1].propertyName)
        assertEquals("Int", properties[1].kotlinType)
    }

    @Test
    fun collectSinkPortProperties_multi_sink_returns_prefixed_property_names() {
        val generator = ModuleGenerator()
        val flowGraph = createMultiSinkFlowGraph()

        val properties = generator.collectSinkPortProperties(flowGraph)

        assertEquals(2, properties.size)
        assertEquals("displayReceiverSeconds", properties[0].propertyName)
        assertEquals("Int", properties[0].kotlinType)
        assertEquals("displayReceiver", properties[0].sinkNodeCamelCase)

        assertEquals("alertReceiverLevel", properties[1].propertyName)
        assertEquals("String", properties[1].kotlinType)
        assertEquals("alertReceiver", properties[1].sinkNodeCamelCase)
    }

    @Test
    fun collectSinkPortProperties_zero_sink_returns_empty_list() {
        val generator = ModuleGenerator()
        val flowGraph = createZeroSinkFlowGraph()

        val properties = generator.collectSinkPortProperties(flowGraph)

        assertTrue(properties.isEmpty())
    }

    // ========== generateControllerInterfaceClass Tests ==========

    @Test
    fun generateControllerInterfaceClass_single_sink_matches_expected_output() {
        val generator = ModuleGenerator()
        val flowGraph = createSingleSinkFlowGraph()
        val packageName = "io.codenode.stopwatch.generated"

        val output = generator.generateControllerInterfaceClass(flowGraph, packageName)

        val expected = buildString {
            appendLine("package io.codenode.stopwatch.generated")
            appendLine()
            appendLine("import io.codenode.fbpdsl.model.ExecutionState")
            appendLine("import io.codenode.fbpdsl.model.FlowGraph")
            appendLine("import kotlinx.coroutines.flow.StateFlow")
            appendLine()
            appendLine("interface StopWatchControllerInterface {")
            appendLine("    val seconds: StateFlow<Int>")
            appendLine("    val minutes: StateFlow<Int>")
            appendLine("    val executionState: StateFlow<ExecutionState>")
            appendLine("    fun start(): FlowGraph")
            appendLine("    fun stop(): FlowGraph")
            appendLine("    fun reset(): FlowGraph")
            appendLine("    fun pause(): FlowGraph")
            appendLine("    fun resume(): FlowGraph")
            appendLine("}")
        }

        assertEquals(expected, output)
    }

    @Test
    fun generateControllerInterfaceClass_multi_sink_uses_prefixed_property_names() {
        val generator = ModuleGenerator()
        val flowGraph = createMultiSinkFlowGraph()
        val packageName = "io.codenode.generated"

        val output = generator.generateControllerInterfaceClass(flowGraph, packageName)

        assertTrue(output.contains("interface MultiSinkControllerInterface {"))
        assertTrue(output.contains("    val displayReceiverSeconds: StateFlow<Int>"))
        assertTrue(output.contains("    val alertReceiverLevel: StateFlow<String>"))
        assertTrue(output.contains("    val executionState: StateFlow<ExecutionState>"))
        assertTrue(output.contains("    fun start(): FlowGraph"))
        assertTrue(output.contains("    fun resume(): FlowGraph"))
    }

    @Test
    fun generateControllerInterfaceClass_zero_sink_has_only_executionState_and_lifecycle() {
        val generator = ModuleGenerator()
        val flowGraph = createZeroSinkFlowGraph()
        val packageName = "io.codenode.generated"

        val output = generator.generateControllerInterfaceClass(flowGraph, packageName)

        assertTrue(output.contains("interface NoSinkControllerInterface {"))
        assertTrue(output.contains("    val executionState: StateFlow<ExecutionState>"))
        assertTrue(output.contains("    fun start(): FlowGraph"))
        assertTrue(output.contains("    fun stop(): FlowGraph"))
        assertTrue(output.contains("    fun reset(): FlowGraph"))
        assertTrue(output.contains("    fun pause(): FlowGraph"))
        assertTrue(output.contains("    fun resume(): FlowGraph"))

        // Should NOT contain any sink-derived properties
        assertFalse(output.contains("StateFlow<Int>"))
        assertFalse(output.contains("StateFlow<String>"))
    }

    // ========== generateControllerClass sink property derivation Tests ==========

    @Test
    fun generateControllerClass_derives_stateflow_properties_from_sink_ports() {
        val generator = ModuleGenerator()
        val flowGraph = createSingleSinkFlowGraph()
        val packageName = "io.codenode.stopwatch.generated"

        val output = generator.generateControllerClass(flowGraph, packageName)

        // Should contain derived StateFlow properties wired to sink component
        assertTrue(output.contains("val seconds: StateFlow<Int> = flow.displayReceiver.secondsFlow"),
            "Controller should derive seconds from displayReceiver.secondsFlow")
        assertTrue(output.contains("val minutes: StateFlow<Int> = flow.displayReceiver.minutesFlow"),
            "Controller should derive minutes from displayReceiver.minutesFlow")

        // Should NOT contain the old hardcoded StateFlow property names
        assertFalse(output.contains("elapsedSeconds"),
            "Controller should not contain old hardcoded elapsedSeconds property")
        assertFalse(output.contains("elapsedMinutes"),
            "Controller should not contain old hardcoded elapsedMinutes property")
    }
}
