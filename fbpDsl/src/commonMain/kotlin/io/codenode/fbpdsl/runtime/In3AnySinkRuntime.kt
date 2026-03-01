/*
 * In3AnySinkRuntime - Runtime for any-input sink nodes with 3 inputs
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
 * Specialized NodeRuntime for any-input sink nodes with 3 inputs.
 *
 * @param A Type of first input
 * @param B Type of second input
 * @param C Type of third input
 */
class In3AnySinkRuntime<A : Any, B : Any, C : Any>(
    codeNode: CodeNode,
    private val initialValue1: A,
    private val initialValue2: B,
    private val initialValue3: C,
    private val consume: In3AnySinkBlock<A, B, C>
) : NodeRuntime(codeNode) {

    var inputChannel1: ReceiveChannel<A>? = null
    var inputChannel2: ReceiveChannel<B>? = null
    var inputChannel3: ReceiveChannel<C>? = null

    private var lastValue1: A = initialValue1
    private var lastValue2: B = initialValue2
    private var lastValue3: C = initialValue3

    fun resetCachedValues() {
        lastValue1 = initialValue1
        lastValue2 = initialValue2
        lastValue3 = initialValue3
    }

    override fun start(scope: CoroutineScope, processingBlock: suspend () -> Unit) {
        nodeControlJob?.cancel()
        executionState = ExecutionState.RUNNING
        registry?.register(this)

        nodeControlJob = scope.launch {
            try {
                val inChannel1 = inputChannel1 ?: return@launch
                val inChannel2 = inputChannel2 ?: return@launch
                val inChannel3 = inputChannel3 ?: return@launch

                while (executionState != ExecutionState.IDLE) {
                    while (executionState == ExecutionState.PAUSED) {
                        delay(10)
                    }
                    if (executionState == ExecutionState.IDLE) break

                    select<Unit> {
                        inChannel1.onReceive { value ->
                            lastValue1 = value
                            consume(lastValue1, lastValue2, lastValue3)
                        }
                        inChannel2.onReceive { value ->
                            lastValue2 = value
                            consume(lastValue1, lastValue2, lastValue3)
                        }
                        inChannel3.onReceive { value ->
                            lastValue3 = value
                            consume(lastValue1, lastValue2, lastValue3)
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
