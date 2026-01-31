/*
 * IPPalette - InformationPacket Type Selection Panel
 * Provides searchable list of IP types with color indicators for type selection
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.fbpdsl.model.IPColor
import io.codenode.fbpdsl.model.InformationPacketType

/**
 * IP Palette component for browsing and selecting InformationPacket types.
 *
 * Displays a searchable list of IP types with color indicators. Users can
 * search by type name (case-insensitive) and click to select a type.
 *
 * @param ipTypes List of available IP types to display
 * @param selectedTypeId ID of the currently selected type (for highlighting)
 * @param onTypeSelected Callback when a type is selected
 * @param modifier Modifier for the palette
 */
@Composable
fun IPPalette(
    ipTypes: List<InformationPacketType>,
    selectedTypeId: String? = null,
    onTypeSelected: (InformationPacketType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    // Filter types by search query
    val filteredTypes = remember(ipTypes, searchQuery) {
        if (searchQuery.isBlank()) {
            ipTypes
        } else {
            ipTypes.filter { ipType ->
                ipType.typeName.contains(searchQuery, ignoreCase = true) ||
                    ipType.description?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    Column(
        modifier = modifier
            .width(200.dp)
            .fillMaxHeight()
            .background(Color(0xFFFAFAFA))
            .border(1.dp, Color(0xFFE0E0E0))
    ) {
        // Header
        Text(
            text = "IP Types",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        )

        // Search box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search types...", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth()
        )

        Divider(color = Color(0xFFE0E0E0))

        // Type list
        if (filteredTypes.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matching types",
                    fontSize = 12.sp,
                    color = Color(0xFF757575)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredTypes, key = { it.id }) { ipType ->
                    IPTypeItem(
                        ipType = ipType,
                        isSelected = ipType.id == selectedTypeId,
                        onClick = { onTypeSelected(ipType) }
                    )
                }
            }
        }
    }
}

/**
 * Individual IP type item in the palette.
 *
 * @param ipType The IP type to display
 * @param isSelected Whether this type is currently selected
 * @param onClick Callback when item is clicked
 */
@Composable
private fun IPTypeItem(
    ipType: InformationPacketType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color(0xFFE3F2FD) else Color.White
    val borderColor = if (isSelected) Color(0xFF2196F3) else Color(0xFFE0E0E0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        backgroundColor = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        elevation = if (isSelected) 2.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color swatch
            ColorSwatch(
                color = ipType.color,
                modifier = Modifier.size(24.dp)
            )

            // Type info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = ipType.typeName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF212121),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                ipType.description?.let { desc ->
                    Text(
                        text = desc,
                        fontSize = 10.sp,
                        color = Color(0xFF757575),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Circular color swatch displaying an IPColor.
 *
 * @param color The IPColor to display
 * @param modifier Modifier for the swatch
 */
@Composable
fun ColorSwatch(
    color: IPColor,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(color.red, color.green, color.blue))
            .border(1.dp, Color(0xFFBDBDBD), CircleShape)
    )
}

