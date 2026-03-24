/*
 * ForecastDisplayList - IP type for formatted forecast entries ready for UI display
 * License: Apache 2.0
 */

package io.codenode.weatherforecast.models

data class ForecastDisplayList(
    val entries: List<ForecastEntry>
)
