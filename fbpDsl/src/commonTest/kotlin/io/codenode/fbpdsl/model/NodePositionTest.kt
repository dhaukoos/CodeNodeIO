/*
 * NodePositionTest - Tests for Node.Position coordinate space
 * Verifies that Node.Position accepts any signed coordinate values
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlin.test.Test
import kotlin.test.assertEquals

class NodePositionTest {

    // T002: Negative coordinates should be accepted
    @Test
    fun `Position with negative x and y should be valid`() {
        val pos = Node.Position(-10.0, -20.0)
        assertEquals(-10.0, pos.x)
        assertEquals(-20.0, pos.y)
    }

    // T003: Single-axis negative values
    @Test
    fun `Position with negative x and zero y should be valid`() {
        val pos = Node.Position(-0.5, 0.0)
        assertEquals(-0.5, pos.x)
        assertEquals(0.0, pos.y)
    }

    @Test
    fun `Position with zero x and negative y should be valid`() {
        val pos = Node.Position(0.0, -0.5)
        assertEquals(0.0, pos.x)
        assertEquals(-0.5, pos.y)
    }

    // T004: Positive values remain valid
    @Test
    fun `Position with positive x and y should be valid`() {
        val pos = Node.Position(100.0, 200.0)
        assertEquals(100.0, pos.x)
        assertEquals(200.0, pos.y)
    }

    @Test
    fun `ORIGIN should be at zero zero`() {
        assertEquals(0.0, Node.Position.ORIGIN.x)
        assertEquals(0.0, Node.Position.ORIGIN.y)
    }

    @Test
    fun `Position with large negative values should be valid`() {
        val pos = Node.Position(-9999.0, -9999.0)
        assertEquals(-9999.0, pos.x)
        assertEquals(-9999.0, pos.y)
    }
}
