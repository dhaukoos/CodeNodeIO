# Research: Vertical Slice Refactor

**Feature**: 064-vertical-slice-refactor
**Date**: 2026-04-04

## R1: Vertical-Slice Module Boundaries

**Decision**: Decompose graphEditor, kotlinCompiler, and circuitSimulator into six workflow-oriented modules plus the composition root (modeled as source + sink).

| Module | Workflow | Key Responsibilities | File Count |
|--------|----------|---------------------|------------|
| flowGraph-types | IP type lifecycle | Discovery, registry, repository, file generation, migration. Consolidates IP type concerns previously scattered across inspect, persist, and generate. | 9 |
| flowGraph-compose | Building a flow graph interactively | Adding nodes from palette, connecting ports, validating connections (port type checking, cycle detection), configuring node properties. Input: user gestures + flowGraphModel. Output: graphState. | 10 |
| flowGraph-persist | Saving and loading | FlowGraphSerializer, FlowKtParser, file I/O, template registry. Input: flowGraphModel + ipTypeMetadata. Output: serializedOutput, loadedFlowGraph, graphNodeTemplates. | 8 |
| flowGraph-execute | Running and observing | Dynamic runtime pipeline, coroutine orchestration, execution control (pause/resume/step), data flow animation state. Absorbs circuitSimulator module. Input: flowGraphModel + nodeDescriptors. Output: executionState, animations, debugSnapshots. | 7 |
| flowGraph-generate | Producing deployable code | All kotlinCompiler generators (25 generators, 8 templates), module save workflow, CodeNode definition codegen, runtime file generation. Absorbs kotlinCompiler module. Input: flowGraphModel + serializedOutput + nodeDescriptors + ipTypeMetadata. Output: generatedOutput. | 46 |
| flowGraph-inspect | Understanding components | Node palette, filesystem scanner for node discovery, node property viewer, CodeNode text editor, debuggable data preview. Input: filesystemPaths, classpathEntries. Output: nodeDescriptors. | 13 |
| graphEditor (root) | Composition root | Compose UI composables, ViewModels, DI wiring. Modeled as graphEditor-source (ViewModel command actions) and graphEditor-sink (reactive Compose UI state). No business logic. | 27 |

**Total**: 120 files across 7 targets.

**Rationale**: Vertical slices align with user intent, not technical layers. Each slice naturally maps to a flow graph itself — dogfooding the FBP paradigm. The six-module partition was reached by extracting flowGraph-types from the original five-module plan to eliminate cyclic dependencies (inspect↔persist, inspect↔generate).

**Alternatives considered**:
- **Five modules (original plan)**: Had cyclic dependencies between inspect↔persist (IP type data) and inspect↔generate (IP type metadata). Eliminated by extracting flowGraph-types.
- **Horizontal layers** (model/service/repository): Would require touching multiple layers for any new feature. Contradicts FBP philosophy.
- **Fewer modules** (3 large slices): Would still leave modules with multiple responsibilities.
- **More modules** (e.g., separate palette from discovery): Over-decomposition for current codebase size.

**Future extraction candidates**: Five secondary concerns emerged as sub-workflows within existing slices. These are properly housed in their parent slice today but may warrant extraction if they grow significantly:

| Concern | ~Size | Current Home | Extraction Trigger |
|---------|-------|-------------|-------------------|
| Validation (port type, cycle detection, license, property) | 2000 lines | Distributed across compose, execute, generate | Pre/post-validation hooks needed; multi-tool validation (CLI, API server) |
| Entity scaffolding (EntityModuleGenerator + 16 sub-generators) | 2000 lines | generate | Entity generation exceeds 30% of generate's code; entity-specific UI/config grows |
| Build/deploy scaffolding (BuildScriptGenerator, ModuleGenerator) | 1400 lines | generate | Deployment workflows become independent (CI/CD config, artifact signing, publication) |
| Debug/observe (DataFlowDebugger, DataFlowAnimationController) | 600 lines | execute | Headless execution and visual debugging need to run independently |
| Template registry (NodeTemplateRegistry + 7 templates) | 300 lines | persist | Custom user-defined templates become a first-class feature |

---

## R2: Responsibility Bucket Assignment Strategy

**Decision**: Use a multi-signal approach combining import analysis, type usage, and call-site patterns to assign each of the ~120 source files across graphEditor, kotlinCompiler, and circuitSimulator to exactly one of seven buckets (types, compose, persist, execute, generate, inspect, root).

**Assignment signals (in priority order)**:
1. **Primary type operated on**: If a file's core logic manages IP type discovery/registry/repository → types. If it manipulates FlowGraph structure → compose. If it reads/writes files → persist. If it manages coroutine channels/execution → execute. If it generates source code → generate. If it scans/discovers/catalogs nodes → inspect.
2. **Import analysis**: The set of imports reveals which domain a file belongs to. `io.codenode.fbpdsl.serialization.*` → persist. `io.codenode.fbpdsl.runtime.*` → execute. `io.codenode.kotlincompiler.*` → generate.
3. **Compose/@Composable annotation**: Files with `@Composable` functions are UI — they stay in graphEditor (composition root) unless they are purely rendering a specific slice's data.
4. **Cross-reference density**: If a file has more cross-references to one bucket than another, it belongs in the bucket with the highest affinity.

**Rationale**: Single-signal assignment is fragile — some serialization files may support generation workflows. Multi-signal analysis catches these cases.

**Alternatives considered**:
- **Directory-based assignment only**: Quick but inaccurate. The `state/` directory contains files for compose, execute, and inspect workflows. Rejected.
- **Automated static analysis tool**: Overkill for ~120 files. Manual analysis with import checking produces richer annotations.
- **Module-based assignment** (all kotlinCompiler → generate, all circuitSimulator → execute): Close but not exact — some kotlinCompiler utilities may serve persist or inspect.

---

## R3: Characterization Test Strategy

**Decision**: Write characterization tests at the ViewModel/state/generator layer, organized into test classes covering the major seam categories across all three modules, plus structural tests for the architecture FlowGraph.

| Test Class | Module | Seam Category | What It Pins |
|-----------|--------|---------------|-------------|
| GraphDataOpsCharacterizationTest | graphEditor | Graph data operations | Node creation, port connection, port type validation, cycle detection, GraphNode creation, port mapping |
| RuntimeExecutionCharacterizationTest | graphEditor | Runtime execution | Graph execution start/stop, state transitions, channel wiring, execution state propagation |
| ViewModelCharacterizationTest | graphEditor | ViewModel integration | ViewModel state after graph mutations, palette state management, properties panel state |
| SerializationRoundTripCharacterizationTest | graphEditor | Serialization | .flow.kt round-trip fidelity for all node types (CodeNode, GraphNode, nested GraphNode), port types, connections |
| ArchitectureFlowKtsTest | graphEditor | Architecture structure | 8 nodes, 19 connections, DAG property, hub sources, command/state flow separation, target platforms |
| CodeGenerationCharacterizationTest | kotlinCompiler | Code generation | Generator output for known FlowGraph inputs: component generation, flow generation, module generation |
| FlowKtGeneratorCharacterizationTest | kotlinCompiler | Flow serialization | .flow.kt compiled output generation for FlowGraphs with various node/connection configurations |
| RuntimeSessionCharacterizationTest | circuitSimulator | Runtime session | Session lifecycle (start/stop/pause/resume), animation controller state, debugger snapshot capture |

**Rationale**:
- ViewModel/state layer is the natural seam between "business logic" (which will move to slices) and "UI" (which stays in graphEditor)
- Generator output is the natural seam for kotlinCompiler — pin what generators produce for known inputs
- RuntimeSession is the orchestration seam for circuitSimulator — pin lifecycle and observer behavior
- ArchitectureFlowKtsTest validates the architecture FlowGraph's structural invariants that Phase B depends on
- Tests at these layers run without Compose — just kotlin.test + coroutines-test
- Eight categories cover the seams across all three modules plus architecture validation

**Alternatives considered**:
- **UI-level tests using Compose test framework**: Too slow, too brittle, not available in all CI environments. Rejected.
- **Function-level unit tests**: Too granular. Would create hundreds of tests that don't map to extraction boundaries. Rejected.
- **Integration tests using full app startup**: Too coarse. A single failure wouldn't indicate which seam broke. Rejected.

---

## R4: Seam Identification and Documentation Format

**Decision**: Document seams as a dependency matrix in each module's ARCHITECTURE.md, with each entry specifying source file, target file, dependency type, and which module boundary it crosses.

**Seam types**:
- **Function call**: File A calls a function defined in File B
- **Type reference**: File A references a class/interface/enum defined in File B
- **Inheritance**: File A extends or implements a type from File B
- **State sharing**: File A reads or writes state owned by File B (e.g., MutableStateFlow)

**Documentation format**:
```markdown
| Source File | Target File | Type | Source Bucket | Target Bucket | Boundary |
|------------|------------|------|---------------|---------------|----------|
| GraphEditorViewModel.kt | FlowGraphSerializer.kt | Function call | root | persist | root→persist |
```

**Rationale**: A tabular format enables filtering by boundary and counting. This directly informs the interface design for each module.

---

## R5: Extraction Order Strategy

**Decision**: Extract in order of decreasing independence and decreasing risk:

1. **flowGraph-types** (first): IP type lifecycle is the most fundamental — consolidates concerns previously scattered across inspect, persist, and generate. No dependencies on other workflow modules. Eliminating this scatter first prevents confusion in subsequent extractions.
2. **flowGraph-persist** (second): Serialization is self-contained — FlowGraphSerializer and FlowKtParser have clear inputs (FlowGraph) and outputs (.flow.kt text). Depends on types (already extracted).
3. **flowGraph-inspect** (third): Discovery/registry logic is read-only with respect to graph state. Depends on types (already extracted) but not on persist.
4. **flowGraph-execute** (fourth): Runtime pipeline has clear boundaries (FlowGraph in, execution state out) but has more integration points with the UI (animation state, preview panel). Absorbs circuitSimulator entirely.
5. **flowGraph-generate** (fifth): Code generation depends on persist (for serialized output) and inspect (for node definitions). Absorbs kotlinCompiler entirely. Extract after its dependencies are stable.
6. **flowGraph-compose** (last): Graph composition is the most tightly coupled to the UI. Many ViewModels directly manage composition state. Extract last when all other slices are stable.

**Rationale**: Each extraction step leaves the application fully functional because earlier extractions have fewer consumers. The composition root (graphEditor) delegates to extracted modules one at a time, keeping the surface area of change small. This order was validated by dependency analysis confirming no step N depends on unextracted module N+1.

**Alternatives considered**:
- **Original 5-module order (persist→inspect→execute→generate→compose)**: Did not account for IP type concerns creating cycles. Rejected in favor of types-first.
- **Extract compose first** (most code): Would require the most interface changes upfront. Rejected.
- **Extract all at once**: Big-bang migration. No intermediate safety. Contradicts Strangler Fig pattern. Rejected.

---

## R6: Architecture FlowGraph Design

**Decision**: Create `architecture.flow.kt` using the FlowGraph DSL with eight GraphNode containers representing the target modules. The FlowGraph serves dual purpose: Phase A target blueprint and Phase B scaffold for progressive CodeNode population.

**Node design**:
- Six workflow modules → GraphNode containers with typed `exposeInput`/`exposeOutput` ports
- Composition root → two GraphNode containers: graphEditor-source (command outputs only) and graphEditor-sink (state inputs only)
- All ports use data-oriented naming (nodeDescriptors, ipTypeMetadata, flowGraphModel, etc.)
- 19 connections: 15 state flows + 4 command flows
- Connection graph forms a DAG — validated by ArchitectureFlowKtsTest

**Port naming convention**: Data-oriented (what flows through) rather than service-oriented (what capability is offered):
- `nodeDescriptors` (not `nodeRegistry`)
- `ipTypeMetadata` (not `ipTypeRegistry`)
- `serializedOutput` (not `serializer`)
- `graphNodeTemplates` (not `templateService`)
- `flowGraphModel` (not `flowGraph`)

**Source/sink split rationale**: The graphEditor composition root handles bidirectional data flow — ViewModel command actions flow out to workflow modules, and reactive Compose UI state flows in from workflow modules. Modeling this as two separate nodes (source and sink) maintains the DAG property while accurately representing FBP data flow semantics.

**Rationale**: Using the project's own `.flow.kt` format means the architecture can be opened in the graphEditor itself. The GraphNode container approach allows Phase B to populate each container with a real CodeNode without changing any connections — only the node type changes.

**Alternatives considered**:
- **Mermaid diagram in Markdown**: Not openable in the graphEditor. Doesn't dogfood the paradigm. Rejected.
- **CodeNode containers** (instead of GraphNode): CodeNodes are leaf nodes; GraphNodes are containers that can hold children. GraphNodes are semantically correct for modules that will contain sub-nodes. Rejected CodeNode.
- **Single root node** (instead of source/sink split): Would create cycles in the connection graph (root outputs to modules AND receives from modules). Rejected.

---

## R7: Cyclic Dependency Resolution

**Decision**: Eliminate the two cycles found in the original 5-module partition by extracting flowGraph-types as a dedicated module.

**Cycles identified**:
1. **inspect↔persist**: inspect provided IP type registry data that persist consumed for serialization validation; persist provided serialized IP type definitions that inspect consumed for registry population.
2. **inspect↔generate**: inspect provided IP type metadata that generate consumed for code generation; generate produced IP type definition files that inspect consumed for discovery.

**Root cause**: IP type lifecycle concerns (discovery, registry, repository, file generation, migration) were scattered across three modules. Each module held part of the IP type story, creating mutual dependencies.

**Resolution**: Extract 9 files into flowGraph-types:
- From inspect (6 files): IP type discovery, registry, and repository
- From persist (2 files): IP type serialization and migration
- From generate (1 file): IP type file generation

**Result**: Both cycles eliminated. types feeds data downstream to compose, persist, generate, and rootSink. inspect feeds nodeDescriptors downstream to compose, execute, generate, and rootSink. No module receives data from a module that also depends on it.

**Alternatives considered (Options A-F)**:
- **Option A**: Shared IP type module (generic utility) — too broad, not workflow-aligned
- **Option B**: Merge inspect and persist — would create an oversized module with mixed responsibilities
- **Option C**: Break cycles with events/callbacks — adds runtime complexity without eliminating structural coupling
- **Option D**: Accept cycles with careful interface design — violates DAG constraint needed for Phase B
- **Option E**: Move IP types to fbpDsl — fbpDsl is shared vocabulary, not workflow logic
- **Option F (chosen)**: Extract flowGraph-types as a dedicated workflow module — cleanly consolidates IP type lifecycle, eliminates both cycles, aligns with vertical-slice philosophy
