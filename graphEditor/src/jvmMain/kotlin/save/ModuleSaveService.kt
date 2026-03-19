/*
 * ModuleSaveService
 * Creates KMP module structure when saving a FlowGraph
 * License: Apache 2.0
 */

package io.codenode.grapheditor.save

import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.kotlincompiler.generator.FlowKtGenerator
import io.codenode.kotlincompiler.generator.ModuleGenerator
import io.codenode.kotlincompiler.generator.RuntimeFlowGenerator
import io.codenode.kotlincompiler.generator.RuntimeControllerGenerator
import io.codenode.kotlincompiler.generator.RuntimeControllerInterfaceGenerator
import io.codenode.kotlincompiler.generator.RuntimeControllerAdapterGenerator
import io.codenode.kotlincompiler.generator.RuntimeViewModelGenerator
import io.codenode.kotlincompiler.generator.UserInterfaceStubGenerator
import io.codenode.kotlincompiler.generator.RepositoryCodeGenerator
import io.codenode.kotlincompiler.generator.EntityProperty
import io.codenode.kotlincompiler.generator.EntityInfo
import io.codenode.kotlincompiler.generator.EntityModuleSpec
import io.codenode.kotlincompiler.generator.EntityModuleGenerator
import io.codenode.grapheditor.repository.FileCustomNodeRepository
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
        const val USER_INTERFACE_SUBPACKAGE = "userInterface"
    }

    private val moduleGenerator = ModuleGenerator()
    private val flowKtGenerator = FlowKtGenerator()
    private val runtimeFlowGenerator = RuntimeFlowGenerator()
    private val runtimeControllerGenerator = RuntimeControllerGenerator()
    private val runtimeControllerInterfaceGenerator = RuntimeControllerInterfaceGenerator()
    private val runtimeControllerAdapterGenerator = RuntimeControllerAdapterGenerator()
    private val runtimeViewModelGenerator = RuntimeViewModelGenerator()
    private val userInterfaceStubGenerator = UserInterfaceStubGenerator()
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
            val userInterfacePackage = "$basePackage.$USER_INTERFACE_SUBPACKAGE"

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
            createDirectoryStructure(moduleDir, userInterfacePackage, flowGraph)

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
                flowKtGenerator.generateFlowKt(flowGraph, basePackage, null, ipTypeNames),
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

            // Generate user interface stub (write-once, preserves existing UI code)
            generateUserInterfaceStub(
                flowGraph, moduleDir, userInterfacePackage, generatedPackage,
                filesCreated
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

    private val entityModuleGenerator = EntityModuleGenerator()

    /**
     * Saves a complete entity CRUD module generated from an EntityModuleSpec.
     *
     * Creates the module directory, writes all entity module files (nodes, FlowGraph,
     * ViewModel, UI, persistence wiring), and writes persistence files (Entity, DAO,
     * Repository) to the shared persistence module. Regenerates AppDatabase.kt with
     * all known entities.
     *
     * @param spec The entity module specification
     * @param moduleOutputDir Parent directory where the module will be created
     * @param persistenceDir The persistence module source directory
     * @return ModuleSaveResult with success status and file tracking
     */
    fun saveEntityModule(
        spec: EntityModuleSpec,
        moduleOutputDir: File,
        persistenceDir: File
    ): ModuleSaveResult {
        return try {
            val output = entityModuleGenerator.generateModule(spec)
            val moduleDir = File(moduleOutputDir, spec.pluralName)

            val filesCreated = mutableListOf<String>()
            val filesOverwritten = mutableListOf<String>()

            // Create module directory structure
            createDirectoryStructure(moduleDir, spec.basePackage, output.flowGraph)
            createDirectoryStructure(moduleDir, "${spec.basePackage}.$GENERATED_SUBPACKAGE", output.flowGraph)
            createDirectoryStructure(moduleDir, "${spec.basePackage}.$PROCESSING_LOGIC_SUBPACKAGE", output.flowGraph)
            createDirectoryStructure(moduleDir, "${spec.basePackage}.$USER_INTERFACE_SUBPACKAGE", output.flowGraph)

            // Write build.gradle.kts (only if new)
            writeFileIfNew(
                File(moduleDir, "build.gradle.kts"),
                moduleGenerator.generateBuildGradle(output.flowGraph, spec.pluralName, isEntityModule = true),
                "build.gradle.kts",
                filesCreated
            )

            // Write all module files
            for ((relativePath, content) in output.moduleFiles) {
                val file = File(moduleDir, relativePath)
                file.parentFile.mkdirs()
                writeFileAlways(file, content, relativePath, filesCreated, filesOverwritten)
            }

            // Write persistence files to the shared persistence module
            for ((relativePath, content) in output.persistenceFiles) {
                val file = File(persistenceDir, relativePath.removePrefix("src/commonMain/kotlin/${spec.persistencePackage.replace(".", "/")}/"))
                    .let { File(persistenceDir, it.name) }
                if (!file.exists()) {
                    file.writeText(content)
                    filesCreated.add("persistence/${file.name}")
                }
            }

            // Regenerate AppDatabase.kt with all entities
            regenerateAppDatabase(persistenceDir, spec, filesOverwritten)

            // Add module to settings.gradle.kts and graphEditor/build.gradle.kts
            addModuleToGradleFiles(moduleOutputDir, spec.pluralName, filesOverwritten)

            // Wire module into graphEditor: PreviewProvider, ModuleSessionFactory, Main.kt
            wireGraphEditorIntegration(moduleOutputDir, spec, filesCreated, filesOverwritten)

            ModuleSaveResult(
                success = true,
                moduleDir = moduleDir,
                filesCreated = filesCreated,
                filesOverwritten = filesOverwritten
            )
        } catch (e: Exception) {
            ModuleSaveResult(
                success = false,
                errorMessage = "Failed to save entity module: ${e.message}"
            )
        }
    }

    /**
     * Scans the persistence directory for all *Entity.kt files, collects entity info,
     * and regenerates AppDatabase.kt with the complete entity list.
     */
    private fun regenerateAppDatabase(
        persistenceDir: File,
        spec: EntityModuleSpec,
        filesOverwritten: MutableList<String>
    ) {
        val persistencePackage = spec.persistencePackage

        // Scan for all Entity files in the persistence directory
        val entityFiles = persistenceDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith("Entity.kt") && it.name != "BaseEntity.kt" }
            ?: emptyList()

        val entityInfos = entityFiles.map { file ->
            val entityClassName = file.nameWithoutExtension // e.g., "UserProfileEntity"
            val entityName = entityClassName.removeSuffix("Entity") // e.g., "UserProfile"
            EntityInfo(
                entityName = entityName,
                tableName = entityName.lowercase() + "s",
                daoName = "${entityName}Dao"
            )
        }

        if (entityInfos.isNotEmpty()) {
            val appDatabaseContent = repositoryCodeGenerator.generateDatabase(entityInfos, persistencePackage)
            val appDatabaseFile = File(persistenceDir, "AppDatabase.kt")
            appDatabaseFile.writeText(appDatabaseContent)
            filesOverwritten.add("persistence/AppDatabase.kt")
        }
    }

    /**
     * Adds the module include to settings.gradle.kts and the implementation dependency
     * to graphEditor/build.gradle.kts, if not already present.
     */
    private fun addModuleToGradleFiles(
        projectDir: File,
        moduleName: String,
        filesOverwritten: MutableList<String>
    ) {
        val includeEntry = "include(\":$moduleName\")"
        val implEntry = "implementation(project(\":$moduleName\"))"

        // Update settings.gradle.kts
        val settingsFile = File(projectDir, "settings.gradle.kts")
        if (settingsFile.exists()) {
            val content = settingsFile.readText()
            if (!content.contains(includeEntry)) {
                // Append before the final blank lines
                val trimmed = content.trimEnd()
                settingsFile.writeText("$trimmed\n$includeEntry\n")
                filesOverwritten.add("settings.gradle.kts")
            }
        }

        // Update graphEditor/build.gradle.kts
        val buildFile = File(projectDir, "graphEditor/build.gradle.kts")
        if (buildFile.exists()) {
            val content = buildFile.readText()
            if (!content.contains(implEntry)) {
                // Insert after the last implementation(project(":...")) line
                val insertAfter = "implementation(project(\":persistence\"))"
                if (content.contains(insertAfter)) {
                    val updated = content.replace(
                        insertAfter,
                        "$insertAfter\n                $implEntry"
                    )
                    buildFile.writeText(updated)
                    filesOverwritten.add("graphEditor/build.gradle.kts")
                }
            }
        }
    }

    /**
     * Wires a new entity module into graphEditor: creates PreviewProvider,
     * adds ModuleSessionFactory case, and adds Main.kt registrations.
     */
    private fun wireGraphEditorIntegration(
        projectDir: File,
        spec: EntityModuleSpec,
        filesCreated: MutableList<String>,
        filesOverwritten: MutableList<String>
    ) {
        val entityName = spec.entityName
        val pluralName = spec.pluralName
        val packageLower = pluralName.lowercase()
        val graphEditorUiDir = File(projectDir, "graphEditor/src/jvmMain/kotlin/ui")

        // 1. Generate PreviewProvider file
        val providerFile = File(graphEditorUiDir, "${pluralName}PreviewProvider.kt")
        if (!providerFile.exists()) {
            providerFile.writeText(buildString {
                appendLine("/*")
                appendLine(" * ${pluralName}PreviewProvider - Provides $pluralName preview composables for the runtime panel")
                appendLine(" * License: Apache 2.0")
                appendLine(" */")
                appendLine()
                appendLine("package io.codenode.grapheditor.ui")
                appendLine()
                appendLine("import io.codenode.$packageLower.${pluralName}ViewModel")
                appendLine("import io.codenode.$packageLower.userInterface.$pluralName")
                appendLine()
                appendLine("/**")
                appendLine(" * Provides preview composables that render $pluralName components,")
                appendLine(" * driven by the RuntimeSession's ViewModel state.")
                appendLine(" */")
                appendLine("object ${pluralName}PreviewProvider {")
                appendLine()
                appendLine("    /**")
                appendLine("     * Registers $pluralName preview composables with the PreviewRegistry.")
                appendLine("     */")
                appendLine("    fun register() {")
                appendLine("        PreviewRegistry.register(\"$pluralName\") { viewModel, modifier ->")
                appendLine("            val vm = viewModel as ${pluralName}ViewModel")
                appendLine("            $pluralName(viewModel = vm, modifier = modifier)")
                appendLine("        }")
                appendLine("    }")
                appendLine("}")
            })
            filesCreated.add("graphEditor/ui/${pluralName}PreviewProvider.kt")
        }

        // 2. Add case to ModuleSessionFactory
        val factoryFile = File(graphEditorUiDir, "ModuleSessionFactory.kt")
        if (factoryFile.exists()) {
            var content = factoryFile.readText()

            // Add imports if not present
            val importBlock = buildString {
                appendLine("import io.codenode.$packageLower.${pluralName}ViewModel")
                appendLine("import io.codenode.$packageLower.generated.${pluralName}Controller")
                appendLine("import io.codenode.$packageLower.generated.${pluralName}ControllerAdapter")
                appendLine("import io.codenode.persistence.${entityName}Dao")
                append("import io.codenode.$packageLower.${pluralName.replaceFirstChar { it.lowercase() }}FlowGraph")
            }
            val flowGraphImport = "import io.codenode.$packageLower.${pluralName.replaceFirstChar { it.lowercase() }}FlowGraph"
            if (!content.contains(flowGraphImport)) {
                // Insert imports after the last existing import line
                val lastImportIndex = content.lastIndexOf("\nimport ")
                val endOfLastImport = content.indexOf('\n', lastImportIndex + 1)
                content = content.substring(0, endOfLastImport + 1) +
                    importBlock + "\n" +
                    content.substring(endOfLastImport + 1)
            }

            // Add DAO injection if not present
            val daoInjection = "private val ${entityName.replaceFirstChar { it.lowercase() }}Dao: ${entityName}Dao by inject()"
            if (!content.contains(daoInjection)) {
                val insertAfter = content.lastIndexOf("by inject()")
                val endOfLine = content.indexOf('\n', insertAfter)
                content = content.substring(0, endOfLine + 1) +
                    "    $daoInjection\n" +
                    content.substring(endOfLine + 1)
            }

            // Add when branch if not present
            val flowGraphVarName = pluralName.replaceFirstChar { it.lowercase() } + "FlowGraph"
            val sessionMethodName = "create${pluralName}Session"
            if (!content.contains("\"$pluralName\"")) {
                val elseNull = "else -> null"
                content = content.replace(
                    elseNull,
                    "\"$pluralName\" -> $sessionMethodName(editorFlowGraph)\n            $elseNull"
                )

                // Add factory method before the closing brace
                val factoryMethod = buildString {
                    appendLine()
                    appendLine("    private fun $sessionMethodName(editorFlowGraph: FlowGraph?): RuntimeSession {")
                    appendLine("        val controller = ${pluralName}Controller($flowGraphVarName)")
                    appendLine("        controller.start()")
                    appendLine("        val adapter = ${pluralName}ControllerAdapter(controller)")
                    appendLine("        val viewModel = ${pluralName}ViewModel(adapter, ${entityName.replaceFirstChar { it.lowercase() }}Dao)")
                    appendLine("        return RuntimeSession(controller, viewModel, editorFlowGraph ?: $flowGraphVarName)")
                    appendLine("    }")
                }
                val lastBrace = content.lastIndexOf('}')
                content = content.substring(0, lastBrace) + factoryMethod + content.substring(lastBrace)
            }

            factoryFile.writeText(content)
            filesOverwritten.add("graphEditor/ui/ModuleSessionFactory.kt")
        }

        // 3. Add PreviewProvider registration in Main.kt
        val mainFile = File(projectDir, "graphEditor/src/jvmMain/kotlin/Main.kt")
        if (mainFile.exists()) {
            var content = mainFile.readText()

            // Add provider import if not present
            val providerImport = "import io.codenode.grapheditor.ui.${pluralName}PreviewProvider"
            if (!content.contains(providerImport)) {
                // Find the last PreviewProvider import line
                val lines = content.lines()
                val lastProviderIdx = lines.indexOfLast { it.contains("PreviewProvider") && it.trimStart().startsWith("import") }
                if (lastProviderIdx >= 0) {
                    val mutableLines = lines.toMutableList()
                    mutableLines.add(lastProviderIdx + 1, providerImport)
                    content = mutableLines.joinToString("\n")
                }
            }

            // Add provider registration if not present
            val registerCall = "${pluralName}PreviewProvider.register()"
            if (!content.contains(registerCall)) {
                val lastRegister = content.lastIndexOf("PreviewProvider.register()")
                val endOfLine = content.indexOf('\n', lastRegister)
                content = content.substring(0, endOfLine + 1) +
                    "        $registerCall\n" +
                    content.substring(endOfLine + 1)
            }

            // Add Koin module import if not present
            val koinModuleImport = "import io.codenode.$packageLower.${pluralName.replaceFirstChar { it.lowercase() }}Module"
            if (!content.contains(koinModuleImport)) {
                val geoImport = "import io.codenode.geolocations.geoLocationsModule"
                val geoImportEnd = content.indexOf(geoImport)
                if (geoImportEnd >= 0) {
                    val endOfGeoImport = content.indexOf('\n', geoImportEnd)
                    content = content.substring(0, endOfGeoImport + 1) +
                        "$koinModuleImport\n" +
                        content.substring(endOfGeoImport + 1)
                }
            }

            // Add DAO single in Koin startKoin block if not present
            val daoSingle = "single { DatabaseModule.getDatabase().${entityName.replaceFirstChar { it.lowercase() }}Dao() }"
            if (!content.contains(daoSingle)) {
                val lastDaoSingle = content.lastIndexOf("single { DatabaseModule.getDatabase().")
                val endOfLine = content.indexOf('\n', lastDaoSingle)
                content = content.substring(0, endOfLine + 1) +
                    "                $daoSingle\n" +
                    content.substring(endOfLine + 1)
            }

            // Add Koin module reference if not present
            val koinModuleRef = "${pluralName.replaceFirstChar { it.lowercase() }}Module"
            val koinModulesSection = content.indexOf("geoLocationsModule")
            if (koinModulesSection >= 0 && !content.contains("$koinModuleRef\n") && !content.contains("$koinModuleRef,")) {
                val endOfGeoModule = content.indexOf('\n', koinModulesSection)
                // Check if geoLocationsModule line ends with comma or not
                val geoLine = content.substring(koinModulesSection, endOfGeoModule).trim()
                if (!geoLine.endsWith(",")) {
                    // Add comma to geoLocationsModule line
                    content = content.substring(0, endOfGeoModule) + "," +
                        content.substring(endOfGeoModule)
                }
                val updatedEndOfGeoModule = content.indexOf('\n', koinModulesSection)
                content = content.substring(0, updatedEndOfGeoModule + 1) +
                    "            $koinModuleRef\n" +
                    content.substring(updatedEndOfGeoModule + 1)
            }

            mainFile.writeText(content)
            filesOverwritten.add("graphEditor/Main.kt")
        }
    }

    /**
     * Removes a previously created entity module and all its artifacts.
     *
     * Cleans up: custom node definitions, module directory, persistence files,
     * AppDatabase.kt, and Gradle entries. Each step is independent and tolerant
     * of missing artifacts.
     *
     * @param entityName PascalCase entity name (e.g., "GeoLocation")
     * @param moduleName Plural module name (e.g., "GeoLocations")
     * @param moduleDir Path to the module directory
     * @param persistenceDir Path to the persistence source directory
     * @param projectDir Project root directory (contains settings.gradle.kts)
     * @param customNodeRepository Repository for custom node CRUD
     * @param sourceIPTypeId UUID of the source IP Type
     * @return Summary string describing what was removed
     */
    fun removeEntityModule(
        entityName: String,
        moduleName: String,
        moduleDir: File,
        persistenceDir: File,
        projectDir: File,
        customNodeRepository: FileCustomNodeRepository,
        sourceIPTypeId: String
    ): String {
        val results = mutableListOf<String>()

        // 1. Remove custom node definitions matching sourceIPTypeId
        try {
            val nodesToRemove = customNodeRepository.getAll()
                .filter { it.sourceIPTypeId == sourceIPTypeId }
            var nodesRemoved = 0
            for (node in nodesToRemove) {
                if (customNodeRepository.remove(node.id)) {
                    nodesRemoved++
                }
            }
            if (nodesRemoved > 0) results.add("$nodesRemoved node${if (nodesRemoved != 1) "s" else ""}")
        } catch (e: Exception) {
            results.add("node removal failed: ${e.message}")
        }

        // 2. Delete module directory recursively
        try {
            if (moduleDir.exists()) {
                moduleDir.deleteRecursively()
                results.add("module directory")
            }
        } catch (e: Exception) {
            results.add("directory deletion failed: ${e.message}")
        }

        // 3. Remove persistence files
        try {
            val persistenceFileNames = listOf(
                "${entityName}Entity.kt",
                "${entityName}Dao.kt",
                "${entityName}Repository.kt"
            )
            var persistenceRemoved = 0
            for (fileName in persistenceFileNames) {
                val file = File(persistenceDir, fileName)
                if (file.exists() && file.delete()) {
                    persistenceRemoved++
                }
            }
            if (persistenceRemoved > 0) results.add("$persistenceRemoved persistence file${if (persistenceRemoved != 1) "s" else ""}")
        } catch (e: Exception) {
            results.add("persistence removal failed: ${e.message}")
        }

        // 4. Regenerate AppDatabase.kt from remaining entities
        try {
            val entityFiles = persistenceDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith("Entity.kt") && it.name != "BaseEntity.kt" }
                ?: emptyList()

            val entityInfos = entityFiles.map { file ->
                val entityClassName = file.nameWithoutExtension
                val name = entityClassName.removeSuffix("Entity")
                EntityInfo(
                    entityName = name,
                    tableName = name.lowercase() + "s",
                    daoName = "${name}Dao"
                )
            }

            val persistencePackage = "io.codenode.persistence"
            if (entityInfos.isNotEmpty()) {
                val appDatabaseContent = repositoryCodeGenerator.generateDatabase(entityInfos, persistencePackage)
                File(persistenceDir, "AppDatabase.kt").writeText(appDatabaseContent)
                results.add("AppDatabase updated")
            } else {
                // No entities left — write minimal AppDatabase
                val appDatabaseFile = File(persistenceDir, "AppDatabase.kt")
                appDatabaseFile.writeText(buildString {
                    appendLine("package io.codenode.persistence")
                    appendLine()
                    appendLine("import androidx.room.Database")
                    appendLine("import androidx.room.RoomDatabase")
                    appendLine("import androidx.room.RoomDatabaseConstructor")
                    appendLine("import androidx.room.ConstructedBy")
                    appendLine()
                    appendLine("@Database(entities = [], version = 1)")
                    appendLine("@ConstructedBy(AppDatabaseConstructor::class)")
                    appendLine("abstract class AppDatabase : RoomDatabase()")
                    appendLine()
                    appendLine("@Suppress(\"EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA\")")
                    appendLine("expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>")
                })
                results.add("AppDatabase updated (no entities)")
            }
        } catch (e: Exception) {
            results.add("AppDatabase regeneration failed: ${e.message}")
        }

        // 5. Remove Gradle entries
        try {
            var gradleRemoved = 0
            val includeEntry = "include(\":$moduleName\")"
            val settingsFile = File(projectDir, "settings.gradle.kts")
            if (settingsFile.exists()) {
                val lines = settingsFile.readLines()
                val filtered = lines.filter { it.trim() != includeEntry }
                if (filtered.size < lines.size) {
                    settingsFile.writeText(filtered.joinToString("\n") + "\n")
                    gradleRemoved++
                }
            }

            val implEntry = "implementation(project(\":$moduleName\"))"
            val buildFile = File(projectDir, "graphEditor/build.gradle.kts")
            if (buildFile.exists()) {
                val lines = buildFile.readLines()
                val filtered = lines.filter { !it.trim().contains(implEntry) }
                if (filtered.size < lines.size) {
                    buildFile.writeText(filtered.joinToString("\n") + "\n")
                    gradleRemoved++
                }
            }

            if (gradleRemoved > 0) results.add("$gradleRemoved gradle entr${if (gradleRemoved != 1) "ies" else "y"}")
        } catch (e: Exception) {
            results.add("gradle removal failed: ${e.message}")
        }

        // 6. Remove graphEditor integration (PreviewProvider, ModuleSessionFactory, Main.kt)
        try {
            unwireGraphEditorIntegration(projectDir, entityName, moduleName, results)
        } catch (e: Exception) {
            results.add("graphEditor unwiring failed: ${e.message}")
        }

        return if (results.isNotEmpty()) {
            "Removed $moduleName module: ${results.joinToString(", ")}"
        } else {
            "No artifacts found to remove for $moduleName"
        }
    }

    /**
     * Removes graphEditor integration artifacts for a module:
     * PreviewProvider file, ModuleSessionFactory entries, and Main.kt registrations.
     */
    private fun unwireGraphEditorIntegration(
        projectDir: File,
        entityName: String,
        moduleName: String,
        results: MutableList<String>
    ) {
        val packageLower = moduleName.lowercase()
        val graphEditorUiDir = File(projectDir, "graphEditor/src/jvmMain/kotlin/ui")
        var unwired = 0

        // 1. Delete PreviewProvider file
        val providerFile = File(graphEditorUiDir, "${moduleName}PreviewProvider.kt")
        if (providerFile.exists() && providerFile.delete()) {
            unwired++
        }

        // 2. Remove entries from ModuleSessionFactory
        val factoryFile = File(graphEditorUiDir, "ModuleSessionFactory.kt")
        if (factoryFile.exists()) {
            var content = factoryFile.readText()
            val originalLength = content.length

            // Remove imports for this module
            val importPatterns = listOf(
                "import io.codenode.$packageLower.",
                "import io.codenode.persistence.${entityName}Dao"
            )
            val lines = content.lines().toMutableList()
            lines.removeAll { line -> importPatterns.any { pattern -> line.trimStart().startsWith(pattern) } }

            // Remove DAO injection line
            lines.removeAll { it.contains("${entityName}Dao by inject()") }

            // Remove when branch
            lines.removeAll { it.contains("\"$moduleName\" ->") }

            // Remove factory method (find and remove the entire method)
            content = lines.joinToString("\n")
            val methodStart = content.indexOf("    private fun create${moduleName}Session(")
            if (methodStart >= 0) {
                // Find the closing brace of the method (count braces)
                var braceCount = 0
                var methodEnd = methodStart
                var foundFirstBrace = false
                for (i in methodStart until content.length) {
                    if (content[i] == '{') {
                        braceCount++
                        foundFirstBrace = true
                    } else if (content[i] == '}') {
                        braceCount--
                        if (foundFirstBrace && braceCount == 0) {
                            methodEnd = i + 1
                            break
                        }
                    }
                }
                // Include any leading blank line
                var start = methodStart
                while (start > 0 && content[start - 1] == '\n') start--
                if (start > 0) start++ // keep one newline
                content = content.substring(0, start) + content.substring(methodEnd)
            }

            if (content.length != originalLength) {
                factoryFile.writeText(content)
                unwired++
            }
        }

        // 3. Remove entries from Main.kt
        val mainFile = File(projectDir, "graphEditor/src/jvmMain/kotlin/Main.kt")
        if (mainFile.exists()) {
            val content = mainFile.readText()
            val lines = content.lines().toMutableList()
            val originalSize = lines.size

            // Remove PreviewProvider import
            lines.removeAll { it.contains("import io.codenode.grapheditor.ui.${moduleName}PreviewProvider") }

            // Remove PreviewProvider registration
            lines.removeAll { it.contains("${moduleName}PreviewProvider.register()") }

            // Remove Koin module import
            lines.removeAll { it.contains("import io.codenode.$packageLower.${moduleName.replaceFirstChar { it.lowercase() }}Module") }

            // Remove DAO single
            lines.removeAll { it.contains("${entityName.replaceFirstChar { it.lowercase() }}Dao()") && it.contains("DatabaseModule.getDatabase()") }

            // Remove Koin module reference
            val koinModuleRef = "${moduleName.replaceFirstChar { it.lowercase() }}Module"
            lines.removeAll { it.trim() == koinModuleRef || it.trim() == "$koinModuleRef," }

            // Fix trailing comma on previous Koin module line if needed
            val koinModulesIdx = lines.indexOfLast { it.contains("Module") && it.trim().endsWith(",") && lines.indexOf(it) > lines.size - 20 }
            if (koinModulesIdx >= 0) {
                val nextNonBlank = lines.drop(koinModulesIdx + 1).firstOrNull { it.isNotBlank() }
                if (nextNonBlank?.trim()?.startsWith(")") == true) {
                    lines[koinModulesIdx] = lines[koinModulesIdx].trimEnd().removeSuffix(",")
                }
            }

            if (lines.size != originalSize) {
                mainFile.writeText(lines.joinToString("\n"))
                unwired++
            }
        }

        if (unwired > 0) results.add("graphEditor integration ($unwired file${if (unwired != 1) "s" else ""})")
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
     * Generates the user interface Composable stub file in the userInterface subpackage.
     *
     * Write-once: only creates the file if it doesn't already exist, preserving
     * any user-implemented UI code on re-save.
     *
     * @param flowGraph The flow graph
     * @param moduleDir The module root directory
     * @param userInterfacePackage The userInterface subpackage name
     * @param generatedPackage The generated package name (for ViewModel import)
     * @param filesCreated List to track created files
     */
    private fun generateUserInterfaceStub(
        flowGraph: FlowGraph,
        moduleDir: File,
        userInterfacePackage: String,
        generatedPackage: String,
        filesCreated: MutableList<String>
    ) {
        val uiPackagePath = userInterfacePackage.replace(".", "/")
        val stubFileName = userInterfaceStubGenerator.getStubFileName(flowGraph)
        val stubFile = File(moduleDir, "src/commonMain/kotlin/$uiPackagePath/$stubFileName")
        val relativePath = "src/commonMain/kotlin/$uiPackagePath/$stubFileName"

        if (!stubFile.exists()) {
            val content = userInterfaceStubGenerator.generate(flowGraph, userInterfacePackage, generatedPackage)
            stubFile.writeText(content)
            filesCreated.add(relativePath)
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
            "${effectiveModuleName}Controller.kt" to runtimeControllerGenerator.generate(flowGraph, generatedPackage, processingLogicPackage, basePackage),
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

        // All ProcessLogic stubs are orphaned — CodeNodeDefinitions handle their own logic
        val existingComponentFiles = sourceDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith("ProcessLogic.kt") }
            ?: emptyList()

        for (file in existingComponentFiles) {
            val relativePath = "src/commonMain/kotlin/$packagePath/${file.name}"
            file.delete()
            filesDeleted.add(relativePath)
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
