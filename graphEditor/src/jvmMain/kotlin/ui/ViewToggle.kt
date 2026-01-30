/*
 * ViewToggle - View Mode Switcher Component
 * UI component for toggling between visual and textual graph representations
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.codenode.fbpdsl.model.FlowGraph

/**
 * View mode enumeration
 */
enum class ViewMode {
    VISUAL,
    TEXTUAL,
    SPLIT
}

/**
 * View toggle button component
 *
 * @param currentMode The current active view mode
 * @param onModeChanged Callback when view mode changes
 * @param modifier Modifier for the toggle component
 */
@Composable
fun ViewToggle(
    currentMode: ViewMode,
    onModeChanged: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colors.surface)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "View:",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface
        )

        // Visual view button
        ToggleButton(
            text = "Visual",
            selected = currentMode == ViewMode.VISUAL,
            onClick = { onModeChanged(ViewMode.VISUAL) }
        )

        // Textual view button
        ToggleButton(
            text = "Textual",
            selected = currentMode == ViewMode.TEXTUAL,
            onClick = { onModeChanged(ViewMode.TEXTUAL) }
        )

        // Split view button
        ToggleButton(
            text = "Split",
            selected = currentMode == ViewMode.SPLIT,
            onClick = { onModeChanged(ViewMode.SPLIT) }
        )
    }
}

/**
 * Individual toggle button
 */
@Composable
private fun ToggleButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (selected) {
                MaterialTheme.colors.primary
            } else {
                MaterialTheme.colors.surface
            },
            contentColor = if (selected) {
                MaterialTheme.colors.onPrimary
            } else {
                MaterialTheme.colors.onSurface
            }
        ),
        elevation = if (selected) {
            ButtonDefaults.elevation(defaultElevation = 4.dp)
        } else {
            ButtonDefaults.elevation(defaultElevation = 0.dp)
        }
    ) {
        Text(text = text, style = MaterialTheme.typography.button)
    }
}

/**
 * Composite view component that displays the appropriate view based on mode
 *
 * @param flowGraph The flow graph to display
 * @param viewMode The current view mode
 * @param onVisualViewContent Composable content for visual view
 * @param modifier Modifier for the view container
 * @param overrideText Optional text to display in textual view instead of generated DSL
 * @param overrideTitle Optional title for textual view when showing override text
 */
@Composable
fun GraphViewContainer(
    flowGraph: FlowGraph,
    viewMode: ViewMode,
    onVisualViewContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overrideText: String? = null,
    overrideTitle: String? = null
) {
    when (viewMode) {
        ViewMode.VISUAL -> {
            Box(modifier = modifier.fillMaxSize()) {
                onVisualViewContent()
            }
        }

        ViewMode.TEXTUAL -> {
            TextualView(
                flowGraph = flowGraph,
                modifier = modifier.fillMaxSize(),
                overrideText = overrideText
            )
        }

        ViewMode.SPLIT -> {
            Row(modifier = modifier.fillMaxSize()) {
                // Visual view on the left
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    onVisualViewContent()
                }

                // Divider
                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                )

                // Textual view on the right
                CompactTextualView(
                    flowGraph = flowGraph,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    overrideText = overrideText,
                    overrideTitle = overrideTitle
                )
            }
        }
    }
}

/**
 * Stateful view toggle with integrated view container
 *
 * @param flowGraph The flow graph to display
 * @param initialMode Initial view mode (defaults to Visual)
 * @param onVisualViewContent Composable content for visual view
 * @param modifier Modifier for the component
 * @param overrideText Optional text to display in textual view instead of generated DSL
 * @param overrideTitle Optional title for textual view when showing override text
 */
@Composable
fun GraphEditorWithToggle(
    flowGraph: FlowGraph,
    initialMode: ViewMode = ViewMode.VISUAL,
    onVisualViewContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overrideText: String? = null,
    overrideTitle: String? = null
) {
    var currentMode by remember { mutableStateOf(initialMode) }

    Column(modifier = modifier.fillMaxSize()) {
        // Toggle controls at the top
        ViewToggle(
            currentMode = currentMode,
            onModeChanged = { newMode -> currentMode = newMode },
            modifier = Modifier.fillMaxWidth()
        )

        // Divider
        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
        )

        // View content
        GraphViewContainer(
            flowGraph = flowGraph,
            viewMode = currentMode,
            onVisualViewContent = onVisualViewContent,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            overrideText = overrideText,
            overrideTitle = overrideTitle
        )
    }
}
