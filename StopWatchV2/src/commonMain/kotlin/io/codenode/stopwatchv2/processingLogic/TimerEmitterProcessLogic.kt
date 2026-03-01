package io.codenode.stopwatchv2.processingLogic

import io.codenode.fbpdsl.runtime.Out2TickBlock
import io.codenode.fbpdsl.runtime.ProcessResult2
import io.codenode.stopwatchv2.stateProperties.TimerEmitterStateProperties

/**
 * Tick function for the TimerEmitter node.
 *
 * Node type: Generator (0 inputs, 2 outputs)
 *
 * Outputs:
 *   - elapsedSeconds: Int
 *   - elapsedMinutes: Int
 *
 */
val timerEmitterTick: Out2TickBlock<Int, Int> = {
    var newSeconds = TimerEmitterStateProperties._elapsedSeconds.value + 1
    var newMinutes = TimerEmitterStateProperties._elapsedMinutes.value

    if (newSeconds >= 60) {
        newSeconds = 0
        newMinutes += 1
    }

    TimerEmitterStateProperties._elapsedSeconds.value = newSeconds
    TimerEmitterStateProperties._elapsedMinutes.value = newMinutes

    ProcessResult2.both(newSeconds, newMinutes)
}
