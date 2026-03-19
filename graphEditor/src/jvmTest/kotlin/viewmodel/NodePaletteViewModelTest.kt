/*
 * NodePaletteViewModelTest - Unit tests for NodePaletteViewModel
 * Verifies search and category expansion without Compose UI dependencies
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import io.codenode.fbpdsl.model.NodeTypeDefinition
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

        viewModel.toggleCategory(NodeTypeDefinition.NodeCategory.SERVICE)

        val state = viewModel.state.first()
        assertTrue(state.expandedCategories.contains(NodeTypeDefinition.NodeCategory.SERVICE))
    }

    @Test
    fun `toggleCategory removes category when already expanded`() = runTest {
        val viewModel = createViewModel()

        // Expand
        viewModel.toggleCategory(NodeTypeDefinition.NodeCategory.SERVICE)
        assertTrue(viewModel.state.first().expandedCategories.contains(NodeTypeDefinition.NodeCategory.SERVICE))

        // Collapse
        viewModel.toggleCategory(NodeTypeDefinition.NodeCategory.SERVICE)
        assertFalse(viewModel.state.first().expandedCategories.contains(NodeTypeDefinition.NodeCategory.SERVICE))
    }

    @Test
    fun `expandCategory adds category without toggle`() = runTest {
        val viewModel = createViewModel()

        viewModel.expandCategory(NodeTypeDefinition.NodeCategory.TRANSFORMER)
        assertTrue(viewModel.state.first().expandedCategories.contains(NodeTypeDefinition.NodeCategory.TRANSFORMER))

        // Calling again should not collapse
        viewModel.expandCategory(NodeTypeDefinition.NodeCategory.TRANSFORMER)
        assertTrue(viewModel.state.first().expandedCategories.contains(NodeTypeDefinition.NodeCategory.TRANSFORMER))
    }

    @Test
    fun `collapseCategory removes category`() = runTest {
        val viewModel = createViewModel()

        viewModel.expandCategory(NodeTypeDefinition.NodeCategory.SERVICE)
        viewModel.collapseCategory(NodeTypeDefinition.NodeCategory.SERVICE)

        assertFalse(viewModel.state.first().expandedCategories.contains(NodeTypeDefinition.NodeCategory.SERVICE))
    }

    @Test
    fun `collapseAllCategories clears all expanded categories`() = runTest {
        val viewModel = createViewModel()

        viewModel.expandCategory(NodeTypeDefinition.NodeCategory.SERVICE)
        viewModel.expandCategory(NodeTypeDefinition.NodeCategory.TRANSFORMER)
        viewModel.expandCategory(NodeTypeDefinition.NodeCategory.GENERIC)

        viewModel.collapseAllCategories()

        assertTrue(viewModel.state.first().expandedCategories.isEmpty())
    }

    @Test
    fun `multiple categories can be expanded simultaneously`() = runTest {
        val viewModel = createViewModel()

        viewModel.expandCategory(NodeTypeDefinition.NodeCategory.SERVICE)
        viewModel.expandCategory(NodeTypeDefinition.NodeCategory.TRANSFORMER)
        viewModel.expandCategory(NodeTypeDefinition.NodeCategory.VALIDATOR)

        val state = viewModel.state.first()
        assertEquals(3, state.expandedCategories.size)
        assertTrue(state.expandedCategories.contains(NodeTypeDefinition.NodeCategory.SERVICE))
        assertTrue(state.expandedCategories.contains(NodeTypeDefinition.NodeCategory.TRANSFORMER))
        assertTrue(state.expandedCategories.contains(NodeTypeDefinition.NodeCategory.VALIDATOR))
    }
}
