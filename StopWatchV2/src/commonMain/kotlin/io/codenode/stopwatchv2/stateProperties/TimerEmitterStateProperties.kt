package io.codenode.stopwatchv2.stateProperties

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State properties for the TimerEmitter node.
 *
 * Output ports:
 *   - elapsedSeconds: Int
 *   - elapsedMinutes: Int
 */
object TimerEmitterStateProperties {

    internal val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSecondsFlow: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    internal val _elapsedMinutes = MutableStateFlow(0)
    val elapsedMinutesFlow: StateFlow<Int> = _elapsedMinutes.asStateFlow()

    fun reset() {
        _elapsedSeconds.value = 0
        _elapsedMinutes.value = 0
    }
}
