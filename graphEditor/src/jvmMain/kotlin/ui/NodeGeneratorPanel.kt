/*
 * NodeGeneratorPanel - UI panel for creating custom node types
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.grapheditor.state.NodeGeneratorState
import io.codenode.grapheditor.repository.CustomNodeDefinition

/**
 * Node Generator panel composable for creating custom node types.
 * Allows users to specify name, input count (0-3), and output count (0-3).
 *
 * @param state Current form state
 * @param onStateChange Callback when form values change
 * @param onCreateNode Callback when Create is clicked with valid data
 * @param modifier Compose modifier for styling/layout
 */
@Composable
fun NodeGeneratorPanel(
    state: NodeGeneratorState,
    onStateChange: (NodeGeneratorState) -> Unit,
    onCreateNode: (CustomNodeDefinition) -> Unit,
    modifier: Modifier = Modifier
) {
    val portOptions = listOf(0, 1, 2, 3)
    var inputDropdownExpanded by remember { mutableStateOf(false) }
    var outputDropdownExpanded by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .width(250.dp)
            .background(Color(0xFFF5F5F5))
            .border(1.dp, Color(0xFFE0E0E0))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Collapsible header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isExpanded) "▼" else "▶",
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "Node Generator",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )
        }

        // Expandable content
        if (isExpanded) {
            Divider(color = Color(0xFFE0E0E0))

            // Name input
        OutlinedTextField(
            value = state.name,
            onValueChange = { onStateChange(state.withName(it)) },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                backgroundColor = Color.White
            )
        )

        // Input count dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Inputs",
                fontSize = 12.sp,
                color = Color(0xFF757575)
            )
            Box {
                OutlinedButton(
                    onClick = { inputDropdownExpanded = true },
                    modifier = Modifier.width(80.dp)
                ) {
                    Text("${state.inputCount}")
                    Spacer(Modifier.weight(1f))
                    Text("▼", fontSize = 10.sp)
                }
                DropdownMenu(
                    expanded = inputDropdownExpanded,
                    onDismissRequest = { inputDropdownExpanded = false }
                ) {
                    portOptions.forEach { count ->
                        DropdownMenuItem(
                            onClick = {
                                onStateChange(state.withInputCount(count))
                                inputDropdownExpanded = false
                            }
                        ) {
                            Text("$count")
                        }
                    }
                }
            }
        }

        // Output count dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Outputs",
                fontSize = 12.sp,
                color = Color(0xFF757575)
            )
            Box {
                OutlinedButton(
                    onClick = { outputDropdownExpanded = true },
                    modifier = Modifier.width(80.dp)
                ) {
                    Text("${state.outputCount}")
                    Spacer(Modifier.weight(1f))
                    Text("▼", fontSize = 10.sp)
                }
                DropdownMenu(
                    expanded = outputDropdownExpanded,
                    onDismissRequest = { outputDropdownExpanded = false }
                ) {
                    portOptions.forEach { count ->
                        DropdownMenuItem(
                            onClick = {
                                onStateChange(state.withOutputCount(count))
                                outputDropdownExpanded = false
                            }
                        ) {
                            Text("$count")
                        }
                    }
                }
            }
        }

        // Preview of genericType (only show when valid)
        if (state.name.isNotBlank()) {
            Text(
                text = "Type: ${state.genericType}",
                fontSize = 11.sp,
                color = if (state.isValid) Color(0xFF4CAF50) else Color(0xFFFF5722),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            if (!state.isValid && state.inputCount == 0 && state.outputCount == 0) {
                Text(
                    text = "At least one port required",
                    fontSize = 10.sp,
                    color = Color(0xFFFF5722)
                )
            }
        }

        Divider(color = Color(0xFFE0E0E0))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cancel button
            OutlinedButton(
                onClick = { onStateChange(state.reset()) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            // Create button - disabled when !state.isValid
            Button(
                onClick = {
                    val node = CustomNodeDefinition.create(
                        name = state.name.trim(),
                        inputCount = state.inputCount,
                        outputCount = state.outputCount
                    )
                    onCreateNode(node)
                    onStateChange(state.reset())
                },
                enabled = state.isValid,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF2196F3),
                    disabledBackgroundColor = Color(0xFFBDBDBD)
                )
            ) {
                Text("Create", color = Color.White)
            }
        }
        }
    }
}
