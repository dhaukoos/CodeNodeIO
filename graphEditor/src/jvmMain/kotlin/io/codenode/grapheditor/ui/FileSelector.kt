/*
 * FileSelector - File Selector Dropdown for Central Panel Header
 * Allows navigation between flowGraph file and CodeNode source files
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.codenode.flowgraphinspect.viewmodel.FileEntry

/**
 * Dropdown file selector for choosing between flowGraph and CodeNode source files.
 *
 * @param fileEntries List of available file entries (flowGraph file + CodeNode files)
 * @param selectedEntry The currently selected file entry
 * @param onFileSelected Callback when a file entry is selected
 * @param modifier Modifier for the selector
 */
@Composable
fun FileSelector(
    fileEntries: List<FileEntry>,
    selectedEntry: FileEntry?,
    onFileSelected: (FileEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.onSurface
            )
        ) {
            Text(
                text = selectedEntry?.displayName ?: "Select file...",
                style = MaterialTheme.typography.body2,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select file",
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            fileEntries.forEach { entry ->
                DropdownMenuItem(
                    onClick = {
                        onFileSelected(entry)
                        expanded = false
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Visual indicator for file type
                        Text(
                            text = if (entry.isFlowGraph) "📄" else "📝",
                            style = MaterialTheme.typography.body2
                        )
                        Column {
                            Text(
                                text = entry.displayName,
                                style = MaterialTheme.typography.body2
                            )
                            if (!entry.isFlowGraph) {
                                Text(
                                    text = entry.filePath.name,
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
