/*
 * FlowGraph - Top-level Container for FBP Application
 * Represents a complete application feature or system
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * FlowGraph represents a top-level container for a complete Flow-based Programming application.
 * It serves as the root of the node hierarchy and contains all root-level nodes, connections,
 * and metadata for code generation.
 *
 * Based on FBP principles, FlowGraphs:
 * - Contain root-level nodes (CodeNodes and GraphNodes)
 * - Define connections between nodes
 * - Specify target platforms for code generation
 * - Maintain versioning and metadata
 * - Can be serialized to DSL files for persistence
 *
 * @property id Unique identifier for this flow graph
 * @property name Human-readable name (used for file naming and identification)
 * @property version Semantic version (MAJOR.MINOR.PATCH format)
 * @property description Optional documentation describing the graph's purpose
 * @property rootNodes List of top-level Node entities (CodeNodes or GraphNodes)
 * @property connections List of Connection entities linking nodes
 * @property metadata Project metadata (author, created date, tags, etc.)
 * @property targetPlatforms List of code generation targets
 *
 * @sample
 * ```kotlin
 * val userValidationGraph = FlowGraph(
 *     id = "graph_user_validation",
 *     name = "UserValidation",
 *     version = "1.0.0",
 *     description = "Validates user input and routes to appropriate handlers",
 *     rootNodes = listOf(inputNode, validatorNode, processorNode),
 *     connections = listOf(
 *         ConnectionFactory.create(inputNode.id, "output", validatorNode.id, "input")
 *     ),
 *     targetPlatforms = listOf(
 *         TargetPlatform.KMP_ANDROID,
 *         TargetPlatform.KMP_IOS,
 *         TargetPlatform.GO_SERVER
 *     )
 * )
 * ```
 */
@Serializable
data class FlowGraph(
    val id: String,
    val name: String,
    val version: String,
    val description: String? = null,
    @Transient val rootNodes: List<Node> = emptyList(),
    @Transient val connections: List<Connection> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
    val targetPlatforms: List<TargetPlatform> = emptyList()
) {
    /**
     * Target platforms for code generation
     */
    @Serializable
    enum class TargetPlatform {
        /** Kotlin Multiplatform - Android target */
        KMP_ANDROID,

        /** Kotlin Multiplatform - iOS target */
        KMP_IOS,

        /** Kotlin Multiplatform - Desktop/JVM target */
        KMP_DESKTOP,

        /** Kotlin Multiplatform - Web/JS target */
        KMP_WEB,

        /** Kotlin Multiplatform - Web/Wasm target */
        KMP_WASM,

        /** Go - Server/Backend target */
        GO_SERVER,

        /** Go - CLI application target */
        GO_CLI
    }

    init {
        require(id.isNotBlank()) { "FlowGraph ID cannot be blank" }
        require(name.isNotBlank()) { "FlowGraph name cannot be blank" }
        require(version.isNotBlank()) { "FlowGraph version cannot be blank" }
        require(isValidSemanticVersion(version)) {
            "FlowGraph version must be valid semantic version (MAJOR.MINOR.PATCH), got: $version"
        }
    }

    /**
     * Validates that this FlowGraph is well-formed
     *
     * @return Validation result with success flag and error messages
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate basic attributes
        if (id.isBlank()) {
            errors.add("FlowGraph ID cannot be blank")
        }
        if (name.isBlank()) {
            errors.add("FlowGraph name cannot be blank")
        }
        if (version.isBlank()) {
            errors.add("FlowGraph version cannot be blank")
        } else if (!isValidSemanticVersion(version)) {
            errors.add("FlowGraph version must be valid semantic version (MAJOR.MINOR.PATCH), got: $version")
        }

        // Validate that graph has at least one node (warning, not error)
        if (rootNodes.isEmpty()) {
            // This is acceptable for an empty/new graph
            // errors.add("FlowGraph should have at least one root node")
        }

        // Validate all root nodes
        rootNodes.forEach { node ->
            val nodeValidation = node.validate()
            if (!nodeValidation.success) {
                errors.add("Invalid root node '${node.name}': ${nodeValidation.errors.joinToString(", ")}")
            }

            // Verify root nodes have no parent
            if (node.parentNodeId != null) {
                errors.add("Root node '${node.name}' should not have a parent (parentNodeId should be null)")
            }
        }

        // Validate all connections
        connections.forEach { connection ->
            val connectionValidation = connection.validate()
            if (!connectionValidation.success) {
                errors.add("Invalid connection '${connection.id}': ${connectionValidation.errors.joinToString(", ")}")
            }

            // Verify connection nodes exist in the graph
            val sourceExists = findNode(connection.sourceNodeId) != null
            val targetExists = findNode(connection.targetNodeId) != null

            if (!sourceExists) {
                errors.add("Connection '${connection.id}' references non-existent source node '${connection.sourceNodeId}'")
            }
            if (!targetExists) {
                errors.add("Connection '${connection.id}' references non-existent target node '${connection.targetNodeId}'")
            }
        }

        // Validate no duplicate node IDs
        val nodeIds = getAllNodes().map { it.id }
        val duplicateIds = nodeIds.groupingBy { it }.eachCount().filter { it.value > 1 }
        if (duplicateIds.isNotEmpty()) {
            errors.add("Duplicate node IDs found: ${duplicateIds.keys.joinToString(", ")}")
        }

        // Validate target platforms specified
        if (targetPlatforms.isEmpty()) {
            // This is acceptable for design/development phase
            // errors.add("FlowGraph should specify at least one target platform")
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Checks if a version string is valid semantic version format
     *
     * @param version Version string to validate
     * @return true if valid semantic version (MAJOR.MINOR.PATCH)
     */
    private fun isValidSemanticVersion(version: String): Boolean {
        val semverPattern = Regex("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.-]+)?(\\+[a-zA-Z0-9.-]+)?$")
        return semverPattern.matches(version)
    }

    /**
     * Creates a copy of this FlowGraph with updated root nodes
     *
     * @param nodes The new list of root nodes
     * @return New FlowGraph instance with updated nodes
     */
    fun withNodes(nodes: List<Node>): FlowGraph {
        return copy(rootNodes = nodes)
    }

    /**
     * Creates a copy of this FlowGraph with an added root node
     *
     * @param node The node to add
     * @return New FlowGraph instance with added node
     */
    fun addNode(node: Node): FlowGraph {
        return copy(rootNodes = rootNodes + node)
    }

    /**
     * Creates a copy of this FlowGraph with a removed root node
     *
     * @param nodeId The ID of the node to remove
     * @return New FlowGraph instance with removed node
     */
    fun removeNode(nodeId: String): FlowGraph {
        return copy(rootNodes = rootNodes.filter { it.id != nodeId })
    }

    /**
     * Creates a copy of this FlowGraph with updated connections
     *
     * @param newConnections The new list of connections
     * @return New FlowGraph instance with updated connections
     */
    fun withConnections(newConnections: List<Connection>): FlowGraph {
        return copy(connections = newConnections)
    }

    /**
     * Creates a copy of this FlowGraph with an added connection
     *
     * @param connection The connection to add
     * @return New FlowGraph instance with added connection
     */
    fun addConnection(connection: Connection): FlowGraph {
        return copy(connections = connections + connection)
    }

    /**
     * Creates a copy of this FlowGraph with a removed connection
     *
     * @param connectionId The ID of the connection to remove
     * @return New FlowGraph instance with removed connection
     */
    fun removeConnection(connectionId: String): FlowGraph {
        return copy(connections = connections.filter { it.id != connectionId })
    }

    /**
     * Creates a copy of this FlowGraph with updated version
     *
     * @param newVersion The new semantic version
     * @return New FlowGraph instance with updated version
     */
    fun withVersion(newVersion: String): FlowGraph {
        require(isValidSemanticVersion(newVersion)) {
            "Version must be valid semantic version (MAJOR.MINOR.PATCH), got: $newVersion"
        }
        return copy(version = newVersion)
    }

    /**
     * Creates a copy of this FlowGraph with updated metadata
     *
     * @param newMetadata The new metadata map
     * @return New FlowGraph instance with updated metadata
     */
    fun withMetadata(newMetadata: Map<String, String>): FlowGraph {
        return copy(metadata = newMetadata)
    }

    /**
     * Creates a copy of this FlowGraph with added metadata entry
     *
     * @param key Metadata key
     * @param value Metadata value
     * @return New FlowGraph instance with added metadata
     */
    fun addMetadata(key: String, value: String): FlowGraph {
        return copy(metadata = metadata + (key to value))
    }

    /**
     * Creates a copy of this FlowGraph with updated target platforms
     *
     * @param platforms The new list of target platforms
     * @return New FlowGraph instance with updated platforms
     */
    fun withTargetPlatforms(platforms: List<TargetPlatform>): FlowGraph {
        return copy(targetPlatforms = platforms)
    }

    /**
     * Creates a copy of this FlowGraph with an added target platform
     *
     * @param platform The platform to add
     * @return New FlowGraph instance with added platform
     */
    fun addTargetPlatform(platform: TargetPlatform): FlowGraph {
        return copy(targetPlatforms = targetPlatforms + platform)
    }

    /**
     * Gets all nodes in the graph (root nodes and their descendants)
     *
     * @return Flattened list of all nodes
     */
    fun getAllNodes(): List<Node> {
        val allNodes = mutableListOf<Node>()
        rootNodes.forEach { node ->
            allNodes.add(node)
            if (node is GraphNode) {
                allNodes.addAll(node.getAllDescendants())
            }
        }
        return allNodes
    }

    /**
     * Gets all CodeNodes in the graph (terminal processing nodes)
     *
     * @return List of all CodeNodes
     */
    fun getAllCodeNodes(): List<CodeNode> {
        return getAllNodes().filterIsInstance<CodeNode>()
    }

    /**
     * Gets all GraphNodes in the graph (container nodes)
     *
     * @return List of all GraphNodes
     */
    fun getAllGraphNodes(): List<GraphNode> {
        return getAllNodes().filterIsInstance<GraphNode>()
    }

    /**
     * Finds a node by ID (searches root nodes and descendants)
     *
     * @param nodeId The ID of the node to find
     * @return The node if found, null otherwise
     */
    fun findNode(nodeId: String): Node? {
        return getAllNodes().find { it.id == nodeId }
    }

    /**
     * Finds a root node by ID
     *
     * @param nodeId The ID of the root node to find
     * @return The root node if found, null otherwise
     */
    fun findRootNode(nodeId: String): Node? {
        return rootNodes.find { it.id == nodeId }
    }

    /**
     * Finds a connection by ID
     *
     * @param connectionId The ID of the connection to find
     * @return The connection if found, null otherwise
     */
    fun findConnection(connectionId: String): Connection? {
        return connections.find { it.id == connectionId }
    }

    /**
     * Gets all connections involving a specific node (as source or target)
     *
     * @param nodeId The ID of the node
     * @return List of connections where the node is source or target
     */
    fun getConnectionsForNode(nodeId: String): List<Connection> {
        return connections.filter {
            it.sourceNodeId == nodeId || it.targetNodeId == nodeId
        }
    }

    /**
     * Gets the total number of nodes in the graph (including descendants)
     *
     * @return Total node count
     */
    fun getTotalNodeCount(): Int {
        return getAllNodes().size
    }

    /**
     * Gets the total number of connections in the graph
     *
     * @return Total connection count
     */
    fun getTotalConnectionCount(): Int {
        return connections.size
    }

    /**
     * Gets metadata value by key
     *
     * @param key Metadata key
     * @return Metadata value or null if not found
     */
    fun getMetadata(key: String): String? {
        return metadata[key]
    }

    /**
     * Checks if this graph targets a specific platform
     *
     * @param platform The target platform to check
     * @return true if platform is in targetPlatforms list
     */
    fun targetsPlatform(platform: TargetPlatform): Boolean {
        return targetPlatforms.contains(platform)
    }

    /**
     * Checks if this graph targets any KMP platform
     *
     * @return true if any KMP platform is targeted
     */
    fun targetsKMP(): Boolean {
        return targetPlatforms.any {
            it == TargetPlatform.KMP_ANDROID ||
            it == TargetPlatform.KMP_IOS ||
            it == TargetPlatform.KMP_DESKTOP ||
            it == TargetPlatform.KMP_WEB ||
            it == TargetPlatform.KMP_WASM
        }
    }

    /**
     * Checks if this graph targets any Go platform
     *
     * @return true if any Go platform is targeted
     */
    fun targetsGo(): Boolean {
        return targetPlatforms.any {
            it == TargetPlatform.GO_SERVER ||
            it == TargetPlatform.GO_CLI
        }
    }
}
