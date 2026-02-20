/*
 * Module Generator
 * Generates complete KMP modules from FlowGraph definitions
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*
import kotlin.reflect.KClass

/**
 * Generates complete Kotlin Multiplatform modules from FlowGraph definitions.
 *
 * Creates a fully structured KMP module including:
 * - Directory structure for all target platforms
 * - build.gradle.kts with proper KMP configuration
 * - FlowGraph instantiation class
 * - RootControlNode controller wrapper class
 *
 * Generated modules can be compiled and used as standalone libraries
 * or integrated into larger KMP projects.
 *
 * @sample
 * ```kotlin
 * val generator = ModuleGenerator()
 * val flowGraph = FlowGraph(...)
 *
 * // Generate complete module
 * val module = generator.generateModule(flowGraph, "my-module")
 * module.writeTo(outputDirectory)
 *
 * // Or generate individual parts
 * val buildGradle = generator.generateBuildGradle(flowGraph, "my-module")
 * val flowClass = generator.generateFlowGraphClass(flowGraph, "io.mycompany.flows")
 * ```
 */
class ModuleGenerator {

    companion object {
        const val DEFAULT_PACKAGE = "io.codenode.generated"
        const val KOTLIN_VERSION = "2.1.21"
        const val COMPOSE_VERSION = "1.7.3"
        const val COROUTINES_VERSION = "1.8.0"
        const val SERIALIZATION_VERSION = "1.6.0"
        const val LIFECYCLE_VERSION = "2.8.0"

        /**
         * Maps Connection.channelCapacity to the appropriate Channel constructor argument.
         *
         * @param capacity The channel capacity from Connection.channelCapacity
         * @return String representation of the Channel constructor argument:
         *         - 0 → "Channel.RENDEZVOUS" (sender blocks until receiver ready)
         *         - -1 → "Channel.UNLIMITED" (no backpressure)
         *         - N (positive) → "N" (buffered with backpressure at capacity)
         */
        fun channelCapacityArg(capacity: Int): String {
            return when (capacity) {
                0 -> "Channel.RENDEZVOUS"
                -1 -> "Channel.UNLIMITED"
                else -> "$capacity"
            }
        }
    }

    private val factoryGenerator = FlowGraphFactoryGenerator()

    /**
     * Generates a complete KMP module from a FlowGraph.
     *
     * @param flowGraph The flow graph to generate a module from
     * @param moduleName The name of the module (used for directory and gradle config)
     * @param packageName The base package name (default: io.codenode.generated)
     * @return GeneratedModule containing all generated files and structure
     */
    fun generateModule(
        flowGraph: FlowGraph,
        moduleName: String,
        packageName: String = DEFAULT_PACKAGE
    ): GeneratedModule {
        val structure = generateModuleStructure(flowGraph, moduleName, packageName)
        val files = mutableListOf<GeneratedFile>()

        // Add build.gradle.kts
        files.add(GeneratedFile(
            name = "build.gradle.kts",
            path = "",
            content = generateBuildGradle(flowGraph, moduleName)
        ))

        // Add settings.gradle.kts
        files.add(GeneratedFile(
            name = "settings.gradle.kts",
            path = "",
            content = generateSettingsGradle(moduleName)
        ))

        // Add FlowGraph class
        val flowClassName = "${flowGraph.name.pascalCase()}Flow"
        val flowPackagePath = packageName.replace(".", "/")
        files.add(GeneratedFile(
            name = "$flowClassName.kt",
            path = "src/commonMain/kotlin/$flowPackagePath",
            content = generateFlowGraphClass(flowGraph, packageName)
        ))

        // Add Controller class
        val controllerClassName = "${flowGraph.name.pascalCase()}Controller"
        files.add(GeneratedFile(
            name = "$controllerClassName.kt",
            path = "src/commonMain/kotlin/$flowPackagePath",
            content = generateControllerClass(flowGraph, packageName)
        ))

        return GeneratedModule(
            name = moduleName,
            structure = structure,
            files = files
        )
    }

    /**
     * Generates the module directory structure based on target platforms.
     *
     * @param flowGraph The flow graph to determine platforms from
     * @param moduleName The module name
     * @param packageName The base package name
     * @return ModuleStructure containing all required directories
     */
    fun generateModuleStructure(
        flowGraph: FlowGraph,
        moduleName: String,
        packageName: String = DEFAULT_PACKAGE
    ): ModuleStructure {
        val directories = mutableListOf<String>()
        val packagePath = packageName.replace(".", "/")

        // Always include common source sets
        directories.add("src/commonMain/kotlin")
        directories.add("src/commonMain/kotlin/$packagePath")
        directories.add("src/commonTest/kotlin")
        directories.add("src/commonTest/kotlin/$packagePath")

        // Always include JVM (required for desktop and testing)
        directories.add("src/jvmMain/kotlin")
        directories.add("src/jvmMain/kotlin/$packagePath")
        directories.add("src/jvmTest/kotlin")

        // Add platform-specific directories based on targets
        if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)) {
            directories.add("src/androidMain/kotlin")
            directories.add("src/androidMain/kotlin/$packagePath")
            directories.add("src/androidTest/kotlin")
        }

        if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_IOS)) {
            directories.add("src/iosMain/kotlin")
            directories.add("src/iosMain/kotlin/$packagePath")
            directories.add("src/iosTest/kotlin")
        }

        if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_WEB)) {
            directories.add("src/jsMain/kotlin")
            directories.add("src/jsMain/kotlin/$packagePath")
            directories.add("src/jsTest/kotlin")
        }

        if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_WASM)) {
            directories.add("src/wasmJsMain/kotlin")
            directories.add("src/wasmJsMain/kotlin/$packagePath")
        }

        return ModuleStructure(
            moduleName = moduleName,
            directories = directories
        )
    }

    /**
     * Generates build.gradle.kts content for the KMP module.
     *
     * @param flowGraph The flow graph to configure targets from
     * @param moduleName The module name
     * @return Generated build.gradle.kts content
     */
    fun generateBuildGradle(flowGraph: FlowGraph, moduleName: String): String {
        return buildString {
            // Header
            appendLine("/*")
            appendLine(" * Build script for ${flowGraph.name}")
            appendLine(" * Module: $moduleName")
            appendLine(" * Version: ${flowGraph.version}")
            appendLine(" * Generated by CodeNodeIO ModuleGenerator")
            appendLine(" * License: Apache 2.0")
            appendLine(" */")
            appendLine()

            // Plugins (versions managed by root project's pluginManagement)
            appendLine("plugins {")
            appendLine("    kotlin(\"multiplatform\")")
            appendLine("    kotlin(\"plugin.serialization\")")
            appendLine("    id(\"org.jetbrains.compose\")")
            appendLine("    id(\"org.jetbrains.kotlin.plugin.compose\")")
            if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)) {
                appendLine("    id(\"com.android.library\")")
            }
            appendLine("}")
            appendLine()

            // Kotlin block
            appendLine("kotlin {")

            // JVM target (always included)
            appendLine("    jvm {")
            appendLine("        compilations.all {")
            appendLine("            kotlinOptions.jvmTarget = \"17\"")
            appendLine("        }")
            appendLine("    }")

            // Android target
            if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)) {
                appendLine()
                appendLine("    androidTarget {")
                appendLine("        compilations.all {")
                appendLine("            kotlinOptions.jvmTarget = \"17\"")
                appendLine("        }")
                appendLine("    }")
            }

            // iOS targets
            if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_IOS)) {
                appendLine()
                appendLine("    listOf(")
                appendLine("        iosX64(),")
                appendLine("        iosArm64(),")
                appendLine("        iosSimulatorArm64()")
                appendLine("    ).forEach { iosTarget ->")
                appendLine("        iosTarget.binaries.framework {")
                appendLine("            baseName = \"${moduleName.replace("-", "")}\"")
                appendLine("            isStatic = true")
                appendLine("        }")
                appendLine("    }")
            }

            // JS target
            if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_WEB)) {
                appendLine()
                appendLine("    js(IR) {")
                appendLine("        browser {")
                appendLine("            commonWebpackConfig {")
                appendLine("                cssSupport {")
                appendLine("                    enabled.set(true)")
                appendLine("                }")
                appendLine("            }")
                appendLine("        }")
                appendLine("        binaries.executable()")
                appendLine("    }")
            }

            // Wasm target
            if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_WASM)) {
                appendLine()
                appendLine("    wasmJs {")
                appendLine("        browser()")
                appendLine("        binaries.executable()")
                appendLine("    }")
            }

            // Use default hierarchy template for iOS source sets
            if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_IOS)) {
                appendLine()
                appendLine("    // Use default hierarchy template for shared iOS source sets")
                appendLine("    applyDefaultHierarchyTemplate()")
            }

            // Source sets
            appendLine()
            appendLine("    sourceSets {")
            appendLine("        val commonMain by getting {")
            appendLine("            dependencies {")
            appendLine("                implementation(project(\":fbpDsl\"))")
            appendLine("                implementation(\"org.jetbrains.kotlinx:kotlinx-coroutines-core:$COROUTINES_VERSION\")")
            appendLine("                implementation(\"org.jetbrains.kotlinx:kotlinx-serialization-json:$SERIALIZATION_VERSION\")")
            appendLine("                // KMP-compatible lifecycle support (works on all platforms)")
            appendLine("                implementation(\"org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:$LIFECYCLE_VERSION\")")
            appendLine("            }")
            appendLine("        }")
            appendLine()
            appendLine("        val commonTest by getting {")
            appendLine("            dependencies {")
            appendLine("                implementation(kotlin(\"test\"))")
            appendLine("            }")
            appendLine("        }")

            if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)) {
                appendLine()
                appendLine("        val androidMain by getting {")
                appendLine("            dependencies {")
                appendLine("                implementation(\"org.jetbrains.kotlinx:kotlinx-coroutines-android:$COROUTINES_VERSION\")")
                appendLine("            }")
                appendLine("        }")
            }

            appendLine("    }")
            appendLine("}")

            // Android block if needed
            if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)) {
                appendLine()
                appendLine("android {")
                appendLine("    namespace = \"${DEFAULT_PACKAGE}.${moduleName.replace("-", "")}\"")
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
            appendLine(" * Generated by CodeNodeIO ModuleGenerator")
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

    /**
     * Generates FlowGraph instantiation class.
     *
     * Creates a class that instantiates all nodes from the FlowGraph
     * and wires up connections between them.
     *
     * @param flowGraph The flow graph to generate code for
     * @param packageName The package name for the generated class
     * @return Generated Kotlin class content
     */
    fun generateFlowGraphClass(flowGraph: FlowGraph, packageName: String): String {
        val className = "${flowGraph.name.pascalCase()}Flow"
        val allCodeNodes = flowGraph.getAllCodeNodes()

        return buildString {
            // Header
            appendLine("/*")
            appendLine(" * ${flowGraph.name} Flow")
            appendLine(" * Generated by CodeNodeIO ModuleGenerator")
            appendLine(" * License: Apache 2.0")
            appendLine(" */")
            appendLine()
            appendLine("package $packageName")
            appendLine()

            // Imports
            appendLine("import kotlinx.coroutines.CoroutineScope")
            appendLine("import kotlinx.coroutines.flow.MutableSharedFlow")
            appendLine("import kotlinx.coroutines.flow.SharedFlow")
            appendLine("import kotlinx.coroutines.launch")
            appendLine("import kotlinx.coroutines.channels.Channel")
            appendLine("import kotlinx.coroutines.channels.SendChannel")
            appendLine("import kotlinx.coroutines.channels.ReceiveChannel")
            appendLine()

            // Class documentation
            appendLine("/**")
            appendLine(" * Flow orchestrator for: ${flowGraph.name}")
            appendLine(" *")
            appendLine(" * Version: ${flowGraph.version}")
            flowGraph.description?.let { appendLine(" * Description: $it") }
            appendLine(" *")
            appendLine(" * Nodes: ${allCodeNodes.size}")
            appendLine(" * Connections: ${flowGraph.connections.size}")
            appendLine(" *")
            appendLine(" * @generated by CodeNodeIO ModuleGenerator")
            appendLine(" */")
            appendLine("class $className {")
            appendLine()

            // Node component properties (internal for controller access)
            if (allCodeNodes.isNotEmpty()) {
                appendLine("    // Node components (internal for controller access)")
                allCodeNodes.forEach { node ->
                    val propName = node.name.camelCase()
                    val typeName = node.name.pascalCase()
                    appendLine("    internal val $propName = ${typeName}Component()")
                }
                appendLine()
            }

            // Connection channels
            if (flowGraph.connections.isNotEmpty()) {
                appendLine("    // Connection channels")
                flowGraph.connections.forEach { connection ->
                    val channelName = "channel_${connection.id.replace("-", "_")}"
                    val capacityArg = channelCapacityArg(connection.channelCapacity)
                    appendLine("    private val $channelName = Channel<Any>($capacityArg)")
                }
                appendLine()
            }

            // Start method
            appendLine("    /**")
            appendLine("     * Starts the flow with the given coroutine scope.")
            appendLine("     *")
            appendLine("     * @param scope The coroutine scope to run in")
            appendLine("     */")
            appendLine("    suspend fun start(scope: CoroutineScope) {")
            appendLine("        wireConnections()")
            allCodeNodes.forEach { node ->
                val propName = node.name.camelCase()
                appendLine("        $propName.start(scope)")
            }
            appendLine("    }")
            appendLine()

            // Stop method
            appendLine("    /**")
            appendLine("     * Stops all components in the flow.")
            appendLine("     * Closes channels first for graceful shutdown, then stops components.")
            appendLine("     */")
            appendLine("    fun stop() {")
            // Close channels first (graceful shutdown - allows buffered data to drain)
            if (flowGraph.connections.isNotEmpty()) {
                appendLine("        // Close channels first (graceful shutdown)")
                flowGraph.connections.forEach { connection ->
                    val channelName = "channel_${connection.id.replace("-", "_")}"
                    appendLine("        $channelName.close()")
                }
                appendLine()
            }
            // Then stop components
            appendLine("        // Stop components")
            allCodeNodes.forEach { node ->
                val propName = node.name.camelCase()
                appendLine("        $propName.stop()")
            }
            appendLine("    }")
            appendLine()

            // Wire connections method
            appendLine("    /**")
            appendLine("     * Wires up connections between components.")
            appendLine("     * Assigns channels to component SendChannel/ReceiveChannel properties.")
            appendLine("     */")
            appendLine("    private fun wireConnections() {")
            if (flowGraph.connections.isEmpty()) {
                appendLine("        // No connections to wire")
            } else {
                flowGraph.connections.forEach { connection ->
                    val sourceNode = flowGraph.findNode(connection.sourceNodeId)
                    val targetNode = flowGraph.findNode(connection.targetNodeId)
                    if (sourceNode != null && targetNode != null) {
                        val sourceProp = (sourceNode as? CodeNode)?.name?.camelCase() ?: "source"
                        val targetProp = (targetNode as? CodeNode)?.name?.camelCase() ?: "target"
                        val channelName = "channel_${connection.id.replace("-", "_")}"
                        appendLine("        // ${sourceNode.name} -> ${targetNode.name}")
                        appendLine("        $sourceProp.outputChannel = $channelName")
                        appendLine("        $targetProp.inputChannel = $channelName")
                    }
                }
            }
            appendLine("    }")

            appendLine("}")
            appendLine()

            // Generate stub component classes for nodes that don't have a _useCaseClass configured
            // (nodes with _useCaseClass use external ProcessingLogic implementations)
            val nodesNeedingStubs = allCodeNodes.filter { node ->
                !node.configuration.containsKey("_useCaseClass")
            }
            nodesNeedingStubs.forEach { node ->
                val typeName = node.name.pascalCase()
                appendLine("/**")
                appendLine(" * Component for node: ${node.name}")
                appendLine(" * Type: ${node.codeNodeType.typeName}")
                appendLine(" */")
                appendLine("class ${typeName}Component {")
                appendLine("    val input = MutableSharedFlow<Any>(replay = 1)")
                appendLine("    val output = MutableSharedFlow<Any>(replay = 1)")
                appendLine()
                appendLine("    suspend fun start(scope: CoroutineScope) {")
                appendLine("        // TODO: Implement processing logic")
                appendLine("    }")
                appendLine()
                appendLine("    fun stop() {")
                appendLine("        // TODO: Implement cleanup")
                appendLine("    }")
                appendLine("}")
                appendLine()
            }
        }
    }

    /**
     * T044: Generates {GraphName}Factory.kt with createXXXFlowGraph() function.
     *
     * The factory function instantiates all ProcessingLogic components
     * and creates a fully configured FlowGraph ready for execution.
     *
     * @param flowGraph The flow graph to generate a factory for
     * @param packageName The package name for the generated class
     * @return Generated Kotlin factory class content
     */
    fun generateFactoryClass(flowGraph: FlowGraph, packageName: String): String {
        return factoryGenerator.generateFactory(flowGraph, packageName)
    }

    /**
     * Gets the factory file name for a FlowGraph.
     *
     * @param flowGraph The flow graph
     * @return File name in format {GraphName}Factory.kt
     */
    fun getFactoryFileName(flowGraph: FlowGraph): String {
        return factoryGenerator.getFactoryFileName(flowGraph)
    }

    /**
     * Gets the list of required ProcessingLogic components for a FlowGraph.
     *
     * @param flowGraph The flow graph to analyze
     * @return List of component class names that must be implemented
     */
    fun getRequiredComponents(flowGraph: FlowGraph): List<String> {
        return factoryGenerator.getRequiredComponents(flowGraph)
    }

    /**
     * Validates that all required ProcessingLogic components exist.
     *
     * @param flowGraph The flow graph to validate
     * @param existingFiles Set of file names that exist in the source directory
     * @return ComponentValidationResult indicating validity and missing components
     */
    fun validateComponents(flowGraph: FlowGraph, existingFiles: Set<String>): ComponentValidationResult {
        return factoryGenerator.validateComponents(flowGraph, existingFiles)
    }

    /**
     * Generates RootControlNode controller wrapper class.
     *
     * Creates a class that wraps RootControlNode to provide
     * execution control for the generated flow.
     *
     * @param flowGraph The flow graph to generate controller for
     * @param packageName The package name for the generated class
     * @return Generated Kotlin class content
     */
    fun generateControllerClass(flowGraph: FlowGraph, packageName: String): String {
        val className = "${flowGraph.name.pascalCase()}Controller"
        val flowClassName = "${flowGraph.name.pascalCase()}Flow"

        return buildString {
            // Header
            appendLine("/*")
            appendLine(" * ${flowGraph.name} Controller")
            appendLine(" * Generated by CodeNodeIO ModuleGenerator")
            appendLine(" * License: Apache 2.0")
            appendLine(" */")
            appendLine()
            appendLine("package $packageName")
            appendLine()

            // Imports
            appendLine("import io.codenode.fbpdsl.model.RootControlNode")
            appendLine("import io.codenode.fbpdsl.model.FlowGraph")
            appendLine("import io.codenode.fbpdsl.model.FlowExecutionStatus")
            appendLine("import io.codenode.fbpdsl.model.ExecutionState")
            appendLine("import io.codenode.fbpdsl.model.ControlConfig")
            appendLine("import androidx.lifecycle.Lifecycle")
            appendLine("import androidx.lifecycle.LifecycleEventObserver")
            appendLine("import androidx.lifecycle.LifecycleOwner")
            appendLine("import kotlinx.coroutines.CoroutineScope")
            appendLine("import kotlinx.coroutines.Dispatchers")
            appendLine("import kotlinx.coroutines.SupervisorJob")
            appendLine("import kotlinx.coroutines.cancel")
            appendLine("import kotlinx.coroutines.flow.MutableStateFlow")
            appendLine("import kotlinx.coroutines.flow.StateFlow")
            appendLine("import kotlinx.coroutines.flow.asStateFlow")
            appendLine("import kotlinx.coroutines.launch")
            appendLine()

            // Class documentation
            appendLine("/**")
            appendLine(" * Controller for ${flowGraph.name} flow execution.")
            appendLine(" *")
            appendLine(" * Provides unified control operations for starting, pausing,")
            appendLine(" * stopping, and monitoring the flow execution.")
            appendLine(" *")
            appendLine(" * This class wraps RootControlNode to provide a clean API")
            appendLine(" * specific to the ${flowGraph.name} flow.")
            appendLine(" *")
            appendLine(" * @param flowGraph The FlowGraph instance to control")
            appendLine(" * @generated by CodeNodeIO ModuleGenerator")
            appendLine(" */")
            appendLine("class $className(")
            appendLine("    private var flowGraph: FlowGraph")
            appendLine(") {")
            appendLine()

            // Internal controller
            appendLine("    private var controller: RootControlNode = RootControlNode.createFor(")
            appendLine("        flowGraph = flowGraph,")
            appendLine("        name = \"${flowGraph.name}Controller\"")
            appendLine("    )")
            appendLine()

            // Flow instance
            appendLine("    private val flow = $flowClassName()")
            appendLine()

            // Flow scope
            appendLine("    /**")
            appendLine("     * CoroutineScope for running the flow. Created on start, cancelled on stop.")
            appendLine("     */")
            appendLine("    private var flowScope: CoroutineScope? = null")
            appendLine()

            // Lifecycle tracking
            appendLine("    /**")
            appendLine("     * Tracks whether the flow was running before a lifecycle-triggered pause.")
            appendLine("     * Used to restore running state when lifecycle resumes.")
            appendLine("     */")
            appendLine("    private var wasRunningBeforePause: Boolean = false")
            appendLine()

            // StateFlow properties derived from sink node input ports
            val sinkProperties = collectSinkPortProperties(flowGraph)
            sinkProperties.forEach { prop ->
                appendLine("    val ${prop.propertyName}: StateFlow<${prop.kotlinType}> = flow.${prop.sinkNodeCamelCase}.${prop.propertyName}Flow")
            }
            appendLine()
            appendLine("    private val _executionState = MutableStateFlow(ExecutionState.IDLE)")
            appendLine("    /**")
            appendLine("     * Current execution state as observable StateFlow.")
            appendLine("     * Updates when start(), stop(), pause(), or reset() is called.")
            appendLine("     */")
            appendLine("    val executionState: StateFlow<ExecutionState> = _executionState.asStateFlow()")
            appendLine()

            // Start method
            appendLine("    /**")
            appendLine("     * Starts all nodes in the flow.")
            appendLine("     *")
            appendLine("     * Transitions all nodes to RUNNING state and starts the actual flow execution.")
            appendLine("     * State propagates to descendants respecting independentControl flags.")
            appendLine("     *")
            appendLine("     * @return Updated FlowGraph with all nodes running")
            appendLine("     */")
            appendLine("    fun start(): FlowGraph {")
            appendLine("        // Update FlowGraph state model")
            appendLine("        flowGraph = controller.startAll()")
            appendLine("        controller = RootControlNode.createFor(flowGraph, \"${flowGraph.name}Controller\")")
            appendLine("        _executionState.value = ExecutionState.RUNNING")
            appendLine()
            appendLine("        // Cancel any existing scope and create a new one")
            appendLine("        flowScope?.cancel()")
            appendLine("        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)")
            appendLine("        flowScope = scope")
            appendLine()
            appendLine("        // Set the timer emitter to RUNNING and start the flow")
            appendLine("        flow.timerEmitter.executionState = ExecutionState.RUNNING")
            appendLine("        scope.launch {")
            appendLine("            flow.start(scope)")
            appendLine("        }")
            appendLine()
            appendLine("        return flowGraph")
            appendLine("    }")
            appendLine()

            // Pause method
            appendLine("    /**")
            appendLine("     * Pauses all nodes in the flow.")
            appendLine("     *")
            appendLine("     * Transitions all nodes to PAUSED state.")
            appendLine("     * State propagates to descendants respecting independentControl flags.")
            appendLine("     *")
            appendLine("     * @return Updated FlowGraph with all nodes paused")
            appendLine("     */")
            appendLine("    fun pause(): FlowGraph {")
            appendLine("        flowGraph = controller.pauseAll()")
            appendLine("        controller = RootControlNode.createFor(flowGraph, \"${flowGraph.name}Controller\")")
            appendLine("        _executionState.value = ExecutionState.PAUSED")
            appendLine("        return flowGraph")
            appendLine("    }")
            appendLine()

            // Stop method
            appendLine("    /**")
            appendLine("     * Stops all nodes in the flow.")
            appendLine("     *")
            appendLine("     * Transitions all nodes to IDLE state and stops the actual flow execution.")
            appendLine("     * State propagates to descendants respecting independentControl flags.")
            appendLine("     *")
            appendLine("     * @return Updated FlowGraph with all nodes stopped")
            appendLine("     */")
            appendLine("    fun stop(): FlowGraph {")
            appendLine("        // Stop the actual flow execution")
            appendLine("        flow.timerEmitter.executionState = ExecutionState.IDLE")
            appendLine("        flow.stop()")
            appendLine("        flowScope?.cancel()")
            appendLine("        flowScope = null")
            appendLine()
            appendLine("        // Update FlowGraph state model")
            appendLine("        flowGraph = controller.stopAll()")
            appendLine("        controller = RootControlNode.createFor(flowGraph, \"${flowGraph.name}Controller\")")
            appendLine("        _executionState.value = ExecutionState.IDLE")
            appendLine("        return flowGraph")
            appendLine("    }")
            appendLine()

            // Reset method
            appendLine("    /**")
            appendLine("     * Resets the flow to initial state.")
            appendLine("     *")
            appendLine("     * Stops all nodes and clears any accumulated state.")
            appendLine("     * Equivalent to stop() but semantically represents a fresh start.")
            appendLine("     *")
            appendLine("     * @return Updated FlowGraph with all nodes reset to IDLE")
            appendLine("     */")
            appendLine("    fun reset(): FlowGraph {")
            appendLine("        wasRunningBeforePause = false")
            appendLine("        // Reset the timer component state")
            appendLine("        flow.timerEmitter.reset()")
            appendLine("        return stop()")
            appendLine("    }")
            appendLine()

            // GetStatus method
            appendLine("    /**")
            appendLine("     * Gets the current execution status of all nodes.")
            appendLine("     *")
            appendLine("     * @return FlowExecutionStatus with node counts and overall state")
            appendLine("     */")
            appendLine("    fun getStatus(): FlowExecutionStatus {")
            appendLine("        return controller.getStatus()")
            appendLine("    }")
            appendLine()

            // SetNodeState method
            appendLine("    /**")
            appendLine("     * Sets execution state for a specific node.")
            appendLine("     *")
            appendLine("     * @param nodeId The ID of the node to update")
            appendLine("     * @param state The new execution state")
            appendLine("     * @return Updated FlowGraph")
            appendLine("     */")
            appendLine("    fun setNodeState(nodeId: String, state: ExecutionState): FlowGraph {")
            appendLine("        flowGraph = controller.setNodeState(nodeId, state)")
            appendLine("        controller = RootControlNode.createFor(flowGraph, \"${flowGraph.name}Controller\")")
            appendLine("        return flowGraph")
            appendLine("    }")
            appendLine()

            // SetNodeConfig method
            appendLine("    /**")
            appendLine("     * Sets control configuration for a specific node.")
            appendLine("     *")
            appendLine("     * @param nodeId The ID of the node to update")
            appendLine("     * @param config The new control configuration")
            appendLine("     * @return Updated FlowGraph")
            appendLine("     */")
            appendLine("    fun setNodeConfig(nodeId: String, config: ControlConfig): FlowGraph {")
            appendLine("        flowGraph = controller.setNodeConfig(nodeId, config)")
            appendLine("        controller = RootControlNode.createFor(flowGraph, \"${flowGraph.name}Controller\")")
            appendLine("        return flowGraph")
            appendLine("    }")
            appendLine()

            // BindToLifecycle method (KMP-compatible)
            appendLine("    /**")
            appendLine("     * Binds the flow execution to a Lifecycle (works on all KMP platforms).")
            appendLine("     *")
            appendLine("     * When the lifecycle enters ON_START, resumes if previously running.")
            appendLine("     * When the lifecycle enters ON_STOP, pauses and tracks state.")
            appendLine("     * When the lifecycle is ON_DESTROY, stops the flow completely.")
            appendLine("     *")
            appendLine("     * @param lifecycle The lifecycle to bind to")
            appendLine("     */")
            appendLine("    fun bindToLifecycle(lifecycle: Lifecycle) {")
            appendLine("        lifecycle.addObserver(object : LifecycleEventObserver {")
            appendLine("            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {")
            appendLine("                when (event) {")
            appendLine("                    Lifecycle.Event.ON_START -> {")
            appendLine("                        if (wasRunningBeforePause) {")
            appendLine("                            start()")
            appendLine("                            wasRunningBeforePause = false")
            appendLine("                        }")
            appendLine("                    }")
            appendLine("                    Lifecycle.Event.ON_STOP -> {")
            appendLine("                        val status = getStatus()")
            appendLine("                        wasRunningBeforePause = status.overallState == ExecutionState.RUNNING")
            appendLine("                        if (wasRunningBeforePause) {")
            appendLine("                            pause()")
            appendLine("                        }")
            appendLine("                    }")
            appendLine("                    Lifecycle.Event.ON_DESTROY -> {")
            appendLine("                        stop()")
            appendLine("                    }")
            appendLine("                    else -> { /* no-op */ }")
            appendLine("                }")
            appendLine("            }")
            appendLine("        })")
            appendLine("    }")
            appendLine()

            // CurrentFlowGraph property
            appendLine("    /**")
            appendLine("     * Gets the current FlowGraph state.")
            appendLine("     */")
            appendLine("    val currentFlowGraph: FlowGraph")
            appendLine("        get() = flowGraph")

            appendLine("}")
        }
    }

    /**
     * Collects observable state entries from sink node input ports.
     *
     * Algorithm:
     * 1. Filter to SINK CodeNodes
     * 2. Collect input ports from each sink
     * 3. If multiple sinks contribute, prefix property names with sinkNodeCamelCase
     * 4. Return collected SinkPortProperty entries
     *
     * @param flowGraph The flow graph to analyze
     * @return List of SinkPortProperty entries
     */
    fun collectSinkPortProperties(flowGraph: FlowGraph): List<SinkPortProperty> {
        val sinkNodes = flowGraph.getAllCodeNodes().filter { it.codeNodeType == CodeNodeType.SINK }
        val multiSink = sinkNodes.size > 1

        return sinkNodes.flatMap { node ->
            val nodeCamelCase = node.name.camelCase()
            node.inputPorts.map { port ->
                val propertyName = if (multiSink) {
                    "$nodeCamelCase${port.name.pascalCase()}"
                } else {
                    port.name
                }
                SinkPortProperty(
                    propertyName = propertyName,
                    kotlinType = port.dataType.simpleName ?: "Any",
                    sinkNodeName = node.name,
                    sinkNodeCamelCase = nodeCamelCase,
                    portId = port.id
                )
            }
        }
    }

    /**
     * Generates a ControllerInterface from a FlowGraph.
     *
     * The interface declares observable StateFlow properties derived from
     * sink node input ports, plus executionState and 5 lifecycle methods.
     *
     * @param flowGraph The flow graph to generate interface from
     * @param packageName The package name for the generated interface
     * @return Generated Kotlin interface source code
     */
    fun generateControllerInterfaceClass(flowGraph: FlowGraph, packageName: String): String {
        val interfaceName = "${flowGraph.name.pascalCase()}ControllerInterface"
        val properties = collectSinkPortProperties(flowGraph)

        return buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import io.codenode.fbpdsl.model.ExecutionState")
            appendLine("import io.codenode.fbpdsl.model.FlowGraph")
            appendLine("import kotlinx.coroutines.flow.StateFlow")
            appendLine()
            appendLine("interface $interfaceName {")
            properties.forEach { prop ->
                appendLine("    val ${prop.propertyName}: StateFlow<${prop.kotlinType}>")
            }
            appendLine("    val executionState: StateFlow<ExecutionState>")
            appendLine("    fun start(): FlowGraph")
            appendLine("    fun stop(): FlowGraph")
            appendLine("    fun reset(): FlowGraph")
            appendLine("    fun pause(): FlowGraph")
            appendLine("    fun resume(): FlowGraph")
            appendLine("}")
        }
    }
}

/**
 * Represents an observable state property derived from a sink node input port.
 *
 * @property propertyName The generated property name (port name, or prefixed with sinkNodeCamelCase for multi-sink)
 * @property kotlinType The Kotlin type name (e.g., "Int", "String")
 * @property sinkNodeName The original sink node name (e.g., "DisplayReceiver")
 * @property sinkNodeCamelCase The camelCase form of the sink node name (e.g., "displayReceiver")
 * @property portId The port ID from the FlowGraph
 */
data class SinkPortProperty(
    val propertyName: String,
    val kotlinType: String,
    val sinkNodeName: String,
    val sinkNodeCamelCase: String,
    val portId: String
)

/**
 * Represents the directory structure of a generated module.
 *
 * @property moduleName The name of the module
 * @property directories List of directory paths to create
 */
data class ModuleStructure(
    val moduleName: String,
    val directories: List<String>
)

/**
 * Represents a generated file.
 *
 * @property name The file name (e.g., "build.gradle.kts")
 * @property path The relative path within the module (e.g., "src/commonMain/kotlin")
 * @property content The file content
 */
data class GeneratedFile(
    val name: String,
    val path: String,
    val content: String
) {
    /**
     * Gets the full relative path including the file name.
     */
    val fullPath: String
        get() = if (path.isEmpty()) name else "$path/$name"
}

/**
 * Represents a complete generated module.
 *
 * @property name The module name
 * @property structure The directory structure
 * @property files All generated files
 */
data class GeneratedModule(
    val name: String,
    val structure: ModuleStructure,
    val files: List<GeneratedFile>
) {
    /**
     * Writes all files to the specified output directory.
     *
     * @param outputDir The directory to write files to
     */
    fun writeTo(outputDir: java.io.File) {
        // Create directories
        structure.directories.forEach { dir ->
            java.io.File(outputDir, dir).mkdirs()
        }

        // Write files
        files.forEach { file ->
            val targetFile = java.io.File(outputDir, file.fullPath)
            targetFile.parentFile?.mkdirs()
            targetFile.writeText(file.content)
        }
    }

    /**
     * Gets the total number of generated files.
     */
    fun fileCount(): Int = files.size

    /**
     * Gets all generated file names.
     */
    fun fileNames(): List<String> = files.map { it.name }
}
