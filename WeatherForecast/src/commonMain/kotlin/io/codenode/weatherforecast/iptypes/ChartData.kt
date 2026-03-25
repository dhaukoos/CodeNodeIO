/*
 * ChartData - Custom IP Type
 * @IPType
 * @TypeName ChartData
 * @TypeId ip_forecastchartdata
 * @Color rgb(156, 39, 176)
 * License: Apache 2.0
 */

package io.codenode.weatherforecast.iptypes

data class ChartData(
    val labels: List<String>,
    val values: List<Double>,
    val unit: String,
    val minValue: Double,
    val maxValue: Double
)
