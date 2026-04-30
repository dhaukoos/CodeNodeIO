/*
 * ErrorConsolePanel - Bottom panel showing copyable runtime / diagnostic error messages
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One captured error/diagnostic event surfaced to the user.
 *
 * @property timestamp Wall-clock millis when the entry was captured (used for ordering + display).
 * @property source Short tag identifying the producing subsystem (e.g., "Runtime", "Save", "UI-FBP").
 * @property message The full, copy-friendly error text.
 */
@Immutable
data class ErrorConsoleEntry(
    val timestamp: Long,
    val source: String,
    val message: String
)

/**
 * Bottom panel that surfaces error / diagnostic messages in copyable form.
 *
 * Renders newest-entries-first inside a [SelectionContainer] so the user can drag-select any
 * portion of the text and copy via the OS clipboard. A per-entry copy-icon button copies that
 * entry's full message + source + timestamp in one click. The panel is collapsible — toggles via
 * the header row. Auto-expands when at least one entry is present and the panel was previously
 * collapsed.
 *
 * @param entries Newest-last list of captured errors. (The panel renders newest-first internally.)
 * @param onClear Optional handler for the "Clear" button. When null the button is hidden.
 * @param modifier Layout modifier (e.g., the parent's column slot).
 */
@Composable
fun ErrorConsolePanel(
    entries: List<ErrorConsoleEntry>,
    onClear: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    // Auto-expand the panel the first time a new entry arrives, then leave the user in control.
    val previousCount = remember { mutableStateOf(0) }
    if (entries.size > previousCount.value && entries.isNotEmpty()) {
        expanded = true
    }
    previousCount.value = entries.size

    val errorRed = Color(0xFFD32F2F)
    val headerBackground = if (entries.isEmpty()) Color(0xFFF5F5F5) else Color(0x14D32F2F)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE0E0E0))
    ) {
        // Header row — clickable to toggle.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBackground)
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = if (entries.isEmpty()) Color.Gray else errorRed
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (entries.isEmpty()) "Error Console (no messages)"
                       else "Error Console (${entries.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (entries.isEmpty()) Color.DarkGray else errorRed
            )
            Spacer(Modifier.weight(1f))
            if (onClear != null && entries.isNotEmpty() && expanded) {
                TextButton(onClick = onClear) {
                    Text("Clear", fontSize = 11.sp)
                }
            }
            if (entries.isNotEmpty() && expanded) {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(entries.joinToString("\n\n") { it.formatForClipboard() }))
                }) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Copy all",
                        tint = errorRed,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Copy all", fontSize = 11.sp, color = errorRed)
                }
            }
        }

        // Body — visible only when expanded AND there's content.
        if (expanded && entries.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .background(Color(0xFFFFFAFA))
                    .verticalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Newest first — entries arrive newest-last.
                        entries.asReversed().forEach { entry ->
                            ErrorEntryRow(entry, errorRed) {
                                clipboard.setText(AnnotatedString(entry.formatForClipboard()))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorEntryRow(entry: ErrorConsoleEntry, accent: Color, onCopy: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x40D32F2F))
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "[${entry.source}]",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatTimestamp(entry.timestamp),
                fontSize = 10.sp,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.weight(1f))
            // Per-entry copy button — copies just this entry's formatted text.
            TextButton(onClick = onCopy, contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "Copy this entry",
                    tint = accent,
                    modifier = Modifier.padding(end = 2.dp)
                )
                Text("Copy", fontSize = 10.sp, color = accent)
            }
        }
        Spacer(Modifier.padding(2.dp))
        // The error message itself — wrapped by the outer SelectionContainer for drag-select.
        Text(
            text = entry.message,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF333333)
        )
    }
}

private fun ErrorConsoleEntry.formatForClipboard(): String =
    "[${formatTimestamp(timestamp)}] [$source] $message"

private fun formatTimestamp(epochMillis: Long): String {
    // Local time, HH:mm:ss — sufficient for in-session debugging.
    val time = java.time.LocalTime.ofInstant(
        java.time.Instant.ofEpochMilli(epochMillis),
        java.time.ZoneId.systemDefault()
    )
    return time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
}
