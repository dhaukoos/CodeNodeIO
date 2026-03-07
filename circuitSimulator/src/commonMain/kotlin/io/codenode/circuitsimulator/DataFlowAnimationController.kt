/*
 * DataFlowAnimationController - Manages animated dots on connections
 * License: Apache 2.0
 */

package io.codenode.circuitsimulator

import io.codenode.fbpdsl.model.FlowGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages animated dots traveling along connection curves during runtime execution.
 *
 * Responsibilities:
 * - Creates emission observer callbacks that map (nodeId, portIndex) to connections
 * - Maintains a list of active animations as a StateFlow for UI observation
 * - Runs a frame loop to prune completed animations
 * - Supports pause/resume by adjusting animation timestamps
 */
class DataFlowAnimationController {

    private val _activeAnimations = MutableStateFlow<List<ConnectionAnimation>>(emptyList())
    /** Current active animations for UI rendering */
    val activeAnimations: StateFlow<List<ConnectionAnimation>> = _activeAnimations.asStateFlow()

    private var frameLoopJob: Job? = null
    private var pauseTimeMs: Long? = null

    /**
     * Creates an emission observer callback that maps IP emissions to connection animations.
     *
     * The returned callback is passed to ModuleController.setEmissionObserver().
     * When invoked with (nodeId, portIndex), it:
     * 1. Finds the node's output port at the given index
     * 2. Finds all connections originating from that port
     * 3. Creates a ConnectionAnimation for each connection
     *
     * @param flowGraph The FlowGraph containing nodes and connections
     * @param attenuationMs Lambda returning the current attenuation delay in ms
     * @return A callback suitable for ModuleController.setEmissionObserver()
     */
    fun createEmissionObserver(
        flowGraph: FlowGraph,
        attenuationMs: () -> Long
    ): (String, Int) -> Unit {
        // Pre-compute: for each (nodeId, portIndex) -> list of connection IDs
        val nodePortToConnections = buildPortConnectionMap(flowGraph)

        return { nodeId: String, portIndex: Int ->
            val key = "$nodeId:$portIndex"
            val connectionIds = nodePortToConnections[key] ?: emptyList()

            if (connectionIds.isNotEmpty()) {
                val now = currentTimeMs()
                val duration = (attenuationMs() * 0.8).toLong()
                if (duration > 0) {
                    val newAnimations = connectionIds.map { connId ->
                        ConnectionAnimation(
                            connectionId = connId,
                            startTimeMs = now,
                            durationMs = duration
                        )
                    }
                    _activeAnimations.value = _activeAnimations.value + newAnimations
                }
            }
        }
    }

    /**
     * Starts the frame loop that prunes completed animations.
     * Runs approximately every 16ms (~60fps).
     *
     * @param scope CoroutineScope to launch the frame loop in
     */
    fun startFrameLoop(scope: CoroutineScope) {
        frameLoopJob?.cancel()
        frameLoopJob = scope.launch {
            while (isActive) {
                delay(16)
                val now = currentTimeMs()
                val current = _activeAnimations.value
                if (current.isNotEmpty()) {
                    val active = current.filter { !it.isComplete(now) }
                    if (active.size != current.size) {
                        _activeAnimations.value = active
                    }
                }
            }
        }
    }

    /**
     * Stops the frame loop.
     */
    fun stopFrameLoop() {
        frameLoopJob?.cancel()
        frameLoopJob = null
    }

    /**
     * Pauses all active animations by recording the pause timestamp.
     * Animation progress is frozen until [resume] is called.
     */
    fun pause() {
        pauseTimeMs = currentTimeMs()
    }

    /**
     * Resumes all active animations by adjusting their start times
     * to account for the paused duration.
     */
    fun resume() {
        val pausedAt = pauseTimeMs ?: return
        val pausedDuration = currentTimeMs() - pausedAt
        pauseTimeMs = null

        val adjusted = _activeAnimations.value.map { anim ->
            anim.copy(startTimeMs = anim.startTimeMs + pausedDuration)
        }
        _activeAnimations.value = adjusted
    }

    /**
     * Immediately removes all active animations.
     */
    fun clear() {
        _activeAnimations.value = emptyList()
    }

    /**
     * Builds a lookup map from "nodeId:portIndex" to list of connection IDs.
     *
     * For each node in the FlowGraph, maps each output port (by index)
     * to all connections that originate from that port.
     */
    private fun buildPortConnectionMap(flowGraph: FlowGraph): Map<String, List<String>> {
        val map = mutableMapOf<String, MutableList<String>>()

        for (node in flowGraph.getAllNodes()) {
            val outputPorts = node.outputPorts
            for ((index, port) in outputPorts.withIndex()) {
                val key = "${node.id}:$index"
                val connections = flowGraph.connections.filter { conn ->
                    conn.sourceNodeId == node.id && conn.sourcePortId == port.id
                }
                if (connections.isNotEmpty()) {
                    map[key] = connections.map { it.id }.toMutableList()
                }
            }
        }

        return map
    }

    /**
     * Returns the current system time in milliseconds.
     * Extracted for testability.
     */
    internal fun currentTimeMs(): Long = System.currentTimeMillis()
}
