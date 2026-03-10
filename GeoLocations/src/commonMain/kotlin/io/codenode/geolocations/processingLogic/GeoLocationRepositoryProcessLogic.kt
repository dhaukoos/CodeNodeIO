package io.codenode.geolocations.processingLogic

import io.codenode.fbpdsl.runtime.In3AnyOut2TickBlock
import io.codenode.fbpdsl.runtime.ProcessResult2
import io.codenode.persistence.GeoLocationEntity
import io.codenode.persistence.GeoLocationRepository
import io.codenode.geolocations.GeoLocationsPersistence

/**
 * Tick function for the GeoLocationRepository node.
 *
 * Node type: Processor (3 any-inputs, 2 outputs)
 *
 * Inputs:
 *   - save: GeoLocationEntity (or Unit sentinel for no-op)
 *   - update: GeoLocationEntity (or Unit sentinel for no-op)
 *   - remove: GeoLocationEntity (or Unit sentinel for no-op)
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

val geoLocationRepositoryTick: In3AnyOut2TickBlock<Any, Any, Any, Any, Any> = { save, update, remove ->
    try {
        val repo = GeoLocationRepository(GeoLocationsPersistence.dao)
        val isFreshSave = save is GeoLocationEntity && save !== lastSaveRef
        val isFreshUpdate = update is GeoLocationEntity && update !== lastUpdateRef
        val isFreshRemove = remove is GeoLocationEntity && remove !== lastRemoveRef

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
