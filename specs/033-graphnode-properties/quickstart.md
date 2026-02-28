# Quickstart: View GraphNode Properties

**Feature**: 033-graphnode-properties
**Date**: 2026-02-27

## Overview

Add GraphNode support to the Properties Panel so that selecting a GraphNode displays its name (editable), ports, and child node names. Extract shared name/port UI into a `SharedNodeProperties` composable used by both CodeNode and GraphNode panels.

## Files to Modify

### 1. `graphEditor/src/jvmMain/kotlin/state/GraphState.kt`
- Generalize `updateNodeName()` to handle both `CodeNode` and `GraphNode` via `when` block

### 2. `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`
- Extract `SharedNodeProperties` composable (name TextField + port sections) from `PropertiesContent`
- Refactor `PropertiesContent` to use `SharedNodeProperties` as its uppermost element
- Add `GraphNodePropertiesPanel` composable using `SharedNodeProperties` + child nodes list
- Add `selectedGraphNode: GraphNode?` parameter to `CompactPropertiesPanelWithViewModel`
- Priority logic: IP type > connection > GraphNode > CodeNode > empty state

### 3. `graphEditor/src/jvmMain/kotlin/Main.kt`
- Derive `selectedGraphNode` alongside `selectedNode` from selection state
- Pass `selectedGraphNode` to `CompactPropertiesPanelWithViewModel`
- Wire name change callback to generalized `updateNodeName()`

## Build & Verify

```bash
./gradlew :graphEditor:compileKotlinJvm
```

## Manual Test

1. Open graph editor
2. Create 2+ nodes and group them (creates a GraphNode)
3. Click the GraphNode on the canvas
4. Verify Properties Panel shows: name field, ports, child nodes list
5. Edit name → verify it updates
6. Click a CodeNode → verify panel switches to CodeNode view (with same name/port layout)
7. Click empty canvas → verify panel shows empty state
