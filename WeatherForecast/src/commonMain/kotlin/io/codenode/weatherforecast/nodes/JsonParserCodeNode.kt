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
import io.codenode.weatherforecast.iptypes.ForecastData
import io.codenode.weatherforecast.iptypes.HttpResponse
import io.codenode.weatherforecast.models.OpenMeteoResponse
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Transformer node that receives an HttpResponse, parses the JSON body
 * using kotlinx-serialization into structured ForecastData, and outputs it.
 */
object JsonParserCodeNode : CodeNodeDefinition {
    override val name = "JsonParser"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Parses Open-Meteo JSON response into structured forecast data"
    override val inputPorts = listOf(PortSpec("response", HttpResponse::class))
    override val outputPorts = listOf(PortSpec("forecastData", ForecastData::class))

    private val json = Json { ignoreUnknownKeys = true }

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createContinuousTransformer<HttpResponse, ForecastData>(
            name = name,
            transform = { input ->
                if (input.statusCode != 200 || input.body.isEmpty()) {
                    WeatherForecastState._errorMessage.value =
                        "HTTP error: status ${input.statusCode}"
                    WeatherForecastState._isLoading.value = false
                    ForecastData(
                        dates = emptyList(),
                        maxTemperatures = emptyList(),
                        minTemperatures = emptyList(),
                        temperatureUnit = "°C"
                    )
                } else {
                    try {
                        val parsed = json.decodeFromString<OpenMeteoResponse>(input.body)
                        ForecastData(
                            dates = parsed.daily.time,
                            maxTemperatures = parsed.daily.temperatureMax,
                            minTemperatures = parsed.daily.temperatureMin,
                            temperatureUnit = parsed.dailyUnits.temperatureMax
                        )
                    } catch (e: SerializationException) {
                        WeatherForecastState._errorMessage.value =
                            "Invalid JSON format: ${e.message?.take(100)}"
                        WeatherForecastState._isLoading.value = false
                        ForecastData(
                            dates = emptyList(),
                            maxTemperatures = emptyList(),
                            minTemperatures = emptyList(),
                            temperatureUnit = "°C"
                        )
                    } catch (e: Exception) {
                        WeatherForecastState._errorMessage.value =
                            "Parse error: ${e.message?.take(100)}"
                        WeatherForecastState._isLoading.value = false
                        ForecastData(
                            dates = emptyList(),
                            maxTemperatures = emptyList(),
                            minTemperatures = emptyList(),
                            temperatureUnit = "°C"
                        )
                    }
                }
            }
        )
    }
}
