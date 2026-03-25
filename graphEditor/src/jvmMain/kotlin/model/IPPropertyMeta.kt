/*
 * IPPropertyMeta - Metadata for a single property parsed from an IP type file
 * License: Apache 2.0
 */

package io.codenode.grapheditor.model

/**
 * A single property parsed from a data class in an IP type definition file.
 *
 * @property name Property name (e.g., "latitude")
 * @property kotlinType Kotlin type as string (e.g., "Double", "String", "List<String>")
 * @property typeId Resolved IP type ID if it maps to a registered type (e.g., "ip_double")
 * @property isRequired True if not nullable (no `?` suffix in the source)
 */
data class IPPropertyMeta(
    val name: String,
    val kotlinType: String,
    val typeId: String,
    val isRequired: Boolean = true
)
