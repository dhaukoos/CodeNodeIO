# Quickstart: InformationPacket Palette Support

**Feature**: 003-ip-palette-support
**Date**: 2026-01-30

## Overview

This guide covers the key integration scenarios for the InformationPacket Palette feature. Follow these scenarios to verify the feature is working correctly.

## Scenario 1: Browse IP Types in Palette

**Goal**: View and search available InformationPacket types

### Steps

1. Open the graphEditor application
2. Locate the IP Palette panel (alongside the Node Palette)
3. Observe the default IP types listed:
   - Any (black indicator)
   - Int (blue indicator)
   - Double (purple indicator)
   - Boolean (green indicator)
   - String (orange indicator)
4. Type "Int" in the search field
5. Observe only "Int" type remains visible
6. Clear the search field
7. All types are visible again

### Expected Result

```
┌─ IP Types ──────────────┐
│ [Search types...]       │
├─────────────────────────┤
│ ■ Any                   │  <- Black square
│ ■ Int                   │  <- Blue square
│ ■ Double                │  <- Purple square
│ ■ Boolean               │  <- Green square
│ ■ String                │  <- Orange square
└─────────────────────────┘
```

---

## Scenario 2: View IP Type Definition

**Goal**: See the DSL code definition of an IP type

### Steps

1. Open graphEditor with IP Palette visible
2. Click on "String" in the IP Palette
3. Switch to or view the Textual view

### Expected Result

The Textual view displays:

```kotlin
// InformationPacket Type: String
ipType("String") {
    payloadType = String::class
    color = IPColor(255, 152, 0)
    description = "Text string type"
}
```

---

## Scenario 3: Select a Connection

**Goal**: Click on a connection to select it and view its properties

### Steps

1. Create a graph with at least two connected nodes
2. In the Visual view, click on the connection line between nodes
3. Observe the connection becomes highlighted
4. View the Properties Panel

### Expected Result

```
┌─ Properties ────────────┐
│ Connection              │
├─────────────────────────┤
│ IP Type: Any            │
│                         │
│ Color:                  │
│ ┌────┐ 0, 0, 0         │  <- Black swatch + RGB
│ └────┘                  │
│                         │
│ Source: nodeA:output    │
│ Target: nodeB:input     │
└─────────────────────────┘
```

---

## Scenario 4: Change Connection IP Type

**Goal**: Change a connection's IP type using the context menu

### Steps

1. Create a graph with a connection
2. Right-click on the connection in the Visual view
3. Context menu appears with all IP types listed
4. Select "String" from the menu
5. Menu closes
6. Connection now shows String type color (orange)
7. Properties Panel updates to show "IP Type: String"

### Expected Result

Before:
```
[nodeA]-----(black)-----[nodeB]
```

After selecting String:
```
[nodeA]=====(orange)====[nodeB]
```

---

## Scenario 5: Edit IP Type Color

**Goal**: Modify the color of an IP type via the Properties Panel

### Steps

1. Select a connection with IP type assigned
2. In Properties Panel, locate the Color field
3. Edit the RGB value from "255, 152, 0" to "200, 50, 50"
4. Press Enter or click outside the field
5. Color swatch updates to show new red color
6. Connection in Visual view updates to show new color

### Expected Result

Color swatch changes from orange to red, connection line in canvas reflects new color.

---

## Scenario 6: Invalid RGB Input Handling

**Goal**: Verify validation of RGB color input

### Steps

1. Select a connection with IP type assigned
2. In the Color editor, enter invalid value: "300, 100, 50"
3. Error message appears: "Values must be 0-255"
4. Color swatch retains previous valid color
5. Enter valid value: "200, 100, 50"
6. Error clears, color updates

### Expected Result

```
┌─ Color ─────────────────┐
│ ┌────┐ 300, 100, 50    │  <- Invalid input
│ └────┘                  │
│ Values must be 0-255   │  <- Error message in red
└─────────────────────────┘
```

---

## Scenario 7: Connection Selection Priority

**Goal**: Verify nodes take priority over connections for selection

### Steps

1. Create overlapping node and connection
2. Click on the node (even if connection is nearby)
3. Node is selected (not connection)
4. Properties Panel shows node properties

### Expected Result

Node selection takes precedence. Only clicking directly on connection line (not on/near nodes) selects the connection.

---

## Common Integration Code

### Accessing the IP Type Registry

```kotlin
// Get default registry with 5 Kotlin types
val registry = IPTypeRegistry.withDefaults()

// Get all types
val allTypes = registry.getAllTypes()

// Search for types
val filtered = registry.search("int")  // Returns Int type

// Get specific type
val stringType = registry.getById("ip_string")
```

### Creating Custom IP Type

```kotlin
val customType = InformationPacketType(
    id = "ip_custom",
    typeName = "CustomData",
    payloadType = MyCustomData::class,
    color = IPColor(100, 200, 150),
    description = "My custom data type"
)

registry.register(customType)
```

### Updating Connection IP Type

```kotlin
// In GraphState
graphState.updateConnectionIPType(
    connectionId = "conn_123",
    ipTypeId = "ip_string"
)
```

---

## Troubleshooting

### Connection not selectable

- Ensure click is directly on the connection line
- Check click tolerance (8 pixels from line)
- Verify connection is not obscured by a node

### Context menu not appearing

- Must right-click directly on connection
- Ensure right-click is detected (try secondary button)
- Check that connection exists and is valid

### Color not updating

- Verify RGB format: "R, G, B" (comma-separated)
- Each value must be 0-255
- Check for validation error messages
