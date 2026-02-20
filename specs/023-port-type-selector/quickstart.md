# Quickstart: Port Type Selector

**Feature**: 023-port-type-selector
**Date**: 2026-02-20

## Overview

This feature adds an IP type dropdown to port rows in the graphEditor's Properties panel for Generic Nodes. Selecting a type propagates the change to all connected elements (connections and remote ports), implementing a "last change wins" model.

## Before & After

### Properties Panel - Port Row Layout

**Before**:
```
Input Ports:
  Input 1: [ seconds          ]
  Input 2: [ minutes          ]

Output Ports:
  Output 1: [ elapsedSeconds   ]
```

**After**:
```
Input Ports:
  Input 1: [ seconds          ] [ ● Int     ▼ ]
  Input 2: [ minutes          ] [ ● Int     ▼ ]

Output Ports:
  Output 1: [ elapsedSeconds   ] [ ● Int     ▼ ]
```

### Type Propagation

**Before** (manual, error-prone):
```
1. Change port A type to Int
2. Manually find connection attached to port A
3. Manually update connection's IP type to Int
4. Manually find port B on the other end
5. Manually update port B's type to Int
```

**After** (automatic, single action):
```
1. Change port A type to Int
   → Connection automatically updates to Int
   → Port B automatically updates to Int
```

### Dropdown Interaction

**Before**: No type selector exists. Ports default to Any::class.

**After**:
```
User clicks dropdown on port row:
┌──────────────────┐
│ ● Any            │  ← Currently selected (default)
│ ● Int            │
│ ● Double         │
│ ● Boolean        │
│ ● String         │
└──────────────────┘

User selects "Int":
- Port dataType → Int::class
- All attached connections → ipTypeId = "ip_int"
- All remote ports → dataType = Int::class
```

## User Workflow

### Setting Port Types on a New Node

1. Create a Generic Node on the canvas
2. Add input/output ports via the Properties panel
3. For each port, type a name in the text field
4. Select an IP type from the dropdown next to the name
5. Connect the node to other nodes
6. The connection and remote ports inherit the type automatically

### Changing Types on an Existing Graph

1. Select a node with typed ports
2. Open the Properties panel
3. Change a port's type from the dropdown (e.g., Int → String)
4. All attached connections update to String
5. All remote ports update to String
6. Save the graph - types persist in the .flow.kts file

### Verifying Persistence

1. Set port types on several nodes
2. Save the graph (Ctrl+S)
3. Close and reopen the graph
4. Verify all port types show correct selections in their dropdowns
5. Verify connections still have correct IP type colors

## Files Changed

| File | Change Type | Description |
|------|-------------|-------------|
| `graphEditor/.../state/GraphState.kt` | Modify | Add `updatePortType()` with propagation logic |
| `graphEditor/.../ui/PropertiesPanel.kt` | Modify | Add IP type dropdown to port rows |
| `graphEditor/.../viewmodel/PropertiesPanelViewModel.kt` | Modify | Add `updatePortType()` callback |
| `graphEditor/.../Main.kt` | Modify | Wire `onPortTypeChanged` callback |
