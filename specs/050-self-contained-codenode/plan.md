# Implementation Plan: Self-Contained CodeNode Definition

**Branch**: `050-self-contained-codenode` | **Date**: 2026-03-13 | **Spec**: `specs/050-self-contained-codenode/spec.md`
**Input**: Feature specification from `/specs/050-self-contained-codenode/spec.md`

## Summary

Replace the fragmented node definition pattern (separate CustomNodeDefinition + ProcessingLogic + generated runtime references) with a single-file self-contained node class implementing a `CodeNodeDefinition` interface. The Node Generator produces an editable Kotlin object file, the graph editor auto-discovers it via a `NodeDefinitionRegistry` (classpath + filesystem + legacy), and the runtime resolves processing logic by node name instead of hardcoded imports. Includes migration of all 6 EdgeArtFilter nodes as end-to-end validation.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0
**Storage**: FileCustomNodeRepository (JSON at `~/.codenode/custom-nodes.json`) for legacy; classpath + filesystem scanning for new nodes
**Testing**: kotlin.test (commonTest), JVM test runner via Gradle
**Target Platform**: JVM Desktop (Compose Desktop)
**Project Type**: KMP multi-module (fbpDsl, graphEditor, nodes, per-module flow projects)
**Performance Goals**: Node discovery on startup < 500ms for up to 100 node definitions
**Constraints**: Must coexist with legacy CustomNodeDefinition pattern during transition
**Scale/Scope**: ~10-50 node definitions per project, 3 discovery levels (Module, Project, Universal)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| **Licensing (Critical)** | PASS | No new dependencies added; all existing deps are Apache 2.0/MIT |
| **I. Code Quality First** | PASS | Single-file pattern improves maintainability; clear single responsibility per node object |
| **II. Test-Driven Development** | PASS | FR-008 explicitly requires standalone unit testability; CodeNodeDefinition interface enables isolated testing |
| **III. User Experience Consistency** | PASS | Extends existing Node Generator UI with category/level selectors; palette display uses existing patterns |
| **IV. Performance Requirements** | PASS | Classpath scanning is bounded; template parsing is lazy; legacy lookup unchanged |
| **V. Observability & Debugging** | PASS | Registry provides `isCompiled()` check; error messages for name conflicts and missing definitions |

**Post-Phase 1 Re-check**: All gates still pass. The new `nodes` module follows existing KMP conventions. No new external dependencies introduced.

## Project Structure

### Documentation (this feature)

```text
specs/050-self-contained-codenode/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0: 6 research decisions
├── data-model.md        # Phase 1: entities and relationships
├── quickstart.md        # Phase 1: 7-step implementation guide
├── contracts/           # Phase 1: interface contracts
│   └── code-node-definition.md
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/
├── CodeNodeDefinition.kt          # New: interface + PortSpec + NodeCategory enum
└── (existing runtime classes)     # Unchanged

nodes/                              # New: project-level nodes module
├── build.gradle.kts               # KMP module with fbpDsl dependency
└── src/commonMain/kotlin/io/codenode/nodes/
    └── (generated node files)     # e.g., SepiaTransformerCodeNode.kt

EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/
├── nodes/                          # New: module-level self-contained nodes
│   ├── ImagePickerCodeNode.kt      # Migrated from processingLogic/ImagePickerSourceLogic.kt
│   ├── GrayscaleTransformerCodeNode.kt  # Migrated from processingLogic/GrayscaleTransformerProcessLogic.kt
│   ├── EdgeDetectorCodeNode.kt     # Migrated from processingLogic/EdgeDetectorProcessLogic.kt
│   ├── ColorOverlayCodeNode.kt     # Migrated from processingLogic/ColorOverlayProcessLogic.kt
│   ├── SepiaTransformerCodeNode.kt  # Migrated from processingLogic/SepiaTransformerProcessLogic.kt
│   └── ImageViewerCodeNode.kt      # Migrated from processingLogic/ImageViewerSinkLogic.kt
└── processingLogic/                # Legacy files removed after migration

graphEditor/src/jvmMain/kotlin/
├── Main.kt                        # Modified: registry replaces hardcoded registration
└── io/codenode/grapheditor/
    ├── state/
    │   └── NodeDefinitionRegistry.kt   # New: discovery + lookup
    ├── viewmodel/
    │   └── NodeGeneratorViewModel.kt   # Modified: category, level, file generation
    └── ui/
        └── NodeGeneratorPanel.kt       # Modified: category + level UI selectors
```

**Structure Decision**: Extends the existing KMP multi-module architecture. The `fbpDsl` module gets the interface (shared across all targets). A new `nodes` module provides the default Project-level location for generated nodes. The `graphEditor` module gets the registry and updated generator.

## Complexity Tracking

No constitution violations. All changes follow existing patterns and add no new external dependencies.
