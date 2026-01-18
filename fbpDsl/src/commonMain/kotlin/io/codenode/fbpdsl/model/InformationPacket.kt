/*
 * InformationPacket (IP) - Core FBP Data Carrier
 * Represents immutable data flowing through the FBP graph using Kotlin's generic types
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlin.reflect.KClass

/**
 * InformationPacket (IP) represents type-safe data flowing through the FBP graph.
 * IPs are immutable data carriers that move between nodes through connections.
 *
 * Based on J. Paul Morrison's Flow-based Programming principles, where IPs
 * are discrete units of data that maintain identity as they flow through the system.
 *
 * This implementation uses Kotlin's generic types where each type is represented
 * by a Kotlin data class, providing compile-time type safety and runtime type information.
 *
 * @param T The type of data this IP carries (must be a Kotlin data class)
 * @property id Unique identifier for this IP instance
 * @property dataType Runtime type information (KClass) for type checking and validation
 * @property payload The actual data content (strongly typed as T)
 * @property metadata Optional metadata (source node, timestamp, trace ID for debugging)
 *
 * @sample
 * ```kotlin
 * // Define a data class for the payload
 * data class UserData(val name: String, val email: String)
 *
 * // Create a typed InformationPacket
 * val userIP = InformationPacket(
 *     id = "ip_123",
 *     dataType = UserData::class,
 *     payload = UserData("John Doe", "john@example.com")
 * )
 * ```
 */
data class InformationPacket<T : Any>(
    val id: String,
    val dataType: KClass<T>,
    val payload: T,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * State of the Information Packet in its lifecycle
     */
    enum class State {
        /** IP has been created but not yet sent */
        CREATED,

        /** IP is currently in transit through a connection */
        IN_TRANSIT,

        /** IP has been consumed by a target node */
        CONSUMED,

        /** IP has been archived for debugging/replay */
        ARCHIVED
    }

    /**
     * Gets the simple name of the data type (e.g., "UserData" instead of full qualified name)
     *
     * @return Simple class name of the payload type
     */
    val typeName: String
        get() = dataType.simpleName ?: dataType.toString()

    /**
     * Validates that this IP is well-formed and ready for transmission
     *
     * @return true if IP is valid, false otherwise
     */
    fun isValid(): Boolean {
        return id.isNotBlank() && payload::class == dataType
    }

    /**
     * Checks if the payload is an instance of the declared data type
     *
     * @return true if payload type matches dataType declaration
     */
    fun hasValidType(): Boolean {
        return dataType.isInstance(payload)
    }

    /**
     * Creates a copy of this IP with updated metadata
     *
     * @param additionalMetadata New metadata to merge with existing
     * @return New IP with combined metadata
     */
    fun withMetadata(additionalMetadata: Map<String, String>): InformationPacket<T> {
        return copy(metadata = metadata + additionalMetadata)
    }

    /**
     * Adds a trace entry to track IP flow through the graph
     *
     * @param nodeId ID of the node processing this IP
     * @param timestamp Processing timestamp
     * @return New IP with trace entry added
     */
    fun addTrace(nodeId: String, timestamp: Long = System.currentTimeMillis()): InformationPacket<T> {
        val traceEntry = "node:$nodeId,time:$timestamp"
        val traces = metadata["traces"]?.let { "$it;$traceEntry" } ?: traceEntry
        return withMetadata(mapOf("traces" to traces))
    }

    /**
     * Transforms this IP's payload to a different type
     *
     * @param T2 The target type for transformation
     * @param transformer Function that transforms the payload from T to T2
     * @return New InformationPacket with transformed payload
     */
    inline fun <reified T2 : Any> transform(transformer: (T) -> T2): InformationPacket<T2> {
        return InformationPacket(
            id = id,
            dataType = T2::class,
            payload = transformer(payload),
            metadata = metadata
        )
    }

    override fun toString(): String {
        return "InformationPacket(id='$id', type='$typeName', payload=$payload, metadata=$metadata)"
    }
}

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

// ============================================================================
// Common Data Types - Example data classes for common FBP use cases
// ============================================================================

/**
 * Simple string data payload
 */
data class StringData(val value: String)

/**
 * Numeric data payload
 */
data class NumberData(val value: Double)

/**
 * Boolean flag data payload
 */
data class BooleanData(val value: Boolean)

/**
 * Generic error data with message and optional code
 */
data class ErrorData(
    val message: String,
    val code: String? = null,
    val details: Map<String, String> = emptyMap()
)

/**
 * List/collection data payload
 */
data class ListData<T : Any>(val items: List<T>)

/**
 * Map/dictionary data payload
 */
data class MapData<K : Any, V : Any>(val entries: Map<K, V>)
