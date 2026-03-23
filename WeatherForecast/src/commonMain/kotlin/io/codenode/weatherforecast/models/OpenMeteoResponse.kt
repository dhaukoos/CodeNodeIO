/*
 * OpenMeteoResponse - Serializable data classes for the Open-Meteo API JSON response
 * License: Apache 2.0
 */

package io.codenode.weatherforecast.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val daily: DailyData = DailyData(),
    @SerialName("daily_units")
    val dailyUnits: DailyUnits = DailyUnits()
)

@Serializable
data class DailyData(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m_max")
    val temperatureMax: List<Double> = emptyList(),
    @SerialName("temperature_2m_min")
    val temperatureMin: List<Double> = emptyList()
)

@Serializable
data class DailyUnits(
    @SerialName("temperature_2m_max")
    val temperatureMax: String = "°C",
    @SerialName("temperature_2m_min")
    val temperatureMin: String = "°C"
)
