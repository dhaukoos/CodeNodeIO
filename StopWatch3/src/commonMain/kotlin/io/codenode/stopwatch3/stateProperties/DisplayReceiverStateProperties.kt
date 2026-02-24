package io.codenode.stopwatch3.stateProperties

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State properties for the DisplayReceiver node.
 *
 * Input ports:
 *   - seconds: Int
 *   - minutes: Int
 */
object DisplayReceiverStateProperties {

    internal val _seconds = MutableStateFlow(0)
    val secondsFlow: StateFlow<Int> = _seconds.asStateFlow()

    internal val _minutes = MutableStateFlow(0)
    val minutesFlow: StateFlow<Int> = _minutes.asStateFlow()

    fun reset() {
        _seconds.value = 0
        _minutes.value = 0
    }
}
