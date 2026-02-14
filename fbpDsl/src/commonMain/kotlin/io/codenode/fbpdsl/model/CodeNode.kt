/*
 * CodeNode - Terminal Processing Node for FBP Graph
 * Represents executable business logic controlled by coroutines
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Processing logic interface for CodeNode execution
 *
 * Takes a map of input port names to InformationPackets and produces
 * a map of output port names to InformationPackets.
 *
 * This is a functional interface (SAM - Single Abstract Method) that supports:
 * - Lambda syntax: `ProcessingLogic { inputs -> ... }`
 * - Class implementations: `class MyUseCase : ProcessingLogic { ... }`
 * - Dependency injection, state management, and lifecycle hooks
 *
 * The suspend operator function supports asynchronous operations like
 * API calls, database queries, or file I/O.
 *
 * @sample
 * ```kotlin
 * // Lambda usage
 * val logic: ProcessingLogic = { inputs ->
 *     // Process and return outputs
 *     mapOf(...)
 * }
 *
 * // Class usage (UseCase pattern)
 * class TransformUseCase(private val apiClient: ApiClient) : ProcessingLogic {
 *     override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
 *         // Use injected dependencies
 *         return mapOf(...)
 *     }
 * }
 * ```
 */
fun interface ProcessingLogic {
    /**
     * Processes input InformationPackets and produces output InformationPackets
     *
     * @param inputs Map of input port name to received InformationPacket
     * @return Map of output port name to produced InformationPacket
     */
    suspend operator fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>>
}

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
 * @property processingLogic Lambda function that processes input IPs and produces output IPs
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
 *     ),
 *     processingLogic = { inputs ->
 *         val apiData = inputs["apiInput"]?.payload as? ApiUserData
 *         val domainUser = apiData?.let { DomainUser(it.name, it.email) }
 *         mapOf("domainOutput" to InformationPacketFactory.create(domainUser))
 *     }
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
    @Transient val processingLogic: ProcessingLogic? = null,
    override val controlConfig: ControlConfig = ControlConfig()
) : Node() {

    /**
     * Runtime job reference for node lifecycle control.
     * Tracks the active coroutine job when the node is running.
     * Marked @Transient as Job cannot be serialized.
     */
    @Transient
    var nodeControlJob: Job? = null

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

        // Processing logic should be defined for execution (warning, not error)
        // Note: This is a soft requirement - some nodes might be placeholders during design
        if (processingLogic == null) {
            // This is acceptable for initial development/design, but the node cannot execute
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

    /**
     * Checks if this CodeNode has processing logic defined
     *
     * @return true if processingLogic is not null
     */
    fun hasProcessingLogic(): Boolean = processingLogic != null

    /**
     * Creates a copy of this CodeNode with new processing logic
     *
     * @param logic The new processing logic function
     * @return New CodeNode instance with updated processing logic
     */
    fun withProcessingLogic(logic: ProcessingLogic): CodeNode {
        return copy(processingLogic = logic)
    }

    /**
     * Executes the processing logic with the given inputs
     *
     * @param inputs Map of input port names to InformationPackets
     * @return Map of output port names to InformationPackets
     * @throws IllegalStateException if processingLogic is null
     */
    suspend fun process(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        val logic = processingLogic ?: throw IllegalStateException(
            "CodeNode '$name' has no processing logic defined. Cannot execute."
        )
        return logic(inputs)
    }

    /**
     * Starts the node's processing loop.
     *
     * Manages the nodeControlJob lifecycle:
     * 1. Cancels any existing job (prevents duplicate jobs)
     * 2. Launches new job in provided scope
     * 3. Executes processingBlock within the job
     *
     * Note: executionState should be set to RUNNING by the caller before calling start(),
     * or use withExecutionState() for immutability.
     *
     * @param scope CoroutineScope to launch the processing job in
     * @param processingBlock Custom processing logic to execute in the job loop
     */
    fun start(
        scope: CoroutineScope,
        processingBlock: suspend () -> Unit
    ) {
        // Cancel existing job if running (prevents duplicate jobs)
        nodeControlJob?.cancel()

        // Launch the processing job
        nodeControlJob = scope.launch {
            processingBlock()
        }
    }

    /**
     * Stops the node's processing loop.
     *
     * Manages graceful shutdown:
     * 1. Cancels the nodeControlJob
     * 2. Sets nodeControlJob to null
     *
     * Valid from RUNNING or PAUSED states. No-op if already IDLE.
     *
     * Note: executionState transition to IDLE should be handled
     * by the caller or via withExecutionState() for immutability.
     */
    fun stop() {
        nodeControlJob?.cancel()
        nodeControlJob = null
    }

    /**
     * Pauses the node's processing loop.
     *
     * Signals pause intent without cancelling the job:
     * 1. Only meaningful when state is RUNNING
     * 2. Job remains active but processing should check isPaused()
     *
     * Note: For data class immutability, caller should use withExecutionState()
     * to transition to PAUSED state. This method signals intent.
     * The processing loop must check isPaused() to honor pause requests.
     */
    fun pause() {
        // No-op if not running - pause only valid from RUNNING state
        if (executionState != ExecutionState.RUNNING) return
        // Actual state change handled by caller via withExecutionState()
    }

    /**
     * Resumes the node's processing loop from paused state.
     *
     * Signals resume intent:
     * 1. Only meaningful when state is PAUSED
     * 2. Processing loop should resume when isPaused() returns false
     *
     * Note: For data class immutability, caller should use withExecutionState()
     * to transition back to RUNNING state. This method signals intent.
     */
    fun resume() {
        // No-op if not paused - resume only valid from PAUSED state
        if (executionState != ExecutionState.PAUSED) return
        // Actual state change handled by caller via withExecutionState()
    }
}
