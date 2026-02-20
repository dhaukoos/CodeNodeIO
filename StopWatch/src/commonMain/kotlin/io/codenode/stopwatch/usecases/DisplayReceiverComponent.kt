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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
class DisplayReceiverComponent : ProcessingLogic {

    // Observable state flows for displayed time
    private val _seconds = MutableStateFlow(0)
    val secondsFlow: StateFlow<Int> = _seconds.asStateFlow()

    private val _minutes = MutableStateFlow(0)
    val minutesFlow: StateFlow<Int> = _minutes.asStateFlow()

    /**
     * Consumer function - business logic for processing received timer values.
     * Updates StateFlows for UI observation.
     */
    val consumer: In2SinkBlock<Int, Int> = { seconds, minutes ->
        _seconds.value = seconds
        _minutes.value = minutes
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

    val codeNode: CodeNode
        get() = sinkRuntime.codeNode

    var inputChannel: ReceiveChannel<Int>?
        get() = sinkRuntime.inputChannel1
        set(value) {
            sinkRuntime.inputChannel1 = value
        }

    var inputChannel2: ReceiveChannel<Int>?
        get() = sinkRuntime.inputChannel2
        set(value) {
            sinkRuntime.inputChannel2 = value
        }

    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        inputs["seconds"]?.let { packet ->
            @Suppress("UNCHECKED_CAST")
            (packet as? InformationPacket<Int>)?.payload?.let { receiveSeconds(it) }
        }
        inputs["minutes"]?.let { packet ->
            @Suppress("UNCHECKED_CAST")
            (packet as? InformationPacket<Int>)?.payload?.let { receiveMinutes(it) }
        }
        return emptyMap()
    }

    suspend fun start(scope: CoroutineScope) {
        sinkRuntime.start(scope) {}
    }

    fun stop() {
        sinkRuntime.stop()
    }

    fun reset() {
        _seconds.value = 0
        _minutes.value = 0
    }

    fun receiveSeconds(seconds: Int) {
        _seconds.value = seconds
    }

    fun receiveMinutes(minutes: Int) {
        _minutes.value = minutes
    }
}
