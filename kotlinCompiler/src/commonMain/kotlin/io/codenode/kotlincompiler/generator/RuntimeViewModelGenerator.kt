/*
 * RuntimeViewModelGenerator
 * Generates {Name}ViewModel.kt stub with Module State object from FlowGraph
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.FlowGraph

/**
 * Generates a {Name}ViewModel.kt stub file in the base package containing:
 * 1. A marker-delineated {ModuleName}State object with MutableStateFlow/StateFlow pairs
 *    derived from source output ports and sink input ports via ObservableStateResolver
 * 2. A {ModuleName}ViewModel class extending ViewModel with state delegation from
 *    {ModuleName}State and control method delegation from {ModuleName}ControllerInterface
 *
 * The Module Properties section (the State object) is delineated by marker comments
 * and can be selectively regenerated while preserving user code outside the markers.
 */
class RuntimeViewModelGenerator {

    private val observableStateResolver = ObservableStateResolver()

    companion object {
        const val MODULE_PROPERTIES_START = "// ===== MODULE PROPERTIES START ====="
        const val MODULE_PROPERTIES_END = "// ===== MODULE PROPERTIES END ====="
    }

    /**
     * Generates the full {Name}ViewModel.kt stub file content from a FlowGraph.
     *
     * @param flowGraph The flow graph to generate from
     * @param basePackage The base package for the ViewModel stub file
     * @param generatedPackage The generated package (for ControllerInterface import)
     * @return Generated Kotlin source code
     */
    fun generate(flowGraph: FlowGraph, basePackage: String, generatedPackage: String): String {
        val flowName = flowGraph.name.pascalCase()
        val observableProps = observableStateResolver.getObservableStateProperties(flowGraph)
        val entityInfo = detectEntityModule(flowGraph)

        return buildString {
            generateHeader(flowName)
            generatePackage(basePackage)
            generateImports(flowName, generatedPackage, observableProps.isNotEmpty(), entityInfo)
            appendLine()

            // Module Properties section (marker-delineated, regenerated on save)
            append(generateModulePropertiesSection(flowName, observableProps, entityInfo))

            // ViewModel class section (user-editable, preserved across regenerations)
            generateViewModelSection(flowName, observableProps, entityInfo)
        }
    }

    /**
     * Generates only the Module Properties section (between markers) for a FlowGraph.
     * Used for selective regeneration: the caller reads the existing file, replaces
     * the content between markers with this regenerated section, and preserves
     * everything outside the markers.
     *
     * @param flowGraph The flow graph to generate from
     * @return The Module Properties section including start/end markers
     */
    fun generateModulePropertiesSection(flowGraph: FlowGraph): String {
        val flowName = flowGraph.name.pascalCase()
        val observableProps = observableStateResolver.getObservableStateProperties(flowGraph)
        val entityInfo = detectEntityModule(flowGraph)
        return generateModulePropertiesSection(flowName, observableProps, entityInfo)
    }

    private fun generateModulePropertiesSection(
        flowName: String,
        observableProps: List<ObservableProperty>,
        entityInfo: EntityModuleInfo? = null
    ): String {
        return buildString {
            appendLine(MODULE_PROPERTIES_START)
            appendLine("// Auto-generated from source output ports and sink input ports. Do not edit this section manually.")
            appendLine("// Changes here will be overwritten on next code generation.")
            appendLine()
            appendLine("object ${flowName}State {")

            if (observableProps.isNotEmpty()) {
                observableProps.forEach { prop ->
                    appendLine()
                    // For entity modules, use concrete types instead of Any
                    val resolvedType = if (entityInfo != null) {
                        resolveEntityPropertyType(prop.name, prop.typeName, entityInfo)
                    } else {
                        prop.typeName
                    }
                    if (prop.defaultValue == "null") {
                        appendLine("    internal val _${prop.name} = MutableStateFlow<${resolvedType}?>(null)")
                        appendLine("    val ${prop.name}Flow: StateFlow<${resolvedType}?> = _${prop.name}.asStateFlow()")
                    } else {
                        appendLine("    internal val _${prop.name} = MutableStateFlow(${prop.defaultValue})")
                        appendLine("    val ${prop.name}Flow: StateFlow<${resolvedType}> = _${prop.name}.asStateFlow()")
                    }
                }

                if (entityInfo != null) {
                    appendLine()
                    appendLine("    internal val _${entityInfo.pluralNameLower} = MutableStateFlow<List<${entityInfo.entityName}>>(emptyList())")
                }

                appendLine()
                appendLine("    fun reset() {")
                observableProps.forEach { prop ->
                    appendLine("        _${prop.name}.value = ${prop.defaultValue}")
                }
                if (entityInfo != null) {
                    appendLine("        _${entityInfo.pluralNameLower}.value = emptyList()")
                }
                appendLine("    }")
            }

            appendLine("}")
            appendLine()
            appendLine(MODULE_PROPERTIES_END)
            appendLine()
        }
    }

    private fun StringBuilder.generateHeader(flowName: String) {
        appendLine("/*")
        appendLine(" * ${flowName}ViewModel")
        appendLine(" * Generated by CodeNodeIO RuntimeViewModelGenerator")
        appendLine(" * License: Apache 2.0")
        appendLine(" */")
        appendLine()
    }

    private fun StringBuilder.generatePackage(basePackage: String) {
        appendLine("package $basePackage")
        appendLine()
    }

    private fun StringBuilder.generateImports(
        flowName: String,
        generatedPackage: String,
        hasObservableState: Boolean,
        entityInfo: EntityModuleInfo? = null
    ) {
        appendLine("import androidx.lifecycle.ViewModel")
        if (entityInfo != null) {
            appendLine("import androidx.lifecycle.viewModelScope")
        }
        appendLine("import io.codenode.fbpdsl.model.ExecutionState")
        appendLine("import io.codenode.fbpdsl.model.FlowGraph")
        if (hasObservableState || entityInfo != null) {
            appendLine("import kotlinx.coroutines.flow.MutableStateFlow")
            appendLine("import kotlinx.coroutines.flow.StateFlow")
            appendLine("import kotlinx.coroutines.flow.asStateFlow")
        } else {
            appendLine("import kotlinx.coroutines.flow.StateFlow")
        }
        if (entityInfo != null) {
            appendLine("import kotlinx.coroutines.launch")
        }
        appendLine("import $generatedPackage.${flowName}ControllerInterface")
        if (entityInfo != null) {
            appendLine("import io.codenode.persistence.${entityInfo.entityName}Dao")
            appendLine("import io.codenode.persistence.${entityInfo.entityName}Entity")
            appendLine("import io.codenode.persistence.${entityInfo.entityName}Repository")
            appendLine("import ${entityInfo.ipTypesPackage}.${entityInfo.entityName}")
            appendLine("import ${entityInfo.basePackage}.to${entityInfo.entityName}")
        }
    }

    private fun StringBuilder.generateViewModelSection(
        flowName: String,
        observableProps: List<ObservableProperty>,
        entityInfo: EntityModuleInfo? = null
    ) {
        appendLine("// ============================================================")
        appendLine("// ViewModel")
        appendLine("// Binding interface between composable UI and FlowGraph.")
        appendLine("// User-editable section below — preserved across regenerations.")
        appendLine("// ============================================================")
        appendLine()
        appendLine("/**")
        appendLine(" * ViewModel for the $flowName composable.")
        appendLine(" * Bridges FlowGraph domain logic with Compose UI.")
        appendLine(" *")
        appendLine(" * @param controller The ${flowName}ControllerInterface that manages FlowGraph execution")
        appendLine(" * @generated by CodeNodeIO RuntimeViewModelGenerator")
        appendLine(" */")
        appendLine("class ${flowName}ViewModel(")
        if (entityInfo != null) {
            appendLine("    private val controller: ${flowName}ControllerInterface,")
            appendLine("    ${entityInfo.entityNameLower}Dao: ${entityInfo.entityName}Dao")
        } else {
            appendLine("    private val controller: ${flowName}ControllerInterface")
        }
        appendLine(") : ViewModel() {")
        appendLine()

        if (observableProps.isNotEmpty()) {
            appendLine("    // Observable state from module properties")
            observableProps.forEach { prop ->
                val resolvedType = if (entityInfo != null) {
                    resolveEntityPropertyType(prop.name, prop.typeName, entityInfo)
                } else {
                    prop.typeName
                }
                val typeStr = if (prop.defaultValue == "null") "${resolvedType}?" else resolvedType
                appendLine("    val ${prop.name}: StateFlow<$typeStr> = ${flowName}State.${prop.name}Flow")
            }
            appendLine()
        }

        if (entityInfo != null) {
            appendLine("    // Reactive entity list from repository")
            appendLine("    val ${entityInfo.pluralNameLower}: StateFlow<List<${entityInfo.entityName}>> = ${flowName}State._${entityInfo.pluralNameLower}")
            appendLine()
        }

        appendLine("    // Execution state from controller")
        appendLine("    val executionState: StateFlow<ExecutionState> = controller.executionState")
        appendLine()

        if (entityInfo != null) {
            appendLine("    init {")
            appendLine("        // Collect repository changes into the ${entityInfo.pluralNameLower} StateFlow")
            appendLine("        viewModelScope.launch {")
            appendLine("            val repo = ${entityInfo.entityName}Repository(${entityInfo.entityNameLower}Dao)")
            appendLine("            repo.observeAll().collect { list ->")
            appendLine("                ${flowName}State._${entityInfo.pluralNameLower}.value = list.map { it.to${entityInfo.entityName}() }")
            appendLine("            }")
            appendLine("        }")
            appendLine("    }")
            appendLine()
            appendLine("    // CRUD methods — trigger operations via the FlowGraph reactive source")
            appendLine("    fun addEntity(${entityInfo.entityNameLower}: ${entityInfo.entityName}) {")
            appendLine("        ${flowName}State._save.value = ${entityInfo.entityNameLower}")
            appendLine("    }")
            appendLine()
            appendLine("    fun updateEntity(${entityInfo.entityNameLower}: ${entityInfo.entityName}) {")
            appendLine("        ${flowName}State._update.value = ${entityInfo.entityNameLower}")
            appendLine("    }")
            appendLine()
            appendLine("    fun removeEntity(${entityInfo.entityNameLower}: ${entityInfo.entityName}) {")
            appendLine("        ${flowName}State._remove.value = ${entityInfo.entityNameLower}")
            appendLine("    }")
            appendLine()
        }

        appendLine("    // Control methods")
        appendLine("    fun start(): FlowGraph = controller.start()")
        appendLine()
        appendLine("    fun stop(): FlowGraph = controller.stop()")
        appendLine()
        appendLine("    fun reset(): FlowGraph = controller.reset()")
        appendLine()
        appendLine("    fun pause(): FlowGraph = controller.pause()")
        appendLine()
        appendLine("    fun resume(): FlowGraph = controller.resume()")
        appendLine("}")
        appendLine()
    }

    /**
     * Detects if a FlowGraph represents an entity CRUD module by looking for nodes
     * with _cudSource, _repository, and _display configurations.
     *
     * @return EntityModuleInfo if this is an entity module, null otherwise
     */
    private fun detectEntityModule(flowGraph: FlowGraph): EntityModuleInfo? {
        val codeNodes = flowGraph.getAllNodes()
            .filterIsInstance<CodeNode>()

        val cudNode = codeNodes.find { it.configuration["_cudSource"] == "true" }
        val repoNode = codeNodes.find { it.configuration["_repository"] == "true" }
        val displayNode = codeNodes.find { it.configuration["_display"] == "true" }

        if (cudNode == null || repoNode == null || displayNode == null) return null

        val entityName = repoNode.configuration["_sourceIPTypeName"] ?: return null
        val entityNameLower = entityName.replaceFirstChar { it.lowercase() }
        val pluralNameLower = pluralize(entityNameLower)

        val pluralName = pluralize(entityName)
        val basePackage = "io.codenode.${pluralName.lowercase()}"

        val ipTypesPackage = repoNode.configuration["_ipTypesPackage"] ?: "$basePackage.iptypes"

        return EntityModuleInfo(
            entityName = entityName,
            entityNameLower = entityNameLower,
            pluralNameLower = pluralNameLower,
            basePackage = basePackage,
            ipTypesPackage = ipTypesPackage
        )
    }

    /**
     * Resolves the concrete type for an entity module state property.
     * CUD ports (save, update, remove) use the IP type. Display ports (result, error) use String.
     * Other ports keep their original type.
     */
    private fun resolveEntityPropertyType(propName: String, originalType: String, entityInfo: EntityModuleInfo): String {
        return when (propName) {
            "save", "update", "remove" -> entityInfo.entityName
            "result", "error" -> "String"
            else -> originalType
        }
    }

    /**
     * Information about an entity module detected from FlowGraph node configurations.
     */
    data class EntityModuleInfo(
        val entityName: String,
        val entityNameLower: String,
        val pluralNameLower: String,
        val basePackage: String,
        val ipTypesPackage: String
    )
}
