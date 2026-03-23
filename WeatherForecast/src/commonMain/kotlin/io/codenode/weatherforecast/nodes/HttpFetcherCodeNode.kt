/*
 * HttpFetcherCodeNode - Transformer node that fetches weather data via HTTPS
 * License: Apache 2.0
 */

package io.codenode.weatherforecast.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.weatherforecast.WeatherForecastState
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

/**
 * Transformer node that receives Coordinates, constructs an Open-Meteo API URL,
 * performs an HTTPS GET request using Ktor Client, and outputs an HttpResponse map.
 */
object HttpFetcherCodeNode : CodeNodeDefinition {
    override val name = "HttpFetcher"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Fetches weather forecast data from Open-Meteo via HTTPS GET"
    override val inputPorts = listOf(PortSpec("coordinates", Any::class))
    override val outputPorts = listOf(PortSpec("response", Any::class))

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createContinuousTransformer<Any, Any>(
            name = name,
            transform = { input ->
                val coords = input as Map<*, *>
                val lat = coords["latitude"] as? Double ?: 40.16
                val lon = coords["longitude"] as? Double ?: -105.10

                val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon" +
                    "&daily=temperature_2m_max,temperature_2m_min" +
                    "&timezone=auto"

                try {
                    val client = HttpClient()
                    val httpResponse = client.get(url)
                    val body = httpResponse.bodyAsText()
                    val statusCode = httpResponse.status.value
                    client.close()

                    WeatherForecastState._errorMessage.value = null
                    mapOf(
                        "statusCode" to statusCode,
                        "body" to body
                    )
                } catch (e: Exception) {
                    WeatherForecastState._errorMessage.value = "Network error: ${e.message}"
                    WeatherForecastState._isLoading.value = false
                    mapOf(
                        "statusCode" to 0,
                        "body" to ""
                    )
                }
            }
        )
    }
}
