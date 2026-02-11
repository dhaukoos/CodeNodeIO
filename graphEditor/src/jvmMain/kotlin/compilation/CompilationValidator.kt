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
 * Pre-defined specification for the processingLogicFile required property.
 *
 * Validates that:
 * - The property is not blank
 * - The value ends with .kt (Kotlin file extension)
 * - The referenced file exists relative to the project root
 */
val PROCESSING_LOGIC_SPEC = RequiredPropertySpec(
    key = "processingLogicFile",
    displayName = "Processing Logic",
    validator = { path, projectRoot ->
        when {
            path.isBlank() -> "Path is required"
            !path.endsWith(".kt") -> "Must be a Kotlin file (.kt)"
            !File(projectRoot, path).exists() -> "File not found: $path"
            else -> null  // Valid
        }
    }
)
