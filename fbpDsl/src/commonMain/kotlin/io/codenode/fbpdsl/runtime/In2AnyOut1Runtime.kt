/*
 * In2AnyOut1Runtime - Runtime for any-input nodes with 2 inputs and 1 output
 * Fires process block when ANY input receives data
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.ExecutionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

/**
 * Specialized NodeRuntime for any-input nodes with 2 inputs and 1 output.
 *
 * Uses `select` expression to fire the process block as soon as ANY input
 * channel delivers data, providing the most recent cached value for the
 * other input(s).
 *
 * @param A Type of first input
 * @param B Type of second input
 * @param R Type of output
 * @param codeNode The underlying CodeNode model
 * @param initialValue1 Initial/default value for input 1
 * @param initialValue2 Initial/default value for input 2
 * @param process The processing function that combines both inputs
 */
class In2AnyOut1Runtime<A : Any, B : Any, R : Any>(
    codeNode: CodeNode,
    private val initialValue1: A,
    private val initialValue2: B,
    private val process: In2AnyOut1ProcessBlock<A, B, R>
) : NodeRuntime(codeNode) {

    var inputChannel1: ReceiveChannel<A>? = null
    var inputChannel2: ReceiveChannel<B>? = null
    var outputChannel: SendChannel<R>? = null

    private var lastValue1: A = initialValue1
    private var lastValue2: B = initialValue2

    /**
     * Resets cached values to their initial defaults.
     */
    fun resetCachedValues() {
        lastValue1 = initialValue1
        lastValue2 = initialValue2
    }

    override fun start(scope: CoroutineScope, processingBlock: suspend () -> Unit) {
        nodeControlJob?.cancel()
        executionState = ExecutionState.RUNNING
        registry?.register(this)

        nodeControlJob = scope.launch {
            try {
                val inChannel1 = inputChannel1 ?: return@launch
                val inChannel2 = inputChannel2 ?: return@launch
                val outChannel = outputChannel ?: return@launch

                while (executionState != ExecutionState.IDLE) {
                    while (executionState == ExecutionState.PAUSED) {
                        delay(10)
                    }
                    if (executionState == ExecutionState.IDLE) break

                    select<Unit> {
                        inChannel1.onReceive { value ->
                            lastValue1 = value
                            val delayMs = attenuationDelayMs
                            if (delayMs != null && delayMs > 0) delay(delayMs)
                            val result = process(lastValue1, lastValue2)
                            outChannel.send(result)
                            onEmit?.invoke(codeNode.name, 0)
                            onEmitValue?.invoke(codeNode.name, 0, result)
                        }
                        inChannel2.onReceive { value ->
                            lastValue2 = value
                            val delayMs = attenuationDelayMs
                            if (delayMs != null && delayMs > 0) delay(delayMs)
                            val result = process(lastValue1, lastValue2)
                            outChannel.send(result)
                            onEmit?.invoke(codeNode.name, 0)
                            onEmitValue?.invoke(codeNode.name, 0, result)
                        }
                    }
                }
            } catch (_: ClosedReceiveChannelException) {
            } catch (_: ClosedSendChannelException) {
            } finally {
                executionState = ExecutionState.IDLE
                outputChannel?.close()
            }
        }
    }
}
