/*
 * ControlledStopWatch - Android-specific StopWatch using generated StopWatchController
 * T040: Imports StopWatchController from generated module
 * T041: Uses executionState from controller instead of local isRunning state
 * License: Apache 2.0
 */

package io.codenode.mobileapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.generated.stopwatch.StopWatchController

/**
 * Android-specific StopWatch composable that uses the generated StopWatchController.
 *
 * This composable demonstrates the integration between:
 * - UI composable (StopWatchFace)
 * - Generated controller (StopWatchController)
 * - FBP model (FlowGraph, ExecutionState)
 *
 * The controller manages:
 * - Execution state (IDLE, RUNNING, PAUSED)
 * - Elapsed time tracking (seconds, minutes)
 * - State transitions (start, stop, pause, reset)
 *
 * @param flowGraph The FlowGraph definition for the stopwatch
 * @param modifier Modifier for the composable
 * @param minSize Minimum size for the clock face
 */
@Composable
fun ControlledStopWatch(
    flowGraph: FlowGraph,
    modifier: Modifier = Modifier,
    minSize: Dp = 200.dp
) {
    // Create controller instance - remember it to survive recomposition
    val controller = remember(flowGraph) { StopWatchController(flowGraph) }

    // Collect state from controller's StateFlow properties
    val executionState by controller.executionState.collectAsState()
    val elapsedSeconds by controller.elapsedSeconds.collectAsState()
    val elapsedMinutes by controller.elapsedMinutes.collectAsState()

    // T041: Derive isRunning from executionState
    val isRunning = executionState == ExecutionState.RUNNING

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Reuse the extracted StopWatchFace from commonMain
        StopWatchFace(
            minSize = minSize,
            seconds = elapsedSeconds,
            minutes = elapsedMinutes,
            isRunning = isRunning
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Digital time display
        val minutesStr = elapsedMinutes.toString().padStart(2, '0')
        val secondsStr = elapsedSeconds.toString().padStart(2, '0')
        Text(
            text = "$minutesStr:$secondsStr",
            style = TextStyle(fontSize = 24.sp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Control buttons - delegate to controller
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    if (isRunning) {
                        controller.stop()
                    } else {
                        controller.start()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color.Red else Color.Green
                )
            ) {
                Text(if (isRunning) "Stop" else "Start")
            }

            Button(
                onClick = {
                    controller.reset()
                },
                enabled = !isRunning && (elapsedSeconds > 0 || elapsedMinutes > 0)
            ) {
                Text("Reset")
            }
        }
    }
}
