/*
 * ForecastDisplayList - Custom IP Type
 * @IPType
 * @TypeName ForecastDisplayList
 * @TypeId ip_forecastdisplaylist
 * @Color rgb(76, 175, 80)
 * License: Apache 2.0
 */

package io.codenode.weatherforecast.iptypes

data class ForecastDisplayList(
    val entries: List<ForecastEntry>
)
