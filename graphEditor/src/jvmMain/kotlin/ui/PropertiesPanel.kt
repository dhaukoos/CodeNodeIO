/*
 * PropertiesPanel - Node Configuration Panel
 * Displays and edits properties for selected nodes in the flow graph
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.model.NodeTypeDefinition
import io.codenode.grapheditor.state.IPTypeRegistry
import io.codenode.grapheditor.viewmodel.PropertiesPanelViewModel
import io.codenode.grapheditor.viewmodel.PropertiesPanelViewModelState
import io.codenode.fbpdsl.model.IPColor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * State holder for PropertiesPanel component.
 *
 * Manages the selected node, its properties, validation state, and change tracking.
 */
data class PropertiesPanelState(
    val selectedNode: CodeNode? = null,
    val nodeName: String = selectedNode?.name ?: "",
    val properties: Map<String, String> = selectedNode?.configuration ?: emptyMap(),
    val propertyDefinitions: List<PropertyDefinition> = emptyList(),
    val originalNodeName: String = nodeName,
    val originalProperties: Map<String, String> = properties,
    val validationErrors: Map<String, String> = emptyMap(),
    val onPropertyChanged: ((String, String) -> Unit)? = null,
    val onNodeNameChanged: ((String) -> Unit)? = null,
    val onPortNameChanged: ((String, String) -> Unit)? = null,  // (portId, newName)
    val onPortTypeChanged: ((String, String) -> Unit)? = null  // (portId, typeName)
) {
    /** Whether this is a generic node type */
    val isGenericNode: Boolean get() = selectedNode?.codeNodeType == CodeNodeType.GENERIC
    /** Whether no node is selected */
    val isEmptyState: Boolean get() = selectedNode == null

    /** Whether properties or name have been modified since last save */
    val isDirty: Boolean get() = properties != originalProperties || nodeName != originalNodeName

    /** Whether there are validation errors */
    val hasValidationErrors: Boolean get() = validationErrors.isNotEmpty()

    /** Whether all validation passes */
    val isValid: Boolean get() = validationErrors.isEmpty()

    /**
     * Selects a new node and loads its properties
     */
    fun selectNode(node: CodeNode): PropertiesPanelState {
        return copy(
            selectedNode = node,
            nodeName = node.name,
            properties = node.configuration,
            originalNodeName = node.name,
            originalProperties = node.configuration,
            validationErrors = emptyMap()
        )
    }

    /**
     * Clears the current selection
     */
    fun clearSelection(): PropertiesPanelState {
        return copy(
            selectedNode = null,
            nodeName = "",
            properties = emptyMap(),
            originalNodeName = "",
            originalProperties = emptyMap(),
            validationErrors = emptyMap()
        )
    }

    /**
     * Updates the node name
     */
    fun withNodeName(name: String): PropertiesPanelState {
        return copy(nodeName = name)
    }

    /**
     * Updates a property value
     */
    fun withPropertyValue(key: String, value: String): PropertiesPanelState {
        return copy(properties = properties + (key to value))
    }

    /**
     * Notifies listeners of node name change (for external state sync)
     */
    fun updateNodeName(name: String) {
        onNodeNameChanged?.invoke(name)
    }

    /**
     * Notifies listeners of property change (for external state sync)
     */
    fun updateProperty(key: String, value: String) {
        onPropertyChanged?.invoke(key, value)
    }

    /**
     * Notifies listeners of port name change (for external state sync)
     */
    fun updatePortName(portId: String, newName: String) {
        onPortNameChanged?.invoke(portId, newName)
    }

    /**
     * Notifies listeners of port type change (for external state sync)
     */
    fun updatePortType(portId: String, typeName: String) {
        onPortTypeChanged?.invoke(portId, typeName)
    }

    /**
     * Marks current properties and name as saved (resets dirty state)
     */
    fun markSaved(): PropertiesPanelState {
        return copy(originalNodeName = nodeName, originalProperties = properties)
    }

    /**
     * Validates all properties against their definitions
     */
    fun validate(): PropertiesPanelState {
        val errors = mutableMapOf<String, String>()

        propertyDefinitions.forEach { def ->
            val value = properties[def.name] ?: ""

            // Required check
            if (def.required && value.isBlank()) {
                errors[def.name] = "${def.name} is required"
            }

            // Number range check
            if (def.type == PropertyType.NUMBER && value.isNotBlank()) {
                val numValue = value.toDoubleOrNull()
                if (numValue != null) {
                    def.minValue?.let { min ->
                        if (numValue < min) {
                            errors[def.name] = "${def.name} must be at least $min"
                        }
                    }
                    def.maxValue?.let { max ->
                        if (numValue > max) {
                            errors[def.name] = "${def.name} must be at most $max"
                        }
                    }
                } else {
                    errors[def.name] = "${def.name} must be a valid number"
                }
            }

            // Pattern check
            if (def.pattern != null && value.isNotBlank()) {
                if (!value.matches(Regex(def.pattern))) {
                    errors[def.name] = "${def.name} format is invalid"
                }
            }
        }

        return copy(validationErrors = errors)
    }

    /**
     * Gets the validation error for a specific property
     */
    fun getErrorForProperty(name: String): String? = validationErrors[name]

    companion object {
        /**
         * Derives property definitions from a NodeTypeDefinition's schema
         *
         * Parses JSON Schema to extract property types, constraints, and options.
         */
        fun derivePropertyDefinitions(nodeTypeDef: NodeTypeDefinition): List<PropertyDefinition> {
            val schema = nodeTypeDef.configurationSchema
            if (schema.isNullOrBlank()) {
                // No schema - create basic STRING definitions from defaults
                return nodeTypeDef.defaultConfiguration.keys.map { key ->
                    PropertyDefinition(
                        name = key,
                        type = PropertyType.STRING,
                        required = false
                    )
                }
            }

            return try {
                parseJsonSchema(schema, nodeTypeDef.defaultConfiguration.keys)
            } catch (e: Exception) {
                // Fallback to basic definitions on parse error
                nodeTypeDef.defaultConfiguration.keys.map { key ->
                    PropertyDefinition(
                        name = key,
                        type = PropertyType.STRING,
                        required = false
                    )
                }
            }
        }

        /**
         * Parses a JSON Schema to extract property definitions
         */
        private fun parseJsonSchema(schema: String, defaultKeys: Set<String>): List<PropertyDefinition> {
            val json = Json { ignoreUnknownKeys = true }
            val schemaObj = json.parseToJsonElement(schema).jsonObject

            val properties = schemaObj["properties"]?.jsonObject ?: return emptyList()
            val requiredList = schemaObj["required"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()

            return properties.keys.map { key ->
                val propDef = properties[key]?.jsonObject ?: return@map PropertyDefinition(key, PropertyType.STRING)

                val type = when (propDef["type"]?.jsonPrimitive?.content) {
                    "integer", "number" -> PropertyType.NUMBER
                    "boolean" -> PropertyType.BOOLEAN
                    "string" -> {
                        // Check for enum
                        if (propDef.containsKey("enum")) {
                            PropertyType.DROPDOWN
                        } else {
                            PropertyType.STRING
                        }
                    }
                    else -> PropertyType.STRING
                }

                val options: List<String> = if (type == PropertyType.DROPDOWN) {
                    propDef["enum"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList<String>()
                } else {
                    emptyList<String>()
                }

                val minValue = propDef["minimum"]?.jsonPrimitive?.doubleOrNull
                val maxValue = propDef["maximum"]?.jsonPrimitive?.doubleOrNull
                val pattern = propDef["pattern"]?.jsonPrimitive?.contentOrNull

                PropertyDefinition(
                    name = key,
                    type = type,
                    required = requiredList.contains(key),
                    options = options,
                    minValue = minValue,
                    maxValue = maxValue,
                    pattern = pattern
                )
            }
        }
    }
}

/**
 * Definition for a property in the properties panel
 */
data class PropertyDefinition(
    val name: String,
    val type: PropertyType,
    val required: Boolean = false,
    val options: List<String> = emptyList(),
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val pattern: String? = null,
    val description: String? = null
) {
    /** The UI editor type to use for this property */
    val editorType: EditorType
        get() = when (type) {
            PropertyType.STRING -> EditorType.TEXT_FIELD
            PropertyType.NUMBER -> EditorType.NUMBER_FIELD
            PropertyType.BOOLEAN -> EditorType.CHECKBOX
            PropertyType.DROPDOWN -> EditorType.DROPDOWN
        }
}

/**
 * Property value types
 */
enum class PropertyType {
    STRING,
    NUMBER,
    BOOLEAN,
    DROPDOWN
}

/**
 * UI editor types for properties
 */
enum class EditorType {
    TEXT_FIELD,
    NUMBER_FIELD,
    CHECKBOX,
    DROPDOWN
}

/**
 * PropertiesPanel Composable - displays and edits node properties
 *
 * @param state The current state of the properties panel
 * @param onStateChange Callback when state changes
 * @param modifier Modifier for the panel
 */
@Composable
fun PropertiesPanel(
    state: PropertiesPanelState,
    onStateChange: (PropertiesPanelState) -> Unit,
    ipTypeRegistry: IPTypeRegistry? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colors.surface,
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Header
            PropertiesPanelHeader(
                nodeName = state.selectedNode?.name,
                nodeType = state.selectedNode?.nodeType,
                isDirty = state.isDirty,
                hasErrors = state.hasValidationErrors
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Content
            if (state.isEmptyState) {
                EmptyStateMessage()
            } else {
                PropertiesContent(
                    state = state,
                    onNameChange = { name ->
                        val newState = state.withNodeName(name)
                        onStateChange(newState)
                        state.updateNodeName(name)
                    },
                    onPropertyChange = { key, value ->
                        val newState = state.withPropertyValue(key, value).validate()
                        onStateChange(newState)
                        state.updateProperty(key, value)
                    },
                    onPortNameChange = { portId, newName ->
                        state.updatePortName(portId, newName)
                    },
                    onPortTypeChange = { portId, typeName ->
                        state.updatePortType(portId, typeName)
                    },
                    ipTypeRegistry = ipTypeRegistry,
                    modifier = Modifier.weight(1f)
                )

                // Action buttons
                if (state.isDirty) {
                    PropertiesActionBar(
                        onSave = {
                            onStateChange(state.markSaved())
                        },
                        onReset = {
                            onStateChange(state.copy(
                                properties = state.originalProperties,
                                validationErrors = emptyMap()
                            ))
                        },
                        canSave = state.isValid
                    )
                }
            }
        }
    }
}

/**
 * Header showing node name and type
 */
@Composable
private fun PropertiesPanelHeader(
    nodeName: String?,
    nodeType: String?,
    isDirty: Boolean,
    hasErrors: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Properties",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            if (nodeName != null) {
                Text(
                    text = "$nodeName ($nodeType)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        // Status indicators
        if (isDirty) {
            Text(
                text = "●",
                color = Color(0xFFFFAA00),
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        if (hasErrors) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Validation errors",
                tint = MaterialTheme.colors.error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Empty state message when no node is selected
 */
@Composable
private fun EmptyStateMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Select a node to view its properties",
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
    }
}

/**
 * A port editor row with a name text field and an IP type dropdown.
 */
@Composable
private fun PortEditorRow(
    label: String,
    portName: String,
    portDataTypeName: String,
    ipTypes: List<io.codenode.fbpdsl.model.InformationPacketType>,
    onNameChange: (String) -> Unit,
    onTypeChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Port name text field
            OutlinedTextField(
                value = portName,
                onValueChange = onNameChange,
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 40.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                singleLine = true,
                isError = portName.isBlank(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = MaterialTheme.colors.surface,
                    focusedBorderColor = MaterialTheme.colors.primary,
                    unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                )
            )

            // IP type dropdown
            if (ipTypes.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                val currentType = ipTypes.find { it.typeName == portDataTypeName }
                    ?: ipTypes.find { it.typeName == "Any" }

                Box(modifier = Modifier.width(100.dp)) {
                    OutlinedTextField(
                        value = currentType?.typeName ?: "Any",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 40.dp)
                            .clickable { expanded = true },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                        readOnly = true,
                        singleLine = true,
                        leadingIcon = {
                            if (currentType != null) {
                                val ipColor = currentType.color
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            Color(
                                                red = ipColor.red / 255f,
                                                green = ipColor.green / 255f,
                                                blue = ipColor.blue / 255f
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
                                modifier = Modifier.size(16.dp).clickable { expanded = true }
                            )
                        },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            backgroundColor = MaterialTheme.colors.surface,
                            focusedBorderColor = MaterialTheme.colors.primary,
                            unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                        )
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ipTypes.forEach { ipType ->
                            DropdownMenuItem(
                                onClick = {
                                    onTypeChange(ipType.typeName)
                                    expanded = false
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
                                        color = if (ipType.typeName == portDataTypeName) {
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
            }
        }
    }
}

/**
 * Scrollable content area with property editors
 */
@Composable
private fun PropertiesContent(
    state: PropertiesPanelState,
    onNameChange: (String) -> Unit,
    onPropertyChange: (String, String) -> Unit,
    onPortNameChange: (String, String) -> Unit = { _, _ -> },
    onPortTypeChange: (String, String) -> Unit = { _, _ -> },
    ipTypeRegistry: IPTypeRegistry? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Node Name - always at the top
        PropertyEditorRow(
            definition = PropertyDefinition(
                name = "Name",
                type = PropertyType.STRING,
                required = true,
                description = "Display name for this node"
            ),
            value = state.nodeName,
            error = if (state.nodeName.isBlank()) "Name is required" else null,
            onValueChange = onNameChange
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Generic Node specific properties (port names)
        if (state.isGenericNode) {
            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // Section header
            Text(
                text = "Port Names",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            val ipTypes = ipTypeRegistry?.getAllTypes() ?: emptyList()

            // Input Port Names
            val inputPorts = state.selectedNode?.inputPorts ?: emptyList()
            if (inputPorts.isNotEmpty()) {
                Text(
                    text = "Input Ports",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                inputPorts.forEach { port ->
                    PortEditorRow(
                        label = "Input ${inputPorts.indexOf(port) + 1}",
                        portName = port.name,
                        portDataTypeName = port.dataType.simpleName ?: "Any",
                        ipTypes = ipTypes,
                        onNameChange = { onPortNameChange(port.id, it) },
                        onTypeChange = { onPortTypeChange(port.id, it) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Output Port Names
            val outputPorts = state.selectedNode?.outputPorts ?: emptyList()
            if (outputPorts.isNotEmpty()) {
                Text(
                    text = "Output Ports",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                outputPorts.forEach { port ->
                    PortEditorRow(
                        label = "Output ${outputPorts.indexOf(port) + 1}",
                        portName = port.name,
                        portDataTypeName = port.dataType.simpleName ?: "Any",
                        ipTypes = ipTypes,
                        onNameChange = { onPortNameChange(port.id, it) },
                        onTypeChange = { onPortTypeChange(port.id, it) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        // Required Properties section for GENERIC nodes
        if (state.isGenericNode) {
            Divider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "Required Properties",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // _genericType - display as read-only info
            val genericTypeValue = state.properties["_genericType"] ?: ""
            if (genericTypeValue.isNotBlank()) {
                PropertyEditorRow(
                    definition = PropertyDefinition(
                        name = "Generic Type",
                        type = PropertyType.STRING,
                        required = false,
                        description = "Port configuration type (e.g., in0out2, in2out0)"
                    ),
                    value = genericTypeValue,
                    error = null,
                    onValueChange = { onPropertyChange("_genericType", it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Divider(modifier = Modifier.padding(vertical = 4.dp))
        Spacer(modifier = Modifier.height(4.dp))

        // Configuration properties section (exclude internal properties starting with _)
        val configProperties = state.properties.filterKeys { !it.startsWith("_") }
        val filteredDefinitions = state.propertyDefinitions.filter { !it.name.startsWith("_") }

        // Show header only if there are visible properties
        if (configProperties.isNotEmpty() || filteredDefinitions.isNotEmpty()) {
            Text(
                text = "Configuration",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // If we have property definitions, use them for ordering and types
        if (filteredDefinitions.isNotEmpty()) {
            filteredDefinitions.forEach { def ->
                PropertyEditorRow(
                    definition = def,
                    value = state.properties[def.name] ?: "",
                    error = state.getErrorForProperty(def.name),
                    onValueChange = { onPropertyChange(def.name, it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else if (configProperties.isNotEmpty()) {
            // No definitions - show non-internal properties as text fields
            configProperties.forEach { (key, value) ->
                PropertyEditorRow(
                    definition = PropertyDefinition(key, PropertyType.STRING),
                    value = value,
                    error = state.getErrorForProperty(key),
                    onValueChange = { onPropertyChange(key, it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Single property editor row
 */
@Composable
private fun PropertyEditorRow(
    definition: PropertyDefinition,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Label
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = definition.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            if (definition.required) {
                Text(
                    text = " *",
                    color = MaterialTheme.colors.error,
                    fontSize = 12.sp
                )
            }
        }

        // Description (if provided)
        if (!definition.description.isNullOrBlank()) {
            Text(
                text = definition.description,
                fontSize = 10.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Editor based on type
        when (definition.editorType) {
            EditorType.TEXT_FIELD -> TextFieldEditor(
                value = value,
                onValueChange = onValueChange,
                isError = error != null
            )
            EditorType.NUMBER_FIELD -> NumberFieldEditor(
                value = value,
                onValueChange = onValueChange,
                isError = error != null
            )
            EditorType.CHECKBOX -> CheckboxEditor(
                value = value,
                onValueChange = onValueChange
            )
            EditorType.DROPDOWN -> DropdownEditor(
                value = value,
                options = definition.options,
                onValueChange = onValueChange
            )
        }

        // Error message
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colors.error,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * Action bar with save and reset buttons
 */
@Composable
private fun PropertiesActionBar(
    onSave: () -> Unit,
    onReset: () -> Unit,
    canSave: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(onClick = onReset) {
            Text("Reset")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onSave,
            enabled = canSave
        ) {
            Text("Apply")
        }
    }
}

/**
 * Compact properties panel for integration with main editor
 */
@Composable
fun CompactPropertiesPanel(
    selectedNode: CodeNode?,
    selectedConnection: Connection? = null,
    flowGraph: FlowGraph? = null,
    propertyDefinitions: List<PropertyDefinition> = emptyList(),
    onNodeNameChanged: (String) -> Unit = { _ -> },
    onPropertyChanged: (String, String) -> Unit = { _, _ -> },
    onPortNameChanged: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    // Show connection properties if a connection is selected
    if (selectedConnection != null && flowGraph != null) {
        ConnectionPropertiesPanel(
            connection = selectedConnection,
            flowGraph = flowGraph,
            modifier = modifier.width(280.dp)
        )
    } else {
        var state by remember(selectedNode) {
            mutableStateOf(
                if (selectedNode != null) {
                    PropertiesPanelState(
                        selectedNode = selectedNode,
                        propertyDefinitions = propertyDefinitions,
                        onNodeNameChanged = onNodeNameChanged,
                        onPropertyChanged = onPropertyChanged,
                        onPortNameChanged = onPortNameChanged
                    )
                } else {
                    PropertiesPanelState()
                }
            )
        }

        PropertiesPanel(
            state = state,
            onStateChange = { state = it },
            modifier = modifier.width(280.dp)
        )
    }
}

/**
 * Properties panel for displaying connection information
 */
@Composable
fun ConnectionPropertiesPanel(
    connection: Connection,
    flowGraph: FlowGraph,
    ipTypeRegistry: IPTypeRegistry? = null,
    onIPTypeChanged: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sourceNode = flowGraph.findNode(connection.sourceNodeId)
    val targetNode = flowGraph.findNode(connection.targetNodeId)
    val sourcePort = sourceNode?.outputPorts?.find { it.id == connection.sourcePortId }
    val targetPort = targetNode?.inputPorts?.find { it.id == connection.targetPortId }

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colors.surface,
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Header
            Text(
                text = "Connection Properties",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Connection details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Source information
                Text(
                    text = "Source",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                ConnectionInfoRow(label = "Node", value = sourceNode?.name ?: "Unknown")
                ConnectionInfoRow(label = "Port", value = sourcePort?.name ?: "Unknown")

                Spacer(modifier = Modifier.height(12.dp))

                // Target information
                Text(
                    text = "Target",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                ConnectionInfoRow(label = "Node", value = targetNode?.name ?: "Unknown")
                ConnectionInfoRow(label = "Port", value = targetPort?.name ?: "Unknown")

                Spacer(modifier = Modifier.height(12.dp))

                // Connection ID
                Text(
                    text = "Details",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                ConnectionInfoRow(label = "ID", value = connection.id)

                // IP Type selector
                if (ipTypeRegistry != null) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "IP Type",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val ipTypes = ipTypeRegistry.getAllTypes()
                    var expanded by remember { mutableStateOf(false) }
                    val currentType = connection.ipTypeId?.let { ipTypeRegistry.getById(it) }
                        ?: ipTypes.find { it.typeName == "Any" }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = currentType?.typeName ?: "Any",
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 40.dp)
                                .clickable { expanded = true },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                            readOnly = true,
                            singleLine = true,
                            leadingIcon = {
                                if (currentType != null) {
                                    val ipColor = currentType.color
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(
                                                Color(
                                                    red = ipColor.red / 255f,
                                                    green = ipColor.green / 255f,
                                                    blue = ipColor.blue / 255f
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
                                    modifier = Modifier.size(16.dp).clickable { expanded = true }
                                )
                            },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                backgroundColor = MaterialTheme.colors.surface,
                                focusedBorderColor = MaterialTheme.colors.primary,
                                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                            )
                        )

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            ipTypes.forEach { ipType ->
                                DropdownMenuItem(
                                    onClick = {
                                        onIPTypeChanged?.invoke(connection.id, ipType.id)
                                        expanded = false
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
                                            color = if (ipType.id == connection.ipTypeId ||
                                                (connection.ipTypeId == null && ipType.typeName == "Any")) {
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
                }
            }
        }
    }
}

/**
 * A simple row displaying a label and value for connection info
 */
@Composable
private fun ConnectionInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * ViewModel-based Compact Properties Panel for integration with main editor.
 * Uses PropertiesPanelViewModel for state management.
 *
 * This composable is purely for UI rendering - all business logic and state
 * management is delegated to the PropertiesPanelViewModel.
 *
 * @param viewModel The ViewModel managing state and business logic
 * @param selectedNode The currently selected node (for property definitions)
 * @param selectedConnection The currently selected connection (for connection display)
 * @param flowGraph The flow graph (for connection lookup)
 * @param propertyDefinitions Property definitions for the selected node type
 * @param modifier Modifier for the panel
 */
@Composable
fun CompactPropertiesPanelWithViewModel(
    viewModel: PropertiesPanelViewModel,
    selectedNode: CodeNode?,
    selectedConnection: Connection? = null,
    flowGraph: FlowGraph? = null,
    propertyDefinitions: List<PropertyDefinition> = emptyList(),
    ipTypeRegistry: IPTypeRegistry? = null,
    modifier: Modifier = Modifier
) {
    val vmState by viewModel.state.collectAsState()

    // Sync selection state with ViewModel
    LaunchedEffect(selectedNode, selectedConnection) {
        when {
            selectedConnection != null -> viewModel.selectConnection(selectedConnection)
            selectedNode != null -> viewModel.selectNode(selectedNode)
            else -> viewModel.clearSelection()
        }
    }

    // Show connection properties if a connection is selected
    if (selectedConnection != null && flowGraph != null) {
        ConnectionPropertiesPanel(
            connection = selectedConnection,
            flowGraph = flowGraph,
            ipTypeRegistry = ipTypeRegistry,
            onIPTypeChanged = { connectionId, ipTypeId ->
                viewModel.updateConnectionIPType(connectionId, ipTypeId)
            },
            modifier = modifier.width(280.dp)
        )
    } else {
        // Bridge ViewModel state to legacy PropertiesPanelState for UI rendering
        val legacyState = remember(vmState, selectedNode, propertyDefinitions) {
            if (selectedNode != null) {
                PropertiesPanelState(
                    selectedNode = selectedNode,
                    nodeName = vmState.nodeName.ifEmpty { selectedNode.name },
                    properties = vmState.properties.ifEmpty { selectedNode.configuration },
                    propertyDefinitions = propertyDefinitions,
                    originalNodeName = vmState.originalNodeName,
                    originalProperties = vmState.originalProperties,
                    validationErrors = vmState.validationErrors,
                    onNodeNameChanged = { name -> viewModel.updateNodeName(name) },
                    onPropertyChanged = { key, value -> viewModel.updateProperty(key, value) },
                    onPortNameChanged = { portId, newName -> viewModel.updatePortName(portId, newName) },
                    onPortTypeChanged = { portId, typeName -> viewModel.updatePortType(portId, typeName) }
                )
            } else {
                PropertiesPanelState()
            }
        }

        PropertiesPanel(
            state = legacyState,
            onStateChange = { newState ->
                // Sync changes back to ViewModel
                if (newState.nodeName != legacyState.nodeName) {
                    viewModel.updateNodeName(newState.nodeName)
                }
                newState.properties.forEach { (key, value) ->
                    if (value != legacyState.properties[key]) {
                        viewModel.updateProperty(key, value)
                    }
                }
            },
            ipTypeRegistry = ipTypeRegistry,
            modifier = modifier.width(280.dp)
        )
    }
}
