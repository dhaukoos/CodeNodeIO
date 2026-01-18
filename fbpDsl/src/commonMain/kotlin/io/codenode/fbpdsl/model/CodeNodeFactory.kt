/*
 * CodeNodeFactory - Factory for creating CodeNode instances
 * Provides convenience methods for common CodeNode patterns
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

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
     * @param processingLogic Optional reference to code template
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
        processingLogic: String? = null,
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
     * @param name Human-readable name
     * @param inputPortName Name for the input port
     * @param outputPortName Name for the output port
     * @param position Canvas position
     * @param processingLogic Optional reference to code template
     * @param description Optional documentation
     * @return New CodeNode configured as a transformer
     */
    inline fun <reified TIn : Any, reified TOut : Any> createTransformer(
        name: String,
        inputPortName: String = "input",
        outputPortName: String = "output",
        position: Node.Position = Node.Position.ORIGIN,
        processingLogic: String? = null,
        description: String? = null
    ): CodeNode {
        val nodeId = NodeIdGenerator.generateId("codenode")
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
     * @param name Human-readable name
     * @param inputPortName Name for the input port
     * @param outputPortName Name for the output port (passed items)
     * @param position Canvas position
     * @param processingLogic Optional reference to code template
     * @param description Optional documentation
     * @return New CodeNode configured as a filter
     */
    inline fun <reified T : Any> createFilter(
        name: String,
        inputPortName: String = "input",
        outputPortName: String = "output",
        position: Node.Position = Node.Position.ORIGIN,
        processingLogic: String? = null,
        description: String? = null
    ): CodeNode {
        val nodeId = NodeIdGenerator.generateId("codenode")
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
     * @param name Human-readable name
     * @param inputPortName Name for the input port
     * @param outputPortNames Names for the output ports
     * @param position Canvas position
     * @param processingLogic Optional reference to code template
     * @param description Optional documentation
     * @return New CodeNode configured as a splitter
     */
    inline fun <reified T : Any> createSplitter(
        name: String,
        inputPortName: String = "input",
        outputPortNames: List<String> = listOf("output1", "output2"),
        position: Node.Position = Node.Position.ORIGIN,
        processingLogic: String? = null,
        description: String? = null
    ): CodeNode {
        val nodeId = NodeIdGenerator.generateId("codenode")
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
     * @param name Human-readable name
     * @param inputPortNames Names for the input ports
     * @param outputPortName Name for the output port
     * @param position Canvas position
     * @param processingLogic Optional reference to code template
     * @param description Optional documentation
     * @return New CodeNode configured as a merger
     */
    inline fun <reified T : Any> createMerger(
        name: String,
        inputPortNames: List<String> = listOf("input1", "input2"),
        outputPortName: String = "output",
        position: Node.Position = Node.Position.ORIGIN,
        processingLogic: String? = null,
        description: String? = null
    ): CodeNode {
        val nodeId = NodeIdGenerator.generateId("codenode")
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
     * @param name Human-readable name
     * @param inputPortName Name for the input port
     * @param validPortName Name for the valid output port
     * @param invalidPortName Name for the invalid output port
     * @param position Canvas position
     * @param processingLogic Optional reference to code template
     * @param description Optional documentation
     * @return New CodeNode configured as a validator
     */
    inline fun <reified T : Any> createValidator(
        name: String,
        inputPortName: String = "input",
        validPortName: String = "valid",
        invalidPortName: String = "invalid",
        position: Node.Position = Node.Position.ORIGIN,
        processingLogic: String? = null,
        description: String? = null
    ): CodeNode {
        val nodeId = NodeIdGenerator.generateId("codenode")
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
     * @param name Human-readable name
     * @param outputPortName Name for the output port
     * @param position Canvas position
     * @param processingLogic Optional reference to code template
     * @param description Optional documentation
     * @return New CodeNode configured as a generator
     */
    inline fun <reified T : Any> createGenerator(
        name: String,
        outputPortName: String = "output",
        position: Node.Position = Node.Position.ORIGIN,
        processingLogic: String? = null,
        description: String? = null
    ): CodeNode {
        val nodeId = NodeIdGenerator.generateId("codenode")
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
     * @param name Human-readable name
     * @param inputPortName Name for the input port
     * @param position Canvas position
     * @param processingLogic Optional reference to code template
     * @param description Optional documentation
     * @return New CodeNode configured as a sink
     */
    inline fun <reified T : Any> createSink(
        name: String,
        inputPortName: String = "input",
        position: Node.Position = Node.Position.ORIGIN,
        processingLogic: String? = null,
        description: String? = null
    ): CodeNode {
        val nodeId = NodeIdGenerator.generateId("codenode")
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
}
