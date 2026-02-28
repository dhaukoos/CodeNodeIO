# Implementation Plan: View GraphNode Properties

**Branch**: `033-graphnode-properties` | **Date**: 2026-02-27 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/033-graphnode-properties/spec.md`

## Summary

When a GraphNode is selected on the canvas, the Properties Panel displays its editable name, input/output ports, and a read-only list of child node names. Currently GraphNodes are excluded because of explicit `as? CodeNode` casts at three levels (Main.kt selection, GraphState.updateNodeName, PropertiesPanelViewModel). The fix extracts a `SharedNodeProperties` composable (name TextField + port sections) shared by both CodeNode and GraphNode panels, adds a `GraphNodePropertiesPanel` that uses it and appends a child nodes list, and generalizes `updateNodeName()` to handle both node types.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3
**Primary Dependencies**: Compose Material (OutlinedTextField, Text, Surface, Column, Row), kotlinx-coroutines 1.8.0
**Storage**: N/A (in-memory FlowGraph state)
**Testing**: Manual verification via `./gradlew :graphEditor:compileKotlinJvm`
**Target Platform**: JVM Desktop (Compose Desktop)
**Project Type**: Multiplatform desktop application
**Performance Goals**: Immediate UI response on node selection
**Constraints**: Must not break existing CodeNode, Connection, or IP Type properties panel behavior
**Scale/Scope**: 3 files modified, ~80 lines added

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | New composable follows established patterns; single responsibility |
| II. Test-Driven Development | PASS | Compile verification; UI composable follows existing untested pattern |
| III. User Experience Consistency | PASS | Reuses existing port display components; consistent panel layout |
| IV. Performance Requirements | PASS | No performance-critical paths; simple property display |
| V. Observability & Debugging | N/A | UI-only change, no production services |
| Licensing & IP | PASS | No new dependencies |

**Post-Phase 1 re-check**: All gates still pass. No new dependencies, patterns, or complexity introduced.

## Project Structure

### Documentation (this feature)

```text
specs/033-graphnode-properties/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0: research findings
├── data-model.md        # Phase 1: data model (no new entities)
├── quickstart.md        # Phase 1: quickstart guide
└── checklists/
    └── requirements.md  # Spec quality checklist
```

### Source Code (repository root)

```text
graphEditor/src/jvmMain/kotlin/
├── state/
│   └── GraphState.kt           # Generalize updateNodeName() for both CodeNode and GraphNode
├── ui/
│   └── PropertiesPanel.kt      # Add GraphNodePropertiesPanel composable; update CompactPropertiesPanelWithViewModel
└── Main.kt                     # Derive selectedGraphNode; pass to panel; wire name callback
```

**Structure Decision**: All changes are within the existing `graphEditor` module. No new files or directories needed. `SharedNodeProperties`, `GraphNodePropertiesPanel`, and the refactored CodeNode panel are all in `PropertiesPanel.kt` alongside the existing `ConnectionPropertiesPanel` and `IPTypePropertiesPanel`, sharing access to the private `PortEditorRow` helper.

## Implementation Steps

### Step 1: Generalize `GraphState.updateNodeName()`

**File**: `graphEditor/src/jvmMain/kotlin/state/GraphState.kt`

Current code casts to `CodeNode` only. Change to handle both types via `when`:

```kotlin
fun updateNodeName(nodeId: String, newName: String) {
    val node = flowGraph.findNode(nodeId) ?: return
    val updatedNode = when (node) {
        is CodeNode -> node.copy(name = newName)
        is GraphNode -> node.copy(name = newName)
    }
    flowGraph = flowGraph.removeNode(nodeId).addNode(updatedNode)
    isDirty = true
}
```

### Step 2: Extract `SharedNodeProperties` composable

**File**: `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`

Extract the name TextField and port sections from `PropertiesContent` into a new `SharedNodeProperties` composable that works with any `Node` type:

```kotlin
@Composable
private fun SharedNodeProperties(
    nodeName: String,
    inputPorts: List<Port<*>>,
    outputPorts: List<Port<*>>,
    onNameChange: (String) -> Unit,
    onPortNameChange: (String, String) -> Unit,
    onPortTypeChange: (String, String) -> Unit,
    ipTypeRegistry: IPTypeRegistry?,
    portIPTypeNames: Map<String, String>
)
```

Contains:
- Name `PropertyEditorRow` (editable, required, "Display name for this node")
- "Port Names" section header + divider
- Input Ports list (each via `PortEditorRow`)
- Output Ports list (each via `PortEditorRow`)

### Step 3: Refactor `PropertiesContent` to use `SharedNodeProperties`

**File**: `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`

Replace the inline name and port code in `PropertiesContent` with a call to `SharedNodeProperties`, keeping the CodeNode-specific sections below (generic type config, configuration properties).

### Step 4: Add `GraphNodePropertiesPanel` composable

**File**: `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`

New composable following the `ConnectionPropertiesPanel` pattern:
- Header: "GraphNode Properties"
- `SharedNodeProperties` (name + ports)
- "Child Nodes" section: read-only list of child node names
- Empty state message if no child nodes

Parameters:
- `graphNode: GraphNode`
- `onNameChanged: (String) -> Unit`
- `onPortNameChanged: (String, String) -> Unit`
- `onPortTypeChanged: (String, String) -> Unit`
- `ipTypeRegistry: IPTypeRegistry?`
- `portIPTypeNames: Map<String, String>`
- `modifier: Modifier`

### Step 5: Update `CompactPropertiesPanelWithViewModel`

**File**: `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`

Add `selectedGraphNode: GraphNode?` parameter. Update priority logic:
1. IP type selected (no node/connection) → `IPTypePropertiesPanel`
2. Connection selected → `ConnectionPropertiesPanel`
3. GraphNode selected → `GraphNodePropertiesPanel` (new)
4. CodeNode selected → existing `PropertiesPanel`
5. Nothing selected → empty state

### Step 6: Update `Main.kt` selection derivation

**File**: `graphEditor/src/jvmMain/kotlin/Main.kt`

- Derive `selectedGraphNode: GraphNode?` alongside existing `selectedNode: CodeNode?`
- Pass `selectedGraphNode` to `CompactPropertiesPanelWithViewModel`
- Wire `onGraphNodeNameChanged` callback to `graphState.updateNodeName()`
- Wire port name/type change callbacks to existing `graphState.updatePortName()` / `graphState.updatePortType()`
