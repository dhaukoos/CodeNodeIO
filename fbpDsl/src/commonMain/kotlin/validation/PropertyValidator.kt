/*
 * PropertyValidator - Validation Logic for Node Properties
 * Provides comprehensive validation for PropertyConfiguration against schemas
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.validation

import io.codenode.fbpdsl.model.*

/**
 * Result of property validation containing success status and detailed errors.
 */
data class PropertyValidationResult(
    val isValid: Boolean,
    val errors: Map<String, List<String>> = emptyMap(),
    val warnings: Map<String, List<String>> = emptyMap()
) {
    /** All error messages flattened to a list */
    val allErrors: List<String>
        get() = errors.flatMap { (key, messages) -> messages.map { "$key: $it" } }

    /** All warning messages flattened to a list */
    val allWarnings: List<String>
        get() = warnings.flatMap { (key, messages) -> messages.map { "$key: $it" } }

    /** Whether there are any warnings */
    val hasWarnings: Boolean get() = warnings.isNotEmpty()

    companion object {
        /** Creates a successful validation result */
        fun success() = PropertyValidationResult(isValid = true)

        /** Creates a failed validation result with errors */
        fun failure(errors: Map<String, List<String>>) =
            PropertyValidationResult(isValid = false, errors = errors)
    }
}

/**
 * PropertyValidator provides comprehensive validation for node property configurations.
 *
 * Supports validation against:
 * - Required fields
 * - Type constraints (string, number, boolean, enum)
 * - Range constraints (min/max for numbers)
 * - Pattern constraints (regex for strings)
 * - Custom validation rules
 */
object PropertyValidator {

    /**
     * Validates a PropertyConfiguration against a NodeTypeDefinition's schema
     *
     * @param config The property configuration to validate
     * @param nodeTypeDef The node type definition with schema and defaults
     * @return PropertyValidationResult with validation status and any errors
     */
    fun validate(
        config: PropertyConfiguration,
        nodeTypeDef: NodeTypeDefinition
    ): PropertyValidationResult {
        val errors = mutableMapOf<String, MutableList<String>>()
        val warnings = mutableMapOf<String, MutableList<String>>()

        // Merge with defaults for complete property set
        val mergedConfig = config.mergeWithDefaults(nodeTypeDef)

        // Parse schema if available
        val schemaConstraints = parseSchema(nodeTypeDef.configurationSchema)

        // Validate each property
        mergedConfig.properties.forEach { (key, value) ->
            val constraints = schemaConstraints[key] ?: PropertyConstraints(key)
            val propertyErrors = validateProperty(key, value, constraints)
            if (propertyErrors.isNotEmpty()) {
                errors[key] = propertyErrors.toMutableList()
            }
        }

        // Check for required properties
        schemaConstraints.filter { it.value.required }.forEach { (key, _) ->
            if (!mergedConfig.properties.containsKey(key) ||
                mergedConfig.properties[key].isNullOrBlank()) {
                errors.getOrPut(key) { mutableListOf() }.add("$key is required")
            }
        }

        // Check for unknown properties (warning only)
        config.properties.keys.filter { key ->
            !nodeTypeDef.defaultConfiguration.containsKey(key) &&
            !schemaConstraints.containsKey(key)
        }.forEach { key ->
            warnings.getOrPut(key) { mutableListOf() }
                .add("Unknown property '$key' not in schema")
        }

        return PropertyValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Validates a single property value against constraints
     *
     * @param key Property name
     * @param value Property value
     * @param constraints Validation constraints
     * @return List of error messages (empty if valid)
     */
    fun validateProperty(
        key: String,
        value: String,
        constraints: PropertyConstraints
    ): List<String> {
        val errors = mutableListOf<String>()

        // Skip validation for empty non-required fields
        if (value.isBlank() && !constraints.required) {
            return emptyList()
        }

        // Required check
        if (constraints.required && value.isBlank()) {
            errors.add("$key is required")
            return errors // Don't continue validation for empty required fields
        }

        // Type-specific validation
        when (constraints.type) {
            PropertyValueType.STRING -> {
                // Min length
                constraints.minLength?.let { min ->
                    if (value.length < min) {
                        errors.add("$key must be at least $min characters")
                    }
                }
                // Max length
                constraints.maxLength?.let { max ->
                    if (value.length > max) {
                        errors.add("$key must be at most $max characters")
                    }
                }
                // Pattern
                constraints.pattern?.let { pattern ->
                    try {
                        if (!value.matches(Regex(pattern))) {
                            errors.add("$key does not match required pattern")
                        }
                    } catch (e: Exception) {
                        // Invalid pattern - skip
                    }
                }
            }

            PropertyValueType.NUMBER -> {
                val numValue = value.toDoubleOrNull()
                if (numValue == null) {
                    errors.add("$key must be a valid number")
                } else {
                    // Minimum
                    constraints.minimum?.let { min ->
                        if (numValue < min) {
                            errors.add("$key must be at least $min")
                        }
                    }
                    // Maximum
                    constraints.maximum?.let { max ->
                        if (numValue > max) {
                            errors.add("$key must be at most $max")
                        }
                    }
                    // Exclusive minimum
                    constraints.exclusiveMinimum?.let { min ->
                        if (numValue <= min) {
                            errors.add("$key must be greater than $min")
                        }
                    }
                    // Exclusive maximum
                    constraints.exclusiveMaximum?.let { max ->
                        if (numValue >= max) {
                            errors.add("$key must be less than $max")
                        }
                    }
                    // Multiple of
                    constraints.multipleOf?.let { mult ->
                        if (numValue % mult != 0.0) {
                            errors.add("$key must be a multiple of $mult")
                        }
                    }
                }
            }

            PropertyValueType.INTEGER -> {
                val intValue = value.toLongOrNull()
                if (intValue == null) {
                    errors.add("$key must be a valid integer")
                } else {
                    constraints.minimum?.let { min ->
                        if (intValue < min) {
                            errors.add("$key must be at least ${min.toLong()}")
                        }
                    }
                    constraints.maximum?.let { max ->
                        if (intValue > max) {
                            errors.add("$key must be at most ${max.toLong()}")
                        }
                    }
                }
            }

            PropertyValueType.BOOLEAN -> {
                val validBooleans = listOf("true", "false", "1", "0", "yes", "no", "on", "off")
                if (value.lowercase() !in validBooleans) {
                    errors.add("$key must be a boolean (true/false)")
                }
            }

            PropertyValueType.ENUM -> {
                if (constraints.enumValues != null && value !in constraints.enumValues) {
                    errors.add("$key must be one of: ${constraints.enumValues.joinToString(", ")}")
                }
            }
        }

        // Custom validation
        constraints.customValidator?.let { validator ->
            val customError = validator(value)
            if (customError != null) {
                errors.add(customError)
            }
        }

        return errors
    }

    /**
     * Validates expression references in properties against a FlowGraph
     *
     * @param config The property configuration
     * @param graph The flow graph containing nodes for reference resolution
     * @return PropertyValidationResult with validation status
     */
    fun validateExpressions(
        config: PropertyConfiguration,
        graph: FlowGraph
    ): PropertyValidationResult {
        val errors = mutableMapOf<String, MutableList<String>>()

        config.properties.forEach { (key, value) ->
            if (config.isExpression(key)) {
                val refs = config.getExpressionReferences(key)
                refs.forEach { ref ->
                    val parts = ref.split(".")
                    if (parts.size < 2) {
                        errors.getOrPut(key) { mutableListOf() }
                            .add("Invalid expression reference '$ref' (expected: nodeName.portName)")
                        return@forEach
                    }

                    val nodeName = parts[0]
                    val portName = parts[1]

                    val node = graph.getAllNodes().find { it.name == nodeName }
                    if (node == null) {
                        errors.getOrPut(key) { mutableListOf() }
                            .add("Expression references non-existent node: $nodeName")
                        return@forEach
                    }

                    val port = node.outputPorts.find { it.name == portName }
                    if (port == null) {
                        errors.getOrPut(key) { mutableListOf() }
                            .add("Expression references non-existent port: $nodeName.$portName")
                    }
                }
            }
        }

        return PropertyValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Performs quick validation without schema (basic type checking)
     *
     * @param properties Map of property key-value pairs
     * @param typeHints Optional map of property names to expected types
     * @return PropertyValidationResult
     */
    fun quickValidate(
        properties: Map<String, String>,
        typeHints: Map<String, PropertyValueType> = emptyMap()
    ): PropertyValidationResult {
        val errors = mutableMapOf<String, MutableList<String>>()

        properties.forEach { (key, value) ->
            val type = typeHints[key] ?: inferType(value)
            val constraints = PropertyConstraints(key, type = type)
            val propErrors = validateProperty(key, value, constraints)
            if (propErrors.isNotEmpty()) {
                errors[key] = propErrors.toMutableList()
            }
        }

        return PropertyValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Infers the property type from its value
     */
    private fun inferType(value: String): PropertyValueType {
        return when {
            value.isBlank() -> PropertyValueType.STRING
            value.toLongOrNull() != null -> PropertyValueType.INTEGER
            value.toDoubleOrNull() != null -> PropertyValueType.NUMBER
            value.lowercase() in listOf("true", "false") -> PropertyValueType.BOOLEAN
            else -> PropertyValueType.STRING
        }
    }

    /**
     * Parses a JSON Schema string to extract property constraints
     */
    private fun parseSchema(schema: String?): Map<String, PropertyConstraints> {
        if (schema.isNullOrBlank()) return emptyMap()

        // Simple JSON parsing - in production, use a JSON Schema library
        return try {
            parseSimpleJsonSchema(schema)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Simple JSON Schema parser for common validation rules
     */
    private fun parseSimpleJsonSchema(schema: String): Map<String, PropertyConstraints> {
        val constraints = mutableMapOf<String, PropertyConstraints>()

        // Extract required array
        val requiredRegex = """"required"\s*:\s*\[([^\]]*)\]""".toRegex()
        val requiredMatch = requiredRegex.find(schema)
        val requiredFields = requiredMatch?.groupValues?.get(1)
            ?.split(",")
            ?.map { it.trim().trim('"') }
            ?.toSet() ?: emptySet()

        // Extract properties object
        val propertiesRegex = """"properties"\s*:\s*\{([^}]*(?:\{[^}]*\}[^}]*)*)\}""".toRegex()
        val propertiesMatch = propertiesRegex.find(schema)
        val propertiesContent = propertiesMatch?.groupValues?.get(1) ?: return emptyMap()

        // Parse each property
        val propertyPattern = """"(\w+)"\s*:\s*\{([^}]*)\}""".toRegex()
        propertyPattern.findAll(propertiesContent).forEach { match ->
            val propName = match.groupValues[1]
            val propDef = match.groupValues[2]

            val type = extractString(propDef, "type")
            val valueType = when (type) {
                "integer" -> PropertyValueType.INTEGER
                "number" -> PropertyValueType.NUMBER
                "boolean" -> PropertyValueType.BOOLEAN
                "string" -> {
                    if (propDef.contains("\"enum\"")) PropertyValueType.ENUM
                    else PropertyValueType.STRING
                }
                else -> PropertyValueType.STRING
            }

            val enumValues = if (valueType == PropertyValueType.ENUM) {
                extractArray(propDef, "enum")
            } else null

            constraints[propName] = PropertyConstraints(
                name = propName,
                type = valueType,
                required = propName in requiredFields,
                minimum = extractNumber(propDef, "minimum"),
                maximum = extractNumber(propDef, "maximum"),
                exclusiveMinimum = extractNumber(propDef, "exclusiveMinimum"),
                exclusiveMaximum = extractNumber(propDef, "exclusiveMaximum"),
                minLength = extractNumber(propDef, "minLength")?.toInt(),
                maxLength = extractNumber(propDef, "maxLength")?.toInt(),
                pattern = extractString(propDef, "pattern"),
                enumValues = enumValues
            )
        }

        return constraints
    }

    private fun extractString(json: String, key: String): String? {
        val regex = """"$key"\s*:\s*"([^"]*)"""".toRegex()
        return regex.find(json)?.groupValues?.get(1)
    }

    private fun extractNumber(json: String, key: String): Double? {
        val regex = """"$key"\s*:\s*(-?\d+\.?\d*)""".toRegex()
        return regex.find(json)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractArray(json: String, key: String): List<String>? {
        val regex = """"$key"\s*:\s*\[([^\]]*)\]""".toRegex()
        return regex.find(json)?.groupValues?.get(1)
            ?.split(",")
            ?.map { it.trim().trim('"') }
            ?.filter { it.isNotBlank() }
    }
}

/**
 * Supported property value types
 */
enum class PropertyValueType {
    STRING,
    NUMBER,
    INTEGER,
    BOOLEAN,
    ENUM
}

/**
 * Validation constraints for a property
 */
data class PropertyConstraints(
    val name: String,
    val type: PropertyValueType = PropertyValueType.STRING,
    val required: Boolean = false,
    val minimum: Double? = null,
    val maximum: Double? = null,
    val exclusiveMinimum: Double? = null,
    val exclusiveMaximum: Double? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null,
    val enumValues: List<String>? = null,
    val multipleOf: Double? = null,
    val customValidator: ((String) -> String?)? = null
)

/**
 * Builder for PropertyConstraints
 */
class PropertyConstraintsBuilder(private val name: String) {
    private var type: PropertyValueType = PropertyValueType.STRING
    private var required: Boolean = false
    private var minimum: Double? = null
    private var maximum: Double? = null
    private var minLength: Int? = null
    private var maxLength: Int? = null
    private var pattern: String? = null
    private var enumValues: List<String>? = null
    private var customValidator: ((String) -> String?)? = null

    fun type(type: PropertyValueType) = apply { this.type = type }
    fun required(required: Boolean = true) = apply { this.required = required }
    fun min(value: Double) = apply { this.minimum = value }
    fun max(value: Double) = apply { this.maximum = value }
    fun range(min: Double, max: Double) = apply { this.minimum = min; this.maximum = max }
    fun minLength(length: Int) = apply { this.minLength = length }
    fun maxLength(length: Int) = apply { this.maxLength = length }
    fun length(min: Int, max: Int) = apply { this.minLength = min; this.maxLength = max }
    fun pattern(regex: String) = apply { this.pattern = regex }
    fun enum(vararg values: String) = apply { this.enumValues = values.toList() }
    fun validate(validator: (String) -> String?) = apply { this.customValidator = validator }

    fun build() = PropertyConstraints(
        name = name,
        type = type,
        required = required,
        minimum = minimum,
        maximum = maximum,
        minLength = minLength,
        maxLength = maxLength,
        pattern = pattern,
        enumValues = enumValues,
        customValidator = customValidator
    )
}

/**
 * DSL function for building constraints
 */
fun constraints(name: String, block: PropertyConstraintsBuilder.() -> Unit): PropertyConstraints {
    return PropertyConstraintsBuilder(name).apply(block).build()
}
