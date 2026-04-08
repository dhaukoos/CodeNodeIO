# Feature Specification: flowGraph-inspect Module Extraction

**Feature Branch**: `067-extract-flowgraph-inspect`
**Created**: 2026-04-08
**Status**: Draft
**Input**: Step 3 of vertical-slice decomposition — extract 7 node discovery/inspection logic files from graphEditor into a new flowGraph-inspect module, wrap as a coarse-grained CodeNode with FBP-native data flow ports, and wire into architecture.flow.kt. Compose UI files (CodeEditor, ColorEditor, IPPalette, NodePalette, SyntaxHighlighter) remain in graphEditor as presentation concerns.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Extract Inspection Logic into Standalone Module (Priority: P1)

The 7 non-UI files responsible for node discovery, node definition registry, palette state management, filesystem scanning, and preview discovery are extracted from graphEditor into a new flowGraph-inspect module. Compose UI composables (CodeEditor, ColorEditor, IPPalette, NodePalette, SyntaxHighlighter) remain in graphEditor as presentation concerns — matching the principle established in feature 066 where ViewSynchronizer and TextualView stayed in graphEditor. The module depends only on fbpDsl, flowGraph-types, and flowGraph-persist — no dependency on graphEditor.

The 7 files to extract:
1. NodeDefinitionRegistry.kt — central node discovery and registration (341 lines, fbpDsl + ServiceLoader)
2. CodeEditorViewModel.kt — file I/O and editor state (193 lines, depends on NodeDefinitionRegistry)
3. IPPaletteViewModel.kt — IP type palette state (160 lines, fbpDsl + flowGraph-types)
4. GraphNodePaletteViewModel.kt — GraphNode palette section state (59 lines, flowGraph-persist)
5. NodePaletteViewModel.kt — node palette state (104 lines, fbpDsl only)
6. ComposableDiscovery.kt — filesystem scanning for composables (32 lines, java.io.File only)
7. DynamicPreviewDiscovery.kt — reflection-based preview discovery (56 lines, java.io.File + reflection)

**Why this priority**: The module boundary must exist and compile before anything else can reference it. This is the foundational extraction step following the Strangler Fig pattern (copy files, keep originals, switch consumers, remove originals).

**Independent Test**: `./gradlew :flowGraph-inspect:compileKotlinJvm` succeeds and the copied files resolve all imports from fbpDsl, flowGraph-types, and flowGraph-persist. No graphEditor dependency exists in the module's build configuration.

**Acceptance Scenarios**:

1. **Given** the 7 inspection logic files live in graphEditor, **When** they are copied to flowGraph-inspect with updated package declarations, **Then** `./gradlew :flowGraph-inspect:compileKotlinJvm` succeeds with zero errors.
2. **Given** the flowGraph-inspect module exists, **When** its build configuration is inspected, **Then** it depends only on `:fbpDsl`, `:flowGraph-types`, and `:flowGraph-persist` — no `:graphEditor` dependency.
3. **Given** the 7 files are copied to the new module, **When** the original files still exist in graphEditor, **Then** both modules compile independently (Strangler Fig coexistence).
4. **Given** Compose UI files (CodeEditor, ColorEditor, IPPalette, NodePalette, SyntaxHighlighter) exist in graphEditor, **When** the extraction is complete, **Then** they remain in graphEditor unchanged.

---

### User Story 2 - Wrap Module as FlowGraphInspect CodeNode (Priority: P2)

A coarse-grained CodeNode wraps the entire inspect module behind typed input/output ports. The module boundary is expressed as FBP-native data flow — not service interfaces. Data flows in as filesystemPaths (directories to scan for node definitions) and classpathEntries (classpath locations for compiled node discovery). Data flows out as nodeDescriptors (discovered node metadata for palette population).

**Why this priority**: The CodeNode is the module's external contract. Without it, there is no FBP-native boundary — consumers would need direct class imports, defeating the vertical-slice architecture.

**Independent Test**: TDD tests verify port signatures (2 inputs, 1 output, all String type), runtime type, and data flow through channels — input on filesystemPaths or classpathEntries port triggers node discovery and emits nodeDescriptors output. Run `./gradlew :flowGraph-inspect:jvmTest` to confirm all CodeNode tests pass.

**Acceptance Scenarios**:

1. **Given** FlowGraphInspectCodeNode exists, **When** its port metadata is inspected, **Then** it has 2 input ports (filesystemPaths, classpathEntries) and 1 output port (nodeDescriptors).
2. **Given** the CodeNode receives filesystem path data on its input port, **When** the processing logic runs, **Then** it emits discovered node descriptor metadata on the nodeDescriptors port.
3. **Given** the CodeNode receives classpath entry data, **When** the processing logic scans for CodeNode definitions, **Then** it emits the combined set of discovered nodes on the nodeDescriptors port.
4. **Given** either input port receives data independently, **When** the CodeNode processes the input, **Then** it uses the cached value from the other input to produce a complete scan result (anyInput mode).

---

### User Story 3 - Migrate Call Sites to New Module (Priority: P3)

All graphEditor files that currently import inspection-related classes directly (NodeDefinitionRegistry, palette ViewModels, discovery utilities) switch their imports to the flowGraph-inspect module packages. Compose UI composables that take ViewModels as parameters (CodeEditor, IPPalette, NodePalette) continue to reference the ViewModels but via the new module's packages. The original files in graphEditor become dead code.

**Why this priority**: Consumers must switch to the new module before originals can be removed. This is the Strangler Fig "redirect" step.

**Independent Test**: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` — all existing tests pass. No graphEditor source file imports from the old inspection packages (only the dead original files themselves still exist).

**Acceptance Scenarios**:

1. **Given** all call sites have been updated, **When** a search is performed for old package imports in graphEditor (excluding the original dead files), **Then** zero matches are found.
2. **Given** all call sites use the new module's packages, **When** the full test suite runs, **Then** all tests pass with zero regressions.
3. **Given** Main.kt, CodeEditor.kt, IPPalette.kt, NodePalette.kt, RuntimePreviewPanel.kt, ModuleSessionFactory.kt, NodeGeneratorViewModel.kt, and LevelCompatibilityChecker.kt reference inspection types, **When** their imports are updated, **Then** they compile against the new module packages.

---

### User Story 4 - Remove Original Files from graphEditor (Priority: P4)

The 7 original inspection logic files are deleted from graphEditor. Any remaining same-package references are resolved with explicit imports to the new module. The graphEditor module no longer contains any node discovery or palette state management code — only the Compose UI composables that render the palette.

**Why this priority**: Completes the Strangler Fig extraction. Must happen after all consumers are redirected (US3).

**Independent Test**: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` — all tests pass after deletion. The 7 files no longer exist in graphEditor.

**Acceptance Scenarios**:

1. **Given** all consumers use the new module, **When** the 7 original files are deleted, **Then** `./gradlew :graphEditor:compileKotlinJvm` succeeds.
2. **Given** test files in the same packages used same-package access, **When** the originals are removed, **Then** explicit imports are added to resolve any compilation errors.

---

### User Story 5 - Wire into architecture.flow.kt (Priority: P5)

The flowGraph-inspect GraphNode in architecture.flow.kt is populated with the FlowGraphInspect CodeNode as a child node, with port mappings wiring exposed ports to child ports. The architecture test is updated to reflect any connection or port changes.

**Why this priority**: Makes the architecture diagram reflect the actual CodeNode implementation. Completes the vertical-slice wiring.

**Independent Test**: `./gradlew :graphEditor:jvmTest --tests "characterization.ArchitectureFlowKtsTest"` — all architecture tests pass.

**Acceptance Scenarios**:

1. **Given** the flowGraph-inspect GraphNode exists in architecture.flow.kt, **When** it is opened, **Then** a FlowGraphInspect child codeNode is visible with 2 inputs and 1 output.
2. **Given** port mappings exist, **When** each exposed port is inspected, **Then** it maps to the corresponding child CodeNode port.
3. **Given** the architecture is updated, **When** the architecture test runs, **Then** all assertions pass including connection counts and node structure.

---

### User Story 6 - Verify Extraction Integrity (Priority: P6)

The dependency direction is verified (graphEditor → flowGraph-inspect → flowGraph-types → fbpDsl), no circular dependencies exist, the Strangler Fig commit sequence is preserved in git history, and the full test suite passes across all modules.

**Why this priority**: Final validation gate — ensures the extraction didn't introduce architectural violations.

**Independent Test**: `./gradlew :flowGraph-inspect:dependencies` shows only fbpDsl, flowGraph-types, and flowGraph-persist. Full test suite passes across all modules.

**Acceptance Scenarios**:

1. **Given** the extraction is complete, **When** flowGraph-inspect's dependency tree is inspected, **Then** only `:fbpDsl`, `:flowGraph-types`, and `:flowGraph-persist` appear as project dependencies.
2. **Given** the git history, **When** commits are listed in order, **Then** the Strangler Fig sequence is visible: module creation → file copy → TDD tests → CodeNode implementation → call site migration → original removal → architecture wiring.
3. **Given** all modules, **When** the full test suite runs, **Then** zero test regressions across all modules.

---

### Edge Cases

- What happens when filesystem paths point to directories that don't exist? The existing scanning logic handles missing directories gracefully — no nodes are discovered, no error is thrown.
- What happens when no CodeNode definitions are found on the classpath? An empty node descriptor list is emitted on the output port.
- What happens when both inputs arrive simultaneously? The anyInput mode processes whichever arrives first, using the cached value from the other input. No data loss occurs.
- What happens when node descriptor output is empty? The downstream palette receives an empty list and displays no custom nodes — this is valid behavior.
- What happens when a ViewModel's state class extends BaseState (a marker interface in graphEditor)? The extracted ViewModels' state classes either import BaseState from graphEditor or drop the marker — BaseState stays in graphEditor since non-extracted ViewModels also use it.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST extract exactly 7 non-UI files from graphEditor into a new flowGraph-inspect module: NodeDefinitionRegistry, CodeEditorViewModel, IPPaletteViewModel, GraphNodePaletteViewModel, NodePaletteViewModel, ComposableDiscovery, and DynamicPreviewDiscovery.
- **FR-002**: Compose UI composables (CodeEditor, ColorEditor, IPPalette, NodePalette, SyntaxHighlighter) MUST remain in graphEditor — they are presentation concerns, not inspection logic.
- **FR-003**: The flowGraph-inspect module MUST depend only on fbpDsl, flowGraph-types, and flowGraph-persist — no dependency on graphEditor or any other application module.
- **FR-004**: The FlowGraphInspectCodeNode MUST expose 2 input ports (filesystemPaths, classpathEntries) and 1 output port (nodeDescriptors), all of type String — matching the port signature already defined in architecture.flow.kt.
- **FR-005**: The module boundary MUST be expressed as FBP-native data flow through CodeNode ports — no Koin-wired service interfaces.
- **FR-006**: The CodeNode MUST use data-oriented port naming (filesystemPaths, classpathEntries, nodeDescriptors) — not service-oriented naming.
- **FR-007**: The CodeNode MUST follow the TDD pattern — tests are written and committed before implementation, verified to fail, then implementation makes them pass.
- **FR-008**: The extraction MUST follow the Strangler Fig pattern — copy files first, keep originals, switch consumers, then remove originals — with each step as a separate git commit.
- **FR-009**: The ViewModelCharacterizationTest (palette and registry state) MUST continue to pass throughout the extraction.
- **FR-010**: All existing tests across graphEditor, kotlinCompiler, circuitSimulator, flowGraph-types, and flowGraph-persist MUST pass after extraction.
- **FR-011**: The architecture.flow.kt GraphNode for flowGraph-inspect MUST contain the FlowGraphInspect child codeNode with port mappings for all exposed ports.
- **FR-012**: The CodeNode MUST use anyInput mode — either input port receiving data independently triggers processing using the cached value from the other input.

### Key Entities

- **FlowGraphInspectCodeNode**: The coarse-grained CodeNode wrapping all node discovery and inspection functionality. Receives filesystem paths and classpath entries as inputs, produces node descriptor metadata as output.
- **Node Descriptors**: Metadata describing available nodes for the palette — name, type, port signatures, category, source location. Flows out of the CodeNode as serialized String data.
- **Filesystem Paths**: Directory locations to scan for CodeNode definition source files — flows into the CodeNode as String data.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The flowGraph-inspect module compiles independently with zero errors and zero dependencies on graphEditor.
- **SC-002**: 100% of existing tests pass across all 6 modules (graphEditor, kotlinCompiler, circuitSimulator, flowGraph-types, flowGraph-persist, flowGraph-inspect) after extraction.
- **SC-003**: Zero circular dependencies exist in the module graph — verified by dependency inspection.
- **SC-004**: The Strangler Fig commit sequence contains at least 6 distinct commits matching the pattern: setup → copy → TDD tests → implementation → migration → removal.
- **SC-005**: The architecture.flow.kt accurately reflects the live FlowGraphInspect CodeNode with correct port counts and mappings.
- **SC-006**: No service-oriented interfaces (Koin modules, DI bindings) are introduced — the module boundary is purely FBP data flow through CodeNode ports.
- **SC-007**: All 5 Compose UI files (CodeEditor, ColorEditor, IPPalette, NodePalette, SyntaxHighlighter) remain in graphEditor unchanged after extraction.

## Assumptions

- The 7 non-UI files identified for extraction have been verified via dependency analysis to have zero circular dependency risk with graphEditor. All graphEditor-internal dependencies are within the extraction set or point to already-extracted modules (fbpDsl, flowGraph-types, flowGraph-persist).
- The flowGraph-inspect module follows the same KMP structure as flowGraph-types and flowGraph-persist (commonMain + jvmMain source sets).
- The FlowGraphInspectCodeNode will use a processor runtime with anyInput mode matching its port signature (2 inputs, 1 output).
- Node descriptor output is serialized as String data — the downstream consumer (compose, execute, generate modules) deserializes it.
- The inspect module is read-only with respect to graph state — it discovers and describes available nodes but does not modify the flow graph.
- GraphNodePaletteViewModel depends on flowGraph-persist (GraphNodeTemplateMeta), so flowGraph-inspect will have a dependency on flowGraph-persist in addition to fbpDsl and flowGraph-types.
- BaseState marker interface stays in graphEditor — extracted ViewModels will handle this dependency during the planning phase.
- PlacementLevel.kt already lives in fbpDsl and does not need extraction.
- MIGRATION.md's original 13-file count for inspect included 5 Compose UI files and 1 file already in fbpDsl. The corrected count is 7 logic/state files, following the "Compose stays in graphEditor" principle from feature 066.

## Scope Boundaries

### In Scope

- Extracting the 7 non-UI inspection logic files to a new module
- Creating FlowGraphInspectCodeNode with FBP-native port boundary
- TDD tests for the CodeNode
- Migrating all call sites in graphEditor
- Removing original files
- Updating architecture.flow.kt with child node and port mappings
- Verifying dependency direction and test suite integrity

### Out of Scope

- Moving Compose UI composables (CodeEditor, ColorEditor, IPPalette, NodePalette, SyntaxHighlighter) — they stay in graphEditor
- Modifying node discovery behavior or scanning logic
- Adding new node palette features
- Extracting other modules (compose, execute, generate)
- Runtime integration or Koin DI wiring
- Changing the node descriptor data format
- Updating MIGRATION.md (can be done as a follow-up)
