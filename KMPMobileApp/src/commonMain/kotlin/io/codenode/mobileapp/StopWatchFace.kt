/*
 * StopWatchFace - Analog stopwatch face composable
 * Extracted from StopWatch.kt for T038-T039
 * License: Apache 2.0
 */

package io.codenode.mobileapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
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
 * The stopwatch face (analog display).
 *
 * Renders an analog clock face with:
 * - Circle outline
 * - Tick marks at each second (larger at 5-second intervals)
 * - Numbers at 5-second positions
 * - Minutes hand (shorter, black)
 * - Seconds hand (longer, magenta)
 *
 * @param modifier Modifier for the composable
 * @param minSize Minimum size for the clock face
 * @param seconds Current elapsed seconds (0-59)
 * @param minutes Current elapsed minutes
 * @param isRunning Whether the stopwatch is currently running
 */
@Composable
fun StopWatchFace(
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
