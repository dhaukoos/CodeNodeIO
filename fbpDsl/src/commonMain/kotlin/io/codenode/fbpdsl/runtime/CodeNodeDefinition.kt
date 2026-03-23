/*
 * CodeNodeDefinition - Self-contained node definition interface
 * Bundles metadata (name, category, ports) and processing logic in a single file
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.NodeTypeDefinition
import io.codenode.fbpdsl.model.Port
import io.codenode.fbpdsl.model.PortTemplate
import kotlin.reflect.KClass

/**
 * Describes a single port on a node definition.
 *
 * @property name Port display name (e.g., "image", "result")
 * @property dataType The data type flowing through this port
 */
data class PortSpec(
    val name: String,
    val dataType: KClass<*>
)

/**
 * The contract that all self-contained node definitions must implement.
 *
 * A CodeNodeDefinition is a Kotlin object (singleton) that bundles:
 * - Metadata: name, category, description, ports
 * - Factory: createRuntime() produces a fully configured NodeRuntime
 * - Conversion: toNodeTypeDefinition() for palette display
 *
 * This replaces the fragmented pattern of separate CustomNodeDefinition +
 * generated runtime references.
 */
interface CodeNodeDefinition {
    /** Unique node name for palette and registry lookup */
    val name: String

    /** Determines palette group, color coding, and runtime type */
    val category: CodeNodeType

    /** Human-readable description shown in palette tooltip */
    val description: String?
        get() = null

    /** Input port definitions (may be empty for sources) */
    val inputPorts: List<PortSpec>

    /** Output port definitions (may be empty for sinks) */
    val outputPorts: List<PortSpec>

    /** When true, uses any-input runtime variants (fires on ANY input, not ALL) */
    val anyInput: Boolean
        get() = false

    /** Absolute path to the source `.kt` file for this node definition, if known.
     *  Used by the graphEditor to enable in-app editing of node source code. */
    val sourceFilePath: String?
        get() = null

    /**
     * Creates a fully configured NodeRuntime instance with processing logic embedded.
     *
     * The returned runtime type should match the port configuration:
     * - 0 inputs, 1 output → SourceRuntime
     * - 0 inputs, 2 outputs → SourceOut2Runtime
     * - 1 input, 0 outputs → SinkRuntime
     * - 1 input, 1 output → TransformerRuntime
     * - 2 inputs, 1 output → In2Out1Runtime
     * - (etc. for all runtime type combinations)
     *
     * @param name Instance name for the runtime (may differ from definition name
     *             if multiple instances exist)
     * @return A NodeRuntime subclass with processing logic ready to execute
     */
    fun createRuntime(name: String): NodeRuntime

    /**
     * Converts this node definition to a NodeTypeDefinition for palette display.
     *
     * Default implementation builds a NodeTypeDefinition from the interface
     * properties using the generic node type factory.
     *
     * @return NodeTypeDefinition with correct name, category, port templates
     */
    fun toNodeTypeDefinition(): NodeTypeDefinition {
        val portTemplates = buildList {
            inputPorts.forEach { port ->
                add(
                    PortTemplate(
                        name = port.name,
                        direction = Port.Direction.INPUT,
                        dataType = port.dataType,
                        required = false,
                        description = "Input port: ${port.name}"
                    )
                )
            }
            outputPorts.forEach { port ->
                add(
                    PortTemplate(
                        name = port.name,
                        direction = Port.Direction.OUTPUT,
                        dataType = port.dataType,
                        required = false,
                        description = "Output port: ${port.name}"
                    )
                )
            }
        }

        val nodeDescription = description
            ?: "${category.typeName} node with ${inputPorts.size} input(s) and ${outputPorts.size} output(s)"

        return NodeTypeDefinition(
            id = name.lowercase().replace(" ", "_"),
            name = name,
            category = category,
            description = nodeDescription,
            portTemplates = portTemplates,
            defaultConfiguration = mapOf(
                "_genericType" to "in${inputPorts.size}${if (anyInput && inputPorts.size >= 2) "any" else ""}out${outputPorts.size}",
                "_codeNodeDefinition" to "true",
                "_codeNodeClass" to (this::class.qualifiedName ?: "")
            )
        )
    }
}
