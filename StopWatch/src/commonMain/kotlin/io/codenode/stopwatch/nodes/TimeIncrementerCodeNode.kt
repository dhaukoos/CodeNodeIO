/*
 * TimeIncrementerCodeNode - Self-contained processor node for time incrementing
 * License: Apache 2.0
 */

package io.codenode.stopwatch.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.fbpdsl.runtime.ProcessResult2
import io.codenode.stopwatch.StopWatchState

/**
 * Processor node that increments seconds and rolls over to minutes at 60.
 *
 * Receives elapsed seconds and minutes, increments seconds by 1,
 * rolls over to a new minute at 60 seconds, updates StopWatchState,
 * and emits the display values downstream.
 */
object TimeIncrementerCodeNode : CodeNodeDefinition {
    override val name = "TimeIncrementer"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Increments seconds and rolls over to minutes at 60"
    override val inputPorts = listOf(
        PortSpec("elapsedSeconds", Int::class),
        PortSpec("elapsedMinutes", Int::class)
    )
    override val outputPorts = listOf(
        PortSpec("seconds", Int::class),
        PortSpec("minutes", Int::class)
    )

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createIn2Out2Processor<Int, Int, Int, Int>(
            name = name,
            process = { elapsedSeconds, elapsedMinutes ->
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
        )
    }
}
