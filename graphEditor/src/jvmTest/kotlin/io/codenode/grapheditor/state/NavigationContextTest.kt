/*
 * NavigationContext Test
 * Unit tests for NavigationContext data class
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import kotlin.test.*

/**
 * TDD tests for NavigationContext.
 * These tests verify that NavigationContext correctly:
 * - Manages navigation path as a stack
 * - Provides pushInto/popOut navigation operations
 * - Calculates computed properties for current context
 * - Supports hierarchical navigation up to 5 levels deep
 */
class NavigationContextTest {

    // ============================================
    // Initial State Tests
    // ============================================

    @Test
    fun `empty NavigationContext should be at root`() {
        val context = NavigationContext()
        assertTrue(context.isAtRoot)
    }

    @Test
    fun `empty NavigationContext should have empty path`() {
        val context = NavigationContext()
        assertTrue(context.path.isEmpty())
    }

    @Test
    fun `empty NavigationContext should have null currentGraphNodeId`() {
        val context = NavigationContext()
        assertNull(context.currentGraphNodeId)
    }

    @Test
    fun `empty NavigationContext should have depth 0`() {
        val context = NavigationContext()
        assertEquals(0, context.depth)
    }

    @Test
    fun `empty NavigationContext should have null parentGraphNodeId`() {
        val context = NavigationContext()
        assertNull(context.parentGraphNodeId)
    }

    @Test
    fun `empty NavigationContext should not allow navigation out`() {
        val context = NavigationContext()
        assertFalse(context.canNavigateOut)
    }

    // ============================================
    // pushInto Tests
    // ============================================

    @Test
    fun `pushInto should add graphNodeId to path`() {
        val context = NavigationContext()
        val newContext = context.pushInto("graphNode1")

        assertEquals(listOf("graphNode1"), newContext.path)
    }

    @Test
    fun `pushInto should update currentGraphNodeId`() {
        val context = NavigationContext()
        val newContext = context.pushInto("graphNode1")

        assertEquals("graphNode1", newContext.currentGraphNodeId)
    }

    @Test
    fun `pushInto should no longer be at root`() {
        val context = NavigationContext()
        val newContext = context.pushInto("graphNode1")

        assertFalse(newContext.isAtRoot)
    }

    @Test
    fun `pushInto should allow navigation out`() {
        val context = NavigationContext()
        val newContext = context.pushInto("graphNode1")

        assertTrue(newContext.canNavigateOut)
    }

    @Test
    fun `pushInto should increment depth`() {
        val context = NavigationContext()
        val newContext = context.pushInto("graphNode1")

        assertEquals(1, newContext.depth)
    }

    @Test
    fun `pushInto multiple times should build path`() {
        val context = NavigationContext()
            .pushInto("graphNode1")
            .pushInto("graphNode2")
            .pushInto("graphNode3")

        assertEquals(listOf("graphNode1", "graphNode2", "graphNode3"), context.path)
        assertEquals(3, context.depth)
        assertEquals("graphNode3", context.currentGraphNodeId)
    }

    @Test
    fun `pushInto should support 5 levels of nesting`() {
        val context = NavigationContext()
            .pushInto("level1")
            .pushInto("level2")
            .pushInto("level3")
            .pushInto("level4")
            .pushInto("level5")

        assertEquals(5, context.depth)
        assertEquals("level5", context.currentGraphNodeId)
        assertEquals("level4", context.parentGraphNodeId)
    }

    // ============================================
    // popOut Tests
    // ============================================

    @Test
    fun `popOut should remove last graphNodeId from path`() {
        val context = NavigationContext(path = listOf("graphNode1", "graphNode2"))
        val newContext = context.popOut()

        assertEquals(listOf("graphNode1"), newContext.path)
    }

    @Test
    fun `popOut should update currentGraphNodeId to parent`() {
        val context = NavigationContext(path = listOf("graphNode1", "graphNode2"))
        val newContext = context.popOut()

        assertEquals("graphNode1", newContext.currentGraphNodeId)
    }

    @Test
    fun `popOut to root should set currentGraphNodeId to null`() {
        val context = NavigationContext(path = listOf("graphNode1"))
        val newContext = context.popOut()

        assertNull(newContext.currentGraphNodeId)
        assertTrue(newContext.isAtRoot)
    }

    @Test
    fun `popOut should decrement depth`() {
        val context = NavigationContext(path = listOf("graphNode1", "graphNode2"))
        val newContext = context.popOut()

        assertEquals(1, newContext.depth)
    }

    @Test
    fun `popOut at root should remain at root`() {
        val context = NavigationContext()
        val newContext = context.popOut()

        assertTrue(newContext.isAtRoot)
        assertEquals(0, newContext.depth)
    }

    // ============================================
    // reset Tests
    // ============================================

    @Test
    fun `reset should return to root`() {
        val context = NavigationContext(path = listOf("graphNode1", "graphNode2", "graphNode3"))
        val newContext = context.reset()

        assertTrue(newContext.isAtRoot)
        assertTrue(newContext.path.isEmpty())
        assertEquals(0, newContext.depth)
    }

    @Test
    fun `reset on empty context should remain at root`() {
        val context = NavigationContext()
        val newContext = context.reset()

        assertTrue(newContext.isAtRoot)
    }

    // ============================================
    // parentGraphNodeId Tests
    // ============================================

    @Test
    fun `parentGraphNodeId should be null at depth 1`() {
        val context = NavigationContext(path = listOf("graphNode1"))
        assertNull(context.parentGraphNodeId)
    }

    @Test
    fun `parentGraphNodeId should return second-to-last at depth 2`() {
        val context = NavigationContext(path = listOf("graphNode1", "graphNode2"))
        assertEquals("graphNode1", context.parentGraphNodeId)
    }

    @Test
    fun `parentGraphNodeId should return second-to-last at deeper levels`() {
        val context = NavigationContext(path = listOf("level1", "level2", "level3", "level4"))
        assertEquals("level3", context.parentGraphNodeId)
    }

    // ============================================
    // contains Tests
    // ============================================

    @Test
    fun `contains should return true for graphNodeId in path`() {
        val context = NavigationContext(path = listOf("graphNode1", "graphNode2", "graphNode3"))
        assertTrue(context.contains("graphNode2"))
    }

    @Test
    fun `contains should return false for graphNodeId not in path`() {
        val context = NavigationContext(path = listOf("graphNode1", "graphNode2"))
        assertFalse(context.contains("graphNode3"))
    }

    @Test
    fun `contains should return false for empty path`() {
        val context = NavigationContext()
        assertFalse(context.contains("graphNode1"))
    }

    // ============================================
    // Immutability Tests
    // ============================================

    @Test
    fun `pushInto should not modify original context`() {
        val original = NavigationContext()
        val newContext = original.pushInto("graphNode1")

        assertTrue(original.isAtRoot)
        assertFalse(newContext.isAtRoot)
    }

    @Test
    fun `popOut should not modify original context`() {
        val original = NavigationContext(path = listOf("graphNode1", "graphNode2"))
        val newContext = original.popOut()

        assertEquals(2, original.depth)
        assertEquals(1, newContext.depth)
    }

    // ============================================
    // Edge Cases
    // ============================================

    @Test
    fun `path should preserve order`() {
        val context = NavigationContext()
            .pushInto("first")
            .pushInto("second")
            .pushInto("third")

        assertEquals("first", context.path[0])
        assertEquals("second", context.path[1])
        assertEquals("third", context.path[2])
    }
}
