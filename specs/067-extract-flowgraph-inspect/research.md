# Research: flowGraph-inspect Module Extraction

## R1: Which Files Belong in flowGraph-inspect?

**Decision**: Extract 7 non-UI files, not 13. Exclude 5 Compose UI composables and PlacementLevel.

**Rationale**: The original MIGRATION.md listed 13 files for the inspect bucket. Analysis reveals:
- 5 files are Compose UI composables (`CodeEditor.kt`, `ColorEditor.kt`, `IPPalette.kt`, `NodePalette.kt`, `SyntaxHighlighter.kt`) — they stay in graphEditor per the "Compose stays in graphEditor" principle established in feature 066.
- `PlacementLevel.kt` already lives in `fbpDsl/src/commonMain/kotlin/` — it was incorrectly counted as a graphEditor file.
- The remaining 7 files are pure non-UI logic with zero graphEditor-only dependencies.

**Files to extract (7)**:
1. `io/codenode/grapheditor/state/NodeDefinitionRegistry.kt` (341 lines, ServiceLoader + java.io.File)
2. `viewmodel/CodeEditorViewModel.kt` (193 lines, depends on NodeDefinitionRegistry)
3. `viewmodel/IPPaletteViewModel.kt` (160 lines, depends on flowGraph-types IPTypeRegistry)
4. `viewmodel/GraphNodePaletteViewModel.kt` (59 lines, depends on flowGraph-persist GraphNodeTemplateMeta)
5. `viewmodel/NodePaletteViewModel.kt` (104 lines, fbpDsl only)
6. `ui/ComposableDiscovery.kt` (32 lines, java.io.File only)
7. `ui/DynamicPreviewDiscovery.kt` (56 lines, java.io.File + reflection)

**Internal cross-references**: Only one — CodeEditorViewModel imports NodeDefinitionRegistry. Both are in the extraction set.

**Alternatives considered**:
- Include all 13: Rejected — Compose UI files would pull Compose Desktop dependencies into the module
- Include PlacementLevel: Rejected — already in fbpDsl, not a graphEditor file
- Exclude ComposableDiscovery/DynamicPreviewDiscovery (they're in ui/): Included — despite being in the ui/ directory, these are pure filesystem/reflection logic with no @Composable annotations

## R2: Runtime Type for CodeNode

**Decision**: Use `In2AnyOut1Runtime<String, String, String>` with `anyInput = true`.

**Rationale**: The inspect CodeNode has 2 inputs (filesystemPaths, classpathEntries) and 1 output (nodeDescriptors). Using `anyInput` mode because:
- Either input can arrive independently — a filesystem path scan and a classpath scan are independent operations
- When filesystemPaths arrives, the CodeNode scans for .kt source files containing CodeNode definitions
- When classpathEntries arrives, the CodeNode scans for compiled CodeNode definitions via ServiceLoader
- The combined result (from cached values of both inputs) is emitted as nodeDescriptors
- This matches the port signature already defined in architecture.flow.kt

**Alternatives considered**:
- `In2Out1Runtime` (requires ALL inputs): Rejected — would block node discovery until both inputs arrive, even when one is sufficient
- `SourceRuntime` (no inputs): Rejected — discovery needs filesystem/classpath paths as input data

## R3: KMP Source Set Split

**Decision**: All 7 files in jvmMain, no commonMain files. CodeNode + tests in jvmMain.

**Rationale**:
- All 7 files use JVM-specific APIs: `java.io.File`, `java.util.ServiceLoader`, `Class.forName()` (reflection)
- Even the ViewModels use `androidx.lifecycle.ViewModel` which is available on JVM
- No file is pure Kotlin without JVM dependencies
- commonMain source set will exist (for KMP structure) but remain empty initially

**Alternatives considered**:
- Put ViewModels in commonMain: Not possible — they depend on NodeDefinitionRegistry which uses java.io.File
- Split ViewModels from registry: Unnecessary complexity — all files are JVM-only

## R4: BaseState Marker Interface

**Decision**: Extracted ViewModels' state classes drop the `BaseState` marker. BaseState stays in graphEditor.

**Rationale**: BaseState is a marker interface (`interface BaseState` — no methods, no properties) used by 10 ViewModels across graphEditor, including ones that stay in root (GraphEditorViewModel), go to compose (CanvasInteractionViewModel, PropertiesPanelViewModel), and go to generate (NodeGeneratorViewModel, IPGeneratorViewModel). Moving BaseState to flowGraph-inspect would create a reverse dependency. The 4 extracted state classes (CodeEditorState, IPPaletteState, GraphNodePaletteState, NodePaletteState) simply drop `: BaseState` — this has zero behavioral impact since the interface has no members.

**Alternatives considered**:
- Move BaseState to a shared module: Over-engineering for a marker interface with no members
- Copy BaseState to flowGraph-inspect: Duplication without benefit
- Import BaseState from graphEditor: Creates circular dependency (inspect → graphEditor)

## R5: Dependencies

**Decision**: flowGraph-inspect depends on `:fbpDsl`, `:flowGraph-types`, and `:flowGraph-persist`.

**Rationale**:
- `:fbpDsl` — CodeNodeType, NodeTypeDefinition, CodeNodeFactory, CodeNodeDefinition, runtime types, PlacementLevel
- `:flowGraph-types` — IPTypeRegistry (used by IPPaletteViewModel), FileIPTypeRepository (used by IPPaletteViewModel)
- `:flowGraph-persist` — GraphNodeTemplateMeta (used by GraphNodePaletteViewModel)
- No dependency on `:graphEditor` — consumers in graphEditor depend on flowGraph-inspect, not the reverse

**Dependency direction**: graphEditor → flowGraph-inspect → {flowGraph-types, flowGraph-persist} → fbpDsl

## R6: Data-Oriented Port Naming

**Decision**: Use data-oriented names matching architecture.flow.kt: filesystemPaths, classpathEntries, nodeDescriptors.

**Rationale**: Feature 064 R6 established data-oriented naming over service-oriented naming. These names describe the data shape flowing through ports:
- `filesystemPaths` — directory locations to scan for CodeNode definition source files
- `classpathEntries` — classpath locations for compiled CodeNode discovery via ServiceLoader
- `nodeDescriptors` — discovered node metadata (name, type, ports, category, source location)

Already defined in architecture.flow.kt.

## R7: Call Site Migration Scope

**Decision**: 8 graphEditor files need import updates, plus 2 existing test files that should migrate.

**Call sites that import from the 7 files**:
1. `Main.kt` — imports NodeDefinitionRegistry, CodeEditorViewModel, IPPaletteViewModel, NodePaletteViewModel, DynamicPreviewDiscovery
2. `ui/CodeEditor.kt` — imports CodeEditorViewModel (Compose UI, stays in graphEditor)
3. `ui/IPPalette.kt` — imports IPPaletteViewModel (Compose UI, stays in graphEditor)
4. `ui/NodePalette.kt` — imports NodePaletteViewModel (implied by usage)
5. `ui/RuntimePreviewPanel.kt` — imports discoverComposables from ComposableDiscovery
6. `viewmodel/NodeGeneratorViewModel.kt` — imports NodeDefinitionRegistry, NodeTemplateMeta
7. `ui/ModuleSessionFactory.kt` — imports NodeDefinitionRegistry
8. `io/.../state/LevelCompatibilityChecker.kt` — imports NodeDefinitionRegistry (if applicable)

**Test files to migrate**:
- `graphEditor/src/jvmTest/kotlin/viewmodel/IPPaletteViewModelTest.kt` → move to flowGraph-inspect
- `graphEditor/src/jvmTest/kotlin/viewmodel/NodePaletteViewModelTest.kt` → move to flowGraph-inspect

**Test file that needs import updates** (stays in graphEditor):
- `graphEditor/src/jvmTest/kotlin/characterization/ViewModelCharacterizationTest.kt` — references NodePaletteViewModel

## R8: Package Structure

**Decision**: Use `io.codenode.flowgraphinspect` as root package with sub-packages: `registry`, `viewmodel`, `discovery`, `node`.

**Rationale**: Follows the pattern from flowGraph-persist (`io.codenode.flowgraphpersist`) with semantic sub-packages:
- `registry/` — NodeDefinitionRegistry (+ NodeTemplateMeta inner data class)
- `viewmodel/` — CodeEditorViewModel, IPPaletteViewModel, GraphNodePaletteViewModel, NodePaletteViewModel
- `discovery/` — ComposableDiscovery, DynamicPreviewDiscovery
- `node/` — FlowGraphInspectCodeNode (+ test)

**Note on NodeTemplateMeta**: This is a `data class` defined inside NodeDefinitionRegistry.kt. It stays with NodeDefinitionRegistry — no separate file needed.
