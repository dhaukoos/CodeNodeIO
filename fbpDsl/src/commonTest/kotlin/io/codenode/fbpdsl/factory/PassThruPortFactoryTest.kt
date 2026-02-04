/*
 * PassThruPortFactory Tests
 * TDD tests for creating PassThruPorts for boundary-crossing connections
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.factory

import io.codenode.fbpdsl.model.*
import kotlin.reflect.KClass
import kotlin.test.*

/**
 * TDD tests for PassThruPortFactory.
 * Tests the creation and validation of PassThruPorts for boundary-crossing connections.
 */
class PassThruPortFactoryTest {

    // ============================================
    // T013: Unit tests for PassThruPortFactory.create()
    // ============================================

    @Test
    fun `create should return Success with valid PassThruPort for valid inputs`() {
        // Given: Valid inputs for creating an INPUT PassThruPort
        val graphNodeId = "graphNode-1"
        val upstreamNodeId = "external-source"
        val upstreamPortId = "output"
        val downstreamNodeId = "internal-processor"
        val downstreamPortId = "input"

        // When: Creating a PassThruPort
        val result = PassThruPortFactory.create(
            graphNodeId = graphNodeId,
            upstreamNodeId = upstreamNodeId,
            upstreamPortId = upstreamPortId,
            downstreamNodeId = downstreamNodeId,
            downstreamPortId = downstreamPortId,
            direction = Port.Direction.INPUT,
            dataType = String::class
        )

        // Then: Should return Success with valid PassThruPort
        assertTrue(result.isSuccess, "Expected Success but got Failure: ${result.exceptionOrNull()?.message}")
        val passThruPort = result.getOrNull()
        assertNotNull(passThruPort)
        assertEquals(graphNodeId, passThruPort.owningNodeId)
        assertEquals(upstreamNodeId, passThruPort.upstreamNodeId)
        assertEquals(upstreamPortId, passThruPort.upstreamPortId)
        assertEquals(downstreamNodeId, passThruPort.downstreamNodeId)
        assertEquals(downstreamPortId, passThruPort.downstreamPortId)
        assertEquals(Port.Direction.INPUT, passThruPort.direction)
        assertEquals(String::class, passThruPort.dataType)
    }

    @Test
    fun `create should return Success for OUTPUT PassThruPort`() {
        // Given: Valid inputs for creating an OUTPUT PassThruPort
        val graphNodeId = "graphNode-1"
        val upstreamNodeId = "internal-processor"
        val upstreamPortId = "output"
        val downstreamNodeId = "external-sink"
        val downstreamPortId = "input"

        // When: Creating an OUTPUT PassThruPort
        val result = PassThruPortFactory.create(
            graphNodeId = graphNodeId,
            upstreamNodeId = upstreamNodeId,
            upstreamPortId = upstreamPortId,
            downstreamNodeId = downstreamNodeId,
            downstreamPortId = downstreamPortId,
            direction = Port.Direction.OUTPUT,
            dataType = String::class
        )

        // Then: Should return Success with OUTPUT direction
        assertTrue(result.isSuccess)
        val passThruPort = result.getOrNull()
        assertNotNull(passThruPort)
        assertEquals(Port.Direction.OUTPUT, passThruPort.direction)
    }

    @Test
    fun `create should generate unique port ID`() {
        // Given: Two calls to create with same parameters
        val graphNodeId = "graphNode-1"

        // When: Creating two PassThruPorts
        val result1 = PassThruPortFactory.create(
            graphNodeId = graphNodeId,
            upstreamNodeId = "up",
            upstreamPortId = "out",
            downstreamNodeId = "down",
            downstreamPortId = "in",
            direction = Port.Direction.INPUT,
            dataType = String::class
        )

        val result2 = PassThruPortFactory.create(
            graphNodeId = graphNodeId,
            upstreamNodeId = "up",
            upstreamPortId = "out",
            downstreamNodeId = "down",
            downstreamPortId = "in",
            direction = Port.Direction.INPUT,
            dataType = String::class
        )

        // Then: Both should succeed with different IDs
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertNotEquals(result1.getOrNull()?.id, result2.getOrNull()?.id)
    }

    @Test
    fun `create should set owningNodeId to graphNodeId`() {
        // When: Creating a PassThruPort
        val result = PassThruPortFactory.create(
            graphNodeId = "my-graph-node",
            upstreamNodeId = "up",
            upstreamPortId = "out",
            downstreamNodeId = "down",
            downstreamPortId = "in",
            direction = Port.Direction.INPUT,
            dataType = String::class
        )

        // Then: owningNodeId should be the graphNodeId
        assertTrue(result.isSuccess)
        assertEquals("my-graph-node", result.getOrNull()?.owningNodeId)
    }

    @Test
    fun `create should generate meaningful port name`() {
        // When: Creating a PassThruPort
        val result = PassThruPortFactory.create(
            graphNodeId = "graphNode-1",
            upstreamNodeId = "source",
            upstreamPortId = "data_output",
            downstreamNodeId = "target",
            downstreamPortId = "data_input",
            direction = Port.Direction.INPUT,
            dataType = String::class
        )

        // Then: Port name should be meaningful (not empty)
        assertTrue(result.isSuccess)
        val passThruPort = result.getOrNull()
        assertNotNull(passThruPort)
        assertTrue(passThruPort.name.isNotBlank(), "Port name should not be blank")
    }

    // ============================================
    // T014: Unit tests for PassThruPort type validation
    // ============================================

    @Test
    fun `create should succeed when dataType matches both endpoints`() {
        // Given: Compatible data types
        val result = PassThruPortFactory.create(
            graphNodeId = "graphNode-1",
            upstreamNodeId = "up",
            upstreamPortId = "out",
            downstreamNodeId = "down",
            downstreamPortId = "in",
            direction = Port.Direction.INPUT,
            dataType = String::class
        )

        // Then: Should succeed
        assertTrue(result.isSuccess)
        assertEquals(String::class, result.getOrNull()?.dataType)
    }

    @Test
    fun `create should work with different data types`() {
        // Given: Int data type
        val intResult = PassThruPortFactory.create(
            graphNodeId = "graphNode-1",
            upstreamNodeId = "up",
            upstreamPortId = "out",
            downstreamNodeId = "down",
            downstreamPortId = "in",
            direction = Port.Direction.INPUT,
            dataType = Int::class
        )

        // Then: Should succeed with Int type
        assertTrue(intResult.isSuccess)
        assertEquals(Int::class, intResult.getOrNull()?.dataType)
    }

    @Test
    fun `create should work with Any data type for generic passthrough`() {
        // Given: Any data type (allows any data to pass through)
        val result = PassThruPortFactory.create(
            graphNodeId = "graphNode-1",
            upstreamNodeId = "up",
            upstreamPortId = "out",
            downstreamNodeId = "down",
            downstreamPortId = "in",
            direction = Port.Direction.INPUT,
            dataType = Any::class
        )

        // Then: Should succeed with Any type
        assertTrue(result.isSuccess)
        assertEquals(Any::class, result.getOrNull()?.dataType)
    }

    // ============================================
    // T015: Unit tests for PassThruPort direction validation
    // ============================================

    @Test
    fun `create should succeed with INPUT direction`() {
        // When: Creating INPUT PassThruPort
        val result = PassThruPortFactory.create(
            graphNodeId = "graphNode-1",
            upstreamNodeId = "external",
            upstreamPortId = "output",
            downstreamNodeId = "internal",
            downstreamPortId = "input",
            direction = Port.Direction.INPUT,
            dataType = String::class
        )

        // Then: Should succeed with INPUT direction
        assertTrue(result.isSuccess)
        assertEquals(Port.Direction.INPUT, result.getOrNull()?.direction)
    }

    @Test
    fun `create should succeed with OUTPUT direction`() {
        // When: Creating OUTPUT PassThruPort
        val result = PassThruPortFactory.create(
            graphNodeId = "graphNode-1",
            upstreamNodeId = "internal",
            upstreamPortId = "output",
            downstreamNodeId = "external",
            downstreamPortId = "input",
            direction = Port.Direction.OUTPUT,
            dataType = String::class
        )

        // Then: Should succeed with OUTPUT direction
        assertTrue(result.isSuccess)
        assertEquals(Port.Direction.OUTPUT, result.getOrNull()?.direction)
    }

    @Test
    fun `INPUT PassThruPort should have external upstream and internal downstream`() {
        // For INPUT: external (upstream) -> GraphNode boundary -> internal (downstream)
        val result = PassThruPortFactory.create(
            graphNodeId = "graphNode-1",
            upstreamNodeId = "external-source",
            upstreamPortId = "output",
            downstreamNodeId = "internal-target",
            downstreamPortId = "input",
            direction = Port.Direction.INPUT,
            dataType = String::class
        )

        assertTrue(result.isSuccess)
        val port = result.getOrNull()!!
        assertEquals("external-source", port.upstreamNodeId)
        assertEquals("internal-target", port.downstreamNodeId)
        assertTrue(port.isInput())
        assertFalse(port.isOutput())
    }

    @Test
    fun `OUTPUT PassThruPort should have internal upstream and external downstream`() {
        // For OUTPUT: internal (upstream) -> GraphNode boundary -> external (downstream)
        val result = PassThruPortFactory.create(
            graphNodeId = "graphNode-1",
            upstreamNodeId = "internal-source",
            upstreamPortId = "output",
            downstreamNodeId = "external-target",
            downstreamPortId = "input",
            direction = Port.Direction.OUTPUT,
            dataType = String::class
        )

        assertTrue(result.isSuccess)
        val port = result.getOrNull()!!
        assertEquals("internal-source", port.upstreamNodeId)
        assertEquals("external-target", port.downstreamNodeId)
        assertTrue(port.isOutput())
        assertFalse(port.isInput())
    }

    // ============================================
    // Additional validation tests
    // ============================================

    @Test
    fun `create should fail with blank graphNodeId`() {
        // When: Creating with blank graphNodeId
        val result = PassThruPortFactory.create(
            graphNodeId = "",
            upstreamNodeId = "up",
            upstreamPortId = "out",
            downstreamNodeId = "down",
            downstreamPortId = "in",
            direction = Port.Direction.INPUT,
            dataType = String::class
        )

        // Then: Should fail
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("graphNodeId") == true ||
                   result.exceptionOrNull()?.message?.contains("blank") == true)
    }

    @Test
    fun `create should fail with blank upstreamNodeId`() {
        // When: Creating with blank upstreamNodeId
        val result = PassThruPortFactory.create(
            graphNodeId = "graphNode-1",
            upstreamNodeId = "",
            upstreamPortId = "out",
            downstreamNodeId = "down",
            downstreamPortId = "in",
            direction = Port.Direction.INPUT,
            dataType = String::class
        )

        // Then: Should fail
        assertTrue(result.isFailure)
    }

    @Test
    fun `create should fail with blank downstreamNodeId`() {
        // When: Creating with blank downstreamNodeId
        val result = PassThruPortFactory.create(
            graphNodeId = "graphNode-1",
            upstreamNodeId = "up",
            upstreamPortId = "out",
            downstreamNodeId = "",
            downstreamPortId = "in",
            direction = Port.Direction.INPUT,
            dataType = String::class
        )

        // Then: Should fail
        assertTrue(result.isFailure)
    }

    @Test
    fun `created PassThruPort should be valid`() {
        // When: Creating a PassThruPort
        val result = PassThruPortFactory.create(
            graphNodeId = "graphNode-1",
            upstreamNodeId = "up",
            upstreamPortId = "out",
            downstreamNodeId = "down",
            downstreamPortId = "in",
            direction = Port.Direction.INPUT,
            dataType = String::class
        )

        // Then: The created port should pass validation
        assertTrue(result.isSuccess)
        val port = result.getOrNull()!!
        assertTrue(port.isValid(), "Created PassThruPort should be valid")
        assertTrue(port.isPassThruPort(), "Should identify as PassThruPort")
    }
}
