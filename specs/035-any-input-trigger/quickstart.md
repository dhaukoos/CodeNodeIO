# Quickstart: Any-Input Trigger Mode

**Feature**: 035-any-input-trigger
**Date**: 2026-03-01

## Integration Scenario 1: Creating an Any-Input Node via UI

```
1. Open the Graph Editor → Node Generator panel
2. Enter name: "SensorMerger"
3. Set Inputs: 2
4. Set Outputs: 1
5. Observe: "Any Input" toggle switch appears (was hidden at 1 input)
6. Toggle "Any Input" ON
7. Observe: Type preview changes from "in2out1" to "in2anyout1"
8. Click "Create"
9. Verify: Node appears in palette with type "in2anyout1"
```

## Integration Scenario 2: Any-Input Runtime Behavior

```kotlin
// Create an any-input processor
val merger = CodeNodeFactory.createIn2AnyOut1Processor<Int, Int, Int>(
    name = "Merger"
) { a, b -> a + b }

// Wire channels
merger.inputChannel1 = channel1
merger.inputChannel2 = channel2
merger.outputChannel = outputChannel

// Start
merger.start(scope) { }

// Send to input 1 only → process fires with (value1=5, value2=0)
channel1.send(5)
// Output: 5

// Send to input 2 only → process fires with (value1=5, value2=3)
channel2.send(3)
// Output: 8
```

## Integration Scenario 3: Code Generation Output

Given a flow graph with an any-input node `SensorMerger` (2 inputs, 1 output):

### Generated Flow Code
```kotlin
// Uses any-input factory method
val sensorMerger = CodeNodeFactory.createIn2AnyOut1Processor<Int, Int, Int>(
    name = "SensorMerger",
    process = sensorMergerTick
)
```

### Generated Processing Logic Stub
```kotlin
import io.codenode.fbpdsl.runtime.In2AnyOut1TickBlock

val sensorMergerTick: In2AnyOut1TickBlock<Int, Int, Int> = { input1, input2 ->
    // TODO: Implement SensorMerger tick logic
    0
}
```

## Integration Scenario 4: Toggle Behavior with Input Count Changes

```
1. Set Inputs: 3, toggle "Any Input" ON → type preview: "in3anyout1"
2. Reduce Inputs to 1 → "Any Input" toggle hides, turns OFF → type preview: "in1out1"
3. Increase Inputs to 2 → "Any Input" toggle reappears (OFF) → type preview: "in2out1"
```

## Integration Scenario 5: Persistence Round-Trip

```
1. Create an any-input node "SensorMerger" (2 inputs, 1 output, anyInput=true)
2. Close and reopen the application
3. Verify: "SensorMerger" appears in palette with type "in2anyout1"
4. Verify: CustomNodeDefinition JSON contains "anyInput": true
```
