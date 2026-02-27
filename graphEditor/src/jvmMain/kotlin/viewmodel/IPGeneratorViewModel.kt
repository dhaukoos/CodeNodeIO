/*
 * IPGeneratorViewModel - ViewModel for the IP Generator Panel
 * Encapsulates state and business logic for creating custom IP types
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import androidx.lifecycle.ViewModel
import io.codenode.grapheditor.model.CustomIPTypeDefinition
import io.codenode.grapheditor.model.IPProperty
import io.codenode.grapheditor.repository.FileIPTypeRepository
import io.codenode.grapheditor.state.IPTypeRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * State data class for the IP Generator Panel.
 * Contains form fields for defining a custom IP type.
 *
 * @param typeName User-entered type name
 * @param isExpanded Whether the panel is expanded
 */
data class IPGeneratorPanelState(
    val typeName: String = "",
    val isExpanded: Boolean = false
) : BaseState {
    /**
     * Computed property: form is valid when type name is non-blank.
     * Additional validation (name conflicts, property validation) will be added in US3.
     */
    val isValid: Boolean
        get() = typeName.isNotBlank()
}

/**
 * ViewModel for the IP Generator Panel.
 * Manages state and business logic for creating custom IP types.
 *
 * @param ipTypeRegistry Registry where new types are registered
 * @param repository Repository for persisting custom IP types to disk
 */
class IPGeneratorViewModel(
    private val ipTypeRegistry: IPTypeRegistry,
    private val repository: FileIPTypeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(IPGeneratorPanelState())
    val state: StateFlow<IPGeneratorPanelState> = _state.asStateFlow()

    /**
     * Updates the type name field.
     *
     * @param name The new type name value
     */
    fun setTypeName(name: String) {
        _state.update { it.copy(typeName = name) }
    }

    /**
     * Toggles the expanded/collapsed state of the panel.
     */
    fun toggleExpanded() {
        _state.update { it.copy(isExpanded = !it.isExpanded) }
    }

    /**
     * Creates a new custom IP type from the current form state.
     * Only creates if the state is valid. After successful creation,
     * the type is registered in the IPTypeRegistry, persisted via the repository,
     * and the form is reset.
     *
     * @return The created CustomIPTypeDefinition, or null if state was invalid
     */
    fun createType(): CustomIPTypeDefinition? {
        val currentState = _state.value
        if (!currentState.isValid) return null

        val definition = CustomIPTypeDefinition(
            id = "ip_${currentState.typeName.lowercase().replace(" ", "_")}_${UUID.randomUUID().toString().take(8)}",
            typeName = currentState.typeName.trim(),
            properties = emptyList(),
            color = CustomIPTypeDefinition.nextColor(ipTypeRegistry.customTypeCount())
        )

        ipTypeRegistry.registerCustomType(definition)
        repository.add(definition)
        reset()
        return definition
    }

    /**
     * Resets the form to its default state.
     * Preserves the expanded state of the panel.
     */
    fun reset() {
        _state.update {
            IPGeneratorPanelState(isExpanded = it.isExpanded)
        }
    }
}
