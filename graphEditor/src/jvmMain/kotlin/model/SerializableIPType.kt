/*
 * SerializableIPType - Serializable DTOs for custom IP type persistence
 * License: Apache 2.0
 */

package io.codenode.grapheditor.model

import io.codenode.fbpdsl.model.IPColor
import io.codenode.fbpdsl.model.InformationPacketType
import kotlinx.serialization.Serializable

/**
 * Serializable DTO for a single property within a custom IP type.
 *
 * @property name Property name
 * @property typeId ID of the IP type for this property (e.g., "ip_string")
 * @property isRequired Whether the property is required (non-nullable) or optional
 */
@Serializable
data class SerializableIPProperty(
    val name: String,
    val typeId: String,
    val isRequired: Boolean = true
) {
    /**
     * Converts this serializable DTO to a domain [IPProperty].
     */
    fun toIPProperty(): IPProperty = IPProperty(
        name = name,
        typeId = typeId,
        isRequired = isRequired
    )

    companion object {
        /**
         * Creates a serializable DTO from a domain [IPProperty].
         */
        fun fromIPProperty(property: IPProperty): SerializableIPProperty =
            SerializableIPProperty(
                name = property.name,
                typeId = property.typeId,
                isRequired = property.isRequired
            )
    }
}

/**
 * Serializable DTO for persisting custom IP types to JSON.
 *
 * Mirrors [CustomIPTypeDefinition] but uses [payloadTypeName] (String) instead of KClass<*>
 * for serialization compatibility. Custom composite types always use "Any" as the payload type name.
 *
 * @property id Unique identifier matching the registered type ID
 * @property typeName Display name (e.g., "UserProfile")
 * @property payloadTypeName KClass simple name; always "Any" for custom composite types
 * @property color Visual color for the type
 * @property description Optional type description
 * @property properties Property definitions for this type
 */
@Serializable
data class SerializableIPType(
    val id: String,
    val typeName: String,
    val payloadTypeName: String = "Any",
    val color: IPColor,
    val description: String? = null,
    val properties: List<SerializableIPProperty> = emptyList()
) {
    /**
     * Converts this serializable DTO to a domain [CustomIPTypeDefinition].
     */
    fun toDefinition(): CustomIPTypeDefinition = CustomIPTypeDefinition(
        id = id,
        typeName = typeName,
        properties = properties.map { it.toIPProperty() },
        color = color
    )

    /**
     * Converts this serializable DTO to an [InformationPacketType] for registry registration.
     * Custom composite types use Any::class as the payload type.
     */
    fun toInformationPacketType(): InformationPacketType = InformationPacketType(
        id = id,
        typeName = typeName,
        payloadType = Any::class,
        color = color,
        description = description
    )

    companion object {
        /**
         * Creates a serializable DTO from a domain [CustomIPTypeDefinition].
         *
         * @param definition The domain definition to convert
         * @param description Optional description for the type
         */
        fun fromDefinition(
            definition: CustomIPTypeDefinition,
            description: String? = null
        ): SerializableIPType = SerializableIPType(
            id = definition.id,
            typeName = definition.typeName,
            payloadTypeName = "Any",
            color = definition.color,
            description = description,
            properties = definition.properties.map { SerializableIPProperty.fromIPProperty(it) }
        )
    }
}
