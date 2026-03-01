/*
 * StopWatchPreviewProvider - Provides StopWatch preview composables for the runtime panel
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.codenode.circuitsimulator.RuntimeSession
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.stopwatchv2.userInterface.StopWatchV2
import io.codenode.stopwatchv2.userInterface.StopWatchV2Screen

/**
 * Provides preview composables that render StopWatch components,
 * driven by the RuntimeSession's ViewModel state.
 */
object StopWatchPreviewProvider {

    /**
     * Renders the StopWatch preview: analog clock face + digital MM:SS display.
     * Uses the StopWatch composable directly (no control buttons).
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

        StopWatchV2(
            modifier = modifier,
            minSize = 200.dp,
            seconds = seconds,
            minutes = minutes,
            isRunning = isRunning
        )
    }

    /**
     * Renders the full StopWatchScreen preview: clock face, digital time, and control buttons.
     *
     * @param runtimeSession The RuntimeSession providing the ViewModel state
     * @param modifier Modifier for the preview container
     */
    @Composable
    fun ScreenPreview(
        runtimeSession: RuntimeSession,
        modifier: Modifier = Modifier
    ) {
        StopWatchV2Screen(
            viewModel = runtimeSession.viewModel,
            modifier = modifier,
            minSize = 200.dp
        )
    }
}
