/*
 * ForecastData - IP type for parsed forecast data from Open-Meteo
 * License: Apache 2.0
 */

package io.codenode.weatherforecast.models

data class ForecastData(
    val dates: List<String>,
    val maxTemperatures: List<Double>,
    val minTemperatures: List<Double>,
    val temperatureUnit: String
)
