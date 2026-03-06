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
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.stopwatch.StopWatchViewModel
import io.codenode.stopwatch.userInterface.StopWatch
import io.codenode.stopwatch.userInterface.StopWatchScreen

/**
 * Provides preview composables that render StopWatch components,
 * driven by the RuntimeSession's ViewModel state.
 */
object StopWatchPreviewProvider {

    /**
     * Registers StopWatch preview composables with the PreviewRegistry.
     */
    fun register() {
        PreviewRegistry.register("StopWatch") { viewModel, modifier ->
            val vm = viewModel as StopWatchViewModel
            val seconds by vm.seconds.collectAsState()
            val minutes by vm.minutes.collectAsState()
            val executionState by vm.executionState.collectAsState()
            val isRunning = executionState == ExecutionState.RUNNING

            StopWatch(
                modifier = modifier,
                minSize = 200.dp,
                seconds = seconds,
                minutes = minutes,
                isRunning = isRunning
            )
        }

        PreviewRegistry.register("StopWatchScreen") { viewModel, modifier ->
            val vm = viewModel as StopWatchViewModel
            StopWatchScreen(
                viewModel = vm,
                modifier = modifier,
                minSize = 200.dp
            )
        }
    }
}
