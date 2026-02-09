/*
 * StopWatchFlowGraphTest - TDD Tests for StopWatch Virtual Circuit
 * Verifies FlowGraph structure for the StopWatch demo feature
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import io.codenode.fbpdsl.dsl.flowGraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TDD Tests for StopWatch FlowGraph Creation (User Story 1)
 *
 * These tests verify that:
 * - T009: FlowGraph with name "StopWatch" can be created
 * - T010: TimerEmitter CodeNode (0 inputs, 2 Int outputs) validates
 * - T011: DisplayReceiver CodeNode (2 Int inputs, 0 outputs) validates
 * - T012: Connections between Int ports validate successfully
 */
class StopWatchFlowGraphTest {

    // ========== T009: FlowGraph Creation Tests ==========

    @Test
    fun `T009 - FlowGraph with name StopWatch can be created`() {
        // Given: A FlowGraph builder
        val graph = flowGraph("StopWatch", version = "1.0.0") {
            // Empty graph for now - just testing name
        }

        // Then: FlowGraph should have correct name
        assertEquals("StopWatch", graph.name)
        assertEquals("1.0.0", graph.version)
    }

    @Test
    fun `T009 - StopWatch FlowGraph validates successfully`() {
        // Given: A StopWatch FlowGraph with minimal configuration
        val graph = createStopWatchFlowGraph()

        // When: Validating the graph
        val validation = graph.validate()

        // Then: Validation should pass
        assertTrue(validation.success, "Validation errors: ${validation.errors}")
    }

    @Test
    fun `T009 - StopWatch FlowGraph has correct description`() {
        // Given: A StopWatch FlowGraph
        val graph = flowGraph(
            "StopWatch",
            version = "1.0.0",
            description = "Virtual circuit demo for stopwatch functionality"
        ) {
            // Add minimal nodes
            codeNode("TimerEmitter") {
                output("elapsedSeconds", Int::class)
            }
        }

        // Then: Description should be set
        assertEquals("Virtual circuit demo for stopwatch functionality", graph.description)
    }

    // ========== T010: TimerEmitter CodeNode Tests ==========

    @Test
    fun `T010 - CodeNode with 0 inputs and 2 Int outputs validates successfully`() {
        // Given: A TimerEmitter-style CodeNode (GENERATOR pattern)
        val timerEmitter = CodeNode(
            id = "timer-emitter-1",
            name = "TimerEmitter",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(), // 0 inputs
            outputPorts = listOf(
                PortFactory.output<Int>("elapsedSeconds", "timer-emitter-1"),
                PortFactory.output<Int>("elapsedMinutes", "timer-emitter-1")
            )
        )

        // When: Validating the node
        val validation = timerEmitter.validate()

        // Then: Validation should pass
        assertTrue(validation.success, "Validation errors: ${validation.errors}")
    }

    @Test
    fun `T010 - TimerEmitter has correct port configuration`() {
        // Given: A TimerEmitter CodeNode
        val timerEmitter = createTimerEmitterNode()

        // Then: Should have 0 inputs and 2 outputs
        assertEquals(0, timerEmitter.inputPorts.size, "TimerEmitter should have 0 input ports")
        assertEquals(2, timerEmitter.outputPorts.size, "TimerEmitter should have 2 output ports")

        // Verify output port names
        val portNames = timerEmitter.outputPorts.map { it.name }
        assertTrue("elapsedSeconds" in portNames, "Should have elapsedSeconds port")
        assertTrue("elapsedMinutes" in portNames, "Should have elapsedMinutes port")
    }

    @Test
    fun `T010 - TimerEmitter output ports are typed as Int`() {
        // Given: A TimerEmitter CodeNode
        val timerEmitter = createTimerEmitterNode()

        // Then: Both output ports should have Int type
        timerEmitter.outputPorts.forEach { port ->
            assertEquals(Int::class, port.dataType,
                "Port ${port.name} should have Int type, got ${port.dataType}")
        }
    }

    @Test
    fun `T010 - TimerEmitter is classified as GENERATOR`() {
        // Given: A TimerEmitter CodeNode
        val timerEmitter = createTimerEmitterNode()

        // Then: Should be of type GENERATOR
        assertEquals(CodeNodeType.GENERATOR, timerEmitter.codeNodeType)
    }

    // ========== T011: DisplayReceiver CodeNode Tests ==========

    @Test
    fun `T011 - CodeNode with 2 Int inputs and 0 outputs validates successfully`() {
        // Given: A DisplayReceiver-style CodeNode (SINK pattern)
        val displayReceiver = CodeNode(
            id = "display-receiver-1",
            name = "DisplayReceiver",
            codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 100.0),
            inputPorts = listOf(
                PortFactory.input<Int>("seconds", "display-receiver-1"),
                PortFactory.input<Int>("minutes", "display-receiver-1")
            ),
            outputPorts = emptyList() // 0 outputs
        )

        // When: Validating the node
        val validation = displayReceiver.validate()

        // Then: Validation should pass
        assertTrue(validation.success, "Validation errors: ${validation.errors}")
    }

    @Test
    fun `T011 - DisplayReceiver has correct port configuration`() {
        // Given: A DisplayReceiver CodeNode
        val displayReceiver = createDisplayReceiverNode()

        // Then: Should have 2 inputs and 0 outputs
        assertEquals(2, displayReceiver.inputPorts.size, "DisplayReceiver should have 2 input ports")
        assertEquals(0, displayReceiver.outputPorts.size, "DisplayReceiver should have 0 output ports")

        // Verify input port names
        val portNames = displayReceiver.inputPorts.map { it.name }
        assertTrue("seconds" in portNames, "Should have seconds port")
        assertTrue("minutes" in portNames, "Should have minutes port")
    }

    @Test
    fun `T011 - DisplayReceiver input ports are typed as Int`() {
        // Given: A DisplayReceiver CodeNode
        val displayReceiver = createDisplayReceiverNode()

        // Then: Both input ports should have Int type
        displayReceiver.inputPorts.forEach { port ->
            assertEquals(Int::class, port.dataType,
                "Port ${port.name} should have Int type, got ${port.dataType}")
        }
    }

    @Test
    fun `T011 - DisplayReceiver is classified as SINK`() {
        // Given: A DisplayReceiver CodeNode
        val displayReceiver = createDisplayReceiverNode()

        // Then: Should be of type SINK
        assertEquals(CodeNodeType.SINK, displayReceiver.codeNodeType)
    }

    // ========== T012: Connection Validation Tests ==========

    @Test
    fun `T012 - Connection between Int output and Int input validates successfully`() {
        // Given: TimerEmitter and DisplayReceiver nodes
        val timerEmitter = createTimerEmitterNode()
        val displayReceiver = createDisplayReceiverNode()

        // Create connection for elapsedSeconds -> seconds
        val sourcePort = timerEmitter.outputPorts.find { it.name == "elapsedSeconds" }!!
        val targetPort = displayReceiver.inputPorts.find { it.name == "seconds" }!!

        val connection = Connection(
            id = "conn_seconds",
            sourceNodeId = timerEmitter.id,
            sourcePortId = sourcePort.id,
            targetNodeId = displayReceiver.id,
            targetPortId = targetPort.id
        )

        // When: Validating the connection with ports
        val validation = connection.validateWithPorts(sourcePort, targetPort)

        // Then: Validation should pass
        assertTrue(validation.success, "Validation errors: ${validation.errors}")
    }

    @Test
    fun `T012 - Int output port is compatible with Int input port`() {
        // Given: Int-typed ports
        val outputPort = PortFactory.output<Int>("output", "node-1")
        val inputPort = PortFactory.input<Int>("input", "node-2")

        // When: Checking compatibility
        val isCompatible = outputPort.isCompatibleWith(inputPort)

        // Then: Should be compatible
        assertTrue(isCompatible, "Int output should be compatible with Int input")
    }

    @Test
    fun `T012 - Multiple connections in StopWatch FlowGraph validate successfully`() {
        // Given: Complete StopWatch FlowGraph
        val graph = createStopWatchFlowGraph()

        // Then: Should have 2 connections
        assertEquals(2, graph.connections.size, "StopWatch should have 2 connections")

        // Validate all connections
        graph.connections.forEach { connection ->
            val basicValidation = connection.validate()
            assertTrue(basicValidation.success,
                "Connection ${connection.id} validation failed: ${basicValidation.errors}")
        }
    }

    @Test
    fun `T012 - Connection references correct port IDs`() {
        // Given: Complete StopWatch FlowGraph
        val graph = createStopWatchFlowGraph()

        // Find connections
        val secondsConnection = graph.connections.find { it.id == "conn_seconds" }
        val minutesConnection = graph.connections.find { it.id == "conn_minutes" }

        // Then: Connections should exist and reference correct nodes
        assertNotNull(secondsConnection, "Should have seconds connection")
        assertNotNull(minutesConnection, "Should have minutes connection")

        val timerEmitter = graph.rootNodes.find { it.name == "TimerEmitter" }
        val displayReceiver = graph.rootNodes.find { it.name == "DisplayReceiver" }

        assertNotNull(timerEmitter, "Should have TimerEmitter node")
        assertNotNull(displayReceiver, "Should have DisplayReceiver node")

        assertEquals(timerEmitter.id, secondsConnection.sourceNodeId)
        assertEquals(displayReceiver.id, secondsConnection.targetNodeId)
        assertEquals(timerEmitter.id, minutesConnection.sourceNodeId)
        assertEquals(displayReceiver.id, minutesConnection.targetNodeId)
    }

    // ========== Helper Functions ==========

    /**
     * Creates a TimerEmitter CodeNode matching the StopWatch specification
     */
    private fun createTimerEmitterNode(id: String = "timer-emitter"): CodeNode {
        return CodeNode(
            id = id,
            name = "TimerEmitter",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                PortFactory.output<Int>("elapsedSeconds", id),
                PortFactory.output<Int>("elapsedMinutes", id)
            ),
            controlConfig = ControlConfig(speedAttenuation = 1000L)
        )
    }

    /**
     * Creates a DisplayReceiver CodeNode matching the StopWatch specification
     */
    private fun createDisplayReceiverNode(id: String = "display-receiver"): CodeNode {
        return CodeNode(
            id = id,
            name = "DisplayReceiver",
            codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 100.0),
            inputPorts = listOf(
                PortFactory.input<Int>("seconds", id),
                PortFactory.input<Int>("minutes", id)
            ),
            outputPorts = emptyList()
        )
    }

    /**
     * Creates a complete StopWatch FlowGraph matching the specification
     */
    private fun createStopWatchFlowGraph(): FlowGraph {
        val timerEmitter = createTimerEmitterNode()
        val displayReceiver = createDisplayReceiverNode()

        // Get port IDs for connections
        val elapsedSecondsPort = timerEmitter.outputPorts.find { it.name == "elapsedSeconds" }!!
        val elapsedMinutesPort = timerEmitter.outputPorts.find { it.name == "elapsedMinutes" }!!
        val secondsPort = displayReceiver.inputPorts.find { it.name == "seconds" }!!
        val minutesPort = displayReceiver.inputPorts.find { it.name == "minutes" }!!

        val connections = listOf(
            Connection(
                id = "conn_seconds",
                sourceNodeId = timerEmitter.id,
                sourcePortId = elapsedSecondsPort.id,
                targetNodeId = displayReceiver.id,
                targetPortId = secondsPort.id,
                channelCapacity = 1
            ),
            Connection(
                id = "conn_minutes",
                sourceNodeId = timerEmitter.id,
                sourcePortId = elapsedMinutesPort.id,
                targetNodeId = displayReceiver.id,
                targetPortId = minutesPort.id,
                channelCapacity = 1
            )
        )

        return FlowGraph(
            id = "stopwatch-flow",
            name = "StopWatch",
            version = "1.0.0",
            description = "Virtual circuit demo for stopwatch functionality",
            rootNodes = listOf(timerEmitter, displayReceiver),
            connections = connections,
            targetPlatforms = listOf(
                FlowGraph.TargetPlatform.KMP_ANDROID,
                FlowGraph.TargetPlatform.KMP_IOS,
                FlowGraph.TargetPlatform.KMP_DESKTOP
            )
        )
    }
}
