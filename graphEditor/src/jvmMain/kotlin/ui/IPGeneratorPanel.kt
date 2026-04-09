/*
 * IPGeneratorPanel - UI panel for creating custom IP types
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.fbpdsl.model.InformationPacketType
import io.codenode.flowgraphtypes.model.CustomIPTypeDefinition
import io.codenode.fbpdsl.model.PlacementLevel
import io.codenode.flowgraphgenerate.viewmodel.IPGeneratorViewModel
import io.codenode.flowgraphgenerate.viewmodel.IPGeneratorPanelState
import io.codenode.flowgraphgenerate.viewmodel.IPPropertyState

/**
 * IP Generator panel composable for creating custom IP types.
 * Allows users to specify a type name, add typed properties, and create a new IP type.
 *
 * @param viewModel The ViewModel managing state and business logic
 * @param onTypeCreated Callback when a type is successfully created
 * @param ipTypes List of available IP types for property type selection
 * @param modifier Compose modifier for styling/layout
 */
@Composable
fun IPGeneratorPanel(
    viewModel: IPGeneratorViewModel,
    onTypeCreated: (CustomIPTypeDefinition) -> Unit,
    ipTypes: List<InformationPacketType> = emptyList(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    IPGeneratorPanelContent(
        state = state,
        ipTypes = ipTypes,
        onToggleExpanded = { viewModel.toggleExpanded() },
        onTypeNameChange = { viewModel.setTypeName(it) },
        onAddProperty = { viewModel.addProperty() },
        onRemoveProperty = { viewModel.removeProperty(it) },
        onPropertyNameChange = { id, name -> viewModel.updatePropertyName(id, name) },
        onPropertyTypeChange = { id, typeId -> viewModel.updatePropertyType(id, typeId) },
        onPropertyRequiredChange = { id, required -> viewModel.updatePropertyRequired(id, required) },
        onLevelChange = { viewModel.setSelectedLevel(it) },
        onLevelDropdownExpandedChange = { viewModel.setLevelDropdownExpanded(it) },
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
    ipTypes: List<InformationPacketType>,
    onToggleExpanded: () -> Unit,
    onTypeNameChange: (String) -> Unit,
    onAddProperty: () -> Unit,
    onRemoveProperty: (String) -> Unit,
    onPropertyNameChange: (String, String) -> Unit,
    onPropertyTypeChange: (String, String) -> Unit,
    onPropertyRequiredChange: (String, Boolean) -> Unit,
    onLevelChange: (PlacementLevel) -> Unit,
    onLevelDropdownExpandedChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(220.dp)
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
                isError = state.hasNameConflict,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = Color.White,
                    errorBorderColor = Color(0xFFD32F2F)
                )
            )
            if (state.hasNameConflict) {
                Text(
                    text = "Name already exists",
                    color = Color(0xFFD32F2F),
                    fontSize = 11.sp
                )
            }

            // Level dropdown (matching Node Generator design)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Level",
                    fontSize = 12.sp,
                    color = Color(0xFF757575)
                )
                Box {
                    OutlinedButton(
                        onClick = { onLevelDropdownExpandedChange(true) },
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text(state.selectedLevel.displayName, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text("▼", fontSize = 10.sp)
                    }
                    DropdownMenu(
                        expanded = state.levelDropdownExpanded,
                        onDismissRequest = { onLevelDropdownExpandedChange(false) }
                    ) {
                        state.availableLevels.forEach { level ->
                            DropdownMenuItem(
                                onClick = {
                                    onLevelChange(level)
                                    onLevelDropdownExpandedChange(false)
                                }
                            ) {
                                Text(level.displayName)
                            }
                        }
                    }
                }
            }

            // Properties section header with add button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Properties",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF757575)
                )
                IconButton(
                    onClick = onAddProperty,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add property",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Property rows
            state.properties.forEach { propertyState ->
                IPPropertyRow(
                    propertyState = propertyState,
                    ipTypes = ipTypes,
                    isNameDuplicate = propertyState.id in state.duplicatePropertyNameIds,
                    onNameChange = { name -> onPropertyNameChange(propertyState.id, name) },
                    onTypeChange = { typeId -> onPropertyTypeChange(propertyState.id, typeId) },
                    onRequiredChange = { required -> onPropertyRequiredChange(propertyState.id, required) },
                    onRemove = { onRemoveProperty(propertyState.id) }
                )
            }
            if (state.hasDuplicatePropertyNames) {
                Text(
                    text = "Duplicate property names",
                    color = Color(0xFFD32F2F),
                    fontSize = 11.sp
                )
            }

            Divider(color = Color(0xFFE0E0E0))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

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

/**
 * A single property row in the IP Generator form.
 * Contains a name text field, type dropdown with color swatch, required checkbox, and remove button.
 */
@Composable
private fun IPPropertyRow(
    propertyState: IPPropertyState,
    ipTypes: List<InformationPacketType>,
    isNameDuplicate: Boolean = false,
    onNameChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onRequiredChange: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    val currentType = ipTypes.find { it.id == propertyState.selectedTypeId }
        ?: ipTypes.find { it.id == "ip_any" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, Color(0xFFE0E0E0))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Row 1: Name field + remove button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = propertyState.name,
                onValueChange = onNameChange,
                placeholder = { Text("name", fontSize = 11.sp) },
                singleLine = true,
                isError = isNameDuplicate,
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 36.dp),
                textStyle = TextStyle(fontSize = 12.sp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = Color.White,
                    focusedBorderColor = MaterialTheme.colors.primary,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    errorBorderColor = Color(0xFFD32F2F)
                )
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove property",
                    tint = Color(0xFFFF5722),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Row 2: Type dropdown + required checkbox
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Type dropdown
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = currentType?.typeName ?: "Any",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 36.dp)
                        .clickable { typeDropdownExpanded = true },
                    textStyle = TextStyle(fontSize = 11.sp),
                    readOnly = true,
                    singleLine = true,
                    leadingIcon = {
                        if (currentType != null) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        Color(
                                            red = currentType.color.red / 255f,
                                            green = currentType.color.green / 255f,
                                            blue = currentType.color.blue / 255f
                                        ),
                                        CircleShape
                                    )
                            )
                        }
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select type",
                            modifier = Modifier.size(16.dp).clickable { typeDropdownExpanded = true }
                        )
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        backgroundColor = Color.White,
                        focusedBorderColor = MaterialTheme.colors.primary,
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )

                DropdownMenu(
                    expanded = typeDropdownExpanded,
                    onDismissRequest = { typeDropdownExpanded = false }
                ) {
                    ipTypes.forEach { ipType ->
                        DropdownMenuItem(
                            onClick = {
                                onTypeChange(ipType.id)
                                typeDropdownExpanded = false
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            Color(
                                                red = ipType.color.red / 255f,
                                                green = ipType.color.green / 255f,
                                                blue = ipType.color.blue / 255f
                                            ),
                                            CircleShape
                                        )
                                )
                                Text(
                                    text = ipType.typeName,
                                    fontSize = 12.sp,
                                    color = if (ipType.id == propertyState.selectedTypeId) {
                                        MaterialTheme.colors.primary
                                    } else {
                                        MaterialTheme.colors.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Required checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = propertyState.isRequired,
                    onCheckedChange = onRequiredChange,
                    modifier = Modifier.size(20.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF2196F3)
                    )
                )
                Text(
                    text = "Req",
                    fontSize = 10.sp,
                    color = Color(0xFF757575),
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }
}
