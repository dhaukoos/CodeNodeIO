package io.codenode.repositorypattern.stateProperties

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State properties for the FilterSort node.
 *
 * Input ports:
 *   - filterBy: Any
 *   - sortBy: Any
 *   - observeAll: Any
 * Output ports:
 *   - filteredList: Any
 */
object FilterSortStateProperties {

    internal val _filterBy = MutableStateFlow(TODO("Provide initial value for Any"))
    val filterByFlow: StateFlow<Any> = _filterBy.asStateFlow()

    internal val _sortBy = MutableStateFlow(TODO("Provide initial value for Any"))
    val sortByFlow: StateFlow<Any> = _sortBy.asStateFlow()

    internal val _observeAll = MutableStateFlow(TODO("Provide initial value for Any"))
    val observeAllFlow: StateFlow<Any> = _observeAll.asStateFlow()

    internal val _filteredList = MutableStateFlow(TODO("Provide initial value for Any"))
    val filteredListFlow: StateFlow<Any> = _filteredList.asStateFlow()

    fun reset() {
        _filterBy.value = TODO("Provide initial value for Any")
        _sortBy.value = TODO("Provide initial value for Any")
        _observeAll.value = TODO("Provide initial value for Any")
        _filteredList.value = TODO("Provide initial value for Any")
    }
}
