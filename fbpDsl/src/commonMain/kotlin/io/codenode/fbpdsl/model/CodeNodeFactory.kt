/*
 * CodeNodeFactory - Factory for creating CodeNode instances
 * Provides convenience methods for common CodeNode patterns
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import io.codenode.fbpdsl.runtime.ContinuousFilterPredicate
import io.codenode.fbpdsl.runtime.ContinuousGeneratorBlock
import io.codenode.fbpdsl.runtime.ContinuousSinkBlock
import io.codenode.fbpdsl.runtime.ContinuousTransformBlock
import io.codenode.fbpdsl.runtime.FilterRuntime
import io.codenode.fbpdsl.runtime.GeneratorRuntime
import io.codenode.fbpdsl.runtime.SinkRuntime
import io.codenode.fbpdsl.runtime.TransformerRuntime
import kotlinx.coroutines.channels.Channel

/**
 * Factory for creating CodeNode instances with type-safe patterns
 */
object CodeNodeFactory {
    /**
     * Creates a simple CodeNode with default settings
     *
     * @param name Human-readable name
     * @param codeNodeType Type classification enum
     * @param position Canvas position
     * @param inputPorts List of input ports
     * @param outputPorts List of output ports
     * @param processingLogic Optional processing logic lambda function
     * @param description Optional documentation
     * @param configuration Optional key-value configuration
     * @return New CodeNode instance
     */
    fun create(
        name: String,
        codeNodeType: CodeNodeType,
        position: Node.Position = Node.Position.ORIGIN,
        inputPorts: List<Port<*>> = emptyList(),
        outputPorts: List<Port<*>> = emptyList(),
        processingLogic: ProcessingLogic? = null,
        description: String? = null,
        configuration: Map<String, String> = emptyMap()
    ): CodeNode {
        return CodeNode(
            id = NodeIdGenerator.generateId("codenode"),
            name = name,
            codeNodeType = codeNodeType,
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
     * @param TIn Input type parameter
     * @param TOut Output type parameter
     * @param name Human-readable name
     * @param inputPortName Name for the input port
     * @param outputPortName Name for the output port
     * @param position Canvas position
     * @param description Optional documentation
     * @param transform Type-safe transformation function from TIn to TOut
     * @return New CodeNode configured as a transformer
     *
     * @sample
     * ```kotlin
     * data class InputData(val value: String)
     * data class OutputData(val result: Int)
     *
     * val node = CodeNodeFactory.createTransformer<InputData, OutputData>(
     *     name = "StringToInt",
     *     transform = { input ->
     *         OutputData(input.value.toIntOrNull() ?: 0)
     *     }
     * )
     * ```
     */
    inline fun <reified TIn : Any, reified TOut : Any> createTransformer(
        name: String,
        inputPortName: String = "input",
        outputPortName: String = "output",
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline transform: suspend (TIn) -> TOut
    ): CodeNode {
        val nodeId = NodeIdGenerator.generateId("codenode")

        // Wrap the type-safe transform function in ProcessingLogic
        val processingLogic = ProcessingLogic { inputs ->
            @Suppress("UNCHECKED_CAST")
            val inputPacket = inputs[inputPortName] as? InformationPacket<TIn>
                ?: throw IllegalArgumentException("Missing or invalid input packet for port '$inputPortName'")

            val result = transform(inputPacket.payload)
            mapOf(outputPortName to InformationPacketFactory.create(result))
        }

        return CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.TRANSFORMER,
            description = description,
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
     * @param T Type parameter for both input and output
     * @param name Human-readable name
     * @param inputPortName Name for the input port
     * @param outputPortName Name for the output port (passed items)
     * @param position Canvas position
     * @param description Optional documentation
     * @param predicate Type-safe filter predicate - returns true to pass, false to drop
     * @return New CodeNode configured as a filter
     *
     * @sample
     * ```kotlin
     * data class NumberData(val value: Int)
     *
     * val node = CodeNodeFactory.createFilter<NumberData>(
     *     name = "PositiveNumberFilter",
     *     predicate = { data -> data.value > 0 }
     * )
     * ```
     */
    inline fun <reified T : Any> createFilter(
        name: String,
        inputPortName: String = "input",
        outputPortName: String = "output",
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline predicate: suspend (T) -> Boolean
    ): CodeNode {
        val nodeId = NodeIdGenerator.generateId("codenode")

        // Wrap the type-safe predicate in ProcessingLogic
        val processingLogic = ProcessingLogic { inputs ->
            @Suppress("UNCHECKED_CAST")
            val inputPacket = inputs[inputPortName] as? InformationPacket<T>
                ?: throw IllegalArgumentException("Missing or invalid input packet for port '$inputPortName'")

            // Only pass through if predicate returns true
            if (predicate(inputPacket.payload)) {
                mapOf(outputPortName to inputPacket)
            } else {
                emptyMap() // Drop the packet
            }
        }

        return CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.FILTER,
            description = description,
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
     * @param T Type parameter for input and outputs
     * @param name Human-readable name
     * @param inputPortName Name for the input port
     * @param outputPortNames Names for the output ports
     * @param position Canvas position
     * @param description Optional documentation
     * @param split Type-safe splitter function - maps input to output port assignments
     * @return New CodeNode configured as a splitter
     *
     * @sample
     * ```kotlin
     * data class NumberData(val value: Int)
     *
     * val node = CodeNodeFactory.createSplitter<NumberData>(
     *     name = "EvenOddSplitter",
     *     outputPortNames = listOf("even", "odd"),
     *     split = { data ->
     *         if (data.value % 2 == 0) listOf("even") else listOf("odd")
     *     }
     * )
     * ```
     */
    inline fun <reified T : Any> createSplitter(
        name: String,
        inputPortName: String = "input",
        outputPortNames: List<String> = listOf("output1", "output2"),
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline split: suspend (T) -> List<String>
    ): CodeNode {
        val nodeId = NodeIdGenerator.generateId("codenode")

        // Wrap the type-safe split function in ProcessingLogic
        val processingLogic = ProcessingLogic { inputs ->
            @Suppress("UNCHECKED_CAST")
            val inputPacket = inputs[inputPortName] as? InformationPacket<T>
                ?: throw IllegalArgumentException("Missing or invalid input packet for port '$inputPortName'")

            // Get target ports from split function
            val targetPorts = split(inputPacket.payload)

            // Send to each target port
            targetPorts.associateWith { inputPacket }
        }

        return CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.SPLITTER,
            description = description,
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
     * @param T Type parameter for inputs and output
     * @param name Human-readable name
     * @param inputPortNames Names for the input ports
     * @param outputPortName Name for the output port
     * @param position Canvas position
     * @param description Optional documentation
     * @param merge Type-safe merge function - combines available inputs into one output
     * @return New CodeNode configured as a merger
     *
     * @sample
     * ```kotlin
     * data class NumberData(val value: Int)
     *
     * val node = CodeNodeFactory.createMerger<NumberData>(
     *     name = "SumMerger",
     *     inputPortNames = listOf("a", "b", "c"),
     *     merge = { inputs ->
     *         val sum = inputs.values.sumOf { it.value }
     *         NumberData(sum)
     *     }
     * )
     * ```
     */
    inline fun <reified T : Any> createMerger(
        name: String,
        inputPortNames: List<String> = listOf("input1", "input2"),
        outputPortName: String = "output",
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline merge: suspend (Map<String, T>) -> T
    ): CodeNode {
        val nodeId = NodeIdGenerator.generateId("codenode")

        // Wrap the type-safe merge function in ProcessingLogic
        val processingLogic = ProcessingLogic { inputs ->
            @Suppress("UNCHECKED_CAST")
            // Extract payloads from all available input packets
            val payloads = inputs.mapValues { (_, packet) ->
                (packet as? InformationPacket<T>)?.payload
                    ?: throw IllegalArgumentException("Invalid input packet type")
            }

            // Merge the inputs
            val result = merge(payloads)
            mapOf(outputPortName to InformationPacketFactory.create(result))
        }

        return CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.MERGER,
            description = description,
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

    /**
     * Creates a validator CodeNode with one input and two output ports (valid/invalid)
     *
     * @param T Type parameter for input and outputs
     * @param name Human-readable name
     * @param inputPortName Name for the input port
     * @param validPortName Name for the valid output port
     * @param invalidPortName Name for the invalid output port
     * @param position Canvas position
     * @param description Optional documentation
     * @param validate Type-safe validation function - returns true if valid, false if invalid
     * @return New CodeNode configured as a validator
     *
     * @sample
     * ```kotlin
     * data class EmailData(val address: String)
     *
     * val node = CodeNodeFactory.createValidator<EmailData>(
     *     name = "EmailValidator",
     *     validate = { email -> email.address.contains("@") }
     * )
     * ```
     */
    inline fun <reified T : Any> createValidator(
        name: String,
        inputPortName: String = "input",
        validPortName: String = "valid",
        invalidPortName: String = "invalid",
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline validate: suspend (T) -> Boolean
    ): CodeNode {
        val nodeId = NodeIdGenerator.generateId("codenode")

        // Wrap the type-safe validate function in ProcessingLogic
        val processingLogic = ProcessingLogic { inputs ->
            @Suppress("UNCHECKED_CAST")
            val inputPacket = inputs[inputPortName] as? InformationPacket<T>
                ?: throw IllegalArgumentException("Missing or invalid input packet for port '$inputPortName'")

            // Route to valid or invalid port based on validation result
            val targetPort = if (validate(inputPacket.payload)) validPortName else invalidPortName
            mapOf(targetPort to inputPacket)
        }

        return CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.VALIDATOR,
            description = description,
            position = position,
            inputPorts = listOf(
                PortFactory.input<T>(inputPortName, nodeId, required = true)
            ),
            outputPorts = listOf(
                PortFactory.output<T>(validPortName, nodeId),
                PortFactory.output<T>(invalidPortName, nodeId)
            ),
            processingLogic = processingLogic
        )
    }

    /**
     * Creates a generator CodeNode with no input and one output port
     *
     * @param T Type parameter for output
     * @param name Human-readable name
     * @param outputPortName Name for the output port
     * @param position Canvas position
     * @param description Optional documentation
     * @param generate Type-safe generation function - produces output data
     * @return New CodeNode configured as a generator
     *
     * @sample
     * ```kotlin
     * data class TimestampData(val time: Long)
     *
     * val node = CodeNodeFactory.createGenerator<TimestampData>(
     *     name = "TimestampGenerator",
     *     generate = { TimestampData(Clock.System.now().toEpochMilliseconds()) }
     * )
     * ```
     */
    inline fun <reified T : Any> createGenerator(
        name: String,
        outputPortName: String = "output",
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline generate: suspend () -> T
    ): CodeNode {
        val nodeId = NodeIdGenerator.generateId("codenode")

        // Wrap the type-safe generate function in ProcessingLogic
        val processingLogic = ProcessingLogic {
            val result = generate()
            mapOf(outputPortName to InformationPacketFactory.create(result))
        }

        return CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.GENERATOR,
            description = description,
            position = position,
            inputPorts = emptyList(),
            outputPorts = listOf(
                PortFactory.output<T>(outputPortName, nodeId)
            ),
            processingLogic = processingLogic
        )
    }

    /**
     * Creates a sink CodeNode with one input and no output ports
     *
     * @param T Type parameter for input
     * @param name Human-readable name
     * @param inputPortName Name for the input port
     * @param position Canvas position
     * @param description Optional documentation
     * @param consume Type-safe consumption function - performs side effects with input
     * @return New CodeNode configured as a sink
     *
     * @sample
     * ```kotlin
     * data class LogData(val message: String)
     *
     * val node = CodeNodeFactory.createSink<LogData>(
     *     name = "Logger",
     *     consume = { log -> println(log.message) }
     * )
     * ```
     */
    inline fun <reified T : Any> createSink(
        name: String,
        inputPortName: String = "input",
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline consume: suspend (T) -> Unit
    ): CodeNode {
        val nodeId = NodeIdGenerator.generateId("codenode")

        // Wrap the type-safe consume function in ProcessingLogic
        val processingLogic = ProcessingLogic { inputs ->
            @Suppress("UNCHECKED_CAST")
            val inputPacket = inputs[inputPortName] as? InformationPacket<T>
                ?: throw IllegalArgumentException("Missing or invalid input packet for port '$inputPortName'")

            consume(inputPacket.payload)
            emptyMap() // No outputs
        }

        return CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.SINK,
            description = description,
            position = position,
            inputPorts = listOf(
                PortFactory.input<T>(inputPortName, nodeId, required = true)
            ),
            outputPorts = emptyList(),
            processingLogic = processingLogic
        )
    }

    // ========== Continuous Factory Methods ==========

    /**
     * Creates a continuous generator node that emits values in a loop.
     *
     * The generator runs continuously until stopped, emitting values via the
     * provided emit function. The generator block should check `isActive` to
     * support graceful shutdown.
     *
     * @param T Type of values emitted
     * @param name Human-readable name
     * @param channelCapacity Buffer capacity for output channel (default: BUFFERED = 64)
     * @param position Canvas position
     * @param description Optional documentation
     * @param generate Processing block that receives an emit function
     * @return NodeRuntime configured for continuous generation
     *
     * @sample
     * ```kotlin
     * val timer = CodeNodeFactory.createContinuousGenerator<Int>(
     *     name = "Counter"
     * ) { emit ->
     *     var count = 0
     *     while (isActive) {
     *         delay(1000)
     *         emit(++count)
     *     }
     * }
     *
     * timer.start(scope) { }
     * ```
     */
    inline fun <reified T : Any> createContinuousGenerator(
        name: String,
        channelCapacity: Int = Channel.BUFFERED,
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline generate: ContinuousGeneratorBlock<T>
    ): GeneratorRuntime<T> {
        val nodeId = NodeIdGenerator.generateId("codenode")

        val codeNode = CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.GENERATOR,
            description = description,
            position = position,
            inputPorts = emptyList(),
            outputPorts = listOf(
                PortFactory.output<T>("output", nodeId)
            )
        )

        return GeneratorRuntime(codeNode, channelCapacity, generate)
    }

    /**
     * Creates a continuous sink node that consumes values in a loop.
     *
     * The sink runs continuously until stopped, consuming values from the
     * input channel. The sink handles channel closure gracefully.
     *
     * @param T Type of values consumed
     * @param name Human-readable name
     * @param position Canvas position
     * @param description Optional documentation
     * @param consume Processing block called for each received value
     * @return SinkRuntime configured for continuous consumption
     *
     * @sample
     * ```kotlin
     * val logger = CodeNodeFactory.createContinuousSink<String>(
     *     name = "Logger"
     * ) { message ->
     *     println(message)
     * }
     *
     * logger.inputChannel = sourceChannel
     * logger.start(scope) { }
     * ```
     */
    inline fun <reified T : Any> createContinuousSink(
        name: String,
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline consume: ContinuousSinkBlock<T>
    ): SinkRuntime<T> {
        val nodeId = NodeIdGenerator.generateId("codenode")

        val codeNode = CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.SINK,
            description = description,
            position = position,
            inputPorts = listOf(
                PortFactory.input<T>("input", nodeId, required = true)
            ),
            outputPorts = emptyList()
        )

        return SinkRuntime(codeNode, consume)
    }

    /**
     * Creates a continuous transformer node that transforms values in a loop.
     *
     * The transformer runs continuously until stopped, receiving values from
     * the input channel, applying the transform function, and sending results
     * to the output channel.
     *
     * @param TIn Type of input values
     * @param TOut Type of output values
     * @param name Human-readable name
     * @param position Canvas position
     * @param description Optional documentation
     * @param transform Processing block that transforms input to output
     * @return TransformerRuntime configured for continuous transformation
     *
     * @sample
     * ```kotlin
     * val doubler = CodeNodeFactory.createContinuousTransformer<Int, Int>(
     *     name = "Doubler"
     * ) { value ->
     *     value * 2
     * }
     *
     * doubler.inputChannel = sourceChannel
     * doubler.transformerOutputChannel = destChannel
     * doubler.start(scope) { }
     * ```
     */
    inline fun <reified TIn : Any, reified TOut : Any> createContinuousTransformer(
        name: String,
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline transform: ContinuousTransformBlock<TIn, TOut>
    ): TransformerRuntime<TIn, TOut> {
        val nodeId = NodeIdGenerator.generateId("codenode")

        val codeNode = CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.TRANSFORMER,
            description = description,
            position = position,
            inputPorts = listOf(
                PortFactory.input<TIn>("input", nodeId, required = true)
            ),
            outputPorts = listOf(
                PortFactory.output<TOut>("output", nodeId)
            )
        )

        return TransformerRuntime(codeNode, transform)
    }

    /**
     * Creates a continuous filter node that filters values in a loop.
     *
     * The filter runs continuously until stopped, receiving values from
     * the input channel, applying the predicate, and sending matching values
     * to the output channel. Values that don't match are dropped.
     *
     * @param T Type of values being filtered
     * @param name Human-readable name
     * @param position Canvas position
     * @param description Optional documentation
     * @param predicate Returns true to pass value through, false to drop
     * @return FilterRuntime configured for continuous filtering
     *
     * @sample
     * ```kotlin
     * val evenFilter = CodeNodeFactory.createContinuousFilter<Int>(
     *     name = "EvenFilter"
     * ) { value ->
     *     value % 2 == 0
     * }
     *
     * evenFilter.inputChannel = sourceChannel
     * evenFilter.outputChannel = destChannel
     * evenFilter.start(scope) { }
     * ```
     */
    inline fun <reified T : Any> createContinuousFilter(
        name: String,
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline predicate: ContinuousFilterPredicate<T>
    ): FilterRuntime<T> {
        val nodeId = NodeIdGenerator.generateId("codenode")

        val codeNode = CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.FILTER,
            description = description,
            position = position,
            inputPorts = listOf(
                PortFactory.input<T>("input", nodeId, required = true)
            ),
            outputPorts = listOf(
                PortFactory.output<T>("output", nodeId)
            )
        )

        return FilterRuntime(codeNode, predicate)
    }
}
