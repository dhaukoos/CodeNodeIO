/*
 * FlowGraphInspectCodeNode - Coarse-grained CodeNode wrapping node discovery and inspection
 * 2 inputs (filesystemPaths, classpathEntries), 1 output (nodeDescriptors)
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.nodes

import io.codenode.fbpdsl.model.*
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.fbpdsl.runtime.resolveSourceFilePath
import io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry
import io.codenode.flowgraphinspect.registry.NodeTemplateMeta
import java.io.File

/**
 * Coarse-grained CodeNode that wraps all node discovery and inspection functionality:
 * filesystem scanning for source templates, classpath scanning for compiled CodeNodes,
 * and merged node descriptor output for the palette.
 *
 * Ports:
 * - Input 1: filesystemPaths — directory paths to scan for .kt CodeNode source files
 * - Input 2: classpathEntries — package prefixes for compiled CodeNode discovery via ServiceLoader
 * - Output 1: nodeDescriptors — JSON array of discovered node metadata
 *
 * Uses anyInput mode: either input independently triggers processing with cached values.
 */
object FlowGraphInspectCodeNode : CodeNodeDefinition {
    override val name = "FlowGraphInspect"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Node discovery from filesystem and classpath"
    override val sourceFilePath: String? get() = resolveSourceFilePath(this::class.java)
    override val inputPorts = listOf(
        PortSpec("filesystemPaths", String::class),
        PortSpec("classpathEntries", String::class)
    )
    override val outputPorts = listOf(
        PortSpec("nodeDescriptors", String::class)
    )
    override val anyInput = true

    override fun createRuntime(name: String): NodeRuntime {
        val registry = NodeDefinitionRegistry()
        var cachedFilesystemPaths = ""
        var cachedClasspathEntries = ""

        return CodeNodeFactory.createIn2AnyOut1Processor<String, String, String>(
            name = name,
            initialValue1 = "",
            initialValue2 = ""
        ) { filesystemPaths, classpathEntries ->
            // Cache inputs
            if (filesystemPaths.isNotEmpty()) {
                cachedFilesystemPaths = filesystemPaths
            }
            if (classpathEntries.isNotEmpty()) {
                cachedClasspathEntries = classpathEntries
            }

            // Scan filesystem paths for .kt source files containing CodeNode definitions
            val effectiveFsPaths = if (filesystemPaths.isNotEmpty()) filesystemPaths else cachedFilesystemPaths
            if (effectiveFsPaths.isNotEmpty()) {
                effectiveFsPaths.split(File.pathSeparator).forEach { path ->
                    val dir = File(path.trim())
                    if (dir.isDirectory) {
                        registry.scanDirectory(dir)
                    }
                }
            }

            // Scan classpath for compiled CodeNode definitions via ServiceLoader
            val effectiveCpEntries = if (classpathEntries.isNotEmpty()) classpathEntries else cachedClasspathEntries
            if (effectiveCpEntries.isNotEmpty()) {
                // ServiceLoader discovery is triggered by discoverAll()
                registry.discoverAll()
            }

            // Build JSON output from all discovered nodes
            val allNodes = registry.getAllForPalette()
            buildNodeDescriptorsJson(allNodes)
        }
    }

    /**
     * Builds a JSON string containing all discovered node descriptors.
     */
    private fun buildNodeDescriptorsJson(nodes: List<NodeTypeDefinition>): String {
        return buildString {
            append("{\"nodes\":[")
            nodes.forEachIndexed { index, ntd ->
                if (index > 0) append(",")
                append("{\"name\":\"${ntd.name}\"")
                append(",\"category\":\"${ntd.category.name}\"")
                ntd.description?.let { desc ->
                    append(",\"description\":\"${desc.replace("\"", "\\\"")}\"")
                }
                val inputs = ntd.portTemplates.count { it.direction == Port.Direction.INPUT }
                val outputs = ntd.portTemplates.count { it.direction == Port.Direction.OUTPUT }
                append(",\"inputPorts\":$inputs")
                append(",\"outputPorts\":$outputs")
                append("}")
            }
            append("]}")
        }
    }
}
