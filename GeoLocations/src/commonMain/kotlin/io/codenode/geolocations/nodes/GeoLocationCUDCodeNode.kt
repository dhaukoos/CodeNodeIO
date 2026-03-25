/*
 * GeoLocationCUDCodeNode - Self-contained source node for CRUD operations
 * License: Apache 2.0
 */

package io.codenode.geolocations.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.fbpdsl.runtime.ProcessResult3
import io.codenode.geolocations.GeoLocationsState
import io.codenode.geolocations.iptypes.GeoLocation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Source node that emits save, update, and remove operations from GeoLocationsState.
 *
 * Collects from the _save, _update, and _remove StateFlows independently.
 * When a non-null value arrives, emits a selective ProcessResult3 and resets
 * the state to null.
 */
object GeoLocationCUDCodeNode : CodeNodeDefinition {
    override val name = "GeoLocationCUD"
    override val category = CodeNodeType.SOURCE
    override val description = "Emits save, update, and remove operations for geo locations"
    override val inputPorts = emptyList<PortSpec>()
    override val outputPorts = listOf(
        PortSpec("save", GeoLocation::class),
        PortSpec("update", GeoLocation::class),
        PortSpec("remove", GeoLocation::class)
    )

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createSourceOut3<GeoLocation, GeoLocation, GeoLocation>(
            name = name,
            generate = { emit ->
                coroutineScope {
                    launch {
                        GeoLocationsState._save.drop(1).collect { save ->
                            if (save != null) {
                                emit(ProcessResult3(save, null, null))
                                GeoLocationsState._save.value = null
                            }
                        }
                    }
                    launch {
                        GeoLocationsState._update.drop(1).collect { update ->
                            if (update != null) {
                                emit(ProcessResult3(null, update, null))
                                GeoLocationsState._update.value = null
                            }
                        }
                    }
                    launch {
                        GeoLocationsState._remove.drop(1).collect { remove ->
                            if (remove != null) {
                                emit(ProcessResult3(null, null, remove))
                                GeoLocationsState._remove.value = null
                            }
                        }
                    }
                }
            }
        )
    }
}
