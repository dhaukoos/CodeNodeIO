/*
 * GeoLocationRepositoryCodeNode - Self-contained processor node for CRUD persistence
 * License: Apache 2.0
 */

package io.codenode.geolocations.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeCategory
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.fbpdsl.runtime.ProcessResult2
import io.codenode.persistence.GeoLocationEntity
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
    override val category = NodeCategory.PROCESSOR
    override val description = "Performs save, update, and remove operations on geo locations"
    override val inputPorts = listOf(
        PortSpec("save", Any::class),
        PortSpec("update", Any::class),
        PortSpec("remove", Any::class)
    )
    override val outputPorts = listOf(
        PortSpec("result", Any::class),
        PortSpec("error", Any::class)
    )
    override val anyInput = true

    override fun createRuntime(name: String): NodeRuntime {
        var lastSaveRef: Any? = null
        var lastUpdateRef: Any? = null
        var lastRemoveRef: Any? = null

        return CodeNodeFactory.createIn3AnyOut2Processor<Any, Any, Any, Any, Any>(
            name = name,
            initialValue1 = Unit,
            initialValue2 = Unit,
            initialValue3 = Unit,
            process = { save, update, remove ->
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
        )
    }
}
