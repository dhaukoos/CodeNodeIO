/*
 * ProcessingLogicStubGenerator
 * Generates ProcessingLogic stub files for CodeNodes
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.Port

/**
 * Generator for ProcessingLogic stub files.
 *
 * Generates a Kotlin source file with a stub implementation of ProcessingLogic
 * for each CodeNode. The generated stubs include:
 * - Class implementing ProcessingLogic interface
 * - Stub invoke() method with TODO
 * - KDoc with node type and port descriptions
 *
 * T032: Create ProcessingLogicStubGenerator class
 * T033: Implement class declaration with ProcessingLogic interface
 * T034: Implement invoke() method stub with NotImplementedError
 * T035: Implement KDoc generation with node type and port descriptions
 */
class ProcessingLogicStubGenerator {

    /**
     * Generates a ProcessingLogic stub file content for a CodeNode.
     *
     * @param codeNode The code node to generate a stub for
     * @param packageName The package name for the generated file
     * @return Generated Kotlin source code
     */
    fun generateStub(codeNode: CodeNode, packageName: String): String {
        val className = getClassName(codeNode)
        val builder = StringBuilder()

        // Package declaration
        builder.appendLine("package $packageName")
        builder.appendLine()

        // Imports
        builder.appendLine("import io.codenode.fbpdsl.model.ProcessingLogic")
        builder.appendLine("import io.codenode.fbpdsl.model.InformationPacket")
        builder.appendLine()

        // T035: KDoc with node type and port descriptions
        builder.append(generateKDoc(codeNode))

        // T033: Class declaration implementing ProcessingLogic
        builder.appendLine("class $className : ProcessingLogic {")
        builder.appendLine()

        // T034: invoke() method stub
        builder.append(generateInvokeMethod(codeNode))

        builder.appendLine("}")

        return builder.toString()
    }

    /**
     * Gets the filename for a ProcessingLogic stub.
     *
     * @param codeNode The code node
     * @return Filename in format NodeNameComponent.kt
     */
    fun getStubFileName(codeNode: CodeNode): String {
        return "${getClassName(codeNode)}.kt"
    }

    /**
     * Gets the class name for a ProcessingLogic stub.
     * Converts node name to PascalCase and adds Component suffix.
     */
    private fun getClassName(codeNode: CodeNode): String {
        val baseName = codeNode.name
            .split(" ", "-", "_")
            .filter { it.isNotBlank() }
            .joinToString("") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
            .filter { it.isLetterOrDigit() }

        return "${baseName}Component"
    }

    /**
     * T035: Generates KDoc comment with node type and port descriptions.
     */
    private fun generateKDoc(codeNode: CodeNode): String {
        val builder = StringBuilder()

        builder.appendLine("/**")
        builder.appendLine(" * ProcessingLogic implementation for ${codeNode.name} node.")
        builder.appendLine(" *")
        builder.appendLine(" * Node Type: ${codeNode.codeNodeType.name} - ${getNodeTypeDescription(codeNode)}")
        builder.appendLine(" *")

        // Input ports section
        if (codeNode.inputPorts.isNotEmpty()) {
            builder.appendLine(" * ## Input Ports")
            codeNode.inputPorts.forEach { port ->
                builder.appendLine(" * - **${port.name}**: ${port.dataType.simpleName ?: "Any"}${if (port.required) " (required)" else ""}")
            }
            builder.appendLine(" *")
        } else {
            builder.appendLine(" * ## Input Ports")
            builder.appendLine(" * - None (Generator node)")
            builder.appendLine(" *")
        }

        // Output ports section
        if (codeNode.outputPorts.isNotEmpty()) {
            builder.appendLine(" * ## Output Ports")
            codeNode.outputPorts.forEach { port ->
                builder.appendLine(" * - **${port.name}**: ${port.dataType.simpleName ?: "Any"}${if (port.required) " (required)" else ""}")
            }
            builder.appendLine(" *")
        } else {
            builder.appendLine(" * ## Output Ports")
            builder.appendLine(" * - None (Sink node)")
            builder.appendLine(" *")
        }

        builder.appendLine(" * @see ProcessingLogic")
        builder.appendLine(" */")

        return builder.toString()
    }

    /**
     * Gets a human-readable description of the node type.
     */
    private fun getNodeTypeDescription(codeNode: CodeNode): String {
        return when (codeNode.codeNodeType.name) {
            "GENERATOR" -> "Generates data (no input required)"
            "TRANSFORMER" -> "Transforms input data to output"
            "FILTER" -> "Filters input based on predicate"
            "SPLITTER" -> "Splits single input into multiple outputs"
            "MERGER" -> "Merges multiple inputs into single output"
            "VALIDATOR" -> "Validates input data against rules"
            "SINK" -> "Consumes data (no output)"
            "API_ENDPOINT" -> "API endpoint integration"
            "DATABASE" -> "Database operation"
            else -> "Custom processing node"
        }
    }

    /**
     * T034: Generates the invoke() method stub.
     */
    private fun generateInvokeMethod(codeNode: CodeNode): String {
        val builder = StringBuilder()
        val indent = "    "

        builder.appendLine("${indent}/**")
        builder.appendLine("${indent} * Processes input packets and produces output packets.")
        builder.appendLine("${indent} *")
        builder.appendLine("${indent} * @param inputs Map of input port name to InformationPacket")
        builder.appendLine("${indent} * @return Map of output port name to InformationPacket")
        builder.appendLine("${indent} */")
        builder.appendLine("${indent}override suspend operator fun invoke(")
        builder.appendLine("${indent}${indent}inputs: Map<String, InformationPacket<*>>")
        builder.appendLine("${indent}): Map<String, InformationPacket<*>> {")

        // Add helpful comments about expected inputs/outputs
        if (codeNode.inputPorts.isNotEmpty()) {
            builder.appendLine("${indent}${indent}// Expected inputs:")
            codeNode.inputPorts.forEach { port ->
                builder.appendLine("${indent}${indent}// - inputs[\"${port.name}\"]?.payload as? ${port.dataType.simpleName ?: "Any"}")
            }
            builder.appendLine()
        }

        if (codeNode.outputPorts.isNotEmpty()) {
            builder.appendLine("${indent}${indent}// Expected outputs:")
            codeNode.outputPorts.forEach { port ->
                builder.appendLine("${indent}${indent}// - \"${port.name}\" -> InformationPacket of ${port.dataType.simpleName ?: "Any"}")
            }
            builder.appendLine()
        }

        builder.appendLine("${indent}${indent}TODO(\"Implement ${codeNode.name} processing logic\")")
        builder.appendLine("${indent}}")
        builder.appendLine()

        return builder.toString()
    }
}
