package io.codenode.stopwatch4.processingLogic

import io.codenode.fbpdsl.runtime.Out2TickBlock
import io.codenode.fbpdsl.runtime.ProcessResult2

/**
 * Tick function for the TimerEmitter node.
 *
 * Node type: Generator (0 inputs, 2 outputs)
 *
 * Outputs:
 *   - elapsedSeconds: Int
 *   - elapsedMinuts: Int
 *
 */
val timerEmitterTick: Out2TickBlock<Int, Int> = {
    var newSeconds = _elapsedSeconds.value + 1
    var newMinutes = _elapsedMinutes.value

    if (newSeconds >= 60) {
        newSeconds = 0
        newMinutes += 1
    }

    _elapsedSeconds.value = newSeconds
    _elapsedMinutes.value = newMinutes

    ProcessResult2.both(newSeconds, newMinutes)
}
