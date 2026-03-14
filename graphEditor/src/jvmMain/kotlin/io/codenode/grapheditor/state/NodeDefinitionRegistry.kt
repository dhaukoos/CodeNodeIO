/*
 * NodeDefinitionRegistry - Central registry for self-contained node definitions
 * Discovers nodes from classpath, filesystem, and legacy JSON sources
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import io.codenode.fbpdsl.model.NodeTypeDefinition
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeCategory
import io.codenode.grapheditor.repository.CustomNodeDefinition
import io.codenode.grapheditor.repository.CustomNodeRepository
import java.io.File
import java.util.ServiceLoader

/**
 * Metadata parsed from Universal-level node source files (not compiled).
 * These appear in the palette but cannot be executed until copied into a project.
 *
 * @property name Node name parsed from file
 * @property category Category parsed from file
 * @property inputCount Number of input ports
 * @property outputCount Number of output ports
 * @property filePath Absolute path to the source file
 */
data class NodeTemplateMeta(
    val name: String,
    val category: NodeCategory,
    val inputCount: Int,
    val outputCount: Int,
    val filePath: String
)

/**
 * Central registry that discovers and manages all available node definitions.
 *
 * Discovers nodes from three sources:
 * 1. **Compiled**: CodeNodeDefinition implementations on the classpath (Module + Project levels)
 * 2. **Templates**: .kt files in ~/.codenode/nodes/ (Universal level, metadata only)
 * 3. **Legacy**: CustomNodeDefinition entries from CustomNodeRepository (backward compatibility)
 *
 * @param customNodeRepository Optional legacy repository for backward compatibility
 */
class NodeDefinitionRegistry(
    private val customNodeRepository: CustomNodeRepository? = null
) {
    /** Nodes discovered from classpath (Module + Project levels) */
    private val compiledNodes = mutableMapOf<String, CodeNodeDefinition>()

    /** Nodes discovered from Universal level (metadata only) */
    private val templateNodes = mutableMapOf<String, NodeTemplateMeta>()

    /** Backward-compatible legacy custom nodes */
    private val legacyNodes = mutableListOf<CustomNodeDefinition>()

    /**
     * Scans all three levels for node definitions. Populates internal maps.
     *
     * Side effects:
     * - Classpath scan populates compiledNodes
     * - Filesystem scan populates templateNodes
     * - JSON load populates legacyNodes
     */
    fun discoverAll() {
        discoverCompiledNodes()
        discoverTemplateNodes()
        discoverLegacyNodes()
    }

    /**
     * Looks up a compiled node definition by name.
     *
     * @param name The node name to look up
     * @return The compiled node definition, or null if not found
     */
    fun getByName(name: String): CodeNodeDefinition? {
        return compiledNodes[name]
    }

    /**
     * Returns a merged list from all three sources, suitable for Node Palette display.
     *
     * Ordering: Compiled nodes first, then templates (marked as non-executable),
     * then legacy custom nodes.
     *
     * Compiled nodes that overlap with legacy entries (by name) will replace
     * the legacy entry to avoid duplicates.
     *
     * @return List of NodeTypeDefinitions for palette display
     */
    fun getAllForPalette(): List<NodeTypeDefinition> {
        val result = mutableListOf<NodeTypeDefinition>()

        // Compiled nodes first
        compiledNodes.values.forEach { node ->
            result.add(node.toNodeTypeDefinition())
        }

        // Template nodes (Universal level) -- palette display only
        templateNodes.values.forEach { template ->
            result.add(templateToNodeTypeDefinition(template))
        }

        // Legacy nodes -- exclude any that have been superseded by compiled nodes
        val compiledNames = compiledNodes.keys
        legacyNodes
            .filter { it.name !in compiledNames }
            .forEach { legacy ->
                result.add(legacy.toNodeTypeDefinition())
            }

        return result
    }

    /**
     * Checks whether a node with the given name is compiled (on the classpath).
     *
     * @param name The node name to check
     * @return true if the node is compiled and executable
     */
    fun isCompiled(name: String): Boolean {
        return compiledNodes.containsKey(name)
    }

    /**
     * Checks whether a node with the given name exists in any source.
     *
     * @param name The node name to check
     * @return true if the name is taken
     */
    fun nameExists(name: String): Boolean {
        return compiledNodes.containsKey(name)
                || templateNodes.containsKey(name)
                || legacyNodes.any { it.name == name }
    }

    /**
     * Registers a compiled CodeNodeDefinition directly.
     * Used for programmatic registration (e.g., from module-level nodes).
     *
     * @param node The node definition to register
     */
    fun register(node: CodeNodeDefinition) {
        compiledNodes[node.name] = node
    }

    // ========== Discovery Methods ==========

    /**
     * Discovers compiled CodeNodeDefinition implementations from the classpath.
     * Uses ServiceLoader for standard JVM service discovery.
     */
    private fun discoverCompiledNodes() {
        compiledNodes.clear()
        try {
            val loader = ServiceLoader.load(CodeNodeDefinition::class.java)
            for (node in loader) {
                compiledNodes[node.name] = node
            }
        } catch (e: Exception) {
            // ServiceLoader may fail if no META-INF/services file exists -- that's OK
        }
    }

    /**
     * Discovers Universal-level node templates from the user's .codenode/nodes/ directory.
     * Parses file metadata (name, category, ports) without compiling.
     */
    private fun discoverTemplateNodes() {
        templateNodes.clear()
        val universalDir = File(System.getProperty("user.home"), ".codenode/nodes")
        if (!universalDir.isDirectory) return

        universalDir.listFiles(java.io.FileFilter { it.extension == "kt" })?.forEach { file ->
            val meta = parseTemplateMetadata(file)
            if (meta != null) {
                templateNodes[meta.name] = meta
            }
        }
    }

    /**
     * Loads legacy custom node definitions from the CustomNodeRepository.
     */
    private fun discoverLegacyNodes() {
        legacyNodes.clear()
        val repo = customNodeRepository ?: return
        legacyNodes.addAll(repo.getAll())
    }

    // ========== Parsing Helpers ==========

    /**
     * Parses metadata from a Universal-level .kt node source file.
     * Looks for CodeNodeDefinition property patterns in the source text.
     */
    private fun parseTemplateMetadata(file: File): NodeTemplateMeta? {
        val content = try {
            file.readText()
        } catch (e: Exception) {
            return null
        }

        // Parse name from: override val name = "NodeName"
        val nameMatch = Regex("override\\s+val\\s+name\\s*=\\s*\"([^\"]+)\"").find(content)
            ?: return null

        // Parse category from: override val category = NodeCategory.TRANSFORMER
        val categoryMatch = Regex("""override\s+val\s+category\s*=\s*NodeCategory\.(\w+)""").find(content)
        val category = categoryMatch?.groupValues?.get(1)?.let { categoryName ->
            try {
                NodeCategory.valueOf(categoryName)
            } catch (e: IllegalArgumentException) {
                NodeCategory.PROCESSOR
            }
        } ?: NodeCategory.PROCESSOR

        // Count ports from listOf(...) patterns
        val inputPortsMatch = Regex("""override\s+val\s+inputPorts\s*=\s*listOf\(([^)]*)\)""").find(content)
        val outputPortsMatch = Regex("""override\s+val\s+outputPorts\s*=\s*listOf\(([^)]*)\)""").find(content)

        val inputCount = countPortSpecs(inputPortsMatch?.groupValues?.get(1))
        val outputCount = countPortSpecs(outputPortsMatch?.groupValues?.get(1))

        return NodeTemplateMeta(
            name = nameMatch.groupValues[1],
            category = category,
            inputCount = inputCount,
            outputCount = outputCount,
            filePath = file.absolutePath
        )
    }

    /**
     * Counts PortSpec entries in a listOf(...) content string.
     */
    private fun countPortSpecs(listContent: String?): Int {
        if (listContent == null || listContent.isBlank()) return 0
        return Regex("""PortSpec\(""").findAll(listContent).count()
    }

    /**
     * Converts a NodeTemplateMeta to a NodeTypeDefinition for palette display.
     */
    private fun templateToNodeTypeDefinition(template: NodeTemplateMeta): NodeTypeDefinition {
        val category = when (template.category) {
            NodeCategory.SOURCE -> NodeTypeDefinition.NodeCategory.UI_COMPONENT
            NodeCategory.TRANSFORMER -> NodeTypeDefinition.NodeCategory.TRANSFORMER
            NodeCategory.PROCESSOR -> NodeTypeDefinition.NodeCategory.TRANSFORMER
            NodeCategory.SINK -> NodeTypeDefinition.NodeCategory.UI_COMPONENT
        }

        return io.codenode.fbpdsl.factory.createGenericNodeType(
            numInputs = template.inputCount,
            numOutputs = template.outputCount,
            customName = template.name,
            customDescription = "${template.name} (template -- not compiled)"
        ).copy(category = category)
    }
}
