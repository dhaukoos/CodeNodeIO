# Quickstart: State Properties Stubs

## Scenario: StopWatch4-like FlowGraph Compilation

### Input: FlowGraph with 2-output generator + 2-input sink

```
TimerEmitter (GENERATOR)          DisplayReceiver (SINK)
  outputs:                          inputs:
    - elapsedSeconds: Int             - seconds: Int
    - elapsedMinutes: Int             - minutes: Int
```

### Expected Output After Compile

#### 1. `stateProperties/TimerEmitterStateProperties.kt`

```kotlin
package io.codenode.stopwatch4.stateProperties

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TimerEmitterStateProperties {
    internal val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSecondsFlow: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    internal val _elapsedMinutes = MutableStateFlow(0)
    val elapsedMinutesFlow: StateFlow<Int> = _elapsedMinutes.asStateFlow()

    fun reset() {
        _elapsedSeconds.value = 0
        _elapsedMinutes.value = 0
    }
}
```

#### 2. `stateProperties/DisplayReceiverStateProperties.kt`

```kotlin
package io.codenode.stopwatch4.stateProperties

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DisplayReceiverStateProperties {
    internal val _seconds = MutableStateFlow(0)
    val secondsFlow: StateFlow<Int> = _seconds.asStateFlow()

    internal val _minutes = MutableStateFlow(0)
    val minutesFlow: StateFlow<Int> = _minutes.asStateFlow()

    fun reset() {
        _seconds.value = 0
        _minutes.value = 0
    }
}
```

#### 3. `processingLogic/TimerEmitterProcessLogic.kt` (updated stub)

```kotlin
package io.codenode.stopwatch4.processingLogic

import io.codenode.fbpdsl.runtime.Out2TickBlock
import io.codenode.fbpdsl.runtime.ProcessResult2
import io.codenode.stopwatch4.stateProperties.TimerEmitterStateProperties

val timerEmitterTick: Out2TickBlock<Int, Int> = {
    // TODO: Implement TimerEmitter tick logic
    ProcessResult2.both(0, 0)
}
```

#### 4. `generated/StopWatch4Flow.kt` (updated Flow class)

```kotlin
class StopWatch4Flow {
    // Observable state delegated from state properties
    val secondsFlow: StateFlow<Int> = DisplayReceiverStateProperties.secondsFlow
    val minutesFlow: StateFlow<Int> = DisplayReceiverStateProperties.minutesFlow

    internal val timerEmitter = CodeNodeFactory.createTimedOut2Generator<Int, Int>(
        name = "TimerEmitter",
        tickIntervalMs = 1000L,
        tick = timerEmitterTick
    )

    internal val displayReceiver = CodeNodeFactory.createIn2Sink<Int, Int>(
        name = "DisplayReceiver",
        consume = { seconds, minutes ->
            DisplayReceiverStateProperties._seconds.value = seconds
            DisplayReceiverStateProperties._minutes.value = minutes
            displayReceiverTick(seconds, minutes)
        }
    )

    fun reset() {
        TimerEmitterStateProperties.reset()
        DisplayReceiverStateProperties.reset()
    }
    // ... start(), stop(), wireConnections() unchanged
}
```

## Verification Checklist

1. Compile a StopWatch4-like FlowGraph
2. Verify `stateProperties/` directory created with 2 files
3. Verify each file contains correct MutableStateFlow/StateFlow pairs matching port names/types
4. Verify processing logic stubs import their state properties object
5. Verify Flow class delegates `secondsFlow`/`minutesFlow` from `DisplayReceiverStateProperties`
6. Verify Flow's `reset()` calls both state properties' `reset()` methods
7. Re-compile and verify existing state property files are preserved
