/*
 * ConnectionSegment Unit Tests
 * Tests for segment data structure and Connection.segments property
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class ConnectionSegmentTest {

    // ==================== ConnectionSegment Data Class Tests ====================

    @Test
    fun `ConnectionSegment stores all required properties`() {
        val segment = ConnectionSegment(
            id = "segment-1",
            sourceNodeId = "node-a",
            sourcePortId = "port-out",
            targetNodeId = "node-b",
            targetPortId = "port-in",
            scopeNodeId = null,
            parentConnectionId = "conn-1"
        )

        assertEquals("segment-1", segment.id)
        assertEquals("node-a", segment.sourceNodeId)
        assertEquals("port-out", segment.sourcePortId)
        assertEquals("node-b", segment.targetNodeId)
        assertEquals("port-in", segment.targetPortId)
        assertNull(segment.scopeNodeId)
        assertEquals("conn-1", segment.parentConnectionId)
    }

    @Test
    fun `ConnectionSegment with root scope has null scopeNodeId`() {
        val segment = ConnectionSegment(
            id = "seg-root",
            sourceNodeId = "a",
            sourcePortId = "out",
            targetNodeId = "b",
            targetPortId = "in",
            scopeNodeId = null,
            parentConnectionId = "conn-1"
        )

        assertNull(segment.scopeNodeId)
    }

    @Test
    fun `ConnectionSegment inside GraphNode has non-null scopeNodeId`() {
        val segment = ConnectionSegment(
            id = "seg-interior",
            sourceNodeId = "passthru",
            sourcePortId = "in",
            targetNodeId = "internal-node",
            targetPortId = "input",
            scopeNodeId = "graphNode-1",
            parentConnectionId = "conn-1"
        )

        assertEquals("graphNode-1", segment.scopeNodeId)
    }

    @Test
    fun `ConnectionSegment equality based on all properties`() {
        val seg1 = ConnectionSegment(
            id = "seg-1",
            sourceNodeId = "a",
            sourcePortId = "out",
            targetNodeId = "b",
            targetPortId = "in",
            scopeNodeId = null,
            parentConnectionId = "conn-1"
        )

        val seg2 = ConnectionSegment(
            id = "seg-1",
            sourceNodeId = "a",
            sourcePortId = "out",
            targetNodeId = "b",
            targetPortId = "in",
            scopeNodeId = null,
            parentConnectionId = "conn-1"
        )

        assertEquals(seg1, seg2)
    }

    @Test
    fun `ConnectionSegment inequality when scope differs`() {
        val seg1 = ConnectionSegment(
            id = "seg-1",
            sourceNodeId = "a",
            sourcePortId = "out",
            targetNodeId = "b",
            targetPortId = "in",
            scopeNodeId = null,
            parentConnectionId = "conn-1"
        )

        val seg2 = ConnectionSegment(
            id = "seg-1",
            sourceNodeId = "a",
            sourcePortId = "out",
            targetNodeId = "b",
            targetPortId = "in",
            scopeNodeId = "graphNode-1",
            parentConnectionId = "conn-1"
        )

        assertNotEquals(seg1, seg2)
    }

    // ==================== Connection.segments Property Tests (T006) ====================

    @Test
    fun `Direct connection has single segment`() {
        // CodeNode A -> CodeNode B (no GraphNode boundary)
        val connection = Connection(
            id = "conn-direct",
            sourceNodeId = "nodeA",
            sourcePortId = "output",
            targetNodeId = "nodeB",
            targetPortId = "input"
        )

        val segments = connection.segments

        assertEquals(1, segments.size)
        assertEquals("nodeA", segments[0].sourceNodeId)
        assertEquals("output", segments[0].sourcePortId)
        assertEquals("nodeB", segments[0].targetNodeId)
        assertEquals("input", segments[0].targetPortId)
        assertNull(segments[0].scopeNodeId, "Direct connection segment should be at root level")
    }

    @Test
    fun `Connection segments are cached after first access`() {
        val connection = Connection(
            id = "conn-cached",
            sourceNodeId = "nodeA",
            sourcePortId = "output",
            targetNodeId = "nodeB",
            targetPortId = "input"
        )

        val segments1 = connection.segments
        val segments2 = connection.segments

        // Should return same instance (cached)
        assertTrue(segments1 === segments2, "Segments should be cached and return same instance")
    }

    @Test
    fun `Connection invalidateSegments clears cache`() {
        val connection = Connection(
            id = "conn-invalidate",
            sourceNodeId = "nodeA",
            sourcePortId = "output",
            targetNodeId = "nodeB",
            targetPortId = "input"
        )

        val segments1 = connection.segments
        connection.invalidateSegments()
        val segments2 = connection.segments

        // After invalidation, should compute new instance
        assertTrue(segments1 !== segments2, "After invalidation, segments should be recomputed")
    }

    @Test
    fun `Segment first source matches connection source`() {
        val connection = Connection(
            id = "conn-source-match",
            sourceNodeId = "sourceNode",
            sourcePortId = "sourcePort",
            targetNodeId = "targetNode",
            targetPortId = "targetPort"
        )

        val segments = connection.segments

        assertEquals(connection.sourceNodeId, segments.first().sourceNodeId)
        assertEquals(connection.sourcePortId, segments.first().sourcePortId)
    }

    @Test
    fun `Segment last target matches connection target`() {
        val connection = Connection(
            id = "conn-target-match",
            sourceNodeId = "sourceNode",
            sourcePortId = "sourcePort",
            targetNodeId = "targetNode",
            targetPortId = "targetPort"
        )

        val segments = connection.segments

        assertEquals(connection.targetNodeId, segments.last().targetNodeId)
        assertEquals(connection.targetPortId, segments.last().targetPortId)
    }

    @Test
    fun `All segments reference parent connection`() {
        val connection = Connection(
            id = "conn-parent-ref",
            sourceNodeId = "nodeA",
            sourcePortId = "output",
            targetNodeId = "nodeB",
            targetPortId = "input"
        )

        val segments = connection.segments

        segments.forEach { segment ->
            assertEquals("conn-parent-ref", segment.parentConnectionId)
        }
    }

    // ==================== Segment Chain Validation Tests ====================

    @Test
    fun `validateSegmentChain returns success for valid single segment`() {
        val connection = Connection(
            id = "conn-valid",
            sourceNodeId = "nodeA",
            sourcePortId = "output",
            targetNodeId = "nodeB",
            targetPortId = "input"
        )

        val result = connection.validateSegmentChain()

        assertTrue(result.success, "Single segment chain should be valid: ${result.errorMessage()}")
    }

    @Test
    fun `Connection with segments not empty`() {
        val connection = Connection(
            id = "conn-not-empty",
            sourceNodeId = "nodeA",
            sourcePortId = "output",
            targetNodeId = "nodeB",
            targetPortId = "input"
        )

        val segments = connection.segments

        assertTrue(segments.isNotEmpty(), "Connection must have at least one segment")
    }
}
