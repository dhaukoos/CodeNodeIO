/*
 * TypedUseCases - Typed base classes for common CodeNode processing patterns
 * Provides type-safe abstractions for typical data flow operations
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.usecase

import io.codenode.fbpdsl.model.InformationPacket
import io.codenode.fbpdsl.model.InformationPacketFactory
import io.codenode.fbpdsl.model.ProcessingLogic
import kotlin.reflect.KClass

/**
 * Base class for transformer use cases with single input and single output
 *
 * Provides type-safe transformation from input type TIn to output type TOut.
 *
 * @param TIn Input data type
 * @param TOut Output data type
 * @param inputPortName Name of the input port (default: "input")
 * @param outputPortName Name of the output port (default: "output")
 *
 * @sample
 * ```kotlin
 * data class Temperature(val celsius: Double)
 * data class TemperatureF(val fahrenheit: Double)
 *
 * class CelsiusToFahrenheitUseCase : TransformerUseCase<Temperature, TemperatureF>() {
 *     override suspend fun transform(input: Temperature): TemperatureF {
 *         return TemperatureF(input.celsius * 9/5 + 32)
 *     }
 * }
 * ```
 */
abstract class TransformerUseCase<TIn : Any, TOut : Any>(
    protected val outputType: KClass<TOut>,
    protected val inputPortName: String = "input",
    protected val outputPortName: String = "output"
) : ProcessingLogic {

    /**
     * Transform input data to output data
     *
     * @param input The input data
     * @return The transformed output data
     */
    protected abstract suspend fun transform(input: TIn): TOut

    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        @Suppress("UNCHECKED_CAST")
        val inputPacket = inputs[inputPortName] as? InformationPacket<TIn>
            ?: throw IllegalArgumentException("Missing or invalid input packet for port '$inputPortName'")

        val result = transform(inputPacket.payload)
        return mapOf(outputPortName to InformationPacketFactory.createWithType(outputType, result))
    }
}

/**
 * Base class for filter use cases with single input and conditional output
 *
 * Provides type-safe filtering where items pass through only if predicate returns true.
 *
 * @param T Data type for both input and output
 * @param inputPortName Name of the input port (default: "input")
 * @param outputPortName Name of the output port (default: "output")
 *
 * @sample
 * ```kotlin
 * data class NumberData(val value: Int)
 *
 * class EvenNumberFilterUseCase : FilterUseCase<NumberData>() {
 *     override suspend fun shouldPass(input: NumberData): Boolean {
 *         return input.value % 2 == 0
 *     }
 * }
 * ```
 */
abstract class FilterUseCase<T : Any>(
    protected val inputPortName: String = "input",
    protected val outputPortName: String = "output"
) : ProcessingLogic {

    /**
     * Determine if input should pass through the filter
     *
     * @param input The input data
     * @return true to pass through, false to drop
     */
    protected abstract suspend fun shouldPass(input: T): Boolean

    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        @Suppress("UNCHECKED_CAST")
        val inputPacket = inputs[inputPortName] as? InformationPacket<T>
            ?: throw IllegalArgumentException("Missing or invalid input packet for port '$inputPortName'")

        return if (shouldPass(inputPacket.payload)) {
            mapOf(outputPortName to inputPacket)
        } else {
            emptyMap() // Drop the packet
        }
    }
}

/**
 * Base class for validator use cases with routing to valid/invalid outputs
 *
 * Provides type-safe validation with separate output ports for valid and invalid data.
 *
 * @param T Data type for input and outputs
 * @param inputPortName Name of the input port (default: "input")
 * @param validPortName Name of the valid output port (default: "valid")
 * @param invalidPortName Name of the invalid output port (default: "invalid")
 *
 * @sample
 * ```kotlin
 * data class EmailData(val address: String)
 *
 * class EmailValidatorUseCase : ValidatorUseCase<EmailData>() {
 *     override suspend fun isValid(input: EmailData): Boolean {
 *         return input.address.contains("@") && input.address.contains(".")
 *     }
 * }
 * ```
 */
abstract class ValidatorUseCase<T : Any>(
    protected val inputPortName: String = "input",
    protected val validPortName: String = "valid",
    protected val invalidPortName: String = "invalid"
) : ProcessingLogic {

    /**
     * Validate the input data
     *
     * @param input The input data
     * @return true if valid, false if invalid
     */
    protected abstract suspend fun isValid(input: T): Boolean

    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        @Suppress("UNCHECKED_CAST")
        val inputPacket = inputs[inputPortName] as? InformationPacket<T>
            ?: throw IllegalArgumentException("Missing or invalid input packet for port '$inputPortName'")

        val targetPort = if (isValid(inputPacket.payload)) validPortName else invalidPortName
        return mapOf(targetPort to inputPacket)
    }
}

/**
 * Base class for splitter use cases with dynamic output port selection
 *
 * Provides type-safe routing to one or more output ports based on input data.
 *
 * @param T Data type for input and outputs
 * @param inputPortName Name of the input port (default: "input")
 *
 * @sample
 * ```kotlin
 * data class OrderData(val amount: Double, val priority: String)
 *
 * class OrderRouterUseCase : SplitterUseCase<OrderData>() {
 *     override suspend fun selectOutputPorts(input: OrderData): List<String> {
 *         return when {
 *             input.priority == "urgent" -> listOf("urgent")
 *             input.amount > 1000 -> listOf("highValue")
 *             else -> listOf("standard")
 *         }
 *     }
 * }
 * ```
 */
abstract class SplitterUseCase<T : Any>(
    protected val inputPortName: String = "input"
) : ProcessingLogic {

    /**
     * Select which output port(s) should receive the input
     *
     * @param input The input data
     * @return List of output port names
     */
    protected abstract suspend fun selectOutputPorts(input: T): List<String>

    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        @Suppress("UNCHECKED_CAST")
        val inputPacket = inputs[inputPortName] as? InformationPacket<T>
            ?: throw IllegalArgumentException("Missing or invalid input packet for port '$inputPortName'")

        val targetPorts = selectOutputPorts(inputPacket.payload)
        return targetPorts.associateWith { inputPacket }
    }
}

/**
 * Base class for merger use cases combining multiple inputs into one output
 *
 * Provides type-safe merging of multiple input values into a single output.
 *
 * @param T Data type for inputs and output
 * @param outputPortName Name of the output port (default: "output")
 *
 * @sample
 * ```kotlin
 * data class SensorReading(val temperature: Double, val humidity: Double, val timestamp: Long)
 *
 * class SensorMergerUseCase : MergerUseCase<SensorReading>() {
 *     override suspend fun merge(inputs: Map<String, SensorReading>): SensorReading {
 *         // Average all sensor readings
 *         val avgTemp = inputs.values.map { it.temperature }.average()
 *         val avgHumidity = inputs.values.map { it.humidity }.average()
 *         return SensorReading(avgTemp, avgHumidity, Clock.System.now().toEpochMilliseconds())
 *     }
 * }
 * ```
 */
abstract class MergerUseCase<T : Any>(
    protected val outputType: KClass<T>,
    protected val outputPortName: String = "output"
) : ProcessingLogic {

    /**
     * Merge multiple input values into single output
     *
     * @param inputs Map of port name to input data
     * @return The merged output data
     */
    protected abstract suspend fun merge(inputs: Map<String, T>): T

    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        @Suppress("UNCHECKED_CAST")
        // Extract payloads from all available input packets
        val payloads = inputs.mapValues { (_, packet) ->
            (packet as? InformationPacket<T>)?.payload
                ?: throw IllegalArgumentException("Invalid input packet type")
        }

        val result = merge(payloads)
        return mapOf(outputPortName to InformationPacketFactory.createWithType(outputType, result))
    }
}

/**
 * Base class for generator use cases with no input
 *
 * Provides type-safe generation of output data without requiring input.
 *
 * @param T Output data type
 * @param outputPortName Name of the output port (default: "output")
 *
 * @sample
 * ```kotlin
 * data class TimestampData(val timestamp: Long, val formatted: String)
 *
 * class TimestampGeneratorUseCase : GeneratorUseCase<TimestampData>() {
 *     override suspend fun generate(): TimestampData {
 *         val now = Clock.System.now().toEpochMilliseconds()
 *         return TimestampData(now, now.toString())
 *     }
 * }
 * ```
 */
abstract class GeneratorUseCase<T : Any>(
    protected val outputType: KClass<T>,
    protected val outputPortName: String = "output"
) : ProcessingLogic {

    /**
     * Generate output data
     *
     * @return The generated output data
     */
    protected abstract suspend fun generate(): T

    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        val result = generate()
        return mapOf(outputPortName to InformationPacketFactory.createWithType(outputType, result))
    }
}

/**
 * Base class for sink use cases with no output
 *
 * Provides type-safe consumption of input data for side effects (logging, persistence, etc.)
 *
 * @param T Input data type
 * @param inputPortName Name of the input port (default: "input")
 *
 * @sample
 * ```kotlin
 * data class LogEntry(val level: String, val message: String, val timestamp: Long)
 *
 * class LoggerUseCase(private val logger: Logger) : SinkUseCase<LogEntry>() {
 *     override suspend fun consume(input: LogEntry) {
 *         logger.log(input.level, input.message)
 *     }
 * }
 * ```
 */
abstract class SinkUseCase<T : Any>(
    protected val inputPortName: String = "input"
) : ProcessingLogic {

    /**
     * Consume input data for side effects
     *
     * @param input The input data
     */
    protected abstract suspend fun consume(input: T)

    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        @Suppress("UNCHECKED_CAST")
        val inputPacket = inputs[inputPortName] as? InformationPacket<T>
            ?: throw IllegalArgumentException("Missing or invalid input packet for port '$inputPortName'")

        consume(inputPacket.payload)
        return emptyMap() // No outputs
    }
}
