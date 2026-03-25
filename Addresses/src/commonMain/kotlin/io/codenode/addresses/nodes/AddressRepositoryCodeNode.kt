/*
 * AddressRepositoryCodeNode - Self-contained processor node for CRUD persistence
 * License: Apache 2.0
 */

package io.codenode.addresses.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.fbpdsl.runtime.ProcessResult2
import io.codenode.addresses.iptypes.Address
import io.codenode.addresses.iptypes.toEntity
import io.codenode.persistence.AddressRepository
import io.codenode.addresses.AddressesPersistence

/**
 * Processor node that performs CRUD operations on addresses.
 *
 * Uses In3AnyOut2 pattern (fires on ANY input change). Per-channel identity
 * tracking prevents stale cached values from re-triggering operations.
 * Accesses DAO via AddressesPersistence (Koin).
 */
object AddressRepositoryCodeNode : CodeNodeDefinition {
    override val name = "AddressRepository"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Performs save, update, and remove operations on addresses"
    override val inputPorts = listOf(
        PortSpec("save", Address::class),
        PortSpec("update", Address::class),
        PortSpec("remove", Address::class)
    )
    override val outputPorts = listOf(
        PortSpec("result", String::class),
        PortSpec("error", String::class)
    )
    override val anyInput = true

    override fun createRuntime(name: String): NodeRuntime {
        var lastSaveRef: Address? = null
        var lastUpdateRef: Address? = null
        var lastRemoveRef: Address? = null

        return CodeNodeFactory.createIn3AnyOut2Processor<Address, Address, Address, String, String>(
            name = name,
            initialValue1 = Address(street = "", city = "", state = "", zip = 0),
            initialValue2 = Address(street = "", city = "", state = "", zip = 0),
            initialValue3 = Address(street = "", city = "", state = "", zip = 0),
            process = { save, update, remove ->
                try {
                    val repo = AddressRepository(AddressesPersistence.dao)
                    val isFreshSave = save !== lastSaveRef && save.street.isNotEmpty()
                    val isFreshUpdate = update !== lastUpdateRef && update.street.isNotEmpty()
                    val isFreshRemove = remove !== lastRemoveRef && remove.street.isNotEmpty()

                    when {
                        isFreshSave -> {
                            lastSaveRef = save
                            repo.save(save.toEntity())
                            ProcessResult2.first("Saved: ${save.street}")
                        }
                        isFreshUpdate -> {
                            lastUpdateRef = update
                            repo.update(update.toEntity())
                            ProcessResult2.first("Updated: ${update.street}")
                        }
                        isFreshRemove -> {
                            lastRemoveRef = remove
                            repo.remove(remove.toEntity())
                            ProcessResult2.first("Removed: ${remove.street}")
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
