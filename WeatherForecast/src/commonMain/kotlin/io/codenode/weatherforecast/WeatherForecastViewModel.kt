/*
 * WeatherForecastViewModel
 * ViewModel for the WeatherForecast composable
 * License: Apache 2.0
 */

package io.codenode.weatherforecast

import androidx.lifecycle.ViewModel
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.weatherforecast.generated.WeatherForecastControllerInterface
import io.codenode.weatherforecast.models.ChartData
import io.codenode.weatherforecast.models.ForecastEntry
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for the WeatherForecast composable.
 * Bridges FlowGraph domain logic with Compose UI.
 *
 * @param controller The WeatherForecastControllerInterface that manages FlowGraph execution
 */
class WeatherForecastViewModel(
    private val controller: WeatherForecastControllerInterface
) : ViewModel() {

    // Observable state from module properties
    val forecastEntries: StateFlow<List<ForecastEntry>> = WeatherForecastState.forecastEntriesFlow
    val chartData: StateFlow<ChartData?> = WeatherForecastState.chartDataFlow
    val errorMessage: StateFlow<String?> = WeatherForecastState.errorMessageFlow
    val isLoading: StateFlow<Boolean> = WeatherForecastState.isLoadingFlow
    val latitude: StateFlow<Double> = WeatherForecastState.latitudeFlow
    val longitude: StateFlow<Double> = WeatherForecastState.longitudeFlow

    // Execution state from controller
    val executionState: StateFlow<ExecutionState> = controller.executionState

    // Control methods
    fun start(): FlowGraph = controller.start()

    fun stop(): FlowGraph = controller.stop()

    fun reset(): FlowGraph = controller.reset()

    fun pause(): FlowGraph = controller.pause()

    fun resume(): FlowGraph = controller.resume()

    /**
     * Triggers a refresh of the forecast data.
     * Sets isLoading to true, which the TriggerSource node observes
     * to emit new coordinates and start the pipeline.
     */
    fun refresh() {
        WeatherForecastState._isLoading.value = true
    }

    /**
     * Updates the latitude for the next forecast fetch.
     */
    fun setLatitude(lat: Double) {
        WeatherForecastState._latitude.value = lat
    }

    /**
     * Updates the longitude for the next forecast fetch.
     */
    fun setLongitude(lon: Double) {
        WeatherForecastState._longitude.value = lon
    }
}
