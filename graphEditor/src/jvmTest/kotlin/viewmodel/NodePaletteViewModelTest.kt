/*
 * NodePaletteViewModelTest - Unit tests for NodePaletteViewModel
 * Verifies search and category expansion without Compose UI dependencies
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import io.codenode.fbpdsl.model.CodeNodeType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NodePaletteViewModelTest {

    private fun createViewModel(): NodePaletteViewModel {
        return NodePaletteViewModel()
    }

    @Test
    fun `initial state has empty search query`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.state.first()

        assertEquals("", state.searchQuery)
        assertTrue(state.expandedCategories.isEmpty())
    }

    @Test
    fun `setSearchQuery updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.setSearchQuery("transformer")

        val state = viewModel.state.first()
        assertEquals("transformer", state.searchQuery)
    }

    @Test
    fun `clearSearch resets to empty`() = runTest {
        val viewModel = createViewModel()

        viewModel.setSearchQuery("test")
        viewModel.clearSearch()

        val state = viewModel.state.first()
        assertEquals("", state.searchQuery)
    }

    @Test
    fun `toggleCategory adds category when not expanded`() = runTest {
        val viewModel = createViewModel()

        viewModel.toggleCategory(CodeNodeType.TRANSFORMER)

        val state = viewModel.state.first()
        assertTrue(state.expandedCategories.contains(CodeNodeType.TRANSFORMER))
    }

    @Test
    fun `toggleCategory removes category when already expanded`() = runTest {
        val viewModel = createViewModel()

        // Expand
        viewModel.toggleCategory(CodeNodeType.TRANSFORMER)
        assertTrue(viewModel.state.first().expandedCategories.contains(CodeNodeType.TRANSFORMER))

        // Collapse
        viewModel.toggleCategory(CodeNodeType.TRANSFORMER)
        assertFalse(viewModel.state.first().expandedCategories.contains(CodeNodeType.TRANSFORMER))
    }

    @Test
    fun `expandCategory adds category without toggle`() = runTest {
        val viewModel = createViewModel()

        viewModel.expandCategory(CodeNodeType.TRANSFORMER)
        assertTrue(viewModel.state.first().expandedCategories.contains(CodeNodeType.TRANSFORMER))

        // Calling again should not collapse
        viewModel.expandCategory(CodeNodeType.TRANSFORMER)
        assertTrue(viewModel.state.first().expandedCategories.contains(CodeNodeType.TRANSFORMER))
    }

    @Test
    fun `collapseCategory removes category`() = runTest {
        val viewModel = createViewModel()

        viewModel.expandCategory(CodeNodeType.TRANSFORMER)
        viewModel.collapseCategory(CodeNodeType.TRANSFORMER)

        assertFalse(viewModel.state.first().expandedCategories.contains(CodeNodeType.TRANSFORMER))
    }

    @Test
    fun `collapseAllCategories clears all expanded categories`() = runTest {
        val viewModel = createViewModel()

        viewModel.expandCategory(CodeNodeType.SOURCE)
        viewModel.expandCategory(CodeNodeType.TRANSFORMER)
        viewModel.expandCategory(CodeNodeType.VALIDATOR)

        viewModel.collapseAllCategories()

        assertTrue(viewModel.state.first().expandedCategories.isEmpty())
    }

    @Test
    fun `multiple categories can be expanded simultaneously`() = runTest {
        val viewModel = createViewModel()

        viewModel.expandCategory(CodeNodeType.SOURCE)
        viewModel.expandCategory(CodeNodeType.TRANSFORMER)
        viewModel.expandCategory(CodeNodeType.VALIDATOR)

        val state = viewModel.state.first()
        assertEquals(3, state.expandedCategories.size)
        assertTrue(state.expandedCategories.contains(CodeNodeType.SOURCE))
        assertTrue(state.expandedCategories.contains(CodeNodeType.TRANSFORMER))
        assertTrue(state.expandedCategories.contains(CodeNodeType.VALIDATOR))
    }
}
