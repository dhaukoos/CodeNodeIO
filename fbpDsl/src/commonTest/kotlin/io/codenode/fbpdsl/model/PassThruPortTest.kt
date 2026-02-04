/*
 * PassThruPort Unit Tests
 * Tests for composition pattern and property delegation
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class PassThruPortTest {

    // Helper to create a test Port
    private fun createTestPort(
        id: String = "port-1",
        name: String = "testPort",
        direction: Port.Direction = Port.Direction.INPUT,
        owningNodeId: String = "graphNode-1"
    ): Port<String> = Port(
        id = id,
        name = name,
        direction = direction,
        dataType = String::class,
        owningNodeId = owningNodeId
    )

    // ==================== Composition Tests ====================

    @Test
    fun `PassThruPort wraps underlying Port`() {
        val underlyingPort = createTestPort()
        val passThruPort = PassThruPort(
            port = underlyingPort,
            upstreamNodeId = "external-node",
            upstreamPortId = "external-output",
            downstreamNodeId = "internal-node",
            downstreamPortId = "internal-input"
        )

        assertEquals(underlyingPort, passThruPort.port)
    }

    @Test
    fun `PassThruPort stores upstream references`() {
        val passThruPort = PassThruPort(
            port = createTestPort(),
            upstreamNodeId = "source-node",
            upstreamPortId = "source-port",
            downstreamNodeId = "target-node",
            downstreamPortId = "target-port"
        )

        assertEquals("source-node", passThruPort.upstreamNodeId)
        assertEquals("source-port", passThruPort.upstreamPortId)
    }

    @Test
    fun `PassThruPort stores downstream references`() {
        val passThruPort = PassThruPort(
            port = createTestPort(),
            upstreamNodeId = "source-node",
            upstreamPortId = "source-port",
            downstreamNodeId = "target-node",
            downstreamPortId = "target-port"
        )

        assertEquals("target-node", passThruPort.downstreamNodeId)
        assertEquals("target-port", passThruPort.downstreamPortId)
    }

    // ==================== Property Delegation Tests ====================

    @Test
    fun `PassThruPort delegates id to underlying port`() {
        val underlyingPort = createTestPort(id = "delegated-id")
        val passThruPort = PassThruPort(
            port = underlyingPort,
            upstreamNodeId = "up",
            upstreamPortId = "up-port",
            downstreamNodeId = "down",
            downstreamPortId = "down-port"
        )

        assertEquals("delegated-id", passThruPort.id)
    }

    @Test
    fun `PassThruPort delegates name to underlying port`() {
        val underlyingPort = createTestPort(name = "delegated-name")
        val passThruPort = PassThruPort(
            port = underlyingPort,
            upstreamNodeId = "up",
            upstreamPortId = "up-port",
            downstreamNodeId = "down",
            downstreamPortId = "down-port"
        )

        assertEquals("delegated-name", passThruPort.name)
    }

    @Test
    fun `PassThruPort delegates direction to underlying port`() {
        val inputPort = createTestPort(direction = Port.Direction.INPUT)
        val outputPort = createTestPort(direction = Port.Direction.OUTPUT)

        val inputPassThru = PassThruPort(
            port = inputPort,
            upstreamNodeId = "up",
            upstreamPortId = "up-port",
            downstreamNodeId = "down",
            downstreamPortId = "down-port"
        )

        val outputPassThru = PassThruPort(
            port = outputPort,
            upstreamNodeId = "up",
            upstreamPortId = "up-port",
            downstreamNodeId = "down",
            downstreamPortId = "down-port"
        )

        assertEquals(Port.Direction.INPUT, inputPassThru.direction)
        assertEquals(Port.Direction.OUTPUT, outputPassThru.direction)
    }

    @Test
    fun `PassThruPort delegates dataType to underlying port`() {
        val underlyingPort = createTestPort()
        val passThruPort = PassThruPort(
            port = underlyingPort,
            upstreamNodeId = "up",
            upstreamPortId = "up-port",
            downstreamNodeId = "down",
            downstreamPortId = "down-port"
        )

        assertEquals(String::class, passThruPort.dataType)
    }

    @Test
    fun `PassThruPort delegates owningNodeId to underlying port`() {
        val underlyingPort = createTestPort(owningNodeId = "graph-node-owner")
        val passThruPort = PassThruPort(
            port = underlyingPort,
            upstreamNodeId = "up",
            upstreamPortId = "up-port",
            downstreamNodeId = "down",
            downstreamPortId = "down-port"
        )

        assertEquals("graph-node-owner", passThruPort.owningNodeId)
    }

    // ==================== Equality Tests ====================

    @Test
    fun `PassThruPort equality based on all properties`() {
        val port1 = createTestPort(id = "port-1")
        val port2 = createTestPort(id = "port-1")

        val passThru1 = PassThruPort(
            port = port1,
            upstreamNodeId = "up",
            upstreamPortId = "up-port",
            downstreamNodeId = "down",
            downstreamPortId = "down-port"
        )

        val passThru2 = PassThruPort(
            port = port2,
            upstreamNodeId = "up",
            upstreamPortId = "up-port",
            downstreamNodeId = "down",
            downstreamPortId = "down-port"
        )

        assertEquals(passThru1, passThru2)
    }

    @Test
    fun `PassThruPort inequality when upstream differs`() {
        val port = createTestPort()

        val passThru1 = PassThruPort(
            port = port,
            upstreamNodeId = "up-1",
            upstreamPortId = "up-port",
            downstreamNodeId = "down",
            downstreamPortId = "down-port"
        )

        val passThru2 = PassThruPort(
            port = port,
            upstreamNodeId = "up-2",
            upstreamPortId = "up-port",
            downstreamNodeId = "down",
            downstreamPortId = "down-port"
        )

        assertNotEquals(passThru1, passThru2)
    }

    // ==================== INPUT PassThruPort Tests ====================

    @Test
    fun `INPUT PassThruPort has external upstream and internal downstream`() {
        // For INPUT PassThruPort: upstream is external, downstream is internal
        val graphNodeId = "group-1"
        val externalNodeId = "external-source"
        val internalNodeId = "internal-target"

        val inputPassThru = PassThruPort(
            port = createTestPort(
                direction = Port.Direction.INPUT,
                owningNodeId = graphNodeId
            ),
            upstreamNodeId = externalNodeId,
            upstreamPortId = "output",
            downstreamNodeId = internalNodeId,
            downstreamPortId = "input"
        )

        assertEquals(Port.Direction.INPUT, inputPassThru.direction)
        assertEquals(graphNodeId, inputPassThru.owningNodeId)
        assertEquals(externalNodeId, inputPassThru.upstreamNodeId)
        assertEquals(internalNodeId, inputPassThru.downstreamNodeId)
    }

    // ==================== OUTPUT PassThruPort Tests ====================

    @Test
    fun `OUTPUT PassThruPort has internal upstream and external downstream`() {
        // For OUTPUT PassThruPort: upstream is internal, downstream is external
        val graphNodeId = "group-1"
        val internalNodeId = "internal-source"
        val externalNodeId = "external-target"

        val outputPassThru = PassThruPort(
            port = createTestPort(
                direction = Port.Direction.OUTPUT,
                owningNodeId = graphNodeId
            ),
            upstreamNodeId = internalNodeId,
            upstreamPortId = "output",
            downstreamNodeId = externalNodeId,
            downstreamPortId = "input"
        )

        assertEquals(Port.Direction.OUTPUT, outputPassThru.direction)
        assertEquals(graphNodeId, outputPassThru.owningNodeId)
        assertEquals(internalNodeId, outputPassThru.upstreamNodeId)
        assertEquals(externalNodeId, outputPassThru.downstreamNodeId)
    }

    // ==================== isPassThruPort Helper Tests ====================

    @Test
    fun `isPassThruPort returns true for PassThruPort`() {
        val passThruPort = PassThruPort(
            port = createTestPort(),
            upstreamNodeId = "up",
            upstreamPortId = "up-port",
            downstreamNodeId = "down",
            downstreamPortId = "down-port"
        )

        assertTrue(passThruPort.isPassThruPort())
    }
}
