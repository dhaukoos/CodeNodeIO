package io.codenode.stopwatch.processingLogic

import io.codenode.fbpdsl.runtime.In2Out2TickBlock
import io.codenode.fbpdsl.runtime.ProcessResult2

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
    // TODO: Implement TimeIncrementer tick logic
    ProcessResult2.both(0, 0)
}
