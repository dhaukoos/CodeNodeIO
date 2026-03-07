# Data Model: Collapsible Panels

**Feature**: 042-collapsible-panels
**Date**: 2026-03-07

## Entities

### PanelSide (Enum)

Determines the directional behavior of a collapsible panel.

| Value | Toggle Strip Position | Expanded Chevron | Collapsed Chevron |
|-------|----------------------|------------------|-------------------|
| LEFT  | Right edge of panel  | ChevronLeft      | ChevronRight      |
| RIGHT | Left edge of panel   | ChevronRight     | ChevronLeft       |

### Panel State (In-Memory)

Each collapsible panel has an independent boolean state managed in Main.kt:

| State Variable               | Default | Panel                              |
|-----------------------------|---------|------------------------------------|
| isNodePanelExpanded         | true    | Node Generator / Node Palette      |
| isIPPanelExpanded           | true    | IP Generator / IP Types            |
| isPropertiesPanelExpanded   | true    | Properties Panel                   |
| isRuntimePanelExpanded      | false   | Runtime Preview Panel (existing)   |

Note: `isRuntimePanelExpanded` already exists and defaults to `false`. The three new panel states default to `true` (expanded).

## State Transitions

```
Expanded --[click toggle]--> Collapsed
Collapsed --[click toggle]--> Expanded
```

No other state transitions. Panel state is not affected by:
- Node selection on canvas
- Module switching
- Window resizing
- Other panel collapse/expand actions
