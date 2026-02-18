/*
 * RootControlNode - Master Controller for FlowGraph Execution
 * Provides unified execution control operations for entire flow
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import io.codenode.fbpdsl.runtime.RuntimeRegistry
import kotlinx.datetime.Clock

/**
 * Master controller for an entire FlowGraph.
 *
 * RootControlNode provides unified execution control operations that affect
 * all nodes in the attached FlowGraph. It acts as a single point of control
 * for starting, pausing, stopping, and monitoring the entire flow.
 *
 * This class is NOT serialized with the FlowGraph - it's a runtime controller
 * that wraps a FlowGraph for execution management.
 *
 * @property id Unique identifier for this controller
 * @property flowGraph The flow graph being controlled
 * @property name Human-readable name for this controller
 * @property createdAt Timestamp of creation (epoch milliseconds)
 * @property registry Optional RuntimeRegistry for propagating state to running NodeRuntime instances
 *
 * @sample
 * ```kotlin
 * // Create controller for a flow
 * val controller = RootControlNode.createFor(flowGraph, name = "MainController")
 *
 * // Start everything
 * val runningFlow = controller.startAll()
 *
 * // Check status
 * val status = controller.getStatus()
 * println("Running: ${status.runningCount}, Errors: ${status.errorCount}")
 *
 * // Pause for debugging
 * val pausedFlow = controller.pauseAll()
 *
 * // Stop when done
 * val stoppedFlow = controller.stopAll()
 * ```
 */
data class RootControlNode(
    val id: String,
    val flowGraph: FlowGraph,
    val name: String,
    val createdAt: Long,
    val registry: RuntimeRegistry? = null
) {
    companion object {
        /**
         * Creates a new RootControlNode for the given FlowGraph.
         *
         * @param flowGraph The FlowGraph to control
         * @param name Human-readable name (default: "Controller")
         * @param registry Optional RuntimeRegistry for runtime state propagation
         * @return New RootControlNode instance
         */
        fun createFor(
            flowGraph: FlowGraph,
            name: String = "Controller",
            registry: RuntimeRegistry? = null
        ): RootControlNode {
            return RootControlNode(
                id = "controller_${Clock.System.now().toEpochMilliseconds()}_${(0..999999).random()}",
                flowGraph = flowGraph,
                name = name,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                registry = registry
            )
        }
    }

    /**
     * Transitions all root nodes to RUNNING state.
     *
     * State propagates to all descendants (respecting independentControl).
     *
     * @return Updated FlowGraph with all applicable nodes in RUNNING state
     */
    fun startAll(): FlowGraph {
        return setAllRootNodesState(ExecutionState.RUNNING)
    }

    /**
     * Transitions all root nodes to PAUSED state.
     *
     * State propagates to all descendants (respecting independentControl).
     * Also calls pause() on all registered runtimes via the registry.
     *
     * @return Updated FlowGraph with all applicable nodes in PAUSED state
     */
    fun pauseAll(): FlowGraph {
        val updatedGraph = setAllRootNodesState(ExecutionState.PAUSED)
        registry?.pauseAll()
        return updatedGraph
    }

    /**
     * Transitions all root nodes back to RUNNING state from PAUSED.
     *
     * State propagates to all descendants (respecting independentControl).
     * Also calls resume() on all registered runtimes via the registry.
     *
     * @return Updated FlowGraph with all applicable nodes in RUNNING state
     */
    fun resumeAll(): FlowGraph {
        val updatedGraph = setAllRootNodesState(ExecutionState.RUNNING)
        registry?.resumeAll()
        return updatedGraph
    }

    /**
     * Transitions all root nodes to IDLE state.
     *
     * State propagates to all descendants (respecting independentControl).
     * Also calls stop() on all registered runtimes via the registry.
     *
     * @return Updated FlowGraph with all applicable nodes in IDLE state
     */
    fun stopAll(): FlowGraph {
        val updatedGraph = setAllRootNodesState(ExecutionState.IDLE)
        registry?.stopAll()
        return updatedGraph
    }

    /**
     * Gets aggregated execution status across all nodes.
     *
     * @return FlowExecutionStatus with accurate counts and overall state
     */
    fun getStatus(): FlowExecutionStatus {
        return FlowExecutionStatus.fromFlowGraph(flowGraph)
    }

    /**
     * Sets execution state for a specific node by ID.
     *
     * If the target is a GraphNode, state propagates to descendants
     * (respecting independentControl).
     *
     * @param nodeId The ID of the node to update
     * @param newState The new execution state
     * @return Updated FlowGraph with the node's state changed
     * @throws NoSuchElementException if nodeId is not found
     */
    fun setNodeState(nodeId: String, newState: ExecutionState): FlowGraph {
        // Find the node to verify it exists
        val targetNode = flowGraph.findNode(nodeId)
            ?: throw NoSuchElementException("Node with ID '$nodeId' not found in FlowGraph")

        // Update the node in the graph
        return updateNodeInGraph(flowGraph, nodeId, newState)
    }

    /**
     * Sets control configuration for a specific node by ID.
     *
     * If the target is a GraphNode, config propagates to descendants
     * (respecting independentControl).
     *
     * @param nodeId The ID of the node to update
     * @param newConfig The new control configuration
     * @return Updated FlowGraph with the node's config changed
     * @throws NoSuchElementException if nodeId is not found
     */
    fun setNodeConfig(nodeId: String, newConfig: ControlConfig): FlowGraph {
        val targetNode = flowGraph.findNode(nodeId)
            ?: throw NoSuchElementException("Node with ID '$nodeId' not found in FlowGraph")

        return updateNodeConfigInGraph(flowGraph, nodeId, newConfig)
    }

    // ========== Private Helper Methods ==========

    /**
     * Sets execution state on all root nodes with propagation.
     */
    private fun setAllRootNodesState(newState: ExecutionState): FlowGraph {
        val updatedRootNodes = flowGraph.rootNodes.map { node ->
            node.withExecutionState(newState, propagate = true)
        }
        return flowGraph.withNodes(updatedRootNodes)
    }

    /**
     * Updates a specific node's state within the graph hierarchy.
     */
    private fun updateNodeInGraph(graph: FlowGraph, nodeId: String, newState: ExecutionState): FlowGraph {
        val updatedRootNodes = graph.rootNodes.map { rootNode ->
            updateNodeStateRecursive(rootNode, nodeId, newState)
        }
        return graph.withNodes(updatedRootNodes)
    }

    /**
     * Recursively finds and updates a node's execution state.
     */
    private fun updateNodeStateRecursive(node: Node, targetId: String, newState: ExecutionState): Node {
        return if (node.id == targetId) {
            // Found the target node, update it
            node.withExecutionState(newState, propagate = true)
        } else if (node is GraphNode) {
            // Search in children
            val updatedChildren = node.childNodes.map { child ->
                updateNodeStateRecursive(child, targetId, newState)
            }
            node.copy(childNodes = updatedChildren)
        } else {
            // Not the target and no children
            node
        }
    }

    /**
     * Updates a specific node's config within the graph hierarchy.
     */
    private fun updateNodeConfigInGraph(graph: FlowGraph, nodeId: String, newConfig: ControlConfig): FlowGraph {
        val updatedRootNodes = graph.rootNodes.map { rootNode ->
            updateNodeConfigRecursive(rootNode, nodeId, newConfig)
        }
        return graph.withNodes(updatedRootNodes)
    }

    /**
     * Recursively finds and updates a node's control configuration.
     */
    private fun updateNodeConfigRecursive(node: Node, targetId: String, newConfig: ControlConfig): Node {
        return if (node.id == targetId) {
            // Found the target node, update it
            node.withControlConfig(newConfig, propagate = true)
        } else if (node is GraphNode) {
            // Search in children
            val updatedChildren = node.childNodes.map { child ->
                updateNodeConfigRecursive(child, targetId, newConfig)
            }
            node.copy(childNodes = updatedChildren)
        } else {
            // Not the target and no children
            node
        }
    }
}
