/*
 * GroupContextMenu - Context Menu for Group/Ungroup Operations
 * Provides UI for grouping selected nodes into GraphNode or ungrouping
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.codenode.grapheditor.state.GroupContextMenuState

/**
 * Context menu for grouping and ungrouping nodes.
 * Appears on right-click when nodes are selected.
 *
 * @param state The current menu state containing position and selection info
 * @param onGroup Called when "Group" is selected
 * @param onUngroup Called when "Ungroup" is selected
 * @param onDismiss Called when the menu should close
 */
@Composable
fun GroupContextMenu(
    state: GroupContextMenuState,
    onGroup: () -> Unit,
    onUngroup: () -> Unit,
    onDismiss: () -> Unit
) {
    // Don't render if no options available
    if (!state.hasOptions) {
        return
    }

    Popup(
        offset = IntOffset(state.position.x.toInt(), state.position.y.toInt()),
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnClickOutside = true
        ),
        onKeyEvent = { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                onDismiss()
                true
            } else {
                false
            }
        }
    ) {
        Card(
            modifier = Modifier
                .width(180.dp),
            elevation = 8.dp,
            shape = RoundedCornerShape(4.dp),
            backgroundColor = Color.White
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                // Group option
                if (state.showGroupOption) {
                    ContextMenuItem(
                        icon = Icons.Default.Layers,
                        text = "Group (${state.selectedNodeIds.size} nodes)",
                        onClick = {
                            onGroup()
                            onDismiss()
                        }
                    )
                }

                // Divider between options
                if (state.showGroupOption && state.showUngroupOption) {
                    Divider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = Color(0xFFE0E0E0)
                    )
                }

                // Ungroup option
                if (state.showUngroupOption) {
                    ContextMenuItem(
                        icon = Icons.Default.FolderOpen,
                        text = "Ungroup",
                        onClick = {
                            onUngroup()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Individual menu item with icon and text.
 */
@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF616161),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color(0xFF212121)
        )
    }
}
