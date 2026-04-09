# Feature Specification: Extract flowGraph-generate Module

**Feature Branch**: `069-extract-flowgraph-generate`
**Created**: 2026-04-08
**Status**: Draft
**Input**: User description: "flowGraph-generate module extraction — Step 5 of Phase B vertical-slice decomposition"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Extract Code Generation Files into flowGraph-generate Module (Priority: P1)

As a developer working on the Phase B vertical-slice decomposition, I need to extract all code generation files (67 from kotlinCompiler + 6 from graphEditor) into a new `flowGraph-generate` KMP module so that code generation concerns are isolated behind an FBP-native boundary expressed as CodeNode ports.

The extraction follows the Strangler Fig pattern: copy files into the new module first, switch all consumers to import from the new location, then remove the originals. The kotlinCompiler module moves wholesale (all 67 files including source, tests, templates, and validators). From graphEditor, 6 files move: 2 viewmodel files (IPGeneratorViewModel.kt, NodeGeneratorViewModel.kt), 3 compilation files (CompilationService.kt, CompilationValidator.kt, RequiredPropertyValidator.kt), and 1 save file (ModuleSaveService.kt). The Compose UI panels (IPGeneratorPanel.kt, NodeGeneratorPanel.kt) remain in graphEditor since they are display-layer components.

**Why this priority**: This is the core extraction — without it, the module boundary doesn't exist. All other stories depend on this.

**Independent Test**: Can be verified by running the new module's compilation and confirming all existing characterization tests pass from the new module location.

**Acceptance Scenarios**:

1. **Given** the kotlinCompiler module with 67 files and graphEditor with 6 generate-bucket files, **When** the extraction is complete, **Then** a new `flowGraph-generate` KMP module exists containing all source and test files under the `io.codenode.flowgraphgenerate` package.
2. **Given** consumers importing from `io.codenode.kotlincompiler` or graphEditor generate packages, **When** imports are migrated, **Then** all consumers reference `io.codenode.flowgraphgenerate` and compile successfully.
3. **Given** the original files in kotlinCompiler and graphEditor, **When** originals are removed, **Then** the kotlinCompiler module is deleted entirely and the 6 graphEditor files are removed, with no remaining references to old packages.
4. **Given** the IPGeneratorPanel.kt and NodeGeneratorPanel.kt files in graphEditor, **When** extraction is complete, **Then** these Compose UI panels remain in graphEditor and import from `io.codenode.flowgraphgenerate` for their ViewModel dependencies.

---

### User Story 2 - Wrap flowGraph-generate as a Sub-FlowGraph with Multiple CodeNodes (Priority: P2)

As a developer, I need the flowGraph-generate module boundary expressed as a sub-FlowGraph of multiple CodeNodes (each within the 3-input maximum), so that the module participates in the FBP data flow graph without requiring a new 4-input runtime type.

Per architecture.flow.kt, the generate graphNode has 4 inputs and 1 output. Since CodeNode templates only support up to 3 inputs, the internals are decomposed into two child CodeNodes:

- **GenerateContextAggregator** (In2AnyOut1): Takes `flowGraphModel` + `serializedOutput` and produces a combined `generationContext`. These two inputs naturally pair — they represent *what* to generate (the graph model and its serialized form from persist).
- **FlowGraphGenerate** (In3AnyOut1): Takes `generationContext` + `nodeDescriptors` + `ipTypeMetadata` and produces `generatedOutput`. These represent the combined context plus *how* to generate (node definitions and type metadata).

Both CodeNodes use anyInput mode. An internal connection within the graphNode wires the aggregator's output to the main generator's input.

**Why this priority**: The CodeNode sub-graph is the FBP-native module boundary. It must exist before wiring into the architecture graph.

**Independent Test**: Can be verified by running unit tests that confirm port signatures, runtime creation, internal wiring, and basic data flow through both CodeNodes.

**Acceptance Scenarios**:

1. **Given** the GenerateContextAggregator CodeNode, **When** inspected, **Then** it declares 2 input ports (flowGraphModel, serializedOutput) and 1 output port (generationContext), all typed as String, with anyInput=true.
2. **Given** the FlowGraphGenerate CodeNode, **When** inspected, **Then** it declares 3 input ports (generationContext, nodeDescriptors, ipTypeMetadata) and 1 output port (generatedOutput), all typed as String, with anyInput=true.
3. **Given** both CodeNodes wired together, **When** data arrives on flowGraphModel or serializedOutput, **Then** the aggregator fires, produces generationContext, which feeds into FlowGraphGenerate.
4. **Given** FlowGraphGenerate receives generationContext plus nodeDescriptors or ipTypeMetadata, **When** the processing block runs, **Then** it produces output on the generatedOutput port.

---

### User Story 3 - Wire flowGraph-generate Sub-FlowGraph into architecture.flow.kt (Priority: P3)

As a developer, I need the flowGraph-generate graphNode in architecture.flow.kt updated with two child codeNode definitions, their internal connection, port mappings, and verified external connections so that the architecture graph accurately reflects the real module wiring.

**Why this priority**: This completes the integration into the architecture flow graph. It depends on US1 (module exists) and US2 (CodeNodes exist).

**Independent Test**: Can be verified by parsing architecture.flow.kt and confirming the generate graphNode contains two child codeNodes with correct port mappings, an internal connection, and all 20 external connections remain valid.

**Acceptance Scenarios**:

1. **Given** the generate graphNode in architecture.flow.kt, **When** updated, **Then** it contains two child codeNodes: "GenerateContextAggregator" (TRANSFORMER, 2 inputs, 1 output) and "FlowGraphGenerate" (TRANSFORMER, 3 inputs, 1 output).
2. **Given** the two child codeNodes, **When** port mappings are defined, **Then** the 4 exposed input ports map to the correct child codeNode inputs (flowGraphModel and serializedOutput → aggregator; nodeDescriptors and ipTypeMetadata → generator), and the exposed output port maps to FlowGraphGenerate's generatedOutput.
3. **Given** the internal connection, **When** defined, **Then** GenerateContextAggregator's generationContext output connects to FlowGraphGenerate's generationContext input.
4. **Given** the updated architecture.flow.kt, **When** parsed, **Then** it produces a valid FlowGraph with all external connections intact and the generate node has the expected two-node sub-graph structure.

---

### Edge Cases

- What happens when graphEditor UI panels (IPGeneratorPanel, NodeGeneratorPanel) reference ViewModel classes that moved to flowGraph-generate? They must be updated to import from the new package.
- What happens when the idePlugin references kotlinCompiler classes? The idePlugin dependency must be updated from `:kotlinCompiler` to `:flowGraph-generate`.
- What happens when kotlinCompiler tests use JVM-specific APIs (characterization tests in jvmTest)? They must be placed in the `jvmTest` source set in the new module.
- What happens when kotlinCompiler's commonTest files reference commonMain code? Package declarations must be updated consistently across both source sets.
- What happens when the kotlinCompiler module is deleted but settings.gradle.kts still includes it? The settings file must be updated to replace `:kotlinCompiler` with `:flowGraph-generate`.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST create a new `flowGraph-generate` KMP module with the standard multi-platform structure (commonMain, commonTest, jvmMain, jvmTest).
- **FR-002**: System MUST copy 65 kotlinCompiler files (source, tests, templates, validators, characterization tests) into the new module with package renamed from `io.codenode.kotlincompiler` to `io.codenode.flowgraphgenerate`. Two jvmMain tool scripts (RegenerateStopWatch.kt, GenerateGeoLocationModule.kt) are excluded — they were missed during the feature 060 DemoProject split and must be moved to CodeNodeIO-DemoProject before kotlinCompiler is deleted.
- **FR-003**: System MUST copy 6 graphEditor generate-bucket files (IPGeneratorViewModel.kt, NodeGeneratorViewModel.kt, CompilationService.kt, CompilationValidator.kt, RequiredPropertyValidator.kt, ModuleSaveService.kt) into the new module with appropriate package names.
- **FR-004**: System MUST NOT move IPGeneratorPanel.kt or NodeGeneratorPanel.kt — these Compose UI panels remain in graphEditor.
- **FR-005**: System MUST implement two CodeNodeDefinitions: GenerateContextAggregatorCodeNode (2 inputs: flowGraphModel, serializedOutput; 1 output: generationContext) and FlowGraphGenerateCodeNode (3 inputs: generationContext, nodeDescriptors, ipTypeMetadata; 1 output: generatedOutput).
- **FR-006**: System MUST use anyInput mode on both CodeNodes so each fires on any single input arriving.
- **FR-007**: System MUST migrate all consumers (graphEditor, idePlugin, and any other modules referencing kotlinCompiler) to import from `io.codenode.flowgraphgenerate`.
- **FR-008**: System MUST delete the entire kotlinCompiler module directory after all consumers are migrated.
- **FR-009**: System MUST remove the 6 original graphEditor generate-bucket files after consumers are migrated.
- **FR-010**: System MUST update architecture.flow.kt to add two child codeNodes, their internal connection, and port mappings to the generate graphNode.
- **FR-011**: System MUST update settings.gradle.kts to replace the `:kotlinCompiler` include with `:flowGraph-generate`.
- **FR-012**: System MUST ensure all existing characterization tests (CodeGenerationCharacterizationTest, FlowKtGeneratorCharacterizationTest) pass from the new module location.
- **FR-013**: System MUST include TDD unit tests for the FlowGraphGenerateCodeNode covering port signatures, runtime creation, and data flow.
- **FR-014**: System MUST follow the Strangler Fig pattern: copy first, migrate consumers, then remove originals — each as separate verifiable steps.

### Key Entities

- **GenerateContextAggregatorCodeNode**: CodeNodeDefinition (In2AnyOut1) that combines flowGraphModel and serializedOutput into a generationContext.
- **FlowGraphGenerateCodeNode**: CodeNodeDefinition (In3AnyOut1) that takes generationContext, nodeDescriptors, and ipTypeMetadata to produce generatedOutput.
- **flowGraph-generate module**: KMP module absorbing all of kotlinCompiler plus 6 graphEditor generate-bucket files.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The `flowGraph-generate` module compiles successfully on all target platforms.
- **SC-002**: All existing characterization tests pass from the new module location without modification to test logic.
- **SC-003**: All existing tests across the entire project pass after extraction is complete.
- **SC-004**: Zero references remain to `io.codenode.kotlincompiler` package in any source file outside `flowGraph-generate`.
- **SC-005**: The kotlinCompiler module directory no longer exists in the project.
- **SC-006**: architecture.flow.kt parses successfully and contains the generate graphNode with two child codeNodes (GenerateContextAggregator and FlowGraphGenerate), an internal connection, and correct port mappings.
- **SC-007**: Unit tests for both CodeNodes verify port signatures, anyInput mode, internal wiring, and end-to-end data flow through the sub-graph.
- **SC-008**: graphEditor UI panels (IPGeneratorPanel, NodeGeneratorPanel) compile and reference ViewModels from `io.codenode.flowgraphgenerate`.

## Assumptions

- The kotlinCompiler module has no consumers outside graphEditor and idePlugin. If other modules depend on it, they will be discovered and migrated during the consumer migration phase.
- The 6 graphEditor generate-bucket files do not have circular dependencies back to graphEditor Compose UI code. If they do, the dependency direction must be inverted (UI depends on generate, not vice versa).
- In2AnyOut1Runtime and In3AnyOut1Runtime exist in fbpDsl and are sufficient for the two-node decomposition.
- The build.gradle.kts for the new module will depend on :fbpDsl, :flowGraph-types, :flowGraph-persist, and :flowGraph-inspect as needed by the extracted code.
