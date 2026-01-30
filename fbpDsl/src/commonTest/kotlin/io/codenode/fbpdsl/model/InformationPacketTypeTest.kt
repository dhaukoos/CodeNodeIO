/*
 * InformationPacketType Test
 * Unit tests for InformationPacketType validation and functionality
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlin.test.*

/**
 * TDD tests for InformationPacketType.
 * These tests verify that InformationPacketType correctly:
 * - Validates required fields (id, typeName)
 * - Stores payload type and color
 * - Generates DSL code representation
 * - Supports copy operations
 */
class InformationPacketTypeTest {

    // ============================================
    // Basic Construction Tests
    // ============================================

    @Test
    fun `should create InformationPacketType with required fields`() {
        // Given required fields
        val ipType = InformationPacketType(
            id = "ip_string",
            typeName = "String",
            payloadType = String::class
        )

        // Then values should be stored correctly
        assertEquals("ip_string", ipType.id)
        assertEquals("String", ipType.typeName)
        assertEquals(String::class, ipType.payloadType)
        assertEquals(IPColor.BLACK, ipType.color) // default
        assertNull(ipType.description)
    }

    @Test
    fun `should create InformationPacketType with all fields`() {
        // Given all fields
        val color = IPColor(255, 152, 0)
        val ipType = InformationPacketType(
            id = "ip_string",
            typeName = "String",
            payloadType = String::class,
            color = color,
            description = "Text string type"
        )

        // Then values should be stored correctly
        assertEquals("ip_string", ipType.id)
        assertEquals("String", ipType.typeName)
        assertEquals(String::class, ipType.payloadType)
        assertEquals(color, ipType.color)
        assertEquals("Text string type", ipType.description)
    }

    // ============================================
    // Validation Tests
    // ============================================

    @Test
    fun `should reject blank id`() {
        assertFailsWith<IllegalArgumentException> {
            InformationPacketType(
                id = "",
                typeName = "String",
                payloadType = String::class
            )
        }
    }

    @Test
    fun `should reject whitespace-only id`() {
        assertFailsWith<IllegalArgumentException> {
            InformationPacketType(
                id = "   ",
                typeName = "String",
                payloadType = String::class
            )
        }
    }

    @Test
    fun `should reject blank typeName`() {
        assertFailsWith<IllegalArgumentException> {
            InformationPacketType(
                id = "ip_string",
                typeName = "",
                payloadType = String::class
            )
        }
    }

    @Test
    fun `should reject whitespace-only typeName`() {
        assertFailsWith<IllegalArgumentException> {
            InformationPacketType(
                id = "ip_string",
                typeName = "   ",
                payloadType = String::class
            )
        }
    }

    // ============================================
    // toCode() Tests
    // ============================================

    @Test
    fun `toCode should generate valid DSL without description`() {
        val ipType = InformationPacketType(
            id = "ip_int",
            typeName = "Int",
            payloadType = Int::class,
            color = IPColor(33, 150, 243)
        )

        val code = ipType.toCode()

        assertTrue(code.contains("// InformationPacket Type: Int"))
        assertTrue(code.contains("ipType(\"Int\")"))
        assertTrue(code.contains("payloadType = Int::class"))
        assertTrue(code.contains("color = IPColor(33, 150, 243)"))
        assertFalse(code.contains("description"))
    }

    @Test
    fun `toCode should include description when present`() {
        val ipType = InformationPacketType(
            id = "ip_string",
            typeName = "String",
            payloadType = String::class,
            color = IPColor(255, 152, 0),
            description = "Text string type"
        )

        val code = ipType.toCode()

        assertTrue(code.contains("description = \"Text string type\""))
    }

    @Test
    fun `toCode should use BLACK color for default Any type`() {
        val ipType = InformationPacketType(
            id = "ip_any",
            typeName = "Any",
            payloadType = Any::class
        )

        val code = ipType.toCode()

        assertTrue(code.contains("color = IPColor(0, 0, 0)"))
    }

    // ============================================
    // withColor() Tests
    // ============================================

    @Test
    fun `withColor should return new instance with updated color`() {
        val original = InformationPacketType(
            id = "ip_string",
            typeName = "String",
            payloadType = String::class,
            color = IPColor.BLACK
        )

        val newColor = IPColor(200, 50, 50)
        val updated = original.withColor(newColor)

        // Original should be unchanged
        assertEquals(IPColor.BLACK, original.color)

        // Updated should have new color
        assertEquals(newColor, updated.color)

        // Other fields should be preserved
        assertEquals(original.id, updated.id)
        assertEquals(original.typeName, updated.typeName)
        assertEquals(original.payloadType, updated.payloadType)
    }

    // ============================================
    // Data Class Equality Tests
    // ============================================

    @Test
    fun `equal types should be equal`() {
        val type1 = InformationPacketType(
            id = "ip_string",
            typeName = "String",
            payloadType = String::class,
            color = IPColor(255, 152, 0)
        )
        val type2 = InformationPacketType(
            id = "ip_string",
            typeName = "String",
            payloadType = String::class,
            color = IPColor(255, 152, 0)
        )
        assertEquals(type1, type2)
    }

    @Test
    fun `types with different ids should not be equal`() {
        val type1 = InformationPacketType(
            id = "ip_string",
            typeName = "String",
            payloadType = String::class
        )
        val type2 = InformationPacketType(
            id = "ip_str",
            typeName = "String",
            payloadType = String::class
        )
        assertNotEquals(type1, type2)
    }

    @Test
    fun `types with different colors should not be equal`() {
        val type1 = InformationPacketType(
            id = "ip_string",
            typeName = "String",
            payloadType = String::class,
            color = IPColor.BLACK
        )
        val type2 = InformationPacketType(
            id = "ip_string",
            typeName = "String",
            payloadType = String::class,
            color = IPColor.WHITE
        )
        assertNotEquals(type1, type2)
    }

    // ============================================
    // toString() Tests
    // ============================================

    @Test
    fun `toString should include key fields`() {
        val ipType = InformationPacketType(
            id = "ip_string",
            typeName = "String",
            payloadType = String::class,
            color = IPColor(255, 152, 0)
        )

        val str = ipType.toString()

        assertTrue(str.contains("ip_string"))
        assertTrue(str.contains("String"))
        assertTrue(str.contains("255, 152, 0"))
    }

    // ============================================
    // Various Payload Type Tests
    // ============================================

    @Test
    fun `should accept Int payload type`() {
        val ipType = InformationPacketType(
            id = "ip_int",
            typeName = "Int",
            payloadType = Int::class,
            color = IPColor(33, 150, 243)
        )
        assertEquals(Int::class, ipType.payloadType)
    }

    @Test
    fun `should accept Double payload type`() {
        val ipType = InformationPacketType(
            id = "ip_double",
            typeName = "Double",
            payloadType = Double::class,
            color = IPColor(156, 39, 176)
        )
        assertEquals(Double::class, ipType.payloadType)
    }

    @Test
    fun `should accept Boolean payload type`() {
        val ipType = InformationPacketType(
            id = "ip_boolean",
            typeName = "Boolean",
            payloadType = Boolean::class,
            color = IPColor(76, 175, 80)
        )
        assertEquals(Boolean::class, ipType.payloadType)
    }

    @Test
    fun `should accept Any payload type`() {
        val ipType = InformationPacketType(
            id = "ip_any",
            typeName = "Any",
            payloadType = Any::class,
            color = IPColor.BLACK
        )
        assertEquals(Any::class, ipType.payloadType)
    }
}
