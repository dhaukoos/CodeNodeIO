package io.codenode.repositorypattern.stateProperties

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State properties for the GenericRepository node.
 *
 * Input ports:
 *   - insert: Any
 *   - update: Any
 *   - delete: Any
 * Output ports:
 *   - getAll: Any
 */
object GenericRepositoryStateProperties {

    internal val _insert = MutableStateFlow(TODO("Provide initial value for Any"))
    val insertFlow: StateFlow<Any> = _insert.asStateFlow()

    internal val _update = MutableStateFlow(TODO("Provide initial value for Any"))
    val updateFlow: StateFlow<Any> = _update.asStateFlow()

    internal val _delete = MutableStateFlow(TODO("Provide initial value for Any"))
    val deleteFlow: StateFlow<Any> = _delete.asStateFlow()

    internal val _getAll = MutableStateFlow(TODO("Provide initial value for Any"))
    val getAllFlow: StateFlow<Any> = _getAll.asStateFlow()

    fun reset() {
        _insert.value = TODO("Provide initial value for Any")
        _update.value = TODO("Provide initial value for Any")
        _delete.value = TODO("Provide initial value for Any")
        _getAll.value = TODO("Provide initial value for Any")
    }
}
