package io.codenode.fbpdsl.model

import kotlin.reflect.KClass

/**
 * PortTemplate represents a specification for a port in a NodeTypeDefinition.
 * It defines the interface contract for ports that will be created when
 * a node is instantiated from this type definition.
 *
 * @property name Port name (e.g., "input", "output", "error")
 * @property direction Whether this port receives (INPUT) or sends (OUTPUT) data
 * @property dataType The KClass representing the expected data type
 * @property required Whether this port must be connected for valid graph
 * @property description Optional documentation for this port
 *
 * @sample
 *
 * ```kotlin
 * val inputPortTemplate = PortTemplate(
 *     name = "userInput",
 *     direction = Port.Direction.INPUT,
 *     dataType = UserData::class,
 *     required = true,
 *     description = "User data to be validated"
 * )
 * ```
 */
data class PortTemplate(
    val name: String,
    val direction: Port.Direction,
    val dataType: KClass<*>,
    val required: Boolean = false,
    val description: String? = null
) {
    /**
     * Gets the simple name of the data type
     *
     * @return Simple class name of the port's data type
     */
    val typeName: String
        get() = dataType.simpleName ?: dataType.toString()

    /**
     * Validates that this PortTemplate is well-formed
     *
     * @return Validation result with success flag and error messages
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        if (name.isBlank()) {
            errors.add("PortTemplate name cannot be blank")
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Creates a Port instance from this template for a specific owning node
     *
     * @param T The type parameter for the port
     * @param owningNodeId The ID of the node that will own this port
     * @return New Port instance based on this template
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> createPort(owningNodeId: String): Port<T> {
        return Port(
            id = PortFactory.generateId(),
            name = name,
            direction = direction,
            dataType = dataType as KClass<T>,
            required = required,
            defaultValue = null,
            validationRules = emptyList(),
            owningNodeId = owningNodeId
        )
    }

    /**
     * Checks if this port template is compatible with another for connection
     *
     * @param other The other port template to check compatibility with
     * @return true if ports can be connected, false otherwise
     */
    fun isCompatibleWith(other: PortTemplate): Boolean {
        // Can only connect OUTPUT to INPUT
        if (this.direction == Port.Direction.OUTPUT && other.direction == Port.Direction.INPUT) {
            return areTypesCompatible(this.dataType, other.dataType)
        }
        if (this.direction == Port.Direction.INPUT && other.direction == Port.Direction.OUTPUT) {
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

        // For more sophisticated type checking, we'd need platform-specific reflection
        return false
    }
}
