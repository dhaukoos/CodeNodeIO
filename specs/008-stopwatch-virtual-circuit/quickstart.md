# Quickstart: StopWatch Virtual Circuit Demo

**Feature**: 008-stopwatch-virtual-circuit
**Date**: 2026-02-08

## Overview

This guide walks you through creating, generating, and integrating the StopWatch virtual circuit. By the end, you'll have a working stopwatch app powered by an FBP-based architecture.

---

## Prerequisites

- **Kotlin**: 2.1.21 (installed via project configuration)
- **IDE**: IntelliJ IDEA or Android Studio with Kotlin plugin
- **Platform Tools**: Android SDK (API 24+), Xcode (for iOS testing)
- **Project**: CodeNodeIO repository cloned and building successfully

Verify build:
```bash
./gradlew build
```

---

## Step 1: Create the FlowGraph

### Option A: Using graphEditor (Recommended)

1. Launch graphEditor:
   ```bash
   ./gradlew :graphEditor:run
   ```

2. Create a new FlowGraph:
   - Click **New** in the toolbar
   - Name: "StopWatch"

3. Add TimerEmitter node:
   - Drag a CodeNode from the palette
   - Name: "TimerEmitter"
   - Type: GENERATOR
   - Add output ports:
     - `elapsedSeconds` (Int)
     - `elapsedMinutes` (Int)

4. Add DisplayReceiver node:
   - Drag another CodeNode
   - Name: "DisplayReceiver"
   - Type: SINK
   - Add input ports:
     - `seconds` (Int)
     - `minutes` (Int)

5. Connect nodes:
   - Drag from `TimerEmitter.elapsedSeconds` to `DisplayReceiver.seconds`
   - Drag from `TimerEmitter.elapsedMinutes` to `DisplayReceiver.minutes`

6. Configure RootControlNode:
   - Select the FlowGraph (click background)
   - Set `speedAttenuation`: 1000 (milliseconds)

7. Save:
   - Click **Save**
   - Save to `demos/stopwatch/StopWatch.flow.kts`

### Option B: Manual DSL Creation

Create `demos/stopwatch/StopWatch.flow.kts`:

```kotlin
flowGraph("StopWatch") {
    version = "1.0.0"
    description = "Virtual circuit demo for stopwatch functionality"

    codeNode("TimerEmitter") {
        type = NodeType.GENERATOR
        outputPort("elapsedSeconds", Int::class)
        outputPort("elapsedMinutes", Int::class)
        controlConfig {
            speedAttenuation = 1000L
        }
    }

    codeNode("DisplayReceiver") {
        type = NodeType.SINK
        inputPort("seconds", Int::class)
        inputPort("minutes", Int::class)
    }

    connect("TimerEmitter.elapsedSeconds", "DisplayReceiver.seconds")
    connect("TimerEmitter.elapsedMinutes", "DisplayReceiver.minutes")

    targetPlatforms(ANDROID, IOS, DESKTOP)
}
```

---

## Step 2: Generate the KMP Module

### Using graphEditor

1. Open the StopWatch.flow.kts file
2. Click **Compile** in the toolbar
3. Select output directory: `StopWatch/` (project root)
4. Wait for generation to complete

### Using CLI (if available)

```bash
./gradlew :kotlinCompiler:run --args="generate demos/stopwatch/StopWatch.flow.kts StopWatch/"
```

### Verify Generated Structure

```
StopWatch/
├── build.gradle.kts
├── settings.gradle.kts
├── src/
│   ├── commonMain/kotlin/io/codenode/generated/stopwatch/
│   │   ├── StopWatchFlowGraph.kt
│   │   ├── StopWatchController.kt
│   │   ├── TimerEmitterComponent.kt
│   │   └── DisplayReceiverComponent.kt
│   ├── androidMain/kotlin/...
│   └── iosMain/kotlin/...
```

---

## Step 3: Integrate with KMPMobileApp

### Add Module to Project

1. Edit `settings.gradle.kts` (project root):
   ```kotlin
   include(":StopWatch")
   ```

2. Edit `KMPMobileApp/build.gradle.kts`:
   ```kotlin
   kotlin {
       sourceSets {
           commonMain.dependencies {
               implementation(project(":StopWatch"))
           }
       }
   }
   ```

3. Sync Gradle:
   ```bash
   ./gradlew --refresh-dependencies
   ```

---

## Step 4: Refactor StopWatch Composable

Replace the content of `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/StopWatch.kt`:

```kotlin
package io.codenode.mobileapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.generated.stopwatch.StopWatchController
import io.codenode.fbpdsl.model.ExecutionState

@Composable
fun StopWatch(
    modifier: Modifier = Modifier,
    minSize: Dp = 200.dp
) {
    // Create and remember the controller
    val controller = remember { StopWatchController() }

    // Optional: Bind to Android/iOS lifecycle
    // This pauses the timer when app goes to background and resumes when returning
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(controller, lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner.lifecycle)
    }

    // Collect state from controller
    val executionState by controller.executionStateFlow.collectAsState()
    val elapsedSeconds by controller.elapsedSecondsFlow.collectAsState()
    val elapsedMinutes by controller.elapsedMinutesFlow.collectAsState()

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

        Spacer(modifier = Modifier.height(16.dp))

        // Digital time display
        val minutesStr = elapsedMinutes.toString().padStart(2, '0')
        val secondsStr = elapsedSeconds.toString().padStart(2, '0')
        Text(
            text = "$minutesStr:$secondsStr",
            style = TextStyle(fontSize = 24.sp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Control buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    if (isRunning) controller.stop() else controller.start()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color.Red else Color.Green
                )
            ) {
                Text(if (isRunning) "Stop" else "Start")
            }

            Button(
                onClick = { controller.reset() },
                enabled = !isRunning && (elapsedSeconds > 0 || elapsedMinutes > 0)
            ) {
                Text("Reset")
            }
        }
    }
}
```

### Lifecycle Binding (Optional)

The `bindToLifecycle()` call is optional but recommended for mobile apps. It provides:

- **Auto-pause**: Timer pauses when app goes to background (ON_PAUSE)
- **Auto-resume**: Timer resumes when app returns to foreground (ON_RESUME) if it was running before
- **Clean stop**: Timer stops when activity is destroyed (ON_DESTROY)

If you prefer manual control only (no automatic pause/resume), simply remove the `LaunchedEffect` block.

---

## Step 5: Implement UseCases (if not generated)

### TimerEmitterComponent.kt

If the generated UseCase is a stub, implement the timer logic:

```kotlin
class TimerEmitterComponent : ProcessingLogic {
    private var seconds = 0
    private var minutes = 0

    override suspend fun process(
        inputs: Map<String, Any?>,
        outputs: MutableMap<String, Any?>,
        config: ControlConfig,
        state: ExecutionState
    ) {
        while (state == ExecutionState.RUNNING) {
            delay(config.speedAttenuation)
            seconds += 1
            if (seconds >= 60) {
                seconds = 0
                minutes += 1
            }
            outputs["elapsedSeconds"] = seconds
            outputs["elapsedMinutes"] = minutes
        }
    }
}
```

### DisplayReceiverComponent.kt

The display receiver typically just forwards values to UI state; often no custom implementation needed.

---

## Step 6: Test the Integration

### Unit Tests

Run the test suite:
```bash
./gradlew :StopWatch:test
./gradlew :KMPMobileApp:testDebugUnitTest
```

### Manual Testing

1. **Android**:
   ```bash
   ./gradlew :KMPMobileApp:installDebug
   ```
   - Launch app on device/emulator
   - Tap Start → watch seconds count
   - Tap Stop → timer pauses
   - Tap Reset → values return to 00:00

2. **iOS** (requires Xcode):
   - Open `KMPMobileApp/iosApp/iosApp.xcodeproj`
   - Run on simulator

---

## Troubleshooting

### "Unresolved reference: StopWatchController"

- Ensure `include(":StopWatch")` is in settings.gradle.kts
- Ensure `implementation(project(":StopWatch"))` is in dependencies
- Run `./gradlew --refresh-dependencies`

### "Execution fails silently"

- Check that `speedAttenuation` is set (default 0 = no delay)
- Verify RootControlNode is transitioning to RUNNING state
- Add logging in TimerEmitterComponent.process()

### "Build fails with Kotlin version mismatch"

- Ensure StopWatch/build.gradle.kts uses Kotlin 2.1.21
- Ensure Compose version matches KMPMobileApp (1.7.3)

---

## Next Steps

- [ ] Add hour display for long-running timers
- [ ] Add lap time recording (new CodeNode)
- [ ] Publish StopWatch module to Maven Local
- [ ] Create iOS-specific UI variation

---

## References

- [Specification](spec.md)
- [Implementation Plan](plan.md)
- [Data Model](data-model.md)
- [Research](research.md)
