/*
 * FlowGraphDeserializer - Graph Deserialization from .flow.kts Files
 * Loads FlowGraph instances from Kotlin DSL script files using the Kotlin scripting engine
 * License: Apache 2.0
 */

package io.codenode.grapheditor.serialization

import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.model.ValidationResult
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
        return DeserializationResult.error(
            "Manual deserialization not yet implemented. " +
            "Kotlin scripting engine is required for .flow.kts file evaluation. " +
            "Ensure kotlin-scripting dependencies are available."
        )
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
