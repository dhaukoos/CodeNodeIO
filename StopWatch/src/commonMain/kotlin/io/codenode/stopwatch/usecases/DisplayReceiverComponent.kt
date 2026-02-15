/*
 * DisplayReceiverComponent - UseCase for DisplayReceiver CodeNode
 * Receives timer values and exposes them as observable state for UI rendering
 * License: Apache 2.0
 */

package io.codenode.stopwatch.usecases

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.model.InformationPacket
import io.codenode.fbpdsl.model.ProcessingLogic
import io.codenode.fbpdsl.runtime.SinkRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * DisplayReceiver UseCase - Sink node that receives time values for UI rendering.
 *
 * This component uses CodeNodeFactory.createContinuousSink to create a
 * SinkRuntime that manages the receive loop lifecycle.
 *
 * Features:
 * - Receives elapsed time from TimerEmitter via channel
 * - Exposes StateFlows for UI observation
 * - Handles channel closure gracefully
 *
 * Type: SINK (1 input: TimerOutput, 0 outputs)
 */
class DisplayReceiverComponent : ProcessingLogic {

    // Observable state flows for displayed time - declared first for closure capture
    private val _displayedSeconds = MutableStateFlow(0)
    val displayedSecondsFlow: StateFlow<Int> = _displayedSeconds.asStateFlow()

    private val _displayedMinutes = MutableStateFlow(0)
    val displayedMinutesFlow: StateFlow<Int> = _displayedMinutes.asStateFlow()

    /**
     * SinkRuntime created via factory method.
     * Manages the receive loop lifecycle with proper channel handling.
     */
    private val sinkRuntime: SinkRuntime<TimerOutput> = CodeNodeFactory.createContinuousSink(
        name = "DisplayReceiver",
        description = "Receives timer values and exposes them for UI rendering"
    ) { timerOutput ->
        // Update state flows for UI observation
        _displayedSeconds.value = timerOutput.elapsedSeconds
        _displayedMinutes.value = timerOutput.elapsedMinutes
    }

    /**
     * CodeNode reference - delegates to sinkRuntime.
     */
    val codeNode: CodeNode
        get() = sinkRuntime.codeNode

    /**
     * Input channel for FBP point-to-point semantics with backpressure.
     * Must be assigned before start() is called.
     */
    var inputChannel: ReceiveChannel<TimerOutput>?
        get() = sinkRuntime.inputChannel
        set(value) {
            sinkRuntime.inputChannel = value
        }

    /**
     * ProcessingLogic implementation - processes incoming time values.
     * For continuous operation, use start() instead.
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
     * Starts collecting from the input channel.
     * Delegates to SinkRuntime.start().
     *
     * @param scope CoroutineScope to run collection in
     */
    suspend fun start(scope: CoroutineScope) {
        sinkRuntime.start(scope) {
            // Processing block is ignored - sink uses its own block
        }
    }

    /**
     * Stops collecting from input channel.
     * Delegates to SinkRuntime.stop().
     */
    fun stop() {
        sinkRuntime.stop()
    }

    /**
     * Receives a seconds value from the timer.
     * Used by ProcessingLogic interface for single-invocation mode.
     *
     * @param seconds The elapsed seconds value
     */
    fun receiveSeconds(seconds: Int) {
        _displayedSeconds.value = seconds
    }

    /**
     * Receives a minutes value from the timer.
     * Used by ProcessingLogic interface for single-invocation mode.
     *
     * @param minutes The elapsed minutes value
     */
    fun receiveMinutes(minutes: Int) {
        _displayedMinutes.value = minutes
    }
}
