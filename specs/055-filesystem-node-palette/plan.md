# Implementation Plan: Filesystem-Driven Node Palette

**Branch**: `055-filesystem-node-palette` | **Date**: 2026-03-19 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/055-filesystem-node-palette/spec.md`

## Summary

Restructure the Node Palette and Node Generator to use a unified category system based on `CodeNodeType` (9 values: SOURCE, SINK, TRANSFORMER, FILTER, SPLITTER, MERGER, VALIDATOR, API_ENDPOINT, DATABASE). Remove the separate `NodeCategory` (4-value) and `NodeTypeDefinition.NodeCategory` (7-value) enums. The palette displays only filesystem-discovered CodeNodes (no hardcoded samples or templates), grouped by CodeNodeType, showing only populated categories. The Node Generator dropdown always shows all 9 types.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3
**Primary Dependencies**: kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0
**Storage**: N/A (filesystem-based discovery of .kt source files)
**Testing**: kotlin.test + kotlinx-coroutines-test (runTest)
**Target Platform**: JVM Desktop (Compose Desktop)
**Project Type**: Multi-module KMP
**Performance Goals**: Palette load < 2 seconds with 100 CodeNode files
**Constraints**: Backward-compatible .flow.kts deserialization for CUSTOM/GENERIC values
**Scale/Scope**: ~70 files affected across 6 modules (fbpDsl, graphEditor, StopWatch, UserProfiles, GeoLocations, Addresses, EdgeArtFilter)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Consolidating 3 enums into 1 improves maintainability and removes confusing mapping layers |
| II. Test-Driven Development | PASS | Existing test files will be updated; no new untested code paths |
| III. User Experience Consistency | PASS | Palette categories become more descriptive (9 specific types vs 7 generic UI categories) |
| IV. Performance Requirements | PASS | Filesystem scan on launch only; palette load target < 2s |
| V. Observability & Debugging | PASS | Silent skip for malformed files; no new logging requirements |
| Licensing & IP | PASS | No new dependencies introduced |

**Gate result**: PASS — no violations.

## Project Structure

### Documentation (this feature)

```text
specs/055-filesystem-node-palette/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (files to modify)

```text
# Core enum changes (fbpDsl module)
fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt           # Remove CUSTOM, GENERIC from CodeNodeType
fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/NodeTypeDefinition.kt  # Remove NodeCategory enum; change category field to CodeNodeType
fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/CodeNodeDefinition.kt # Remove NodeCategory enum; update category + toNodeTypeDefinition()
fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactory.kt # Update category references

# Graph editor (graphEditor module)
graphEditor/src/jvmMain/kotlin/Main.kt                                       # Remove createSampleNodeTypes(), update palette wiring
graphEditor/src/jvmMain/kotlin/ui/NodePalette.kt                             # Group by CodeNodeType, show only populated categories
graphEditor/src/jvmMain/kotlin/ui/NodeGeneratorPanel.kt                      # Dropdown uses CodeNodeType (9 values, always all shown)
graphEditor/src/jvmMain/kotlin/ui/DragAndDropHandler.kt                      # Remove category mapping; use CodeNodeType directly
graphEditor/src/jvmMain/kotlin/viewmodel/NodePaletteViewModel.kt             # expandedCategories: Set<CodeNodeType>
graphEditor/src/jvmMain/kotlin/viewmodel/NodeGeneratorViewModel.kt           # category: CodeNodeType, update generateCodeNodeContent()
graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/NodeDefinitionRegistry.kt # Update NodeTemplateMeta, parsing, conversion

# CodeNode implementation files (import changes: NodeCategory → CodeNodeType)
StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/nodes/TimerEmitterCodeNode.kt
StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/nodes/TimeIncrementerCodeNode.kt
StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/nodes/DisplayReceiverCodeNode.kt
UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/nodes/*.kt       # 3 files
GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/nodes/*.kt       # 3 files
Addresses/src/commonMain/kotlin/io/codenode/addresses/nodes/*.kt             # 3 files
EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/nodes/*.kt     # 6 files
nodes/src/commonMain/kotlin/io/codenode/nodes/*.kt                           # 2 files

# Test files (category reference updates)
fbpDsl/src/commonTest/kotlin/model/PropertyConfigurationTest.kt
fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/factory/GenericNodeTypeFactoryTest.kt
graphEditor/src/jvmTest/kotlin/ui/GenericNodePaletteTest.kt
graphEditor/src/jvmTest/kotlin/ui/NodePaletteTest.kt
graphEditor/src/jvmTest/kotlin/ui/PropertiesPanelTest.kt
graphEditor/src/jvmTest/kotlin/viewmodel/NodePaletteViewModelTest.kt
graphEditor/src/jvmTest/kotlin/viewmodel/NodeGeneratorViewModelTest.kt
```

**Structure Decision**: Existing KMP multi-module structure. No new modules or directories. This is a refactor across existing files.

## Key Design Decisions

### 1. CodeNodeType as Single Category System

Remove `CodeNodeDefinition.NodeCategory` (4 values) and `NodeTypeDefinition.NodeCategory` (7 values). `CodeNodeType` becomes the sole category enum used everywhere.

**CodeNodeType final values (9)**:
SOURCE, SINK, TRANSFORMER, FILTER, SPLITTER, MERGER, VALIDATOR, API_ENDPOINT, DATABASE

**Removed**: CUSTOM, GENERIC

### 2. PROCESSOR → CodeNodeType Mapping

The old `NodeCategory.PROCESSOR` (2+ inputs and/or 2+ outputs) doesn't exist in CodeNodeType. Existing CodeNodeDefinition files using PROCESSOR must be remapped:

| Old Category | Port Pattern | New CodeNodeType |
|-------------|-------------|-----------------|
| PROCESSOR | 2+ in, 1 out | MERGER |
| PROCESSOR | 1 in, 2+ out | SPLITTER |
| PROCESSOR | 2+ in, 2+ out | TRANSFORMER |

Existing implementations:
- `TimeIncrementerCodeNode` (2 in, 2 out) → TRANSFORMER
- `UserProfileRepositoryCodeNode` (2 in, 0 out) → SINK (already SINK-like)
- `GeoLocationRepositoryCodeNode` (2 in, 0 out) → SINK
- `AddressRepositoryCodeNode` (2 in, 0 out) → SINK

### 3. NodeTypeDefinition.category Field Type Change

`NodeTypeDefinition.category` changes from `NodeTypeDefinition.NodeCategory` to `CodeNodeType`. This eliminates all mapping/conversion code in:
- `CodeNodeDefinition.toNodeTypeDefinition()` — no more `paletteCategory` when-expression
- `NodeDefinitionRegistry.templateToNodeTypeDefinition()` — no more category mapping
- `DragAndDropHandler.createNodeFromType()` — use `nodeType.category` directly
- `Main.kt` node creation — use `nodeType.category` directly

### 4. Palette Shows Only Populated Categories

The palette filters out CodeNodeType categories with zero discovered nodes. The Node Generator always shows all 9 in its dropdown.

### 5. Backward-Compatible Deserialization

`.flow.kts` files may contain `nodeType = "CUSTOM"` or `nodeType = "GENERIC"`. The deserializer in `FlowGraphDsl.kt` already falls back to `CodeNodeType.CUSTOM` for unknown values. After removing CUSTOM/GENERIC, update the fallback to map:
- "CUSTOM" → TRANSFORMER (reasonable default)
- "GENERIC" → TRANSFORMER (reasonable default)
- Unknown → TRANSFORMER (existing default)

### 6. Remove Hardcoded Sample Nodes

Delete `createSampleNodeTypes()` in Main.kt (~185 lines). The palette is entirely driven by `registry.getAllForPalette()`.
