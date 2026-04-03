# GraphEditor Architecture Audit

**Feature**: 064-vertical-slice-refactor
**Date**: 2026-04-02
**Total Source Files**: 77 (graphEditor/src/jvmMain/kotlin/)

## Responsibility Buckets

Six buckets classify every graphEditor source file by its primary user-facing workflow:

| Bucket | Target Module | Scope |
|--------|--------------|-------|
| **compose** | flowGraph-compose | Building a flow graph interactively: adding nodes from the palette, connecting ports, validating connections (port type checking, cycle detection), configuring node properties. Owns graph mutation logic — the path from user gesture to valid FlowGraph. |
| **persist** | flowGraph-persist | Saving and loading flow graphs: serialization to `.flow.kts` DSL, deserialization back to in-memory FlowGraph, filesystem I/O, reconciling deserialized state with editor state. Owns the round-trip workflow between memory and disk. |
| **execute** | flowGraph-execute | Running a flow graph and observing results: dynamic runtime pipeline, coroutine channel orchestration, execution control (start/stop/pause/resume/step), data flow animation state, runtime preview. Owns everything from "press Play" to "see results." |
| **generate** | flowGraph-generate | Producing deployable code from a graph: module save workflow, CodeNode definition codegen, runtime file generation, build configuration output. Owns the path from FlowGraph to generated source files on disk. |
| **inspect** | flowGraph-inspect | Understanding available components: node palette, IP type registry, filesystem scanner for node discovery, CodeNode source text editor, IP type file generation, debuggable data preview. Owns discovery and examination of what's available to compose with. |
| **root** | graphEditor (stays) | Composition root: Compose UI composables that render the editor, ViewModels that wire slices together, DI/wiring, application entry point. No business logic — only presentation and orchestration. |

## Assignment Methodology

Each file is assigned to exactly one bucket using a multi-signal approach, evaluated in priority order:

1. **Primary type operated on**: What domain object does this file's core logic manipulate? FlowGraph structure → compose. File I/O → persist. Coroutine channels/execution state → execute. Generated source code → generate. Node definitions/registry → inspect.

2. **Import analysis**: The set of imports reveals domain affinity:
   - `io.codenode.fbpdsl.serialization.*` → persist
   - `io.codenode.fbpdsl.runtime.*` → execute
   - `io.codenode.kotlincompiler.*` → generate
   - `io.codenode.grapheditor.state.NodeDefinitionRegistry`, `IPTypeRegistry` → inspect
   - `io.codenode.fbpdsl.model.FlowGraph`, `Node`, `Connection` (mutations) → compose

3. **@Composable annotation**: Files with `@Composable` functions are UI — they default to **root** unless they exclusively render a specific slice's data with no other orchestration, in which case they may move with their slice's ViewModel.

4. **Cross-reference density**: If a file has cross-references to multiple buckets, it belongs to the bucket with the highest affinity (most references). Ties are broken by which workflow the file is *called from* most often.

## File Audit

### ui/ (25 files)

| File | Bucket | Primary Responsibility | Cross-Bucket Dependencies |
|------|--------|----------------------|--------------------------|
| ui/CanvasControls.kt | root | Canvas UI controls (zoom/pan buttons) | compose (GraphState) |
| ui/CodeEditor.kt | inspect | Editable code editor with syntax highlighting for CodeNode .kt sources | root (CodeEditorViewModel) |
| ui/CollapsiblePanel.kt | root | Reusable collapsible wrapper for side panels | None |
| ui/ColorEditor.kt | inspect | RGB color value editor for IP types | None (pure UI) |
| ui/ComposableDiscovery.kt | inspect | Filesystem scanner for userInterface/ composables | None (File I/O only) |
| ui/ConnectionContextMenu.kt | compose | Context menu for connection IP type assignment | inspect (InformationPacketType lookup) |
| ui/ConnectionHandler.kt | compose | Connection creation state and workflow logic | compose (GraphState for port validation) |
| ui/DragAndDropHandler.kt | compose | Drag-and-drop state for palette-to-canvas node placement | inspect (NodeTypeDefinition), compose (GraphState) |
| ui/DynamicPreviewDiscovery.kt | inspect | Runtime reflection-based PreviewProvider discovery | None (Reflection API only) |
| ui/ErrorDisplay.kt | root | Error message display UI with severity levels | compose (GraphState for error state) |
| ui/FileSelector.kt | root | Dropdown selector for flowGraph/CodeNode files | None |
| ui/FlowGraphCanvas.kt | root | Main visual canvas rendering and interaction orchestration | compose (GraphState, SelectableElement), execute (ConnectionAnimation) |
| ui/FlowGraphPropertiesDialog.kt | root | Dialog for editing FlowGraph name/platforms | None (FlowGraph.TargetPlatform only) |
| ui/GraphNodePaletteSection.kt | root | Collapsible palette section for GraphNode templates with distinct card design | persist (GraphNodeTemplateMeta), inspect (PlacementLevel) |
| ui/IPGeneratorPanel.kt | generate | UI panel for creating custom IP types | inspect (IPGeneratorViewModel, PlacementLevel, InformationPacketType) |
| ui/IPPalette.kt | inspect | Searchable list of IP types with color indicators | inspect (IPPaletteViewModel) |
| ui/ModuleSessionFactory.kt | execute | Factory for RuntimeSession instances via reflection | inspect (NodeDefinitionRegistry) |
| ui/NodeGeneratorPanel.kt | generate | UI panel for creating custom node types | inspect (NodeGeneratorViewModel, PlacementLevel) |
| ui/NodePalette.kt | inspect | Categorized node type selection with drag-drop support | inspect (NodePaletteViewModel, GraphNodePaletteViewModel) |
| ui/PropertiesPanel.kt | root | Node configuration and property editor panel (orchestrates multiple concerns) | compose (GraphState, PropertiesPanelViewModel), inspect (IPTypeRegistry), persist (PromotionCandidate) |
| ui/PropertyEditors.kt | root | Specialized input components (text, number, boolean, dropdown) | None (pure UI helpers) |
| ui/RuntimePreviewPanel.kt | execute | Collapsible panel for runtime execution and preview rendering | inspect (module discovery) |
| ui/SyntaxHighlighter.kt | inspect | DSL syntax highlighting for text views | None |
| ui/TextualView.kt | persist | Displays flow graphs as readable DSL text via TextGenerator | persist (TextGenerator), inspect (SyntaxHighlighter) |
| ui/ViewToggle.kt | root | View mode switcher (visual/textual/split) | None |

### viewmodel/ (11 files)

| File | Bucket | Primary Responsibility | Cross-Bucket Dependencies |
|------|--------|----------------------|--------------------------|
| viewmodel/BaseState.kt | root | Marker interface for all ViewModel state data classes | None |
| viewmodel/CanvasInteractionViewModel.kt | compose | Node drag, connection creation, rectangular selection, port hover, interaction mode | None (fbpdsl.model types only) |
| viewmodel/CodeEditorViewModel.kt | inspect | File I/O for .kt source files, dirty tracking, editor state management | persist (file read/write), inspect (NodeDefinitionRegistry) |
| viewmodel/GraphEditorViewModel.kt | root | Orchestration ViewModel: file ops, undo/redo, group/ungroup, navigation, dialogs, status | compose (GraphState), persist (FlowKtParser) |
| viewmodel/GraphNodePaletteViewModel.kt | inspect | GraphNode template browsing, expand/collapse state, search filtering | persist (GraphNodeTemplateMeta) |
| viewmodel/IPGeneratorViewModel.kt | generate | Custom IP type creation: form state, property rows, file generation | inspect (IPTypeRegistry, IPTypeDiscovery), persist (IPTypeFileGenerator) |
| viewmodel/IPPaletteViewModel.kt | inspect | IP type browsing/selection, search filtering, type deletion | persist (FileIPTypeRepository), inspect (IPTypeRegistry) |
| viewmodel/NodeGeneratorViewModel.kt | generate | Custom CodeNode file generation: form state, code generation, filesystem paths | inspect (NodeDefinitionRegistry, PlacementLevel) |
| viewmodel/NodePaletteViewModel.kt | inspect | Node type browsing, search filtering, category expansion state | None (fbpdsl.model types only) |
| viewmodel/PropertiesPanelViewModel.kt | compose | Node/connection property editing: selection, dirty tracking, validation | None (fbpdsl.model types only) |
| viewmodel/SharedStateProvider.kt | root | DI container for shared state; provides CompositionLocal access to GraphState, UndoRedoManager, PropertyChangeTracker, IPTypeRegistry | compose (GraphState, UndoRedoManager, PropertyChangeTracker), inspect (IPTypeRegistry) |

### state/ (8 files)

| File | Bucket | Primary Responsibility | Cross-Bucket Dependencies |
|------|--------|----------------------|--------------------------|
| state/GraphState.kt | compose | Central graph mutation hub: add/remove nodes, connect/disconnect ports, selection, viewport, grouping, navigation, validation | inspect (IPTypeRegistry for port type updates) |
| state/IPTypeDiscovery.kt | inspect | Filesystem scanner for IP type definitions from three-tier structure; parses @IPType metadata | None (filesystem I/O only) |
| state/IPTypeFileGenerator.kt | generate | Code generation for new IP type .kt files; writes to appropriate filesystem tier | inspect (PlacementLevel) |
| state/IPTypeRegistry.kt | inspect | Runtime registry for InformationPacket types; lookup by ID/name, filtering, search | None (pure in-memory registry) |
| state/NodeGeneratorState.kt | root | UI form state holder for Node Generator panel; immutable data class | None |
| state/PropertyChangeTracker.kt | compose | Undo/redo integration for property changes via PropertyChangeCommand | compose (UndoRedoManager, GraphState) |
| state/UndoRedoManager.kt | compose | Command pattern for undo/redo; maintains dual stacks with history limits | compose (GraphState) |
| state/ViewSynchronizer.kt | persist | Bidirectional sync between visual and textual graph views; TextGenerator regeneration | compose (GraphState.flowGraph), persist (TextGenerator) |

### model/ (6 files)

| File | Bucket | Primary Responsibility | Cross-Bucket Dependencies |
|------|--------|----------------------|--------------------------|
| model/GraphNodeTemplateMeta.kt | persist | Lightweight metadata for saved GraphNode templates (name, ports, tier) | inspect (PlacementLevel) |
| model/IPProperty.kt | inspect | Domain model for custom IP type properties | None |
| model/IPPropertyMeta.kt | inspect | Parsed metadata for a single property from IP type .kt file during discovery | None |
| model/IPTypeFileMeta.kt | inspect | Metadata extracted from .kt IP type files during filesystem discovery | inspect (PlacementLevel) |
| model/PlacementLevel.kt | inspect | Shared enum for filesystem tier placement (Module/Project/Universal) | None |
| model/SerializableIPType.kt | persist | Serializable DTOs for custom IP type JSON persistence (legacy) | inspect (IPProperty) |

### serialization/ (3 files)

| File | Bucket | Primary Responsibility | Cross-Bucket Dependencies |
|------|--------|----------------------|--------------------------|
| serialization/FlowGraphSerializer.kt | persist | Converts FlowGraph to .flow.kts DSL format for file persistence | None (fbpdsl.model types only) |
| serialization/FlowKtParser.kt | persist | Parses compiled .flow.kt files back to FlowGraph model | None (fbpdsl.model types only) |
| serialization/GraphNodeTemplateSerializer.kt | persist | Wraps FlowGraphSerializer/FlowKtParser with metadata headers for .flow.kts templates | persist (FlowGraphSerializer, FlowKtParser), inspect (IPTypeRegistry) |

### compilation/ (3 files)

| File | Bucket | Primary Responsibility | Cross-Bucket Dependencies |
|------|--------|----------------------|--------------------------|
| compilation/CompilationService.kt | generate | Integration between graphEditor and module generation; validates and compiles FlowGraphs | generate (RequiredPropertyValidator) |
| compilation/CompilationValidator.kt | generate | Validates module structure before compilation (dirs, sources, build config) | None |
| compilation/RequiredPropertyValidator.kt | generate | Validates CodeNodes have required properties before code generation | None |

### rendering/ (3 files)

| File | Bucket | Primary Responsibility | Cross-Bucket Dependencies |
|------|--------|----------------------|--------------------------|
| rendering/ConnectionRenderer.kt | root | Renders connections as bezier curves on canvas | None (Compose graphics + fbpdsl types) |
| rendering/NodeRenderer.kt | root | Renders nodes on canvas with styling by type and state | None (Compose graphics + fbpdsl types) |
| rendering/PortRenderer.kt | root | Renders ports (circles/squares) with hover/selection states | None (Compose graphics + fbpdsl types) |

### repository/ (2 files)

| File | Bucket | Primary Responsibility | Cross-Bucket Dependencies |
|------|--------|----------------------|--------------------------|
| repository/FileIPTypeRepository.kt | persist | File-based JSON persistence for custom IP types (legacy, retained for entity module tracking) | inspect (SerializableIPType) |
| repository/IPTypeMigration.kt | inspect | One-time migration from legacy JSON repository to filesystem-based .kt files | persist (FileIPTypeRepository), inspect (IPTypeDiscovery), generate (IPTypeFileGenerator) |

### save/ (1 file)

| File | Bucket | Primary Responsibility | Cross-Bucket Dependencies |
|------|--------|----------------------|--------------------------|
| save/ModuleSaveService.kt | generate | Unified service for saving FlowGraphs as complete KMP module structures | persist (FlowKtGenerator), generate (10+ kotlincompiler generators) |

### io/codenode/grapheditor/state/ (9 files)

| File | Bucket | Primary Responsibility | Cross-Bucket Dependencies |
|------|--------|----------------------|--------------------------|
| io/.../state/GraphNodeTemplateInstantiator.kt | persist | Deep-copy instantiation of saved GraphNode templates with ID remapping | persist (GraphNodeTemplateSerializer, GraphNodeTemplateRegistry) |
| io/.../state/GraphNodeTemplateRegistry.kt | persist | Discovery, caching, save/remove of .flow.kts GraphNode templates across three tiers | persist (GraphNodeTemplateSerializer) |
| io/.../state/GroupContextMenuState.kt | root | UI state data class for group/ungroup context menu | None |
| io/.../state/LevelCompatibilityChecker.kt | compose | Validates child node compatibility when promoting GraphNodes to higher tiers | inspect (NodeDefinitionRegistry), compose (NodePromoter) |
| io/.../state/NavigationContext.kt | root | Hierarchical navigation state for GraphNode zoom in/out (path stack) | None |
| io/.../state/NodeDefinitionRegistry.kt | inspect | Central discovery registry for all node definitions (ServiceLoader + filesystem) | None |
| io/.../state/NodePromoter.kt | compose | Copies CodeNode .kt files to target tier with updated package declarations; import analysis | inspect (PlacementLevel) |
| io/.../state/SelectableElement.kt | root | Sealed interface unifying selection model for nodes and connections | None |
| io/.../state/SelectionState.kt | root | Multi-selection state management with rectangular selection tracking | None |

### io/codenode/grapheditor/ui/ (5 files)

| File | Bucket | Primary Responsibility | Cross-Bucket Dependencies |
|------|--------|----------------------|--------------------------|
| io/.../ui/GraphNodeRenderer.kt | root | Canvas renderer for GraphNode visual (double-border, gradient, header, badges) | root (PortRenderer) |
| io/.../ui/GroupContextMenu.kt | root | Popup context menu composable for grouping/ungrouping | root (GroupContextMenuState) |
| io/.../ui/NavigationBreadcrumb.kt | root | Breadcrumb trail composable for GraphNode navigation path | root (NavigationContext) |
| io/.../ui/NavigationZoomOutButton.kt | root | Floating zoom-out button for parent GraphNode navigation | None |
| io/.../ui/SelectionBox.kt | root | Canvas composable rendering dotted rectangle during rectangular selection | None |

### Root (1 file)

| File | Bucket | Primary Responsibility | Cross-Bucket Dependencies |
|------|--------|----------------------|--------------------------|
| Main.kt | root | App entry point: Compose UI, state initialization, DI wiring, all panels | ALL buckets (orchestrates compose, persist, execute, generate, inspect) |

## Seam Matrix

Cross-bucket dependencies where one file directly references types or functions from a file in a different bucket. Seams at module boundaries that will need interfaces during extraction.

| Source File | Target File | Type | Source Bucket | Target Bucket | Boundary |
|------------|------------|------|---------------|---------------|----------|
| state/GraphState.kt | state/IPTypeRegistry.kt | Function call | compose | inspect | compose→inspect |
| state/PropertyChangeTracker.kt | state/UndoRedoManager.kt | Function call | compose | compose | (internal) |
| state/PropertyChangeTracker.kt | state/GraphState.kt | Function call | compose | compose | (internal) |
| state/ViewSynchronizer.kt | state/GraphState.kt | State sharing | persist | compose | persist→compose |
| ui/ConnectionContextMenu.kt | state/IPTypeRegistry.kt | Type reference | compose | inspect | compose→inspect |
| ui/ConnectionHandler.kt | state/GraphState.kt | Function call | compose | compose | (internal) |
| ui/DragAndDropHandler.kt | state/GraphState.kt | Function call | compose | compose | (internal) |
| ui/DragAndDropHandler.kt | io/.../state/NodeDefinitionRegistry.kt | Type reference | compose | inspect | compose→inspect |
| ui/FlowGraphCanvas.kt | state/GraphState.kt | State sharing | root | compose | root→compose |
| ui/FlowGraphCanvas.kt | io/.../state/SelectableElement.kt | Type reference | root | root | (internal) |
| ui/PropertiesPanel.kt | state/GraphState.kt | State sharing | root | compose | root→compose |
| ui/PropertiesPanel.kt | state/IPTypeRegistry.kt | Function call | root | inspect | root→inspect |
| ui/PropertiesPanel.kt | viewmodel/PropertiesPanelViewModel.kt | Function call | root | compose | root→compose |
| ui/PropertiesPanel.kt | io/.../state/LevelCompatibilityChecker.kt | Function call | root | compose | root→compose |
| ui/ErrorDisplay.kt | state/GraphState.kt | State sharing | root | compose | root→compose |
| ui/CanvasControls.kt | state/GraphState.kt | Function call | root | compose | root→compose |
| ui/GraphNodePaletteSection.kt | model/GraphNodeTemplateMeta.kt | Type reference | root | persist | root→persist |
| ui/GraphNodePaletteSection.kt | model/PlacementLevel.kt | Type reference | root | inspect | root→inspect |
| ui/IPGeneratorPanel.kt | viewmodel/IPGeneratorViewModel.kt | Function call | generate | generate | (internal) |
| ui/NodeGeneratorPanel.kt | viewmodel/NodeGeneratorViewModel.kt | Function call | generate | generate | (internal) |
| ui/IPPalette.kt | viewmodel/IPPaletteViewModel.kt | Function call | inspect | inspect | (internal) |
| ui/NodePalette.kt | viewmodel/NodePaletteViewModel.kt | Function call | inspect | inspect | (internal) |
| ui/NodePalette.kt | viewmodel/GraphNodePaletteViewModel.kt | Function call | inspect | inspect | (internal) |
| ui/CodeEditor.kt | viewmodel/CodeEditorViewModel.kt | Function call | inspect | inspect | (internal) |
| ui/TextualView.kt | ui/SyntaxHighlighter.kt | Function call | persist | inspect | persist→inspect |
| ui/RuntimePreviewPanel.kt | ui/ModuleSessionFactory.kt | Function call | execute | execute | (internal) |
| ui/ModuleSessionFactory.kt | io/.../state/NodeDefinitionRegistry.kt | Type reference | execute | inspect | execute→inspect |
| viewmodel/GraphEditorViewModel.kt | state/GraphState.kt | State sharing | root | compose | root→compose |
| viewmodel/GraphEditorViewModel.kt | serialization/FlowKtParser.kt | Function call | root | persist | root→persist |
| viewmodel/SharedStateProvider.kt | state/GraphState.kt | State sharing | root | compose | root→compose |
| viewmodel/SharedStateProvider.kt | state/UndoRedoManager.kt | State sharing | root | compose | root→compose |
| viewmodel/SharedStateProvider.kt | state/PropertyChangeTracker.kt | State sharing | root | compose | root→compose |
| viewmodel/SharedStateProvider.kt | state/IPTypeRegistry.kt | State sharing | root | inspect | root→inspect |
| viewmodel/IPGeneratorViewModel.kt | state/IPTypeRegistry.kt | Function call | generate | inspect | generate→inspect |
| viewmodel/IPGeneratorViewModel.kt | state/IPTypeDiscovery.kt | Function call | generate | inspect | generate→inspect |
| viewmodel/IPGeneratorViewModel.kt | state/IPTypeFileGenerator.kt | Function call | generate | generate | (internal) |
| viewmodel/IPPaletteViewModel.kt | repository/FileIPTypeRepository.kt | Function call | inspect | persist | inspect→persist |
| viewmodel/IPPaletteViewModel.kt | state/IPTypeRegistry.kt | Function call | inspect | inspect | (internal) |
| viewmodel/CodeEditorViewModel.kt | io/.../state/NodeDefinitionRegistry.kt | Function call | inspect | inspect | (internal) |
| viewmodel/NodeGeneratorViewModel.kt | io/.../state/NodeDefinitionRegistry.kt | Function call | generate | inspect | generate→inspect |
| serialization/GraphNodeTemplateSerializer.kt | serialization/FlowGraphSerializer.kt | Function call | persist | persist | (internal) |
| serialization/GraphNodeTemplateSerializer.kt | serialization/FlowKtParser.kt | Function call | persist | persist | (internal) |
| serialization/GraphNodeTemplateSerializer.kt | state/IPTypeRegistry.kt | Function call | persist | inspect | persist→inspect |
| io/.../state/GraphNodeTemplateRegistry.kt | serialization/GraphNodeTemplateSerializer.kt | Function call | persist | persist | (internal) |
| io/.../state/GraphNodeTemplateInstantiator.kt | serialization/GraphNodeTemplateSerializer.kt | Function call | persist | persist | (internal) |
| io/.../state/GraphNodeTemplateInstantiator.kt | io/.../state/GraphNodeTemplateRegistry.kt | Function call | persist | persist | (internal) |
| io/.../state/LevelCompatibilityChecker.kt | io/.../state/NodeDefinitionRegistry.kt | Function call | compose | inspect | compose→inspect |
| io/.../state/LevelCompatibilityChecker.kt | io/.../state/NodePromoter.kt | Function call | compose | compose | (internal) |
| repository/IPTypeMigration.kt | repository/FileIPTypeRepository.kt | Function call | inspect | persist | inspect→persist |
| repository/IPTypeMigration.kt | state/IPTypeDiscovery.kt | Function call | inspect | inspect | (internal) |
| repository/IPTypeMigration.kt | state/IPTypeFileGenerator.kt | Function call | inspect | generate | inspect→generate |
| save/ModuleSaveService.kt | serialization/FlowGraphSerializer.kt | Function call | generate | persist | generate→persist |
| compilation/CompilationService.kt | compilation/RequiredPropertyValidator.kt | Function call | generate | generate | (internal) |
| Main.kt | (all buckets) | Function call / State sharing | root | ALL | root→all |

### Seam Count by Boundary

| Boundary | Count | Notes |
|----------|-------|-------|
| root→compose | 8 | GraphState is the most referenced cross-boundary target |
| root→inspect | 3 | IPTypeRegistry, PlacementLevel |
| root→persist | 2 | FlowKtParser, GraphNodeTemplateMeta |
| compose→inspect | 4 | IPTypeRegistry, NodeDefinitionRegistry |
| persist→compose | 1 | ViewSynchronizer→GraphState |
| persist→inspect | 2 | GraphNodeTemplateSerializer→IPTypeRegistry, TextualView→SyntaxHighlighter |
| execute→inspect | 1 | ModuleSessionFactory→NodeDefinitionRegistry |
| generate→inspect | 3 | IPGeneratorVM, NodeGeneratorVM→NodeDefinitionRegistry |
| generate→persist | 1 | ModuleSaveService→FlowGraphSerializer |
| inspect→persist | 2 | IPPaletteVM→FileIPTypeRepository, IPTypeMigration→FileIPTypeRepository |
| inspect→generate | 1 | IPTypeMigration→IPTypeFileGenerator |
| **Total cross-boundary** | **28** | Excluding root→all (Main.kt) and internal seams |

## Summary

### Files Per Bucket

| Bucket | Count | Percentage |
|--------|-------|-----------|
| **root** | 28 | 36% |
| **inspect** | 17 | 22% |
| **persist** | 13 | 17% |
| **compose** | 9 | 12% |
| **generate** | 7 | 9% |
| **execute** | 3 | 4% |
| **Total** | **77** | **100%** |

### Key Observations

1. **root is the largest bucket (28 files)** — This is expected: graphEditor is primarily a Compose Desktop application. After extraction, these files remain as the composition root with UI composables and orchestration ViewModels.

2. **inspect is the second largest (17 files)** — Discovery, registry, palette, and text editing form a significant independent workflow. This is a strong extraction candidate.

3. **compose has only 9 files but is the most referenced** — GraphState alone accounts for 8 inbound seams from root. This will require the most careful interface design during extraction.

4. **execute is surprisingly small (3 files)** — Most execution logic lives in fbpDsl (runtime classes, channels). The graphEditor execute bucket is just the bridge between UI and fbpDsl runtime.

5. **Main.kt is the universal connector** — It references every bucket, confirming its role as the composition root. It will be the last file refactored (if at all).

6. **Highest-traffic seam: root→compose (8 seams)** — Extracting compose last (per the plan) is correct, as it has the most consumers in root.

7. **inspect is a dependency hub** — Both compose and generate depend on inspect (IPTypeRegistry, NodeDefinitionRegistry, PlacementLevel). Extract inspect second to make it available to later extractions.
