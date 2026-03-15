/*
 * PipelineValidation - Pre-start validation types for dynamic pipelines
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

/**
 * Type of validation error detected during pipeline validation.
 */
enum class ValidationErrorType {
    /** A node name on the canvas has no matching CodeNodeDefinition in the registry */
    UNRESOLVABLE_NODE,

    /** A connection references a port that doesn't exist on the resolved node */
    INVALID_PORT,

    /** The connection graph contains a cycle */
    CYCLE_DETECTED
}

/**
 * A single validation error found during pipeline pre-start validation.
 *
 * @property type The category of validation error
 * @property nodeId The node instance ID involved (if applicable)
 * @property nodeName The node name involved (if applicable)
 * @property message Human-readable error description
 */
data class ValidationError(
    val type: ValidationErrorType,
    val nodeId: String? = null,
    val nodeName: String? = null,
    val message: String
)

/**
 * The outcome of pre-start pipeline validation.
 *
 * @property isValid Whether the pipeline can be built
 * @property errors List of specific errors found (empty if valid)
 */
data class PipelineValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError> = emptyList()
) {
    companion object {
        /** Creates a successful validation result */
        fun valid(): PipelineValidationResult = PipelineValidationResult(isValid = true)

        /** Creates a failed validation result with the given errors */
        fun errors(errors: List<ValidationError>): PipelineValidationResult =
            PipelineValidationResult(isValid = false, errors = errors)
    }
}
