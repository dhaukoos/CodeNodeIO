/*
 * StatePropertiesGenerator
 * Generates per-node state property files with MutableStateFlow/StateFlow pairs
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.CodeNode

/**
 * Generator for per-node state property files.
 *
 * Generates a Kotlin object per CodeNode containing MutableStateFlow/StateFlow
 * pairs for each port (both input and output). The generated object lives in
 * a `stateProperties/` sub-package and provides:
 * - `internal` MutableStateFlow properties (`_portName`) for same-module access
 * - Public StateFlow accessors (`portNameFlow`) for external observation
 * - A `reset()` method to restore all properties to their initial defaults
 *
 * Generated files are placed in a stateProperties/ package, separate from
 * generated/ files, because they are intended to be edited by the developer.
 */
class StatePropertiesGenerator {

    /**
     * Determines whether a state properties file should be generated for the given node.
     *
     * @param codeNode The code node to check
     * @return true if node has at least one input or output port
     */
    fun shouldGenerate(codeNode: CodeNode): Boolean {
        return codeNode.inputPorts.isNotEmpty() || codeNode.outputPorts.isNotEmpty()
    }

    /**
     * Gets the filename for a state properties file.
     *
     * @param codeNode The code node
     * @return Filename in format {NodeName}StateProperties.kt
     */
    fun getStatePropertiesFileName(codeNode: CodeNode): String {
        return "${codeNode.name.pascalCase()}StateProperties.kt"
    }

    /**
     * Gets the Kotlin object name for a state properties file.
     *
     * @param codeNode The code node
     * @return Object name in format {NodeName}StateProperties
     */
    fun getStatePropertiesObjectName(codeNode: CodeNode): String {
        return "${codeNode.name.pascalCase()}StateProperties"
    }

    /**
     * Generates a state properties file content for a CodeNode.
     *
     * @param codeNode The code node to generate state properties for
     * @param packageName The package name for the stateProperties sub-package
     * @return Generated Kotlin source code, or empty string if no ports exist
     */
    fun generateStateProperties(codeNode: CodeNode, packageName: String): String {
        if (!shouldGenerate(codeNode)) return ""

        val objectName = getStatePropertiesObjectName(codeNode)
        val allPorts = codeNode.inputPorts + codeNode.outputPorts

        return buildString {
            // Package declaration
            appendLine("package $packageName")
            appendLine()

            // Imports
            appendLine("import kotlinx.coroutines.flow.MutableStateFlow")
            appendLine("import kotlinx.coroutines.flow.StateFlow")
            appendLine("import kotlinx.coroutines.flow.asStateFlow")
            appendLine()

            // KDoc
            appendLine("/**")
            appendLine(" * State properties for the ${codeNode.name} node.")
            appendLine(" *")
            if (codeNode.inputPorts.isNotEmpty()) {
                appendLine(" * Input ports:")
                codeNode.inputPorts.forEach { port ->
                    appendLine(" *   - ${port.name}: ${port.dataType.simpleName ?: "Any"}")
                }
            }
            if (codeNode.outputPorts.isNotEmpty()) {
                appendLine(" * Output ports:")
                codeNode.outputPorts.forEach { port ->
                    appendLine(" *   - ${port.name}: ${port.dataType.simpleName ?: "Any"}")
                }
            }
            appendLine(" */")

            // Object declaration
            appendLine("object $objectName {")

            // MutableStateFlow + StateFlow pairs for each port
            allPorts.forEach { port ->
                val portName = port.name.camelCase()
                val typeName = port.dataType.simpleName ?: "Any"
                val defaultValue = defaultForType(typeName)
                appendLine()
                appendLine("    internal val _$portName = MutableStateFlow($defaultValue)")
                appendLine("    val ${portName}Flow: StateFlow<$typeName> = _$portName.asStateFlow()")
            }

            // reset() method
            appendLine()
            appendLine("    fun reset() {")
            allPorts.forEach { port ->
                val portName = port.name.camelCase()
                val typeName = port.dataType.simpleName ?: "Any"
                val defaultValue = defaultForType(typeName)
                appendLine("        _$portName.value = $defaultValue")
            }
            appendLine("    }")

            appendLine("}")
        }
    }

    private fun defaultForType(typeName: String): String = when (typeName) {
        "Int" -> "0"
        "Long" -> "0L"
        "Double" -> "0.0"
        "Float" -> "0.0f"
        "String" -> "\"\""
        "Boolean" -> "false"
        else -> "TODO(\"Provide initial value for $typeName\")"
    }
}
