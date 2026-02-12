/*
 * StopWatch - Stopwatch composable using generated StopWatchController
 * Uses the StopWatch.flow virtual circuit via generated StopWatchController.
 * Works on all KMP platforms (Android, iOS, Desktop).
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
import io.codenode.stopwatch.generated.StopWatchController

/**
 * Data class to hold elapsed time components for the stopwatch.
 */
data class StopWatchTime(
    val minutes: Int = 0,
    val seconds: Int = 0
)

/**
 * StopWatch composable using the generated StopWatchController.
 *
 * This composable demonstrates the integration between:
 * - UI composable (StopWatchFace)
 * - Generated controller (StopWatchController from StopWatch.flow)
 * - FBP model (FlowGraph, ExecutionState)
 *
 * The controller manages:
 * - Execution state (IDLE, RUNNING, PAUSED)
 * - Elapsed time tracking (seconds, minutes)
 * - State transitions (start, stop, pause, reset)
 *
 * @param controller The StopWatchController instance to use
 * @param modifier Modifier for the composable
 * @param minSize Minimum size for the clock face
 */
@Composable
fun StopWatch(
    controller: StopWatchController,
    modifier: Modifier = Modifier,
    minSize: Dp = 200.dp
) {
    // Collect state from controller's StateFlow properties
    val executionState by controller.executionState.collectAsState()
    val elapsedSeconds by controller.elapsedSeconds.collectAsState()
    val elapsedMinutes by controller.elapsedMinutes.collectAsState()

    // Derive isRunning from executionState
    val isRunning = executionState == ExecutionState.RUNNING

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
