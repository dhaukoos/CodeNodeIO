/*
 * TriggerSourceCodeNode - Source node that emits coordinates on trigger
 * License: Apache 2.0
 */

package io.codenode.weatherforecast.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.weatherforecast.WeatherForecastState
import kotlinx.coroutines.flow.drop

/**
 * Source node that reads latitude/longitude from WeatherForecastState and
 * emits a Coordinates map on each trigger (manual refresh).
 *
 * The UI triggers a refresh by updating WeatherForecastState._isLoading to true,
 * which this node observes to emit the current coordinates.
 */
object TriggerSourceCodeNode : CodeNodeDefinition {
    override val name = "TriggerSource"
    override val category = CodeNodeType.SOURCE
    override val description = "Emits latitude/longitude coordinates on manual trigger"
    override val inputPorts = emptyList<PortSpec>()
    override val outputPorts = listOf(
        PortSpec("coordinates", Any::class)
    )

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createContinuousSource<Any>(
            name = name,
            generate = { emit ->
                // Emit once on start with current coordinates
                val initialCoords = mapOf(
                    "latitude" to WeatherForecastState._latitude.value,
                    "longitude" to WeatherForecastState._longitude.value
                )
                emit(initialCoords)

                // Then emit on each loading trigger (skip initial false)
                WeatherForecastState._isLoading
                    .drop(1)
                    .collect { isLoading ->
                        if (isLoading) {
                            val coords = mapOf(
                                "latitude" to WeatherForecastState._latitude.value,
                                "longitude" to WeatherForecastState._longitude.value
                            )
                            emit(coords)
                        }
                    }
            }
        )
    }
}
