/*
 * DataMapperCodeNode - Transformer node that maps forecast data to display and chart formats
 * License: Apache 2.0
 */

package io.codenode.weatherforecast.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.fbpdsl.runtime.ProcessResult2
import io.codenode.weatherforecast.models.ForecastEntry

/**
 * Transformer node with 1 input and 2 outputs (fan-out).
 * Receives ForecastData map, formats it into:
 *   - output1: ForecastDisplayList (list of formatted entries for list view)
 *   - output2: ForecastChartData (arrays for chart rendering)
 */
object DataMapperCodeNode : CodeNodeDefinition {
    override val name = "DataMapper"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Maps forecast data to display list and chart data formats"
    override val inputPorts = listOf(PortSpec("forecastData", Any::class))
    override val outputPorts = listOf(
        PortSpec("displayList", Any::class),
        PortSpec("chartData", Any::class)
    )

    @Suppress("UNCHECKED_CAST")
    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createIn1Out2Processor<Any, Any, Any>(
            name = name,
            process = { input ->
                val data = input as Map<*, *>
                val dates = data["dates"] as? List<String> ?: emptyList()
                val maxTemps = data["maxTemperatures"] as? List<Double> ?: emptyList()
                val minTemps = data["minTemperatures"] as? List<Double> ?: emptyList()
                val unit = data["temperatureUnit"] as? String ?: "°C"

                // Build display entries
                val entries = dates.indices.map { i ->
                    ForecastEntry(
                        date = formatDate(dates.getOrElse(i) { "" }),
                        high = maxTemps.getOrElse(i) { 0.0 },
                        low = minTemps.getOrElse(i) { 0.0 },
                        unit = unit
                    )
                }

                // Build chart labels (short day names)
                val chartLabels = dates.map { dateStr ->
                    formatShortDate(dateStr)
                }

                val displayList = mapOf("entries" to entries)
                val chartData = mapOf(
                    "labels" to chartLabels,
                    "values" to maxTemps,
                    "unit" to unit
                )

                ProcessResult2.both(displayList, chartData)
            }
        )
    }

    /** Formats "2026-03-23" to "Mon 03/23" */
    private fun formatDate(dateStr: String): String {
        if (dateStr.length < 10) return dateStr
        val month = dateStr.substring(5, 7)
        val day = dateStr.substring(8, 10)
        return "$month/$day"
    }

    /** Formats "2026-03-23" to "03/23" for chart labels */
    private fun formatShortDate(dateStr: String): String {
        if (dateStr.length < 10) return dateStr
        val month = dateStr.substring(5, 7)
        val day = dateStr.substring(8, 10)
        return "$month/$day"
    }
}
