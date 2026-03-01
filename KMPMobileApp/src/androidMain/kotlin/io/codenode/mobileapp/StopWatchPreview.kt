package io.codenode.mobileapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.codenode.stopwatch.generated.StopWatchControllerAdapter
import io.codenode.stopwatch.StopWatchViewModel
import io.codenode.stopwatch.generated.StopWatchController
import io.codenode.stopwatch.stopWatchFlowGraph
import io.codenode.stopwatch.userInterface.StopWatchScreen

@Preview
@Composable
private fun StopWatchPreview() {
    val controller = remember { StopWatchController(stopWatchFlowGraph) }
    val viewModel = remember { StopWatchViewModel(StopWatchControllerAdapter(controller)) }
    StopWatchScreen(
        viewModel = viewModel,
        minSize = 200.dp
    )
}
