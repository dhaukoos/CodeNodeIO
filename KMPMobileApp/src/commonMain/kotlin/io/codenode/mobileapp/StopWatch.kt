/*
 * StopWatch - Stopwatch composable using StopWatchViewModel
 * Uses ViewModel pattern to bridge FlowGraph domain logic with Compose UI.
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
import io.codenode.stopwatch.generated.StopWatchViewModel

/**
 * Data class to hold elapsed time components for the stopwatch.
 */
data class StopWatchTime(
    val minutes: Int = 0,
    val seconds: Int = 0
)

/**
 * StopWatch composable using ViewModel pattern.
 *
 * This composable demonstrates the integration between:
 * - UI composable (StopWatchFace)
 * - ViewModel (StopWatchViewModel)
 * - FlowGraph domain logic (via StopWatchController)
 *
 * The ViewModel manages:
 * - State observation via StateFlow (seconds, minutes, executionState)
 * - Action delegation (start, stop, reset)
 *
 * @param viewModel The StopWatchViewModel instance
 * @param modifier Modifier for the composable
 * @param minSize Minimum size for the clock face
 */
@Composable
fun StopWatch(
    viewModel: StopWatchViewModel,
    modifier: Modifier = Modifier,
    minSize: Dp = 200.dp
) {
    // Collect state from ViewModel's StateFlow properties
    val executionState by viewModel.executionState.collectAsState()
    val seconds by viewModel.seconds.collectAsState()
    val minutes by viewModel.minutes.collectAsState()

    // Derive state flags from executionState
    val isRunning = executionState == ExecutionState.RUNNING
    val isPaused = executionState == ExecutionState.PAUSED
    val isIdle = executionState == ExecutionState.IDLE

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StopWatchFace(
            minSize = minSize,
            seconds = seconds,
            minutes = minutes,
            isRunning = isRunning
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Digital time display
        val minutesStr = minutes.toString().padStart(2, '0')
        val secondsStr = seconds.toString().padStart(2, '0')
        Text(
            text = "$minutesStr:$secondsStr",
            style = TextStyle(fontSize = 24.sp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Control buttons - delegate to ViewModel
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Start button - visible when IDLE or after stop
            if (isIdle) {
                Button(
                    onClick = { viewModel.start() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Green
                    )
                ) {
                    Text("Start")
                }
            }

            // Stop button - visible when RUNNING
            if (isRunning) {
                Button(
                    onClick = { viewModel.stop() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Stop")
                }
            }

            // Pause button - visible when RUNNING
            if (isRunning) {
                Button(
                    onClick = { viewModel.pause() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFA500) // Orange
                    )
                ) {
                    Text("Pause")
                }
            }

            // Resume button - visible when PAUSED
            if (isPaused) {
                Button(
                    onClick = { viewModel.resume() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Green
                    )
                ) {
                    Text("Resume")
                }
            }

            // Stop button also visible when PAUSED (to fully stop from paused state)
            if (isPaused) {
                Button(
                    onClick = { viewModel.stop() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Stop")
                }
            }

            // Reset button - enabled when IDLE or PAUSED with elapsed time
            Button(
                onClick = { viewModel.reset() },
                enabled = (isIdle || isPaused) && (seconds > 0 || minutes > 0)
            ) {
                Text("Reset")
            }
        }
    }
}
