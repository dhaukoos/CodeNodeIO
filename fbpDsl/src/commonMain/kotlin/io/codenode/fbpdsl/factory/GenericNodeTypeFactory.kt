/*
 * GenericNodeTypeFactory - Factory Functions for Generic Node Types
 * Creates NodeTypeDefinition instances for generic nodes with configurable ports
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.factory

import io.codenode.fbpdsl.model.NodeTypeDefinition
import io.codenode.fbpdsl.model.Port
import io.codenode.fbpdsl.model.PortTemplate

/**
 * Factory object for creating generic NodeTypeDefinition instances.
 *
 * Generic nodes allow users to create flexible processing nodes with configurable
 * numbers of inputs (0-5) and outputs (0-5). This factory provides functions to
 * create individual node types and collections of predefined types.
 */
object GenericNodeTypeFactory {
    /** Minimum number of input/output ports */
    private const val MIN_PORTS = 0

    /** Maximum number of input/output ports */
    private const val MAX_PORTS = 5

    /** Lazy-initialized cache of all 36 generic node types */
    private val allGenericNodeTypesCache: List<NodeTypeDefinition> by lazy {
        buildList {
            for (inputs in MIN_PORTS..MAX_PORTS) {
                for (outputs in MIN_PORTS..MAX_PORTS) {
                    add(createGenericNodeType(inputs, outputs))
                }
            }
        }
    }

    /** Common generic node types for simplified palette */
    private val commonGenericNodeTypesCache: List<NodeTypeDefinition> by lazy {
        listOf(
            createGenericNodeType(0, 1), // Generator/Source
            createGenericNodeType(1, 0), // Sink
            createGenericNodeType(1, 1), // Simple Transformer
            createGenericNodeType(1, 2), // Splitter
            createGenericNodeType(2, 1)  // Merger
        )
    }
}

/**
 * Creates a NodeTypeDefinition for a generic node with the specified configuration.
 *
 * @param numInputs Number of input ports (must be 0-5)
 * @param numOutputs Number of output ports (must be 0-5)
 * @param customName Display name; defaults to "in{M}out{N}" pattern
 * @param customDescription Node description; generated if null
 * @param iconResource Path to custom icon resource
 * @param useCaseClassName Fully-qualified UseCase class name for processing logic
 * @param inputNames Custom names for input ports (size must match numInputs if provided)
 * @param outputNames Custom names for output ports (size must match numOutputs if provided)
 * @return NodeTypeDefinition configured for the specified port layout
 * @throws IllegalArgumentException if numInputs or numOutputs is out of range [0, 5]
 * @throws IllegalArgumentException if inputNames/outputNames size doesn't match port count
 */
fun createGenericNodeType(
    numInputs: Int,
    numOutputs: Int,
    customName: String? = null,
    customDescription: String? = null,
    iconResource: String? = null,
    useCaseClassName: String? = null,
    inputNames: List<String>? = null,
    outputNames: List<String>? = null
): NodeTypeDefinition {
    // Validate port counts
    require(numInputs in 0..5) {
        "numInputs must be between 0 and 5, was: $numInputs"
    }
    require(numOutputs in 0..5) {
        "numOutputs must be between 0 and 5, was: $numOutputs"
    }

    // Validate custom names match port counts
    if (inputNames != null) {
        require(inputNames.size == numInputs) {
            "inputNames size (${inputNames.size}) must match numInputs ($numInputs)"
        }
    }
    if (outputNames != null) {
        require(outputNames.size == numOutputs) {
            "outputNames size (${outputNames.size}) must match numOutputs ($numOutputs)"
        }
    }

    // Generate default name pattern
    val defaultName = "in${numInputs}out${numOutputs}"
    val name = customName ?: defaultName

    // Generate ID
    val id = if (customName != null) {
        customName.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "_")
    } else {
        "generic_in${numInputs}_out${numOutputs}"
    }

    // Generate description
    val description = customDescription ?: generateDescription(numInputs, numOutputs, useCaseClassName)

    // Generate port templates
    val effectiveInputNames = inputNames ?: (1..numInputs).map { "input$it" }
    val effectiveOutputNames = outputNames ?: (1..numOutputs).map { "output$it" }

    val portTemplates = buildList {
        effectiveInputNames.forEach { portName ->
            add(
                PortTemplate(
                    name = portName,
                    direction = Port.Direction.INPUT,
                    dataType = Any::class,
                    required = false,
                    description = "Input port: $portName"
                )
            )
        }
        effectiveOutputNames.forEach { portName ->
            add(
                PortTemplate(
                    name = portName,
                    direction = Port.Direction.OUTPUT,
                    dataType = Any::class,
                    required = false,
                    description = "Output port: $portName"
                )
            )
        }
    }

    // Build default configuration
    val defaultConfiguration = buildMap {
        put("_genericType", "in${numInputs}out${numOutputs}")
        if (iconResource != null) {
            put("_iconResource", iconResource)
        }
        if (useCaseClassName != null) {
            put("_useCaseClass", useCaseClassName)
        }
    }

    // Build code template for KMP
    val kmpCodeTemplate = generateKmpCodeTemplate(name, numInputs, numOutputs, useCaseClassName)

    return NodeTypeDefinition(
        id = id,
        name = name,
        category = NodeTypeDefinition.NodeCategory.GENERIC,
        description = description,
        portTemplates = portTemplates,
        defaultConfiguration = defaultConfiguration,
        configurationSchema = generateConfigurationSchema(numInputs, numOutputs),
        codeTemplates = mapOf("KMP" to kmpCodeTemplate)
    )
}

/**
 * Returns all 36 standard generic node type definitions.
 *
 * Covers all combinations of 0-5 inputs and 0-5 outputs.
 * First call creates all instances; subsequent calls return cached list.
 *
 * @return List of 36 NodeTypeDefinitions
 */
fun getAllGenericNodeTypes(): List<NodeTypeDefinition> {
    return GenericNodeTypeFactory.run {
        // Access the private cache through the companion object scope
        buildList {
            for (inputs in 0..5) {
                for (outputs in 0..5) {
                    add(createGenericNodeType(inputs, outputs))
                }
            }
        }
    }.let { types ->
        // Cache in a holder object
        AllTypesHolder.types
    }
}

/**
 * Returns commonly-used generic node types for a simplified palette.
 *
 * Contains 5 common patterns:
 * - in0out1: Generator/Source
 * - in1out0: Sink
 * - in1out1: Simple Transformer
 * - in1out2: Splitter
 * - in2out1: Merger
 *
 * @return List of 5 common NodeTypeDefinitions
 */
fun getCommonGenericNodeTypes(): List<NodeTypeDefinition> {
    return CommonTypesHolder.types
}

// Cache holders using lazy initialization
private object AllTypesHolder {
    val types: List<NodeTypeDefinition> by lazy {
        buildList {
            for (inputs in 0..5) {
                for (outputs in 0..5) {
                    add(createGenericNodeType(inputs, outputs))
                }
            }
        }
    }
}

private object CommonTypesHolder {
    val types: List<NodeTypeDefinition> by lazy {
        listOf(
            createGenericNodeType(0, 1), // Generator/Source
            createGenericNodeType(1, 0), // Sink
            createGenericNodeType(1, 1), // Simple Transformer
            createGenericNodeType(1, 2), // Splitter
            createGenericNodeType(2, 1)  // Merger
        )
    }
}

/**
 * Generates a description for a generic node type.
 */
private fun generateDescription(numInputs: Int, numOutputs: Int, useCaseClassName: String?): String {
    val inputDesc = when (numInputs) {
        0 -> "no inputs"
        1 -> "1 input"
        else -> "$numInputs inputs"
    }
    val outputDesc = when (numOutputs) {
        0 -> "no outputs"
        1 -> "1 output"
        else -> "$numOutputs outputs"
    }
    val useCaseDesc = if (useCaseClassName != null) {
        " (UseCase: ${useCaseClassName.substringAfterLast('.')})"
    } else {
        ""
    }
    return "Generic processing node with $inputDesc and $outputDesc$useCaseDesc"
}

/**
 * Generates a KMP code template for the generic node.
 */
private fun generateKmpCodeTemplate(
    name: String,
    numInputs: Int,
    numOutputs: Int,
    useCaseClassName: String?
): String {
    val className = name.split(" ", "_", "-")
        .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
        .let { if (it.first().isDigit()) "Node$it" else it }

    return buildString {
        appendLine("class ${className}Component : Component {")
        if (useCaseClassName != null) {
            appendLine("    private val useCase = $useCaseClassName()")
        }
        appendLine("    override suspend fun process(inputs: Map<String, Any>): Map<String, Any> {")
        if (useCaseClassName != null) {
            appendLine("        return useCase.execute(inputs)")
        } else {
            appendLine("        // TODO: Implement processing logic")
            appendLine("        return emptyMap()")
        }
        appendLine("    }")
        appendLine("}")
    }
}

/**
 * Generates a JSON Schema for the generic node configuration.
 */
private fun generateConfigurationSchema(numInputs: Int, numOutputs: Int): String {
    return """
        {
            "type": "object",
            "properties": {
                "_genericType": {
                    "type": "string",
                    "const": "in${numInputs}out${numOutputs}"
                },
                "_iconResource": {
                    "type": "string"
                },
                "_useCaseClass": {
                    "type": "string"
                }
            },
            "required": ["_genericType"]
        }
    """.trimIndent()
}
