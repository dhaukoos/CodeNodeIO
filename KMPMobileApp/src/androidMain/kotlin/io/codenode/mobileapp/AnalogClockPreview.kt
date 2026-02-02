package io.codenode.mobileapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
private fun AnalogClockPreview() {
    AnalogClock(
        minSize = 200.dp,
        initialTime = ClockTime(hour = 10, minute = 10, second = 30),
        isClockRunning = false
    )
}
