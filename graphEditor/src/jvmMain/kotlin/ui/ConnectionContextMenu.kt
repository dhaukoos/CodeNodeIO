/*
 * ConnectionContextMenu - Context Menu for Connection IP Type Selection
 * Displays a dropdown menu for changing a connection's IP type
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.codenode.fbpdsl.model.InformationPacketType

/**
 * Data class representing the state of a connection context menu.
 * This is used to track when and where to show the context menu.
 *
 * @property connectionId The ID of the connection being modified
 * @property position Screen position where the menu should appear
 * @property currentTypeId The currently assigned IP type ID (null if unassigned)
 */
data class ConnectionContextMenuState(
    val connectionId: String,
    val position: Offset,
    val currentTypeId: String?
)

/**
 * Context menu for changing a connection's IP type.
 * Displays a list of available IP types with color indicators.
 *
 * @param connectionId ID of the connection being modified
 * @param position Screen position for menu placement
 * @param ipTypes List of available IP types to choose from
 * @param currentTypeId Currently selected IP type ID (shows checkmark)
 * @param onTypeSelected Callback when a type is selected: (connectionId, ipTypeId)
 * @param onDismiss Callback when menu should close
 * @param modifier Modifier for the menu
 */
@Composable
fun ConnectionContextMenu(
    connectionId: String,
    position: Offset,
    ipTypes: List<InformationPacketType>,
    currentTypeId: String?,
    onTypeSelected: (String, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Popup(
        offset = IntOffset(position.x.toInt(), position.y.toInt()),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = modifier,
            elevation = 8.dp,
            shape = RoundedCornerShape(4.dp),
            backgroundColor = MaterialTheme.colors.surface
        ) {
            Column(
                modifier = Modifier.width(200.dp)
            ) {
                // Header
                Text(
                    text = "Change IP Type",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(12.dp)
                )

                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))

                // IP Type list
                ipTypes.forEach { ipType ->
                    IPTypeMenuItem(
                        ipType = ipType,
                        isSelected = ipType.id == currentTypeId,
                        onClick = {
                            onTypeSelected(connectionId, ipType.id)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Individual menu item for an IP type.
 * Shows color indicator, type name, and checkmark if selected.
 *
 * @param ipType The IP type to display
 * @param isSelected Whether this type is currently selected
 * @param onClick Callback when this item is clicked
 */
@Composable
private fun IPTypeMenuItem(
    ipType: InformationPacketType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = Color(
                        red = ipType.color.red,
                        green = ipType.color.green,
                        blue = ipType.color.blue
                    ),
                    shape = RoundedCornerShape(2.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.Gray,
                    shape = RoundedCornerShape(2.dp)
                )
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Type name
        Text(
            text = ipType.typeName,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )

        // Checkmark for current type
        if (isSelected) {
            Text(
                text = "\u2713",  // Checkmark character
                color = Color(0xFF4CAF50),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Extension function to convert IPColor to Compose Color.
 * Normalizes RGB values from 0-255 range to 0-1 float range.
 */
private fun Color(red: Int, green: Int, blue: Int): Color {
    return Color(
        red = red / 255f,
        green = green / 255f,
        blue = blue / 255f
    )
}
