/*
 * IPTypeFileGenerator - Generates Kotlin data class files for new IP types
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import io.codenode.grapheditor.model.CustomIPTypeDefinition
import io.codenode.fbpdsl.model.PlacementLevel
import java.io.File

/**
 * Generates Kotlin data class files for new IP types and writes them to
 * the appropriate filesystem tier directory.
 *
 * @param projectRoot The project root directory
 */
class IPTypeFileGenerator(
    private val projectRoot: File
) {

    /**
     * Generates and writes a `.kt` IP type file for the given definition.
     *
     * @param definition The IP type definition (name, properties, color)
     * @param level Target filesystem tier (MODULE, PROJECT, UNIVERSAL)
     * @param activeModulePath Absolute path to the active module directory (required for MODULE level)
     * @return Absolute path to the generated file
     */
    fun generateIPTypeFile(
        definition: CustomIPTypeDefinition,
        level: PlacementLevel,
        activeModulePath: String? = null
    ): String {
        val outputDir = resolveOutputDirectory(level, activeModulePath)
        val fileName = "${definition.typeName}.kt"
        val file = File(outputDir, fileName)
        val packageName = resolvePackageName(level, activeModulePath)

        val content = generateFileContent(definition, packageName)
        file.writeText(content)

        return file.absolutePath
    }

    /**
     * Resolves the output directory for the given level.
     * Creates the directory if it doesn't exist.
     */
    fun resolveOutputDirectory(level: PlacementLevel, activeModulePath: String?): File {
        val dir = when (level) {
            PlacementLevel.MODULE -> {
                requireNotNull(activeModulePath) { "activeModulePath is required for MODULE level" }
                val modulePath = File(activeModulePath)
                val moduleName = modulePath.name.lowercase()
                File(modulePath, "src/commonMain/kotlin/io/codenode/$moduleName/iptypes")
            }
            PlacementLevel.PROJECT -> {
                File(projectRoot, "iptypes/src/commonMain/kotlin/io/codenode/iptypes")
            }
            PlacementLevel.UNIVERSAL -> {
                File(System.getProperty("user.home"), ".codenode/iptypes")
            }
        }
        dir.mkdirs()
        return dir
    }

    /**
     * Maps an IP type ID to a Kotlin type name.
     */
    fun mapKotlinType(typeId: String): String = when (typeId) {
        "ip_string" -> "String"
        "ip_int" -> "Int"
        "ip_double" -> "Double"
        "ip_boolean" -> "Boolean"
        "ip_any" -> "Any"
        else -> "Any"
    }

    private fun resolvePackageName(level: PlacementLevel, activeModulePath: String?): String = when (level) {
        PlacementLevel.MODULE -> {
            val moduleName = File(activeModulePath!!).name.lowercase()
            "io.codenode.$moduleName.iptypes"
        }
        PlacementLevel.PROJECT -> "io.codenode.iptypes"
        PlacementLevel.UNIVERSAL -> "" // No package for universal templates
    }

    private fun generateFileContent(definition: CustomIPTypeDefinition, packageName: String): String {
        val color = definition.color
        return buildString {
            appendLine("/*")
            appendLine(" * ${definition.typeName} - Custom IP Type")
            appendLine(" * @IPType")
            appendLine(" * @TypeName ${definition.typeName}")
            appendLine(" * @TypeId ${definition.id}")
            appendLine(" * @Color rgb(${color.red}, ${color.green}, ${color.blue})")
            appendLine(" * License: Apache 2.0")
            appendLine(" */")
            appendLine()
            if (packageName.isNotEmpty()) {
                appendLine("package $packageName")
                appendLine()
            }
            appendLine("data class ${definition.typeName}(")
            if (definition.properties.isEmpty()) {
                appendLine("    val value: Any? = null")
            } else {
                for ((index, prop) in definition.properties.withIndex()) {
                    val kotlinType = mapKotlinType(prop.typeId)
                    val nullable = if (!prop.isRequired) "?" else ""
                    val default = if (!prop.isRequired) " = null" else ""
                    val comma = if (index < definition.properties.size - 1) "," else ""
                    appendLine("    val ${prop.name}: $kotlinType$nullable$default$comma")
                }
            }
            appendLine(")")
        }
    }
}
