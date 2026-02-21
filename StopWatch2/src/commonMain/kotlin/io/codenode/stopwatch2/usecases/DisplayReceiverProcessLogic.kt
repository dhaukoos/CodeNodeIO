package io.codenode.stopwatch2.usecases.logicmethods

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
 */
val displayReceiverTick: In2SinkTickBlock<Int, Int> = { seconds, minutes ->
    // TODO: Implement DisplayReceiver tick logic
}
