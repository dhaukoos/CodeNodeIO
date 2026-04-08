# Vertical Slice Migration Map

**Feature**: 064-vertical-slice-refactor
**Date**: 2026-04-02

## Guiding Principle: Compose Stays in graphEditor

**Established during feature 066 (flowGraph-persist extraction)**: Files containing `@Composable` functions or primarily Compose UI rendering remain in graphEditor as presentation concerns. Only non-UI logic, state management, and ViewModels are extracted into vertical-slice modules. This principle was validated when ViewSynchronizer.kt and TextualView.kt were kept in graphEditor during the persist extraction, and again when 5 Compose UI files (CodeEditor, ColorEditor, IPPalette, NodePalette, SyntaxHighlighter) were kept during the inspect spec (feature 067).

**Why**: Compose composables are presentation layer — they render state but don't own business logic. Extracting them would pull Compose Desktop dependencies into modules that should be pure Kotlin/KMP. The extracted modules expose their functionality through CodeNode FBP ports, not through UI composables.

---

## Cross-Module Seam Summary

Three source modules are being decomposed into six vertical-slice target modules plus a composition root. This section documents the cross-module seams — dependencies that cross the boundaries between graphEditor, kotlinCompiler, and circuitSimulator.

### Source Module Dependency Direction

```
graphEditor ──depends on──► kotlinCompiler (code generation)
graphEditor ──depends on──► circuitSimulator (runtime session)
graphEditor ──depends on──► fbpDsl (shared vocabulary)
kotlinCompiler ──depends on──► fbpDsl (shared vocabulary)
circuitSimulator ──depends on──► fbpDsl (shared vocabulary)
```

**Key finding**: Dependencies flow **one-way** from graphEditor to the other two modules. Neither kotlinCompiler nor circuitSimulator imports from graphEditor or from each other. This simplifies extraction — each satellite module can be moved independently.

### graphEditor → kotlinCompiler Seams

| graphEditor File | kotlinCompiler File | Type | graphEditor Bucket | Target Module |
|-----------------|-------------------|------|-------------------|---------------|
| save/ModuleSaveService.kt | generator/KotlinCodeGenerator.kt | Function call | generate | flowGraph-generate |
| save/ModuleSaveService.kt | generator/ModuleGenerator.kt | Function call | generate | flowGraph-generate |
| save/ModuleSaveService.kt | generator/FlowKtGenerator.kt | Function call | generate | flowGraph-generate |
| compilation/CompilationService.kt | validator/LicenseValidator.kt | Function call | generate | flowGraph-generate |

**Resolution**: All graphEditor files that depend on kotlinCompiler are themselves in the `generate` bucket. When both move to flowGraph-generate, these cross-module seams become internal dependencies. **No new interfaces needed.**

### graphEditor → circuitSimulator Seams

| graphEditor File | circuitSimulator File | Type | graphEditor Bucket | Target Module |
|-----------------|---------------------|------|-------------------|---------------|
| ui/RuntimePreviewPanel.kt | RuntimeSession.kt | Function call | execute | flowGraph-execute |
| ui/ModuleSessionFactory.kt | RuntimeSession.kt | Function call | execute | flowGraph-execute |
| ui/FlowGraphCanvas.kt | ConnectionAnimation.kt | Type reference | root | root→execute |

**Resolution**: ModuleSessionFactory moves to flowGraph-execute alongside circuitSimulator files, so its seam with RuntimeSession becomes internal. RuntimePreviewPanel.kt stays in graphEditor (Compose UI) and will reference execute via the module's CodeNode ports. FlowGraphCanvas.kt references ConnectionAnimation for rendering animated dots; this is the one true cross-module interface needed (root→execute boundary).

### Consolidated Seam Counts

| Boundary | Seam Count | Post-Extraction |
|----------|-----------|-----------------|
| graphEditor(generate) → kotlinCompiler | 4 | Internal to flowGraph-generate |
| graphEditor(execute) → circuitSimulator | 2 | Internal to flowGraph-execute |
| graphEditor(root) → circuitSimulator | 1 | Interface needed: root→execute (ConnectionAnimation) |
| kotlinCompiler → circuitSimulator | 0 | No dependency |
| circuitSimulator → kotlinCompiler | 0 | No dependency |
| **Total cross-module seams** | **7** | **1 interface needed** |

### Audit File Counts

| Source Module | File Count | Bucket Distribution |
|--------------|-----------|-------------------|
| graphEditor | 77 | root: 43 (incl. Compose UI files), inspect: 7, persist: 6, compose: 4, generate: 6, execute: 1 |
| kotlinCompiler | 38 | generate: 38 |
| circuitSimulator | 5 | execute: 5 |
| **Total** | **120** | |

## Module Boundaries

Every file from the three source module audits (graphEditor: 77, kotlinCompiler: 38, circuitSimulator: 5 = 120 total) is assigned to exactly one target module below.

### flowGraph-types (9 files)

IP type lifecycle — discovery, registry, repository, file generation, and migration. Consolidates IP type concerns that were previously scattered across inspect, persist, and generate, eliminating both cyclic dependencies in the module graph.

| File | Source Module | Current Path | Previously Assigned To |
|------|-------------|-------------|----------------------|
| IPTypeDiscovery.kt | graphEditor | state/IPTypeDiscovery.kt | inspect |
| IPTypeRegistry.kt | graphEditor | state/IPTypeRegistry.kt | inspect |
| IPProperty.kt | graphEditor | model/IPProperty.kt | inspect |
| IPPropertyMeta.kt | graphEditor | model/IPPropertyMeta.kt | inspect |
| IPTypeFileMeta.kt | graphEditor | model/IPTypeFileMeta.kt | inspect |
| IPTypeMigration.kt | graphEditor | repository/IPTypeMigration.kt | inspect |
| FileIPTypeRepository.kt | graphEditor | repository/FileIPTypeRepository.kt | persist |
| SerializableIPType.kt | graphEditor | model/SerializableIPType.kt | persist |
| IPTypeFileGenerator.kt | graphEditor | state/IPTypeFileGenerator.kt | generate |

**Outbound dependencies**: fbpDsl (shared vocabulary)
**Inbound consumers**: compose, persist, generate, root (all via ipTypeMetadata). **Types is a hub module — no back-edges.**

**Why this module exists**: Without it, inspect ↔ persist and inspect ↔ generate form cycles (inspect provides IP type metadata to persist/generate, but persist provides FileIPTypeRepository back to inspect and generate provides IPTypeFileGenerator back to inspect). Extracting the IP type lifecycle into its own module makes all three relationships one-way.

### flowGraph-compose (4 files)

Graph mutation logic — the path from user gesture to valid FlowGraph. Non-UI state and logic only.

| File | Source Module | Current Path |
|------|-------------|-------------|
| CanvasInteractionViewModel.kt | graphEditor | viewmodel/CanvasInteractionViewModel.kt |
| PropertiesPanelViewModel.kt | graphEditor | viewmodel/PropertiesPanelViewModel.kt |
| LevelCompatibilityChecker.kt | graphEditor | io/.../state/LevelCompatibilityChecker.kt |
| NodePromoter.kt | graphEditor | io/.../state/NodePromoter.kt |

**Compose UI files staying in graphEditor** (per guiding principle):
- GraphState.kt (has @Composable snapshot-aware helpers)
- PropertyChangeTracker.kt (has @Composable rememberPropertyChangeTracker)
- UndoRedoManager.kt (has @Composable rememberUndoRedoManager)
- ConnectionContextMenu.kt (has @Composable)
- ConnectionHandler.kt (has @Composable rememberConnectionCreationState)
- DragAndDropHandler.kt (has @Composable rememberDragAndDropState)

**Outbound dependencies**: inspect (NodeDefinitionRegistry, PlacementLevel)
**Inbound consumers**: root (seams through GraphState which stays in root)

### flowGraph-persist (6 files) — **EXTRACTED in feature 066**

Round-trip workflow between in-memory FlowGraph and `.flow.kt` files on disk.

| File | Source Module | Current Path |
|------|-------------|-------------|
| FlowGraphSerializer.kt | graphEditor | serialization/FlowGraphSerializer.kt |
| FlowKtParser.kt | graphEditor | serialization/FlowKtParser.kt |
| GraphNodeTemplateSerializer.kt | graphEditor | serialization/GraphNodeTemplateSerializer.kt |
| GraphNodeTemplateMeta.kt | graphEditor | model/GraphNodeTemplateMeta.kt |
| GraphNodeTemplateInstantiator.kt | graphEditor | io/.../state/GraphNodeTemplateInstantiator.kt |
| GraphNodeTemplateRegistry.kt | graphEditor | io/.../state/GraphNodeTemplateRegistry.kt |

**Compose UI files staying in graphEditor** (per guiding principle):
- ViewSynchronizer.kt (Compose-aware state synchronization)
- TextualView.kt (Compose UI composable)

**Moved to flowGraph-types**: SerializableIPType.kt, FileIPTypeRepository.kt

**Outbound dependencies**: types (ipTypeMetadata for serialization)
**Inbound consumers**: root (2 seams), generate (1 — ModuleSaveService→FlowGraphSerializer), inspect (GraphNodePaletteViewModel→GraphNodeTemplateMeta)

### flowGraph-execute (1 file from graphEditor + 5 from circuitSimulator = 6 files)

Running a flow graph and observing results — from "press Play" to "see results."

**From circuitSimulator (absorbed entirely)**:

| File | Source Module | Current Path |
|------|-------------|-------------|
| CircuitSimulator.kt | circuitSimulator | CircuitSimulator.kt |
| ConnectionAnimation.kt | circuitSimulator | ConnectionAnimation.kt |
| DataFlowAnimationController.kt | circuitSimulator | DataFlowAnimationController.kt |
| DataFlowDebugger.kt | circuitSimulator | DataFlowDebugger.kt |
| RuntimeSession.kt | circuitSimulator | RuntimeSession.kt |

**From graphEditor (execute bucket)**:

| File | Source Module | Current Path |
|------|-------------|-------------|
| ModuleSessionFactory.kt | graphEditor | ui/ModuleSessionFactory.kt |

**Compose UI files staying in graphEditor** (per guiding principle):
- RuntimePreviewPanel.kt (has @Composable — Compose UI for runtime preview)

**Outbound dependencies**: inspect (ModuleSessionFactory→NodeDefinitionRegistry)
**Inbound consumers**: root (1 seam — FlowGraphCanvas→ConnectionAnimation)

### flowGraph-generate (44 files: 38 from kotlinCompiler + 6 from graphEditor)

Producing deployable code from a graph — FlowGraph to generated source files on disk.

**From kotlinCompiler (absorbed entirely — 38 files)**:

| File | Source Module | Current Path |
|------|-------------|-------------|
| BuildScriptGenerator.kt | kotlinCompiler | generator/BuildScriptGenerator.kt |
| ComponentGenerator.kt | kotlinCompiler | generator/ComponentGenerator.kt |
| ConfigAwareGenerator.kt | kotlinCompiler | generator/ConfigAwareGenerator.kt |
| ConnectionWiringResolver.kt | kotlinCompiler | generator/ConnectionWiringResolver.kt |
| EntityCUDCodeNodeGenerator.kt | kotlinCompiler | generator/EntityCUDCodeNodeGenerator.kt |
| EntityDisplayCodeNodeGenerator.kt | kotlinCompiler | generator/EntityDisplayCodeNodeGenerator.kt |
| EntityFlowGraphBuilder.kt | kotlinCompiler | generator/EntityFlowGraphBuilder.kt |
| EntityModuleGenerator.kt | kotlinCompiler | generator/EntityModuleGenerator.kt |
| EntityModuleSpec.kt | kotlinCompiler | generator/EntityModuleSpec.kt |
| EntityPersistenceGenerator.kt | kotlinCompiler | generator/EntityPersistenceGenerator.kt |
| EntityRepositoryCodeNodeGenerator.kt | kotlinCompiler | generator/EntityRepositoryCodeNodeGenerator.kt |
| EntityUIGenerator.kt | kotlinCompiler | generator/EntityUIGenerator.kt |
| FlowGenerator.kt | kotlinCompiler | generator/FlowGenerator.kt |
| FlowGraphFactoryGenerator.kt | kotlinCompiler | generator/FlowGraphFactoryGenerator.kt |
| FlowKtGenerator.kt | kotlinCompiler | generator/FlowKtGenerator.kt |
| GenericNodeGenerator.kt | kotlinCompiler | generator/GenericNodeGenerator.kt |
| KotlinCodeGenerator.kt | kotlinCompiler | generator/KotlinCodeGenerator.kt |
| ModuleGenerator.kt | kotlinCompiler | generator/ModuleGenerator.kt |
| ObservableStateResolver.kt | kotlinCompiler | generator/ObservableStateResolver.kt |
| RepositoryCodeGenerator.kt | kotlinCompiler | generator/RepositoryCodeGenerator.kt |
| RuntimeControllerAdapterGenerator.kt | kotlinCompiler | generator/RuntimeControllerAdapterGenerator.kt |
| RuntimeControllerGenerator.kt | kotlinCompiler | generator/RuntimeControllerGenerator.kt |
| RuntimeControllerInterfaceGenerator.kt | kotlinCompiler | generator/RuntimeControllerInterfaceGenerator.kt |
| RuntimeFlowGenerator.kt | kotlinCompiler | generator/RuntimeFlowGenerator.kt |
| RuntimeTypeResolver.kt | kotlinCompiler | generator/RuntimeTypeResolver.kt |
| RuntimeViewModelGenerator.kt | kotlinCompiler | generator/RuntimeViewModelGenerator.kt |
| UserInterfaceStubGenerator.kt | kotlinCompiler | generator/UserInterfaceStubGenerator.kt |
| NodeTemplate.kt | kotlinCompiler | templates/NodeTemplate.kt |
| SourceTemplate.kt | kotlinCompiler | templates/SourceTemplate.kt |
| FilterTemplate.kt | kotlinCompiler | templates/FilterTemplate.kt |
| MergerTemplate.kt | kotlinCompiler | templates/MergerTemplate.kt |
| SinkTemplate.kt | kotlinCompiler | templates/SinkTemplate.kt |
| SplitterTemplate.kt | kotlinCompiler | templates/SplitterTemplate.kt |
| TransformerTemplate.kt | kotlinCompiler | templates/TransformerTemplate.kt |
| ValidatorTemplate.kt | kotlinCompiler | templates/ValidatorTemplate.kt |
| LicenseValidator.kt | kotlinCompiler | validator/LicenseValidator.kt |
| RegenerateStopWatch.kt | kotlinCompiler | jvmMain/tools/RegenerateStopWatch.kt |
| GenerateGeoLocationModule.kt | kotlinCompiler | jvmMain/GenerateGeoLocationModule.kt |

**From graphEditor (generate bucket — 6 files)**:

| File | Source Module | Current Path |
|------|-------------|-------------|
| IPGeneratorViewModel.kt | graphEditor | viewmodel/IPGeneratorViewModel.kt |
| NodeGeneratorViewModel.kt | graphEditor | viewmodel/NodeGeneratorViewModel.kt |
| CompilationService.kt | graphEditor | compilation/CompilationService.kt |
| CompilationValidator.kt | graphEditor | compilation/CompilationValidator.kt |
| RequiredPropertyValidator.kt | graphEditor | compilation/RequiredPropertyValidator.kt |
| ModuleSaveService.kt | graphEditor | save/ModuleSaveService.kt |

**Compose UI files staying in graphEditor** (per guiding principle):
- IPGeneratorPanel.kt (has @Composable — IP type generator UI)
- NodeGeneratorPanel.kt (has @Composable — node generator UI)

**Moved to flowGraph-types**: IPTypeFileGenerator.kt

**Outbound dependencies**: types (ipTypeMetadata), inspect (NodeGeneratorVM→NodeDefinitionRegistry), persist (ModuleSaveService→FlowGraphSerializer)
**Inbound consumers**: root (0 direct — generation is triggered from root ViewModels)

### flowGraph-inspect (7 files)

Node discovery, definition registry, palette state management, filesystem scanning, and preview discovery. Non-UI logic only.

| File | Source Module | Current Path |
|------|-------------|-------------|
| NodeDefinitionRegistry.kt | graphEditor | io/.../state/NodeDefinitionRegistry.kt |
| CodeEditorViewModel.kt | graphEditor | viewmodel/CodeEditorViewModel.kt |
| IPPaletteViewModel.kt | graphEditor | viewmodel/IPPaletteViewModel.kt |
| GraphNodePaletteViewModel.kt | graphEditor | viewmodel/GraphNodePaletteViewModel.kt |
| NodePaletteViewModel.kt | graphEditor | viewmodel/NodePaletteViewModel.kt |
| ComposableDiscovery.kt | graphEditor | ui/ComposableDiscovery.kt |
| DynamicPreviewDiscovery.kt | graphEditor | ui/DynamicPreviewDiscovery.kt |

**Compose UI files staying in graphEditor** (per guiding principle):
- CodeEditor.kt (has @Composable — code editor UI)
- ColorEditor.kt (has @Composable — color picker UI)
- IPPalette.kt (has @Composable — IP type palette UI)
- NodePalette.kt (has @Composable — node palette UI)
- SyntaxHighlighter.kt (has @Composable — syntax highlighting UI)

**Already in fbpDsl**: PlacementLevel.kt (not a graphEditor file — lives in fbpDsl/src/commonMain)

**Moved to flowGraph-types**: IPTypeDiscovery.kt, IPTypeRegistry.kt, IPProperty.kt, IPPropertyMeta.kt, IPTypeFileMeta.kt, IPTypeMigration.kt

**Outbound dependencies**: types (IPPaletteVM uses ipTypeMetadata), persist (GraphNodePaletteViewModel→GraphNodeTemplateMeta)
**Inbound consumers**: compose (nodeDescriptors), execute (nodeDescriptors), generate (nodeDescriptors), root (nodeDescriptors). Inspect provides node discovery; IP type concerns are now in types.

### graphEditor — composition root (43 files, stays)

Compose UI composables, orchestration ViewModels, DI wiring, renderers, and all @Composable presentation files retained per the "Compose stays in graphEditor" principle. No business logic.

**Original root files (27)**:

| File | Source Module | Current Path |
|------|-------------|-------------|
| CanvasControls.kt | graphEditor | ui/CanvasControls.kt |
| CollapsiblePanel.kt | graphEditor | ui/CollapsiblePanel.kt |
| ErrorDisplay.kt | graphEditor | ui/ErrorDisplay.kt |
| FileSelector.kt | graphEditor | ui/FileSelector.kt |
| FlowGraphCanvas.kt | graphEditor | ui/FlowGraphCanvas.kt |
| FlowGraphPropertiesDialog.kt | graphEditor | ui/FlowGraphPropertiesDialog.kt |
| GraphNodePaletteSection.kt | graphEditor | ui/GraphNodePaletteSection.kt |
| PropertiesPanel.kt | graphEditor | ui/PropertiesPanel.kt |
| PropertyEditors.kt | graphEditor | ui/PropertyEditors.kt |
| ViewToggle.kt | graphEditor | ui/ViewToggle.kt |
| BaseState.kt | graphEditor | viewmodel/BaseState.kt |
| GraphEditorViewModel.kt | graphEditor | viewmodel/GraphEditorViewModel.kt |
| SharedStateProvider.kt | graphEditor | viewmodel/SharedStateProvider.kt |
| NodeGeneratorState.kt | graphEditor | state/NodeGeneratorState.kt |
| ConnectionRenderer.kt | graphEditor | rendering/ConnectionRenderer.kt |
| NodeRenderer.kt | graphEditor | rendering/NodeRenderer.kt |
| PortRenderer.kt | graphEditor | rendering/PortRenderer.kt |
| GroupContextMenuState.kt | graphEditor | io/.../state/GroupContextMenuState.kt |
| NavigationContext.kt | graphEditor | io/.../state/NavigationContext.kt |
| SelectableElement.kt | graphEditor | io/.../state/SelectableElement.kt |
| SelectionState.kt | graphEditor | io/.../state/SelectionState.kt |
| Main.kt | graphEditor | Main.kt |
| GraphNodeRenderer.kt | graphEditor | io/.../ui/GraphNodeRenderer.kt |
| GroupContextMenu.kt | graphEditor | io/.../ui/GroupContextMenu.kt |
| NavigationBreadcrumb.kt | graphEditor | io/.../ui/NavigationBreadcrumb.kt |
| NavigationZoomOutButton.kt | graphEditor | io/.../ui/NavigationZoomOutButton.kt |
| SelectionBox.kt | graphEditor | io/.../ui/SelectionBox.kt |

**Compose UI files retained from extraction buckets (+16)**:

| File | Originally Assigned To | Reason Retained |
|------|----------------------|-----------------|
| ViewSynchronizer.kt | persist | @Composable — state synchronization UI |
| TextualView.kt | persist | @Composable — textual view composable |
| GraphState.kt | compose | @Composable snapshot-aware helpers |
| PropertyChangeTracker.kt | compose | @Composable rememberPropertyChangeTracker |
| UndoRedoManager.kt | compose | @Composable rememberUndoRedoManager |
| ConnectionContextMenu.kt | compose | @Composable — connection context menu UI |
| ConnectionHandler.kt | compose | @Composable rememberConnectionCreationState |
| DragAndDropHandler.kt | compose | @Composable rememberDragAndDropState |
| RuntimePreviewPanel.kt | execute | @Composable — runtime preview UI |
| IPGeneratorPanel.kt | generate | @Composable — IP type generator UI |
| NodeGeneratorPanel.kt | generate | @Composable — node generator UI |
| CodeEditor.kt | inspect | @Composable — code editor UI |
| ColorEditor.kt | inspect | @Composable — color picker UI |
| IPPalette.kt | inspect | @Composable — IP type palette UI |
| NodePalette.kt | inspect | @Composable — node palette UI |
| SyntaxHighlighter.kt | inspect | @Composable — syntax highlighting UI |

**Total root: 43 files** (27 original + 16 Compose UI retained)

### File Count Validation

| Target Module | Files | Source Breakdown |
|--------------|-------|-----------------|
| flowGraph-types | 9 | graphEditor: 9 |
| flowGraph-compose | 4 | graphEditor: 4 |
| flowGraph-persist | 6 | graphEditor: 6 |
| flowGraph-execute | 6 | circuitSimulator: 5, graphEditor: 1 |
| flowGraph-generate | 44 | kotlinCompiler: 38, graphEditor: 6 |
| flowGraph-inspect | 7 | graphEditor: 7 |
| graphEditor (root) | 43 | graphEditor: 43 (27 original + 16 Compose UI retained) |
| **Total** | **119** | graphEditor: 76*, kotlinCompiler: 38, circuitSimulator: 5 |

*PlacementLevel.kt was originally counted in the graphEditor audit (77 files) but already lives in fbpDsl/src/commonMain. Corrected count: 76 actual graphEditor files.

**Note on "Compose stays in graphEditor" correction**: The original audit assigned files to extraction buckets without distinguishing UI composables from non-UI logic. During feature 066 (persist extraction), the principle was established that @Composable files stay in graphEditor. This table reflects the corrected counts after applying that principle across all buckets. 16 Compose UI files moved from extraction buckets back to graphEditor root.

---

## Public APIs

> **Note**: The original migration plan (feature 064) designed service interfaces (Koin-wired DI). During implementation (features 065-066), the architectural decision was made that module boundaries are expressed as **FBP-native data flow through CodeNode ports** — not service interfaces. The interfaces below are preserved for reference but are **superseded** by the CodeNode port signatures defined in each module's spec. See the "Guiding Principle" section above.

Each target module was originally planned to expose a public API through Kotlin interfaces. These have been replaced by coarse-grained CodeNode boundaries with typed input/output ports.

### flowGraph-types API

**Interface: `IPTypeRegistryService`**
Exposes IP type lookup from `IPTypeRegistry.kt`. This is the **most broadly consumed interface** — used by compose, persist, generate, and root.

```kotlin
interface IPTypeRegistryService {
    val registeredTypes: StateFlow<List<InformationPacketType>>
    fun getType(typeId: String): InformationPacketType?
    fun registerType(type: InformationPacketType)
    fun search(query: String): List<InformationPacketType>
}
```

**Current call sites** (consumed by 4 other slices):
- `GraphState.kt`, `ConnectionContextMenu.kt` (compose→types)
- `GraphNodeTemplateSerializer.kt` (persist→types)
- `SharedStateProvider.kt`, `PropertiesPanel.kt`, `GraphNodePaletteSection.kt` (root→types)
- `IPGeneratorViewModel.kt` (generate→types)

**Interface: `IPTypeGenerationService`**
Exposes IP type file generation from `IPTypeFileGenerator.kt`.

```kotlin
interface IPTypeGenerationService {
    fun generateIPTypeFile(name: String, properties: List<IPProperty>, level: PlacementLevel): String
}
```

**Note**: `IPTypeMigration.kt` (also in types) calls `IPTypeFileGenerator.kt` (also in types) — this was the former inspect→generate back-edge, now internal to the types module.

**Interface: `IPTypeRepositoryService`**
Exposes IP type persistence from `FileIPTypeRepository.kt`.

```kotlin
interface IPTypeRepositoryService {
    fun loadTypes(): List<SerializableIPType>
    fun saveTypes(types: List<SerializableIPType>)
    fun migrate()
}
```

**Note**: `IPPaletteViewModel.kt` (inspect) and `IPTypeMigration.kt` (types) were the former consumers of `FileIPTypeRepository` — the latter is now internal to types, and the former uses `IPTypeRepositoryService`.

### flowGraph-compose API

**Interface: `GraphCompositionService`**
Exposes graph mutation operations currently provided by `GraphState.kt`.

```kotlin
interface GraphCompositionService {
    val flowGraph: StateFlow<FlowGraph>
    val selectedElements: StateFlow<Set<SelectableElement>>
    val validationErrors: StateFlow<List<ValidationError>>

    fun addNode(node: Node, position: Node.Position)
    fun removeNode(nodeId: String)
    fun connectPorts(sourceNodeId: String, sourcePortId: String, targetNodeId: String, targetPortId: String)
    fun disconnectPorts(connectionId: String)
    fun validateConnection(sourcePort: Port<*>, targetPort: Port<*>): Boolean
    fun detectCycles(): List<List<String>>
    fun updateNodePosition(nodeId: String, position: Node.Position)
    fun selectElement(element: SelectableElement)
    fun clearSelection()
}
```

**Current call sites** (8 seams from root):
- `FlowGraphCanvas.kt` → GraphState (state sharing)
- `PropertiesPanel.kt` → GraphState (state sharing)
- `ErrorDisplay.kt` → GraphState (state sharing)
- `CanvasControls.kt` → GraphState (function call)
- `GraphEditorViewModel.kt` → GraphState (state sharing)
- `SharedStateProvider.kt` → GraphState, UndoRedoManager, PropertyChangeTracker (state sharing)
- `PropertiesPanel.kt` → PropertiesPanelViewModel, LevelCompatibilityChecker (function call)

**Interface: `UndoRedoService`**
Exposes undo/redo operations currently in `UndoRedoManager.kt`.

```kotlin
interface UndoRedoService {
    val canUndo: StateFlow<Boolean>
    val canRedo: StateFlow<Boolean>
    fun undo()
    fun redo()
    fun executeCommand(command: Command)
}
```

### flowGraph-persist API

**Interface: `FlowGraphPersistenceService`**
Exposes serialization/deserialization currently in `FlowGraphSerializer.kt` and `FlowKtParser.kt`.

```kotlin
interface FlowGraphPersistenceService {
    fun serialize(flowGraph: FlowGraph): String
    fun deserialize(flowKtContent: String): FlowGraph
}
```

**Current call sites**:
- `GraphEditorViewModel.kt` → FlowKtParser (root→persist)
- `ModuleSaveService.kt` → FlowGraphSerializer (generate→persist)

**Interface: `GraphNodeTemplateService`**
Exposes template management currently split across `GraphNodeTemplateRegistry`, `GraphNodeTemplateSerializer`, `GraphNodeTemplateInstantiator`.

```kotlin
interface GraphNodeTemplateService {
    val templates: StateFlow<List<GraphNodeTemplateMeta>>
    fun saveTemplate(flowGraph: FlowGraph, name: String, level: PlacementLevel): GraphNodeTemplateMeta
    fun loadTemplate(meta: GraphNodeTemplateMeta): FlowGraph
    fun instantiateTemplate(meta: GraphNodeTemplateMeta): FlowGraph
    fun removeTemplate(meta: GraphNodeTemplateMeta)
}
```

**Current call sites**:
- `GraphNodePaletteSection.kt` → GraphNodeTemplateMeta (root→persist, type reference)
- `GraphNodePaletteViewModel.kt` → GraphNodeTemplateMeta (inspect→persist, type reference)

### flowGraph-execute API

**Interface: `RuntimeExecutionService`**
Exposes runtime session lifecycle currently in `RuntimeSession.kt`.

```kotlin
interface RuntimeExecutionService {
    val executionState: StateFlow<ExecutionState>
    val attenuationDelayMs: StateFlow<Long>
    val animateDataFlow: StateFlow<Boolean>
    val activeAnimations: StateFlow<List<ConnectionAnimation>>

    fun start()
    fun stop()
    fun pause()
    fun resume()
    fun setAttenuation(ms: Int)
    fun setAnimateDataFlow(enabled: Boolean)
}
```

**Interface: `ConnectionAnimationProvider`**
Exposes animation data for canvas rendering. This is the **one true cross-module interface** needed between root and execute (the only post-extraction cross-module seam).

```kotlin
interface ConnectionAnimationProvider {
    val activeAnimations: StateFlow<List<ConnectionAnimation>>
}
```

**Current call site**:
- `FlowGraphCanvas.kt` → ConnectionAnimation (root→execute, type reference)

**Interface: `DebugSnapshotProvider`**
Exposes per-connection value snapshots from `DataFlowDebugger.kt`.

```kotlin
interface DebugSnapshotProvider {
    fun getSnapshotValue(connectionId: String): Any?
    fun clear()
}
```

### flowGraph-generate API

**Interface: `CodeGenerationService`**
Exposes module generation currently spread across `KotlinCodeGenerator`, `ModuleGenerator`, `ModuleSaveService`.

```kotlin
interface CodeGenerationService {
    fun generateModule(flowGraph: FlowGraph, outputDir: String, config: GenerationConfig): GeneratedProject
    fun generateFlowKt(flowGraph: FlowGraph, packageName: String): String
    fun validateForGeneration(flowGraph: FlowGraph): List<ValidationError>
}
```

**Moved to flowGraph-types**: `IPTypeGenerationService` (IPTypeFileGenerator.kt moved to types)

### flowGraph-inspect API

**Interface: `NodeRegistryService`**
Exposes node discovery from `NodeDefinitionRegistry.kt`. Consumed by compose, execute, generate, and root.

```kotlin
interface NodeRegistryService {
    val nodeDefinitions: StateFlow<List<NodeDefinition>>
    fun getDefinition(nodeTypeId: String): NodeDefinition?
    fun refresh()
}
```

**Current call sites** (consumed by 4 other slices):
- `DragAndDropHandler.kt` (compose→inspect)
- `LevelCompatibilityChecker.kt` (compose→inspect)
- `ModuleSessionFactory.kt` (execute→inspect)
- `NodeGeneratorViewModel.kt` (generate→inspect)

**Moved to flowGraph-types**: `IPTypeRegistryService` (IPTypeRegistry.kt moved to types)

### Interface Summary

| Module | Interfaces Exposed | Consumers |
|--------|-------------------|-----------|
| flowGraph-types | IPTypeRegistryService, IPTypeGenerationService, IPTypeRepositoryService | compose, persist, generate, inspect, root |
| flowGraph-compose | GraphCompositionService, UndoRedoService | root |
| flowGraph-persist | FlowGraphPersistenceService, GraphNodeTemplateService | root, generate |
| flowGraph-execute | RuntimeExecutionService, ConnectionAnimationProvider, DebugSnapshotProvider | root |
| flowGraph-generate | CodeGenerationService | root |
| flowGraph-inspect | NodeRegistryService | compose, execute, generate, root |

---

## Extraction Order

Per research.md R5: extract in order of decreasing independence and decreasing risk. Types first because it is the most broadly consumed module and has no outbound dependencies.

### Step 1: flowGraph-types (9 files)

**Why first**: IP type lifecycle is the most broadly consumed concern — compose, persist, generate, inspect, and root all depend on it. Extracting types first makes its interfaces available to every subsequent step. It has no outbound dependencies (only fbpDsl). Its extraction also eliminates both cycles from the module graph immediately.

**Files that move**: 9 files from graphEditor (2 state/, 4 model/, 2 repository/, 1 state/)

**Interfaces created**:
- `IPTypeRegistryService` (IP type lookup — most broadly consumed)
- `IPTypeGenerationService` (IP type file generation)
- `IPTypeRepositoryService` (IP type persistence)

**Call sites that change to delegation**:
- `GraphState.kt`, `ConnectionContextMenu.kt` → use `IPTypeRegistryService` instead of `IPTypeRegistry` directly
- `GraphNodeTemplateSerializer.kt` → uses `IPTypeRegistryService`
- `IPGeneratorViewModel.kt` → uses `IPTypeRegistryService`
- `SharedStateProvider.kt`, `PropertiesPanel.kt`, `GraphNodePaletteSection.kt` → use `IPTypeRegistryService`
- `IPPaletteViewModel.kt` → uses `IPTypeRepositoryService` instead of `FileIPTypeRepository` directly

**Characterization tests that must pass**:
- All existing tests: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest`

### Step 2: flowGraph-persist (6 files) — **COMPLETED (feature 066)**

**Why second**: Serialization is self-contained — clear inputs (FlowGraph) and outputs (.flow.kt text). Depends only on types (for IP type metadata during serialization) which is already extracted. No runtime or UI state management.

**Files that move**: 6 files from graphEditor (3 serialization/, 1 model/, 2 io/.../state/ template files). ViewSynchronizer.kt and TextualView.kt stay in graphEditor (Compose UI).

**Module boundary**: FlowGraphPersistCodeNode with 2 inputs (flowGraphModel, ipTypeMetadata) and 3 outputs (serializedOutput, loadedFlowGraph, graphNodeTemplates). FBP-native data flow, no service interfaces.

**Characterization tests that must pass**:
- `SerializationRoundTripCharacterizationTest` (graphEditor)
- `FlowKtGeneratorCharacterizationTest` (kotlinCompiler — FlowKtGenerator also produces .flow.kt but lives in generate, not persist)
- All existing tests: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest`

### Step 3: flowGraph-inspect (7 files)

**Why third**: Node discovery logic is read-only with respect to graph state. No longer depends on persist or generate (those dependencies moved to types). Inspect provides node discovery consumed by compose, execute, and generate.

**Files that move**: 7 non-UI files from graphEditor (1 io/.../state/, 4 viewmodel/, 2 ui/ discovery files). 5 Compose UI composables (CodeEditor, ColorEditor, IPPalette, NodePalette, SyntaxHighlighter) stay in graphEditor. PlacementLevel.kt already in fbpDsl.

**Module boundary**: FlowGraphInspectCodeNode with 2 inputs (filesystemPaths, classpathEntries) and 1 output (nodeDescriptors). FBP-native data flow, no service interfaces.

**Call sites that change to delegation**:
- `DragAndDropHandler.kt` → uses `NodeRegistryService` for node definitions
- `LevelCompatibilityChecker.kt` → uses `NodeRegistryService`
- `ModuleSessionFactory.kt` → uses `NodeRegistryService`
- `NodeGeneratorViewModel.kt` → uses `NodeRegistryService`

**Characterization tests that must pass**:
- `ViewModelCharacterizationTest` (graphEditor — palette and registry state)
- All existing tests across all modules

### Step 4: flowGraph-execute (6 files)

**Why fourth**: Runtime pipeline has clear boundaries (FlowGraph in, execution state out). Depends on inspect (ModuleSessionFactory→NodeDefinitionRegistry) which is already extracted. The animation state integration with UI requires the `ConnectionAnimationProvider` interface.

**Files that move**: 5 files from circuitSimulator (entire module) + 1 file from graphEditor (ModuleSessionFactory). RuntimePreviewPanel.kt stays in graphEditor (Compose UI).

**Interfaces created**:
- `RuntimeExecutionService` (lifecycle control)
- `ConnectionAnimationProvider` (animation data for canvas)
- `DebugSnapshotProvider` (value snapshots)

**Call sites that change to delegation**:
- `FlowGraphCanvas.kt` → uses `ConnectionAnimationProvider` instead of `ConnectionAnimation` directly
- Root ViewModels → use `RuntimeExecutionService` for preview panel control

**Characterization tests that must pass**:
- `RuntimeSessionCharacterizationTest` (circuitSimulator)
- `RuntimeExecutionCharacterizationTest` (graphEditor)
- All existing tests across all modules

### Step 5: flowGraph-generate (44 files)

**Why fifth**: Code generation depends on types (IP type metadata), persist (serialized output), and inspect (node definitions). All three are already extracted and stable. This is the largest extraction (44 files) but the simplest structurally — kotlinCompiler moves wholesale, plus 6 graphEditor generate-bucket files.

**Files that move**: 38 files from kotlinCompiler (entire module) + 6 files from graphEditor (2 viewmodel/, 3 compilation/, 1 save/). IPGeneratorPanel.kt and NodeGeneratorPanel.kt stay in graphEditor (Compose UI).

**Interfaces created**:
- `CodeGenerationService` (module generation)

**Call sites that change to delegation**:
- Root orchestration → uses `CodeGenerationService` instead of calling generators directly

**Characterization tests that must pass**:
- `CodeGenerationCharacterizationTest` (kotlinCompiler)
- `FlowKtGeneratorCharacterizationTest` (kotlinCompiler)
- All existing tests across all modules

### Step 6: flowGraph-compose (4 files)

**Why last**: Graph composition logic that doesn't require Compose. All other slices are stable, so compose extraction only needs to create interfaces consumed by root (the composition shell).

**Files that move**: 4 non-UI files from graphEditor (2 viewmodel/, 2 io/.../state/). 6 Compose UI files (GraphState, PropertyChangeTracker, UndoRedoManager, ConnectionContextMenu, ConnectionHandler, DragAndDropHandler) stay in graphEditor.

**Interfaces created**:
- `GraphCompositionService` (graph mutations)
- `UndoRedoService` (undo/redo)

**Call sites that change to delegation**:
- `FlowGraphCanvas.kt` → uses `GraphCompositionService` instead of `GraphState` directly
- `PropertiesPanel.kt` → uses `GraphCompositionService` for state and `UndoRedoService` for undo
- `ErrorDisplay.kt` → uses `GraphCompositionService.validationErrors`
- `CanvasControls.kt` → uses `GraphCompositionService` for zoom/pan
- `GraphEditorViewModel.kt` → uses `GraphCompositionService` and `UndoRedoService`
- `SharedStateProvider.kt` → provides compose services via DI

**Characterization tests that must pass**:
- `GraphDataOpsCharacterizationTest` (graphEditor)
- `ViewModelCharacterizationTest` (graphEditor)
- All existing tests across all modules

### Extraction Order Dependency Diagram

```
Step 1: flowGraph-types ────────────────────────────────────────────────►
            │
            │ types interfaces available (most broadly consumed)
            ▼
Step 2: flowGraph-persist ──────────────────────────────────────────────►
            │
            │ persist interfaces available
            ▼
Step 3: flowGraph-inspect ──────────────────────────────────────────────►
            │
            │ inspect interfaces available
            ▼
Step 4: flowGraph-execute ──────────────────────────────────────────────►
            │
            │ execute interfaces available
            ▼
Step 5: flowGraph-generate ─────────────────────────────────────────────►
            │
            │ generate interfaces available
            ▼
Step 6: flowGraph-compose ──────────────────────────────────────────────►
            │
            │ all slices extracted
            ▼
        graphEditor = composition root + Compose UI (43 files)
```

### Extraction Order Dependency Validation

| Step | Module | Depends On | Already Extracted? | Circular? |
|------|--------|-----------|-------------------|-----------|
| 1 | types | fbpDsl (shared vocab) | N/A (not extracted) | No |
| 2 | persist | types (ipTypeMetadata), fbpDsl | Step 1 ✓ | No |
| 3 | inspect | fbpDsl | N/A (no module deps) | No |
| 4 | execute | inspect (ModuleSessionFactory→NodeDefinitionRegistry), fbpDsl | Step 3 ✓ | No |
| 5 | generate | types (ipTypeMetadata), persist (serializedOutput), inspect (nodeDescriptors), fbpDsl | Steps 1-3 ✓ | No |
| 6 | compose | types (ipTypeMetadata), inspect (nodeDescriptors), fbpDsl | Steps 1, 3 ✓ | No |

**No circular dependencies**: Every step depends only on modules extracted in earlier steps (or fbpDsl which is never extracted — it's shared vocabulary). The former cycles (inspect ↔ persist, inspect ↔ generate) were eliminated by extracting flowGraph-types.

---

## Step-by-Step Instructions

### Before any extraction

1. Ensure all characterization tests pass: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest`
2. Create a new branch per extraction step (e.g., `065-extract-persist`, `066-extract-inspect`, etc.)
3. Each step follows the Strangler Fig pattern: define interface → copy implementation → delegate → delete duplicate

### Per-step template (Strangler Fig + CodeNode)

For each extraction step N:

1. **Create module**: Add `flowGraph-{name}/build.gradle.kts` and update `settings.gradle.kts`
2. **Copy non-UI files**: Copy files listed in the Module Boundaries section (excluding @Composable files) to the new module with updated packages
3. **TDD tests**: Write tests for the CodeNode port signatures and data flow — tests must compile and fail
4. **Implement CodeNode**: Create coarse-grained CodeNode wrapping the module behind FBP-native input/output ports
5. **Add dependency**: `graphEditor/build.gradle.kts` adds `implementation(project(":flowGraph-{name}"))`
6. **Migrate call sites**: Change graphEditor imports to use the new module's packages
7. **Run tests**: `./gradlew :graphEditor:jvmTest :flowGraph-{name}:jvmTest` — all tests must pass
8. **Delete originals**: Remove the copied files from graphEditor
9. **Wire architecture**: Populate the GraphNode in architecture.flow.kt with the child CodeNode and port mappings
10. **Verify**: Full test suite green, no circular dependencies, Strangler Fig commit sequence visible in git history

### Post-extraction state

After all six extractions:
- `graphEditor/` contains 43 files: Compose composables, orchestration ViewModels, renderers, DI wiring, Main.kt, and all @Composable presentation files retained per the "Compose stays in graphEditor" principle
- `flowGraph-types/` contains 9 files — IP type lifecycle (FBP-native CodeNode boundary)
- `flowGraph-compose/` contains 4 files — non-UI graph mutation logic (FBP-native CodeNode boundary)
- `flowGraph-persist/` contains 6 files — serialization/persistence (FBP-native CodeNode boundary) — **EXTRACTED (feature 066)**
- `flowGraph-execute/` contains 6 files — runtime execution (FBP-native CodeNode boundary)
- `flowGraph-generate/` contains 44 files — code generation (FBP-native CodeNode boundary)
- `flowGraph-inspect/` contains 7 files — node discovery/inspection (FBP-native CodeNode boundary)
- `kotlinCompiler/` module is **removed** (absorbed into flowGraph-generate)
- `circuitSimulator/` module is **removed** (absorbed into flowGraph-execute)
- All modules depend on `fbpDsl` for shared vocabulary (FlowGraph, Node, Port, Connection, etc.)
- Each extracted module exposes its boundary as a coarse-grained CodeNode with FBP-native data flow ports — no Koin/DI service interfaces
- The module dependency graph is a **DAG** — no circular dependencies
