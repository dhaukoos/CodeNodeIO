/*
 * NodeGeneratorViewModel - ViewModel for the Node Generator Panel
 * Encapsulates state and business logic for creating custom node types
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import io.codenode.grapheditor.repository.CustomNodeDefinition
import io.codenode.grapheditor.repository.CustomNodeRepository

/**
 * State data class for the Node Generator Panel.
 * Contains all UI state including form fields and dropdown expansion states.
 *
 * @param name User-entered node name
 * @param inputCount Number of input ports (0-3)
 * @param outputCount Number of output ports (0-3)
 * @param isExpanded Whether the panel is expanded
 * @param inputDropdownExpanded Whether the input count dropdown is open
 * @param outputDropdownExpanded Whether the output count dropdown is open
 */
data class NodeGeneratorPanelState(
    val name: String = "",
    val inputCount: Int = 1,
    val outputCount: Int = 1,
    val isExpanded: Boolean = false,
    val inputDropdownExpanded: Boolean = false,
    val outputDropdownExpanded: Boolean = false
) : BaseState {
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
}

/**
 * ViewModel for the Node Generator Panel.
 * Manages state and business logic for creating custom node types.
 *
 * This ViewModel encapsulates all state that was previously scattered across
 * the NodeGeneratorPanel composable and the external NodeGeneratorState.
 *
 * @param customNodeRepository Repository for persisting custom node definitions
 */
class NodeGeneratorViewModel(
    private val customNodeRepository: CustomNodeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NodeGeneratorPanelState())
    val state: StateFlow<NodeGeneratorPanelState> = _state.asStateFlow()

    /**
     * Updates the node name field.
     *
     * @param name The new name value
     */
    fun setName(name: String) {
        _state.update { it.copy(name = name) }
    }

    /**
     * Updates the input count, coerced to valid range [0, 3].
     *
     * @param count The new input count
     */
    fun setInputCount(count: Int) {
        _state.update { it.copy(inputCount = count.coerceIn(0, 3)) }
    }

    /**
     * Updates the output count, coerced to valid range [0, 3].
     *
     * @param count The new output count
     */
    fun setOutputCount(count: Int) {
        _state.update { it.copy(outputCount = count.coerceIn(0, 3)) }
    }

    /**
     * Toggles the expanded/collapsed state of the panel.
     */
    fun toggleExpanded() {
        _state.update { it.copy(isExpanded = !it.isExpanded) }
    }

    /**
     * Sets the input dropdown expanded state.
     *
     * @param expanded Whether the dropdown should be expanded
     */
    fun setInputDropdownExpanded(expanded: Boolean) {
        _state.update { it.copy(inputDropdownExpanded = expanded) }
    }

    /**
     * Sets the output dropdown expanded state.
     *
     * @param expanded Whether the dropdown should be expanded
     */
    fun setOutputDropdownExpanded(expanded: Boolean) {
        _state.update { it.copy(outputDropdownExpanded = expanded) }
    }

    /**
     * Creates a new custom node definition from the current form state.
     * Only creates if the state is valid. After successful creation,
     * the node is added to the repository and the form is reset.
     *
     * @return The created CustomNodeDefinition, or null if state was invalid
     */
    fun createNode(): CustomNodeDefinition? {
        val currentState = _state.value
        if (!currentState.isValid) return null

        val node = CustomNodeDefinition.create(
            name = currentState.name.trim(),
            inputCount = currentState.inputCount,
            outputCount = currentState.outputCount
        )
        customNodeRepository.add(node)
        reset()
        return node
    }

    /**
     * Resets the form to its default state.
     * Preserves the expanded state of the panel.
     */
    fun reset() {
        _state.update {
            NodeGeneratorPanelState(isExpanded = it.isExpanded)
        }
    }
}
