# Quickstart: Unified FlowGraph Execution Control

**Feature**: 019-flowgraph-execution-control
**Date**: 2026-02-17

## Overview

This guide helps developers implement unified execution control for FlowGraph, enabling pause/resume functionality that routes through RootControlNode and propagates to all registered NodeRuntime instances.

## Prerequisites

- Kotlin 2.1.21 or later
- Compose Multiplatform 1.7.3
- kotlinx-coroutines 1.8.0
- Existing fbpDsl module with NodeRuntime classes
- Existing StopWatch module with StopWatchController

## Key Files to Understand

Before implementing, review these existing files:

| File | Why Review |
|------|------------|
| `fbpDsl/src/commonMain/.../runtime/NodeRuntime.kt` | Base runtime class with existing pause()/resume() |
| `fbpDsl/src/commonMain/.../runtime/TransformerRuntime.kt` | Reference implementation with pause hook |
| `fbpDsl/src/commonMain/.../model/RootControlNode.kt` | Central controller for FlowGraph state |
| `StopWatch/src/commonMain/.../StopWatchController.kt` | Current controller implementation |
| `KMPMobileApp/src/commonMain/.../StopWatch.kt` | Current UI with Start/Stop buttons |

## Implementation Steps

### Step 1: Create RuntimeRegistry

Create `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/RuntimeRegistry.kt`:

```kotlin
package io.codenode.fbpdsl.runtime

import java.util.concurrent.ConcurrentHashMap

/**
 * Registry that tracks active NodeRuntime instances for a flow.
 * Enables centralized pause/resume control through RootControlNode.
 */
class RuntimeRegistry {

    private val runtimes = ConcurrentHashMap<String, NodeRuntime<*>>()

    /**
     * Register a runtime when it starts.
     */
    fun register(runtime: NodeRuntime<*>) {
        runtimes[runtime.codeNode.id] = runtime
    }

    /**
     * Unregister a runtime when it stops.
     */
    fun unregister(runtime: NodeRuntime<*>) {
        runtimes.remove(runtime.codeNode.id)
    }

    /**
     * Pause all registered runtimes.
     */
    fun pauseAll() {
        runtimes.values.forEach { it.pause() }
    }

    /**
     * Resume all registered runtimes.
     */
    fun resumeAll() {
        runtimes.values.forEach { it.resume() }
    }

    /**
     * Stop all registered runtimes and clear registry.
     */
    fun stopAll() {
        runtimes.values.forEach { it.stop() }
        runtimes.clear()
    }

    /**
     * Number of registered runtimes.
     */
    val count: Int get() = runtimes.size
}
```

### Step 2: Add Pause Hook to Processing Loops

Add this pattern to all runtime processing loops that don't have it:

```kotlin
// In GeneratorRuntime, SinkRuntime, Out2GeneratorRuntime, etc.
nodeControlJob = scope.launch {
    try {
        while (executionState != ExecutionState.IDLE) {
            // PAUSE HOOK - Add at start of each iteration
            while (executionState == ExecutionState.PAUSED) {
                delay(10)
            }
            if (executionState == ExecutionState.IDLE) break

            // ... existing processing logic ...
        }
    } finally {
        // ... cleanup ...
    }
}
```

### Step 3: Update RootControlNode

Modify `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/RootControlNode.kt`:

```kotlin
class RootControlNode private constructor(
    private val flowGraph: FlowGraph,
    private val name: String,
    private val registry: RuntimeRegistry? = null
) {
    // ... existing code ...

    fun pauseAll(): FlowGraph {
        val updatedGraph = // ... existing model update ...
        registry?.pauseAll()
        return updatedGraph
    }

    fun resumeAll(): FlowGraph {
        val updatedGraph = flowGraph.withRootNodes(
            flowGraph.rootNodes.map { it.withExecutionState(ExecutionState.RUNNING, propagate = true) }
        )
        registry?.resumeAll()
        return updatedGraph
    }

    companion object {
        fun createFor(
            flowGraph: FlowGraph,
            name: String,
            registry: RuntimeRegistry? = null
        ): RootControlNode = RootControlNode(flowGraph, name, registry)
    }
}
```

### Step 4: Update StopWatchController

Modify to use RootControlNode with registry:

```kotlin
class StopWatchController(private var flowGraph: FlowGraph) {
    private val registry = RuntimeRegistry()
    private var controller = RootControlNode.createFor(flowGraph, "StopWatchController", registry)

    fun pause(): FlowGraph {
        flowGraph = controller.pauseAll()
        controller = RootControlNode.createFor(flowGraph, "StopWatchController", registry)
        _executionState.value = ExecutionState.PAUSED
        return flowGraph
    }

    fun resume(): FlowGraph {
        flowGraph = controller.resumeAll()
        controller = RootControlNode.createFor(flowGraph, "StopWatchController", registry)
        _executionState.value = ExecutionState.RUNNING
        return flowGraph
    }
}
```

### Step 5: Update ViewModel and UI

Add pause/resume to StopWatchViewModel:

```kotlin
fun pause(): FlowGraph = controller.pause()
fun resume(): FlowGraph = controller.resume()
```

Update StopWatch.kt composable:

```kotlin
@Composable
fun StopWatch(viewModel: StopWatchViewModel, ...) {
    val executionState by viewModel.executionState.collectAsState()

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Start/Stop button
        Button(
            onClick = {
                if (executionState == ExecutionState.RUNNING) {
                    viewModel.stop()
                } else if (executionState == ExecutionState.IDLE) {
                    viewModel.start()
                }
            },
            enabled = executionState != ExecutionState.PAUSED
        ) {
            Text(if (executionState == ExecutionState.RUNNING) "Stop" else "Start")
        }

        // Pause/Resume button - only visible when RUNNING or PAUSED
        if (executionState == ExecutionState.RUNNING || executionState == ExecutionState.PAUSED) {
            Button(
                onClick = {
                    if (executionState == ExecutionState.RUNNING) {
                        viewModel.pause()
                    } else {
                        viewModel.resume()
                    }
                }
            ) {
                Text(if (executionState == ExecutionState.PAUSED) "Resume" else "Pause")
            }
        }

        // Reset button
        Button(
            onClick = { viewModel.reset() },
            enabled = executionState != ExecutionState.RUNNING
        ) {
            Text("Reset")
        }
    }
}
```

## Verification

Run the following commands to verify the implementation:

```bash
# Run RuntimeRegistry tests
./gradlew :fbpDsl:jvmTest --tests "*RuntimeRegistryTest*"

# Run pause/resume integration tests
./gradlew :fbpDsl:jvmTest --tests "*PauseResumeTest*"

# Run StopWatch tests
./gradlew :StopWatch:jvmTest

# Run ViewModel tests
./gradlew :KMPMobileApp:testDebugUnitTest

# Build for Android
./gradlew :KMPMobileApp:assembleDebug
```

## Validation Checklist

- [ ] RuntimeRegistry created in fbpDsl/runtime/
- [ ] RuntimeRegistry has register, unregister, pauseAll, resumeAll, stopAll methods
- [ ] RuntimeRegistry is thread-safe (uses ConcurrentHashMap)
- [ ] RootControlNode updated with resumeAll() method
- [ ] RootControlNode accepts optional RuntimeRegistry parameter
- [ ] All *Runtime classes have pause hook in processing loop
- [ ] GeneratorRuntime has pause hook
- [ ] SinkRuntime has pause hook
- [ ] Out2GeneratorRuntime has pause hook
- [ ] In2SinkRuntime has pause hook
- [ ] StopWatchController uses RootControlNode with registry
- [ ] StopWatchController has pause() and resume() methods
- [ ] StopWatchViewModel has pause() and resume() methods
- [ ] StopWatch composable has Pause/Resume button
- [ ] Pause/Resume button visibility based on executionState
- [ ] Unit tests pass for RuntimeRegistry
- [ ] Integration tests pass for pause/resume flow
- [ ] App runs identically to before with new pause/resume feature
