package io.codenode.mobileapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Extension function to convert seconds to radians for clock hand positioning.
 * Subtracts 90 degrees (PI/2) because 0 degrees starts at 3 o'clock position.
 */
private fun Int.secondsToRad(): Float {
    return ((this * 6.0 - 90.0) * (PI / 180.0)).toFloat()
}

/**
 * Data class to hold time components for the clock.
 */
data class ClockTime(
    val hour: Int,
    val minute: Int,
    val second: Int
)

/**
 * Gets the current time using kotlinx-datetime (multiplatform).
 */
private fun getCurrentTime(): ClockTime {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return ClockTime(
        hour = now.hour,
        minute = now.minute,
        second = now.second
    )
}

@Composable
fun AnalogClock(
    modifier: Modifier = Modifier,
    minSize: Dp = 64.dp,
    initialTime: ClockTime = getCurrentTime(),
    isClockRunning: Boolean = true
) {
    val textMeasurer = rememberTextMeasurer()

    var seconds by remember { mutableStateOf(initialTime.second) }
    var minutes by remember { mutableStateOf(initialTime.minute) }
    var hours by remember { mutableStateOf(initialTime.hour) }

    var hourAngle by remember {
        mutableStateOf(0.0)
    }

    LaunchedEffect(key1 = minutes) {
        // Calculate hour angle when minutes change
        hourAngle = (minutes / 60.0 * 30.0) - 90.0 + (hours * 30)
    }

    LaunchedEffect(isClockRunning) {
        while (isClockRunning) {
            seconds += 1
            if (seconds > 60) {
                seconds = 1
                minutes++
            }
            if (minutes > 60) {
                minutes = 1
                hours++
            }
            delay(1000)
        }
    }

    BoxWithConstraints {
        // This is the estate we are going to work with
        val width = if (minWidth < 1.dp) minSize else minWidth
        val height = if (minHeight < 1.dp) minSize else minHeight

        Canvas(
            modifier = modifier
                .size(width, height)
        ) {
            // Calculate the radius (40% of width for responsiveness)
            val radius = size.width * .4f

            // Draw the circle
            drawCircle(
                color = Color.Black,
                style = Stroke(width = radius * .05f),
                radius = radius,
                center = size.center
            )

            // The degree difference between each 'minute' line
            val angleDegreeDifference = (360f / 60f)

            // Drawing all lines
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

                // Draw hour numbers at 5-minute intervals
                if (it % 5 == 0) {
                    val positionX = (radius * .75f) * cos(angleRadDifference) + size.center.x
                    val positionY = (radius * .75f) * sin(angleRadDifference) + size.center.y
                    val text = (it / 5).toString()
                    val textSize = (radius * .15f).sp

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

            // Hour hand
            drawLine(
                color = Color.Black,
                start = size.center,
                end = Offset(
                    x = (radius * .55f) * cos((hourAngle * (PI / 180)).toFloat()) + size.center.x,
                    y = (radius * .55f) * sin((hourAngle * (PI / 180)).toFloat()) + size.center.y
                ),
                strokeWidth = radius * .02f,
                cap = StrokeCap.Square
            )

            // Minutes hand
            val minutesAngle = (seconds / 60.0 * 6.0) - 90.0 + (minutes * 6.0)
            drawLine(
                color = Color.Black,
                start = size.center,
                end = Offset(
                    x = (radius * .7f) * cos((minutesAngle * (PI / 180)).toFloat()) + size.center.x,
                    y = (radius * .7f) * sin((minutesAngle * (PI / 180)).toFloat()) + size.center.y
                ),
                strokeWidth = radius * .01f,
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

            // Paused text
            if (!isClockRunning) {
                val pausedText = "PAUSED"
                val pausedTextSize = (radius * .15f).sp

                val pausedLayoutResult = textMeasurer.measure(
                    text = pausedText,
                    style = TextStyle(
                        fontSize = pausedTextSize,
                        color = Color.Magenta
                    )
                )

                drawText(
                    textLayoutResult = pausedLayoutResult,
                    topLeft = Offset(
                        x = size.center.x - (pausedLayoutResult.size.width / 2),
                        y = size.center.y - (pausedLayoutResult.size.height / 2)
                    )
                )
            }
        }
    }
}
