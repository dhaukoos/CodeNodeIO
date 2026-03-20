/*
 * TimerEmitterCodeNode - Self-contained source node for timer emission
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
import kotlinx.coroutines.flow.combine

/**
 * Source node that emits elapsed seconds and minutes from StopWatchState.
 *
 * Combines the _elapsedSeconds and _elapsedMinutes StateFlows and emits
 * both values as a ProcessResult2 whenever either changes. The initial
 * combine emission (0, 0) kick-starts the feedback loop — TimeIncrementer
 * processes it and updates state, which triggers the next emission.
 *
 * Note: The generated StopWatchFlow used .drop(1) here because the
 * generated controller manually injected initial values into the output
 * channels. The self-contained CodeNode omits .drop(1) so the combine
 * flow's initial emission serves as the kick-start instead.
 */
object TimerEmitterCodeNode : CodeNodeDefinition {
    override val name = "TimerEmitter"
    override val category = CodeNodeType.SOURCE
    override val description = "Emits elapsed seconds and minutes from timer state"
    override val inputPorts = emptyList<PortSpec>()
    override val outputPorts = listOf(
        PortSpec("elapsedSeconds", Int::class),
        PortSpec("elapsedMinutes", Int::class)
    )

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createSourceOut2<Int, Int>(
            name = name,
            generate = { emit ->
                combine(
                    StopWatchState._elapsedSeconds,
                    StopWatchState._elapsedMinutes
                ) { elapsedSeconds, elapsedMinutes ->
                    ProcessResult2.both(elapsedSeconds, elapsedMinutes)
                }.collect { result ->
                    emit(result)
                }
            }
        )
    }
}
