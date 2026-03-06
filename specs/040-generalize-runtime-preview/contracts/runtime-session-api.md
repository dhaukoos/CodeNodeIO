# Contract: RuntimeSession API (Generalized)

**Location**: `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/RuntimeSession.kt`

## Purpose

Module-agnostic runtime session that manages flow execution lifecycle for any loaded module.

## Interface Definition

```kotlin
class RuntimeSession(
    private val controller: ModuleController,
    val viewModel: Any
) {
    val executionState: StateFlow<ExecutionState>
    val attenuationDelayMs: StateFlow<Long>

    fun start()
    fun stop()
    fun pause()
    fun resume()
    fun setAttenuation(ms: Long)
}
```

## Constructor Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `controller` | `ModuleController` | The module's controller implementing the common interface |
| `viewModel` | `Any` | The module's ViewModel, stored opaquely for preview providers |

## Properties

| Property | Type | Description |
|----------|------|-------------|
| `executionState` | `StateFlow<ExecutionState>` | Delegated from controller |
| `attenuationDelayMs` | `StateFlow<Long>` | Current attenuation delay (0-2000ms) |
| `viewModel` | `Any` | Module's ViewModel, cast by preview providers |

## Factory Functions

The graphEditor uses factory functions to create RuntimeSession for each module:

```kotlin
fun createStopWatchSession(): RuntimeSession {
    val controller = StopWatchController(stopWatchFlowGraph)
    val adapter = StopWatchControllerAdapter(controller)
    val viewModel = StopWatchViewModel(adapter)
    return RuntimeSession(controller, viewModel)
}

fun createUserProfilesSession(): RuntimeSession {
    val controller = UserProfilesController(userProfilesFlowGraph)
    controller.start()
    val adapter = UserProfilesControllerAdapter(controller)
    val viewModel = UserProfilesViewModel(adapter)
    return RuntimeSession(controller, viewModel)
}
```

## Changes from Current API

| Aspect | Before | After |
|--------|--------|-------|
| Constructor | No parameters (hardcoded StopWatch) | `(controller, viewModel)` |
| `viewModel` type | `StopWatchViewModel` | `Any` |
| Module coupling | Imports StopWatch classes | Imports only `ModuleController` from fbpDsl |
