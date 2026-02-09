/*
 * PortFactory - Factory for Creating Port Instances
 * Provides convenience methods for port creation with type inference
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.datetime.Clock
import kotlin.reflect.KClass

/**
 * Factory for creating Port instances with convenient type-safe methods
 */
object PortFactory {
    /**
     * Creates a simple INPUT port with default settings using reified type
     *
     * @param T The type of data this port accepts (must be a Kotlin data class)
     * @param name Port name
     * @param owningNodeId Parent node ID
     * @param required Whether connection is mandatory
     * @param defaultValue Optional default value if port is unconnected
     * @param validationRules Optional validation predicates for incoming data
     * @return New Port instance
     *
     * @sample
     * ```kotlin
     * data class UserData(val name: String, val email: String)
     *
     * val inputPort = PortFactory.input<UserData>(
     *     name = "userInput",
     *     owningNodeId = "node_123",
     *     required = true
     * )
     * ```
     */
    inline fun <reified T : Any> input(
        name: String,
        owningNodeId: String,
        required: Boolean = false,
        defaultValue: T? = null,
        validationRules: List<(T) -> ValidationResult> = emptyList()
    ): Port<T> {
        return Port(
            id = generateId(),
            name = name,
            direction = Port.Direction.INPUT,
            dataType = T::class,
            required = required,
            defaultValue = defaultValue,
            validationRules = validationRules,
            owningNodeId = owningNodeId
        )
    }

    /**
     * Creates a simple OUTPUT port with default settings using reified type
     *
     * @param T The type of data this port emits (must be a Kotlin data class)
     * @param name Port name
     * @param owningNodeId Parent node ID
     * @param validationRules Optional validation predicates for outgoing data
     * @return New Port instance
     *
     * @sample
     * ```kotlin
     * data class ProcessedData(val result: String, val timestamp: Long)
     *
     * val outputPort = PortFactory.output<ProcessedData>(
     *     name = "result",
     *     owningNodeId = "node_456"
     * )
     * ```
     */
    inline fun <reified T : Any> output(
        name: String,
        owningNodeId: String,
        validationRules: List<(T) -> ValidationResult> = emptyList()
    ): Port<T> {
        return Port(
            id = generateId(),
            name = name,
            direction = Port.Direction.OUTPUT,
            dataType = T::class,
            required = false,
            defaultValue = null,
            validationRules = validationRules,
            owningNodeId = owningNodeId
        )
    }

    /**
     * Creates an INPUT port with explicit type specification
     * Useful when the type cannot be inferred or needs to be specified explicitly
     *
     * @param T The type of data this port accepts
     * @param name Port name
     * @param dataType The KClass representing the expected data type
     * @param owningNodeId Parent node ID
     * @param required Whether connection is mandatory
     * @param defaultValue Optional default value if port is unconnected
     * @param validationRules Optional validation predicates for incoming data
     * @return New Port instance
     */
    fun <T : Any> inputWithType(
        name: String,
        dataType: KClass<T>,
        owningNodeId: String,
        required: Boolean = false,
        defaultValue: T? = null,
        validationRules: List<(T) -> ValidationResult> = emptyList()
    ): Port<T> {
        return Port(
            id = generateId(),
            name = name,
            direction = Port.Direction.INPUT,
            dataType = dataType,
            required = required,
            defaultValue = defaultValue,
            validationRules = validationRules,
            owningNodeId = owningNodeId
        )
    }

    /**
     * Creates an OUTPUT port with explicit type specification
     * Useful when the type cannot be inferred or needs to be specified explicitly
     *
     * @param T The type of data this port emits
     * @param name Port name
     * @param dataType The KClass representing the data type
     * @param owningNodeId Parent node ID
     * @param validationRules Optional validation predicates for outgoing data
     * @return New Port instance
     */
    fun <T : Any> outputWithType(
        name: String,
        dataType: KClass<T>,
        owningNodeId: String,
        validationRules: List<(T) -> ValidationResult> = emptyList()
    ): Port<T> {
        return Port(
            id = generateId(),
            name = name,
            direction = Port.Direction.OUTPUT,
            dataType = dataType,
            required = false,
            defaultValue = null,
            validationRules = validationRules,
            owningNodeId = owningNodeId
        )
    }

    /**
     * Generates a unique ID for a Port
     *
     * @return Unique identifier string
     */
    fun generateId(): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val random = (0..999999).random()
        return "port_${timestamp}_$random"
    }
}
