/*
 * ModuleSaveService
 * Creates KMP module structure when saving a FlowGraph
 * License: Apache 2.0
 */

package io.codenode.grapheditor.save

import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.kotlincompiler.generator.FlowKtGenerator
import io.codenode.kotlincompiler.generator.ModuleGenerator
import io.codenode.kotlincompiler.generator.ProcessingLogicStubGenerator
import io.codenode.kotlincompiler.generator.RuntimeFlowGenerator
import io.codenode.kotlincompiler.generator.RuntimeControllerGenerator
import io.codenode.kotlincompiler.generator.RuntimeControllerInterfaceGenerator
import io.codenode.kotlincompiler.generator.RuntimeControllerAdapterGenerator
import io.codenode.kotlincompiler.generator.RuntimeViewModelGenerator
import io.codenode.kotlincompiler.generator.RepositoryCodeGenerator
import io.codenode.kotlincompiler.generator.EntityProperty
import io.codenode.kotlincompiler.generator.EntityInfo
import java.io.File

/**
 * Result of a module save operation.
 *
 * @property success Whether the save succeeded
 * @property moduleDir The created module directory (if successful)
 * @property errorMessage Error message (if failed)
 * @property filesCreated List of new files created during save
 * @property filesOverwritten List of files overwritten during save
 * @property filesDeleted List of orphaned files deleted during save
 * @property warnings List of warnings (non-fatal issues)
 */
data class ModuleSaveResult(
    val success: Boolean,
    val moduleDir: File? = null,
    val errorMessage: String? = null,
    val filesCreated: List<String> = emptyList(),
    val filesOverwritten: List<String> = emptyList(),
    val filesDeleted: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Service for saving FlowGraphs as KMP module structures.
 *
 * The unified [saveModule] creates the full module in a single call:
 * module directory, gradle files, .flow.kt in the base package source set,
 * 4 runtime files under generated/, a ViewModel stub in the base package,
 * and ProcessingLogic stubs under processingLogic/. Orphaned stubs are deleted.
 *
 * The ViewModel stub contains a marker-delineated Module Properties section
 * that is selectively regenerated on re-save while preserving user code
 * outside the markers.
 */
class ModuleSaveService {

    companion object {
        const val DEFAULT_PACKAGE_PREFIX = "io.codenode"
        const val GENERATED_SUBPACKAGE = "generated"
        const val PROCESSING_LOGIC_SUBPACKAGE = "processingLogic"
        const val PERSISTENCE_SUBPACKAGE = "persistence"
    }

    private val moduleGenerator = ModuleGenerator()
    private val flowKtGenerator = FlowKtGenerator()
    private val stubGenerator = ProcessingLogicStubGenerator()
    private val runtimeFlowGenerator = RuntimeFlowGenerator()
    private val runtimeControllerGenerator = RuntimeControllerGenerator()
    private val runtimeControllerInterfaceGenerator = RuntimeControllerInterfaceGenerator()
    private val runtimeControllerAdapterGenerator = RuntimeControllerAdapterGenerator()
    private val runtimeViewModelGenerator = RuntimeViewModelGenerator()
    private val repositoryCodeGenerator = RepositoryCodeGenerator()

    /**
     * Saves a FlowGraph as a complete KMP module.
     *
     * Creates the full module in a single call: module directory, gradle files,
     * .flow.kt in the base package source set, 4 runtime files, ProcessingLogic stubs,
     * and a ViewModel stub in the base package. On re-save, .flow.kt and runtime files
     * are always overwritten, existing stubs are preserved, new stubs are created for
     * added nodes, orphaned stubs are deleted for removed nodes, and the ViewModel's
     * Module Properties section is selectively regenerated while preserving user code.
     *
     * When [regenerateStubs] is true, ProcessingLogic stubs are selectively regenerated
     * (boilerplate updated with current port names/types while preserving the lambda body).
     *
     * @param flowGraph The flow graph to save
     * @param outputDir Parent directory where the module will be created
     * @param packageName Package name for generated code (default: io.codenode.{modulename})
     * @param moduleName Module name (default: derived from FlowGraph name)
     * @param regenerateStubs When true, regenerate stub files with updated boilerplate
     * @return ModuleSaveResult with success status, module directory, and file tracking
     */
    fun saveModule(
        flowGraph: FlowGraph,
        outputDir: File,
        packageName: String? = null,
        moduleName: String? = null,
        regenerateStubs: Boolean = false,
        ipTypeProperties: Map<String, List<EntityProperty>> = emptyMap(),
        ipTypeNames: Map<String, String> = emptyMap()
    ): ModuleSaveResult {
        return try {
            val effectiveModuleName = moduleName ?: deriveModuleName(flowGraph.name)

            val basePackage = packageName ?: "$DEFAULT_PACKAGE_PREFIX.${effectiveModuleName.lowercase()}"
            val generatedPackage = "$basePackage.$GENERATED_SUBPACKAGE"
            val processingLogicPackage = "$basePackage.$PROCESSING_LOGIC_SUBPACKAGE"

            // Create module directory
            val moduleDir = File(outputDir, effectiveModuleName)
            if (!moduleDir.exists()) {
                moduleDir.mkdirs()
            }

            val filesCreated = mutableListOf<String>()
            val filesOverwritten = mutableListOf<String>()
            val filesDeleted = mutableListOf<String>()

            // Create source directory structure
            createDirectoryStructure(moduleDir, basePackage, flowGraph)
            createDirectoryStructure(moduleDir, generatedPackage, flowGraph)
            createDirectoryStructure(moduleDir, processingLogicPackage, flowGraph)

            // Write gradle files (only if they don't exist)
            writeFileIfNew(
                File(moduleDir, "build.gradle.kts"),
                moduleGenerator.generateBuildGradle(flowGraph, effectiveModuleName),
                "build.gradle.kts",
                filesCreated
            )
            writeFileIfNew(
                File(moduleDir, "settings.gradle.kts"),
                generateSettingsGradle(effectiveModuleName),
                "settings.gradle.kts",
                filesCreated
            )

            // Write .flow.kt in source set (always overwrite)
            val basePackagePath = basePackage.replace(".", "/")
            val flowKtFileName = "${effectiveModuleName}.flow.kt"
            val flowKtRelativePath = "src/commonMain/kotlin/$basePackagePath/$flowKtFileName"
            writeFileAlways(
                File(moduleDir, flowKtRelativePath),
                flowKtGenerator.generateFlowKt(flowGraph, basePackage, processingLogicPackage, ipTypeNames),
                flowKtRelativePath,
                filesCreated,
                filesOverwritten
            )

            // Generate 4 runtime files in generated/ (always overwrite)
            generateRuntimeFilesTracked(
                flowGraph, moduleDir, basePackage, generatedPackage, processingLogicPackage,
                effectiveModuleName, filesCreated, filesOverwritten
            )

            // Generate ViewModel stub in base package (selective regeneration)
            generateViewModelStub(
                flowGraph, moduleDir, basePackage, generatedPackage,
                effectiveModuleName, filesCreated, filesOverwritten
            )

            // Generate processing logic stubs (don't overwrite existing, unless regenerating)
            generateProcessingLogicStubs(
                flowGraph, moduleDir, processingLogicPackage,
                filesCreated, filesOverwritten, regenerateStubs
            )

            // Generate persistence layer for repository nodes
            generatePersistenceFiles(
                flowGraph, moduleDir, basePackage, ipTypeProperties,
                filesCreated, filesOverwritten
            )

            // Delete orphaned stubs
            deleteOrphanedComponents(flowGraph, moduleDir, processingLogicPackage, filesDeleted)

            ModuleSaveResult(
                success = true,
                moduleDir = moduleDir,
                filesCreated = filesCreated,
                filesOverwritten = filesOverwritten,
                filesDeleted = filesDeleted
            )
        } catch (e: Exception) {
            ModuleSaveResult(
                success = false,
                errorMessage = "Failed to save module: ${e.message}"
            )
        }
    }

    /**
     * Derives a valid module name from a FlowGraph name.
     *
     * Handles:
     * - Spaces (replaced with nothing, using PascalCase)
     * - Special characters (removed)
     *
     * @param flowGraphName The FlowGraph name
     * @return A valid module directory name
     */
    internal fun deriveModuleName(flowGraphName: String): String {
        // Split on spaces and capitalize each word (PascalCase)
        return flowGraphName
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString("") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
            .filter { it.isLetterOrDigit() || it == '-' || it == '_' }
    }

    /**
     * Creates the source directory structure for the module.
     *
     * Creates:
     * - src/commonMain/kotlin/{package}
     * - src/commonTest/kotlin/{package}
     * - Platform-specific directories based on FlowGraph targets
     *
     * @param moduleDir The module root directory
     * @param packageName The package name (e.g., io.codenode.stopwatch.generated)
     * @param flowGraph The flow graph (for determining target platforms)
     */
    private fun createDirectoryStructure(
        moduleDir: File,
        packageName: String,
        flowGraph: FlowGraph
    ) {
        val packagePath = packageName.replace(".", "/")

        // Common source sets (always created)
        File(moduleDir, "src/commonMain/kotlin/$packagePath").mkdirs()
        File(moduleDir, "src/commonTest/kotlin/$packagePath").mkdirs()

        // JVM source sets (always included)
        File(moduleDir, "src/jvmMain/kotlin/$packagePath").mkdirs()
        File(moduleDir, "src/jvmTest/kotlin").mkdirs()

        // Platform-specific source sets
        if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)) {
            File(moduleDir, "src/androidMain/kotlin/$packagePath").mkdirs()
            File(moduleDir, "src/androidTest/kotlin").mkdirs()
        }

        if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_IOS)) {
            File(moduleDir, "src/iosMain/kotlin/$packagePath").mkdirs()
            File(moduleDir, "src/iosTest/kotlin").mkdirs()
        }

        if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_WEB)) {
            File(moduleDir, "src/jsMain/kotlin/$packagePath").mkdirs()
            File(moduleDir, "src/jsTest/kotlin").mkdirs()
        }

        if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_WASM)) {
            File(moduleDir, "src/wasmJsMain/kotlin/$packagePath").mkdirs()
        }
    }

    /**
     * Generates the ViewModel stub file in the base package.
     *
     * On first save, generates the full ViewModel stub with Module Properties section
     * and ViewModel class. On re-save, performs selective regeneration: reads the
     * existing file, replaces the content between MODULE PROPERTIES START/END markers
     * with freshly generated module properties, and preserves all user code outside
     * the markers. If markers are missing in an existing file, treats it as fresh generation.
     *
     * @param flowGraph The flow graph
     * @param moduleDir The module root directory
     * @param basePackage The base package name
     * @param generatedPackage The generated package name (for ControllerInterface import)
     * @param effectiveModuleName The module name
     * @param filesCreated List to track created files
     * @param filesOverwritten List to track overwritten files
     */
    private fun generateViewModelStub(
        flowGraph: FlowGraph,
        moduleDir: File,
        basePackage: String,
        generatedPackage: String,
        effectiveModuleName: String,
        filesCreated: MutableList<String>,
        filesOverwritten: MutableList<String>
    ) {
        val basePackagePath = basePackage.replace(".", "/")
        val viewModelFileName = "${effectiveModuleName}ViewModel.kt"
        val viewModelFile = File(moduleDir, "src/commonMain/kotlin/$basePackagePath/$viewModelFileName")
        val relativePath = "src/commonMain/kotlin/$basePackagePath/$viewModelFileName"

        if (!viewModelFile.exists()) {
            // First save: generate full ViewModel stub
            val content = runtimeViewModelGenerator.generate(flowGraph, basePackage, generatedPackage)
            viewModelFile.writeText(content)
            filesCreated.add(relativePath)
        } else {
            // Re-save: selective regeneration of Module Properties section
            val existingContent = viewModelFile.readText()
            val startMarker = RuntimeViewModelGenerator.MODULE_PROPERTIES_START
            val endMarker = RuntimeViewModelGenerator.MODULE_PROPERTIES_END

            val startIndex = existingContent.indexOf(startMarker)
            val endIndex = existingContent.indexOf(endMarker)

            if (startIndex >= 0 && endIndex >= 0 && endIndex > startIndex) {
                // Find the end of the endMarker line (include trailing newline)
                val endOfMarkerLine = existingContent.indexOf('\n', endIndex)
                val effectiveEnd = if (endOfMarkerLine >= 0) endOfMarkerLine + 1 else existingContent.length

                // Replace the section between markers (inclusive) with regenerated content
                val newModuleProperties = runtimeViewModelGenerator.generateModulePropertiesSection(flowGraph)
                val newContent = existingContent.substring(0, startIndex) +
                    newModuleProperties +
                    existingContent.substring(effectiveEnd)
                viewModelFile.writeText(newContent)
                filesOverwritten.add(relativePath)
            } else {
                // Markers missing: treat as fresh generation
                val content = runtimeViewModelGenerator.generate(flowGraph, basePackage, generatedPackage)
                viewModelFile.writeText(content)
                filesOverwritten.add(relativePath)
            }
        }
    }

    /**
     * Generates ProcessingLogic stub files for each CodeNode in the FlowGraph.
     *
     * Creates one stub file per CodeNode, only if the file doesn't already exist
     * (to preserve user implementations). When [regenerateStubs] is true,
     * existing stubs are selectively regenerated: boilerplate (package, imports,
     * KDoc, type alias, parameter names) is updated while the lambda body
     * (user implementation) is preserved.
     *
     * @param flowGraph The flow graph containing CodeNodes
     * @param moduleDir The module root directory
     * @param packageName The package name for generated files
     * @param filesCreated List to track created files
     * @param filesOverwritten List to track overwritten files
     * @param regenerateStubs When true, selectively regenerate existing stubs
     */
    private fun generateProcessingLogicStubs(
        flowGraph: FlowGraph,
        moduleDir: File,
        packageName: String,
        filesCreated: MutableList<String>,
        filesOverwritten: MutableList<String>,
        regenerateStubs: Boolean
    ) {
        val packagePath = packageName.replace(".", "/")
        val sourceDir = File(moduleDir, "src/commonMain/kotlin/$packagePath")

        // Get all CodeNodes from the flow graph (including nested ones)
        val codeNodes = flowGraph.getAllCodeNodes()

        for (codeNode in codeNodes) {
            val stubFileName = stubGenerator.getStubFileName(codeNode)
            val stubFile = File(sourceDir, stubFileName)
            val relativePath = "src/commonMain/kotlin/$packagePath/$stubFileName"

            if (!stubFile.exists()) {
                // New stub: generate fresh
                val stubContent = stubGenerator.generateStub(codeNode, packageName)
                stubFile.writeText(stubContent)
                filesCreated.add(relativePath)
            } else if (regenerateStubs) {
                // Existing stub + regenerate flag: selectively regenerate
                val existingContent = stubFile.readText()
                val preservedBody = stubGenerator.extractLambdaBody(existingContent)
                val newContent = if (preservedBody != null) {
                    stubGenerator.generateStubWithPreservedBody(
                        codeNode, packageName, preservedBody
                    )
                } else {
                    // Could not parse lambda body — regenerate fresh
                    stubGenerator.generateStub(codeNode, packageName)
                }
                stubFile.writeText(newContent)
                filesOverwritten.add(relativePath)
            }
            // else: existing stub + no regenerate flag → preserve as-is (do nothing)
        }
    }

    /**
     * Writes a file only if it doesn't already exist.
     * Tracks newly created files in [filesCreated].
     */
    private fun writeFileIfNew(
        file: File,
        content: String,
        relativePath: String,
        filesCreated: MutableList<String>
    ) {
        if (!file.exists()) {
            file.writeText(content)
            filesCreated.add(relativePath)
        }
    }

    /**
     * Writes a file, always overwriting existing content.
     * Tracks as [filesOverwritten] if file existed, or [filesCreated] if new.
     */
    private fun writeFileAlways(
        file: File,
        content: String,
        relativePath: String,
        filesCreated: MutableList<String>,
        filesOverwritten: MutableList<String>
    ) {
        val existed = file.exists()
        file.writeText(content)
        if (existed) filesOverwritten.add(relativePath) else filesCreated.add(relativePath)
    }

    /**
     * Generates the 4 runtime files with proper overwrite tracking.
     * Files are always written; tracked as overwritten if they existed, created if new.
     */
    private fun generateRuntimeFilesTracked(
        flowGraph: FlowGraph,
        moduleDir: File,
        basePackage: String,
        generatedPackage: String,
        processingLogicPackage: String,
        effectiveModuleName: String,
        filesCreated: MutableList<String>,
        filesOverwritten: MutableList<String>
    ) {
        val generatedPath = generatedPackage.replace(".", "/")
        val generatedDir = File(moduleDir, "src/commonMain/kotlin/$generatedPath")

        val runtimeFiles = listOf(
            "${effectiveModuleName}Flow.kt" to runtimeFlowGenerator.generate(flowGraph, generatedPackage, processingLogicPackage, basePackage),
            "${effectiveModuleName}Controller.kt" to runtimeControllerGenerator.generate(flowGraph, generatedPackage, processingLogicPackage),
            "${effectiveModuleName}ControllerInterface.kt" to runtimeControllerInterfaceGenerator.generate(flowGraph, generatedPackage),
            "${effectiveModuleName}ControllerAdapter.kt" to runtimeControllerAdapterGenerator.generate(flowGraph, generatedPackage)
        )

        for ((fileName, content) in runtimeFiles) {
            val file = File(generatedDir, fileName)
            val relativePath = "src/commonMain/kotlin/$generatedPath/$fileName"
            writeFileAlways(file, content, relativePath, filesCreated, filesOverwritten)
        }
    }

    /**
     * Generates persistence layer files for repository nodes in the FlowGraph.
     * Detects repository nodes via `_repository` configuration, generates Entity, DAO,
     * Repository, BaseDao, AppDatabase, DatabaseModule, and platform-specific builders.
     */
    private fun generatePersistenceFiles(
        flowGraph: FlowGraph,
        moduleDir: File,
        basePackage: String,
        ipTypeProperties: Map<String, List<EntityProperty>>,
        filesCreated: MutableList<String>,
        filesOverwritten: MutableList<String>
    ) {
        val repositoryNodes = flowGraph.getAllCodeNodes()
            .filter { it.configuration["_repository"] == "true" }

        if (repositoryNodes.isEmpty()) return

        val persistencePackage = "$basePackage.$PERSISTENCE_SUBPACKAGE"
        val persistencePath = persistencePackage.replace(".", "/")

        // Ensure persistence directories exist
        File(moduleDir, "src/commonMain/kotlin/$persistencePath").mkdirs()
        File(moduleDir, "src/jvmMain/kotlin/$persistencePath").mkdirs()
        if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)) {
            File(moduleDir, "src/androidMain/kotlin/$persistencePath").mkdirs()
        }
        if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_IOS)) {
            File(moduleDir, "src/iosMain/kotlin/$persistencePath").mkdirs()
        }

        // Generate BaseDao.kt
        writeFileAlways(
            File(moduleDir, "src/commonMain/kotlin/$persistencePath/BaseDao.kt"),
            repositoryCodeGenerator.generateBaseDao(persistencePackage),
            "src/commonMain/kotlin/$persistencePath/BaseDao.kt",
            filesCreated, filesOverwritten
        )

        // Generate per-entity files and collect EntityInfo for database
        val entityInfos = mutableListOf<EntityInfo>()

        for (node in repositoryNodes) {
            val sourceIPTypeName = node.configuration["_sourceIPTypeName"] ?: continue
            val sourceIPTypeId = node.configuration["_sourceIPTypeId"] ?: continue

            val tableName = sourceIPTypeName.lowercase() + "s"
            val entityInfo = EntityInfo(
                entityName = sourceIPTypeName,
                tableName = tableName,
                daoName = "${sourceIPTypeName}Dao"
            )
            entityInfos.add(entityInfo)

            // Get properties from IP type registry data
            val properties = ipTypeProperties[sourceIPTypeId] ?: emptyList()

            // Entity
            writeFileAlways(
                File(moduleDir, "src/commonMain/kotlin/$persistencePath/${sourceIPTypeName}Entity.kt"),
                repositoryCodeGenerator.generateEntity(sourceIPTypeName, properties, persistencePackage),
                "src/commonMain/kotlin/$persistencePath/${sourceIPTypeName}Entity.kt",
                filesCreated, filesOverwritten
            )

            // DAO
            writeFileAlways(
                File(moduleDir, "src/commonMain/kotlin/$persistencePath/${sourceIPTypeName}Dao.kt"),
                repositoryCodeGenerator.generateDao(sourceIPTypeName, tableName, persistencePackage),
                "src/commonMain/kotlin/$persistencePath/${sourceIPTypeName}Dao.kt",
                filesCreated, filesOverwritten
            )

            // Repository
            writeFileAlways(
                File(moduleDir, "src/commonMain/kotlin/$persistencePath/${sourceIPTypeName}Repository.kt"),
                repositoryCodeGenerator.generateRepository(sourceIPTypeName, persistencePackage),
                "src/commonMain/kotlin/$persistencePath/${sourceIPTypeName}Repository.kt",
                filesCreated, filesOverwritten
            )
        }

        // Generate shared AppDatabase.kt
        writeFileAlways(
            File(moduleDir, "src/commonMain/kotlin/$persistencePath/AppDatabase.kt"),
            repositoryCodeGenerator.generateDatabase(entityInfos, persistencePackage),
            "src/commonMain/kotlin/$persistencePath/AppDatabase.kt",
            filesCreated, filesOverwritten
        )

        // Generate DatabaseModule.kt
        writeFileAlways(
            File(moduleDir, "src/commonMain/kotlin/$persistencePath/DatabaseModule.kt"),
            repositoryCodeGenerator.generateDatabaseModule(persistencePackage),
            "src/commonMain/kotlin/$persistencePath/DatabaseModule.kt",
            filesCreated, filesOverwritten
        )

        // Generate platform-specific DatabaseBuilder files
        val dbFileName = "app.db"

        // JVM (always included)
        writeFileAlways(
            File(moduleDir, "src/jvmMain/kotlin/$persistencePath/DatabaseBuilder.jvm.kt"),
            repositoryCodeGenerator.generateDatabaseBuilder("jvm", persistencePackage, dbFileName),
            "src/jvmMain/kotlin/$persistencePath/DatabaseBuilder.jvm.kt",
            filesCreated, filesOverwritten
        )

        // Android
        if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)) {
            writeFileAlways(
                File(moduleDir, "src/androidMain/kotlin/$persistencePath/DatabaseBuilder.android.kt"),
                repositoryCodeGenerator.generateDatabaseBuilder("android", persistencePackage, dbFileName),
                "src/androidMain/kotlin/$persistencePath/DatabaseBuilder.android.kt",
                filesCreated, filesOverwritten
            )
        }

        // iOS
        if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_IOS)) {
            writeFileAlways(
                File(moduleDir, "src/iosMain/kotlin/$persistencePath/DatabaseBuilder.ios.kt"),
                repositoryCodeGenerator.generateDatabaseBuilder("ios", persistencePackage, dbFileName),
                "src/iosMain/kotlin/$persistencePath/DatabaseBuilder.ios.kt",
                filesCreated, filesOverwritten
            )
        }
    }

    /**
     * Deletes orphaned ProcessingLogic files (stubs for nodes that no longer exist).
     * Tracks deleted files in [filesDeleted].
     */
    private fun deleteOrphanedComponents(
        flowGraph: FlowGraph,
        moduleDir: File,
        packageName: String,
        filesDeleted: MutableList<String>
    ) {
        val packagePath = packageName.replace(".", "/")
        val sourceDir = File(moduleDir, "src/commonMain/kotlin/$packagePath")

        if (!sourceDir.exists()) return

        val codeNodes = flowGraph.getAllCodeNodes()
        val expectedFiles = codeNodes.map { stubGenerator.getStubFileName(it) }.toSet()

        val existingComponentFiles = sourceDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith("ProcessLogic.kt") }
            ?: emptyList()

        for (file in existingComponentFiles) {
            if (file.name !in expectedFiles) {
                val relativePath = "src/commonMain/kotlin/$packagePath/${file.name}"
                file.delete()
                filesDeleted.add(relativePath)
            }
        }
    }

    /**
     * Generates settings.gradle.kts content.
     *
     * @param moduleName The module name
     * @return Generated settings.gradle.kts content
     */
    private fun generateSettingsGradle(moduleName: String): String {
        return buildString {
            appendLine("/*")
            appendLine(" * Settings for $moduleName")
            appendLine(" * Generated by CodeNodeIO ModuleSaveService")
            appendLine(" * License: Apache 2.0")
            appendLine(" */")
            appendLine()
            appendLine("pluginManagement {")
            appendLine("    repositories {")
            appendLine("        google()")
            appendLine("        mavenCentral()")
            appendLine("        gradlePluginPortal()")
            appendLine("    }")
            appendLine("}")
            appendLine()
            appendLine("dependencyResolutionManagement {")
            appendLine("    repositories {")
            appendLine("        google()")
            appendLine("        mavenCentral()")
            appendLine("    }")
            appendLine("}")
            appendLine()
            appendLine("rootProject.name = \"$moduleName\"")
        }
    }
}
