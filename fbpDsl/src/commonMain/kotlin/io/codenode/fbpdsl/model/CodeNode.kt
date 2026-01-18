/*
 * CodeNode - Terminal Processing Node for FBP Graph
 * Represents executable business logic controlled by coroutines
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * CodeNode represents a terminal node in the FBP graph hierarchy that executes business logic.
 * CodeNodes are controlled by long-running coroutines (Kotlin) or goroutines (Go) and perform
 * the actual data processing in the flow-based program.
 *
 * Based on FBP principles, CodeNodes:
 * - Execute a simple loop: listen on input ports → process → emit on output ports
 * - Are terminal nodes (cannot have children)
 * - Support control operations (pause, resume, speed attenuation)
 * - Have clear execution state lifecycle
 *
 * @property id Unique identifier for this node
 * @property name Human-readable name
 * @property nodeType Type descriptor (e.g., "Transformer", "Validator", "APIEndpoint")
 * @property description Optional documentation
 * @property position Visual canvas position
 * @property inputPorts List of INPUT ports for receiving data
 * @property outputPorts List of OUTPUT ports for emitting data
 * @property configuration Key-value property map for node settings
 * @property parentNodeId Optional reference to parent GraphNode
 * @property executionState Current state of the node's execution
 * @property coroutineHandle Optional runtime reference to controlling coroutine
 * @property processingLogic Reference to code template or custom implementation
 * @property controlConfig Pause/resume/speed attenuation settings
 *
 * @sample
 * ```kotlin
 * val transformerNode = CodeNode(
 *     id = "node_123",
 *     name = "UserDataTransformer",
 *     nodeType = "Transformer",
 *     description = "Transforms user data from API format to domain model",
 *     position = Node.Position(100.0, 200.0),
 *     inputPorts = listOf(
 *         PortFactory.input<ApiUserData>("apiInput", "node_123", required = true)
 *     ),
 *     outputPorts = listOf(
 *         PortFactory.output<DomainUser>("domainOutput", "node_123")
 *     ),
 *     processingLogic = "UserTransformerTemplate"
 * )
 * ```
 */
@Serializable
data class CodeNode(
    override val id: String,
    override val name: String,
    override val nodeType: String,
    override val description: String? = null,
    override val position: Position,
    @Transient override val inputPorts: List<Port<*>> = emptyList(),
    @Transient override val outputPorts: List<Port<*>> = emptyList(),
    override val configuration: Map<String, String> = emptyMap(),
    override val parentNodeId: String? = null,
    val executionState: ExecutionState = ExecutionState.IDLE,
    val coroutineHandle: String? = null,
    val processingLogic: String? = null,
    val controlConfig: ControlConfig = ControlConfig()
) : Node() {

    /**
     * State of the CodeNode's execution lifecycle
     */
    @Serializable
    enum class ExecutionState {
        /** Node is not currently processing */
        IDLE,

        /** Node is actively processing InformationPackets */
        RUNNING,

        /** Node execution is paused, buffering incoming IPs */
        PAUSED,

        /** Node encountered an error and stopped execution */
        ERROR
    }

    /**
     * Configuration for execution control operations
     *
     * @property pauseBufferSize Maximum number of IPs to buffer when paused
     * @property speedAttenuation Delay in milliseconds between processing cycles (for debugging/simulation)
     * @property autoResumeOnError Whether to automatically resume after error state
     */
    @Serializable
    data class ControlConfig(
        val pauseBufferSize: Int = 100,
        val speedAttenuation: Long = 0L,
        val autoResumeOnError: Boolean = false
    ) {
        init {
            require(pauseBufferSize > 0) { "Pause buffer size must be positive, got $pauseBufferSize" }
            require(speedAttenuation >= 0L) { "Speed attenuation cannot be negative, got $speedAttenuation" }
        }
    }

    /**
     * Validates that this CodeNode is well-formed
     *
     * @return Validation result with success flag and error messages
     */
    override fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        // First run base Node validation
        val baseValidation = super.validate()
        errors.addAll(baseValidation.errors)

        // CodeNode-specific validation: cannot have children (terminal node)
        // This is enforced by the type system - CodeNodes don't have a childNodes property

        // Must have at least one input port OR one output port
        if (inputPorts.isEmpty() && outputPorts.isEmpty()) {
            errors.add("CodeNode must have at least one port (input or output)")
        }

        // Processing logic should be defined for code generation (warning, not error)
        // Note: This is a soft requirement - some nodes might be placeholders
        if (processingLogic.isNullOrBlank()) {
            // This is acceptable for initial development, but log a warning
            // errors.add("CodeNode '${name}' has no processing logic defined")
        }

        // Validate control config
        if (controlConfig.pauseBufferSize <= 0) {
            errors.add("Pause buffer size must be positive")
        }
        if (controlConfig.speedAttenuation < 0) {
            errors.add("Speed attenuation cannot be negative")
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Creates a copy of this CodeNode at a new position
     *
     * @param newPosition The new position on the canvas
     * @return New CodeNode instance at the new position
     */
    override fun withPosition(newPosition: Position): CodeNode {
        return copy(position = newPosition)
    }

    /**
     * Creates a copy of this CodeNode with a new parent
     *
     * @param newParentId The new parent GraphNode ID
     * @return New CodeNode instance with updated parent
     */
    override fun withParent(newParentId: String?): CodeNode {
        return copy(parentNodeId = newParentId)
    }

    /**
     * Creates a copy of this CodeNode with a new execution state
     *
     * @param newState The new execution state
     * @return New CodeNode instance with updated state
     */
    fun withExecutionState(newState: ExecutionState): CodeNode {
        return copy(executionState = newState)
    }

    /**
     * Creates a copy of this CodeNode with a new coroutine handle
     *
     * @param handle The runtime coroutine handle reference
     * @return New CodeNode instance with updated handle
     */
    fun withCoroutineHandle(handle: String?): CodeNode {
        return copy(coroutineHandle = handle)
    }

    /**
     * Creates a copy of this CodeNode with updated control configuration
     *
     * @param newConfig The new control configuration
     * @return New CodeNode instance with updated config
     */
    fun withControlConfig(newConfig: ControlConfig): CodeNode {
        return copy(controlConfig = newConfig)
    }

    /**
     * Checks if this CodeNode is currently executing
     *
     * @return true if state is RUNNING
     */
    fun isRunning(): Boolean = executionState == ExecutionState.RUNNING

    /**
     * Checks if this CodeNode is paused
     *
     * @return true if state is PAUSED
     */
    fun isPaused(): Boolean = executionState == ExecutionState.PAUSED

    /**
     * Checks if this CodeNode is in error state
     *
     * @return true if state is ERROR
     */
    fun isError(): Boolean = executionState == ExecutionState.ERROR

    /**
     * Checks if this CodeNode is idle (not processing)
     *
     * @return true if state is IDLE
     */
    fun isIdle(): Boolean = executionState == ExecutionState.IDLE

    /**
     * Checks if this CodeNode can accept new InformationPackets
     *
     * @return true if state is IDLE or RUNNING
     */
    fun canAcceptPackets(): Boolean = executionState == ExecutionState.IDLE || executionState == ExecutionState.RUNNING
}

/**
 * Factory for creating CodeNode instances
 */
object CodeNodeFactory {
    /**
     * Creates a simple CodeNode with default settings
     *
     * @param name Human-readable name
     * @param nodeType Type descriptor (e.g., "Transformer", "Validator")
     * @param position Canvas position
     * @param inputPorts List of input ports
     * @param outputPorts List of output ports
     * @param processingLogic Optional reference to code template
     * @param description Optional documentation
     * @param configuration Optional key-value configuration
     * @return New CodeNode instance
     */
    fun create(
        name: String,
        nodeType: String,
        position: Node.Position = Node.Position.ORIGIN,
        inputPorts: List<Port<*>> = emptyList(),
        outputPorts: List<Port<*>> = emptyList(),
        processingLogic: String? = null,
        description: String? = null,
        configuration: Map<String, String> = emptyMap()
    ): CodeNode {
        return CodeNode(
            id = NodeIdGenerator.generateId("codenode"),
            name = name,
            nodeType = nodeType,
            description = description,
            position = position,
            inputPorts = inputPorts,
            outputPorts = outputPorts,
            configuration = configuration,
            processingLogic = processingLogic
        )
    }

    /**
     * Creates a transformer CodeNode with one input and one output port
     *
     * @param name Human-readable name
     * @param inputPortName Name for the input port
     * @param outputPortName Name for the output port
     * @param position Canvas position
     * @param processingLogic Optional reference to code template
     * @return New CodeNode configured as a transformer
     */
    inline fun <reified TIn : Any, reified TOut : Any> createTransformer(
        name: String,
        inputPortName: String = "input",
        outputPortName: String = "output",
        position: Node.Position = Node.Position.ORIGIN,
        processingLogic: String? = null
    ): CodeNode {
        val nodeId = NodeIdGenerator.generateId("codenode")
        return CodeNode(
            id = nodeId,
            name = name,
            nodeType = "Transformer",
            position = position,
            inputPorts = listOf(
                PortFactory.input<TIn>(inputPortName, nodeId, required = true)
            ),
            outputPorts = listOf(
                PortFactory.output<TOut>(outputPortName, nodeId)
            ),
            processingLogic = processingLogic
        )
    }

    /**
     * Creates a filter CodeNode with one input and one output port
     *
     * @param name Human-readable name
     * @param inputPortName Name for the input port
     * @param outputPortName Name for the output port (passed items)
     * @param position Canvas position
     * @param processingLogic Optional reference to code template
     * @return New CodeNode configured as a filter
     */
    inline fun <reified T : Any> createFilter(
        name: String,
        inputPortName: String = "input",
        outputPortName: String = "output",
        position: Node.Position = Node.Position.ORIGIN,
        processingLogic: String? = null
    ): CodeNode {
        val nodeId = NodeIdGenerator.generateId("codenode")
        return CodeNode(
            id = nodeId,
            name = name,
            nodeType = "Filter",
            position = position,
            inputPorts = listOf(
                PortFactory.input<T>(inputPortName, nodeId, required = true)
            ),
            outputPorts = listOf(
                PortFactory.output<T>(outputPortName, nodeId)
            ),
            processingLogic = processingLogic
        )
    }

    /**
     * Creates a splitter CodeNode with one input and multiple output ports
     *
     * @param name Human-readable name
     * @param inputPortName Name for the input port
     * @param outputPortNames Names for the output ports
     * @param position Canvas position
     * @param processingLogic Optional reference to code template
     * @return New CodeNode configured as a splitter
     */
    inline fun <reified T : Any> createSplitter(
        name: String,
        inputPortName: String = "input",
        outputPortNames: List<String> = listOf("output1", "output2"),
        position: Node.Position = Node.Position.ORIGIN,
        processingLogic: String? = null
    ): CodeNode {
        val nodeId = NodeIdGenerator.generateId("codenode")
        return CodeNode(
            id = nodeId,
            name = name,
            nodeType = "Splitter",
            position = position,
            inputPorts = listOf(
                PortFactory.input<T>(inputPortName, nodeId, required = true)
            ),
            outputPorts = outputPortNames.map { portName ->
                PortFactory.output<T>(portName, nodeId)
            },
            processingLogic = processingLogic
        )
    }

    /**
     * Creates a merger CodeNode with multiple input ports and one output port
     *
     * @param name Human-readable name
     * @param inputPortNames Names for the input ports
     * @param outputPortName Name for the output port
     * @param position Canvas position
     * @param processingLogic Optional reference to code template
     * @return New CodeNode configured as a merger
     */
    inline fun <reified T : Any> createMerger(
        name: String,
        inputPortNames: List<String> = listOf("input1", "input2"),
        outputPortName: String = "output",
        position: Node.Position = Node.Position.ORIGIN,
        processingLogic: String? = null
    ): CodeNode {
        val nodeId = NodeIdGenerator.generateId("codenode")
        return CodeNode(
            id = nodeId,
            name = name,
            nodeType = "Merger",
            position = position,
            inputPorts = inputPortNames.map { portName ->
                PortFactory.input<T>(portName, nodeId, required = false)
            },
            outputPorts = listOf(
                PortFactory.output<T>(outputPortName, nodeId)
            ),
            processingLogic = processingLogic
        )
    }
}
