/*
 * Kotlin Code Generator
 * Generates KMP code from FBP graphs
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import com.squareup.kotlinpoet.*
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.model.Connection

/**
 * Main code generator that orchestrates KMP code generation from FBP graphs.
 *
 * Uses KotlinPoet to generate type-safe, compilable Kotlin code for:
 * - Node components (via ComponentGenerator)
 * - Flow/channel connections (via FlowGenerator)
 * - Build scripts (via BuildScriptGenerator)
 *
 * Generated code is compatible with Android, iOS, Desktop (JVM), and Web targets.
 */
class KotlinCodeGenerator {

    private val componentGenerator = ComponentGenerator()
    private val flowGenerator = FlowGenerator()

    /**
     * Generates a complete KMP project from a FlowGraph
     *
     * @param flowGraph The flow graph to generate code from
     * @return GeneratedProject containing all generated files
     */
    fun generateProject(flowGraph: FlowGraph): GeneratedProject {
        val files = mutableListOf<FileSpec>()

        // Generate component classes for each CodeNode
        flowGraph.getAllCodeNodes().forEach { node ->
            files.add(componentGenerator.generateComponent(node))
        }

        // Generate flow orchestration
        files.add(flowGenerator.generateFlowOrchestrator(flowGraph))

        return GeneratedProject(
            name = flowGraph.name,
            version = flowGraph.version,
            files = files
        )
    }

    /**
     * Generates a single component class from a CodeNode
     *
     * @param node The code node to generate a component for
     * @return FileSpec containing the generated Kotlin file
     */
    fun generateNodeComponent(node: CodeNode): FileSpec {
        return componentGenerator.generateComponent(node)
    }

    /**
     * Generates flow orchestration code for connections
     *
     * @param flowGraph The flow graph with connections to generate
     * @return FileSpec containing the flow orchestrator
     */
    fun generateFlowCode(flowGraph: FlowGraph): FileSpec {
        return flowGenerator.generateFlowOrchestrator(flowGraph)
    }
}

/**
 * Represents a generated KMP project with all its files
 */
data class GeneratedProject(
    val name: String,
    val version: String,
    val files: List<FileSpec>
)

/**
 * Converts a string to PascalCase for class naming.
 * Handles snake_case, kebab-case, and lowercase inputs.
 *
 * Note: This preserves original behavior where delimiters are simply removed
 * rather than using them as word boundaries for capitalization.
 * e.g., "data_processor" -> "Dataprocessor" (not "DataProcessor")
 *
 * @return PascalCase version of the string
 */
fun String.pascalCase(): String {
    if (this.isBlank()) return "UnnamedComponent"

    // Capitalize first letter, remove delimiters and special characters
    return this
        .replaceFirstChar { it.uppercase() }
        .replace("_", "")
        .replace("-", "")
        .replace(Regex("[^a-zA-Z0-9]"), "") // Remove remaining special characters
        .ifBlank { "UnnamedComponent" }
}

/**
 * Converts a string to camelCase for variable/function naming.
 *
 * @return camelCase version of the string
 */
fun String.camelCase(): String {
    val pascal = this.pascalCase()
    return pascal.replaceFirstChar { it.lowercase() }
}

/**
 * Sanitizes a name to be a valid Kotlin identifier.
 *
 * @return Sanitized identifier string
 */
fun String.toKotlinIdentifier(): String {
    if (this.isBlank()) return "unnamed"

    // Remove invalid characters, ensure starts with letter
    val sanitized = this.replace(Regex("[^a-zA-Z0-9_]"), "")
    return if (sanitized.isEmpty() || !sanitized[0].isLetter()) {
        "v$sanitized"
    } else {
        sanitized
    }
}
