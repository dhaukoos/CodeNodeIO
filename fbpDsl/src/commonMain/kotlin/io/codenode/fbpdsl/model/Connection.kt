/*
 * Connection - Data Flow Link Between Ports
 * Represents edge in FBP graph, implemented as channels in Kotlin/Go
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Connection represents a data flow link between two ports in the FBP graph.
 * Connections are the edges that link nodes together, implemented as channels
 * in Kotlin (using coroutines) or Go (using goroutines).
 *
 * Based on FBP principles and the Single Responsibility Principle, connections:
 * - Link exactly one source OUTPUT port to one target INPUT port
 * - Carry InformationPackets from source to target without modification
 * - Can buffer packets based on channelCapacity
 * - Enforce type compatibility between connected ports
 *
 * Note: Data transformation should be handled by dedicated transformer nodes,
 * not by connections. This keeps the Connection class focused on its single
 * responsibility: linking ports and managing data flow.
 *
 * @property id Unique identifier for this connection
 * @property sourceNodeId Reference to the source Node
 * @property sourcePortId Reference to the source OUTPUT Port
 * @property targetNodeId Reference to the target Node
 * @property targetPortId Reference to the target INPUT Port
 * @property channelCapacity Buffer size for the channel (0 = unbuffered, default)
 * @property parentScopeId Optional reference to parent GraphNode or FlowGraph
 * @property ipTypeId Optional reference to InformationPacketType for typed connections
 *
 * @sample
 * ```kotlin
 * val connection = Connection(
 *     id = "conn_123",
 *     sourceNodeId = "node_1",
 *     sourcePortId = "port_out",
 *     targetNodeId = "node_2",
 *     targetPortId = "port_in",
 *     channelCapacity = 10  // Buffer up to 10 packets
 * )
 * ```
 */
@Serializable
data class Connection(
    val id: String,
    val sourceNodeId: String,
    val sourcePortId: String,
    val targetNodeId: String,
    val targetPortId: String,
    val channelCapacity: Int = 0,
    val parentScopeId: String? = null,
    val ipTypeId: String? = null
) {
    // ==================== Segment Support ====================

    /**
     * Cached segments list - computed on first access, invalidated when graph changes
     */
    @Transient
    private var _segments: List<ConnectionSegment>? = null

    /**
     * Ordered list of segments composing this connection.
     * Single-segment for direct connections (CodeNode to CodeNode).
     * Multi-segment for connections crossing GraphNode boundaries.
     *
     * Segments are computed lazily and cached for performance.
     * Call invalidateSegments() after graph structure changes.
     */
    val segments: List<ConnectionSegment>
        get() = _segments ?: computeSegments().also { _segments = it }

    /**
     * Invalidates the cached segments list.
     * Call this after any graph structure change that affects this connection
     * (e.g., grouping nodes, ungrouping, adding/removing PassThruPorts).
     */
    fun invalidateSegments() {
        _segments = null
    }

    /**
     * Computes the segment list for this connection.
     * For now, creates a single segment representing direct connections.
     * Multi-segment computation for boundary crossings will be enhanced
     * in User Story 2 (T028-T030).
     *
     * @return List of ConnectionSegment representing this connection's visual path
     */
    private fun computeSegments(): List<ConnectionSegment> {
        // Default: single segment for direct CodeNode-to-CodeNode connections
        // This will be enhanced to detect boundary crossings in later tasks
        return listOf(
            ConnectionSegment(
                id = "${id}_seg_0",
                sourceNodeId = sourceNodeId,
                sourcePortId = sourcePortId,
                targetNodeId = targetNodeId,
                targetPortId = targetPortId,
                scopeNodeId = parentScopeId,
                parentConnectionId = id
            )
        )
    }

    /**
     * Information about a boundary port for segment computation.
     * Used to determine where connections cross GraphNode boundaries.
     */
    data class BoundaryPortInfo(
        val direction: String,  // "INPUT" or "OUTPUT"
        val externalNodeId: String,
        val externalPortId: String,
        val internalNodeId: String,
        val internalPortId: String
    )

    /**
     * Computes the segment list for this connection with full graph context.
     * This method determines if the connection crosses GraphNode boundaries
     * and creates appropriate segments for each scope.
     *
     * @param graphContext Map of GraphNode ID to list of child node IDs
     * @param boundaryPorts Map of GraphNode ID to its boundary ports (PassThruPorts)
     * @return List of ConnectionSegment representing this connection's visual path
     */
    fun computeSegmentsWithContext(
        graphContext: Map<String, List<String>> = emptyMap(),
        boundaryPorts: Map<String, Map<String, BoundaryPortInfo>> = emptyMap()
    ): List<ConnectionSegment> {
        // If no graph context provided, return simple single segment
        if (graphContext.isEmpty() && boundaryPorts.isEmpty()) {
            return listOf(
                ConnectionSegment(
                    id = "${id}_seg_0",
                    sourceNodeId = sourceNodeId,
                    sourcePortId = sourcePortId,
                    targetNodeId = targetNodeId,
                    targetPortId = targetPortId,
                    scopeNodeId = parentScopeId,
                    parentConnectionId = id
                )
            )
        }

        // Find which GraphNode (if any) contains the source and target nodes
        val sourceContainer = findContainingGraphNode(sourceNodeId, graphContext)
        val targetContainer = findContainingGraphNode(targetNodeId, graphContext)

        // Case 1: Both nodes in same scope (or both at root level)
        if (sourceContainer == targetContainer) {
            return listOf(
                ConnectionSegment(
                    id = "${id}_seg_0",
                    sourceNodeId = sourceNodeId,
                    sourcePortId = sourcePortId,
                    targetNodeId = targetNodeId,
                    targetPortId = targetPortId,
                    scopeNodeId = sourceContainer,  // null for root, graphNodeId for internal
                    parentConnectionId = id
                )
            )
        }

        // Case 2: Connection crosses boundary - need to compute segments
        return computeBoundaryCrossingSegments(
            sourceContainer,
            targetContainer,
            graphContext,
            boundaryPorts
        )
    }

    /**
     * Finds which GraphNode directly contains the given node ID.
     * Returns null if the node is at root level.
     */
    private fun findContainingGraphNode(
        nodeId: String,
        graphContext: Map<String, List<String>>
    ): String? {
        for ((graphNodeId, childIds) in graphContext) {
            if (nodeId in childIds) {
                return graphNodeId
            }
        }
        return null
    }

    /**
     * Computes segments for a connection that crosses one or more GraphNode boundaries.
     */
    private fun computeBoundaryCrossingSegments(
        sourceContainer: String?,
        targetContainer: String?,
        graphContext: Map<String, List<String>>,
        boundaryPorts: Map<String, Map<String, BoundaryPortInfo>>
    ): List<ConnectionSegment> {
        val segments = mutableListOf<ConnectionSegment>()
        var segmentIndex = 0

        // Determine the path from source to target through boundary ports
        // Build the ancestry chain for both source and target
        val sourcePath = buildAncestryPath(sourceContainer, graphContext)
        val targetPath = buildAncestryPath(targetContainer, graphContext)

        // Find common ancestor (LCA)
        val commonAncestor = findCommonAncestor(sourcePath, targetPath)

        // Segments going OUT from source to common ancestor
        var currentNode = sourceNodeId
        var currentPort = sourcePortId
        var currentScope = sourceContainer

        // Go up from source container to common ancestor
        val sourceToCommon = getPathToAncestor(sourceContainer, commonAncestor, graphContext)
        for (graphNodeId in sourceToCommon) {
            val ports = boundaryPorts[graphNodeId] ?: continue
            val boundaryPort = ports.values.find {
                it.direction == "OUTPUT" &&
                    it.internalNodeId == currentNode &&
                    it.internalPortId == currentPort
            } ?: continue

            val portId = ports.entries.find { it.value == boundaryPort }?.key ?: continue

            // Add interior segment
            segments.add(
                ConnectionSegment(
                    id = "${id}_seg_${segmentIndex++}",
                    sourceNodeId = currentNode,
                    sourcePortId = currentPort,
                    targetNodeId = graphNodeId,
                    targetPortId = portId,
                    scopeNodeId = graphNodeId,
                    parentConnectionId = id
                )
            )

            currentNode = graphNodeId
            currentPort = portId
            currentScope = findContainingGraphNode(graphNodeId, graphContext)
        }

        // Add segment at common ancestor level (or root)
        val commonToTarget = getPathFromAncestor(commonAncestor, targetContainer, graphContext)

        if (commonToTarget.isNotEmpty()) {
            val nextGraphNode = commonToTarget.first()
            val ports = boundaryPorts[nextGraphNode] ?: emptyMap()
            val boundaryPort = ports.values.find {
                it.direction == "INPUT" &&
                    (it.externalNodeId == currentNode || it.externalPortId == currentPort)
            }

            if (boundaryPort != null) {
                val portId = ports.entries.find { it.value == boundaryPort }?.key ?: ""

                // Exterior segment to next GraphNode
                segments.add(
                    ConnectionSegment(
                        id = "${id}_seg_${segmentIndex++}",
                        sourceNodeId = currentNode,
                        sourcePortId = currentPort,
                        targetNodeId = nextGraphNode,
                        targetPortId = portId,
                        scopeNodeId = commonAncestor,
                        parentConnectionId = id
                    )
                )

                currentNode = nextGraphNode
                currentPort = portId
                currentScope = nextGraphNode
            }
        }

        // Go down from common ancestor to target container
        for (i in 1 until commonToTarget.size) {
            val graphNodeId = commonToTarget[i]
            val ports = boundaryPorts[graphNodeId] ?: continue
            val boundaryPort = ports.values.find {
                it.direction == "INPUT" &&
                    it.externalNodeId == currentNode
            } ?: continue

            val portId = ports.entries.find { it.value == boundaryPort }?.key ?: continue

            segments.add(
                ConnectionSegment(
                    id = "${id}_seg_${segmentIndex++}",
                    sourceNodeId = currentNode,
                    sourcePortId = currentPort,
                    targetNodeId = graphNodeId,
                    targetPortId = portId,
                    scopeNodeId = currentScope,
                    parentConnectionId = id
                )
            )

            currentNode = graphNodeId
            currentPort = portId
            currentScope = graphNodeId
        }

        // Add final segment to target
        if (currentNode != targetNodeId || currentPort != targetPortId) {
            segments.add(
                ConnectionSegment(
                    id = "${id}_seg_${segmentIndex++}",
                    sourceNodeId = currentNode,
                    sourcePortId = currentPort,
                    targetNodeId = targetNodeId,
                    targetPortId = targetPortId,
                    scopeNodeId = targetContainer,
                    parentConnectionId = id
                )
            )
        }

        return if (segments.isEmpty()) {
            // Fallback: single segment if no boundary crossings detected
            listOf(
                ConnectionSegment(
                    id = "${id}_seg_0",
                    sourceNodeId = sourceNodeId,
                    sourcePortId = sourcePortId,
                    targetNodeId = targetNodeId,
                    targetPortId = targetPortId,
                    scopeNodeId = parentScopeId,
                    parentConnectionId = id
                )
            )
        } else {
            segments
        }
    }

    /**
     * Builds ancestry path from a node up to root (list of containing GraphNode IDs).
     */
    private fun buildAncestryPath(
        containerId: String?,
        graphContext: Map<String, List<String>>
    ): List<String> {
        if (containerId == null) return emptyList()

        val path = mutableListOf(containerId)
        var current: String = containerId

        while (true) {
            val parent = findContainingGraphNode(current, graphContext)
            if (parent == null) break
            path.add(parent)
            current = parent
        }

        return path.reversed()  // From root to node
    }

    /**
     * Finds the lowest common ancestor of two paths.
     */
    private fun findCommonAncestor(
        path1: List<String>,
        path2: List<String>
    ): String? {
        var common: String? = null
        for (i in 0 until minOf(path1.size, path2.size)) {
            if (path1[i] == path2[i]) {
                common = path1[i]
            } else {
                break
            }
        }
        return common
    }

    /**
     * Gets path from a node up to an ancestor (exclusive of ancestor).
     */
    private fun getPathToAncestor(
        fromId: String?,
        ancestorId: String?,
        graphContext: Map<String, List<String>>
    ): List<String> {
        if (fromId == null || fromId == ancestorId) return emptyList()

        val path = mutableListOf<String>()
        var current = fromId

        while (current != null && current != ancestorId) {
            path.add(current)
            current = findContainingGraphNode(current, graphContext)
        }

        return path
    }

    /**
     * Gets path from an ancestor down to a node (inclusive of node, exclusive of ancestor).
     */
    private fun getPathFromAncestor(
        ancestorId: String?,
        toId: String?,
        graphContext: Map<String, List<String>>
    ): List<String> {
        if (toId == null) return emptyList()
        if (ancestorId == toId) return emptyList()

        val path = mutableListOf<String>()
        var current = toId

        while (current != null && current != ancestorId) {
            path.add(0, current)  // Prepend to get correct order
            current = findContainingGraphNode(current, graphContext)
        }

        return path
    }

    /**
     * Validates that segments form a continuous path from source to target.
     *
     * Checks:
     * 1. At least one segment exists
     * 2. First segment source matches connection source
     * 3. Last segment target matches connection target
     * 4. Each segment's target matches next segment's source
     *
     * @return ValidationResult with success flag and error messages
     */
    fun validateSegmentChain(): ValidationResult {
        val errors = mutableListOf<String>()
        val segs = segments

        // Check 1: At least one segment
        if (segs.isEmpty()) {
            errors.add("Connection must have at least one segment")
            return ValidationResult(success = false, errors = errors)
        }

        // Check 2: First segment source matches connection source
        val first = segs.first()
        if (first.sourceNodeId != sourceNodeId) {
            errors.add("First segment source node '${first.sourceNodeId}' must match connection source '$sourceNodeId'")
        }
        if (first.sourcePortId != sourcePortId) {
            errors.add("First segment source port '${first.sourcePortId}' must match connection source '$sourcePortId'")
        }

        // Check 3: Last segment target matches connection target
        val last = segs.last()
        if (last.targetNodeId != targetNodeId) {
            errors.add("Last segment target node '${last.targetNodeId}' must match connection target '$targetNodeId'")
        }
        if (last.targetPortId != targetPortId) {
            errors.add("Last segment target port '${last.targetPortId}' must match connection target '$targetPortId'")
        }

        // Check 4: Adjacent segments connect properly
        for (i in 0 until segs.size - 1) {
            val current = segs[i]
            val next = segs[i + 1]
            if (current.targetNodeId != next.sourceNodeId) {
                errors.add("Segment $i target node '${current.targetNodeId}' must match segment ${i + 1} source node '${next.sourceNodeId}'")
            }
            if (current.targetPortId != next.sourcePortId) {
                errors.add("Segment $i target port '${current.targetPortId}' must match segment ${i + 1} source port '${next.sourcePortId}'")
            }
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    init {
        require(id.isNotBlank()) { "Connection ID cannot be blank" }
        require(sourceNodeId.isNotBlank()) { "Source node ID cannot be blank" }
        require(sourcePortId.isNotBlank()) { "Source port ID cannot be blank" }
        require(targetNodeId.isNotBlank()) { "Target node ID cannot be blank" }
        require(targetPortId.isNotBlank()) { "Target port ID cannot be blank" }
        require(channelCapacity >= 0) { "Channel capacity cannot be negative, got $channelCapacity" }
    }

    /**
     * Validates that this connection is well-formed
     *
     * Note: This method performs basic structural validation.
     * Full validation (port direction, type compatibility, cycle detection)
     * requires access to the full graph context and should be performed
     * at the FlowGraph or GraphNode level.
     *
     * @return Validation result with success flag and error messages
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate basic attributes
        if (id.isBlank()) {
            errors.add("Connection ID cannot be blank")
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

        // Validate channel capacity
        if (channelCapacity < 0) {
            errors.add("Channel capacity cannot be negative, got $channelCapacity")
        }

        // Cannot connect a node to itself on the same port
        if (sourceNodeId == targetNodeId && sourcePortId == targetPortId) {
            errors.add("Cannot create self-loop connection on same port")
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Validates this connection with access to actual port objects
     *
     * This performs full validation including port direction and type compatibility.
     *
     * @param sourcePort The source port object
     * @param targetPort The target port object
     * @return Validation result with success flag and error messages
     */
    fun validateWithPorts(sourcePort: Port<*>, targetPort: Port<*>): ValidationResult {
        val errors = mutableListOf<String>()

        // First run basic validation
        val basicValidation = validate()
        errors.addAll(basicValidation.errors)

        // Verify port IDs match
        if (sourcePort.id != sourcePortId) {
            errors.add("Source port ID mismatch: expected '$sourcePortId', got '${sourcePort.id}'")
        }
        if (targetPort.id != targetPortId) {
            errors.add("Target port ID mismatch: expected '$targetPortId', got '${targetPort.id}'")
        }

        // Verify source port is OUTPUT
        if (sourcePort.direction != Port.Direction.OUTPUT) {
            errors.add("Source port '${sourcePort.name}' must be OUTPUT direction, got ${sourcePort.direction}")
        }

        // Verify target port is INPUT
        if (targetPort.direction != Port.Direction.INPUT) {
            errors.add("Target port '${targetPort.name}' must be INPUT direction, got ${targetPort.direction}")
        }

        // Verify type compatibility
        if (!sourcePort.isCompatibleWith(targetPort)) {
            errors.add("Incompatible port types: source port '${sourcePort.name}' type '${sourcePort.typeName}' cannot connect to target port '${targetPort.name}' type '${targetPort.typeName}'")
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Creates a copy of this connection with a new channel capacity
     *
     * @param capacity The new buffer size
     * @return New Connection instance with updated capacity
     */
    fun withChannelCapacity(capacity: Int): Connection {
        require(capacity >= 0) { "Channel capacity cannot be negative" }
        return copy(channelCapacity = capacity)
    }

    /**
     * Creates a copy of this connection with a new parent scope
     *
     * @param scopeId The new parent GraphNode or FlowGraph ID
     * @return New Connection instance with updated parent
     */
    fun withParentScope(scopeId: String?): Connection {
        return copy(parentScopeId = scopeId)
    }

    /**
     * Checks if this connection is buffered (capacity > 0)
     *
     * @return true if channelCapacity is greater than 0
     */
    fun isBuffered(): Boolean = channelCapacity > 0

    /**
     * Checks if this connection is unbuffered (capacity = 0)
     *
     * @return true if channelCapacity is 0
     */
    fun isUnbuffered(): Boolean = channelCapacity == 0

    /**
     * Checks if this connection links two different nodes
     *
     * @return true if source and target node IDs are different
     */
    fun connectsDifferentNodes(): Boolean = sourceNodeId != targetNodeId

    /**
     * Checks if this connection is a self-loop (connects a node to itself)
     *
     * @return true if source and target node IDs are the same
     */
    fun isSelfLoop(): Boolean = sourceNodeId == targetNodeId

    /**
     * Gets a human-readable description of this connection
     *
     * @return String describing the connection flow
     */
    fun getDescription(): String {
        return "$sourceNodeId:$sourcePortId -> $targetNodeId:$targetPortId"
    }

    /**
     * Checks if this connection belongs to a parent scope
     *
     * @return true if parentScopeId is not null
     */
    fun hasParentScope(): Boolean = parentScopeId != null

    /**
     * Checks if this connection is at root level (no parent scope)
     *
     * @return true if parentScopeId is null
     */
    fun isRootLevel(): Boolean = parentScopeId == null

    /**
     * Creates a copy of this connection with a new IP type
     *
     * @param typeId The InformationPacketType ID to assign (or null to clear)
     * @return New Connection instance with updated ipTypeId
     */
    fun withIPType(typeId: String?): Connection {
        return copy(ipTypeId = typeId)
    }

    /**
     * Checks if this connection has an assigned IP type
     *
     * @return true if ipTypeId is not null
     */
    fun hasIPType(): Boolean = ipTypeId != null

    /**
     * Checks if this connection is untyped (no IP type assigned)
     *
     * @return true if ipTypeId is null
     */
    fun isUntyped(): Boolean = ipTypeId == null
}
