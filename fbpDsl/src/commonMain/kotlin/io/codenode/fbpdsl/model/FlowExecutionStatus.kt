/*
 * FlowExecutionStatus - Aggregated execution status across a FlowGraph
 * Provides counts and overall state for monitoring and control
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

/**
 * Aggregated execution status across a FlowGraph.
 *
 * This class provides a snapshot of the execution state across all nodes
 * in a FlowGraph, useful for monitoring, debugging, and control operations.
 *
 * @property totalNodes Total number of nodes in the graph (including descendants)
 * @property idleCount Count of nodes in IDLE state
 * @property runningCount Count of nodes in RUNNING state
 * @property pausedCount Count of nodes in PAUSED state
 * @property errorCount Count of nodes in ERROR state
 * @property independentControlCount Count of nodes with independentControl enabled
 * @property overallState Derived overall state based on priority rules
 *
 * **Derivation Rules for overallState**:
 * 1. If any node is in ERROR → ERROR
 * 2. Else if any node is RUNNING → RUNNING
 * 3. Else if any node is PAUSED → PAUSED
 * 4. Else → IDLE
 */
data class FlowExecutionStatus(
    val totalNodes: Int,
    val idleCount: Int,
    val runningCount: Int,
    val pausedCount: Int,
    val errorCount: Int,
    val independentControlCount: Int,
    val overallState: ExecutionState
) {
    init {
        require(totalNodes >= 0) { "Total nodes cannot be negative" }
        require(idleCount >= 0) { "Idle count cannot be negative" }
        require(runningCount >= 0) { "Running count cannot be negative" }
        require(pausedCount >= 0) { "Paused count cannot be negative" }
        require(errorCount >= 0) { "Error count cannot be negative" }
        require(independentControlCount >= 0) { "Independent control count cannot be negative" }
    }

    companion object {
        /**
         * Creates a FlowExecutionStatus by analyzing all nodes in a FlowGraph.
         *
         * @param flowGraph The FlowGraph to analyze
         * @return FlowExecutionStatus with accurate counts and derived overallState
         */
        fun fromFlowGraph(flowGraph: FlowGraph): FlowExecutionStatus {
            val allNodes = flowGraph.getAllNodes()

            var idleCount = 0
            var runningCount = 0
            var pausedCount = 0
            var errorCount = 0
            var independentControlCount = 0

            allNodes.forEach { node ->
                when (node.executionState) {
                    ExecutionState.IDLE -> idleCount++
                    ExecutionState.RUNNING -> runningCount++
                    ExecutionState.PAUSED -> pausedCount++
                    ExecutionState.ERROR -> errorCount++
                }

                if (node.controlConfig.independentControl) {
                    independentControlCount++
                }
            }

            // Derive overall state based on priority rules
            val overallState = when {
                errorCount > 0 -> ExecutionState.ERROR
                runningCount > 0 -> ExecutionState.RUNNING
                pausedCount > 0 -> ExecutionState.PAUSED
                else -> ExecutionState.IDLE
            }

            return FlowExecutionStatus(
                totalNodes = allNodes.size,
                idleCount = idleCount,
                runningCount = runningCount,
                pausedCount = pausedCount,
                errorCount = errorCount,
                independentControlCount = independentControlCount,
                overallState = overallState
            )
        }
    }
}
