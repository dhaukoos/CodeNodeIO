/*
 * DataFlowDebugger - Per-connection transit snapshot storage
 * Captures the most recent data value on each connection for runtime debugging
 * License: Apache 2.0
 */

package io.codenode.circuitsimulator

import io.codenode.fbpdsl.model.FlowGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stores per-connection transit snapshots for runtime data inspection.
 *
 * When debug mode is enabled, each channel emission captures the most recent
 * value that passed through each connection. Values are keyed by connection ID
 * and exposed as StateFlows for UI observation.
 *
 * Lifecycle:
 * - Created/initialized when runtime starts with animation enabled
 * - Snapshots retained when paused (for inspection)
 * - Cleared on stop or when animation is disabled
 */
class DataFlowDebugger {

    private val _snapshots = mutableMapOf<String, MutableStateFlow<Any?>>()

    /**
     * Creates a value emission observer callback that maps emissions to connection snapshots.
     *
     * Uses the same nodePort-to-connection mapping pattern as DataFlowAnimationController.
     *
     * @param flowGraph The FlowGraph containing nodes and connections
     * @return A callback (nodeName, portIndex, value) -> Unit
     */
    fun createValueObserver(flowGraph: FlowGraph): (String, Int, Any?) -> Unit {
        val nodePortToConnections = buildPortConnectionMap(flowGraph)

        return { nodeName: String, portIndex: Int, value: Any? ->
            val key = "$nodeName:$portIndex"
            val connectionIds = nodePortToConnections[key] ?: emptyList()
            for (connId in connectionIds) {
                val flow = _snapshots.getOrPut(connId) { MutableStateFlow(null) }
                flow.value = value
            }
        }
    }

    /**
     * Returns the snapshot StateFlow for a given connection ID.
     * Returns null if no snapshot exists for this connection.
     */
    fun getSnapshot(connectionId: String): StateFlow<Any?>? {
        return _snapshots[connectionId]?.asStateFlow()
    }

    /**
     * Returns the current snapshot value for a given connection ID.
     * Returns null if no snapshot exists or no value has been captured.
     */
    fun getSnapshotValue(connectionId: String): Any? {
        return _snapshots[connectionId]?.value
    }

    /**
     * Clears all captured snapshots.
     */
    fun clear() {
        _snapshots.values.forEach { it.value = null }
        _snapshots.clear()
    }

    /**
     * Builds a lookup map from "nodeName:portIndex" to list of connection IDs.
     * Same logic as DataFlowAnimationController.buildPortConnectionMap().
     */
    private fun buildPortConnectionMap(flowGraph: FlowGraph): Map<String, List<String>> {
        val map = mutableMapOf<String, MutableList<String>>()

        for (node in flowGraph.getAllNodes()) {
            val outputPorts = node.outputPorts
            for ((index, port) in outputPorts.withIndex()) {
                val key = "${node.name}:$index"
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
}
