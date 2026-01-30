/*
 * GenericNodeGenerator - Code Generation for Generic Nodes
 * Generates KMP components from generic nodes with configurable inputs/outputs
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.Port

/**
 * Generates Kotlin components from generic nodes.
 *
 * Generic nodes are identified by the presence of "_genericType" in their configuration.
 * When a "_useCaseClass" is specified, the generated component delegates to that UseCase.
 * When no UseCase is specified, a placeholder component with TODO markers is generated.
 *
 * Generated components include:
 * - Input channel properties for each input port
 * - Output channel properties for each output port
 * - A process() function that either delegates to UseCase or contains TODO placeholders
 * - A start() function for coroutine-based execution
 */
class GenericNodeGenerator {

    companion object {
        const val GENERATED_PACKAGE = "io.codenode.generated"
        const val GENERIC_TYPE_KEY = "_genericType"
        const val USE_CASE_CLASS_KEY = "_useCaseClass"
    }

    /**
     * Checks if a node is a generic node (has _genericType in configuration).
     *
     * @param node The CodeNode to check
     * @return true if the node has _genericType configuration, false otherwise
     */
    fun supportsGenericNode(node: CodeNode): Boolean {
        return node.configuration.containsKey(GENERIC_TYPE_KEY)
    }

    /**
     * Generates a component class from a generic CodeNode.
     *
     * If the node has a _useCaseClass, generates a component that delegates to the UseCase.
     * Otherwise, generates a placeholder component with TODO markers.
     *
     * @param node The generic CodeNode to generate a component for
     * @return FileSpec containing the generated Kotlin file
     */
    fun generateComponent(node: CodeNode): FileSpec {
        val className = node.name.pascalCase()
        val classNameObj = ClassName(GENERATED_PACKAGE, className)

        val useCaseClass = node.configuration[USE_CASE_CLASS_KEY]

        val componentClass = if (useCaseClass != null) {
            generateUseCaseComponent(node, classNameObj, useCaseClass)
        } else {
            generatePlaceholderComponent(node, classNameObj)
        }

        return FileSpec.builder(GENERATED_PACKAGE, className)
            .addType(componentClass)
            .addImport("kotlinx.coroutines.flow", "Flow", "MutableSharedFlow", "collect")
            .addImport("kotlinx.coroutines", "CoroutineScope", "launch")
            .build()
    }

    /**
     * Generates a component that delegates to a UseCase class.
     */
    private fun generateUseCaseComponent(
        node: CodeNode,
        className: ClassName,
        useCaseClassName: String
    ): TypeSpec {
        val useCaseSimpleName = useCaseClassName.substringAfterLast(".")
        val useCasePackage = if (useCaseClassName.contains(".")) {
            useCaseClassName.substringBeforeLast(".")
        } else {
            GENERATED_PACKAGE
        }
        val useCaseType = ClassName(useCasePackage, useCaseSimpleName)

        val classBuilder = TypeSpec.classBuilder(className)
            .addKdoc(generateKDoc(node, useCaseClassName))
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("useCase", useCaseType)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("useCase", useCaseType)
                    .initializer("useCase")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )

        // Add input channel properties
        node.inputPorts.forEach { port ->
            classBuilder.addProperty(generateInputChannelProperty(port))
        }

        // Add output channel properties
        node.outputPorts.forEach { port ->
            classBuilder.addProperty(generateOutputChannelProperty(port))
        }

        // Add genericType property for documentation
        node.configuration[GENERIC_TYPE_KEY]?.let { genericType ->
            classBuilder.addProperty(
                PropertySpec.builder("genericType", String::class)
                    .initializer("%S", genericType)
                    .addModifiers(KModifier.PRIVATE)
                    .addKdoc("Generic node type pattern: $genericType")
                    .build()
            )
        }

        // Add process function that delegates to UseCase
        classBuilder.addFunction(generateUseCaseProcessFunction(node))

        // Add start function
        classBuilder.addFunction(generateStartFunction(node))

        return classBuilder.build()
    }

    /**
     * Generates a placeholder component with TODO markers when no UseCase is specified.
     */
    private fun generatePlaceholderComponent(
        node: CodeNode,
        className: ClassName
    ): TypeSpec {
        val classBuilder = TypeSpec.classBuilder(className)
            .addKdoc(generatePlaceholderKDoc(node))

        // Add input channel properties
        node.inputPorts.forEach { port ->
            classBuilder.addProperty(generateInputChannelProperty(port))
        }

        // Add output channel properties
        node.outputPorts.forEach { port ->
            classBuilder.addProperty(generateOutputChannelProperty(port))
        }

        // Add genericType property for documentation
        node.configuration[GENERIC_TYPE_KEY]?.let { genericType ->
            classBuilder.addProperty(
                PropertySpec.builder("genericType", String::class)
                    .initializer("%S", genericType)
                    .addModifiers(KModifier.PRIVATE)
                    .addKdoc("Generic node type pattern: $genericType")
                    .build()
            )
        }

        // Add placeholder process function
        classBuilder.addFunction(generatePlaceholderProcessFunction(node))

        // Add start function
        classBuilder.addFunction(generateStartFunction(node))

        return classBuilder.build()
    }

    /**
     * Generates KDoc for a UseCase-delegating component.
     */
    private fun generateKDoc(node: CodeNode, useCaseClassName: String): CodeBlock {
        val genericType = node.configuration[GENERIC_TYPE_KEY] ?: "unknown"
        return CodeBlock.builder()
            .add("Generated component for generic node: ${node.name}\n")
            .add("\n")
            .add("Generic Type: $genericType\n")
            .add("UseCase: $useCaseClassName\n")
            .add("Inputs: ${node.inputPorts.size} (${node.inputPorts.joinToString { it.name }})\n")
            .add("Outputs: ${node.outputPorts.size} (${node.outputPorts.joinToString { it.name }})\n")
            .apply {
                node.description?.let { add("\nDescription: $it\n") }
            }
            .add("\n")
            .add("@generated by CodeNodeIO KotlinCompiler (GenericNodeGenerator)\n")
            .build()
    }

    /**
     * Generates KDoc for a placeholder component.
     */
    private fun generatePlaceholderKDoc(node: CodeNode): CodeBlock {
        val genericType = node.configuration[GENERIC_TYPE_KEY] ?: "unknown"
        return CodeBlock.builder()
            .add("Generated placeholder component for generic node: ${node.name}\n")
            .add("\n")
            .add("Generic Type: $genericType\n")
            .add("Inputs: ${node.inputPorts.size} (${node.inputPorts.joinToString { it.name }})\n")
            .add("Outputs: ${node.outputPorts.size} (${node.outputPorts.joinToString { it.name }})\n")
            .add("\n")
            .add("TODO: Implement processing logic or assign a UseCase class.\n")
            .apply {
                node.description?.let { add("\nDescription: $it\n") }
            }
            .add("\n")
            .add("@generated by CodeNodeIO KotlinCompiler (GenericNodeGenerator)\n")
            .build()
    }

    /**
     * Generates an input channel property.
     */
    private fun generateInputChannelProperty(port: Port<*>): PropertySpec {
        val flowType = ClassName("kotlinx.coroutines.flow", "MutableSharedFlow")
            .parameterizedBy(ANY)
        val sanitizedName = port.name.sanitizeIdentifier()

        return PropertySpec.builder(
            "${sanitizedName}Input",
            flowType
        )
            .initializer("%T()", ClassName("kotlinx.coroutines.flow", "MutableSharedFlow"))
            .addKdoc("Input channel for port: ${port.name}")
            .build()
    }

    /**
     * Generates an output channel property.
     */
    private fun generateOutputChannelProperty(port: Port<*>): PropertySpec {
        val flowType = ClassName("kotlinx.coroutines.flow", "MutableSharedFlow")
            .parameterizedBy(ANY)
        val sanitizedName = port.name.sanitizeIdentifier()

        return PropertySpec.builder(
            "${sanitizedName}Output",
            flowType
        )
            .initializer("%T()", ClassName("kotlinx.coroutines.flow", "MutableSharedFlow"))
            .addKdoc("Output channel for port: ${port.name}")
            .build()
    }

    /**
     * Generates a process function that delegates to the UseCase.
     */
    private fun generateUseCaseProcessFunction(node: CodeNode): FunSpec {
        val funBuilder = FunSpec.builder("process")
            .addModifiers(KModifier.SUSPEND)
            .addKdoc("Processes input data by delegating to the UseCase.\n")

        // Add parameters for each input port
        node.inputPorts.forEach { port ->
            val sanitizedName = port.name.sanitizeIdentifier()
            funBuilder.addParameter(sanitizedName, ANY.copy(nullable = true))
        }

        // Generate return type and body based on output count
        when (node.outputPorts.size) {
            0 -> {
                // No outputs - Unit return, call execute
                funBuilder.addCode(
                    CodeBlock.builder()
                        .addStatement("useCase.execute(${node.inputPorts.joinToString(", ") { it.name.sanitizeIdentifier() }})")
                        .build()
                )
            }
            1 -> {
                // Single output - return the result
                funBuilder.returns(ANY.copy(nullable = true))
                funBuilder.addCode(
                    CodeBlock.builder()
                        .addStatement("return useCase.execute(${node.inputPorts.joinToString(", ") { it.name.sanitizeIdentifier() }})")
                        .build()
                )
            }
            else -> {
                // Multiple outputs - return a map
                val mapType = MAP.parameterizedBy(STRING, ANY.copy(nullable = true))
                funBuilder.returns(mapType)
                funBuilder.addCode(
                    CodeBlock.builder()
                        .addStatement("val result = useCase.execute(${node.inputPorts.joinToString(", ") { it.name.sanitizeIdentifier() }})")
                        .addStatement("return result as? Map<String, Any?> ?: mapOf(${node.outputPorts.joinToString(", ") { "\"${it.name}\" to result" }})")
                        .build()
                )
            }
        }

        return funBuilder.build()
    }

    /**
     * Generates a placeholder process function with TODO markers.
     */
    private fun generatePlaceholderProcessFunction(node: CodeNode): FunSpec {
        val funBuilder = FunSpec.builder("process")
            .addModifiers(KModifier.SUSPEND)
            .addKdoc("Processes input data.\n")
            .addKdoc("\n")
            .addKdoc("TODO: Implement processing logic for this generic node.\n")

        // Add parameters for each input port
        node.inputPorts.forEach { port ->
            val sanitizedName = port.name.sanitizeIdentifier()
            funBuilder.addParameter(sanitizedName, ANY.copy(nullable = true))
        }

        // Generate return type and body based on output count
        when (node.outputPorts.size) {
            0 -> {
                // No outputs - Unit return
                funBuilder.addCode(
                    CodeBlock.builder()
                        .addStatement("// TODO: Implement sink/consumer logic")
                        .addStatement("// Process: ${node.inputPorts.joinToString(", ") { it.name }}")
                        .build()
                )
            }
            1 -> {
                // Single output - return null placeholder
                funBuilder.returns(ANY.copy(nullable = true))
                funBuilder.addCode(
                    CodeBlock.builder()
                        .addStatement("// TODO: Implement transformation logic")
                        .addStatement("// Inputs: ${node.inputPorts.joinToString(", ") { it.name }}")
                        .addStatement("// Output: ${node.outputPorts.first().name}")
                        .addStatement("return null")
                        .build()
                )
            }
            else -> {
                // Multiple outputs - return empty map placeholder
                val mapType = MAP.parameterizedBy(STRING, ANY.copy(nullable = true))
                funBuilder.returns(mapType)
                funBuilder.addCode(
                    CodeBlock.builder()
                        .addStatement("// TODO: Implement multi-output logic")
                        .addStatement("// Inputs: ${node.inputPorts.joinToString(", ") { it.name }}")
                        .addStatement("// Outputs: ${node.outputPorts.joinToString(", ") { it.name }}")
                        .addStatement("return mapOf(${node.outputPorts.joinToString(", ") { "\"${it.name}\" to null" }})")
                        .build()
                )
            }
        }

        return funBuilder.build()
    }

    /**
     * Generates the start function for coroutine execution.
     */
    private fun generateStartFunction(node: CodeNode): FunSpec {
        val scopeType = ClassName("kotlinx.coroutines", "CoroutineScope")

        return FunSpec.builder("start")
            .addParameter("scope", scopeType)
            .addKdoc("Starts the component's processing loop in the given coroutine scope.\n")
            .addCode(
                CodeBlock.builder()
                    .beginControlFlow("scope.launch")
                    .apply {
                        if (node.inputPorts.isNotEmpty()) {
                            val firstInput = node.inputPorts.first()
                            val sanitizedName = firstInput.name.sanitizeIdentifier()
                            beginControlFlow("${sanitizedName}Input.collect { input ->")

                            // Call process with all inputs (simplified - uses first input for all)
                            val processCall = if (node.inputPorts.size == 1) {
                                "val result = process(input)"
                            } else {
                                "val result = process(${node.inputPorts.joinToString(", ") { "input" }})"
                            }
                            addStatement(processCall)

                            // Emit to outputs
                            if (node.outputPorts.isNotEmpty()) {
                                val firstOutput = node.outputPorts.first()
                                val outputName = firstOutput.name.sanitizeIdentifier()
                                addStatement("${outputName}Output.emit(result)")
                            }
                            endControlFlow()
                        } else {
                            // Generator pattern - no inputs
                            addStatement("// Generator node - implement generation loop")
                            addStatement("val result = process()")
                            if (node.outputPorts.isNotEmpty()) {
                                val firstOutput = node.outputPorts.first()
                                val outputName = firstOutput.name.sanitizeIdentifier()
                                addStatement("${outputName}Output.emit(result)")
                            }
                        }
                    }
                    .endControlFlow()
                    .build()
            )
            .build()
    }
}

/**
 * Sanitizes a string to be a valid Kotlin identifier.
 * Removes hyphens, underscores become camelCase boundaries.
 */
private fun String.sanitizeIdentifier(): String {
    if (this.isBlank()) return "unnamed"

    // Replace hyphens and underscores, convert to camelCase
    return this
        .replace("-", "_")
        .split("_")
        .mapIndexed { index, part ->
            if (index == 0) part.lowercase()
            else part.replaceFirstChar { it.uppercase() }
        }
        .joinToString("")
        .replace(Regex("[^a-zA-Z0-9]"), "")
        .ifBlank { "unnamed" }
}
