# Quickstart: Collapsible Panels

**Feature**: 042-collapsible-panels
**Date**: 2026-03-07

## Overview

This feature adds collapse/expand toggles to three panels in the graph editor: Node Generator/Node Palette (far left), IP Generator/IP Types (second from left), and Properties Panel (right). The existing RuntimePreviewPanel already has this behavior and serves as the reference pattern.

## Key Files

| File | Role |
|------|------|
| `graphEditor/src/jvmMain/kotlin/ui/CollapsiblePanel.kt` | NEW — Reusable collapsible wrapper composable |
| `graphEditor/src/jvmMain/kotlin/Main.kt` | Panel state variables and layout wiring |
| `graphEditor/src/jvmMain/kotlin/ui/RuntimePreviewPanel.kt` | Reference pattern (existing collapsible panel) |

## Implementation Approach

1. **Extract pattern**: Create `CollapsiblePanel` composable that encapsulates the toggle strip + content visibility pattern from RuntimePreviewPanel
2. **Add state**: Add three `mutableStateOf(true)` variables in Main.kt for the new panels
3. **Wrap panels**: In Main.kt's layout Row, wrap each panel column with CollapsiblePanel
4. **Optionally refactor**: Consider refactoring RuntimePreviewPanel to use CollapsiblePanel internally for consistency

## CollapsiblePanel Usage

```kotlin
CollapsiblePanel(
    isExpanded = isNodePanelExpanded,
    onToggle = { isNodePanelExpanded = !isNodePanelExpanded },
    side = PanelSide.LEFT
) {
    Column {
        NodeGeneratorPanel(...)
        NodePalette(...)
    }
}
```

## Build & Run

```bash
./gradlew :graphEditor:run
```

Verify by clicking the chevron strips on each panel edge to collapse/expand.
