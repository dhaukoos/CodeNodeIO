/*
 * ProcessingLogicStubGenerator
 * Generates tick function stub files for CodeNodes
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.CodeNode

/**
 * Generator for tick function stub files.
 *
 * Generates a Kotlin source file with a top-level val property containing
 * a typed tick function stub for each CodeNode. The generated stubs include:
 * - Package declaration in the processingLogic package
 * - Import for the correct tick type alias
 * - KDoc with node type and port descriptions
 * - Val with typed tick lambda and TODO placeholder
 *
 * Generated stub files are placed in a processingLogic/ package, separate from
 * generated/ files, because they are intended to be edited by the developer.
 */
class ProcessingLogicStubGenerator {

    /**
     * Determines whether a stub should be generated for the given node.
     *
     * @param codeNode The code node to check
     * @return false if node has 0 inputs and 0 outputs, or if input/output count exceeds 3
     */
    fun shouldGenerateStub(codeNode: CodeNode): Boolean {
        val inputCount = codeNode.inputPorts.size
        val outputCount = codeNode.outputPorts.size
        if (inputCount == 0 && outputCount == 0) return false
        if (inputCount > 3 || outputCount > 3) return false
        return true
    }

    /**
     * Gets the filename for a tick function stub.
     *
     * @param codeNode The code node
     * @return Filename in format {NodeName}ProcessLogic.kt
     */
    fun getStubFileName(codeNode: CodeNode): String {
        return "${codeNode.name.pascalCase()}ProcessLogic.kt"
    }

    /**
     * Gets the tick val property name for a node.
     *
     * @param codeNode The code node
     * @return Val name in format {nodeName}Tick (camelCase)
     */
    fun getTickValName(codeNode: CodeNode): String {
        return "${codeNode.name.camelCase()}Tick"
    }

    /**
     * Gets the fully parameterized tick type alias for a node.
     *
     * Maps (inputCount, outputCount) to the correct tick type alias with
     * type parameters resolved from port data types.
     *
     * @param codeNode The code node
     * @return Tick type alias string (e.g., "Out2TickBlock<Int, Int>")
     */
    fun getTickTypeAlias(codeNode: CodeNode): String {
        val inputCount = codeNode.inputPorts.size
        val outputCount = codeNode.outputPorts.size

        return when {
            // Generators (0 inputs)
            inputCount == 0 && outputCount == 1 ->
                "GeneratorTickBlock<${outType(codeNode, 0)}>"
            inputCount == 0 && outputCount == 2 ->
                "Out2TickBlock<${outType(codeNode, 0)}, ${outType(codeNode, 1)}>"
            inputCount == 0 && outputCount == 3 ->
                "Out3TickBlock<${outType(codeNode, 0)}, ${outType(codeNode, 1)}, ${outType(codeNode, 2)}>"

            // Filter vs Transformer (1 input, 1 output)
            inputCount == 1 && outputCount == 1 -> {
                val inT = inType(codeNode, 0)
                val outT = outType(codeNode, 0)
                if (inT == outT) "FilterTickBlock<$inT>"
                else "TransformerTickBlock<$inT, $outT>"
            }

            // Multi-input, single-output processors
            inputCount == 2 && outputCount == 1 ->
                "In2Out1TickBlock<${inType(codeNode, 0)}, ${inType(codeNode, 1)}, ${outType(codeNode, 0)}>"
            inputCount == 3 && outputCount == 1 ->
                "In3Out1TickBlock<${inType(codeNode, 0)}, ${inType(codeNode, 1)}, ${inType(codeNode, 2)}, ${outType(codeNode, 0)}>"

            // Single-input, multi-output processors
            inputCount == 1 && outputCount == 2 ->
                "In1Out2TickBlock<${inType(codeNode, 0)}, ${outType(codeNode, 0)}, ${outType(codeNode, 1)}>"
            inputCount == 1 && outputCount == 3 ->
                "In1Out3TickBlock<${inType(codeNode, 0)}, ${outType(codeNode, 0)}, ${outType(codeNode, 1)}, ${outType(codeNode, 2)}>"

            // Multi-input, multi-output processors
            inputCount == 2 && outputCount == 2 ->
                "In2Out2TickBlock<${inType(codeNode, 0)}, ${inType(codeNode, 1)}, ${outType(codeNode, 0)}, ${outType(codeNode, 1)}>"
            inputCount == 2 && outputCount == 3 ->
                "In2Out3TickBlock<${inType(codeNode, 0)}, ${inType(codeNode, 1)}, ${outType(codeNode, 0)}, ${outType(codeNode, 1)}, ${outType(codeNode, 2)}>"
            inputCount == 3 && outputCount == 2 ->
                "In3Out2TickBlock<${inType(codeNode, 0)}, ${inType(codeNode, 1)}, ${inType(codeNode, 2)}, ${outType(codeNode, 0)}, ${outType(codeNode, 1)}>"
            inputCount == 3 && outputCount == 3 ->
                "In3Out3TickBlock<${inType(codeNode, 0)}, ${inType(codeNode, 1)}, ${inType(codeNode, 2)}, ${outType(codeNode, 0)}, ${outType(codeNode, 1)}, ${outType(codeNode, 2)}>"

            // Sinks (0 outputs)
            inputCount == 1 && outputCount == 0 ->
                "SinkTickBlock<${inType(codeNode, 0)}>"
            inputCount == 2 && outputCount == 0 ->
                "In2SinkTickBlock<${inType(codeNode, 0)}, ${inType(codeNode, 1)}>"
            inputCount == 3 && outputCount == 0 ->
                "In3SinkTickBlock<${inType(codeNode, 0)}, ${inType(codeNode, 1)}, ${inType(codeNode, 2)}>"

            else -> ""
        }
    }

    /**
     * Generates a tick function stub file content for a CodeNode.
     *
     * @param codeNode The code node to generate a stub for
     * @param packageName The base package name (logicmethods sub-package will be appended)
     * @param statePropertiesPackage The stateProperties package for import (null to omit)
     * @return Generated Kotlin source code, or empty string if stub should not be generated
     */
    fun generateStub(codeNode: CodeNode, packageName: String, statePropertiesPackage: String? = null): String {
        if (!shouldGenerateStub(codeNode)) return ""

        val tickTypeAlias = getTickTypeAlias(codeNode)
        val tickValName = getTickValName(codeNode)
        val typeAliasName = tickTypeAlias.substringBefore("<")
        val inputCount = codeNode.inputPorts.size
        val outputCount = codeNode.outputPorts.size
        val hasPorts = inputCount > 0 || outputCount > 0

        return buildString {
            // Package declaration
            appendLine("package $packageName")
            appendLine()

            // Imports
            appendLine("import io.codenode.fbpdsl.runtime.$typeAliasName")
            if (outputCount == 2) {
                appendLine("import io.codenode.fbpdsl.runtime.ProcessResult2")
            } else if (outputCount == 3) {
                appendLine("import io.codenode.fbpdsl.runtime.ProcessResult3")
            }
            if (statePropertiesPackage != null && hasPorts) {
                val objectName = "${codeNode.name.pascalCase()}StateProperties"
                appendLine("import $statePropertiesPackage.$objectName")
            }
            appendLine()

            // KDoc
            appendLine("/**")
            appendLine(" * Tick function for the ${codeNode.name} node.")
            appendLine(" *")
            appendLine(" * Node type: ${getNodeCategory(codeNode)} ($inputCount inputs, $outputCount outputs)")
            appendLine(" *")
            if (inputCount > 0) {
                appendLine(" * Inputs:")
                codeNode.inputPorts.forEach { port ->
                    appendLine(" *   - ${port.name}: ${port.dataType.simpleName ?: "Any"}")
                }
                appendLine(" *")
            }
            if (outputCount > 0) {
                appendLine(" * Outputs:")
                codeNode.outputPorts.forEach { port ->
                    appendLine(" *   - ${port.name}: ${port.dataType.simpleName ?: "Any"}")
                }
                appendLine(" *")
            }
            appendLine(" */")

            // Val declaration
            append("val $tickValName: $tickTypeAlias = {")

            // Lambda parameters for inputs
            if (inputCount > 0) {
                val params = codeNode.inputPorts.joinToString(", ") { it.name }
                append(" $params ->")
            }
            appendLine()

            // TODO comment
            appendLine("    // TODO: Implement ${codeNode.name} tick logic")

            // Default return value
            val defaultReturn = getDefaultReturnValue(codeNode)
            if (defaultReturn.isNotEmpty()) {
                appendLine("    $defaultReturn")
            }

            appendLine("}")
        }
    }

    /**
     * Gets the default return value for the tick function body.
     *
     * @param codeNode The code node
     * @return Default return expression, or empty string for sinks
     */
    private fun getDefaultReturnValue(codeNode: CodeNode): String {
        val inputCount = codeNode.inputPorts.size
        val outputCount = codeNode.outputPorts.size

        // Sinks have no return value
        if (outputCount == 0) return ""

        // Filter: return true
        if (inputCount == 1 && outputCount == 1 && inType(codeNode, 0) == outType(codeNode, 0)) {
            return "true"
        }

        // Single output: type-specific default
        if (outputCount == 1) {
            return defaultForType(outType(codeNode, 0))
        }

        // 2 outputs: ProcessResult2.both(d1, d2)
        if (outputCount == 2) {
            val d1 = defaultForType(outType(codeNode, 0))
            val d2 = defaultForType(outType(codeNode, 1))
            return "ProcessResult2.both($d1, $d2)"
        }

        // 3 outputs: ProcessResult3(d1, d2, d3)
        if (outputCount == 3) {
            val d1 = defaultForType(outType(codeNode, 0))
            val d2 = defaultForType(outType(codeNode, 1))
            val d3 = defaultForType(outType(codeNode, 2))
            return "ProcessResult3($d1, $d2, $d3)"
        }

        return "TODO(\"Provide default value\")"
    }

    /**
     * Gets the node category name for documentation.
     */
    private fun getNodeCategory(codeNode: CodeNode): String {
        val inputCount = codeNode.inputPorts.size
        val outputCount = codeNode.outputPorts.size
        return when {
            inputCount == 0 && outputCount > 0 -> "Generator"
            inputCount > 0 && outputCount == 0 -> "Sink"
            inputCount == 1 && outputCount == 1 -> {
                if (inType(codeNode, 0) == outType(codeNode, 0)) "Filter" else "Transformer"
            }
            else -> "Processor"
        }
    }

    private fun inType(codeNode: CodeNode, index: Int): String =
        codeNode.inputPorts.getOrNull(index)?.dataType?.simpleName ?: "Any"

    private fun outType(codeNode: CodeNode, index: Int): String =
        codeNode.outputPorts.getOrNull(index)?.dataType?.simpleName ?: "Any"

    private fun defaultForType(typeName: String): String = when (typeName) {
        "Int" -> "0"
        "Long" -> "0L"
        "Double" -> "0.0"
        "Float" -> "0.0f"
        "String" -> "\"\""
        "Boolean" -> "false"
        else -> "TODO(\"Provide default value\")"
    }
}
