/*
 * AddressesPreviewProvider - Provides Addresses preview composables for the runtime panel
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.addresses.AddressesViewModel
import io.codenode.addresses.userInterface.Addresses

/**
 * Provides preview composables that render Addresses components,
 * driven by the RuntimeSession's ViewModel state.
 */
object AddressesPreviewProvider {

    /**
     * Registers Addresses preview composables with the PreviewRegistry.
     */
    fun register() {
        PreviewRegistry.register("Addresses") { viewModel, modifier ->
            val vm = viewModel as AddressesViewModel
            Addresses(viewModel = vm, modifier = modifier)
        }
    }
}
