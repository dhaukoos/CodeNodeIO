# Vertical Slice Migration Map

**Feature**: 064-vertical-slice-refactor
**Date**: 2026-04-02

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

**Resolution**: RuntimePreviewPanel and ModuleSessionFactory are in the `execute` bucket — they move to flowGraph-execute alongside circuitSimulator files, so those seams become internal. FlowGraphCanvas.kt references ConnectionAnimation for rendering animated dots; this is the one true cross-module interface needed (root→execute boundary).

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
| graphEditor | 77 | root: 28, inspect: 17, persist: 13, compose: 9, generate: 7, execute: 3 |
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

### flowGraph-compose (10 files)

Graph mutation logic — the path from user gesture to valid FlowGraph.

| File | Source Module | Current Path |
|------|-------------|-------------|
| GraphState.kt | graphEditor | state/GraphState.kt |
| PropertyChangeTracker.kt | graphEditor | state/PropertyChangeTracker.kt |
| UndoRedoManager.kt | graphEditor | state/UndoRedoManager.kt |
| ConnectionContextMenu.kt | graphEditor | ui/ConnectionContextMenu.kt |
| ConnectionHandler.kt | graphEditor | ui/ConnectionHandler.kt |
| DragAndDropHandler.kt | graphEditor | ui/DragAndDropHandler.kt |
| CanvasInteractionViewModel.kt | graphEditor | viewmodel/CanvasInteractionViewModel.kt |
| PropertiesPanelViewModel.kt | graphEditor | viewmodel/PropertiesPanelViewModel.kt |
| LevelCompatibilityChecker.kt | graphEditor | io/.../state/LevelCompatibilityChecker.kt |
| NodePromoter.kt | graphEditor | io/.../state/NodePromoter.kt |

**Outbound dependencies**: inspect (IPTypeRegistry, NodeDefinitionRegistry, PlacementLevel)
**Inbound consumers**: root (8 seams — GraphState is the most referenced target)

### flowGraph-persist (8 files)

Round-trip workflow between in-memory FlowGraph and `.flow.kt` files on disk.

| File | Source Module | Current Path |
|------|-------------|-------------|
| FlowGraphSerializer.kt | graphEditor | serialization/FlowGraphSerializer.kt |
| FlowKtParser.kt | graphEditor | serialization/FlowKtParser.kt |
| GraphNodeTemplateSerializer.kt | graphEditor | serialization/GraphNodeTemplateSerializer.kt |
| GraphNodeTemplateMeta.kt | graphEditor | model/GraphNodeTemplateMeta.kt |
| ViewSynchronizer.kt | graphEditor | state/ViewSynchronizer.kt |
| TextualView.kt | graphEditor | ui/TextualView.kt |
| GraphNodeTemplateInstantiator.kt | graphEditor | io/.../state/GraphNodeTemplateInstantiator.kt |
| GraphNodeTemplateRegistry.kt | graphEditor | io/.../state/GraphNodeTemplateRegistry.kt |

**Moved to flowGraph-types**: SerializableIPType.kt, FileIPTypeRepository.kt

**Outbound dependencies**: types (ipTypeMetadata for serialization), compose (ViewSynchronizer→GraphState), inspect (TextualView→SyntaxHighlighter)
**Inbound consumers**: root (2 seams), generate (1 — ModuleSaveService→FlowGraphSerializer)

### flowGraph-execute (7 files from graphEditor + 5 from circuitSimulator = 12 files)

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
| RuntimePreviewPanel.kt | graphEditor | ui/RuntimePreviewPanel.kt |

**Note**: Only 2 graphEditor files are in the execute bucket (not 3 as estimated in the audit summary — the summary count was slightly off). Total: 7 files.

**Outbound dependencies**: inspect (ModuleSessionFactory→NodeDefinitionRegistry)
**Inbound consumers**: root (1 seam — FlowGraphCanvas→ConnectionAnimation)

### flowGraph-generate (46 files: 38 from kotlinCompiler + 8 from graphEditor)

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

**From graphEditor (generate bucket — 8 files)**:

| File | Source Module | Current Path |
|------|-------------|-------------|
| IPGeneratorPanel.kt | graphEditor | ui/IPGeneratorPanel.kt |
| NodeGeneratorPanel.kt | graphEditor | ui/NodeGeneratorPanel.kt |
| IPGeneratorViewModel.kt | graphEditor | viewmodel/IPGeneratorViewModel.kt |
| NodeGeneratorViewModel.kt | graphEditor | viewmodel/NodeGeneratorViewModel.kt |
| CompilationService.kt | graphEditor | compilation/CompilationService.kt |
| CompilationValidator.kt | graphEditor | compilation/CompilationValidator.kt |
| RequiredPropertyValidator.kt | graphEditor | compilation/RequiredPropertyValidator.kt |
| ModuleSaveService.kt | graphEditor | save/ModuleSaveService.kt |

**Moved to flowGraph-types**: IPTypeFileGenerator.kt

**Outbound dependencies**: types (ipTypeMetadata), inspect (NodeGeneratorVM→NodeDefinitionRegistry), persist (ModuleSaveService→FlowGraphSerializer)
**Inbound consumers**: root (0 direct — generation is triggered from root ViewModels)

### flowGraph-inspect (13 files)

Understanding available components — node palette, filesystem node scanning, CodeNode text editing.

| File | Source Module | Current Path |
|------|-------------|-------------|
| CodeEditor.kt | graphEditor | ui/CodeEditor.kt |
| ColorEditor.kt | graphEditor | ui/ColorEditor.kt |
| ComposableDiscovery.kt | graphEditor | ui/ComposableDiscovery.kt |
| DynamicPreviewDiscovery.kt | graphEditor | ui/DynamicPreviewDiscovery.kt |
| IPPalette.kt | graphEditor | ui/IPPalette.kt |
| NodePalette.kt | graphEditor | ui/NodePalette.kt |
| SyntaxHighlighter.kt | graphEditor | ui/SyntaxHighlighter.kt |
| CodeEditorViewModel.kt | graphEditor | viewmodel/CodeEditorViewModel.kt |
| GraphNodePaletteViewModel.kt | graphEditor | viewmodel/GraphNodePaletteViewModel.kt |
| IPPaletteViewModel.kt | graphEditor | viewmodel/IPPaletteViewModel.kt |
| NodePaletteViewModel.kt | graphEditor | viewmodel/NodePaletteViewModel.kt |
| PlacementLevel.kt | graphEditor | model/PlacementLevel.kt |
| NodeDefinitionRegistry.kt | graphEditor | io/.../state/NodeDefinitionRegistry.kt |

**Moved to flowGraph-types**: IPTypeDiscovery.kt, IPTypeRegistry.kt, IPProperty.kt, IPPropertyMeta.kt, IPTypeFileMeta.kt, IPTypeMigration.kt

**Outbound dependencies**: types (IPPaletteVM uses ipTypeMetadata)
**Inbound consumers**: compose (nodeDescriptors), execute (nodeDescriptors), generate (nodeDescriptors), root (nodeDescriptors). Inspect provides node discovery; IP type concerns are now in types.

### graphEditor — composition root (22 files, stays)

Compose UI composables, orchestration ViewModels, DI wiring. No business logic.

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

**Plus 5 UI-only composables** (already in io/.../ui/):

| File | Source Module | Current Path |
|------|-------------|-------------|
| GraphNodeRenderer.kt | graphEditor | io/.../ui/GraphNodeRenderer.kt |
| GroupContextMenu.kt | graphEditor | io/.../ui/GroupContextMenu.kt |
| NavigationBreadcrumb.kt | graphEditor | io/.../ui/NavigationBreadcrumb.kt |
| NavigationZoomOutButton.kt | graphEditor | io/.../ui/NavigationZoomOutButton.kt |
| SelectionBox.kt | graphEditor | io/.../ui/SelectionBox.kt |

**Total root: 27 files**

### File Count Validation

| Target Module | Files | Source Breakdown |
|--------------|-------|-----------------|
| flowGraph-types | 9 | graphEditor: 9 |
| flowGraph-compose | 10 | graphEditor: 10 |
| flowGraph-persist | 8 | graphEditor: 8 |
| flowGraph-execute | 7 | circuitSimulator: 5, graphEditor: 2 |
| flowGraph-generate | 46 | kotlinCompiler: 38, graphEditor: 8 |
| flowGraph-inspect | 13 | graphEditor: 13 |
| graphEditor (root) | 27 | graphEditor: 27 |
| **Total** | **120** | graphEditor: 77, kotlinCompiler: 38, circuitSimulator: 5 |

**Note on audit summary discrepancies**: The original graphEditor/ARCHITECTURE.md summary listed bucket counts (root: 28, inspect: 17, persist: 13, compose: 9, generate: 7, execute: 3) that differ slightly from the file-by-file tally above. The per-file audit tables are authoritative; the summary was an approximation. This migration map uses the file-by-file assignments.

---

## Public APIs

Each target module exposes a public API through Kotlin interfaces. These interfaces replace the direct function calls currently crossing module boundaries (documented in the seam matrices).

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

### Step 2: flowGraph-persist (8 files)

**Why second**: Serialization is self-contained — clear inputs (FlowGraph) and outputs (.flow.kt text). Depends only on types (for IP type metadata during serialization) which is already extracted. No runtime or UI state management.

**Files that move**: 8 files from graphEditor (serialization/, 1 model file, state/ViewSynchronizer, ui/TextualView, 2 io/.../state/ template files)

**Interfaces created**:
- `FlowGraphPersistenceService` (serialize/deserialize)
- `GraphNodeTemplateService` (template CRUD)

**Call sites that change to delegation**:
- `GraphEditorViewModel.kt` → calls `FlowGraphPersistenceService.deserialize()` instead of `FlowKtParser` directly
- `ModuleSaveService.kt` → calls `FlowGraphPersistenceService.serialize()` instead of `FlowGraphSerializer` directly
- `GraphNodePaletteSection.kt` → references `GraphNodeTemplateMeta` via persist API types

**Characterization tests that must pass**:
- `SerializationRoundTripCharacterizationTest` (graphEditor)
- `FlowKtGeneratorCharacterizationTest` (kotlinCompiler — FlowKtGenerator also produces .flow.kt but lives in generate, not persist)
- All existing tests: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest`

### Step 3: flowGraph-inspect (13 files)

**Why third**: Node discovery logic is read-only with respect to graph state. No longer depends on persist or generate (those dependencies moved to types). Inspect provides `NodeRegistryService` consumed by compose, execute, and generate.

**Files that move**: 13 files from graphEditor (7 ui/, 3 viewmodel/, 1 model/, 1 io/.../state/)

**Interfaces created**:
- `NodeRegistryService` (node discovery — consumed by compose, execute, generate, root)

**Call sites that change to delegation**:
- `DragAndDropHandler.kt` → uses `NodeRegistryService` for node definitions
- `LevelCompatibilityChecker.kt` → uses `NodeRegistryService`
- `ModuleSessionFactory.kt` → uses `NodeRegistryService`
- `NodeGeneratorViewModel.kt` → uses `NodeRegistryService`

**Characterization tests that must pass**:
- `ViewModelCharacterizationTest` (graphEditor — palette and registry state)
- All existing tests across all modules

### Step 4: flowGraph-execute (7 files)

**Why fourth**: Runtime pipeline has clear boundaries (FlowGraph in, execution state out). Depends on inspect (ModuleSessionFactory→NodeDefinitionRegistry) which is already extracted. The animation state integration with UI requires the `ConnectionAnimationProvider` interface.

**Files that move**: 5 files from circuitSimulator (entire module) + 2 files from graphEditor (ModuleSessionFactory, RuntimePreviewPanel)

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

### Step 5: flowGraph-generate (46 files)

**Why fifth**: Code generation depends on types (IP type metadata), persist (serialized output), and inspect (node definitions). All three are already extracted and stable. This is the largest extraction (46 files) but the simplest structurally — kotlinCompiler moves wholesale, plus 8 graphEditor generate-bucket files.

**Files that move**: 38 files from kotlinCompiler (entire module) + 8 files from graphEditor (2 ui/, 2 viewmodel/, 3 compilation/, 1 save/)

**Interfaces created**:
- `CodeGenerationService` (module generation)

**Call sites that change to delegation**:
- Root orchestration → uses `CodeGenerationService` instead of calling generators directly

**Characterization tests that must pass**:
- `CodeGenerationCharacterizationTest` (kotlinCompiler)
- `FlowKtGeneratorCharacterizationTest` (kotlinCompiler)
- All existing tests across all modules

### Step 6: flowGraph-compose (10 files)

**Why last**: Graph composition is the most tightly coupled to the UI — GraphState alone has 8 inbound seams from root. All other slices are stable, so compose extraction only needs to create interfaces consumed by root (the composition shell).

**Files that move**: 10 files from graphEditor (3 state/, 3 ui/, 2 viewmodel/, 2 io/.../state/)

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
        graphEditor = composition root only (27 files)
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

### Per-step template

For each extraction step N:

1. **Create module**: Add `flowGraph-{name}/build.gradle.kts` and update `settings.gradle.kts`
2. **Define interfaces**: Create Kotlin interfaces in the new module's public API package
3. **Copy implementation**: Copy files listed in the Module Boundaries section to the new module
4. **Add dependency**: `graphEditor/build.gradle.kts` adds `implementation(project(":flowGraph-{name}"))`
5. **Delegate**: Change call sites in graphEditor to use the interface (backed by the new module's implementation)
6. **Run tests**: `./gradlew :graphEditor:jvmTest :flowGraph-{name}:jvmTest` — all characterization tests must pass
7. **Delete duplicates**: Remove the copied files from graphEditor
8. **Run tests again**: Full test suite must remain green
9. **Write new unit tests**: TDD tests for the new module's API contract (these replace characterization tests over time)

### Post-extraction state

After all six extractions:
- `graphEditor/` contains only 27 files: Compose composables, orchestration ViewModels, renderers, DI wiring, Main.kt
- `flowGraph-types/` contains 9 files with `IPTypeRegistryService`, `IPTypeGenerationService`, `IPTypeRepositoryService` interfaces
- `flowGraph-compose/` contains 10 files with `GraphCompositionService` and `UndoRedoService` interfaces
- `flowGraph-persist/` contains 8 files with `FlowGraphPersistenceService` and `GraphNodeTemplateService` interfaces
- `flowGraph-execute/` contains 7 files with `RuntimeExecutionService`, `ConnectionAnimationProvider`, `DebugSnapshotProvider` interfaces
- `flowGraph-generate/` contains 46 files with `CodeGenerationService` interface
- `flowGraph-inspect/` contains 13 files with `NodeRegistryService` interface
- `kotlinCompiler/` module is **removed** (absorbed into flowGraph-generate)
- `circuitSimulator/` module is **removed** (absorbed into flowGraph-execute)
- All modules depend on `fbpDsl` for shared vocabulary (FlowGraph, Node, Port, Connection, etc.)
- The module dependency graph is a **DAG** — no circular dependencies
