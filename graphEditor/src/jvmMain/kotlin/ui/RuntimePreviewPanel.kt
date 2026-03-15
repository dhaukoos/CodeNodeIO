/*
 * RuntimePreviewPanel - Collapsible panel for runtime execution controls and preview
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.circuitsimulator.RuntimeSession
import io.codenode.fbpdsl.model.ExecutionState
import java.io.File

/**
 * Collapsible right-side panel for runtime execution controls.
 *
 * Provides:
 * - Execution state indicator
 * - Start/Stop toggle button and Pause/Resume toggle button
 * - Collapsible speed attenuation section with slider and animate toggle
 * - Preview composable dropdown (discovers composables from loaded module)
 * - Preview area rendering the selected composable
 *
 * @param runtimeSession The RuntimeSession orchestrator
 * @param isExpanded Whether the panel is currently expanded
 * @param onToggle Callback to toggle panel visibility
 * @param moduleRootDir The root directory of the currently loaded module (null if none)
 * @param flowGraphName The name of the current FlowGraph (used for default selection)
 * @param modifier Modifier for the panel container
 */
@Composable
fun RuntimePreviewPanel(
    runtimeSession: RuntimeSession?,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    moduleRootDir: File? = null,
    flowGraphName: String = "",
    animateDataFlow: Boolean = false,
    onAnimateDataFlowChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val executionState = runtimeSession?.executionState?.collectAsState()?.value ?: ExecutionState.IDLE
    val attenuationMs = runtimeSession?.attenuationDelayMs?.collectAsState()?.value ?: 0L

    // Discover composables from the loaded module
    val composables = remember(moduleRootDir) {
        moduleRootDir?.let { discoverComposables(it) } ?: emptyList()
    }

    // Selected composable state — default to flowGraphName match
    var selectedComposable by remember(composables, flowGraphName) {
        val default = composables.firstOrNull { it == flowGraphName }
        mutableStateOf(default)
    }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Collapsible state for Speed Attenuation section
    var attenuationExpanded by remember { mutableStateOf(false) }

    CollapsiblePanel(
        isExpanded = isExpanded,
        onToggle = onToggle,
        side = PanelSide.RIGHT,
        modifier = modifier
    ) {
            Column(
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Scrollable controls section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
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

                // Control buttons — Start/Stop toggle + Pause/Resume toggle
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Start/Stop toggle button
                    val isRunningOrPaused = executionState == ExecutionState.RUNNING || executionState == ExecutionState.PAUSED
                    if (isRunningOrPaused) {
                        // Show Stop
                        Button(
                            onClick = { runtimeSession?.stop() },
                            enabled = runtimeSession != null,
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
                    } else {
                        // Show Start
                        Button(
                            onClick = { runtimeSession?.start() },
                            enabled = runtimeSession != null && executionState == ExecutionState.IDLE,
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
                    }

                    // Pause/Resume toggle button
                    if (executionState == ExecutionState.PAUSED) {
                        // Show Resume
                        Button(
                            onClick = { runtimeSession?.resume() },
                            enabled = runtimeSession != null,
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
                    } else {
                        // Show Pause
                        Button(
                            onClick = { runtimeSession?.pause() },
                            enabled = runtimeSession != null && executionState == ExecutionState.RUNNING,
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
                }

                Divider()

                // Speed Attenuation — collapsible section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { attenuationExpanded = !attenuationExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (attenuationExpanded) "\u25BC" else "\u25B6",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Speed Attenuation",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                }

                if (attenuationExpanded) {
                    Text(
                        text = "Delay: ${attenuationMs}ms",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Slider(
                        value = attenuationMs.toFloat(),
                        onValueChange = { value ->
                            // Snap to nearest 200ms interval
                            val snapped = (value / 200f).toLong() * 200L
                            runtimeSession?.setAttenuation(snapped)
                        },
                        enabled = runtimeSession != null,
                        valueRange = 0f..2000f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0ms", fontSize = 10.sp, color = Color.Gray)
                        Text("2000ms", fontSize = 10.sp, color = Color.Gray)
                    }

                    // Animate Data Flow toggle
                    val animationEnabled = runtimeSession != null && attenuationMs >= 200L
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Animate Data Flow", fontSize = 11.sp)
                            Switch(
                                checked = animateDataFlow,
                                onCheckedChange = { onAnimateDataFlowChanged(it) },
                                enabled = animationEnabled
                            )
                        }
                        if (!animationEnabled) {
                            Text(
                                text = "Requires \u2265200ms attenuation",
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Divider()

                // Preview composable selector
                if (moduleRootDir == null) {
                    Text(
                        text = "No module loaded",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                } else if (composables.isEmpty()) {
                    Text(
                        text = "No composables found",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                } else {
                    Text(
                        text = "Preview Composable",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Box {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedComposable ?: "Select...",
                                modifier = Modifier.weight(1f),
                                fontSize = 12.sp
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select composable",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            composables.forEach { name ->
                                DropdownMenuItem(
                                    onClick = {
                                        selectedComposable = name
                                        dropdownExpanded = false
                                    }
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 12.sp,
                                        fontWeight = if (name == selectedComposable) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                } // end scrollable controls section

                // Preview area - renders selected composable (non-scrollable, fills remaining space)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (runtimeSession == null) {
                        Text(
                            text = "No runtime available for this module",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    } else if (moduleRootDir == null || composables.isEmpty()) {
                        Text(
                            text = "No preview available",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    } else {
                        val previewFn = selectedComposable?.let { PreviewRegistry.get(it) }
                        if (previewFn != null) {
                            previewFn(runtimeSession.viewModel, Modifier)
                        } else if (selectedComposable == null) {
                            Text(
                                text = "Select a composable to preview",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        } else {
                            Text(
                                text = "Preview not available for: $selectedComposable",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
