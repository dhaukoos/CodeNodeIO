/*
 * Port - Entry/Exit Point for Node Data Flow
 * Defines how nodes can be connected and what types of data they accept/emit
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Port represents an entry or exit point on a Node for data flow.
 * Ports define how nodes can be connected and what types of data they accept/emit.
 *
 * Based on FBP principles, ports are typed connection points that enforce
 * data flow compatibility and validation.
 *
 * @property id Unique identifier for this port
 * @property name Human-readable name (e.g., "input", "output", "error")
 * @property direction Whether this port receives (INPUT) or sends (OUTPUT) data
 * @property dataType Expected InformationPacket type for type safety
 * @property required Whether this port must be connected for valid graph
 * @property defaultValue Optional default IP if input port is unconnected
 * @property validationRules Optional validation logic for incoming IPs
 * @property owningNodeId Reference to the parent Node that owns this port
 */
@Serializable
data class Port(
    val id: String,
    val name: String,
    val direction: Direction,
    val dataType: String,
    val required: Boolean = false,
    val defaultValue: JsonElement? = null,
    val validationRules: List<String> = emptyList(),
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
     * Validates that this port is well-formed
     *
     * @return true if port is valid, false otherwise
     */
    fun isValid(): Boolean {
        return id.isNotBlank() &&
                name.isNotBlank() &&
                dataType.isNotBlank() &&
                owningNodeId.isNotBlank()
    }

    /**
     * Checks if this port is compatible with another port for connection
     *
     * @param other The port to check compatibility with
     * @return true if ports can be connected, false otherwise
     */
    fun isCompatibleWith(other: Port): Boolean {
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
    private fun areTypesCompatible(sourceType: String, targetType: String): Boolean {
        // Exact match
        if (sourceType == targetType) return true

        // "Any" type is compatible with everything
        if (targetType == "Any" || sourceType == "Any") return true

        // TODO: Implement more sophisticated type compatibility (inheritance, generics)
        return false
    }

    /**
     * Validates an InformationPacket against this port's rules
     *
     * @param ip The InformationPacket to validate
     * @return Validation result with success flag and error messages
     */
    fun validatePacket(ip: InformationPacket): ValidationResult {
        val errors = mutableListOf<String>()

        // Check type compatibility
        if (ip.type != dataType && dataType != "Any") {
            errors.add("Type mismatch: expected '$dataType', got '${ip.type}'")
        }

        // Apply custom validation rules
        validationRules.forEach { rule ->
            // TODO: Implement validation rule execution
            // For now, just validate that IP is well-formed
            if (!ip.isValid()) {
                errors.add("IP failed validation rule: $rule")
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
    fun withOwner(newOwningNodeId: String): Port {
        return copy(owningNodeId = newOwningNodeId)
    }

}

/**
 * Factory for creating Port instances
 */
object PortFactory {
    /**
     * Creates a simple INPUT port with default settings
     *
     * @param name Port name
     * @param dataType Expected data type
     * @param owningNodeId Parent node ID
     * @param required Whether connection is mandatory
     * @return New Port instance
     */
    fun input(
        name: String,
        dataType: String,
        owningNodeId: String,
        required: Boolean = false
    ): Port {
        return Port(
            id = generateId(),
            name = name,
            direction = Port.Direction.INPUT,
            dataType = dataType,
            required = required,
            owningNodeId = owningNodeId
        )
    }

    /**
     * Creates a simple OUTPUT port with default settings
     *
     * @param name Port name
     * @param dataType Data type this port emits
     * @param owningNodeId Parent node ID
     * @return New Port instance
     */
    fun output(
        name: String,
        dataType: String,
        owningNodeId: String
    ): Port {
        return Port(
            id = generateId(),
            name = name,
            direction = Port.Direction.OUTPUT,
            dataType = dataType,
            owningNodeId = owningNodeId
        )
    }

    /**
     * Generates a unique ID for a Port
     *
     * @return Unique identifier string
     */
    private fun generateId(): String {
        val timestamp = System.currentTimeMillis()
        val random = (0..999999).random()
        return "port_${timestamp}_$random"
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
