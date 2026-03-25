/*
 * ForecastData - Custom IP Type
 * @IPType
 * @TypeName ForecastData
 * @TypeId ip_forecastdata
 * @Color rgb(33, 150, 243)
 * License: Apache 2.0
 */

package io.codenode.weatherforecast.iptypes

data class ForecastData(
    val dates: List<String>,
    val maxTemperatures: List<Double>,
    val minTemperatures: List<Double>,
    val temperatureUnit: String
)
