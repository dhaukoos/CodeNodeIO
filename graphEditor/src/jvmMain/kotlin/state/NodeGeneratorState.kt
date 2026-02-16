/*
 * NodeGeneratorState - Form state for the Node Generator panel
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

/**
 * Holds the current form state for the Node Generator panel.
 * Immutable data class - all mutations return new instances via copy().
 *
 * @param name User-entered node name
 * @param inputCount Number of input ports (0-3)
 * @param outputCount Number of output ports (0-3)
 */
data class NodeGeneratorState(
    val name: String = "",
    val inputCount: Int = 1,
    val outputCount: Int = 1
) {
    /**
     * Computed property: form is valid when name is non-blank AND
     * at least one port exists (not both 0/0).
     */
    val isValid: Boolean
        get() = name.isNotBlank() && !(inputCount == 0 && outputCount == 0)

    /**
     * Computed property: genericType string following the pattern "inXoutY"
     * where X is the number of inputs and Y is the number of outputs.
     */
    val genericType: String
        get() = "in${inputCount}out${outputCount}"

    /**
     * Reset form to default values.
     */
    fun reset(): NodeGeneratorState = NodeGeneratorState()

    /**
     * Update name field.
     */
    fun withName(name: String): NodeGeneratorState = copy(name = name)

    /**
     * Update input count, coerced to valid range [0, 3].
     */
    fun withInputCount(count: Int): NodeGeneratorState = copy(inputCount = count.coerceIn(0, 3))

    /**
     * Update output count, coerced to valid range [0, 3].
     */
    fun withOutputCount(count: Int): NodeGeneratorState = copy(outputCount = count.coerceIn(0, 3))
}
