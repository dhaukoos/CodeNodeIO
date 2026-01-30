/*
 * IPColor Test
 * Unit tests for IPColor RGB validation
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlin.test.*

/**
 * TDD tests for IPColor validation.
 * These tests verify that IPColor correctly validates:
 * - RGB component ranges (0-255)
 * - String conversion (toRgbString, fromRgbString)
 * - Companion object constants (BLACK, WHITE)
 */
class IPColorTest {

    // ============================================
    // Basic Construction Tests
    // ============================================

    @Test
    fun `should create IPColor with valid RGB values`() {
        // Given valid RGB values
        val color = IPColor(33, 150, 243)

        // Then values should be stored correctly
        assertEquals(33, color.red)
        assertEquals(150, color.green)
        assertEquals(243, color.blue)
    }

    @Test
    fun `should create IPColor with default values (black)`() {
        // Given default constructor
        val color = IPColor()

        // Then should be black
        assertEquals(0, color.red)
        assertEquals(0, color.green)
        assertEquals(0, color.blue)
    }

    @Test
    fun `should create IPColor with boundary values 0 and 255`() {
        // Given boundary values
        val colorMin = IPColor(0, 0, 0)
        val colorMax = IPColor(255, 255, 255)

        // Then both should be valid
        assertEquals(0, colorMin.red)
        assertEquals(255, colorMax.red)
    }

    // ============================================
    // Validation Tests
    // ============================================

    @Test
    fun `should reject red value below 0`() {
        assertFailsWith<IllegalArgumentException> {
            IPColor(-1, 100, 100)
        }
    }

    @Test
    fun `should reject red value above 255`() {
        assertFailsWith<IllegalArgumentException> {
            IPColor(256, 100, 100)
        }
    }

    @Test
    fun `should reject green value below 0`() {
        assertFailsWith<IllegalArgumentException> {
            IPColor(100, -1, 100)
        }
    }

    @Test
    fun `should reject green value above 255`() {
        assertFailsWith<IllegalArgumentException> {
            IPColor(100, 256, 100)
        }
    }

    @Test
    fun `should reject blue value below 0`() {
        assertFailsWith<IllegalArgumentException> {
            IPColor(100, 100, -1)
        }
    }

    @Test
    fun `should reject blue value above 255`() {
        assertFailsWith<IllegalArgumentException> {
            IPColor(100, 100, 256)
        }
    }

    // ============================================
    // String Conversion Tests
    // ============================================

    @Test
    fun `should convert to RGB string format`() {
        val color = IPColor(33, 150, 243)
        assertEquals("33, 150, 243", color.toRgbString())
    }

    @Test
    fun `should parse valid RGB string`() {
        val color = IPColor.fromRgbString("33, 150, 243")
        assertEquals(33, color.red)
        assertEquals(150, color.green)
        assertEquals(243, color.blue)
    }

    @Test
    fun `should parse RGB string with extra spaces`() {
        val color = IPColor.fromRgbString("  33 ,  150  , 243  ")
        assertEquals(33, color.red)
        assertEquals(150, color.green)
        assertEquals(243, color.blue)
    }

    @Test
    fun `should reject RGB string with wrong number of values`() {
        assertFailsWith<IllegalArgumentException> {
            IPColor.fromRgbString("100, 100")
        }
    }

    @Test
    fun `should reject RGB string with non-numeric values`() {
        assertFailsWith<IllegalArgumentException> {
            IPColor.fromRgbString("abc, 100, 100")
        }
    }

    @Test
    fun `should reject RGB string with out-of-range values`() {
        assertFailsWith<IllegalArgumentException> {
            IPColor.fromRgbString("300, 100, 100")
        }
    }

    // ============================================
    // Companion Object Constants Tests
    // ============================================

    @Test
    fun `BLACK constant should be (0, 0, 0)`() {
        assertEquals(IPColor(0, 0, 0), IPColor.BLACK)
    }

    @Test
    fun `WHITE constant should be (255, 255, 255)`() {
        assertEquals(IPColor(255, 255, 255), IPColor.WHITE)
    }

    // ============================================
    // Data Class Equality Tests
    // ============================================

    @Test
    fun `equal colors should be equal`() {
        val color1 = IPColor(100, 150, 200)
        val color2 = IPColor(100, 150, 200)
        assertEquals(color1, color2)
    }

    @Test
    fun `different colors should not be equal`() {
        val color1 = IPColor(100, 150, 200)
        val color2 = IPColor(100, 150, 201)
        assertNotEquals(color1, color2)
    }

    @Test
    fun `copy should create equal instance`() {
        val original = IPColor(100, 150, 200)
        val copy = original.copy()
        assertEquals(original, copy)
    }

    @Test
    fun `copy with modified value should differ`() {
        val original = IPColor(100, 150, 200)
        val modified = original.copy(red = 50)
        assertEquals(50, modified.red)
        assertEquals(150, modified.green)
        assertEquals(200, modified.blue)
    }
}
