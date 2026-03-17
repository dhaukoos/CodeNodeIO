/*
 * UserProfilesDisplayCodeNode - Self-contained sink node for display output
 * License: Apache 2.0
 */

package io.codenode.userprofiles.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeCategory
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.userprofiles.UserProfilesState

/**
 * Sink node that receives result and error values and updates UserProfilesState.
 *
 * Uses SinkIn2 (synchronous receive from both channels) matching the
 * existing generated UserProfilesDisplay behavior.
 */
object UserProfilesDisplayCodeNode : CodeNodeDefinition {
    override val name = "UserProfilesDisplay"
    override val category = NodeCategory.SINK
    override val description = "Displays result and error messages for user profile operations"
    override val inputPorts = listOf(
        PortSpec("result", Any::class),
        PortSpec("error", Any::class)
    )
    override val outputPorts = emptyList<PortSpec>()

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createSinkIn2<Any, Any>(
            name = name,
            consume = { result, error ->
                UserProfilesState._result.value = result
                UserProfilesState._error.value = error
            }
        )
    }
}
