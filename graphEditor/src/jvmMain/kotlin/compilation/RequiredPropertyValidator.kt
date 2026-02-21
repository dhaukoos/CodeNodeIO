/*
 * RequiredPropertyValidator - Validates Required Configuration Properties
 * Ensures CodeNodes have all required properties before compilation
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compilation

import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.FlowGraph

/**
 * Represents a single missing required property on a node.
 *
 * @property nodeId Unique identifier of the node
 * @property nodeName Human-readable name for error messages
 * @property propertyName Name of the missing property
 * @property reason Why this property is required
 */
data class PropertyValidationError(
    val nodeId: String,
    val nodeName: String,
    val propertyName: String,
    val reason: String = "required for code generation"
)

/**
 * Result of validating all nodes in a FlowGraph for required properties.
 *
 * @property success True if all required properties are defined
 * @property errors List of all validation errors found
 */
data class PropertyValidationResult(
    val success: Boolean,
    val errors: List<PropertyValidationError> = emptyList()
) {
    /**
     * Formats validation errors into a human-readable message.
     * Groups errors by node and lists missing properties for each.
     *
     * @return Formatted error message, empty string if validation succeeded
     */
    fun toErrorMessage(): String {
        if (success) return ""
        return errors.groupBy { it.nodeId }.entries.joinToString("\n") { (_, nodeErrors) ->
            val nodeName = nodeErrors.first().nodeName
            val properties = nodeErrors.map { it.propertyName }.joinToString(", ")
            "Node '$nodeName' missing required properties: $properties"
        }
    }
}

/**
 * Validates that CodeNodes have all required configuration properties.
 *
 * Used by CompilationService before module generation to ensure
 * all GENERIC nodes have the necessary properties for code generation.
 *
 * @sample
 * ```kotlin
 * val validator = RequiredPropertyValidator()
 * val result = validator.validate(flowGraph)
 * if (!result.success) {
 *     println("Validation failed: ${result.toErrorMessage()}")
 * }
 * ```
 */
class RequiredPropertyValidator {

    /**
     * Map of CodeNodeType to required property names.
     * No configuration properties are currently required — port counts and types
     * are derived directly from the CodeNode model by RuntimeTypeResolver.
     */
    private val requiredSpecs = mapOf<CodeNodeType, Set<String>>()

    /**
     * Validates all nodes in the flow graph have required properties.
     *
     * Iterates through all CodeNodes and checks that each has all
     * required properties defined (non-null and non-blank).
     *
     * @param flowGraph The flow graph to validate
     * @return PropertyValidationResult with success status and any errors
     */
    fun validate(flowGraph: FlowGraph): PropertyValidationResult {
        val errors = mutableListOf<PropertyValidationError>()

        flowGraph.getAllCodeNodes().forEach { node ->
            val required = getRequiredProperties(node.codeNodeType)
            required.forEach { propertyName ->
                val value = node.configuration[propertyName]
                if (value.isNullOrBlank()) {
                    errors.add(
                        PropertyValidationError(
                            nodeId = node.id,
                            nodeName = node.name,
                            propertyName = propertyName,
                            reason = "required for code generation"
                        )
                    )
                }
            }
        }

        return PropertyValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Gets the required properties for a specific node type.
     *
     * @param nodeType The CodeNodeType to check
     * @return Set of required property names, empty if none required
     */
    fun getRequiredProperties(nodeType: CodeNodeType): Set<String> {
        return requiredSpecs[nodeType] ?: emptySet()
    }
}
