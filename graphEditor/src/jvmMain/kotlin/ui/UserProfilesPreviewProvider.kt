/*
 * UserProfilesPreviewProvider - Provides UserProfiles preview composables for the runtime panel
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.userprofiles.UserProfilesViewModel
import io.codenode.userprofiles.userInterface.UserProfiles

/**
 * Provides preview composables that render UserProfiles components,
 * driven by the RuntimeSession's ViewModel state.
 */
object UserProfilesPreviewProvider {

    /**
     * Registers UserProfiles preview composables with the PreviewRegistry.
     */
    fun register() {
        PreviewRegistry.register("UserProfiles") { viewModel, modifier ->
            val vm = viewModel as UserProfilesViewModel
            UserProfiles(viewModel = vm, modifier = modifier)
        }
    }
}
