# Research: Dynamic Runtime Pipeline

## R1: FlowGraph-to-Runtime Mapping Strategy

**Decision**: Read the editor's FlowGraph directly — iterate `getAllCodeNodes()` for nodes and `connections` for wiring. Map node names to `CodeNodeDefinition` via the registry, not node IDs.

**Rationale**: The FlowGraph stores nodes by `id` (unique per instance) and `name` (human-readable, matches CodeNodeDefinition.name). Connections reference `sourceNodeId`/`targetNodeId` (node instance IDs) and `sourcePortId`/`targetPortId` (port IDs). The port IDs on the canvas correspond to `Port.id`, which follows the naming pattern used when nodes are instantiated from `NodeTypeDefinition` port templates. The CodeNodeDefinition's `inputPorts`/`outputPorts` use `PortSpec.name` which matches the port template names.

**Alternatives considered**:
- Parse the `.flow.kts` file → rejected: file may not be saved; editor FlowGraph is the live truth.
- Use node IDs for registry lookup → rejected: IDs are instance-specific; names match CodeNodeDefinition.name.

## R2: Channel Wiring Approach

**Decision**: For each connection in the FlowGraph, create a `Channel<Any>(Channel.BUFFERED)` and assign it to the appropriate input/output properties on the resolved `NodeRuntime` instances. Use the same wiring patterns as `EdgeArtFilterFlow.wireConnections()`.

**Rationale**: The existing runtime types (`TransformerRuntime`, `SinkRuntime`, `SourceOut2Runtime`, `In2Out1Runtime`, etc.) expose typed channel properties (`inputChannel`, `outputChannel`, `inputChannel1`, `inputChannel2`, `outputChannel1`, `outputChannel2`). The dynamic builder must map port indices to these properties. Port index 0 → the base property (e.g., `inputChannel`), port index 1 → `inputChannel2`, etc.

**Key patterns to support**:
- 1-to-1: `TransformerRuntime.inputChannel = channel; TransformerRuntime.outputChannel = channel`
- Fan-out: `SourceOut2Runtime.outputChannel1` and `outputChannel2` are separate channels wired to different downstream nodes
- Fan-in: `In2Out1Runtime.inputChannel1` and `inputChannel2` from different upstream nodes
- Intermediate channels: When a `SendChannel` output connects to a `Channel` input, create an intermediate `Channel<Any>(Channel.BUFFERED)` (same as `EdgeArtFilterFlow.wireConnections()` pattern)

**Alternatives considered**:
- Type-safe generic channels → rejected: runtime types use `Any` erasure at the channel level; type safety is enforced by the processing logic, not the channel.

## R3: Runtime Type Resolution

**Decision**: `CodeNodeDefinition.createRuntime(name)` returns `NodeRuntime`. The dynamic builder casts to the specific runtime subclass based on the `NodeCategory` and port count to access channel properties.

**Rationale**: The runtime hierarchy is:
- SOURCE (0 in, 1 out) → `SourceRuntime`
- SOURCE (0 in, 2 out) → `SourceOut2Runtime`
- SOURCE (0 in, 3 out) → `SourceOut3Runtime`
- TRANSFORMER (1 in, 1 out) → `TransformerRuntime`
- PROCESSOR (2 in, 1 out) → `In2Out1Runtime`
- PROCESSOR (1 in, 2 out) → `In1Out2Runtime`
- SINK (1 in, 0 out) → `SinkRuntime`
- SINK (2 in, 0 out) → `In2SinkRuntime`
- (etc. for all InXOutY combinations)

The category + port count uniquely identifies the runtime subclass. The builder uses this to know which channel properties exist.

**Alternatives considered**:
- Reflection to discover channel properties → rejected: fragile, platform-dependent
- A new `WirableRuntime` interface → possible future improvement but over-engineering for now; direct casting works with known types

## R4: Fallback Detection

**Decision**: Before building a dynamic pipeline, check if all canvas nodes have CodeNodeDefinition entries in the registry. If any node lacks coverage, fall back to the existing `ModuleSessionFactory.createSession()` path.

**Rationale**: Existing modules like StopWatch have specialized controllers with timer logic, DAO injection (UserProfiles), etc. that can't be replicated by generic dynamic wiring. The fallback check is simple: iterate `flowGraph.getAllCodeNodes()`, call `registry.getByName(node.name)` for each, and if any returns null → use generated controller.

**Implementation**: Add a `canBuildDynamicPipeline(flowGraph, registry)` check in `ModuleSessionFactory`. If true, create a `DynamicPipelineController`. If false, use existing per-module factory methods.

## R5: ViewModel Resolution for Dynamic Pipelines

**Decision**: For modules using the dynamic pipeline, the ViewModel must still be created per-module since ViewModels have module-specific state (e.g., `EdgeArtFilterViewModel` observes `EdgeArtFilterState`). The `ModuleSessionFactory` continues to create the ViewModel; only the controller is replaced.

**Rationale**: The ViewModel is passed to `RuntimeSession` as `Any` and cast by preview providers. The dynamic pipeline doesn't change how ViewModels work — it only changes how runtimes are created and wired. Module-specific state objects (like `EdgeArtFilterState`) are used by both the CodeNodeDefinitions' processing logic and the ViewModel, so they continue to work unchanged.

**Alternatives considered**:
- Generic ViewModel → rejected: each module has unique UI state requirements
- No ViewModel → rejected: RuntimeSession.viewModel is required for preview rendering

## R6: Port ID Mapping

**Decision**: Map FlowGraph port IDs to runtime channel properties using positional index derived from the port ordering in the CodeNodeDefinition.

**Rationale**: When nodes are placed on the canvas from a `NodeTypeDefinition`, ports are created from `portTemplates`. The port IDs follow patterns like `{nodeId}_input_0`, `{nodeId}_output_0`, etc. The index portion maps directly to the channel property naming: index 0 → base channel (`inputChannel`/`outputChannel`), index 1 → `inputChannel2`/`outputChannel2`, etc.

The mapping function: extract the port index from the port ID, then use it to get/set the corresponding channel property on the runtime.
