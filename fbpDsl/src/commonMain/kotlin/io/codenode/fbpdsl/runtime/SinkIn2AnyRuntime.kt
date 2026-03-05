/*
 * SinkIn2AnyRuntime - Runtime for any-input sink nodes with 2 inputs
 * Fires consume block when ANY input receives data
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.ExecutionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

/**
 * Specialized NodeRuntime for any-input sink nodes with 2 inputs.
 *
 * Uses `select` expression to fire the consume block as soon as ANY input
 * channel delivers data, providing the most recent cached value for the
 * other input(s).
 *
 * @param A Type of first input
 * @param B Type of second input
 * @param codeNode The underlying CodeNode model
 * @param initialValue1 Initial/default value for input 1
 * @param initialValue2 Initial/default value for input 2
 * @param consume The sink function that processes both inputs
 */
class SinkIn2AnyRuntime<A : Any, B : Any>(
    codeNode: CodeNode,
    private val initialValue1: A,
    private val initialValue2: B,
    private val consume: SinkIn2AnyBlock<A, B>
) : NodeRuntime(codeNode) {

    var inputChannel1: ReceiveChannel<A>? = null
    var inputChannel2: ReceiveChannel<B>? = null

    private var lastValue1: A = initialValue1
    private var lastValue2: B = initialValue2

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

                while (executionState != ExecutionState.IDLE) {
                    while (executionState == ExecutionState.PAUSED) {
                        delay(10)
                    }
                    if (executionState == ExecutionState.IDLE) break

                    select<Unit> {
                        inChannel1.onReceive { value ->
                            lastValue1 = value
                            consume(lastValue1, lastValue2)
                        }
                        inChannel2.onReceive { value ->
                            lastValue2 = value
                            consume(lastValue1, lastValue2)
                        }
                    }
                }
            } catch (_: ClosedReceiveChannelException) {
            } finally {
                executionState = ExecutionState.IDLE
            }
        }
    }
}
