# Quickstart: Redesign Processing Logic Stub Generator

**Feature**: 026-processing-logic-stubs
**Date**: 2026-02-20

## Before & After

### Before: ProcessingLogic Pattern

**Generated stub** (`TimerEmitterComponent.kt`):
```kotlin
package io.codenode.stopwatch.usecases

import io.codenode.fbpdsl.model.ProcessingLogic
import io.codenode.fbpdsl.model.InformationPacket

/**
 * ProcessingLogic stub for the TimerEmitter node.
 * Node type: GENERATOR
 * Outputs: elapsedSeconds (Int), elapsedMinutes (Int)
 */
class TimerEmitterComponent : ProcessingLogic {
    override suspend operator fun invoke(
        inputs: Map<String, InformationPacket<*>>
    ): Map<String, InformationPacket<*>> {
        TODO("Implement TimerEmitter processing logic")
    }
}
```

**Generated factory code**:
```kotlin
val timerEmitterLogic = TimerEmitterComponent()
val timerEmitter = CodeNode(
    name = "TimerEmitter",
    processingLogic = timerEmitterLogic,
    // ...
)
```

**Problems**:
- Untyped: `Map<String, InformationPacket<*>>` loses all type safety
- Class ceremony: interface, override, operator fun syntax for a simple lambda
- Doesn't integrate with the channel-based runtime (tick blocks, NodeRuntime)

---

### After: Tick Function Stub Pattern

**Generated stub** (`TimerEmitterProcessLogic.kt` in `logicmethods/`):
```kotlin
package io.codenode.stopwatch.logicmethods

import io.codenode.fbpdsl.runtime.Out2TickBlock
import io.codenode.fbpdsl.runtime.ProcessResult2

/**
 * Tick function for the TimerEmitter node.
 *
 * Node type: Generator (0 inputs, 2 outputs)
 *
 * Outputs:
 *   - elapsedSeconds: Int
 *   - elapsedMinutes: Int
 */
val timerEmitterTick: Out2TickBlock<Int, Int> = {
    // TODO: Implement TimerEmitter tick logic
    ProcessResult2.both(0, 0)
}
```

**Generated factory code**:
```kotlin
import io.codenode.stopwatch.logicmethods.timerEmitterTick

val timerEmitter = CodeNodeFactory.createTimedOut2Generator<Int, Int>(
    name = "TimerEmitter",
    tickIntervalMs = 1000,
    tick = timerEmitterTick
)
```

**Benefits**:
- Fully typed: `Out2TickBlock<Int, Int>` enforces the correct signature
- Minimal: just a val with a lambda — no class, no interface, no overrides
- Directly compatible with the channel-based runtime

---

## Complete Example: StopWatch After Migration

### Generator Stub: `logicmethods/TimerEmitterProcessLogic.kt`

```kotlin
package io.codenode.stopwatch.logicmethods

import io.codenode.fbpdsl.runtime.Out2TickBlock
import io.codenode.fbpdsl.runtime.ProcessResult2

/**
 * Tick function for the TimerEmitter node.
 *
 * Node type: Generator (0 inputs, 2 outputs)
 *
 * Outputs:
 *   - elapsedSeconds: Int
 *   - elapsedMinutes: Int
 */
val timerEmitterTick: Out2TickBlock<Int, Int> = {
    // User fills in actual logic:
    ProcessResult2.both(0, 0)
}
```

### Sink Stub: `logicmethods/DisplayReceiverProcessLogic.kt`

```kotlin
package io.codenode.stopwatch.logicmethods

import io.codenode.fbpdsl.runtime.In2SinkTickBlock

/**
 * Tick function for the DisplayReceiver node.
 *
 * Node type: Sink (2 inputs, 0 outputs)
 *
 * Inputs:
 *   - seconds: Int
 *   - minutes: Int
 */
val displayReceiverTick: In2SinkTickBlock<Int, Int> = { seconds, minutes ->
    // TODO: Implement DisplayReceiver tick logic
}
```

### Processor Stub Example: `logicmethods/DataTransformerProcessLogic.kt`

```kotlin
package io.codenode.stopwatch.logicmethods

import io.codenode.fbpdsl.runtime.TransformerTickBlock

/**
 * Tick function for the DataTransformer node.
 *
 * Node type: Transformer (1 input, 1 output)
 *
 * Input: rawData (String)
 * Output: processedData (Int)
 */
val dataTransformerTick: TransformerTickBlock<String, Int> = { rawData ->
    // TODO: Implement DataTransformer tick logic
    0
}
```

### Filter Stub Example: `logicmethods/EvenFilterProcessLogic.kt`

```kotlin
package io.codenode.stopwatch.logicmethods

import io.codenode.fbpdsl.runtime.FilterTickBlock

/**
 * Tick function for the EvenFilter node.
 *
 * Node type: Filter (1 input, 1 output, same type)
 *
 * Input: value (Int)
 * Output: value (Int) — passed through if predicate returns true
 */
val evenFilterTick: FilterTickBlock<Int> = { value ->
    // TODO: Implement EvenFilter filter logic
    true
}
```
