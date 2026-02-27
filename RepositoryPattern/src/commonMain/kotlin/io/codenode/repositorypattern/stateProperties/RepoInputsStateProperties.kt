package io.codenode.repositorypattern.stateProperties

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State properties for the RepoInputs node.
 *
 * Output ports:
 *   - save: Any
 *   - update: Any
 *   - remove: Any
 */
object RepoInputsStateProperties {

    internal val _save = MutableStateFlow(TODO("Provide initial value for Any"))
    val saveFlow: StateFlow<Any> = _save.asStateFlow()

    internal val _update = MutableStateFlow(TODO("Provide initial value for Any"))
    val updateFlow: StateFlow<Any> = _update.asStateFlow()

    internal val _remove = MutableStateFlow(TODO("Provide initial value for Any"))
    val removeFlow: StateFlow<Any> = _remove.asStateFlow()

    fun reset() {
        _save.value = TODO("Provide initial value for Any")
        _update.value = TODO("Provide initial value for Any")
        _remove.value = TODO("Provide initial value for Any")
    }
}
