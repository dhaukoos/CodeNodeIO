package io.codenode.userprofiles.processingLogic

import io.codenode.fbpdsl.runtime.In3AnyOut2ProcessBlock
import io.codenode.fbpdsl.runtime.ProcessResult2
import io.codenode.userprofiles.persistence.DatabaseModule
import io.codenode.userprofiles.persistence.UserProfileEntity
import io.codenode.userprofiles.persistence.UserProfileRepository

/**
 * Tick function for the UserProfileRepository node.
 *
 * Node type: Processor (3 inputs, 2 outputs)
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
 * Dispatches to save/update/remove based on which input is a real value
 * (not the Unit sentinel). Only one operation should be non-Unit per invocation.
 */
val userProfileRepositoryTick: In3AnyOut2ProcessBlock<Any, Any, Any, Any, Any> = { save, update, remove ->
    try {
        val repo = UserProfileRepository(DatabaseModule.getDatabase().userProfileDao())
        when {
            save is UserProfileEntity -> {
                repo.save(save)
                ProcessResult2.first("Saved: ${save.name}")
            }
            update is UserProfileEntity -> {
                repo.update(update)
                ProcessResult2.first("Updated: ${update.name}")
            }
            remove is UserProfileEntity -> {
                repo.remove(remove)
                ProcessResult2.first("Removed: ${remove.name}")
            }
            else -> ProcessResult2(null, null) // no-op (Unit sentinel values)
        }
    } catch (e: Exception) {
        ProcessResult2.second("Error: ${e.message}")
    }
}
