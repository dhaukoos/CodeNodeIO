/*
 * GraphNodePaletteSection - Collapsible "GraphNodes" section in the Node Palette
 * Displays saved GraphNode templates with port count badges
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.grapheditor.model.GraphNodeTemplateMeta
import io.codenode.fbpdsl.model.PlacementLevel

/**
 * Collapsible "GraphNodes" section in the Node Palette.
 * Lists all saved GraphNode templates as cards with port count badges.
 *
 * @param templates List of GraphNode template metadata entries
 * @param isExpanded Whether the section is currently expanded
 * @param onToggle Callback to toggle expand/collapse
 * @param onTemplateSelected Callback when a template card is clicked
 * @param modifier Modifier for the section
 */
@Composable
fun GraphNodePaletteSection(
    templates: List<GraphNodeTemplateMeta>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onTemplateSelected: (GraphNodeTemplateMeta) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (templates.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isExpanded) "▼" else "▶",
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 8.dp)
            )

            Text(
                text = "GraphNodes",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1565C0)
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "${templates.size}",
                fontSize = 11.sp,
                color = Color(0xFF757575)
            )
        }

        if (isExpanded) {
            templates.forEach { template ->
                GraphNodeTemplateCard(
                    template = template,
                    onClick = { onTemplateSelected(template) }
                )
            }
        }
    }
}

/**
 * Card for a single GraphNode template in the palette.
 * Visually distinct from CodeNode cards with blue tint, composition icon,
 * and level indicator pill.
 */
@Composable
private fun GraphNodeTemplateCard(
    template: GraphNodeTemplateMeta,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        backgroundColor = Color(0xFFE3F2FD),
        elevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFF1565C0).copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            // Name row with composition icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Composition icon (nested squares)
                Text(
                    text = "\u25A3",  // ▣ nested square
                    fontSize = 14.sp,
                    color = Color(0xFF1565C0),
                    modifier = Modifier.padding(end = 6.dp)
                )

                Text(
                    text = template.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF212121),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            template.description?.let { desc ->
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = Color(0xFF757575),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Badges row: ports and child count
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (template.inputPortCount > 0) {
                    PortCountBadge(
                        count = template.inputPortCount,
                        label = "in",
                        color = Color(0xFF4CAF50)
                    )
                }

                if (template.outputPortCount > 0) {
                    PortCountBadge(
                        count = template.outputPortCount,
                        label = "out",
                        color = Color(0xFF2196F3)
                    )
                }

                if (template.childNodeCount > 0) {
                    PortCountBadge(
                        count = template.childNodeCount,
                        label = "nodes",
                        color = Color(0xFF9E9E9E)
                    )
                }
            }

            // Level indicator pill on its own row
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                LevelIndicatorPill(level = template.tier)
            }
        }
    }
}

/**
 * Level indicator pill showing Module/Project/Universal with tier-appropriate color.
 */
@Composable
private fun LevelIndicatorPill(level: PlacementLevel) {
    val color = when (level) {
        PlacementLevel.MODULE -> Color(0xFF7B1FA2)
        PlacementLevel.PROJECT -> Color(0xFF1565C0)
        PlacementLevel.UNIVERSAL -> Color(0xFF2E7D32)
    }
    val label = when (level) {
        PlacementLevel.MODULE -> "Module"
        PlacementLevel.PROJECT -> "Project"
        PlacementLevel.UNIVERSAL -> "Universal"
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Small badge showing a count with label.
 */
@Composable
private fun PortCountBadge(
    count: Int,
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(2.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = "$count $label",
            fontSize = 10.sp,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
