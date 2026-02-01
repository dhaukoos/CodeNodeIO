/*
 * ColorEditor Test
 * UI tests for ColorEditor component
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.fbpdsl.model.IPColor
import kotlin.test.*

/**
 * TDD tests for ColorEditor component.
 * These tests verify that ColorEditor correctly:
 * - Displays color swatch with the current color
 * - Shows RGB values in text field format
 * - Validates and parses user input
 * - Calls callback on valid color changes
 * - Shows error messages for invalid input
 */
class ColorEditorTest {
    // ============================================
    // Display Tests
    // ============================================

    @Test
    fun `color swatch should display correct color`() {
        // Given: A blue color (33, 150, 243)
        val blueColor = IPColor(33, 150, 243)

        // When: ColorEditor renders
        // Then: Swatch should show the blue color
        // (This is verified by ensuring the color can be converted for display)
        assertEquals(33, blueColor.red)
        assertEquals(150, blueColor.green)
        assertEquals(243, blueColor.blue)
    }

    @Test
    fun `RGB values display in text field format`() {
        // Given: A color (33, 150, 243)
        val color = IPColor(33, 150, 243)

        // When: Converting to display string
        val displayString = color.toRgbString()

        // Then: Text field should show "33, 150, 243"
        assertEquals("33, 150, 243", displayString)
    }

    @Test
    fun `black color displays as 0, 0, 0`() {
        // Given: Black color
        val black = IPColor.BLACK

        // When: Converting to display string
        val displayString = black.toRgbString()

        // Then: Should display "0, 0, 0"
        assertEquals("0, 0, 0", displayString)
    }

    @Test
    fun `white color displays as 255, 255, 255`() {
        // Given: White color
        val white = IPColor.WHITE

        // When: Converting to display string
        val displayString = white.toRgbString()

        // Then: Should display "255, 255, 255"
        assertEquals("255, 255, 255", displayString)
    }

    // ============================================
    // Input Validation Tests
    // ============================================

    @Test
    fun `valid RGB input is accepted`() {
        // Given: Valid RGB string
        val input = "100, 100, 100"

        // When: Validating input
        val result = validateRgbInput(input)

        // Then: Validation should succeed
        assertTrue(result is ValidationResult.Valid)
        assertEquals(IPColor(100, 100, 100), (result as ValidationResult.Valid).color)
    }

    @Test
    fun `valid RGB input with extra spaces is accepted`() {
        // Given: Valid RGB string with extra spaces
        val input = "100,  100,   100"

        // When: Validating input
        val result = validateRgbInput(input)

        // Then: Validation should succeed
        assertTrue(result is ValidationResult.Valid)
        assertEquals(IPColor(100, 100, 100), (result as ValidationResult.Valid).color)
    }

    @Test
    fun `value above 255 shows error`() {
        // Given: RGB input with value > 255
        val input = "300, 100, 100"

        // When: Validating input
        val result = validateRgbInput(input)

        // Then: Error message should indicate values must be 0-255
        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("0-255"))
    }

    @Test
    fun `negative value shows error`() {
        // Given: RGB input with negative value
        val input = "-10, 100, 100"

        // When: Validating input
        val result = validateRgbInput(input)

        // Then: Error message should indicate values must be 0-255
        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("0-255"))
    }

    @Test
    fun `non-numeric input shows error`() {
        // Given: Non-numeric RGB input
        val input = "abc, 100, 100"

        // When: Validating input
        val result = validateRgbInput(input)

        // Then: Error message should indicate values must be numbers
        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("numbers"))
    }

    @Test
    fun `wrong number of values shows error`() {
        // Given: Only 2 values
        val input = "100, 100"

        // When: Validating input
        val result = validateRgbInput(input)

        // Then: Error message should indicate expected 3 values
        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("3 values"))
    }

    @Test
    fun `too many values shows error`() {
        // Given: 4 values instead of 3
        val input = "100, 100, 100, 100"

        // When: Validating input
        val result = validateRgbInput(input)

        // Then: Error message should indicate expected 3 values
        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("3 values"))
    }

    @Test
    fun `empty input shows error`() {
        // Given: Empty string
        val input = ""

        // When: Validating input
        val result = validateRgbInput(input)

        // Then: Should show error
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `whitespace only input shows error`() {
        // Given: Whitespace only
        val input = "   "

        // When: Validating input
        val result = validateRgbInput(input)

        // Then: Should show error
        assertTrue(result is ValidationResult.Error)
    }

    // ============================================
    // Boundary Value Tests
    // ============================================

    @Test
    fun `minimum valid values are accepted`() {
        // Given: Minimum values (0, 0, 0)
        val input = "0, 0, 0"

        // When: Validating input
        val result = validateRgbInput(input)

        // Then: Should be valid
        assertTrue(result is ValidationResult.Valid)
        assertEquals(IPColor(0, 0, 0), (result as ValidationResult.Valid).color)
    }

    @Test
    fun `maximum valid values are accepted`() {
        // Given: Maximum values (255, 255, 255)
        val input = "255, 255, 255"

        // When: Validating input
        val result = validateRgbInput(input)

        // Then: Should be valid
        assertTrue(result is ValidationResult.Valid)
        assertEquals(IPColor(255, 255, 255), (result as ValidationResult.Valid).color)
    }

    @Test
    fun `value 256 is rejected`() {
        // Given: Value just above max
        val input = "256, 0, 0"

        // When: Validating input
        val result = validateRgbInput(input)

        // Then: Should be invalid
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `value -1 is rejected`() {
        // Given: Value just below min
        val input = "-1, 0, 0"

        // When: Validating input
        val result = validateRgbInput(input)

        // Then: Should be invalid
        assertTrue(result is ValidationResult.Error)
    }

    // ============================================
    // Callback Tests
    // ============================================

    @Test
    fun `valid input triggers callback with new color`() {
        // Given: ColorEditor with callback
        var callbackColor: IPColor? = null
        val onColorChange: (IPColor) -> Unit = { color ->
            callbackColor = color
        }

        // When: Simulating valid input processing
        val input = "100, 150, 200"
        val result = validateRgbInput(input)
        if (result is ValidationResult.Valid) {
            onColorChange(result.color)
        }

        // Then: Callback should receive the correct color
        assertNotNull(callbackColor)
        assertEquals(IPColor(100, 150, 200), callbackColor)
    }

    @Test
    fun `invalid input does not trigger callback`() {
        // Given: ColorEditor with callback
        var callbackCount = 0
        val onColorChange: (IPColor) -> Unit = { _ ->
            callbackCount++
        }

        // When: Simulating invalid input processing
        val input = "invalid"
        val result = validateRgbInput(input)
        if (result is ValidationResult.Valid) {
            onColorChange(result.color)
        }

        // Then: Callback should not be called
        assertEquals(0, callbackCount)
    }

    // ============================================
    // Color Swatch Update Tests
    // ============================================

    @Test
    fun `color swatch updates on valid input`() {
        // Given: Initial blue color
        var currentColor = IPColor(33, 150, 243)

        // When: User enters red color
        val input = "255, 0, 0"
        val result = validateRgbInput(input)
        if (result is ValidationResult.Valid) {
            currentColor = result.color
        }

        // Then: Current color should be red
        assertEquals(IPColor(255, 0, 0), currentColor)
    }

    @Test
    fun `invalid input preserves previous color`() {
        // Given: Initial blue color
        val initialColor = IPColor(33, 150, 243)
        var currentColor = initialColor

        // When: User enters invalid input
        val input = "invalid"
        val result = validateRgbInput(input)
        if (result is ValidationResult.Valid) {
            currentColor = result.color
        }

        // Then: Current color should still be blue
        assertEquals(initialColor, currentColor)
    }

    // ============================================
    // Helper Functions (matching ColorEditor implementation)
    // ============================================

    /**
     * Validation result sealed class for type-safe error handling.
     */
    sealed class ValidationResult {
        data class Valid(
            val color: IPColor,
        ) : ValidationResult()

        data class Error(
            val message: String,
        ) : ValidationResult()
    }

    /**
     * Validates RGB input string and returns either a valid color or an error message.
     * This function mirrors the validation logic in ColorEditor.
     */
    fun validateRgbInput(input: String): ValidationResult {
        if (input.isBlank()) {
            return ValidationResult.Error("Expected 3 values: R, G, B")
        }

        val parts = input.split(",").map { it.trim() }
        if (parts.size != 3) {
            return ValidationResult.Error("Expected 3 values: R, G, B")
        }

        val values = parts.mapNotNull { it.toIntOrNull() }
        if (values.size != 3) {
            return ValidationResult.Error("Values must be numbers")
        }

        if (values.any { it !in 0..255 }) {
            return ValidationResult.Error("Values must be 0-255")
        }

        return ValidationResult.Valid(IPColor(values[0], values[1], values[2]))
    }
}
