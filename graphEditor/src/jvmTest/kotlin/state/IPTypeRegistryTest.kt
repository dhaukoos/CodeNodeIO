/*
 * IPTypeRegistry Test
 * Unit tests for IPTypeRegistry functionality
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import io.codenode.fbpdsl.model.IPColor
import io.codenode.fbpdsl.model.InformationPacketType
import kotlin.test.*

/**
 * TDD tests for IPTypeRegistry.
 * These tests verify that IPTypeRegistry correctly:
 * - Registers and unregisters IP types
 * - Looks up types by ID and typeName
 * - Searches for types by query
 * - Provides default Kotlin types
 * - Updates IP type colors
 */
class IPTypeRegistryTest {

    // ============================================
    // Default Registry Tests
    // ============================================

    @Test
    fun `withDefaults should create registry with 5 default types`() {
        val registry = IPTypeRegistry.withDefaults()
        assertEquals(5, registry.size())
    }

    @Test
    fun `withDefaults should include Any type with black color`() {
        val registry = IPTypeRegistry.withDefaults()
        val anyType = registry.getById("ip_any")

        assertNotNull(anyType)
        assertEquals("Any", anyType.typeName)
        assertEquals(Any::class, anyType.payloadType)
        assertEquals(IPColor(0, 0, 0), anyType.color)
    }

    @Test
    fun `withDefaults should include Int type with blue color`() {
        val registry = IPTypeRegistry.withDefaults()
        val intType = registry.getById("ip_int")

        assertNotNull(intType)
        assertEquals("Int", intType.typeName)
        assertEquals(Int::class, intType.payloadType)
        assertEquals(IPColor(33, 150, 243), intType.color)
    }

    @Test
    fun `withDefaults should include Double type with purple color`() {
        val registry = IPTypeRegistry.withDefaults()
        val doubleType = registry.getById("ip_double")

        assertNotNull(doubleType)
        assertEquals("Double", doubleType.typeName)
        assertEquals(Double::class, doubleType.payloadType)
        assertEquals(IPColor(156, 39, 176), doubleType.color)
    }

    @Test
    fun `withDefaults should include Boolean type with green color`() {
        val registry = IPTypeRegistry.withDefaults()
        val boolType = registry.getById("ip_boolean")

        assertNotNull(boolType)
        assertEquals("Boolean", boolType.typeName)
        assertEquals(Boolean::class, boolType.payloadType)
        assertEquals(IPColor(76, 175, 80), boolType.color)
    }

    @Test
    fun `withDefaults should include String type with orange color`() {
        val registry = IPTypeRegistry.withDefaults()
        val stringType = registry.getById("ip_string")

        assertNotNull(stringType)
        assertEquals("String", stringType.typeName)
        assertEquals(String::class, stringType.payloadType)
        assertEquals(IPColor(255, 152, 0), stringType.color)
    }

    // ============================================
    // Empty Registry Tests
    // ============================================

    @Test
    fun `empty should create registry with no types`() {
        val registry = IPTypeRegistry.empty()
        assertEquals(0, registry.size())
        assertTrue(registry.getAllTypes().isEmpty())
    }

    // ============================================
    // Register/Unregister Tests
    // ============================================

    @Test
    fun `register should add type to registry`() {
        val registry = IPTypeRegistry.empty()
        val customType = InformationPacketType(
            id = "ip_custom",
            typeName = "Custom",
            payloadType = String::class,
            color = IPColor(100, 100, 100)
        )

        registry.register(customType)

        assertEquals(1, registry.size())
        assertEquals(customType, registry.getById("ip_custom"))
    }

    @Test
    fun `register should replace existing type with same ID`() {
        val registry = IPTypeRegistry.empty()
        val original = InformationPacketType(
            id = "ip_custom",
            typeName = "Original",
            payloadType = String::class
        )
        val replacement = InformationPacketType(
            id = "ip_custom",
            typeName = "Replacement",
            payloadType = Int::class
        )

        registry.register(original)
        registry.register(replacement)

        assertEquals(1, registry.size())
        assertEquals("Replacement", registry.getById("ip_custom")?.typeName)
    }

    @Test
    fun `unregister should remove type from registry`() {
        val registry = IPTypeRegistry.withDefaults()
        val initial = registry.size()

        val removed = registry.unregister("ip_string")

        assertNotNull(removed)
        assertEquals("String", removed.typeName)
        assertEquals(initial - 1, registry.size())
        assertNull(registry.getById("ip_string"))
    }

    @Test
    fun `unregister should return null for non-existent type`() {
        val registry = IPTypeRegistry.empty()
        val result = registry.unregister("non_existent")
        assertNull(result)
    }

    // ============================================
    // Lookup Tests
    // ============================================

    @Test
    fun `getById should return null for non-existent ID`() {
        val registry = IPTypeRegistry.withDefaults()
        assertNull(registry.getById("non_existent"))
    }

    @Test
    fun `getByTypeName should find type by display name`() {
        val registry = IPTypeRegistry.withDefaults()
        val stringType = registry.getByTypeName("String")

        assertNotNull(stringType)
        assertEquals("ip_string", stringType.id)
    }

    @Test
    fun `getByTypeName should return null for non-existent name`() {
        val registry = IPTypeRegistry.withDefaults()
        assertNull(registry.getByTypeName("NonExistent"))
    }

    @Test
    fun `getAllTypes should return all registered types`() {
        val registry = IPTypeRegistry.withDefaults()
        val types = registry.getAllTypes()

        assertEquals(5, types.size)
        assertTrue(types.any { it.id == "ip_any" })
        assertTrue(types.any { it.id == "ip_int" })
        assertTrue(types.any { it.id == "ip_double" })
        assertTrue(types.any { it.id == "ip_boolean" })
        assertTrue(types.any { it.id == "ip_string" })
    }

    // ============================================
    // Search Tests
    // ============================================

    @Test
    fun `search should find types by partial name match`() {
        val registry = IPTypeRegistry.withDefaults()
        val results = registry.search("Int")

        assertEquals(1, results.size)
        assertEquals("ip_int", results[0].id)
    }

    @Test
    fun `search should be case insensitive`() {
        val registry = IPTypeRegistry.withDefaults()
        val results = registry.search("string")

        assertEquals(1, results.size)
        assertEquals("ip_string", results[0].id)
    }

    @Test
    fun `search should return multiple matches`() {
        val registry = IPTypeRegistry.withDefaults()
        // "oolean" matches Boolean, "ouble" matches Double - test with 'o'
        val results = registry.search("o")

        // Should match Boolean and Double (both contain 'o')
        assertTrue(results.size >= 2)
        assertTrue(results.any { it.typeName == "Boolean" })
        assertTrue(results.any { it.typeName == "Double" })
    }

    @Test
    fun `search should return empty list for no matches`() {
        val registry = IPTypeRegistry.withDefaults()
        val results = registry.search("xyz")

        assertTrue(results.isEmpty())
    }

    @Test
    fun `search with empty query should return all types`() {
        val registry = IPTypeRegistry.withDefaults()
        val results = registry.search("")

        assertEquals(5, results.size)
    }

    // ============================================
    // Contains and Size Tests
    // ============================================

    @Test
    fun `contains should return true for existing type`() {
        val registry = IPTypeRegistry.withDefaults()
        assertTrue(registry.contains("ip_string"))
    }

    @Test
    fun `contains should return false for non-existent type`() {
        val registry = IPTypeRegistry.withDefaults()
        assertFalse(registry.contains("non_existent"))
    }

    @Test
    fun `size should return correct count`() {
        val registry = IPTypeRegistry.empty()
        assertEquals(0, registry.size())

        registry.register(
            InformationPacketType(
                id = "ip_1",
                typeName = "Type1",
                payloadType = String::class
            )
        )
        assertEquals(1, registry.size())

        registry.register(
            InformationPacketType(
                id = "ip_2",
                typeName = "Type2",
                payloadType = Int::class
            )
        )
        assertEquals(2, registry.size())
    }

    // ============================================
    // Clear Tests
    // ============================================

    @Test
    fun `clear should remove all types`() {
        val registry = IPTypeRegistry.withDefaults()
        assertEquals(5, registry.size())

        registry.clear()

        assertEquals(0, registry.size())
        assertTrue(registry.getAllTypes().isEmpty())
    }

    // ============================================
    // Update Color Tests
    // ============================================

    @Test
    fun `updateColor should change type color`() {
        val registry = IPTypeRegistry.withDefaults()
        val newColor = IPColor(200, 50, 50)

        val result = registry.updateColor("ip_string", newColor)

        assertTrue(result)
        assertEquals(newColor, registry.getById("ip_string")?.color)
    }

    @Test
    fun `updateColor should return false for non-existent type`() {
        val registry = IPTypeRegistry.withDefaults()
        val newColor = IPColor(200, 50, 50)

        val result = registry.updateColor("non_existent", newColor)

        assertFalse(result)
    }

    @Test
    fun `updateColor should preserve other type properties`() {
        val registry = IPTypeRegistry.withDefaults()
        val originalType = registry.getById("ip_string")!!
        val newColor = IPColor(200, 50, 50)

        registry.updateColor("ip_string", newColor)
        val updatedType = registry.getById("ip_string")!!

        assertEquals(originalType.id, updatedType.id)
        assertEquals(originalType.typeName, updatedType.typeName)
        assertEquals(originalType.payloadType, updatedType.payloadType)
        assertEquals(originalType.description, updatedType.description)
        assertEquals(newColor, updatedType.color)
    }
}
