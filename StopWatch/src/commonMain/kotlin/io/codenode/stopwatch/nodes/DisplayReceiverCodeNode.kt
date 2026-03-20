/*
 * DisplayReceiverCodeNode - Self-contained sink node for display output
 * License: Apache 2.0
 */

package io.codenode.stopwatch.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.stopwatch.StopWatchState

/**
 * Sink node that receives display seconds and minutes and updates StopWatchState.
 *
 * Uses SinkIn2Any pattern (fires on ANY input change) with initial values of 0
 * for both channels. Updates _seconds and _minutes in StopWatchState for UI display.
 */
object DisplayReceiverCodeNode : CodeNodeDefinition {
    override val name = "DisplayReceiver"
    override val category = CodeNodeType.SINK
    override val description = "Displays seconds and minutes from timer processing"
    override val inputPorts = listOf(
        PortSpec("seconds", Int::class),
        PortSpec("minutes", Int::class)
    )
    override val outputPorts = emptyList<PortSpec>()
    override val anyInput = true

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createSinkIn2Any<Int, Int>(
            name = name,
            initialValue1 = 0,
            initialValue2 = 0,
            consume = { seconds, minutes ->
                StopWatchState._seconds.value = seconds
                StopWatchState._minutes.value = minutes
            }
        )
    }
}
