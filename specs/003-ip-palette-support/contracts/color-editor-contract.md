# Color Editor Contract

**Component**: ColorEditor
**Module**: graphEditor
**Date**: 2026-01-30

## Purpose

Display and edit RGB color values in the Properties Panel. Shows a color preview swatch next to editable RGB text input.

## Interface

```kotlin
@Composable
fun ColorEditor(
    color: IPColor,
    onColorChange: (IPColor) -> Unit,
    label: String = "Color",
    modifier: Modifier = Modifier
)
```

## Props

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| color | IPColor | Yes | Current color value |
| onColorChange | (IPColor) -> Unit | Yes | Callback when color changes |
| label | String | No | Label for the editor (default: "Color") |
| modifier | Modifier | No | Compose modifier for styling |

## Visual Structure

```
┌─────────────────────────────────────┐
│ Color                               │  <- Label
│ ┌────┐  ┌─────────────────────────┐ │
│ │    │  │ 33, 150, 243            │ │  <- Color swatch + RGB input
│ └────┘  └─────────────────────────┘ │
│          Invalid RGB value (0-255)  │  <- Error message (if invalid)
└─────────────────────────────────────┘
```

## Behavior Specifications

### Color Display
- **Swatch size**: 24x24 dp with 4dp corner radius
- **Swatch border**: 1dp border in gray for visibility on light/dark backgrounds
- **Position**: Left of RGB input field

### RGB Input Format
- **Format**: "R, G, B" (e.g., "33, 150, 243")
- **Separators**: Comma with optional spaces
- **Validation**: Each component must be 0-255

### Validation Rules
```kotlin
fun validateRgbInput(input: String): ValidationResult {
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
```

### Update Behavior
- **On blur**: Validate and apply if valid, revert if invalid
- **On Enter**: Same as blur
- **During typing**: Show validation error but don't revert
- **Invalid input**: Show error message, keep previous valid color

## Implementation

```kotlin
@Composable
fun ColorEditor(
    color: IPColor,
    onColorChange: (IPColor) -> Unit,
    label: String = "Color",
    modifier: Modifier = Modifier
) {
    var textValue by remember(color) {
        mutableStateOf(color.toRgbString())
    }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color swatch
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color.toComposeColor(), RoundedCornerShape(4.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
            )

            Spacer(Modifier.width(8.dp))

            // RGB input
            OutlinedTextField(
                value = textValue,
                onValueChange = { newValue ->
                    textValue = newValue
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
                modifier = Modifier.weight(1f)
            )
        }

        // Error message
        error?.let { errorMessage ->
            Text(
                text = errorMessage,
                color = MaterialTheme.colors.error,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
```

## Test Contracts

```kotlin
class ColorEditorTest {
    @Test
    fun `displays color swatch with correct color`() {
        // Given: Color (33, 150, 243)
        // When: ColorEditor renders
        // Then: Swatch shows blue color
    }

    @Test
    fun `displays RGB values in text field`() {
        // Given: Color (33, 150, 243)
        // When: ColorEditor renders
        // Then: Text field shows "33, 150, 243"
    }

    @Test
    fun `valid RGB input updates color`() {
        // Given: ColorEditor with initial color
        // When: User types "100, 100, 100"
        // Then: onColorChange called with IPColor(100, 100, 100)
    }

    @Test
    fun `invalid RGB input shows error`() {
        // Given: ColorEditor displayed
        // When: User types "300, 100, 100"
        // Then: Error message "Values must be 0-255" is shown
    }

    @Test
    fun `non-numeric input shows error`() {
        // Given: ColorEditor displayed
        // When: User types "abc, 100, 100"
        // Then: Error message "Values must be numbers" is shown
    }

    @Test
    fun `wrong number of values shows error`() {
        // Given: ColorEditor displayed
        // When: User types "100, 100"
        // Then: Error message "Expected 3 values: R, G, B" is shown
    }

    @Test
    fun `color swatch updates on valid input`() {
        // Given: ColorEditor with blue color
        // When: User enters "255, 0, 0"
        // Then: Swatch updates to show red
    }

    @Test
    fun `invalid input preserves previous color swatch`() {
        // Given: ColorEditor with blue color (33, 150, 243)
        // When: User types invalid "abc"
        // Then: Swatch still shows blue
    }
}
```

## Integration with Properties Panel

When a connection is selected, the Properties Panel displays:

```kotlin
@Composable
fun ConnectionPropertiesPanel(
    connection: Connection,
    ipType: InformationPacketType?,
    onIpColorChange: (IPColor) -> Unit
) {
    Column {
        // IP Type name (read-only)
        Text("IP Type: ${ipType?.typeName ?: "Any"}")

        Spacer(Modifier.height(8.dp))

        // Color editor (editable)
        ipType?.let { type ->
            ColorEditor(
                color = type.color,
                onColorChange = onIpColorChange,
                label = "Type Color"
            )
        }
    }
}
```
