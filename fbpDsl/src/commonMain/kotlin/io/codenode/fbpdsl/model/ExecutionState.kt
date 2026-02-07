/*
 * ExecutionState - Execution Lifecycle State for FBP Nodes
 * Represents the current execution state of any Node in the graph
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.serialization.Serializable

/**
 * Represents the execution lifecycle state of any Node in the FBP graph.
 *
 * This enum was extracted from CodeNode to enable both CodeNode and GraphNode
 * to share the same execution state management, enabling hierarchical control
 * propagation through the node tree.
 *
 * **State Transitions**:
 * ```
 * IDLE → RUNNING (start)
 * RUNNING → PAUSED (pause)
 * RUNNING → ERROR (error occurred)
 * PAUSED → RUNNING (resume)
 * PAUSED → IDLE (stop)
 * ERROR → IDLE (reset)
 * ERROR → RUNNING (force start / error recovery)
 * ```
 */
@Serializable
enum class ExecutionState {
    /** Node is not currently processing; ready to start */
    IDLE,

    /** Node is actively processing InformationPackets */
    RUNNING,

    /** Node execution is paused; buffering incoming packets */
    PAUSED,

    /** Node encountered an error and stopped execution */
    ERROR
}
