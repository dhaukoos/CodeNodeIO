package io.codenode.repositorypattern.stateProperties

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State properties for the SelectInputs node.
 *
 * Output ports:
 *   - output1: Any
 *   - output2: Any
 */
object SelectInputsStateProperties {

    internal val _output1 = MutableStateFlow(TODO("Provide initial value for Any"))
    val output1Flow: StateFlow<Any> = _output1.asStateFlow()

    internal val _output2 = MutableStateFlow(TODO("Provide initial value for Any"))
    val output2Flow: StateFlow<Any> = _output2.asStateFlow()

    fun reset() {
        _output1.value = TODO("Provide initial value for Any")
        _output2.value = TODO("Provide initial value for Any")
    }
}
