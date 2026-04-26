/*
 * NewFlowGraphDialog - Simple name-only dialog for creating a new FlowGraph
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import java.io.File

@Composable
fun NewFlowGraphDialog(
    moduleFlowDir: File?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var name by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isValid = name.isNotBlank() && errorMessage == null

    fun validate(input: String): String? {
        if (input.isBlank()) return null
        if (input.contains(Regex("[/\\\\:*?\"<>|]"))) return "Name contains invalid characters"
        if (moduleFlowDir != null) {
            val existing = File(moduleFlowDir, "${input}.flow.kt")
            if (existing.exists()) return "A flowGraph named '$input' already exists in this module"
        }
        return null
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = modifier
                .width(400.dp)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            Key.Escape -> { onDismiss(); true }
                            Key.Enter -> {
                                if (isValid) { onConfirm(name); true } else false
                            }
                            else -> false
                        }
                    } else false
                },
            elevation = 16.dp,
            shape = RoundedCornerShape(8.dp),
            backgroundColor = MaterialTheme.colors.surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New FlowGraph",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    TextButton(onClick = onDismiss) {
                        Text("\u2715", fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(16.dp))

                Text("FlowGraph Name", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = validate(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null,
                    placeholder = {
                        Text(
                            "Enter flowGraph name",
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                        )
                    }
                )

                if (errorMessage != null) {
                    Text(
                        errorMessage!!,
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.error
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(name) },
                        enabled = isValid
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}
