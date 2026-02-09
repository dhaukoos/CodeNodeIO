/*
 * StopWatchSerializationTest - TDD Tests for StopWatch FlowGraph Serialization
 * Verifies .flow.kts DSL serialization/deserialization
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.serialization

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TDD Tests for StopWatch FlowGraph Serialization (User Story 1)
 *
 * T013: Verify FlowGraph serializes to .flow.kts and deserializes correctly
 *
 * Note: The current fbpDsl uses the Kotlin DSL approach (FlowGraphDsl.kt) for
 * serialization. The .flow.kts files are Kotlin script files that use the DSL
 * to define the FlowGraph. This test verifies that the DSL can create FlowGraphs
 * that match the expected structure.
 */
class StopWatchSerializationTest {

    // ========== T013: DSL Serialization Tests ==========

    @Test
    fun `T013 - FlowGraph DSL creates graph with correct name and version`() {
        // Given: A FlowGraph created using DSL
        val graph = flowGraph("StopWatch", version = "1.0.0") {
            // Empty graph
        }

        // Then: Should have correct metadata
        assertEquals("StopWatch", graph.name)
        assertEquals("1.0.0", graph.version)
    }

    @Test
    fun `T013 - FlowGraph DSL creates TimerEmitter with correct ports`() {
        // Given: A FlowGraph with TimerEmitter created using DSL
        val graph = flowGraph("StopWatch", version = "1.0.0") {
            codeNode("TimerEmitter") {
                output("elapsedSeconds", Int::class)
                output("elapsedMinutes", Int::class)
            }
        }

        // Then: Should have TimerEmitter node with correct ports
        val timerEmitter = graph.rootNodes.find { it.name == "TimerEmitter" }
        assertNotNull(timerEmitter, "Should have TimerEmitter node")

        val codeNode = timerEmitter as CodeNode
        assertEquals(0, codeNode.inputPorts.size, "TimerEmitter should have 0 inputs")
        assertEquals(2, codeNode.outputPorts.size, "TimerEmitter should have 2 outputs")

        val outputNames = codeNode.outputPorts.map { it.name }
        assertTrue("elapsedSeconds" in outputNames, "Should have elapsedSeconds output")
        assertTrue("elapsedMinutes" in outputNames, "Should have elapsedMinutes output")
    }

    @Test
    fun `T013 - FlowGraph DSL creates DisplayReceiver with correct ports`() {
        // Given: A FlowGraph with DisplayReceiver created using DSL
        val graph = flowGraph("StopWatch", version = "1.0.0") {
            codeNode("DisplayReceiver") {
                input("seconds", Int::class)
                input("minutes", Int::class)
            }
        }

        // Then: Should have DisplayReceiver node with correct ports
        val displayReceiver = graph.rootNodes.find { it.name == "DisplayReceiver" }
        assertNotNull(displayReceiver, "Should have DisplayReceiver node")

        val codeNode = displayReceiver as CodeNode
        assertEquals(2, codeNode.inputPorts.size, "DisplayReceiver should have 2 inputs")
        assertEquals(0, codeNode.outputPorts.size, "DisplayReceiver should have 0 outputs")

        val inputNames = codeNode.inputPorts.map { it.name }
        assertTrue("seconds" in inputNames, "Should have seconds input")
        assertTrue("minutes" in inputNames, "Should have minutes input")
    }

    @Test
    fun `T013 - FlowGraph DSL creates connections using infix connect`() {
        // Given: A FlowGraph with connected nodes using DSL
        val graph = flowGraph("StopWatch", version = "1.0.0") {
            val timerEmitter = codeNode("TimerEmitter") {
                output("elapsedSeconds", Int::class)
                output("elapsedMinutes", Int::class)
            }

            val displayReceiver = codeNode("DisplayReceiver") {
                input("seconds", Int::class)
                input("minutes", Int::class)
            }

            // Create connections using DSL infix syntax
            timerEmitter.output("elapsedSeconds") connect displayReceiver.input("seconds")
            timerEmitter.output("elapsedMinutes") connect displayReceiver.input("minutes")
        }

        // Then: Should have 2 connections
        assertEquals(2, graph.connections.size, "Should have 2 connections")

        // Verify connection structure
        graph.connections.forEach { connection ->
            assertTrue(connection.validate().success,
                "Connection ${connection.id} should be valid")
        }
    }

    @Test
    fun `T013 - FlowGraph DSL supports target platforms`() {
        // Given: A FlowGraph with target platforms
        val graph = flowGraph("StopWatch", version = "1.0.0") {
            targetPlatforms(
                FlowGraph.TargetPlatform.KMP_ANDROID,
                FlowGraph.TargetPlatform.KMP_IOS,
                FlowGraph.TargetPlatform.KMP_DESKTOP
            )
        }

        // Then: Should have correct platforms
        assertEquals(3, graph.targetPlatforms.size)
        assertTrue(graph.targetsPlatform(FlowGraph.TargetPlatform.KMP_ANDROID))
        assertTrue(graph.targetsPlatform(FlowGraph.TargetPlatform.KMP_IOS))
        assertTrue(graph.targetsPlatform(FlowGraph.TargetPlatform.KMP_DESKTOP))
    }

    @Test
    fun `T013 - Complete StopWatch FlowGraph created via DSL validates`() {
        // Given: A complete StopWatch FlowGraph using DSL
        val graph = createStopWatchFlowGraphViaDsl()

        // When: Validating the graph
        val validation = graph.validate()

        // Then: Should be valid
        assertTrue(validation.success, "Validation errors: ${validation.errors}")
    }

    @Test
    fun `T013 - DSL-created graph matches manual creation structure`() {
        // Given: FlowGraph created via DSL
        val dslGraph = createStopWatchFlowGraphViaDsl()

        // Given: FlowGraph created manually
        val manualGraph = createStopWatchFlowGraphManually()

        // Then: Both should have same basic structure
        assertEquals(manualGraph.name, dslGraph.name, "Names should match")
        assertEquals(manualGraph.version, dslGraph.version, "Versions should match")
        assertEquals(manualGraph.rootNodes.size, dslGraph.rootNodes.size, "Node count should match")
        assertEquals(manualGraph.connections.size, dslGraph.connections.size, "Connection count should match")

        // Verify node names match
        val manualNodeNames = manualGraph.rootNodes.map { it.name }.sorted()
        val dslNodeNames = dslGraph.rootNodes.map { it.name }.sorted()
        assertEquals(manualNodeNames, dslNodeNames, "Node names should match")
    }

    @Test
    fun `T013 - FlowGraph DSL supports metadata`() {
        // Given: A FlowGraph with metadata
        val graph = flowGraph("StopWatch", version = "1.0.0") {
            metadata("author", "CodeNodeIO")
            metadata("createdDate", "2026-02-08")
        }

        // Then: Metadata should be accessible
        assertEquals("CodeNodeIO", graph.getMetadata("author"))
        assertEquals("2026-02-08", graph.getMetadata("createdDate"))
    }

    @Test
    fun `T013 - FlowGraph JSON serialization preserves basic properties`() {
        // Given: A simple FlowGraph
        val originalGraph = FlowGraph(
            id = "test-graph",
            name = "StopWatch",
            version = "1.0.0",
            description = "Test description",
            metadata = mapOf("key" to "value"),
            targetPlatforms = listOf(FlowGraph.TargetPlatform.KMP_ANDROID)
        )

        // When: Serializing using kotlinx.serialization
        val json = kotlinx.serialization.json.Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }
        val serialized = json.encodeToString(FlowGraph.serializer(), originalGraph)

        // Then: Deserialize should produce equivalent graph
        val deserialized = json.decodeFromString(FlowGraph.serializer(), serialized)

        assertEquals(originalGraph.id, deserialized.id)
        assertEquals(originalGraph.name, deserialized.name)
        assertEquals(originalGraph.version, deserialized.version)
        assertEquals(originalGraph.description, deserialized.description)
        assertEquals(originalGraph.metadata, deserialized.metadata)
        assertEquals(originalGraph.targetPlatforms, deserialized.targetPlatforms)
    }

    // ========== Helper Functions ==========

    /**
     * Creates a complete StopWatch FlowGraph using the DSL
     */
    private fun createStopWatchFlowGraphViaDsl(): FlowGraph {
        return flowGraph(
            "StopWatch",
            version = "1.0.0",
            description = "Virtual circuit demo for stopwatch functionality"
        ) {
            val timerEmitter = codeNode("TimerEmitter") {
                output("elapsedSeconds", Int::class)
                output("elapsedMinutes", Int::class)
            }

            val displayReceiver = codeNode("DisplayReceiver") {
                input("seconds", Int::class)
                input("minutes", Int::class)
            }

            timerEmitter.output("elapsedSeconds") connect displayReceiver.input("seconds")
            timerEmitter.output("elapsedMinutes") connect displayReceiver.input("minutes")

            targetPlatforms(
                FlowGraph.TargetPlatform.KMP_ANDROID,
                FlowGraph.TargetPlatform.KMP_IOS,
                FlowGraph.TargetPlatform.KMP_DESKTOP
            )
        }
    }

    /**
     * Creates a complete StopWatch FlowGraph manually (for comparison)
     */
    private fun createStopWatchFlowGraphManually(): FlowGraph {
        val timerEmitterId = "timer-emitter"
        val displayReceiverId = "display-receiver"

        val timerEmitter = CodeNode(
            id = timerEmitterId,
            name = "TimerEmitter",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                PortFactory.output<Int>("elapsedSeconds", timerEmitterId),
                PortFactory.output<Int>("elapsedMinutes", timerEmitterId)
            )
        )

        val displayReceiver = CodeNode(
            id = displayReceiverId,
            name = "DisplayReceiver",
            codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 100.0),
            inputPorts = listOf(
                PortFactory.input<Int>("seconds", displayReceiverId),
                PortFactory.input<Int>("minutes", displayReceiverId)
            ),
            outputPorts = emptyList()
        )

        val elapsedSecondsPort = timerEmitter.outputPorts.find { it.name == "elapsedSeconds" }!!
        val elapsedMinutesPort = timerEmitter.outputPorts.find { it.name == "elapsedMinutes" }!!
        val secondsPort = displayReceiver.inputPorts.find { it.name == "seconds" }!!
        val minutesPort = displayReceiver.inputPorts.find { it.name == "minutes" }!!

        return FlowGraph(
            id = "stopwatch-flow",
            name = "StopWatch",
            version = "1.0.0",
            description = "Virtual circuit demo for stopwatch functionality",
            rootNodes = listOf(timerEmitter, displayReceiver),
            connections = listOf(
                Connection(
                    id = "conn_seconds",
                    sourceNodeId = timerEmitterId,
                    sourcePortId = elapsedSecondsPort.id,
                    targetNodeId = displayReceiverId,
                    targetPortId = secondsPort.id
                ),
                Connection(
                    id = "conn_minutes",
                    sourceNodeId = timerEmitterId,
                    sourcePortId = elapsedMinutesPort.id,
                    targetNodeId = displayReceiverId,
                    targetPortId = minutesPort.id
                )
            ),
            targetPlatforms = listOf(
                FlowGraph.TargetPlatform.KMP_ANDROID,
                FlowGraph.TargetPlatform.KMP_IOS,
                FlowGraph.TargetPlatform.KMP_DESKTOP
            )
        )
    }
}
