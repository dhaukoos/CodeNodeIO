/*
 * SelectionState Test
 * Unit tests for SelectionState data class
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.*

/**
 * TDD tests for SelectionState.
 * These tests verify that SelectionState correctly:
 * - Manages selected node IDs
 * - Manages selected connection IDs
 * - Tracks rectangular selection state
 * - Provides computed properties for selection status
 */
class SelectionStateTest {

    // ============================================
    // Initial State Tests
    // ============================================

    @Test
    fun `empty SelectionState should have no selected nodes`() {
        val state = SelectionState()
        assertTrue(state.selectedNodeIds.isEmpty())
    }

    @Test
    fun `empty SelectionState should have no selected connections`() {
        val state = SelectionState()
        assertTrue(state.selectedConnectionIds.isEmpty())
    }

    @Test
    fun `empty SelectionState should not have active rectangular selection`() {
        val state = SelectionState()
        assertFalse(state.isRectangularSelectionActive)
    }

    @Test
    fun `empty SelectionState should have null selection box bounds`() {
        val state = SelectionState()
        assertNull(state.selectionBoxBounds)
    }

    // ============================================
    // Node Selection Tests
    // ============================================

    @Test
    fun `SelectionState should store selected node IDs`() {
        val state = SelectionState(
            selectedNodeIds = setOf("node1", "node2", "node3")
        )
        assertEquals(3, state.selectedNodeIds.size)
        assertTrue(state.selectedNodeIds.contains("node1"))
        assertTrue(state.selectedNodeIds.contains("node2"))
        assertTrue(state.selectedNodeIds.contains("node3"))
    }

    @Test
    fun `hasNodeSelection should return true when nodes are selected`() {
        val state = SelectionState(selectedNodeIds = setOf("node1"))
        assertTrue(state.hasNodeSelection)
    }

    @Test
    fun `hasNodeSelection should return false when no nodes are selected`() {
        val state = SelectionState()
        assertFalse(state.hasNodeSelection)
    }

    @Test
    fun `nodeSelectionCount should return correct count`() {
        val state = SelectionState(selectedNodeIds = setOf("node1", "node2", "node3"))
        assertEquals(3, state.nodeSelectionCount)
    }

    // ============================================
    // Connection Selection Tests
    // ============================================

    @Test
    fun `SelectionState should store selected connection IDs`() {
        val state = SelectionState(
            selectedConnectionIds = setOf("conn1", "conn2")
        )
        assertEquals(2, state.selectedConnectionIds.size)
        assertTrue(state.selectedConnectionIds.contains("conn1"))
        assertTrue(state.selectedConnectionIds.contains("conn2"))
    }

    @Test
    fun `hasConnectionSelection should return true when connections are selected`() {
        val state = SelectionState(selectedConnectionIds = setOf("conn1"))
        assertTrue(state.hasConnectionSelection)
    }

    @Test
    fun `hasConnectionSelection should return false when no connections are selected`() {
        val state = SelectionState()
        assertFalse(state.hasConnectionSelection)
    }

    @Test
    fun `connectionSelectionCount should return correct count`() {
        val state = SelectionState(selectedConnectionIds = setOf("conn1", "conn2"))
        assertEquals(2, state.connectionSelectionCount)
    }

    // ============================================
    // Combined Selection Tests
    // ============================================

    @Test
    fun `hasSelection should return true when nodes are selected`() {
        val state = SelectionState(selectedNodeIds = setOf("node1"))
        assertTrue(state.hasSelection)
    }

    @Test
    fun `hasSelection should return true when connections are selected`() {
        val state = SelectionState(selectedConnectionIds = setOf("conn1"))
        assertTrue(state.hasSelection)
    }

    @Test
    fun `hasSelection should return true when both nodes and connections are selected`() {
        val state = SelectionState(
            selectedNodeIds = setOf("node1"),
            selectedConnectionIds = setOf("conn1")
        )
        assertTrue(state.hasSelection)
    }

    @Test
    fun `hasSelection should return false when nothing is selected`() {
        val state = SelectionState()
        assertFalse(state.hasSelection)
    }

    @Test
    fun `totalSelectionCount should sum nodes and connections`() {
        val state = SelectionState(
            selectedNodeIds = setOf("node1", "node2"),
            selectedConnectionIds = setOf("conn1", "conn2", "conn3")
        )
        assertEquals(5, state.totalSelectionCount)
    }

    // ============================================
    // canGroup Tests
    // ============================================

    @Test
    fun `canGroup should return true when 2 or more nodes are selected`() {
        val state = SelectionState(selectedNodeIds = setOf("node1", "node2"))
        assertTrue(state.canGroup)
    }

    @Test
    fun `canGroup should return false when fewer than 2 nodes are selected`() {
        val state = SelectionState(selectedNodeIds = setOf("node1"))
        assertFalse(state.canGroup)
    }

    @Test
    fun `canGroup should return false when no nodes are selected`() {
        val state = SelectionState()
        assertFalse(state.canGroup)
    }

    @Test
    fun `canGroup should ignore connection selection count`() {
        val state = SelectionState(
            selectedNodeIds = setOf("node1"),
            selectedConnectionIds = setOf("conn1", "conn2", "conn3")
        )
        assertFalse(state.canGroup) // Only 1 node selected, connections don't count
    }

    // ============================================
    // Selection Box Tests
    // ============================================

    @Test
    fun `selectionBoxBounds should be null when selection box is not active`() {
        val state = SelectionState()
        assertNull(state.selectionBoxBounds)
    }

    @Test
    fun `selectionBoxBounds should return Rect when both start and end are set`() {
        val state = SelectionState(
            selectionBoxStart = Offset(10f, 20f),
            selectionBoxEnd = Offset(100f, 150f),
            isRectangularSelectionActive = true
        )
        val bounds = state.selectionBoxBounds
        assertNotNull(bounds)
        assertEquals(10f, bounds.left)
        assertEquals(20f, bounds.top)
        assertEquals(100f, bounds.right)
        assertEquals(150f, bounds.bottom)
    }

    @Test
    fun `selectionBoxBounds should handle reversed coordinates`() {
        // When user drags from bottom-right to top-left
        val state = SelectionState(
            selectionBoxStart = Offset(100f, 150f),
            selectionBoxEnd = Offset(10f, 20f),
            isRectangularSelectionActive = true
        )
        val bounds = state.selectionBoxBounds
        assertNotNull(bounds)
        // Rect normalizes to proper min/max
        assertEquals(10f, bounds.left)
        assertEquals(20f, bounds.top)
        assertEquals(100f, bounds.right)
        assertEquals(150f, bounds.bottom)
    }

    @Test
    fun `selectionBoxBounds should be null when only start is set`() {
        val state = SelectionState(
            selectionBoxStart = Offset(10f, 20f),
            selectionBoxEnd = null
        )
        assertNull(state.selectionBoxBounds)
    }

    @Test
    fun `selectionBoxBounds should be null when only end is set`() {
        val state = SelectionState(
            selectionBoxStart = null,
            selectionBoxEnd = Offset(100f, 150f)
        )
        assertNull(state.selectionBoxBounds)
    }

    // ============================================
    // Immutability Tests
    // ============================================

    @Test
    fun `copy should create independent instance`() {
        val original = SelectionState(selectedNodeIds = setOf("node1"))
        val copy = original.copy(selectedNodeIds = setOf("node2"))

        assertEquals(setOf("node1"), original.selectedNodeIds)
        assertEquals(setOf("node2"), copy.selectedNodeIds)
    }
}
