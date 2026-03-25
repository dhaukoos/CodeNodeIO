/*
 * UserProfileCUDCodeNode - Self-contained source node for CRUD operations
 * License: Apache 2.0
 */

package io.codenode.userprofiles.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.fbpdsl.runtime.ProcessResult3
import io.codenode.userprofiles.UserProfilesState
import io.codenode.userprofiles.iptypes.UserProfile
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Source node that emits save, update, and remove operations from UserProfilesState.
 *
 * Collects from the _save, _update, and _remove StateFlows independently
 * (each in its own coroutine). When a non-null value arrives, emits a
 * selective ProcessResult3 and resets the state to null.
 */
object UserProfileCUDCodeNode : CodeNodeDefinition {
    override val name = "UserProfileCUD"
    override val category = CodeNodeType.SOURCE
    override val description = "Emits save, update, and remove operations for user profiles"
    override val inputPorts = emptyList<PortSpec>()
    override val outputPorts = listOf(
        PortSpec("save", UserProfile::class),
        PortSpec("update", UserProfile::class),
        PortSpec("remove", UserProfile::class)
    )

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createSourceOut3<UserProfile, UserProfile, UserProfile>(
            name = name,
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
    }
}
