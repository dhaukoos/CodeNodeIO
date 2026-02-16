/*
 * CustomNodeRepository - Interface for managing custom node type persistence
 * License: Apache 2.0
 */

package io.codenode.grapheditor.repository

/**
 * Repository interface for managing persistence of user-created custom node types.
 */
interface CustomNodeRepository {
    /**
     * Returns all saved custom node definitions.
     */
    fun getAll(): List<CustomNodeDefinition>

    /**
     * Adds a new custom node definition and persists to storage.
     *
     * @param node The custom node definition to add
     */
    fun add(node: CustomNodeDefinition)

    /**
     * Loads custom nodes from storage file.
     * Called on application startup.
     * If file is missing or corrupted, initializes an empty list.
     */
    fun load()

    /**
     * Saves current custom nodes to storage file.
     * Called after each add() operation.
     */
    fun save()
}
