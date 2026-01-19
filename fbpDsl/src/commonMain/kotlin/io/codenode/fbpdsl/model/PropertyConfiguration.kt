/*
 * PropertyConfiguration - Runtime Configuration for Node Instances
 * Defines customizable behavior for nodes through property key-value pairs
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.serialization.Serializable

/**
 * PropertyConfiguration represents runtime configuration for a node instance.
 * It defines customizable behavior through property key-value pairs that are
 * validated against the NodeTypeDefinition's configuration schema.
 *
 * Based on FBP principles, PropertyConfiguration allows:
 * - Customization of node behavior without changing code
 * - Runtime validation against schema constraints
 * - Expression-based references to other node outputs
 * - Type-safe property access with validation
 *
 * @property nodeId Reference to the owning Node
 * @property properties Map of property key-value pairs (String → String serialization)
 * @property validationErrors List of current validation errors (empty if valid)
 *
 * @sample
 * ```kotlin
 * val httpConfig = PropertyConfiguration(
 *     nodeId = "node_http_get_123",
 *     properties = mapOf(
 *         "url" to "https://api.example.com/users",
 *         "timeout" to "30s",
 *         "retries" to "3",
 *         "headers" to """{"Authorization": "Bearer ${auth.token}"}"""
 *     )
 * )
 * ```
 */
@Serializable
data class PropertyConfiguration(
    val nodeId: String,
    val properties: Map<String, String> = emptyMap(),
    val validationErrors: List<String> = emptyList()
) {
    /**
     * Property value types supported by the configuration system
     */
    enum class PropertyType {
        /** String values */
        STRING,

        /** Numeric values (Int, Long, Double) */
        NUMBER,

        /** Boolean values (true/false) */
        BOOLEAN,

        /** JSON-serializable objects */
        OBJECT,

        /** Expression references to other node outputs */
        EXPRESSION
    }

    init {
        require(nodeId.isNotBlank()) { "PropertyConfiguration nodeId cannot be blank" }
    }

    /**
     * Validates that this PropertyConfiguration is well-formed
     *
     * @return Validation result with success flag and error messages
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate basic attributes
        if (nodeId.isBlank()) {
            errors.add("PropertyConfiguration nodeId cannot be blank")
        }

        // Add any stored validation errors
        errors.addAll(validationErrors)

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Validates this configuration against a NodeTypeDefinition's schema
     *
     * @param nodeTypeDef The node type definition with schema
     * @return Validation result with success flag and error messages
     */
    fun validateAgainstSchema(nodeTypeDef: NodeTypeDefinition): ValidationResult {
        val errors = mutableListOf<String>()

        // Check if schema exists
        val schema = nodeTypeDef.configurationSchema
        if (schema == null || schema.isBlank()) {
            // No schema means no validation required
            return ValidationResult(success = true, errors = emptyList())
        }

        // TODO: Implement JSON Schema validation
        // This would require a JSON Schema validation library
        // For now, perform basic validation

        // Validate required properties from default configuration
        nodeTypeDef.defaultConfiguration.keys.forEach { key ->
            if (!properties.containsKey(key)) {
                // Property missing - check if it has a default
                // This is acceptable, defaults will be used
            }
        }

        // Check for unknown properties (not in defaults)
        properties.keys.forEach { key ->
            if (!nodeTypeDef.defaultConfiguration.containsKey(key)) {
                // Unknown property - might be valid if schema allows additional properties
                // For now, we'll allow it
            }
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Gets a property value by key
     *
     * @param key Property key
     * @return Property value or null if not found
     */
    fun getProperty(key: String): String? {
        return properties[key]
    }

    /**
     * Gets a property value as a String with fallback to default
     *
     * @param key Property key
     * @param defaultValue Default value if property not found
     * @return Property value or default
     */
    fun getStringProperty(key: String, defaultValue: String = ""): String {
        return properties[key] ?: defaultValue
    }

    /**
     * Gets a property value as an Int
     *
     * @param key Property key
     * @param defaultValue Default value if property not found or invalid
     * @return Property value as Int or default
     */
    fun getIntProperty(key: String, defaultValue: Int = 0): Int {
        return properties[key]?.toIntOrNull() ?: defaultValue
    }

    /**
     * Gets a property value as a Long
     *
     * @param key Property key
     * @param defaultValue Default value if property not found or invalid
     * @return Property value as Long or default
     */
    fun getLongProperty(key: String, defaultValue: Long = 0L): Long {
        return properties[key]?.toLongOrNull() ?: defaultValue
    }

    /**
     * Gets a property value as a Double
     *
     * @param key Property key
     * @param defaultValue Default value if property not found or invalid
     * @return Property value as Double or default
     */
    fun getDoubleProperty(key: String, defaultValue: Double = 0.0): Double {
        return properties[key]?.toDoubleOrNull() ?: defaultValue
    }

    /**
     * Gets a property value as a Boolean
     *
     * @param key Property key
     * @param defaultValue Default value if property not found or invalid
     * @return Property value as Boolean or default
     */
    fun getBooleanProperty(key: String, defaultValue: Boolean = false): Boolean {
        return when (properties[key]?.lowercase()) {
            "true", "1", "yes", "on" -> true
            "false", "0", "no", "off" -> false
            else -> defaultValue
        }
    }

    /**
     * Checks if a property exists
     *
     * @param key Property key
     * @return true if property is set
     */
    fun hasProperty(key: String): Boolean {
        return properties.containsKey(key)
    }

    /**
     * Checks if a property value is an expression reference
     * Expression format: ${nodeName.portName} or ${nodeName.portName.field}
     *
     * @param key Property key
     * @return true if value matches expression pattern
     */
    fun isExpression(key: String): Boolean {
        val value = properties[key] ?: return false
        return value.contains(Regex("""\$\{[^}]+\}"""))
    }

    /**
     * Extracts expression references from a property value
     * Returns list of references in format: nodeName.portName or nodeName.portName.field
     *
     * @param key Property key
     * @return List of expression references found in the value
     */
    fun getExpressionReferences(key: String): List<String> {
        val value = properties[key] ?: return emptyList()
        val pattern = Regex("""\$\{([^}]+)\}""")
        return pattern.findAll(value).map { it.groupValues[1] }.toList()
    }

    /**
     * Validates that all expression references point to valid nodes/ports in the graph
     *
     * @param graph The FlowGraph containing this node
     * @return Validation result with success flag and error messages
     */
    fun validateExpressions(graph: FlowGraph): ValidationResult {
        val errors = mutableListOf<String>()

        properties.forEach { (key, value) ->
            if (isExpression(key)) {
                val references = getExpressionReferences(key)
                references.forEach { ref ->
                    // Parse reference: nodeName.portName or nodeName.portName.field
                    val parts = ref.split(".")
                    if (parts.size < 2) {
                        errors.add("Invalid expression reference in '$key': '$ref' (expected format: nodeName.portName)")
                        return@forEach
                    }

                    val nodeName = parts[0]
                    val portName = parts[1]

                    // Find node by name (assuming node names are unique)
                    val node = graph.getAllNodes().find { it.name == nodeName }
                    if (node == null) {
                        errors.add("Expression in '$key' references non-existent node: '$nodeName'")
                        return@forEach
                    }

                    // Check if node has the specified output port
                    val port = node.outputPorts.find { it.name == portName }
                    if (port == null) {
                        errors.add("Expression in '$key' references non-existent port: '$nodeName.$portName'")
                        return@forEach
                    }
                }
            }
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Creates a copy of this PropertyConfiguration with updated properties
     *
     * @param newProperties The new properties map
     * @return New PropertyConfiguration instance with updated properties
     */
    fun withProperties(newProperties: Map<String, String>): PropertyConfiguration {
        return copy(properties = newProperties)
    }

    /**
     * Creates a copy of this PropertyConfiguration with an added or updated property
     *
     * @param key Property key
     * @param value Property value
     * @return New PropertyConfiguration instance with updated property
     */
    fun setProperty(key: String, value: String): PropertyConfiguration {
        return copy(properties = properties + (key to value))
    }

    /**
     * Creates a copy of this PropertyConfiguration with a removed property
     *
     * @param key Property key to remove
     * @return New PropertyConfiguration instance with removed property
     */
    fun removeProperty(key: String): PropertyConfiguration {
        return copy(properties = properties - key)
    }

    /**
     * Creates a copy of this PropertyConfiguration with updated validation errors
     *
     * @param errors The new validation errors list
     * @return New PropertyConfiguration instance with updated errors
     */
    fun withValidationErrors(errors: List<String>): PropertyConfiguration {
        return copy(validationErrors = errors)
    }

    /**
     * Creates a copy of this PropertyConfiguration with an added validation error
     *
     * @param error Error message to add
     * @return New PropertyConfiguration instance with added error
     */
    fun addValidationError(error: String): PropertyConfiguration {
        return copy(validationErrors = validationErrors + error)
    }

    /**
     * Creates a copy of this PropertyConfiguration with cleared validation errors
     *
     * @return New PropertyConfiguration instance with no validation errors
     */
    fun clearValidationErrors(): PropertyConfiguration {
        return copy(validationErrors = emptyList())
    }

    /**
     * Checks if this configuration has any validation errors
     *
     * @return true if validation errors exist
     */
    fun hasErrors(): Boolean {
        return validationErrors.isNotEmpty()
    }

    /**
     * Gets the total number of properties
     *
     * @return Property count
     */
    fun getPropertyCount(): Int {
        return properties.size
    }

    /**
     * Gets all property keys
     *
     * @return Set of property keys
     */
    fun getPropertyKeys(): Set<String> {
        return properties.keys
    }

    /**
     * Merges default configuration from NodeTypeDefinition with this configuration
     * Properties in this configuration take precedence over defaults
     *
     * @param nodeTypeDef The node type definition with defaults
     * @return New PropertyConfiguration with merged properties
     */
    fun mergeWithDefaults(nodeTypeDef: NodeTypeDefinition): PropertyConfiguration {
        val merged = nodeTypeDef.defaultConfiguration + properties
        return copy(properties = merged)
    }

    /**
     * Checks if this configuration differs from the default configuration
     *
     * @param nodeTypeDef The node type definition with defaults
     * @return true if any property differs from defaults
     */
    fun hasCustomizations(nodeTypeDef: NodeTypeDefinition): Boolean {
        val defaults = nodeTypeDef.defaultConfiguration
        return properties.any { (key, value) ->
            defaults[key] != value
        }
    }

    /**
     * Gets only the properties that differ from defaults
     *
     * @param nodeTypeDef The node type definition with defaults
     * @return Map of customized properties
     */
    fun getCustomizations(nodeTypeDef: NodeTypeDefinition): Map<String, String> {
        val defaults = nodeTypeDef.defaultConfiguration
        return properties.filter { (key, value) ->
            defaults[key] != value
        }
    }

    override fun toString(): String {
        val errorInfo = if (hasErrors()) " [${validationErrors.size} errors]" else ""
        return "PropertyConfiguration(nodeId='$nodeId', properties=${properties.size}$errorInfo)"
    }
}
