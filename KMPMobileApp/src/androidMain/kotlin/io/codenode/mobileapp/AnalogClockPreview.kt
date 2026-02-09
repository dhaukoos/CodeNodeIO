package io.codenode.mobileapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.codenode.generated.stopwatch.StopWatchController

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
    val flowGraph = remember { createStopWatchFlowGraph() }
    val controller = remember(flowGraph) { StopWatchController(flowGraph) }
    StopWatch(
        controller = controller,
        minSize = 200.dp
    )
}
