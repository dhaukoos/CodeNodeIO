/*
 * GenericNodeConfiguration - Configuration Data for Generic Node Types
 * Holds configurable properties for generic nodes with flexible input/output ports
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.factory

import io.codenode.fbpdsl.model.ValidationResult

/**
 * Configuration data class for generic node types.
 *
 * Generic nodes allow users to create flexible processing nodes with configurable
 * numbers of inputs (0-5) and outputs (0-5). This class holds the configuration
 * properties that define the node's structure and behavior.
 *
 * @property numInputs Number of input ports (must be 0-5)
 * @property numOutputs Number of output ports (must be 0-5)
 * @property customName User-defined display name, overrides default "in{M}out{N}" pattern
 * @property iconResource Path to custom icon/image resource for the node
 * @property inputNames Custom names for input ports (size must match numInputs if provided)
 * @property outputNames Custom names for output ports (size must match numOutputs if provided)
 * @property useCaseClassName Fully-qualified class name for the UseCase providing processing logic
 */
data class GenericNodeConfiguration(
    val numInputs: Int,
    val numOutputs: Int,
    val customName: String? = null,
    val iconResource: String? = null,
    val inputNames: List<String>? = null,
    val outputNames: List<String>? = null,
    val useCaseClassName: String? = null
) {
    companion object {
        /** Minimum number of input/output ports */
        const val MIN_PORTS = 0

        /** Maximum number of input/output ports */
        const val MAX_PORTS = 5
    }

    /**
     * Generates the default name for this generic node type.
     *
     * @return Name in format "in{M}out{N}" (e.g., "in2out1")
     */
    fun defaultName(): String = "in${numInputs}out${numOutputs}"

    /**
     * Gets the effective display name for this node.
     *
     * @return Custom name if set, otherwise the default name
     */
    fun effectiveName(): String = customName ?: defaultName()

    /**
     * Generates default input port names.
     *
     * @return List of names like ["input1", "input2", ...]
     */
    fun defaultInputNames(): List<String> = (1..numInputs).map { "input$it" }

    /**
     * Generates default output port names.
     *
     * @return List of names like ["output1", "output2", ...]
     */
    fun defaultOutputNames(): List<String> = (1..numOutputs).map { "output$it" }

    /**
     * Gets the effective input port names.
     *
     * @return Custom names if provided, otherwise default names
     */
    fun effectiveInputNames(): List<String> = inputNames ?: defaultInputNames()

    /**
     * Gets the effective output port names.
     *
     * @return Custom names if provided, otherwise default names
     */
    fun effectiveOutputNames(): List<String> = outputNames ?: defaultOutputNames()

    /**
     * Validates this configuration.
     *
     * Checks that:
     * - numInputs is in range [0, 5]
     * - numOutputs is in range [0, 5]
     * - inputNames size matches numInputs (if provided)
     * - outputNames size matches numOutputs (if provided)
     *
     * @return ValidationResult with success flag and error messages
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        if (numInputs !in MIN_PORTS..MAX_PORTS) {
            errors.add("numInputs must be between $MIN_PORTS and $MAX_PORTS, was: $numInputs")
        }

        if (numOutputs !in MIN_PORTS..MAX_PORTS) {
            errors.add("numOutputs must be between $MIN_PORTS and $MAX_PORTS, was: $numOutputs")
        }

        if (inputNames != null && inputNames.size != numInputs) {
            errors.add("inputNames size (${inputNames.size}) must match numInputs ($numInputs)")
        }

        if (outputNames != null && outputNames.size != numOutputs) {
            errors.add("outputNames size (${outputNames.size}) must match numOutputs ($numOutputs)")
        }

        // Validate port name uniqueness within input names
        if (inputNames != null) {
            val duplicateInputs = inputNames.groupingBy { it }.eachCount().filter { it.value > 1 }
            if (duplicateInputs.isNotEmpty()) {
                errors.add("inputNames contains duplicates: ${duplicateInputs.keys.joinToString(", ")}")
            }
        }

        // Validate port name uniqueness within output names
        if (outputNames != null) {
            val duplicateOutputs = outputNames.groupingBy { it }.eachCount().filter { it.value > 1 }
            if (duplicateOutputs.isNotEmpty()) {
                errors.add("outputNames contains duplicates: ${duplicateOutputs.keys.joinToString(", ")}")
            }
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Checks if this configuration is valid.
     *
     * @return true if configuration passes all validation rules
     */
    fun isValid(): Boolean = validate().success

    /**
     * Generates a unique identifier for this generic node configuration.
     *
     * @return ID in format "generic_in{M}_out{N}" or derived from custom name
     */
    fun generateId(): String {
        val baseName = customName?.lowercase()?.replace(" ", "_") ?: "generic_in${numInputs}_out${numOutputs}"
        return baseName.replace(Regex("[^a-z0-9_]"), "_")
    }

    /**
     * Generates a description for this generic node type.
     *
     * @return Auto-generated description based on port configuration
     */
    fun generateDescription(): String {
        val inputDesc = when (numInputs) {
            0 -> "no inputs"
            1 -> "1 input"
            else -> "$numInputs inputs"
        }
        val outputDesc = when (numOutputs) {
            0 -> "no outputs"
            1 -> "1 output"
            else -> "$numOutputs outputs"
        }
        val useCaseDesc = if (useCaseClassName != null) " (UseCase: ${useCaseClassName.substringAfterLast('.')})" else ""
        return "Generic processing node with $inputDesc and $outputDesc$useCaseDesc"
    }
}
