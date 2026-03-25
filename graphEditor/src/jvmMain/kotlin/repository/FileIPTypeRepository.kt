/*
 * FileIPTypeRepository - File-based persistence for custom IP types
 * License: Apache 2.0
 */

package io.codenode.grapheditor.repository

import io.codenode.grapheditor.model.CustomIPTypeDefinition
import io.codenode.grapheditor.model.SerializableIPType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * File-based repository for persisting custom IP types to JSON.
 *
 * @deprecated IP type creation and discovery is now filesystem-based via IPTypeDiscovery
 * and IPTypeFileGenerator. This repository is retained only for entity module tracking
 * (hasEntityModule/setEntityModule) and legacy migration. New IP types should be created
 * via IPTypeFileGenerator which writes .kt files to the iptypes/ directory.
 *
 * @param filePath Path to the JSON storage file (defaults to ~/.codenode/custom-ip-types.json)
 */
@Deprecated("Use IPTypeDiscovery + IPTypeFileGenerator for IP type management. Retained for entity module tracking.")
class FileIPTypeRepository(
    private val filePath: String = "${System.getProperty("user.home")}/.codenode/custom-ip-types.json"
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val customTypes = mutableListOf<SerializableIPType>()

    /**
     * Returns all persisted custom IP types as serializable DTOs.
     */
    fun getAll(): List<SerializableIPType> = customTypes.toList()

    /**
     * Returns all persisted custom IP types as domain definitions.
     */
    fun getAllDefinitions(): List<CustomIPTypeDefinition> =
        customTypes.map { it.toDefinition() }

    /**
     * Adds a custom IP type and persists to disk.
     *
     * @param definition The domain definition to add
     * @param description Optional description for the type
     */
    fun add(definition: CustomIPTypeDefinition, description: String? = null) {
        customTypes.add(SerializableIPType.fromDefinition(definition, description))
        save()
    }

    /**
     * Removes a custom IP type by ID and persists the change.
     *
     * @param id The type ID to remove
     * @return true if a type was removed
     */
    fun remove(id: String): Boolean {
        val removed = customTypes.removeAll { it.id == id }
        if (removed) {
            save()
        }
        return removed
    }

    /**
     * Marks an IP type as having (or not having) an entity module.
     *
     * @param id The type ID to update
     * @param hasModule true if an entity module exists, false otherwise
     * @return true if the type was found and updated
     */
    fun setEntityModule(id: String, hasModule: Boolean): Boolean {
        val index = customTypes.indexOfFirst { it.id == id }
        if (index < 0) return false
        customTypes[index] = customTypes[index].copy(hasEntityModule = hasModule)
        save()
        return true
    }

    /**
     * Returns whether an IP type has an entity module.
     */
    fun hasEntityModule(id: String): Boolean =
        customTypes.any { it.id == id && it.hasEntityModule }

    /**
     * Returns all IP type IDs that have entity modules.
     */
    fun getEntityModuleIPTypeIds(): Set<String> =
        customTypes.filter { it.hasEntityModule }.map { it.id }.toSet()

    /**
     * Loads custom IP types from the JSON file.
     *
     * If the file does not exist or is corrupt, the list remains empty.
     * This is safe to call multiple times.
     */
    fun load() {
        val file = File(filePath)
        if (file.exists()) {
            try {
                val content = file.readText()
                if (content.isNotBlank()) {
                    val loaded: List<SerializableIPType> = json.decodeFromString(content)
                    customTypes.clear()
                    customTypes.addAll(loaded)
                }
            } catch (e: Exception) {
                println("Warning: Could not load custom IP types from $filePath: ${e.message}")
            }
        }
    }

    /**
     * Saves all custom IP types to the JSON file.
     *
     * Creates parent directories if they don't exist.
     */
    fun save() {
        try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(customTypes.toList()))
        } catch (e: Exception) {
            println("Error: Could not save custom IP types to $filePath: ${e.message}")
            throw e
        }
    }
}
