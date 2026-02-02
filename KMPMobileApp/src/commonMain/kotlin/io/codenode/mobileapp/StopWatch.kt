package io.codenode.mobileapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Extension function to convert seconds to radians for hand positioning.
 * Subtracts 90 degrees (PI/2) because 0 degrees starts at 3 o'clock position.
 */
private fun Int.secondsToRad(): Float {
    return ((this * 6.0 - 90.0) * (PI / 180.0)).toFloat()
}

/**
 * Data class to hold elapsed time components for the stopwatch.
 */
data class StopWatchTime(
    val minutes: Int = 0,
    val seconds: Int = 0
)

/**
 * StopWatch composable with controls for start/stop and reset.
 */
@Composable
fun StopWatch(
    modifier: Modifier = Modifier,
    minSize: Dp = 200.dp
) {
    var isRunning by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var elapsedMinutes by remember { mutableStateOf(0) }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000)
            elapsedSeconds += 1
            if (elapsedSeconds >= 60) {
                elapsedSeconds = 0
                elapsedMinutes += 1
            }
        }
    }

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

        // Control buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { isRunning = !isRunning },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color.Red else Color.Green
                )
            ) {
                Text(if (isRunning) "Stop" else "Start")
            }

            Button(
                onClick = {
                    isRunning = false
                    elapsedSeconds = 0
                    elapsedMinutes = 0
                },
                enabled = !isRunning && (elapsedSeconds > 0 || elapsedMinutes > 0)
            ) {
                Text("Reset")
            }
        }
    }
}

/**
 * The stopwatch face (analog display).
 */
@Composable
private fun StopWatchFace(
    modifier: Modifier = Modifier,
    minSize: Dp = 200.dp,
    seconds: Int = 0,
    minutes: Int = 0,
    isRunning: Boolean = false
) {
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints {
        val width = if (minWidth < 1.dp) minSize else minWidth
        val height = if (minHeight < 1.dp) minSize else minHeight

        Canvas(
            modifier = modifier.size(width, height)
        ) {
            val radius = size.width * .4f

            // Draw the circle
            drawCircle(
                color = Color.Black,
                style = Stroke(width = radius * .05f),
                radius = radius,
                center = size.center
            )

            val angleDegreeDifference = (360f / 60f)

            // Drawing tick marks
            (1..60).forEach {
                val angleRadDifference =
                    (((angleDegreeDifference * it) - 90f) * (PI / 180f)).toFloat()
                val lineLength = if (it % 5 == 0) radius * .85f else radius * .93f
                val lineColour = if (it % 5 == 0) Color.Black else Color.Gray
                val startOffsetLine = Offset(
                    x = lineLength * cos(angleRadDifference) + size.center.x,
                    y = lineLength * sin(angleRadDifference) + size.center.y
                )
                val endOffsetLine = Offset(
                    x = (radius - ((radius * .05f) / 2)) * cos(angleRadDifference) + size.center.x,
                    y = (radius - ((radius * .05f) / 2)) * sin(angleRadDifference) + size.center.y
                )
                drawLine(
                    color = lineColour,
                    start = startOffsetLine,
                    end = endOffsetLine
                )

                // Draw numbers at 5-second intervals (showing seconds: 5, 10, 15, etc.)
                if (it % 5 == 0) {
                    val positionX = (radius * .75f) * cos(angleRadDifference) + size.center.x
                    val positionY = (radius * .75f) * sin(angleRadDifference) + size.center.y
                    val text = it.toString()
                    val textSize = (radius * .075f).sp

                    val textLayoutResult = textMeasurer.measure(
                        text = text,
                        style = TextStyle(
                            fontSize = textSize,
                            color = Color.Gray
                        )
                    )

                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = positionX - (textLayoutResult.size.width / 2),
                            y = positionY - (textLayoutResult.size.height / 2)
                        )
                    )
                }
            }

            // Draw the center dot
            drawCircle(
                color = Color.Black,
                radius = radius * .02f,
                center = size.center
            )

            // Minutes hand
            val minutesAngle = (seconds / 60.0 * 6.0) - 90.0 + (minutes * 6.0)
            drawLine(
                color = Color.Black,
                start = size.center,
                end = Offset(
                    x = (radius * .55f) * cos((minutesAngle * (PI / 180)).toFloat()) + size.center.x,
                    y = (radius * .55f) * sin((minutesAngle * (PI / 180)).toFloat()) + size.center.y
                ),
                strokeWidth = radius * .02f,
                cap = StrokeCap.Square
            )

            // Seconds hand
            drawLine(
                color = Color.Magenta,
                start = size.center,
                end = Offset(
                    x = (radius * .9f) * cos(seconds.secondsToRad()) + size.center.x,
                    y = (radius * .9f) * sin(seconds.secondsToRad()) + size.center.y
                ),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )

        }
    }
}
