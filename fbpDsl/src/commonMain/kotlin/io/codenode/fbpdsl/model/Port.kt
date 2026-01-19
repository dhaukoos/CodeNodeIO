/*
 * Port - Entry/Exit Point for Node Data Flow
 * Defines how nodes can be connected and what types of data they accept/emit
 * Uses Kotlin's generic types for compile-time type safety
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlin.reflect.KClass

/**
 * Port represents a type-safe entry or exit point on a Node for data flow.
 * Ports define how nodes can be connected and what types of data they accept/emit.
 *
 * Based on FBP principles, ports are typed connection points that enforce
 * data flow compatibility and validation using Kotlin's type system.
 *
 * This implementation uses KClass to represent data types, ensuring type safety
 * at both compile-time (where possible) and runtime.
 *
 * @param T The type of data this port accepts/emits (must be a Kotlin data class)
 * @property id Unique identifier for this port
 * @property name Human-readable name (e.g., "input", "output", "error")
 * @property direction Whether this port receives (INPUT) or sends (OUTPUT) data
 * @property dataType The KClass representing the expected data type
 * @property required Whether this port must be connected for valid graph
 * @property defaultValue Optional default payload if input port is unconnected
 * @property validationRules Optional validation predicates for incoming data
 * @property owningNodeId Reference to the parent Node that owns this port
 *
 * @sample
 * ```kotlin
 * data class UserData(val name: String, val email: String)
 *
 * // Create a typed input port
 * val userInputPort = Port(
 *     id = "port_1",
 *     name = "userInput",
 *     direction = Port.Direction.INPUT,
 *     dataType = UserData::class,
 *     required = true,
 *     owningNodeId = "node_123"
 * )
 * ```
 */
data class Port<T : Any>(
    val id: String,
    val name: String,
    val direction: Direction,
    val dataType: KClass<T>,
    val required: Boolean = false,
    val defaultValue: T? = null,
    val validationRules: List<(T) -> ValidationResult> = emptyList(),
    val owningNodeId: String
) {
    /**
     * Direction of data flow for this port
     */
    enum class Direction {
        /** Port receives data (input to node) */
        INPUT,

        /** Port sends data (output from node) */
        OUTPUT
    }

    /**
     * Gets the simple name of the data type (e.g., "UserData" instead of full qualified name)
     *
     * @return Simple class name of the port's data type
     */
    val typeName: String
        get() = dataType.simpleName ?: dataType.toString()

    /**
     * Validates that this port is well-formed
     *
     * @return true if port is valid, false otherwise
     */
    fun isValid(): Boolean {
        return id.isNotBlank() &&
                name.isNotBlank() &&
                owningNodeId.isNotBlank()
    }

    /**
     * Checks if this port is compatible with another port for connection
     *
     * @param other The port to check compatibility with
     * @return true if ports can be connected, false otherwise
     */
    fun isCompatibleWith(other: Port<*>): Boolean {
        // Can only connect OUTPUT to INPUT
        if (this.direction == Direction.OUTPUT && other.direction == Direction.INPUT) {
            return areTypesCompatible(this.dataType, other.dataType)
        }
        if (this.direction == Direction.INPUT && other.direction == Direction.OUTPUT) {
            return areTypesCompatible(other.dataType, this.dataType)
        }
        return false
    }

    /**
     * Checks if two data types are compatible for connection
     *
     * @param sourceType The type from the OUTPUT port
     * @param targetType The type from the INPUT port
     * @return true if types are compatible
     */
    private fun areTypesCompatible(sourceType: KClass<*>, targetType: KClass<*>): Boolean {
        // Exact match
        if (sourceType == targetType) return true

        // "Any" type is compatible with everything
        if (targetType == Any::class || sourceType == Any::class) return true

        // Check if sourceType is a subclass of targetType (covariance)
        // Note: In Kotlin/Common, full reflection may be limited
        // This is a basic check - more sophisticated type checking would require platform-specific code
        try {
            return targetType.isInstance(sourceType)
        } catch (e: Exception) {
            // If reflection fails, fall back to exact match only
            return false
        }
    }

    /**
     * Validates an InformationPacket against this port's rules
     *
     * @param ip The InformationPacket to validate
     * @return Validation result with success flag and error messages
     */
    fun validatePacket(ip: InformationPacket<T>): ValidationResult {
        val errors = mutableListOf<String>()

        // Check type compatibility
        if (!ip.hasValidType()) {
            errors.add("IP has invalid type: payload type does not match declared dataType")
        }

        if (ip.dataType != dataType && dataType != Any::class) {
            errors.add("Type mismatch: expected '${typeName}', got '${ip.typeName}'")
        }

        // Validate IP is well-formed
        if (!ip.isValid()) {
            errors.add("IP failed basic validation (invalid id or type mismatch)")
        }

        // Apply custom validation rules to the payload
        validationRules.forEach { rule ->
            val result = rule(ip.payload)
            if (!result.success) {
                errors.addAll(result.errors)
            }
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Creates a copy of this port with a new owning node ID
     *
     * @param newOwningNodeId The new owner node ID
     * @return New port instance with updated owner
     */
    fun withOwner(newOwningNodeId: String): Port<T> {
        return copy(owningNodeId = newOwningNodeId)
    }

}

/**
 * Result of port validation
 *
 * @property success Whether validation passed
 * @property errors List of validation error messages
 */
data class ValidationResult(
    val success: Boolean,
    val errors: List<String> = emptyList()
) {
    /**
     * Gets a formatted error message string
     */
    fun errorMessage(): String = errors.joinToString("; ")
}
