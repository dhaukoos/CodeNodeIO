# IP Palette Component Contract

**Component**: IPPalette
**Module**: graphEditor
**Date**: 2026-01-30

## Purpose

Displays a searchable, scrollable list of InformationPacket types with color indicators. Allows users to select an IP type to view its definition in the Textual view.

## Interface

```kotlin
@Composable
fun IPPalette(
    ipTypes: List<InformationPacketType>,
    selectedTypeId: String? = null,
    onTypeSelected: (InformationPacketType) -> Unit = {},
    modifier: Modifier = Modifier
)
```

## Props

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| ipTypes | List<InformationPacketType> | Yes | List of IP types to display |
| selectedTypeId | String? | No | Currently selected type ID |
| onTypeSelected | (InformationPacketType) -> Unit | No | Callback when type is selected |
| modifier | Modifier | No | Compose modifier for styling |

## Visual Structure

```
┌─────────────────────────┐
│ IP Types                │  <- Header
├─────────────────────────┤
│ [Search types...]       │  <- Search field
├─────────────────────────┤
│ ■ Any                   │  <- Type item (color + name)
│ ■ Int                   │
│ ■ Double                │  <- Scrollable list
│ ■ Boolean               │
│ ■ String                │
└─────────────────────────┘
```

## Behavior Specifications

### Search Filtering
- **Input**: User types in search field
- **Filter**: Case-insensitive substring match on typeName
- **Empty result**: Show "No matching types" message
- **Clear**: Empty search shows all types

### Type Selection
- **Click**: Invokes `onTypeSelected` callback
- **Visual**: Selected type gets highlighted background
- **Side effect**: Triggers Textual view to show type definition code

### Color Display
- **Shape**: Small rounded square (8x8 dp with 2dp corner radius)
- **Position**: Left of type name
- **Border**: 1dp border in slightly darker shade for visibility

## Test Contracts

```kotlin
class IPPaletteTest {
    @Test
    fun `displays all provided IP types`() {
        // Given: 5 IP types
        // When: IPPalette is rendered
        // Then: All 5 type names are visible
    }

    @Test
    fun `search filters types by name`() {
        // Given: IP types including "String"
        // When: User types "str" in search
        // Then: Only "String" type is visible
    }

    @Test
    fun `empty search shows all types`() {
        // Given: Search field has text
        // When: User clears search
        // Then: All types are visible
    }

    @Test
    fun `clicking type invokes callback`() {
        // Given: IP types displayed
        // When: User clicks "Int" type
        // Then: onTypeSelected called with Int type
    }

    @Test
    fun `selected type is visually highlighted`() {
        // Given: "String" is selectedTypeId
        // When: IPPalette is rendered
        // Then: String row has highlighted background
    }

    @Test
    fun `color indicator shows correct RGB color`() {
        // Given: Int type with color (33, 150, 243)
        // When: IPPalette is rendered
        // Then: Int row shows blue color indicator
    }

    @Test
    fun `no matches shows empty state message`() {
        // Given: IP types displayed
        // When: User searches "xyz"
        // Then: "No matching types" message is visible
    }
}
```

## Accessibility

- Search field is focusable and keyboard navigable
- Type items are clickable with appropriate touch targets (min 44dp height)
- Color indicators have text alternatives (type name)
- Screen readers announce type names
