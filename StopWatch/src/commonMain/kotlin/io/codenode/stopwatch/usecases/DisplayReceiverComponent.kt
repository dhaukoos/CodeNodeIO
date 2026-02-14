/*
 * DisplayReceiverComponent - UseCase for DisplayReceiver CodeNode
 * Receives timer values and exposes them as observable state for UI rendering
 * License: Apache 2.0
 */

package io.codenode.stopwatch.usecases

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.InformationPacket
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.ProcessingLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * DisplayReceiver UseCase - Sink node that receives time values for UI rendering.
 *
 * This component receives elapsed time from TimerEmitter and exposes it as
 * observable StateFlows for the UI layer (StopWatchFace composable).
 *
 * Type: SINK (2 inputs: seconds, minutes; 0 outputs)
 */
class DisplayReceiverComponent : ProcessingLogic {

    /**
     * CodeNode reference for lifecycle delegation.
     * Job management is delegated to this node's nodeControlJob.
     */
    var codeNode: CodeNode? = CodeNode(
        id = "display-receiver",
        name = "DisplayReceiver",
        codeNodeType = CodeNodeType.SINK,
        position = Node.Position(0.0, 0.0)
    )

    /**
     * Input channel for FBP point-to-point semantics with backpressure.
     * Assigned by flow wiring before start() is called.
     * Uses typed ReceiveChannel<TimerOutput> for type safety.
     */
    var inputChannel: ReceiveChannel<TimerOutput>? = null

    // Observable state flows for displayed time
    private val _displayedSeconds = MutableStateFlow(0)
    val displayedSecondsFlow: StateFlow<Int> = _displayedSeconds.asStateFlow()

    private val _displayedMinutes = MutableStateFlow(0)
    val displayedMinutesFlow: StateFlow<Int> = _displayedMinutes.asStateFlow()

    /**
     * ProcessingLogic implementation - processes incoming time values.
     */
    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        // Extract seconds and minutes from inputs
        inputs["seconds"]?.let { packet ->
            @Suppress("UNCHECKED_CAST")
            (packet as? InformationPacket<Int>)?.payload?.let { receiveSeconds(it) }
        }
        inputs["minutes"]?.let { packet ->
            @Suppress("UNCHECKED_CAST")
            (packet as? InformationPacket<Int>)?.payload?.let { receiveMinutes(it) }
        }

        // Sink has no outputs
        return emptyMap()
    }

    /**
     * Starts collecting from the input channel using for-loop iteration.
     * Delegates job management to CodeNode.start().
     * The for-loop automatically handles channel closure gracefully.
     *
     * @param scope CoroutineScope to run collection in
     */
    suspend fun start(scope: CoroutineScope) {
        val node = codeNode ?: return
        val channel = inputChannel ?: return

        // Delegate job management to CodeNode
        node.start(scope) {
            try {
                for (timerOutput in channel) {
                    receiveSeconds(timerOutput.elapsedSeconds)
                    receiveMinutes(timerOutput.elapsedMinutes)
                }
                // For-loop exits normally when channel is closed
            } catch (e: ClosedReceiveChannelException) {
                // Channel closed unexpectedly - graceful shutdown
            }
        }
    }

    /**
     * Stops collecting from input channel.
     * Delegates job cancellation to CodeNode.stop().
     * Channel closure is handled by the flow orchestrator.
     */
    fun stop() {
        codeNode?.stop()
    }

    /**
     * Receives a seconds value from the timer.
     *
     * @param seconds The elapsed seconds value
     */
    fun receiveSeconds(seconds: Int) {
        _displayedSeconds.value = seconds
    }

    /**
     * Receives a minutes value from the timer.
     *
     * @param minutes The elapsed minutes value
     */
    fun receiveMinutes(minutes: Int) {
        _displayedMinutes.value = minutes
    }
}
