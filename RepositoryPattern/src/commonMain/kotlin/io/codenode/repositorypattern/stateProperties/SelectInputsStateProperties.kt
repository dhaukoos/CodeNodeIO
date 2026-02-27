package io.codenode.repositorypattern.stateProperties

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State properties for the SelectInputs node.
 *
 * Output ports:
 *   - filterBy: Any
 *   - sortBy: Any
 */
object SelectInputsStateProperties {

    internal val _filterBy = MutableStateFlow(TODO("Provide initial value for Any"))
    val filterByFlow: StateFlow<Any> = _filterBy.asStateFlow()

    internal val _sortBy = MutableStateFlow(TODO("Provide initial value for Any"))
    val sortByFlow: StateFlow<Any> = _sortBy.asStateFlow()

    fun reset() {
        _filterBy.value = TODO("Provide initial value for Any")
        _sortBy.value = TODO("Provide initial value for Any")
    }
}
