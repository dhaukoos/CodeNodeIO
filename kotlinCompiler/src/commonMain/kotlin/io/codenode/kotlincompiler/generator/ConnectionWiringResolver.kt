/*
 * ConnectionWiringResolver
 * Resolves FlowGraph connections to channel property assignments
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.FlowGraph

/**
 * Represents a single channel wiring statement between two runtime instances.
 *
 * @property targetVarName camelCase variable name of the target runtime (e.g., "displayReceiver")
 * @property targetChannelProp Channel property on target (e.g., "inputChannel1")
 * @property sourceVarName camelCase variable name of the source runtime (e.g., "timerEmitter")
 * @property sourceChannelProp Channel property on source (e.g., "outputChannel1")
 */
data class WiringStatement(
    val targetVarName: String,
    val targetChannelProp: String,
    val sourceVarName: String,
    val sourceChannelProp: String
)

/**
 * Resolves FlowGraph connections to channel property assignments.
 *
 * Maps each Connection to a WiringStatement that assigns a source output
 * channel to a target input channel on the runtime instances. Channel
 * property names follow the runtime convention:
 * - 1 input: `inputChannel`; 2+: `inputChannel1`, `inputChannel2`, `inputChannel3`
 * - 1 output: `outputChannel`; 2+: `outputChannel1`, `outputChannel2`, `outputChannel3`
 */
class ConnectionWiringResolver {

    /**
     * Resolves all connections in a FlowGraph to wiring statements.
     *
     * @param flowGraph The FlowGraph containing connections and nodes
     * @return List of wiring statements for channel assignments
     */
    fun getWiringStatements(flowGraph: FlowGraph): List<WiringStatement> {
        return flowGraph.connections.mapNotNull { connection ->
            val sourceNode = flowGraph.findNode(connection.sourceNodeId) as? CodeNode ?: return@mapNotNull null
            val targetNode = flowGraph.findNode(connection.targetNodeId) as? CodeNode ?: return@mapNotNull null

            val sourcePortIndex = sourceNode.outputPorts.indexOfFirst { it.id == connection.sourcePortId }
            val targetPortIndex = targetNode.inputPorts.indexOfFirst { it.id == connection.targetPortId }

            if (sourcePortIndex < 0 || targetPortIndex < 0) return@mapNotNull null

            val sourceChannelProp = resolveOutputChannelProp(sourcePortIndex, sourceNode.outputPorts.size)
            val targetChannelProp = resolveInputChannelProp(targetPortIndex, targetNode.inputPorts.size)

            WiringStatement(
                targetVarName = targetNode.name.camelCase(),
                targetChannelProp = targetChannelProp,
                sourceVarName = sourceNode.name.camelCase(),
                sourceChannelProp = sourceChannelProp
            )
        }
    }

    /**
     * Resolves the output channel property name based on port index and total output count.
     *
     * 1 output: `outputChannel`
     * 2+ outputs: `outputChannel1`, `outputChannel2`, `outputChannel3`
     */
    private fun resolveOutputChannelProp(portIndex: Int, totalOutputs: Int): String {
        return if (totalOutputs == 1) {
            "outputChannel"
        } else {
            "outputChannel${portIndex + 1}"
        }
    }

    /**
     * Resolves the input channel property name based on port index and total input count.
     *
     * 1 input: `inputChannel`
     * 2+ inputs: `inputChannel1`, `inputChannel2`, `inputChannel3`
     */
    private fun resolveInputChannelProp(portIndex: Int, totalInputs: Int): String {
        return if (totalInputs == 1) {
            "inputChannel"
        } else {
            "inputChannel${portIndex + 1}"
        }
    }
}
