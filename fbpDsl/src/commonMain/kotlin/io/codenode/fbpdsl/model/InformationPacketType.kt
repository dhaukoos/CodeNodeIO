/*
 * InformationPacketType - Type Definition for Information Packets
 * Defines a type of InformationPacket with associated metadata and visual properties
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlin.reflect.KClass

/**
 * Defines a type of InformationPacket with associated metadata and visual properties.
 *
 * InformationPacketType represents a category of data that can flow through connections
 * in a flow graph. Each type has a unique ID, a display name, a Kotlin class representing
 * the payload type, and a color for visual identification.
 *
 * @property id Unique identifier for this IP type (e.g., "ip_string", "ip_int")
 * @property typeName Human-readable name displayed in the UI (e.g., "String", "Int")
 * @property payloadType The Kotlin class representing the payload data type
 * @property color RGB color for visual identification in the graph editor
 * @property description Optional description of this IP type's purpose
 *
 * @sample
 * ```kotlin
 * // Create a custom IP type
 * val userDataType = InformationPacketType(
 *     id = "ip_user_data",
 *     typeName = "UserData",
 *     payloadType = UserData::class,
 *     color = IPColor(100, 200, 150),
 *     description = "User profile information"
 * )
 *
 * // Generate DSL code representation
 * println(userDataType.toCode())
 * ```
 */
data class InformationPacketType(
    val id: String,
    val typeName: String,
    val payloadType: KClass<*>,
    val color: IPColor = IPColor.BLACK,
    val description: String? = null
) {
    init {
        require(id.isNotBlank()) { "ID cannot be blank" }
        require(typeName.isNotBlank()) { "Type name cannot be blank" }
    }

    /**
     * Generates DSL code representation of this IP type.
     *
     * The output format matches the FBP DSL syntax for defining IP types.
     *
     * @return Kotlin DSL code string representing this IP type
     *
     * Example output:
     * ```kotlin
     * // InformationPacket Type: String
     * ipType("String") {
     *     payloadType = String::class
     *     color = IPColor(255, 152, 0)
     *     description = "Text string type"
     * }
     * ```
     */
    fun toCode(): String = buildString {
        appendLine("// InformationPacket Type: $typeName")
        appendLine("ipType(\"$typeName\") {")
        appendLine("    payloadType = ${payloadType.simpleName}::class")
        appendLine("    color = IPColor(${color.red}, ${color.green}, ${color.blue})")
        description?.let { appendLine("    description = \"$it\"") }
        appendLine("}")
    }

    /**
     * Creates a copy of this type with a modified color.
     *
     * @param newColor The new color for the IP type
     * @return A new InformationPacketType with the updated color
     */
    fun withColor(newColor: IPColor): InformationPacketType = copy(color = newColor)

    override fun toString(): String {
        return "InformationPacketType(id='$id', typeName='$typeName', payloadType=${payloadType.simpleName}, color=${color.toRgbString()})"
    }
}
