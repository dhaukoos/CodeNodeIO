/*
 * PropertyEditors - Specialized Input Components for Node Properties
 * Provides text, number, boolean, and dropdown editors for property values
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Parses a hex color string to a Compose Color
 */
private fun parseHexColor(hexColor: String): Color {
    val hex = hexColor.removePrefix("#")
    return when (hex.length) {
        6 -> Color(
            red = hex.substring(0, 2).toInt(16) / 255f,
            green = hex.substring(2, 4).toInt(16) / 255f,
            blue = hex.substring(4, 6).toInt(16) / 255f
        )
        8 -> Color(
            alpha = hex.substring(0, 2).toInt(16) / 255f,
            red = hex.substring(2, 4).toInt(16) / 255f,
            green = hex.substring(4, 6).toInt(16) / 255f,
            blue = hex.substring(6, 8).toInt(16) / 255f
        )
        else -> Color.Gray
    }
}

/**
 * Text field editor for STRING properties
 *
 * @param value Current value
 * @param onValueChange Callback when value changes
 * @param isError Whether the field has a validation error
 * @param placeholder Placeholder text when empty
 * @param modifier Modifier for the field
 */
@Composable
fun TextFieldEditor(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    placeholder: String = "",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp),
        textStyle = TextStyle(fontSize = 12.sp),
        singleLine = true,
        isError = isError,
        placeholder = {
            if (placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                )
            }
        },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            backgroundColor = MaterialTheme.colors.surface,
            focusedBorderColor = MaterialTheme.colors.primary,
            unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
            errorBorderColor = MaterialTheme.colors.error
        )
    )
}

/**
 * Number field editor for NUMBER properties
 *
 * Validates input to only allow numeric values (integers and decimals).
 *
 * @param value Current value as string
 * @param onValueChange Callback when value changes
 * @param isError Whether the field has a validation error
 * @param modifier Modifier for the field
 */
@Composable
fun NumberFieldEditor(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            // Allow empty, digits, one decimal point, and leading minus
            if (newValue.isEmpty() ||
                newValue == "-" ||
                newValue.matches(Regex("^-?\\d*\\.?\\d*$"))) {
                onValueChange(newValue)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp),
        textStyle = TextStyle(fontSize = 12.sp),
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            backgroundColor = MaterialTheme.colors.surface,
            focusedBorderColor = MaterialTheme.colors.primary,
            unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
            errorBorderColor = MaterialTheme.colors.error
        )
    )
}

/**
 * Checkbox editor for BOOLEAN properties
 *
 * @param value Current value as string ("true" or "false")
 * @param onValueChange Callback when value changes
 * @param label Optional label text
 * @param modifier Modifier for the checkbox
 */
@Composable
fun CheckboxEditor(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier
) {
    val isChecked = value.lowercase() in listOf("true", "1", "yes", "on")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable { onValueChange(if (isChecked) "false" else "true") },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { checked ->
                onValueChange(if (checked) "true" else "false")
            },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colors.primary,
                uncheckedColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        )
        if (label != null) {
            Text(
                text = label,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/**
 * Dropdown editor for DROPDOWN/enum properties
 *
 * @param value Current selected value
 * @param options List of available options
 * @param onValueChange Callback when selection changes
 * @param modifier Modifier for the dropdown
 */
@Composable
fun DropdownEditor(
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .clickable { expanded = true },
            textStyle = TextStyle(fontSize = 12.sp),
            readOnly = true,
            singleLine = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select",
                    modifier = Modifier.clickable { expanded = true }
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
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                ) {
                    Text(
                        text = option,
                        fontSize = 12.sp,
                        color = if (option == value) {
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

/**
 * Multi-line text area editor for long text properties
 *
 * @param value Current value
 * @param onValueChange Callback when value changes
 * @param isError Whether the field has a validation error
 * @param maxLines Maximum number of visible lines
 * @param modifier Modifier for the field
 */
@Composable
fun TextAreaEditor(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    maxLines: Int = 5,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp, max = (maxLines * 20 + 20).dp),
        textStyle = TextStyle(fontSize = 12.sp),
        maxLines = maxLines,
        isError = isError,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            backgroundColor = MaterialTheme.colors.surface,
            focusedBorderColor = MaterialTheme.colors.primary,
            unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
            errorBorderColor = MaterialTheme.colors.error
        )
    )
}

/**
 * Color picker editor for color properties
 *
 * Shows a text field with the hex color value and a color preview.
 *
 * @param value Current hex color value (e.g., "#FF5500")
 * @param onValueChange Callback when color changes
 * @param modifier Modifier for the editor
 */
@Composable
fun ColorEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val color = try {
        parseHexColor(value)
    } catch (e: Exception) {
        Color.Gray
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color preview
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color)
                .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.3f))
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Hex input
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                // Accept hex color format
                if (newValue.isEmpty() || newValue.matches(Regex("^#?[0-9A-Fa-f]{0,8}$"))) {
                    onValueChange(if (newValue.startsWith("#")) newValue else "#$newValue")
                }
            },
            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 52.dp),
            textStyle = TextStyle(fontSize = 12.sp),
            singleLine = true,
            placeholder = {
                Text(
                    text = "#RRGGBB",
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                )
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                backgroundColor = MaterialTheme.colors.surface
            )
        )
    }
}

/**
 * Slider editor for numeric range properties
 *
 * @param value Current value as string
 * @param onValueChange Callback when value changes
 * @param minValue Minimum allowed value
 * @param maxValue Maximum allowed value
 * @param steps Number of discrete steps (0 for continuous)
 * @param modifier Modifier for the slider
 */
@Composable
fun SliderEditor(
    value: String,
    onValueChange: (String) -> Unit,
    minValue: Float,
    maxValue: Float,
    steps: Int = 0,
    modifier: Modifier = Modifier
) {
    val floatValue = value.toFloatOrNull() ?: minValue

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = minValue.toString(),
                fontSize = 10.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = floatValue.toString(),
                fontSize = 12.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
            Text(
                text = maxValue.toString(),
                fontSize = 10.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
        }

        Slider(
            value = floatValue.coerceIn(minValue, maxValue),
            onValueChange = { onValueChange(it.toString()) },
            valueRange = minValue..maxValue,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colors.primary,
                activeTrackColor = MaterialTheme.colors.primary
            )
        )
    }
}

/**
 * Read-only display for non-editable properties
 *
 * @param value The value to display
 * @param modifier Modifier for the display
 */
@Composable
fun ReadOnlyDisplay(
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(
                MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
                shape = MaterialTheme.shapes.small
            )
            .border(
                1.dp,
                MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = value,
            fontSize = 12.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
    }
}

/**
 * File browser editor for FILE_PATH properties.
 * Displays a text field with a browse button for selecting files.
 *
 * Provides both manual text entry and file browser dialog functionality.
 * Selected files are converted to relative paths from projectRoot for portability.
 *
 * @param value Current file path value (relative path)
 * @param onValueChange Callback when value changes (receives relative path)
 * @param isError Whether there's a validation error
 * @param errorMessage Optional error message to display below the field
 * @param projectRoot Project root directory for relative path conversion (defaults to current directory)
 * @param modifier Modifier for the editor
 */
@Composable
fun FileBrowserEditor(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null,
    projectRoot: File = File(System.getProperty("user.dir")),
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    // Normalize path separators to forward slashes for portability
                    onValueChange(newValue.replace('\\', '/'))
                },
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 52.dp),
                textStyle = TextStyle(fontSize = 12.sp),
                singleLine = true,
                isError = isError,
                placeholder = {
                    Text(
                        text = "Select a file...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = MaterialTheme.colors.surface,
                    focusedBorderColor = MaterialTheme.colors.primary,
                    unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                    errorBorderColor = MaterialTheme.colors.error
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val selectedFile = showProcessingLogicFileDialog(projectRoot)
                    if (selectedFile != null) {
                        val relativePath = convertToRelativePath(selectedFile, projectRoot)
                        onValueChange(relativePath)
                    }
                },
                modifier = Modifier.height(36.dp)
            ) {
                Text("Browse", fontSize = 12.sp)
            }
        }

        // Error message display
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colors.error,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * Shows a file dialog for selecting ProcessingLogic implementation files.
 *
 * Opens a JFileChooser dialog filtered to show only Kotlin files (*.kt).
 * The dialog starts in the provided project root directory.
 *
 * @param projectRoot The project root directory to start browsing from
 * @return The selected file, or null if the user cancelled
 */
fun showProcessingLogicFileDialog(projectRoot: File): File? {
    val fileChooser = JFileChooser().apply {
        dialogTitle = "Select ProcessingLogic Implementation"
        currentDirectory = projectRoot
        fileFilter = FileNameExtensionFilter("Kotlin Files (*.kt)", "kt")
        isAcceptAllFileFilterUsed = false
    }

    return if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        fileChooser.selectedFile
    } else {
        null
    }
}

/**
 * Converts an absolute file path to a relative path from the project root.
 *
 * Uses forward slashes for cross-platform compatibility.
 * If the file is not under the project root, returns the absolute path.
 *
 * @param file The file to convert
 * @param projectRoot The project root directory
 * @return Relative path with forward slashes, or absolute path if not under project root
 */
fun convertToRelativePath(file: File, projectRoot: File): String {
    val absolutePath = file.absolutePath.replace('\\', '/')
    val rootPath = projectRoot.absolutePath.replace('\\', '/')

    return if (absolutePath.startsWith(rootPath)) {
        absolutePath.removePrefix(rootPath).removePrefix("/")
    } else {
        // File is outside project root, return absolute path
        absolutePath
    }
}

/**
 * Property editor that automatically selects the appropriate editor
 * based on the property definition.
 *
 * @param definition The property definition
 * @param value Current property value
 * @param onValueChange Callback when value changes
 * @param isError Whether there's a validation error
 * @param modifier Modifier for the editor
 */
@Composable
fun AutoPropertyEditor(
    definition: PropertyDefinition,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    when (definition.editorType) {
        EditorType.TEXT_FIELD -> TextFieldEditor(
            value = value,
            onValueChange = onValueChange,
            isError = isError,
            modifier = modifier
        )
        EditorType.NUMBER_FIELD -> {
            // Use slider if we have min/max values
            if (definition.minValue != null && definition.maxValue != null) {
                SliderEditor(
                    value = value,
                    onValueChange = onValueChange,
                    minValue = definition.minValue.toFloat(),
                    maxValue = definition.maxValue.toFloat(),
                    modifier = modifier
                )
            } else {
                NumberFieldEditor(
                    value = value,
                    onValueChange = onValueChange,
                    isError = isError,
                    modifier = modifier
                )
            }
        }
        EditorType.CHECKBOX -> CheckboxEditor(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
        )
        EditorType.DROPDOWN -> DropdownEditor(
            value = value,
            options = definition.options,
            onValueChange = onValueChange,
            modifier = modifier
        )
        EditorType.FILE_BROWSER -> FileBrowserEditor(
            value = value,
            onValueChange = onValueChange,
            isError = isError,
            modifier = modifier
        )
    }
}
