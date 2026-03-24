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
import io.codenode.weatherforecast.models.ForecastDisplayList

/**
 * Sink node with 2 inputs (anyInput mode) that receives:
 *   - input1: ForecastDisplayList (formatted entries for list view)
 *   - input2: ChartData (data for chart rendering)
 *
 * Updates WeatherForecastState with the received data for UI display.
 */
object ForecastDisplayCodeNode : CodeNodeDefinition {
    override val name = "ForecastDisplay"
    override val category = CodeNodeType.SINK
    override val description = "Updates UI state with forecast display list and chart data"
    override val inputPorts = listOf(
        PortSpec("displayList", ForecastDisplayList::class),
        PortSpec("chartData", ChartData::class)
    )
    override val outputPorts = emptyList<PortSpec>()
    override val anyInput = true

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createSinkIn2Any<ForecastDisplayList, ChartData>(
            name = name,
            initialValue1 = ForecastDisplayList(entries = emptyList()),
            initialValue2 = ChartData(labels = emptyList(), values = emptyList(), unit = "°C", minValue = 0.0, maxValue = 0.0),
            consume = { displayList, chartData ->
                if (displayList.entries.isNotEmpty()) {
                    WeatherForecastState._forecastEntries.value = displayList.entries
                }

                if (chartData.values.isNotEmpty()) {
                    WeatherForecastState._chartData.value = chartData
                }

                // Clear loading state
                WeatherForecastState._isLoading.value = false
                WeatherForecastState._errorMessage.value = null
            }
        )
    }
}
