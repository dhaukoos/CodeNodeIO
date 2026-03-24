/*
 * ForecastChart - Compose Canvas line chart for daily high temperatures
 * License: Apache 2.0
 */

package io.codenode.weatherforecast.userInterface

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.weatherforecast.models.ChartData
import kotlin.math.roundToInt

/**
 * Draws a line chart of daily high temperatures using Compose Canvas.
 *
 * Features:
 * - Line connecting data points with filled circles at each point
 * - X-axis date labels
 * - Y-axis temperature labels with unit
 * - Grid lines for readability
 *
 * @param chartData The chart data containing labels, values, unit, and value range
 * @param modifier Modifier for layout
 */
@Composable
fun ForecastChart(
    chartData: ChartData,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val lineColor = Color(0xFFE53935) // Red for high temps
    val gridColor = Color(0xFFE0E0E0)
    val pointColor = Color(0xFFB71C1C)
    val labelColor = Color(0xFF616161)
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(start = 4.dp, end = 4.dp)
    ) {
        if (chartData.values.isEmpty()) return@Canvas

        val leftPadding = 44f
        val rightPadding = 12f
        val topPadding = 16f
        val bottomPadding = 28f

        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding

        val dataMin = chartData.minValue
        val dataMax = chartData.maxValue
        val range = (dataMax - dataMin).coerceAtLeast(1.0)
        // Add 10% padding above and below data range
        val yMin = dataMin - range * 0.1
        val yMax = dataMax + range * 0.1
        val yRange = yMax - yMin

        val pointCount = chartData.values.size
        val xStep = if (pointCount > 1) chartWidth / (pointCount - 1) else chartWidth

        // Helper to map data value to canvas Y
        fun valueToY(value: Double): Float {
            return (topPadding + chartHeight * (1.0 - (value - yMin) / yRange)).toFloat()
        }

        // Helper to map index to canvas X
        fun indexToX(index: Int): Float {
            return leftPadding + index * xStep
        }

        // Draw horizontal grid lines (3 lines)
        for (i in 0..3) {
            val gridValue = yMin + yRange * i / 3.0
            val y = valueToY(gridValue)
            drawLine(
                color = gridColor,
                start = Offset(leftPadding, y),
                end = Offset(leftPadding + chartWidth, y),
                strokeWidth = 1f
            )
            // Y-axis label
            val labelText = "${gridValue.roundToInt()}${chartData.unit}"
            val measured = textMeasurer.measure(labelText, labelStyle)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(0f, y - measured.size.height / 2f)
            )
        }

        // Build path and draw line
        val path = Path()
        chartData.values.forEachIndexed { index, value ->
            val x = indexToX(index)
            val y = valueToY(value)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )

        // Draw data points and X-axis labels
        chartData.values.forEachIndexed { index, value ->
            val x = indexToX(index)
            val y = valueToY(value)

            // Data point
            drawCircle(
                color = pointColor,
                radius = 4f,
                center = Offset(x, y)
            )

            // X-axis date label
            if (index < chartData.labels.size) {
                val label = chartData.labels[index]
                val measured = textMeasurer.measure(label, labelStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(x - measured.size.width / 2f, size.height - bottomPadding + 4f)
                )
            }
        }
    }
}
