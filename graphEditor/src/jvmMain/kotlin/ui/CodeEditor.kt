/*
 * CodeEditor - Editable Code Editor Composable
 * Provides syntax-highlighted code editing with line numbers and save capability
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.grapheditor.viewmodel.CodeEditorViewModel

/**
 * VisualTransformation that applies syntax highlighting to text.
 */
private class SyntaxHighlightTransformation : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val highlighted = SyntaxHighlighter.highlight(text.text)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

/**
 * Editable code editor with syntax highlighting, line numbers, and save support.
 *
 * @param viewModel The CodeEditorViewModel managing file state
 * @param onSaveStatusMessage Callback to report save status messages
 * @param modifier Modifier for the editor
 */
@Composable
fun CodeEditor(
    viewModel: CodeEditorViewModel,
    onSaveStatusMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val editorState by viewModel.state.collectAsState()
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val syntaxTransformation = remember { SyntaxHighlightTransformation() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF2B2B2B))
    ) {
        // Editor toolbar (shows "Editable" indicator to distinguish from read-only TextualView)
        if (editorState.hasFile && !editorState.isReadOnly) {
            EditorToolbar(
                isDirty = editorState.isDirty,
                fileName = editorState.currentFile?.name ?: "",
                onSave = {
                    val success = viewModel.save()
                    onSaveStatusMessage(
                        if (success) "File saved: ${editorState.currentFile?.name}"
                        else "Save failed: ${editorState.errorMessage ?: "unknown error"}"
                    )
                }
            )
        }

        // Error state
        if (editorState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2B2B2B))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = editorState.errorMessage!!,
                    color = Color(0xFFFF6B68),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            }
            return@Column
        }

        // No file loaded state
        if (!editorState.hasFile) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2B2B2B))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No file loaded",
                    color = Color(0xFF808080),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            }
            return@Column
        }

        // Code editor with line numbers
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
        ) {
            // Line number gutter
            val lineCount = editorState.editedContent.count { it == '\n' } + 1
            val gutterWidth = (lineCount.toString().length * 10 + 24).dp

            Column(
                modifier = Modifier
                    .width(gutterWidth)
                    .fillMaxHeight()
                    .background(Color(0xFF313335))
                    .verticalScroll(verticalScrollState)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount) {
                    Text(
                        text = "$i",
                        color = Color(0xFF808080),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            // Editor content
            var textFieldValue by remember(editorState.currentFile, editorState.originalContent) {
                mutableStateOf(TextFieldValue(editorState.editedContent))
            }

            // Sync ViewModel content when textFieldValue changes
            LaunchedEffect(textFieldValue.text) {
                if (textFieldValue.text != editorState.editedContent) {
                    viewModel.updateContent(textFieldValue.text)
                }
            }

            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(verticalScrollState)
                    .horizontalScroll(horizontalScrollState)
                    .padding(16.dp),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFFA9B7C6)
                ),
                cursorBrush = SolidColor(Color.White),
                visualTransformation = syntaxTransformation
            )
        }
    }
}

/**
 * Toolbar showing file name, dirty indicator, and save button.
 */
@Composable
private fun EditorToolbar(
    isDirty: Boolean,
    fileName: String,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF3C3F41),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = fileName,
                    color = Color(0xFFBBBBBB),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                if (isDirty) {
                    Text(
                        text = " ●",
                        color = Color(0xFFFFAA00),
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Editable",
                    color = Color(0xFF6A8759),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }

            if (isDirty) {
                IconButton(
                    onClick = onSave,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save file",
                        tint = Color(0xFFBBBBBB),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dialog for unsaved changes confirmation.
 *
 * @param fileName Name of the file with unsaved changes
 * @param onSave Save and continue
 * @param onDiscard Discard changes and continue
 * @param onCancel Cancel the navigation
 */
@Composable
fun UnsavedChangesDialog(
    fileName: String,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Unsaved Changes") },
        text = { Text("\"$fileName\" has unsaved changes. What would you like to do?") },
        buttons = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                TextButton(onClick = onDiscard) {
                    Text("Discard")
                }
                Button(onClick = onSave) {
                    Text("Save")
                }
            }
        }
    )
}
