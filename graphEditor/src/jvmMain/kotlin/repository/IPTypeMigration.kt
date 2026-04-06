/*
 * IPTypeMigration - One-time migration from legacy JSON repository to filesystem
 * License: Apache 2.0
 */

package io.codenode.grapheditor.repository

import io.codenode.fbpdsl.model.PlacementLevel
import io.codenode.grapheditor.state.IPTypeDiscovery
import io.codenode.grapheditor.state.IPTypeFileGenerator
import java.io.File

/**
 * Handles one-time migration of custom IP types from the legacy JSON repository
 * (`~/.codenode/custom-ip-types.json`) to filesystem-based `.kt` files in the
 * Universal directory (`~/.codenode/iptypes/`).
 *
 * Types that already exist at the Module level (discovered via IPTypeDiscovery)
 * are skipped to avoid duplicates.
 *
 * Migration only runs if the JSON file exists and the iptypes directory is empty.
 * After successful migration, the JSON file is renamed to `.bak`.
 *
 * @param fileGenerator The file generator for creating .kt IP type files
 * @param discovery The discovery instance for checking existing Module-level types
 */
class IPTypeMigration(
    private val fileGenerator: IPTypeFileGenerator,
    private val discovery: IPTypeDiscovery
) {

    private val jsonFile = File(System.getProperty("user.home"), ".codenode/custom-ip-types.json")
    private val iptypesDir = File(System.getProperty("user.home"), ".codenode/iptypes")

    /**
     * Migrates legacy JSON IP types to filesystem if needed.
     * Skips types that already exist at the Module level.
     *
     * @return Number of types migrated, or 0 if migration was skipped
     */
    fun migrateIfNeeded(): Int {
        // Skip if no JSON file exists
        if (!jsonFile.exists()) return 0

        // Skip if iptypes directory already has content (migration already done)
        if (iptypesDir.isDirectory && (iptypesDir.listFiles()?.isNotEmpty() == true)) return 0

        // Discover types already present at Module level
        val existingModuleTypes = discovery.discoverAll()
            .filter { it.tier == PlacementLevel.MODULE }
            .map { it.typeName }
            .toSet()

        // Load legacy types
        @Suppress("DEPRECATION")
        val repository = FileIPTypeRepository(jsonFile.absolutePath)
        repository.load()
        val definitions = repository.getAllDefinitions()

        if (definitions.isEmpty()) return 0

        // Generate .kt files only for types NOT already at Module level
        var migratedCount = 0
        var skippedCount = 0
        for (definition in definitions) {
            if (definition.typeName in existingModuleTypes) {
                skippedCount++
                continue
            }
            try {
                fileGenerator.generateIPTypeFile(definition, PlacementLevel.UNIVERSAL)
                migratedCount++
            } catch (e: Exception) {
                println("Warning: Failed to migrate IP type '${definition.typeName}': ${e.message}")
            }
        }

        // Rename JSON to .bak only after all files are written
        if (migratedCount > 0 || skippedCount > 0) {
            try {
                val bakFile = File(jsonFile.parent, "custom-ip-types.json.bak")
                jsonFile.renameTo(bakFile)
                if (migratedCount > 0) {
                    println("Migrated $migratedCount IP types from JSON to filesystem. JSON backed up to ${bakFile.name}")
                }
                if (skippedCount > 0) {
                    println("Skipped $skippedCount IP types already present at Module level.")
                }
            } catch (e: Exception) {
                println("Warning: Could not rename JSON file to .bak: ${e.message}")
            }
        }

        return migratedCount
    }
}
