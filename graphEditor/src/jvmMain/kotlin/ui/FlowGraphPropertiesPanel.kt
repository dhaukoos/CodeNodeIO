/*
 * FlowGraphPropertiesPanel
 * Properties panel for configuring FlowGraph-level settings
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.codenode.fbpdsl.model.FlowGraph

/**
 * Target platform display information.
 */
data class TargetPlatformInfo(
    val platform: FlowGraph.TargetPlatform,
    val displayName: String,
    val description: String
)

/**
 * Available KMP target platforms with display information.
 */
val KMP_TARGET_PLATFORMS = listOf(
    TargetPlatformInfo(
        FlowGraph.TargetPlatform.KMP_ANDROID,
        "Android",
        "Generate Android-compatible Kotlin code"
    ),
    TargetPlatformInfo(
        FlowGraph.TargetPlatform.KMP_IOS,
        "iOS",
        "Generate iOS framework (Kotlin/Native)"
    ),
    TargetPlatformInfo(
        FlowGraph.TargetPlatform.KMP_DESKTOP,
        "Desktop (JVM)",
        "Generate JVM desktop application code"
    ),
    TargetPlatformInfo(
        FlowGraph.TargetPlatform.KMP_WEB,
        "Web (JavaScript)",
        "Generate JavaScript/browser code"
    ),
    TargetPlatformInfo(
        FlowGraph.TargetPlatform.KMP_WASM,
        "Web (Wasm)",
        "Generate WebAssembly code (experimental)"
    )
)

/**
 * Dialog for editing FlowGraph properties.
 *
 * Allows users to configure:
 * - Graph name (used for generated module name)
 * - Graph description
 * - Graph version
 * - Target platforms for code generation
 *
 * @param flowGraph The current FlowGraph
 * @param onNameChange Callback when name changes
 * @param onDescriptionChange Callback when description changes
 * @param onVersionChange Callback when version changes
 * @param onPlatformToggle Callback when a platform is toggled
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun FlowGraphPropertiesDialog(
    flowGraph: FlowGraph,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String?) -> Unit,
    onVersionChange: (String) -> Unit,
    onPlatformToggle: (FlowGraph.TargetPlatform, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var editedName by remember(flowGraph.name) { mutableStateOf(flowGraph.name) }
    var editedDescription by remember(flowGraph.description) { mutableStateOf(flowGraph.description ?: "") }
    var editedVersion by remember(flowGraph.version) { mutableStateOf(flowGraph.version) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var versionError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(min = 400.dp, max = 500.dp)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Title
                Text(
                    text = "Flow Graph Properties",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Name field
                    Text(
                        text = "Name",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF616161)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { newValue ->
                            editedName = newValue
                            nameError = if (newValue.isBlank()) "Name cannot be empty" else null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = nameError != null,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF2196F3),
                            errorBorderColor = Color(0xFFE53935)
                        )
                    )
                    if (nameError != null) {
                        Text(
                            text = nameError!!,
                            fontSize = 12.sp,
                            color = Color(0xFFE53935)
                        )
                    }
                    Text(
                        text = "Used as the generated module name",
                        fontSize = 11.sp,
                        color = Color(0xFF9E9E9E)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Version field
                    Text(
                        text = "Version",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF616161)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editedVersion,
                        onValueChange = { newValue ->
                            editedVersion = newValue
                            versionError = if (!isValidSemanticVersion(newValue)) {
                                "Must be semantic version (e.g., 1.0.0)"
                            } else null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = versionError != null,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF2196F3),
                            errorBorderColor = Color(0xFFE53935)
                        )
                    )
                    if (versionError != null) {
                        Text(
                            text = versionError!!,
                            fontSize = 12.sp,
                            color = Color(0xFFE53935)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description field
                    Text(
                        text = "Description",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF616161)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editedDescription,
                        onValueChange = { editedDescription = it },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        maxLines = 3,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF2196F3)
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Target Platforms section
                    Text(
                        text = "Target Platforms",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF424242)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select platforms for code generation",
                        fontSize = 12.sp,
                        color = Color(0xFF9E9E9E)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Platform checkboxes
                    KMP_TARGET_PLATFORMS.forEach { platformInfo ->
                        val isChecked = flowGraph.targetPlatforms.contains(platformInfo.platform)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    onPlatformToggle(platformInfo.platform, checked)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF2196F3)
                                )
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = platformInfo.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF424242)
                                )
                                Text(
                                    text = platformInfo.description,
                                    fontSize = 11.sp,
                                    color = Color(0xFF9E9E9E)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            // Apply changes
                            if (nameError == null && versionError == null) {
                                if (editedName != flowGraph.name) {
                                    onNameChange(editedName)
                                }
                                if (editedVersion != flowGraph.version) {
                                    onVersionChange(editedVersion)
                                }
                                val newDescription = editedDescription.ifBlank { null }
                                if (newDescription != flowGraph.description) {
                                    onDescriptionChange(newDescription)
                                }
                                onDismiss()
                            }
                        },
                        enabled = nameError == null && versionError == null,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF2196F3)
                        )
                    ) {
                        Text("Save", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Validates semantic version format.
 */
private fun isValidSemanticVersion(version: String): Boolean {
    val semverPattern = Regex("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.-]+)?(\\+[a-zA-Z0-9.-]+)?$")
    return semverPattern.matches(version)
}
