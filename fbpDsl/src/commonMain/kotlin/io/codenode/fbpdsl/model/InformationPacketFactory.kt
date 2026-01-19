/*
 * InformationPacketFactory - Factory for Creating InformationPacket Instances
 * Provides convenience methods for packet creation with automatic ID generation
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlin.reflect.KClass

/**
 * Factory for creating InformationPacket instances with automatic ID generation
 */
object InformationPacketFactory {
    /**
     * Creates a type-safe InformationPacket with automatic ID generation
     *
     * @param T The type of data (must be a Kotlin data class)
     * @param payload The strongly-typed data content
     * @param metadata Optional metadata map
     * @return New InformationPacket instance with generated ID
     *
     * @sample
     * ```kotlin
     * data class Temperature(val value: Double, val unit: String)
     *
     * val tempIP = InformationPacketFactory.create(
     *     payload = Temperature(22.5, "Celsius")
     * )
     * ```
     */
    inline fun <reified T : Any> create(
        payload: T,
        metadata: Map<String, String> = emptyMap()
    ): InformationPacket<T> {
        return InformationPacket(
            id = generateId(),
            dataType = T::class,
            payload = payload,
            metadata = metadata
        )
    }

    /**
     * Creates an InformationPacket with explicit type specification
     * Useful when the type cannot be inferred or needs to be specified explicitly
     *
     * @param T The type of data
     * @param dataType The KClass representing the type
     * @param payload The data content
     * @param metadata Optional metadata map
     * @return New InformationPacket instance
     */
    fun <T : Any> createWithType(
        dataType: KClass<T>,
        payload: T,
        metadata: Map<String, String> = emptyMap()
    ): InformationPacket<T> {
        return InformationPacket(
            id = generateId(),
            dataType = dataType,
            payload = payload,
            metadata = metadata
        )
    }

    /**
     * Generates a unique ID for an InformationPacket
     * Using timestamp + random to ensure uniqueness within a single JVM instance
     *
     * @return Unique identifier string
     */
    fun generateId(): String {
        val timestamp = System.currentTimeMillis()
        val random = (0..999999).random()
        return "ip_${timestamp}_$random"
    }
}
