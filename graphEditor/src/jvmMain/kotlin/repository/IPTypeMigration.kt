/*
 * IPTypeMigration - One-time migration from legacy JSON repository to filesystem
 * License: Apache 2.0
 */

package io.codenode.grapheditor.repository

import io.codenode.grapheditor.model.PlacementLevel
import io.codenode.grapheditor.state.IPTypeFileGenerator
import java.io.File

/**
 * Handles one-time migration of custom IP types from the legacy JSON repository
 * (`~/.codenode/custom-ip-types.json`) to filesystem-based `.kt` files in the
 * Universal directory (`~/.codenode/iptypes/`).
 *
 * Migration only runs if the JSON file exists and the iptypes directory is empty.
 * After successful migration, the JSON file is renamed to `.bak`.
 *
 * @param fileGenerator The file generator for creating .kt IP type files
 */
class IPTypeMigration(
    private val fileGenerator: IPTypeFileGenerator
) {

    private val jsonFile = File(System.getProperty("user.home"), ".codenode/custom-ip-types.json")
    private val iptypesDir = File(System.getProperty("user.home"), ".codenode/iptypes")

    /**
     * Migrates legacy JSON IP types to filesystem if needed.
     *
     * @return Number of types migrated, or 0 if migration was skipped
     */
    fun migrateIfNeeded(): Int {
        // Skip if no JSON file exists
        if (!jsonFile.exists()) return 0

        // Skip if iptypes directory already has content (migration already done)
        if (iptypesDir.isDirectory && (iptypesDir.listFiles()?.isNotEmpty() == true)) return 0

        // Load legacy types
        val repository = FileIPTypeRepository(jsonFile.absolutePath)
        repository.load()
        val definitions = repository.getAllDefinitions()

        if (definitions.isEmpty()) return 0

        // Generate .kt files for each type in Universal directory
        var migratedCount = 0
        for (definition in definitions) {
            try {
                fileGenerator.generateIPTypeFile(definition, PlacementLevel.UNIVERSAL)
                migratedCount++
            } catch (e: Exception) {
                println("Warning: Failed to migrate IP type '${definition.typeName}': ${e.message}")
            }
        }

        // Rename JSON to .bak only after all files are written
        if (migratedCount > 0) {
            try {
                val bakFile = File(jsonFile.parent, "custom-ip-types.json.bak")
                jsonFile.renameTo(bakFile)
                println("Migrated $migratedCount IP types from JSON to filesystem. JSON backed up to ${bakFile.name}")
            } catch (e: Exception) {
                println("Warning: Could not rename JSON file to .bak: ${e.message}")
            }
        }

        return migratedCount
    }
}
