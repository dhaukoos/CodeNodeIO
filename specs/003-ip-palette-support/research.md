# Research: InformationPacket Palette Support

**Feature**: 003-ip-palette-support
**Date**: 2026-01-30

## Research Questions

### 1. How should IP type colors be represented and stored?

**Decision**: Use RGB color representation with a dedicated `IPColor` data class

**Rationale**:
- RGB (0-255 per channel) is universally understood and easy to edit
- Compose Desktop uses `Color` which accepts RGB values
- Simple data class allows serialization to .flow.kts files
- Matches the spec requirement for "text-editable RGB value"

**Alternatives Considered**:
- **Hex strings (#RRGGBB)**: Harder for users to edit manually, requires parsing
- **HSL/HSV**: More intuitive for color picking but spec specifically asks for RGB
- **Named colors**: Limited palette, not extensible

**Implementation**:
```kotlin
data class IPColor(
    val red: Int = 0,    // 0-255
    val green: Int = 0,  // 0-255
    val blue: Int = 0    // 0-255
) {
    init {
        require(red in 0..255) { "Red must be 0-255" }
        require(green in 0..255) { "Green must be 0-255" }
        require(blue in 0..255) { "Blue must be 0-255" }
    }

    fun toComposeColor(): Color = Color(red, green, blue)
    fun toRgbString(): String = "$red, $green, $blue"

    companion object {
        val BLACK = IPColor(0, 0, 0)
        fun fromRgbString(rgb: String): IPColor { ... }
    }
}
```

### 2. Where should IP type definitions be stored?

**Decision**: Create `InformationPacketType` class in fbpDsl module with a registry in graphEditor

**Rationale**:
- Model definition belongs in `fbpDsl` (shared across modules)
- UI registry with default types belongs in `graphEditor`
- Follows existing pattern of NodeTypeDefinition in fbpDsl, NodePalette in graphEditor
- Allows future extension for custom IP types

**Alternatives Considered**:
- **Everything in graphEditor**: Would break separation of concerns, can't reuse in other modules
- **Everything in fbpDsl**: Would couple model to UI concerns (registry, defaults)
- **Separate ip-types module**: Over-engineering for 5 default types

### 3. How should connection selection work with existing click handling?

**Decision**: Extend existing gesture detection to include connection hit-testing

**Rationale**:
- FlowGraphCanvas already has `detectTapGestures` and `detectDragGestures`
- Connections are drawn as Bezier curves - need hit-testing along curve
- Selection state already exists in GraphState (`selectedConnectionIds`)
- Click tolerance of ~5-10 pixels for usability

**Implementation Approach**:
1. Add `findConnectionAtPosition()` function (similar to `findNodeAtPosition()`)
2. Use parametric Bezier sampling for hit detection
3. Prioritize node selection over connection selection (nodes are on top)
4. Update `onTap` handler to check connections if no node hit

### 4. How should the right-click context menu be implemented in Compose Desktop?

**Decision**: Use Compose `ContextMenuArea` or custom `Popup` with `DropdownMenu`

**Rationale**:
- Compose Desktop supports right-click via `PointerButton.Secondary`
- `DropdownMenu` provides native-looking menu styling
- Can position menu at click location using `Popup`
- Follows Material Design patterns already used in the app

**Implementation**:
```kotlin
var showContextMenu by remember { mutableStateOf(false) }
var contextMenuPosition by remember { mutableStateOf(Offset.Zero) }
var contextMenuConnectionId by remember { mutableStateOf<String?>(null) }

// In pointer input handler:
if (button == PointerButton.Secondary) {
    val connection = findConnectionAtPosition(...)
    if (connection != null) {
        contextMenuConnectionId = connection.id
        contextMenuPosition = position
        showContextMenu = true
    }
}

// Render menu:
if (showContextMenu) {
    Popup(offset = contextMenuPosition.toIntOffset()) {
        DropdownMenu(expanded = showContextMenu, onDismissRequest = { showContextMenu = false }) {
            ipTypes.forEach { ipType ->
                DropdownMenuItem(onClick = { changeConnectionType(contextMenuConnectionId, ipType) }) {
                    IPTypeMenuItem(ipType)
                }
            }
        }
    }
}
```

### 5. Default color assignments for the 5 Kotlin types

**Decision**: Assign distinct, accessible colors to each default type

**Rationale**:
- Colors should be distinguishable for accessibility
- Should work on both light and dark backgrounds
- Black for Any (as specified), distinctive colors for others

**Color Assignments**:
| Type | Color | RGB | Rationale |
|------|-------|-----|-----------|
| Any | Black | (0, 0, 0) | Spec requirement, neutral/universal |
| Int | Blue | (33, 150, 243) | Common convention for integers |
| Double | Purple | (156, 39, 176) | Distinguishes from Int |
| Boolean | Green | (76, 175, 80) | True/false, go/stop metaphor |
| String | Orange | (255, 152, 0) | Text/string convention in many IDEs |

### 6. How to display IP type code in Textual view?

**Decision**: Generate Kotlin DSL representation of IP type definition

**Rationale**:
- Textual view already displays flow graph code
- IP types can be represented as Kotlin code snippets
- Follows existing pattern of showing DSL code

**Example Output**:
```kotlin
// InformationPacket Type: String
ipType("String") {
    payloadType = String::class
    color = IPColor(255, 152, 0)  // Orange
}
```

## Integration Points

### Existing Code to Modify

1. **InformationPacket.kt**: No changes needed - existing model is fine
2. **Connection.kt**: Add optional `ipTypeId: String?` property
3. **GraphState.kt**: Already has `selectedConnectionIds` - just need to wire up
4. **FlowGraphCanvas.kt**: Add connection hit-testing and right-click handling
5. **PropertiesPanel.kt**: Add connection properties display mode

### New Files Required

1. **InformationPacketType.kt** (fbpDsl): IP type definition with color
2. **IPPalette.kt** (graphEditor): UI palette component
3. **IPTypeRegistry.kt** (graphEditor): Registry with default types
4. **ColorEditor.kt** (graphEditor): RGB editor component
5. **ConnectionContextMenu.kt** (graphEditor): Right-click menu

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Connection hit detection performance | Low | Medium | Use spatial partitioning if needed, sample curve at 10 points |
| Color accessibility issues | Medium | Low | Test with colorblind simulation, provide text labels |
| Context menu platform differences | Low | Low | Compose Desktop handles this; test on macOS/Windows |
| Serialization backward compatibility | Medium | High | Make ipTypeId optional with null default |
