/*
 * TimerEmitterComponent - UseCase for TimerEmitter CodeNode
 * Implements the timer tick logic for the StopWatch virtual circuit
 * License: Apache 2.0
 */

package io.codenode.stopwatch.usecases

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.InformationPacket
import io.codenode.fbpdsl.model.InformationPacketFactory
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.ProcessingLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive

/**
 * Data class for timer output values
 */
data class TimerOutput(
    val elapsedSeconds: Int,
    val elapsedMinutes: Int
)

/**
 * TimerEmitter UseCase - Generator node that emits elapsed time at regular intervals.
 *
 * This component implements the timer logic from the original StopWatch LaunchedEffect:
 * - Emits elapsedSeconds every speedAttenuation milliseconds
 * - Rolls seconds to 0 and increments minutes at 60
 * - Only emits when executionState == RUNNING
 *
 * Type: GENERATOR (0 inputs, 2 outputs: elapsedSeconds, elapsedMinutes)
 *
 * @param speedAttenuation Tick interval in milliseconds (default: 1000ms = 1 second)
 * @param initialSeconds Initial seconds value (for testing)
 * @param initialMinutes Initial minutes value (for testing)
 */
class TimerEmitterComponent(
    private val speedAttenuation: Long = 1000L,
    initialSeconds: Int = 0,
    initialMinutes: Int = 0
) : ProcessingLogic {

    /**
     * CodeNode reference for lifecycle delegation.
     * Job management is delegated to this node's nodeControlJob.
     */
    var codeNode: CodeNode? = CodeNode(
        id = "timer-emitter",
        name = "TimerEmitter",
        codeNodeType = CodeNodeType.GENERATOR,
        position = Node.Position(0.0, 0.0)
    )

    /**
     * Output channel for FBP point-to-point semantics with backpressure.
     * Assigned by flow wiring before start() is called.
     * Uses typed SendChannel<TimerOutput> for type safety.
     */
    var outputChannel: SendChannel<TimerOutput>? = null

    // Observable state flows for elapsed time
    private val _elapsedSeconds = MutableStateFlow(initialSeconds)
    val elapsedSecondsFlow: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _elapsedMinutes = MutableStateFlow(initialMinutes)
    val elapsedMinutesFlow: StateFlow<Int> = _elapsedMinutes.asStateFlow()

    /**
     * Execution state - delegated to CodeNode.
     * Getter returns codeNode's executionState; setter updates codeNode via copy.
     */
    var executionState: ExecutionState
        get() = codeNode?.executionState ?: ExecutionState.IDLE
        set(value) {
            codeNode = codeNode?.withExecutionState(value)
        }

    /**
     * ProcessingLogic implementation - generates timer output.
     * For continuous operation, use start() instead.
     */
    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        // Single invocation returns current state
        return mapOf(
            "elapsedSeconds" to InformationPacketFactory.create(_elapsedSeconds.value),
            "elapsedMinutes" to InformationPacketFactory.create(_elapsedMinutes.value)
        )
    }

    /**
     * Starts the continuous timer tick loop.
     * Delegates job management to CodeNode.start().
     *
     * @param scope CoroutineScope to run the timer in
     */
    suspend fun start(scope: CoroutineScope) {
        val node = codeNode ?: return

        // Delegate job management to CodeNode
        node.start(scope) {
            // Check executionState in while loop condition
            while (currentCoroutineContext().isActive && executionState == ExecutionState.RUNNING) {
                // delay for speedAttenuation interval
                delay(speedAttenuation)

                // Check state again after delay (may have changed during delay)
                if (executionState != ExecutionState.RUNNING) break

                // Increment seconds with rollover logic
                var newSeconds = _elapsedSeconds.value + 1
                var newMinutes = _elapsedMinutes.value

                if (newSeconds >= 60) {
                    newSeconds = 0
                    newMinutes += 1
                }

                // Update state flows
                _elapsedSeconds.value = newSeconds
                _elapsedMinutes.value = newMinutes

                // Send to output channel for downstream connections
                val timerOutput = TimerOutput(newSeconds, newMinutes)
                try {
                    outputChannel?.send(timerOutput)
                } catch (e: ClosedSendChannelException) {
                    // Channel closed - graceful shutdown, exit loop
                    break
                }
            }
        }
    }

    /**
     * Stops the timer.
     * Delegates job cancellation to CodeNode.stop().
     */
    fun stop() {
        executionState = ExecutionState.IDLE
        codeNode?.stop()
    }

    /**
     * Resets the timer to zero.
     */
    fun reset() {
        stop()
        _elapsedSeconds.value = 0
        _elapsedMinutes.value = 0
    }
}
