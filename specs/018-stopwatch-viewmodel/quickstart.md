# Quickstart: StopWatch ViewModel Pattern

**Feature**: 018-stopwatch-viewmodel
**Date**: 2026-02-16

## Overview

This guide helps developers implement the ViewModel pattern for the StopWatch module, bridging the FlowGraph domain logic with Compose UI in the KMPMobileApp.

## Prerequisites

- Kotlin 2.1.21 or later
- Compose Multiplatform 1.7.3
- kotlinx-coroutines 1.8.0
- lifecycle-viewmodel-compose 2.8.0 (JetBrains multiplatform ViewModel)
- Existing StopWatch module with StopWatchController

## New Dependency

Add to `KMPMobileApp/build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            // ... existing dependencies ...
            // JetBrains Multiplatform ViewModel
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
        }

        commonTest.dependencies {
            // ... existing dependencies ...
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
        }
    }
}
```

## Key Files to Understand

Before implementing, review these existing files:

| File | Why Review |
|------|------------|
| `StopWatch/src/commonMain/kotlin/.../StopWatchController.kt` | Controller providing StateFlow state |
| `KMPMobileApp/src/commonMain/kotlin/.../StopWatch.kt` | Current composable using controller directly |
| `KMPMobileApp/src/commonMain/kotlin/.../StopWatchFace.kt` | Pure rendering composable (unchanged) |
| `KMPMobileApp/src/commonMain/kotlin/.../App.kt` | Composition root |
| `graphEditor/src/jvmMain/kotlin/viewmodel/` | Reference ViewModel pattern |

## Implementation Steps

### Step 1: Create StopWatchViewModel

Create `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/viewmodel/StopWatchViewModel.kt`:

```kotlin
package io.codenode.mobileapp.viewmodel

import androidx.lifecycle.ViewModel
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.stopwatch.generated.StopWatchController
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for the StopWatch composable.
 * Bridges FlowGraph domain logic with Compose UI.
 *
 * @param controller The StopWatchController that manages FlowGraph execution
 */
class StopWatchViewModel(
    private val controller: StopWatchController
) : ViewModel() {

    /**
     * Current elapsed seconds (0-59).
     * Delegated from controller's StateFlow.
     */
    val elapsedSeconds: StateFlow<Int> = controller.elapsedSeconds

    /**
     * Current elapsed minutes.
     * Delegated from controller's StateFlow.
     */
    val elapsedMinutes: StateFlow<Int> = controller.elapsedMinutes

    /**
     * Current execution state (IDLE, RUNNING, PAUSED).
     * Delegated from controller's StateFlow.
     */
    val executionState: StateFlow<ExecutionState> = controller.executionState

    /**
     * Starts the stopwatch.
     * @return Updated FlowGraph
     */
    fun start(): FlowGraph = controller.start()

    /**
     * Stops the stopwatch.
     * @return Updated FlowGraph
     */
    fun stop(): FlowGraph = controller.stop()

    /**
     * Resets the stopwatch to initial state.
     * @return Updated FlowGraph
     */
    fun reset(): FlowGraph = controller.reset()
}
```

### Step 2: Update StopWatch Composable

Modify `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatch.kt`:

```kotlin
package io.codenode.mobileapp

import androidx.compose.runtime.*
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.mobileapp.viewmodel.StopWatchViewModel
// ... other imports ...

/**
 * StopWatch composable using ViewModel pattern.
 *
 * @param viewModel The StopWatchViewModel instance
 * @param modifier Modifier for the composable
 * @param minSize Minimum size for the clock face
 */
@Composable
fun StopWatch(
    viewModel: StopWatchViewModel,
    modifier: Modifier = Modifier,
    minSize: Dp = 200.dp
) {
    // Collect state from ViewModel's StateFlow properties
    val executionState by viewModel.executionState.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val elapsedMinutes by viewModel.elapsedMinutes.collectAsState()

    // Derive isRunning from executionState
    val isRunning = executionState == ExecutionState.RUNNING

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StopWatchFace(
            minSize = minSize,
            seconds = elapsedSeconds,
            minutes = elapsedMinutes,
            isRunning = isRunning
        )

        // ... rest of UI ...

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {
                    if (isRunning) viewModel.stop() else viewModel.start()
                }
            ) {
                Text(if (isRunning) "Stop" else "Start")
            }

            Button(
                onClick = { viewModel.reset() },
                enabled = !isRunning && (elapsedSeconds > 0 || elapsedMinutes > 0)
            ) {
                Text("Reset")
            }
        }
    }
}
```

### Step 3: Create ViewModel in App.kt

Update `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/App.kt`:

```kotlin
@Composable
fun App() {
    // Create FlowGraph and controller (existing code)
    val flowGraph = remember { ... }
    val controller = remember { StopWatchController(flowGraph) }

    // Create ViewModel wrapping controller
    val stopWatchViewModel = remember { StopWatchViewModel(controller) }

    MaterialTheme {
        StopWatch(viewModel = stopWatchViewModel)
    }
}
```

### Step 4: Write Unit Tests

Create `KMPMobileApp/src/commonTest/kotlin/io/codenode/mobileapp/viewmodel/StopWatchViewModelTest.kt`:

```kotlin
package io.codenode.mobileapp.viewmodel

import io.codenode.fbpdsl.model.ExecutionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StopWatchViewModelTest {

    @Test
    fun `initial state is IDLE with zero elapsed time`() = runTest {
        val controller = FakeStopWatchController()
        val viewModel = StopWatchViewModel(controller)

        assertEquals(0, viewModel.elapsedSeconds.first())
        assertEquals(0, viewModel.elapsedMinutes.first())
        assertEquals(ExecutionState.IDLE, viewModel.executionState.first())
    }

    @Test
    fun `start delegates to controller`() = runTest {
        val controller = FakeStopWatchController()
        val viewModel = StopWatchViewModel(controller)

        viewModel.start()

        assertTrue(controller.startCalled)
    }

    @Test
    fun `stop delegates to controller`() = runTest {
        val controller = FakeStopWatchController()
        val viewModel = StopWatchViewModel(controller)

        viewModel.stop()

        assertTrue(controller.stopCalled)
    }

    @Test
    fun `reset delegates to controller`() = runTest {
        val controller = FakeStopWatchController()
        val viewModel = StopWatchViewModel(controller)

        viewModel.reset()

        assertTrue(controller.resetCalled)
    }
}

// Fake controller for testing
class FakeStopWatchController {
    val elapsedSeconds = MutableStateFlow(0)
    val elapsedMinutes = MutableStateFlow(0)
    val executionState = MutableStateFlow(ExecutionState.IDLE)

    var startCalled = false
    var stopCalled = false
    var resetCalled = false

    fun start() { startCalled = true }
    fun stop() { stopCalled = true }
    fun reset() { resetCalled = true }
}
```

## Verification

Run the following commands to verify the implementation:

```bash
# Run ViewModel tests
./gradlew :KMPMobileApp:test

# Build for Android
./gradlew :KMPMobileApp:assembleDebug

# Build for iOS (requires macOS)
./gradlew :KMPMobileApp:linkDebugFrameworkIosSimulatorArm64
```

## Validation Checklist

- [x] StopWatchViewModel created in `viewmodel/` package
- [x] ViewModel extends JetBrains ViewModel class
- [x] State exposed as StateFlow (not MutableStateFlow)
- [x] Actions delegate to StopWatchController
- [x] StopWatch composable uses collectAsState()
- [x] StopWatch composable has no direct controller access
- [x] Unit tests pass without Compose UI dependencies
- [x] App runs identically to before refactoring
- [x] Works on Android, iOS simulator, and Desktop
