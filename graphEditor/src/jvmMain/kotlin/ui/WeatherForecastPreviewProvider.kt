/*
 * WeatherForecastPreviewProvider - Provides WeatherForecast preview composable for the runtime panel
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.weatherforecast.WeatherForecastViewModel
import io.codenode.weatherforecast.userInterface.WeatherForecastUI

/**
 * Provides preview composables that render WeatherForecast components,
 * driven by the RuntimeSession's ViewModel state.
 */
object WeatherForecastPreviewProvider {

    /**
     * Registers WeatherForecast preview composables with the PreviewRegistry.
     */
    fun register() {
        PreviewRegistry.register("WeatherForecast") { viewModel, modifier ->
            val vm = viewModel as WeatherForecastViewModel
            WeatherForecastUI(viewModel = vm, modifier = modifier)
        }
    }
}
