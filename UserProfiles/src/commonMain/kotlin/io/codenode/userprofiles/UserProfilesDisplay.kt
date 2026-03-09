/*
 * UserProfilesDisplay Sink Node
 * Extracted from UserProfilesFlow for better componentization
 * License: Apache 2.0
 */

package io.codenode.userprofiles

import io.codenode.fbpdsl.model.CodeNodeFactory

/**
 * Creates the UserProfilesDisplay sink node.
 *
 * This sink node receives result and error values from the UserProfileRepository
 * processor and updates the corresponding StateFlows in UserProfilesState.
 */
internal fun createUserProfilesDisplay() = CodeNodeFactory.createSinkIn2<Any, Any>(
    name = "UserProfilesDisplay",
    consume = { result, error ->
        UserProfilesState._result.value = result
        UserProfilesState._error.value = error
    }
)
