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
        // Source nodes (0 inputs) are ViewModel-driven — no processing logic stub
        if (inputCount == 0) return false
        // Sink nodes (0 outputs) are pure state bridges — no processing logic stub
        if (outputCount == 0) return false
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
     * type parameters resolved from port data types. When anyInput is true,
     * returns any-input variant names (e.g., "In2AnyOut1TickBlock" instead of "In2Out1TickBlock").
     *
     * @param codeNode The code node
     * @param anyInput When true, returns any-input tick type alias variant
     * @return Tick type alias string (e.g., "Out2TickBlock<Int, Int>")
     */
    fun getTickTypeAlias(codeNode: CodeNode, anyInput: Boolean = false): String {
        val inputCount = codeNode.inputPorts.size
        val outputCount = codeNode.outputPorts.size
        val any = if (anyInput && inputCount >= 2) "Any" else ""

        return when {
            // Source nodes (0 inputs) — no tick type alias, ViewModel-driven
            inputCount == 0 -> ""

            // Filter vs Transformer (1 input, 1 output)
            inputCount == 1 && outputCount == 1 -> {
                val inT = inType(codeNode, 0)
                val outT = outType(codeNode, 0)
                if (inT == outT) "FilterTickBlock<$inT>"
                else "TransformerTickBlock<$inT, $outT>"
            }

            // Multi-input, single-output processors
            inputCount == 2 && outputCount == 1 ->
                "In2${any}Out1TickBlock<${inType(codeNode, 0)}, ${inType(codeNode, 1)}, ${outType(codeNode, 0)}>"
            inputCount == 3 && outputCount == 1 ->
                "In3${any}Out1TickBlock<${inType(codeNode, 0)}, ${inType(codeNode, 1)}, ${inType(codeNode, 2)}, ${outType(codeNode, 0)}>"

            // Single-input, multi-output processors
            inputCount == 1 && outputCount == 2 ->
                "In1Out2TickBlock<${inType(codeNode, 0)}, ${outType(codeNode, 0)}, ${outType(codeNode, 1)}>"
            inputCount == 1 && outputCount == 3 ->
                "In1Out3TickBlock<${inType(codeNode, 0)}, ${outType(codeNode, 0)}, ${outType(codeNode, 1)}, ${outType(codeNode, 2)}>"

            // Multi-input, multi-output processors
            inputCount == 2 && outputCount == 2 ->
                "In2${any}Out2TickBlock<${inType(codeNode, 0)}, ${inType(codeNode, 1)}, ${outType(codeNode, 0)}, ${outType(codeNode, 1)}>"
            inputCount == 2 && outputCount == 3 ->
                "In2${any}Out3TickBlock<${inType(codeNode, 0)}, ${inType(codeNode, 1)}, ${outType(codeNode, 0)}, ${outType(codeNode, 1)}, ${outType(codeNode, 2)}>"
            inputCount == 3 && outputCount == 2 ->
                "In3${any}Out2TickBlock<${inType(codeNode, 0)}, ${inType(codeNode, 1)}, ${inType(codeNode, 2)}, ${outType(codeNode, 0)}, ${outType(codeNode, 1)}>"
            inputCount == 3 && outputCount == 3 ->
                "In3${any}Out3TickBlock<${inType(codeNode, 0)}, ${inType(codeNode, 1)}, ${inType(codeNode, 2)}, ${outType(codeNode, 0)}, ${outType(codeNode, 1)}, ${outType(codeNode, 2)}>"

            // Sinks (0 outputs) — no tick type alias, pure state bridges
            outputCount == 0 -> ""

            else -> ""
        }
    }

    /**
     * Generates a tick function stub file content for a CodeNode.
     *
     * @param codeNode The code node to generate a stub for
     * @param packageName The base package name (logicmethods sub-package will be appended)
     * @return Generated Kotlin source code, or empty string if stub should not be generated
     */
    fun generateStub(codeNode: CodeNode, packageName: String): String {
        if (!shouldGenerateStub(codeNode)) return ""

        val anyInput = codeNode.configuration["_genericType"]?.contains("any") == true
        val tickTypeAlias = getTickTypeAlias(codeNode, anyInput)
        val tickValName = getTickValName(codeNode)
        val typeAliasName = tickTypeAlias.substringBefore("<")
        val inputCount = codeNode.inputPorts.size
        val outputCount = codeNode.outputPorts.size


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
            inputCount == 0 && outputCount > 0 -> "Source"
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

    /**
     * Extracts the lambda body from an existing stub file.
     *
     * The lambda body is everything between the `{ params ->` (or `{` for generators)
     * opening and the final closing `}`. Returns null if the pattern is not found.
     *
     * @param existingContent The content of the existing stub file
     * @return The lambda body lines (indented as in the original), or null if not parseable
     */
    fun extractLambdaBody(existingContent: String): String? {
        val lines = existingContent.lines()

        // Find the line containing `val ...Tick: ... = {`
        val openIndex = lines.indexOfFirst { it.trimStart().startsWith("val ") && it.contains("= {") }
        if (openIndex == -1) return null

        // Find the last `}` line (closing the lambda)
        val closeIndex = lines.indexOfLast { it.trim() == "}" }
        if (closeIndex == -1 || closeIndex <= openIndex) return null

        // Lambda body is everything between opening line and closing `}`
        val bodyLines = lines.subList(openIndex + 1, closeIndex)
        return bodyLines.joinToString("\n")
    }

    /**
     * Generates a stub with a preserved lambda body instead of the default TODO.
     *
     * Used during stub regeneration to update boilerplate (package, imports, KDoc,
     * type alias, parameter names) while keeping the user's implementation.
     *
     * @param codeNode The code node to generate a stub for
     * @param packageName The package name
     * @param preservedBody The lambda body to insert (from [extractLambdaBody])
     * @return Generated Kotlin source code with preserved body
     */
    fun generateStubWithPreservedBody(
        codeNode: CodeNode,
        packageName: String,
        preservedBody: String
    ): String {
        if (!shouldGenerateStub(codeNode)) return ""

        val anyInput = codeNode.configuration["_genericType"]?.contains("any") == true
        val tickTypeAlias = getTickTypeAlias(codeNode, anyInput)
        val tickValName = getTickValName(codeNode)
        val typeAliasName = tickTypeAlias.substringBefore("<")
        val inputCount = codeNode.inputPorts.size
        val outputCount = codeNode.outputPorts.size


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

            // Val declaration with new parameters but preserved body
            append("val $tickValName: $tickTypeAlias = {")

            // Lambda parameters for inputs
            if (inputCount > 0) {
                val params = codeNode.inputPorts.joinToString(", ") { it.name }
                append(" $params ->")
            }
            appendLine()

            // Preserved lambda body
            appendLine(preservedBody)

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
        else -> "TODO(\"Provide default value\")"
    }
}
