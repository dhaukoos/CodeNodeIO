/*
 * IPTypeRegistry - Registry for InformationPacket Types
 * Manages available IP types for the graphEditor
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import io.codenode.fbpdsl.model.IPColor
import io.codenode.fbpdsl.model.InformationPacketType

/**
 * Runtime registry of available InformationPacket types for the graphEditor.
 *
 * IPTypeRegistry manages the collection of IP types that can be assigned to
 * connections in the flow graph. It provides methods for registering, looking up,
 * and searching IP types.
 *
 * The registry can be initialized with default Kotlin types using [withDefaults].
 *
 * @sample
 * ```kotlin
 * // Get registry with default types
 * val registry = IPTypeRegistry.withDefaults()
 *
 * // Get all available types
 * val allTypes = registry.getAllTypes()
 *
 * // Search for types
 * val intTypes = registry.search("int")
 *
 * // Get specific type by ID
 * val stringType = registry.getById("ip_string")
 * ```
 */
class IPTypeRegistry {
    private val types = mutableMapOf<String, InformationPacketType>()

    /**
     * Registers an InformationPacket type in the registry.
     *
     * If a type with the same ID already exists, it will be replaced.
     *
     * @param type The InformationPacketType to register
     */
    fun register(type: InformationPacketType) {
        types[type.id] = type
    }

    /**
     * Unregisters an InformationPacket type from the registry.
     *
     * @param id The ID of the type to remove
     * @return The removed type, or null if not found
     */
    fun unregister(id: String): InformationPacketType? {
        return types.remove(id)
    }

    /**
     * Gets an InformationPacket type by its unique ID.
     *
     * @param id The type ID (e.g., "ip_string")
     * @return The InformationPacketType, or null if not found
     */
    fun getById(id: String): InformationPacketType? = types[id]

    /**
     * Gets an InformationPacket type by its display name.
     *
     * @param typeName The type name (e.g., "String")
     * @return The InformationPacketType, or null if not found
     */
    fun getByTypeName(typeName: String): InformationPacketType? =
        types.values.find { it.typeName == typeName }

    /**
     * Gets all registered InformationPacket types.
     *
     * @return List of all registered types
     */
    fun getAllTypes(): List<InformationPacketType> = types.values.toList()

    /**
     * Searches for InformationPacket types matching the query.
     *
     * Performs case-insensitive substring matching on the typeName.
     *
     * @param query The search query string
     * @return List of matching types
     */
    fun search(query: String): List<InformationPacketType> =
        types.values.filter { it.typeName.contains(query, ignoreCase = true) }

    /**
     * Checks if a type with the given ID is registered.
     *
     * @param id The type ID to check
     * @return true if the type is registered
     */
    fun contains(id: String): Boolean = types.containsKey(id)

    /**
     * Gets the number of registered types.
     *
     * @return The count of registered types
     */
    fun size(): Int = types.size

    /**
     * Clears all registered types from the registry.
     */
    fun clear() {
        types.clear()
    }

    /**
     * Updates the color of a registered IP type.
     *
     * @param id The type ID to update
     * @param newColor The new color to assign
     * @return true if the type was found and updated, false otherwise
     */
    fun updateColor(id: String, newColor: IPColor): Boolean {
        val type = types[id] ?: return false
        types[id] = type.withColor(newColor)
        return true
    }

    companion object {
        /**
         * Creates a new IPTypeRegistry populated with default Kotlin types.
         *
         * Default types include:
         * - Any (black) - Universal type that accepts any payload
         * - Int (blue) - Integer numeric type
         * - Double (purple) - Floating-point numeric type
         * - Boolean (green) - True/false boolean type
         * - String (orange) - Text string type
         *
         * @return IPTypeRegistry with 5 default types registered
         */
        fun withDefaults(): IPTypeRegistry = IPTypeRegistry().apply {
            register(
                InformationPacketType(
                    id = "ip_any",
                    typeName = "Any",
                    payloadType = Any::class,
                    color = IPColor(0, 0, 0),
                    description = "Universal type that accepts any payload"
                )
            )
            register(
                InformationPacketType(
                    id = "ip_int",
                    typeName = "Int",
                    payloadType = Int::class,
                    color = IPColor(33, 150, 243),
                    description = "Integer numeric type"
                )
            )
            register(
                InformationPacketType(
                    id = "ip_double",
                    typeName = "Double",
                    payloadType = Double::class,
                    color = IPColor(156, 39, 176),
                    description = "Floating-point numeric type"
                )
            )
            register(
                InformationPacketType(
                    id = "ip_boolean",
                    typeName = "Boolean",
                    payloadType = Boolean::class,
                    color = IPColor(76, 175, 80),
                    description = "True/false boolean type"
                )
            )
            register(
                InformationPacketType(
                    id = "ip_string",
                    typeName = "String",
                    payloadType = String::class,
                    color = IPColor(255, 152, 0),
                    description = "Text string type"
                )
            )
        }

        /**
         * Creates an empty IPTypeRegistry.
         *
         * @return Empty IPTypeRegistry
         */
        fun empty(): IPTypeRegistry = IPTypeRegistry()
    }
}
