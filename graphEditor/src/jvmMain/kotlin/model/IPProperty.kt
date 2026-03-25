/*
 * IPProperty - Domain model for custom IP type properties
 * License: Apache 2.0
 */

package io.codenode.grapheditor.model

import io.codenode.fbpdsl.model.IPColor

/**
 * Represents a single named, typed property within a custom IP type definition.
 *
 * @property name Property name (e.g., "age", "email")
 * @property typeId ID of the IP type for this property (e.g., "ip_string")
 * @property isRequired Whether the property is required (non-nullable) or optional (nullable with null default)
 */
data class IPProperty(
    val name: String,
    val typeId: String,
    val isRequired: Boolean = true
)

/**
 * Represents a user-defined composite IP type with its properties.
 *
 * This is the in-memory definition used during creation and persisted via FileIPTypeRepository.
 *
 * @property id Unique identifier (e.g., "ip_userprofile")
 * @property typeName Display name (e.g., "UserProfile")
 * @property properties Ordered list of typed properties (may be empty for marker types)
 * @property color Visual color for the type in the UI
 */
data class CustomIPTypeDefinition(
    val id: String,
    val typeName: String,
    val properties: List<IPProperty> = emptyList(),
    val color: IPColor = IPColor.BLACK,
    val filePath: String? = null,
    val tier: PlacementLevel? = null
) {
    companion object {
        /**
         * Palette of 8 visually distinct colors for auto-assignment to custom IP types.
         * These colors avoid the 5 built-in type colors (black, blue, purple, green, orange).
         */
        val COLOR_PALETTE = listOf(
            IPColor(233, 30, 99),    // Pink
            IPColor(0, 188, 212),    // Cyan
            IPColor(121, 85, 72),    // Brown
            IPColor(255, 87, 34),    // Deep Orange
            IPColor(63, 81, 181),    // Indigo
            IPColor(0, 150, 136),    // Teal
            IPColor(205, 220, 57),   // Lime
            IPColor(158, 158, 158)   // Grey
        )

        /**
         * Returns the next color from the palette based on the current count of custom types.
         *
         * @param customTypeCount The number of existing custom types
         * @return The next color from the cycling palette
         */
        fun nextColor(customTypeCount: Int): IPColor =
            COLOR_PALETTE[customTypeCount % COLOR_PALETTE.size]
    }
}
