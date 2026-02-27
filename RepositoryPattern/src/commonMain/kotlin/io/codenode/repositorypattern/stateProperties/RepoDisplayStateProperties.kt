package io.codenode.repositorypattern.stateProperties

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State properties for the RepoDisplay node.
 *
 * Input ports:
 *   - filteredList: Any
 */
object RepoDisplayStateProperties {

    internal val _filteredList = MutableStateFlow(TODO("Provide initial value for Any"))
    val filteredListFlow: StateFlow<Any> = _filteredList.asStateFlow()

    fun reset() {
        _filteredList.value = TODO("Provide initial value for Any")
    }
}
