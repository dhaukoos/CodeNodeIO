package io.codenode.repositorypattern.stateProperties

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State properties for the RepoDisplay node.
 *
 * Input ports:
 *   - input1: Any
 */
object RepoDisplayStateProperties {

    internal val _input1 = MutableStateFlow(TODO("Provide initial value for Any"))
    val input1Flow: StateFlow<Any> = _input1.asStateFlow()

    fun reset() {
        _input1.value = TODO("Provide initial value for Any")
    }
}
