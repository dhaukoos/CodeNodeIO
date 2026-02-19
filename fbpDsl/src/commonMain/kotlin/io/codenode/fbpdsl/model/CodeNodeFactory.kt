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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
     * Creates a generator CodeNode with no input and one output port.
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
    @Deprecated(
        message = "Use createContinuousGenerator for continuous mode operation. " +
            "This method returns a CodeNode for single-invocation patterns. " +
            "For continuous streaming, migrate to: " +
            "CodeNodeFactory.createContinuousGenerator<T>(name) { emit -> while (isActive) { emit(value) } }",
        replaceWith = ReplaceWith(
            "createContinuousGenerator(name, position = position, description = description) { emit -> emit(generate()) }",
            "io.codenode.fbpdsl.model.CodeNodeFactory.createContinuousGenerator"
        ),
        level = DeprecationLevel.WARNING
    )
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
     * Creates a sink CodeNode with one input and no output ports.
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
    @Deprecated(
        message = "Use createContinuousSink for continuous mode operation. " +
            "This method returns a CodeNode for single-invocation patterns. " +
            "For continuous streaming, migrate to: " +
            "CodeNodeFactory.createContinuousSink<T>(name) { value -> consume(value) }",
        replaceWith = ReplaceWith(
            "createContinuousSink(name, position = position, description = description, consume = consume)",
            "io.codenode.fbpdsl.model.CodeNodeFactory.createContinuousSink"
        ),
        level = DeprecationLevel.WARNING
    )
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

    // ========== Multi-Input Processor Factory Methods ==========

    /**
     * Creates a continuous processor node with 2 inputs and 1 output.
     *
     * The processor runs continuously until stopped, receiving values from
     * both input channels, applying the process function, and sending results
     * to the output channel. Uses synchronous receive pattern (waits for both inputs).
     *
     * @param A Type of first input
     * @param B Type of second input
     * @param R Type of output
     * @param name Human-readable name
     * @param position Canvas position
     * @param description Optional documentation
     * @param process Processing function that combines both inputs
     * @return In2Out1Runtime configured for continuous processing
     *
     * @sample
     * ```kotlin
     * val adder = CodeNodeFactory.createIn2Out1Processor<Int, Int, Int>(
     *     name = "Adder"
     * ) { a, b -> a + b }
     *
     * adder.inputChannel = channel1
     * adder.inputChannel2 = channel2
     * adder.outputChannel = outputChannel
     * adder.start(scope) { }
     * ```
     */
    inline fun <reified A : Any, reified B : Any, reified R : Any> createIn2Out1Processor(
        name: String,
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline process: io.codenode.fbpdsl.runtime.In2Out1ProcessBlock<A, B, R>
    ): io.codenode.fbpdsl.runtime.In2Out1Runtime<A, B, R> {
        val nodeId = NodeIdGenerator.generateId("codenode")

        val codeNode = CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.TRANSFORMER,
            description = description,
            position = position,
            inputPorts = listOf(
                PortFactory.input<A>("input1", nodeId, required = true),
                PortFactory.input<B>("input2", nodeId, required = true)
            ),
            outputPorts = listOf(
                PortFactory.output<R>("output", nodeId)
            )
        )

        return io.codenode.fbpdsl.runtime.In2Out1Runtime(codeNode, process)
    }

    /**
     * Creates a continuous processor node with 3 inputs and 1 output.
     *
     * The processor runs continuously until stopped, receiving values from
     * all three input channels, applying the process function, and sending results
     * to the output channel. Uses synchronous receive pattern (waits for all inputs).
     *
     * @param A Type of first input
     * @param B Type of second input
     * @param C Type of third input
     * @param R Type of output
     * @param name Human-readable name
     * @param position Canvas position
     * @param description Optional documentation
     * @param process Processing function that combines all three inputs
     * @return In3Out1Runtime configured for continuous processing
     *
     * @sample
     * ```kotlin
     * val tripleAdder = CodeNodeFactory.createIn3Out1Processor<Int, Int, Int, Int>(
     *     name = "TripleAdder"
     * ) { a, b, c -> a + b + c }
     *
     * tripleAdder.inputChannel = channel1
     * tripleAdder.inputChannel2 = channel2
     * tripleAdder.inputChannel3 = channel3
     * tripleAdder.outputChannel = outputChannel
     * tripleAdder.start(scope) { }
     * ```
     */
    inline fun <reified A : Any, reified B : Any, reified C : Any, reified R : Any> createIn3Out1Processor(
        name: String,
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline process: io.codenode.fbpdsl.runtime.In3Out1ProcessBlock<A, B, C, R>
    ): io.codenode.fbpdsl.runtime.In3Out1Runtime<A, B, C, R> {
        val nodeId = NodeIdGenerator.generateId("codenode")

        val codeNode = CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.TRANSFORMER,
            description = description,
            position = position,
            inputPorts = listOf(
                PortFactory.input<A>("input1", nodeId, required = true),
                PortFactory.input<B>("input2", nodeId, required = true),
                PortFactory.input<C>("input3", nodeId, required = true)
            ),
            outputPorts = listOf(
                PortFactory.output<R>("output", nodeId)
            )
        )

        return io.codenode.fbpdsl.runtime.In3Out1Runtime(codeNode, process)
    }

    // ========== Multi-Input Sink Factory Methods ==========

    /**
     * Creates a continuous sink node with 2 inputs.
     *
     * The sink runs continuously until stopped, receiving values from
     * both input channels (synchronous receive) and consuming them.
     * No output channels.
     *
     * @param A Type of first input
     * @param B Type of second input
     * @param name Human-readable name
     * @param position Canvas position
     * @param description Optional documentation
     * @param consume Sink function that processes both inputs
     * @return In2SinkRuntime configured for continuous consumption
     *
     * @sample
     * ```kotlin
     * val pairLogger = CodeNodeFactory.createIn2Sink<Int, String>(
     *     name = "PairLogger"
     * ) { num, text -> println("$num: $text") }
     *
     * pairLogger.inputChannel = channel1
     * pairLogger.inputChannel2 = channel2
     * pairLogger.start(scope) { }
     * ```
     */
    inline fun <reified A : Any, reified B : Any> createIn2Sink(
        name: String,
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline consume: io.codenode.fbpdsl.runtime.In2SinkBlock<A, B>
    ): io.codenode.fbpdsl.runtime.In2SinkRuntime<A, B> {
        val nodeId = NodeIdGenerator.generateId("codenode")

        val codeNode = CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.SINK,
            description = description,
            position = position,
            inputPorts = listOf(
                PortFactory.input<A>("input1", nodeId, required = true),
                PortFactory.input<B>("input2", nodeId, required = true)
            ),
            outputPorts = emptyList()
        )

        return io.codenode.fbpdsl.runtime.In2SinkRuntime(codeNode, consume)
    }

    /**
     * Creates a continuous sink node with 3 inputs.
     *
     * The sink runs continuously until stopped, receiving values from
     * all three input channels (synchronous receive) and consuming them.
     * No output channels.
     *
     * @param A Type of first input
     * @param B Type of second input
     * @param C Type of third input
     * @param name Human-readable name
     * @param position Canvas position
     * @param description Optional documentation
     * @param consume Sink function that processes all three inputs
     * @return In3SinkRuntime configured for continuous consumption
     *
     * @sample
     * ```kotlin
     * val tripleLogger = CodeNodeFactory.createIn3Sink<Int, Int, Int>(
     *     name = "TripleLogger"
     * ) { a, b, c -> println("Sum: ${a + b + c}") }
     *
     * tripleLogger.inputChannel = channel1
     * tripleLogger.inputChannel2 = channel2
     * tripleLogger.inputChannel3 = channel3
     * tripleLogger.start(scope) { }
     * ```
     */
    inline fun <reified A : Any, reified B : Any, reified C : Any> createIn3Sink(
        name: String,
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline consume: io.codenode.fbpdsl.runtime.In3SinkBlock<A, B, C>
    ): io.codenode.fbpdsl.runtime.In3SinkRuntime<A, B, C> {
        val nodeId = NodeIdGenerator.generateId("codenode")

        val codeNode = CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.SINK,
            description = description,
            position = position,
            inputPorts = listOf(
                PortFactory.input<A>("input1", nodeId, required = true),
                PortFactory.input<B>("input2", nodeId, required = true),
                PortFactory.input<C>("input3", nodeId, required = true)
            ),
            outputPorts = emptyList()
        )

        return io.codenode.fbpdsl.runtime.In3SinkRuntime(codeNode, consume)
    }

    // ========== Timed Generator Factory Methods ==========

    /**
     * Creates a timed 2-output generator node that calls a tick function at a regular interval.
     *
     * The runtime manages the execution loop (delay, pause/resume, stop).
     * The tick function provides only the per-tick business logic.
     *
     * @param U Type of first output
     * @param V Type of second output
     * @param name Human-readable name
     * @param tickIntervalMs Milliseconds between tick invocations
     * @param channelCapacity Buffer capacity for output channels (default: BUFFERED = 64)
     * @param position Canvas position
     * @param description Optional documentation
     * @param tick Function called once per interval, returns values to emit
     * @return Out2GeneratorRuntime configured for timed tick mode
     */
    inline fun <reified U : Any, reified V : Any> createTimedOut2Generator(
        name: String,
        tickIntervalMs: Long,
        channelCapacity: Int = Channel.BUFFERED,
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline tick: io.codenode.fbpdsl.runtime.Out2TickBlock<U, V>
    ): io.codenode.fbpdsl.runtime.Out2GeneratorRuntime<U, V> {
        // Wrap tick in a generate block that manages the timed loop.
        // Note: The generate lambda is compiled in this module. With real dispatchers
        // (production), delay() works correctly. For virtual-time tests, callers
        // should use createOut2Generator with a test-defined loop instead.
        val timedGenerate: io.codenode.fbpdsl.runtime.Out2GeneratorBlock<U, V> = { emit ->
            while (currentCoroutineContext().isActive) {
                delay(tickIntervalMs)
                emit(tick())
            }
        }

        return createOut2Generator(
            name = name,
            channelCapacity = channelCapacity,
            position = position,
            description = description,
            generate = timedGenerate
        )
    }

    // ========== Multi-Output Generator Factory Methods ==========

    /**
     * Creates a continuous generator node with 2 outputs.
     *
     * The generator runs continuously until stopped, emitting ProcessResult2
     * values that are distributed to the two output channels. Non-null values
     * in the ProcessResult2 are sent to their respective channels.
     *
     * @param U Type of first output
     * @param V Type of second output
     * @param name Human-readable name
     * @param channelCapacity Buffer capacity for output channels (default: BUFFERED = 64)
     * @param position Canvas position
     * @param description Optional documentation
     * @param generate Generator function that emits ProcessResult2 values
     * @return Out2GeneratorRuntime configured for continuous generation
     *
     * @sample
     * ```kotlin
     * val pairGenerator = CodeNodeFactory.createOut2Generator<Int, String>(
     *     name = "PairGenerator"
     * ) { emit ->
     *     var count = 0
     *     while (isActive) {
     *         emit(ProcessResult2(++count, "item$count"))
     *     }
     * }
     *
     * val out1 = pairGenerator.outputChannel1!!
     * val out2 = pairGenerator.outputChannel2!!
     * pairGenerator.start(scope) { }
     * ```
     */
    inline fun <reified U : Any, reified V : Any> createOut2Generator(
        name: String,
        channelCapacity: Int = Channel.BUFFERED,
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline generate: io.codenode.fbpdsl.runtime.Out2GeneratorBlock<U, V>
    ): io.codenode.fbpdsl.runtime.Out2GeneratorRuntime<U, V> {
        val nodeId = NodeIdGenerator.generateId("codenode")

        val codeNode = CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.GENERATOR,
            description = description,
            position = position,
            inputPorts = emptyList(),
            outputPorts = listOf(
                PortFactory.output<U>("output1", nodeId),
                PortFactory.output<V>("output2", nodeId)
            )
        )

        return io.codenode.fbpdsl.runtime.Out2GeneratorRuntime(codeNode, channelCapacity, generate)
    }

    /**
     * Creates a continuous generator node with 3 outputs.
     *
     * The generator runs continuously until stopped, emitting ProcessResult3
     * values that are distributed to the three output channels. Non-null values
     * in the ProcessResult3 are sent to their respective channels.
     *
     * @param U Type of first output
     * @param V Type of second output
     * @param W Type of third output
     * @param name Human-readable name
     * @param channelCapacity Buffer capacity for output channels (default: BUFFERED = 64)
     * @param position Canvas position
     * @param description Optional documentation
     * @param generate Generator function that emits ProcessResult3 values
     * @return Out3GeneratorRuntime configured for continuous generation
     *
     * @sample
     * ```kotlin
     * val tripleGenerator = CodeNodeFactory.createOut3Generator<Int, String, Boolean>(
     *     name = "TripleGenerator"
     * ) { emit ->
     *     emit(ProcessResult3(1, "a", true))
     *     emit(ProcessResult3(2, "b", false))
     * }
     *
     * val out1 = tripleGenerator.outputChannel1!!
     * val out2 = tripleGenerator.outputChannel2!!
     * val out3 = tripleGenerator.outputChannel3!!
     * tripleGenerator.start(scope) { }
     * ```
     */
    inline fun <reified U : Any, reified V : Any, reified W : Any> createOut3Generator(
        name: String,
        channelCapacity: Int = Channel.BUFFERED,
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline generate: io.codenode.fbpdsl.runtime.Out3GeneratorBlock<U, V, W>
    ): io.codenode.fbpdsl.runtime.Out3GeneratorRuntime<U, V, W> {
        val nodeId = NodeIdGenerator.generateId("codenode")

        val codeNode = CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.GENERATOR,
            description = description,
            position = position,
            inputPorts = emptyList(),
            outputPorts = listOf(
                PortFactory.output<U>("output1", nodeId),
                PortFactory.output<V>("output2", nodeId),
                PortFactory.output<W>("output3", nodeId)
            )
        )

        return io.codenode.fbpdsl.runtime.Out3GeneratorRuntime(codeNode, channelCapacity, generate)
    }

    // ========== Multi-Output Processor Factory Methods ==========

    /**
     * Creates a continuous processor node with 1 input and 2 outputs.
     *
     * The processor runs continuously until stopped, receiving values from
     * the input channel, applying the process function that returns ProcessResult2,
     * and sending non-null values to respective output channels (selective output).
     *
     * @param A Type of input
     * @param U Type of first output
     * @param V Type of second output
     * @param name Human-readable name
     * @param channelCapacity Buffer capacity for output channels (default: BUFFERED = 64)
     * @param position Canvas position
     * @param description Optional documentation
     * @param process Processing function that produces ProcessResult2
     * @return In1Out2Runtime configured for continuous processing
     */
    inline fun <reified A : Any, reified U : Any, reified V : Any> createIn1Out2Processor(
        name: String,
        channelCapacity: Int = Channel.BUFFERED,
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline process: io.codenode.fbpdsl.runtime.In1Out2ProcessBlock<A, U, V>
    ): io.codenode.fbpdsl.runtime.In1Out2Runtime<A, U, V> {
        val nodeId = NodeIdGenerator.generateId("codenode")

        val codeNode = CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.TRANSFORMER,
            description = description,
            position = position,
            inputPorts = listOf(
                PortFactory.input<A>("input", nodeId, required = true)
            ),
            outputPorts = listOf(
                PortFactory.output<U>("output1", nodeId),
                PortFactory.output<V>("output2", nodeId)
            )
        )

        return io.codenode.fbpdsl.runtime.In1Out2Runtime(codeNode, channelCapacity, process)
    }

    /**
     * Creates a continuous processor node with 1 input and 3 outputs.
     *
     * The processor runs continuously until stopped, receiving values from
     * the input channel, applying the process function that returns ProcessResult3,
     * and sending non-null values to respective output channels (selective output).
     *
     * @param A Type of input
     * @param U Type of first output
     * @param V Type of second output
     * @param W Type of third output
     * @param name Human-readable name
     * @param channelCapacity Buffer capacity for output channels (default: BUFFERED = 64)
     * @param position Canvas position
     * @param description Optional documentation
     * @param process Processing function that produces ProcessResult3
     * @return In1Out3Runtime configured for continuous processing
     */
    inline fun <reified A : Any, reified U : Any, reified V : Any, reified W : Any> createIn1Out3Processor(
        name: String,
        channelCapacity: Int = Channel.BUFFERED,
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline process: io.codenode.fbpdsl.runtime.In1Out3ProcessBlock<A, U, V, W>
    ): io.codenode.fbpdsl.runtime.In1Out3Runtime<A, U, V, W> {
        val nodeId = NodeIdGenerator.generateId("codenode")

        val codeNode = CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.TRANSFORMER,
            description = description,
            position = position,
            inputPorts = listOf(
                PortFactory.input<A>("input", nodeId, required = true)
            ),
            outputPorts = listOf(
                PortFactory.output<U>("output1", nodeId),
                PortFactory.output<V>("output2", nodeId),
                PortFactory.output<W>("output3", nodeId)
            )
        )

        return io.codenode.fbpdsl.runtime.In1Out3Runtime(codeNode, channelCapacity, process)
    }

    /**
     * Creates a continuous processor node with 2 inputs and 2 outputs.
     *
     * The processor runs continuously until stopped, receiving values from
     * both input channels (synchronous receive), applying the process function
     * that returns ProcessResult2, and sending non-null values to respective
     * output channels (selective output).
     *
     * @param A Type of first input
     * @param B Type of second input
     * @param U Type of first output
     * @param V Type of second output
     * @param name Human-readable name
     * @param channelCapacity Buffer capacity for output channels (default: BUFFERED = 64)
     * @param position Canvas position
     * @param description Optional documentation
     * @param process Processing function that produces ProcessResult2
     * @return In2Out2Runtime configured for continuous processing
     */
    inline fun <reified A : Any, reified B : Any, reified U : Any, reified V : Any> createIn2Out2Processor(
        name: String,
        channelCapacity: Int = Channel.BUFFERED,
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline process: io.codenode.fbpdsl.runtime.In2Out2ProcessBlock<A, B, U, V>
    ): io.codenode.fbpdsl.runtime.In2Out2Runtime<A, B, U, V> {
        val nodeId = NodeIdGenerator.generateId("codenode")

        val codeNode = CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.TRANSFORMER,
            description = description,
            position = position,
            inputPorts = listOf(
                PortFactory.input<A>("input1", nodeId, required = true),
                PortFactory.input<B>("input2", nodeId, required = true)
            ),
            outputPorts = listOf(
                PortFactory.output<U>("output1", nodeId),
                PortFactory.output<V>("output2", nodeId)
            )
        )

        return io.codenode.fbpdsl.runtime.In2Out2Runtime(codeNode, channelCapacity, process)
    }

    /**
     * Creates a continuous processor node with 2 inputs and 3 outputs.
     *
     * The processor runs continuously until stopped, receiving values from
     * both input channels (synchronous receive), applying the process function
     * that returns ProcessResult3, and sending non-null values to respective
     * output channels (selective output).
     *
     * @param A Type of first input
     * @param B Type of second input
     * @param U Type of first output
     * @param V Type of second output
     * @param W Type of third output
     * @param name Human-readable name
     * @param channelCapacity Buffer capacity for output channels (default: BUFFERED = 64)
     * @param position Canvas position
     * @param description Optional documentation
     * @param process Processing function that produces ProcessResult3
     * @return In2Out3Runtime configured for continuous processing
     */
    inline fun <reified A : Any, reified B : Any, reified U : Any, reified V : Any, reified W : Any> createIn2Out3Processor(
        name: String,
        channelCapacity: Int = Channel.BUFFERED,
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline process: io.codenode.fbpdsl.runtime.In2Out3ProcessBlock<A, B, U, V, W>
    ): io.codenode.fbpdsl.runtime.In2Out3Runtime<A, B, U, V, W> {
        val nodeId = NodeIdGenerator.generateId("codenode")

        val codeNode = CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.TRANSFORMER,
            description = description,
            position = position,
            inputPorts = listOf(
                PortFactory.input<A>("input1", nodeId, required = true),
                PortFactory.input<B>("input2", nodeId, required = true)
            ),
            outputPorts = listOf(
                PortFactory.output<U>("output1", nodeId),
                PortFactory.output<V>("output2", nodeId),
                PortFactory.output<W>("output3", nodeId)
            )
        )

        return io.codenode.fbpdsl.runtime.In2Out3Runtime(codeNode, channelCapacity, process)
    }

    /**
     * Creates a continuous processor node with 3 inputs and 2 outputs.
     *
     * The processor runs continuously until stopped, receiving values from
     * all three input channels (synchronous receive), applying the process function
     * that returns ProcessResult2, and sending non-null values to respective
     * output channels (selective output).
     *
     * @param A Type of first input
     * @param B Type of second input
     * @param C Type of third input
     * @param U Type of first output
     * @param V Type of second output
     * @param name Human-readable name
     * @param channelCapacity Buffer capacity for output channels (default: BUFFERED = 64)
     * @param position Canvas position
     * @param description Optional documentation
     * @param process Processing function that produces ProcessResult2
     * @return In3Out2Runtime configured for continuous processing
     */
    inline fun <reified A : Any, reified B : Any, reified C : Any, reified U : Any, reified V : Any> createIn3Out2Processor(
        name: String,
        channelCapacity: Int = Channel.BUFFERED,
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline process: io.codenode.fbpdsl.runtime.In3Out2ProcessBlock<A, B, C, U, V>
    ): io.codenode.fbpdsl.runtime.In3Out2Runtime<A, B, C, U, V> {
        val nodeId = NodeIdGenerator.generateId("codenode")

        val codeNode = CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.TRANSFORMER,
            description = description,
            position = position,
            inputPorts = listOf(
                PortFactory.input<A>("input1", nodeId, required = true),
                PortFactory.input<B>("input2", nodeId, required = true),
                PortFactory.input<C>("input3", nodeId, required = true)
            ),
            outputPorts = listOf(
                PortFactory.output<U>("output1", nodeId),
                PortFactory.output<V>("output2", nodeId)
            )
        )

        return io.codenode.fbpdsl.runtime.In3Out2Runtime(codeNode, channelCapacity, process)
    }

    /**
     * Creates a continuous processor node with 3 inputs and 3 outputs.
     *
     * The processor runs continuously until stopped, receiving values from
     * all three input channels (synchronous receive), applying the process function
     * that returns ProcessResult3, and sending non-null values to respective
     * output channels (selective output).
     *
     * @param A Type of first input
     * @param B Type of second input
     * @param C Type of third input
     * @param U Type of first output
     * @param V Type of second output
     * @param W Type of third output
     * @param name Human-readable name
     * @param channelCapacity Buffer capacity for output channels (default: BUFFERED = 64)
     * @param position Canvas position
     * @param description Optional documentation
     * @param process Processing function that produces ProcessResult3
     * @return In3Out3Runtime configured for continuous processing
     */
    inline fun <reified A : Any, reified B : Any, reified C : Any, reified U : Any, reified V : Any, reified W : Any> createIn3Out3Processor(
        name: String,
        channelCapacity: Int = Channel.BUFFERED,
        position: Node.Position = Node.Position.ORIGIN,
        description: String? = null,
        noinline process: io.codenode.fbpdsl.runtime.In3Out3ProcessBlock<A, B, C, U, V, W>
    ): io.codenode.fbpdsl.runtime.In3Out3Runtime<A, B, C, U, V, W> {
        val nodeId = NodeIdGenerator.generateId("codenode")

        val codeNode = CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.TRANSFORMER,
            description = description,
            position = position,
            inputPorts = listOf(
                PortFactory.input<A>("input1", nodeId, required = true),
                PortFactory.input<B>("input2", nodeId, required = true),
                PortFactory.input<C>("input3", nodeId, required = true)
            ),
            outputPorts = listOf(
                PortFactory.output<U>("output1", nodeId),
                PortFactory.output<V>("output2", nodeId),
                PortFactory.output<W>("output3", nodeId)
            )
        )

        return io.codenode.fbpdsl.runtime.In3Out3Runtime(codeNode, channelCapacity, process)
    }
}
