/*
 * ColorEditor - RGB Color Editor Component
 * Display and edit RGB color values with color swatch preview
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.fbpdsl.model.IPColor

/**
 * Validation result for RGB input parsing.
 */
sealed class ValidationResult {
    data class Valid(
        val color: IPColor,
    ) : ValidationResult()

    data class Error(
        val message: String,
    ) : ValidationResult()
}

/**
 * Validates RGB input string and returns either a valid color or an error message.
 *
 * @param input The user input string in format "R, G, B"
 * @return ValidationResult.Valid with the parsed color, or ValidationResult.Error with message
 */
fun validateRgbInput(input: String): ValidationResult {
    if (input.isBlank()) {
        return ValidationResult.Error("Expected 3 values: R, G, B")
    }

    val parts = input.split(",").map { it.trim() }
    if (parts.size != 3) {
        return ValidationResult.Error("Expected 3 values: R, G, B")
    }

    val values = parts.mapNotNull { it.toIntOrNull() }
    if (values.size != 3) {
        return ValidationResult.Error("Values must be numbers")
    }

    if (values.any { it !in 0..255 }) {
        return ValidationResult.Error("Values must be 0-255")
    }

    return ValidationResult.Valid(IPColor(values[0], values[1], values[2]))
}

/**
 * Converts an IPColor to a Compose Color for rendering.
 *
 * @return Compose Color equivalent of this IPColor
 */
fun IPColor.toComposeColor(): Color = Color(red, green, blue)

/**
 * Color editor component with color swatch and RGB text input.
 *
 * Displays a color preview swatch next to an editable RGB text field.
 * Validates input in real-time and shows error messages for invalid values.
 *
 * @param color Current color value
 * @param onColorChange Callback when color changes (only called for valid input)
 * @param label Label for the editor (default: "Color")
 * @param modifier Modifier for styling
 */
@Composable
fun ColorEditor(
    color: IPColor,
    onColorChange: (IPColor) -> Unit,
    label: String = "Color",
    modifier: Modifier = Modifier,
) {
    // Track text value separately to allow invalid intermediate states
    var textValue by remember(color) {
        mutableStateOf(color.toRgbString())
    }

    // Track error state
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Label
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSurface,
        )

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Color swatch (24x24 dp with 4dp corner radius)
            Box(
                modifier =
                    Modifier
                        .size(24.dp)
                        .background(color.toComposeColor(), RoundedCornerShape(4.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp)),
            )

            Spacer(Modifier.width(8.dp))

            // RGB input field
            OutlinedTextField(
                value = textValue,
                onValueChange = { newValue ->
                    textValue = newValue

                    // Validate and update color if valid
                    when (val result = validateRgbInput(newValue)) {
                        is ValidationResult.Valid -> {
                            error = null
                            onColorChange(result.color)
                        }
                        is ValidationResult.Error -> {
                            error = result.message
                        }
                    }
                },
                isError = error != null,
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.body2,
                colors =
                    TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor =
                            if (error != null) {
                                MaterialTheme.colors.error
                            } else {
                                MaterialTheme.colors.primary
                            },
                        unfocusedBorderColor =
                            if (error != null) {
                                MaterialTheme.colors.error.copy(alpha = 0.5f)
                            } else {
                                Color.Gray
                            },
                    ),
            )
        }

        // Error message
        error?.let { errorMessage ->
            Text(
                text = errorMessage,
                color = MaterialTheme.colors.error,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp, start = 32.dp),
            )
        }
    }
}

/**
 * Compact color editor with inline swatch for use in tight spaces.
 *
 * @param color Current color value
 * @param onColorChange Callback when color changes
 * @param modifier Modifier for styling
 */
@Composable
fun CompactColorEditor(
    color: IPColor,
    onColorChange: (IPColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    var textValue by remember(color) {
        mutableStateOf(color.toRgbString())
    }
    var isValid by remember { mutableStateOf(true) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Small color swatch
        Box(
            modifier =
                Modifier
                    .size(16.dp)
                    .background(color.toComposeColor(), RoundedCornerShape(2.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(2.dp)),
        )

        Spacer(Modifier.width(4.dp))

        // Compact input
        OutlinedTextField(
            value = textValue,
            onValueChange = { newValue ->
                textValue = newValue
                when (val result = validateRgbInput(newValue)) {
                    is ValidationResult.Valid -> {
                        isValid = true
                        onColorChange(result.color)
                    }
                    is ValidationResult.Error -> {
                        isValid = false
                    }
                }
            },
            isError = !isValid,
            singleLine = true,
            modifier = Modifier.width(120.dp),
            textStyle = MaterialTheme.typography.caption,
        )
    }
}

/**
 * Read-only color display with swatch and RGB text.
 *
 * @param color Color to display
 * @param label Optional label
 * @param modifier Modifier for styling
 */
@Composable
fun ColorDisplay(
    color: IPColor,
    label: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Color swatch
        Box(
            modifier =
                Modifier
                    .size(20.dp)
                    .background(color.toComposeColor(), RoundedCornerShape(4.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp)),
        )

        Spacer(Modifier.width(8.dp))

        Column {
            label?.let {
                Text(
                    text = it,
                    fontSize = 10.sp,
                    color = Color.Gray,
                )
            }
            Text(
                text = color.toRgbString(),
                fontSize = 12.sp,
                color = MaterialTheme.colors.onSurface,
            )
        }
    }
}
