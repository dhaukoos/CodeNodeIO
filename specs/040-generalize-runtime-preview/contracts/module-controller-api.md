# Contract: ModuleController Interface

**Location**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ModuleController.kt`

## Purpose

Common interface for all generated module controllers, enabling RuntimeSession to work with any module without knowing its concrete type.

## Interface Definition

```kotlin
interface ModuleController {
    val executionState: StateFlow<ExecutionState>
    fun start(): FlowGraph
    fun stop(): FlowGraph
    fun pause(): FlowGraph
    fun resume(): FlowGraph
    fun reset(): FlowGraph
    fun setAttenuationDelay(ms: Long?)
}
```

## Properties

| Property | Type | Description |
|----------|------|-------------|
| `executionState` | `StateFlow<ExecutionState>` | Current execution state: IDLE, RUNNING, PAUSED, ERROR |

## Methods

| Method | Parameters | Returns | Description |
|--------|-----------|---------|-------------|
| `start()` | — | `FlowGraph` | Start flow execution |
| `stop()` | — | `FlowGraph` | Stop execution, reset to IDLE |
| `pause()` | — | `FlowGraph` | Pause execution |
| `resume()` | — | `FlowGraph` | Resume from paused state |
| `reset()` | — | `FlowGraph` | Reset all state to initial values |
| `setAttenuationDelay(ms)` | `ms: Long?` | — | Set speed attenuation delay on all runtime nodes |

## Implementors

All generated controllers must implement this interface:
- `StopWatchController : ModuleController`
- `UserProfilesController : ModuleController`
- Any future generated controller

## Usage by RuntimeSession

```kotlin
class RuntimeSession(
    private val controller: ModuleController,
    val viewModel: Any
)
```

RuntimeSession delegates all lifecycle operations to the ModuleController. The `viewModel` is stored as `Any` and cast by preview providers that know the concrete type.
