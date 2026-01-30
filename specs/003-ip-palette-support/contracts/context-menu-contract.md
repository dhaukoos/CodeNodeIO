# Connection Context Menu Contract

**Component**: ConnectionContextMenu
**Module**: graphEditor
**Date**: 2026-01-30

## Purpose

Display a dropdown menu when right-clicking on a connection, allowing users to change the connection's IP type from a list of available types.

## Interface

```kotlin
@Composable
fun ConnectionContextMenu(
    connectionId: String,
    position: Offset,
    ipTypes: List<InformationPacketType>,
    currentTypeId: String?,
    onTypeSelected: (String, String) -> Unit,  // (connectionId, ipTypeId)
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
)
```

## Props

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| connectionId | String | Yes | ID of the connection being modified |
| position | Offset | Yes | Screen position for menu placement |
| ipTypes | List<InformationPacketType> | Yes | Available IP types |
| currentTypeId | String? | No | Currently selected IP type ID |
| onTypeSelected | (String, String) -> Unit | Yes | Callback with (connectionId, newTypeId) |
| onDismiss | () -> Unit | Yes | Callback when menu should close |

## Visual Structure

```
┌──────────────────────┐
│ Change IP Type       │  <- Header
├──────────────────────┤
│ ■ Any          ✓     │  <- Current type (checkmark)
│ ■ Int                │
│ ■ Double             │  <- Type items with colors
│ ■ Boolean            │
│ ■ String             │
└──────────────────────┘
```

## Behavior Specifications

### Menu Positioning
- **Anchor**: Positioned at right-click location
- **Overflow**: Adjust position if menu would extend off-screen
- **Z-order**: Rendered above all other content

### Type Selection
- **Click**: Invokes `onTypeSelected(connectionId, typeId)`
- **Current type**: Shown with checkmark indicator
- **Auto-close**: Menu closes after selection

### Dismissal
- **Click outside**: Invokes `onDismiss`
- **Escape key**: Invokes `onDismiss`
- **Selection**: Invokes `onDismiss` after `onTypeSelected`

### Menu Items
- **Color indicator**: Small rounded square matching IP type color
- **Type name**: Text label
- **Current indicator**: Checkmark for currently selected type

## Implementation

```kotlin
@Composable
fun ConnectionContextMenu(
    connectionId: String,
    position: Offset,
    ipTypes: List<InformationPacketType>,
    currentTypeId: String?,
    onTypeSelected: (String, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Popup(
        offset = IntOffset(position.x.toInt(), position.y.toInt()),
        onDismissRequest = onDismiss
    ) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier.width(200.dp)) {
                Text(
                    text = "Change IP Type",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp)
                )
                Divider()
                ipTypes.forEach { ipType ->
                    IPTypeMenuItem(
                        ipType = ipType,
                        isSelected = ipType.id == currentTypeId,
                        onClick = {
                            onTypeSelected(connectionId, ipType.id)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun IPTypeMenuItem(
    ipType: InformationPacketType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    ipType.color.toComposeColor(),
                    RoundedCornerShape(2.dp)
                )
                .border(1.dp, Color.Gray, RoundedCornerShape(2.dp))
        )

        Spacer(Modifier.width(8.dp))

        // Type name
        Text(
            text = ipType.typeName,
            modifier = Modifier.weight(1f)
        )

        // Checkmark for current type
        if (isSelected) {
            Text("✓", color = Color(0xFF4CAF50))
        }
    }
}
```

## Test Contracts

```kotlin
class ConnectionContextMenuTest {
    @Test
    fun `displays all IP types`() {
        // Given: 5 IP types
        // When: Context menu is shown
        // Then: All 5 type names are visible
    }

    @Test
    fun `current type shows checkmark`() {
        // Given: Connection has IP type "String"
        // When: Context menu is shown
        // Then: String row has checkmark indicator
    }

    @Test
    fun `clicking type invokes callback`() {
        // Given: Context menu is shown
        // When: User clicks "Int" type
        // Then: onTypeSelected called with (connectionId, "ip_int")
    }

    @Test
    fun `menu closes after selection`() {
        // Given: Context menu is shown
        // When: User selects a type
        // Then: onDismiss is called
    }

    @Test
    fun `clicking outside closes menu`() {
        // Given: Context menu is shown
        // When: User clicks outside menu
        // Then: onDismiss is called
    }

    @Test
    fun `escape key closes menu`() {
        // Given: Context menu is shown
        // When: User presses Escape
        // Then: onDismiss is called
    }

    @Test
    fun `menu positioned at click location`() {
        // Given: Right-click at (100, 200)
        // When: Context menu is shown
        // Then: Menu top-left is at (100, 200)
    }

    @Test
    fun `each type shows correct color indicator`() {
        // Given: Int type with blue color
        // When: Context menu is shown
        // Then: Int row has blue color square
    }
}
```

## Accessibility

- Menu is keyboard navigable
- Items can be selected with Enter key
- Escape key dismisses menu
- Screen reader announces menu title and items
