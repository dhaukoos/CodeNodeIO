package io.codenode.mobileapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.codenode.stopwatch.generated.StopWatchControllerAdapter
import io.codenode.stopwatch.generated.StopWatchViewModel
import io.codenode.stopwatch.generated.StopWatchController
import io.codenode.stopwatch.stopWatchFlowGraph

@Preview
@Composable
private fun AnalogClockPreview() {
    AnalogClock(
        minSize = 200.dp,
        initialTime = ClockTime(hour = 10, minute = 10, second = 30),
        isClockRunning = false
    )
}

@Preview
@Composable
private fun StopWatchPreview() {
    val controller = remember { StopWatchController(stopWatchFlowGraph) }
    val viewModel = remember { StopWatchViewModel(StopWatchControllerAdapter(controller)) }
    StopWatch(
        viewModel = viewModel,
        minSize = 200.dp
    )
}
