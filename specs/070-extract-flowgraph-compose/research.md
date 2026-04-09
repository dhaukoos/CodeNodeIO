# Research: Extract flowGraph-compose Module

**Feature**: 070-extract-flowgraph-compose
**Date**: 2026-04-09

## R1: File Identification — Which 4 Files Move

**Decision**: The 4 files that move are 2 from viewmodel/ and 2 from state/:
- viewmodel/CanvasInteractionViewModel.kt — canvas interaction state (drag, connection, selection, hover)
- viewmodel/PropertiesPanelViewModel.kt — properties panel state (node editing, validation)
- state/NodeGeneratorState.kt — form state data class for node generation
- state/ViewSynchronizer.kt — bidirectional sync between visual and textual graph representations

**Rationale**: These 4 files represent graph composition logic (mutations, interactions, state management) that doesn't require Compose UI rendering. The remaining viewmodel/ files stay because they are orchestration (GraphEditorViewModel.kt is a call site that consumes compose services), DI infrastructure (SharedStateProvider.kt provides services), or shared foundations (BaseState.kt is a marker interface used by all ViewModels).

Note: CanvasInteractionViewModel.kt imports `androidx.compose.ui.geometry.Offset/Rect` and ViewSynchronizer.kt imports `androidx.compose.runtime.*`. These are Compose foundation types, not UI rendering — the files must go in jvmMain (not commonMain) due to these Compose Desktop dependencies.

**Files that stay in graphEditor (6)**:
- state/GraphState.kt — core domain model referenced by all UI
- state/PropertyChangeTracker.kt — undo tracking with @Composable helpers
- state/UndoRedoManager.kt — command pattern with @Composable helpers
- ui/ConnectionContextMenu.kt — Compose UI component
- ui/ConnectionHandler.kt — Compose event handler
- ui/DragAndDropHandler.kt — Compose event handler

**Alternatives Considered**: Moving all viewmodel/ files was rejected because GraphEditorViewModel.kt is the orchestration layer (composition root), SharedStateProvider.kt is DI wiring, and BaseState.kt is a shared marker interface.

## R2: Runtime Type for FlowGraphCompose CodeNode

**Decision**: Use In3AnyOut1Runtime for FlowGraphCompose.

**Rationale**: The compose graphNode in architecture.flow.kt has exactly 3 inputs (flowGraphModel, nodeDescriptors, ipTypeMetadata) and 1 output (graphState). In3AnyOut1Runtime exists in fbpDsl with factory method `CodeNodeFactory.createIn3AnyOut1Processor()`. anyInput mode ensures each input independently triggers processing with cached values.

**Alternatives Considered**: No sub-graph decomposition needed (unlike flowGraph-generate which had 4 inputs requiring a 2+3 split). A single CodeNode is sufficient.

## R3: Package Naming

**Decision**: Use `io.codenode.flowgraphcompose` as the base package.

**Rationale**: Follows the established pattern: flowGraph-types → `flowgraphtypes`, flowGraph-persist → `flowgraphpersist`, flowGraph-inspect → `flowgraphinspect`, flowGraph-execute → `flowgraphexecute`, flowGraph-generate → `flowgraphgenerate`.

## R4: Source Set Placement

**Decision**: All 4 extracted files go to jvmMain. CodeNode goes to jvmMain/nodes/.

**Rationale**: CanvasInteractionViewModel.kt uses `androidx.compose.ui.geometry.Offset/Rect` and ViewSynchronizer.kt uses `androidx.compose.runtime.*` — both are JVM Desktop Compose dependencies. PropertiesPanelViewModel.kt and NodeGeneratorState.kt could theoretically go in commonMain, but keeping them with their co-dependent files in jvmMain avoids split-package complexity. The CodeNode uses `resolveSourceFilePath(this::class.java)` which is JVM-specific.

## R5: Dependency Graph — Avoiding Circular Dependencies

**Decision**: flowGraph-compose depends on :fbpDsl (FBP types). graphEditor depends on :flowGraph-compose (for the moved ViewModels). flowGraph-compose does NOT depend on graphEditor.

**Rationale**: The critical risk is that moved files (CanvasInteractionViewModel, ViewSynchronizer) may reference GraphState.kt which stays in graphEditor. If so, GraphState or a subset of its interface must be abstracted — either:
1. Move the GraphState interface into fbpDsl or flowGraph-compose, with the concrete implementation staying in graphEditor
2. Have the moved files depend on fbpDsl model types (FlowGraph, Node, Connection) instead of GraphState directly

Option 2 is preferred because GraphState is likely a Compose-specific wrapper around fbpDsl model types, and the ViewModels can operate on the underlying model directly.

**Alternatives Considered**: Having flowGraph-compose depend on graphEditor was rejected — it creates a circular dependency (graphEditor already depends on flowGraph-compose for the moved files).

## R6: Build Configuration

**Decision**: flowGraph-compose/build.gradle.kts follows the KMP pattern from features 065-069, with Compose Desktop dependency for jvmMain.

**Dependencies**:
- commonMain: none specific (just kotlin stdlib)
- jvmMain: `project(":fbpDsl")`, lifecycle-viewmodel-compose 2.8.0, compose-desktop (for geometry types)
- jvmTest: JUnit5, kotlin-test

**Rationale**: Matches the pattern from flowGraph-inspect which also has lifecycle-viewmodel-compose in jvmMain. The compose-desktop dependency is needed for `Offset`, `Rect`, and `@Composable` annotations used by the moved files.

Note: If the moved files have minimal Compose dependencies that can be abstracted away, the compose-desktop dependency may be avoidable. This should be evaluated during implementation.

## R7: Consumer Migration

**Decision**: graphEditor is the only consumer of the 4 moved files. Update imports in graphEditor from `io.codenode.grapheditor.viewmodel` / `io.codenode.grapheditor.state` to `io.codenode.flowgraphcompose.viewmodel` / `io.codenode.flowgraphcompose.state`.

**Consumers** (files referencing the 4 moved files):
- Main.kt — creates/references ViewModels
- UI composables — reference CanvasInteractionViewModel for canvas state
- PropertiesPanel.kt — references PropertiesPanelViewModel
- SharedStateProvider.kt — wires ViewModels
- Other UI files — may reference NodeGeneratorState, ViewSynchronizer

## R8: Node Discovery and Edit Button Support

**Decision**: FlowGraphComposeCodeNode must include: (1) META-INF/codenode/node-definitions registry file, (2) sourceFilePath override via resolveSourceFilePath, (3) nodes/ directory naming convention.

**Rationale**: Feature 069 established that phase B CodeNodes require these three elements for discovery by NodeDefinitionRegistry (classpath scanning via custom registry) and for the edit button to appear in the GraphNodePropertiesPanel.
