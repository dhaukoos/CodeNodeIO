/*
 * FlowGraphDsl - Kotlin DSL for Flow-based Programming Graphs
 * Provides infix functions for creating flow graphs with readable syntax
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.dsl

import io.codenode.fbpdsl.model.*
import kotlin.reflect.KClass
import kotlinx.datetime.Clock

/**
 * DSL marker to prevent nested DSL scope confusion
 */
@DslMarker
annotation class FlowGraphDslMarker

/**
 * Entry point for creating a FlowGraph using the DSL
 *
 * @param name The name of the flow graph
 * @param version Semantic version (defaults to "1.0.0")
 * @param description Optional description
 * @param block DSL builder block
 * @return Configured FlowGraph instance
 *
 * @sample
 * ```kotlin
 * val graph = flowGraph("UserValidation", version = "1.0.0") {
 *     val inputNode = codeNode("UserInput") {
 *         output("userData", User::class)
 *     }
 *     val validatorNode = codeNode("EmailValidator") {
 *         input("user", User::class)
 *         output("valid", User::class)
 *         output("error", ErrorData::class)
 *     }
 *     inputNode.output("userData") connect validatorNode.input("user")
 * }
 * ```
 */
fun flowGraph(
    name: String,
    version: String = "1.0.0",
    description: String? = null,
    block: FlowGraphBuilder.() -> Unit
): FlowGraph {
    val builder = FlowGraphBuilder(name, version, description)
    builder.block()
    return builder.build()
}

/**
 * Builder for constructing FlowGraph instances using DSL
 */
@FlowGraphDslMarker
class FlowGraphBuilder(
    private val name: String,
    private val version: String,
    private val description: String?
) {
    private val nodes = mutableListOf<Node>()
    private val connections = mutableListOf<Connection>()
    private val metadata = mutableMapOf<String, String>()
    private val targetPlatforms = mutableListOf<FlowGraph.TargetPlatform>()

    /**
     * Creates a CodeNode and adds it to the graph
     *
     * @param name Node name
     * @param nodeType Optional node type definition ID
     * @param block Configuration block for the node
     * @return The created NodeBuilder for method chaining
     */
    fun codeNode(
        name: String,
        nodeType: String? = null,
        block: NodeBuilder.() -> Unit = {}
    ): NodeBuilder {
        val builder = NodeBuilder(name, nodeType, isGraphNode = false, flowGraphBuilder = this)
        builder.block()
        val node = builder.buildCodeNode()
        nodes.add(node)
        return builder
    }

    /**
     * Creates a GraphNode (container node) and adds it to the graph
     *
     * @param name Node name
     * @param block Configuration block for the graph node
     * @return The created GraphNodeBuilder for method chaining
     */
    fun graphNode(
        name: String,
        block: GraphNodeBuilder.() -> Unit = {}
    ): GraphNodeBuilder {
        val builder = GraphNodeBuilder(name, flowGraphBuilder = this)
        builder.block()
        val node = builder.build()
        nodes.add(node)
        return builder
    }

    /**
     * Adds a connection between two ports
     *
     * @param connection The connection to add
     */
    fun addConnection(connection: Connection) {
        connections.add(connection)
    }

    /**
     * Sets metadata for the graph
     *
     * @param key Metadata key
     * @param value Metadata value
     */
    fun metadata(key: String, value: String) {
        metadata[key] = value
    }

    /**
     * Adds a target platform for code generation
     *
     * @param platform Target platform
     */
    fun targetPlatform(platform: FlowGraph.TargetPlatform) {
        targetPlatforms.add(platform)
    }

    /**
     * Adds multiple target platforms
     *
     * @param platforms Target platforms
     */
    fun targetPlatforms(vararg platforms: FlowGraph.TargetPlatform) {
        targetPlatforms.addAll(platforms)
    }

    /**
     * Builds the final FlowGraph instance
     *
     * @return Configured FlowGraph
     */
    fun build(): FlowGraph {
        val graphId = "graph_${Clock.System.now().toEpochMilliseconds()}_${(0..999999).random()}"
        return FlowGraph(
            id = graphId,
            name = name,
            version = version,
            description = description,
            rootNodes = nodes,
            connections = connections,
            metadata = metadata,
            targetPlatforms = targetPlatforms
        )
    }
}

/**
 * Builder for constructing Node instances (CodeNode and GraphNode setup)
 */
@FlowGraphDslMarker
class NodeBuilder(
    private val name: String,
    private val nodeType: String?,
    private val isGraphNode: Boolean,
    private val flowGraphBuilder: FlowGraphBuilder? = null
) {
    private val inputPorts = mutableListOf<Port<*>>()
    private val outputPorts = mutableListOf<Port<*>>()
    private val nodeId = "node_${Clock.System.now().toEpochMilliseconds()}_${(0..999999).random()}"
    private val configuration = mutableMapOf<String, String>()

    /**
     * Adds an input port to the node
     *
     * @param portName Port name
     * @param dataType Data type for the port
     * @param required Whether the port must be connected
     * @return PortReference for connection DSL
     */
    fun <T : Any> input(
        portName: String,
        dataType: KClass<T>,
        required: Boolean = false
    ): PortReference {
        val port = PortFactory.inputWithType(
            name = portName,
            dataType = dataType,
            owningNodeId = nodeId,
            required = required
        )
        inputPorts.add(port)
        return PortReference(nodeId, port.id, portName, Port.Direction.INPUT, flowGraphBuilder)
    }

    /**
     * Adds an output port to the node
     *
     * @param portName Port name
     * @param dataType Data type for the port
     * @return PortReference for connection DSL
     */
    fun <T : Any> output(
        portName: String,
        dataType: KClass<T>
    ): PortReference {
        val port = PortFactory.outputWithType(
            name = portName,
            dataType = dataType,
            owningNodeId = nodeId
        )
        outputPorts.add(port)
        return PortReference(nodeId, port.id, portName, Port.Direction.OUTPUT, flowGraphBuilder)
    }

    /**
     * Sets a configuration property
     *
     * @param key Property key
     * @param value Property value
     */
    fun config(key: String, value: String) {
        configuration[key] = value
    }

    /**
     * Gets a port reference by name for connections
     *
     * @param portName Port name
     * @return PortReference for connection DSL
     */
    fun output(portName: String): PortReference {
        val port = outputPorts.find { it.name == portName }
            ?: throw IllegalArgumentException("Output port '$portName' not found on node '$name'")
        return PortReference(nodeId, port.id, portName, Port.Direction.OUTPUT, flowGraphBuilder)
    }

    /**
     * Gets an input port reference by name for connections
     *
     * @param portName Port name
     * @return PortReference for connection DSL
     */
    fun input(portName: String): PortReference {
        val port = inputPorts.find { it.name == portName }
            ?: throw IllegalArgumentException("Input port '$portName' not found on node '$name'")
        return PortReference(nodeId, port.id, portName, Port.Direction.INPUT, flowGraphBuilder)
    }

    /**
     * Builds a CodeNode instance
     *
     * @return Configured CodeNode
     */
    fun buildCodeNode(): CodeNode {
        return CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.TRANSFORMER, // Default type, could be parameterized
            description = null,
            position = Node.Position(0.0, 0.0), // Default position, can be set later
            inputPorts = inputPorts,
            outputPorts = outputPorts,
            configuration = configuration,
            parentNodeId = null
        )
    }

    /**
     * Gets the node ID (for internal use)
     */
    internal fun getNodeId(): String = nodeId
}

/**
 * Builder for constructing GraphNode instances (container nodes)
 */
@FlowGraphDslMarker
class GraphNodeBuilder(
    private val name: String,
    private val flowGraphBuilder: FlowGraphBuilder? = null
) {
    private val childNodes = mutableListOf<Node>()
    private val internalConnections = mutableListOf<Connection>()
    private val inputPorts = mutableListOf<Port<*>>()
    private val outputPorts = mutableListOf<Port<*>>()
    private val portMappings = mutableMapOf<String, String>()
    private val nodeId = "graphNode_${Clock.System.now().toEpochMilliseconds()}_${(0..999999).random()}"

    /**
     * Adds a child CodeNode to this GraphNode
     *
     * @param name Child node name
     * @param nodeType Optional node type definition ID
     * @param block Configuration block for the child node
     * @return The created NodeBuilder
     */
    fun codeNode(
        name: String,
        nodeType: String? = null,
        block: NodeBuilder.() -> Unit = {}
    ): NodeBuilder {
        val builder = NodeBuilder(name, nodeType, isGraphNode = false)
        builder.block()
        val node = builder.buildCodeNode().copy(parentNodeId = nodeId)
        childNodes.add(node)
        return builder
    }

    /**
     * Adds a child GraphNode to this GraphNode (nested composition)
     *
     * @param name Child graph node name
     * @param block Configuration block for the child graph node
     * @return The created GraphNodeBuilder
     */
    fun graphNode(
        name: String,
        block: GraphNodeBuilder.() -> Unit = {}
    ): GraphNodeBuilder {
        val builder = GraphNodeBuilder(name)
        builder.block()
        val node = builder.build().copy(parentNodeId = nodeId)
        childNodes.add(node)
        return builder
    }

    /**
     * Adds an input port to this GraphNode
     *
     * @param portName Port name
     * @param dataType Data type for the port
     * @param required Whether the port must be connected
     * @return PortReference for connection DSL
     */
    fun <T : Any> input(
        portName: String,
        dataType: KClass<T>,
        required: Boolean = false
    ): PortReference {
        val port = PortFactory.inputWithType(
            name = portName,
            dataType = dataType,
            owningNodeId = nodeId,
            required = required
        )
        inputPorts.add(port)
        return PortReference(nodeId, port.id, portName, Port.Direction.INPUT, flowGraphBuilder)
    }

    /**
     * Adds an output port to this GraphNode
     *
     * @param portName Port name
     * @param dataType Data type for the port
     * @return PortReference for connection DSL
     */
    fun <T : Any> output(
        portName: String,
        dataType: KClass<T>
    ): PortReference {
        val port = PortFactory.outputWithType(
            name = portName,
            dataType = dataType,
            owningNodeId = nodeId
        )
        outputPorts.add(port)
        return PortReference(nodeId, port.id, portName, Port.Direction.OUTPUT, flowGraphBuilder)
    }

    /**
     * Maps an external port to an internal child port
     *
     * @param externalPortName The GraphNode's port name
     * @param childPortPath Child port path (e.g., "childNode.portName")
     */
    fun mapPort(externalPortName: String, childPortPath: String) {
        portMappings[externalPortName] = childPortPath
    }

    /**
     * Adds an internal connection between child nodes
     *
     * @param connection The connection to add
     */
    fun addInternalConnection(connection: Connection) {
        internalConnections.add(connection)
    }

    /**
     * Builds the final GraphNode instance
     *
     * @return Configured GraphNode
     */
    fun build(): GraphNode {
        // Convert port mappings from String to PortMapping objects
        val portMappingObjects = portMappings.mapValues { (_, childPortPath) ->
            val parts = childPortPath.split(".")
            if (parts.size != 2) {
                throw IllegalArgumentException("Port mapping must be in format 'childNodeId.portName', got: '$childPortPath'")
            }
            GraphNode.PortMapping(
                childNodeId = parts[0],
                childPortName = parts[1]
            )
        }

        return GraphNode(
            id = nodeId,
            name = name,
            description = null,
            position = Node.Position(0.0, 0.0), // Default position, can be set later
            inputPorts = inputPorts,
            outputPorts = outputPorts,
            configuration = emptyMap(),
            parentNodeId = null,
            childNodes = childNodes,
            internalConnections = internalConnections,
            portMappings = portMappingObjects
        )
    }
}

/**
 * Reference to a port for use in connection DSL
 */
data class PortReference(
    val nodeId: String,
    val portId: String,
    val portName: String,
    val direction: Port.Direction,
    internal val builder: FlowGraphBuilder? = null
) {
    /**
     * Infix function to create a connection between ports
     *
     * @param target Target port reference
     * @return The created connection
     */
    infix fun connect(target: PortReference): Connection {
        // Validate direction compatibility
        if (this.direction != Port.Direction.OUTPUT) {
            throw IllegalArgumentException("Source port must be OUTPUT, got ${this.direction}")
        }
        if (target.direction != Port.Direction.INPUT) {
            throw IllegalArgumentException("Target port must be INPUT, got ${target.direction}")
        }

        val connection = ConnectionFactory.create(
            sourceNodeId = this.nodeId,
            sourcePortId = this.portId,
            targetNodeId = target.nodeId,
            targetPortId = target.portId
        )

        // Auto-register connection if builder is available
        builder?.addConnection(connection)

        return connection
    }

    /**
     * Creates a buffered connection with specified capacity
     *
     * @param capacity Channel buffer capacity
     * @return ConnectionBuilder for further configuration
     */
    fun connectBuffered(capacity: Int): ConnectionBuilder {
        return ConnectionBuilder(this, capacity)
    }
}

/**
 * Builder for creating connections with additional configuration
 */
class ConnectionBuilder(
    private val source: PortReference,
    private val capacity: Int = 0
) {
    /**
     * Completes the connection to the target port
     *
     * @param target Target port reference
     * @return The created connection
     */
    infix fun to(target: PortReference): Connection {
        // Validate direction compatibility
        if (source.direction != Port.Direction.OUTPUT) {
            throw IllegalArgumentException("Source port must be OUTPUT, got ${source.direction}")
        }
        if (target.direction != Port.Direction.INPUT) {
            throw IllegalArgumentException("Target port must be INPUT, got ${target.direction}")
        }

        return ConnectionFactory.createBuffered(
            sourceNodeId = source.nodeId,
            sourcePortId = source.portId,
            targetNodeId = target.nodeId,
            targetPortId = target.portId,
            capacity = capacity
        )
    }
}

/**
 * DSL marker for port configuration
 */
@FlowGraphDslMarker
annotation class PortDsl

/**
 * Type aliases for common data types used in DSL
 */
typealias StringType = StringData
typealias NumberType = NumberData
typealias BooleanType = BooleanData
typealias ErrorType = ErrorData
