/*
 * EdgeArtFilterPreviewProvider - Provides EdgeArtFilter preview composable for the runtime panel
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.edgeartfilter.EdgeArtFilterViewModel
import io.codenode.edgeartfilter.userInterface.EdgeArtFilter

/**
 * Provides preview composables that render EdgeArtFilter components,
 * driven by the RuntimeSession's ViewModel state.
 */
object EdgeArtFilterPreviewProvider {

    /**
     * Registers EdgeArtFilter preview composables with the PreviewRegistry.
     */
    fun register() {
        PreviewRegistry.register("EdgeArtFilter") { viewModel, modifier ->
            val vm = viewModel as EdgeArtFilterViewModel
            EdgeArtFilter(viewModel = vm, modifier = modifier)
        }
    }
}
