/*
 * StopWatchPreviewProvider - Provides StopWatch preview composable for the runtime panel
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.circuitsimulator.RuntimeSession
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.stopwatch.userInterface.StopWatchFace

/**
 * Provides a preview composable that renders the StopWatch face and digital
 * time display, driven by the RuntimeSession's ViewModel state.
 *
 * Renders only the visual output (clock face + digital time) without control
 * buttons, since the RuntimePreviewPanel provides its own execution controls.
 */
object StopWatchPreviewProvider {

    /**
     * Renders the StopWatch preview: analog clock face + digital MM:SS display.
     *
     * @param runtimeSession The RuntimeSession providing the ViewModel state
     * @param modifier Modifier for the preview container
     */
    @Composable
    fun Preview(
        runtimeSession: RuntimeSession,
        modifier: Modifier = Modifier
    ) {
        val viewModel = runtimeSession.viewModel
        val seconds by viewModel.seconds.collectAsState()
        val minutes by viewModel.minutes.collectAsState()
        val executionState by viewModel.executionState.collectAsState()
        val isRunning = executionState == ExecutionState.RUNNING

        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StopWatchFace(
                minSize = 200.dp,
                seconds = seconds,
                minutes = minutes,
                isRunning = isRunning
            )

            Spacer(modifier = Modifier.height(8.dp))

            val minutesStr = minutes.toString().padStart(2, '0')
            val secondsStr = seconds.toString().padStart(2, '0')
            Text(
                text = "$minutesStr:$secondsStr",
                style = TextStyle(fontSize = 24.sp)
            )
        }
    }
}
