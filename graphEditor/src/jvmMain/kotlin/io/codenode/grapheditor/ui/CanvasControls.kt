/*
 * CanvasControls - Zoom and Pan Controls for Canvas
 * Provides UI controls for canvas navigation and manipulation
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.grapheditor.state.GraphState

/**
 * Canvas control panel with zoom and pan controls
 *
 * @param graphState The graph state to control
 * @param modifier Modifier for the control panel
 * @param showZoomLevel Whether to display the current zoom level
 * @param onResetView Callback when reset view is clicked
 * @param onFitToScreen Callback when fit to screen is clicked
 */
@Composable
fun CanvasControls(
    graphState: GraphState,
    modifier: Modifier = Modifier,
    showZoomLevel: Boolean = true,
    onResetView: () -> Unit = {},
    onFitToScreen: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Zoom in button
        IconButton(
            onClick = {
                graphState.zoom(1.2f)
            },
            modifier = Modifier.size(40.dp)
        ) {
            Text(
                text = "+",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
        }

        // Zoom level display
        if (showZoomLevel) {
            Text(
                text = "${(graphState.scale * 100).toInt()}%",
                fontSize = 12.sp,
                color = Color(0xFF424242)
            )
        }

        // Zoom out button
        IconButton(
            onClick = {
                graphState.zoom(0.8f)
            },
            modifier = Modifier.size(40.dp)
        ) {
            Text(
                text = "-",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
        }

        Divider(color = Color(0xFFE0E0E0))

        // Reset view button
        IconButton(
            onClick = {
                graphState.resetViewport()
                onResetView()
            },
            modifier = Modifier.size(40.dp)
        ) {
            Text(
                text = "⊙",
                fontSize = 20.sp,
                color = Color(0xFF757575)
            )
        }

        // Fit to screen button
        IconButton(
            onClick = onFitToScreen,
            modifier = Modifier.size(40.dp)
        ) {
            Text(
                text = "⛶",
                fontSize = 20.sp,
                color = Color(0xFF757575)
            )
        }
    }
}

/**
 * Compact canvas controls (horizontal layout)
 *
 * @param graphState The graph state to control
 * @param modifier Modifier for the control panel
 * @param showZoomLevel Whether to display the current zoom level
 */
@Composable
fun CompactCanvasControls(
    graphState: GraphState,
    modifier: Modifier = Modifier,
    showZoomLevel: Boolean = true
) {
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Zoom out button
        IconButton(
            onClick = { graphState.zoom(0.8f) },
            modifier = Modifier.size(32.dp)
        ) {
            Text(
                text = "-",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )
        }

        // Zoom level
        if (showZoomLevel) {
            Text(
                text = "${(graphState.scale * 100).toInt()}%",
                fontSize = 11.sp,
                color = Color(0xFF424242),
                modifier = Modifier.widthIn(min = 40.dp)
            )
        }

        // Zoom in button
        IconButton(
            onClick = { graphState.zoom(1.2f) },
            modifier = Modifier.size(32.dp)
        ) {
            Text(
                text = "+",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )
        }

        Divider(
            modifier = Modifier
                .width(1.dp)
                .height(24.dp),
            color = Color(0xFFE0E0E0)
        )

        // Reset view button
        IconButton(
            onClick = { graphState.resetViewport() },
            modifier = Modifier.size(32.dp)
        ) {
            Text(
                text = "⊙",
                fontSize = 18.sp,
                color = Color(0xFF424242)
            )
        }
    }
}

/**
 * Minimap control showing overview of the graph
 *
 * @param graphState The graph state
 * @param modifier Modifier for the minimap
 * @param size Size of the minimap
 */
@Composable
fun MinimapControl(
    graphState: GraphState,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 150.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        // Minimap implementation would go here
        // For now, just show a placeholder
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Minimap",
                fontSize = 10.sp,
                color = Color(0xFF9E9E9E)
            )
        }
    }
}

/**
 * Zoom slider control
 *
 * @param graphState The graph state
 * @param modifier Modifier for the slider
 * @param minZoom Minimum zoom level (default: 0.1f = 10%)
 * @param maxZoom Maximum zoom level (default: 5.0f = 500%)
 */
@Composable
fun ZoomSlider(
    graphState: GraphState,
    modifier: Modifier = Modifier,
    minZoom: Float = 0.1f,
    maxZoom: Float = 5.0f
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Zoom: ${(graphState.scale * 100).toInt()}%",
            fontSize = 12.sp,
            color = Color(0xFF424242)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = graphState.scale,
            onValueChange = { graphState.updateScale(it) },
            valueRange = minZoom..maxZoom,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF2196F3),
                activeTrackColor = Color(0xFF2196F3),
                inactiveTrackColor = Color(0xFFE0E0E0)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${(minZoom * 100).toInt()}%",
                fontSize = 10.sp,
                color = Color(0xFF9E9E9E)
            )
            Text(
                text = "${(maxZoom * 100).toInt()}%",
                fontSize = 10.sp,
                color = Color(0xFF9E9E9E)
            )
        }
    }
}

/**
 * Pan controls (directional buttons)
 *
 * @param graphState The graph state
 * @param modifier Modifier for the control panel
 * @param panAmount Amount to pan in pixels (default: 50)
 */
@Composable
fun PanControls(
    graphState: GraphState,
    modifier: Modifier = Modifier,
    panAmount: Float = 50f
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Up button
        IconButton(
            onClick = {
                graphState.pan(androidx.compose.ui.geometry.Offset(0f, panAmount))
            }
        ) {
            Text(
                text = "↑",
                fontSize = 20.sp,
                color = Color(0xFF757575)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left button
            IconButton(
                onClick = {
                    graphState.pan(androidx.compose.ui.geometry.Offset(panAmount, 0f))
                }
            ) {
                Text(
                    text = "←",
                    fontSize = 20.sp,
                    color = Color(0xFF757575)
                )
            }

            // Center/reset button
            IconButton(
                onClick = {
                    graphState.updatePanOffset(androidx.compose.ui.geometry.Offset.Zero)
                }
            ) {
                Text(
                    text = "⊙",
                    fontSize = 20.sp,
                    color = Color(0xFF757575)
                )
            }

            // Right button
            IconButton(
                onClick = {
                    graphState.pan(androidx.compose.ui.geometry.Offset(-panAmount, 0f))
                }
            ) {
                Text(
                    text = "→",
                    fontSize = 20.sp,
                    color = Color(0xFF757575)
                )
            }
        }

        // Down button
        IconButton(
            onClick = {
                graphState.pan(androidx.compose.ui.geometry.Offset(0f, -panAmount))
            }
        ) {
            Text(
                text = "↓",
                fontSize = 20.sp,
                color = Color(0xFF757575)
            )
        }
    }
}

/**
 * Combined canvas control panel with all controls
 *
 * @param graphState The graph state
 * @param modifier Modifier for the panel
 * @param showMinimap Whether to show the minimap
 * @param showPanControls Whether to show pan controls
 */
@Composable
fun FullCanvasControlPanel(
    graphState: GraphState,
    modifier: Modifier = Modifier,
    showMinimap: Boolean = false,
    showPanControls: Boolean = false
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Zoom controls
        CanvasControls(graphState)

        // Pan controls
        if (showPanControls) {
            PanControls(graphState)
        }

        // Minimap
        if (showMinimap) {
            MinimapControl(graphState)
        }
    }
}
