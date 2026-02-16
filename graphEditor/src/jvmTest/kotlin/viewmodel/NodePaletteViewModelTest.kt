/*
 * NodePaletteViewModelTest - Unit tests for NodePaletteViewModel
 * Verifies search, category expansion, and custom node deletion without Compose UI dependencies
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import io.codenode.fbpdsl.model.NodeTypeDefinition
import io.codenode.grapheditor.repository.CustomNodeDefinition
import io.codenode.grapheditor.repository.CustomNodeRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Fake repository for testing - stores nodes in memory
 */
class FakePaletteNodeRepository : CustomNodeRepository {
    private val nodes = mutableListOf<CustomNodeDefinition>()

    override fun getAll(): List<CustomNodeDefinition> = nodes.toList()

    override fun add(node: CustomNodeDefinition) {
        nodes.add(node)
    }

    override fun load() {
        // No-op for testing
    }

    override fun save() {
        // No-op for testing
    }

    override fun remove(id: String): Boolean {
        return nodes.removeIf { it.id == id }
    }

    fun addTestNode(name: String) {
        val node = CustomNodeDefinition.create(name = name, inputCount = 1, outputCount = 1)
        nodes.add(node)
    }
}

class NodePaletteViewModelTest {

    private fun createViewModel(
        repository: CustomNodeRepository = FakePaletteNodeRepository(),
        onCustomNodesChanged: () -> Unit = {}
    ): NodePaletteViewModel {
        return NodePaletteViewModel(repository, onCustomNodesChanged)
    }

    @Test
    fun `initial state has empty search query`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.state.first()

        assertEquals("", state.searchQuery)
        assertTrue(state.expandedCategories.isEmpty())
        assertTrue(state.deletableNodeNames.isEmpty())
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
    fun `updateDeletableNodeNames updates state`() = runTest {
        val viewModel = createViewModel()

        val nodeNames = setOf("CustomNode1", "CustomNode2")
        viewModel.updateDeletableNodeNames(nodeNames)

        val state = viewModel.state.first()
        assertEquals(nodeNames, state.deletableNodeNames)
    }

    @Test
    fun `deleteCustomNode removes from repository and calls callback`() = runTest {
        val repository = FakePaletteNodeRepository()
        repository.addTestNode("MyCustomNode")

        var callbackCalled = false
        val viewModel = createViewModel(
            repository = repository,
            onCustomNodesChanged = { callbackCalled = true }
        )

        viewModel.updateDeletableNodeNames(setOf("MyCustomNode"))
        val deleted = viewModel.deleteCustomNode("MyCustomNode")

        assertTrue(deleted)
        assertTrue(callbackCalled)
        assertEquals(0, repository.getAll().size)
    }

    @Test
    fun `deleteCustomNode returns false for non-existent node`() = runTest {
        val repository = FakePaletteNodeRepository()
        val viewModel = createViewModel(repository)

        val deleted = viewModel.deleteCustomNode("NonExistent")

        assertFalse(deleted)
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
