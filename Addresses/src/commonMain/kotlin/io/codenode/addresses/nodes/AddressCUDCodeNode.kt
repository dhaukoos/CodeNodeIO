/*
 * AddressCUDCodeNode - Self-contained source node for CRUD operations
 * License: Apache 2.0
 */

package io.codenode.addresses.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.fbpdsl.runtime.ProcessResult3
import io.codenode.addresses.AddressesState
import io.codenode.addresses.iptypes.Address
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Source node that emits save, update, and remove operations from AddressesState.
 *
 * Collects from the _save, _update, and _remove StateFlows independently.
 * When a non-null value arrives, emits a selective ProcessResult3 and resets
 * the state to null.
 */
object AddressCUDCodeNode : CodeNodeDefinition {
    override val name = "AddressCUD"
    override val category = CodeNodeType.SOURCE
    override val description = "Emits save, update, and remove operations for addresses"
    override val inputPorts = emptyList<PortSpec>()
    override val outputPorts = listOf(
        PortSpec("save", Address::class),
        PortSpec("update", Address::class),
        PortSpec("remove", Address::class)
    )

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createSourceOut3<Address, Address, Address>(
            name = name,
            generate = { emit ->
                coroutineScope {
                    launch {
                        AddressesState._save.drop(1).collect { save ->
                            if (save != null) {
                                emit(ProcessResult3(save, null, null))
                                AddressesState._save.value = null
                            }
                        }
                    }
                    launch {
                        AddressesState._update.drop(1).collect { update ->
                            if (update != null) {
                                emit(ProcessResult3(null, update, null))
                                AddressesState._update.value = null
                            }
                        }
                    }
                    launch {
                        AddressesState._remove.drop(1).collect { remove ->
                            if (remove != null) {
                                emit(ProcessResult3(null, null, remove))
                                AddressesState._remove.value = null
                            }
                        }
                    }
                }
            }
        )
    }
}
