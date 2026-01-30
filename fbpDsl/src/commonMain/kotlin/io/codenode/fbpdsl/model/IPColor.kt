/*
 * IPColor - RGB Color for InformationPacket Types
 * Represents an RGB color for visual identification of IP types
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.serialization.Serializable

/**
 * Represents an RGB color for visual identification of InformationPacket types.
 *
 * Each color component (red, green, blue) must be in the range 0-255.
 * The default color is black (0, 0, 0).
 *
 * @property red Red component (0-255)
 * @property green Green component (0-255)
 * @property blue Blue component (0-255)
 *
 * @sample
 * ```kotlin
 * // Create a blue color
 * val blue = IPColor(33, 150, 243)
 *
 * // Use predefined constants
 * val black = IPColor.BLACK
 * val white = IPColor.WHITE
 *
 * // Convert to RGB string for display
 * println(blue.toRgbString()) // "33, 150, 243"
 * ```
 */
@Serializable
data class IPColor(
    val red: Int = 0,
    val green: Int = 0,
    val blue: Int = 0
) {
    init {
        require(red in 0..255) { "Red must be 0-255, got $red" }
        require(green in 0..255) { "Green must be 0-255, got $green" }
        require(blue in 0..255) { "Blue must be 0-255, got $blue" }
    }

    /**
     * Converts this color to an RGB string representation.
     * Format: "R, G, B" (e.g., "33, 150, 243")
     *
     * @return RGB string in format "R, G, B"
     */
    fun toRgbString(): String = "$red, $green, $blue"

    companion object {
        /** Black color (0, 0, 0) - default for "Any" type */
        val BLACK = IPColor(0, 0, 0)

        /** White color (255, 255, 255) */
        val WHITE = IPColor(255, 255, 255)

        /**
         * Parses an RGB string into an IPColor.
         * Accepts format: "R, G, B" with optional spaces.
         *
         * @param rgb String in format "R, G, B"
         * @return IPColor if parsing succeeds
         * @throws IllegalArgumentException if format is invalid or values out of range
         */
        fun fromRgbString(rgb: String): IPColor {
            val parts = rgb.split(",").map { it.trim() }
            require(parts.size == 3) { "Expected 3 values: R, G, B" }

            val values = parts.map { part ->
                part.toIntOrNull() ?: throw IllegalArgumentException("Values must be numbers")
            }

            return IPColor(values[0], values[1], values[2])
        }
    }
}
