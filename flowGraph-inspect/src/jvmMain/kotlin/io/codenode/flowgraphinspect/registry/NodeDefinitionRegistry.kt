/*
 * NodeDefinitionRegistry - Central registry for self-contained node definitions
 * Discovers nodes from classpath, filesystem, and legacy JSON sources
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.registry

import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.NodeTypeDefinition
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
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
    val category: CodeNodeType,
    val inputCount: Int,
    val outputCount: Int,
    val filePath: String
)

/**
 * Central registry that discovers and manages all available node definitions.
 *
 * Discovers nodes from two sources:
 * 1. **Compiled**: CodeNodeDefinition implementations on the classpath (Module + Project levels)
 * 2. **Templates**: .kt files in ~/.codenode/nodes/ (Universal level, metadata only)
 */
class NodeDefinitionRegistry {
    /** Nodes discovered from classpath (Module + Project levels) */
    private val compiledNodes = mutableMapOf<String, CodeNodeDefinition>()

    /** Nodes discovered from Universal level (metadata only) */
    private val templateNodes = mutableMapOf<String, NodeTemplateMeta>()

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
     * Returns a merged list from compiled and template sources, suitable for Node Palette display.
     *
     * Ordering: Compiled nodes first, then templates (marked as non-executable).
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
    }

    /**
     * Checks port signature compatibility between two nodes.
     * Returns null if compatible, or a warning message if port counts differ.
     *
     * @param existingName Name of the node being replaced
     * @param replacementName Name of the replacement node
     * @return Warning message if incompatible, null if compatible
     */
    fun checkPortCompatibility(existingName: String, replacementName: String): String? {
        val existing = getPortCounts(existingName) ?: return null
        val replacement = getPortCounts(replacementName) ?: return null

        val warnings = mutableListOf<String>()
        if (existing.first != replacement.first) {
            warnings.add("input count differs (${existing.first} vs ${replacement.first})")
        }
        if (existing.second != replacement.second) {
            warnings.add("output count differs (${existing.second} vs ${replacement.second})")
        }
        return if (warnings.isEmpty()) null
        else "Port mismatch: ${warnings.joinToString(", ")}"
    }

    /**
     * Gets the (inputCount, outputCount) for a node by name from any source.
     */
    private fun getPortCounts(name: String): Pair<Int, Int>? {
        compiledNodes[name]?.let {
            return Pair(it.inputPorts.size, it.outputPorts.size)
        }
        templateNodes[name]?.let {
            return Pair(it.inputCount, it.outputCount)
        }
        return null
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

    /**
     * Gets the source file path for a node by name, if available.
     * Checks compiled nodes first (via CodeNodeDefinition.sourceFilePath),
     * then falls back to template nodes (filesystem metadata).
     *
     * @param name The node name to look up
     * @return Absolute file path, or null if no source file is discoverable
     */
    fun getSourceFilePath(name: String): String? {
        // Check compiled nodes (self-declared source path)
        compiledNodes[name]?.sourceFilePath?.let { return it }
        // Fall back to template nodes (filesystem-discovered path)
        return templateNodes[name]?.filePath
    }

    /**
     * Registers a template node from its metadata directly.
     * Used for immediate palette registration after generating a new CodeNode file.
     *
     * @param template The template metadata to register
     */
    fun registerTemplate(template: NodeTemplateMeta) {
        templateNodes[template.name] = template
    }

    /**
     * Scans an additional directory for .kt template files and adds them to templateNodes.
     * Also attempts to load compiled CodeNodeDefinition objects via reflection if the
     * classes are on the classpath (e.g., when running from a project's runGraphEditor task).
     *
     * @param directory The directory to scan for .kt files containing CodeNodeDefinition objects
     */
    fun scanDirectory(directory: File) {
        if (!directory.isDirectory) return
        directory.listFiles(java.io.FileFilter { it.extension == "kt" })?.forEach { file ->
            val meta = parseTemplateMetadata(file)
            if (meta != null) {
                templateNodes[meta.name] = meta

                // Attempt to load the compiled CodeNodeDefinition via reflection
                if (!compiledNodes.containsKey(meta.name)) {
                    tryLoadCompiledNode(file)
                }
            }
        }
    }

    /**
     * Attempts to load a compiled CodeNodeDefinition from a source .kt file via reflection.
     * Extracts the package declaration and object name, then uses Class.forName() to load
     * the Kotlin object singleton. Only works if the class is on the classpath.
     */
    private fun tryLoadCompiledNode(file: File) {
        try {
            val content = file.readText()

            // Extract package declaration
            val packageMatch = Regex("^package\\s+([\\w.]+)", RegexOption.MULTILINE).find(content)
                ?: return
            val packageName = packageMatch.groupValues[1]

            // Extract object name (e.g., "object TimerEmitterCodeNode : CodeNodeDefinition")
            val objectMatch = Regex("object\\s+(\\w+)\\s*:\\s*CodeNodeDefinition").find(content)
                ?: return
            val objectName = objectMatch.groupValues[1]

            val fqcn = "$packageName.$objectName"
            val clazz = Class.forName(fqcn)

            // Kotlin objects have an INSTANCE field
            val instance = clazz.getField("INSTANCE").get(null)
            if (instance is CodeNodeDefinition) {
                compiledNodes[instance.name] = instance
            }
        } catch (_: ClassNotFoundException) {
            // Class not on classpath — expected for uncompiled templates
        } catch (_: NoSuchFieldException) {
            // Not a Kotlin object — skip
        } catch (_: Exception) {
            // Other reflection errors — skip silently
        }
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

        // Parse category from: override val category = CodeNodeType.TRANSFORMER
        val categoryMatch = Regex("""override\s+val\s+category\s*=\s*CodeNodeType\.(\w+)""").find(content)
        val category = categoryMatch?.groupValues?.get(1)?.let { categoryName ->
            try {
                CodeNodeType.valueOf(categoryName)
            } catch (e: IllegalArgumentException) {
                CodeNodeType.TRANSFORMER
            }
        } ?: CodeNodeType.TRANSFORMER

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
        return io.codenode.fbpdsl.factory.createGenericNodeType(
            numInputs = template.inputCount,
            numOutputs = template.outputCount,
            customName = template.name,
            customDescription = "${template.name} (template -- not compiled)"
        ).copy(category = template.category)
    }
}
