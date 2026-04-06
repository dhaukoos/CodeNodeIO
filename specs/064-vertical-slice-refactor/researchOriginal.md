# Research: Vertical Slice Refactor

**Feature**: 064-vertical-slice-refactor
**Date**: 2026-04-02

## R1: Vertical-Slice Module Boundaries

**Decision**: Decompose graphEditor, kotlinCompiler, and circuitSimulator into five workflow-oriented modules plus the composition root.

| Module | Workflow | Key Responsibilities |
|--------|----------|---------------------|
| flowGraph-compose | Building a flow graph interactively | Adding nodes from palette, connecting ports, validating connections (port type checking, cycle detection), configuring node properties. Input: user gestures. Output: valid FlowGraph. |
| flowGraph-persist | Saving and loading | FlowGraphSerializer, FlowKtParser, file I/O, reconciling deserialized state with editor state. Input: FlowGraph. Output: .flow.kts file (or reverse). |
| flowGraph-execute | Running and observing | Dynamic runtime pipeline, coroutine orchestration, execution control (pause/resume/step), data flow animation state, RuntimeSession, DataFlowAnimationController, DataFlowDebugger. Absorbs circuitSimulator module. Input: FlowGraph. Output: execution state stream, IP snapshots. |
| flowGraph-generate | Producing deployable code | All kotlinCompiler generators (25 generators, 8 templates), module save workflow, CodeNode definition codegen, runtime file generation, build configuration. Absorbs kotlinCompiler module. Input: FlowGraph + generation config. Output: file tree on disk. |
| flowGraph-inspect | Understanding components | Node palette, IP type registry, filesystem scanner for node discovery, node property viewer, CodeNode text editor, debuggable data preview. Input: filesystem paths, classpath. Output: node descriptors, IP type metadata. |
| graphEditor | Composition root | Compose UI composables, ViewModels, DI wiring. Wires slices together. No business logic. |

**Rationale**: Vertical slices align with user intent, not technical layers. A developer adding "export graph as PNG" creates a new slice rather than cutting across existing layers. Each slice naturally maps to a flow graph itself — dogfooding the FBP paradigm.

**Alternatives considered**:
- **Horizontal layers** (model/service/repository): Would require touching multiple layers for any new feature. Rejected because it contradicts the FBP philosophy where workflows are self-contained.
- **Fewer modules** (e.g., 3 large slices): Would still leave modules with multiple responsibilities. Five slices map cleanly to the five distinct user workflows.
- **More modules** (e.g., separate palette from discovery): Over-decomposition. Palette and discovery are part of the same "understanding what's available" workflow.

**Future extraction candidates**: The expanded scope analysis (graphEditor + kotlinCompiler + circuitSimulator) confirmed that five slices cover all primary user workflows. However, five secondary concerns emerged as sub-workflows within existing slices. These are properly housed in their parent slice today but may warrant extraction if they grow significantly:

| Concern | ~Size | Current Home | Extraction Trigger |
|---------|-------|-------------|-------------------|
| Validation (port type, cycle detection, license, property) | 2000 lines | Distributed across compose, execute, generate | Pre/post-validation hooks needed; multi-tool validation (CLI, API server) |
| Entity scaffolding (EntityModuleGenerator + 16 sub-generators) | 2000 lines | generate | Entity generation exceeds 30% of generate's code; entity-specific UI/config grows |
| Build/deploy scaffolding (BuildScriptGenerator, ModuleGenerator) | 1400 lines | generate | Deployment workflows become independent (CI/CD config, artifact signing, publication) |
| Debug/observe (DataFlowDebugger, DataFlowAnimationController) | 600 lines | execute | Headless execution and visual debugging need to run independently |
| Template registry (NodeTemplateRegistry + 7 templates) | 300 lines | inspect | Custom user-defined templates become a first-class feature |

These are sub-concerns within workflows, not distinct user intents — a developer doesn't think "now I'm validating" as a separate activity. They remain inside their parent slice unless the extraction triggers are met.

---

## R2: Responsibility Bucket Assignment Strategy

**Decision**: Use a multi-signal approach combining import analysis, type usage, and call-site patterns to assign each of the ~123 source files across graphEditor, kotlinCompiler, and circuitSimulator to exactly one bucket.

**Assignment signals (in priority order)**:
1. **Primary type operated on**: If a file's core logic manipulates FlowGraph structure → compose. If it reads/writes files → persist. If it manages coroutine channels/execution → execute. If it generates source code → generate. If it scans/discovers/catalogs → inspect.
2. **Import analysis**: The set of imports reveals which domain a file belongs to. `io.codenode.fbpdsl.serialization.*` → persist. `io.codenode.fbpdsl.runtime.*` → execute. `io.codenode.kotlincompiler.*` → generate.
3. **Compose/@Composable annotation**: Files with `@Composable` functions are UI — they stay in graphEditor (composition root) unless they are purely rendering a specific slice's data, in which case they may move with their slice's ViewModel.
4. **Cross-reference density**: If a file has more cross-references to one bucket than another, it belongs in the bucket with the highest affinity.

**Rationale**: Single-signal assignment (e.g., "all files in `serialization/` go to persist") is fragile — some serialization files may support generation workflows. Multi-signal analysis catches these cases.

**Alternatives considered**:
- **Directory-based assignment only**: Quick but inaccurate. The `state/` directory contains files for compose, execute, and inspect workflows. Rejected.
- **Automated static analysis tool**: Could parse the AST, but overkill for ~123 files. Manual analysis with import checking is sufficient and produces richer annotations (why a file belongs to a bucket, not just that it does).
- **Module-based assignment** (all kotlinCompiler → generate, all circuitSimulator → execute): Close but not exact — some kotlinCompiler utilities may serve persist or inspect. Multi-signal analysis catches these edge cases.

---

## R3: Characterization Test Strategy

**Decision**: Write characterization tests at the ViewModel/state/generator layer, organized into test classes covering the major seam categories across all three modules.

| Test Class | Module | Seam Category | What It Pins |
|-----------|--------|---------------|-------------|
| GraphDataOpsCharacterizationTest | graphEditor | Graph data operations | Node creation, port connection, port type validation, cycle detection, GraphNode creation, port mapping |
| RuntimeExecutionCharacterizationTest | graphEditor | Runtime execution | Graph execution start/stop, state transitions, channel wiring, execution state propagation |
| ViewModelCharacterizationTest | graphEditor | ViewModel integration | ViewModel state after graph mutations, palette state management, properties panel state |
| SerializationRoundTripCharacterizationTest | graphEditor | Serialization | .flow.kts round-trip fidelity for all node types (CodeNode, GraphNode, nested GraphNode), port types, connections |
| CodeGenerationCharacterizationTest | kotlinCompiler | Code generation | Generator output for known FlowGraph inputs: component generation, flow generation, module generation |
| FlowKtGeneratorCharacterizationTest | kotlinCompiler | Flow serialization | .flow.kt compiled output generation for FlowGraphs with various node/connection configurations |
| RuntimeSessionCharacterizationTest | circuitSimulator | Runtime session | Session lifecycle (start/stop/pause/resume), animation controller state, debugger snapshot capture |

**Rationale**:
- ViewModel/state layer is the natural seam between "business logic" (which will move to slices) and "UI" (which stays in graphEditor)
- Generator output is the natural seam for kotlinCompiler — pin what generators produce for known inputs
- RuntimeSession is the orchestration seam for circuitSimulator — pin lifecycle and observer behavior
- Tests at these layers run without Compose — just kotlin.test + coroutines-test
- Existing test patterns in graphEditor/src/jvmTest and kotlinCompiler/src/jvmTest already demonstrate this approach
- Seven categories cover the seams across all three modules

**Alternatives considered**:
- **UI-level tests using Compose test framework**: Requires `@OptIn(ExperimentalTestApi::class)` and `createComposeRule()`. Too slow, too brittle, and not available in all CI environments. Rejected.
- **Function-level unit tests**: Too granular. Would create hundreds of tests that are hard to maintain and don't map to extraction boundaries. Rejected.
- **Integration tests using full app startup**: Too coarse. A single failure wouldn't indicate which seam broke. Rejected.

---

## R4: Seam Identification and Documentation Format

**Decision**: Document seams as a dependency matrix in ARCHITECTURE.md, with each entry specifying source file, target file, dependency type, and which module boundary it crosses.

**Seam types**:
- **Function call**: File A calls a function defined in File B
- **Type reference**: File A references a class/interface/enum defined in File B
- **Inheritance**: File A extends or implements a type from File B
- **State sharing**: File A reads or writes state owned by File B (e.g., MutableStateFlow)

**Documentation format**:
```markdown
| Source File | Target File | Type | Source Bucket | Target Bucket | Boundary |
|------------|------------|------|---------------|---------------|----------|
| GraphEditorViewModel.kt | FlowGraphSerializer.kt | Function call | compose | persist | compose→persist |
```

**Rationale**: A tabular format enables filtering by boundary (e.g., "show all compose→persist seams") and counting (e.g., "persist has 12 inbound seams"). This directly informs the interface design for each module.

---

## R5: Extraction Order Strategy

**Decision**: Extract in order of decreasing independence and decreasing risk:

1. **flowGraph-persist** (first): Serialization is the most self-contained — FlowGraphSerializer and FlowKtParser have clear inputs (FlowGraph) and outputs (.flow.kts text). Fewest inbound dependencies from other business logic.
2. **flowGraph-inspect** (second): Discovery/registry logic is read-only with respect to graph state. Depends on persist for loading definitions but not vice versa.
3. **flowGraph-execute** (third): Runtime pipeline has clear boundaries (FlowGraph in, execution state out) but has more integration points with the UI (animation state, preview panel). Absorbs circuitSimulator entirely (RuntimeSession, DataFlowAnimationController, DataFlowDebugger).
4. **flowGraph-generate** (fourth): Code generation depends on persist (for .flow.kts output) and potentially inspect (for node definitions). Absorbs kotlinCompiler entirely (25 generators, 8 templates, validator). Extract after its dependencies are stable.
5. **flowGraph-compose** (last): Graph composition is the most tightly coupled to the UI. Many ViewModels directly manage composition state. Extract last when all other slices are stable.

**Rationale**: Each extraction step leaves the application fully functional because earlier extractions have fewer consumers. The composition root (graphEditor) delegates to extracted modules one at a time, keeping the surface area of change small.

**Alternatives considered**:
- **Extract compose first** (most code): Would require the most interface changes upfront and risk destabilizing the UI. Rejected.
- **Extract all at once**: Big-bang migration. No intermediate safety. Contradicts Strangler Fig pattern. Rejected.
- **Extract by directory**: Directories don't align with module boundaries (e.g., `state/` spans multiple slices). Rejected.

---

## R6: Meta-FlowGraph Representation

**Decision**: Create a `.flow.kts` file using the existing FlowGraph DSL where each vertical-slice module is a CodeNode with ports representing its public API. Connections show data flow between modules.

**Node design**:
- Each module → one CodeNode with descriptive name
- Input ports → data the module consumes (e.g., flowGraph-persist has input port "FlowGraph")
- Output ports → data the module produces (e.g., flowGraph-persist has output port "FilePath")
- fbpDsl → represented as the shared IP type vocabulary, not a node

**Rationale**: Using the project's own `.flow.kts` format means the architecture diagram can be opened and viewed in the graphEditor itself. This dogfoods the FBP paradigm and creates living documentation that evolves with the architecture.

**Alternatives considered**:
- **Mermaid diagram in Markdown**: Not openable in the graphEditor. Doesn't dogfood the paradigm. Rejected.
- **Separate diagramming tool**: External dependency, not version-controllable in the same way. Rejected.
