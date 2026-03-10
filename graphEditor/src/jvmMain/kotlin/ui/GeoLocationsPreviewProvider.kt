/*
 * GeoLocationsPreviewProvider - Provides GeoLocations preview composables for the runtime panel
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.geolocations.GeoLocationsViewModel
import io.codenode.geolocations.userInterface.GeoLocations

/**
 * Provides preview composables that render GeoLocations components,
 * driven by the RuntimeSession's ViewModel state.
 */
object GeoLocationsPreviewProvider {

    /**
     * Registers GeoLocations preview composables with the PreviewRegistry.
     */
    fun register() {
        PreviewRegistry.register("GeoLocations") { viewModel, modifier ->
            val vm = viewModel as GeoLocationsViewModel
            GeoLocations(viewModel = vm, modifier = modifier)
        }
    }
}
