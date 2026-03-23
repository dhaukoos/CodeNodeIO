/*
 * ChartData - Chart-ready data structure for temperature line chart
 * License: Apache 2.0
 */

package io.codenode.weatherforecast.models

data class ChartData(
    val labels: List<String>,
    val values: List<Double>,
    val unit: String,
    val minValue: Double,
    val maxValue: Double
)
