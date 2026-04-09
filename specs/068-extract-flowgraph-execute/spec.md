# Feature Specification: flowGraph-execute Module Extraction

**Feature Branch**: `068-extract-flowgraph-execute`
**Created**: 2026-04-08
**Status**: Draft
**Input**: Step 4 of vertical-slice decomposition — extract 5 runtime execution files from circuitSimulator and 1 orchestration file from graphEditor into a new flowGraph-execute module, wrap as a coarse-grained CodeNode with FBP-native data flow ports, and wire into architecture.flow.kt. RuntimePreviewPanel.kt stays in graphEditor as a Compose UI presentation concern.
**Parent Feature**: 064-vertical-slice-refactor (Phase B)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Extract Runtime Execution Logic into Standalone Module (Priority: P1)

The 6 non-UI files responsible for runtime session orchestration, data flow animation, debug snapshots, and module session creation are extracted into a new flowGraph-execute module. The 5 circuitSimulator files (RuntimeSession, DataFlowAnimationController, DataFlowDebugger, ConnectionAnimation, CircuitSimulator) move in their entirety — circuitSimulator is fully absorbed. ModuleSessionFactory moves from graphEditor. RuntimePreviewPanel.kt stays in graphEditor as a Compose UI composable — matching the "Compose stays in graphEditor" principle established in features 066 and 067.

The 6 files to extract:
1. RuntimeSession.kt (circuitSimulator) — runtime lifecycle orchestration, observer wiring, state management (239 lines)
2. DataFlowAnimationController.kt (circuitSimulator) — animated dot creation, frame loop, pause/resume (175 lines)
3. DataFlowDebugger.kt (circuitSimulator) — per-connection value snapshots (98 lines)
4. ConnectionAnimation.kt (circuitSimulator) — data class for animation dot state (45 lines)
5. CircuitSimulator.kt (circuitSimulator) — Phase 1 placeholder stub (25 lines)
6. ModuleSessionFactory.kt (graphEditor) — factory creating RuntimeSession instances via reflection and DynamicPipelineController (depends on NodeDefinitionRegistry from flowGraph-inspect)

**Why this priority**: The module boundary must exist and compile before anything else can reference it. This is the foundational extraction step following the Strangler Fig pattern (copy files, keep originals, switch consumers, remove originals).

**Independent Test**: `./gradlew :flowGraph-execute:compileKotlinJvm` succeeds and the copied files resolve all imports from fbpDsl and flowGraph-inspect. No graphEditor or circuitSimulator dependency exists in the module's build configuration.

**Acceptance Scenarios**:

1. **Given** the 6 execution logic files live in circuitSimulator and graphEditor, **When** they are copied to flowGraph-execute with updated package declarations, **Then** `./gradlew :flowGraph-execute:compileKotlinJvm` succeeds with zero errors.
2. **Given** the flowGraph-execute module exists, **When** its build configuration is inspected, **Then** it depends only on `:fbpDsl` and `:flowGraph-inspect` — no dependency on `:graphEditor` or `:circuitSimulator`.
3. **Given** the 6 files are copied to the new module, **When** the original files still exist in their source modules, **Then** all three modules (flowGraph-execute, graphEditor, circuitSimulator) compile independently (Strangler Fig coexistence).
4. **Given** RuntimePreviewPanel.kt exists in graphEditor, **When** the extraction is complete, **Then** it remains in graphEditor unchanged.

---

### User Story 2 - Wrap Module as FlowGraphExecute CodeNode (Priority: P2)

A coarse-grained CodeNode wraps the entire execute module behind typed input/output ports. The module boundary is expressed as FBP-native data flow — not service interfaces. Data flows in as flowGraphModel (the flow graph to execute) and nodeDescriptors (node definitions needed for runtime pipeline building). Data flows out as executionState (current execution lifecycle state), animations (active connection animation data for canvas rendering), and debugSnapshots (per-connection value captures for runtime inspection).

**Why this priority**: The CodeNode is the module's external contract. Without it, there is no FBP-native boundary — consumers would need direct class imports, defeating the vertical-slice architecture.

**Independent Test**: TDD tests verify port signatures (2 inputs, 3 outputs, all String type), runtime type, and data flow through channels. Run `./gradlew :flowGraph-execute:jvmTest` to confirm all CodeNode tests pass.

**Acceptance Scenarios**:

1. **Given** FlowGraphExecuteCodeNode exists, **When** its port metadata is inspected, **Then** it has 2 input ports (flowGraphModel, nodeDescriptors) and 3 output ports (executionState, animations, debugSnapshots).
2. **Given** the CodeNode receives flow graph model data on its flowGraphModel port, **When** execution is triggered, **Then** it emits execution state changes on the executionState output port.
3. **Given** the CodeNode is executing with animation enabled, **When** data flows through the pipeline, **Then** it emits animation data on the animations output port.
4. **Given** either input port receives data independently, **When** the CodeNode processes the input, **Then** it uses the cached value from the other input to configure the runtime pipeline (anyInput mode).

---

### User Story 3 - Migrate Call Sites to New Module (Priority: P3)

All graphEditor files that currently import execution-related classes directly (RuntimeSession, ConnectionAnimation, ModuleSessionFactory) switch their imports to the flowGraph-execute module packages. FlowGraphCanvas.kt continues to consume ConnectionAnimation data but via the new module's package. Main.kt switches ModuleSessionFactory and RuntimeSession imports. The original files in circuitSimulator and graphEditor become dead code.

**Why this priority**: Consumers must switch to the new module before originals can be removed. This is the Strangler Fig "redirect" step.

**Independent Test**: `./gradlew :graphEditor:jvmTest :circuitSimulator:jvmTest` — all existing tests pass. No graphEditor source file imports from the old circuitSimulator packages (only the dead original files themselves still exist).

**Acceptance Scenarios**:

1. **Given** all call sites have been updated, **When** a search is performed for old circuitSimulator package imports in graphEditor (excluding dead original files), **Then** zero matches are found.
2. **Given** all call sites use the new module's packages, **When** the full test suite runs, **Then** all tests pass with zero regressions.
3. **Given** Main.kt, FlowGraphCanvas.kt, and RuntimePreviewPanel.kt reference execution types, **When** their imports are updated, **Then** they compile against the new module packages.

---

### User Story 4 - Remove Original Files and Absorb circuitSimulator (Priority: P4)

The 5 original files are deleted from circuitSimulator (effectively absorbing the entire module into flowGraph-execute). ModuleSessionFactory is deleted from graphEditor. Any remaining same-package references are resolved with explicit imports to the new module. The circuitSimulator module may be removed from settings.gradle.kts if no other modules depend on it.

**Why this priority**: Completes the Strangler Fig extraction. Must happen after all consumers are redirected (US3).

**Independent Test**: `./gradlew :graphEditor:jvmTest` — all tests pass after deletion. The 6 files no longer exist in their source modules.

**Acceptance Scenarios**:

1. **Given** all consumers use the new module, **When** the 5 circuitSimulator files and 1 graphEditor file are deleted, **Then** `./gradlew :graphEditor:compileKotlinJvm` succeeds.
2. **Given** circuitSimulator has no remaining source files, **When** it is removed from the build, **Then** no other module's compilation breaks.
3. **Given** the RuntimeSessionCharacterizationTest existed in circuitSimulator, **When** the originals are removed, **Then** the test is migrated to flowGraph-execute and passes.

---

### User Story 5 - Wire into architecture.flow.kt (Priority: P5)

The flowGraph-execute GraphNode in architecture.flow.kt is populated with the FlowGraphExecute CodeNode as a child node, with port mappings wiring exposed ports to child ports. The architecture test is updated to reflect any connection or port changes.

**Why this priority**: Makes the architecture diagram reflect the actual CodeNode implementation. Completes the vertical-slice wiring.

**Independent Test**: `./gradlew :graphEditor:jvmTest --tests "io.codenode.grapheditor.characterization.ArchitectureFlowKtsTest"` — all architecture tests pass.

**Acceptance Scenarios**:

1. **Given** the flowGraph-execute GraphNode exists in architecture.flow.kt, **When** it is opened, **Then** a FlowGraphExecute child codeNode is visible with 2 inputs and 3 outputs.
2. **Given** port mappings exist, **When** each exposed port is inspected, **Then** it maps to the corresponding child CodeNode port.
3. **Given** the architecture is updated, **When** the architecture test runs, **Then** all assertions pass including connection counts and node structure.

---

### User Story 6 - Verify Extraction Integrity (Priority: P6)

The dependency direction is verified (graphEditor -> flowGraph-execute -> flowGraph-inspect -> fbpDsl), no circular dependencies exist, the Strangler Fig commit sequence is preserved in git history, and the full test suite passes across all modules.

**Why this priority**: Final validation gate — ensures the extraction didn't introduce architectural violations.

**Independent Test**: `./gradlew :flowGraph-execute:dependencies` shows only fbpDsl and flowGraph-inspect. Full test suite passes across all modules.

**Acceptance Scenarios**:

1. **Given** the extraction is complete, **When** flowGraph-execute's dependency tree is inspected, **Then** only `:fbpDsl` and `:flowGraph-inspect` appear as project dependencies.
2. **Given** the git history, **When** commits are listed in order, **Then** the Strangler Fig sequence is visible: module creation -> file copy -> TDD tests -> CodeNode implementation -> call site migration -> original removal -> architecture wiring.
3. **Given** all modules, **When** the full test suite runs, **Then** zero test regressions across all modules.

---

### Edge Cases

- What happens when the flow graph model is empty or invalid? The RuntimeSession validates via DynamicPipelineController before starting — a validationError is emitted and execution does not begin.
- What happens when nodeDescriptors is empty (no nodes discovered)? The pipeline controller cannot build a pipeline with no node definitions — validation fails gracefully.
- What happens when animation is enabled but attenuation is below the 200ms threshold? RuntimeSession auto-disables animation when attenuation drops below 200ms.
- What happens when the pipeline is stopped mid-execution? RuntimeSession's stop() method clears animations, resets the controller, and transitions to IDLE state.
- What happens when circuitSimulator is removed but a downstream module still depends on it? All dependencies must be redirected to flowGraph-execute before circuitSimulator removal — compilation will fail otherwise, which is caught during the verification phase.
- What happens when ModuleSessionFactory cannot find a module's ViewModel via reflection? It handles missing ViewModels gracefully — the session is created without a ViewModel, and RuntimePreviewPanel shows controls without a module-specific preview.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST extract exactly 5 files from circuitSimulator (RuntimeSession, DataFlowAnimationController, DataFlowDebugger, ConnectionAnimation, CircuitSimulator) and 1 file from graphEditor (ModuleSessionFactory) into a new flowGraph-execute module.
- **FR-002**: The circuitSimulator module MUST be fully absorbed — all its source files move to flowGraph-execute.
- **FR-003**: RuntimePreviewPanel.kt MUST remain in graphEditor — it is a Compose UI presentation concern.
- **FR-004**: The flowGraph-execute module MUST depend only on fbpDsl and flowGraph-inspect — no dependency on graphEditor, circuitSimulator, or any other application module.
- **FR-005**: The FlowGraphExecuteCodeNode MUST expose 2 input ports (flowGraphModel, nodeDescriptors) and 3 output ports (executionState, animations, debugSnapshots), all of type String — matching the port signature already defined in architecture.flow.kt.
- **FR-006**: The module boundary MUST be expressed as FBP-native data flow through CodeNode ports — no Koin-wired service interfaces.
- **FR-007**: The CodeNode MUST use data-oriented port naming (flowGraphModel, nodeDescriptors, executionState, animations, debugSnapshots) — not service-oriented naming.
- **FR-008**: The CodeNode MUST follow the TDD pattern — tests are written and committed before implementation, verified to fail, then implementation makes them pass.
- **FR-009**: The extraction MUST follow the Strangler Fig pattern — copy files first, keep originals, switch consumers, then remove originals — with each step as a separate git commit.
- **FR-010**: The RuntimeSessionCharacterizationTest MUST continue to pass throughout the extraction.
- **FR-011**: All existing tests across graphEditor, circuitSimulator, flowGraph-types, flowGraph-persist, and flowGraph-inspect MUST pass after extraction.
- **FR-012**: The architecture.flow.kt GraphNode for flowGraph-execute MUST contain the FlowGraphExecute child codeNode with port mappings for all exposed ports.
- **FR-013**: The CodeNode MUST use anyInput mode — either input port receiving data independently triggers processing using the cached value from the other input.

### Key Entities

- **FlowGraphExecuteCodeNode**: The coarse-grained CodeNode wrapping all runtime execution functionality. Receives flow graph model and node descriptors as inputs, produces execution state, animation data, and debug snapshots as outputs.
- **Execution State**: Lifecycle state of the runtime pipeline (IDLE, RUNNING, PAUSED, ERROR). Flows out of the CodeNode as serialized String data.
- **Animations**: Active connection animation dots traveling along connections. Flows out as serialized animation data for canvas rendering.
- **Debug Snapshots**: Per-connection value captures showing the most recent data flowing through each connection. Flows out as serialized snapshot data for runtime inspection.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The flowGraph-execute module compiles independently with zero errors and zero dependencies on graphEditor or circuitSimulator.
- **SC-002**: 100% of existing tests pass across all modules (graphEditor, circuitSimulator, flowGraph-types, flowGraph-persist, flowGraph-inspect, flowGraph-execute) after extraction.
- **SC-003**: Zero circular dependencies exist in the module graph — verified by dependency inspection.
- **SC-004**: The Strangler Fig commit sequence contains at least 6 distinct commits matching the pattern: setup -> copy -> TDD tests -> implementation -> migration -> removal.
- **SC-005**: The architecture.flow.kt accurately reflects the live FlowGraphExecute CodeNode with correct port counts and mappings.
- **SC-006**: No service-oriented interfaces are introduced — the module boundary is purely FBP data flow through CodeNode ports.
- **SC-007**: RuntimePreviewPanel.kt remains in graphEditor unchanged after extraction.
- **SC-008**: The circuitSimulator module is fully absorbed into flowGraph-execute — zero source files remain in circuitSimulator.

## Assumptions

- The 5 circuitSimulator files have been verified via the feature 064 architecture audit to map cleanly to the execute bucket with zero cross-bucket complexity.
- ModuleSessionFactory depends on NodeDefinitionRegistry (now in flowGraph-inspect) and DynamicPipelineController (in fbpDsl) — both are available as module dependencies.
- The flowGraph-execute module follows the same KMP structure as other extracted modules (commonMain + jvmMain source sets). CircuitSimulator files are currently in commonMain, and ModuleSessionFactory uses JVM-specific reflection.
- The CodeNode will use a processor runtime with anyInput mode matching its port signature (2 inputs, 3 outputs).
- All output port data is serialized as String — downstream consumers in the composition root (graphEditor-sink) deserialize it.
- RuntimePreviewPanel.kt in graphEditor will import from the new flowGraph-execute module packages after the call site migration.
- FlowGraphCanvas.kt receives ConnectionAnimation data from the composition root — it will import ConnectionAnimation from the new module package.
- ModuleSessionFactory uses PersistenceBootstrap for Koin initialization — this dependency will need to be available in the new module or injected via the composition root.
- DataFlowAnimationController and DataFlowDebugger both implement identical buildPortConnectionMap() logic — this duplication is preserved as-is during extraction (refactoring is out of scope).

## Scope Boundaries

### In Scope

- Extracting the 6 execution logic files to a new module
- Absorbing the entire circuitSimulator module into flowGraph-execute
- Creating FlowGraphExecuteCodeNode with FBP-native port boundary
- TDD tests for the CodeNode
- Migrating all call sites in graphEditor
- Removing original files from circuitSimulator and graphEditor
- Migrating RuntimeSessionCharacterizationTest to flowGraph-execute
- Updating architecture.flow.kt with child node and port mappings
- Verifying dependency direction and test suite integrity

### Out of Scope

- Moving RuntimePreviewPanel.kt — it stays in graphEditor as a Compose UI concern
- Refactoring DataFlowAnimationController/DataFlowDebugger duplicate buildPortConnectionMap() logic
- Modifying runtime execution behavior or animation logic
- Adding new execution features or debug capabilities
- Extracting other modules (compose, generate)
- Runtime integration or Koin DI wiring at the module boundary
- Changing the ConnectionAnimation data model
- Updating MIGRATION.md (can be done as a follow-up)
