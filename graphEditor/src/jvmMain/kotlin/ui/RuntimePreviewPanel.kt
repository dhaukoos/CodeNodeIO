/*
 * RuntimePreviewPanel - Collapsible panel for runtime execution controls and preview
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.circuitsimulator.RuntimeSession
import io.codenode.fbpdsl.model.ExecutionState

/**
 * Collapsible right-side panel for runtime execution controls.
 *
 * Provides:
 * - Execution state indicator
 * - Start/Stop/Pause/Resume buttons with contextual enable/disable
 * - Attenuation slider (0ms to 5000ms)
 * - Preview area placeholder (for US2)
 *
 * @param runtimeSession The RuntimeSession orchestrator
 * @param isExpanded Whether the panel is currently expanded
 * @param onToggle Callback to toggle panel visibility
 * @param modifier Modifier for the panel container
 */
@Composable
fun RuntimePreviewPanel(
    runtimeSession: RuntimeSession,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val executionState by runtimeSession.executionState.collectAsState()
    val attenuationMs by runtimeSession.attenuationDelayMs.collectAsState()

    Row(modifier = modifier) {
        // Vertical divider + toggle strip (always visible)
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(20.dp)
                .clickable(onClick = onToggle)
                .background(MaterialTheme.colors.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                contentDescription = if (isExpanded) "Collapse runtime panel" else "Expand runtime panel",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }

        // Panel content (only when expanded)
        if (isExpanded) {
            Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

            Column(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Text(
                    text = "Runtime Preview",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Divider()

                // Execution State indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("State:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    val (stateText, stateColor) = when (executionState) {
                        ExecutionState.IDLE -> "Idle" to Color.Gray
                        ExecutionState.RUNNING -> "Running" to Color(0xFF4CAF50)
                        ExecutionState.PAUSED -> "Paused" to Color(0xFFFF9800)
                        ExecutionState.ERROR -> "Error" to Color.Red
                    }
                    Text(
                        text = stateText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = stateColor
                    )
                }

                // Control buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Start button - only when Idle
                    Button(
                        onClick = { runtimeSession.start() },
                        enabled = executionState == ExecutionState.IDLE,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF4CAF50)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Start",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(2.dp))
                        Text("Start", fontSize = 11.sp, color = Color.White)
                    }

                    // Pause button - only when Running
                    Button(
                        onClick = { runtimeSession.pause() },
                        enabled = executionState == ExecutionState.RUNNING,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFFF9800)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Pause,
                            contentDescription = "Pause",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(2.dp))
                        Text("Pause", fontSize = 11.sp, color = Color.White)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Resume button - only when Paused
                    Button(
                        onClick = { runtimeSession.resume() },
                        enabled = executionState == ExecutionState.PAUSED,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF2196F3)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(2.dp))
                        Text("Resume", fontSize = 11.sp, color = Color.White)
                    }

                    // Stop button - when Running or Paused
                    Button(
                        onClick = { runtimeSession.stop() },
                        enabled = executionState == ExecutionState.RUNNING || executionState == ExecutionState.PAUSED,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFF44336)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(2.dp))
                        Text("Stop", fontSize = 11.sp, color = Color.White)
                    }
                }

                Divider()

                // Attenuation slider
                Text(
                    text = "Speed Attenuation",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "Delay: ${attenuationMs}ms",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Slider(
                    value = attenuationMs.toFloat(),
                    onValueChange = { value ->
                        // Snap to nearest 100ms interval
                        val snapped = (value / 100f).toLong() * 100L
                        runtimeSession.setAttenuation(snapped)
                    },
                    valueRange = 0f..5000f,
                    steps = 49,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0ms", fontSize = 10.sp, color = Color.Gray)
                    Text("5000ms", fontSize = 10.sp, color = Color.Gray)
                }

                Divider()

                // Preview area placeholder (US2 will add StopWatch composable here)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No preview available",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
