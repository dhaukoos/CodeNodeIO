/*
 * Node Template Interface
 * Base interface for code generation templates
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.templates

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType

/**
 * Interface for node-specific code generation templates.
 *
 * Templates provide specialized code generation for different node types,
 * producing optimized and idiomatic Kotlin code for each type of FBP node.
 */
interface NodeTemplate {
    /**
     * The node type this template handles.
     */
    val nodeType: CodeNodeType

    /**
     * Generates a TypeSpec for the given CodeNode.
     *
     * @param node The CodeNode to generate code for
     * @param className The class name to use for the generated class
     * @return TypeSpec containing the generated class definition
     */
    fun generate(node: CodeNode, className: ClassName): TypeSpec
}

/**
 * Registry for node templates.
 *
 * Provides lookup of templates by node type and manages the collection
 * of available templates for code generation.
 */
class NodeTemplateRegistry {

    private val templates = mutableMapOf<CodeNodeType, NodeTemplate>()

    init {
        // Register built-in templates
        register(TransformerTemplate())
        register(FilterTemplate())
        register(ValidatorTemplate())
        register(SplitterTemplate())
        register(MergerTemplate())
        register(GeneratorTemplate())
        register(SinkTemplate())
    }

    /**
     * Registers a template for a node type.
     *
     * @param template The template to register
     */
    fun register(template: NodeTemplate) {
        templates[template.nodeType] = template
    }

    /**
     * Gets a template for the given node type.
     *
     * @param nodeType The type of node to get a template for
     * @return The template if one exists, null otherwise
     */
    fun getTemplate(nodeType: CodeNodeType): NodeTemplate? {
        return templates[nodeType]
    }

    /**
     * Checks if a template exists for the given node type.
     *
     * @param nodeType The type to check
     * @return true if a template exists
     */
    fun hasTemplate(nodeType: CodeNodeType): Boolean {
        return templates.containsKey(nodeType)
    }

    /**
     * Gets all registered templates.
     *
     * @return Map of node types to their templates
     */
    fun getAllTemplates(): Map<CodeNodeType, NodeTemplate> {
        return templates.toMap()
    }
}
