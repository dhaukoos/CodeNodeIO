/*
 * TriggerSourceCodeNode - Source node that emits coordinates on trigger
 * License: Apache 2.0
 */

package io.codenode.weatherforecast.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.NodeTypeDefinition
import io.codenode.fbpdsl.model.Port
import io.codenode.fbpdsl.model.PortTemplate
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.weatherforecast.WeatherForecastState
import io.codenode.weatherforecast.models.Coordinates
import kotlinx.coroutines.flow.drop

/**
 * Source node that reads latitude/longitude from WeatherForecastState and
 * emits a Coordinates object on each trigger (manual refresh).
 *
 * The UI triggers a refresh by updating WeatherForecastState._isLoading to true,
 * which this node observes to emit the current coordinates.
 *
 * Exposes latitude and longitude as configurable node properties in the
 * graphEditor properties panel.
 */
object TriggerSourceCodeNode : CodeNodeDefinition {
    override val name = "TriggerSource"
    override val category = CodeNodeType.SOURCE
    override val description = "Emits latitude/longitude coordinates on manual trigger"
    override val inputPorts = emptyList<PortSpec>()
    override val outputPorts = listOf(
        PortSpec("coordinates", Coordinates::class)
    )

    override fun toNodeTypeDefinition(): NodeTypeDefinition {
        val portTemplates = outputPorts.map { port ->
            PortTemplate(
                name = port.name,
                direction = Port.Direction.OUTPUT,
                dataType = port.dataType,
                required = false,
                description = "Output port: ${port.name}"
            )
        }

        return NodeTypeDefinition(
            id = name.lowercase().replace(" ", "_"),
            name = name,
            category = category,
            description = description ?: "Source node",
            portTemplates = portTemplates,
            defaultConfiguration = mapOf(
                "_genericType" to "in0out1",
                "_codeNodeDefinition" to "true",
                "_codeNodeClass" to (this::class.qualifiedName ?: ""),
                "latitude" to "40.16",
                "longitude" to "-105.10"
            ),
            configurationSchema = """
            {
                "type": "object",
                "properties": {
                    "latitude": { "type": "number", "minimum": -90, "maximum": 90 },
                    "longitude": { "type": "number", "minimum": -180, "maximum": 180 }
                },
                "required": ["latitude", "longitude"]
            }
            """.trimIndent()
        )
    }

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createContinuousSource<Coordinates>(
            name = name,
            generate = { emit ->
                // Emit once on start with current coordinates
                emit(Coordinates(
                    latitude = WeatherForecastState._latitude.value,
                    longitude = WeatherForecastState._longitude.value
                ))

                // Then emit on each loading trigger (skip initial false)
                WeatherForecastState._isLoading
                    .drop(1)
                    .collect { isLoading ->
                        if (isLoading) {
                            emit(Coordinates(
                                latitude = WeatherForecastState._latitude.value,
                                longitude = WeatherForecastState._longitude.value
                            ))
                        }
                    }
            }
        )
    }
}
