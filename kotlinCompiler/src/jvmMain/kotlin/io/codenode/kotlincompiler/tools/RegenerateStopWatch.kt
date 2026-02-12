/*
 * RegenerateStopWatch Tool
 * Regenerates StopWatch module generated files using the latest ModuleGenerator
 *
 * Run with: ./gradlew :kotlinCompiler:run -PmainClass=io.codenode.kotlincompiler.tools.RegenerateStopWatchKt
 * Or from IDE: Run this file's main function
 *
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.tools

import io.codenode.fbpdsl.model.*
import io.codenode.kotlincompiler.generator.ModuleGenerator
import java.io.File

/**
 * Creates a StopWatch FlowGraph matching the existing module structure.
 */
private fun createStopWatchFlowGraph(): FlowGraph {
    val timerEmitterId = "node_1770875159380_2079"
    val displayReceiverId = "node_1770875159414_5237"

    val timerEmitter = CodeNode(
        id = timerEmitterId,
        name = "TimerEmitter",
        codeNodeType = CodeNodeType.GENERATOR,
        position = Node.Position(100.0, 100.0),
        inputPorts = emptyList(),
        outputPorts = listOf(
            Port(
                id = "port_1770875159390_3123_elapsedSeconds",
                name = "elapsedSeconds",
                direction = Port.Direction.OUTPUT,
                dataType = Any::class,
                owningNodeId = timerEmitterId
            ),
            Port(
                id = "port_1770875159406_3495_elapsedMinutes",
                name = "elapsedMinutes",
                direction = Port.Direction.OUTPUT,
                dataType = Any::class,
                owningNodeId = timerEmitterId
            )
        ),
        configuration = mapOf(
            "speedAttenuation" to "1000",
            "_useCaseClass" to "io.codenode.stopwatch.usecases.TimerEmitterComponent"
        )
    )

    val displayReceiver = CodeNode(
        id = displayReceiverId,
        name = "DisplayReceiver",
        codeNodeType = CodeNodeType.SINK,
        position = Node.Position(400.0, 100.0),
        inputPorts = listOf(
            Port(
                id = "port_1770875159414_670_seconds",
                name = "seconds",
                direction = Port.Direction.INPUT,
                dataType = Any::class,
                owningNodeId = displayReceiverId
            ),
            Port(
                id = "port_1770875159414_9992_minutes",
                name = "minutes",
                direction = Port.Direction.INPUT,
                dataType = Any::class,
                owningNodeId = displayReceiverId
            )
        ),
        outputPorts = emptyList(),
        configuration = mapOf(
            "_useCaseClass" to "io.codenode.stopwatch.usecases.DisplayReceiverComponent"
        )
    )

    val connections = listOf(
        Connection(
            id = "conn_1770875159420_3821",
            sourceNodeId = timerEmitterId,
            sourcePortId = "port_1770875159390_3123_elapsedSeconds",
            targetNodeId = displayReceiverId,
            targetPortId = "port_1770875159414_670_seconds",
            channelCapacity = 0
        ),
        Connection(
            id = "conn_1770875159422_6258",
            sourceNodeId = timerEmitterId,
            sourcePortId = "port_1770875159406_3495_elapsedMinutes",
            targetNodeId = displayReceiverId,
            targetPortId = "port_1770875159414_9992_minutes",
            channelCapacity = 0
        )
    )

    return FlowGraph(
        id = "graph_1770875159422_279723",
        name = "StopWatch",
        version = "1.0.0",
        description = "Virtual circuit demo for stopwatch functionality",
        rootNodes = listOf(timerEmitter, displayReceiver),
        connections = connections,
        targetPlatforms = listOf(
            FlowGraph.TargetPlatform.KMP_ANDROID,
            FlowGraph.TargetPlatform.KMP_IOS,
            FlowGraph.TargetPlatform.KMP_DESKTOP
        )
    )
}

/**
 * Main entry point for regenerating StopWatch module files.
 */
fun main() {
    val flowGraph = createStopWatchFlowGraph()
    val generator = ModuleGenerator()
    val packageName = "io.codenode.stopwatch.generated"
    val usecasesPackage = "io.codenode.stopwatch.usecases"

    // Find project root
    val currentDir = File(System.getProperty("user.dir"))
    val projectRoot = if (currentDir.name == "kotlinCompiler") currentDir.parentFile else currentDir
    val stopWatchDir = File(projectRoot, "StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated")

    if (!stopWatchDir.exists()) {
        println("Error: StopWatch generated directory not found: ${stopWatchDir.absolutePath}")
        return
    }

    println("Regenerating StopWatch module files...")
    println("Output directory: ${stopWatchDir.absolutePath}")
    println()

    // Generate Controller class
    val controllerContent = generator.generateControllerClass(flowGraph, packageName)
    val controllerFile = File(stopWatchDir, "StopWatchController.kt")
    controllerFile.writeText(controllerContent)
    println("Generated: ${controllerFile.name}")

    // Generate Flow class with usecases imports
    var flowContent = generator.generateFlowGraphClass(flowGraph, packageName)
    // Add usecases imports
    flowContent = flowContent.replaceFirst(
        "package $packageName\n",
        """package $packageName

import $usecasesPackage.TimerEmitterComponent
import $usecasesPackage.DisplayReceiverComponent
import $usecasesPackage.TimerOutput
"""
    )
    val flowFile = File(stopWatchDir, "StopWatchFlow.kt")
    flowFile.writeText(flowContent)
    println("Generated: ${flowFile.name}")

    println()
    println("Regeneration complete!")
}
