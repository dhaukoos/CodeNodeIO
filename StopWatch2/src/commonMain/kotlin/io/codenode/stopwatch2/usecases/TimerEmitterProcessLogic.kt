package io.codenode.stopwatch2.usecases.logicmethods

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
    // TODO: Implement TimerEmitter tick logic
    ProcessResult2.both(0, 0)
}
