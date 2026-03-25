/*
 * ForecastEntry - Custom IP Type
 * @IPType
 * @TypeName ForecastEntry
 * @TypeId ip_forecastentry
 * @Color rgb(121, 85, 72)
 * License: Apache 2.0
 */

package io.codenode.weatherforecast.iptypes

data class ForecastEntry(
    val date: String,
    val high: Double,
    val low: Double,
    val unit: String
)
