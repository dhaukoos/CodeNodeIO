/*
 * StopWatchFlowTest - Verify StopWatch.flow can be loaded
 * Part of User Story 1: StopWatch FlowGraph Creation (T014-T019)
 * License: Apache 2.0
 */

package io.codenode.grapheditor.serialization

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for verifying the StopWatch.flow file can be loaded correctly
 */
class StopWatchFlowTest {

    companion object {
        /**
         * Find the project root directory by looking for settings.gradle.kts
         */
        private fun findProjectRoot(): File {
            var dir = File(System.getProperty("user.dir"))
            while (dir.parentFile != null) {
                if (File(dir, "settings.gradle.kts").exists()) {
                    return dir
                }
                dir = dir.parentFile
            }
            // Fallback: assume current dir is project root
            return File(System.getProperty("user.dir"))
        }

        private val PROJECT_ROOT = findProjectRoot()
        private val STOPWATCH_FILE = File(PROJECT_ROOT, "demos/stopwatch/StopWatch.flow")
    }

    @Test
    fun `T014-T019 - StopWatch flow file exists and is valid`() {
        // Given: The StopWatch.flow file
        val file = STOPWATCH_FILE

        // Then: File should exist
        assertTrue(file.exists(), "StopWatch.flow should exist at ${file.absolutePath}")

        // And: File should pass validation
        val validation = FlowGraphDeserializer.validateFile(file)
        assertTrue(validation.success, "File validation failed: ${validation.errors}")
    }

    @Test
    fun `T014-T019 - StopWatch flow kts deserializes correctly`() {
        // Given: The StopWatch.flow.kts file content
        val dslContent = STOPWATCH_FILE.readText()

        // When: Deserializing
        val result = FlowGraphDeserializer.deserialize(dslContent)

        // Then: Deserialization should succeed
        assertTrue(result.isSuccess, "Deserialization failed: ${result.errorMessage}")
        assertNotNull(result.graph, "Graph should not be null")

        val graph = result.graph!!

        // And: Graph should have correct name and version
        assertEquals("StopWatch", graph.name, "Graph name should be StopWatch")
        assertEquals("1.0.0", graph.version, "Graph version should be 1.0.0")
        assertEquals("Virtual circuit demo for stopwatch functionality", graph.description)
    }

    @Test
    fun `T015 - StopWatch has TimerEmitter node with correct ports`() {
        // Given: The deserialized StopWatch FlowGraph
        val graph = loadStopWatchGraph()

        // When: Finding the TimerEmitter node
        val timerEmitter = graph.rootNodes.find { it.name == "TimerEmitter" }

        // Then: TimerEmitter should exist
        assertNotNull(timerEmitter, "TimerEmitter node should exist")

        // And: Should have 0 input ports and 2 output ports
        assertEquals(0, timerEmitter.inputPorts.size, "TimerEmitter should have 0 input ports")
        assertEquals(2, timerEmitter.outputPorts.size, "TimerEmitter should have 2 output ports")

        // And: Output ports should have correct names
        val outputNames = timerEmitter.outputPorts.map { it.name }
        assertTrue("elapsedSeconds" in outputNames, "Should have elapsedSeconds output")
        assertTrue("elapsedMinutes" in outputNames, "Should have elapsedMinutes output")
    }

    @Test
    fun `T016 - StopWatch has DisplayReceiver node with correct ports`() {
        // Given: The deserialized StopWatch FlowGraph
        val graph = loadStopWatchGraph()

        // When: Finding the DisplayReceiver node
        val displayReceiver = graph.rootNodes.find { it.name == "DisplayReceiver" }

        // Then: DisplayReceiver should exist
        assertNotNull(displayReceiver, "DisplayReceiver node should exist")

        // And: Should have 2 input ports and 0 output ports
        assertEquals(2, displayReceiver.inputPorts.size, "DisplayReceiver should have 2 input ports")
        assertEquals(0, displayReceiver.outputPorts.size, "DisplayReceiver should have 0 output ports")

        // And: Input ports should have correct names
        val inputNames = displayReceiver.inputPorts.map { it.name }
        assertTrue("seconds" in inputNames, "Should have seconds input")
        assertTrue("minutes" in inputNames, "Should have minutes input")
    }

    @Test
    fun `T017 - StopWatch has correct connections`() {
        // Given: The deserialized StopWatch FlowGraph
        val graph = loadStopWatchGraph()

        // Then: Should have 2 connections
        assertEquals(2, graph.connections.size, "StopWatch should have 2 connections")

        // And: Connections should link correct nodes
        val timerEmitter = graph.rootNodes.find { it.name == "TimerEmitter" }
        val displayReceiver = graph.rootNodes.find { it.name == "DisplayReceiver" }

        assertNotNull(timerEmitter, "TimerEmitter should exist")
        assertNotNull(displayReceiver, "DisplayReceiver should exist")

        // Verify both connections exist
        val sourceNodeIds = graph.connections.map { it.sourceNodeId }
        val targetNodeIds = graph.connections.map { it.targetNodeId }

        assertTrue(sourceNodeIds.all { it == timerEmitter.id },
            "All connections should originate from TimerEmitter")
        assertTrue(targetNodeIds.all { it == displayReceiver.id },
            "All connections should target DisplayReceiver")
    }

    @Test
    fun `T018 - TimerEmitter has speedAttenuation configuration`() {
        // Given: The deserialized StopWatch FlowGraph
        val graph = loadStopWatchGraph()

        // When: Finding the TimerEmitter node
        val timerEmitter = graph.rootNodes.find { it.name == "TimerEmitter" }
        assertNotNull(timerEmitter, "TimerEmitter should exist")

        // Then: Should have speedAttenuation config
        val speedAttenuation = timerEmitter.configuration["speedAttenuation"]
        assertNotNull(speedAttenuation, "TimerEmitter should have speedAttenuation config")
        assertEquals("1000", speedAttenuation, "speedAttenuation should be 1000")
    }

    @Test
    fun `T019 - StopWatch graph round-trips through serialization`() {
        // Given: The deserialized StopWatch FlowGraph
        val originalGraph = loadStopWatchGraph()

        // When: Re-serializing and deserializing
        val serialized = FlowGraphSerializer.serialize(originalGraph)
        val result = FlowGraphDeserializer.deserialize(serialized)

        // Then: Round-trip should succeed
        assertTrue(result.isSuccess, "Round-trip deserialization failed: ${result.errorMessage}")
        assertNotNull(result.graph)

        val roundTrippedGraph = result.graph!!

        // And: Graph properties should be preserved
        assertEquals(originalGraph.name, roundTrippedGraph.name)
        assertEquals(originalGraph.version, roundTrippedGraph.version)
        assertEquals(originalGraph.rootNodes.size, roundTrippedGraph.rootNodes.size)
        assertEquals(originalGraph.connections.size, roundTrippedGraph.connections.size)
    }

    /**
     * Helper function to load the StopWatch FlowGraph
     */
    private fun loadStopWatchGraph(): io.codenode.fbpdsl.model.FlowGraph {
        val dslContent = STOPWATCH_FILE.readText()
        val result = FlowGraphDeserializer.deserialize(dslContent)
        assertTrue(result.isSuccess, "Failed to load StopWatch.flow.kts: ${result.errorMessage}")
        return result.graph!!
    }
}
