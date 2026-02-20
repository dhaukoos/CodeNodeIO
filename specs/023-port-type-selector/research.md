# Research: Port Type Selector

**Feature**: 023-port-type-selector
**Date**: 2026-02-20

## R1: Port DataType Storage Mechanism

**Decision**: Use `Port.dataType: KClass<T>` directly, resolved from `IPTypeRegistry.getByTypeName()`.

**Rationale**: The Port model already stores `dataType` as a `KClass<T>`. The IPTypeRegistry already maps type names to `InformationPacketType` objects with `payloadType: KClass<*>`. No model changes needed - just a lookup + copy.

**Alternatives Considered**:
- Store `ipTypeId: String` on Port (like Connection does) - rejected because it would duplicate data and require model changes across the entire codebase.
- Store both `dataType` and `ipTypeId` on Port - rejected as unnecessary complexity; `dataType.simpleName` can resolve back to the registry entry.

## R2: Propagation Strategy

**Decision**: Non-recursive single-pass propagation. The function that changes a type immediately updates all directly connected elements but does not re-trigger propagation on those elements.

**Rationale**: Prevents infinite loops in cyclic graphs. Keeps the operation bounded at O(connections) for a given port. The "last change wins" semantic is naturally achieved because each user action triggers exactly one propagation pass.

**Alternatives Considered**:
- Recursive propagation with visited set - rejected as over-engineered for the use case.
- Event-based propagation (observer pattern) - rejected because the graph state is immutable copy-on-write, making event-based updates unnecessarily complex.
- No propagation (manual per-element) - rejected per spec requirement.

## R3: Dropdown UI Pattern

**Decision**: Reuse existing `DropdownEditor` composable from `PropertyEditors.kt`, enhanced with color swatches.

**Rationale**: The graphEditor already has a working `DropdownEditor` (lines 189-247 of PropertyEditors.kt) with the exact interaction pattern needed (OutlinedTextField + DropdownMenu). Using it maintains UX consistency.

**Alternatives Considered**:
- Custom popup with search/filter - rejected as over-engineered for 5 types.
- Radio buttons - rejected; doesn't scale if custom types are added later.

## R4: Existing Infrastructure Audit

| Component | Status | Notes |
|-----------|--------|-------|
| `Port.dataType: KClass<T>` | Ready | Already stores and serializes type |
| `Connection.ipTypeId: String?` | Ready | Already stores and serializes IP type ID |
| `IPTypeRegistry` | Ready | 5 default types, `getByTypeName()` and `getAllTypes()` available |
| `GraphState.updatePortName()` | Pattern to follow | Same immutable copy pattern for `updatePortType()` |
| `GraphState.updateConnectionIPType()` | Ready | Already exists, can be called during propagation |
| `DropdownEditor` composable | Ready | Reusable for port type dropdown |
| `FlowGraphSerializer` | Ready | Already serializes `dataType` and `ipTypeId` |
| `PropertiesPanelViewModel` | Pattern to follow | Needs `updatePortType()` + `onPortTypeChanged` callback |
