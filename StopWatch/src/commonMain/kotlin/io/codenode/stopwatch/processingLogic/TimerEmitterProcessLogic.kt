package io.codenode.stopwatch.processingLogic

import io.codenode.fbpdsl.runtime.Out2TickBlock
import io.codenode.fbpdsl.runtime.ProcessResult2

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

// Local state for timer tracking (replaces StateProperties)
private var currentSeconds = 0
private var currentMinutes = 0

/**
 * Resets the timer emitter's local state.
 * Called by StopWatchFlow.reset() to ensure timer starts from zero.
 */
fun resetTimerEmitterState() {
    currentSeconds = 0
    currentMinutes = 0
}

val timerEmitterTick: Out2TickBlock<Int, Int> = {
    var newSeconds = currentSeconds + 1
    var newMinutes = currentMinutes

    if (newSeconds >= 60) {
        newSeconds = 0
        newMinutes += 1
    }

    currentSeconds = newSeconds
    currentMinutes = newMinutes

    ProcessResult2.both(newSeconds, newMinutes)
}
