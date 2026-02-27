/*
 * IPGeneratorPanel - UI panel for creating custom IP types
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
import io.codenode.grapheditor.model.CustomIPTypeDefinition
import io.codenode.grapheditor.viewmodel.IPGeneratorViewModel
import io.codenode.grapheditor.viewmodel.IPGeneratorPanelState

/**
 * IP Generator panel composable for creating custom IP types.
 * Allows users to specify a type name and create a new IP type.
 *
 * This composable is the stateful wrapper - all business logic and state
 * management is delegated to the IPGeneratorViewModel.
 *
 * @param viewModel The ViewModel managing state and business logic
 * @param onTypeCreated Callback when a type is successfully created
 * @param modifier Compose modifier for styling/layout
 */
@Composable
fun IPGeneratorPanel(
    viewModel: IPGeneratorViewModel,
    onTypeCreated: (CustomIPTypeDefinition) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    IPGeneratorPanelContent(
        state = state,
        onToggleExpanded = { viewModel.toggleExpanded() },
        onTypeNameChange = { viewModel.setTypeName(it) },
        onCancel = { viewModel.reset() },
        onCreate = {
            viewModel.createType()?.let { definition ->
                onTypeCreated(definition)
            }
        },
        modifier = modifier
    )
}

/**
 * Stateless content composable for the IP Generator Panel.
 * Pure rendering function with no business logic.
 */
@Composable
private fun IPGeneratorPanelContent(
    state: IPGeneratorPanelState,
    onToggleExpanded: () -> Unit,
    onTypeNameChange: (String) -> Unit,
    onCancel: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                text = "IP Generator",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )
        }

        // Expandable content
        if (state.isExpanded) {
            Divider(color = Color(0xFFE0E0E0))

            // Type name input
            OutlinedTextField(
                value = state.typeName,
                onValueChange = onTypeNameChange,
                label = { Text("Type Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = Color.White
                )
            )

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
