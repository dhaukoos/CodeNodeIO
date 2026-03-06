/*
 * ModuleController - Common interface for generated module controllers
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowGraph
import kotlinx.coroutines.flow.StateFlow

/**
 * Common interface for all generated module controllers.
 *
 * Every module's generated controller (e.g., StopWatchController, UserProfilesController)
 * implements this interface, enabling RuntimeSession to orchestrate any module's lifecycle
 * without module-specific knowledge.
 */
interface ModuleController {
    val executionState: StateFlow<ExecutionState>
    fun start(): FlowGraph
    fun stop(): FlowGraph
    fun pause(): FlowGraph
    fun resume(): FlowGraph
    fun reset(): FlowGraph
    fun setAttenuationDelay(ms: Long?)
}
