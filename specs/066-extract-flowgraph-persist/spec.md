# Feature Specification: flowGraph-persist Module Extraction

**Feature Branch**: `066-extract-flowgraph-persist`
**Created**: 2026-04-07
**Status**: Draft
**Input**: Step 2 of vertical-slice decomposition — extract 8 serialization/persistence files from graphEditor into a new flowGraph-persist module, wrap as a coarse-grained CodeNode with FBP-native data flow ports, and wire into architecture.flow.kt.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Extract Persistence Files into Standalone Module (Priority: P1)

The 8 files responsible for flow graph serialization, deserialization, template management, and textual view are extracted from graphEditor into a new flowGraph-persist module. The module depends only on fbpDsl and flowGraph-types — no dependency on graphEditor. All existing serialization tests continue to pass against the new module location.

**Why this priority**: The module boundary must exist and compile before anything else can reference it. This is the foundational extraction step following the Strangler Fig pattern (copy files, keep originals, switch consumers, remove originals).

**Independent Test**: `./gradlew :flowGraph-persist:jvmTest` compiles and the copied files resolve all imports from fbpDsl and flowGraph-types. No graphEditor dependency exists in the module's build configuration.

**Acceptance Scenarios**:

1. **Given** the 8 persistence files live in graphEditor, **When** they are copied to flowGraph-persist with updated package declarations, **Then** `./gradlew :flowGraph-persist:compileKotlinJvm` succeeds with zero errors.
2. **Given** the flowGraph-persist module exists, **When** its build configuration is inspected, **Then** it depends only on `:fbpDsl` and `:flowGraph-types` — no `:graphEditor` dependency.
3. **Given** the 8 files are copied to the new module, **When** the original files still exist in graphEditor, **Then** both modules compile independently (Strangler Fig coexistence).

---

### User Story 2 - Wrap Module as FlowGraphPersist CodeNode (Priority: P2)

A coarse-grained CodeNode wraps the entire persist module behind typed input/output ports. The module boundary is expressed as FBP-native data flow — not service interfaces. Data flows in as flowGraphModel (the graph to serialize/deserialize) and ipTypeMetadata (type information needed during serialization). Data flows out as serializedOutput (.flow.kt text), loadedFlowGraph (deserialized graph data), and graphNodeTemplates (available template metadata).

**Why this priority**: The CodeNode is the module's external contract. Without it, there is no FBP-native boundary — consumers would need direct class imports, defeating the vertical-slice architecture.

**Independent Test**: TDD tests verify port signatures (2 inputs, 3 outputs, all String type), runtime type (processor runtime with appropriate input/output count), and data flow through channels — input on flowGraphModel port produces output on serializedOutput port. Run `./gradlew :flowGraph-persist:jvmTest` to confirm all CodeNode tests pass.

**Acceptance Scenarios**:

1. **Given** FlowGraphPersistCodeNode exists, **When** its port metadata is inspected, **Then** it has 2 input ports (flowGraphModel, ipTypeMetadata) and 3 output ports (serializedOutput, loadedFlowGraph, graphNodeTemplates).
2. **Given** the CodeNode receives a flowGraphModel string on its input port, **When** the processing logic runs, **Then** it emits serialized .flow.kt text on the serializedOutput port.
3. **Given** the CodeNode receives a serialized .flow.kt string as a command on flowGraphModel, **When** the processing logic detects a deserialize command, **Then** it emits the parsed FlowGraph data on the loadedFlowGraph port.
4. **Given** the CodeNode receives a template CRUD command, **When** the command is processed, **Then** it emits updated template metadata on the graphNodeTemplates port.

---

### User Story 3 - Migrate Call Sites to New Module (Priority: P3)

All graphEditor files that currently import persistence classes directly (FlowKtParser, FlowGraphSerializer, GraphNodeTemplateMeta, GraphNodeTemplateSerializer, GraphNodeTemplateRegistry, ViewSynchronizer, TextualView) switch their imports to the flowGraph-persist module packages. The original files in graphEditor become dead code.

**Why this priority**: Consumers must switch to the new module before originals can be removed. This is the Strangler Fig "redirect" step.

**Independent Test**: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` — all existing tests pass. No graphEditor source file imports from the old persistence packages (only the dead original files themselves still exist).

**Acceptance Scenarios**:

1. **Given** all call sites have been updated, **When** a search is performed for old package imports in graphEditor (excluding the original dead files), **Then** zero matches are found.
2. **Given** all call sites use the new module's packages, **When** the full test suite runs, **Then** all tests pass with zero regressions.
3. **Given** the kotlinCompiler module references FlowKtGenerator (which produces .flow.kt but lives in generate, not persist), **When** its tests run, **Then** FlowKtGeneratorCharacterizationTest passes unchanged.

---

### User Story 4 - Remove Original Files from graphEditor (Priority: P4)

The 8 original persistence files are deleted from graphEditor. Any remaining same-package references are resolved with explicit imports to the new module. The graphEditor module no longer contains any serialization/persistence code.

**Why this priority**: Completes the Strangler Fig extraction. Must happen after all consumers are redirected (US3).

**Independent Test**: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` — all tests pass after deletion. The 8 files no longer exist in graphEditor.

**Acceptance Scenarios**:

1. **Given** all consumers use the new module, **When** the 8 original files are deleted, **Then** `./gradlew :graphEditor:compileKotlinJvm` succeeds.
2. **Given** test files in the same packages used same-package access, **When** the originals are removed, **Then** explicit imports are added to resolve any compilation errors.

---

### User Story 5 - Wire into architecture.flow.kt (Priority: P5)

The flowGraph-persist GraphNode in architecture.flow.kt is populated with the FlowGraphPersistCodeNode as a child node, with port mappings wiring exposed ports to child ports. The architecture test is updated to reflect any connection or port changes.

**Why this priority**: Makes the architecture diagram reflect the actual CodeNode implementation. Completes the vertical-slice wiring.

**Independent Test**: `./gradlew :graphEditor:jvmTest --tests "characterization.ArchitectureFlowKtsTest"` — all architecture tests pass.

**Acceptance Scenarios**:

1. **Given** the flowGraph-persist GraphNode exists in architecture.flow.kt, **When** it is opened, **Then** a FlowGraphPersist child codeNode is visible with 2 inputs and 3 outputs.
2. **Given** port mappings exist, **When** each exposed port is inspected, **Then** it maps to the corresponding child CodeNode port.
3. **Given** the architecture is updated, **When** the architecture test runs, **Then** all assertions pass including connection counts and node structure.

---

### User Story 6 - Verify Extraction Integrity (Priority: P6)

The dependency direction is verified (graphEditor → flowGraph-persist → flowGraph-types → fbpDsl), no circular dependencies exist, the Strangler Fig commit sequence is preserved in git history, and the full test suite passes across all modules.

**Why this priority**: Final validation gate — ensures the extraction didn't introduce architectural violations.

**Independent Test**: `./gradlew :flowGraph-persist:dependencies` shows only fbpDsl and flowGraph-types. Full test suite passes across all modules.

**Acceptance Scenarios**:

1. **Given** the extraction is complete, **When** flowGraph-persist's dependency tree is inspected, **Then** only `:fbpDsl` and `:flowGraph-types` appear as project dependencies.
2. **Given** the git history, **When** commits are listed in order, **Then** the Strangler Fig sequence is visible: module creation → file copy → TDD tests → CodeNode implementation → call site migration → original removal → architecture wiring.
3. **Given** all modules, **When** the full test suite runs, **Then** zero test regressions across all modules.

---

### Edge Cases

- What happens when a .flow.kt file contains syntax errors during deserialization? The existing FlowKtParser error handling is preserved — parsing returns a failure result, not an exception.
- What happens when template files reference IP types that no longer exist? The CodeNode still emits available template metadata; missing type references are logged but don't prevent template listing.
- What happens when the serializer encounters a GraphNode with child nodes of unknown types? Existing behavior is preserved — unknown node types are serialized as generic nodes.
- What happens when flowGraphModel input is empty or null? The CodeNode does not emit on any output port (no-op).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST extract exactly 6 files from graphEditor into a new flowGraph-persist module: FlowGraphSerializer, FlowKtParser, GraphNodeTemplateSerializer, GraphNodeTemplateMeta, GraphNodeTemplateRegistry, and GraphNodeTemplateInstantiator. (Research R1: ViewSynchronizer and TextualView belong in the compose slice, not persist.)
- **FR-002**: The flowGraph-persist module MUST depend only on fbpDsl and flowGraph-types — no dependency on graphEditor or any other application module.
- **FR-003**: The FlowGraphPersistCodeNode MUST expose 2 input ports (flowGraphModel, ipTypeMetadata) and 3 output ports (serializedOutput, loadedFlowGraph, graphNodeTemplates), all of type String.
- **FR-004**: The module boundary MUST be expressed as FBP-native data flow through CodeNode ports — no Koin-wired service interfaces.
- **FR-005**: The CodeNode MUST use data-oriented port naming (flowGraphModel, serializedOutput, loadedFlowGraph, graphNodeTemplates) — not service-oriented naming.
- **FR-006**: The CodeNode MUST follow the TDD pattern — tests are written and committed before implementation, verified to fail, then implementation makes them pass.
- **FR-007**: The extraction MUST follow the Strangler Fig pattern — copy files first, keep originals, switch consumers, then remove originals — with each step as a separate git commit.
- **FR-008**: The SerializationRoundTripCharacterizationTest MUST continue to pass throughout the extraction.
- **FR-009**: The FlowKtGeneratorCharacterizationTest in kotlinCompiler MUST continue to pass — FlowKtGenerator lives in generate, not persist.
- **FR-010**: The architecture.flow.kt GraphNode for flowGraph-persist MUST contain the FlowGraphPersist child codeNode with port mappings for all exposed ports.
- **FR-011**: All existing tests across graphEditor, kotlinCompiler, circuitSimulator, and flowGraph-types MUST pass after extraction.

### Key Entities

- **FlowGraphPersistCodeNode**: The coarse-grained CodeNode wrapping all persistence functionality. Receives flow graph data and type metadata as inputs, produces serialized text, loaded graphs, and template listings as outputs.
- **FlowGraph Model Data**: Serialized representation of a flow graph (the .flow.kt text format) — flows in and out of the CodeNode as String data on ports.
- **GraphNode Template Metadata**: Information about saved GraphNode templates (name, file path, description) — emitted on the graphNodeTemplates output port.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The flowGraph-persist module compiles independently with zero errors and zero dependencies on graphEditor.
- **SC-002**: 100% of existing tests pass across all 5 modules (graphEditor, kotlinCompiler, circuitSimulator, flowGraph-types, flowGraph-persist) after extraction.
- **SC-003**: Zero circular dependencies exist in the module graph — verified by dependency inspection.
- **SC-004**: The Strangler Fig commit sequence contains at least 6 distinct commits matching the pattern: setup → copy → TDD tests → implementation → migration → removal.
- **SC-005**: The architecture.flow.kt accurately reflects the live FlowGraphPersist CodeNode with correct port counts and mappings.
- **SC-006**: No service-oriented interfaces (Koin modules, DI bindings) are introduced — the module boundary is purely FBP data flow through CodeNode ports.

## Assumptions

- The 8 files identified for extraction are complete and no additional persistence-related files need to move. If additional files are discovered during implementation, they will be included.
- The flowGraph-persist module follows the same KMP structure as flowGraph-types (commonMain + jvmMain source sets).
- The FlowGraphPersistCodeNode will use a processor runtime with the appropriate input/output count matching its port signature (2 inputs, 3 outputs).
- IP type metadata needed during serialization will arrive on the ipTypeMetadata input port as serialized String data (same format emitted by FlowGraphTypesCodeNode).
- Template files are stored on the filesystem — the CodeNode handles filesystem I/O internally, receiving only the root path information needed to locate templates.

## Scope Boundaries

### In Scope

- Extracting the 8 persistence files to a new module
- Creating FlowGraphPersistCodeNode with FBP-native port boundary
- TDD tests for the CodeNode
- Migrating all call sites in graphEditor
- Removing original files
- Updating architecture.flow.kt with child node and port mappings
- Verifying dependency direction and test suite integrity

### Out of Scope

- Modifying FlowKtGenerator (lives in generate module, not persist)
- Changing serialization format or behavior
- Adding new persistence features
- Extracting other modules (inspect, compose, execute, generate)
- Runtime integration or Koin DI wiring
