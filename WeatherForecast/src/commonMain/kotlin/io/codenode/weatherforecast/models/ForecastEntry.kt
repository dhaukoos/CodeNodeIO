/*
 * ForecastEntry - A single day's forecast formatted for list display
 * License: Apache 2.0
 */

package io.codenode.weatherforecast.models

data class ForecastEntry(
    val date: String,
    val high: Double,
    val low: Double,
    val unit: String
)
