# Implementation Plan: Save GraphNodes

**Branch**: `063-save-graphnodes` | **Date**: 2026-04-01 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/063-save-graphnodes/spec.md`

## Summary

Enable users to save GraphNode compositions to the Node Palette for reuse. Saved GraphNodes are persisted as `.flow.kts` template files in a three-tier filesystem (Module/Project/Universal), discovered on startup, displayed in a dedicated "GraphNodes" palette section with visually-distinct cards, and instantiated via drag-and-drop as independent deep copies. The Properties panel gains Add/Remove Palette controls with level selection. When child nodes exist at more specific levels than the target save level, a promotion dialog copies those definitions to the target level.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop 1.7.3 (UI), kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0
**Storage**: Filesystem — `.flow.kts` template files at three tier locations
**Testing**: `./gradlew :graphEditor:jvmTest` and `./gradlew :fbpDsl:jvmTest` (kotlin.test)
**Target Platform**: JVM Desktop (macOS/Linux/Windows)
**Project Type**: KMP multi-module (graphEditor, fbpDsl, kotlinCompiler modules)
**Performance Goals**: Template save/load < 500ms; palette discovery < 1s on startup with dozens of templates
**Constraints**: No new external dependencies; must work with existing FlowGraphSerializer/FlowKtParser infrastructure
**Scale/Scope**: Tens to low hundreds of saved GraphNode templates across all tiers

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Research Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Feature adds new classes following existing patterns (Registry, ViewModel, Panel). Single responsibility maintained. |
| II. Test-Driven Development | PASS | Unit tests for serialization, discovery, and promotion logic. Manual tests for UI integration. |
| III. User Experience Consistency | PASS | Reuses existing PlacementLevel selector pattern, GraphNode blue color scheme, and palette card layout conventions. |
| IV. Performance Requirements | PASS | Lightweight metadata parsing for discovery; full deserialization only on instantiation. No new database or network calls. |
| V. Observability & Debugging | PASS | Errors during save/load/promotion are user-facing dialogs. No background failures. |
| Licensing | PASS | No new dependencies. All code is Apache 2.0 project code. |

### Post-Design Re-Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | GraphNodeTemplateRegistry follows same pattern as NodeDefinitionRegistry. Serialization reuses FlowGraphSerializer. |
| II. Test-Driven Development | PASS | Tests cover: template serialization round-trip, metadata parsing, tier precedence, promotion logic, ID remapping on instantiation. |
| III. User Experience Consistency | PASS | Blue-tinted palette cards match GraphNodeRenderer canvas styling. Level selector matches NodeGeneratorPanel/IPGeneratorPanel pattern. |
| IV. Performance Requirements | PASS | Metadata header parsing is O(1) per file (first ~10 lines). Full parse only on drag-to-canvas. |
| V. Observability & Debugging | PASS | Promotion dialog explicitly lists affected nodes. Save/remove confirmations prevent accidental actions. |

No gate violations. No complexity tracking needed.

## Project Structure

### Documentation (this feature)

```text
specs/063-save-graphnodes/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0: Technical decisions
├── data-model.md        # Phase 1: Entity definitions
├── quickstart.md        # Phase 1: Integration scenarios
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
# New files for this feature
graphEditor/src/jvmMain/kotlin/
├── io/codenode/grapheditor/state/
│   └── GraphNodeTemplateRegistry.kt     # Discovery, caching, save/remove
├── model/
│   └── GraphNodeTemplateMeta.kt         # Lightweight metadata model
├── viewmodel/
│   └── GraphNodePaletteViewModel.kt     # State for GraphNode palette section
├── ui/
│   └── GraphNodePaletteSection.kt       # "GraphNodes" dropdown + distinct cards
└── serialization/
    └── GraphNodeTemplateSerializer.kt   # Save/load .flow.kts with metadata header

# Modified files
graphEditor/src/jvmMain/kotlin/
├── ui/
│   ├── NodePalette.kt                   # Add "GraphNodes" collapsible section
│   └── PropertiesPanel.kt              # Add Palette section for GraphNode selection
├── viewmodel/
│   ├── NodePaletteViewModel.kt          # Integrate GraphNode search results
│   └── PropertiesPanelViewModel.kt      # Add palette save/remove actions
├── io/codenode/grapheditor/state/
│   └── NodeDefinitionRegistry.kt        # (Reference for pattern; may not need changes)
└── Main.kt                              # Wire GraphNodeTemplateRegistry into app state

# Existing files referenced (read-only patterns)
graphEditor/src/jvmMain/kotlin/
├── model/PlacementLevel.kt              # Reused as-is
├── state/IPTypeDiscovery.kt             # Pattern reference for three-tier scanning
├── state/IPTypeFileGenerator.kt         # Pattern reference for tier path resolution
├── viewmodel/NodeGeneratorViewModel.kt  # Pattern reference for level selector
├── serialization/FlowGraphSerializer.kt # Reused for GraphNode DSL generation
├── serialization/FlowKtParser.kt        # Reused for GraphNode DSL parsing
└── ui/DragAndDropHandler.kt             # Extended for GraphNode instantiation

# Test files
graphEditor/src/jvmTest/kotlin/
├── state/GraphNodeTemplateRegistryTest.kt
├── serialization/GraphNodeTemplateSerializerTest.kt
└── model/GraphNodeTemplateMetaTest.kt
```

**Structure Decision**: All changes are in the existing `graphEditor` module (JVM Desktop). The `fbpDsl` module (KMP commonMain) requires no changes — GraphNode model is already `@Serializable` and FlowGraphSerializer already handles GraphNode serialization. New files follow the existing package structure: `state/` for registries, `model/` for data classes, `viewmodel/` for state management, `ui/` for composables, `serialization/` for file I/O.

### Filesystem Layout (Runtime)

```text
# Three-tier template storage
{module}/src/commonMain/kotlin/io/codenode/{modname}/graphnodes/
    └── {TemplateName}.flow.kts          # Module-level templates

{projectRoot}/graphnodes/
    └── {TemplateName}.flow.kts          # Project-level templates

~/.codenode/graphnodes/
    └── {TemplateName}.flow.kts          # Universal-level templates
```

## Key Technical Decisions

### 1. Serialization: `.flow.kts` with Metadata Header

Saved GraphNode templates use the existing `.flow.kts` DSL format (FlowGraphSerializer) wrapped in a metadata comment header for lightweight discovery. This avoids inventing a new format and ensures round-trip fidelity.

**Template file structure:**
```
/*
 * GraphNode Template: ProcessingPipeline
 * @GraphNodeTemplate
 * @TemplateName ProcessingPipeline
 * @Description Multi-stage data processing pipeline
 * @InputPorts 2
 * @OutputPorts 1
 * @ChildNodes 3
 * Created: 2026-04-01T10:30:00
 */

// ... standard .flow.kts DSL content for the GraphNode ...
```

### 2. Discovery: Filesystem Scanning with Metadata Parsing

`GraphNodeTemplateRegistry` scans all three tier directories on startup, parsing only the metadata comment header (first ~10 lines) from each `.flow.kts` file. Full deserialization is deferred to instantiation time.

### 3. Promotion: Copy with Package Update

When saving at a more general level, child CodeNode `.kt` files are copied (not moved) to the target tier's `nodes/` directory. Package declarations are updated. Transitive IP type dependencies are also promoted. The original files remain intact.

### 4. Instantiation: Deep Copy with ID Remapping

Dragging a saved GraphNode from the palette triggers full `.flow.kts` deserialization followed by ID remapping for all nodes, connections, ports, and port mappings. This ensures each instance is fully independent.

### 5. Palette Integration: Separate Section, Shared Search

GraphNodes appear in their own collapsible "GraphNodes" section in the NodePalette, distinct from CodeNodeType categories. The palette search box filters both CodeNodes and GraphNodes simultaneously.
