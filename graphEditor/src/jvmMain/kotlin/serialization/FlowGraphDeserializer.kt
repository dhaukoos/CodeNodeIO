/*
 * FlowGraphDeserializer - Graph Deserialization from .flow.kts Files
 * Loads FlowGraph instances from Kotlin DSL script files using the Kotlin scripting engine
 * License: Apache 2.0
 */

package io.codenode.grapheditor.serialization

import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.model.ValidationResult
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port
import io.codenode.fbpdsl.model.Connection
import java.io.File
import java.io.Reader
import javax.script.ScriptEngineManager
import javax.script.ScriptException

/**
 * Deserializes FlowGraph instances from .flow.kts Kotlin DSL files
 * Uses Kotlin scripting engine to evaluate DSL scripts
 */
object FlowGraphDeserializer {

    /**
     * Deserializes a FlowGraph from a Kotlin DSL string
     *
     * @param dslContent The Kotlin DSL script content
     * @return DeserializationResult with the graph or error information
     */
    fun deserialize(dslContent: String): DeserializationResult {
        return try {
            // Try to use Kotlin scripting engine if available
            val engine = try {
                ScriptEngineManager().getEngineByExtension("kts")
            } catch (e: Exception) {
                null
            }

            if (engine != null) {
                // Use Kotlin scripting engine
                try {
                    val result = engine.eval(dslContent)

                    if (result is FlowGraph) {
                        DeserializationResult.success(result)
                    } else {
                        DeserializationResult.error(
                            "Script did not produce a FlowGraph. Got: ${result?.javaClass?.simpleName ?: "null"}"
                        )
                    }
                } catch (e: ScriptException) {
                    DeserializationResult.error(
                        "Script evaluation failed: ${e.message}",
                        e
                    )
                }
            } else {
                // Fallback: Parse DSL manually (simplified parsing)
                deserializeManually(dslContent)
            }
        } catch (e: Exception) {
            DeserializationResult.error(
                "Deserialization failed: ${e.message}",
                e
            )
        }
    }

    /**
     * Deserializes a FlowGraph from a file
     *
     * @param file The .flow.kts file to read
     * @return DeserializationResult with the graph or error information
     */
    fun deserializeFromFile(file: File): DeserializationResult {
        return try {
            if (!file.exists()) {
                return DeserializationResult.error("File not found: ${file.absolutePath}")
            }

            if (!file.canRead()) {
                return DeserializationResult.error("File not readable: ${file.absolutePath}")
            }

            val content = file.readText()
            deserialize(content)
        } catch (e: Exception) {
            DeserializationResult.error(
                "Failed to read file: ${e.message}",
                e
            )
        }
    }

    /**
     * Deserializes a FlowGraph from a Reader
     *
     * @param reader The reader to read from
     * @return DeserializationResult with the graph or error information
     */
    fun deserializeFromReader(reader: Reader): DeserializationResult {
        return try {
            val content = reader.readText()
            deserialize(content)
        } catch (e: Exception) {
            DeserializationResult.error(
                "Failed to read from reader: ${e.message}",
                e
            )
        }
    }

    /**
     * Manual deserialization fallback (simplified parsing)
     * This is a basic implementation that handles simple cases
     * For production use, proper Kotlin script evaluation should be used
     */
    private fun deserializeManually(dslContent: String): DeserializationResult {
        return try {
            // Extract graph name and version from flowGraph(...) declaration
            val flowGraphPattern = Regex("""flowGraph\s*\(\s*"([^"]*)"\s*,\s*version\s*=\s*"([^"]*)"\s*(?:,\s*description\s*=\s*"([^"]*)"\s*)?\)\s*\{""")
            val flowGraphMatch = flowGraphPattern.find(dslContent)
                ?: return DeserializationResult.error("Could not find flowGraph declaration in file")

            val name = flowGraphMatch.groupValues[1]
            val version = flowGraphMatch.groupValues[2]
            val description = flowGraphMatch.groupValues.getOrNull(3)?.takeIf { it.isNotEmpty() }

            // Parse nodes
            val nodes = mutableListOf<CodeNode>()
            val nodeIdMap = mutableMapOf<String, String>() // variable name -> node ID

            val nodePattern = Regex("""val\s+(\w+)\s*=\s*codeNode\s*\(\s*"([^"]*)"\s*(?:,\s*nodeType\s*=\s*"([^"]*)"\s*)?\)\s*\{([^}]*)\}""", RegexOption.DOT_MATCHES_ALL)
            nodePattern.findAll(dslContent).forEach { match ->
                val varName = match.groupValues[1]
                val nodeName = match.groupValues[2]
                val nodeBody = match.groupValues[4]

                // Parse position
                val posPattern = Regex("""position\s*\(\s*([\d.]+)\s*,\s*([\d.]+)\s*\)""")
                val posMatch = posPattern.find(nodeBody)
                val x = posMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                val y = posMatch?.groupValues?.get(2)?.toDoubleOrNull() ?: 0.0

                // Parse description
                val descPattern = Regex("""description\s*=\s*"([^"]*)"""")
                val nodeDesc = descPattern.find(nodeBody)?.groupValues?.get(1)

                val nodeId = "node_${System.currentTimeMillis()}_${(0..9999).random()}"

                // Parse input ports
                val inputPorts = mutableListOf<Port<Any>>()
                val inputPattern = Regex("""input\s*\(\s*"([^"]*)"\s*,\s*(\w+)::class(?:\s*,\s*required\s*=\s*(true|false))?\s*\)""")
                inputPattern.findAll(nodeBody).forEach { inputMatch ->
                    val portName = inputMatch.groupValues[1]
                    val portId = "port_${System.currentTimeMillis()}_${(0..9999).random()}_$portName"
                    inputPorts.add(Port<Any>(
                        id = portId,
                        name = portName,
                        direction = Port.Direction.INPUT,
                        dataType = Any::class,
                        owningNodeId = nodeId
                    ))
                }

                // Parse output ports
                val outputPorts = mutableListOf<Port<Any>>()
                val outputPattern = Regex("""output\s*\(\s*"([^"]*)"\s*,\s*(\w+)::class(?:\s*,\s*required\s*=\s*(true|false))?\s*\)""")
                outputPattern.findAll(nodeBody).forEach { outputMatch ->
                    val portName = outputMatch.groupValues[1]
                    val portId = "port_${System.currentTimeMillis()}_${(0..9999).random()}_$portName"
                    outputPorts.add(Port<Any>(
                        id = portId,
                        name = portName,
                        direction = Port.Direction.OUTPUT,
                        dataType = Any::class,
                        owningNodeId = nodeId
                    ))
                }

                val node = CodeNode(
                    id = nodeId,
                    name = nodeName,
                    codeNodeType = CodeNodeType.CUSTOM,
                    description = nodeDesc,
                    position = Node.Position(x, y),
                    inputPorts = inputPorts,
                    outputPorts = outputPorts
                )

                nodes.add(node)
                nodeIdMap[varName] = nodeId
            }

            // Parse connections
            val connections = mutableListOf<Connection>()
            val connPattern = Regex("""(\w+)\.output\s*\(\s*"([^"]*)"\s*\)\s*connect\s*(\w+)\.input\s*\(\s*"([^"]*)"\s*\)""")
            connPattern.findAll(dslContent).forEach { match ->
                val sourceVar = match.groupValues[1]
                val sourcePortName = match.groupValues[2]
                val targetVar = match.groupValues[3]
                val targetPortName = match.groupValues[4]

                val sourceNodeId = nodeIdMap[sourceVar]
                val targetNodeId = nodeIdMap[targetVar]

                if (sourceNodeId != null && targetNodeId != null) {
                    // Find the actual port IDs
                    val sourceNode = nodes.find { it.id == sourceNodeId }
                    val targetNode = nodes.find { it.id == targetNodeId }

                    val sourcePort = sourceNode?.outputPorts?.find { it.name == sourcePortName }
                    val targetPort = targetNode?.inputPorts?.find { it.name == targetPortName }

                    if (sourcePort != null && targetPort != null) {
                        connections.add(Connection(
                            id = "conn_${System.currentTimeMillis()}_${(0..9999).random()}",
                            sourceNodeId = sourceNodeId,
                            sourcePortId = sourcePort.id,
                            targetNodeId = targetNodeId,
                            targetPortId = targetPort.id
                        ))
                    }
                }
            }

            // Create the FlowGraph manually
            val graphId = "graph_${System.currentTimeMillis()}_${(0..999999).random()}"
            val graph = FlowGraph(
                id = graphId,
                name = name,
                version = version,
                description = description,
                rootNodes = nodes,
                connections = connections,
                metadata = emptyMap(),
                targetPlatforms = emptyList()
            )

            DeserializationResult.success(graph)
        } catch (e: Exception) {
            DeserializationResult.error(
                "Manual deserialization failed: ${e.message}",
                e
            )
        }
    }

    /**
     * Validates a .flow.kts file before attempting to deserialize
     *
     * @param file The file to validate
     * @return ValidationResult with any pre-deserialization issues
     */
    fun validateFile(file: File): ValidationResult {
        val errors = mutableListOf<String>()

        // Check file exists
        if (!file.exists()) {
            errors.add("File does not exist: ${file.absolutePath}")
        }

        // Check file is readable
        if (file.exists() && !file.canRead()) {
            errors.add("File is not readable: ${file.absolutePath}")
        }

        // Check file extension
        if (!file.name.endsWith(".flow.kts") && !file.name.endsWith(".kts")) {
            errors.add("File does not have .flow.kts or .kts extension: ${file.name}")
        }

        // Check file is not empty
        if (file.exists() && file.length() == 0L) {
            errors.add("File is empty: ${file.absolutePath}")
        }

        // Basic syntax check (look for flowGraph declaration)
        if (file.exists() && file.canRead()) {
            try {
                val content = file.readText()
                if (!content.contains("flowGraph")) {
                    errors.add("File does not contain 'flowGraph' DSL declaration")
                }
            } catch (e: Exception) {
                errors.add("Failed to read file for validation: ${e.message}")
            }
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Loads a FlowGraph from a file, throwing exception on failure
     *
     * @param file The file to load from
     * @return The loaded FlowGraph
     * @throws DeserializationException if deserialization fails
     */
    fun load(file: File): FlowGraph {
        val result = deserializeFromFile(file)
        if (result.isSuccess && result.graph != null) {
            return result.graph
        } else {
            throw DeserializationException(
                result.errorMessage ?: "Unknown deserialization error",
                result.exception
            )
        }
    }

    /**
     * Loads a FlowGraph from a string, throwing exception on failure
     *
     * @param dslContent The DSL content to load from
     * @return The loaded FlowGraph
     * @throws DeserializationException if deserialization fails
     */
    fun load(dslContent: String): FlowGraph {
        val result = deserialize(dslContent)
        if (result.isSuccess && result.graph != null) {
            return result.graph
        } else {
            throw DeserializationException(
                result.errorMessage ?: "Unknown deserialization error",
                result.exception
            )
        }
    }

    /**
     * Attempts to deserialize and returns null on failure (safe operation)
     *
     * @param file The file to load from
     * @return The loaded FlowGraph or null if deserialization failed
     */
    fun tryLoad(file: File): FlowGraph? {
        return try {
            val result = deserializeFromFile(file)
            if (result.isSuccess) result.graph else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Attempts to deserialize and returns null on failure (safe operation)
     *
     * @param dslContent The DSL content to load from
     * @return The loaded FlowGraph or null if deserialization failed
     */
    fun tryLoad(dslContent: String): FlowGraph? {
        return try {
            val result = deserialize(dslContent)
            if (result.isSuccess) result.graph else null
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Result of deserialization operation
 *
 * @property isSuccess Whether deserialization was successful
 * @property graph The deserialized FlowGraph (null if failed)
 * @property errorMessage Error message if deserialization failed
 * @property exception Exception that caused the failure (if any)
 */
data class DeserializationResult(
    val isSuccess: Boolean,
    val graph: FlowGraph?,
    val errorMessage: String?,
    val exception: Exception?
) {
    companion object {
        /**
         * Creates a successful deserialization result
         */
        fun success(graph: FlowGraph): DeserializationResult {
            return DeserializationResult(
                isSuccess = true,
                graph = graph,
                errorMessage = null,
                exception = null
            )
        }

        /**
         * Creates a failed deserialization result
         */
        fun error(message: String, exception: Exception? = null): DeserializationResult {
            return DeserializationResult(
                isSuccess = false,
                graph = null,
                errorMessage = message,
                exception = exception
            )
        }
    }

    /**
     * Returns the graph or throws an exception if deserialization failed
     */
    fun getOrThrow(): FlowGraph {
        if (isSuccess && graph != null) {
            return graph
        } else {
            throw DeserializationException(
                errorMessage ?: "Deserialization failed",
                exception
            )
        }
    }

    /**
     * Returns the graph or null if deserialization failed
     */
    fun getOrNull(): FlowGraph? {
        return if (isSuccess) graph else null
    }

    /**
     * Returns the graph or a default value if deserialization failed
     */
    fun getOrDefault(default: FlowGraph): FlowGraph {
        return if (isSuccess && graph != null) graph else default
    }
}

/**
 * Exception thrown when deserialization fails
 */
class DeserializationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Loads a FlowGraph from a .flow.kts file (extension function on File)
 *
 * @return DeserializationResult with the graph or error information
 */
fun File.loadFlowGraph(): DeserializationResult {
    return FlowGraphDeserializer.deserializeFromFile(this)
}

/**
 * Parses a FlowGraph from a DSL string (extension function on String)
 *
 * @return DeserializationResult with the graph or error information
 */
fun String.parseFlowGraph(): DeserializationResult {
    return FlowGraphDeserializer.deserialize(this)
}
