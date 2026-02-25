package io.codenode.stopwatch.processingLogic

import io.codenode.fbpdsl.runtime.In2SinkTickBlock
import io.codenode.stopwatch.stateProperties.DisplayReceiverStateProperties

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
    DisplayReceiverStateProperties._seconds.value = seconds
    DisplayReceiverStateProperties._minutes.value = minutes
}
