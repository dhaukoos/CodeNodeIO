/*
 * UserProfileRepositoryCodeNode - Self-contained processor node for CRUD persistence
 * License: Apache 2.0
 */

package io.codenode.userprofiles.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.fbpdsl.runtime.ProcessResult2
import io.codenode.persistence.UserProfileRepository
import io.codenode.userprofiles.UserProfilesPersistence
import io.codenode.userprofiles.iptypes.UserProfile
import io.codenode.userprofiles.iptypes.toEntity

/**
 * Processor node that performs CRUD operations on user profiles.
 *
 * Uses In3AnyOut2 pattern (fires on ANY input change). Per-channel identity
 * tracking prevents stale cached values from re-triggering operations.
 * Accesses DAO via UserProfilesPersistence (Koin).
 */
object UserProfileRepositoryCodeNode : CodeNodeDefinition {
    override val name = "UserProfileRepository"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Performs save, update, and remove operations on user profiles"
    override val inputPorts = listOf(
        PortSpec("save", UserProfile::class),
        PortSpec("update", UserProfile::class),
        PortSpec("remove", UserProfile::class)
    )
    override val outputPorts = listOf(
        PortSpec("result", String::class),
        PortSpec("error", String::class)
    )
    override val anyInput = true

    override fun createRuntime(name: String): NodeRuntime {
        var lastSaveRef: UserProfile? = null
        var lastUpdateRef: UserProfile? = null
        var lastRemoveRef: UserProfile? = null

        return CodeNodeFactory.createIn3AnyOut2Processor<UserProfile, UserProfile, UserProfile, String, String>(
            name = name,
            initialValue1 = UserProfile(name = "", isActive = false),
            initialValue2 = UserProfile(name = "", isActive = false),
            initialValue3 = UserProfile(name = "", isActive = false),
            process = { save, update, remove ->
                try {
                    val repo = UserProfileRepository(UserProfilesPersistence.dao)
                    val isFreshSave = save !== lastSaveRef && save.name.isNotEmpty()
                    val isFreshUpdate = update !== lastUpdateRef && update.name.isNotEmpty()
                    val isFreshRemove = remove !== lastRemoveRef && remove.name.isNotEmpty()

                    when {
                        isFreshSave -> {
                            lastSaveRef = save
                            repo.save(save.toEntity())
                            ProcessResult2.first("Saved: ${save.name}")
                        }
                        isFreshUpdate -> {
                            lastUpdateRef = update
                            repo.update(update.toEntity())
                            ProcessResult2.first("Updated: ${update.name}")
                        }
                        isFreshRemove -> {
                            lastRemoveRef = remove
                            repo.remove(remove.toEntity())
                            ProcessResult2.first("Removed: ${remove.name}")
                        }
                        else -> ProcessResult2(null, null)
                    }
                } catch (e: Exception) {
                    ProcessResult2.second("Error: ${e.message}")
                }
            }
        )
    }
}
