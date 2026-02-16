/*
 * FileCustomNodeRepository - File-based persistence for custom node types
 * License: Apache 2.0
 */

package io.codenode.grapheditor.repository

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * File-based implementation of CustomNodeRepository.
 * Persists custom node types to JSON file at ~/.codenode/custom-nodes.json
 *
 * @param filePath Path to the JSON storage file (defaults to ~/.codenode/custom-nodes.json)
 */
class FileCustomNodeRepository(
    private val filePath: String = "${System.getProperty("user.home")}/.codenode/custom-nodes.json"
) : CustomNodeRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val nodes = mutableListOf<CustomNodeDefinition>()

    override fun getAll(): List<CustomNodeDefinition> = nodes.toList()

    override fun add(node: CustomNodeDefinition) {
        nodes.add(node)
        save()
    }

    override fun load() {
        val file = File(filePath)
        if (file.exists()) {
            try {
                val content = file.readText()
                if (content.isNotBlank()) {
                    val loadedNodes: List<CustomNodeDefinition> = json.decodeFromString(content)
                    nodes.clear()
                    nodes.addAll(loadedNodes)
                }
            } catch (e: Exception) {
                println("Warning: Could not load custom nodes from $filePath: ${e.message}")
                // Keep nodes list empty on error - allows users to create new nodes
            }
        }
    }

    override fun save() {
        try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(nodes.toList()))
        } catch (e: Exception) {
            println("Error: Could not save custom nodes to $filePath: ${e.message}")
            throw e
        }
    }

    override fun remove(id: String): Boolean {
        val removed = nodes.removeAll { it.id == id }
        if (removed) {
            save()
        }
        return removed
    }
}
