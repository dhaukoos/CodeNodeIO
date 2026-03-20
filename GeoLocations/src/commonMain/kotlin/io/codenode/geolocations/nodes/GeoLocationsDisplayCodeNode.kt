/*
 * GeoLocationsDisplayCodeNode - Self-contained sink node for display output
 * License: Apache 2.0
 */

package io.codenode.geolocations.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.geolocations.GeoLocationsState

/**
 * Sink node that receives result and error values and updates GeoLocationsState.
 *
 * Uses SinkIn2 (synchronous receive from both channels) matching the
 * existing generated GeoLocationsDisplay behavior.
 */
object GeoLocationsDisplayCodeNode : CodeNodeDefinition {
    override val name = "GeoLocationsDisplay"
    override val category = CodeNodeType.SINK
    override val description = "Displays result and error messages for geo location operations"
    override val inputPorts = listOf(
        PortSpec("result", Any::class),
        PortSpec("error", Any::class)
    )
    override val outputPorts = emptyList<PortSpec>()

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createSinkIn2<Any, Any>(
            name = name,
            consume = { result, error ->
                GeoLocationsState._result.value = result
                GeoLocationsState._error.value = error
            }
        )
    }
}
