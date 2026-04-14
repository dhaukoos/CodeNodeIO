# Research: Architecture IP Types

**Feature**: 073-architecture-ip-types
**Date**: 2026-04-14 (revised)

## R1: IP Type Registration Mechanism

**Decision**: Use the existing filesystem-based IP type discovery system, extended to support `typealias` declarations. Each IP type is a `.kt` file with `@IPType` metadata comment headers. The body contains a `typealias` that references the actual domain class.

**Rationale**: The IP type system is mature and well-tested. Extending the discovery parser to handle `typealias` is a small, targeted change. This keeps architecture IP types in the same discovery pipeline as all other IP types.

**Alternatives considered**:
- Programmatic registration in code: Rejected — bypasses the standard discovery pipeline.
- JSON configuration: Rejected — the project has migrated away from JSON-based IP type storage.
- `data class` with placeholder properties: Rejected — contradicts the "self-documenting" goal. Typealiases reference real domain classes without duplication.

## R2: Placement Tier

**Decision**: Place architecture IP types at the PROJECT tier in `iptypes/src/commonMain/kotlin/io/codenode/iptypes/` (and `jvmMain` for types referencing JVM-only classes).

**Rationale**: Architecture types describe cross-module data flows. PROJECT tier makes them always visible. Some domain classes (ParseResult, ConnectionAnimation, GraphState) are in `jvmMain` source sets, so their typealiases must also be in `jvmMain`.

**Alternatives considered**:
- UNIVERSAL tier (`~/.codenode/iptypes/`): Rejected — project-specific types, not universal.
- MODULE tier: Rejected — spans all modules; hiding behind module loading defeats the purpose.

## R3: Typealias Approach (Revised from Clarification Session)

**Decision**: Architecture IP types use `typealias` declarations referencing actual domain classes. No property duplication. The real domain classes remain the single source of truth.

**Rationale**: A `typealias` like `typealias NodeDescriptors = List<NodeTypeDefinition>` makes the IP type self-documenting — a designer can see that the nodeDescriptors port carries a list of NodeTypeDefinition objects. The types are compilable and potentially usable at runtime.

**Discovery system extension**: IPTypeDiscovery must be extended with a new regex pattern to parse `typealias (\w+)\s*=\s*(.+)` in addition to the existing `data class` pattern. When a typealias is found:
- `typeName` = the alias name (LHS)
- `className` = the target type's fully qualified name (resolved from imports + RHS)
- `properties` = empty list (properties come from the target type, not the alias)
- KClass resolution via `Class.forName()` works the same as for data classes

**File format**:
```kotlin
/*
 * NodeDescriptors - Custom IP Type
 * @IPType
 * @TypeName NodeDescriptors
 * @TypeId ip_nodedescriptors
 * @Color rgb(233, 30, 99)
 * License: Apache 2.0
 */

package io.codenode.iptypes

import io.codenode.fbpdsl.model.NodeTypeDefinition

typealias NodeDescriptors = List<NodeTypeDefinition>
```

**Alternatives considered**:
- `data class` with full property schemas: Rejected — creates duplication and drift.
- `data class` with placeholder `val value: Any? = null`: Rejected — not self-documenting.

## R4: Color Assignment

**Decision**: Assign 14 distinct colors avoiding the 5 reserved built-in colors (black, blue, purple, green, orange).

**Colors planned**:
1. NodeDescriptors — Pink `rgb(233, 30, 99)`
2. IPTypeMetadata — Cyan `rgb(0, 188, 212)`
3. FlowGraphModel — Brown `rgb(121, 85, 72)`
4. LoadedFlowGraph — Deep Orange `rgb(255, 87, 34)`
5. GraphNodeTemplates — Indigo `rgb(63, 81, 181)`
6. RuntimeExecutionState — Teal `rgb(0, 150, 136)`
7. DataFlowAnimations — Lime `rgb(205, 220, 57)`
8. DebugSnapshots — Grey `rgb(158, 158, 158)`
9. EditorGraphState — Deep Purple `rgb(103, 58, 183)`
10. GeneratedOutput — Amber `rgb(255, 193, 7)`
11. GenerationContext — Light Blue `rgb(3, 169, 244)`
12. FilesystemPath — Blue Grey `rgb(96, 125, 139)`
13. ClasspathEntry — Light Green `rgb(139, 195, 74)`
14. IPTypeCommand — Red `rgb(244, 67, 54)`

## R5: architecture.flow.kt Port Type Syntax

**Decision**: Replace `String::class` with the typealias class references in port declarations (e.g., `exposeOutput("nodeDescriptors", NodeDescriptors::class)`). For `typealias` declarations, Kotlin resolves `TypeAlias::class` to the underlying type's KClass at compile time.

**Note**: Since `typealias NodeDescriptors = List<NodeTypeDefinition>` erases to `List::class` at runtime, the IP type registry lookup must match by type ID (from the `@TypeId` annotation), not by KClass. The `.flow.kt` serializer already stores port types by name/ID, so this works with the existing persistence mechanism.

## R6: Existing Port Type Exceptions

**Decision**: `serializedOutput` remains typed as `String::class` — it legitimately carries plain-text `.flow.kt` DSL source code.

## R7: Domain Class Module Locations

Research into where each referenced domain class lives:

| Domain Class | Module | Source Set | Package |
|-------------|--------|------------|---------|
| NodeTypeDefinition | fbpDsl | commonMain | io.codenode.fbpdsl.model |
| InformationPacketType | fbpDsl | commonMain | io.codenode.fbpdsl.model |
| FlowGraph | fbpDsl | commonMain | io.codenode.fbpdsl.model |
| ExecutionState | fbpDsl | commonMain | io.codenode.fbpdsl.model |
| ParseResult | flowGraph-persist | jvmMain | io.codenode.flowgraphpersist.serialization |
| GraphNodeTemplateMeta | flowGraph-persist | commonMain | io.codenode.flowgraphpersist.model |
| ConnectionAnimation | flowGraph-execute | jvmMain | io.codenode.flowgraphexecute |
| GraphState | graphEditor | jvmMain | io.codenode.grapheditor.state |

**Implication**: Types referencing jvmMain classes (ParseResult, ConnectionAnimation, GraphState) must be placed in `iptypes/src/jvmMain/`. Types referencing commonMain classes can be in `iptypes/src/commonMain/`.

## R8: IPTypeDiscovery Parsing Extension

**Current parsing approach** (in `IPTypeDiscovery.kt`):
- `ipTypeMarkerPattern`: `@IPType` — identifies IP type files
- `typeNamePattern`: `@TypeName\s+(\S+)` — captures display name
- `typeIdPattern`: `@TypeId\s+(\S+)` — captures unique ID
- `colorPattern`: `@Color\s+rgb\((\d+),\s*(\d+),\s*(\d+)\)` — captures RGB color
- `packagePattern`: `^package\s+([\w.]+)` — captures package
- `dataClassPattern`: `data\s+class\s+(\w+)\s*\(([^)]*)\)` — captures class name + properties

**New pattern needed**:
- `typealiasPattern`: `typealias\s+(\w+)\s*=\s*(.+)` — captures alias name + target type expression

**Parsing logic change**: After scanning for `@IPType` marker, attempt `dataClassPattern` first (existing behavior). If not found, attempt `typealiasPattern`. If typealias found, create `IPTypeFileMeta` with:
- `typeName` from `@TypeName` (or alias name from LHS)
- `properties` = empty list (typealias has no own properties)
- `className` = resolved from package + alias name

## R9: iptypes Module Setup (Revised from Clarification Session)

**Decision**: Create a new `iptypes` Gradle module in the CodeNodeIO repository root, modeled after the demo project's `iptypes` module. Keep it separate from `flowGraph-types`.

**Rationale**: flowGraph-types is infrastructure (registry, discovery engine, file generator) — the *machinery* for managing IP types. The IP type *definitions themselves* belong in application code. This mirrors how Gradle (build tool) is separate from `build.gradle.kts` (project config). The demo project already establishes this pattern with a standalone `iptypes/` module.

**Alternatives considered**:
- Merge into flowGraph-types: Rejected — mixes infrastructure and data concerns. flowGraph-types manages types; projects define types.
- Place files in flowGraph-types subdirectory: Rejected — same concern mixing, plus would require flowGraph-types to depend on downstream modules.

**Dependencies needed**:
```
commonMain:
  - project(":fbpDsl")           # NodeTypeDefinition, InformationPacketType, FlowGraph, ExecutionState
  - project(":flowGraph-persist") # GraphNodeTemplateMeta

jvmMain:
  - project(":flowGraph-persist") # ParseResult (jvmMain only)
  - project(":flowGraph-execute") # ConnectionAnimation (jvmMain only)
```

**No graphEditor dependency**: EditorGraphState uses `data class` format (not typealias) to avoid a circular dependency. GraphState cannot move out of graphEditor because it depends on `androidx.compose.runtime.*` (mutableStateOf, snapshotFlow), which would break KMP compatibility for all downstream consumers of flowGraph-types.

## R10: EditorGraphState as Data Class

**Decision**: EditorGraphState is a `data class` with descriptive properties (not a typealias to GraphState).

**Rationale**: GraphState in graphEditor depends on Compose runtime. The `iptypes` module must not depend on graphEditor (circular dependency) or Compose (breaks KMP). A data class with meaningful property names (flowGraph, selection, pan, zoom, isDirty) communicates the structure without referencing the Compose-coupled class.

**File format**:
```kotlin
/*
 * EditorGraphState - Custom IP Type
 * @IPType
 * @TypeName EditorGraphState
 * @TypeId ip_editorgraphstate
 * @Color rgb(103, 58, 183)
 * License: Apache 2.0
 */

package io.codenode.iptypes

data class EditorGraphState(
    val flowGraph: String,
    val selection: String,
    val panOffset: String,
    val scale: String,
    val isDirty: Boolean = false
)
```
