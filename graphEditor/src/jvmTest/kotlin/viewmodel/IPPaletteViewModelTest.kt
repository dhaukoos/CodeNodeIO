/*
 * IPPaletteViewModelTest - Unit tests for IPPaletteViewModel
 * Verifies IP type selection and search functionality without Compose UI dependencies
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import io.codenode.fbpdsl.model.InformationPacketType
import io.codenode.fbpdsl.model.IPColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IPPaletteViewModelTest {

    private fun createTestIPType(id: String, name: String, description: String? = null): InformationPacketType {
        return InformationPacketType(
            id = id,
            typeName = name,
            payloadType = String::class,
            color = IPColor(255, 0, 0),
            description = description ?: "Test type $name"
        )
    }

    @Test
    fun `initial state has no selection and empty search`() = runTest {
        val viewModel = IPPaletteViewModel()
        val state = viewModel.state.first()

        assertNull(state.selectedTypeId)
        assertEquals("", state.searchQuery)
    }

    @Test
    fun `selectType updates selectedTypeId and calls callback`() = runTest {
        var selectedType: InformationPacketType? = null

        val viewModel = IPPaletteViewModel(
            onTypeSelected = { selectedType = it }
        )

        val testType = createTestIPType("type1", "String")
        viewModel.selectType(testType)

        val state = viewModel.state.first()
        assertEquals("type1", state.selectedTypeId)
        assertEquals(testType, selectedType)
    }

    @Test
    fun `clearSelection resets selectedTypeId and calls callback with null`() = runTest {
        var selectedType: InformationPacketType? = createTestIPType("dummy", "Dummy")

        val viewModel = IPPaletteViewModel(
            onTypeSelected = { selectedType = it }
        )

        // First select a type
        viewModel.selectType(createTestIPType("type1", "String"))
        assertEquals("type1", viewModel.state.first().selectedTypeId)

        // Then clear
        viewModel.clearSelection()

        val state = viewModel.state.first()
        assertNull(state.selectedTypeId)
        assertNull(selectedType)
    }

    @Test
    fun `setSearchQuery updates state`() = runTest {
        val viewModel = IPPaletteViewModel()

        viewModel.setSearchQuery("string")

        val state = viewModel.state.first()
        assertEquals("string", state.searchQuery)
    }

    @Test
    fun `clearSearch resets to empty`() = runTest {
        val viewModel = IPPaletteViewModel()

        viewModel.setSearchQuery("test")
        viewModel.clearSearch()

        val state = viewModel.state.first()
        assertEquals("", state.searchQuery)
    }

    @Test
    fun `toggleSelection selects when not selected`() = runTest {
        var selectedType: InformationPacketType? = null

        val viewModel = IPPaletteViewModel(
            onTypeSelected = { selectedType = it }
        )

        val testType = createTestIPType("type1", "String")
        viewModel.toggleSelection(testType)

        assertEquals("type1", viewModel.state.first().selectedTypeId)
        assertEquals(testType, selectedType)
    }

    @Test
    fun `toggleSelection deselects when already selected`() = runTest {
        var selectedType: InformationPacketType? = null

        val viewModel = IPPaletteViewModel(
            onTypeSelected = { selectedType = it }
        )

        val testType = createTestIPType("type1", "String")

        // Select first
        viewModel.selectType(testType)
        assertEquals("type1", viewModel.state.first().selectedTypeId)

        // Toggle should deselect
        viewModel.toggleSelection(testType)

        assertNull(viewModel.state.first().selectedTypeId)
        assertNull(selectedType)
    }

    @Test
    fun `matchesSearch returns true for empty query`() = runTest {
        val viewModel = IPPaletteViewModel()
        val testType = createTestIPType("type1", "String")

        assertTrue(viewModel.matchesSearch(testType))
    }

    @Test
    fun `matchesSearch matches type name case-insensitively`() = runTest {
        val viewModel = IPPaletteViewModel()

        viewModel.setSearchQuery("STRING")

        val testType = createTestIPType("type1", "String")
        assertTrue(viewModel.matchesSearch(testType))

        val nonMatchingType = createTestIPType("type2", "Integer")
        assertFalse(viewModel.matchesSearch(nonMatchingType))
    }

    @Test
    fun `matchesSearch matches description case-insensitively`() = runTest {
        val viewModel = IPPaletteViewModel()

        viewModel.setSearchQuery("numeric")

        val testType = createTestIPType("type1", "Int", description = "A numeric integer type")
        assertTrue(viewModel.matchesSearch(testType))

        val nonMatchingType = createTestIPType("type2", "String", description = "Text data")
        assertFalse(viewModel.matchesSearch(nonMatchingType))
    }

    @Test
    fun `selecting different type updates selectedTypeId`() = runTest {
        val viewModel = IPPaletteViewModel()

        val type1 = createTestIPType("type1", "String")
        val type2 = createTestIPType("type2", "Integer")

        viewModel.selectType(type1)
        assertEquals("type1", viewModel.state.first().selectedTypeId)

        viewModel.selectType(type2)
        assertEquals("type2", viewModel.state.first().selectedTypeId)
    }

    @Test
    fun `callback is invoked on each selection change`() = runTest {
        val selectedTypes = mutableListOf<InformationPacketType?>()

        val viewModel = IPPaletteViewModel(
            onTypeSelected = { selectedTypes.add(it) }
        )

        val type1 = createTestIPType("type1", "String")
        val type2 = createTestIPType("type2", "Integer")

        viewModel.selectType(type1)
        viewModel.selectType(type2)
        viewModel.clearSelection()

        assertEquals(3, selectedTypes.size)
        assertEquals(type1, selectedTypes[0])
        assertEquals(type2, selectedTypes[1])
        assertNull(selectedTypes[2])
    }
}
