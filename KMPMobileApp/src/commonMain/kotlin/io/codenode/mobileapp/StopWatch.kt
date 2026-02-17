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
import io.codenode.mobileapp.viewmodel.StopWatchViewModel

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
 * - State observation via StateFlow (elapsedSeconds, elapsedMinutes, executionState)
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
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val elapsedMinutes by viewModel.elapsedMinutes.collectAsState()

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

        // Control buttons - delegate to ViewModel
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    if (isRunning) {
                        viewModel.stop()
                    } else {
                        viewModel.start()
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
                    viewModel.reset()
                },
                enabled = !isRunning && (elapsedSeconds > 0 || elapsedMinutes > 0)
            ) {
                Text("Reset")
            }
        }
    }
}
