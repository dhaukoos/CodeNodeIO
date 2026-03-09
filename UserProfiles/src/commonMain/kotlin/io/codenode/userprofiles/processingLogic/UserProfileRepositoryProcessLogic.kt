package io.codenode.userprofiles.processingLogic

import io.codenode.fbpdsl.runtime.In3AnyOut2ProcessBlock
import io.codenode.fbpdsl.runtime.ProcessResult2
import io.codenode.userprofiles.persistence.UserProfileEntity
import io.codenode.userprofiles.persistence.UserProfileRepository
import io.codenode.userprofiles.UserProfilesPersistence

/**
 * Tick function for the UserProfileRepository node.
 *
 * Node type: Processor (3 any-inputs, 2 outputs)
 *
 * Inputs:
 *   - save: UserProfileEntity (or Unit sentinel for no-op)
 *   - update: UserProfileEntity (or Unit sentinel for no-op)
 *   - remove: UserProfileEntity (or Unit sentinel for no-op)
 *
 * Outputs:
 *   - result: Any (success message)
 *   - error: Any (error message)
 *
 * Uses In3AnyOut2 (fires on any input). Per-channel identity tracking
 * prevents stale cached values from re-triggering operations.
 */
private var lastSaveRef: Any? = null
private var lastUpdateRef: Any? = null
private var lastRemoveRef: Any? = null

val userProfileRepositoryTick: In3AnyOut2ProcessBlock<Any, Any, Any, Any, Any> = { save, update, remove ->
    try {
        val repo = UserProfileRepository(UserProfilesPersistence.dao)
        val isFreshSave = save is UserProfileEntity && save !== lastSaveRef
        val isFreshUpdate = update is UserProfileEntity && update !== lastUpdateRef
        val isFreshRemove = remove is UserProfileEntity && remove !== lastRemoveRef

        when {
            isFreshSave -> {
                lastSaveRef = save
                repo.save(save)
                ProcessResult2.first("Saved: ${save.name}")
            }
            isFreshUpdate -> {
                lastUpdateRef = update
                repo.update(update)
                ProcessResult2.first("Updated: ${update.name}")
            }
            isFreshRemove -> {
                lastRemoveRef = remove
                repo.remove(remove)
                ProcessResult2.first("Removed: ${remove.name}")
            }
            else -> ProcessResult2(null, null)
        }
    } catch (e: Exception) {
        ProcessResult2.second("Error: ${e.message}")
    }
}
