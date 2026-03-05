/*
 * RuntimeTypeResolver
 * Maps node port counts to CodeNodeFactory method names and runtime types
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.CodeNode

/**
 * Resolves the CodeNodeFactory method name, runtime type, and tick parameter
 * name for a given CodeNode based on its input/output port counts.
 *
 * Uses the port count mapping from research.md R4 to determine:
 * - Which `CodeNodeFactory.create*()` method to call
 * - What runtime class the node maps to
 * - What the tick/consume/transform parameter is named
 */
class RuntimeTypeResolver {

    /**
     * Gets the CodeNodeFactory method name for creating a runtime instance.
     *
     * @param node The code node
     * @param anyInput When true, returns any-input factory variant (e.g., "createIn2AnyOut1Processor")
     * @return Factory method name (e.g., "createContinuousSource", "createIn2Sink")
     */
    fun getFactoryMethodName(node: CodeNode, anyInput: Boolean = false): String {
        val inputs = node.inputPorts.size
        val outputs = node.outputPorts.size
        val any = if (anyInput && inputs >= 2) "Any" else ""

        return when {
            // Generators (0 inputs)
            inputs == 0 && outputs == 1 -> "createContinuousSource"
            inputs == 0 && outputs == 2 -> "createTimedOut2Generator"
            inputs == 0 && outputs == 3 -> "createTimedOut3Generator"

            // Sinks (0 outputs)
            inputs == 1 && outputs == 0 -> "createContinuousSink"
            inputs == 2 && outputs == 0 -> "createIn2${any}Sink"
            inputs == 3 && outputs == 0 -> "createIn3${any}Sink"

            // Filter vs Transformer (1 in, 1 out)
            inputs == 1 && outputs == 1 -> {
                val inType = node.inputPorts[0].dataType.simpleName ?: "Any"
                val outType = node.outputPorts[0].dataType.simpleName ?: "Any"
                if (inType == outType) "createContinuousFilter" else "createContinuousTransformer"
            }

            // Multi-input processors
            inputs == 2 && outputs == 1 -> "createIn2${any}Out1Processor"
            inputs == 3 && outputs == 1 -> "createIn3${any}Out1Processor"

            // Single-input, multi-output processors
            inputs == 1 && outputs == 2 -> "createIn1Out2Processor"
            inputs == 1 && outputs == 3 -> "createIn1Out3Processor"

            // Multi-input, multi-output processors
            inputs == 2 && outputs == 2 -> "createIn2${any}Out2Processor"
            inputs == 2 && outputs == 3 -> "createIn2${any}Out3Processor"
            inputs == 3 && outputs == 2 -> "createIn3${any}Out2Processor"
            inputs == 3 && outputs == 3 -> "createIn3${any}Out3Processor"

            else -> "createContinuousSource" // fallback
        }
    }

    /**
     * Gets the runtime type name with generic type parameters.
     *
     * @param node The code node
     * @param anyInput When true, returns any-input runtime variant (e.g., "In2AnyOut1Runtime<Int, Int, Int>")
     * @return Runtime type string (e.g., "SourceRuntime<Int>", "Out2GeneratorRuntime<Int, Int>")
     */
    fun getRuntimeTypeName(node: CodeNode, anyInput: Boolean = false): String {
        val inputs = node.inputPorts.size
        val outputs = node.outputPorts.size
        val any = if (anyInput && inputs >= 2) "Any" else ""

        val typeParams = buildTypeParams(node)

        return when {
            inputs == 0 && outputs == 1 -> "SourceRuntime<$typeParams>"
            inputs == 0 && outputs == 2 -> "Out2GeneratorRuntime<$typeParams>"
            inputs == 0 && outputs == 3 -> "Out3GeneratorRuntime<$typeParams>"

            inputs == 1 && outputs == 0 -> "SinkRuntime<$typeParams>"
            inputs == 2 && outputs == 0 -> "In2${any}SinkRuntime<$typeParams>"
            inputs == 3 && outputs == 0 -> "In3${any}SinkRuntime<$typeParams>"

            inputs == 1 && outputs == 1 -> {
                val inType = node.inputPorts[0].dataType.simpleName ?: "Any"
                val outType = node.outputPorts[0].dataType.simpleName ?: "Any"
                if (inType == outType) "FilterRuntime<$inType>" else "TransformerRuntime<$typeParams>"
            }

            inputs == 2 && outputs == 1 -> "In2${any}Out1Runtime<$typeParams>"
            inputs == 3 && outputs == 1 -> "In3${any}Out1Runtime<$typeParams>"
            inputs == 1 && outputs == 2 -> "In1Out2Runtime<$typeParams>"
            inputs == 1 && outputs == 3 -> "In1Out3Runtime<$typeParams>"
            inputs == 2 && outputs == 2 -> "In2${any}Out2Runtime<$typeParams>"
            inputs == 2 && outputs == 3 -> "In2${any}Out3Runtime<$typeParams>"
            inputs == 3 && outputs == 2 -> "In3${any}Out2Runtime<$typeParams>"
            inputs == 3 && outputs == 3 -> "In3${any}Out3Runtime<$typeParams>"

            else -> "SourceRuntime<Any>"
        }
    }

    /**
     * Gets the tick/consume/transform parameter name for the factory method.
     *
     * @param node The code node
     * @param anyInput When true, uses any-input variant (parameter names are the same)
     * @return Parameter name ("tick", "consume", "transform", "filter", "process")
     */
    fun getTickParamName(node: CodeNode, anyInput: Boolean = false): String {
        val inputs = node.inputPorts.size
        val outputs = node.outputPorts.size

        return when {
            inputs == 0 -> "tick"
            outputs == 0 -> "consume"
            inputs == 1 && outputs == 1 -> {
                val inType = node.inputPorts[0].dataType.simpleName ?: "Any"
                val outType = node.outputPorts[0].dataType.simpleName ?: "Any"
                if (inType == outType) "filter" else "transform"
            }
            else -> "process"
        }
    }

    /**
     * Gets the generic type parameters string for a node.
     *
     * @param node The code node
     * @return Comma-separated type parameters (e.g., "Int, Int")
     */
    fun getTypeParams(node: CodeNode): String = buildTypeParams(node)

    /**
     * Builds comma-separated type parameters from input + output port types.
     */
    private fun buildTypeParams(node: CodeNode): String {
        val types = mutableListOf<String>()
        node.inputPorts.forEach { types.add(it.dataType.simpleName ?: "Any") }
        node.outputPorts.forEach { types.add(it.dataType.simpleName ?: "Any") }
        return types.joinToString(", ")
    }
}
