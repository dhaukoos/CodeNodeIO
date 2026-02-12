/*
 * CompilationValidator - Pre-compilation validation for FlowGraph
 * Validates that all required properties are configured before code generation
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compilation

import java.io.File

/**
 * Describes a validation error for a specific property.
 *
 * @property propertyName The name of the property that failed validation
 * @property reason The error message explaining why validation failed (e.g., "file not found", "invalid format")
 */
data class PropertyValidationError(
    val propertyName: String,
    val reason: String
)

/**
 * Describes a validation error for a specific node.
 *
 * Contains lists of missing required properties and invalid properties for a single CodeNode.
 *
 * @property nodeId The unique identifier of the node with validation errors
 * @property nodeName The display name of the node for user-friendly error messages
 * @property missingProperties List of property names that are required but not configured
 * @property invalidProperties List of property validation errors for configured but invalid properties
 */
data class NodeValidationError(
    val nodeId: String,
    val nodeName: String,
    val missingProperties: List<String> = emptyList(),
    val invalidProperties: List<PropertyValidationError> = emptyList()
) {
    /**
     * Generates a human-readable error message for this node.
     *
     * Example outputs:
     * - "TimerEmitter: Missing required: processingLogicFile"
     * - "DisplayReceiver: Invalid: processingLogicFile (file not found)"
     * - "MyNode: Missing required: processingLogicFile; Invalid: speedAttenuation"
     */
    val message: String
        get() = buildString {
            append("$nodeName: ")
            if (missingProperties.isNotEmpty()) {
                append("Missing required: ${missingProperties.joinToString(", ")}")
            }
            if (invalidProperties.isNotEmpty()) {
                if (missingProperties.isNotEmpty()) append("; ")
                append("Invalid: ${invalidProperties.joinToString(", ") { "${it.propertyName} (${it.reason})" }}")
            }
        }
}

/**
 * Result of pre-compilation validation check.
 *
 * Contains the overall success status and a list of errors for any nodes that failed validation.
 *
 * @property success Whether validation passed (true if no errors)
 * @property nodeErrors List of validation errors, one per node with issues
 */
data class CompilationValidationResult(
    val success: Boolean,
    val nodeErrors: List<NodeValidationError> = emptyList()
) {
    /**
     * Whether the validation result indicates the graph is valid for compilation.
     * Equivalent to success && nodeErrors.isEmpty() but provided for clarity.
     */
    val isValid: Boolean get() = success && nodeErrors.isEmpty()

    /**
     * Generates a summary of all validation errors for display.
     *
     * Returns "Validation passed" if no errors, otherwise returns each node's
     * error message on a separate line.
     */
    val errorSummary: String
        get() = if (isValid) {
            "Validation passed"
        } else {
            nodeErrors.joinToString("\n") { it.message }
        }

    companion object {
        /**
         * Creates a successful validation result with no errors.
         */
        fun success(): CompilationValidationResult = CompilationValidationResult(success = true)

        /**
         * Creates a failed validation result with the given node errors.
         */
        fun failure(errors: List<NodeValidationError>): CompilationValidationResult =
            CompilationValidationResult(success = false, nodeErrors = errors)
    }
}

/**
 * Specification for a required property that must be validated before compilation.
 *
 * Defines what property to check, how to display it to users, and an optional
 * validator function for custom validation logic beyond "is not blank".
 *
 * @property key The configuration key to check (e.g., "processingLogicFile")
 * @property displayName Human-readable name for error messages (e.g., "Processing Logic")
 * @property validator Optional validation function that returns an error message or null if valid.
 *                     The function receives the property value and should check format, existence, etc.
 */
data class RequiredPropertySpec(
    val key: String,
    val displayName: String,
    val validator: ((String, File) -> String?)? = null
)

/**
 * Pre-defined specification for the _useCaseClass required property.
 * This is the configuration key used in the Properties Panel for ProcessingLogic files.
 *
 * Validates that:
 * - The property is not blank
 * - The value ends with .kt (Kotlin file extension)
 * - The referenced file exists relative to the project root
 */
val USE_CASE_CLASS_SPEC = RequiredPropertySpec(
    key = "_useCaseClass",
    displayName = "UseCase Class",
    validator = { path, projectRoot ->
        when {
            path.isBlank() -> "Path is required"
            !path.endsWith(".kt") -> "Must be a Kotlin file (.kt)"
            else -> {
                // Handle both absolute and relative paths
                val file = if (File(path).isAbsolute) {
                    File(path)
                } else {
                    File(projectRoot, path)
                }
                if (!file.exists()) "File not found: ${file.absolutePath}" else null
            }
        }
    }
)

/**
 * Alias for backward compatibility - processingLogicFile maps to _useCaseClass
 */
val PROCESSING_LOGIC_SPEC = USE_CASE_CLASS_SPEC

/**
 * Validates FlowGraph before compilation to ensure all required properties are configured.
 *
 * Checks each CodeNode for required properties like _useCaseClass and validates
 * that configured values are valid (file exists, correct extension, etc.).
 */
object CompilationValidator {

    /**
     * List of required property specifications to validate for each CodeNode.
     */
    private val requiredProperties = listOf(USE_CASE_CLASS_SPEC)

    /**
     * Validates a FlowGraph for compilation readiness.
     *
     * Iterates over all CodeNodes in the graph and validates that each has all
     * required properties configured with valid values.
     *
     * @param flowGraph The flow graph to validate
     * @param projectRoot Project root directory for resolving relative file paths
     * @return CompilationValidationResult indicating success or listing all validation errors
     */
    fun validate(flowGraph: io.codenode.fbpdsl.model.FlowGraph, projectRoot: File): CompilationValidationResult {
        val nodeErrors = mutableListOf<NodeValidationError>()

        // T028: Iterate over all CodeNodes to check required properties
        flowGraph.rootNodes
            .filterIsInstance<io.codenode.fbpdsl.model.CodeNode>()
            .forEach { node ->
                val error = validateNode(node, projectRoot)
                if (error != null) {
                    nodeErrors.add(error)
                }
            }

        // T030: Aggregate NodeValidationError list into CompilationValidationResult
        return if (nodeErrors.isEmpty()) {
            CompilationValidationResult.success()
        } else {
            CompilationValidationResult.failure(nodeErrors)
        }
    }

    /**
     * Validates a single CodeNode for required properties.
     *
     * @param node The CodeNode to validate
     * @param projectRoot Project root for resolving file paths
     * @return NodeValidationError if validation failed, null if valid
     */
    private fun validateNode(
        node: io.codenode.fbpdsl.model.CodeNode,
        projectRoot: File
    ): NodeValidationError? {
        val missingProperties = mutableListOf<String>()
        val invalidProperties = mutableListOf<PropertyValidationError>()

        // T029: Validate each required property
        requiredProperties.forEach { spec ->
            val value = node.configuration[spec.key]

            if (value == null || value.isBlank()) {
                // Property is missing or blank
                missingProperties.add(spec.displayName)
            } else if (spec.validator != null) {
                // Property exists, run custom validator
                val errorMessage = spec.validator.invoke(value, projectRoot)
                if (errorMessage != null) {
                    invalidProperties.add(PropertyValidationError(spec.displayName, errorMessage))
                }
            }
        }

        // Return error only if there are issues
        return if (missingProperties.isNotEmpty() || invalidProperties.isNotEmpty()) {
            NodeValidationError(
                nodeId = node.id,
                nodeName = node.name,
                missingProperties = missingProperties,
                invalidProperties = invalidProperties
            )
        } else {
            null
        }
    }
}
