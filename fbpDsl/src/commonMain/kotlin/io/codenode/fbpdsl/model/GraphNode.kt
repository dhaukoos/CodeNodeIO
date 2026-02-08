/*
 * GraphNode - Hierarchical Container Node for FBP Graph
 * Represents organizational container functioning as "virtual circuit board"
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * GraphNode represents a hierarchical container node in the FBP graph that groups related logic flows.
 * GraphNodes function as "virtual circuit boards" - they organize and contain child nodes but do not
 * execute their own business logic.
 *
 * Based on FBP principles, GraphNodes:
 * - Contain child nodes (CodeNodes or nested GraphNodes)
 * - Have no controlling coroutine (no execution)
 * - Expose ports that map to child node ports
 * - Propagate control state (pause/resume/speed) to all descendants
 * - Enable hierarchical composition and reuse
 *
 * @property id Unique identifier for this node
 * @property name Human-readable name
 * @property description Optional documentation
 * @property position Visual canvas position
 * @property inputPorts List of INPUT ports (references to child ports)
 * @property outputPorts List of OUTPUT ports (references to child ports)
 * @property configuration Key-value property map for node settings
 * @property parentNodeId Optional reference to parent GraphNode
 * @property childNodes List of child Node entities (CodeNodes or nested GraphNodes)
 * @property internalConnections List of Connection entities between child nodes
 * @property portMappings Map of GraphNode port ID to child node port reference
 *
 * @sample
 * ```kotlin
 * val userProcessingGraph = GraphNode(
 *     id = "graph_user_processing",
 *     name = "UserProcessingPipeline",
 *     description = "Validates, transforms, and enriches user data",
 *     position = Node.Position(0.0, 0.0),
 *     inputPorts = listOf(
 *         PortFactory.input<RawUserData>("rawInput", "graph_user_processing")
 *     ),
 *     outputPorts = listOf(
 *         PortFactory.output<ProcessedUser>("processedOutput", "graph_user_processing")
 *     ),
 *     childNodes = listOf(validatorNode, transformerNode, enricherNode),
 *     internalConnections = listOf(
 *         // Connections between child nodes
 *     ),
 *     portMappings = mapOf(
 *         "rawInput" to PortMapping("validator_node", "input"),
 *         "processedOutput" to PortMapping("enricher_node", "output")
 *     )
 * )
 * ```
 */
@Serializable
data class GraphNode(
    override val id: String,
    override val name: String,
    override val description: String? = null,
    override val position: Position,
    @Transient override val inputPorts: List<Port<*>> = emptyList(),
    @Transient override val outputPorts: List<Port<*>> = emptyList(),
    override val configuration: Map<String, String> = emptyMap(),
    override val parentNodeId: String? = null,
    @Transient val childNodes: List<Node> = emptyList(),
    @Transient val internalConnections: List<Connection> = emptyList(),
    val portMappings: Map<String, PortMapping> = emptyMap(),
    override val executionState: ExecutionState = ExecutionState.IDLE,
    override val controlConfig: ControlConfig = ControlConfig()
) : Node() {

    /**
     * Returns the string representation of the node type
     */
    override val nodeType: String
        get() = "Graph Node"

    /**
     * Mapping from a GraphNode port to a child node port
     *
     * @property childNodeId The ID of the child node
     * @property childPortName The name of the port on the child node
     */
    @Serializable
    data class PortMapping(
        val childNodeId: String,
        val childPortName: String
    )

    /**
     * Validates that this GraphNode is well-formed
     *
     * @return Validation result with success flag and error messages
     */
    override fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        // First run base Node validation
        val baseValidation = super.validate()
        errors.addAll(baseValidation.errors)

        // GraphNode-specific validation: must have at least one child
        if (childNodes.isEmpty()) {
            errors.add("GraphNode '$name' must have at least one child node")
        }

        // Validate all child nodes
        childNodes.forEach { child ->
            val childValidation = child.validate()
            if (!childValidation.success) {
                errors.add("Invalid child node '${child.name}': ${childValidation.errors.joinToString(", ")}")
            }

            // Verify child has correct parent reference
            if (child.parentNodeId != id) {
                errors.add("Child node '${child.name}' has incorrect parentNodeId: expected '$id', got '${child.parentNodeId}'")
            }
        }

        // Validate no circular parent-child relationships
        // Check 1: This node's parentNodeId should not be this node itself
        if (parentNodeId == id) {
            errors.add("Circular parent-child relationship detected: GraphNode '$name' has itself as parent")
        }

        // Check 2: This node should not appear anywhere in its own descendant tree
        // This catches cases where editing/construction creates a containment loop
        val descendantIds = getAllDescendantIds()
        if (descendantIds.contains(id)) {
            errors.add("Circular containment detected: GraphNode '$name' appears in its own descendant tree")
        }

        // Check 3: No child should have a parentNodeId pointing to a sibling or descendant
        // (children should only point to this node as parent)
        childNodes.forEach { child ->
            if (child.parentNodeId != null && child.parentNodeId != id) {
                // If it points to a descendant, that's a circular reference
                if (descendantIds.contains(child.parentNodeId)) {
                    errors.add("Circular reference: Child '${child.name}' has parentNodeId pointing to descendant '${child.parentNodeId}'")
                }
            }
        }

        // Validate port mappings reference actual child ports
        portMappings.forEach { (portName, mapping) ->
            val childNode = childNodes.find { it.id == mapping.childNodeId }
            if (childNode == null) {
                errors.add("Port mapping '$portName' references non-existent child node '${mapping.childNodeId}'")
            } else {
                val childPort = childNode.findPort(mapping.childPortName)
                if (childPort == null) {
                    errors.add("Port mapping '$portName' references non-existent port '${mapping.childPortName}' on child node '${mapping.childNodeId}'")
                }
            }
        }

        // Validate all GraphNode ports have mappings
        getAllPorts().forEach { port ->
            if (!portMappings.containsKey(port.name)) {
                errors.add("GraphNode port '${port.name}' has no mapping to child node port")
            }
        }

        // Validate internal connections only link child nodes
        internalConnections.forEach { connection ->
            val sourceIsChild = childNodes.any { it.id == connection.sourceNodeId }
            val targetIsChild = childNodes.any { it.id == connection.targetNodeId }

            if (!sourceIsChild) {
                errors.add("Internal connection '${connection.id}' has source node '${connection.sourceNodeId}' that is not a child of this GraphNode")
            }
            if (!targetIsChild) {
                errors.add("Internal connection '${connection.id}' has target node '${connection.targetNodeId}' that is not a child of this GraphNode")
            }
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Creates a copy of this GraphNode at a new position
     *
     * @param newPosition The new position on the canvas
     * @return New GraphNode instance at the new position
     */
    override fun withPosition(newPosition: Position): GraphNode {
        return copy(position = newPosition)
    }

    /**
     * Creates a copy of this GraphNode with a new parent
     *
     * @param newParentId The new parent GraphNode ID
     * @return New GraphNode instance with updated parent
     */
    override fun withParent(newParentId: String?): GraphNode {
        return copy(parentNodeId = newParentId)
    }

    /**
     * Creates a copy of this GraphNode with a new execution state.
     *
     * When propagate=true (default), the state change propagates to all
     * descendant nodes that don't have independentControl enabled.
     *
     * @param newState The new execution state
     * @param propagate If true, propagate state to children (default: true)
     * @return New GraphNode instance with updated execution state
     */
    override fun withExecutionState(newState: ExecutionState, propagate: Boolean): GraphNode {
        return if (propagate) {
            // Propagate state to all children (respecting independentControl)
            val updatedChildren = propagateStateToChildren(newState)
            copy(executionState = newState, childNodes = updatedChildren)
        } else {
            // Only update this node's state
            copy(executionState = newState)
        }
    }

    /**
     * Propagates execution state to all child nodes.
     *
     * For each child:
     * - If child has independentControl=true, it is unchanged
     * - If child is a CodeNode, creates copy with new state
     * - If child is a GraphNode, recursively calls withExecutionState(state, propagate=true)
     *
     * @param newState The execution state to propagate
     * @return List of updated child nodes
     */
    fun propagateStateToChildren(newState: ExecutionState): List<Node> {
        return childNodes.map { child ->
            if (child.controlConfig.independentControl) {
                // Independent nodes are not affected by parent state changes
                child
            } else {
                // Propagate state to this child (and its descendants if it's a GraphNode)
                child.withExecutionState(newState, propagate = true)
            }
        }
    }

    /**
     * Creates a copy of this GraphNode with a new control configuration.
     *
     * When propagate=true (default), the config change propagates to all
     * descendant nodes that don't have independentControl enabled.
     * Note: The independentControl flag itself is never propagated.
     *
     * @param newConfig The new control configuration
     * @param propagate If true, propagate config to children (default: true)
     * @return New GraphNode instance with updated control configuration
     */
    override fun withControlConfig(newConfig: ControlConfig, propagate: Boolean): GraphNode {
        return if (propagate) {
            // Propagate config to all children (respecting independentControl)
            val updatedChildren = propagateConfigToChildren(newConfig)
            copy(controlConfig = newConfig, childNodes = updatedChildren)
        } else {
            // Only update this node's config
            copy(controlConfig = newConfig)
        }
    }

    /**
     * Gets the effective control configuration for this GraphNode.
     *
     * For GraphNode, this returns the node's own controlConfig.
     * GraphNode is authoritative for its subtree - its config defines
     * the effective settings for all descendants (unless they have
     * independentControl enabled).
     *
     * @return The effective ControlConfig for this node
     */
    override fun getEffectiveControlConfig(): ControlConfig {
        return controlConfig
    }

    /**
     * Propagates control configuration to all child nodes.
     *
     * For each child:
     * - If child has independentControl=true, it is unchanged (retains its entire config)
     * - Otherwise, child receives the new config BUT keeps its own independentControl flag
     *
     * Note: The independentControl flag is never propagated - it's a local setting.
     *
     * @param newConfig The control configuration to propagate
     * @return List of updated child nodes
     */
    fun propagateConfigToChildren(newConfig: ControlConfig): List<Node> {
        return childNodes.map { child ->
            if (child.controlConfig.independentControl) {
                // Independent nodes retain their entire config
                child
            } else {
                // Propagate config but preserve child's own independentControl flag
                val childConfig = newConfig.copy(independentControl = child.controlConfig.independentControl)
                child.withControlConfig(childConfig, propagate = true)
            }
        }
    }

    /**
     * Creates a copy of this GraphNode with updated child nodes
     *
     * @param children The new list of child nodes
     * @return New GraphNode instance with updated children
     */
    fun withChildren(children: List<Node>): GraphNode {
        return copy(childNodes = children)
    }

    /**
     * Creates a copy of this GraphNode with an added child node
     *
     * @param child The child node to add
     * @return New GraphNode instance with added child
     */
    fun addChild(child: Node): GraphNode {
        return copy(childNodes = childNodes + child)
    }

    /**
     * Creates a copy of this GraphNode with a removed child node
     *
     * @param childId The ID of the child node to remove
     * @return New GraphNode instance with removed child
     */
    fun removeChild(childId: String): GraphNode {
        return copy(childNodes = childNodes.filter { it.id != childId })
    }

    /**
     * Creates a copy of this GraphNode with updated internal connections
     *
     * @param connections The new list of internal connections
     * @return New GraphNode instance with updated connections
     */
    fun withConnections(connections: List<Connection>): GraphNode {
        return copy(internalConnections = connections)
    }

    /**
     * Creates a copy of this GraphNode with an added internal connection
     *
     * @param connection The connection to add
     * @return New GraphNode instance with added connection
     */
    fun addConnection(connection: Connection): GraphNode {
        return copy(internalConnections = internalConnections + connection)
    }

    /**
     * Creates a copy of this GraphNode with a removed internal connection
     *
     * @param connectionId The ID of the connection to remove
     * @return New GraphNode instance with removed connection
     */
    fun removeConnection(connectionId: String): GraphNode {
        return copy(internalConnections = internalConnections.filter { it.id != connectionId })
    }

    /**
     * Creates a copy of this GraphNode with updated port mappings
     *
     * @param mappings The new port mappings
     * @return New GraphNode instance with updated mappings
     */
    fun withPortMappings(mappings: Map<String, PortMapping>): GraphNode {
        return copy(portMappings = mappings)
    }

    /**
     * Creates a copy of this GraphNode with an added port mapping
     *
     * @param portName The GraphNode port name
     * @param mapping The mapping to a child node port
     * @return New GraphNode instance with added mapping
     */
    fun addPortMapping(portName: String, mapping: PortMapping): GraphNode {
        return copy(portMappings = portMappings + (portName to mapping))
    }

    /**
     * Creates a copy of this GraphNode with a removed port mapping
     *
     * @param portName The GraphNode port name to remove mapping for
     * @return New GraphNode instance with removed mapping
     */
    fun removePortMapping(portName: String): GraphNode {
        return copy(portMappings = portMappings - portName)
    }

    /**
     * Gets all child nodes recursively (including nested GraphNode children)
     *
     * @return Flattened list of all descendant nodes
     */
    fun getAllDescendants(): List<Node> {
        val descendants = mutableListOf<Node>()
        childNodes.forEach { child ->
            descendants.add(child)
            if (child is GraphNode) {
                descendants.addAll(child.getAllDescendants())
            }
        }
        return descendants
    }

    /**
     * Gets all CodeNode descendants (terminal nodes only)
     *
     * @return List of all descendant CodeNodes
     */
    fun getAllCodeNodes(): List<CodeNode> {
        return getAllDescendants().filterIsInstance<CodeNode>()
    }

    /**
     * Gets all GraphNode descendants (container nodes only)
     *
     * @return List of all descendant GraphNodes
     */
    fun getAllGraphNodes(): List<GraphNode> {
        return getAllDescendants().filterIsInstance<GraphNode>()
    }

    /**
     * Gets all descendant node IDs (for circular reference detection)
     *
     * @return Set of all descendant node IDs
     */
    fun getAllDescendantIds(): Set<String> {
        return getAllDescendants().map { it.id }.toSet()
    }

    /**
     * Finds a child node by ID
     *
     * @param childId The ID of the child node to find
     * @return The child node if found, null otherwise
     */
    fun findChild(childId: String): Node? {
        return childNodes.find { it.id == childId }
    }

    /**
     * Finds a descendant node by ID (searches recursively)
     *
     * @param nodeId The ID of the node to find
     * @return The node if found, null otherwise
     */
    fun findDescendant(nodeId: String): Node? {
        return getAllDescendants().find { it.id == nodeId }
    }

    /**
     * Finds an internal connection by ID
     *
     * @param connectionId The ID of the connection to find
     * @return The connection if found, null otherwise
     */
    fun findConnection(connectionId: String): Connection? {
        return internalConnections.find { it.id == connectionId }
    }

    /**
     * Gets the port mapping for a GraphNode port
     *
     * @param portName The name of the GraphNode port
     * @return The port mapping if exists, null otherwise
     */
    fun getPortMapping(portName: String): PortMapping? {
        return portMappings[portName]
    }

    /**
     * Checks if this GraphNode has any children
     *
     * @return true if childNodes is not empty
     */
    fun hasChildren(): Boolean = childNodes.isNotEmpty()

    /**
     * Checks if this GraphNode contains a specific child node
     *
     * @param childId The ID of the child node to check
     * @return true if the child exists
     */
    fun containsChild(childId: String): Boolean {
        return childNodes.any { it.id == childId }
    }

    /**
     * Gets the depth of nesting (0 for root-level, 1 for one level deep, etc.)
     *
     * @return Maximum nesting depth of this GraphNode
     */
    fun getMaxDepth(): Int {
        if (childNodes.isEmpty()) {
            return 0
        }
        val maxChildDepth = childNodes.filterIsInstance<GraphNode>()
            .maxOfOrNull { it.getMaxDepth() } ?: 0
        return maxChildDepth + 1
    }

    /**
     * Gets the total number of nodes in this hierarchy (including this node)
     *
     * @return Total count of nodes
     */
    fun getTotalNodeCount(): Int {
        return 1 + getAllDescendants().size
    }

    /**
     * Gets all connections involving a specific child node
     *
     * @param childId The ID of the child node
     * @return List of connections where the child is source or target
     */
    fun getConnectionsForChild(childId: String): List<Connection> {
        return internalConnections.filter {
            it.sourceNodeId == childId || it.targetNodeId == childId
        }
    }
}
