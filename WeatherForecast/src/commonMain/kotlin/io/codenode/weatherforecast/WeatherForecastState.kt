/*
 * WeatherForecastState
 * Singleton state object for the WeatherForecast module
 * License: Apache 2.0
 */

package io.codenode.weatherforecast

import io.codenode.weatherforecast.iptypes.ChartData
import io.codenode.weatherforecast.iptypes.ForecastEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ===== MODULE PROPERTIES START =====
// ===== MODULE PROPERTIES END =====

object WeatherForecastState {

    internal val _forecastEntries = MutableStateFlow<List<ForecastEntry>>(emptyList())
    val forecastEntriesFlow: StateFlow<List<ForecastEntry>> = _forecastEntries.asStateFlow()

    internal val _chartData = MutableStateFlow<ChartData?>(null)
    val chartDataFlow: StateFlow<ChartData?> = _chartData.asStateFlow()

    internal val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessageFlow: StateFlow<String?> = _errorMessage.asStateFlow()

    internal val _isLoading = MutableStateFlow(false)
    val isLoadingFlow: StateFlow<Boolean> = _isLoading.asStateFlow()

    internal val _latitude = MutableStateFlow(40.16)
    val latitudeFlow: StateFlow<Double> = _latitude.asStateFlow()

    internal val _longitude = MutableStateFlow(-105.10)
    val longitudeFlow: StateFlow<Double> = _longitude.asStateFlow()

    fun reset() {
        _forecastEntries.value = emptyList()
        _chartData.value = null
        _errorMessage.value = null
        _isLoading.value = false
        _latitude.value = 40.16
        _longitude.value = -105.10
    }
}
