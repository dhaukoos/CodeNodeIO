/*
 * ForecastDisplayCodeNode - Sink node that updates WeatherForecastState with display data
 * License: Apache 2.0
 */

package io.codenode.weatherforecast.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.weatherforecast.WeatherForecastState
import io.codenode.weatherforecast.models.ChartData
import io.codenode.weatherforecast.models.ForecastEntry

/**
 * Sink node with 2 inputs (anyInput mode) that receives:
 *   - input1: ForecastDisplayList (formatted entries for list view)
 *   - input2: ForecastChartData (arrays for chart rendering)
 *
 * Updates WeatherForecastState with the received data for UI display.
 */
object ForecastDisplayCodeNode : CodeNodeDefinition {
    override val name = "ForecastDisplay"
    override val category = CodeNodeType.SINK
    override val description = "Updates UI state with forecast display list and chart data"
    override val inputPorts = listOf(
        PortSpec("displayList", Any::class),
        PortSpec("chartData", Any::class)
    )
    override val outputPorts = emptyList<PortSpec>()
    override val anyInput = true

    @Suppress("UNCHECKED_CAST")
    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createSinkIn2Any<Any, Any>(
            name = name,
            initialValue1 = emptyMap<String, Any>(),
            initialValue2 = emptyMap<String, Any>(),
            consume = { displayListData, chartDataData ->
                // Process display list
                val displayMap = displayListData as? Map<*, *>
                val entries = displayMap?.get("entries") as? List<ForecastEntry>
                if (entries != null && entries.isNotEmpty()) {
                    WeatherForecastState._forecastEntries.value = entries
                }

                // Process chart data
                val chartMap = chartDataData as? Map<*, *>
                val labels = chartMap?.get("labels") as? List<String>
                val values = chartMap?.get("values") as? List<Double>
                val unit = chartMap?.get("unit") as? String ?: "°C"
                if (labels != null && values != null && values.isNotEmpty()) {
                    WeatherForecastState._chartData.value = ChartData(
                        labels = labels,
                        values = values,
                        unit = unit,
                        minValue = values.minOrNull() ?: 0.0,
                        maxValue = values.maxOrNull() ?: 0.0
                    )
                }

                // Clear loading state
                WeatherForecastState._isLoading.value = false
                WeatherForecastState._errorMessage.value = null
            }
        )
    }
}
