/*
 * FlowGraphManager Service
 * Project-level service for managing flow graph lifecycle
 * License: Apache 2.0
 */

package io.codenode.ideplugin.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.grapheditor.serialization.FlowGraphDeserializer
import io.codenode.grapheditor.serialization.FlowGraphSerializer
import io.codenode.grapheditor.serialization.DeserializationResult
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Project-level service that manages flow graph lifecycle and persistence.
 *
 * Responsibilities:
 * - Loading flow graphs from .flow.kts files
 * - Saving flow graphs to files
 * - Tracking currently open/active graphs
 * - Providing access to graphs for other services and actions
 * - Caching loaded graphs for performance
 */
@Service(Service.Level.PROJECT)
class FlowGraphManager(private val project: Project) : Disposable {

    private val logger = Logger.getInstance(FlowGraphManager::class.java)

    /**
     * Cache of loaded flow graphs, keyed by file path.
     */
    private val graphCache = ConcurrentHashMap<String, CachedFlowGraph>()

    /**
     * Currently active/focused flow graph (from the editor).
     */
    @Volatile
    private var activeGraph: FlowGraph? = null

    /**
     * Currently active file path.
     */
    @Volatile
    private var activeFilePath: String? = null

    /**
     * Listeners for graph change events.
     */
    private val changeListeners = mutableListOf<FlowGraphChangeListener>()

    init {
        // Listen for file changes to invalidate cache
        project.messageBus.connect(this).subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    events.forEach { event ->
                        val path = event.path
                        if (path?.endsWith(".flow.kts") == true) {
                            invalidateCache(path)
                        }
                    }
                }
            }
        )
    }

    /**
     * Loads a flow graph from a VirtualFile.
     *
     * @param file The .flow.kts virtual file
     * @return The parsed FlowGraph
     * @throws FlowGraphParseException if the file is malformed
     */
    fun loadFlowGraph(file: VirtualFile): FlowGraph {
        val path = file.path
        logger.info("Loading flow graph from: $path")

        // Check cache first
        val cached = graphCache[path]
        if (cached != null && cached.modificationStamp == file.modificationStamp) {
            logger.debug("Returning cached flow graph for: $path")
            return cached.graph
        }

        // Load from file
        val content = String(file.contentsToByteArray(), Charsets.UTF_8)
        val result = FlowGraphDeserializer.deserialize(content)

        val graph = result.graph
        if (!result.isSuccess || graph == null) {
            val errorMessage = result.errorMessage ?: "Unknown parsing error"
            logger.error("Failed to parse flow graph: $errorMessage")
            throw FlowGraphParseException(errorMessage, file.path, result.exception)
        }

        // Cache the result
        graphCache[path] = CachedFlowGraph(
            graph = graph,
            modificationStamp = file.modificationStamp,
            filePath = path
        )

        logger.info("Successfully loaded flow graph: ${graph.name}")
        return graph
    }

    /**
     * Loads a flow graph from a file path string.
     *
     * @param filePath The path to the .flow.kts file
     * @return The parsed FlowGraph or null if loading fails
     */
    fun loadFlowGraph(filePath: String): FlowGraph? {
        val file = VirtualFileManager.getInstance().findFileByUrl("file://$filePath")
        return if (file != null) {
            try {
                loadFlowGraph(file)
            } catch (e: Exception) {
                logger.warn("Failed to load flow graph from $filePath: ${e.message}")
                null
            }
        } else {
            // Try loading from java.io.File
            val javaFile = File(filePath)
            if (javaFile.exists()) {
                val result = FlowGraphDeserializer.deserializeFromFile(javaFile)
                result.graph
            } else {
                null
            }
        }
    }

    /**
     * Saves a flow graph to a VirtualFile.
     *
     * @param flowGraph The flow graph to save
     * @param file The target .flow.kts file
     */
    fun saveFlowGraph(flowGraph: FlowGraph, file: VirtualFile) {
        val path = file.path
        logger.info("Saving flow graph to: $path")

        val content = FlowGraphSerializer.serialize(flowGraph)

        // Write to file
        file.getOutputStream(this).use { output ->
            output.write(content.toByteArray(Charsets.UTF_8))
        }

        // Update cache
        graphCache[path] = CachedFlowGraph(
            graph = flowGraph,
            modificationStamp = file.modificationStamp,
            filePath = path
        )

        // Notify listeners
        notifyGraphChanged(flowGraph, path)

        logger.info("Successfully saved flow graph: ${flowGraph.name}")
    }

    /**
     * Saves a flow graph to a file path.
     *
     * @param flowGraph The flow graph to save
     * @param filePath The target file path
     */
    fun saveFlowGraph(flowGraph: FlowGraph, filePath: String) {
        val content = FlowGraphSerializer.serialize(flowGraph)
        File(filePath).writeText(content, Charsets.UTF_8)

        // Update cache
        graphCache[filePath] = CachedFlowGraph(
            graph = flowGraph,
            modificationStamp = System.currentTimeMillis(),
            filePath = filePath
        )

        // Notify listeners
        notifyGraphChanged(flowGraph, filePath)
    }

    /**
     * Gets all flow graph files in the project.
     *
     * @return List of .flow.kts virtual files
     */
    fun getAllFlowGraphs(): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()
        val baseDir = project.baseDir ?: return result

        collectFlowGraphFiles(baseDir, result)
        return result
    }

    /**
     * Recursively collects .flow.kts files.
     */
    private fun collectFlowGraphFiles(dir: VirtualFile, result: MutableList<VirtualFile>) {
        for (child in dir.children) {
            if (child.isDirectory) {
                // Skip common non-source directories
                if (child.name !in listOf("build", "node_modules", ".git", ".idea", ".gradle")) {
                    collectFlowGraphFiles(child, result)
                }
            } else if (child.name.endsWith(".flow.kts")) {
                result.add(child)
            }
        }
    }

    /**
     * Sets the currently active flow graph (from the editor).
     *
     * @param graph The active flow graph
     * @param filePath The path to the active file
     */
    fun setActiveGraph(graph: FlowGraph?, filePath: String?) {
        this.activeGraph = graph
        this.activeFilePath = filePath

        if (graph != null && filePath != null) {
            logger.debug("Active graph set to: ${graph.name} ($filePath)")
        } else {
            logger.debug("Active graph cleared")
        }
    }

    /**
     * Gets the currently active flow graph.
     *
     * @return The active FlowGraph or null if none is active
     */
    fun getActiveGraph(): FlowGraph? = activeGraph

    /**
     * Gets the path to the currently active file.
     *
     * @return The active file path or null
     */
    fun getActiveFilePath(): String? = activeFilePath

    /**
     * Checks if a flow graph is loaded for the given file.
     *
     * @param filePath The file path to check
     * @return True if a graph is cached for this file
     */
    fun isLoaded(filePath: String): Boolean = graphCache.containsKey(filePath)

    /**
     * Gets a cached flow graph by file path.
     *
     * @param filePath The file path
     * @return The cached FlowGraph or null
     */
    fun getCached(filePath: String): FlowGraph? = graphCache[filePath]?.graph

    /**
     * Invalidates the cache for a specific file.
     *
     * @param filePath The file path to invalidate
     */
    fun invalidateCache(filePath: String) {
        graphCache.remove(filePath)
        logger.debug("Cache invalidated for: $filePath")
    }

    /**
     * Clears all cached flow graphs.
     */
    fun clearCache() {
        graphCache.clear()
        logger.debug("All cached flow graphs cleared")
    }

    /**
     * Adds a change listener.
     *
     * @param listener The listener to add
     */
    fun addChangeListener(listener: FlowGraphChangeListener) {
        changeListeners.add(listener)
    }

    /**
     * Removes a change listener.
     *
     * @param listener The listener to remove
     */
    fun removeChangeListener(listener: FlowGraphChangeListener) {
        changeListeners.remove(listener)
    }

    /**
     * Notifies listeners of a graph change.
     */
    private fun notifyGraphChanged(graph: FlowGraph, filePath: String) {
        changeListeners.forEach { listener ->
            try {
                listener.onGraphChanged(graph, filePath)
            } catch (e: Exception) {
                logger.warn("Error notifying graph change listener: ${e.message}")
            }
        }
    }

    /**
     * Tries to load a flow graph, returning a result object.
     *
     * @param file The file to load from
     * @return DeserializationResult with the graph or error
     */
    fun tryLoadFlowGraph(file: VirtualFile): DeserializationResult {
        return try {
            val graph = loadFlowGraph(file)
            DeserializationResult.success(graph)
        } catch (e: FlowGraphParseException) {
            DeserializationResult.error(e.message ?: "Parse error", e)
        } catch (e: Exception) {
            DeserializationResult.error("Failed to load: ${e.message}", e)
        }
    }

    override fun dispose() {
        graphCache.clear()
        changeListeners.clear()
        activeGraph = null
        activeFilePath = null
    }

    companion object {
        /**
         * Gets the FlowGraphManager instance for a project.
         */
        @JvmStatic
        fun getInstance(project: Project): FlowGraphManager {
            return project.getService(FlowGraphManager::class.java)
        }
    }
}

/**
 * Cached flow graph with metadata.
 */
private data class CachedFlowGraph(
    val graph: FlowGraph,
    val modificationStamp: Long,
    val filePath: String
)

/**
 * Listener for flow graph change events.
 */
interface FlowGraphChangeListener {
    /**
     * Called when a flow graph is saved or modified.
     *
     * @param graph The modified graph
     * @param filePath The file path
     */
    fun onGraphChanged(graph: FlowGraph, filePath: String)
}

/**
 * Exception thrown when flow graph parsing fails.
 */
class FlowGraphParseException(
    message: String,
    val filePath: String,
    cause: Throwable? = null
) : Exception("Failed to parse flow graph at $filePath: $message", cause)
