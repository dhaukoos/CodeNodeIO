package io.codenode.mobileapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.codenode.stopwatchv2.generated.StopWatchV2ControllerAdapter
import io.codenode.stopwatchv2.generated.StopWatchV2ViewModel
import io.codenode.stopwatchv2.generated.StopWatchV2Controller
import io.codenode.stopwatchv2.stopWatchV2FlowGraph
import io.codenode.stopwatchv2.userInterface.StopWatchV2Screen

@Preview
@Composable
private fun StopWatchPreview() {
    val controller = remember { StopWatchV2Controller(stopWatchV2FlowGraph) }
    val viewModel = remember { StopWatchV2ViewModel(StopWatchV2ControllerAdapter(controller)) }
    StopWatchV2Screen(
        viewModel = viewModel,
        minSize = 200.dp
    )
}
