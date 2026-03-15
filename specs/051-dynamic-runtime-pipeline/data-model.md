# Data Model: Dynamic Runtime Pipeline

## Entities

### DynamicPipeline

A runtime-constructed pipeline built from a FlowGraph snapshot.

| Field | Type | Description |
|-------|------|-------------|
| flowGraph | FlowGraph | The FlowGraph snapshot used to build this pipeline |
| runtimes | Map<String, NodeRuntime> | Node instance ID → created NodeRuntime |
| channels | List<Channel<Any>> | All created channels (for cleanup) |
| scope | CoroutineScope | The coroutine scope running the pipeline |

**Lifecycle states**: IDLE → RUNNING → PAUSED → IDLE (via stop)

**Operations**:
- `start(scope)`: Start all runtimes in topological order (sources first, then downstream)
- `stop()`: Stop all runtimes, close all channels, cancel scope
- `pause()`: Pause all runtimes via registry
- `resume()`: Resume all runtimes via registry

### PipelineValidationResult

The outcome of pre-start validation.

| Field | Type | Description |
|-------|------|-------------|
| isValid | Boolean | Whether the pipeline can be built |
| errors | List<ValidationError> | List of specific errors found |

### ValidationError

A single validation error.

| Field | Type | Description |
|-------|------|-------------|
| type | ErrorType | UNRESOLVABLE_NODE, INVALID_PORT, CYCLE_DETECTED |
| nodeId | String? | The node instance ID involved (if applicable) |
| nodeName | String? | The node name involved (if applicable) |
| message | String | Human-readable error description |

## Relationships

```text
FlowGraph (input)
    │
    ├── CodeNode[] ──lookup──> CodeNodeDefinition (from registry)
    │                              │
    │                              └── createRuntime() ──> NodeRuntime
    │
    └── Connection[] ──wire──> Channel<Any>[]
                                   │
                                   └── assigned to NodeRuntime input/output properties

DynamicPipelineController
    ├── owns DynamicPipeline
    ├── implements ModuleController
    ├── manages RuntimeRegistry (for pause/resume propagation)
    └── exposes executionState: StateFlow<ExecutionState>
```

## State Transitions

```text
DynamicPipelineController states:

  IDLE ──start()──> [validate FlowGraph]
                        │
                        ├── valid ──> [build pipeline] ──> RUNNING
                        │
                        └── invalid ──> [emit errors] ──> IDLE (with error message)

  RUNNING ──pause()──> PAUSED
  RUNNING ──stop()──> [cleanup pipeline] ──> IDLE
  PAUSED ──resume()──> RUNNING
  PAUSED ──stop()──> [cleanup pipeline] ──> IDLE
```

## Port Index Mapping

Runtime channel properties follow a positional convention:

| Port Index | Input Property | Output Property |
|------------|---------------|-----------------|
| 0 | `inputChannel` | `outputChannel` (Source: `outputChannel1`) |
| 1 | `inputChannel2` (or `inputChannel1` for In2*) | `outputChannel2` |
| 2 | `inputChannel3` | `outputChannel3` |

Note: `SourceOut2Runtime` uses `outputChannel1`/`outputChannel2` (1-indexed).
`In2Out1Runtime` uses `inputChannel1`/`inputChannel2` (1-indexed).
`TransformerRuntime` uses `inputChannel`/`outputChannel` (no index).
`SinkRuntime` uses `inputChannel` (no index).

The builder must handle these naming inconsistencies based on the runtime type.
