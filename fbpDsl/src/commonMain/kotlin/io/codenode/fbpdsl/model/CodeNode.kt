/*
 * CodeNode - Terminal Processing Node for FBP Graph
 * Represents executable business logic controlled by coroutines
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Type classification for CodeNode instances
 */
@Serializable
enum class CodeNodeType {
    /** Transforms input data to different output type */
    TRANSFORMER,

    /** Filters input based on predicate, passing only matching items */
    FILTER,

    /** Splits single input into multiple outputs */
    SPLITTER,

    /** Merges multiple inputs into single output */
    MERGER,

    /** Validates input data against rules */
    VALIDATOR,

    /** Generates data (no input required) */
    GENERATOR,

    /** Consumes data (no output) */
    SINK,

    /** API endpoint integration */
    API_ENDPOINT,

    /** Database operation */
    DATABASE,

    /** Custom/user-defined node type */
    CUSTOM,

    /** Generic node with configurable inputs/outputs (0-5 each) */
    GENERIC;

    /**
     * Returns a human-readable type name
     */
    val typeName: String
        get() = name.lowercase().replace('_', ' ').split(' ')
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}

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
 * @property codeNodeType Type classification enum
 * @property description Optional documentation
 * @property position Visual canvas position
 * @property inputPorts List of INPUT ports for receiving data
 * @property outputPorts List of OUTPUT ports for emitting data
 * @property configuration Key-value property map for node settings
 * @property parentNodeId Optional reference to parent GraphNode
 * @property executionState Current state of the node's execution
 * @property coroutineHandle Optional runtime reference to controlling coroutine
 * @property controlConfig Pause/resume/speed attenuation settings
 *
 * @sample
 * ```kotlin
 * val transformerNode = CodeNode(
 *     id = "node_123",
 *     name = "UserDataTransformer",
 *     codeNodeType = CodeNodeType.TRANSFORMER,
 *     description = "Transforms user data from API format to domain model",
 *     position = Node.Position(100.0, 200.0),
 *     inputPorts = listOf(
 *         PortFactory.input<ApiUserData>("apiInput", "node_123", required = true)
 *     ),
 *     outputPorts = listOf(
 *         PortFactory.output<DomainUser>("domainOutput", "node_123")
 *     )
 * )
 * ```
 */
@Serializable
data class CodeNode(
    override val id: String,
    override val name: String,
    val codeNodeType: CodeNodeType,
    override val description: String? = null,
    override val position: Position,
    @Transient override val inputPorts: List<Port<*>> = emptyList(),
    @Transient override val outputPorts: List<Port<*>> = emptyList(),
    override val configuration: Map<String, String> = emptyMap(),
    override val parentNodeId: String? = null,
    override val executionState: ExecutionState = ExecutionState.IDLE,
    val coroutineHandle: String? = null,
    override val controlConfig: ControlConfig = ControlConfig()
) : Node() {

    /**
     * Returns the string representation of the node type
     * Derived from the codeNodeType enum
     */
    override val nodeType: String
        get() = codeNodeType.typeName

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
     * Creates a copy of this CodeNode with a new execution state.
     *
     * For CodeNode, the propagate parameter is ignored since CodeNodes
     * are terminal nodes with no children.
     *
     * @param newState The new execution state
     * @param propagate Ignored for CodeNode (no children to propagate to)
     * @return New CodeNode instance with updated state
     */
    override fun withExecutionState(newState: ExecutionState, propagate: Boolean): CodeNode {
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
     * Creates a copy of this CodeNode with updated control configuration.
     *
     * For CodeNode, the propagate parameter is ignored since CodeNodes
     * are terminal nodes with no children.
     *
     * @param newConfig The new control configuration
     * @param propagate Ignored for CodeNode (no children to propagate to)
     * @return New CodeNode instance with updated config
     */
    override fun withControlConfig(newConfig: ControlConfig, propagate: Boolean): CodeNode {
        return copy(controlConfig = newConfig)
    }

    /**
     * Gets the effective control configuration for this CodeNode.
     *
     * For CodeNode, this simply returns the node's own controlConfig,
     * as any parent propagation has already been applied.
     *
     * @return The effective ControlConfig for this node
     */
    override fun getEffectiveControlConfig(): ControlConfig {
        return controlConfig
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
