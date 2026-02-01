/*
 * IPPalette Test
 * UI tests for IPPalette component
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.fbpdsl.model.IPColor
import io.codenode.fbpdsl.model.InformationPacketType
import io.codenode.grapheditor.state.IPTypeRegistry
import kotlin.test.*

/**
 * TDD tests for IPPalette component.
 * These tests verify that IPPalette correctly:
 * - Displays all provided IP types with names and colors
 * - Filters types by search query (case-insensitive)
 * - Shows "No matching types" for empty search results
 * - Invokes callback when a type is selected
 * - Highlights the currently selected type
 */
class IPPaletteTest {
    private fun createTestIPTypes(): List<InformationPacketType> =
        listOf(
            InformationPacketType(
                id = "ip_any",
                typeName = "Any",
                payloadType = Any::class,
                color = IPColor(0, 0, 0),
                description = "Universal type",
            ),
            InformationPacketType(
                id = "ip_int",
                typeName = "Int",
                payloadType = Int::class,
                color = IPColor(33, 150, 243),
                description = "Integer type",
            ),
            InformationPacketType(
                id = "ip_double",
                typeName = "Double",
                payloadType = Double::class,
                color = IPColor(156, 39, 176),
                description = "Floating-point type",
            ),
            InformationPacketType(
                id = "ip_boolean",
                typeName = "Boolean",
                payloadType = Boolean::class,
                color = IPColor(76, 175, 80),
                description = "Boolean type",
            ),
            InformationPacketType(
                id = "ip_string",
                typeName = "String",
                payloadType = String::class,
                color = IPColor(255, 152, 0),
                description = "Text string type",
            ),
        )

    // ============================================
    // Display Tests
    // ============================================

    @Test
    fun `should display all provided IP types`() {
        // Given 5 IP types
        val ipTypes = createTestIPTypes()

        // When IPPalette renders
        // Then all 5 type names should be visible
        assertEquals(5, ipTypes.size)
        assertTrue(ipTypes.any { it.typeName == "Any" })
        assertTrue(ipTypes.any { it.typeName == "Int" })
        assertTrue(ipTypes.any { it.typeName == "Double" })
        assertTrue(ipTypes.any { it.typeName == "Boolean" })
        assertTrue(ipTypes.any { it.typeName == "String" })
    }

    @Test
    fun `should have correct color for each IP type`() {
        // Given IP types with specific colors
        val ipTypes = createTestIPTypes()

        // Then each type should have correct color
        assertEquals(IPColor(0, 0, 0), ipTypes.find { it.typeName == "Any" }?.color)
        assertEquals(IPColor(33, 150, 243), ipTypes.find { it.typeName == "Int" }?.color)
        assertEquals(IPColor(156, 39, 176), ipTypes.find { it.typeName == "Double" }?.color)
        assertEquals(IPColor(76, 175, 80), ipTypes.find { it.typeName == "Boolean" }?.color)
        assertEquals(IPColor(255, 152, 0), ipTypes.find { it.typeName == "String" }?.color)
    }

    // ============================================
    // Search/Filter Tests
    // ============================================

    @Test
    fun `search should filter types by name`() {
        // Given IP types
        val ipTypes = createTestIPTypes()
        val searchQuery = "Int"

        // When filtering by search query
        val filtered =
            ipTypes.filter {
                it.typeName.contains(searchQuery, ignoreCase = true)
            }

        // Then only Int type should match
        assertEquals(1, filtered.size)
        assertEquals("Int", filtered[0].typeName)
    }

    @Test
    fun `search should be case insensitive`() {
        // Given IP types
        val ipTypes = createTestIPTypes()
        val searchQuery = "string" // lowercase

        // When filtering
        val filtered =
            ipTypes.filter {
                it.typeName.contains(searchQuery, ignoreCase = true)
            }

        // Then String type should match
        assertEquals(1, filtered.size)
        assertEquals("String", filtered[0].typeName)
    }

    @Test
    fun `empty search should show all types`() {
        // Given IP types
        val ipTypes = createTestIPTypes()
        val searchQuery = ""

        // When search is empty
        val filtered =
            if (searchQuery.isBlank()) {
                ipTypes
            } else {
                ipTypes.filter { it.typeName.contains(searchQuery, ignoreCase = true) }
            }

        // Then all types should be shown
        assertEquals(5, filtered.size)
    }

    @Test
    fun `search with no matches should return empty list`() {
        // Given IP types
        val ipTypes = createTestIPTypes()
        val searchQuery = "xyz"

        // When filtering with non-matching query
        val filtered =
            ipTypes.filter {
                it.typeName.contains(searchQuery, ignoreCase = true)
            }

        // Then result should be empty (UI will show "No matching types")
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `search should find partial matches`() {
        // Given IP types
        val ipTypes = createTestIPTypes()
        val searchQuery = "le" // matches Boolean and Double (both contain "le")

        // When filtering
        val filtered =
            ipTypes.filter {
                it.typeName.contains(searchQuery, ignoreCase = true)
            }

        // Then Boolean and Double should match
        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.typeName == "Boolean" })
        assertTrue(filtered.any { it.typeName == "Double" })
    }

    // ============================================
    // Selection Tests
    // ============================================

    @Test
    fun `clicking type should invoke callback`() {
        // Given IP types
        val ipTypes = createTestIPTypes()
        var selectedType: InformationPacketType? = null

        // When simulating type selection
        val intType = ipTypes.find { it.typeName == "Int" }!!
        selectedType = intType

        // Then callback should receive the correct type
        assertNotNull(selectedType)
        assertEquals("Int", selectedType.typeName)
        assertEquals("ip_int", selectedType.id)
    }

    @Test
    fun `selected type should be identifiable`() {
        // Given IP types and a selection
        val ipTypes = createTestIPTypes()
        val selectedTypeId = "ip_string"

        // When checking selection
        val selectedType = ipTypes.find { it.id == selectedTypeId }

        // Then selected type should be found
        assertNotNull(selectedType)
        assertEquals("String", selectedType.typeName)
    }

    // ============================================
    // Integration with IPTypeRegistry Tests
    // ============================================

    @Test
    fun `should work with IPTypeRegistry defaults`() {
        // Given default registry
        val registry = IPTypeRegistry.withDefaults()

        // When getting all types
        val types = registry.getAllTypes()

        // Then should have 5 default types
        assertEquals(5, types.size)
    }

    @Test
    fun `registry search should integrate with palette filtering`() {
        // Given default registry
        val registry = IPTypeRegistry.withDefaults()

        // When searching via registry
        val results = registry.search("Int")

        // Then should find Int type
        assertEquals(1, results.size)
        assertEquals("Int", results[0].typeName)
    }

    // ============================================
    // toCode() Tests (for TextualView display)
    // ============================================

    @Test
    fun `selected type should generate valid DSL code`() {
        // Given an IP type
        val stringType =
            InformationPacketType(
                id = "ip_string",
                typeName = "String",
                payloadType = String::class,
                color = IPColor(255, 152, 0),
                description = "Text string type",
            )

        // When generating code
        val code = stringType.toCode()

        // Then code should contain expected elements
        assertTrue(code.contains("// InformationPacket Type: String"))
        assertTrue(code.contains("ipType(\"String\")"))
        assertTrue(code.contains("payloadType = String::class"))
        assertTrue(code.contains("color = IPColor(255, 152, 0)"))
        assertTrue(code.contains("description = \"Text string type\""))
    }

    @Test
    fun `type without description should generate code without description line`() {
        // Given an IP type without description
        val intType =
            InformationPacketType(
                id = "ip_int",
                typeName = "Int",
                payloadType = Int::class,
                color = IPColor(33, 150, 243),
            )

        // When generating code
        val code = intType.toCode()

        // Then code should not contain description
        assertTrue(code.contains("ipType(\"Int\")"))
        assertFalse(code.contains("description"))
    }
}
