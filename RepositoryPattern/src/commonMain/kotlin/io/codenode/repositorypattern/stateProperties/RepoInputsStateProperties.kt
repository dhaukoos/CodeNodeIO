package io.codenode.repositorypattern.stateProperties

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State properties for the RepoInputs node.
 *
 * Output ports:
 *   - output1: Any
 *   - output2: Any
 *   - output3: Any
 */
object RepoInputsStateProperties {

    internal val _output1 = MutableStateFlow(TODO("Provide initial value for Any"))
    val output1Flow: StateFlow<Any> = _output1.asStateFlow()

    internal val _output2 = MutableStateFlow(TODO("Provide initial value for Any"))
    val output2Flow: StateFlow<Any> = _output2.asStateFlow()

    internal val _output3 = MutableStateFlow(TODO("Provide initial value for Any"))
    val output3Flow: StateFlow<Any> = _output3.asStateFlow()

    fun reset() {
        _output1.value = TODO("Provide initial value for Any")
        _output2.value = TODO("Provide initial value for Any")
        _output3.value = TODO("Provide initial value for Any")
    }
}
