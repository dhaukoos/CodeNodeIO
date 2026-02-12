/*
 * FlowGraphPropertiesDialog - Properties Dialog for FlowGraph Settings
 * Allows editing the FlowGraph name and target platforms
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

/**
 * Maps TargetPlatform enum values to human-readable display names.
 */
private fun TargetPlatform.displayName(): String = when (this) {
    TargetPlatform.KMP_ANDROID -> "Android"
    TargetPlatform.KMP_IOS -> "iOS"
    TargetPlatform.KMP_DESKTOP -> "Desktop (JVM)"
    TargetPlatform.KMP_WEB -> "Web (JavaScript)"
    TargetPlatform.KMP_WASM -> "Web (Wasm)"
    TargetPlatform.GO_SERVER -> "Go Server"
    TargetPlatform.GO_CLI -> "Go CLI"
}

/**
 * Dialog for editing FlowGraph properties (name and target platforms).
 * Displays as a centered popup with a card container.
 *
 * @param name Current FlowGraph name
 * @param targetPlatforms Currently selected target platforms
 * @param onNameChanged Callback when name is edited
 * @param onTargetPlatformToggled Callback when a platform checkbox is toggled
 * @param onDismiss Callback when dialog should close
 * @param modifier Modifier for the dialog
 */
@Composable
fun FlowGraphPropertiesDialog(
    name: String,
    targetPlatforms: Set<TargetPlatform>,
    onNameChanged: (String) -> Unit,
    onTargetPlatformToggled: (TargetPlatform) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Focus requester to enable keyboard events
    val focusRequester = remember { FocusRequester() }

    // Request focus when the dialog appears
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Centered popup
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
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FlowGraph Properties",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    TextButton(onClick = onDismiss) {
                        Text("\u2715", fontSize = 16.sp)  // X character
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))

                Spacer(modifier = Modifier.height(16.dp))

                // Module Name field
                Text(
                    text = "Module Name",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "Enter module name",
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                        )
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Used as the generated code module name",
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Target Platforms section
                Text(
                    text = "Target Platforms",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Only show KMP platforms as specified in the plan
                val kmpPlatforms = listOf(
                    TargetPlatform.KMP_ANDROID,
                    TargetPlatform.KMP_IOS,
                    TargetPlatform.KMP_DESKTOP,
                    TargetPlatform.KMP_WEB,
                    TargetPlatform.KMP_WASM
                )

                kmpPlatforms.forEach { platform ->
                    PlatformCheckboxRow(
                        platform = platform,
                        isChecked = platform in targetPlatforms,
                        onToggle = { onTargetPlatformToggled(platform) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

/**
 * A row with a checkbox for a target platform.
 *
 * @param platform The target platform
 * @param isChecked Whether the platform is currently selected
 * @param onToggle Callback when the checkbox is toggled
 */
@Composable
private fun PlatformCheckboxRow(
    platform: TargetPlatform,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colors.primary,
                uncheckedColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = platform.displayName(),
            fontSize = 14.sp
        )
    }
}
