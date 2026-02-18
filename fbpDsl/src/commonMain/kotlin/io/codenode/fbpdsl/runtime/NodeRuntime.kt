/*
 * NodeRuntime - Runtime execution wrapper for CodeNode
 * Owns all runtime state including lifecycle control and channel wiring
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.ExecutionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

/**
 * Runtime execution wrapper for a CodeNode.
 *
 * Owns all runtime state including:
 * - Execution state (IDLE, RUNNING, PAUSED, ERROR)
 * - Coroutine job for lifecycle control
 * - Input/output channels for continuous processing
 * - Optional registry for centralized lifecycle management
 *
 * This separation keeps CodeNode as a pure serializable model.
 *
 * @param T The primary data type flowing through this node
 * @property codeNode The underlying CodeNode model (immutable definition)
 * @property registry Optional RuntimeRegistry for automatic registration on start/stop
 */
open class NodeRuntime<T : Any>(
    val codeNode: CodeNode,
    var registry: RuntimeRegistry? = null
) {
    /**
     * Current execution state of this node.
     * Mutable at runtime, not persisted with CodeNode.
     */
    var executionState: ExecutionState = ExecutionState.IDLE

    /**
     * Runtime job reference for lifecycle control.
     * Tracks the active coroutine when the node is running.
     */
    var nodeControlJob: Job? = null

    /**
     * Input channel for receiving data (sink/transformer nodes).
     */
    var inputChannel: ReceiveChannel<T>? = null

    /**
     * Output channel for emitting data (generator/transformer nodes).
     */
    var outputChannel: SendChannel<T>? = null

    /**
     * Starts the node's processing loop.
     *
     * Manages the nodeControlJob lifecycle:
     * 1. Cancels any existing job (prevents duplicate jobs)
     * 2. Registers with RuntimeRegistry (if provided)
     * 3. Sets executionState to RUNNING
     * 4. Launches new job in provided scope
     * 5. Executes processingBlock within the job
     *
     * @param scope CoroutineScope to launch the processing job in
     * @param processingBlock Custom processing logic to execute in the job loop
     */
    open fun start(scope: CoroutineScope, processingBlock: suspend () -> Unit) {
        // Cancel existing job if running (prevents duplicate jobs)
        nodeControlJob?.cancel()

        // Register with registry for centralized lifecycle control
        registry?.register(this)

        // Transition to RUNNING state
        executionState = ExecutionState.RUNNING

        // Launch the processing job
        nodeControlJob = scope.launch {
            try {
                processingBlock()
            } finally {
                // Close output channel when loop exits
                outputChannel?.close()
            }
        }
    }

    /**
     * Stops the node's processing loop.
     *
     * Manages graceful shutdown:
     * 1. Unregisters from RuntimeRegistry (if provided)
     * 2. Sets executionState to IDLE
     * 3. Cancels the nodeControlJob
     * 4. Sets nodeControlJob to null
     */
    fun stop() {
        // Unregister from registry before stopping
        registry?.unregister(this)

        executionState = ExecutionState.IDLE
        nodeControlJob?.cancel()
        nodeControlJob = null
    }

    /**
     * Pauses the node's processing loop.
     *
     * Sets executionState to PAUSED. The processing loop must check
     * isPaused() to honor pause requests.
     *
     * Only valid when state is RUNNING.
     */
    fun pause() {
        if (executionState != ExecutionState.RUNNING) return
        executionState = ExecutionState.PAUSED
    }

    /**
     * Resumes the node's processing loop from paused state.
     *
     * Sets executionState back to RUNNING.
     *
     * Only valid when state is PAUSED.
     */
    fun resume() {
        if (executionState != ExecutionState.PAUSED) return
        executionState = ExecutionState.RUNNING
    }

    /**
     * Checks if this node is currently executing.
     */
    fun isRunning(): Boolean = executionState == ExecutionState.RUNNING

    /**
     * Checks if this node is paused.
     */
    fun isPaused(): Boolean = executionState == ExecutionState.PAUSED

    /**
     * Checks if this node is idle (not processing).
     */
    fun isIdle(): Boolean = executionState == ExecutionState.IDLE
}
