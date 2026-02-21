# Quickstart: Generate Runtime Files from FlowGraph Compilation

**Feature**: 027-generate-runtime-files
**Date**: 2026-02-20

## Integration Scenario

### Scenario 1: Compile StopWatch2 FlowGraph

1. Open graphEditor, load or create "StopWatch2" FlowGraph with:
   - TimerEmitter node (GENERIC, 0 inputs, 2 outputs: "elapsedSeconds" Int, "elapsedMinuts" Int)
   - DisplayReceiver node (GENERIC, 2 inputs: "seconds" Int, "minutes" Int, 0 outputs)
   - 2 connections: elapsedSeconds → seconds, elapsedMinuts → minutes

2. Click Compile/Save action

3. **Expected output** in `StopWatch2/src/commonMain/kotlin/io/codenode/stopwatch2/`:

```
generated/
├── StopWatch2.flow.kt                     (existing - already generated)
├── StopWatch2Flow.kt                      (NEW - this feature)
├── StopWatch2Controller.kt                (NEW - this feature)
├── StopWatch2ControllerInterface.kt       (NEW - this feature)
├── StopWatch2ControllerAdapter.kt         (NEW - this feature)
└── StopWatch2ViewModel.kt                 (NEW - this feature)
usecases/
├── logicmethods/
│   ├── TimerEmitterProcessLogic.kt        (existing - already generated)
│   └── DisplayReceiverProcessLogic.kt     (existing - already generated)
```

### Scenario 2: Verify Generated File Contents

**StopWatch2Flow.kt** should contain:
- `import io.codenode.stopwatch2.usecases.TimerEmitterComponent`
- `import io.codenode.stopwatch2.usecases.DisplayReceiverComponent`
- `internal val timerEmitter = TimerEmitterComponent()`
- `internal val displayReceiver = DisplayReceiverComponent()`
- `wireConnections()` with:
  - `displayReceiver.inputChannel = timerEmitter.outputChannel1`
  - `displayReceiver.inputChannel2 = timerEmitter.outputChannel2`

**StopWatch2Controller.kt** should contain:
- `val seconds: StateFlow<Int>` (from DisplayReceiver "seconds" input port)
- `val minutes: StateFlow<Int>` (from DisplayReceiver "minutes" input port)
- `val executionState: StateFlow<ExecutionState>`
- Methods: `start()`, `stop()`, `pause()`, `resume()`, `reset()`

**StopWatch2ControllerInterface.kt** should contain:
- Same StateFlow properties and methods as Controller, declared as interface members

**StopWatch2ControllerAdapter.kt** should contain:
- Implements `StopWatch2ControllerInterface`
- Constructor takes `StopWatch2Controller`
- All members delegate to controller

**StopWatch2ViewModel.kt** should contain:
- Extends `androidx.lifecycle.ViewModel`
- Constructor takes `StopWatch2ControllerInterface`
- All members delegate to controller interface

## Testing Approach

### Unit Tests (kotlinCompiler module)

For each generator, test with a FlowGraph fixture:
1. **Minimal flow**: 1 generator + 1 sink + 1 connection
2. **StopWatch-like flow**: 1 multi-output generator + 1 multi-input sink + 2 connections
3. **No-connection flow**: 2 standalone nodes, 0 connections
4. **No-sink flow**: Only generator nodes (no observable state except executionState)
5. **Duplicate port names**: 2 sinks with same port names → disambiguation check

### Integration Tests (graphEditor module)

Test via `ModuleSaveService.saveModule()`:
1. Save a FlowGraph and verify all 5 files are created in `generated/` directory
2. Re-save and verify files are overwritten (not duplicated)
3. Verify `filesCreated` list includes the new runtime files
