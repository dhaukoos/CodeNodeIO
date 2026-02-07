/*
 * ControlConfig - Execution Control Configuration for FBP Nodes
 * Configuration for pause/resume/speed attenuation and independent control
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.serialization.Serializable

/**
 * Configuration for execution control operations.
 *
 * This class was extracted from CodeNode to enable both CodeNode and GraphNode
 * to share the same control configuration, enabling hierarchical control
 * propagation through the node tree.
 *
 * @property pauseBufferSize Maximum number of InformationPackets to buffer when paused.
 *                           Must be positive.
 * @property speedAttenuation Delay in milliseconds between processing cycles.
 *                            Useful for debugging/simulation. Must be non-negative.
 * @property autoResumeOnError Whether to automatically resume after error state.
 * @property independentControl When true, exempts this node from parent state propagation.
 *                              The node retains its own executionState and controlConfig
 *                              when parent changes. Must be controlled directly.
 *
 * **Behavior**:
 * - When `independentControl = true`, the node:
 *   - Retains its own `executionState` when parent changes
 *   - Retains its own `controlConfig` settings when parent changes
 *   - Must be controlled directly (not via parent propagation)
 * - When `independentControl = false` (default), the node:
 *   - Inherits parent's `executionState` on parent state change
 *   - Inherits parent's `controlConfig` on parent config change (except `independentControl` itself)
 */
@Serializable
data class ControlConfig(
    val pauseBufferSize: Int = 100,
    val speedAttenuation: Long = 0L,
    val autoResumeOnError: Boolean = false,
    val independentControl: Boolean = false
) {
    init {
        require(pauseBufferSize > 0) { "Pause buffer size must be positive, got $pauseBufferSize" }
        require(speedAttenuation >= 0L) { "Speed attenuation cannot be negative, got $speedAttenuation" }
    }
}
