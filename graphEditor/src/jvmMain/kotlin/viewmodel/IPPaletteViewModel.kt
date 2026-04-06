/*
 * IPPaletteViewModel - ViewModel for the IP Type Palette Panel
 * Encapsulates state and business logic for IP type browsing and selection
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import io.codenode.fbpdsl.model.InformationPacketType
import io.codenode.flowgraphtypes.repository.FileIPTypeRepository
import io.codenode.flowgraphtypes.registry.IPTypeRegistry

/**
 * State data class for the IP Palette Panel.
 * Contains search query and selection state.
 *
 * @param searchQuery Current search filter text
 * @param selectedTypeId ID of the currently selected IP type, or null if none
 */
data class IPPaletteState(
    val searchQuery: String = "",
    val selectedTypeId: String? = null,
    val deletableTypeIds: Set<String> = emptySet()
) : BaseState

/**
 * ViewModel for the IP Palette Panel.
 * Manages state and business logic for IP type browsing and selection.
 *
 * This ViewModel encapsulates:
 * - Search query state for filtering IP types
 * - Selection state for the currently selected IP type
 *
 * @param onTypeSelected Optional callback when a type is selected (for external coordination)
 */
class IPPaletteViewModel(
    private val ipTypeRegistry: IPTypeRegistry? = null,
    private val ipTypeRepository: FileIPTypeRepository? = null,
    private val onCustomTypesChanged: () -> Unit = {},
    private val onTypeSelected: (InformationPacketType?) -> Unit = {}
) : ViewModel() {

    private val _state = MutableStateFlow(IPPaletteState())
    val state: StateFlow<IPPaletteState> = _state.asStateFlow()

    /**
     * Updates the search query for filtering IP types.
     *
     * @param query The new search query text
     */
    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    /**
     * Selects an IP type.
     *
     * @param ipType The IP type to select
     */
    fun selectType(ipType: InformationPacketType) {
        _state.update { it.copy(selectedTypeId = ipType.id) }
        onTypeSelected(ipType)
    }

    /**
     * Clears the current selection.
     */
    fun clearSelection() {
        _state.update { it.copy(selectedTypeId = null) }
        onTypeSelected(null)
    }

    /**
     * Toggles selection of an IP type.
     * If the type is already selected, it will be deselected.
     * If it's not selected, it will be selected.
     *
     * @param ipType The IP type to toggle
     */
    fun toggleSelection(ipType: InformationPacketType) {
        val currentSelectedId = _state.value.selectedTypeId
        if (currentSelectedId == ipType.id) {
            clearSelection()
        } else {
            selectType(ipType)
        }
    }

    /**
     * Clears the search query.
     */
    fun clearSearch() {
        _state.update { it.copy(searchQuery = "") }
    }

    /**
     * Checks if an IP type matches the current search query.
     *
     * @param ipType The IP type to check
     * @return true if the type matches the search, false otherwise
     */
    fun matchesSearch(ipType: InformationPacketType): Boolean {
        val query = _state.value.searchQuery
        if (query.isBlank()) return true
        return ipType.typeName.contains(query, ignoreCase = true) ||
               ipType.description?.contains(query, ignoreCase = true) == true
    }

    /**
     * Updates the set of deletable type IDs.
     * Called when the list of custom IP types changes.
     *
     * @param ids Set of type IDs that can be deleted
     */
    fun updateDeletableTypeIds(ids: Set<String>) {
        _state.update { it.copy(deletableTypeIds = ids) }
    }

    /**
     * Deletes a custom IP type by its ID.
     * Unregisters from the registry, removes from the repository, and notifies listeners.
     *
     * @param id The ID of the type to delete
     * @return true if the type was found and deleted, false otherwise
     */
    fun deleteCustomType(id: String): Boolean {
        val registry = ipTypeRegistry ?: return false
        val repository = ipTypeRepository ?: return false

        // Delete the filesystem file if it exists
        val filePath = registry.getFilePath(id)
        if (filePath != null) {
            try {
                val file = java.io.File(filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {
                // File deletion failed — continue with registry/repo cleanup
            }
        }

        val removed = registry.unregister(id)
        return if (removed != null) {
            repository.remove(id)
            val updatedIds = registry.getCustomTypeIds()
            _state.update { it.copy(deletableTypeIds = updatedIds) }
            onCustomTypesChanged()
            true
        } else {
            false
        }
    }
}
