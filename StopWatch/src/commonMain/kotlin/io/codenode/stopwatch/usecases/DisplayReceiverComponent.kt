/*
 * DisplayReceiverComponent - UseCase for DisplayReceiver CodeNode
 * Receives timer values and exposes them as observable state for UI rendering
 * License: Apache 2.0
 */

package io.codenode.stopwatch.usecases

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.InformationPacket
import io.codenode.fbpdsl.model.ProcessingLogic
import io.codenode.fbpdsl.runtime.In2SinkBlock
import io.codenode.fbpdsl.runtime.In2SinkRuntime
import io.codenode.fbpdsl.runtime.RuntimeRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive

/**
 * DisplayReceiver UseCase - Sink node that receives time values for UI rendering.
 *
 * This component uses CodeNodeFactory.createIn2Sink to create an
 * In2SinkRuntime that manages the receive loop lifecycle with two typed input channels.
 *
 * Features:
 * - Receives elapsed seconds from inputChannel (first input)
 * - Receives elapsed minutes from inputChannel2 (second input)
 * - Exposes StateFlows for UI observation
 * - Handles channel closure gracefully
 *
 * Type: SINK (2 inputs: Int for seconds, Int for minutes, 0 outputs)
 */
class DisplayReceiverComponent (
    private val speedAttenuation: Long = 1000L,
) : ProcessingLogic {

    // Observable state flows for displayed time - declared first for closure capture
    private val _displayedSeconds = MutableStateFlow(0)
    val displayedSecondsFlow: StateFlow<Int> = _displayedSeconds.asStateFlow()

    private val _displayedMinutes = MutableStateFlow(0)
    val displayedMinutesFlow: StateFlow<Int> = _displayedMinutes.asStateFlow()



    val consumer : In2SinkBlock<Int, Int> = { seconds, minutes ->
        // Update state flows for UI observation
        _displayedSeconds.value = seconds
        _displayedMinutes.value = minutes

//        // Continuous timer loop - runs until stopped
//        while (currentCoroutineContext().isActive && executionState == ExecutionState.RUNNING) {
//            // Delay for tick interval
//            if (speedAttenuation > 0) {
//                delay(speedAttenuation)
//                // Check state again after delay (may have changed during delay)
//                if (executionState != ExecutionState.RUNNING) break
//            }
//
//            // Update state flows for UI observation
//            _displayedSeconds.value = seconds
//            _displayedMinutes.value = minutes
//
//        }
    }

    /**
     * In2SinkRuntime created via factory method.
     * Manages the receive loop lifecycle with proper dual-channel handling.
     * inputChannel: seconds (Int), inputChannel2: minutes (Int)
     */
    private val sinkRuntime: In2SinkRuntime<Int, Int> = CodeNodeFactory.createIn2Sink(
        name = "DisplayReceiver",
        description = "Receives timer values on two typed channels and exposes them for UI rendering",
        consume = consumer
    )
//    { seconds, minutes ->
//        // Update state flows for UI observation
//        _displayedSeconds.value = seconds
//        _displayedMinutes.value = minutes
//    }


    /**
     * Execution state - delegated to sinkRuntime.
     */
    var executionState: ExecutionState
        get() = sinkRuntime.executionState
        set(value) {
            sinkRuntime.executionState = value
        }

    /**
     * RuntimeRegistry for centralized lifecycle control.
     * Delegated to underlying sinkRuntime.
     */
    var registry: RuntimeRegistry?
        get() = sinkRuntime.registry
        set(value) {
            sinkRuntime.registry = value
        }

    /**
     * CodeNode reference - delegates to sinkRuntime.
     */
    val codeNode: CodeNode
        get() = sinkRuntime.codeNode

    /**
     * First input channel (seconds) for FBP point-to-point semantics with backpressure.
     * Must be assigned before start() is called.
     */
    var inputChannel: ReceiveChannel<Int>?
        get() = sinkRuntime.inputChannel
        set(value) {
            sinkRuntime.inputChannel = value
        }

    /**
     * Second input channel (minutes) for FBP point-to-point semantics with backpressure.
     * Must be assigned before start() is called.
     */
    var inputChannel2: ReceiveChannel<Int>?
        get() = sinkRuntime.inputChannel2
        set(value) {
            sinkRuntime.inputChannel2 = value
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
