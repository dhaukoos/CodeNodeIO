# Implementation Plan: Port Type Selector

**Branch**: `023-port-type-selector` | **Date**: 2026-02-20 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/023-port-type-selector/spec.md`

## Summary

Add an IP type dropdown selector to the Properties panel for Generic Node ports in the graphEditor. When a user changes a port's type, the change propagates to all attached connections and their remote ports. Connection type changes propagate back to attached ports. The "last change wins" rule ensures the most recent type assignment flows to all connected elements in a single non-recursive pass.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3
**Primary Dependencies**: kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0
**Storage**: .flow.kts files (DSL text-based serialization via FlowGraphSerializer)
**Testing**: kotlin.test + JUnit (graphEditor JVM tests)
**Target Platform**: Desktop (JVM) - graphEditor module
**Project Type**: Multi-module KMP project (graphEditor is JVM-only Compose Desktop app)
**Performance Goals**: Type selection and propagation must complete within a single frame (~16ms)
**Constraints**: Port.dataType uses `KClass<T>` (generic), requiring lookup from IPTypeRegistry; immutable state model (copy-on-write pattern)
**Scale/Scope**: ~5 files modified, 1 new callback + propagation function, ~200 lines of new code

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | PASS | Follows existing PropertiesPanel patterns (DropdownEditor, PropertyEditorRow). Single responsibility: dropdown selection + propagation function. |
| II. Test-Driven Development | PASS | Unit tests for propagation logic (pure state transformation). No complex UI testing needed - follows existing untested composable pattern. |
| III. UX Consistency | PASS | Reuses existing DropdownEditor composable with established styling. Type colors provide additional visual feedback. |
| IV. Performance | PASS | Single-pass propagation on small connection sets (<100). No algorithmic concerns. |
| V. Observability | PASS | Desktop editor - no production monitoring needed. isDirty flag tracks changes for save. |
| Licensing | PASS | No new dependencies. All changes in existing Apache 2.0 codebase. |

**Post-Design Re-check**: All gates still PASS. No complexity violations.

## Project Structure

### Documentation (this feature)

```text
specs/023-port-type-selector/
├── plan.md              # This file
├── research.md          # Phase 0: research decisions
├── data-model.md        # Phase 1: entity relationships
├── quickstart.md        # Phase 1: before/after examples
├── contracts/           # Phase 1: UI interaction contracts
│   └── ui-interactions.md
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/
├── state/
│   └── GraphState.kt              # Add updatePortType() + propagateType()
├── ui/
│   └── PropertiesPanel.kt         # Add IP type dropdown to port rows
├── viewmodel/
│   └── PropertiesPanelViewModel.kt # Add updatePortType() callback + onPortTypeChanged
└── Main.kt                        # Wire onPortTypeChanged callback
```

**Structure Decision**: All changes within the existing graphEditor module. No new files needed - extending existing state management, ViewModel, and UI composables.

## Design Decisions

### D1: Port Type Stored as KClass, Looked Up from IPTypeRegistry

The `Port<T>` model stores `dataType: KClass<T>`. The IPTypeRegistry maps type names to `InformationPacketType` objects which contain `payloadType: KClass<*>`. The dropdown displays `typeName` strings; on selection, the registry resolves the string to a `KClass<*>` which is set on the port.

**Mapping**: `dropdown selection ("Int") → registry.getByTypeName("Int") → type.payloadType (Int::class) → port.dataType`

### D2: Non-Recursive Single-Pass Propagation

When a port type changes:
1. Update the port's `dataType`
2. Find all connections where `sourcePortId == portId` or `targetPortId == portId`
3. Update each connection's `ipTypeId`
4. For each connection, find the remote port (the other end) and update its `dataType`

This is a single pass - no cascading. If port A → conn → port B, changing port A updates conn and port B, but does NOT then trigger port B's propagation logic. This prevents infinite loops and keeps the operation O(connections).

When a connection type changes:
1. Update the connection's `ipTypeId`
2. Find source port and target port
3. Update both ports' `dataType`

Same single-pass rule applies.

### D3: Dropdown Shows Type Name with Color Indicator

The dropdown uses the existing `DropdownEditor` pattern but enhances menu items with a small color swatch (circle or square) next to each type name, using the `IPColor` from `InformationPacketType`. This provides visual consistency with connection colors.

### D4: Callback Chain Follows Existing Pattern

```
PropertiesPanel (UI)
  → PropertiesPanelViewModel.updatePortType(portId, typeName)
    → onPortTypeChanged callback (injected)
      → GraphState.updatePortType(nodeId, portId, typeName)
        → propagateTypeToConnections(portId, ipTypeId, payloadType)
```

This mirrors the existing `updatePortName` → `onPortNameChanged` chain.

## Key Implementation Details

### GraphState.updatePortType()

```
fun updatePortType(nodeId: String, portId: String, typeName: String):
  1. Look up IPType from registry by typeName
  2. Copy the port with new dataType = ipType.payloadType
  3. Copy the node with updated port lists
  4. Update flowGraph (removeNode + addNode)
  5. Find all connections attached to portId
  6. For each connection:
     a. Copy connection with ipTypeId = ipType.id
     b. Find the remote port (other end of connection)
     c. Copy remote port with new dataType
     d. Copy remote node with updated port lists
  7. Update flowGraph with all connection + remote node changes
  8. Set isDirty = true
```

### PropertiesPanel Port Row Layout

Current layout per port:
```
[ Port Name TextField ]
```

New layout per port:
```
[ Port Name TextField ] [ IP Type Dropdown ▼ ]
```

The TextField and Dropdown share the row width (e.g., 60/40 split or weighted).

### Serialization

Port `dataType` is already serialized as `KClass.simpleName` in `FlowGraphSerializer` (e.g., `input("seconds", Int::class)`). Connection `ipTypeId` is already serialized via `withType "{id}"`. No serialization changes needed - both fields persist correctly with existing code.

## Complexity Tracking

No constitution violations. No complexity justifications needed.
