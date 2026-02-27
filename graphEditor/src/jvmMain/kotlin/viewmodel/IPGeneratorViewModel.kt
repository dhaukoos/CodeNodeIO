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
 * UI state for a single property row in the IP Generator form.
 *
 * @param id Unique identifier for the row (for list operations)
 * @param name Property name entered by user
 * @param selectedTypeId Selected IP type ID from dropdown
 * @param isRequired Whether property is required or optional
 */
data class IPPropertyState(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val selectedTypeId: String = "ip_any",
    val isRequired: Boolean = true
)

/**
 * State data class for the IP Generator Panel.
 * Contains form fields for defining a custom IP type.
 *
 * @param typeName User-entered type name
 * @param properties Current property rows in the form
 * @param isExpanded Whether the panel is expanded
 * @param existingTypeNames Set of existing type names for conflict detection (case-insensitive)
 */
data class IPGeneratorPanelState(
    val typeName: String = "",
    val properties: List<IPPropertyState> = emptyList(),
    val isExpanded: Boolean = false,
    val existingTypeNames: Set<String> = emptySet()
) : BaseState {
    val hasNameConflict: Boolean
        get() = typeName.isNotBlank() && existingTypeNames.any {
            it.equals(typeName.trim(), ignoreCase = true)
        }

    val hasEmptyPropertyNames: Boolean
        get() = properties.any { it.name.isBlank() }

    val hasDuplicatePropertyNames: Boolean
        get() = properties
            .filter { it.name.isNotBlank() }
            .groupBy { it.name.trim().lowercase() }
            .any { it.value.size > 1 }

    val duplicatePropertyNameIds: Set<String>
        get() {
            val grouped = properties
                .filter { it.name.isNotBlank() }
                .groupBy { it.name.trim().lowercase() }
            return grouped.filter { it.value.size > 1 }
                .flatMap { it.value.map { p -> p.id } }
                .toSet()
        }

    val isValid: Boolean
        get() = typeName.isNotBlank() &&
                !hasNameConflict &&
                !hasEmptyPropertyNames &&
                !hasDuplicatePropertyNames
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

    private val _state = MutableStateFlow(IPGeneratorPanelState(
        existingTypeNames = ipTypeRegistry.getAllTypes().map { it.typeName }.toSet()
    ))
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
     * Adds a new empty property row to the form.
     */
    fun addProperty() {
        _state.update { it.copy(properties = it.properties + IPPropertyState()) }
    }

    /**
     * Removes a property row by its unique ID.
     *
     * @param id The property row ID to remove
     */
    fun removeProperty(id: String) {
        _state.update { it.copy(properties = it.properties.filter { p -> p.id != id }) }
    }

    /**
     * Updates the name of a property row.
     *
     * @param id The property row ID
     * @param name The new property name
     */
    fun updatePropertyName(id: String, name: String) {
        _state.update { state ->
            state.copy(properties = state.properties.map { p ->
                if (p.id == id) p.copy(name = name) else p
            })
        }
    }

    /**
     * Updates the selected type of a property row.
     *
     * @param id The property row ID
     * @param typeId The new IP type ID
     */
    fun updatePropertyType(id: String, typeId: String) {
        _state.update { state ->
            state.copy(properties = state.properties.map { p ->
                if (p.id == id) p.copy(selectedTypeId = typeId) else p
            })
        }
    }

    /**
     * Updates the required flag of a property row.
     *
     * @param id The property row ID
     * @param isRequired Whether the property is required
     */
    fun updatePropertyRequired(id: String, isRequired: Boolean) {
        _state.update { state ->
            state.copy(properties = state.properties.map { p ->
                if (p.id == id) p.copy(isRequired = isRequired) else p
            })
        }
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

        val properties = currentState.properties.map { propertyState ->
            IPProperty(
                name = propertyState.name.trim(),
                typeId = propertyState.selectedTypeId,
                isRequired = propertyState.isRequired
            )
        }

        val definition = CustomIPTypeDefinition(
            id = "ip_${currentState.typeName.lowercase().replace(" ", "_")}_${UUID.randomUUID().toString().take(8)}",
            typeName = currentState.typeName.trim(),
            properties = properties,
            color = CustomIPTypeDefinition.nextColor(ipTypeRegistry.customTypeCount())
        )

        ipTypeRegistry.registerCustomType(definition)
        repository.add(definition)
        _state.update {
            IPGeneratorPanelState(
                isExpanded = it.isExpanded,
                existingTypeNames = ipTypeRegistry.getAllTypes().map { t -> t.typeName }.toSet()
            )
        }
        return definition
    }

    /**
     * Resets the form to its default state.
     * Preserves the expanded state of the panel.
     */
    fun reset() {
        _state.update {
            IPGeneratorPanelState(
                isExpanded = it.isExpanded,
                existingTypeNames = it.existingTypeNames
            )
        }
    }

    fun refreshExistingTypeNames() {
        _state.update {
            it.copy(existingTypeNames = ipTypeRegistry.getAllTypes().map { t -> t.typeName }.toSet())
        }
    }
}
