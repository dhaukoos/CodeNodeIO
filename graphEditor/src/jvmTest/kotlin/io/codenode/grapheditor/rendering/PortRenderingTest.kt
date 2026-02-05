/*
 * PortRenderingTest - Unit Tests for Port Shape Detection and Rendering
 * Tests visual distinction between regular Ports (circles) and PassThruPorts (squares)
 * License: Apache 2.0
 */

package io.codenode.grapheditor.rendering

import io.codenode.fbpdsl.model.PassThruPort
import io.codenode.fbpdsl.model.Port
import io.codenode.grapheditor.rendering.PortShape
import io.codenode.grapheditor.rendering.getPortShape
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for port shape detection and rendering logic.
 *
 * User Story 3: Visual Distinction of PassThruPorts
 * - Regular Ports render as circles (CIRCLE shape)
 * - PassThruPorts render as squares (SQUARE shape)
 */
class PortRenderingTest {

    // ==================== T033: Port.getPortShape() returns CIRCLE ====================

    @Test
    fun `regular Port returns CIRCLE shape`() {
        // Given: A regular input port
        val port = Port(
            id = "port-1",
            name = "input",
            direction = Port.Direction.INPUT,
            dataType = String::class,
            owningNodeId = "node-1"
        )

        // When: Getting the port shape
        val shape = port.getPortShape()

        // Then: Shape should be CIRCLE
        assertEquals(PortShape.CIRCLE, shape)
    }

    @Test
    fun `regular output Port returns CIRCLE shape`() {
        // Given: A regular output port
        val port = Port(
            id = "port-2",
            name = "output",
            direction = Port.Direction.OUTPUT,
            dataType = String::class,
            owningNodeId = "node-1"
        )

        // When: Getting the port shape
        val shape = port.getPortShape()

        // Then: Shape should be CIRCLE
        assertEquals(PortShape.CIRCLE, shape)
    }

    @Test
    fun `regular Port with any data type returns CIRCLE shape`() {
        // Given: Ports with different data types
        val stringPort = Port(
            id = "port-string",
            name = "stringInput",
            direction = Port.Direction.INPUT,
            dataType = String::class,
            owningNodeId = "node-1"
        )
        val intPort = Port(
            id = "port-int",
            name = "intInput",
            direction = Port.Direction.INPUT,
            dataType = Int::class,
            owningNodeId = "node-1"
        )
        val anyPort = Port(
            id = "port-any",
            name = "anyInput",
            direction = Port.Direction.INPUT,
            dataType = Any::class,
            owningNodeId = "node-1"
        )

        // When/Then: All should return CIRCLE
        assertEquals(PortShape.CIRCLE, stringPort.getPortShape())
        assertEquals(PortShape.CIRCLE, intPort.getPortShape())
        assertEquals(PortShape.CIRCLE, anyPort.getPortShape())
    }

    // ==================== T034: PassThruPort.getPortShape() returns SQUARE ====================

    @Test
    fun `PassThruPort returns SQUARE shape`() {
        // Given: An INPUT PassThruPort
        val passThruPort = PassThruPort(
            port = Port(
                id = "passthru-1",
                name = "boundary_in",
                direction = Port.Direction.INPUT,
                dataType = String::class,
                owningNodeId = "graphNode-1"
            ),
            upstreamNodeId = "external-source",
            upstreamPortId = "output",
            downstreamNodeId = "internal-processor",
            downstreamPortId = "input"
        )

        // When: Getting the port shape
        val shape = passThruPort.getPortShape()

        // Then: Shape should be SQUARE
        assertEquals(PortShape.SQUARE, shape)
    }

    @Test
    fun `OUTPUT PassThruPort returns SQUARE shape`() {
        // Given: An OUTPUT PassThruPort
        val passThruPort = PassThruPort(
            port = Port(
                id = "passthru-2",
                name = "boundary_out",
                direction = Port.Direction.OUTPUT,
                dataType = String::class,
                owningNodeId = "graphNode-1"
            ),
            upstreamNodeId = "internal-processor",
            upstreamPortId = "output",
            downstreamNodeId = "external-sink",
            downstreamPortId = "input"
        )

        // When: Getting the port shape
        val shape = passThruPort.getPortShape()

        // Then: Shape should be SQUARE
        assertEquals(PortShape.SQUARE, shape)
    }

    @Test
    fun `PassThruPort with different data types returns SQUARE shape`() {
        // Given: PassThruPorts with different data types
        val stringPassThru = PassThruPort(
            port = Port(
                id = "passthru-string",
                name = "string_in",
                direction = Port.Direction.INPUT,
                dataType = String::class,
                owningNodeId = "graphNode-1"
            ),
            upstreamNodeId = "ext-1",
            upstreamPortId = "out",
            downstreamNodeId = "int-1",
            downstreamPortId = "in"
        )
        val intPassThru = PassThruPort(
            port = Port(
                id = "passthru-int",
                name = "int_in",
                direction = Port.Direction.INPUT,
                dataType = Int::class,
                owningNodeId = "graphNode-1"
            ),
            upstreamNodeId = "ext-2",
            upstreamPortId = "out",
            downstreamNodeId = "int-2",
            downstreamPortId = "in"
        )

        // When/Then: All PassThruPorts should return SQUARE
        assertEquals(PortShape.SQUARE, stringPassThru.getPortShape())
        assertEquals(PortShape.SQUARE, intPassThru.getPortShape())
    }

    // ==================== T035: Rendering tests for circle vs square shapes ====================

    @Test
    fun `PortShape CIRCLE and SQUARE are distinct values`() {
        // Given: The PortShape enum
        // When/Then: CIRCLE and SQUARE should be different values
        assertEquals(2, PortShape.entries.size, "PortShape should have exactly 2 values")
        assert(PortShape.CIRCLE != PortShape.SQUARE) { "CIRCLE and SQUARE must be distinct" }
    }

    @Test
    fun `PortDimensions provides correct size multiplier for squares`() {
        // Given: PortDimensions constants
        // When/Then: Square size multiplier should be greater than 1 (squares are larger)
        assert(PortDimensions.SQUARE_SIZE_MULTIPLIER > 1f) {
            "Square ports should be larger than circle ports"
        }
        assertEquals(1.75f, PortDimensions.SQUARE_SIZE_MULTIPLIER)
    }

    @Test
    fun `PortDimensions provides correct base size`() {
        // Given: PortDimensions constants
        // When/Then: Base size should be 8dp
        assertEquals(8f, PortDimensions.BASE_SIZE)
    }

    @Test
    fun `PortDimensions provides hover multiplier`() {
        // Given: PortDimensions constants
        // When/Then: Hover multiplier should be 1.5x
        assertEquals(1.5f, PortDimensions.HOVER_MULTIPLIER)
    }

    @Test
    fun `PortColors provides direction-specific colors`() {
        // Given: PortColors constants
        // When/Then: INPUT and OUTPUT colors should be different
        assert(PortColors.INPUT_FILL != PortColors.OUTPUT_FILL) {
            "INPUT and OUTPUT should have different fill colors"
        }
        assert(PortColors.INPUT_STROKE != PortColors.OUTPUT_STROKE) {
            "INPUT and OUTPUT should have different stroke colors"
        }
    }

    @Test
    fun `shape detection is consistent for same port instance`() {
        // Given: A regular port and a PassThruPort
        val regularPort = Port(
            id = "regular-1",
            name = "input",
            direction = Port.Direction.INPUT,
            dataType = String::class,
            owningNodeId = "node-1"
        )
        val passThruPort = PassThruPort(
            port = Port(
                id = "passthru-1",
                name = "boundary",
                direction = Port.Direction.INPUT,
                dataType = String::class,
                owningNodeId = "graphNode-1"
            ),
            upstreamNodeId = "ext",
            upstreamPortId = "out",
            downstreamNodeId = "int",
            downstreamPortId = "in"
        )

        // When: Getting shape multiple times
        val shape1 = regularPort.getPortShape()
        val shape2 = regularPort.getPortShape()
        val shape3 = passThruPort.getPortShape()
        val shape4 = passThruPort.getPortShape()

        // Then: Results should be consistent
        assertEquals(shape1, shape2, "Regular port shape should be consistent")
        assertEquals(shape3, shape4, "PassThruPort shape should be consistent")
        assertEquals(PortShape.CIRCLE, shape1)
        assertEquals(PortShape.SQUARE, shape3)
    }
}
