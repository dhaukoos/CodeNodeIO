package io.codenode.stopwatch.processingLogic

import io.codenode.fbpdsl.runtime.In2SinkTickBlock

/**
 * Tick function for the DisplayReceiver node.
 *
 * Node type: Sink (2 inputs, 0 outputs)
 *
 * Inputs:
 *   - seconds: Int
 *   - minutes: Int
 *
 * Note: State updates (StopWatchState._seconds, StopWatchState._minutes) are
 * handled by the Flow's consume block. This tick is a passthrough for any
 * additional display-side processing logic.
 */
val displayReceiverTick: In2SinkTickBlock<Int, Int> = { seconds, minutes ->
    // No-op: observable state updates are handled by the Flow's consume block
    // via StopWatchState._seconds.value and StopWatchState._minutes.value
}
