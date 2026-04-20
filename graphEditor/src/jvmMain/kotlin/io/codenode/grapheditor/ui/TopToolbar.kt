/*
 * TopToolbar - Main toolbar for Graph Editor
 * File operations, undo/redo, and grouping controls
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.fbpdsl.model.FeatureGate
import io.codenode.grapheditor.state.UndoRedoManager

@Composable
fun TopToolbar(
    undoRedoManager: UndoRedoManager,
    featureGate: FeatureGate? = null,
    canGroup: Boolean = false,
    canUngroup: Boolean = false,
    isInsideGraphNode: Boolean = false,
    currentGraphNodeName: String? = null,
    flowGraphName: String = "New Graph",
    onShowProperties: () -> Unit = {},
    onNew: () -> Unit,
    onOpen: () -> Unit,
    onSave: () -> Unit,
    onGenerate: () -> Unit = {},
    onGenerateUIFBP: () -> Unit = {},
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onGroup: () -> Unit = {},
    onUngroup: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(56.dp),
        color = Color(0xFF2196F3),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Back button (only visible when inside a GraphNode)
            if (isInsideGraphNode) {
                TextButton(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text("\u2190 Back")  // Left arrow
                }

                Divider(
                    modifier = Modifier.width(1.dp).height(32.dp),
                    color = Color.White.copy(alpha = 0.3f)
                )
            }

            // Title with optional breadcrumb
            Text(
                text = if (isInsideGraphNode && currentGraphNodeName != null) {
                    "Inside: $currentGraphNodeName"
                } else {
                    "CodeNodeIO Graph Editor"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Show graph name and properties button when at root level
            if (!isInsideGraphNode) {
                Text(
                    text = " - ",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 18.sp
                )
                Text(
                    text = flowGraphName,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    fontSize = 18.sp
                )
                IconButton(onClick = onShowProperties) {
                    Text(
                        text = "\u2699",  // Gear icon
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // File operations
            TextButton(
                onClick = onNew,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("New")
            }

            TextButton(
                onClick = onOpen,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Open")
            }

            TextButton(
                onClick = onSave,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Save")
            }

            TextButton(
                onClick = onGenerate,
                enabled = featureGate?.canGenerate() != false,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Generate Module")
            }

            TextButton(
                onClick = onGenerateUIFBP,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Generate UI-FBP")
            }

            Divider(
                modifier = Modifier.width(1.dp).height(32.dp),
                color = Color.White.copy(alpha = 0.3f)
            )

            // Undo/Redo
            TextButton(
                onClick = onUndo,
                enabled = undoRedoManager.canUndo,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Undo")
            }

            TextButton(
                onClick = onRedo,
                enabled = undoRedoManager.canRedo,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Redo")
            }

            Divider(
                modifier = Modifier.width(1.dp).height(32.dp),
                color = Color.White.copy(alpha = 0.3f)
            )

            // Group/Ungroup
            TextButton(
                onClick = onGroup,
                enabled = canGroup,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Group")
            }

            TextButton(
                onClick = onUngroup,
                enabled = canUngroup,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Ungroup")
            }
        }
    }
}
