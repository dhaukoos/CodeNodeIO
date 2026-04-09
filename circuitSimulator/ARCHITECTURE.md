# CircuitSimulator Architecture Audit

**Feature**: 064-vertical-slice-refactor
**Date**: 2026-04-02
**Total Source Files**: 5 (circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/)

## Responsibility Buckets

Six buckets classify every circuitSimulator source file by its primary user-facing workflow:

| Bucket | Target Module | Scope |
|--------|--------------|-------|
| **compose** | flowGraph-compose | Building a flow graph interactively: adding nodes from the palette, connecting ports, validating connections (port type checking, cycle detection), configuring node properties. Owns graph mutation logic — the path from user gesture to valid FlowGraph. |
| **persist** | flowGraph-persist | Saving and loading flow graphs: serialization to `.flow.kts` DSL, deserialization back to in-memory FlowGraph, filesystem I/O, reconciling deserialized state with editor state. Owns the round-trip workflow between memory and disk. |
| **execute** | flowGraph-execute | Running a flow graph and observing results: dynamic runtime pipeline, coroutine channel orchestration, execution control (start/stop/pause/resume/step), data flow animation state, runtime preview. Owns everything from "press Play" to "see results." |
| **generate** | flowGraph-generate | Producing deployable code from a graph: module save workflow, CodeNode definition codegen, runtime file generation, build configuration output. Owns the path from FlowGraph to generated source files on disk. |
| **inspect** | flowGraph-inspect | Understanding available components: node palette, IP type registry, filesystem scanner for node discovery, CodeNode source text editor, IP type file generation, debuggable data preview. Owns discovery and examination of what's available to compose with. |
| **root** | graphEditor (stays) | Composition root: Compose UI composables that render the editor, ViewModels that wire slices together, DI/wiring, application entry point. No business logic — only presentation and orchestration. |

## Assignment Methodology

Each file is assigned to exactly one bucket using a multi-signal approach, evaluated in priority order:

1. **Primary type operated on**: What domain object does this file's core logic manipulate? FlowGraph structure → compose. File I/O → persist. Coroutine channels/execution state → execute. Generated source code → generate. Node definitions/registry → inspect.

2. **Import analysis**: The set of imports reveals domain affinity:
   - `io.codenode.fbpdsl.serialization.*` → persist
   - `io.codenode.fbpdsl.runtime.*` → execute
   - `io.codenode.flowgraphgenerate.*` → generate
   - Registry/discovery types → inspect
   - `io.codenode.fbpdsl.model.FlowGraph`, `Node`, `Connection` (mutations) → compose

3. **Cross-reference density**: If a file has cross-references to multiple buckets, it belongs to the bucket with the highest affinity (most references). Ties are broken by which workflow the file is *called from* most often.

## File Audit

### circuitsimulator/ (5 files)

| File | Bucket | Primary Responsibility | Cross-Module Dependencies |
|------|--------|----------------------|--------------------------|
| CircuitSimulator.kt | execute | Stub for graph execution orchestration (placeholder phase 1 implementation). Holds FlowGraph reference and provides `validate()` and `execute()` entry points. | fbpDsl (FlowGraph, InformationPacket) |
| ConnectionAnimation.kt | execute | Value object modeling a single animated dot on a connection. Computes animation progress and completion state using linear interpolation over system time. Pure data model. | None |
| DataFlowAnimationController.kt | execute | Runtime animation choreography: creates emission observer callbacks, maintains StateFlow of active animations, runs 60fps frame loop to prune completed animations, supports pause/resume with timestamp adjustments. Maps (nodeId, portIndex) pairs to connection IDs via pre-computed lookup table. | fbpDsl (FlowGraph); kotlinx-coroutines (CoroutineScope, StateFlow, Job, delay) |
| DataFlowDebugger.kt | execute | Per-connection value snapshot capture for runtime debugging. Creates value observer callbacks and exposes latest transit value on each connection via StateFlow. Uses identical node-port-to-connection mapping pattern as DataFlowAnimationController. | fbpDsl (FlowGraph); kotlinx-coroutines (StateFlow) |
| RuntimeSession.kt | execute | Composition orchestrator for module lifecycle: manages state transitions (IDLE→RUNNING→PAUSED), attenuation delay propagation, observer wiring for animation and debugging. Owns DataFlowAnimationController and DataFlowDebugger instances. Supports both static and dynamic (canvas-editable) FlowGraphs via optional provider. Adapts for pre-started controllers (event-driven modules). | fbpDsl (ExecutionState, FlowGraph); fbpDsl runtime (DynamicPipelineController, ModuleController); kotlinx-coroutines (CoroutineScope, StateFlow, SupervisorJob, Dispatchers) |

## Seam Matrix

### Internal Seams (within circuitSimulator)

| Source File | Target File | Type | Notes |
|------------|------------|------|-------|
| RuntimeSession.kt | DataFlowAnimationController.kt | Function call | Creates instance, calls createEmissionObserver(), startFrameLoop(), stopFrameLoop(), pauseAnimations(), resumeAnimations() |
| RuntimeSession.kt | DataFlowDebugger.kt | Function call | Creates instance, calls createValueObserver() |
| DataFlowAnimationController.kt | ConnectionAnimation.kt | Type reference | Creates and manages ConnectionAnimation instances |

### Cross-Module Seams

| Source File | Target Module | Type | Dependency |
|------------|--------------|------|-----------|
| CircuitSimulator.kt | fbpDsl | Type reference | FlowGraph, InformationPacket |
| DataFlowAnimationController.kt | fbpDsl | Type reference | FlowGraph (for building port-to-connection map) |
| DataFlowDebugger.kt | fbpDsl | Type reference | FlowGraph (for building port-to-connection map) |
| RuntimeSession.kt | fbpDsl | Type reference | ExecutionState, FlowGraph |
| RuntimeSession.kt | fbpDsl | Type reference | ModuleController, DynamicPipelineController (runtime interfaces) |

**Note**: circuitSimulator has **no imports from graphEditor or kotlinCompiler**. It is consumed by graphEditor (via RuntimePreviewPanel/ModuleSessionFactory) but does not depend on either. This confirms circuitSimulator maps cleanly to flowGraph-execute.

**Inbound seams** (from graphEditor into circuitSimulator — documented in graphEditor/ARCHITECTURE.md):
- graphEditor/ui/RuntimePreviewPanel.kt → RuntimeSession (execute→execute, stays within flowGraph-execute)
- graphEditor/ui/ModuleSessionFactory.kt → RuntimeSession (execute→execute, stays within flowGraph-execute)

### Seam Count by Boundary

| Boundary | Count | Notes |
|----------|-------|-------|
| execute→fbpDsl (type reference) | 5 | FlowGraph, ExecutionState, ModuleController, DynamicPipelineController, InformationPacket — shared vocabulary |
| (internal execute→execute) | 3 | RuntimeSession owns AnimationController and Debugger |
| **Total cross-module** | **0** | No cross-module seams to graphEditor or kotlinCompiler |

## Summary

### Files Per Bucket

| Bucket | Count | Percentage |
|--------|-------|-----------|
| **execute** | 5 | 100% |
| **Total** | **5** | **100%** |

### Key Observations

1. **circuitSimulator is a pure execute module** — Every file maps to the execute bucket. This confirms the plan's assessment that circuitSimulator dissolves entirely into flowGraph-execute.

2. **No cross-bucket complexity** — Like kotlinCompiler, circuitSimulator is monolithic in its responsibility. Extraction to flowGraph-execute is a clean module move.

3. **RuntimeSession is the orchestration hub** — It owns both DataFlowAnimationController and DataFlowDebugger, managing their lifecycle through the module's execution states. This is the primary integration seam for graphEditor consumers.

4. **Duplicate port-to-connection mapping logic** — Both DataFlowAnimationController and DataFlowDebugger implement identical `buildPortConnectionMap()` logic. A candidate for extraction to a shared utility within flowGraph-execute.

5. **fbpDsl is the only external dependency** — All files import fbpDsl types (FlowGraph, ExecutionState, ModuleController). No imports from graphEditor or kotlinCompiler.

6. **Debug/observe is a sub-concern** — DataFlowDebugger and DataFlowAnimationController together form the "Debug/observe" sub-concern (~600 lines) identified in research.md as a future extraction candidate if headless execution and visual debugging need to run independently.
