/*
 * ConnectionSegment - Visual Portion of a Connection
 * Represents the path between two endpoints within a single scope context
 * Used for rendering connections that cross GraphNode boundaries
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.serialization.Serializable

/**
 * ConnectionSegment represents a visual portion of a Connection, specifically
 * the path between two endpoints within a single scope context.
 *
 * Connections crossing GraphNode boundaries are split into multiple segments:
 * - Each segment is visible only within its scope context
 * - Adjacent segments share a common PassThruPort endpoint
 * - Segments are ordered from overall source to overall target
 *
 * Segment Count Rules:
 * - CodeNode → CodeNode: 1 segment (direct, no boundary)
 * - CodeNode → PassThruPort → CodeNode: 2 segments (1 exterior, 1 interior)
 * - CodeNode → PassThruPort → PassThruPort → CodeNode: 3 segments (nested GraphNode)
 *
 * @property id Unique identifier for this segment
 * @property sourceNodeId Starting node of this segment
 * @property sourcePortId Starting port of this segment
 * @property targetNodeId Ending node of this segment
 * @property targetPortId Ending port of this segment
 * @property scopeNodeId Visibility scope: null = root level, otherwise = inside this GraphNode
 * @property parentConnectionId Reference to the parent Connection that owns this segment
 *
 * @sample
 * ```kotlin
 * // Exterior segment at root level (outside GraphNode)
 * val exteriorSegment = ConnectionSegment(
 *     id = "seg-1",
 *     sourceNodeId = "external-source",
 *     sourcePortId = "output",
 *     targetNodeId = "graphNode-1",  // PassThruPort on GraphNode boundary
 *     targetPortId = "passthru-in",
 *     scopeNodeId = null,  // Root level
 *     parentConnectionId = "conn-1"
 * )
 *
 * // Interior segment inside GraphNode
 * val interiorSegment = ConnectionSegment(
 *     id = "seg-2",
 *     sourceNodeId = "graphNode-1",  // PassThruPort on GraphNode boundary
 *     sourcePortId = "passthru-in",
 *     targetNodeId = "internal-processor",
 *     targetPortId = "input",
 *     scopeNodeId = "graphNode-1",  // Inside this GraphNode
 *     parentConnectionId = "conn-1"
 * )
 * ```
 */
@Serializable
data class ConnectionSegment(
    val id: String,
    val sourceNodeId: String,
    val sourcePortId: String,
    val targetNodeId: String,
    val targetPortId: String,
    val scopeNodeId: String?,
    val parentConnectionId: String
) {
    init {
        require(id.isNotBlank()) { "Segment ID cannot be blank" }
        require(sourceNodeId.isNotBlank()) { "Source node ID cannot be blank" }
        require(sourcePortId.isNotBlank()) { "Source port ID cannot be blank" }
        require(targetNodeId.isNotBlank()) { "Target node ID cannot be blank" }
        require(targetPortId.isNotBlank()) { "Target port ID cannot be blank" }
        require(parentConnectionId.isNotBlank()) { "Parent connection ID cannot be blank" }
    }

    /**
     * Checks if this segment is at root level (not inside any GraphNode)
     *
     * @return true if scopeNodeId is null
     */
    fun isRootLevel(): Boolean = scopeNodeId == null

    /**
     * Checks if this segment is inside a GraphNode
     *
     * @return true if scopeNodeId is not null
     */
    fun isInsideGraphNode(): Boolean = scopeNodeId != null

    /**
     * Checks if this segment is visible in the given context
     *
     * @param contextScopeId The current navigation context (null = root, or GraphNode ID)
     * @return true if this segment should be rendered in the given context
     */
    fun isVisibleInContext(contextScopeId: String?): Boolean {
        return scopeNodeId == contextScopeId
    }

    /**
     * Gets a human-readable description of this segment
     *
     * @return String describing the segment flow
     */
    fun getDescription(): String {
        val scope = scopeNodeId ?: "root"
        return "Segment[$id]: $sourceNodeId:$sourcePortId -> $targetNodeId:$targetPortId (scope: $scope)"
    }

    /**
     * Validates that this segment is well-formed
     *
     * @return ValidationResult with success flag and error messages
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        if (id.isBlank()) {
            errors.add("Segment ID cannot be blank")
        }
        if (sourceNodeId.isBlank()) {
            errors.add("Source node ID cannot be blank")
        }
        if (sourcePortId.isBlank()) {
            errors.add("Source port ID cannot be blank")
        }
        if (targetNodeId.isBlank()) {
            errors.add("Target node ID cannot be blank")
        }
        if (targetPortId.isBlank()) {
            errors.add("Target port ID cannot be blank")
        }
        if (parentConnectionId.isBlank()) {
            errors.add("Parent connection ID cannot be blank")
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }
}
