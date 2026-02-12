/*
 * CompilationValidationDialog
 * Displays validation errors before compilation
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
import io.codenode.grapheditor.compilation.CompilationValidationResult
import io.codenode.grapheditor.compilation.NodeValidationError

/**
 * Dialog for displaying compilation validation errors.
 *
 * Shows a list of nodes that failed validation with their specific errors,
 * allowing users to understand what needs to be fixed before compilation.
 *
 * @param validationResult The compilation validation result containing errors
 * @param onDismiss Callback when user dismisses the dialog
 * @param modifier Optional modifier
 */
@Composable
fun CompilationValidationDialog(
    validationResult: CompilationValidationResult,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .widthIn(min = 400.dp, max = 600.dp)
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Title with error icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Error icon (red circle with X)
                    Surface(
                        color = Color(0xFFE53935),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Text(
                        text = "Compilation Validation Failed",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF424242)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = "The following nodes have missing or invalid required properties:",
                    fontSize = 14.sp,
                    color = Color(0xFF757575)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable error list
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    validationResult.nodeErrors.forEachIndexed { index, error ->
                        NodeValidationErrorItem(error)
                        if (index < validationResult.nodeErrors.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer with dismiss button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF2196F3)
                        )
                    ) {
                        Text("OK", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Individual node validation error item.
 *
 * Displays the node name and lists all missing and invalid properties.
 */
@Composable
private fun NodeValidationErrorItem(
    error: NodeValidationError,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFFFF3E0),  // Light orange background
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Node name
            Text(
                text = error.nodeName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFFE65100)  // Dark orange
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Missing properties
            if (error.missingProperties.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "Missing: ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF616161)
                    )
                    Text(
                        text = error.missingProperties.joinToString(", "),
                        fontSize = 12.sp,
                        color = Color(0xFF424242)
                    )
                }
            }

            // Invalid properties
            error.invalidProperties.forEach { propertyError ->
                Row(
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "Invalid ${propertyError.propertyName}: ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF616161)
                    )
                    Text(
                        text = propertyError.reason,
                        fontSize = 12.sp,
                        color = Color(0xFFD32F2F)  // Red for error reason
                    )
                }
            }
        }
    }
}
