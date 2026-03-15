/*
 * DynamicPipelineBuilder - Builds runnable pipelines from FlowGraph at runtime
 * Resolves node names to CodeNodeDefinitions, creates runtimes, wires channels
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

/**
 * Builds a runnable pipeline from a FlowGraph by resolving node names to
 * CodeNodeDefinitions, creating runtimes, and wiring channels.
 */
object DynamicPipelineBuilder {

    /**
     * Checks if every CodeNode name in the FlowGraph has a CodeNodeDefinition in the lookup.
     */
    fun canBuildDynamic(flowGraph: FlowGraph, lookup: NodeDefinitionLookup): Boolean {
        return flowGraph.getAllCodeNodes().all { node ->
            lookup(node.name) != null
        }
    }

    /**
     * Validates a FlowGraph for dynamic pipeline construction.
     *
     * Checks:
     * 1. All node names are resolvable via the lookup
     * 2. All connections reference valid ports on resolved nodes
     * 3. No cycles in the connection graph
     */
    fun validate(flowGraph: FlowGraph, lookup: NodeDefinitionLookup): PipelineValidationResult {
        val errors = mutableListOf<ValidationError>()
        val codeNodes = flowGraph.getAllCodeNodes()

        if (codeNodes.isEmpty()) {
            return PipelineValidationResult.errors(listOf(
                ValidationError(
                    type = ValidationErrorType.UNRESOLVABLE_NODE,
                    message = "FlowGraph has no CodeNodes"
                )
            ))
        }

        // Check 1: All node names resolvable
        val resolvedDefs = mutableMapOf<String, CodeNodeDefinition>() // nodeId -> definition
        for (node in codeNodes) {
            val def = lookup(node.name)
            if (def == null) {
                errors.add(ValidationError(
                    type = ValidationErrorType.UNRESOLVABLE_NODE,
                    nodeId = node.id,
                    nodeName = node.name,
                    message = "Node '${node.name}' (id: ${node.id}) has no CodeNodeDefinition in the registry"
                ))
            } else {
                resolvedDefs[node.id] = def
            }
        }

        // If any nodes are unresolvable, don't bother checking ports/cycles
        if (errors.isNotEmpty()) {
            return PipelineValidationResult.errors(errors)
        }

        // Check 2: All connections reference valid ports
        // Port matching is index-based: find the port by ID in the canvas CodeNode's
        // port list, then verify the index is within the CodeNodeDefinition's port count.
        // This avoids name mismatches between DSL port names (e.g. "image") and
        // CodeNodeDefinition PortSpec names (e.g. "output1").
        for (conn in flowGraph.connections) {
            val sourceDef = resolvedDefs[conn.sourceNodeId]
            val targetDef = resolvedDefs[conn.targetNodeId]

            if (sourceDef != null) {
                val sourceNode = flowGraph.findNode(conn.sourceNodeId) as? CodeNode
                val outputIndex = sourceNode?.outputPorts?.indexOfFirst { it.id == conn.sourcePortId } ?: -1
                if (outputIndex < 0 || outputIndex >= sourceDef.outputPorts.size) {
                    val portName = extractPortName(conn.sourcePortId, conn.sourceNodeId)
                    errors.add(ValidationError(
                        type = ValidationErrorType.INVALID_PORT,
                        nodeId = conn.sourceNodeId,
                        nodeName = sourceDef.name,
                        message = "Output port '$portName' (index $outputIndex) not valid on node '${sourceDef.name}' (has ${sourceDef.outputPorts.size} output ports)"
                    ))
                }
            }

            if (targetDef != null) {
                val targetNode = flowGraph.findNode(conn.targetNodeId) as? CodeNode
                val inputIndex = targetNode?.inputPorts?.indexOfFirst { it.id == conn.targetPortId } ?: -1
                if (inputIndex < 0 || inputIndex >= targetDef.inputPorts.size) {
                    val portName = extractPortName(conn.targetPortId, conn.targetNodeId)
                    errors.add(ValidationError(
                        type = ValidationErrorType.INVALID_PORT,
                        nodeId = conn.targetNodeId,
                        nodeName = targetDef.name,
                        message = "Input port '$portName' (index $inputIndex) not valid on node '${targetDef.name}' (has ${targetDef.inputPorts.size} input ports)"
                    ))
                }
            }
        }

        // Check 3: Cycle detection via topological sort
        val nodeIds = codeNodes.map { it.id }.toSet()
        val adjacency = mutableMapOf<String, MutableList<String>>()
        for (id in nodeIds) adjacency[id] = mutableListOf()
        for (conn in flowGraph.connections) {
            if (conn.sourceNodeId in nodeIds && conn.targetNodeId in nodeIds) {
                adjacency[conn.sourceNodeId]!!.add(conn.targetNodeId)
            }
        }

        val visited = mutableSetOf<String>()
        val inStack = mutableSetOf<String>()
        var hasCycle = false

        fun dfs(nodeId: String) {
            if (hasCycle) return
            visited.add(nodeId)
            inStack.add(nodeId)
            for (neighbor in adjacency[nodeId] ?: emptyList()) {
                if (neighbor in inStack) {
                    hasCycle = true
                    return
                }
                if (neighbor !in visited) {
                    dfs(neighbor)
                }
            }
            inStack.remove(nodeId)
        }

        for (nodeId in nodeIds) {
            if (nodeId !in visited) dfs(nodeId)
        }

        if (hasCycle) {
            errors.add(ValidationError(
                type = ValidationErrorType.CYCLE_DETECTED,
                message = "Connection graph contains a cycle"
            ))
        }

        return if (errors.isEmpty()) {
            PipelineValidationResult.valid()
        } else {
            PipelineValidationResult.errors(errors)
        }
    }

    /**
     * Builds a DynamicPipeline from a validated FlowGraph.
     *
     * Creates NodeRuntime instances via CodeNodeDefinition.createRuntime() and
     * prepares wiring instructions for channel connections.
     *
     * @throws IllegalStateException if validation fails
     */
    fun build(flowGraph: FlowGraph, lookup: NodeDefinitionLookup): DynamicPipeline {
        val codeNodes = flowGraph.getAllCodeNodes()
        val runtimes = mutableMapOf<String, NodeRuntime>()
        val definitions = mutableMapOf<String, CodeNodeDefinition>()

        for (node in codeNodes) {
            val def = lookup(node.name)
                ?: error("Node '${node.name}' not found in registry. Call validate() first.")
            definitions[node.id] = def
            runtimes[node.id] = def.createRuntime(node.name)
        }

        return DynamicPipeline(
            flowGraph = flowGraph,
            runtimes = runtimes,
            definitions = definitions
        )
    }

    /**
     * Extracts the port name from a canvas port ID.
     *
     * Canvas port IDs follow the pattern: {nodeId}_input_{portName} or {nodeId}_output_{portName}
     * We need to strip the node ID prefix and direction to get the port name.
     */
    internal fun extractPortName(portId: String, nodeId: String): String {
        // Port IDs created by DragAndDropHandler: "${nodeId}_input_${template.name}"
        // nodeId may contain underscores, so we strip the known prefix
        val nodePrefix = nodeId.removePrefix("node_")
        val inputPrefix = "${nodePrefix}_input_"
        val outputPrefix = "${nodePrefix}_output_"

        return when {
            portId.startsWith(inputPrefix) -> portId.removePrefix(inputPrefix)
            portId.startsWith(outputPrefix) -> portId.removePrefix(outputPrefix)
            // Fallback: try after last known separator
            "_input_" in portId -> portId.substringAfter("_input_")
            "_output_" in portId -> portId.substringAfter("_output_")
            else -> portId
        }
    }
}

/**
 * A runtime-constructed pipeline built from a FlowGraph snapshot.
 *
 * Manages lifecycle (start, stop, pause, resume) for all dynamically-created runtimes
 * and handles channel wiring based on FlowGraph connections.
 */
class DynamicPipeline(
    val flowGraph: FlowGraph,
    val runtimes: Map<String, NodeRuntime>,
    private val definitions: Map<String, CodeNodeDefinition>
) {
    private val createdChannels = mutableListOf<Channel<Any>>()

    /**
     * Starts all runtimes in topological order and wires channels.
     *
     * Order: sources first (they create owned channels), then wire, then downstream.
     */
    fun start(scope: CoroutineScope) {
        val sortedIds = topologicalSort()

        // Separate sources from non-sources
        val sourceIds = sortedIds.filter { isSourceRuntime(runtimes[it]!!) }
        val nonSourceIds = sortedIds.filter { !isSourceRuntime(runtimes[it]!!) }

        // 1. Start sources (they create/recreate owned output channels)
        for (nodeId in sourceIds) {
            runtimes[nodeId]!!.start(scope) {}
        }

        // 2. Wire all connections
        wireConnections()

        // 3. Start non-source runtimes in topological order
        for (nodeId in nonSourceIds) {
            runtimes[nodeId]!!.start(scope) {}
        }
    }

    /**
     * Stops all runtimes and closes all created channels.
     */
    fun stop() {
        for (runtime in runtimes.values) {
            runtime.stop()
        }
        for (channel in createdChannels) {
            channel.close()
        }
        createdChannels.clear()
    }

    /**
     * Pauses all runtimes via the registry pattern.
     */
    fun pause() {
        for (runtime in runtimes.values) {
            runtime.pause()
        }
    }

    /**
     * Resumes all runtimes.
     */
    fun resume() {
        for (runtime in runtimes.values) {
            runtime.resume()
        }
    }

    /**
     * Sets a RuntimeRegistry on all runtimes for centralized lifecycle control.
     */
    fun setRegistry(registry: RuntimeRegistry) {
        for (runtime in runtimes.values) {
            runtime.registry = registry
        }
    }

    /**
     * Sets attenuation delay on all runtimes.
     */
    fun setAttenuationDelay(ms: Long?) {
        for (runtime in runtimes.values) {
            runtime.attenuationDelayMs = ms
        }
    }

    /**
     * Sets emission observer on all runtimes.
     */
    fun setEmissionObserver(observer: ((String, Int) -> Unit)?) {
        for (runtime in runtimes.values) {
            runtime.onEmit = observer
        }
    }

    /**
     * Sets value observer on all runtimes.
     */
    fun setValueObserver(observer: ((String, Int, Any?) -> Unit)?) {
        for (runtime in runtimes.values) {
            runtime.onEmitValue = observer
        }
    }

    /**
     * Wire channels between runtimes based on FlowGraph connections.
     *
     * For source runtimes (which own their output channels): read the source's
     * output Channel and assign it directly to the target's input.
     *
     * For non-source runtimes (Transformer, Processor, Filter): create an
     * intermediate Channel and assign it to both source output and target input.
     */
    @Suppress("UNCHECKED_CAST")
    private fun wireConnections() {
        for (conn in flowGraph.connections) {
            val sourceRuntime = runtimes[conn.sourceNodeId] ?: continue
            val targetRuntime = runtimes[conn.targetNodeId] ?: continue

            // Index-based port matching: find port by ID in the canvas CodeNode's
            // port list to get the positional index for runtime channel wiring
            val sourceNode = flowGraph.findNode(conn.sourceNodeId) as? CodeNode ?: continue
            val targetNode = flowGraph.findNode(conn.targetNodeId) as? CodeNode ?: continue
            val outputIndex = sourceNode.outputPorts.indexOfFirst { it.id == conn.sourcePortId }
            val inputIndex = targetNode.inputPorts.indexOfFirst { it.id == conn.targetPortId }

            if (outputIndex < 0 || inputIndex < 0) continue

            if (isSourceRuntime(sourceRuntime) || isMultiOutputRuntime(sourceRuntime)) {
                // Source/multi-output runtimes own their output channels (Channel type, private set)
                // Read the channel and assign to downstream input
                val channel = getOwnedOutputChannel(sourceRuntime, outputIndex) ?: continue
                setInputChannel(targetRuntime, inputIndex, channel)
            } else {
                // Transformer/Filter/InXOut1 runtimes have SendChannel outputs
                // Create intermediate Channel for both sides
                val channel = Channel<Any>(Channel.BUFFERED)
                createdChannels.add(channel)
                setOutputChannel(sourceRuntime, outputIndex, channel)
                setInputChannel(targetRuntime, inputIndex, channel)
            }
        }
    }

    /**
     * Topological sort of node IDs (sources first, sinks last).
     */
    private fun topologicalSort(): List<String> {
        val nodeIds = runtimes.keys.toList()
        val inDegree = mutableMapOf<String, Int>()
        val adjacency = mutableMapOf<String, MutableList<String>>()

        for (id in nodeIds) {
            inDegree[id] = 0
            adjacency[id] = mutableListOf()
        }

        for (conn in flowGraph.connections) {
            if (conn.sourceNodeId in runtimes && conn.targetNodeId in runtimes) {
                adjacency[conn.sourceNodeId]!!.add(conn.targetNodeId)
                inDegree[conn.targetNodeId] = (inDegree[conn.targetNodeId] ?: 0) + 1
            }
        }

        val queue = ArrayDeque<String>()
        for (id in nodeIds) {
            if ((inDegree[id] ?: 0) == 0) queue.add(id)
        }

        val sorted = mutableListOf<String>()
        while (queue.isNotEmpty()) {
            val nodeId = queue.removeFirst()
            sorted.add(nodeId)
            for (neighbor in adjacency[nodeId] ?: emptyList()) {
                inDegree[neighbor] = (inDegree[neighbor] ?: 1) - 1
                if (inDegree[neighbor] == 0) queue.add(neighbor)
            }
        }

        // Add any remaining disconnected nodes
        for (id in nodeIds) {
            if (id !in sorted) sorted.add(id)
        }

        return sorted
    }

    private fun isSourceRuntime(runtime: NodeRuntime): Boolean {
        return runtime is SourceRuntime<*> ||
                runtime is SourceOut2Runtime<*, *> ||
                runtime is SourceOut3Runtime<*, *, *>
    }

    private fun isMultiOutputRuntime(runtime: NodeRuntime): Boolean {
        return runtime is In1Out2Runtime<*, *, *> ||
                runtime is In1Out3Runtime<*, *, *, *> ||
                runtime is In2Out2Runtime<*, *, *, *> ||
                runtime is In2Out3Runtime<*, *, *, *, *> ||
                runtime is In3Out2Runtime<*, *, *, *, *> ||
                runtime is In3Out3Runtime<*, *, *, *, *, *> ||
                runtime is In2AnyOut2Runtime<*, *, *, *> ||
                runtime is In2AnyOut3Runtime<*, *, *, *, *> ||
                runtime is In3AnyOut2Runtime<*, *, *, *, *> ||
                runtime is In3AnyOut3Runtime<*, *, *, *, *, *>
    }

    /**
     * Gets an owned output Channel from a source or multi-output runtime.
     */
    @Suppress("UNCHECKED_CAST")
    private fun getOwnedOutputChannel(runtime: NodeRuntime, portIndex: Int): Channel<Any>? {
        return when (runtime) {
            is SourceRuntime<*> -> runtime.outputChannel as? Channel<Any>
            is SourceOut2Runtime<*, *> -> when (portIndex) {
                0 -> runtime.outputChannel1 as? Channel<Any>
                1 -> runtime.outputChannel2 as? Channel<Any>
                else -> null
            }
            is SourceOut3Runtime<*, *, *> -> when (portIndex) {
                0 -> runtime.outputChannel1 as? Channel<Any>
                1 -> runtime.outputChannel2 as? Channel<Any>
                2 -> runtime.outputChannel3 as? Channel<Any>
                else -> null
            }
            is In1Out2Runtime<*, *, *> -> when (portIndex) {
                0 -> runtime.outputChannel1 as? Channel<Any>
                1 -> runtime.outputChannel2 as? Channel<Any>
                else -> null
            }
            is In1Out3Runtime<*, *, *, *> -> when (portIndex) {
                0 -> runtime.outputChannel1 as? Channel<Any>
                1 -> runtime.outputChannel2 as? Channel<Any>
                2 -> runtime.outputChannel3 as? Channel<Any>
                else -> null
            }
            is In2Out2Runtime<*, *, *, *> -> when (portIndex) {
                0 -> runtime.outputChannel1 as? Channel<Any>
                1 -> runtime.outputChannel2 as? Channel<Any>
                else -> null
            }
            is In2Out3Runtime<*, *, *, *, *> -> when (portIndex) {
                0 -> runtime.outputChannel1 as? Channel<Any>
                1 -> runtime.outputChannel2 as? Channel<Any>
                2 -> runtime.outputChannel3 as? Channel<Any>
                else -> null
            }
            is In3Out2Runtime<*, *, *, *, *> -> when (portIndex) {
                0 -> runtime.outputChannel1 as? Channel<Any>
                1 -> runtime.outputChannel2 as? Channel<Any>
                else -> null
            }
            is In3Out3Runtime<*, *, *, *, *, *> -> when (portIndex) {
                0 -> runtime.outputChannel1 as? Channel<Any>
                1 -> runtime.outputChannel2 as? Channel<Any>
                2 -> runtime.outputChannel3 as? Channel<Any>
                else -> null
            }
            is In2AnyOut2Runtime<*, *, *, *> -> when (portIndex) {
                0 -> runtime.outputChannel1 as? Channel<Any>
                1 -> runtime.outputChannel2 as? Channel<Any>
                else -> null
            }
            is In2AnyOut3Runtime<*, *, *, *, *> -> when (portIndex) {
                0 -> runtime.outputChannel1 as? Channel<Any>
                1 -> runtime.outputChannel2 as? Channel<Any>
                2 -> runtime.outputChannel3 as? Channel<Any>
                else -> null
            }
            is In3AnyOut2Runtime<*, *, *, *, *> -> when (portIndex) {
                0 -> runtime.outputChannel1 as? Channel<Any>
                1 -> runtime.outputChannel2 as? Channel<Any>
                else -> null
            }
            is In3AnyOut3Runtime<*, *, *, *, *, *> -> when (portIndex) {
                0 -> runtime.outputChannel1 as? Channel<Any>
                1 -> runtime.outputChannel2 as? Channel<Any>
                2 -> runtime.outputChannel3 as? Channel<Any>
                else -> null
            }
            else -> null
        }
    }

    /**
     * Sets the output channel on a non-source, single-output runtime.
     */
    @Suppress("UNCHECKED_CAST")
    private fun setOutputChannel(runtime: NodeRuntime, portIndex: Int, channel: Channel<Any>) {
        when (runtime) {
            is TransformerRuntime<*, *> ->
                (runtime as TransformerRuntime<Any, Any>).outputChannel = channel
            is FilterRuntime<*> ->
                (runtime as FilterRuntime<Any>).outputChannel = channel
            is In2Out1Runtime<*, *, *> ->
                (runtime as In2Out1Runtime<Any, Any, Any>).outputChannel = channel
            is In3Out1Runtime<*, *, *, *> ->
                (runtime as In3Out1Runtime<Any, Any, Any, Any>).outputChannel = channel
            is In2AnyOut1Runtime<*, *, *> ->
                (runtime as In2AnyOut1Runtime<Any, Any, Any>).outputChannel = channel
            is In3AnyOut1Runtime<*, *, *, *> ->
                (runtime as In3AnyOut1Runtime<Any, Any, Any, Any>).outputChannel = channel
        }
    }

    /**
     * Sets the input channel on a runtime at the given port index.
     */
    @Suppress("UNCHECKED_CAST")
    private fun setInputChannel(runtime: NodeRuntime, portIndex: Int, channel: ReceiveChannel<Any>) {
        when (runtime) {
            // Single input (index 0 only)
            is SinkRuntime<*> ->
                (runtime as SinkRuntime<Any>).inputChannel = channel
            is TransformerRuntime<*, *> ->
                (runtime as TransformerRuntime<Any, Any>).inputChannel = channel
            is FilterRuntime<*> ->
                (runtime as FilterRuntime<Any>).inputChannel = channel

            // Single input on multi-output types
            is In1Out2Runtime<*, *, *> ->
                (runtime as In1Out2Runtime<Any, Any, Any>).inputChannel = channel
            is In1Out3Runtime<*, *, *, *> ->
                (runtime as In1Out3Runtime<Any, Any, Any, Any>).inputChannel = channel

            // 2-input types
            is In2Out1Runtime<*, *, *> -> when (portIndex) {
                0 -> (runtime as In2Out1Runtime<Any, Any, Any>).inputChannel1 = channel
                1 -> (runtime as In2Out1Runtime<Any, Any, Any>).inputChannel2 = channel
            }
            is In2Out2Runtime<*, *, *, *> -> when (portIndex) {
                0 -> (runtime as In2Out2Runtime<Any, Any, Any, Any>).inputChannel1 = channel
                1 -> (runtime as In2Out2Runtime<Any, Any, Any, Any>).inputChannel2 = channel
            }
            is In2Out3Runtime<*, *, *, *, *> -> when (portIndex) {
                0 -> (runtime as In2Out3Runtime<Any, Any, Any, Any, Any>).inputChannel1 = channel
                1 -> (runtime as In2Out3Runtime<Any, Any, Any, Any, Any>).inputChannel2 = channel
            }
            is SinkIn2Runtime<*, *> -> when (portIndex) {
                0 -> (runtime as SinkIn2Runtime<Any, Any>).inputChannel1 = channel
                1 -> (runtime as SinkIn2Runtime<Any, Any>).inputChannel2 = channel
            }
            is In2AnyOut1Runtime<*, *, *> -> when (portIndex) {
                0 -> (runtime as In2AnyOut1Runtime<Any, Any, Any>).inputChannel1 = channel
                1 -> (runtime as In2AnyOut1Runtime<Any, Any, Any>).inputChannel2 = channel
            }
            is In2AnyOut2Runtime<*, *, *, *> -> when (portIndex) {
                0 -> (runtime as In2AnyOut2Runtime<Any, Any, Any, Any>).inputChannel1 = channel
                1 -> (runtime as In2AnyOut2Runtime<Any, Any, Any, Any>).inputChannel2 = channel
            }
            is In2AnyOut3Runtime<*, *, *, *, *> -> when (portIndex) {
                0 -> (runtime as In2AnyOut3Runtime<Any, Any, Any, Any, Any>).inputChannel1 = channel
                1 -> (runtime as In2AnyOut3Runtime<Any, Any, Any, Any, Any>).inputChannel2 = channel
            }
            is SinkIn2AnyRuntime<*, *> -> when (portIndex) {
                0 -> (runtime as SinkIn2AnyRuntime<Any, Any>).inputChannel1 = channel
                1 -> (runtime as SinkIn2AnyRuntime<Any, Any>).inputChannel2 = channel
            }

            // 3-input types
            is In3Out1Runtime<*, *, *, *> -> when (portIndex) {
                0 -> (runtime as In3Out1Runtime<Any, Any, Any, Any>).inputChannel1 = channel
                1 -> (runtime as In3Out1Runtime<Any, Any, Any, Any>).inputChannel2 = channel
                2 -> (runtime as In3Out1Runtime<Any, Any, Any, Any>).inputChannel3 = channel
            }
            is In3Out2Runtime<*, *, *, *, *> -> when (portIndex) {
                0 -> (runtime as In3Out2Runtime<Any, Any, Any, Any, Any>).inputChannel1 = channel
                1 -> (runtime as In3Out2Runtime<Any, Any, Any, Any, Any>).inputChannel2 = channel
                2 -> (runtime as In3Out2Runtime<Any, Any, Any, Any, Any>).inputChannel3 = channel
            }
            is In3Out3Runtime<*, *, *, *, *, *> -> when (portIndex) {
                0 -> (runtime as In3Out3Runtime<Any, Any, Any, Any, Any, Any>).inputChannel1 = channel
                1 -> (runtime as In3Out3Runtime<Any, Any, Any, Any, Any, Any>).inputChannel2 = channel
                2 -> (runtime as In3Out3Runtime<Any, Any, Any, Any, Any, Any>).inputChannel3 = channel
            }
            is SinkIn3Runtime<*, *, *> -> when (portIndex) {
                0 -> (runtime as SinkIn3Runtime<Any, Any, Any>).inputChannel1 = channel
                1 -> (runtime as SinkIn3Runtime<Any, Any, Any>).inputChannel2 = channel
                2 -> (runtime as SinkIn3Runtime<Any, Any, Any>).inputChannel3 = channel
            }
            is In3AnyOut1Runtime<*, *, *, *> -> when (portIndex) {
                0 -> (runtime as In3AnyOut1Runtime<Any, Any, Any, Any>).inputChannel1 = channel
                1 -> (runtime as In3AnyOut1Runtime<Any, Any, Any, Any>).inputChannel2 = channel
                2 -> (runtime as In3AnyOut1Runtime<Any, Any, Any, Any>).inputChannel3 = channel
            }
            is In3AnyOut2Runtime<*, *, *, *, *> -> when (portIndex) {
                0 -> (runtime as In3AnyOut2Runtime<Any, Any, Any, Any, Any>).inputChannel1 = channel
                1 -> (runtime as In3AnyOut2Runtime<Any, Any, Any, Any, Any>).inputChannel2 = channel
                2 -> (runtime as In3AnyOut2Runtime<Any, Any, Any, Any, Any>).inputChannel3 = channel
            }
            is In3AnyOut3Runtime<*, *, *, *, *, *> -> when (portIndex) {
                0 -> (runtime as In3AnyOut3Runtime<Any, Any, Any, Any, Any, Any>).inputChannel1 = channel
                1 -> (runtime as In3AnyOut3Runtime<Any, Any, Any, Any, Any, Any>).inputChannel2 = channel
                2 -> (runtime as In3AnyOut3Runtime<Any, Any, Any, Any, Any, Any>).inputChannel3 = channel
            }
            is SinkIn3AnyRuntime<*, *, *> -> when (portIndex) {
                0 -> (runtime as SinkIn3AnyRuntime<Any, Any, Any>).inputChannel1 = channel
                1 -> (runtime as SinkIn3AnyRuntime<Any, Any, Any>).inputChannel2 = channel
                2 -> (runtime as SinkIn3AnyRuntime<Any, Any, Any>).inputChannel3 = channel
            }
        }
    }
}
