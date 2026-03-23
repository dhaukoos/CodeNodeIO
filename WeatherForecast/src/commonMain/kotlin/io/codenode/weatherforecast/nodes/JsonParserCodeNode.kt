/*
 * JsonParserCodeNode - Transformer node that parses Open-Meteo JSON response
 * License: Apache 2.0
 */

package io.codenode.weatherforecast.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.weatherforecast.WeatherForecastState
import io.codenode.weatherforecast.models.OpenMeteoResponse
import kotlinx.serialization.json.Json

/**
 * Transformer node that receives an HttpResponse map, parses the JSON body
 * using kotlinx-serialization into structured ForecastData, and outputs it.
 */
object JsonParserCodeNode : CodeNodeDefinition {
    override val name = "JsonParser"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Parses Open-Meteo JSON response into structured forecast data"
    override val inputPorts = listOf(PortSpec("response", Any::class))
    override val outputPorts = listOf(PortSpec("forecastData", Any::class))

    private val json = Json { ignoreUnknownKeys = true }

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createContinuousTransformer<Any, Any>(
            name = name,
            transform = { input ->
                val response = input as Map<*, *>
                val statusCode = response["statusCode"] as? Int ?: 0
                val body = response["body"] as? String ?: ""

                if (statusCode != 200 || body.isEmpty()) {
                    WeatherForecastState._errorMessage.value =
                        "HTTP error: status $statusCode"
                    WeatherForecastState._isLoading.value = false
                    mapOf(
                        "dates" to emptyList<String>(),
                        "maxTemperatures" to emptyList<Double>(),
                        "minTemperatures" to emptyList<Double>(),
                        "temperatureUnit" to "°C"
                    )
                } else {
                    try {
                        val parsed = json.decodeFromString<OpenMeteoResponse>(body)
                        mapOf(
                            "dates" to parsed.daily.time,
                            "maxTemperatures" to parsed.daily.temperatureMax,
                            "minTemperatures" to parsed.daily.temperatureMin,
                            "temperatureUnit" to parsed.dailyUnits.temperatureMax
                        )
                    } catch (e: Exception) {
                        WeatherForecastState._errorMessage.value =
                            "Parse error: ${e.message}"
                        WeatherForecastState._isLoading.value = false
                        mapOf(
                            "dates" to emptyList<String>(),
                            "maxTemperatures" to emptyList<Double>(),
                            "minTemperatures" to emptyList<Double>(),
                            "temperatureUnit" to "°C"
                        )
                    }
                }
            }
        )
    }
}
