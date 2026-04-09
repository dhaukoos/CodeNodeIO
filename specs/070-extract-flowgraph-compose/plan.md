# Implementation Plan: Extract flowGraph-compose Module

**Branch**: `070-extract-flowgraph-compose` | **Date**: 2026-04-09 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/070-extract-flowgraph-compose/spec.md`

## Summary

Extract 4 non-UI graph composition files (2 viewmodel, 2 state) from graphEditor into a new `flowGraph-compose` KMP module using the Strangler Fig pattern. The module boundary is expressed as a single FlowGraphComposeCodeNode (In3AnyOut1, anyInput) taking flowGraphModel + nodeDescriptors + ipTypeMetadata → graphState. This is the final workflow module extraction (Step 6), completing the Phase B vertical-slice decomposition. Architecture.flow.kt is updated with the child codeNode and port mappings, making all six workflow graphNodes fully populated.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: fbpDsl (core FBP domain model, CodeNodeFactory, In3AnyOut1Runtime), Compose Desktop (geometry types for jvmMain), lifecycle-viewmodel-compose 2.8.0, kotlinx-coroutines 1.8.0
**Storage**: N/A — in-memory graph state
**Testing**: kotlin.test (commonTest), JUnit5 (jvmTest), characterization tests (jvmTest)
**Target Platform**: KMP Desktop (JVM), KMP iOS, KMP Android
**Project Type**: KMP multi-module (Gradle composite)
**Performance Goals**: N/A (graph composition is interactive but not latency-critical for this extraction)
**Constraints**: Must not break any existing tests; Strangler Fig pattern ensures incremental migration; flowGraph-compose MUST NOT depend on graphEditor (no circular dependencies)
**Scale/Scope**: 4 files from graphEditor moving to new module (~1,200 lines)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Module extraction improves single-responsibility; CodeNode boundary is a clean public interface |
| II. Test-Driven Development | PASS | TDD for CodeNode; characterization tests must pass; all existing tests must pass |
| III. User Experience Consistency | N/A | No user-facing UI changes (Compose UI files stay in graphEditor) |
| IV. Performance Requirements | N/A | Graph composition performance unchanged by module boundary |
| V. Observability & Debugging | PASS | CodeNode ports provide observable data flow boundaries |
| Licensing & IP | PASS | All dependencies are Apache 2.0 / MIT (Compose Desktop, lifecycle-viewmodel are Apache 2.0) |

**Gate Result**: PASS — no violations.

**Post-Design Re-Check**: Same assessment. The jvmMain source set placement and Compose Desktop dependency for geometry types are consistent with the flowGraph-inspect module pattern. No new licensing concerns.

## Project Structure

### Documentation (this feature)

```text
specs/070-extract-flowgraph-compose/
├── spec.md
├── plan.md              # This file
├── research.md          # Phase 0 output
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
flowGraph-compose/
├── build.gradle.kts
└── src/
    ├── jvmMain/kotlin/io/codenode/flowgraphcompose/
    │   ├── viewmodel/
    │   │   ├── CanvasInteractionViewModel.kt
    │   │   └── PropertiesPanelViewModel.kt
    │   ├── state/
    │   │   ├── NodeGeneratorState.kt
    │   │   └── ViewSynchronizer.kt
    │   └── nodes/
    │       └── FlowGraphComposeCodeNode.kt
    ├── jvmMain/resources/
    │   └── META-INF/codenode/
    │       └── node-definitions
    └── jvmTest/kotlin/io/codenode/flowgraphcompose/
        └── nodes/
            └── FlowGraphComposeCodeNodeTest.kt
```

**Structure Decision**: Follows the established KMP module pattern from features 065-069. All 4 extracted files go to jvmMain because they depend on Compose Desktop types (Offset, Rect, @Composable annotations) or lifecycle-viewmodel-compose. The CodeNode goes in jvmMain/nodes/ following the directory naming convention established in feature 069. Source files preserve their original subdirectory structure (viewmodel/, state/) under the new `io.codenode.flowgraphcompose` package.

## Research Decisions

### R1: File Identification

**Decision**: 4 files move from graphEditor to flowGraph-compose:
1. viewmodel/CanvasInteractionViewModel.kt — canvas interaction state
2. viewmodel/PropertiesPanelViewModel.kt — properties panel state
3. state/NodeGeneratorState.kt — node generator form state
4. state/ViewSynchronizer.kt — visual/textual sync

**Rationale**: These represent graph composition logic without UI rendering. GraphEditorViewModel.kt and SharedStateProvider.kt stay as orchestration/DI call sites. GraphState.kt, PropertyChangeTracker.kt, and UndoRedoManager.kt stay due to Compose dependencies or core data model status.

### R2: Runtime Type

**Decision**: Use In3AnyOut1Runtime for FlowGraphCompose CodeNode (single node, no sub-graph).

**Rationale**: 3 inputs + 1 output fits In3AnyOut1 exactly. No decomposition needed (unlike flowGraph-generate which had 4 inputs).

### R3: Package Naming

**Decision**: `io.codenode.flowgraphcompose` base package.

**Rationale**: Consistent with flowgraphtypes, flowgraphpersist, flowgraphinspect, flowgraphexecute, flowgraphgenerate.

### R4: Source Set Placement

**Decision**: All files in jvmMain (not commonMain).

**Rationale**: CanvasInteractionViewModel uses Compose Offset/Rect, ViewSynchronizer uses @Composable. PropertiesPanelViewModel and NodeGeneratorState kept in jvmMain to avoid split-package complexity.

### R5: Circular Dependency Avoidance

**Decision**: flowGraph-compose depends on :fbpDsl. graphEditor depends on :flowGraph-compose. If moved files reference GraphState.kt, they must be refactored to depend on fbpDsl model types (FlowGraph, Node, Connection) instead.

**Rationale**: GraphState is likely a Compose-specific wrapper. ViewModels can operate on underlying fbpDsl types, eliminating the back-dependency on graphEditor.

### R6: Build Configuration

**Decision**: KMP module with jvmMain deps: :fbpDsl, lifecycle-viewmodel-compose 2.8.0, compose-desktop (for geometry types), kotlinx-coroutines.

**Rationale**: Matches flowGraph-inspect pattern which also uses lifecycle-viewmodel-compose in jvmMain.

### R7: Consumer Migration

**Decision**: graphEditor is the sole consumer. Update imports in Main.kt, SharedStateProvider.kt, UI composables, and any other files referencing the 4 moved classes.

### R8: Node Discovery and Edit Button

**Decision**: Include META-INF/codenode/node-definitions, sourceFilePath via resolveSourceFilePath, and nodes/ directory naming.

**Rationale**: Required for NodeDefinitionRegistry discovery and Properties panel edit button, per feature 069 conventions.

## Complexity Tracking

No constitution violations to justify.
