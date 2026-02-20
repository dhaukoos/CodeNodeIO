/*
 * FlowGraphFactoryGenerator
 * Generates factory functions for FlowGraph instantiation with tick stubs
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*

/**
 * Result of tick stub validation.
 *
 * @property isValid Whether all required tick stub files exist
 * @property missingStubs List of stub file names that are missing
 */
data class ComponentValidationResult(
    val isValid: Boolean,
    val missingStubs: List<String> = emptyList()
) {
    // Kept as 'missingStubs' but property name preserved for backward compat
    val missingComponents: List<String> get() = missingStubs
}

/**
 * Generator for FlowGraph factory functions.
 *
 * Generates a Kotlin source file with a factory function that:
 * - Imports tick functions from the logicmethods package
 * - Creates NodeRuntime instances via CodeNodeFactory.createTimed* methods
 * - Passes tick function references to the factory methods
 */
class FlowGraphFactoryGenerator {

    /**
     * Gets the tick val name for a node.
     *
     * @param node The code node
     * @return Val name in format {nodeName}Tick
     */
    fun getTickValName(node: CodeNode): String {
        return "${node.name.camelCase()}Tick"
    }

    /**
     * Gets the import statement for a node's tick function.
     *
     * @param node The code node
     * @param packageName The base package name
     * @return Full import statement
     */
    fun getTickImport(node: CodeNode, packageName: String): String {
        return "$packageName.logicmethods.${getTickValName(node)}"
    }

    /**
     * Gets the CodeNodeFactory method name for a node based on its port configuration.
     *
     * @param node The code node
     * @return Factory method name (e.g., "createTimedOut2Generator")
     */
    fun getFactoryMethodName(node: CodeNode): String {
        val inputCount = node.inputPorts.size
        val outputCount = node.outputPorts.size

        return when {
            inputCount == 0 && outputCount == 1 -> "createTimedGenerator"
            inputCount == 0 && outputCount == 2 -> "createTimedOut2Generator"
            inputCount == 0 && outputCount == 3 -> "createTimedOut3Generator"

            inputCount == 1 && outputCount == 1 -> {
                val inType = node.inputPorts[0].dataType.simpleName ?: "Any"
                val outType = node.outputPorts[0].dataType.simpleName ?: "Any"
                if (inType == outType) "createTimedFilter" else "createTimedTransformer"
            }

            inputCount == 2 && outputCount == 1 -> "createTimedIn2Out1Processor"
            inputCount == 3 && outputCount == 1 -> "createTimedIn3Out1Processor"
            inputCount == 1 && outputCount == 2 -> "createTimedIn1Out2Processor"
            inputCount == 1 && outputCount == 3 -> "createTimedIn1Out3Processor"
            inputCount == 2 && outputCount == 2 -> "createTimedIn2Out2Processor"
            inputCount == 2 && outputCount == 3 -> "createTimedIn2Out3Processor"
            inputCount == 3 && outputCount == 2 -> "createTimedIn3Out2Processor"
            inputCount == 3 && outputCount == 3 -> "createTimedIn3Out3Processor"

            inputCount == 1 && outputCount == 0 -> "createTimedSink"
            inputCount == 2 && outputCount == 0 -> "createTimedIn2Sink"
            inputCount == 3 && outputCount == 0 -> "createTimedIn3Sink"

            else -> "createTimed"
        }
    }

    /**
     * Gets the type parameter string for the factory method call.
     *
     * @param node The code node
     * @return Type parameter string (e.g., "Int, Int")
     */
    fun getTypeParams(node: CodeNode): String {
        val inputCount = node.inputPorts.size
        val outputCount = node.outputPorts.size
        val inputTypes = node.inputPorts.map { it.dataType.simpleName ?: "Any" }
        val outputTypes = node.outputPorts.map { it.dataType.simpleName ?: "Any" }

        return when {
            // Generators: just output types
            inputCount == 0 -> outputTypes.joinToString(", ")
            // Sinks: just input types
            outputCount == 0 -> inputTypes.joinToString(", ")
            // Filter: single type
            inputCount == 1 && outputCount == 1 && inputTypes[0] == outputTypes[0] -> inputTypes[0]
            // Transformer: input, output
            inputCount == 1 && outputCount == 1 -> "${inputTypes[0]}, ${outputTypes[0]}"
            // Processors: input types, then output types
            else -> (inputTypes + outputTypes).joinToString(", ")
        }
    }

    /**
     * Generates a factory file content for a FlowGraph.
     *
     * @param flowGraph The flow graph to generate a factory for
     * @param packageName The package name for the generated file
     * @param usecasesPackage Optional package for tick stubs (defaults to packageName)
     * @return Generated Kotlin source code
     */
    fun generateFactory(flowGraph: FlowGraph, packageName: String, usecasesPackage: String? = null): String {
        val factoryFunctionName = "create${flowGraph.name.pascalCase()}FlowGraph"
        val allCodeNodes = flowGraph.getAllCodeNodes()
        val logicMethodsBase = usecasesPackage ?: packageName

        return buildString {
            // Package declaration
            appendLine("package $packageName")
            appendLine()

            // Imports
            appendLine("import io.codenode.fbpdsl.model.CodeNodeFactory")

            // Import tick functions from logicmethods
            allCodeNodes.forEach { node ->
                appendLine("import ${getTickImport(node, logicMethodsBase)}")
            }
            appendLine()

            // KDoc for factory function
            appendLine("/**")
            appendLine(" * Creates the runtime nodes for the ${flowGraph.name} flow graph.")
            appendLine(" *")
            appendLine(" * Nodes: ${allCodeNodes.size}")
            appendLine(" * Connections: ${flowGraph.connections.size}")
            appendLine(" *")
            appendLine(" * @generated by CodeNodeIO FlowGraphFactoryGenerator")
            appendLine(" */")

            // Factory function signature
            appendLine("fun $factoryFunctionName() {")

            val indent = "    "

            // Create NodeRuntime instances via CodeNodeFactory
            allCodeNodes.forEach { node ->
                val varName = node.name.camelCase()
                val factoryMethod = getFactoryMethodName(node)
                val typeParams = getTypeParams(node)
                val tickValName = getTickValName(node)
                val tickInterval = node.configuration["tickIntervalMs"] ?: "1000"

                appendLine("${indent}val $varName = CodeNodeFactory.$factoryMethod<$typeParams>(")
                appendLine("${indent}${indent}name = \"${node.name}\",")
                appendLine("${indent}${indent}tickIntervalMs = $tickInterval,")
                appendLine("${indent}${indent}tick = $tickValName")
                appendLine("${indent})")
                appendLine()
            }

            appendLine("}")
        }
    }

    /**
     * Gets the factory file name for a FlowGraph.
     *
     * @param flowGraph The flow graph
     * @return File name in format {GraphName}Factory.kt
     */
    fun getFactoryFileName(flowGraph: FlowGraph): String {
        return "${flowGraph.name.pascalCase()}Factory.kt"
    }

    /**
     * Gets the list of required tick stub file names.
     *
     * @param flowGraph The flow graph to analyze
     * @return List of unique tick stub file names required
     */
    fun getRequiredComponents(flowGraph: FlowGraph): List<String> {
        val stubGenerator = ProcessingLogicStubGenerator()
        return flowGraph.getAllCodeNodes()
            .filter { stubGenerator.shouldGenerateStub(it) }
            .map { "${it.name.pascalCase()}ProcessLogic" }
            .distinct()
    }

    /**
     * Validates that all required tick stub files exist.
     *
     * @param flowGraph The flow graph to validate
     * @param existingFiles Set of file names that exist in the source directory
     * @return ComponentValidationResult indicating validity and any missing stubs
     */
    fun validateComponents(flowGraph: FlowGraph, existingFiles: Set<String>): ComponentValidationResult {
        val required = getRequiredComponents(flowGraph)
        val missing = required.filter { stubName ->
            !existingFiles.contains("$stubName.kt")
        }
        return ComponentValidationResult(
            isValid = missing.isEmpty(),
            missingStubs = missing
        )
    }

    /**
     * Escapes special characters in strings for Kotlin code.
     */
    private fun escapeString(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
