/*
 * AddressesDisplayCodeNode - Self-contained sink node for display output
 * License: Apache 2.0
 */

package io.codenode.addresses.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.addresses.AddressesState

/**
 * Sink node that receives result and error values and updates AddressesState.
 *
 * Uses SinkIn2 (synchronous receive from both channels) matching the
 * existing generated AddressesDisplay behavior.
 */
object AddressesDisplayCodeNode : CodeNodeDefinition {
    override val name = "AddressesDisplay"
    override val category = CodeNodeType.SINK
    override val description = "Displays result and error messages for address operations"
    override val inputPorts = listOf(
        PortSpec("result", String::class),
        PortSpec("error", String::class)
    )
    override val outputPorts = emptyList<PortSpec>()

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createSinkIn2<String, String>(
            name = name,
            consume = { result, error ->
                AddressesState._result.value = result
                AddressesState._error.value = error
            }
        )
    }
}
