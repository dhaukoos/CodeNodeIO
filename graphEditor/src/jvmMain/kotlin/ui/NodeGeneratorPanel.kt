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
import io.codenode.grapheditor.viewmodel.NodeGeneratorViewModel
import io.codenode.grapheditor.viewmodel.NodeGeneratorPanelState
import io.codenode.grapheditor.repository.CustomNodeDefinition

/**
 * Node Generator panel composable for creating custom node types.
 * Allows users to specify name, input count (0-3), and output count (0-3).
 *
 * This composable is purely for UI rendering - all business logic and state
 * management is delegated to the NodeGeneratorViewModel.
 *
 * @param viewModel The ViewModel managing state and business logic
 * @param onNodeCreated Callback when a node is successfully created
 * @param modifier Compose modifier for styling/layout
 */
@Composable
fun NodeGeneratorPanel(
    viewModel: NodeGeneratorViewModel,
    onNodeCreated: (CustomNodeDefinition) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    NodeGeneratorPanelContent(
        state = state,
        onToggleExpanded = { viewModel.toggleExpanded() },
        onNameChange = { viewModel.setName(it) },
        onInputCountChange = { viewModel.setInputCount(it) },
        onOutputCountChange = { viewModel.setOutputCount(it) },
        onInputDropdownExpandedChange = { viewModel.setInputDropdownExpanded(it) },
        onOutputDropdownExpandedChange = { viewModel.setOutputDropdownExpanded(it) },
        onCancel = { viewModel.reset() },
        onCreate = {
            viewModel.createNode()?.let { node ->
                onNodeCreated(node)
            }
        },
        modifier = modifier
    )
}

/**
 * Stateless content composable for the Node Generator Panel.
 * Pure rendering function with no business logic.
 */
@Composable
private fun NodeGeneratorPanelContent(
    state: NodeGeneratorPanelState,
    onToggleExpanded: () -> Unit,
    onNameChange: (String) -> Unit,
    onInputCountChange: (Int) -> Unit,
    onOutputCountChange: (Int) -> Unit,
    onInputDropdownExpandedChange: (Boolean) -> Unit,
    onOutputDropdownExpandedChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val portOptions = listOf(0, 1, 2, 3)

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
                .clickable { onToggleExpanded() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (state.isExpanded) "▼" else "▶",
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
        if (state.isExpanded) {
            Divider(color = Color(0xFFE0E0E0))

            // Name input
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
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
                        onClick = { onInputDropdownExpandedChange(true) },
                        modifier = Modifier.width(80.dp)
                    ) {
                        Text("${state.inputCount}")
                        Spacer(Modifier.weight(1f))
                        Text("▼", fontSize = 10.sp)
                    }
                    DropdownMenu(
                        expanded = state.inputDropdownExpanded,
                        onDismissRequest = { onInputDropdownExpandedChange(false) }
                    ) {
                        portOptions.forEach { count ->
                            DropdownMenuItem(
                                onClick = {
                                    onInputCountChange(count)
                                    onInputDropdownExpandedChange(false)
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
                        onClick = { onOutputDropdownExpandedChange(true) },
                        modifier = Modifier.width(80.dp)
                    ) {
                        Text("${state.outputCount}")
                        Spacer(Modifier.weight(1f))
                        Text("▼", fontSize = 10.sp)
                    }
                    DropdownMenu(
                        expanded = state.outputDropdownExpanded,
                        onDismissRequest = { onOutputDropdownExpandedChange(false) }
                    ) {
                        portOptions.forEach { count ->
                            DropdownMenuItem(
                                onClick = {
                                    onOutputCountChange(count)
                                    onOutputDropdownExpandedChange(false)
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
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                // Create button - disabled when !state.isValid
                Button(
                    onClick = onCreate,
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
