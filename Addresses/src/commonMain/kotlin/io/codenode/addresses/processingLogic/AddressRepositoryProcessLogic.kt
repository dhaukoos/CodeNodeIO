package io.codenode.addresses.processingLogic

import io.codenode.fbpdsl.runtime.In3AnyOut2TickBlock
import io.codenode.fbpdsl.runtime.ProcessResult2
import io.codenode.persistence.AddressEntity
import io.codenode.persistence.AddressRepository
import io.codenode.addresses.AddressesPersistence

/**
 * Tick function for the AddressRepository node.
 *
 * Node type: Processor (3 any-inputs, 2 outputs)
 *
 * Inputs:
 *   - save: AddressEntity (or Unit sentinel for no-op)
 *   - update: AddressEntity (or Unit sentinel for no-op)
 *   - remove: AddressEntity (or Unit sentinel for no-op)
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

val addressRepositoryTick: In3AnyOut2TickBlock<Any, Any, Any, Any, Any> = { save, update, remove ->
    try {
        val repo = AddressRepository(AddressesPersistence.dao)
        val isFreshSave = save is AddressEntity && save !== lastSaveRef
        val isFreshUpdate = update is AddressEntity && update !== lastUpdateRef
        val isFreshRemove = remove is AddressEntity && remove !== lastRemoveRef

        when {
            isFreshSave -> {
                lastSaveRef = save
                repo.save(save)
                ProcessResult2.first("Saved: ${save.id}")
            }
            isFreshUpdate -> {
                lastUpdateRef = update
                repo.update(update)
                ProcessResult2.first("Updated: ${update.id}")
            }
            isFreshRemove -> {
                lastRemoveRef = remove
                repo.remove(remove)
                ProcessResult2.first("Removed: ${remove.id}")
            }
            else -> ProcessResult2(null, null)
        }
    } catch (e: Exception) {
        ProcessResult2.second("Error: ${e.message}")
    }
}
