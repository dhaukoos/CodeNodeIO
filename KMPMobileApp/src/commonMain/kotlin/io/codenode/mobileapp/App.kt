/*
 * Main composable entry point for KMPMobileApp
 * Shared UI code for Android and iOS
 * Uses the StopWatch.flow virtual circuit via generated StopWatchController
 */
package io.codenode.mobileapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.codenode.fbpdsl.model.*
import io.codenode.generated.stopwatch.StopWatchController

/**
 * Creates the StopWatch FlowGraph definition.
 *
 * This defines the virtual circuit with:
 * - TimerEmitter: Generates elapsed time signals
 * - DisplayReceiver: Receives time signals for display
 * - Connections: Links emitter outputs to receiver inputs
 */
fun createStopWatchFlowGraph(): FlowGraph {
    val timerEmitterId = "timer-emitter"
    val displayReceiverId = "display-receiver"

    val timerEmitter = CodeNode(
        id = timerEmitterId,
        name = "TimerEmitter",
        codeNodeType = CodeNodeType.GENERATOR,
        position = Node.Position(100.0, 100.0),
        inputPorts = emptyList(),
        outputPorts = listOf(
            Port(
                id = "${timerEmitterId}_elapsedSeconds",
                name = "elapsedSeconds",
                direction = Port.Direction.OUTPUT,
                dataType = Int::class,
                owningNodeId = timerEmitterId
            ),
            Port(
                id = "${timerEmitterId}_elapsedMinutes",
                name = "elapsedMinutes",
                direction = Port.Direction.OUTPUT,
                dataType = Int::class,
                owningNodeId = timerEmitterId
            )
        ),
        controlConfig = ControlConfig(speedAttenuation = 1000L)
    )

    val displayReceiver = CodeNode(
        id = displayReceiverId,
        name = "DisplayReceiver",
        codeNodeType = CodeNodeType.SINK,
        position = Node.Position(400.0, 100.0),
        inputPorts = listOf(
            Port(
                id = "${displayReceiverId}_seconds",
                name = "seconds",
                direction = Port.Direction.INPUT,
                dataType = Int::class,
                owningNodeId = displayReceiverId
            ),
            Port(
                id = "${displayReceiverId}_minutes",
                name = "minutes",
                direction = Port.Direction.INPUT,
                dataType = Int::class,
                owningNodeId = displayReceiverId
            )
        ),
        outputPorts = emptyList()
    )

    val connections = listOf(
        Connection(
            id = "conn_seconds",
            sourceNodeId = timerEmitterId,
            sourcePortId = "${timerEmitterId}_elapsedSeconds",
            targetNodeId = displayReceiverId,
            targetPortId = "${displayReceiverId}_seconds",
            channelCapacity = 1
        ),
        Connection(
            id = "conn_minutes",
            sourceNodeId = timerEmitterId,
            sourcePortId = "${timerEmitterId}_elapsedMinutes",
            targetNodeId = displayReceiverId,
            targetPortId = "${displayReceiverId}_minutes",
            channelCapacity = 1
        )
    )

    return FlowGraph(
        id = "stopwatch-flow",
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
 * Main application composable.
 * This is the entry point for the shared UI.
 */
@Composable
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainContent()
        }
    }
}

/**
 * Main content composable displaying a greeting and stopwatch.
 * Creates the StopWatchController from the FlowGraph definition.
 */
@Composable
fun MainContent() {
    // Create FlowGraph and Controller - remember to survive recomposition
    val flowGraph = remember { createStopWatchFlowGraph() }
    val controller = remember(flowGraph) { StopWatchController(flowGraph) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "CodeNodeIO Mobile",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = greet(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp)
        )
        StopWatch(
            controller = controller,
            modifier = Modifier.padding(top = 32.dp),
            minSize = 400.dp
        )
    }
}
