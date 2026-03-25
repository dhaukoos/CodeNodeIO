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
import io.codenode.weatherforecast.iptypes.Coordinates
import io.codenode.weatherforecast.iptypes.HttpResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

/**
 * Transformer node that receives Coordinates, constructs an Open-Meteo API URL,
 * performs an HTTPS GET request using Ktor Client, and outputs an HttpResponse.
 */
object HttpFetcherCodeNode : CodeNodeDefinition {
    override val name = "HttpFetcher"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Fetches weather forecast data from Open-Meteo via HTTPS GET"
    override val inputPorts = listOf(PortSpec("coordinates", Coordinates::class))
    override val outputPorts = listOf(PortSpec("response", HttpResponse::class))

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createContinuousTransformer<Coordinates, HttpResponse>(
            name = name,
            transform = { input ->
                val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=${input.latitude}&longitude=${input.longitude}" +
                    "&daily=temperature_2m_max,temperature_2m_min" +
                    "&timezone=auto"

                try {
                    val client = HttpClient()
                    val httpResponse = client.get(url)
                    val body = httpResponse.bodyAsText()
                    val statusCode = httpResponse.status.value
                    client.close()

                    if (statusCode != 200) {
                        WeatherForecastState._errorMessage.value =
                            "HTTP $statusCode: ${httpResponse.status.description}"
                        WeatherForecastState._isLoading.value = false
                    } else {
                        WeatherForecastState._errorMessage.value = null
                    }

                    HttpResponse(statusCode = statusCode, body = body)
                } catch (e: Exception) {
                    val message = when {
                        e.message?.contains("UnknownHost", ignoreCase = true) == true ->
                            "No internet connection or invalid hostname"
                        e.message?.contains("timeout", ignoreCase = true) == true ->
                            "Request timed out — try again"
                        e.message?.contains("Connection refused", ignoreCase = true) == true ->
                            "Connection refused by server"
                        else -> "Network error: ${e.message}"
                    }
                    WeatherForecastState._errorMessage.value = message
                    WeatherForecastState._isLoading.value = false
                    HttpResponse(statusCode = 0, body = "")
                }
            }
        )
    }
}
