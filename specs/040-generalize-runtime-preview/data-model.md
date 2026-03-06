# Data Model: Generalize Runtime Preview

**Feature**: 040-generalize-runtime-preview
**Date**: 2026-03-06

## Entities

### ModuleController (Interface)

The common contract for all generated module controllers.

| Field | Type | Description |
|-------|------|-------------|
| `executionState` | `StateFlow<ExecutionState>` | Current execution state |

| Method | Signature | Description |
|--------|-----------|-------------|
| `start` | `(): FlowGraph` | Start flow execution |
| `stop` | `(): FlowGraph` | Stop and reset to IDLE |
| `pause` | `(): FlowGraph` | Pause execution |
| `resume` | `(): FlowGraph` | Resume from pause |
| `reset` | `(): FlowGraph` | Reset all state |
| `setAttenuationDelay` | `(ms: Long?)` | Set speed delay on all nodes |

**Implementors**: StopWatchController, UserProfilesController, all future generated controllers.

### PreviewRegistry (Singleton)

Maps composable names to preview rendering functions.

| Field | Type | Description |
|-------|------|-------------|
| `entries` | `Map<String, PreviewComposable>` | Internal map of name → preview function |

**PreviewComposable type**: `@Composable (viewModel: Any, modifier: Modifier) -> Unit`

### RuntimeSession (Refactored)

Module-agnostic orchestrator for flow execution.

| Field | Type | Description |
|-------|------|-------------|
| `controller` | `ModuleController` | Injected module controller |
| `viewModel` | `Any` | Module-specific ViewModel |
| `executionState` | `StateFlow<ExecutionState>` | Delegated from controller |
| `attenuationDelayMs` | `StateFlow<Long>` | Current attenuation (0-2000ms) |

## Relationships

```
ModuleController <|.. StopWatchController
ModuleController <|.. UserProfilesController
RuntimeSession --> ModuleController : delegates lifecycle
RuntimeSession --> Any(ViewModel) : holds reference
PreviewRegistry --> PreviewComposable : maps names to renderers
RuntimePreviewPanel --> RuntimeSession : uses for controls
RuntimePreviewPanel --> PreviewRegistry : looks up previews
```

## State Transitions

RuntimeSession execution states remain unchanged:

```
IDLE → RUNNING (start)
RUNNING → PAUSED (pause)
PAUSED → RUNNING (resume)
RUNNING → IDLE (stop)
PAUSED → IDLE (stop)
ANY → IDLE (module switch)
```
