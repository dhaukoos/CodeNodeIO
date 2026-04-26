/*
 * ModulePropertiesDialog - Create and edit module properties
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.clickable
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
import io.codenode.fbpdsl.model.FlowGraph.TargetPlatform

enum class ModuleDialogMode { CREATE, EDIT }

private fun TargetPlatform.displayName(): String = when (this) {
    TargetPlatform.KMP_ANDROID -> "Android"
    TargetPlatform.KMP_IOS -> "iOS"
    TargetPlatform.KMP_DESKTOP -> "Desktop (JVM)"
    TargetPlatform.KMP_WEB -> "Web (JavaScript)"
    TargetPlatform.KMP_WASM -> "Web (Wasm)"
    TargetPlatform.GO_SERVER -> "Go Server"
    TargetPlatform.GO_CLI -> "Go CLI"
}

@Composable
fun ModulePropertiesDialog(
    mode: ModuleDialogMode,
    existingName: String = "",
    existingPath: String = "",
    existingPlatforms: Set<TargetPlatform> = emptySet(),
    onCreateModule: (name: String, platforms: List<TargetPlatform>) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var name by remember { mutableStateOf(if (mode == ModuleDialogMode.EDIT) existingName else "") }
    var selectedPlatforms by remember {
        mutableStateOf(
            if (mode == ModuleDialogMode.EDIT) existingPlatforms
            else emptySet()
        )
    }

    val isNameValid = name.length >= 3
    val hasPlatforms = selectedPlatforms.isNotEmpty()
    val isCreateEnabled = isNameValid && hasPlatforms

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
                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                        onDismiss()
                        true
                    } else {
                        false
                    }
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
                        text = if (mode == ModuleDialogMode.CREATE) "Create New Module" else "Module Properties",
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

                Text("Module Name", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))

                if (mode == ModuleDialogMode.CREATE) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = name.isNotEmpty() && !isNameValid,
                        placeholder = {
                            Text(
                                "Module name (min 3 chars)",
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    )
                    if (name.isNotEmpty() && !isNameValid) {
                        Text(
                            "Name must be at least 3 characters",
                            fontSize = 12.sp,
                            color = MaterialTheme.colors.error
                        )
                    }
                } else {
                    Text(
                        text = existingName,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    if (existingPath.isNotEmpty()) {
                        Text(
                            text = existingPath,
                            fontSize = 12.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text("Target Platforms", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                if (!hasPlatforms && mode == ModuleDialogMode.CREATE) {
                    Text(
                        "Select at least 1 platform",
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                val kmpPlatforms = listOf(
                    TargetPlatform.KMP_ANDROID,
                    TargetPlatform.KMP_IOS,
                    TargetPlatform.KMP_DESKTOP,
                    TargetPlatform.KMP_WEB,
                    TargetPlatform.KMP_WASM
                )

                kmpPlatforms.forEach { platform ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPlatforms = if (platform in selectedPlatforms) {
                                    selectedPlatforms - platform
                                } else {
                                    selectedPlatforms + platform
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = platform in selectedPlatforms,
                            onCheckedChange = {
                                selectedPlatforms = if (platform in selectedPlatforms) {
                                    selectedPlatforms - platform
                                } else {
                                    selectedPlatforms + platform
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colors.primary,
                                uncheckedColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(platform.displayName(), fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (mode == ModuleDialogMode.CREATE) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onCreateModule(name, selectedPlatforms.toList()) },
                            enabled = isCreateEnabled
                        ) {
                            Text("Create Module")
                        }
                    } else {
                        Button(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}
