package io.codenode.stopwatch.processingLogic

import io.codenode.fbpdsl.runtime.In2Out2TickBlock
import io.codenode.fbpdsl.runtime.ProcessResult2
import io.codenode.stopwatch.StopWatchState

/**
 * Tick function for the TimeIncrementer node.
 *
 * Node type: Processor (2 inputs, 2 outputs)
 *
 * Inputs:
 *   - elapsedSeconds: Int
 *   - elapsedMinutes: Int
 *
 * Outputs:
 *   - seconds: Int
 *   - minutes: Int
 *
 */
val timeIncrementerTick: In2Out2TickBlock<Int, Int, Int, Int> = { elapsedSeconds, elapsedMinutes ->
    var newSeconds = elapsedSeconds + 1
    var newMinutes = elapsedMinutes

    if (newSeconds >= 60) {
        newSeconds = 0
        newMinutes += 1
    }

    StopWatchState._elapsedSeconds.value = newSeconds
    StopWatchState._elapsedMinutes.value = newMinutes

    if (newMinutes != elapsedMinutes) {
        ProcessResult2.both(newSeconds, newMinutes)
    } else {
        ProcessResult2.first(newSeconds)
    }
}
