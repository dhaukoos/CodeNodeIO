# Vertical Slice Migration Map

**Feature**: 064-vertical-slice-refactor
**Date**: 2026-04-02

## Cross-Module Seam Summary

Three source modules are being decomposed into five vertical-slice target modules. This section documents the cross-module seams — dependencies that cross the boundaries between graphEditor, kotlinCompiler, and circuitSimulator.

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

### flowGraph-persist (10 files)

Round-trip workflow between in-memory FlowGraph and `.flow.kts` files on disk.

| File | Source Module | Current Path |
|------|-------------|-------------|
| FlowGraphSerializer.kt | graphEditor | serialization/FlowGraphSerializer.kt |
| FlowKtParser.kt | graphEditor | serialization/FlowKtParser.kt |
| GraphNodeTemplateSerializer.kt | graphEditor | serialization/GraphNodeTemplateSerializer.kt |
| GraphNodeTemplateMeta.kt | graphEditor | model/GraphNodeTemplateMeta.kt |
| SerializableIPType.kt | graphEditor | model/SerializableIPType.kt |
| FileIPTypeRepository.kt | graphEditor | repository/FileIPTypeRepository.kt |
| ViewSynchronizer.kt | graphEditor | state/ViewSynchronizer.kt |
| TextualView.kt | graphEditor | ui/TextualView.kt |
| GraphNodeTemplateInstantiator.kt | graphEditor | io/.../state/GraphNodeTemplateInstantiator.kt |
| GraphNodeTemplateRegistry.kt | graphEditor | io/.../state/GraphNodeTemplateRegistry.kt |

**Outbound dependencies**: compose (ViewSynchronizer→GraphState), inspect (GraphNodeTemplateSerializer→IPTypeRegistry, TextualView→SyntaxHighlighter)
**Inbound consumers**: root (2 seams), compose (0), generate (1 — ModuleSaveService→FlowGraphSerializer), inspect (2 — IPPaletteVM, IPTypeMigration→FileIPTypeRepository)

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

### flowGraph-generate (47 files from kotlinCompiler + 9 from graphEditor = 56 files)

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

**From graphEditor (generate bucket — 9 files)**:

| File | Source Module | Current Path |
|------|-------------|-------------|
| IPGeneratorPanel.kt | graphEditor | ui/IPGeneratorPanel.kt |
| NodeGeneratorPanel.kt | graphEditor | ui/NodeGeneratorPanel.kt |
| IPGeneratorViewModel.kt | graphEditor | viewmodel/IPGeneratorViewModel.kt |
| NodeGeneratorViewModel.kt | graphEditor | viewmodel/NodeGeneratorViewModel.kt |
| IPTypeFileGenerator.kt | graphEditor | state/IPTypeFileGenerator.kt |
| CompilationService.kt | graphEditor | compilation/CompilationService.kt |
| CompilationValidator.kt | graphEditor | compilation/CompilationValidator.kt |
| RequiredPropertyValidator.kt | graphEditor | compilation/RequiredPropertyValidator.kt |
| ModuleSaveService.kt | graphEditor | save/ModuleSaveService.kt |

**Outbound dependencies**: inspect (IPGeneratorVM→IPTypeRegistry/IPTypeDiscovery, NodeGeneratorVM→NodeDefinitionRegistry), persist (ModuleSaveService→FlowGraphSerializer)
**Inbound consumers**: root (0 direct — generation is triggered from root ViewModels), inspect (1 — IPTypeMigration→IPTypeFileGenerator)

### flowGraph-inspect (19 files)

Understanding available components — discovery, registry, palette, and text editing.

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
| IPTypeDiscovery.kt | graphEditor | state/IPTypeDiscovery.kt |
| IPTypeRegistry.kt | graphEditor | state/IPTypeRegistry.kt |
| IPProperty.kt | graphEditor | model/IPProperty.kt |
| IPPropertyMeta.kt | graphEditor | model/IPPropertyMeta.kt |
| IPTypeFileMeta.kt | graphEditor | model/IPTypeFileMeta.kt |
| PlacementLevel.kt | graphEditor | model/PlacementLevel.kt |
| NodeDefinitionRegistry.kt | graphEditor | io/.../state/NodeDefinitionRegistry.kt |
| IPTypeMigration.kt | graphEditor | repository/IPTypeMigration.kt |

**Outbound dependencies**: persist (IPPaletteVM→FileIPTypeRepository, IPTypeMigration→FileIPTypeRepository), generate (IPTypeMigration→IPTypeFileGenerator)
**Inbound consumers**: compose (4 seams), generate (3 seams), persist (2 seams), execute (1 seam), root (3 seams). **Inspect is the most depended-upon slice.**

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
| flowGraph-compose | 10 | graphEditor: 10 |
| flowGraph-persist | 10 | graphEditor: 10 |
| flowGraph-execute | 7 | circuitSimulator: 5, graphEditor: 2 |
| flowGraph-generate | 47 | kotlinCompiler: 38, graphEditor: 9 |
| flowGraph-inspect | 19 | graphEditor: 19 |
| graphEditor (root) | 27 | graphEditor: 27 |
| **Total** | **120** | graphEditor: 77, kotlinCompiler: 38, circuitSimulator: 5 |

**Note on audit summary discrepancies**: The original graphEditor/ARCHITECTURE.md summary listed bucket counts (root: 28, inspect: 17, persist: 13, compose: 9, generate: 7, execute: 3) that differ slightly from the file-by-file tally above. The per-file audit tables are authoritative; the summary was an approximation. This migration map uses the file-by-file assignments.

---

## Public APIs

Each target module exposes a public API through Kotlin interfaces. These interfaces replace the direct function calls currently crossing module boundaries (documented in the seam matrices).

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

**Interface: `IPTypeGenerationService`**
Exposes IP type file generation from `IPTypeFileGenerator.kt`.

```kotlin
interface IPTypeGenerationService {
    fun generateIPTypeFile(name: String, properties: List<IPProperty>, level: PlacementLevel): String
}
```

**Current call sites**:
- `IPTypeMigration.kt` → IPTypeFileGenerator (inspect→generate)

### flowGraph-inspect API

**Interface: `NodeRegistryService`**
Exposes node discovery from `NodeDefinitionRegistry.kt`. This is the **most consumed interface** across all slices.

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

**Interface: `IPTypeRegistryService`**
Exposes IP type lookup from `IPTypeRegistry.kt`.

```kotlin
interface IPTypeRegistryService {
    val registeredTypes: StateFlow<List<InformationPacketType>>
    fun getType(typeId: String): InformationPacketType?
    fun registerType(type: InformationPacketType)
    fun search(query: String): List<InformationPacketType>
}
```

**Current call sites** (consumed by 3 other slices):
- `GraphState.kt`, `ConnectionContextMenu.kt` (compose→inspect)
- `GraphNodeTemplateSerializer.kt` (persist→inspect)
- `SharedStateProvider.kt`, `PropertiesPanel.kt`, `GraphNodePaletteSection.kt` (root→inspect)
- `IPGeneratorViewModel.kt` (generate→inspect)

### Interface Summary

| Module | Interfaces Exposed | Consumers |
|--------|-------------------|-----------|
| flowGraph-compose | GraphCompositionService, UndoRedoService | root |
| flowGraph-persist | FlowGraphPersistenceService, GraphNodeTemplateService | root, generate, inspect |
| flowGraph-execute | RuntimeExecutionService, ConnectionAnimationProvider, DebugSnapshotProvider | root |
| flowGraph-generate | CodeGenerationService, IPTypeGenerationService | root, inspect |
| flowGraph-inspect | NodeRegistryService, IPTypeRegistryService | compose, persist, execute, generate, root |

---

## Extraction Order

Per research.md R5: extract in order of decreasing independence and decreasing risk.

### Step 1: flowGraph-persist (10 files)

**Why first**: Serialization is the most self-contained — clear inputs (FlowGraph) and outputs (.flow.kts text). Fewest inbound dependencies from other business logic. No runtime or UI state management.

**Files that move**: 10 files from graphEditor (serialization/, 2 model files, repository/FileIPTypeRepository, state/ViewSynchronizer, ui/TextualView, 2 io/.../state/ template files)

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

### Step 2: flowGraph-inspect (19 files)

**Why second**: Discovery/registry logic is read-only with respect to graph state. Depends on persist for loading definitions (IPPaletteVM→FileIPTypeRepository) but persist is already extracted. Inspect is the most depended-upon slice — extracting it early makes its interfaces available to all later extractions.

**Files that move**: 19 files from graphEditor (7 ui/, 4 viewmodel/, 2 state/, 4 model/, 1 io/.../state/, 1 repository/)

**Interfaces created**:
- `NodeRegistryService` (node discovery — consumed by compose, execute, generate)
- `IPTypeRegistryService` (IP type lookup — consumed by compose, persist, generate, root)

**Call sites that change to delegation**:
- `GraphState.kt` → calls `IPTypeRegistryService` instead of `IPTypeRegistry` directly
- `ConnectionContextMenu.kt` → uses `IPTypeRegistryService` for type lookup
- `DragAndDropHandler.kt` → uses `NodeRegistryService` for node definitions
- `LevelCompatibilityChecker.kt` → uses `NodeRegistryService`
- `ModuleSessionFactory.kt` → uses `NodeRegistryService`
- `SharedStateProvider.kt` → provides `IPTypeRegistryService` instead of `IPTypeRegistry`
- `GraphNodeTemplateSerializer.kt` → uses `IPTypeRegistryService`
- `IPGeneratorViewModel.kt`, `NodeGeneratorViewModel.kt` → use both registry services

**Characterization tests that must pass**:
- `ViewModelCharacterizationTest` (graphEditor — palette and registry state)
- All existing tests across all modules

### Step 3: flowGraph-execute (7 files)

**Why third**: Runtime pipeline has clear boundaries (FlowGraph in, execution state out). Depends on inspect (ModuleSessionFactory→NodeDefinitionRegistry) which is already extracted. The animation state integration with UI requires the `ConnectionAnimationProvider` interface.

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

### Step 4: flowGraph-generate (47 files)

**Why fourth**: Code generation depends on persist (for .flow.kts output) and inspect (for node definitions). Both are already extracted and stable. This is the largest extraction (47 files) but the simplest structurally — kotlinCompiler moves wholesale, plus 9 graphEditor generate-bucket files.

**Files that move**: 38 files from kotlinCompiler (entire module) + 9 files from graphEditor (2 ui/, 2 viewmodel/, 1 state/, 3 compilation/, 1 save/)

**Interfaces created**:
- `CodeGenerationService` (module generation)
- `IPTypeGenerationService` (IP type file generation)

**Call sites that change to delegation**:
- Root orchestration → uses `CodeGenerationService` instead of calling generators directly
- `IPTypeMigration.kt` → uses `IPTypeGenerationService` instead of `IPTypeFileGenerator` directly

**Characterization tests that must pass**:
- `CodeGenerationCharacterizationTest` (kotlinCompiler)
- `FlowKtGeneratorCharacterizationTest` (kotlinCompiler)
- All existing tests across all modules

### Step 5: flowGraph-compose (10 files)

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
Step 1: flowGraph-persist ──────────────────────────────────────────────►
            │
            │ persist interfaces available
            ▼
Step 2: flowGraph-inspect ──────────────────────────────────────────────►
            │
            │ inspect interfaces available (most consumed)
            ▼
Step 3: flowGraph-execute ──────────────────────────────────────────────►
            │
            │ execute interfaces available
            ▼
Step 4: flowGraph-generate ─────────────────────────────────────────────►
            │
            │ generate interfaces available
            ▼
Step 5: flowGraph-compose ──────────────────────────────────────────────►
            │
            │ all slices extracted
            ▼
        graphEditor = composition root only (27 files)
```

### Extraction Order Dependency Validation

| Step | Module | Depends On | Already Extracted? | Circular? |
|------|--------|-----------|-------------------|-----------|
| 1 | persist | fbpDsl (shared vocab) | N/A (not extracted) | No |
| 2 | inspect | persist (IPPaletteVM→FileIPTypeRepository), fbpDsl | Step 1 ✓ | No |
| 3 | execute | inspect (ModuleSessionFactory→NodeDefinitionRegistry), fbpDsl | Step 2 ✓ | No |
| 4 | generate | persist (ModuleSaveService→FlowGraphSerializer), inspect (IPGeneratorVM→registries), fbpDsl | Steps 1-2 ✓ | No |
| 5 | compose | inspect (GraphState→IPTypeRegistry, DragAndDropHandler→NodeDefinitionRegistry), fbpDsl | Step 2 ✓ | No |

**No circular dependencies**: Every step depends only on modules extracted in earlier steps (or fbpDsl which is never extracted — it's shared vocabulary).

**Note on inspect→generate**: `IPTypeMigration.kt` (inspect) depends on `IPTypeFileGenerator.kt` (generate). This is a forward dependency — inspect is extracted in Step 2 but generate isn't extracted until Step 4. During Step 2, this call goes through the generate interface which still lives in graphEditor. When generate is extracted in Step 4, the call becomes cross-module via `IPTypeGenerationService`. This is safe because:
1. During Step 2: IPTypeMigration calls IPTypeFileGenerator in graphEditor (same module, unchanged)
2. During Step 4: IPTypeMigration calls IPTypeGenerationService interface (cross-module, clean)

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

After all five extractions:
- `graphEditor/` contains only 27 files: Compose composables, orchestration ViewModels, renderers, DI wiring, Main.kt
- `flowGraph-compose/` contains 10 files with `GraphCompositionService` and `UndoRedoService` interfaces
- `flowGraph-persist/` contains 10 files with `FlowGraphPersistenceService` and `GraphNodeTemplateService` interfaces
- `flowGraph-execute/` contains 7 files with `RuntimeExecutionService`, `ConnectionAnimationProvider`, `DebugSnapshotProvider` interfaces
- `flowGraph-generate/` contains 47 files with `CodeGenerationService` and `IPTypeGenerationService` interfaces
- `flowGraph-inspect/` contains 19 files with `NodeRegistryService` and `IPTypeRegistryService` interfaces
- `kotlinCompiler/` module is **removed** (absorbed into flowGraph-generate)
- `circuitSimulator/` module is **removed** (absorbed into flowGraph-execute)
- All modules depend on `fbpDsl` for shared vocabulary (FlowGraph, Node, Port, Connection, etc.)
