/*
 * ModuleSaveService
 * Creates KMP module structure when saving a FlowGraph
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.save

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.flowgraphgenerate.generator.FlowKtGenerator
import io.codenode.flowgraphgenerate.generator.ModuleGenerator
import io.codenode.flowgraphgenerate.generator.RuntimeFlowGenerator
import io.codenode.flowgraphgenerate.generator.RuntimeControllerGenerator
import io.codenode.flowgraphgenerate.generator.RuntimeControllerInterfaceGenerator
import io.codenode.flowgraphgenerate.generator.RuntimeControllerAdapterGenerator
import io.codenode.flowgraphgenerate.generator.RuntimeViewModelGenerator
import io.codenode.flowgraphgenerate.generator.UserInterfaceStubGenerator
import io.codenode.flowgraphgenerate.generator.RepositoryCodeGenerator
import io.codenode.flowgraphgenerate.generator.EntityProperty
import io.codenode.flowgraphgenerate.generator.EntityInfo
import io.codenode.flowgraphgenerate.generator.EntityModuleSpec
import io.codenode.flowgraphgenerate.generator.EntityModuleGenerator
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
 * 4 runtime files under generated/, and a ViewModel stub in the base package.
 *
 * The ViewModel stub contains a marker-delineated Module Properties section
 * that is selectively regenerated on re-save while preserving user code
 * outside the markers.
 */
class ModuleSaveService {

    companion object {
        const val DEFAULT_PACKAGE_PREFIX = "io.codenode"
        const val FLOW_SUBPACKAGE = "flow"
        const val CONTROLLER_SUBPACKAGE = "controller"
        const val VIEWMODEL_SUBPACKAGE = "viewmodel"
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
     * Saves only the .flow.kt file without generating any module scaffolding.
     *
     * If an existing module directory is found (has build.gradle.kts), writes the .flow.kt
     * into the module's source set. Otherwise, writes a flat .flow.kt file directly
     * into the output directory. No directories, gradle files, runtime controllers,
     * ViewModels, UI stubs, or persistence files are created.
     *
     * @param flowGraph The flow graph to save
     * @param outputDir Parent directory where the module directory lives
     * @param moduleName Module name (default: derived from FlowGraph name)
     * @param ipTypeNames Map of IP type id → display name for port type resolution
     * @param codeNodeClassLookup Function to resolve CodeNode class names from node names
     * @return ModuleSaveResult with success status and the single .flow.kt file tracked
     */
    fun saveFlowKtOnly(
        flowGraph: FlowGraph,
        outputDir: File,
        moduleName: String? = null,
        ipTypeNames: Map<String, String> = emptyMap(),
        codeNodeClassLookup: (String) -> String? = { null }
    ): ModuleSaveResult {
        return try {
            val enrichedFlowGraph = enrichWithCodeNodeMetadata(flowGraph, codeNodeClassLookup)
            val effectiveModuleName = moduleName ?: deriveModuleName(enrichedFlowGraph.name)
            val basePackage = "$DEFAULT_PACKAGE_PREFIX.${effectiveModuleName.lowercase()}"

            val filesCreated = mutableListOf<String>()
            val filesOverwritten = mutableListOf<String>()

            val moduleDir = File(outputDir, effectiveModuleName)
            val existingModule = moduleDir.exists() && File(moduleDir, "build.gradle.kts").exists()

            val flowPackage = "$basePackage.$FLOW_SUBPACKAGE"
            val flowKtContent = flowKtGenerator.generateFlowKt(
                enrichedFlowGraph, flowPackage, null, ipTypeNames
            )

            val targetFile: File
            val relativePath: String
            if (existingModule) {
                val flowPackagePath = flowPackage.replace(".", "/")
                val flowKtFileName = "${effectiveModuleName}.flow.kt"
                relativePath = "src/commonMain/kotlin/$flowPackagePath/$flowKtFileName"
                targetFile = File(moduleDir, relativePath)
            } else {
                val flowKtFileName = "${effectiveModuleName}.flow.kt"
                relativePath = flowKtFileName
                targetFile = File(outputDir, flowKtFileName)
            }

            writeFileAlways(targetFile, flowKtContent, relativePath, filesCreated, filesOverwritten)

            ModuleSaveResult(
                success = true,
                moduleDir = if (existingModule) moduleDir else null,
                filesCreated = filesCreated,
                filesOverwritten = filesOverwritten,
                filesDeleted = emptyList()
            )
        } catch (e: Exception) {
            ModuleSaveResult(
                success = false,
                errorMessage = "Failed to save .flow.kt: ${e.message}"
            )
        }
    }

    /**
     * Saves a FlowGraph as a complete KMP module.
     *
     * Creates the full module in a single call: module directory, gradle files,
     * .flow.kt in the base package source set, 4 runtime files, and a ViewModel stub
     * in the base package. On re-save, .flow.kt and runtime files are always overwritten,
     * and the ViewModel's Module Properties section is selectively regenerated while
     * preserving user code.
     *
     * @param flowGraph The flow graph to save
     * @param outputDir Parent directory where the module will be created
     * @param packageName Package name for generated code (default: io.codenode.{modulename})
     * @param moduleName Module name (default: derived from FlowGraph name)
     * @return ModuleSaveResult with success status, module directory, and file tracking
     */
    fun saveModule(
        flowGraph: FlowGraph,
        outputDir: File,
        packageName: String? = null,
        moduleName: String? = null,
        ipTypeProperties: Map<String, List<EntityProperty>> = emptyMap(),
        ipTypeNames: Map<String, String> = emptyMap(),
        codeNodeClassLookup: (String) -> String? = { null }
    ): ModuleSaveResult {
        return try {
            val enrichedFlowGraph = enrichWithCodeNodeMetadata(flowGraph, codeNodeClassLookup)
            val effectiveModuleName = moduleName ?: deriveModuleName(enrichedFlowGraph.name)

            val basePackage = packageName ?: "$DEFAULT_PACKAGE_PREFIX.${effectiveModuleName.lowercase()}"
            val flowPackage = "$basePackage.$FLOW_SUBPACKAGE"
            val controllerPackage = "$basePackage.$CONTROLLER_SUBPACKAGE"
            val viewModelPackage = "$basePackage.$VIEWMODEL_SUBPACKAGE"
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
            createDirectoryStructure(moduleDir, basePackage, enrichedFlowGraph)
            createDirectoryStructure(moduleDir, flowPackage, enrichedFlowGraph)
            createDirectoryStructure(moduleDir, controllerPackage, enrichedFlowGraph)
            createDirectoryStructure(moduleDir, viewModelPackage, enrichedFlowGraph)
            createDirectoryStructure(moduleDir, userInterfacePackage, enrichedFlowGraph)

            // Write gradle files (only if they don't exist)
            writeFileIfNew(
                File(moduleDir, "build.gradle.kts"),
                moduleGenerator.generateBuildGradle(enrichedFlowGraph, effectiveModuleName),
                "build.gradle.kts",
                filesCreated
            )
            writeFileIfNew(
                File(moduleDir, "settings.gradle.kts"),
                generateSettingsGradle(effectiveModuleName),
                "settings.gradle.kts",
                filesCreated
            )

            // Write .flow.kt in flow/ subdirectory (always overwrite)
            val flowPackagePath = flowPackage.replace(".", "/")
            val flowKtFileName = "${effectiveModuleName}.flow.kt"
            val flowKtRelativePath = "src/commonMain/kotlin/$flowPackagePath/$flowKtFileName"
            writeFileAlways(
                File(moduleDir, flowKtRelativePath),
                flowKtGenerator.generateFlowKt(enrichedFlowGraph, flowPackage, null, ipTypeNames),
                flowKtRelativePath,
                filesCreated,
                filesOverwritten
            )

            // Generate runtime files: Flow.kt in flow/, Controller*.kt in controller/
            generateRuntimeFilesTracked(
                enrichedFlowGraph, moduleDir, basePackage, flowPackage, controllerPackage, viewModelPackage,
                effectiveModuleName, filesCreated, filesOverwritten
            )

            // Generate ViewModel stub in viewmodel/ (selective regeneration)
            generateViewModelStub(
                enrichedFlowGraph, moduleDir, basePackage, viewModelPackage, controllerPackage,
                effectiveModuleName, filesCreated, filesOverwritten
            )

            // Generate user interface stub (write-once, preserves existing UI code)
            generateUserInterfaceStub(
                enrichedFlowGraph, moduleDir, userInterfacePackage, viewModelPackage,
                filesCreated
            )

            // Generate persistence layer for repository nodes
            generatePersistenceFiles(
                enrichedFlowGraph, moduleDir, basePackage, ipTypeProperties,
                filesCreated, filesOverwritten
            )

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
     * Enriches FlowGraph nodes with _codeNodeClass metadata from the registry.
     * Existing modules saved before this metadata existed need enrichment so that
     * generators use the CodeNode-aware path instead of legacy CodeNodeFactory.
     */
    private fun enrichWithCodeNodeMetadata(
        flowGraph: FlowGraph,
        codeNodeClassLookup: (String) -> String?
    ): FlowGraph {
        val enrichedNodes = flowGraph.rootNodes.map { node ->
            if (node is CodeNode && node.configuration["_codeNodeClass"] == null) {
                val qualifiedName = codeNodeClassLookup(node.name)
                if (qualifiedName != null) {
                    node.copy(
                        configuration = node.configuration + mapOf(
                            "_codeNodeClass" to qualifiedName,
                            "_codeNodeDefinition" to "true"
                        )
                    )
                } else node
            } else node
        }
        return if (enrichedNodes !== flowGraph.rootNodes) {
            flowGraph.copy(rootNodes = enrichedNodes)
        } else flowGraph
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
            // Ensure iptypes/ is a proper Gradle KMP module
            ensureIPTypesModule(moduleOutputDir)

            // Add id field to IP type file if not present (needed for entity identity)
            addIdFieldToIPType(moduleOutputDir, spec)

            val output = entityModuleGenerator.generateModule(spec)
            val moduleDir = File(moduleOutputDir, spec.pluralName)

            val filesCreated = mutableListOf<String>()
            val filesOverwritten = mutableListOf<String>()

            // Create module directory structure
            createDirectoryStructure(moduleDir, spec.basePackage, output.flowGraph)
            createDirectoryStructure(moduleDir, "${spec.basePackage}.$FLOW_SUBPACKAGE", output.flowGraph)
            createDirectoryStructure(moduleDir, "${spec.basePackage}.$CONTROLLER_SUBPACKAGE", output.flowGraph)
            createDirectoryStructure(moduleDir, "${spec.basePackage}.$VIEWMODEL_SUBPACKAGE", output.flowGraph)
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

            // Write persistence files to the shared persistence module (in entity subdirectory)
            val entitySubDir = File(persistenceDir, spec.entityName)
            entitySubDir.mkdirs()
            for ((relativePath, content) in output.persistenceFiles) {
                val fileName = File(relativePath).name
                val file = File(entitySubDir, fileName)
                if (!file.exists()) {
                    file.writeText(content)
                    filesCreated.add("persistence/${spec.entityName}/$fileName")
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

        // Scan entity subdirectories for *Entity.kt files
        val entityFiles = (persistenceDir.listFiles() ?: emptyArray())
            .filter { it.isDirectory }
            .flatMap { subDir ->
                subDir.listFiles()?.filter { it.isFile && it.name.endsWith("Entity.kt") } ?: emptyList()
            }

        val entityInfos = entityFiles.map { file ->
            val entityClassName = file.nameWithoutExtension // e.g., "UserProfileEntity"
            val entityName = entityClassName.removeSuffix("Entity") // e.g., "UserProfile"
            val subPackage = "${persistencePackage}.${entityName.lowercase()}"
            EntityInfo(
                entityName = entityName,
                tableName = entityName.lowercase() + "s",
                daoName = "${entityName}Dao",
                subPackage = subPackage
            )
        }

        if (entityInfos.isNotEmpty()) {
            // Read current database version and increment
            val appDatabaseFile = File(persistenceDir, "AppDatabase.kt")
            val currentVersion = if (appDatabaseFile.exists()) {
                val versionMatch = Regex("""version\s*=\s*(\d+)""").find(appDatabaseFile.readText())
                versionMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            } else 0
            val newVersion = currentVersion + 1

            val appDatabaseContent = repositoryCodeGenerator.generateDatabase(entityInfos, persistencePackage, newVersion)
            appDatabaseFile.writeText(appDatabaseContent)
            filesOverwritten.add("persistence/AppDatabase.kt")
        }
    }

    /**
     * Adds `val id: Long = 0` to the IP type data class if not already present.
     * Entity modules need the id field for persistence identity tracking.
     */
    private fun addIdFieldToIPType(projectDir: File, spec: EntityModuleSpec) {
        val ipTypeFile = File(
            projectDir,
            "iptypes/src/commonMain/kotlin/${spec.ipTypesPackage.replace(".", "/")}/${spec.entityName}.kt"
        )
        if (!ipTypeFile.exists()) return

        val content = ipTypeFile.readText()
        if (content.contains("val id:")) return // Already has id field

        // Insert `val id: Long = 0,` as the first property in the data class
        val updated = content.replace(
            "data class ${spec.entityName}(",
            "data class ${spec.entityName}(\n    val id: Long = 0,"
        )
        ipTypeFile.writeText(updated)
    }

    /**
     * Ensures the iptypes/ directory is a proper Gradle KMP module so entity modules
     * can depend on it. Creates build.gradle.kts and adds include(":iptypes") to
     * settings.gradle.kts if not already present.
     */
    private fun ensureIPTypesModule(projectDir: File) {
        val ipTypesDir = File(projectDir, "iptypes")
        val buildFile = File(ipTypesDir, "build.gradle.kts")

        if (!buildFile.exists()) {
            ipTypesDir.mkdirs()
            buildFile.writeText(generateIPTypesBuildGradle())
        }

        // Ensure include(":iptypes") in settings.gradle.kts
        val settingsFile = File(projectDir, "settings.gradle.kts")
        if (settingsFile.exists()) {
            val content = settingsFile.readText()
            val includeEntry = "include(\":iptypes\")"
            if (!content.contains(includeEntry)) {
                val trimmed = content.trimEnd()
                settingsFile.writeText("$trimmed\n$includeEntry\n")
            }
        }

        // Ensure iptypes is in graphEditorRuntime in root build.gradle.kts
        addToGraphEditorRuntime(projectDir, "iptypes")
    }

    /**
     * Adds a module to the graphEditorRuntime configuration in the root build.gradle.kts.
     * This ensures the module is on the classpath when running ./gradlew runGraphEditor.
     */
    private fun addToGraphEditorRuntime(projectDir: File, moduleName: String) {
        val rootBuildFile = File(projectDir, "build.gradle.kts")
        if (!rootBuildFile.exists()) return

        val content = rootBuildFile.readText()
        val runtimeEntry = "graphEditorRuntime(project(\":$moduleName\"))"
        if (content.contains(runtimeEntry)) return

        // Insert after the last graphEditorRuntime(project(":...")) block
        val insertPattern = Regex("""graphEditorRuntime\(project\(":[^"]+"\)\)\s*\{[^}]*\}""")
        val matches = insertPattern.findAll(content).toList()
        val lastMatch = matches.lastOrNull() ?: return

        val newEntry = buildString {
            appendLine("    graphEditorRuntime(project(\":$moduleName\")) {")
            appendLine("        attributes {")
            appendLine("            attribute(Attribute.of(\"org.jetbrains.kotlin.platform.type\", String::class.java), \"jvm\")")
            appendLine("        }")
            append("    }")
        }
        val updated = content.substring(0, lastMatch.range.last + 1) +
            "\n$newEntry" +
            content.substring(lastMatch.range.last + 1)
        rootBuildFile.writeText(updated)
    }

    /**
     * Generates a minimal KMP build.gradle.kts for the shared iptypes module.
     * No persistence dependency — just a pure data class module.
     */
    private fun generateIPTypesBuildGradle(): String = buildString {
        appendLine("/*")
        appendLine(" * Shared IP Types module")
        appendLine(" * Contains data class definitions for IP types used across entity modules.")
        appendLine(" * Generated by CodeNodeIO")
        appendLine(" * License: Apache 2.0")
        appendLine(" */")
        appendLine()
        appendLine("plugins {")
        appendLine("    kotlin(\"multiplatform\")")
        appendLine("    kotlin(\"plugin.serialization\")")
        appendLine("    id(\"com.android.library\")")
        appendLine("}")
        appendLine()
        appendLine("kotlin {")
        appendLine("    jvm {")
        appendLine("        compilations.all {")
        appendLine("            kotlinOptions.jvmTarget = \"17\"")
        appendLine("        }")
        appendLine("    }")
        appendLine()
        appendLine("    androidTarget {")
        appendLine("        compilations.all {")
        appendLine("            kotlinOptions.jvmTarget = \"17\"")
        appendLine("        }")
        appendLine("    }")
        appendLine()
        appendLine("    listOf(")
        appendLine("        iosX64(),")
        appendLine("        iosArm64(),")
        appendLine("        iosSimulatorArm64()")
        appendLine("    ).forEach { iosTarget ->")
        appendLine("        iosTarget.binaries.framework {")
        appendLine("            baseName = \"iptypes\"")
        appendLine("            isStatic = true")
        appendLine("        }")
        appendLine("    }")
        appendLine()
        appendLine("    applyDefaultHierarchyTemplate()")
        appendLine()
        appendLine("    sourceSets {")
        appendLine("        val commonMain by getting {")
        appendLine("            dependencies {")
        appendLine("                implementation(\"org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0\")")
        appendLine("            }")
        appendLine("        }")
        appendLine()
        appendLine("        val commonTest by getting {")
        appendLine("            dependencies {")
        appendLine("                implementation(kotlin(\"test\"))")
        appendLine("            }")
        appendLine("        }")
        appendLine("    }")
        appendLine("}")
        appendLine()
        appendLine("android {")
        appendLine("    namespace = \"io.codenode.iptypes\"")
        appendLine("    compileSdk = 34")
        appendLine()
        appendLine("    defaultConfig {")
        appendLine("        minSdk = 24")
        appendLine("        targetSdk = 34")
        appendLine("    }")
        appendLine()
        appendLine("    compileOptions {")
        appendLine("        sourceCompatibility = JavaVersion.VERSION_17")
        appendLine("        targetCompatibility = JavaVersion.VERSION_17")
        appendLine("    }")
        appendLine("}")
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

        // Add module to graphEditorRuntime classpath in root build.gradle.kts
        addToGraphEditorRuntime(projectDir, moduleName)
        filesOverwritten.add("build.gradle.kts")
    }

    /**
     * Wires a new entity module into the project: creates PreviewProvider in the
     * module's jvmMain source set and updates PersistenceBootstrap with the new DAO.
     *
     * The graphEditor discovers PreviewProviders and CodeNodeDefinitions at runtime
     * via reflection — no modifications to Main.kt or ModuleSessionFactory needed.
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

        // 1. Generate PreviewProvider in the module's jvmMain source set.
        // NOTE: This file requires compileOnly("io.codenode:graphEditor") in the module's
        // jvmMain dependencies to compile. It is discovered and invoked via reflection by
        // the graphEditor's DynamicPreviewDiscovery at runtime.
        val moduleUiDir = File(projectDir, "$pluralName/src/jvmMain/kotlin/io/codenode/$packageLower/userInterface")
        moduleUiDir.mkdirs()
        val providerFile = File(moduleUiDir, "${pluralName}PreviewProvider.kt")
        if (!providerFile.exists()) {
            providerFile.writeText(buildString {
                appendLine("/*")
                appendLine(" * ${pluralName}PreviewProvider - Provides $pluralName preview composables for the runtime panel")
                appendLine(" * License: Apache 2.0")
                appendLine(" */")
                appendLine()
                appendLine("package io.codenode.$packageLower.userInterface")
                appendLine()
                appendLine("import io.codenode.$packageLower.${pluralName}ViewModel")
                appendLine("import io.codenode.previewapi.PreviewRegistry")
                appendLine()
                appendLine("/**")
                appendLine(" * Provides preview composables that render $pluralName components,")
                appendLine(" * driven by the RuntimeSession's ViewModel state.")
                appendLine(" * Discovered and invoked by the graphEditor at runtime via reflection.")
                appendLine(" *")
                appendLine(" * To compile this file, add to the module's build.gradle.kts:")
                appendLine(" *   val jvmMain by getting { dependencies { compileOnly(\"io.codenode:graphEditor\") } }")
                appendLine(" */")
                appendLine("object ${pluralName}PreviewProvider {")
                appendLine()
                appendLine("    fun register() {")
                appendLine("        PreviewRegistry.register(\"$pluralName\") { viewModel, modifier ->")
                appendLine("            val vm = viewModel as ${pluralName}ViewModel")
                appendLine("            $pluralName(viewModel = vm, modifier = modifier)")
                appendLine("        }")
                appendLine("    }")
                appendLine("}")
            })
            filesCreated.add("$pluralName/src/jvmMain/kotlin/.../userInterface/${pluralName}PreviewProvider.kt")
        }

        // 2. Update PersistenceBootstrap to register the new DAO
        val bootstrapFile = File(projectDir, "persistence/src/commonMain/kotlin/io/codenode/persistence/PersistenceBootstrap.kt")
        if (bootstrapFile.exists()) {
            var content = bootstrapFile.readText()
            val daoRegistration = "single<${entityName}Dao> { db.${entityName.replaceFirstChar { it.lowercase() }}Dao() }"
            if (!content.contains(daoRegistration)) {
                // Insert before the closing }) of the koin.loadModules block
                val insertPoint = content.lastIndexOf("})")
                if (insertPoint >= 0) {
                    content = content.substring(0, insertPoint) +
                        "    $daoRegistration\n            " +
                        content.substring(insertPoint)
                    bootstrapFile.writeText(content)
                    filesOverwritten.add("persistence/PersistenceBootstrap.kt")
                }
            }
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
     * @param sourceIPTypeId UUID of the source IP Type
     * @return Summary string describing what was removed
     */
    fun removeEntityModule(
        entityName: String,
        moduleName: String,
        moduleDir: File,
        persistenceDir: File,
        projectDir: File,
        sourceIPTypeId: String
    ): String {
        val results = mutableListOf<String>()

        // 1. Delete module directory recursively
        try {
            if (moduleDir.exists()) {
                moduleDir.deleteRecursively()
                results.add("module directory")
            }
        } catch (e: Exception) {
            results.add("directory deletion failed: ${e.message}")
        }

        // 3. Remove persistence entity subdirectory
        try {
            val entitySubDir = File(persistenceDir, entityName)
            if (entitySubDir.isDirectory && entitySubDir.deleteRecursively()) {
                results.add("persistence directory ${entityName}/")
            } else {
                // Fallback: try flat files for backwards compatibility
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
            }
        } catch (e: Exception) {
            results.add("persistence removal failed: ${e.message}")
        }

        // 3b. Clean up Room schema JSON files (build artifacts)
        try {
            val schemasDir = File(persistenceDir, "../../../../schemas")
            if (schemasDir.isDirectory) {
                schemasDir.deleteRecursively()
            }
        } catch (_: Exception) {
            // Schema cleanup is best-effort
        }

        // 4. Regenerate AppDatabase.kt from remaining entities (with incremented version)
        try {
            val entityFiles = (persistenceDir.listFiles() ?: emptyArray())
                .filter { it.isDirectory }
                .flatMap { subDir ->
                    subDir.listFiles()?.filter { it.isFile && it.name.endsWith("Entity.kt") } ?: emptyList()
                }

            val entityInfos = entityFiles.map { file ->
                val entityClassName = file.nameWithoutExtension
                val name = entityClassName.removeSuffix("Entity")
                val subPackage = "io.codenode.persistence.${name.lowercase()}"
                EntityInfo(
                    entityName = name,
                    tableName = name.lowercase() + "s",
                    daoName = "${name}Dao",
                    subPackage = subPackage
                )
            }

            // Read current database version and increment
            val appDatabaseFile = File(persistenceDir, "AppDatabase.kt")
            val currentVersion = if (appDatabaseFile.exists()) {
                val versionMatch = Regex("""version\s*=\s*(\d+)""").find(appDatabaseFile.readText())
                versionMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
            } else 1
            val newVersion = currentVersion + 1

            val persistencePackage = "io.codenode.persistence"
            if (entityInfos.isNotEmpty()) {
                val appDatabaseContent = repositoryCodeGenerator.generateDatabase(entityInfos, persistencePackage, newVersion)
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
                    appendLine("@Database(entities = [], version = $newVersion)")
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

            // Remove graphEditorRuntime block from root build.gradle.kts
            val rootBuildFile = File(projectDir, "build.gradle.kts")
            if (rootBuildFile.exists()) {
                val runtimeBlock = Regex(
                    """[ \t]*graphEditorRuntime\(project\(":$moduleName"\)\)\s*\{[^}]*\{[^}]*\}[^}]*\}\n?"""
                )
                val original = rootBuildFile.readText()
                val updated = runtimeBlock.replace(original, "")
                if (updated != original) {
                    rootBuildFile.writeText(updated)
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
     * Removes project-side integration artifacts for a module:
     * PreviewProvider file from the module's jvmMain source set, and
     * DAO registration from PersistenceBootstrap.
     *
     * No graphEditor files (Main.kt, ModuleSessionFactory) need modification
     * since discovery is now runtime-based.
     */
    private fun unwireGraphEditorIntegration(
        projectDir: File,
        entityName: String,
        moduleName: String,
        results: MutableList<String>
    ) {
        val packageLower = moduleName.lowercase()
        var unwired = 0

        // 1. Delete PreviewProvider file from module's jvmMain
        val providerFile = File(projectDir, "$moduleName/src/jvmMain/kotlin/io/codenode/$packageLower/userInterface/${moduleName}PreviewProvider.kt")
        if (providerFile.exists() && providerFile.delete()) {
            unwired++
        }
        // Also check legacy location (graphEditor/ui/) for backwards compatibility
        val legacyProviderFile = File(projectDir, "graphEditor/src/jvmMain/kotlin/ui/${moduleName}PreviewProvider.kt")
        if (legacyProviderFile.exists() && legacyProviderFile.delete()) {
            unwired++
        }

        // 2. Remove DAO registration and import from PersistenceBootstrap
        val bootstrapFile = File(projectDir, "persistence/src/commonMain/kotlin/io/codenode/persistence/PersistenceBootstrap.kt")
        if (bootstrapFile.exists()) {
            var content = bootstrapFile.readText()
            val daoLine = "single<${entityName}Dao>"
            val daoImport = "import io.codenode.persistence.${entityName.lowercase()}.${entityName}Dao"
            val lines = content.lines().toMutableList()
            val originalSize = lines.size
            lines.removeAll { it.contains(daoLine) || it.trim() == daoImport }
            if (lines.size != originalSize) {
                bootstrapFile.writeText(lines.joinToString("\n"))
                unwired++
            }
        }

        if (unwired > 0) {
            results.add("Removed $unwired integration artifacts")
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
        viewModelPackage: String,
        controllerPackage: String,
        effectiveModuleName: String,
        filesCreated: MutableList<String>,
        filesOverwritten: MutableList<String>
    ) {
        val viewModelPath = viewModelPackage.replace(".", "/")
        val viewModelFileName = "${effectiveModuleName}ViewModel.kt"
        val viewModelFile = File(moduleDir, "src/commonMain/kotlin/$viewModelPath/$viewModelFileName")
        val relativePath = "src/commonMain/kotlin/$viewModelPath/$viewModelFileName"

        if (!viewModelFile.exists()) {
            // First save: generate full ViewModel stub
            val content = runtimeViewModelGenerator.generate(flowGraph, viewModelPackage, controllerPackage)
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
                val content = runtimeViewModelGenerator.generate(flowGraph, viewModelPackage, controllerPackage)
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
        viewModelPackage: String,
        filesCreated: MutableList<String>
    ) {
        val uiPackagePath = userInterfacePackage.replace(".", "/")
        val stubFileName = userInterfaceStubGenerator.getStubFileName(flowGraph)
        val stubFile = File(moduleDir, "src/commonMain/kotlin/$uiPackagePath/$stubFileName")
        val relativePath = "src/commonMain/kotlin/$uiPackagePath/$stubFileName"

        if (!stubFile.exists()) {
            val content = userInterfaceStubGenerator.generate(flowGraph, userInterfacePackage, viewModelPackage)
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
        flowPackage: String,
        controllerPackage: String,
        viewModelPackage: String,
        effectiveModuleName: String,
        filesCreated: MutableList<String>,
        filesOverwritten: MutableList<String>
    ) {
        val flowPath = flowPackage.replace(".", "/")
        val controllerPath = controllerPackage.replace(".", "/")
        val flowDir = File(moduleDir, "src/commonMain/kotlin/$flowPath")
        val controllerDir = File(moduleDir, "src/commonMain/kotlin/$controllerPath")

        val flowFile = "${effectiveModuleName}Flow.kt" to runtimeFlowGenerator.generate(flowGraph, flowPackage, viewModelPackage)
        writeFileAlways(File(flowDir, flowFile.first), flowFile.second, "src/commonMain/kotlin/$flowPath/${flowFile.first}", filesCreated, filesOverwritten)

        val controllerFiles = listOf(
            "${effectiveModuleName}Controller.kt" to runtimeControllerGenerator.generate(flowGraph, controllerPackage, viewModelPackage),
            "${effectiveModuleName}ControllerInterface.kt" to runtimeControllerInterfaceGenerator.generate(flowGraph, controllerPackage),
            "${effectiveModuleName}ControllerAdapter.kt" to runtimeControllerAdapterGenerator.generate(flowGraph, controllerPackage)
        )

        for ((fileName, content) in controllerFiles) {
            val file = File(controllerDir, fileName)
            val relativePath = "src/commonMain/kotlin/$controllerPath/$fileName"
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
