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
 * Increments elapsed time using StopWatchState as the source of truth,
 * keeping observable state in sync with the processing logic.
 * Reset is handled by StopWatchState.reset().
 */
val timeIncrementerTick: In2Out2TickBlock<Int, Int, Int, Int> = { _, _ ->
    var newSeconds = StopWatchState._elapsedSeconds.value + 1
    var newMinutes = StopWatchState._elapsedMinutes.value

    if (newSeconds >= 60) {
        newSeconds = 0
        newMinutes += 1
    }

    StopWatchState._elapsedSeconds.value = newSeconds
    StopWatchState._elapsedMinutes.value = newMinutes

    ProcessResult2.both(newSeconds, newMinutes)
}
