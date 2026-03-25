/*
 * GeoLocationRepositoryCodeNode - Self-contained processor node for CRUD persistence
 * License: Apache 2.0
 */

package io.codenode.geolocations.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.fbpdsl.runtime.ProcessResult2
import io.codenode.geolocations.iptypes.GeoLocation
import io.codenode.geolocations.iptypes.toEntity
import io.codenode.persistence.GeoLocationRepository
import io.codenode.geolocations.GeoLocationsPersistence

/**
 * Processor node that performs CRUD operations on geo locations.
 *
 * Uses In3AnyOut2 pattern (fires on ANY input change). Per-channel identity
 * tracking prevents stale cached values from re-triggering operations.
 * Accesses DAO via GeoLocationsPersistence (Koin).
 */
object GeoLocationRepositoryCodeNode : CodeNodeDefinition {
    override val name = "GeoLocationRepository"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Performs save, update, and remove operations on geo locations"
    override val inputPorts = listOf(
        PortSpec("save", GeoLocation::class),
        PortSpec("update", GeoLocation::class),
        PortSpec("remove", GeoLocation::class)
    )
    override val outputPorts = listOf(
        PortSpec("result", String::class),
        PortSpec("error", String::class)
    )
    override val anyInput = true

    override fun createRuntime(name: String): NodeRuntime {
        var lastSaveRef: GeoLocation? = null
        var lastUpdateRef: GeoLocation? = null
        var lastRemoveRef: GeoLocation? = null

        return CodeNodeFactory.createIn3AnyOut2Processor<GeoLocation, GeoLocation, GeoLocation, String, String>(
            name = name,
            initialValue1 = GeoLocation(name = "", lat = 0.0, lon = 0.0),
            initialValue2 = GeoLocation(name = "", lat = 0.0, lon = 0.0),
            initialValue3 = GeoLocation(name = "", lat = 0.0, lon = 0.0),
            process = { save, update, remove ->
                try {
                    val repo = GeoLocationRepository(GeoLocationsPersistence.dao)
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
