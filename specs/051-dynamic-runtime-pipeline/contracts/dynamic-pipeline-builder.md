# Contract: DynamicPipelineBuilder

## Purpose

Builds a runnable pipeline from a FlowGraph by resolving node names to CodeNodeDefinitions, creating runtimes, and wiring channels.

## Interface

### `validate(flowGraph: FlowGraph, registry: NodeDefinitionRegistry): PipelineValidationResult`

**Pre-conditions**:
- `flowGraph` is non-null with at least one node
- `registry` has been populated via `discoverAll()` + any manual registrations

**Post-conditions**:
- Returns `PipelineValidationResult.valid()` if all nodes resolvable, all connection ports valid, no cycles
- Returns `PipelineValidationResult.errors(list)` with specific error details otherwise
- Does NOT modify any state

### `build(flowGraph: FlowGraph, registry: NodeDefinitionRegistry): DynamicPipeline`

**Pre-conditions**:
- `validate()` has returned valid (caller's responsibility; build throws on invalid input)

**Post-conditions**:
- Returns a `DynamicPipeline` with:
  - One `NodeRuntime` per CodeNode in the FlowGraph (resolved via registry)
  - Channels wired according to FlowGraph connections
  - All runtimes in IDLE state (not yet started)
- FlowGraph and registry are not modified

### `canBuildDynamic(flowGraph: FlowGraph, registry: NodeDefinitionRegistry): Boolean`

**Pre-conditions**: None

**Post-conditions**:
- Returns `true` if every CodeNode name in the FlowGraph has a CodeNodeDefinition in the registry
- Returns `false` if any node name is missing

---

# Contract: DynamicPipelineController

## Purpose

Implements `ModuleController` for dynamically-built pipelines. Manages lifecycle, attenuation, and observers.

## Interface

Implements all `ModuleController` methods:

### `start(): FlowGraph`

**Pre-conditions**: Controller is in IDLE state

**Post-conditions**:
- Calls `DynamicPipelineBuilder.validate()` then `build()`
- Starts all runtimes in topological order (sources first)
- Sets execution state to RUNNING
- Returns the FlowGraph

**Error behavior**: If validation fails, sets execution state back to IDLE and surfaces errors via a callback/state

### `stop(): FlowGraph`

**Pre-conditions**: Controller is in RUNNING or PAUSED state

**Post-conditions**:
- Stops all runtimes
- Closes all channels
- Cancels coroutine scope
- Sets execution state to IDLE

### `pause(): FlowGraph`

**Post-conditions**: All runtimes paused via RuntimeRegistry; execution state PAUSED

### `resume(): FlowGraph`

**Post-conditions**: All runtimes resumed via RuntimeRegistry; execution state RUNNING

### `setAttenuationDelay(ms: Long?)`

**Post-conditions**: Attenuation applied to all runtimes in the current pipeline

### `setEmissionObserver(observer)` / `setValueObserver(observer)`

**Post-conditions**: Observers applied to all runtimes; re-applied on each `start()`
