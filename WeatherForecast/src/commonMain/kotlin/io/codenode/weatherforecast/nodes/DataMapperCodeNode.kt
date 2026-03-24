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
import io.codenode.weatherforecast.models.ChartData
import io.codenode.weatherforecast.models.ForecastData
import io.codenode.weatherforecast.models.ForecastDisplayList
import io.codenode.weatherforecast.models.ForecastEntry

/**
 * Transformer node with 1 input and 2 outputs (fan-out).
 * Receives ForecastData, formats it into:
 *   - output1: ForecastDisplayList (list of formatted entries for list view)
 *   - output2: ChartData (arrays for chart rendering)
 */
object DataMapperCodeNode : CodeNodeDefinition {
    override val name = "DataMapper"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Maps forecast data to display list and chart data formats"
    override val inputPorts = listOf(PortSpec("forecastData", ForecastData::class))
    override val outputPorts = listOf(
        PortSpec("displayList", ForecastDisplayList::class),
        PortSpec("chartData", ChartData::class)
    )

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createIn1Out2Processor<ForecastData, ForecastDisplayList, ChartData>(
            name = name,
            process = { input ->
                // Build display entries
                val entries = input.dates.indices.map { i ->
                    ForecastEntry(
                        date = formatDate(input.dates.getOrElse(i) { "" }),
                        high = input.maxTemperatures.getOrElse(i) { 0.0 },
                        low = input.minTemperatures.getOrElse(i) { 0.0 },
                        unit = input.temperatureUnit
                    )
                }

                // Build chart labels (short day names)
                val chartLabels = input.dates.map { formatShortDate(it) }

                val displayList = ForecastDisplayList(entries = entries)
                val chartData = ChartData(
                    labels = chartLabels,
                    values = input.maxTemperatures,
                    unit = input.temperatureUnit,
                    minValue = input.maxTemperatures.minOrNull() ?: 0.0,
                    maxValue = input.maxTemperatures.maxOrNull() ?: 0.0
                )

                ProcessResult2.both(displayList, chartData)
            }
        )
    }

    /** Formats "2026-03-23" to "03/23" */
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
