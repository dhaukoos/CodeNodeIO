/*
 * UserProfileCUD Source Node
 * Extracted from UserProfilesFlow for better componentization
 * License: Apache 2.0
 */

package io.codenode.userprofiles

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.ProcessResult3
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Creates the UserProfileCUD source node.
 *
 * This source node collects from the save, update, and remove StateFlows
 * in UserProfilesState and emits them as a ProcessResult3 to downstream processors.
 */
internal fun createUserProfileCUD() = CodeNodeFactory.createSourceOut3<Any, Any, Any>(
    name = "UserProfileCUD",
    generate = { emit ->
        coroutineScope {
            launch {
                UserProfilesState._save.drop(1).collect { save ->
                    if (save != null) {
                        emit(ProcessResult3(save, null, null))
                        UserProfilesState._save.value = null
                    }
                }
            }
            launch {
                UserProfilesState._update.drop(1).collect { update ->
                    if (update != null) {
                        emit(ProcessResult3(null, update, null))
                        UserProfilesState._update.value = null
                    }
                }
            }
            launch {
                UserProfilesState._remove.drop(1).collect { remove ->
                    if (remove != null) {
                        emit(ProcessResult3(null, null, remove))
                        UserProfilesState._remove.value = null
                    }
                }
            }
        }
    }
)
