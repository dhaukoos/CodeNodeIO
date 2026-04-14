/*
 * IPTypeDiscovery - Discovers IP type definition files from the filesystem
 * License: Apache 2.0
 */

package io.codenode.flowgraphtypes.discovery

import io.codenode.fbpdsl.model.IPColor
import io.codenode.flowgraphtypes.model.IPPropertyMeta
import io.codenode.flowgraphtypes.model.IPTypeFileMeta
import io.codenode.fbpdsl.model.PlacementLevel
import java.io.File
import kotlin.reflect.KClass

/**
 * Discovers IP type definition files from the four-tier filesystem and returns parsed metadata.
 *
 * IP type files are Kotlin data classes with `@IPType` metadata comment headers.
 * Discovery scans Module, Project, Universal, and Internal directories, deduplicating by
 * type name with tier precedence (Module > Project > Universal > Internal).
 *
 * @param projectRoot The project root directory
 * @param modulePaths List of module root directories to scan for Module-level IP types
 * @param toolRoot The graph editor's own source root (for INTERNAL tier IP types).
 *        When set, scans `<toolRoot>/iptypes/src/{commonMain,jvmMain}/kotlin/.../iptypes/`.
 */
class IPTypeDiscovery(
    private val projectRoot: File,
    private val modulePaths: List<File> = emptyList(),
    private val toolRoot: File? = null
) {

    // Regex patterns for parsing IP type file metadata
    private val ipTypeMarkerPattern = Regex("""@IPType""")
    private val typeNamePattern = Regex("""@TypeName\s+(\S+)""")
    private val typeIdPattern = Regex("""@TypeId\s+(\S+)""")
    private val colorPattern = Regex("""@Color\s+rgb\((\d+),\s*(\d+),\s*(\d+)\)""")
    private val packagePattern = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)
    private val dataClassPattern = Regex("""data\s+class\s+(\w+)\s*\(([^)]*)\)""", RegexOption.DOT_MATCHES_ALL)
    private val typealiasPattern = Regex("""typealias\s+(\w+)\s*=\s*(.+)""")
    private val propertyPattern = Regex("""val\s+(\w+)\s*:\s*([\w.<>?]+)""")

    /**
     * Scans all four tiers and returns discovered IP type metadata,
     * deduplicated by typeName with tier precedence (Module > Project > Universal > Internal).
     */
    fun discoverAll(): List<IPTypeFileMeta> {
        val allTypes = mutableListOf<IPTypeFileMeta>()

        // Scan Module directories (highest precedence) — both commonMain and jvmMain
        for (modulePath in modulePaths) {
            val ipTypesDir = findIpTypesDir(modulePath, "commonMain")
            if (ipTypesDir != null) {
                allTypes.addAll(scanDirectory(ipTypesDir, PlacementLevel.MODULE))
            }
            val jvmIpTypesDir = findIpTypesDir(modulePath, "jvmMain")
            if (jvmIpTypesDir != null) {
                allTypes.addAll(scanDirectory(jvmIpTypesDir, PlacementLevel.MODULE))
            }
        }

        // Scan Project directory (commonMain and jvmMain)
        val projectIpTypesDir = File(projectRoot, "iptypes/src/commonMain/kotlin/io/codenode/iptypes")
        if (projectIpTypesDir.isDirectory) {
            allTypes.addAll(scanDirectory(projectIpTypesDir, PlacementLevel.PROJECT))
        }
        val projectJvmIpTypesDir = File(projectRoot, "iptypes/src/jvmMain/kotlin/io/codenode/iptypes")
        if (projectJvmIpTypesDir.isDirectory) {
            allTypes.addAll(scanDirectory(projectJvmIpTypesDir, PlacementLevel.PROJECT))
        }

        // Scan Universal directory
        val universalDir = File(System.getProperty("user.home"), ".codenode/iptypes")
        if (universalDir.isDirectory) {
            allTypes.addAll(scanDirectory(universalDir, PlacementLevel.UNIVERSAL))
        }

        // Scan Internal directory (lowest precedence — tool's own built-in IP types)
        // toolRoot is the graph editor project root; its iptypes live in the "iptypes" module
        if (toolRoot != null) {
            val iptypesModule = File(toolRoot, "iptypes")
            val internalCommonDir = findIpTypesDir(iptypesModule, "commonMain")
            if (internalCommonDir != null) {
                allTypes.addAll(scanDirectory(internalCommonDir, PlacementLevel.INTERNAL))
            }
            val internalJvmDir = findIpTypesDir(iptypesModule, "jvmMain")
            if (internalJvmDir != null) {
                allTypes.addAll(scanDirectory(internalJvmDir, PlacementLevel.INTERNAL))
            }
        }

        // Deduplicate: keep the most specific tier for each typeName
        return allTypes
            .groupBy { it.typeName }
            .mapValues { (_, types) ->
                types.minByOrNull { it.tier.ordinal } ?: types.first()
            }
            .values
            .toList()
    }

    /**
     * Scans a directory for `.kt` files containing IP type definitions.
     * Files that fail to parse are skipped with a warning.
     */
    fun scanDirectory(dir: File, tier: PlacementLevel): List<IPTypeFileMeta> {
        if (!dir.isDirectory) return emptyList()

        return dir.listFiles { file -> file.extension == "kt" }
            ?.mapNotNull { file ->
                try {
                    parseIPTypeFile(file.absolutePath)?.copy(tier = tier)
                } catch (e: Exception) {
                    println("Warning: Failed to parse IP type file ${file.name}: ${e.message}")
                    null
                }
            }
            ?: emptyList()
    }

    /**
     * Parses a single `.kt` file and extracts IP type metadata.
     *
     * @return IPTypeFileMeta if the file contains a valid `@IPType` marker, null otherwise
     */
    fun parseIPTypeFile(filePath: String): IPTypeFileMeta? {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) return null

        val content = file.readText()

        // Check for @IPType marker
        if (!ipTypeMarkerPattern.containsMatchIn(content)) return null

        // Extract metadata from comment header
        val typeName = typeNamePattern.find(content)?.groupValues?.get(1) ?: return null
        val typeId = typeIdPattern.find(content)?.groupValues?.get(1) ?: "ip_${typeName.lowercase()}"
        val color = colorPattern.find(content)?.let {
            IPColor(
                it.groupValues[1].toInt(),
                it.groupValues[2].toInt(),
                it.groupValues[3].toInt()
            )
        } ?: IPColor(0, 0, 0)

        // Extract package
        val packageName = packagePattern.find(content)?.groupValues?.get(1) ?: ""

        // Try data class first (existing behavior), then fall back to typealias
        val dataClassMatch = dataClassPattern.find(content)
        val properties = if (dataClassMatch != null) {
            parseDataClassProperties(content)
        } else {
            // Typealias or other declaration — no own properties
            emptyList()
        }

        // Resolve class name: use typealias name if present and no data class
        val resolvedTypeName = if (dataClassMatch != null) {
            dataClassMatch.groupValues[1]
        } else {
            typealiasPattern.find(content)?.groupValues?.get(1) ?: typeName
        }

        // Build fully qualified class name
        val className = if (packageName.isNotEmpty()) "$packageName.$resolvedTypeName" else resolvedTypeName

        return IPTypeFileMeta(
            typeName = typeName,
            typeId = typeId,
            color = color,
            properties = properties,
            filePath = filePath,
            tier = PlacementLevel.PROJECT, // Default; caller overrides via copy()
            packageName = packageName,
            className = className
        )
    }

    /**
     * Attempts to resolve the real KClass for a discovered IP type via reflection.
     * Returns Any::class if the class is not on the classpath (e.g., Universal templates).
     */
    fun resolveKClass(meta: IPTypeFileMeta): KClass<*> {
        if (meta.tier == PlacementLevel.UNIVERSAL || meta.tier == PlacementLevel.INTERNAL) return Any::class
        return try {
            Class.forName(meta.className).kotlin
        } catch (_: ClassNotFoundException) {
            Any::class
        }
    }

    /**
     * Parses data class field declarations from source content.
     */
    private fun parseDataClassProperties(content: String): List<IPPropertyMeta> {
        val dataClassMatch = dataClassPattern.find(content) ?: return emptyList()
        val fieldsBlock = dataClassMatch.groupValues[2]

        return propertyPattern.findAll(fieldsBlock).map { match ->
            val name = match.groupValues[1]
            val kotlinType = match.groupValues[2].removeSuffix("?")
            val isRequired = !match.groupValues[2].endsWith("?")
            val typeId = mapKotlinTypeToId(kotlinType)
            IPPropertyMeta(
                name = name,
                kotlinType = kotlinType,
                typeId = typeId,
                isRequired = isRequired
            )
        }.filter { it.name != "id" } // Exclude the id field (auto-generated primary key)
            .toList()
    }

    /**
     * Maps a Kotlin type name to an IP type ID for the registry.
     */
    private fun mapKotlinTypeToId(kotlinType: String): String = when (kotlinType) {
        "String" -> "ip_string"
        "Int" -> "ip_int"
        "Long" -> "ip_int"
        "Double" -> "ip_double"
        "Float" -> "ip_double"
        "Boolean" -> "ip_boolean"
        else -> "ip_any"
    }

    /**
     * Finds the iptypes directory within a module by scanning the source tree.
     */
    private fun findIpTypesDir(modulePath: File, sourceSet: String = "commonMain"): File? {
        val sourceSetKotlin = File(modulePath, "src/$sourceSet/kotlin")
        if (!sourceSetKotlin.isDirectory) return null

        // Walk directory tree looking for an "iptypes" directory
        return sourceSetKotlin.walkTopDown()
            .filter { it.isDirectory && it.name == "iptypes" }
            .firstOrNull()
    }
}
